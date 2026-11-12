import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;

/** Two independent persistent connections exercise the current Nokia protocol. */
public final class TcpGameplayCheck {
 static final class Player implements AutoCloseable {
  final Socket socket; final DataInputStream in; final DataOutputStream out; String token="";
  Player(int port,String email)throws Exception {
   socket=new Socket("127.0.0.1",port);socket.setSoTimeout(5000);
   in=new DataInputStream(socket.getInputStream());out=new DataOutputStream(socket.getOutputStream());
   token=call("login",o->{o.writeUTF(email);o.writeUTF("test-password-123");}).readUTF();
  }
  DataInputStream call(String op,Tcp768Check.Writer writer)throws Exception {
   return Tcp768Check.ok(Tcp768Check.frame(in,out,1,token,Tcp768Check.body(op,writer)));
  }
  void sync(int seq,int kind,int arg)throws Exception {
   call("sync",o->{
    o.writeInt(0);o.writeByte(0);o.writeBoolean(true);
    o.writeInt(1);o.writeBoolean(false);o.writeInt(0);
    o.writeInt(-1);o.writeInt(-1);o.writeInt(-1);
    o.writeByte(kind==0?0:1);
    if(kind!=0){o.writeInt(seq);o.writeByte(kind);o.writeShort(arg);}
   });
  }
  public void close()throws IOException {socket.close();}
 }
 static void await(BooleanSupplier condition,String label)throws Exception {
  long deadline=System.nanoTime()+3_000_000_000L;
  while(!condition.getAsBoolean()&&System.nanoTime()<deadline)Thread.sleep(10);
  Tcp768Check.check(condition.getAsBoolean(),label);
 }
 public static void main(String[] args)throws Exception {
  var store=new CloudStore.Memory();
  store.register("host@tcp.test","test-password-123","TCP Host");
  store.register("guest@tcp.test","test-password-123","TCP Guest");
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)) {
   server.startTcp("127.0.0.1",0,8);
   try(var host=new Player(server.tcpPort(),"host@tcp.test");var guest=new Player(server.tcpPort(),"guest@tcp.test")) {
    int id=host.call("create2",o->{o.writeByte(1);o.writeByte(0);o.writeByte(-1);o.writeByte(12);}).readInt();
    guest.call("join",o->{o.writeInt(id);o.writeByte(1);o.writeUTF("");});
    var room=server.rooms.get(id);
    Tcp768Check.check(room.members[0]!=null&&room.members[1]!=null,"Two TCP players join opposite teams in the same room");
    guest.call("ready",o->o.writeBoolean(true));host.call("start",null);
    host.sync(0,0,0);guest.sync(0,0,0);
    await(()->room.phase==1,"Both TCP players finish loading before the battle starts");
    host.sync(1,3,2);int energy=room.battle.units[0].energy;
    host.sync(1,3,2);
    Tcp768Check.check(room.battle.boost==50&&room.battle.units[0].energy==energy,"A retried TCP command does not spend energy twice");
    guest.sync(1,3,2);
    Tcp768Check.check(room.battle.units[0].energy==energy,"The opponent cannot control the active TCP player's turn");
    host.sync(2,1,0);host.sync(3,2,-(1+47*128+36));
    await(()->room.battle.shotSerial>0,"Releasing force over TCP fires an authoritative projectile");
    host.sync(0,0,0);guest.sync(0,0,0);
    Tcp768Check.check(server.tcp.active.get()==2,"Both players reuse their original sockets through login, room and battle");
    guest.call("leave",null);host.call("logout",null);
    Tcp768Check.check(!server.sessions.containsKey(host.token),"TCP logout revokes the game session");
   }
   await(()->server.tcp.active.get()==0,"Both TCP connections are released after the players disconnect");
  }
  System.out.println("All two-player TCP gameplay checks passed");
 }
}
