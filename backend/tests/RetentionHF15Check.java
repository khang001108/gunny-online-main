import com.sun.net.httpserver.HttpServer;
import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;
public final class RetentionHF15Check {
 public static void main(String[] args)throws Exception {
  HttpServer mock=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  List<String> calls=new ArrayList<>();boolean[] fail={false};
  mock.createContext("/",x->{String path=x.getRequestURI().getPath();String body=new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);calls.add(path+" "+body);String reply;int status=200;
   if(fail[0]){status=400;reply="{\"code\":\"22023\"}";}
   else if(path.endsWith("gunny_retention_save_hf15")){var p=Json.obj(Json.parse(body));reply=Json.write(Map.of("offline_days",Json.num(p,"p_days",0),"enabled",p.get("p_enabled")));}
   else reply="{\"offline_days\":90,\"enabled\":true}";
   byte[] bytes=reply.getBytes(StandardCharsets.UTF_8);x.sendResponseHeaders(status,bytes.length);x.getResponseBody().write(bytes);x.close();});mock.start();
  try{AdminData a=new AdminData(new CloudStore("http://127.0.0.1:"+mock.getAddress().getPort(),"public","sb_secret_test",true));
   for(boolean enabled:new boolean[]{true,false}){var result=Json.obj(a.call("retention-save",Map.of("days",90,"enabled",enabled)));if(Json.num(result,"offline_days",0)!=90||!Boolean.valueOf(enabled).equals(result.get("enabled")))throw new AssertionError("policy response");}
   if(!calls.get(0).startsWith("/rest/v1/rpc/gunny_retention_save_hf15 ")||!calls.get(0).contains("\"p_days\":90")||!calls.get(1).contains("\"p_enabled\":false"))throw new AssertionError(calls);
   a.call("retention-policy",Map.of());if(!calls.get(2).startsWith("/rest/v1/rpc/gunny_admin "))throw new AssertionError("read route changed");
   fail[0]=true;try{a.call("retention-save",Map.of("days",90,"enabled",true));throw new AssertionError("failure hidden");}catch(IllegalArgumentException e){if(!e.getMessage().contains("22023")||e.getMessage().contains("vật phẩm"))throw e;}
   System.out.println("PASS typed retention RPC, toggle true/false, read route, admin-specific error");
  }finally{mock.stop(0);}
 }
}
