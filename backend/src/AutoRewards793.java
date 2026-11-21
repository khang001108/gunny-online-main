import java.io.*;import java.util.*;
final class AutoRewards793{
 static void claim(GunnyServer server,GunnyServer.Room r,GunnyServer.Session user)throws Exception{
  synchronized(r){if(!r.settled||r.battle==null||!r.battle.over||System.currentTimeMillis()<r.rewardsAt+12000)throw new IllegalArgumentException("Chưa hết thời gian chọn thẻ");}
  int limit=DungeonRewards.picks(r.kind,r.dungeonStage);for(int tries=0;tries<36;tries++){
   int pick,card=-1;synchronized(r){if(!r.rewardEligible(user))return;pick=r.cardPicks.getOrDefault(user.id,0);if(pick>=limit)return;int total=DungeonRewards.cards(r.kind,r.dungeonStage),start=server.random.nextInt(total);for(int n=0;n<total;n++){int c=(start+n)%total;if(r.cardNames[c]==null||r.cardNames[c].isEmpty()){card=c;break;}}}if(card<0)throw new IllegalArgumentException("Bộ thẻ đã kín");
   try{if(r.kind>=2){var body=new byte[]{(byte)pick,(byte)card};Expansion780.handle(server,"claim780",user,new DataInputStream(new ByteArrayInputStream(body)),new DataOutputStream(new ByteArrayOutputStream()));}
   else{int[] reward=server.store.claim(r.match,user.id,card,200+server.random.nextInt(801));user.profile=server.store.profile(user.id);synchronized(r){r.cardNames[reward[0]]=user.profile.name;r.cardCoins[reward[0]]=reward[1];r.cardPicks.put(user.id,1);}}
   }catch(IllegalArgumentException collision){if(tries==35)throw collision;}
  }
 }
}
