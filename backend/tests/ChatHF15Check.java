import java.io.*;
import java.net.*;
import java.nio.file.*;
public final class ChatHF15Check {
 static int passed;
 static void ok(boolean v,String s){if(!v)throw new AssertionError(s);passed++;System.out.println("PASS: "+s);}
 static String readMessage(DataInputStream in)throws Exception{in.readLong();String name=in.readUTF(),body=in.readUTF();in.readByte();return name+":"+body;}
 static DataInputStream poll(String t,boolean portraits)throws Exception{return ServerCheck.call(t,"chatpoll",o->{o.writeInt(0);o.writeLong(0);if(portraits)o.writeByte(15);});}
 public static void main(String[] args)throws Exception{
  CloudStore.Memory store=new CloudStore.Memory();store.register("a@hf15.test","test-password-123","Người Một");store.register("b@hf15.test","test-password-123","Người Hai");
  try(GunnyServer server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   ServerCheck.base="http://127.0.0.1:"+server.port();String a=Equipment718ServerCheck.login("a@hf15.test"),b=Equipment718ServerCheck.login("b@hf15.test");var pa=server.sessions.get(a).profile;pa.gender=1;pa.skin=2;pa.custom=true;pa.wornMask=125;pa.look[1]=3;
   String message="Xin chào! Đường về làng, tiếng Việt có dấu.";
   ServerCheck.call(a,"chatsend",o->{o.writeInt(0);o.writeUTF(message);o.writeByte(-1);});
   var in=poll(b,true);in.readBoolean();ok(in.readUnsignedByte()==1,"Preview message");ok(readMessage(in).equals("Người Một:"+message),"Unicode preview roundtrip");ok(in.readUnsignedByte()==1,"History message");ok(readMessage(in).equals("Người Một:"+message),"Unicode history roundtrip");ok(in.readUnsignedByte()==15&&in.readUnsignedByte()==1&&in.readBoolean(),"Optional portrait extension");ok(in.readUnsignedByte()==1&&in.readUnsignedByte()==2&&in.readBoolean()&&in.readUnsignedByte()==125,"Correct gender, skin, custom and mask");in.readUnsignedByte();ok(in.readUnsignedByte()==3,"Correct equipment look");for(int k=0;k<3;k++)in.readUnsignedByte();ok(in.available()==0,"Exact payload length");
   in=poll(b,false);in.readBoolean();in.readUnsignedByte();readMessage(in);in.readUnsignedByte();readMessage(in);ok(in.available()==0,"Old client receives unchanged payload");
   ServerCheck.call(a,"logout",null);in=poll(b,true);in.readBoolean();in.readUnsignedByte();readMessage(in);in.readUnsignedByte();readMessage(in);ok(in.readUnsignedByte()==15&&in.readUnsignedByte()==1&&in.readBoolean()&&in.readUnsignedByte()==1,"Offline sender portrait available from store");
   a=Equipment718ServerCheck.login("a@hf15.test");int id=ServerCheck.create(a,0,-1);ServerCheck.call(b,"join",o->{o.writeInt(id);o.writeByte(1);});var room=server.rooms.get(id);synchronized(room){room.strict=false;room.start();room.phase=1;}
   ServerCheck.call(a,"chatroomsend",o->{o.writeInt(id);o.writeByte(1);o.writeUTF(message);o.writeByte(-1);});in=ServerCheck.call(b,"chatroom",o->{o.writeInt(id);o.writeByte(1);});ok(in.readInt()==id&&in.readUnsignedByte()==1&&in.readUnsignedByte()==1,"Battle chat delivers speech");ok(in.readUnsignedByte()==0&&in.readUnsignedShort()>0&&in.readUTF().equals(message),"Battle chat preserves sender and Unicode");
  }System.out.println("PASS "+passed+" chat checks");
 }
}
