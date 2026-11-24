#!/usr/bin/env bash
# Real VipSystem compiled with minimal MIDP/UI stand-ins. No handset emulator required.
set -euo pipefail
cd -- "$(dirname -- "$0")/.."
work=$(mktemp -d); trap 'rm -rf "$work"' EXIT
cp src/{VipSystem,VipRules,WingBattleArt}.java "$work/"
mkdir -p "$work/javax/microedition/lcdui"
printf 'package javax.microedition.lcdui; public class Image {}\n' > "$work/javax/microedition/lcdui/Image.java"
cat > "$work/WingCacheCheck.java" <<'JAVA'
import javax.microedition.lcdui.Image;
class PhoneCanvas {
 int s=1,mode=14,frame,cameraX,cameraY,shoppingMoney,reads,draws;boolean pose;
 final Battle game=new Battle();final Net net=new Net();final Bag bag=new Bag();final Shop shop=new Shop();final Tip tooltip=new Tip();
 boolean onlineAccount(){return true;}boolean onlineBattle(){return true;}boolean attackPose(int i){return pose;}boolean freezeHolding(int i){return false;}
 Image combatRoomArt(String n){reads++;return new Image();}void part(Image i,int x,int y,int w,int h,int px,int py,boolean flip){if(i!=null)draws++;}void img(Image i,int x,int y){}void pcText(String a,int x,int y){}void pcCenter(String a,int x,int y){}String pcShort(String a,int n){return a;}void drawItemSelection(int a,int b,int c,int d,boolean f){}void message(String a){}String nextEconomyKey(){return "k";}
 static class Net{int count=2,roomKind;boolean vipCap=true;boolean[] present={true,true};int[] actorWing={3,4,-1,-1,-1,-1},actorVip={9500,9500,0,0,0,0};String pending="",operation="";void request(String a){}}
 static class Bag{void list(){}}static class Shop{Image card;int[] box(int i){return new int[4];}}static class Tip{void goods(String a,String b,String c,int[] d){}}
}
class Battle {int unitCount=2;Unit[] units={new Unit(),new Unit()};static class Unit{int npc,slope,face=1,x,y;}static int clamp(int x,int l,int h){return Math.max(l,Math.min(h,x));}static int cos(int a){return 1024;}static int sin(int a){return 0;}}
class NativeUi{static Image fit(Image i,int a,int b,int c){return i;}}
public class WingCacheCheck{
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] a){PhoneCanvas c=new PhoneCanvas();VipSystem v=new VipSystem(c);v.prepareBattle();ok(v.battleReady,"loading ready");ok(v.battleAngles[3]==31&&v.battleAngles[4]==31,"all angles for two wings");ok(v.battleAngles[0]==0,"unequipped not loaded");int n=c.reads;c.mode=1;
 for(int f=0;f<100;f++){c.frame=f;c.game.units[0].slope=-30+(f%5)*15;c.game.units[0].face=f%2==0?1:-1;c.pose=f%3==0;v.battleWing(0,false);v.battleWing(0,true);v.back(v.actorExp(0),v.actorWing(0),0,0,1,false);v.back(v.actorExp(1),v.actorWing(1),0,0,2,false);v.releaseLargeEffects();}
 ok(c.reads==n&&c.draws>=200,"no in-battle wing/VIP loads across poses");c.net.actorWing[0]=1;ok(v.actorWing(0)==3,"encounter equipment frozen");ok(WingBattleArt.nearest(4,0)==2&&WingBattleArt.nearest(0,2)==-1,"safe fallback");v.release();ok(!v.battleReady&&v.battleAngles[3]==0&&v.sideWings[3][2][0]==null,"release on exit");System.out.println("PASS: preload equipped wings; cache-only render; immutable equipment; fallback; release");}
}
JAVA
javac -encoding UTF-8 -d "$work" "$work"/*.java "$work/javax/microedition/lcdui/Image.java"
java -Xmx64m -cp "$work" WingCacheCheck
