import java.io.*;import java.net.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.net.http.*;
/** Real TCP version gate and validation; no Supabase credentials or live server needed. */
public class HF43GateCheck {
 static int checks;static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;System.out.println("PASS: "+label);}
 record Reply(int status,byte[] body){DataInputStream read(){return new DataInputStream(new ByteArrayInputStream(body));}}
 static final class Peer implements AutoCloseable {
  Socket socket;DataInputStream in;DataOutputStream out;
  Peer(int port)throws IOException{socket=new Socket("127.0.0.1",port);socket.setSoTimeout(3000);in=new DataInputStream(socket.getInputStream());out=new DataOutputStream(socket.getOutputStream());}
  void send(int magic,int kind,String token,int size,byte[] body)throws IOException{out.writeInt(magic);out.writeByte(kind);out.writeInt(TribalTcpCheck.FLAGS);out.writeUTF(token);out.write(new byte[]{0,1,2});out.writeInt(size);if(body!=null)out.write(body);out.flush();}
  Reply frame(int magic,int kind,String token,byte[] body)throws IOException{send(magic,kind,token,body==null?0:body.length,body);int status=in.readInt(),size=in.readInt();if(size<0||size>131072)throw new IOException("reply size");byte[] bytes=in.readNBytes(size);if(bytes.length!=size)throw new EOFException();return new Reply(status,bytes);}
  boolean closed()throws IOException{try{return in.read()==-1;}catch(SocketException expected){return true;}}
  public void close()throws IOException{socket.close();}
 }
 static void rejected(Reply reply)throws Exception{check(reply.status==200,"Legacy JAR receives readable game error");var data=reply.read();check(data.readUnsignedByte()==1&&data.readUTF().contains(GameRelease.VERSION),"Legacy JAR displays required update");}
 public static void main(String[] args)throws Exception{
  var store=new CloudStore.Memory();store.register("hf43@test.local","test-password-123","VersionTester");
  byte[] login=Tcp768Check.body("login",o->{o.writeUTF("hf43@test.local");o.writeUTF("test-password-123");});
  byte[] register=Tcp768Check.body("register",o->{o.writeUTF("old@test.local");o.writeUTF("test-password-123");o.writeUTF("OldVersion");});
  check(GameRelease.magic(0)==GameRelease.LEGACY_MAGIC&&GameRelease.magic(1)==GameRelease.WIRE_MAGIC&&GameRelease.magic(2)==GameRelease.WIRE_MAGIC,"Health stays backward readable; commands/events require new epoch");
  check(GameRelease.healthError("{\"ok\":true,\"protocol\":1,\"version\":\"7.9.18\"}")!=null,"Client preflight rejects old server");
  check(GameRelease.healthError("{\"ok\":true,\"protocol\":1,\"version\":\"7.9.19\",\"wireEpoch\":42,\"tcp\":1}")!=null,"Same display version cannot bypass wire epoch");
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   server.tcpOnly=true;server.startTcp("127.0.0.1",0,16);int port=server.tcpPort();String token;
   try(var old=new Peer(port)){
    var health=old.frame(GameRelease.LEGACY_MAGIC,0,"",null);String json=new String(health.body,StandardCharsets.UTF_8);
    check(health.status==200&&GameRelease.healthError(json)==null&&json.contains("\"minVersion\":\"7.9.21\""),"Public pre-login health identifies exact required release");
    rejected(old.frame(GameRelease.LEGACY_MAGIC,1,"",login));check(old.closed(),"Legacy socket released after update message");
    check(server.sessions.isEmpty(),"Legacy login creates no session");check(!server.limits.containsKey("auth:127.0.0.1"),"Old login blocked before account authentication");
   }
   try(var prior=new Peer(port)){rejected(prior.frame(GameRelease.HF43_MAGIC,1,"",login));check(prior.closed(),"HF43 socket rejected with update notice");}
   try(var prior=new Peer(port)){rejected(prior.frame(GameRelease.HF44_MAGIC,1,"",login));check(prior.closed(),"HF44 socket rejected with update notice");}
   try(var old=new Peer(port)){rejected(old.frame(GameRelease.LEGACY_MAGIC,1,"",register));check(!store.accounts.containsKey("old@test.local"),"Legacy registration cannot write account data");}
   try(var current=new Peer(port)){
    var reply=current.frame(GameRelease.WIRE_MAGIC,1,"",login);var data=reply.read();check(reply.status==200&&data.readUnsignedByte()==0,"Current build can log in");token=data.readUTF();check(token.length()==43&&server.sessions.containsKey(token),"Server-generated session is valid");
    reply=current.frame(GameRelease.WIRE_MAGIC,1,token,Tcp768Check.body("rooms",null));check(reply.status==200&&reply.read().readUnsignedByte()==0,"Current client reuses socket for authenticated gameplay");
    try(var old=new Peer(port)){rejected(old.frame(GameRelease.LEGACY_MAGIC,1,token,Tcp768Check.body("rooms",null)));check(server.sessions.containsKey(token),"Legacy command rejected without invalidating current player");}
    var eventBody=new ByteArrayOutputStream();var event=new DataOutputStream(eventBody);event.writeInt(GunnyServer.MAGIC);event.writeInt(0);event.writeLong(Long.MIN_VALUE);
    try(var old=new Peer(port)){check(old.frame(GameRelease.LEGACY_MAGIC,2,token,eventBody.toByteArray()).status==426,"Legacy event socket rejected");}
    try(var fresh=new Peer(port)){var evt=fresh.frame(GameRelease.WIRE_MAGIC,2,token,eventBody.toByteArray());check(evt.status==200&&evt.body.length==8,"Current event channel remains compatible");}
    try(var bad=new Peer(port)){bad.send(GameRelease.WIRE_MAGIC,1,"a",0,null);check(bad.closed(),"Malformed token length rejected before reading body");}
    try(var bad=new Peer(port)){bad.send(GameRelease.WIRE_MAGIC,1,"",2049,null);check(bad.closed(),"Oversized unauthenticated frame rejected without waiting for payload");}
    try(var bad=new Peer(port)){bad.send(GameRelease.WIRE_MAGIC,2,token,17,null);check(bad.closed(),"Malformed event frame rejected without waiting for payload");}
    try(var bad=new Peer(port)){var denial=bad.frame(GameRelease.WIRE_MAGIC,1,"A".repeat(43),Tcp768Check.body("rooms",null));check(denial.read().readUnsignedByte()==2,"Unknown token cannot access rooms");}
    check(current.frame(GameRelease.WIRE_MAGIC,1,token,Tcp768Check.body("rooms",null)).read().readUnsignedByte()==0,"Invalid clients do not interrupt authenticated socket");
   }
   var http=HttpClient.newHttpClient();var direct=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+server.port()+"/api")).POST(HttpRequest.BodyPublishers.ofByteArray(login)).build(),HttpResponse.BodyHandlers.discarding());check(direct.statusCode()==410,"Production HTTP route cannot bypass TCP gate");
  }
  System.out.println("HF43 PASS: "+checks+" checks");
 }
}
