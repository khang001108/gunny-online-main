import java.io.*;
import java.util.Vector;

/** Bounded receive-ahead queue; receipt and completed playback are separate. */
final class OnlineChunks {
    static final int BUFFER_FRAMES=18, BUFFER_MS=650, MAX_SEGMENTS=5;
    static final int PVP_SHOT_BUFFER_FRAMES=30, PVP_SHOT_BUFFER_MS=1050;
    final OnlineClient net;
    final Vector queued=new Vector();
    int turn=-1, completed=-1, received=-1, id, remaining, kind;
    boolean terminal, resolved, ownPending, resetTerrain, playing;boolean recovering;int recoveryFailures;
    DataInputStream frames;
    byte[] finalFrame,previousFrame;
    long waitingSince,ownSince;int underruns;boolean waitingForNext, gapCounted;int rebufferBoost;
    final FrameInput snapshotInput=new FrameInput();
    final DataInputStream snapshotReader=new DataInputStream(snapshotInput);

    static final class Segment {
        int id, kind, count;
        boolean terminal, resolved;
        byte[] data;
    }
    OnlineChunks(OnlineClient owner) { net=owner; }
    void reset() {
        turn=completed=received=-1;queued.removeAllElements();
        frames=null;finalFrame=previousFrame=null;remaining=0;id=-1;kind=0;terminal=resolved=resetTerrain=false;ownPending=false;playing=false;waitingSince=ownSince=0;waitingForNext=gapCounted=false;rebufferBoost=0;snapshotInput.bind(null);
    }
    int requestTurn(int displayedTurn) {
        return net.prefetchCap&&turn>=0 ? turn : displayedTurn;
    }
    int cached() {
        if(recovering)return Integer.MIN_VALUE;
        if (!net.prefetchCap) return frames!=null||ownPending ? id : completed;
        // Values below -1 advertise a full receive window.
        return queued.size()+(frames!=null||ownPending?1:0)>=MAX_SEGMENTS ? -received-2 : received;
    }
    // No canvas access: validate and copy a segment on the network thread.
    static Segment decodeSegment(DataInputStream in,boolean done)throws IOException {
        int next=in.readInt(),size=in.readInt();
        if(size<4||size>65536)throw new IOException("Đoạn quá lớn.");
        byte[] data=new byte[size];in.readFully(data);
        DataInputStream block=new DataInputStream(new ByteArrayInputStream(data));
        Segment segment=new Segment();segment.id=next;segment.kind=block.readUnsignedByte();
        segment.terminal=block.readBoolean();segment.count=block.readUnsignedShort();segment.resolved=done;segment.data=data;
        if(segment.count<1||segment.count>2000)throw new IOException("Sai số khung hình.");
        int decodedSize=0;for(int i=0;i<segment.count;i++)decodedSize=SnapshotDelta.validate(block,decodedSize);
        if(block.available()!=0)throw new IOException("Dữ liệu hành động dư.");
        return segment;
    }
    void read(DataInputStream in,boolean done,int serverTurn)throws IOException {
        int stage=in.readUnsignedByte();
        readPrepared(in,done,serverTurn,stage,stage==4?decodeSegment(in,done):null);
    }
    void readPrepared(DataInputStream in,boolean done,int serverTurn,int stage,Segment segment)throws IOException {
        if (turn!=serverTurn) { reset();turn=serverTurn; }
        if(recovering&&stage==0){if(snapshot(in,done,serverTurn)){recovering=false;recoveryFailures=0;}return;}
        if (stage==0 || stage==5&&net.liveCap) { if(net.phase==2){reset();turn=serverTurn;} snapshot(in,done,serverTurn);return; }
        if (stage==3) return;
        if (stage!=4||segment==null) throw new IOException("Sai đoạn hành động.");
        int next=segment.id;byte[] data=segment.data;
        if (!net.inBattle) { net.prepareBattle();turn=serverTurn; }
        if (next<=received) return;
        if (next!=received+1) { reset();turn=serverTurn;recovering=true;net.predicting=net.resolutionSeen=false;net.c.held=net.c.fireKey=0;net.revision=0;net.lastPoll=0;return; }
        if (queued.size()+(frames!=null||ownPending?1:0)>=MAX_SEGMENTS) return;
        boolean own=net.predicting&&net.predictedTurn==serverTurn&&net.localTurn();
        if (own) {
            // Already validated all frames. Local prediction only needs the terminal state.
            if(!segment.terminal){received=completed=next;return;}
            previousFrame=null;DataInputStream block=new DataInputStream(new ByteArrayInputStream(data));block.skipBytes(4);
            id=next;resolved=done;terminal=segment.terminal;kind=segment.kind;
            for (int i=0;i<segment.count;i++) finalFrame=frame(block);
            received=next;
            if (terminal) { ownPending=true;ownSince=System.currentTimeMillis();finish(); }
            else { completed=id;finalFrame=previousFrame=null; }
            return;
        }
        queued.addElement(segment);received=next;
        if (waitingSince==0) waitingSince=System.currentTimeMillis();
        if (frames==null) {advance();waitingForNext=false;gapCounted=false;}
        net.resolutionSeen=false;net.c.held=net.c.fireKey=0;
    }
    boolean snapshot(DataInputStream in,boolean done,int serverTurn)throws IOException {
        try {net.acceptSnapshot(in,done,serverTurn);recoveryFailures=0;return true;}
        catch(IOException invalid){
            if(++recoveryFailures>2)throw invalid;
            reset();turn=serverTurn;recovering=true;net.predicting=net.resolutionSeen=false;
            net.c.held=net.c.fireKey=0;net.revision=0;net.lastPoll=0;return false;
        }
    }
    byte[] frame(DataInputStream in)throws IOException {previousFrame=SnapshotDelta.readReusable(in,previousFrame);return previousFrame;}
    void advance() throws IOException {
        if (queued.isEmpty()) return;
        Segment segment=(Segment)queued.elementAt(0);queued.removeElementAt(0);
        // Build receive-ahead before entering a projectile sequence, not mid-flight.
        if(net.prefetchCap&&kind!=3&&segment.kind==3){
            playing=false;waitingSince=System.currentTimeMillis();
        }
        id=segment.id;kind=segment.kind;terminal=segment.terminal;resolved=segment.resolved;
        remaining=segment.count;previousFrame=null;
        frames=new DataInputStream(new ByteArrayInputStream(segment.data));frames.skipBytes(4);resetTerrain=true;
    }
    int bufferedFrames(){int n=remaining;for(int i=0;i<queued.size();i++)n+=((Segment)queued.elementAt(i)).count;return n;}
    boolean ready(long now) {
        int buffered=bufferedFrames();boolean hasTerminal=terminal;
        for (int i=0;i<queued.size();i++) {
            Segment segment=(Segment)queued.elementAt(i);hasTerminal|=segment.terminal;
        }
        boolean shot=kind==3;
        int target=shot?PVP_SHOT_BUFFER_FRAMES:BUFFER_FRAMES;
        int delay=shot?PVP_SHOT_BUFFER_MS:BUFFER_MS;
        // Increase receive-ahead only when measured request jitter is high.
        if(net.prefetchCap&&net.tcp.jitter>100){target+=6;delay+=200;}
        target+=rebufferBoost;delay+=rebufferBoost*33;
        return !net.prefetchCap||buffered>=target||hasTerminal||queued.size()+1>=MAX_SEGMENTS||now-waitingSince>=delay;
    }
    // Called under the canvas lock. Continue fetching even if event notifications lag.
    boolean wantsMore() {
        if(!net.prefetchCap||recovering||turn<0||received<0||terminal)return false;
        for(int i=0;i<queued.size();i++)if(((Segment)queued.elementAt(i)).terminal)return false;
        return queued.size()+(frames!=null||ownPending?1:0)<MAX_SEGMENTS;
    }
    boolean animate() {
        if (frames==null) {
            if(waitingForNext&&!terminal){
                if(!gapCounted){underruns++;gapCounted=true;rebufferBoost=Math.min(12,rebufferBoost+6);}
                // Still playing a remote turn. Never fall through to prediction/visual ticks.
                return true;
            }
            return false;
        }
        waitingForNext=false;
        if (!playing) {
            if (!ready(System.currentTimeMillis())) return true;
            playing=true;
        }
        try {
            resetTerrain=false; // Remote terrain is updated incrementally, never rebuilt per segment.
            net.c.game.ageTrails();
            readFrame(frame(frames));
            for (int i=0;i<net.c.game.ballCount;i++) net.c.game.stampTrail(net.c.game.balls[i]);
            net.resolutionSeen=false;
            if (--remaining==0) {
                frames=null;previousFrame=null;completed=id;
                net.c.game.pendingNext=terminal&&resolved;
                net.resolutionSeen=terminal&&(resolved||net.c.game.over);if(net.resolutionSeen)net.processingSince=0;
                if (queued.isEmpty()) { playing=false;waitingSince=0;waitingForNext=!terminal;gapCounted=false; }
                else advance();
                net.lastPoll=0;
            }
            return true;
        } catch (IOException error) {
            if(++recoveryFailures<=2){reset();recovering=true;net.predicting=net.resolutionSeen=false;net.c.held=net.c.fireKey=0;net.revision=0;net.lastPoll=0;return true;}
            net.disconnect("Không đọc được đoạn hành động: "+error.toString());return true;
        }
    }
    void readFrame(byte[] data)throws IOException {
        snapshotInput.bind(data);
        try{net.readSnapshot(snapshotReader);}finally{snapshotInput.bind(null);}
    }
    void finish() {
        if (!ownPending) return;
        boolean timedOut=ownSince>0&&System.currentTimeMillis()-ownSince>=5000;
        if(!timedOut&&((!net.c.game.pendingNext&&!net.c.game.over&&!(SulanRules.active(net.c.game)&&resolved))||net.c.powShotPending))return;
        if(timedOut){net.c.powShotPending=false;net.c.powCutinTicks=0;}
        try {
            net.c.resetOnlineTerrain();readFrame(finalFrame);
            net.c.game.pendingNext=resolved;net.predicting=false;net.resolutionSeen=resolved||net.c.game.over;
            completed=id;ownPending=false;ownSince=0;finalFrame=previousFrame=null;if(net.resolutionSeen)net.processingSince=0;net.lastPoll=0;
        } catch (IOException error) { net.disconnect("Không đọc được kết quả."); }
    }
}

