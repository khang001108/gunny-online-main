import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.microemu.app.*;
import org.microemu.app.util.DeviceEntry;
import org.microemu.device.*;
import org.microemu.device.impl.*;
import org.microemu.device.j2se.*;
import org.microemu.MIDletBridge;

/** Actual MIDP drawing, packed-resource bounds, cache stability, and server/client round trip. */
public final class UiWeaponCombatCheck {
    static int checks;
    static void ok(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
    static int hash(javax.microedition.lcdui.Image image){
        int[] pixels=new int[image.getWidth()*image.getHeight()];
        image.getRGB(pixels,0,image.getWidth(),0,0,image.getWidth(),image.getHeight());
        return Arrays.hashCode(pixels);
    }
    static void screenshot(PhoneCanvas c,String name)throws Exception{
        var image=javax.microedition.lcdui.Image.createImage(320*c.s,240*c.s);
        c.paint(image.getGraphics());
        int w=image.getWidth(),h=image.getHeight();int[] pixels=new int[w*h];image.getRGB(pixels,0,w,0,0,w,h);
        var png=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_RGB);png.setRGB(0,0,w,h,pixels,0,w);
        var dest=Path.of("client/build/weapon-combat-review",name+"-"+w+".png");Files.createDirectories(dest.getParent());
        javax.imageio.ImageIO.write(png,"png",dest.toFile());
    }
    static int isolated(PhoneCanvas c,Battle.Ball ball,Battle.Blast blast){
        var image=javax.microedition.lcdui.Image.createImage(110*c.s,110*c.s);c.g=image.getGraphics();c.ox=c.oy=0;
        c.g.setColor(0x1c283b);c.g.fillRect(0,0,image.getWidth(),image.getHeight());
        if(ball!=null)c.combatWeapons.projectile(ball,55,55);else c.combatWeapons.impact(blast,55,55);
        return hash(image);
    }
    public static void main(String[] args)throws Exception{
        Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});
        int scale=Integer.parseInt(args[0]);System.setProperty("java.awt.headless","true");
        var headless=new Headless();var field=Headless.class.getDeclaredField("emulator");field.setAccessible(true);
        var common=(Common)field.get(headless);
        common.initParams(new ArrayList<>(List.of("--rms","memory","--appclassloader","system","GunnyMIDlet")),
            new DeviceEntry("Nokia",null,DeviceImpl.RESIZABLE_LOCATION,true,false),J2SEDevice.class);
        var display=(J2SEDeviceDisplay)DeviceFactory.getDevice().getDeviceDisplay();
        display.setDisplayRectangle(new Rectangle(0,0,320*scale,240*scale));display.setDisplayPaintable(new Rectangle(0,0,320*scale,240*scale));
        common.initMIDlet(true);Thread.sleep(250);var app=(GunnyMIDlet)MIDletBridge.getCurrentMIDlet();
        if(app.game==null)app.game=new PhoneCanvas(app);var c=app.game;c.running=false;Thread.sleep(100);
        c.presentation.boot=2;c.presentation.music=false;c.presentation.effects=true;c.tutorialDone=true;c.openLobby();
        c.training=true;c.owned=1023;c.wornMask=3;c.equipped=0;c.beginMatch(0);c.prepareBattleAssets();c.mode=1;
        ok(c.combatWeapons.loads<8,"Loading only decodes participant weapons");
        for(int weapon:new int[]{0,3,5,9,2,4,7,1}){
            c.combatWeapons.prepare(1<<weapon);var pack=c.combatWeapons.weapons[weapon];
            for(short[] frames:pack.frames)for(short frame:frames){
                int offset=(frame&65535)*6;ok(offset+5<pack.regions.length,"Packed frame references a region");
                ok((pack.regions[offset]+pack.regions[offset+2])*scale<=pack.atlas.getWidth()
                    &&(pack.regions[offset+1]+pack.regions[offset+3])*scale<=pack.atlas.getHeight(),"Region is within decoded atlas");
            }
            var ball=new Battle.Ball();ball.weapon=weapon;ball.age=6;ball.vx=900;ball.vy=-400;
            ball.powered=false;int normal=isolated(c,ball,null);ball.powered=true;
            ok(normal!=isolated(c,ball,null),"Normal and POW projectiles differ: "+weapon);
            var blast=new Battle.Blast();blast.weapon=weapon;blast.time=8;int commonHit=isolated(c,null,blast);blast.powered=true;
            ok(commonHit!=isolated(c,null,blast),"Normal and POW impacts differ: "+weapon);
            for(boolean powered:new boolean[]{false,true}){
                var poses=new HashSet<Integer>();blast.powered=powered;
                for(int tick=0;tick<25;tick++){blast.time=tick;poses.add(isolated(c,null,blast));}
                ok(poses.size()>=10,"Impact retains native animation poses: "+weapon+"/"+powered);
                // Render every rotation, animated projectile row, trail fade, and held transform.
                for(int age=0;age<160;age++){ball.age=age;ball.powered=powered;isolated(c,ball,null);}
                for(int direction=0;direction<16;direction++)for(int age=0;age<15;age++)
                    c.combatWeapons.trail(weapon,powered,direction,age,15,55,55);
                for(int frame=0;frame<8;frame++)for(int transform=0;transform<4;transform++)
                    c.combatWeapons.draw(weapon,powered,WeaponCombatArt.HELD,frame,55,55,transform);
                c.game.start(true);c.game.units[0].weapon=weapon;c.game.units[0].x=130;
                c.game.units[0].y=c.surface(130,0);c.game.units[0].angle=45;c.game.units[0].rage=100;
                c.game.units[1].x=430;c.game.units[1].y=c.surface(430,0);c.game.pow=powered;
                c.cameraX=c.cameraY=0;c.frame=20;c.toastTicks=0;c.powCutinTicks=0;
                ball.age=8;ball.x=205*256;ball.y=90*256;ball.owner=0;c.game.ballCount=1;c.game.balls[0]=ball;
                blast.x=245;blast.y=155;blast.time=8;c.game.blastCount=1;c.game.blasts[0]=blast;
                screenshot(c,"weapon-"+weapon+(powered?"-pow":"-normal"));
            }
        }
        int loads=c.combatWeapons.loads;ok(loads==8,"All eight weapons have one cached pack each");
        var output=javax.microedition.lcdui.Image.createImage(320*scale,240*scale);var graphics=output.getGraphics();
        // Worst-case visible volley: fifteen projectiles, sixteen overlapping impacts, bounded trails.
        c.game.ballCount=15;c.game.blastCount=16;c.game.trailCount=0;
        for(int i=0;i<15;i++){var ball=new Battle.Ball();ball.weapon=new int[]{0,1,2,3,4,5,7,9}[i%8];ball.powered=i%2==0;
            ball.x=(130+i*9)*256;ball.y=(80+i%4*15)*256;ball.vx=900;ball.vy=-200;c.game.balls[i]=ball;c.game.stampTrail(ball);}
        for(int i=0;i<16;i++){var blast=new Battle.Blast();blast.weapon=new int[]{0,1,2,3,4,5,7,9}[i%8];blast.powered=i%2==0;blast.x=150+i%4*35;blast.y=120+i/4*12;c.game.blasts[i]=blast;}
        for(int i=0;i<50;i++){c.frame++;c.paint(graphics);}
        long start=System.nanoTime();
        for(int frame=0;frame<400;frame++){
            for(int i=0;i<c.game.ballCount;i++)c.game.balls[i].age=frame;
            for(int i=0;i<c.game.blastCount;i++)c.game.blasts[i].time=frame%25;
            c.frame++;c.paint(graphics);
        }
        long elapsed=System.nanoTime()-start;
        ok(c.combatWeapons.loads==loads,"Flight/impact paint performs no asset decoding");
        System.out.println("MIDP weapon draw benchmark: scale="+scale+", frames=400, total_ms="+elapsed/1000000+", avg_us="+elapsed/400000);
        wireRoundTrip(c);
        var visuals=new OnlineVisuals();var visualBattle=new Battle(c);
        var normal=new Battle.Blast();normal.weapon=4;normal.x=normal.y=100;
        visualBattle.blasts[0]=normal;visualBattle.blastCount=1;visuals.age(visualBattle,1000);
        visuals.age(visualBattle,1300);ok(normal.time>=8,"Existing explosion continues aging");
        var powered=new Battle.Blast();powered.weapon=4;powered.x=powered.y=100;powered.powered=true;
        visualBattle.blasts[1]=powered;visualBattle.blastCount=2;visuals.age(visualBattle,1300);
        ok(powered.time==0,"POW explosion at same location has an independent visual clock");
        c.releaseBattleArt();for(var pack:c.combatWeapons.weapons)ok(pack==null,"Leaving battle releases native packs");
        c.net.stop();c.stop();System.out.println("PASS UiWeaponCombatCheck "+checks+" checks at "+(320*scale)+"x"+(240*scale));System.exit(0);
    }
    static void wireRoundTrip(PhoneCanvas c)throws Exception{
        var store=new CloudStore.Memory();var host=Expansion780Check.session(store,"WeaponCombat",40);
        host.sulan7912=host.tribal797=host.clock793=host.vip=host.delta=host.tribal=host.playerUi=host.prefetch=host.expansion=host.campaign=host.flock=host.actors=host.itemFx=host.items=host.equipment=host.arsenal=host.modern=host.async=host.events=host.progress=host.bagLayout=host.social=host.matchmaking=host.roomFix=host.waitBot=host.emptyKit=true;host.itemVersion=2;host.live=false;
        try(var server=new GunnyServer(new java.net.InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
            server.clock.shutdownNow();var room=server.new Room(host,0,0,0);room.selectedBots[1]=0;room.refreshWaiting();room.start();
            var n=c.net;n.worker=Thread.currentThread();n.logged=true;n.capabilities(GunnyServer.capabilities(false,true));
            var lobby=new ByteArrayOutputStream();server.lobby(new DataOutputStream(lobby),room,host);
            n.inBattle=false;n.readLobby(new DataInputStream(new ByteArrayInputStream(lobby.toByteArray())));n.prepareBattle();
            for(int i=0;i<2;i++){
                room.battle.pow=i==1;room.battle.units[0].weapon=4;
                var ball=room.battle.make(45,60);ball.x=(180+i*35)*256;ball.y=160*256;room.battle.impact(ball);
            }
            for(int pass=0;pass<2;pass++){
                if(pass==1){room.battle.blasts[0].powered=true;room.battle.blasts[1].powered=false;}
                var bytes=new ByteArrayOutputStream();server.snapshot(new DataOutputStream(bytes),room,0,host);
                var input=new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));n.readSnapshot(input);
                ok(input.available()==0,"Normal/POW snapshot keeps existing packet layout");
                ok(c.game.blastCount==2&&c.game.blasts[0].powered==(pass==1)&&c.game.blasts[1].powered==(pass==0),"Online playback preserves and resets each impact mode independently");
                ok(c.game.blasts[0].radius==room.battle.blasts[0].radius,"Online radius remains unchanged");
            }
        }
    }
}
