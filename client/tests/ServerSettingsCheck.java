import java.lang.reflect.*;import java.util.*;
import org.microemu.app.*;import org.microemu.app.util.DeviceEntry;import org.microemu.device.*;import org.microemu.device.impl.*;import org.microemu.device.j2se.*;import org.microemu.MIDletBridge;
public final class ServerSettingsCheck {
 static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);System.out.println("PASS: "+msg);}
 public static void main(String[] args)throws Exception{Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});System.setProperty("java.awt.headless","true");int scale=args.length==0?1:Integer.parseInt(args[0]);var h=new Headless();var f=Headless.class.getDeclaredField("emulator");f.setAccessible(true);var common=(Common)f.get(h);common.initParams(new ArrayList<String>(Arrays.asList("--rms","memory","--appclassloader","system","GunnyMIDlet")),new DeviceEntry("Nokia",null,DeviceImpl.RESIZABLE_LOCATION,true,false),J2SEDevice.class);var display=(J2SEDeviceDisplay)DeviceFactory.getDevice().getDeviceDisplay();display.setDisplayRectangle(new Rectangle(0,0,320*scale,240*scale));display.setDisplayPaintable(new Rectangle(0,0,320*scale,240*scale));common.initMIDlet(true);Thread.sleep(200);var app=(GunnyMIDlet)MIDletBridge.getCurrentMIDlet();if(app.game==null)app.game=new PhoneCanvas(app);var c=app.game;javax.microedition.lcdui.Display.getDisplay(app).setCurrent(c);Thread.sleep(150);c.running=false;Thread.sleep(100);c.presentation.boot=2;c.presentation.music=false;c.tutorialDone=true;c.playerName="Gunner Test";c.mode=0;

  var net=c.net;net.loginForm(false);
  check(net.endpoint.equals("socket://gunnyjava.xubi.org:22123"),"Default server uses public port 22123");
  net.auth.selected=net.auth.fields()+2;net.auth.activate();Thread.sleep(150);
  check(javax.microedition.lcdui.Display.getDisplay(app).getCurrent() instanceof javax.microedition.lcdui.Form,"Server link opens settings instead of browser");
  var settings=new ServerSettings(net);settings.open();
  check(settings.internet.getString().equals("socket://gunnyjava.xubi.org:22123")&&settings.wifi.getString().equals("socket://192.168.1.28:22123"),"Both Internet and Wi-Fi defaults are shown");
  settings.choice.setSelectedIndex(1,true);settings.commandAction(settings.save,settings.form);
  check(net.endpoint.equals("socket://192.168.1.28:22123")&&net.addressField.getString().equals(net.endpoint),"Wi-Fi selection updates login address");
  var restored=new OnlineClient(c);check(restored.endpoint.equals(net.endpoint),"Saved choice survives client restart and overrides manifest");
  settings=new ServerSettings(net);settings.open();settings.internet.setString("socket://new.example:23456");settings.wifi.setString("socket://192.168.1.50:23456");settings.choice.setSelectedIndex(0,true);settings.commandAction(settings.save,settings.form);
  restored=new OnlineClient(c);check(restored.endpoint.equals("socket://new.example:23456"),"Custom endpoint persists after restart");
  settings=new ServerSettings(restored);settings.open();check(settings.wifi.getString().equals("socket://192.168.1.50:23456"),"Edited Wi-Fi preset is retained");
  settings.internet.setString("http://invalid");settings.commandAction(settings.save,settings.form);check(restored.endpoint.equals("socket://new.example:23456"),"Invalid endpoint does not replace saved choice");
  check(OnlineClient.migrateEndpoint("socket://gunnyjava.xubi.org:22122").endsWith(":22123"),"Old default migrates to new public port");
  net.stop();c.stop();System.out.println("Server settings checks passed at scale "+scale);System.exit(0);
 }
}
