/** One reward table and deterministic grid navigation for room preview and detail popup. */
final class DungeonRewardPreview {
 static final int FOCUS=66, WEAPON=5, WING=6, GIFT=7;
 static final int PANEL_X=10,PANEL_Y=20,PANEL_W=300,PANEL_H=200;
 static final int CLOSE_X=PANEL_X+PANEL_W-25,CLOSE_Y=PANEL_Y+5;
 static boolean closeHit(int x,int y){return x>=CLOSE_X&&x<CLOSE_X+21&&y>=CLOSE_Y&&y<CLOSE_Y+19;}
 static int count(int stage,int diff){diff=DungeonBalance.rewardTier(diff);return 5+(stage==6&&diff>0?1:0)+(stage==6&&diff==2?1:0)+(stage==2||stage==6?1:0);}
 static int item(int stage,int diff,int index){diff=DungeonBalance.rewardTier(diff);if(index<5)return index;if(stage==6&&diff==2){if(index==5)return WEAPON;if(index==6)return WING;}return GIFT;}
 static int tenths(int stage,int diff,int item){diff=DungeonBalance.rewardTier(diff);if(item<5){int factor=stage==6&&diff==2?17:18;return (StoneDrops.WEIGHTS[diff][item]*factor*10+9)/18;}return item==WEAPON?(stage==6&&diff==2?1000:0):item==WING?56:1000;}
 static String percent(int stage,int diff,int item){int p=tenths(stage,diff,item);return p==1000?"100%":p/10+"."+p%10+"%";}
 static int columns(int count){return count>6?4:3;}
 static int x(int count,int index){return 20+index%columns(count)*(280/columns(count));}
 static int y(int count,int index){return 49+index/columns(count)*77;}
 static int width(int count){return 280/columns(count)-4;}
 static int move(int index,int count,int dir){int cols=columns(count);if(dir==1&&index%cols>0)return index-1;if(dir==2&&index%cols<cols-1&&index+1<count)return index+1;if(dir==4&&index>=cols)return index-cols;if(dir==8&&index+cols<count)return index+cols;if(dir==8&&index<cols&&count>cols)return count-1;return index;}
 static int hit(int count,int px,int py){for(int i=0;i<count;i++)if(px>=x(count,i)&&px<x(count,i)+width(count)&&py>=y(count,i)&&py<y(count,i)+72)return i;return -1;}
}
