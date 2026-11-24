import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.microedition.lcdui.*;

final class LobbyChat {
    private static final int PUBLIC = 0, CONTACTS = -1, CLOSED = -2;
    private static final int VISIBLE = 8, MESSAGE_ROWS = 3, MESSAGE_H = 40;
    private int visibleRows(){return channel==CONTACTS?VISIBLE:MESSAGE_ROWS;}
    private static final int[] BUTTON_X = { 20, 231, 254, 276 };
    private static final int[] BUTTON_W = { 210, 22, 22, 22 };
    private static final int PANEL_X = 12, PANEL_Y = 20, PANEL_W = 296, PANEL_H = 216;
    private static final int INPUT_Y = 207, ROW_Y = 70;
    private final OnlineClient net;
    private final PhoneCanvas c;

    boolean available, opened, unread;
    private int channel = PUBLIC, first, selected, generation;
    private int sentChannel, sentGeneration, sendChannel, draftEmoji = -1, readChannel;
    private long pollAt, contactsAt, cursor, sentCursor, readId, seenId;
    private long renderedId;
    private int renderedChannel = CLOSED;
    private String peerName = "", draft = "", outgoing = "", prefix = "", note = "";
    private String queued = "";
    private boolean loading, more, emojiPicker, namePicker;
    final RoomSpeech roomSpeech;
    private Image backdrop, launchIcon, quickIcon, smileIcon, sendIcon, faces, closeIcon, input, facePanel;
    private long openedAt, socialContactsAt;
    private final QuickChat quick;
    private final Message[] history = new Message[16];
    private int historyCount;
    private Contact[] contacts = new Contact[0];
    private String[] names = new String[0];

    private static final class Message {
        long id;
        String author, body;
        int emoji;
        String portraitKey; int sex,skin,mask; boolean custom; int[] look;

        Message(DataInputStream in) throws IOException {
            id = in.readLong();
            author = in.readUTF();
            body = in.readUTF();
            emoji = in.readByte();
        }

        void readPortrait(DataInputStream in)throws IOException{
            if(!in.readBoolean())return;
            sex=in.readUnsignedByte();skin=in.readUnsignedByte();custom=in.readBoolean();mask=in.readUnsignedByte();look=new int[5];
            portraitKey=sex+":"+skin+":"+mask+":"+custom;
            boolean valid=sex<=1&&skin<=9;
            for(int i=0;i<5;i++){look[i]=in.readUnsignedByte();portraitKey+=":"+look[i];if(look[i]>9)valid=false;}
            if(!valid)portraitKey=null;
        }
        String text() { return author + ": " + body; }
    }

    private static final class Contact {
        int id;
        String name;
        boolean unread;

        Contact(DataInputStream in) throws IOException {
            id = in.readInt();
            name = in.readUTF();
            unread = in.readBoolean();
        }
    }

    LobbyChat(OnlineClient owner) {
        net = owner;
        c = owner.c;
        quick = new QuickChat(c, this);
        roomSpeech = new RoomSpeech(this, net, quick);
    }

    boolean lobby() {
        return net.logged && !net.homeAfterLogin && c.mode == 0;
    }

    boolean enter(int key) { return key == 13 || key == 10; }
    boolean hotkey(int key) { return key == 'g' || key == 'G'; }
    void openCompose() {
        if (!opened) toggle();
        if (!opened) return;
        // Enter always composes a public message, even when a DM was unread.
        if (channel < PUBLIC) switchChannel(PUBLIC, "");
        compose();
    }
    int rawBinding(int raw, int mapped) {
        return enter(raw) || hotkey(raw) || raw == '/' || raw == '@' ? raw : mapped;
    }

    void toggle() {
        if (!lobby()) return;
        if (!available) {
            c.popup.notice("Chat", "Cập nhật máy chủ để dùng chat.");
            return;
        }
        if (opened) {
            close();
            return;
        }
        opened = true;
        openedAt = System.currentTimeMillis();
        switchChannel(unread ? CONTACTS : PUBLIC, "");
    }

