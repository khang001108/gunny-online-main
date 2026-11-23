import java.io.*;
import javax.microedition.lcdui.*;

/**
 * Native artwork with six portrait cards, friend requests and allied
 * invitations.
 */
final class OnlineSocial implements CommandListener {
    final OnlineClient net;
    final PhoneCanvas c;
    boolean friendsPopup;
    String searchQuery = "";
    boolean opened, inviting, answer, offlineInvite;
    int nav, footer;
    int tab, page, selected, myId, target, action, bot, inviteSeen;
    long poll;
    int count;
    final int[] ids = new int[128], sex = new int[128], skin = new int[128], experience = new int[128];
    final int[][] looks = new int[128][5];
    final boolean[] custom = new boolean[128], online = new boolean[128];
    final String[] names = new String[128];
    Image close;

    final Command accept = new Command("Vào đội", Command.OK, 1);

    OnlineSocial(OnlineClient n) {
        net = n;
        c = n.c;
    }

    Image infoFrame;
    int infoFocus;
    boolean itemInfo;
    boolean viewing;
    boolean returnToRanking; // true only when the player profile came from Ranking793
    String infoName;
    int infoVip,infoWing=-1;
    int infoSex, infoSkin, infoMask, infoWeapon, infoExp, infoWins, infoRank;
    final int[] infoForge=new int[3],infoRanks=new int[130];
    final int[] infoLook = new int[5];
    Image infoHero;
    final Image[] infoIcons = new Image[7];

    void readInfo(DataInputStream in) throws IOException {
        infoName = in.readUTF();
        infoSex = in.readUnsignedByte();
        infoSkin = in.readUnsignedByte();
        infoMask = in.readUnsignedByte();
        for (int k = 0; k < 5; k++)
            infoLook[k] = in.readUnsignedByte();
        infoWeapon = in.readUnsignedByte();
        infoExp = in.readInt();
        infoWins = in.readInt();
        infoRank = in.readInt();
        if(net.expansionCap)for(int i=0;i<3;i++)infoForge[i]=in.readUnsignedShort();
        for(int i=0;i<130;i++)infoRanks[i]=0;
        if(in.available()>=130)for(int i=0;i<130;i++)infoRanks[i]=in.readUnsignedByte();
        if(net.vipCap){infoVip=in.readInt();infoWing=in.readByte();}
        infoHero = c.equipmentPreview(infoSex, infoSkin, (infoMask & 2) == 0, infoLook, infoMask, 64, 80);
        for (int k = 0; k < 7; k++) {
            infoIcons[k] = null;
            if ((infoMask & (1 << k)) != 0)
                infoIcons[k] = k == 0 ? c.weapons[infoWeapon]
                        : c.load("item" + infoSex + "_" + (k - 1) + "_" + (k == 1 ? infoSkin : infoLook[k - 2]));
        }
        profileLoading = false;
        profileError = "";
        itemInfo = false;
        infoFocus = 0;
        for (int n = 0; n < 7; n++)
            if (infoIcons[BagScreen.WORN[n]] != null) {
                infoFocus = n;
                break;
            }
    }

    int[] infoBox(int n) {
        int[] b = c.bag.box(n==7?41:31+n);
        return new int[] { b[0] + 84, b[1], b[2], b[3] };
    }

    void infoMove(int dir) {
        int[] a = infoBox(infoFocus);
        int best = infoFocus, score = 999999;
        for (int n = 0; n < 8; n++)
            if (n != infoFocus && infoHas(n)) {
                int[] b = infoBox(n);
                int dx = b[0] * 2 + b[2] - a[0] * 2 - a[2], dy = b[1] * 2 + b[3] - a[1] * 2 - a[3],
                        along = dir == 1 ? -dx : dir == 2 ? dx : dir == 4 ? -dy : dy,
                        cross = dir == 1 || dir == 2 ? dy : dx;
                if (along > 0) {
                    int value = along + Math.abs(cross) * 4;
                    if (value < score) {
                        score = value;
                        best = n;
                    }
                }
            }
        infoFocus = best;
        itemInfo = false;
    }

