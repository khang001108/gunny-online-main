import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Image;

/** Edits a Canvas field using the device's key events. */
final class InlineText {
    interface Done { void accept(String value); }

    private final PhoneCanvas c;
    private Done done;
    private String label, value;
    private int mode, room, max, cursor, x, y, width;
    private boolean numeric, secret, panel, chatField, telex = true;
    private Image popupFrame, popupClose, popupInput, popupButton;
    private int skinScale;
    // Boxed editor: 248 x 86; input and buttons retain their original anchors.
    private static final int PANEL_X=36,PANEL_Y=76,PANEL_W=248,PANEL_H=86;
    private static final int CLOSE_X=PANEL_X+PANEL_W-25,CLOSE_Y=PANEL_Y+5;
    void releaseArt(){popupFrame=popupClose=popupInput=popupButton=null;skinScale=0;}
    private void loadSkin(){
        if(popupFrame!=null&&skinScale==c.s)return;
        releaseArt();skinScale=c.s;
        popupFrame=NativeUi.popupFrame(c.combatRoomArt("settings743-popup"),PANEL_W,PANEL_H,c.s);
        popupClose=c.combatRoomArt("bag715-close");
        popupInput=NativeUi.fit(c.combatRoomArt("auth714-input"),230*c.s,16*c.s,4*c.s);
        popupButton=NativeUi.fit(c.combatRoomArt("popup751-button"),74*c.s,22*c.s,5*c.s);
    }

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
        chatField = "Trò chuyện".equals(title);
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
        // Cho phép nhập @ bằng phím # trong ô Email
        if (!numeric && !secret
                && label != null
                && label.toLowerCase().indexOf("email") >= 0
                && key == Canvas.KEY_POUND) {
            key = '@';
        }
        if (chatField && key == Canvas.KEY_POUND) { telex = !telex; return true; }
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
                && (!numeric || key >= '0' && key <= '9')  ) {
            String left = chatField && telex ? VietnameseInput.compose(value.substring(0,cursor),(char)key) : null;
            if(left==null)left=value.substring(0,cursor)+(char)key;
            if(left.length()+value.length()-cursor<=max){value=left+value.substring(cursor);cursor=left.length();}
        }
        return true;
    }

    boolean pointer(int px, int py) {
        if (!active()) return false;
        if (panel) {
            if (px>=CLOSE_X&&px<CLOSE_X+21&&py>=CLOSE_Y&&py<CLOSE_Y+19) cancel();
            else if (py>=y+22&&py<y+44) {
                if(px>=x+8&&px<x+82)cancel();
                else if(px>=x+width-82&&px<x+width-8)finish();
            }
        } else if (px < x || px >= x + width || py < y || py >= y + 16) finish();
        return true;
    }

    void draw() {
        if (!active()) return;
        c.all();
        if (panel) {
            loadSkin();
            c.img(popupFrame,PANEL_X,PANEL_Y);
            c.pcCenter(c.pcShort(label,196),160,PANEL_Y+9);
            c.img(popupClose,CLOSE_X,CLOSE_Y);
            c.img(popupInput,x,y);
            c.img(popupButton,x+8,y+22);
            c.img(popupButton,x+width-82,y+22);
            c.pcCenter("Hủy",x+45,y+28);
            c.pcCenter("Xong",x+width-45,y+28);
        }else c.rect(x,y,width,16,0x292119);
        String display = value;
        if (secret) {
            StringBuffer masked = new StringBuffer();
            for (int i = 0; i < value.length(); i++) masked.append('*');
            display = masked.toString();
        }
        int usable=width-(chatField?32:10);
        int start = 0;
        while (start < cursor && c.pcWidth(display.substring(start,cursor)) > usable) start++;
        int end = display.length();
        while (end > cursor && c.pcWidth(display.substring(start,end)) > usable) end--;
        c.pcText(display.substring(start,end),x + 4,y + 3);
        if(chatField)c.pcText(telex?"VI":"EN",x+width-22,y+3);
        if (System.currentTimeMillis() / 400 % 2 == 0)
            c.rect(x + 4 + c.pcWidth(display.substring(start,cursor)),y + 3,1,10,panel?0x392918:0xffdf95);
    }
}
