/** Shared cumulative EXP and equipment rating, CLDC compatible. */
final class Progression {
 static int level(int exp){return Math.min(70,1+(int)((Math.sqrt(1+Math.max(0,exp)*4.0/25)-1)/2));}
 static int experienceForLevel(int lv){lv=Math.max(1,Math.min(70,lv));return 25*(lv-1)*lv;}
 static int baseHp(int exp){return 4650+150*(level(exp)-1);}
 static int bonus(int exp){return growth(exp,0);}
 static int growth(int exp,int stat){return (level(exp)-1)*(stat==0?5:stat==1?8:3); }
 static final int[] BOT_LEVELS={1,5,10,15,20,60,65,70};
 static int botExperience(int bot){return experienceForLevel(BOT_LEVELS[Math.max(0,Math.min(7,bot))]);}
 static int within(int exp){int lv=level(exp);return lv==70?0:(int)(Math.max(0,exp)-25L*(lv-1)*lv);}
 static int limit(int exp){int lv=level(exp);return lv==70?0:50*lv;}
 static int windLimit(int exp){return Math.min(30,9+level(exp));}
 static int power(int mask,int weapon){return ItemRules.baseStat(mask,weapon,0)+ItemRules.baseStat(mask,weapon,1)+ItemRules.baseStat(mask,weapon,2);}
 static int power(int mask,int weapon,int exp){return power(mask,weapon)+growth(exp,0)+growth(exp,1)+growth(exp,2);}
 static boolean reward(int winner,int team,int damage){return winner<2&&(winner==team||damage>0);}
}
