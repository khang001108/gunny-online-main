#!/usr/bin/env bash
# Real VipSystem compiled with minimal MIDP/UI stand-ins. No handset emulator required.
set -euo pipefail
cd -- "$(dirname -- "$0")/.."
work=$(mktemp -d); trap 'rm -rf "$work"' EXIT
cp src/{VipSystem,VipRules,WingBattleArt,WingAttachment,WingOneArt,WingGroupArt}.java "$work/"
mkdir -p "$work/javax/microedition/lcdui/game"
printf 'package javax.microedition.lcdui; public class Image {}\n' > "$work/javax/microedition/lcdui/Image.java"
cat > "$work/javax/microedition/lcdui/Graphics.java" <<'JAVA'
package javax.microedition.lcdui;
public class Graphics { public static final int TOP=16,LEFT=4; public int x,y,w,h,sx,transform,draws;public Image image; public void drawRegion(Image i,int sx,int sy,int w,int h,int t,int x,int y,int anchor){this.image=i;this.x=x;this.y=y;this.w=w;this.h=h;this.sx=sx;this.transform=t;draws++;} }
JAVA
cat > "$work/javax/microedition/lcdui/game/Sprite.java" <<'JAVA'
package javax.microedition.lcdui.game;
public class Sprite {public static final int TRANS_NONE=0,TRANS_MIRROR_ROT180=1,TRANS_MIRROR=2,TRANS_ROT180=3,TRANS_MIRROR_ROT270=4,TRANS_ROT90=5,TRANS_ROT270=6,TRANS_MIRROR_ROT90=7;}
JAVA
cat > "$work/WingCacheCheck.java" <<'JAVA'
import javax.microedition.lcdui.Image;
class PhoneCanvas {
 javax.microedition.lcdui.Graphics g=new javax.microedition.lcdui.Graphics();int ox=0,oy=0;boolean customLook;int s=1,mode=14,frame,cameraX,cameraY,shoppingMoney,reads,draws,anim=1,lastX,lastY;boolean pose;Image lastImage;
 final Battle game=new Battle();final Net net=new Net();final Bag bag=new Bag();final Shop shop=new Shop();final Tip tooltip=new Tip();
 boolean onlineAccount(){return true;}boolean onlineBattle(){return true;}boolean attackPose(int i){return pose;}boolean freezeHolding(int i){return false;}int unitAnimation(int i){return anim;}
 Image combatRoomArt(String n){reads++;return new Image();}void part(Image i,int x,int y,int w,int h,int px,int py,boolean flip){if(i!=null){draws++;lastImage=i;lastX=px;lastY=py;}}void img(Image i,int x,int y){}void pcText(String a,int x,int y){}void pcCenter(String a,int x,int y){}String pcShort(String a,int n){return a;}void drawItemSelection(int a,int b,int c,int d,boolean f){}void message(String a){}String nextEconomyKey(){return "k";}
 static class Net{int count=2,roomKind;boolean vipCap=true;boolean[] custom={true,true},present={true,true},bots={false,false};int[] actorWing={3,4,-1,-1,-1,-1},actorVip={9500,9500,0,0,0,0};String pending="",operation="";void request(String a){}}
 static class Bag{void list(){}}static class Shop{Image card,money;int[] box(int i){return new int[4];}}static class Tip{void goods(String a,String b,String c,int[] d){}}
}
class Battle {int unitCount=2;Unit[] units={new Unit(),new Unit()};static class Unit{int npc,slope,face=1,x,y;}static int clamp(int x,int l,int h){return Math.max(l,Math.min(h,x));}static int cos(int a){return (int)(Math.cos(a*Math.PI/180)*1024);}static int sin(int a){return (int)(Math.sin(a*Math.PI/180)*1024);}}
class NativeUi{static Image fit(Image i,int a,int b,int c){return i;}}
public class WingCacheCheck{
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
 int checks=0;int[] GX={-3,0,-1,0,-1,0,-1,0,-1,0,-4,-4,-4,-4,-4,-4,-4,-3,-2,-4,-4,-4};
 int[] GY={-8,-5,-5,-5,-5,-5,-5,-5,-5,-5,-7,-7,-7,-7,-7,-7,-7,-7,-7,-7,-7,-7};
 int[] GL={0,15,15,15,15,15,15,15,15,15,-5,-6,-12,-13,-12,-10,-15,-29,-34,0,5,7};
 for(int scale=1;scale<=2;scale++)for(int wing=1;wing<=4;wing++){
  int[] X=wing==4?new int[]{-6,-6,-5,-6,-5,-6,-5,-6,-5,-6,-6,-6,-6,-6,-6,-6,-6,-6,-6,-6,-6,-6}:wing==3?new int[]{2,3,2,3,2,3,2,3,2,3,2,2,2,2,2,2,2,2,2,2,2,2}:GX,Y=wing==4?new int[]{-7,-4,-4,-4,-4,-4,-4,-4,-4,-4,-7,-7,-7,-7,-7,-7,-7,-7,-7,-7,-7,-7}:wing==3?new int[]{-5,0,0,0,0,0,0,0,0,0,-8,-8,-8,-8,-8,-8,-9,-10,-11,-9,-6,-7}:GY,LEAN=wing==4?new int[]{-40,15,15,15,15,15,15,15,15,15,-50,-45,-55,-50,-50,-45,-60,-70,-75,-55,-50,-50}:wing==3?new int[]{-35,0,0,0,0,0,0,5,0,-5,-40,-40,-50,-50,-50,-45,-60,-70,-80,-55,-45,-40}:GL;
  PhoneCanvas c=new PhoneCanvas();c.s=scale;c.net.actorWing[0]=wing;c.net.actorWing[1]=-1;VipSystem v=new VipSystem(c);v.prepareBattle();
  ok(v.wingGroupMask[wing-1]==(1<<WingGroupArt.angles(wing).length)-1,"all extra angles for one wing at scale "+scale+" wing "+wing);int reads=c.reads;
  for(int face=-1;face<=1;face+=2)for(int anim=0;anim<22;anim++)for(int layer=0;layer<2;layer++){
   c.anim=anim;c.pose=anim>=10;c.game.units[0].face=face;c.game.units[0].slope=0;c.frame=12;int pose=WingAttachment.pose(anim,scale==1);
   int angle=WingGroupArt.select(wing,v.battleAngles[wing],v.wingGroupMask[wing-1],LEAN[pose]);
   ok((angle<5?angle*15-30:WingGroupArt.angles(wing)[angle-5])==LEAN[pose],"exact configured angle");
   ok(WingAttachment.jointX(wing,pose,face,0)==X[pose]*face&&WingAttachment.jointY(wing,pose,face,0)==Y[pose],"JSON coordinates");
   int[] b=angle>=5?WingGroupArt.bounds(scale):WingBattleArt.bounds(scale);int q=angle>=5?WingGroupArt.index(wing,angle-5,layer):WingBattleArt.index(wing,angle,layer);int rx=b[q]-(angle>=5?0:48),ry=b[q+1]-(angle>=5?0:48);
   v.battleWing(0,layer==1);ok(c.g.image==(angle>=5?v.wingGroupAngles[wing-1][angle-5][layer]:v.sideWings[wing][angle][layer]),"image");
   ok(c.g.x==(X[pose]*face+(face<0?-rx-b[q+2]:rx))*scale&&c.g.y==(Y[pose]+ry)*scale,"mirror and crop placement");ok(c.g.sx==6*b[q+2]*scale,"frame");checks++;
  }
  ok(c.reads==reads,"no loads in paint");v.release();ok(v.wingGroupMask[wing-1]==0,"release");
 }
 ok(WingAttachment.tilt(0,18,1,0)==-104&&WingAttachment.jointX(0,18,1,0)==-5,"wing 1 preserved");
 ok(WingAttachment.tilt(5,18,1,0)==-15&&WingAttachment.jointX(5,18,1,0)==-2,"wing 6 preserved");
 System.out.println("PASS "+checks+" wing-group draws; exact angles, coordinates, both scales, cache-only, release and unchanged wing 1/6");
 }
}
JAVA
javac -encoding UTF-8 -d "$work" "$work"/*.java "$work"/javax/microedition/lcdui/*.java "$work"/javax/microedition/lcdui/game/*.java
java -Xmx128m -cp "$work" WingCacheCheck
