import java.lang.reflect.*;import java.util.*;
import org.microemu.app.*;import org.microemu.app.util.DeviceEntry;import org.microemu.device.*;import org.microemu.device.impl.*;import org.microemu.device.j2se.*;import org.microemu.MIDletBridge;
public final class WeaponQueueClientCheck {
 static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);System.out.println("PASS: "+msg);}
 public static void main(String[] args)throws Exception{Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});System.setProperty("java.awt.headless","true");int scale=args.length==0?1:Integer.parseInt(args[0]);var h=new Headless();var f=Headless.class.getDeclaredField("emulator");f.setAccessible(true);var common=(Common)f.get(h);common.initParams(new ArrayList<String>(Arrays.asList("--rms","memory","--appclassloader","system","GunnyMIDlet")),new DeviceEntry("Nokia",null,DeviceImpl.RESIZABLE_LOCATION,true,false),J2SEDevice.class);var display=(J2SEDeviceDisplay)DeviceFactory.getDevice().getDeviceDisplay();display.setDisplayRectangle(new Rectangle(0,0,320*scale,240*scale));display.setDisplayPaintable(new Rectangle(0,0,320*scale,240*scale));common.initMIDlet(true);Thread.sleep(200);var app=(GunnyMIDlet)MIDletBridge.getCurrentMIDlet();if(app.game==null)app.game=new PhoneCanvas(app);var c=app.game;javax.microedition.lcdui.Display.getDisplay(app).setCurrent(c);Thread.sleep(150);c.running=false;Thread.sleep(100);c.presentation.boot=2;c.presentation.music=false;c.tutorialDone=true;c.playerName="Gunner Test";c.mode=0;



  var net=c.net;net.alive=false;net.logged=true;net.roomId=0;c.mode=16;c.owned=c.wornMask=0;
  net.roomCount=1;net.roomIds[0]=42;net.roomPhases[0]=0;net.roomPlayers[0]=1;net.roomSeats[0]=net.roomCapacities[0]=2;net.selection=2;
  net.rooms.activate();check(net.pending.length()==0&&c.popup.opened,"Room card blocks an unarmed player with popup");
  c.popup.dismiss();net.joinForm();check(net.pending.length()==0&&c.popup.opened,"Join-by-code form also blocks unarmed entry");
  c.popup.dismiss();check(!net.social.respond(true)&&net.pending.length()==0&&c.popup.opened,"Accepting invite is blocked until weapon is equipped");
  c.owned=c.wornMask=1;c.equipped=0;check(!c.requireWeapon(),"Owned and equipped weapon passes client check");
  net.roomId=42;net.phase=0;net.roomKind=1;net.matchQueued=true;
  net.updateQueueTime(60,false);long deadline=net.queueDeadline;
  check(net.queueSeconds(deadline-60000)==60&&net.queueSeconds(deadline-55000)==55,"Queue countdown decreases from 60 to 55");
  check(net.queueSeconds(deadline-1000)==1&&net.queueSeconds(deadline)==0&&net.queueSeconds(deadline+5000)==0,"Countdown reaches zero and never becomes negative");
  int[] sixty=queueImage(c);net.queueDeadline-=5000;int[] fiftyFive=queueImage(c);check(!Arrays.equals(sixty,fiftyFive),"Rendered matchmaking digits change from 60 to 55");
  net.queueDeadline+=5000;
  net.queueDeadline-=5000;long running=net.queueDeadline;net.updateQueueTime(60,false);
  check(net.queueDeadline==running,"Repeated server snapshot does not restart the countdown");
  net.updateQueueTime(0,false);check(net.queueSeconds()==0,"Cancel clears the queue clock");
  net.updateQueueTime(60,false);check(net.queueSeconds()==60,"A new search starts a fresh 60-second countdown");
  net.updateQueueTime(0,true);check(net.queueSeconds()==0,"Expiry clears remaining queue time");
  net.stop();c.stop();System.exit(0);
 }
 static int[] queueImage(PhoneCanvas c){
  var image=javax.microedition.lcdui.Image.createImage(320*c.s,240*c.s);c.g=image.getGraphics();c.ox=c.oy=0;c.presentation.queue();
  int[] pixels=new int[image.getWidth()*image.getHeight()];image.getRGB(pixels,0,image.getWidth(),0,0,image.getWidth(),image.getHeight());return pixels;
 }
}
