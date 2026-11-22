import javax.microedition.lcdui.*;

/**
 * Two working slots and two inventory sections, following the supplied native
 * forge.
 */
final class ForgeScreen {
    void releaseArt(){resultSuccess=resultFailure=glow=star=null;frame=stone=stoneSmall=left=inventoryPanel=cell=equipTab=equipment=material=percent=on=off=action=fill=success=slotSuccess=continuousText=stoneLabel=hammer=closeButton=luckyIcon=protectIcon=null;for(int i=0;i<strength.length;i++)strength[i]=null;running=false;}

    Image resultSuccess,resultFailure,glow,star;int resultKind;long resultUntil;
    final PhoneCanvas c;
    int page, selection, count, kind, item, expected, expectedProgress, focus, confirm, animFrom, animTo, riseRank;
    final int[] kinds = new int[130], items = new int[130];
    final Image[] strength = new Image[16];
    String requestKey = "";
    Image closeButton,luckyIcon,protectIcon;
    Image frame, stone, stoneSmall, left, inventoryPanel, cell, equipTab, equipment, material, percent, on, off, action, fill,
            success, slotSuccess, continuousText, stoneLabel, hammer;
    int grade=1,quantity=1;
    boolean lucky,protect;
    boolean info;int step,stoneSelection;final int[] ownedGrades=new int[5];int gradeCount;
    boolean continuous, running;
    long nextAt, animStart, successUntil, riseStart;

    ForgeScreen(PhoneCanvas canvas) {
        c = canvas;
    }

    void open() {
        page = selection = focus = confirm = 0;
        step=0;resultUntil=successUntil=riseStart=0;quantity=1;continuous = running = false;
        requestKey = "";
        c.mode = 18;
        refresh();
        if (c.onlineAccount())
            c.net.request("economy");
    }

    void refresh() {
        gradeCount=0;for(int g=1;g<=5;g++)if(c.economy.material(g-1)>0)ownedGrades[gradeCount++]=g;stoneSelection=Math.min(stoneSelection,Math.max(0,gradeCount-1));
        previewStone();
        page=step==1?0:selection/25;
        count = 0;
        for (int k = 0; k < 7; k++)
            if (ForgeRules.allowed(k))
                for (int i = 0; i < 10; i++)
                    if (c.inventory.owns(k, i) && !(k == 0 && !ShopCatalog.weapon(i))) {
                        kinds[count] = k;
                        items[count++] = i;
                    }
        selection = Math.min(selection, Math.max(0, count - 1));
        if (count > 0) {
            kind = kinds[selection];
            item = items[selection];
            int i = ForgeRules.index(c.gender, kind, item);
            expected = c.economy.ranks()[i];
            expectedProgress = c.economy.progress()[i];
        }
    }

    boolean sending() {
        return c.net.operation.equals("forgechance") || c.net.pending.equals("forgechance");
    }

    void select(int index) {
        if (sending() || index < 0 || index >= count)
            return;
        running = false;
        selection = index;
        page = index / 25;
        confirm = 0;
        requestKey = "";
        refresh();
    }

    void load() {
        c.bag.load();
        if (frame != null)
            return;
        frame = c.combatRoomArt("forge780-frame");
        closeButton = NativeUi.fit(c.bag.close,17*c.s,16*c.s,2*c.s);
        luckyIcon=NativeUi.fit(c.stoneShop.icon(5),12*c.s,12*c.s,0);protectIcon=NativeUi.fit(c.stoneShop.icon(6),12*c.s,12*c.s,0);
        stone = c.stoneShop.icon(0);
        stoneSmall = NativeUi.fit(stone, 12 * c.s, 12 * c.s, 2 * c.s);
        left = c.combatRoomArt("forge780-left");
        inventoryPanel = NativeUi.fit(left, 142 * c.s, 168 * c.s, 5 * c.s);
        cell = NativeUi.fit(c.bag.cell, 25 * c.s, 25 * c.s, 2 * c.s);
        equipTab = NativeUi.fit(c.bag.equipOn, 21 * c.s, 43 * c.s, 2 * c.s);
        equipment = c.combatRoomArt("forge780-equipment");
        material = c.combatRoomArt("forge780-material");
        percent = NativeUi.fit(c.combatRoomArt("forge780-percent"), 82 * c.s, 10 * c.s, 3 * c.s);
        action = NativeUi.fit(c.combatRoomArt("auth714-button"), 78 * c.s, 21 * c.s, 5 * c.s);
        fill = NativeUi.fit(c.combatRoomArt("forge780-fill"), 76 * c.s, 6 * c.s, 2 * c.s);
        resultSuccess=c.combatRoomArt("forge789-success");resultFailure=c.combatRoomArt("forge789-failure");
        glow=c.combatRoomArt("forge789-glow");star=c.combatRoomArt("forge789-star");
        continuousText = c.combatRoomArt("forge780-continuous");
        stoneLabel = c.combatRoomArt("forge780-stone-label");
        hammer = NativeUi.fit(c.combatRoomArt("forge780-hammer"),
        14 * c.s, 14 * c.s, 2 * c.s);
        on = c.combatRoomArt("auth-check-on");
        off = c.combatRoomArt("auth-check-off");
    }

