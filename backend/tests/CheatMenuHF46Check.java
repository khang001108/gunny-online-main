import java.io.*;
import java.net.*;
import java.nio.file.*;
public class CheatMenuHF46Check {
 static int checks;
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);checks++;}
 static byte[] call(GunnyServer server,GunnyServer.Session s,int action,boolean on,String key)throws Exception{
  ByteArrayOutputStream req=new ByteArrayOutputStream();DataOutputStream w=new DataOutputStream(req);w.writeByte(action);w.writeBoolean(on);if(action==6||action==7)w.writeUTF(key);
  ByteArrayOutputStream reply=new ByteArrayOutputStream();TestCheats.handle(server,s,new DataInputStream(new ByteArrayInputStream(req.toByteArray())),new DataOutputStream(reply));return reply.toByteArray();
 }
 static void denied(GunnyServer server,GunnyServer.Session s,int action)throws Exception{try{call(server,s,action,true,"deny");throw new AssertionError("unexpected grant "+action);}catch(IllegalArgumentException expected){checks++;}}
 public static void main(String[] args)throws Exception{
  CloudStore.Memory store=new CloudStore.Memory();store.register("test@local.test","password123","Cheat Test");GunnyServer.Session s=new GunnyServer.Session();s.id=store.login("test@local.test","password123").userId;s.profile=store.profile(s.id);s.lastSeen=System.currentTimeMillis();
  try(GunnyServer server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();for(int a:new int[]{7,8,9})denied(server,s,a);
   store.testCheat(s.id,6,true);call(server,s,7,true,"group1");ok(s.profile.stones==999999&&s.profile.luckyCharms==999999&&s.profile.protectionCharms==999999,"materials");ok(s.profile.coins==999999&&s.profile.shoppingMoney==999999,"money");for(int v:s.profile.vipCards)ok(v==999,"VIP grant");call(server,s,7,true,"group1");for(int v:s.profile.vipCards)ok(v==999,"retry no duplicate");
   call(server,s,8,true,"");ok(Progression.level(s.profile.experience)==70,"level");ok(s.profile.forgeLevels[0]==15,"forge");call(server,s,4,true,"");ok(s.profile.testGod,"god on");call(server,s,4,false,"");ok(!s.profile.testGod,"god off");denied(server,s,9);
   GunnyServer.Room r=server.new Room(s,0,0,-1);server.rooms.put(r.id,r);s.room=r.id;r.ground=server.new Ground(0);r.battle=new Battle(r.ground);r.battle.start(true);r.battle.explicitHumans=true;r.battle.units[0].human=true;r.avatars[0].owner=s;r.phase=1;r.battle.active=0;r.battle.units[0].rage=0;r.battle.units[1].rage=13;
   byte[] reply=call(server,s,9,true,"");ok(r.battle.units[0].rage==100&&r.battle.units[1].rage==13,"own rage only");DataInputStream tail=new DataInputStream(new ByteArrayInputStream(reply,reply.length-9,9));ok(tail.readInt()==r.id&&tail.readInt()==r.battle.turn&&tail.readUnsignedByte()==0,"response identity");ok(r.battle.prop(5)&&r.battle.pow&&r.battle.units[0].rage==0,"normal BPOW consumes rage");denied(server,s,9);r.battle.pow=false;r.battle.active=1;denied(server,s,9);r.battle.active=0;r.battle.charging=true;denied(server,s,9);r.battle.charging=false;r.phase=0;denied(server,s,9);store.testCheat(s.id,6,false);denied(server,s,7);denied(server,s,8);denied(server,s,9);
  }
  System.out.println("PASS "+checks+" cheat menu checks");
 }
}
