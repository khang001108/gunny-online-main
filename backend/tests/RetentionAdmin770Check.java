import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.util.*;

public final class RetentionAdmin770Check {
 static final class Records extends AdminData {
  int changes;Records(CloudStore store){super(store);}
  Object call(String op,Map<String,Object> data){
   if(op.equals("retention-policy"))return Map.of("enabled",false,"offline_days",90);
   if(Set.of("retention-save","offline-days","extend-offline").contains(op)){changes++;return Map.of("ok",true);}
   return Map.of();
  }
 }
 public static void main(String[] args)throws Exception {
  var store=new CloudStore.Memory();var records=new Records(store);String password="retention-test-password-770";
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store,records,password,true)){
   String base="http://127.0.0.1:"+server.port();var http=HttpClient.newHttpClient();
   var login=http.send(HttpRequest.newBuilder(URI.create(base+"/admin/api/login")).header("Origin",base).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(Json.write(Map.of("password",password)))).build(),HttpResponse.BodyHandlers.ofString());
   String cookie=login.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
   var policy=http.send(HttpRequest.newBuilder(URI.create(base+"/admin/api/retention-policy")).header("Cookie",cookie).GET().build(),HttpResponse.BodyHandlers.ofString());
   Tcp768Check.check(policy.statusCode()==200&&policy.body().contains("90"),"Admin can load the retention policy");
   String id=UUID.randomUUID().toString();
   String[] ops={"retention-save","retention-save","offline-days","extend-offline","extend-offline"};
   Map<?,?>[] data={Map.of("days",90,"enabled",true),Map.of("days",0,"enabled",true),Map.of("id",id,"days",2.5),Map.of("id",id,"days",30),Map.of("id",id,"days",30,"request_key",UUID.randomUUID().toString())};
   int[] expected={200,400,400,400,200};
   for(int i=0;i<ops.length;i++){
    var response=http.send(HttpRequest.newBuilder(URI.create(base+"/admin/api/"+ops[i])).header("Origin",base).header("Cookie",cookie).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(Json.write(data[i]))).build(),HttpResponse.BodyHandlers.ofString());
    Tcp768Check.check(response.statusCode()==expected[i],"Retention admin validates request "+i);
   }
   Tcp768Check.check(records.changes==2,"Invalid limits and missing idempotency keys never reach the database");
   var foreign=http.send(HttpRequest.newBuilder(URI.create(base+"/admin/api/retention-save")).header("Origin","https://foreign.invalid").header("Cookie",cookie).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"days\":90,\"enabled\":true}")).build(),HttpResponse.BodyHandlers.ofString());
   Tcp768Check.check(foreign.statusCode()==403,"Foreign websites cannot change the cleanup policy");
  }
 }
}
