import javax.microedition.lcdui.*;
/** Cooperative browser, a three-seat staging room, and its own map/difficulty picker. */
final class DungeonScreen {
    void releaseArt(){closeRewards();tribalWaitingCard=tribalPlayingCard=null;for(int i=0;i<8;i++)largeRewardIcons[i]=null;for(int i=0;i<3;i++){seatPortraits[i]=null;seatKeys[i]=null;}rewardFrame=pickerPanel=rewardCell=portraitFrame=rewardPopupFrame=rewardPopupClose=rewardPopupCell=null;for(int i=0;i<8;i++)rewardIcons[i]=null;for(int i=0;i<2;i++)pickerCards[i]=portraitCards[i]=null;previewCover=rewardWing=null;previewStage=-1;banner=background=smallCover=seatCard=emptyCard=localPortrait=allyPortrait=closedCard=null;portraitKey="";for(int i=0;i<stageCovers.length;i++)stageCovers[i]=null;}

 static final int CONFIRM_SOLO = 6, CONFIRM_LEAVE_CAMPAIGN = 35;
 static final String TITLE = "Ải Viễn Chinh";
 final PhoneCanvas c;final OnlineClient n;int focus,page;boolean picker;Image banner,background;
 DungeonScreen(PhoneCanvas canvas){c=canvas;n=c.net;}
 void load(){if(banner==null)banner=c.combatRoomArt("dungeon780-banner");}
 int minimumLevel(int family){return family==1?TribalRules.MIN_LEVEL:15;}
 boolean levelAllowed(int family){int level=Progression.level(c.experience),need=minimumLevel(family);if(level>=need)return true;c.popup.notice("Chưa đủ cấp",(family==1?"Bộ Lạc Tà Thần":"Huyệt Ma Kiến")+" yêu cầu Lv "+need+". Nhân vật hiện ở Lv "+level+".");return false;}
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
 // List texture spans Y=49..167 (118px): 3 rows of 28px with balanced margins.
 // 6 cards/page = 2 columns x 3 rows, Y=55,94,133; top/bottom=6px.
 private static final int PAGE_SIZE=6, FIRST_ROW=55, ROW_STEP=39, CARD_WIDTH=144, CARD_HEIGHT=28;
 private static final int TEAM=0, NEW_ROOM=1, PREV=34, NEXT=35;
 private static final int CODE=36, JOIN=37, REFRESH=38, SEARCH_Y=35;
 private static final int[] MENU={PREV,NEXT,TEAM,NEW_ROOM,CODE,JOIN,REFRESH};
 private static final int[] MENU_X={13,74,143,235}, MENU_W={55,55,61,66};
 private static final int[] SEARCH_X={13,109,149}, SEARCH_W={92,35,47};
 private Image waitingCard, playingCard, lockArt;
 private Image tribalWaitingCard, tribalPlayingCard;
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
  // Free 14 px under the fixed header for the moved search toolbar.
  // Move only the gray list texture, not the decorative outer frame.
  // This preserves the original pixel ratio and trims 14 px from the empty
  // dark area above the bottom buttons (E72: 320x240, E6: scaled by c.s).
  c.part(background,7,153,306,14,7,35,false); // dark strip behind toolbar
  c.part(background,7,35,306,118,7,49,false); // gray room rows down 14px
  c.part(background,8,23,42,15,8,23,false); // preserve "Danh sach phong" tab
  if(waitingCard==null){
   waitingCard=c.combatRoomArt("dungeon-room-available");
   playingCard=c.combatRoomArt("dungeon-room-playing");
   lockArt=c.combatRoomArt("roomlist-lock");
   Image source=c.combatRoomArt("roomlist-background");
   int[] x={6,55,105,246}, y={217,217,209,209}, w={48,48,61,66}, h={12,12,26,26};
   for(int i=0;i<menuArt.length;i++){
    Image button=Image.createImage(source,x[i]*c.s,y[i]*c.s,w[i]*c.s,h[i]*c.s,0);
    if(i<2){
        menuArt[i]=NativeUi.fit(
            button,
            MENU_W[i]*c.s,
            12*c.s,
            2*c.s
        );
    }else{
        // Giữ nguyên ảnh nút PvP
        menuArt[i]=button;
    }
   }
   codeArt=NativeUi.fit(c.combatRoomArt("roomlist-search"),SEARCH_W[0]*c.s,14*c.s,3*c.s);
   enterArt=Image.createImage(source,231*c.s,15*c.s,35*c.s,14*c.s,0);
   refreshArt=NativeUi.fit(Image.createImage(source,270*c.s,15*c.s,42*c.s,13*c.s,0),SEARCH_W[2]*c.s,14*c.s,2*c.s);
   onlineArt=NativeUi.fit(c.combatRoomArt("roomlist-filter"),107*c.s,14*c.s,3*c.s);
  }
  int count=cardsOnPage();
  for(int slot=0;slot<count;slot++)drawRoomCard(page*PAGE_SIZE+slot,roomX(slot),roomY(slot));
  if(n.dungeonListLoading&&count==0)c.pcCenter("Đang tải phòng viễn chinh...",160,106);
  else if(count==0)c.pcCenter("Chưa có phòng • Tạo phòng để bắt đầu",160,106);
  drawSearch();
  for(int i=0;i<menuArt.length;i++){
   int y=i<2?177:167;
   c.img(menuArt[i],MENU_X[i],y);
   if(focus==MENU[i])c.drawSelectionGlow(MENU_X[i]-1,y-1,MENU_W[i]+2,(i<2?14:28));
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
  boolean tribal=n.roomTribal[i];
  Image card=playing?playingCard:waitingCard;
  if(tribal){
   if(playing){
    if(tribalPlayingCard==null)tribalPlayingCard=c.combatRoomArt("dungeon-room-tribal-playing");
    if(tribalPlayingCard!=null)card=tribalPlayingCard;
   }else{
    if(tribalWaitingCard==null)tribalWaitingCard=c.combatRoomArt("dungeon-room-tribal-available");
    if(tribalWaitingCard!=null)card=tribalWaitingCard;
   }
  }
  c.img(card,x,y);
  c.pcText(tribal?"Bộ Lạc Tà Thần":"Huyệt Ma Kiến",x+5,y+2);
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
  for(int i=0;i<menuArt.length;i++)if(id==MENU[i])return i<2?183:180;
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
  // Hitbox follows each button after its Y position changes.
  for(int i=0;i<menuArt.length;i++){
   int top=i<2?177:167, height=i<2?12:26;
   if(x>=MENU_X[i]&&x<MENU_X[i]+MENU_W[i]&&y>=top&&y<top+height){
    focus=MENU[i];activateList();return;
   }
  }
 }
 void drawWaiting() {
  boolean sameRoom = observedRoom == n.roomId;
  if(!sameRoom||n.phase!=0||(rewardsOpen&&(rewardStage!=n.dungeonStage||rewardDifficulty!=n.dungeonDifficulty)))closeRewards();
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
  else if(rewardsOpen)drawRewardPopup();

 }

