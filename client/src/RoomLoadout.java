import javax.microedition.lcdui.*;
import javax.microedition.rms.RecordStore;

/** Shared native room panel and reusable, energy-costed Z/X/C loadout. */
final class RoomLoadout {
    final PhoneCanvas c;
    final int[] kit = { 0, 1, 2 }, onlineKit = { 0, 1, 2 };
    int editSlot;
    static final int EQUIPPED = 31, AVAILABLE = 32;
    static final int EQUIP_GLOW_X = 217, EQUIP_GLOW_Y = 39, EQUIP_GLOW_W = 100, EQUIP_GLOW_H = 39;
    static final int AVAIL_GLOW_X = 217, AVAIL_GLOW_Y = 84, AVAIL_GLOW_W = 100, AVAIL_GLOW_H = 58;
    int inside, chosenItem = -1;
    boolean info;
    int focusRoom = -1;
    Image squareCancel;
    Image squareInvite, squareStart, squareReady;
    Image background, panel, left, cell, selected, invite, server, start, ready, card, waiting, closed, teamCard,
            teamWaiting, teamClosed, chat, tooltip, randomMap;
    final Image[] icons = new Image[6], grayIcons = new Image[6];
    Image mapPreview;
    int mapPreviewKey = Integer.MIN_VALUE;

    void releaseMenuArt() {
        dungeonMapCover = dungeonMapGreen = dungeonMapYellow = null;
        dungeonCoverStage = -1;
        mapPreview = null;
        mapPreviewKey = Integer.MIN_VALUE;
        squareCancel = squareInvite = squareStart = squareReady = background = panel = left = cell = selected = invite = server = start = ready = card = waiting = closed = teamCard = teamWaiting = teamClosed = chat = tooltip = randomMap = toolBar = null;
        for (int i = 0; i < tools.length; i++)
            tools[i] = null;
    }

    RoomLoadout(PhoneCanvas canvas) {
        c = canvas;
        try {
            RecordStore rs = RecordStore.openRecordStore("gunny-loadout-v2", true);
            if (rs.getNumRecords() > 0) {
                byte[] b = rs.getRecord(1);
                int[] a = { b[0], b[1], b[2] };
                if (Battle.validKit(a))
                    for (int i = 0; i < 3; i++)
                        kit[i] = a[i];
            }
            rs.closeRecordStore();
        } catch (Exception ignored) {
        }
    }

    void load() {
        if (panel != null)
            return;
        squareInvite = c.combatRoomArt("room-invite-square");
        squareStart = c.combatRoomArt("match742-start");
        squareCancel = c.combatRoomArt("match742-cancel");
        squareReady = c.combatRoomArt("room-ready-square");
        background = art("background");
        panel = art("panel");
        left = art("left");
        cell = art("cell");
        selected = art("selected");
        invite = art("invite");
        server = art("server");
        start = art("start");
        ready = art("ready");
        card = art("card");
        waiting = art("waiting");
        closed = art("closed");
        teamCard = art("team-card");
        teamWaiting = art("team-waiting");
        teamClosed = art("team-closed");
        chat = art("chat");
        tooltip = art("tooltip");
        randomMap = art("map-random");
    }

    Image art(String name) {
        return c.combatRoomArt("kit-" + name);
    }

    Image icon(int id) {
        if (id < 0 || id >= 6)
            return null;
        if (icons[id] == null)
            icons[id] = art("item" + id);
        return icons[id];
    }

    boolean validItem(int id) {
        return id >= 0 && id < Battle.ITEM_COST.length && id < icons.length;
    }

    Image grayIcon(int id) {
        if (!validItem(id))
            return null;
        if (grayIcons[id] == null)
            grayIcons[id] = c.combatRoomArt("kit-item" + id + "-gray");
        return grayIcons[id];
    }

