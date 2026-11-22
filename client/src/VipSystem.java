import java.io.*;
import javax.microedition.lcdui.Image;

/**
 * Server-owned inventory and source-derived graphics. All image caches are
 * screen-local.
 */
final class VipSystem {
    final PhoneCanvas c;
    int experience, owned, equipped = -1;
    final int[] cards = new int[13];
    boolean loaded;
    long poll;
    int item, shopFocus;
    String requestKey = "", requestOp = "";
    int requestItem;
    final Image[][] staticRings = new Image[3][3], staticWings = new Image[6][3];
    final Image[][] rings = new Image[3][4], wings = new Image[6][3];
    final Image[] badges = new Image[10], wingIcons = new Image[6], cardIcons = new Image[2];
    Image progress, fill;
    int barFrom, barTo;
    long barAt;
    static final int[] SIZE = { 72, 44, 24, 144 };

    VipSystem(PhoneCanvas c) {
        this.c = c;
    }

    void resetAccount() {
        experience = owned = 0;
        equipped = -1;
        loaded = false;
        poll = 0;
        requestKey = requestOp = "";
        for (int i = 0; i < 13; i++)
            cards[i] = 0;
    }

    int exp() {
        return c.onlineAccount() ? experience : 0;
    }

    int wing() {
        return c.onlineAccount() ? equipped : -1;
    }

    int hp() {
        return VipRules.hp(wing());
    }