    int percent() {
        return count==0||expected>=ForgeRules.MAX_RANK?0:ForgeRules.chance(expected,grade,quantity,lucky)/100;
    }

    int shownPercent(){return percent();}
    void previewStone(){if(step==1&&gradeCount>0){grade=ownedGrades[stoneSelection];quantity=1;requestKey="";}}
    void drawUpgradeFx(long now){
        if(now>=successUntil)return;
        int elapsed=(int)(now-animStart),frame=Math.min(5,elapsed*6/1100);
        c.part(glow,frame*60,0,60,60,20,65,false);
        for(int i=0;i<4;i++){int p=(elapsed/100+i*2)%8;int x=p<3?21+p*24:p<5?69:p<7?69-(p-4)*24:21;int y=p<3?68:p<5?68+(p-2)*20:p<7?108:88;c.img(star,x,y);}
    }

    void draw() {
        load();
        refresh();
        long now = System.currentTimeMillis();
        c.all();
        c.img(c.lobbyBackground, 0, 0);
        c.img(frame, 4, 8);
        c.pcText("Tiệm rèn", 13, 14);
        c.img(closeButton,289,10);
        c.wallet(153, 34);
        c.img(left, 12, 55);
        c.pcText("Trang bị", 22, 57);
        c.pcText("Đá cường hóa", 84, 57);
        c.img(equipment, 24, 72);
        c.img(material, 94, 72);
        if (count > 0)
            c.img(c.bag.itemIcon(kind, item), 38, 83);
        if (c.economy.material(grade-1) > 0)
            c.img(c.stoneShop.icon(grade-1), 107, 83);
        drawUpgradeFx(now);
        c.pcCenter("+" + expected, 51, 108);
        c.pcCenter("x" + c.economy.material(grade-1), 120, 108);
        c.img(percent, 45, 123);
        int pct = shownPercent(), fw = 76 * pct / 100;
        if (fw > 0)
            c.part(fill, 0, 0, fw, 6, 48, 125, false);

        int chance=ForgeRules.chance(expected,grade,quantity,lucky);
        c.pcCenter("Tỉ lệ: "+chance/100+"."+(chance%100<10?"0":"")+chance%100+"%",85,123);
        c.pcCenter(step==0?"Chọn trang bị":step==1?"Chọn đá cường hóa":"Chọn bùa và cường hóa",80,140);
        c.img(lucky?on:off,19,157);
        c.img(luckyIcon,34,157);c.pcText("May 25% ("+c.economy.material(5)+")",49,158);
        c.img(protect?on:off,19,173);
        c.img(protectIcon,34,173);c.pcText("Bảo vệ ("+c.economy.material(6)+")",49,174);
        if(focus==7||lucky)c.drawSelectionGlow(17,155,123,16);
        if(focus==8||protect)c.drawSelectionGlow(17,171,123,15);
        if(focus==2||continuous)c.drawSelectionGlow(17,185,123,13);
        c.img(continuous?on:off,19,187);
        c.pcText("Liên tục",34,187);
        c.img(hammer, 30, 202);
        c.img(action, 48, 198);
        c.pcCenter(running ? "Dừng" : "Cường hóa", 87, 203);

        if(focus==9)c.drawSelectionGlow(94,72,52,53);
        if (focus == 1)
            c.drawSelectionGlow(48, 198, 78, 21);
        c.img(inventoryPanel, 149, 55);
        c.img(c.bag.input, 153, 57);
        c.pcText(c.pcShort(step==1&&gradeCount>0?StoneShop.NAMES[ownedGrades[stoneSelection]-1]:count>0?c.bag.itemName(kind,item):"Trang bị",120),160,62);
        c.img(equipTab, 292, 57);
        for (int n = 0; n < 25; n++) {
            int i = page * 25 + n, x = 154 + n % 5 * 26, y = 78 + n / 5 * 26;
            c.img(cell, x, y);
            if(step==1){if(i<gradeCount){c.img(c.stoneShop.icon(ownedGrades[i]-1),x+2,y);c.pcText("L"+ownedGrades[i],x+12,y);}}else if (i < count)
                c.img(c.bag.itemIcon(kinds[i], items[i]), x + 2, y);
            if (i == (step==1?stoneSelection:selection) && (step==1?gradeCount:count) > 0)
                c.drawSelectionGlow(x, y, 25, 25);
        }
        c.img(c.bag.arrow, 155, 207);
        c.pcCenter("<", 165, 210);
        c.pcCenter((page + 1) + "/" + Math.max(1, ((step==1?gradeCount:count) + 24) / 25), 222, 210);
        c.img(c.bag.arrow, 269, 207);
        c.pcCenter(">", 279, 210);
        if(now<resultUntil)c.img(resultKind>0?resultSuccess:resultFailure,16,54-(int)Math.min(10,(now-animStart)/130));
        if(info&&step==2&&(focus==7||focus==8||focus==9))c.stoneShop.drawInfo(focus==7?5:focus==8?6:grade-1,new int[]{154,90,25,25});
        else if(info&&step==1&&gradeCount>0)c.stoneShop.drawInfo(ownedGrades[stoneSelection]-1,new int[]{154+stoneSelection*26,78,25,25});
        else if (info && count > 0)
            c.tooltip.draw(kind, item, new int[] { 154 + selection % 5 * 26, 78 + (selection % 25) / 5 * 26, 25, 25 });
    }

