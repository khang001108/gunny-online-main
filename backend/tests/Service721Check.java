import java.net.*;import java.nio.charset.*;import com.sun.net.httpserver.*;
public final class Service721Check {
 public static void main(String[] args)throws Exception{
  String[] code={"PGRST202"};HttpServer fake=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  fake.createContext("/",x->{x.getRequestBody().readAllBytes();byte[] b=("{\"code\":\""+code[0]+"\",\"message\":\"database error\"}").getBytes(StandardCharsets.UTF_8);x.sendResponseHeaders(404,b.length);x.getResponseBody().write(b);x.close();});fake.start();
  try{CloudStore c=new CloudStore("http://127.0.0.1:"+fake.getAddress().getPort(),"sb_publishable_test","sb_secret_test",true);
   for(String expected:new String[]{"PGRST202","P0002","42501","42883","PGRST205"}){code[0]=expected;try{c.equip("12345678-1234-1234-1234-123456789abc",0,0);throw new AssertionError("Expected database error");}catch(IllegalStateException e){String message=e.getMessage();if(!message.contains("gunny_equip")||!message.contains(expected))throw new AssertionError(message);if(!expected.equals("PGRST202")&&(message.contains("migration")||message.contains("thiếu API")))throw new AssertionError("Wrong diagnosis: "+message);if(message.contains("sb_secret")||message.contains("12345678"))throw new AssertionError("Sensitive data in error");System.out.println("PASS: RPC diagnosis distinguishes "+expected+" without exposing credentials");}}
  }finally{fake.stop(0);}System.out.println("All service 7.2.1 checks passed");
 }
}
