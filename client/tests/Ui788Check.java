import java.lang.reflect.*;
import java.util.*;
import org.microemu.app.*;
import org.microemu.app.util.DeviceEntry;
import org.microemu.device.*;
import org.microemu.device.impl.*;
import org.microemu.device.j2se.*;
import org.microemu.MIDletBridge;

public class Ui788Check {
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
        c.openLobby();c.experience=Progression.experienceForLevel(70);c.coins=999999;c.shoppingMoney=999354;
        c.mode=6;c.shop.open();c.shop.tab=3;c.shop.focus=0;c.stoneShop.focus=0;
        c.shop.key(0,2,false);if(c.stoneShop.focus!=1)throw new AssertionError("Right column");
        c.shop.key(0,8,false);if(c.stoneShop.focus!=3)throw new AssertionError("Down two cells");
        c.shop.key(0,1,false);if(c.stoneShop.focus!=2)throw new AssertionError("Left column");
        c.shop.key(0,4,false);if(c.stoneShop.focus!=0)throw new AssertionError("Up two cells");
        c.shop.key(0,8,false);c.shop.key(0,8,false);c.shop.key(0,8,false);if(c.shop.focus!=12)throw new AssertionError("Grid to pager");
        c.shop.focus=13;c.shop.key(10,0,true);if(c.stoneShop.focus!=6)throw new AssertionError("Next materials page");
        c.shop.key(0,4,false);if(c.shop.focus!=0)throw new AssertionError("Pager up returns to valid cell");
        c.shop.key(0,1,false);if(c.stoneShop.focus!=1)throw new AssertionError("Left page retains row");
        c.stoneShop.focus=0;c.shop.focus=0;shot(c,"shop788");c.shop.key(-6,0,false);shot(c,"shop788-tip");c.shop.key('B',0,false);if(!c.popup.opened)throw new AssertionError("B purchase shortcut");c.popup.opened=false;c.stoneShop.infoType=-1;
        c.mode=19;c.offlineDungeon.open();c.experience=Progression.experienceForLevel(70);shot(c,"dungeon788-offline");
        var n=c.net;n.host=true;n.inBattle=false;n.phase=0;n.dungeonStage=0;c.dungeon.openPicker(true);if(!c.dungeon.picker)throw new AssertionError("Legacy stage zero unlocked");c.dungeon.picker=false;n.dungeonStage=2;c.dungeon.openPicker(true);if(!c.dungeon.picker)throw new AssertionError("Boss room unlocked");c.dungeon.picker=false;
        var store=new CloudStore.Memory();store.register("ui788@local.test","test-password-123","Ronin");var u=new GunnyServer.Session();u.id=store.login("ui788@local.test","test-password-123").userId;u.profile=store.profile(u.id);u.profile.experience=Progression.experienceForLevel(70);u.social=u.expansion=true;
        try(var server=new GunnyServer(new java.net.InetSocketAddress("127.0.0.1",0),java.nio.file.Path.of("backend/assets"),store)){
         server.clock.shutdownNow();var room=server.new Room(u,6,2,-1,true);room.kind=3;room.dungeonStage=1;room.refreshWaiting();
         n.socialCap=n.expansionCap=true;n.modernRooms=n.actorsCap=n.equipmentCap=n.matchmakingCap=n.roomFixCap=n.waitBotCap=n.itemsCap=n.tribalCap=n.playerUiCap=false;
         room.cardNames[4]="Ronin";room.cardCoins[4]=317;
         for(int i=0;i<18;i++){n.cardCoins[i]=200+i*19;n.cardTypes[i]=i==15?4:0;}
         n.deckRevealed=true;
         var bytes=new java.io.ByteArrayOutputStream();server.lobby(new java.io.DataOutputStream(bytes),room,u);n.readLobby(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));
         for(int i=0;i<18;i++)if(i!=4&&n.cardCoins[i]!=200+i*19)throw new AssertionError("Sync erased revealed amount "+i);
         if(n.cardCoins[4]!=317||n.cardTypes[15]!=4)throw new AssertionError("Claimed and material rewards preserved");
         n.deckRevealed=false;n.logged=true;n.token="fixture";n.roomKind=3;n.roomTitle="Ải Viễn Chinh";n.actorExperience[0]=c.experience;n.dungeonStage=1;c.mode=16;c.dungeon.drawWaiting();shot(c,"dungeon788");
         if(c.dungeon.seatPortraits[0].getHeight()>39*c.s)throw new AssertionError("Portrait bounded to seat");
         c.dungeon.openPicker(true);shot(c,"picker788");c.dungeon.keyRoom(true,0,2,false);if(c.dungeon.draftDungeon!=1)throw new AssertionError("Tribal selected");shot(c,"picker788-tribal");c.dungeon.picker=false;
         n.inBattle=true;n.roomKind=0;n.deckRevealed=true;c.mode=5;n.results.stage=1;n.results.claimed=true;n.results.chosen=4;n.results.coins=317;n.myActor=0;n.names[0]="Ronin";n.names[1]="Kenji";shot(c,"cards788");
        }
        c.stop();System.out.println("PASS spatial Shop, B purchase, unlocked maps, bounded portraits, HP and revealed deck sync at "+(320*scale)+"x"+(240*scale));System.exit(0);
    }
}
