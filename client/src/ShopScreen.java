import javax.microedition.lcdui.*;

final class ShopScreen {
    void releaseArt(){frame=card=cartArt=buyArt=previous=next=money=coin=close=curtain=subTab=activeSubTab=null;for(int i=0;i<mainTabs.length;i++)for(int j=0;j<2;j++)mainTabs[i][j]=null;}

    final PhoneCanvas c;
    int tab, kind, page, selected, focus, trialMask, trialSkin, trialWeapon;
    boolean trialCustom;
    final int[] cart = new int[7], sentCart = new int[7], trialLook = new int[5];
    Image frame, card, cartArt, buyArt, previous, next, money, coin, close;
    Image curtain, subTab, activeSubTab;
    final Image[][] mainTabs = new Image[4][2];

    final Command exchangeOK = new Command("Đổi", Command.OK, 1), exchangeBack = new Command("Hủy", Command.BACK, 2);
    static final int[] TAB_VALUES={0,1,3,4,2},TAB_IDS={6,7,19,43,18};
    boolean headerFocus(){return focus==6||focus==7||focus==19||focus==43||focus==18;}
    static final int[] EQUIPMENT = { 0, 2, 3, 6 }, BEAUTY = { 4, 5, 1 };

    ShopScreen(PhoneCanvas owner) {
        c = owner;
    }

    void open() {
        tab = kind = page = selected = focus = 0;
        resetTrial();
        if (c.onlineAccount() && c.net.shopCap && !c.net.busy && c.net.pending.length() == 0) {
            c.inventory.onlineLoaded = false;
            c.net.request("shop");
        }
    }

    void resetTrial() {
        trialMask = c.wornMask;
        trialSkin = c.skin;
        trialWeapon = c.equipped;
        trialCustom = c.customLook;
        for (int i = 0; i < 5; i++)
            trialLook[i] = c.look[i];
        rebuildCart();
    }

    int[] categories() {
        return tab == 0 ? EQUIPMENT : BEAUTY;
    }

    int count() {
        return kind == 0 ? PhoneCanvas.ACTIVE_WEAPONS.length : 10;
    }

    int item(int index) {
        return kind == 0 ? PhoneCanvas.ACTIVE_WEAPONS[index] : index;
    }

    void load() {
        if (frame != null)
            return;
        c.bag.load();
        frame = c.combatRoomArt("shop716-frame");
        card = c.combatRoomArt("shop718-cell");
        cartArt = c.combatRoomArt("shop716-cart");
        buyArt = c.combatRoomArt("shop716-buy");
        previous = c.combatRoomArt("shop716-previous");
        next = c.combatRoomArt("shop716-next");
        money = c.combatRoomArt("wallet-money");
        coin = c.combatRoomArt("wallet-coin");
        close = c.combatRoomArt("bag715-close");
        curtain = c.combatRoomArt("shop-curtain");
        subTab = c.combatRoomArt("shop-sub-normal");
        activeSubTab = c.combatRoomArt("shop-sub-active");
        String[] names = {"equipment", "beauty", "props", "exchange"};
        for (int i = 0; i < names.length; i++) {
            mainTabs[i][0] = NativeUi.fit(c.combatRoomArt("shop-tab-" + names[i]),31*c.s,20*c.s,0);
            mainTabs[i][1] = NativeUi.fit(c.combatRoomArt("shop-tab-" + names[i] + "-active"),31*c.s,20*c.s,0);
        }
    }

    Image icon(int k, int i) {
        return c.bag.itemIcon(k, i);
    }

    String name(int k, int i) {
        return c.bag.itemName(k, i);
    }

    int trialItem(int k) {
        return k == 0 ? trialWeapon : k == 1 ? trialSkin : trialLook[k - 2];
    }

    void rebuildCart() {
        for (int k = 0; k < 7; k++) {
            int i = trialItem(k);
            cart[k] = (trialMask & (1 << k)) != 0 && !c.inventory.owns(k, i) ? 1 << i : 0;
        }
    }

    void trial(int index) {
        selected = index;
        int i = item(index);
        trialMask = ItemRules.maskAfter(trialMask, kind, false);
        if (kind == 0)
            trialWeapon = i;
        else if (kind == 1)
            trialSkin = i;
        else
            trialLook[kind - 2] = i;
        trialCustom = (trialMask & 2) == 0;
        rebuildCart();
    }

    boolean ready() {
        return !c.onlineAccount() || c.net.equipmentCap && c.inventory.onlineLoaded;
    }

    int total() {
        return ready() ? c.inventory.total(cart) : 0;
    }

    int amount() {
        int n = 0;
        for (int k = 0; k < 7; k++)
            if (cart[k] != 0)
                n++;
        return n;
    }

