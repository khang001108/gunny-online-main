import javax.microedition.lcdui.*;

/**
 * Solo waits for humans; teams expose six seats; custom rooms keep their own
 * layout.
 */
final class OnlineWaitingRoom {

    final OnlineClient net;
    final PhoneCanvas c;
    Image slotClose, slotEmote;
    Image soloFrame, slot, closed, redLabel, blueLabel, redSwitch, blueSwitch, button, exit, exitSelected, inviteArt,
            startArt, readyArt, actionGround, randomPreview, classic;
    final Image[] portraits = new Image[6];
    final String[] portraitKeys = new String[6];

    OnlineWaitingRoom(OnlineClient n) {
        net = n;
        c = n.c;
    }

    void reset() {
        c.roomSelection = versus() ? 4 : 2;
        for (int i = 0; i < 6; i++) {
            portraits[i] = null;
            portraitKeys[i] = null;
        }
    }

    void releaseArt() {
        soloFrame = slot = closed = redLabel = blueLabel = redSwitch = blueSwitch = button = exit = exitSelected = inviteArt = startArt = readyArt = actionGround = randomPreview = classic = null;
        for (int i = 0; i < 6; i++) {
            portraits[i] = null;
            portraitKeys[i] = null;
        }
    }

    int self() {
        return Math.max(0, Math.min(net.count - 1, net.myActor));
    }

    int peer() {
        return 1 - self() % 2;
    }

    boolean versus() {
        return net.roomKind == 1;
    }

    boolean enemyPresent() {
        for (int i = 0; i < net.count; i++)
            if (i % 2 != self() % 2 && net.present[i] && !net.bots[i])
                return true;
        return false;
    }

    void portrait(int actor, int x, int y) {
        int offset = !versus() && net.count != 6 && net.roomKind != 3 ? 28 : 0;
        c.net.chat.roomSpeech.head(actor, x + (net.count == 6 ? 14 : 25), y - offset + 13);
        try {
            if (portraits[actor] == null || !net.standingKeys[actor].equals(portraitKeys[actor])) {
                portraits[actor] = net.buildStanding(actor, net.count == 6 ? 29 : 52, net.count == 6 ? 43 : 64);
                portraitKeys[actor] = net.standingKeys[actor];
            }
            c.vip.back(net.actorVip[actor], net.actorWing[actor], x + (net.count == 6 ? 14 : 26),
                    y + (net.count == 6 ? 43 : 70), net.count == 6 ? 1 : 0, false);
            c.img(portraits[actor], x, y);
            c.drawLevel(net.actorExperience[actor], x + (net.count == 6 ? 14 : 45), y - 4);
        } catch (Exception ignored) {
        }
    }

    String rewardLabel(int actor) {
        return (net.rewardWaitingMask & (1 << actor)) != 0 ? "Nhận quà" : "Đã xong";
    }

    void hostBadge(int actor, int x, int y) {
        if (net.rewardWaitingMask != 0) {
            c.pcText(rewardLabel(actor), x, y);
            return;
        }
        if (actor == net.hostActor) {
            c.loadRoomArt();
            c.img(c.roomHost, x, y);
        }
    }

    void load() {
        if (soloFrame != null)
            return;
        soloFrame = c.combatRoomArt("teams-room-frame");
        slot = c.combatRoomArt("teams-room-small-slot");
        closed = c.combatRoomArt("teams-room-open-slot");
        redLabel = c.combatRoomArt("teams-red-label");
        blueLabel = c.combatRoomArt("teams-blue-label");
        redSwitch = c.combatRoomArt("teams-red-switch");
        blueSwitch = c.combatRoomArt("teams-blue-switch");
        slotClose = c.combatRoomArt("room-slot-close");
        slotEmote = c.combatRoomArt("room-slot-emote");
        button = c.combatRoomArt("room-action-blank");
        startArt = c.combatRoomArt("room-action-start");
        readyArt = c.combatRoomArt("room-action-ready");
        actionGround = c.combatRoomArt("room-action-ground");
        exit = c.combatRoomArt("ui-close");
        exitSelected = c.combatRoomArt("ui-close-selected");
        inviteArt = c.combatRoomArt("teams-invite");
        randomPreview = c.combatRoomArt("map-room-random-preview");
    }

