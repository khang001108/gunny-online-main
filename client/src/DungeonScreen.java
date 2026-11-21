import javax.microedition.lcdui.*;
/** Cooperative browser, a three-seat staging room, and its own map/difficulty picker. */
final class DungeonScreen {
    void releaseArt(){for(int i=0;i<3;i++){seatPortraits[i]=null;seatKeys[i]=null;}rewardFrame=null;previewCover=rewardWing=null;previewStage=-1;banner=background=smallCover=seatCard=emptyCard=localPortrait=allyPortrait=closedCard=null;portraitKey="";for(int i=0;i<stageCovers.length;i++)stageCovers[i]=null;}

 static final int CONFIRM_SOLO = 6;
 static final String TITLE = "Ải Viễn Chinh";
 final PhoneCanvas c;final OnlineClient n;int focus,page;boolean picker;Image banner,background;
 DungeonScreen(PhoneCanvas canvas){c=canvas;n=c.net;}
 void load(){if(banner==null)banner=c.combatRoomArt("dungeon780-banner");}
 String difficultyName(int value){return value==0?"Dễ":value==1?"Thường":"Khó";}
 private final Image[] stageCovers = new Image[2];
 private Image smallCover, seatCard, emptyCard, localPortrait;
 private int allyPortraitId = -1;
 private Image allyPortrait;
 private Image closedCard;
 private int observedRoom;
 private final String[] observedPlayers = new String[6];
 private String portraitKey = "";
 int draftDungeon;private int draftDifficulty;int pickerStep;
 private boolean confirmFocused;

 Image stageCover(int stage) {
  if(stage>=3)return c.tribalArt.cover(stage-2);
  int index = stage == 2 ? 1 : 0;
  if (stageCovers[index] == null)
   stageCovers[index] = c.combatRoomArt("dungeon-stage-" + (index + 1));
  return stageCovers[index];
 }

 Image smallCover() {
  load();
  if (smallCover == null)
   smallCover = NativeUi.fit(banner, 88*c.s, 32*c.s, 0);
  return smallCover;
 }
 private static final int PAGE_SIZE=8, FIRST_ROW=34, ROW_STEP=29, CARD_WIDTH=144, CARD_HEIGHT=28;
 private static final int TEAM=0, NEW_ROOM=1, PREV=34, NEXT=35;
 private static final int CODE=36, JOIN=37, REFRESH=38, SEARCH_Y=153;
 private static final int[] MENU={PREV,NEXT,TEAM,NEW_ROOM,CODE,JOIN,REFRESH};
 private static final int[] MENU_X={13,74,145,228}, MENU_W={55,55,72,78};
 private static final int[] SEARCH_X={13,109,149}, SEARCH_W={92,35,47};
 private Image waitingCard, playingCard, lockArt;
 private Image codeArt, enterArt, refreshArt, onlineArt;
 private final Image[] menuArt=new Image[4];
 String roomCode="";

 private int lastListPage(){
  return Math.max(0,(n.roomCount-1)/PAGE_SIZE);
 }
 private int cardsOnPage(){
  page=Math.min(page,lastListPage());
  return Math.max(0,Math.min(PAGE_SIZE,n.roomCount-page*PAGE_SIZE));
 }
 private int roomX(int slot){return 13+slot%2*150;}
 private int roomY(int slot){return FIRST_ROW+slot/2*ROW_STEP;}

