/** Shared numeric VIP/wing rules. No client authority over inventory or rolls. */
final class VipRules {
 static final int TOTAL=21200;
 static final int[] COST={500,600,700,800,1000,1200,1400,1600,2000,2800,3800,4800};
 static final int[] WING_VIP={1,3,5,8,12},WING_PRICE={200,500,1000,2500,5000},WING_HP={500,1000,1500,2500,4000};
 static final String[] WING_NAMES={"Cánh Bướm","Cánh Thiên Thần","Cánh Ác Ma","Cánh Phượng Hoàng","Cánh Huyền Thoại"};
 static int level(int exp){int level=0;exp=Math.max(0,Math.min(TOTAL,exp));while(level<12&&exp>=COST[level])exp-=COST[level++];return level;}
 static int within(int exp){for(int i=0;i<12;i++){if(exp<COST[i])return Math.max(0,exp);exp-=COST[i];}return 0;}
 static int need(int exp){int lv=level(exp);return lv==12?0:COST[lv];}
 static int hp(int wing){return wing>=0&&wing<5?WING_HP[wing]:0;}
 static String cardName(int item){return item==12?"Full VIP":"Thẻ VIP "+(item+1);}
}
