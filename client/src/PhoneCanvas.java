import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;
import javax.microedition.rms.RecordStore;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Native-resolution, low-memory MIDP renderer. All positions use a 320x240
 * layout.
 */
final class PhoneCanvas extends Canvas implements Runnable, Battle.Ground {
    final GunnyMIDlet app;
    final GiftBag gifts=new GiftBag(this);
    final EconomyInventory economy=new EconomyInventory(this);
    final OfflineDungeon offlineDungeon=new OfflineDungeon(this);
    final HomeScreen home=new HomeScreen(this);final Ranking793 ranking=new Ranking793(this);final ForgeScreen forge=new ForgeScreen(this);final StoneShop stoneShop=new StoneShop(this);DungeonScreen dungeon;
    int economySequence;String nextEconomyKey(){return "g780-"+System.currentTimeMillis()+"-"+(++economySequence)+"-"+Math.abs(game.rand(1000000));}
    OnlineClient net;

    volatile boolean offlineTest;
    int testSavedMask, testSavedOwned, testSavedWeapon;
    boolean testSavedAim;

    void startOfflineTest() {
        if(onlineAccount()||net.inBattle||net.busy||net.pending.length()>0||net.operation.length()>0||net.wake.waiting||resetPending)return;
        if(!offlineTest){
            testSavedMask=wornMask;testSavedOwned=owned;testSavedWeapon=equipped;testSavedAim=game.cheatAim;
            offlineTest=true;
        }
        if(!enabledWeapon(equipped))equipped=0;
        wornMask|=1;
        for(int i=0;i<ACTIVE_WEAPONS.length;i++)owned|=1<<ACTIVE_WEAPONS[i];
        game.cheatAim=false;
        net.startupTried=true;net.auth.clearEditor();net.auth.releaseArt();
        training=false;offlineDungeon.active=false;twoPlayers=false;allyMode=0;chosenAllyBot=-1;
        randomMap=false;selectedMap=0;turnSeconds=12;
        popup.opened=false;popup.resumeOffline();presentation.settings=false;presentation.showFps=true;
        held=fireKey=0;game.charging=false;
        frameGap.reset();
        loadingRequested=true;resetPending=true;
    }

    void finishOfflineTest(){
        if(!offlineTest)return;
        offlineTest=false;wornMask=testSavedMask;owned=testSavedOwned;equipped=testSavedWeapon;game.cheatAim=testSavedAim;
        net.loginForm(false);
    }

    boolean onlineBattle() {
        return net != null && net.inBattle;
    }

    boolean onlineAccount() {
        return net != null && net.logged;
    }

    int experience, offlineExperience, serverRank;
    int shoppingMoney, wornMask = 3;
    final ShopInventory inventory = new ShopInventory(this);
    final ShopScreen shop = new ShopScreen(this);
    final BagScreen bag = new BagScreen(this);
    final RoomLoadout loadout = new RoomLoadout(this);
    final Battle game = new Battle(this);
    final MapPicker maps = new MapPicker(this);
    int turnSeconds = 12;
    Thread loop;
    volatile boolean running, alive = true;
    int s = 1, ox, oy, cameraX, cameraY, map, selectedMap, menuSelection, propSelection;
    boolean freeCamera, cameraReturning, cameraOnPlayer;
    int cameraHeld, cameraActor = -1;
    int mode = 0; // 0 lobby, 1 battle, 2 pause, 3 tools, 4 help, 5 result
    int muzzleTicks;
    volatile int loadTicks;
    int allyMode, chosenAllyBot = -1, botChoice, mapCursor;
    boolean loadingRequested, randomMap = true;
    static final String[] BOT_NAMES = { "Minh Anh", "Tuấn Phong", "Hoàng Nam", "Gia Huy", "Ngọc Linh", "Thu Hà",
            "Mai Chi", "Bảo Ngọc" };

    static final int[] BOT_SKINS = {1,3,5,7,0,2,4,6}, BOT_WEAPONS = {9,0,3,5};
    static final int[] LOOK_ORDER = {0,3,2,1,4};

    int botSex(int id) {
        return id / 4;
    }

    int botSkin(int id) {
        return BOT_SKINS[id];
    }

    int botWeapon(int id) {
        int w = BOT_WEAPONS[id % 4];
        return w == equipped ? nextWeapon(w, 1) : w;
    }

    void toggleRoomSlot() {
        allyMode = allyMode == 0 ? 2 : 0;
        chosenAllyBot = -1;
        roomMate = null;
    }

    void setRoomControl(int value) {
        if (allyMode != value) {
            allyMode = value;
            chosenAllyBot = -1;
            roomMate = null;
        }
    }

    void toggleRoomControl() {
        setRoomControl(allyMode == 1 ? 2 : 1);
    }

    void inviteBot() {
        botChoice = chosenAllyBot < 0 ? 0 : chosenAllyBot;
        net.social.openOffline();
        mode = 15;
    }

    void chooseRoomBot() {
        chosenAllyBot = botChoice;
        if (allyMode == 0)
            allyMode = 1;
        roomMate = null;
        mode = 10;
    }

    void confirmRoomMap() {
        randomMap = mapCursor == 0;
        if (!randomMap)
            selectedMap = mapCursor - 1;
        mode = 10;
    }

    final Image[] actors = new Image[6], actorStand = new Image[6];
    final int[] actorSex = new int[6], actorSkin = new int[6];
    Image versusBg, versusLogo, versusOK, versusMode, versusTime, versusDigits, pcFont, roomHost, roomReady, roomSelf,
            roomMate;
    final Image[] versusPortrait = new Image[6];

    boolean isLocalTurn() {
        if (onlineBattle())
            return net.localTurn();
        return game.teams ? game.humanTurn() : game.active == 0;
    }

    int playerActor() {
        if (onlineBattle())
            return net.localTurn() ? game.active : net.myActor;
        return game.teams && isLocalTurn() ? game.active : 0;
    }

    String actorName(int i) {
        if(i<0||i>=game.unitCount)return "";
        if (game.units[i].npc > 0 && (game.dungeonStage>0 || offlineDungeon.active || onlineBattle() && net.roomKind == 3))
            return TribalRules.active(game)?TribalRules.name(game.units[i].npc):AntCaveRules.name(game.units[i].npc, map == 7);
        if (onlineBattle())
            return i<net.names.length&&net.names[i]!=null?net.names[i]:"";
        if(offlineDungeon.active&&i>0&&i%2==0)
            return offlineDungeon.allyAt(i)>=0?BOT_NAMES[offlineDungeon.allyAt(i)]:"";
        return i == 0 ? playerName
                : game.teams
                        ? (i == 2 && allyMode == 2 ? "Người 2"
                                : i == 2 && chosenAllyBot >= 0 ? BOT_NAMES[chosenAllyBot]
                                        : BOT_NAMES[(Math.max(0,game.units[i].botStyle) + (i % 2) * 4) % 8])
                        : training ? "Bia tập" : BOT_NAMES[(Math.max(0,game.units[i].botStyle) + (i<actorSex.length?actorSex[i]*4:0)) % 8];
    }

    void actorAppearance(int i, int anim, int x, int y, boolean flip) {
        if (onlineBattle()) {
            net.actor(i, anim, x, y, flip);
            return;
        }
        if (i > 0 && actors[i] != null)
            part(actors[i], anim * 32, 0, 32, 40, x, y, flip);
        else
            appearance(i == 0 ? gender : 1 - gender, anim, x, y, flip);
    }

    final Image[] loadingHeroes=new Image[6];
    final int[] loadingWidths=new int[6];
    boolean loadingParticipant(int index){return index>=0&&index<6&&index<game.unitCount&&(onlineBattle()&&net.roomKind==3?index%2==0&&net.present[index]:game.units[index].npc==0&&game.units[index].hp>0);}
    int loadingTeam(int index){return onlineBattle()&&net.roomKind==3?index%2:game.teams?game.units[index].team:index;}
    int[] loadingSlot(int actor){
        int team=loadingTeam(actor),count=0,leader=-1,rank=0;
        for(int i=0;i<game.unitCount&&i<6;i++)if(loadingParticipant(i)&&loadingTeam(i)==team){if(leader<0)leader=i;count++;}
        if(onlineBattle()&&net.hostActor>=0&&loadingParticipant(net.hostActor)&&loadingTeam(net.hostActor)==team)leader=net.hostActor;
        if(onlineBattle())for(int i=0;i<6;i++)if(loadingParticipant(i)&&loadingTeam(i)==team&&(net.loadingLeaders&(1<<i))!=0){leader=i;break;}
        int center=team==0?75:244;
        if(count<=1)return new int[]{center,76,92};
        if(count==2)return actor==leader?new int[]{center-24,58,80}:new int[]{center+30,36,52};
        if(actor==leader)return new int[]{center,50,72};
        for(int i=0;i<actor;i++)if(i!=leader&&loadingParticipant(i)&&loadingTeam(i)==team)rank++;
        return new int[]{center+(rank==0?-48:48),32,48};
    }
    boolean loadingReady(int actor){
        if(!onlineBattle())return actor!=0||loadTicks>=60;
        if(net.controls[actor])return loadTicks>=60;
        if(net.bots[actor])return true;
        return net.playerUiCap?(net.loadingMask&(1<<actor))!=0:net.phase==1;
    }
    boolean allLoadingReady(){for(int i=0;i<game.unitCount&&i<6;i++)if(loadingParticipant(i)&&!loadingReady(i))return false;return true;}
    boolean canFinishLoading(){return loadTicks>=60&&vip.battleReady&&(!onlineBattle()||net.phase==1);}
    void tickLoading() {
        if (mode == 14) {
            // Never report ready to the server until local assets have been prepared.
            if(loadTicks==59&&!vip.battleReady)prepareBattleAssets();
            if(loadTicks<60)loadTicks++;
            if (canFinishLoading()) {
                mode = 1;
                releaseLoadingArt();
                if (game.dungeonStage > 0) {
                    Battle.Unit player=game.units[0];
                    cameraX=Battle.clamp(player.x-160,0,160);
                    cameraY=Battle.clamp(player.y-135,0,40);
                }
                lastTurn=-1;
                syncTurnNotice();
            }
        }
    }

    int pcWidth(String value) {
        if(value==null)return 0;
        int w = 0;
        for (int i = 0; i < value.length(); i++)
            w += (widths[glyph(value.charAt(i))] * 8 + 5) / 11;
        return w;
    }

    void pcText(String value, int x, int y) {
        if(value==null)return;
        if (pcFont == null)
            pcFont = combatRoomArt("font-pc");
        for (int n = 0; n < value.length(); n++) {
            int idx = glyph(value.charAt(n));
            part(pcFont, idx % 16 * 8, idx / 16 * 10, 8, 10, x, y, false);
            x += (widths[idx] * 8 + 5) / 11;
        }
    }

    void pcCenter(String value, int x, int y) {
        pcText(value, x - pcWidth(value) / 2, y);
    }

    String pcShort(String name, int width) {
        if(name==null)return "";
        int measured=pcWidth(name);
        if(measured<=width)return name;
        int end=name.length(), dots=pcWidth("...");
        while(end>1&&measured+dots>width){
            measured-=(widths[glyph(name.charAt(--end))]*8+5)/11;
        }
        return name.substring(0,end)+"...";
    }

    String layerName(int sex, int k, int item, int mask, String prefix) {
        if ((mask & (1 << (k + 2))) != 0)
            return prefix + sex + "_" + (k + 1) + "_" + item;
        if (k == 0 || k == 3)
            return (prefix.equals("look") ? "base" : prefix.equals("stand") ? "base-stand" : "base-cutin") + sex + "-"
                    + (k == 0 ? "cloth" : "face");
        return null;
    }

    Image equipmentPreview(int sex, int costume, boolean custom, int[] parts, int mask, int w, int h)
            throws IOException {
        int[] source = new int[80 * 100 * s * s];
        if (custom) {
            int[] order = LOOK_ORDER;
            for (int i = 0; i < 5; i++) {
                int k = order[i];
                String name = layerName(sex, k, parts[k], mask, "cutin");
                if (name != null)
                    blendCutin(source, load(name));
            }
        } else
            blendCutin(source, load("cutin" + sex + "_0_" + costume));
        int width = w * s, height = h * s;
        int[] target = new int[width * height];
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                target[y * width + x] = source[y * 100 * s / height * 80 * s + x * 80 * s / width];
        return Image.createRGBImage(target, width, height, true);
    }

    Image standingPreview(int sex, int costume, boolean local, int w, int h) throws IOException {
        return equipmentPreview(sex, costume, local && customLook, look, local ? wornMask : 3, w, h);
    }

    void drawVersus() {
        all();
        if (versusBg == null || versusLogo==null || versusOK==null || versusMode==null || versusTime==null || versusDigits==null) {
            versusBg = combatRoomArt("room-loading-bg");
            versusLogo = combatRoomArt("room-loading-vs");
            versusOK = combatRoomArt("room-loading-ok");
            versusMode = combatRoomArt("room-loading-mode");
            versusTime = combatRoomArt("room-loading-time");
            versusDigits = combatRoomArt("room-loading-digits");
        }
        img(versusBg, 0, 0);
        img(versusLogo, 124, 15);
        if(TribalRules.active(game)){pcCenter("Your Team",72,8);pcCenter("Enemy Team",264,8);}
        img(versusTime, 137, 110);
        int seconds = Math.max(0, 60 - loadTicks / 30);
        part(versusDigits, seconds / 10 * 17, 0, 17, 21, 143, 120, false);
        part(versusDigits, seconds % 10 * 17, 0, 17, 21, 160, 120, false);
        int dungeonStage = offlineDungeon.active ? offlineDungeon.stage
                : onlineBattle() && net.roomKind == 3 ? net.dungeonStage : 0;
        if (dungeonStage > 0) pcCenter("Ải Viễn Chinh", 160, 74);
        else img(versusMode, 139, 73);
        if(dungeonStage>=3)img(tribalArt.cover(dungeonStage-2),224,48);
        else if (dungeonStage > 0) {
            int stageIndex = dungeonStage == 2 ? 1 : 0;
            if (dungeonLoadingCovers[stageIndex] == null)
                dungeonLoadingCovers[stageIndex] = combatRoomArt("dungeon-loading-stage-" + (stageIndex + 1));
            img(dungeonLoadingCovers[stageIndex], 220, 48);
        }
        for(int i=0;i<game.unitCount&&i<6;i++){
            if(!loadingParticipant(i))continue;
            int[] slot=loadingSlot(i);int center=slot[0],width=slot[1],height=slot[2];
            if(loadingHeroes[i]==null||loadingWidths[i]!=width){
                try{
                    loadingHeroes[i]=onlineBattle()?net.buildStanding(i,width,height):
                        standingPreview(i==0?gender:actorSex[i],i==0?skin:actorSkin[i],i==0,width,height);
                    loadingWidths[i]=width;
                }catch(IOException ignored){}
            }
            vip.backStatic(vip.actorExp(i),vip.actorWing(i),center,145,width>=52?0:1);
            img(loadingHeroes[i],center-width/2,145-height);
            img(weapons[game.units[i].weapon],center+width/3-8,127);
            if(loadingReady(i)){
                if(versusOK!=null)img(versusOK,center-versusOK.getWidth()/(2*s),149);else pcCenter("OK",center,149);
            }else pcCenter((onlineBattle()&&net.clock793?net.loadingPercent[i]+"%":Math.min(100,loadTicks*100/60)+"%"),center,151);
            pcCenter(pcShort(actorName(i),width+8),center,165);
        }
        pcText(offlineDungeon.active?"Huyệt Ma Kiến • Ải "+offlineDungeon.stage:onlineBattle()&&net.roomKind==3?(net.dungeonStage>=3?"Bộ Lạc Tà Thần • Ải "+DungeonRewards.tribalNumber(net.dungeonStage):"Huyệt Ma Kiến • Ải "+net.dungeonStage):game.unitCount == 6 ? "Tổ đội • 3 đấu 3"
                : game.teams ? "Phòng riêng • 2 đấu 2" : onlineBattle()&&net.roomKind==1?"Đấu đơn • 1 đấu 1":"Phòng riêng • 1 đấu 1", 8, 179);
        pcText("Chuẩn bị nhân vật và vũ khí", 8, 190);
        pcText(loadTicks<60?"Đang tải tài nguyên...":allLoadingReady()?"Tất cả đã OK. Chuẩn bị vào trận...":"Chờ người chơi khác tải xong...",8,201);
        if (roomMaps[map] == null) {
            Image original = combatRoomArt("room-map" + map);

            // Phong to kin chieu ngang, chua dai duoi cho ten map.
            int targetW = 94 * s;
            int targetH = 39 * s;

            if (original.getWidth() != targetW ||
                original.getHeight() != targetH) {
                roomMaps[map] = NativeUi.fit(
                    original, targetW, targetH, 0
                );
            } else {
                roomMaps[map] = original;
            }
        }

        // Phóng ảnh bản đồ vừa khung Đấu trường (tọa độ logic 320x240)
        img(roomMaps[map], 157, 189);

        // Ten map nam tren dai duoi khung, khong de len anh.
        pcCenter(pcShort(MAPS[map], 90), 204, 217);

    }

    final Image[] dungeonLoadActors=new Image[3], dungeonLoadCompact=new Image[3];
    final Image[] dungeonLoadingCovers = new Image[2];
    Image roomLayout, roomWaiting, roomClosed, roomCheckOn, roomCheckOff, roomRandom, botListFrame;
    final VipSystem vip=new VipSystem(this);
    final Image[] roomControl = new Image[3], botPortrait = new Image[8];
    final Image[] roomMaps = new Image[12];
    int roomSelection = 5, infoReturnMode, bagReturnMode, shopReturnMode;
    static final int[][] ROOM_BOX = { { 16, 29, 105, 103 }, { 125, 29, 105, 103 }, { 263, 30, 44, 43 },
            { 263, 80, 44, 43 }, { 141, 145, 117, 56 }, { 263, 143, 44, 43 }, { 16, 7, 104, 18 },
            { 284, 208, 24, 28 } };

    void roomMove(int dir) {
        int[] from = ROOM_BOX[roomSelection];
        int fx = from[0] * 2 + from[2], fy = from[1] * 2 + from[3], best = -1;
        long score = Long.MAX_VALUE;
        for (int i = 0; i < ROOM_BOX.length; i++) {
            if (i == roomSelection)
                continue;
            int[] r = ROOM_BOX[i];
            int dx = r[0] * 2 + r[2] - fx, dy = r[1] * 2 + r[3] - fy,
                    along = dir == 1 ? -dx : dir == 2 ? dx : dir == 4 ? -dy : dy,
                    across = dir == 1 || dir == 2 ? dy : dx;
            if (along <= 8)
                continue;
            long value = (long) along * along + 4L * across * across;
            if (value < score) {
                score = value;
                best = i;
            }
        }
        if (best >= 0)
            roomSelection = best;
    }

