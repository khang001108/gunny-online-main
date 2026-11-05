import java.net.*;import java.nio.file.*;
/** Cosmetic timers progress while async physics waits; no HP/turn/position changes. */
public class Effects752ServerCheck {
 static void ok(boolean b,String message){if(!b)throw new AssertionError(message);System.out.println("PASS: "+message);}
 public static void main(String[] args)throws Exception{CloudStore.Memory store=new CloudStore.Memory();store.register("a@752.test","test-password-123","A");try(GunnyServer server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
 ServerCheck.base="http://127.0.0.1:"+server.port();String token=Equipment718ServerCheck.login("a@752.test");int id=Equipment718ServerCheck.call(token,"create",out->{out.writeByte(1);out.writeByte(0);out.writeByte(-1);}).readInt();GunnyServer.Room room=server.rooms.get(id);server.sessions.get(token).async=true;synchronized(room){room.strict=false;room.start();room.phase=1;Battle b=room.battle;Battle.Unit u=b.units[0];int hp=u.hp,x=u.x,y=u.y,turn=b.turn,clock=b.countdown;u.landingTicks=32;u.flashTicks=36;u.attackTicks=8;for(int i=0;i<40;i++)room.tick();ok(u.landingTicks==0&&u.flashTicks==0&&u.attackTicks==0,"Idle async input cannot freeze landing/buff/attack effects");ok(u.hp==hp&&u.x==x&&u.y==y&&b.turn==turn&&b.countdown==clock,"Cosmetic aging does not advance authoritative physics");b.pendingNext=true;u.landingTicks=32;for(int i=0;i<40;i++)room.tick();ok(u.landingTicks==0&&b.pendingNext,"Landing fades while waiting for playback acknowledgement");}
 }System.out.println("All effects 7.5.2 server checks passed");}
}
