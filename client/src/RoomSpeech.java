import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.microedition.lcdui.Image;

/** Ephemeral speech tied to the actor's current room and phase. */
final class RoomSpeech {
    private static final int WAITING = 1, FIGHT = 2;
    private final LobbyChat chat;
    private final OnlineClient net;
    private final PhoneCanvas c;
    private final QuickChat quick;
    private final String[] messages = new String[6];
    private final int[] emojis = new int[6];
    private final long[] until = new long[6];
    private final int[] headX = new int[6], headY = new int[6];
    private final boolean[] seen = new boolean[6];
    private final String[] bubbleTexts=new String[6];
    private final String[][] bubbleLines=new String[6][];
    private final boolean[] bubbleHud=new boolean[6];
    boolean supported;
    private boolean bar, picker;
    private int selected, lastContext, lastRoom, sendRoom, sendPhase, sendEmoji;
    private String outgoing;
    private long pollAt;
    private Image input, sendIcon, quickIcon, smile;

    RoomSpeech(LobbyChat owner, OnlineClient client, QuickChat phrases) {
        chat = owner;
        net = client;
        c = client.c;
        quick = phrases;
    }

    int context() {
        if ((c.mode == 1 || c.mode == 3) && !c.game.over) return FIGHT;
        if (c.mode == 10 || c.mode == 19 && !c.offlineDungeon.picker) return WAITING;
        if (c.mode == 16 && net.logged && net.roomId > 0 && net.phase == 0
                && !net.social.opened && !net.create.opened && !c.dungeon.picker) return WAITING;
        return 0;
    }

    void reset() {
        if (lastContext != 0) quick.opened = false;
        lastContext = lastRoom = 0;
        bar = picker = false;
        outgoing = null;
        pollAt = 0;
        for (int i = 0; i < until.length; i++) { until[i] = 0; seen[i] = false; messages[i]=bubbleTexts[i]=null;bubbleLines[i]=null; }
    }

    private int refresh() {
        int current = context();
        if (current != lastContext || net.roomId != lastRoom) {
            reset();
            lastContext = current;
            lastRoom = net.roomId;
        }
        return current;
    }

    boolean key(int key, int dir, boolean ok) {
        int current = refresh();
        if (current == 0) return false;
        if (quick.opened) { quick.key(key,dir,ok); return true; }
        if (picker) {
            if (key == -7 || key == '@') picker = false;
            else if (ok || key == -6) chooseEmoji(selected);
            else if (dir != 0) {
                int delta = dir == 1 ? -1 : dir == 2 ? 1 : dir == 4 ? -10 : 10;
                selected = Battle.clamp(selected+delta,0,chat.emojiCount()-1);
            }
            return true;
        }
        if (key == '/') {
            bar = current == WAITING;
            quick.open();
            c.held = c.fireKey = c.cameraHeld = 0;
            return true;
        }
        if (key == 13 || key == 10) {
            if (current==FIGHT && (c.fireKey!=0 || c.game.charging && c.isLocalTurn())) { c.message("Thả phím bắn trước khi chat."); return true; }
            if(c.mode==3)c.exitBuffs();
            compose(); return true;
        }
        if (current != WAITING) return false;
        if (key == '@') { bar = picker = true; selected = 0; return true; }
        if (bar && key == -7) { bar = false; return true; }
        return false;
    }

    private void compose() {
        bar = true;
        c.typing.field("Trò chuyện","",96,false,false,14,210,224,new InlineText.Done() {
            public void accept(String value) { if(context()==FIGHT)bar=false; send(value,-1); }
        });
    }

    private void chooseEmoji(int index) {
        picker = false;
        send("",chat.expandedEmoji ? index+6 : index);
    }

    boolean pointer(int x, int y) {
        if (refresh()==0) return false;
        if (quick.opened) { quick.pointer(x,y); return true; }
        if (picker) {
            if (x>=52 && x<262 && y>=58 && y<191) {
                int index=(x-52)/21+(y-58)/19*10;
                if (index<chat.emojiCount()) chooseEmoji(index);
            } else picker=false;
            return true;
        }
        if (!bar || y<207 || y>=230 || x<12 || x>=309) return false;
        if (x>=278) { picker=true; selected=0; }
        else if (x>=255) quick.open();
        else compose();
        return true;
    }

    void send(String value, int emoji) {
        int current=refresh();
        value=value.trim();
        if (current==0 || value.length()==0 && emoji<0) return;
        if (value.length()>96) value=value.substring(0,96);
        if (net.roomId==0) {
            show(0,value,emoji,3800);
        } else {
            if (!supported) {c.message("Cập nhật máy chủ 7.8.3 để dùng chat trong phòng.");return;}
            if (outgoing!=null) {c.message("Đang gửi tin nhắn trước.");return;}
            sendRoom=net.roomId;
            sendPhase=current-1;
            sendEmoji=emoji;
            outgoing=value;
        }
    }

    String nextRequest(long now) {
        int current=refresh();
        if (!supported || current==0 || net.roomId<=0 || net.phase!=current-1) return "";
        if (outgoing!=null) return "chatroomsend";
        if (now<pollAt) return "";
        pollAt=now+900;
        return "chatroom";
    }