    void save() {
        try {
            RecordStore rs = RecordStore.openRecordStore("gunny-loadout-v2", true);
            byte[] b = { (byte) kit[0], (byte) kit[1], (byte) kit[2] };
            if (rs.getNumRecords() == 0)
                rs.addRecord(b, 0, 3);
            else
                rs.setRecord(1, b, 0, 3);
            rs.closeRecordStore();
        } catch (Exception ignored) {
        }
    }

    int[] currentKit(boolean online) {
        return online ? onlineKit : kit;
    }

    void resetFocus() {
        inside = 0;
        chosenItem = -1;
        info = false;
        editSlot = 0;
        focusRoom = -1;
    }

    boolean group(int f) {
        return f == EQUIPPED || f == AVAILABLE;
    }

    boolean activeFocus() {
        if (inside != 0 && (focusRoom != (c.mode == 10 ? 0 : c.net.roomId)
                || c.roomSelection != (inside == 2 ? AVAILABLE : EQUIPPED)))
            resetFocus();
        return inside != 0;
    }

    void enterGroup(int group, boolean online) {
        c.roomSelection = group;
        focusRoom = online ? c.net.roomId : 0;
        inside = group == AVAILABLE ? 2 : 1;
        editSlot = 0;
        chosenItem = -1;
        info = false;
    }

    boolean change(int slot, int id, boolean online) {
        if (online && (!c.net.itemsCap || !c.net.emptyKitCap || c.net.inBattle || c.net.phase != 0)) {
            c.message("Cập nhật server HF13; đổi đạo cụ trước trận.");
            return false;
        }
        if (online && (c.net.busy || c.net.pending.length() > 0)) {
            c.message("Đang đồng bộ, chờ một chút.");
            return false;
        }
        int[] a = currentKit(online);
        if (a[slot] == id)
            return true;
        if (id != Battle.EMPTY_ITEM)
            for (int i = 0; i < 3; i++)
                if (i != slot && a[i] == id) {
                    c.message("Đạo cụ đã có trong trận.");
                    return false;
                }
        int old = a[slot];
        a[slot] = id;
        if (online && !c.net.request("loadout")) {
            a[slot] = old;
            return false;
        }
        if (!online)
            save();
        return true;
    }

    void selectItem(boolean online) {
        int[] a = currentKit(online);
        int id = editSlot;
        for (int i = 0; i < 3; i++)
            if (a[i] == id) {
                c.message("Đạo cụ đã có trong trận.");
                return;
            }
        for (int i = 0; i < 3; i++)
            if (a[i] == Battle.EMPTY_ITEM) {
                change(i, id, online);
                return;
            }
        chosenItem = id;
        inside = 3;
        editSlot = 0;
        c.roomSelection = EQUIPPED;
        info = false;
    }

    void confirmItem(boolean online) {
        if (inside == 2)
            selectItem(online);
        else if (inside == 1)
            change(editSlot, Battle.EMPTY_ITEM, online);
        else if (inside == 3 && change(editSlot, chosenItem, online)) {
            editSlot = chosenItem;
            chosenItem = -1;
            inside = 2;
            c.roomSelection = AVAILABLE;
        }
        info = false;
    }

    void backGroup() {
        if (info) {
            info = false;
            return;
        }
        if (inside == 3) {
            inside = 2;
            editSlot = chosenItem;
            chosenItem = -1;
            c.roomSelection = AVAILABLE;
        } else
            resetFocus();
    }

    boolean groupKey(int key, int dir, boolean ok, boolean online) {
        if (!activeFocus())
            return false;
        if (key == -7) {
            backGroup();
            return true;
        }
        if (key == -6) {
            int id = inside == 2 ? editSlot : currentKit(online)[editSlot];
            if (validItem(id))
                info = !info;
            else
                info = false;
            return true;
        }
        if (dir != 0) {
            info = false;
            int col = editSlot % 3, row = editSlot / 3;
            if (dir == 1 && col > 0)
                editSlot--;
            if (dir == 2 && col < 2)
                editSlot++;
            if (inside == 2) {
                if (dir == 4 && row > 0)
                    editSlot -= 3;
                if (dir == 8 && row == 0)
                    editSlot += 3;
            }
            return true;
        }
        if (ok) {
            if (info)
                info = false;
            else
                confirmItem(online);
            return true;
        }
        return true;
    }

