import java.util.*;
public class TribalWavesCheck {
 static void ok(boolean v,String s){if(!v)throw new AssertionError(s);}
 static int live(Battle b,int kind){int n=0;for(var u:b.units)if(u.hp>0&&u.npc==kind)n++;return n;}
 public static void main(String[] args){
  var b=TribalCheck.battle(1,0);ok(live(b,TribalRules.SPEAR)==6&&live(b,TribalRules.ELITE)==1,"Initial six troops and one warrior");
  for(var u:b.units)if(u.hp>0&&u.npc==TribalRules.SPEAR)ok(u.x>b.units[1].x,"Troops spawn behind warrior facing party");
  TribalRules.wave(b);ok(live(b,TribalRules.SPEAR)==6&&live(b,TribalRules.ELITE)==1,"Six troops plus one warrior fit");
  for(int n=0;n<4;n++)TribalRules.wave(b);ok(live(b,TribalRules.SPEAR)==6,"Troop cap");
  int killed=0;for(var u:b.units)if(killed<3&&u.hp>0&&u.npc==TribalRules.SPEAR){u.hp=0;killed++;}b.check();TribalRules.wave(b);ok(live(b,TribalRules.SPEAR)==6,"Refill only available places");
  b.active=1;b.ballCount=0;b.tribalClock=72;TribalRules.tickAI(b);ok(b.ballCount==3,"Three shots");
  for(int i=1;i<3;i++){ok(b.balls[i].x==b.balls[0].x&&b.balls[i].y==b.balls[0].y,"Same muzzle, no parallel bundle");ok(b.balls[i].vx!=b.balls[i-1].vx&&b.balls[i].vy!=b.balls[i-1].vy,"Angular spread");}
  ok(b.balls[1].vy<0&&Battle.gravity(b.balls[1])>0,"Upward launch then gravity");
  b=TribalCheck.battle(1,0);b.units[0].x=40;b.active=3;b.botActing=true;
  for(int i:new int[]{3,5}){var u=b.units[i];u.x=102;u.y=238;u.energy=240;b.active=i;b.move(-1);}
  ok(b.units[3].x==101&&b.units[5].x==101,"Tribal followers share position");
  b.botActing=false;b.units[0].x=80;b.tribalClock=72;int tribalHp=b.units[0].hp;TribalRules.tickAI(b);ok(tribalHp-b.units[0].hp==1080,"Overlapping tribesmen each deal their melee hit");
  b=new Battle(TribalCheck.flat);b.start(true);b.teams=true;b.dungeonStage=1;b.unitCount=4;b.units[0].hp=30000;b.units[0].x=40;b.units[0].y=238;b.units[2].hp=0;
  for(int i:new int[]{1,3}){AntCaveRules.spawn(b,i,1,0);b.units[i].x=102;b.units[i].y=238;b.units[i].face=-1;b.stepAnt(i);}
  ok(b.units[1].x==101&&b.units[3].x==101,"Ants share position");
  for(int i:new int[]{1,3}){b.units[i].x=65;b.units[i].attackTicks=18;}int hp=b.units[0].hp;b.botWait=56;b.antGroupTurn(1);ok(hp-b.units[0].hp==180,"Overlapping ants each deal their melee hit");
  b.dungeonStage=0;ok(b.blockedStep(66,65,1,23),"Normal battle collision preserved");
  System.out.println("PASS wave size/cap/refill, rear spawn, arcing spread, NPC overlap and stacked melee damage");
 }
}
