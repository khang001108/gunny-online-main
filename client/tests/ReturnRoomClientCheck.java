import java.lang.reflect.*;import java.util.*;
import org.microemu.app.*;import org.microemu.app.util.DeviceEntry;import org.microemu.device.*;import org.microemu.device.impl.*;import org.microemu.device.j2se.*;import org.microemu.MIDletBridge;
public final class ReturnRoomClientCheck {
 static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);System.out.println("PASS: "+msg);}
 public static void main(String[] args)throws Exception{Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});System.setProperty("java.awt.headless","true");int scale=args.length==0?1:Integer.parseInt(args[0]);var h=new Headless();var f=Headless.class.getDeclaredField("emulator");f.setAccessible(true);var common=(Common)f.get(h);common.initParams(new ArrayList<String>(Arrays.asList("--rms","memory","--appclassloader","system","GunnyMIDlet")),new DeviceEntry("Nokia",null,DeviceImpl.RESIZABLE_LOCATION,true,false),J2SEDevice.class);var display=(J2SEDeviceDisplay)DeviceFactory.getDevice().getDeviceDisplay();display.setDisplayRectangle(new Rectangle(0,0,320*scale,240*scale));display.setDisplayPaintable(new Rectangle(0,0,320*scale,240*scale));common.initMIDlet(true);Thread.sleep(200);var app=(GunnyMIDlet)MIDletBridge.getCurrentMIDlet();if(app.game==null)app.game=new PhoneCanvas(app);var c=app.game;javax.microedition.lcdui.Display.getDisplay(app).setCurrent(c);Thread.sleep(150);c.running=false;Thread.sleep(100);c.presentation.boot=2;c.presentation.music=false;c.tutorialDone=true;c.playerName="Gunner Test";c.mode=0;


  var net=c.net;net.alive=false;net.inBattle=true;net.roomKind=0;net.results.leave();
  check(net.pending.equals("leave"),"1v1 PVP result leaves for the room list");
  net.pending="";net.roomKind=1;net.results.leave();check(net.pending.equals("leave"),"Solo mode result leaves for the room list");
  net.pending="";net.results.stage=1;net.results.claimed=true;net.results.activate();check(net.pending.equals("leave"),"Solo confirmation after claiming reward goes to room list");
  net.pending="";net.results.key(-7,0,false);check(net.pending.equals("leave"),"Solo back key after reward goes to room list");net.results.reset();
  net.pending="";net.roomKind=0;net.count=4;net.results.leave();check(net.pending.equals("returnroom"),"2v2 keeps its waiting room");
  net.pending="";net.roomKind=2;net.count=6;net.results.leave();check(net.pending.equals("returnroom"),"3v3 keeps its party room");
  net.pending="";net.roomKind=3;net.dungeonStage=2;net.results.leave();check(net.pending.equals("returnroom"),"Final dungeon result returns to existing room");
  net.returnToWaitingRoom();check(!net.inBattle&&c.mode==16&&net.queueCount==0,"Returning clears battle state and opens waiting room");
  net.inBattle=false;c.offlineDungeon.active=false;net.results.leave();check(c.mode==10,"Offline PVP returns to setup");
  c.offlineDungeon.active=true;c.offlineDungeon.stage=2;net.results.leave();check(c.mode==19&&!c.offlineDungeon.active&&c.offlineDungeon.stage==1,"Offline dungeon returns to setup");
  net.stop();c.stop();System.exit(0);
 }
}
