import javax.microedition.lcdui.*;

/** Draft selection is local; only Confirm changes the authoritative room. */
final class MapPicker {
    final PhoneCanvas c;boolean online;int choice,seconds=12,focus,stage;
    static final int CONFIRM_X=236,CONFIRM_Y=210,CONFIRM_W=50,CONFIRM_H=20;
    Image frame,blueButton;final Image[] banners=new Image[7];
    Image dungeonCover, tribalCover, compactFont;
    private static final String DUNGEON_DESCRIPTION =
        "Dũng sĩ tứ phương bước vào rừng yên ắng, cứ như không có chuyện gì xảy ra. "
        + "Họ đâu ngờ trong cánh rừng sâu thẳm kia có cặp mắt đang quan sát họ.";
    static final int[] TIMES={8,12,15};
    static final String[] DESCRIPTION={"Chọn ngẫu nhiên trong 6 bản đồ.","Đu quay và những mái nhà rực nắng.","Rễ cây đan xen giữa khu rừng xanh.","Lớp học phép thuật với nhiều vật cản.","Những ngôi nhà nhỏ giữa các đảo nổi.","Cầu gỗ uốn cong, cuộn trục trên cao.","Con tàu đắm dưới làn nước xanh."};
    MapPicker(PhoneCanvas canvas){c=canvas;}
    void open(boolean network){online=network;choice=network?Battle.clamp(c.net.roomChoice+1,0,banners.length-1):c.randomMap?0:c.pvpMap()+1;seconds=network?c.net.roomSeconds:c.turnSeconds;if(seconds!=8&&seconds!=12&&seconds!=15)seconds=12;stage=0;focus=choice;c.mode=17;}
    Image banner(int index){if(banners[index]==null)banners[index]=c.combatRoomArt("map-banner-"+index);return banners[index];}
    String name(){return choice==0?"Ngẫu nhiên":PhoneCanvas.MAPS[choice-1];}
    void drawFrame(String title) {
        c.all();
        if(frame==null){frame=c.combatRoomArt("map-picker-frame");blueButton=c.combatRoomArt("map-picker-time");}
        c.img(frame,0,0);
        c.pcText(title,8,8);
    }
    void drawDungeon(int difficulty, boolean confirmFocused) {
        drawFrame("Chọn ải viễn chinh");
        for(int i=0;i<2;i++){int x=16+i*94;if(i==0&&dungeonCover==null)dungeonCover=c.combatRoomArt("dungeon-picker-cover");if(i==1&&tribalCover==null)tribalCover=c.combatRoomArt("tribal-picker-cover");if(c.dungeon.portraitFrame==null){c.loadout.load();c.dungeon.portraitFrame=NativeUi.fit(c.loadout.card,82*c.s,172*c.s,5*c.s);}c.img(c.dungeon.portraitFrame,x,31);c.img(c.dungeon.portraitCard(i),x+2,33);c.pcCenter(i==0?"Huyệt Ma":"Bộ Lạc",x+41,143);c.pcCenter(i==0?"Kiến":"Tà Thần",x+41,157);c.pcCenter(i==0?"Lv15+":"Lv30+",x+41,170);c.pcCenter(i==0?"Ải: 1-2":"Ải: 1-3",x+41,182);if(c.dungeon.draftDungeon==i)c.drawSelectionGlow(x-2,29,86,176);}
        boolean tribal=c.onlineAccount()&&c.dungeon.draftDungeon==1;
        c.part(frame, 223, 17, 78, 14, 223, 31, false);
        c.pcCenter("Mức độ", 262, 34);
        for (int i = 0; i < 3; i++) {
            int x = 213 + i*33;
            c.img(blueButton,x,54);
            c.pcCenter(c.dungeon.difficultyName(i),x+15,60);
            if (difficulty == i) c.drawSelectionGlow(x-1,53,33,20);
        }
        c.img(tribal?c.tribalArt.smallCover():c.dungeon.smallCover(),216,89);
        c.pcText(tribal?"Bộ Lạc Tà Thần":"Huyệt Ma Kiến",211,130);
        drawDungeonDescription();
        if (confirmFocused) c.drawSelectionGlow(CONFIRM_X,CONFIRM_Y,CONFIRM_W,CONFIRM_H);
    }
    private void drawDungeonDescription() {
        String description=c.onlineAccount()&&c.dungeon.draftDungeon==1?"Hạ 20 lính bộ lạc, đánh bại anh em Bulan và Sulan hai dạng. Tổ đội 1-3 người.":DUNGEON_DESCRIPTION;
        if (compactFont == null) {
            Image source = c.combatRoomArt("font-pc");
            compactFont = NativeUi.fit(source, source.getWidth()*3/4, source.getHeight()*7/10, 0);
        }
        int x = 216, y = 143, start = 0;
        while (start < description.length()) {
            int end = description.indexOf(' ', start);
            if (end < 0) end = description.length();
            String word = description.substring(start, end);
            int width = 0;
            for (int i = 0; i < word.length(); i++)
                width += compactWidth(word.charAt(i));
            if (x + width > 306) {
                x = 216;
                y += 8;
                if(y>199)return;
            }
            for (int i = 0; i < word.length(); i++) {
                int glyph = c.glyph(word.charAt(i));
                c.part(compactFont, glyph%16*6, glyph/16*7, 6, 7, x, y, false);
                x += compactWidth(word.charAt(i));
            }
            x += 3;
            start = end + 1;
        }
    }
    void smallText(String text,int center,int y){if(compactFont==null){Image source=c.combatRoomArt("font-pc");compactFont=NativeUi.fit(source,source.getWidth()*3/4,source.getHeight()*7/10,0);}int width=0;for(int i=0;i<text.length();i++)width+=compactWidth(text.charAt(i));int x=center-width/2;for(int i=0;i<text.length();i++){int glyph=c.glyph(text.charAt(i));c.part(compactFont,glyph%16*6,glyph/16*7,6,7,x,y,false);x+=compactWidth(text.charAt(i));}}
    private int compactWidth(char ch) {
        return Math.max(2,(c.pcWidth(String.valueOf(ch))*3+2)/4);
    }
    boolean hitConfirm(int x,int y) {
        return x>=CONFIRM_X && x<CONFIRM_X+CONFIRM_W && y>=CONFIRM_Y && y<CONFIRM_Y+CONFIRM_H;
    }
    void draw(){
        drawFrame("Chọn bản đồ");
        for(int i=0;i<7;i++){int x=12+i%2*95,y=28+i/2*42;c.img(banner(i),x,y);c.pcCenter(c.pcShort(i==0?"Ngẫu nhiên":PhoneCanvas.MAPS[i-1],83),x+44,y+12);if(choice==i)c.drawSelectionGlow(x-2,y-2,92,36);if(focus==i){c.line(x-2,y-2,x+89,y-2,0xffee7b);c.line(x-2,y+34,x+89,y+34,0xffee7b);c.line(x-2,y-2,x-2,y+34,0xffee7b);c.line(x+89,y-2,x+89,y+34,0xffee7b);}}
        for(int i=0;i<3;i++){int x=213+i*33;c.img(blueButton,x,54);c.pcCenter(TIMES[i]+" giây",x+15,60);if(seconds==TIMES[i]||focus==7+i)c.drawSelectionGlow(x-1,53,33,20);}
        c.img(banner(choice),216,89);c.pcText(c.pcShort(name(),87),216,130);
        drawDescription(DESCRIPTION[choice], 150, 12, 192);
        if(focus==10)c.drawSelectionGlow(CONFIRM_X,CONFIRM_Y,CONFIRM_W,CONFIRM_H);
    }
    void drawDescription(String text, int y, int lineHeight, int bottom) {
        while (text.length()>0 && y<bottom) {
            int end=text.length();
            while(end>1 && c.pcWidth(text.substring(0,end))>86)end--;
            if(end<text.length()){int space=text.lastIndexOf(' ',end-1);if(space>0)end=space;}
            c.pcText(text.substring(0,end),216,y);
            text=text.substring(end).trim();
            y+=lineHeight;
        }
    }
    void cancel(){frame=blueButton=null;c.mode=online?16:10;}
    void confirm(){
        if(online){if(!c.net.host){notice("Chỉ chủ phòng được chọn bản đồ.");return;}if(!c.net.modernRooms){notice("Cập nhật máy chủ để chọn bản đồ online.");return;}if(c.net.pending.length()>0||c.net.operation.equals("roomsettings"))return;c.net.mapDraft=choice-1;c.net.timeDraft=seconds;c.net.request("roomsettings");frame=blueButton=null;c.mode=16;}
        else{c.randomMap=choice==0;if(choice>0)c.selectedMap=choice-1;c.turnSeconds=seconds;frame=blueButton=null;c.mode=10;}
    }
    void notice(String text){Alert a=new Alert("Bản đồ",text,null,AlertType.INFO);a.setTimeout(Alert.FOREVER);Display.getDisplay(c.app).setCurrent(a,c);}
    int timeFocus(){for(int i=0;i<TIMES.length;i++)if(seconds==TIMES[i])return 7+i;return 8;}
    void back(){if(stage==2){stage=1;focus=timeFocus();}else if(stage==1){stage=0;focus=choice;}else cancel();}
    void activate(){if(stage==0){choice=focus;stage=1;focus=timeFocus();}else if(stage==1){seconds=TIMES[focus-7];stage=2;focus=10;}else confirm();}
    void key(int key,int direction,boolean ok){
        if(key==-7){back();return;}
        if(direction!=0){
            if(stage==0){if(direction==1&&focus%2==1)focus--;else if(direction==2&&focus%2==0&&focus<6)focus++;else if(direction==4&&focus>=2)focus-=2;else if(direction==8&&focus+2<7)focus+=2;choice=focus;}
            else if(stage==1){if(direction==1||direction==4)focus=Math.max(7,focus-1);else if(direction==2||direction==8)focus=Math.min(9,focus+1);seconds=TIMES[focus-7];}
        }else if(ok||key==-6)activate();
    }
    void pointer(int x,int y){
        if(x>=297&&y<25){cancel();return;}
        if(stage==1&&x>=210&&y>=52&&y<76){int i=Math.max(0,Math.min(2,(x-213)/33));focus=7+i;seconds=TIMES[i];return;}
        if(x>=CONFIRM_X&&x<CONFIRM_X+CONFIRM_W&&y>=CONFIRM_Y&&y<CONFIRM_Y+CONFIRM_H){activate();return;}
        if(stage==0)for(int i=0;i<7;i++){int bx=12+i%2*95,by=28+i/2*42;if(x>=bx&&x<bx+88&&y>=by&&y<by+32){focus=choice=i;return;}}
    }
}