    boolean groupPointer(int x, int y, boolean online) {
        if (!activeFocus())
            return false;
        if (info) {
            info = false;
            return true;
        }
        int f = hit(x, y);
        if (f != (inside == 2 ? AVAILABLE : EQUIPPED)) {
            backGroup();
            return true;
        }
        if (x < 222 || x >= 309 || y < (inside == 2 ? 88 : 46) || y >= (inside == 2 ? 136 : 74))
            return true;
        int col = Battle.clamp((x - 222) / 29, 0, 2), slot = col + (inside == 2 && y >= 112 ? 3 : 0);
        if (slot == editSlot)
            confirmItem(online);
        else
            editSlot = slot;
        return true;
    }

    static final String[] DETAILS = { "Ẩn nhân vật trong 2 lượt.", "Đạn tiếp theo đóng băng mục tiêu.",
            "Hồi 10% HP tối đa cho các đồng minh còn sống.", "Ẩn cả đội trong 2 lượt.",
            "Đạn tự điều chỉnh hướng khi gần mục tiêu.", "Tăng 50 điểm nộ." };

    void drawGroups(boolean online) {
        int[] a = currentKit(online);
        int f = c.roomSelection;
        boolean focused = activeFocus();
        c.pcText("Trong trận", 222, 32);
        for (int i = 0; i < 3; i++) {
            int x = 222 + i * 29;
            c.img(selected, x, 46);
            c.img(icon(a[i]), x, 47);
            c.pcText(c.keyBindings.name(i == 0 ? 'z' : i == 1 ? 'x' : 'c'), x, 42);
        }
        c.pcText("Đạo cụ", 222, 77);
        for (int i = 0; i < 6; i++) {
            int x = 222 + i % 3 * 29, y = 88 + i / 3 * 24;
            c.part(cell, 0, 0, 24, 24, x, y, false);
            c.img(icon(i), x, y);
        }
        if (!focused) {
            if (f == EQUIPPED)
                c.drawSelectionGlow(EQUIP_GLOW_X, EQUIP_GLOW_Y, EQUIP_GLOW_W, EQUIP_GLOW_H);
            else if (f == AVAILABLE)
                c.drawSelectionGlow(AVAIL_GLOW_X, AVAIL_GLOW_Y, AVAIL_GLOW_W, AVAIL_GLOW_H);
        } else if (inside == 2)
            c.drawSelectionGlow(221 + editSlot % 3 * 29, 87 + editSlot / 3 * 24, 27, 26);
        else
            c.drawSelectionGlow(221 + editSlot * 29, 45, 27, 27);
    }

    void drawGroupInfo(boolean online) {
        if (!activeFocus())
            return;
        if (inside == 3 && !info)
            c.drawTooltip("Chọn ô thay thế", 160, 142);
        if (info) {
            int id = inside == 2 ? editSlot : currentKit(online)[editSlot];
            if (validItem(id))
                c.tooltip.goods(Battle.ITEM_NAMES[id], "Đạo cụ",
                        "Thể lực: " + Battle.ITEM_COST[id] + " TL\n" + DETAILS[id] + "\nDùng 1 lần mỗi trận.",
                        new int[] { 222 + editSlot % 3 * 29, inside == 2 ? 88 + editSlot / 3 * 24 : 46, 24, 24 });
        }
    }

    void apply() {
        int[] selectedKit = c.onlineAccount() ? onlineKit : kit;
        for (int i = 0; i < c.game.unitCount; i++)
            if (i == 0 || c.game.units[i].human)
                for (int k = 0; k < 3; k++)
                    c.game.units[i].kit[k] = selectedKit[k];
    }

