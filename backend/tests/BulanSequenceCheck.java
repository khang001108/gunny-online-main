import java.net.*;import java.nio.file.*;
public class BulanSequenceCheck {
 static void ok(boolean v,String msg){if(!v)throw new AssertionError(msg);}
 public static void main(String[] args)throws Exception {
  var store=new CloudStore.Memory();var host=Expansion780Check.session(store,"Bulan sequence",70);host.tribal=host.tribal797=host.campaign=host.flock=true;
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();var room=server.new Room(host,6,2,-1,true);room.kind=3;room.strict=true;room.dungeonStage=4;room.refreshWaiting();room.start();var b=room.battle;
   ok(TribalRules.introActor(b)==0,"Opening starts on player");int turns=b.turn;
   for(int i=0;i<TribalRules.BULAN_LEFT_AT+1;i++)b.tick();ok(TribalRules.introActor(b)==1&&b.botPhase==TribalRules.BULAN_LEFT,"Brother speaks first");
   while(TribalRules.BULAN_INTRO_END-b.tribalFx<TribalRules.BULAN_RIGHT_AT+1)b.tick();ok(TribalRules.introActor(b)==3&&b.botPhase==TribalRules.BULAN_INTRO,"Sister speaks next");
   while(b.tribalFx>0)b.tick();ok(b.turn==turns&&b.active==3,"Intro pauses turns then sister begins");
   int[][] expected={{1,10,3,4},{2,4,1,10}};
   for(int actor:new int[]{3,1})for(int cycle=0;cycle<4;cycle++){
    TribalRules.setup(b,2,1,false);b.tribalFx=0;b.active=actor;b.units[actor].power=cycle;b.units[0].hp=b.units[0].maxHp=30000;
    for(int i=0;i<31;i++)TribalRules.tickAI(b);
    int action=expected[actor==3?0:1][cycle];ok(TribalRules.bulanAction(b.units[actor])==action,"Four-action cycle "+actor+":"+cycle);
    int level=cycle%3+1;ok(b.units[actor].x==TribalRules.platformX(b.units[actor].npc)&&b.units[actor].y==TribalRules.platformY(b,b.units[actor].npc,level),"Lands on correct column tier");
    for(int i=31;i<87;i++)TribalRules.tickAI(b);
    if(action==1){ok(b.ballCount==1&&b.balls[0].weapon==TribalRules.SPIRIT_SHOT,"One native curved spear");ok(b.balls[0].vy<0,"Spear arcs upwards");}
    if(action==2)ok(TribalRules.alive(b,TribalRules.FATIGUE),"Fatigue totem");
    if(action==3){ok(TribalRules.alive(b,TribalRules.CHAIN),"Chain totem");b.active=0;int angle=b.units[0].angle;b.adjustAngle(8);ok(angle==b.units[0].angle,"All players angle lock");b.active=actor;}
    if(action==4){int adds=0;for(var u:b.units)if(u.hp>0&&u.npc==TribalRules.ELITE){adds++;ok(u.y==TribalRules.platformY(b,b.units[actor].npc,4),"Warrior on top tier");}ok(adds==1,"Summon exactly one warrior");}
    if(action==10)ok(b.units[0].hp<30000&&TribalRules.swordVisible(b),"Ancient sword hits from below and remains visible");
   }
   for(int d=0;d<3;d++){TribalRules.setup(b,2,d,false);TribalRules.summon(b,TribalRules.FATIGUE,178);b.beginTurn(0);ok(b.units[0].energy==Math.max(0,b.units[0].maxEnergy-TribalRules.fatigue(b)),"Difficulty scales fatigue");}
  }
  System.out.println("PASS Bulan opening, supported tiers, four-turn skill cycles, curved spear, ancient sword, summons and difficulty effects");
 }
}
