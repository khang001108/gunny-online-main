import java.io.*;
import java.net.*;
import java.nio.file.Path;

/** End-to-end persistent TCP frame check against the real HTTP game handler. */
public final class Tcp768Check {
 static final int FLAGS=0x1ffff;
 static byte[] body(String op,Writer writer)throws Exception {
  var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(GunnyServer.MAGIC);out.writeUTF(op);if(writer!=null)writer.write(out);out.close();return bytes.toByteArray();
 }
 static byte[] frame(DataInputStream in,DataOutputStream out,int kind,String token,byte[] body)throws Exception {
  out.writeInt(TcpGameServer.MAGIC);out.writeByte(kind);out.writeInt(FLAGS);out.writeUTF(token==null?"":token);out.write(new byte[]{0,1,2});out.writeInt(body==null?0:body.length);if(body!=null)out.write(body);out.flush();
  int status=in.readInt(),length=in.readInt();if(status!=200)throw new AssertionError("TCP status "+status);byte[] response=in.readNBytes(length);if(response.length!=length)throw new EOFException();return response;
 }
 static DataInputStream ok(byte[] response)throws Exception {var in=new DataInputStream(new ByteArrayInputStream(response));int code=in.readUnsignedByte();if(code!=0)throw new AssertionError(code+":"+in.readUTF());return in;}
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);System.out.println("PASS: "+label);}
 public static void main(String[] args)throws Exception {
  var store=new CloudStore.Memory();store.register("tcp@768.test","test-password-123","TCP Gunner");
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.startTcp("127.0.0.1",0,8);
   try(var socket=new Socket("127.0.0.1",server.tcpPort());var in=new DataInputStream(socket.getInputStream());var out=new DataOutputStream(socket.getOutputStream())){
    String health=new String(frame(in,out,0,"",null),java.nio.charset.StandardCharsets.UTF_8);check(health.contains("\"tcp\":1")&&health.contains("\"longpoll\":0"),"TCP health advertises persistent transport without HTTP long poll");
    DataInputStream login=ok(frame(in,out,1,"",body("login",o->{o.writeUTF("tcp@768.test");o.writeUTF("test-password-123");})));String token=login.readUTF();check(token.length()>20,"Login returns a session token over TCP");
    DataInputStream rooms=ok(frame(in,out,1,token,body("rooms",null)));check(rooms.readUnsignedShort()==0,"A second request reuses the same socket and session");
    DataInputStream presence=ok(frame(in,out,1,token,body("presence",null)));check(presence.readInt()==1,"Feature headers and source address survive the TCP bridge");
   }
   for(int i=0;i<20&&server.tcp.active.get()!=0;i++)Thread.sleep(10);
   check(server.tcp.active.get()==0,"Closed client is removed from TCP connection count");
  }
  System.out.println("All Raspberry Pi TCP 7.6.8 checks passed");
 }
 interface Writer {void write(DataOutputStream out)throws Exception;}
}
