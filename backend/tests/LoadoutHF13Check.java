import java.io.*;import java.net.*;import java.nio.file.*;import java.util.*;
public final class LoadoutHF13Check {
 static int checks; static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);checks++;}
 static class Player implements AutoCloseable {
  final Socket socket;final DataInputStream in;final DataOutputStream out;String token="";int[] kit={0,1,2};
  Player(int port,String email)throws Exception{socket=new Socket("127.0.0.1",port);socket.setSoTimeout(5000);in=new DataInputStream(socket.getInputStream());out=new DataOutputStream(socket.getOutputStream());token=call("login",o->{o.writeUTF(email);o.writeUTF("test-password-123");}).readUTF();}
  DataInputStream call(String op,Tcp768Check.Writer writer)throws Exception{byte[] body=Tcp768Check.body(op,writer);out.writeInt(TcpGameServer.MAGIC);out.writeByte(1);out.writeInt(TribalTcpCheck.FLAGS|Integer.MIN_VALUE);out.writeUTF(token);for(int k:kit)out.writeByte(k);out.writeInt(body.length);out.write(body);out.flush();check(in.readInt()==200,"TCP transport");return Tcp768Check.ok(in.readNBytes(in.readInt()));}
  public void close()throws IOException{socket.close();}
 }
 public static void main(String[] args)throws Exception{
  for(int a=-1;a<=7;a++)for(int b=-1;b<=7;b++)for(int c=-1;c<=7;c++){
   int[] kit={a,b,c};boolean expected=true;boolean[] seen=new boolean[6];for(int id:kit){if(id<0||id>6){expected=false;break;}if(id<6){if(seen[id])expected=false;seen[id]=true;}}
   check(Battle.validKit(kit)==expected,"kit validation "+Arrays.toString(kit));if(expected){int[] legacy=GunnyServer.legacyKit(kit);check(Battle.validKit(legacy)&&legacy[0]<6&&legacy[1]<6&&legacy[2]<6,"legacy wire compatibility");}
  }
  var store=new CloudStore.Memory();String email="kit13@tcp.test";store.register(email,"test-password-123","Kit test");store.profile(store.login(email,"test-password-123").userId).experience=Progression.experienceForLevel(30);
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){server.clock.shutdownNow();server.startTcp("127.0.0.1",0,8);try(var p=new Player(server.tcpPort(),email)){
   int id=p.call("tribalcreate",o->{o.writeByte(0);o.writeUTF("Test");o.writeUTF("");}).readInt();var r=server.rooms.get(id);var session=server.sessions.get(p.token);check(session.emptyKit,"capability propagated");
   for(int[] kit:new int[][]{{6,1,2},{6,6,2},{6,6,6},{4,6,6},{4,5,6},{4,5,0},{4,1,0},{6,1,0}}){p.kit=kit;p.call("loadout",null);check(Arrays.equals(session.kit,kit),"server stores "+Arrays.toString(kit));}
   p.call("start",null);check(Arrays.equals(r.battle.units[0].kit,p.kit),"kit transfers into battle");r.battle.tribalFx=0;r.battle.active=0;int energy=r.battle.units[0].energy;check(!r.battle.item(0)&&r.battle.units[0].energy==energy,"empty slot cannot consume energy");
  }}System.out.println("PASS: "+checks+" loadout HF13 checks");
 }
}
