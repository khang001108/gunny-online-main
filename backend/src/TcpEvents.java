import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Server change notifications over a separate persistent TCP socket. */
final class TcpEvents {
 final GunnyServer server;final Object changed=new Object();final Set<String> waiting=ConcurrentHashMap.newKeySet();
 TcpEvents(GunnyServer s){server=s;}
 void signal(){synchronized(changed){changed.notifyAll();}}
 byte[] waitFor(String token,byte[] body)throws Exception {
  if(body.length!=16)throw new IOException("Bad event request");
  var in=new DataInputStream(new ByteArrayInputStream(body));if(in.readInt()!=GunnyServer.MAGIC)throw new IOException("Bad protocol");
  int room=in.readInt();long cursor=in.readLong();var session=server.sessions.get(token);
  if(session==null||session.expiry<System.currentTimeMillis())throw new SecurityException();
  if(!waiting.add(token))throw new IOException("Already subscribed");
  try{long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(12),mark=0;
   synchronized(changed){while(true){
    if(server.closed||server.sessions.get(token)!=session||session.expiry<System.currentTimeMillis())throw new SecurityException();
    var r=server.rooms.get(room);if(session.room!=room||r==null)break;
    mark=server.longPoll.state(r);long remaining=deadline-System.nanoTime();
    if(mark!=cursor||remaining<=0)break;
    TimeUnit.NANOSECONDS.timedWait(changed,remaining);
   }}
   var bytes=new ByteArrayOutputStream();new DataOutputStream(bytes).writeLong(mark);return bytes.toByteArray();
  }finally{waiting.remove(token);}
 }
}
