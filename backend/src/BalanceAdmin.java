import java.nio.file.*;
import java.io.*;
import java.util.*;
/** Draft preview never mutates the live simulation. Saved settings activate at next startup. */
final class BalanceAdmin {
 static Path file(){String p=CloudStore.env("GUNNY_BALANCE_FILE");return Path.of(p.isEmpty()?"balance.properties":p).toAbsolutePath();}
 static int[] read()throws IOException{Path p=file();if(!Files.exists(p))return BalanceSettings.DEFAULT.clone();return BalanceSettings.decode(Files.readString(p).trim());}
 static void load()throws IOException{BalanceSettings.values=read();}
 static int number(Map<String,Object> m,String key,int min,int max){Object v=m.get(key);if(!(v instanceof Number))throw new IllegalArgumentException("Thiếu: "+key);double d=((Number)v).doubleValue();if(d!=(int)d||d<min||d>max)throw new IllegalArgumentException("Giá trị không hợp lệ: "+key);return (int)d;}
 static int[] config(Map<String,Object> m){if(m==null)throw new IllegalArgumentException("Thiếu cấu hình");int[] a=new int[BalanceSettings.KEYS.length];for(int i=0;i<a.length;i++)a[i]=number(m,BalanceSettings.KEYS[i],BalanceSettings.MIN[i],BalanceSettings.MAX[i]);return BalanceSettings.validate(a);}
 static Map<String,Object> map(int[] a){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<a.length;i++)m.put(BalanceSettings.KEYS[i],a[i]);return m;}
 static synchronized Object operation(String op,Map<String,Object> input)throws IOException{
  if(op.equals("balance"))return Map.of("active",map(BalanceSettings.values),"saved",map(read()),"defaults",map(BalanceSettings.DEFAULT),"min",map(BalanceSettings.MIN),"max",map(BalanceSettings.MAX));
  int[] cfg=config(Json.obj(input.get("settings")));
  if(op.equals("balance-save")){Path p=file();Files.createDirectories(p.getParent());Path t=Files.createTempFile(p.getParent(),"balance-",".tmp");try{Files.writeString(t,BalanceSettings.encode(cfg)+"\n");try{Files.move(t,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(t,p,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(t);}return Map.of("saved",map(cfg),"restartRequired",!Arrays.equals(cfg,BalanceSettings.values));}
  if(!op.equals("balance-preview"))throw new IllegalArgumentException("Sai thao tác");
  return preview(input,cfg);
 }
 static Map<String,Integer> actor(Map<String,Object> a,int[] cfg){int level=number(a,"level",1,70),weapon=number(a,"weapon",0,9),exp=Progression.experienceForLevel(level),wingHp=number(a,"wingHp",0,4000);int[] stats={Progression.growth(exp,0),Progression.growth(exp,1),Progression.growth(exp,2)};int gearDefense=0;int[] kinds={0,2,3,6};String[] keys={"weaponRank","armorRank","hatRank","glassesRank"};for(int k=0;k<kinds.length;k++){int rank=number(a,keys[k],0,15);for(int j=0;j<3;j++){int value=BalanceSettings.equipmentStat(kinds[k],weapon,rank,j,cfg);stats[j]+=value;if(j==1)gearDefense+=value;}}// Hair and face, equipped at base stats, as in a full separate-item set.
  for(int k=4;k<=5;k++){stats[0]+=ItemRules.attack(k,0);int d=ItemRules.defense(k,0);stats[1]+=d;gearDefense+=d;stats[2]+=ItemRules.luck(k,0);}
  return Map.of("attack",stats[0],"defense",stats[1],"luck",stats[2],"hp",Progression.baseHp(exp)+gearDefense+wingHp,"level",level,"energy",240+stats[2]);
 }
 static Map<String,Integer> npc(Map<String,Object> m){int stage=number(m,"stage",1,6),id=number(m,"npc",1,15),d=number(m,"difficulty",0,3),party=number(m,"party",1,3);return Map.of("attack",DungeonBalance.attack(stage,id,d),"defense",DungeonBalance.defense(stage,id,d),"luck",DungeonBalance.luck(stage,id,d),"hp",DungeonBalance.hp(stage,id,d,party),"level",DungeonBalance.level(stage,d),"energy",240);}
 static Map<String,Object> preview(Map<String,Object> input,int[] cfg){
  Map<String,Integer> a=actor(Json.obj(input.get("attacker")),cfg),b=actor(Json.obj(input.get("defender")),cfg);
  int extra=number(input,"extra",0,2)*2,boost=number(input,"boost",0,2)*50;
  boolean triple=number(input,"triple",0,1)==1,pow=number(input,"pow",0,1)==1,pvp=number(input,"pvp",0,1)==1;
  if(!pvp&&input.containsKey("npcMode")){int mode=number(input,"npcMode",0,2);if(mode!=0){Map<String,Integer> npc=npc(Json.obj(input.get("npcSettings")));if(mode==1){b=npc;}else{a=npc;extra=boost=0;triple=pow=false;}}}
  int distance=number(input,"impactPct",35,100),count=(1+extra)*(triple?3:1),raw=CombatBalance.projectileDamage(a.get("attack"),boost,triple,extra,pow,pvp)*distance/100;
  int normal=CombatBalance.mitigate(raw,b.get("defense"),a.get("luck"),cfg),crit=normal*150/100,chance=CombatBalance.critChance(a.get("luck"),cfg);
  boolean equal=pvp&&cfg[13]<100&&a.get("level")<=b.get("level")+5&&a.get("attack")*100<=b.get("attack")*125&&a.get("hp")*100<=b.get("hp")*125;
  int cap=equal?b.get("hp")*cfg[13]/100:Integer.MAX_VALUE;int low=Math.min(cap,normal*count),high=Math.min(cap,crit*count),mean=Math.min(cap,(int)((long)count*(normal*10000L+(crit-normal)*(long)chance)/10000));
  return Map.of("attacker",a,"defender",b,"hit",Map.ofEntries(Map.entry("projectiles",count),Map.entry("raw",raw),Map.entry("normal",normal),Map.entry("critical",crit),Map.entry("critBasisPoints",chance),Map.entry("penetrationPct",CombatBalance.penetration(a.get("luck"),cfg)),Map.entry("minTotal",low),Map.entry("maxTotal",high),Map.entry("estimateTotal",mean),Map.entry("remainingHp",Math.max(0,b.get("hp")-mean)),Map.entry("turnCap",equal?cap:0)));
 }
}
