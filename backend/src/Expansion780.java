import java.io.*;import java.util.*;
/** Server-only entry points: client supplies intents; prices, ranks and rewards are authoritative. */
final class Expansion780 {
 static void write(DataOutputStream out,CloudStore.Profile p)throws IOException{out.writeInt(p.stones);out.writeBoolean(p.dungeonClear);for(int rank:p.forgeLevels)out.writeByte(rank);for(int progress:p.forgeProgress)out.writeShort(progress);for(int amount:p.craftStones)out.writeInt(amount);out.writeInt(p.luckyCharms);out.writeInt(p.protectionCharms);out.writeInt(p.wings);out.writeBoolean(p.wingEquipped);out.writeInt(p.vipSpears);out.writeBoolean(p.testCheats);out.writeBoolean(p.testGod);out.writeInt(p.giftLevel);out.writeInt(p.giftBags);out.writeInt(p.vipTickets);out.writeLong(p.vipUntil);}
 static int[] bonuses(CloudStore.Profile p){return ForgeRules.equipped(p.forgeLevels,p.gender,p.wornMask,p.equipped,p.skin,p.look);}
 static boolean handles(String op){return Set.of("economy","buystones","forge","claim780","dungeonrooms","dungeoncreate","dungeonquick","dungeonteam","dungeonsettings","dungeoncontinue").contains(op);}
 static void handle(GunnyServer server,String op,GunnyServer.Session s,DataInputStream in,DataOutputStream out)throws Exception{
  if(!s.expansion)throw new IllegalArgumentException("Cập nhật game 7.8.0");
  if(op.equals("economy")){s.profile=server.store.profile(s.id);write(out,s.profile);return;}
  if(op.equals("forge"))throw new IllegalArgumentException("Cập nhật game để dùng cường hóa xác suất.");
  if(op.equals("buystones")||op.equals("forge")){
   if(s.room!=0)throw new IllegalArgumentException("Rời phòng trước khi mua đá hoặc rèn");
   Map<String,Object> payload;if(op.equals("buystones"))payload=Map.of("quantity",in.readUnsignedShort());else payload=Map.of("sex",in.readUnsignedByte(),"kind",in.readUnsignedByte(),"item",in.readUnsignedByte(),"rank",in.readUnsignedByte(),"progress",in.readUnsignedShort());
   if(op.equals("forge")&&!ForgeRules.allowed((Integer)payload.get("kind")))throw new IllegalArgumentException("Chỉ rèn vũ khí, áo, nón và kính");
   String key=in.readUTF();s.profile=server.store.economy780(s.id,op.equals("buystones")?"buy":"forge",payload,key);GunnyServer.profile(out,s.profile);out.writeInt(s.profile.shoppingMoney);write(out,s.profile);return;
  }
  if(op.equals("claim780")){int pick=in.readUnsignedByte(),card=in.readUnsignedByte();var room=server.requireRoom(s);String match;Map<String,Object> rewardConfig;int difficulty;synchronized(room){if(!room.host.expansion||room.kind<2||room.phase!=2||!room.settled||!room.cardsOpen())throw new IllegalArgumentException("Chờ bộ thẻ mở");if(pick>=DungeonRewards.picks(room.kind,room.dungeonStage)||card>=DungeonRewards.cards(room.kind,room.dungeonStage))throw new IllegalArgumentException("Sai lượt hoặc vị trí thẻ thưởng của ải");match=room.match;difficulty=room.difficulty;rewardConfig=room.kind==3?RewardSettings.forRoom(room):null;}
   // Storage can take seconds. Never hold the room lock across HTTPS: the shared
   // simulation clock also visits finished rooms while other matches are running.
   if(TribalDungeon.is(room)){int[] reward=TribalDungeon.claim(server,room,s.id,pick,card);s.profile=server.store.profile(s.id);synchronized(room){room.cardNames[reward[0]]=s.profile.name;room.cardCoins[reward[0]]=reward[2];room.cardTypes[reward[0]]=reward[1];room.cardPicks.merge(s.id,pick+1,Math::max);}server.events.signal();out.writeByte(reward[0]);out.writeByte(reward[1]);out.writeShort(reward[2]);out.writeByte(reward[3]);GunnyServer.profile(out,s.profile);write(out,s.profile);return;}
   int[] r=room.kind==3?server.store.claimDungeon(match,s.id,pick,card,difficulty,rewardConfig):server.store.claim780(match,s.id,pick,card);int type=r[2]>0?StoneDrops.type(r.length>4?r[4]:1):0;s.profile=server.store.profile(s.id);
   synchronized(room){if(match.equals(room.match)){room.cardNames[r[0]]=s.profile.name;room.cardCoins[r[0]]=r[1]>0?r[1]:r[2];room.cardTypes[r[0]]=type;room.cardPicks.merge(s.id,pick+1,Math::max);}}
   server.events.signal();out.writeByte(r[0]);out.writeByte(type);out.writeShort(r[1]>0?r[1]:r[2]);out.writeByte(r[3]);GunnyServer.profile(out,s.profile);write(out,s.profile);return;
  }

  Dungeon780.handle(server,op,s,in,out);
 }
}
