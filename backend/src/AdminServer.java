import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
/** Same-origin password-protected management app. No service key reaches the browser. */
final class AdminServer implements AutoCloseable {
 final GunnyServer s;final AdminData data;final SupportPage support;final byte[] passwordHash;final boolean enabled,test;
 final Map<String,Long> tokens=new ConcurrentHashMap<>();final SecureRandom random=new SecureRandom();
 final ScheduledExecutorService tasks=Executors.newSingleThreadScheduledExecutor();
 AdminServer(GunnyServer server){this(server,new AdminData(server.store),CloudStore.env("GUNNY_ADMIN_PASSWORD"),false);}
 AdminServer(GunnyServer server,AdminData records,String password,boolean testing){s=server;data=records;support=new SupportPage(records);test=testing;enabled=!password.isEmpty();if(enabled&&password.length()<16)throw new IllegalArgumentException("GUNNY_ADMIN_PASSWORD requires at least 16 characters");passwordHash=digest(password);}
 static byte[] digest(String text){try{return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
 void start(){if(enabled)tasks.scheduleWithFixedDelay(this::maintenance,5,30,TimeUnit.SECONDS);}
 void maintenance(){try{
  List<Map<String,Object>> active=new ArrayList<>();long now=System.currentTimeMillis();Map<String,Long> seen=new HashMap<>();
  for(var u:s.sessions.values())if(u.expiry>now)seen.merge(u.id,u.lastSeen,Math::max);
  Set<String> ids=seen.keySet();
  for(var entry:seen.entrySet())active.add(Map.of("id",entry.getKey(),"at",Instant.ofEpochMilli(entry.getValue()).toString()));
  if(!active.isEmpty()){Set<String> existing=new HashSet<>();for(Object id:(List<?>)data.call("presence",Map.of("users",active)))existing.add(String.valueOf(id));for(String id:ids)if(!existing.contains(id))revoke(id);}
  data.call("maintenance",Map.of());
  tokens.entrySet().removeIf(e->e.getValue()<now);
 }catch(Exception e){System.err.println("Admin maintenance failed: "+e.getClass().getSimpleName()+"; check admin SQL migration and service key");}}
 void revoke(String id){for(var u:s.sessions.values())if(u.id.equals(id)){s.leave(u);s.sessions.remove(u.token);}}
 List<?> login(String id)throws Exception{if(!enabled)return List.of();Map<String,Object> r=data.login(id);if(Boolean.TRUE.equals(r.get("blocked")))throw new SecurityException("Tài khoản đã bị xoá theo lịch.");Object a=r.get("notices");return a instanceof List?(List<?>)a:List.of();}
 void writeNotices(DataOutputStream out,List<?> notices)throws IOException{out.writeByte(Math.min(21,notices.size()));for(int i=0;i<notices.size()&&i<21;i++){Map<String,Object> n=Json.obj(notices.get(i));out.writeUTF(Json.str(n,"title"));out.writeUTF(Json.str(n,"body"));out.writeUTF(Json.str(n,"appeal"));}}
 void json(HttpExchange x,int status,Object body)throws IOException{byte[] b=Json.write(body).getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");x.sendResponseHeaders(status,b.length);x.getResponseBody().write(b);}
 Map<String,Object> body(HttpExchange x)throws IOException{int limit=x.getRequestURI().getPath().equals("/admin/api/support-qr")?2100000:16384;byte[] b=x.getRequestBody().readNBytes(limit+1);if(b.length>limit)throw new IllegalArgumentException("Dữ liệu quá dài");return Json.obj(Json.parse(new String(b,StandardCharsets.UTF_8)));}
 String cookie(HttpExchange x){String value=x.getRequestHeaders().getFirst("Cookie");if(value!=null)for(String part:value.split(";")){String[] p=part.trim().split("=",2);if(p.length==2&&p[0].equals("gunny_admin"))return p[1];}return "";}
 static boolean privateHttpHost(String host,InetAddress peer){
  if(host==null||peer==null||!peer.isSiteLocalAddress()&&!peer.isLoopbackAddress())return false;
  if(host.equalsIgnoreCase("localhost")||host.equals("[::1]")||host.equals("::1"))return true;
  String[] parts=host.split("\\.",-1);if(parts.length!=4)return false;int[] octets=new int[4];
  for(int i=0;i<4;i++){if(!parts[i].matches("0|[1-9][0-9]{0,2}"))return false;octets[i]=Integer.parseInt(parts[i]);if(octets[i]>255)return false;}
  return octets[0]==10||octets[0]==127||octets[0]==192&&octets[1]==168||octets[0]==172&&octets[1]>=16&&octets[1]<=31;
 }
 boolean localHttp(HttpExchange x){try{URI u=URI.create(x.getRequestHeaders().getFirst("Origin"));return "http".equals(u.getScheme())&&privateHttpHost(u.getHost(),x.getRemoteAddress().getAddress());}catch(Exception e){return false;}}
 boolean sameOrigin(HttpExchange x){String origin=x.getRequestHeaders().getFirst("Origin"),host=x.getRequestHeaders().getFirst("Host");if(origin==null||host==null)return false;try{URI u=URI.create(origin);return u.getUserInfo()==null&&u.getRawQuery()==null&&u.getRawFragment()==null&&(u.getRawPath()==null||u.getRawPath().isEmpty())&&("https".equals(u.getScheme())||localHttp(x))&&u.getRawAuthority().equalsIgnoreCase(host);}catch(Exception e){return false;}}
 String secureCookie(HttpExchange x){return localHttp(x)&&sameOrigin(x)?"":"; Secure";}
 void handle(HttpExchange x)throws IOException{try{
  Headers h=x.getResponseHeaders();h.set("X-Gunny-Version","7.8.8");h.set("Cache-Control","no-store");h.set("X-Content-Type-Options","nosniff");h.set("Referrer-Policy","no-referrer");h.set("Content-Security-Policy","default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; img-src 'self'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
  if(!enabled){json(x,503,Map.of("error","Trang quản trị chưa được bật. Cấu hình GUNNY_ADMIN_PASSWORD trên máy chủ."));return;}
  String path=x.getRequestURI().getPath(),method=x.getRequestMethod();
  if(!path.startsWith("/admin/api/")){
   String file=path.equals("/admin")||path.equals("/admin/")?"index.html":path.equals("/admin/app.js")?"app.js":path.equals("/admin/support.js")?"support.js":path.equals("/admin/style.css")?"style.css":"";
   if(file.isEmpty()||!method.equals("GET")){json(x,404,Map.of("error","Không tìm thấy"));return;}
   byte[] b=Files.readAllBytes(Path.of("public/admin",file));h.set("Content-Type",file.endsWith("js")?"text/javascript; charset=utf-8":file.endsWith("css")?"text/css; charset=utf-8":"text/html; charset=utf-8");x.sendResponseHeaders(200,b.length);x.getResponseBody().write(b);return;
  }
  boolean mutation=!method.equals("GET");if(mutation&&(!method.equals("POST")||!sameOrigin(x)||!"application/json".equalsIgnoreCase(x.getRequestHeaders().getFirst("Content-Type")))){json(x,403,Map.of("error","Yêu cầu không hợp lệ"));return;}
  String ip=x.getRemoteAddress().getAddress().getHostAddress();if(!s.limit("admin:"+ip,120)){json(x,429,Map.of("error","Thao tác quá nhanh"));return;}
  if(path.equals("/admin/api/login")){
   if(!method.equals("POST")){json(x,405,Map.of());return;}if(!s.limit("admin-login:"+ip,8)){json(x,429,Map.of("error","Thử lại sau một phút"));return;}
   String pw=Json.str(body(x),"password");if(pw.length()>256||!MessageDigest.isEqual(passwordHash,digest(pw))){json(x,401,Map.of("error","Mật khẩu không đúng"));return;}
   byte[] b=new byte[32];random.nextBytes(b);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(b);long now=System.currentTimeMillis();tokens.entrySet().removeIf(e->e.getValue()<now);if(tokens.size()>=64){json(x,429,Map.of("error","Quá nhiều phiên quản trị"));return;}tokens.put(token,now+8*3600000L);h.add("Set-Cookie","gunny_admin="+token+"; HttpOnly; SameSite=Strict; Path=/admin; Max-Age=28800"+secureCookie(x));json(x,200,Map.of("ok",true));return;
  }
  String token=cookie(x);Long expires=tokens.get(token);if(expires==null||expires<System.currentTimeMillis()){json(x,401,Map.of("error","Hãy đăng nhập quản trị"));return;}
  if(path.equals("/admin/api/session")&&method.equals("GET")){json(x,200,Map.of("ok",true));return;}
  if(path.equals("/admin/api/logout")&&method.equals("POST")){tokens.remove(token);h.add("Set-Cookie","gunny_admin=; HttpOnly; SameSite=Strict; Path=/admin; Max-Age=0"+secureCookie(x));json(x,200,Map.of("ok",true));return;}
  String op=path.substring("/admin/api/".length());Map<String,Object> input;
  if(method.equals("GET")&&(op.equals("accounts")||op.equals("announcements")||op.equals("retention-policy")||op.equals("support-list"))){input=new HashMap<>();String query=x.getRequestURI().getRawQuery();if(query!=null)for(String p:query.split("&")){String[] parts=p.split("=",2);if(parts.length==2)input.put(URLDecoder.decode(parts[0],StandardCharsets.UTF_8),URLDecoder.decode(parts[1],StandardCharsets.UTF_8));}}
  else if(method.equals("POST")&&Set.of("test-cheats","test-cheats-status","schedule","cancel-delete","delete-now","announcement-save","announcement-delete","wallet-add","retention-save","offline-days","extend-offline","support-qr","support-add","support-comment").contains(op))input=body(x);
  else{json(x,404,Map.of("error","Không tìm thấy"));return;}
  if(op.equals("delete-now")&&!Json.str(input,"confirm").equals("XOA"))throw new IllegalArgumentException("Nhập XOA để xác nhận xoá vĩnh viễn");
  if(op.equals("announcement-save")){String title=Json.str(input,"title").trim(),message=Json.str(input,"body").trim();if(title.isEmpty()||title.length()>80||message.isEmpty()||message.length()>1000)throw new IllegalArgumentException("Tiêu đề tối đa 80, nội dung tối đa 1.000 ký tự");Instant start=Instant.parse(Json.str(input,"starts_at")),end=Instant.parse(Json.str(input,"ends_at"));if(!end.isAfter(start))throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");}
  if(op.equals("wallet-add")){int coins=Json.num(input,"coins",-1),money=Json.num(input,"money",-1);if(!(input.get("coins") instanceof Number)||!(input.get("money") instanceof Number)||((Number)input.get("coins")).doubleValue()!=coins||((Number)input.get("money")).doubleValue()!=money||coins<0||coins>999999||money<0||money>999999||coins+money==0||Json.str(input,"reason").trim().isEmpty()||Json.str(input,"reason").length()>300)throw new IllegalArgumentException("Nhập số nguyên không âm, số cộng lớn hơn 0 và lý do");UUID.fromString(Json.str(input,"request_key"));UUID.fromString(Json.str(input,"id"));}
  if(Set.of("retention-save","offline-days","extend-offline").contains(op)){
   Object days=input.get("days");boolean inherit=op.equals("offline-days")&&days==null;
   if(!inherit&&(!(days instanceof Number)||((Number)days).doubleValue()!=((Number)days).intValue()||((Number)days).intValue()<1||((Number)days).intValue()>3650))throw new IllegalArgumentException("Số ngày phải là số nguyên từ 1 đến 3650");
   if(!inherit)input.put("days",((Number)days).intValue());
   if(!op.equals("retention-save"))UUID.fromString(Json.str(input,"id"));if(op.equals("extend-offline"))UUID.fromString(Json.str(input,"request_key"));
  }
  if(op.equals("test-cheats")||op.equals("test-cheats-status")){String id=Json.str(input,"id");UUID.fromString(id);if(op.equals("test-cheats")){if(!(input.get("enabled") instanceof Boolean))throw new IllegalArgumentException("Thiếu trạng thái cheat");s.store.testCheat(id,6,Boolean.TRUE.equals(input.get("enabled")));refreshProfile(id);}var profile=s.store.profile(id);json(x,200,Map.of("enabled",profile.testCheats));return;}
  Object result=op.equals("accounts")?accounts(input):op.startsWith("support-")?support.operation(op,input):data.call(op,input);
  if(op.equals("support-add")){
   // The durable transaction has committed. Refresh failures must not make it look rejected.
   try{refreshProfile(Json.str(input,"id"));}catch(Exception refreshError){System.err.println("Support gift committed; profile refresh deferred");}
  }else if(op.equals("wallet-add"))refreshProfile(Json.str(input,"id"));
  if(op.equals("delete-now"))revoke(Json.str(input,"id"));
  json(x,200,result);
 }catch(IllegalArgumentException e){json(x,400,Map.of("error",e.getMessage()==null?"Dữ liệu không hợp lệ":e.getMessage()));}
 catch(Exception e){System.err.println("Admin request failed: "+e.getClass().getSimpleName());json(x,503,Map.of("error",x.getRequestURI().getPath().contains("/support-")?"Chưa xử lý được. Kiểm tra migration 202611160001_support.sql và kết nối Supabase.":"Chưa xử lý được. Kiểm tra dữ liệu nhập, migration quản trị 7.5.0 và kết nối Supabase."));}finally{x.close();}}
 void refreshProfile(String id)throws Exception{CloudStore.Profile fresh=s.store.profile(id);for(var user:s.sessions.values())if(user.id.equals(id))synchronized(user){user.profile=fresh;}}
 Map<String,Object> accounts(Map<String,Object> input)throws Exception{
  int offset=Integer.parseInt(input.getOrDefault("offset","0").toString());
  if(offset<0||offset>1000000)throw new IllegalArgumentException("Trang tài khoản không hợp lệ");
  Map<String,Object> query=new HashMap<>(input),result=new HashMap<>();
  Map<String,Map<String,Object>> unique=new LinkedHashMap<>();
  // The existing RPC paginates by registration date. Collect before activity sorting.
  for(int page=0;page<=1000000;page+=100){
   query.put("offset",page);
   Map<String,Object> response=Json.obj(data.call("accounts",query));
   if(page==0)result.putAll(response);
   List<?> rows=(List<?>)response.get("accounts");int previousSize=unique.size();
   for(Object value:rows){Map<String,Object> row=new HashMap<>(Json.obj(value));unique.put(Json.str(row,"id"),row);}
   if(rows.size()<100||unique.size()==previousSize||page+rows.size()>=Json.num(response,"total",Integer.MAX_VALUE))break;
  }
  long now=System.currentTimeMillis();Map<String,Instant> activity=new HashMap<>();
  for(var session:s.sessions.values())if(session.expiry>now)
   activity.merge(session.id,Instant.ofEpochMilli(session.lastSeen),(first,last)->first.isAfter(last)?first:last);
  Set<String> online=new HashSet<>();
  for(var entry:activity.entrySet()){
   if(now-entry.getValue().toEpochMilli()<60000)online.add(entry.getKey());
   Map<String,Object> row=unique.get(entry.getKey());
   if(row!=null&&entry.getValue().isAfter(accountTime(row,"last_seen_at")))row.put("last_seen_at",entry.getValue().toString());
  }
  List<Map<String,Object>> rows=new ArrayList<>(unique.values());
  for(var row:rows){Instant last=lastActivity(row);row.put("last_activity_at",last.equals(Instant.MIN)?null:last.toString());}
  rows.sort(Comparator.<Map<String,Object>,Boolean>comparing(row->online.contains(Json.str(row,"id"))).reversed()
   .thenComparing(row->accountTime(row,"last_activity_at"),Comparator.reverseOrder())
   .thenComparing(row->accountTime(row,"created_at"),Comparator.reverseOrder())
   .thenComparing(row->Json.str(row,"id")));
  int start=Math.min(offset,rows.size()),end=Math.min(start+100,rows.size());
  result.put("accounts",new ArrayList<>(rows.subList(start,end)));result.put("online",new ArrayList<>(online));
  result.put("matched",rows.size());result.put("now",Instant.ofEpochMilli(now).toString());return result;
 }
 static Instant accountTime(Map<String,Object> row,String field){
  String value=Json.str(row,field);return value.isEmpty()?Instant.MIN:OffsetDateTime.parse(value).toInstant();
 }
 static Instant lastActivity(Map<String,Object> row){
  return Collections.max(List.of(accountTime(row,"last_seen_at"),accountTime(row,"last_login_at")));
 }
 public void close(){tasks.shutdownNow();tokens.clear();}
}