    void drawAction(int x, int y) {
        Image art = net.host ? startArt : readyArt;
        int state = c.roomSelection == 5 ? 1 : 0;
        c.part(art, state * 44, 0, 44, 44, x, y, false);
        if (net.inBattle || net.matchQueued || !net.host && (net.readyMask & (1 << self())) != 0)
            c.pcCenter(actionLabel(), x + 22, y + 44);
    }

    void drawExit() {
        c.img(c.roomSelection == 7 ? exitSelected : exit, 294, 3);
    }

    String waitText() {
        int votes = 0, people = 0;
        for (int i = 0; i < net.count; i += 2)
            if (net.present[i] && !net.bots[i]) {
                people++;
                if ((net.botVoteMask & (1 << i)) != 0)
                    votes++;
            }
        return net.queueSeconds() + "s • " + c.keyBindings.name('h') + ": đấu bot (" + votes + "/" + people + ")";
    }

    String actionLabel() {
        if (net.matchmakingCap && !net.inBattle && net.host)
            return net.matchQueued ? "Đang tìm" : "Bắt đầu";
        return net.inBattle ? "Về trận"
                : net.host ? (net.roomKind == 2 ? "Chờ đủ 6" : "Sẵn sàng")
                        : (net.readyMask & (1 << self())) != 0 ? "Đã sẵn" : "Sẵn sàng";
    }

    void draw() {
        load();
        c.all();
        if (versus())
            drawVersus();
        else if (net.count == 6)
            drawTeams();
        else
            drawClassic();
    }

    void drawTeams() {
        c.loadout.load();
        c.img(c.loadout.background, 0, 0);
        c.img(c.loadout.left, 0, 0);
        c.img(redLabel, 7, 21);
        c.img(blueLabel, 7, 109);
        c.img(redSwitch, 6, 77);
        c.img(blueSwitch, 6, 165);
        for (int team = 0; team < 2; team++)
            for (int n = 0; n < 3; n++) {
                int actor = team + n * 2, x = 46 + n * 54, y = 16 + team * 88;
                c.img(net.present[actor] ? c.loadout.teamCard
                        : (net.closedSlots & (1 << actor)) != 0 ? c.loadout.teamClosed : c.loadout.teamWaiting, x, y);
                if (c.roomSelection == 60 + actor)
                    c.drawSelectionGlow(x - 1, y - 1, 52, 78);
                if (net.present[actor]) {
                    c.pcText(c.pcShort(net.names[actor], 43), x + 3, y + 3);
                    portrait(actor, x + 10, y + 16);
                    if (!(net.host && actor == self()) && (net.readyMask & (1 << actor)) != 0) {
                        c.loadRoomArt();
                        c.img(c.roomReady, x + 2, y + 28);
                    }
                    if (net.rewardWaitingMask != 0 && !net.bots[actor])
                        c.pcCenter(rewardLabel(actor), x + 25, y + 63);
                    else if (actor == net.hostActor)
                        hostBadge(actor, x + 2, y + 63);
                    else
                        c.pcCenter(net.controls[actor] ? "Bạn" : net.bots[actor] ? "AI" : "Chờ", x + 25, y + 63);
                }
            }
        c.pcText("Tổ đội 3 đấu 3 • Chờ sẵn sàng", 9, 188);
        c.loadout.draw(true);
        focus();
    }

