import java.io.*;import javax.microedition.rms.RecordStore;
/** Separate RMS record preserves the original offline inventory and login snapshot. */
final class EconomyInventory {
 final PhoneCanvas c;int offlineStones,onlineStones;boolean offlineClear,onlineClear,loaded;final int[] offlineRanks=new int[130],onlineRanks=new int[130],offlineProgress=new int[130],onlineProgress=new int[130];
 EconomyInventory(PhoneCanvas canvas){c=canvas;}
 void resetOnline(){
  loaded=false;
  onlineStones=0;
  onlineClear=false;
  for(int i=0;i<onlineRanks.length;i++)onlineRanks[i]=onlineProgress[i]=0;
 }
 int stones(){return c.onlineAccount()?onlineStones:offlineStones;}int[] ranks(){return c.onlineAccount()?onlineRanks:offlineRanks;}int[] progress(){return c.onlineAccount()?onlineProgress:offlineProgress;}boolean clear(){return c.onlineAccount()?onlineClear:offlineClear;}
 void load(){try{RecordStore r=RecordStore.openRecordStore("gunny-economy780",true);if(r.getNumRecords()>0){DataInputStream in=new DataInputStream(new ByteArrayInputStream(r.getRecord(1)));int version=in.readInt();if(version!=780&&version!=781)throw new IOException();offlineStones=in.readInt();offlineClear=in.readBoolean();for(int i=0;i<130;i++)offlineRanks[i]=Math.min(ForgeRules.MAX_RANK,in.readUnsignedByte());if(version==781)for(int i=0;i<130;i++)offlineProgress[i]=in.readUnsignedShort();}r.closeRecordStore();}catch(Exception ignored){}}
 void save(){if(c.onlineAccount())return;try{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);o.writeInt(781);o.writeInt(offlineStones);o.writeBoolean(offlineClear);for(int i=0;i<130;i++)o.writeByte(offlineRanks[i]);for(int i=0;i<130;i++)o.writeShort(offlineProgress[i]);o.close();RecordStore r=RecordStore.openRecordStore("gunny-economy780",true);byte[] data=b.toByteArray();if(r.getNumRecords()==0)r.addRecord(data,0,data.length);else r.setRecord(1,data,0,data.length);r.closeRecordStore();}catch(Exception ignored){}}
 void read(DataInputStream in)throws IOException{onlineStones=in.readInt();onlineClear=in.readBoolean();if(onlineStones<0)throw new IOException("Sai số đá");for(int i=0;i<130;i++){onlineRanks[i]=in.readUnsignedByte();if(onlineRanks[i]>ForgeRules.MAX_RANK)throw new IOException("Sai cấp rèn");}for(int i=0;i<130;i++){onlineProgress[i]=in.readUnsignedShort();if(onlineRanks[i]>=ForgeRules.MAX_RANK||onlineProgress[i]>=ForgeRules.cost(onlineRanks[i]))onlineProgress[i]=0;}loaded=true;}
 int[] bonuses(){return ForgeRules.equipped(ranks(),c.gender,c.wornMask,c.equipped,c.skin,c.look);}
 boolean buy(int quantity){if(c.onlineAccount())return false;if(quantity<1||quantity>1000||c.shoppingMoney<quantity*80||offlineStones>999999-quantity){c.message("Chưa đủ tiền hoặc số lượng không hợp lệ");return false;}c.shoppingMoney-=quantity*80;offlineStones+=quantity;c.inventory.save();c.saveSettings();save();return true;}
 boolean forge(int kind,int item,int expected,int expectedProgress){if(c.onlineAccount())return false;int i=ForgeRules.index(c.gender,kind,item);if(!ForgeRules.allowed(kind)||!c.inventory.owns(kind,item)||offlineRanks[i]!=expected||offlineProgress[i]!=expectedProgress){c.message("Tiến độ cường hóa đã thay đổi");return false;}if(expected>=ForgeRules.MAX_RANK||offlineStones<=0){c.message(expected>=ForgeRules.MAX_RANK?"Đã đạt +15":"Không còn đá cường hóa");return false;}int spend=Math.min(offlineStones,ForgeRules.cost(expected)-offlineProgress[i]);offlineStones-=spend;offlineProgress[i]+=spend;if(offlineProgress[i]>=ForgeRules.cost(expected)){offlineProgress[i]=0;offlineRanks[i]++;}save();return true;}
}
