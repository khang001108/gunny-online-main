import java.lang.reflect.*;import java.util.*;
import org.microemu.app.*;import org.microemu.app.util.DeviceEntry;import org.microemu.device.*;import org.microemu.device.impl.*;import org.microemu.device.j2se.*;import org.microemu.MIDletBridge;
public final class ReturnRoomClientCheck {
 static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);System.out.println("PASS: "+msg);}
 public static void main(String[] args)throws Exception{Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});System.setProperty("java.awt.headless","true");int scale=args.length==0?1:Integer.parseInt(args[0]);var h=new Headless();var f=Headless.class.getDeclaredField("emulator");f.setAccessible(true);var common=(Common)f.get(h);common.initParams(new ArrayList<String>(Arrays.asList("--rms","memory","--appclassloader","system","GunnyMIDlet")),new DeviceEntry("Nokia",null,DeviceImpl.RESIZABLE_LOCATION,true,false),J2SEDevice.class);var display=(J2SEDeviceDisplay)DeviceFactory.getDevice().getDeviceDisplay();display.setDisplayRectangle(new Rectangle(0,0,320*scale,240*scale));display.setDisplayPaintable(new Rectangle(0,0,320*scale,240*scale));common.initMIDlet(true);Thread.sleep(200);var app=(GunnyMIDlet)MIDletBridge.getCurrentMIDlet();if(app.game==null)app.game=new PhoneCanvas(app);var c=app.game;javax.microedition.lcdui.Display.getDisplay(app).setCurrent(c);Thread.sleep(150);c.running=false;Thread.sleep(100);c.presentation.boot=2;c.presentation.music=false;c.tutorialDone=true;c.playerName="Gunner Test";c.mode=0;


  var net=c.net;net.alive=false;
  for(int i=0;i<6;i++){
   net.names[i]="Player "+i;net.present[i]=true;net.wornMasks[i]=3;net.controls[i]=i==0;
  }
  net.roomMap=0;
  for(int[] scenario:new int[][]{{0,2},{0,4},{1,2},{2,6},{3,6}}){
   net.roomKind=scenario[0];net.count=scenario[1];net.inBattle=false;net.phase=0;c.mode=16;
   net.showRoomState("start");
   check(c.mode==16&&!net.inBattle,"Pending search keeps waiting room, kind "+net.roomKind+", count "+net.count);
   net.phase=3;
   net.showRoomState("start");
   check(c.mode==14&&net.inBattle&&c.game.unitCount==net.count,"Matched start enters loading directly, kind "+net.roomKind+", count "+net.count);
   net.showRoomState("emote");
   check(c.mode==14,"Late room response preserves loading");
   c.mode=1;net.phase=1;net.showRoomState("emote");
   check(c.mode==1,"Late room response preserves combat");
   c.game.units[0].npc=0;c.game.units[0].hp=100;net.actorWing[0]=0;
   var dummy=javax.microedition.lcdui.Image.createImage(1,1);
   for(int a=0;a<5;a++)for(int l=0;l<2;l++)c.vip.sideWings[0][a][l]=dummy;
   c.vip.pruneBattleWings();int retained=0;
   for(int a=0;a<5;a++)for(int l=0;l<2;l++)if(c.vip.sideWings[0][a][l]!=null)retained++;
   check(retained==2,"Only the living player's current wing angle remains cached");net.actorWing[0]=-1;
   net.returnToWaitingRoom();
   check(c.pixels==null&&c.scratch==null&&c.background==null&&c.hardTerrain==null&&c.onlineGround==null,"Terrain released after returning to waiting room");
   for(int i=0;i<c.tiles.length;i++)if(c.tiles[i]!=null)throw new AssertionError("Retained terrain tile");
   for(int i=0;i<c.fadedUnits.length;i++)if(c.fadedUnits[i]!=null||c.slopeBody[i]!=null)throw new AssertionError("Retained actor cache");
  }
  net.inBattle=true;net.roomKind=0;net.count=2;net.results.leave();
  check(net.pending.equals("returnroom"),"Private 1v1 PVP returns to its party room");
  net.pending="";net.results.stage=1;net.results.claimed=true;net.results.activate();
  check(net.pending.equals("returnroom"),"Private 1v1 reward confirmation keeps its room");
  net.pending="";net.results.key(-7,0,false);
  check(net.pending.equals("returnroom"),"Private 1v1 back key keeps its room");net.results.reset();
  net.pending="";net.roomKind=1;net.results.leave();check(net.pending.equals("leave"),"Solo mode result leaves for the room list");
  net.pending="";net.results.stage=1;net.results.claimed=true;net.results.activate();check(net.pending.equals("leave"),"Solo confirmation after claiming reward goes to room list");
  net.pending="";net.results.key(-7,0,false);check(net.pending.equals("leave"),"Solo back key after reward goes to room list");net.results.reset();
  net.pending="";net.roomKind=0;net.count=4;net.results.leave();check(net.pending.equals("returnroom"),"2v2 keeps its waiting room");
  net.pending="";net.roomKind=2;net.count=6;net.results.leave();check(net.pending.equals("returnroom"),"3v3 keeps its party room");
  net.roomKind=3;
  for(int stage:new int[]{1,2,3,4,6})for(int winner:new int[]{0,1}){
   net.pending="";net.dungeonStage=stage;c.game.winner=winner;net.results.leave();check(net.pending.equals("returnroom"),"Dungeon stage "+stage+" winner "+winner+" returns to existing room");
  }
  net.pending="";net.phase=0;net.host=true;net.roomKind=3;net.present[2]=net.present[4]=false;
  c.roomSelection=60;c.loadout.move(2,true,true);check(c.roomSelection==62,"Right focuses second dungeon slot");
  c.loadout.move(1,true,true);check(c.roomSelection==60,"Left focuses first dungeon slot");
  c.loadout.move(8,true,true);check(c.roomSelection==64,"Down focuses third dungeon slot");
  c.dungeon.keyWaiting(-5,0,true);check(net.pending.equals("slot")&&net.slotDraft==4,"OK targets focused dungeon slot");
  net.pending="";c.dungeon.pointerWaiting(150,100);check(c.roomSelection==62&&net.slotDraft==2,"Full second card is touch selectable");
  net.pending="";net.roomId=1018;net.dungeonStage=6;net.dungeonDifficulty=2;c.dungeon.picker=false;
  c.roomSelection=64;c.loadout.move(2,true,true);check(c.roomSelection==DungeonRewardPreview.FOCUS,"Right from third slot focuses rewards");
  c.dungeon.keyWaiting(-5,0,true);check(c.dungeon.rewardsOpen&&c.dungeon.rewardCount()==8,"OK opens hard final reward grid");
  c.dungeon.keyWaiting(0,2,false);check(c.dungeon.rewardFocus==1,"Grid focus moves right");
  c.dungeon.keyWaiting(-6,0,false);check(c.dungeon.rewardInfo,"Left softkey opens item information");
  c.dungeon.keyWaiting(-7,0,false);check(c.dungeon.rewardsOpen&&!c.dungeon.rewardInfo,"Back closes only item info");
  c.dungeon.keyWaiting(-7,0,false);check(!c.dungeon.rewardsOpen&&c.roomSelection==DungeonRewardPreview.FOCUS,"Back restores reward card focus");
  c.dungeon.pointerWaiting(150,150);check(c.dungeon.rewardsOpen&&!c.dungeon.picker,"Reward card touch opens rewards not map picker");c.dungeon.closeRewards();
  net.pending="";c.game.dungeonStage=2;c.game.active=0;c.cameraActor=0;c.freeCamera=c.cameraReturning=c.cameraOnPlayer=false;
  c.game.units[1].x=405;c.game.units[1].y=150;c.game.botPhase=AntCaveRules.QUEEN_HIT;
  c.game.balls[0]=new Battle.Ball();c.game.balls[0].x=80*256;c.game.balls[0].y=100*256;c.game.ballCount=1;c.cameraX=160;
  for(int i=0;i<40;i++)c.followCamera();check(c.cameraX==0,"Queen dialogue must not override projectile camera");
  c.game.balls[0].x=400*256;for(int i=0;i<40;i++)c.followCamera();check(c.cameraX==160,"Camera tracks projectile across map");
  c.game.ballCount=0;c.game.balls[0]=null;
  net.ack=10;net.sequence=13;net.sentMax=12;net.queueCount=3;net.returnToWaitingRoom();check(net.sequence==10&&net.sentMax==10&&net.ack==10,"Discarded queued commands rebase to acknowledged sequence");net.acknowledgeCommands(12);check(net.sequence==12&&net.sentMax==12,"Server ACK can advance the next local command");check(!net.inBattle&&c.mode==16&&net.queueCount==0,"Returning clears battle state and opens waiting room");
  net.inBattle=false;c.offlineDungeon.active=false;net.results.leave();check(c.mode==10,"Offline PVP returns to setup");
  c.offlineDungeon.active=true;c.offlineDungeon.stage=2;net.results.leave();check(c.mode==19&&!c.offlineDungeon.active&&c.offlineDungeon.stage==1,"Offline dungeon returns to setup");
  net.stop();c.stop();System.exit(0);
 }
}
