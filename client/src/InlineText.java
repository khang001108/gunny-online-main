import javax.microedition.lcdui.Canvas;

/** Edits a Canvas field using the device's key events. */
final class InlineText {
    interface Done { void accept(String value); }

    private final PhoneCanvas c;
    private Done done;
    private String label, value;
    private int mode, room, max, cursor, x, y, width;
    private boolean numeric, secret, panel;

    InlineText(PhoneCanvas canvas) { c = canvas; }

    void open(String title, String initial, int limit, boolean digits, boolean masked, Done action) {
        start(title, initial, limit, digits, masked, 45, 108, 230, true, action);
    }

    void field(String title, String initial, int limit, boolean digits, boolean masked,
               int left, int top, int w, Done action) {
        start(title, initial, limit, digits, masked, left, top, w, false, action);
    }

    private void start(String title, String initial, int limit, boolean digits, boolean masked,
                       int left, int top, int w, boolean boxed, Done action) {
        label = title;
        value = initial;
        cursor = initial.length();
        max = limit;
        numeric = digits;
        secret = masked;
        panel = boxed;
        x = left;
        y = top;
        width = w;
        mode = c.mode;
        room = c.net.roomId;
        done = action;
        c.held = c.fireKey = c.cameraHeld = 0;
    }

    boolean active() {
        if (done != null && (mode != c.mode || room != c.net.roomId)) cancel();
        return done != null;
    }

    void cancel() { done = null; value = ""; }

    void finish() {
        if (!active()) return;
        Done action = done;
        String result = value;
        cancel();
        action.accept(result);
    }

    boolean key(int key) {
        if (!active()) return false;
        if (key == -7 || key == 27) cancel();
        else if (key == 13 || key == 10 || key == -6 || key == -5) finish();
        else if (key == 8 || key == -8 || key == 127) {
            if (cursor > 0) {
                value = value.substring(0, cursor - 1) + value.substring(cursor);
                cursor--;
            }
        } else if (key < 0) {
            try {
                int action = c.getGameAction(key);
                if (action == Canvas.LEFT && cursor > 0) cursor--;
                if (action == Canvas.RIGHT && cursor < value.length()) cursor++;
                if (action == Canvas.FIRE) finish();
            } catch (IllegalArgumentException ignored) {}
        } else if (key >= 32 && key <= 0xffff && (key < 127 || key >= 160)
                && (!numeric || key >= '0' && key <= '9') && value.length() < max) {
            value = value.substring(0, cursor) + (char)key + value.substring(cursor);
            cursor++;
        }
        return true;
    }

    boolean pointer(int px, int py) {
        if (!active()) return false;
        if (py >= y && py < y + 16 && px >= x + width && px < x + width + 24) finish();
        else if (panel && py >= y + 20 && py < y + 38) {
            if (px >= x && px < x + width / 2) cancel();
            else if (px < x + width) finish();
        } else if (!panel && (px < x || px >= x + width || py < y || py >= y + 16)) finish();
        return true;
    }

    void draw() {
        if (!active()) return;
        c.all();
        if (panel) {
            c.panel(x - 9, y - 23, width + 18, 64);
            c.pcCenter(label, x + width / 2, y - 17);
            c.ui.button("Hủy",x + 8,y + 20,74,17,false);
            c.ui.button("Xong",x + width - 82,y + 20,74,17,false);
        }
        c.rect(x,y,width,16,0x292119);
        String display = value;
        if (secret) {
            StringBuffer masked = new StringBuffer();
            for (int i = 0; i < value.length(); i++) masked.append('*');
            display = masked.toString();
        }
        int start = 0;
        while (start < cursor && c.pcWidth(display.substring(start,cursor)) > width - 10) start++;
        int end = display.length();
        while (end > cursor && c.pcWidth(display.substring(start,end)) > width - 10) end--;
        c.pcText(display.substring(start,end),x + 4,y + 3);
        if (System.currentTimeMillis() / 400 % 2 == 0)
            c.rect(x + 4 + c.pcWidth(display.substring(start,cursor)),y + 3,1,10,0xffdf95);
    }
}
