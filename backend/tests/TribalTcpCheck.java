import java.io.*;import java.net.*;import java.nio.file.*;import java.util.*;
/** Actual framed TCP room lifecycle with three independent account connections. */
public class TribalTcpCheck {
 static int checks;static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
 static final int FLAGS=1073741824|536870912|67108864|33554432|16777216|8388608|4194304|0x3fffff;
 static class Player implements AutoCloseable {
  int flags=FLAGS;Socket socket;DataInputStream in;DataOutputStream out;String token="";
  Player(int port,String email)throws Exception{this(port,email,FLAGS);}
  Player(int port,String email,int flags)throws Exception{this.flags=flags;socket=new Socket("127.0.0.1",port);socket.setSoTimeout(5000);in=new DataInputStream(socket.getInputStream());out=new DataOutputStream(socket.getOutputStream());token=call("login",o->{o.writeUTF(email);o.writeUTF("test-password-123");}).readUTF();}
  DataInputStream call(String op,Tcp768Check.Writer writer)throws Exception {byte[] body=Tcp768Check.body(op,writer);out.writeInt(TcpGameServer.MAGIC);out.writeByte(1);out.writeInt(flags);out.writeUTF(token);out.write(new byte[]{0,1,2});out.writeInt(body.length);out.write(body);out.flush();check(in.readInt()==200,"TCP transport");int size=in.readInt();return Tcp768Check.ok(in.readNBytes(size));}
  public void close()throws IOException{socket.close();}
 }
 public static void main(String[] args)throws Exception {
  var store=new CloudStore.Memory();String[] emails={"tribal1@tcp.test","tribal2@tcp.test","tribal3@tcp.test"};for(int i=0;i<3;i++){store.register(emails[i],"test-password-123","Raid "+i);store.profile(store.login(emails[i],"test-password-123").userId).experience=Progression.experienceForLevel(30);}
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){server.clock.shutdownNow();server.startTcp("127.0.0.1",0,8);Player[] p=new Player[3];try{
   for(int i=0;i<3;i++)p[i]=new Player(server.tcpPort(),emails[i]);
   int id=p[0].call("tribalcreate",o->{o.writeByte(2);o.writeUTF("Bộ Lạc Tà Thần");o.writeUTF("");}).readInt();
   for(int i=1;i<3;i++)p[i].call("join",o->{o.writeInt(id);o.writeByte(0);o.writeUTF("");});var r=server.rooms.get(id);check(r.cardPlayers()==3,"Three real players share one party");
   for(int stage=1;stage<=3;stage++){
    for(int i=1;i<3;i++)p[i].call("ready",o->o.writeBoolean(true));p[0].call("start",null);check(r.map==(stage==3?11:7+stage),"Separate stage map through TCP start");for(var member:r.members)if(member!=null)member.ready=true;r.tick();r.loadingReadyAt=System.currentTimeMillis()-2001;r.tick();check(r.phase==1,"Loading finishes");
    if(stage==1){r.battle.tribalFx=0;r.battle.active=0;int turn=r.battle.turn;p[0].call("sync",o->{o.writeInt(0);o.writeByte(0);o.writeBoolean(true);o.writeInt(turn);o.writeBoolean(false);o.writeInt(0);o.writeInt(-1);o.writeInt(-1);o.writeInt(-1);o.writeByte(2);o.writeInt(1);o.writeByte(1);o.writeShort(0);o.writeInt(2);o.writeByte(2);o.writeShort(-(1+45*128+80));});for(int tick=0;tick<600&&!r.battle.pendingNext;tick++)r.tick();check(r.battle.shotSerial>0&&r.battle.pendingNext,"TCP firing uses ordinary projectiles and finishes the turn");int hp=r.battle.units[0].hp;p[0].close();p[0]=new Player(server.tcpPort(),emails[0]);check(p[0].call("tribalresume",null).readBoolean(),"Re-login resumes active campaign");check(r.battle.units[0].hp==hp&&r.host==server.sessions.get(p[0].token),"Reconnection keeps HP and authority");}
    // Controlled knockouts exercise the real transition/settlement/card routes without a long manual battle.
    while(!r.battle.over){for(var u:r.battle.units)if(u.team==1)u.hp=0;r.battle.tribalFx=0;r.battle.check();if(r.battle.dungeonStage==3&&r.battle.tribalGoal>0)TribalRules.round(r.battle);if(r.battle.tribalState==TribalRules.TRANSFORM){while(r.battle.tribalFx>0)r.battle.tick();}}for(var m:r.members)if(m!=null)m.resultAck=r.battle.turn;r.tick();long deadline=System.currentTimeMillis()+5000;while(!r.settled&&System.currentTimeMillis()<deadline)Thread.sleep(10);check(r.settled,"Stage persisted before cards open");for(var player:p)player.call("cardsready",null);
    if(stage<3){
     try{p[0].call("claim780",o->{o.writeByte(0);o.writeByte(9);});throw new AssertionError("Tenth card accepted on nine-card crossing");}catch(AssertionError expected){check(expected.getMessage().startsWith("1:"),"Server rejects invalid crossing reward intent");}
     try{p[0].call("claim780",o->{o.writeByte(1);o.writeByte(8);});throw new AssertionError("Second pick accepted on crossing");}catch(AssertionError expected){check(expected.getMessage().startsWith("1:"),"Server rejects invalid crossing reward intent");}
    }
    int[] deck=(int[])store.tribalStore.matches.get(r.match).get("deck");List<Integer> order=new ArrayList<>();for(int i=0;i<(stage==3?18:9);i++)order.add(i);if(stage==3)order.sort(Comparator.comparingInt((Integer card)->deck[card]==2?10:deck[card]).reversed());
    for(int actor=0;actor<3;actor++)for(int pick=0;pick<(stage==3?2:1);pick++){int card=order.get(actor*(stage==3?2:1)+pick),which=pick;var reply=p[actor].call("claim780",o->{o.writeByte(which);o.writeByte(card);});check(reply.readUnsignedByte()==card,"Server confirms shared card");int stones=r.members[actor*2].profile.stones+Arrays.stream(r.members[actor*2].profile.craftStones).sum();p[actor].call("claim780",o->{o.writeByte(which);o.writeByte(card);});check(r.members[actor*2].profile.stones+Arrays.stream(r.members[actor*2].profile.craftStones).sum()==stones,"Repeated TCP claim never duplicates stones");}
    var revealed=p[0].call("deck",null);check(revealed.readBoolean(),"Deck opens after the actual allowed picks");check(revealed.readUnsignedByte()==(stage==3?18:9),"Crossing has nine cards; boss has eighteen");
    if(stage<3){for(var player:p)player.call("dungeoncontinue",null);check(r.phase==0&&r.dungeonStage==(stage==2?6:4),"All clients return to next staging room once");}
   }
   check(Arrays.stream(r.members).filter(Objects::nonNull).mapToInt(m->m.profile.wings).sum()==1,"Only one wing granted across the final deck");for(var player:p)player.call("leave",null);check(Arrays.stream(r.members).allMatch(Objects::isNull),"All players leave for lobby after final rewards");
  }finally{for(var player:p)if(player!=null)player.close();}}
  System.out.println("PASS: "+checks+" TCP campaign lifecycle checks (3 players, 3 stages, reconnect, real firing, reward retries)");
 }
}