    void close() {
        if(opened) c.typing.cancel();
        opened = namePicker = emojiPicker = false;
        quick.opened = false;

        pollAt = 0;
        renderedChannel = CLOSED;
        generation++;
    }

    void reset() {
        roomSpeech.reset();
        close();
        unread = loading = false;
        historyCount = 0;
        contacts = new Contact[0];
        draft = outgoing = note = "";
        completedMention = "";
        draftEmoji = -1;
        for (int i = 0; i < history.length; i++) history[i] = null;
        for(int i=0;i<portraitKeys.length;i++){portraitKeys[i]=null;portraitImages[i]=null;}
        names = new String[0];
        channel = PUBLIC;
        blockedReadUntil = 0;
        socialContactsAt = 0;
        sending = false;
        queued = "";
        readId = seenId = 0;
    }

    void leaveWaitingRoom() {
        if (!opened) return;
        c.typing.cancel();
        close();
    }

    void requestSocialContacts() { socialContactsAt = 0; }

    boolean unreadFrom(int id) {
        for (int i = 0; i < contacts.length; i++)
            if (contacts[i].id == id) return contacts[i].unread;
        return false;
    }

    private boolean friendsPopupVisible() {
        return net.social.opened && net.social.friendsPopup && net.social.tab == 0
                && !net.social.viewing && !net.social.searching;
    }

    private void switchChannel(int target, String name) {
        if (channel != target) {
            draft = "";
            draftEmoji = -1;
            completedMention = "";
        }
        channel = target;
        peerName = name;
        first = selected = 0;
        cursor = 0;
        more = false;
        historyCount = 0;
        seenId = 0;
        renderedChannel = CLOSED;
        contactsAt = pollAt = 0;
        note = "";
        loading = true;
        generation++;
    }

    String nextRequest(long now) {
        synchronized (c) { return nextRequestOnCanvas(now); }
    }

    private String nextRequestOnCanvas(long now) {
        String speech = roomSpeech.nextRequest(now);
        if (speech.length()>0) return speech;
        if (!available || !lobby()) return "";
        if (queued.length() > 0) {
            String op = queued;
            if (!op.equals("chatsend") && !opened) op = "";
            queued = "";
            return op;
        }
        if (opened && channel > 0 && !c.typing.active() && !c.popup.opened && !emojiPicker && !namePicker
                && !quick.opened && !c.presentation.settings
                && renderedChannel == channel && renderedId > seenId) {
            readChannel = channel;
            readId = renderedId;
            seenId = readId;
            return "chatread";
        }
        if (opened && channel == CONTACTS && now >= contactsAt) {
            contactsAt = now + 6000;
            sentGeneration = generation;
            return "chatfriends";
        }
        if (!opened && friendsPopupVisible() && now >= socialContactsAt) {
            // Refresh unread flags for dots in the Friends popup; no protocol change.
            socialContactsAt = now + 6000;
            sentGeneration = generation;
            return "chatfriends";
        }
        if (now < pollAt) return "";
        pollAt = now + (opened ? 2300 : 5000);
        return "chatpoll";
    }

    void write(String op, DataOutputStream out) throws IOException {
        synchronized (c) { writeOnCanvas(op, out); }
    }

    private void writeOnCanvas(String op, DataOutputStream out) throws IOException {
        if (op.startsWith("chatroom")) { roomSpeech.write(op,out); return; }
        if (op.equals("chatsend")) {
            out.writeInt(sendChannel);
            out.writeUTF(outgoing);
            out.writeByte(sendEmoji);
        } else if (op.equals("chatread")) {
            out.writeInt(readChannel);
            out.writeLong(readId);
        } else if (op.equals("chatpoll")) {
            sentGeneration = generation;
            sentChannel = opened ? channel : CONTACTS;
            sentCursor = opened ? cursor : 0;
            out.writeInt(sentChannel);
            out.writeLong(sentCursor);
            if(opened && channel>=PUBLIC)out.writeByte(15);
        } else if (op.equals("chatfriends")) {
            sentGeneration = generation;
        } else if (op.equals("chatmentions")) {
            sentGeneration = generation;
            out.writeUTF(prefix);
        }
    }

