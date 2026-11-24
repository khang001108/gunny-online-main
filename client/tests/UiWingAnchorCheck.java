import java.lang.reflect.*;
import java.util.*;
import org.microemu.app.*;
import org.microemu.app.util.DeviceEntry;
import org.microemu.device.*;
import org.microemu.device.impl.*;
import org.microemu.device.j2se.*;
import org.microemu.MIDletBridge;

public class UiWingAnchorCheck {
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

        c.beginMatch(0);n.inBattle=true;n.count=2;c.game.unitCount=2;c.mode=1;n.actorWing[0]=4;n.actorVip[0]=0;c.game.units[0].x=180;c.game.units[0].y=160;c.game.active=0;c.game.units[0].weapon=0;c.game.units[0].hp=30000;
        for(int i=0;i<2;i++){n.names[i]="Player "+i;n.present[i]=true;n.wornMasks[i]=3;n.custom[i]=false;c.actors[i]=c.load("look"+n.sex[i]+"_0_"+n.skin[i]);c.actorStand[i]=n.buildStanding(i,32,40);}
        c.game.units[1].x=450;
        c.vip.prepareBattle();
        for(int face=-1;face<=1;face+=2)for(int slope=-30;slope<=30;slope+=30)for(int pose=0;pose<3;pose++){
            var u=c.game.units[0];u.face=face;u.slope=slope;u.walk=pose==1?4:0;u.attackTicks=pose==2?6:0;c.frame=8;
            shot(c,"wing-anchor-"+face+"-"+slope+"-"+pose);
        }
        var target=javax.microedition.lcdui.Image.createImage(320*scale,240*scale);
        for(int wing=0;wing<6;wing++){c.vip.release();n.actorWing[0]=wing;c.vip.prepareBattle();for(int slope=-30;slope<=30;slope+=15)for(int frame=0;frame<38;frame++){c.frame=frame;c.game.units[0].slope=slope;c.paint(target.getGraphics());}}
        n.actorWing[0]=2;c.game.units[0].attackTicks=0;n.expansionCap=false;c.mode=5;n.results.stage=1;
        shot(c,"wing-result-raised");
        n.expansionCap=true;n.roomKind=3;n.dungeonStage=1;n.results.hero=null;n.results.load();
        n.results.hero=n.buildStanding(0,96,120);shot(c,"wing-result-small-raised");
        if(c.vip.actorWing(6)!=-1||c.vip.actorExp(12)!=0)throw new AssertionError("NPC index bounds");
        c.stop();System.out.println("PASS wing poses both directions slopes at "+320*scale);System.exit(0);
    }
}


