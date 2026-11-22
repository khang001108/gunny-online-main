import java.lang.reflect.*;
import java.util.*;
import org.microemu.app.*;
import org.microemu.app.util.DeviceEntry;
import org.microemu.device.*;
import org.microemu.device.impl.*;
import org.microemu.device.j2se.*;
import org.microemu.MIDletBridge;

public class Ui795Check {
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
        c.mode=8;c.bag.open();shot(c,"vip790-bag");
        int cardCount=0;for(int i=0;i<c.bag.count;i++)if(c.bag.kinds[i]==BagScreen.VIP_KIND)cardCount++;
        if(cardCount!=13)throw new AssertionError("13 VIP items in inventory");
        c.bag.switchTab(1);int wings=0;for(int i=0;i<c.bag.count;i++)if(c.bag.kinds[i]==BagScreen.WING_KIND)wings++;
        if(wings!=5||c.vip.hp()!=4000)throw new AssertionError("Permanent owned wing inventory/HP");shot(c,"vip790-wings");
        c.bag.choose(41);c.bag.activate();if(!n.pending.equals("vipequip")||c.vip.item!=-1||c.vip.equipped!=4)throw new AssertionError("Unequip waits for server");n.pending="";c.vip.requestKey="";
        c.bag.switchTab(0);for(int i=0;i<c.bag.count;i++)if(c.bag.kinds[i]==BagScreen.VIP_KIND){c.bag.page=i/25;c.bag.choose(i%25);break;}
        c.bag.activate();int selected=c.vip.item;String key=c.vip.requestKey;c.vip.submit("vipuse",12);
        if(!n.pending.equals("vipuse")||c.vip.cards[selected]!=999||c.vip.item!=selected||!c.vip.requestKey.equals(key))throw new AssertionError("Repeated press preserves pending item and receipt");n.pending="";
        c.mode=6;c.shop.open();c.shop.tab=3;c.shop.focus=19;c.shop.key(0,2,false);
        if(c.shop.tab!=4||c.shop.focus!=43)throw new AssertionError("VIP tab is next to Props");
        c.shop.key(0,8,false);c.shop.key(0,8,false);if(c.vip.shopFocus!=2)throw new AssertionError("VIP two-column navigation");shot(c,"vip790-shop");
        c.vip.experience=0;c.vip.owned=0;c.vip.shopFocus=4;c.vip.buy();if(n.pending.length()!=0)throw new AssertionError("Locked wing cannot buy");shot(c,"vip790-shop-locked");c.vip.info=true;shot(c,"vip790-shop-info");c.vip.info=false;
        c.vip.experience=VipRules.TOTAL;c.vip.buy();if(!n.pending.equals("vipbuy")||c.vip.owned!=0)throw new AssertionError("Buy waits for server");n.pending="";
        c.mode=0;n.social.opened=n.social.inviting=true;n.social.friendsPopup=false;n.social.tab=0;n.social.count=2;n.social.names[0]=null;n.social.names[1]="Kenji";n.social.online[1]=true;shot(c,"vip790-invite");
        if(c.pcWidth(null)!=0||!c.pcShort(null,20).equals(""))throw new AssertionError("Null invite names safe");n.social.opened=n.social.inviting=false;
        n.social.viewing=true;n.social.infoName="Kenji";n.social.infoVip=VipRules.TOTAL;n.social.infoWing=3;n.social.infoExp=c.experience;n.social.infoHero=c.standingPreview(c.gender,c.skin,true,64,80);n.social.opened=true;shot(c,"vip790-friend");n.social.opened=false;n.social.viewing=false;
        c.mode=8;c.vip.equipped=4;c.vip.owned=31;int cumulative=0;for(int i=0;i<9;i++){cumulative+=VipRules.COST[i];c.vip.experience=cumulative;if(VipRules.level(cumulative)!=i+1)throw new AssertionError("VIP tier "+i);shot(c,"vip790-tier-"+(i+1));}
        c.vip.release();for(int i=0;i<3;i++)for(int j=0;j<3;j++)if(c.vip.rings[i][j]!=null)throw new AssertionError("Ring release");
        if(c.vip.wings[4][0]!=null)throw new AssertionError("Wing release");
        n.inBattle=true;n.roomKind=0;n.myActor=0;n.settled=n.cardsOpen=true;n.resultsReady=false;c.game.over=true;c.game.winner=0;n.results.reset();n.results.syncStage();
        if(n.results.stage!=1)throw new AssertionError("Other player's ACK does not block own cards");n.results.claimed=true;n.results.activate();
        if(!n.pending.equals("leave"))throw new AssertionError("Own claim can return before deck reveal");n.pending="";
        n.inBattle=false;c.vip.resetAccount();if(c.vip.exp()!=0||c.vip.cards[0]!=0||c.vip.wing()!=-1)throw new AssertionError("New account clears old VIP state");
        n.inBattle=false;c.vip.experience=VipRules.TOTAL;c.vip.equipped=4;c.wornMask=3;c.equipped=0;c.owned=1;c.beginMatch(0);n.inBattle=true;n.count=2;for(int i=0;i<2;i++){n.names[i]="Player "+i;n.present[i]=true;n.wornMasks[i]=3;}n.actorVip[0]=VipRules.TOTAL;n.actorWing[0]=4;c.actorStand[0]=n.buildStanding(0,32,40);c.actorStand[1]=n.buildStanding(1,32,40);c.game.units[0].experience=c.experience;c.game.units[0].wingHp=4000;ItemRules.apply(c.game.units[0],3);c.mode=1;c.game.units[0].x=180;c.game.units[0].y=160;c.game.active=0;c.frame=95;shot(c,"vip790-combat");
        c.game.pow=true;c.startPowCutin();c.powCutinTicks=c.POW_CUTIN_TIME-50;shot(c,"vip790-pow-vip");
        c.vip.experience=0;n.actorVip[0]=0;shot(c,"vip790-pow-zero");c.powCutinTicks=0;
        n.inBattle=false;c.openLobby();n.logged=true;c.vip.experience=VipRules.TOTAL;c.vip.equipped=4;c.mode=8;c.bag.open();
        display.setDisplayRectangle(new Rectangle(0,0,240*scale,320*scale));display.setDisplayPaintable(new Rectangle(0,0,240*scale,320*scale));
        var widthField=javax.microedition.lcdui.Displayable.class.getDeclaredField("width");var heightField=javax.microedition.lcdui.Displayable.class.getDeclaredField("height");widthField.setAccessible(true);heightField.setAccessible(true);widthField.setInt(c,240*scale);heightField.setInt(c,320*scale);
        var portraitImage=javax.microedition.lcdui.Image.createImage(240*scale,320*scale);c.paint(portraitImage.getGraphics());
        if(!c.portraitViewport||c.pointerX(239*scale,0)!=0||c.pointerY(0,0)!=240*scale-1)throw new AssertionError("Portrait viewport and inverse pointer mapping "+c.getWidth()+"x"+c.getHeight()+" turn="+c.portraitViewport+" x="+c.pointerX(239*scale,0)+" y="+c.pointerY(0,0));
        int[] portraitPixels=new int[240*320*scale*scale];portraitImage.getRGB(portraitPixels,0,240*scale,0,0,240*scale,320*scale);
        var portraitPng=new java.awt.image.BufferedImage(240*scale,320*scale,java.awt.image.BufferedImage.TYPE_INT_RGB);portraitPng.setRGB(0,0,240*scale,320*scale,portraitPixels,0,240*scale);javax.imageio.ImageIO.write(portraitPng,"png",java.nio.file.Path.of("client/build/vip790-portrait-"+scale+".png").toFile());
        display.setDisplayRectangle(new Rectangle(0,0,320*scale,240*scale));display.setDisplayPaintable(new Rectangle(0,0,320*scale,240*scale));widthField.setInt(c,320*scale);heightField.setInt(c,240*scale);shot(c,"vip790-final");if(c.portraitSurface!=null)throw new AssertionError("Portrait buffer released on landscape");
        var frameImage=javax.microedition.lcdui.Image.createImage(320*scale,240*scale);
        for(int round=0;round<6;round++){c.beginMatch(round%6);n.inBattle=true;c.mode=1;n.actorVip[0]=VipRules.TOTAL;n.actorWing[0]=round%5;for(int tick=0;tick<60;tick++){c.frame++;c.paint(frameImage.getGraphics());}c.openLobby();n.inBattle=false;c.mode=8;c.bag.open();c.paint(frameImage.getGraphics());}
        var store=new CloudStore.Memory();var host=Expansion780Check.session(store,"VipUiHost",70);var guest=Expansion780Check.session(store,"VipUiGuest",30);
        host.vip=host.social=host.expansion=host.actors=host.equipment=host.modern=host.async=host.tribal=host.playerUi=true;
        host.clock793=true;n.clock793=true;host.loadingPercent=37;host.events=false;host.profile.vipExp=VipRules.TOTAL;host.profile.equippedWing=4;host.profile.wingOwned=31;guest.profile.vipExp=1800;guest.profile.equippedWing=1;
        try(var server=new GunnyServer(new java.net.InetSocketAddress("127.0.0.1",0),java.nio.file.Path.of("backend/assets"),store)){
          server.clock.shutdownNow();var room=server.new Room(host,6,2,-1,true);room.kind=3;room.dungeonStage=1;room.members[2]=guest;room.refreshWaiting();
          n.logged=true;n.inBattle=false;n.vipCap=n.socialCap=n.expansionCap=n.actorsCap=n.equipmentCap=n.modernRooms=n.asyncTurns=n.tribalCap=n.playerUiCap=true;
          n.matchmakingCap=n.roomFixCap=n.waitBotCap=n.itemsCap=n.departureCap=false;
          var wire=new java.io.ByteArrayOutputStream();server.lobby(new java.io.DataOutputStream(wire),room,host);var read=new java.io.DataInputStream(new java.io.ByteArrayInputStream(wire.toByteArray()));n.readLobby(read);
          if(read.available()!=0||n.actorVip[2]!=1800||n.actorWing[2]!=1||c.game.units[2].wingHp!=1000)throw new AssertionError("Server roster decodes VIP and equipped wing/HP without byte drift");
          c.mode=16;shot(c,"vip790-dungeon-waiting");room.kind=2;room.refreshWaiting();wire.reset();server.lobby(new java.io.DataOutputStream(wire),room,host);n.readLobby(new java.io.DataInputStream(new java.io.ByteArrayInputStream(wire.toByteArray())));shot(c,"vip790-pvp-waiting");n.rewardWaitingMask=4;shot(c,"vip790-pvp-rewards-waiting");if(!n.waiting.rewardLabel(2).equals("Nhận quà")||!n.waiting.rewardLabel(0).equals("Đã xong"))throw new AssertionError("Every party member has a reward status label");
        }
        n.roomKind=1;if(!n.results.returnOperation().equals("leave"))throw new AssertionError("Solo result exits to lobby");
        c.economy.testCheats=true;c.returnMode=0;c.mode=9;c.cheatSelection=6;shot(c,"fix791-cheat");if(c.cheatOptions()!=7)throw new AssertionError("VIP cheat visible");
        n.logged=true;n.inBattle=false;c.mode=0;c.ranking.opened=true;var rankBytes=new java.io.ByteArrayOutputStream();var rankOut=new java.io.DataOutputStream(rankBytes);rankOut.writeShort(8);for(int i=0;i<8;i++){rankOut.writeInt(1000+i);rankOut.writeInt(Progression.experienceForLevel(70-i));rankOut.writeUTF("Người chơi "+(i+1));rankOut.writeByte(i%8);rankOut.writeInt(35000-i*1000);rankOut.writeInt(120-i*10);}c.ranking.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(rankBytes.toByteArray())));shot(c,"fix793-ranking");c.ranking.key(0,8,false);if(c.ranking.selected!=1)throw new AssertionError("Rank scrolling");c.ranking.opened=false;
        if(c.ranking.levels[0]==null||c.ranking.weapons[0]==null)throw new AssertionError("Native leaderboard icons");
        c.mode=8;n.pending="";c.stoneShop.convert(2);if(!c.popup.opened||c.popup.action!=31||n.pending.length()!=0)throw new AssertionError("Stone conversion waits for confirmation");c.popup.selected=0;c.popup.choose();if(n.pending.length()!=0)throw new AssertionError("Cancel preserves stones");c.stoneShop.convert(2);c.popup.selected=1;c.popup.choose();if(!n.pending.equals("vipuse")||c.vip.item!=15)throw new AssertionError("Confirmed Lv3 conversion wire item");n.pending="";

        n.clock793=true;n.inBattle=true;n.settled=n.cardsOpen=true;c.game.over=true;c.game.winner=0;n.myActor=0;n.rewardDeadline=System.currentTimeMillis()+12000;n.results.syncStage();if(n.results.stage!=0)throw new AssertionError("Two-second result delay");n.rewardDeadline=System.currentTimeMillis()+9999;n.results.syncStage();if(n.results.stage!=1)throw new AssertionError("Cards open after delay");n.pending="";n.busy=false;n.rewardDeadline=System.currentTimeMillis()-1;n.results.tick();if(!n.pending.equals("autocards"))throw new AssertionError("Ten-second auto claim request");n.pending="";n.inBattle=false;
        n.logged=false;n.startupTried=false;c.presentation.boot=0;n.startupLogin();if(n.startupTried)throw new AssertionError("Login waits for sound choice");c.presentation.key(10,0,true);c.presentation.tick();if(!n.startupTried||c.mode!=16||n.form==null)throw new AssertionError("Login after sound choice");
        n.logged=true;n.inBattle=false;n.clock793=true;c.mode=6;c.shop.open();c.shop.tab=4;c.shop.focus=0;c.vip.shopFocus=1;n.pending="";c.vip.shopKey(10,0,true);if(c.vip.trialWing!=1||n.pending.length()!=0)throw new AssertionError("Wing OK previews without buying");shot(c,"fix793-shop-preview");
        c.mode=14;n.loadingPercent[0]=37;n.loadingPercent[1]=61;n.loadingMask=0;n.bots[0]=n.bots[1]=false;c.game.unitCount=2;n.present[0]=n.present[1]=true;n.inBattle=true;n.roomKind=0;n.names[0]="P1";n.names[1]="P2";n.actorVip[0]=n.actorVip[1]=0;c.loadTicks=20;c.game.over=false;c.powCutinTicks=0;shot(c,"fix793-loading");
        c.bag.switchTab(1);c.bag.list();for(int i=0;i<c.bag.count;i++)if(c.bag.kinds[i]==BagScreen.WING_KIND){c.bag.page=i/25;c.bag.focus=i%25;c.bag.wingHeld=-1;n.pending="";c.bag.activate();if(n.pending.length()>0)throw new AssertionError("First wing OK only selects");c.bag.activate();if(!n.pending.equals("vipequip"))throw new AssertionError("Second wing OK equips");n.pending="";break;}
        n.inBattle=false;n.logged=true;c.mode=0;c.presentation.settings=true;c.presentation.option=2;c.presentation.key(0,8,false);if(c.presentation.option!=3)throw new AssertionError("Logout settings navigation");shot(c,"ui794-settings");c.presentation.key(10,0,true);if(!c.popup.opened||c.popup.action!=32)throw new AssertionError("Logout confirmation");c.popup.selected=0;c.popup.choose();if(!n.logged)throw new AssertionError("Cancel logout keeps account");c.presentation.settings=false;
        c.mode=0;c.message("Thông báo dùng chung nền đen");shot(c,"ui794-home-toast");c.mode=6;c.shop.open();shot(c,"ui794-shop-toast");c.mode=8;c.bag.open();shot(c,"ui794-bag-toast");
        n.logged=false;n.loginForm(false);c.toastTicks=0;shot(c,"ui794-login");if(c.home.hit(22,217)!=-1||c.home.hit(300,217)!=4)throw new AssertionError("Login exposes exit only");for(int i=0;i<40;i++)c.net.auth.move(8);if(c.net.auth.selected>=100&&c.net.auth.selected!=104)throw new AssertionError("No hidden login toolbar focus");c.net.auth.leave();if(!c.popup.opened||c.popup.action!=NativePopup.CONFIRM_QUIT)throw new AssertionError("Back from login asks quit");c.popup.dismiss();
        c.popup.dismiss();n.logged=true;n.inBattle=false;c.mode=0;c.beginMatch(0);n.inBattle=true;n.count=2;c.game.unitCount=2;n.actorWing[0]=3;n.actorVip[0]=0;c.mode=1;c.game.units[0].x=180;c.game.units[0].y=160;c.game.active=0;c.game.units[0].weapon=0;c.game.units[0].hp=30000;for(int i=0;i<2;i++){n.present[i]=true;n.wornMasks[i]=3;n.custom[i]=false;c.actors[i]=c.load("look"+n.sex[i]+"_0_"+n.skin[i]);c.actorStand[i]=n.buildStanding(i,32,40);}
        for(int wing=0;wing<6;wing++){n.actorWing[0]=wing;for(int angle=-30;angle<=30;angle+=15){c.game.units[0].slope=angle;for(int tick=0;tick<38;tick++){c.frame++;c.paint(frameImage.getGraphics());}}}
        n.actorWing[0]=3;c.game.units[0].slope=0;for(int tick=0;tick<38;tick++){c.frame=tick;shot(c,"wing795-motion-"+tick);}c.vip.release();
        c.stop();System.out.println("PASS VIP inventory, wing slot, shop locks/navigation, receipt drafts, null invite, 9 tier renders, independent cards and cache release at "+(320*scale)+"x"+(240*scale));System.exit(0);
    }
}
