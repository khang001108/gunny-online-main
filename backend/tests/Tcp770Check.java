import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.util.concurrent.*;

public final class Tcp770Check {
 static byte[] event(int room,long cursor)throws IOException {var b=new ByteArrayOutputStream();var out=new DataOutputStream(b);out.writeInt(GunnyServer.MAGIC);out.writeInt(room);out.writeLong(cursor);return b.toByteArray();}
 public static void main(String[] args)throws Exception {
  var store=new CloudStore.Memory();store.register("host@tcp.test","test-password-123","Host");store.register("guest@tcp.test","test-password-123","Guest");
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.startTcp("127.0.0.1",0,8);server.tcpOnly=true;
   var http=HttpClient.newHttpClient();for(String path:new String[]{"/api","/wait"})Tcp768Check.check(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+server.port()+path)).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.discarding()).statusCode()==410,"Production rejects legacy HTTP game traffic at "+path);
   try(var host=new TcpGameplayCheck.Player(server.tcpPort(),"host@tcp.test");var guest=new TcpGameplayCheck.Player(server.tcpPort(),"guest@tcp.test");var socket=new Socket("127.0.0.1",server.tcpPort())){
    socket.setSoTimeout(15000);var in=new DataInputStream(socket.getInputStream());var out=new DataOutputStream(socket.getOutputStream());
    int id=host.call("create2",o->{o.writeByte(1);o.writeByte(0);o.writeByte(-1);o.writeByte(12);}).readInt();
    guest.call("join",o->{o.writeInt(id);o.writeByte(1);o.writeUTF("");});
    long cursor=new DataInputStream(new ByteArrayInputStream(Tcp768Check.frame(in,out,2,host.token,event(id,Long.MIN_VALUE)))).readLong();
    // Stop HTTP entirely: game traffic must remain functional through the in-process handler.
    server.http.stop(0);
    var changed=CompletableFuture.supplyAsync(()->{try{return new DataInputStream(new ByteArrayInputStream(Tcp768Check.frame(in,out,2,host.token,event(id,cursor)))).readLong();}catch(Exception e){throw new CompletionException(e);}});
    guest.call("ready",o->o.writeBoolean(true));
    Tcp768Check.check(changed.get(2,TimeUnit.SECONDS)!=cursor,"TCP change notifications wake without an HTTP polling connection");
    host.call("start",null);host.sync(0,0,0);guest.sync(0,0,0);var room=server.rooms.get(id);
    TcpGameplayCheck.await(()->room.phase==1,"TCP game continues with the HTTP listener stopped");
    synchronized(room){room.battle.countdown=2;}
    host.sync(1,1,0);host.sync(2,6,90*16);host.sync(3,6,90*16);
    Tcp768Check.check(room.battle.countdown==2&&room.battle.units[0].power==20,"Server accepts the finite charge cycle beyond the remaining turn time");
    host.sync(4,6,20*16);
    Tcp768Check.check(room.battle.pendingNext&&!room.battle.charging&&room.battle.shotSerial==0,"Overcharge is authoritative and stops at the online turn barrier");
   }
  }
  System.out.println("All TCP 7.7.0 direct dispatch, notification and charge checks passed");
 }
}
