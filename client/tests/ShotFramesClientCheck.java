import java.lang.reflect.*;import java.util.*;
import org.microemu.app.*;import org.microemu.app.util.DeviceEntry;import org.microemu.device.*;import org.microemu.device.impl.*;import org.microemu.device.j2se.*;import org.microemu.MIDletBridge;
public final class ShotFramesClientCheck {
 static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);System.out.println("PASS: "+msg);}
 public static void main(String[] args)throws Exception{Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});System.setProperty("java.awt.headless","true");int scale=args.length==0?1:Integer.parseInt(args[0]);var h=new Headless();var f=Headless.class.getDeclaredField("emulator");f.setAccessible(true);var common=(Common)f.get(h);common.initParams(new ArrayList<String>(Arrays.asList("--rms","memory","--appclassloader","system","GunnyMIDlet")),new DeviceEntry("Nokia",null,DeviceImpl.RESIZABLE_LOCATION,true,false),J2SEDevice.class);var display=(J2SEDeviceDisplay)DeviceFactory.getDevice().getDeviceDisplay();display.setDisplayRectangle(new Rectangle(0,0,320*scale,240*scale));display.setDisplayPaintable(new Rectangle(0,0,320*scale,240*scale));common.initMIDlet(true);Thread.sleep(200);var app=(GunnyMIDlet)MIDletBridge.getCurrentMIDlet();if(app.game==null)app.game=new PhoneCanvas(app);var c=app.game;javax.microedition.lcdui.Display.getDisplay(app).setCurrent(c);Thread.sleep(150);c.running=false;Thread.sleep(100);c.presentation.boot=2;c.presentation.music=false;c.tutorialDone=true;c.playerName="Gunner Test";c.mode=0;



  var net=c.net;net.alive=false;net.logged=true;
  try(var input=new java.io.DataInputStream(new java.io.FileInputStream("client/build/shot-frames.bin"))){
   net.capabilities(input.readUTF());check(net.shotFrames&&!net.liveCap&&(net.tcpFlags()&4194304)!=0,"Client negotiates ordered authoritative shot frames over TCP");
   byte[] lobby=new byte[input.readInt()];input.readFully(lobby);net.readLobby(new java.io.DataInputStream(new java.io.ByteArrayInputStream(lobby)));
   int turn=input.readInt(),count=input.readInt(),visible=0,explosions=0;
   byte[][] packets=new byte[count][];
   for(int chunk=0;chunk<count;chunk++){packets[chunk]=new byte[input.readInt()];input.readFully(packets[chunk]);}
   int next=0;int[] stablePixels=null;
   while(next<count||net.chunks.frames!=null){
    if(next<count&&net.chunks.cached()>=-1){
     net.chunks.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(packets[next++])),true,turn);
    }
    if(net.chunks.frames!=null){net.animate();if(stablePixels==null)stablePixels=c.pixels;else check(stablePixels==c.pixels,"Remote frame preserves terrain allocation");if(c.game.ballCount>0)visible++;if(c.game.blastCount>0)explosions++;
     for(int i=0;i<c.game.ballCount;i++){
      Battle.Ball ball=c.game.balls[i];if(c.game.ground.solid(ball.x/256,ball.y/256))throw new AssertionError("Projectile crossed solid terrain during playback");
     }}
   }
   check(visible>0,"Thrown projectile is visible during remote playback");
   check(explosions>0,"Remote playback includes impact explosion");
   check(net.revision==input.readInt(),"Playback applies exactly the server terrain impacts");
   check(net.resolutionSeen,"Next turn waits until authoritative playback finishes");
  }
  net.stop();c.stop();System.exit(0);
 }
}
