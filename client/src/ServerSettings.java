import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.RecordStore;

/** Editable server presets, saved on the handset independently of credentials. */
final class ServerSettings implements CommandListener {
    final OnlineClient net;
    final Form form=new Form("Máy chủ");
    final ChoiceGroup choice=new ChoiceGroup("Kết nối bằng",Choice.EXCLUSIVE);
    final TextField internet=new TextField("4G / mạng khác","socket://gunnyjava.xubi.org:22123",180,TextField.URL);
    final TextField wifi=new TextField("Wi-Fi cùng Pi","socket://192.168.1.28:22123",180,TextField.URL);
    final Command save=new Command("Lưu",Command.OK,1),back=new Command("Trở về",Command.BACK,2);

    ServerSettings(OnlineClient owner){net=owner;}
    void open(){
        net.authDraft();
        RecordStore rs=null;
        try{
            rs=RecordStore.openRecordStore("gunny-servers",true);
            if(rs.getNumRecords()>0){
                DataInputStream in=new DataInputStream(new ByteArrayInputStream(rs.getRecord(1)));
                internet.setString(in.readUTF());wifi.setString(in.readUTF());in.close();
            }
        }catch(Exception ignored){}finally{if(rs!=null)try{rs.closeRecordStore();}catch(Exception ignored){}}
        choice.append("4G / mạng khác",null);choice.append("Wi-Fi cùng Pi",null);
        if(net.endpoint.equals(wifi.getString()))choice.setSelectedIndex(1,true);
        else if(!net.endpoint.equals(internet.getString()))internet.setString(net.endpoint);
        form.append(choice);form.append(internet);form.append(wifi);
        form.append("Chọn mạng rồi Lưu. Có thể sửa cả hai địa chỉ khi đổi máy chủ. Wi-Fi cùng Pi chỉ dùng trong mạng nhà.");
        form.addCommand(save);form.addCommand(back);form.setCommandListener(this);
        Display.getDisplay(net.c.app).setCurrent(form);
    }
    public void commandAction(Command command,Displayable screen){
        if(command==back){Display.getDisplay(net.c.app).setCurrent(net.c);return;}
        if(command!=save)return;
        RecordStore rs=null;
        try{
            String outside=internet.getString().trim(),inside=wifi.getString().trim();
            OnlineClient.validateEndpoint(outside);OnlineClient.validateEndpoint(inside);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            out.writeUTF(outside);out.writeUTF(inside);out.close();
            rs=RecordStore.openRecordStore("gunny-servers",true);byte[] data=bytes.toByteArray();
            if(rs.getNumRecords()==0)rs.addRecord(data,0,data.length);else rs.setRecord(1,data,0,data.length);
            net.endpoint=choice.getSelectedIndex()==1?inside:outside;
            if(net.addressField!=null)net.addressField.setString(net.endpoint);
            net.tcp.close();net.wake.invalidate();net.saveConfig();
            Display.getDisplay(net.c.app).setCurrent(net.c);
        }catch(Exception error){
            Alert alert=new Alert("Máy chủ",error.getMessage(),null,AlertType.ERROR);
            alert.setTimeout(Alert.FOREVER);Display.getDisplay(net.c.app).setCurrent(alert,form);
        }finally{if(rs!=null)try{rs.closeRecordStore();}catch(Exception ignored){}}
    }
}
