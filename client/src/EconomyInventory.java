import java.io.*;import javax.microedition.rms.RecordStore;
/** Separate RMS record preserves the original offline inventory and login snapshot. */
final class EconomyInventory {
 final PhoneCanvas c;int offlineStones,onlineStones;boolean offlineClear,onlineClear,loaded;final int[] offlineRanks=new int[130],onlineRanks=new int[130],offlineProgress=new int[130],onlineProgress=new int[130];
 final int[] offlineMaterials=new int[6],onlineMaterials=new int[6];
 int wings,vipSpears;boolean wingEquipped;
 final java.util.Random random=new java.util.Random();
 int material(int type){return type==0?stones():(c.onlineAccount()?onlineMaterials:offlineMaterials)[type-1];}
 EconomyInventory(PhoneCanvas canvas){c=canvas;}
 void resetOnline(){
  loaded=false;wings=vipSpears=0;wingEquipped=false;
  onlineStones=0;for(int i=0;i<6;i++)onlineMaterials[i]=0;
  onlineClear=false;
  for(int i=0;i<onlineRanks.length;i++)onlineRanks[i]=onlineProgress[i]=0;
 }
 int stones(){return c.onlineAccount()?onlineStones:offlineStones;}int[] ranks(){return c.onlineAccount()?onlineRanks:offlineRanks;}int[] progress(){return c.onlineAccount()?onlineProgress:offlineProgress;}boolean clear(){return c.onlineAccount()?onlineClear:offlineClear;}
 void load(){try{RecordStore r=RecordStore.openRecordStore("gunny-economy780",true);if(r.getNumRecords()>0){DataInputStream in=new DataInputStream(new ByteArrayInputStream(r.getRecord(1)));int version=in.readInt();if(version!=780&&version!=781&&version!=784)throw new IOException();offlineStones=in.readInt();offlineClear=in.readBoolean();for(int i=0;i<130;i++)offlineRanks[i]=Math.min(ForgeRules.MAX_RANK,in.readUnsignedByte());if(version>=781)for(int i=0;i<130;i++)offlineProgress[i]=in.readUnsignedShort();if(version>=784)for(int i=0;i<6;i++)offlineMaterials[i]=in.readInt();}r.closeRecordStore();}catch(Exception ignored){}}
 void save(){if(c.onlineAccount())return;try{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);o.writeInt(784);o.writeInt(offlineStones);o.writeBoolean(offlineClear);for(int i=0;i<130;i++)o.writeByte(offlineRanks[i]);for(int i=0;i<130;i++)o.writeShort(offlineProgress[i]);for(int i=0;i<6;i++)o.writeInt(offlineMaterials[i]);o.close();RecordStore r=RecordStore.openRecordStore("gunny-economy780",true);byte[] data=b.toByteArray();if(r.getNumRecords()==0)r.addRecord(data,0,data.length);else r.setRecord(1,data,0,data.length);r.closeRecordStore();}catch(Exception ignored){}}
 void read(DataInputStream in)throws IOException{onlineStones=in.readInt();onlineClear=in.readBoolean();if(onlineStones<0)throw new IOException("Sai số đá");for(int i=0;i<130;i++){onlineRanks[i]=in.readUnsignedByte();if(onlineRanks[i]>ForgeRules.MAX_RANK)throw new IOException("Sai cấp rèn");}for(int i=0;i<130;i++){onlineProgress[i]=in.readUnsignedShort();if(onlineRanks[i]>=ForgeRules.MAX_RANK||onlineProgress[i]>=ForgeRules.cost(onlineRanks[i]))onlineProgress[i]=0;}if(in.available()>=24)for(int i=0;i<6;i++)onlineMaterials[i]=in.readInt();if(in.available()>=9){wings=in.readInt();wingEquipped=in.readBoolean();vipSpears=in.readInt();}loaded=true;}
 int[] bonuses(){return ForgeRules.equipped(ranks(),c.gender,c.wornMask,c.equipped,c.skin,c.look);}
 boolean buy(int quantity){if(c.onlineAccount())return false;if(quantity<1||quantity>1000||c.shoppingMoney<quantity*80||offlineStones>999999-quantity){c.message("Chưa đủ tiền hoặc số lượng không hợp lệ");return false;}c.shoppingMoney-=quantity*80;offlineStones+=quantity;c.inventory.save();c.saveSettings();save();return true;}
 boolean buyMaterial(int type,int quantity){
  if(c.onlineAccount())return false;
  int cost=ForgeRules.price(type)*quantity;
  if(type<0||type>6||quantity<1||quantity>1000||c.shoppingMoney<cost||material(type)>999999-quantity){
   c.message("Chưa đủ tiền hoặc số lượng không hợp lệ");return false;
  }
  c.shoppingMoney-=cost;
  if(type==0)offlineStones+=quantity;else offlineMaterials[type-1]+=quantity;
  c.saveSettings();save();return true;
 }
 boolean forgeChance(int kind,int item,int expected,int grade,int quantity,boolean lucky,boolean protect){
  if(c.onlineAccount())return false;
  int index=ForgeRules.index(c.gender,kind,item),chance=ForgeRules.chance(expected,grade,quantity,lucky);
  if(!ForgeRules.allowed(kind)||!c.inventory.owns(kind,item)||offlineRanks[index]!=expected||chance==0){
   c.message("Trang bị hoặc cấp rèn đã thay đổi");return false;
  }
  if(material(grade-1)<quantity||lucky&&material(5)<1||protect&&material(6)<1){
   c.message("Thiếu đá hoặc bùa đã chọn");return false;
  }
  if(grade==1)offlineStones-=quantity;else offlineMaterials[grade-2]-=quantity;
  if(lucky)offlineMaterials[4]--;if(protect)offlineMaterials[5]--;
  offlineRanks[index]=ForgeRules.result(expected,random.nextInt(10000)<chance,protect);
  offlineProgress[index]=0;save();return true;
 }
 boolean forge(int kind,int item,int expected,int expectedProgress){
  return forgeChance(kind,item,expected,1,1,false,false);
 }
}