 void drawRoom(boolean online) {
  c.loadRoomArt();
  c.loadout.load();
  c.all();
  c.img(c.loadout.background, 0, 0);
  c.wallet(8, 4);
  if (seatCard == null) {
   seatCard = NativeUi.fit(c.loadout.card, 82*c.s, 80*c.s, 5*c.s);
   emptyCard = NativeUi.fit(c.loadout.waiting, 82*c.s, 80*c.s, 5*c.s); // Same waiting art as Online
   closedCard = NativeUi.fit(c.loadout.closed, 82*c.s, 80*c.s, 5*c.s);
  }
  for (int seat = 0; seat < 3; seat++) {
   drawSeat(seat, online);
   if(c.roomSelection==60+seat*2)c.drawSelectionGlow(17+seat%2*102,27+seat/2*83,84,82);
  }

  int stage = online ? n.dungeonStage : c.offlineDungeon.stage;
  // Reward preview is room content, not a floating overlay. Draw before toolbar/tooltips.
  drawRewards(stage,online?n.dungeonDifficulty:c.offlineDungeon.difficulty);
  if(c.roomSelection==DungeonRewardPreview.FOCUS)c.drawSelectionGlow(119,110,84,82);
  c.loadout.draw(online, true);

 }

 final Image[] seatPortraits=new Image[3];final String[] seatKeys=new String[3];Image rewardFrame;
 Image previewCover;int previewStage=-1;
 Image rewardWing;
 Image rewardCell;final Image[] rewardIcons=new Image[8];
 void rewardItem(int index,Image icon,String percent){int x=125+index%3*24,y=116+index/3*23;c.clip(x,y,23,23);c.img(icon,x+4,y+1);c.maps.smallText(percent,x+11,y+15);c.all();}
 boolean rewardsOpen,rewardInfo;int rewardFocus,rewardStage,rewardDifficulty;
 final Image[] largeRewardIcons=new Image[8];
 Image rewardPopupFrame,rewardPopupClose,rewardPopupCell;int rewardPopupCellW;
 Image rewardSource(int item){if(item<5)return c.stoneShop.icon(item);if(item==DungeonRewardPreview.WEAPON)return c.weapons[8];if(item==DungeonRewardPreview.WING)return c.combatRoomArt("tribal-wing");return c.gifts.icon(1);}
 void drawRewards(int stage,int diff){
  if(rewardFrame==null)rewardFrame=NativeUi.fit(c.combatRoomArt("item752-tooltip"),82*c.s,80*c.s,5*c.s);
  c.img(rewardFrame,120,111);
  int count=DungeonRewardPreview.count(stage,diff);
  for(int i=0;i<count;i++){int item=DungeonRewardPreview.item(stage,diff,i);if(rewardIcons[item]==null)rewardIcons[item]=NativeUi.fit(rewardSource(item),14*c.s,14*c.s,0);rewardItem(i,rewardIcons[item],DungeonRewardPreview.percent(stage,diff,item));}
 }
 void closeRewards(){rewardsOpen=rewardInfo=false;rewardFocus=0;}
 void openRewards(boolean online){rewardStage=online?n.dungeonStage:c.offlineDungeon.stage;rewardDifficulty=online?n.dungeonDifficulty:c.offlineDungeon.difficulty;rewardFocus=0;rewardInfo=false;rewardsOpen=true;c.roomSelection=DungeonRewardPreview.FOCUS;}
 int rewardCount(){return DungeonRewardPreview.count(rewardStage,rewardDifficulty);}
 Image largeRewardIcon(int item){if(largeRewardIcons[item]==null){Image src=rewardSource(item);int w=src.getWidth(),h=src.getHeight(),limit=40*c.s;largeRewardIcons[item]=NativeUi.fit(src,Math.max(1,w*limit/Math.max(w,h)),Math.max(1,h*limit/Math.max(w,h)),0);}return largeRewardIcons[item];}
 void drawRewardPopup(){
  c.all();
  if(rewardPopupFrame==null){rewardPopupFrame=NativeUi.popupFrame(c.combatRoomArt("settings743-popup"),DungeonRewardPreview.PANEL_W,DungeonRewardPreview.PANEL_H,c.s);rewardPopupClose=c.combatRoomArt("bag715-close");}
  c.img(rewardPopupFrame,DungeonRewardPreview.PANEL_X,DungeonRewardPreview.PANEL_Y);
  c.center("Quà thưởng",160,DungeonRewardPreview.PANEL_Y+8);
  int count=rewardCount(),w=DungeonRewardPreview.width(count);
  if(rewardPopupCell==null||rewardPopupCellW!=w){rewardPopupCellW=w;rewardPopupCell=NativeUi.fit(c.combatRoomArt("bag715-cell"),w*c.s,72*c.s,4*c.s);}
  for(int i=0;i<count;i++){
   int item=DungeonRewardPreview.item(rewardStage,rewardDifficulty,i),x=DungeonRewardPreview.x(count,i),y=DungeonRewardPreview.y(count,i);
   c.img(rewardPopupCell,x,y);
   Image icon=largeRewardIcon(item);c.img(icon,x+(w-icon.getWidth()/c.s)/2,y+5+(40-icon.getHeight()/c.s)/2);
   c.center(DungeonRewardPreview.percent(rewardStage,rewardDifficulty,item),x+w/2,y+51);
   if(i==rewardFocus)c.drawItemSelection(x,y,w,72,rewardInfo);
  }
  if(rewardInfo){
   int item=DungeonRewardPreview.item(rewardStage,rewardDifficulty,rewardFocus);int[] anchor={DungeonRewardPreview.x(count,rewardFocus),DungeonRewardPreview.y(count,rewardFocus),w,72};
   if(item<5)c.stoneShop.drawInfo(item,anchor);
   else if(item==DungeonRewardPreview.WEAPON)c.tooltip.draw(0,8,anchor);
   else if(item==DungeonRewardPreview.WING)c.tooltip.goods(c.vip.wingName(5),"Cánh","Cánh thưởng phó bản. Trang bị vĩnh viễn.",anchor);
   else c.tooltip.goods(c.gifts.name(1),"Quà",c.gifts.dungeonDescription(),anchor);
  }
  c.all();c.img(rewardPopupClose,DungeonRewardPreview.CLOSE_X,DungeonRewardPreview.CLOSE_Y);
 }
 void keyRewards(int key,int dir,boolean ok){if(key==-7){if(rewardInfo)rewardInfo=false;else closeRewards();return;}if(key==-6||ok){rewardInfo=!rewardInfo;return;}if(dir!=0){rewardInfo=false;rewardFocus=DungeonRewardPreview.move(rewardFocus,rewardCount(),dir);}}
 void pointerRewards(int x,int y){if(DungeonRewardPreview.closeHit(x,y)){closeRewards();return;}int hit=DungeonRewardPreview.hit(rewardCount(),x,y);if(hit>=0){if(hit==rewardFocus)rewardInfo=!rewardInfo;else{rewardFocus=hit;rewardInfo=false;}}}
 private void drawSeat(int seat, boolean online) {
  int actor = seat * 2;
  int x = 18 + (seat % 2) * 102;
  int y = 28 + (seat / 2) * 83;
  int bot = c.offlineDungeon.allyAt(actor);
  boolean occupied = online ? n.present[actor] : seat == 0 || bot >= 0;
  boolean closed = ((online ? n.closedSlots : c.offlineDungeon.closedSlots) & (1 << actor)) != 0;

  // The same waiting/closed/card assets as the regular online room, fitted to
  // the 82x80 dungeon slots. Keep all three slots and the rewards panel.
  c.img(occupied ? seatCard : closed ? closedCard : emptyCard, x, y);
  if (!occupied) return;

  // All coordinates below are relative to the top-left of the smaller card.
  // Keep the name in the header, put the level beside the head, and reserve
  // the bottom row for the host/ally label (not over the weapon or the feet).
  final int avatarX = x + 27, avatarY = y + 20;
  final int ringX = x + 42, feetY = y + 62;
  final int levelX = x + 52, levelY = y + 13;
  final int weaponX = x + 2, weaponY = y + 44;
  final int labelY = y + 69;
  final int portraitClipX = x + 2, portraitClipY = y + 13;
  final int portraitClipW = 78, portraitClipH = 57;

  if (!online && seat != 0) {
   if (allyPortraitId != bot) {
    allyPortraitId = bot;
    try {
     allyPortrait = c.standingPreview(c.botSex(bot),c.botSkin(bot),false,29,39);
    } catch (java.io.IOException ignored) {
     allyPortrait = null;
    }
   }
   c.pcText(c.pcShort(PhoneCanvas.BOT_NAMES[bot], 72), x + 5, y + 4);
   c.clip(portraitClipX,portraitClipY,portraitClipW,portraitClipH);
   c.img(allyPortrait,avatarX,avatarY);
   c.all();
   c.img(c.weapons[c.botWeapon(bot)],weaponX,weaponY);
   c.drawLevel(Progression.botExperience(bot),levelX,levelY);
   //c.pcCenter("Đồng minh",x+41,labelY);
   return;
  }

  c.pcText(c.pcShort(online ? n.names[actor] : c.playerName, 72), x + 5, y + 0);
  if (online) {
   try {
    if (seatPortraits[seat] == null || n.standingKeys[actor] != null
        && !n.standingKeys[actor].equals(seatKeys[seat])) {
     seatPortraits[seat] = n.buildStanding(actor,29,39);
     seatKeys[seat] = n.standingKeys[actor];
    }
    c.clip(portraitClipX,portraitClipY,portraitClipW,portraitClipH);
    c.vip.back(n.actorVip[actor],n.actorWing[actor],ringX,feetY,1,false);
    c.img(seatPortraits[seat],avatarX,avatarY);
   } catch (java.io.IOException ignored) {
   } finally {
    // Avoid leaking the slot clip into the following badge and other slots.
    c.all();
   }
   n.chat.roomSpeech.head(actor,x+42,y+26);
   c.drawLevel(n.actorExperience[actor],levelX,levelY);
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
   c.clip(portraitClipX,portraitClipY,portraitClipW,portraitClipH);
   c.vip.back(c.vip.exp(),c.vip.wing(),ringX,feetY,1,false);
   c.img(localPortrait,avatarX,avatarY);
   c.all();
   n.chat.roomSpeech.head(0,x+42,y+26);
   c.drawLevel(c.experience,levelX,levelY);
  }

  if (online ? (n.wornMasks[actor] & 1) != 0 : (c.wornMask & 1) != 0)
   c.img(c.weapons[online ? n.weapon[actor] : c.equipped],weaponX,weaponY);

  if (online && n.rewardWaitingMask != 0) {
   c.pcCenter((n.rewardWaitingMask & (1 << actor)) != 0 ? "Nhận thưởng" : "Đã xong",x+41,labelY);
  } else if (!online || actor == n.hostActor) {
   c.img(c.roomHost,x+4,labelY);
  } else {
   // Match the regular online room: ready is a diagonal stamp, not a text
   // overlay on the character; keep the ally label on the bottom row.
   if ((n.readyMask & (1 << actor)) != 0)
    c.img(c.roomReady,x+12,y+27);
   //c.pcCenter("Đồng minh",x+41,labelY);
  }
 }

