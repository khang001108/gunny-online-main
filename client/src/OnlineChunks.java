import java.io.*;
import java.util.Vector;

/** Bounded receive-ahead queue; receipt and completed playback are separate. */
final class OnlineChunks {
    static final int BUFFER_FRAMES=18, BUFFER_MS=650, MAX_SEGMENTS=5;
    static final int PVP_SHOT_BUFFER_FRAMES=30, PVP_SHOT_BUFFER_MS=1050;
    // HF28: authoritative Sulan scene
    static final int SULAN_SCENE_CHUNK=5;
    boolean sceneReceived,hf38ServerAction;
    final OnlineClient net;
    final Vector queued=new Vector();
    int turn=-1, completed=-1, received=-1, id, remaining, kind, activeBytes;
    static final int MAX_BUFFER_BYTES=128*1024, RECEIVE_RESERVE=65536;
    boolean terminal, resolved, ownPending, resetTerrain, playing;boolean recovering;int recoveryFailures;
    DataInputStream frames;
    byte[] finalFrame,previousFrame;
    long waitingSince,ownSince,stallSince;int underruns;boolean waitingForNext, gapCounted, flightStarted;int rebufferBoost;
    final FrameInput snapshotInput=new FrameInput();
    final DataInputStream snapshotReader=new DataInputStream(snapshotInput);

