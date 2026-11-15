import java.net.*;
import java.nio.file.*;
import java.io.*;

public final class PlayerUiCheck {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);System.out.println("PASS: "+message);}
    public static void main(String[] args)throws Exception{
        var store=new CloudStore.Memory();
        store.register("one@ui.test","test-password-123","Gunner");
        boolean rejected=false;
        try{store.register("two@ui.test","test-password-123"," gunNER ");}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected&&store.accounts.size()==1,"Duplicate names reject registration without creating an account");
        store.register("two@ui.test","test-password-123","Other");
        String first=store.login("one@ui.test","test-password-123").userId;
        String second=store.login("two@ui.test","test-password-123").userId;
        rejected=false;try{store.appearance(second,"GUNNER",0,0,false,new int[5]);}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected&&store.profile(second).name.equals("Other"),"Renaming cannot take an existing name");
        store.appearance(first,"gunner",0,0,false,new int[5]);
        check(store.profile(first).name.equals("gunner"),"Owner can update their own capitalization");
        try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
            server.clock.shutdownNow();
            var host=ReturnRoomCheck.player("host");var guest=ReturnRoomCheck.player("guest");
            var room=server.new Room(host,0,0,-1);room.members[1]=guest;room.start();
            host.ready=true;guest.ready=false;room.lastActive=System.currentTimeMillis()-60000;
            room.tick();check(room.phase==3,"Loading never bypasses an unfinished player after a timeout");
            host.playerUi=true;
            var bytes=new ByteArrayOutputStream();server.lobby(new DataOutputStream(bytes),room,host);
            check((bytes.toByteArray()[bytes.size()-2]&255)==1,"Loading mask reports each player independently");
            guest.ready=true;room.tick();check(room.phase==1,"Match starts when every player reports loaded");
            var headers=new com.sun.net.httpserver.Headers();TcpGameServer.headers(headers,16777216,new int[]{0,1,2});
            check("1".equals(headers.getFirst("X-Gunny-PlayerUi")),"TCP gateway negotiates the player UI extension");
        }
    }
}
