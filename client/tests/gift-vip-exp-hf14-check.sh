#!/usr/bin/env bash
# Compile real GiftBag and VipRules against small MIDP/UI stand-ins.
set -euo pipefail
cd -- "$(dirname -- "$0")/.."
work=$(mktemp -d); trap 'rm -rf "$work"' EXIT
cp src/{GiftBag,VipRules}.java "$work/"
mkdir -p "$work/javax/microedition/lcdui" "$work/javax/microedition/rms"
printf 'package javax.microedition.lcdui; public class Image {}\n' > "$work/javax/microedition/lcdui/Image.java"
cat > "$work/javax/microedition/rms/RecordStore.java" <<'JAVA'
package javax.microedition.rms;
public class RecordStore{public static RecordStore openRecordStore(String n,boolean c){return new RecordStore();}public int getNumRecords(){return 0;}public byte[] getRecord(int n){return null;}public void closeRecordStore(){}public void addRecord(byte[] b,int o,int n){}public void setRecord(int id,byte[] b,int o,int n){}}
JAVA
cat > "$work/GiftClientHF14.java" <<'JAVA'
import java.io.*;import javax.microedition.lcdui.Image;
class PhoneCanvas{
 boolean online=true;int experience,coins;final Net net=new Net();final Eco economy=new Eco();final NativePopup popup=new NativePopup();final Game game=new Game();final Vip vip=new Vip();final Bag bag=new Bag();final Stone stoneShop=new Stone();String msg="";
 boolean onlineAccount(){return online;} Image combatRoomArt(String s){return new Image();}void message(String s){msg=s;}String nextEconomyKey(){return "test";}void saveSettings(){}void all(){}void img(Image a,int x,int y){}void pcCenter(String s,int x,int y){}void pcText(String s,int x,int y){}void rect(int x,int y,int w,int h,int c){}String pcShort(String s,int w){return s;}void drawSelectionGlow(int x,int y,int w,int h){}
 static class Net{boolean inBattle,giftCap=true,giftExpCap=true,busy;int roomId,phase;String pending="";void request(String op){pending=op;}}
 static class Eco{int giftLevel=1,giftBags=1,vipTickets=1,offlineStones;int[] offlineMaterials=new int[6];void save(){}}
 static class Game{int rand(int n){return 0;}}
 static class Vip{int[] cards=new int[13];boolean read;Image art=new Image();Image cardIcon(int n){return art;}void read(DataInputStream in)throws IOException{in.readInt();in.readByte();in.readByte();for(int i=0;i<13;i++)cards[i]=in.readInt();in.readInt();read=true;}}
 static class Bag{int count;void list(){count++;}}static class Stone{Image icon(int i){return new Image();}}
}
class NativePopup{Image frame,button,close;int width,height,left,top,choiceCount,rows,visibleRows;String title;void notice(String t,String s){title=t;}Image fitFrame(){return frame;}int buttonWidth(String s){return 70;}Image fittedButton(int i,int w){return button;}}
class Progression{static int level(int x){return 1;}}
public class GiftClientHF14{
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{PhoneCanvas c=new PhoneCanvas();GiftBag g=new GiftBag(c);g.use(1);ok(c.net.pending.equals("giftopenexp"),"new endpoint");
 for(int k=0;k<9;k++){ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);o.writeByte(2);o.writeByte(3);o.writeInt(20);o.writeByte(10+k);o.writeInt(1);o.writeInt(0);o.writeByte(0);o.writeByte(-1);for(int i=0;i<13;i++)o.writeInt(i==k?1:0);o.writeInt(0);DataInputStream in=new DataInputStream(new ByteArrayInputStream(b.toByteArray()));g.read(in,true);ok(in.available()==0&&c.vip.read&&c.vip.cards[k]==1&&g.rewardName(10+k,1).equals("Thẻ VIP "+(k+1))&&g.rewardIcon(10+k)==c.vip.art&&g.showing,"tier and immediate inventory");g.draw();}
 c.net.pending="";c.net.giftExpCap=false;g.use(1);ok(c.net.pending.length()==0&&c.msg.indexOf("HF14")>=0,"old server blocked");c.online=false;g.bags=1;g.use(1);ok(g.rewards.length==2&&g.rewards[0]==3&&!g.visible(2),"offline never mints time VIP");System.out.println("PASS: client GiftBag endpoint, nine tier names/icons, immediate VIP state, offline isolation");}
}
JAVA
javac -encoding UTF-8 -d "$work" "$work"/*.java "$work"/javax/microedition/{lcdui,rms}/*.java
java -cp "$work" GiftClientHF14
