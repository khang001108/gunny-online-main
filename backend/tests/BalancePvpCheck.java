import java.util.Arrays;
public class BalancePvpCheck {
 static int n;
 static void ok(boolean x,String why){if(!x)throw new AssertionError(why);n++;}
 static class Ground implements Battle.Ground{public boolean solid(int x,int y){return y>=170;}public int surface(int x,int y){return 170;}public void crater(int x,int y,int r){}}
 static Battle.Unit gear(int lv,int weapon,int rank,int mask){Battle.Unit u=new Battle.Unit();u.experience=Progression.experienceForLevel(lv);u.weapon=weapon;int[] ranks=new int[130];Arrays.fill(ranks,rank);ForgeRules.apply(u,ForgeRules.equipped(ranks,0,mask,weapon,0,new int[5]),ranks);ItemRules.apply(u,mask);return u;}
 public static void main(String[] args){
  double max=0;String desc="";
  for(int lv:new int[]{1,15,30,50,70})for(int w=0;w<10;w++)for(int rank:new int[]{0,5,10,15})for(int mask:new int[]{1,3,13,77,125}){
   Battle.Unit a=gear(lv,w,rank,mask);int base=Battle.baseDamage(w)+a.attackBonus;
   for(int boost=0;boost<=100;boost+=50)for(int extra=0;extra<=4;extra+=2)for(int t=0;t<2;t++)for(int pow=0;pow<2;pow++){
    int count=(extra+1)*(t==1?3:1);int raw=CombatBalance.projectileDamage(base,boost,t==1,extra,pow==1,true);
    // All projectiles hit at the center and all crit: conservative stress bound.
    int total=count*(CombatBalance.mitigate(raw,a.defense,a.luck,true)*150/100);
    double frac=(double)Math.min(total,a.maxHp*80/100)/a.maxHp;if(frac>max){max=frac;desc="lv="+lv+" weapon="+w+" rank="+rank+" mask="+mask+" boost="+boost+" extra="+extra+" triple="+t+" pow="+pow+" dmg="+total+" hp="+a.maxHp;}
    ok(CombatBalance.projectileDamage(base,boost,t==1,extra,pow==1,false)>=raw,"pve unchanged budget");
   }
  }
  System.out.println("Worst same-gear all-crit ratio after guard: "+max+" "+desc);
  Battle b=new Battle(new Ground());b.start(true);b.units[0]=gear(70,8,15,125);b.units[1]=gear(70,8,15,125);b.units[0].x=50;b.units[1].x=350;b.units[0].y=b.units[1].y=170;b.active=0;b.units[0].rage=100;
  int hp=b.units[1].hp;ok(b.prop(0)&&b.prop(0)&&b.prop(1)&&b.prop(2)&&b.prop(2),"max combo available");
  int base=Battle.baseDamage(8)+b.units[0].attackBonus;int oldRaw=base*2*60/100*82/100;int newRaw=b.make(45,65).damage;
  System.out.println("Lv70 full+15: atk="+base+" def="+b.units[1].defense+" luck="+b.units[0].luck+" HP="+hp+" old raw/shot="+oldRaw+" new raw/shot="+newRaw);
  int red=0;for(int i=0;i<15;i++){Battle.Ball ball=b.make(45,65);ball.x=b.units[1].x*256;ball.y=Battle.targetY(b.units[1])*256;b.shotSerial=i;b.impact(ball);if(b.damagePopups[b.damagePopupCount-1].style==Battle.DamagePopup.STYLE_SPECIAL)red++;}ok(b.units[1].hp>0,"real impact survival");System.out.println("15 actual hits: damage="+(hp-b.units[1].hp)+" remain="+b.units[1].hp+" crit="+red);
  Battle edge=new Battle(new Ground());edge.start(true);edge.units[0]=gear(70,8,15,1);edge.units[1]=gear(70,8,15,1);edge.units[0].x=50;edge.units[1].x=350;edge.units[0].y=edge.units[1].y=170;int ehp=edge.units[1].hp;
  for(int i=0;i<15;i++){Battle.Ball ball=new Battle.Ball();ball.owner=0;ball.damage=9000;ball.x=edge.units[1].x*256;ball.y=Battle.targetY(edge.units[1])*256;edge.impact(ball);}ok(edge.units[1].hp==ehp-ehp*80/100,"edge actual impact guard");edge.beginTurn(0);Battle.Ball lethal=new Battle.Ball();lethal.owner=0;lethal.damage=9000;lethal.x=edge.units[1].x*256;lethal.y=Battle.targetY(edge.units[1])*256;edge.impact(lethal);ok(edge.units[1].hp==0,"next turn can finish");
  Battle strong=new Battle(new Ground());strong.start(true);strong.units[0]=gear(70,8,15,125);strong.units[1]=gear(15,8,0,1);ok(CombatBalance.limitTurnDamage(strong,0,1,99999)==99999,"strong versus weak no artificial cap");strong.dungeonStage=1;ok(CombatBalance.limitTurnDamage(strong,0,1,99999)==99999,"pve no guard");
  ok(CombatBalance.critChance(1000)==1000&&CombatBalance.critChance(10000)==1500,"crit chance");ok(CombatBalance.penetration(1000)==10,"penetration");
  int count=0;for(int i=0;i<10000;i++){b.shotSerial=i;boolean actual=CombatBalance.roll(b,0,1,12345,54321,0)<CombatBalance.critChance(b.units[0].luck);boolean redHit=CombatBalance.special(b,0,1,1000,12345,54321,0);ok(actual==redHit,"actual crit only");if(redHit)count++;}System.out.println("Red rate sample="+count+"/10000");System.out.println("PASS "+n);
 }
}
