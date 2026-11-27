import javax.microedition.lcdui.Image;
/** Native labeled Flash poses baked for MIDP; caches only the current encounter. */
final class TribalArt {
 // Visual-only alignment for tribal NPCs (logical 320x240 pixels).
 static final int GROUND_DRAW_OFFSET=3;
 final PhoneCanvas c;final Image[] sprites=new Image[16],headers=new Image[16],bossFaces=new Image[16];
 final int[] fireGroundX=new int[9],fireGroundY=new int[9];
 Image playerSpearArt;final Image[] sceneArt=new Image[4];boolean scenePrepared;
 // Eight recently used pose rows, not six fully decoded 8x8 atlases. All eight original frames remain.
 final Image[] poseRows=new Image[8];final int[] poseKeys=new int[8],poseUse=new int[8];
 final int[] retryFrame=new int[128];int poseUseClock;
 Image cover,small,wing,missiles,healthFrame,healthBar,bulanTitle,bulanShade,sword,sulanTitle,redSpeech;int shadeAlpha=-1;int coverStage,cachedStage;
 TribalArt(PhoneCanvas canvas){c=canvas;}
 void resetVisualClock(){for(int i=0;i<9;i++)fireGroundX[i]=-1;}
 int poseClock(){return c.frame&0x7fffffff;}
 void clearPoseRows(){for(int i=0;i<poseRows.length;i++){poseRows[i]=null;poseKeys[i]=-1;poseUse[i]=0;}for(int i=0;i<retryFrame.length;i++)retryFrame[i]=0;poseUseClock=0;}
 void release(){scenePrepared=false;resetVisualClock();clearPoseRows();playerSpearArt=null;for(int i=0;i<sceneArt.length;i++)sceneArt[i]=null;for(int i=0;i<16;i++){sprites[i]=headers[i]=bossFaces[i]=null;}cover=small=wing=missiles=healthFrame=healthBar=bulanTitle=bulanShade=sword=sulanTitle=redSpeech=null;talkLines=null;talkLine=-1;dialogueText=null;dialogueLines=null;shadeAlpha=-1;coverStage=cachedStage=0;}
 void encounter(){if(cachedStage==c.game.dungeonStage)return;if(cachedStage!=0)release();cachedStage=c.game.dungeonStage;}
 boolean rowAtlas(int kind){return SulanRules.active(c.game)&&(kind==4||kind==6||kind==7||kind==12||kind==14||kind==15);}
 Image atlas(int kind){if(sprites[kind]==null)sprites[kind]=safeLoad("tribal797-npc-"+kind);return sprites[kind];}
 Image poseRow(int kind,int row){
  int key=kind*8+row,now=poseClock(),slot=-1,oldest=-1;
  for(int i=0;i<poseRows.length;i++){
   if(poseRows[i]!=null&&poseKeys[i]==key){poseUse[i]=++poseUseClock;return poseRows[i];}
   if(poseRows[i]==null)slot=i;else if(oldest<0||poseUse[i]<poseUse[oldest])oldest=i;
  }
  // Retry a failed decode after a short render-count delay, not an emulator clock delay.
  if(retryFrame[key]!=0&&now>=retryFrame[key]&&now-retryFrame[key]<24)return null;
  if(slot<0)slot=oldest;
  poseRows[slot]=null;poseKeys[slot]=-1;
  Image art=safeLoad("hf40-npc-"+kind+"-row-"+row);
  if(art==null){retryFrame[key]=now==0?1:now;return null;}
  retryFrame[key]=0;poseRows[slot]=art;poseKeys[slot]=key;poseUse[slot]=++poseUseClock;return art;
 }
 int size(int kind){return kind>=14?96:kind==8||kind==9?72:56;}
 Image smallCover(){if(small==null)small=c.combatRoomArt("tribal797-banner");return small;}
 Image cover(int legacy){int number=legacy>=3?3:legacy;if(cover==null||coverStage!=number){coverStage=number;cover=c.combatRoomArt("tribal797-cover-"+number);}return cover;}
 void preloadBattle(){
  encounter();
  // Same encounter-local cache and present-NPC preload for every Tribal stage.
  for(int i=0;i<c.game.unitCount;i++){
   Battle.Unit u=c.game.units[i];int k=u.npc;
   if(k>=4&&k<=15&&u.hp>0){if(rowAtlas(k))poseRow(k,0);else atlas(k);if(headers[k]==null)headers[k]=safeLoad("tribal797-portrait-"+k);}
  }
  if(missiles==null)missiles=safeLoad("tribal797-projectiles");
  if(healthFrame==null)healthFrame=safeLoad("npc780-boss-frame");if(healthBar==null)healthBar=safeLoad("npc780-boss-hp");
  if(SulanRules.active(c.game))preloadScenes();
 }
 Image safeLoad(String name){try{return c.combatRoomArt(name);}catch(OutOfMemoryError low){return null;}}
 void preloadScenes(){
  if(scenePrepared)return;scenePrepared=true;
  sceneArt[0]=safeLoad("hf24-gold-death");sceneArt[1]=safeLoad("hf24-red-rebirth");
  sceneArt[2]=safeLoad("hf24-bean-grow");sceneArt[3]=safeLoad("hf24-bean-talk");
  sulanTitle=safeLoad("tribal-sulan-title");redSpeech=safeLoad("font-sulan-red");
  if(c.speechFont==null)c.speechFont=safeLoad("font-dungeon-speech");
  try{c.impactArt(0);c.impactArt(5);}catch(OutOfMemoryError low){}
 }
 boolean drawScene(int kind,int row,int tick,int cell,int x,int y,boolean flip){
  int i=kind==TribalRules.SULAN&&row==6?0:kind==TribalRules.RAGE&&row==7?1:kind==TribalRules.BEAN&&row==7?2:kind==TribalRules.BEAN&&row==2?3:-1;
  if(i<0||sceneArt[i]==null)return false;
  int sx=i==0?20:i==1?10:i==2?12:0,sy=i==0?26:i==1?20:i==2?14:0;
  int w=i==0?58:i==1?72:i==2?33:56,h=i==0?68:i==1?76:i==2?42:56;
  c.part(sceneArt[i],tick%4*w,tick/4*h,w,h,x-cell/2+(flip?cell-sx-w:sx),y-cell+5+sy,flip);return true;
 }
 void header(int kind){if(headers[kind]==null)headers[kind]=safeLoad("tribal797-portrait-"+kind);c.img(headers[kind],0,10);}
 void draw(int actor){
  Battle b=c.game;Battle.Unit u=b.units[actor];int kind=u.npc;
  if(SulanRules.active(b)&&actor==1&&b.tribalState==TribalRules.OPENING&&SulanRules.INTRO-b.tribalFx<60)return;
  if(kind<4||kind>15||u.maxHp==0||u.hp<=0&&u.flashTicks==0&&b.tribalState!=TribalRules.DEAD&&!(actor==1&&b.tribalState==TribalRules.TRANSFORM))return;
  if(u.x-c.cameraX< -96||u.x-c.cameraX>416||u.y-c.cameraY< -140||u.y-c.cameraY>380)return;
  encounter();
  boolean goldDeath=SulanRules.active(b)&&actor==1&&b.tribalState==TribalRules.TRANSFORM&&SulanRules.TRANSFORM-b.tribalFx<SulanRules.GOLD_END;
  if(goldDeath)kind=TribalRules.SULAN;
  
  int cell=size(kind),row=0,tick=poseClock()/3%8;boolean acting=actor==b.active||(b.dungeonStage==3||SulanRules.active(b))&&u.npc==TribalRules.SPEAR&&b.units[b.active].npc==TribalRules.SPEAR;
  if(SulanRules.active(b)&&kind==TribalRules.BEAN&&(b.active==actor&&b.tribalState==TribalRules.SPECIAL||b.tribalState==TribalRules.TRANSFORM&&SulanRules.TRANSFORM-b.tribalFx>=SulanRules.BEAN_TALK)){row=2;tick=b.tribalState==TribalRules.TRANSFORM?Math.min(7,(SulanRules.TRANSFORM-b.tribalFx-SulanRules.BEAN_TALK)*8/40):Math.min(7,u.walk*8/65);}
  else if(SulanRules.active(b)&&kind==TribalRules.BEAN&&b.tribalState==TribalRules.TRANSFORM&&SulanRules.TRANSFORM-b.tribalFx>=SulanRules.BEAN_AT&&SulanRules.TRANSFORM-b.tribalFx<SulanRules.BEAN_AT+30){row=7;tick=(SulanRules.TRANSFORM-b.tribalFx-SulanRules.BEAN_AT)*8/30;}
  else if(SulanRules.active(b)&&actor==1&&b.tribalState==TribalRules.DEAD){int age=SulanRules.DEATH-b.tribalFx;row=age<70?0:6;tick=age<70?poseClock()/3%8:Math.min(6,(age-70)*7/80);}
  else if(goldDeath){row=6;tick=Math.min(7,(SulanRules.TRANSFORM-b.tribalFx)*8/SulanRules.GOLD_END);}
  else if(u.hp<=0){row=6;tick=Math.min(7,(30-Math.min(30,u.flashTicks))*8/30);}
  else if(kind==TribalRules.RAGE&&b.tribalState==TribalRules.TRANSFORM){int age=SulanRules.TRANSFORM-b.tribalFx;row=age<SulanRules.RED_END?7:0;tick=age<SulanRules.RED_END?Math.min(7,(age-SulanRules.GOLD_END)*8/(SulanRules.RED_END-SulanRules.GOLD_END)):poseClock()/3%8;}
  else if(kind==TribalRules.SULAN&&b.tribalState==TribalRules.OPENING&&b.sulanLine==SulanRules.ROAR&&SulanRules.INTRO-b.tribalFx<SulanRules.ROAR_CAMERA){row=3;tick=Math.min(7,Math.max(0,SulanRules.INTRO-b.tribalFx-SulanRules.ROAR_AT)*8/36);}
  else if(u.flashTicks>0&&u.lastBuff==14){row=7;tick=Math.min(7,(30-Math.min(30,u.flashTicks))*8/30);}
  else if(u.flashTicks>0&&u.lastBuff!=13){row=5;tick=Math.min(7,(36-Math.min(36,u.flashTicks))*8/36);}
  else if(acting&&b.tribalState==TribalRules.MOVE||b.dungeonStage==3&&b.tribalState==TribalRules.OPENING&&u.walk>0){row=1;tick=u.walk/3%8;}
  else if(acting&&(b.tribalState==TribalRules.SUMMON||b.tribalState==TribalRules.SPECIAL||u.attackTicks>0)){
   row=b.tribalState==TribalRules.SUMMON?3:b.tribalState==TribalRules.SPECIAL?4:2;
   tick=u.attackTicks>0?Math.min(7,(36-u.attackTicks)*8/36):SulanRules.active(b)?poseClock()/4%8:u.walk/4%8;
  }
  int x=u.x-c.cameraX,y=u.y-c.cameraY+GROUND_DRAW_OFFSET;
  if(SulanRules.active(b)&&actor==1)y-=SulanRules.drop(b);
  tick=Battle.clamp(tick,0,7);row=Battle.clamp(row,0,7);
  if(drawScene(kind,row,tick,cell,x,y,u.face>0)){}
  else {
   boolean split=rowAtlas(kind);Image art=split?poseRow(kind,row):atlas(kind);int drawRow=split?0:row;
   // In a transient allocation failure use the moving idle row, never permanently lock a sprite.
   if(art==null&&split&&row!=0){art=poseRow(kind,0);tick=poseClock()/3%8;}
   if(art!=null)c.part(art,tick*cell,drawRow*cell,cell,cell,x-cell/2,y-cell+5,u.face>0);
   else c.rect(x-7,y-22,14,22,u.team==0?0x50ba68:0xbd6739);
  }
  if(u.hp<=0)return;
  c.rect(x-13,y+2,26,3,0x382b20);c.rect(x-12,y+3,24*u.hp/Math.max(1,u.maxHp),1,u.team==0?0x57cb6c:0xe85043);
  c.pcCenter(TribalRules.name(kind),x,y+7);
  if(kind==TribalRules.BOMB)c.pcCenter("Nổ: "+Math.max(1,2-u.power),x,y-39);
 }
 void preloadPlayerSpear(){if(missiles==null)missiles=safeLoad("tribal797-projectiles");if(playerSpearArt==null)playerSpearArt=safeLoad("hf20-player-spear32");}
 boolean drawPlayerSpear(int dir,int x,int y){
  if(playerSpearArt!=null){c.part(playerSpearArt,(dir%8)*32,(dir/8)*32,32,32,x-16,y-16,false);return true;}
  if(missiles==null)return false;
  c.part(missiles,((dir+2)/4%8)*32,(TribalRules.SPIRIT_SHOT-TribalRules.SPEAR_SHOT)*32,32,32,x-16,y-16,false);return true;
 }
 boolean playerSpear(Battle.Ball ball){return drawPlayerSpear(SpearVisual.direction(ball.vx,ball.vy),ball.x/256-c.cameraX,ball.y/256-c.cameraY);}
 boolean heldPlayerSpear(int face,int pose,int aim,int x,int y){int dir=SpearVisual.heldDirection(face,pose,aim);return drawPlayerSpear(dir,x+SpearVisual.gripX(dir),y+SpearVisual.gripY(dir));}
 void projectile(Battle.Ball ball){
  if(missiles==null)missiles=safeLoad("tribal797-projectiles");
  int row=ball.weapon-TribalRules.SPEAR_SHOT;
  int frame=ball.weapon==TribalRules.FIRE_SHOT?ball.age/6%8:c.flightDirection(ball.vx,ball.vy)/2;
  c.part(missiles,frame*32,row*32,32,32,ball.x/256-c.cameraX-16,ball.y/256-c.cameraY-16,false);
 }
 void trail(int x,int y,int weapon,int age,int frame){int id=weapon==TribalRules.FIRE_SHOT?0:8;int size=c.projectileArtSize(id),fade=Math.min(5,age*6/Math.max(1,18));c.part(c.trailArt[id],frame*size,fade*size,size,size,x-size/2,y-size/2,false);}

