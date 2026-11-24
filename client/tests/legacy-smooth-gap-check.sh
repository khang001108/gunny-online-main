#!/usr/bin/env bash
# Standalone regression test: real OnlineChunks with small fake game, no emulator.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
cp src/OnlineChunks.java src/FrameInput.java src/SnapshotDelta.java "$work/"
cat > "$work/ChunkGapCheck.java" <<'JAVA'
import java.io.*;
class OnlineClient {
 boolean prefetchCap=true,inBattle=true,predicting,resolutionSeen,liveCap;int phase=1,roomKind=0,predictedTurn,revision;long lastPoll,processingSince;final Canvas c=new Canvas();final Tcp tcp=new Tcp();
 boolean localTurn(){return false;}void prepareBattle(){}void acceptSnapshot(DataInputStream in,boolean done,int turn)throws IOException{}void readSnapshot(DataInputStream in)throws IOException{c.game.value=in.readUnsignedByte();if(in.available()!=0)throw new IOException("bad frame");}void disconnect(String error){throw new AssertionError(error);}
 static class Canvas {int held,fireKey,powCutinTicks;boolean powShotPending;final Game game=new Game();void resetOnlineTerrain(){}}
 static class Game {int ballCount,value;Object[] balls=new Object[0];boolean pendingNext,over;void ageTrails(){}void stampTrail(Object a){}}
 static class Tcp {int jitter;}
}
class SulanRules {static boolean active(OnlineClient.Game game){return false;}}
public class ChunkGapCheck {
 static void ok(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 static DataInputStream packet(int id,boolean terminal,int kind,int count,int start)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeByte(4);out.writeInt(id);out.writeInt(4+3*count);out.writeByte(kind);out.writeBoolean(terminal);out.writeShort(count);for(int i=0;i<count;i++){out.writeShort(1);out.writeByte(start+i);}return new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));}
 public static void main(String[] args)throws Exception{OnlineClient net=new OnlineClient();OnlineChunks chunks=new OnlineChunks(net);
 chunks.read(packet(0,false,3,6,0),false,1);ok(!chunks.ready(chunks.waitingSince),"initial buffer");chunks.waitingSince-=3000;for(int i=0;i<6;i++)ok(chunks.animate(),"playing");ok(chunks.completed==0&&!net.resolutionSeen,"nonterminal");
 for(int i=0;i<10;i++)ok(chunks.animate(),"gap still owns simulation");ok(chunks.underruns==1&&chunks.rebufferBoost==6,"gap counted once");ok(chunks.wantsMore(),"refill");
 DataInputStream in=packet(1,false,3,6,6);int stage=in.readUnsignedByte();OnlineChunks.Segment prepared=OnlineChunks.decodeSegment(in,false);chunks.readPrepared(in,false,1,stage,prepared);ok(!chunks.ready(chunks.waitingSince),"rebuffer");
 chunks.read(packet(2,true,3,3,12),true,1);ok(chunks.ready(chunks.waitingSince),"terminal releases buffer");for(int i=0;i<9;i++)ok(chunks.animate(),"ordered replay");ok(chunks.completed==2&&net.resolutionSeen&&net.c.game.value==14,"terminal applied in order");ok(!chunks.animate(),"done");
 chunks.reset();for(int i=0;i<5;i++)chunks.read(packet(i,false,1,1,i),false,2);ok(chunks.cached()==-6&&chunks.ready(chunks.waitingSince),"full small-segment window starts without deadlock");System.out.println("PASS: gap ownership, adaptive refill, staged decode, terminal ordering, full window");}
}
JAVA
javac -encoding UTF-8 -d "$work" "$work"/*.java
java -cp "$work" ChunkGapCheck
