import javax.microedition.lcdui.Image;

final class BagScreen {
    void releaseArt(){frame=cell=slot=equipOn=equipOff=fashionOn=fashionOff=arrow=input=close=preview=propsOn=propsOff=stoneIcon=statsPanel=female=male=moving=null;previewKey="";for(int i=0;i<icons.length;i++)icons[i]=null;for(int i=0;i<worn.length;i++)worn[i]=null;}

    static final int STONE_KIND = -2,GIFT_KIND=-3,VIP_KIND=-4,WING_KIND=-5;
    final PhoneCanvas c;
    int tab, filter, page, focus, selected, count;
    final int[] kinds = new int[90], items = new int[90];
    final Image[] icons = new Image[60], worn = new Image[7];
    Image frame, cell, slot, equipOn, equipOff, fashionOn, fashionOff, arrow, input, close, preview;
    String previewKey = "";
    int iconSex = -1;
    boolean info;
    int infoKind, infoItem;
    Image propsOn, propsOff;
    Image stoneIcon;
    Image statsPanel;
    Image female, male;
    Image moving;
    int sx, sy, tx, ty, pendingKind, pendingItem;
    boolean pendingRemove;
    int heldSource = -1;
    long moveAt;
    static final int PAGE = 25;
    static final int[] WORN = { 3, 4, 2, 1, 6, 5, 0 };

    BagScreen(PhoneCanvas owner) {
        c = owner;
    }

    void open() {
        tab = c.bagTab == 0 ? 0 : 1;
        filter = 0;
        page = selected = focus = 0;
        info = false;
        heldSource = -1;
        list();
    }

    void list() {
        count = c.bagLayout.fill(tab, filter, kinds, items);
        if(tab==0)for(int type=0;type<3;type++)if(c.gifts.visible(type)&&count<kinds.length){kinds[count]=GIFT_KIND;items[count++]=type;}
        if (tab == 0)for(int type=0;type<7;type++)if(c.economy.material(type)>0&&count<kinds.length){kinds[count]=STONE_KIND;items[count++]=type;}
        if(tab==0&&c.onlineAccount())for(int type=0;type<13&&count<kinds.length;type++)if(c.vip.cards[type]>0){kinds[count]=VIP_KIND;items[count++]=type;}
        if(tab==1&&c.onlineAccount())for(int type=0;type<6&&count<kinds.length;type++)if(type==5?c.economy.wings>0:(c.vip.owned&(1<<type))!=0){kinds[count]=WING_KIND;items[count++]=type;}
        page = Math.min(page, lastPage());
        selected = Math.min(selected, Math.max(0, count - 1));
        if (iconSex != c.gender) {
            for (int i = 0; i < 60; i++)
                icons[i] = null;
            iconSex = c.gender;
        }
    }

    int lastPage() {
        return heldSource >= 0 ? (tab == 0 ? 0 : 2) : Math.max(0, (count - 1) / PAGE);
    }

    void load() {
        if (frame != null)
            return;
        frame = c.combatRoomArt("bag715-frame");
        cell = c.combatRoomArt("bag715-cell");
        slot = c.combatRoomArt("bag715-worn");
        equipOn = c.combatRoomArt("bag715-equip-on");
        equipOff = c.combatRoomArt("bag715-equip-off");
        fashionOn = c.combatRoomArt("bag715-fashion-on");
        fashionOff = c.combatRoomArt("bag715-fashion-off");
        arrow = c.combatRoomArt("bag715-arrow");
        input = c.combatRoomArt("bag715-filter");
        close = c.combatRoomArt("bag715-close");
        female = c.combatRoomArt("bag720-female");
        male = c.combatRoomArt("bag720-male");
    }

    Image itemIcon(int kind, int item) {
        if (kind == 0)
            return c.weapons[item];
        int id = (kind - 1) * 10 + item;
        try {
            if (icons[id] == null)
                icons[id] = c.load("item" + c.gender + "_" + (kind - 1) + "_" + item);
        } catch (Exception ignored) {
        }
        return icons[id];
    }