    void read(String op, DataInputStream in) throws IOException {
        if (op.startsWith("chatroom")) { if(op.equals("chatroomsend")) roomSpeech.sent(); roomSpeech.read(in); return; }
        if (op.equals("chatpoll")) {
            unread = in.readBoolean();
            int previewCount = in.readUnsignedByte();
            if (previewCount > 5) throw new IOException("Sai danh sách chat.");
            for (int i = 0; i < previewCount; i++) new Message(in);
            int count = in.readUnsignedByte();
            if (count > 17) throw new IOException("Sai lịch sử chat.");
            if (opened && sentGeneration == generation && sentChannel == channel && sentCursor == cursor && channel >= PUBLIC) {
                long anchor = first < historyCount ? history[first].id : 0;
                historyCount = Math.min(count, history.length);
                for (int i = 0; i < historyCount; i++) history[i] = new Message(in);
                for(int i=historyCount;i<count;i++)new Message(in);
                if(in.available()>0 && in.readUnsignedByte()==15){int portraits=in.readUnsignedByte();if(portraits!=count)throw new IOException("Sai ảnh chat.");for(int i=0;i<portraits;i++){Message m=i<historyCount?history[i]:null;if(m!=null)m.readPortrait(in);else if(in.readBoolean())for(int k=0;k<9;k++)in.readUnsignedByte();}}
                more = count > history.length;
                if (!loading && first > 0) {
                    for (int i = 0; i < historyCount; i++)
                        if (history[i].id == anchor) { first = i; break; }
                }
                first = Math.min(first, Math.max(0, historyCount - MESSAGE_ROWS));
                selected = Math.min(selected, Math.max(0, Math.min(MESSAGE_ROWS, historyCount - first) - 1));
                loading = false;
            }
        } else if (op.equals("chatfriends")) {
            if (sentGeneration != generation) return;
            boolean showContacts = opened && channel == CONTACTS;
            if (!showContacts && (opened || !friendsPopupVisible())) return;
            int count = in.readUnsignedShort();
            if (count > 128) throw new IOException("Danh sách chat quá lớn.");
            Contact[] updated = new Contact[count];
            int old = showContacts && first + selected < contacts.length
                    ? contacts[first + selected].id : 0;
            for (int i = 0; i < count; i++) updated[i] = new Contact(in);
            contacts = updated;
            if (!showContacts) return; // Preserve popup Friends navigation state.
            first = Math.min(first, Math.max(0, count - VISIBLE));
            selected = Math.min(selected, Math.max(0, Math.min(VISIBLE, count - first) - 1));
            for (int i = 0; old != 0 && i < count; i++) {
                if (contacts[i].id != old) continue;
                first = Math.min(first, i);
                first = Math.max(first, i - VISIBLE + 1);
                selected = i - first;
                break;
            }
            loading = false;
        } else if (op.equals("chatmentions")) {
            if (!opened || sentGeneration != generation) return;
            int count = in.readUnsignedByte();
            if (count > 12) throw new IOException("Sai danh sách gợi ý.");
            names = new String[count];
            for (int i = 0; i < count; i++) names[i] = in.readUTF();
            namePicker = true;
            pickerFirst = pickerSelected = 0;
            note = "";
        } else if (op.equals("chatsend")) {
            if (channel == sendChannel && generation == sendGeneration) {
                draft = "";
                completedMention = "";
                draftEmoji = -1;
                cursor = 0;
                first = selected = 0;
                generation++;
            }
            sending = false;
            pollAt = 0;
            note = "";
        } else if (op.equals("chatread")) {
            pollAt = 0;
        }
    }

