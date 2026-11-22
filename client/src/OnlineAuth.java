import javax.microedition.lcdui.*;

/** Compact native login/registration popup over the main hall. */
final class OnlineAuth {
 final OnlineClient net;final PhoneCanvas c;int selected;boolean footerTouch;int footerHover=-1;
 Image logo,smallLogo,loginPanel,registerPanel,input,button,on,off,close;

 OnlineAuth(OnlineClient owner){net=owner;c=owner.c;}
 void reset(){selected=0;footerTouch=false;footerHover=-1;clearEditor();}
 void releaseArt(){logo=smallLogo=loginPanel=registerPanel=input=button=on=off=close=null;}
 void clearEditor(){c.typing.cancel();}
 int fields(){return net.registration?4:2;}
 TextField field(int i){return net.registration?(i==0?net.nameField:i==1?net.emailField:i==2?net.passwordField:net.confirmField):(i==0?net.emailField:net.passwordField);}
 String label(int i){return net.registration?(i==0?"Tên NV":i==1?"Email":i==2?"Mật khẩu":"Nhập lại"):(i==0?"Email":"Mật khẩu");}
 int rowY(int i){return net.registration?56+i*22:81+i*26;}
 int top(){return net.registration?29:49;}
 int checksY(){return net.registration?148:135;}
 int submitY(){return net.registration?164:153;}
 int linksY(){return net.registration?189:181;}
 boolean blocked(){return net.busy||net.pending.length()>0||net.wake.waiting;}
 void load(){if(loginPanel!=null)return;logo=c.combatRoomArt("auth714-logo");smallLogo=c.combatRoomArt("auth714-logo-small");loginPanel=c.combatRoomArt("auth714-login");registerPanel=c.combatRoomArt("auth714-register");input=c.combatRoomArt("auth714-input");button=c.combatRoomArt("auth714-button");on=c.combatRoomArt("auth-check-on");off=c.combatRoomArt("auth-check-off");close=c.combatRoomArt("auth714-close");}
 int[] box(int id){int n=fields();if(id>=100){int x=id==100?22:id==101?64:id==103?108:300;return new int[]{x-20,199,40,40};}if(id<n)return new int[]{105,rowY(id)-1,174,23};if(id==n)return new int[]{110,submitY()-1,100,25};if(id==n+1)return new int[]{168,linksY()-2,114,13};if(id==n+2)return new int[]{107,linksY()-2,51,13};if(id==n+3)return new int[]{268,top()+3,22,20};return new int[]{id==n+4?60:171,checksY()-2,id==n+4?106:108,14};}
 void draw(){load();int previous=c.lobbySelection;c.lobbySelection=selected>=100?selected-100:footerTouch?footerHover:-1;c.drawLobby();c.lobbySelection=previous;c.img(net.registration?smallLogo:logo,net.registration?135:118,0);c.img(net.registration?registerPanel:loginPanel,26,top());c.pcText(net.registration?"ĐĂNG KÝ TÀI KHOẢN":"ĐĂNG NHẬP",38,top()+8);c.img(close,269,top()+4);
  for(int i=0;i<fields();i++){int y=rowY(i);c.pcText(label(i),38,y+6);c.img(input,106,y);TextField f=field(i);String text=f==null?"":f.getString();if(f!=null&&(f.getConstraints()&TextField.PASSWORD)!=0)text="******************".substring(0,Math.min(18,text.length()));if(text.length()==0)text="OK để nhập";c.pcText(c.pcShort(text,158),112,y+6);}
  c.img(net.remember?on:off,62,checksY());c.pcText("Lưu mật khẩu",76,checksY()+1);c.img(net.autoLogin?on:off,173,checksY());c.pcText("Tự đăng nhập",187,checksY()+1);c.img(button,111,submitY());c.pcCenter(blocked()?"Đang nối "+Math.max(0,(net.transportSince==0?0:System.currentTimeMillis()-net.transportSince)/1000)+"s":net.registration?"Đăng ký":"Đăng nhập",160,submitY()+7);
  c.pcText("Máy chủ",112,linksY());c.pcText(net.registration?"Đã có TK? Đăng nhập":"Chưa có TK? Đăng ký",171,linksY());
  if(selected<100){int[] b=box(selected);c.drawSelectionGlow(b[0],b[1],b[2],b[3]);}else{int old=c.lobbySelection;c.lobbySelection=selected-100;c.drawLobbyTooltip();c.lobbySelection=old;}
 }
 void edit(final TextField field){
  if(field==null||blocked())return;
  int row=selected;
  c.typing.field(field.getLabel(),field.getString(),field.getMaxSize(),false,
      (field.getConstraints()&TextField.PASSWORD)!=0,110,rowY(row)+3,163,new InlineText.Done(){
   public void accept(String value){field.setString(value);}
  });
 }
 String serverUrl(){String url=net.webEndpoint.trim();while(url.endsWith("/"))url=url.substring(0,url.length()-1);return url.length()==0?"":url+"/download/";}
 void serverWeb(){try{String url=serverUrl();if(url.length()==0)throw new Exception();if(c.app.platformRequest(url))c.app.quit();}catch(Exception e){c.message("Chưa cấu hình địa chỉ web của Pi 5.");}}
 void toggleRemember(){net.remember=!net.remember;if(!net.remember){net.autoLogin=false;net.savedPassword="";}else if(net.passwordField!=null)net.savedPassword=net.passwordField.getString();net.authDraft();net.saveConfig();}
 void toggleAuto(){net.autoLogin=!net.autoLogin;if(net.autoLogin){net.remember=true;if(net.passwordField!=null&&net.passwordField.getString().length()>0)net.savedPassword=net.passwordField.getString();}net.authDraft();net.saveConfig();}
 void footerAction(int id){if(id!=4)return;if(id==0){c.openBag();c.bagReturnMode=16;}else if(id==1){c.openShop();c.shopReturnMode=16;}else if(id==3){c.infoReturnMode=16;c.mode=11;}else if(id==4)c.popup.confirmQuit();}
 void activate(){int n=fields();if(selected==n+3){if(blocked())net.cancelWake();else leave();return;}if(blocked())return;if(selected>=100){footerAction(selected-100);return;}if(net.emailField==null)net.loginForm(false);if(selected<n)edit(field(selected));else if(selected==n)net.commandAction(net.submit,(Displayable)net.form);else if(selected==n+1){net.authDraft();net.loginForm(!net.registration);}else if(selected==n+2)new ServerSettings(net).open();else if(selected==n+4)toggleRemember();else if(selected==n+5)toggleAuto();}
 void leave(){if(!blocked())c.popup.confirmQuit();}