    boolean infoHas(int n){return n==7?infoWing>=0:infoIcons[BagScreen.WORN[n]]!=null;}
    void infoToggle() {
        if (infoHas(infoFocus))
            itemInfo = !itemInfo;
    }

    Image infoStats;
    void drawInfo() {
        c.bag.load();
        if (infoFrame == null)
            infoFrame = c.combatRoomArt("friend763-frame");
        c.all();
        c.img(c.lobbyBackground, 0, 0);
        c.img(infoFrame, 87, 8);
        c.pcText("Thông tin", 98, 17);
        c.img(c.bag.close, 209, 12);
        if (profileLoading || profileError.length() > 0) {
            c.pcCenter(profileLoading ? "Đang tải..." : profileError, 161, 110);
            return;
        }
        c.pcCenter(c.pcShort(infoName, 117), 161, 46);
        c.vip.back(infoVip,infoWing,160,138,0,false);c.img(infoHero, 128, 58);
        for (int n = 0; n < 7; n++) {
            int k = BagScreen.WORN[n];
            int[] b = infoBox(n);
            c.img(c.bag.slot, b[0], b[1]);
            if (infoIcons[k] != null)
                c.img(infoIcons[k], b[0] + 1, b[1] + 1);
            else c.pcCenter(k==0?"Vũ khí":k==1?"Bộ":k==2?"Áo":k==3?"Nón":k==4?"Tóc":k==5?"Mặt":"Kính",b[0]+12,b[1]+10);
        }
        c.img(c.bag.slot,100,168);if(infoWing>=0)c.img(c.vip.wingIcon(infoWing),101,169);else c.pcCenter("Cánh",112,177);
        c.vip.badge(infoVip,172,54);c.pcCenter(VipRules.need(infoVip)==0?"MAX":VipRules.within(infoVip)+"/"+VipRules.need(infoVip),182,69);
        c.drawLevel(infoExp,128,130);c.pcText(Progression.level(infoExp)==70?"EXP MAX":"EXP "+Progression.within(infoExp)+"/"+Progression.limit(infoExp),156,140);
        if(infoStats==null)infoStats=NativeUi.fit(c.combatRoomArt("item752-tooltip"),99*c.s,42*c.s,4*c.s);
        c.img(infoStats,125,152);String[] labels={"Công","Thủ","May"};for(int i=0;i<3;i++)c.pcText(labels[i]+" "+Progression.bonus(infoExp)+" (+"+infoForge[i]+")",132,156+i*11);
        c.img(c.bag.female,101,203);c.img(c.bag.male,127,203);
        int hp=Progression.baseHp(infoExp)+infoForge[1]+VipRules.hp(infoWing),energy=240+Progression.bonus(infoExp)+infoForge[2];
        for(int i=0;i<2;i++){int y=203+i*8;c.rect(154,y,65,7,0x35271d);c.rect(155,y+1,63,5,i==0?0xd63832:0x3bba53);c.pcText((i==0?"HP "+hp:"TL "+energy),156,y);}
        int[] b = infoBox(infoFocus);
        if (infoHas(infoFocus))
            c.drawItemSelection(b[0], b[1], b[2], b[3], itemInfo);
        if(itemInfo&&infoFocus==7){c.tooltip.goods(c.vip.wingName(infoWing),"Cánh","+"+VipRules.hp(infoWing)+" HP khi trang bị.",infoBox(7));return;}
        if (itemInfo) {
            int k = BagScreen.WORN[infoFocus], item = k == 0 ? infoWeapon : k == 1 ? infoSkin : infoLook[k - 2];
            c.tooltip.draw(k,item,b,"Đang trang bị",infoSex,infoRanks[ForgeRules.index(infoSex,k,item)]);
        }
    }

    void openOffline() {
        returnToRanking = false;
        friendsPopup = false;
        offlineInvite = true;
        inviting = opened = true;
        tab = 2;
        page = selected = nav = 0;
        refresh();
    }

