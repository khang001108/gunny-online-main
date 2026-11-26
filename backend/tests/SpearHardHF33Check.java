import java.util.*;
public final class SpearHardHF33Check {
 static int checks;
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);checks++;}
 static Map<String,Object> match(String id,int stage,int diff,int winner){return Map.of("match",UUID.randomUUID().toString(),"run",UUID.randomUUID().toString(),"stage",stage,"difficulty",diff,"winner",winner,"players",List.of(Map.of("id",id,"team",0)),"min",10,"max",100);}
 public static void main(String[] args)throws Exception{
  var store=new CloudStore.Memory();int n=0;
  for(int stage=1;stage<=4;stage++)for(int diff=0;diff<3;diff++)for(int winner=0;winner<3;winner++)for(boolean owned:new boolean[]{false,true}){
   String email="hf33-"+(n++)+"@test.local";store.register(email,"test-password-123","HF33 user "+n);var p=store.profile(store.login(email,"test-password-123").userId);
   if(owned){p.owned|=256;p.vipSpears=1;p.equipped=8;}
   p.giftBags=10;boolean finalWin=stage==4&&winner==0,hard=finalWin&&diff==2;
   var req=match(p.id,stage,diff,winner);store.tribal("finish",req);
   check(((p.owned&256)!=0)==(owned||hard),"Ownership");check(p.vipSpears==(owned||hard?1:0),"Spear count");check(p.giftBags==10+(finalWin?1:0)+(hard&&owned?1:0),"Bag amount");
   check(!owned||p.equipped==8,"Existing equip kept");check(Arrays.stream(store.tribalDeck(Json.str(req,"match"))).noneMatch(x->x==3),"No spear cards");
   int bags=p.giftBags;store.tribal("finish",req);check(p.giftBags==bags&&p.vipSpears==(owned||hard?1:0),"Idempotent finish");
  }
  System.out.println("PASS: "+checks+" HF33 checks");
 }
}
