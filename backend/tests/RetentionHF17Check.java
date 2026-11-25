import java.time.*;
import java.util.*;

/** Deterministic current inactivity math; no production data. */
public final class RetentionHF17Check {
 static final Instant NOW=Instant.parse("2026-11-25T09:00:00Z");
 static final long DAY=86400;
 static int checks;
 static void eq(Object actual,Object expected,String name){if(!Objects.equals(actual,expected))throw new AssertionError(name+": "+actual+" != "+expected);checks++;}
 static Map<String,Object> row(int ago){Map<String,Object> r=new HashMap<>();r.put("created_at",NOW.minusSeconds(150*DAY).toString());r.put("last_login_at",NOW.minusSeconds(ago*DAY).toString());r.put("last_seen_at",NOW.minusSeconds(ago*DAY).toString());r.put("max_offline_seconds",900*DAY);r.put("retention_since",NOW.toString());return r;}
 static void end(Map<String,Object> r,Instant expected){eq(r.get("inactive_delete_at"),expected==null?null:expected.toString(),"deadline");}
 public static void main(String[] args){
  for(int ago=0;ago<=120;ago++){
   var r=row(ago);AdminServer.retentionFields(r,NOW,false,true,90);eq(r.get("offline_seconds"),ago*DAY,"current offline, never historical max");end(r,NOW.plusSeconds((90-ago)*DAY));
   AdminServer.retentionFields(r,NOW,false,false,90);end(r,null);
   r.put("last_login_at",NOW.toString());AdminServer.retentionFields(r,NOW,true,true,90);eq(r.get("offline_seconds"),0L,"login resets current period");end(r,NOW.plusSeconds(90*DAY));
  }
  var r=row(5);r.put("offline_days",30);AdminServer.retentionFields(r,NOW,false,true,90);end(r,NOW.plusSeconds(25*DAY));
  r.put("protected_until",NOW.plusSeconds(100*DAY).toString());AdminServer.retentionFields(r,NOW,false,true,90);end(r,NOW.plusSeconds(100*DAY));
  r.put("delete_at",NOW.plusSeconds(3*DAY).toString());AdminServer.retentionFields(r,NOW,false,true,90);eq(r.get("effective_delete_at"),r.get("delete_at"),"earlier manual deadline");
  AdminServer.retentionFields(r,NOW,false,false,90);end(r,null);eq(r.get("effective_delete_at"),r.get("delete_at"),"manual schedule independent of auto policy");
  var fresh=row(5);fresh.remove("last_login_at");fresh.remove("last_seen_at");AdminServer.retentionFields(fresh,NOW,false,true,90);eq(fresh.get("offline_seconds"),150*DAY,"never logged in: account creation");end(fresh,NOW.minusSeconds(60*DAY));
  var live=row(5);live.put("last_seen_at",NOW.minusSeconds(10).toString());AdminServer.retentionFields(live,NOW,true,true,90);eq(live.get("offline_seconds"),0L,"online current interval");end(live,NOW.plusSeconds(90*DAY-10));
  System.out.println("PASS: "+checks+" HF17 current-inactivity assertions");
 }
}
