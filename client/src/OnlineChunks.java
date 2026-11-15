import java.io.*;

/** Ordered authoritative animation frames, buffered per movement/POW/volley. */
final class OnlineChunks {
 final OnlineClient net;int turn=-1,completed=-1,id,remaining,kind;boolean terminal,resolved,ownPending;boolean resetTerrain;DataInputStream frames;byte[] finalFrame;
 OnlineChunks(OnlineClient n){net=n;}
 void reset(){turn=completed=-1;frames=null;finalFrame=null;remaining=0;ownPending=false;}
 void read(DataInputStream in,boolean done,int serverTurn)throws IOException{
  if(turn!=serverTurn){reset();turn=serverTurn;}
  int stage=in.readUnsignedByte();if(stage==0||stage==5&&net.liveCap){net.acceptSnapshot(in,done,serverTurn);return;}if(stage==3)return;if(stage!=4)throw new IOException("Sai đoạn hành động.");
  int next=in.readInt(),size=in.readInt();if(size<4||size>1048576)throw new IOException("Đoạn quá lớn.");byte[] data=new byte[size];in.readFully(data);
  if(next<=completed||frames!=null||ownPending)return;if(next!=completed+1)throw new IOException("Thiếu đoạn hành động.");
  id=next;resolved=done;DataInputStream block=new DataInputStream(new ByteArrayInputStream(data));kind=block.readUnsignedByte();terminal=block.readBoolean();remaining=block.readUnsignedShort();if(remaining<1||remaining>2000)throw new IOException("Sai số khung hình.");
  boolean own=net.predicting&&net.predictedTurn==serverTurn&&net.localTurn();
  if(own){while(remaining-->0)finalFrame=frame(block);if(terminal){ownPending=true;finish();}else{completed=id;finalFrame=null;}return;}
  frames=block;resetTerrain=true;net.resolutionSeen=false;net.c.held=net.c.fireKey=0;
 }
 byte[] frame(DataInputStream in)throws IOException{int size=in.readUnsignedShort();byte[] data=new byte[size];in.readFully(data);return data;}
 boolean animate(){if(frames==null)return false;try{if(resetTerrain||terminal&&remaining==1){net.c.resetOnlineTerrain();resetTerrain=false;}net.c.game.ageTrails();net.readSnapshot(new DataInputStream(new ByteArrayInputStream(frame(frames))));for(int i=0;i<net.c.game.ballCount;i++)net.c.game.stampTrail(net.c.game.balls[i]);if(--remaining==0){frames=null;completed=id;net.c.game.pendingNext=terminal&&resolved;net.resolutionSeen=terminal&&(resolved||net.c.game.over);net.lastPoll=0;}return true;}catch(IOException e){net.disconnect("Không đọc được đoạn hành động.");return true;}}
 void finish(){if(!ownPending||(!net.c.game.pendingNext&&!net.c.game.over)||net.c.powShotPending)return;try{net.c.resetOnlineTerrain();net.readSnapshot(new DataInputStream(new ByteArrayInputStream(finalFrame)));net.c.game.pendingNext=resolved;net.predicting=false;net.resolutionSeen=resolved||net.c.game.over;completed=id;ownPending=false;finalFrame=null;net.lastPoll=0;}catch(IOException e){net.disconnect("Không đọc được kết quả.");}}
}
