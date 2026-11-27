#!/usr/bin/env bash
# Real VipSystem compiled with minimal MIDP/UI stand-ins. No handset emulator required.
set -euo pipefail
cd -- "$(dirname -- "$0")/.."
work=$(mktemp -d); trap 'rm -rf "$work"' EXIT
cp src/{VipSystem,VipRules,WingBattleArt,WingAttachment}.java "$work/"
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
 public static void main(String[] a){PhoneCanvas c=new PhoneCanvas();VipSystem v=new VipSystem(c);v.prepareBattle();ok(v.battleReady,"loading ready");ok(v.battleAngles[3]==31&&v.battleAngles[4]==31,"all angles for two wings");ok(v.battleAngles[0]==0,"unequipped not loaded");int n=c.reads;c.mode=1;
 int checks=0;
 for(int scale=1;scale<=2;scale++)for(int face=-1;face<=1;face+=2)for(int slope=-65;slope<=65;slope+=5)for(int anim=1;anim<=21;anim++)for(int layer=0;layer<2;layer++){
   c.s=scale;Battle.Unit u=c.game.units[0];u.face=face;u.slope=slope;u.x=100;u.y=120;c.anim=anim;c.pose=anim>=10;
   c.frame=17;c.cameraX=7;c.cameraY=9;c.ox=3;c.oy=4;
   v.battleWing(0,layer==1);int t=c.pose?0:slope;
   int pose=WingAttachment.pose(anim,scale==1);int desired=WingAttachment.tilt(pose,face,t);
   int choice=WingAttachment.select(31,desired),q=choice/5,angle=choice%5;
   int[] b=WingBattleArt.bounds(scale);int p=WingBattleArt.index(3,angle,layer);
   ok(c.g.image==v.sideWings[3][angle][layer],"selected atlas");
   ok(c.g.sx==8*b[p+2]*scale,"same flap frame");
   // Independent corner transform in floating point, rotate then mirror.
   double minX=1e9,minY=1e9;for(int corner=0;corner<4;corner++){
     int xx=b[p]-48+((corner&1)!=0?b[p+2]:0),yy=b[p+1]-48+((corner&2)!=0?b[p+3]:0);
     double rad=q*Math.PI/2;double rx=xx*Math.cos(rad)-yy*Math.sin(rad),ry=xx*Math.sin(rad)+yy*Math.cos(rad);
     minX=Math.min(minX,rx*face);minY=Math.min(minY,ry);
   }
   int ax=WingAttachment.jointX(pose,face,t),ay=WingAttachment.jointY(pose,face,t);
   ok(c.g.x==3+(100-7+ax+(int)Math.round(minX))*scale&&c.g.y==4+(120-9+ay+(int)Math.round(minY))*scale,"joint/crop transform");
   int[] right={0,5,3,6},left={2,4,1,7};ok(c.g.transform==(face<0?left[q]:right[q]),"mirror order");
   ok(q==0,"no quarter turn");int delta=Math.abs((Battle.clamp(desired,-30,30)-(angle*15-30)+720)%360);delta=Math.min(delta,360-delta);ok(delta<=15,"angle tolerance");checks++;
 }
 ok(WingAttachment.jointX(1,1,0)==-4&&WingAttachment.jointY(1,1,0)==-5,"flat crouch rear joint");
 ok(WingAttachment.tilt(1,1,0)==15&&WingAttachment.tilt(10,1,0)==0,"pose change");
 System.out.println("PASS: "+checks+" attachment draws on both scales");
 c.s=1;
 c.anim=1;

 for(int f=0;f<100;f++){c.frame=f;c.game.units[0].slope=-30+(f%5)*15;c.game.units[0].face=f%2==0?1:-1;c.pose=f%3==0;v.battleWing(0,false);v.battleWing(0,true);v.back(v.actorExp(0),v.actorWing(0),0,0,1,false);v.back(v.actorExp(1),v.actorWing(1),0,0,2,false);v.releaseLargeEffects();}
 ok(c.reads==n&&c.g.draws>=200,"no in-battle wing/VIP loads across poses");c.net.actorWing[0]=1;ok(v.actorWing(0)==3,"encounter equipment frozen");ok(WingBattleArt.nearest(4,0)==2&&WingBattleArt.nearest(0,2)==-1,"safe fallback");v.release();ok(!v.battleReady&&v.battleAngles[3]==0&&v.sideWings[3][2][0]==null,"release on exit");System.out.println("PASS: preload equipped wings; cache-only render; immutable equipment; fallback; release");}
}
JAVA
javac -encoding UTF-8 -d "$work" "$work"/*.java "$work"/javax/microedition/lcdui/*.java "$work"/javax/microedition/lcdui/game/*.java
java -Xmx64m -cp "$work" WingCacheCheck
