import java.io.*;

/** Match waiting rosters; solo may pair with a queued one-human private room. */
final class RoomMatchmaker {
 final GunnyServer server;
 RoomMatchmaker(GunnyServer s){server=s;}
 int size(GunnyServer.Room r){int n=0;for(int i=0;i<r.count;i+=2)if(r.members[i]!=null||r.selectedBots[i]>=0||i==2&&r.allyBot>=0)n++;return n;}
 boolean ready(GunnyServer.Room r){if(r.hasReturningParty())return false;if(r.equipmentPending>0)return false;for(var member:r.members)if(member!=null&&!GunnyServer.hasWeapon(member.profile))return false;if(r.phase!=0||r.opponents()||r.queued&&r.queueSince>0&&System.currentTimeMillis()-r.queueSince>=60000)return false;for(int i=2;i<r.count;i+=2)if(r.members[i]!=null&&!r.members[i].armed)return false;return true;}
 boolean compatible(GunnyServer.Room a,GunnyServer.Room b){return a.host.delta==b.host.delta&&a.host.expansion==b.host.expansion&&a.host.actors==b.host.actors&&a.host.live==b.host.live&&a.host.itemFx==b.host.itemFx&&a.host.items==b.host.items&&a.host.itemVersion==b.host.itemVersion&&a.host.arsenal==b.host.arsenal&&a.host.equipment==b.host.equipment&&a!=b&&a.kind==b.kind&&a.count==b.count&&ready(a)&&ready(b)&&size(a)==size(b);}
 boolean sameProtocol(GunnyServer.Session a,GunnyServer.Session b){return a!=b&&!a.id.equals(b.id)&&a.delta==b.delta&&a.expansion==b.expansion&&a.actors==b.actors&&a.live==b.live&&a.itemFx==b.itemFx&&a.items==b.items&&a.itemVersion==b.itemVersion&&a.arsenal==b.arsenal&&a.equipment==b.equipment;}
 boolean singleHuman(GunnyServer.Room r){
  if(r.kind!=0&&r.kind!=1||r.phase!=0||!r.strict||r.hasReturningParty()||r.equipmentPending>0||r.botMatch||r.allyBot>=0||r.enemyBot>=0||r.bot>=0||!GunnyServer.hasWeapon(r.host.profile))return false;
  int humans=0;for(int i=0;i<r.count;i++){if(r.selectedBots[i]>=0)return false;if(r.members[i]!=null)humans++;}
  return humans==1&&r.slot(r.host)>=0;
 }
 boolean soloWaiting(GunnyServer.Room r,long now){return singleHuman(r)&&r.queued&&!r.timedOut&&r.queueSince>0&&now-r.queueSince<60000&&now-r.host.lastSeen<45000;}
 // Called under rooms + participating room locks. Solo always owns the battle state.
 void pairSolo(GunnyServer.Room solo,GunnyServer.Session opponent){
  solo.members[1]=opponent;opponent.room=solo.id;opponent.held=0;opponent.armed=true;solo.host.armed=true;
  solo.requestedMap=-1;solo.map=-1;solo.mapLimit=6;solo.seconds=12;solo.refreshWaiting();solo.start();
 }
 GunnyServer.Room quickSolo(GunnyServer.Session s){
  synchronized(server.rooms){
   server.leave(s);
   for(var candidate:server.rooms.values())synchronized(candidate){
    if(server.rooms.get(candidate.id)!=candidate||!soloWaiting(candidate,System.currentTimeMillis())||!sameProtocol(candidate.host,s))continue;
    if(candidate.kind==1){pairSolo(candidate,s);return candidate;}
    var solo=newSolo(s);synchronized(solo){server.rooms.put(solo.id,solo);takePrivate(solo,candidate);}return solo;
   }
   if(server.rooms.size()>=server.maxRooms)throw new IllegalArgumentException("Hết chỗ tìm trận.");
   var solo=newSolo(s);server.rooms.put(solo.id,solo);return solo;
  }
 }
 GunnyServer.Room newSolo(GunnyServer.Session s){
  var r=server.new Room(s,-1,0,-1);r.kind=1;r.strict=true;r.mapLimit=6;r.seconds=12;r.refreshWaiting();r.queued=true;r.queueSince=System.currentTimeMillis();s.room=r.id;s.armed=true;s.held=0;return r;
 }
 void takePrivate(GunnyServer.Room solo,GunnyServer.Room party){
  var opponent=party.host;pairSolo(solo,opponent);party.members[party.slot(opponent)]=null;party.queued=false;server.rooms.remove(party.id);
 }
 void saveReturnRooms(GunnyServer.Room battle,GunnyServer.Room party){
  var home=party.waitingCopy();
  for(var member:party.members)if(member!=null)battle.returnRooms.put(member,home);
 }
 void merge(GunnyServer.Room a,GunnyServer.Room b){
  saveReturnRooms(a,a);saveReturnRooms(a,b);
  for(int i=0;i<a.count;i+=2){var member=b.members[i];a.members[i+1]=member;a.selectedBots[i+1]=b.selectedBots[i];if(member!=null){member.room=a.id;member.armed=true;}b.members[i]=null;}
  if(a.kind==0&&size(a)==1)a.count=2;a.enemyBot=b.allyBot;a.queued=b.queued=false;server.rooms.remove(b.id);a.refreshWaiting();for(var s:a.members)if(s!=null)s.armed=true;a.start();
 }
 void voteBot(GunnyServer.Session s)throws Exception{
  synchronized(server.rooms){var r=server.requireRoom(s);synchronized(r){int seat=r.slot(s);if(r.kind==1){
   // A late/repeated H after a real match must not interrupt loading.
   if(r.phase>0)return;
   if(r.host!=s||seat!=0)throw new IllegalArgumentException("Không phải lượt tìm trận của bạn.");
   if(r.timedOut||r.queueSince==0||System.currentTimeMillis()-r.queueSince>=60000){r.queued=false;r.queueSince=0;r.botVotes=0;r.timedOut=true;return;}
   if(!soloWaiting(r,System.currentTimeMillis()))throw new IllegalArgumentException("Chưa thể đấu bot.");
   r.selectedBots[1]=server.random.nextInt(8);r.botMatch=true;r.requestedMap=-1;r.map=-1;r.queued=false;s.armed=true;r.start();return;
  }if(r.phase!=0||seat<0||seat%2!=0||r.opponents()||r.queueSince==0||r.timedOut||System.currentTimeMillis()-r.queueSince>=60000)throw new IllegalArgumentException("Chỉ xác nhận bot khi đang tìm trận trong 60 giây.");r.botVotes|=1<<seat;for(int i=0;i<r.count;i+=2)if(r.members[i]!=null&&(r.botVotes&(1<<i))==0)return;
   saveReturnRooms(r,r);int fighters=size(r);if(r.kind==0&&fighters==1)r.count=2;r.botMatch=true;r.enemyBot=fighters>1?server.random.nextInt(8):-1;for(int i=0;i<r.count;i+=2)if(r.members[i]!=null)r.members[i].armed=true;r.queued=false;r.start();
  }}
 }
 void start(GunnyServer.Session s,int target,String password)throws Exception{
  if(target!=0)throw new IllegalArgumentException("Đã bỏ đấu phòng theo mã.");
  synchronized(server.rooms){var a=server.requireRoom(s);synchronized(a){if(a.host!=s||a.phase!=0)throw new IllegalArgumentException("Chỉ chủ phòng đang chờ được tìm đối thủ.");if(a.queued&&target==0){a.queued=false;a.queueSince=0;a.botVotes=0;return;}for(var member:a.members)if(member!=null&&!GunnyServer.hasWeapon(member.profile))throw new IllegalArgumentException("Không có trang bị vũ khí không thể bắt đầu");if(a.opponents()){a.start();return;}if(!ready(a))throw new IllegalArgumentException("Chờ đồng minh sẵn sàng.");if(a.kind==2&&size(a)!=3)throw new IllegalArgumentException("Tổ đội cần đủ 3 người cùng đội.");
   a.queued=true;a.timedOut=false;a.queueSince=System.currentTimeMillis();a.botVotes=0;for(var b:server.rooms.values()){if(b==a)continue;synchronized(b){if(a.kind==0&&b.kind==1&&soloWaiting(a,System.currentTimeMillis())&&soloWaiting(b,System.currentTimeMillis())&&sameProtocol(a.host,b.host)){takePrivate(b,a);return;}if(b.queued&&compatible(a,b)){merge(a,b);return;}}}

  }}
 }
}
