import java.io.*;
import java.util.*;
final class DeckReveal {
 /** Unpicked cards are display only; use exactly the same configured distributions as claims. */
 static int[] dungeonCard(Map<String,Object> config,int rare,Random random){
  if(rare==2)return new int[]{2,RewardSettings.roll(config,"lucky",random)};
  if(rare>0)return new int[]{rare,1};
  return new int[]{StoneDrops.type(RewardSettings.grade(config,random.nextInt(100))),RewardSettings.roll(config,"stone",random)};
 }
 static boolean ready(GunnyServer.Room r){if(r.phase!=2||!r.settled||!r.cardsOpen())return false;int limit=r.host.expansion?DungeonRewards.picks(r.kind,r.dungeonStage):1;for(var m:r.members)if(m!=null&&r.rewardEligible(m)&&r.cardPicks.getOrDefault(m.id,0)<limit&&System.currentTimeMillis()-m.lastSeen<45000)return false;return true;}
 static void handle(GunnyServer server,GunnyServer.Session user,DataOutputStream out)throws Exception{
  var r=server.requireRoom(user);String match;boolean need;
  synchronized(r){match=r.match;need=TribalDungeon.is(r)&&(r.revealedDeck==null||!match.equals(r.revealMatch));if(!ready(r)){out.writeBoolean(false);return;}}
  int[] deck=need?server.store.tribalDeck(match):null;
  synchronized(r){if(!match.equals(r.match)){out.writeBoolean(false);return;}if(r.phase!=2||!r.settled||!r.cardsOpen()){out.writeBoolean(false);return;}
   int limit=r.host.expansion?DungeonRewards.picks(r.kind,r.dungeonStage):1;
   for(var member:r.members)if(member!=null&&r.rewardEligible(member)&&r.cardPicks.getOrDefault(member.id,0)<limit&&System.currentTimeMillis()-member.lastSeen<45000){out.writeBoolean(false);return;}
   int count=DungeonRewards.cards(r.kind,r.dungeonStage);
   if(r.revealedDeck==null||!r.match.equals(r.revealMatch)){int[] rare=deck;r.revealedDeck=new int[count*2];r.revealMatch=r.match;
    Map<String,Object> config=r.kind==3?RewardSettings.forRoom(r):null;
    for(int i=0;i<count;i++){
     int type,amount;
     if(r.cardNames[i]!=null){type=r.cardTypes[i];amount=r.cardCoins[i];}
     else if(r.kind==3){int[] reward=dungeonCard(config,rare==null?0:rare[i],server.random);type=reward[0];amount=reward[1];}
     else{type=r.kind==2&&server.random.nextBoolean()?1:0;amount=type==1?1+server.random.nextInt(10):r.kind==2?500+server.random.nextInt(1501):200+server.random.nextInt(801);}
     r.revealedDeck[i*2]=type;r.revealedDeck[i*2+1]=amount;
    }
   }for(int i=0;i<count;i++)if(r.cardNames[i]!=null){r.revealedDeck[i*2]=r.cardTypes[i];r.revealedDeck[i*2+1]=r.cardCoins[i];}out.writeBoolean(true);out.writeByte(count);for(int i=0;i<count;i++){out.writeByte(r.revealedDeck[i*2]);out.writeShort(r.revealedDeck[i*2+1]);}
  }
 }
}