    void open(boolean room) {
        returnToRanking = false;
        offlineInvite = false;
        if (!net.socialCap) {
            c.message("Cập nhật server để dùng bạn bè.");
            return;
        }
        friendsPopup = !room;
        if (friendsPopup) net.chat.requestSocialContacts();
        inviting = true;
        opened = true;
        nav = page = selected = 0;
        tab = room ? 2 : 0;
        refresh();
    }

    volatile boolean loading, profileLoading, actionLoading, wantList;
    volatile String pendingOp = "";
    String listError = "", profileError = "";
    int generation, loadedTab = -1;
    long listPoll;

    void closeInfo() {
        viewing = profileLoading = itemInfo = false;
        profileError = "";
        if (pendingOp.equals("playerinfo"))
            pendingOp = "";
        if (returnToRanking) {
            returnToRanking = false;
            opened = false;
            c.ranking.opened = true; // Keep the previously selected ranking row/page.
        }
    }

    void cancelRequests() {
        returnToRanking = false;
        pendingOp = "";
        wantList = loading = profileLoading = actionLoading = opened = viewing = searching = false;
        generation++;
    }

    String nextRequest(long now) {
        if (pendingOp.length() > 0) {
            String op = pendingOp;
            pendingOp = "";
            return op;
        }
        if (opened && !offlineInvite && tab != 2 && !viewing && !searching && !actionLoading) {
            if (!loading && !wantList && now - listPoll > 2000)
                wantList = true;
            if (wantList) {
                wantList = false;
                loading = true;
                listPoll = now;
                return tab == 4 ? "findfriend" : "social";
            }
        }
        return "";
    }

    void failed(String op, String message) {
        if (op.equals("social") || op.equals("findfriend")) {
            loading = false;
            wantList = false;
            listError = "Không tải được danh sách";
            listPoll = System.currentTimeMillis();
        }
        if (op.equals("playerinfo")) {
            profileLoading = false;
            profileError = "Không tải được thông tin";
        }
        if (op.equals("friend"))
            actionLoading = false;
    }

    void refresh() {
        if (tab == 2) {
            count = 8;
            loading = false;
            return;
        }
        if (offlineInvite) {
            count = 0;
            return;
        }
        generation++;
        if (loadedTab != tab)
            count = 0;
        loading = true;
        listError = "";
        wantList = true;
    }

    String emptyText() {
        return loading ? "Đang tải..."
                : listError.length() > 0 ? listError
                        : tab == 0 ? "Chưa có bạn bè"
                                : tab == 3 ? "Chưa có lời mời"
                                        : tab == 4 ? "Không tìm thấy người chơi" : tab == 1 ? "Sảnh chưa có người chơi" : "Chưa có người chơi";
    }

    void read(DataInputStream in) throws IOException {
        if (tab == 2)
            return;
        loading = false;
        loadedTab = tab;
        listError = "";
        listPoll = System.currentTimeMillis();
        myId = in.readInt();
        int receivedCount = in.readUnsignedShort();
        if (receivedCount > 128)
            throw new IOException("Sai danh sách.");
        for (int i = 0; i < receivedCount; i++) {
            ids[i] = in.readInt();
            names[i] = in.readUTF();
            online[i] = in.readBoolean();
            sex[i] = in.readUnsignedByte();
            skin[i] = in.readUnsignedByte();
            custom[i] = in.readBoolean();
            for (int k = 0; k < 5; k++)
                looks[i][k] = in.readUnsignedByte();
            experience[i] = net.actorsCap ? in.readInt() : 0;
        }
        count=receivedCount;
        page = Math.min(page, Math.max(0, (count - 1) / rows()));
    }

    int rowExperience(int index) { return tab == 2 ? Progression.botExperience(index) : experience[index]; }

    // The old four-tab full-screen list has been retired. Always use the
    // same compact popup for friends, lobby players, requests, and invitations.
    void draw() {
        if (searching) {
            drawInvitePopup();
            c.popup.renderInput("Tìm kiếm bạn bè", searchQuery, searchFocus);
        } else if (viewing) {
            drawInfo();
        } else {
            drawInvitePopup();
        }
    }

    boolean friendsSearchTab() {
        return friendsPopup && (tab == 3 || tab == 4);
    }

