import java.io.*;
import java.net.*;
import java.nio.file.*;

public final class ChunkPrefetchCheck {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("PASS: " + message);
    }
    static DataInputStream packet(GunnyServer.Room room, GunnyServer.Session player, int completed, int cached) throws Exception {
        var bytes = new ByteArrayOutputStream();
        room.chunkPacket(new DataOutputStream(bytes), player, 0, room.battle.turn, completed, cached);
        return new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    }
    public static void main(String[] args) throws Exception {
        try (var server = new GunnyServer(new InetSocketAddress("127.0.0.1", 0), Path.of("backend/assets"), new CloudStore.Memory())) {
            server.clock.shutdownNow();
            var headers=new com.sun.net.httpserver.Headers();
            TcpGameServer.headers(headers,8388608,new int[]{0,1,2});
            check("1".equals(headers.getFirst("X-Gunny-Prefetch")), "TCP gateway preserves the receive-ahead capability");
            var host = ReturnRoomCheck.player("prefetch-host");
            var guest = ReturnRoomCheck.player("prefetch-guest");
            host.async = guest.async = true;
            host.prefetch = guest.prefetch = true;
            var room = server.new Room(host, 0, 0, -1);
            room.members[1] = guest;
            room.start();room.phase = 1;
            room.chunkBegin(1);
            for (int tick = 0; tick < 17; tick++) { room.battle.units[0].x++;room.chunkFrame(); }
            room.chunkFlush(true);
            check(room.chunks.size() == 4 && room.chunkTerminal, "Server streams six-frame segments and a valid terminal segment");
            for (int id = 0; id < 4; id++) {
                var reply = packet(room, guest, -1, id - 1);
                check(reply.readByte() == 4 && reply.readInt() == id, "Receive next packet before previous playback completes: " + id);
            }
            check(packet(room, guest, -1, -5).readByte() == 3, "Full client queue gets a heartbeat");
            var legacy = ReturnRoomCheck.player("legacy-viewer");
            var first = packet(room, legacy, -1, -1);
            check(first.readByte() == 4 && first.readInt() == 0, "Legacy viewer still receives its first segment");
            check(packet(room, legacy, -1, 0).readByte() == 3, "Legacy cached acknowledgement retains existing semantics");
            room.battle.pendingNext = true;
            host.resolution = room.battle.turn;
            int turn = room.battle.turn;
            room.tick();
            check(room.battle.turn == turn, "Downloaded segments do not release the turn barrier");
            guest.resolution = turn;room.tick();
            check(room.battle.turn == turn + 1, "Both completed playbacks release the next turn");
            room.chunkBegin(3);
            for(int i=0;i<5;i++)room.chunkFrame();
            check(room.chunks.size()==1,"First PvP shot segment published after six frames");
            var firstShot=new DataInputStream(new ByteArrayInputStream(room.chunks.get(0)));
            check(firstShot.readUnsignedByte()==3&&!firstShot.readBoolean()&&firstShot.readUnsignedShort()==6,"First shot segment has correct type and frame count");
            for(int i=0;i<11;i++)room.chunkFrame();
            check(room.chunks.size()==1,"Subsequent shot segment retains normal batching");
            room.chunkFrame();
            var nextShot=new DataInputStream(new ByteArrayInputStream(room.chunks.get(1)));
            check(nextShot.readUnsignedByte()==3&&!nextShot.readBoolean()&&nextShot.readUnsignedShort()==12,"Subsequent PvP shot segment retains twelve frames");
        }
    }
}
