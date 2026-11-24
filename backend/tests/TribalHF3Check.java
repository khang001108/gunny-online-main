/** HF3 behavioral tests: wiped waves, friendly pass-through, flight, red HP, heal cadence, dialogue, facing. */
public final class TribalHF3Check {
 static int checks;
 static void ok(boolean v,String s){if(!v)throw new AssertionError(s);checks++;}
 static void wipe(){for(int party=1;party<=3;party++){
  Battle b=TribalCheck.battle(1,0);for(int i=0;i<6;i+=2){b.units[i].hp=i/2<party?30000:0;b.units[i].maxHp=30000;}
  b.tribalState=TribalRules.IDLE;b.beginTurn(0);int shots=b.shotSerial;
  for(int i=0;i<b.unitCount;i++)if(b.units[i].team==1)b.units[i].hp=0;b.check();int goal=b.tribalGoal;
  for(int i=0;i<party;i++){b.next();ok(b.units[b.active].npc==0,"Wiped wave skips every enemy attack turn");}
  ok(b.active==0&&b.tribalGoal==goal&&TribalRules.alive(b,TribalRules.ELITE)&&TribalRules.alive(b,TribalRules.SPEAR)&&b.shotSerial==shots,"Respawn only; party immediately gets turn");
  for(int i=0;i<party;i++)b.next();ok(b.units[b.active].npc==TribalRules.SPEAR,"Next enemy round operates normally");
 }}
 static void shots(){Battle b=TribalCheck.battle(1,0);b.tribalState=TribalRules.IDLE;b.units[0].x=60;b.units[0].hp=b.units[0].maxHp=30000;b.units[1].x=300;b.units[1].face=-1;
  for(int i=3;i<b.unitCount;i++)if(b.units[i].npc==TribalRules.SPEAR)b.units[i].hp=0;
  TribalRules.spawn(b,3,TribalRules.SPEAR,285);b.beginTurn(1);int hp=b.units[0].hp,friend=b.units[3].hp;TribalRules.projectile(b,1,0,TribalRules.SPEAR_SHOT,0);
  Battle.Ball p=b.balls[0];ok(p.vy<0&&Battle.gravity(p)==20,"Warrior uses upward Bulan gravity");b.tick();ok(b.ballCount==1,"Friendly spear unit at muzzle does not intercept");int frames=1,min=p.y;while(b.ballCount>0&&frames<80){b.tick();min=Math.min(min,p.y);frames++;}
  ok(frames<=32&&b.units[0].hp<hp&&b.units[3].hp==friend&&min<210*256,"Real flight hits player, arcs and leaves allied soldier unharmed");
  for(int d:new int[]{40,150,240,400})ok(TribalRules.flightSteps(d)>=85&&TribalRules.flightSteps(d)<=150,"All tribal throws use Bulan flight duration");
 }
 static void finalBoss(){int[] hp={16000,40600,53200};for(int d=0;d<3;d++){
  Battle b=Sulan7912Check.raid(3,d);b.beginTurn(0);b.units[0].x=420;b.units[2].x=240;SulanRules.updateFacing(b);ok(b.units[1].face==1,"Gold faces player behind even with ally in front");b.units[0].x=60;SulanRules.updateFacing(b);ok(b.units[1].face==-1,"Gold faces left again");
  Sulan7912Check.red(b);ok(b.units[1].maxHp==hp[d]&&b.units[1].hp==hp[d],"Red HP doubled on all difficulties");b.beginTurn(0);b.units[0].x=430;SulanRules.updateFacing(b);ok(b.units[1].face==1,"Red follows player behind");
  b.units[1].hp=hp[d]/2;int initial=b.units[1].hp;
  for(int call=1;call<=10;call++){b.units[1].power=call;SulanRules.callHealTotem(b);ok(b.units[1].hp==initial+hp[d]*15/100*(call/5),"Exactly 15 percent max HP per five calls");ok(b.units[12].npc==TribalRules.HEAL&&b.units[12].hp>0,"Native totem alive");}
  b.units[1].hp=hp[d]-1;b.units[1].power=15;SulanRules.callHealTotem(b);ok(b.units[1].hp==hp[d],"Healing capped at max HP");
  b.sulanStory=0;b.sulanStoryClock=119;b.sulanTalk=0;SulanRules.ambient(b);ok(b.sulanLine==SulanRules.STORY1&&b.sulanSpeaker==6,"Story first line belongs to pea");b.sulanTalk=0;b.sulanStoryClock=119;SulanRules.ambient(b);ok(b.sulanLine==SulanRules.STORY2&&b.sulanSpeaker==6,"Awaken villagers line belongs to pea");
 }}
 public static void main(String[] args){wipe();shots();finalBoss();System.out.println("PASS "+checks+" HF3 wave skip, allied collision, flight, doubled HP, totem cadence, dialogue and facing checks");}
}
