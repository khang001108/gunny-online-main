/** Equipment stats are shared by client, authoritative server and tooltips. */
final class ItemRules {
 static final int[] DEFENSE={40,25,20,35,65,30,0,30,0,25};
 static final int[] LUCK={20,15,40,25,5,35,0,30,0,20};
 static boolean validMask(int mask){return mask>=0&&mask<=127&&((mask&2)==0||(mask&124)==0);}
 static int clothes(int mask){int n=0;for(int k=1;k<7;k++)if((mask&(1<<k))!=0)n++;return n;}
 static int attack(int kind,int item){return kind==0?Battle.baseDamage(item):5;}
 static int defense(int kind,int item){return kind==0?DEFENSE[item]:5;}
 static int luck(int kind,int item){return kind==0?LUCK[item]:5;}
 static int maskAfter(int mask,int kind,boolean remove){if(remove)return mask&~(1<<kind);if(kind==1)return(mask&1)|2;if(kind>=2)return(mask&~2)|(1<<kind);return mask|1;}
 static void stats(Battle.Unit u,int mask){u.statsEnabled=true;u.wornMask=mask;u.attackBonus=clothes(mask)*5;u.maxHp=Battle.MAXHP+u.attackBonus+((mask&1)!=0?DEFENSE[u.weapon]:0);u.maxEnergy=240+u.attackBonus+((mask&1)!=0?LUCK[u.weapon]:0);}
 static void apply(Battle.Unit u,int mask){stats(u,mask);u.hp=u.maxHp;u.energy=u.maxEnergy;}
 static void weapon(Battle.Unit u,int id){u.weapon=id;if(u.statsEnabled){stats(u,u.wornMask);u.hp=Math.min(u.hp,u.maxHp);u.energy=Math.min(u.energy,u.maxEnergy);}}
}
