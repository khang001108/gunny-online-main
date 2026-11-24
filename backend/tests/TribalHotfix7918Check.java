import java.net.InetSocketAddress;
import java.nio.file.Path;
/** Regression checks for the three-stage Tà Thần hotfix. */
public final class TribalHotfix7918Check {
 static int checks;
 static void ok(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
 static int troops(Battle b){int n=0;for(int i=0;i<b.unitCount;i++)if(b.units[i].npc==TribalRules.SPEAR&&b.units[i].hp>0)n++;return n;}
 static void stageOne(Battle b){
  ok(troops(b)==6,"Initial six troops");
  while(b.tribalFx>0){b.tick();for(int i=0;i<b.unitCount;i++){Battle.Unit u=b.units[i];if(u.hp>0&&u.npc==TribalRules.SPEAR)ok(u.y==b.ground.surface(u.x,5),"Opening troops follow the ground");}}
  int killed=0;
  while(killed<20){
   ok(troops(b)<=6&&b.units[1].hp>0,"Six troops maximum, warrior kept alive");
   for(int i=0;i<b.unitCount;i++){Battle.Unit u=b.units[i];if(u.npc==TribalRules.SPEAR&&u.hp>0){u.hp=0;killed++;}}
   b.check();ok(b.tribalGoal==20-killed,"Only actual troop deaths count");
   if(killed<20){ok(!b.over&&b.tribalOutcome<0,"No early win");TribalRules.wave(b);}
  }
  ok(b.units[1].hp>0&&b.tribalOutcome==0,"20 troops is sufficient even with a living warrior");
  while(b.tribalFx>0)b.tick();ok(b.over&&b.winner==0,"Results after 20th kill");
 }
 static void transform(GunnyServer.Room r,GunnyServer.Session host){
  Battle b=r.battle;r.phase=1;b.tribalFx=0;b.tribalState=TribalRules.IDLE;b.beginTurn(0);r.inputTicks=120;
  int turn=b.turn;b.units[1].hp=0;b.check();
  ok(b.tribalPhase==1&&b.units[1].npc==TribalRules.SULAN&&b.units[1].hp==0,"Gold death plays before red spawn");
  for(int t=0;t<SulanRules.GOLD_END;t++){r.tick();ok(b.units[6].npc!=TribalRules.BEAN,"No premature pea");}
  r.tick();ok(b.tribalPhase==2&&b.units[1].npc==TribalRules.RAGE&&b.units[1].hp==b.units[1].maxHp,"Red spawns only after death sequence");
  while(SulanRules.TRANSFORM-b.tribalFx<=SulanRules.RED_END){ok(b.units[6].npc!=TribalRules.BEAN,"Red born sequence precedes pea");r.tick();}
  while(b.tribalFx>0)r.tick();ok(b.units[6].npc==TribalRules.BEAN&&b.units[6].hp>0,"Pea arrives after red form");
  ok(b.pendingNext&&b.turn==turn&&b.active==0,"Terminal transform stays on original turn until acknowledgement");
  host.resolution=turn;r.tick();ok(b.turn==turn+1&&b.active==0&&b.input()&&!b.pendingNext&&r.inputTicks==0&&b.countdown==b.turnLimit,"Fresh controllable player turn");
  for(int i=0;i<30;i++)r.tick();ok(r.chunkTurn!=b.turn,"No idle playback flood in the fresh turn");
  int time=b.countdown,x=b.units[0].x;
  r.command(host,host.sequence+1,6,16|2);ok(b.countdown==time-1&&r.inputTicks==1&&b.units[0].x!=x,"Movement and countdown work without a buff");
  // A direct 500 damage hit must not be reduced to 67 by the old 640 defense.
  Battle.Ball shot=new Battle.Ball();shot.owner=0;shot.damage=500;shot.radius=1;shot.x=b.units[1].x*256;shot.y=Battle.targetY(b.units[1])*256;
  int hp=b.units[1].hp;b.impact(shot);ok(hp-b.units[1].hp==500,"Red boss receives full direct damage");
  SulanRules.callAdds(b);ok(SulanRules.addCount(b,TribalRules.SPEAR)==3,"Red summons only three troops");
 }
 public static void main(String[] args)throws Exception{
  var store=new CloudStore.Memory();var host=Expansion780Check.session(store,"TribalHotfix",70);host.tribal=host.flock=host.campaign=true;host.live=false;host.prefetch=true;
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();var room=server.new Room(host,6,2,-1,true);room.kind=3;room.dungeonStage=3;room.selectedBots[2]=1;room.selectedBots[4]=3;room.refreshWaiting();
   ok(room.avatars[2].bot&&room.avatars[4].bot&&!room.avatars[2].name.equals("Đang chờ"),"Invited bots retained in waiting room");
   room.start();ok(room.battle.units[2].hp>0&&room.battle.units[4].hp>0&&!room.battle.units[2].human&&!room.battle.units[4].human&&room.avatars[2].bot&&room.avatars[4].bot,"Bots are real loading and battle participants");
   room.battle.deferNext=false;stageOne(room.battle);
   var finalRoom=server.new Room(host,6,2,-1,true);finalRoom.kind=3;finalRoom.dungeonStage=6;finalRoom.refreshWaiting();finalRoom.start();transform(finalRoom,host);
  }
  System.out.println("PASS "+checks+" Tà Thần hotfix assertions (bots, grounded spawn, 20 kills, transformation, timer, damage and adds)");
 }
}
