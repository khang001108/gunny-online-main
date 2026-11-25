import java.io.*;import java.net.*;import java.nio.file.*;import java.util.*;
/** Session takeover and a current-state checkpoint for an active Tà Thần stage 2. */
public final class SessionRecoveryHF13Check {
 static void ok(boolean value,String message){if(!value)throw new AssertionError(message);System.out.println("PASS: "+message);}
 static byte[] raw(TribalTcpCheck.Player p,String op,Tcp768Check.Writer w)throws Exception{
  byte[] body=Tcp768Check.body(op,w);p.out.writeInt(TcpGameServer.MAGIC);p.out.writeByte(1);p.out.writeInt(p.flags);p.out.writeUTF(p.token);p.out.write(new byte[]{0,1,2});p.out.writeInt(body.length);p.out.write(body);p.out.flush();if(p.in.readInt()!=200)throw new IOException("status");return p.in.readNBytes(p.in.readInt());
 }
 static byte[] packet(GunnyServer.Room r,GunnyServer.Session s,int cached)throws Exception{var b=new ByteArrayOutputStream();r.chunkPacket(new DataOutputStream(b),s,0,r.battle.turn,-1,cached);return b.toByteArray();}
 static byte[] current(GunnyServer server,GunnyServer.Room r,GunnyServer.Session s)throws Exception{var b=new ByteArrayOutputStream();b.write(0);server.snapshot(new DataOutputStream(b),r,0,s);return b.toByteArray();}
 static void sync(TribalTcpCheck.Player p,GunnyServer.Room r,int cached,boolean ack)throws Exception{p.call("sync",o->{o.writeInt(0);o.writeByte(0);o.writeBoolean(true);o.writeInt(r.battle.turn);o.writeBoolean(ack);o.writeInt(r.departureVersion);o.writeInt(-1);o.writeInt(-1);o.writeInt(cached);o.writeByte(0);o.writeInt(0);});}
 public static void main(String[] a)throws Exception{
  var store=new CloudStore.Memory();String email="hf13@tcp.test";store.register(email,"test-password-123","HF13 Gunner");store.profile(store.login(email,"test-password-123").userId).experience=Progression.experienceForLevel(40);
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();server.startTcp("127.0.0.1",0,8);
   try(var old=new TribalTcpCheck.Player(server.tcpPort(),email)){
    int id=old.call("tribalcreate",o->{o.writeByte(0);o.writeUTF("HF13");o.writeUTF("");}).readInt();var room=server.rooms.get(id);room.dungeonStage=4;room.refreshWaiting();old.call("start",null);room.phase=1;room.battle.tribalFx=0;room.battle.active=0;
    var prior=server.sessions.get(old.token);room.chunkBegin(1);room.chunkFrame();room.chunkFlush(false);ok(room.chunks.size()>0,"Stage 2 has historical frames before reconnect");int turn=room.battle.turn;room.battle.units[1].y+=7;int y=room.battle.units[1].y,hp=room.battle.units[0].hp;
    try(var fresh=new TribalTcpCheck.Player(server.tcpPort(),email)){
     var owner=server.sessions.get(fresh.token);ok(room.host==owner&&room.members[0]==owner&&prior.room==0&&!server.sessions.containsKey(old.token),"Only the new session owns the original room");
     ok(owner.async&&owner.chunks&&owner.prefetch&&owner.delta&&owner.events&&room.battle.deferNext,"Negotiated protocol preserved immediately on takeover");
     var expired=new DataInputStream(new ByteArrayInputStream(raw(old,"sync",null)));ok(expired.readUnsignedByte()==2&&expired.readUTF().equals(SessionRevocations.REPLACED),"Old device receives distinct account takeover error before command parsing");
     ok(fresh.call("tribalresume",null).readBoolean()&&room.battle.turn==turn&&room.battle.units[0].hp==hp&&room.battle.units[1].y==y,"Resume retains turn, HP, and current boss position");
     sync(fresh,room,Integer.MIN_VALUE+1,false);ok(room.checkpointActive(owner),"TCP recovery requests authoritative current-turn checkpoint");
     ok(Arrays.equals(packet(room,owner,-1),current(server,room,owner)),"Old chunks never replay after current-state checkpoint");
     room.battle.units[1].y+=5;ok(Arrays.equals(packet(room,owner,-1),current(server,room,owner)),"Subsequent recovery snapshot reflects new boss position, not history");
     room.battle.pendingNext=true;room.chunkTerminal=true;sync(fresh,room,-1,true);ok(owner.resolution==turn,"Checkpoint viewer can acknowledge turn without replaying old chunks");
     room.battle.pendingNext=false;room.battle.turn++;room.chunkBegin(1);room.chunkFrame();room.chunkFlush(false);ok(!room.checkpointActive(owner)&&packet(room,owner,-1)[0]==4,"Next turn returns to normal chunk playback");
    }
   }
  }
  var revoked=new SessionRevocations();revoked.replace("old",100);ok(revoked.reason("old",101).equals(SessionRevocations.REPLACED)&&revoked.reason("missing",101).equals(SessionRevocations.EXPIRED)&&revoked.reason("old",2_000_000).equals(SessionRevocations.EXPIRED),"Revocation distinguishes replaced, unknown and expired tokens");
 }
}