 void drawList(){
  load();
  c.all();
  if(background==null)background=c.combatRoomArt("dungeon-room-browser");
  c.img(background,0,0);
  if(waitingCard==null){
   waitingCard=c.combatRoomArt("dungeon-room-available");
   playingCard=c.combatRoomArt("dungeon-room-playing");
   lockArt=c.combatRoomArt("roomlist-lock");
   Image source=c.combatRoomArt("roomlist-background");
   int[] x={6,55,105,246}, y={217,217,209,209}, w={48,48,61,66}, h={12,12,26,26};
   for(int i=0;i<menuArt.length;i++){
    Image button=Image.createImage(source,x[i]*c.s,y[i]*c.s,w[i]*c.s,h[i]*c.s,0);
    menuArt[i]=NativeUi.fit(button,MENU_W[i]*c.s,(i<2?12:22)*c.s,2*c.s);
   }
   codeArt=NativeUi.fit(c.combatRoomArt("roomlist-search"),SEARCH_W[0]*c.s,14*c.s,3*c.s);
   enterArt=Image.createImage(source,231*c.s,15*c.s,35*c.s,14*c.s,0);
   refreshArt=NativeUi.fit(Image.createImage(source,270*c.s,15*c.s,42*c.s,13*c.s,0),SEARCH_W[2]*c.s,14*c.s,2*c.s);
   onlineArt=NativeUi.fit(c.combatRoomArt("roomlist-filter"),107*c.s,14*c.s,3*c.s);
  }
  int count=cardsOnPage();
  for(int slot=0;slot<count;slot++)drawRoomCard(page*PAGE_SIZE+slot,roomX(slot),roomY(slot));
  if(n.dungeonListLoading&&count==0)c.pcCenter("Đang tải phòng viễn chinh...",160,91);
  else if(count==0)c.pcCenter("Chưa có phòng • Tạo phòng để bắt đầu",160,91);
  drawSearch();
  for(int i=0;i<menuArt.length;i++){
   int y=i<2?177:172;
   c.img(menuArt[i],MENU_X[i],y);
   if(focus==MENU[i])c.drawSelectionGlow(MENU_X[i]-1,y-1,MENU_W[i]+2,(i<2?14:24));
  }
  n.rooms.drawToolbar(focus);
  n.rooms.drawToolTip(focus);
 }

 private void drawSearch(){
  c.img(codeArt,SEARCH_X[0],SEARCH_Y);
  c.pcText(roomCode.length()==0?"Nhập số phòng":roomCode,SEARCH_X[0]+13,SEARCH_Y+2);
  c.img(enterArt,SEARCH_X[1],SEARCH_Y);
  c.img(refreshArt,SEARCH_X[2],SEARCH_Y);
  c.img(onlineArt,200,SEARCH_Y);
  c.rect(204,SEARCH_Y+2,99,10,0x68431f);
  c.pcCenter("Online: "+(n.presenceCap?""+n.onlineCount:"—"),253,SEARCH_Y+2);
  if(focus>=CODE&&focus<=REFRESH){
   int i=focus-CODE;
   c.drawSelectionGlow(SEARCH_X[i]-1,SEARCH_Y-1,SEARCH_W[i]+2,16);
  }
 }

 private void drawRoomCard(int i,int x,int y){
  boolean playing=n.roomPhases[i]!=0;
  c.img(playing?playingCard:waitingCard,x,y);
  c.pcText(n.roomTribal[i]?"Bộ Lạc Tà Thần":"Huyệt Ma Kiến",x+5,y+2);
  if(n.roomLocked[i])c.img(lockArt,x+87,y+2);
  c.pcText("Số phòng "+n.roomIds[i],x+5,y+14);
  c.pcCenter(n.roomCounts[i]+"/3",x+128,y+14);
  if(focus==i+2)c.drawSelectionGlow(x-1,y-1,CARD_WIDTH+2,CARD_HEIGHT+2);
 }

 void keyList(int key,int dir,boolean ok){
  if(key==-7){n.returnHome();return;}
  if(dir!=0)moveList(dir);
  else if(key=='r'||key=='R')refresh();
  else if(ok||key==-6)activateList();
 }

 private void moveList(int dir){
  int count=cardsOnPage(), best=focus, score=99999;
  int x=listFocusX(focus),y=listFocusY(focus);
  int actions=count+MENU.length;
  for(int i=0;i<actions+OnlineRooms.TOOL_IDS.length;i++){
   int id=i<count?page*PAGE_SIZE+i+2:i<actions?MENU[i-count]:OnlineRooms.TOOL_IDS[i-actions];
   int dx=listFocusX(id)-x,dy=listFocusY(id)-y;
   int along=dir==1?-dx:dir==2?dx:dir==4?-dy:dy;
   int across=dir==1||dir==2?Math.abs(dy):Math.abs(dx);
   if(along>0&&along+3*across<score){score=along+3*across;best=id;}
  }
  focus=best;
 }
 private int listFocusX(int id){
  if(id>=41)return n.rooms.focusX(id);
  if(id>=CODE&&id<=REFRESH)return SEARCH_X[id-CODE]+SEARCH_W[id-CODE]/2;
  for(int i=0;i<menuArt.length;i++)if(id==MENU[i])return MENU_X[i]+MENU_W[i]/2;
  return roomX((id-2)%PAGE_SIZE)+CARD_WIDTH/2;
 }
 private int listFocusY(int id){
  if(id>=41)return n.rooms.focusY(id);
  if(id>=CODE&&id<=REFRESH)return SEARCH_Y+7;
  for(int i=0;i<menuArt.length;i++)if(id==MENU[i])return 183;
  return roomY((id-2)%PAGE_SIZE)+CARD_HEIGHT/2;
 }
 private void refresh(){
  if(!n.operation.equals("dungeonrooms")&&!n.pending.equals("dungeonrooms")&&n.request("dungeonrooms"))
   n.dungeonListLoading=true;
 }

