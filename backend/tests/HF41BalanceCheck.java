import java.util.*;
public final class HF41BalanceCheck {
 static int checks;
 static void ok(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
 public static void main(String[] args){
  int[] ranks=new int[130],look=new int[5];
  for(int w=0;w<10;w++)for(int r=0;r<=15;r++){
   ranks[w]=r;int[] f=ForgeRules.equipped(ranks,0,1,w,0,look);int expected=Battle.baseDamage(w)*(r+1)/2;
   int[] g=ForgeRules.gear(1,w,f);ok(g[0]==expected,"weapon total");
   Battle.Unit u=new Battle.Unit();u.weapon=w;ForgeRules.apply(u,f,ranks);ItemRules.apply(u,1);ok(Battle.baseDamage(w)+u.attackBonus==expected,"battle weapon total");
   int next=(w+1)%10;ItemRules.weapon(u,next);ok(Battle.baseDamage(next)+u.attackBonus==Battle.baseDamage(next)*(ranks[next]+1)/2,"switch weapon");
  }
  for(int k:new int[]{2,3,6}){int prev=0;for(int r=0;r<=15;r++){
   int a=ItemRules.attack(k,0)+ForgeRules.statBonus(k,0,r,0),d=ItemRules.defense(k,0)+ForgeRules.statBonus(k,0,r,1),l=ItemRules.luck(k,0)+ForgeRules.statBonus(k,0,r,2);int total=a+d+l;
   ok(total>=prev,"monotone rank");prev=total;ok(Math.abs((k==6?l:d)-2*a)<=3,"ratio rounding");if(r==15)ok(total==(k==6?1250:1175),"maximum total");
  }}
  Arrays.fill(ranks,15);int[] f=ForgeRules.equipped(ranks,0,(1<<2)|(1<<3),3,0,look);int[] g=ForgeRules.gear(12,3,f);ok(g[0]+g[1]+g[2]==2350,"armor pair total");
  for(int lv=1;lv<=70;lv++){int e=Progression.experienceForLevel(lv);ok(Progression.baseHp(e)==4650+150*(lv-1),"hp");ok(Progression.growth(e,0)==5*(lv-1)&&Progression.growth(e,1)==8*(lv-1)&&Progression.growth(e,2)==3*(lv-1),"growth");for(int wing=-1;wing<5;wing++){Battle.Unit u=new Battle.Unit();u.experience=e;u.wingHp=VipRules.hp(wing);ItemRules.apply(u,0);ok(u.maxHp==Progression.baseHp(e)+VipRules.hp(wing),"wing unchanged");}}
  ok(CombatBalance.hit(1600,600,0,9999)==1000,"example");ok(CombatBalance.hit(1600,99999,0,9999)==960,"cap");ok(CombatBalance.hit(1600,600,1000,0)==1590,"crit plus pen");
  for(int luck=0;luck<=2000;luck++){ok(CombatBalance.critChance(luck)<=1500&&CombatBalance.penetration(luck)<=10,"luck caps");for(int d=0;d<=5000;d+=100){int v=CombatBalance.hit(1600,d,luck,9999);ok(v>=960&&v<=1600,"damage bounds");}}
  ok(ItemRules.DEFENSE[8]==5&&ItemRules.LUCK[8]==5,"spear");
  Battle b=new Battle(new Arsenal717ServerCheck.Ground());b.start(true);
  b.units[0].luck=0;b.units[1].luck=0;int first=0;
  for(int i=0;i<10000;i++)if(CombatBalance.firstActor(b)==0)first++;
  ok(first>4700&&first<5300,"equal luck fair");
  b.units[0].luck=1000;b.units[1].luck=100;first=0;
  for(int i=0;i<10000;i++)if(CombatBalance.firstActor(b)==0)first++;
  ok(first>8800&&first<9400,"higher luck weighted");
  b.units[0].hp=0;for(int i=0;i<100;i++)ok(CombatBalance.firstActor(b)==1,"dead cannot start");
  b.units[0].hp=3000;b.units[0].statsEnabled=true;b.units[0].luck=0;b.units[1].statsEnabled=true;b.units[1].defense=600;b.units[1].hp=3000;b.units[1].maxHp=3000;
  Battle.Ball shot=new Battle.Ball();shot.owner=0;shot.x=b.units[1].x*256;shot.y=Battle.targetY(b.units[1])*256;shot.damage=1600;shot.radius=10;b.impact(shot);ok(b.units[1].hp==2300,"real impact defense");
  System.out.println("PASS "+checks+" HF41 balance checks");
 }
}
