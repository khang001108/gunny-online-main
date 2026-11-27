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
    // HF34: independent static HUD cache survives releaseLoadingArt().
    final Image[][] battleRings = new Image[3][2];
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
        battleReady=false; battleBytes=0;
        clearBattleRings();
        for(int i=0;i<6;i++){battleAngles[i]=0; battleWingIds[i]=-1; battleXp[i]=0;}
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

    void releaseLoadingArt() {
        for(int i=0;i<staticRings.length;i++)for(int j=0;j<staticRings[i].length;j++)staticRings[i][j]=null;
        for(int i=0;i<staticWings.length;i++)for(int j=0;j<staticWings[i].length;j++)staticWings[i][j]=null;
    }

    // Encounter caches are immutable after loading, even as the player changes pose.
    boolean battleReady;
    int battleBytes;
    final int[] battleAngles=new int[6],battleWingIds=new int[6],battleXp=new int[6];
    boolean battleScreen(){return c.mode==1||c.mode==2||c.mode==14;}
    boolean roomFor(int bytes){
        return battleBytes+bytes<=(c.s==1?6:20)*1024*1024
            && Runtime.getRuntime().freeMemory()>bytes+(c.s==1?1024:3072)*1024;
    }
    void loadBattleAngle(int w,int a,boolean essential){
        if((battleAngles[w]&(1<<a))!=0)return;
        int bytes=WingBattleArt.bytes(c.s,w,a);
        if(!essential&&!roomFor(bytes))return;
        try{
            Image inner=c.combatRoomArt("wing812-packed-"+w+"-"+a+"-0");
            Image outer=c.combatRoomArt("wing812-packed-"+w+"-"+a+"-1");
            if(inner==null||outer==null)return;
            sideWings[w][a][0]=inner;sideWings[w][a][1]=outer;
            battleAngles[w]|=1<<a;battleBytes+=bytes;
        }catch(OutOfMemoryError low){ /* leave the angle absent; never decode in paint */ }
    }
    Image preloadSmall(String name,int bytes,boolean essential){
        if(!essential&&!roomFor(bytes))return null;
        try{Image image=c.combatRoomArt(name);if(image!=null)battleBytes+=bytes;return image;}
        catch(OutOfMemoryError low){return null;}
    }
    // Bpow uses a single tier-specific frame, prepared only during Loading.
    // No animated cut-in atlas, no image decoding in the draw path.
    void preparePowRings(int mask){
        int size=SIZE[3],pixelBytes=size*size*4*c.s*c.s;
        for(int k=0;k<3;k++)if((mask&(1<<k))!=0&&rings[k][3]==null)
            rings[k][3]=preloadSmall("vip-ring-hf18-"+k+"-1",pixelBytes,true);
    }
    void prepareBattle(){
        if(battleReady)return;
        int wingMask=0,ringMask=0;
        for(int i=0;i<6;i++){
            battleWingIds[i]=rawActorWing(i);battleXp[i]=rawActorExp(i);
            // Loading may precede the first battle snapshot: use authoritative lobby equipment.
            if(c.onlineBattle()){
                boolean player=i<c.net.count&&(c.net.roomKind==3?c.net.present[i]&&i%2==0:c.net.present[i]||c.net.bots[i]);
                battleWingIds[i]=player?c.net.actorWing[i]:-1;
                battleXp[i]=player?c.net.actorVip[i]:0;
            }
            if(battleWingIds[i]>=0&&battleWingIds[i]<6)wingMask|=1<<battleWingIds[i];
            int lv=VipRules.level(battleXp[i]);if(lv>0)ringMask|=1<<((lv-1)/3);
        }
        // HF34: six small static rings first (all tiers, both HUD sizes).
        // Do not let the optional animated wing caches starve these images.
        for(int k=0;k<3;k++)for(int context=1;context<=2;context++){
            int size=SIZE[context];
            if(battleRings[k][context-1]==null)
                battleRings[k][context-1]=preloadSmall("vip796-ring-static-"+k+"-"+size,size*size*4*c.s*c.s,true);
        }
        preparePowRings(ringMask);
        for(int w=0;w<6;w++)if((wingMask&(1<<w))!=0)loadBattleAngle(w,2,true);
        // Allocate battle tilt art before optional animated portrait art.
        int[] angles={1,3,0,4};
        for(int i=0;i<angles.length;i++)for(int w=0;w<6;w++)if((wingMask&(1<<w))!=0)loadBattleAngle(w,angles[i],false);
        for(int w=0;w<6;w++)if((wingMask&(1<<w))!=0)for(int context=1;context<=2;context++){
            int size=WING_SIZE[context];
            wings[w][context]=preloadSmall("wing796-show-"+w+"-"+size,20*size*WING_HEIGHT[context]*4*c.s*c.s,false);
        }
        // HF34: no animated HUD ring atlases in battle.
        battleReady=true;
    }

    void releaseLargeEffects() {
        if(battleReady&&battleScreen())return;
        for (int i = 0; i < 3; i++){
            rings[i][0] = rings[i][3] = null;
        }
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
        if(context==3){
            // Bpow: fixed full-size frame, behind the cut-in actor.
            // Never load or rotate images while drawing the skill.
            Image image=rings[kind][3];
            if(image!=null)c.part(image,0,0,size,size,x-size/2,y-size,false);
            return;
        }
        if(battleScreen()&&(context==1||context==2)){
            Image image=battleRings[kind][context-1];
            c.img(image,x-size/2,y-size);
            return;
        }
        // Other screens retain their original animation.
        if(!battleScreen()&&context==0)for(int k=0;k<3;k++)if(k!=kind)rings[k][context]=null;
        if(rings[kind][context]==null&&!battleScreen())
            rings[kind][context]=c.combatRoomArt("vip-ring-"+kind+"-"+size);
        if(rings[kind][context]!=null)
            c.part(rings[kind][context],(frame%16)*size,0,size,size,x-size/2,y-size,false);
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
            if (wings[wing][context] == null && !battleScreen())
                wings[wing][context] = c.combatRoomArt("wing796-show-" + wing + "-" + size);
            c.part(wings[wing][context], (int) (System.currentTimeMillis() / 65 % 20) * size, 0, size, h, x - size / 2,
                    feet - h * 3 / 4 - wingLift, false);
        }
    }

    void backStatic(int xp, int wing, int x, int feet, int context) {
        if(!battleScreen())releaseBattleWings();
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

    void clearBattleRings() {
        for(int k=0;k<3;k++)for(int i=0;i<2;i++)battleRings[k][i]=null;
    }

    void releaseBattleWings() {
        battleReady=false; battleBytes=0;
        clearBattleRings();
        for(int w=0;w<6;w++)battleAngles[w]=0;
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
        boolean shooting = c.attackPose(actor) || c.freezeHolding(actor);
        int tilt = u.slope; // Keep terrain tilt even during charge / aim.
        if (u.face > 0)
            tilt = -tilt;
        int angle = Battle.clamp((tilt + 30 + 7) / 15, 0, 4), layer = outer ? 1 : 0;
        // Use cached art only, with its baked attachment pivot at (48,48).
        angle = WingBattleArt.nearest(battleAngles[wing], angle);
        if (angle < 0)
            return;
        int[] bounds = WingBattleArt.bounds(c.s);
        int p = WingBattleArt.index(wing, angle, layer);
        int width = bounds[p + 2], height = bounds[p + 3];
        int offsetX = u.face < 0 ? 96 - bounds[p] - width : bounds[p];
        // Body pivots around the feet in drawSlopedBody. Shooting body art is upright.
        // Root is at the lower back, not the feet (old +10 offset) or high above it.
        int spineX = -2 * u.face, spineY = shooting ? -10 : -12;
        int bodySlope = shooting ? 0 : u.slope;
        int cs = Battle.cos(bodySlope), sn = Battle.sin(bodySlope);
        int anchorX = (spineX * cs + spineY * sn) / 1024;
        int anchorY = (-spineX * sn + spineY * cs) / 1024;
        c.part(sideWings[wing][angle][layer], c.frame / 2 % 19 * width, 0, width, height,
                u.x - c.cameraX + anchorX - 48 + offsetX,
                u.y - c.cameraY + anchorY - 48 + bounds[p + 1], u.face < 0);
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
        return battleReady&&battleScreen()&&actor>=0&&actor<6?battleXp[actor]:rawActorExp(actor);
    }

    int rawActorExp(int actor) {
        return actor < 0 || actor >= 6 || actor >= c.game.unitCount || c.game.units[actor].npc != 0 ? 0
                : c.onlineBattle() ? c.net.actorVip[actor] : actor == 0 ? exp() : 0;
    }

    int actorWing(int actor) {
        return battleReady&&battleScreen()&&actor>=0&&actor<6?battleWingIds[actor]:rawActorWing(actor);
    }

    int rawActorWing(int actor) {
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
            if ((owned & (1 << i)) != 0) {
                c.pcText("Đã có", b[0] + 4, b[1] + 28);
            } else if (VipRules.level(exp()) < VipRules.WING_VIP[i]) {
                c.pcText("Cần VIP " + VipRules.WING_VIP[i], b[0] + 4, b[1] + 28);
            } else {
                // HF32: same shopping-money icon and spacing as ordinary Shop items.
                c.img(c.shop.money, b[0] + 29, b[1] + 27);
                c.pcText("" + VipRules.WING_PRICE[i], b[0] + 43, b[1] + 27);
            }
            if (i == shopFocus)
                c.drawItemSelection(b[0], b[1], b[2], b[3], false);
        }
        c.pcCenter("Cần cấp VIP • OK: thử • B: mua", 229, 217);
    }

    void buy() {
        // HF29: validate the selected wing and show the actual rejection reason.
        if(shopFocus<0||shopFocus>=VipRules.WING_PRICE.length)return;
        if(!c.onlineAccount()||!c.net.vipCap){c.message("Đăng nhập và cập nhật server VIP");return;}
        if(!loaded){c.message("Chờ tải dữ liệu VIP");return;}
        if (VipRules.level(exp()) < VipRules.WING_VIP[shopFocus]) {
            c.message("Cần VIP " + VipRules.WING_VIP[shopFocus]);
            return;
        }
        if ((owned & (1 << shopFocus)) != 0) {
            c.message("Đã sở hữu cánh");
            return;
        }
        if(c.shoppingMoney<VipRules.WING_PRICE[shopFocus]){c.message("Thiếu "+(VipRules.WING_PRICE[shopFocus]-c.shoppingMoney)+" tiền mua sắm");return;}
        submit("vipbuy", shopFocus);
    }
}