    void failed(String op, String message) {
        if (op.startsWith("chatroom")) { roomSpeech.failed(op,message); return; }
        loading = false;
        if (op.equals("chatsend")) sending = false;
        if (op.equals("chatread")) {
            // Retry after a fresh poll instead of repeatedly acknowledging a failed request.
            renderedChannel = CLOSED;
            seenId = 0;
            if (opened && channel == readChannel) {
                renderedId = 0;
                blockedReadUntil = System.currentTimeMillis() + 6000;
            }
        }
        note = message == null ? "Không tải được chat." : message;
        pollAt = System.currentTimeMillis() + 6000;
    }

    private long blockedReadUntil;
    private boolean sending;
    private int sendEmoji, sendGeneration, pickerFirst, pickerSelected;

    private void compose() {
        if (channel < PUBLIC || sending) return;
        c.typing.field("Trò chuyện",draft,160,false,false,20,INPUT_Y+3,210,new InlineText.Done(){
            public void accept(String value) { draft=value; submitDraft(); }
        });
    }

    void useQuickText(String text) {
        if (roomSpeech.context()!=0) { roomSpeech.send(text,-1); return; }
        draft = text;
        submitDraft();
    }

    private void submitDraft() {
        if (channel < PUBLIC || sending || queued.length() > 0) return;
        String value = draft.trim();
        if (value.length() == 0 && draftEmoji < 0) return;
        int at = mentionStart(value);
        if (at >= 0 && draft.equals(value) && value.indexOf(' ', at) < 0 && channel == PUBLIC
                && !value.substring(at + 1).equals(completedMention)) {
            suggest();
            return;
        }
        sendChannel = channel;
        sendGeneration = generation;
        outgoing = value;
        sendEmoji = draftEmoji;
        sending = true;
        note = "Đang gửi...";
        queued = "chatsend";
    }

    private int mentionStart(String value) {
        int at = value.lastIndexOf('@');
        return at >= 0 && (at == 0 || value.charAt(at - 1) == ' ') ? at : -1;
    }

    private void suggest() {
        if (sending || queued.length() > 0) return;
        int at = mentionStart(draft);
        if (at < 0 || draft.indexOf(' ', at) >= 0 || draft.length() - at > 14) {
            if (draft.length() > 158) return;
            draft = draft.trim() + (draft.trim().length() == 0 ? "@" : " @");
            at = draft.length() - 1;
        }
        if (channel > PUBLIC) {
            insertMention(peerName);
            return;
        }
        prefix = draft.substring(at + 1);
        note = "Đang tìm người chơi...";
        queued = "chatmentions";
    }

    private void insertMention(String name) {
        int at = mentionStart(draft);
        String value = draft.substring(0, at + 1) + name + " ";
        if (value.length() <= 160) {
            draft = value;
            completedMention = name;
        }
        namePicker = false;
        compose();
    }

    private void chooseRow(int row) {
        int i = first + row;
        if (channel == CONTACTS) {
            if (i < contacts.length) switchChannel(contacts[i].id, contacts[i].name);
        } else if (i < historyCount) {
            Message m = history[i];
            c.popup.notice(m.author, m.body.length() > 0 ? m.body : "Cảm xúc");
        }
    }

    private void olderPage() {
        if (!more || historyCount == 0) return;
        cursor = history[historyCount - 1].id;
        historyCount = first = selected = 0;
        more = false;
        generation++;
        pollAt = 0;
        loading = true;
    }

    private void older() {
        int last = Math.max(0, historyCount - MESSAGE_ROWS);
        if (first < last) {
            first = Math.min(first + MESSAGE_ROWS, last);
            selected = 0;
        } else olderPage();
    }

