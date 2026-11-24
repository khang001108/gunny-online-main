import java.io.*;
import java.net.*;
import java.nio.file.*;

/** Exercises the new hidden solo queue and cancellation handshake over real TCP. */
public final class SoloMatchmaking7918Check {
 static int checks;
 static void ok(boolean b,String label){if(!b)throw new AssertionError(label);checks++;System.out.println("PASS: "+label);}
 static int quick(TcpGameplayCheck.Player p)throws Exception{return p.call("quick",out->out.writeByte(1)).readInt();}
 static int room(TcpGameplayCheck.Player p)throws Exception{return p.call("create2",out->{out.writeByte(1);out.writeByte(0);out.writeByte(-1);out.writeByte(12);}).readInt();}
 static boolean denied(TcpGameplayCheck.Player p,String op,Tcp768Check.Writer writer)throws Exception{
  byte[] response=Tcp768Check.frame(p.in,p.out,1,p.token,Tcp768Check.body(op,writer));return response[0]!=0;
 }
 static void cancel(TcpGameplayCheck.Player p)throws Exception{ok(p.call("cancelquick",null).readBoolean(),"Cancel removes a waiting ticket");}
 static void pair(GunnyServer server,int id,TcpGameplayCheck.Player a,TcpGameplayCheck.Player b)throws Exception{
  var r=server.rooms.get(id);ok(r!=null&&r.kind==1&&r.count==2&&r.phase==3,"Matched exactly two players in loading phase");
  ok(r.requestedMap==-1&&r.map>=0&&r.map<6,"Server chooses one random PvP map");
  ok(r.members[0]!=null&&r.members[1]!=null&&r.battle.units[0].human&&r.battle.units[1].human,"Both actors are real players");
  ok(server.sessions.get(a.token).room==id&&server.sessions.get(b.token).room==id,"Both sessions share the same battle");
  ok(!a.call("cancelquick",null).readBoolean(),"Late cancel cannot abandon a matched battle");
  a.sync(0,0,0);b.sync(0,0,0);TcpGameplayCheck.await(()->r.phase==1,"Both players complete loading before combat");
  a.call("leave",null);b.call("leave",null);
 }
 public static void main(String[] args)throws Exception{
  var store=new CloudStore.Memory();for(int i=0;i<4;i++)store.register("solo"+i+"@test.local","test-password-123","Solo "+i);
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.startTcp("127.0.0.1",0,8);
   try(var a=new TcpGameplayCheck.Player(server.tcpPort(),"solo0@test.local");var b=new TcpGameplayCheck.Player(server.tcpPort(),"solo1@test.local");var c=new TcpGameplayCheck.Player(server.tcpPort(),"solo2@test.local");var d=new TcpGameplayCheck.Player(server.tcpPort(),"solo3@test.local")){
    int id=quick(a);var waiting=server.rooms.get(id);
    ok(waiting.kind==1&&waiting.queued&&waiting.phase==0&&waiting.queueSince>0,"Solo waits with server-side 60-second timer");
    ok(b.call("rooms",null).readUnsignedShort()==0,"Solo queue hidden from room browser");
    ok(denied(b,"join",o->{o.writeInt(id);o.writeByte(1);o.writeUTF("");}),"Cannot join solo queue manually");
    ok(denied(a,"botvote",null),"Solo does not start against a bot");
    ok(quick(b)==id,"Solo vs solo shares first ticket");pair(server,id,a,b);
    int privateId=room(b);int q=quick(a);ok(server.rooms.get(q).phase==0,"Unqueued private room is not eligible");
    int joined=b.call("start",null).readInt();ok(joined==q&&!server.rooms.containsKey(privateId),"Private host start finds waiting solo");pair(server,q,a,b);
    privateId=room(b);b.call("start",null);ok(server.rooms.get(privateId).queued,"Private host queues first");q=quick(a);ok(!server.rooms.containsKey(privateId),"Solo finds one-player private room already queued");pair(server,q,a,b);
    q=quick(a);var expired=server.rooms.get(q);synchronized(expired){expired.queueSince=System.currentTimeMillis()-60001;expired.tick();}ok(expired.timedOut&&!expired.queued&&expired.phase==0,"Timeout stops search at 60 seconds");
    int q2=quick(b);ok(q2!=q&&server.rooms.get(q2).phase==0,"Expired ticket cannot be matched");cancel(a);cancel(b);
    int team=c.call("create3",o->{o.writeByte(1);o.writeByte(0);o.writeByte(-1);o.writeByte(12);o.writeUTF("Two allies");o.writeUTF("");}).readInt();
    d.call("join",o->{o.writeInt(team);o.writeByte(0);o.writeUTF("");});d.call("ready",o->o.writeBoolean(true));c.call("start",null);
    q=quick(a);ok(server.rooms.get(q).phase==0&&server.rooms.get(team).phase==0,"Two-player party stays separate from solo");cancel(a);
    c.call("leave",null);d.call("leave",null);
   }
  }
  System.out.println("All "+checks+" solo matchmaking checks passed");
 }
}
