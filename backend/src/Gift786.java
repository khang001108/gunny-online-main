import java.io.*;
/** A receipt-backed item use; the handset never chooses reward rolls. */
final class Gift786 {
 static void handle(GunnyServer server,GunnyServer.Session s,DataInputStream in,DataOutputStream out,boolean exp)throws Exception{
  var room=server.rooms.get(s.room);if(room!=null)synchronized(room){if(room.phase!=0)throw new IllegalArgumentException("Không mở quà khi đang trong trận");}int type=in.readUnsignedByte();String key=in.readUTF();if(!exp&&type!=0)throw new IllegalArgumentException("Cập nhật client để mở quà EXP VIP");int[] rewards=server.store.giftOpen(s.id,type,key);s.profile=server.store.profile(s.id);GunnyServer.profile(out,s.profile);out.writeInt(s.profile.shoppingMoney);out.writeInt(s.profile.experience);Expansion780.write(out,s.profile);out.writeByte(rewards.length/2);for(int i=0;i<rewards.length;i+=2){out.writeByte(rewards[i]);out.writeInt(rewards[i+1]);}if(exp)VipServer.write(out,s.profile);
 }
}
