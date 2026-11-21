import java.io.*;import javax.microedition.lcdui.Image;import javax.microedition.rms.RecordStore;
/** Item use and reward cards, with strictly separate offline counters. */
final class GiftBag {
 final PhoneCanvas c;final Image[] art=new Image[3];Image gold;boolean loaded,showing;int level=1,bags,tickets,requestType;long until;String requestKey="";int[] rewards=new int[0];
 GiftBag(PhoneCanvas canvas){c=canvas;}
 void releaseArt(){for(int i=0;i<art.length;i++)art[i]=null;gold=null;showing=false;}
 Image icon(int type){if(art[type]==null)art[type]=c.combatRoomArt("gift786-"+type);return art[type];}
 void load(){if(loaded)return;loaded=true;try{RecordStore rs=RecordStore.openRecordStore("gunny-gifts786",true);if(rs.getNumRecords()>0){DataInputStream in=new DataInputStream(new ByteArrayInputStream(rs.getRecord(1)));level=in.readInt();bags=in.readInt();tickets=in.readInt();until=in.readLong();}rs.closeRecordStore();}catch(Exception ignored){}}
 void save(){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(level);out.writeInt(bags);out.writeInt(tickets);out.writeLong(until);RecordStore rs=RecordStore.openRecordStore("gunny-gifts786",true);byte[] b=bytes.toByteArray();if(rs.getNumRecords()==0)rs.addRecord(b,0,b.length);else rs.setRecord(1,b,0,b.length);rs.closeRecordStore();}catch(Exception ignored){c.message("Chưa lưu được quà offline");}}
 int value(int type){if(c.onlineAccount())return type==0?c.economy.giftLevel:type==1?c.economy.giftBags:c.economy.vipTickets;load();return type==0?level:type==1?bags:tickets;}
 String name(int type){return type==0?"Hòm cấp "+value(0):type==1?"Túi quà phó bản":"Thẻ VIP 1 ngày";}
 boolean visible(int type){if(c.onlineAccount()&&!c.net.giftCap)return false;return type==0?value(0)<=70:value(type)>0;}
 static String quantity(int n){return n>=1000?""+(n/1000)+"k":""+n;}
 void awardOffline(){load();bags=Math.min(999999,bags+1);save();}
 void use(int type){if(c.onlineAccount()&&(c.net.inBattle||c.net.roomId>0&&c.net.phase!=0)){c.popup.notice("Sử dụng quà","Không mở quà khi đang trong trận");return;}
  if(type==0&&Progression.level(c.experience)<value(0)){c.popup.notice(name(type),"Cần đạt Lv "+value(0)+" để mở hòm");return;}
  if(c.onlineAccount()){if(!c.net.giftCap){c.message("Cập nhật máy chủ để mở quà");return;}if(c.net.busy||c.net.pending.length()>0)return;if(requestKey.length()==0||requestType!=type){requestType=type;requestKey=c.nextEconomyKey();}c.net.request("giftopen");return;}
  load();if(type==0){if(level>70)return;int lv=level++,stone=50+c.game.rand(51),coin=100*lv;c.economy.offlineStones=Math.min(999999,c.economy.offlineStones+stone);c.coins=Math.min(999999,c.coins+coin);rewards=lv<70?new int[]{0,level,1,coin,2,stone}:new int[]{1,coin,2,stone};}
  else if(type==1){if(bags<1)return;bags--;int stone=20+c.game.rand(51);c.economy.offlineMaterials[0]=Math.min(999999,c.economy.offlineMaterials[0]+stone);boolean vip=c.game.rand(100)<10;if(vip)tickets++;rewards=vip?new int[]{3,stone,4,1}:new int[]{3,stone};}
  else{if(tickets<1)return;tickets--;until=Math.max(until,System.currentTimeMillis()/1000)+86400;rewards=new int[]{5,1};}
  c.economy.save();c.saveSettings();save();show(type);
 }
 void read(DataInputStream in)throws IOException{int count=in.readUnsignedByte();if(count<1||count>4)throw new IOException("Sai quà");int[] next=new int[count*2];for(int i=0;i<next.length;i+=2){next[i]=in.readUnsignedByte();next[i+1]=in.readInt();if(next[i]>5||next[i+1]<1)throw new IOException("Sai phần thưởng");}rewards=next;requestKey="";show(requestType);c.bag.list();}
 void show(int type){c.popup.notice(type==0?"Hòm quà theo cấp":type==1?"Túi quà phó bản":"Sử dụng thẻ VIP","Bạn nhận được vật phẩm sau:");showing=true;}
 String rewardName(int type,int n){return type==0?"Hòm quà Lv "+n:type==1?"Xu":type==2?"Đá cường hóa Lv1":type==3?"Đá cường hóa Lv2":type==4?"Thẻ VIP 1 ngày":"VIP thêm 1 ngày";}
 Image rewardIcon(int type){return type==0?icon(0):type==2?c.stoneShop.icon(0):type==3?c.stoneShop.icon(1):type>=4?icon(2):gold();}
 Image gold(){if(gold==null)gold=c.combatRoomArt("result-gold");return gold;}
 long vipUntil(){if(c.onlineAccount())return c.economy.vipUntil;load();return until;}
 void draw(){NativePopup p=c.popup;if(p.frame==null){p.frame=c.combatRoomArt("settings743-popup");p.button=c.combatRoomArt("popup751-button");p.close=c.combatRoomArt("bag715-close");}p.width=292;p.height=204;p.left=14;p.top=18;p.choiceCount=1;p.rows=p.visibleRows=0;
  c.all();c.img(p.fitFrame(),14,18);c.pcCenter(p.title,160,27);c.pcCenter("Bạn nhận được vật phẩm sau:",160,53);
  for(int i=0;i<rewards.length/2;i++){int type=rewards[i*2],amount=rewards[i*2+1],x=25+i%2*136,y=76+i/2*50;c.rect(x,y,133,45,0x8c693a);c.img(rewardIcon(type),x+4,y+9);c.pcText(c.pcShort(rewardName(type,amount),96),x+31,y+7);c.pcText(type==0?"Nhận trong Túi":"x"+amount,x+31,y+23);}
  int w=p.buttonWidth("Đồng ý"),x=(320-w)/2;c.img(p.fittedButton(0,w),x,191);c.pcCenter("Đồng ý",160,196);c.drawSelectionGlow(x-1,190,w+2,24);
 }
}
