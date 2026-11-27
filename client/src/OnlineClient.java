import java.io.*;
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.RecordStore;

/** Binary HTTP or persistent TCP transport works without WebSocket on MIDP. */
final class OnlineClient implements Runnable,CommandListener,ItemCommandListener {
    final Battle.Ball[] snapshotBalls=new Battle.Ball[20];
    final Battle.Blast[] snapshotBlasts=new Battle.Blast[16];
    final Battle.DamagePopup[] snapshotDamage=new Battle.DamagePopup[20];
    static final int MAGIC=0x474e5931;
    int forgeProgressExpected;
    boolean sulan7912Cap;boolean tribal797Cap;boolean clock793;int sentLoading=-1;long rewardDeadline,loadPercentPoll;final int[] loadingPercent=new int[6];
    String cheatVipKey="";boolean vipCap;int rewardWaitingMask;final int[] actorVip=new int[6],actorWing={-1,-1,-1,-1,-1,-1};
    boolean deckCap,deckRevealed;long deckPoll;boolean giftCap,giftExpCap;boolean deltaCap;boolean compactLobby;int lobbyVersion;boolean forgeChanceCap;int craftGrade=1,craftQuantity=1,craftMaterial;boolean craftLucky,craftProtect;
    final boolean[] actorWings=new boolean[6];boolean tribalCap;final boolean[] roomTribal=new boolean[32];
    boolean playerUiCap;int loadingMask,loadingLeaders;boolean shotFrames,prefetchCap;boolean flockCap,campaignCap,expansionCap,dungeonBrowser,homeAfterLogin,dungeonAfterLogin;int dungeonDifficulty,dungeonStage,dungeonKills,rewardPicks,stoneQuantity,forgeKind,forgeItem,forgeExpected;String economyKey="";final int[] roomDifficulties=new int[32],cardTypes=new int[18];boolean actorsCap,liveCap;int hostActor=-1;boolean progressCap;int progressRoom=-1;long progressPoll;final int[] actorExperience=new int[6];boolean bagCap;boolean adminCap;boolean eventCap;final TcpEventsClient events=new TcpEventsClient(this);final TcpTransport tcp=new TcpTransport();boolean emotionsCap,presenceCap;int onlineCount;long presencePoll;boolean characterCap,choosingCharacter;int genderDraft;int inviteSeat=-1;boolean equipmentCap;int equipKind,equipItem,exchangeAmount;String exchangeKey="";boolean arsenalCap;boolean shopCap;boolean cardsCap;final int[] roomKinds=new int[32],roomMaps=new int[32],roomPlayers=new int[32],roomSeats=new int[32];boolean itemsCap,emptyKitCap;final OnlineSocial social;final LobbyChat chat;boolean roomFixCap;int sentSocialTab,sentSocialGeneration,sentInfoTarget;long roomPoll;int closedSlots,slotDraft,emoteDraft;final int[] emotes=new int[6];boolean waitBotCap,queueTimedOut;int waitingSeconds,botVoteMask;long queueDeadline;int queueClockRoom;boolean matchmakingCap,matchQueued;boolean socialCap,resultsReady,cardsOpen;final String[] cardNames=new String[18];final int[] cardCoins=new int[18];final OnlineVisuals visuals=new OnlineVisuals();final OnlineChunks chunks=new OnlineChunks(this);boolean chunksCap;final OnlineReplay replay=new OnlineReplay(this);boolean replayCap;final DepartureNotice departure=new DepartureNotice(this);boolean repairGround,groundAwaiting;boolean departureCap,departureFinish;int departureVersion;final NetworkGuard guard=new NetworkGuard(this);volatile int networkGeneration;volatile long transportSince;final PhoneCanvas c;final ServerWake wake=new ServerWake(this);final OnlineMotion motion=new OnlineMotion(this);final OnlineCreate create;final OnlineResult results;final OnlineAuth auth;final OnlineRooms rooms;final OnlineWaitingRoom waiting;volatile String operation="";boolean autoLogin,startupTried;boolean asyncTurns,remember;String savedPassword="";boolean predicting,resolutionSeen;long processingSince;int predictedTurn=-1,sentMax,roomKind,createKind;boolean preciseControls,modernRooms,strictRoom,canStartRoom,readyDraft;int roomChoice=-1,roomSeconds=12,readyMask,mapDraft=-1,timeDraft=12,teamDraft;int sentHeld;Thread worker;volatile boolean alive=true,logged,inBattle,busy;volatile Connection connection;
    String endpoint="",webEndpoint="",email="",token="",password="",pending="",status="Chưa đăng nhập",registerName="";
    volatile String rewardOperation="";int rewardPick;
    int cardDraft;final int[] damage=new int[6],kills=new int[6];int roomId,phase,count=2,roomMap=-1,sequence,ack,revision,selection,roomCount,buyId,joinId,joinTeam,myActor,lastPow=-1;
    boolean host,settled,rewardRefreshed;long lastPoll,roomEnteredAt;volatile long lastBattleSync;final boolean[] roomLocked=new boolean[32];final int[] roomIds=new int[32],roomCounts=new int[32],roomPhases=new int[32],roomCapacities=new int[32];final String[] roomNames=new String[32];
    final int[] queueSeq=new int[256],queueKind=new int[256],queueArg=new int[256];int queueCount;boolean sequenceResync;
    final int[] wornMasks={3,3,3,3,3,3};final String[] names=new String[6];final int[] sex=new int[6],skin=new int[6],weapon=new int[6];final boolean[] custom=new boolean[6],controls=new boolean[6],present=new boolean[6],bots=new boolean[6];final int[][] look=new int[6][5];
    final Image[][] layers=new Image[6][5];final Image[] standing=new Image[6];final String[] standingKeys=new String[6];Image onlineFrame,onlineCard,onlineWaiting,onlineButton;
    String roomTitle="Thi đấu vui vẻ";String joinSecret="";String networkStage="";
    int offlineMoney,offlineMask,offlineGender;String offlineName;boolean offlineCustom,offlineTutorial,offlineCheatAim;final int[] offlineData=new int[11];
    void captureOffline(){if(offlineName!=null)return;offlineCheatAim=c.game.cheatAim;c.game.cheatAim=false;c.offlineExperience=c.experience;offlineMoney=c.shoppingMoney;offlineMask=c.wornMask;offlineGender=c.gender;offlineName=c.playerName;offlineCustom=c.customLook;offlineTutorial=c.tutorialDone;offlineData[0]=c.coins;offlineData[1]=c.wins;offlineData[2]=c.owned;offlineData[3]=c.equipped;offlineData[6]=c.gender;offlineData[5]=c.skin;for(int i=0;i<5;i++)offlineData[i+6]=c.look[i];}
    void restoreOffline(){if(offlineName!=null){c.experience=c.offlineExperience;c.serverRank=0;c.game.cheatAim=offlineCheatAim;c.playerName=offlineName;c.customLook=offlineCustom;c.tutorialDone=offlineTutorial;c.coins=offlineData[0];c.wins=offlineData[1];c.owned=offlineData[2];c.equipped=offlineData[3];c.gender=offlineGender;c.shoppingMoney=offlineMoney;c.wornMask=offlineMask;c.skin=offlineData[5];for(int i=0;i<5;i++)c.look[i]=offlineData[i+6];offlineName=null;}c.loadLook();}
    Form form;TextField joinPassword;TextField addressField,emailField,passwordField,confirmField,nameField,codeField;ChoiceGroup teamField;boolean registration;
    final Command submit=new Command("Đồng ý",Command.OK,1),cancel=new Command("Trở về",Command.BACK,2),signup=new Command("Đăng ký",Command.SCREEN,3),signin=new Command("Đăng nhập",Command.SCREEN,4),check=new Command("Kiểm tra kết nối",Command.SCREEN,5);
    OnlineClient(PhoneCanvas canvas){c=canvas;social=new OnlineSocial(this);chat=new LobbyChat(this);create=new OnlineCreate(this);results=new OnlineResult(this);auth=new OnlineAuth(this);rooms=new OnlineRooms(this);waiting=new OnlineWaitingRoom(this);String configured=c.app.getAppProperty("Gunny-Server-URL"),web=c.app.getAppProperty("Gunny-Web-URL");if(configured!=null)endpoint=configured.trim();if(web!=null)webEndpoint=web.trim();try{RecordStore rs=RecordStore.openRecordStore("gunny-online",true);if(rs.getNumRecords()>0){DataInputStream in=new DataInputStream(new ByteArrayInputStream(rs.getRecord(1)));String saved=in.readUTF();if(saved.trim().length()>0)endpoint=saved.trim();email=in.readUTF();if(in.available()>0){remember=in.readBoolean();if(remember)savedPassword=in.readUTF();if(in.available()>0)autoLogin=in.readBoolean();if(!remember)autoLogin=false;}in.close();}rs.closeRecordStore();}catch(Exception ignored){}if(endpoint.indexOf("gunny-online-main.onrender.com")>=0)endpoint="";if(webEndpoint.indexOf("gunny-online-main.onrender.com")>=0)webEndpoint="";endpoint=migrateEndpoint(endpoint);if(endpoint.length()==0)endpoint="socket://gunnyjava.xubi.org:22123";if(webEndpoint.length()==0)webEndpoint="https://gunnyjava.xubi.org";}
    boolean eligibleResult(){return c.game.winner<2&&(roomKind==3?c.game.winner==myActor%2:c.game.winner==myActor%2||rewardPicks>0||damage[myActor]>0);}
    boolean localTurn(){return inBattle&&c.game.active<count&&controls[c.game.active];}
    synchronized boolean request(String op){if(c.offlineTest)return false;if(logged&&(op.equals("tribalcreate")||op.equals("dungeoncreate")||op.equals("dungeonquick")||op.equals("dungeonteam"))&&!c.dungeon.levelAllowed(op.equals("tribalcreate")?1:0))return false;if((op.equals("tribalcreate")||op.equals("tribalresume"))&&(!tribal797Cap||!sulan7912Cap)){c.message("Cập nhật máy chủ 7.9.18 để chơi Tà Thần mới.");return false;}guard.start();if(wake.waiting||pending.length()>0||operation.equals("logout")||busy&&(wake.cancelled||op.equals("login")||op.equals("register"))){c.message("Đang kết nối, chờ một chút.");return false;}wake.cancelled=false;pending=op;notifyAll();status="Đang kết nối...";if(worker==null||!worker.isAlive()){worker=new Thread(this);worker.start();}return true;}
    boolean receivingReward(){return rewardOperation.equals("claim")||rewardOperation.equals("claim780")||operation.equals("claim")||operation.equals("claim780")||pending.equals("claim")||pending.equals("claim780");}
    synchronized boolean rewardRequest(String op,int card){
        if(rewardOperation.length()>0||receivingReward()||operation.equals("cardsready")||pending.equals("cardsready")||!logged||!inBattle)return false;
        cardDraft=card;rewardPick=rewardPicks;rewardOperation=op;notifyAll();
        if(worker==null||!worker.isAlive()){worker=new Thread(this);worker.start();}return true;
    }
    synchronized void command(int kind,int arg){if(phase!=1||sequenceResync||chunks.frames!=null||chunks.ownPending||chunks.waitingForNext||repairGround||groundAwaiting||departure.paused||replay.playing&&!replay.own||!localTurn()||queueCount>=254||c.game.pendingNext||SulanRules.active(c.game)&&c.powCutinTicks>0)return;
        if(asyncTurns){beginPrediction();Battle b=c.game;if(!b.input())return;if(kind==1)b.charge();else if(kind==2){Battle.Unit u=b.units[b.active];arg=-(1+u.angle*128+u.power);if(SulanRules.active(b)&&chunksCap){
            if(!b.charging)return;
            // HF38: aim/charge are predicted; flight/POW/transform are server playback only.
            b.charging=false;b.pendingNext=true;resolutionSeen=false;
            c.powShotPending=false;c.powCutinTicks=0;
        }else if(b.pow){b.charging=false;c.startPowCutin();c.powShotPending=true;lastPow=b.turn;}else b.fire();processingSince=System.currentTimeMillis();}else if(kind==8){if(!itemsCap||!b.item(arg)){c.message("Chưa dùng được: kiểm tra thể lực / lượt dùng.");return;}}else if(kind==3)b.prop(arg);else if(kind==4){b.pass();processingSince=System.currentTimeMillis();}else if(kind==5)ItemRules.weapon(b.units[b.active],arg);}
        else arg=motion.input(kind,arg,sequence+1);
        queueSeq[queueCount]=++sequence;queueKind[queueCount]=kind;queueArg[queueCount++]=arg;lastPoll=0;notifyAll();
    }
    void beginPrediction(){if(predicting)return;predicting=true;predictedTurn=c.game.turn;c.game.deferNext=true;}
    synchronized boolean frame(int held){if(queueCount>0&&queueKind[queueCount-1]==6&&queueSeq[queueCount-1]>sentMax&&(queueArg[queueCount-1]&15)==held&&queueArg[queueCount-1]<1400){queueArg[queueCount-1]+=16;return true;}if(queueCount>=254)return false;queueSeq[queueCount]=++sequence;queueKind[queueCount]=6;queueArg[queueCount++]=16+held;notifyAll();return true;}
    void abortNetwork(long expected,String reason){Connection old;synchronized(c){synchronized(this){if(expected!=0&&(!busy||transportSince!=expected))return;networkGeneration++;old=connection;connection=null;worker=null;busy=false;transportSince=0;pending=operation="";password="";wake.cancelled=true;wake.waiting=false;}tcp.close();events.cancel();social.cancelRequests();chat.reset();logged=inBattle=predicting=false;chunks.reset();replay.reset();motion.reset();results.reset();c.releaseBattleArt();roomId=queueCount=revision=0;token="";c.held=c.fireKey=0;wake.invalidate();restoreOffline();c.presentation.settings=false;c.ranking.opened=false;loginForm(false);sequenceResync=checkpointRequested=false;snapshotRecoveryTurn=-1;recoveryAttempts=0;lobbyVersion=0;chunks.recovering=false;c.popup.notice("Mất kết nối",reason);}NetworkGuard.closeLater(old);}
    void disconnect(String reason){endSession("Mất kết nối",reason+"\nHãy đăng nhập lại.");}
    void endSession(String heading,String reason){
        rewardOperation="";events.cancel();tcp.close();
        synchronized(c){
            social.cancelRequests();chat.reset();logged=inBattle=predicting=resolutionSeen=repairGround=groundAwaiting=false;
            sequenceResync=checkpointRequested=false;snapshotRecoveryTurn=-1;recoveryAttempts=0;
            chunks.reset();chunks.recovering=false;chunks.recoveryFailures=0;replay.reset();motion.reset();results.reset();
            soloQueue.clear();c.dungeon.closeRewards();c.releaseBattleArt();roomId=phase=revision=lobbyVersion=0;resetCommandQueue(0);
            token="";pending="";c.held=c.fireKey=0;c.powShotPending=false;c.powCutinTicks=0;c.game.charging=false;
            c.resetPending=c.loadingRequested=false;departure.paused=false;processingSince=0;predictedTurn=-1;
            homeAfterLogin=dungeonAfterLogin=dungeonBrowser=false;startupTried=true;
            wake.invalidate();restoreOffline();c.presentation.settings=false;c.ranking.opened=false;
            c.popup.opened=false;c.popup.resumeOffline();loginForm(false);status=reason;c.popup.notice(heading,reason);
        }
    }
    // HF27 diagnostic: sampled by the HUD, no new network fields.
    volatile long diagSentAt,diagReplyAt,diagAppliedAt;
    volatile int diagPhase,diagRoom=-1,diagReqTurn=-1,diagReqCompleted=-1,diagReqCached=-1,diagServerTurn=-1,diagStage=-1,diagSegment=-1,diagSyncCount,diagFailures;
    volatile boolean diagReqAck,diagServerDone;
    volatile String diagError="";
    void diagRequest(int turn,int completed,int cached,boolean ack){if(diagRoom!=roomId){diagRoom=roomId;diagReplyAt=diagAppliedAt=0;diagSyncCount=diagFailures=0;diagServerTurn=diagStage=diagSegment=-1;diagError="";}diagReqTurn=turn;diagReqCompleted=completed;diagReqCached=cached;diagReqAck=ack;diagSentAt=System.currentTimeMillis();diagPhase=1;}
    boolean checkpointCap,checkpointRequested;int snapshotRecoveryTurn=-1,recoveryAttempts;
    static final class LobbyCacheMismatch extends IOException {LobbyCacheMismatch(){super("Sai cache phòng.");}}
    static final class SessionEnded extends ServerReply {SessionEnded(String message){super(message);}}
    void beginRoomRecovery(int knownAck){
        synchronized(c){if(knownAck>=0)resetCommandQueue(knownAck);lobbyVersion=0;lastPoll=lastBattleSync=0;
            if(inBattle){resyncCommands();checkpointRequested=checkpointCap&&chunksCap;chunks.recovering=chunksCap;}
            c.held=c.fireKey=0;c.message("Đang đồng bộ lại trận đấu...");
        }
    }

