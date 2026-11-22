/** Deterministic three-encounter adaptation of the supplied Gunny Mobi recordings.
 * Save IDs 3/4/6 remain stable. ID 5 is a legacy alias of the Bulan encounter.
 * Timings and damage are MIDP tuning, not claimed recovered Mobi server formulas.
 */
final class TribalRules {
 static final int IDLE=0,SELECT_TARGET=1,MOVE=2,AIM=3,ATTACK=4,SUMMON=5,SPECIAL=6,DEAD=7,OPENING=8,TRANSFORM=9;
 static final int SPEAR=4,JAVELIN=5,ELITE=6,BEAN=7,BROTHER=8,SISTER=9,CHAIN=10,FATIGUE=11,HEAL=12,BOMB=13,SULAN=14,RAGE=15;
 static final int SPEAR_SHOT=11,SPIRIT_SHOT=12,FIRE_SHOT=13,CHIEF_SHOT=14;
 static final int INTRO_SPEAR=1,MASK_LINE=2,CHICKEN_LINE=3,THROW_LINE=4,BULAN_INTRO=5,FATIGUE_LINE=6,CHAIN_LINE=7,CALL_LINE=8,SULAN_INTRO=9,HEAL_LINE=10,POWER_LINE=11,BORROW_LINE=12,FIRE_LINE=13,BOMB_LINE=14,BEAN_LINE=15,BEAN_POWER=16,REVIVE_LINE=17,BULAN_THROW=18;
 static final String[] STAGES={"Lính Bộ Lạc","Anh em Bulan","Anh em Bulan","Đền thờ Sulan"};
 static final int[] HP={850,850,1500,3000,6500,6500,1000,1000,1400,1000,10500,14000};
 static final int[] DAMAGE={180,230,300};
 static final int[] WAVES={4,4,4},STONE_MIN={10,50,100},STONE_MAX={100,200,300};
 static final int GOAL=20;
 static final int[] ENEMIES={1,3,5,6,7,8,9,10,11,12};
 static int reviveRounds=2,minLevel=15;
 static boolean active(Battle b){return b.dungeonStage>=3;}
 static int stage(Battle b){return b.dungeonStage-2;}
 static boolean twins(Battle b){return b.dungeonStage==4||b.dungeonStage==5;}
 static boolean payload(int weapon){return weapon>=SPEAR_SHOT&&weapon<=CHIEF_SHOT;}
 static boolean floats(Battle.Unit u){return u.npc==BROTHER||u.npc==SISTER;}
 static String name(int kind){switch(kind){case SPEAR:case JAVELIN:return "Lính Bộ Lạc";case ELITE:return "Chiến Sĩ Bộ Lạc";case BEAN:return "Đậu Hà Lan";case BROTHER:return "Bulan Anh";case SISTER:return "Bulan Em";case CHAIN:return "Vật Tổ Xiềng Xích";case FATIGUE:return "Vật Tổ Mệt Mỏi";case HEAL:return "Vật Tổ HP";case BOMB:return "Vật Tổ Nổ";case SULAN:case RAGE:return "Sulan";}return "Bộ Lạc Tà Thần";}
 static String dialogue(Battle b){switch(b.botPhase){
  case INTRO_SPEAR:return "Một vật tế đổi một con thỏ, vậy sao không bắt thỏ?";
  case MASK_LINE:return "Đứng sau mặt nạ, không ai biết ta là ai!";
  case CHICKEN_LINE:return "Hôm qua ta đã lén ăn gà dùng tế lễ :)";
  case THROW_LINE:return "Hãy xem đây!";
  case BULAN_INTRO:return "Anh ơi, đây là vật tế sống cuối cùng, mau bắt về.";
  case FATIGUE_LINE:return "Hãy thử Vật Tổ Mệt Mỏi của ta!";
  case CHAIN_LINE:return "Hãy thử Vật Tổ Xiềng Xích của ta!";
  case CALL_LINE:return "Chiến sĩ bộ lạc, hạ gục chúng!";
  case SULAN_INTRO:return "Dám ngăn nghi lễ của ta, đừng hòng sống sót trở về!";
  case HEAL_LINE:return "Vật Tổ HP, hãy cho ta sức mạnh.";
  case POWER_LINE:return "Ngươi có thể chống lại sức mạnh của ta?";
  case BORROW_LINE:return "Cho ta mượn tạm thân thể nhỏ bé này nhé!";
  case FIRE_LINE:return "Nếm thử sức mạnh của ta!";
  case BOMB_LINE:return "Hãy ném đi!";
  case BEAN_LINE:return "Hãy đánh vào thân thể của tù trưởng!";
  case BEAN_POWER:return "Ta đã trở lại, hãy nhận sức mạnh này!";
  case BULAN_THROW:return "Ném một đống lớn!";
  case REVIVE_LINE:return "Anh em ta sẽ trở lại!";
 }return "";}
 static int speaker(Battle b){if(b.botPhase==BEAN_LINE||b.botPhase==BEAN_POWER){for(int i=6;i<13;i++)if(b.units[i].npc==BEAN&&b.units[i].hp>0)return i;}return b.units[b.active].npc>0?b.active:1;}
 static void place(Battle b,int slot,int x){Battle.Unit u=b.units[slot];u.x=Battle.clamp(x,18,Battle.W-18);u.y=b.ground.surface(u.x,5);u.vy=0;b.refreshAim(u);}
 static void spawn(Battle b,int slot,int kind,int x){
  Battle.Unit u=b.units[slot];u.npc=kind;u.team=kind==BEAN?0:1;u.human=false;u.dead=false;u.statsEnabled=false;
  u.maxHp=HP[kind-SPEAR]*(100+b.dungeonDifficulty*45)/100;u.hp=u.maxHp;u.maxEnergy=u.energy=240;
  u.weapon=8;u.attackBonus=DAMAGE[b.dungeonDifficulty]-Battle.baseDamage(8);u.defense=kind==RAGE?640:0;
  u.face=-1;u.angle=45;u.power=0;u.flashTicks=30;u.attackTicks=u.walk=u.frozen=u.freezeTurns=0;u.lastBuff=14;place(b,slot,x);
  if(floats(u)){u.y=140;u.face=kind==BROTHER?1:-1;}
 }
 static int allyX(int actor,boolean middle){return middle?240+(actor==0?0:actor==2?-36:36):52+actor*14;}
 static void wave(Battle b){
  for(int k=0;k<ENEMIES.length;k++){Battle.Unit old=b.units[ENEMIES[k]];if(old.hp<=0){old.npc=0;old.maxHp=0;old.dead=true;}}
  int amount=Math.min(6,b.tribalGoal);
  for(int n=0;n<amount;n++){int slot=ENEMIES[n];int kind=n==amount-1?ELITE:SPEAR;spawn(b,slot,kind,300+n*28);b.tribalDead3++;}
  b.tribalWave++;
 }
 static void setup(Battle b,int encounter,int difficulty,boolean ignoredBeanSave){
  b.dungeonStage=encounter+2;b.dungeonDifficulty=difficulty;b.unitCount=13;b.teams=true;b.tribalOutcome=-1;b.tribalGoal=GOAL;
  b.tribalRound=1;b.tribalPhase=1;b.tribalWave=b.tribalClock=b.tribalDead1=b.tribalDead3=0;
  b.tribalFx=120;b.tribalState=OPENING;b.tribalBeanSaved=false;b.ballCount=b.bursts=b.settle=0;
  for(int i=0;i<13;i++){Battle.Unit u=b.units[i];if(i<6&&i%2==0){u.npc=0;u.team=0;if(u.hp>0)place(b,i,allyX(i,twins(b)));}else{u.npc=0;u.hp=0;u.dead=true;u.maxHp=0;}}
  if(encounter==1){wave(b);b.botPhase=INTRO_SPEAR;}
  else if(twins(b)){spawn(b,1,BROTHER,88);spawn(b,3,SISTER,408);b.botPhase=BULAN_INTRO;}
  else{spawn(b,1,SULAN,300);b.botPhase=SULAN_INTRO;}
  b.active=twins(b)?3:1;b.botWait=0;b.countdown=b.turnLimit;b.over=false;b.winner=-1;b.charging=false;
 }
 static boolean alive(Battle b,int kind){for(int i=0;i<b.unitCount;i++)if(b.units[i].npc==kind&&b.units[i].hp>0)return true;return false;}
 static boolean fighter(Battle.Unit u){return u.hp>0&&u.npc!=BEAN&&(u.npc<CHAIN||u.npc>=SULAN);}
 static int target(Battle b,int actor){
  int best=-1,distance=99999;Battle.Unit who=b.units[actor];
  if(who.npc==0&&who.team==0&&stage(b)==4&&b.units[1].hp>0)return 1;
  for(int i=0;i<b.unitCount;i++){Battle.Unit u=b.units[i];if(u.hp<=0||u.team==who.team||who.team==1&&u.npc!=0)continue;int d=Math.abs(u.x-who.x);if(d<distance){distance=d;best=i;}}
  return best<0?actor:best;
 }
 static int nextActor(Battle b,int actor){
  if(stage(b)==1){
   int first=-1,last=-1;for(int i=0;i<b.unitCount;i++)if(b.units[i].hp>0){if(b.units[i].npc==SPEAR&&first<0)first=i;if(b.units[i].npc==ELITE&&last<0)last=i;}
   int[] order={first,last,0,2,4};int old=b.units[actor].npc==SPEAR?0:b.units[actor].npc==ELITE?1:actor==0?2:actor==2?3:4;
   for(int n=1;n<=5;n++){int k=(old+n)%5;if(order[k]>=0&&b.units[order[k]].hp>0){if(k<=old)round(b);return order[k];}}
   return actor;
  }
  if(twins(b)){
   int[] order={3,1,6,7,8,9,10,11,12,0,2,4};int old=0;for(int i=0;i<order.length;i++)if(order[i]==actor)old=i;
   for(int n=1;n<=order.length;n++){int at=(old+n)%order.length,k=order[at];if(fighter(b.units[k])){if(at<=old)round(b);return k;}}return actor;
  }
  for(int n=1;n<=b.unitCount;n++){int k=(actor+n)%b.unitCount;if(fighter(b.units[k])){if(k<=actor)round(b);return k;}}return actor;
 }
 static int summon(Battle b,int kind,int x){int live=0;for(int n=6;n<13;n++)if(b.units[n].hp>0&&b.units[n].npc==kind)live++;if(live>=(kind==BEAN||kind==CHAIN||kind==FATIGUE?1:2))return -1;for(int i=6;i<13;i++)if(b.units[i].hp<=0){spawn(b,i,kind,x);return i;}return -1;}
 static void heal(Battle b,Battle.Unit u,int amount){if(u.hp<=0)return;u.hp=Math.min(u.maxHp,u.hp+amount);u.flashTicks=36;u.lastBuff=13;}
 static void round(Battle b){
  b.tribalRound++;
  if(twins(b)){
   if(b.units[1].hp<=0&&b.units[3].hp>0&&b.tribalRound-b.tribalDead1>=reviveRounds){spawn(b,1,BROTHER,88);b.units[1].hp=b.units[1].maxHp/2;}
   if(b.units[3].hp<=0&&b.units[1].hp>0&&b.tribalRound-b.tribalDead3>=reviveRounds){spawn(b,3,SISTER,408);b.units[3].hp=b.units[3].maxHp/2;}
  }
  for(int i=6;i<13;i++){
   Battle.Unit u=b.units[i];if(u.hp<=0)continue;
   if(u.npc==HEAL)heal(b,b.units[1],b.units[1].maxHp/12);
   if(u.npc==BOMB&&++u.power>=2){Battle.Ball hit=new Battle.Ball();hit.owner=1;hit.x=u.x*256;hit.y=(u.y-12)*256;hit.damage=DAMAGE[b.dungeonDifficulty]*2;hit.radius=42;hit.weapon=FIRE_SHOT;b.impact(hit);u.hp=0;}
  }
  if(stage(b)==4&&alive(b,BEAN)){
   if(b.tribalRound%3==0){b.tribalBeanSaved=true;b.tribalDead3=b.tribalRound+2;b.tribalState=SPECIAL;b.tribalFx=45;b.botPhase=BEAN_POWER;for(int i=0;i<6;i+=2)if(b.units[i].hp>0){b.units[i].flashTicks=36;b.units[i].lastBuff=13;}}
   else if(b.tribalRound>=b.tribalDead3)b.tribalBeanSaved=false;
  }
 }
 static void beginTurn(Battle b){b.tribalClock=0;if(b.tribalFx==0)b.tribalState=IDLE;else if(b.tribalState==SPECIAL)b.botPhase=BEAN_POWER;
  if(b.units[b.active].npc==0&&alive(b,FATIGUE))b.units[b.active].energy=Math.max(0,b.units[b.active].energy-100);
 }
 static void tickEffect(Battle b){
  if(--b.tribalFx>0)return;
  if(b.tribalState==DEAD){check(b);return;}
  if(b.tribalState==TRANSFORM){b.tribalState=IDLE;b.next();return;}
  b.tribalState=IDLE;b.botPhase=0;
 }
 static void win(Battle b,int side){b.finishPower();b.over=true;b.winner=side;b.charging=false;b.pendingNext=false;b.bursts=b.ballCount=b.settle=0;b.winSerial++;}
 static boolean check(Battle b){
  if(b.tribalOutcome>=0){if(b.tribalFx<=0)win(b,b.tribalOutcome);return b.over;}
  boolean player=false,enemy=false;
  for(int i=0;i<b.unitCount;i++){
   Battle.Unit u=b.units[i];if(u.hp<=0&&!u.dead){u.dead=true;u.deathX=u.x;u.deathY=u.y;u.flashTicks=30;
    if(stage(b)==1&&u.team==1&&u.npc>=SPEAR&&u.npc<=ELITE){b.tribalDead1++;b.tribalGoal=Math.max(0,b.tribalGoal-1);}
    if(twins(b)&&i==1)b.tribalDead1=b.tribalRound;if(twins(b)&&i==3)b.tribalDead3=b.tribalRound;
   }
   if(u.hp>0){if(u.npc==0&&u.team==0)player=true;if(u.team==1&&fighter(u))enemy=true;}
  }
  if(!player){win(b,1);return true;}
  if(stage(b)==4&&b.units[1].hp<=0&&b.tribalPhase==1){
   int x=b.units[1].x;spawn(b,1,RAGE,x);b.tribalPhase=2;b.tribalFx=75;b.tribalState=TRANSFORM;b.botPhase=POWER_LINE;
   b.finishPower();b.ballCount=b.bursts=b.settle=0;b.charging=false;b.hitSerial++;return false;
  }
  if(stage(b)==1&&!enemy&&b.tribalGoal>0){wave(b);return false;}
  if(stage(b)==4&&b.units[1].hp<=0||twins(b)&&b.units[1].hp<=0&&b.units[3].hp<=0||!enemy){b.tribalOutcome=0;b.tribalState=DEAD;b.tribalFx=stage(b)==4?65:35;b.ballCount=b.bursts=b.settle=0;b.charging=false;return false;}
  if(b.turn>200){win(b,1);return true;}return false;
 }
 static void projectile(Battle b,int actor,int victim,int weapon,int offset){
  if(b.ballCount>=b.balls.length)return;Battle.Unit u=b.units[actor],v=b.units[victim];Battle.Ball p=new Battle.Ball();
  p.owner=actor;p.weapon=weapon;p.x=(u.x+u.face*(u.npc>=SULAN?22:12))*256;p.y=(u.y-(u.npc>=SULAN?39:22))*256;
  int steps=Math.max(70,Math.min(170,Math.abs(v.x-u.x)*2/3+65));
  p.vx=(v.x*256-p.x)*4/steps;p.vy=((Battle.targetY(v)+offset)*256-p.y)*4/steps-Battle.gravity(p)*(steps+1)/2;
  p.radius=weapon==FIRE_SHOT?11:4;p.damage=DAMAGE[b.dungeonDifficulty]*(u.npc==RAGE?130:100)/100;
  b.balls[b.ballCount++]=p;
 }
 static void strike(Battle b,int victim,int damage){Battle.Unit v=b.units[victim];Battle.Ball p=new Battle.Ball();p.owner=b.active;p.x=v.x*256;p.y=Battle.targetY(v)*256;p.weapon=SPEAR_SHOT;p.damage=damage;p.radius=1;b.impact(p);}
 static int action(Battle b,Battle.Unit u){
  int round=b.tribalRound;
  if(u.npc==SPEAR||u.npc==JAVELIN)return 0;
  if(u.npc==ELITE)return 1;
  if(u.npc==BROTHER)return round%3==1?2:round%3==0?4:1;
  if(u.npc==SISTER)return round%3==2?3:1;
  if(u.npc==SULAN)return round%4==2?5:round%4==3?4:round%4==0?6:1;
  if(!alive(b,BEAN))return 7;
  if(Math.abs(u.x-b.units[target(b,b.active)].x)<48&&round%2==0)return 9;
  return round%4==0?6:8;
 }
 static void tickAI(Battle b){
  Battle.Unit u=b.units[b.active];if(u.hp<=0){b.next();return;}
  int t=++b.tribalClock;b.botWait=t;u.walk=t;b.botActing=true;
  try{
   int victim=target(b,b.active);Battle.Unit v=b.units[victim];u.face=v.x<u.x?-1:1;
   int action=action(b,u);
   if(t<=12){b.tribalState=IDLE;return;}
   if(t==13){b.tribalState=SELECT_TARGET;b.botPhase=action==2?FATIGUE_LINE:action==3?CHAIN_LINE:action==4?CALL_LINE:action==5?HEAL_LINE:action==6?BOMB_LINE:action==7?BORROW_LINE:u.npc>=SULAN?BOMB_LINE:u.npc==BROTHER||u.npc==SISTER?BULAN_THROW:(b.tribalRound%2==0?MASK_LINE:CHICKEN_LINE);}
   if(t<=46){
    if(action==0){b.tribalState=MOVE;int leader=b.active;
     for(int i=0;i<b.unitCount;i++)if(b.units[i].hp>0&&b.units[i].npc==SPEAR){Battle.Unit ally=b.units[i];int enemy=target(b,i);ally.face=b.units[enemy].x<ally.x?-1:1;ally.walk=t;b.active=i;if(Math.abs(ally.x-b.units[enemy].x)>30)b.move(ally.face);}
     b.active=leader;}
    else b.tribalState=action>=2&&action<=7?SUMMON:AIM;
    return;
   }
   if(t==47){u.attackTicks=36;if(action==0)for(int i=0;i<b.unitCount;i++)if(b.units[i].hp>0&&b.units[i].npc==SPEAR)b.units[i].attackTicks=36;b.tribalState=action>=2&&action<=7?SUMMON:action==9?SPECIAL:ATTACK;}
   if(t<61)return;
   if(t==61){
    if(action==0){int leader=b.active;for(int i=0;i<b.unitCount;i++)if(b.units[i].hp>0&&b.units[i].npc==SPEAR){Battle.Unit ally=b.units[i];int enemy=target(b,i);ally.attackTicks=22;b.active=i;if(Math.abs(ally.x-b.units[enemy].x)<42&&Math.abs(ally.y-b.units[enemy].y)<32)strike(b,enemy,DAMAGE[b.dungeonDifficulty]);}b.active=leader;b.settle=24;}
    else if(action==9){if(Math.abs(u.x-v.x)<55&&Math.abs(u.y-v.y)<50)strike(b,victim,DAMAGE[b.dungeonDifficulty]);b.settle=24;}
    else if(action==2||action==3){summon(b,action==2?FATIGUE:CHAIN,action==2?184:296);b.settle=30;}
    else if(action==4){for(int n=0;n<2;n++){int slot=summon(b,ELITE,twins(b)?(u.x<240?106:390):u.x-38-n*25);if(slot>=0&&twins(b))b.units[slot].y=86;}b.settle=30;}
    else if(action==5){summon(b,HEAL,u.x-40);summon(b,HEAL,u.x+40);b.settle=30;}
    else if(action==6){summon(b,BOMB,Battle.clamp(v.x-28,20,460));summon(b,BOMB,Battle.clamp(v.x+28,20,460));b.settle=30;}
    else if(action==7){strike(b,victim,DAMAGE[b.dungeonDifficulty]*4/3);summon(b,BEAN,Battle.clamp(v.x-54,24,450));b.tribalBeanSaved=false;b.settle=30;}
    else{
     if(b.shotObserver!=null)b.shotObserver.beforeShot();
     if(action==8){for(int n=-1;n<=1;n++)projectile(b,b.active,victim,FIRE_SHOT,n*8);b.botPhase=FIRE_LINE;}
     else projectile(b,b.active,victim,u.npc==ELITE?SPEAR_SHOT:u.npc>=SULAN?CHIEF_SHOT:SPIRIT_SHOT,0);
     b.shotSerial++;
    }
    return;
   }
   if(t>100&&!b.busy())b.next();
   if(t>200)b.next();
  }finally{b.botActing=false;}
 }
}