    boolean toolbarTouch;
    int toolbarHover = -1;
    Image toolBar;
    final Image[] tools = new Image[10];
    static final int[] TOOL_IDS = { 41, 42, 45, 46, 47 }, TOOL_X = { 26, 65, 104, 143, 182 };

    void drawToolbar(boolean online) {
        if (toolBar == null) {
            toolBar = art("toolbar");
            for (int i = 0; i < 10; i++)
                tools[i] = art("tool" + (i / 2) + "-" + (i % 2));
        }
        int focus = toolbarTouch ? toolbarHover : c.roomSelection;
        c.img(toolBar, 0, 195);
        for (int i = 0; i < 5; i++) {
            if (!online && i == 2)
                continue;
            int tx = toolX(i, online);
            Image image = tools[i * 2 + (focus == TOOL_IDS[i] ? 1 : 0)];
            c.img(image, tx - image.getWidth() / c.s / 2, 238 - image.getHeight() / c.s);
            if (focus == TOOL_IDS[i]) {
                String title = new String[] { "Shop", "Túi", "Bạn bè", "Thiết lập",
                        online ? "Rời phòng" : "Về sảnh" }[i];
                c.drawTooltip(title, Math.min(tx, 208 - (c.textWidth(title) + 12) / 2), 168);
            }
        }
    }

    int toolX(int i, boolean online) {
        return TOOL_X[i] - (!online && i > 2 ? 39 : 0);
    }

    int toolbarHit(int x, int y) {
        if (y < 195 || y >= 240 || x < 7 || x >= 205)
            return -1;
        for (int i = 0; i < 5; i++)
            if ((c.onlineAccount() || i != 2) && Math.abs(x - toolX(i, c.onlineAccount())) < 19)
                return TOOL_IDS[i];
        return -1;
    }

    boolean pressToolbar(int x, int y) {
        int id = toolbarHit(x, y);
        if (id < 0)
            return false;
        toolbarTouch = true;
        toolbarHover = c.roomSelection = id;
        return true;
    }

    void dragToolbar(int x, int y) {
        toolbarHover = toolbarHit(x, y);
        if (toolbarHover >= 0)
            c.roomSelection = toolbarHover;
    }

    void releaseToolbar(int x, int y) {
        int id = toolbarHit(x, y);
        boolean activate = toolbarTouch && id >= 0 && id == toolbarHover;
        toolbarTouch = false;
        toolbarHover = -1;
        if (activate) {
            c.roomSelection = id;
            activate(c.onlineAccount());
        }
    }

    void draw(boolean online) {
        draw(online, false);
    }

    void drawMapPreviewImage(int map) {
        if (map <= 0)
            return;
        if (mapPreview == null || mapPreviewKey != map) {
            Image source = c.maps.banner(map);
            mapPreview = source == null ? null : NativeUi.fit(source, 90 * c.s, 40 * c.s, 0);
            mapPreviewKey = map;
        }
        if (mapPreview != null)
            c.img(mapPreview, 222, 150);
    }

