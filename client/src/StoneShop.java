import javax.microedition.lcdui.Image;

/** Stone grades and consumable charms share the existing shop cards. */
final class StoneShop {
 static final int[] PACKS={1,10,50,100,500,1000};
 static final int CONFIRM_PURCHASE=5;
 static final String[] NAMES={"Đá cấp 1","Đá cấp 2","Đá cấp 3","Đá cấp 4","Đá cấp 5",
     "Bùa may 25%","Bùa bảo vệ"};
 final PhoneCanvas c;
 int focus;
 private int confirmedType,requestType=-1;
 final Image[] icons=new Image[7];
 String requestKey="";
 StoneShop(PhoneCanvas canvas){c=canvas;}
 int quantity(){return 1;}
 int price(){return ForgeRules.price(focus);}
 Image icon(int type){
  if(icons[type]==null)icons[type]=c.combatRoomArt("craft-material-"+type);
  return icons[type];
 }
 void draw(){
  c.pcCenter("NGUYÊN LIỆU CƯỜNG HÓA",229,68);
  int first=focus/6*6;
  for(int cell=0;cell<6&&first+cell<7;cell++){
   int type=first+cell;int[] box=c.shop.box(cell);int x=box[0],y=box[1];
   c.img(c.shop.card,x,y);c.img(icon(type),x+3,y+8);
   c.pcText(NAMES[type],x+28,y+5);c.img(c.shop.money,x+29,y+26);
   c.pcText(""+ForgeRules.price(type),x+43,y+26);
   if(focus==type&&c.shop.focus<6)c.drawItemSelection(x,y,box[2],box[3],false);
  }
  if(infoType>=0)drawInfo(infoType,c.shop.box(focus%6));
  c.pcCenter("Có "+c.economy.material(focus)+" • "+(focus/6+1)+"/2",229,214);
 }
 void buy(){
  if(c.net.pending.length()>0||c.net.operation.equals("buycraft"))return;
  if(c.net.roomId>0){c.message("Rời phòng trước khi mua vật liệu");return;}
  if(c.onlineAccount()&&!c.net.forgeChanceCap){c.message("Cập nhật máy chủ cường hóa");return;}
  confirmedType=focus;
  c.popup.open("Mua nguyên liệu","Mua 1 "+NAMES[focus]+" với "+price()+" tiền?",
      new String[]{"Hủy","Mua"},CONFIRM_PURCHASE);
 }
 void confirmPurchase(){
  if(c.mode!=6||c.shop.tab!=3||c.net.roomId>0||c.net.pending.length()>0)return;
  if(c.onlineAccount()){
   if(!c.net.forgeChanceCap)return;
   if(requestKey.length()==0||requestType!=confirmedType){
    requestKey=c.nextEconomyKey();requestType=confirmedType;
   }
   c.net.craftMaterial=confirmedType;c.net.stoneQuantity=1;c.net.economyKey=requestKey;
   c.net.request("buycraft");
  }else if(c.economy.buyMaterial(confirmedType,1))c.message("Đã mua "+NAMES[confirmedType]);
 }
 int infoType=-1;
 String description(int type){return (type<5?"Tăng tỉ lệ cường hóa theo cấp đá. Tiêu hao kể cả thất bại.":type==5?"Tăng 25% tỉ lệ gốc. Ví dụ 40% thành 50%. Tiêu hao mỗi lần rèn.":"Không giảm cấp khi rèn thất bại từ +5. Tiêu hao mỗi lần rèn.")+"\nCó: "+c.economy.material(type)+"\nGiá: "+ForgeRules.price(type);}
 void info(int type){infoType=infoType==type?-1:type;}
 void drawInfo(int type,int[] anchor){c.tooltip.goods(NAMES[type],type<5?"Đá Lv"+(type+1):"Bùa",description(type),anchor);}
 void key(int key,int dir,boolean ok){
  if(key==-6){info(focus);return;}

  if(infoType>=0){infoType=-1;if(key==-6||key==-7||ok)return;}
  if(key==-7){c.shop.leave();return;}
  if(c.shop.focus==14){
   if(dir!=0)c.shop.focus=focus%6;else if(ok)buy();return;
  }
  if(dir==1&&focus>0)focus--;
  else if(dir==2&&focus<6)focus++;
  else if(dir==4){if(focus%6>=2)focus-=2;else{c.shop.focus=19;return;}}
  else if(dir==8){if(focus+2<7)focus+=2;else{c.shop.focus=14;return;}}
  else if(ok)buy();
  c.shop.focus=focus%6;
 }
 boolean pointer(int x,int y){
  int first=focus/6*6;
  for(int cell=0;cell<6&&first+cell<7;cell++){
   int[] b=c.shop.box(cell);
   if(x>=b[0]&&x<b[0]+b[2]&&y>=b[1]&&y<b[1]+b[3]){
    focus=first+cell;c.shop.focus=cell;buy();return true;
   }
  }
  return false;
 }
}
