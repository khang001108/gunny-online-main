import javax.microedition.lcdui.*;

/**
 * Shared island home; login changes its state rather than replacing it with the
 * room browser.
 */
final class HomeScreen {
    void releaseArt(){background=online=forge=dungeon=onlineGlow=forgeGlow=onlineZoom=forgeZoom=null;for(int i=0;i<labels.length;i++)labels[i]=null;for(int i=0;i<friends.length;i++)friends[i]=null;}

    static final int CONFIRM_OFFLINE = 7;
    final PhoneCanvas c;
    Image background, online, forge, dungeon, onlineGlow, forgeGlow, onlineZoom, forgeZoom;
    static final int FRIENDS = 8;
    final Image[] labels = new Image[10], friends = new Image[2];
    static final int[] X = { 64, 22, 163, 151, 300, 236, 94, 83, 108 },
            Y = { 217, 217, 92, 217, 217, 162, 143, 79, 217 };

    HomeScreen(PhoneCanvas canvas) {
        c = canvas;
    }

    void draw() {
        if (background == null) {
            background = c.combatRoomArt("lobby780");
            online = c.combatRoomArt("lobby780-online");
            forge = c.combatRoomArt("lobby780-forge");
            dungeon = NativeUi.fit(c.combatRoomArt("lobby-online"), 96 * c.s, 72 * c.s, 0);
        }
        c.all();
        c.img(background, 0, 0);
        if(!c.onlineAccount()){Image exit=c.lobbyMenu[8+(c.lobbySelection==4?1:0)];c.img(exit,300-exit.getWidth()/c.s/2,c.lobbyIconY(4,exit.getHeight()/c.s));return;}
        for (int i = 0; i < 5; i++) {
            Image image = c.lobbyMenu[i * 2 + (i == c.lobbySelection ? 1 : 0)];
            c.img(image, x(i) - image.getWidth() / c.s / 2, c.lobbyIconY(i, image.getHeight() / c.s));
        }
        if (c.lobbySelection == 7) {
            if (forgeZoom == null) {
                forgeZoom = c.combatRoomArt("home791-forge-zoom");
                forgeGlow = c.combatRoomArt("home791-forge-glow");
            }
            c.img(forgeGlow, 42, 39);
            c.img(forgeZoom, 45, 42);
        } else
            c.img(forge, 51, 51);
        if (c.lobbySelection == 6) {
            if (onlineZoom == null) {
                onlineZoom = c.combatRoomArt("home791-online-zoom");
                onlineGlow = c.combatRoomArt("home791-online-glow");
            }
            c.img(onlineGlow, 62, 99);
            c.img(onlineZoom, 65, 102);
        } else
            c.img(online, 70, 113);
        if (c.lobbySelection == 5) {
            if (c.onlineHover == null)
                c.onlineHover = c.combatRoomArt("lobby-online-hover");
            c.img(c.onlineHover, 183, 109);
        } else
            c.img(dungeon, 188, 117);
        c.img(c.lobbyHallLabel, X[2] - c.lobbyHallLabel.getWidth() / (2 * c.s), 141);
        drawLabel(6,X[6],168);
        drawLabel(7, X[7], 101);
        drawLabel(5, 226, 193);
        int selected = c.lobbySelection;
        if (selected == 0 || selected == 1 || selected == 3 || selected == 4)
            drawLabel(selected, x(selected), 222);
        if (c.onlineAccount()) {
            int state = selected == FRIENDS ? 1 : 0;
            if (friends[state] == null) friends[state] = c.combatRoomArt("room-tool2-" + state);
            Image icon = friends[state];
            c.img(icon, x(FRIENDS) - icon.getWidth() / (2 * c.s), c.lobbyIconY(FRIENDS, icon.getHeight() / c.s));
            if (state == 1) drawLabel(9, x(FRIENDS), 222);
            c.net.chat.drawPreview();
        }
    }

    int x(int index) { return index == 3 && !c.onlineAccount() ? 108 : X[index]; }

    void drawLabel(int index, int center, int y) {
        if (labels[index] == null)
            labels[index] = c.combatRoomArt("home-label-" + index);
        Image label = labels[index];
        if (label != null)
            c.img(label, center - label.getWidth() / (2 * c.s), y);
    }

