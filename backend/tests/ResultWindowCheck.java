import java.net.*;
import java.nio.file.*;
final class ResultWindowCheck {
 public static void main(String[] args)throws Exception {
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),new CloudStore.Memory())){
   server.clock.shutdownNow();
   var room=ShotBatchHF10Check.room(server,true,0,0);
   room.phase=2;room.battle.over=true;room.battle.winner=0;room.lastActive=100000;
   if(ResultResume.expired(room,159999)||!ResultResume.expired(room,160001))throw new AssertionError("60-second retention");
   room.phase=1;if(ResultResume.expired(room,999999))throw new AssertionError("active battle expiry");
   System.out.println("PASS result window: 60 seconds; active battle excluded");
  }
 }
}
