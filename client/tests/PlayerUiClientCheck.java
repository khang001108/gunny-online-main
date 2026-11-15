import java.lang.reflect.*;import java.util.*;
import org.microemu.app.*;import org.microemu.app.util.DeviceEntry;import org.microemu.device.*;import org.microemu.device.impl.*;import org.microemu.device.j2se.*;import org.microemu.MIDletBridge;
public final class PlayerUiClientCheck {
 static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);System.out.println("PASS: "+msg);}
 public static void main(String[] args)throws Exception{Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});System.setProperty("java.awt.headless","true");int scale=args.length==0?1:Integer.parseInt(args[0]);var h=new Headless();var f=Headless.class.getDeclaredField("emulator");f.setAccessible(true);var common=(Common)f.get(h);common.initParams(new ArrayList<String>(Arrays.asList("--rms","memory","--appclassloader","system","GunnyMIDlet")),new DeviceEntry("Nokia",null,DeviceImpl.RESIZABLE_LOCATION,true,false),J2SEDevice.class);var display=(J2SEDeviceDisplay)DeviceFactory.getDevice().getDeviceDisplay();display.setDisplayRectangle(new Rectangle(0,0,320*scale,240*scale));display.setDisplayPaintable(new Rectangle(0,0,320*scale,240*scale));common.initMIDlet(true);Thread.sleep(200);var app=(GunnyMIDlet)MIDletBridge.getCurrentMIDlet();if(app.game==null)app.game=new PhoneCanvas(app);var c=app.game;javax.microedition.lcdui.Display.getDisplay(app).setCurrent(c);Thread.sleep(150);c.running=false;Thread.sleep(100);c.presentation.boot=2;c.presentation.music=false;c.tutorialDone=true;c.playerName="Gunner Test";c.mode=0;



  var net=c.net;net.alive=false;net.logged=net.inBattle=net.playerUiCap=true;net.phase=3;net.count=6;net.hostActor=0;net.roomKind=2;c.game.unitCount=6;c.game.teams=true;c.mode=14;c.map=0;c.loadTicks=60;
  for(int i=0;i<6;i++){c.game.units[i].npc=0;c.game.units[i].hp=c.game.units[i].maxHp=1000;c.game.units[i].team=i%2;net.present[i]=true;net.controls[i]=i==0;net.wornMasks[i]=3;net.names[i]="Player "+i;}
  net.loadingMask=0;check(c.loadingReady(0)&&!c.loadingReady(2),"Local loaded player turns green before unfinished teammate");
  net.loadingMask=1<<2;check(c.loadingReady(2)&&!c.loadingReady(4),"Remote OK follows the server's per-player mask");
  c.tickLoading();check(c.mode==14,"Client waits in loading until server releases the barrier");
  c.keyPressed(32);check(c.mode==14,"OK key cannot bypass other players' loading");
  c.pointerPressed(100*scale,100*scale);check(c.mode==14,"Touch cannot bypass other players' loading");
  for(int i=0;i<6;i++){int[] slot=c.loadingSlot(i);check(i%2==0?slot[0]+slot[1]/2<155:slot[0]-slot[1]/2>165,"Each loading portrait stays in its team background");}
  check(c.loadingSlot(0)[1]>c.loadingSlot(2)[1]&&c.loadingSlot(0)[0]==75,"Party leader is large and centered between smaller allies");
  net.loadingLeaders=1<<3;check(c.loadingSlot(3)[0]==244&&c.loadingSlot(3)[1]>c.loadingSlot(1)[1],"Opposing party captain also stays in the center");net.loadingLeaders=0;
  save(c,"team");
  for(int i=1;i<6;i++)c.game.units[i].hp=0;net.roomKind=3;net.dungeonStage=1;c.map=6;
  int[] solo=c.loadingSlot(0);check(solo[0]==75&&solo[1]==76,"Solo portrait grows and centers in the left half");save(c,"solo");
  c.game.units[2].hp=c.game.units[4].hp=1000;save(c,"dungeon");
  net.loadingMask=63;net.phase=1;c.tickLoading();check(c.mode==1,"All players ready allows entering battle");
  c.mode=16;net.inBattle=false;net.roomId=0;c.owned=c.wornMask=1;c.equipped=0;
  var bytes=new java.io.ByteArrayOutputStream();var out=new java.io.DataOutputStream(bytes);
  out.writeInt(77);out.writeUTF("Inviter");out.writeBoolean(true);out.writeUTF("Gunner Friend");out.writeByte(0);out.writeByte(0);out.writeByte(3);for(int i=0;i<5;i++)out.writeByte(0);out.writeInt(Progression.experienceForLevel(25));
  net.social.pulse(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));
  check(c.popup.opened&&c.popup.action==NativePopup.INVITE_TEAM&&c.popup.playerPortrait!=null&&c.popup.playerName.equals("Gunner Friend")&&c.popup.playerLevel==25,"Invitation uses common popup with sender portrait, name and level");
  save(c,"invite");c.popup.selected=0;c.popup.choose();check(net.pending.equals("inviteanswer")&&net.social.answer&&!c.popup.opened,"Accept button answers invitation");
  net.pending="";net.operation="";c.popup.playerNotice("Mời vào đội","Mời vào phòng",null,"Friend",25,new String[]{"Vào đội","Từ chối"},NativePopup.INVITE_TEAM);c.popup.dismiss();
  check(net.pending.equals("inviteanswer")&&!net.social.answer,"Closing invitation declines it");
  for(int[] velocity:new int[][]{{100,850},{-100,850},{200,-700},{-300,-800},{0,1000}}){
   double actual=Math.atan2(velocity[1],velocity[0])*180/Math.PI;if(actual<0)actual+=360;
   double error=Math.abs(actual-c.queenArrowDirection(velocity[0],velocity[1])*5);error=Math.min(error,360-error);
   check(error<=3.1,"Queen arrow follows projectile velocity on ascent and descent");
   check(c.queenArrowImage(velocity[0],velocity[1])!=null,"Queen arrow rotation renders");
  }
  net.pending="";net.operation="";net.busy=false;net.choosingCharacter=true;
  c.mode=7;c.createFocus=0;c.gender=0;c.playerName="Registered";c.loadCreation();
  c.keyPressed('N');check(!c.typing.active(),"Character choice ignores the rename shortcut for registered accounts");
  c.editName();check(!c.typing.active(),"Character choice blocks direct rename entry");
  c.pointerPressed(150*scale,160*scale);check(!c.typing.active(),"Registered name has no editable touch field");
  c.keyPressed('8');check(c.createFocus==2,"Down skips the name field and selects Enter Game");
  c.keyPressed('2');check(c.createFocus==0,"Up returns directly to gender choice");
  c.keyPressed('6');check(c.gender==1,"Gender can still be selected by keyboard");
  c.keyPressed(32);check(c.createFocus==2&&net.pending.length()==0,"Gender confirmation selects Enter Game");
  c.keyPressed(32);check(net.pending.equals("appearance"),"Enter Game submits character choice");
  var appearance=new java.io.DataInputStream(new java.io.ByteArrayInputStream(net.body("appearance")));
  appearance.readInt();appearance.readUTF();
  check(appearance.readUTF().equals("Registered")&&appearance.readByte()==1,"Character submission keeps registered name with chosen gender");
  net.pending="";c.pointerPressed(120*scale,110*scale);
  check(c.gender==0&&c.createFocus==2,"Touch gender choice proceeds directly to Enter Game");
  c.pointerPressed(150*scale,210*scale);check(net.pending.equals("appearance"),"Touch Enter Game submits character choice");
  save(c,"creation");
  net.logged=net.choosingCharacter=false;net.pending="";c.createFocus=0;
  c.keyPressed('8');check(c.createFocus==1,"Offline creation retains its name step");
  c.keyPressed('N');check(c.typing.active(),"Offline name remains editable");c.typing.cancel();
  net.stop();c.stop();System.exit(0);
 }
 static void save(PhoneCanvas c,String name)throws Exception{
  var image=javax.microedition.lcdui.Image.createImage(320*c.s,240*c.s);c.g=image.getGraphics();c.ox=c.oy=0;
  if(name.equals("invite"))c.popup.draw();else if(name.equals("creation"))c.drawCreation();else c.drawVersus();
  int[] pixels=new int[image.getWidth()*image.getHeight()];image.getRGB(pixels,0,image.getWidth(),0,0,image.getWidth(),image.getHeight());
  var output=new java.awt.image.BufferedImage(image.getWidth(),image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);output.setRGB(0,0,image.getWidth(),image.getHeight(),pixels,0,image.getWidth());
  javax.imageio.ImageIO.write(output,"png",new java.io.File("client/build/player-ui-"+name+"-"+c.s+".png"));
 }
}
