
import javax.microedition.lcdui.Image;

/**
 * Shared native item-info frame for both dungeon families.
 * Fixed centered layout.
 */
final class DungeonConditions {
    final PhoneCanvas c;
    Image frame, orange, green;
    int stage = -1, room = -1;
    long until;

    DungeonConditions(PhoneCanvas canvas) {
        c = canvas;
    }

    void reset() {
        stage = room = -1;
        until = 0;
        frame = orange = green = null;
    }

    void toggle() {
        until = System.currentTimeMillis() < until
                ? 0 : System.currentTimeMillis() + 3000;
    }

    boolean visible() {
        return c.game.dungeonStage > 0
                && System.currentTimeMillis() < until;
    }

    void draw() {
        int current = c.game.dungeonStage;
        if (current <= 0 || c.game.over) return;

        int id = c.onlineBattle() ? c.net.roomId : 0;

        if (stage != current || room != id) {
            stage = current;
            room = id;
            until = System.currentTimeMillis() + 3000;
        }

        if (!visible()) return;

        if (frame == null) {
            frame = NativeUi.fit(
                c.combatRoomArt("item752-tooltip"),
                288 * c.s,
                84 * c.s,
                7 * c.s
            );
        }

        c.img(frame, 16, 44);

        if (orange == null) {
            orange = c.combatRoomArt("font-pc-orange");
            green = c.combatRoomArt("font-pc-green");
        }

        final int LEFT_X = 91;
        final int RIGHT_X = 228;

        // Tieu de can giua hai cot
        Image old = c.pcFont;
        try {
            c.pcFont = orange;
            c.pcCenter("Điều kiện thắng", LEFT_X, 50);

            c.pcFont = green;
            c.pcCenter("Điều kiện thua", RIGHT_X, 50);
        } finally {
            c.pcFont = old;
        }

        // Duong chia cot
        c.line(159, 49, 159, 118, 0x8c826b);
        c.line(23, 64, 295, 64, 0x8c826b);

        // Noi dung dieu kien thang
        String win = current == 1 ? "Hạ 20 con kiến"
                : current == 2 ? "Đánh bại Nữ Vương"
                : current == 3 ? "Hạ 20 lính bộ lạc"
                : TribalRules.twins(c.game)
                ? "Hạ cả hai anh em Bulan"
                : "Đánh bại Sulan";

        // Ha chu dieu kien thang xuong
        c.pcCenter(win, LEFT_X, 90);

        // Can giua noi dung cot thua
        c.pcCenter("Cả đội bị đánh bại", RIGHT_X, 73);
        c.pcCenter("Quá giới hạn lượt", RIGHT_X, 88);

        // Huong dan
        c.pcText("M: xem / ẩn", 24, 109);
    }
}
