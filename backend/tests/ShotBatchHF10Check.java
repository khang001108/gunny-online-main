import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
/** Check real authoritative frames, bounded batch fallback, and unchanged physics. */
public final class ShotBatchHF10Check {
 static void ok(boolean v,String label){if(!v)throw new AssertionError(label);System.out.println("PASS: "+label);}
 static GunnyServer.Room room(GunnyServer server,boolean batch,int dungeon,int extra)throws Exception{
  var host=ReturnRoomCheck.player("host"+batch+dungeon+extra);var guest=ReturnRoomCheck.player("guest"+batch+dungeon+extra);
  host.async=guest.async=host.prefetch=guest.prefetch=true;host.delta=guest.delta=batch;
  var r=server.new Room(host,1,0,-1);r.members[1]=guest;r.start();r.phase=1;
  r.battle.seed=123456;r.battle.wind=0;r.battle.lastWind=0;r.battle.dungeonStage=dungeon;
  for(int i=0;i<2;i++){var u=r.battle.units[i];u.x=i==0?100:380;u.y=r.ground.surface(u.x,0);u.hp=u.maxHp=10000;u.human=true;}
  r.battle.active=0;r.battle.units[0].face=1;r.battle.units[0].angle=45;r.battle.units[0].power=70;r.battle.extra=extra;r.battle.fire();return r;
 }
 static byte[] finalState(GunnyServer server,GunnyServer.Room room)throws Exception{var bytes=new ByteArrayOutputStream();server.snapshot(new DataOutputStream(bytes),room,0,room.host,true);return bytes.toByteArray();}
 static void finish(GunnyServer.Room r,boolean ahead){for(int i=0;i<3000&&!r.battle.pendingNext&&!r.battle.over;i++){if(ahead)r.tickAhead();else r.tick();}if(!r.battle.pendingNext&&!r.battle.over)throw new AssertionError("unfinished");}
 static void checkFlight(GunnyServer server,int dungeon,int extra)throws Exception{
  var fast=room(server,true,dungeon,extra);var slow=room(server,false,dungeon,extra);
  ok(fast.batchShot&&fast.chunks.isEmpty(),"No shot block published at trigger, dungeon="+dungeon+" extra="+extra);
  for(int i=0;i<8;i++)fast.tick();
  ok(fast.chunks.isEmpty(),"No partial flight block published after initial windup");
  finish(fast,true);finish(slow,false);
  ok(Arrays.equals(finalState(server,fast),finalState(server,slow)),"Identical authoritative end state, including terrain, HP, RNG effects");
  ok(fast.chunkTerminal&&fast.chunks.size()==1,"One complete shot block ("+fast.chunks.get(0).length+" bytes)");
  var data=new DataInputStream(new ByteArrayInputStream(fast.chunks.get(0)));
  ok(data.readUnsignedByte()==4&&data.readBoolean(),"Complete-shot type and terminal flag");int frames=data.readUnsignedShort();ok(frames>8&&frames<=GunnyServer.Room.SHOT_BATCH_FRAMES,"All "+frames+" frames available before playback");
  var reply=new ByteArrayOutputStream();fast.chunkPacket(new DataOutputStream(reply),fast.members[1],0,fast.battle.turn,-1,-1);
  var in=new DataInputStream(new ByteArrayInputStream(reply.toByteArray()));ok(in.readUnsignedByte()==4&&in.readInt()==0&&in.readInt()==fast.chunks.get(0).length,"Same stage-4 TCP envelope, full batch in one response");
 }
 static void dungeon(GunnyServer s,int stage)throws Exception{
  var host=ReturnRoomCheck.player("dungeon"+stage);host.profile.experience=Progression.experienceForLevel(70);host.async=host.prefetch=host.delta=host.campaign=host.flock=host.tribal=host.tribal797=host.sulan7912=true;
  var r=s.new Room(host,6,2,-1,true);r.kind=3;r.dungeonStage=stage;r.strict=true;r.refreshWaiting();r.start();r.phase=1;
  // Skip the opening sequence only in the test fixture, not in production.
  r.battle.tribalFx=0;r.battle.botPhase=0;r.battle.active=0;r.battle.units[0].human=true;r.battle.pendingNext=false;r.battle.over=false;
  r.battle.units[0].angle=45;r.battle.units[0].power=50;r.battle.wind=0;r.battle.fire();
  ok(r.batchShot&&r.chunks.isEmpty(),"Dungeon stage "+stage+" starts an unpublished complete shot");
  for(int i=0;i<3000&&r.batchShot;i++)r.tickAhead();
  ok(!r.batchShot&&!r.chunks.isEmpty(),"Dungeon stage "+stage+" publishes after flight or bounded fallback");
  ok(r.chunks.get(0)[0]==4,"Dungeon stage "+stage+" uses complete flight block");
 }
 public static void main(String[] args)throws Exception{try(var s=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),new CloudStore.Memory())){s.clock.shutdownNow();checkFlight(s,0,0);checkFlight(s,0,2);dungeon(s,1);dungeon(s,2);dungeon(s,3);dungeon(s,6);
  var r=room(s,true,0,0);r.members[1].delta=false;r.chunkFrames=null;r.chunkBegin(3);ok(!r.batchShot,"Legacy/mixed room keeps streaming");
  var longShot=room(s,true,0,0);for(int i=0;i<GunnyServer.Room.SHOT_BATCH_FRAMES+5;i++)longShot.chunkFrame();ok(longShot.shotStreaming&&!longShot.batchShot&&!longShot.chunks.isEmpty(),"Long flight falls back instead of unbounded allocation");for(byte[] packet:longShot.chunks)if(packet.length>GunnyServer.Room.SHOT_BATCH_BYTES+4)throw new AssertionError("oversize");
 }}
}
