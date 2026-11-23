import java.io.*;
import java.util.*;

/** Public leaderboard includes offline accounts, never admin-enabled test cheats. */
final class Ranking793 {
    static final int MAX_ENTRIES = 128; // Existing E72/E6 client protocol limit.

    static void write(GunnyServer server, GunnyServer.Session viewer, DataOutputStream out) throws Exception {
        List<CloudStore.Profile> list = new ArrayList<>();
        for (CloudStore.Profile profile : server.store.rankingProfiles()) {
            if (profile != null && !profile.testCheats) list.add(profile);
        }
        // Preserve the existing leaderboard order.
        list.sort(Comparator.<CloudStore.Profile>comparingInt(p -> p.wins).reversed()
                .thenComparing(Comparator.comparingInt((CloudStore.Profile p) -> p.maxDamage).reversed())
                .thenComparing(p -> p.id));
        int count = Math.min(MAX_ENTRIES, list.size());
        out.writeShort(count);
        for (int i = 0; i < count; i++) {
            CloudStore.Profile p = list.get(i);
            int favorite = p.equipped >= 0 && p.equipped < 10 ? p.equipped : 0;
            for (int w = 0; w < 10; w++)
                if (p.weaponUses[w] > p.weaponUses[favorite]) favorite = w;
            out.writeInt(p.publicId);
            out.writeInt(p.experience);
            out.writeUTF(p.name);
            out.writeByte(favorite);
            out.writeInt(p.maxDamage);
            out.writeInt(p.wins);
        }
        out.writeShort(count);
        for (int i = 0; i < count; i++) {
            CloudStore.Profile p = list.get(i);
            out.writeByte(p.gender);
            out.writeByte(p.skin);
            out.writeBoolean(p.custom);
            out.writeByte(p.wornMask);
            for (int k = 0; k < 5; k++) out.writeByte(p.look[k]);
        }
    }
}
