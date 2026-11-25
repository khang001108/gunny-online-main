public final class TribalDamageHF4Check {
 static int checks;
 static void ok(boolean v,String m){if(!v)throw new AssertionError(m);checks++;}
 public static void main(String[] args){
  int[] base={1080,1380,1800};
  for(int d=0;d<3;d++){
   ok(TribalRules.DAMAGE[d]==base[d],"Configured baseline damage at each difficulty");
   for(int kind:new int[]{TribalRules.ELITE,TribalRules.BROTHER,TribalRules.SISTER,TribalRules.SULAN,TribalRules.RAGE}){
    Battle b=TribalCheck.battle(kind==TribalRules.SULAN||kind==TribalRules.RAGE?4:1,d);b.tribalFx=0;b.tribalState=0;TribalRules.spawn(b,1,kind,300);b.units[0].hp=b.units[0].maxHp=30000;b.units[0].x=60;
    int weapon=kind==TribalRules.ELITE?TribalRules.SPEAR_SHOT:kind==TribalRules.RAGE?TribalRules.FIRE_SHOT:kind==TribalRules.SULAN?TribalRules.CHIEF_SHOT:TribalRules.SPIRIT_SHOT;
    TribalRules.projectile(b,1,0,weapon,0);
    ok(b.ballCount==1&&b.balls[0].damage==(kind==TribalRules.RAGE?base[d]*130/100:kind==TribalRules.SULAN?base[d]:base[d]*2),"Stage-specific projectile damage: "+kind);
   }
   Battle b=Sulan7912Check.raid(1,d);Sulan7912Check.red(b);b.units[0].x=20;b.sulanBurn[0]=1;int hp=b.units[0].hp;b.beginTurn(0);ok(hp-b.units[0].hp==6*(30+d*25),"HF5 red burn");
  }
  System.out.println("PASS "+checks+" HF5 damage calibration checks");
 }
}
