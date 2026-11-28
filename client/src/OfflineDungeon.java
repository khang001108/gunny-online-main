/** Compatibility state for the shared renderer; local campaigns have been removed. */
final class OfflineDungeon{
 final PhoneCanvas c;boolean active,settled,picker;int stage,difficulty,kills,spawned,alive,closedSlots;final int[] allies={-1,-1};
 OfflineDungeon(PhoneCanvas c){this.c=c;}
 void open(){c.net.openDungeon();}void draw(){c.net.draw();}void key(int key,int dir,boolean ok){c.net.key(key,dir,ok);}void pointer(int x,int y){c.net.pointer(x,y);}
 void begin(){open();}void prepare(){open();}void returnForBoss(){if(c.onlineAccount())c.net.request("returnroom");else open();}
 int allyAt(int actor){return -1;}void progress(){active=false;}void finish(){active=false;}void skipStage(){if(c.onlineAccount()){c.cheatSelection=3;c.net.requestCheat(3);}}
}
