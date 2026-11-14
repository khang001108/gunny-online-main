import java.io.*;import java.util.*;
/** Two-stage cooperative Ant Cave. PVP rooms never pass through this controller. */
final class Dungeon780 {
 static boolean space(GunnyServer.Room room){for(int i=2;i<6;i+=2)if(room.members[i]==null&&room.selectedBots[i]<0&&(room.closedSlots&(1<<i))==0)return true;return false;}
 static void eligible(GunnyServer.Session s,int difficulty){
  int level=Progression.level(s.profile.experience);
  if(level<15)throw new IllegalArgumentException("Huyệt Ma Kiến yêu cầu Lv 15. Tài khoản hiện ở Lv "+level+".");
  if(difficulty<0||difficulty>2||difficulty==2&&!s.profile.dungeonClear)
   throw new IllegalArgumentException("Vượt chế độ Thường để mở Khó");
 }
 static void handle(GunnyServer server,String op,GunnyServer.Session s,DataInputStream in,DataOutputStream out)throws Exception{
  if(!s.flock)throw new IllegalArgumentException("Cập nhật game để chơi Huyệt Ma Kiến.");
  if(op.equals("dungeoncontinue")){
   var room=server.requireRoom(s);
   synchronized(room){
    if(room.kind!=3)throw new IllegalArgumentException("Không phải phòng ải viễn chinh");
    if(room.phase!=0||room.dungeonStage!=2){
     if(room.phase!=2||room.dungeonStage!=1||room.battle.winner!=0||!room.cardsOpen())
      throw new IllegalArgumentException("Chưa thể vào ải 2");
     for(var member:room.members)if(member!=null&&room.cardPicks.getOrDefault(member.id,0)<1)
      throw new IllegalArgumentException("Chờ đồng đội lật thẻ ải 1");
     room.phase=0;room.dungeonStage=2;room.battle=null;room.ground=null;
     room.resetDungeonResult();room.clearArmed();room.refreshWaiting();
    }
    server.lobby(out,room,s);
   }
   return;
  }
  if(op.equals("dungeonrooms")){
   if(Progression.level(s.profile.experience)<15)s.profile=server.store.profile(s.id);
   eligible(s,0);
   server.roomList(out,s,true);
   return;
  }
  if(op.equals("dungeonteam")){
   joinWaitingTeam(server,s,out);
   return;
  }
  if(op.equals("dungeonsettings")){var r=server.requireRoom(s);int difficulty=in.readUnsignedByte();synchronized(r){if(r.kind!=3||r.host!=s||r.phase!=0||r.dungeonStage==2)throw new IllegalArgumentException("Chỉ chủ phòng chờ được chọn ải viễn chinh");for(var member:r.members)if(member!=null)eligible(member,difficulty);r.difficulty=difficulty;r.clearArmed();server.lobby(out,r,s);}return;}
  if(op.equals("dungeoncreate")||op.equals("dungeonquick")){int difficulty=in.readUnsignedByte();String name=op.equals("dungeoncreate")?GunnyServer.validRoomName(in.readUTF()):"Huyệt Ma Kiến";String secret=op.equals("dungeoncreate")?in.readUTF():"";eligible(s,difficulty);if(secret.length()>16)throw new IllegalArgumentException("Mật mã tối đa 16 ký tự");
   synchronized(server.rooms){GunnyServer.Room room=null;if(op.equals("dungeonquick"))for(var r:server.rooms.values())synchronized(r){if(r.kind==3&&r.phase==0&&r.dungeonStage!=2&&r.difficulty==difficulty&&r.secret.isEmpty()&&r.cardPlayers()<3&&space(r)&&r.host.actors==s.actors&&r.host.live==s.live&&r.host.equipment==s.equipment&&r.host.arsenal==s.arsenal&&r.host.items==s.items&&r.host.itemVersion==s.itemVersion&&r.host.itemFx==s.itemFx){room=r;break;}}
    if(room==null){if(server.rooms.size()>=server.maxRooms)throw new IllegalArgumentException("Hết phòng trống");server.leave(s);room=server.new Room(s,6,2,-1,true);room.kind=3;room.difficulty=difficulty;room.title=name;room.secret=secret;room.strict=true;room.seconds=15;room.mapLimit=8;room.map=6;room.refreshWaiting();server.rooms.put(room.id,room);s.room=room.id;}
    else{synchronized(room){int slot=-1;for(int i=2;i<6;i+=2)if(room.members[i]==null&&room.selectedBots[i]<0&&(room.closedSlots&(1<<i))==0){slot=i;break;}if(slot<0)throw new IllegalArgumentException("Phòng đã đủ người");server.leave(s);room.members[slot]=s;s.room=room.id;room.refreshWaiting();}}
    server.lobby(out,room,s);
   }return;
  }
  throw new IllegalArgumentException("Sai thao tác ải viễn chinh");
 }
 private static void joinWaitingTeam(GunnyServer server,GunnyServer.Session viewer,DataOutputStream out)throws Exception{
  eligible(viewer,0);
  synchronized(server.rooms){
   if(viewer.room!=0)throw new IllegalArgumentException("Rời phòng hiện tại trước khi tìm tổ đội.");
   var candidates=new ArrayList<>(server.rooms.values());
   Collections.shuffle(candidates,server.random);
   for(var room:candidates)synchronized(room){
    if(server.rooms.get(room.id)!=room||room.kind!=3||room.phase!=0||room.dungeonStage==2
        ||!room.secret.isEmpty()||room.difficulty==2&&!viewer.profile.dungeonClear
        ||!compatible(room.host,viewer))continue;
    for(int seat=2;seat<6;seat+=2){
     if(room.occupiedSeat(seat)||(room.closedSlots&(1<<seat))!=0)continue;
     room.members[seat]=viewer;
     viewer.room=room.id;
     viewer.held=0;
     viewer.armed=false;
     room.refreshWaiting();
     server.lobby(out,room,viewer);
     return;
    }
   }
  }
  throw new IllegalArgumentException("Chưa có phòng ải viễn chinh phù hợp còn chỗ. Hãy thử lại hoặc chọn Tạo phòng.");
 }

