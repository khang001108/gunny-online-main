import java.io.*;
import java.net.*;
import java.nio.file.*;

/** Produces a real server volley for playback in the handset emulator. */
public final class ShotFramesFixture {
    public static void main(String[] args) throws Exception {
        try (var server = new GunnyServer(new InetSocketAddress("127.0.0.1", 0), Path.of("backend/assets"), new CloudStore.Memory())) {
            server.clock.shutdownNow();
            var host = ReturnRoomCheck.player("shooter");
            var guest = ReturnRoomCheck.player("observer");
            for (var player : new GunnyServer.Session[]{host, guest}) {
                for (String field : new String[]{"playerUi", "expansion", "campaign", "flock", "actors", "equipment", "items", "itemFx", "modern", "async", "events", "social", "matchmaking", "roomFix", "waitBot", "chunks", "replay"}) {
                    var property = GunnyServer.Session.class.getDeclaredField(field);
                    property.setBoolean(player, true);
                }
                player.itemVersion = 2;
            }
            var room = server.new Room(host, 0, 0, -1);
            room.members[1] = guest;
            host.room = guest.room = room.id;
            room.start();
            room.phase = 1;
            host.prefetch = true;
            room.battle.units[0].weapon = 3;
            String mode=args.length==0?"shot":args[0];
            if(mode.equals("walk")){
                room.chunkBegin(1);
                for(int tick=0;tick<36;tick++){room.battle.move(1);room.chunkFrame();}
                room.battle.pass();room.chunkFrame();
            }else{
                room.battle.units[0].angle=55;
                if(mode.equals("pow"))room.battle.pow=true;
                room.command(host,1,1,0);
                room.command(host,2,2,-(1+55*128+35));
                for(int tick=0;tick<1500&&!room.battle.pendingNext&&!room.battle.over;tick++)room.tick();
            }
            room.chunkFlush(true);
            var target = Path.of("client/build/"+mode+"-frames.bin");
            Files.createDirectories(target.getParent());
            try (var output = new DataOutputStream(Files.newOutputStream(target))) {
                output.writeUTF(GunnyServer.capabilities(false, true));
                var bytes = new ByteArrayOutputStream();
                server.lobby(new DataOutputStream(bytes), room, guest);
                output.writeInt(bytes.size()); output.write(bytes.toByteArray());
                output.writeInt(room.battle.turn);
                output.writeInt(room.chunks.size());
                for (int chunk = 0; chunk < room.chunks.size(); chunk++) {
                    bytes.reset();
                    room.chunkPacket(new DataOutputStream(bytes), guest, 0, room.battle.turn, chunk - 1, -1);
                    output.writeInt(bytes.size()); output.write(bytes.toByteArray());
                }
                output.writeInt(room.ground.craters.size());
            }
            if (!mode.equals("walk")&&room.ground.craters.isEmpty()) throw new AssertionError("Volley must hit terrain");
            System.out.println("PASS: captured authoritative "+mode+" frames");
        }
    }
}
