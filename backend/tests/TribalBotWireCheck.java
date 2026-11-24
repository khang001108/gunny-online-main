import java.io.*;
import java.net.*;
import java.nio.file.*;
/** Real TCP invite/lobby/start regression, including AI turns in all three Tà Thần maps. */
public final class TribalBotWireCheck {
 static int checks;
 static void ok(boolean test,String label){if(!test)throw new AssertionError(label);checks++;}
 static int lobby(DataInputStream in,int a,int b)throws Exception{
  int id=in.readInt();in.readUnsignedByte();in.readBoolean();in.readByte();int n=in.readUnsignedByte();in.readBoolean();ok(n==6,"six room seats");
  for(int i=0;i<n;i++){
   String name=in.readUTF();in.readUnsignedByte();in.readUnsignedByte();in.readBoolean();for(int k=0;k<5;k++)in.readUnsignedByte();in.readUnsignedByte();boolean own=in.readBoolean(),present=in.readBoolean(),bot=in.readBoolean();in.readUnsignedByte();int xp=in.readInt();in.readBoolean();
   if(i==2||i==4){int expected=i==2?a:b;ok(present==(expected>=0),"presence at "+i);ok(bot==(expected>=0),"AI flag at "+i);ok(!own,"AI not controlled by host");ok(name.equals(expected>=0?GunnyServer.BOT_NAMES[expected]:"Đang chờ"),"correct name on wire: "+name);if(expected>=0)ok(xp==Progression.botExperience(expected),"AI level on wire");}
  }return id;
 }
 public static void main(String[] args)throws Exception{
  var store=new CloudStore.Memory();String email="botwire@tcp.test";store.register(email,"test-password-123","Kenji");store.profile(store.login(email,"test-password-123").userId).experience=Progression.experienceForLevel(70);
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();server.startTcp("127.0.0.1",0,8);
   try(var p=new TribalTcpCheck.Player(server.tcpPort(),email)){
    for(int stage:new int[]{3,4,6}){
     int id=lobby(p.call("tribalcreate",o->{o.writeByte(0);o.writeUTF("AI test");o.writeUTF("");}),-1,-1);
     var r=server.rooms.get(id);r.dungeonStage=stage;r.refreshWaiting();
     lobby(p.call("allyai",o->{o.writeByte(1);o.writeByte(2);}),1,-1);
     lobby(p.call("allyai",o->{o.writeByte(7);o.writeByte(4);}),1,7);
     lobby(p.call("start",null),1,7);
     var battle=r.battle;ok(battle!=null,"battle created");
     for(int seat:new int[]{2,4}){var u=battle.units[seat];ok(u.hp>0&&u.maxHp==u.hp&&!u.dead&&!u.human&&u.npc==0&&u.team==0,"AI alive at seat "+seat+" stage "+stage);ok(u.x==TribalRules.allyX(seat,stage==4),"AI spawn position");}
     ok(battle.units[2].x!=battle.units[4].x,"distinct AI positions");
     for(var m:r.members)if(m!=null)m.ready=true;r.tick();r.loadingReadyAt=System.currentTimeMillis()-2001;r.tick();ok(r.phase==1,"loading complete");
     battle.tribalFx=0;battle.tribalState=TribalRules.IDLE;
     for(int seat:new int[]{2,4}){battle.pendingNext=false;battle.ballCount=battle.bursts=battle.settle=0;battle.beginTurn(seat);int turn=battle.turn,shot=battle.shotSerial;for(int i=0;i<600&&!battle.pendingNext&&battle.shotSerial==shot;i++)r.tick();ok(battle.shotSerial>shot||battle.pendingNext||battle.turn!=turn,"AI executes or passes turn at seat "+seat+" stage "+stage);}
     p.call("leave",null);
    }
   }
  }
  System.out.println("PASS "+checks+" TCP AI invite/name/loading/spawn/turn checks across all three stages");
 }
}
