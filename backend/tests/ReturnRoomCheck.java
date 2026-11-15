import java.net.*;
import java.nio.file.*;
import java.io.*;

public final class ReturnRoomCheck {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);System.out.println("PASS: "+message);}
 static GunnyServer.Session player(String id){var s=new GunnyServer.Session();s.id=id;s.profile=new CloudStore.Profile(id,id);s.profile.name=id;s.profile.owned=1;s.profile.equipped=0;s.profile.experience=Progression.experienceForLevel(20);s.lastSeen=System.currentTimeMillis();return s;}
 public static void main(String[] args)throws Exception{
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),new CloudStore.Memory())){
   server.clock.shutdownNow();
   for(int kind:new int[]{0,2,3}){
    var p1=player("host"+kind);var p2=player("guest"+kind);p1.expansion=true;
    var r=server.new Room(p1,0,1,-1,kind>0);r.kind=kind;r.members[kind==3?2:1]=p2;r.selectedBots[kind==3?4:3]=2;
    p1.room=p2.room=r.id;server.rooms.put(r.id,r);r.refreshWaiting();r.ground=r.newGround(0);r.battle=new Battle(r.ground);r.battle.over=true;r.battle.winner=0;r.phase=2;r.settled=true;r.dungeonStage=kind==3?2:0;
    r.battle.damage[kind==3?2:1]=10;r.cardPicks.put(p1.id,kind>0?2:1);r.cardPicks.put(p2.id,kind>0?2:1);
    String match=r.match;
    int picks=r.cardPicks.remove(p1.id);boolean rejected=false;try{r.returnAfterMatch(p1);}catch(IllegalArgumentException expected){rejected=true;}check(rejected&&r.returned.isEmpty(),"Unclaimed rewards cannot be discarded by return");r.cardPicks.put(p1.id,picks);r.returnAfterMatch(p1);
    check(r.viewerPhase(p1)==0&&r.viewerPhase(p2)==2&&r.battle!=null,"Early return keeps teammate's result available, kind "+kind);
    var bytes=new ByteArrayOutputStream();server.lobby(new DataOutputStream(bytes),r,p1);var packet=new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));check(packet.readInt()==r.id&&packet.readByte()==0,"Returned player receives waiting room phase");
    check(!r.canStart(),"Cannot restart while teammate is receiving rewards");
    r.returnAfterMatch(p2);
    check(r.phase==0&&r.battle==null&&!r.settled&&!r.match.equals(match),"Last return resets match state, kind "+kind);
    check(p1.room==r.id&&p2.room==r.id&&r.members[0]==p1&&r.members[kind==3?2:1]==p2,"P1 and P2 keep original room and seats");
    check(r.selectedBots[kind==3?4:3]==2&&r.avatars[kind==3?4:3].bot,"Selected bot returns to same seat");
    check(r.cardPicks.isEmpty()&&!p2.armed,"Rewards and ready state reset for next match");
    if(kind==3)check(r.dungeonStage==1,"Completed dungeon returns to stage one setup");
    r.returnAfterMatch(p2);check(r.phase==0,"Duplicate return is harmless");r.start();check(r.phase==3&&r.participants.size()==2,"Same party can start a fresh match");
   }
  }
 }
}

