import java.io.*;import java.util.*;
/** Test-account permission is checked in storage; client flags never authorize a cheat. */
final class TestCheats {
 static void handle(GunnyServer server,GunnyServer.Session s,DataInputStream in,DataOutputStream out)throws Exception{
  int action=in.readUnsignedByte();boolean enabled=in.readBoolean();if(action>6)throw new IllegalArgumentException("Sai cheat");
  s.profile=server.store.profile(s.id);if(!s.profile.testCheats)throw new IllegalArgumentException("Admin chưa bật cheat cho tài khoản này");
  var room=server.rooms.get(s.room);
  if(action==6){String key=in.readUTF();if(!key.matches("[A-Za-z0-9_-]{1,58}"))throw new IllegalArgumentException("Sai mã cheat VIP");if(room!=null&&room.phase!=0)throw new IllegalArgumentException("Về sảnh để nhận thẻ VIP");for(int i=0;i<13;i++)if(s.profile.vipCards[i]<=999000)s.profile=server.store.vip(s.id,"grant",i,999,key+"-"+i);}
  else if(action==5){if(room==null)throw new IllegalArgumentException("Bạn chưa ở trong phó bản");synchronized(room){if(room.kind!=3||room.phase!=1||room.battle.over)throw new IllegalArgumentException("Chỉ qua ải đang đánh trong phó bản");Battle b=room.battle;if(TribalRules.active(b)){if(TribalRules.stage(b)==2){b.units[6].hp=Math.max(1,b.units[6].hp);room.beanSaved=true;}TribalRules.win(b,0);}else{b.over=true;b.winner=0;b.pendingNext=b.charging=false;b.bursts=b.ballCount=b.settle=0;b.winSerial++;}room.chunks.clear();room.chunkFrames=null;room.deltaFrame=null;room.chunkTurn=room.replayTurn=-1;room.replayStart=null;room.phase=2;room.lastActive=System.currentTimeMillis();for(var member:room.members)if(member!=null)member.resultAck=b.turn;server.queueSettlement(room);}}
  else {server.store.testCheat(s.id,action,enabled);s.profile=server.store.profile(s.id);if(room!=null)synchronized(room){int slot=room.slot(s);if(slot>=0&&room.battle!=null){var u=room.battle.units[slot];u.god=s.profile.testCheats&&s.profile.testGod;u.experience=s.profile.experience;ForgeRules.apply(u,Expansion780.bonuses(s.profile),s.profile.forgeLevels);ItemRules.stats(u,s.profile.wornMask);if(u.god)u.hp=u.maxHp;}}}
  GunnyServer.profile(out,s.profile);out.writeInt(s.profile.shoppingMoney);out.writeInt(s.profile.experience);Expansion780.write(out,s.profile);
 }
}
