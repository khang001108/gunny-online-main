import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.RecordStore;

final class QuickChat {
    private static final String STORE = "gunny-quick-chat";
    private final PhoneCanvas c;
    private final LobbyChat chat;

    private final String[] phrases = {"Ta chỉ xem chút thôi", "Vậy cũng đánh được ư, hu hu!",
        "Chớ xem thường ta!", "Sẵn sàng! Ready 3, 2, 1!", "Chơi thêm trận nữa nhé!"};
    boolean opened;
    private boolean editing, loaded;
    private int selection;

    private Image panel, editorBar;

    QuickChat(PhoneCanvas canvas, LobbyChat owner) {
        c = canvas;
        chat = owner;
    }

    void open() {
        if (!loaded) {
            loaded = true;
            try {
                RecordStore rs = RecordStore.openRecordStore(STORE, true);
                try {
                    if (rs.getNumRecords() > 0) {
                        DataInputStream in = new DataInputStream(new ByteArrayInputStream(rs.getRecord(1)));
                        for (int i = 0; i < phrases.length; i++) phrases[i] = in.readUTF();
                    }
                } finally { rs.closeRecordStore(); }
            } catch (Exception ignored) {}
        }
        opened = true;
        selection = 0;
        editing = false;
    }


    void draw() {
        if (panel == null) {
            panel = c.combatRoomArt("chat-fast-panel");
            editorBar = c.combatRoomArt("chat-fast-input");
        }
        c.img(panel, 44, 45);
        c.pcCenter(editing ? "Tự thiết lập" : "Tin nhắn nhanh", 160, 54);
        for (int i = 0; i < phrases.length; i++) {
            String label = phrases[i].length() == 0 ? (editing ? "Thêm tin nhắn" : "") : phrases[i];
            c.pcText(c.pcShort(label, 205), 56, 74 + i*14);
            if (selection == i) c.drawSelectionGlow(52, 72+i*14, 215, 14);
        }
        c.img(editorBar, 91, 146);
        c.pcCenter(editing ? "Tin nhắn nhanh" : "Tự thiết lập", 160, 152);
        if (selection == phrases.length) c.drawSelectionGlow(91, 146, 138, 22);
    }

    void key(int key, int dir, boolean ok) {
        if (key == -7 || key == '0') opened = false;
        else if (dir == 4 || dir == 8) selection = (selection + (dir == 4 ? 5 : 1)) % 6;
        else if (ok || key == -6) choose();
    }

    void pointer(int x,int y) {
        if (x < 44 || x >= 276 || y < 45 || y >= 177 || x >= 251 && y < 68) opened = false;
        else if (x >= 52 && x < 267 && y >= 72 && y < 142) {
            selection = (y-72)/14;
            choose();
        } else if (x >= 91 && x < 229 && y >= 146 && y < 168) {
            selection = phrases.length;
            choose();
        }
    }

    private void choose() {
        if (selection == phrases.length) {
            editing = !editing;
            selection = 0;
        } else if (editing) {
            final int index=selection;
            c.typing.field("Tin nhắn nhanh",phrases[index],160,false,false,55,72+index*14,211,
                new InlineText.Done(){
                    public void accept(String value){phrases[index]=value.trim();persist();}
                });
        } else if (phrases[selection].length() > 0) {
            opened = false;
            chat.useQuickText(phrases[selection]);
        }
    }


    private void persist() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            for (int i = 0; i < phrases.length; i++) out.writeUTF(phrases[i]);
            out.close();
            byte[] data = bytes.toByteArray();
            RecordStore rs = RecordStore.openRecordStore(STORE, true);
            try {
                if (rs.getNumRecords() == 0) rs.addRecord(data, 0, data.length);
                else rs.setRecord(1, data, 0, data.length);
            } finally { rs.closeRecordStore(); }
        } catch (Exception error) {
            c.popup.notice("Tin nhắn nhanh", "Không lưu được tin nhắn trên máy.");
        }
    }
}
