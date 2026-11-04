import java.io.*;
import java.net.*;
import java.nio.file.*;

public final class RoomFix693ServerCheck {
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS: "+s);}
 static void action(GunnyServer server,GunnyServer.Session s,String op,int value)throws Exception{server.social.handle(op,s,new DataInputStream(new ByteArrayInputStream(new byte[]{(byte)value})),new DataOutputStream(new ByteArrayOutputStream()));}
 public static void main(String[] args)throws Exception{CloudStore.Memory store=new CloudStore.Memory();try(GunnyServer server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
  var host=Matchmaking680Check.person(store,50);var guest=Matchmaking680Check.person(store,51);var room=Matchmaking680Check.room(server,host,null);action(server,host,"slot",2);ok((room.closedSlots&4)!=0,"Host closes empty allied slot");boolean denied=false;try{action(server,host,"allyai",1);}catch(IllegalArgumentException e){denied=true;}ok(denied,"Closed slot cannot receive AI");action(server,host,"slot",2);action(server,host,"allyai",1);ok(room.allyBot==1,"Reopened allied slot accepts AI");action(server,host,"slot",2);ok(room.allyBot<0,"X removes allied AI");action(server,host,"emote",1);ok(room.emotes[0]==1,"Native smile is associated with the sender's card");
  var team=server.new Room(host,1,2,-1,true);team.kind=2;team.strict=true;server.rooms.put(team.id,team);host.room=team.id;team.refreshWaiting();team.switchTeam(host,1);ok(!host.armed&&team.slot(host)==1,"Team switch clears ready status");action(server,host,"allyai",3);action(server,host,"allyai",4);ok(team.selectedBots[3]==3&&team.selectedBots[5]==4,"Blue team accepts two native AI allies");team.members[0]=guest;guest.room=team.id;team.refreshWaiting();guest.armed=true;action(server,guest,"allyai",1);action(server,guest,"allyai",2);guest.armed=true;ok(team.canStart(),"A single room with two complete opposing teams can start");team.autoStartAt=System.currentTimeMillis()-1;team.tick();ok(team.phase==3&&team.battle.unitCount==6&&team.battle.units[5].hp>0&&!team.battle.units[5].human,"3v3 starts in the same room with active AI actors");
 }System.out.println("All native room correction checks passed");}
}
