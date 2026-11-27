import java.io.*;
import java.util.*;
/** Room adapter for the shared three-encounter campaign (saved stage identities remain stable). Ant Cave and PvP keep their routes. */
final class TribalDungeon {
 static boolean isRoom(GunnyServer server,int id){var r=server.rooms.get(id);return r!=null&&is(r);}
 static boolean is(GunnyServer.Room r){return r.kind==3&&r.dungeonStage>=3;}
 static void eligible(GunnyServer.Session s,int difficulty){if(!s.tribal||!s.tribal797||!s.sulan7912)throw new IllegalArgumentException("Cập nhật game 7.9.21 để vào Bộ Lạc Tà Thần");if(difficulty<0||difficulty>DungeonBalance.MAX_DIFFICULTY)throw new IllegalArgumentException("Mức độ phó bản không hợp lệ");int level=Progression.level(s.profile.experience);if(level<TribalRules.MIN_LEVEL)throw new IllegalArgumentException("Bộ Lạc Tà Thần yêu cầu Lv 30. "+s.profile.name+" hiện ở Lv "+level+".");GunnyServer.requireWeapon(s);}
 static void handle(GunnyServer server,String op,GunnyServer.Session s,DataInputStream in,DataOutputStream out)throws Exception {
  if(!s.tribal||!s.tribal797)throw new IllegalArgumentException("Cập nhật game 7.9.18 để chơi Bộ Lạc Tà Thần");
  if(op.equals("tribalwing")){if(s.room!=0)throw new IllegalArgumentException("Rời phòng trước khi thay cánh");server.store.tribal("wing",Map.of("user",s.id,"equip",in.readBoolean()));s.profile=server.store.profile(s.id);Expansion780.write(out,s.profile);return;}
  if(op.equals("tribalresume")){GunnyServer.Room room=server.rooms.get(s.room);boolean active=room!=null&&is(room);out.writeBoolean(active);if(active)synchronized(room){server.lobby(out,room,s);}return;}
  if(op.equals("tribalcreate")){int difficulty=in.readUnsignedByte();String name=GunnyServer.validRoomName(in.readUTF()),secret=in.readUTF();eligible(s,difficulty);server.store.tribal("status",Map.of());if(secret.length()>16)throw new IllegalArgumentException("Mật mã tối đa 16 ký tự");synchronized(server.rooms){if(server.rooms.size()>=server.maxRooms)throw new IllegalArgumentException("Hết phòng trống");server.leave(s);var room=server.new Room(s,6,2,-1,true);room.kind=3;room.dungeonStage=3;room.difficulty=difficulty;room.title=name;room.secret=secret;room.strict=true;room.seconds=15;room.mapLimit=12;room.refreshWaiting();server.rooms.put(room.id,room);s.room=room.id;server.lobby(out,room,s);}return;}
  var r=server.requireRoom(s);synchronized(r){if(!is(r))throw new IllegalArgumentException("Không phải Bộ Lạc Tà Thần");if(op.equals("tribalsettings")){int difficulty=in.readUnsignedByte();if(r.host!=s||r.phase!=0||r.dungeonStage!=3)throw new IllegalArgumentException("Chỉ chọn độ khó trước ải 1");for(var member:r.members)if(member!=null)eligible(member,difficulty);r.difficulty=difficulty;r.clearArmed();server.lobby(out,r,s);return;}}
  throw new IllegalArgumentException("Sai thao tác Bộ Lạc Tà Thần");
 }
 // The selected bot ID, not an absent Session or an avatar preview, owns an AI seat.
 static boolean allyPresent(GunnyServer.Room r,int seat){return seat>=0&&seat<6&&seat%2==0&&(r.members[seat]!=null||r.selectedBots[seat]>=0);}
 static void waiting(GunnyServer.Room r){
  for(int i=0;i<6;i++){
   var a=r.avatars[i];
   if(i%2==1){a.name=r.battle==null?"Bộ Lạc Tà Thần":TribalRules.name(r.battle.units[i].npc);a.bot=true;a.owner=null;}
   else if(r.members[i]==null){
    int id=r.selectedBots[i];a.owner=null;
    if(id>=0){a.bot=true;a.name=GunnyServer.BOT_NAMES[id];a.sex=id/4;a.skin=GunnyServer.BOT_SKINS[id];a.weapon=GunnyServer.WEAPONS[id%4];a.experience=Progression.botExperience(id);}
    else{a.name="Đang chờ";a.bot=false;a.experience=0;}
   }
  }
 }
 static void begin(GunnyServer.Room r){
  if(r.dungeonStage==3)r.campaignId=UUID.randomUUID().toString();
  for(int i=0;i<6;i+=2){
   var u=r.battle.units[i];
   if(!allyPresent(r,i)){u.hp=u.maxHp=0;u.energy=0;u.dead=true;u.human=false;}
   else if(r.members[i]==null){var a=r.avatars[i];u.human=false;u.dead=false;u.weapon=a.weapon;u.experience=a.experience;u.botStyle=r.selectedBots[i]%4;ItemRules.apply(u,a.wornMask);}
  }
  TribalRules.setup(r.battle,r.dungeonStage-2,r.difficulty,r.beanSaved);waiting(r);checkpoint(r);
 }
 static void progress(GunnyServer.Room r){if(r.battle.over){r.savedTribalTurn=-2;checkpoint(r);return;}if(r.savedTribalTurn!=r.battle.turn){r.savedTribalTurn=r.battle.turn;checkpoint(r);}}
 static Map<String,Object> state(GunnyServer.Room r){List<Object> units=new ArrayList<>();for(var u:r.battle.units)units.add(List.of(u.npc,u.x,u.y,u.hp,u.maxHp,u.dead));return Map.of("turn",r.battle.turn,"round",r.battle.tribalRound,"phase",r.battle.tribalPhase,"wave",r.battle.tribalWave,"active",r.battle.active,"units",units,"bean",r.beanSaved,"over",r.battle.over,"winner",r.battle.winner);}
 static void checkpoint(GunnyServer.Room r){String run=r.campaignId;int stage=r.dungeonStage-2,difficulty=DungeonBalance.rewardTier(r.difficulty),turn=r.battle.turn;List<String> members=new ArrayList<>();for(var m:r.members)if(m!=null)members.add(m.id);var state=state(r);r.savesTribal(run,stage,difficulty,turn,members,state);}
 static void settle(GunnyServer server,GunnyServer.Room r,List<Map<String,Object>> players)throws Exception {var payload=new HashMap<String,Object>();payload.put("match",r.match);payload.put("run",r.campaignId);payload.put("stage",r.dungeonStage-2);int rewardDifficulty=DungeonBalance.rewardTier(r.difficulty);payload.put("difficulty",rewardDifficulty);payload.put("winner",r.battle.winner);payload.put("players",players);var config=RewardSettings.row(true,r.difficulty);payload.put("config",config);payload.put("min",RewardSettings.n(config,"stoneMin"));payload.put("max",RewardSettings.n(config,"stoneMax"));server.store.tribal("finish",payload);}
 static int[] claim(GunnyServer server,GunnyServer.Room r,String id,int pick,int card)throws Exception {var result=server.store.tribal("claim",Map.of("match",r.match,"user",id,"pick",pick,"card",card));return new int[]{Json.num(result,"card",-1),Json.num(result,"type",0),Json.num(result,"amount",0),Json.num(result,"pick",-1)};}
 static boolean reconnect(GunnyServer server,GunnyServer.Session old,GunnyServer.Session fresh){var r=server.rooms.get(old.room);if(r==null||!is(r)||!fresh.tribal||!fresh.tribal797||(r.host.delta!=fresh.delta||r.host.sulan7912!=fresh.sulan7912))return false;synchronized(r){int seat=r.slot(old);if(seat<0)return false;r.members[seat]=fresh;for(var avatar:r.avatars)if(avatar!=null&&avatar.owner==old)avatar.owner=fresh;System.arraycopy(old.kit,0,fresh.kit,0,3);if(r.host==old)r.host=fresh;fresh.room=r.id;fresh.sequence=0;fresh.held=0;fresh.ready=false;fresh.armed=old.armed;fresh.resolution=-1;r.lastActive=System.currentTimeMillis();return true;}}
}
