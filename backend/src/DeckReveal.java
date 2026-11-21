import java.io.*;
final class DeckReveal {
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
    for(int i=0;i<count;i++){int type,amount;if(r.cardNames[i]!=null){type=r.cardTypes[i];amount=r.cardCoins[i];}else if(rare!=null&&rare[i]>0){type=rare[i];amount=1;}else if(r.kind==3){int grade=StoneDrops.grade(r.difficulty,server.random.nextInt(100));type=StoneDrops.type(grade);amount=TribalDungeon.is(r)?TribalRules.STONE_MIN[r.difficulty]+server.random.nextInt(TribalRules.STONE_MAX[r.difficulty]-TribalRules.STONE_MIN[r.difficulty]+1):10+server.random.nextInt(91);}else{type=r.kind==2&&server.random.nextBoolean()?1:0;amount=type==1?1+server.random.nextInt(10):r.kind==2?500+server.random.nextInt(1501):200+server.random.nextInt(801);}r.revealedDeck[i*2]=type;r.revealedDeck[i*2+1]=amount;}
   }out.writeBoolean(true);out.writeByte(count);for(int i=0;i<count;i++){out.writeByte(r.revealedDeck[i*2]);out.writeShort(r.revealedDeck[i*2+1]);}
  }
 }
}
