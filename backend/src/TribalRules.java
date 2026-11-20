/** Shared, deterministic campaign rules. All online mutations run on the server. */
final class TribalRules {
 static final int IDLE=0,SELECT_TARGET=1,MOVE=2,AIM=3,ATTACK=4,SUMMON=5,SPECIAL=6,DEAD=7;
 static final int SPEAR=4,JAVELIN=5,ELITE=6,BEAN=7,BROTHER=8,SISTER=9,CHAIN=10,FATIGUE=11,HEAL=12,BOMB=13,SULAN=14,RAGE=15;
 static final String[] STAGES={"Rừng nguyên sinh","Bảo vệ Đậu Thần","Bulan Anh và Bulan Em","Đền thờ Sulan"};
 static final int[] HP={850,750,1500,6000,6500,6500,1000,1000,1400,1000,10500,14000};
 static final int[] DAMAGE={180,230,300};
 static final int[] WAVES={2,3,4}, STONE_MIN={10,50,100},STONE_MAX={100,200,300};
 static int reviveRounds=2, minLevel=15;
 static boolean active(Battle b){return b.dungeonStage>=3;}
 static int stage(Battle b){return b.dungeonStage-2;}
 static String name(int kind){switch(kind){case SPEAR:return "Thổ dân cầm giáo";case JAVELIN:return "Thổ dân ném lao";case ELITE:return "Thổ dân tinh nhuệ";case BEAN:return "Đậu Thần";case BROTHER:return "Bulan Anh";case SISTER:return "Bulan Em";case CHAIN:return "Xiềng Xích";case FATIGUE:return "Mỏi Mệt";case HEAL:return "Vật tổ hồi máu";case BOMB:return "Vật tổ phát nổ";case SULAN:return "Sulan";case RAGE:return "Sulan nổi giận";}return "Bộ Lạc Tà Thần";}
 static String dialogue(Battle b){if(b.tribalState==DEAD)return "Sulan: Không thể nào...!";int s=stage(b);if(b.tribalFx>0&&s==4&&b.tribalPhase==2)return "Sulan: Các ngươi sẽ phải trả giá!";if(b.turn==1)return s==1?"Thổ dân: Kẻ lạ mặt! Hãy rời khỏi rừng!":s==2?"Đậu Thần: Xin hãy bảo vệ ta!":s==3?"Bulan: Chúng ta sẽ luôn hồi sinh bên nhau!":"Sulan: Dám xâm phạm đền thờ của ta!";return "";}
 static void place(Battle b,int slot,int x){Battle.Unit u=b.units[slot];u.x=Battle.clamp(x,18,Battle.W-18);u.y=b.ground.surface(u.x,5);u.vy=0;b.refreshAim(u);}
 static void spawn(Battle b,int slot,int kind,int x){Battle.Unit u=b.units[slot];u.npc=kind;u.team=kind==BEAN?0:1;u.human=false;u.dead=false;u.statsEnabled=false;u.maxHp=HP[kind-SPEAR]*(100+b.dungeonDifficulty*45)/100;u.hp=u.maxHp;u.maxEnergy=u.energy=240;u.weapon=kind>=SULAN?9:8;u.attackBonus=DAMAGE[b.dungeonDifficulty]*(kind==RAGE?135:100)/100-Battle.baseDamage(u.weapon);u.defense=kind==RAGE?200:0;u.face=-1;u.angle=45;u.power=kind==BOMB?0:65;u.flashTicks=30;u.attackTicks=u.walk=u.frozen=u.freezeTurns=0;place(b,slot,x);}
 static void wave(Battle b){int count=3+b.dungeonDifficulty;for(int n=0;n<count;n++){int slot=n<3?1+n*2:7+n-3;int kind=n==count-1&&b.dungeonDifficulty>0?ELITE:n%2==0?SPEAR:JAVELIN;int x=stage(b)==2?(n%2==0?35+n*13:405+n*7):280+n*38;spawn(b,slot,kind,x);}b.tribalWave++;}
 static void setup(Battle b,int stage,int difficulty,boolean beanSaved){b.dungeonStage=stage+2;b.dungeonDifficulty=difficulty;b.unitCount=13;b.teams=true;b.tribalOutcome=-1;b.tribalGoal=WAVES[difficulty];b.tribalRound=1;b.tribalPhase=1;b.tribalWave=b.tribalState=b.tribalClock=b.tribalDead1=b.tribalDead3=0;b.tribalFx=60;b.tribalBeanSaved=beanSaved;for(int i=0;i<13;i++){Battle.Unit u=b.units[i];if(i<6&&i%2==0){u.npc=0;u.team=0;if(u.hp>0)place(b,i,stage==2?160+i*23:45+i*22);}else{u.npc=0;u.hp=0;u.dead=true;u.maxHp=0;}}
  if(stage<=2){if(stage==2)spawn(b,6,BEAN,240);wave(b);b.active=1;}
  else if(stage==3){spawn(b,1,BROTHER,335);spawn(b,3,SISTER,410);b.active=1;}
  else{spawn(b,1,SULAN,380);if(beanSaved)spawn(b,12,BEAN,85);b.active=1;}
  b.botWait=0;b.botPhase=0;b.countdown=b.turnLimit;b.over=false;b.winner=-1;
 }
 static boolean alive(Battle b,int kind){for(int i=0;i<b.unitCount;i++)if(b.units[i].npc==kind&&b.units[i].hp>0)return true;return false;}
 static boolean fighter(Battle.Unit u){return u.hp>0&&u.npc!=BEAN&&(u.npc<CHAIN||u.npc>=SULAN);}
 static int target(Battle b,int actor){if(stage(b)==2&&b.units[actor].team==1&&b.units[6].hp>0)return 6;int best=-1,d=99999;for(int i=0;i<b.unitCount;i++)if(b.units[i].hp>0&&b.units[i].team!=b.units[actor].team){int dist=Math.abs(b.units[i].x-b.units[actor].x);if(dist<d){best=i;d=dist;}}return best<0?actor:best;}
 static int nextActor(Battle b,int actor){for(int n=1;n<=b.unitCount;n++){int k=(actor+n)%b.unitCount;if(fighter(b.units[k])){if(k<=actor)round(b);return k;}}return actor;}
 static void summon(Battle b,int kind,int x){for(int i=6;i<13;i++)if(b.units[i].hp<=0&&b.units[i].npc!=BEAN){spawn(b,i,kind,x);return;}}
 static void heal(Battle b,Battle.Unit u,int amount){if(u.hp<=0)return;u.hp=Math.min(u.maxHp,u.hp+amount);u.flashTicks=36;u.lastBuff=13;}
 static void round(Battle b){b.tribalRound++;if(stage(b)==3){if(b.units[1].hp<=0&&b.units[3].hp>0&&b.tribalRound-b.tribalDead1>=reviveRounds){spawn(b,1,BROTHER,335);b.units[1].hp=b.units[1].maxHp/2;}if(b.units[3].hp<=0&&b.units[1].hp>0&&b.tribalRound-b.tribalDead3>=reviveRounds){spawn(b,3,SISTER,410);b.units[3].hp=b.units[3].maxHp/2;}}
  for(int i=6;i<13;i++){Battle.Unit u=b.units[i];if(u.hp<=0)continue;if(u.npc==HEAL)heal(b,b.units[1],b.units[1].maxHp/12);if(u.npc==BOMB){u.power++;if(u.power>=2){Battle.Ball bomb=new Battle.Ball();bomb.owner=1;bomb.x=u.x*256;bomb.y=(u.y-12)*256;bomb.damage=DAMAGE[b.dungeonDifficulty]*2;bomb.radius=48;bomb.weapon=9;b.impact(bomb);u.hp=0;}}}
  if(stage(b)==4&&b.tribalBeanSaved&&alive(b,BEAN)&&b.tribalRound%3==0){for(int i=0;i<6;i+=2)heal(b,b.units[i],180);b.tribalFx=45;}
 }
 static void beginTurn(Battle b){b.tribalClock=0;b.tribalState=IDLE;if(b.units[b.active].npc==0&&alive(b,FATIGUE))b.units[b.active].energy=Math.max(0,b.units[b.active].energy-100);}
 static void win(Battle b,int side){b.finishPower();b.over=true;b.winner=side;b.charging=false;b.pendingNext=false;b.bursts=b.ballCount=b.settle=0;b.winSerial++;}
 static boolean check(Battle b){if(b.tribalOutcome>=0){if(b.tribalFx<=0)win(b,b.tribalOutcome);return b.over;}boolean player=false,enemy=false;for(int i=0;i<b.unitCount;i++){Battle.Unit u=b.units[i];if(u.hp<=0&&!u.dead){u.dead=true;u.deathX=u.x;u.deathY=u.y;u.flashTicks=24;if(i==1)b.tribalDead1=b.tribalRound;if(i==3)b.tribalDead3=b.tribalRound;}if(u.hp>0){if(u.npc==0&&u.team==0)player=true;if(u.team==1&&fighter(u))enemy=true;}}
  if(!player||stage(b)==2&&b.units[6].hp<=0){win(b,1);return true;}
  if(stage(b)==4&&b.units[1].hp<=0&&b.tribalPhase==1){spawn(b,1,RAGE,380);b.tribalPhase=2;b.tribalFx=60;b.tribalState=SPECIAL;b.hitSerial++;return false;}
  if(!enemy){if(stage(b)<=2&&b.tribalWave<b.tribalGoal){wave(b);return false;}b.tribalOutcome=0;b.tribalState=stage(b)==4?DEAD:SPECIAL;b.tribalFx=stage(b)==2||stage(b)==4?45:0;if(b.tribalFx==0)win(b,0);return b.over;}
  if(b.tribalRound>80){win(b,1);return true;}return false;
 }
 /** AI uses the same movement, ballistic solver, fire, collisions and damage as PvP. */
 static void tickAI(Battle b){Battle.Unit u=b.units[b.active];if(u.hp<=0){b.next();return;}int t=++b.tribalClock;b.botWait=t;b.botActing=true;try{if(t<=8){b.tribalState=IDLE;return;}int target=target(b,b.active);Battle.Unit victim=b.units[target];if(t<=14){b.tribalState=SELECT_TARGET;u.face=victim.x<u.x?-1:1;return;}
  if(t<40&&u.npc==SPEAR){b.tribalState=MOVE;if(Math.abs(u.x-victim.x)>38)b.move(u.face);return;}
  if(t==40){boolean boss=u.npc==BROTHER||u.npc==SISTER||u.npc>=SULAN;if(boss&&b.tribalRound%2==0){b.tribalState=SUMMON;int kind=stage(b)==3?(u.npc==BROTHER?CHAIN:FATIGUE):b.tribalRound%4==0?BOMB:HEAL;summon(b,kind,kind==BOMB?victim.x:300+b.rand(120));}
   else b.tribalState=AIM;u.face=victim.x<u.x?-1:1;b.aim();u.attackTicks=24;return;}
  if(t==48){b.tribalState=u.npc==RAGE?SPECIAL:ATTACK;if(u.npc==SPEAR&&Math.abs(u.x-victim.x)<46&&Math.abs(u.y-victim.y)<30){Battle.Ball hit=new Battle.Ball();hit.owner=b.active;hit.x=victim.x*256;hit.y=Battle.targetY(victim)*256;hit.damage=DAMAGE[b.dungeonDifficulty];hit.radius=3;hit.weapon=0;b.impact(hit);b.settle=18;}else{if(u.npc==RAGE)b.triple=true;b.fire();}return;}
  if(t>55&&!b.busy())b.next();if(t>180)b.next();
 }finally{b.botActing=false;}}
}
