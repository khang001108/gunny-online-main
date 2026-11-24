/** Pure Java checks, no emulator or MIDP APIs required. */
public final class DungeonRewardPreviewCheck {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){
  for(int stage:new int[]{1,2,3,4,6})for(int d=0;d<3;d++){
   int count=DungeonRewardPreview.count(stage,d),cols=DungeonRewardPreview.columns(count),mask=0;
   check(count==(stage==2?6:stage==6?6+d:5),"count");
   for(int i=0;i<count;i++){
    int item=DungeonRewardPreview.item(stage,d,i),x=DungeonRewardPreview.x(count,i),y=DungeonRewardPreview.y(count,i),w=DungeonRewardPreview.width(count);
    check(item>=0&&item<8&&(mask&(1<<item))==0,"unique reward");mask|=1<<item;
    check(x>=10&&x+w<=310&&y>=36&&y+72<=202,"E72 layout bounds");
    check(DungeonRewardPreview.hit(count,x+w/2,y+36)==i,"touch center");
    for(int dir:new int[]{1,2,4,8}){int next=DungeonRewardPreview.move(i,count,dir);check(next>=0&&next<count,"direction bounds");}
    if(i<5){int factor=stage==6?(d==2?11:d==1?15:18):18;check(DungeonRewardPreview.tenths(stage,d,item)==(StoneDrops.WEIGHTS[d][i]*factor*10+9)/18,"original odds");}
    if(item==7)check(DungeonRewardPreview.percent(stage,d,item).equals("100%"),"gift odds");
   }
   check((mask&31)==31,"all stone grades");
   check(((mask&128)!=0)==(stage==2||stage==6),"gift on final stage");
   check(((mask&32)!=0)==(stage==6&&d>0),"weapon reward");
   check(((mask&64)!=0)==(stage==6&&d==2),"wing reward");
   check(DungeonRewardPreview.move(0,count,2)==1,"right");
   check(DungeonRewardPreview.move(1,count,1)==0,"left");
   check(DungeonRewardPreview.move(0,count,8)==cols,"down");
   check(DungeonRewardPreview.move(cols,count,4)==0,"up");
  }
  for(int count=5;count<=8;count++)for(int i=0;i<count;i++){
   int x=DungeonRewardPreview.x(count,i),y=DungeonRewardPreview.y(count,i),w=DungeonRewardPreview.width(count);
   check(y>=DungeonRewardPreview.PANEL_Y+25&&y+72<DungeonRewardPreview.PANEL_Y+DungeonRewardPreview.PANEL_H,"inside gold popup body");
   check(!DungeonRewardPreview.closeHit(x+w/2,y+36),"reward click is not close");
  }
  int cx=DungeonRewardPreview.CLOSE_X,cy=DungeonRewardPreview.CLOSE_Y;
  check(DungeonRewardPreview.closeHit(cx,cy),"X top left");
  check(DungeonRewardPreview.closeHit(cx+20,cy+18),"X bottom right");
  check(!DungeonRewardPreview.closeHit(cx-1,cy),"outside X left");
  check(!DungeonRewardPreview.closeHit(cx+21,cy),"outside X right");
  check(!DungeonRewardPreview.closeHit(cx,cy+19),"outside X bottom");
  check(DungeonRewardPreview.hit(8,cx+10,cy+9)==-1,"X is outside grid");
  check(DungeonRewardPreview.percent(6,2,5).equals("33.3%"),"hard weapon");
  check(DungeonRewardPreview.percent(6,1,5).equals("16.7%"),"normal weapon");
  check(DungeonRewardPreview.percent(6,2,6).equals("5.6%"),"wing odds");
  System.out.println("Dungeon reward preview: "+checks+" checks passed");
 }
}