    void release() {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                staticRings[i][j] = null;
        for (int i = 0; i < 6; i++)
            for (int j = 0; j < 3; j++)
                staticWings[i][j] = null;
        for (int w = 0; w < 6; w++)
            for (int a = 0; a < 5; a++)
                for (int l = 0; l < 2; l++)
                    sideWings[w][a][l] = null;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 4; j++)
                rings[i][j] = null;
        for (int i = 0; i < 6; i++)
            for (int j = 0; j < 3; j++)
                wings[i][j] = null;
        for (int i = 0; i < 10; i++)
            badges[i] = null;
        for (int i = 0; i < 6; i++)
            wingIcons[i] = null;
        cardIcons[0] = cardIcons[1] = null;
        progress = fill = sparkle = null;
    }

    void releaseLargeEffects() {
        for (int i = 0; i < 3; i++)
            rings[i][0] = rings[i][3] = null;
        for (int i = 0; i < 6; i++)
            wings[i][0] = null;
    }

    Image cardIcon(int n) {
        int i = n == 12 ? 1 : 0;
        if (cardIcons[i] == null)
            cardIcons[i] = c.combatRoomArt("vip-card-" + i);
        return cardIcons[i];
    }

    Image wingIcon(int n) {
        if (wingIcons[n] == null)
            wingIcons[n] = c.combatRoomArt("vip-wing-icon-" + n);
        return wingIcons[n];
    }

    String wingName(int n) {
        return n == 5 ? "Cánh phó bản" : VipRules.WING_NAMES[n];
    }

    void read(DataInputStream in) throws IOException {
        int xp = in.readInt(), mask = in.readUnsignedByte(), wear = in.readByte();
        if (xp < 0 || xp > 21200 || mask > 31 || wear < -1 || wear > 5)
            throw new IOException("Sai trạng thái VIP");
        int[] next = new int[13];
        for (int i = 0; i < 13; i++) {
            next[i] = in.readInt();
            if (next[i] < 0 || next[i] > 999999)
                throw new IOException("Sai số thẻ VIP");
        }
        int money = in.readInt();
        barFrom = VipRules.level(xp) == VipRules.level(experience) && VipRules.need(experience) > 0
                ? 51 * VipRules.within(experience) / VipRules.need(experience)
                : 0;
        barTo = VipRules.need(xp) == 0 ? 51 : 51 * VipRules.within(xp) / VipRules.need(xp);
        barAt = System.currentTimeMillis();
        experience = xp;
        owned = mask;
        equipped = wear;
        for (int i = 0; i < 13; i++)
            cards[i] = next[i];
        c.shoppingMoney = money;
        loaded = true;
        poll = System.currentTimeMillis();
        c.bag.list();
    }

    void submit(String op, int selected) {
        if (!c.onlineAccount() || !c.net.vipCap) {
            c.message("Đăng nhập và cập nhật server VIP");
            return;
        }
        if (c.net.pending.startsWith("vip") || c.net.operation.equals("vipuse") || c.net.operation.equals("vipbuy")
                || c.net.operation.equals("vipequip"))
            return;
        if (!op.equals(requestOp) || selected != requestItem || requestKey.length() == 0) {
            requestKey = c.nextEconomyKey();
            requestOp = op;
            requestItem = selected;
        }
        item = selected;
        c.net.request(op);
    }

    void progress() {
        badge(exp(), 88, 54);
        c.pcCenter(VipRules.need(exp()) == 0 ? "MAX" : VipRules.within(exp()) + "/" + VipRules.need(exp()), 98, 69);
    }

    void badge(int xp, int x, int y) {
        int lv = VipRules.level(xp);
        if (lv > 0 && lv <= 9) {
            if (badges[lv] == null)
                badges[lv] = NativeUi.fit(c.combatRoomArt("vip-icon-" + lv), 20 * c.s, 13 * c.s, 0);
            c.img(badges[lv], x, y);
        } else
            c.pcText("VIP", x, y);
    }

    void ring(int kind, int context, int frame, int x, int y) {
        int size = SIZE[context];
        // Small avatar rings may show several VIP tiers in the same frame.
        if(context==0||context==3)for(int k=0;k<3;k++)if(k!=kind)rings[k][context]=null;
        if (rings[kind][context] == null)
            rings[kind][context] = c.combatRoomArt("vip-ring-" + kind + "-" + size);
        c.part(rings[kind][context], frame % 16 * size, 0, size, size, x - size / 2, y - size, false);
    }

    static final int[] WING_SIZE = { 112, 64, 32 }, WING_HEIGHT = { 72, 42, 22 };

    void back(int xp, int wing, int x, int feet, int context, boolean burst) {
        back(xp, wing, x, feet, context, burst, 0);
    }

    void back(int xp, int wing, int x, int feet, int context, boolean burst, int wingLift) {
        if (c.mode == 0 || c.mode == 5 || c.mode == 6 || c.mode == 8) {
            releaseBattleWings();
            if (context == 0)
                for (int w = 0; w < 6; w++)
                    if (w != wing)
                        wings[w][0] = null;
        }
        int lv = VipRules.level(xp);
        if (lv > 0)
            ring((lv - 1) / 3, context, (int) (System.currentTimeMillis() / 75 % 16), x, feet);
        if (wing >= 0 && wing < 6 && context < 3) {
            int size = WING_SIZE[context], h = WING_HEIGHT[context];
            if (wings[wing][context] == null)
                wings[wing][context] = c.combatRoomArt("wing796-show-" + wing + "-" + size);
            c.part(wings[wing][context], (int) (System.currentTimeMillis() / 65 % 20) * size, 0, size, h, x - size / 2,
                    feet - h * 3 / 4 - wingLift, false);
        }
    }

    void backStatic(int xp, int wing, int x, int feet, int context) {
        releaseBattleWings();
        int lv = VipRules.level(xp);
        if (lv > 0) {
            int kind = (lv - 1) / 3, size = SIZE[context];
            if (staticRings[kind][context] == null)
                staticRings[kind][context] = c.combatRoomArt("vip796-ring-static-" + kind + "-" + size);
            c.img(staticRings[kind][context], x - size / 2, feet - size);
        }
        if (wing >= 0 && wing < 6) {
            int size = WING_SIZE[context], h = WING_HEIGHT[context];
            if (staticWings[wing][context] == null)
                staticWings[wing][context] = c.combatRoomArt("wing796-static-" + wing + "-" + size);
            c.img(staticWings[wing][context], x - size / 2, feet - h * 3 / 4);
        }
    }

    void releaseBattleWings() {
        for (int w = 0; w < 6; w++)
            for (int a = 0; a < 5; a++)
                sideWings[w][a][0] = sideWings[w][a][1] = null;
    }

    final Image[][][] sideWings = new Image[6][5][2];

    void battleWing(int actor, boolean outer) {
        int wing = actorWing(actor);
        if (wing < 0 || wing > 5)
            return;
        Battle.Unit u = c.game.units[actor];
        int tilt = c.attackPose(actor) || c.freezeHolding(actor) ? 0 : u.slope;
        if (u.face > 0)
            tilt = -tilt;
        int angle = Battle.clamp((tilt + 30 + 7) / 15, 0, 4), layer = outer ? 1 : 0;
        if (sideWings[wing][angle][layer] == null)
            sideWings[wing][angle][layer] = c.combatRoomArt("wing795-game-" + wing + "-" + angle + "-" + layer);
        boolean shooting = c.attackPose(actor) || c.freezeHolding(actor);
        int bodySlope = shooting ? 0 : u.slope;
        int spineY = shooting ? -10 : -13, cs = Battle.cos(bodySlope), sn = -Battle.sin(bodySlope), spineX = -u.face;
        int anchorX = (spineX * cs - spineY * sn) / 1024, anchorY = (spineX * sn + spineY * cs) / 1024;
        c.part(sideWings[wing][angle][layer], c.frame / 2 % 19 * 96, 0, 96, 96, u.x - c.cameraX + anchorX - 48,
                u.y - c.cameraY + anchorY - 48+10, u.face < 0);
    }

    boolean angleUsed(int wing, int angle) {
        for (int i = 0; i < c.game.unitCount; i++)
            if (actorWing(i) == wing) {
                Battle.Unit u = c.game.units[i];
                int tilt = c.attackPose(i) || c.freezeHolding(i) ? 0 : u.slope;
                if (u.face > 0)
                    tilt = -tilt;
                if (Battle.clamp((tilt + 37) / 15, 0, 4) == angle)
                    return true;
            }
        return false;
    }

    int trialWing = -2;
    boolean info;
    Image lock;

    void drawInfo() {
        c.tooltip
                .goods(wingName(shopFocus), "Cánh",
                        "+" + VipRules.WING_HP[shopFocus] + " HP khi trang bị. Cần VIP " + VipRules.WING_VIP[shopFocus]
                                + ". Giá " + VipRules.WING_PRICE[shopFocus] + " tiền. Vĩnh viễn.",
                        c.shop.box(shopFocus));
    }

    Image sparkle;

    Image star() {
        if (sparkle == null)
            sparkle = c.combatRoomArt("forge789-star");
        return sparkle;
    }

    int actorExp(int actor) {
        return actor < 0 || actor >= 6 || actor >= c.game.unitCount || c.game.units[actor].npc != 0 ? 0
                : c.onlineBattle() ? c.net.actorVip[actor] : actor == 0 ? exp() : 0;
    }

    int actorWing(int actor) {
        return actor < 0 || actor >= 6 || actor >= c.game.unitCount || c.game.units[actor].npc != 0 ? -1
                : c.onlineBattle() ? c.net.actorWing[actor] : actor == 0 ? wing() : -1;
    }

    void drawShop() {
        for (int i = 0; i < 5; i++) {
            int[] b = c.shop.box(i);
            c.img(c.shop.card, b[0], b[1]);
            c.img(wingIcon(i), b[0] + 3, b[1] + 3);
            c.pcText(c.pcShort(wingName(i), 48), b[0] + 27, b[1] + 3);
            if (VipRules.level(exp()) < VipRules.WING_VIP[i]) {
                if (lock == null)
                    lock = c.combatRoomArt("roomlist-lock");
                c.img(lock, b[0] + 61, b[1] + 24);
            }
            c.pcText((owned & (1 << i)) != 0 ? "Đã có"
                    : VipRules.level(exp()) < VipRules.WING_VIP[i] ? "Cần VIP " + VipRules.WING_VIP[i]
                            : "" + VipRules.WING_PRICE[i] + " tiền",
                    b[0] + 4, b[1] + 28);
            if (i == shopFocus)
                c.drawItemSelection(b[0], b[1], b[2], b[3], false);
        }
        c.pcCenter("Cần cấp VIP • OK: thử • B: mua", 229, 217);
    }

    void buy() {
        if (VipRules.level(exp()) < VipRules.WING_VIP[shopFocus]) {
            c.message("Cần VIP " + VipRules.WING_VIP[shopFocus]);
            return;
        }
        if ((owned & (1 << shopFocus)) != 0) {
            c.message("Đã sở hữu cánh");
            return;
        }
        submit("vipbuy", shopFocus);
    }
}
