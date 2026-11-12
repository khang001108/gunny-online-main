/** Shared cumulative EXP and equipment rating, CLDC compatible. */
final class Progression {
 static int level(int exp){return Math.min(70,1+(int)((Math.sqrt(1+Math.max(0,exp)*4.0/25)-1)/2));}
 static int within(int exp){int lv=level(exp);return lv==70?0:(int)(Math.max(0,exp)-25L*(lv-1)*lv);}
 static int limit(int exp){int lv=level(exp);return lv==70?0:50*lv;}
 static int windLimit(int exp){return Math.min(30,9+level(exp));}
 static int power(int mask,int weapon){return ((mask&1)!=0?Battle.baseDamage(weapon)+ItemRules.DEFENSE[weapon]+ItemRules.LUCK[weapon]:0)+ItemRules.clothes(mask)*15;}
 static boolean reward(int winner,int team,int damage){return winner<2&&(winner==team||damage>0);}
}