    private void moveRow(int direction) {
        int count = channel == CONTACTS ? contacts.length : historyCount;
        int delta = direction == 4 ? -1 : 1;
        int i = first + selected + (channel == CONTACTS ? delta : -delta);
        if (i >= count && channel != CONTACTS && more) {
            olderPage();
        } else if (i >= 0 && i < count) {
            first = Math.min(first, i);
            first = Math.max(first, i - visibleRows() + 1);
            selected = i - first;
        }
    }

    private void button(int index) {
        if (index == 0) {
            compose();
        } else if (index == 1) { if (draft.length()>0||draftEmoji>=0) submitDraft(); else compose(); }
        else if (index == 3) {
            emojiPicker = true;
            pickerSelected = 0;
            pickerFirst = 0;
        } else quick.open();
    }

    boolean key(int key, int dir, boolean ok) {
        if (!lobby()) return false;
        if (!opened) {
            if (enter(key)) { openCompose(); return true; }
            if (hotkey(key)) { toggle(); return true; }
            return false;
        }
        if (hotkey(key)) { close(); return true; }
        if (quick.opened) { quick.key(key, dir, ok); return true; }
        if (key == -7 || key == '0') {
            if (namePicker || emojiPicker) namePicker = emojiPicker = false;
            else if (channel > PUBLIC) switchChannel(CONTACTS, "");
            else close();
            return true;
        }
        if (namePicker || emojiPicker) {
            int count = namePicker ? names.length : emojiCount();
            if (count > 0 && dir != 0) {
                pickerSelected = Battle.clamp(pickerSelected + (emojiPicker && (dir == 4 || dir == 8) ? (dir == 4 ? -10 : 10) : (dir == 4 || dir == 1 ? -1 : 1)), 0, count - 1);
                pickerFirst = Math.min(pickerFirst, pickerSelected);
                pickerFirst = Math.max(pickerFirst, pickerSelected - VISIBLE + 1);
            } else if (ok || key == -6) choosePicker(pickerSelected);
            return true;
        }
        if (key == '#') switchChannel(channel == PUBLIC ? CONTACTS : PUBLIC, "");
        else if (key == '@' && channel >= PUBLIC) button(3);
        else if (key == '/' && channel >= PUBLIC) button(2);
        else if ((key == 13 || key == 10) && channel >= PUBLIC) compose();
        else if (key == '9' && channel >= PUBLIC) older();
        else if (key == -6 && channel >= PUBLIC) compose();
        else if (key == '*') {
            if (channel == CONTACTS) contactsAt = 0;
            else switchChannel(channel, peerName);
        } else if (dir == 4 || dir == 8) moveRow(dir);
        else if (dir == 1 || dir == 2) {
            int target = dir == 1 ? PUBLIC : CONTACTS;
            if (channel != target && !(target == CONTACTS && channel > PUBLIC)) switchChannel(target, "");
        }
        else if (ok || key == -6) {
            chooseRow(selected);
        }
        return true;
    }

    private void choosePicker(int i) {
        if (namePicker) {
            if (i < names.length) insertMention(names[i]);
            else namePicker = false;
        } else {
            draftEmoji = expandedEmoji ? i + 6 : i;
            emojiPicker = false;
        }
    }

