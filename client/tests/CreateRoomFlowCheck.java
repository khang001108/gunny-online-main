import java.lang.reflect.*;import java.util.*;
import org.microemu.app.*;import org.microemu.app.util.DeviceEntry;import org.microemu.device.*;import org.microemu.device.impl.*;import org.microemu.device.j2se.*;import org.microemu.MIDletBridge;
public final class CreateRoomFlowCheck {
 static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);System.out.println("PASS: "+msg);}
 public static void main(String[] args)throws Exception{Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});System.setProperty("java.awt.headless","true");int scale=args.length==0?1:Integer.parseInt(args[0]);var h=new Headless();var f=Headless.class.getDeclaredField("emulator");f.setAccessible(true);var common=(Common)f.get(h);common.initParams(new ArrayList<String>(Arrays.asList("--rms","memory","--appclassloader","system","GunnyMIDlet")),new DeviceEntry("Nokia",null,DeviceImpl.RESIZABLE_LOCATION,true,false),J2SEDevice.class);var display=(J2SEDeviceDisplay)DeviceFactory.getDevice().getDeviceDisplay();display.setDisplayRectangle(new Rectangle(0,0,320*scale,240*scale));display.setDisplayPaintable(new Rectangle(0,0,320*scale,240*scale));common.initMIDlet(true);Thread.sleep(200);var app=(GunnyMIDlet)MIDletBridge.getCurrentMIDlet();if(app.game==null)app.game=new PhoneCanvas(app);var c=app.game;javax.microedition.lcdui.Display.getDisplay(app).setCurrent(c);Thread.sleep(150);c.running=false;Thread.sleep(100);c.presentation.boot=2;c.presentation.music=false;c.tutorialDone=true;c.playerName="Gunner Test";c.mode=0;



  var net=c.net;net.alive=false;net.logged=true;net.modernRooms=net.asyncTurns=net.matchmakingCap=true;
  net.dungeonBrowser=true;net.dungeonListLoading=false;c.mode=16;c.dungeon.focus=1;
  c.dungeon.activateList();
  check(net.create.opened&&net.create.dungeon&&net.create.name.equals("Phó bản vui vẻ"),"Dungeon Create opens shared room settings with correct suggestion");
  var frame=javax.microedition.lcdui.Image.createImage(320*scale,240*scale);c.paint(frame.getGraphics());
  int[] pixels=new int[320*scale*240*scale];frame.getRGB(pixels,0,320*scale,0,0,320*scale,240*scale);
  var picture=new java.awt.image.BufferedImage(320*scale,240*scale,java.awt.image.BufferedImage.TYPE_INT_RGB);picture.setRGB(0,0,320*scale,240*scale,pixels,0,320*scale);
  javax.imageio.ImageIO.write(picture,"png",new java.io.File("client/build/create-room-"+scale+".png"));
  net.create.name="Phòng của tôi";net.create.secret="1234";net.create.locked=true;
  net.pointer(150,198);
  check(!net.create.opened&&net.pending.equals("dungeoncreate"),"Confirm requests dungeon room immediately through pointer handler");
  var body=new java.io.DataInputStream(new java.io.ByteArrayInputStream(net.body(net.pending)));
  body.readInt();check(body.readUTF().equals("dungeoncreate")&&body.readByte()==0&&body.readUTF().equals("Phòng của tôi")&&body.readUTF().equals("1234"),"Dungeon request carries chosen name and password with initial Easy difficulty");
  net.pending=net.operation="";net.create.openDungeon();net.create.key(-7,0,false);
  check(!net.create.opened&&net.pending.length()==0,"Cancel returns to dungeon browser without creating a room");
  net.dungeonBrowser=false;net.selection=0;net.rooms.activate();
  check(net.create.name.equals("Thi đấu vui vẻ")&&!net.create.dungeon&&!net.create.team,"PVP gets its own name after opening dungeon settings");
  net.create.team=true;net.create.open();check(net.create.name.equals("Tổ đội vui vẻ")&&!net.create.dungeon,"3v3 gets the team suggestion");
  net.create.focus=2;net.create.activate();check(net.pending.equals("createteam"),"3v3 confirmation still creates a team room");
  net.stop();c.stop();System.exit(0);
 }
}
