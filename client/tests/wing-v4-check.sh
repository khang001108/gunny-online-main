#!/usr/bin/env bash
# Real VipSystem compiled with minimal MIDP/UI stand-ins. No handset emulator required.
set -euo pipefail
cd -- "$(dirname -- "$0")/.."
work=$(mktemp -d); trap 'rm -rf "$work"' EXIT
cp src/{VipSystem,VipRules,WingBattleArt,WingAttachment,WingOneArt,WingGroupArt,WingBalancedArt,WingPackedArt}.java "$work/"
mkdir -p "$work/javax/microedition/lcdui/game"
printf 'package javax.microedition.lcdui; public class Image {public static boolean failNative; public int[] rgb; public int width,height; public static Image createRGBImage(int[] p,int w,int h,boolean a){if(failNative)throw new OutOfMemoryError();Image i=new Image();i.rgb=(int[])p.clone();i.width=w;i.height=h;return i;}}\n' > "$work/javax/microedition/lcdui/Image.java"
cat > "$work/javax/microedition/lcdui/Graphics.java" <<'JAVA'
package javax.microedition.lcdui;
public class Graphics {
 public static final int TOP=16,LEFT=4; public int x,y,w,h,draws;public int[] pixels;
 public void drawRegion(Image i,int sx,int sy,int w,int h,int t,int x,int y,int anchor){this.x=x;this.y=y;this.w=w;this.h=h;pixels=new int[w*h];for(int row=0;row<h;row++)for(int col=0;col<w;col++)pixels[row*w+col]=i.rgb[(sy+row)*i.width+sx+(t==2?w-1-col:col)];draws++;}
 public void drawRGB(int[] p,int off,int stride,int x,int y,int w,int h,boolean a){this.x=x;this.y=y;this.w=w;this.h=h;pixels=new int[w*h];for(int row=0;row<h;row++)System.arraycopy(p,off+row*stride,pixels,row*w,w);draws++;}
}
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
 Image combatRoomArt(String n){reads++;return new Image();}void part(Image i,int x,int y,int w,int h,int px,int py,boolean flip){if(i!=null){draws++;lastImage=i;lastX=px;lastY=py;}}void img(Image i,int x,int y){}void pcText(String a,int x,int y){}void pcCenter(String a,int x,int y){}String pcShort(String a,int n){return a;}void drawItemSelection(int a,int b,int c,int d,boolean f){}void message(String a){}String nextEconomyKey(){return "k";}
 static class Net{int count=2,roomKind;boolean vipCap=true;boolean[] custom={true,true},present={true,true},bots={false,false};int[] actorWing={3,4,-1,-1,-1,-1},actorVip={9500,9500,0,0,0,0};String pending="",operation="";void request(String a){}}
 static class Bag{void list(){}}static class Shop{Image card,money;int[] box(int i){return new int[4];}}static class Tip{void goods(String a,String b,String c,int[] d){}}
}
class Battle {int unitCount=2;Unit[] units={new Unit(),new Unit()};static class Unit{int npc,slope,face=1,x,y;}static int clamp(int x,int l,int h){return Math.max(l,Math.min(h,x));}static int cos(int a){return (int)(Math.cos(a*Math.PI/180)*1024);}static int sin(int a){return (int)(Math.sin(a*Math.PI/180)*1024);}}
class NativeUi{static Image fit(Image i,int a,int b,int c){return i;}}
public class WingCacheCheck {
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  int scale=Integer.parseInt(args[0]),checks=0;WingPackedArt.Work work=new WingPackedArt.Work(scale);
  for(int wing=0;wing<6;wing++)for(int a=0;a<WingBalancedArt.count(wing);a++){
   WingPackedArt compact=WingPackedArt.load(wing,a,Integer.MAX_VALUE),nativeArt=WingPackedArt.load(wing,a,Integer.MAX_VALUE);
   ok(compact!=null&&nativeArt!=null,"asset present");nativeArt.promote(work);ok(nativeArt.nativeAtlas!=null&&nativeArt.data==null,"native promotion");
   javax.microedition.lcdui.Graphics g1=new javax.microedition.lcdui.Graphics(),g2=new javax.microedition.lcdui.Graphics();
   for(int frame=0;frame<19;frame++)for(int layer=0;layer<2;layer++)for(int face=0;face<2;face++){
    compact.draw(g1,frame,layer,face!=0,200,200,work);nativeArt.draw(g2,frame,layer,face!=0,200,200,work);
    ok(g1.x==g2.x&&g1.y==g2.y&&g1.w==g2.w&&g1.h==g2.h&&java.util.Arrays.equals(g1.pixels,g2.pixels),"native/compact exact pixels");checks++;
   }
  }
  for(int fallback=0;fallback<2;fallback++)for(int wing=0;wing<6;wing++){
   Image.failNative=fallback!=0;PhoneCanvas c=new PhoneCanvas();c.s=scale;c.net.actorWing[0]=wing;c.net.actorWing[1]=-1;VipSystem v=new VipSystem(c);v.prepareBattle();int reads=c.reads;
   for(int face=-1;face<=1;face+=2)for(int anim=0;anim<22;anim++)for(int layer=0;layer<2;layer++)for(int tick=0;tick<38;tick+=2){
    c.anim=anim;c.pose=anim>=10;c.game.units[0].face=face;c.game.units[0].slope=0;c.frame=tick;int pose=WingAttachment.pose(anim,scale==1);
    int wanted=WingAttachment.tilt(wing,pose,face,0),a=WingBalancedArt.select(wing,wanted);WingPackedArt art=v.packedWings[wing][a];
    ok(WingBalancedArt.angle(wing,a)==wanted&&art!=null,"exact configured angle");ok((art.nativeAtlas==null)==(fallback!=0),"cache tier");int p=(layer*19+tick/2)*8,w=art.meta[p+2],rx=art.meta[p+4],ry=art.meta[p+5];int draws=c.g.draws;
    v.battleWing(0,layer==1);ok(c.g.draws==draws+1,"draw");ok(c.g.x==WingAttachment.jointX(wing,pose,face,0)*scale+(face<0?-rx-w:rx)&&c.g.y==WingAttachment.jointY(wing,pose,face,0)*scale+ry,"anchor");checks++;
   }
   ok(c.reads==reads,"no asset requests in paint");v.release();ok(v.wingWork==null,"work released");for(int a=0;a<15;a++)ok(v.packedWings[wing][a]==null,"released");
  }
  System.out.println("PASS scale "+scale+": "+checks+" draws; all 19 frames, exact angles, native/compact identical, OOM fallback");
 }
}
JAVA
javac -encoding UTF-8 -d "$work" "$work"/*.java "$work"/javax/microedition/lcdui/*.java "$work"/javax/microedition/lcdui/game/*.java
for scale in 1 2; do java -Xmx128m -cp "$work:$(pwd)/assets$scale" WingCacheCheck "$scale"; done
