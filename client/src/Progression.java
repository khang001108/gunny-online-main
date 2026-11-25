/** Shared cumulative EXP and equipment rating, CLDC compatible. */
final class Progression {
 static int level(int exp){int lo=1,hi=70;while(lo<hi){int mid=(lo+hi+1)/2;if(exp>=25*(mid-1)*mid)lo=mid;else hi=mid-1;}return lo;}
 static int experienceForLevel(int lv){lv=Math.max(1,Math.min(70,lv));return 25*(lv-1)*lv;}
 static int baseHp(int exp){return Battle.MAXHP+(15000-Battle.MAXHP)*(level(exp)-1)/69;}
 static int bonus(int exp){return (level(exp)-1)*5;}
 static final int[] BOT_LEVELS={1,5,10,15,20,60,65,70};
 static int botExperience(int bot){return experienceForLevel(BOT_LEVELS[Math.max(0,Math.min(7,bot))]);}
 static int within(int exp){int lv=level(exp);return lv==70?0:(int)(Math.max(0,exp)-25L*(lv-1)*lv);}
 static int limit(int exp){int lv=level(exp);return lv==70?0:50*lv;}
 static int windLimit(int exp){return Math.min(30,9+level(exp));}
 static int power(int mask,int weapon){return ((mask&1)!=0?Battle.baseDamage(weapon)+ItemRules.DEFENSE[weapon]+ItemRules.LUCK[weapon]:0)+ItemRules.clothes(mask)*15;}
 static int power(int mask,int weapon,int exp){return power(mask,weapon)+bonus(exp)*3;}
 static boolean reward(int winner,int team,int damage){return winner<2&&(winner==team||damage>0);}
}
