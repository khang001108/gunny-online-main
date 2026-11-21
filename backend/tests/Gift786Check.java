import java.util.*;import java.util.concurrent.*;
public class Gift786Check {
 static int checks;static void check(boolean b,String text){if(!b)throw new AssertionError(text);checks++;}
 static void reject(Runnable action){try{action.run();throw new AssertionError("Expected rejection");}catch(IllegalArgumentException good){checks++;}}
 public static void main(String[] args)throws Exception{
  var store=new CloudStore.Memory();store.register("gift@local.test","test-password-123","Gift user");var p=store.profile(store.login("gift@local.test","test-password-123").userId);
  int[] first=store.giftOpen(p.id,0,"level-1");check(p.giftLevel==2&&p.stones>=50&&p.stones<=100&&p.coins==100,"Level 1 chest contents");int stone=p.stones;check(Arrays.equals(first,store.giftOpen(p.id,0,"level-1"))&&p.stones==stone,"Chest replay does not duplicate");reject(()->store.giftOpen(p.id,0,"too-low"));reject(()->store.giftOpen(p.id,1,"level-1"));
  p.experience=Progression.experienceForLevel(70);for(int lv=2;lv<=70;lv++){store.giftOpen(p.id,0,"level-"+lv);check(p.giftLevel==lv+1,"Next level chest");}reject(()->store.giftOpen(p.id,0,"level-end"));
  p.giftBags=1;var pool=Executors.newFixedThreadPool(6);List<Future<int[]>> calls=new ArrayList<>();for(int i=0;i<12;i++)calls.add(pool.submit(()->store.giftOpen(p.id,1,"same-bag")));for(var call:calls)check(Arrays.equals(calls.get(0).get(),call.get()),"Concurrent reward identical");pool.shutdownNow();check(p.giftBags==0&&p.craftStones[0]>=20&&p.craftStones[0]<=70,"One bag consumed and Lv2 credited");reject(()->store.giftOpen(p.id,1,"empty"));
  p.vipTickets=2;store.giftOpen(p.id,2,"vip-a");long time=p.vipUntil;store.giftOpen(p.id,2,"vip-a");check(p.vipTickets==1&&p.vipUntil==time,"VIP retry idempotent");store.giftOpen(p.id,2,"vip-b");check(p.vipTickets==0&&p.vipUntil==time+86400,"VIP duration stacks");
  String match=UUID.randomUUID().toString();var players=List.<Map<String,Object>>of(Map.of("id",p.id,"team",0));store.finishDungeon780(match,0,players,0);store.finishDungeon780(match,0,players,0);check(p.giftBags==1,"Final Ant victory awards bag once");store.finishAntStage(UUID.randomUUID().toString(),0,players,0);check(p.giftBags==1,"Intermediate Ant stage does not award final bag");
  var payload=Map.<String,Object>of("match",UUID.randomUUID().toString(),"run",UUID.randomUUID().toString(),"stage",4,"difficulty",0,"winner",0,"players",players,"min",10,"max",100);store.tribal("finish",payload);store.tribal("finish",payload);check(p.giftBags==2,"Final Tribal victory awards one bag exactly once");
  int[] ranks=new int[130];for(int kind:new int[]{2,3,6}){for(int rank=0;rank<=15;rank++){ranks[ForgeRules.index(0,kind,0)]=rank;check(ForgeRules.bonus(ranks,0,kind,0)==13*rank,"Defense increases 13 per rank");}}
  check(ForgeRules.bonus(ranks,0,2,0)==195,"Armor +15 gives 195");System.out.println("PASS: "+checks+" gift, concurrency, expiry and defense checks");
 }
}