 private void joinByCode(){
  if(roomCode.length()==0){n.rooms.code();return;}
  try{
   int id=Integer.parseInt(roomCode);
   if(id<=0)throw new NumberFormatException();
   for(int i=0;i<n.roomCount;i++)if(n.roomIds[i]==id){
    joinRoom(i);
    return;
   }
   c.popup.notice(TITLE,"Không tìm thấy phòng viễn chinh. Chọn Tạo mới để cập nhật danh sách.");
  }catch(NumberFormatException invalid){
   c.popup.notice(TITLE,"Nhập mã phòng hợp lệ.");
  }
 }

 void activateList(){
  if(focus>=41){n.rooms.activateTool(focus);return;}
  if(focus==CODE){n.rooms.code();return;}
  if(focus==REFRESH){refresh();return;}
  if(n.dungeonListLoading)return;
  if(focus==JOIN){joinByCode();return;}
  if(focus==NEW_ROOM){
   n.create.openDungeon();return;
  }
  if(focus==TEAM){
   if(c.requireWeapon())return;
   if(!n.operation.equals("dungeonteam"))n.request("dungeonteam");
   return;
  }
  if(focus==PREV||focus==NEXT){
   page=Battle.clamp(page+(focus==PREV?-1:1),0,lastListPage());return;
  }
  int i=focus-2;
  if(i<0||i>=n.roomCount)return;
  joinRoom(i);
 }

 private void joinRoom(int i){
  if(n.roomPhases[i]!=0||n.roomPlayers[i]>=n.roomSeats[i]){
   c.popup.notice(DungeonScreen.TITLE,n.roomPhases[i]!=0?"Phòng đang chơi.":"Phòng đã đủ người.");
   return;
  }
  n.joinId=n.roomIds[i];n.joinTeam=0;n.joinSecret="";
  if(n.roomLocked[i])n.rooms.join(n.roomIds[i]);
  else if(!n.operation.equals("join"))n.request("join");
 }

 void pointerList(int x,int y){
  int count=cardsOnPage();
  for(int slot=0;slot<count;slot++){
   int left=roomX(slot),top=roomY(slot);
   if(x>=left&&x<left+CARD_WIDTH&&y>=top&&y<top+CARD_HEIGHT){
    focus=page*PAGE_SIZE+slot+2;activateList();return;
   }
  }
  if(n.rooms.pressToolbar(x,y))return;
  if(y>=SEARCH_Y&&y<SEARCH_Y+15)for(int i=0;i<SEARCH_X.length;i++){
   if(x>=SEARCH_X[i]&&x<SEARCH_X[i]+SEARCH_W[i]){focus=CODE+i;activateList();return;}
  }
  if(y>=170&&y<195)for(int i=0;i<menuArt.length;i++){
   if(x>=MENU_X[i]&&x<MENU_X[i]+MENU_W[i]){focus=MENU[i];activateList();return;}
  }
 }
 void drawWaiting() {
  boolean sameRoom = observedRoom == n.roomId;
  observedRoom = n.roomId;
  for (int actor = 0; actor < 6; actor += 2) {
   String name = n.present[actor] ? n.names[actor] : null;
   if (sameRoom && n.phase == 0 && actor != n.myActor && name != null
       && !name.equals(observedPlayers[actor]))
    c.message(name + " đã tham gia phòng");
   observedPlayers[actor] = name;
  }
  drawRoom(true);
  if (picker) drawPicker();
  if (c.toastTicks > 0) c.drawBattleNotice();
 }