 void openPicker(boolean online) {

  if (online && (n.inBattle || !n.host || n.phase != 0)) {
   c.message("Chỉ chủ phòng được chọn ải viễn chinh trước trận");
   return;
  }
  if(online&&n.dungeonStage>(n.dungeonStage>=3?3:1)){c.popup.open("Chọn bản đồ","Chọn map sẽ mất tiến trình hiện tại. Tiếp tục?",new String[]{"Hủy","Tiếp tục"},34);return;}
  beginPicker(online);
 }
 void beginPicker(boolean online){
  draftDungeon=online&&n.dungeonStage>=3?1:0;
  draftDifficulty = online ? n.dungeonDifficulty : c.offlineDungeon.difficulty;
  pickerStep=0;confirmFocused = false;
  setPicker(online, true);
 }

 private void setPicker(boolean online, boolean value) {
  if (online) picker = value;
  else c.offlineDungeon.picker = value;
 }

 Image pickerPanel;final Image[] pickerCards=new Image[2];
 Image mapCover(int family){if(pickerCards[family]==null)pickerCards[family]=NativeUi.fit(family==1?c.tribalArt.smallCover():smallCover(),128*c.s,64*c.s,0);return pickerCards[family];}
 Image portraitFrame;final Image[] portraitCards=new Image[2];
 Image portraitCard(int family){if(portraitCards[family]==null)portraitCards[family]=NativeUi.fit(c.combatRoomArt(family==0?"dungeon-picker-cover":"tribal-picker-cover"),78*c.s,98*c.s,0);return portraitCards[family];}
 void drawPicker(){c.maps.drawDungeon(draftDifficulty,pickerStep==2);}

