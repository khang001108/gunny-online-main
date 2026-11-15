import java.io.*;import javax.microedition.rms.RecordStore;
/** Sparse slot positions; offline and authenticated account layouts never share storage. */
final class BagLayout {
 final PhoneCanvas c;final int[] offline=new int[175],online=new int[175],before=new int[175];boolean loaded,pending;
 BagLayout(PhoneCanvas owner){c=owner;for(int i=0;i<175;i++)offline[i]=online[i]=-1;}
 void resetOnline(){pending=false;for(int i=0;i<175;i++)online[i]=-1;}
 int[] current(){if(c.onlineAccount())return online;if(!loaded){loaded=true;try{RecordStore r=RecordStore.openRecordStore("gunny-layout753",true);if(r.getNumRecords()>0){DataInputStream in=new DataInputStream(new ByteArrayInputStream(r.getRecord(1)));for(int i=0;i<175;i++)offline[i]=in.readShort();}r.closeRecordStore();}catch(Exception ignored){}}return offline;}
 int start(int tab){return tab==0?0:25+c.gender*75;}
 int capacity(int tab){return tab==0?25:75;}
 boolean visible(int value,int tab){if(value<0||value>69)return false;int k=value/10,item=value%10;if(tab==0?(k!=0||!PhoneCanvas.enabledWeapon(item)):k<1)return false;if(!c.inventory.owns(k,item))return false;return(c.wornMask&(1<<k))==0||c.bag.wornItem(k)!=item;}
 int fill(int tab,int filter,int[] kinds,int[] items){int[] slots=current();int offset=start(tab),cap=capacity(tab);boolean[] seen=new boolean[70];for(int i=0;i<cap;i++){int value=slots[offset+i];if(!visible(value,tab)||seen[value])slots[offset+i]=-1;else seen[value]=true;}for(int k=tab==0?0:1;k<=(tab==0?0:6);k++)for(int item=0;item<10;item++){int value=k*10+item;if(visible(value,tab)&&!seen[value]){for(int i=0;i<cap;i++)if(slots[offset+i]<0){slots[offset+i]=value;seen[value]=true;break;}}}int count=0;for(int i=0;i<kinds.length;i++){int value=i<cap?slots[offset+i]:-1;if(value>=0&&(filter==0||value/10==filter)){kinds[i]=value/10;items[i]=value%10;count=i+1;}else kinds[i]=items[i]=-1;}return count;}
 void read(DataInputStream in)throws IOException{if(in.readUnsignedShort()!=175)throw new IOException("Sai vị trí túi");for(int i=0;i<175;i++){int v=in.readShort();if(v< -1||v>69)throw new IOException("Sai vật phẩm");online[i]=v;}}
 boolean move(int tab,int source,int target){if(source<0||target<0||source>=capacity(tab)||target>=capacity(tab))return false;if(c.onlineAccount()&&(!c.net.bagCap||c.net.busy||c.net.pending.length()>0||pending)){c.message("Chờ máy chủ sẵn sàng để sắp xếp túi");return false;}int[] slots=current();int a=start(tab)+source,b=start(tab)+target;if(slots[a]<0||slots[b]>=0)return false;for(int i=0;i<175;i++)before[i]=slots[i];slots[b]=slots[a];slots[a]=-1;if(c.onlineAccount()){pending=true;c.net.request("baglayout");}else save();return true;}
 void save(){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);for(int i=0;i<175;i++)out.writeShort(offline[i]);RecordStore r=RecordStore.openRecordStore("gunny-layout753",true);byte[] b=bytes.toByteArray();if(r.getNumRecords()==0)r.addRecord(b,0,b.length);else r.setRecord(1,b,0,b.length);r.closeRecordStore();}catch(Exception ignored){c.message("Chưa lưu được vị trí túi");}}
 void rollback(){if(pending){for(int i=0;i<175;i++)online[i]=before[i];pending=false;}}
}
