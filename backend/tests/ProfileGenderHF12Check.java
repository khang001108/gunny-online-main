import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public final class ProfileGenderHF12Check {
 static int checks;
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
 static byte[] info(GunnyServer server,GunnyServer.Session session,int target,boolean both)throws Exception{
  ByteArrayOutputStream req=new ByteArrayOutputStream(),res=new ByteArrayOutputStream();
  DataOutputStream out=new DataOutputStream(req);out.writeInt(target);if(both)out.writeByte(1);
  server.social.handle("playerinfo",session,new DataInputStream(new ByteArrayInputStream(req.toByteArray())),new DataOutputStream(res));return res.toByteArray();
 }
 public static void main(String[] args)throws Exception{
  CloudStore.Memory store=new CloudStore.Memory();store.register("target@hf12.test","test-password-123","Target");
  CloudStore.Profile p=store.profile(store.login("target@hf12.test","test-password-123").userId);
  try(GunnyServer server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){
   GunnyServer.Session viewer=new GunnyServer.Session();viewer.expansion=true;viewer.vip=true;
   for(int sex=0;sex<2;sex++){
    p.gender=sex;p.skin=4;p.look=new int[]{1,2,3,4,5};p.wornMask=125;p.equipped=3;
    Arrays.fill(p.wardrobe,0);p.wardrobe[(1-sex)*6+1]=2;p.wardrobe[(1-sex)*6+2]=1;p.wardrobe[(1-sex)*6+4]=16;
    int[] look=p.look.clone();int[] wardrobe=p.wardrobe.clone();
    byte[] old=info(server,viewer,p.publicId,false),now=info(server,viewer,p.publicId,true);
    ok(now.length==old.length+9,"nine byte extension");ok(Arrays.equals(old,Arrays.copyOf(now,old.length)),"old prefix unchanged");
    DataInputStream in=new DataInputStream(new ByteArrayInputStream(now,old.length,9));
    ok(in.readUnsignedByte()==0x12,"marker");ok(in.readUnsignedByte()==1-sex,"other sex");ok(in.readUnsignedByte()==0,"skin fallback");ok(in.readUnsignedByte()==45,"only compatible clothing and weapon");
    int[] expected={1,0,0,4,0};for(int v:expected)ok(in.readUnsignedByte()==v,"matching gender switch item");
    ok(p.gender==sex&&p.skin==4&&p.wornMask==125&&p.equipped==3&&Arrays.equals(p.look,look)&&Arrays.equals(p.wardrobe,wardrobe),"view did not mutate target");
   }
  }
  System.out.println("Profile gender HF12: "+checks+" checks passed");
 }
}
