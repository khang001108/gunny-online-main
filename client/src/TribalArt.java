import javax.microedition.lcdui.Image;
/** Native labeled Flash poses baked for MIDP; caches only the current encounter. */
final class TribalArt {
 final PhoneCanvas c;final Image[] sprites=new Image[16],headers=new Image[16];
 Image cover,small,wing,missiles,healthFrame,healthBar,loadingBackground;int coverStage,cachedStage;
 TribalArt(PhoneCanvas canvas){c=canvas;}
 void release(){for(int i=0;i<16;i++){sprites[i]=headers[i]=null;}cover=small=wing=missiles=healthFrame=healthBar=loadingBackground=null;coverStage=cachedStage=0;}
 Image atlas(int kind){if(sprites[kind]==null)sprites[kind]=c.combatRoomArt("tribal797-npc-"+kind);return sprites[kind];}
 int size(int kind){return kind>=14?96:kind==8||kind==9?72:56;}
 Image smallCover(){if(small==null)small=c.combatRoomArt("tribal797-banner");return small;}
 Image cover(int legacy){int number=legacy>=3?3:legacy;if(cover==null||coverStage!=number){coverStage=number;cover=c.combatRoomArt("tribal797-cover-"+number);}return cover;}
 void header(int kind){if(headers[kind]==null)headers[kind]=c.combatRoomArt("tribal797-portrait-"+kind);c.img(headers[kind],0,10);}
 void draw(int actor){
  Battle b=c.game;Battle.Unit u=b.units[actor];int kind=u.npc;
  if(kind<4||kind>15||u.maxHp==0||u.hp<=0&&u.flashTicks==0&&b.tribalState!=TribalRules.DEAD)return;
  if(cachedStage!=b.dungeonStage){for(int i=0;i<16;i++)sprites[i]=headers[i]=null;cachedStage=b.dungeonStage;}
  if(kind==TribalRules.RAGE){sprites[TribalRules.SULAN]=null;headers[TribalRules.SULAN]=null;}
  Image art=atlas(kind);if(art==null)return;int cell=size(kind),row=0,tick=c.frame/3%8;boolean acting=actor==b.active||b.dungeonStage==3&&u.npc==TribalRules.SPEAR&&b.units[b.active].npc==TribalRules.SPEAR;
  if(u.hp<=0){row=6;tick=Math.min(7,(30-Math.min(30,u.flashTicks))*8/30);}
  else if(kind==TribalRules.RAGE&&b.tribalState==TribalRules.TRANSFORM){row=7;tick=Math.min(7,(75-b.tribalFx)*8/75);}
  else if(u.flashTicks>0&&u.lastBuff==14){row=7;tick=Math.min(7,(30-Math.min(30,u.flashTicks))*8/30);}
  else if(u.flashTicks>0&&u.lastBuff!=13){row=5;tick=Math.min(7,(36-Math.min(36,u.flashTicks))*8/36);}
  else if(acting&&b.tribalState==TribalRules.MOVE){row=1;tick=u.walk/3%8;}
  else if(acting&&(b.tribalState==TribalRules.SUMMON||b.tribalState==TribalRules.SPECIAL||u.attackTicks>0)){
   row=b.tribalState==TribalRules.SUMMON?3:b.tribalState==TribalRules.SPECIAL?4:2;
   tick=u.attackTicks>0?Math.min(7,(36-u.attackTicks)*8/36):u.walk/4%8;
  }
  int x=u.x-c.cameraX,y=u.y-c.cameraY;
  c.part(art,tick*cell,row*cell,cell,cell,x-cell/2,y-cell+5,u.face>0);
  if(u.hp<=0)return;
  c.rect(x-13,y+2,26,3,0x382b20);c.rect(x-12,y+3,24*u.hp/Math.max(1,u.maxHp),1,u.team==0?0x57cb6c:0xe85043);
  c.pcCenter(TribalRules.name(kind),x,y+7);
  if(kind==TribalRules.BOMB)c.pcCenter("Nổ: "+Math.max(1,2-u.power),x,y-39);
 }
 void projectile(Battle.Ball ball){
  if(missiles==null)missiles=c.combatRoomArt("tribal797-projectiles");
  int row=ball.weapon-TribalRules.SPEAR_SHOT;
  int frame=ball.weapon==TribalRules.FIRE_SHOT?ball.age/6%8:c.flightDirection(ball.vx,ball.vy)/2;
  c.part(missiles,frame*32,row*32,32,32,ball.x/256-c.cameraX-16,ball.y/256-c.cameraY-16,false);
 }
 void trail(int x,int y,int weapon,int age){if(weapon==TribalRules.SPEAR_SHOT)return;int r=age<5?2:1;c.oval(x-r,y-r,r*2,r*2,weapon==TribalRules.FIRE_SHOT?0xffbd4d:0x9ce9f7);}
 void impact(Battle.Blast blast){int frame=Math.min(11,blast.time*12/25),weapon=blast.weapon==TribalRules.FIRE_SHOT?0:5;c.part(c.impactArt(weapon),frame%4*96,frame/4*96,96,96,blast.x-c.cameraX-48,blast.y-c.cameraY-48,false);}
 void boss(int actor,int y){
  Battle.Unit u=c.game.units[actor];if(healthFrame==null){healthFrame=c.combatRoomArt("npc780-boss-frame");healthBar=c.combatRoomArt("npc780-boss-hp");}
  int x=171,percent=Math.max(0,u.hp)*100/Math.max(1,u.maxHp);c.img(healthFrame,x,y);
  int cx=c.g.getClipX(),cy=c.g.getClipY(),cw=c.g.getClipWidth(),ch=c.g.getClipHeight();
  c.clip(x+29,y+10,Math.max(0,46*percent/100),12);c.img(healthBar,x+23,y+9);c.g.setClip(cx,cy,cw,ch);
  if(headers[u.npc]==null)headers[u.npc]=c.combatRoomArt("tribal797-portrait-"+u.npc);
  c.clip(x+1,y+1,23,24);c.img(headers[u.npc],x-9,y-7);c.g.setClip(cx,cy,cw,ch);
  c.pcCenter(TribalRules.name(u.npc),x+46,y+2);c.pcCenter(percent+"%",x+50,y+14);
 }
 void hud(){
  Battle b=c.game;if(!TribalRules.active(b))return;
  c.pcText("Tà Thần "+DungeonRewards.tribalNumber(b.dungeonStage)+(b.dungeonStage==3?" • "+(20-b.tribalGoal)+"/20":""),69,33);
  if(TribalRules.twins(b)){boss(1,0);boss(3,25);}else if(b.dungeonStage==6)boss(1,0);
  if(TribalRules.alive(b,TribalRules.CHAIN))c.pcText("Xiềng Xích: khóa góc",5,72);
  if(TribalRules.alive(b,TribalRules.FATIGUE))c.pcText("Mệt Mỏi: -100 TL",5,84);
  if(b.tribalBeanSaved)c.pcText("Đậu Hà Lan: Phủ Sức Mạnh",67,62);
 }
 String[] lines(String text){String[] tmp=new String[5];int count=0,start=0;while(start<text.length()&&count<5){int end=start,last=start;while(end<text.length()){int next=text.indexOf(' ',end);if(next<0)next=text.length();if(c.pcWidth(text.substring(start,next))>170&&last>start)break;last=next;if(next==text.length())break;end=next+1;}if(last<=start)last=Math.min(text.length(),start+24);tmp[count++]=text.substring(start,last);start=last+1;}String[] out=new String[count];for(int i=0;i<count;i++)out[i]=tmp[i];return out;}
 void overlay(){
  Battle b=c.game;if(!TribalRules.active(b))return;
  if(b.tribalState==TribalRules.OPENING&&b.tribalFx>65){
   c.panel(57,81,206,78);c.pcCenter("Điều kiện thắng",160,87);
   c.pcCenter(b.dungeonStage==3?"Hạ 20 kẻ địch":TribalRules.twins(b)?"Hạ hai anh em Bulan":"Đánh bại hai dạng Sulan",160,103);
   c.pcCenter("Thua khi cả đội bị đánh bại",160,120);c.pcCenter("Giới hạn 200 lượt",160,135);return;
  }
  if(b.tribalState==TribalRules.TRANSFORM&&b.tribalFx>0){c.pcCenter("Sulan thức tỉnh!",160,76);return;}
  if(b.botPhase>0&&(b.tribalFx>0||b.units[b.active].walk<95)){
   int actor=TribalRules.speaker(b);Battle.Unit u=b.units[actor];int x=u.x-c.cameraX,y=u.y-c.cameraY-(u.npc>=14?64:40);
   if(x>=-20&&x<340)c.drawSpeechBubble(Battle.clamp(x,25,295),Math.max(102,y),lines(TribalRules.dialogue(b)),65);
  }
 }
 void wings(int x,int y){if(wing==null)wing=c.combatRoomArt("tribal-wing");if(wing!=null)c.img(wing,x-wing.getWidth()/c.s/2,y-29);}
}