    int rows() {
        return friendsSearchTab() ? 5 : 6;
    }

    Image searchPlus;
    boolean searching;
    int searchFocus;
    Image popup, popupTab, popupInvite, popupRefresh, person, arrowUp, arrowDown, scrollTrack, scrollThumb, female;
    static final int[] INVITE_TABS = { 2, 0, 1 };
    static final String[] FRIEND_TAB_LABELS = { "Bạn bè", "Thêm bạn", "Sảnh" };
    static final String[] INVITE_TAB_LABELS = { "Liên hệ", "Bạn bè", "Phòng" };

    int inviteTabIndex() {
        return friendsPopup ? (tab == 0 ? 0 : tab == 1 ? 2 : 1) : tab == 2 ? 0 : tab == 0 ? 1 : 2;
    }

    void popupLoad() {
        if (popup != null)
            return;
        popup = c.combatRoomArt("invite-popup");
        popupTab = c.combatRoomArt("invite-tab");
        popupInvite = c.combatRoomArt("invite-invite");
        popupRefresh = c.combatRoomArt("invite-refresh");
        person = c.combatRoomArt("invite-person");
        female = c.combatRoomArt("invite-female");
        arrowUp = c.combatRoomArt("invite-up");
        arrowDown = c.combatRoomArt("invite-down");
        close = c.combatRoomArt("bag715-close");
        searchPlus = c.combatRoomArt("friend767-plus");
        scrollTrack = c.combatRoomArt("invite-track");
        scrollThumb = c.combatRoomArt("invite-thumb");
    }

    void drawInvitePopup() {
        popupLoad();
        c.img(popup, 60, 8);
        c.pcText(friendsPopup ? "Bạn bè" : "Mời", 71, 18);
        c.img(close, 233, 14);
        String[] labels = friendsPopup ? FRIEND_TAB_LABELS : INVITE_TAB_LABELS;
        for (int n = 0; n < labels.length; n++) {
            int x = 70 + n * 55;
            c.img(popupTab, x, 41);
            c.pcCenter(labels[n], x + 27, 47);
            if (friendsPopup ? inviteTabIndex() == n : tab == INVITE_TABS[n])
                c.drawSelectionGlow(x - 1, 40, 56, 22);
        }
        if (friendsSearchTab()) {
            c.pcText("Tìm kiếm bạn bè", 77, 74);
            // c.img(popupInvite, 198, 69); // khazg tắt
            c.img(searchPlus, 211, 70);
            if (nav == 1 && selected < 0)
                c.drawSelectionGlow(70, 66, 169, 23);
        }
        for (int slot = 0; slot < rows(); slot++) {
            int i = page * rows() + slot, y = 66 + (friendsSearchTab() ? 23 : 0) + slot * 23;
            if (i >= count)
                continue;
            c.img((tab == 2 ? c.botSex(i) : sex[i]) == 1 ? female : person, 74, y + 2);
            c.drawLevel(rowExperience(i), 90, y + 2);
            // One line per player: name -> full account ID -> status dot.
            // Reserve ID + dot space first. Only long names may be truncated.
            final int textX = 116;
            if (tab == 2) {
                c.pcText(c.pcShort(PhoneCanvas.BOT_NAMES[i], 78), textX, y + 8);
            } else {
                String idText = " ID:" + ids[i];
                String fullName = names[i] == null ? "" : names[i];
                int nameSpace = 191 - textX - c.pcWidth(idText) - 8;
                int dot;
                if (nameSpace >= 21) {
                    // Typical IDs fit alongside the (potentially shortened) name.
                    String name = c.pcShort(fullName, nameSpace);
                    c.pcText(name, textX, y + 8);
                    int idX = textX + c.pcWidth(name);
                    c.pcText(idText, idX, y + 8);
                    dot = idX + c.pcWidth(idText) + 2;
                } else {
                    // Large numeric IDs must remain complete: use the second
                    // line instead of pushing the ID into the action button.
                    c.pcText(c.pcShort(fullName, 75), textX, y + 2);
                    c.pcText(idText, textX, y + 12);
                    dot = textX + c.pcWidth(idText) + 2;
                }
                // Unread private message takes precedence over online green.
                boolean messageUnread = friendsPopup && tab == 0 && net.chat.unreadFrom(ids[i]);
                c.g.setColor(messageUnread ? 0x681515 : online[i] ? 0x174a18 : 0x555555);
                c.g.fillArc(c.ox + dot * c.s, c.oy + (y + 9) * c.s,
                        6 * c.s, 6 * c.s, 0, 360);
                c.g.setColor(messageUnread ? 0xff4242 : online[i] ? 0x66ef49 : 0xb3b3b3);
                c.g.fillArc(c.ox + (dot + 1) * c.s, c.oy + (y + 10) * c.s,
                        4 * c.s, 4 * c.s, 0, 360);
            }
            c.img(popupInvite, 198, y + 3);
            c.pcCenter(friendsPopup ? (tab == 3 ? "Nhận" : tab == 0 ? "Xem" : "Kết bạn") : "Mời", 218, y + 8);
            if (nav == 1 && selected == slot)
                c.drawSelectionGlow(70, y, 169, 23);
        }
        c.img(scrollTrack, 240, 80);
        c.img(arrowUp, 240, 66);
        c.img(arrowDown, 240, 191);
        int pages = Math.max(1, (count + rows() - 1) / rows());
        c.img(scrollThumb, 241, 81 + (pages <= 1 ? 0 : page * 88 / (pages - 1)));
        c.img(popupRefresh, 70, 208);
        c.pcCenter("Danh sách tạo mới", 120, 214);
        c.pcCenter((page + 1) + "/" + Math.max(1, (count + rows() - 1) / rows()), 211, 214);
        if (count == 0)
            c.pcCenter(emptyText(), 158, 126);
        if (nav == 2) {
            if (footer == 2)
                c.drawSelectionGlow(69, 207, 102, 22);
            else
                c.drawSelectionGlow(239, footer == 0 ? 66 : 191, 12, 15);
        }
    }

