import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** Exercises the actual HTTP protocol, auth boundary and authoritative rules. */
public final class ServerCheck {
    interface Writer {void write(DataOutputStream out)throws Exception;}
    static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();static String base;static boolean MODERN;
    static void ok(boolean yes,String message){if(!yes)throw new AssertionError(message);System.out.println("PASS: "+message);}
    static DataInputStream call(String token,String op,Writer writer)throws Exception {ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(GunnyServer.MAGIC);out.writeUTF(op);if(writer!=null)writer.write(out);out.close();HttpRequest.Builder request=HttpRequest.newBuilder(URI.create(base+"/api")).timeout(Duration.ofSeconds(10)).header("Content-Type","application/octet-stream");if(MODERN)request.header("X-Gunny-Client","2");if(token!=null)request.header("Authorization","Bearer "+token);byte[] body=HTTP.send(request.POST(HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray())).build(),HttpResponse.BodyHandlers.ofByteArray()).body();DataInputStream in=new DataInputStream(new ByteArrayInputStream(body));int status=in.readUnsignedByte();if(status!=0)throw new IOException(status+":"+in.readUTF());return in;}
    static String account(String email,String name)throws Exception {call(null,"register",out->{out.writeUTF(email);out.writeUTF("test-password-123");out.writeUTF(name);});DataInputStream in=call(null,"login",out->{out.writeUTF(email);out.writeUTF("test-password-123");});return in.readUTF();}
    static int create(String token,int ally,int bot)throws Exception {return call(token,"create",out->{out.writeByte(1);out.writeByte(ally);out.writeByte(bot);}).readInt();}
    static DataInputStream sync(String token,int seq,int command,int arg,boolean ready)throws Exception {return call(token,"sync",out->{out.writeInt(0);out.writeByte(0);out.writeBoolean(ready);out.writeByte(command==0?0:1);if(command!=0){out.writeInt(seq);out.writeByte(command);out.writeShort(arg);}});}
    static void failure(String token,String op,Writer writer,String message)throws Exception {boolean rejected=false;try{call(token,op,writer);}catch(IOException e){rejected=true;}ok(rejected,message);}
    public static void main(String[] args)throws Exception {
        CloudStore.Memory store=new CloudStore.Memory();try(GunnyServer server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
            base="http://127.0.0.1:"+server.port();ok(HTTP.send(HttpRequest.newBuilder(URI.create(base+"/health")).GET().build(),HttpResponse.BodyHandlers.ofString()).body().contains("\"ok\":true"),"Health endpoint reports the live server");
            for(String path:List.of("/","/auth/confirmed")){
                HttpResponse<String> page=HTTP.send(HttpRequest.newBuilder(URI.create(base+path)).GET().build(),HttpResponse.BodyHandlers.ofString());
                ok(page.statusCode()==200&&page.body().contains("Tiếp tục trong Gunny")&&!page.body().contains("localhost"),"Email redirect landing page is available at "+path);
                ok(page.headers().firstValue("Cache-Control").orElse("").equals("no-store")&&page.headers().firstValue("Referrer-Policy").orElse("").equals("no-referrer"),"Email landing does not cache the page or leak referrers");
                String hash=Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(GunnyServer.EMAIL_SCRIPT.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                ok(page.headers().firstValue("Content-Security-Policy").orElse("").contains("'sha256-"+hash+"'"),"Email landing permits only its own URL-cleanup script");
            }
            ok(HTTP.send(HttpRequest.newBuilder(URI.create(base+"/auth/confirmed")).method("HEAD",HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString()).body().isEmpty(),"Email landing supports HEAD without a response body");
            ok(HTTP.send(HttpRequest.newBuilder(URI.create(base+"/auth/confirmed")).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString()).statusCode()==405,"Email landing never accepts posted account tokens");
            ok(HTTP.send(HttpRequest.newBuilder(URI.create(base+"/unknown")).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode()==404,"Unknown routes remain 404");
            failure(null,"rooms",null,"Unauthenticated room access is rejected");String a=account("one@example.test","Người Một"),b=account("two@example.test","Người Hai");GunnyServer.Session sa=server.sessions.get(a),sb=server.sessions.get(b);
            failure(null,"login",out->{out.writeUTF("one@example.test");out.writeUTF("incorrect-password");},"Wrong password is rejected");
            ok(sa.profile.id!=null&&sa.profile.coins==0,"Login loads the server-owned account profile");
            sa.profile.shoppingMoney=1000;call(a,"buy",out->out.writeByte(3));call(a,"buy",out->out.writeByte(3));ok(sa.profile.coins==0&&sa.profile.shoppingMoney==550&&sa.profile.equipped==3,"Buying and re-equipping charge the server wallet once");failure(a,"buy",out->out.writeByte(9),"Insufficient funds cannot unlock a weapon");
            int id=create(a,0,-1);int firstId=id;call(b,"join",out->{out.writeInt(firstId);out.writeByte(1);});GunnyServer.Room room=server.rooms.get(id);ok(room.members[1]==sb,"Second account joins the opposite team in the same room");failure(b,"start",null,"Guest cannot start the host's room");call(a,"start",null);
            Thread.sleep(160);ok(room.phase==3&&room.battle.countdown==600,"Battle clock waits for clients to finish loading");sync(a,0,0,0,true);sync(b,0,0,0,true);Thread.sleep(100);ok(room.phase==1,"Both ready clients start the authoritative clock");
            sync(a,1,1,0,true);Thread.sleep(170);ok(room.battle.charging&&room.battle.units[0].power>0,"Held shot force advances on the server");int energy=room.battle.units[0].energy;sync(b,1,3,2,true);ok(room.battle.units[0].energy==energy&&room.battle.boost==0,"Other player cannot use buffs on the active player's turn");
            sync(a,2,2,0,true);Thread.sleep(350);ok(room.battle.shotSerial>0,"Release creates a projectile on the server");failure(a,"sethp",out->out.writeInt(999999),"Client cannot submit arbitrary HP or currency");
            call(b,"leave",null);call(a,"leave",null);id=create(a,1,6);int secondId=id;call(b,"join",out->{out.writeInt(secondId);out.writeByte(1);});call(a,"start",null);sync(a,0,0,0,true);sync(b,0,0,0,true);Thread.sleep(100);room=server.rooms.get(id);
            ok(room.count==4&&room.avatars[2].owner==null&&room.avatars[2].skin==GunnyServer.BOT_SKINS[6],"2v2 includes the selected allied bot with the correct outfit");
            sync(a,3,3,2,true);sync(a,3,3,2,true);ok(room.battle.boost==50&&room.battle.units[0].energy==150,"Retried command sequence cannot consume the same buff twice");
            synchronized(room){room.battle.units[0].rage=100;room.battle.units[0].energy=240;}
            sync(a,4,3,5,true);ok(room.battle.pow&&room.battle.units[0].rage==0,"POW spends all rage immediately on activation, before charging or firing");sync(a,5,1,0,true);Thread.sleep(150);int shots=room.battle.shotSerial;sync(a,6,2,-(1+47*128+127),true);ok(room.battle.charging&&room.powTicks==0,"Release cannot submit force above 100");sync(a,7,2,-(1+250*128+36),true);ok(room.battle.charging&&room.powTicks==0,"Release cannot submit an angle outside weapon limits");sync(b,2,2,-(1+47*128+36),true);ok(room.battle.charging&&room.powTicks==0,"An opponent cannot release the active player's selected shot");sync(a,8,2,-(1+47*128+36),true);ok(room.battle.units[0].angle==47&&room.battle.units[0].power==36,"Delayed release preserves the selected angle and force");ok(room.powTicks>0&&room.battle.shotSerial==shots,"POW release waits for the full cinematic before shooting");
            synchronized(room){for(int t=0;t<160;t++)room.tick();}ok(room.battle.shotSerial>shots&&room.battle.units[0].rage==0,"POW fires once after the server cinematic and consumes rage");
            synchronized(room){room.battle.ballCount=room.battle.bursts=room.battle.settle=0;room.battle.units[1].hp=0;room.battle.units[3].hp=0;room.battle.check();room.tick();}Thread.sleep(150);ok(room.settled&&store.profile(sa.id).coins==500&&store.profile(sa.id).wins==1,"Winning team reward is settled by the server");
            store.settle(room.match,0,room.participants);ok(store.profile(sa.id).coins==500&&store.profile(sa.id).wins==1,"Duplicate settlement cannot grant duplicate coins or wins");
            call(a,"logout",null);failure(a,"rooms",null,"Logout revokes the session token");ok(Json.str(Json.obj(Json.parse(Json.write(Map.of("name","Tên \"Việt\"\n")))),"name").equals("Tên \"Việt\"\n"),"Supabase JSON codec preserves Vietnamese and escaped text");
        }
        System.out.println("All online HTTP server checks passed");
    }
}