    void draw() {
        load();
        rebuildCart();
        c.all();
        c.img(c.lobbyBackground, 0, 0);
        c.img(frame, 3, 8);
        c.img(curtain, 6, 9);
        c.img(close, 289, 12);
        c.pcCenter("Phòng thử", 77, 46);
        c.bag.characterFor(trialMask, trialCustom, trialSkin, trialLook, trialWeapon);
        if (tab < 2)
            c.pcCenter(c.pcShort(name(kind, item(selected)), 68), 77, 182);
        for(int i=0;i<5;i++){int value=TAB_VALUES[i],x=151+i*32;if(value==4)c.ui.button("VIP",x,38,31,20,tab==4);else c.img(mainTabs[value==3?2:value==2?3:value][tab==value?1:0],x,38);}
        if(tab==4){c.vip.drawShop();}
        else if (tab == 2) {
            c.exchangePanel.draw();
        } else if (tab == 3) {
            c.stoneShop.draw();
        } else {
            int[] cats = categories();
            for (int i = 0; i < cats.length; i++) {
                int x = 151 + i * 40;
                c.img(kind == cats[i] || focus == 8 + i ? activeSubTab : subTab, x, 63);
                c.pcCenter(
                        cats[i] == 0 ? "Vũ khí"
                                : cats[i] == 1 ? "Bộ"
                                        : cats[i] == 2 ? "Áo" : cats[i] == 3 ? "Nón" : PhoneCanvas.BAG_TABS[cats[i]],
                        x + 19, 68);
            }
            for (int slot = 0; slot < 6; slot++) {
                int id = page * 6 + slot;
                if (id >= count())
                    break;
                int i = item(id), x = 151 + slot % 2 * 80, y = 87 + slot / 2 * 41;
                c.img(card, x, y);
                c.img(icon(kind, i), x + 2, y + 7);
                c.pcText(c.pcShort(name(kind, i), 43), x + 29, y + 4);
                if (c.inventory.owns(kind, i))
                    c.pcText("Đã có", x + 29, y + 15);
                c.img(money, x + 29, y + 27);
                c.pcText("" + ShopCatalog.price(kind, i), x + 43, y + 27);
            }
            c.img(previous, 151, 213);
            c.pcCenter((page + 1) + "/" + ((count() + 5) / 6), 229, 217);
            c.img(next, 284, 213);
        }
        c.img(coin, 12, 196);
        c.pcText("" + c.coins, 26, 196);
        c.img(money, 12, 208);
        c.pcText("" + c.shoppingMoney, 26, 208);
        c.pcText("Tổng", 12, 221);
        c.img(money, 43, 220);
        c.pcText("" + (tab == 4 ? VipRules.WING_PRICE[c.vip.shopFocus] : tab == 3 ? c.stoneShop.price() : total()), 57, 221);
        if(tab==3)c.pcText("Thông tin",4,229);
        if (tab != 2) {
            c.ui.button("Mua(B)",106,201,37,21,false);
            if (focus == 14 && tab == 3)
                c.drawItemSelection(106, 201, 37, 21, false);
        }
        int[] b = box(focus);
        boolean tabFocus = headerFocus()
                || focus >= 8 && focus <= 11;
        if (!tabFocus && (tab != 3 || focus >= 6) && (tab != 2 || focus != 17))
            c.drawItemSelection(b[0], b[1], b[2], b[3], true);
        if(tab==3&&c.stoneShop.infoType>=0)c.stoneShop.drawInfo(c.stoneShop.infoType,box(c.stoneShop.focus%6));
        else if (c.bag.info)
            c.bag.drawInfo();
        else if (c.toastTicks > 0) {
            c.panel(16, 27, 288, 17);
            c.pcCenter(c.pcShort(c.toast, 276), 160, 32);
        }
    }

    int[] box(int id) {
        if (id < 6)
            return new int[] { 151 + id % 2 * 80, 87 + id / 2 * 41, 76, 40 };
        for(int i=0;i<5;i++)if(id==TAB_IDS[i])return new int[]{151+i*32,38,31,20};
        if (id >= 8 && id <= 11)
            return new int[] { 151 + (id - 8) * 40, 63, 38, 20 };
        if (id == 12 || id == 13)
            return new int[] { id == 12 ? 151 : 284, 213, 22, 16 };
        if (id == 14)
            return new int[] { 106, 201, 37, 21 };
        if (id == 15)
            return new int[] { 289, 12, 21, 19 };
        if (id == 17)
            return new int[] { 178, 161, 48, 20 };
        return c.bag.box(31 + id - 20);
    }