    void draw(boolean online, boolean dungeon) {
        load();
        int f = c.roomSelection;
        String title = online ? c.net.roomTitle : dungeon ? "Phó bản vui vẻ" : "Thi đấu vui vẻ";
        c.img(panel, 214, 0);
        c.pcText("Phòng " + (online ? "" + c.net.roomId : "Offline"), 221, 4);
        c.pcCenter(c.pcShort(title, 88), 267, 18);
        drawGroups(online);
        if (dungeon) {
            drawDungeonMap(online);
        } else {
            int choice = online ? c.net.roomChoice : c.randomMap ? -1 : c.pvpMap();
            int map = choice >= 0 && choice < PhoneCanvas.PVP_MAP_COUNT ? choice + 1 : 0;
            if (map == 0) {
                c.img(randomMap, 220, 147);
                c.pcCenter((online ? c.net.roomSeconds : c.turnSeconds) + " giây", 266, 181);
            } else {
                c.img(randomMap, 220, 147);
                drawMapPreviewImage(map);
                c.pcCenter(c.pcShort(PhoneCanvas.MAPS[map - 1], 88), 267, 181);
            }
        }
        drawToolbar(online);
        c.img(squareInvite, 220, 197);
        Image a = c.presentation.searching() ? squareCancel : online && !c.net.host ? squareReady : squareStart;
        c.img(a, 272, 197);
        if (f == 2)
            c.drawSelectionGlow(219, 196, 46, 44);
        else if (f == 5)
            c.drawSelectionGlow(271, 196, 46, 44);
        else if (f == 29 || f == 4)
            c.drawSelectionGlow(219, 146, 96, 46);
        drawGroupInfo(online);
    }

    Image dungeonMapCover, dungeonMapGreen, dungeonMapYellow;
    int dungeonCoverStage = -1;

    private void drawDungeonMap(boolean online) {
        int selectedStage = online ? (c.net.dungeonStage >= 3 ? 3 : 1) : (c.offlineDungeon.stage >= 3 ? 3 : 1);
        if (dungeonMapCover == null || dungeonCoverStage != selectedStage) {
            dungeonCoverStage = selectedStage;
            dungeonMapCover = NativeUi.fit(c.dungeon.mapCover(selectedStage >= 3 ? 1 : 0), 82 * c.s, 37 * c.s, 0);
            dungeonMapGreen = c.combatRoomArt("font-pc-green");
            dungeonMapYellow = c.combatRoomArt("font-pc-yellow");
        }
        c.img(randomMap, 220, 147);
        c.img(dungeonMapCover, 230, 151);
        // Keep the original settings corner above the cover.
        for (int row = 0; row < 17; row++) {
            int width = row + 1;
            c.part(randomMap, 93 - width, 24 + row, width, 1, 313 - width, 171 + row, false);
        }
        dungeonMapRow("Độ khó: ",
                c.dungeon.difficultyName(online ? c.net.dungeonDifficulty : c.offlineDungeon.difficulty), 153,
                dungeonMapYellow);
        dungeonMapRow("Cấp: ", selectedStage >= 3 ? "30+" : "15+", 165, dungeonMapGreen);
        dungeonMapRow("Ải: ", c.net.dungeonStage >= 3 ? DungeonRewards.tribalNumber(c.net.dungeonStage) + "-3"
                : Math.max(1, c.net.dungeonStage) + "-2", 177, dungeonMapGreen);
    }

    private void dungeonMapRow(String label, String value, int y, Image font) {
        c.tooltip.colored(label, 231, y, font);
        c.pcText(value, 233 + c.pcWidth(label), y);
    }

    boolean activate(boolean online) {
        int f = c.roomSelection;
        if (f >= 60 && f < 66)
            return false;
        if (f >= 41) {
            if (f == 46) {
                c.presentation.settings = true;
                return true;
            }
            if (f == 47) {
                if (online && c.net.roomKind == 3)
                    c.dungeon.leaveWaiting();
                else if (online)
                    c.net.request("leave");
                else
                    c.openLobby();
            } else if (online)
                c.net.rooms.activateTool(f);
            else if (f == 41)
                c.openShop();
            else if (f == 42)
                c.openBag();
            else if (f == 45)
                return true;
            else if (f == 46)
                c.net.waiting.notice("HƯỚNG DẪN", c.keyBindings.help());
            return true;
        }
        if (group(f)) {
            enterGroup(f, online);
            return true;
        }
        if (f == 29 || f == 4) {
            if (online) {
                if (!c.net.host || c.net.inBattle) {
                    c.message("Chỉ chủ phòng được chọn bản đồ trước trận.");
                    return true;
                }
            }
            c.maps.open(online);
            return true;
        }
        return false;
    }

