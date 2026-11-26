import java.io.*;
/** Actual OnlineChunks/FrameInput/SnapshotDelta against real server-produced scene packets.
 * The minimal doubles below isolate queue logic from MIDP rendering and full snapshot parsing.
 */
public final class HF38ChunkCheck {
 static int checks;
 static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);checks++;}
 static OnlineChunks.Segment segment(byte[] data,int id,boolean done)throws Exception{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(id);out.writeInt(data.length);out.write(data);return OnlineChunks.decodeSegment(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),done);
 }
 static byte[] simple(int kind,boolean terminal,int turn)throws Exception{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeByte(kind);out.writeBoolean(terminal);out.writeShort(1);out.writeShort(4);out.writeInt(turn);return bytes.toByteArray();
 }
 public static void main(String[] args)throws Exception{
  OnlineClient n=new OnlineClient();OnlineChunks q=n.chunks;
  try(DataInputStream in=new DataInputStream(new FileInputStream(args[0]))){
   int turn=in.readInt(),size=in.readInt();n.c.game.turn=turn;n.predictedTurn=turn;n.predicting=true;n.c.game.pendingNext=true;n.c.powShotPending=true;
   int expected=0;
   for(int i=0;i<size;i++){
    byte[] bytes=new byte[in.readInt()];in.readFully(bytes);OnlineChunks.Segment seg=segment(bytes,i,i==size-1);expected+=seg.count;
    q.readPrepared(null,i==size-1,turn,4,seg);ok(!n.predicting&&q.hf38ServerAction,"server scene wins over local prediction");
    ok(!n.resolutionSeen,"no early ACK");q.waitingSince=System.currentTimeMillis()-2000;
    while(q.frames!=null)q.animate();ok(q.completed==i,"all frames in segment consumed");
   }
   ok(n.framesRead==expected,"all authoritative transformation frames played");ok(n.resolutionSeen&&n.hf38AckDue&&n.acks==1,"exactly one ACK on final playback");
  }
  q.reset();n.predicting=true;n.predictedTurn=n.c.game.turn=10;n.resolutionSeen=false;n.hf38AckDue=false;n.c.game.pendingNext=true;
  q.readPrepared(null,false,10,4,segment(simple(3,false,10),0,false));ok(q.frames!=null&&!n.predicting&&!n.c.game.pendingNext,"authoritative shot replaces local pending state");
  q.waitingSince=System.currentTimeMillis()-2000;while(q.frames!=null)q.animate();ok(!n.resolutionSeen,"nonterminal shot must not ACK");
  q.readPrepared(null,true,10,4,segment(simple(3,true,10),1,true));while(q.frames!=null)q.animate();ok(n.resolutionSeen&&n.hf38AckDue,"terminal shot sends ACK");
  q.reset();n.predicting=true;n.predictedTurn=n.c.game.turn=11;n.resolutionSeen=false;n.c.game.ballCount=3;n.c.game.bursts=1;n.c.powShotPending=true;
  q.readPrepared(null,true,11,4,segment(simple(1,true,11),0,true));ok(q.ownPending,"own terminal segment queued");q.finish();ok(!q.ownPending&&q.completed==0&&n.resolutionSeen,"terminal checkpoint beats stale local physics");
  q.reset();n.predicting=true;n.predictedTurn=n.c.game.turn=12;n.resolutionSeen=true;q.readPrepared(null,false,13,4,segment(simple(1,false,13),0,false));ok(!n.predicting&&!n.resolutionSeen,"old-turn prediction and ACK discarded");
  q.readPrepared(null,false,13,4,segment(simple(1,false,13),3,false));ok(q.recovering&&n.recoveries==1,"missing segment requests recovery");
  System.out.println("PASS "+checks+" HF38 actual chunk decoder and queue checks (headless doubles)");
 }
}
class Battle {int turn,ballCount,bursts,trailCount;Object[] balls=new Object[20];boolean pendingNext,over;void ageTrails(){}void stampTrail(Object b){}}
class SulanRules {static boolean active(Battle b){return true;}}
class CanvasDouble {Battle game=new Battle();int held,fireKey,powCutinTicks;boolean powShotPending;void message(String s){}void resetOnlineTerrain(){} }
class TcpDouble {int jitter;}
class OnlineClient {
 final OnlineChunks chunks=new OnlineChunks(this);final CanvasDouble c=new CanvasDouble();final TcpDouble tcp=new TcpDouble();
 boolean prefetchCap=true,liveCap,checkpointCap=true,predicting,resolutionSeen,inBattle=true,hf38AckDue;int phase=1,predictedTurn,framesRead,acks,recoveries;long processingSince,lastPoll;
 boolean localTurn(){return true;}void prepareBattle(){}void readSnapshot(DataInputStream in)throws IOException{c.game.turn=in.readInt();while(in.available()>0)in.readByte();c.game.ballCount=c.game.bursts=0;framesRead++;}
 void acceptSnapshot(DataInputStream in,boolean done,int turn)throws IOException{readSnapshot(in);resolutionSeen=done;}
 void hf38SignalAck(){hf38AckDue=true;acks++;}void resyncCommands(){chunks.reset();chunks.recovering=true;predicting=resolutionSeen=false;recoveries++;}void disconnect(String msg){throw new AssertionError(msg);}
}