    void popupTab(int index) {
        if (friendsPopup)
            tab = index == 0 ? 0 : index == 1 ? 3 : 1;
        else
            tab = INVITE_TABS[index];
        page = 0;
        selected = friendsSearchTab() ? -1 : 0;
        refresh();
    }

    void popupPage(int delta) {
        int pages = Math.max(1, (count + rows() - 1) / rows());
        page = (page + pages + delta) % pages;
        selected = friendsSearchTab() ? -1 : 0;
        refresh();
    }

    void popupKey(int key, int dir, boolean ok) {
        if (key == -7) {
            opened = false;
            if (offlineInvite) {
                offlineInvite = false;
                if (c.mode != 19) c.mode = 10;
            }
            return;
        }
        if (key == '#') {
            popupTab((inviteTabIndex() + 1) % 3);
            return;
        }
        if (dir != 0) {
            if (nav == 0) {
                if (dir == 1 || dir == 2)
                    popupTab((inviteTabIndex() + (dir == 1 ? 2 : 1)) % 3);
                else if (dir == 8)
                    nav = 1;
            } else if (nav == 1) {
                if (dir == 4) {
                    if (selected == (friendsSearchTab() ? -1 : 0))
                        nav = 0;
                    else
                        selected--;
                } else if (dir == 8) {
                    if (selected >= Math.min(rows() - 1, count - page * rows() - 1)) {
                        nav = 2;
                        footer = 2;
                    } else
                        selected++;
                } else
                    popupPage(dir == 1 ? -1 : 1);
            } else {
                if (dir == 4)
                    nav = 1;
                else if (dir == 1 || dir == 2)
                    footer = (footer + (dir == 1 ? 2 : 1)) % 3;
            }
        } else if (ok || key == -6) {
            if (nav == 0)
                nav = 1;
            else if (nav == 1)
                choose();
            else if (footer == 2) {
                page = selected = 0;
                refresh();
            } else
                popupPage(footer == 0 ? -1 : 1);
        }
    }