    String itemName(int kind, int item) {
        if(kind==STONE_KIND)return StoneShop.NAMES[item];if(kind==GIFT_KIND)return c.gifts.name(item);
        return kind == 0 ? PhoneCanvas.WEAPON_NAMES[item] : PhoneCanvas.BAG_TABS[kind] + " " + (item + 1);
    }

    String entryName(int id){if(kinds[id]==VIP_KIND)return VipRules.cardName(items[id]);if(kinds[id]==WING_KIND)return c.vip.wingName(items[id]);return kinds[id]==STONE_KIND?StoneShop.NAMES[items[id]]:kinds[id]==GIFT_KIND?c.gifts.name(items[id]):kinds[id]>=0?itemName(kinds[id],items[id]):"Vật phẩm";}
    String itemName(int id) {
        return itemName(kinds[id], items[id]);
    }

    int wornItem(int kind) {
        return kind == 0 ? c.equipped : kind == 1 ? c.skin : c.look[kind - 2];
    }

    void character() {
        characterFor(c.wornMask, c.customLook, c.skin, c.look, c.equipped);
    }

    void characterFor(int mask, boolean custom, int skin, int[] parts, int weapon) {
        load();
        String key = c.gender + ":" + mask + ":" + skin + ":" + custom;
        for (int k = 0; k < 5; k++)
            key += ":" + parts[k];
        if (!key.equals(previewKey)) {
            previewKey = key;
            try {
                preview = c.equipmentPreview(c.gender, skin, custom, parts, mask, 64, 80);
            } catch (Exception ignored) {
                preview = null;
            }
        }
        c.vip.back(c.vip.exp(),c.vip.wing(),76,138,0,false);
          c.img(preview, 44, 58);
        for (int i = 0; i < 7; i++) {
            int[] b = box(31 + i);
            int kind = WORN[i];
            c.img(slot, b[0], b[1]);
            if ((mask & (1 << kind)) != 0 && !(moving != null && !pendingRemove && pendingKind == kind))
                c.img(itemIcon(kind, kind == 0 ? weapon : kind == 1 ? skin : parts[kind - 2]), b[0] + 1, b[1] + 1);
            else
                c.pcCenter(
                        kind == 0 ? "Vũ khí"
                                : kind == 1 ? "Bộ"
                                        : kind == 2 ? "Áo"
                                                : kind == 3 ? "Nón" : kind == 4 ? "Tóc" : kind == 5 ? "Mặt" : "Kính",
                        b[0] + 12, b[1] + 10);
        }
    }