 void move(int dir){int n=fields();if(selected==n-1&&dir==8){selected=n+4;return;}if((selected==n+4||selected==n+5)&&dir==8){selected=n;return;}if((selected==n+4||selected==n+5)&&dir==4){selected=n-1;return;}if(selected==n&&dir==4){selected=n+4;return;}int[] ids=new int[n+10];int count=0;for(int i=0;i<n+6;i++)ids[count++]=i;ids[count++]=104;int[] from=box(selected);int x=from[0]*2+from[2],y=from[1]*2+from[3],best=selected,score=999999;for(int i=0;i<count;i++){int[] b=box(ids[i]);int dx=b[0]*2+b[2]-x,dy=b[1]*2+b[3]-y,along=dir==1?-dx:dir==2?dx:dir==4?-dy:dy,cross=dir==1||dir==2?dy:dx;if(along<=0)continue;int cost=along+Math.abs(cross)*8;if(cost<score){score=cost;best=ids[i];}}selected=best;}
 void key(int key,int direction,boolean ok){if(key==-7||key==-6&&blocked()){if(blocked())net.cancelWake();else leave();return;}if(blocked()){if(ok&&selected==fields()+3)activate();return;}if(direction!=0)move(direction);else if(ok||key==-6)activate();}
 int footerHit(int x,int y){if(y<199)return -1;int id=c.lobbyHit(x,y);return id==4?4:-1;}
 void footerDrag(int x,int y){footerHover=footerHit(x,y);if(footerHover>=0)selected=100+footerHover;}
 void footerRelease(int x,int y){int id=footerHit(x,y);boolean open=footerTouch&&id>=0&&id==footerHover;footerTouch=false;footerHover=-1;if(open)footerAction(id);}
 void pointer(int x,int y){int n=fields();int[] closeBox=box(n+3);if(x>=closeBox[0]&&x<closeBox[0]+closeBox[2]&&y>=closeBox[1]&&y<closeBox[1]+closeBox[3]){selected=n+3;activate();return;}if(blocked())return;int id=footerHit(x,y);if(id>=0){footerTouch=true;footerHover=id;selected=100+id;return;}for(int i=0;i<n+6;i++){int[] b=box(i);if(x>=b[0]&&x<b[0]+b[2]&&y>=b[1]&&y<b[1]+b[3]){selected=i;activate();return;}}}
}
