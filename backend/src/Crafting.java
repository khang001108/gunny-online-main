import java.io.*;
import java.util.*;

/** Server rolls are stored together with consumption under one idempotent receipt. */
final class Crafting {
    static boolean handles(String op) {
        return op.equals("forgechance") || op.equals("buycraft");
    }

    static void handle(GunnyServer server,String op,GunnyServer.Session session,
                       DataInputStream in,DataOutputStream out)throws Exception {
        if(session.room!=0)throw new IllegalArgumentException("Rời phòng trước khi mua hoặc rèn.");
        Map<String,Object> payload=new LinkedHashMap<>();
        String action;
        if(op.equals("buycraft")) {
            action="buy";
            int type=in.readUnsignedByte(), quantity=in.readUnsignedShort();
            if(type>6||quantity<1||quantity>1000)throw new IllegalArgumentException("Sai vật liệu.");
            payload.put("material",type);
            payload.put("quantity",quantity);
        }else{
            action="forge";
            int sex=in.readUnsignedByte(),kind=in.readUnsignedByte(),item=in.readUnsignedByte();
            int rank=in.readUnsignedByte(),grade=in.readUnsignedByte(),quantity=in.readUnsignedByte();
            boolean lucky=in.readBoolean(),protect=in.readBoolean();
            if(!ForgeRules.allowed(kind)||ForgeRules.chance(rank,grade,quantity,lucky)==0)
                throw new IllegalArgumentException("Sai yêu cầu cường hóa.");
            ForgeRules.index(sex,kind,item);
            payload.put("sex",sex);payload.put("kind",kind);payload.put("item",item);
            payload.put("rank",rank);payload.put("grade",grade);payload.put("quantity",quantity);
            payload.put("lucky",lucky);payload.put("protect",protect);
        }
        String key=in.readUTF();
        session.profile=server.store.crafting(session.id,action,payload,key,server.random.nextInt(10000));
        GunnyServer.profile(out,session.profile);
        out.writeInt(session.profile.shoppingMoney);
        Expansion780.write(out,session.profile);
    }
}