    void draw() {
        list();
        load();
        c.all();
        c.img(c.lobbyBackground, 0, 0);
        c.img(frame, 3, 8);
        c.pcText("Túi (B)", 14, 17);
        if(c.gifts.vipUntil()>System.currentTimeMillis()/1000)c.pcText("VIP "+((c.gifts.vipUntil()-System.currentTimeMillis()/1000+3599)/3600)+"h",76,17);

        c.img(close, 289, 12);
        c.pcCenter(c.pcShort(c.playerName, 62), 46, 46);
        character();
        c.img(input, 153, 43);
        c.pcText(c.pcShort(heldSource >= 0 ? "OK: đặt • 0: mặc • R: huỷ"
                : focus<PAGE&&page*PAGE+focus<count?entryName(page*PAGE+focus):tab==0?"Vật phẩm":filter==0?"Toàn bộ":PhoneCanvas.BAG_TABS[filter], 127), 159, 48);
        for (int i = 0; i < PAGE; i++) {
            int x = 154 + i % 5 * 26, y = 64 + i / 5 * 26;
            c.img(cell, x, y);
            int id = page * PAGE + i;
            if(id<count&&kinds[id]==VIP_KIND){c.img(c.vip.cardIcon(items[id]),x+1,y+1);c.pcText(items[id]==12?"MAX":"V"+(items[id]+1),x+1,y);c.pcText(GiftBag.quantity(c.vip.cards[items[id]]),x+1,y+16);}
            else if(id<count&&kinds[id]==WING_KIND){c.img(c.vip.wingIcon(items[id]),x+1,y+1);if(c.vip.wing()==items[id])c.pcText("Đeo",x+1,y+16);}
            else if(id<count&&kinds[id]==GIFT_KIND){c.img(c.gifts.icon(items[id]),x+1,y+1);c.pcText(items[id]==0?"Lv"+c.gifts.value(0):GiftBag.quantity(c.gifts.value(items[id])),x+1,y+16);
            }else if (id < count && kinds[id] == STONE_KIND) {
                c.img(c.stoneShop.icon(items[id]), x + 1, y + 1);
                if(items[id]<5)c.pcText("L"+(items[id]+1),x+12,y+1);
                c.pcText(GiftBag.quantity(c.economy.material(items[id])), x + 1, y + 16);
            } else if (id < count && kinds[id] >= 0 && !isWorn(id)
                    && !(moving != null && pendingRemove && kinds[id] == pendingKind && items[id] == pendingItem))
                c.img(itemIcon(kinds[id], items[id]), x + 1, y + 1);
        }
        c.img(slot,16,168);if(c.vip.wing()>=0)c.img(c.vip.wingIcon(c.vip.wing()),17,169);else c.pcCenter("Cánh",28,177);
        c.vip.progress();drawStats();
        drawTabs();
        c.img(female, 17, 203); // cao thấp thanh chọn giới tính
        c.img(male, 43, 203);
        c.img(arrow, 155, 203); // cao thấp thanh mũi tên trái phải
        c.pcCenter("<", 165, 205);
        c.pcCenter((page + 1) + "/" + (lastPage() + 1), 222, 205);
        c.img(arrow, 269, 203);
        c.pcCenter(">", 279, 205);
        int[] b = box(focus);
        c.drawItemSelection(b[0], b[1], b[2], b[3], info);
        drawMove();
        if (heldSource >= 0 && focus < PAGE && page * PAGE + focus != heldSource) {
            int value = c.bagLayout.current()[c.bagLayout.start(tab) + heldSource];
            if (value >= 0 && c.bagLayout.current()[c.bagLayout.start(tab) + page * PAGE + focus] < 0)
                c.img(itemIcon(value / 10, value % 10), b[0] + 1, b[1] + 1);
        }
        if (info)
            drawInfo();
        else if (c.toastTicks > 0) {
            c.panel(20, 31, 264, 18);
            c.pcCenter(c.pcShort(c.toast, 250), 152, 36);
        }
    }

    void drawStats() {
        if (statsPanel == null)
            statsPanel = NativeUi.fit(c.combatRoomArt("item752-tooltip"), 99 * c.s, 42 * c.s, 4 * c.s);
        c.drawLevel(c.experience, 44, 130);
        c.pcText("EXP " + c.pcShort(c.expProgress(), 58), 72, 140); // cao thấp thanh exp
        c.img(statsPanel, 41, 152); // cao thấp thanh công thủ may
        int base = Progression.bonus(c.experience);
        int[] gear = ForgeRules.gear(c.wornMask, c.equipped, c.economy.bonuses());
        String[] labels = { "Công", "Thủ", "May" };
        for (int i = 0; i < 3; i++)
            c.pcText(labels[i] + " " + base + " (+" + gear[i] + ")", 48, 156 + i * 11); // cao thấp thanh công thủ may
        drawBars();
    }

    void drawBars() {
        int base = Progression.bonus(c.experience);
        int[] gear = ForgeRules.gear(c.wornMask, c.equipped, c.economy.bonuses());
        int hp = Progression.baseHp(c.experience) + gear[1]+c.vip.hp(), energy = 240 + base + gear[2];
        int[] colors = { 0xd63832, 0x3bba53 };
        String[] labels = { "HP " + hp, "TL " + energy };
        for (int i = 0; i < 2; i++) {
            int y = 203 + i * 8; // cao thấp thanh máu, TL
            c.rect(70, y, 65, 7, 0x35271d);
            c.rect(71, y + 1, 63, 5, colors[i]);
            c.pcText(c.pcShort(labels[i], 62), 72, y);
        }
    }