    void write(String op,DataOutputStream out)throws IOException {
        if (op.equals("chatroomsend")) {
            out.writeInt(sendRoom);
            out.writeByte(sendPhase);
            out.writeUTF(outgoing==null?"":outgoing);
            out.writeByte(sendEmoji);
        } else {
            out.writeInt(net.roomId);
            out.writeByte(lastContext-1);
        }
    }

    void read(DataInputStream in)throws IOException {
        int room=in.readInt(), phase=in.readUnsignedByte(), count=in.readUnsignedByte();
        if (count>6) throw new IOException("Sai dữ liệu bong bóng.");
        if (room!=net.roomId || phase!=net.phase || refresh()!=phase+1) return;
        long now=System.currentTimeMillis();
        for (int i=0;i<until.length;i++) until[i]=0;
        for (int i=0;i<count;i++) {
            int actor=in.readUnsignedByte(), time=in.readUnsignedShort();
            String text=in.readUTF();
            int emoji=in.readByte();
            if(actor<6 && time>0 && time<=4000 && text.length()<=96 && emoji>=-1 && emoji<=66)
                show(actor,text,emoji,time);
        }
    }

    void sent() { outgoing=null; }

    void failed(String op,String message) {
        if(op.equals("chatroomsend")) {
            outgoing=null;
            c.message(message==null?"Không gửi được tin nhắn.":message);
        }
        pollAt=System.currentTimeMillis()+3000;
    }

    private void show(int actor,String text,int emoji,int time) {
        messages[actor]=text;
        emojis[actor]=emoji;
        until[actor]=System.currentTimeMillis()+time;
    }

    void beginFrame() { refresh(); for(int i=0;i<seen.length;i++) seen[i]=false; }

    void head(int actor,int x,int y) {
        if(actor<0||actor>=seen.length)return;
        headX[actor]=x;
        headY[actor]=y;
        seen[actor]=true;
    }

    void draw() {
        int current=refresh();
        if (current==0) return;
        c.all();
        long now=System.currentTimeMillis();
        for(int i=0;i<seen.length;i++)
            if(seen[i]&&until[i]>now && headX[i]>=0 && headX[i]<320 && headY[i]>=12 && headY[i]<235)
                bubble(i,headX[i],headY[i],messages[i],emojis[i],current==FIGHT);
        if(current==FIGHT)for(int i=0;i<seen.length;i++)
            if(seen[i]&&until[i]<=now&&c.emotions.until[i]>now)
                bubble(i,headX[i],headY[i],"",c.emotions.values[i],true);
        if(current==FIGHT && !c.typing.active())bar=false;
        if (bar) drawBar();
        if (picker) drawPicker();
        if (quick.opened) quick.draw();
    }

    private void bubble(int actor,int x,int y,String text,int emoji,boolean hud) {
        String[] lines=bubbleLines[actor];
        if(lines==null||bubbleHud[actor]!=hud||!text.equals(bubbleTexts[actor])){
            int widthLimit=hud?115:145;
            if(text.length()==0)lines=hud?new String[]{"     ","     "}:new String[]{"     "};
            else if(c.pcShort(text,widthLimit).equals(text))lines=new String[]{text};
            else {
                int split=1;
                while(split<text.length()&&c.pcWidth(text.substring(0,split+1))<=widthLimit)split++;
                int space=text.lastIndexOf(' ',split);
                if(space>0)split=space;
                lines=new String[]{text.substring(0,split),c.pcShort(text.substring(split).trim(),widthLimit)};
            }
            bubbleTexts[actor]=text;bubbleHud[actor]=hud;bubbleLines[actor]=lines;
        }
        int top=36+actor/2*36;
        if(hud)c.drawSpeechBubble(x,y,lines,top,true);
        else c.drawSpeechBubble(x,y,lines,4);
        if(text.length()==0){
            if(hud){
                int width=c.pcWidth(lines[0])+20;
                int left=c.speechBubbleLeft(x,width,true);
                chat.drawEmoji(emoji,left+(width-22)/2,Math.max(top,y+9)+5);
            }else chat.drawEmoji(emoji,Battle.clamp(x-8,10,294),Math.max(9,y-32));
        }
    }

    private void drawBar() {
        if (input==null) {
            input=c.combatRoomArt("chat-input");
            sendIcon=c.combatRoomArt("chat-send");
            quickIcon=c.combatRoomArt("chat-quick");
            smile=c.combatRoomArt("chat-smile");
        }
        c.img(input,15,207);
        c.rect(14,210,245,16,0x292119);
        c.pcText("Nhập tin nhắn...",20,213);
        c.img(sendIcon,239,211);
        c.img(quickIcon,260,211);
        c.img(smile,283,211);
    }

    private void drawPicker() {
        c.panel(46,46,226,152);
        c.pcCenter("Cảm xúc",160,47);
        for(int i=0;i<chat.emojiCount();i++) {
            int x=52+i%10*21, y=58+i/10*19;
            if(i==selected)c.rect(x,y,20,18,0xebce72);
            chat.drawEmoji(chat.expandedEmoji?i+6:i,x+2,y);
        }
    }
}
