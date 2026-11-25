import java.io.*;import java.net.*;import java.nio.file.*;import java.util.*;
/** Real TCP wire and exactly-once checks for the new gift response. */
public final class GiftVipExpHF14Check {
 static int checks;static void ok(boolean v,String why){if(!v)throw new AssertionError(why);checks++;}
 static int[] gift(DataInputStream in,CloudStore.Profile p)throws Exception{
  in.readUTF();in.skipNBytes(2+1+5+4+4+4+1);in.readInt();in.readInt();
  var bytes=new ByteArrayOutputStream();Expansion780.write(new DataOutputStream(bytes),p);
  ok(Arrays.equals(in.readNBytes(bytes.size()),bytes.toByteArray()),"Economy state matches server");
  int n=in.readUnsignedByte();int[] reward=new int[n*2];for(int i=0;i<n;i++){reward[i*2]=in.readUnsignedByte();reward[i*2+1]=in.readInt();}
  int[] vip=VipTcp790Check.state(in);ok(vip[0]==p.vipExp,"Immediate VIP EXP snapshot");for(int i=0;i<13;i++)ok(vip[i+3]==p.vipCards[i],"Immediate VIP card inventory");return reward;
 }
 static DataInputStream open(TribalTcpCheck.Player p,String op,int type,String key)throws Exception{return p.call(op,o->{o.writeByte(type);o.writeUTF(key);});}
 public static void main(String[] args)throws Exception{
  var store=new CloudStore.Memory();String email="gift14@tcp.test";store.register(email,"test-password-123","Gift14");var profile=store.profile(store.login(email,"test-password-123").userId);profile.giftBags=2;profile.vipTickets=1;
  // Guarantee a card drop in the actual TCP test without modifying production odds.
  long seed=0;for(;;seed++){Random r=new Random(seed);r.nextInt(51);if(r.nextInt(100)<10)break;}store.rewardRandom.setSeed(seed);
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.clock.shutdownNow();server.startTcp("127.0.0.1",0,4);
   try(var p=new TribalTcpCheck.Player(server.tcpPort(),email,TribalTcpCheck.FLAGS|134217728)){
    boolean rejected=false;try{open(p,"giftopen",1,"old-client");}catch(AssertionError e){rejected=e.getMessage().startsWith("1:");}ok(rejected&&profile.giftBags==2,"Old client blocked without consuming bag");
    int[] r=gift(open(p,"giftopenexp",1,"bag"),profile);ok(r.length==4&&r[2]>=10&&r[2]<=18&&r[3]==1&&profile.giftBags==1,"Bag grants a VIP EXP card");
    int card=r[2]-10;ok(profile.vipCards[card]==1,"Card inserted");ok(Arrays.equals(r,gift(open(p,"giftopenexp",1,"bag"),profile))&&profile.giftBags==1&&profile.vipCards[card]==1,"Retry does not duplicate");
    int[] used=VipTcp790Check.state(p.call("vipuse",o->{o.writeByte(card);o.writeUTF("use-card");}));ok(used[0]==(card+1)*100&&used[card+3]==0,"Using reward card actually increases VIP EXP");
    int[] legacy=gift(open(p,"giftopenexp",2,"legacy"),profile);ok(Arrays.equals(legacy,new int[]{10,1})&&profile.vipTickets==0&&profile.vipUntil==0&&profile.vipCards[0]==1,"Legacy ticket converts to VIP1");
    int[] level=gift(open(p,"giftopenexp",0,"level"),profile);ok(level[0]==0&&profile.giftLevel==2,"Level chest unchanged");
   }
  }
  System.out.println("PASS: "+checks+" HF14 real TCP gift, card inventory, use, retry and legacy conversion checks");
 }
}