    void drawTabs() {
        if (propsOn == null) {
            equipOn = c.combatRoomArt("bag780-equip-on");
            equipOff = c.combatRoomArt("bag780-equip-off");
            fashionOn = c.combatRoomArt("bag780-fashion-on");
            fashionOff = c.combatRoomArt("bag780-fashion-off");
        }
        c.img(tab == 0 ? equipOn : equipOff, tab == 0 ? 290 : 292, 45);
        c.img(tab == 1 ? fashionOn : fashionOff, tab == 1 ? 290 : 292, 91); // cao thấp thanh chọn tab
    }

    void showInfo() {
        if(focus<PAGE&&page*PAGE+focus<count&&kinds[page*PAGE+focus]<-1){infoKind=kinds[page*PAGE+focus];infoItem=items[page*PAGE+focus];info=true;return;}
        if(focus==41){if(c.vip.wing()>=0){infoKind=WING_KIND;infoItem=c.vip.wing();info=true;}return;}
        int kind = -1, item = 0;
        if (focus < PAGE && page * PAGE + focus < count && kinds[page * PAGE + focus] >= 0
                && !isWorn(page * PAGE + focus)) {
            int id = page * PAGE + focus;
            kind = kinds[id];
            item = items[id];
        } else if (focus >= 31 && focus <= 37) {
            kind = WORN[focus - 31];
            if ((c.wornMask & (1 << kind)) == 0)
                return;
            item = wornItem(kind);
        }
        if (kind >= 0) {
            infoKind = kind;
            infoItem = item;
            info = true;
        }
    }

    void drawInfo() {
        if(infoKind==VIP_KIND){c.tooltip.goods(VipRules.cardName(infoItem),"Thẻ VIP",infoItem==12?"Dùng: đạt ngay VIP 12. Vật phẩm chỉ do quản trị cấp.":"Dùng: +"+((infoItem+1)*100)+" EXP VIP. Tiêu hao 1 thẻ.",box(focus));return;}
        if(infoKind==WING_KIND){if(infoItem<0)return;c.tooltip.goods(c.vip.wingName(infoItem),"Cánh",infoItem==5?"Cánh thưởng phó bản. Trang bị vĩnh viễn.":"+"+VipRules.WING_HP[infoItem]+" HP khi đeo. Cần VIP "+VipRules.WING_VIP[infoItem]+" để mua. Vĩnh viễn.",box(focus));return;}

        if(infoKind==GIFT_KIND){String desc=infoItem==0?"Thưởng: hòm Lv"+(c.gifts.value(0)+1)+", xu, đá Lv1 x50-100":infoItem==1?"Thưởng: đá Lv2 x20-70. Thẻ VIP 1 ngày: 10%.":"Kích hoạt VIP 1 ngày. Cộng dồn thời hạn.";c.tooltip.goods(c.gifts.name(infoItem),infoItem==0?"Hòm Lv"+c.gifts.value(0):"Quà",desc,box(focus));return;}
        if(infoKind==STONE_KIND){c.stoneShop.drawInfo(infoItem,box(focus));return;}
        c.tooltip.draw(infoKind, infoItem, c.mode == 6 ? c.shop.box(c.shop.focus) : box(focus));
    }

    boolean isWorn(int id) {
        int kind = kinds[id];
        if(kind==WING_KIND)return c.vip.wing()==items[id];
        if (kind < 0)
            return false;
        return (c.wornMask & (1 << kind)) != 0 && items[id] == wornItem(kind);
    }

    int[] box(int id) {
        if(id==41)return new int[]{16,168,25,27};
        if (id == 39 || id == 40)
            return new int[] { id == 39 ? 17 : 43, 203, 23, 16 }; // cao thấp thanh chọn giới tính
        if (id < PAGE)
            return new int[] { 154 + id % 5 * 26, 64 + id / 5 * 26, 25, 25 };
        if (id == 25 || id == 26)
            return new int[] { 292, id == 25 ? 45 : 91, 23, 45 };
        if (id == 27 || id == 28)
            return new int[] { id == 27 ? 155 : 269, 203, 20, 15 };
        if (id == 30)
            return new int[] { 153, 43, 135, 18 };
        if (id == 38)
            return new int[] { 289, 12, 21, 19 };
        int i = id - 31;
        return new int[] { i < 4 ? 16 : 113, 56 + (i < 4 ? i : i - 4) * 27, 25, 27 };
    }

