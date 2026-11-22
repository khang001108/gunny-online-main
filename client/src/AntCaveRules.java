/** Native NPC setup shared by offline play and the TCP server. */
final class AntCaveRules {
 static final int ANT_RELUCTANT=10, ANT_ATTACK=11, GUARD_ORDER=12, QUEEN_THREAT=13;
 static final int ANT_HALT=14, GUARD_LOOT=15, GUARD_HAIR=16, QUEEN_HIT=17, QUEEN_SUCCESS=18, QUEEN_WARNING=19;
 static final int[] ANT_LINES={ANT_RELUCTANT,ANT_ATTACK,ANT_HALT};
 static final int[] GUARD_LINES={GUARD_ORDER,GUARD_LOOT,GUARD_HAIR};
 static final int QUEEN_INTRO=100, QUEEN_SPEECH=150, QUEEN_TITLE=210;
 static final int QUEEN_TITLE_REVEAL=21, QUEEN_TITLE_HOLD=60;
 static final int QUEEN_CALL=QUEEN_TITLE+QUEEN_TITLE_REVEAL+QUEEN_TITLE_HOLD;
 static final int GUARD_ENTRY=QUEEN_CALL+40, GUARD_ORDER_START=GUARD_ENTRY+44;
 static final int INTRO_END=GUARD_ORDER_START+62, QUEEN_DEATH=INTRO_END+9, DEATH_END=QUEEN_DEATH+80;
 static final int[] ANTS={1,3,5,6,7,8,9,10,11,12};
 static final int ANT_GOAL=20, ANT_WAVE=6;
 static final int ANT_INTRO=700, ANT_INTRO_END=745;
 static final int[] GUARDS={3,5,6,7,8,9,10,11};
 static final int INITIAL_GUARDS=5, GUARD_INTERVAL=9;
 static final int REINFORCEMENTS=550;
 static final int QUEEN_ARROW=10;

 static boolean titleVisible(Battle b){return b.botPhase>=QUEEN_TITLE&&b.botPhase<QUEEN_CALL;}
 static boolean dying(Battle b){return b.botPhase>=QUEEN_DEATH&&b.botPhase<DEATH_END;}
 static boolean summoning(Battle b){return b.botPhase>=REINFORCEMENTS&&b.botPhase<ANT_INTRO;}
 static boolean antOpening(Battle b){return b.botPhase>=ANT_INTRO&&b.botPhase<ANT_INTRO_END;}
 static void placeAlly(Battle b,int slot){
  Battle.Unit u=b.units[slot];u.x=(b.dungeonStage==1?240:90)+allyOffset(slot);u.y=b.ground.surface(u.x,20);u.face=1;b.refreshAim(u);
 }
 static int allyOffset(int slot){return slot==0?0:slot==2?-35:35;}

 static int initialAntWave(Battle b){
  b.unitCount=13;
  for(int i=0;i<ANTS.length;i++){
   Battle.Unit ant=b.units[ANTS[i]];
   ant.hp=ant.maxHp=ant.energy=0;ant.npc=1+i%2;ant.dead=false;ant.team=1;
  }
  return spawnAntWave(b,0,0);
 }

 static int spawnAntWave(Battle b,int spawned,int live){
  int amount=Math.min(ANT_WAVE,ANT_GOAL-spawned), placed=0;
  if(count(live)+amount>ANTS.length)return live;
  for(int i=0;i<ANTS.length&&placed<amount;i++){
   int slot=ANTS[i];
   if((live&(1<<slot))!=0)continue;
   // Alternate colours across both sides, so a colour never attacks from just one edge.
   boolean left=placed%2==0;
   int lane=placed/2;
   int color=1+(placed+lane)%2;
   int x=spawnPosition(b,slot,left,lane);
   spawn(b,slot,color,b.dungeonDifficulty);
   Battle.Unit ant=b.units[slot];
   ant.x=x;
   ant.y=b.ground.surface(ant.x,20);
   ant.face=left?1:-1;
   b.refreshAim(ant);
   live|=1<<slot;
   placed++;
  }
  return live;
 }
 static int spawnPosition(Battle b,int slot,boolean left,int lane){
  int x=left?44+lane*24:Battle.W-44-lane*24, step=left?18:-18;
  for(int tries=0;tries<5&&b.occupied(x,slot,17);tries++)x+=step;
  return x;
 }

 static int survivingAnts(Battle b,int live){
  for(int i=0;i<ANTS.length;i++)if(b.units[ANTS[i]].hp<=0)live&=~(1<<ANTS[i]);
  return live;
 }

 static int count(int mask){
  int total=0;
  for(;mask!=0;mask&=mask-1)total++;
  return total;
 }

 static int firstAnt(Battle b,int color){
  for(int i=0;i<b.unitCount;i++)if(b.units[i].npc==color&&b.units[i].hp>0)return i;
  return -1;
 }

