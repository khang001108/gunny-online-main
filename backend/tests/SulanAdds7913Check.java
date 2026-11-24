import java.util.*;
/** Real AI ticks: summon cadence, bounded roster, target selection and turn order. */
public final class SulanAdds7913Check {
 static int checks;
 static void ok(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
 static Battle raid(){Battle b=Sulan7912Check.raid(1,0);b.units[0].hp=b.units[0].maxHp=300000;return b;} // isolate long reinforcement cadence from HF4 increased damage
 static void finish(Battle b){int turn=b.turn;for(int t=0;t<1300&&b.turn==turn&&!b.over;t++)b.tick();ok(b.turn!=turn&&!b.over,"NPC action terminates");}
 static void boss(Battle b){b.beginTurn(1);finish(b);}
 static void count(Battle b,int melee,int elites){ok(SulanRules.addCount(b,TribalRules.SPEAR)==melee,"Correct tribal troop count: "+melee);ok(SulanRules.addCount(b,TribalRules.ELITE)==elites,"Correct warrior count: "+elites);}
 static void cadence(){
  Battle b=raid();count(b,0,0);boss(b);count(b,0,0);ok(b.units[1].power==1&&b.tribalWave==0,"First boss turn does not call reinforcements");
  for(int n=0;n<3;n++){b.beginTurn(0);for(int t=0;t<15;t++)b.tick();}ok(b.units[1].power==1&&b.tribalWave==0,"Player turns do not advance boss summon cadence");
  b.beginTurn(1);for(int t=0;t<100;t++)b.tick();count(b,0,0);finish(b);count(b,3,1);ok(b.tribalWave==1&&b.active==SulanRules.firstAdd(b,TribalRules.SPEAR,-1),"Summon finishes after second boss attack, then troop group acts");
  int elite=SulanRules.firstAdd(b,TribalRules.ELITE,-1);finish(b);ok(b.active==elite,"Troop group is scheduled once, then warrior");int shots=b.shotSerial;finish(b);ok(b.shotSerial-shots==1&&b.active==0,"Warrior uses original three spears and returns turn to player");
  boss(b);count(b,3,1);ok(b.tribalWave==1,"Third boss turn does not summon");
  int troop=SulanRules.firstAdd(b,TribalRules.SPEAR,-1);b.units[troop].hp=321;int x=b.units[troop].x;boss(b);count(b,3,1);ok(b.units[troop].hp==321&&b.units[troop].x==x,"Fourth turn preserves surviving troops without healing or relocation");ok(b.tribalWave==2,"One call per second boss turn");
  b.units[troop].hp=0;b.units[elite].hp=0;boss(b);count(b,2,0);boss(b);count(b,3,1);ok(b.tribalWave==3,"Sixth turn refills only missing units");
 }
 static void red(){
  Battle b=raid();boss(b);boss(b);int old=SulanRules.firstAdd(b,TribalRules.SPEAR,-1);b.units[old].hp=222;Sulan7912Check.red(b);count(b,3,1);ok(b.units[old].hp==222&&b.tribalWave==0&&b.units[1].power==0,"Transform keeps gold survivors and resets red boss cadence");
  int beanHp=b.units[6].hp;boss(b);count(b,3,1);boss(b);count(b,3,2);ok(b.units[6].npc==TribalRules.BEAN&&b.units[6].hp<beanHp&&b.units[6].hp>0,"Red reinforcements never overwrite the living pea");ok(b.active==6,"Pea keeps immediate turn after red Sulan");
  finish(b);ok(b.active==SulanRules.firstAdd(b,TribalRules.SPEAR,-1),"After extinguishing, the full troop group acts");finish(b);
  int firstElite=SulanRules.firstAdd(b,TribalRules.ELITE,-1);ok(b.active==firstElite,"First red warrior acts");finish(b);int second=SulanRules.firstAdd(b,TribalRules.ELITE,firstElite);ok(b.active==second,"Second red warrior acts");finish(b);ok(b.active==0,"Complete enemy team returns to player without looping");
  b.units[6].hp=0;boss(b);boss(b);count(b,3,2);ok(b.units[6].npc==TribalRules.BEAN&&b.units[6].hp==0,"Calls reserve pea slot even after it dies");
  for(int n=0;n<12;n++)boss(b);count(b,3,2);ok(b.unitCount==13,"Repeated calls keep the original bounded roster");
 }
 static void positions(){for(int face:new int[]{-1,1}){
  Battle b=raid();b.units[1].face=face;b.units[1].x=240;b.tribalPhase=2;b.units[1].power=2;SulanRules.callAdds(b);count(b,3,2);
  for(int slot:SulanRules.ADD_SLOTS){Battle.Unit u=b.units[slot];if(u.hp<=0)continue;ok((u.x-b.units[1].x)*face>0,"Spawn is in front of boss for either facing");ok(u.face==face&&u.y==238&&u.x>=18&&u.x<=462,"Correct facing, ground and world bounds");for(int i=0;i<6;i+=2)if(b.units[i].hp>0)ok(Math.abs(b.units[i].x-u.x)>=18,"No spawn on a player");}
  ok(b.units[6].npc==0&&b.units[6].maxHp==0,"Pea slot is reserved before transformation");
 }
  Battle party=Sulan7912Check.raid(3,0);Sulan7912Check.red(party);party.units[1].power=2;SulanRules.callAdds(party);count(party,3,2);
  for(int slot:SulanRules.ADD_SLOTS)if(party.units[slot].hp>0){Battle.Unit u=party.units[slot];ok(u.x<party.units[1].x,"Full party still spawns every reinforcement in front");for(int ally:new int[]{0,2,4,6})ok(Math.abs(party.units[ally].x-u.x)>=18,"No spawn on any of three players or pea");}
 }
 static void targets(){
  Battle b=raid();Sulan7912Check.red(b);TribalRules.spawn(b,3,TribalRules.SPEAR,b.units[6].x+24);ok(TribalRules.target(b,3)==6,"Melee troop selects nearby pea");int hp=b.units[6].hp;b.beginTurn(3);finish(b);ok(b.units[6].hp<hp,"Melee troop actually damages pea");
  b=raid();Sulan7912Check.red(b);TribalRules.spawn(b,3,TribalRules.SPEAR,b.units[0].x+24);ok(TribalRules.target(b,3)==0,"Melee troop selects nearby player");hp=b.units[0].hp;b.beginTurn(3);finish(b);ok(b.units[0].hp<hp,"Melee troop actually damages player");
  b=raid();Sulan7912Check.red(b);TribalRules.spawn(b,3,TribalRules.ELITE,200);TribalRules.spawn(b,5,TribalRules.SPEAR,188);b.units[0].x=20;ok(TribalRules.target(b,3)==6,"Warrior targets nearby pea");hp=b.units[6].hp;b.beginTurn(3);b.tribalClock=72;TribalRules.tickAI(b);ok(b.ballCount==3&&b.balls[0].owner==3,"Reinforced warrior uses its own original spear payloads");b.tick();ok(b.ballCount>0,"Friendly troops do not stop warrior's spears at launch");finish(b);ok(b.units[6].hp<hp,"Warrior spear can hit pea through allied troops");
  b.units[6].hp=0;ok(TribalRules.target(b,3)==0,"Dead pea is ignored and warrior retargets player");hp=b.units[0].hp;b.beginTurn(3);finish(b);ok(b.units[0].hp<hp,"Warrior actually damages player");
  b.units[1].hp=0;b.check();for(int n=0;n<SulanRules.DEATH;n++)b.tick();ok(b.over&&b.winner==0,"Final Sulan death still wins with reinforcements alive");
 }
 public static void main(String[] args){cadence();red();positions();targets();System.out.println("PASS "+checks+" boss reinforcement cadence, roster, placement, turns, pea/player targets and real damage checks");}
}
