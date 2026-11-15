import java.io.IOException;
import javax.microedition.io.Connection;

/** Read-only readiness probes: never retry an account-changing POST blindly. */
final class ServerWake {
    final OnlineClient net;
    volatile boolean waiting,cancelled;
    volatile int attempts;
    volatile long started;
    long readyAt;String readyEndpoint="";
    int retryMs=5000,limitMs=180000,attemptMs=20000;
    ServerWake(OnlineClient client){net=client;}
    static class Cancelled extends IOException {Cancelled(){super("Đã hủy kết nối.");}}
    static class Rejected extends IOException {Rejected(String message){super(message);}}
    static class Unavailable extends IOException {Unavailable(int code){super("Máy chủ chưa sẵn sàng ("+code+").");}}
    static boolean transientCode(int code){return code==408||code==502||code==503||code==504;}
    void invalidate(){readyAt=0;}
    void healthy(){readyEndpoint=net.endpoint;readyAt=System.currentTimeMillis();}
    void checkCancelled()throws Cancelled{if(cancelled||!net.alive)throw new Cancelled();}
    void cancel(){cancelled=true;net.tcp.close();NetworkGuard.closeLater(net.connection);}
    void ensureReady()throws IOException{
        final int generation=net.networkGeneration;checkCancelled();
        if(readyEndpoint.equals(net.endpoint)&&readyAt>0&&System.currentTimeMillis()-readyAt<60000)return;
        OnlineClient.validateEndpoint(net.endpoint);
        started=System.currentTimeMillis();attempts=0;waiting=true;int previousMode=net.c.mode;net.c.mode=16;
        try{
            while(true){
                if(generation!=net.networkGeneration)throw new Cancelled();checkCancelled();long remaining=limitMs-(System.currentTimeMillis()-started);
                if(remaining<=0)throw new Rejected("Chưa nhận được phản hồi từ máy chủ. Kiểm tra mạng rồi thử lại.");
                attempts++;net.status=attempts==1?"Đang kết nối máy chủ...":"Đang nối lại TCP, vui lòng chờ...";
                try{net.probeHealth((int)Math.min(attemptMs,remaining));checkCancelled();healthy();net.status="Máy chủ đã sẵn sàng";return;}
                catch(Rejected e){throw e;}
                catch(IOException e){if(generation!=net.networkGeneration)throw new Cancelled();checkCancelled();net.status="Đang nối lại TCP, vui lòng chờ...";}
                long next=Math.min(started+limitMs,System.currentTimeMillis()+retryMs);
                while(System.currentTimeMillis()<next){if(generation!=net.networkGeneration)throw new Cancelled();checkCancelled();try{long pause=next-System.currentTimeMillis();if(pause>0)Thread.sleep(Math.min(100,pause));}catch(InterruptedException ignored){}}
            }
        }finally{if(generation==net.networkGeneration)waiting=false;if(generation==net.networkGeneration&&!cancelled&&net.c.mode==16&&previousMode!=16)net.c.mode=previousMode;}
    }
    /** Close a blocked GET so one attempt cannot hold the pending action forever. */
    static final class Deadline implements Runnable {
        final Connection connection;final long end;volatile boolean done;
        Deadline(Connection connection,int milliseconds){this.connection=connection;end=System.currentTimeMillis()+milliseconds;}
        public void run(){while(!done&&System.currentTimeMillis()<end)try{Thread.sleep(100);}catch(InterruptedException ignored){}if(!done)try{connection.close();}catch(Exception ignored){}}
    }
}