 void drawRoom(boolean online) {
  c.loadRoomArt();
  c.loadout.load();
  c.all();
  c.img(c.loadout.background, 0, 0);
  c.wallet(8, 4);
  if (seatCard == null) {
   seatCard = NativeUi.fit(c.loadout.card, 82*c.s, 68*c.s, 5*c.s);
   emptyCard = NativeUi.fit(c.loadout.waiting, 82*c.s, 68*c.s, 5*c.s);
   closedCard = NativeUi.fit(c.loadout.closed, 82*c.s, 68*c.s, 5*c.s);
  }
  for (int seat = 0; seat < 3; seat++)
   drawSeat(seat, online);

  int stage = online ? n.dungeonStage : c.offlineDungeon.stage;
  if(previewStage!=stage||previewCover==null){previewStage=stage;previewCover=NativeUi.fit(stageCover(stage),82*c.s,68*c.s,0);}
  c.img(previewCover,120,99);
  c.loadout.draw(online, true);
  drawRewards(stage,online?n.dungeonDifficulty:c.offlineDungeon.difficulty);
 }

 final Image[] seatPortraits=new Image[3];final String[] seatKeys=new String[3];Image rewardFrame;
 Image previewCover;int previewStage=-1;
 Image rewardWing;
 void drawRewards(int stage,int diff){if(rewardFrame==null)rewardFrame=NativeUi.fit(c.combatRoomArt("item752-tooltip"),204*c.s,27*c.s,4*c.s);c.img(rewardFrame,6,168);boolean tribal=stage>=3,finish=stage==2||stage==6;int factor=tribal&&stage==6?(diff==2?11:diff==1?15:18):18;for(int g=0;g<5;g++){int x=8+g*25;c.img(c.stoneShop.icon(g),x,169);int p=(StoneDrops.WEIGHTS[diff][g]*factor*10+9)/18;c.pcText(p/10+"."+p%10+"%",x,183);}int x=133;if(tribal&&stage==6&&diff>0){c.img(c.weapons[8],x,170);c.pcText(diff==2?"33.3%":"16.7%",x,183);x+=25;if(diff==2){if(rewardWing==null)rewardWing=NativeUi.fit(c.combatRoomArt("tribal-wing"),23*c.s,19*c.s,0);c.img(rewardWing,x,170);c.pcText("5.6%",x,183);x+=25;}}if(finish){c.img(c.gifts.icon(1),x,169);c.pcText("100%",x,183);}}
 private void drawSeat(int seat, boolean online) {
  int actor = seat * 2;
  int x = 18 + (seat % 2) * 102;
  int y = 28 + (seat / 2) * 71;
  int bot = c.offlineDungeon.allyAt(actor);
  boolean occupied = online ? n.present[actor] : seat == 0 || bot >= 0;
  boolean closed = ((online ? n.closedSlots : c.offlineDungeon.closedSlots) & (1 << actor)) != 0;
  c.img(occupied ? seatCard : closed ? closedCard : emptyCard, x, y);
  if (!occupied) return;
  if (!online && seat != 0) {
   if (allyPortraitId != bot) {
    allyPortraitId = bot;
    try {
     allyPortrait = c.standingPreview(c.botSex(bot),c.botSkin(bot),false,29,39);
    } catch (java.io.IOException ignored) {
     allyPortrait = null;
    }
   }
   c.pcText(c.pcShort(PhoneCanvas.BOT_NAMES[bot],72), x+4, y+4);
   c.clip(x+2,y+14,78,48);c.img(allyPortrait,x+25,y+17);c.all();
   c.img(c.weapons[c.botWeapon(bot)],x+7,y+43);
   c.drawLevel(Progression.botExperience(bot),x+54,y+39);
   return;
  }
  c.pcText(c.pcShort(online ? n.names[actor] : c.playerName, 72), x+4, y+4);
  if (online) {
   try{if(seatPortraits[seat]==null||n.standingKeys[actor]!=null&&!n.standingKeys[actor].equals(seatKeys[seat])){seatPortraits[seat]=n.buildStanding(actor,29,39);seatKeys[seat]=n.standingKeys[actor];}Image person=seatPortraits[seat];c.clip(x+2,y+14,78,48);c.vip.back(n.actorVip[actor],n.actorWing[actor],x+40,y+56,1,false);c.img(person,x+25,y+17);c.all();}catch(java.io.IOException ignored){}
   n.chat.roomSpeech.head(actor,x+40,y+26);c.drawLevel(n.actorExperience[actor],x+54,y+39);
  } else {
   String key = c.gender + ":" + c.skin + ":" + c.wornMask + ":" + c.customLook;
   for (int i = 0; i < c.look.length; i++) key += ":" + c.look[i];
   if (!key.equals(portraitKey)) {
    portraitKey = key;
    try {
     localPortrait = c.equipmentPreview(c.gender,c.skin,c.customLook,c.look,c.wornMask,29,39);
    } catch (java.io.IOException ignored) {
     localPortrait = null;
    }
   }
   c.clip(x+2,y+14,78,48);c.vip.back(c.vip.exp(),c.vip.wing(),x+40,y+56,1,false);c.img(localPortrait,x+25,y+17);c.all();
   n.chat.roomSpeech.head(0,x+48,y+27);
   c.drawLevel(c.experience, x+54, y+39);
  }
  if (online ? (n.wornMasks[actor]&1)!=0 : (c.wornMask&1)!=0)
   c.img(c.weapons[online ? n.weapon[actor] : c.equipped], x+7, y+43);
  if(online&&n.rewardWaitingMask!=0){c.pcCenter((n.rewardWaitingMask&(1<<actor))!=0?"Nhận thưởng":"Đã xong",x+41,y+57);return;}
  if (!online || actor == n.hostActor)
   c.img(c.roomHost, x+4, y+55);
  else if ((n.readyMask & (1 << actor)) != 0)
   c.pcCenter("Sẵn sàng", x+41, y+57);
 }