    boolean soloWaiting(){return logged&&roomId>0&&roomKind==1&&phase==0;}
    boolean soloSearching(){return c.mode==16&&soloWaiting();}
    final SoloQueueControl soloQueue=new SoloQueueControl();
    synchronized void cancelSolo(){if(soloWaiting()){soloQueue.request(roomId,"cancelquick");notifyAll();}}
    synchronized void soloBot(){if(soloWaiting()&&!operation.equals("cancelquick")&&!operation.equals("botvote")){soloQueue.request(roomId,"botvote");notifyAll();}}
    synchronized String takeSoloAction(long now){return soloWaiting()?soloQueue.next(roomId,now):"";}
    void quick(int kind){if(kind==1&&c.requireWeapon())return;if(kind==2&&matchmakingCap){create.team=true;create.open();return;}departure.shown=0;departure.paused=false;if(!asyncTurns){c.message("Cập nhật server 6.3.0 trước.");return;}c.resetPending=c.loadingRequested=false;c.held=c.fireKey=0;createKind=kind;if(kind==1){soloQueue.clear();chat.leaveWaitingRoom();social.opened=false;c.popup.opened=false;c.loadout.resetFocus();}request("quick");}
    synchronized boolean createCustom(){
        if(roomId>0||enteringRoom(operation)||enteringRoom(pending))return false;
        if(pending.equals("rooms")||pending.equals("dungeonrooms"))pending="";
        departure.shown=0;departure.paused=false;c.resetPending=c.loadingRequested=false;
        c.held=c.fireKey=0;createKind=0;
        if(create.dungeon){dungeonDifficulty=0;return request(create.dungeonId==1?"tribalcreate":"dungeoncreate");}
        return request(create.team&&matchmakingCap?"createteam":asyncTurns?"create3":"create2");
    }
    void startupLogin(){if(presentationPending()||startupTried)return;startupTried=true;if(!logged)loginForm(false);}
    boolean presentationPending(){return c.presentation.boot<2;}

