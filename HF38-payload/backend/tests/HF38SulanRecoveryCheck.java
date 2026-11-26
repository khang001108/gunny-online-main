import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
/** Real server simulation, wire segments, fallback and room isolation. */
public final class HF38SulanRecoveryCheck {
 static int checks;
 static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);checks++;}
 static GunnyServer.Room room(GunnyServer s,int people,int diff)throws Exception{
  var host=player("hf38-"+people+"-"+diff+"-0");
  var r=s.new Room(host,6,2,-1,true);r.kind=3;r.strict=true;r.dungeonStage=6;r.difficulty=diff;
  for(int i=1;i<people;i++)r.members[2*i]=player("hf38-"+people+"-"+diff+"-"+i);
  for(var m:r.members)if(m!=null){m.room=r.id;m.armed=true;}
  r.refreshWaiting();for(var m:r.members)if(m!=null)m.armed=true;r.start();r.phase=1;
  for(int i=0;i<people;i++){var u=r.battle.units[2*i];u.hp=u.maxHp=30000;u.human=true;}
  return r;
 }
 static GunnyServer.Session player(String id){var p=ReturnRoomCheck.player(id);p.profile.experience=Progression.experienceForLevel(70);p.async=p.chunks=p.prefetch=p.delta=p.campaign=p.flock=p.tribal=p.tribal797=p.sulan7912=p.expansion=p.items=p.itemFx=p.actors=p.events=true;return p;}
 static void advance(GunnyServer.Room r,int max){for(int i=0;i<max&&!r.battle.pendingNext&&!r.battle.over;i++)r.tickAhead();ok(r.battle.pendingNext||r.battle.over,"encounter reaches terminal state");}
 static void ack(GunnyServer.Room r){int before=r.battle.turn;for(var m:r.members)if(m!=null){m.resolution=before;m.lastSeen=System.currentTimeMillis();}r.tick();ok(r.battle.turn==before+1,"all acknowledged, next turn");}
 static int frames(GunnyServer.Room r,int kind)throws Exception{int count=0;for(byte[] block:r.chunks){var in=new DataInputStream(new ByteArrayInputStream(block));int k=in.readUnsignedByte();in.readBoolean();int n=in.readUnsignedShort();ok(k!=4,"no complete-flight batching in Sulan");ok(n>0&&block.length<=16388,"bounded nonempty chunk");if(k==kind)count+=n;}return count;}
 static void fixture(GunnyServer.Room r)throws Exception{
  Path path=Path.of(System.getProperty("hf38.fixture", "hf38-scene-wire.bin"));
  try(var out=new DataOutputStream(Files.newOutputStream(path))){out.writeInt(r.battle.turn);out.writeInt(r.chunks.size());for(byte[] block:r.chunks){out.writeInt(block.length);out.write(block);}}
 }
 public static void main(String[] args)throws Exception{
  try(var s=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),new CloudStore.Memory())){
   s.clock.shutdownNow();
   for(int people=1;people<=3;people++)for(int diff=0;diff<3;diff++){
    var r=room(s,people,diff);var b=r.battle;
    advance(r,SulanRules.INTRO+5);ok(frames(r,5)>=SulanRules.INTRO,"full intro streamed");ok(r.chunkTerminal,"intro ends with terminal chunk");ack(r);
    b.units[1].hp=0;b.check();ok(b.tribalFx==SulanRules.TRANSFORM,"golden death starts original transform");
    advance(r,SulanRules.TRANSFORM+5);ok(b.tribalFx==0&&b.tribalPhase==2&&b.units[1].npc==TribalRules.RAGE,"red form finishes");ok(b.units[6].npc==TribalRules.BEAN&&b.units[6].hp>0,"pea summons");ok(frames(r,5)>=SulanRules.TRANSFORM,"full transformation streamed");ok(r.chunkTerminal,"transformation terminal emitted");
    if(people==1&&diff==0&&System.getProperty("hf38.fixture")!=null)fixture(r);
    // Last segment delivered but no playback progress: recover only lagging peer.
    var lag=r.members[0];int turn=b.turn;lag.hf37Progress(r.id,turn,r.chunks.size()-2,System.currentTimeMillis()-7000);lag.chunkDelivered=r.chunks.size()-1;r.waitingSince=System.currentTimeMillis()-7000;r.tick();ok(lag.hf37ForceTurn==turn&&b.turn==turn,"request checkpoint without forcing turn");
    ack(r);b.beginTurn(1);b.units[1].power=0;int before=b.shotSerial;advance(r,1200);ok(b.shotSerial-before==12,"red boss four salvos retained");ok(r.chunkTerminal&&frames(r,3)>0,"authoritative projectile chunks");ack(r);
   }
   var r=room(s,1,0);r.battle.tribalFx=0;r.battle.tribalState=TribalRules.IDLE;r.battle.active=0;r.chunkBegin(1);
   while(r.chunks.size()<GunnyServer.Room.HF38_MAX_CHUNKS)r.chunks.add(new byte[4]);r.chunkFlush(false);
   ok(r.hf38SnapshotOnly()&&r.chunks.isEmpty()&&r.chunkFrames==null,"cap releases retained history");
   var reply=new ByteArrayOutputStream();boolean rejected=false;try{r.chunkPacket(new DataOutputStream(reply),r.host,0,r.battle.turn,-1,-1);}catch(GunnyServer.SequenceGap expected){rejected=true;}
   ok(rejected,"old cached sequence requests recovery");r.chunkPacket(new DataOutputStream(reply),r.host,0,r.battle.turn,-1,Integer.MIN_VALUE+1);ok(r.checkpointActive(r.host)&&reply.toByteArray()[0]==0,"checkpoint yields full authoritative snapshot");
   r.battle.pendingNext=true;ack(r);ok(!r.hf38SnapshotOnly(),"next turn leaves fallback mode");
   // Deliberately corrupt one test room; an ordinary room must still execute a tick.
   var broken=room(s,1,0);broken.battle.active=999;s.rooms.put(broken.id,broken);
   var other=room(s,1,0);s.rooms.put(other.id,other);int fx=other.battle.tribalFx;s.tick();ok(other.battle.tribalFx==fx-1,"broken room does not interrupt other room tick");
  }
  System.out.println("PASS "+checks+" HF38 Sulan stream/transform/ACK/cap/isolation checks");
 }
}
