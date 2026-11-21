import javax.microedition.lcdui.*;
import javax.microedition.rms.RecordStore;
/** Shared native room panel and reusable, energy-costed Z/X/C loadout. */
final class RoomLoadout {
 final PhoneCanvas c;final int[] kit={0,1,2},onlineKit={0,1,2};int editSlot;
 Image squareCancel;Image squareInvite,squareStart,squareReady;Image background,panel,left,cell,selected,invite,server,start,ready,card,waiting,closed,teamCard,teamWaiting,teamClosed,chat,tooltip,randomMap;
 final Image[] icons=new Image[6],grayIcons=new Image[6];
 void releaseMenuArt(){squareCancel=squareInvite=squareStart=squareReady=background=panel=left=cell=selected=invite=server=start=ready=card=waiting=closed=teamCard=teamWaiting=teamClosed=chat=tooltip=randomMap=toolBar=null;for(int i=0;i<tools.length;i++)tools[i]=null;}
 RoomLoadout(PhoneCanvas canvas){c=canvas;try{RecordStore rs=RecordStore.openRecordStore("gunny-loadout-v2",true);if(rs.getNumRecords()>0){byte[] b=rs.getRecord(1);int[] a={b[0],b[1],b[2]};if(Battle.validKit(a))for(int i=0;i<3;i++)kit[i]=a[i];}rs.closeRecordStore();}catch(Exception ignored){}}
 void load(){if(panel!=null)return;squareInvite=c.combatRoomArt("room-invite-square");squareStart=c.combatRoomArt("match742-start");squareCancel=c.combatRoomArt("match742-cancel");squareReady=c.combatRoomArt("room-ready-square");background=art("background");panel=art("panel");left=art("left");cell=art("cell");selected=art("selected");invite=art("invite");server=art("server");start=art("start");ready=art("ready");card=art("card");waiting=art("waiting");closed=art("closed");teamCard=art("team-card");teamWaiting=art("team-waiting");teamClosed=art("team-closed");chat=art("chat");tooltip=art("tooltip");randomMap=art("map-random");}
 Image art(String name){return c.combatRoomArt("kit-"+name);}
 Image icon(int id){if(id<0||id>=6)return null;if(icons[id]==null)icons[id]=art("item"+id);return icons[id];}
 Image grayIcon(int id){if(grayIcons[id]==null)grayIcons[id]=c.combatRoomArt("kit-item"+id+"-gray");return grayIcons[id];}
 void save(){try{RecordStore rs=RecordStore.openRecordStore("gunny-loadout-v2",true);byte[] b={(byte)kit[0],(byte)kit[1],(byte)kit[2]};if(rs.getNumRecords()==0)rs.addRecord(b,0,3);else rs.setRecord(1,b,0,3);rs.closeRecordStore();}catch(Exception ignored){}}
 void choose(int id,boolean online){
  if(online&&(!c.net.itemsCap||c.net.inBattle)){c.message("Cập nhật server 7.4.4; đổi đạo cụ trước trận.");return;}
  int[] selectedKit=online?onlineKit:kit;
  int old=selectedKit[editSlot];
  if(online&&!c.net.request("loadout"))return;
  for(int i=0;i<3;i++)if(selectedKit[i]==id)selectedKit[i]=old;
  selectedKit[editSlot]=id;
  if(!online)save();
 }
 void apply(){int[] selectedKit=c.onlineAccount()?onlineKit:kit;for(int i=0;i<c.game.unitCount;i++)if(i==0||c.game.units[i].human)for(int k=0;k<3;k++)c.game.units[i].kit[k]=selectedKit[k];}
 boolean toolbarTouch;int toolbarHover=-1;Image toolBar;final Image[] tools=new Image[10];static final int[] TOOL_IDS={41,42,45,46,47},TOOL_X={26,65,104,143,182};
 void drawToolbar(boolean online){if(toolBar==null){toolBar=art("toolbar");for(int i=0;i<10;i++)tools[i]=art("tool"+(i/2)+"-"+(i%2));}int focus=toolbarTouch?toolbarHover:c.roomSelection;c.img(toolBar,0,195);for(int i=0;i<5;i++){if(!online&&i==2)continue;int tx=toolX(i,online);Image image=tools[i*2+(focus==TOOL_IDS[i]?1:0)];c.img(image,tx-image.getWidth()/c.s/2,238-image.getHeight()/c.s);if(focus==TOOL_IDS[i]){String title=new String[]{"Shop","Túi","Bạn bè","Thiết lập",online?"Rời phòng":"Về sảnh"}[i];c.drawTooltip(title,Math.min(tx,208-(c.textWidth(title)+12)/2),168);}}}
 int toolX(int i,boolean online){return TOOL_X[i]-(!online&&i>2?39:0);}
 int toolbarHit(int x,int y){if(y<195||y>=240||x<7||x>=205)return -1;for(int i=0;i<5;i++)if((c.onlineAccount()||i!=2)&&Math.abs(x-toolX(i,c.onlineAccount()))<19)return TOOL_IDS[i];return -1;}
 boolean pressToolbar(int x,int y){int id=toolbarHit(x,y);if(id<0)return false;toolbarTouch=true;toolbarHover=c.roomSelection=id;return true;}
 void dragToolbar(int x,int y){toolbarHover=toolbarHit(x,y);if(toolbarHover>=0)c.roomSelection=toolbarHover;}
 void releaseToolbar(int x,int y){int id=toolbarHit(x,y);boolean activate=toolbarTouch&&id>=0&&id==toolbarHover;toolbarTouch=false;toolbarHover=-1;if(activate){c.roomSelection=id;activate(c.onlineAccount());}}
 void draw(boolean online){draw(online,false);}
 void draw(boolean online,boolean dungeon){load();int f=c.roomSelection;String title=online?c.net.roomTitle:dungeon?"Phó bản vui vẻ":"Thi đấu vui vẻ";
  int[] kit=online?onlineKit:this.kit;
  c.img(panel,214,0);c.pcText("Phòng "+(online?""+c.net.roomId:"Offline"),221,4);c.pcCenter(c.pcShort(title,88),267,18);c.pcText("Trong trận",222,32);
  for(int i=0;i<3;i++){int x=222+i*29;c.img(selected,x,46);c.img(icon(kit[i]),x,47);c.pcText(c.keyBindings.name(i==0?'z':i==1?'x':'c'),x,42);if(editSlot==i)c.drawSelectionGlow(x-1,45,27,27);}
  c.pcText("Đạo cụ",222,77);for(int i=0;i<6;i++){int x=222+i%3*29,y=88+i/3*24;c.part(cell,0,0,24,24,x,y,false);c.img(icon(i),x,y);if(f==23+i)c.drawSelectionGlow(x-1,y-1,28,28);}
  int id=f>=23&&f<=28?f-23:kit[editSlot];
  if (dungeon) {
   drawDungeonMap(online);
  } else {
   int choice=online?c.net.roomChoice:c.randomMap?-1:c.pvpMap();
   int map=choice>=0&&choice<PhoneCanvas.PVP_MAP_COUNT?choice+1:0;
   if(map==0){c.img(randomMap,220,147);c.pcCenter((online?c.net.roomSeconds:c.turnSeconds)+" giây",266,181);}
   else{c.img(c.maps.banner(map),223,149);c.pcCenter(c.pcShort(PhoneCanvas.MAPS[map-1],88),267,181);}
  }
  drawToolbar(online);
  if(f>=20&&f<=28)c.drawTooltip(Battle.ITEM_NAMES[id]+" • "+Battle.ITEM_COST[id]+" TL",108,181);
  c.img(squareInvite,220,197);Image a=c.presentation.searching()?squareCancel:online&&!c.net.host?squareReady:squareStart;c.img(a,272,197);
  if(f>=20&&f<=22)c.drawSelectionGlow(221+(f-20)*29,45,27,27);else if(f==2)c.drawSelectionGlow(219,196,46,44);else if(f==5)c.drawSelectionGlow(271,196,46,44);else if(f==29||f==4)c.drawSelectionGlow(219,146,96,46);
 }
 Image dungeonMapCover, dungeonMapGreen, dungeonMapYellow;
 private void drawDungeonMap(boolean online) {
  if (dungeonMapCover == null) {
   dungeonMapCover = NativeUi.fit(c.dungeon.smallCover(), 82*c.s, 37*c.s, 0);
   dungeonMapGreen = c.combatRoomArt("font-pc-green");
   dungeonMapYellow = c.combatRoomArt("font-pc-yellow");
  }
  c.img(randomMap,220,147);
  c.img(dungeonMapCover,230,151);
  // Keep the original settings corner above the cover.
  for (int row = 0; row < 17; row++) {
   int width = row + 1;
   c.part(randomMap,93-width,24+row,width,1,313-width,171+row,false);
  }
  dungeonMapRow("Dạng phòng", "Ải Viễn Chinh", 151, dungeonMapGreen);
  dungeonMapRow("Độ khó phòng", c.dungeon.difficultyName(online?c.net.dungeonDifficulty:c.offlineDungeon.difficulty), 160, dungeonMapYellow);
  dungeonMapRow("Giới thiệu cấp", "15+", 169, dungeonMapGreen);
  dungeonMapRow("LC đề cử", "100", 178, dungeonMapGreen);
 }
 private void dungeonMapRow(String label, String value, int y, Image font) {
  c.tooltip.colored(label,231,y,font);
  c.pcText(value,233+c.pcWidth(label),y);
 }
 boolean activate(boolean online){int f=c.roomSelection;if(f>=60&&f<66)return false;if(f>=41){if(f==46){c.presentation.settings=true;return true;}if(f==47){if(online)c.net.request("leave");else c.openLobby();}else if(online)c.net.rooms.activateTool(f);else if(f==41)c.openShop();else if(f==42)c.openBag();else if(f==45)return true;else if(f==46)c.net.waiting.notice("HƯỚNG DẪN",c.keyBindings.help());return true;}if(f>=20&&f<=22){editSlot=f-20;return true;}if(f>=23&&f<=28){choose(f-23,online);return true;}if(f==29||f==4){if(online){if(!c.net.host||c.net.inBattle){c.message("Chỉ chủ phòng được chọn bản đồ trước trận.");return true;}}c.maps.open(online);return true;}return false;}
 int x(int f){if(f>=60&&f<66)return 71+(f-60)/2*54;for(int i=0;i<5;i++)if(f==TOOL_IDS[i])return toolX(i,c.onlineAccount());if(f>=20&&f<=22)return 234+(f-20)*29;if(f>=23&&f<=28)return 235+(f-23)%3*29;return f==10?196:f==11?94:f==30?265:f==29||f==4?267:f==2?242:f==5?294:f==7?305:f==1?156:f==8||f==9?25:56;}
 int y(int f){if(f>=60&&f<66)return 54+(f-60)%2*88;if(f>=41)return 220;if(f>=20&&f<=22)return 62;if(f>=23&&f<=28)return 104+(f-23)/3*24;return f==30||f==2||f==5?218:f==29||f==4?173:f==7?12:f==1?128:f==0?128:f==10||f==11?84:f==8?77:f==9?165:37;}
 void move(int dir,boolean online){move(dir,online,false);}
 void move(int dir,boolean online,boolean dungeon){
  int[] order=dungeon?new int[]{2,5,20,21,22,23,24,25,26,27,28,29,41,42,45,46,47}
   :online&&c.net.count==6?new int[]{60,61,62,63,64,65,8,9,2,5,7,20,21,22,23,24,25,26,27,28,29,41,42,45,46,47}
   :new int[]{0,1,2,5,7,11,10,20,21,22,23,24,25,26,27,28,29,41,42,45,46,47};
  int from=c.roomSelection,best=from,score=999999;
  for(int i=0;i<order.length;i++){
   int f=order[i];
   if(!online&&(f==11||f==10||f==45))continue;
   if(online&&(f==0||!c.net.host&&(f==10||f==1)))continue;
   int dx=x(f)-x(from),dy=y(f)-y(from),along=dir==1?-dx:dir==2?dx:dir==4?-dy:dy,cross=dir==1||dir==2?dy:dx;
   if(along<=0)continue;
   int v=along*along+cross*cross*4;
   if(v<score){score=v;best=f;}
  }
  c.roomSelection=best;
 }
 int hit(int x,int y){if(x<214)return -1;if(y>=45&&y<74&&x>=221&&x<309)return 20+Math.min(2,(x-221)/29);if(y>=88&&y<136&&x>=221&&x<310)return 23+Math.min(2,(x-221)/29)+(y-88)/24*3;if(y>=146&&y<197)return 29;if(y>=197)return x<267?2:5;return -1;}
 boolean pointer(int x,int y,boolean online){if(pressToolbar(x,y))return true;int f=hit(x,y);if(f<0)return false;c.roomSelection=f;if(!activate(online)){if(online)c.net.waiting.activate();else c.roomActivate();}return true;}
 void use(int slot){if(!c.isLocalTurn()||c.freeCamera){c.message("Đến lượt bạn và trở về nhân vật để dùng.");return;}Battle.Unit u=c.game.units[c.game.active];if((u.itemUsed&(1<<slot))!=0){c.message("Đạo cụ này đã dùng trong trận");return;}if(u.energy<Battle.ITEM_COST[u.kit[slot]]){c.message("Không đủ thể lực để sử dụng");return;}if(c.onlineBattle())c.net.command(8,slot);else if(!c.game.item(slot))c.message("Đạo cụ chưa thể sử dụng lúc này");}
 Image frost,ice,freezeImpact,hiding;Image effect(String name){return c.combatRoomArt("kit-"+name);}
 Image ice(){if(ice==null)ice=effect("ice-projectile");return ice;}
 void frost(int x,int y){if(frost==null)frost=effect("frost");c.part(frost,c.frame/6%4*64,0,64,64,x-32,y-56,false);}
 void impact(int age,int x,int y){if(freezeImpact==null)freezeImpact=effect("freeze-impact");c.part(freezeImpact,Math.min(4,age/5)*64,0,64,64,x-32,y-48,false);}
 void hud(){Battle.Unit u=c.game.units[c.playerActor()];for(int k=0;k<3;k++){int x=184+k*29,y=170;c.img(c.hudPropSlot,x,y);if((u.itemUsed&(1<<k))==0){c.img(u.energy<Battle.ITEM_COST[u.kit[k]]?grayIcon(u.kit[k]):icon(u.kit[k]),x,y);c.text(c.keyBindings.name(k==0?'z':k==1?'x':'c'),x,y-2);}}if(c.game.units[c.game.active].frozen==2)c.pcCenter("Đóng băng",160,80);}
 boolean hidden(int index){Battle.Unit u=c.game.units[index],self=c.game.units[c.playerActor()];return u.invisible>0&&index!=c.playerActor()&&(!c.game.teams||u.team!=self.team);}
 void preview(){} // Dẫn đường bends real projectiles near a target; it is not an aiming preview.
}
