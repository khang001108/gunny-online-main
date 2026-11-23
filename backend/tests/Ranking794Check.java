import java.io.*;
import java.net.*;
import java.nio.file.*;

/** Regression: all saved profiles, online or offline, except enabled test cheats. */
public class Ranking794Check {
    static DataInputStream ranking(GunnyServer server, GunnyServer.Session viewer) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Ranking793.write(server, viewer, new DataOutputStream(bytes));
        return new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    }

    public static void main(String[] args) throws Exception {
        CloudStore.Memory store = new CloudStore.Memory();
        GunnyServer.Session online = Expansion780Check.session(store, "Portrait794", 70);
        online.profile.gender = 1;
        online.profile.skin = 2;
        online.profile.custom = true;
        online.profile.wornMask = 125;
        for (int i = 0; i < 5; i++) online.profile.look[i] = i;
        store.register("offline@ranking.test", "test-password-123", "Offline User");
        CloudStore.Profile offline = store.profile(store.login("offline@ranking.test", "test-password-123").userId);
        offline.wins = 11; // Ranks first even without a session.
        store.register("cheat@ranking.test", "test-password-123", "Cheat User");
        CloudStore.Profile cheat = store.profile(store.login("cheat@ranking.test", "test-password-123").userId);
        cheat.wins = 1000;
        cheat.testCheats = true;
        try (GunnyServer server = new GunnyServer(new InetSocketAddress("127.0.0.1", 0),
                Path.of("backend/assets"), store)) {
            online.token = "portrait";
            server.sessions.put(online.token, online);
            DataInputStream in = ranking(server, online);
            if (in.readUnsignedShort() != 2) throw new AssertionError("Online plus offline players");
            if (in.readInt() != offline.publicId) throw new AssertionError("Offline player ranks first");
            in.readInt();
            if (!"Offline User".equals(in.readUTF())) throw new AssertionError("Offline name");
            in.readUnsignedByte();in.readInt();in.readInt();
            if (in.readInt() != online.profile.publicId) throw new AssertionError("Online player second");
            in.readInt();in.readUTF();in.readUnsignedByte();in.readInt();in.readInt();
            if (in.readUnsignedShort() != 2) throw new AssertionError("Portrait metadata count");
            for (int k = 0; k < 2; k++) {
                int gender=in.readUnsignedByte(), skin=in.readUnsignedByte();
                boolean custom=in.readBoolean();int mask=in.readUnsignedByte();
                if(k==1 && (gender!=1||skin!=2||!custom||mask!=125))
                    throw new AssertionError("Online portrait unchanged");
                for (int i = 0; i < 5; i++) {
                    int look=in.readUnsignedByte();
                    if (k==1 && look!=i) throw new AssertionError("Appearance layer");
                }
            }
            if (in.available()!=0) throw new AssertionError("Trailing metadata");

            online.profile.testCheats=true;
            in=ranking(server, online);
            if(in.readUnsignedShort()!=1 || in.readInt()!=offline.publicId)
                throw new AssertionError("Online cheat account excluded");
            online.profile.testCheats=false;
            cheat.testCheats=false;
            in=ranking(server,online);
            if(in.readUnsignedShort()!=3 || in.readInt()!=cheat.publicId)
                throw new AssertionError("Disabling cheat restores offline account");
            online.profile.testCheats=true;offline.testCheats=true;cheat.testCheats=true;
            in=ranking(server,online);
            if(in.readUnsignedShort()!=0||in.readUnsignedShort()!=0)
                throw new AssertionError("All cheat accounts excluded with matching metadata");
        }
        System.out.println("PASS: offline profiles, cheat exclusion/re-enable, sort order and portrait protocol");
    }
}