    boolean pointer(int x, int y) {
        if (!lobby()) return false;
        if (!opened) {
            if (x >= 278 && x < 320 && y >= 0 && y < 38) { toggle(); return true; }
            return false;
        }
        if (quick.opened) { quick.pointer(x, y); return true; }
        if (emojiPicker && x >= 52 && x < 262 && y >= 58 && y < 58 + 7 * 19) {
            int i = (x - 52) / 21 + (y - 58) / 19 * 10;
            if (i < emojiCount()) choosePicker(i);
            return true;
        }
        if (x >= 282 && x < 304 && y >= 25 && y < 47) {
            if (namePicker || emojiPicker) namePicker = emojiPicker = false;
            else close();
        } else if (namePicker) {
            if (x >= 28 && x < 292 && y >= ROW_Y && y < ROW_Y + VISIBLE * 14) {
                int i = pickerFirst + (y - ROW_Y) / 14;
                if (i < names.length) choosePicker(i);
            }
        } else if (y >= 48 && y < 65 && x >= 22 && x < 194) {
            switchChannel(x < 110 ? PUBLIC : CONTACTS, "");
        } else if (x >= 27 && x < 293 && y >= ROW_Y && y < ROW_Y + visibleRows() * (channel==CONTACTS?14:MESSAGE_H)) {
            int row = (y - ROW_Y) / (channel==CONTACTS?14:MESSAGE_H);
            int index = channel == CONTACTS ? row : Math.min(MESSAGE_ROWS, historyCount - first) - 1 - row;
            if (index >= 0) {
                selected = index;
                chooseRow(index);
            }
        } else if (channel == CONTACTS && y >= 192 && y < 205) {
            shiftContacts(x < 160 ? -VISIBLE : VISIBLE);
        } else if (channel >= PUBLIC && y >= INPUT_Y && y < INPUT_Y + 22) {
            for (int i = 0; i < BUTTON_X.length; i++)
                if (x >= BUTTON_X[i] && x < BUTTON_X[i] + BUTTON_W[i]) {
                    button(i);
                    break;
                }
        }
        return true;
    }

    void drawPreview() {
        if (!available) return;
        if (launchIcon == null) launchIcon = c.combatRoomArt("chat-launch");
        c.img(launchIcon, 291, 4);
        c.pcCenter(c.economy.testCheats ? "Chat" : "Chat (G)", 300, 25);
        if (unread) dot(310, 4);
    }

    private void dot(int x, int y) {
        c.g.setColor(0xff3e3e);
        c.g.fillArc(c.ox + x * c.s, c.oy + y * c.s, 6 * c.s, 6 * c.s, 0, 360);
    }

    void draw() {
        if (!opened || !lobby()) return;
        if (backdrop == null) {
            // Use the same native frame as the shared popup system. Chat only owns
            // the white content surface inside that frame.
            Image popupFrame = c.combatRoomArt("settings743-popup");
            backdrop = NativeUi.fit(popupFrame, PANEL_W * c.s, PANEL_H * c.s, 6 * c.s);
            closeIcon = c.combatRoomArt("bag715-close");
            input = c.combatRoomArt("chat-input");
            quickIcon = c.combatRoomArt("chat-quick");
            smileIcon = c.combatRoomArt("chat-smile");
            sendIcon = c.combatRoomArt("chat-send");
        }
        int open = Math.min(180, (int)(System.currentTimeMillis()-openedAt));
        if (open < 180) {
            int w = 22 + (PANEL_W-22)*open/180, h = 20 + (PANEL_H-20)*open/180;
            c.clip(PANEL_X + PANEL_W-w, PANEL_Y, w, h);
            c.img(backdrop,PANEL_X,PANEL_Y);
            c.all();
            return;
        }
        c.img(backdrop,PANEL_X,PANEL_Y);
        c.clip(PANEL_X + 5, 47, PANEL_W - 10, 13);
        c.img(backdrop,PANEL_X,PANEL_Y - 13);
        c.all();
        HeaderArt.center(c,HeaderArt.CHAT,160,33);
        c.img(closeIcon,283,25);
        // Approved chat layout: white message surface inside the shared yellow popup.
        c.rect(20,66,280,126,0x9b6d31);
        c.rect(22,68,276,122,0xffffff);
        if (namePicker || emojiPicker) {
            drawPicker();
            c.all();
            return;
        }
        c.ui.button("Công khai",22,48,84,17,channel == PUBLIC);
        c.ui.button("Bạn bè",110,48,84,17,channel != PUBLIC);
        if (unread) dot(185, 49);
        if (channel == CONTACTS) drawContacts();
        else drawMessages();
        if (note.length() > 0) c.pcCenter(c.pcShort(note, 258),160,194);
        c.all();
        if (quick.opened) quick.draw();
    }

