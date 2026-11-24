import java.lang.reflect.*;
import java.util.*;
import org.microemu.app.*;
import org.microemu.app.util.DeviceEntry;
import org.microemu.device.*;
import org.microemu.device.impl.*;
import org.microemu.device.j2se.*;
import org.microemu.MIDletBridge;

public class UiSulan7912Check {
    static void shot(PhoneCanvas c,String name)throws Exception{
        var image=javax.microedition.lcdui.Image.createImage(320*c.s,240*c.s);
        for(int step=0;step<40;step++)c.followCamera();
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

        var n=c.net;n.worker=Thread.currentThread();n.logged=true;n.capabilities(GunnyServer.capabilities(false,true));
        var store=new CloudStore.Memory();var host=Expansion780Check.session(store,"TribalUi797",70);
        host.sulan7912=host.tribal797=host.clock793=host.vip=host.delta=host.tribal=host.playerUi=host.prefetch=host.expansion=host.campaign=host.flock=host.actors=host.itemFx=host.items=host.equipment=host.arsenal=host.modern=host.async=host.events=host.progress=host.bagLayout=host.social=host.matchmaking=host.roomFix=host.waitBot=true;host.itemVersion=2;host.live=false;
        try(var server=new GunnyServer(new java.net.InetSocketAddress("127.0.0.1",0),java.nio.file.Path.of("backend/assets"),store)){
         server.clock.shutdownNow();
         if(c.game.dungeonStage!=0)throw new AssertionError("Cold start must not depend on Ant Cave initialization");
         for(int stage:new int[]{3,4,6}){
          var room=server.new Room(host,6,2,-1,true);room.kind=3;room.strict=true;room.dungeonStage=stage;room.refreshWaiting();room.start();if(stage==3)TribalRules.round(room.battle);
          n.inBattle=false;var wire=new java.io.ByteArrayOutputStream();server.lobby(new java.io.DataOutputStream(wire),room,host);n.readLobby(new java.io.DataInputStream(new java.io.ByteArrayInputStream(wire.toByteArray())));n.prepareBattle();
          if(!c.game.teams||c.game.dungeonStage!=stage)throw new AssertionError("Dungeon presentation restored before snapshot");
          for(int i=1;i<6;i+=2)if(c.loadingParticipant(i))throw new AssertionError("No enemy slot as loading hero after reconnect");
          c.mode=14;shot(c,"tribal797-reconnect-loading-"+stage);sync(server,room,host,c);
          room.chunkBegin(1);for(int tick=0;tick<18;tick++){room.battle.tick();room.chunkFrame();}room.chunkFlush(false);
          n.chunks.reset();n.prefetchCap=false;
          for(int segment=0;segment<room.chunks.size();segment++){
           var block=room.chunks.get(segment);var packetBytes=new java.io.ByteArrayOutputStream();var packetOut=new java.io.DataOutputStream(packetBytes);packetOut.writeByte(4);packetOut.writeInt(segment);packetOut.writeInt(block.length);packetOut.write(block);
           n.chunks.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(packetBytes.toByteArray())),false,room.battle.turn);
           while(n.chunks.frames!=null)n.chunks.animate();if(!n.logged)throw new AssertionError("Action playback disconnected stage "+stage);
          }
          n.chunks.frames=new java.io.DataInputStream(new java.io.ByteArrayInputStream(new byte[]{0,5,1}));n.chunks.remaining=1;n.chunks.playing=true;n.chunks.animate();
          if(!n.logged||n.chunks.cached()!=Integer.MIN_VALUE)throw new AssertionError("Broken action requests full state without disconnect");
          var recoveryBytes=new java.io.ByteArrayOutputStream();serverRecovery(server,room,host,recoveryBytes);n.chunks.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(recoveryBytes.toByteArray())),false,room.battle.turn);
          if(n.chunks.recovering||c.game.unitCount!=(stage==3?14:13))throw new AssertionError("Full state restores playable NPC roster");
          n.prefetchCap=true;
          boolean[] before=room.ground.solid.clone();room.ground.crater(240,220,60);
          if(!java.util.Arrays.equals(before,room.ground.solid)||!room.ground.craters.isEmpty())throw new AssertionError("Server Tribal ground unchanged");
          for(int x=30;x<450;x+=30){int y=c.surface(x,0);c.crater(x,y,30);if(c.surface(x,0)!=y)throw new AssertionError("Client Tribal ground unchanged");}
          if(c.game.dungeonStage!=stage||c.map!=room.map||c.game.unitCount!=(stage==3?14:13))throw new AssertionError("Campaign snapshot/map");
          c.mode=16;n.inBattle=false;n.phase=0;n.dungeonStage=stage;shot(c,"tribal798-room-"+stage);c.dungeon.beginPicker(true);c.dungeon.keyWaiting(0,2,false);c.dungeon.keyWaiting(10,0,true);c.dungeon.keyWaiting(0,2,false);shot(c,"tribal798-picker-"+stage);c.dungeon.picker=false;n.inBattle=true;c.mode=14;c.loadTicks=20;shot(c,"tribal797-loading-"+stage);
          c.mode=1;c.cameraY=0;c.cameraX=stage==6?150:0;c.frame=9;shot(c,"tribal797-intro-"+stage);
          if(stage==4){
           for(int age:new int[]{0,60,150,245}){
            room.battle.tribalFx=TribalRules.BULAN_INTRO_END-age;room.battle.tribalState=TribalRules.OPENING;room.battle.botPhase=age<45||age>=225?0:age<135?TribalRules.BULAN_LEFT:TribalRules.BULAN_INTRO;sync(server,room,host,c);
            for(int step=0;step<25;step++)c.followCamera();shot(c,"bulan-intro-"+age);
           }
           for(int actor:new int[]{3,1})for(int cycle=0;cycle<4;cycle++){
            TribalRules.setup(room.battle,2,1,false);room.battle.tribalFx=0;room.battle.active=actor;room.battle.units[actor].power=cycle;
            for(int tick=0;tick<88;tick++)TribalRules.tickAI(room.battle);sync(server,room,host,c);for(int step=0;step<25;step++)c.followCamera();shot(c,"bulan-skill-"+actor+"-"+cycle);
           }
           TribalRules.setup(room.battle,2,0,false);
          }
          room.battle.tribalFx=0;room.battle.tribalState=0;room.battle.botPhase=0;sync(server,room,host,c);shot(c,"tribal797-stage-"+stage);
          if(stage==3){c.cameraX=160;c.cameraY=40;shot(c,"tribal797-tribes-spawn");c.cameraX=0;c.cameraY=0;}
          if(stage==4){c.cameraX=160;shot(c,"tribal797-bulan-right");c.cameraX=0;}
          room.battle.tribalClock=0;
          for(int t=0;t<62;t++)room.battle.tick();sync(server,room,host,c);c.frame=71;shot(c,"tribal797-action-"+stage);
          if(stage==3){
           for(int actor=0;actor<room.battle.unitCount;actor++)if(room.battle.units[actor].npc==TribalRules.ELITE){
            room.battle.active=actor;room.battle.tribalClock=72;room.battle.ballCount=0;TribalRules.tickAI(room.battle);sync(server,room,host,c);
            if(c.game.ballCount!=3)throw new AssertionError("Three arcing spear shots received");
            for(int t=0;t<14;t++){room.battle.tick();sync(server,room,host,c);c.game.ageTrails();for(int i=0;i<c.game.ballCount;i++)c.game.stampTrail(c.game.balls[i]);}
            c.cameraX=120;c.cameraY=0;shot(c,"tribal797-three-spears");break;
           }
          }
          // Every pose row/column must load and draw in the actual handset JAR.
          for(int actor=0;actor<c.game.unitCount;actor++)if(c.game.units[actor].npc>=4){int kind=c.game.units[actor].npc;var atlas=c.tribalArt.atlas(kind);int cell=c.tribalArt.size(kind)*c.s;if(atlas==null||atlas.getWidth()!=cell*8||atlas.getHeight()!=cell*8)throw new AssertionError("Native pose grid "+kind);for(int state=0;state<8;state++){c.game.tribalState=state;for(int frame=0;frame<8;frame++){c.frame=frame*3;c.paint(javax.microedition.lcdui.Image.createImage(320*c.s,240*c.s).getGraphics());}}}
          if(stage==6){
           var b=room.battle;b.sulanExternalClock=false;b.deferNext=false;b.tribalState=TribalRules.OPENING;SulanRules.reset(b);
           int[] ages={0,60,91,190,270};for(int age:ages){while(SulanRules.INTRO-b.tribalFx<age)b.tick();sync(server,room,host,c);shot(c,"sulan7912-intro-"+age);}
           while(b.tribalFx>0)b.tick();b.units[1].hp=0;b.check();sync(server,room,host,c);shot(c,"sulan7912-borrow");
           for(int t=0;t<115;t++)b.tick();sync(server,room,host,c);shot(c,"sulan7912-rebirth");
           for(int t=115;t<SulanRules.TRANSFORM-10;t++)b.tick();sync(server,room,host,c);shot(c,"sulan7912-bean");
           while(b.tribalFx>0)b.tick();b.beginTurn(1);for(int t=0;t<75;t++)b.tick();sync(server,room,host,c);shot(c,"sulan7912-hellfire");
           int turn=b.turn;for(int t=0;t<900&&turn==b.turn;t++)b.tick();if(b.active!=6)throw new AssertionError("Sulan ends all salvos before bean turn");
           for(int t=0;t<26;t++)b.tick();sync(server,room,host,c);shot(c,"sulan7912-extinguish");if(c.game.sulanFireCount!=0)throw new AssertionError("Fire state synced to MIDP");
           b.beginTurn(1);for(int t=0;t<66;t++)b.tick();sync(server,room,host,c);shot(c,"sulan7912-fury");
           b.units[1].hp=0;b.check();for(int t=0;t<125;t++)b.tick();sync(server,room,host,c);shot(c,"sulan7912-charred");
           for(int id=11;id<=14;id++){var ball=new Battle.Ball();ball.weapon=id;ball.vx=900;ball.vy=-150;ball.x=240*256;ball.y=100*256;c.game.balls[0]=ball;c.game.ballCount=1;c.game.stampTrail(ball);shot(c,"sulan7912-payload-"+id);}
           c.game.ballCount=0;
          }
          c.tribalArt.release();for(var im:c.tribalArt.sprites)if(im!=null)throw new AssertionError("Stage cache release");
         }
        }
        var tcpStore=new CloudStore.Memory();String mail="midp-bulan@tcp.test";tcpStore.register(mail,"test-password-123","BulanTCP");tcpStore.profile(tcpStore.login(mail,"test-password-123").userId).experience=Progression.experienceForLevel(70);
        try(var tcpServer=new GunnyServer(new java.net.InetSocketAddress("127.0.0.1",0),java.nio.file.Path.of("backend/assets"),tcpStore)){
         tcpServer.clock.shutdownNow();tcpServer.startTcp("127.0.0.1",0,8);
         try(var player=new TribalTcpCheck.Player(tcpServer.tcpPort(),mail,n.tcpFlags())){
          var lobby=player.call("tribalcreate",o->{o.writeByte(0);o.writeUTF("MIDP Bulan TCP");o.writeUTF("");});n.inBattle=false;n.readLobby(lobby);if(lobby.available()!=0)throw new AssertionError("TCP lobby fully decoded");
          var raid=tcpServer.rooms.get(n.roomId);
          for(int oldSeats:new int[]{2,4,6})for(int stage:new int[]{3,4,6}){
           raid.count=oldSeats;

           raid.phase=0;raid.battle=null;raid.dungeonStage=stage;raid.refreshWaiting();raid.start();raid.phase=1;n.inBattle=false;
           var packet=player.call("sync",o->{o.writeInt(0);o.writeByte(0);o.writeBoolean(true);o.writeInt(-1);o.writeBoolean(false);o.writeInt(raid.departureVersion);o.writeInt(-1);o.writeInt(-1);o.writeInt(-1);o.writeByte(0);});
           n.ack=packet.readInt();n.readLobby(packet);boolean done=packet.readBoolean();int turn=packet.readInt();n.chunks.read(packet,done,turn);
           if(packet.available()!=0||c.game.unitCount!=(stage==3?14:13))throw new AssertionError("TCP snapshot NPC count and byte alignment stage "+stage);
          }
         }
        }
        n.inBattle=false;n.phase=0;n.host=true;n.dungeonStage=4;n.pending="";n.operation="";c.mode=16;c.popup.dismiss();c.dungeon.openPicker(true);if(!c.popup.opened||c.popup.action!=34)throw new AssertionError("Progress reset warning");c.popup.selected=0;c.popup.choose();if(c.dungeon.picker||n.pending.length()>0)throw new AssertionError("Cancel keeps progress");c.dungeon.openPicker(true);c.popup.selected=1;c.popup.choose();if(!c.dungeon.picker||c.dungeon.pickerStep!=0)throw new AssertionError("Accept opens card grid without resetting yet");c.dungeon.draftDungeon=1;c.dungeon.keyWaiting(10,0,true);c.dungeon.keyWaiting(0,2,false);c.dungeon.keyWaiting(0,2,false);c.dungeon.keyWaiting(10,0,true);if(c.dungeon.pickerStep!=2)throw new AssertionError("Difficulty then confirmation");shot(c,"tribal798-hard-confirm");c.dungeon.keyWaiting(10,0,true);if(!n.pending.equals("dungeonsettings"))throw new AssertionError("Only confirmation sends map reset");n.pending="";n.dungeonStage=3;c.loadout.draw(true,true);var fixedCover=c.loadout.dungeonMapCover;n.dungeonStage=6;c.loadout.draw(true,true);if(c.loadout.dungeonMapCover!=fixedCover)throw new AssertionError("Selected family map stays fixed across stages");
        n.pending="";n.inBattle=false;n.phase=0;n.host=true;n.dungeonStage=6;n.dungeonDifficulty=2;c.mode=16;c.dungeon.picker=false;shot(c,"tribal799-hard-rewards");c.dungeon.beginPicker(true);c.dungeon.pointerWaiting(160,65);if(c.dungeon.draftDungeon!=1||c.dungeon.pickerStep!=1)throw new AssertionError("Right portrait card selects Tribal");c.dungeon.pointerWaiting(292,61);if(c.dungeon.pickerStep!=2)throw new AssertionError("Native right difficulty buttons");shot(c,"tribal799-picker-native");if(c.dungeon.portraitFrame.getWidth()!=82*scale||c.dungeon.portraitCards[0].getWidth()!=78*scale)throw new AssertionError("Native portrait cards remain inside left grid");c.dungeon.picker=false;
        n.logged=true;n.inBattle=false;n.phase=0;n.host=true;n.pending="";n.operation="";c.mode=16;c.dungeon.picker=false;c.popup.dismiss();c.experience=Progression.experienceForLevel(14);if(c.dungeon.levelAllowed(0)||!c.popup.text.contains("Lv 15"))throw new AssertionError("Ant blocks level14 with correct message");c.popup.dismiss();c.experience=Progression.experienceForLevel(15);if(!c.dungeon.levelAllowed(0))throw new AssertionError("Ant exact level15");c.experience=Progression.experienceForLevel(29);if(c.dungeon.levelAllowed(1)||!c.popup.text.contains("Lv 30")||!c.popup.text.contains("Lv 29"))throw new AssertionError("Tribal blocks level29 clearly");shot(c,"levels7911-underlevel");c.popup.dismiss();c.dungeon.beginPicker(true);c.dungeon.draftDungeon=1;c.dungeon.pickerStep=0;c.dungeon.keyWaiting(10,0,true);if(c.dungeon.pickerStep!=0||n.pending.length()>0||!c.popup.opened)throw new AssertionError("Blocked card cannot select difficulty");c.popup.dismiss();c.experience=Progression.experienceForLevel(30);c.dungeon.keyWaiting(10,0,true);if(c.dungeon.pickerStep!=1)throw new AssertionError("Tribal exact level30 selects difficulty");shot(c,"levels7911-requirements");c.dungeon.picker=false;n.pending="";c.experience=Progression.experienceForLevel(29);if(n.request("tribalcreate")||n.pending.length()>0)throw new AssertionError("Client cannot queue low-level Tribal creation");c.popup.dismiss();
        n.inBattle=n.logged=false;c.beginMatch(0);c.mode=1;shot(c,"tribal797-pvp-regression");if(c.game.dungeonStage!=0)throw new AssertionError("PvP reset");
        c.stop();System.out.println("PASS real MIDP campaign renderer, rooms/loading, authoritative snapshots, native poses, 4 NPC payloads, cache release and PvP at "+(320*scale)+"x"+(240*scale));System.exit(0);
    }
    static void serverRecovery(GunnyServer server,GunnyServer.Room room,GunnyServer.Session host,java.io.ByteArrayOutputStream bytes)throws Exception{room.chunkPacket(new java.io.DataOutputStream(bytes),host,0,room.battle.turn,-1,Integer.MIN_VALUE);}
    static void sync(GunnyServer server,GunnyServer.Room room,GunnyServer.Session host,PhoneCanvas c)throws Exception{
     var wire=new java.io.ByteArrayOutputStream();server.snapshot(new java.io.DataOutputStream(wire),room,0,host);var input=new java.io.DataInputStream(new java.io.ByteArrayInputStream(wire.toByteArray()));c.net.readSnapshot(input);if(input.available()!=0)throw new AssertionError("Snapshot trailing bytes");
    }
}