    int x(int f) {
        if (group(f))
            return 267;
        if (f >= 60 && f < 66)
            return 71 + (f - 60) / 2 * 54;
        for (int i = 0; i < 5; i++)
            if (f == TOOL_IDS[i])
                return toolX(i, c.onlineAccount());
        if (f >= 20 && f <= 22)
            return 234 + (f - 20) * 29;
        if (f >= 23 && f <= 28)
            return 235 + (f - 23) % 3 * 29;
        return f == 10 ? 196
                : f == 11 ? 94
                        : f == 30 ? 265
                                : f == 29 || f == 4 ? 267
                                        : f == 2 ? 242
                                                : f == 5 ? 294
                                                        : f == 7 ? 305 : f == 1 ? 156 : f == 8 || f == 9 ? 25 : 56;
    }

    int y(int f) {
        if (group(f))
            return f == EQUIPPED ? 57 : 112;
        if (f >= 60 && f < 66)
            return 54 + (f - 60) % 2 * 88;
        if (f >= 41)
            return 220;
        if (f >= 20 && f <= 22)
            return 62;
        if (f >= 23 && f <= 28)
            return 104 + (f - 23) / 3 * 24;
        return f == 30 || f == 2 || f == 5 ? 218
                : f == 29 || f == 4 ? 173
                        : f == 7 ? 12
                                : f == 1 ? 128
                                        : f == 0 ? 128 : f == 10 || f == 11 ? 84 : f == 8 ? 77 : f == 9 ? 165 : 37;
    }

    int focusX(int f, boolean dungeon) {
        return dungeon && f >= 60 && f <= DungeonRewardPreview.FOCUS ? 59 + ((f - 60) / 2 % 2) * 102 : x(f);
    }

    int focusY(int f, boolean dungeon) {
        return dungeon && f >= 60 && f <= DungeonRewardPreview.FOCUS ? 68 + ((f - 60) / 4) * 83 : y(f);
    }

    void move(int dir, boolean online) {
        move(dir, online, false);
    }

    void move(int dir, boolean online, boolean dungeon) {
        int[] order = dungeon
                ? new int[] { 60, 62, 64, DungeonRewardPreview.FOCUS, 2, 5, EQUIPPED, AVAILABLE, 29, 41, 42, 45, 46,
                        47 }
                : online && c.net.count == 6
                        ? new int[] { 60, 61, 62, 63, 64, 65, 8, 9, 2, 5, 7, EQUIPPED, AVAILABLE, 29, 41, 42, 45, 46,
                                47 }
                        : new int[] { 0, 1, 2, 5, 7, 11, 10, EQUIPPED, AVAILABLE, 29, 41, 42, 45, 46, 47 };
        int from = c.roomSelection, best = from, score = 999999;
        for (int i = 0; i < order.length; i++) {
            int f = order[i];
            if (!online && (f == 11 || f == 10 || f == 45))
                continue;
            if (online && (f == 0 || !c.net.host && (f == 10 || f == 1)))
                continue;
            int dx = focusX(f, dungeon) - focusX(from, dungeon), dy = focusY(f, dungeon) - focusY(from, dungeon),
                    along = dir == 1 ? -dx : dir == 2 ? dx : dir == 4 ? -dy : dy,
                    cross = dir == 1 || dir == 2 ? dy : dx;
            if (along <= 0)
                continue;
            int v = along * along + cross * cross * 4;
            if (v < score) {
                score = v;
                best = f;
            }
        }
        c.roomSelection = best;
    }

