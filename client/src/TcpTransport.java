import java.io.*;
import javax.microedition.io.*;

/** Persistent framed TCP transport for Raspberry Pi deployments. */
final class TcpTransport {
 static final int MAGIC=0x474e5954,MAX_REPLY=131072;
 StreamConnection connection;DataInputStream in;DataOutputStream out;String address="";
 synchronized byte[] request(String endpoint,int kind,int flags,String token,int[] kit,byte[] body)throws IOException{
  try{
   ensure(endpoint);out.writeInt(MAGIC);out.writeByte(kind);out.writeInt(flags);out.writeUTF(token==null?"":token);
   for(int i=0;i<3;i++)out.writeByte(kit==null?0:kit[i]);out.writeInt(body==null?0:body.length);if(body!=null)out.write(body);out.flush();
   int status=in.readInt(),length=in.readInt();if(length<0||length>MAX_REPLY)throw new IOException("Phản hồi TCP quá lớn.");byte[] reply=new byte[length];in.readFully(reply);if(status!=200)throw new IOException("Máy chủ TCP chưa sẵn sàng ("+status+").");return reply;
  }catch(IOException e){close();throw e;}
 }
 void ensure(String endpoint)throws IOException{
  if(connection!=null&&address.equals(endpoint))return;close();connection=(StreamConnection)Connector.open(endpoint,Connector.READ_WRITE,true);address=endpoint;in=connection.openDataInputStream();out=connection.openDataOutputStream();
 }
 void close(){StreamConnection c=connection;DataInputStream input=in;DataOutputStream output=out;connection=null;in=null;out=null;address="";if(c!=null)try{c.close();}catch(Exception ignored){}if(input!=null)try{input.close();}catch(Exception ignored){}if(output!=null)try{output.close();}catch(Exception ignored){}}
}
