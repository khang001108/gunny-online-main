import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Server-only reward settings. Each claim takes one immutable configuration row. */
final class RewardSettings {
 static final String[] KEYS={"stoneMin","stoneMax","grade1","grade2","grade3","grade4","grade5","luckyBase","luckyMin","luckyMax","luckyCards","protectMin","protectMax","luckyChance","protectChance"};
 static final int[][] WEIGHTS={{65,25,8,2,0},{25,35,25,12,3},{5,15,30,35,15},{5,15,30,35,15}};
 static Map<String,Object> defaults(){Map<String,Object> m=new LinkedHashMap<>();for(int family=0;family<2;family++)for(int d=0;d<4;d++){Map<String,Object> r=new LinkedHashMap<>();int lo=family==0?3:new int[]{5,25,50,50}[d],hi=family==0?25:new int[]{50,100,150,150}[d];int pl=family==0?new int[]{0,0,5,10}[d]:new int[]{0,5,10,15}[d];int ph=family==0?new int[]{0,0,10,20}[d]:new int[]{0,10,15,20}[d];int[] w=WEIGHTS[d];int[] v={lo,hi,w[0],w[1],w[2],w[3],w[4],1,5,10,family==1&&d>=2?1:0,pl,ph,100,ph>0?100:0};for(int i=0;i<KEYS.length;i++)r.put(KEYS[i],v[i]);m.put((family==0?"ant":"tribal")+d,r);}m.put("gift",Map.of("stoneMin",10,"stoneMax",35,"vipChance",10));return m;}
 static Path file(){return BalanceAdmin.file().resolveSibling("rewards.json");}
 static Map<String,Object> validate(Map<String,Object> m){if(m==null)throw new IllegalArgumentException("Thiếu cấu hình thưởng");Map<String,Object> result=new LinkedHashMap<>();for(String key:defaults().keySet()){Map<String,Object> row=Json.obj(m.get(key));if(row==null)throw new IllegalArgumentException("Thiếu "+key);Map<String,Object> out=new LinkedHashMap<>();String[] fields=key.equals("gift")?new String[]{"stoneMin","stoneMax","vipChance"}:KEYS;for(String k:fields){int min=k.startsWith("stone")||k.startsWith("luckyM")?1:0,max=k.startsWith("grade")||k.equals("vipChance")?100:k.equals("luckyCards")?18:k.startsWith("stone")?300:100;out.put(k,BalanceAdmin.number(row,k,min,max));}if(n(out,"stoneMin")>n(out,"stoneMax"))throw new IllegalArgumentException("Min đá > Max: "+key);if(!key.equals("gift")){int sum=0;for(int j=1;j<=5;j++)sum+=n(out,"grade"+j);if(sum!=100||n(out,"protectMin")>n(out,"protectMax")||n(out,"luckyMin")>n(out,"luckyMax"))throw new IllegalArgumentException("Tổng tỷ lệ đá phải = 100%, min ≤ max: "+key);}result.put(key,out);}return result;}
 static int n(Map<String,Object> row,String key){return Json.num(row,key,0);}
 static volatile Map<String,Object> active=defaults();
 static synchronized void load()throws IOException{if(Files.exists(file())){active=validate(Json.obj(Json.parse(Files.readString(file()))));System.out.println("Reward settings loaded: "+file());}else System.out.println("Reward settings defaults (no saved file): "+file());}
 static synchronized Map<String,Object> save(Map<String,Object> draft)throws IOException{Map<String,Object> valid=validate(draft);Path p=file();Files.createDirectories(p.getParent());Path t=Files.createTempFile(p.getParent(),"rewards-",".tmp");try{Files.writeString(t,Json.write(valid));try{Files.move(t,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(t,p,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(t);}active=valid;return valid;}
 static Map<String,Object> row(boolean tribal,int difficulty){return Json.obj(active.get((tribal?"tribal":"ant")+Math.max(0,Math.min(3,difficulty))));}
 /** Freeze one configuration per result, including settlement retries and all card picks. */
 static Map<String,Object> forRoom(GunnyServer.Room r){synchronized(r){if(r.rewardConfig==null||!r.match.equals(r.rewardConfigMatch)){r.rewardConfig=Map.copyOf(row(TribalDungeon.is(r),r.difficulty));r.rewardConfigMatch=r.match;}return r.rewardConfig;}}
 static Map<String,Object> gift(){return Json.obj(active.get("gift"));}
 static int roll(Map<String,Object> r,String key,Random random){int lo=n(r,key+"Min"),hi=n(r,key+"Max");return lo+random.nextInt(hi-lo+1);}
 static int chance(Map<String,Object> r,String key,Random random){return random.nextInt(100)<n(r,key+"Chance")?1:0;}
 static int grade(Map<String,Object> r,int roll){int sum=0;for(int i=1;i<=5;i++){sum+=n(r,"grade"+i);if(roll<sum)return i;}throw new IllegalArgumentException("Invalid stone roll");}
}
