#!/usr/bin/env bash
# Real VipSystem compiled with minimal MIDP/UI stand-ins. No handset emulator required.
set -euo pipefail
cd -- "$(dirname -- "$0")/.."
work=$(mktemp -d); trap 'rm -rf "$work"' EXIT
cp src/{VipSystem,VipRules,WingBattleArt,WingAttachment,WingOneArt,WingGroupArt,WingBalancedArt}.java "$work/"
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
 javax.microedition.lcdui.Graphics g=new javax.microedition.lcdui.Graphics();int ox=0,oy=0;boolean customLook;int s=1,mode=14,frame,cameraX,cameraY,shoppingMoney,reads,draws,anim=1,lastX,lastY;boolean pose;int maxWingFrames=10;Image lastImage;
 final Battle game=new Battle();final Net net=new Net();final Bag bag=new Bag();final Shop shop=new Shop();final Tip tooltip=new Tip();
 boolean onlineAccount(){return true;}boolean onlineBattle(){return true;}boolean attackPose(int i){return pose;}boolean freezeHolding(int i){return false;}int unitAnimation(int i){return anim;}
 Image combatRoomArt(String n){reads++;if(n.startsWith("wingbal")&&Integer.parseInt(n.substring(7,n.indexOf('-')))>maxWingFrames)return null;return new Image();}void part(Image i,int x,int y,int w,int h,int px,int py,boolean flip){if(i!=null){draws++;lastImage=i;lastX=px;lastY=py;}}void img(Image i,int x,int y){}void pcText(String a,int x,int y){}void pcCenter(String a,int x,int y){}String pcShort(String a,int n){return a;}void drawItemSelection(int a,int b,int c,int d,boolean f){}void message(String a){}String nextEconomyKey(){return "k";}
 static class Net{int count=2,roomKind;boolean vipCap=true;boolean[] custom={true,true},present={true,true},bots={false,false};int[] actorWing={3,4,-1,-1,-1,-1},actorVip={9500,9500,0,0,0,0};String pending="",operation="";void request(String a){}}
 static class Bag{void list(){}}static class Shop{Image card,money;int[] box(int i){return new int[4];}}static class Tip{void goods(String a,String b,String c,int[] d){}}
}
class Battle {int unitCount=2;Unit[] units={new Unit(),new Unit()};static class Unit{int npc,slope,face=1,x,y;}static int clamp(int x,int l,int h){return Math.max(l,Math.min(h,x));}static int cos(int a){return (int)(Math.cos(a*Math.PI/180)*1024);}static int sin(int a){return (int)(Math.sin(a*Math.PI/180)*1024);}}
class NativeUi{static Image fit(Image i,int a,int b,int c){return i;}}
public class WingCacheCheck{
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
 int checks=0;
 for(int scale=1;scale<=2;scale++)for(int wing=0;wing<6;wing++)for(int tier=0;tier<3;tier++){
  PhoneCanvas c=new PhoneCanvas();c.s=scale;c.net.actorWing[0]=wing;c.net.actorWing[1]=-1;c.maxWingFrames=tier==0?1:tier==1?4:10;VipSystem v=new VipSystem(c);v.prepareBattle();
  int reads=c.reads;
  for(int face=-1;face<=1;face+=2)for(int anim=0;anim<22;anim++)for(int layer=0;layer<2;layer++)for(int tick=0;tick<38;tick+=2){
   c.anim=anim;c.pose=anim>=10;c.game.units[0].face=face;c.game.units[0].slope=0;c.frame=tick;int pose=WingAttachment.pose(anim,scale==1);
   int wanted=WingAttachment.tilt(wing,pose,face,0),a=WingBalancedArt.select(wing,wanted),frames=v.balancedFrames[wing][a];
   ok(WingBalancedArt.angle(wing,a)==wanted,"exact flat pose angle wing="+wing+" pose="+pose);
   ok(frames==c.maxWingFrames,"tier for pose "+wing+" scale "+scale+" frames "+frames+" expected "+c.maxWingFrames);
   int[] b=WingBalancedArt.bounds(scale,wing,a);int q=WingBalancedArt.index(wing,a,layer);int rx=b[q]-(a>=5?0:48),ry=b[q+1]-(a>=5?0:48);
   v.battleWing(0,layer==1);ok(c.g.image==v.balancedWings[wing][a][layer],"image");
   ok(c.g.x==(WingAttachment.jointX(wing,pose,face,0)+(face<0?-rx-b[q+2]:rx))*scale&&c.g.y==(WingAttachment.jointY(wing,pose,face,0)+ry)*scale,"mirror and crop placement");
   ok(c.g.sx==(tick/2%19*frames/19)*b[q+2]*scale,"frame");checks++;
  }
  ok(c.reads==reads,"no loads in paint");v.release();for(int a=0;a<15;a++)ok(v.balancedFrames[wing][a]==0,"release");
 }
 // Mixed wing types: low-memory admission is simulated by refusing animated atlas decodes.
 for(int scale=1;scale<=2;scale++)for(int first=0;first<6;first++)for(int second=0;second<6;second++){
  PhoneCanvas c=new PhoneCanvas();c.s=scale;c.maxWingFrames=1;c.net.actorWing[0]=first;c.net.actorWing[1]=second;VipSystem v=new VipSystem(c);v.prepareBattle();int reads=c.reads;
  for(int actor=0;actor<2;actor++)for(int anim=0;anim<22;anim++){
   int w=c.net.actorWing[actor],pose=WingAttachment.pose(anim,scale==1);int a=WingBalancedArt.select(w,WingAttachment.tilt(w,pose,1,0));
   ok(v.balancedFrames[w][a]==1,"mixed static pose");c.anim=anim;c.pose=anim>=10;c.game.units[actor].slope=0;v.battleWing(actor,false);ok(c.g.image==v.balancedWings[w][a][0],"mixed render");checks++;
  }
  ok(c.reads==reads,"mixed paint cache only");
 }
 System.out.println("PASS "+checks+" balanced wing draws; static/4/10 frame tiers, exact pose angles, both scales, no image loads in paint");
 }
}
JAVA
javac -encoding UTF-8 -d "$work" "$work"/*.java "$work"/javax/microedition/lcdui/*.java "$work"/javax/microedition/lcdui/game/*.java
java -Xmx128m -cp "$work" WingCacheCheck