    boolean valid(int id) {
        if (id == 29)
            return false;
        return id >= 0 && id <= 41;
    }

    void choose(int id) {
        focus = id;if(id==41){infoKind=WING_KIND;infoItem=c.vip.wing();return;}
        if (id < PAGE && page * PAGE + id < count) {
            selected = page * PAGE + id;
            infoKind = kinds[selected];
            infoItem = items[selected];
        } else if (id >= 31 && id <= 37) {
            infoKind = WORN[id - 31];
            infoItem = wornItem(infoKind);
        }
    }

    int nearestBag(int[] anchor) {
        int best = -1, score = 999999;
        for (int i = 0; i < PAGE; i++)
            if (page * PAGE + i < count && kinds[page * PAGE + i] >= 0 && !isWorn(page * PAGE + i)) {
                int[] b = box(i);
                int cost = Math.abs(b[1] * 2 + b[3] - anchor[1] * 2 - anchor[3]) * 4 + Math.abs(b[0] - anchor[0]);
                if (cost < score) {
                    score = cost;
                    best = i;
                }
            }
        return best >= 0 ? best
                : Battle.clamp((anchor[1] + anchor[3] / 2 - 64) / 26, 0, 4) * 5 + (anchor[0] < 154 ? 0 : 4);
    }

    int nearestWorn(int[] a) {
        int best = 31, score = 999999;
        for (int id = 31; id <= 37; id++) {
            int[] b = box(id);
            int cost = Math.abs(b[1] * 2 + b[3] - a[1] * 2 - a[3]) * 7 + Math.abs(b[0] - a[0]);
            if (cost < score) {
                best = id;
                score = cost;
            }
        }
        return best;
    }

    void move(int dir) {
        if (dir == 1 && (focus == 25 || focus == 26) || dir == 2 && focus >= 31 && focus <= 37) {
            choose(nearestBag(box(focus)));
            return;
        }
        if (focus < PAGE) {
            int row = focus / 5, col = focus % 5;
            if (dir == 2) {
                if (col < 4)
                    choose(focus + 1);
                else if (page < lastPage()) {
                    turnPage(1);
                    choose(row * 5);
                }
                return;
            }
            if (dir == 1) {
                if (col > 0)
                    choose(focus - 1);
                else if (page > 0) {
                    turnPage(-1);
                    choose(row * 5 + 4);
                } else
                    choose(nearestWorn(box(focus)));
                return;
            }
            if (dir == 4) {
                choose(row > 0 ? focus - 5 : 30);
                return;
            }
            if (dir == 8) {
                choose(row < 4 ? focus + 5 : col < 2 ? 27 : 28);
                return;
            }
        }
        int[] f = box(focus);
        int x = f[0] * 2 + f[2], y = f[1] * 2 + f[3], best = focus, score = 999999;
        for (int id = 0; id <= 41; id++)
            if (valid(id) && id != focus) {
                int[] b = box(id);
                int dx = b[0] * 2 + b[2] - x, dy = b[1] * 2 + b[3] - y,
                        along = dir == 1 ? -dx : dir == 2 ? dx : dir == 4 ? -dy : dy,
                        cross = dir == 1 || dir == 2 ? dy : dx;
                if (along > 0) {
                    int cost = along + Math.abs(cross) * 7;
                    if (cost < score) {
                        score = cost;
                        best = id;
                    }
                }
            }
        choose(best);
    }

    void switchTab(int value) {
        heldSource = -1;
        info = false;
        tab = value;
        filter = page = selected = 0;
        focus = 0;
        list();
    }

    void turnPage(int delta) {
        page = Battle.clamp(page + delta, 0, lastPage());
        selected = page * PAGE;
    }

