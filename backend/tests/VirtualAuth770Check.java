import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class VirtualAuth770Check {
 public static void main(String[] args)throws Exception {
  var created=new AtomicInteger();String id="12345678-1234-1234-1234-123456789abc";
  var fake=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  fake.createContext("/",x->{try{
   if(!x.getRequestURI().getPath().equals("/auth/v1/admin/users"))throw new AssertionError("Signup must never use email-delivery endpoints");
   if(!"sb_secret_test".equals(x.getRequestHeaders().getFirst("apikey")))throw new AssertionError("Only server credentials create virtual accounts");
   Map<String,Object> body=Json.obj(Json.parse(new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8)));
   if(!Boolean.TRUE.equals(body.get("email_confirm"))||!"virtual".equals(Json.obj(body.get("app_metadata")).get("account_type")))throw new AssertionError("Virtual account must be immediately usable");
   if(!"fake@no-mail.invalid".equals(body.get("email"))||!"Gunner".equals(Json.obj(body.get("user_metadata")).get("display_name")))throw new AssertionError("Keep the supplied login and profile name");
   created.incrementAndGet();byte[] reply=Json.write(Map.of("id",id)).getBytes(StandardCharsets.UTF_8);x.sendResponseHeaders(200,reply.length);x.getResponseBody().write(reply);
  }catch(Throwable e){e.printStackTrace();x.sendResponseHeaders(500,-1);}finally{x.close();}});fake.start();
  try{var store=new CloudStore("http://127.0.0.1:"+fake.getAddress().getPort(),"sb_publishable_test","sb_secret_test",true);store.register("fake@no-mail.invalid","test-password-123","Gunner");if(created.get()!=1)throw new AssertionError();System.out.println("PASS: Fake-email registration uses server-only Admin Auth, auto-confirms and sends no mail");}
  finally{fake.stop(0);}
 }
}