    void upgrade() {
        if (running) {
            running = false;
            return;
        }
        if (count == 0 || expected >= ForgeRules.MAX_RANK)
            return;
        if (c.net.roomId > 0) {
            c.message("Rời phòng trước khi rèn");
            return;
        }
        if (c.economy.material(grade-1) < quantity) {
            failed();
            c.message("Không còn đá cường hóa");
            return;
        }
        if(c.onlineAccount()&&!c.net.forgeChanceCap){c.message("Cập nhật máy chủ cường hóa");return;}
        running = continuous;
        submit();
    }

    void submit() {
        if (sending() || c.net.pending.length() > 0)
            return;
        if (c.onlineAccount()) {
            if (requestKey.length() == 0)
                requestKey = c.nextEconomyKey();
            c.net.forgeKind = kind;
            c.net.forgeItem = item;
            c.net.forgeExpected = expected;
            c.net.forgeProgressExpected = expectedProgress;
            c.net.economyKey = requestKey;
            c.net.craftGrade=grade;c.net.craftQuantity=quantity;c.net.craftLucky=lucky;c.net.craftProtect=protect;
            c.net.request("forgechance");
        } else if (c.economy.forgeChance(kind,item,expected,grade,quantity,lucky,protect))
            complete();
        else
            failed();
    }

    void complete() {
        int before = expected, beforeProgress = expectedProgress,
                from = before >= ForgeRules.MAX_RANK ? 100 : beforeProgress * 100 / ForgeRules.cost(before);
        confirm = 0;
        requestKey = "";
        refresh();
        animFrom = from;
        animTo = 100;
        animStart = System.currentTimeMillis();
        nextAt = animStart + 1100;
        if (expected > before) {
            successUntil = animStart + 1100;
            riseStart = animStart;
            riseRank = expected;
        }
        resultKind=expected>before?1:-1;resultUntil=animStart+1800;if(resultKind<0)successUntil=0;
        c.message(expected > before ? "Trang bị +" + expected : expected<before?"Giảm xuống +"+expected:"Giữ cấp +"+expected);
    }

    void failed() {
        running = false;
        confirm = 0;
    }

    void tick() {
        if (c.mode != 18) {
            running = false;
            return;
        }
        if (!running || c.net.busy || c.net.pending.length() > 0 || System.currentTimeMillis() < nextAt)
            return;
        refresh();
        if (count == 0 || expected >= ForgeRules.MAX_RANK || c.economy.material(grade-1)<quantity || lucky&&c.economy.material(5)<1 || protect&&c.economy.material(6)<1) {
            running = false;
            c.message(expected >= ForgeRules.MAX_RANK ? "Đã đạt +15" : "Dừng: đã hết đá");
            return;
        }
        submit();
    }

    void toggle() {
        continuous = !continuous;
        if (!continuous)
            running = false;
    }

    void leave() {
        running = false;
        c.openLobby();
    }

