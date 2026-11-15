import javax.microedition.lcdui.Image;
final class ExchangePanel {
 final PhoneCanvas c;int selected,amount=1;Image frame,button,input;static final int[] QUICK={20,50,100};
 ExchangePanel(PhoneCanvas owner){c=owner;}
 void draw(){if(frame==null){frame=c.combatRoomArt("exchange744-panel");button=c.combatRoomArt("exchange744-button");input=c.combatRoomArt("exchange744-input");}c.img(frame,151,81);c.pcCenter("Đổi xu",230,88);c.pcCenter("100 xu = 1 tiền",230,108);c.pcText("Số tiền:",158,131);c.img(input,219,126);c.pcCenter(""+amount,254,131);c.img(button,252,152);c.pcCenter("Đổi",277,157);c.pcText("Đổi nhanh",158,157);for(int i=0;i<3;i++){int x=156+i*50;c.img(button,x,178);c.pcCenter(""+(QUICK[i]*100),x+23,183);}if(c.shop.focus!=18&&c.shop.focus!=6&&c.shop.focus!=7){int[] box=box(selected);c.drawItemSelection(box[0],box[1],box[2],box[3],false);}}
 int[] box(int id){return id==0?new int[]{219,126,72,20}:id==1?new int[]{252,152,46,22}:new int[]{156+(id-2)*50,178,46,22};}
 void convert(int value){if(value<=0||value>9999||value>c.coins/100||c.shoppingMoney>999999-value){c.popup.notice("Đổi xu","Số lượng không hợp lệ hoặc chưa đủ xu");return;}amount=value;if(c.onlineAccount()){if(c.net.roomId>0){c.popup.notice("Đổi xu","Rời phòng trước khi đổi xu");return;}if(c.net.busy||c.net.pending.length()>0){c.popup.notice("Đổi xu","Đang xử lý, vui lòng thử lại");return;}c.net.exchangeAmount=value;c.net.exchangeKey=Long.toString(System.currentTimeMillis())+"-"+c.game.rand(1000000);c.net.request("exchange");}else if(c.inventory.exchange(value))c.popup.notice("Đổi thành công","Đã nhận "+value+" tiền mua sắm");}
 void activate(){if(selected==0)c.shop.exchange();else convert(selected==1?amount:QUICK[selected-2]);}
 void key(int key,int dir,boolean ok){if(key==-7){c.shop.leave();return;}if(dir==4&&selected==0){c.shop.focus=18;return;}if(dir!=0)selected=(selected+5+(dir==1||dir==4?-1:1))%5;else if(ok||key==-6)activate();}
 boolean pointer(int x,int y){for(int i=0;i<5;i++){int[] b=box(i);if(x>=b[0]&&x<b[0]+b[2]&&y>=b[1]&&y<b[1]+b[3]){selected=i;c.shop.focus=17;activate();return true;}}return false;}
}
