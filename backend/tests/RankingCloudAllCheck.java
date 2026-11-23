import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Offline, admin-cheat and public appearance fields from Supabase REST. */
public class RankingCloudAllCheck {
    public static void main(String[] args) throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        HttpServer fake = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fake.createContext("/rest/v1/profiles", x -> {
            if (!"GET".equals(x.getRequestMethod())) throw new AssertionError("ranking must read-only GET");
            if (!"sb_secret_test".equals(x.getRequestHeaders().getFirst("apikey")))
                throw new AssertionError("ranking must use server-side credential");
            if (x.getRequestHeaders().getFirst("Authorization") != null)
                throw new AssertionError("new sb_secret_ keys must not be sent as bearer");
            String q = x.getRequestURI().getRawQuery();
            if (q == null || !q.contains("or=(test_cheats.eq.false,test_cheats.is.null)")
                    || !q.contains("order=wins.desc,max_damage.desc,id.asc")
                    || !q.contains("limit=128") || !q.contains("weapon_uses")
                    || q.contains("online")) throw new AssertionError("incorrect global ranking filter: " + q);
            requestCount.incrementAndGet();
            // Include one test-cheat row even though the real database query filters it,
            // verifying the Java layer does not expose forbidden accounts either.
            String result = "["
                    + "{\"id\":\"u1\",\"public_id\":1001,\"display_name\":\"Offline Name\","
                    + "\"wins\":3,\"max_damage\":3500,\"experience\":500,\"equipped\":2,"
                    + "\"gender\":1,\"skin\":3,\"custom_look\":true,\"worn_mask\":125,"
                    + "\"look\":[1,2,3,4,5],\"weapon_uses\":[0,0,4,0,0,0,0,0,0,0],\"test_cheats\":false},"
                    + "{\"id\":\"u2\",\"public_id\":1002,\"display_name\":\"Test cheat\",\"test_cheats\":true},"
                    + "{\"id\":\"u3\",\"public_id\":1003,\"display_name\":\"Legacy NULL\",\"test_cheats\":null}"
                    + "]";
            byte[] response = result.getBytes(StandardCharsets.UTF_8);
            x.getResponseHeaders().set("Content-Type", "application/json");
            x.sendResponseHeaders(200, response.length);
            x.getResponseBody().write(response);
            x.close();
        });
        fake.start();
        try {
            CloudStore store = new CloudStore("http://127.0.0.1:" + fake.getAddress().getPort(),
                    "sb_publishable_test", "sb_secret_test", true);
            List<CloudStore.Profile> list = store.rankingProfiles();
            if (list.size() != 2 || !"Offline Name".equals(list.get(0).name)
                    || !"Legacy NULL".equals(list.get(1).name))
                throw new AssertionError("offline/noncheat and NULL rows only");
            CloudStore.Profile p = list.get(0);
            if (p.publicId != 1001 || p.experience != 500 || p.wins != 3
                    || p.maxDamage != 3500 || p.weaponUses[2] != 4 || p.look[4] != 5
                    || p.gender != 1 || !p.custom || p.wornMask != 125)
                throw new AssertionError("incorrect public profile mapping");
            if (requestCount.get() != 1) throw new AssertionError("unexpected number of database reads");
            System.out.println("PASS: Supabase global profiles REST filters, secret-key headers, cheat exclusion, portrait fields");
        } finally {fake.stop(0);}
    }
}
