import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.RecordStore;

/** Bind raw MIDP key codes; emulator/controller buttons work when delivered as key events. */
final class KeyBindings implements CommandListener {
 final PhoneCanvas c;
 static final int[] DEFAULT={52,54,50,56,32,-6,-7,'q','w','e','r','t','b','v','f','z','x','c','i','s','*','h','@','p','l'};
 static final String[] LABEL={"Trái / di chuyển","Phải / di chuyển","Lên / tăng góc","Xuống / giảm góc","OK / giữ lực / bắn","Phím mềm trái / camera","Trở về / tạm dừng","Buff +2","Buff ba tia","Buff sát thương","Hồi HP","Bay","POW","Bỏ lượt","Đổi vũ khí","Đạo cụ 1","Đạo cụ 2","Đạo cụ 3","Ba lô","Cửa hàng","Thiết lập","Đấu bot","Biểu cảm","Túi trang bị","Túi thời trang"};
 final int[] keys=new int[DEFAULT.length];final boolean[] custom=new boolean[DEFAULT.length];
 Form form;ChoiceGroup choice;int capture=-1;boolean releaseCapture;
 final Command change=new Command("Gán phím",Command.OK,1),reset=new Command("Mặc định",Command.SCREEN,2),back=new Command("Trở về",Command.BACK,3);
 KeyBindings(PhoneCanvas canvas){c=canvas;defaults();try{RecordStore r=RecordStore.openRecordStore("gunny-keybindings",true);if(r.getNumRecords()>0){DataInputStream in=new DataInputStream(new ByteArrayInputStream(r.getRecord(1)));if(in.readInt()==1){int count=in.readUnsignedByte();for(int i=0;i<count;i++){boolean changed=in.readBoolean();int value=in.readInt();if(i<keys.length){custom[i]=changed;keys[i]=value;}}}in.close();}r.closeRecordStore();}catch(Exception ignored){}}
 void defaults(){for(int i=0;i<keys.length;i++){keys[i]=DEFAULT[i];custom[i]=false;}}
 void save(){try{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);out.writeInt(1);out.writeByte(keys.length);for(int i=0;i<keys.length;i++){out.writeBoolean(custom[i]);out.writeInt(keys[i]);}out.close();RecordStore r=RecordStore.openRecordStore("gunny-keybindings",true);byte[] data=b.toByteArray();if(r.getNumRecords()==0)r.addRecord(data,0,data.length);else r.setRecord(1,data,0,data.length);r.closeRecordStore();}catch(Exception ignored){}}
 int map(int raw){for(int i=0;i<keys.length;i++)if(custom[i]&&keys[i]==raw)return DEFAULT[i];return raw;}
 String name(int action){for(int i=0;i<DEFAULT.length;i++)if(DEFAULT[i]==action)return nameAt(i);return ""+action;}
 String nameAt(int index){int key=keys[index];if(!custom[index]){if(index==0)return "Trái";if(index==1)return "Phải";if(index==2)return "Lên";if(index==3)return "Xuống";if(index==4)return "OK/Space";if(index==5)return "Mềm trái";if(index==6)return "Mềm phải";}
  if(custom[index]&&key>127)return "#"+key;
  try{String label=c.getKeyName(key);if(label!=null&&label.length()>0)return label;}catch(Exception ignored){}
  return key>=33&&key<127?""+(char)key:"#"+key;
 }
 void open(){form=new Form("Gán phím điều khiển");choice=new ChoiceGroup("Chọn chức năng",ChoiceGroup.EXCLUSIVE);for(int i=0;i<keys.length;i++)choice.append(LABEL[i]+": "+nameAt(i),null);form.append("Chọn chức năng, bấm Gán phím, rồi nhấn nút bàn phím hoặc tay cầm. Nút phải được trình giả lập chuyển thành phím Java ME.");form.append(choice);form.addCommand(change);form.addCommand(reset);form.addCommand(back);form.setCommandListener(this);Display.getDisplay(c.app).setCurrent(form);}
 public void commandAction(Command command,Displayable source){if(command==back){capture=-1;Display.getDisplay(c.app).setCurrent(c);return;}if(command==reset){defaults();save();open();return;}if(command==change){capture=choice.getSelectedIndex();c.held=c.fireKey=c.cameraHeld=0;Display.getDisplay(c.app).setCurrent(c);}}
 boolean press(int raw){if(capture<0)return false;int index=capture;for(int i=0;i<keys.length;i++)if(i!=index&&custom[i]&&keys[i]==raw){c.message("Phím đang dùng cho "+LABEL[i]+". Chọn phím khác.");return true;}keys[index]=raw;custom[index]=true;capture=-1;releaseCapture=true;save();open();choice.setSelectedIndex(index,true);return true;}
 void draw(){if(capture>=0)c.popup.render("NHẤN PHÍM MỚI",LABEL[capture]+"\nNhấn nút muốn gán. Có thể dùng nút tay cầm qua giả lập.",new String[]{"Chờ phím..."},0);}
 String help(){return name(52)+"/"+name(54)+": di chuyển; "+name(50)+"/"+name(56)+": góc. Giữ "+name(32)+": lấy lực, thả để bắn. "+name('z')+"/"+name('x')+"/"+name('c')+": đạo cụ; "+name('q')+"/"+name('w')+"/"+name('e')+"/"+name('r')+"/"+name('t')+": buff; "+name('b')+": POW; "+name('v')+": bỏ lượt.";}
}