 void impact(Battle.Blast blast){int frame=Math.min(11,blast.time*12/25),weapon=blast.weapon==TribalRules.FIRE_SHOT?0:5;Image art=c.impactArt[weapon];if(art==null)try{art=c.impactArt(weapon);}catch(OutOfMemoryError low){}if(art==null){c.oval(blast.x-c.cameraX-5,blast.y-c.cameraY-5,10,10,0xff9844);return;}c.part(art,frame%4*96,frame/4*96,96,96,blast.x-c.cameraX-48,blast.y-c.cameraY-48,false);}
 void boss(int actor,int y){
  Battle.Unit u=c.game.units[actor];if(u.npc<4||u.npc>15||u.maxHp<=0)return;if(healthFrame==null)healthFrame=safeLoad("npc780-boss-frame");if(healthBar==null)healthBar=safeLoad("npc780-boss-hp");
  int x=171,percent=Math.max(0,u.hp)*100/Math.max(1,u.maxHp);c.img(healthFrame,x,y);
  int cx=c.g.getClipX(),cy=c.g.getClipY(),cw=c.g.getClipWidth(),ch=c.g.getClipHeight();
  c.clip(x+29,y+10,Math.max(0,46*percent/100),12);c.img(healthBar,x+23,y+9);c.g.setClip(cx,cy,cw,ch);
  if(bossFaces[u.npc]==null){Image face=headers[u.npc];if(face==null)face=safeLoad("tribal797-portrait-"+u.npc);if(face!=null)try{bossFaces[u.npc]=NativeUi.fit(face,22*c.s,23*c.s,0);}catch(OutOfMemoryError low){}}
  c.clip(x+1,y+1,23,24);c.img(bossFaces[u.npc],x+3,y+3);c.g.setClip(cx,cy,cw,ch);
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
 String dialogueText;String[] dialogueLines;
 String[] cachedLines(String text){if(dialogueLines==null||!text.equals(dialogueText)){dialogueText=text;dialogueLines=lines(text);}return dialogueLines;}
 String[] lines(String text){String[] tmp=new String[5];int count=0,start=0;while(start<text.length()&&count<5){int end=start,last=start;while(end<text.length()){int next=text.indexOf(' ',end);if(next<0)next=text.length();if(c.pcWidth(text.substring(start,next))>170&&last>start)break;last=next;if(next==text.length())break;end=next+1;}if(last<=start)last=Math.min(text.length(),start+24);tmp[count++]=text.substring(start,last);start=last+1;}String[] out=new String[count];for(int i=0;i<count;i++)out[i]=tmp[i];return out;}
 void overlay(){
  Battle b=c.game;if(!TribalRules.active(b))return;
  if(SulanRules.active(b)){sulanOverlay();return;}
  if(TribalRules.twins(b)&&b.tribalState==TribalRules.OPENING){
   int age=TribalRules.BULAN_INTRO_END-b.tribalFx;
   if(age>=TribalRules.BULAN_TITLE_AT){drawBulanTitle(age-TribalRules.BULAN_TITLE_AT);return;}
   if(age<TribalRules.BULAN_LEFT_AT)return;
  }
  if(b.tribalState==TribalRules.TRANSFORM&&b.tribalFx>0){c.pcCenter("Sulan thức tỉnh!",160,76);return;}
  if(b.botPhase>0&&(b.tribalFx>0||b.units[b.active].walk<95)){
   int actor=TribalRules.speaker(b);Battle.Unit u=b.units[actor];int x=u.x-c.cameraX,y=u.y-c.cameraY-(u.npc>=14?64:40);
   if(x>=-20&&x<340)c.drawSpeechBubble(Battle.clamp(x,25,295),Battle.clamp(y,65,190),cachedLines(TribalRules.dialogue(b)),65);
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
  int step=Math.min(5,Math.max(0,Math.min(age,duration-age)/2));
  int alpha=56+step*20;
  if(bulanShade==null||alpha!=shadeAlpha)try{int[] rgb=new int[320*c.s*8*c.s];for(int i=0;i<rgb.length;i++)rgb[i]=alpha<<24;bulanShade=Image.createRGBImage(rgb,320*c.s,8*c.s,true);shadeAlpha=alpha;}catch(OutOfMemoryError low){return;}
  for(int y=0;y<240;y+=8)c.img(bulanShade,0,y);
 }
 int talkLine=-1;String[] talkLines;
 void sulanOverlay(){
  Battle b=c.game;int age=SulanRules.INTRO-b.tribalFx;
  if(b.tribalState==TribalRules.OPENING&&age>=SulanRules.TITLE_AT&&age<SulanRules.TITLE_AT+81){int t=age-SulanRules.TITLE_AT;titleShade(t,81);if(sulanTitle==null)sulanTitle=safeLoad("tribal-sulan-title");c.clip(8,40,t<10?101:t<21?183:304,34);c.img(sulanTitle,8,40);c.all();return;}
  if(b.sulanTalk<=0||b.sulanLine==0)return;
  if(talkLine!=b.sulanLine||talkLines==null){talkLine=b.sulanLine;talkLines=lines(SulanRules.dialogue(talkLine));}
  int actor=b.sulanSpeaker;Battle.Unit u=b.units[actor];
  if(u.hp<=0&&u.npc==TribalRules.BEAN)return;
  int x=u.x-c.cameraX,y=u.y-c.cameraY-(u.npc>=14?64:u.npc==TribalRules.BEAN?45:36);
  // Anchor the tail to the speaker, never the active player's HUD. If the head
  // is too near the HUD, put the bubble below it with an upward tail.
  int height=talkLines.length*11+10,feet=u.y-c.cameraY+6;
  boolean below=y<65+height+9&&feet+height+9<=240;
  int tip=below?feet:y,top=below||y>=65+height+9?65:4;
  if(SulanRules.red(b)){if(redSpeech==null)redSpeech=safeLoad("font-sulan-red");c.speechOverride=redSpeech;}
  try{if(c.speechFont==null)c.speechFont=safeLoad("font-dungeon-speech");if(c.speechFont==null){if(talkLines.length>0)c.pcCenter(talkLines[0],160,75);return;}if(x>=0&&x<320&&y>=0&&y<240)c.drawSpeechBubble(x,tip,talkLines,top,below);}finally{c.speechOverride=null;}
 }
 void worldEffects(){
  Battle b=c.game;
  if(SulanRules.active(b)){
   if(missiles==null)missiles=safeLoad("tribal797-projectiles");
   for(int n=0;n<7;n+=2)if(b.sulanBurn[n]>0&&b.units[n].hp>0)c.part(missiles,(poseClock()/4%8)*32,2*32,32,32,b.units[n].x-c.cameraX-16,b.units[n].y-c.cameraY-25,false);
   for(int i=0;i<b.sulanFireCount;i++){int n=(poseClock()/3+i*3)%12,x=b.sulanFireX[i]-c.cameraX,y;if(fireGroundX[i]!=b.sulanFireX[i]){fireGroundX[i]=b.sulanFireX[i];fireGroundY[i]=b.ground.surface(fireGroundX[i],5);}y=fireGroundY[i]-c.cameraY;c.clip(x-23,y-34,46,36);if(c.impactArt[0]!=null)c.part(c.impactArt[0],n%4*96,n/4*96,96,96,x-48,y-65,false);else c.part(missiles,(n%8)*32,2*32,32,32,x-16,y-28,false);c.all();}

  }
  if(!TribalRules.swordVisible(b))return;
  if(sword==null)sword=c.combatRoomArt("tribal-bulan-sword");
  int age=b.units[b.active].walk-75,rise=Math.min(64,(age+1)*8);if(age>24)rise=Math.max(0,(36-age)*64/12);
  for(int i=0;i<6;i+=2)if(b.units[i].hp>0){int x=b.units[i].x-c.cameraX,y=b.units[i].y-c.cameraY;c.clip(x-14,y-64,28,64);c.img(sword,x-12,y-rise);c.all();}
 }
 void wings(int x,int y){if(wing==null)wing=c.combatRoomArt("tribal-wing");if(wing!=null)c.img(wing,x-wing.getWidth()/c.s/2,y-29);}
}
