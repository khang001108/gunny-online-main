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
    final Image[] heads = new Image[6];
    Image panel, row, tabArt, invite, close;

    final Command done = new Command("Tìm", Command.OK, 1), back = new Command("Hủy", Command.BACK, 2),
            accept = new Command("Vào đội", Command.OK, 1), decline = new Command("Từ chối", Command.CANCEL, 2);

    OnlineSocial(OnlineClient n) {
        net = n;
        c = n.c;
    }

    Image infoFrame;
    int infoFocus;
    boolean itemInfo;
    boolean viewing;
    String infoName;
    int infoSex, infoSkin, infoMask, infoWeapon, infoExp, infoWins, infoRank;
    final int[] infoForge=new int[3];
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
        int[] b = c.bag.box(31 + n);
        return new int[] { b[0] + 84, b[1], b[2], b[3] };
    }

    void infoMove(int dir) {
        int[] a = infoBox(infoFocus);
        int best = infoFocus, score = 999999;
        for (int n = 0; n < 7; n++)
            if (n != infoFocus && infoIcons[BagScreen.WORN[n]] != null) {
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

    void infoToggle() {
        if (infoIcons[BagScreen.WORN[infoFocus]] != null)
            itemInfo = !itemInfo;
    }

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
        c.img(infoHero, 128, 58);
        for (int n = 0; n < 7; n++) {
            int k = BagScreen.WORN[n];
            int[] b = infoBox(n);
            c.img(c.bag.slot, b[0], b[1]);
            if (infoIcons[k] != null)
                c.img(infoIcons[k], b[0] + 1, b[1] + 1);
        }
        if (c.bag.statsPanel == null)
            c.bag.statsPanel = NativeUi.fit(c.combatRoomArt("item752-tooltip"), 85 * c.s, 54 * c.s, 4 * c.s);
        c.img(c.bag.statsPanel, 132, 138);
        c.drawLevel(infoExp, 135, 139);
        c.pcText("EXP", 162, 140);
        c.pcText(Progression.level(infoExp) == 70 ? "MAX"
                : Progression.within(infoExp) + "/" + Progression.limit(infoExp), 162, 150);
        c.pcText("Xếp hạng: " + infoRank, 136, 161);
        c.pcText("Thắng: " + infoWins, 136, 171);
        c.pcText("Lực chiến: " + (Progression.power(infoMask, infoWeapon, infoExp)+infoForge[0]+infoForge[1]+infoForge[2]), 136, 181);
        int[] b = infoBox(infoFocus);
        if (infoIcons[BagScreen.WORN[infoFocus]] != null)
            c.drawItemSelection(b[0], b[1], b[2], b[3], itemInfo);
        if (itemInfo) {
            int k = BagScreen.WORN[infoFocus], item = k == 0 ? infoWeapon : k == 1 ? infoSkin : infoLook[k - 2];
            c.tooltip.draw(k, item, b, "Đang trang bị");
        }
    }

    void openOffline() {
        friendsPopup = false;
        offlineInvite = true;
        inviting = opened = true;
        tab = 2;
        page = selected = nav = 0;
        refresh();
    }

    void open(boolean room) {
        offlineInvite = false;
        if (!net.socialCap) {
            c.message("Cập nhật server để dùng bạn bè.");
            return;
        }
        friendsPopup = !room;
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
    }

    void cancelRequests() {
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
            if (!loading && !wantList && now - listPoll > 4000)
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
        for (int i = 0; i < 6; i++)
            heads[i] = null;
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
                                        : tab == 4 ? "Không tìm thấy người chơi" : "Chưa có người chơi";
    }

    void read(DataInputStream in) throws IOException {
        if (tab == 2)
            return;
        loading = false;
        loadedTab = tab;
        listError = "";
        listPoll = System.currentTimeMillis();
        myId = in.readInt();
        count = in.readUnsignedShort();
        if (count > 128)
            throw new IOException("Sai danh sách.");
        for (int i = 0; i < count; i++) {
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
        page = Math.min(page, Math.max(0, (count - 1) / rows()));
        for (int i = 0; i < 6; i++)
            heads[i] = null;
    }

    int rowExperience(int index) { return tab == 2 ? Progression.botExperience(index) : experience[index]; }

    Image head(int index) throws IOException {
        int gender = tab == 2 ? c.botSex(index) : sex[index], costume = tab == 2 ? c.botSkin(index) : skin[index];
        int[] source = new int[80 * 100 * c.s * c.s];
        if (tab != 2 && custom[index]) {
            int[] order = { 0, 3, 2, 1, 4 };
            for (int k = 0; k < 5; k++)
                c.blendCutin(source, c.load("cutin" + gender + "_" + (order[k] + 1) + "_" + looks[index][order[k]]));
        } else
            c.blendCutin(source, c.load("cutin" + gender + "_0_" + costume));
        int side = 30 * c.s;
        int[] pixels = new int[side * side];
        for (int y = 0; y < side; y++)
            for (int x = 0; x < side; x++)
                pixels[y * side + x] = source[(y * 65 * c.s / side) * 80 * c.s + x * 80 * c.s / side];
        return Image.createRGBImage(pixels, side, side, true);
    }

    void draw() {
        if (searching) {
            drawInvitePopup();
            c.popup.renderInput("Tìm kiếm bạn bè", searchQuery, searchFocus);
            return;
        }
        if (viewing) {
            drawInfo();
            return;
        }
        if (inviting) {
            drawInvitePopup();
            return;
        }
        if (panel == null) {
            panel = c.combatRoomArt("social-panel");
            row = c.combatRoomArt("social-row");
            tabArt = c.combatRoomArt("social-tab");
            invite = c.combatRoomArt("social-invite");
            close = c.combatRoomArt("ui-close");
        }
        c.img(panel, 0, 0);
        c.pcText(inviting ? "Mời đồng minh" : "Bạn bè • ID " + myId, 21, 18);
        c.img(close, 289, 12);
        String[] labels = { "Bạn bè", "Sảnh", "AI", "Yêu cầu" };
        for (int i = 0; i < 4; i++) {
            c.img(tabArt, 19 + i * 69, 39);
            c.pcCenter(labels[i], 50 + i * 69, 45);
            if (tab == i)
                c.drawSelectionGlow(18 + i * 69, 38, 65, 22);
        }
        for (int slot = 0; slot < 4 && page * 4 + slot < count; slot++) {
            int i = page * 4 + slot, x = 20, y = 68 + slot * 33;
            c.img(row, x, y);
            try {
                if (heads[slot] == null)
                    heads[slot] = head(i);
            } catch (Exception ignored) {
            }
            c.img(heads[slot], x + 4, y + 4);
            c.pcText(c.pcShort(tab == 2 ? PhoneCanvas.BOT_NAMES[i] : names[i], 88), x + 39, y + 4);
            c.pcText(tab == 2 ? "AI" : ("ID " + ids[i] + (online[i] ? " • On" : " • Off")), x + 39, y + 16);
            c.pcText((tab == 2 ? c.botSex(i) : sex[i]) == 1 ? "Nữ" : "Nam", x + 133, y + 5);
            c.drawLevel(rowExperience(i), x + 160, y + 4);
            c.img(invite, x + 215, y + 10);
            c.pcCenter(tab == 3 ? "Nhận" : tab == 2 || inviting ? "Mời" : tab == 0 ? "Đã kết" : "Kết bạn", x + 241,
                    y + 13);
            if (nav == 1 && selected == slot)
                c.drawSelectionGlow(x - 1, y - 1, 280, 31);
        }
        if (count == 0)
            c.pcCenter(emptyText(), 160, 119);
        c.img(tabArt, 20, 207);
        c.pcCenter("Trước", 51, 213);
        c.pcCenter((page + 1) + "/" + Math.max(1, (count + 3) / 4), 128, 213);
        c.img(tabArt, 158, 207);
        c.pcCenter("Sau", 189, 213);
        c.img(tabArt, 231, 207);
        c.pcCenter("ID", 262, 213);
        if (nav == 2)
            c.drawSelectionGlow(footer == 0 ? 19 : footer == 1 ? 157 : 230, 206, 65, 22);
    }

    int rows() {
        return friendsPopup && tab != 0 ? 5 : 6;
    }

    Image searchPlus;
    boolean searching;
    int searchFocus;
    Image popup, popupTab, popupInvite, popupRefresh, person, arrowUp, arrowDown, scrollTrack, scrollThumb, female;
    static final int[] INVITE_TABS = { 2, 0, 1 };

    int inviteTabIndex() {
        return friendsPopup ? (tab == 0 ? 0 : 1) : tab == 2 ? 0 : tab == 0 ? 1 : 2;
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
        String[] labels = friendsPopup ? new String[] { "Bạn bè", "Thêm bạn" }
                : new String[] { "Liên hệ", "Bạn bè", "Phòng" };
        for (int n = 0; n < labels.length; n++) {
            int x = 70 + n * 55;
            c.img(popupTab, x, 41);
            c.pcCenter(labels[n], x + 27, 47);
            if (friendsPopup ? (n == 0 ? tab == 0 : tab == 3 || tab == 4) : tab == INVITE_TABS[n])
                c.drawSelectionGlow(x - 1, 40, 56, 22);
        }
        if (friendsPopup && tab != 0) {
            c.pcText("Tìm kiếm bạn bè", 77, 74);
            // c.img(popupInvite, 198, 69); // khazg tắt
            c.img(searchPlus, 211, 70);
            if (nav == 1 && selected < 0)
                c.drawSelectionGlow(70, 66, 169, 23);
        }
        for (int slot = 0; slot < rows(); slot++) {
            int i = page * rows() + slot, y = 66 + (friendsPopup && tab != 0 ? 23 : 0) + slot * 23;
            if (i >= count)
                continue;
            c.img((tab == 2 ? c.botSex(i) : sex[i]) == 1 ? female : person, 74, y + 2);
            c.drawLevel(rowExperience(i), 90, y + 2);
            String name = c.pcShort(tab == 2 ? PhoneCanvas.BOT_NAMES[i] : names[i], 68);
            c.pcText(name, 118, y + 8);
            if (friendsPopup && tab != 2 && online[i]) {
                int dot = 122 + c.pcWidth(name);
                c.g.setColor(0x174a18);
                c.g.fillArc(c.ox + dot * c.s, c.oy + (y + 9) * c.s, 6 * c.s, 6 * c.s, 0, 360);
                c.g.setColor(0x66ef49);
                c.g.fillArc(c.ox + (dot + 1) * c.s, c.oy + (y + 10) * c.s, 4 * c.s, 4 * c.s, 0, 360);
            }
            c.img(popupInvite, 198, y + 3);
            c.pcCenter(friendsPopup ? (tab == 3 ? "Nhận" : tab == 4 ? "Kết bạn" : "Xem") : "Mời", 218, y + 8);
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
            tab = index == 0 ? 0 : 3;
        else
            tab = INVITE_TABS[index];
        page = 0;
        selected = friendsPopup && tab != 0 ? -1 : 0;
        refresh();
    }

    void popupPage(int delta) {
        int pages = Math.max(1, (count + rows() - 1) / rows());
        page = (page + pages + delta) % pages;
        selected = friendsPopup && tab != 0 ? -1 : 0;
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
            popupTab((inviteTabIndex() + 1) % (friendsPopup ? 2 : 3));
            return;
        }
        if (dir != 0) {
            if (nav == 0) {
                if (dir == 1 || dir == 2)
                    popupTab((inviteTabIndex() + (dir == 1 ? (friendsPopup ? 1 : 2) : 1)) % (friendsPopup ? 2 : 3));
                else if (dir == 8)
                    nav = 1;
            } else if (nav == 1) {
                if (dir == 4) {
                    if (selected == (friendsPopup && tab != 0 ? -1 : 0))
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
            popupTab(Math.min(friendsPopup ? 1 : 2, (x - 70) / 55));
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
            if (friendsPopup && tab != 0 && y < 89) {
                selected = -1;
                id();
                return;
            }
            selected = (y - 66 - (friendsPopup && tab != 0 ? 23 : 0)) / 23;
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
        if (friendsPopup && tab != 0 && selected < 0) {
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
            return;
        }
        if (viewing) {
            if (key == -7 || key == '0') {
                closeInfo();
            } else if (!profileLoading && profileError.length() == 0) {
                if (dir != 0)
                    infoMove(dir);
                else if (key == -6 || ok)
                    infoToggle();
            }
            return;
        }
        if (friendsPopup && (key == 'i' || key == 'I')) {
            id();
            return;
        }
        if (inviting) {
            popupKey(key, dir, ok);
            return;
        }
        if (key == -7) {
            opened = false;
            return;
        }
        if (key == '#') {
            tab = (tab + 1) % 4;
            page = selected = 0;
            refresh();
            return;
        }
        if (key == 'i' || key == 'I') {
            id();
            return;
        }
        if (dir != 0) {
            if (nav == 0) {
                if (dir == 1 || dir == 2) {
                    tab = (tab + (dir == 1 ? 3 : 1)) % 4;
                    page = selected = 0;
                    refresh();
                } else if (dir == 8)
                    nav = 1;
            } else if (nav == 1) {
                if (dir == 4) {
                    if (selected == (friendsPopup && tab != 0 ? -1 : 0))
                        nav = 0;
                    else
                        selected--;
                } else if (dir == 8) {
                    if (selected >= Math.min(3, count - page * 4 - 1))
                        nav = 2;
                    else
                        selected++;
                } else {
                    page = (page + (dir == 1 ? Math.max(1, (count + 3) / 4) - 1 : 1)) % Math.max(1, (count + 3) / 4);
                    refresh();
                }
            } else {
                if (dir == 4)
                    nav = 1;
                else
                    footer = (footer + (dir == 1 ? 2 : 1)) % 3;
            }
        } else if (ok || key == -6) {
            if (nav == 0)
                nav = 1;
            else if (nav == 1)
                choose();
            else if (footer == 2)
                id();
            else {
                int pages = Math.max(1, (count + 3) / 4);
                page = (page + (footer == 0 ? pages - 1 : 1)) % pages;
                refresh();
            }
        }
    }

    void pointer(int x, int y) {
        if (searching) {
            searchPointer(x, y);
            return;
        }
        if (viewing) {
            if (x >= 209 && x < 231 && y >= 12 && y < 33) {
                closeInfo();
                return;
            }
            if (profileLoading || profileError.length() > 0)
                return;
            for (int n = 0; n < 7; n++) {
                int[] b = infoBox(n);
                if (x >= b[0] && x < b[0] + b[2] && y >= b[1] && y < b[1] + b[3]
                        && infoIcons[BagScreen.WORN[n]] != null) {
                    if (infoFocus == n)
                        infoToggle();
                    else {
                        infoFocus = n;
                        itemInfo = false;
                    }
                    return;
                }
            }
            return;
        }
        if (inviting) {
            popupPointer(x, y);
            return;
        }
        if (x >= 288 && y < 36) {
            opened = false;
            return;
        }
        if (y >= 39 && y < 61) {
            tab = Math.max(0, Math.min(3, (x - 19) / 69));
            page = selected = 0;
            refresh();
            return;
        }
        if (y >= 207) {
            if (x >= 230)
                id();
            else {
                int pages = Math.max(1, (count + 3) / 4);
                page = (page + (x < 100 ? pages - 1 : 1)) % pages;
                selected = 0;
                refresh();
            }
            return;
        }
        if (y >= 68 && y < 203 && x >= 20) {
            nav = 1;
            selected = Math.min(3, (y - 68) / 33);
            choose();
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

    boolean respond(boolean acceptInvite){answer=acceptInvite;return net.request("inviteanswer");}
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
