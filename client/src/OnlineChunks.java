import java.io.*;
import java.util.Vector;

/** Bounded receive-ahead queue; receipt and completed playback are separate. */
final class OnlineChunks {
    static final int BUFFER_FRAMES=15, BUFFER_MS=500, MAX_SEGMENTS=4;
    final OnlineClient net;
    final Vector queued=new Vector();
    int turn=-1, completed=-1, received=-1, id, remaining, kind;
    boolean terminal, resolved, ownPending, resetTerrain, playing;boolean recovering;int recoveryFailures;
    DataInputStream frames;
    byte[] finalFrame,previousFrame;
    long waitingSince,ownSince;

    static final class Segment {
        int id, kind, count;
        boolean terminal, resolved;
        byte[] data;
    }
    OnlineChunks(OnlineClient owner) { net=owner; }
    void reset() {
        turn=completed=received=-1;queued.removeAllElements();
        frames=null;finalFrame=previousFrame=null;remaining=0;id=-1;kind=0;terminal=resolved=resetTerrain=false;ownPending=false;playing=false;waitingSince=ownSince=0;
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
    void read(DataInputStream in, boolean done, int serverTurn) throws IOException {
        if (turn!=serverTurn) { reset();turn=serverTurn; }
        int stage=in.readUnsignedByte();
        if(recovering&&stage==0){if(snapshot(in,done,serverTurn)){recovering=false;recoveryFailures=0;}return;}
        if (stage==0 || stage==5&&net.liveCap) { if(net.phase==2){reset();turn=serverTurn;} snapshot(in,done,serverTurn);return; }
        if (stage==3) return;
        if (stage!=4) throw new IOException("Sai đoạn hành động.");
        int next=in.readInt(), size=in.readInt();
        if (size<4 || size>65536) throw new IOException("Đoạn quá lớn.");
        byte[] data=new byte[size];in.readFully(data);
        // prepareBattle resets this object, so initialize before retaining data.
        if (!net.inBattle) { net.prepareBattle();turn=serverTurn; }
        if (next<=received) return;
        if (next!=received+1) { reset();turn=serverTurn;recovering=true;net.predicting=net.resolutionSeen=false;net.c.held=net.c.fireKey=0;net.revision=0;net.lastPoll=0;return; }
        if (queued.size()+(frames!=null||ownPending?1:0)>=MAX_SEGMENTS) return;
        DataInputStream block=new DataInputStream(new ByteArrayInputStream(data));
        Segment segment=new Segment();
        segment.id=next;segment.kind=block.readUnsignedByte();segment.terminal=block.readBoolean();
        segment.count=block.readUnsignedShort();segment.resolved=done;segment.data=data;
        if (segment.count<1 || segment.count>2000) throw new IOException("Sai số khung hình.");
        int decodedSize=0;for(int i=0;i<segment.count;i++)decodedSize=SnapshotDelta.validate(block,decodedSize);
        if (block.available()!=0) throw new IOException("Dữ liệu hành động dư.");
        boolean own=net.predicting&&net.predictedTurn==serverTurn&&net.localTurn();
        if (own) {
            previousFrame=null;block=new DataInputStream(new ByteArrayInputStream(data));block.skipBytes(4);
            id=next;resolved=done;terminal=segment.terminal;kind=segment.kind;
            for (int i=0;i<segment.count;i++) finalFrame=frame(block);
            received=next;
            if (terminal) { ownPending=true;ownSince=System.currentTimeMillis();finish(); }
            else { completed=id;finalFrame=previousFrame=null; }
            return;
        }
        queued.addElement(segment);received=next;
        if (waitingSince==0) waitingSince=System.currentTimeMillis();
        if (frames==null) advance();
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
        id=segment.id;kind=segment.kind;terminal=segment.terminal;resolved=segment.resolved;
        remaining=segment.count;previousFrame=null;
        frames=new DataInputStream(new ByteArrayInputStream(segment.data));frames.skipBytes(4);resetTerrain=true;
    }
    boolean ready(long now) {
        int buffered=remaining;boolean hasTerminal=terminal;
        for (int i=0;i<queued.size();i++) {
            Segment segment=(Segment)queued.elementAt(i);buffered+=segment.count;hasTerminal|=segment.terminal;
        }
        return !net.prefetchCap||buffered>=BUFFER_FRAMES||hasTerminal||now-waitingSince>=BUFFER_MS;
    }
    boolean animate() {
        if (frames==null) return false;
        if (!playing) {
            if (!ready(System.currentTimeMillis())) return true;
            playing=true;
        }
        try {
            resetTerrain=false; // Remote terrain is updated incrementally, never rebuilt per segment.
            net.c.game.ageTrails();
            net.readSnapshot(new DataInputStream(new ByteArrayInputStream(frame(frames))));
            for (int i=0;i<net.c.game.ballCount;i++) net.c.game.stampTrail(net.c.game.balls[i]);
            net.resolutionSeen=false;
            if (--remaining==0) {
                frames=null;previousFrame=null;completed=id;
                net.c.game.pendingNext=terminal&&resolved;
                net.resolutionSeen=terminal&&(resolved||net.c.game.over);if(net.resolutionSeen)net.processingSince=0;
                if (queued.isEmpty()) { playing=false;waitingSince=0; }
                else advance();
                net.lastPoll=0;
            }
            return true;
        } catch (IOException error) {
            if(++recoveryFailures<=2){reset();recovering=true;net.predicting=net.resolutionSeen=false;net.c.held=net.c.fireKey=0;net.revision=0;net.lastPoll=0;return true;}
            net.disconnect("Không đọc được đoạn hành động: "+error.toString());return true;
        }
    }
    void finish() {
        if (!ownPending) return;
        boolean timedOut=ownSince>0&&System.currentTimeMillis()-ownSince>=5000;
        if(!timedOut&&((!net.c.game.pendingNext&&!net.c.game.over&&!(SulanRules.active(net.c.game)&&resolved))||net.c.powShotPending))return;
        if(timedOut){net.c.powShotPending=false;net.c.powCutinTicks=0;}
        try {
            net.c.resetOnlineTerrain();net.readSnapshot(new DataInputStream(new ByteArrayInputStream(finalFrame)));
            net.c.game.pendingNext=resolved;net.predicting=false;net.resolutionSeen=resolved||net.c.game.over;
            completed=id;ownPending=false;ownSince=0;finalFrame=previousFrame=null;if(net.resolutionSeen)net.processingSince=0;net.lastPoll=0;
        } catch (IOException error) { net.disconnect("Không đọc được kết quả."); }
    }
}

