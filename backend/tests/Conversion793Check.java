import java.io.*;
public final class Conversion793Check {
 static int checks;
 static void ok(boolean v){if(!v)throw new AssertionError("conversion check "+checks);checks++;}
 public static void main(String[] args)throws Exception{
  var store=new CloudStore.Memory();var s=Expansion780Check.session(store,"Convert793",70);var p=s.profile;p.shoppingMoney=0;p.vipExp=9500;
  for(int i=0;i<13;i++){p.vipCards[i]=2;int before=p.shoppingMoney,gain=i==12?9500:(i+1)*100;store.vip(s.id,"use",i,1,"card"+i);ok(p.shoppingMoney==before+gain&&p.vipCards[i]==1&&p.vipExp==9500);store.vip(s.id,"use",i,1,"card"+i);ok(p.shoppingMoney==before+gain&&p.vipCards[i]==1);}
  p.vipExp=9450;p.vipCards[0]=1;int before=p.shoppingMoney;store.vip(s.id,"use",0,1,"overflow");ok(p.vipExp==9500&&p.shoppingMoney==before+50);
  for(int i=0;i<5;i++){if(i==0)p.stones=2;else p.craftStones[i-1]=2;before=p.shoppingMoney;store.vip(s.id,"use",13+i,1,"stone"+i);ok(p.shoppingMoney==before+100*(1<<i));ok((i==0?p.stones:p.craftStones[i-1])==1);store.vip(s.id,"use",13+i,1,"stone"+i);ok((i==0?p.stones:p.craftStones[i-1])==1&&p.shoppingMoney==before+100*(1<<i));}
  p.shoppingMoney=999999;p.vipCards[0]=1;p.stones=1;for(int item:new int[]{0,13}){boolean denied=false;try{store.vip(s.id,"use",item,1,"full"+item);}catch(IllegalArgumentException expected){denied=true;}ok(denied&&p.vipCards[0]==1&&p.stones==1&&p.shoppingMoney==999999);}
  p.shoppingMoney=0;store.vip(s.id,"use",13,1,"wire");s.vip=true;try(var server=new GunnyServer(new java.net.InetSocketAddress("127.0.0.1",0),java.nio.file.Path.of("backend/assets"),store)){var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeByte(13);out.writeUTF("wire");var answer=new ByteArrayOutputStream();VipServer.handle(server,s,"vipuse",new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),new DataOutputStream(answer));ok(answer.size()>150);ok(p.shoppingMoney==100&&p.stones==0);}
  System.out.println("PASS "+checks+" conversion/MAX/overflow/grades/retry/wallet/wire checks");
 }
}