    void key(int key, int dir, boolean ok) {
        if(step==2&&key==-7){step=1;focus=0;info=false;return;}if(step==2&&focus==0&&(dir!=0||ok))step=0;
        if(step<2){if(key==-7){if(step==1){step=0;info=false;}else leave();return;}if(key==-6){info=!info;return;}if(dir!=0){info=false;if(step==1){stoneSelection=Battle.clamp(stoneSelection+(dir==1||dir==4?-1:1),0,Math.max(0,gradeCount-1));previewStone();}else select(Battle.clamp(selection+(dir==1?-1:dir==2?1:dir==4?-5:5),0,count-1));return;}if(ok){info=false;if(step==0){if(count>0){step=1;focus=0;previewStone();}}else if(gradeCount>0){grade=ownedGrades[stoneSelection];quantity=1;step=2;focus=7;}else c.message("Chưa có đá cường hóa");}return;}

        if(!sending()&&!running){


            if(key=='l'||key=='L'){lucky=!lucky;focus=7;requestKey="";return;}
            if(key=='p'||key=='P'){protect=!protect;focus=8;requestKey="";return;}
        }
        if(key==-6&&(focus==7||focus==8||focus==9)){info=!info;return;}
        if(focus>=7&&focus<=9){
            if(dir==4)focus=focus==9?0:focus==7?9:7;
            else if(dir==8)focus=focus==9?7:focus==7?8:2;
            else if(dir==1||dir==2){if(focus==9&&!sending()&&!running){step=1;focus=0;info=false;}else focus=0;}
            else if(ok&&!sending()&&!running){if(focus==7)lucky=!lucky;else if(focus==8)protect=!protect;else{step=1;focus=0;info=false;}requestKey="";}
            else if(key==-7)leave();
            return;
        }
        if (key == -6) {
            if (info)
                info = false;
            else if (count > 0)
                info = true;
            return;
        }
        if (key == -7) {
            leave();
            return;
        }
        // QWERTY Space swaps between the forge controls and the equipment bag.
        // Enter/5/center-FIRE remains the activation key.
        if (key == 32) {
            info = false;
            focus = focus == 0 ? 1 : 0;
            return;
        }
        if (dir != 0) {
            if (focus == 0) {
                if (dir == 1 || dir == 2) {
                    int row = selection % 25 / 5;
                    if (dir == 1 && selection % 5 == 0 && page > 0)
                        select(Math.min((page - 1) * 25 + row * 5 + 4, count - 1));
                    else if (dir == 1 && selection % 5 == 0)
                        focus = 1;
                    else if (dir == 2 && selection % 5 == 4 && page < (count - 1) / 25)
                        select(Math.min((page + 1) * 25 + row * 5, count - 1));
                    else if (dir == 2 && selection % 5 == 4)
                        focus = 1;
                    else
                        select(Battle.clamp(selection + (dir == 1 ? -1 : 1), 0, count - 1));
                } else if (dir == 4) {
                    if (selection >= 5)
                        select(selection - 5);
                    else
                        focus = 3;
                } else if (selection + 5 < count)
                    select(selection + 5);
                else
                    focus = 6;
            } else if (focus == 6)
                focus = dir == 4 ? 0 : dir == 1 ? 2 : dir == 2 ? 0 : 1;
            else if (focus == 2)
                focus = dir == 4 ? 8 : dir == 2 ? 0 : 1;
            else if (focus == 1) {
                if (dir == 2) {
                    focus = 0;
                } else if (dir == 4) {
                    focus = 2;
                } else if (dir == 1) {
                    focus = 1;
                } else {
                    focus = 2;
                }
            }
            else
                focus = 0;
            return;
        }
        if (ok) {
            if (focus == 3)
                leave();
            else if (focus == 4 || focus == 5)
                select(Battle.clamp(selection + (focus == 4 ? -25 : 25), 0, count - 1));
            else if (focus == 2)
                toggle();
            else if (focus == 6) {
                if (dir == 1) {
                    focus = 1;
                } else {
                    focus = 0;
                }
            } else {
                focus = 1;
                upgrade();
            }
        }
    }

    void pointer(int x, int y) {
        if(step<2){if(x>=289&&y<33){leave();return;}if(x>=154&&x<284&&y>=78&&y<208){int id=(y-78)/26*5+(x-154)/26;if(step==0){if(id<count){select(id);key(10,0,true);}}else if(id<gradeCount){stoneSelection=id;key(10,0,true);}}return;}

        if(!sending()&&!running&&x>=18&&x<138){
            if(y>=138&&y<153){focus=9;if(x<93)grade=grade%5+1;else{step=1;focus=0;info=false;}requestKey="";return;}
            if(y>=155&&y<171){lucky=!lucky;focus=7;requestKey="";return;}
            if(y>=171&&y<185){protect=!protect;focus=8;requestKey="";return;}
            if(y>=185&&y<198){toggle();return;}
        }
        if (x >= 289 && y < 33) {
            leave();
            return;
        }
        if (x >= 154 && x < 284 && y >= 78 && y < 208) {
            select(page * 25 + (y - 78) / 26 * 5 + (x - 154) / 26);
            focus = 0;
            return;
        }
        if (y >= 155 && y < 176 && x >= 14 && x < 82) {
            focus = 2;
            toggle();
            return;
        }
        if (y >= 205 && x >= 150 && x < 185) {
            select(Battle.clamp(selection - 25, 0, count - 1));
            return;
        }
        if (y >= 205 && x >= 265 && x < 300) {
            select(Battle.clamp(selection + 25, 0, count - 1));
            return;
        }
        if (x >= 28 && x < 128 && y >= 194 && y < 223) {
            focus = 1;
            upgrade();
        }
    }
}
