import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** Exercises global activity ordering through the authenticated admin HTTP route. */
public final class AdminActivityCheck {
    static String id(int index) { return new UUID(0, index + 1).toString(); }

    static Map<String,Object> account(int index, Instant now) {
        var row = new HashMap<String,Object>();
        row.put("id", id(index));
        row.put("display_name", "Player " + index);
        row.put("email", "player" + index + "@activity.test");
        row.put("public_id", 1001 + index);
        row.put("created_at", now.minusSeconds(86400L * (index + 1)).toString());
        row.put("last_seen_at", now.minusSeconds(3600L * (index + 1)).toString());
        row.put("last_login_at", now.minusSeconds(7200L * (index + 1)).toString());
        row.put("max_offline_seconds", 0);
        row.put("coins", 0);
        row.put("shopping_money", 0);
        return row;
    }

    static void session(GunnyServer server, String token, int index, long seen, long expiry) {
        var session = new GunnyServer.Session();
        session.id = id(index);
        session.token = token;
        session.lastSeen = seen;
        session.expiry = expiry;
        server.sessions.put(token, session);
    }

    static Map<String,Object> list(String query)throws Exception {
        var reply = Admin750Check.req("accounts?" + query, null, false);
        Admin750Check.ok(reply.statusCode() == 200, "Authenticated account list loads");
        return Json.obj(Json.parse(reply.body()));
    }

    public static void main(String[] args)throws Exception {
        var store = new CloudStore.Memory();
        var records = new Admin750Check.Records(store) {
            synchronized Object call(String operation, Map<String,Object> input)throws Exception {
                if (operation.equals("retention-policy")) return Map.of("enabled", false, "offline_days", 90);
                if (operation.equals("retention-save")) return Map.of("ok", true);
                return super.call(operation, input);
            }
        };
        Instant now = Instant.now();
        for (int index = 0; index < 130; index++) records.accounts.put(id(index), account(index, now));
        records.accounts.get(id(0)).remove("last_seen_at");
        records.accounts.get(id(0)).remove("last_login_at");
        records.accounts.get(id(110)).put("last_seen_at", now.minusSeconds(1).toString());
        records.accounts.get(id(111)).put("last_login_at", now.minusSeconds(2).toString());
        try (var server = new GunnyServer(new InetSocketAddress("127.0.0.1", args.length > 0 ? 8765 : 0),
                Path.of("backend/assets"), store, records, Admin750Check.PASSWORD, true)) {
            server.admin.tasks.shutdownNow();
            Admin750Check.base = "http://127.0.0.1:" + server.port();
            long millis = now.toEpochMilli();
            session(server, "old-session", 129, millis - 120000, millis + 3600000);
            session(server, "current-session", 129, millis - 5000, millis + 3600000);
            session(server, "expired-session", 127, millis, millis - 1);
            session(server, "stale-session", 128, millis - 61000, millis + 3600000);
            if (args.length > 0) {
                server.clock.scheduleWithFixedDelay(() -> server.sessions.get("current-session").lastSeen = System.currentTimeMillis(),
                        0, 10, java.util.concurrent.TimeUnit.SECONDS);
                System.out.println("Local activity preview " + Admin750Check.base + "/admin/");
                new java.util.concurrent.CountDownLatch(1).await();
                return;
            }
            var login = Admin750Check.req("login", Map.of("password", Admin750Check.PASSWORD), true);
            Admin750Check.cookie = login.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
            var first = list("offset=0");
            List<?> rows = (List<?>) first.get("accounts");
            Admin750Check.ok(rows.size() == 100 && Json.num(first, "matched", 0) == 130, "Activity ordering retains 100-row pagination and matched count");
            var leader = Json.obj(rows.get(0));
            Admin750Check.ok(leader.get("id").equals(id(129)), "Online player from the second registration page moves to the top");
            Admin750Check.ok(Instant.parse(Json.str(leader, "last_seen_at")).toEpochMilli() == millis - 5000,
                    "Most recent session overrides stale stored presence");
            Admin750Check.ok(Json.obj(rows.get(1)).get("id").equals(id(110)) && Json.obj(rows.get(2)).get("id").equals(id(111)),
                    "Offline players follow last connection or login in descending order");
            Admin750Check.ok(((List<?>) first.get("online")).equals(List.of(id(129))), "Expired and stale sessions are excluded from online status");
            var sorted = list("offset=0&sort=offline");
            var sortedRows = (List<?>) sorted.get("accounts");
            long previous = Long.MAX_VALUE;
            for(Object entry:sortedRows){long seconds=((Number)Json.obj(entry).get("offline_seconds")).longValue();Admin750Check.ok(seconds<=previous,"Current inactivity sorting descends");previous=seconds;}
            Admin750Check.ok(((Number)leader.get("offline_seconds")).longValue()==0,"Online resets offline counter despite history");
            var second = list("offset=100");
            var combined = new HashSet<String>();
            for (var page : List.of(first, second)) for (Object value : (List<?>) page.get("accounts"))
                combined.add(Json.str(Json.obj(value), "id"));
            var tail = (List<?>) second.get("accounts");
            Admin750Check.ok(tail.size() == 30 && combined.size() == 130, "Activity pages contain each account once");
            Admin750Check.ok(Json.obj(tail.get(29)).get("id").equals(id(0)), "Accounts without activity sort last");
            var filtered = list("offset=0&search=player129%40activity.test");
            Admin750Check.ok(((List<?>) filtered.get("accounts")).size() == 1 && Json.num(filtered, "matched", 0) == 1,
                    "Search filters before activity pagination");
            Admin750Check.ok(((List<?>) list("offset=200").get("accounts")).isEmpty(), "Out-of-range page is empty");
            server.admin.maintenance();
            Admin750Check.ok(Instant.parse(Json.str(records.accounts.get(id(129)), "last_seen_at")).toEpochMilli() == millis - 5000,
                    "Maintenance persists the latest of multiple sessions");
        }
    }
}