    void popupPointer(int x, int y) {
        if (x >= 233 && x < 254 && y >= 14 && y < 33) {
            opened = false;
            if (offlineInvite) {
                offlineInvite = false;
                if (c.mode != 19) c.mode = 10;
            }
            return;
        }
        if (x < 60 || x >= 260 || y < 8 || y >= 233)
            return;
        if (y >= 41 && y < 62 && x >= 70 && x < 235) {
            popupTab(Math.min(2, (x - 70) / 55));
            nav = 0;
            return;
        }
        if (x >= 239 && y >= 65 && y < 80) {
            popupPage(-1);
            return;
        }
        if (x >= 239 && y >= 190 && y < 205) {
            popupPage(1);
            return;
        }
        if (y >= 208 && x >= 70 && x < 172) {
            nav = 2;
            footer = 2;
            page = selected = 0;
            refresh();
            return;
        }
        if (x >= 239 && x < 251 && y >= 80 && y < 190) {
            int pages = Math.max(1, (count + rows() - 1) / rows());
            page = Math.min(pages - 1, (y - 80) * pages / 110);
            selected = 0;
            return;
        }
        if (x >= 70 && x < 239 && y >= 66 && y < 204) {
            nav = 1;
            if (friendsSearchTab() && y < 89) {
                selected = -1;
                id();
                return;
            }
            selected = (y - 66 - (friendsSearchTab() ? 23 : 0)) / 23;
            choose();
        }
    }

    void editSearch() {
        c.typing.field("Tìm bạn bằng tên hoặc ID",searchQuery,40,false,false,
            52,103,216,new InlineText.Done(){
                public void accept(String value){searchQuery=value;runSearch();}
            });
    }
    void choose() {
        if (friendsSearchTab() && selected < 0) {
            id();
            return;
        }
        int i = page * rows() + selected;
        if (i < 0 || i >= count)
            return;
        if (offlineInvite) {
            if (tab == 2) {
                c.botChoice = i;
                if (c.mode == 19) {
                    c.dungeon.inviteOfflineBot(i);
                    return;
                }
                c.chooseRoomBot();
                opened = offlineInvite = false;
            }
            return;
        }
        if (actionLoading || profileLoading)
            return;
        if (tab == 2) {
            if (net.roomKind == 1) {
                c.message("Đấu đơn chỉ đấu với người thật.");
                return;
            }
            if (!inviting) {
                c.message("Mở Mời trong phòng để chọn AI.");
                return;
            }
            bot = i;
            net.request("allyai");
            return;
        }
        target = ids[i];
        if (friendsPopup) {
            if (tab == 0) {
                viewing = profileLoading = true;
                profileError = "";
                itemInfo = false;
                pendingOp = "playerinfo";
                return;
            }
            action = tab == 3 ? 1 : 0;
            actionLoading = true;
            c.popup.notice("Bạn bè", action == 1 ? "Đang đồng ý kết bạn..." : "Đang gửi lời mời...");
            pendingOp = "friend";
            return;
        }
        if (tab == 0 && !inviting) {
            c.message("ID " + target + (online[i] ? " • Online" : " • Offline"));
            return;
        }
        if (tab == 3) {
            action = 1;
            net.request("friend");
        } else if (inviting) {
            net.request("invite");
        } else {
            action = 0;
            net.request("friend");
        }
    }

    void key(int key, int dir, boolean ok) {
        if (searching) {
            searchKey(key, dir, ok);
        } else if (viewing) {
            if (key == -7 || key == '0') {
                closeInfo();
            } else if (!profileLoading && profileError.length() == 0) {
                if (dir != 0) infoMove(dir);
                else if (key == -6 || ok) infoToggle();
            }
        } else if (friendsPopup && (key == 'i' || key == 'I')) {
            id();
        } else {
            popupKey(key, dir, ok);
        }
    }

