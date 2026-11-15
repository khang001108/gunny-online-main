import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public final class SupportPageCheck {
    static class Records extends Admin750Check.Records {
        final List<Map<String,Object>> entries = new ArrayList<>();
        String qr = "", mime = "image/jpeg";
        Records(CloudStore.Memory store) throws Exception { super(store); }
        synchronized Object call(String op, Map<String,Object> input) throws Exception {
            if (op.equals("support-public")) return Map.of("qr", qr, "mime", mime, "entries", entries);
            if (op.equals("support-list")) return entries;
            if (op.equals("retention-policy")) return Map.of("enabled", false, "offline_days", 90);
            if (op.equals("support-qr")) {
                qr = Json.str(input, "qr"); mime = Json.str(input, "mime"); return Map.of();
            }
            if (op.equals("support-add")) {
                if (!(input.get("amount") instanceof Long)) throw new AssertionError("SQL amounts must serialize as integer text");
                for (var entry : entries) if (entry.get("request_key").equals(input.get("request_key"))) return entry;
                var player = memory.profile(Json.str(input, "id"));
                player.stones += Json.num(input, "stones", 0);
                player.coins += Json.num(input, "coins", 0);
                player.shoppingMoney += Json.num(input, "money", 0);
                var entry = new HashMap<>(input);
                entry.put("display_name", player.name); entry.put("created_at", java.time.Instant.now().toString());
                entries.add(entry); return entry;
            }
            return super.call(op, input);
        }
    }

    public static void main(String[] args) throws Exception {
        var memory = new CloudStore.Memory();
        memory.register("support@local.test", "test-password-123", "Gunner");
        var records = new Records(memory);
        try (var server = new GunnyServer(new InetSocketAddress("127.0.0.1", args.length == 0 ? 0 : 8765),
                Path.of("backend/assets"), memory, records, Admin750Check.PASSWORD, true)) {
            Admin750Check.base = "http://127.0.0.1:" + server.port();
            Admin750Check.cookie = "";
            if (args.length > 0) {
                System.out.println("PREVIEW " + Admin750Check.base);
                new java.util.concurrent.CountDownLatch(1).await();
                return;
            }
            Admin750Check.ok(Admin750Check.req("support-list", null, false).statusCode() == 401, "Support records require admin login");
            var login = Admin750Check.req("login", Map.of("password", Admin750Check.PASSWORD), true);
            Admin750Check.cookie = login.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
            String id = records.accounts.keySet().iterator().next();
            var payload = new HashMap<String,Object>(Map.of("id", id, "request_key", UUID.randomUUID().toString(),
                "amount", 50000, "stones", 10, "coins", 20, "money", 30, "comment", "<script>privateGiftMarker</script>"));
            Admin750Check.ok(Admin750Check.req("support-add", payload, false).statusCode() == 403, "Cross-origin support gifts are rejected");
            Admin750Check.ok(Admin750Check.req("support-add", payload, true).statusCode() == 200, "Admin grants all three gift types");
            Admin750Check.ok(Admin750Check.req("support-add", payload, true).statusCode() == 200 && memory.profile(id).stones == 10, "Retry reuses a single gift record");
            var page = Admin750Check.http.send(java.net.http.HttpRequest.newBuilder(URI.create(Admin750Check.base + "/download/")).GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            Admin750Check.ok(page.body().contains("<strong>TÊN NHÂN VẬT ONLINE</strong>") && page.body().contains("Gunner"), "Download page renders support text and acknowledgements");
            Admin750Check.ok(!page.body().contains(id) && !page.body().contains("request_key") && !page.body().contains("stones"), "Public page hides identifiers and gift fields");
            Admin750Check.ok(page.body().contains("&lt;script&gt;") && !page.body().contains("<script>privateGiftMarker"), "Public comments are escaped");
            var invalid = new HashMap<>(payload); invalid.put("stones", 0.5);
            Admin750Check.ok(Admin750Check.req("support-add", invalid, true).statusCode() == 400, "Fractional gifts are rejected");
            Admin750Check.ok(Admin750Check.req("support-qr", Map.of("qr", Base64.getEncoder().encodeToString("<svg/>".getBytes()), "mime", "image/jpeg"), true).statusCode() == 400, "Non-image uploads are rejected");
            byte[] image = Files.readAllBytes(Path.of("public/support-qr.jpg"));
            Admin750Check.ok(Admin750Check.req("support-qr", Map.of("qr", Base64.getEncoder().encodeToString(image), "mime", "image/jpeg"), true).statusCode() == 200, "Admin can replace QR image");
            var response = Admin750Check.http.send(java.net.http.HttpRequest.newBuilder(URI.create(Admin750Check.base + "/download/support-qr.jpg")).GET().build(), java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            Admin750Check.ok(Arrays.equals(image, response.body()), "Public QR serves the exact uploaded bytes");
        }
    }
}
