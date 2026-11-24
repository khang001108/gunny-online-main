import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public final class MatchReturnRegressionCheck {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("PASS: " + message);
    }

    static void finish(GunnyServer.Room room) {
        room.battle.over = true;
        room.battle.winner = 2;
        room.phase = 2;
        room.settled = true;
    }

    public static void main(String[] args) throws Exception {
        var store = new CloudStore.Memory();
        try (var server = new GunnyServer(new InetSocketAddress("127.0.0.1", 0), Path.of("backend/assets"), store)) {
            server.clock.shutdownNow();
            for (int partySize : new int[]{1, 2, 3}) {
                var players = new GunnyServer.Session[partySize * 2];
                for (int i = 0; i < players.length; i++) {
                    players[i] = Matchmaking680Check.person(store, partySize * 100 + i);
                }
                var first = server.new Room(players[0], 1, 2, -1, partySize == 3);
                var second = server.new Room(players[partySize], 1, 2, -1, partySize == 3);
                for (var room : List.of(first, second)) {
                    room.customAllies = room.strict = true;
                    room.kind = partySize == 3 ? 2 : 0;
                    server.rooms.put(room.id, room);
                }
                for (int i = 0; i < partySize; i++) {
                    first.members[i * 2] = players[i];
                    second.members[i * 2] = players[partySize + i];
                    players[i].room = first.id;
                    players[partySize + i].room = second.id;
                    players[i].armed = players[partySize + i].armed = true;
                }
                first.refreshWaiting(); second.refreshWaiting();
                server.matchmaker.merge(first, second);
                finish(first);
                first.returnAfterMatch(players[partySize]);
                var secondHome = server.requireRoom(players[partySize]);
                check(secondHome.host == players[partySize], "Opponent retains their own host after " + partySize + "v" + partySize);
                check(Arrays.stream(secondHome.members).noneMatch(s -> s == players[0]), "Opposing teams return to separate waiting rooms");
                if (partySize > 1) check(!secondHome.canStart(), "Party waits for teammates still viewing results");
                for (var player : players) if (player.room == first.id) first.returnAfterMatch(player);
                var firstHome = server.requireRoom(players[0]);
                check(firstHome.host == players[0] && firstHome != secondHome, "Both original party leaders keep host rights");
                for (int i = 0; i < partySize; i++) {
                    check(players[i].room == firstHome.id && players[partySize + i].room == secondHome.id, "Each ally returns to their own party");
                }
                check(!firstHome.opponents() && !secondHome.opponents(), "No old opponent survives into next search");
            }

            var host = Matchmaking680Check.person(store, 999);
            var room = Matchmaking680Check.room(server, host, null);
            room.allyBot = 2;
            room.refreshWaiting();
            server.matchmaker.start(host, 0, "");
            server.matchmaker.voteBot(host);
            check(room.count == 4, "Ally bot creates a real 2v2 battle");
            finish(room);
            room.returnAfterMatch(host);
            var home = server.requireRoom(host);
            check(home.allyBot == 2 && home.enemyBot == -1 && !home.botMatch, "Only invited ally bot returns with the host");
            var bytes = new ByteArrayOutputStream();
            server.social.handle("slot", host, new DataInputStream(new ByteArrayInputStream(new byte[]{2})), new DataOutputStream(bytes));
            check(home.allyBot == -1 && !home.opponents(), "Kicking ally bot clears the old opponent state");
            server.matchmaker.start(host, 0, "");
            server.matchmaker.voteBot(host);
            check(home.count == 2 && home.battle.units[1].hp > 0, "Next bot match becomes exactly 1v1");


            var teamHost = Matchmaking680Check.person(store, 1100);
            var botTeam = server.new Room(teamHost, 1, 2, -1, true);
            botTeam.kind = 2;botTeam.strict = true;teamHost.room = botTeam.id;
            botTeam.selectedBots[2] = 1;botTeam.selectedBots[4] = 5;
            server.rooms.put(botTeam.id, botTeam);botTeam.refreshWaiting();
            check(server.matchmaker.size(botTeam) == 3, "3v3 counts selected ally bots toward the party size");
            server.matchmaker.start(teamHost, 0, "");server.matchmaker.voteBot(teamHost);
            check(botTeam.battle.units[2].hp > 0 && botTeam.battle.units[4].hp > 0, "Chosen 3v3 allies enter combat alive");
            check(botTeam.battle.units[1].hp > 0 && botTeam.battle.units[3].hp > 0 && botTeam.battle.units[5].hp > 0, "3v3 bot match has three active opponents");


            server.rooms.clear();
            store.register("solo-a@regression.test", "test-password-123", "Solo A");
            store.register("solo-b@regression.test", "test-password-123", "Solo B");
            server.startTcp("127.0.0.1", 0, 8);
            for (int partySize : new int[]{1, 2}) {
                var clients = new ArrayList<TcpGameplayCheck.Player>();
                try {
                    for (int i = 0; i < partySize * 2; i++) {
                        String email = "private-" + partySize + "-" + i + "@regression.test";
                        store.register(email, "test-password-123", "Private " + partySize + " " + i);
                        clients.add(new TcpGameplayCheck.Player(server.tcpPort(), email));
                    }
                    var a = clients.get(0);
                    var b = clients.get(partySize);
                    int aId = a.call("create3", out -> {
                        out.writeByte(1);out.writeByte(2);out.writeByte(-1);out.writeByte(12);
                        out.writeUTF("First party");out.writeUTF("");
                    }).readInt();
                    int bId = b.call("create3", out -> {
                        out.writeByte(1);out.writeByte(2);out.writeByte(-1);out.writeByte(12);
                        out.writeUTF("Second party");out.writeUTF("");
                    }).readInt();
                    if (partySize == 2) {
                        clients.get(1).call("join", out -> {out.writeInt(aId);out.writeByte(0);out.writeUTF("");});
                        clients.get(3).call("join", out -> {out.writeInt(bId);out.writeByte(0);out.writeUTF("");});
                        clients.get(1).call("ready", out -> out.writeBoolean(true));
                        clients.get(3).call("ready", out -> out.writeBoolean(true));
                    }
                    var waiting = a.call("start", null);
                    check(waiting.readInt() == aId && waiting.readByte() == 0 && waiting.readBoolean(), "First TCP party stays host while searching");
                    var matched = b.call("start", null);
                    int battleId = matched.readInt();
                    check(matched.readByte() == 3, "Matched TCP start responds with loading phase for " + partySize + "v" + partySize);
                    var battle = server.rooms.get(battleId);
                    check(battle.count == partySize * 2, "TCP match contains exactly the two original parties");
                    finish(battle);
                    for (var client : clients) {
                        var reply = client.call("returnroom", null);
                        int homeId = reply.readInt();
                        check(reply.readByte() == 0, "TCP result returns to waiting phase");
                        var session = server.sessions.get(client.token);
                        var partyHome = server.rooms.get(homeId);
                        boolean firstParty = clients.indexOf(client) < partySize;
                        check(partyHome.title.equals(firstParty ? "First party" : "Second party") && partyHome.seconds == 12 && partyHome.requestedMap == 1, "TCP return restores own room title, map and turn time");
                        check((session == partyHome.host) == (client == a || client == b), "TCP return preserves original leader and teammate roles");
                    }
                } finally {
                    for (var client : clients) client.close();
                }
                server.rooms.clear();
            }
            try (var a = new TcpGameplayCheck.Player(server.tcpPort(), "solo-a@regression.test");
                 var b = new TcpGameplayCheck.Player(server.tcpPort(), "solo-b@regression.test")) {
                int battleId = a.call("quick", out -> out.writeByte(1)).readInt();
                check(b.call("quick", out -> out.writeByte(1)).readInt() == battleId, "Solo entry point pairs two TCP players");
                var battle = server.rooms.get(battleId);
                check(battle.phase == 3, "Solo starts the loading handshake automatically");
                finish(battle);
                a.call("leave", null); b.call("leave", null);
                check(server.sessions.get(a.token).room == 0 && server.sessions.get(b.token).room == 0,
                    "Solo opponents return to lobby without waiting rooms");
            }

            var headers = new com.sun.net.httpserver.Headers();
            TcpGameServer.headers(headers, 131072 | 4194304, new int[]{0, 1, 2});
            check("1".equals(headers.getFirst("X-Gunny-Actors")) && "0".equals(headers.getFirst("X-Gunny-Live")), "TCP negotiates ordered shot frames with actor stats");
        }
    }
}