    void change(int kind, int item, boolean remove) {
        if (c.net.pending.length() > 0
                || c.net.busy && !c.net.operation.equals("sync") && !c.net.operation.equals("socialpulse")
                        && !c.net.operation.equals("presence") && !c.net.operation.equals("rooms"))
            return;
        if (c.onlineAccount() && (c.net.inBattle || c.net.roomId > 0 && c.net.phase > 0)) {
            c.message("Không thay đồ khi đang trong trận");
            return;
        }
        if (!remove && !c.inventory.owns(kind, item))
            return;
        pendingKind = kind;
        pendingItem = item;
        pendingRemove = remove;
        if (c.onlineAccount()) {
            if (!c.net.equipmentCap) {
                c.message("Cập nhật server 7.1.8");
                return;
            }
            c.net.equipKind = kind;
            c.net.equipItem = remove ? -1 : item;
            c.net.request("equip");
        } else {
            c.setEquipment(kind, item, remove);
            confirmMove();
        }
    }

    void confirmMove() {
        if (c.mode != 8) {
            moving = null;
            return;
        }
        int slotId = 37;
        for (int i = 0; i < 7; i++)
            if (WORN[i] == pendingKind)
                slotId = 31 + i;
        int[] wear = box(slotId);
        if (pendingRemove) {
            tab = pendingKind == 0 ? 0 : 1;
            filter = 0;
            list();
        }
        int id = -1;
        for (int i = 0; i < count; i++)
            if (kinds[i] == pendingKind && items[i] == pendingItem) {
                id = i;
                break;
            }
        if (id < 0)
            return;
        page = id / PAGE;
        selected = id;
        int[] bag = box(id % PAGE);
        sx = pendingRemove ? wear[0] : bag[0];
        sy = pendingRemove ? wear[1] : bag[1];
        tx = pendingRemove ? bag[0] : wear[0];
        ty = pendingRemove ? bag[1] : wear[1];
        moving = itemIcon(pendingKind, pendingItem);
        moveAt = System.currentTimeMillis();
        focus = pendingRemove ? id % PAGE : slotId;
    }

    void drawMove() {
        if (moving == null)
            return;
        int t = (int) (System.currentTimeMillis() - moveAt);
        if (t >= 320) {
            moving = null;
            return;
        }
        t = t * t * (960 - 2 * t) / (320 * 320);
        c.img(moving, sx + (tx - sx) * t / 320, sy + (ty - sy) * t / 320);
    }

    void equip() {
        int id = page * PAGE + focus;
        if (focus < PAGE && id < count && kinds[id] >= 0 && !isWorn(id)) {
            selected = id;
            change(kinds[id], items[id], false);
        }
    }

    void activate() {
        if(focus==41){if(c.vip.wing()>=0)c.vip.submit("vipequip",-1);return;}
        if(focus<PAGE&&page*PAGE+focus<count){int id=page*PAGE+focus;if(kinds[id]==VIP_KIND){c.vip.submit("vipuse",items[id]);return;}if(kinds[id]==WING_KIND){c.vip.submit("vipequip",c.vip.wing()==items[id]?-1:items[id]);return;}}

        if(focus<PAGE&&page*PAGE+focus<count&&kinds[page*PAGE+focus]==GIFT_KIND){heldSource=-1;info=false;c.gifts.use(items[page*PAGE+focus]);return;}
        if(focus<PAGE&&page*PAGE+focus<count&&kinds[page*PAGE+focus]==STONE_KIND){int m=items[page*PAGE+focus];heldSource=-1;infoKind=STONE_KIND;infoItem=m;info=!info;return;}

        if (heldSource >= 0 && focus < PAGE) {
            int target = page * PAGE + focus;
            if (target == heldSource) {
                selected = heldSource;
                heldSource = -1;
                info = false;
                equip();
            } else if (c.bagLayout.move(tab, heldSource, target)) {
                heldSource = -1;
                info = false;
                list();
            } else
                c.message("Hãy chọn ô trống để đặt đồ");
            return;
        }
        if ((focus < PAGE || focus >= 31 && focus <= 37) && !info) {
            showInfo();
            if (info && focus < PAGE)
                heldSource = page * PAGE + focus;
            return;
        }
        info = false;
        if (focus < PAGE)
            equip();
        else if (focus == 25 || focus == 26)
            switchTab(focus - 25);
        else if (focus == 27 || focus == 28)
            turnPage(focus == 27 ? -1 : 1);
        else if (focus == 30) {
            filter = filter == 0 ? 1 : filter == 6 ? 0 : filter + 1;
            page = selected = 0;
            list();
        } else if (focus == 39 || focus == 40)
            changeGender(focus == 39 ? 1 : 0);
        else if (focus == 38)
            leave();
        else if (focus >= 31 && focus <= 37) {
            int kind = WORN[focus - 31];
            if ((c.wornMask & (1 << kind)) != 0)
                change(kind, wornItem(kind), true);
        }
    }

