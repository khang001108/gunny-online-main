import java.lang.reflect.*;
import java.util.*;
import org.microemu.app.*;
import org.microemu.app.util.DeviceEntry;
import org.microemu.device.*;
import org.microemu.device.impl.*;
import org.microemu.device.j2se.*;
import org.microemu.MIDletBridge;

public class Ui787Check {
    static void shot(PhoneCanvas c,String name)throws Exception{
        var image=javax.microedition.lcdui.Image.createImage(320*c.s,240*c.s);
        c.paint(image.getGraphics());
        int w=image.getWidth(),h=image.getHeight();
        int[] pixels=new int[w*h];
        image.getRGB(pixels,0,w,0,0,w,h);
        var png=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_RGB);
        png.setRGB(0,0,w,h,pixels,0,w);
        javax.imageio.ImageIO.write(png,"png",java.nio.file.Path.of("client/build/"+name+"-"+w+".png").toFile());
    }
    public static void main(String[] args)throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});
        System.setProperty("java.awt.headless","true");
        int scale=Integer.parseInt(args[0]);
        var headless=new Headless();
        var field=Headless.class.getDeclaredField("emulator");
        field.setAccessible(true);
        var common=(Common)field.get(headless);
        common.initParams(new ArrayList<>(List.of("--rms","memory","--appclassloader","system","GunnyMIDlet")),
            new DeviceEntry("Nokia",null,DeviceImpl.RESIZABLE_LOCATION,true,false),J2SEDevice.class);
        var display=(J2SEDeviceDisplay)DeviceFactory.getDevice().getDeviceDisplay();
        display.setDisplayRectangle(new Rectangle(0,0,320*scale,240*scale));
        display.setDisplayPaintable(new Rectangle(0,0,320*scale,240*scale));
        common.initMIDlet(true);
        Thread.sleep(250);
        var app=(GunnyMIDlet)MIDletBridge.getCurrentMIDlet();
        if(app.game==null)app.game=new PhoneCanvas(app);
        var c=app.game;
        c.running=false;
        Thread.sleep(100);
        c.presentation.boot=2;c.presentation.music=false;c.tutorialDone=true;
        c.openLobby();c.beginMatch(0);c.mode=14;c.loadTicks=60;c.versusBg=c.combatRoomArt("room-loading-bg");c.versusOK=null;shot(c,"loading787");if(c.versusOK==null)throw new AssertionError("Partial loading atlas not restored");
        c.openLobby();c.economy.offlineStones=0;c.economy.offlineMaterials[0]=22;c.economy.offlineMaterials[3]=44;c.forge.open();if(c.forge.step!=0)throw new AssertionError("Equipment first");c.forge.key(10,0,true);if(c.forge.step!=1||c.forge.gradeCount!=2)throw new AssertionError("Owned stones only");c.forge.key(-6,0,false);shot(c,"forge787-stones");c.forge.key(10,0,true);if(c.forge.step!=2||c.forge.quantity!=1||c.forge.grade!=2)throw new AssertionError("Stone confirms to charms");c.forge.focus=2;shot(c,"forge787-continuous");
        c.mode=8;c.bag.open();for(int i=0;i<c.bag.count;i++)if(c.bag.kinds[i]==BagScreen.STONE_KIND){c.bag.page=i/25;c.bag.choose(i%25);c.bag.key(-6,0,false);if(!c.bag.info)throw new AssertionError("Material tooltip");break;}shot(c,"bag787-tip");c.bag.info=false;
        c.mode=6;c.shop.open();c.shop.tab=3;c.shop.focus=5;c.stoneShop.focus=5;c.shop.key(-6,0,false);if(c.popup.opened||c.stoneShop.infoType!=5)throw new AssertionError("Shop uses item panel");shot(c,"shop787-tip");
        c.mode=19;c.offlineDungeon.open();shot(c,"dungeon787");c.dungeon.openPicker(false);c.dungeon.keyRoom(false,10,0,true);if(c.dungeon.pickerStep!=1)throw new AssertionError("Picker moves to difficulty");shot(c,"picker787");
        c.mode=0;var social=c.net.social;social.opened=true;social.viewing=true;social.profileLoading=false;social.infoName="Friend";social.infoExp=Progression.experienceForLevel(70);social.infoRank=5;social.infoWins=5;shot(c,"friend787");
        c.stop();System.out.println("PASS partial loading crash regression, staged forge, tooltips, friend and dungeon at "+(320*scale)+"x"+(240*scale));System.exit(0);
    }
}
