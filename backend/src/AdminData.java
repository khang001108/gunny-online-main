import java.util.*;
/** Durable administrative records live in Supabase, never on Render's ephemeral disk. */
class AdminData {
 final CloudStore store;
 AdminData(CloudStore s){store=s;}
 Object call(String op,Map<String,Object> data)throws Exception{
  // HF29: Json.parse uses Double; Postgres ->> integer must receive integral JSON.
  if(op.equals("wallet-add")){data=new HashMap<>(data);data.put("coins",Json.num(data,"coins",0));data.put("money",Json.num(data,"money",0));}
  if(op.equals("retention-save"))return store.request("/rest/v1/rpc/gunny_retention_save_hf15","POST",Json.write(Map.of("p_days",data.get("days"),"p_enabled",data.get("enabled"))),true);return store.request(op.startsWith("support-")?"/rest/v1/rpc/gunny_support_admin":"/rest/v1/rpc/gunny_admin","POST",Json.write(Map.of("p_op",op,"p_data",data)),true);}
 Map<String,Object> login(String id)throws Exception{return Json.obj(call("login",Map.of("id",id)));}
}
