import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Only this server knows the service role key. The MIDlet never receives it. */
class CloudStore {
    static final class Profile {
        String id,name;int publicId,gender,skin,coins=1000,wins,owned=1,equipped;boolean custom;int[] look=new int[5];
        Profile(String id,String name){this.id=id;this.name=name;}
    }
    static final class Login {String userId;int lifetime=3600;Login(String id){userId=id;}}
    final String url,key,service;
    final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12)).followRedirects(HttpClient.Redirect.NEVER).build();
    CloudStore(){this(env("SUPABASE_URL"),env("SUPABASE_PUBLISHABLE_KEY").isEmpty()?env("SUPABASE_ANON_KEY"):env("SUPABASE_PUBLISHABLE_KEY"),env("SUPABASE_SECRET_KEY").isEmpty()?env("SUPABASE_SERVICE_ROLE_KEY"):env("SUPABASE_SECRET_KEY"),false);}
    CloudStore(String address,String publicKey,String privateKey,boolean testLoopback){url=address.replaceAll("/+$","");key=publicKey;service=privateKey;boolean local=testLoopback&&(url.startsWith("http://127.0.0.1:")||url.startsWith("http://localhost:"));if((!url.startsWith("https://")&&!local)||key.isEmpty()||service.isEmpty())throw new IllegalStateException("Configure SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY and SUPABASE_SECRET_KEY");}
    CloudStore(boolean test){url=key=service="";}
    static String env(String key){String v=System.getenv(key);return v==null?"":v.trim();}
    Object request(String path,String method,String body,boolean admin)throws Exception {
        HttpRequest.Builder b=HttpRequest.newBuilder(URI.create(url+path)).timeout(Duration.ofSeconds(20)).header("apikey",admin?service:key).header("Content-Type","application/json");
        if(admin&&!service.startsWith("sb_secret_"))b.header("Authorization","Bearer "+service);
        HttpResponse<String> r=http.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        if(r.statusCode()<200||r.statusCode()>=300){String detail=r.body();if(detail.contains("user_already_exists")||detail.contains("email_exists")||detail.contains("User already registered"))throw new IllegalArgumentException("Email đã tồn tại. Hãy chọn Đăng nhập với mật khẩu cũ.");if(detail.contains("email_not_confirmed"))throw new IllegalArgumentException("Email chưa xác nhận. Mở thư Gunny và bấm Xác nhận email, rồi đăng nhập lại.");if(detail.contains("Card already taken"))throw new IllegalArgumentException("Thẻ đã được mở. Chọn thẻ khác.");if(detail.contains("Insufficient coins"))throw new IllegalArgumentException("Chưa đủ vàng để mua vũ khí.");if(detail.contains("email_address_not_authorized")||detail.contains("Error sending confirmation"))throw new IllegalArgumentException("Supabase chưa gửi được email. Kiểm tra SMTP hoặc dùng tài khoản đã xác nhận.");if(r.statusCode()==429)throw new IllegalArgumentException("Dịch vụ đang giới hạn yêu cầu. Thử lại sau.");if(r.statusCode()==400||r.statusCode()==401||r.statusCode()==422)throw new IllegalArgumentException("Thông tin không hợp lệ, email chưa xác nhận hoặc tài khoản chưa đúng.");throw new IllegalStateException("Dịch vụ tài khoản tạm thời không phản hồi ("+r.statusCode()+").");}
        return r.body().isBlank()?null:Json.parse(r.body());
    }
    void register(String email,String password,String name)throws Exception{Object reply=request("/auth/v1/signup","POST",Json.write(Map.of("email",email,"password",password,"data",Map.of("display_name",name))),false);Map<String,Object> r=Json.obj(reply);Object user=r.get("user");if(user instanceof Map)r=Json.obj(user);if(r.get("identities") instanceof List&&((List<?>)r.get("identities")).isEmpty())throw new IllegalArgumentException("Email đã tồn tại. Hãy chọn Đăng nhập với mật khẩu cũ.");}
    Login login(String email,String password)throws Exception{Map<String,Object> r=Json.obj(request("/auth/v1/token?grant_type=password","POST",Json.write(Map.of("email",email,"password",password)),false));Login l=new Login(Json.str(Json.obj(r.get("user")),"id"));l.lifetime=Math.min(3600,Json.num(r,"expires_in",3600));return l;}
    Profile profile(String id)throws Exception {
        UUID.fromString(id);List<?> rows=(List<?>)request("/rest/v1/profiles?id=eq."+id+"&select=*","GET",null,true);
        if(rows.isEmpty())throw new IllegalStateException("Chưa có hồ sơ. Hãy chạy migration Supabase.");Map<String,Object> r=Json.obj(rows.get(0));Profile p=new Profile(id,Json.str(r,"display_name"));p.publicId=Json.num(r,"public_id",0);p.gender=Json.num(r,"gender",0);p.skin=Json.num(r,"skin",0);p.coins=Json.num(r,"coins",0);p.wins=Json.num(r,"wins",0);p.owned=Json.num(r,"owned",1);p.equipped=Json.num(r,"equipped",0);p.custom=Boolean.TRUE.equals(r.get("custom_look"));Object look=r.get("look");if(look instanceof List)for(int i=0;i<5&&i<((List<?>)look).size();i++)p.look[i]=((Number)((List<?>)look).get(i)).intValue();return p;
    }
    Profile appearance(String id,String name,int gender,int skin,boolean custom,int[] look)throws Exception {UUID.fromString(id);request("/rest/v1/profiles?id=eq."+id,"PATCH",Json.write(Map.of("display_name",name,"gender",gender,"skin",skin,"custom_look",custom,"look",Arrays.stream(look).boxed().toList())),true);return profile(id);}
    Profile buy(String id,int weapon)throws Exception{request("/rest/v1/rpc/gunny_buy_weapon","POST",Json.write(Map.of("p_user",id,"p_weapon",weapon)),true);return profile(id);}
    void settle(String match,int winner,List<Map<String,Object>> players)throws Exception{request("/rest/v1/rpc/gunny_finish_match","POST",Json.write(Map.of("p_match",match,"p_winner",winner,"p_players",players)),true);}

    int[] claim(String match,String id,int card,int coins)throws Exception{Map<String,Object> r=Json.obj(request("/rest/v1/rpc/gunny_claim_card","POST",Json.write(Map.of("p_match",match,"p_user",id,"p_card",card,"p_coins",coins)),true));return new int[]{Json.num(r,"card",-1),Json.num(r,"coins",0)};}
    /** Explicit loopback-only test adapter; unavailable in a deployed Render service. */
    static final class Memory extends CloudStore {
        record Account(byte[] salt,byte[] hash,Profile profile){}
        final Map<String,Account> accounts=new ConcurrentHashMap<>();final Map<String,int[]> rewards=new ConcurrentHashMap<>();final Map<String,Set<String>> participants=new ConcurrentHashMap<>();final Set<String> settled=ConcurrentHashMap.newKeySet();final SecureRandom random=new SecureRandom();
        Memory(){super(true);}
        byte[] hash(String p,byte[] salt)throws Exception{PBEKeySpec spec=new PBEKeySpec(p.toCharArray(),salt,120000,256);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}finally{spec.clearPassword();}}
        synchronized void register(String email,String password,String name)throws Exception{if(accounts.containsKey(email))throw new IllegalArgumentException("Email đã được dùng.");byte[] salt=new byte[16];random.nextBytes(salt);Profile p=new Profile(UUID.randomUUID().toString(),name);p.publicId=accounts.size()+1001;accounts.put(email,new Account(salt,hash(password,salt),p));}
        Login login(String email,String password)throws Exception{Account a=accounts.get(email);if(a==null||!MessageDigest.isEqual(a.hash,hash(password,a.salt)))throw new IllegalArgumentException("Sai email hoặc mật khẩu.");return new Login(a.profile.id);}
        Profile profile(String id){for(Account a:accounts.values())if(a.profile.id.equals(id))return a.profile;throw new IllegalArgumentException("Không có tài khoản.");}
        synchronized Profile appearance(String id,String name,int gender,int skin,boolean custom,int[] look){Profile p=profile(id);p.name=name;p.gender=gender;p.skin=skin;p.custom=custom;p.look=look.clone();return p;}
        synchronized Profile buy(String id,int weapon){Profile p=profile(id);int price=weapon==0?0:weapon==3?450:weapon==5?800:1200;if((p.owned&(1<<weapon))==0){if(p.coins<price)throw new IllegalArgumentException("Không đủ vàng.");p.coins-=price;p.owned|=1<<weapon;}p.equipped=weapon;return p;}
        synchronized int[] claim(String match,String id,int card,int coins){if(card<0||card>8||coins<50||coins>200||!participants.getOrDefault(match,Set.of()).contains(id))throw new IllegalArgumentException("Không có quyền nhận thưởng.");String key=match+":"+id;if(rewards.containsKey(key))return rewards.get(key).clone();for(Map.Entry<String,int[]> e:rewards.entrySet())if(e.getKey().startsWith(match+":")&&e.getValue()[0]==card)throw new IllegalArgumentException("Thẻ đã được mở. Chọn thẻ khác.");Profile p=profile(id);p.coins=Math.min(999999,p.coins+coins);int[] r={card,coins};rewards.put(key,r);return r.clone();}
        synchronized void settle(String match,int winner,List<Map<String,Object>> players){if(!settled.add(match))return;Set<String> ids=new HashSet<>();for(Map<String,Object> p:players)ids.add((String)p.get("id"));participants.put(match,ids);for(Map<String,Object> player:players){Profile p=profile((String)player.get("id"));boolean won=((Number)player.get("team")).intValue()==winner;p.coins=Math.min(999999,p.coins+(won?300:80));if(won)p.wins++;}}
    }
}
