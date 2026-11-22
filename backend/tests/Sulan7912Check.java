import java.io.*;import java.net.*;import java.nio.file.*;import java.util.*;
/** Full tick-driven Sulan transitions, target counts and authoritative damage. */
public final class Sulan7912Check {
 static int checks;static void ok(boolean v,String text){if(!v)throw new AssertionError(text);checks++;}
 static Battle raid(int people,int diff){Battle b=TribalCheck.battle(4,diff);b.tribalState=0;for(int n=0;n<6;n+=2){b.units[n].hp=n/2<people?30000:0;b.units[n].maxHp=30000;b.units[n].human=true;}return b;}
 static void red(Battle b){b.units[1].hp=0;b.check();for(int i=0;i<SulanRules.TRANSFORM;i++)b.tick();ok(b.tribalPhase==2&&b.units[1].npc==15&&!b.over,"Red form resumes same battle");}
 static void turn(Battle b,int actor){b.beginTurn(actor);int old=b.turn;for(int t=0;t<1200&&b.turn==old&&!b.over;t++)b.tick();ok(b.turn!=old||b.over,"Sulan/bean turn terminates without a loading deadlock");}
 static void intro(){Battle b=raid(1,0);SulanRules.reset(b);b.tribalState=TribalRules.OPENING;int hp=b.units[0].hp;
  for(int i=0;i<60;i++){ok(SulanRules.cameraActor(b)==0,"First two seconds stay on player");b.tick();}
  ok(SulanRules.cameraActor(b)==1&&SulanRules.drop(b)==140,"Camera follows falling golden boss");for(int i=0;i<31;i++)b.tick();ok(SulanRules.shake(b)&&b.sulanLine==SulanRules.INTRO_LINE,"Landing shakes and speaks");
  while(b.tribalFx>0)b.tick();ok(b.active==0&&b.input()&&b.units[0].hp==hp,"Player starts after title and roar, no opening damage");
 }
 static void attacks(){for(int people=1;people<=3;people++){
  Battle b=raid(people,0);int shots=b.shotSerial;turn(b,1);ok(b.shotSerial-shots==people,"Gold throws once per alive player: "+people);ok(b.active==0,"Gold returns to first player");
  red(b);ok(b.units[6].npc==7&&b.units[6].hp==SulanRules.beanHp(0),"Transform summons one low-HP pea");shots=b.shotSerial;turn(b,1);ok(b.shotSerial-shots==12,"Hellfire has four volleys of three balls: "+people);ok(b.active==6,"Pea gets extinguishing turn after red boss");
  for(int n=0;n<6;n+=2)if(b.units[n].hp>0)b.sulanBurn[n]=150;b.sulanFireCount=1;b.sulanFireX[0]=60;b.sulanFireLife[0]=150;turn(b,6);ok(b.sulanFireCount==0&&Arrays.stream(b.sulanBurn).sum()==0,"Pea extinguishes ground and attached flames");ok(b.active==0,"Pea returns turn to team");
  int hp=b.units[6].hp;turn(b,1);ok(b.shotSerial-shots==12&&b.units[6].hp<hp,"Next red turn is all-position fury, including pea");
 }}
 static void damage(){for(int diff=0;diff<3;diff++){
  Battle tuned=raid(1,diff);red(tuned);tuned.units[1].power=1;turn(tuned,1);ok(diff==2?tuned.units[6].hp==0:tuned.units[6].hp>0,"Hard pea falls to fury; Easy/Normal survive a hit");
  Battle b=raid(1,diff);red(b);b.beginTurn(0);Battle.Ball p=new Battle.Ball();p.owner=0;p.damage=500;p.radius=1;p.x=b.units[6].x*256;p.y=Battle.targetY(b.units[6])*256;int before=b.units[6].hp;b.impact(p);ok(before-b.units[6].hp==100,"Pea receives exactly 20 percent friendly damage");
  b.sulanBurn[0]=180;before=b.units[0].hp;b.units[0].x=20;for(int t=0;t<30;t++)SulanRules.ambient(b);ok(before==b.units[0].hp,"Burn does not tick with render time");b.beginTurn(0);ok(before-b.units[0].hp==30+diff*25,"Burn damages on actor turn");
  b.units[0].x=b.units[6].x;int guarded=SulanRules.damage(b,flame(),0,400);b.units[0].x=20;ok(guarded==200&&SulanRules.damage(b,flame(),0,400)==400,"Standing near living pea halves red fire damage");
  b.units[6].hp=0;ok(SulanRules.damage(b,flame(),0,400)==400,"Dead pea cannot protect");
  b.units[1].power=1;turn(b,1);ok(!b.over,"Party survives on all difficulties");
  b.units[1].hp=0;b.check();ok(b.sulanLine==SulanRules.FAREWELL&&b.tribalState==TribalRules.DEAD&&!b.over,"Farewell precedes burned corpse and reward");for(int t=0;t<SulanRules.DEATH;t++)b.tick();ok(b.over&&b.winner==0,"Final death animation ends in victory");
 }
 Battle b=raid(1,0);red(b);b.beginTurn(0);for(int n=0;n<700;n++)SulanRules.ambient(b);ok(b.sulanStory==3&&b.input(),"Three story lines never interrupt player's controls");
 b=raid(1,0);red(b);b.beginTurn(0);b.sulanExternalClock=true;b.units[0].x=20;b.sulanBurn[0]=180;int hp=b.units[0].hp;for(int n=0;n<90;n++)b.tick();ok(b.units[0].hp==hp,"Batched input cannot accelerate the external burn clock");for(int n=0;n<30;n++)SulanRules.ambient(b);ok(b.units[0].hp==hp,"Wall clock no longer damages burn");b.beginTurn(0);ok(b.units[0].hp==hp-30,"Burn applies on actor turn only");
 }
 static Battle.Ball flame(){Battle.Ball p=new Battle.Ball();p.owner=1;p.weapon=TribalRules.FIRE_SHOT;return p;}
 static void wire()throws Exception{var store=new CloudStore.Memory();var host=Expansion780Check.session(store,"SulanWire",70);host.sulan7912=host.tribal=host.campaign=host.flock=true;
  try(var s=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){s.clock.shutdownNow();var r=s.new Room(host,6,2,-1,true);r.kind=3;r.dungeonStage=6;r.refreshWaiting();r.start();r.battle.sulanFireCount=2;r.battle.sulanFireX[0]=45;r.battle.sulanFireLife[0]=179;r.battle.sulanBurn[0]=155;
   var extended=new ByteArrayOutputStream();s.snapshot(new DataOutputStream(extended),r,0,host);host.sulan7912=false;var legacy=new ByteArrayOutputStream();s.snapshot(new DataOutputStream(legacy),r,0,host);ok(extended.size()-legacy.size()==32,"Only negotiated Sulan clients receive bounded state suffix");
  }
 }
 public static void main(String[] args)throws Exception{intro();attacks();damage();wire();System.out.println("PASS "+checks+" Sulan opening, two forms, 1-3 players, three salvos, pea, burn, story, death and wire checks");}
}
