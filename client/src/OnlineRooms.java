import javax.microedition.lcdui.*;

/** Server room browser. No offline match setup or local match starts here. */
final class OnlineRooms implements CommandListener {
    final OnlineClient net;final PhoneCanvas c;
    boolean toolbarTouch;final Image[] toolIcons=new Image[10];
    final Image[] cardMaps=new Image[7];Image challengeCard;Image toolbar;Image background,card,filterArt,searchArt,lock,font;List channelList;final Command chooseChannel=new Command("Chọn",Command.OK,1);
    int page,filter,teamMode;String roomCode="";
    final int[] visible=new int[32];int matches;
    final Command enter=new Command("Vào phòng",Command.OK,1),back=new Command("Trở về",Command.BACK,2);
    // Existing selection 0=create, 1=join, 2..33=room; browser controls start at 34.
    static final int[] TOOL_IDS={41,42,45,46,47},TOOL_X={62,120,179,238,296};
    static final int FILTER=34,PREV=35,NEXT=36,TEAM=37,SOLO=38,REFRESH=39,CHANNEL=40;
    int kind(int i){return net.roomKinds[i];}
    boolean full(int i){return net.roomPlayers[i]>=net.roomSeats[i];}
    Image map(int i){int index=net.roomMaps[i]+1;if(index<0||index>=cardMaps.length)index=0;if(cardMaps[index]==null)cardMaps[index]=c.combatRoomArt("roomlist-map"+index);return cardMaps[index];}
    void list(){matches=0;for(int i=0;i<net.roomCount;i++)if(filter==0||filter==1&&kind(i)!=2||filter==2&&kind(i)==2)visible[matches++]=i;page=Math.max(0,Math.min(page,Math.max(0,(matches-1)/4)));}
    OnlineRooms(OnlineClient owner){net=owner;c=owner.c;}
    void load(){if(background!=null)return;loadToolbar();background=c.combatRoomArt("roomlist-background");card=c.combatRoomArt("roomlist-card-free");challengeCard=c.combatRoomArt("roomlist-card-challenge");filterArt=c.combatRoomArt("roomlist-filter");searchArt=c.combatRoomArt("roomlist-search");lock=c.combatRoomArt("roomlist-lock");font=c.combatRoomArt("roomlist-font");}
    void releaseArt(){toolbarTouch=false;for(int i=0;i<10;i++)toolIcons[i]=null;toolbar=background=card=challengeCard=filterArt=searchArt=lock=font=null;for(int i=0;i<7;i++)cardMaps[i]=null;}
    void glow(int id,int x,int y,int w,int h){if(net.selection==id)c.drawSelectionGlow(x,y,w,h);}
    void text(String value,int x,int y){for(int i=0;i<value.length();i++){int glyph=c.glyph(value.charAt(i));c.part(font,glyph%16*8,glyph/16*10,8,10,x,y,false);x+=(c.widths[glyph]*8+5)/11;}}
    void drawToolbar(int selected){
        loadToolbar();
        c.img(toolbar,0,195);
        for(int i=0;i<TOOL_IDS.length;i++){
            Image icon=toolIcons[i*2+(selected==TOOL_IDS[i]?1:0)];
            c.img(icon,TOOL_X[i]-icon.getWidth()/c.s/2,237-icon.getHeight()/c.s);
        }
    }
    void loadToolbar(){
        if(toolbar!=null)return;
        toolbar=c.combatRoomArt("room-toolbar");
        for(int i=0;i<toolIcons.length;i++)toolIcons[i]=c.combatRoomArt("room-tool"+(i/2)+"-"+(i%2));
    }
    void drawToolTip(int selected){
        for(int i=0;i<TOOL_IDS.length;i++)if(i!=2&&selected==TOOL_IDS[i])
            c.drawTooltip(new String[]{"Shop ("+c.keyBindings.name('s')+")","Túi ("+c.keyBindings.name('i')+")","Bạn bè","Thiết lập","Về sảnh"}[i],TOOL_X[i],168);
    }
    void draw(){
        load();list();c.all();c.img(background,0,0);c.part(background,0,205,320,35,0,165,false);c.part(background,0,140,320,45,0,195,false);drawToolbar(net.selection);
        c.img(filterArt,43,14);c.img(filterArt,94,14);c.rect(44,15,104,12,0x68431f);c.pcCenter("Online: "+(net.presenceCap?""+net.onlineCount:"—"),96,16);
        if(false){c.img(filterArt,94,14);c.pcCenter(filter==1?"Tự do":"Khiêu chiến",119,16);}
        if(roomCode.length()>0){c.img(searchArt,151,15);text(roomCode,163,17);}
        glow(1,151,15,78,14);glow(REFRESH,270,15,42,13);
        for(int slot=0;slot<4&&page*4+slot<matches;slot++){
            int i=visible[page*4+slot],x=12+slot%2*150,y=43+slot/2*40;
            c.img(map(i),x+60,y+5);c.img(kind(i)==2?challengeCard:card,x,y);text(net.roomPlayers[i]+"/"+net.roomSeats[i],x+69,y+7);
            text("Số phòng: "+net.roomIds[i],x+5,y+25);
            if(net.roomLocked[i])c.img(lock,x+56,y+8);
            glow(i+2,x,y,146,38);
        }
        if(matches==0){c.pcCenter(filter==0?"Chưa có phòng online":"Không có phòng phù hợp",160,84);c.pcCenter("Chọn Tạo phòng để mời bạn bè",160,102);}
        if(!net.status.equals("Có "+net.roomCount+" phòng")&&!net.status.equals("Đã đăng nhập")&&!net.status.equals("Đăng ký thành công"))c.pcCenter(c.pcShort(net.status,286),160,146);
        if(page>0)c.pcCenter("Trang "+(page+1)+"/"+Math.max(1,(matches+3)/4),160,156);
        glow(PREV,6,177,46,12);glow(NEXT,56,177,47,12);glow(TEAM,105,169,61,27);glow(SOLO,175,169,61,27);glow(0,245,169,66,27);drawToolTip(net.selection);
    }
    void channels(){channelList=new List("Máy chủ",List.IMPLICIT);channelList.append("1",null);channelList.addCommand(chooseChannel);channelList.addCommand(back);channelList.setCommandListener(this);Display.getDisplay(c.app).setCurrent(channelList);}
    void refresh(){if(net.busy||net.pending.length()>0)return;net.request("rooms");}
    void code(){
        if(net.pending.length()>0||net.operation.startsWith("create")||net.operation.equals("join"))return;
        final boolean expedition=expeditionList();
        int y=expedition?152:14;
        c.typing.field("Nhập số phòng",expedition?c.dungeon.roomCode:roomCode,6,true,false,
            expedition?14:152,y,expedition?89:76,new InlineText.Done(){
                public void accept(String value){
                    if(expedition)c.dungeon.roomCode=value;
                    else {roomCode=value;joinCode();}
                }
            });
    }
    public void commandAction(Command command,Displayable screen){
        if(screen==channelList){Display.getDisplay(c.app).setCurrent(c);channelList=null;}
    }
    void joinCode(){try{int id=Integer.parseInt(roomCode);if(id<=0)throw new NumberFormatException();join(id);}catch(NumberFormatException e){c.message("Nhập mã phòng hợp lệ.");net.status="Nhập mã phòng hợp lệ.";}}
    void join(int id){if(c.requireWeapon())return;if(net.pending.length()>0||net.operation.equals("join")||net.operation.startsWith("create"))return;net.joinId=id;net.joinTeam=net.modernRooms?2:1;for(int i=0;i<net.roomCount;i++)if(net.roomIds[i]==id&&(net.roomCapacities[i]>2||net.asyncTurns)){net.joinForm();return;}net.request("join");}
    void activateTool(int selected){
        if(selected==41)c.openShop();
        else if(selected==42)c.openBag();
        else if(selected==45)net.social.open(false);
        else if(selected==46)c.presentation.settings=true;
        else if(selected==47)net.returnHome();
    }
    void activate(){
        int selected=net.selection;if(selected>=41){activateTool(selected);return;}if(selected==0){net.create.team=false;net.create.open();return;}if(selected==1){code();return;}
        if(selected>=2&&selected<34){int i=selected-2;if(i<net.roomCount){if(net.roomPhases[i]!=0||full(i)){net.status="Phòng đã bắt đầu hoặc đủ người.";return;}join(net.roomIds[i]);}return;}
        if(selected==CHANNEL){channels();return;}if(selected==FILTER){filter=(filter+1)%3;page=0;}else if(selected==PREV)page=Math.max(0,page-1);else if(selected==NEXT){list();page=Math.min(Math.max(0,(matches-1)/4),page+1);}else if(selected==TEAM){teamMode=1;net.quick(2);}else if(selected==SOLO){teamMode=0;net.quick(1);}else if(selected==REFRESH)refresh();
    }
    int focusX(int id){if(id>=2&&id<34){for(int i=0;i<4&&page*4+i<matches;i++)if(visible[page*4+i]+2==id)return 85+i%2*150;}if(id==CHANNEL)return 67;if(id==FILTER)return 121;if(id==1)return 190;if(id==REFRESH)return 291;if(id==PREV)return 29;if(id==NEXT)return 79;if(id==TEAM)return 135;if(id==SOLO)return 205;if(id==0)return 278;for(int i=0;i<TOOL_IDS.length;i++)if(TOOL_IDS[i]==id)return TOOL_X[i];return 160;}
    int focusY(int id){if(id>=2&&id<34){for(int i=0;i<4&&page*4+i<matches;i++)if(visible[page*4+i]+2==id)return 62+i/2*40;}if(id==CHANNEL||id==FILTER||id==1||id==REFRESH)return 21;return id>=41?218:182;}
    void key(int key,int dir,boolean ok){
        if(key=='r'||key=='R'){refresh();return;}
        if(dir!=0){list();int[] order=new int[20];int n=0;order[n++]=1;order[n++]=REFRESH;for(int slot=0;slot<4&&page*4+slot<matches;slot++)order[n++]=visible[page*4+slot]+2;order[n++]=PREV;order[n++]=NEXT;order[n++]=TEAM;order[n++]=SOLO;order[n++]=0;for(int i=0;i<TOOL_IDS.length;i++)order[n++]=TOOL_IDS[i];
            int x=focusX(net.selection),y=focusY(net.selection),best=-1,score=99999;for(int i=0;i<n;i++){if(order[i]==net.selection)continue;int dx=focusX(order[i])-x,dy=focusY(order[i])-y,along=dir==1?-dx:dir==2?dx:dir==4?-dy:dy,cross=dir==1||dir==2?Math.abs(dy):Math.abs(dx);if(along<=0)continue;int cost=along+cross*3;if(cost<score){score=cost;best=order[i];}}if(best>=0)net.selection=best;
        }else if(ok||key==-6)activate();
    }
    int toolHit(int x,int y){if(y<195||y>=240)return -1;for(int i=0;i<5;i++)if(Math.abs(x-TOOL_X[i])<23)return TOOL_IDS[i];return -1;}
    private boolean expeditionList(){return net.dungeonBrowser&&net.roomId==0;}
    boolean pressToolbar(int x,int y){
        int hit=toolHit(x,y);
        if(hit<0)return false;
        toolbarTouch=true;
        if(expeditionList())c.dungeon.focus=hit;
        else net.selection=hit;
        return true;
    }
    void drag(int x,int y){if(toolbarTouch)pressToolbar(x,y);}
    void release(int x,int y){
        toolbarTouch=false;
        int hit=toolHit(x,y);
        if(hit>=0&&hit==(expeditionList()?c.dungeon.focus:net.selection))activateTool(hit);
    }
    void pointer(int x,int y){
        if(y>=14&&y<30){if(x>=151&&x<229)net.selection=1;else if(x>=232&&x<266){if(roomCode.length()==0)code();else joinCode();return;}else if(x>=270&&x<312)net.selection=REFRESH;else return;activate();return;}
        list();for(int slot=0;slot<4&&page*4+slot<matches;slot++){int rx=12+slot%2*150,ry=43+slot/2*40;if(x>=rx&&x<rx+146&&y>=ry&&y<ry+38){net.selection=visible[page*4+slot]+2;activate();return;}}
        if(y>=195){pressToolbar(x,y);return;}
        if(y>=167&&y<195){if(x<53)net.selection=PREV;else if(x<99)net.selection=NEXT;else if(x<169)net.selection=TEAM;else if(x<239)net.selection=SOLO;else net.selection=0;activate();}
    }
}
