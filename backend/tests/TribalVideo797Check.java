import java.net.*;import java.nio.file.*;import java.util.*;
/** Behavioral checks against observed campaign structure and attack identities. */
public final class TribalVideo797Check {
 static int checks;static void ok(boolean v,String s){if(!v)throw new AssertionError(s);checks++;}
 static Battle raid(int stage){var b=TribalCheck.battle(stage,0);b.units[0].maxHp=b.units[0].hp=30000;return b;}
 static void turn(Battle b){int start=b.turn;for(int i=0;i<500&&b.turn==start&&!b.over;i++)b.tick();ok(b.turn!=start||b.over,"NPC action terminates");}
 static void npcOnly(Battle b,int keep){for(int i=1;i<b.unitCount;i++)if(i!=keep&&b.units[i].npc>0){b.units[i].hp=0;b.units[i].dead=true;}b.active=keep;}
 static void attacks(){
  var b=raid(1);npcOnly(b,1);b.units[1].npc=TribalRules.SPEAR;b.units[1].x=200;b.units[0].x=52;turn(b);ok(b.shotSerial==0&&b.units[0].hp==30000,"Melee tribesman cannot damage a distant target");
  b=raid(1);npcOnly(b,1);b.units[1].npc=TribalRules.SPEAR;b.units[1].x=400;b.units[0].x=52;for(int i=0;i<47;i++)b.tick();ok(b.units[1].attackTicks==0,"Distant melee troop does not play a stabbing attack");
  b=raid(1);npcOnly(b,1);b.units[1].npc=TribalRules.SPEAR;b.units[1].x=90;b.units[0].x=52;int hp=b.units[0].hp;turn(b);ok(b.units[0].hp<hp&&b.shotSerial==0,"Close tribesman actually stabs");
  b=raid(1);int elite=-1;for(int i=0;i<13;i++)if(b.units[i].npc==TribalRules.ELITE)elite=i;npcOnly(b,elite);for(int i=0;i<73;i++)b.tick();ok(b.ballCount==3&&Arrays.stream(b.balls,0,b.ballCount).allMatch(x->x.weapon==TribalRules.SPEAR_SHOT),"Purple warrior launches three spear payloads");hp=b.units[0].hp;turn(b);ok(b.units[0].hp<hp,"Thrown NPC spear follows physics and hits");
  b=raid(2);b.active=3;for(int i=0;i<75;i++)b.tick();ok(b.ballCount==1&&b.balls[0].weapon==TribalRules.SPIRIT_SHOT,"Bulan uses his own thrown staff");
  b=raid(4);b.tribalState=0;int shots=b.shotSerial;turn(b);ok(b.shotSerial-shots==1,"Golden Sulan throws once at the only player");
  b=raid(4);b.units[1].hp=0;b.check();for(int i=0;i<SulanRules.TRANSFORM;i++)b.tick();b.beginTurn(1);shots=b.shotSerial;turn(b);ok(b.shotSerial-shots==3,"Red Sulan launches three sequential flame salvos for one player");
 }
 static void skills(){
  var b=raid(2);b.active=1;for(int i=0;i<75;i++)b.tick();ok(TribalRules.alive(b,TribalRules.FATIGUE)&&b.ballCount==0,"Bulan casts a fatigue totem without also firing");
  b=raid(2);b.active=3;b.units[3].power=2;for(int i=0;i<75;i++)b.tick();ok(TribalRules.alive(b,TribalRules.CHAIN),"Other Bulan casts a chain totem");
  b=raid(4);b.tribalState=0;b.tribalRound=2;turn(b);ok(!TribalRules.alive(b,TribalRules.HEAL),"Sulan uses the requested spear cycle without old healing totems");
  b=raid(4);b.units[1].hp=0;b.check();int time=b.countdown;for(int i=0;i<SulanRules.TRANSFORM-1;i++)b.tick();ok(b.countdown==time&&!b.input()&&b.ballCount==0,"Transformation is a real frozen transition");b.tick();ok(b.tribalFx==0&&!b.over,"Transformation resumes the same battle");
  ok(TribalRules.alive(b,TribalRules.BEAN),"Rebirth brings out the pea helper");SulanRules.ignite(b,b.units[0].x);b.beginTurn(6);turn(b);ok(b.tribalBeanSaved&&b.sulanFireCount==0,"Pea extinguishes flames in its own turn");
  b.units[1].hp=0;b.check();for(int i=0;i<SulanRules.DEATH;i++)b.tick();ok(b.over&&b.winner==0,"Final boss death wins after the farewell and native death sequence");
  b=raid(2);b.units[0].hp=0;b.check();ok(b.over&&b.winner==1,"Party wipe loses Bulan encounter");
 }
 static void campaign()throws Exception{
  var store=new CloudStore.Memory();var host=Expansion780Check.session(store,"Video797",70);host.tribal=host.flock=host.campaign=true;
  host.tribal797=false;try{TribalDungeon.eligible(host,0);throw new AssertionError("Old client accepted");}catch(IllegalArgumentException expected){ok(expected.getMessage().contains("7.9.13"),"Unsupported NPC clients receive an update message");}host.tribal797=true;
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();
   for(int stage:new int[]{3,4,6}){
    var r=server.new Room(host,6,2,-1,true);r.kind=3;r.strict=true;r.dungeonStage=stage;r.refreshWaiting();r.start();var b=r.battle;b.sulanExternalClock=false;b.deferNext=false;b.tribalFx=0;b.units[0].god=true;b.units[0].attackBonus=8000;b.cheatAim=stage!=6;
    // Aim a real ballistic arc over reinforcements; straight homing is intercepted by the new front line.
    int ticks=0;while(!b.over&&ticks++<100000){if(b.humanTurn()&&!b.busy()&&b.tribalFx==0){if(stage==6)b.aim();else b.units[0].power=70;b.fire();}b.tick();}
    if(!b.over||b.winner!=0)System.out.println("DEBUG stage="+stage+" ticks="+ticks+" turn="+b.turn+" active="+b.active+" phase="+b.tribalPhase+" round="+b.tribalRound+" fx="+b.tribalFx+" outcome="+b.tribalOutcome+" hp="+b.units[1].hp+" bean="+TribalRules.alive(b,TribalRules.BEAN)+" saved="+b.tribalBeanSaved+" goal="+b.tribalGoal);ok(b.over&&b.winner==0,"Playable full encounter with movement, AI, impacts and death: "+stage);
    ok(b.turn<=200,"Win within video turn limit");
    for(int y=0;y<264;y++)for(int x=0;x<480;x++)if((r.ground.hard[(y*480+x)>>3]&255)!=255)throw new AssertionError("Tribal terrain must be indestructible");checks++;
   }
  }
 }
 public static void main(String[] args)throws Exception{attacks();skills();campaign();System.out.println("PASS: "+checks+" video campaign, real attack, capability and full encounter checks");}
}
