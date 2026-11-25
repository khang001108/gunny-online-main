import java.net.*;import java.nio.file.Path;import java.util.*;
/** Economy invariants, derived equipment stats and all 20 ant respawns/stage transitions. */
public final class Expansion780Check {
 static int checks;static void check(boolean v,String text){if(!v)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static void reject(Runnable fn,String text){try{fn.run();throw new AssertionError(text);}catch(IllegalArgumentException expected){check(true,text);}}
 static GunnyServer.Session session(CloudStore.Memory store,String name,int level)throws Exception{String email=name+"@780.test";store.register(email,"test-password-123",name);var s=new GunnyServer.Session();s.id=store.login(email,"test-password-123").userId;s.profile=store.profile(s.id);s.profile.experience=Progression.experienceForLevel(level);s.profile.characterChosen=true;s.profile.shoppingMoney=999999;s.profile.stones=10000;s.sulan7912=s.tribal797=s.expansion=s.actors=s.live=s.equipment=s.async=s.chunks=s.social=s.events=s.modern=s.arsenal=true;s.expiry=System.currentTimeMillis()+3600000;s.lastSeen=System.currentTimeMillis();s.armed=true;return s;}
 static final class SlowRewardStore extends CloudStore {
  final java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);final Profile p=new Profile(UUID.randomUUID().toString(),"Slow reward");
  SlowRewardStore(){super(true);}
  int[] claim780(String match,String id,int pick,int card)throws Exception{entered.countDown();if(!release.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Fixture timed out");return new int[]{card,500,0,pick};}
  Profile profile(String id){return p;}
 }
 static void slowStorageDoesNotBlockPhysics()throws Exception{var store=new SlowRewardStore();try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){server.clock.shutdownNow();var s=new GunnyServer.Session();s.id=store.p.id;s.profile=store.p;s.expansion=true;var room=server.new Room(s,1,0,-1);room.kind=2;room.phase=2;room.shared=false;room.settled=true;room.ground=room.newGround(1);room.battle=new Battle(room.ground);room.battle.over=true;room.battle.winner=0;server.rooms.put(room.id,room);s.room=room.id;var response=new java.io.ByteArrayOutputStream();var claim=java.util.concurrent.CompletableFuture.runAsync(()->{try{Expansion780.handle(server,"claim780",s,new java.io.DataInputStream(new java.io.ByteArrayInputStream(new byte[]{0,0})),new java.io.DataOutputStream(response));}catch(Exception e){throw new RuntimeException(e);}});store.entered.await(2,java.util.concurrent.TimeUnit.SECONDS);try{var tick=java.util.concurrent.CompletableFuture.runAsync(server::tick);tick.get(300,java.util.concurrent.TimeUnit.MILLISECONDS);check(true,"Slow reward storage never blocks the shared simulation clock");}finally{store.release.countDown();}claim.get(2,java.util.concurrent.TimeUnit.SECONDS);}}
 public static void main(String[] args)throws Exception{
  for(int rank=0;rank<ForgeRules.MAX_RANK;rank++)check(ForgeRules.cost(rank)==250*(rank+1),"Forge rank "+rank+" has increasing 250-stone increments");
  int[] ranks=new int[130];for(int sex=0;sex<2;sex++)for(int kind=0;kind<7;kind++)for(int item=0;item<10;item++){int index=ForgeRules.index(sex,kind,item);check(index>=0&&index<130,"Forge index remains inside inventory");}
  ranks[ForgeRules.index(0,0,0)]=2;ranks[ForgeRules.index(0,1,0)]=3;ranks[ForgeRules.index(0,2,0)]=4;
  int[] b=ForgeRules.equipped(ranks,0,3,0,0,new int[5]);check(Arrays.equals(b,new int[]{215,15,15}),"Weapon grows attack; full suit grows all three stats");
  b=ForgeRules.equipped(ranks,0,5,0,0,new int[5]);check(Arrays.equals(b,new int[]{200,52,0}),"Separate clothing grows defense only");
  var unit=new Battle.Unit();unit.experience=Progression.experienceForLevel(20);ForgeRules.apply(unit,b,ranks);ItemRules.apply(unit,5);check(unit.attackBonus==300&&unit.defense==192&&unit.luck==120,"Level, equipment and forge bonuses combine exactly once");
  ItemRules.weapon(unit,3);check(unit.forgeAttack==0&&unit.forgeDefense==52,"Changing weapon replaces its forge contribution and preserves armor bonuses");
  var queen=new Battle.Unit();queen.npc=3;queen.x=380;queen.y=210;
  check(Battle.hitsUnit(queen,380,150)&&!Battle.hitsUnit(queen,420,150),"Queen collision covers her visible torso without a rectangular oversize hitbox");
  var person=new Battle.Unit();person.x=100;person.y=210;check(Battle.hitsUnit(person,107,198)&&!Battle.hitsUnit(person,109,198),"Human PVP collision remains unchanged");
  Battle.Ground flat=new Battle.Ground(){public boolean solid(int x,int y){return y>=210;}public int surface(int x,int y){return 210;}public void crater(int x,int y,int radius){}};
  var melee=new Battle(flat);melee.start(true);melee.explicitHumans=true;melee.active=1;melee.units[0].human=true;melee.units[0].x=100;melee.units[0].y=210;melee.units[1].npc=1;melee.units[1].human=false;melee.units[1].x=130;melee.units[1].y=210;melee.units[1].weapon=0;melee.units[1].attackBonus=120-Battle.baseDamage(0);for(int i=0;i<100&&melee.damage[1]==0;i++)melee.tick();check(melee.damage[1]==120,"An ant walks into bite range and deals authoritative melee damage");
  var ranged=new Battle(flat);ranged.start(true);ranged.seed=11;ranged.explicitHumans=true;ranged.active=1;ranged.wind=0;ranged.units[0].human=true;ranged.units[0].x=100;ranged.units[0].y=210;ranged.units[1].npc=3;ranged.units[1].human=false;ranged.units[1].x=380;ranged.units[1].y=210;ranged.units[1].weapon=9;ranged.units[1].attackBonus=300-Battle.baseDamage(9);for(int i=0;i<1200&&ranged.damage[1]==0;i++)ranged.tick();check(ranged.shotSerial>0&&ranged.damage[1]>0,"The Queen aims and damages a player using authoritative projectiles");
  var store=new CloudStore.Memory();var host=session(store,"Host",15);var guest=session(store,"Guest",20);var third=session(store,"Third",30);var low=session(store,"Low",14);
  int wallet=host.profile.shoppingMoney;store.economy780(host.id,"buy",Map.of("quantity",2),"buy-key");store.economy780(host.id,"buy",Map.of("quantity",2),"buy-key");check(host.profile.shoppingMoney==wallet-160&&host.profile.stones==10002,"Buy retries charge once at 80 per stone");
  reject(()->store.economy780(host.id,"buy",Map.of("quantity",3),"buy-key"),"Request key rejects changed quantity");reject(()->store.economy780(guest.id,"buy",Map.of("quantity",2),"buy-key"),"Request key cannot change account");
  var payload=Map.<String,Object>of("sex",0,"kind",0,"item",0,"rank",0,"progress",0);store.economy780(host.id,"forge",payload,"forge-key");store.economy780(host.id,"forge",payload,"forge-key");check(host.profile.stones==9752&&host.profile.forgeLevels[0]==1,"Forge retry upgrades once and costs 250 stones");reject(()->store.economy780(host.id,"forge",payload,"stale-key"),"Different key with stale rank cannot spend again");
  for(int kind:new int[]{1,4,5}){int badKind=kind;reject(()->store.economy780(host.id,"forge",Map.of("sex",0,"kind",badKind,"item",0,"rank",0,"progress",0),"restricted-"+badKind),"Forging rejects the restricted category "+kind);}
  reject(()->Dungeon780.eligible(low,0),"Lv 14 cannot enter Ant Cave");reject(()->Dungeon780.eligible(host,2),"Hard mode is locked before normal clear");
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();var r=server.new Room(host,6,2,-1,true);r.kind=3;r.map=6;r.difficulty=1;r.strict=true;r.members[2]=guest;r.members[4]=third;r.refreshWaiting();r.start();
   check(r.participants.size()==3&&r.participants.stream().allMatch(x->x.get("team").equals(0)),"Dungeon records three humans on one team");
   check(r.dungeonStage==1&&r.antSpawned==6&&r.battle.units[1].npc>0&&r.battle.units[3].npc>0,"First stage contains actual red/blue ant NPCs");
   Battle ants=r.battle;int red=AntCaveRules.firstAnt(ants,1),blue=AntCaveRules.firstAnt(ants,2);
   check(ants.units[0].x==240&&ants.units[2].x<240&&ants.units[4].x>240,"The whole party starts spread around center in stage one");
   int leftAnts=0,rightAnts=0,walking=0;
   for(int slot:AntCaveRules.ANTS)if(ants.units[slot].hp>0){if(ants.units[slot].x<240)leftAnts++;else rightAnts++;}
   while(AntCaveRules.antOpening(ants))ants.tick();
   for(int i=0;i<4;i++)ants.tick();
   for(int slot:AntCaveRules.ANTS)if(ants.units[slot].npc==1&&ants.units[slot].walk>0)walking++;
   check(leftAnts==3&&rightAnts==3&&walking>1,"Three ants spawn per side and same-colour ants advance together");
   check(AntCaveRules.nextActor(ants,red)==blue,"The blue group acts immediately after the red group");
   check(AntCaveRules.nextActor(ants,blue)==0&&AntCaveRules.nextActor(ants,0)==2&&AntCaveRules.nextActor(ants,2)==4&&AntCaveRules.nextActor(ants,4)==red,"Each ally takes a turn before the ants act again");
   for(int i=0;i<4;i++)ants.units[AntCaveRules.ANTS[i]].hp=0;
   ants.pendingNext=true;Dungeon780.progress(r);ants.pendingNext=false;
   check(AntCaveRules.count(r.antAlive)==8&&r.antSpawned==12,"A six-ant wave respects the ten-ant cap");
   check(r.battle.units[0].forgeAttack==100,"Server applies the account's equipped weapon forge rank");
   int waves=0;while(r.antKills<20&&waves++<12){Battle current=r.battle;for(int i:AntCaveRules.ANTS)current.units[i].hp=0;current.check();Dungeon780.progress(r);}
   check(r.antKills==20&&r.antSpawned==20,"Exactly twenty ant kills are required across respawns");
   check(r.dungeonStage==1&&r.battle.over&&r.battle.winner==0,"Twentieth kill finishes the first stage");
   var players=List.copyOf(r.participants);int coins=host.profile.coins;
   store.finishAntStage(r.match,0,players,1);
   check(!host.profile.dungeonClear&&host.profile.coins==coins,"Stage one gives EXP without unlocking Hard");
   int[] stageCard=store.claim780(r.match,host.id,0,8);
   check(stageCard[2]>=5&&stageCard[2]<=50,"Stage-one card gives reinforcement stones");
   reject(()->store.claim780(r.match,host.id,1,0),"Stage one rejects a second pick");
   reject(()->store.claim780(r.match,guest.id,0,9),"Stage one rejects a tenth card");
   r.phase=0;r.dungeonStage=2;r.resetDungeonResult();r.refreshWaiting();guest.armed=third.armed=true;r.start();
   check(r.battle.units[1].npc==3&&r.battle.units[1].hp==16000,"Normal Queen has 16000 HP");
   check(r.battle.units[0].x<160&&r.battle.units[2].x!=r.battle.units[0].x&&r.battle.units[4].x<160,"Stage two has separated spawn positions on the left");
   Battle boss=r.battle;int health=boss.units[0].hp;
   boss.units[2].human=false;boss.active=2;
   var friendly=boss.make(45,60);friendly.x=boss.units[0].x*256;friendly.y=Battle.targetY(boss.units[0])*256;
   boss.impact(friendly);
   check(health-boss.units[0].hp>0&&health-boss.units[0].hp<=60,"A cooperative bot blast does light friendly damage");
   boss.active=1;check(boss.make(45,60).weapon==AntCaveRules.QUEEN_ARROW,"The Queen fires her own arrow projectile");
   boss.units[1].hp=0;boss.check();check(!boss.over&&AntCaveRules.dying(boss),"The Queen has a defeat sequence");
   for(int i=0;i<80;i++)boss.tick();
   check(boss.over&&boss.winner==0,"Defeating the Queen completes the dungeon");
   String match=UUID.randomUUID().toString();store.finishDungeon780(match,0,players,1);
   check(host.profile.dungeonClear&&host.profile.coins==coins,"Normal boss win unlocks Hard without a currency reward");
   for(int pick=0;pick<2;pick++){int[] reward=store.claim780(match,host.id,pick,pick);check(reward[1]==0&&reward[2]>=5&&reward[2]<=50,"Dungeon card gives 5–50 stones only");check(Arrays.equals(reward,store.claim780(match,host.id,pick,pick)),"Dungeon reward retries preserve their original roll");}
   reject(()->store.claim780(match,host.id,2,2),"Third card is rejected");reject(()->store.claim780(match,guest.id,0,0),"A card cannot be claimed by two accounts");Dungeon780.eligible(host,2);
   var solo=server.new Room(host,6,2,-1,true);solo.kind=3;solo.map=6;solo.difficulty=2;solo.refreshWaiting();solo.start();check(solo.battle.units[2].hp==0&&solo.battle.units[4].hp==0,"Empty human seats do not become allied NPCs");check(Dungeon780.space(solo),"A cooperative party offers empty human seats");solo.closedSlots=20;check(!Dungeon780.space(solo),"Quick party search respects seats closed by the host");
   solo.phase=0;solo.dungeonStage=2;solo.resetDungeonResult();solo.refreshWaiting();solo.start();check(solo.battle.units[1].maxHp==21000,"Hard Queen is stronger than Normal");
  }
  slowStorageDoesNotBlockPhysics();
  System.out.println("All "+checks+" expansion 7.8.0 checks passed");
 }
}
