/** HP progression, enemy scaling at every tier and identical PvP/PvE flight cadence. */
public final class BalanceHF16Check {
 static int checks;
 static void ok(boolean pass,String label){if(!pass)throw new AssertionError(label);checks++;}
 public static void main(String[] args)throws Exception{
  if(args.length>0)TribalConfig.load();
  int previous=0;
  for(int level=1;level<=70;level++){
   int xp=Progression.experienceForLevel(level),hp=Progression.baseHp(xp);
   ok(hp>previous,"HP grows at level "+level);previous=hp;
  }
  ok(previous==15000&&Progression.baseHp(0)==2000,"Base HP endpoints");
  Battle b=new Battle(TribalCheck.flat);b.start(true);
  for(int stage=0;stage<=6;stage++){b.dungeonStage=stage;ok(b.flightSubsteps()==5,"Shared flight cadence "+stage);}
  int[][] antHp={{450,600,900},{700,900,1200},{6000,8000,10500}};
  int[][] antDamage={{90,120,150},{90,120,150},{240,300,420}};
  for(int d=0;d<3;d++){
   for(int kind=1;kind<=3;kind++){
    b.dungeonStage=kind==3?2:1;AntCaveRules.spawn(b,1,kind,d);
    Battle.Unit u=b.units[1];ok(u.hp==antHp[kind-1][d]*2&&u.maxHp==u.hp,"Ant HP");
    ok(Battle.baseDamage(u.weapon)+u.attackBonus==antDamage[kind-1][d]*2,"Ant damage");
   }
   for(int stage=3;stage<=6;stage++){
    b.dungeonStage=stage;b.dungeonDifficulty=d;
    int factor=stage==6?1:2;ok(TribalRules.enemyDamage(b)==TribalRules.DAMAGE[d]*factor,"Tribal damage tier");
    for(int kind=TribalRules.SPEAR;kind<=TribalRules.RAGE;kind++){
     // Sulan and Rage are exclusive to the final encounter.
     if(stage<6&&kind>=TribalRules.SULAN)continue;
     TribalRules.spawn(b,1,kind,300);Battle.Unit u=b.units[1];
     int base=(kind==TribalRules.RAGE&&d==0?8000:TribalRules.HP[kind-TribalRules.SPEAR])*(100+d*45)/100*(kind==TribalRules.RAGE?2:1);
     ok(u.hp==base*(kind==TribalRules.BEAN?1:factor)&&u.maxHp==u.hp,"Tribal spawn HP");
     ok(u.maxHp<=65535,"HP fits unsigned snapshot field");
    }
   }
  }
  System.out.println("PASS "+checks+" balance checks; base damage "+TribalRules.DAMAGE[0]+"/"+TribalRules.DAMAGE[1]+"/"+TribalRules.DAMAGE[2]);
 }
}
