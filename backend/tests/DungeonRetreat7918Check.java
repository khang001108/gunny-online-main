import java.io.*;
import java.net.*;
import java.nio.file.*;

/** Real TCP retreat must retain the dungeon party without closing a teammate's battle. */
public final class DungeonRetreat7918Check {
 static void ok(boolean v,String m){if(!v)throw new AssertionError(m);System.out.println("PASS: "+m);}
 public static void main(String[] args)throws Exception{
  var store=new CloudStore.Memory();
  for(int i=0;i<2;i++){String email="retreat"+i+"@test.local";store.register(email,"test-password-123","Retreat "+i);store.profile(store.login(email,"test-password-123").userId).experience=Progression.experienceForLevel(70);}
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();server.startTcp("127.0.0.1",0,8);
   try(var a=new TribalTcpCheck.Player(server.tcpPort(),"retreat0@test.local");var b=new TribalTcpCheck.Player(server.tcpPort(),"retreat1@test.local")){
    for(int stage:new int[]{1,2,3,4,6})for(int people:new int[]{1,2})for(int quitter:new int[]{0,1}){
     if(people==1&&quitter==1)continue;
     int id=a.call(stage<3?"dungeoncreate":"tribalcreate",out->{out.writeByte(0);out.writeUTF("Ải viễn chinh");out.writeUTF("secret");}).readInt();
     if(people==2)b.call("join",out->{out.writeInt(id);out.writeByte(0);out.writeUTF("secret");});
     var r=server.rooms.get(id);r.dungeonStage=stage;r.refreshWaiting();
     if(people==2)b.call("ready",out->out.writeBoolean(true));a.call("start",null);
     boolean playing=people==1||quitter==1;
     if(playing){for(var member:r.members)if(member!=null)member.ready=true;r.tick();r.loadingReadyAt=System.currentTimeMillis()-2100;r.tick();ok(r.phase==1,"Test starts an active dungeon battle");}
     var quitting=quitter==0?a:b;var other=quitter==0?b:a;
     var qs=server.sessions.get(quitting.token);int money=qs.profile.shoppingMoney,stones=qs.profile.stones;
     var reply=quitting.call("dungeonretreat",null);int homeId=reply.readInt();var home=server.rooms.get(homeId);
     ok(reply.readByte()==0&&qs.room==homeId&&home.kind==3&&home.dungeonStage==stage,"Retreat restores dungeon waiting stage "+stage+", party "+people+", seat "+quitter);
     ok(home.title.equals("Ải viễn chinh")&&home.secret.equals("secret")&&home.difficulty==0&&home.map==r.map,"Dungeon title, password, difficulty and map survive");
     ok(quitting.call("dungeonretreat",null).readInt()==homeId&&qs.profile.shoppingMoney==money&&qs.profile.stones==stones,"Retry is harmless and quitting grants no items");
     if(people==2){
      var os=server.sessions.get(other.token);ok(os.room==id&&r.phase==(playing?1:3)&&r.battle!=null&&home.hasReturningParty()&&!home.canStart(),"Teammate keeps loading/battle; waiting party cannot restart early");
      os.ready=true;r.tick();r.loadingReadyAt=System.currentTimeMillis()-2100;r.tick();ok(r.phase==1,"Remaining teammate can finish loading without quitter");
      if(stage==6){
       r.battle.over=true;r.battle.winner=1;r.phase=2;r.settled=true;
       var returned=other.call("returnroom",null);ok(returned.readInt()==homeId&&returned.readByte()==0,"Teammate can return from result to the same waiting party");
      }else ok(other.call("dungeonretreat",null).readInt()==homeId,"Second quitter rejoins own party");
      ok(home.host==server.sessions.get(a.token)&&!home.hasReturningParty(),"Party leader and seats stay intact when everyone returns");
     }else ok(!server.rooms.containsKey(id),"Abandoned solo battle is released");
     for(var member:home.members)if(member!=null)member.armed=true;
     home.start();ok(home.phase==3&&home.dungeonStage==stage,"Same dungeon can retry after returning");
     a.call("leave",null);if(people==2)b.call("leave",null);server.rooms.clear();
    }
    int pvpId=a.call("quick",out->out.writeByte(1)).readInt();boolean refused=false;
    try{a.call("dungeonretreat",null);}catch(AssertionError expected){refused=expected.getMessage().contains("Không phải phòng");}
    ok(refused&&server.sessions.get(a.token).room==pvpId,"Dungeon retreat cannot alter a PvP room");
   }
  }
 }
}