    int roomHit(int x, int y) {
        for (int i = 0; i < ROOM_BOX.length; i++) {
            int[] r = ROOM_BOX[i];
            if (x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3])
                return i;
        }
        return -1;
    }

    void roomActivate() {
        if (loadout.activate(false))
            return;
        if (roomSelection == 0) {
            presentation.settings = true;
        } else if (roomSelection == 1 || roomSelection == 6)
            toggleRoomSlot();
        else if (roomSelection == 2)
            inviteBot();
        else if (roomSelection == 3) {
            roomSelection = RoomLoadout.AVAILABLE;
        } else if (roomSelection == 4) {
            maps.open(false);
        } else if (roomSelection == 5) {
            if (!onlineAccount()) {
                presentation.toggleSearch();
                return;
            }
            if (onlineAccount()) {
                net.createRoom(allyMode > 0);
                return;
            }
            training = false;
            loadingRequested = true;
            resetPending = true;
        } else if (roomSelection == 7) {
            if (onlineAccount())
                net.open();
            else
                openLobby();
        }
    }

    Image lobbyBag, lobbyShop, lobbyFight, lobbyHallLabel;
    int lobbySelection = -1;
    final Image[] lobbyMenu = new Image[10];
    static final int[] LOBBY_X = { 64, 22, 163, 108, 300 };
    static final String[] LOBBY_LABELS = { "Túi", "Shop", "Sảnh", "Thiết lập", "Thoát" };
    Image fxBuff, fxLanding, fxPowRing, fxRadial, fxDamage, fxDamageBackground;

    Image combatEffect(String name) {
        try {
            return load("fx-" + name);
        } catch (IOException e) {
            return null;
        }
    }

    static final int POW_CUTIN_TIME = 150;
    int powCutinTicks, powCutinActor;
    boolean powCutinLeft, powShotPending;
    Image powCutin, powWorld;
    int[] powWorldPixels, powZoomPixels, powSampleX, powDistanceX, powShade;
    int powLightX=160,powLightY=120;int powFocusX, powFocusY, powZoomPhase = -1, powZoomDark = -1;

    boolean showingPowCutin() {
        return powCutinTicks > 0 && powCutin != null && game.active == powCutinActor && game.pow && !game.over;
    }

    void blendCutin(int[] pixels, Image layer) {
        blendCutin(pixels, layer, new int[pixels.length]);
    }

    void blendCutin(int[] pixels, Image layer, int[] data) {
        layer.getRGB(data, 0, 80 * s, 0, 0, 80 * s, 100 * s);
        for (int i = 0; i < data.length; i++) {
            int fg = data[i], a = fg >>> 24, bg = pixels[i], b = (bg >>> 24) * (255 - a) / 255, out = a + b;
            if (out == 0)
                continue;
            int r = (((fg >> 16) & 255) * a + ((bg >> 16) & 255) * b) / out,
                    green = (((fg >> 8) & 255) * a + ((bg >> 8) & 255) * b) / out,
                    blue = ((fg & 255) * a + (bg & 255) * b) / out;
            pixels[i] = (out << 24) | (r << 16) | (green << 8) | blue;
        }
    }

    void startPowCutin() {
        resetPowWorld();
        powCutinActor = game.active;
        powCutinLeft = game.units[powCutinActor].x - cameraX < 160;

        if (fxRadial == null)
            fxRadial = combatEffect("pow-radial");
        try {
            int[] pixels = new int[80 * 100 * s * s], blend = new int[pixels.length];
            if (onlineBattle()) {
                int sex = net.sex[powCutinActor], mask = net.wornMasks[powCutinActor];
                if ((mask & 2) != 0)
                    blendCutin(pixels, load("cutin" + sex + "_0_" + net.skin[powCutinActor]), blend);
                else {
                    int[] order = LOOK_ORDER;
                    for (int i = 0; i < 5; i++) {
                        int k = order[i];
                        String layer = layerName(sex, k, net.look[powCutinActor][k], mask, "cutin");
                        if (layer != null)
                            blendCutin(pixels, load(layer), blend);
                    }
                }
            } else if (powCutinActor == 0) {
                if ((wornMask & 2) != 0)
                    blendCutin(pixels, load("cutin" + gender + "_0_" + skin), blend);
                else {
                    int[] order = LOOK_ORDER;
                    for (int i = 0; i < 5; i++) {
                        int k = order[i];
                        String layer = layerName(gender, k, look[k], wornMask, "cutin");
                        if (layer != null)
                            blendCutin(pixels, load(layer), blend);
                    }
                }
            } else
                blendCutin(pixels, load("cutin" + actorSex[powCutinActor] + "_0_" + actorSkin[powCutinActor]), blend);
            powCutin = Image.createRGBImage(pixels, 80 * s, 100 * s, true);
            powCutinTicks = POW_CUTIN_TIME;
        } catch (IOException ignored) {
            powCutin = null;
            powCutinTicks = 0;
        }
    }

    int powCutinX() {
        int age = POW_CUTIN_TIME - powCutinTicks,
                offset = age < 12 ? (12 - age) * 10 : powCutinTicks < 12 ? (12 - powCutinTicks) * 10 : 0;
        return powCutinLeft ? 20 - offset : 220 + offset;
    }

    int turnNoticeX() {
        int age = Battle.clamp(55 - toastTicks, 0, 6), remaining = 6 - age;
        return 77 + 243 * remaining * remaining / 36;
    }

    void drawPowCutin() {
        if (!showingPowCutin())
            return;
        int age = POW_CUTIN_TIME - powCutinTicks;
        if (age < 30)
            return;
        int x = Battle.clamp(powLightX,72,248)-40, effectFrame = Math.min(19, (age - 30) * 20 / Math.max(1, POW_CUTIN_TIME - 42));
        vip.back(vip.actorExp(powCutinActor),-1,x+40,Battle.clamp(powLightY,72,168)+72,3,true);
        if (age >= 42 && powCutinTicks > 7)
            img(powCutin, x, Battle.clamp(powLightY,72,168)-50);
    }

    void resetPowWorld() {
        vip.releaseLargeEffects();
        powWorld = null;
        powWorldPixels = powZoomPixels = powSampleX = powDistanceX = powShade = null;
        powZoomPhase = powZoomDark = -1;
    }

    void drawPowWorldZoom(Graphics screen, int oldX, int oldY) {
        int width = 320 * s, height = 240 * s;
        if (powWorldPixels == null) {
            powWorldPixels = new int[width * height];
            powZoomPixels = new int[width * height];
            powSampleX = new int[width];
            powDistanceX = new int[width];
            powShade = new int[256];
            powWorld.getRGB(powWorldPixels, 0, width, 0, 0, width, height);
            powWorld = null;
            powFocusX = Battle.clamp((game.units[powCutinActor].x - cameraX) * s, 0, width - 1);
            powFocusY = Battle.clamp((game.units[powCutinActor].y - cameraY - 18) * s, 0, height - 1);
        }
        int age = POW_CUTIN_TIME - powCutinTicks, phase = Math.min(12, Math.min(age, Math.max(0, powCutinTicks - 1))),
                dark = Math.min(205, age * 205 / 21);
        if (powCutinTicks < 12)
            dark = dark * Math.max(0, powCutinTicks - 1) / 12;
        if (phase != powZoomPhase || dark != powZoomDark) {
            int zoom = 1024 + phase * 1024 / 12, focusX = powFocusX, focusY = powFocusY,
                    left = Battle.clamp(focusX - (powCutinLeft ? 100 : 220) * s * 1024 / zoom, 0,
                            width - width * 1024 / zoom),
                    top = Battle.clamp(focusY - 133 * s * 1024 / zoom, 0, height - height * 1024 / zoom),
                    base = 255 - dark;
            powLightX=(powFocusX-left)*zoom/1024/s;powLightY=(powFocusY-top)*zoom/1024/s;
            for (int x = 0; x < width; x++) {
                int sx = left + x * 1024 / zoom, dx = (sx - focusX) / s;
                powSampleX[x] = sx;
                powDistanceX[x] = dx * dx;
            }
            if (dark != powZoomDark)
                for (int i = 0; i < 256; i++)
                    powShade[i] = i * base / 255;
            for (int y = 0; y < height; y++) {
                int sy = top + y * 1024 / zoom, row = sy * width, dy = (sy - focusY) / s, dy2 = dy * dy,
                        out = y * width;
                for (int x = 0; x < width; x++) {
                    int distance = powDistanceX[x] + dy2, color = powWorldPixels[row + powSampleX[x]];
                    if (distance >= 1600) {
                        powZoomPixels[out + x] = 0xff000000 | (powShade[(color >> 16) & 255] << 16)
                                | (powShade[(color >> 8) & 255] << 8) | powShade[color & 255];
                    } else {
                        int factor = base + dark * (1600 - Math.max(400, distance)) / 1200;
                        powZoomPixels[out + x] = 0xff000000 | ((((color >> 16) & 255) * factor / 255) << 16)
                                | ((((color >> 8) & 255) * factor / 255) << 8) | ((color & 255) * factor / 255);
                    }
                }
            }
            powZoomPhase = phase;
            powZoomDark = dark;
        }
        g = screen;
        ox = oldX;
        oy = oldY;
        all();
        g.drawRGB(powZoomPixels, 0, width, ox, oy, width, height, false);
        if (age < 42)
            part(fxRadial, age % 4 * 320, 0, 320, 190, 0, 0, false);
    }

    Image iceDirections;
    byte[] hardTerrain;

    boolean diggable(int x, int y) {
        int i = y * 480 + x;
        return hardTerrain == null || (hardTerrain[i >> 3] & (1 << (i & 7))) == 0;
    }

    Image flightProjectile, hudPowerTicks;
    Image hudHealthFrame, hudHealthFill, hudEnergyFill, hudPowerFrame, hudPowerFill, hudAngle, hudTomb, hudPowRing,
            hudPowReady, hudPowLabel, hudPropSlot, hudPowerLast, hudPowAura, hudAngleJoin;
    Image hudWindTitle, hudWindLast, hudWindArrow, hudPass, hudCountDown, hudMapFrame, hudMapTitle, hudMapGrid,
            hudMapSettings, hudMapClose, hudPortraitHP, miniTerrain;
    boolean miniDirty = true;
    Image hudYourTurn, hudSelectGlow, hudResultWin, hudResultLose, hudResultTie, standingSuit;
    final Image[] standingLayers = new Image[5];
    final Image[] slopeBody = new Image[Battle.MAX_UNITS];
    final int[] slopeKeys = new int[Battle.MAX_UNITS];
    int[] slopePixels, slopeRotated, slopeLayer;
    Image dialFace;
    int dialSlope = 999;
    int[] dialPixels, dialRotated;
    Image aimSector;
    int aimSectorMin = -1, aimSectorMax = -1;
    int held, fireKey, pressTouch = -1, frame, shootSound, hitSound, oldWin;
    boolean started, twoPlayers, resetPending;
    String toast = "";
    int toastTicks, wins, buffFlash;
    Image background, font, title, fireImage, powImage, stickImage;
    final Image[] hero = new Image[2], portrait = new Image[2], thumbs = new Image[12], weapons = new Image[10];
    final Image[] tiles = new Image[8];
    final boolean[] dirty = new boolean[8];
    int coins = 800, owned = 1, equipped, shopSelection;
    final BagLayout bagLayout = new BagLayout(this);
    final NativeUi ui = new NativeUi(this);
    final InlineText typing = new InlineText(this);
    final ItemTooltip tooltip = new ItemTooltip(this);
    final GameNotices notices = new GameNotices(this);
    final NativePopup popup = new NativePopup(this);
    final ExchangePanel exchangePanel = new ExchangePanel(this);
    final GamePresentation presentation = new GamePresentation(this);
    final KeyBindings keyBindings = new KeyBindings(this);
    final BattleEmotes emotions = new BattleEmotes(this);
    int bagTab, bagItem, skin, cheatSelection, returnMode;
    boolean customLook;
    final int[] look = { 0, 0, 0, 0, 0 };
    final Image[] lookLayers = new Image[5], bagIcons = new Image[10];
    static final String[] BAG_TABS = { "Vũ khí", "Nguyên bộ", "Quần áo", "Mũ", "Tóc", "Mặt", "Kính" };
    int gender, createFocus, lastTurn;
    boolean hasProfile, training, tutorialDone;
    String playerName = "Gunner";
    Image creation;
    final Image[] chooseArt = new Image[2];

    Image onlineHover, onlineLabel;
    Image lobbyBackground, smallTitle;
    final Image[] carry = new Image[10], backCarry = new Image[10], bullets = new Image[10], trailArt = new Image[10],
            shopArt = new Image[10], propArt = new Image[6];
    final Image[] impactArt = new Image[10];
    static final int[] ACTIVE_WEAPONS = { 0, 3, 5, 9, 2, 4, 7, 1 };
    static final String[] WEAPON_NAMES = { "Lựu đạn", "Pháo", "Tủ thuốc", "Lu gạch", "Tủ lạnh", "Phi tiêu", "Cung",
            "Giỏ đồ", "VIP Thương Cổ", "Sấm sét" };
    static final String[] WEAPON_DESC = { "Lựu đạn: nổ và đào đất", "Pháo: ném ống pháo, nổ tập trung",
            "Tủ thuốc: ném viên thuốc, nhạy gió", "Lu gạch: ném viên gạch", "Tủ lạnh: đạn nặng, đào đất rộng",
            "Phi tiêu: đạn phi tiêu", "Cung: bắn mũi tên", "Giỏ đồ: ném cà chua xoay", "VIP Thương Cổ: phần thưởng Sulan",
            "Sấm sét: luồng điện" };
    static final int[] PRICES = { 0, 650, 950, 450, 550, 800, 1100, 700, 600, 1200 };
    int[] pixels, scratch;
    int terrainWidth, terrainHeight;
    char[] glyphs;
    byte[] widths;
    Graphics g;
    static final int PVP_MAP_COUNT = 6;
    static final String[] MAPS = { "Đu Quay", "Rừng Nhiệt Đới", "Lớp Học Ma Thuật", "Phố Nấm Lùn", "Mộc Diệp Trấn",
            "Đáy Biển", "Huyệt Ma Kiến • Ải 1", "Huyệt Ma Kiến • Nữ Vương", "Tà Thần 1", "Tà Thần 2 • Bulan", "Tà Thần 2 • Bulan", "Tà Thần 3 • Sulan" };
    static final String[] PROPS = { "+2 đợt bắn", "Đạn 3 tia", "Sát thương +50%", "Hồi 500 HP", "Bay đến điểm đáp",
            "POW đặc biệt" };
    static final String[] PROP_KEYS = { "Q", "W", "E", "R", "T", "B" };
    static final int[] HUD_PROPS = { 0, 1, 2, 3, 4 };

    PhoneCanvas(GunnyMIDlet app) throws IOException {
        this.app = app;
        game.deferBotPow = true;
        setFullScreenMode(true);
        DataInputStream input = new DataInputStream(getClass().getResourceAsStream("/screen.dat"));
        s = input.readUnsignedByte();
        input.close();
        font = load("font");
        fireImage = load("fire");
        powImage = load("pow");
        stickImage = load("stick");
        flightProjectile = load("flight-projectile");
        hudPowerTicks = load("hud-power-ticks");
        fxDamage = load("fx-damage-digits");
        fxDamageBackground = load("fx-damage-background");
        lobbyHallLabel = load("lobby-hall-label");
        lobbyBag = load("lobby-bag");
        lobbyShop = load("lobby-shop");
        lobbyFight = load("lobby-arena");
        for (int i = 0; i < 5; i++)
            for (int state = 0; state < 2; state++)
                lobbyMenu[i * 2 + state] = load("lobby-menu" + i + "-" + state);
        input = new DataInputStream(getClass().getResourceAsStream("/font.dat"));
        int count = input.readUnsignedShort();
        glyphs = new char[count];
        widths = new byte[count];
        for (int i = 0; i < count; i++) {
            glyphs[i] = input.readChar();
            widths[i] = input.readByte();
        }
        input.close();
        glyphLow=new int[512];glyphVietnamese=new int[128];
        for(int i=0;i<glyphLow.length;i++)glyphLow[i]=31;
        for(int i=0;i<glyphVietnamese.length;i++)glyphVietnamese[i]=31;
        for(int i=glyphs.length-1;i>=0;i--){int code=glyphs[i];if(code<512)glyphLow[code]=i;else if(code>=0x1e80&&code<0x1f00)glyphVietnamese[code-0x1e80]=i;}
        for (int i = 0; i < 2; i++)
            hero[i] = load("hero" + i);
        for (int i = 0; i < MAPS.length; i++)
            thumbs[i] = load("thumb" + i);
        for (int i = 0; i < 10; i++)
            if (enabledWeapon(i))
                weapons[i] = load("weapon" + i);
        for (int i = 0; i < 10; i++)
            if (enabledWeapon(i)) {
                bullets[i] = load("bullet" + i);
                trailArt[i] = load("trail" + i);
            }
        for (int i = 0; i < 6; i++)
            propArt[i] = load("prop" + i);
        hudHealthFrame = load("hud-health-frame");
        hudHealthFill = load("hud-health-fill");
        hudEnergyFill = load("hud-energy-fill");
        hudPowerFrame = load("hud-power-frame");
        hudPowerFill = load("hud-power-fill");
        hudAngle = load("hud-angle");
        hudTomb = load("hud-tomb");
        hudPowRing = load("hud-pow-ring");
        hudPowReady = load("hud-pow-ready");
        hudPowLabel = load("hud-pow-label");
        hudPropSlot = load("hud-prop-slot");
        hudPowerLast = load("hud-power-last");
        hudPowAura = load("hud-pow-aura");
        hudAngleJoin = load("hud-angle-join");
        hudWindTitle = load("hud-wind-title");
        hudWindLast = load("hud-wind-last");
        hudWindArrow = load("hud-wind-arrow");
        hudPass = load("hud-pass");
        hudCountDown = load("hud-countdown");
        hudMapFrame = load("hud-map-frame");
        hudMapTitle = load("hud-map-title");
        hudMapGrid = load("hud-map-grid");
        hudMapSettings = load("hud-map-settings");
        hudMapClose = load("hud-map-close");
        hudPortraitHP = load("hud-portrait-hp");
        hudYourTurn = load("hud-your-turn");
        hudSelectGlow = load("hud-select-glow");
        hudResultWin = load("hud-result-win");
        hudResultLose = load("hud-result-lose");
        hudResultTie = load("hud-result-tie");
        smallTitle = load("title_small");
        lobbyBackground = load("lobby");
        background = load("bg0");
        loadPortraits();
        loadSettings();
        loadExperience();
        loadProfile();
        loadBag();
        loadLook();
        if (!hasProfile) {
            mode = 7;
            loadCreation();
        }
        net = new OnlineClient(this);
        dungeon = new DungeonScreen(this);
        inventory.load();
        economy.load();
        loadLook();
    }

    Image load(String name) throws IOException {
        return Image.createImage("/" + name + ".png");
    }

    void loadPortraits() {
        try {
            for (int i = 0; i < 2; i++)
                if (portrait[i] == null)
                    portrait[i] = load("portrait" + i);
        } catch (IOException ignored) {
        }
    }

    void loadExperience() {
        try {
            RecordStore rs = RecordStore.openRecordStore("gunny-exp757", true);
            if (rs.getNumRecords() > 0) {
                DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(rs.getRecord(1)));
                experience = in.readInt();
                if (in.available() >= 4)
                    wins = in.readInt();
            }
            rs.closeRecordStore();
        } catch (Exception ignored) {
        }
    }

    void saveExperience() {
        if(offlineTest)return;
        if (onlineAccount())
            return;
        try {
            RecordStore rs = RecordStore.openRecordStore("gunny-exp757", true);
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(b);
            out.writeInt(experience);
            out.writeInt(wins);
            byte[] data = b.toByteArray();
            if (rs.getNumRecords() == 0)
                rs.addRecord(data, 0, data.length);
            else
                rs.setRecord(1, data, 0, data.length);
            rs.closeRecordStore();
        } catch (Exception ignored) {
        }
    }

    int actorExp(int i) {
        return onlineBattle() ? net.actorExperience[i] : game.units[i].experience;
    }

    final Image[] levelIcons = new Image[8];
    final int[] levelIds = new int[8];
    int levelCache;

    Image levelIcon(int exp) {
        int lv = Progression.level(exp);
        for (int i = 0; i < 8; i++)
            if (levelIds[i] == lv)
                return levelIcons[i];
        int slot = levelCache++ % 8;
        levelIcons[slot] = combatRoomArt("level758-" + lv);
        levelIds[slot] = lv;
        return levelIcons[slot];
    }

    void drawLevel(int exp, int x, int y) {
        img(levelIcon(exp), x, y);
    }

    String expProgress() {
        return Progression.level(experience) == 70 ? "MAX"
                : Progression.within(experience) + "/" + Progression.limit(experience);
    }

    void loadSettings() {
        try {
            RecordStore rs = RecordStore.openRecordStore("gunny-v2", true);
            if (rs.getNumRecords() > 0) {
                byte[] b = rs.getRecord(1);
                wins = ((b[2] & 255) << 8) | (b[3] & 255);
                if (b.length >= 10) {
                    coins = ((b[4] & 255) << 24) | ((b[5] & 255) << 16) | ((b[6] & 255) << 8) | (b[7] & 255);
                    owned = (b[8] & 255) | 1;
                    equipped = b[9] & 15;
                    if (equipped > 9 || (owned & (1 << equipped)) == 0)
                        equipped = 0;
                }
            }
            rs.closeRecordStore();
        } catch (Exception ignored) {
        }
        loadBag();
    }

    void saveSettings() {
        if(offlineTest)return;
        if (onlineAccount())
            return;
        saveBag();
        try {
            RecordStore rs = RecordStore.openRecordStore("gunny-v2", true);
            byte[] b = { (byte) 0, (byte) 0, (byte) (wins >> 8), (byte) wins, (byte) (coins >> 24),
                    (byte) (coins >> 16), (byte) (coins >> 8), (byte) coins, (byte) owned, (byte) equipped };
            if (rs.getNumRecords() == 0)
                rs.addRecord(b, 0, b.length);
            else
                rs.setRecord(1, b, 0, b.length);
            rs.closeRecordStore();
        } catch (Exception ignored) {
        }
    }

    static boolean enabledWeapon(int id) {
        return ShopCatalog.weapon(id);
    }

    static int nextWeapon(int id, int direction) {
        for (int i = 0; i < ACTIVE_WEAPONS.length; i++)
            if (ACTIVE_WEAPONS[i] == id)
                return ACTIVE_WEAPONS[(i + direction + ACTIVE_WEAPONS.length) % ACTIVE_WEAPONS.length];
        return 0;
    }

    void loadBag() {
        try {
            RecordStore rs = RecordStore.openRecordStore("gunny-bag", true);
            if (rs.getNumRecords() > 0) {
                DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(rs.getRecord(1)));
                owned = in.readUnsignedShort() | 1;
                equipped = in.readUnsignedByte();
                skin = in.readUnsignedByte();
                customLook = in.readBoolean();
                for (int i = 0; i < 5; i++)
                    look[i] = in.readUnsignedByte();
                in.close();
            }
            rs.closeRecordStore();
        } catch (Exception ignored) {
        }
        if (!enabledWeapon(equipped))
            equipped = 0;
    }

    void saveBag() {
        if (onlineAccount()) {
            net.request("appearance");
            return;
        }
        try {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(bytes);
            out.writeShort(owned);
            out.writeByte(equipped);
            out.writeByte(skin);
            out.writeBoolean(customLook);
            for (int i = 0; i < 5; i++)
                out.writeByte(look[i]);
            out.close();
            RecordStore rs = RecordStore.openRecordStore("gunny-bag", true);
            byte[] data = bytes.toByteArray();
            if (rs.getNumRecords() == 0)
                rs.addRecord(data, 0, data.length);
            else
                rs.setRecord(1, data, 0, data.length);
            rs.closeRecordStore();
        } catch (Exception ignored) {
        }
    }

    void loadLook() {
        roomSelf = roomMate = null;
        try {
            if (hero[1 - gender] == null)
                hero[1 - gender] = load("hero" + (1 - gender));
            for (int i = 0; i < 5; i++) {
                lookLayers[i] = null;
                standingLayers[i] = null;
            }
            standingSuit = null;
            hero[gender] = null;
            System.gc();
            if (customLook) {
                for (int i = 0; i < 5; i++) {
                    String name = layerName(gender, i, look[i], wornMask, "look"),
                            stand = layerName(gender, i, look[i], wornMask, "stand");
                    if (name != null)
                        lookLayers[i] = load(name);
                    if (stand != null)
                        standingLayers[i] = load(stand);
                }
            } else {
                hero[gender] = load("look" + gender + "_0_" + skin);
                standingSuit = load("stand" + gender + "_0_" + skin);
            }
        } catch (IOException e) {
            message("Không đọc được trang phục");
        }
    }

    void appearance(int sex, int anim, int x, int y, boolean flip) {
        if (sex == gender && customLook) {
            int[] order = LOOK_ORDER;
            for (int i = 0; i < 5; i++)
                part(lookLayers[order[i]],
                        (s == 1 ? (anim >= 16 ? 8 + anim - 16
                                : anim == 0 ? 0 : anim < 10 ? 1 + (anim - 1) % 5 : 6 + anim % 2) : anim) * 32,
                        0, 32, 40, x, y, flip);
        } else
            part(hero[sex], anim * 32, 0, 32, 40, x, y, flip);
    }

    void standingAppearance(int x, int y) {
        if (customLook) {
            int[] order = LOOK_ORDER;
            for (int i = 0; i < 5; i++)
                img(standingLayers[order[i]], x, y);
        } else
            img(standingSuit, x, y);
    }

    Image heldArt(int id) {
        try {
            if (carry[id] == null)
                carry[id] = load("carry" + id);
        } catch (IOException ignored) {
        }
        return carry[id];
    }

    Image impactArt(int weapon) {
        try {
            if (impactArt[weapon] == null)
                impactArt[weapon] = load("impact" + weapon);
        } catch (IOException ignored) {
        }
        return impactArt[weapon];
    }

    Image backArt(int id) {
        try {
            if (backCarry[id] == null)
                backCarry[id] = load("back" + id);
        } catch (IOException ignored) {
        }
        return backCarry[id];
    }

    void openBag() {
        bagReturnMode = mode == 16 || mode == 10 || mode == 19 ? mode : 0;
        mode = 8;
        held = 0;
        fireKey = 0;
        game.charging = false;
        bagItem = 0;
        bag.open();
    }

    void refreshBag() {
        if (bagTab == 0 && !enabledWeapon(bagItem))
            bagItem = 0;
        try {
            for (int i = 0; i < 10; i++) {
                bagIcons[i] = null;
                bagIcons[i] = bagTab == 0 ? weapons[i] : load("item" + gender + "_" + (bagTab - 1) + "_" + i);
            }
        } catch (IOException ignored) {
            message("Thiếu ảnh vật phẩm");
        }
    }

    void equipBag() {
        bag.change(bagTab, bagItem, false);
    }

    void setEquipment(int kind, int item, boolean remove) {
        wornMask = ItemRules.maskAfter(wornMask, kind, remove);
        if (!remove) {
            if (kind == 0)
                equipped = item;
            else if (kind == 1)
                skin = item;
            else
                look[kind - 2] = item;
        }
        customLook = (wornMask & 2) == 0;
        loadLook();
        inventory.save();
        saveBag();
        saveSettings();
    }

    Image walletCoin, walletMoney;

    void wallet(int x, int y) {
        if (walletCoin == null) {
            walletCoin = combatRoomArt("wallet-coin");
            walletMoney = combatRoomArt("wallet-money");
        }
        img(walletCoin, x, y);
        pcText("" + coins, x + 14, y + 1);
        img(walletMoney, x + 103, y);
        pcText("" + shoppingMoney, x + 117, y + 1);
    }

    void drawBag() {
        bag.draw();
    }

    void openCheats() {
        if(offlineTest){message("Test Offline: không dùng cheat");return;}
        if (onlineAccount()) {
            if(!economy.testCheats){message("Admin chưa bật cheat cho tài khoản này");return;}
        }
        returnMode = mode;
        mode = 9;
        held = 0;
        fireKey = 0;
        game.charging = false;
        cheatSelection = 0;
    }

    void cheat() {
        if (onlineAccount()){net.request("testcheat");return;}
        if (cheatSelection == 4) {
            if (cheatOptions() == 5) {
                mode = returnMode;
                offlineDungeon.skipStage();
            }
            return;
        }
        if (cheatSelection == 0)
            coins = Math.min(999999, coins + 100000);
        else if (cheatSelection == 1) {
            owned |= 703;
            shoppingMoney = 999999;
            for (int i = 0; i < 12; i++)
                inventory.offline[i] = 1023;
            inventory.save();
        } else if (cheatSelection == 3) {
            experience = Math.max(experience, Progression.experienceForLevel(Progression.level(experience) + 1));
            saveExperience();
        } else
            game.cheatAim = !game.cheatAim;
        saveSettings();
        message("Đã áp dụng cheat test");
    }

    int cheatOptions() {
        if(onlineAccount())return 7;
        return returnMode == 1 && offlineDungeon.active && !game.over ? 5 : 4;
    }

    void drawCheats() {
        if(onlineAccount()){panel(30,32,260,198);center("CHEAT TÀI KHOẢN TEST",160,42);String[] names={"FULL ĐÁ LV1-5 + BÙA","FULL CƯỜNG HÓA +15","FULL CẤP NHÂN VẬT","FULL TIỀN MUA SẮM","BẤT TỬ: "+(economy.testGod?"BẬT":"TẮT"),"QUA ẢI PHÓ BẢN","+999 THẺ VIP / EXP / FULL"};for(int i=0;i<7;i++)button(names[i],40,59+i*23,240,21,cheatSelection==i);return;}
        boolean canSkip = cheatOptions() == 5;
        panel(30, canSkip ? 22 : 42, 260, canSkip ? 214 : 189);
        center("CHEAT • TEST OFFLINE", 160, 50);
        button("+100.000 XU", 50, 78, 220, 25, cheatSelection == 0);
        button("MỞ ĐỒ + TIỀN MUA SẮM", 50, 108, 220, 25, cheatSelection == 1);
        button("ĐẠN TỰ DẪN: " + (game.cheatAim ? "BẬT" : "TẮT"), 50, 138, 220, 25, cheatSelection == 2);
        button("+1 LEVEL", 50, 168, 220, 25, cheatSelection == 3);
        if (canSkip)
            button("QUA ẢI", 50, 198, 220, 25, cheatSelection == 4);
        center("OK: áp dụng • Phím phải: đóng", 160, canSkip ? 225 : 209);
    }

    void loadProfile() {
        try {
            RecordStore rs = RecordStore.openRecordStore("gunny-profile", true);
            if (rs.getNumRecords() > 0) {
                DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(rs.getRecord(1)));
                gender = in.readUnsignedByte();
                playerName = in.readUTF();
                tutorialDone = in.readBoolean();
                hasProfile = true;
                in.close();
            }
            rs.closeRecordStore();
        } catch (Exception ignored) {
        }
    }

    void saveProfile() {
        if(offlineTest)return;
        if (onlineAccount()) {
            net.request("appearance");
            return;
        }
        try {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(bytes);
            out.writeByte(gender);
            out.writeUTF(playerName);
            out.writeBoolean(tutorialDone);
            out.close();
            RecordStore rs = RecordStore.openRecordStore("gunny-profile", true);
            byte[] data = bytes.toByteArray();
            if (rs.getNumRecords() == 0)
                rs.addRecord(data, 0, data.length);
            else
                rs.setRecord(1, data, 0, data.length);
            rs.closeRecordStore();
            hasProfile = true;
        } catch (Exception ignored) {
        }
    }

    void loadCreation() {
        try {
            creation = load("creation");
            chooseArt[0] = load("choose0");
            chooseArt[1] = load("choose1");
        } catch (IOException ignored) {
        }
    }

    void editName() {
        if (onlineAccount() && net.choosingCharacter) return;
        typing.field("Tên nhân vật", playerName, 13, false, false, 105, 158, 110, new InlineText.Done() {
            public void accept(String text) {
                String value = text.trim();
                if (value.length() == 0) return;
                playerName = value;
                if (onlineAccount() && !net.choosingCharacter && !net.busy && net.pending.length() == 0)
                    net.request("appearance");
                createFocus = 2;
            }
        });
    }

    void enterCharacter() {
        if (onlineAccount()) {
            if (net.busy || net.pending.length() > 0)
                return;
            net.request("appearance");
            return;
        }
        if (playerName.trim().length() == 0) {
            message("Hãy nhập tên nhân vật");
            return;
        }
        saveProfile();
        loadLook();
        creation = null;
        chooseArt[0] = chooseArt[1] = null;
        training = !tutorialDone;
        if (training) {
            selectedMap = 1;
            twoPlayers = false;
            resetPending = true;
        } else
            openLobby();
    }

    void drawCreation() {
        all();
        img(creation, 0, 0);
        panel(69, 8, 182, 24);
        center("CHỌN NHÂN VẬT", 160, 13);
        if (gender == 0) {
            img(chooseArt[0], 0, 51);
            img(portrait[1], 241, 86);
        } else {
            img(portrait[0], 0, 86);
            img(chooseArt[1], 214, 51);
        }
        button("NAM", 113, 102, 44, 25, gender == 0);
        button("NỮ", 165, 102, 44, 25, gender == 1);
        text("Tên:", 111, 140);
        if (onlineAccount())
            center(playerName, 160, 155);
        else {
            button(playerName, 101, 155, 118, 23, createFocus == 1);
            center("N: nhập tên • Tối đa 13 ký tự", 160, 181);
        }
        button("VÀO GAME", 102, 202, 116, 28, createFocus == 2);
        if (createFocus == 0)
            center("Trái/phải: chọn • Xuống: tiếp", 160, 36);
        if (!onlineAccount())
            button("Online (O)", 235, 211, 81, 22, false);
    }

    synchronized void start() {
        running = true;
        if (loop == null) {
            loop = new Thread(this);
            loop.start();
        }
    }

    synchronized void suspend() {
        presentation.silence();
        running = false;
        cameraHeld = held = 0;
        if (onlineBattle()) {
            if (fireKey != 0 && game.charging)
                net.command(2, 0);
            if (mode == 2)
                mode = 1;
        } else if (mode == 1 && !popup.opened)
            popup.pause();
        fireKey = 0;
        game.charging = false;
    }

    void stop() {
        presentation.silence();
        if (net != null)
            net.stop();
        alive = running = false;
        saveSettings();
    }

    protected void hideNotify() {
        suspend();
    }

    protected synchronized void showNotify() {
        if (mode == 2)
            mode = 1;
        running = true;
    }

    // Slow presentation time, never the collision-checked physics substeps.
    boolean projectilePacing() {
        return mode==1&&!game.over&&BattlePlaybackPacer.projectile(game.ballCount,game.bursts)
            &&(onlineBattle()||!popup.pausedOffline);
    }

    public void run() {
        BattlePlaybackPacer pacer=new BattlePlaybackPacer();
        while (alive) {
            long began = System.currentTimeMillis();
            int tickPeriod=BattlePlaybackPacer.NORMAL_MS;
            if (running) {
                synchronized (this) {
                    frame++;
                    presentation.tick();
                    forge.tick();
                    if(mode==5&&!offlineTest&&net!=null)net.results.tick();
                    tickLoading();
                    if (muzzleTicks > 0)
                        muzzleTicks--;
                    if (resetPending) {
                        resetPending = false;
                        try {
                            beginMatch();
                            if (loadingRequested) {
                                loadingRequested = false;
                                loadTicks = 0;
                                mode = 14;
                            }
                        } catch (Throwable e) {
                            message("Không đủ bộ nhớ. Hãy khởi động lại game.");
                            mode = 0;
                            if(offlineTest){releaseBattleArt();finishOfflineTest();}
                        }
                    }
                    if(mode==1&&!vip.battleReady)prepareBattleAssets();
                    if(presentation.showFps&&mode==1&&!popup.pausedOffline)frameGap.sample(began,projectilePacing());
                    else frameGap.idle();
                    if(mode!=1||!onlineBattle())pacer.reset();
                    if (mode == 1 && onlineBattle()) {
                        int steps=pacer.steps(System.currentTimeMillis(),projectilePacing());
                        for(int step=0;step<steps&&mode==1&&onlineBattle();step++){
                            net.animate();
                            // A catch-up tick may launch or reveal a projectile. Never
                            // consume another frame in the same render after that.
                            if(projectilePacing()){pacer.reset();break;}
                        }
                        followCamera();
                        if (game.shotSerial != shootSound) {
                            shootSound = game.shotSerial;
                        }
                        if (game.hitSerial != hitSound) {
                            hitSound = game.hitSerial;
                        }
                    } else if (mode == 1 && !popup.pausedOffline && powShotPending) {
                        tickPowSequence();
                    } else if (mode == 1 && !popup.pausedOffline) {
                        if (game.input()) {
                            if ((held & 1) != 0)
                                game.move(-1);
                            if ((held & 2) != 0)
                                game.move(1);
                            if ((held & 4) != 0)
                                game.adjustAngle(1);
                            if ((held & 8) != 0)
                                game.adjustAngle(-1);
                        }
                        game.tick();
                        if(offlineDungeon.active)offlineDungeon.progress();
                        if (training && !game.over && game.active == 1 && !game.busy())
                            game.next();
                        followCamera();
                        syncBotPow();
                        syncTurnNotice();
                        if (game.shotSerial != shootSound) {
                            shootSound = game.shotSerial;
                            muzzleTicks = 5;
                        }
                        if (game.hitSerial != hitSound) {
                            hitSound = game.hitSerial;
                        }
                        if (game.over&&mode==1) {
                            mode = 5;
                            held = 0;
                            if(offlineTest){ /* diagnostic match: no rewards or profile writes */ }
                            else if(offlineDungeon.active){offlineDungeon.finish();}else if (training) {
                                if (game.winner == 0) {
                                    tutorialDone = true;
                                    saveProfile();
                                }
                            } else {
                                if (game.winner == 0)
                                    wins++;
                                coins = Math.min(999999, coins + (game.winner == 0 ? (game.teams ? 1000 : 500)
                                        : game.winner < 2 && game.damage[0] > 0 ? (game.teams ? 500 : 250) : 0));
                                experience = (int) Math.min(2147483647L, experience + (game.winner == 0 ? 300L : 100L));
                                saveExperience();
                                saveSettings();
                            }
                        }
                    }
                    if (toastTicks > 0)
                        toastTicks--;
                    if (buffFlash > 0)
                        buffFlash--;
                    tickPeriod=BattlePlaybackPacer.period(projectilePacing());
                }
                if(presentation.showFps)updateMs=(updateMs*3+(int)Math.min(1000L,System.currentTimeMillis()-began)+2)/4;
                repaint();
                serviceRepaints();
            }
            if(!running)pacer.reset();
            long wait = tickPeriod - (System.currentTimeMillis() - began);
            try {
                Thread.sleep(wait > 2 ? wait : 2);
            } catch (InterruptedException ignored) {
            }
        }
    }

    boolean requireWeapon() {
        if ((wornMask & 1) == 0 || (owned & (1 << equipped)) == 0) {
            popup.open("Gợi ý", "Không có trang bị vũ khí không thể bắt đầu", new String[] { "Đồng ý", "Hủy" }, 0);
            return true;
        }
        return false;
    }

    void beginMatch() throws IOException {
        beginMatch(training || !randomMap ? pvpMap() : game.rand(PVP_MAP_COUNT));
    }

    int pvpMap() {
        return selectedMap >= 0 && selectedMap < PVP_MAP_COUNT ? selectedMap : 0;
    }

    void releaseNpcArt(){
        queenCall=queenBow=queenDeath=queenArrow=queenIdle=queenShade=queenHit=queenTitle=speechFont=null;
        for(int i=0;i<2;i++){antIdle[i]=antWalk[i]=antBeat[i]=null;for(int j=0;j<antPoses[i].length;j++)antPoses[i][j]=null;}
        for(int i=0;i<queenArrowRotations.length;i++)queenArrowRotations[i]=null;
        for(int i=0;i<npc780.length;i++)npc780[i]=npcHud780[i]=npcHeader780[i]=null;
        bossFrame=bossHp=bossPortrait=null;
        for(int i=0;i<antWasDead.length;i++){antWasDead[i]=false;antDeathFrame[i]=antLastWalk[i]=antLastMoveFrame[i]=0;}
        tribalArt.release();conditions.reset();
    }
    // Local JAR resources only. Called while Loading, never from paint.
    void prepareBattleAssets(){
        if(vip.battleReady)return;
        System.gc();
        // Current weapons, common item effects, HUD and projectiles already in base cache.
        int mask=0;
        for(int i=0;i<game.unitCount;i++){
            Battle.Unit u=game.units[i];int weapon=u.weapon;
            if(onlineBattle()&&i<net.count&&net.present[i]&&u.npc==0)weapon=net.weapon[i];
            if(weapon>=0&&weapon<10)mask|=1<<weapon;
            if(i<6&&u.npc==0)try{levelIcon(actorExp(i));}catch(OutOfMemoryError low){}
        }
        if((mask&(1<<8))!=0)try{tribalArt.preloadPlayerSpear();}catch(OutOfMemoryError low){ /* keep original projectile fallback */ }
        // Reserve Sulan NPC artwork before optional wing angles consume the heap.
        if(game.dungeonStage==6)tribalArt.preloadSulan();
        vip.prepareBattle();
        if(game.dungeonStage>=3)mask|=(1<<0)|(1<<5);
        for(int w=0;w<10;w++)if((mask&(1<<w))!=0){
            if(Runtime.getRuntime().freeMemory()<(s==1?1024:3072)*1024)break;
            try{heldArt(w);backArt(w);impactArt(w);}catch(OutOfMemoryError low){break;}
        }
        try{
            if(Runtime.getRuntime().freeMemory()<(s==1?1024:3072)*1024)return;
            if(fxLanding==null)fxLanding=combatEffect("flight-landing");
            if(fxBuff==null)fxBuff=combatEffect("buff-flash");
            if(iceDirections==null)iceDirections=combatRoomArt("ice762-directions");
            for(int i=0;i<6;i++){loadout.icon(i);loadout.grayIcon(i);}
            for(int i=0;i<grayProps.length;i++)grayProp(i);
            if(loadout.frost==null)loadout.frost=loadout.effect("frost");
            if(loadout.freezeImpact==null)loadout.freezeImpact=loadout.effect("freeze-impact");
            loadout.ice();
            if(game.dungeonStage>0&&game.dungeonStage<3){
                if(speechFont==null)speechFont=combatRoomArt("font-dungeon-speech");
                for(int type=1;type<=2;type++){
                    npc780[type]=combatRoomArt("npc780-"+type);
                    antIdle[type-1]=combatRoomArt("npc780-idle-"+type);
                    antWalk[type-1]=combatRoomArt("npc780-walk-"+type);
                    antBeat[type-1]=combatRoomArt("npc780-beat-"+type);
                }
                if(game.dungeonStage==2){
                    npc780[3]=combatRoomArt("npc780-3");queenArrow=combatRoomArt("npc780-queen-arrow");
                    queenIdle=combatRoomArt("npc780-queen-idle");queenBow=combatRoomArt("npc780-queen-bow");
                    queenCall=combatRoomArt("npc780-queen-call");queenHit=combatRoomArt("npc780-queen-hit");
                }
            }
            if(game.dungeonStage>=3)tribalArt.preloadBattle();
        }catch(OutOfMemoryError low){ /* optional art may retain its original lazy fallback */ }
    }
    void releaseLoadingArt(){
        versusBg=versusLogo=versusOK=versusMode=versusTime=versusDigits=null;
        for(int i=0;i<loadingHeroes.length;i++){loadingHeroes[i]=versusPortrait[i]=null;loadingWidths[i]=0;}
        for(int i=0;i<dungeonLoadingCovers.length;i++)dungeonLoadingCovers[i]=null;
        vip.releaseLoadingArt();
    }
    // Only call when leaving the encounter, never when opening a pause/menu overlay.
    void releaseBattleArt(){
        releaseNpcArt();releaseLoadingArt();vip.release();
        background=miniTerrain=opponentStanding=powCutin=aimSector=dialFace=fadeBuffer=null;
        pixels=scratch=slopePixels=slopeRotated=slopeLayer=fadePixels=dialPixels=dialRotated=miniSample=null;
        onlineGround=hardTerrain=null;
        powShotPending=false;powCutinTicks=0;resetPowWorld();
        aimSectorMin=aimSectorMax=-1;miniDirty=true;
        for(int i=0;i<tiles.length;i++){tiles[i]=null;dirty[i]=true;}
        for(int i=0;i<fadedUnits.length;i++){fadedUnits[i]=null;slopeBody[i]=null;}
        for(int i=0;i<actors.length;i++)actors[i]=actorStand[i]=null;
        for(int i=0;i<impactArt.length;i++)impactArt[i]=null;
        if(net!=null){for(int i=0;i<net.layers.length;i++)for(int j=0;j<5;j++)net.layers[i][j]=null;}
    }
    void beginMatch(int battleMap) throws IOException {
        releaseBattleArt();loadout.releaseMenuArt();gifts.releaseArt();home.releaseArt();ranking.releaseArt();bag.releaseArt();shop.releaseArt();forge.releaseArt();tooltip.releaseArt();popup.releaseArt();ui.releaseArt();typing.releaseArt();
        if(dungeon!=null)dungeon.releaseArt();if(net!=null){net.waiting.releaseArt();net.rooms.releaseArt();for(int i=0;i<net.standing.length;i++)net.standing[i]=null;}
        for(int i=0;i<stoneShop.icons.length;i++)stoneShop.icons[i]=null;

        presentation.offlineSearch = 0;
        emotions.reset();
        if (!onlineAccount() && requireWeapon()) {
            mode = 10;
            loadingRequested = false;
            return;
        }

        freeCamera = cameraReturning = cameraOnPlayer = false;
        cameraHeld = 0;
        onlineGround = null;opponentStanding=null;
        if (net != null)
            net.results.reset();
        powShotPending = false;
        powCutinTicks = 0;
        powCutin = null;
        resetPowWorld();
        buffFlash = 0;
        for (int i = 0; i < 10; i++)
            impactArt[i] = null;
        for (int i = 0; i < 6; i++) {
            slopeBody[i] = null;
            actors[i] = actorStand[i] = versusPortrait[i] = null;loadingHeroes[i]=null;loadingWidths[i]=0;
        }
        miniTerrain = null;
        miniDirty = true;
        held = 0;
        creation = null;
        chooseArt[0] = chooseArt[1] = null;
        smallTitle = null;
        lobbyBackground = null;
        for (int i = 0; i < 10; i++) {
            shopArt[i] = null;
            bagIcons[i] = null;
            carry[i] = null;
            backCarry[i] = null;
        }
        portrait[0] = portrait[1] = null;
        for (int i = 0; i < 8; i++)
            tiles[i] = null;
        pixels = scratch = null;
        background = null;
        System.gc();
        aimSector = null;
        aimSectorMin = aimSectorMax = -1;
        map = battleMap;
        background = load("bg" + map);
        Image terrain = load("land" + map);
        hardTerrain = new byte[15840];
        DataInputStream hardIn = new DataInputStream(getClass().getResourceAsStream("/hard" + map + ".dat"));
        hardIn.readFully(hardTerrain);
        hardIn.close();
        terrainWidth = 480 * s;
        terrainHeight = 264 * s;
        pixels = new int[terrainWidth * terrainHeight];
        terrain.getRGB(pixels, 0, terrainWidth, 0, 0, terrainWidth, terrainHeight);
        terrain = null;
        System.gc();
        scratch = new int[60 * s * terrainHeight];
        for (int i = 0; i < 8; i++)
            dirty[i] = true;
        rebuildTiles();
        game.windLimit = Progression.windLimit(experience);
        game.start(twoPlayers);
        game.viewX = -25;
        game.viewY = -10000;
        game.viewWidth = 530;
        game.viewHeight = 10255;
        game.units[0].weapon = equipped;
        game.units[1].weapon = nextWeapon(equipped, 1);
        if (training) {
            game.units[0].x = 365;
            game.units[0].y = surface(365, 35);
            game.units[0].face = -1;
            game.units[1].x = 125;
            game.units[1].y = surface(125, 35);
            game.units[1].hp = 650;
            game.countdown = 450;
            game.refreshAim(game.units[0]);
            int best = 999999, bestAngle = 45;
            for (int a = Battle.minLocalAngle(game.units[0]); a <= Battle.maxLocalAngle(game.units[0]); a++) {
                int score = game.error(a, 55);
                if (score < best) {
                    best = score;
                    bestAngle = a;
                }
            }
            game.units[0].angle = bestAngle;
        }
        if (!training && allyMode > 0 && chosenAllyBot >= 0) {
            game.configureTeam(allyMode);
            game.units[0].weapon = equipped;
            for (int i = 1; i < 4; i++) {
                game.units[i].weapon = nextWeapon(equipped, i);
                actorSex[i] = game.rand(2);
                actorSkin[i] = (game.units[i].botStyle * 2 + game.turn) % 10;
                if (i == 2) {
                    actorSex[i] = botSex(chosenAllyBot);
                    actorSkin[i] = botSkin(chosenAllyBot);
                    game.units[i].botStyle = chosenAllyBot % 4;
                } else {
                    while (actorSkin[i] == skin || actorSkin[i] == botSkin(chosenAllyBot)
                            || i == 3 && actorSkin[i] == actorSkin[1])
                        actorSkin[i] = (actorSkin[i] + 1) % 10;
                }
                actors[i] = load("look" + actorSex[i] + "_0_" + actorSkin[i]);
                actorStand[i] = load("stand" + actorSex[i] + "_0_" + actorSkin[i]);
            }
            game.units[2].weapon = botWeapon(chosenAllyBot);
            for (int i = 1; i < 4; i += 2) {
                int w = nextWeapon(equipped, 1);
                while (w == game.units[2].weapon || i == 3 && w == game.units[1].weapon)
                    w = nextWeapon(w, 1);
                game.units[i].weapon = w;
            }
        } else if (!training && !onlineAccount()) {
            game.units[1].botStyle = game.rand(4);
            game.units[1].weapon = nextWeapon(equipped, 1 + game.rand(3));
            actorSex[1] = game.rand(2);
            actorSkin[1] = game.rand(10);
            actors[1] = load("look" + actorSex[1] + "_0_" + actorSkin[1]);
            actorStand[1] = load("stand" + actorSex[1] + "_0_" + actorSkin[1]);
            opponentStanding = actorStand[1];
            opponentStandingGender = 1 - gender;
        }
        if (!training) {
            game.turnLimit = turnSeconds * 30;
            game.countdown = game.turnLimit;
        }
        loadout.apply();
        if (!onlineAccount())
            for (int i = 0; i < game.unitCount; i++) {
                game.units[i].experience = i == 0 ? experience : Progression.botExperience(i == 2 && chosenAllyBot >= 0 ? chosenAllyBot : actorSex[i] * 4 + Math.max(0, game.units[i].botStyle));
                if(i==0)ForgeRules.apply(game.units[i],economy.bonuses(),economy.ranks());
                ItemRules.apply(game.units[i], i == 0 ? wornMask : 3);
                if (training && i > 0)
                    game.units[i].hp = 650;
            }
        lastTurn = game.turn;
        cameraActor = game.active;
        cameraX = training ? 85 : 65;
        cameraY = training ? 0 : 20;
        mode = 1;
        started = true;
        shootSound = hitSound = 0;
        toast = "Đến lượt bạn!";
        toastTicks = 55;
    }

    void openLobby() {
        releaseBattleArt();
        if(net!=null&&!net.inBattle)for(int i=0;i<net.layers.length;i++)for(int j=0;j<5;j++)net.layers[i][j]=null;
        offlineDungeon.active=false;forge.running=false;
        toastTicks = 0;
        resetPending = loadingRequested = false;
        presentation.offlineSearch = 0;
        resetPowWorld();
        freeCamera = cameraReturning = cameraOnPlayer = false;
        cameraHeld = 0;
        onlineGround = null;
        lobbySelection = -1;
        for (int i = 0; i < 10; i++)
            impactArt[i] = null;
        for (int i = 0; i < 6; i++) {
            slopeBody[i] = null;
            actors[i] = actorStand[i] = versusPortrait[i] = null;loadingHeroes[i]=null;loadingWidths[i]=0;
        }
        slopePixels = slopeRotated = slopeLayer = null;
        miniTerrain = null;
        miniDirty = true;
        training = false;
        pixels = scratch = null;
        for (int i = 0; i < 8; i++)
            tiles[i] = null;
        System.gc();
        held = 0;
        fireKey = 0;
        game.charging = false;
        mode = 0;
        menuSelection = 0;
        if(offlineTest){finishOfflineTest();return;}
        loadPortraits();
        try {
            if (smallTitle == null)
                smallTitle = load("title_small");
            if (lobbyBackground == null)
                lobbyBackground = load("lobby");
        } catch (IOException ignored) {
        }
    }

    void followCamera() {
        if (cameraActor != game.active) {
            cameraActor = game.active;
            freeCamera = cameraReturning = cameraOnPlayer = false;
            cameraHeld = 0;
        }
        if (freeCamera) {
            if ((cameraHeld & 1) != 0)
                cameraX -= 4;
            if ((cameraHeld & 2) != 0)
                cameraX += 4;
            if ((cameraHeld & 4) != 0)
                cameraY -= 4;
            if ((cameraHeld & 8) != 0)
                cameraY += 4;
            cameraX = Battle.clamp(cameraX, 0, 160);
            cameraY = Battle.clamp(cameraY, -80, 80);
            return;
        }
        if (cameraReturning) {
            Battle.Unit target = game.units[playerActor()];
            int x = Battle.clamp(target.x - 160, 0, 160), y = Battle.clamp(target.y - 100, 0, 40);
            cameraX = cameraApproach(cameraX, x);
            cameraY = cameraApproach(cameraY, y);
            if (cameraX == x && cameraY == y)
                cameraReturning = false;
            return;
        }
        int tx, ty;
        boolean queenDialogue = !TribalRules.active(game)&&(AntCaveRules.introducing(game) && game.botPhase < AntCaveRules.GUARD_ENTRY
                || AntCaveRules.dying(game) || AntCaveRules.summoning(game) && game.botPhase<AntCaveRules.REINFORCEMENTS+45
                || game.botPhase == AntCaveRules.QUEEN_THREAT || game.botPhase==AntCaveRules.QUEEN_HIT
                || game.botPhase==AntCaveRules.QUEEN_SUCCESS);
        // Queen hit/threat dialogue can remain set during projectile flight.
        // Follow the live projectile before any queen/guard dialogue framing.
        boolean queenProjectile=game.dungeonStage==2&&game.ballCount>0;
        if(queenProjectile){
            Battle.Ball b=game.balls[0];
            tx=b.x/256-160;ty=b.y/256-82;
        } else if(SulanRules.stomping(game)||SulanRules.cinematic(game)||TribalRules.active(game)&&game.tribalState==TribalRules.OPENING&&game.tribalFx>0){
            int elapsed=120-game.tribalFx,slot=SulanRules.active(game)?SulanRules.cameraActor(game):TribalRules.twins(game)?TribalRules.introActor(game):elapsed<25?0:game.active;
            if(slot==0&&onlineBattle())slot=playerActor();
            tx=game.units[slot].x-160;ty=game.units[slot].y-(game.units[slot].npc==0?135:170);
        } else if(AntCaveRules.antOpening(game)||AntCaveRules.introducing(game)&&game.botPhase<AntCaveRules.QUEEN_SPEECH){
            boolean ants=AntCaveRules.antOpening(game);
            int elapsed=game.botPhase-(ants?AntCaveRules.ANT_INTRO:AntCaveRules.QUEEN_INTRO);
            int slot=elapsed<12?0:elapsed<23?2:4;
            if(elapsed>=30)slot=game.active;
            if(game.units[slot].hp<=0)slot=0;
            tx=game.units[slot].x-160;
            ty=game.units[slot].y-(slot==1?195:135);
        } else if ((AntCaveRules.introducing(game) && game.botPhase >= AntCaveRules.GUARD_ENTRY
                || AntCaveRules.summoning(game)&&!queenDialogue)
                && game.units[3].hp > 0) {
            Battle.Unit guard = game.units[3];
            tx = guard.x - 160;
            ty = guard.y - 138;
        } else if (queenDialogue) {
            tx = game.units[1].x - 160;
            ty = game.units[1].y - 195;
        } else if (game.ballCount > 0) {
            Battle.Ball b = game.balls[0];
            tx = b.x / 256 - 160;
            ty = b.y / 256 - 82;
        } else if (cameraOnPlayer) {
            Battle.Unit u = game.units[playerActor()];
            tx = u.x - 160;
            ty = u.y - 100;
        } else {
            Battle.Unit u = game.units[game.active];
            tx = u.x - 160;
            ty = u.y - 100;
        }
        tx = Math.max(0, Math.min(160, tx));
        ty = Battle.clamp(ty, queenDialogue && !queenProjectile ? -100 : 0, 40);
        cameraX = cameraApproach(cameraX, tx);
        cameraY = cameraApproach(cameraY, ty);
        if (Math.abs(cameraX - tx) < 4)
            cameraX = tx;
        if (Math.abs(cameraY - ty) < 4)
            cameraY = ty;
    }

    void resetOnlineTerrain() throws IOException {
        net.revision=0;
        Image terrain = load("land" + map);
        terrain.getRGB(pixels, 0, terrainWidth, 0, 0, terrainWidth, terrainHeight);
        if (s > 1) {
            if (onlineGround == null)
                onlineGround = new byte[15840];
            DataInputStream in = new DataInputStream(getClass().getResourceAsStream("/collision" + map + ".dat"));
            in.readFully(onlineGround);
            in.close();
        } else
            onlineGround = null;
        for (int i = 0; i < 8; i++)
            dirty[i] = true;
        miniDirty = true;
    }

    byte[] onlineGround;

    public boolean solid(int x, int y) {
        if (x < 0 || x >= 480 || y < 0 || y >= 264)
            return false;
        if (onlineGround != null) {
            int i = y * 480 + x;
            return (onlineGround[i >> 3] & (1 << (i & 7))) != 0;
        }
        if((map==6||map==7)&&hardTerrain!=null){
            int bit=y*480+x;
            return (hardTerrain[bit>>3] & 1<<(bit&7))!=0;
        }
        return pixels != null && (pixels[(y * s) * terrainWidth + x * s] >>> 24) > 96;
    }

    public int surface(int x, int from) {
        if (x < 3 || x >= 477)
            return 294;
        for (int y = Math.max(0, from); y < 264; y++)
            if (solid(x - 2, y) || solid(x + 2, y))
                return y;
        return 294;
    }

    public void crater(int x, int y, int r) {
        if(map>=8&&map<=11)return;
        if (onlineGround != null)
            for (int py = Math.max(0, y - r); py <= Math.min(263, y + r); py++)
                for (int px = Math.max(0, x - r); px <= Math.min(479, x + r); px++)
                    if ((px - x) * (px - x) + (py - y) * (py - y) <= r * r && diggable(px, py)) {
                        int i = py * 480 + px;
                        onlineGround[i >> 3] &= ~(1 << (i & 7));
                    }
        miniDirty = true;
        int cx = x * s, cy = y * s, rr = r * s, outer = rr + 2 * s;
        int start = Math.max(0, cx - outer), end = Math.min(terrainWidth - 1, cx + outer);
        for (int py = Math.max(0, cy - outer); py < Math.min(terrainHeight, cy + outer + 1); py++)
            for (int px = start; px <= end; px++) {
                if (!diggable(px / s, py / s))
                    continue;
                int dx = px - cx, dy = py - cy, d = dx * dx + dy * dy, idx = py * terrainWidth + px;
                if (d <= rr * rr)
                    pixels[idx] = 0;
                else if (d <= outer * outer) {
                    int p = pixels[idx];
                    pixels[idx] = (p & 0xff000000) | ((p & 0x00fefefe) >>> 1);
                }
            }
        for (int i = start / (60 * s); i <= end / (60 * s) && i < 8; i++)
            dirty[i] = true;
    }

    void rebuildTiles() {
        if (pixels == null)
            return;
        int tw = 60 * s;
        for (int i = 0; i < 8; i++)
            if (dirty[i]) {
                for (int y = 0; y < terrainHeight; y++)
                    System.arraycopy(pixels, y * terrainWidth + i * tw, scratch, y * tw, tw);
                tiles[i] = null;
                tiles[i] = Image.createRGBImage(scratch, tw, terrainHeight, true);
                dirty[i] = false;
            }
    }

    // Smoothed UI frame rate, shown only when enabled in settings.
    private long fpsWindowStart;
    private int fpsWindowFrames, fpsValue;
    private String fpsText = "FPS 0",netText="",netText2="";
    private long lastNetRx,lastNetTx;
    private volatile int updateMs,paintMs;
    final FrameGapMonitor frameGap=new FrameGapMonitor();
    String gapText="";
    // Visible only with the existing FPS setting, on Sulan's online encounter.
    final String[] sulanDiagLines=new String[11];long sulanDiagAt;int sulanDiagRoom=-1;
    static long diagAge(long now,long then){return then<=0?-1:Math.max(0,now-then);}
    static int diagFlag(boolean b){return b?1:0;}
    void drawSulanDiagnostic(long now){
        if(mode!=1||!onlineBattle()||!SulanRules.active(game)||popup.opened)return;
        OnlineChunks ch=net.chunks;Battle b=game;
        if(sulanDiagRoom!=net.roomId||now-sulanDiagAt>=500){
            sulanDiagRoom=net.roomId;sulanDiagAt=now;
            sulanDiagLines[0]="HF27-842 R"+net.roomId+" T"+b.turn+" S"+net.diagServerTurn+" A"+b.active+" NPC"+b.units[b.active].npc;
            sulanDiagLines[1]="St"+b.tribalState+" Fx"+b.tribalFx+" Cl"+b.tribalClock+" Talk"+b.sulanTalk+" Cd"+b.countdown;
            sulanDiagLines[2]="Next"+diagFlag(b.pendingNext)+" ACK"+diagFlag(net.resolutionSeen)+" Local"+diagFlag(net.localTurn())+" In"+diagFlag(b.input())+" Pred"+diagFlag(net.predicting);
            sulanDiagLines[3]="CT"+ch.turn+" Rx"+ch.received+" Done"+ch.completed+" ID"+ch.id+" K"+ch.kind+" Rem"+ch.remaining;
            sulanDiagLines[4]="B"+ch.bufferedFrames()+" Gap"+ch.underruns+" Wait"+diagFlag(ch.waitingForNext)+" End"+diagFlag(ch.terminal)+" Own"+diagFlag(ch.ownPending);
            sulanDiagLines[5]="Req T"+net.diagReqTurn+" C"+net.diagReqCompleted+" A"+diagFlag(net.diagReqAck)+" Rsp"+net.diagStage+" #"+net.diagSegment+" D"+diagFlag(net.diagServerDone);
            sulanDiagLines[6]="Net"+net.diagPhase+" OK"+net.diagSyncCount+" Err"+net.diagFailures+" Age"+diagAge(now,net.diagAppliedAt)+"ms";
            sulanDiagLines[7]="Tx"+diagAge(now,net.diagSentAt)+" Rx"+diagAge(now,net.diagReplyAt)+" RTT"+net.tcp.rtt+" Q"+net.queueCount+" Op"+(net.busy?net.operation:"-");
            sulanDiagLines[8]="Rec"+diagFlag(ch.recovering)+" Seq"+diagFlag(net.sequenceResync)+" Ground"+diagFlag(net.groundAwaiting)+" Snap"+net.snapshotRecoveryTurn+" "+net.diagError;
            sulanDiagLines[9]="Balls"+b.ballCount+" Burst"+b.bursts+" Settle"+b.settle+" Scene"+diagFlag(SulanRules.cinematic(b));
            sulanDiagLines[10]="Cache"+net.diagReqCached+" WaitAge"+diagAge(now,ch.waitingSince)+"ms Own"+diagAge(now,ch.ownSince);
        }
        all();rect(0,54,320,121,0x21170e);
        for(int i=0;i<sulanDiagLines.length;i++)pcText(pcShort(sulanDiagLines[i],314),3,55+i*11);
    }

    boolean portraitViewport;Image portraitSurface;
    int pointerX(int x,int y){return portraitViewport?y-(getHeight()-320*s)/2:x;}
    int pointerY(int x,int y){return portraitViewport?240*s-1-(x-(getWidth()-240*s)/2):y;}
    protected synchronized void paint(Graphics graphics) {
        long paintStart=presentation.showFps?System.currentTimeMillis():0;
        portraitViewport=getWidth()<320*s&&getWidth()>=240*s&&getHeight()>=320*s;
        if(portraitViewport){
            if(portraitSurface==null)portraitSurface=Image.createImage(320*s,240*s);
            paintGame(portraitSurface.getGraphics());
            graphics.setClip(0,0,getWidth(),getHeight());graphics.setColor(0x10182a);graphics.fillRect(0,0,getWidth(),getHeight());
            graphics.drawRegion(portraitSurface,0,0,320*s,240*s,Sprite.TRANS_ROT90,(getWidth()-240*s)/2,(getHeight()-320*s)/2,Graphics.TOP|Graphics.LEFT);
        }else{portraitSurface=null;paintGame(graphics);}
        if(paintStart!=0)paintMs=(paintMs*3+(int)Math.min(1000L,System.currentTimeMillis()-paintStart)+2)/4;
    }
    void paintGame(Graphics graphics) {
        g = graphics;
        net.chat.roomSpeech.beginFrame();
        ox = portraitViewport?0:(getWidth() - 320 * s) / 2;
        oy = portraitViewport?0:(getHeight() - 240 * s) / 2;
        g.setColor(0x10182a);
        g.fillRect(0, 0, getWidth(), getHeight());
        if(mode==19){offlineDungeon.draw();}else if(mode==18){forge.draw();}else if (mode == 17) {
            maps.draw();
        } else if (mode == 16) {
            net.draw();
            if (net.chat.lobby()) net.chat.drawPreview();
        } else if (mode == 14)
            drawVersus();
        else if (mode == 15) {
            drawBattleSetup();
            drawBotList();
        } else if (mode == 12) {
            drawBattleSetup();
            drawRoomMapPicker();
        } else if (mode == 13) {
            drawBattleSetup();
            drawRoomHelp();
        } else if (mode == 11)
            drawPlayerInfo();
        else if (mode == 10)
            drawBattleSetup();
        else if (mode == 8)
            drawBag();
        else if (mode == 9) {
            if (pixels != null)
                drawBattle();
            else
                drawLobby();
            drawCheats();
        } else if (mode == 7)
            drawCreation();
        else if (mode == 0) {
            drawLobby();
        }
        else if (mode == 5)
            drawResult();
        else if (mode == 6)
            drawShop();
        else {
            drawBattle();
            if (mode == 3)
                drawTools();
            else if (mode == 4)
                drawHelp();
            else if (mode == 5)
                drawResult();
        }
        if(mode==0&&ranking.opened)ranking.draw();
        if (mode == 0 && net.social.opened) net.social.draw();
        if(!net.soloSearching()){
            if (net.chat.lobby()) net.chat.draw();
            net.chat.roomSpeech.draw();
            presentation.draw();
            popup.draw();
            typing.draw();
        }
        if(!net.soloSearching()&&presentation.boot==2&&toastTicks>0&&!popup.opened)drawBattleNotice();
        if (presentation.showFps) {
            long now = System.currentTimeMillis();
            if (fpsWindowStart == 0) fpsWindowStart = now;
            fpsWindowFrames++;
            if (now - fpsWindowStart >= 1000) {
                fpsValue = (int)(fpsWindowFrames * 1000L / Math.max(1L, now - fpsWindowStart));
                fpsText = "FPS " + fpsValue + ((onlineBattle()||offlineTest)?" U"+updateMs+" P"+paintMs:"");
                gapText=(onlineBattle()||offlineTest)?"Gap "+frameGap.takeWindowPeak()+"ms Shot "+frameGap.shotPeak()+"ms":"";
                if(onlineBattle()){
                    long rx=net.tcp.rxBytes+net.events.transport.rxBytes,tx=net.tcp.txBytes+net.events.transport.txBytes;
                    long ms=Math.max(1,now-fpsWindowStart);
                    long rxRate=Math.max(0,rx-lastNetRx)*1000/ms,txRate=Math.max(0,tx-lastNetTx)*1000/ms;
                    lastNetRx=rx;lastNetTx=tx;
                    netText="RTT "+net.tcp.rtt+" J "+net.tcp.jitter+" RX/TX "+rxRate+"/"+txRate+" B/s";
                    netText2="RAM "+((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024)+"K Q "+net.queueCount+" B "+net.chunks.bufferedFrames()+" G "+net.chunks.underruns;
                }else{netText=netText2="";lastNetRx=lastNetTx=0;}
                fpsWindowFrames = 0;
                fpsWindowStart = now;
            }
            all();
            rect(0,0,Math.min(180,Math.max(48,pcWidth(fpsText)+6)),14,0x21170e);
            pcText(fpsText,3,2);
            if(onlineBattle()&&netText.length()>0){rect(0,14,240,26,0x21170e);pcText(netText,3,15);pcText(netText2,3,28);}
            if((onlineBattle()||offlineTest)&&mode==1&&gapText.length()>0){int y=onlineBattle()?40:14;rect(0,y,Math.min(240,pcWidth(gapText)+6),13,0x21170e);pcText(gapText,3,y+1);}
            drawSulanDiagnostic(now);
        } else {
            fpsWindowStart = 0;
            fpsWindowFrames = fpsValue = 0;
            fpsText = "FPS 0";
        }
    }

    void clip(int x, int y, int w, int h) {
        g.setClip(ox + x * s, oy + y * s, w * s, h * s);
    }

    void all() {
        clip(0, 0, 320, 240);
    }

    void rect(int x, int y, int w, int h, int c) {
        g.setColor(c);
        g.fillRect(ox + x * s, oy + y * s, w * s, h * s);
    }

    void line(int x, int y, int xx, int yy, int c) {
        g.setColor(c);
        g.drawLine(ox + x * s, oy + y * s, ox + xx * s, oy + yy * s);
    }

    void oval(int x, int y, int w, int h, int c) {
        g.setColor(c);
        g.fillArc(ox + x * s, oy + y * s, w * s, h * s, 0, 360);
    }

    void img(Image im, int x, int y) {
        if (im != null)
            g.drawImage(im, ox + x * s, oy + y * s, Graphics.TOP | Graphics.LEFT);
    }

    void part(Image im, int x, int y, int w, int h, int px, int py, boolean flip) {
        if (im != null)
            g.drawRegion(im, x * s, y * s, w * s, h * s, flip ? Sprite.TRANS_MIRROR : Sprite.TRANS_NONE, ox + px * s,
                    oy + py * s, Graphics.TOP | Graphics.LEFT);
    }

    int[] glyphLow,glyphVietnamese;
    int glyph(char c) {
        if(glyphLow!=null){if(c<512)return glyphLow[c];if(c>=0x1e80&&c<0x1f00)return glyphVietnamese[c-0x1e80];}
        for (int i = 0; i < glyphs.length; i++)
            if (glyphs[i] == c)
                return i;
        return 31;
    }

    int textWidth(String value) {
        int w = 0;
        for (int i = 0; i < value.length(); i++)
            w += widths[glyph(value.charAt(i))];
        return w;
    }

    void text(String value, int x, int y) {
        for (int n = 0; n < value.length(); n++) {
            int idx = glyph(value.charAt(n));
            part(font, idx % 16 * 11, idx / 16 * 14, 11, 14, x, y, false);
            x += widths[idx];
        }
    }

    void center(String value, int x, int y) {
        text(value, x - textWidth(value) / 2, y);
    }

    void panel(int x, int y, int w, int h) {
        rect(x, y, w, h, 0x283b58);
        rect(x + 1, y + 1, w - 2, h - 2, 0x526987);
        rect(x + 2, y + 2, w - 4, h - 4, 0x192b45);
        line(x + 2, y + 2, x + w - 3, y + 2, 0x91b6d2);
        line(x + 2, y + h - 3, x + w - 3, y + h - 3, 0x0a172b);
    }

    void button(String label, int x, int y, int w, int h, boolean selected) {
        ui.button(label, x, y, w, h, selected);
    }

    int lobbyIconY(int index, int height) {
        return index == 2 ? 92 - height / 2 : 237 - height;
    }

    void drawLobby(){home.draw();}

    void drawLegacyLobby() {
        all();
        img(lobbyBackground, 0, 0);
        for (int i = 0; i < 5; i++) {
            boolean selected = i == lobbySelection;
            Image icon = lobbyMenu[i * 2 + (selected ? 1 : 0)];
            int w = icon.getWidth() / s, h = icon.getHeight() / s, x = LOBBY_X[i] - w / 2, y = lobbyIconY(i, h);
            img(icon, x, y);
            if (i == 2)
                img(lobbyHallLabel, LOBBY_X[i] - 24, 141);
        }
        if (onlineLabel == null) {
            onlineLabel = combatRoomArt("lobby-online-label");
            onlineHover = combatRoomArt("lobby-online-hover");
        }
        if (lobbySelection == 5)
            img(onlineHover, 179, 117);
        img(onlineLabel, 204, 201);
        drawLobbyTooltip();
    }

    String lobbyTooltipLabel() {
        return lobbySelection == 0 ? "Túi (" + keyBindings.name('i') + ")"
                : lobbySelection == 1 ? "Shop (" + keyBindings.name('s') + ")" : lobbySelection == 3 ? "Thiết lập" : "Thoát";
    }

    void drawLobbyTooltip() {
        if (lobbySelection < 0 || lobbySelection == 2 || lobbySelection == 5)
            return;
        String label = lobbyTooltipLabel();
        Image icon = lobbyMenu[lobbySelection * 2 + 1];
        int width = textWidth(label) + 12, height = 20,
                x = Battle.clamp(LOBBY_X[lobbySelection] - width / 2, 2, 318 - width),
                y = lobbyIconY(lobbySelection, icon.getHeight() / s) - height - 3;
        g.setColor(0x31291d);
        g.fillRoundRect(ox + x * s, oy + y * s, width * s, height * s, 4 * s, 4 * s);
        g.setColor(0xb19a63);
        g.drawRoundRect(ox + x * s, oy + y * s, (width - 1) * s, (height - 1) * s, 4 * s, 4 * s);
        text(label, x + 6, y + 4);
    }

    void drawTooltip(String label, int centerX, int y) {
        int width = textWidth(label) + 12, height = 20, x = Battle.clamp(centerX - width / 2, 2, 318 - width);
        g.setColor(0x31291d);
        g.fillRoundRect(ox + x * s, oy + y * s, width * s, height * s, 4 * s, 4 * s);
        g.setColor(0xb19a63);
        g.drawRoundRect(ox + x * s, oy + y * s, (width - 1) * s, (height - 1) * s, 4 * s, 4 * s);
        text(label, x + 6, y + 4);
    }

    int lobbyHit(int x,int y){return home.hit(x,y);}

    int legacyLobbyHit(int x, int y) {
        if (x >= 181 && x < 290 && y >= 117 && y < 230)
            return 5;
        if (x >= 119 && x < 208 && y >= 50 && y < 162)
            return 2;
        if (y < 176 || y >= 240)
            return -1;
        int[] order = { 0, 1, 3, 4 };
        for (int n = 0; n < 4; n++) {
            int i = order[n], half = i == 3 ? 25 : 21;
            if (x >= LOBBY_X[i] - half && x < LOBBY_X[i] + half)
                return i;
        }
        return -1;
    }

    void moveLobby(int dir){home.move(dir);}

    void moveLegacyLobby(int dir) {
        int[] px = { 64, 22, 163, 108, 300, 237 }, py = { 217, 217, 92, 217, 217, 162 };
        if (lobbySelection < 0) {
            lobbySelection = 2;
            return;
        }
        int best = -1, score = 99999;
        for (int i = 0; i < 6; i++) {
            if (i == lobbySelection)
                continue;
            int dx = px[i] - px[lobbySelection], dy = py[i] - py[lobbySelection],
                    along = dir == 1 ? -dx : dir == 2 ? dx : dir == 4 ? -dy : dy,
                    cross = dir == 1 || dir == 2 ? Math.abs(dy) : Math.abs(dx);
            if (along <= 0)
                continue;
            int cost = along + cross * 2;
            if (cost < score) {
                score = cost;
                best = i;
            }
        }
        if (best >= 0)
            lobbySelection = best;
    }

    void stepLobby(int direction) {
        moveLobby(direction < 0 ? 1 : 2);
    }

    void drawPlayerInfo() {
        all();
        img(lobbyBackground, 0, 0);
        panel(25, 25, 270, 186);
        center("THÔNG TIN", 160, 35);
        standingAppearance(43, 76);
        text("Tên: " + playerName, 100, 66);
        text("Giới tính: " + (gender == 0 ? "Nam" : "Nữ"), 100, 88);
        text("Xu: " + coins, 100, 106);
        text("Tiền: " + shoppingMoney, 100, 123);
        text("Trận thắng: " + wins, 100, 140);
        text("Vũ khí: " + WEAPON_NAMES[equipped], 100, 157);
        center("OK / Phím phải: trở về", 160, 187);
    }

    boolean hallRoomHit(int x, int y) {
        return x >= 138 && x < 211 && y >= 58 && y < 130;
    }

    void activateLobby(){home.activate();}

    void activateLegacyLobby() {
        if (lobbySelection == 5) {
            net.open();
            return;
        }
        if (lobbySelection == 0)
            openBag();
        else if (lobbySelection == 1)
            openShop();
        else if (lobbySelection == 3) {
            presentation.settings = true;
        } else if (lobbySelection == 4)
            popup.confirmQuit();
        else {
            if (onlineAccount()) {
                net.open();
                return;
            }
            mode = 10;
            loadout.toolbarTouch = false;
            loadout.toolbarHover = -1;
            menuSelection = 0;
            twoPlayers = false;
            roomSelection = 5;
        }
    }

    String roomShortName(String name) {
        while (name.length() > 1 && textWidth(name) > 53)
            name = name.substring(0, name.length() - 1);
        return name;
    }

    Image roomExitSelected;

    void loadRoomArt() {
        if (roomLayout == null) {
            roomLayout = combatRoomArt("room-layout");
            roomHost = combatRoomArt("room-host");
            roomReady = combatRoomArt("room-ready");
            roomClosed = combatRoomArt("room-closed");
            roomWaiting = combatRoomArt("room-waiting");
            roomRandom = combatRoomArt("room-map-random");
            for (int i = 0; i < 3; i++)
                roomControl[i] = combatRoomArt("room-control-" + (i == 0 ? "closed" : i == 1 ? "bot" : "human"));
        }
    }

    void drawRoomExitSelected() {
        if (roomExitSelected == null)
            roomExitSelected = combatRoomArt("room-exit-selected");
        img(roomExitSelected, 283, 207);
    }

    void drawBattleSetup() {
        all();
        loadRoomArt();
        loadout.load();
        img(loadout.background, 0, 0);
        img(loadout.left, 0, 0);
        try {
            if (roomSelf == null)
                roomSelf = standingPreview(gender, skin, true, 52, 64);
            if (allyMode != 0 && chosenAllyBot >= 0 && roomMate == null)
                roomMate = standingPreview(botSex(chosenAllyBot), botSkin(chosenAllyBot), false, 52, 64);
        } catch (IOException ignored) {
        }
        wallet(8, 24);
        g.translate(0, -28 * s);
        img(loadout.card, 5, 76);
        pcText(pcShort(playerName, 84), 11, 79);
        img(roomSelf, 29, 97);
        net.chat.roomSpeech.head(0,55,83);
        if ((wornMask & 1) != 0)
            img(weapons[equipped], 13, 143);
        img(roomHost, 12, 169);
        drawLevel(experience, 76, 96);
        img(allyMode == 0 ? loadout.closed : chosenAllyBot < 0 ? loadout.waiting : loadout.card, 108, 76);
        if (allyMode != 0 && chosenAllyBot >= 0) {
            pcText(pcShort(allyMode == 1 ? BOT_NAMES[chosenAllyBot] : "Người 2", 84), 114, 79);
            img(roomMate, 132, 97);
            img(weapons[botWeapon(chosenAllyBot)], 115, 143);
            drawLevel(Progression.botExperience(chosenAllyBot), 179, 96);
            img(roomReady, 112, 125);
        }
        g.translate(0, 28 * s);
        loadout.draw(false);
        if (roomSelection == 0)
            drawSelectionGlow(4, 47, 102, 107);
        else if (roomSelection == 1)
            drawSelectionGlow(107, 47, 102, 107);
    }

    void drawBotList() {
        if (net.social.offlineInvite) {
            net.social.drawInvitePopup();
            return;
        }
        if (botListFrame == null)
            botListFrame = combatRoomArt("room-bot-list");
        img(botListFrame, 24, 14);
        pcCenter("MỜI BOT VÀO PHÒNG", 160, 24);
        int first = botChoice / 3 * 3;
        for (int row = 0; row < 3 && first + row < 8; row++) {
            int id = first + row, y = 47 + row * 51;
            try {
                if (botPortrait[id] == null)
                    botPortrait[id] = standingPreview(botSex(id), botSkin(id), false, 32, 40);
            } catch (IOException ignored) {
            }
            img(botPortrait[id], 44, y + 2);
            pcText(BOT_NAMES[id], 88, y + 5);
            pcText(pcShort(WEAPON_NAMES[botWeapon(id)], 132), 88, y + 20);
            img(weapons[botWeapon(id)], 243, y + 10);
            if (id == botChoice)
                drawSelectionGlow(37, y - 1, 246, 48);
        }
        pcCenter("Phím hướng: chọn • OK: mời", 160, 204);
        pcCenter("Phím phải: trở về", 160, 217);
    }

    Image combatRoomArt(String name) {
        try {
            return load(name);
        } catch (IOException e) {
            return null;
        }
    }

    void drawRoomMapPicker() {
        panel(22, 43, 276, 158);
        pcCenter("CHỌN BẢN ĐỒ", 160, 49);
        for (int i = 0; i < 4; i++) {
            int x = 34 + i % 2 * 140, y = 64 + i / 2 * 59;
            if (i == 0)
                img(roomRandom, x, y);
            else {
                img(thumbs[i - 1], x + 7, y);
                pcCenter(MAPS[i - 1], x + 47, y + 42);
            }
            if (i == mapCursor)
                drawSelectionGlow(x - 3, y - 2, i == 0 ? 123 : 101, 60);
        }
        pcCenter("Chọn Ngẫu nhiên hoặc map • OK: xác nhận", 160, 187);
    }

    void drawRoomHelp() {
        panel(30, 55, 260, 132);
        center("PHÒNG THI ĐẤU", 160, 65);
        center("4 phím hướng: chuyển ô chọn", 160, 88);
        center("OK: mở mục đang chọn", 160, 109);
        center("Ô 2: Đóng / Máy / Người", 160, 130);
        center("Bắt đầu: vào trận • OK: đóng", 160, 155);
    }

    void openShop() {
        shopReturnMode = mode == 16 || mode == 10 || mode == 19 ? mode : 0;
        mode = 6;
        held = 0;
        fireKey = 0;
        game.charging = false;
        shopSelection = equipped;
        shop.open();
    }

    void buyEquip() {
        if (onlineAccount()) {
            net.buyId = shopSelection;
            net.request("buy");
            return;
        }
        int id = shopSelection;
        if ((owned & (1 << id)) == 0) {
            if (coins < PRICES[id]) {
                message("Chưa đủ vàng • thắng trận +300");
                return;
            }
            coins -= PRICES[id];
            owned |= 1 << id;
        }
        equipped = id;
        saveSettings();
        message("Đã trang bị " + WEAPON_NAMES[id]);
    }

    void drawShop() {
        shop.draw();
    }

    final DungeonConditions conditions=new DungeonConditions(this);
    // Exact logical screen bounds; keep all visible effects and animation frames.
    static boolean worldVisible(int x,int y,int w,int h) {
        return x<320 && y<240 && x+w>0 && y+h>0;
    }

    void drawBattle() {
        if (onlineBattle())
            net.visuals.age(game, System.currentTimeMillis());
        Graphics screen = g;
        int oldX = ox, oldY = oy;
        boolean zoom = showingPowCutin();
        int tribalShake=(SulanRules.active(game)?SulanRules.shake(game):TribalRules.swordShaking(game)||TribalRules.active(game)&&game.tribalPhase==2&&game.tribalFx>0&&game.tribalState!=TribalRules.DEAD)?(frame%2==0?1:-1):0;cameraX+=tribalShake;
        if (!zoom && (powWorld != null || powWorldPixels != null))
            resetPowWorld();
        if (!zoom && powCutinTicks <= 0) powCutin=null;
        if (!zoom || powWorldPixels == null) {
            if (zoom) {
                if (powWorld == null)
                    powWorld = Image.createImage(320 * s, 240 * s);
                g = powWorld.getGraphics();
                ox = oy = 0;
            }

            rebuildTiles();
            clip(0, 0, 320, 240);
            img(background, 0, 0);
            for (int i = 0; i < 8; i++) {
                int x = i * 60 - cameraX;
                if (x > -61 && x < 320)
                    img(tiles[i], x, -cameraY);
            }
            drawAimFan();
            for (int i = 0; i < game.unitCount; i++)
                drawUnit(i);
            if(TribalRules.active(game))tribalArt.worldEffects();
            drawAimRay();
            loadout.preview();
            for (int i = 0; presentation.effects && i < game.trailCount; i++) {
                if(game.trailWeapon[i]==8){
                    int x=game.trailX[i]/256-cameraX,y=game.trailY[i]/256-cameraY;
                    int radius=Math.max(1,3-game.trailAge[i]/5);
                    if(worldVisible(x-radius,y-radius,radius*2,radius*2))oval(x-radius,y-radius,radius*2,radius*2,game.trailAge[i]<5?0xb8e778:0x688449);
                    continue;
                }
                if(TribalRules.payload(game.trailWeapon[i])){tribalArt.trail(game.trailX[i]/256-cameraX,game.trailY[i]/256-cameraY,game.trailWeapon[i],game.trailAge[i],game.trailFrame[i]);continue;}
                if(game.trailWeapon[i]==AntCaveRules.QUEEN_ARROW){
                    int x=game.trailX[i]/256-cameraX,y=game.trailY[i]/256-cameraY;
                    int radius=Math.max(1,3-game.trailAge[i]/5);
                    oval(x-radius,y-radius,radius*2,radius*2,game.trailAge[i]<5?0xffbe56:0x9b622d);
                    continue;
                }
                int fade = game.trailAge[i] * 6 / game.trailLife[i];
                int size = projectileArtSize(game.trailWeapon[i]);
                if(!worldVisible(game.trailX[i]/256-cameraX-size/2,game.trailY[i]/256-cameraY-size/2,size,size))continue;
                part(trailArt[game.trailWeapon[i]], game.trailFrame[i] * size, fade * size, size, size,
                        game.trailX[i] / 256 - cameraX - size / 2, game.trailY[i] / 256 - cameraY - size / 2, false);
            }
            for (int i = 0; i < game.ballCount; i++) {
                Battle.Ball b = game.balls[i];
                int x = b.x / 256 - cameraX, y = b.y / 256 - cameraY;
                int size = projectileArtSize(b.weapon);
                if(TribalRules.payload(b.weapon)){tribalArt.projectile(b);continue;}
                if (b.freeze) {
                    if (iceDirections == null)
                        iceDirections = combatRoomArt("ice762-directions");
                    part(iceDirections, flightDirection(b.vx, b.vy) * 32, 0, 32, 32, x - 16, y - 16, false);
                } else if (b.weapon == AntCaveRules.QUEEN_ARROW) {
                    if (queenArrow == null) queenArrow = combatRoomArt("npc780-queen-arrow");
                    img(queenArrowImage(b.vx,b.vy),x-16,y-16);
                } else if (b.fly) {
                    drawFlight(b.vx, b.vy, b.age / 8 % 2, x, y);
                } else if(b.weapon==8&&tribalArt.playerSpear(b)){
                    // Bulan spear sprite, already cached during Loading.
                } else if(worldVisible(x-size/2,y-size/2,size,size))
                    part(bullets[b.weapon], projectileFrame(b) * size, b.weapon == 9 ? (b.age / 4 % 5) * size : 0, size,
                            size, x - size / 2, y - size / 2, false);
            }
            for (int i = 0; presentation.effects && i < game.blastCount; i++) {
                Battle.Blast b = game.blasts[i];
                int x = b.x - cameraX, y = b.y - cameraY;
                if(TribalRules.payload(b.weapon)){tribalArt.impact(b);continue;}
                if (b.freeze) {
                    loadout.impact(b.time, x, y);
                    continue;
                }
                if(!worldVisible(x-48,y-48,96,96))continue;
                int ef = Math.min(11, b.time * 12 / 25);
                part(impactArt(b.weapon), ef % 4 * 96, ef / 4 * 96, 96, 96, x - 48, y - 48, false);
            }
            drawDamagePopups();
        }
        if (zoom)
            drawPowWorldZoom(screen, oldX, oldY);
        all();
        cameraX-=tribalShake;
        if(AntCaveRules.titleVisible(game))drawQueenTitle();
        drawTop();
        drawHud();
        if(!TribalRules.active(game))drawDungeonSpeech();else tribalArt.overlay();
        drawPowCutin();conditions.draw();
        if (training && !game.busy()) {
            panel(52, 149, 208, 42);
            center(game.turn <= 1 ? "Giữ " + keyBindings.name(32) + " để lấy lực" : "Chỉnh góc để bắn trúng bia", 156, 152);
            center("Thả phím khi lực đạt 50-60", 156, 169);
        }
        emotions.draw();
    }

    void drawAimFan() {
        if (!game.input() || !isLocalTurn())
            return;
        Battle.Unit u = game.units[game.active];
        game.refreshAim(u);
        int low = Battle.minAimAngle(u), high = Battle.maxAimAngle(u), radius = Battle.AIM_RADIUS * s,
                side = radius * 2 + 1;
        if (aimSector == null || aimSectorMin != low || aimSectorMax != high) {
            int[] data = new int[side * side];
            int rr = radius * radius, inner = (radius - s) * (radius - s);
            int lowCos = Battle.cos(low), lowSin = Battle.sin(low), highCos = Battle.cos(high),
                    highSin = Battle.sin(high);
            for (int py = 0; py < side; py++)
                for (int px = 0; px < side; px++) {
                    int dx = px - radius, dy = radius - py, d = dx * dx + dy * dy;
                    if (d <= rr && dy * lowCos - dx * lowSin >= 0 && dy * highCos - dx * highSin <= 0) {
                        int alpha = d >= inner ? 190 : 12 + d * 70 / rr;
                        data[py * side + px] = (alpha << 24) | 0xff743a;
                    }
                }
            aimSector = Image.createRGBImage(data, side, side, true);
            aimSectorMin = low;
            aimSectorMax = high;
        }
        int x = Battle.guideOriginX(u) - cameraX, y = Battle.guideOriginY(u) - cameraY;
        g.drawRegion(aimSector, 0, 0, side, side, u.face < 0 ? Sprite.TRANS_MIRROR : Sprite.TRANS_NONE,
                ox + (x - Battle.AIM_RADIUS) * s, oy + (y - Battle.AIM_RADIUS) * s, Graphics.TOP | Graphics.LEFT);
    }

    void drawAimRay() {
        if (!game.input() || !isLocalTurn())
            return;
        Battle.Unit u = game.units[game.active];
        int distance = game.guideDistance(u);
        for (int d = 4; d < distance; d += 5) {
            int x = Battle.aimPointX(u, d) - cameraX, y = Battle.aimPointY(u, d) - cameraY;
            oval(x - 1, y - 1, 3, 3, 0xb58412);
            oval(x, y - 1, 2, 2, 0xffe74d);
        }
        int x = Battle.aimPointX(u, distance) - cameraX, y = Battle.aimPointY(u, distance) - cameraY;
        oval(x - 1, y - 1, 3, 3, 0xffe74d);
        int labelX = Battle.guideOriginX(u) - cameraX + (u.face > 0 ? -65 : 12),
                labelY = Battle.guideOriginY(u) - cameraY - 15;
        text("Góc " + Battle.aimAngle(u) + "°", Math.max(0, Math.min(265, labelX)),
                Math.max(54, Math.min(193, labelY)));
    }

    int projectileArtSize(int weapon) {
        return weapon == 9 ? 32 : Battle.projectileSize(weapon);
    }

    int projectileFrame(Battle.Ball b) {
        return Battle.ballFrame(b);
    }

    void drawFlight(int vx, int vy, int pose, int x, int y) {
        int direction = flightDirection(Math.abs(vx), vy);
        part(flightProjectile, direction * 32, pose * 32, 32, 32, x - 16, y - 16, vx < 0);
    }

    final Image[] queenArrowRotations=new Image[8];
    final int[] queenArrowAngles=new int[8];
    int queenArrowSlot;
    int queenArrowDirection(int vx,int vy){
        int best=0;long score=Long.MIN_VALUE;
        for(int i=0;i<72;i++){
            long dot=(long)vx*Battle.cos(i*5)+(long)vy*Battle.sin(i*5);
            if(dot>score){score=dot;best=i;}
        }
        return best;
    }
    Image queenArrowImage(int vx,int vy){
        int direction=queenArrowDirection(vx,vy);
        for(int i=0;i<queenArrowRotations.length;i++)if(queenArrowRotations[i]!=null&&queenArrowAngles[i]==direction)return queenArrowRotations[i];
        if(queenArrow==null)queenArrow=combatRoomArt("npc780-queen-arrow");
        int size=32*s,cos=Battle.cos(direction*5),sin=Battle.sin(direction*5);
        int[] source=new int[size*size],pixels=new int[size*size];
        queenArrow.getRGB(source,0,size,0,0,size,size);
        for(int y=0;y<size;y++)for(int x=0;x<size;x++){
            int dx=x*2-size+1,dy=y*2-size+1;
            int sx=((dx*cos+dy*sin)/1024+size-1)/2;
            int sy=((-dx*sin+dy*cos)/1024+size-1)/2;
            if(sx>=0&&sx<size&&sy>=0&&sy<size)pixels[y*size+x]=source[sy*size+sx];
        }
        int slot=queenArrowSlot;queenArrowSlot=(queenArrowSlot+1)%queenArrowRotations.length;
        queenArrowAngles[slot]=direction;
        return queenArrowRotations[slot]=Image.createRGBImage(pixels,size,size,true);
    }
    int flightDirection(int vx, int vy) {
        int best = 0;
        long score = Long.MIN_VALUE;
        for (int i = 0; i < 16; i++) {
            int degrees = i * 360 / 16;
            long dot = (long) vx * Battle.cos(degrees) + (long) vy * Battle.sin(degrees);
            if (dot > score) {
                score = dot;
                best = i;
            }
        }
        return best;
    }

    static boolean throwingWeapon(int id) {
        return Battle.isThrown(id);
    }

    boolean flightActor(int index) {
        return index == game.active && game.fly;
    }

    boolean throwingActor(int index) {
        return throwingWeapon(game.units[index].weapon) || flightActor(index)
                || index == game.active && game.freezeShot;
    }

    boolean readyToAttack(int index) {
        return index == game.active && (game.charging || (game.bursts > 0 && game.ballCount == 0
                && game.burstDelay <= (throwingActor(index) ? Battle.THROW_WINDUP : 2)));
    }

    boolean attackPose(int index) {
        return readyToAttack(index) || game.units[index].attackTicks > 0;
    }

    boolean freezeHolding(int index) {
        Battle.Unit u = game.units[index];
        return index == game.active && game.freezeShot && !game.pendingNext && !game.over && u.attackTicks == 0
                && game.ballCount == 0 && game.settle == 0;
    }

    Image buffArt(int id) {
        return id >= 11 && id <= 16 ? loadout.icon(id - 11) : id >= 0 && id < propArt.length ? propArt[id] : null;
    }

    boolean handVisible(int index) {
        return freezeHolding(index) || attackPose(index) && (!throwingActor(index) || readyToAttack(index));
    }

    int unitAnimation(int index) {
        Battle.Unit u = game.units[index];
        if (throwingActor(index)) {
            // User-selected source sequence: 32 -> 33 -> 34 -> 35.
            if (readyToAttack(index))
                return game.charging ? 18 : 18 + Math.min(2, (Battle.THROW_WINDUP - game.burstDelay) / 2);
            if (u.attackTicks > 0)
                return 21;
            if (freezeHolding(index))
                return 18;
        } else if (attackPose(index))
            return readyToAttack(index) ? 10 : 10 + Math.min(5, (8 - u.attackTicks) * 6 / 8);
        return (held & 3) != 0 && index == game.active ? 1 + frame / 2 % 9 : 1;
    }

    // Native glove joints from source cloth cells 30..35,26,27,29,28.
    static final int[] THROW_HAND_X = { -10, -11, -10, -5, -1, 0, 3, 3, 3, 1 };
    static final int[] THROW_HAND_Y = { -9, -8, -7, -9, -8, -6, -8, -7, -7, -5 };
    static final int[] THROW_HELD_FRAMES = { 7, 5, 2, 0 };

    int handOffsetX(int index) {
        int anim = unitAnimation(index);
        return throwingActor(index) ? THROW_HAND_X[anim - 16] : 7;
    }

    int handOffsetY(int index) {
        int anim = unitAnimation(index);
        return throwingActor(index) ? THROW_HAND_Y[anim - 16] : -12;
    }

    static int handSize(int weapon) {
        return throwingWeapon(weapon) ? 32 : 24;
    }

    int heldFrame(int index) {
        Battle.Unit u = game.units[index];
        if (throwingActor(index))
            return THROW_HELD_FRAMES[Math.min(3, unitAnimation(index) - 18)];
        int angle = Math.abs(Battle.aimAngle(u));
        return (angle > 90 ? 180 - angle : angle) * 7 / 90;
    }

    int heldTransform(int index) {
        Battle.Unit u = game.units[index];
        int angle = Battle.aimAngle(u);
        boolean mirrorX = (u.face < 0) ^ (!throwingActor(index) && angle > 90),
                mirrorY = !throwingActor(index) && angle < 0;
        return mirrorY ? (mirrorX ? Sprite.TRANS_ROT180 : Sprite.TRANS_MIRROR_ROT180)
                : (mirrorX ? Sprite.TRANS_MIRROR : Sprite.TRANS_NONE);
    }

    Image fadeBuffer;
    int[] fadePixels;
    final Image[] fadedUnits = new Image[Battle.MAX_UNITS];
    final int[] fadedKeys = new int[Battle.MAX_UNITS];

    void drawUnit(int index) {
        if(index>=6&&game.units[index].npc==0)return;
        int dx=0,dy=0;if(onlineBattle()&&net.liveCap){long now=System.currentTimeMillis();dx=net.motion.offset(index,false,s,now);dy=net.motion.offset(index,true,s,now);}
        g.translate(dx,dy);try{drawUnitAuthoritative(index);}finally{g.translate(-dx,-dy);}
    }

    void drawUnitAuthoritative(int index) {
        Battle.Unit u = game.units[index];
        if (loadout.hidden(index))
            return;
        if (u.invisible <= 0 || u.hp <= 0) {
            drawUnitRaw(index);
            return;
        }
        int fadeKey = unitAnimation(index) + u.weapon * 31 + u.hp * 101 + u.face * 137 + u.slope * 173
                + frame / 3 % 8 * 251 + u.freezeTurns * 300001 + u.rage * 503 + u.flashTicks * 601
                + u.landingTicks * 701 + u.lastBuff * 1901 + (index == game.active && game.freezeShot ? 1234567 : 0)
                + (index == game.active && game.fly ? 234567 : 0);
        if (fadedUnits[index] != null && fadedKeys[index] == fadeKey) {
            img(fadedUnits[index], u.x - cameraX - 40, u.y - cameraY - 70);
            return;
        }
        int width = 80 * s, height = 100 * s;
        if (fadePixels == null || fadePixels.length != width * height) {
            fadePixels = new int[width * height];
            fadeBuffer = Image.createImage(width, height);
        }
        Graphics old = g;
        int oldOx = ox, oldOy = oy, oldX = cameraX, oldY = cameraY;
        g = fadeBuffer.getGraphics();
        g.setColor(0xff00ff);
        g.fillRect(0, 0, width, height);
        ox = oy = 0;
        cameraX = u.x - 40;
        cameraY = u.y - 70;
        drawUnitRaw(index);
        fadeBuffer.getRGB(fadePixels, 0, width, 0, 0, width, height);
        g = old;
        ox = oldOx;
        oy = oldOy;
        cameraX = oldX;
        cameraY = oldY;
        for (int i = 0; i < fadePixels.length; i++) {
            int color = fadePixels[i] & 0xffffff;
            fadePixels[i] = color == 0xff00ff ? 0 : 0x58000000 | color;
        }
        fadedUnits[index] = Image.createRGBImage(fadePixels, width, height, true);
        fadedKeys[index] = fadeKey;
        img(fadedUnits[index], u.x - cameraX - 40, u.y - cameraY - 70);
    }

    final TribalArt tribalArt=new TribalArt(this);
    final Image[] npc780=new Image[4];
    final Image[][] antPoses = new Image[2][5];
    final Image[] antIdle = new Image[2];
    final Image[] antWalk = new Image[2];
    final Image[] antBeat = new Image[2];
    Image queenCall, queenBow, queenDeath, queenArrow, queenIdle, queenTitle, speechFont;
    Image bossFrame,bossHp,bossPortrait;
    Image queenShade,queenHit;
    int queenShadeAlpha;
    final int[] antLastWalk = new int[Battle.MAX_UNITS], antLastMoveFrame = new int[Battle.MAX_UNITS];
    final int[] antDeathFrame = new int[Battle.MAX_UNITS];
    final boolean[] antWasDead = new boolean[Battle.MAX_UNITS];
    private static final String[] ANT_RELUCTANT = {"Đừng trách, cuộc","đời mà... bị ép!"};
    private static final String[] ANT_ATTACK = {"Đánh... đánh... đánh!"};
    private static final String[] QUEEN_INTRO = {
        "Rương là của ta,", "bảo bối là của ta,", "chỉ cần ta nhìn thấy", "đều là của ta!"
    };
    private static final String[] QUEEN_CALL = {"Hộ giá! Hộ giá!!"};
    private static final String[] GUARD_ORDER = {
        "Tiêu diệt kẻ xâm nhập,", "Nữ Vương sẽ thưởng cho!"
    };
    private static final String[] QUEEN_THREAT = {"Cho nếm mùi lợi hại!"};
    private static final String[] ANT_HALT = {"Không được nhúc nhích!", "Tiền thưởng hội, mạng của mình."};
    private static final String[] GUARD_LOOT = {"Giao hết của cải đây!"};
    private static final String[] GUARD_HAIR = {"Dũng sĩ mạnh nhất", "được ban sợi tóc Nữ Vương."};
    private static final String[] QUEEN_HIT = {"Ặc, giáp trụ xinh", "của mình bị trầy rồi..."};
    private static final String[] QUEEN_SUCCESS = {"Trúng phóc!"};
    private static final String[] QUEEN_SUMMON = {"Cộc, cộc! Vệ sĩ đâu?", "Mau tới hộ giá!"};
    private static final String[] QUEEN_FALL = {"Ôi, vương miện của ta!"};
    private static final String[] QUEEN_LAST = {"Các ngươi hãy đợi đó!"};
    private static final String[] QUEEN_WARNING = {"Nữ Vương Kiến", "Vệ sĩ, bảo vệ Huyệt Ma Kiến!"};

    int queenActionFrame(Battle.Unit unit) {
        int phase = game.botPhase;
        if (phase >= AntCaveRules.QUEEN_CALL && phase < AntCaveRules.GUARD_ENTRY)
            return (phase - AntCaveRules.QUEEN_CALL) / 4;
        if (AntCaveRules.summoning(game))
            return Math.min(9,(phase-AntCaveRules.REINFORCEMENTS)/4);
        if (phase == AntCaveRules.QUEEN_THREAT)
            return game.charging ? 15 : 10 + (frame/5)%3;
        if (unit.attackTicks > 0)
            return Math.min(19, 16 + (8 - unit.attackTicks) / 2);
        return -1;
    }

    void drawDungeonSpeech() {
        if (game.over || !(offlineDungeon.active || onlineBattle() && net.roomKind == 3))
            return;
        int phase = game.botPhase, speaker = game.active;
        String[] lines;
        if (AntCaveRules.introducing(game)) {
            if (phase < AntCaveRules.QUEEN_SPEECH)return;
            if (phase < AntCaveRules.QUEEN_TITLE)
                lines = QUEEN_INTRO;
            else if (phase < AntCaveRules.QUEEN_CALL)
                return;
            else if (phase < AntCaveRules.GUARD_ENTRY)
                lines = QUEEN_CALL;
            else if (phase >= AntCaveRules.GUARD_ORDER_START) {
                speaker = 3;
                lines = GUARD_ORDER;
            } else return;
        } else if (phase == AntCaveRules.ANT_RELUCTANT) lines = ANT_RELUCTANT;
        else if (phase == AntCaveRules.ANT_ATTACK) lines = ANT_ATTACK;
        else if (phase == AntCaveRules.ANT_HALT) lines = ANT_HALT;
        else if (phase == AntCaveRules.GUARD_ORDER) lines = GUARD_ORDER;
        else if (phase == AntCaveRules.GUARD_LOOT) lines = GUARD_LOOT;
        else if (phase == AntCaveRules.GUARD_HAIR) lines = GUARD_HAIR;
        else if (phase == AntCaveRules.QUEEN_THREAT) lines = QUEEN_THREAT;
        else if (phase == AntCaveRules.QUEEN_SUCCESS) {speaker=1;lines=QUEEN_SUCCESS;}
        else if (phase == AntCaveRules.QUEEN_HIT) {speaker=1;lines=QUEEN_HIT;}
        else if (phase == AntCaveRules.QUEEN_WARNING) {
            drawSpeechBubble(160,100,QUEEN_WARNING);
            return;
        }
        else if (AntCaveRules.summoning(game)) {speaker=1;lines=QUEEN_SUMMON;}
        else if (AntCaveRules.dying(game)) {speaker=1;lines=phase<AntCaveRules.QUEEN_DEATH+40?QUEEN_FALL:QUEEN_LAST;}
        else return;
        Battle.Unit unit = game.units[speaker];
        int x = unit.x - cameraX, y = unit.y - cameraY;
        if (unit.hp <= 0 && !AntCaveRules.dying(game) || unit.npc == 0 || x < -12 || x > 332) return;
        drawSpeechBubble(x, y - (unit.npc == 3 ? AntCaveRules.dying(game)?63:89 : 36), lines);
    }

    void drawSpeechBubble(int x, int tip, String[] lines) {
        drawSpeechBubble(x,tip,lines,31);
    }

    void drawSpeechBubble(int x, int tip, String[] lines,int top) {
        drawSpeechBubble(x,tip,lines,top,false);
    }

    int speechBubbleLeft(int x,int width,boolean tailUp) {
        int left=tailUp?(x<160?x-width+14:x-14):x-width/2;
        return Battle.clamp(left,4,316-width);
    }

    Image speechOverride;
    void drawSpeechBubble(int x, int tip, String[] lines,int top,boolean tailUp) {
        int w = 0;
        for (int i=0; i<lines.length; i++) w=Math.max(w,pcWidth(lines[i]));
        w += 20;
        int h = lines.length * 11 + 10;
        if(!tailUp)tip=Math.max(top+h+9,tip);
        int left=speechBubbleLeft(x,w,tailUp);
        int y=tailUp?Math.max(top,tip+9):tip-h-9;
        int tail = Battle.clamp(x, left + 14, left + w - 14);
        int px = ox + left*s, py = oy + y*s;
        g.setColor(0x302d2e);
        g.fillRoundRect(px,py,w*s,h*s,12*s,12*s);
        g.fillTriangle(ox+(tail-9)*s,py+(tailUp?4:h-4)*s,ox+(tail+10)*s,py+(tailUp?4:h-4)*s,ox+x*s,oy+tip*s);
        g.setColor(0xc6cfee);
        g.fillRoundRect(px+s,py+s,(w-2)*s,(h-2)*s,11*s,11*s);
        g.fillTriangle(ox+(tail-7)*s,py+(tailUp?5:h-5)*s,ox+(tail+8)*s,py+(tailUp?5:h-5)*s,ox+x*s,oy+(tailUp?tip+2:tip-2)*s);
        g.setColor(0xffffff);
        g.fillRoundRect(px+3*s,py+3*s,(w-6)*s,(h-6)*s,8*s,8*s);
        g.fillTriangle(ox+(tail-5)*s,py+(tailUp?6:h-6)*s,ox+(tail+6)*s,py+(tailUp?6:h-6)*s,ox+x*s,oy+(tailUp?tip+5:tip-5)*s);
        if (speechFont == null) speechFont = combatRoomArt("font-dungeon-speech");
        for (int row=0; row<lines.length; row++) {
            String value=lines[row];
            int textX=left+(w-pcWidth(value))/2;
            for (int i=0; i<value.length(); i++) {
                int glyph=glyph(value.charAt(i));
                part(speechOverride==null?speechFont:speechOverride,glyph%16*8,glyph/16*10,8,10,textX,y+5+row*11,false);
                textX+=(widths[glyph]*8+5)/11;
            }
        }
    }

    Image antPose(int type, int pose, Image fallback) {
        Image[] poses = antPoses[type-1];
        if (poses[pose] == null)
            poses[pose] = combatRoomArt("npc780-"+type+"-pose-"+pose);
        return poses[pose] == null ? fallback : poses[pose];
    }
    void drawNpc780(int index) {
        Battle.Unit unit = game.units[index];
        int type = unit.npc, x = unit.x-cameraX, y = unit.y-cameraY;
        if (npc780[type] == null) npc780[type] = combatRoomArt("npc780-"+type);
        Image sprite = npc780[type];
        if (sprite == null) return;
        antWasDead[index] = false;
        if (antLastWalk[index] != unit.walk) {
            antLastWalk[index] = unit.walk;
            antLastMoveFrame[index] = frame;
        }
        y+=type==3?7:2;
        int w = sprite.getWidth()/s, h = sprite.getHeight()/s;
        boolean flip = type < 3 && unit.face > 0;
        int attackAge = 18 - unit.attackTicks;
        int queenPose = type == 3 && game.active == index ? queenActionFrame(unit) : -1;
        if(type==3&&unit.flashTicks>0){
            if(queenHit==null)queenHit=combatRoomArt("npc780-queen-hit");
            drawQueenPose(queenHit,Math.min(7,(24-unit.flashTicks)/3),x,y,unit.face>0);
        } else if (queenPose >= 0) {
            if (queenPose < 10) {
                if (queenCall == null) queenCall=combatRoomArt("npc780-queen-call");
                drawQueenPose(queenCall,queenPose,x,y,unit.face>0);
            } else {
                if (queenBow == null) queenBow=combatRoomArt("npc780-queen-bow");
                drawQueenPose(queenBow,queenPose-10,x,y,unit.face>0);
            }
        } else if (type < 3 && unit.attackTicks > 0 && (type == 2 || attackAge < 14)) {
            if (antBeat[type-1] == null)
                antBeat[type-1] = combatRoomArt("npc780-beat-"+type);
            int cell = Math.max(0, attackAge);
            if (antBeat[type-1] != null) {
                int width = antBeat[type-1].getWidth()/(5*s);
                int height = type == 1 ? 42 : 40;
                part(antBeat[type-1],cell%5*width,cell/5*height,width,height,x-width/2,y-38,flip);
            } else
                part(sprite,0,0,w,h,x-w/2,y-h,flip);
        } else if (type < 3 && unit.attackTicks == 0 && unit.walk > 0 && frame-antLastMoveFrame[index] < 5) {
            if (antWalk[type-1] == null)
                antWalk[type-1] = combatRoomArt("npc780-walk-"+type);
            int cell = (unit.walk-1)%17/2;
            if (antWalk[type-1] != null)
                part(antWalk[type-1],cell%3*48,cell/3*40,48,40,x-24,y-38,flip);
            else
                part(sprite,0,0,w,h,x-w/2,y-h,flip);
        } else if (type < 3) {
            if (antIdle[type-1] == null)
                antIdle[type-1] = combatRoomArt("npc780-idle-"+type);
            int cell = (frame+index*5)%17;
            if (antIdle[type-1] != null)
                part(antIdle[type-1],cell%5*44,cell/5*36,44,36,x-22,y-36,flip);
            else
                part(sprite,0,0,w,h,x-w/2,y-h,flip);
        } else {
            if(queenIdle==null)queenIdle=combatRoomArt("npc780-queen-idle");
            int tick=(frame/2)%25;
            int pose=tick<11?tick/2:tick==11?6:(tick+2)/2;
            if(queenIdle!=null)part(queenIdle,pose%4*76,pose/4*100,76,100,x-38,y-98,unit.face>0);
            else part(sprite,0,0,w,h,x-w/2,y-h,unit.face<0);
        }
        rect(x-13,y+2,26,3,0x293040);
        rect(x-12,y+3,Math.max(1,unit.hp*24/unit.maxHp),1,0xff7954);
        pcCenter(actorName(index),Battle.clamp(x,36,284),y+6);
    }
    void drawQueenPose(Image art,int pose,int x,int y,boolean flip) {
        part(art,pose%5*100,pose/5*102,100,102,x-50,y-98,flip);
    }

    final String[] battleNameSource=new String[Battle.MAX_UNITS],battleNameShort=new String[Battle.MAX_UNITS];
    String battleLabel(int index){
        String label=actorName(index);
        if(!game.teams)return label;
        if(label.equals(battleNameSource[index]))return battleNameShort[index];
        battleNameSource[index]=label;
        if(textWidth(label)>44){while(label.length()>1&&textWidth(label+"...")>44)label=label.substring(0,label.length()-1);label+="...";}
        return battleNameShort[index]=label;
    }

    void drawUnitRaw(int index) {
        if(index>=6&&game.units[index].npc==0)return;
        if(offlineDungeon.active&&index>0&&index<6&&index%2==0&&offlineDungeon.allyAt(index)<0)return;
        Battle.Unit u = game.units[index];
        if(u.maxHp==0&&u.hp<=0||index>=6&&u.npc==0&&u.hp<=0)return;
        if(onlineBattle()&&net.roomKind==3&&index<net.count&&index%2==0&&!net.present[index]&&u.hp<=0)return;
        if (loadout.hidden(index))
            return;
        if(u.npc>=4){tribalArt.draw(index);return;}
        if (u.npc > 0) {
            if (u.hp <= 0) {
                if (u.npc==3&&AntCaveRules.dying(game)) {
                    if (queenDeath==null)queenDeath=combatRoomArt("npc780-queen-death");
                    int pose=Math.min(12,(game.botPhase-AntCaveRules.QUEEN_DEATH)/4);
                    drawQueenPose(queenDeath,pose,u.x-cameraX,u.y-cameraY+7,u.face>0);
                    return;
                }
                // HP can reach zero before the next snapshot marks the death.
                // Unspawned reinforcement slots have zero max HP and stay hidden.
                if (u.npc < 3 && (u.dead || u.maxHp > 0)) {
                    if (!antWasDead[index]) antDeathFrame[index] = frame;
                    antWasDead[index] = true;
                    int age = frame-antDeathFrame[index];
                    if (age < 36) {
                        if (npc780[u.npc] == null)
                            npc780[u.npc] = combatRoomArt("npc780-"+u.npc);
                        Image carcass = antPose(u.npc, age < 7 ? 3 : 4, npc780[u.npc]);
                        if (carcass != null) {
                            int w = carcass.getWidth()/s, h = carcass.getHeight()/s;
                            int deathX = u.dead ? u.deathX : u.x;
                            int deathY = u.dead ? u.deathY : u.y;
                            part(carcass,0,0,w,h,deathX-cameraX-w/2,deathY-cameraY-h,u.face>0);
                        }
                    }
                    return;
                }
                Image sprite = npc780[u.npc];
                if (sprite != null && u.dead) {
                    int x = u.deathX - cameraX;
                    int y = u.deathY - cameraY;
                    g.drawRegion(sprite, 0, 0, sprite.getWidth(), sprite.getHeight(),
                            javax.microedition.lcdui.game.Sprite.TRANS_ROT90,
                            ox + (x - sprite.getHeight() / s / 2) * s,
                            oy + (y - sprite.getWidth() / s) * s, Graphics.TOP | Graphics.LEFT);
                }
                return;
            }
            drawNpc780(index);
            return;
        }
        if (u.hp <= 0) {
            drawDeadUnit(index);
            return;
        }
        int x = u.x - cameraX, y = u.y - cameraY;

        if(!(showingPowCutin()&&powCutinActor==index))vip.battleWing(index,false);
        if (u.rage >= 100)
            part(hudPowAura, frame / 3 % 5 * 64, 0, 64, 54, x - 32, y - 50, false);

        boolean shooting = attackPose(index) || freezeHolding(index);
        int anim = unitAnimation(index);
        oval(x - 10, y - 3, 22, 5, 0x453b35);
        if (!shooting && u.slope != 0)
            drawSlopedBody(index, anim, x, y);
        else {
            // Original back and body sprites share their origin, animation and mirror.
            if (!shooting)
                part(backArt(u.weapon), (anim - 1) * 32, 0, 32, 40, x - 16, y - 38, u.face < 0);
            boolean carrying = handVisible(index), rearHand = carrying && throwingActor(index) && anim == 18;
            if (rearHand)
                drawHeldItem(index, x, y);
            actorAppearance(index, anim, x - 16, y - 38, u.face < 0);
            int cs = Battle.aimCos(u), sn = Battle.aimSin(u);
            if (carrying && !rearHand)
                drawHeldItem(index, x, y);
            if (carrying && throwingActor(index)) {
                // Restore the original glove over the grip so the fingers hold it.
                int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
                g.clipRect(ox + (x + u.face * handOffsetX(index) - 2) * s, oy + (y + handOffsetY(index) - 2) * s, 4 * s,
                        4 * s);
                actorAppearance(index, anim, x - 16, y - 38, u.face < 0);
                g.setClip(cx, cy, cw, ch);
            }
            if (!throwingActor(index) && u.attackTicks >= 6) {
                oval(x + cs * 17 / 1024 - 3, y - 12 - sn * 17 / 1024 - 3, 6, 6, u.weapon == 9 ? 0x8ceaff : 0xffdb85);
            }
        }
        if(!(showingPowCutin()&&powCutinActor==index))vip.battleWing(index,true);
        if (u.freezeTurns > 0)
            loadout.frost(x, y);
        if (index == game.active && !game.busy()) {
            int by = y - 44 + (frame / 6 % 2);
            line(x - 4, by, x, by + 3, 0x5edbff);
            line(x, by + 3, x + 4, by, 0x5edbff);
        }
        rect(x - 13, y + 2, 26, 3, 0x293040);
        rect(x - 12, y + 3, 24 * u.hp / u.maxHp, 1, (game.teams ? u.team == 0 : index == 0) ? 0x58b8ff : 0xff7954);
        if (x >= 0 && x < 320) {
            String label = battleLabel(index);
            Image level = levelIcon(actorExp(index));
            int levelSpace = level == null ? 0 : level.getWidth() / s + 3;
            int labelWidth = textWidth(label);
            int labelX = Battle.clamp(x - labelWidth / 2, 1 + levelSpace, 318 - labelWidth);
            img(level, labelX - levelSpace, y + 5);
            text(label, labelX, y + 6);
        }
        if (u.landingTicks > 0) {
            if (fxLanding == null)
                fxLanding = combatEffect("flight-landing");
            int effectFrame = Math.min(12, (32 - u.landingTicks) * 13 / 32);
            part(fxLanding, effectFrame % 4 * 64, effectFrame / 4 * 80, 64, 80, x - 32, y - 72, false);
        }
        if (u.flashTicks > 0) {
            if (fxBuff == null)
                fxBuff = combatEffect("buff-flash");
            int effectFrame = Math.min(15, (36 - u.flashTicks) * 16 / 36);
            part(fxBuff, effectFrame % 4 * 56, effectFrame / 4 * 67, 56, 67, x - 28, y - 58, false);
            if (u.flashTicks <= 27 && u.flashTicks > 7)
                img(buffArt(u.lastBuff), x - 13, y - 65 - (36 - u.flashTicks) / 4);
        }
    }

    void drawDamagePopups() {
        for (int n = 0; n < game.damagePopupCount; n++) {
            Battle.DamagePopup popup = game.damagePopups[n];
            String digits = "" + popup.value;
            int x = popup.x - cameraX - digits.length() * 5, y = popup.y - cameraY - Math.min(24, popup.age * 2 / 3);
            if (popup.powered)
                img(fxDamageBackground, popup.x - cameraX - 29, y - 5);
            for (int i = 0; i < digits.length(); i++)
                part(fxDamage, (digits.charAt(i) - '0') * 12, 0, 12, 18, x + i * 10, y, false);
        }
    }

    void drawSlopedBody(int index, int anim, int x, int y) {
        if(index<0||index>=actors.length)return;
        Battle.Unit u = game.units[index];
        int key = anim + 100 * (u.slope + 65) + 20000 * (u.face + 1) + 60000 * u.weapon;
        if (slopeBody[index] == null || slopeKeys[index] != key) {
            int side = 88 * s;
            if (slopePixels == null) {
                slopePixels = new int[side * side];
                slopeRotated = new int[side * side];
                slopeLayer = new int[32 * 40 * s * s];
            }
            for (int i = 0; i < slopePixels.length; i++)
                slopePixels[i] = 0;
            stampSlopeLayer(backArt(u.weapon), anim - 1, u.face < 0);
            int sex = index == 0 ? gender : 1 - gender;
            if (onlineBattle() && net.custom[index]) {
                int[] order = LOOK_ORDER;
                int cell = s == 1
                        ? (anim >= 16 ? 8 + anim - 16 : anim == 0 ? 0 : anim < 10 ? 1 + (anim - 1) % 5 : 6 + anim % 2)
                        : anim;
                for (int i = 0; i < 5; i++)
                    stampSlopeLayer(net.layers[index][order[i]], cell, u.face < 0);
            } else if (!onlineBattle() && index == 0 && customLook) {
                int[] order = LOOK_ORDER;
                int cell = s == 1
                        ? (anim >= 16 ? 8 + anim - 16 : anim == 0 ? 0 : anim < 10 ? 1 + (anim - 1) % 5 : 6 + anim % 2)
                        : anim;
                for (int i = 0; i < 5; i++)
                    stampSlopeLayer(lookLayers[order[i]], cell, u.face < 0);
            } else
                stampSlopeLayer((onlineBattle() || index > 0) && actors[index] != null ? actors[index] : hero[sex],
                        anim, u.face < 0);
            int cs = Battle.cos(u.slope), sn = Battle.sin(u.slope), ax = 44 * s, ay = 60 * s;
            int rowX=-cs*ax+sn*ay+512,rowY=-sn*ax-cs*ay+512,pos=0;
            for(int py=0;py<side;py++,rowX-=sn,rowY+=cs){
                int nx=rowX,ny=rowY;
                for(int px=0;px<side;px++,nx+=cs,ny+=sn){
                    int sx=ax+((nx+(nx<0?1023:0))>>10),sy=ay+((ny+(ny<0?1023:0))>>10);
                    slopeRotated[pos++]=sx>=0&&sx<side&&sy>=0&&sy<side?slopePixels[sy*side+sx]:0;
                }
            }
            slopeBody[index] = Image.createRGBImage(slopeRotated, side, side, true);
            slopeKeys[index] = key;
        }
        img(slopeBody[index], x - 44, y - 60);
    }

    void stampSlopeLayer(Image art, int cell, boolean flip) {
        if (art == null)
            return;
        int width = 32 * s, height = 40 * s, side = 88 * s, left = 28 * s, top = 22 * s;
        art.getRGB(slopeLayer, 0, width, cell * width, 0, width, height);
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                int fg = slopeLayer[y * width + (flip ? width - 1 - x : x)], alpha = fg >>> 24;
                if (alpha == 0)
                    continue;
                int index = (top + y) * side + left + x, bg = slopePixels[index], backAlpha = bg >>> 24;
                if (alpha == 255 || backAlpha == 0) {
                    slopePixels[index] = fg;
                    continue;
                }
                int weight = (backAlpha * (255 - alpha) + 127) / 255, outAlpha = alpha + weight;
                int red = (((fg >> 16) & 255) * alpha + ((bg >> 16) & 255) * weight + outAlpha / 2) / outAlpha;
                int green = (((fg >> 8) & 255) * alpha + ((bg >> 8) & 255) * weight + outAlpha / 2) / outAlpha;
                int blue = ((fg & 255) * alpha + (bg & 255) * weight + outAlpha / 2) / outAlpha;
                slopePixels[index] = (outAlpha << 24) | (red << 16) | (green << 8) | blue;
            }
    }

    void drawDeadUnit(int index) {
        Battle.Unit u = game.units[index];
        int x = u.deathX - cameraX, y = u.deathY - cameraY;
        if (!u.dead) {
            x = u.x - cameraX;
            y = Math.min(Battle.H - 4, u.y) - cameraY;
        }
        img(hudTomb, x - 11, y - 29);
        int bob = Battle.sin(frame * 5) * 3 / 1024;
        actorAppearance(index, 26, x - 16, y - 65 + bob, u.face < 0);
        center(actorName(index), Math.max(30, Math.min(290, x)), y + 5);
    }

    void drawHeldItem(int index, int x, int y) {
        if (flightActor(index)) {
            Battle.Unit u = game.units[index];
            drawFlight(Battle.aimCos(u), -Battle.aimSin(u), 0,
                    x + u.face * handOffsetX(index), y + handOffsetY(index));
            return;
        }
        if (index == game.active && game.freezeShot) {
            img(loadout.ice(), x + game.units[index].face * handOffsetX(index) - 12, y + handOffsetY(index) - 8);
            return;
        }
        Battle.Unit u = game.units[index];
        if(u.weapon==8&&tribalArt.heldPlayerSpear(u.face,unitAnimation(index),Battle.aimAngle(u),x+u.face*handOffsetX(index),y+handOffsetY(index)))return;
        int size = handSize(u.weapon);
        Image art = heldArt(u.weapon);
        if (art != null)
            g.drawRegion(art, heldFrame(index) * size * s, 0, size * s, size * s, heldTransform(index),
                    ox + (x + u.face * handOffsetX(index) - size / 2) * s, oy + (y + handOffsetY(index) - size / 2) * s,
                    Graphics.TOP | Graphics.LEFT);
    }

    Image opponentStanding;
    int opponentStandingGender = -1;
    final Image[] teamNameBackground = new Image[2];

    void drawTeamName() {
        String name = activeStandingName();
        int side = game.teams ? game.units[game.active].team : game.active;
        int limit = game.dungeonStage==2&&game.active==1 ? 132 : 108;
        int w = Math.min(limit, textWidth(name) + 9);
        if (teamNameBackground[side] == null || teamNameBackground[side].getWidth() != w * s) {
            int width = w * s, height = 11 * s;
            int[] pixels = new int[width * height];
            int color = side == 0 ? 0x164bff : 0xf02026;
            for (int x = 0; x < width; x++) {
                int alpha = 235 * (width - x) / width;
                for (int y = 0; y < height; y++)
                    pixels[y * width + x] = (alpha << 24) | color;
            }
            teamNameBackground[side] = Image.createRGBImage(pixels, width, height, true);
        }
        img(teamNameBackground[side], 0, 0);
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.clipRect(ox, oy, limit * s, 14 * s);
        text(name, 1, 0);
        g.setClip(cx, cy, cw, ch);
    }

    String activeStandingName() {
        return actorName(game.active);
    }

    void drawQueenTitle() {
        int age=game.botPhase-AntCaveRules.QUEEN_TITLE;
        int alpha=56+Math.min(5,Math.min(age,AntCaveRules.QUEEN_CALL-game.botPhase)/2)*20;
        if(queenShade==null||queenShadeAlpha!=alpha){
            int[] pixels=new int[320*s*8*s];
            for(int i=0;i<pixels.length;i++)pixels[i]=alpha<<24;
            queenShade=Image.createRGBImage(pixels,320*s,8*s,true);
            queenShadeAlpha=alpha;
        }
        for(int y=0;y<240;y+=8)img(queenShade,0,y);
        if(queenTitle==null)queenTitle=combatRoomArt("npc780-queen-title");
        int reveal=age<10?101:age<AntCaveRules.QUEEN_TITLE_REVEAL?183:304;
        clip(8,40,reveal,34);
        img(queenTitle,8,40);
        all();
    }

    void drawBossStatus() {
        if(bossFrame==null){
            bossFrame=combatRoomArt("npc780-boss-frame");
            bossHp=combatRoomArt("npc780-boss-hp");
            bossPortrait=combatRoomArt("npc780-boss-portrait");
        }
        Battle.Unit queen=game.units[1];
        int percent=queen.hp<=0?0:Math.max(1,queen.hp*100/Math.max(1,queen.maxHp));
        int x=171;
        int cy=g.getClipY(),cx=g.getClipX(),cw=g.getClipWidth(),ch=g.getClipHeight();
        img(bossFrame,x,0);
        img(bossPortrait,x+3,3);
        if(percent>0){
            g.clipRect(ox+(x+29)*s,oy+10*s,Math.max(1,46*percent/100)*s,12*s);
            img(bossHp,x+23,9);
        }
        g.setClip(cx,cy,cw,ch);
        pcCenter("Daniliz",x+43,2);
        pcCenter(percent+"%",x+50,14);
    }

    void drawTop() {
        if(game.dungeonStage!=2||game.active!=1)drawTeamName();
        if(offlineDungeon.active||onlineBattle()&&net.roomKind==3){int stage=offlineDungeon.active?offlineDungeon.stage:net.dungeonStage,kills=offlineDungeon.active?offlineDungeon.kills:net.dungeonKills,diff=offlineDungeon.active?offlineDungeon.difficulty:net.dungeonDifficulty;if(stage<3)pcText(stage==1?"Ải 1: "+kills+"/20":"Ải 2: Nữ Vương",72,32);pcText(dungeon.difficultyName(diff),193,32);}
        tribalArt.hud();
        int activeNpc = game.units[game.active].npc;
        if(activeNpc==0)vip.back(vip.actorExp(game.active),vip.actorWing(game.active),14,58,1,false);
        if(activeNpc>=4)tribalArt.header(activeNpc);
        else if (activeNpc > 0 && activeNpc < npcHeader780.length) {
            if (npcHeader780[activeNpc] == null) {
                Image source = combatRoomArt("npc780-" + activeNpc);
                if (source != null) {
                    int width = Math.min(48, source.getWidth()/s);
                    int height = source.getHeight()*width/source.getWidth();
                    npcHeader780[activeNpc] = NativeUi.fit(source,width*s,height*s,0);
                }
            }
            img(npcHeader780[activeNpc],0,12);
        } else if (onlineBattle())
            img(actorStand[game.active], -2, 9);
        else if (game.active == 0)
            standingAppearance(-2, 9);
        else if (actorStand[game.active] != null)
            img(actorStand[game.active], -2, 9);
        else {
            int opponentGender = 1 - gender;
            if (opponentStanding == null || opponentStandingGender != opponentGender) {
                try {
                    opponentStanding = load("heroStand" + opponentGender);
                    opponentStandingGender = opponentGender;
                } catch (IOException e) {
                    opponentStanding = null;
                }
            }
            img(opponentStanding, -2, 9);
        }
        if (game.teams || onlineBattle()&&net.roomKind==3) {
            for (int i = 0; i < game.unitCount; i++)
                if(loadingParticipant(i) && (!offlineDungeon.active||game.units[i].hp>0))drawTurnPortrait(i, i % 2 == 0 ? (game.dungeonStage>0?61:70) + i / 2 * 24 : 190 + i / 2 * 24, 1);
        } else {
            drawTurnPortrait(0, 112, 1);
            drawTurnPortrait(1, 184, 1);
        }
        drawMiniMap();
        int windX=game.dungeonStage==2?152:160;
        img(hudWindTitle,windX-12,0);
        part(hudWindArrow,0,0,36,12,windX-18,10,game.wind>=0);
        center(""+Math.abs(game.wind)/10+"."+Math.abs(game.wind)%10,windX,10);
        if(game.dungeonStage==2){
            drawBossStatus();
            if(game.active==1)drawTeamName();
        }
        if (game.units[game.active].frozen != 2 && !game.over && !game.busy() && !game.pendingNext && isLocalTurn()) {
            int seconds = (game.countdown + 29) / 30;
            String digits = "" + seconds;
            int x = 160 - digits.length() * 10;
            for (int i = 0; i < digits.length(); i++)
                part(hudCountDown, (digits.charAt(i) - '0') * 20, 0, 20, 28, x + i * 20, 32, false);
            img(hudPass, 144, 62);
        }
    }

    final Image[] npcHud780=new Image[4],npcHeader780=new Image[4];
    void drawTurnPortrait(int index, int x, int y) {
        net.chat.roomSpeech.head(index,x+12,y+26);
        img(hudPropSlot, x, y);
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.clipRect(ox + (x + 1) * s, oy + (y + 1) * s, 22 * s, 22 * s);
        int type = game.units[index].npc;
        if (type > 0 && type < npcHud780.length) {
            if (npcHud780[type] == null) {
                Image source = combatRoomArt("npc780-" + type);
                if (source != null) {
                    if (type == 3)
                        source = Image.createImage(source,0,0,source.getWidth(),Math.min(source.getHeight(),source.getWidth()),0);
                    npcHud780[type] = NativeUi.fit(source,22*s,22*s,0);
                }
            }
            img(npcHud780[type],x+1,y+1);
        } else {
            vip.back(vip.actorExp(index),vip.actorWing(index),x+12,y+25,2,false);actorAppearance(index,0,x-4,y-7,false);
        }
        g.setClip(cx, cy, cw, ch);
        int color = index == game.active ? 0xffd455 : 0xc5d1d9;
        line(x, y, x + 23, y, color);
        line(x, y, x, y + 23, color);
        line(x + 23, y, x + 23, y + 23, color);
        nativeFill(hudPortraitHP, x + 1, y + 24, 22, 2, game.units[index].hp, game.units[index].maxHp);
        if (index == game.active && !game.over)
            drawSelectionGlow(x - 2, y - 2, 28, 28);
    }

    int[] miniSample;
    void drawMiniMap() {
        if (miniTerrain == null || miniDirty) {
            int width = 70 * s, height = 34 * s;
            if(miniSample==null)miniSample=new int[width*height];
            int[] sample=miniSample;
            if (pixels != null)
                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        sample[y * width + x] = pixels[(y * terrainHeight / height) * terrainWidth
                                + x * terrainWidth / width];
            miniTerrain = Image.createRGBImage(sample, width, height, true);
            miniDirty = false;
        }
        img(hudMapFrame, 248, 0);
        img(hudMapGrid, 249, 9);
        img(miniTerrain, 249, 9);
        img(hudMapTitle, 248, 0);
        img(hudMapSettings, 302, 1);
        img(hudMapClose, 311, 1);
        text("Dừng", 270, 0);
        int left = 249 + Battle.clamp(cameraX, 0, 160) * 70 / 480, top = 9 + Battle.clamp(cameraY, 0, 40) * 34 / 264;
        int width = 320 * 70 / 480, height = Math.min(240, 264 - Math.max(0, cameraY)) * 34 / 264;
        line(left, top, left + width, top, 0xf3f0dc);
        line(left, top, left, top + height, 0xf3f0dc);
        line(left + width, top, left + width, top + height, 0xf3f0dc);
        line(left, top + height, left + width, top + height, 0xf3f0dc);
        for (int i = 0; i < game.unitCount; i++) {
            Battle.Unit u = game.units[i];
            if (loadout.hidden(i) || game.dungeonStage>0&&u.hp<=0)
                continue;
            oval(248 + u.x * 70 / 480, 8 + Math.min(264, u.y) * 34 / 264, 3, 3,
                    u.hp <= 0 ? 0x9b9b9b : (game.teams ? u.team == 0 : i == 0) ? 0x48aeff : 0xff3734);
        }
    }

    final Image[] grayProps = new Image[5];

    Image grayProp(int i) {
        if (grayProps[i] == null)
            grayProps[i] = combatRoomArt("buff754-gray" + i);
        return grayProps[i];
    }

    boolean propDisabled(int i, Battle.Unit u) {
        return u.energy < Battle.propCost(i) || i == 4 && u.flyCooldown > 0
                || i == 3 && (u.heals <= 0 || u.hp >= u.maxHp) || i == 0 && game.extra > 2 || i == 1 && game.triple
                || i == 2 && game.boost >= 100 || game.freezeShot && (i == 0 || i == 1 || i == 2 || i == 4);
    }

    void drawHud() {
        Battle.Unit u = game.units[playerActor()];
        img(hudAngleJoin, 0, 198);
        // Buff history belongs to the active actor, not the local player.
        // Show the same ordered list for humans and bots on both teams.
        for (int i = 0; i < game.usedPropCount; i++) {
            int x = 46 + i % 6 * 25, y = 189 - i / 6 * 25, id = game.usedPropHistory[i];
            img(hudPropSlot, x, y);
            img(buffArt(id), x - 1, y + 1);
        }
        // Paint power numbers and pointer over the buff icons.
        drawPowerRuler(u);
        drawAngleDial(u);
        img(hudHealthFrame, 208, 212);
        nativeFill(hudEnergyFill, 210, 214, 54, 6, u.energy, u.maxEnergy);
        nativeFill(hudHealthFill, 210, 223, 63, 11, u.hp, u.maxHp);
        center("" + u.energy, 237, 212);
        center("" + u.hp, 241, 222);
        int rage = Battle.clamp(u.rage, 0, 100);
        part(hudPowRing, rage / 10 * 44, 0, 44, 44, 270, 188, false);
        if (rage >= 100) {
            if (frame / 5 % 2 == 0) {
                img(hudPowReady, 278, 195);
            } else
                img(hudPowLabel, 282, 202);
        } else {
            img(hudPowLabel, 282, 202);
            center(isLocalTurn() && game.pow ? "Đã bật" : "" + rage, 292, 220);
        }
        if (isLocalTurn() && game.pow) {
            line(272, 188, 311, 188, 0xffe37e);
            line(272, 232, 311, 232, 0xffe37e);
        }
        for (int i = 0; i < 5; i++) {
            img(hudPropSlot, 296, 50 + i * 27);
            img(propDisabled(i, u) ? grayProp(i) : propArt[i], 295, 51 + i * 27);
            String boundKey = keyBindings.name(PROP_KEYS[i].charAt(0) + 32);
            int keyRight = i == 3 || i == 4 && u.flyCooldown > 0 ? 304 : 319;
            int keyX = Math.min(297, keyRight - textWidth(boundKey));
            rect(keyX - 1, 50 + i * 27, textWidth(boundKey) + 2, 9, 0x263444);
            text(boundKey, keyX, 48 + i * 27);
            if (i == 4 && u.flyCooldown > 0) {
                text("" + Math.min(2, u.flyCooldown), 306, 48 + i * 27);
            }
            if (i == 3) {
                rect(305, 50 + i * 27, 15, 10, 0x172235);
                text("" + u.heals, 306, 48 + i * 27);
            }
        }
        loadout.hud();
        String powKey = keyBindings.name('b');
        text(powKey, Math.min(306, 319 - textWidth(powKey)), 188);
        if (buffFlash > 0 && mode != 3)
            drawTools();
        text(freeCamera ? "Về NV" : "Camera", 2, 187);
    }

    void drawAngleDial(Battle.Unit u) {
        if (dialFace == null || dialSlope != u.slope) {
            int side = 40 * s;
            if (dialPixels == null) {
                dialPixels = new int[side * side];
                hudAngle.getRGB(dialPixels, 0, side, 0, 0, side, side);
            }
            if(dialRotated==null)dialRotated=new int[side*side];
            int[] rotated=dialRotated;
            int cs = Battle.cos(u.slope), sn = Battle.sin(u.slope), middle = side - 1;
            for (int py = 0; py < side; py++)
                for (int px = 0; px < side; px++) {
                    int dx = 2 * px - middle, dy = 2 * py - middle;
                    int sx = (cs * dx - sn * dy + middle * 1024 + 1024) / 2048,
                            sy = (sn * dx + cs * dy + middle * 1024 + 1024) / 2048;
                    rotated[py * side + px] = sx >= 0 && sx < side && sy >= 0 && sy < side ? dialPixels[sy * side + sx] : 0;
                }
            dialFace = Image.createRGBImage(rotated, side, side, true);
            dialSlope = u.slope;
        }
        img(dialFace, 0, 198);
        dialNeedle(u, Battle.minAimAngle(u), 0x151515, 17);
        dialNeedle(u, Battle.maxAimAngle(u), 0x151515, 17);
        dialNeedle(u, Battle.aimAngle(u), 0xf23825, 16);
        oval(18, 216, 4, 4, 0xb8bac1);
        center("" + u.angle + "°", 20, 221);
    }

    void dialNeedle(Battle.Unit u, int angle, int color, int radius) {
        int x = 20 + u.face * Battle.cos(angle) * radius / 1024, y = 218 - Battle.sin(angle) * radius / 1024;
        line(20, 218, x, y, color);
        line(20, 217, x, y - 1, color);
    }

    void drawPowerRuler(Battle.Unit u) {
        img(hudPowerFrame, 40, 222);
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.clipRect(ox + 55 * s, oy + 229 * s, 151 * s, 6 * s);
        if (u.lastPower >= 0)
            nativeFill(hudPowerLast, 55, 229, 151, 6, u.lastPower, 100);
        if (isLocalTurn() && (game.charging || game.busy()))
            nativeFill(hudPowerFill, 55, 229, 151, 6, u.power, 100);
        g.setClip(cx, cy, cw, ch);
        img(hudPowerTicks, 55, 225);
        for (int mark = 0; mark <= 100; mark += 5) {
            int x = 55 + 151 * mark / 100;
            if (mark > 0 && mark % 20 == 0)
                center("" + mark, Math.min(199, x), 215);
        }
        g.setColor(0xed3f2c);
        g.fillTriangle(ox + 51 * s, oy + 221 * s, ox + 59 * s, oy + 221 * s, ox + 55 * s, oy + 225 * s);
        if (isLocalTurn() && (game.charging || game.busy())) {
            int pointer = 55 + 151 * Battle.clamp(u.power, 0, 100) / 100;
            line(pointer, 225, pointer, 234, 0x40e8ff);
            g.setColor(0x40e8ff);
            g.fillTriangle(ox + (pointer - 4) * s, oy + 218 * s, ox + (pointer + 4) * s, oy + 218 * s, ox + pointer * s,
                    oy + 225 * s);
            center("" + u.power, Math.max(62, Math.min(198, pointer)), 204);
        }
    }

    void nativeFill(Image art, int x, int y, int w, int h, int value, int maximum) {
        int width = w * Battle.clamp(value, 0, maximum) / maximum;
        if (width > 0)
            part(art, 0, 0, width, h, x, y, false);
    }

    void drawTools() {
        drawSelectionGlow(292, 48 + propSelection * 27, 28, 28);
    }

    void drawSelectionGlow(int x, int y, int w, int h) {
        drawSelectionGlowPhase(x, y, w, h, frame / 5 % 4);
    }

    void drawItemSelection(int x, int y, int w, int h, boolean inspecting) {
        int t = (int) (System.currentTimeMillis() / 180 % 6);
        drawSelectionGlowPhase(x, y, w, h, inspecting ? (t < 4 ? t : 6 - t) : 0);
    }

    void drawSelectionGlowPhase(int x, int y, int w, int h, int phase) {
        if (hudSelectGlow == null)
            try {
                hudSelectGlow = load("hud-select-glow");
            } catch (Exception ignored) {
                return;
            }
        int frameX = phase * 28, corner = 6;
        part(hudSelectGlow, frameX, 0, corner, corner, x, y, false);
        part(hudSelectGlow, frameX + 22, 0, corner, corner, x + w - corner, y, false);
        part(hudSelectGlow, frameX, 22, corner, corner, x, y + h - corner, false);
        part(hudSelectGlow, frameX + 22, 22, corner, corner, x + w - corner, y + h - corner, false);
        for (int dx = corner; dx < w - corner; dx += 16) {
            int width = Math.min(16, w - corner - dx);
            part(hudSelectGlow, frameX + corner, 0, width, corner, x + dx, y, false);
            part(hudSelectGlow, frameX + corner, 22, width, corner, x + dx, y + h - corner, false);
        }
        for (int dy = corner; dy < h - corner; dy += 16) {
            int height = Math.min(16, h - corner - dy);
            part(hudSelectGlow, frameX, corner, corner, height, x, y + dy, false);
            part(hudSelectGlow, frameX + 22, corner, corner, height, x + w - corner, y + dy, false);
        }
    }

    void enterBuffs() {
        if (!isLocalTurn() || !game.input())
            return;
        mode = 3;
        propSelection = Battle.clamp(propSelection, 0, 4);
        held = fireKey = 0;
        pressTouch = -1;
        game.charging = false;
    }

    void exitBuffs() {
        mode = 1;
        held = fireKey = 0;
        pressTouch = -1;
        game.charging = false;
    }

    void drawHelp() {
        panel(11, 27, 298, 183);
        center("HƯỚNG DẪN", 160, 36);
        String[] lines = { keyBindings.name(52)+" / "+keyBindings.name(54)+": di chuyển", keyBindings.name(50)+" / "+keyBindings.name(56)+": chỉnh góc", "Giữ "+keyBindings.name(32)+": lấy lực", "Thả phím để bắn; đồng hồ dừng", keyBindings.name('q')+" +2, "+keyBindings.name('w')+" x3, "+keyBindings.name('e')+" +50%", keyBindings.name('r')+" hồi HP, "+keyBindings.name('t')+" bay, "+keyBindings.name('b')+" POW", keyBindings.name('z')+" / "+keyBindings.name('x')+" / "+keyBindings.name('c')+": đạo cụ", keyBindings.name('v')+": bỏ lượt; "+keyBindings.name('f')+": đổi vũ khí", "Quá lực: quay về 0 một lần rồi bỏ lượt" };
        for (int i = 0; i < lines.length; i++)
            text(lines[i], 26, 57 + i * 16);
    }

    void drawResult() {
        if(offlineTest){
            all();rect(0,0,320,240,0x192b45);
            img(game.winner==0?hudResultWin:game.winner==1?hudResultLose:hudResultTie,game.winner==0?65:game.winner==1?101:125,43);
            pcCenter("TEST BOT OFFLINE - Không tính thưởng",160,105);
            pcCenter("Shot gap: "+frameGap.shotPeak()+" ms",160,123);
            button("Chơi lại",80,150,160,25,true);
            button("Về đăng nhập",80,185,160,25,false);
            return;
        }
        if (net != null) {
            net.results.draw();
            return;
        }
        int winner = game.winner;
        if (onlineBattle() && winner < 2)
            winner = winner == net.myActor % 2 ? 0 : 1;
        if (winner == 0)
            img(hudResultWin, 65, 43);
        else if (winner == 1)
            img(hudResultLose, 101, 43);
        else
            img(hudResultTie, 125, 43);
        center(onlineBattle() ? "OK: phòng • Phím phải: phòng" : "OK: chơi lại • Phím phải: sảnh", 160, 179);
    }

    void syncTurnNotice() {
        if(mode!=1||onlineBattle()&&net.phase!=1)return;
        if (game.turn == lastTurn)
            return;
        lastTurn = game.turn;
        if (isLocalTurn() && !game.over)
            message("Đến lượt bạn!");
        else if (toast.equals("Đến lượt bạn!")) {
            toast = "";
            toastTicks = 0;
        }
    }

    Image noticeSource, noticePanel, noticeFont;
    int noticeW, noticeH, noticeCount;
    final String[] noticeRows=new String[5];
    String noticeText;

    void drawBattleNotice() {
        if (noticeSource == null) {
            noticeSource = combatRoomArt("notice754-bg");
            noticeFont = combatRoomArt("font-pc-yellow");
        }
        String value=toast==null?"":toast;
        if(!value.equals(noticeText)||noticePanel==null){
            noticeText=value;
            String rest=value;
            int count=0,w=0;
            while(rest.length()>0&&count<noticeRows.length){
                int end=rest.length();
                while(end>1&&pcWidth(rest.substring(0,end))>280)end--;
                if(end<rest.length()){int space=rest.lastIndexOf(' ',end);if(space>0)end=space;}
                noticeRows[count]=rest.substring(0,end);
                w=Math.max(w,pcWidth(noticeRows[count++]));rest=rest.substring(end).trim();
            }
            noticeCount=count;
            for(int i=count;i<noticeRows.length;i++)noticeRows[i]=null;
            w=Math.max(110,w+18);int h=count*12+10;
            if(noticePanel==null||noticeW!=w||noticeH!=h){
                noticeW=w;noticeH=h;noticePanel=NativeUi.fit(noticeSource,w*s,h*s,4*s);
            }
        }
        img(noticePanel,(320-noticeW)/2,63);
        for(int i=0;i<noticeCount;i++)tooltip.colored(noticeRows[i],160-pcWidth(noticeRows[i])/2,68+i*12,noticeFont);
    }

    void message(String value) {
        toast = value==null?"":value;
        toastTicks = 55;
    }

    int action(int key) {
        try {
            return getGameAction(key);
        } catch (Exception ignored) {
            return 0;
        }
    }

    int direction(int key) {
        int a = action(key);
        if (a == LEFT || key == KEY_NUM4)
            return 1;
        if (a == RIGHT || key == KEY_NUM6)
            return 2;
        if (a == UP || key == KEY_NUM2)
            return 4;
        if (a == DOWN || key == KEY_NUM8)
            return 8;
        return 0;
    }

    boolean fire(int key) {
        return key == 32 || key == 13 || key == 10 || key == KEY_NUM5 || action(key) == FIRE;
    }

    boolean nokiaQwerty() {
        String platform = System.getProperty("microedition.platform");
        if (platform == null)
            return false;
        platform = platform.toUpperCase();
        return platform.indexOf("E72") >= 0 || platform.indexOf("E71") >= 0 || platform.indexOf("E63") >= 0
                || platform.indexOf("E6-") >= 0;
    }

    // int buffKey(int key) {
    //     if (key == '1' && nokiaQwerty())
    //         return 1;
    //     int lower = key >= 65 && key <= 90 ? key + 32 : key;
    //     return lower == 'q' ? 0
    //             : lower == 'w' ? 1 : lower == 'e' ? 2 : lower == 'r' ? 3 : lower == 't' ? 4 : lower == 'b' ? 5 : -1;
    // }
    int buffKey(int key) {
        if (key == '1' && nokiaQwerty()) return 1;

        // Nokia đang dùng bàn phím tiếng Việt
        if (key == 'ư' || key == 'Ư') {
            return 1; // Tương đương phím W
        }

        int lower = key >= 'A' && key <= 'Z' ? key + 32 : key;

        return lower == 'q' ? 0 :
            lower == 'w' ? 1 :
            lower == 'e' ? 2 :
            lower == 'r' ? 3 :
            lower == 't' ? 4 :
            lower == 'b' ? 5 : -1;
    }
    
    int cameraApproach(int from, int to) {
        int diff = to - from;
        if (diff == 0)
            return from;
        int step = Math.max(1, Math.min(12, Math.abs(diff) / 5));
        return from + (diff < 0 ? -step : step);
    }

    void toggleCamera() {
        if (game.charging && isLocalTurn()) {
            message("Thả phím bắn trước khi xem camera.");
            return;
        }
        freeCamera = !freeCamera;
        cameraHeld = held = fireKey = 0;
        cameraReturning = !freeCamera;
        if (!freeCamera)
            cameraOnPlayer = true;
        message(freeCamera ? "4 hướng: xem bản đồ • "+keyBindings.name(-6)+": về NV" : "Đã trở về nhân vật");
    }

    protected synchronized void keyPressed(int key) {
        if(net.soloSearching()){int mapped=keyBindings.map(key);if(key==-7||mapped==-7||key==27||key=='0')net.cancelSolo();else if(key=='h'||key=='H'||mapped=='h'||mapped=='H')net.soloBot();return;}
        if (typing.key(key)) { repaint(); return; }
        if(mode==1&&game.dungeonStage>0&&(key=='m'||key=='M')){conditions.toggle();repaint();return;}
        int rawKey = key;
        if (keyBindings.press(key)) return;
        key = keyBindings.map(key);
        if (mode == 1 && powShotPending && rawKey!=13 && rawKey!=10)
            return;
        int dir = direction(key);
        boolean ok = fire(key);
        if (popup.opened) {
            popup.key(key, dir, ok);
            return;
        }
        if(mode==16&&net.roomKind==3&&net.roomId>0&&dungeon.rewardsOpen&&!net.social.opened){dungeon.keyRewards(key,dir,ok);return;}
        if((key=='g'||key=='G')&&onlineAccount()&&economy.testCheats&&mode!=9){openCheats();return;}
        if (net.chat.opened && !presentation.settings && net.chat.key(net.chat.rawBinding(rawKey,key), dir, ok))
            return;
        if(roomKitContext()&&loadout.groupKey(key,dir,ok,mode!=10))return;
        if (presentation.key(key, dir, ok))
            return;
        if (mode == 0 && net.social.opened) {
            net.social.key(key, dir, ok);
            return;
        }
        if(mode==0&&ranking.opened){ranking.key(key,dir,ok);return;}
        if(!onlineAccount()&&!offlineTest&&mode!=16){net.loginForm(false);return;}
        if (net.chat.roomSpeech.key(net.chat.rawBinding(rawKey,key),dir,ok)) return;
        if (net.chat.key(net.chat.rawBinding(rawKey,key), dir, ok)) return;
        if (mode == 10 && (key == -6 || key == -7)) {
            if (key == -6)
                inviteBot();
            else {
                roomSelection = 5;
                roomActivate();
            }
            return;
        }
        if (mode == 16 && net.logged && net.roomId > 0 && net.roomKind != 3 && net.phase == 0 && !net.social.opened && !net.create.opened
                && (key == -6 || key == -7)) {
            if (key == -6)
                net.waiting.invite();
            else if (presentation.searching() && net.host)
                presentation.toggleSearch();
            else {
                roomSelection = 5;
                net.waiting.activate();
            }
            return;
        }
        if (key == -9 && (mode == 0 || mode == 10 || mode == 16)) {
            presentation.settings = true;
            return;
        }
        if (mode == 1 && emotions.opened) {
            emotions.key(key, dir, ok);
            return;
        }
        if (mode == 1 && key == '@') {
            emotions.toggle();
            return;
        }
        if ((mode == 1 || mode == 3) && key == -6) {
            mode = 1;
            toggleCamera();
            return;
        }
        if (mode == 1 && freeCamera && dir != 0) {
            cameraHeld |= dir;
            return;
        }
        if (mode == 1 || mode == 3) {
            int lowerItem = key >= 65 && key <= 90 ? key + 32 : key;
            if (lowerItem == 'z' || lowerItem == 'x' || lowerItem == 'c') {
                loadout.use(lowerItem == 'z' ? 0 : lowerItem == 'x' ? 1 : 2);
                return;
            }
            int prop = buffKey(key);
            if (prop >= 0) {
                if (freeCamera) {
                    message(keyBindings.name(-6)+": trở về nhân vật để dùng đạo cụ.");
                    return;
                }
                if (!isLocalTurn()) {
                    message("Đến lượt bạn mới sử dụng được");
                    return;
                }
                if (mode == 3)
                    exitBuffs();
                useProp(prop);
                return;
            }
            if (freeCamera && key != -7 && key != '0')
                return;
        }
        if ((mode == 0 || mode == 7 && !onlineAccount()) && (key == 'o' || key == 'O')) {
            net.open();
            return;
        }
        if(mode==19){offlineDungeon.key(key,dir,ok);return;}if(mode==18){forge.key(key,dir,ok);return;}
        if (mode == 17) {
            maps.key(key, dir, ok);
            return;
        }
        if (mode == 16) {
            net.key(key, dir, ok);
            return;
        }
        if (onlineBattle() && (mode == 1 || mode == 3 || mode == 5 || mode == 2)) {
            if (mode == 5) {
                net.results.key(key, dir, ok);
                return;
            }
            if (key == -7 || key == '0') {
                held = 0;
                popup.pause();
                return;
            }
            if (mode == 5)
                return;
            if (!isLocalTurn()) {
                message("Đến lượt bạn mới sử dụng được");
                return;
            }
            if (dir != 0) {
                if (isLocalTurn())
                    held |= dir;
                return;
            }
            if (key == 'b' || key == 'B') {
                net.command(3, 5);
                return;
            }
            if (key == 'v' || key == 'V') {
                net.command(4, 0);
                return;
            }
            if (key == 'f' || key == 'F') {
                int w = game.units[game.active].weapon;
                do {
                    w = nextWeapon(w, 1);
                } while ((owned & (1 << w)) == 0);
                net.command(5, w);
                return;
            }
            if (ok && isLocalTurn() && fireKey == 0) {
                fireKey = key;
                net.command(1, 0);
            }
            return;
        }
        if ((key == 'g' || key == 'G') && mode != 7 && mode != 9) {
            openCheats();
            return;
        }
        if (mode == 9) {
            if (key == -7) {
                mode = returnMode;
                return;
            }
            if (dir == 4)
                cheatSelection = (cheatSelection + cheatOptions() - 1) % cheatOptions();
            else if (dir == 8)
                cheatSelection = (cheatSelection + 1) % cheatOptions();
            else if (ok || key == -6)
                cheat();
            return;
        }
        if (mode == 8) {
            bag.key(key, dir, ok);
            return;
        }

        if (mode == 7) {
            if (key == 'n' || key == 'N') {
                editName();
                return;
            }
            if (dir == 1 || dir == 2)
                gender = 1 - gender;
            else if (dir == 4)
                createFocus = onlineAccount() ? 2 - createFocus : (createFocus + 2) % 3;
            else if (dir == 8)
                createFocus = onlineAccount() ? 2 - createFocus : (createFocus + 1) % 3;
            else if (ok || key == -6) {
                if (createFocus == 0)
                    createFocus = onlineAccount() ? 2 : 1;
                else if (createFocus == 1)
                    editName();
                else
                    enterCharacter();
            }
            return;
        }
        if (mode == 14) {
            if (ok && canFinishLoading()) {
                mode = 1;
                releaseLoadingArt();
                toast = "Đến lượt bạn!";
                toastTicks = 55;
            }
            return;
        }
        if (mode == 12) {
            if (key == -7) {
                mode = 10;
                return;
            }
            if (dir == 1)
                mapCursor = (mapCursor + 3) % 4;
            else if (dir == 2)
                mapCursor = (mapCursor + 1) % 4;
            else if (dir == 4 || dir == 8)
                mapCursor = (mapCursor + 2) % 4;
            else if (ok || key == -6)
                confirmRoomMap();
            return;
        }
        if (mode == 15) {
            if (net.social.offlineInvite) {
                net.social.popupKey(key, dir, ok);
                return;
            }
            if (key == -7) {
                mode = 10;
                return;
            }
            if (dir == 4 || dir == 1)
                botChoice = (botChoice + 7) % 8;
            else if (dir == 8 || dir == 2)
                botChoice = (botChoice + 1) % 8;
            else if (ok || key == -6)
                chooseRoomBot();
            return;
        }
        if (mode == 13) {
            if (ok || key == -7 || key == -6)
                mode = 10;
            return;
        }
        if (mode == 10) {
            if (key == -7) {
                if (onlineAccount())
                    net.open();
                else
                    openLobby();
                return;
            }
            if (key == 32) {
                toggleRoomControl();
                return;
            }
            if (dir != 0)
                loadout.move(dir, false);
            else if (ok || key == -6)
                roomActivate();
            return;
        }
        if (mode == 11) {
            if (ok || key == -7 || key == -6)
                mode = infoReturnMode;
            return;
        }
        if (mode == 0) {
            if (key == -7) {
                popup.confirmQuit();
                return;
            }
            if (key == 'i' || key == 'I' || key == 'b' || key == 'B') {
                openBag();
                return;
            }
            if (key == 's' || key == 'S') {
                openShop();
                return;
            }
            if (dir != 0)
                moveLobby(dir);
            else if (ok || key == -6)
                activateLobby();
            return;
        }
        if (mode == 6) {
            shop.key(key, dir, ok);
            return;
        }
        if (mode == 4) {
            mode = 1;
            return;
        }
        if (mode == 5) {
            if(offlineTest){if(key==-7||key=='0')openLobby();else if(ok||key==-6)startOfflineTest();return;}
            net.results.key(key, dir, ok);
            return;
        }
        if ((mode == 1 || mode == 3) && (key == 'b' || key == 'B')) {
            if (mode == 3)
                exitBuffs();
            useProp(5);
            return;
        }
        if (mode == 3) {
            if (key == -7) {
                exitBuffs();
                return;
            }
            if (dir == 4)
                propSelection = (propSelection + 4) % 5;
            else if (dir == 8)
                propSelection = (propSelection + 1) % 5;
            else if (ok)
                useProp(HUD_PROPS[propSelection]);
            return;
        }
        if (key == -7 || key == '0') {
            held = 0;
            game.charging = false;
            popup.pause();
            return;
        }
        if (key == -6) {
            toggleCamera();
            return;
        }
        if (dir != 0) {
            held |= dir;
            return;
        }
        if (ok) {
            if (fireKey == 0) {
                fireKey = key;
                game.charge();
            }
            return;
        }
        int lower = key >= 65 && key <= 90 ? key + 32 : key;
        if (lower == 'v')
            game.pass();
        else if (lower == 'f' && game.input()) {
            int next = game.units[game.active].weapon;
            do {
                next = nextWeapon(next, 1);
            } while ((owned & (1 << next)) == 0);
            ItemRules.weapon(game.units[game.active], next);
            equipped = next;
            saveSettings();
            message(WEAPON_NAMES[next]);
        }
    }

    void syncBotPow() {
        if (game.botPowQueued && !powShotPending) {
            startPowCutin();
            if (powCutinTicks > 0)
                powShotPending = true;
            else
                game.releaseBotPow();
        }
    }

    void tickPowSequence() {
        if (!powShotPending)
            return;
        if (powCutinTicks > 0)
            powCutinTicks--;
        if (powCutinTicks == 0) {
            powShotPending = false;
            powCutin = null;
            resetPowWorld();
            if (!isLocalTurn())
                game.releaseBotPow();
            else
                game.fire();
        }
    }

    void releaseShot() {
        if (onlineBattle()) {
            net.command(2, 0);
            return;
        }
        if (powShotPending)
            return;
        if (isLocalTurn() && game.pow && game.input()) {
            game.charging = false;
            held = fireKey = 0;
            startPowCutin();
            if (powCutinTicks > 0) {
                powShotPending = true;
                return;
            }
        }
        game.fire();
    }

    protected synchronized void keyReleased(int key) {
        if (keyBindings.capture >= 0) return;
        key = keyBindings.map(key);
        if (freeCamera) {
            cameraHeld &= ~direction(key);
            if (mode == 1)
                return;
        }
        if (onlineBattle()) {
            held &= ~direction(key);
            if (key == fireKey) {
                fireKey = 0;
                net.command(2, 0);
            }
            return;
        }
        if ((mode == 1 || mode == 3) && buffKey(key) >= 0)
            return;
        held &= ~direction(key);
        if (key == fireKey) {
            fireKey = 0;
            if (mode == 1 && game.charging)
                releaseShot();
            else
                game.charging = false;
        }
    }

    protected synchronized void keyRepeated(int key) {
        if (typing.active()) { if (key == 8 || key == -8 || key == 127) keyPressed(key); return; }
        if ((mode == 17 || mode == 16 && net.social.opened || mode == 0 && net.social.opened
                || net.chat.opened && net.chat.lobby())
                && direction(keyBindings.map(key)) != 0) {
            keyPressed(key);
            return;
        }
        if (mode == 3 && !(key >= '1' && key <= '5') && (direction(keyBindings.map(key)) == 4 || direction(keyBindings.map(key)) == 8))
            keyPressed(key);
    }

    void useProp(int id) {
        if (id == 5 && (game.pow || game.units[game.active].rage < 100)) {
            message(game.pow ? "POW đã kích hoạt" : "POW chưa đầy");
            return;
        }
        if (id == 4 && game.units[game.active].flyCooldown > 0) {
            message("Kỹ năng còn cần " + Math.min(2, game.units[game.active].flyCooldown) + " TURN mới sử dụng được");
            return;
        }
        if (game.units[game.active].energy < Battle.propCost(id)) {
            message("Không đủ thể lực để sử dụng");
            return;
        }
        if (onlineBattle()) {
            net.command(3, id);
            return;
        }
        if (isLocalTurn() && game.prop(id)) {
            if (id < 5) {
                propSelection = id;
                buffFlash = 45;
            }
            if (id == 5 && !game.pow) {
                powCutinTicks = 0;
                powCutin = null;
            }
            message(PROPS[id]);
        } else {
            message(id == 3 ? "Không thể hồi máu: HP đầy hoặc hết lượt hồi" : "Đạo cụ chưa thể sử dụng lúc này");
        }
    }

    boolean roomKitContext(){return !presentation.settings&&!net.social.opened&&!net.create.opened&&!net.chat.opened&&!net.inBattle&&(mode==10||mode==19||mode==16&&net.logged&&net.roomId>0&&net.phase==0&&!dungeon.picker&&!dungeon.rewardsOpen);}
    protected synchronized void pointerPressed(int px, int py) {
        int mappedX=pointerX(px,py),mappedY=pointerY(px,py);px=mappedX;py=mappedY;
        if(net.soloSearching()){int sx=(px-ox)/s,sy=(py-oy)/s;if(sx>=5&&sx<149){if(sy>=200&&sy<212)net.soloBot();else if(sy>=212&&sy<229)net.cancelSolo();}return;}
        if (typing.pointer((px-ox)/s,(py-oy)/s)) { repaint(); return; }
        if (mode == 1 && powShotPending)
            return;
        int x = (px - ox) / s, y = (py - oy) / s;
        if (popup.opened) {
            popup.pointer(x, y);
            return;
        }
        if(mode==16&&net.roomKind==3&&net.roomId>0&&dungeon.rewardsOpen&&!net.social.opened){dungeon.pointerRewards(x,y);return;}
        if(roomKitContext()&&loadout.groupPointer(x,y,mode!=10))return;
        if (presentation.pointer(x, y))
            return;
        if (mode == 0 && net.social.opened) {
            net.social.pointer(x, y);
            return;
        }
        if(mode==0&&ranking.opened){ranking.pointer(x,y);return;}
        if(!onlineAccount()&&!offlineTest&&mode!=16){net.loginForm(false);return;}
        if (net.chat.roomSpeech.pointer(x,y)) return;
        if (net.chat.pointer(x, y)) return;
        if (mode == 10 && x < 210 && y >= 48 && y < 154)
            y += 28;
        if (mode == 1 && x < 45 && y >= 183 && y < 198) {
            toggleCamera();
            return;
        }
        if (mode == 1 && freeCamera && !(x >= 248 && y < 9)) {
            if (x < 45 && y >= 198) {
                pressTouch = 2;
                touchDirection(x, y);
            }
            return;
        }
        if (mode == 1 && emotions.opened) {
            emotions.pointer(x, y);
            return;
        }
        if(mode==19){offlineDungeon.pointer(x,y);return;}if(mode==18){forge.pointer(x,y);return;}
        if (mode == 17) {
            maps.pointer(x, y);
            return;
        }
        if (mode == 16) {
            net.pointer(x, y);
            return;
        }
        if (mode == 7 && !onlineAccount() && x >= 235 && y >= 211) {
            net.open();
            return;
        }
        if (onlineBattle() && (mode == 1 || mode == 3 || mode == 5)) {
            if (mode == 5) {
                net.results.pointer(x, y);
                return;
            }
            if (x >= 248 && y < 9) {
                held = 0;
                popup.pause();
                return;
            }
            if (!isLocalTurn()) {
                message("Đến lượt bạn mới sử dụng được");
                return;
            }
            if (x >= 144 && x < 176 && y >= 62 && y < 76) {
                net.command(4, 0);
                return;
            }
            if (x >= 270 && x < 314 && y >= 188) {
                useProp(5);
                return;
            }
            if (x >= 296 && y >= 50 && y < 185) {
                useProp(HUD_PROPS[(y - 50) / 27]);
                return;
            }
            if (x >= 184 && x < 271 && y >= 170 && y < 194) {
                loadout.use((x - 184) / 29);
                return;
            }
            if (x >= 40 && x < 208 && y >= 211) {
                pressTouch = 1;
                net.command(1, 0);
            } else if (x < 45 && y > 195) {
                pressTouch = 2;
                touchDirection(x, y);
            }
            return;
        }
        if (mode == 9) {
            if(onlineAccount()){if(y>=59&&y<220&&x>=40&&x<280){cheatSelection=(y-59)/23;cheat();}return;}
            int options = cheatOptions();
            if (x >= 50 && x < 270 && y >= 78 && y < 78 + options * 30 && (y - 78) % 30 < 25) {
                cheatSelection = (y - 78) / 30;
                cheat();
            } else if (y >= (options == 5 ? 225 : 200))
                mode = returnMode;
            return;
        }
        if (mode == 8) {
            bag.pointer(x, y);
            return;
        }
        if (mode == 7) {
            if (y >= 102 && y < 132) {
                gender = x < 160 ? 0 : 1;
                createFocus = onlineAccount() ? 2 : 1;
            } else if (y >= 150 && y < 180 && !onlineAccount())
                editName();
            else if (y >= 201)
                enterCharacter();
            return;
        }
        if (mode == 6) {
            shop.pointer(x, y);
            return;
        }
        if (mode == 11) {
            mode = infoReturnMode;
            return;
        }
        if (mode == 0) {
            int hit = lobbyHit(x, y);
            if (hit >= 0) {
                lobbySelection = hit;
                pressTouch = 3;
            } else if (hallRoomHit(x, y)) {
                lobbySelection = 2;
                activateLobby();
            }
            return;
        }
        if (mode == 14) {
            if (canFinishLoading()){
                mode = 1;releaseLoadingArt();
            }
            return;
        }
        if (mode == 12) {
            if (y >= 62 && y < 179 && x >= 28 && x < 278) {
                mapCursor = (y >= 121 ? 2 : 0) + (x >= 168 ? 1 : 0);
                confirmRoomMap();
            } else if (y >= 184)
                confirmRoomMap();
            return;
        }
        if (mode == 15) {
            if (net.social.offlineInvite) {
                net.social.popupPointer(x, y);
                return;
            }
            if (y >= 46 && y < 199) {
                int choice = botChoice / 3 * 3 + (y - 46) / 51;
                if (choice < 8) {
                    botChoice = choice;
                    chooseRoomBot();
                }
            } else if (y >= 212)
                mode = 10;
            return;
        }
        if (mode == 13) {
            mode = 10;
            return;
        }
        if (mode == 10) {
            if (x >= 292 && y < 25) {
                roomSelection = 7;
                roomActivate();
                return;
            }
            if (loadout.pointer(x, y, false))
                return;
            int hit = x < 106 && y >= 76 && y < 182 ? 0 : x >= 108 && x < 209 && y >= 76 && y < 182 ? 1 : -1;
            if (hit == 6) {
                roomSelection = 6;
                setRoomControl(x < 65 ? 1 : 2);
            } else if (hit >= 0) {
                roomSelection = hit;
                roomActivate();
            }
            return;
        }

        if (mode == 4) {
            mode = 1;
            return;
        }
        if (mode == 5) {
            if(offlineTest){if(x>=80&&x<240&&y>=150&&y<175)startOfflineTest();else if(x>=80&&x<240&&y>=185&&y<210)openLobby();return;}
            net.results.pointer(x, y);
            return;
        }
        if (mode == 3) {
            if (x >= 295 && y >= 50 && y < 185) {
                propSelection = (y - 50) / 27;
                useProp(HUD_PROPS[propSelection]);
            } else
                exitBuffs();
            return;
        }
        if (x >= 144 && x < 176 && y >= 62 && y < 76) {
            game.pass();
            return;
        }
        if (x >= 248 && y < 9) {
            held = 0;
            game.charging = false;
            popup.pause();
            return;
        }
        if (x >= 270 && x < 314 && y >= 188 && y < 232) {
            useProp(5);
            return;
        }
        if (x < 45 && y >= 183 && y < 198) {
            toggleCamera();
            return;
        }
        if (x >= 296 && y >= 50 && y < 185) {
            useProp(HUD_PROPS[(y - 50) / 27]);
            return;
        }
        if (x >= 184 && x < 271 && y >= 170 && y < 194) {
            loadout.use((x - 184) / 29);
            return;
        }
        if (x >= 40 && x < 208 && y >= 211) {
            pressTouch = 1;
            game.charge();
        } else if (x < 40 && y > 195) {
            pressTouch = 2;
            touchDirection(x, y);
        }
    }

    void touchDirection(int x, int y) {
        int dx = x - 20, dy = y - 217;
        int direction = Math.abs(dx) > Math.abs(dy) ? (dx < 0 ? 1 : 2) : (dy < 0 ? 4 : 8);
        if (freeCamera)
            cameraHeld = direction;
        else
            held = direction;
    }

    protected synchronized void pointerDragged(int x, int y) {
        int mappedX=pointerX(x,y),mappedY=pointerY(x,y);x=mappedX;y=mappedY;
        if (mode == 16 && !net.logged && net.auth.footerTouch) {
            net.auth.footerDrag((x - ox) / s, (y - oy) / s);
            return;
        }
        if (loadout.toolbarTouch && (mode == 10 || mode == 16 || mode == 19)) {
            loadout.dragToolbar((x - ox) / s, (y - oy) / s);
            return;
        }
        if (mode == 16 && net.rooms.toolbarTouch) {
            net.rooms.drag((x - ox) / s, (y - oy) / s);
            return;
        }
        if (mode == 0 && pressTouch == 3) {
            int hit = lobbyHit((x - ox) / s, (y - oy) / s);
            if (hit >= 0)
                lobbySelection = hit;
            return;
        }
        if (pressTouch == 2)
            touchDirection((x - ox) / s, (y - oy) / s);
    }

    protected synchronized void pointerReleased(int x, int y) {
        int mappedX=pointerX(x,y),mappedY=pointerY(x,y);x=mappedX;y=mappedY;
        if (mode == 16 && !net.logged && net.auth.footerTouch) {
            net.auth.footerRelease((x - ox) / s, (y - oy) / s);
            return;
        }
        if (loadout.toolbarTouch && (mode == 10 || mode == 16 || mode == 19)) {
            loadout.releaseToolbar((x - ox) / s, (y - oy) / s);
            return;
        }
        if (mode == 16 && net.rooms.toolbarTouch) {
            net.rooms.release((x - ox) / s, (y - oy) / s);
            return;
        }
        if (mode == 0 && pressTouch == 3) {
            pressTouch = -1;
            if (lobbyHit((x - ox) / s, (y - oy) / s) == lobbySelection)
                activateLobby();
            return;
        }
        cameraHeld = held = 0;
        if (pressTouch == 1 && mode == 1 && (onlineBattle() || game.charging))
            releaseShot();
        pressTouch = -1;
    }
}