    boolean valid(int id) {
        if(id==43)return true;
        if (id == 16)
            return false;
        if (id == 19)
            return true;
        if (id >= 20)
            return id <= 26;
        if (id == 17)
            return tab == 2;
        if (id < 6)
            return tab == 3 ? c.stoneShop.focus/6*6+id<7 : tab != 2 && page * 6 + id < count();
        if (id >= 8 && id <= 11)
            return tab < 2 && id - 8 < categories().length;
        if (id == 12 || id == 13)
            return tab != 2;
        return true;
    }

    int header() {
        return tab == 4 ? 43 : tab == 3 ? 19 : tab == 2 ? 18 : 6 + tab;
    }

    void hover() {
        if (headerFocus()) {
            int value = focus == 43 ? 4 : focus == 19 ? 3 : focus == 18 ? 2 : focus - 6;
            if (tab != value)
                selectTab(value);
        } else if (focus >= 8 && focus <= 11 && tab < 2) {
            int value = categories()[focus - 8];
            if (kind != value)
                chooseCategory(value);
        } else if (focus < 6) {
            if(tab==3)c.stoneShop.focus=c.stoneShop.focus/6*6+focus;
            else selected = page * 6 + focus;
        }
    }

    int categoryFocus() {
        int[] cats = categories();
        for (int i = 0; i < cats.length; i++)
            if (cats[i] == kind)
                return 8 + i;
        return 8;
    }

    void move(int dir) {
        if (headerFocus()) {
            if(dir==1||dir==2){int at=0;for(int i=0;i<5;i++)if(TAB_VALUES[i]==tab)at=i;at=Battle.clamp(at+(dir==1?-1:1),0,4);focus=TAB_IDS[at];hover();}
            else if(dir==8){focus=tab==4?c.vip.shopFocus:tab==3?c.stoneShop.focus%6:tab==2?17:categoryFocus();}
            return;
        }
        if (focus >= 8 && focus <= 11) {
            if (dir == 4)
                focus = header();
            else if (dir == 8) {
                focus = 0;
                hover();
            } else if (dir == 1 || dir == 2) {
                focus = 8 + Battle.clamp(focus - 8 + (dir == 1 ? -1 : 1), 0, categories().length - 1);
                hover();
            }
            return;
        }
        if (focus < 6) {
            int row = focus / 2, col = focus % 2;
            if (dir == 4) {
                focus = row > 0 ? focus - 2 : categoryFocus();
                hover();
                return;
            }
            if (dir == 8) {
                int candidate = focus + 2;
                focus = candidate < 6 && valid(candidate) ? candidate : col == 0 ? 12 : 13;
                hover();
                return;
            }
            if (dir == 1 || dir == 2) {
                if (dir == 1 && col == 1)
                    focus--;
                else if (dir == 2 && col == 0 && valid(focus + 1))
                    focus++;
                else if (dir == 1 && page > 0) {
                    page--;
                    focus = Math.min(row * 2 + 1, count() - 1 - page * 6);
                } else if (dir == 2 && page < (count() - 1) / 6) {
                    page++;
                    focus = Math.min(row * 2, count() - 1 - page * 6);
                } else if (dir == 1)
                    focus = 20 + c.bag.nearestWorn(box(focus)) - 31;
                hover();
                return;
            }
        }
        int[] f = box(focus);
        int x = f[0] * 2 + f[2], y = f[1] * 2 + f[3], best = focus, score = 999999;
        for (int id = 0; id <= 26; id++)
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
        focus = best;
        hover();
    }

    void chooseCategory(int k) {
        kind = k;
        page = selected = 0;
    }

    void selectTab(int value) {
        c.bag.info = false;
        tab = value;
        if (tab < 2)
            chooseCategory(categories()[0]);
    }

    void toggle() {
        trial(selected);
    }

    void checkout() {
        if (!ready()) {
            c.message("Chờ tải shop hoặc cập nhật server 7.1.8");
            return;
        }
        if (c.net.busy || c.net.pending.length() > 0)
            return;
        rebuildCart();
        if (amount() == 0) {
            c.message(trialMask == 0 ? "Hãy chọn đồ mặc thử" : "Các món đang thử đã có trong túi");
            return;
        }
        if (c.onlineAccount()) {
            if (c.net.roomId > 0) {
                c.message("Mua đồ ở sảnh trước");
                return;
            }
            for (int k = 0; k < 7; k++)
                sentCart[k] = cart[k];
            c.net.request("checkout");
        } else if (c.inventory.buy(cart))
            bought();
    }

    void bought() {
        rebuildCart();
        c.message("Đã mua đồ vào túi");
    }

