import java.util.*;
public final class HF45BalanceAdminCheck {
 static int checks;
 static void ok(boolean v,String m){if(!v)throw new AssertionError(m);checks++;}
 public static void main(String[] args)throws Exception{
  int[] cfg=BalanceSettings.DEFAULT.clone();ok(Arrays.equals(cfg,BalanceSettings.decode(BalanceSettings.encode(cfg))),"config round trip");
  for(int k:new int[]{2,3,6})for(int stat=0;stat<3;stat++){int prev=0;for(int rank=0;rank<=15;rank++){int v=BalanceSettings.equipmentStat(k,0,rank,stat,cfg);ok(v>=prev,"growth monotonic");prev=v;}}
  ok(BalanceSettings.equipmentStat(2,0,15,1,cfg)+BalanceSettings.equipmentStat(3,0,15,1,cfg)==3000,"armor and hat defense");ok(BalanceSettings.equipmentStat(6,0,15,2,cfg)==300,"glasses luck");
  Map<String,Object> gear=Map.of("level",70,"weapon",8,"weaponRank",15,"armorRank",15,"hatRank",15,"glassesRank",15,"wingHp",0);
  var a=BalanceAdmin.actor(gear,cfg);Battle.Unit u=BalancePvpCheck.gear(70,8,15,125);ok(a.get("attack")==Battle.baseDamage(u.weapon)+u.attackBonus&&a.get("defense")==u.defense&&a.get("luck")==u.luck&&a.get("hp")==u.maxHp,"preview matches authoritative gear");
  Map<String,Object> input=new HashMap<>(Map.of("attacker",gear,"defender",gear,"extra",2,"boost",2,"triple",1,"pow",0,"pvp",1,"impactPct",100));
  var p=BalanceAdmin.preview(input,cfg);System.out.println(Json.write(p));var h=Json.obj(p.get("hit"));ok(Json.num(h,"projectiles",0)==15,"15 hit combo");ok(Json.num(h,"normal",0)>1000,"no aggregate raw nerf");ok(Json.num(h,"remainingHp",0)>0,"equal turn guard");
  input.put("pvp",0);ok(Json.num(Json.obj(BalanceAdmin.preview(input,cfg).get("hit")),"turnCap",-1)==0,"PvE no guard");
  input.put("npcMode",2);input.put("npcSettings",Map.of("stage",6,"npc",15,"difficulty",2,"party",1));var npc=BalanceAdmin.preview(input,cfg);ok(Json.num(Json.obj(npc.get("attacker")),"attack",-1)==DungeonBalance.attack(6,15,2),"PvE actual boss stats");ok(Json.num(Json.obj(npc.get("hit")),"projectiles",0)==1,"boss ordinary hit without player buffs");
  int[] draft=cfg.clone();draft[2]=2000;ok(BalanceAdmin.actor(gear,draft).get("defense")==u.defense+500,"preview uses draft");ok(BalanceSettings.values[2]==1500,"preview does not mutate active");
  draft[2]=99999;try{BalanceSettings.validate(draft);throw new AssertionError("validation");}catch(IllegalArgumentException expected){checks++;}
  if(!CloudStore.env("GUNNY_BALANCE_FILE").isEmpty()){int[] saved=cfg.clone();saved[2]=1600;BalanceAdmin.operation("balance-save",Map.of("settings",BalanceAdmin.map(saved)));ok(BalanceSettings.values[2]==1500,"save pending until restart");BalanceAdmin.load();ok(BalanceSettings.values[2]==1600,"reload persisted config");ok(GunnyServer.capabilities(true,true).contains(BalanceSettings.encode(saved)),"health announces saved config");BalanceSettings.values=cfg;}
  System.out.println("PASS "+checks+" HF45 balance checks");
 }
}
