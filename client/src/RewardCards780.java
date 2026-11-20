import javax.microedition.lcdui.Image;

/** Shared card art and server-confirmed picks for team and dungeon results. */
final class RewardCards780 {
 final OnlineResult result;
 final PhoneCanvas c;
 final OnlineClient n;
 int selected,offlinePicks,revealing=-1;
 long revealAt;
 final boolean[] taken=new boolean[18];
 final int[] amount=new int[18],type=new int[18];
 Image stone,wing;
 RewardCards780(OnlineResult result){this.result=result;c=result.c;n=result.net;}
 void reset(){
  selected=offlinePicks=0;revealing=-1;revealAt=0;
  for(int i=0;i<taken.length;i++){taken[i]=false;amount[i]=type[i]=0;}
 }
 int picks(){return result.online()?n.rewardPicks:offlinePicks;}
 int limit(){return result.antStage()?1:2;}
 int columns(){return result.antStage()?3:6;}
 void reveal(int card){revealing=card;revealAt=System.currentTimeMillis();}
 boolean flipping(){return System.currentTimeMillis()-revealAt<700;}
 boolean open(int card){return result.online()?n.cardNames[card]!=null&&n.cardNames[card].length()>0:taken[card];}
 int left(){return result.antStage()?166:13;}
 void draw(){
  if(stone==null)stone=c.combatRoomArt("stone780");
  c.all();c.img(result.background,0,0);
  c.pcCenter((result.antStage()?"ẢI 1 • ":"")+"BẢNG THẺ • "+picks()+"/"+limit()+" đã chọn",160,13);
  if(result.antStage()){
   c.img(result.light,5,39);
   c.img(result.hero,18,49);
   c.pcCenter(c.pcShort(result.name(result.actor()),128),78,175);
  }
  int cols=columns();
  for(int i=0;i<cols*3;i++){
   int x=left()+i%cols*51,y=45+i/cols*59;
   if(i==revealing&&flipping()){
    int frame=(int)Math.min(6,(System.currentTimeMillis()-revealAt)/100);
    c.part(result.flip,frame*38,0,38,55,x,y,false);
   }else{
    boolean opened=open(i);
    c.img(opened?result.front:result.back,x,y);
    if(opened){
     int reward=result.online()?n.cardCoins[i]:amount[i],kind=result.online()?n.cardTypes[i]:type[i];
     if(kind<2)c.img(kind==1?stone:result.gold,x+8,y+9);else if(kind==2){if(wing==null)wing=NativeUi.fit(c.combatRoomArt("tribal-wing"),22*c.s,20*c.s,0);c.img(wing,x+8,y+6);}else c.img(c.weapons[8],x+8,y+7);
     c.pcCenter("+"+reward,x+19,y+35);
     if(result.online())c.pcCenter(c.pcShort(n.cardNames[i],36),x+19,y+46);
    }
   }
   if(selected==i)c.drawSelectionGlow(x-2,y-2,42,59);
  }
  c.pcCenter(picks()>=limit()?result.antStage()||n.roomKind==3&&n.dungeonStage>=3&&n.dungeonStage<6?"OK: tiếp tục ải sau":"OK: về sảnh"
          :n.busy?"Đang nhận thưởng...":"Chọn thẻ • OK: lật",160,228);
 }
 void activate(){
  if(flipping())return;
  if(picks()>=limit()){result.leave();return;}
  if(open(selected)){if(result.online())c.message("Thẻ đã có người chọn");return;}
  if(result.online()){
   if(n.busy||n.pending.length()>0)return;
   n.cardDraft=selected;n.request("claim780");
  }else{
   taken[selected]=true;
   type[selected]=c.offlineDungeon.active?1:c.game.rand(2);
   amount[selected]=c.offlineDungeon.active?10+c.game.rand(91):type[selected]==1?1+c.game.rand(10):500+c.game.rand(1501);
   if(type[selected]==1){
    c.economy.offlineStones=Math.min(999999,c.economy.offlineStones+amount[selected]);c.economy.save();
   }else{
    c.coins=Math.min(999999,c.coins+amount[selected]);c.saveSettings();
   }
   offlinePicks++;reveal(selected);
  }
 }
 void key(int key,int dir,boolean ok){
  if(flipping())return;
  if(key==-7){if(picks()>=limit())result.leave();else c.message("Bạn còn "+(limit()-picks())+" lượt chọn thẻ");return;}
  int cols=columns(),cards=cols*3;
  if(dir!=0)selected=(selected+cards+(dir==1?-1:dir==2?1:dir==4?-cols:cols))%cards;
  else if(ok||key==-6)activate();
 }
 void pointer(int x,int y){
  if(y>=225){if(!flipping()&&picks()>=limit())result.leave();return;}
  int xx=x-left(),yy=y-45;
  if(xx>=0&&xx<columns()*51&&xx%51<38&&yy>=0&&yy<177&&yy%59<55){
   selected=yy/59*columns()+xx/51;activate();
  }
 }
}