    int hit(int x, int y) {
        if (x < 214)
            return -1;
        if (y >= EQUIP_GLOW_Y && y < EQUIP_GLOW_Y + EQUIP_GLOW_H && x >= EQUIP_GLOW_X
                && x < EQUIP_GLOW_X + EQUIP_GLOW_W)
            return EQUIPPED;
        if (y >= AVAIL_GLOW_Y && y < AVAIL_GLOW_Y + AVAIL_GLOW_H && x >= AVAIL_GLOW_X
                && x < AVAIL_GLOW_X + AVAIL_GLOW_W)
            return AVAILABLE;
        if (y >= 146 && y < 197)
            return 29;
        if (y >= 197)
            return x < 267 ? 2 : 5;
        return -1;
    }

    boolean pointer(int x, int y, boolean online) {
        if (pressToolbar(x, y))
            return true;
        int f = hit(x, y);
        if (f < 0)
            return false;
        c.roomSelection = f;
        if (!activate(online)) {
            if (online)
                c.net.waiting.activate();
            else
                c.roomActivate();
        }
        return true;
    }

    void use(int slot) {
        if (slot < 0 || slot >= 3)
            return;
        if (!c.isLocalTurn() || c.freeCamera) {
            c.message("Đến lượt bạn và trở về nhân vật để dùng.");
            return;
        }
        int actor = c.game.active;
        if (actor < 0 || actor >= c.game.unitCount)
            return;
        Battle.Unit u = c.game.units[actor];
        int id = u.kit[slot];
        if (!validItem(id)) {
            c.message(id == Battle.EMPTY_ITEM ? "Ô đạo cụ trống." : "Đạo cụ đang đồng bộ, chờ một chút.");
            return;
        }
        if ((u.itemUsed & (1 << slot)) != 0) {
            c.message("Đạo cụ này đã dùng trong trận");
            return;
        }
        if (u.energy < Battle.ITEM_COST[id]) {
            c.message("Không đủ thể lực để sử dụng");
            return;
        }
        if (c.onlineBattle())
            c.net.command(8, slot);
        else if (!c.game.item(slot))
            c.message("Đạo cụ chưa thể sử dụng lúc này");
    }

    Image frost, ice, freezeImpact, hiding;

    Image effect(String name) {
        return c.combatRoomArt("kit-" + name);
    }

    Image ice() {
        if (ice == null)
            ice = effect("ice-projectile");
        return ice;
    }

    void frost(int x, int y) {
        if (frost == null)
            frost = effect("frost");
        c.part(frost, c.frame / 6 % 4 * 64, 0, 64, 64, x - 32, y - 56, false);
    }

    void impact(int age, int x, int y) {
        if (freezeImpact == null)
            freezeImpact = effect("freeze-impact");
        c.part(freezeImpact, Math.min(4, age / 5) * 64, 0, 64, 64, x - 32, y - 48, false);
    }

    void hud() {
        int actor = c.playerActor();
        if (actor < 0 || actor >= c.game.unitCount)
            return;
        Battle.Unit u = c.game.units[actor];
        for (int k = 0; k < 3; k++) {
            int x = 184 + k * 29, y = 170, id = u.kit[k];
            c.img(c.hudPropSlot, x, y);
            if ((u.itemUsed & (1 << k)) == 0) {
                if (validItem(id))
                    c.img(u.energy < Battle.ITEM_COST[id] ? grayIcon(id) : icon(id), x, y);
                c.text(c.keyBindings.name(k == 0 ? 'z' : k == 1 ? 'x' : 'c'), x, y - 2);
            }
        }
        int active = c.game.active;
        if (active >= 0 && active < c.game.unitCount && c.game.units[active].frozen == 2)
            c.pcCenter("Đóng băng", 160, 80);
    }

    boolean hidden(int index) {
        Battle.Unit u = c.game.units[index], self = c.game.units[c.playerActor()];
        return u.invisible > 0 && index != c.playerActor() && (!c.game.teams || u.team != self.team);
    }

    void preview() {
    } // Dẫn đường bends real projectiles near a target; it is not an aiming preview.
}
