import java.io.*;import java.util.*;
final class Ranking793{
 static void write(GunnyServer server,GunnyServer.Session viewer,DataOutputStream out)throws IOException{
  Map<String,CloudStore.Profile> unique=new HashMap<>();for(var s:server.sessions.values())if(server.social.isOnline(s)&&!s.profile.testCheats)unique.put(s.id,s.profile);
  var list=new ArrayList<>(unique.values());list.sort(Comparator.<CloudStore.Profile>comparingInt(p->p.wins).reversed().thenComparing(Comparator.comparingInt((CloudStore.Profile p)->p.maxDamage).reversed()).thenComparing(p->p.id));
  out.writeShort(Math.min(128,list.size()));for(int i=0;i<Math.min(128,list.size());i++){var p=list.get(i);int favorite=p.equipped;for(int w=0;w<10;w++)if(p.weaponUses[w]>p.weaponUses[favorite])favorite=w;out.writeInt(p.publicId);out.writeInt(p.experience);out.writeUTF(p.name);out.writeByte(favorite);out.writeInt(p.maxDamage);out.writeInt(p.wins);}
  out.writeShort(Math.min(128,list.size()));for(int i=0;i<Math.min(128,list.size());i++){var p=list.get(i);out.writeByte(p.gender);out.writeByte(p.skin);out.writeBoolean(p.custom);out.writeByte(p.wornMask);for(int k=0;k<5;k++)out.writeByte(p.look[k]);}
 }
}
