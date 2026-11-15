import java.lang.reflect.*;import java.util.*;
import org.microemu.app.*;import org.microemu.app.util.DeviceEntry;import org.microemu.device.*;import org.microemu.device.impl.*;import org.microemu.device.j2se.*;import org.microemu.MIDletBridge;
public final class HudSpeechCheck {
 static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);System.out.println("PASS: "+msg);}
 public static void main(String[] args)throws Exception{Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});System.setProperty("java.awt.headless","true");int scale=args.length==0?1:Integer.parseInt(args[0]);var h=new Headless();var f=Headless.class.getDeclaredField("emulator");f.setAccessible(true);var common=(Common)f.get(h);common.initParams(new ArrayList<String>(Arrays.asList("--rms","memory","--appclassloader","system","GunnyMIDlet")),new DeviceEntry("Nokia",null,DeviceImpl.RESIZABLE_LOCATION,true,false),J2SEDevice.class);var display=(J2SEDeviceDisplay)DeviceFactory.getDevice().getDeviceDisplay();display.setDisplayRectangle(new Rectangle(0,0,320*scale,240*scale));display.setDisplayPaintable(new Rectangle(0,0,320*scale,240*scale));common.initMIDlet(true);Thread.sleep(200);var app=(GunnyMIDlet)MIDletBridge.getCurrentMIDlet();if(app.game==null)app.game=new PhoneCanvas(app);var c=app.game;javax.microedition.lcdui.Display.getDisplay(app).setCurrent(c);Thread.sleep(150);c.running=false;Thread.sleep(100);c.presentation.boot=2;c.presentation.music=false;c.tutorialDone=true;c.playerName="Gunner Test";c.mode=0;



  var net=c.net;net.alive=false;net.logged=net.inBattle=true;net.roomId=42;net.phase=1;net.count=2;c.mode=1;c.ox=c.oy=0;
  c.game.unitCount=2;c.game.teams=false;
  for(int i=0;i<6;i++){c.game.units[i].npc=0;c.game.units[i].hp=c.game.units[i].maxHp=1000;}
  c.game.units[0].x=20;c.game.units[1].x=450;
  var speech=net.chat.roomSpeech;speech.supported=true;
  message(speech,42,0,"Xin chào P2!",-1);
  var first=draw(c,0);var distant=draw(c,400);
  check(Arrays.equals(pixels(first),pixels(distant)),"Chat remains fixed to P1 HUD avatar when camera moves to far-right P2");
  check((distantPixel(distant,124,30,scale)&0xffffff)!=0x13579b,"Bubble tail points upward to sender portrait");
  javax.imageio.ImageIO.write(toImage(distant),"png",new java.io.File("client/build/hud-speech-"+scale+".png"));
  speech.reset();
  var bytes=new java.io.ByteArrayOutputStream();var out=new java.io.DataOutputStream(bytes);
  for(int i=0;i<6;i++){out.writeByte(i==0?3:255);out.writeShort(i==0?3000:0);}
  c.emotions.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));
  first=draw(c,0);distant=draw(c,400);
  check(Arrays.equals(pixels(first),pixels(distant)),"Legacy battle emoji is visible beside HUD portrait for an offscreen sender");
  check((distantPixel(distant,124,30,scale)&0xffffff)!=0x13579b,"Emoji balloon shares the upward sender pointer");
  c.emotions.reset();c.game.teams=true;c.game.unitCount=6;net.count=6;
  message(speech,42,4,"",64);first=draw(c,0);distant=draw(c,400);
  check(Arrays.equals(pixels(first),pixels(distant)),"3v3 expanded emoji follows its own HUD portrait");
  c.mode=16;net.phase=0;message(speech,42,0,"Chat phòng chờ",-1);
  var waiting=javax.microedition.lcdui.Image.createImage(320*scale,240*scale);c.g=waiting.getGraphics();c.g.setColor(0x13579b);c.g.fillRect(0,0,waiting.getWidth(),waiting.getHeight());
  speech.beginFrame();speech.head(0,100,110);speech.draw();
  check((distantPixel(waiting,100,107,scale)&0xffffff)!=0x13579b,"Waiting-room bubble keeps its existing downward pointer");
  net.stop();c.stop();System.exit(0);
 }
 static void message(RoomSpeech speech,int room,int actor,String text,int emoji)throws Exception{
  var bytes=new java.io.ByteArrayOutputStream();var out=new java.io.DataOutputStream(bytes);
  out.writeInt(room);out.writeByte(speech.context()-1);out.writeByte(1);out.writeByte(actor);out.writeShort(4000);out.writeUTF(text);out.writeByte(emoji);
  speech.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));
 }
 static javax.microedition.lcdui.Image draw(PhoneCanvas c,int camera){
  var image=javax.microedition.lcdui.Image.createImage(320*c.s,240*c.s);c.g=image.getGraphics();c.g.setColor(0x13579b);c.g.fillRect(0,0,image.getWidth(),image.getHeight());
  c.cameraX=camera;c.cameraY=0;c.net.chat.roomSpeech.beginFrame();
  if(c.game.teams)for(int i=0;i<6;i++)c.drawTurnPortrait(i,i%2==0?70+i/2*24:190+i/2*24,1);
  else{c.drawTurnPortrait(0,112,1);c.drawTurnPortrait(1,184,1);}
  c.emotions.draw();c.net.chat.roomSpeech.draw();return image;
 }
 static int[] pixels(javax.microedition.lcdui.Image image){int[] data=new int[image.getWidth()*image.getHeight()];image.getRGB(data,0,image.getWidth(),0,0,image.getWidth(),image.getHeight());return data;}
 static int distantPixel(javax.microedition.lcdui.Image image,int x,int y,int scale){return pixels(image)[y*scale*image.getWidth()+x*scale];}
 static java.awt.image.BufferedImage toImage(javax.microedition.lcdui.Image image){var result=new java.awt.image.BufferedImage(image.getWidth(),image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);result.setRGB(0,0,image.getWidth(),image.getHeight(),pixels(image),0,image.getWidth());return result;}
}
