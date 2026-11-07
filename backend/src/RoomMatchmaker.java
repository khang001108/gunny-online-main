import java.io.*;

/** Match whole waiting rosters without mixing solo, private and 3v3 queues. */
final class RoomMatchmaker {
 final GunnyServer server;
 RoomMatchmaker(GunnyServer s){server=s;}
 int size(GunnyServer.Room r){int n=0;for(int i=0;i<r.count;i+=2)if(r.members[i]!=null)n++;if(r.allyBot>=0)n++;return n;}
 boolean ready(GunnyServer.Room r){if(r.equipmentPending>0)return false;for(var member:r.members)if(member!=null&&member.equipment&&(member.profile.wornMask&1)==0)return false;if(r.phase!=0||r.opponents()||r.queued&&r.queueSince>0&&System.currentTimeMillis()-r.queueSince>=60000)return false;for(int i=2;i<r.count;i+=2)if(r.members[i]!=null&&!r.members[i].armed)return false;return true;}
 boolean compatible(GunnyServer.Room a,GunnyServer.Room b){return a.host.itemFx==b.host.itemFx&&a.host.items==b.host.items&&a.host.itemVersion==b.host.itemVersion&&a.host.arsenal==b.host.arsenal&&a.host.equipment==b.host.equipment&&a!=b&&a.kind==b.kind&&a.count==b.count&&ready(a)&&ready(b)&&size(a)==size(b);}
 void merge(GunnyServer.Room a,GunnyServer.Room b){
  for(int i=0;i<a.count;i+=2){var member=b.members[i];a.members[i+1]=member;if(member!=null){member.room=a.id;member.armed=true;}b.members[i]=null;}
  if(a.kind==0&&size(a)==1)a.count=2;a.enemyBot=b.allyBot;a.queued=b.queued=false;server.rooms.remove(b.id);a.refreshWaiting();for(var s:a.members)if(s!=null)s.armed=true;a.start();
 }
 void voteBot(GunnyServer.Session s)throws Exception{
  synchronized(server.rooms){var r=server.requireRoom(s);synchronized(r){int seat=r.slot(s);if(r.phase!=0||seat<0||seat%2!=0||r.opponents()||r.queueSince==0||r.timedOut||System.currentTimeMillis()-r.queueSince>=60000)throw new IllegalArgumentException("Chỉ xác nhận bot khi đang tìm trận trong 60 giây.");r.botVotes|=1<<seat;for(int i=0;i<r.count;i+=2)if(r.members[i]!=null&&(r.botVotes&(1<<i))==0)return;
   int fighters=size(r);if(r.kind==0&&fighters==1)r.count=2;r.botMatch=true;r.enemyBot=fighters>1?server.random.nextInt(8):-1;for(int i=0;i<r.count;i+=2)if(r.members[i]!=null)r.members[i].armed=true;r.queued=false;r.start();
  }}
 }
 void start(GunnyServer.Session s,int target,String password)throws Exception{
  if(target!=0)throw new IllegalArgumentException("Đã bỏ đấu phòng theo mã.");
  synchronized(server.rooms){var a=server.requireRoom(s);synchronized(a){if(a.host!=s||a.phase!=0)throw new IllegalArgumentException("Chỉ chủ phòng đang chờ được tìm đối thủ.");if(a.queued&&target==0){a.queued=false;a.queueSince=0;a.botVotes=0;return;}for(var member:a.members)if(member!=null&&((member.profile.owned&(1<<member.profile.equipped))==0||member.equipment&&(member.profile.wornMask&1)==0))throw new IllegalArgumentException("Không có trang bị vũ khí không thể bắt đầu");if(a.opponents()){a.start();return;}if(!ready(a))throw new IllegalArgumentException("Chờ đồng minh sẵn sàng.");if(a.kind==2&&size(a)!=3)throw new IllegalArgumentException("Tổ đội cần đủ 3 người cùng đội.");
   a.queued=true;a.timedOut=false;a.queueSince=System.currentTimeMillis();a.botVotes=0;for(var b:server.rooms.values()){if(b==a)continue;synchronized(b){if(b.queued&&compatible(a,b)){merge(a,b);return;}}}

  }}
 }
}
