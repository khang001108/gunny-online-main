import java.io.*;
import java.net.*;
import java.nio.file.*;
public final class Update788Check {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);System.out.println("PASS: "+message);}
 static void call(GunnyServer server,GunnyServer.Session user,String op,int... values)throws Exception{
  var data=new ByteArrayOutputStream();var out=new DataOutputStream(data);for(int n:values)out.writeByte(n);
  Dungeon780.handle(server,op,user,new DataInputStream(new ByteArrayInputStream(data.toByteArray())),new DataOutputStream(new ByteArrayOutputStream()));
 }
 public static void main(String[] args)throws Exception{
  int previous=0;for(int level=1;level<=70;level++){
   int xp=Progression.experienceForLevel(level);var unit=new Battle.Unit();unit.experience=xp;ItemRules.apply(unit,0);
   check(unit.maxHp==Progression.baseHp(xp)&&unit.hp==unit.maxHp&&unit.hp>previous,"Unarmored HP at level "+level);previous=unit.hp;
  }
  check(Progression.baseHp(0)==2000&&previous==15000,"Level 1 = 2000; level 70 = 15000 before equipment");
  var unit=new Battle.Unit();unit.experience=Progression.experienceForLevel(70);unit.weapon=0;unit.forgeDefense=195;ItemRules.apply(unit,5);
  check(unit.maxHp==15000+5+ItemRules.DEFENSE[0]+195,"Equipment and forge add HP separately from level");
  var store=new CloudStore.Memory();var user=Expansion780Check.session(store,"Dungeon788",70);user.flock=user.tribal=true;user.profile.dungeonClear=true;
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();var room=server.new Room(user,6,2,-1,true);room.kind=3;room.strict=true;user.room=room.id;server.rooms.put(room.id,room);
   for(int oldStage:new int[]{0,1,2,3,4,5,6}){
    room.phase=0;room.dungeonStage=oldStage;room.beanSaved=true;call(server,user,"dungeonsettings",1,1);
    check(room.dungeonStage==3&&room.difficulty==1&&!room.beanSaved&&room.map==8,"Waiting host can select Tribal from stage "+oldStage);
    call(server,user,"dungeonsettings",0,0);check(room.dungeonStage==1&&room.map==6,"Host can switch back to Ant Cave");
   }
   room.phase=1;try{call(server,user,"dungeonsettings",0,0);throw new AssertionError("Changed during battle");}catch(IllegalArgumentException expected){}
   room.phase=0;server.leave(user);var data=new ByteArrayOutputStream();var out=new DataOutputStream(data);out.writeByte(0);out.writeUTF("Dungeon 788");out.writeUTF("");
   Dungeon780.handle(server,"dungeoncreate",user,new DataInputStream(new ByteArrayInputStream(data.toByteArray())),new DataOutputStream(new ByteArrayOutputStream()));
   check(server.rooms.get(user.room).dungeonStage==1,"New dungeon room starts at stage 1");
  }
 }
}