    void drawClassic() {
        c.loadRoomArt();
        c.loadout.load();
        c.img(c.loadout.background, 0, 0);
        c.img(c.loadout.left, 0, 0);
        c.pcText("Phòng Online", 12, 10);
        c.wallet(8, 24);
        c.g.translate(0, -28 * c.s);
        for (int i = 0; i < 2; i++) {
            int actor = net.count > 2 ? i * 2 : i, x = i == 0 ? 5 : 108;
            c.img(net.present[actor] ? c.loadout.card
                    : (net.closedSlots & (1 << actor)) != 0 ? c.loadout.closed : c.loadout.waiting, x, 76);
            if (net.present[actor]) {
                c.pcText(c.pcShort(net.names[actor], 73), x + 5, 79);
                portrait(actor, x + 24, 97);
                if ((net.wornMasks[actor] & 1) != 0)
                    c.img(c.weapons[net.weapon[actor]], x + 8, 143);
                if (!(net.host && actor == self()) && (net.readyMask & (1 << actor)) != 0)
                    c.img(c.roomReady, x + 7, 125);
                if (net.rewardWaitingMask != 0 && !net.bots[actor])
                    c.pcCenter(rewardLabel(actor), x + 50, 169);
                else if (actor == net.hostActor)
                    hostBadge(actor, x + 7, 169);
                else
                    c.pcCenter(actor == self() ? "Bạn" : net.bots[actor] ? "AI đồng minh" : "Đồng minh", x + 50, 169);
            }
            if (i == 0) {
                c.img(slotEmote, x + 82, 79);
                if (net.emotes[actor] > 0)
                    c.img(slotEmote, x + 44, 92);
            } else if (net.host && !versus())
                c.img(slotClose, x + 82, 79);
        }
        c.g.translate(0, 28 * c.s);
        c.loadout.draw(true);
        focus();
    }

    void mapPreview(int x, int y) {
        int index = !enemyPresent() || net.roomMap < 0 ? 0 : net.roomMap + 1;
        c.img(index == 0 ? randomPreview : c.maps.banner(index), x, y);
        c.pcCenter(index == 0 ? "Ngẫu nhiên" : c.pcShort(PhoneCanvas.MAPS[net.roomMap], 88), x + 44, y + 31);
    }

    void drawVersus() {
        if (c.versusBg == null) {
            c.versusBg = c.combatRoomArt("room-loading-bg");
            c.versusLogo = c.combatRoomArt("room-loading-vs");
            c.versusMode = c.combatRoomArt("room-loading-mode");
        }
        c.img(c.versusBg, 0, 0);
        c.img(c.versusLogo, 124, 15);
        c.img(c.versusMode, 139, 73);
        for (int i = 0; i < 2; i++)
            if (net.present[i]) {
                int x = i == 0 ? (enemyPresent() ? 40 : 25) : 230;
                portrait(i, x, 91);
                c.img(c.weapons[net.weapon[i]], x + 27, 130);
                c.pcCenter(c.pcShort(net.names[i], 90), x + 20, 153);
                if (net.rewardWaitingMask != 0 && !net.bots[i])
                    c.pcCenter(rewardLabel(i), x + 20, 166);
                else if (i == net.hostActor)
                    hostBadge(i, x + 1, 166);
                else
                    c.pcCenter("Sẵn sàng", x + 20, 166);
            }
        c.pcText("Phòng #" + net.roomId + " • Đấu đơn", 8, 181);
        if (enemyPresent())
            c.pcText("Đã tìm thấy người chơi", 8, 194);
        mapPreview(160, 187);
    }

    void focus() {
        int f = c.roomSelection;
        if (f == 7 || f >= 20 || f == 2 || f == 5)
            return;
        if (f == 4)
            return;
        else if (f == 11)
            c.drawSelectionGlow(87, 50, 15, 13);
        else if (f == 10)
            c.drawSelectionGlow(188, 50, 15, 13);
        else if (f == 1)
            c.drawSelectionGlow(107, 47, 102, 107);
        else if (f == 8 || f == 9)
            c.drawSelectionGlow(5, f == 8 ? 76 : 164, 43, 18);
    }

    void notice(String title, String text) {
        c.popup.notice(title, text);
    }

