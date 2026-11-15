import javax.microedition.lcdui.Image;

/** The props tab sells one selected stone bundle at a time. */
final class StoneShop {
 static final int[] PACKS = {1,10,50,100,500,1000};
 static final int CONFIRM_PURCHASE = 5;
 final PhoneCanvas c;
 int focus;
 private int confirmedQuantity;
 Image stone;
 String requestKey = "";
 private int requestQuantity;

 StoneShop(PhoneCanvas canvas) { c = canvas; }

 int quantity() { return PACKS[focus]; }

 void draw() {
  if (stone == null) stone = c.combatRoomArt("stone780");
  c.pcCenter("ĐÁ CƯỜNG HÓA",229,68);
  for (int i = 0; i < PACKS.length; i++) {
   int[] box = c.shop.box(i);
   int x = box[0], y = box[1];
   c.img(c.shop.card,x,y);
   c.img(stone,x+3,y+8);
   c.pcText(PACKS[i]+" đá",x+30,y+5);
   c.img(c.shop.money,x+29,y+26);
   c.pcText(""+(PACKS[i]*80),x+43,y+26);
   if (focus == i && c.shop.focus < 6)
    c.drawItemSelection(x,y,box[2],box[3],false);
  }
  c.pcCenter("Đang có: "+c.economy.stones()+" đá",229,214);
 }

 void buy() {
  if (c.net.pending.length()>0 || c.net.operation.equals("buystones")) return;
  if (c.net.roomId>0) { c.message("Rời phòng trước khi mua đá"); return; }
  if (c.onlineAccount() && !c.net.expansionCap) {
   c.message("Cập nhật máy chủ 7.8.0");
   return;
  }
  confirmedQuantity = quantity();
  c.popup.open("Mua đá cường hóa",
   "Mua "+confirmedQuantity+" đá với "+(confirmedQuantity*80)+" tiền?",
   new String[]{"Hủy","Mua"},CONFIRM_PURCHASE);
 }

 void confirmPurchase() {
  int quantity = confirmedQuantity;
  if (c.mode != 6 || c.shop.tab != 3 || c.net.roomId>0
      || c.net.pending.length()>0 || c.net.operation.equals("buystones")) return;
  if (c.onlineAccount()) {
   if (!c.net.expansionCap) return;
   if (requestKey.length()==0 || requestQuantity!=quantity) {
    requestKey=c.nextEconomyKey();
    requestQuantity=quantity;
   }
   c.net.stoneQuantity=quantity;
   c.net.economyKey=requestKey;
   c.net.request("buystones");
  } else if (c.economy.buy(quantity)) {
   c.message("Đã mua "+quantity+" đá");
  }
 }

 void key(int key,int dir,boolean ok) {
  if (key==-7) { c.shop.leave(); return; }
  if (c.shop.focus==14) {
   if (dir!=0) c.shop.focus=focus;
   else if (ok||key==-6) buy();
   return;
  }
  int row=focus/2,col=focus%2;
  if (dir==1 && col>0) focus--;
  else if (dir==2 && col<1) focus++;
  else if (dir==4) {
   if (row>0) focus-=2;
   else { c.shop.focus=19; return; }
  } else if (dir==8) {
   if (row<2) focus+=2;
   else { c.shop.focus=14; return; }
  } else if (ok||key==-6) {
   buy();
  }
  c.shop.focus=focus;
 }

 boolean pointer(int x,int y) {
  for (int i=0;i<PACKS.length;i++) {
   int[] box=c.shop.box(i);
   if (x>=box[0] && x<box[0]+box[2] && y>=box[1] && y<box[1]+box[3]) {
    focus=c.shop.focus=i;
    buy();
    return true;
   }
  }
  return false;
 }
}
