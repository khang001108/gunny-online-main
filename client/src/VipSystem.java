import java.io.*;
import javax.microedition.lcdui.Image;
/** Server-owned inventory and source-derived graphics. All image caches are screen-local. */
final class VipSystem {
 final PhoneCanvas c;int experience,owned,equipped=-1;final int[] cards=new int[13];boolean loaded;long poll;int item,shopFocus;String requestKey="",requestOp="";int requestItem;
 final Image[][] rings=new Image[3][3],wings=new Image[6][3];final Image[] badges=new Image[10],wingIcons=new Image[6],cardIcons=new Image[2];Image progress,fill;int barFrom,barTo;long barAt;
 static final int[] SIZE={72,44,24};
 VipSystem(PhoneCanvas c){this.c=c;}
 void resetAccount(){experience=owned=0;equipped=-1;loaded=false;poll=0;requestKey=requestOp="";for(int i=0;i<13;i++)cards[i]=0;}
 int exp(){return c.onlineAccount()?experience:0;}
 int wing(){return c.onlineAccount()?equipped:-1;}
 int hp(){return VipRules.hp(wing());}
 void release(){for(int i=0;i<3;i++)for(int j=0;j<3;j++)rings[i][j]=null;for(int i=0;i<6;i++)for(int j=0;j<3;j++)wings[i][j]=null;for(int i=0;i<10;i++)badges[i]=null;for(int i=0;i<6;i++)wingIcons[i]=null;cardIcons[0]=cardIcons[1]=null;progress=fill=sparkle=null;}
 void releaseLargeEffects(){for(int i=0;i<3;i++)rings[i][0]=null;for(int i=0;i<6;i++)wings[i][0]=null;}
 Image cardIcon(int n){int i=n==12?1:0;if(cardIcons[i]==null)cardIcons[i]=c.combatRoomArt("vip-card-"+i);return cardIcons[i];}
 Image wingIcon(int n){if(wingIcons[n]==null)wingIcons[n]=c.combatRoomArt("vip-wing-icon-"+n);return wingIcons[n];}
 String wingName(int n){return n==5?"Cánh phó bản":VipRules.WING_NAMES[n];}
 void read(DataInputStream in)throws IOException{int xp=in.readInt(),mask=in.readUnsignedByte(),wear=in.readByte();if(xp<0||xp>VipRules.TOTAL||mask>31||wear< -1||wear>5)throw new IOException("Sai trạng thái VIP");int[] next=new int[13];for(int i=0;i<13;i++){next[i]=in.readInt();if(next[i]<0||next[i]>999999)throw new IOException("Sai số thẻ VIP");}int money=in.readInt();barFrom=VipRules.level(xp)==VipRules.level(experience)&&VipRules.need(experience)>0?51*VipRules.within(experience)/VipRules.need(experience):0;barTo=VipRules.need(xp)==0?51:51*VipRules.within(xp)/VipRules.need(xp);barAt=System.currentTimeMillis();experience=xp;owned=mask;equipped=wear;for(int i=0;i<13;i++)cards[i]=next[i];c.shoppingMoney=money;loaded=true;poll=System.currentTimeMillis();c.bag.list();}
 void submit(String op,int selected){if(!c.onlineAccount()||!c.net.vipCap){c.message("Đăng nhập và cập nhật server VIP");return;}if(c.net.pending.startsWith("vip")||c.net.operation.equals("vipuse")||c.net.operation.equals("vipbuy")||c.net.operation.equals("vipequip"))return;if(!op.equals(requestOp)||selected!=requestItem||requestKey.length()==0){requestKey=c.nextEconomyKey();requestOp=op;requestItem=selected;}item=selected;c.net.request(op);}
 void progress(){if(progress==null){progress=NativeUi.fit(c.combatRoomArt("vip-progress"),55*c.s,7*c.s,2*c.s);fill=NativeUi.fit(c.combatRoomArt("vip-progress-fill"),51*c.s,3*c.s,0);}c.img(progress,83,44);int xp=exp(),need=VipRules.need(xp),width=need==0?51:51*VipRules.within(xp)/need;if(c.onlineAccount()&&System.currentTimeMillis()-barAt<700)width=barFrom+(barTo-barFrom)*(int)(System.currentTimeMillis()-barAt)/700;if(width>0)c.part(fill,0,0,width,3,85,46,false);c.pcCenter(need==0?"MAX":VipRules.within(xp)+"/"+need,110,52);badge(xp,87,64);}
 void badge(int xp,int x,int y){int lv=VipRules.level(xp);if(lv>0&&lv<=9){if(badges[lv]==null)badges[lv]=NativeUi.fit(c.combatRoomArt("vip-icon-"+lv),20*c.s,13*c.s,0);c.img(badges[lv],x,y);}else c.pcText(lv==0?"VIP0":"VIP"+lv,x,y);}
 void ring(int kind,int context,int frame,int x,int y){int size=SIZE[context];if(rings[kind][context]==null)rings[kind][context]=c.combatRoomArt("vip-ring-"+kind+"-"+size);c.part(rings[kind][context],frame%8*size,0,size,size,x-size/2,y-size,false);}
 void back(int xp,int wing,int x,int feet,int context,boolean burst){int lv=VipRules.level(xp),size=SIZE[context],phase=(c.frame/Math.max(3,18-lv)+(burst?c.frame/2:0))%8;
  if(lv>0){int base=lv<=9?(lv-1)/3:lv==10?1:2;ring(base,context,phase,x,feet);if(lv>=10)ring(lv==12?1:0,context,7-phase,x,feet);if(lv==12)ring(0,context,phase,x,feet);int count=burst?Math.min(8,2+(lv-1)/2):lv<=9?(lv-1)%3:3;for(int i=0;i<count;i++){int a=(phase+(burst?i:i*3))%8;int dx=a==0||a==4?0:a<4?size/3:-size/3,dy=a==2||a==6?size/2:a<2||a>6?size*5/6:size/6;c.img(c.forge.star==null?star():c.forge.star,x+dx-5,feet-dy-5);}}
  if(wing>=0&&wing<6){int h=context==0?48:context==1?30:18;if(wings[wing][context]==null)wings[wing][context]=c.combatRoomArt("vip-wing-"+wing+"-"+size);c.part(wings[wing][context],(c.frame/(wing>=3?5:8)%4)*size,0,size,h,x-size/2,feet-size*3/4,false);if(wing>=3&&c.frame%16<8){c.img(star(),x-size/3,feet-size*2/3);if(wing==4)c.img(star(),x+size/3-10,feet-size/2);}}
 }
 Image sparkle;Image star(){if(sparkle==null)sparkle=c.combatRoomArt("forge789-star");return sparkle;}
 int actorExp(int actor){return c.onlineBattle()?c.net.actorVip[actor]:actor==0?exp():0;}
 int actorWing(int actor){return c.onlineBattle()?c.net.actorWing[actor]:actor==0?wing():-1;}
 void drawShop(){c.pcCenter("VIP "+VipRules.level(exp())+" • "+(VipRules.need(exp())==0?"MAX":VipRules.within(exp())+"/"+VipRules.need(exp())),229,67);for(int i=0;i<5;i++){int[] b=c.shop.box(i);c.img(c.shop.card,b[0],b[1]);c.img(wingIcon(i),b[0]+3,b[1]+3);c.pcText(c.pcShort(wingName(i),48),b[0]+27,b[1]+3);c.pcText("+"+VipRules.WING_HP[i]+" HP",b[0]+27,b[1]+15);c.pcText(VipRules.level(exp())<VipRules.WING_VIP[i]?"Khóa VIP"+VipRules.WING_VIP[i]:(owned&(1<<i))!=0?"Đã có":""+VipRules.WING_PRICE[i]+" tiền",b[0]+4,b[1]+28);if(i==shopFocus)c.drawItemSelection(b[0],b[1],b[2],b[3],false);}c.pcCenter("OK / B: mua • Mềm trái: thông tin",229,213);}
 void shopKey(int key,int dir,boolean ok){if(key==-7){c.shop.leave();return;}if(key==-6){c.popup.notice(wingName(shopFocus),"Cần VIP "+VipRules.WING_VIP[shopFocus]+". +"+VipRules.WING_HP[shopFocus]+" HP khi trang bị. Giá "+VipRules.WING_PRICE[shopFocus]+" tiền. Dùng vĩnh viễn.");return;}if(dir==4&&shopFocus<2){c.shop.focus=43;return;}if(dir!=0)shopFocus=Battle.clamp(shopFocus+(dir==1?-1:dir==2?1:dir==4?-2:2),0,4);else if(ok||key=='b'||key=='B')buy();c.shop.focus=shopFocus;}
 void buy(){if(VipRules.level(exp())<VipRules.WING_VIP[shopFocus]){c.message("Cần VIP "+VipRules.WING_VIP[shopFocus]);return;}if((owned&(1<<shopFocus))!=0){c.message("Đã sở hữu cánh");return;}submit("vipbuy",shopFocus);}
}