 private void confirmPicker(boolean online) {
  if(!levelAllowed(draftDungeon))return;
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
  if(rewardsOpen){keyRewards(key,dir,ok);return;}
  if (online ? picker : c.offlineDungeon.picker) {
   if(key==-7){if(pickerStep>0)pickerStep--;else setPicker(online,false);}else if(dir==1||dir==2){if(pickerStep==0)draftDungeon=1-draftDungeon;else if(pickerStep==1)draftDifficulty=(draftDifficulty+(dir==1?2:1))%3;}else if(dir==4&&pickerStep>0)pickerStep--;else if(ok||key==-6){if(pickerStep==0&&!levelAllowed(draftDungeon))return;if(pickerStep<2)pickerStep++;else confirmPicker(online);}

   return;
  }
  if (key == -7) {
   activateRoom(online, 5);
  } else if (key == -6) {
   inviteFromSelection(online);
  } else if (dir != 0) {
   c.loadout.move(dir, online, true);
  } else if (ok) {
   activateRoom(online, c.roomSelection);
  }
 }

 private void inviteFromSelection(boolean online){
  int f=c.roomSelection;
  if(online){int seat=f-60;n.inviteSeat=(seat==2||seat==4)&&!n.present[seat]&&(n.closedSlots&(1<<seat))==0?seat:-1;n.social.open(true);}
  else n.social.openOffline();
 }

