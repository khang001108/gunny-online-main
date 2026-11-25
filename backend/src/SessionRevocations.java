import java.util.*;
/** Recently replaced tokens receive a distinct error, without retaining their profiles. */
final class SessionRevocations {
 static final String REPLACED="Tài khoản đã đăng nhập trên thiết bị khác.";
 static final String EXPIRED="Phiên đăng nhập hết hạn. Hãy đăng nhập lại.";
 private final LinkedHashMap<String,Long> tokens=new LinkedHashMap<>();
 synchronized void replace(String token,long now){tokens.put(token,now+30*60*1000L);while(tokens.size()>4096)tokens.remove(tokens.keySet().iterator().next());}
 synchronized String reason(String token,long now){Long until=tokens.get(token);if(until!=null&&until>now)return REPLACED;if(until!=null)tokens.remove(token);return EXPIRED;}
}
