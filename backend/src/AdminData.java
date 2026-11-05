import java.util.*;
/** Durable administrative records live in Supabase, never on Render's ephemeral disk. */
class AdminData {
 final CloudStore store;
 AdminData(CloudStore s){store=s;}
 Object call(String op,Map<String,Object> data)throws Exception{return store.request("/rest/v1/rpc/gunny_admin","POST",Json.write(Map.of("p_op",op,"p_data",data)),true);}
 Map<String,Object> login(String id)throws Exception{return Json.obj(call("login",Map.of("id",id)));}
}