 void openPicker(boolean online) {

  if (online && (n.inBattle || !n.host || n.phase != 0)) {
   c.message("Chỉ chủ phòng được chọn ải viễn chinh trước trận");
   return;
  }
  draftDungeon=online&&n.dungeonStage>=3?1:0;
  draftDifficulty = online ? n.dungeonDifficulty : c.offlineDungeon.difficulty;
  pickerStep=0;confirmFocused = false;
  setPicker(online, true);
 }

 private void setPicker(boolean online, boolean value) {
  if (online) picker = value;
  else c.offlineDungeon.picker = value;
 }

 void drawPicker() {
  c.maps.drawDungeon(draftDifficulty, confirmFocused);
  c.pcCenter(pickerStep==0?"Trái/phải chọn ải • OK: mức độ":confirmFocused?"OK: xác nhận":"Trái/phải mức độ • OK: tiếp",160,226);
  if(c.onlineAccount()){c.ui.button(draftDungeon==1?"Bộ Lạc Tà Thần":"Huyệt Ma Kiến",25,195,150,23,false);c.pcCenter("1: Kiến • 2: Tà Thần",99,183);}
 }

 private void confirmPicker(boolean online) {
  if (draftDungeon==0&&draftDifficulty == 2 && !c.economy.clear()) {
   c.maps.notice("Vượt Thường để mở Khó");
   return;
  }
  if (online) {
   if (n.inBattle || !n.host || n.phase != 0) {
    setPicker(true, false);
    c.message("Chỉ chủ phòng được chọn ải viễn chinh trước trận");
    return;
   }
   if (n.pending.length() > 0 || n.operation.equals("dungeonsettings")) return;
   n.dungeonDifficulty = draftDifficulty;
   n.request("dungeonsettings");
  } else {
   c.offlineDungeon.difficulty = draftDifficulty;
  }
  setPicker(online, false);
 }

 void keyWaiting(int key, int dir, boolean ok) {
  keyRoom(true, key, dir, ok);
 }

 void keyRoom(boolean online, int key, int dir, boolean ok) {
  if (online ? picker : c.offlineDungeon.picker) {
   if(online&&(key=='1'||key=='2')){draftDungeon=key-'1';return;}
   if(pickerStep==0){if(key==-7)setPicker(online,false);else if(dir==1||dir==2){if(online)draftDungeon=1-draftDungeon;}else if(ok||key==-6)pickerStep=1;return;}
   if (key == -7){pickerStep=0;confirmFocused=false;}
   else if (dir == 1 || dir == 2) {
    draftDifficulty = (draftDifficulty + (dir == 1 ? 2 : 1)) % 3;
    confirmFocused = false;
   } else if (dir == 4 || dir == 8) {
    confirmFocused = dir == 8;
   } else if (ok || key == -6) {
    if (confirmFocused) confirmPicker(online);
    else confirmFocused = true;
   }
   return;
  }
  if (key == -7) {
   activateRoom(online, 5);
  } else if (key == -6) {
   activateRoom(online, 2);
  } else if (dir != 0) {
   c.loadout.move(dir, online, true);
  } else if (ok) {
   activateRoom(online, c.roomSelection);
  }
 }

