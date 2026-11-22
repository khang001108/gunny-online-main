import java.io.*;import java.net.*;import java.nio.file.*;
public class Ranking794Check{
 public static void main(String[] args)throws Exception{
  var store=new CloudStore.Memory();var a=Expansion780Check.session(store,"Portrait794",70);a.profile.gender=1;a.profile.skin=2;a.profile.custom=true;a.profile.wornMask=125;for(int i=0;i<5;i++)a.profile.look[i]=i;
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   a.token="portrait";server.sessions.put(a.token,a);var bytes=new ByteArrayOutputStream();Ranking793.write(server,a,new DataOutputStream(bytes));var in=new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));if(in.readUnsignedShort()!=1)throw new AssertionError("One online account");in.readInt();in.readInt();in.readUTF();in.readByte();in.readInt();in.readInt();if(in.readUnsignedShort()!=1||in.readUnsignedByte()!=1||in.readUnsignedByte()!=2||!in.readBoolean()||in.readUnsignedByte()!=125)throw new AssertionError("Portrait uses server profile");for(int i=0;i<5;i++)if(in.readUnsignedByte()!=i)throw new AssertionError("Appearance layer");if(in.available()!=0)throw new AssertionError("Trailing metadata");
   a.profile.testCheats=true;bytes.reset();Ranking793.write(server,a,new DataOutputStream(bytes));in=new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));if(in.readUnsignedShort()!=0||in.readUnsignedShort()!=0)throw new AssertionError("Cheat portrait hidden too");
  }
  System.out.println("PASS ranking portrait appearance metadata, backwards-compatible prefix, cheat exclusion");
 }
}