    void invite() {
        net.inviteSeat = c.roomSelection >= 60 && c.roomSelection < 66 ? c.roomSelection - 60 : -1;
        if (net.socialCap) {
            net.social.open(true);
            return;
        }
        notice("MỜI VÀO PHÒNG", "Mã phòng: " + net.roomId + "\n\nBạn bè nhập mã phòng này để vào.");
    }

    void activate() {
        if (c.loadout.activate(true))
            return;
        int f = c.roomSelection;
        if (f >= 60 && f < 66) {
            int seat = f - 60;
            if (!net.host) {
                c.popup.notice("Ô chờ", "Chỉ chủ phòng được đóng hoặc mở ô");
                return;
            }
            if (net.present[seat]) {
                c.popup.notice("Ô chờ", "Ô đã có người chơi");
                return;
            }
            net.slotDraft = seat;
            net.request("slot");
            return;
        }
        if (versus() && f == 2)
            return;
        if (f == 7) {
            net.request("leave");
            return;
        }
        if (f == 5) {
            if (net.inBattle) {
                c.mode = c.game.over ? 5 : 1;
                return;
            }
            if (versus())
                return;
            if (c.requireWeapon())
                return;
            if (net.host) {
                if (net.roomKind == 2 && !net.matchmakingCap) {
                    notice("Tổ đội", "Chờ đủ 6 người. Khi mọi người sẵn sàng, trận tự bắt đầu.");
                    return;
                }
                if (!net.canStartRoom && !net.matchmakingCap) {
                    notice("Phòng", "Chờ người chơi thật vào phòng và sẵn sàng.");
                    return;
                }
                net.request("start");
            } else {
                net.readyDraft = (net.readyMask & (1 << self())) == 0;
                net.request("ready");
            }
            return;
        }
        if (f == 10) {
            net.slotDraft = self() < 2 ? self() % 2 + 2 : self() % 2;
            net.request("slot");
            return;
        }
        if (f == 11) {
            net.emoteDraft = net.emotes[self()] == 0 ? 1 : 0;
            net.request("emote");
            return;
        }
        if (f == 1) {
            if (net.host && !net.present[2]) {
                net.slotDraft = 2;
                net.request("slot");
            }
            return;
        }
        if (f == 2) {
            invite();
            return;
        }
        if (f == 4) {
            if (net.inBattle || !net.host) {
                notice("Bản đồ", net.inBattle ? PhoneCanvas.MAPS[net.roomMap] : "Chỉ chủ phòng được chọn bản đồ.");
                return;
            }
            c.maps.open(true);
            return;
        }
        if (f == 8 || f == 9) {
            net.teamDraft = f - 8;
            net.request("switchteam");
            return;
        }
        notice("ĐẠO CỤ", c.keyBindings.help());
    }

    int focusX(int id) {
        return id == 7 ? 305
                : id == 11 ? 106 : id == 10 ? 215 : id == 1 ? 178 : id == 4 ? 198 : id == 8 || id == 9 ? 34 : 285;
    }

    int focusY(int id) {
        return id == 7 ? 12
                : id == 11 || id == 10 ? 37
                        : id == 1 ? 80 : id == 2 ? 50 : id == 3 ? 100 : id == 8 ? 76 : id == 9 ? 164 : 173;
    }

    void key(int key, int dir, boolean ok) {
        if (!versus() && dir != 0) {
            c.loadout.move(dir, true);
            return;
        }
        if ((key == 'h' || key == 'H') && net.waitBotCap && net.queueSeconds() > 0 && !net.inBattle) {
            net.request("botvote");
            return;
        }
        if (dir != 0) {
            int[] order = versus() ? new int[] { 4 }
                    : net.count == 6 ? new int[] { 8, 9, 2, 3, 4, 5 }
                            : net.host ? new int[] { 11, 1, 10, 2, 3, 4, 5 } : new int[] { 11, 2, 3, 4, 5 };
            int fx = focusX(c.roomSelection), fy = focusY(c.roomSelection), best = c.roomSelection, score = 99999;
            for (int i = 0; i < order.length; i++) {
                int dx = focusX(order[i]) - fx, dy = focusY(order[i]) - fy,
                        along = dir == 1 ? -dx : dir == 2 ? dx : dir == 4 ? -dy : dy,
                        cross = dir == 1 || dir == 2 ? Math.abs(dy) : Math.abs(dx);
                if (along > 0 && along + cross * 3 < score) {
                    score = along + cross * 3;
                    best = order[i];
                }
            }
            c.roomSelection = best;
        } else if (ok || key == -6)
            activate();
    }

