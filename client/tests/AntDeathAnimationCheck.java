import java.lang.reflect.*;import java.util.*;
import org.microemu.app.*;import org.microemu.app.util.DeviceEntry;import org.microemu.device.*;import org.microemu.device.impl.*;import org.microemu.device.j2se.*;import org.microemu.MIDletBridge;
public final class AntDeathAnimationCheck {
 static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);System.out.println("PASS: "+msg);}
 public static void main(String[] args)throws Exception{Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});System.setProperty("java.awt.headless","true");int scale=args.length==0?1:Integer.parseInt(args[0]);var h=new Headless();var f=Headless.class.getDeclaredField("emulator");f.setAccessible(true);var common=(Common)f.get(h);common.initParams(new ArrayList<String>(Arrays.asList("--rms","memory","--appclassloader","system","GunnyMIDlet")),new DeviceEntry("Nokia",null,DeviceImpl.RESIZABLE_LOCATION,true,false),J2SEDevice.class);var display=(J2SEDeviceDisplay)DeviceFactory.getDevice().getDeviceDisplay();display.setDisplayRectangle(new Rectangle(0,0,320*scale,240*scale));display.setDisplayPaintable(new Rectangle(0,0,320*scale,240*scale));common.initMIDlet(true);Thread.sleep(200);var app=(GunnyMIDlet)MIDletBridge.getCurrentMIDlet();if(app.game==null)app.game=new PhoneCanvas(app);var c=app.game;javax.microedition.lcdui.Display.getDisplay(app).setCurrent(c);Thread.sleep(150);c.running=false;Thread.sleep(100);c.presentation.boot=2;c.presentation.music=false;c.tutorialDone=true;c.playerName="Gunner Test";c.mode=0;



  c.net.alive=false;c.offlineDungeon.active=true;c.offlineDungeon.stage=1;c.game.unitCount=7;
  var ant=c.game.units[6];ant.x=160;ant.y=150;ant.maxHp=500;ant.hp=500;ant.face=-1;
  c.cameraX=c.cameraY=0;c.ox=c.oy=0;
  var image=javax.microedition.lcdui.Image.createImage(320*scale,240*scale);
  for(int type:new int[]{1,2}){
   ant.npc=type;ant.hp=500;ant.dead=false;c.frame+=40;render(c,image);
   ant.hp=0;int zeroHp=render(c,image);
   check(zeroHp>0,"Ant "+type+" remains visible immediately at zero HP before death flag");
   int started=c.antDeathFrame[6];
   c.frame+=2;ant.dead=true;ant.deathX=ant.x;ant.deathY=ant.y;
   check(render(c,image)>0&&c.antDeathFrame[6]==started,"Delayed death flag continues the same animation");
   c.frame=started+8;check(render(c,image)>0,"Lying pose follows the falling pose without a blank frame");
   c.frame=started+36;check(render(c,image)==0,"Corpse expires after existing display duration");
   ant.hp=500;ant.dead=false;c.frame++;render(c,image);ant.hp=0;c.frame++;
   check(render(c,image)>0&&c.antDeathFrame[6]==c.frame,"Respawned ant starts a fresh death animation");
   ant.maxHp=0;ant.dead=false;check(render(c,image)==0,"Unused reinforcement slot remains hidden");ant.maxHp=500;
  }
  c.net.stop();c.stop();System.exit(0);
 }
 static int render(PhoneCanvas c,javax.microedition.lcdui.Image image){
  c.g=image.getGraphics();c.g.setColor(0x13579b);c.g.fillRect(0,0,image.getWidth(),image.getHeight());c.drawUnitRaw(6);
  int[] pixels=new int[image.getWidth()*image.getHeight()];image.getRGB(pixels,0,image.getWidth(),0,0,image.getWidth(),image.getHeight());
  int changed=0;for(int pixel:pixels)if((pixel&0xffffff)!=0x13579b)changed++;return changed;
 }
}
