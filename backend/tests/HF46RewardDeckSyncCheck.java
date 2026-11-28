import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/** Run with GUNNY_BALANCE_FILE pointing to a temporary test directory. */
public final class HF46RewardDeckSyncCheck {
 static int checks;
 static void ok(boolean p,String m){if(!p)throw new AssertionError(m);checks++;}
 static Map<String,Object> settings(int amount,int grade){
  var all=RewardSettings.defaults();
  for(String key:all.keySet())if(!key.equals("gift")){
   var row=Json.obj(all.get(key));
   int n=amount+Integer.parseInt(key.substring(key.length()-1));
   row.put("stoneMin",n);row.put("stoneMax",n);
   for(int g=1;g<=5;g++)row.put("grade"+g,g==grade?100:0);
   row.put("luckyCards",2);row.put("luckyMin",3);row.put("luckyMax",3);
   row.put("luckyChance",100);row.put("luckyBase",2);
   row.put("protectChance",100);row.put("protectMin",4);row.put("protectMax",4);
  }
  return RewardSettings.validate(all);
 }
 static byte[] reveal(GunnyServer server,GunnyServer.Session u)throws Exception{
  var b=new ByteArrayOutputStream();DeckReveal.handle(server,u,new DataOutputStream(b));return b.toByteArray();
 }
 static void claim(GunnyServer server,GunnyServer.Session u,int pick,int card)throws Exception{
  Expansion780.handle(server,"claim780",u,new DataInputStream(new ByteArrayInputStream(new byte[]{(byte)pick,(byte)card})),new DataOutputStream(new ByteArrayOutputStream()));
 }
 public static void main(String[] args)throws Exception{
  var old=RewardSettings.active;
  var first=settings(7,5);var next=settings(31,2);
  var store=new CloudStore.Memory();var u=Expansion780Check.session(store,"SyncDeck",70);u.tribal=true;u.profile.dungeonClear=true;
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();
   for(int stage:new int[]{1,2,3,4,6})for(int d=0;d<4;d++){
    RewardSettings.active=first;
    var r=server.new Room(u,6,2,-1,true);r.kind=3;r.dungeonStage=stage;r.difficulty=d;r.phase=2;r.settled=true;r.ground=r.newGround(1);r.battle=new Battle(r.ground);r.battle.over=true;r.battle.winner=0;u.room=r.id;server.rooms.put(r.id,r);u.cardsReady=true;u.resultAck=r.battle.turn;u.lastSeen=System.currentTimeMillis();
    var players=List.<Map<String,Object>>of(Map.of("id",u.id,"team",0));
    var config=RewardSettings.forRoom(r);
    if(TribalDungeon.is(r))TribalDungeon.settle(server,r,players);else if(stage==1)store.finishAntStage(r.match,0,players,DungeonBalance.rewardTier(d));else store.finishDungeon780(r.match,0,players,DungeonBalance.rewardTier(d));
    RewardSettings.active=next;
    ok(RewardSettings.forRoom(r)==config,"saved snapshot stays for current result");
    ok(reveal(server,u)[0]==0,"cannot reveal before picks");
    int[] rare=TribalDungeon.is(r)?store.tribalDeck(r.match):new int[18];
    int picks=DungeonRewards.picks(3,stage),count=DungeonRewards.cards(3,stage),card=0;
    int protection=u.profile.protectionCharms,lucky=u.profile.luckyCharms;
    for(int pick=0;pick<picks;pick++){
     while(rare[card]!=0)card++;
     claim(server,u,pick,card);
     ok(r.cardCoins[card]==7+d&&r.cardTypes[card]==7,"actual stone uses same amount and grade");card++;
    }
    ok(u.profile.protectionCharms==protection+4,"protection once per stage");
    ok(u.profile.luckyCharms==lucky+2*picks,"lucky per claimed stone card");
    int[] before=u.profile.craftStones.clone();int charms=u.profile.luckyCharms,protect=u.profile.protectionCharms;
    byte[] bytes=reveal(server,u);var in=new DataInputStream(new ByteArrayInputStream(bytes));ok(in.readBoolean()&&in.readUnsignedByte()==count,"correct deck size");
    for(int i=0;i<count;i++){
     int type=in.readUnsignedByte(),amount=in.readUnsignedShort();
     ok(rare[i]==2?type==2&&amount==3:type==7&&amount==7+d,"configured reveal card "+i);
    }
    ok(Arrays.equals(bytes,reveal(server,u)),"reveal cached");
    ok(Arrays.equals(before,u.profile.craftStones)&&charms==u.profile.luckyCharms&&protect==u.profile.protectionCharms,"reveal grants nothing");
    r.resetDungeonResult();ok(RewardSettings.n(RewardSettings.forRoom(r),"stoneMin")==31+d,"next stage gets new settings");server.rooms.remove(r.id);
   }
   if(System.getenv("GUNNY_BALANCE_FILE")!=null){RewardSettings.save(first);RewardSettings.active=next;RewardSettings.load();ok(RewardSettings.active.equals(first),"reload file survives restart");}
  }finally{RewardSettings.active=old;}
  System.out.println("PASS "+checks+" configured reward deck checks");
 }
}
