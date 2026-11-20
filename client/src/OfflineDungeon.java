/** Local Ant Cave campaign; never logs in or sends account/network requests. */
final class OfflineDungeon {
 final PhoneCanvas c;boolean active,settled,picker;int stage,difficulty,kills,spawned,alive;
 final int[] allies = {-1,-1};
 int closedSlots;
 OfflineDungeon(PhoneCanvas canvas){c=canvas;}
 void open(){active=false;stage=0;allies[0]=allies[1]=-1;closedSlots=0;picker=false;c.roomSelection=29;c.mode=19;}
 void draw(){
  c.dungeon.drawRoom(false);
  if(picker)c.dungeon.drawPicker();
  if(c.net.social.offlineInvite && c.net.social.opened)c.net.social.drawInvitePopup();
  if(c.toastTicks>0)c.drawBattleNotice();
 }
 void key(int key,int dir,boolean ok){
  if(c.net.social.offlineInvite && c.net.social.opened)c.net.social.popupKey(key,dir,ok);
  else if(!picker && (key=='g' || key=='G'))c.openCheats();
  else c.dungeon.keyRoom(false,key,dir,ok);
 }
 void pointer(int x,int y){
  if(c.net.social.offlineInvite && c.net.social.opened)c.net.social.popupPointer(x,y);
  else c.dungeon.pointerRoom(false,x,y);
 }
 void begin(){
  active=true;settled=false;if(stage!=2)stage=1;kills=spawned=alive=0;prepare();
 }
 void returnForBoss(){active=false;stage=2;c.roomSelection=5;c.mode=19;}
 void prepare(){
  try{
   Battle b=c.game;
   if(c.onlineAccount()||c.requireWeapon()){active=false;return;}
   c.training=false;c.twoPlayers=false;
   int allyMode=c.allyMode;
   c.allyMode=0;
   try{c.beginMatch(stage==1?6:7);}
   finally{c.allyMode=allyMode;}
   b.configureOnlineTeam();b.explicitHumans=true;b.deferNext=true;
   b.turnLimit=b.countdown=450;b.dungeonDifficulty=difficulty;b.dungeonStage=stage;
   for(int i=0;i<6;i++){
    b.units[i].human=i==0;
    b.units[i].hp=0;
    b.units[i].dead=false;
    if(stage==2&&i%2==1)b.units[i].npc=2;
   }
   Battle.Unit u=b.units[0];
   AntCaveRules.placeAlly(b,0);u.weapon=c.equipped;u.experience=c.experience;
   ForgeRules.apply(u,c.economy.bonuses(),c.economy.ranks());ItemRules.apply(u,c.wornMask);
   u.hp=u.maxHp;u.energy=u.maxEnergy;b.refreshAim(u);
   for(int slot=2;slot<=4;slot+=2)if(allyAt(slot)>=0)prepareAlly(b,slot);
   alive=0;b.active=1;
   if(stage==1){
    alive=AntCaveRules.initialAntWave(b);
    spawned=AntCaveRules.count(alive);
    b.active=AntCaveRules.firstAnt(b,1);
   b.botPhase=AntCaveRules.ANT_INTRO;
   }else{
    AntCaveRules.spawn(b,1,3,difficulty);
    AntCaveRules.place(b,1,2,0);
    AntCaveRules.startQueenIntro(b);
   }
   c.loadTicks=0;c.mode=14;
  }catch(Exception e){active=false;c.mode=19;c.popup.notice(DungeonScreen.TITLE,"Không tải được ải viễn chinh");}
 }
 int allyAt(int actor) { return actor==2||actor==4 ? allies[actor/2-1] : -1; }
 private void prepareAlly(Battle battle, int slot) throws java.io.IOException {
  int ally = allyAt(slot);
  Battle.Unit bot = battle.units[slot];
  c.actorSex[slot] = c.botSex(ally);
  c.actorSkin[slot] = c.botSkin(ally);
  String appearance = c.actorSex[slot] + "_0_" + c.actorSkin[slot];
  c.actors[slot] = c.load("look"+appearance);
  c.actorStand[slot] = c.load("stand"+appearance);
  c.versusPortrait[slot] = null;
  bot.botStyle = ally % 4;
  AntCaveRules.placeAlly(battle,slot);
  bot.dead = false;
  bot.hp = bot.maxHp;
  bot.weapon = c.botWeapon(ally);
  bot.experience = Progression.botExperience(ally);
  battle.refreshAim(bot);
 }
 void progress(){
  if(!active)return;
  Battle b=c.game;
  if(stage==1){
   int remaining=AntCaveRules.survivingAnts(b,alive);
   kills+=AntCaveRules.count(alive^remaining);alive=remaining;
   if(b.over&&b.winner==0)b.ballCount=b.bursts=b.settle=0;
   if((b.pendingNext||b.over)&&!b.busy()&&(!b.over||b.winner==0)){
    if(kills>=AntCaveRules.ANT_GOAL){b.over=true;b.winner=0;b.pendingNext=false;return;}
    int next=AntCaveRules.spawnAntWave(b,spawned,alive);
    spawned+=AntCaveRules.count(next^alive);alive=next;
    if(b.over&&b.winner==0){b.over=false;b.winner=-1;b.pendingNext=true;}
   }
  }
  if(!b.over&&!AntCaveRules.dying(b)&&b.turn>300){b.over=true;b.winner=1;}
  if(!b.over&&b.pendingNext&&!b.busy())b.completeTurn();
 }
 void skipStage(){
  Battle b=c.game;
  if(!active||b.over||AntCaveRules.dying(b))return;
  b.ballCount=b.bursts=b.settle=0;
  if(stage==1){
   for(int i=0;i<AntCaveRules.ANTS.length;i++)b.units[AntCaveRules.ANTS[i]].hp=0;
   spawned=kills=AntCaveRules.ANT_GOAL;
   alive=0;
  }else b.units[1].hp=0;
  b.check();
  progress();
 }
 void finish(){if(settled)return;settled=true;if(c.game.winner==0){c.wins++;c.economy.offlineMaterials[4]=Math.min(999999,c.economy.offlineMaterials[4]+1);if(stage==2)c.economy.offlineMaterials[5]=Math.min(999999,c.economy.offlineMaterials[5]+1);if(stage==2&&difficulty==1)c.economy.offlineClear=true;}c.experience=(int)Math.min(2147483647L,c.experience+(c.game.winner==0?300L:100L));c.economy.save();c.saveExperience();c.saveSettings();}
}
