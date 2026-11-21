import java.lang.reflect.*;
import java.util.*;
import org.microemu.app.*;
import org.microemu.app.util.DeviceEntry;
import org.microemu.device.*;
import org.microemu.device.impl.*;
import org.microemu.device.j2se.*;
import org.microemu.MIDletBridge;

public class Ui789Check {
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
        c.forge.open();c.forge.key(10,0,true);if(c.forge.grade!=1)throw new AssertionError("First preview");int chance=c.forge.percent();
        c.forge.key(0,2,false);if(c.forge.grade!=2||c.forge.percent()<=chance||c.forge.step!=1)throw new AssertionError("Preview updates before OK");
        c.forge.key(0,2,false);if(c.forge.grade!=3)throw new AssertionError("Live Lv3 preview");shot(c,"forge789-preview");
        c.forge.key(10,0,true);int index=ForgeRules.index(c.gender,c.forge.kind,c.forge.item);c.economy.offlineRanks[index]=c.forge.expected+1;c.forge.complete();c.forge.animStart=System.currentTimeMillis()-350;shot(c,"forge789-success");
        if(c.forge.resultKind!=1||c.forge.resultSuccess==null||c.forge.star==null)throw new AssertionError("Native success and shimmer loaded");c.forge.complete();if(c.forge.resultKind!=-1||c.forge.successUntil!=0)throw new AssertionError("Failure has its own native result, no success glow");shot(c,"forge789-failure");
        c.gifts.load();c.gifts.bags=1;c.mode=8;c.bag.open();for(int i=0;i<c.bag.count;i++)if(c.bag.kinds[i]==BagScreen.GIFT_KIND&&c.bag.items[i]==1)c.bag.choose(i);c.bag.info=true;c.bag.infoKind=BagScreen.GIFT_KIND;c.bag.infoItem=1;shot(c,"gift789-tip");c.bag.info=false;
        if(c.glyph('-')==c.glyph('?'))throw new AssertionError("Range separator glyph");
        var n=c.net;var result=n.results;n.logged=n.inBattle=true;n.worker=Thread.currentThread();n.roomId=1001;n.roomKind=3;n.expansionCap=true;n.dungeonStage=3;n.rewardPicks=0;n.pending="profile";n.operation="sync";n.busy=true;for(int i=0;i<6;i++)n.names[i]="Đồng đội";n.names[0]="Ronin";c.game.over=true;c.game.winner=0;
        result.load();result.hero=c.standingPreview(c.gender,c.skin,true,96,120);result.stage=1;c.mode=5;
        if(n.receivingReward())throw new AssertionError("Background sync is not a reward request");result.expandedCards.activate();
        if(!n.rewardOperation.equals("claim780")||!n.receivingReward()||!n.pending.equals("profile")||n.rewardPick!=0)throw new AssertionError("Single press queues despite busy background/pending profile");
        int draft=n.cardDraft;result.expandedCards.selected=1;result.expandedCards.activate();if(n.cardDraft!=draft)throw new AssertionError("Repeated press cannot change in-flight claim");
        n.rewardOperation="";n.operation="";n.pending="";n.busy=false;
        for(int i=0;i<18;i++){n.cardNames[i]="";n.cardCoins[i]=30+i;n.cardTypes[i]=i%4==0?4:1;}
        n.deckRevealed=true;n.rewardPicks=1;shot(c,"cards789-nine");if(result.expandedCards.columns()!=3||result.expandedCards.limit()!=1)throw new AssertionError("Nine-card crossing");
        n.dungeonStage=4;if(result.expandedCards.columns()!=3||result.expandedCards.limit()!=1)throw new AssertionError("Second crossing has nine cards");
        n.dungeonStage=6;n.rewardPicks=2;shot(c,"cards789-boss");if(result.expandedCards.columns()!=6||result.expandedCards.limit()!=2||DungeonRewards.tribalNumber(6)!=3)throw new AssertionError("18-card third encounter boss");
        c.stop();System.out.println("PASS one-press queued rewards, live stone preview, native forge results and 9/9/18 cards at "+(320*scale)+"x"+(240*scale));System.exit(0);
    }
}
