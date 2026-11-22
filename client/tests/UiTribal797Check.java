import java.lang.reflect.*;
import java.util.*;
import org.microemu.app.*;
import org.microemu.app.util.DeviceEntry;
import org.microemu.device.*;
import org.microemu.device.impl.*;
import org.microemu.device.j2se.*;
import org.microemu.MIDletBridge;

public class UiTribal797Check {
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

        var n=c.net;n.worker=Thread.currentThread();n.logged=true;n.capabilities(GunnyServer.capabilities(false,true));
        var store=new CloudStore.Memory();var host=Expansion780Check.session(store,"TribalUi797",70);
        host.tribal797=host.clock793=host.vip=host.delta=host.tribal=host.playerUi=host.prefetch=host.expansion=host.campaign=host.flock=host.actors=host.itemFx=host.items=host.equipment=host.arsenal=host.modern=host.async=host.events=host.progress=host.bagLayout=host.social=host.matchmaking=host.roomFix=host.waitBot=true;host.itemVersion=2;host.live=false;
        try(var server=new GunnyServer(new java.net.InetSocketAddress("127.0.0.1",0),java.nio.file.Path.of("backend/assets"),store)){
         server.clock.shutdownNow();
         for(int stage:new int[]{3,4,6}){
          var room=server.new Room(host,6,2,-1,true);room.kind=3;room.strict=true;room.dungeonStage=stage;room.refreshWaiting();room.start();
          n.inBattle=false;var wire=new java.io.ByteArrayOutputStream();server.lobby(new java.io.DataOutputStream(wire),room,host);n.readLobby(new java.io.DataInputStream(new java.io.ByteArrayInputStream(wire.toByteArray())));n.prepareBattle();sync(server,room,host,c);
          if(c.game.dungeonStage!=stage||c.map!=room.map||c.game.unitCount!=13)throw new AssertionError("Campaign snapshot/map");
          c.mode=16;shot(c,"tribal797-room-"+stage);c.mode=14;c.loadTicks=20;shot(c,"tribal797-loading-"+stage);
          c.mode=1;c.cameraY=0;c.cameraX=stage==6?150:0;c.frame=9;shot(c,"tribal797-intro-"+stage);
          room.battle.tribalFx=0;room.battle.tribalState=0;room.battle.botPhase=0;sync(server,room,host,c);shot(c,"tribal797-stage-"+stage);
          if(stage==3){c.cameraX=160;c.cameraY=40;shot(c,"tribal797-tribes-spawn");c.cameraX=0;c.cameraY=0;}
          if(stage==4){c.cameraX=160;shot(c,"tribal797-bulan-right");c.cameraX=0;}
          room.battle.tribalClock=0;
          for(int t=0;t<62;t++)room.battle.tick();sync(server,room,host,c);c.frame=71;shot(c,"tribal797-action-"+stage);
          // Every pose row/column must load and draw in the actual handset JAR.
          for(int actor=0;actor<c.game.unitCount;actor++)if(c.game.units[actor].npc>=4){int kind=c.game.units[actor].npc;var atlas=c.tribalArt.atlas(kind);int cell=c.tribalArt.size(kind)*c.s;if(atlas==null||atlas.getWidth()!=cell*8||atlas.getHeight()!=cell*8)throw new AssertionError("Native pose grid "+kind);for(int state=0;state<8;state++){c.game.tribalState=state;for(int frame=0;frame<8;frame++){c.frame=frame*3;c.paint(javax.microedition.lcdui.Image.createImage(320*c.s,240*c.s).getGraphics());}}}
          if(stage==6){
           room.battle.units[1].hp=0;room.battle.check();sync(server,room,host,c);shot(c,"tribal797-transform");
           room.battle.tribalFx=0;room.battle.tribalState=0;room.battle.tribalClock=0;room.battle.active=1;TribalRules.summon(room.battle,TribalRules.BEAN,190);room.battle.tribalRound=3;
           for(int t=0;t<61;t++)room.battle.tick();sync(server,room,host,c);c.frame=29;shot(c,"tribal797-red-fire");if(c.game.ballCount!=3)throw new AssertionError("Three red fire payloads in snapshot");
           for(int id=11;id<=14;id++){var ball=new Battle.Ball();ball.weapon=id;ball.vx=900;ball.vy=-150;ball.x=240*256;ball.y=100*256;c.game.balls[0]=ball;c.game.ballCount=1;c.game.stampTrail(ball);shot(c,"tribal797-payload-"+id);}
           c.game.ballCount=0;
          }
          c.tribalArt.release();for(var im:c.tribalArt.sprites)if(im!=null)throw new AssertionError("Stage cache release");
         }
        }
        n.inBattle=n.logged=false;c.beginMatch(0);c.mode=1;shot(c,"tribal797-pvp-regression");if(c.game.dungeonStage!=0)throw new AssertionError("PvP reset");
        c.stop();System.out.println("PASS real MIDP campaign renderer, rooms/loading, authoritative snapshots, native poses, 4 NPC payloads, cache release and PvP at "+(320*scale)+"x"+(240*scale));System.exit(0);
    }
    static void sync(GunnyServer server,GunnyServer.Room room,GunnyServer.Session host,PhoneCanvas c)throws Exception{
     var wire=new java.io.ByteArrayOutputStream();server.snapshot(new java.io.DataOutputStream(wire),room,0,host);var input=new java.io.DataInputStream(new java.io.ByteArrayInputStream(wire.toByteArray()));c.net.readSnapshot(input);if(input.available()!=0)throw new AssertionError("Snapshot trailing bytes");
    }
}
