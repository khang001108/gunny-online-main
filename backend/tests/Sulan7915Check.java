public final class Sulan7915Check {
 static void ok(boolean v,String text){if(!v)throw new AssertionError(text);}
 public static void main(String[] args){
  Battle b=Sulan7912Check.raid(1,0);Sulan7912Check.red(b);ok(b.units[1].maxHp==16000,"Easy red HP doubled");ok(b.units[6].face==1,"Pea faces boss");
  b.units[1].hp=4000;b.beginTurn(1);b.tick();ok(b.units[12].npc==TribalRules.HEAL&&b.units[12].hp>0&&b.units[1].hp==4000,"Summon native totem without early healing");for(int i=0;i<10;i++)b.tick();ok(b.units[1].hp==4000,"No heal on each frame");
  b.beginTurn(0);b.sulanBurn[0]=1;b.units[0].x=20;int hp=b.units[0].hp;for(int i=0;i<90;i++)SulanRules.ambient(b);ok(b.units[0].hp==hp,"No damage by seconds");b.beginTurn(0);ok(b.units[0].hp==hp-90,"Damage once on own turn");
  SulanRules.ignite(b,b.units[0].x);Battle.Ball ice=new Battle.Ball();ice.owner=0;ice.freeze=true;ice.x=b.units[0].x*256;ice.y=Battle.targetY(b.units[0])*256;b.impact(ice);ok(b.sulanBurn[0]==0&&b.sulanFireCount==0,"Friendly/self ice extinguishes attached and ground fire");
  b.sulanBurn[0]=1;b.units[6].hp=0;b.beginTurn(0);ok(b.sulanBurn[0]==1,"Dead pea cannot extinguish automatically");
  b=Sulan7912Check.raid(3,0);Sulan7912Check.red(b);b.beginTurn(1);int turn=b.turn;while(b.turn==turn)b.tick();ok(b.shotSerial==12,"Four casts, three fireballs each, independent of party size");
  b.beginTurn(1);for(int i=0;i<65;i++)b.tick();ok(SulanRules.shake(b)&&SulanRules.stomping(b)&&SulanRules.cameraActor(b)==0,"Stomp shakes and pans toward player");
  System.out.println("PASS Sulan 7915 pea facing, 12 fireballs, turn burn, ice, totem heal, stomp and easy HP");
 }
}