    void exchange() {
        if (c.onlineAccount() && c.net.roomId > 0) {
            c.message("Đổi xu ở sảnh trước");
            return;
        }
        if (c.onlineAccount() && !c.net.equipmentCap) {
            c.message("Cập nhật server 7.1.8");
            return;
        }
        c.typing.open("Tiền muốn đổi (100 xu = 1)",""+c.exchangePanel.amount,4,true,false,
            new InlineText.Done(){
                public void accept(String value){
                    if(value.length()==0)c.popup.notice("Đổi xu","Nhập số tiền hợp lệ");
                    else c.exchangePanel.convert(Integer.parseInt(value));
                }
            });
    }

    void activate() {
        if(tab==3&&(focus==12||focus==13)){c.stoneShop.focus=focus==12?0:6;return;}
        if (tab == 3 && focus == 14) {
            c.stoneShop.buy();
            return;
        }
        if ((c.net.busy || c.net.pending.length() > 0) && focus != 15)
            return;
        if (focus < 6)
            trial(page * 6 + focus);
        else if (headerFocus())
            selectTab(focus==43?4:focus == 19 ? 3 : focus == 18 ? 2 : focus - 6);
        else if (focus >= 8 && focus <= 11)
            chooseCategory(categories()[focus - 8]);
        else if (focus == 12 || focus == 13) {
            page = Battle.clamp(page + (focus == 12 ? -1 : 1), 0, (count() - 1) / 6);
            selected = page * 6;
        } else if (focus == 14)
            checkout();
        else if (focus == 15)
            leave();
        else if (focus == 17)
            exchange();
        else if (focus >= 20) {
            int k = BagScreen.WORN[focus - 20];
            trialMask = ItemRules.maskAfter(trialMask, k, true);
            trialCustom = (trialMask & 2) == 0;
            rebuildCart();
        }
    }

    void showInfo() {
        int k = kind, i = -1;
        if (focus < 6 && tab != 2)
            i = item(page * 6 + focus);
        else if (focus >= 20 && focus <= 26) {
            k = BagScreen.WORN[focus - 20];
            if ((trialMask & (1 << k)) != 0)
                i = trialItem(k);
        }
        if (i >= 0) {
            c.bag.infoKind = k;
            c.bag.infoItem = i;
            c.bag.info = true;
        }
    }

    void key(int key, int dir, boolean ok) {
        if(tab==4&&!headerFocus()){c.vip.shopKey(key,dir,ok);return;}
        if(key=='b'||key=='B'){if(tab==4){c.vip.buy();return;}c.stoneShop.infoType=-1;if(tab==3)c.stoneShop.buy();else checkout();return;}
        if (tab == 3 && (focus < 6 || focus == 12 || focus == 13 || focus == 14)) {
            c.stoneShop.key(key, dir, ok);
            return;
        }
        if (tab == 2 && focus != 6 && focus != 7 && focus != 18) {
            c.exchangePanel.key(key, dir, ok);
            return;
        }
        if (c.bag.info) {
            c.bag.info = false;
            if (dir != 0)
                move(dir);
            return;
        }
        if (key == -6) {
            showInfo();
            return;
        }
        if (key == -7) {
            leave();
            return;
        }
        if (dir != 0)
            move(dir);
        else if (ok || key == -6)
            activate();
    }

    void pointer(int x, int y) {
        if (y >= 38 && y < 59 && x >= 151) {
            int at=Math.min(4,(x-151)/32);int value=TAB_VALUES[at];focus=TAB_IDS[at];
            selectTab(value);
            return;
        }
        if(tab==4){if(x>=289&&y<33){leave();return;}for(int i=0;i<5;i++){int[] b=box(i);if(x>=b[0]&&x<b[0]+b[2]&&y>=b[1]&&y<b[1]+b[3]){c.vip.shopFocus=i;c.vip.buy();return;}}if(x>=106&&x<143&&y>=201&&y<222)c.vip.buy();return;}
        if (tab == 3 && c.stoneShop.pointer(x, y))
            return;
        if (tab == 3 && x >= 106 && x < 143 && y >= 201 && y < 222) {
            focus = 14;
            c.stoneShop.buy();
            return;
        }
        if (tab == 2 && c.exchangePanel.pointer(x, y))
            return;
        if (c.bag.info) {
            c.bag.info = false;
            return;
        }
        for (int id = 26; id >= 0; id--)
            if (valid(id)) {
                int[] b = box(id);
                if (x >= b[0] && x < b[0] + b[2] && y >= b[1] && y < b[1] + b[3]) {
                    focus = id;
                    hover();
                    activate();
                    return;
                }
            }
        if (y >= 232)
            leave();
    }

    void leave() {
        c.mode = c.shopReturnMode;
        // frame = card = cartArt = previous = next = money = coin = close = null;
        frame = card = cartArt = buyArt = previous = next = money = coin = close = null;
        c.bag.release();
    }
}
