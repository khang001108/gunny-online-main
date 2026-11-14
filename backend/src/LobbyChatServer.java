import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LobbyChatServer {
    private static final ZoneId PUBLIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final GunnyServer server;
    private final Clock clock;
    private final ArrayList<Message> messages = new ArrayList<>();
    private long nextId;

    private record Message(long id, String sender, String receiver, String name, String text, int emoji, Instant time) {
        Map<String, Object> wire() {
            return Map.of("id", id, "sender_name", name, "message", text, "emoji", emoji);
        }
    }

    LobbyChatServer(GunnyServer server) {
        this(server, Clock.systemUTC());
    }

    LobbyChatServer(GunnyServer server, Clock clock) {
        this.server = server;
        this.clock = clock;
    }

    static boolean handles(String op) {
        return op.equals("chatpoll") || op.equals("chatsend") || op.equals("chatread")
                || op.equals("chatfriends") || op.equals("chatmentions");
    }

    void handle(String op, GunnyServer.Session session, DataInputStream in, DataOutputStream out) throws Exception {
        handle(op, session, in, out, false);
    }

    void handle(String op, GunnyServer.Session session, DataInputStream in, DataOutputStream out, boolean expandedEmoji) throws Exception {
        if (op.equals("chatmentions")) {
            String prefix = in.readUTF();
            if (prefix.length() > 13) throw new IllegalArgumentException("Tên người chơi dài tối đa 13 ký tự.");
            var online = server.sessions.values().stream()
                    .filter(peer -> peer != session && server.social.isOnline(peer))
                    .map(peer -> peer.profile.name)
                    .filter(name -> name.regionMatches(true, 0, prefix, 0, prefix.length()))
                    .distinct().sorted(String.CASE_INSENSITIVE_ORDER).limit(12).toList();
            out.writeByte(online.size());
            for (String name : online) out.writeUTF(name);
            return;
        }
        if (op.equals("chatfriends")) {
            var contacts = (List<?>) query(session, "friends", 0, 0, "", -1);
            if (contacts.size() > 65535) throw new IllegalStateException("Danh sách bạn bè quá lớn.");
            out.writeShort(contacts.size());
            for (Object contact : contacts) {
                var row = Json.obj(contact);
                out.writeInt(Json.num(row, "public_id", 0));
                out.writeUTF(Json.str(row, "name"));
                out.writeBoolean(Boolean.TRUE.equals(row.get("unread")));
            }
            return;
        }

        int peer = in.readInt();
        if (op.equals("chatsend")) {
            String message = in.readUTF().replaceAll("[\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]", " ").strip();
            int emoji = in.readByte();
            if (peer < 0 || message.length() > 160 || emoji < -1 || emoji > (expandedEmoji ? 66 : 5) || message.isEmpty() && emoji < 0)
                throw new IllegalArgumentException("Nhập tin nhắn tối đa 160 ký tự hoặc chọn cảm xúc.");
            if (!server.limit("chat:" + session.id, 24))
                throw new IllegalArgumentException("Bạn gửi tin quá nhanh. Hãy thử lại sau.");
            query(session, "send", peer, 0, message, emoji);
            return;
        }
        long cursor = in.readLong();
        if (op.equals("chatread")) {
            if (peer <= 0 || cursor <= 0) throw new IllegalArgumentException("Sai cuộc trò chuyện.");
            query(session, "read", peer, cursor, "", -1);
            return;
        }
        if (peer < -1 || cursor < 0) throw new IllegalArgumentException("Sai cuộc trò chuyện.");
        var state = Json.obj(query(session, "poll", peer, cursor, "", -1));
        out.writeBoolean(Boolean.TRUE.equals(state.get("unread")));
        writeMessages(out, (List<?>) state.get("preview"), expandedEmoji);
        writeMessages(out, (List<?>) state.get("history"), expandedEmoji);
    }

    private static void writeMessages(DataOutputStream out, List<?> rows, boolean expandedEmoji) throws IOException {
        out.writeByte(rows.size());
        for (Object row : rows) {
            var message = Json.obj(row);
            out.writeLong(((Number) message.get("id")).longValue());
            out.writeUTF(Json.str(message, "sender_name"));
            out.writeUTF(Json.str(message, "message"));
            int emoji = Json.num(message, "emoji", -1);
            out.writeByte(!expandedEmoji && emoji > 5 ? 3 : emoji);
        }
    }

    void purge() {
        if (server.store instanceof CloudStore.Memory) {
            synchronized (this) { discardExpired(clock.instant()); }
            return;
        }
        try {
            query(null, "purge", 0, 0, "", -1);
        } catch (Exception error) {
            System.err.println("Chat retention cleanup failed: " + error.getClass().getSimpleName());
        }
    }

    private Object query(GunnyServer.Session session, String action, int peer, long cursor, String text, int emoji)
            throws Exception {
        if (server.store instanceof CloudStore.Memory) return memory(session, action, peer, cursor, text, emoji);
        var args = new LinkedHashMap<String, Object>();
        args.put("p_user", session == null ? null : session.id);
        args.put("p_action", action);
        args.put("p_peer", peer);
        args.put("p_cursor", cursor);
        args.put("p_message", text);
        args.put("p_emoji", emoji);
        return server.store.request("/rest/v1/rpc/gunny_chat_api", "POST", Json.write(args), true);
    }

    private final java.util.Set<Long> read = new java.util.HashSet<>();

    private void discardExpired(Instant now) {
        var midnight = now.atZone(PUBLIC_ZONE).toLocalDate().atStartOfDay(PUBLIC_ZONE).toInstant();
        var oldestPrivate = now.minus(Duration.ofDays(30));
        messages.removeIf(m -> {
            if (!m.time.isBefore(m.receiver == null ? midnight : oldestPrivate)) return false;
            read.remove(m.id);
            return true;
        });
    }

    private synchronized Object memory(GunnyServer.Session s, String action, int peer, long cursor, String text,
            int emoji) throws Exception {
        Instant now = clock.instant();
        discardExpired(now);
        if (action.equals("friends")) {
            List<Map<String, Object>> contacts = new ArrayList<>();
            for (var relation : server.social.relations(s.id)) {
                if (!Boolean.TRUE.equals(relation.get("accepted"))) continue;
                String id = Json.str(relation, "sender");
                if (id.equals(s.id)) id = Json.str(relation, "receiver");
                var profile = server.store.profile(id);
                contacts.add(Map.of("public_id", profile.publicId, "name", profile.name, "unread",
                        unread(s.id, id)));
            }
            contacts.sort(Comparator.<Map<String, Object>, Boolean>comparing(row -> Boolean.TRUE.equals(row.get("unread")))
                    .reversed().thenComparing(row -> Json.str(row, "name")));
            return contacts;
        }
        String target = peer > 0 ? server.social.target(peer).id : null;
        if (target != null && server.social.relations(s.id).stream().noneMatch(r ->
                Boolean.TRUE.equals(r.get("accepted")) && (target.equals(r.get("sender")) || target.equals(r.get("receiver")))))
            throw new IllegalArgumentException("Chỉ có thể nhắn tin cho bạn bè.");
        if (action.equals("send")) {
            messages.add(new Message(++nextId, s.id, target, s.profile.name, text, emoji, now));
            return Map.of();
        }
        if (action.equals("read")) {
            for (Message m : messages)
                if (m.id <= cursor && m.sender.equals(target) && s.id.equals(m.receiver)) read.add(m.id);
            return Map.of();
        }
        List<Message> newest = new ArrayList<>(messages);
        Collections.reverse(newest);
        return Map.of("unread", unread(s.id, null),
                "preview", newest.stream().filter(m -> m.receiver == null).limit(5).map(Message::wire).toList(),
                "history", newest.stream().filter(m -> cursor == 0 || m.id < cursor)
                        .filter(m -> peer == 0 && m.receiver == null || target != null
                                && (s.id.equals(m.sender) && target.equals(m.receiver)
                                        || target.equals(m.sender) && s.id.equals(m.receiver)))
                        .limit(17).map(Message::wire).toList());
    }

    private boolean unread(String recipient, String sender) {
        return messages.stream().anyMatch(m -> recipient.equals(m.receiver)
                && (sender == null || sender.equals(m.sender)) && !read.contains(m.id));
    }
}