    void pointer(int x, int y) {
        if (net.count == 6 && x >= 46 && x < 208 && y >= 16 && y < 180) {
            int col = (x - 46) / 54, row = (y - 16) / 88;
            c.roomSelection = 60 + col * 2 + row;
            activate();
            return;
        }
        if (!versus() && net.count != 6 && x < 210 && y >= 48 && y < 154)
            y += 28;
        if (!versus() && c.loadout.pointer(x, y, true))
            return;
        if (!versus()) {
            if (net.count == 6) {
                if (x < 46 && y >= 67 && y < 97) {
                    c.roomSelection = 8;
                    activate();
                    return;
                }
                if (x < 46 && y >= 155 && y < 185) {
                    c.roomSelection = 9;
                    activate();
                    return;
                }
                if (x >= 46 && x < 208 && y >= 16 && y < 180) {
                    invite();
                    return;
                }
            } else {
                if (y >= 76 && y < 94 && x >= 87 && x < 104) {
                    c.roomSelection = 11;
                    activate();
                    return;
                }
                if (y >= 76 && y < 94 && x >= 188 && x < 207 && net.host) {
                    c.roomSelection = 10;
                    activate();
                    return;
                }
                if (x >= 108 && x < 209 && y >= 76 && y < 182) {
                    c.roomSelection = 1;
                    activate();
                    return;
                }
            }
            return;
        }
        if (!versus() && net.count != 6 && y >= 76 && y < 94) {
            if (x >= 99 && x < 120) {
                c.roomSelection = 11;
                activate();
                return;
            }
            if (x >= 208 && x < 232 && net.host) {
                c.roomSelection = 10;
                activate();
                return;
            }
        }
        if (net.waitBotCap && net.queueSeconds() > 0 && !net.inBattle && y >= 203 && x < 145) {
            net.request("botvote");
            return;
        }
        if (versus()) {
            if (x >= 164 && x < 258 && y >= 185)
                c.roomSelection = 4;
            else
                return;
        } else if (net.count == 6) {
            if (x >= 270 && y >= 60 && y < 104)
                c.roomSelection = 2;
            else if (x >= 270 && y >= 106 && y < 148)
                c.roomSelection = 3;
            else if (x >= 270 && y >= 150 && y < 192)
                c.roomSelection = 5;
            else if (x >= 165 && x < 259 && y >= 189)
                c.roomSelection = 4;
            else if (x >= 14 && x < 58 && y >= 67 && y < 85)
                c.roomSelection = 8;
            else if (x >= 14 && x < 58 && y >= 155 && y < 173)
                c.roomSelection = 9;
            else if (x >= 70 && x < 256 && y >= 16 && y < 178) {
                invite();
                return;
            } else
                return;
        } else {
            if (x >= 262 && y >= 143 && y < 190)
                c.roomSelection = 5;
            else if (x >= 141 && x < 258 && y >= 143 && y < 201)
                c.roomSelection = 4;
            else if (x >= 262 && y >= 30 && y < 76)
                c.roomSelection = 2;
            else if (x >= 262 && y >= 80 && y < 130)
                c.roomSelection = 3;
            else if (x >= 125 && x < 232 && y >= 29 && y < 134) {
                c.roomSelection = 1;
                activate();
                return;
            } else
                return;
        }
        activate();
    }
}
