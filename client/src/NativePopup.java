import javax.microedition.lcdui.Image;
/** One asset-backed popup for notices, choices and battle menus. */
final class NativePopup {
    void releaseArt(){frame=button=close=fittedFrame=playerPortrait=searchInput=searchInputFit=checkOn=checkOff=null;for(int i=0;i<fittedButtons.length;i++)fittedButtons[i]=null;}

 static final int CONFIRM_QUIT = 8, INVITE_TEAM=30;
 Image playerPortrait;String playerName;int playerLevel;
 final PhoneCanvas c;boolean opened,pausedOffline;int action,selected;String title="",text="";String[] options={"Đóng"};Image frame,button,close;final Image[] fittedButtons=new Image[3];final int[] fittedWidths=new int[3];
 NativePopup(PhoneCanvas canvas){c=canvas;}
 boolean skipHint, hintFocused;
 Image checkOn, checkOff;
 void open(String heading,String message,String[] choices,int callback){c.gifts.showing=false;playerPortrait=null;title=heading;text=message;options=choices;action=callback;selected=scroll=0;skipHint=hintFocused=false;opened=true;c.toastTicks=0;c.held=0;}
 void playerNotice(String heading,String message,Image portrait,String name,int level,String[] choices,int callback){open(heading,message,choices,callback);playerPortrait=portrait;playerName=name;playerLevel=level;}
 void notice(String heading,String message){open(heading,message,new String[]{"Đóng"},0);}
 void confirmQuit(){
  open("Thoát game","Bạn có muốn thoát game không?",new String[]{"Hủy","Thoát"},CONFIRM_QUIT);
 }
 void resumeOffline(){pausedOffline=false;}
 void dismiss(){if(action==INVITE_TEAM&&!c.net.social.respond(false))return;opened=false;resumeOffline();}
 void pause(){pausedOffline=!c.onlineBattle();if(pausedOffline){c.game.charging=false;c.fireKey=0;}open("Tạm dừng",c.onlineBattle()?"Trận online vẫn tiếp tục":"Trận đấu đã tạm dừng",new String[]{"Tiếp tục","Thiết lập","Thoát trận"},1);}
 Image searchInput,searchInputFit;
 void renderInput(String heading,String value,int focus){if(frame==null){frame=c.combatRoomArt("settings743-popup");button=c.combatRoomArt("popup751-button");close=c.combatRoomArt("bag715-close");}width=256;height=136;left=32;top=52;choiceCount=2;c.all();c.img(fitFrame(),left,top);c.pcCenter(heading,160,61);c.img(close,263,57);c.pcText("Tên hoặc ID",50,85);if(searchInput==null)searchInput=c.combatRoomArt("auth714-input");if(searchInputFit==null)searchInputFit=NativeUi.fit(searchInput,220*c.s,24*c.s,5*c.s);c.img(searchInputFit,50,99);c.pcText(c.pcShort(value.length()==0?"Nhập tên hoặc ID...":value,206),57,106);for(int i=0;i<2;i++){int w=buttonWidth(i==0?"Nhập":"Tìm"),x=i==0?58:262-w;c.img(fittedButton(i,w),x,153);c.pcCenter(i==0?"Nhập":"Tìm",x+w/2,158);if(focus==i)c.drawSelectionGlow(x-1,152,w+2,24);}}
 int choiceCount=1;int left=32,top=56,width=256,height=128,rows,visibleRows,scroll;String[] lines=new String[64];Image fittedFrame;int frameW,frameH;
 void layout(String heading,String message,String[] choices){
  choiceCount=choices.length;boolean paged=opened&&action==4;int natural=c.pcWidth(message),wanted=(int)Math.sqrt(Math.max(1,natural)*30)+40;
  int choicesWidth=0;for(int i=0;i<choices.length;i++)choicesWidth=Math.max(choicesWidth,c.pcWidth(choices[i])+28);if(choices.length==2)choicesWidth=choicesWidth*2+36;else choicesWidth+=28;width=paged?256:Battle.clamp(Math.max(choicesWidth,Math.max(c.pcWidth(heading)+60,wanted)),190,292);rows=0;String remaining=message;
  while(remaining.length()>0&&rows<lines.length){int newline=remaining.indexOf('\n'),end=newline<0?remaining.length():newline;while(end>1&&c.pcWidth(remaining.substring(0,end))>width-28)end--;if(end<remaining.length()&&end!=newline){int space=remaining.lastIndexOf(' ',end);if(space>0)end=space;}lines[rows++]=remaining.substring(0,end).trim();remaining=remaining.substring(Math.min(remaining.length(),end+(end==newline?1:0))).trim();}
  visibleRows=Math.min(rows,choices.length==3?5:10);if(paged)visibleRows=Math.min(rows,c.notices.hasAppeal()?4:5);height=paged?128:Math.max(84,visibleRows*12+(choices.length==3?110:68)+(rows>visibleRows?12:0)+(soloHint()?17:0)+(playerPortrait!=null?66:0));left=(320-width)/2;top=(240-height)/2;scroll=Battle.clamp(scroll,0,Math.max(0,rows-visibleRows));
 }
 int buttonWidth(String label){return Battle.clamp(c.pcWidth(label)+28,52,choiceCount==2?(width-36)/2:width-28);}
 int choiceX(int count,int index,int w){return count==2?left+width*(2*index+1)/4-w/2:(320-w)/2;}
 int choiceY(int count,int index){return opened&&action==4?buttonY(count,index):top+height-31-(count==3?(2-index)*24:0);}
 static int buttonY(int count,int index){return count==1?153:count==2?124+index*25:108+index*24;}
 int choiceAt(String[] choices,int x,int y){for(int i=0;i<choices.length;i++){int w=buttonWidth(choices[i]),bx=choiceX(choices.length,i,w),by=choiceY(choices.length,i);if(x>=bx&&x<bx+w&&y>=by&&y<by+22)return i;}return -1;}
 Image fittedButton(int index,int width){if(fittedButtons[index]!=null&&fittedWidths[index]==width)return fittedButtons[index];int sw=button.getWidth(),h=button.getHeight(),w=width*c.s,cap=8*c.s;int[] source=new int[sw*h],pixels=new int[w*h];button.getRGB(source,0,sw,0,0,sw,h);for(int y=0;y<h;y++)for(int x=0;x<w;x++){int sx=x<cap?x:x>=w-cap?sw-(w-x):sw/2;pixels[y*w+x]=source[y*sw+sx];}fittedWidths[index]=width;return fittedButtons[index]=Image.createRGBImage(pixels,w,h,true);}
 Image fitFrame(){if(fittedFrame!=null&&frameW==width&&frameH==height)return fittedFrame;int sw=frame.getWidth(),sh=frame.getHeight(),w=width*c.s,h=height*c.s,edge=6*c.s,header=25*c.s;int[] src=new int[sw*sh],dst=new int[w*h];frame.getRGB(src,0,sw,0,0,sw,sh);for(int y=0;y<h;y++){int sy=y<header?y:y>=h-edge?sh-h+y:header+(y-header)*(sh-header-edge)/(h-header-edge);for(int x=0;x<w;x++){int sx=x<edge?x:x>=w-edge?sw-w+x:edge+(x-edge)*(sw-2*edge)/(w-2*edge);dst[y*w+x]=src[sy*sw+sx];}}frameW=width;frameH=height;return fittedFrame=Image.createRGBImage(dst,w,h,true);}
 void render(String heading,String message,String[] choices,int focus){if(frame==null){frame=c.combatRoomArt("settings743-popup");button=c.combatRoomArt("popup751-button");close=c.combatRoomArt("bag715-close");}layout(heading,message,choices);c.all();c.img(fitFrame(),left,top);c.pcCenter(c.pcShort(heading,width-62),160,top+9);if(opened&&action!=4)c.img(close,left+width-25,top+5);int y=top+31;if(playerPortrait!=null){c.img(playerPortrait,left+18,y);c.pcText(c.pcShort(playerName,width-100),left+80,y+10);c.pcText("Lv "+playerLevel,left+80,y+28);y+=66;}for(int row=0;row<visibleRows;row++)c.pcCenter(lines[scroll+row],160,y+row*12);if(rows>visibleRows)c.pcCenter((scroll+1)+"/"+(rows-visibleRows+1)+" • ↑↓",160,top+height-44);if(opened&&action==4&&c.notices.hasAppeal())c.pcCenter(c.pcShort("Khiếu nại: "+c.notices.appeals[c.notices.index]+" (trái)",222),160,139);for(int i=0;i<choices.length;i++){int by=choiceY(choices.length,i),w=buttonWidth(choices[i]),bx=choiceX(choices.length,i,w);c.img(fittedButton(i,w),bx,by);c.pcCenter(c.pcShort(choices[i],w-28),bx+w/2,by+5);if(focus==i)c.drawSelectionGlow(bx-1,by-1,w+2,23);}}
 boolean soloHint(){return opened&&action==DungeonScreen.CONFIRM_SOLO;}
 int hintY(){return top+height-49;}
 void draw(){
  if(!opened)return;
  if(c.gifts.showing){c.gifts.draw();return;}
  render(title,text,options,hintFocused?-1:selected);
  if(soloHint()){
   if(checkOn==null){checkOn=c.combatRoomArt("auth-check-on");checkOff=c.combatRoomArt("auth-check-off");}
   int x=left+20,y=hintY();
   c.img(skipHint?checkOn:checkOff,x,y);
   c.pcText("Không nhắc nữa",x+15,y+1);
   if(hintFocused)c.drawSelectionGlow(x-2,y-2,16,14);
  }
 }
 void choose(){
  if(action==INVITE_TEAM){if(c.net.social.respond(selected==0))opened=false;return;}
  if((action==2||action==3&&selected==1)&&c.net.pending.length()>0)return;
  if(action==CONFIRM_QUIT){
   boolean quit=selected==1;
   dismiss();
   if(quit)c.app.quit();
   return;
  }
  if(action==HomeScreen.CONFIRM_OFFLINE){
   if(selected==1&&c.onlineAccount()&&!c.net.request("logout"))return;
   dismiss();
   return;
  }
  int callback=action,index=selected;opened=false;if(callback==DungeonScreen.CONFIRM_SOLO){if(index==0)c.dungeon.confirmSolo(skipHint);}else if(callback==StoneShop.CONFIRM_PURCHASE){if(index==1)c.stoneShop.confirmPurchase();}else if(callback==1){if(index==0)resumeOffline();else if(index==1)c.presentation.settings=true;else if(index==2)open("Thoát trận","Bạn muốn rời trận đang chơi?",new String[]{"Ở lại","Thoát trận"},3);}else if(callback==3){resumeOffline();if(index==1){if(c.onlineBattle())c.net.request("leave");else c.openLobby();}}else if(callback==4){c.notices.confirm();}else if(callback==2){c.net.departureVersion=c.net.departure.alertVersion;c.net.departureFinish=index==1;c.net.request("departure");}
 }
 void key(int key,int dir,boolean ok){
  if(c.gifts.showing){if(ok||key==-6||key==-7){c.gifts.showing=false;dismiss();}return;}
  layout(title,text,options);
  if(rows>visibleRows&&(dir==4||dir==8)){scroll=Battle.clamp(scroll+(dir==4?-1:1),0,rows-visibleRows);return;}
  if(action==4&&key==-6&&c.notices.hasAppeal()){c.notices.appeal();return;}
  if(key==-7){if(action!=2&&action!=4)dismiss();return;}
  if(soloHint()){
   if(dir==4){hintFocused=true;return;}
   if(dir==8){hintFocused=false;return;}
   if(hintFocused&&(ok||key==-6)){skipHint=!skipHint;return;}
   if(dir!=0)hintFocused=false;
  }
  if(dir!=0)selected=(selected+options.length+(dir==4||dir==1?-1:1))%options.length;else if(ok||key==-6)choose();
 }
 void pointer(int x,int y){
  if(c.gifts.showing){if(y>=188&&y<=220){c.gifts.showing=false;dismiss();}return;}
  layout(title,text,options);
  if(action==4&&c.notices.hasAppeal()&&x>=48&&x<272&&y>=135&&y<150){c.notices.appeal();return;}
  if(x>=left+width-25&&x<left+width-4&&y>=top+5&&y<top+25){if(action!=2&&action!=4)dismiss();return;}
  if(soloHint()&&x>=left+18&&x<left+38+c.pcWidth("Không nhắc nữa")&&y>=hintY()-2&&y<hintY()+14){
   skipHint=!skipHint;
   hintFocused=true;
   return;
  }
  int hit=choiceAt(options,x,y);if(hit>=0){selected=hit;choose();}
 }
}