 static int nextActor(Battle b,int previous){
  int type=b.units[previous].npc;
  if(type==0){
   for(int slot=previous+2;slot<6;slot+=2)if(b.units[slot].hp>0)return slot;
  }
  if(b.dungeonStage==1){
   if(type==0){b.lastDungeonAlly=previous;int red=firstAnt(b,1);if(red>=0)return red;}
   if(type!=2){int blue=firstAnt(b,2);if(blue>=0)return blue;}
  }else{
   if(type==0&&b.units[1].hp>0){b.lastDungeonAlly=previous;return 1;}
   if(type==3){int guard=firstAnt(b,2);if(guard>=0)return guard;}
  }
  for(int slot=0;slot<6;slot+=2)if(b.units[slot].hp>0)return slot;
  return previous;
 }

 static boolean introducing(Battle battle) {
  return battle.botPhase>=QUEEN_INTRO && battle.botPhase<INTRO_END;
 }
 static void startQueenIntro(Battle battle) {
  battle.dungeonStage=2;
  battle.unitCount=12;
  for(int i=0;i<GUARDS.length;i++){
   Battle.Unit guard=battle.units[GUARDS[i]];
   guard.hp=guard.maxHp=guard.energy=0;guard.npc=2;guard.dead=false;guard.team=1;
  }
  battle.active=1;
  battle.botPhase=QUEEN_INTRO;
 }
 static void tickQueenIntro(Battle battle) {
  int tick=++battle.botPhase;
  int first=firstAlly(battle);
  if(first<0) {
   battle.botPhase=0;
   battle.check();
   return;
  }
  int elapsed=tick-GUARD_ENTRY;
  if(elapsed>=0&&elapsed<INITIAL_GUARDS*GUARD_INTERVAL&&elapsed%GUARD_INTERVAL==0)
   spawnGuard(battle,GUARDS[elapsed/GUARD_INTERVAL]);
  advanceGuards(battle,INITIAL_GUARDS);
  if(tick==INTRO_END)battle.beginTurn(1);
 }
 static void advanceGuards(Battle battle,int end) {
  for(int i=0;i<end;i++){
   Battle.Unit guard=battle.units[GUARDS[i]];
   if(guard.hp<=0)continue;
   int target=144+i*23;
   if(guard.x<=target)continue;
   int x=guard.x-1;
   int y=battle.ground.surface(x,Math.max(0,guard.y-5));
   if(y>=Battle.H || y-guard.y>8||battle.blockedStep(guard.x,x,GUARDS[i],23))continue;
   guard.x=x;
   guard.y=y;
   guard.walk++;
   battle.refreshAim(guard);
  }
 }
 static int firstAlly(Battle battle) {
  int bot=-1;
  for(int slot=0;slot<6;slot+=2) {
   Battle.Unit ally=battle.units[slot];
   if(ally.hp<=0)continue;
   if(ally.human)return slot;
   if(bot<0)bot=slot;
  }
  return bot;
 }
 static void spawnGuard(Battle battle,int slot) {
  spawn(battle,slot,2,battle.dungeonDifficulty);
  place(battle,slot,2,0);
 }
 static void tickReinforcements(Battle b){
  int elapsed=++b.botPhase-REINFORCEMENTS;
  if(elapsed>=35&&elapsed<35+3*GUARD_INTERVAL&&(elapsed-35)%GUARD_INTERVAL==0){
   for(int i=0;i<GUARDS.length;i++){
    if(b.units[GUARDS[i]].hp<=0){spawnGuard(b,GUARDS[i]);break;}
   }
  }
  if(elapsed>=35)advanceGuards(b,GUARDS.length);
  if(elapsed>=95){b.botPhase=0;b.next();}
 }
 static void spawn(Battle b,int slot,int type,int difficulty){Battle.Unit u=b.units[slot];int d=Battle.clamp(difficulty,0,2);u.npc=type;u.human=false;u.team=1;u.statsEnabled=false;u.x=type==3?405:310+slot/2*55;u.y=b.ground.surface(u.x,20);u.vy=u.walk=0;u.dead=false;u.weapon=type==3?9:0;u.face=-1;u.angle=45;u.power=60;int[][] hp={{450,600,900},{700,900,1200},{6000,8000,10500}},attack={{90,120,150},{90,120,150},{240,300,420}},level={{10,15,25},{10,15,25},{18,25,40}};int row=type==3?2:type==2?1:0;u.maxHp=hp[row][d];u.hp=u.maxHp;u.maxEnergy=u.energy=240;u.heals=u.rage=u.freezeTurns=u.frozen=u.invisible=u.attackTicks=u.flashTicks=u.landingTicks=0;u.attackBonus=attack[row][d]-Battle.baseDamage(u.weapon);u.experience=Progression.experienceForLevel(level[row][d]);b.refreshAim(u);}
 static String name(int type,boolean queenStage){return type==3?"Nữ Vương Kiến":queenStage?"Kiến vệ sĩ":type==1?"Kiến trinh sát":"Kiến tiên phong";}
 static void place(Battle battle, int slot, int stage, int spawned) {
  Battle.Unit unit = battle.units[slot];
  if (stage == 1) {
   boolean left = spawned % 2 == 0;
   int spacing = (slot / 2) * 24;
   unit.x = left ? 56 + spacing : Battle.W - 56 - spacing;
   unit.face = left ? 1 : -1;
  } else {
   unit.x = unit.npc == 3 ? 356 : 310;
   unit.face = -1;
  }
  unit.y = battle.ground.surface(unit.x, stage == 2 ? 95 : 20);
  battle.refreshAim(unit);
 }
}
