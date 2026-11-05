import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Authorizations, result barrier and shared-card races with no real cloud account. */
public final class Social670ServerCheck {
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS: "+s);}
 public static void main(String[] args)throws Exception{
  CloudStore.Memory store=new CloudStore.Memory();for(int i=0;i<3;i++)store.register("p"+i+"@670.test","test-password-123","Player "+i);
  try(GunnyServer server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   GunnyServer.Session[] ss=new GunnyServer.Session[3];for(int i=0;i<3;i++){var s=ss[i]=new GunnyServer.Session();s.id=store.login("p"+i+"@670.test","test-password-123").userId;s.profile=store.profile(s.id);s.social=s.async=s.modern=true;s.lastSeen=System.currentTimeMillis();}
   var a=ss[0];var b=ss[1];var c=ss[2];server.social.friend(a,b.profile.publicId,0);ok(server.social.relations(b.id).size()==1,"Friend request belongs to the intended recipient");boolean rejected=false;try{server.social.friend(c,a.profile.publicId,1);}catch(IllegalArgumentException e){rejected=true;}ok(rejected,"An unrelated account cannot accept another person's request");server.social.friend(b,a.profile.publicId,1);ok(Boolean.TRUE.equals(server.social.relations(a.id).get(0).get("accepted")),"Accepted friendship is visible to both accounts");
   var r=server.new Room(a,1,2,-1);r.customAllies=true;r.strict=true;r.members[1]=b;r.members[2]=c;r.refreshWaiting();b.armed=c.armed=true;r.start();r.phase=1;r.battle.units[1].hp=0;r.battle.check();r.phase=2;server.queueSettlement(r);ok(!r.settlementQueued,"Settlement waits for every final-result receipt");a.resultAck=b.resultAck=r.battle.turn;server.queueSettlement(r);ok(!r.settlementQueued,"Two receipts cannot bypass a third participant");c.resultAck=r.battle.turn;server.queueSettlement(r);long until=System.currentTimeMillis()+3000;while(!r.settled&&System.currentTimeMillis()<until)Thread.sleep(10);ok(r.resultsReady(),"All receipts release one saved shared result");a.cardsReady=b.cardsReady=true;ok(!r.cardsOpen(),"Shared card board waits for all participants");c.cardsReady=true;ok(r.cardsOpen(),"All participants open the same board");
   ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);List<Future<Boolean>> answers=new ArrayList<>();for(var s:new GunnyServer.Session[]{a,c})answers.add(pool.submit(()->{start.await();try{store.claim(r.match,s.id,4,200);return true;}catch(IllegalArgumentException e){return false;}}));start.countDown();int wins=0;for(var f:answers)if(f.get())wins++;pool.shutdownNow();ok(wins==1,"Concurrent selection of the same card grants exactly one reward");String owner=store.rewards.keySet().iterator().next().substring(r.match.length()+1);int wallet=store.profile(owner).coins;int[] first=store.claim(r.match,owner,8,200);ok(first[0]==4&&first[1]==200&&store.profile(owner).coins==wallet,"Claim retry returns the committed card without paying twice");
  }System.out.println("All 6.7.0 social and shared-card server checks passed");
 }
}
