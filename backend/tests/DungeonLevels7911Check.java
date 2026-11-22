import java.io.*;import java.net.*;import java.nio.file.*;
public class DungeonLevels7911Check {
 static int checks;static void ok(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
 static void denied(TribalTcpCheck.Player p,String op,Tcp768Check.Writer w,String expected)throws Exception{boolean failed=false;try{p.call(op,w);}catch(AssertionError e){if(!e.getMessage().contains(expected))throw e;failed=true;}ok(failed,"Expected rejection: "+op+" "+expected);}
 public static void main(String[] args)throws Exception{
 var store=new CloudStore.Memory();for(int lv:new int[]{14,15,29,30})Expansion780Check.session(store,"lv"+lv,lv);
 try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){server.clock.shutdownNow();server.startTcp("127.0.0.1",0,8);
 try(var a=new TribalTcpCheck.Player(server.tcpPort(),"lv14@780.test");var b=new TribalTcpCheck.Player(server.tcpPort(),"lv15@780.test");var c=new TribalTcpCheck.Player(server.tcpPort(),"lv29@780.test");var d=new TribalTcpCheck.Player(server.tcpPort(),"lv30@780.test")){
 Tcp768Check.Writer create=o->{o.writeByte(0);o.writeUTF("Level test");o.writeUTF("");};
 denied(a,"dungeoncreate",create,"Lv 15");var low=server.sessions.get(a.token);low.profile.wornMask=0;denied(a,"dungeoncreate",create,"Lv 15");low.profile.wornMask=3;denied(b,"tribalcreate",create,"Lv 30");denied(c,"tribalcreate",create,"Lv 29");
 int tribal=d.call("tribalcreate",create).readInt();ok(server.rooms.get(tribal).dungeonStage==3,"Level30 creates Tribal");
 denied(c,"join",o->{o.writeInt(tribal);o.writeByte(0);o.writeUTF("");},"Lv 30");
 var guest=server.sessions.get(c.token);guest.inviteRoom=tribal;guest.inviteTeam=0;guest.inviteSeat=2;denied(c,"inviteanswer",o->o.writeBoolean(true),"Lv 29");ok(guest.room==0,"Denied invitation never joins");
 denied(c,"dungeonteam",null,"Lv 30");
 int ant=b.call("dungeoncreate",create).readInt();ok(server.rooms.get(ant).dungeonStage==1,"Level15 creates Ant");c.call("dungeonteam",null);ok(guest.room==ant,"Quick join skips inaccessible Tribal and joins Ant");c.call("leave",null);b.call("leave",null);d.call("leave",null);
 int party=d.call("dungeoncreate",create).readInt();c.call("join",o->{o.writeInt(party);o.writeByte(0);o.writeUTF("");});var room=server.rooms.get(party);
 denied(d,"dungeonsettings",o->{o.writeByte(0);o.writeByte(1);},"lv29");ok(room.dungeonStage==1&&room.phase==0,"Failed map change keeps Ant progress");
 room.dungeonStage=3;denied(d,"start",null,"Lv 30");ok(room.phase==0&&room.battle==null,"Start checks every member before mutation");room.dungeonStage=1;
 guest.profile.testCheats=true;denied(d,"dungeonsettings",o->{o.writeByte(0);o.writeByte(1);},"Lv 30");guest.profile.testCheats=false;
 guest.profile.experience=Progression.experienceForLevel(30);d.call("dungeonsettings",o->{o.writeByte(0);o.writeByte(1);});c.call("ready",o->o.writeBoolean(true));d.call("start",null);ok(room.phase==3,"Exact level30 party starts Tribal");
 }
 }
 System.out.println("PASS "+checks+" real TCP level boundaries, create/join/invite/quick-team/map/start and cheat checks");
 }
}
