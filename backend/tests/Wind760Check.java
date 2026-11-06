import java.net.InetSocketAddress;
import java.nio.file.Path;

public class Wind760Check {
 static void ok(boolean value,String message){if(!value)throw new AssertionError(message);System.out.println("PASS: "+message);}
 static int exp(int level){return 25*(level-1)*level;}
 public static void main(String[] args)throws Exception{
  int previous=0;for(int level=1;level<=70;level++){int cap=Progression.windLimit(exp(level));ok(cap==Math.min(30,9+level)&&cap>=previous,"Level "+level+" wind cap "+cap);previous=cap;}
  Battle.Ground ground=new Battle.Ground(){public boolean solid(int x,int y){return y>=200;}public int surface(int x,int from){return 200;}public void crater(int x,int y,int radius){}};
  for(int cap:new int[]{0,10,11,20,30}){Battle battle=new Battle(ground);battle.windLimit=cap;battle.seed=1729;battle.start(true);ok(Math.abs(battle.wind)<=cap,"First turn respects cap "+cap);boolean negative=cap==0,positive=cap==0,zero=false;for(int i=0;i<5000;i++){int before=battle.wind;battle.next();okRange(battle.wind,cap);if(battle.wind<0)negative=true;if(battle.wind>0)positive=true;if(battle.wind==0)zero=true;if(battle.lastWind!=before)throw new AssertionError("Previous wind lost");}ok(negative&&positive&&zero,"Both directions and calm survive for cap "+cap);}
  for(int[] pair:new int[][]{{1,50},{50,1},{11,40},{30,70}}){CloudStore.Memory store=new CloudStore.Memory();store.register("a@wind.test","test-password-123","A");store.register("b@wind.test","test-password-123","B");store.profile(store.login("a@wind.test","test-password-123").userId).experience=exp(pair[0]);store.profile(store.login("b@wind.test","test-password-123").userId).experience=exp(pair[1]);try(GunnyServer server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){ServerCheck.base="http://127.0.0.1:"+server.port();String a=Equipment718ServerCheck.login("a@wind.test"),b=Equipment718ServerCheck.login("b@wind.test");int id=Equipment718ServerCheck.call(a,"create",out->{out.writeByte(0);out.writeByte(0);out.writeByte(-1);}).readInt();Equipment718ServerCheck.call(b,"join",out->{out.writeInt(id);out.writeByte(1);});Equipment718ServerCheck.call(a,"start",null);var room=server.rooms.get(id);int cap=Math.min(Progression.windLimit(exp(pair[0])),Progression.windLimit(exp(pair[1])));ok(room.battle.windLimit==cap&&Math.abs(room.battle.wind)<=cap,"Server uses lowest human level "+pair[0]+" / "+pair[1]);}}
  System.out.println("All wind 7.6.0 checks passed");
 }
 static void okRange(int wind,int cap){if(Math.abs(wind)>cap)throw new AssertionError("Wind exceeds cap: "+wind+" / "+cap);}
}
