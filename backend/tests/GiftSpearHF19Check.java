import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Regression checks for HF19 gift capability/RPC/response and direct Sulan weapon unlock. */
public final class GiftSpearHF19Check {
 static int checks;
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);checks++;}
 static final class RpcProbe extends CloudStore {
  String called;RpcProbe(){super(true);}
  @Override Object request(String path,String method,String body,boolean admin){called=path;check(admin&&method.equals("POST"),"Admin RPC via POST");check(Json.num(Json.obj(Json.parse(body)),"p_type",-1)==1,"Gift type passed");return Map.of("rewards",List.of(List.of(3,25),List.of(10,1)));}
 }
 static Map<String,Object> finish(String id,int stage,int difficulty,int winner){return Map.of("match",UUID.randomUUID().toString(),"run",UUID.randomUUID().toString(),"stage",stage,"difficulty",difficulty,"winner",winner,"players",List.of(Map.of("id",id,"team",0)),"min",10,"max",100);}
 public static void main(String[] args)throws Exception {
  var probe=new RpcProbe();check(Arrays.equals(probe.giftOpen("test",1,"receipt"),new int[]{3,25,10,1}),"EXP reward decode");check(probe.called.equals("/rest/v1/rpc/gunny_gift_open_exp"),"Production uses EXP RPC");
  var store=new CloudStore.Memory();
  for(int diff=0;diff<3;diff++){
   String email="hf19-"+diff+"@test.local";store.register(email,"test-password-123","HF19 user "+diff);var p=store.profile(store.login(email,"test-password-123").userId);
   for(int stage:new int[]{1,2,3}){store.tribal("finish",finish(p.id,stage,diff,0));check((p.owned&256)==0&&p.vipSpears==0,"No spear in early stage");}
   store.tribal("finish",finish(p.id,4,diff,1));check((p.owned&256)==0&&p.vipSpears==0,"No spear on defeat");
   int bags=p.giftBags;var win=finish(p.id,4,diff,0);store.tribal("finish",win);check(((p.owned&256)!=0)==(diff==2)&&p.vipSpears==(diff==2?1:0),"Hard-only direct final victory award");check(p.giftBags==bags+1,"First victory grants normal bag only");
   int[] deck=store.tribalDeck(Json.str(win,"match"));check(Arrays.stream(deck).noneMatch(n->n==3),"No new spear cards");check(Arrays.stream(deck).filter(n->n==2).count()==(diff==2?1:0),"Wing odds unchanged");
   store.tribal("finish",win);check(p.giftBags==bags+1,"Retry first victory does not grant extra bag");
   var repeat=finish(p.id,4,diff,0);store.tribal("finish",repeat);check(p.giftBags==bags+(diff==2?3:2),"Existing owner receives normal bag and bonus bag");
   store.tribal("finish",repeat);check(p.giftBags==bags+(diff==2?3:2),"Retry repeat victory grants no extra bags");check(p.vipSpears==(diff==2?1:0),"No duplicate or low difficulty weapon");
   store.tribal("finish",finish(p.id,4,diff,1));check(p.giftBags==bags+(diff==2?3:2),"Existing owner receives no bags on defeat");
   p.giftBags=999998;store.tribal("finish",finish(p.id,4,diff,0));check(p.giftBags==999999,"Gift bag cap respected");
   if(diff==2){var equipped=store.equip(p.id,0,8);check(equipped.equipped==8,"Granted weapon can be equipped");}
  }
  store.register("gift-hf19@local.test","test-password-123","Gift HF19");var p=store.profile(store.login("gift-hf19@local.test","test-password-123").userId);p.giftBags=1;p.vipTickets=1;
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.startTcp("127.0.0.1",0,8);
   try(var socket=new Socket("127.0.0.1",server.tcpPort());var in=new DataInputStream(socket.getInputStream());var out=new DataOutputStream(socket.getOutputStream())){
    String health=new String(Tcp768Check.frame(in,out,0,"",null),StandardCharsets.UTF_8);check(health.contains("\"giftExpHF14\":1"),"TCP advertises gift EXP capability");
    String token=Tcp768Check.ok(Tcp768Check.frame(in,out,1,"",Tcp768Check.body("login",o->{o.writeUTF("gift-hf19@local.test");o.writeUTF("test-password-123");}))).readUTF();
    byte[] req=Tcp768Check.body("giftopenexp",o->{o.writeByte(1);o.writeUTF("hf19-bag");});
    byte[] reply=Tcp768Check.ok(Tcp768Check.frame(in,out,1,token,req)).readAllBytes();check(p.giftBags==0&&p.craftStones[0]>=20&&p.craftStones[0]<=70&&p.vipTickets==1,"EXP gift opened without minting legacy tickets");
    var bytes=new ByteArrayOutputStream();VipServer.write(new DataOutputStream(bytes),p);byte[] vip=bytes.toByteArray();check(reply.length>=vip.length&&Arrays.equals(Arrays.copyOfRange(reply,reply.length-vip.length,reply.length),vip),"EXP response includes current VIP state");
    int stones=p.craftStones[0];int[] cards=p.vipCards.clone();Tcp768Check.ok(Tcp768Check.frame(in,out,1,token,req));check(stones==p.craftStones[0]&&Arrays.equals(cards,p.vipCards),"Retry did not duplicate rewards");
    int before=p.vipCards[0];Tcp768Check.ok(Tcp768Check.frame(in,out,1,token,Tcp768Check.body("giftopenexp",o->{o.writeByte(2);o.writeUTF("hf19-ticket");})));check(p.vipTickets==0&&p.vipCards[0]==before+1,"Legacy ticket becomes VIP EXP card");
    Tcp768Check.ok(Tcp768Check.frame(in,out,1,token,Tcp768Check.body("giftopen",o->{o.writeByte(0);o.writeUTF("hf19-chest");})));check(p.giftLevel==2,"Legacy level chest still supported");
   }
  }
  System.out.println("PASS: "+checks+" HF19 gift/spear checks");
 }
}