 private void activateRoom(boolean online, int action) {
  c.roomSelection = action;
  if(action==DungeonRewardPreview.FOCUS){openRewards(online);}
  else if(action==60||action==62||action==64){
   if(action!=60)toggleSeat(online,action-60);
  } else if (action == 4 || action == 29) {
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

 private int leavingRoom;
 void leaveWaiting(){
  if(n.roomId<=0||n.roomKind!=3||n.phase!=0)return;
  if(n.dungeonStage==2||n.dungeonStage>3){
   leavingRoom=n.roomId;
   c.popup.open("Rời phòng ải viễn chinh", "Trở về danh sách phòng ải viễn chinh? Tiến trình vượt ải sẽ bị mất.",new String[]{"Ở lại","Trở về"},CONFIRM_LEAVE_CAMPAIGN);
  }else n.request("leave");
 }
 void confirmLeaveWaiting(){
  if(c.mode==16&&n.roomKind==3&&n.phase==0&&n.roomId==leavingRoom)n.request("leave");
 }

 private int soloRoom;

 void start(boolean online, boolean confirmed) {
  if (online != c.onlineAccount()) return;
  if (online && (n.roomId <= 0 || n.roomKind != 3 || !n.host || n.phase != 0)) return;
  if(online&&n.rewardWaitingMask!=0){c.message("Chờ đồng đội nhận thưởng và về phòng");return;}
  if(!levelAllowed(online&&n.dungeonStage>=3?1:0))return;
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
  if(rewardsOpen){pointerRewards(x,y);return;}
  if (online ? picker : c.offlineDungeon.picker) {
   if(x>=14&&x<194&&y>=29&&y<205){draftDungeon=x<106?0:1;if(levelAllowed(draftDungeon))pickerStep=1;}else if(x>=212&&x<311&&y>=53&&y<74){draftDifficulty=Battle.clamp((x-213)/33,0,2);pickerStep=2;}else if(c.maps.hitConfirm(x,y)){if(pickerStep==0&&!levelAllowed(draftDungeon))return;if(pickerStep<2)pickerStep++;else confirmPicker(online);}else if(x>=295&&y<30)setPicker(online,false);

   return;
  }
  if (c.loadout.pressToolbar(x,y)) return;
  if(x>=120&&x<202&&y>=111&&y<191){openRewards(online);return;}
  for(int seat=0;seat<3;seat++){
   int sx=18+seat%2*102,sy=28+seat/2*83;
   if(x>=sx&&x<sx+82&&y>=sy&&y<sy+80){activateRoom(online,60+seat*2);return;}
  }
  int action = c.loadout.hit(x,y);
  if (action >= 0) activateRoom(online, action);
 }
}