 private void activateRoom(boolean online, int action) {
  c.roomSelection = action;
  if (action == 4 || action == 29) {
   openPicker(online);
  } else if (action == 2) {
   if (online) {
    n.inviteSeat = -1;
    n.social.open(true);
   } else {
    n.social.openOffline();
   }
  } else if (action == 5) {
   if (online && n.inBattle) c.mode = c.game.over ? 5 : 1;
   else if (!online || n.host) start(online, false);
   else {
    n.readyDraft = (n.readyMask & (1 << n.myActor)) == 0;
    n.request("ready");
   }
  } else {
   c.loadout.activate(online);
  }
 }

 private int soloRoom;

 void start(boolean online, boolean confirmed) {
  if (online != c.onlineAccount()) return;
  if (online && (n.roomId <= 0 || n.roomKind != 3 || !n.host || n.phase != 0)) return;
  if(online&&n.rewardWaitingMask!=0){c.message("Chờ đồng đội nhận thưởng và về phòng");return;}
  if (Progression.level(c.experience) < 15) {
   c.popup.notice(DungeonScreen.TITLE, "Ải Viễn Chinh yêu cầu Lv 15 trở lên");
   return;
  }
  int difficulty = online ? n.dungeonDifficulty : c.offlineDungeon.difficulty;
  if ((!online||n.dungeonStage<3)&&difficulty == 2 && !c.economy.clear()) {
   c.popup.notice(DungeonScreen.TITLE, "Vượt Thường để mở Khó");
   return;
  }
  if (c.requireWeapon()) return;
  boolean solo = online ? !n.present[2] && !n.present[4]
                        : c.offlineDungeon.allyAt(2) < 0 && c.offlineDungeon.allyAt(4) < 0;
  if (solo && !confirmed && !c.presentation.skipSoloDungeonHint) {
   soloRoom = online ? n.roomId : 0;
   c.popup.open("Gợi ý", "Ải Viễn Chinh phù hợp với tổ đội từ 2 người. Bạn muốn tự mình vượt ải?",
       new String[]{"Đồng ý", "Hủy"}, CONFIRM_SOLO);
   return;
  }
  if (online) n.request("start");
  else c.offlineDungeon.begin();
 }

 void confirmSolo(boolean skipHint) {
  if (soloRoom == 0 ? c.mode != 19 || c.onlineAccount() : n.roomId != soloRoom || c.mode != 16) return;
  if (skipHint) {
   c.presentation.skipSoloDungeonHint = true;
   c.presentation.save();
  }
  start(soloRoom != 0, true);
 }

 void pointerWaiting(int x, int y) {
  pointerRoom(true, x, y);
 }

 void inviteOfflineBot(int bot) {
  OfflineDungeon room = c.offlineDungeon;
  for (int i=0; i<room.allies.length; i++)
   if (room.allies[i]==bot) return;
  for (int i=0; i<room.allies.length; i++) {
   int seat=2+i*2;
   if (room.allies[i]<0 && (room.closedSlots & (1<<seat))==0) {
    room.allies[i]=bot;
    c.message(PhoneCanvas.BOT_NAMES[bot] + " đã tham gia phòng");
    return;
   }
  }
  c.message("Không còn ô trống để mời");
 }

 private void toggleSeat(boolean online, int actor) {
  if (online) {
   if (!n.host || n.phase != 0 || n.present[actor] && !n.bots[actor]) return;
   n.slotDraft = actor;
   n.request("slot");
  } else {
   OfflineDungeon room = c.offlineDungeon;
   room.allies[actor/2-1] = -1;
   room.closedSlots ^= 1 << actor;
  }
 }

 void pointerRoom(boolean online, int x, int y) {
  if (online ? picker : c.offlineDungeon.picker) {
   if(online&&x>=25&&x<180&&y>=29&&y<222){draftDungeon=1-draftDungeon;pickerStep=0;return;}
   if (x >= 297 && y < 25) setPicker(online, false);
   else if (c.maps.hitConfirm(x,y)) confirmPicker(online);
   else if (x >= 212 && x < 311 && y >= 53 && y < 74) {
    draftDifficulty = (x - 212) / 33;
    confirmFocused = false;
   }
   return;
  }
  if (c.loadout.pressToolbar(x,y)) return;
  if(x>=120&&x<202&&y>=99&&y<167){openPicker(online);return;}
  if (x >= 120 && x < 202 && y >= 28 && y < 96) {
   toggleSeat(online, 2);
   return;
  }
  if (x >= 18 && x < 100 && y >= 99 && y < 167) {
   toggleSeat(online, 4);
   return;
  }
  int action = c.loadout.hit(x,y);
  if (action >= 0) activateRoom(online, action);
 }
}
