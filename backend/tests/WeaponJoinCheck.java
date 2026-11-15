import java.net.*;
import java.nio.file.*;

public final class WeaponJoinCheck {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);System.out.println("PASS: "+message);}
    static void denied(TcpGameplayCheck.Player player,String operation,Tcp768Check.Writer body)throws Exception{
        boolean rejected=false;
        try{player.call(operation,body);}catch(AssertionError error){rejected=error.getMessage().contains("vũ khí");}
        check(rejected,"Server blocks unarmed entry through "+operation);
    }
    public static void main(String[] args)throws Exception{
        var store=new CloudStore.Memory();
        store.register("host@weapon.test","test-password-123","Armed Host");
        store.register("guest@weapon.test","test-password-123","Guest");
        try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
            server.clock.shutdownNow();server.startTcp("127.0.0.1",0,8);
            try(var host=new TcpGameplayCheck.Player(server.tcpPort(),"host@weapon.test");var guest=new TcpGameplayCheck.Player(server.tcpPort(),"guest@weapon.test")){
                int roomId=host.call("quick",out->out.writeByte(1)).readInt();
                var session=server.sessions.get(guest.token);var profile=session.profile;
                profile.owned=profile.wornMask=0;
                denied(guest,"join",out->{out.writeInt(roomId);out.writeByte(1);out.writeUTF("");});
                denied(guest,"quick",out->out.writeByte(1));
                check(session.room==0&&server.rooms.get(roomId).members[1]==null,"Rejected guest does not enter or consume the host's seat");
                session.inviteRoom=roomId;session.inviteTeam=1;
                denied(guest,"inviteanswer",out->out.writeBoolean(true));
                check(!guest.call("inviteanswer",out->out.writeBoolean(false)).readBoolean(),"Unarmed player can still decline an invitation");
                profile.owned=1;profile.wornMask=0;
                denied(guest,"join",out->{out.writeInt(roomId);out.writeByte(1);out.writeUTF("");});
                check(!GunnyServer.hasWeapon(profile),"Owning a weapon without equipping it is insufficient");
                profile.experience=Progression.experienceForLevel(20);
                boolean blocked=false;try{Dungeon780.eligible(session,0);}catch(IllegalArgumentException expected){blocked=expected.getMessage().contains("vũ khí");}
                check(blocked,"Dungeon entry also requires equipped weapon");
                profile.wornMask=1;
                check(guest.call("join",out->{out.writeInt(roomId);out.writeByte(1);out.writeUTF("");}).readInt()==roomId,"Guest can join after equipping an owned weapon");
            }
        }
    }
}
