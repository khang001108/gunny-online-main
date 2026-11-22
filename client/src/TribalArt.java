import javax.microedition.lcdui.Image;
/** Native labeled Flash poses baked for MIDP; caches only the current encounter. */
final class TribalArt {
 final PhoneCanvas c;final Image[] sprites=new Image[16],headers=new Image[16];
 Image cover,small,wing,missiles,healthFrame,healthBar,bulanTitle,bulanShade,sword,sulanTitle,redSpeech,mask;int shadeAlpha=-1;int coverStage,cachedStage;
 TribalArt(PhoneCanvas canvas){c=canvas;}
 void release(){for(int i=0;i<16;i++){sprites[i]=headers[i]=null;}cover=small=wing=missiles=healthFrame=healthBar=bulanTitle=bulanShade=sword=sulanTitle=redSpeech=mask=null;talkLines=null;talkLine=-1;shadeAlpha=-1;coverStage=cachedStage=0;}
 Image atlas(int kind){if(sprites[kind]==null)sprites[kind]=c.combatRoomArt("tribal797-npc-"+kind);return sprites[kind];}
 int size(int kind){return kind>=14?96:kind==8||kind==9?72:56;}
 Image smallCover(){if(small==null)small=c.combatRoomArt("tribal797-banner");return small;}
 Image cover(int legacy){int number=legacy>=3?3:legacy;if(cover==null||coverStage!=number){coverStage=number;cover=c.combatRoomArt("tribal797-cover-"+number);}return cover;}
 void header(int kind){if(headers[kind]==null)headers[kind]=c.combatRoomArt("tribal797-portrait-"+kind);c.img(headers[kind],0,10);}
 void draw(int actor){
  Battle b=c.game;Battle.Unit u=b.units[actor];int kind=u.npc;
  if(SulanRules.active(b)&&actor==1&&b.tribalState==TribalRules.OPENING&&SulanRules.INTRO-b.tribalFx<60)return;
  if(kind<4||kind>15||u.maxHp==0||u.hp<=0&&u.flashTicks==0&&b.tribalState!=TribalRules.DEAD)return;
  if(cachedStage!=b.dungeonStage){for(int i=0;i<16;i++)sprites[i]=headers[i]=null;cachedStage=b.dungeonStage;}
  boolean goldDeath=SulanRules.active(b)&&actor==1&&b.tribalState==TribalRules.TRANSFORM&&SulanRules.TRANSFORM-b.tribalFx<100;
  if(goldDeath)kind=TribalRules.SULAN;
  else if(kind==TribalRules.RAGE){sprites[TribalRules.SULAN]=null;headers[TribalRules.SULAN]=null;}
  Image art=atlas(kind);if(art==null)return;int cell=size(kind),row=0,tick=c.frame/3%8;boolean acting=actor==b.active||(b.dungeonStage==3||SulanRules.active(b))&&u.npc==TribalRules.SPEAR&&b.units[b.active].npc==TribalRules.SPEAR;
  if(SulanRules.active(b)&&kind==TribalRules.BEAN&&(b.active==actor&&b.tribalState==TribalRules.SPECIAL||b.tribalState==TribalRules.TRANSFORM&&SulanRules.TRANSFORM-b.tribalFx>=240)){row=2;tick=b.tribalState==TribalRules.TRANSFORM?Math.min(7,(SulanRules.TRANSFORM-b.tribalFx-240)*8/60):Math.min(7,u.walk*8/65);}
  else if(SulanRules.active(b)&&actor==1&&b.tribalState==TribalRules.DEAD){int age=SulanRules.DEATH-b.tribalFx;row=age<70?0:6;tick=age<70?c.frame/3%8:Math.min(6,(age-70)*7/80);}
  else if(goldDeath){row=6;tick=Math.min(6,(SulanRules.TRANSFORM-b.tribalFx)*7/100);}
  else if(u.hp<=0){row=6;tick=Math.min(7,(30-Math.min(30,u.flashTicks))*8/30);}
  else if(kind==TribalRules.RAGE&&b.tribalState==TribalRules.TRANSFORM){row=7;tick=Math.min(7,Math.max(0,SulanRules.TRANSFORM-b.tribalFx-100)*8/80);}
  else if(u.flashTicks>0&&u.lastBuff==14){row=7;tick=Math.min(7,(30-Math.min(30,u.flashTicks))*8/30);}
  else if(u.flashTicks>0&&u.lastBuff!=13){row=5;tick=Math.min(7,(36-Math.min(36,u.flashTicks))*8/36);}
  else if(acting&&b.tribalState==TribalRules.MOVE||b.dungeonStage==3&&b.tribalState==TribalRules.OPENING&&u.walk>0){row=1;tick=u.walk/3%8;}
  else if(acting&&(b.tribalState==TribalRules.SUMMON||b.tribalState==TribalRules.SPECIAL||u.attackTicks>0)){
   row=b.tribalState==TribalRules.SUMMON?3:b.tribalState==TribalRules.SPECIAL?4:2;
   tick=u.attackTicks>0?Math.min(7,(36-u.attackTicks)*8/36):u.walk/4%8;
  }
  int x=u.x-c.cameraX,y=u.y-c.cameraY;
  if(SulanRules.active(b)&&actor==1)y-=SulanRules.drop(b);
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
 void trail(int x,int y,int weapon,int age,int frame){int id=weapon==TribalRules.FIRE_SHOT?0:8;int size=c.projectileArtSize(id),fade=Math.min(5,age*6/Math.max(1,18));c.part(c.trailArt[id],frame*size,fade*size,size,size,x-size/2,y-size/2,false);}

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
  if(TribalRules.alive(b,TribalRules.FATIGUE))c.pcText("Mệt Mỏi: -"+TribalRules.fatigue(b)+" TL",5,84);
  if(b.tribalBeanSaved&&TribalRules.alive(b,TribalRules.BEAN))c.pcText("Đậu thần: đã dập lửa",67,62);
 }
 String[] lines(String text){String[] tmp=new String[5];int count=0,start=0;while(start<text.length()&&count<5){int end=start,last=start;while(end<text.length()){int next=text.indexOf(' ',end);if(next<0)next=text.length();if(c.pcWidth(text.substring(start,next))>170&&last>start)break;last=next;if(next==text.length())break;end=next+1;}if(last<=start)last=Math.min(text.length(),start+24);tmp[count++]=text.substring(start,last);start=last+1;}String[] out=new String[count];for(int i=0;i<count;i++)out[i]=tmp[i];return out;}
 void overlay(){
  Battle b=c.game;if(!TribalRules.active(b))return;
  if(SulanRules.active(b)){sulanOverlay();return;}
  if(TribalRules.twins(b)&&b.tribalState==TribalRules.OPENING){
   int age=TribalRules.BULAN_INTRO_END-b.tribalFx;
   if(age>=TribalRules.BULAN_TITLE_AT){drawBulanTitle(age-TribalRules.BULAN_TITLE_AT);return;}
   if(age<TribalRules.BULAN_LEFT_AT)return;
  }
  if(!TribalRules.twins(b)&&b.tribalState==TribalRules.OPENING&&b.tribalFx>65){
   c.panel(57,81,206,78);c.pcCenter("Điều kiện thắng",160,87);
   c.pcCenter(b.dungeonStage==3?"Hạ 20 kẻ địch":TribalRules.twins(b)?"Hạ hai anh em Bulan":"Đánh bại hai dạng Sulan",160,103);
   c.pcCenter("Thua khi cả đội bị đánh bại",160,120);c.pcCenter("Giới hạn 200 lượt",160,135);return;
  }
  if(b.tribalState==TribalRules.TRANSFORM&&b.tribalFx>0){c.pcCenter("Sulan thức tỉnh!",160,76);return;}
  if(b.botPhase>0&&(b.tribalFx>0||b.units[b.active].walk<95)){
   int actor=TribalRules.speaker(b);Battle.Unit u=b.units[actor];int x=u.x-c.cameraX,y=u.y-c.cameraY-(u.npc>=14?64:40);
   if(x>=-20&&x<340)c.drawSpeechBubble(Battle.clamp(x,25,295),Battle.clamp(y,65,190),lines(TribalRules.dialogue(b)),65);
  }
 }
 void drawBulanTitle(int age){
  int duration=TribalRules.BULAN_INTRO_END-TribalRules.BULAN_TITLE_AT;
  titleShade(age,duration);
  if(bulanTitle==null)bulanTitle=c.combatRoomArt("tribal-bulan-title");
  int width=age<10?85:age<AntCaveRules.QUEEN_TITLE_REVEAL?160:288;
  c.clip(16,43,width,35);c.img(bulanTitle,16,43);c.all();
 }
 void titleShade(int age,int duration){
  int alpha=56+Math.min(5,Math.max(0,Math.min(age,duration-age)/2))*20;
  if(bulanShade==null||alpha!=shadeAlpha){int[] rgb=new int[320*c.s*8*c.s];for(int i=0;i<rgb.length;i++)rgb[i]=alpha<<24;bulanShade=Image.createRGBImage(rgb,320*c.s,8*c.s,true);shadeAlpha=alpha;}
  for(int y=0;y<240;y+=8)c.img(bulanShade,0,y);
 }
 int talkLine=-1;String[] talkLines;
 void sulanOverlay(){
  Battle b=c.game;int age=SulanRules.INTRO-b.tribalFx;
  if(b.tribalState==TribalRules.OPENING&&age>=SulanRules.TITLE_AT&&age<SulanRules.TITLE_AT+81){int t=age-SulanRules.TITLE_AT;titleShade(t,81);if(sulanTitle==null)sulanTitle=c.combatRoomArt("tribal-sulan-title");c.clip(8,40,t<10?101:t<21?183:304,34);c.img(sulanTitle,8,40);c.all();return;}
  if(b.sulanTalk<=0||b.sulanLine==0)return;
  if(talkLine!=b.sulanLine||talkLines==null){talkLine=b.sulanLine;talkLines=lines(SulanRules.dialogue(talkLine));}
  Battle.Unit u=b.units[b.sulanSpeaker];int x=u.x-c.cameraX,y=u.y-c.cameraY-(u.npc>=14?64:36);
  if(SulanRules.red(b)){if(redSpeech==null)redSpeech=c.combatRoomArt("font-sulan-red");c.speechOverride=redSpeech;}
  try{if(x>=-20&&x<340)c.drawSpeechBubble(Battle.clamp(x,25,295),Battle.clamp(y,65,190),talkLines,65);}finally{c.speechOverride=null;}
 }
 void worldEffects(){
  Battle b=c.game;
  if(SulanRules.active(b)){
   if(b.tribalState==TribalRules.TRANSFORM){int t=SulanRules.TRANSFORM-b.tribalFx;if(t<55){if(mask==null)mask=c.combatRoomArt("tribal-sulan-mask");int x=b.units[1].x-c.cameraX,y=b.units[1].y-c.cameraY-53+t*t/50;c.part(mask,0,0,9,23,x-9-t/2,y,false);c.part(mask,9,0,9,23,x+t/2,y+2,false);}}
   if(missiles==null)missiles=c.combatRoomArt("tribal797-projectiles");
   for(int n=0;n<7;n+=2)if(b.sulanBurn[n]>0&&b.units[n].hp>0)c.part(missiles,(c.frame/4%8)*32,2*32,32,32,b.units[n].x-c.cameraX-16,b.units[n].y-c.cameraY-25,false);
   for(int i=0;i<b.sulanFireCount;i++){int n=(c.frame/3+i*3)%12,x=b.sulanFireX[i]-c.cameraX,y=b.ground.surface(b.sulanFireX[i],5)-c.cameraY;c.clip(x-23,y-34,46,36);c.part(c.impactArt(0),n%4*96,n/4*96,96,96,x-48,y-65,false);c.all();}

  }
  if(!TribalRules.swordVisible(b))return;
  if(sword==null)sword=c.combatRoomArt("tribal-bulan-sword");
  int age=b.units[b.active].walk-75,rise=Math.min(64,(age+1)*8);if(age>24)rise=Math.max(0,(36-age)*64/12);
  for(int i=0;i<6;i+=2)if(b.units[i].hp>0){int x=b.units[i].x-c.cameraX,y=b.units[i].y-c.cameraY;c.clip(x-14,y-64,28,64);c.img(sword,x-12,y-rise);c.all();}
 }
 void wings(int x,int y){if(wing==null)wing=c.combatRoomArt("tribal-wing");if(wing!=null)c.img(wing,x-wing.getWidth()/c.s/2,y-29);}
}