    private void row(int i, boolean active) {
        if (active) c.drawSelectionGlow(25, ROW_Y + i * 14, 270, 14);
    }

    private void drawContacts() {
        for (int i = 0; i < VISIBLE && first + i < contacts.length; i++) {
            Contact contact = contacts[first + i];
            row(i, i == selected);
            c.pcText(c.pcShort(contact.name, 249), 31, ROW_Y + 2 + i * 14);
            if (contact.unread) dot(278, ROW_Y + 4 + i * 14);
        }
        if (contacts.length == 0 || loading)
            c.pcCenter(loading ? "Đang tải..." : "Chưa có bạn bè", 160, 114);
        if (contacts.length > VISIBLE) {
            c.pcText("Trang trước", 34, 194);
            c.pcText("Trang sau", 223, 194);
        }
    }

    private void shiftContacts(int delta) {
        first = Battle.clamp(first + delta, 0, Math.max(0, contacts.length - VISIBLE));
        selected = 0;
    }

    private final String[] portraitKeys=new String[8];
    private final Image[] portraitImages=new Image[8];
    private int portraitNext;
    private Image avatar(Message m){
        if(m.portraitKey==null)return null;
        String key=m.portraitKey+":"+c.s;
        for(int i=0;i<portraitKeys.length;i++)if(key.equals(portraitKeys[i]))return portraitImages[i];
        Image image=null;
        try{
            // Same composed head layers as the ranking board, cropped at the head.
            int[] pixels=new int[80*100*c.s*c.s];
            if(m.custom){int[] order={0,3,2,1,4};for(int k=0;k<5;k++){String layer=c.layerName(m.sex,order[k],m.look[order[k]],m.mask,"cutin");if(layer!=null)c.blendCutin(pixels,c.load(layer));}}
            else c.blendCutin(pixels,c.load("cutin"+m.sex+"_0_"+m.skin));
            int side=24*c.s;int[] crop=new int[side*side];for(int y=0;y<side;y++)for(int x=0;x<side;x++)crop[y*side+x]=pixels[(y*65*c.s/side)*80*c.s+x*80*c.s/side];
            image=Image.createRGBImage(crop,side,side,true);
        }catch(Exception ignored){}catch(OutOfMemoryError ignored){}
        portraitKeys[portraitNext]=key;portraitImages[portraitNext]=image;portraitNext=(portraitNext+1)%portraitKeys.length;return image;
    }
    private String[] wrapBody(String body,int limit){
        String[] lines={"",""};int end=0;
        while(end<body.length()&&c.textWidth(body.substring(0,end+1))<=limit)end++;
        if(end==body.length()){lines[0]=body;return lines;}
        int space=body.lastIndexOf(' ',end);if(space>0)end=space;
        end=Math.max(1,end);lines[0]=body.substring(0,end);String tail=body.substring(end).trim();
        if(c.textWidth(tail)>limit){while(tail.length()>0&&c.textWidth(tail+"...")>limit)tail=tail.substring(0,tail.length()-1);tail+="...";}
        lines[1]=tail;return lines;
    }
    private void drawMessages() {
        int drawn=Math.min(MESSAGE_ROWS,historyCount-first);
        for(int i=0;i<drawn;i++){
            int index=drawn-1-i;Message m=history[first+index];boolean mine=m.author.equals(c.playerName);int y=ROW_Y+i*MESSAGE_H;
            int ax=mine?269:26;Image head=avatar(m);c.rect(ax,y+9,24,24,0xf0e3c6);
            if(head!=null)c.img(head,ax,y+9);else c.pcCenter("?",ax+12,y+16);
            String[] lines=wrapBody(m.body,m.emoji>=0?174:194);
            int textW=Math.max(c.textWidth(lines[0]),c.textWidth(lines[1]));int w=Math.min(204,Math.max(36,textW+(m.emoji>=0?28:10)));
            int x=mine?263-w:56, tail=mine?x+w:x, tip=mine?tail+6:tail-6;
            c.pcText(c.pcShort(m.author,w),x+2,y);
            int h=lines[1].length()>0?30:18;if(m.emoji>=0)h=Math.max(h,20);
            int by=y+9;int color=mine?0xddeed0:0xf1e6d2;
            c.g.setColor(0xa18b66);c.g.fillRoundRect(c.ox+x*c.s,c.oy+by*c.s,w*c.s,h*c.s,8*c.s,8*c.s);
            c.g.setColor(color);c.g.fillRoundRect(c.ox+(x+1)*c.s,c.oy+(by+1)*c.s,(w-2)*c.s,(h-2)*c.s,7*c.s,7*c.s);
            c.g.fillTriangle(c.ox+tail*c.s,c.oy+(by+6)*c.s,c.ox+tail*c.s,c.oy+(by+12)*c.s,c.ox+tip*c.s,c.oy+(by+9)*c.s);
            c.text(lines[0],x+4,by+2);if(lines[1].length()>0)c.text(lines[1],x+4,by+15);
            if(m.emoji>=0)drawEmoji(m.emoji,x+w-21,by+2);
            if(index==selected)c.drawSelectionGlow(x-1,by-1,w+2,h+1);
        }
        if (historyCount == 0) c.pcCenter(loading ? "Đang tải..." : "Chưa có tin nhắn", 160, 114);
        if (channel > PUBLIC && drawn > 0 && first == 0 && cursor == 0 && !c.typing.active() && !c.popup.opened
                && !quick.opened && !c.presentation.settings && System.currentTimeMillis() >= blockedReadUntil) {
            renderedId = history[0].id;
            renderedChannel = channel;
        }
        c.img(input, 15, INPUT_Y);
        c.rect(18,INPUT_Y + 3,238,16,0x292119);
        c.pcText(c.pcShort(draft.length() > 0 ? draft : "Nhập tin nhắn...", 175), 24, INPUT_Y + 6);
        c.img(sendIcon, 233, INPUT_Y + 4);
        c.img(quickIcon, 257, INPUT_Y + 4);
        c.img(smileIcon, 279, INPUT_Y + 4);
        if (draftEmoji >= 0) drawEmoji(draftEmoji,209,INPUT_Y + 3);
    }

