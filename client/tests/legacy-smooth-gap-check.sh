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
 boolean own;boolean localTurn(){return own;}void prepareBattle(){}void acceptSnapshot(DataInputStream in,boolean done,int turn)throws IOException{}void readSnapshot(DataInputStream in)throws IOException{c.game.value=in.readUnsignedByte();c.game.ballCount=c.game.value>=100?1:0;if(in.available()!=0)throw new IOException("bad frame");}void disconnect(String error){throw new AssertionError(error);}
 static class Canvas {int held,fireKey,powCutinTicks;boolean powShotPending;final Game game=new Game();void resetOnlineTerrain(){}}
 static class Game {int ballCount,value,bursts;Object[] balls=new Object[1];boolean pendingNext,over;void ageTrails(){}void stampTrail(Object a){}}
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
 chunks.reset();for(int i=0;i<5;i++)chunks.read(packet(i,false,1,1,i),false,2);ok(chunks.cached()==-6&&chunks.ready(chunks.waitingSince),"full small-segment window starts without deadlock");
 // First block tagged movement already has a visible projectile. Changing kind must not pause.
 chunks.reset();chunks.read(packet(0,false,1,2,100),false,3);chunks.waitingSince-=3000;chunks.animate();
 chunks.read(packet(1,false,3,2,102),false,3);chunks.animate();ok(chunks.playing,"no pause at movement to shot boundary");chunks.animate();ok(net.c.game.value==102,"first shot block plays immediately");chunks.animate();ok(chunks.waitingForNext,"in-flight underrun");chunks.animate();
 chunks.read(packet(2,false,3,1,104),false,3);chunks.animate();ok(net.c.game.value==104,"in-flight refill plays next tick without second startup delay");
 chunks.read(packet(3,true,3,1,5),true,3);chunks.animate();ok(net.c.game.value==5&&net.resolutionSeen,"terminal state exactly once");
 // Own shooter bypasses remote playback, even when a terminal state arrives early.
 chunks.reset();net.own=true;net.predicting=true;net.predictedTurn=4;net.c.game.ballCount=1;net.c.game.bursts=1;net.c.game.pendingNext=false;
 chunks.read(packet(0,false,3,2,110),false,4);ok(chunks.frames==null&&chunks.completed==0,"own nonterminal bypass");chunks.read(packet(1,true,3,1,7),true,4);ok(chunks.ownPending&&chunks.frames==null,"own terminal queued");chunks.ownSince-=6000;chunks.finish();ok(chunks.ownPending&&net.c.game.ballCount==1,"no timeout overwrite during own shot");net.c.game.ballCount=net.c.game.bursts=0;net.c.game.pendingNext=true;chunks.finish();ok(!chunks.ownPending&&net.c.game.value==7&&net.resolutionSeen,"own authoritative finish");
 // A full shot follows movement, starts without another startup wait and finishes without a single refill.
 chunks.reset();net.own=false;net.predicting=false;net.c.game.ballCount=0;net.c.game.bursts=0;net.c.game.pendingNext=false;
 chunks.read(packet(0,false,1,2,10),false,5);chunks.waitingSince-=3000;chunks.animate();chunks.animate();
 chunks.read(packet(1,true,4,48,100),true,5);ok(chunks.ready(chunks.waitingSince),"complete shot bypasses startup delay");ok(!chunks.wantsMore(),"complete shot does not fetch more");
 int oldGaps=chunks.underruns;for(int i=0;i<48;i++){ok(chunks.animate(),"complete shot frame");ok(net.c.game.value==100+i,"complete shot ordered frame");}
 ok(chunks.underruns==oldGaps&&net.resolutionSeen&&chunks.completed==1,"no gaps in complete shot");
 chunks.reset();chunks.activeBytes=65537;ok(chunks.windowFull(),"byte budget reserves enough space for next packet");chunks.activeBytes=0;ok(!chunks.windowFull(),"byte budget releases after playback");
 System.out.println("PASS: full shot playback and bounded compressed queue");
 System.out.println("PASS: gap ownership, adaptive refill, staged decode, terminal ordering, full window, shot boundary, midflight resume, own shooter guard");}
}
JAVA
javac -encoding UTF-8 -d "$work" "$work"/*.java
java -cp "$work" ChunkGapCheck
