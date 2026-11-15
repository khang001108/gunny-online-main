import java.io.*;import javax.microedition.lcdui.Image;
final class BattleEmotes {
 final PhoneCanvas c;boolean opened;int selected,draft;long poll;final Image[] icons=new Image[6];final int[] values=new int[6];final long[] until=new long[6];
 static final String[] NAMES={"Thích","Không thích","Tức giận","Haha","Khóc","Lợn"};
 BattleEmotes(PhoneCanvas owner){c=owner;}
 boolean canPoll(){return c.net.emotionsCap&&c.net.inBattle&&c.net.phase==1&&c.mode==1&&!c.game.over&&!c.net.departure.paused;}
 void reset(){opened=false;poll=0;for(int i=0;i<6;i++)until[i]=0;}
 Image icon(int id){if(icons[id]==null)icons[id]=c.combatRoomArt("emotion722-"+id);return icons[id];}
 void toggle(){if(c.mode!=1||c.game.over)return;opened=!opened;c.held=c.fireKey=0;c.game.charging=false;}
 void send(){if(c.onlineBattle()){if(!c.net.emotionsCap){c.message("Cập nhật server để gửi cảm xúc");return;}if(c.net.busy||c.net.pending.length()>0)return;draft=selected;c.net.request("battleemote");}else{int actor=c.isLocalTurn()?c.game.active:0;values[actor]=selected;until[actor]=System.currentTimeMillis()+3000;}opened=false;}
 void read(DataInputStream in)throws IOException{for(int i=0;i<6;i++){int id=in.readUnsignedByte(),remaining=in.readUnsignedShort();if(id<6){values[i]=id;until[i]=System.currentTimeMillis()+remaining;}else until[i]=0;}}
 void draw(){if(!opened)return;c.panel(79,48,162,113);c.pcCenter("Cảm xúc (@)",160,56);for(int i=0;i<6;i++){int x=89+i%3*48,y=77+i/3*39;c.img(icon(i),x+12,y);c.pcCenter(NAMES[i],x+22,y+22);if(i==selected)c.drawSelectionGlow(x,y-2,45,37);}}
 void key(int key,int dir,boolean ok){if(key=='@'||key==-7){opened=false;return;}if(dir!=0)selected=(selected+(dir==1?5:dir==2?1:3))%6;else if(ok||key==-6)send();}
 void pointer(int x,int y){if(x<79||x>=241||y<48||y>=161){opened=false;return;}if(y>=75){int col=(x-89)/48,row=(y-77)/39;if(col>=0&&col<3&&row>=0&&row<2){selected=row*3+col;send();}}}
}
