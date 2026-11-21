import java.lang.reflect.*;
import java.util.*;
import org.microemu.app.*;
import org.microemu.app.util.DeviceEntry;
import org.microemu.device.*;
import org.microemu.device.impl.*;
import org.microemu.device.j2se.*;
import org.microemu.MIDletBridge;

public class ForgeMaterialUiCheck {
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
        c.openLobby();c.net.logged=false;c.economy.offlineStones=100;c.economy.offlineMaterials[4]=3;c.economy.offlineMaterials[5]=2;c.shoppingMoney=20000;c.forge.open();
        c.forge.focus=1;c.forge.key(0,4,false);if(c.forge.focus!=2)throw new AssertionError("Continuous row");c.forge.key(0,4,false);if(c.forge.focus!=8)throw new AssertionError("Protection focus");c.forge.key(0,4,false);if(c.forge.focus!=7)throw new AssertionError("Luck focus");c.forge.key(10,0,true);if(!c.forge.lucky)throw new AssertionError("Luck activation");shot(c,"forge-luck-focus");c.forge.key(0,8,false);c.forge.key(10,0,true);if(!c.forge.protect)throw new AssertionError("Protection activation");shot(c,"forge-protection-focus");
        c.forge.key(-6,0,false);if(!c.popup.opened||!c.popup.title.equals(StoneShop.NAMES[6]))throw new AssertionError("Forge material information");c.popup.dismiss();c.forge.key(0,4,false);c.forge.key(0,4,false);if(c.forge.focus!=9)throw new AssertionError("Stone row navigation");c.forge.grade=1;c.forge.key(0,1,false);if(c.forge.grade!=5)throw new AssertionError("Stone grade wraps");
        c.mode=6;c.shop.open();c.shop.tab=3;
        for(int type=0;type<7;type++){c.stoneShop.focus=type;c.shop.focus=type%6;int count=c.economy.material(type),money=c.shoppingMoney;c.shop.key(-6,0,false);if(!c.popup.opened||!c.popup.title.equals(StoneShop.NAMES[type])||c.popup.action==StoneShop.CONFIRM_PURCHASE||c.economy.material(type)!=count||c.shoppingMoney!=money)throw new AssertionError("Soft-left information, not purchase: "+type);shot(c,"material-info-"+type);c.popup.dismiss();}
        c.stoneShop.focus=5;c.shop.focus=5;shot(c,"shop-material-footer");c.shop.key(10,0,true);if(!c.popup.opened||c.popup.action!=StoneShop.CONFIRM_PURCHASE)throw new AssertionError("OK still purchases");
        c.stop();System.out.println("PASS: Keyboard focus, charm activation, 7 soft-left descriptions and OK purchase at "+(320*scale)+"x"+(240*scale));System.exit(0);
    }
}
