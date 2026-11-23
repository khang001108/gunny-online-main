import java.io.*;import javax.microedition.lcdui.*;
final class Ranking793{
 final PhoneCanvas c;boolean opened;int count,selected;long poll;final int[] id=new int[128],xp=new int[128],weapon=new int[128],damage=new int[128],wins=new int[128];final String[] names=new String[128];Image frame;final Image[] heads=new Image[128];final int[] sex=new int[128],skin=new int[128],mask=new int[128];final boolean[] custom=new boolean[128];final int[][] look=new int[128][5];final Image[] levels=new Image[128],weapons=new Image[10];
 Ranking793(PhoneCanvas c){this.c=c;}
 void releaseArt(){frame=null;for(int i=0;i<128;i++)levels[i]=heads[i]=null;for(int i=0;i<10;i++)weapons[i]=null;}
 void open(){if(!c.onlineAccount()){c.net.loginForm(false);return;}opened=true;selected=0;c.net.request("ranking");}
 void read(DataInputStream in)throws IOException{int n=in.readUnsignedShort();if(n>128)throw new IOException("Sai danh sách xếp hạng");for(int i=0;i<n;i++){id[i]=in.readInt();xp[i]=in.readInt();names[i]=in.readUTF();weapon[i]=in.readUnsignedByte();damage[i]=in.readInt();wins[i]=in.readInt();}if(in.available()>0){int portraits=in.readUnsignedShort();if(portraits!=n)throw new IOException("Sai ảnh xếp hạng");for(int i=0;i<n;i++){sex[i]=in.readUnsignedByte();skin[i]=in.readUnsignedByte();custom[i]=in.readBoolean();mask[i]=in.readUnsignedByte();for(int k=0;k<5;k++)look[i][k]=in.readUnsignedByte();if(sex[i]>1||skin[i]>9)throw new IOException("Sai nhân vật");}}for(int i=0;i<n;i++)levels[i]=heads[i]=null;count=n;selected=Math.min(selected,Math.max(0,n-1));poll=System.currentTimeMillis();}
 void draw() {
  if (frame == null)
   frame = NativeUi.fit(c.combatRoomArt("settings743-popup"), 308*c.s, 228*c.s, 5*c.s);
  c.all();
  // Move the actual golden/brown backing frame upward by 6 logical pixels.
  // Keep white table and its rows independent of the frame's image position.
  c.img(frame, 6, 0);

  // The wooden panel is part of settings743-popup.png.
  // Cover more of its inner wooden area without touching the outer frame.
  final int tableTop = 22;
  // Leave an 8px inset above the shifted frame's bottom border (0+228).
  final int tableBottom = 220;
  final int headerHeight = 16;
  final int firstRowY = 40;
  final int rowStep = 29;
  final int rowHeight = 28;

  c.pcCenter("XẾP HẠNG ONLINE", 160, 9);
  c.rect(14, tableTop, 292, tableBottom - tableTop, 0xfff9e8);
  c.rect(14, tableTop, 292, headerHeight, 0x987342);
  c.pcText("#", 20, tableTop + 4);
  c.pcText("Cấp", 37, tableTop + 4);
  c.pcText("Nhân vật", 86, tableTop + 4);
  c.pcText("VK", 165, tableTop + 4);
  c.pcText("Dame", 196, tableTop + 4);
  c.pcText("Thắng", 258, tableTop + 4);

  int first = selected / 6 * 6;
  for (int row = 0; row < 6 && first + row < count; row++) {
   int i = first + row, y = firstRowY + row * rowStep;
   c.rect(16, y, 288, rowHeight, 0xfff9e8);
   c.pcText("" + (i+1), 20, y+8);
   if (levels[i] == null)
    levels[i] = NativeUi.fit(c.levelIcon(xp[i]), 22*c.s, 22*c.s, 0);
   c.img(levels[i], 36, y+3);
   int w = weapon[i];
   if (w >= 0 && w < 10 && c.weapons[w] != null) {
    if (weapons[w] == null)
     weapons[w] = NativeUi.fit(c.weapons[w], 22*c.s, 22*c.s, 0);
    c.img(weapons[w], 164, y+3);
   }
   if (heads[i] == null) try { heads[i] = head(i); } catch (IOException ignored) {}
   c.img(heads[i], 60, y+3);
   c.pcText(c.pcShort(names[i], 75), 86, y+10);
   c.pcText("" + damage[i], 191, y+8);
   c.pcText("" + wins[i], 258, y+8);
   if (i == selected) {
    c.line(16, y, 304, y, 0xe5ad34);
    c.line(16, y+rowHeight, 304, y+rowHeight, 0xe5ad34);
    c.line(16, y, 16, y+rowHeight, 0xe5ad34);
    c.line(304, y, 304, y+rowHeight, 0xe5ad34);
   }
  }
  if (count == 0)
   c.pcCenter("Chưa có tài khoản đủ điều kiện", 160, 127);
  else
   c.pcCenter("Lên/xuống • OK: xem • Mềm phải: đóng", 160, 214);
 }
 Image head(int i)throws IOException{int[] pixels=new int[80*100*c.s*c.s];if(custom[i]){int[] order={0,3,2,1,4};for(int k=0;k<5;k++){String layer=c.layerName(sex[i],order[k],look[i][order[k]],mask[i],"cutin");if(layer!=null)c.blendCutin(pixels,c.load(layer));}}else c.blendCutin(pixels,c.load("cutin"+sex[i]+"_0_"+skin[i]));int side=22*c.s;int[] crop=new int[side*side];for(int y=0;y<side;y++)for(int x=0;x<side;x++)crop[y*side+x]=pixels[(y*65*c.s/side)*80*c.s+x*80*c.s/side];return Image.createRGBImage(crop,side,side,true);}
 void key(int key,int dir,boolean ok){if(key==-7){opened=false;return;}if(dir!=0)selected=Battle.clamp(selected+(dir==4||dir==1?-1:1),0,Math.max(0,count-1));else if(ok&&count>0){opened=false;c.net.social.opened=c.net.social.viewing=c.net.social.profileLoading=true;c.net.social.target=id[selected];c.net.request("playerinfo");}}
 void pointer(int x, int y) {
  // Keep input aligned with the six visible rows.
  if (y < 6 || y > 233 || x < 6 || x > 314) {
   opened = false;
   return;
  }
  if (y < 40 || y >= 214) return;
  int row = (y - 40) / 29;
  int i = selected / 6 * 6 + row;
  if (row >= 0 && row < 6 && i < count) {
   if (i == selected) key(0, 0, true);
   else selected = i;
  }
 }
}