    void pointer(int x, int y) {
        if (searching) {
            searchPointer(x, y);
            return;
        }
        if (viewing) {
            int[] wingBox = infoBox(7);
            if (infoWing >= 0 && x >= wingBox[0] && x < wingBox[0] + wingBox[2]
                    && y >= wingBox[1] && y < wingBox[1] + wingBox[3]) {
                infoFocus = 7;
                itemInfo = !itemInfo;
                return;
            }
            if (x >= 209 && x < 231 && y >= 12 && y < 33) {
                closeInfo();
                return;
            }
            if (profileLoading || profileError.length() > 0) return;
            for (int n = 0; n < 7; n++) {
                int[] b = infoBox(n);
                if (x >= b[0] && x < b[0] + b[2]
                        && y >= b[1] && y < b[1] + b[3]
                        && infoIcons[BagScreen.WORN[n]] != null) {
                    if (infoFocus == n) infoToggle();
                    else {
                        infoFocus = n;
                        itemInfo = false;
                    }
                    return;
                }
            }
        } else {
            popupPointer(x, y);
        }
    }

    void pulse(DataInputStream in) throws IOException {
        int room = in.readInt();
        String from = in.readUTF();
        if (room == 0) {
            inviteSeen = 0;
            return;
        }
        if (room == inviteSeen)
            return;
        inviteSeen = room;
        Image portrait=null;String name=from;int level=1;
        if(net.playerUiCap&&in.readBoolean()){
            name=in.readUTF();int sex=in.readUnsignedByte(),skin=in.readUnsignedByte(),mask=in.readUnsignedByte();int[] look=new int[5];
            for(int i=0;i<5;i++)look[i]=in.readUnsignedByte();level=Progression.level(in.readInt());
            portrait=c.equipmentPreview(sex,skin,(mask&2)==0,look,mask,48,60);
        }
        c.popup.playerNotice("Mời vào đội",(portrait==null?from+" mời bạn vào phòng #":"Mời bạn vào phòng #")+room,portrait,name,level,new String[]{"Vào đội","Từ chối"},NativePopup.INVITE_TEAM);
    }

    boolean respond(boolean acceptInvite){if(acceptInvite&&c.requireWeapon())return false;answer=acceptInvite;return net.request("inviteanswer");}
    void invited(String message){
        String heading=net.operation.equals("friend")?"Bạn bè":"Mời bạn bè";
        for(int i=0;i<count;i++)if(ids[i]==target){
            try{Image hero=c.equipmentPreview(sex[i],skin[i],custom[i],looks[i],custom[i]?125:3,48,60);
                c.popup.playerNotice(heading,message,hero,names[i],Progression.level(experience[i]),new String[]{"Đóng"},0);return;
            }catch(IOException ignored){}
        }
        c.popup.notice(heading,message);
    }

    void id() {
        searching = true;
        searchFocus = 0;
    }

    void runSearch() {
        searchQuery = searchQuery.trim();
        if (searchQuery.length() == 0) {
            c.popup.notice("Tìm bạn", "Hãy nhập tên hoặc ID.");
            return;
        }
        searching = false;
        tab = 4;
        page = selected = 0;
        refresh();
    }

    void searchKey(int key, int dir, boolean ok) {
        if (key == -7) {
            searching = false;
            return;
        }
        if (key == 8 || key == 127 || key == -8) {
            if (searchQuery.length() > 0)
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            return;
        }
        if (key >= 32 && key <= 126) {
            if (searchQuery.length() < 40)
                searchQuery += (char) key;
            searchFocus = 0;
            return;
        }
        if (dir != 0) {
            searchFocus = dir == 4 || dir == 1 ? 0 : 1;
            return;
        }
        if (ok || key == -6) {
            if (searchFocus == 0)
                editSearch();
            else
                runSearch();
        }
    }

    void searchPointer(int x, int y) {
        if (x >= 263 && x < 285 && y >= 57 && y < 78) {
            searching = false;
            return;
        }
        if (x >= 50 && x < 270 && y >= 99 && y < 123 || x >= 58 && x < 120 && y >= 153 && y < 176)
            editSearch();
        else if (x >= 200 && x < 263 && y >= 153 && y < 176)
            runSearch();
    }

    public void commandAction(Command cmd, Displayable screen) {
        answer = cmd == accept;
        net.request("inviteanswer");
        Display.getDisplay(c.app).setCurrent(c);
    }
}
