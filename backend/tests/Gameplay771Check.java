import java.io.*;
import java.net.*;
import java.nio.file.Path;

/** Computed growth never rewrites saved EXP, and live packets contain the current frame. */
public final class Gameplay771Check {
 static int checks;
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;System.out.println("PASS: "+label);}
 static GunnyServer.Session session(CloudStore.Memory store,String name,int level)throws Exception {
  String email=name+"@771.test";store.register(email,"test-password-123",name);
  var s=new GunnyServer.Session();s.profile=store.profile(store.login(email,"test-password-123").userId);s.id=s.profile.id;s.profile.experience=Progression.experienceForLevel(level);s.profile.characterChosen=true;s.actors=s.live=s.equipment=s.async=s.chunks=true;s.lastSeen=System.currentTimeMillis();return s;
 }
 public static void main(String[] args)throws Exception {
  for(String device:new String[]{"E72-2009","E6"})for(String ext:new String[]{"jar","jad"})check(GunnyServer.downloadFile("Gunny-"+device+"-Online-790."+ext),"Current "+device+" "+ext+" is downloadable");
  check(!GunnyServer.downloadFile("../gunny.env")&&!GunnyServer.downloadFile("Gunny-E6-Online-790.jar/../../gunny.env"),"Installer allowlist rejects traversal and private files");
  int prior=0;
  for(int level=1;level<=70;level++){
   int exp=Progression.experienceForLevel(level);check(Progression.level(exp)==level&&Progression.bonus(exp)==(level-1)*5,"Level "+level+" has exact cumulative EXP and +5 growth");
   if(level>1)check(Progression.level(exp-1)==level-1,"EXP below level "+level+" remains at previous level");
   var u=new Battle.Unit();u.experience=exp;u.weapon=0;ItemRules.apply(u,3);
   check(u.attackBonus==5+(level-1)*5&&u.defense==45+(level-1)*5&&u.luck==25+(level-1)*5,"Level "+level+" grows attack, defense and luck equally");
   check(u.maxHp==Progression.baseHp(exp)+u.defense-Progression.bonus(exp)&&u.maxEnergy==240+u.luck,"Level "+level+" growth feeds combat HP and energy");
   u.hp=100;u.energy=100;ItemRules.weapon(u,9);check(u.hp==100&&u.energy==100&&u.experience==exp,"Changing weapon preserves EXP and does not heal");
   ItemRules.stats(u,0);check(u.attackBonus==(level-1)*5&&u.defense==u.attackBonus&&u.luck==u.attackBonus,"Removing equipment preserves only level growth");
   check(Progression.power(3,0,exp)-Progression.power(3,0)==3*(level-1)*5,"Rating includes all three level bonuses");
  }
  check(Progression.bonus(-100)==0&&Progression.level(Integer.MAX_VALUE)==70,"Growth clamps invalid negative EXP and the level cap");
  var store=new CloudStore.Memory();var host=session(store,"Host",20);var guest=session(store,"Guest",45);
  String uuid=host.id,name=host.profile.name;int exp=host.profile.experience;
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();var room=server.new Room(host,1,0,-1);room.members[1]=guest;room.start();
   check(room.battle.units[0].experience==host.profile.experience&&room.battle.units[1].experience==guest.profile.experience,"Authoritative battle uses both human profiles' EXP");
   check(room.battle.units[1].attackBonus-room.battle.units[0].attackBonus==125,"Higher human level increases actual attack");
   for(int bot=0;bot<8;bot++){
    var bots=server.new Room(host,1,1,bot);bots.start();var u=bots.battle.units[2];
    check(Progression.level(u.experience)==Progression.BOT_LEVELS[bot]&&GunnyServer.experience(bots.avatars[2])==u.experience,"Bot "+bot+" has identical invite/room/combat level");
    if(bot>0)check(u.attackBonus>prior,"Higher bot level has stronger combat stats");prior=u.attackBonus;
   }
   room.battle.charge();room.battle.units[0].power=70;room.battle.fire();for(int t=0;t<30&&room.battle.ballCount==0;t++)room.battle.tick();
   check(room.battle.ballCount>0&&!room.battle.pendingNext,"Shot is still flying for live-delivery check");
   var bytes=new ByteArrayOutputStream();room.chunkPacket(new DataOutputStream(bytes),guest,0,1,-1,-1);var packet=new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
   check(packet.readUnsignedByte()==5&&packet.readInt()==room.battle.turn,"Live packet delivers the current battle frame before the shot finishes");
   check(room.chunks.isEmpty()&&room.chunkFrames==null&&room.replayStart==null,"Live rooms never accumulate replay buffers");
   long before=server.longPoll.state(room);room.battle.balls[0].x+=100;
   check(server.longPoll.state(room)!=before,"Projectile movement wakes TCP even when the turn clock is unchanged");
   before=server.longPoll.state(room);room.powTicks=50;
   check(server.longPoll.state(room)!=before,"POW animation also wakes the live stream");
   check(host.id.equals(uuid)&&host.profile.name.equals(name)&&host.profile.experience==exp,"Derived growth preserves stored identity, name and EXP");
   var old=session(store,"Legacy",45);old.actors=old.live=old.equipment=false;var legacy=server.new Room(old,1,0,-1);legacy.start();
   check(legacy.battle.units[0].experience==0&&legacy.battle.units[0].attackBonus==0,"Legacy fixtures retain their original wire and rules");
  }
  System.out.println("All "+checks+" gameplay 7.7.1 checks passed");
 }
}