    // Cached gold contours follow the enlarged building, with its feet anchored to
    // the same plot.
    Image outline(Image image) {
        int pad = 3 * c.s, w = image.getWidth(), h = image.getHeight(), stride = w + pad * 2;
        int[] source = new int[w * h], pixels = new int[stride * (h + pad * 2)];
        image.getRGB(source, 0, w, 0, 0, w, h);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((source[y*w+x]>>>24)>96&&(x==0||y==0||x==w-1||y==h-1||(source[y*w+x-1]>>>24)<=96||(source[y*w+x+1]>>>24)<=96||(source[(y-1)*w+x]>>>24)<=96||(source[(y+1)*w+x]>>>24)<=96)){
            int step=Math.max(1,c.s);for(int dy=-pad;dy<=pad;dy+=step)for(int dx=-pad;dx<=pad;dx+=step)if(dx*dx+dy*dy<=pad*pad)pixels[(y+dy+pad)*stride+x+dx+pad]=0xffffce00;
        }
        return Image.createRGBImage(pixels, stride, h + pad * 2, true);
    }

    int hit(int x, int y) {
        if(!c.onlineAccount())return x>=278&&y>=194?4:-1;
        if (x >= 48 && x < 118 && y >= 48 && y < 113)
            return 7;
        if (x >= 67 && x < 122 && y >= 110 && y < 180)
            return 6;
        if (x >= 180 && x < 291 && y >= 114 && y < 230)
            return 5;
        if (x >= 119 && x < 208 && y >= 50 && y < 162)
            return 2;
        if (y >= 184 && y < 240) {
            int[] order = c.onlineAccount() ? new int[] { 0, 1, FRIENDS, 3, 4 } : new int[] { 0, 1, 3, 4 };
            for (int i = 0; i < order.length; i++)
                if (Math.abs(x - x(order[i])) < (c.onlineAccount() ? 21 : 24))
                    return order[i];
        }
        return -1;
    }

    void move(int dir) {
        if(!c.onlineAccount()){c.lobbySelection=4;return;}
        if (c.lobbySelection < 0 || c.lobbySelection == FRIENDS && !c.onlineAccount()) {
            c.lobbySelection = 2;
            return;
        }
        // Building neighbours follow the island paths, rather than drifting into the
        // bottom toolbar.
        int at = c.lobbySelection;
        if (at == 6) {
            c.lobbySelection = dir == 1 ? 1 : dir == 2 ? 5 : dir == 4 ? 7 : 0;
            return;
        }
        if (at == 7 && (dir == 2 || dir == 8)) {
            c.lobbySelection = dir == 2 ? 2 : 6;
            return;
        }
        if (at == 5 && (dir == 1 || dir == 4)) {
            c.lobbySelection = dir == 1 ? 6 : 2;
            return;
        }
        if (at == 2 && (dir == 1 || dir == 2 || dir == 8)) {
            c.lobbySelection = dir == 1 ? 7 : dir == 2 ? 5 : 6;
            return;
        }
        int best = c.lobbySelection, score = 99999;
        for (int i = 0; i < X.length; i++)
            if (i != c.lobbySelection && (i != FRIENDS || c.onlineAccount())) {
                int dx = x(i) - x(c.lobbySelection), dy = Y[i] - Y[c.lobbySelection],
                        along = dir == 1 ? -dx : dir == 2 ? dx : dir == 4 ? -dy : dy,
                        cross = dir == 1 || dir == 2 ? Math.abs(dy) : Math.abs(dx);
                if (along > 0 && along + cross * 2 < score) {
                    score = along + cross * 2;
                    best = i;
                }
            }
        c.lobbySelection = best;
    }

    void activate() {
        int choice = c.lobbySelection;
        if(choice==4){c.popup.confirmQuit();return;}
        if (c.net.pending.equals("logout") || c.net.operation.equals("logout")) {
            c.message("Đang đăng xuất.");
            return;
        }
        if(!c.onlineAccount()){c.net.loginForm(false);return;}
        if(choice==6){c.ranking.open();return;}
        if (choice == 5 && c.onlineAccount()) {
            c.net.openDungeon();
            return;
        }
        if (c.onlineAccount() && c.net.homeAfterLogin) {
            c.popup.notice("Tài khoản", "Đang tải dữ liệu tài khoản. Vui lòng chờ.");
            return;
        }
        if (choice == 2 && c.onlineAccount()) {
            c.net.open();
            return;
        }
        if (choice == FRIENDS && c.onlineAccount()) {
            c.net.social.open(false);
            return;
        }
        if (choice == 6) {
            c.net.open();
        } else if (choice == 7)
            c.forge.open();
        else if (choice == 5)
            c.offlineDungeon.open();
        else
            c.activateLegacyLobby();
    }
}