    private String completedMention = "";
    boolean expandedEmoji;
    int emojiCount() { return expandedEmoji ? 61 : 6; }

    void drawEmoji(int i,int x,int y) {
        if (i < 0 || i >= 67) return;
        if(i < 6) {
            c.img(c.emotions.icon(i),x,y-2);
            return;
        }
        i -= 6;
        if (faces == null) faces = c.combatRoomArt("chat-faces");
        c.part(faces, i%10*16,i/10*16,16,16,x,y,false);
    }

    private void drawPicker() {
        if (emojiPicker) {
            if (facePanel == null) facePanel = c.combatRoomArt("chat-face-panel");
            c.img(facePanel,46,35);
            for(int i=0;i<emojiCount();i++) {
                int x=52+i%10*21,y=58+i/10*19;
                if(i==pickerSelected)c.rect(x,y,20,18,0xebce72);
                drawEmoji(expandedEmoji ? i+6 : i,x+2,y);
            }
            return;
        }
        for (int i = pickerFirst; i < names.length && i < pickerFirst + VISIBLE; i++) {
            int y = ROW_Y + (i - pickerFirst) * 14;
            if (i == pickerSelected) c.drawSelectionGlow(27, y, 266, 14);
            c.pcText(names[i], 49, y + 2);
        }
        if (names.length == 0) c.pcCenter("Không tìm thấy người chơi online", 160, 114);
    }
}
