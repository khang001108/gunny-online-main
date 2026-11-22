import javax.microedition.lcdui.Image;
/** Shared native item-info frame for both dungeon families. Presentation only. */
final class DungeonConditions {
 final PhoneCanvas c;Image frame,orange,green;int stage=-1,room=-1;long until;
 DungeonConditions(PhoneCanvas canvas){c=canvas;}
 void reset(){stage=room=-1;until=0;frame=orange=green=null;}
 void toggle(){until=System.currentTimeMillis()<until?0:System.currentTimeMillis()+3000;}
 boolean visible(){return c.game.dungeonStage>0&&System.currentTimeMillis()<until;}
 void draw(){
  int current=c.game.dungeonStage;if(current<=0||c.game.over)return;
  int id=c.onlineBattle()?c.net.roomId:0;
  if(stage!=current||room!=id){stage=current;room=id;until=System.currentTimeMillis()+3000;}
  if(!visible())return;
  if(frame==null)frame=NativeUi.fit(c.combatRoomArt("item752-tooltip"),288*c.s,84*c.s,7*c.s);
  c.img(frame,16,44);if(orange==null){orange=c.combatRoomArt("font-pc-orange");green=c.combatRoomArt("font-pc-green");}Image old=c.pcFont;try{c.pcFont=orange;c.pcText("Điều kiện thắng",24,50);c.pcFont=green;c.pcText("Điều kiện thua",164,50);}finally{c.pcFont=old;}
  c.line(159,49,159,118,0x8c826b);c.line(23,64,295,64,0x8c826b);
  String win=current==1?"Hạ 20 con kiến":current==2?"Đánh bại Nữ Vương":current==3?"Hạ 20 lính bộ lạc":TribalRules.twins(c.game)?"Hạ cả hai anh em Bulan":"Đánh bại Sulan";
  c.pcText(win,24,73);c.pcText("Cả đội bị đánh bại",164,73);c.pcText("Quá giới hạn lượt",164,88);c.pcText("M: xem / ẩn",24,109);
 }
}
