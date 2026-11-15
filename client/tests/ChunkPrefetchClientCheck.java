import java.lang.reflect.*;import java.util.*;
import org.microemu.app.*;import org.microemu.app.util.DeviceEntry;import org.microemu.device.*;import org.microemu.device.impl.*;import org.microemu.device.j2se.*;import org.microemu.MIDletBridge;
public final class ChunkPrefetchClientCheck {
 static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);System.out.println("PASS: "+msg);}
 public static void main(String[] args)throws Exception{Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});System.setProperty("java.awt.headless","true");int scale=args.length==0?1:Integer.parseInt(args[0]);var h=new Headless();var f=Headless.class.getDeclaredField("emulator");f.setAccessible(true);var common=(Common)f.get(h);common.initParams(new ArrayList<String>(Arrays.asList("--rms","memory","--appclassloader","system","GunnyMIDlet")),new DeviceEntry("Nokia",null,DeviceImpl.RESIZABLE_LOCATION,true,false),J2SEDevice.class);var display=(J2SEDeviceDisplay)DeviceFactory.getDevice().getDeviceDisplay();display.setDisplayRectangle(new Rectangle(0,0,320*scale,240*scale));display.setDisplayPaintable(new Rectangle(0,0,320*scale,240*scale));common.initMIDlet(true);Thread.sleep(200);var app=(GunnyMIDlet)MIDletBridge.getCurrentMIDlet();if(app.game==null)app.game=new PhoneCanvas(app);var c=app.game;javax.microedition.lcdui.Display.getDisplay(app).setCurrent(c);Thread.sleep(150);c.running=false;Thread.sleep(100);c.presentation.boot=2;c.presentation.music=false;c.tutorialDone=true;c.playerName="Gunner Test";c.mode=0;



  String mode=args.length>1?args[1]:"shot";
  var net=c.net;net.alive=false;net.logged=true;
  try(var input=new java.io.DataInputStream(new java.io.FileInputStream("client/build/"+mode+"-frames.bin"))){
   net.capabilities(input.readUTF());check(net.shotFrames&&!net.liveCap&&(net.tcpFlags()&4194304)!=0,"Client negotiates ordered authoritative shot frames over TCP");
   byte[] lobby=new byte[input.readInt()];input.readFully(lobby);net.readLobby(new java.io.DataInputStream(new java.io.ByteArrayInputStream(lobby)));
   int turn=input.readInt(),count=input.readInt(),visible=0,explosions=0;
   byte[][] packets=new byte[count][];
   for(int chunk=0;chunk<count;chunk++){packets[chunk]=new byte[input.readInt()];input.readFully(packets[chunk]);}
   check(count>=4,"Fixture contains enough short movement/shot segments for prefetch");
   net.chunks.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(packets[0])),true,turn);
   int initialRemaining=net.chunks.remaining;
   check(!net.chunks.ready(net.chunks.waitingSince),"P2 holds playback until its half-second buffer is ready");
   check(net.chunks.ready(net.chunks.waitingSince+500),"A short nonterminal stream has a bounded startup wait");
   net.chunks.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(packets[1])),true,turn);
   net.chunks.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(packets[2])),true,turn);
   check(net.chunks.remaining==initialRemaining&&net.chunks.queued.size()==2&&net.chunks.completed==-1,"Next packets are retained while current packet has not played");
   net.chunks.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(packets[1])),true,turn);
   check(net.chunks.received==2&&net.chunks.queued.size()==2,"Duplicate packet is ignored without losing queued frames");
   net.chunks.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(packets[3])),true,turn);
   check(net.chunks.cached()<-1,"Four buffered packets close the receive window");
   check(net.chunks.ready(net.chunks.waitingSince),"Fifteen buffered frames release playback");
   check(net.chunks.requestTurn(turn-1)==turn,"Requests identify the received turn while displayed frames are delayed");
   check((net.tcpFlags()&8388608)!=0,"TCP requests negotiate the receive-ahead protocol");
   int displayedTurn=c.game.turn;c.game.turn=turn-1;
   var syncBody=new java.io.DataInputStream(new java.io.ByteArrayInputStream(net.body("sync")));
   syncBody.readInt();syncBody.readUTF();syncBody.readInt();syncBody.readByte();syncBody.readBoolean();
   check(syncBody.readInt()==turn,"Actual sync packet uses the received turn before first delayed frame");c.game.turn=displayedTurn;
   int next=4,previousCompleted=-1,ticks=0,nextArrival=12,minX=999999,maxX=-999999;
   while(next<count||net.chunks.frames!=null){
    if(next<count&&net.chunks.cached()>=-1&&ticks>=nextArrival){net.chunks.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(packets[next++])),true,turn);nextArrival=ticks+6;}
    if(net.chunks.frames!=null){
     int beforeId=net.chunks.id,beforeRemaining=net.chunks.remaining;
     net.animate();ticks++;
     if(net.chunks.id==beforeId&&net.chunks.remaining==beforeRemaining)throw new AssertionError("Playback stalled during simulated network jitter");
     minX=Math.min(minX,c.game.units[0].x);maxX=Math.max(maxX,c.game.units[0].x);
     if(net.chunks.completed<previousCompleted)throw new AssertionError("Playback completion moved backwards");previousCompleted=net.chunks.completed;
     if(c.game.ballCount>0)visible++;if(c.game.blastCount>0)explosions++;
     if(net.chunks.frames!=null&&net.chunks.id!=beforeId)check(net.chunks.playing,"Adjacent queued segments play without rebuffering");
    }
    if(ticks>10000)throw new AssertionError("Playback did not finish");
   }
   check(net.chunks.completed==count-1&&net.resolutionSeen,"Turn is acknowledged only after terminal playback");
   // A missing segment fails without acknowledging or discarding valid queued data.
   net.chunks.reset();
   boolean gapRejected=false;
   try{net.chunks.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(packets[1])),true,turn);}catch(java.io.IOException expected){gapRejected=true;}
   check(gapRejected&&net.chunks.received==-1,"Out-of-order segment is never acknowledged");
   // Local player reconciles nonterminal snapshots without entering the display buffer.
   net.chunks.reset();net.predicting=true;net.predictedTurn=turn;net.myActor=c.game.active;net.controls[c.game.active]=true;
   c.game.pendingNext=c.game.over=false;
   for(int i=0;i<count;i++)net.chunks.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(packets[i])),true,turn);
   check(net.chunks.frames==null&&net.chunks.queued.isEmpty()&&net.chunks.ownPending,"P1 bypasses remote buffering while local shot is running");
   c.game.pendingNext=true;net.chunks.finish();
   check(!net.chunks.ownPending&&net.chunks.completed==count-1,"P1 reconciles only when local shot finishes");
   if(mode.equals("walk"))check(maxX>minX,"Remote walking remains continuous with simulated packet delay");
   else{check(visible>0,"Projectile is visible during remote playback");check(explosions>0,"Remote playback includes impact explosion");}
   check(net.revision==input.readInt(),"Playback applies exactly the server terrain impacts");
   check(net.resolutionSeen,"Next turn waits until authoritative playback finishes");
  }
  net.stop();c.stop();System.exit(0);
 }
}
