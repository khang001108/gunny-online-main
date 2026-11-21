import java.io.*;import java.net.*;import java.nio.file.*;import java.util.*;
public final class VipTcp790Check{
 static int checks;static void check(boolean b,String m){if(!b)throw new AssertionError(m);checks++;}
 static int[] state(DataInputStream in)throws Exception{int[] a=new int[17];a[0]=in.readInt();a[1]=in.readUnsignedByte();a[2]=in.readByte();for(int i=0;i<13;i++)a[3+i]=in.readInt();a[16]=in.readInt();check(in.available()==0,"VIP wire length");return a;}
 public static void main(String[] args)throws Exception{var store=new CloudStore.Memory();String email="vip790@tcp.test";store.register(email,"test-password-123","VIP TCP");var profile=store.profile(store.login(email,"test-password-123").userId);store.vip(profile.id,"grant",0,6,"tcp-grant");profile.shoppingMoney=10000;
 try(var server=new GunnyServer(new InetSocketAddress("127.0.0.1",0),Path.of("backend/assets"),store)){server.clock.shutdownNow();server.startTcp("127.0.0.1",0,6);int flags=TribalTcpCheck.FLAGS|134217728;var p=new TribalTcpCheck.Player(server.tcpPort(),email,flags);
 try{check(server.sessions.get(p.token).vip,"VIP capability negotiated through TCP");check(state(p.call("vipstate",null))[3]==6,"Inventory delivered through TCP");
 for(int i=0;i<5;i++){String key="tcp-use-"+i;state(p.call("vipuse",o->{o.writeByte(0);o.writeUTF(key);}));}
 int[] before=state(p.call("vipstate",null));check(before[0]==500&&before[3]==1,"VIP1 after five cards");
 state(p.call("vipuse",o->{o.writeByte(0);o.writeUTF("tcp-use-4");}));check(profile.vipExp==500&&profile.vipCards[0]==1,"Network retry is idempotent");
 state(p.call("vipbuy",o->{o.writeByte(0);o.writeUTF("tcp-buy");}));check(profile.shoppingMoney==9800&&profile.wingOwned==1,"TCP purchase uses server price");state(p.call("vipequip",o->{o.writeByte(0);o.writeUTF("tcp-equip");}));check(profile.equippedWing==0,"TCP equips purchased wing");
 p.close();p=new TribalTcpCheck.Player(server.tcpPort(),email,flags);int[] after=state(p.call("vipstate",null));check(after[0]==500&&after[2]==0&&after[3]==1&&after[16]==9800,"Login preserves VIP, inventory, equipped wing and wallet");state(p.call("vipequip",o->{o.writeByte(-1);o.writeUTF("tcp-unequip");}));check(profile.equippedWing==-1,"TCP unequips wing");
 }finally{p.close();}}
 System.out.println("PASS: "+checks+" real TCP VIP inventory/purchase/equip/login/retry checks");}
}
