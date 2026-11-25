import java.net.*;
import java.nio.file.*;
/** Pending end-of-turn acknowledgements cannot freeze an entire Sulan room forever. */
public final class SulanRecoveryHF20Check {
 static int checks;
 static void ok(boolean b,String msg){if(!b)throw new AssertionError(msg);checks++;}
 static GunnyServer.Room room(GunnyServer s,int stage)throws Exception {
  var host=ReturnRoomCheck.player("recovery"+stage);
  host.profile.experience=Progression.experienceForLevel(70);
  host.async=host.prefetch=host.delta=host.campaign=host.flock=host.tribal=host.tribal797=host.sulan7912=true;
  var r=s.new Room(host,6,2,-1,true);r.kind=3;r.dungeonStage=stage;r.strict=true;r.refreshWaiting();r.start();r.phase=1;
  r.battle.tribalFx=0;r.battle.tribalState=TribalRules.IDLE;r.battle.active=0;r.battle.units[0].human=true;r.battle.pendingNext=true;r.battle.over=false;
  host.lastSeen=System.currentTimeMillis();host.resolution=-1;
  return r;
 }
 public static void main(String[] args)throws Exception {
  try(var s=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),new CloudStore.Memory())){
   s.clock.shutdownNow();
   var r=room(s,6);int old=r.battle.turn;r.waitingSince=System.currentTimeMillis()-1000;r.tick();
   ok(r.battle.pendingNext&&r.battle.turn==old,"Normal playback receives time to complete");
   r.waitingSince=System.currentTimeMillis()-61000;r.tick();
   ok(!r.battle.pendingNext&&r.battle.turn==old+1,"Sulan does not wait forever");
   ok(r.battle.active==1,"Server selects the normal next actor");
   r=room(s,6);r.battle.tribalPhase=2;r.battle.active=6;TribalRules.spawn(r.battle,6,TribalRules.BEAN,140);r.battle.pendingNext=true;old=r.battle.turn;r.waitingSince=System.currentTimeMillis()-61000;r.tick();
   ok(r.battle.turn==old+1&&r.battle.active==0&&!r.battle.pendingNext,"Pea turn resumes the player after missing acknowledgement");
   r=room(s,4);old=r.battle.turn;r.waitingSince=System.currentTimeMillis()-61000;r.tick();
   ok(r.battle.pendingNext&&r.battle.turn==old,"Other encounters preserve existing ack rules");
  }
  System.out.println("PASS "+checks+" Sulan acknowledgement recovery checks");
 }
}
