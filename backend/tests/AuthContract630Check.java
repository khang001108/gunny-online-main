import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/** Covers Supabase's two documented existing-email responses and card RPC response. */
public final class AuthContract630Check {
    public static void main(String[] args)throws Exception{
        AtomicInteger mode=new AtomicInteger();HttpServer fake=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        fake.createContext("/",x->{String path=x.getRequestURI().getPath(),body="{}";int status=200;x.getRequestBody().readAllBytes();if(path.equals("/auth/v1/signup")){if(mode.get()==0)body="{\"id\":\"12345678-1234-1234-1234-123456789abc\",\"identities\":[]}";else{status=422;body="{\"code\":\"user_already_exists\",\"msg\":\"User already registered\"}";}}else if(path.equals("/rest/v1/rpc/gunny_claim_card")){if(!"sb_secret_test".equals(x.getRequestHeaders().getFirst("apikey")))throw new AssertionError("Reward RPC must use server key");body="{\"card\":4,\"coins\":125}";}byte[] bytes=body.getBytes(StandardCharsets.UTF_8);x.sendResponseHeaders(status,bytes.length);x.getResponseBody().write(bytes);x.close();});fake.start();
        try{CloudStore store=new CloudStore("http://127.0.0.1:"+fake.getAddress().getPort(),"sb_publishable_test","sb_secret_test",true);for(int i=0;i<2;i++){mode.set(i);boolean existing=false;try{store.register("exists@test.test","test-password-123","Tên");}catch(IllegalArgumentException e){existing=e.getMessage().contains("Email đã tồn tại");}if(!existing)throw new AssertionError("Existing email response "+i);System.out.println("PASS: Existing email recognized from Supabase response "+i);}int[] reward=store.claim("12345678-1234-1234-1234-123456789abc","12345678-1234-1234-1234-123456789abc",4,125);if(reward[0]!=4||reward[1]!=125)throw new AssertionError("Reward response");System.out.println("PASS: Card RPC response is parsed with server-only credentials");}finally{fake.stop(0);}
    }
}
