import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;

/** Installable MIDP 2.0 entry point for Nokia S40 and Symbian Java. */
public final class GunnyMIDlet extends MIDlet {
    static final String FALLBACK_VERSION = "7.9.18";
    static final String FALLBACK_VENDOR = "Ronin-Lap";
    static final String FALLBACK_DESCRIPTION = "Gunny Java Online - Tac gia: Ronin-Lap";
    PhoneCanvas game;

    String appVersion(){
        String value = getAppProperty("MIDlet-Version");
        return value != null && value.length() > 0 ? value : FALLBACK_VERSION;
    }

    String appVendor(){
        String value = getAppProperty("MIDlet-Vendor");
        return value != null && value.length() > 0 ? value : FALLBACK_VENDOR;
    }

    String appDescription(){
        String value = getAppProperty("MIDlet-Description");
        return value != null && value.length() > 0 ? value : FALLBACK_DESCRIPTION;
    }
    protected void startApp(){
        try{
            if(game==null)game=new PhoneCanvas(this);
            Display.getDisplay(this).setCurrent(game);game.start();game.net.startupLogin();
        }catch(Throwable ex){
            Alert alert=new Alert("Gunny Java","Không mở được game: "+ex.toString()+". Hãy dùng đúng bản cho màn hình máy.",null,AlertType.ERROR);
            alert.setTimeout(Alert.FOREVER);Display.getDisplay(this).setCurrent(alert);
        }
    }
    protected void pauseApp(){if(game!=null)game.suspend();}
    protected void destroyApp(boolean unconditional){if(game!=null)game.stop();}
    void quit(){destroyApp(true);notifyDestroyed();}
}
