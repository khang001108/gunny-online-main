import java.io.*;import java.net.*;import java.nio.file.*;
public class CompactLobbyCheck {
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);System.out.println("PASS: "+m);}
 static DataInputStream sync(TcpGameplayCheck.Player p,int version)throws Exception{return p.call("sync",o->{o.writeInt(0);o.writeByte(0);o.writeBoolean(true);o.writeInt(1);o.writeBoolean(false);o.writeInt(0);o.writeInt(-1);o.writeInt(-1);o.writeInt(-1);o.writeByte(0);if(version>=0)o.writeInt(version);});}
 public static void main(String[] args)throws Exception{
  var store=new CloudStore.Memory();store.register("a@cache.test","test-password-123","A Cache");store.register("b@cache.test","test-password-123","B Cache");
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();server.startTcp("127.0.0.1",0,8);
   try(var a=new TcpGameplayCheck.Player(server.tcpPort(),"a@cache.test");var b=new TcpGameplayCheck.Player(server.tcpPort(),"b@cache.test")){
    int id=a.call("create2",o->{o.writeByte(1);o.writeByte(0);o.writeByte(-1);o.writeByte(12);}).readInt();b.call("join",o->{o.writeInt(id);o.writeByte(1);o.writeUTF("");});b.call("ready",o->o.writeBoolean(true));a.call("start",null);var room=server.rooms.get(id);room.phase=1;
    var first=sync(a,0);int n=first.available();first.readInt();int version=first.readInt();ok(version>0&&first.readBoolean()&&first.readInt()==id,"first compact sync includes complete room");
    var second=sync(a,version);int compact=second.available();second.readInt();ok(second.readInt()==version&&!second.readBoolean(),"unchanged room omitted");ok(compact<n,"wire reply smaller: "+n+" -> "+compact+" bytes");
    var legacy=sync(b,-1);legacy.readInt();ok(legacy.readInt()==id,"legacy peer receives original wire format");
    room.departureVersion++;var changed=sync(a,version);changed.readInt();int next=changed.readInt();ok(next!=version&&changed.readBoolean()&&changed.readInt()==id,"departure change forces full room");
    var stale=sync(a,version);stale.readInt();ok(stale.readInt()==next&&stale.readBoolean(),"stale client cache resends full room");
    var reset=sync(a,0);reset.readInt();ok(reset.readInt()==next&&reset.readBoolean(),"reset client cache resends full room");
    int before=room.battle.units[0].x;
    Tcp768Check.Writer move=o->{o.writeInt(0);o.writeByte(2);o.writeBoolean(true);o.writeInt(room.battle.turn);o.writeBoolean(false);o.writeInt(room.departureVersion);o.writeInt(-1);o.writeInt(-1);o.writeInt(-1);o.writeByte(1);o.writeInt(1);o.writeByte(6);o.writeShort(3*16+2);o.writeInt(next);};
    var moved=a.call("sync",move);ok(moved.readInt()==1&&room.battle.units[0].x>before,"three movement ticks in one command update the authoritative position");int position=room.battle.units[0].x;a.call("sync",move);ok(room.battle.units[0].x==position,"retry of movement batch does not move twice");
    room.phase=3;var loading=sync(a,next);loading.readInt();loading.readInt();ok(loading.readBoolean(),"loading always receives full room");
   }
  }
 }
}
