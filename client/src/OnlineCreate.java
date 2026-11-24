import javax.microedition.lcdui.*;

/** Separate room setup; never enters quick-match through this dialog. */
final class OnlineCreate {
    final OnlineClient net;final PhoneCanvas c;boolean team,dungeon,opened,locked;int focus,dungeonId;String name="Thi đấu vui vẻ",secret="";
    Image panel,input,button,close,on,off;
    OnlineCreate(OnlineClient n){net=n;c=n.c;}
    void open(){dungeon=false;name=team?"Tổ đội vui vẻ":"Thi đấu vui vẻ";opened=true;focus=0;secret="";locked=false;}
    void openDungeon(){team=false;open();dungeon=true;dungeonId=0;name="Ải viễn chinh";}
    static final int[][] BOXES={{117,110,163,25},{117,141,145,25},{123,185,74,29},{263,24,30,27},{271,147,14,14}};
    String shortName(){String value=name;while(value.length()>0&&c.textWidth(value)>151)value=value.substring(0,value.length()-1);return value;}
    void drawModeTitle(){HeaderArt.center(c,dungeon?HeaderArt.DUNGEON:team?HeaderArt.TEAM:HeaderArt.MATCH,218,75);}
    void draw(){if(panel==null){panel=c.combatRoomArt("create-panel");on=c.combatRoomArt("create-lock-on");off=c.combatRoomArt("create-lock-off");}c.img(panel,18,17);drawModeTitle();c.text(shortName(),122,117);if(locked&&secret.length()>0)c.text("****************".substring(0,Math.min(16,secret.length())),122,148);c.img(locked?on:off,272,148);int[] r=BOXES[focus];c.drawSelectionGlow(r[0],r[1],r[2],r[3]);}
    void move(int dir){int[] from=BOXES[focus];int x=from[0]*2+from[2],y=from[1]*2+from[3],best=focus,score=999999;for(int i=0;i<BOXES.length;i++){int[] box=BOXES[i];int dx=box[0]*2+box[2]-x,dy=box[1]*2+box[3]-y,along=dir==1?-dx:dir==2?dx:dir==4?-dy:dy,cross=dir==1||dir==2?dy:dx;if(along<=0)continue;int cost=along+Math.abs(cross)*8;if(cost<score){score=cost;best=i;}}focus=best;}
    void edit(final int index){
        int[] box=BOXES[index];
        c.typing.field(index==0?"Tên phòng":"Mật mã",index==0?name:secret,index==0?32:16,
            false,index==1,box[0]+4,box[1]+4,box[2]-9,new InlineText.Done(){
                public void accept(String value){
                    if(index==0)name=value.trim();
                    else {secret=value;locked=secret.length()>0;}
                }
            });
    }
    void activate(){if(focus==4){locked=!locked;return;}if(focus<2){edit(focus);return;}if(focus==3){opened=false;return;}if(name.length()==0){c.message("Nhập tên phòng.");return;}if(locked&&secret.length()==0){c.message("Nhập mật mã hoặc bỏ chọn khóa phòng.");focus=1;return;}if(!locked)secret="";if(net.createCustom())opened=false;}
    void key(int key,int dir,boolean ok){if(key==-7){opened=false;return;}if(dir!=0)move(dir);else if(ok||key==-6)activate();}
    void pointer(int x,int y){for(int i=0;i<BOXES.length;i++){int[] r=BOXES[i];if(x>=r[0]&&x<r[0]+r[2]&&y>=r[1]&&y<r[1]+r[3]){focus=i;activate();return;}}}
}
