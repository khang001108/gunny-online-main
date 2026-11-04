import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/** Complete-shot packet stages and receipt barrier, without real cloud credentials. */
public final class ShotPacket650Check {
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS: "+s);}
 static byte[] packet(GunnyServer.Room r,GunnyServer.Session viewer,int buffered)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();r.shotPacket(new DataOutputStream(bytes),viewer,0,buffered);return bytes.toByteArray();}
 public static void main(String[] args)throws Exception{
  CloudStore.Memory store=new CloudStore.Memory();store.register("a@packet.test","test-password-123","A");store.register("b@packet.test","test-password-123","B");
  try(GunnyServer server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   GunnyServer.Session a=new GunnyServer.Session(),b=new GunnyServer.Session();GunnyServer.Session[] members={a,b};for(int i=0;i<2;i++){GunnyServer.Session s=members[i];s.id=store.login((i==0?"a":"b")+"@packet.test","test-password-123").userId;s.profile=store.profile(s.id);s.async=s.modern=s.replay=true;s.lastSeen=System.currentTimeMillis();}
   GunnyServer.Room r=server.new Room(a,1,0,-1);r.members[1]=b;r.refreshWaiting();r.start();r.phase=1;Arrays.fill(r.ground.solid,false);Arrays.fill(r.ground.solid,170*480,220*480,true);for(int i=0;i<2;i++){r.battle.units[i].y=170;r.battle.units[i].slope=0;}r.battle.units[0].angle=45;r.battle.units[0].power=20;
   ok(packet(r,b,-1)[0]==0,"No shot uses the normal turn snapshot");r.battle.charge();r.battle.units[0].power=20;r.battle.fire();ok(r.replayStart!=null&&r.replayTurn==1,"Shot observer captures authoritative launch state");byte[] early=packet(r,b,-1);ok(early.length==1&&early[0]==1,"Resolving shot exposes no partial projectile or crater data");
   for(int i=0;i<1500&&!r.battle.pendingNext&&!r.battle.over;i++)r.tick();ok(r.battle.pendingNext&&r.ground.craters.size()>0,"Server finishes physics and craters before publishing shot");
   DataInputStream in=new DataInputStream(new ByteArrayInputStream(packet(r,b,-1)));ok(in.readUnsignedByte()==2,"Completed shot publishes batch stage");int pre=in.readInt();byte[] start=new byte[pre];in.readFully(start);int end=in.readInt();byte[] result=new byte[end];in.readFully(result);ok(pre>100&&end>100&&in.available()==0,"Batch has exactly one launch and one final snapshot");ok(new DataInputStream(new ByteArrayInputStream(start)).readInt()==1&&new DataInputStream(new ByteArrayInputStream(result)).readInt()==1,"Both batch snapshots belong to the same held turn");
   byte[] cached=packet(r,b,1);ok(cached.length==1&&cached[0]==3,"Buffered receiver only gets a heartbeat instead of repeated terrain data");r.tick();ok(r.battle.turn==1,"Delivery alone does not start another turn");a.resolution=1;r.tick();ok(r.battle.turn==1,"Shooter completion alone does not release peer timer");b.resolution=1;r.tick();ok(r.battle.turn==2&&r.battle.countdown==r.battle.turnLimit,"All playback receipts release full next-turn timer");ok(packet(r,b,1)[0]==0,"New turn cannot replay the previous turn's shot");
  }System.out.println("All 6.5.0 shot packet checks passed");
 }
}
