import java.util.*;
/** Weapon rank values and Ant Cave card rewards, with authoritative memory-store receipts. */
public final class BalanceHF18Check {
 static int checks;
 static void ok(boolean b,String msg){if(!b)throw new AssertionError(msg);checks++;}
 public static void main(String[] args)throws Exception{
  int[] ranks=new int[ForgeRules.SLOTS];int[] look=new int[5];
  for(int rank=0;rank<=15;rank++)for(int weapon=0;weapon<10;weapon++){
   ranks[weapon]=rank;int[] b=ForgeRules.equipped(ranks,0,1,weapon,0,look);
   ok(b[0]==rank*100,"Weapon bonus");
   Battle.Unit u=new Battle.Unit();u.weapon=weapon;
   ForgeRules.apply(u,b,ranks);ItemRules.apply(u,1);
   ok(u.forgeWeaponBonus==rank*100&&u.attackBonus==rank*100,"Battle attack");
   ok(ForgeRules.gear(1,weapon,b)[0]==Battle.baseDamage(weapon)+rank*100,"Bag attack");
   int other=(weapon+1)%10;ItemRules.weapon(u,other);ok(u.forgeAttack==ranks[other]*100,"Weapon switch");
  }
  ok(ForgeRules.rankBonus(2,15)==195&&ForgeRules.rankBonus(3,15)==195&&ForgeRules.rankBonus(6,15)==195,"Armor unchanged");
  for(int roll=10;roll<=100;roll++)ok(StoneDrops.antAmount(roll)==roll/2,"Half quantity");
  int[][] odds={{65,25,8,2,0},{25,35,25,12,3},{5,15,30,35,15}};
  var store=new CloudStore.Memory();var session=Expansion780Check.session(store,"HF18",70);var p=session.profile;p.dungeonClear=true;
  List<Map<String,Object>> players=List.of(Map.of("id",session.id,"team",0,"damage",1));
  for(int d=0;d<3;d++){
   int[] counts=new int[5];for(int roll=0;roll<100;roll++)counts[StoneDrops.grade(d,roll)-1]++;
   ok(Arrays.equals(counts,odds[d]),"Unchanged grade odds");
   for(int stage=1;stage<=2;stage++)for(int trial=0;trial<20;trial++){
    String match=UUID.randomUUID().toString();if(stage==1)store.finishAntStage(match,0,players,d);else store.finishDungeon780(match,0,players,d);
    for(int pick=0;pick<stage;pick++){
     int[] before=p.craftStones.clone();int old=p.stones;int[] r=store.claimDungeon(match,p.id,pick,pick,d);
     ok(r[2]>=5&&r[2]<=50,"Ant reward bounds");
     int grade=r[4];ok(grade>=1&&grade<=5,"Valid grade");
     ok(grade==1?p.stones-old==r[2]:p.stones==old&&p.craftStones[grade-2]-before[grade-2]==r[2],"Exactly credited");
     int balance=p.stones;int[] after=p.craftStones.clone();ok(Arrays.equals(r,store.claimDungeon(match,p.id,pick,pick,d))&&p.stones==balance&&Arrays.equals(after,p.craftStones),"Idempotent retry");
    }
   }
   String match=UUID.randomUUID().toString();store.tribalStore.apply("finish",Map.of("match",match,"stage",1,"difficulty",d,"winner",0,"players",players,"min",TribalRules.STONE_MIN[d],"max",TribalRules.STONE_MAX[d]));
   int amount=Json.num(store.tribalStore.apply("claim",Map.of("match",match,"user",p.id,"pick",0,"card",0)),"amount",0);
   ok(amount>=TribalRules.STONE_MIN[d]&&amount<=TribalRules.STONE_MAX[d],"Tribal reward unchanged");
  }
  System.out.println("PASS "+checks+" HF18 weapon/reward checks");
 }
}
