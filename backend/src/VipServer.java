import java.io.*;
final class VipServer {
 static void write(DataOutputStream out,CloudStore.Profile p)throws IOException{out.writeInt(p.vipExp);out.writeByte(p.wingOwned);out.writeByte(p.equippedWing);for(int n:p.vipCards)out.writeInt(n);out.writeInt(p.shoppingMoney);}
 static void handle(GunnyServer server,GunnyServer.Session s,String op,DataInputStream in,DataOutputStream out)throws Exception{
  if(!s.vip)throw new IllegalArgumentException("Cập nhật client VIP");
  if(op.equals("vipstate")){write(out,s.profile);return;}
  var room=server.rooms.get(s.room);if(room!=null&&room.phase!=0)throw new IllegalArgumentException("Chỉ dùng VIP/cánh trước trận");
  int item=in.readByte();String key=in.readUTF();String action=op.equals("vipuse")?"use":op.equals("vipbuy")?"buy":op.equals("vipequip")?"equip":"";
  if(action.length()==0)throw new IllegalArgumentException("Sai thao tác VIP");s.profile=server.store.vip(s.id,action,item,1,key);
  if(room!=null)synchronized(room){room.refreshWaiting();room.clearArmed();}write(out,s.profile);if(item>=13)Expansion780.write(out,s.profile);server.events.signal();
 }
}
