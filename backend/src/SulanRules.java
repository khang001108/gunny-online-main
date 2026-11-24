/** Server-authoritative final Tribal encounter. Shared verbatim with CLDC client.
 * 30 simulation ticks per second; bounded fire fields, no render-time damage.
 */
final class SulanRules {
 static final int INTRO=350,TITLE_AT=170,TRANSFORM=240,DEATH=150;
 static final int GOLD_END=75,RED_END=140,BEAN_AT=155,BEAN_TALK=200,ROAR_AT=255,ROAR_CAMERA=291,ROAR_HIT=315;
 static final int INTRO_LINE=1,ROAR=2,SPEAR=3,HIT_GOLD=4,HIT_GOLD2=5,BORROW=6,REBIRTH=7,BEAN=8,NEAR=9,STORY1=10,STORY2=11,STORY3=12,HIT_RED=13,HIT_RED2=14,HIT_RED3=15,HELL=16,FURY=17,FAREWELL=18,CALL=19,ADD_ATTACK=20;
 static final int[] ADD_SLOTS={3,5,7,8,9,10,11};
 static boolean active(Battle b){return b.dungeonStage==6;}
 static boolean reinforcement(Battle.Unit u){return u.npc==TribalRules.SPEAR||u.npc==TribalRules.ELITE;}
 static boolean npcTurn(Battle b){return active(b)&&b.units[b.active].npc>0&&!reinforcement(b.units[b.active]);}
 static void reset(Battle b){b.sulanLine=b.sulanTalk=b.sulanStory=b.sulanStoryClock=b.sulanFireCount=b.tribalWave=0;for(int i=0;i<b.sulanBurn.length;i++)b.sulanBurn[i]=0;b.sulanSpeaker=1;b.tribalFx=INTRO;b.botPhase=0;}
 static String dialogue(int line){switch(line){
  case INTRO_LINE:return "Dám ngăn nghi lễ của ta, đừng hòng sống sót trở về!";
  case ROAR:return "Tiếng gầm mãnh hổ!";case SPEAR:return "Giáo nhọn lớn!!";
  case HIT_GOLD:return "Ta giận rồi nha!";case HIT_GOLD2:return "Ta né, ta né, ây da... đau quá!";
  case BORROW:return "Thể xác ốm yếu này, đưa ta mượn tạm.";
  case REBIRTH:return "Lửa bất diệt cháy bùng lên đi!";
  case BEAN:return "Lửa bất diệt khó dập tắt.";case NEAR:return "Đi sát tôi, tôi có thể giảm sát thương.";
  case STORY1:return "Phải ngăn chặn sự trùng sinh của tà thần!";
  case STORY2:return "Đánh bại nó có thể thức tỉnh người dân.";
  case STORY3:return "Đừng bỏ hi vọng, tộc trưởng dân làng đang bị giam giữ!";
  case HIT_RED:return "Chọc giận ta à?";case HIT_RED2:return "Dám hỗn xược với thần linh?";
  case HIT_RED3:return "Đây là đòn tấn công của ngươi sao?";
  case HELL:return "Lửa địa ngục!";case FURY:return "Ngọn lửa hung tàn hãy thiêu rụi nơi đây!";
  case FAREWELL:return "Thân thể nhỏ bé này quá sức chịu đựng rồi.....";
  case CALL:return "Chiến sĩ bộ lạc, hạ gục chúng!";
  case 21:return "Vật Tổ HP, hãy cho ta sức mạnh!";
  case ADD_ATTACK:return "Chiến đấu vì tộc trưởng!";
 }return "";}
 static boolean red(Battle b){return b.sulanSpeaker==1&&b.tribalPhase==2&&b.sulanLine!=BORROW;}
 static void say(Battle b,int line,int actor,int ticks){b.sulanLine=line;b.sulanSpeaker=actor;b.sulanTalk=ticks;}
 static void updateFacing(Battle b){
  Battle.Unit boss=b.units[1];if(boss.hp<=0||boss.npc!=TribalRules.SULAN&&boss.npc!=TribalRules.RAGE)return;
  // Keep the shot's facing during a boss attack; each launch aims at its own victim.
  if(b.active==1&&b.tribalFx==0&&b.tribalClock>0)return;
  int victim=b.units[b.active].npc==0&&b.units[b.active].hp>0&&b.units[b.active].team==0?b.active:TribalRules.target(b,1);if(victim!=1&&b.units[victim].x!=boss.x)boss.face=b.units[victim].x<boss.x?-1:1;
 }
 static void callHealTotem(Battle b){
  Battle.Unit u=b.units[1];if(b.units[12].hp<=0)TribalRules.spawn(b,12,TribalRules.HEAL,Battle.clamp(u.x-u.face*35,18,462));
  // One call per red boss turn; only the fifth call heals, then repeat.
  if(u.power%5==0&&b.units[12].npc==TribalRules.HEAL&&b.units[12].hp>0){TribalRules.heal(b,u,u.maxHp*15/100);say(b,21,1,80);}
 }
 static int firstPlayer(Battle b){for(int i=0;i<6;i+=2)if(b.units[i].hp>0)return i;return 0;}
 static int playerCount(Battle b){int n=0;for(int i=0;i<6;i+=2)if(b.units[i].hp>0)n++;return n;}
 static int target(Battle b,int index){for(int i=0;i<6;i+=2)if(b.units[i].hp>0&&index--==0)return i;return firstPlayer(b);}
 static boolean stomping(Battle b){return active(b)&&b.units[b.active].npc==TribalRules.RAGE&&b.tribalState==TribalRules.SPECIAL&&b.tribalClock>=45&&b.tribalClock<=90;}
 static boolean cinematic(Battle b){return active(b)&&b.tribalFx>0&&(b.tribalState==TribalRules.OPENING||b.tribalState==TribalRules.TRANSFORM||b.tribalState==TribalRules.DEAD);}
 static int cameraActor(Battle b){if(stomping(b))return firstPlayer(b);if(b.tribalState==TribalRules.TRANSFORM&&TRANSFORM-b.tribalFx>=BEAN_AT)return 6;if(b.tribalState==TribalRules.OPENING){int t=INTRO-b.tribalFx;if(t<60||t>=ROAR_CAMERA)return firstPlayer(b);}return 1;}
 static int drop(Battle b){int age=INTRO-b.tribalFx;return b.tribalState==TribalRules.OPENING&&age<90?Math.max(0,140-(age-60)*(age-60)*140/900):0;}
 static boolean shake(Battle b){if(active(b)&&b.units[b.active].npc==TribalRules.RAGE&&b.tribalState==TribalRules.SPECIAL&&b.tribalClock>=45&&b.tribalClock<=78)return true;int age=INTRO-b.tribalFx;return b.tribalState==TribalRules.OPENING&&(age>=90&&age<108||age>=ROAR_HIT-10&&age<ROAR_HIT+10)||b.tribalState==TribalRules.DEAD&&DEATH-b.tribalFx>=70&&DEATH-b.tribalFx<90;}
 static void flash(Battle b,int x,int y){Battle.Blast f=new Battle.Blast();f.x=x;f.y=y;f.weapon=TribalRules.FIRE_SHOT;f.radius=20;if(b.blastCount<b.blasts.length)b.blasts[b.blastCount++]=f;b.hitSerial++;}
 static void transform(Battle b){b.tribalWave=0;b.tribalState=TribalRules.TRANSFORM;b.tribalFx=TRANSFORM;b.sulanFireCount=0;b.ballCount=b.bursts=b.settle=0;b.finishPower();b.charging=b.pendingNext=false;say(b,BORROW,1,100);}
 static void effect(Battle b){
  int state=b.tribalState;
  if(state==TribalRules.OPENING){int t=INTRO-b.tribalFx;
   if(t==90){flash(b,b.units[1].x,b.units[1].y);say(b,INTRO_LINE,1,80);}
   if(t==TITLE_AT)b.sulanTalk=0;
   if(t==ROAR_AT){b.units[1].attackTicks=36;say(b,ROAR,1,75);}
   if(t==ROAR_HIT){int victim=firstPlayer(b);flash(b,b.units[victim].x,b.units[victim].y-15);hurt(b,victim,TribalRules.DAMAGE[b.dungeonDifficulty]);b.check();}
  }else if(state==TribalRules.TRANSFORM){int t=TRANSFORM-b.tribalFx;
   if(t==GOLD_END){int x=b.units[1].x;TribalRules.spawn(b,1,TribalRules.RAGE,x);b.tribalPhase=2;b.units[1].flashTicks=0;say(b,REBIRTH,1,65);}
   if(t==BEAN_AT){int x=0;for(int i=0;i<6;i+=2)if(b.units[i].hp>0)x=Math.max(x,b.units[i].x);TribalRules.spawn(b,6,TribalRules.BEAN,Battle.clamp(x+55,28,b.units[1].x-55));b.units[6].maxHp=b.units[6].hp=beanHp(b.dungeonDifficulty);b.units[6].face=b.units[1].x<b.units[6].x?-1:1;say(b,BEAN,6,60);}
   if(t==BEAN_TALK){b.units[6].attackTicks=36;say(b,NEAR,6,90);}
  }else if(state==TribalRules.DEAD){int t=DEATH-b.tribalFx;
   if(t==70)flash(b,b.units[1].x,b.units[1].y-25);
  }
  if(b.over)return;
  if(b.sulanTalk>0)b.sulanTalk--;
  if(--b.tribalFx>0)return;
  if(state==TribalRules.DEAD){TribalRules.check(b);return;}
  b.tribalClock=0;b.botPhase=0;b.next();
 }
 static int nextActor(Battle b,int actor){
  if(b.tribalFx==0&&(b.tribalState==TribalRules.TRANSFORM||b.tribalState==TribalRules.OPENING))return firstPlayer(b);
  if(actor==1&&b.tribalPhase==2&&b.units[6].npc==TribalRules.BEAN&&b.units[6].hp>0)return 6;
  if(actor==1||actor==6){int melee=firstAdd(b,TribalRules.SPEAR,-1);if(melee>=0)return melee;return nextEliteOrPlayer(b,-1);}
  if(b.units[actor].npc==TribalRules.SPEAR)return nextEliteOrPlayer(b,-1);
  if(b.units[actor].npc==TribalRules.ELITE)return nextEliteOrPlayer(b,actor);
  for(int i=actor+2;i<6;i+=2)if(b.units[i].hp>0)return i;
  return 1;
 }
 static int firstAdd(Battle b,int kind,int after){for(int i=0;i<ADD_SLOTS.length;i++){int slot=ADD_SLOTS[i];if(slot>after&&b.units[slot].hp>0&&b.units[slot].npc==kind)return slot;}return -1;}
 static int nextEliteOrPlayer(Battle b,int after){int elite=firstAdd(b,TribalRules.ELITE,after);if(elite>=0)return elite;b.tribalRound++;return firstPlayer(b);}
 static int addCount(Battle b,int kind){int count=0;for(int i=0;i<ADD_SLOTS.length;i++){Battle.Unit u=b.units[ADD_SLOTS[i]];if(u.hp>0&&u.npc==kind)count++;}return count;}
 static int frontSpawn(Battle b,int slot,int rank){
  Battle.Unit boss=b.units[1];int dir=boss.face<0?-1:1,span=dir<0?boss.x-18:Battle.W-18-boss.x;
  if(span<30)return -1;
  for(int pass=0;pass<2;pass++)for(int step=0;step<=span-30;step++){
   int x=boss.x+dir*(30+(rank*22+step)%(span-29));if(b.ground.surface(x,5)>=Battle.H-12)continue;
   boolean blocked=false;for(int n=0;n<b.unitCount;n++)if(n!=slot&&b.units[n].hp>0&&Math.abs(x-b.units[n].x)<18&&(pass==0||b.units[n].team!=boss.team)){blocked=true;break;}
   if(!blocked)return x;
  }return -1;
 }
 /** Keep surviving reinforcements; refill the native bounded roster every second boss turn. */
 static void callAdds(Battle b){
  int troops=3,elites=b.tribalPhase==1?1:2,rank=0;
  for(int type=0;type<2;type++){int kind=type==0?TribalRules.ELITE:TribalRules.SPEAR,missing=(type==0?elites:troops)-addCount(b,kind);
   for(int i=0;i<ADD_SLOTS.length&&missing>0;i++){int slot=ADD_SLOTS[i];if(b.units[slot].hp>0)continue;int x=frontSpawn(b,slot,rank++);if(x<0)continue;TribalRules.spawn(b,slot,kind,x);b.units[slot].face=b.units[1].face;missing--;}
  }
  b.tribalWave=b.units[1].power/2;b.tribalState=TribalRules.SUMMON;b.units[1].attackTicks=36;say(b,CALL,1,95);
 }
 static void hit(Battle b,int actor,int amount){if(amount<=0||actor!=1||b.tribalFx>0||b.sulanTalk>35)return;say(b,b.tribalPhase==1?(b.rand(2)==0?HIT_GOLD:HIT_GOLD2):HIT_RED+b.rand(3),1,85);}
 static int beanHp(int difficulty){return difficulty==0?1800:difficulty==1?1200:700;}
 static boolean protectedByBean(Battle b,int actor){return b.units[6].npc==TribalRules.BEAN&&b.units[6].hp>0&&Math.abs(b.units[actor].x-b.units[6].x)<=75;}
 static int damage(Battle b,Battle.Ball shot,int actor,int amount){
  if(shot.weapon==TribalRules.FIRE_SHOT&&b.units[shot.owner].npc==TribalRules.RAGE&&actor<7)b.sulanBurn[actor]=1;
  if(b.units[actor].npc==TribalRules.BEAN&&b.units[shot.owner].npc==0)return Math.max(1,amount/5);
  if(b.units[shot.owner].npc==TribalRules.RAGE&&b.units[actor].npc==0&&protectedByBean(b,actor))return amount/2;
  return amount;
 }
 static void hurt(Battle b,int actor,int amount){Battle.Unit u=b.units[actor];if(u.hp<=0||u.god)return;int received=Math.min(u.hp,amount);u.hp-=received;u.flashTicks=20;u.lastBuff=0;b.showDamage(u,received,false);b.hitSerial++;}
 static void ignite(Battle b,int x){
  int at=-1;for(int i=0;i<b.sulanFireCount;i++)if(Math.abs(b.sulanFireX[i]-x)<24){at=i;break;}
  if(at<0){if(b.sulanFireCount==b.sulanFireX.length)return;at=b.sulanFireCount++;}
  b.sulanFireX[at]=Battle.clamp(x,12,468);b.sulanFireLife[at]=180;
 }
 static void ambient(Battle b){
  if(!active(b)||b.over||b.tribalFx>0||b.pendingNext)return;
  updateFacing(b);
  if(b.sulanTalk>0)b.sulanTalk--;
  if(b.tribalPhase==2&&b.units[6].npc==TribalRules.BEAN&&b.units[6].hp>0&&b.units[b.active].npc==0&&b.sulanStory<3&&++b.sulanStoryClock>=120&&b.sulanTalk==0){say(b,STORY1+b.sulanStory++,6,100);b.sulanStoryClock=0;}
  for(int i=b.sulanFireCount-1;i>=0;i--){
   int life=--b.sulanFireLife[i];
   if(life<=0){int last=--b.sulanFireCount;b.sulanFireX[i]=b.sulanFireX[last];b.sulanFireLife[i]=b.sulanFireLife[last];}
  }

 }
 static void beginTurn(Battle b){
  Battle.Unit u=b.units[b.active];if(u.npc==TribalRules.BEAN)u.face=b.units[1].x<u.x?-1:1;
  if(b.active<7&&b.sulanBurn[b.active]>0&&u.hp>0){int amount=30+b.dungeonDifficulty*25;if(u.npc==0&&protectedByBean(b,b.active))amount/=2;hurt(b,b.active,amount);b.check();}
 }
 static void extinguish(Battle b,int x,int y){
  for(int i=0;i<7;i++)if(Math.abs(b.units[i].x-x)<=35&&Math.abs(Battle.targetY(b.units[i])-y)<=40)b.sulanBurn[i]=0;
  for(int i=b.sulanFireCount-1;i>=0;i--)if(Math.abs(b.sulanFireX[i]-x)<=40){int last=--b.sulanFireCount;b.sulanFireX[i]=b.sulanFireX[last];b.sulanFireLife[i]=b.sulanFireLife[last];}
 }
 static void fireball(Battle b,int victim,int spread){TribalRules.projectile(b,1,victim,TribalRules.FIRE_SHOT,spread);}
 static void tickAI(Battle b){
  Battle.Unit u=b.units[b.active];if(u.hp<=0){b.next();return;}
  int t=++b.tribalClock;u.walk=t;b.botWait=t;if(u.npc==TribalRules.BEAN)u.face=b.units[1].x<u.x?-1:1;else if(t==1){int victim=TribalRules.target(b,b.active);if(victim!=b.active&&b.units[victim].x!=u.x)u.face=b.units[victim].x<u.x?-1:1;}
  if(u.npc==TribalRules.BEAN){
   b.tribalState=TribalRules.SPECIAL;
   if(t==1){u.attackTicks=36;say(b,NEAR,6,80);}
   if(t==24){b.sulanFireCount=0;for(int i=0;i<b.sulanBurn.length;i++)b.sulanBurn[i]=0;b.tribalBeanSaved=true;u.flashTicks=36;u.lastBuff=13;}
   if(t>=65&&!b.busy())b.next();return;
  }
  if(t>35&&b.tribalState==TribalRules.SUMMON){if(u.attackTicks==0&&!b.busy()){b.tribalState=TribalRules.IDLE;b.next();}return;}
  if(t==1){u.power++;b.tribalBeanSaved=false;say(b,u.npc==TribalRules.SULAN?SPEAR:u.power%2==1?HELL:FURY,1,130);}
  if(t==1&&u.npc==TribalRules.RAGE)callHealTotem(b);
  if(u.npc==TribalRules.RAGE&&t<=30){b.tribalState=TribalRules.SUMMON;return;}
  boolean fury=u.npc==TribalRules.RAGE&&u.power%2==0;
  b.tribalState=fury?TribalRules.SPECIAL:TribalRules.ATTACK;
  int count=playerCount(b),end=0;
  if(fury){
   if(t==45)u.attackTicks=36;
   if(t==65){for(int n=0;n<7;n+=2){Battle.Unit v=b.units[n];if(v.hp<=0||n==6&&v.npc!=TribalRules.BEAN)continue;flash(b,v.x,v.y-15);int amount=n==6?(b.dungeonDifficulty==0?260:b.dungeonDifficulty==1?430:720):TribalRules.DAMAGE[b.dungeonDifficulty]*2;if(n<6&&protectedByBean(b,n))amount/=2;hurt(b,n,amount);}b.check();}
   end=110;
  }else{
   boolean golden=u.npc==TribalRules.SULAN;
   int launches=golden?count:4;
   for(int n=0;n<launches;n++){int at=55+n*(golden?45:35);
    if(t==at-12)u.attackTicks=36;
    if(t==at){if(b.shotObserver!=null)b.shotObserver.beforeShot();if(golden)TribalRules.projectile(b,1,target(b,n),TribalRules.CHIEF_SHOT,0);else for(int shot=0;shot<3;shot++)fireball(b,target(b,shot%Math.max(1,count)),(shot-1)*12);b.shotSerial+=golden?1:3;}
   }
   end=55+Math.max(0,launches-1)*(golden?45:35)+60;
  }
  if(t>=end&&!b.busy()){if(b.tribalWave<u.power/2){callAdds(b);return;}b.tribalState=TribalRules.IDLE;b.next();}
 }
}
