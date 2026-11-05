import java.net.*;import java.nio.file.*;import java.util.*;
public final class Progression719ServerCheck {
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS: "+s);}
 static void deny(Runnable r){try{r.run();throw new AssertionError("Expected denial");}catch(IllegalArgumentException expected){}}
 public static void main(String[] args)throws Exception{
  CloudStore.Memory store=new CloudStore.Memory();var a=Matchmaking680Check.person(store,7190);var b=Matchmaking680Check.person(store,7191);
  ok(a.profile.coins==0&&a.profile.shoppingMoney==0&&a.profile.owned==1,"New real account starts with no offline currency or unlocked weapons");
  for(int kind=0;kind<3;kind++){String id=UUID.randomUUID().toString();int before=a.profile.coins,other=b.profile.coins;var players=List.of(Map.<String,Object>of("id",a.id,"team",0),Map.<String,Object>of("id",b.id,"team",1));store.settle(id,0,players,kind);int base=kind==2?1000:500,min=kind==2?500:200,max=kind==2?2000:1000;
   ok(a.profile.coins==before+base&&b.profile.coins==other,"Correct winner-only base payout for mode "+kind);store.settle(id,0,players,kind);ok(a.profile.coins==before+base,"Duplicate settlement pays once");deny(()->store.claim(id,b.id,0,min));deny(()->store.claim(id,a.id,0,min-1));deny(()->store.claim(id,a.id,0,max+1));store.claim(id,a.id,0,max);store.claim(id,a.id,8,min);ok(a.profile.coins==before+base+max,"One card per winning account; retries do not pay again");
  }
  try(GunnyServer server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){var r=Matchmaking680Check.room(server,a,null);var target=Matchmaking680Check.room(server,b,null);deny(()->{try{server.matchmaker.start(a,target.id,"");}catch(RuntimeException e){throw e;}catch(Exception e){throw new RuntimeException(e);}});ok(r.phase==0&&target.phase==0,"Disabled room challenge cannot move either roster");}
  System.out.println("All progression 7.1.9 server checks passed");
 }
}
