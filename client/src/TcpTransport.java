import java.io.*;
import javax.microedition.io.*;

/** Persistent framed TCP transport for Raspberry Pi deployments. */
final class TcpTransport {
 static final int MAGIC=0x474e5954,MAX_REPLY=131072;
 StreamConnection connection;DataInputStream in;DataOutputStream out;String address="";
 volatile long txBytes,rxBytes;volatile int rtt=-1,jitter;private byte[] eventReply;
 final ByteArrayOutputStream requestBytes=new ByteArrayOutputStream(256);
 final DataOutputStream requestPacket=new DataOutputStream(requestBytes);
 synchronized byte[] request(String endpoint,int kind,int flags,String token,int[] kit,byte[] body)throws IOException{
  try{
   ensure(endpoint);
   // CLDC socket streams need not buffer primitive writes. Send one framed request.
   boolean large=body!=null&&body.length>4096;
   ByteArrayOutputStream bytes=large?new ByteArrayOutputStream(64+body.length):requestBytes;
   bytes.reset();
   DataOutputStream packet=large?new DataOutputStream(bytes):requestPacket;
   packet.writeInt(MAGIC);packet.writeByte(kind);packet.writeInt(flags);packet.writeUTF(token==null?"":token);
   for(int i=0;i<3;i++)packet.writeByte(kit==null?0:kit[i]);packet.writeInt(body==null?0:body.length);if(body!=null)packet.write(body);
   byte[] wire=bytes.toByteArray();long started=System.currentTimeMillis();out.write(wire);out.flush();txBytes+=wire.length;
   int status=in.readInt(),length=in.readInt();if(length<0||length>MAX_REPLY)throw new IOException("Phản hồi TCP quá lớn.");byte[] reply=kind==2&&length==8?(eventReply==null?(eventReply=new byte[8]):eventReply):new byte[length];in.readFully(reply);rxBytes+=8+length;int sample=(int)Math.min(60000,Math.max(0,System.currentTimeMillis()-started));if(rtt>=0)jitter=(jitter*3+Math.abs(sample-rtt))/4;rtt=sample;if(status!=200)throw new IOException("Máy chủ TCP chưa sẵn sàng ("+status+").");return reply;
  }catch(IOException e){close();throw e;}
 }
 void ensure(String endpoint)throws IOException{
  if(connection!=null&&address.equals(endpoint))return;close();connection=(StreamConnection)Connector.open(endpoint,Connector.READ_WRITE,true);address=endpoint;if(connection instanceof SocketConnection)try{((SocketConnection)connection).setSocketOption(SocketConnection.DELAY,0);}catch(Exception ignored){}in=connection.openDataInputStream();out=connection.openDataOutputStream();
 }
 void close(){StreamConnection c=connection;DataInputStream input=in;DataOutputStream output=out;connection=null;in=null;out=null;address="";if(c!=null)try{c.close();}catch(Exception ignored){}if(input!=null)try{input.close();}catch(Exception ignored){}if(output!=null)try{output.close();}catch(Exception ignored){}}
}
