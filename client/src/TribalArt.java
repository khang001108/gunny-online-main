import javax.microedition.lcdui.Image;
/** Lazy stage-local native sprites. Only the current encounter is cached. */
final class TribalArt {
 final PhoneCanvas c;final Image[] sprites=new Image[16];Image cover,wing,small;int coverStage;
 TribalArt(PhoneCanvas c){this.c=c;}
 void release(){for(int i=0;i<sprites.length;i++)sprites[i]=null;cover=null;coverStage=0;wing=small=null;}
 Image smallCover(){if(small==null)small=NativeUi.fit(cover(1),88*c.s,32*c.s,0);return small;}
 void header(int kind){if(sprites[kind]==null)sprites[kind]=c.combatRoomArt("tribal-npc-"+kind);Image art=sprites[kind];if(art!=null){int size=art.getHeight()/c.s;c.part(art,0,0,size,size,0,9,false);}}
 Image cover(int stage){if(cover==null||coverStage!=stage){coverStage=stage;cover=c.combatRoomArt("tribal-cover-"+stage);}return cover;}
 void draw(int actor){Battle b=c.game;Battle.Unit u=b.units[actor];int kind=u.npc;if(kind<4||kind>15||u.maxHp==0)return;if(sprites[kind]==null)sprites[kind]=c.combatRoomArt("tribal-npc-"+kind);Image atlas=sprites[kind];if(atlas==null)return;int size=atlas.getHeight()/c.s,x=u.x-c.cameraX,y=u.y-c.cameraY;int pose=u.hp<=0?4:u.flashTicks>0?3:u.attackTicks>0?2:u.walk%16>7?1:0;
  c.part(atlas,pose*size,0,size,size,x-size/2,y-size,u.face>0);
  if(u.hp<=0)return;
  if(u.lastBuff==13&&u.flashTicks>0)c.oval(x-18,y-size/2,36,size/2,0x4de7a1);
  c.rect(x-16,y+1,32,4,0x252b2e);c.rect(x-15,y+2,30*u.hp/u.maxHp,2,u.team==0?0x57cb6c:0xe85043);
  c.pcCenter(TribalRules.name(kind),x,y+7);
  if(kind==TribalRules.BOMB){c.oval(x-24,y-4,48,5,c.frame%12<6?0xe93622:0xffe95a);c.pcCenter("Nổ sau "+Math.max(1,2-u.power)+" lượt",x,y-size-12);}
  if(actor==(TribalRules.stage(b)==2?6:b.active)&&b.tribalOutcome<0&&b.tribalClock<45){String speech=TribalRules.dialogue(b);if(speech.length()>0)c.drawSpeechBubble(x,y-size-8,new String[]{speech});}
 }
 void hud(){Battle b=c.game;if(!TribalRules.active(b))return;int stage=TribalRules.stage(b);c.pcText("Ải "+stage+": "+TribalRules.STAGES[stage-1],8,34);
  if(stage>=3){Battle.Unit boss=b.units[1];c.rect(73,48,174,11,0x31231b);c.rect(75,50,170*Math.max(0,boss.hp)/Math.max(1,boss.maxHp),7,0xa9342b);c.pcCenter(TribalRules.name(boss.npc)+" "+boss.hp+"/"+boss.maxHp,160,49);}
  if(stage==3){Battle.Unit other=b.units[3];c.rect(73,61,174,11,0x31231b);c.rect(75,63,170*Math.max(0,other.hp)/Math.max(1,other.maxHp),7,0xb56827);c.pcCenter(TribalRules.name(other.npc)+" "+other.hp+"/"+other.maxHp,160,62);}
  else if(stage==2)c.pcText("Đậu Thần: "+b.units[6].hp+" • Đợt "+b.tribalWave+"/"+b.tribalGoal,65,48);
  if(stage==2&&b.tribalOutcome==0)c.drawSpeechBubble(160,105,new String[]{"Đậu Thần: Cảm ơn các bạn! Đường đã mở!"});
  if(TribalRules.alive(b,TribalRules.CHAIN))c.pcText("Xiềng Xích: khóa góc",7,76);
  if(TribalRules.alive(b,TribalRules.FATIGUE))c.pcText("Mỏi Mệt: -100 thể lực",7,88);
 }
 void wings(int x,int y){if(wing==null)wing=c.combatRoomArt("tribal-wing");if(wing!=null)c.img(wing,x-wing.getWidth()/c.s/2,y-29);}
}
