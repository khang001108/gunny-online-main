import java.util.*;
public final class DungeonCharmsHF30Check {
 static int checks;
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
 public static void main(String[] args)throws Exception{
  var store=new CloudStore.Memory();store.register("hf30@local.test","test-password-123","HF30 test");var p=store.profile(store.login("hf30@local.test","test-password-123").userId);
  Set<Integer> amounts=new HashSet<>();
  for(int i=0;i<320;i++){
   int diff=i%3,stage=i%4+1;String match=UUID.randomUUID().toString();
   var finish=Map.<String,Object>of("match",match,"run",UUID.randomUUID().toString(),"stage",stage,"difficulty",diff,"winner",0,"players",List.of(Map.of("id",p.id,"team",0)),"min",10,"max",100);
   p.wings=i%2;p.wingEquipped=p.wings>0;p.wingOwned=7;p.equippedWing=2;
   int wings=p.wings,vipWings=p.wingOwned,equip=p.equippedWing;boolean legacy=p.wingEquipped;
   store.tribal("finish",finish);int[] deck=store.tribalDeck(match);int rare=-1,n=0;for(int j=0;j<deck.length;j++)if(deck[j]==2){rare=j;n++;}
   check(n==(stage==4&&diff==2?1:0),"Rare odds unchanged");
   // Exercise rare path repeatedly even in a historical deck.
   if(rare<0){rare=0;((int[])store.tribalStore.matches.get(match).get("deck"))[0]=2;}
   int before=p.luckyCharms;var req=Map.<String,Object>of("match",match,"user",p.id,"pick",0,"card",rare);var r=store.tribal("claim",req);int amount=Json.num(r,"amount",0);amounts.add(amount);
   check(Json.num(r,"type",0)==2&&amount>=5&&amount<=20,"Lucky charm range");
   check(p.luckyCharms==before+amount,"Rare total equals displayed amount");
   check(p.wings==wings&&p.wingEquipped==legacy&&p.wingOwned==vipWings&&p.equippedWing==equip,"Wing state preserved");
   check(store.tribal("claim",req).equals(r)&&p.luckyCharms==before+amount,"Retry returns same receipt without double grant");
   int card=(rare+1)%18;int beforeStone=p.luckyCharms;store.tribal("claim",Map.of("match",match,"user",p.id,"pick",1,"card",card));check(p.luckyCharms==beforeStone+1,"Other reward still gives one charm");
  }
  check(amounts.size()==16,"All sixteen quantities appeared");
  System.out.println("PASS: "+checks+" HF30 reward checks");
 }
}