    boolean dungeonListLoading;
    void openDungeon(){
        if(!logged){
            dungeonAfterLogin=true;
            loginForm(false);
            return;
        }
        if(!flockCap){
            c.popup.notice(DungeonScreen.TITLE,"Cập nhật máy chủ để chơi Huyệt Ma Kiến.");
            return;
        }
        if(homeAfterLogin){
            dungeonAfterLogin=true;
            c.message("Đang tải hồ sơ. Xong sẽ mở Ải Viễn Chinh.");
            return;
        }
        openBrowser(true);
    }
    void open(){dungeonAfterLogin=false;if(logged)openBrowser(false);else{dungeonBrowser=false;loginForm(false);}}
    void openBrowser(boolean dungeon){
        dungeonBrowser=dungeon;
        dungeonListLoading=dungeon;
        roomCount=selection=0;
        rooms.page=0;
        c.dungeon.page=0;
        create.opened=false;
        c.dungeon.focus=0;
        roomPoll=0;
        c.mode=16;
        if(pending.length()==0 && !operation.equals("logout"))request(dungeon?"dungeonrooms":"rooms");
    }
    private void finishLogin(){
        homeAfterLogin=false;
        boolean enterDungeon=dungeonAfterLogin;
        dungeonAfterLogin=false;
        if(enterDungeon)openDungeon();
        else {c.openLobby();if(vipCap||tribalCap)synchronized(this){pending=vipCap?"resultresume":"tribalresume";}}
    }
    void returnHome(){
        if(roomId>0||enteringRoom(operation)||enteringRoom(pending)){
            c.message("Đang xử lý, chờ một chút.");
            return;
        }
        dungeonBrowser=false;
        dungeonListLoading=false;
        c.openLobby();
    }
    private boolean enteringRoom(String op){
        return op.equals("join")||op.startsWith("create")||op.equals("dungeoncreate")
                ||op.equals("dungeonquick")||op.equals("dungeonteam")||op.equals("quick")||op.equals("inviteanswer");
    }
    synchronized void createRoom(boolean team){if(!logged){loginForm(false);return;}if(roomId>0||operation.startsWith("create")||pending.startsWith("create"))return;c.resetPending=c.loadingRequested=false;c.held=c.fireKey=0;c.game.charging=false;createKind=team?2:0;c.mode=16;selection=0;if(pending.equals("rooms"))pending="";request(modernRooms?"create2":"create");}
    void authDraft(){if(addressField!=null)endpoint=addressField.getString().trim();if(emailField!=null)email=emailField.getString().trim();}
    void authButton(String label,Command command){StringItem item=new StringItem("",label,Item.BUTTON);item.setDefaultCommand(command);item.setItemCommandListener(this);form.append(item);}
    void loginForm(boolean register){prepareAuth(register);Display.getDisplay(c.app).setCurrent(c);}
    void prepareAuth(boolean register){codeField=null;c.mode=16;registration=register;confirmField=null;form=new Form(register?"ĐĂNG KÝ GUNNY":"ĐĂNG NHẬP GUNNY");addressField=new TextField("Máy chủ TCP",endpoint,180,TextField.URL);emailField=new TextField("Email / tên đăng nhập",email,254,TextField.ANY);passwordField=new TextField("Mật khẩu",!register&&remember?savedPassword:"",128,TextField.PASSWORD);if(register){nameField=new TextField("Tên nhân vật",c.playerName,13,TextField.ANY);form.append(nameField);}form.append(emailField);form.append(passwordField);if(register){confirmField=new TextField("Nhập lại mật khẩu",null,128,TextField.PASSWORD);form.append(confirmField);}authButton(register?"TẠO TÀI KHOẢN":"ĐĂNG NHẬP",submit);authButton(register?"Đã có tài khoản? Đăng nhập":"ĐĂNG KÝ TÀI KHOẢN MỚI",register?signin:signup);form.append(register?"Mật khẩu từ 8 ký tự. Đăng ký xong game sẽ thử đăng nhập tự động.":"Chỉ lưu mật khẩu trên máy khi bạn đánh dấu Lưu mật khẩu.");form.addCommand(submit);form.addCommand(cancel);if(!register)form.addCommand(signup);form.setCommandListener(this);auth.reset();}
    public void commandAction(Command command,Item item){commandAction(command,(Displayable)form);}
    void signupFollowup(String message){status="Đã gửi yêu cầu đăng ký";prepareAuth(false);Alert notice=new Alert("Đăng ký Gunny","Yêu cầu đăng ký đã gửi. "+message+"\n\nKhông cần xác nhận email. Nếu đã có tài khoản, dùng mật khẩu cũ để đăng nhập.",null,AlertType.INFO);notice.setTimeout(Alert.FOREVER);Display.getDisplay(c.app).setCurrent(notice,c);}
    void joinForm(){
        if(c.requireWeapon())return;
        if(!matchmakingCap){
            c.typing.open("Chọn phe: 1 Đỏ, 2 Xanh","2",1,true,false,new InlineText.Done(){
                public void accept(String value){if(value.equals("1")||value.equals("2"))askJoinPassword(value.equals("1")?0:1);}
            });
        }else askJoinPassword(2);
    }
    private void askJoinPassword(final int team){
        if(!asyncTurns){finishJoin(team,"");return;}
        c.typing.open("Mật mã phòng (nếu có)","",16,false,true,new InlineText.Done(){
            public void accept(String value){finishJoin(team,value);}
        });
    }
    private void finishJoin(int team,String secret){
        if(c.requireWeapon())return;
        joinTeam=team;
        joinSecret=secret;
        request("join");
    }
    public void commandAction(Command command,Displayable screen){if(command==signup||command==signin){authDraft();loginForm(command==signup);return;}if(command==cancel){form=null;Display.getDisplay(c.app).setCurrent(c);return;}if(busy||pending.length()>0){Alert wait=new Alert("Đang kết nối","Chờ yêu cầu hiện tại hoàn tất rồi thử lại.",null,AlertType.INFO);wait.setTimeout(2500);Display.getDisplay(c.app).setCurrent(wait,screen==null?c:screen);return;}try{
            if(command==check){authDraft();validateEndpoint(endpoint);saveConfig();request("health");form=null;Display.getDisplay(c.app).setCurrent(c);c.mode=16;return;}
            if(codeField!=null&&form!=null&&screen==form&&form.getTitle().equals("VÀO PHÒNG")){joinId=Integer.parseInt(codeField.getString());joinTeam=matchmakingCap?2:teamField.getSelectedIndex()==0?1:0;joinSecret=joinPassword.getString();codeField=null;request("join");}
            else{endpoint=addressField.getString().trim();validateEndpoint(endpoint);email=emailField.getString().trim();password=passwordField.getString();registerName=registration?nameField.getString().trim():"";if(email.length()==0||password.length()<8)throw new IOException("Nhập email và mật khẩu từ 8 ký tự.");if(registration&&(confirmField==null||!password.equals(confirmField.getString())))throw new IOException("Hai lần nhập mật khẩu chưa khớp.");if(registration&&(registerName.length()==0||registerName.length()>13))throw new IOException("Tên nhân vật từ 1 đến 13 ký tự.");savedPassword=remember?password:"";saveConfig();request(registration?"register":"login");passwordField.setString("");if(confirmField!=null)confirmField.setString("");}
            form=null;Display.getDisplay(c.app).setCurrent(c);c.mode=16;
        }catch(Exception e){password="";Alert a=new Alert("Gunny Online",e.getMessage(),null,AlertType.ERROR);a.setTimeout(4000);Display.getDisplay(c.app).setCurrent(a,codeField==null?c:screen);}}
    boolean tcpEndpoint(){return endpoint.startsWith("socket://");}
    static String migrateEndpoint(String url){if(url.equals("socket://gunnyjava.xubi.org:22122")||url.equals("socket://192.168.1.28:22122"))url=url.substring(0,url.length()-5)+"22123";if(url.startsWith("https://")||url.startsWith("http://")){int start=url.indexOf("://")+3,end=url.indexOf('/',start);String host=end<0?url.substring(start):url.substring(start,end);int port=host.indexOf(':');if(port>=0)host=host.substring(0,port);return "socket://"+host+":22123";}return url;}
    static void validateEndpoint(String url)throws IOException{if(!url.startsWith("socket://")||url.indexOf('/',9)>=0||url.indexOf('@')>=0||url.indexOf('?')>=0||url.indexOf('#')>=0)throw new IOException("Dùng socket://TEN-MIEN:22123 cho game.");int split=url.lastIndexOf(':');if(split<=9)throw new IOException("Thiếu tên miền hoặc cổng TCP.");try{int port=Integer.parseInt(url.substring(split+1));if(port<1||port>65535)throw new Exception();}catch(Exception error){throw new IOException("Cổng TCP từ 1 đến 65535.");}}
    void saveConfig(){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeUTF(endpoint);out.writeUTF(email);out.writeBoolean(remember);if(remember)out.writeUTF(savedPassword);out.writeBoolean(autoLogin&&remember);out.close();RecordStore rs=RecordStore.openRecordStore("gunny-online",true);byte[] data=bytes.toByteArray();if(rs.getNumRecords()==0)rs.addRecord(data,0,data.length);else rs.setRecord(1,data,0,data.length);rs.closeRecordStore();}catch(Exception ignored){}}
    byte[] body(String op)throws IOException {ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(MAGIC);out.writeUTF(op);
        if(op.equals("baglayout")){out.writeShort(175);for(int i=0;i<175;i++)out.writeShort(c.bagLayout.online[i]);}
        else if(op.equals("loadpct")){out.writeByte(Math.min(100,c.loadTicks*100/60));}
        else if(op.equals("equip")){out.writeByte(equipKind);out.writeByte(equipItem);}
        else if(op.equals("exchange")){out.writeInt(exchangeAmount);out.writeUTF(exchangeKey);}
        else if(op.equals("checkout")){out.writeByte(c.gender);for(int k=0;k<7;k++)out.writeShort(c.shop.sentCart[k]);}
        else if(op.equals("findfriend")){sentSocialGeneration=social.generation;out.writeUTF(social.searchQuery);}else if(op.equals("battleemote"))out.writeByte(c.emotions.draft);else if(op.equals("gender"))out.writeByte(genderDraft);else if(op.equals("slot"))out.writeByte(slotDraft);else if(op.equals("emote"))out.writeByte(emoteDraft);else if(op.equals("social")){sentSocialTab=social.tab;sentSocialGeneration=social.generation;out.writeByte(sentSocialTab);}else if(op.equals("friend")){out.writeInt(social.target);out.writeByte(social.action);}else if(op.equals("invite")){out.writeInt(social.target);out.writeByte(inviteSeat);}else if(op.equals("inviteanswer"))out.writeBoolean(social.answer);else if(op.equals("allyai")){out.writeByte(social.bot);out.writeByte(inviteSeat);}else if(op.equals("login")||op.equals("register")){out.writeUTF(email);out.writeUTF(password);if(op.equals("register"))out.writeUTF(registerName);}
        else if(op.equals("quick")){out.writeByte(createKind);}
        else if(op.equals("create")||op.equals("create2")||op.equals("create3")||op.equals("createteam")){out.writeByte(-1);out.writeByte(createKind==2?2:0);out.writeByte(-1);if(!op.equals("create"))out.writeByte(12);if(op.equals("create3")||op.equals("createteam")){out.writeUTF(create.name);out.writeUTF(create.secret);}}
        else if(op.equals("join")){out.writeInt(joinId);out.writeByte(joinTeam);if(asyncTurns)out.writeUTF(joinSecret);}
        else if(op.equals("roomsettings")){out.writeByte(mapDraft);out.writeByte(timeDraft);}
        else if(op.equals("vipuse")||op.equals("vipbuy")||op.equals("vipequip")){out.writeByte(c.vip.item);out.writeUTF(c.vip.requestKey);}else if(op.equals("buystones")){out.writeShort(stoneQuantity);out.writeUTF(economyKey);}else if(op.equals("forge")){out.writeByte(c.gender);out.writeByte(forgeKind);out.writeByte(forgeItem);out.writeByte(forgeExpected);out.writeShort(forgeProgressExpected);out.writeUTF(economyKey);}else if(op.equals("claim780")){out.writeByte(rewardPick);out.writeByte(cardDraft);}else if(op.equals("tribalcreate")){out.writeByte(dungeonDifficulty);out.writeUTF(create.name);out.writeUTF(create.locked?create.secret:"");}
        else if(op.equals("tribalwing")){out.writeBoolean(!c.economy.wingEquipped);}
        else if(op.equals("dungeoncreate")||op.equals("dungeonquick")){out.writeByte(dungeonDifficulty);if(op.equals("dungeoncreate")){out.writeUTF(create.name);out.writeUTF(create.locked?create.secret:"");}}else if(op.equals("dungeonsettings")){out.writeByte(dungeonDifficulty);if(tribalCap)out.writeByte(c.dungeon.draftDungeon);}
        else if((op.equals("giftopen")||op.equals("giftopenexp"))){out.writeByte(c.gifts.requestType);out.writeUTF(c.gifts.requestKey);}
        else if(op.equals("testcheat")){out.writeByte(c.cheatSelection);out.writeBoolean(!c.economy.testGod);if(c.cheatSelection==6){if(cheatVipKey.length()==0)cheatVipKey=c.nextEconomyKey();out.writeUTF(cheatVipKey);}}
        else if(op.equals("buycraft")){out.writeByte(craftMaterial);out.writeShort(stoneQuantity);out.writeUTF(economyKey);}
        else if(op.equals("forgechance")){out.writeByte(c.gender);out.writeByte(forgeKind);out.writeByte(forgeItem);out.writeByte(forgeExpected);out.writeByte(craftGrade);out.writeByte(craftQuantity);out.writeBoolean(craftLucky);out.writeBoolean(craftProtect);out.writeUTF(economyKey);}
        else if(op.startsWith("chat"))chat.write(op,out);
        else if(op.equals("playerinfo")){sentInfoTarget=social.target;out.writeInt(sentInfoTarget);out.writeByte(1);}
        else if(op.equals("departure")){out.writeInt(departureVersion);out.writeBoolean(departureFinish);}
        else if(op.equals("ready"))out.writeBoolean(readyDraft);
        else if(op.equals("switchteam"))out.writeByte(teamDraft);
        else if(op.equals("claim"))out.writeByte(cardDraft);
        else if(op.equals("buy"))out.writeByte(buyId);
        else if(op.equals("appearance")){out.writeUTF(c.playerName);out.writeByte(c.gender);out.writeByte(c.skin);out.writeBoolean(c.customLook);for(int i=0;i<5;i++)out.writeByte(c.look[i]);if(equipmentCap)out.writeByte(c.wornMask);}
        else if(op.equals("sync")){if(repairGround&&inBattle){synchronized(c){revision=0;predicting=false;motion.reset();repairGround=false;groundAwaiting=true;}}out.writeInt(revision);sentHeld=inBattle?c.held&15:0;out.writeByte(sentHeld);out.writeBoolean(inBattle&&c.loadTicks>=60);if(asyncTurns){int requestTurn=chunksCap?chunks.requestTurn(c.game.turn):c.game.turn;out.writeInt(requestTurn);out.writeBoolean(resolutionSeen);if(resolutionSeen)hf38AckDue=false;if(departureCap)out.writeInt(departure.version);if(replayCap)out.writeInt(replay.turn);if(chunksCap){out.writeInt(chunks.completed);if(checkpointCap&&chunks.recovering)checkpointRequested=true;int requestCached=checkpointRequested?Integer.MIN_VALUE+1:chunks.cached();out.writeInt(requestCached);if(SulanRules.active(c.game))diagRequest(requestTurn,chunks.completed,requestCached,resolutionSeen);}}synchronized(this){out.writeByte(Math.min(16,queueCount));for(int i=0;i<queueCount&&i<16;i++){out.writeInt(queueSeq[i]);out.writeByte(queueKind[i]);out.writeShort(queueArg[i]);sentMax=queueSeq[i];}}if(compactLobby)out.writeInt(lobbyVersion);}
        out.close();return bytes.toByteArray();}
    static final class SequenceRecovery extends ServerReply {SequenceRecovery(){super("Đồng bộ lại thứ tự lệnh.");}}
    static class ServerReply extends IOException {ServerReply(String message){super(message);}}
    DataInputStream post(String op)throws IOException {
        if(!logged)wake.ensureReady();
        try{DataInputStream reply=postOnce(op);wake.healthy();return reply;}
        catch(IOException error){
            if(error instanceof ServerWake.Cancelled||error instanceof ServerReply||error instanceof javax.microedition.pki.CertificateException)throw error;
            if(logged&&(inBattle&&(op.equals("sync")||op.equals("battleemotes"))||op.equals("progress")||soloWaiting()&&(op.equals("sync")||op.equals("cancelquick")||op.equals("botvote")))){
                // Commands in sync have sequence IDs: retry cannot apply POW/fire twice.
                // Some Symbian HTTP stacks reject parallel /wait and /api sockets.
                events.failed=true;events.cancel();
                try{Thread.sleep(300);}catch(InterruptedException ignored){}
                DataInputStream reply=postOnce(op);wake.healthy();return reply;
            }
            wake.invalidate();
            if(!logged&&(op.equals("rooms")||op.equals("profile")||op.equals("sync"))){
                wake.ensureReady();DataInputStream reply=postOnce(op);wake.healthy();return reply;
            }
            throw error;
        }
    }
    int tcpFlags(){int f=(sulan7912Cap?1073741824:0)|536870912|(clock793?268435456:0)|(vipCap?134217728:0)|(deltaCap?67108864:0)|(tribalCap?33554432:0)|(playerUiCap?16777216:0)|(shotFrames?4194304:0)|(prefetchCap?8388608:0)|(flockCap?1048576:0)|(campaignCap?524288:0)|262144|131072|65536|1|2;if(bagCap)f|=4;if(equipmentCap)f|=8;if(arsenalCap)f|=16;if(cardsCap)f|=32;if(itemsCap)f|=64;if(emptyKitCap)f|=Integer.MIN_VALUE;if(roomFixCap)f|=128;if(waitBotCap)f|=256;if(matchmakingCap)f|=512;if(socialCap)f|=1024;if(chunksCap)f|=2048;if(replayCap)f|=4096;if(modernRooms)f|=8192;if(asyncTurns)f|=16384;if(departureCap)f|=32768;if(chat.expandedEmoji)f|=2097152;return f;}
    DataInputStream decodeReply(byte[] reply,int generation)throws IOException {if(generation!=networkGeneration)throw new ServerWake.Cancelled();DataInputStream data=new DataInputStream(new ByteArrayInputStream(reply));int code=data.readUnsignedByte();if(code!=0){String message=data.readUTF();if(code==4){int serverAck=data.readInt();synchronized(c){resetCommandQueue(serverAck);if(inBattle)resyncCommands();}throw new SequenceRecovery();}if(code==3){synchronized(c){soloQueue.clear();inBattle=false;chunks.reset();replay.reset();motion.reset();results.reset();c.releaseBattleArt();roomId=0;resetCommandQueue(ack);sequenceResync=checkpointRequested=false;snapshotRecoveryTurn=-1;chunks.recovering=false;revision=0;c.mode=16;c.held=c.fireKey=0;synchronized(this){pending="rooms";}}}if(code==2)throw new SessionEnded(message);throw new ServerReply(message);}return data;}
    DataInputStream postTcp(String op,int generation,byte[] request)throws IOException {int[] kit;synchronized(c){kit=new int[3];System.arraycopy(c.loadout.onlineKit,0,kit,0,3);}networkStage="Mở kết nối TCP";tcp.ensure(endpoint);connection=tcp.connection;networkStage="Gửi yêu cầu TCP";byte[] reply=tcp.request(endpoint,1,tcpFlags(),logged?token:"",kit,request);networkStage="Nhận phản hồi TCP";return decodeReply(reply,generation);}
    DataInputStream postOnce(String op)throws IOException {if(!op.equals("sync"))lobbyVersion=0;if(Thread.currentThread()!=worker)throw new ServerWake.Cancelled();validateEndpoint(endpoint);byte[] request;if(op.equals("sync")){synchronized(c){request=body(op);}}else request=body(op);return postTcp(op,networkGeneration,request);}
    static String failureDetails(Exception error,String stage){String message=error.getMessage();return stage+"\n"+error.getClass().getName()+(message==null?"":"\n"+message)+(error instanceof SecurityException?"\nCho phép game truy cập mạng trong Quản lý ứng dụng.":"");}
    void capabilities(String content)throws IOException {checkpointCap=content.indexOf("\"checkpointHF13\":1")>=0;compactLobby=content.indexOf("\"compactlobby\":1")>=0;lobbyVersion=0;sulan7912Cap=content.indexOf("\"sulan7912\":1")>=0;tribal797Cap=content.indexOf("\"tribal797\":1")>=0;clock793=content.indexOf("\"clock793\":1")>=0;vipCap=content.indexOf("\"vip\":1")>=0;deckCap=content.indexOf("\"deck\":1")>=0;giftCap=content.indexOf("\"gifts\":1")>=0;giftExpCap=content.indexOf("\"giftExpHF14\":1")>=0;deltaCap=content.indexOf("\"delta\":1")>=0;tribalCap=content.indexOf("\"tribal\":1")>=0;forgeChanceCap=content.indexOf("\"forgechance\":1")>=0;playerUiCap=content.indexOf("\"playerui\":1")>=0;shotFrames=content.indexOf("\"shotframes\":1")>=0;prefetchCap=shotFrames&&content.indexOf("\"prefetch\":1")>=0;chat.roomSpeech.supported=content.indexOf("\"roomchat\":1")>=0;chat.expandedEmoji=content.indexOf("\"chatemoji\":1")>=0;chat.available=content.indexOf("\"lobbychat\":1")>=0;flockCap=content.indexOf("\"flock\":1")>=0;campaignCap=content.indexOf("\"campaign\":1")>=0;expansionCap=content.indexOf("\"expansion\":1")>=0;if(!expansionCap)throw new ServerWake.Rejected("Cập nhật máy chủ 7.8.0 để dùng tiệm rèn và ải viễn chinh.");actorsCap=content.indexOf("\"actors\":1")>=0;liveCap=!shotFrames&&content.indexOf("\"live\":1")>=0;if(!actorsCap||(!liveCap&&!shotFrames)||content.indexOf("\"growth\":1")<0)throw new ServerWake.Rejected("Cập nhật máy chủ 7.7.1 để đồng bộ cấp và trận đấu TCP trực tiếp.");progressCap=content.indexOf("\"progress\":1")>=0;bagCap=content.indexOf("\"bag\":1")>=0;adminCap=content.indexOf("\"admin\":1")>=0;eventCap=content.indexOf("\"events\":1")>=0;if(!eventCap||content.indexOf("\"charge\":1")<0)throw new ServerWake.Rejected("Cập nhật máy chủ 7.7.1 để dùng TCP và luật lấy lực mới.");emotionsCap=content.indexOf("\"emotions\":1")>=0;presenceCap=content.indexOf("\"presence\":1")>=0;characterCap=content.indexOf("\"character\":1")>=0;equipmentCap=content.indexOf("\"equipment\":1")>=0;arsenalCap=content.indexOf("\"arsenal\":2")>=0;shopCap=content.indexOf("\"shop\":1")>=0;cardsCap=content.indexOf("\"cards\":1")>=0;itemsCap=content.indexOf("\"items\":2")>=0;emptyKitCap=content.indexOf("\"loadoutHF13\":1")>=0;preciseControls=content.indexOf("\"controls\":2")>=0;modernRooms=content.indexOf("\"rooms\":2")>=0;asyncTurns=content.indexOf("\"async\":1")>=0;departureCap=content.indexOf("\"departures\":1")>=0;replayCap=asyncTurns&&content.indexOf("\"replay\":1")>=0;chunksCap=replayCap&&content.indexOf("\"chunks\":1")>=0;socialCap=chunksCap&&content.indexOf("\"social\":1")>=0;matchmakingCap=socialCap&&content.indexOf("\"matchmaking\":1")>=0;waitBotCap=matchmakingCap&&content.indexOf("\"waitbot\":1")>=0;roomFixCap=waitBotCap&&content.indexOf("\"roomfix\":1")>=0;if(content.indexOf("\"ok\":true")<0||content.indexOf("\"protocol\":1")<0)throw new ServerWake.Rejected("Địa chỉ không trả về máy chủ Gunny giao thức 1.");}
    void probeHealth(int timeout)throws IOException{int generation=networkGeneration;validateEndpoint(endpoint);networkStage="Mở kết nối TCP";tcp.ensure(endpoint);connection=tcp.connection;networkStage="Kiểm tra máy chủ TCP";byte[] reply=tcp.request(endpoint,0,0,"",null,null);if(generation!=networkGeneration)throw new ServerWake.Cancelled();capabilities(new String(reply,"UTF-8"));}
    void checkConnection()throws IOException{
        wake.invalidate();wake.ensureReady();if(Thread.currentThread()!=worker)throw new ServerWake.Cancelled();status="Kết nối máy chủ thành công";
        Alert notice=new Alert("Gunny Online","Kết nối TCP"+" thành công. Bạn có thể đăng ký hoặc đăng nhập.",null,AlertType.INFO);notice.setTimeout(Alert.FOREVER);Display.getDisplay(c.app).setCurrent(notice,c);
    }
    void cancelWake(){abortNetwork(0,"Đã hủy kết nối. Chọn Online để thử lại.");}
    void drawWake(){
        c.panel(22,45,276,165);c.center("ĐANG KẾT NỐI",160,57);
        c.center("Đang đánh thức máy chủ",160,87);c.center("Vui lòng chờ...",160,106);
        c.center("Đã chờ "+Math.max(0,(System.currentTimeMillis()-wake.started)/1000)+" giây • lần "+wake.attempts,160,133);
        c.button("HUỶ / CHƠI OFFLINE",43,169,234,27,true);
    }
    void readProfile(DataInputStream in)throws IOException {int oldSex=c.gender,oldSkin=c.skin;boolean oldCustom=c.customLook;int[] oldLook=new int[5];System.arraycopy(c.look,0,oldLook,0,5);c.playerName=in.readUTF();c.gender=in.readUnsignedByte();c.skin=in.readUnsignedByte();c.customLook=in.readBoolean();for(int i=0;i<5;i++)c.look[i]=in.readUnsignedByte();c.coins=in.readInt();c.wins=in.readInt();c.owned=in.readInt();c.equipped=in.readUnsignedByte();boolean changed=oldSex!=c.gender||oldSkin!=c.skin||oldCustom!=c.customLook;for(int i=0;i<5;i++)if(oldLook[i]!=c.look[i])changed=true;if(changed)c.loadLook();}
    void readLobby(DataInputStream in)throws IOException {int soloSeconds=-1;boolean soloExpired=false;roomId=in.readInt();phase=in.readUnsignedByte();if(phase!=0)chat.leaveWaitingRoom();host=in.readBoolean();roomMap=in.readByte();count=in.readUnsignedByte();settled=in.readBoolean();if(count!=2&&count!=4&&count!=6)throw new IOException("Sai dữ liệu phòng.");
        hostActor=-1;int firstOwned=-1;for(int i=0;i<count;i++){String name=in.readUTF();int nextSex=in.readUnsignedByte(),nextSkin=in.readUnsignedByte();boolean nextCustom=in.readBoolean();boolean changed=standingKeys[i]==null||!name.equals(names[i])||sex[i]!=nextSex||skin[i]!=nextSkin||custom[i]!=nextCustom;names[i]=name;sex[i]=nextSex;skin[i]=nextSkin;custom[i]=nextCustom;for(int k=0;k<5;k++){int part=in.readUnsignedByte();if(look[i][k]!=part)changed=true;look[i][k]=part;}weapon[i]=in.readUnsignedByte();controls[i]=in.readBoolean();present[i]=in.readBoolean();bots[i]=in.readBoolean();int nextMask=equipmentCap?in.readUnsignedByte():custom[i]?125:3;if(wornMasks[i]!=nextMask)changed=true;wornMasks[i]=nextMask;if(actorsCap){actorExperience[i]=in.readInt();if(in.readBoolean())hostActor=i;}if(changed){String key=wornMasks[i]+":"+names[i]+sex[i]+":"+skin[i]+":"+custom[i];for(int k=0;k<5;k++)key+=":"+look[i][k];standingKeys[i]=key;standing[i]=null;}if(controls[i]&&firstOwned<0)firstOwned=i;}if(firstOwned>=0){myActor=firstOwned;if(actorsCap)c.experience=actorExperience[firstOwned];}if(modernRooms){roomChoice=in.readByte();roomSeconds=in.readUnsignedByte();readyMask=in.readUnsignedByte();canStartRoom=in.readBoolean();strictRoom=in.readBoolean();if(asyncTurns){roomKind=in.readUnsignedByte();dungeonBrowser=roomKind==3;}if(departureCap)departure.update(in.readInt(),in.readBoolean(),in.readBoolean(),in.readUTF());}else{roomChoice=roomMap;roomSeconds=20;readyMask=0;canStartRoom=true;strictRoom=false;}if(socialCap){resultsReady=in.readBoolean();cardsOpen=in.readBoolean();for(int i=0;i<(expansionCap?18:9);i++){String cardName=in.readUTF();int cardAmount=in.readUnsignedShort(),cardType=expansionCap?in.readUnsignedByte():0;cardNames[i]=cardName;if(!deckRevealed||cardName.length()>0){cardCoins[i]=cardAmount;cardTypes[i]=cardType;}}if(inBattle&&c.game.over&&settled&&cardsOpen)c.mode=5;results.syncStage();}if(matchmakingCap)matchQueued=in.readBoolean();if(roomFixCap){closedSlots=in.readUnsignedByte();for(int i=0;i<6;i++)emotes[i]=in.readUnsignedByte();}if(waitBotCap){int seconds=in.readUnsignedByte();botVoteMask=in.readUnsignedByte();boolean expired=in.readBoolean();soloSeconds=seconds;soloExpired=expired;updateQueueTime(seconds,expired);if(expired&&!queueTimedOut&&roomKind!=1)c.message("Hết 60 giây tìm trận.");queueTimedOut=expired;}if(itemsCap){roomTitle=in.readUTF();for(int k=0;k<3;k++){int item=in.readUnsignedByte();if(item>Battle.EMPTY_ITEM)throw new IOException("Sai đạo cụ.");if(!pending.equals("loadout"))c.loadout.onlineKit[k]=item;}}if(expansionCap){dungeonDifficulty=in.readUnsignedByte();int previousStage=dungeonStage;dungeonStage=in.readUnsignedByte();dungeonKills=in.readUnsignedByte();rewardPicks=in.readUnsignedByte();if(roomKind==3&&inBattle&&previousStage!=dungeonStage&&phase>0){inBattle=predicting=false;motion.reset();}}if(tribalCap)for(int i=0;i<count;i++)actorWings[i]=in.readBoolean();if(playerUiCap){loadingMask=in.readUnsignedByte();loadingLeaders=in.readUnsignedByte();}if(vipCap){for(int i=0;i<count;i++){actorVip[i]=in.readInt();actorWing[i]=in.readByte();c.game.units[i].wingHp=VipRules.hp(actorWing[i]);}rewardWaitingMask=in.readUnsignedByte();}if(clock793){rewardDeadline=System.currentTimeMillis()+in.readInt();for(int i=0;i<count;i++)loadingPercent[i]=in.readUnsignedByte();results.syncStage();}
        if(soloWaiting())soloQueue.observe(roomId,System.currentTimeMillis(),soloSeconds,soloExpired);else soloQueue.clear();
    }
    Image buildStanding(int actor,int w,int h)throws IOException{return c.equipmentPreview(sex[actor],skin[actor],custom[actor],look[actor],wornMasks[actor],w,h);}
    void updateQueueTime(int seconds,boolean expired){
        long now=System.currentTimeMillis();
        if(expired||seconds<=0){queueDeadline=0;queueClockRoom=0;waitingSeconds=0;return;}
        long reported=now+Math.min(60,seconds)*1000L;
        if(queueDeadline==0||queueClockRoom!=roomId||seconds>waitingSeconds)queueDeadline=reported;
        else queueDeadline=Math.min(queueDeadline,reported);
        queueClockRoom=roomId;waitingSeconds=seconds;
    }
    int queueSeconds(){return queueSeconds(System.currentTimeMillis());}
    int queueSeconds(long now){if(soloWaiting())return soloQueue.seconds(roomId,now);return queueDeadline==0?0:(int)Math.max(0,Math.min(60,(queueDeadline-now+999)/1000));}
    void showRoomState(String operation)throws IOException{
        if(operation.equals("cardsready"))c.mode=5;
        else if(phase==0)c.mode=16;
        else if(!inBattle)prepareBattle();
        else if(operation.equals("departure"))c.mode=1;
    }
    void finishLeave(String op,DataInputStream in)throws IOException {
        c.loadout.resetFocus();
        c.dungeon.closeRewards();boolean leavingDungeon=roomKind==3;boolean leavingSolo=roomKind==1&&op.equals("leave");dungeonBrowser=leavingDungeon;updateQueueTime(0,false);events.cancel();status=in.readUTF();resetCommandQueue(in.readInt());sequenceResync=false;roomId=phase=roomKind=roomCount=0;roomMap=roomChoice=-1;roomPoll=0;social.cancelRequests();inBattle=predicting=resolutionSeen=repairGround=groundAwaiting=false;chunks.reset();replay.reset();predictedTurn=-1;processingSince=0;motion.reset();departure.paused=false;revision=0;queueCount=0;c.resetPending=c.loadingRequested=false;c.held=c.fireKey=0;c.releaseBattleArt();results.reset();c.mode=16;chat.leaveWaitingRoom();if(op.equals("logout")){chat.reset();logged=false;token="";homeAfterLogin=dungeonAfterLogin=dungeonBrowser=false;restoreOffline();c.presentation.settings=false;c.ranking.opened=false;loginForm(false);}else if(leavingDungeon){openBrowser(true);}else if(op.equals("cancelquick")||leavingSolo){waiting.releaseArt();queueTimedOut=false;soloQueue.clear();synchronized(this){pending="";}openBrowser(false);}else synchronized(this){pending="profile";}
    }
    // ACK belongs to the login session, not the room or encounter. Never leave a hole after discarding queued input.
    synchronized void resetCommandQueue(int serverAck){queueCount=0;sequence=ack=sentMax=serverAck;}
    synchronized void acknowledgeCommands(int serverAck){ack=serverAck;int left=0;for(int i=0;i<queueCount;i++)if(queueSeq[i]>ack){queueSeq[left]=queueSeq[i];queueKind[left]=queueKind[i];queueArg[left++]=queueArg[i];}queueCount=left;if(sequence<ack)sequence=ack;if(sentMax<ack)sentMax=ack;}
    void resyncCommands(){hf38AckDue=false;lobbyVersion=0;sequenceResync=true;synchronized(this){queueCount=0;}predicting=resolutionSeen=false;predictedTurn=-1;repairGround=false;groundAwaiting=true;chunks.reset();chunks.recovering=chunksCap;checkpointRequested=checkpointCap&&chunksCap;replay.reset();motion.reset();revision=0;c.held=c.fireKey=0;c.powShotPending=false;c.powCutinTicks=0;if(SulanRules.active(c.game)){c.game.trailCount=0;c.hf39ResetPlayback();for(int i=count;i<c.game.unitCount;i++){Battle.Unit u=c.game.units[i];u.flashTicks=u.landingTicks=u.lastBuff=0;}}processingSince=System.currentTimeMillis();lastPoll=0;}
    void returnToWaitingRoom(){
        hf38AckDue=false;c.dungeon.closeRewards();
        resetCommandQueue(ack);sequenceResync=checkpointRequested=false;snapshotRecoveryTurn=-1;chunks.recovering=false;
        inBattle=predicting=resolutionSeen=repairGround=groundAwaiting=false;queueCount=revision=0;predictedTurn=-1;processingSince=0;departure.paused=false;c.resetPending=c.loadingRequested=false;
        chunks.reset();replay.reset();motion.reset();results.reset();
        if(roomKind==3)dungeonBrowser=true;
        c.releaseBattleArt();c.held=c.fireKey=0;c.roomSelection=5;c.mode=16;
    }
    void prepareBattle()throws IOException {
        hf38AckDue=false;checkpointRequested=false;snapshotRecoveryTurn=-1;chunks.recovering=false;chunks.recoveryFailures=0;recoveryAttempts=0;
        c.loadout.resetFocus();
        for(int i=0;i<layers.length;i++)for(int k=0;k<5;k++)layers[i][k]=null;
        c.conditions.reset();for(int slot=6;slot<c.game.units.length;slot++){c.game.units[slot].npc=0;c.game.units[slot].hp=c.game.units[slot].maxHp=0;}
        c.tribalArt.release();loadingMask=0;sentLoading=-1;visuals.reset();social.opened=false;waiting.releaseArt();c.maps.frame=c.maps.blueButton=null;
        int ally=c.allyMode;
        boolean twoPlayers=c.twoPlayers;
        c.allyMode=0;c.twoPlayers=false;c.training=false;c.offlineDungeon.active=false;
        try{c.beginMatch(roomMap);}
        finally{c.allyMode=ally;c.twoPlayers=twoPlayers;}
        for(int i=0;i<count;i++){for(int k=0;k<5;k++)layers[i][k]=null;if(roomKind==3&&(i%2==1||!present[i])){c.actors[i]=c.actorStand[i]=c.versusPortrait[i]=null;continue;}c.actorSex[i]=sex[i];c.actorSkin[i]=skin[i];boolean own=controls[i]&&sex[i]==c.gender&&skin[i]==c.skin&&custom[i]==c.customLook&&wornMasks[i]==c.wornMask;for(int k=0;k<5;k++)if(look[i][k]!=c.look[k])own=false;
        c.actors[i]=custom[i]?null:own&&c.hero[c.gender]!=null?c.hero[c.gender]:c.load("look"+sex[i]+"_0_"+skin[i]);c.actorStand[i]=buildStanding(i,40,50);c.versusPortrait[i]=c.actorStand[i];if(custom[i])for(int k=0;k<5;k++){layers[i][k]=null;String name=c.layerName(sex[i],k,look[i][k],wornMasks[i],"look");if(name!=null)layers[i][k]=own&&c.lookLayers[k]!=null?c.lookLayers[k]:c.load(name);}}
        if(roomKind==3&&dungeonStage<3)for(int i=1;i<count;i+=2){Image npc=c.load("npc780-"+(dungeonStage==2&&i==1?3:i==3?2:1));int w=Math.min(36*c.s,npc.getWidth()),h=Math.min(45*c.s,npc.getHeight());int width=Math.min(w,npc.getWidth()*h/npc.getHeight()),height=Math.min(h,npc.getHeight()*w/npc.getWidth());c.versusPortrait[i]=NativeUi.fit(npc,Math.max(1,width),Math.max(1,height),0);c.actorStand[i]=c.versusPortrait[i];}
        if(c.s>1){c.onlineGround=new byte[(480*264+7)/8];DataInputStream mask=new DataInputStream(getClass().getResourceAsStream("/collision"+roomMap+".dat"));mask.readFully(c.onlineGround);mask.close();}c.game.viewX=-25;c.game.viewY=-10000;c.game.viewWidth=530;c.game.viewHeight=10255;c.game.explicitHumans=true;c.game.unitCount=count;c.game.teams=count>2||roomKind==3;c.game.dungeonStage=roomKind==3?dungeonStage:0;c.game.hotseat=true;c.game.cheatAim=false;revision=0;chunks.reset();replay.reset();motion.reset();lastPow=-1;rewardRefreshed=false;repairGround=groundAwaiting=false;results.reset();c.emotions.reset();predicting=resolutionSeen=false;processingSince=0;inBattle=true;c.mode=14;c.loadTicks=0;c.lastTurn=-1;c.toastTicks=0;c.held=c.fireKey=0;}
    void readSnapshot(DataInputStream in)throws IOException {if(!inBattle)prepareBattle();if(groundAwaiting)c.resetOnlineTerrain();if(!asyncTurns)motion.before();Battle b=c.game;int oldTurn=b.turn;b.turn=in.readInt();int activeActor=in.readUnsignedByte();if(activeActor>=b.units.length)throw new IOException("Sai người đang đánh trong dữ liệu trận");b.wind=in.readShort();b.lastWind=in.readShort();b.countdown=in.readUnsignedShort();b.winner=in.readByte();b.over=in.readBoolean();b.charging=in.readBoolean();b.pow=in.readBoolean();b.fly=in.readBoolean();b.triple=in.readBoolean();b.extra=in.readUnsignedByte();b.boost=in.readUnsignedByte();b.shotSerial=in.readInt();b.hitSerial=in.readInt();int powTicks=in.readUnsignedShort(),powActor=in.readUnsignedByte();b.bursts=in.readUnsignedShort();if(b.bursts>32)throw new IOException("Sai số đợt đạn.");b.settle=in.readUnsignedShort();b.burstDelay=in.readShort();b.botPhase=in.readShort();b.usedPropCount=in.readUnsignedByte();if(b.usedPropCount>16)throw new IOException("Sai buff.");for(int i=0;i<b.usedPropCount;i++)b.usedPropHistory[i]=in.readUnsignedByte();
        for(int i=0;i<count;i++){Battle.Unit u=b.units[i];u.x=in.readShort();u.y=in.readShort();u.vy=in.readShort();u.hp=in.readUnsignedShort();u.energy=in.readShort();u.rage=in.readShort();u.angle=in.readShort();u.power=in.readShort();u.face=in.readShort();u.walk=in.readShort();int wornWeapon=in.readShort();if(wornWeapon<0||wornWeapon>=10)throw new IOException("Sai vũ khí trong dữ liệu trận");u.weapon=wornWeapon;u.attackTicks=in.readShort();u.slope=in.readShort();u.heals=in.readShort();u.deathX=in.readShort();u.deathY=in.readShort();u.lastPower=in.readShort();u.pendingPower=in.readShort();u.flashTicks=in.readShort();u.landingTicks=in.readShort();u.lastBuff=in.readShort();u.dead=in.readBoolean();if(actorsCap)u.experience=in.readInt();u.team=i%2;u.human=controls[i];u.god=controls[i]&&c.economy.testCheats&&c.economy.testGod;if(expansionCap){u.forgeAttack=in.readShort();u.forgeDefense=in.readShort();u.forgeLuck=in.readShort();int npcKind=in.readUnsignedByte();if(npcKind>TribalRules.RAGE)throw new IOException("Sai NPC trong dữ liệu trận");u.npc=npcKind;u.maxHp=in.readUnsignedShort();u.maxEnergy=in.readUnsignedShort();u.attackBonus=in.readShort();u.forgeWeaponBonus=in.readShort();for(int k=0;k<10;k++)u.weaponForge[k]=in.readShort();}if(u.npc==0&&(equipmentCap||actorsCap))ItemRules.stats(u,wornMasks[i]);}
        b.ballCount=in.readUnsignedByte();if(b.ballCount>20)throw new IOException("Sai đạn.");for(int i=0;i<b.ballCount;i++){Battle.Ball ball=snapshotBalls[i];if(ball==null)snapshotBalls[i]=ball=new Battle.Ball();ball.freeze=ball.guided=ball.homing=false;ball.damage=ball.radius=0;ball.x=in.readInt();ball.y=in.readInt();ball.vx=in.readInt();ball.vy=in.readInt();ball.owner=in.readUnsignedByte();if(ball.owner>=b.units.length)throw new IOException("Sai chủ đạn.");ball.age=in.readUnsignedShort();ball.weapon=in.readUnsignedByte();ball.fly=in.readBoolean();ball.powered=in.readBoolean();b.balls[i]=ball;}
        b.blastCount=in.readUnsignedByte();if(b.blastCount>16)throw new IOException("Sai vụ nổ.");for(int i=0;i<b.blastCount;i++){Battle.Blast f=snapshotBlasts[i];if(f==null)snapshotBlasts[i]=f=new Battle.Blast();f.freeze=false;f.damage=f.owner=0;f.x=in.readShort();f.y=in.readShort();f.radius=in.readUnsignedByte();f.time=in.readUnsignedByte();f.weapon=in.readUnsignedByte();b.blasts[i]=f;}
        b.damagePopupCount=in.readUnsignedByte();if(b.damagePopupCount>20)throw new IOException("Sai sát thương.");for(int i=0;i<b.damagePopupCount;i++){Battle.DamagePopup f=snapshotDamage[i];if(f==null)snapshotDamage[i]=f=new Battle.DamagePopup();f.x=in.readShort();f.y=in.readShort();f.value=in.readUnsignedShort();f.age=in.readUnsignedByte();f.powered=in.readBoolean();b.damagePopups[i]=f;}
        int nextRevision=in.readInt(),craters=in.readUnsignedShort();if(craters>4096)throw new IOException("Sai địa hình.");for(int i=0;i<craters;i++){int x=in.readShort(),y=in.readShort(),radius=in.readShort();if(nextRevision-craters+i>=revision)c.crater(x,y,radius);}revision=nextRevision;if(asyncTurns)for(int i=0;i<count;i++){damage[i]=in.readInt();kills[i]=in.readUnsignedByte();}if(itemsCap){b.freezeShot=in.readBoolean();for(int i=0;i<count;i++){Battle.Unit u=b.units[i];int item0=in.readUnsignedByte(),item1=in.readUnsignedByte(),item2=in.readUnsignedByte();if(!Battle.validKit(item0,item1,item2))throw new IOException("Sai mã đạo cụ trong dữ liệu trận: "+item0+"/"+item1+"/"+item2+"; nhân vật "+i+"; lượt "+b.turn);u.kit[0]=item0;u.kit[1]=item1;u.kit[2]=item2;u.itemUsed=in.readUnsignedByte();u.frozen=in.readUnsignedByte();u.invisible=in.readUnsignedByte();u.assist=in.readBoolean();u.flightAim=i==activeActor&&b.fly;u.freezeTurns=in.readUnsignedByte();u.flyCooldown=in.readUnsignedByte();}for(int i=0;i<b.ballCount;i++)b.balls[i].freeze=in.readBoolean();for(int i=0;i<b.ballCount;i++)b.balls[i].guided=in.readBoolean();for(int i=0;i<b.blastCount;i++)b.blasts[i].freeze=in.readBoolean();}
        b.dungeonStage=roomKind==3?dungeonStage:0;b.dungeonDifficulty=dungeonDifficulty;
        if(tribalCap&&b.dungeonStage>=3){b.tribalRound=in.readUnsignedByte();b.tribalPhase=in.readUnsignedByte();b.tribalWave=in.readUnsignedByte();b.tribalState=in.readUnsignedByte();b.tribalFx=in.readUnsignedShort();b.tribalBeanSaved=in.readBoolean();b.tribalGoal=in.readUnsignedByte();b.tribalOutcome=in.readByte();}
        if(campaignCap&&roomKind==3){
            int size=in.readUnsignedByte();
            if(size>b.units.length-count)throw new IOException("Số đơn vị phó bản không khớp. Hãy build lại client và cập nhật server cùng bản.");
            b.unitCount=count+size;
            for(int i=count;i<b.unitCount;i++){
                Battle.Unit u=b.units[i];
                // Extra NPC wire records omit flash/landing ticks. Age local visual flags
                // on every received frame instead of pinning them forever at the hit pose.
                if(SulanRules.active(b)){if(u.flashTicks>0)u.flashTicks--;if(u.landingTicks>0)u.landingTicks--;}
                int previousHp=u.hp;u.x=in.readShort();u.y=in.readShort();u.hp=in.readUnsignedShort();u.maxHp=in.readUnsignedShort();if(previousHp!=u.hp){u.flashTicks=previousHp<=0?30:24;u.lastBuff=previousHp<=0?14:0;}
                u.face=in.readByte();u.walk=in.readUnsignedShort();u.attackTicks=in.readUnsignedByte();int npcKind=flockCap?in.readUnsignedByte():2;if(npcKind>TribalRules.RAGE)throw new IOException("Sai NPC trong dữ liệu trận");u.npc=npcKind;
                u.dead=in.readBoolean();u.deathX=in.readShort();u.deathY=in.readShort();if(tribalCap)u.power=in.readUnsignedByte();
                u.team=u.npc==TribalRules.BEAN?0:1;u.weapon=0;u.human=false;
            }
        }
        if(sulan7912Cap&&tribalCap&&b.dungeonStage>=3){b.sulanLine=in.readUnsignedByte();b.sulanSpeaker=in.readUnsignedByte();b.sulanTalk=in.readUnsignedShort();b.sulanStory=in.readUnsignedByte();b.sulanStoryClock=in.readUnsignedShort();b.tribalClock=in.readUnsignedShort();b.sulanFireCount=in.readUnsignedByte();if(b.sulanSpeaker>=b.unitCount||b.sulanFireCount>9)throw new IOException("Sai trạng thái Sulan");for(int i=0;i<b.sulanFireCount;i++){b.sulanFireX[i]=in.readUnsignedShort();b.sulanFireLife[i]=in.readUnsignedShort();}for(int n=0;n<7;n++)b.sulanBurn[n]=in.readUnsignedShort();}
        if(activeActor>=b.unitCount)throw new IOException("Người đang đánh vượt số đơn vị trận");b.active=activeActor;
        if(powTicks==0&&!c.powShotPending)c.powCutinTicks=0;
        if(powTicks>0){if(lastPow!=b.turn){lastPow=b.turn;b.active=powActor;c.startPowCutin();}c.powCutinTicks=powTicks;c.powShotPending=false;}
        if(asyncTurns){if(predictedTurn!=b.turn){processingSince=0;resolutionSeen=false;}predictedTurn=b.turn;}groundAwaiting=false;if(asyncTurns){if(liveCap)motion.receiveLive(System.currentTimeMillis());else if(motion.liveReady)motion.reset();if(oldTurn!=b.turn){c.held=c.fireKey=0;if(SulanRules.active(b))c.hf39ResetPlayback();}}else motion.after();if(b.over&&(!socialCap||settled&&cardsOpen)){c.mode=5;c.held=c.fireKey=0;}results.syncStage();c.syncTurnNotice();
    }
    void animate(){
        if(SulanRules.active(c.game)&&chunksCap&&chunks.ownPending&&chunks.terminal&&chunks.resolved){chunks.finish();return;}
        if(snapshotRecoveryTurn==c.game.turn&&!predicting&&(c.game.busy()||c.game.pendingNext||c.game.over||SulanRules.cinematic(c.game)||SulanRules.active(c.game)&&c.powCutinTicks>0)){
            // Snapshot-only recovery must not run physics; local visual timers can still age.
            if(SulanRules.active(c.game)){c.game.tickVisuals();c.game.ageTrails();}return;
        }if(sequenceResync||repairGround||departure.paused||groundAwaiting&&chunks.frames==null)return;Battle b=c.game;if(socialCap&&b.over&&!settled)c.message("Đang lưu kết quả...");if(chunksCap&&chunks.animate())return;if(!chunksCap&&replayCap&&replay.animate())return;
        // HF28: complete a terminal snapshot before any frozen local physics or POW cut-in.
        if(chunksCap&&chunks.ownPending&&SulanRules.active(b)&&b.pendingNext){chunks.finish();return;}
        // Hold the start of a predicted cinematic until its authoritative frames arrive.
        if(chunksCap&&predicting&&SulanRules.cinematic(b)&&(b.tribalState==TribalRules.TRANSFORM||b.tribalState==TribalRules.DEAD)&&!chunks.ownPending){b.tickVisuals();return;}if(processingSince>System.currentTimeMillis())processingSince=System.currentTimeMillis();if(phase==1&&processingSince>0&&System.currentTimeMillis()-processingSince>(SulanRules.active(b)&&predicting&&b.pendingNext?6000:30000)&&queueCount==0&&!chunks.ownPending&&!replay.playing){resyncCommands();c.message("Đang đồng bộ lại dữ liệu...");return;}if(asyncTurns&&(phase==1||replay.playing&&replay.own||chunks.ownPending||phase==2&&predicting&&(b.busy()||c.powShotPending))&&(localTurn()||predicting)){
        if(c.powShotPending){if(--c.powCutinTicks<=0){c.powShotPending=false;b.fire();}return;}if(!b.pendingNext&&!b.over){beginPrediction();if(b.input()){if(!frame(c.held&15)){b.tickVisuals();if(processingSince==0)processingSince=System.currentTimeMillis();return;}if((c.held&1)!=0)b.move(-1);if((c.held&2)!=0)b.move(1);if((c.held&4)!=0)b.adjustAngle(1);if((c.held&8)!=0)b.adjustAngle(-1);}b.tick();}
        if(chunksCap)chunks.finish();else if(replayCap)replay.finish();if((b.busy()||b.pendingNext||b.over)&&processingSince==0)processingSince=System.currentTimeMillis();
        return;
    }if(chunksCap&&!liveCap){b.ageTrails();b.tickVisuals();return;}motion.animate(System.currentTimeMillis());b.ageTrails();if(c.powCutinTicks>0){c.powCutinTicks--;return;}b.tickVisuals();
        // Remote projectiles use authoritative frames. Extrapolating a sparse
        // snapshot can carry a projectile beyond a wall before its hit arrives.

    }
    void acceptSnapshot(DataInputStream in,boolean done,int serverTurn)throws IOException {
        boolean wasResolved=resolutionSeen;int priorTurn=c.game.turn;
        if(!inBattle||!asyncTurns||!predicting||serverTurn!=predictedTurn||checkpointRequested||chunks.recovering||snapshotRecoveryTurn==serverTurn||(done&&(c.game.pendingNext||c.game.over))||phase==2){readSnapshot(in);if(asyncTurns){c.game.deferNext=true;c.game.pendingNext=done;if(done&&processingSince==0)processingSince=System.currentTimeMillis();resolutionSeen=done||c.game.over;predicting=false;if(SulanRules.active(c.game)&&resolutionSeen&&(!wasResolved||priorTurn!=serverTurn))hf38SignalAck();}}
    }
    void actor(int index,int anim,int x,int y,boolean flip){if(custom[index]){int[] order=PhoneCanvas.LOOK_ORDER;int cell=c.s==1?(anim>=16?8+anim-16:anim==0?0:anim<10?1+(anim-1)%5:6+anim%2):anim;for(int k=0;k<5;k++)c.part(layers[index][order[k]],cell*32,0,32,40,x,y,flip);}else c.part(c.actors[index],anim*32,0,32,40,x,y,flip);}
    void draw(){c.all();if(soloSearching()){waiting.drawSoloSearch();return;}c.img(c.lobbyBackground,0,0);if(social.opened){c.all();if(roomId>0){if(roomKind==3)c.dungeon.drawWaiting();else waiting.draw();}else if(dungeonBrowser)c.dungeon.drawList();else rooms.draw();social.draw();return;}if(!logged){auth.draw();return;}if(wake.waiting){drawWake();return;}if(roomId==0){if(dungeonBrowser)c.dungeon.drawList();else rooms.draw();if(create.opened)create.draw();return;}if(roomKind==3)c.dungeon.drawWaiting();else waiting.draw();}
    void activate(){if(!logged){if(selection<2)loginForm(selection==1);else c.openLobby();return;}if(roomId>0){waiting.activate();return;}rooms.activate();}
    void key(int key,int dir,boolean ok){if(social.opened){social.key(key,dir,ok);return;}if(create.opened){create.key(key,dir,ok);return;}if(wake.waiting){if(ok||key==-7||key==-6||key=='0')cancelWake();return;}if(!logged){auth.key(key,dir,ok);return;}if(logged&&roomId==0){if(key=='i'||key=='I'){c.openBag();return;}if(key=='s'||key=='S'){c.openShop();return;}if(key=='n'||key=='N'){c.editName();return;}}if(logged&&(roomId>0&&roomKind==3||roomId==0&&dungeonBrowser)){if(roomId>0)c.dungeon.keyWaiting(key,dir,ok);else c.dungeon.keyList(key,dir,ok);return;}if(key==-7){if(roomId>0)request("leave");else returnHome();return;}if(roomId==0){if(dungeonBrowser)c.dungeon.keyList(key,dir,ok);else rooms.key(key,dir,ok);return;}if(roomKind==3)c.dungeon.keyWaiting(key,dir,ok);else waiting.key(key,dir,ok);}
    void pointer(int x,int y){if(create.opened){create.pointer(x,y);return;}if(logged&&!social.opened&&!create.opened&&(dungeonBrowser||roomKind==3&&roomId>0)){if(roomId>0)c.dungeon.pointerWaiting(x,y);else c.dungeon.pointerList(x,y);return;}if(social.opened){social.pointer(x,y);return;}if(create.opened){create.pointer(x,y);return;}if(wake.waiting){if(y>=169&&y<200)cancelWake();return;}if(!logged){auth.pointer(x,y);return;}if(roomId==0){rooms.pointer(x,y);return;}waiting.pointer(x,y);}
    void stop(){rewardOperation="";events.cancel();tcp.close();alive=false;password="";wake.cancel();Connection active=connection;if(active!=null)try{active.close();}catch(Exception ignored){}}
    // A newly completed turn must wake the worker even if an older sync is in flight.
    volatile boolean hf38AckDue;
    void hf38SignalAck(){hf38AckDue=true;synchronized(this){notifyAll();}}
    boolean battleSyncDue(long now){
        if(!logged||!inBattle||phase!=1||roomId<=0)return false;
        synchronized(c){synchronized(this){
            boolean urgent=sequenceResync||repairGround||BattleNetworkPolicy.urgent(queueKind,queueCount)||(c.held&15)!=sentHeld;
            boolean prefetch=chunks.wantsMore();long elapsed=now-lastBattleSync;if(elapsed<0)elapsed=500;
            if(hf38AckDue&&resolutionSeen&&SulanRules.active(c.game))return true;
            // A missed event must never leave an encounter on a frozen frame.
            if(chunksCap&&snapshotRecoveryTurn==c.game.turn&&elapsed>=100)return true;
            if(chunksCap&&elapsed>=500)return true;
            if(SulanRules.active(c.game)&&elapsed>=250&&(SulanRules.cinematic(c.game)||chunks.scenePlayback()||chunks.ownPending||chunks.waitingForNext||c.game.pendingNext||resolutionSeen))return true;
            return BattleNetworkPolicy.lowBufferDue(elapsed,prefetch,chunks.bufferedFrames(),c.projectilePacing())||BattleNetworkPolicy.due(elapsed,urgent,queueCount>0,events.due(),prefetch);
        }}
    }
    public void run(){if(Thread.currentThread()!=worker)return;int generation=networkGeneration;while(alive&&generation==networkGeneration){String op; synchronized(this){String solo=takeSoloAction(System.currentTimeMillis());if(soloWaiting()){op=solo;}else if(rewardOperation.length()>0){op=rewardOperation;rewardOperation="";}else{op=pending;pending="";}if(op.length()>0)operation=op;}long now=System.currentTimeMillis();if(!soloWaiting())events.ensure();if(op.length()==0&&!soloWaiting()&&!wake.cancelled&&logged&&sequenceResync&&roomId>0){op="sync";}else if(op.length()==0&&!soloWaiting()&&!wake.cancelled&&battleSyncDue(now)){op="sync";}else if(op.length()==0&&!soloWaiting()&&!wake.cancelled&&logged){op=inBattle&&phase==1?"":social.nextRequest(now);if(op.length()==0)op=chat.nextRequest(now);if(op.length()>0){}else if(roomId==0&&c.mode==16&&now-roomPoll>=2000){roomPoll=now;op=dungeonBrowser?"dungeonrooms":"rooms";}else if(clock793&&inBattle&&phase==3&&Math.min(100,c.loadTicks*100/60)!=sentLoading&&now-loadPercentPoll>180){loadPercentPoll=now;sentLoading=Math.min(100,c.loadTicks*100/60);op="loadpct";}else if(c.ranking.opened&&now-c.ranking.poll>5000){op="ranking";}else if(vipCap&&!(inBattle&&phase==1)&&(!c.vip.loaded||now-c.vip.poll>5000)){op="vipstate";}else if(deckCap&&inBattle&&phase==2&&results.stage==1&&!deckRevealed&&now-deckPoll>900){deckPoll=now;op="deck";}else if(progressCap&&(!inBattle||phase==2||progressRoom!=roomId)&&(progressRoom!=roomId||now-progressPoll>5000)){progressPoll=now;op="progress";}else if(c.emotions.canPoll()&&now-c.emotions.poll>1000){c.emotions.poll=now;op="battleemotes";}else if(presenceCap&&!inBattle&&roomId==0&&now-presencePoll>4000){presencePoll=now;op="presence";}else if(socialCap&&!inBattle&&(c.mode==16||c.mode==0)&&now-social.poll>3000){social.poll=now;op="socialpulse";}else if(roomId>0&&!(inBattle&&phase==1)&&((events.changed()||now-lastPoll>=(inBattle?80:900)&&events.due())||inBattle&&(queueCount>0||(c.held&15)!=sentHeld)))op="sync";else if(roomId==0&&c.mode==16&&now-lastPoll>=3000)op=dungeonBrowser?"dungeonrooms":"rooms";}if(op.length()==0){synchronized(this){try{wait(15);}catch(Exception ignored){}}continue;}if(op.equals("rooms")||op.equals("dungeonrooms"))roomPoll=now;if(op.equals("sync"))events.consume();busy=true;operation=op;transportSince=System.currentTimeMillis();int syncAck=-1;try{if(op.equals("wake")){wake.ensureReady();continue;}if(op.equals("health")){checkConnection();continue;}DataInputStream in=post(op);if(op.equals("sync")&&diagRoom==roomId){diagReplyAt=System.currentTimeMillis();diagPhase=2;}if(generation!=networkGeneration){in.close();continue;}boolean created=false;if(op.equals("register")){in.readUTF();in.close();try{in=post("login");created=true;op="login";}catch(Exception loginError){if(generation!=networkGeneration||wake.cancelled||!alive)throw new ServerWake.Cancelled();password="";signupFollowup(loginError.getMessage()==null?"Chưa thể đăng nhập tự động.":loginError.getMessage());lastPoll=System.currentTimeMillis();continue;}}// Compact sync has a fixed header. Decode the expensive chunk outside the render lock.
            int syncVersion=0,syncTurn=-1,syncStage=-1;
            boolean syncHeader=op.equals("sync")&&compactLobby,syncFull=false,syncDone=false,syncLobbyApplied=false;
            OnlineChunks.Segment syncSegment=null;
            if(syncHeader){
                syncAck=in.readInt();syncVersion=in.readInt();syncFull=in.readBoolean();
                if(syncVersion<=0||!syncFull&&(lobbyVersion==0||syncVersion!=lobbyVersion))throw new LobbyCacheMismatch();
                if(syncFull){
                    synchronized(c){
                        if(generation!=networkGeneration){in.close();continue;}
                        acknowledgeCommands(syncAck);readLobby(in);lobbyVersion=syncVersion;syncLobbyApplied=true;
                    }
                }
                if(inBattle&&phase>0&&asyncTurns&&chunksCap){
                    syncDone=in.readBoolean();syncTurn=in.readInt();syncStage=in.readUnsignedByte();
                    if(syncStage==4)syncSegment=OnlineChunks.decodeSegment(in,syncDone);if(diagRoom==roomId){diagServerTurn=syncTurn;diagStage=syncStage;diagServerDone=syncDone;diagSegment=syncSegment==null?-1:syncSegment.id;diagPhase=3;}
                }
            }
            synchronized(c){if(generation!=networkGeneration){in.close();continue;}
                if(op.equals("ranking")){c.ranking.read(in);}else if(op.equals("loadpct")){in.readUnsignedByte();}else if(op.equals("autocards")){readProfile(in);c.shoppingMoney=in.readInt();c.economy.read(in);synchronized(this){pending=results.returnOperation();}}else if(op.equals("resultresume")){if(in.readBoolean()){readLobby(in);c.game.winner=in.readUnsignedByte();results.resumedEligible=in.readBoolean();c.game.over=true;inBattle=phase>0;if(inBattle){results.stage=results.resumedEligible?1:0;results.restoreOwnClaim();c.mode=5;}else{c.mode=16;waiting.reset();}}else if(tribalCap)synchronized(this){pending="tribalresume";}}else if(op.startsWith("vip")){int moneyBefore=c.shoppingMoney;c.vip.read(in);if(op.equals("vipuse")&&c.vip.item>=13){c.economy.read(in);c.bag.list();}if(!op.equals("vipstate")){c.vip.requestKey="";c.message(c.shoppingMoney>moneyBefore?"Đã đổi: +"+(c.shoppingMoney-moneyBefore)+" xu mua sắm":"Đã cập nhật VIP/cánh");}}else if(op.equals("deck")){if(in.readBoolean()){int size=in.readUnsignedByte();if(size>18)throw new IOException("Sai bộ thẻ");for(int i=0;i<size;i++){cardTypes[i]=in.readUnsignedByte();cardCoins[i]=in.readUnsignedShort();}deckRevealed=true;}}else if((op.equals("giftopen")||op.equals("giftopenexp"))){readProfile(in);c.shoppingMoney=in.readInt();c.experience=in.readInt();c.economy.read(in);c.gifts.read(in,op.equals("giftopenexp"));}else if(op.startsWith("chat"))chat.read(op,in);else if(op.equals("testcheat")){readProfile(in);c.shoppingMoney=in.readInt();c.experience=in.readInt();c.economy.read(in);if(c.cheatSelection==6){c.vip.loaded=false;cheatVipKey="";}if(c.cheatSelection==5){chunks.reset();replay.reset();predicting=false;c.mode=c.returnMode;}c.message("Đã áp dụng cheat test");}else if(op.equals("economy")){c.economy.read(in);}else if(op.equals("buystones")||op.equals("forge")||op.equals("buycraft")||op.equals("forgechance")){readProfile(in);c.shoppingMoney=in.readInt();c.economy.read(in);economyKey="";if(op.equals("forge")||op.equals("forgechance"))c.forge.complete();else{c.stoneShop.requestKey="";c.message("Đã mua đá cường hóa");}}else if(op.equals("claim780")){int card=in.readUnsignedByte(),type=in.readUnsignedByte(),amount=in.readUnsignedShort(),pick=in.readUnsignedByte();cardNames[card]=c.playerName;cardTypes[card]=type;cardCoins[card]=amount;rewardPicks=Math.max(rewardPicks,pick+1);results.expandedCards.reveal(card);readProfile(in);c.economy.read(in);}else if(op.equals("playerinfo")){if(social.viewing&&sentInfoTarget==social.target)social.readInfo(in);}else if(op.equals("progress")){progressRoom=roomId;c.experience=in.readInt();c.wins=in.readInt();c.serverRank=in.readInt();int actors=in.readUnsignedByte();if(actors>6)throw new IOException("Sai tiến trình");for(int i=0;i<actors;i++)actorExperience[i]=in.readInt();}else if(op.equals("presence")){onlineCount=in.readInt();if(bagCap&&in.available()>=8){c.coins=in.readInt();c.shoppingMoney=in.readInt();}}else if(op.equals("battleemote")||op.equals("battleemotes")){c.emotions.read(in);}else if(op.equals("findfriend")){if(social.tab==4&&sentSocialGeneration==social.generation)social.read(in);}else if(op.equals("social")){if(social.tab==sentSocialTab&&sentSocialGeneration==social.generation)social.read(in);}else if(op.equals("socialpulse")){social.pulse(in);}else if(op.equals("friend")||op.equals("invite")){status=in.readUTF();if(op.equals("friend")){social.actionLoading=false;social.invited(status);social.refresh();}else social.invited(status);}else if(op.equals("inviteanswer")){if(in.readBoolean()){readLobby(in);social.opened=false;c.mode=16;waiting.reset();}}else if(op.equals("login")){auth.releaseArt();chat.reset();token=in.readUTF();captureOffline();c.shoppingMoney=0;c.wornMask=0;c.experience=0;c.serverRank=0;progressRoom=-1;for(int i=0;i<actorExperience.length;i++)actorExperience[i]=0;c.bagLayout.resetOnline();c.economy.resetOnline();for(int k=0;k<3;k++)c.loadout.onlineKit[k]=k;c.vip.resetAccount();logged=true;events.failed=false;events.failures=0;departure.room=departure.shown=0;departure.paused=false;resetCommandQueue(0);sequenceResync=checkpointRequested=false;snapshotRecoveryTurn=-1;recoveryAttempts=0;chunks.recovering=false;revision=roomId=0;inBattle=false;readProfile(in);status=created?"Đăng ký thành công":"Đã đăng nhập";homeAfterLogin=true;c.mode=0;c.inventory.onlineLoaded=false;if(in.available()>0)c.notices.read(in);synchronized(this){pending=characterCap?"character":shopCap?"shop":"rooms";}}
                else if(op.equals("shop")){
                    c.inventory.read(in);
                    if(c.mode==6)c.shop.resetTrial();
                    if(homeAfterLogin)finishLogin();
                    else if(c.mode==16)synchronized(this){pending=dungeonBrowser?"dungeonrooms":"rooms";}
                }
                else if(op.equals("character")){boolean chosen=in.readBoolean();if(!chosen){choosingCharacter=true;c.gender=0;c.skin=0;c.customLook=true;c.wornMask=0;for(int k=0;k<5;k++)c.look[k]=0;c.loadCreation();c.createFocus=0;c.mode=7;}else synchronized(this){pending=shopCap?"shop":"rooms";}}
                else if(op.equals("baglayout")){c.inventory.read(in);c.bagLayout.pending=false;c.bag.list();status="Đã sắp xếp túi";}
                else if(op.equals("gender")){readProfile(in);c.inventory.read(in);c.bag.previewKey="";c.bag.iconSex=-1;c.bag.list();c.message("Đã đổi nhân vật");}
                else if(op.equals("equip")){readProfile(in);c.inventory.read(in);c.bag.confirmMove();if(c.mode==6)c.shop.resetTrial();status="Đã đổi trang bị";}
                else if(op.equals("exchange")){readProfile(in);c.inventory.read(in);c.popup.notice("Đổi thành công","Đã nhận "+exchangeAmount+" tiền mua sắm");status="Đã đổi xu";}
                else if(op.equals("checkout")){readProfile(in);c.inventory.read(in);c.shop.bought();status="Đã mua đồ";}
                else if(op.equals("claim")){results.chosen=in.readUnsignedByte();results.coins=in.readUnsignedShort();if(!results.claimed)results.revealStarted=System.currentTimeMillis();results.claimed=true;readProfile(in);status="Đã nhận xu";}
                else if(op.equals("profile")||op.equals("buy")||op.equals("appearance")){readProfile(in);status="Đã lưu tài khoản";if(op.equals("appearance")&&choosingCharacter){choosingCharacter=false;c.creation=null;c.chooseArt[0]=c.chooseArt[1]=null;c.mode=0;homeAfterLogin=true;synchronized(this){pending=shopCap?"shop":"rooms";}}}
                else if(op.equals("rooms")||op.equals("dungeonrooms")){
                    if(homeAfterLogin&&!shopCap){
                        finishLogin();
                        in.close();
                        continue;
                    }
                    if(op.equals("dungeonrooms")!=dungeonBrowser||roomId>0){in.close();roomPoll=0;continue;}
                    if(op.equals("dungeonrooms"))dungeonListLoading=false;
                    roomCount=in.readUnsignedShort();if(roomCount>32)throw new IOException("Quá nhiều phòng.");for(int i=0;i<roomCount;i++){roomIds[i]=in.readInt();roomNames[i]=in.readUTF();roomCounts[i]=in.readUnsignedByte();roomCapacities[i]=in.readUnsignedByte();roomPhases[i]=in.readUnsignedByte();roomLocked[i]=asyncTurns&&in.readBoolean();if(cardsCap){roomKinds[i]=in.readUnsignedByte();roomMaps[i]=in.readByte();roomPlayers[i]=in.readUnsignedByte();roomSeats[i]=in.readUnsignedByte();if(roomKinds[i]>(expansionCap?3:2)||roomMaps[i]<-1||roomMaps[i]>(tribalCap?11:expansionCap?7:5)||roomSeats[i]<1||roomSeats[i]>6||roomPlayers[i]>roomSeats[i])throw new IOException("Sai thẻ phòng.");}else{roomKinds[i]=roomCapacities[i]==6?2:0;roomMaps[i]=-1;roomPlayers[i]=roomCounts[i];roomSeats[i]=roomCapacities[i];}if(expansionCap)roomDifficulties[i]=in.readUnsignedByte();if(tribalCap)roomTribal[i]=in.readBoolean();}status=cardsCap?"Có "+roomCount+" phòng":"Cập nhật server 7.1.3 để hiện đúng thẻ phòng.";}
                else if(op.equals("tribalresume")){if(in.readBoolean()){readLobby(in);homeAfterLogin=false;showRoomState(op);if(inBattle&&phase==1&&checkpointCap)beginRoomRecovery(0);}else c.mode=0;}else if(op.equals("tribalwing")){c.economy.read(in);}else if(op.equals("dungeonretreat")||op.equals("pvpretreat")||op.equals("returnroom")){readLobby(in);returnToWaitingRoom();}else if(op.equals("dungeoncontinue")){readLobby(in);returnToWaitingRoom();}else if(op.equals("tribalcreate")||op.equals("dungeoncreate")||op.equals("dungeonquick")||op.equals("dungeonteam")||op.equals("dungeonsettings")||op.equals("create")||op.equals("create2")||op.equals("create3")||op.equals("createteam")||op.equals("botvote")||op.equals("quick")||op.equals("join")||op.equals("start")||op.equals("loadout")||op.equals("roomsettings")||op.equals("ready")||op.equals("switchteam")||op.equals("departure")||op.equals("allyai")||op.equals("slot")||op.equals("emote")||op.equals("cardsready")){readLobby(in);roomEnteredAt=System.currentTimeMillis();rooms.releaseArt();if(roomKind==3&&(op.equals("dungeoncreate")||op.equals("dungeonquick")||op.equals("dungeonteam")||op.equals("join"))){c.roomSelection=29;c.dungeon.picker=false;c.dungeon.closeRewards();}if(op.startsWith("create")||op.equals("quick")||op.equals("join"))waiting.reset();selection=0;status="Đã vào phòng "+roomId;showRoomState(op);lastPoll=0;}
                else if(op.equals("sync")){int replyAck=syncHeader?syncAck:in.readInt();if(sequenceResync||checkpointRequested)resetCommandQueue(replyAck);else acknowledgeCommands(replyAck);if(syncHeader){if(syncVersion<=0||!syncFull&&(lobbyVersion==0||syncVersion!=lobbyVersion))throw new LobbyCacheMismatch();if(syncFull&&!syncLobbyApplied)readLobby(in);lobbyVersion=syncVersion;}else readLobby(in);if(phase==0&&inBattle)returnToWaitingRoom();if(phase>0){boolean done=false;int serverTurn=-1;if(syncStage>=0){done=syncDone;serverTurn=syncTurn;}else if(asyncTurns){done=in.readBoolean();serverTurn=in.readInt();}if(chunksCap){if(checkpointRequested&&syncStage>=0&&syncStage!=0)throw new IOException("Thiếu trạng thái khôi phục trận đấu.");if(syncStage>=0)chunks.readPrepared(in,done,serverTurn,syncStage,syncSegment);else chunks.read(in,done,serverTurn);}else if(replayCap)replay.read(in,done,serverTurn);else acceptSnapshot(in,done,serverTurn);if(checkpointRequested&&!chunks.recovering){snapshotRecoveryTurn=serverTurn;checkpointRequested=false;}if(snapshotRecoveryTurn!=serverTurn)snapshotRecoveryTurn=-1;}sequenceResync=inBattle&&(checkpointRequested||chunks.recovering);if(!sequenceResync)recoveryAttempts=0;status=phase==2?(settled?"Đã lưu kết quả":"Đang lưu kết quả") :"Đã kết nối";if(settled&&!rewardRefreshed){rewardRefreshed=true;progressPoll=0;synchronized(this){pending="profile";}}}
                else if(op.equals("cancelquick")){if(in.readBoolean())finishLeave(op,in);else{readLobby(in);showRoomState(op);}}
                else if(op.equals("leave")||op.equals("logout")){finishLeave(op,in);}
            }in.close();lastPoll=System.currentTimeMillis();if(op.equals("sync")){lastBattleSync=now>lastPoll?lastPoll:now;if(diagRoom==roomId){diagAppliedAt=System.currentTimeMillis();diagPhase=4;diagSyncCount++;}}
        }catch(Exception e){if(generation!=networkGeneration)continue;if(op.equals("sync")&&diagRoom==roomId){diagFailures++;diagPhase=5;diagError=e.getClass().getName();int dot=diagError.lastIndexOf('.');if(dot>=0)diagError=diagError.substring(dot+1);}
            if(e instanceof SessionEnded){String why=e.getMessage();boolean replaced=why!=null&&why.indexOf("thiết bị khác")>=0;endSession(replaced?"Trùng đăng nhập":"Phiên đăng nhập hết hạn",replaced?why+" Phiên này đã kết thúc. Hãy đăng nhập lại để tiếp tục.":why);continue;}
            if(logged&&roomId>0&&op.equals("sync")&&(e instanceof LobbyCacheMismatch||e instanceof IOException&&!(e instanceof ServerReply)&&!(e instanceof ServerWake.Cancelled))&&++recoveryAttempts<=3){
                if(!(e instanceof LobbyCacheMismatch)){tcp.close();events.cancel();events.failed=true;}
                beginRoomRecovery(e instanceof LobbyCacheMismatch?syncAck:-1);
                try{Thread.sleep(200*recoveryAttempts);}catch(InterruptedException ignored){}continue;
            }
            if(e instanceof SequenceRecovery){lastPoll=0;continue;}if(op.equals("forge")||op.equals("forgechance"))c.forge.failed();social.failed(op,e.getMessage());if(e instanceof ServerReply&&logged&&op.equals("battleemotes")){c.emotions.poll=System.currentTimeMillis()+4000;lastPoll=0;continue;}if(wake.cancelled||!alive||e instanceof ServerWake.Cancelled){password="";status="Đã hủy kết nối";continue;}if(!(e instanceof ServerReply)||!logged&&!(op.equals("register")||op.equals("login")||op.equals("health"))){disconnect(failureDetails(e,networkStage));continue;}if(op.equals("baglayout"))c.bagLayout.rollback();
            synchronized(c){
                if(logged&&op.startsWith("chat")){chat.failed(op,e.getMessage());continue;}
                if(logged&&op.equals("dungeonrooms")){
                    if(c.mode==16&&dungeonBrowser&&roomId==0){
                        dungeonBrowser=dungeonListLoading=false;
                        c.openLobby();
                        c.popup.notice(DungeonScreen.TITLE,e.getMessage());
                    }
                    continue;
                }
                if(logged&&homeAfterLogin&&(op.equals("character")||op.equals("shop")||op.equals("rooms"))){
                    homeAfterLogin=dungeonAfterLogin=false;
                    c.openLobby();
                    c.popup.notice("Không tải được hồ sơ",e.getMessage());
                    continue;
                }
            }
            String details=e instanceof ServerReply?e.getMessage():failureDetails(e,networkStage);status=details;if(status.length()>160)status=status.substring(0,157)+"...";synchronized(c){if(logged&&!(inBattle&&c.mode==1))c.popup.notice("Thông báo",status);else c.message(status);if(!logged)c.mode=16;}if(op.equals("register")){synchronized(c){c.popup.notice("Đăng ký",details);}}else if(op.equals("health")||op.equals("login")){Alert error=new Alert(e instanceof ServerReply?(op.equals("register")?"Đăng ký":op.equals("login")?"Đăng nhập":"Máy chủ"):"Lỗi kết nối Gunny",details,null,AlertType.ERROR);error.setTimeout(Alert.FOREVER);Display.getDisplay(c.app).setCurrent(error,c);}lastPoll=System.currentTimeMillis()+1500;try{Thread.sleep(800);}catch(Exception ignored){}
        }finally{if(generation==networkGeneration){if(op.equals("login")||op.equals("register"))password="";operation="";busy=false;transportSince=0;}}}
    }
}
