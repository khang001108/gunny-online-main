import java.io.*;
import java.net.*;
import java.nio.file.*;
/** Exercise real TCP sequence-gap response and a safe retry in the current turn. */
public final class SequenceHF5Check {
 static int checks;
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);checks++;}
 static DataInputStream raw(TribalTcpCheck.Player p,int turn,int... cmds)throws Exception{
  byte[] body=Tcp768Check.body("sync",o->{o.writeInt(0);o.writeByte(0);o.writeBoolean(true);o.writeInt(turn);o.writeBoolean(false);o.writeInt(0);o.writeInt(-1);o.writeInt(-1);o.writeInt(-1);o.writeByte(cmds.length/3);for(int i=0;i<cmds.length;i+=3){o.writeInt(cmds[i]);o.writeByte(cmds[i+1]);o.writeShort(cmds[i+2]);}});
  p.out.writeInt(TcpGameServer.MAGIC);p.out.writeByte(1);p.out.writeInt(p.flags);p.out.writeUTF(p.token);p.out.write(new byte[]{0,1,2});p.out.writeInt(body.length);p.out.write(body);p.out.flush();ok(p.in.readInt()==200,"TCP status");return new DataInputStream(new ByteArrayInputStream(p.in.readNBytes(p.in.readInt())));
 }
 static void success(DataInputStream r,int ack)throws Exception{ok(r.readUnsignedByte()==0&&r.readInt()==ack,"ACK "+ack);}
 static void gap(DataInputStream r,int ack)throws Exception{ok(r.readUnsignedByte()==4,"explicit gap reply");r.readUTF();ok(r.readInt()==ack,"authoritative ACK in gap reply");}
 public static void main(String[] args)throws Exception{
  var store=new CloudStore.Memory();String email="seq@tcp.test";store.register(email,"test-password-123","Seq Test");store.profile(store.login(email,"test-password-123").userId).experience=Progression.experienceForLevel(30);
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();server.startTcp("127.0.0.1",0,8);
   try(var p=new TribalTcpCheck.Player(server.tcpPort(),email)){
    int id=p.call("tribalcreate",o->{o.writeByte(0);o.writeUTF("Sequence test");o.writeUTF("");}).readInt();var r=server.rooms.get(id);p.call("start",null);
    for(var m:r.members)if(m!=null)m.ready=true;r.tick();r.loadingReadyAt=System.currentTimeMillis()-2001;r.tick();
    var b=r.battle;b.tribalFx=0;b.tribalState=TribalRules.IDLE;b.beginTurn(0);int turn=b.turn;
    // First command received; second skipped. Reject later commands without applying them.
    success(raw(p,turn,1,7,0),1);gap(raw(p,turn,3,1,0),1);ok(!b.charging,"out-of-order charge not applied");
    success(raw(p,turn,2,1,0),2);ok(b.charging,"rebased charge accepted");
    success(raw(p,turn,2,1,0),2);ok(b.charging,"duplicate does not change state");
    // Gap on stale turn must also return the authoritative ACK.
    gap(raw(p,turn-1,4,4,0),2);ok(b.charging&&!b.pendingNext,"stale gap does not pass turn");
    success(raw(p,turn-1,3,4,0),3);ok(b.charging&&!b.pendingNext,"contiguous stale command acknowledged but ignored");
    // Reuse session in a subsequent room without skipped commands.
    var left=p.call("leave",null);left.readUTF();ok(left.readInt()==3,"leave returns session ACK");
    int second=p.call("tribalcreate",o->{o.writeByte(0);o.writeUTF("Sequence test 2");o.writeUTF("");}).readInt();ok(second!=id,"new room");success(raw(p,-1,4,7,0),4);
   }
  }
  System.out.println("PASS "+checks+" TCP sequence-gap, retry, stale-turn and room transition checks");
 }
}
