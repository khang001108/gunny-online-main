import java.lang.reflect.*;
import java.util.*;
import org.microemu.app.*;
import org.microemu.app.util.DeviceEntry;
import org.microemu.device.*;
import org.microemu.device.impl.*;
import org.microemu.device.j2se.*;
import org.microemu.MIDletBridge;

public class Gift786ClientCheck {
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
        c.openLobby();var store=new CloudStore.Memory();store.register("gift@local.test","test-password-123","Gunner");var profile=store.profile(store.login("gift@local.test","test-password-123").userId);profile.characterChosen=true;profile.giftBags=2;profile.vipTickets=1;profile.stones=44;profile.craftStones[2]=66;profile.luckyCharms=3;
        try(var server=new GunnyServer(new java.net.InetSocketAddress("127.0.0.1",0),java.nio.file.Path.of("backend/assets"),store)){
         server.clock.shutdownNow();server.startTcp("127.0.0.1",0,4);var n=c.net;n.alive=false;n.worker=Thread.currentThread();n.endpoint="socket://127.0.0.1:"+server.tcpPort();n.email="gift@local.test";n.password="test-password-123";n.probeHealth(3000);var login=n.postOnce("login");n.token=login.readUTF();n.readProfile(login);n.logged=true;c.inventory.read(n.postOnce("shop"));if(!n.giftCap||!n.giftExpCap)throw new AssertionError("Gift capability");
         c.mode=8;c.bag.open();int materials=0,gifts=0;for(int i=0;i<c.bag.count;i++){if(c.bag.kinds[i]==BagScreen.STONE_KIND)materials++;if(c.bag.kinds[i]==BagScreen.GIFT_KIND)gifts++;}if(materials!=3||gifts!=3)throw new AssertionError("Bag materials/gifts");shot(c,"bag786");
         for(int type=0;type<3;type++){c.popup.opened=false;c.bag.list();int slot=-1;for(int i=0;i<c.bag.count;i++)if(c.bag.kinds[i]==BagScreen.GIFT_KIND&&c.bag.items[i]==type)slot=i;if(slot<0)throw new AssertionError("Gift missing from bag");c.bag.page=slot/25;c.bag.choose(slot%25);c.bag.key(10,0,true);if(!n.pending.equals("giftopenexp"))throw new AssertionError("Use queues TCP");n.pending="";var reply=n.postOnce("giftopenexp");n.readProfile(reply);c.shoppingMoney=reply.readInt();c.experience=reply.readInt();c.economy.read(reply);c.gifts.read(reply,true);if(reply.available()!=0)throw new AssertionError("Trailing gift bytes");shot(c,"gift786-"+type);c.popup.key(10,0,true);if(c.popup.opened)throw new AssertionError("OK closes gift popup");}
         if(c.economy.giftLevel!=2||c.economy.giftBags!=1||c.economy.vipTickets<0||c.economy.vipUntil!=0||c.vip.cards[0]<1)throw new AssertionError("Persistent gifts");
         profile.forgeLevels[ForgeRules.index(0,2,0)]=15;c.economy.read(n.postOnce("economy"));c.bag.info=true;c.bag.infoKind=2;c.bag.infoItem=0;c.bag.focus=33;shot(c,"defense786");c.bag.info=false;
         c.forge.open();n.pending="";c.forge.grade=4;shot(c,"forge786");
         // Warm the large menus before each load, then verify their images are released.
         for(int round=0;round<8;round++){c.mode=0;shot(c,"memory786-home");c.mode=8;c.bag.open();shot(c,"memory786-bag");n.roomMap=round%2;n.roomKind=0;n.count=2;n.present[0]=n.present[1]=true;n.controls[0]=true;n.names[0]="Gunner";n.names[1]="Peer";n.sex[0]=n.sex[1]=0;n.weapon[0]=n.weapon[1]=0;n.custom[0]=n.custom[1]=false;n.wornMasks[0]=n.wornMasks[1]=3;n.inBattle=false;n.prepareBattle();if(c.bag.frame!=null||c.home.background!=null||c.shop.frame!=null||c.forge.frame!=null||c.loadout.background!=null||n.rooms.background!=null)throw new AssertionError("Menu image retained during load");if(scale==1&&c.actorStand[0]!=c.versusPortrait[0])throw new AssertionError("Duplicate E72 portrait");n.inBattle=false;c.mode=0;}
         c.customLook=true;c.wornMask=125;c.loadLook();n.custom[0]=true;n.wornMasks[0]=125;n.sex[0]=c.gender;n.skin[0]=c.skin;for(int k=0;k<5;k++)n.look[0][k]=c.look[k];n.controls[0]=true;n.inBattle=false;n.prepareBattle();if(c.actors[0]!=null)throw new AssertionError("Unused suit atlas retained for custom player");for(int k=0;k<5;k++)if(c.lookLayers[k]!=null&&n.layers[0][k]!=c.lookLayers[k])throw new AssertionError("Own clothing atlas loaded twice");c.mode=1;shot(c,"battle786-custom");
         n.tcp.close();n.logged=n.inBattle=false;n.worker=null;
        }
        c.gifts.load();int onlineStone=c.economy.onlineStones;c.gifts.use(0);if(c.gifts.level!=2||c.economy.offlineStones<50||c.economy.onlineStones!=onlineStone)throw new AssertionError("Offline gift isolation");var restored=new GiftBag(c);restored.load();if(restored.level!=2)throw new AssertionError("Offline chest did not persist");
        c.stop();System.out.println("PASS: TCP gift use, bag/forge rendering, VIP EXP cards and 8 menu-to-battle loads at "+(320*scale)+"x"+(240*scale));System.exit(0);
    }
}
