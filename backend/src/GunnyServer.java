import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Authoritative 30Hz server. Shares Battle.java with the CLDC client. */
public final class GunnyServer implements AutoCloseable {
    static final int MAGIC=0x474e5931,MAX_BODY=16384;
    static final String[] BOT_NAMES={"Xạ thủ","Đột kích","Pháo thủ","Hỗ trợ","Xạ thủ nữ","Đột kích nữ","Pháo thủ nữ","Hỗ trợ nữ"};
    static final int[] BOT_SKINS={1,3,5,7,0,2,4,6},WEAPONS={0,3,5,9};
    final CloudStore store;final HttpServer http;final ScheduledExecutorService clock=Executors.newSingleThreadScheduledExecutor();
    final ExecutorService workers=new ThreadPoolExecutor(4,16,60,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(64),new ThreadPoolExecutor.CallerRunsPolicy()),saves=Executors.newSingleThreadExecutor();
    final Map<String,Session> sessions=new ConcurrentHashMap<>();final Map<Integer,Room> rooms=new ConcurrentHashMap<>();
    final Map<String,Window> limits=new ConcurrentHashMap<>();final SecureRandom random=new SecureRandom();final AtomicInteger nextRoom=new AtomicInteger(1000);
    final boolean[][] terrain=new boolean[6][];final int maxRooms;volatile boolean closed;
    static final class Window {long start;int count;}
    static final class Session {String token,id;CloudStore.Profile profile;long expiry,lastSeen;int room,sequence,held;boolean ready,armed,modern;}
    static final class Avatar {String name;int sex,skin,weapon;boolean custom,bot;int[] look=new int[5];Session owner;}
    final class Ground implements Battle.Ground {
        final boolean[] solid;final List<int[]> craters=new ArrayList<>();Ground(int map){solid=terrain[map].clone();}
        public boolean solid(int x,int y){return x>=0&&x<480&&y>=0&&y<220&&solid[y*480+x];}
        public int surface(int x,int from){if(x<3||x>=477)return 250;for(int y=Math.max(0,from);y<220;y++)if(solid(x-2,y)||solid(x+2,y))return y;return 250;}
        public void crater(int x,int y,int radius){for(int py=Math.max(0,y-radius);py<=Math.min(219,y+radius);py++)for(int px=Math.max(0,x-radius);px<=Math.min(479,x+radius);px++)if((px-x)*(px-x)+(py-y)*(py-y)<=radius*radius)solid[py*480+px]=false;if(craters.size()<4096)craters.add(new int[]{x,y,radius});}
    }
    final class Room {
        final int id=nextRoom.incrementAndGet();final String match=UUID.randomUUID().toString();final Session host;final Session[] members=new Session[4];final Avatar[] avatars=new Avatar[4];
        final int count,allyMode,bot;int requestedMap,map=-1,mapLimit=3,seconds=20;boolean strict;int phase,powTicks,powActor;long lastActive=System.currentTimeMillis();boolean settlementQueued;volatile boolean settled;Ground ground;Battle battle;final List<Map<String,Object>> participants=new ArrayList<>();
        Room(Session host,int map,int allyMode,int bot){this.host=host;members[0]=host;requestedMap=map;this.allyMode=allyMode;this.bot=bot;count=allyMode>0?4:2;for(int i=0;i<count;i++)avatars[i]=avatar(null,i);refreshWaiting();}
        Avatar avatar(Session member,int i){Avatar a=new Avatar();if(member!=null){CloudStore.Profile p=member.profile;a.owner=member;a.name=p.name;a.sex=p.gender;a.skin=p.skin;a.custom=p.custom;a.look=p.look.clone();a.weapon=p.equipped;}else{int b=i==2&&bot>=0?bot:random.nextInt(8);a.name=BOT_NAMES[b];a.sex=b/4;a.skin=BOT_SKINS[b];a.weapon=WEAPONS[b%4];a.bot=true;if(i==2&&allyMode==2&&!strict){a.owner=host;a.bot=false;a.name="Người 2";}}return a;}
        void refreshWaiting(){for(int i=0;i<count;i++)avatars[i]=avatar(members[i],i);resolveMap();}
        boolean opponents(){boolean red=false,blue=false;for(int i=0;i<count;i++)if(members[i]!=null){if(i%2==0)red=true;else blue=true;}return red&&blue;}
        void clearArmed(){for(Session member:members)if(member!=null)member.armed=false;}
        void resolveMap(){if(!strict)return;int old=map;if(requestedMap>=0)map=requestedMap;else if(!opponents())map=-1;else if(map<0)map=random.nextInt(mapLimit);if(old!=map)clearArmed();}
        boolean canStart(){if(!strict)return true;if(!opponents())return false;for(Session member:members)if(member!=null&&member!=host&&!member.armed)return false;return true;}
        void settings(Session actor,int choice,int duration){if(actor!=host)throw new IllegalArgumentException("Chỉ chủ phòng được chọn bản đồ.");if(phase!=0)throw new IllegalArgumentException("Trận đã bắt đầu.");if(choice< -1||choice>=mapLimit||duration!=8&&duration!=12&&duration!=15)throw new IllegalArgumentException("Thiết lập bản đồ không hợp lệ.");requestedMap=choice;seconds=duration;map=-1;clearArmed();resolveMap();}
        void switchTeam(Session actor,int team){if(!strict||phase!=0||team<0||team>1)throw new IllegalArgumentException("Chưa thể đổi đội.");int current=slot(actor),target=-1;if(current%2==team)return;for(int i=team;i<count;i+=2)if(members[i]==null){target=i;break;}if(target<0)throw new IllegalArgumentException("Đội đã đủ người.");members[current]=null;members[target]=actor;clearArmed();refreshWaiting();}
        int slot(Session s){for(int i=0;i<count;i++)if(members[i]==s)return i;return -1;}
        boolean controls(Session s,int actor){return actor>=0&&actor<count&&avatars[actor].owner==s;}
        void start(){if(phase!=0)throw new IllegalArgumentException("Trận đã bắt đầu.");if(!canStart())throw new IllegalArgumentException(opponents()?"Chờ người chơi sẵn sàng.":"Chờ đối thủ vào phòng.");if(map<0)map=requestedMap<0?random.nextInt(mapLimit):requestedMap;ground=new Ground(map);battle=new Battle(ground);battle.seed=random.nextInt();battle.start(true);battle.turnLimit=seconds*30;battle.countdown=battle.turnLimit;if(count==4)battle.configureTeam(allyMode);refreshWaiting();
            for(int i=0;i<count;i++){Battle.Unit u=battle.units[i];u.weapon=avatars[i].weapon;u.human=avatars[i].owner!=null;u.botStyle=i==2&&bot>=0?bot%4:random.nextInt(4);battle.refreshAim(u);}
            Set<String> ids=new HashSet<>();for(int i=0;i<count;i++){Session s=avatars[i].owner;if(s!=null&&ids.add(s.id)){s.ready=false;participants.add(Map.of("id",s.id,"team",i%2));}}
            battle.hotseat=count==2&&avatars[1].owner!=null;battle.deferBotPow=true;phase=3;lastActive=System.currentTimeMillis();
        }
        void command(Session s,int seq,int kind,int arg){if(seq<=s.sequence)return;if(seq!=s.sequence+1)throw new IllegalArgumentException("Lệch thứ tự lệnh. Hãy kết nối lại.");s.sequence=seq;if(phase!=1||powTicks>0||!controls(s,battle.active)||!battle.input())return;
            switch(kind){case 1:battle.charge();break;case 2:if(!battle.charging)return;if(arg<0){int intent=-arg-1,angle=intent/128,power=intent%128;Battle.Unit u=battle.units[battle.active];if(power>100||angle<Battle.weaponMinAngle(u.weapon)||angle>Battle.weaponMaxAngle(u.weapon))return;u.angle=angle;u.power=power;}if(battle.pow){battle.charging=false;powActor=battle.active;powTicks=150;}else battle.fire();break;case 3:battle.prop(arg);break;case 4:battle.pass();break;case 5:if(validWeapon(arg)&&(s.profile.owned&(1<<arg))!=0)battle.units[battle.active].weapon=arg;break;default:throw new IllegalArgumentException("Lệnh không hợp lệ.");}
        }
        void tick(){long now=System.currentTimeMillis();if(phase==3){boolean ready=true;for(Session s:members)if(s!=null&&!s.ready)ready=false;if(ready||now-lastActive>20000)phase=1;return;}if(phase!=1)return;
            if(powTicks>0){if(--powTicks==0){if(battle.botPowQueued)battle.releaseBotPow();else{battle.botActing=true;try{battle.fire();}finally{battle.botActing=false;}}}return;}
            for(int i=0;i<count;i++)battle.units[i].human=avatars[i].owner!=null&&now-avatars[i].owner.lastSeen<45000;
            if(count==2)battle.hotseat=battle.units[1].human;
            // Battle's legacy unit-zero rule assumes a local human. A disconnected
            // host still has a bounded turn timer, and cannot lock the room.
            Session owner=avatars[battle.active].owner;if(owner!=null&&now-owner.lastSeen<45000&&battle.input()){
                int held=owner.held;if((held&1)!=0)battle.move(-1);if((held&2)!=0)battle.move(1);if((held&4)!=0)battle.adjustAngle(1);if((held&8)!=0)battle.adjustAngle(-1);
            }
            battle.tick();if(battle.botPowQueued){powActor=battle.active;powTicks=150;}
            if(battle.over){phase=2;lastActive=now;queueSettlement(this);}
        }
    }
    public GunnyServer(InetSocketAddress address,Path assets,CloudStore store)throws Exception {
        this.store=store;maxRooms=Math.max(1,Math.min(32,Integer.parseInt(CloudStore.env("MAX_ROOMS").isEmpty()?"8":CloudStore.env("MAX_ROOMS"))));
        for(int i=0;i<6;i++){BufferedImage image=ImageIO.read(assets.resolve("land"+i+".png").toFile());if(image==null||image.getWidth()!=480||image.getHeight()!=220)throw new IllegalStateException("Use assets1 terrain files");terrain[i]=new boolean[480*220];for(int y=0;y<220;y++)for(int x=0;x<480;x++)terrain[i][y*480+x]=(image.getRGB(x,y)>>>24)>96;}
        http=HttpServer.create(address,64);http.setExecutor(workers);http.createContext("/health",this::health);http.createContext("/api",this::api);http.createContext("/auth/confirmed",this::emailConfirmation);http.createContext("/",this::emailConfirmation);
        http.start();clock.scheduleAtFixedRate(this::tick,33,33,TimeUnit.MILLISECONDS);
    }
    int port(){return http.getAddress().getPort();}
    void health(HttpExchange x)throws IOException{byte[] b="{\"ok\":true,\"protocol\":1,\"controls\":2,\"rooms\":2,\"maps\":6}".getBytes(java.nio.charset.StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json");x.sendResponseHeaders(200,b.length);try(OutputStream out=x.getResponseBody()){out.write(b);}}
    static final String EMAIL_SCRIPT="""
        (function () {
          var state = window.location.search + '&' + window.location.hash;
          if (/(?:[?#&])error(?:_code)?=/.test(state)) {
            document.getElementById('title').textContent = 'Liên kết xác nhận chưa hoàn tất';
            document.getElementById('message').textContent = 'Liên kết có thể đã hết hạn hoặc đã được dùng. Hãy thử đăng nhập trong game; nếu chưa được, yêu cầu email xác nhận mới.';
          }
          if (window.history && window.history.replaceState) {
            window.history.replaceState(null, '', window.location.pathname);
          }
        }());
        """;
    static String emailPage(){return """
        <!doctype html>
        <html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
        <meta name="referrer" content="no-referrer"><title>Gunny Java — xác nhận email</title>
        <style>body{margin:0;min-height:100vh;display:grid;place-items:center;background:#f4e6c6;color:#3c2b1d;font:17px/1.6 system-ui,sans-serif}main{box-sizing:border-box;max-width:560px;margin:24px;padding:32px;background:#fffaf0;border:2px solid #bd8a38;border-radius:18px;box-shadow:0 10px 36px #3c2b1d22}h1{font-size:26px;line-height:1.3}small{color:#705943}.brand{font-weight:bold;color:#996119;letter-spacing:2px}</style></head>
        <body><main><div class="brand">GUNNY JAVA</div><h1 id="title">Tiếp tục trong Gunny</h1>
        <p id="message">Bạn đã mở liên kết xác nhận email. Hãy quay lại game, chọn Đăng nhập và dùng email cùng mật khẩu đã đăng ký.</p>
        <small>Nếu tài khoản vẫn báo chưa xác nhận email, kiểm tra liên kết còn hiệu lực hoặc yêu cầu email xác nhận mới.</small>
        </main><script>"""+EMAIL_SCRIPT+"</script></body></html>";}
    void emailConfirmation(HttpExchange x)throws IOException{
        String path=x.getRequestURI().getPath();
        if(!path.equals("/")&&!path.equals("/auth/confirmed")){x.sendResponseHeaders(404,-1);x.close();return;}
        if(!x.getRequestMethod().equals("GET")&&!x.getRequestMethod().equals("HEAD")){x.getResponseHeaders().set("Allow","GET, HEAD");x.sendResponseHeaders(405,-1);x.close();return;}
        Headers headers=x.getResponseHeaders();headers.set("Content-Type","text/html; charset=utf-8");headers.set("Cache-Control","no-store");headers.set("Referrer-Policy","no-referrer");headers.set("X-Content-Type-Options","nosniff");
        try{String hash=Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(EMAIL_SCRIPT.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            headers.set("Content-Security-Policy","default-src 'none'; style-src 'unsafe-inline'; script-src 'sha256-"+hash+"'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'");
        }catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
        byte[] body=emailPage().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if(x.getRequestMethod().equals("HEAD")){x.sendResponseHeaders(200,-1);x.close();return;}
        x.sendResponseHeaders(200,body.length);try(OutputStream out=x.getResponseBody()){out.write(body);}
    }
    boolean limit(String key,int maximum){Window w=limits.computeIfAbsent(key,k->new Window());synchronized(w){long now=System.currentTimeMillis();if(now-w.start>60000){w.start=now;w.count=0;}return ++w.count<=maximum;}}
    void api(HttpExchange x)throws IOException {
        byte[] response;int status=200;try{
            if(!x.getRequestMethod().equals("POST"))throw new IllegalArgumentException("Chỉ hỗ trợ POST.");
            // Ignore forwarded IP headers: a public client can forge them.
            String ip=x.getRemoteAddress().getAddress().getHostAddress();if(!limit("all:"+ip,20000))throw new IllegalArgumentException("Quá nhiều yêu cầu, thử lại sau.");
            byte[] body=x.getRequestBody().readNBytes(MAX_BODY+1);if(body.length>MAX_BODY)throw new IllegalArgumentException("Yêu cầu quá lớn.");
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(body));if(in.readInt()!=MAGIC)throw new IllegalArgumentException("Sai phiên bản giao thức.");String action=in.readUTF();
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeByte(0);
            if(action.equals("register")||action.equals("login")){
                if(!limit("auth:"+ip,12))throw new IllegalArgumentException("Đăng nhập quá nhanh. Thử lại sau một phút.");
                String email=in.readUTF().trim().toLowerCase(Locale.ROOT),password=in.readUTF();if(email.length()>254||!email.contains("@")||password.length()<8||password.length()>128)throw new IllegalArgumentException("Nhập email và mật khẩu từ 8 ký tự.");
                if(action.equals("register")){String name=validName(in.readUTF());store.register(email,password,name);out.writeUTF("Đã đăng ký. Hãy kiểm tra email xác nhận rồi đăng nhập.");}
                else{CloudStore.Login login=store.login(email,password);Session s=new Session();s.id=login.userId;s.profile=store.profile(s.id);s.expiry=System.currentTimeMillis()+Math.max(60,login.lifetime)*1000L;s.lastSeen=System.currentTimeMillis();byte[] token=new byte[32];random.nextBytes(token);s.token=Base64.getUrlEncoder().withoutPadding().encodeToString(token);if(sessions.size()>=1024)throw new IllegalStateException("Máy chủ đang đầy.");synchronized(sessions){for(Session old:sessions.values())if(old.id.equals(s.id)){leave(old);sessions.remove(old.token);}sessions.put(s.token,s);}out.writeUTF(s.token);profile(out,s.profile);}
            }else{
                String authorization=x.getRequestHeaders().getFirst("Authorization");Session s=authorization!=null&&authorization.startsWith("Bearer ")?sessions.get(authorization.substring(7)):null;
                if(s==null||s.expiry<System.currentTimeMillis())throw new SecurityException("Phiên đăng nhập hết hạn. Hãy đăng nhập lại.");s.lastSeen=System.currentTimeMillis();s.modern="2".equals(x.getRequestHeaders().getFirst("X-Gunny-Client"));if(!limit("session:"+s.token,900))throw new IllegalArgumentException("Thao tác quá nhanh.");
                synchronized(s){if(action.equals("rooms")){List<Room> list=new ArrayList<>(rooms.values());out.writeShort(list.size());for(Room r:list)synchronized(r){out.writeInt(r.id);out.writeUTF(r.host.profile.name);int n=0;for(Session m:r.members)if(m!=null)n++;out.writeByte(n);out.writeByte(r.count);out.writeByte(r.phase);}}
                else if(action.equals("profile")){s.profile=store.profile(s.id);profile(out,s.profile);}
                else if(action.equals("appearance")){String name=validName(in.readUTF());int sex=in.readUnsignedByte(),skin=in.readUnsignedByte();boolean custom=in.readBoolean();int[] look=new int[5];for(int i=0;i<5;i++){look[i]=in.readUnsignedByte();if(look[i]>9)throw new IllegalArgumentException("Trang phục không hợp lệ.");}if(sex>1||skin>9)throw new IllegalArgumentException("Trang phục không hợp lệ.");if(s.room!=0)throw new IllegalArgumentException("Rời phòng trước khi thay đồ.");s.profile=store.appearance(s.id,name,sex,skin,custom,look);profile(out,s.profile);}
                else if(action.equals("buy")){int weapon=in.readUnsignedByte();if(!validWeapon(weapon))throw new IllegalArgumentException("Vũ khí không hợp lệ.");if(s.room!=0)throw new IllegalArgumentException("Rời phòng trước khi mua đồ.");s.profile=store.buy(s.id,weapon);profile(out,s.profile);}
                else if(action.equals("create")||action.equals("create2")){int map=in.readByte(),ally=in.readUnsignedByte(),bot=in.readByte();boolean modern=action.equals("create2");int duration=modern?in.readUnsignedByte():20;if(modern&&!s.modern)throw new IllegalArgumentException("Cập nhật game để tạo phòng mới.");if(map< -1||map>=(modern?6:3)||modern&&duration!=8&&duration!=12&&duration!=15||ally>2||bot< -1||bot>7)throw new IllegalArgumentException("Thiết lập phòng không hợp lệ.");synchronized(rooms){if(rooms.size()>=maxRooms)throw new IllegalArgumentException("Hết phòng trống, thử lại sau.");leave(s);Room room=new Room(s,map,ally,bot);room.strict=modern;room.mapLimit=modern?6:3;room.seconds=duration;room.refreshWaiting();rooms.put(room.id,room);s.room=room.id;s.held=0;lobby(out,room,s);}}
                else if(action.equals("join")){int id=in.readInt(),team=in.readUnsignedByte();if(team>2||team==2&&!s.modern)throw new IllegalArgumentException("Phe không hợp lệ.");Room room=rooms.get(id);if(room==null)throw new IllegalArgumentException("Không tìm thấy phòng.");if(room.strict&&!s.modern)throw new IllegalArgumentException("Cập nhật game để vào phòng này.");boolean automatic=team==2;if(s.room==id){synchronized(room){lobby(out,room,s);}}else{leave(s);synchronized(room){if(automatic)team=room.slot(room.host)%2==0?1:0;if(rooms.get(id)!=room||room.phase!=0)throw new IllegalArgumentException("Phòng đã đóng hoặc trận đã bắt đầu.");int target=-1;for(int i=team;i<room.count;i+=2)if(room.members[i]==null&&!(i==2&&room.allyMode==1)){target=i;break;}if(target<0)throw new IllegalArgumentException("Phe đã đủ người.");room.members[target]=s;s.room=id;s.held=0;s.armed=false;room.refreshWaiting();lobby(out,room,s);}}}
                else if(action.equals("leave")){leave(s);out.writeUTF("Đã rời phòng.");out.writeInt(s.sequence);}
                else if(action.equals("logout")){leave(s);sessions.remove(s.token);out.writeUTF("Đã đăng xuất.");out.writeInt(s.sequence);}
                else if(action.equals("roomsettings")){Room room=requireRoom(s);int choice=in.readByte(),duration=in.readUnsignedByte();synchronized(room){room.settings(s,choice,duration);lobby(out,room,s);}}
                else if(action.equals("ready")){Room room=requireRoom(s);boolean ready=in.readBoolean();synchronized(room){if(room.phase!=0)throw new IllegalArgumentException("Trận đã bắt đầu.");s.armed=ready;lobby(out,room,s);}}
                else if(action.equals("switchteam")){Room room=requireRoom(s);int team=in.readUnsignedByte();synchronized(room){room.switchTeam(s,team);lobby(out,room,s);}}
                else if(action.equals("start")){Room room=requireRoom(s);synchronized(room){if(room.host!=s)throw new IllegalArgumentException("Chỉ chủ phòng được bắt đầu.");room.start();lobby(out,room,s);}}
                else if(action.equals("sync")){Room room=requireRoom(s);int revision=in.readInt(),held=in.readUnsignedByte();boolean ready=in.readBoolean();int count=in.readUnsignedByte();if(held>15||count>16)throw new IllegalArgumentException("Lệnh không hợp lệ.");synchronized(room){s.held=held;s.ready=ready;if(room.phase!=3)room.lastActive=s.lastSeen;for(int i=0;i<count;i++)room.command(s,in.readInt(),in.readUnsignedByte(),in.readShort());out.writeInt(s.sequence);lobby(out,room,s);if(room.phase>0)snapshot(out,room,revision,s);}}
                else throw new IllegalArgumentException("Không có thao tác này.");}
            }
            out.flush();response=bytes.toByteArray();
        }catch(Exception e){ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeByte(e instanceof SecurityException?2:e instanceof RoomMissing?3:1);String message=(e instanceof IllegalArgumentException||e instanceof IllegalStateException||e instanceof SecurityException)?e.getMessage():"Mất kết nối dịch vụ. Hãy thử lại.";out.writeUTF(message==null?"Không xử lý được yêu cầu.":message);out.flush();response=bytes.toByteArray();}
        x.getResponseHeaders().set("Content-Type","application/octet-stream");x.getResponseHeaders().set("Cache-Control","no-store");x.getResponseHeaders().set("X-Content-Type-Options","nosniff");x.sendResponseHeaders(status,response.length);try(OutputStream out=x.getResponseBody()){out.write(response);}finally{x.close();}
    }
    static boolean validWeapon(int id){return id==0||id==3||id==5||id==9;}
    static String validName(String value){value=value.trim();if(value.isEmpty()||value.length()>13||value.chars().anyMatch(c->c<32))throw new IllegalArgumentException("Tên từ 1 đến 13 ký tự.");return value;}
    static final class RoomMissing extends IllegalArgumentException {RoomMissing(){super("Phòng đã đóng. Hãy về danh sách phòng.");}}
    Room requireRoom(Session s){Room room=rooms.get(s.room);if(room==null)throw new RoomMissing();return room;}
    void leave(Session s){Room r=rooms.get(s.room);s.room=0;s.held=0;s.armed=false;if(r!=null)synchronized(r){for(int i=0;i<r.count;i++)if(r.members[i]==s)r.members[i]=null;if(r.phase==0){if(r.host==s){rooms.remove(r.id);for(Session m:r.members)if(m!=null)m.room=0;}else r.refreshWaiting();}else{for(int i=0;i<r.count;i++)if(r.avatars[i].owner==s){r.avatars[i].owner=null;r.avatars[i].bot=true;}s.lastSeen=0;}}}
    static void profile(DataOutputStream out,CloudStore.Profile p)throws IOException {out.writeUTF(p.name);out.writeByte(p.gender);out.writeByte(p.skin);out.writeBoolean(p.custom);for(int v:p.look)out.writeByte(v);out.writeInt(p.coins);out.writeInt(p.wins);out.writeInt(p.owned);out.writeByte(p.equipped);}
    void lobby(DataOutputStream out,Room r,Session viewer)throws IOException {out.writeInt(r.id);out.writeByte(r.phase);out.writeBoolean(viewer==r.host);out.writeByte(r.phase==0?(r.strict?r.map:r.requestedMap):r.map);out.writeByte(r.count);out.writeBoolean(r.settled);
        for(int i=0;i<r.count;i++){Avatar a=r.avatars[i];out.writeUTF(r.phase==0&&r.members[i]==null&&!(i==2&&r.bot>=0)?"Đang chờ":a.name);out.writeByte(a.sex);out.writeByte(a.skin);out.writeBoolean(a.custom);for(int v:a.look)out.writeByte(v);out.writeByte(a.weapon);out.writeBoolean(a.owner==viewer);out.writeBoolean(r.members[i]!=null||i==2&&r.bot>=0);out.writeBoolean(a.bot);}if(viewer.modern){out.writeByte(r.requestedMap);out.writeByte(r.seconds);int mask=0;for(int i=0;i<r.count;i++)if(r.members[i]!=null&&(r.members[i]==r.host||r.members[i].armed))mask|=1<<i;out.writeByte(mask);out.writeBoolean(r.canStart());out.writeBoolean(r.strict);}}
    void snapshot(DataOutputStream out,Room r,int revision,Session viewer)throws IOException {
        Battle b=r.battle;out.writeInt(b.turn);out.writeByte(b.active);out.writeShort(b.wind);out.writeShort(b.lastWind);out.writeShort(b.countdown);out.writeByte(b.winner);out.writeBoolean(b.over);out.writeBoolean(b.charging);out.writeBoolean(b.pow);out.writeBoolean(b.fly);out.writeBoolean(b.triple);out.writeByte(b.extra);out.writeByte(b.boost);out.writeInt(b.shotSerial);out.writeInt(b.hitSerial);out.writeShort(r.powTicks);out.writeByte(r.powActor);out.writeShort(b.bursts);out.writeShort(b.settle);out.writeShort(b.burstDelay);out.writeShort(b.botPhase);out.writeByte(b.usedPropCount);for(int i=0;i<b.usedPropCount;i++)out.writeByte(b.usedPropHistory[i]);
        for(int i=0;i<r.count;i++){Battle.Unit u=b.units[i];for(int v:new int[]{u.x,u.y,u.vy,u.hp,u.energy,u.rage,u.angle,r.controls(viewer,i)?u.power:0,u.face,u.walk,u.weapon,u.attackTicks,u.slope,u.heals,u.deathX,u.deathY,r.controls(viewer,i)?u.lastPower:-1,r.controls(viewer,i)?u.pendingPower:-1,u.flashTicks,u.landingTicks,u.lastBuff})out.writeShort(v);out.writeBoolean(u.dead);}
        out.writeByte(b.ballCount);for(int i=0;i<b.ballCount;i++){Battle.Ball ball=b.balls[i];for(int v:new int[]{ball.x,ball.y,ball.vx,ball.vy})out.writeInt(v);out.writeByte(ball.owner);out.writeShort(ball.age);out.writeByte(ball.weapon);out.writeBoolean(ball.fly);out.writeBoolean(ball.powered);}
        out.writeByte(b.blastCount);for(int i=0;i<b.blastCount;i++){Battle.Blast f=b.blasts[i];out.writeShort(f.x);out.writeShort(f.y);out.writeByte(f.radius);out.writeByte(f.time);out.writeByte(f.weapon);}
        out.writeByte(b.damagePopupCount);for(int i=0;i<b.damagePopupCount;i++){Battle.DamagePopup f=b.damagePopups[i];out.writeShort(f.x);out.writeShort(f.y);out.writeShort(f.value);out.writeByte(f.age);out.writeBoolean(f.powered);}
        revision=Math.max(0,Math.min(revision,r.ground.craters.size()));int end=r.ground.craters.size();out.writeInt(end);out.writeShort(end-revision);for(int i=revision;i<end;i++)for(int v:r.ground.craters.get(i))out.writeShort(v);
    }
    void queueSettlement(Room r){if(r.settlementQueued)return;r.settlementQueued=true;List<Map<String,Object>> players=new ArrayList<>(r.participants);
        saves.submit(()->{for(int attempt=0;attempt<10&&!closed;attempt++){try{store.settle(r.match,r.battle.winner,players);for(Session s:r.members)if(s!=null)s.profile=store.profile(s.id);r.settled=true;return;}catch(Exception ignored){try{Thread.sleep(Math.min(30000,1000L<<attempt));}catch(InterruptedException e){Thread.currentThread().interrupt();return;}}}System.err.println("Match settlement pending: "+r.match);});
    }
    void tick(){try{long now=System.currentTimeMillis();for(Room r:rooms.values())synchronized(r){boolean present=false;for(Session s:r.members)if(s!=null&&now-s.lastSeen<300000)present=true;if(!present||r.phase==2&&now-r.lastActive>180000)rooms.remove(r.id);else r.tick();}for(Session s:sessions.values())if(s.expiry<now){leave(s);sessions.remove(s.token);}if(limits.size()>1024)limits.entrySet().removeIf(e->now-e.getValue().start>60000);}catch(Exception e){System.err.println("Battle tick failed: "+e.getClass().getSimpleName());}}
    public void close(){closed=true;http.stop(0);clock.shutdownNow();workers.shutdownNow();saves.shutdownNow();}
    public static void main(String[] args)throws Exception {boolean test=CloudStore.env("GUNNY_TEST_MODE").equals("1");if(test&&!CloudStore.env("RENDER").isEmpty())throw new IllegalStateException("Test accounts are prohibited on Render");int port=Integer.parseInt(CloudStore.env("PORT").isEmpty()?"8080":CloudStore.env("PORT"));String bind=test?"127.0.0.1":"0.0.0.0";GunnyServer server=new GunnyServer(new InetSocketAddress(bind,port),Path.of(CloudStore.env("GUNNY_ASSETS").isEmpty()?"assets1":CloudStore.env("GUNNY_ASSETS")),test?new CloudStore.Memory():new CloudStore());Runtime.getRuntime().addShutdownHook(new Thread(server::close));System.out.println("Gunny protocol 1 listening on "+bind+":"+server.port()+(test?" (local test accounts only)":""));}
}
