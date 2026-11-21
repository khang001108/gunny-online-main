import java.net.*;import java.net.http.*;import java.nio.file.*;import java.util.*;
/** Exercise production admin settings, including the LAN authority used on Pi. */
public final class AdminLanCheck {
 static int checks;static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);checks++;}
 public static void main(String[] args)throws Exception{
  System.setProperty("jdk.httpclient.allowRestrictedHeaders","host");
  var local=InetAddress.getByName("192.168.1.20");
  for(String host:new String[]{"192.168.1.127","10.0.0.1","172.16.0.1","172.31.255.254","127.0.0.1","localhost","[::1]"})check(AdminServer.privateHttpHost(host,local),"Private literal allowed");
  for(String host:new String[]{"192.168.1.127.evil.test","8.8.8.8","172.32.0.1","172.15.0.1","192.168.1.999","example.com","0127.0.0.1"})check(!AdminServer.privateHttpHost(host,local),"Public/malformed host denied");
  check(!AdminServer.privateHttpHost("192.168.1.127",InetAddress.getByName("8.8.8.8")),"Public peer denied");
  var store=new CloudStore.Memory();
  try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store,new Admin750Check.Records(store),Admin750Check.PASSWORD,false)){
   server.clock.shutdownNow();server.admin.tasks.shutdownNow();var http=HttpClient.newHttpClient();String base="http://127.0.0.1:"+server.port();String authority="192.168.1.127:18080";
   var address=URI.create(base+"/admin/api/login");String body=Json.write(Map.of("password",Admin750Check.PASSWORD));
   var login=HttpRequest.newBuilder(address).header("Host",authority).header("Origin","http://"+authority).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
   var reply=http.send(login,HttpResponse.BodyHandlers.ofString());check(reply.statusCode()==200,"Production LAN login succeeds");String set=reply.headers().firstValue("Set-Cookie").orElseThrow();check(!set.contains("; Secure")&&set.contains("HttpOnly")&&set.contains("SameSite=Strict"),"LAN cookie usable and restricted");String cookie=set.split(";")[0];
   check(http.send(HttpRequest.newBuilder(URI.create(base+"/admin/api/session")).header("Host",authority).header("Cookie",cookie).build(),HttpResponse.BodyHandlers.ofString()).statusCode()==200,"LAN cookie restores session");
   for(String origin:new String[]{"http://evil.test","http://192.168.1.127:18081","null","http://192.168.1.127:18080/path"}){
    var bad=HttpRequest.newBuilder(address).header("Host",authority).header("Origin",origin).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();check(http.send(bad,HttpResponse.BodyHandlers.ofString()).statusCode()==403,"Cross origin rejected");
   }
   var https=HttpRequest.newBuilder(address).header("Host","gunny.example").header("Origin","https://gunny.example").header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();var secure=http.send(https,HttpResponse.BodyHandlers.ofString());check(secure.statusCode()==200&&secure.headers().firstValue("Set-Cookie").orElseThrow().contains("; Secure"),"HTTPS proxy keeps Secure cookie");
   var publicHttp=HttpRequest.newBuilder(address).header("Host","gunny.example").header("Origin","http://gunny.example").header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();check(http.send(publicHttp,HttpResponse.BodyHandlers.ofString()).statusCode()==403,"Public HTTP denied");
   var logout=HttpRequest.newBuilder(URI.create(base+"/admin/api/logout")).header("Host",authority).header("Origin","http://"+authority).header("Cookie",cookie).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build();var out=http.send(logout,HttpResponse.BodyHandlers.ofString());check(out.statusCode()==200&&!out.headers().firstValue("Set-Cookie").orElseThrow().contains("; Secure"),"LAN logout clears matching cookie");
   check(http.send(HttpRequest.newBuilder(URI.create(base+"/admin/api/session")).header("Cookie",cookie).build(),HttpResponse.BodyHandlers.ofString()).statusCode()==401,"Logout revokes session");
  }
  System.out.println("PASS: "+checks+" production LAN admin/origin/cookie checks");
 }
}
