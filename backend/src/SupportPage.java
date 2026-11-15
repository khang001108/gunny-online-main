import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Public acknowledgements expose only names, amounts and comments. */
final class SupportPage {
    final AdminData data;
    private Map<String,Object> cached = Map.of();
    private long refreshed;

    SupportPage(AdminData records) { data = records; }

    synchronized Map<String,Object> publicData() {
        if (System.currentTimeMillis() - refreshed < 30000) return cached;
        try { cached = Json.obj(data.call("support-public", Map.of())); }
        catch (Exception unavailable) { /* The packaged QR is available before migration. */ }
        refreshed = System.currentTimeMillis();
        return cached;
    }

    synchronized void invalidate() { refreshed = 0; }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }

    String render(String html) {
        var output = new StringBuilder();
        Object entries = publicData().get("entries");
        if (entries instanceof List<?> list && !list.isEmpty()) {
            output.append("<h3>Cảm ơn những người đã thắp sáng</h3><ul>");
            for (Object item : list) {
                var entry = Json.obj(item);
                long amount = ((Number)entry.getOrDefault("amount", 0)).longValue();
                output.append("<li><strong>").append(escape(Json.str(entry, "display_name")))
                    .append("</strong> · ").append(String.format(Locale.forLanguageTag("vi-VN"), "%,d", amount))
                    .append(" đ");
                String comment = Json.str(entry, "comment");
                if (!comment.isEmpty()) output.append("<p>").append(escape(comment)).append("</p>");
                output.append("</li>");
            }
            output.append("</ul>");
        }
        return html.replace("<!--SUPPORTERS-->", output);
    }

    void image(HttpExchange exchange) throws IOException {
        var settings = publicData();
        String qr = Json.str(settings, "qr");
        byte[] bytes = qr.isEmpty() ? Files.readAllBytes(GunnyServer.publicRoot().resolve("support-qr.jpg"))
            : Base64.getDecoder().decode(qr);
        var headers = exchange.getResponseHeaders();
        headers.set("Content-Type", qr.isEmpty() ? "image/jpeg" : Json.str(settings, "mime"));
        headers.set("Cache-Control", "no-cache");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Content-Length", String.valueOf(bytes.length));
        if (exchange.getRequestMethod().equals("HEAD")) exchange.sendResponseHeaders(200, -1);
        else { exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); }
        exchange.close();
    }

    static long integer(Map<String,Object> input, String field, long max) {
        Object value = input.get(field);
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())
                || number.doubleValue() != number.longValue() || number.longValue() < 0 || number.longValue() > max)
            throw new IllegalArgumentException("Số tiền và quà phải là số nguyên không âm trong giới hạn.");
        return number.longValue();
    }

    Object operation(String op, Map<String,Object> input) throws Exception {
        if (op.equals("support-qr")) {
            String encoded = Json.str(input, "qr"), mime = Json.str(input, "mime");
            if (!Set.of("image/jpeg", "image/png").contains(mime) || encoded.length() > 2000000)
                throw new IllegalArgumentException("Chọn ảnh JPEG hoặc PNG tối đa 1,4 MB.");
            byte[] bytes = Base64.getDecoder().decode(encoded);
            // Inspect dimensions before decoding to bound image memory use.
            try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw new IllegalArgumentException("Ảnh QR không hợp lệ.");
                var reader = readers.next();
                try {
                    reader.setInput(stream);
                    String format = reader.getFormatName();
                    if (!(mime.equals("image/jpeg") && format.equalsIgnoreCase("JPEG")
                            || mime.equals("image/png") && format.equalsIgnoreCase("PNG"))
                            || reader.getWidth(0) > 4096 || reader.getHeight(0) > 4096
                            || (long)reader.getWidth(0) * reader.getHeight(0) > 8000000)
                        throw new IllegalArgumentException("Ảnh QR sai định dạng hoặc quá lớn.");
                    if (reader.read(0) == null) throw new IllegalArgumentException("Không đọc được ảnh QR.");
                } finally { reader.dispose(); }
            }
        } else if (op.equals("support-add")) {
            UUID.fromString(Json.str(input, "id"));
            UUID.fromString(Json.str(input, "request_key"));
            input.put("amount", integer(input, "amount", 1000000000000L));
            for (String field : List.of("stones", "coins", "money")) input.put(field, integer(input, field, 999999));
        } else if (op.equals("support-comment")) UUID.fromString(Json.str(input, "request_key"));
        if ((op.equals("support-add") || op.equals("support-comment")) && Json.str(input, "comment").length() > 1000)
            throw new IllegalArgumentException("Bình luận tối đa 1.000 ký tự.");
        Object result = data.call(op, input);
        invalidate();
        return result;
    }
}
