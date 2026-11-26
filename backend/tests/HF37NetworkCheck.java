import java.io.*;
import java.net.*;
import java.nio.file.*;
/** Real server and TCP requests; clock stopped to make burst tests deterministic. */
public class HF37NetworkCheck {
 static int checks;
 static void ok(boolean b,String msg){if(!b)throw new AssertionError(msg);checks++;}
 static DataInputStream sync(TribalTcpCheck.Player p,int turn,int completed,int cached,boolean ack,int[][] commands)throws Exception{
  byte[] body=Tcp768Check.body("sync",o->{o.writeInt(0);o.writeByte(0);o.writeBoolean(true);o.writeInt(turn);o.writeBoolean(ack);o.writeInt(0);o.writeInt(-1);o.writeInt(completed);o.writeInt(cached);o.writeByte(commands.length);for(int[] c:commands){o.writeInt(c[0]);o.writeByte(c[1]);o.writeShort(c[2]);}});
  p.out.writeInt(TcpGameServer.MAGIC);p.out.writeByte(1);p.out.writeInt(p.flags);p.out.writeUTF(p.token);p.out.write(new byte[]{0,1,2});p.out.writeInt(body.length);p.out.write(body);p.out.flush();
  ok(p.in.readInt()==200,"TCP status");int size=p.in.readInt();byte[] reply=p.in.readNBytes(size);ok(reply.length==size,"TCP body complete");return new DataInputStream(new ByteArrayInputStream(reply));
 }
 static void success(DataInputStream r,int seq)throws Exception{ok(r.readUnsignedByte()==0,"successful sync");ok(r.readInt()==seq,"server sequence");}
 static void recover(DataInputStream r,int seq)throws Exception{ok(r.readUnsignedByte()==4,"recovery code");r.readUTF();ok(r.readInt()==seq,"recovery sequence");}
 public static void main(String[] args)throws Exception{
  var rate=new GunnyServer.Session();long t=10000000000L;
  ok(rate.hf37Allow(1,90,t),"allow delayed legitimate 90-tick batch");ok(!rate.hf37Allow(1,1,t),"reject immediate burst");ok(rate.hf37Allow(1,40,t+1000000000L),"refill using real elapsed time");ok(!rate.hf37Allow(1,41,t+2000000000L),"40 ticks per second ceiling after burst");ok(rate.hf37Allow(1,40,t+2000000000L),"exact allowed budget");ok(!rate.hf37Allow(1,0,t+2000000000L),"zero cost rejected");ok(rate.hf37Allow(2,90,t+2000000000L),"new room budget");
  // A 30 Hz normal client remains below the sustained ceiling.
  rate=new GunnyServer.Session();for(int i=0;i<900;i++)ok(rate.hf37Allow(7,1,t+i*33333333L),"normal 30Hz tick");
  var store=new CloudStore.Memory();String[] emails={"hf37a@tcp.test","hf37b@tcp.test","hf37c@tcp.test"};for(int i=0;i<3;i++){store.register(emails[i],"test-password-123","HF37 "+i);store.profile(store.login(emails[i],"test-password-123").userId).experience=Progression.experienceForLevel(70);}
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();server.startTcp("127.0.0.1",0,8);try(var a=new TribalTcpCheck.Player(server.tcpPort(),emails[0]);var b=new TribalTcpCheck.Player(server.tcpPort(),emails[1]);var c=new TribalTcpCheck.Player(server.tcpPort(),emails[2])){
    int id=a.call("tribalcreate",o->{o.writeByte(2);o.writeUTF("HF37");o.writeUTF("");}).readInt();
    for(var p:new TribalTcpCheck.Player[]{b,c}){p.call("join",o->{o.writeInt(id);o.writeByte(0);o.writeUTF("");});p.call("ready",o->o.writeBoolean(true));}
    // Start directly at final stage; exercise actual Sulan room & TCP handler.
    var r=server.rooms.get(id);r.dungeonStage=6;r.refreshWaiting();for(var m:r.members)if(m!=null)m.armed=true;a.call("start",null);r.phase=1;r.battle.tribalFx=0;r.battle.tribalState=TribalRules.IDLE;r.battle.active=0;r.battle.units[0].human=true;r.battle.countdown=600;r.battle.turnLimit=600;r.seconds=20;r.battle.pendingNext=false;r.battle.over=false;r.battle.ballCount=r.battle.bursts=r.battle.settle=0;int turn=r.battle.turn;var host=server.sessions.get(a.token);var peer=server.sessions.get(b.token);
    int before=r.battle.countdown;success(sync(a,turn,-1,-1,false,new int[][]{{1,6,90*16}}),1);ok(r.battle.countdown==before-90,"first batch applied once");int after=r.battle.countdown;
    recover(sync(a,turn,-1,-1,false,new int[][]{{2,6,90*16}}),2);ok(r.battle.countdown==after,"speed burst does not mutate simulation");
    success(sync(a,turn,-1,-1,false,new int[][]{{2,6,90*16}}),2);ok(r.battle.countdown==after,"duplicate command ignored");
    success(sync(a,turn,-1,Integer.MIN_VALUE+1,false,new int[][]{{3,6,10*16},{4,4,0}}),4);ok(r.battle.countdown==after&&!r.battle.pendingNext,"checkpoint discards old queued movement and pass");ok(r.checkpointActive(host),"host snapshot mode");
    // A malformed acknowledgement cannot keep a peer in a retry loop.
    recover(sync(b,turn,999,999,false,new int[0][]),0);success(sync(b,turn,-1,Integer.MIN_VALUE+1,false,new int[0][]),0);ok(r.checkpointActive(peer),"bad cache recovered");
    // Force a lagging third peer to request authoritative snapshot; others keep their state.
    var lag=server.sessions.get(c.token);r.battle.pendingNext=true;r.waitingSince=System.currentTimeMillis()-13000;lag.hf37Progress(id,turn,-1,r.waitingSince);r.tick();ok(lag.hf37ForceTurn==turn,"stalled peer flagged");recover(sync(c,turn,-1,-1,false,new int[0][]),0);success(sync(c,turn,-1,Integer.MIN_VALUE+1,false,new int[0][]),0);ok(r.checkpointActive(lag)&&lag.hf37ForceTurn==-1,"peer snapshot resync");
    for(var p:new TribalTcpCheck.Player[]{a,b,c})success(sync(p,turn,-1,-1,true,new int[0][]),server.sessions.get(p.token).sequence);r.tick();ok(r.battle.turn==turn+1&&!r.battle.pendingNext,"all recovered peers advance normal turn");
   }
  }
  System.out.println("PASS "+checks+" HF37 server budget and TCP recovery checks");
 }
}
