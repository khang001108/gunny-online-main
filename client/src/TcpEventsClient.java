import java.io.*;

/** Blocking change notifications use a second TCP socket so commands never wait behind them. */
final class TcpEventsClient implements Runnable {
 final OnlineClient n;final TcpTransport transport=new TcpTransport();Thread thread;
 volatile boolean failed;volatile long signal;long consumed,lastStamp;int failures;
 TcpEventsClient(OnlineClient client){n=client;}
 boolean active(){return n.eventCap&&!failed&&n.logged&&n.roomId>0&&!n.soloWaiting();}
 void ensure(){if(active()&&(thread==null||!thread.isAlive())){thread=new Thread(this);thread.start();}}
 long stamp(){return n.c.game.turn*1000003L+(n.resolutionSeen?17:0)+(n.c.loadTicks>=60?31:0)+n.chunks.completed*79L+(n.chunks.frames!=null||n.chunks.ownPending?n.chunks.id:n.chunks.completed)*103L;}
 boolean due(){return !active()||signal!=consumed||stamp()!=lastStamp||System.currentTimeMillis()-n.lastPoll>1000;}
 boolean changed(){return active()&&signal!=consumed;}
 void consume(){consumed=signal;lastStamp=stamp();}
 void cancel(){transport.close();}
 public void run(){int generation=n.networkGeneration,room=n.roomId;String token=n.token,endpoint=n.endpoint;long cursor=Long.MIN_VALUE;
  while(n.alive&&active()&&generation==n.networkGeneration&&room==n.roomId&&token.equals(n.token)){
   try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(OnlineClient.MAGIC);out.writeInt(room);out.writeLong(cursor);out.close();
    byte[] reply=transport.request(endpoint,2,0,token,null,bytes.toByteArray());
    if(reply.length!=8)throw new IOException("Sai thông báo TCP");
    long next=new DataInputStream(new ByteArrayInputStream(reply)).readLong();
    if(generation!=n.networkGeneration||room!=n.roomId||!token.equals(n.token))break;
    if(next!=cursor){cursor=next;signal++;synchronized(n){n.notifyAll();}}failures=0;
   }catch(Exception e){transport.close();if(++failures>=3){failed=true;signal++;break;}try{Thread.sleep(200*failures);}catch(Exception ignored){}}
  }transport.close();
 }
}