    static final class Segment {
        int id, kind, count;
        boolean terminal, resolved;
        byte[] data, terminalFrame;
    }
    OnlineChunks(OnlineClient owner) { net=owner; }
    void reset() {
        turn=completed=received=-1;queued.removeAllElements();sceneReceived=hf38ServerAction=false;
        frames=null;finalFrame=previousFrame=null;remaining=activeBytes=0;id=-1;kind=0;terminal=resolved=resetTerrain=false;ownPending=false;playing=false;waitingSince=ownSince=stallSince=0;waitingForNext=gapCounted=flightStarted=false;rebufferBoost=0;snapshotInput.bind(null);
    }
    int requestTurn(int displayedTurn) {
        return net.prefetchCap&&turn>=0 ? turn : displayedTurn;
    }
    int cached() {
        if(recovering)return Integer.MIN_VALUE;
        if (!net.prefetchCap) return frames!=null||ownPending ? id : completed;
        // Values below -1 advertise a full receive window.
        return windowFull() ? -received-2 : received;
    }
    // No canvas access: validate and copy a segment on the network thread.
    static Segment decodeSegment(DataInputStream in,boolean done)throws IOException {
        int next=in.readInt(),size=in.readInt();
        if(size<4||size>65536)throw new IOException("Đoạn quá lớn.");
        byte[] data=new byte[size];in.readFully(data);
        DataInputStream block=new DataInputStream(new ByteArrayInputStream(data));
        Segment segment=new Segment();segment.id=next;segment.kind=block.readUnsignedByte();
        segment.terminal=block.readBoolean();segment.count=block.readUnsignedShort();segment.resolved=done;segment.data=data;
        if(next<0||segment.kind<1||segment.kind>5||segment.count<1||segment.count>2000)throw new IOException("Sai đoạn khung hình.");
        int decodedSize=0,lastKey=4;
        for(int i=0;i<segment.count;i++){
            int offset=data.length-block.available();
            if(block.available()<2)throw new IOException("Thiếu khung hình.");
            if((data[offset]&128)==0)lastKey=offset;
            decodedSize=SnapshotDelta.validate(block,decodedSize);
        }
        if(block.available()!=0)throw new IOException("Dữ liệu hành động dư.");
        // Resolve terminal deltas on the receive thread, not inside the Canvas lock.
        if(segment.terminal){
            block=new DataInputStream(new ByteArrayInputStream(data));block.skipBytes(lastKey);
            byte[] last=null;while(block.available()>0)last=SnapshotDelta.readReusable(block,last);
            segment.terminalFrame=last;
        }
        return segment;
    }
    void read(DataInputStream in,boolean done,int serverTurn)throws IOException {
        int stage=in.readUnsignedByte();
        readPrepared(in,done,serverTurn,stage,stage==4?decodeSegment(in,done):null);
    }
    void readPrepared(DataInputStream in,boolean done,int serverTurn,int stage,Segment segment)throws IOException {
        if(turn!=serverTurn){
            reset();turn=serverTurn;
            if(net.c.game.turn!=serverTurn){net.predicting=false;net.resolutionSeen=false;net.hf38AckDue=false;}
        }
        if(recovering&&stage==0){if(snapshot(in,done,serverTurn)){recovering=false;recoveryFailures=0;}return;}
        if (stage==0 || stage==5&&net.liveCap) { if(net.phase==2){reset();turn=serverTurn;} snapshot(in,done,serverTurn);return; }
        if (stage==3) return;
        if (stage!=4||segment==null) throw new IOException("Sai đoạn hành động.");
        int next=segment.id;
        if (!net.inBattle) { net.prepareBattle();turn=serverTurn; }
        if (next<=received) return;
        if (next!=received+1) { net.resyncCommands();turn=serverTurn;return; }
        if (queued.size()+(frames!=null||ownPending?1:0)>=MAX_SEGMENTS) return;
        boolean own=net.predicting&&net.predictedTurn==serverTurn&&net.localTurn();
        // Do not discard server cinematic frames on the firing client.
        if(segment.kind==SULAN_SCENE_CHUNK){
            if(!sceneReceived){net.predicting=false;net.c.game.pendingNext=false;net.c.powShotPending=false;net.c.powCutinTicks=0;}
            sceneReceived=true;
        }
        // HF38: do not simulate Sulan projectiles using incomplete client snapshots.
        if(SulanRules.active(net.c.game)&&(segment.kind==2||segment.kind==3||segment.kind==4||segment.kind==SULAN_SCENE_CHUNK)){
            if(!hf38ServerAction){
                net.predicting=false;net.c.game.pendingNext=false;
                net.c.powShotPending=false;net.c.powCutinTicks=0;net.c.game.trailCount=0;
            }
            hf38ServerAction=true;
        }
        if(sceneReceived||hf38ServerAction)own=false;
        if (own) {
            // Already validated all frames. Local prediction only needs the terminal state.
            if(!segment.terminal){received=completed=next;return;}
            previousFrame=null;
            id=next;resolved=done;terminal=segment.terminal;kind=segment.kind;
            finalFrame=segment.terminalFrame;
            received=next;
            if (terminal) { ownPending=true;ownSince=System.currentTimeMillis(); }
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
            net.resyncCommands();turn=serverTurn;return false;
        }
    }
    byte[] frame(DataInputStream in)throws IOException {previousFrame=SnapshotDelta.readReusable(in,previousFrame);return previousFrame;}
    void advance() throws IOException {
        if (queued.isEmpty()) return;
        Segment segment=(Segment)queued.elementAt(0);queued.removeElementAt(0);
        // Build receive-ahead before entering a projectile sequence, not mid-flight.
        if(net.prefetchCap&&kind!=3&&kind!=4&&(segment.kind==3||segment.kind==4)&&!flightStarted&&net.c.game.ballCount==0){
            playing=false;waitingSince=System.currentTimeMillis();
        }
        id=segment.id;kind=segment.kind;terminal=segment.terminal;resolved=segment.resolved;
        remaining=segment.count;previousFrame=null;activeBytes=segment.data.length;
        frames=new DataInputStream(new ByteArrayInputStream(segment.data));frames.skipBytes(4);resetTerrain=true;
    }
    boolean windowFull(){
        if(queued.size()+(frames!=null||ownPending?1:0)>=MAX_SEGMENTS)return true;
        int bytes=activeBytes;
        for(int i=0;i<queued.size();i++)bytes+=((Segment)queued.elementAt(i)).data.length;
        return bytes>MAX_BUFFER_BYTES-RECEIVE_RESERVE;
    }
    int bufferedFrames(){int n=remaining;for(int i=0;i<queued.size();i++)n+=((Segment)queued.elementAt(i)).count;return n;}
    boolean ready(long now) {
        if(waitingSince>now)waitingSince=now; // clock rollback cannot hold the prebuffer indefinitely
        int buffered=bufferedFrames();boolean hasTerminal=terminal;
        for (int i=0;i<queued.size();i++) {
            Segment segment=(Segment)queued.elementAt(i);hasTerminal|=segment.terminal;
        }
        if(kind==4)return true; // Complete authoritative flight already in memory.
        if(kind==SULAN_SCENE_CHUNK)return buffered>=24||hasTerminal||windowFull()||now-waitingSince>=BUFFER_MS;
        boolean shot=kind==3;
        int target=shot?PVP_SHOT_BUFFER_FRAMES:BUFFER_FRAMES;
        int delay=shot?PVP_SHOT_BUFFER_MS:BUFFER_MS;
        // Increase receive-ahead only when measured request jitter is high.
        if(net.prefetchCap&&net.tcp.jitter>100){target+=6;delay+=200;}
        target+=rebufferBoost;delay+=rebufferBoost*33;
        return !net.prefetchCap||buffered>=target||hasTerminal||windowFull()||now-waitingSince>=delay;
    }
    // Called under the canvas lock. Continue fetching even if event notifications lag.
    boolean wantsMore() {
        if(!net.prefetchCap||recovering||turn<0||received<0||terminal)return false;
        for(int i=0;i<queued.size();i++)if(((Segment)queued.elementAt(i)).terminal)return false;
        return !windowFull();
    }
    boolean scenePlayback(){return sceneReceived&&(frames!=null||waitingForNext&&!terminal);}
    boolean animate() {
        if (frames==null) {
            if(waitingForNext&&!terminal){
                if(stallSince==0||stallSince>System.currentTimeMillis())stallSince=System.currentTimeMillis();
                if(net.checkpointCap&&net.phase==1&&System.currentTimeMillis()-stallSince>=(sceneReceived?20000:8000)){
                    net.resyncCommands();net.c.message("Đang đồng bộ lại trận Sulan...");return true;
                }
                if(!gapCounted){underruns++;gapCounted=true;rebufferBoost=Math.min(12,rebufferBoost+6);}
                // Still playing a remote turn. Never fall through to prediction/visual ticks.
                return true;
            }
            return false;
        }
        waitingForNext=false;stallSince=0;
        if (!playing) {
            if (!flightStarted&&net.c.game.ballCount==0&&!ready(System.currentTimeMillis())) return true;
            playing=true;
        }
        try {
            resetTerrain=false; // Remote terrain is updated incrementally, never rebuilt per segment.
            net.c.game.ageTrails();
            if(SulanRules.active(net.c.game))net.c.game.pendingNext=false;
            readFrame(frame(frames));
            if(net.c.game.ballCount>0)flightStarted=true;
            for (int i=0;i<net.c.game.ballCount;i++) net.c.game.stampTrail(net.c.game.balls[i]);
            net.resolutionSeen=false;
            if (--remaining==0) {
                frames=null;previousFrame=null;activeBytes=0;completed=id;
                net.c.game.pendingNext=terminal&&resolved;
                net.resolutionSeen=terminal&&(resolved||net.c.game.over);if(net.resolutionSeen){net.processingSince=0;if(SulanRules.active(net.c.game))net.hf38SignalAck();}
                if (queued.isEmpty()) { playing=!terminal&&flightStarted;waitingSince=0;waitingForNext=!terminal;gapCounted=false; }
                else advance();
                net.lastPoll=0;
            }
            return true;
        } catch (IOException error) {
            if(++recoveryFailures<=2){net.resyncCommands();return true;}
            net.disconnect("Không đọc được đoạn hành động: "+error.toString());return true;
        }
    }
    void readFrame(byte[] data)throws IOException {
        snapshotInput.bind(data);
        try{net.readSnapshot(snapshotReader);}finally{snapshotInput.bind(null);}
    }
    void finish() {
        if (!ownPending) return;
        long now=System.currentTimeMillis();if(ownSince>now)ownSince=now;
        long elapsed=ownSince>0?now-ownSince:0;
        // Never replace a predicted projectile with the terminal snapshot mid-flight.
        // A 30s failsafe retains recovery for a genuinely stuck simulation.
        // HF28: pendingNext stops physics. A resolved terminal snapshot must win over stale balls.
        boolean authoritativeEnd=SulanRules.active(net.c.game)&&terminal&&resolved&&finalFrame!=null;
        if(!authoritativeEnd&&elapsed<30000&&(net.c.game.ballCount>0||net.c.game.bursts>0||net.c.powShotPending))return;
        boolean timedOut=authoritativeEnd||elapsed>=5000;
        if(!timedOut&&((!net.c.game.pendingNext&&!net.c.game.over&&!(SulanRules.active(net.c.game)&&resolved))||net.c.powShotPending))return;
        if(timedOut){net.c.powShotPending=false;net.c.powCutinTicks=0;}
        try {
            net.c.resetOnlineTerrain();readFrame(finalFrame);
            net.c.game.pendingNext=resolved;net.predicting=false;net.resolutionSeen=resolved||net.c.game.over;
            completed=id;ownPending=false;ownSince=0;finalFrame=previousFrame=null;if(net.resolutionSeen){net.processingSince=0;if(SulanRules.active(net.c.game))net.hf38SignalAck();}net.lastPoll=0;
        } catch (IOException error) { if(++recoveryFailures<=2)net.resyncCommands();else net.disconnect("Không đọc được kết quả."); }
    }
}

