import java.io.*;import javax.microedition.lcdui.*;
final class Ranking793{
 final PhoneCanvas c;boolean opened;int count,selected;long poll;final int[] id=new int[128],xp=new int[128],weapon=new int[128],damage=new int[128],wins=new int[128];final String[] names=new String[128];Image frame,title,close;final Image[] heads=new Image[128];final int[] sex=new int[128],skin=new int[128],mask=new int[128];final boolean[] custom=new boolean[128];final int[][] look=new int[128][5];final Image[] levels=new Image[128],weapons=new Image[10];
 Ranking793(PhoneCanvas c){this.c=c;}
 void releaseArt(){frame=title=close=null;for(int i=0;i<128;i++)levels[i]=heads[i]=null;for(int i=0;i<10;i++)weapons[i]=null;}
 void open(){if(!c.onlineAccount()){c.net.loginForm(false);return;}opened=true;selected=0;c.net.request("ranking");}
 void read(DataInputStream in)throws IOException{int n=in.readUnsignedShort();if(n>128)throw new IOException("Sai danh sách xếp hạng");for(int i=0;i<n;i++){id[i]=in.readInt();xp[i]=in.readInt();names[i]=in.readUTF();weapon[i]=in.readUnsignedByte();damage[i]=in.readInt();wins[i]=in.readInt();}if(in.available()>0){int portraits=in.readUnsignedShort();if(portraits!=n)throw new IOException("Sai ảnh xếp hạng");for(int i=0;i<n;i++){sex[i]=in.readUnsignedByte();skin[i]=in.readUnsignedByte();custom[i]=in.readBoolean();mask[i]=in.readUnsignedByte();for(int k=0;k<5;k++)look[i][k]=in.readUnsignedByte();if(sex[i]>1||skin[i]>9)throw new IOException("Sai nhân vật");}}for(int i=0;i<n;i++)levels[i]=heads[i]=null;count=n;selected=Math.min(selected,Math.max(0,n-1));poll=System.currentTimeMillis();}
 void draw(){
  if(frame==null)frame=NativeUi.fit(c.combatRoomArt("settings743-popup"),308*c.s,228*c.s,5*c.s);
  if(close==null)close=c.combatRoomArt("bag715-close");
  if(title==null)try{title=c.load("home-label-6");}catch(IOException ignored){}
  c.all();
  c.img(frame,6,0);
  if(title!=null)c.img(title,160-title.getWidth()/c.s/2,8);else c.pcCenter("XẾP HẠNG",160,9);
  c.img(close,285,10);
  final int panelX=14,panelW=292;
  final int headerY=38,headerH=16;
  final int tableY=54,tableBottom=219,tableH=tableBottom-tableY;
  final int firstRowY=56,rowStep=27,rowHeight=26;
  c.rect(panelX,tableY,panelW,tableH,0xfff9e8);
  c.rect(panelX,headerY,panelW,headerH,0x987342);
  c.pcText("#",20,headerY+4);
  c.pcText("Cấp",37,headerY+4);
  c.pcText("Nhân vật",86,headerY+4);
  c.pcText("VK",165,headerY+4);
  c.pcText("Dame",196,headerY+4);
  c.pcText("Thắng",258,headerY+4);
  int first=selected/6*6;
  for(int row=0;row<6&&first+row<count;row++){
   int i=first+row,y=firstRowY+row*rowStep;
   c.rect(16,y,288,rowHeight,0xfff9e8);
   c.pcText(""+(i+1),20,y+7);
   if(levels[i]==null)levels[i]=NativeUi.fit(c.levelIcon(xp[i]),22*c.s,22*c.s,0);
   c.img(levels[i],36,y+2);
   int w=weapon[i];
   if(w>=0&&w<10&&c.weapons[w]!=null){if(weapons[w]==null)weapons[w]=NativeUi.fit(c.weapons[w],22*c.s,22*c.s,0);c.img(weapons[w],164,y+2);}   
   if(heads[i]==null)try{heads[i]=head(i);}catch(IOException ignored){}
   c.img(heads[i],60,y+2);
   c.pcText(c.pcShort(names[i],75),86,y+9);
   c.pcText(""+damage[i],191,y+7);
   c.pcText(""+wins[i],258,y+7);
   if(i==selected){c.line(16,y,304,y,0xe5ad34);c.line(16,y+rowHeight,304,y+rowHeight,0xe5ad34);c.line(16,y,16,y+rowHeight,0xe5ad34);c.line(304,y,304,y+rowHeight,0xe5ad34);} 
  }
  if(count==0)c.pcCenter("Chưa có tài khoản đủ điều kiện",160,136);
 }
 Image head(int i)throws IOException{int[] pixels=new int[80*100*c.s*c.s];if(custom[i]){int[] order={0,3,2,1,4};for(int k=0;k<5;k++){String layer=c.layerName(sex[i],order[k],look[i][order[k]],mask[i],"cutin");if(layer!=null)c.blendCutin(pixels,c.load(layer));}}else c.blendCutin(pixels,c.load("cutin"+sex[i]+"_0_"+skin[i]));int side=22*c.s;int[] crop=new int[side*side];for(int y=0;y<side;y++)for(int x=0;x<side;x++)crop[y*side+x]=pixels[(y*65*c.s/side)*80*c.s+x*80*c.s/side];return Image.createRGBImage(crop,side,side,true);}
 void close(){opened=false;}
 void key(int key,int dir,boolean ok){if(key==-7){close();return;}if(dir!=0)selected=Battle.clamp(selected+(dir==4||dir==1?-1:1),0,Math.max(0,count-1));else if(ok&&count>0){opened=false;
  c.net.social.returnToRanking=true;
  c.net.social.opened=c.net.social.viewing=c.net.social.profileLoading=true;
  c.net.social.profileError="";
  c.net.social.itemInfo=false;
  c.net.social.target=id[selected];
  c.net.request("playerinfo");}}
 void pointer(int x,int y){
  if(x>=285&&x<306&&y>=10&&y<31){close();return;}
  final int firstRowY=56,rowStep=27,tableBottom=219;
  if(y<firstRowY||y>=tableBottom||x<16||x>304)return;
  int row=(y-firstRowY)/rowStep;
  if(row<0||row>=6)return;
  int i=selected/6*6+row;
  if(i<count){if(i==selected)key(0,0,true);else selected=i;}
 }
}
