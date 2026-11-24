import java.io.*;
import java.util.Arrays;
/** Per-session exact-byte cache. Full lobby whenever client cannot prove its base. */
final class BattleLobbyCache {
    private byte[] last;
    private int version;
    void write(DataOutputStream out,byte[] current,int clientVersion,boolean active)throws IOException {
        boolean changed=!Arrays.equals(last,current);
        if(changed||version==0){version=version==Integer.MAX_VALUE?1:version+1;last=current;}
        boolean full=!active||clientVersion!=version;
        out.writeInt(version);out.writeBoolean(full);
        if(full)out.write(current);
    }
}
