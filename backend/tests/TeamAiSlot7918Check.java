import java.io.*;
import java.net.*;
import java.nio.file.*;
public final class TeamAiSlot7918Check {
 static int checks;
 static void ok(boolean b,String label){if(!b)throw new AssertionError(label);checks++;System.out.println("PASS: "+label);}
 static void action(GunnyServer server,GunnyServer.Session s,String op,int... values)throws Exception{byte[] bytes=new byte[values.length];for(int i=0;i<values.length;i++)bytes[i]=(byte)values[i];server.social.handle(op,s,new DataInputStream(new ByteArrayInputStream(bytes)),new DataOutputStream(new ByteArrayOutputStream()));}
 static boolean denied(GunnyServer server,GunnyServer.Session s,String op,int... values)throws Exception{try{action(server,s,op,values);return false;}catch(IllegalArgumentException ex){return true;}}
 public static void main(String[] args)throws Exception{
  var store=new CloudStore.Memory();try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();var host=Matchmaking680Check.person(store,900);var guest=Matchmaking680Check.person(store,901);var r=server.new Room(host,2,2,-1,true);r.kind=2;r.strict=true;r.mapLimit=6;r.seconds=12;host.room=guest.room=r.id;r.members[1]=guest;server.rooms.put(r.id,r);r.refreshWaiting();
   action(server,guest,"allyai",5,3);ok(r.selectedBots[3]==5,"Invite Thu Ha");
   ok(denied(server,guest,"allyai",5,5)&&r.selectedBots[5]==-1,"Duplicate on same team denied");
   ok(denied(server,host,"allyai",5,2)&&r.selectedBots[2]==-1,"Duplicate across teams denied");
   ok(denied(server,guest,"slot",3)&&r.selectedBots[3]==5,"Guest cannot close AI seat");
   action(server,host,"slot",3);ok(r.selectedBots[3]==-1&&(r.closedSlots&8)!=0,"Host removes AI and closes seat");
   ok(denied(server,host,"allyai",5,3),"Closed slot rejects invite");
   action(server,host,"slot",3);ok((r.closedSlots&8)==0,"Host reopens seat");
   action(server,host,"allyai",5,3);ok(r.selectedBots[3]==5,"Removed AI can be invited again");
   action(server,host,"allyai",6,5);ok(r.selectedBots[5]==6,"Different AI accepted");
   ok(denied(server,host,"slot",0),"Host cannot close own slot");
   action(server,host,"slot",2);ok((r.closedSlots&4)!=0,"Empty slot closes");
   action(server,host,"slot",2);ok((r.closedSlots&4)==0,"Empty closed slot reopens");
  }System.out.println("PASS "+checks+" team AI seat checks");
 }
}
