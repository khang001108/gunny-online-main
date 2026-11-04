import java.net.*;
import java.nio.file.*;

/** Standalone regression checks for modern room settings and old-client compatibility. */
public final class RoomSettingsCheck {
    public static void main(String[] args)throws Exception{
        try(GunnyServer server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),new CloudStore.Memory())){
            ServerCheck.base="http://127.0.0.1:"+server.port();ServerCheck.MODERN=true;
            String host=ServerCheck.account("settings-host@example.test","Chủ phòng"),guest=ServerCheck.account("settings-guest@example.test","Bạn bè");
            int id=ServerCheck.call(host,"create2",out->{out.writeByte(-1);out.writeByte(0);out.writeByte(-1);out.writeByte(12);}).readInt();GunnyServer.Room room=server.rooms.get(id);
            ServerCheck.ok(room.strict&&room.mapLimit==6&&room.map==-1,"Modern room waits without revealing a random map");
            ServerCheck.failure(host,"start",null,"A modern room cannot start without a human opponent");
            ServerCheck.call(guest,"join",out->{out.writeInt(id);out.writeByte(2);});ServerCheck.ok(room.map>=0&&room.map<6,"Joining resolves one shared map from all six maps");
            ServerCheck.call(host,"roomsettings",out->{out.writeByte(5);out.writeByte(8);});ServerCheck.ok(room.map==5&&room.seconds==8,"Host can choose the new sixth map and eight-second turns");
            ServerCheck.failure(guest,"roomsettings",out->{out.writeByte(3);out.writeByte(15);},"A guest cannot change room settings");
            ServerCheck.failure(host,"roomsettings",out->{out.writeByte(6);out.writeByte(12);},"Invalid map index is rejected");
            ServerCheck.failure(host,"roomsettings",out->{out.writeByte(3);out.writeByte(1);},"Unsupported time limit is rejected");
            ServerCheck.failure(host,"start",null,"Host waits until the opponent confirms ready");
            ServerCheck.call(guest,"ready",out->out.writeBoolean(true));ServerCheck.call(host,"roomsettings",out->{out.writeByte(5);out.writeByte(15);});ServerCheck.ok(!room.canStart()&&!server.sessions.get(guest).armed,"Changing map settings cancels stale ready states");ServerCheck.call(host,"roomsettings",out->{out.writeByte(5);out.writeByte(8);});ServerCheck.call(guest,"ready",out->out.writeBoolean(true));ServerCheck.call(host,"start",null);
            ServerCheck.ok(room.battle.turnLimit==240&&room.map==5,"Battle uses the selected map and time limit");
            ServerCheck.failure(host,"roomsettings",out->{out.writeByte(0);out.writeByte(15);},"Map cannot change after the battle starts");
            ServerCheck.MODERN=false;int old=ServerCheck.create(host,0,-1);GunnyServer.Room legacy=server.rooms.get(old);ServerCheck.ok(!legacy.strict&&legacy.mapLimit==3&&legacy.seconds==20,"Old clients retain their original three-map format and turn time");
            ServerCheck.MODERN=true;int closing=ServerCheck.call(host,"create2",out->{out.writeByte(-1);out.writeByte(0);out.writeByte(-1);out.writeByte(12);}).readInt();ServerCheck.call(guest,"join",out->{out.writeInt(closing);out.writeByte(2);});ServerCheck.call(host,"logout",null);ServerCheck.failure(guest,"sync",out->{out.writeInt(0);out.writeByte(0);out.writeBoolean(false);out.writeByte(0);},"Closed rooms return a structured error to the remaining client");
        }finally{ServerCheck.MODERN=false;}
        System.out.println("All modern room-settings HTTP checks passed");
    }
}
