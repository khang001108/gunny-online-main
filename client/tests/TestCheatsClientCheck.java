import java.lang.reflect.*;
import java.util.*;
import org.microemu.app.*;
import org.microemu.app.util.DeviceEntry;
import org.microemu.device.*;
import org.microemu.device.impl.*;
import org.microemu.device.j2se.*;
import org.microemu.MIDletBridge;

public class TestCheatsClientCheck {
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
        c.openLobby();
        var store=new CloudStore.Memory();store.register("cheat@local.test","test-password-123","Gunner Test");
        var profile=store.profile(store.login("cheat@local.test","test-password-123").userId);profile.characterChosen=true;
        try(var server=new GunnyServer(new java.net.InetSocketAddress("127.0.0.1",0),java.nio.file.Path.of("backend/assets"),store)){
            server.clock.shutdownNow();server.startTcp("127.0.0.1",0,4);
            var n=c.net;n.alive=false;n.worker=Thread.currentThread();n.endpoint="socket://127.0.0.1:"+server.tcpPort();n.email="cheat@local.test";n.password="test-password-123";
            n.probeHealth(3000);var login=n.postOnce("login");n.token=login.readUTF();n.readProfile(login);n.logged=true;
            c.economy.read(n.postOnce("economy"));if(c.economy.testCheats)throw new AssertionError("Default permission");c.openCheats();if(c.mode==9)throw new AssertionError("Unapproved menu opened");
            store.testCheat(profile.id,6,true);c.economy.read(n.postOnce("economy"));c.keyPressed('g');if(c.mode!=9||c.cheatOptions()!=6)throw new AssertionError("G menu");shot(c,"test-cheats");
            for(int action=0;action<5;action++){
                c.cheatSelection=action;var reply=n.postOnce("testcheat");n.readProfile(reply);c.shoppingMoney=reply.readInt();c.experience=reply.readInt();c.economy.read(reply);
                if(reply.available()!=0)throw new AssertionError("Cheat response trailing bytes");
            }
            if(c.economy.stones()!=999999||c.shoppingMoney!=999999||Progression.level(c.experience)!=70||!c.economy.testGod)throw new AssertionError("Cheat values");
            shot(c,"test-cheats-enabled");
            n.create.name="Test skip";n.create.secret="";n.create.locked=false;n.dungeonDifficulty=0;n.readLobby(n.postOnce("tribalcreate"));n.readLobby(n.postOnce("start"));var r=server.rooms.get(n.roomId);r.phase=1;
            var sync=n.postOnce("sync");n.ack=sync.readInt();n.readLobby(sync);boolean done=sync.readBoolean();int turn=sync.readInt();n.chunks.read(sync,done,turn);
            c.mode=1;c.keyPressed('g');c.cheatSelection=5;var skipped=n.postOnce("testcheat");n.readProfile(skipped);c.shoppingMoney=skipped.readInt();c.experience=skipped.readInt();c.economy.read(skipped);
            sync=n.postOnce("sync");n.ack=sync.readInt();n.readLobby(sync);done=sync.readBoolean();turn=sync.readInt();n.chunks.read(sync,done,turn);
            if(!c.game.over||c.mode!=5||n.chunks.frames!=null)throw new AssertionError("Skip final snapshot/result UI");
            store.testCheat(profile.id,6,false);c.economy.read(n.postOnce("economy"));if(c.economy.testCheats||c.economy.testGod)throw new AssertionError("Revocation");
            n.tcp.close();n.logged=n.inBattle=false;n.worker=null;
        }
        c.offlineDungeon.active=true;c.offlineDungeon.stage=1;c.game.winner=0;c.game.over=true;int earned=0,before=c.economy.offlineStones;for(int i=0;i<4;i++)before+=c.economy.offlineMaterials[i];
        for(int d=0;d<3;d++){c.offlineDungeon.difficulty=d;for(int i=0;i<30;i++){var cards=c.net.results.expandedCards;cards.reset();cards.activate();int reward=cards.amount[0];earned+=reward;cards.activate();if(cards.offlinePicks!=1||cards.type[0]<1||cards.type[0]>7||cards.type[0]==2||cards.type[0]==3)throw new AssertionError("Offline graded reward/retry");}}
        int after=c.economy.offlineStones;for(int i=0;i<4;i++)after+=c.economy.offlineMaterials[i];if(after-before!=earned)throw new AssertionError("Offline stones not credited exactly once");
        c.stop();System.out.println("PASS: Real TCP cheat authorization, G menu, five profile actions, stage skip to result UI, revocation offline graded rewards and render at "+(320*scale)+"x"+(240*scale));System.exit(0);
    }
}
