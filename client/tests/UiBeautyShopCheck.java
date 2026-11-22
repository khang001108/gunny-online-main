import java.lang.reflect.*;
import java.util.*;
import org.microemu.app.*;
import org.microemu.app.util.DeviceEntry;
import org.microemu.device.*;
import org.microemu.device.impl.*;
import org.microemu.device.j2se.*;
import org.microemu.MIDletBridge;

public class UiBeautyShopCheck {
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
        c.openLobby();c.experience=Progression.experienceForLevel(70);c.coins=c.shoppingMoney=999999;
        c.economy.offlineStones=100;c.economy.offlineMaterials[0]=100;c.economy.offlineMaterials[1]=100;c.economy.offlineMaterials[4]=9;c.economy.offlineMaterials[5]=9;

        var n=c.net;n.logged=true;n.vipCap=true;n.worker=Thread.currentThread();n.token="fixture";c.playerName="Ronin";
        c.vip.loaded=true;c.vip.experience=VipRules.TOTAL;c.vip.owned=31;c.vip.equipped=4;
        for(int i=0;i<13;i++)c.vip.cards[i]=999;


        c.mode=6;c.shop.open();c.shop.selectTab(1);c.shop.focus=8;
        c.shop.key(0,2,false);c.shop.key(0,2,false);c.shop.key(0,2,false);
        if(!c.shop.wingCategory()||c.shop.focus!=11)throw new AssertionError("Beauty fourth category is wings");
        c.shop.key(0,8,false);c.shop.key(0,2,false);c.shop.key(10,0,true);
        if(c.vip.trialWing!=1||!n.pending.equals(""))throw new AssertionError("Wing preview does not buy");
        shot(c,"shop-beauty-wings");
        c.vip.experience=0;c.vip.owned=0;c.shop.key('B',0,false);
        if(n.pending.length()!=0)throw new AssertionError("VIP locks still enforced");shot(c,"shop-beauty-locked");
        c.vip.experience=VipRules.TOTAL;c.shop.key('B',0,false);
        if(!n.pending.equals("vipbuy")||c.vip.item!=1)throw new AssertionError("Unlocked wing uses VIP purchase");n.pending="";
        c.shop.pointer(160,70);if(c.shop.kind!=4)throw new AssertionError("Hair pointer category");shot(c,"shop-beauty-hair");
        c.shop.pointer(280,70);if(!c.shop.wingCategory())throw new AssertionError("Wing pointer category");
        c.shop.pointer(240,100);if(c.vip.trialWing!=1)throw new AssertionError("Wing pointer preview");
        c.shop.focus=11;c.shop.key(0,4,false);if(c.shop.focus!=7)throw new AssertionError("Wing category to Beauty header");
        c.shop.key(0,2,false);if(c.shop.tab!=3)throw new AssertionError("Beauty to Props");c.shop.key(0,2,false);if(c.shop.tab!=2)throw new AssertionError("Props to Exchange");
        if(ShopScreen.TAB_VALUES.length!=4||c.shop.valid(43))throw new AssertionError("No VIP tab or focus");
        c.stop();System.out.println("PASS Beauty wings, keyboard/pointer navigation, previews, VIP locks and purchases at "+320*scale);System.exit(0);
    }
}