 private static boolean compatible(GunnyServer.Session host,GunnyServer.Session viewer){
  return host.flock&&host.expansion==viewer.expansion&&host.actors==viewer.actors
      &&host.live==viewer.live&&host.equipment==viewer.equipment&&host.arsenal==viewer.arsenal
      &&host.items==viewer.items&&host.itemVersion==viewer.itemVersion&&host.itemFx==viewer.itemFx;
 }

 static void waiting(GunnyServer.Room r){for(int i=0;i<6;i++){var a=r.avatars[i];if(i%2==1){int type=r.battle==null?0:r.battle.units[i].npc;a.name=AntCaveRules.name(type,r.dungeonStage==2);a.bot=true;a.owner=null;a.experience=r.battle!=null&&r.dungeonStage>0?r.battle.units[i].experience:Progression.experienceForLevel(r.difficulty==2?30:r.difficulty==1?20:15);}else if(r.members[i]==null&&r.selectedBots[i]<0){a.name="Đang chờ";a.bot=false;a.owner=null;a.experience=0;}}}
 static void begin(GunnyServer.Room r){if(r.dungeonStage!=2)r.dungeonStage=1;r.antKills=r.antSpawned=r.antAlive=0;setup(r);}
 static void setup(GunnyServer.Room r){Battle b=r.battle;b.dungeonDifficulty=r.difficulty;b.dungeonStage=r.dungeonStage;for(int i=0;i<6;i++){Battle.Unit u=b.units[i];u.npc=0;if(i%2==0){if(r.members[i]==null&&r.selectedBots[i]<0){u.hp=0;u.dead=true;}else{if(r.members[i]==null){u.human=false;u.weapon=r.avatars[i].weapon;u.experience=r.avatars[i].experience;ItemRules.apply(u,3);}
   AntCaveRules.placeAlly(b,i);}}else{u.hp=0;u.dead=false;u.npc=r.dungeonStage==2&&i!=1?2:0;}}
  if(r.dungeonStage==1){
   r.antAlive=AntCaveRules.initialAntWave(b);
   r.antSpawned=AntCaveRules.count(r.antAlive);
   b.active=AntCaveRules.firstAnt(b,1);
   b.botPhase=AntCaveRules.ANT_INTRO;
  }else{
   AntCaveRules.spawn(b,1,3,r.difficulty);
   AntCaveRules.place(b,1,2,0);
   AntCaveRules.startQueenIntro(b);
  }
  waiting(r);
 }
 static void progress(GunnyServer.Room r){Battle b=r.battle;
  if(r.dungeonStage==1){
   int remaining=AntCaveRules.survivingAnts(b,r.antAlive);
   r.antKills+=AntCaveRules.count(r.antAlive^remaining);r.antAlive=remaining;
   if(b.over&&b.winner==0){b.ballCount=b.bursts=b.settle=0;}
   if((b.pendingNext||b.over)&&!b.busy()&&(!b.over||b.winner==0)){
    if(r.antKills>=AntCaveRules.ANT_GOAL){b.over=true;b.winner=0;b.pendingNext=false;return;}
    int next=AntCaveRules.spawnAntWave(b,r.antSpawned,r.antAlive);
    r.antSpawned+=AntCaveRules.count(next^r.antAlive);r.antAlive=next;
    if(b.over&&b.winner==0){b.over=false;b.winner=-1;b.pendingNext=true;}
   }
  }
  if(r.dungeonStage==2&&b.units[1].hp<=0&&!b.over)b.check();
  if(!b.over&&!AntCaveRules.dying(b)&&b.turn>300){b.over=true;b.winner=1;b.pendingNext=false;}
 }
}
