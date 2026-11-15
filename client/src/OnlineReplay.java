import java.io.*;

/** A complete resolved shot is played with offline physics, never packet extrapolation. */
final class OnlineReplay {
    final OnlineClient net;
    boolean playing,own,done;
    int turn=-1,finished=-1;
    byte[] result;
    OnlineReplay(OnlineClient n){net=n;}
    void reset(){playing=own=done=false;turn=finished=-1;result=null;}
    byte[] block(DataInputStream in)throws IOException{int size=in.readInt();if(size<0||size>32768)throw new IOException("Sai dữ liệu phát bắn.");byte[] data=new byte[size];in.readFully(data);return data;}
    void read(DataInputStream in,boolean resolved,int serverTurn)throws IOException{
        int stage=in.readUnsignedByte();
        if(stage==0){net.acceptSnapshot(in,resolved,serverTurn);return;}
        if(stage==3)return; // Already buffered; heartbeat only while watching.
        if(stage!=1&&stage!=2)throw new IOException("Sai trạng thái phát bắn.");
        if(net.processingSince==0)net.processingSince=System.currentTimeMillis();
        if(stage==1)return; // No projectile or crater packets while the server resolves.
        byte[] start=block(in),end=block(in);
        if(serverTurn==finished){net.resolutionSeen=resolved;return;}
        if(playing&&turn==serverTurn)return;
        turn=serverTurn;done=resolved;result=end;playing=true;
        own=net.predicting&&net.predictedTurn==serverTurn&&net.localTurn();
        if(!own){
            PhoneCanvas c=net.c;c.resetOnlineTerrain();net.readSnapshot(new DataInputStream(new ByteArrayInputStream(start)));
            Battle b=c.game;b.deferNext=true;b.pendingNext=false;b.units[b.active].human=true;b.charging=false;
            net.predicting=false;net.resolutionSeen=false;c.held=c.fireKey=0;
            if(b.pow){c.startPowCutin();c.powShotPending=true;}else b.fire();
        }
        finish();
    }
    boolean animate() {
        if(!playing||own)return false;
        PhoneCanvas c=net.c;Battle b=c.game;
        if(c.powShotPending){if(--c.powCutinTicks<=0){c.powShotPending=false;b.fire();}return true;}
        b.tick();finish();return true;
    }
    void finish(){
        if(!playing||(!net.c.game.pendingNext&&!net.c.game.over)||net.c.powShotPending)return;
        try{
            net.c.resetOnlineTerrain();net.readSnapshot(new DataInputStream(new ByteArrayInputStream(result)));
            net.c.game.deferNext=true;net.c.game.pendingNext=done;
            net.predicting=false;net.resolutionSeen=done;finished=turn;playing=false;result=null;net.lastPoll=0;
        }catch(IOException e){net.disconnect("Không đọc được kết quả phát bắn.");}
    }
}