    void changeGender(int sex) {
        heldSource = -1;
        info = false;
        if (sex == c.gender)
            return;
        if (c.onlineAccount()) {
            if (c.net.inBattle || c.net.phase > 0 && c.net.roomId > 0) {
                c.message("Không đổi nhân vật trong trận");
                return;
            }
            if (!c.net.characterCap) {
                c.message("Cập nhật server 7.2.0");
                return;
            }
            if (c.net.busy || c.net.pending.length() > 0)
                return;
            c.net.genderDraft = sex;
            c.net.request("gender");
        } else {
            c.gender = sex;
            if (!c.inventory.owns(1, c.skin))
                c.skin = 0;
            for (int k = 0; k < 5; k++)
                if (!c.inventory.owns(k + 2, c.look[k]))
                    c.look[k] = 0;
            if (!c.inventory.owns(1, c.skin))
                c.wornMask &= ~2;
            for (int k = 0; k < 5; k++)
                if (!c.inventory.owns(k + 2, c.look[k]))
                    c.wornMask &= ~(1 << (k + 2));
            c.customLook = (c.wornMask & 2) == 0;
            c.loadLook();
            c.saveProfile();
            c.saveBag();
            c.inventory.save();
            previewKey = "";
            iconSex = -1;
            list();
        }
    }

    void leave() {
        heldSource = -1;
        c.mode = c.bagReturnMode;
        release();
    }

    void release() {
        propsOn = propsOff = null;
        frame = cell = slot = equipOn = equipOff = fashionOn = fashionOff = arrow = input = close = preview = moving = female = male = null;
        previewKey = "";
        info = false;
        for (int i = 0; i < 60; i++)
            icons[i] = null;
    }

    void key(int key, int dir, boolean ok) {
        if((key=='w'||key=='W')&&c.onlineAccount()&&c.economy.wings>0){c.vip.submit("vipequip",c.vip.wing()==5?-1:5);return;}
        if (key == 'l' || key == 'L' || key == 'p' || key == 'P') {
            switchTab(key == 'p' || key == 'P' ? 0 : 1);
            return;
        }
        if (heldSource >= 0 && (key == -7 || key == 'r' || key == 'R')) {
            heldSource = -1;
            info = false;
            return;
        }
        if (heldSource >= 0 && key == '0') {
            int source = heldSource;
            page = source / PAGE;
            choose(source % PAGE);
            activate();
            return;
        }
        if (key == -6) {
            if (info)
                info = false;
            else
                showInfo();
            return;
        }
        if (info) {
            if (dir != 0) {
                info = false;
                move(dir);
            } else if (ok) {
                activate();
            } else if (key == -6 || key == -7)
                info = false;
            return;
        }
        if (key == -7 || key == 'b' || key == 'B') {
            leave();
            return;
        }
        if (key == '#') {
            switchTab((tab + 1) % 2);
            return;
        }
        if (dir != 0)
            move(dir);
        else if (ok)
            activate();
    }

    void pointer(int x, int y) {
        if (x >= 290 && y >= 45 && y < 92) {
            switchTab(0);
            return;
        }
        if (x >= 290 && y >= 91 && y < 136) {
            switchTab(1);
            return;
        }
        if (info) {
            info = false;
            return;
        }
        for (int id = 41; id >= 0; id--)
            if (valid(id)) {
                int[] b = box(id);
                if (x >= b[0] && x < b[0] + b[2] && y >= b[1] && y < b[1] + b[3]) {
                    boolean again = focus == id;
                    choose(id);
                    if (id >= PAGE || again || heldSource >= 0)
                        activate();
                    return;
                }
            }
        if (y >= 232)
            leave();
    }
}
