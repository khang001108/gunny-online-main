/** Fixed-point artillery rules for CLDC 1.1. No desktop or Android APIs. */
final class Battle {
    static final int W=480,H=220,MAXHP=2000;
    static final int AIM_RADIUS=60;
    static final int AIM_RAY_LENGTH=78;
    static final int THROW_WINDUP=6;
    static boolean isThrown(int weapon){return weapon==1||weapon==2||weapon==3||weapon==4||weapon==5||weapon==7;}
    // Provisional Java ME tuning, not recovered original-client weapon data.
    static int weaponMinAngle(int weapon){return weapon==5?0:weapon==4?15:weapon==2?20:weapon==7?10:weapon==1?15:weapon==3?10:weapon==9?15:20;}
    static int weaponMaxAngle(int weapon){return weapon==5?90:weapon==4?70:weapon==2?75:weapon==7?80:weapon==1?60:weapon==3?80:65;}
    static int projectileSize(int weapon){return weapon==4||weapon==5?32:16;}
    interface Ground { boolean solid(int x,int y); int surface(int x,int from); void crater(int x,int y,int r); }
    final Ground ground;
    interface ShotObserver {void beforeShot();}
    ShotObserver shotObserver;
    final Unit[] units={new Unit(),new Unit(),new Unit(),new Unit(),new Unit(),new Unit()};int unitCount=2;boolean teams;
    boolean explicitHumans;boolean humanTurn(){return explicitHumans?units[active].human:teams?units[active].human:hotseat||active==0;}
    int enemyOf(int actor){int best=actor==0?1:0,distance=99999;for(int i=0;i<unitCount;i++)if(i!=actor&&units[i].hp>0&&(!teams||units[i].team!=units[actor].team)){int d=Math.abs(units[i].x-units[actor].x);if(d<distance){distance=d;best=i;}}return best;}
    static int gravity(Ball b){return b.fly||b.freeze?11:b.weapon==4?14:b.weapon==2?10:b.weapon==7?12:11;}
    static int windStep(int wind,Ball b){return wind/(b.fly||b.freeze?7:b.weapon==4?11:b.weapon==2?5:b.weapon==7?8:b.weapon==1?9:7);}
    static int baseDamage(int weapon){return weapon==0?290:weapon==1?390:weapon==2?300:weapon==4?370:weapon==7?315:330;}
    static int baseRadius(int weapon){return weapon==0?15:weapon==1?10:weapon==2?10:weapon==4?17:weapon==7?14:12;}
    void configureTeam(int ally){teams=true;unitCount=4;hotseat=false;int roster=rand(4);for(int i=0;i<4;i++){Unit u=units[i];u.team=i%2;u.human=i==0||i==2&&ally==2;u.hp=MAXHP;u.energy=240;u.rage=0;u.heals=5;u.dead=false;u.vy=u.walk=u.attackTicks=u.slope=u.flashTicks=u.landingTicks=u.deathX=u.deathY=u.lastBuff=0;u.angle=45;u.power=65;u.lastPower=u.pendingPower=-1;u.x=i==0?70:i==2?140:i==1?400:330;u.y=ground.surface(u.x,35);u.face=u.team==0?1:-1;u.botStyle=(roster+i)%4;u.weapon=new int[]{0,3,5,9}[(roster+i)%4];refreshAim(u);}}
    void configureOnlineTeam(){configureTeam(1);unitCount=6;for(int i=0;i<6;i++){Unit u=units[i];u.team=i%2;u.human=true;u.hp=MAXHP;u.energy=240;u.rage=0;u.heals=5;u.dead=false;u.vy=0;u.angle=45;u.power=65;u.lastPower=u.pendingPower=-1;u.x=i%2==0?60+i/2*55:420-i/2*55;u.y=ground.surface(u.x,35);u.face=i%2==0?1:-1;refreshAim(u);}}
    void completeTurn(){pendingNext=false;boolean deferred=deferNext;deferNext=false;next();deferNext=deferred;}
    final Ball[] balls=new Ball[20];
    final Blast[] blasts=new Blast[16];
    static final class DamagePopup {int x,y,value,age;boolean powered;}
    final DamagePopup[] damagePopups=new DamagePopup[20];int damagePopupCount;
    void showDamage(Unit u,int value,boolean powered){if(value<=0)return;DamagePopup popup=new DamagePopup();popup.x=u.x;popup.y=u.y-34;popup.value=value;popup.powered=powered;if(damagePopupCount==damagePopups.length){for(int i=1;i<damagePopupCount;i++)damagePopups[i-1]=damagePopups[i];damagePopupCount--;}damagePopups[damagePopupCount++]=popup;}
    // Bounded visual-only history; no per-frame particle allocation on CLDC.
    static final int TRAIL_CAPACITY=192;
    final int[] trailX=new int[TRAIL_CAPACITY],trailY=new int[TRAIL_CAPACITY],trailWeapon=new int[TRAIL_CAPACITY],trailFrame=new int[TRAIL_CAPACITY],trailAge=new int[TRAIL_CAPACITY],trailLife=new int[TRAIL_CAPACITY];
    int trailCount;
    int ballCount,blastCount,active,turn,wind,lastWind,countdown,bursts,burstDelay,settle,botWait;
    int turnLimit=600;boolean deferNext,pendingNext;
    final int[] damage=new int[6],kills=new int[6];int extra,boost,weapon,shotSerial,hitSerial,winSerial;
    int selectedWeapon,usedProps,usedPropCount;
    final int[] usedPropHistory=new int[16];
    int viewX=-25,viewY=-10000,viewWidth=W+50,viewHeight=H+10035;
    boolean sequentialAttack;
    boolean botActing,deferBotPow,botPowQueued;int botBlocked,botPhase,botTargetAngle,botTargetPower;
    void releaseBotPow(){if(!botPowQueued)return;botPowQueued=false;if(humanTurn()||!pow||over||busy())return;botActing=true;try{fire();}finally{botActing=false;}}
    boolean freezeShot,triple,pow,fly,charging,over,hotseat,cheatAim;
    int chargeDir=1;
    int winner=-1;
    int seed=37183;
    static final int[] SIN=new int[91];
    static {for(int i=0;i<=90;i++)SIN[i]=(int)(Math.sin(i*Math.PI/180)*1024);}
    static final int[] ITEM_COST={50,150,170,150,120,120};
    static final String[] ITEM_NAMES={"Tàng hình","Đạn băng","Hồi máu nhóm","Tàng hình nhóm","Dẫn đường","Kích nộ"};
    static boolean validKit(int[] kit){return kit!=null&&kit.length==3&&kit[0]>=0&&kit[0]<6&&kit[1]>=0&&kit[1]<6&&kit[2]>=0&&kit[2]<6&&kit[0]!=kit[1]&&kit[0]!=kit[2]&&kit[1]!=kit[2];}
    static final class Unit {final int[] kit={0,1,2};int itemUsed,frozen,freezeTurns,invisible,flyCooldown;boolean assist,flightAim;int team,botStyle=-1;boolean human;int x,y,vy,hp=MAXHP,energy=240,rage,angle=45,power=60,face=1,walk,weapon,attackTicks,slope,heals=5,deathX,deathY,lastPower=-1,pendingPower=-1,flashTicks,landingTicks,lastBuff;boolean dead;}
    static final class Ball {int x,y,vx,vy,owner,age,damage,radius,weapon;boolean guided,freeze,fly,homing,powered;}
    static final class Blast {boolean freeze;int x,y,radius,time,damage,owner,weapon;}
    Battle(Ground ground){this.ground=ground;}
    int rand(int n){seed=seed*1103515245+12345;return (seed>>>1)%n;}
    void start(boolean two){
        explicitHumans=false;deferNext=pendingNext=false;teams=false;unitCount=2;active=0;turn=1;over=false;hotseat=two;winner=-1;ballCount=blastCount=bursts=settle=trailCount=0;
        for(int i=0;i<6;i++){damage[i]=kills[i]=0;Unit u=units[i];u.itemUsed=u.frozen=u.freezeTurns=u.invisible=u.flyCooldown=0;u.assist=false;u.kit[0]=0;u.kit[1]=1;u.kit[2]=2;}damagePopupCount=0;for(int i=0;i<damagePopups.length;i++)damagePopups[i]=null;shotSerial=hitSerial=winSerial=0;wind=rand(61)-30;lastWind=0;countdown=turnLimit;botWait=0;selectedWeapon=0;
        for(int i=0;i<unitCount;i++){Unit u=units[i];u.botStyle=-1;u.x=i==0?100:350;u.y=ground.surface(u.x,35);u.vy=0;u.hp=MAXHP;u.energy=240;u.rage=0;u.angle=45;u.power=65;u.lastPower=u.pendingPower=-1;u.face=i==0?1:-1;u.walk=0;u.attackTicks=0;u.slope=0;u.heals=5;u.flashTicks=u.landingTicks=0;u.dead=false;u.deathX=u.deathY=0;refreshAim(u);}
        clear();
    }
    void clear(){for(int i=0;i<6;i++)units[i].flightAim=false;freezeShot=false;botPhase=0;botPowQueued=false;sequentialAttack=false;usedProps=usedPropCount=0;extra=boost=0;triple=pow=fly=charging=false;}
    boolean busy(){return ballCount>0||bursts>0||settle>0;}
    boolean input(){return units[active].frozen!=2&&!over&&!pendingNext&&!busy()&&(humanTurn()||botActing);}
    static int clamp(int value,int low,int high){return Math.max(low,Math.min(high,value));}
    static int sin(int angle){angle%=360;if(angle<0)angle+=360;return angle<=90?SIN[angle]:angle<=180?SIN[180-angle]:angle<=270?-SIN[angle-180]:-SIN[360-angle];}
    static int cos(int angle){return sin(90+angle);}
    static int minLocalAngle(Unit u){return u.flightAim?0:weaponMinAngle(u.weapon);}
    static int maxLocalAngle(Unit u){return u.flightAim?90:weaponMaxAngle(u.weapon);}
    static int minAimAngle(Unit u){return minLocalAngle(u)+u.slope*u.face;}
    static int maxAimAngle(Unit u){return maxLocalAngle(u)+u.slope*u.face;}
    static int aimAngle(Unit u,int local){return clamp(local,minLocalAngle(u),maxLocalAngle(u))+u.slope*u.face;}
    static int aimAngle(Unit u){return aimAngle(u,u.angle);}
    static int aimCos(Unit u){return cos(aimAngle(u))*u.face;}
    static int aimSin(Unit u){return sin(aimAngle(u));}
    static int launchBaseY(Unit u){return u.y-(isThrown(u.weapon)?(u.weapon==3?11:6):14);}
    // Face-local anchor: recompute from the unit, never from screen coordinates.
    static int guideOriginX(Unit u){return u.x+u.face*6;}
    static int guideOriginY(Unit u){return u.y-16;}
    static int aimPointX(Unit u,int distance){return guideOriginX(u)+aimCos(u)*distance/1024;}
    static int aimPointY(Unit u,int distance){return guideOriginY(u)-aimSin(u)*distance/1024;}
    void refreshAim(Unit u){
        if(ground.solid(u.x-2,u.y+1)||ground.solid(u.x+2,u.y+1)){
            int left=ground.surface(u.x-6,Math.max(0,u.y-18)),right=ground.surface(u.x+6,Math.max(0,u.y-18));
            // Do not interpret a missing ledge or a different floor as a slope.
            if(left<H&&right<H&&Math.abs(left-u.y)<=20&&Math.abs(right-u.y)<=20){
                int rise=left-right,dy=Math.abs(rise),best=0,error=999999;
                for(int angle=0;angle<=65;angle++){
                    int cross=Math.abs(dy*SIN[90-angle]-12*SIN[angle]);
                    if(cross<error){error=cross;best=angle;}
                }
                u.slope=rise<0?-best:best;
            }
        }
        u.angle=clamp(u.angle,minLocalAngle(u),maxLocalAngle(u));
    }
    void adjustAngle(int delta){Unit u=units[active];refreshAim(u);u.angle=clamp(u.angle+delta,minLocalAngle(u),maxLocalAngle(u));}
    int guideDistance(Unit u){
        for(int distance=4;distance<=AIM_RAY_LENGTH;distance++)if(ground.solid(aimPointX(u,distance),aimPointY(u,distance)))return distance;
        return AIM_RAY_LENGTH;
    }
    boolean occupied(int x,int except,int radius){for(int i=0;i<unitCount;i++)if(i!=except&&units[i].hp>0&&Math.abs(x-units[i].x)<radius)return true;return false;}
    void move(int dir){
        if(!input())return;Unit u=units[active];u.face=dir;
        refreshAim(u);int nx=u.x+dir;if(u.energy<2||nx<12||nx>=W-12||occupied(nx,active,20))return;
        int rise=0;while(rise<6&&(ground.solid(nx-2,u.y-rise-1)||ground.solid(nx+2,u.y-rise-1)))rise++;
        if(rise>=6)return;u.x=nx;u.y-=rise;u.energy-=2;u.walk++;refreshAim(u);
    }
    boolean prop(int id){
        if(!input()||id<0||id>5)return false;Unit u=units[active];int cost=id==0||id==1?110:id==2?90:id==3||id==4?150:id==6?70:id>=7&&id<=10?120-(id-6)*10:0;
        if(u.energy<cost||freezeShot&&(id==0||id==1||id==2||id==4||id==5))return false;
        if(id==0){if(extra>2)return false;extra+=2;fly=false;}
        else if(id==1){if(triple)return false;triple=true;fly=false;}
        else if(id==2){if(boost>=100)return false;boost+=50;fly=false;}
        else if(id==3){if(u.hp==MAXHP||u.heals<=0)return false;u.hp=Math.min(MAXHP,u.hp+500);u.heals--;}
        else if(id==4){if(u.flyCooldown>0)return false;u.flyCooldown=3;freezeShot=false;extra=boost=0;triple=pow=false;fly=true;}
        else if(id==5){if(pow||u.rage<100)return false;u.rage=0;pow=true;fly=false;}
        else if(id==6){if(extra>2)return false;extra++;fly=false;}
        else if(id>=7&&id<=10){int amount=(11-id)*10;if(boost+amount>100)return false;boost+=amount;fly=false;}
        else return false;
        u.energy-=cost;u.flightAim=fly;refreshAim(u);if(id!=5||pow){u.flashTicks=36;u.lastBuff=id;}
        if(id==5&&!pow){
            usedProps&=~(1<<5);
            for(int i=usedPropCount-1;i>=0;i--)if(usedPropHistory[i]==5){for(int j=i;j<usedPropCount-1;j++)usedPropHistory[j]=usedPropHistory[j+1];usedPropCount--;break;}
        }else{
            usedProps|=1<<id;if(usedPropCount<usedPropHistory.length)usedPropHistory[usedPropCount++]=id;
        }
        return true;
    }
    boolean ally(int actor,int other){return actor==other||teams&&units[actor].team==units[other].team;}
    boolean item(int slot){
        if(!input()||charging||slot<0||slot>=3)return false;Unit u=units[active];int id=u.kit[slot];
        if(id<0||id>=6||(u.itemUsed&(1<<slot))!=0||u.energy<ITEM_COST[id])return false;
        if(id==0){if(u.invisible>0)return false;u.invisible=2;}
        else if(id==1){if(freezeShot||fly||bursts>0)return false;freezeShot=true;extra=boost=0;triple=pow=false;u.assist=false;}
        else if(id==2){boolean hurt=false;for(int i=0;i<unitCount;i++)if(units[i].hp>0&&ally(active,i)&&units[i].hp<MAXHP)hurt=true;if(!hurt)return false;for(int i=0;i<unitCount;i++)if(units[i].hp>0&&ally(active,i))units[i].hp=Math.min(MAXHP,units[i].hp+300);}
        else if(id==3){boolean visible=false;for(int i=0;i<unitCount;i++)if(units[i].hp>0&&ally(active,i)&&units[i].invisible==0)visible=true;if(!visible)return false;for(int i=0;i<unitCount;i++)if(units[i].hp>0&&ally(active,i))units[i].invisible=Math.max(2,units[i].invisible);}
        else if(id==4){if(u.assist||fly||freezeShot)return false;u.assist=true;}
        else if(id==5){if(u.rage>=100)return false;u.rage=Math.min(100,u.rage+50);}
        u.energy-=ITEM_COST[id];u.itemUsed|=1<<slot;return true;
    }
    void charge(){if(input()){refreshAim(units[active]);charging=true;chargeDir=1;units[active].power=0;}}
    void fire(){if(!input())return;refreshAim(units[active]);if(shotObserver!=null)shotObserver.beforeShot();charging=false;units[active].pendingPower=units[active].power;sequentialAttack=extra>0;bursts=1+extra;burstDelay=freezeShot||isThrown(units[active].weapon)?THROW_WINDUP:0;if(pow)units[active].rage=0;else units[active].rage=Math.min(100,units[active].rage+10);}
    void pass(){if(input()){next();}}
    void tick(){
        ageTrails();
        for(int i=0;i<unitCount;i++){if(units[i].attackTicks>0)units[i].attackTicks--;if(units[i].flashTicks>0)units[i].flashTicks--;if(units[i].landingTicks>0)units[i].landingTicks--;}
        for(int i=damagePopupCount-1;i>=0;i--)if(++damagePopups[i].age>=42){for(int j=i+1;j<damagePopupCount;j++)damagePopups[j-1]=damagePopups[j];damagePopups[--damagePopupCount]=null;}
        for(int i=blastCount-1;i>=0;i--){blasts[i].time++;if(blasts[i].time>24){blasts[i]=blasts[--blastCount];blasts[blastCount]=null;}}
        if(over||pendingNext)return;
        if(units[active].frozen==2){if(--countdown<=0)next();return;}
        for(int n=0;n<unitCount;n++){
            Unit u=units[n];if(u.hp<=0)continue;
            if(!ground.solid(u.x-2,u.y+1)&&!ground.solid(u.x+2,u.y+1)){
                u.vy+=44;int dy=Math.max(1,u.vy/256),surface=ground.surface(u.x,u.y+1);
                u.y=Math.min(surface,u.y+dy);if(u.y==surface)u.vy=0;if(u.y>H+20)u.hp=0;
            }else u.vy=0;
            refreshAim(u);
        }
        boolean wasBusy=busy();
        if(bursts>0&&ballCount==0&&--burstDelay<=0){check();if(over){bursts=0;return;}emit();bursts--;burstDelay=isThrown(units[active].weapon)?THROW_WINDUP:2;}
        for(int sub=0;sub<4;sub++)for(int i=ballCount-1;i>=0;i--){
            Ball b=balls[i];if(sub==0&&!b.fly&&!b.freeze)stampTrail(b);b.age++;if(b.homing){Unit target=units[enemyOf(b.owner)];int dx=target.x*256-b.x,dy=(target.y-12)*256-b.y;int len=Math.max(1,(int)Math.sqrt((double)dx*dx+(double)dy*dy));b.vx=dx*1800/len;b.vy=dy*1800/len;}else{b.vx+=windStep(wind,b);b.vy+=gravity(b);if(b.guided)guide(b);}b.x+=b.vx/4;b.y+=b.vy/4;
            int x=b.x/256,y=b.y/256;boolean hit=!b.homing&&ground.solid(x,y);
            if(!b.fly)for(int n=0;n<unitCount;n++){Unit u=units[n];int dx=x-u.x,dy=y-(u.y-12);if(u.hp>0&&(n!=b.owner||b.age>24)&&dx*dx+dy*dy<70)hit=true;}
            if(hit){impact(b);removeBall(i);}
            else if((sequentialAttack&&(x<viewX||x>=viewX+viewWidth||y<viewY||y>=viewY+viewHeight))||x< -25||x>W+25||y>H+35||b.age>1600)removeBall(i);
        }
        if(wasBusy&&ballCount==0&&bursts==0&&settle<=0)settle=24;
        if(settle>0){if(--settle==0){check();if(!over)next();}return;}
        if(busy())return;check();if(over)return;
        if(!humanTurn()){botTurn();return;}
        if(charging){Unit u=units[active];u.power+=2*chargeDir;if(u.power>=100){u.power=100;chargeDir=-1;}else if(u.power<=0){u.power=0;chargeDir=1;}}
        if(--countdown<=0)next();
    }
    static int ballFrame(Ball b){
        if(b.weapon!=6&&b.weapon!=9)return b.age/8%8;
        int ax=Math.abs(b.vx),ay=Math.abs(b.vy);if(ax>ay*2)return b.vx>0?0:4;if(ay>ax*2)return b.vy<0?2:6;return b.vx>0?(b.vy<0?1:7):(b.vy<0?3:5);
    }
    void guide(Ball b){int best=-1;long distance=100L*100*256*256;for(int i=0;i<unitCount;i++)if(units[i].hp>0&&!ally(b.owner,i)){long dx=units[i].x*256L-b.x,dy=(units[i].y-12)*256L-b.y,d=dx*dx+dy*dy;if(d<distance){distance=d;best=i;}}if(best<0)return;Unit t=units[best];int dx=t.x*256-b.x,dy=(t.y-12)*256-b.y,len=Math.max(1,(int)Math.sqrt((double)dx*dx+(double)dy*dy));int speed=Math.max(500,(int)Math.sqrt((double)b.vx*b.vx+(double)b.vy*b.vy));b.vx+=(dx*speed/len-b.vx)/12;b.vy+=(dy*speed/len-b.vy)/12;}
    void stampTrail(Ball b){
        if(trailCount==TRAIL_CAPACITY)return;
        int i=trailCount++;trailX[i]=b.x;trailY[i]=b.y;trailWeapon[i]=b.weapon;trailFrame[i]=ballFrame(b);trailAge[i]=0;
        // Original particle lifetimes: brick 0.2s, dart/electric/grenade 0.5s.
        // Sample once per 30Hz game tick; retained positions follow the real arc.
        trailLife[i]=b.weapon==3?6:15;
    }
    void ageTrails(){
        int count=0;
        for(int i=0;i<trailCount;i++)if(++trailAge[i]<trailLife[i]){
            if(count!=i){trailX[count]=trailX[i];trailY[count]=trailY[i];trailWeapon[count]=trailWeapon[i];trailFrame[count]=trailFrame[i];trailAge[count]=trailAge[i];trailLife[count]=trailLife[i];}count++;
        }
        trailCount=count;
    }
    void removeBall(int i){balls[i]=balls[--ballCount];balls[ballCount]=null;}
    Ball make(int angle,int power){
        Unit u=units[active];angle=aimAngle(u,angle);int cs=cos(angle)*u.face,sn=sin(angle);
        Ball b=new Ball();b.x=u.x*256+cs*4;b.y=launchBaseY(u)*256-sn*4;
        if(freezeShot||isThrown(u.weapon)){b.x=u.x*256;b.y=launchBaseY(u)*256;}
        int speed=(25+power*2)*256/24;b.vx=cs*speed/1024;b.vy=-sn*speed/1024;b.owner=active;b.fly=fly;b.homing=cheatAim&&active==0&&!fly&&!freezeShot;b.guided=u.assist&&!fly&&!freezeShot;
        b.freeze=freezeShot;b.powered=pow;b.weapon=u.weapon;b.radius=pow?Math.max(20,baseRadius(u.weapon)+5):baseRadius(u.weapon);
        int base=baseDamage(u.weapon);
        b.damage=base*(100+boost)/100;if(triple)b.damage=b.damage*60/100;if(extra>0)b.damage=b.damage*82/100;if(pow)b.damage=b.damage*170/100;
        if(b.guided)b.damage=b.damage/2;
        return b;
    }
    void emit(){
        Unit u=units[active];refreshAim(u);u.attackTicks=8;for(int n=triple?-1:0;n<=(triple?1:0);n++)if(ballCount<balls.length)balls[ballCount++]=make(u.angle+n*7,u.power);
        shotSerial++;
    }
    void impact(Ball b){
        int x=b.x/256,y=b.y/256;
        if(b.fly){int surface=ground.surface(x,Math.max(0,y-18));if(surface<H&&x>12&&x<W-12&&!occupied(x,b.owner,20)){units[b.owner].x=x;units[b.owner].y=surface;units[b.owner].vy=0;units[b.owner].landingTicks=32;}return;}
        if(b.freeze){Blast ice=new Blast();ice.freeze=true;ice.x=x;ice.y=y;ice.weapon=b.weapon;if(blastCount<blasts.length)blasts[blastCount++]=ice;for(int i=0;i<unitCount;i++){Unit u=units[i];int dx=x-u.x,dy=y-u.y+12;if(u.hp>0&&!ally(b.owner,i)&&dx*dx+dy*dy<31*31){u.freezeTurns=2;u.frozen=1;}}hitSerial++;return;}
        Blast fx=new Blast();fx.x=x;fx.y=y;fx.radius=b.radius;fx.owner=b.owner;fx.weapon=b.weapon;
        if(blastCount<blasts.length)blasts[blastCount++]=fx;
        for(int i=0;i<unitCount;i++){
            Unit u=units[i];int dx=x-u.x,dy=y-u.y+12,dist=dx*dx+dy*dy,r=b.radius+23;
            if(u.hp>0&&dist<r*r){int damage=b.damage*(100-(int)Math.sqrt(dist)*65/r)/100;if(u.freezeTurns>0){u.freezeTurns=u.frozen=0;damage=damage/2;}int received=Math.min(u.hp,damage);u.hp=Math.max(0,u.hp-damage);if(i!=b.owner&&(!teams||u.team!=units[b.owner].team)){this.damage[b.owner]+=received;if(u.hp==0)kills[b.owner]++;}u.rage=Math.min(100,u.rage+(received*100+MAXHP-1)/MAXHP);fx.damage=damage;showDamage(u,damage,b.powered);}
        }
        ground.crater(x,y,b.radius);hitSerial++;
    }
    void check(){boolean left=false,right=false;for(int i=0;i<unitCount;i++){Unit u=units[i];if(u.hp<=0&&!u.dead){u.dead=true;u.deathX=clamp(u.x,12,W-12);u.deathY=clamp(u.y,40,H-4);}if(u.hp>0){if((teams?u.team:i)==0)left=true;else right=true;}}if(!left||!right){finishPower();over=true;winner=!left?(!right?2:1):0;charging=false;winSerial++;}}
    void finishPower(){Unit u=units[active];if(u.pendingPower>=0){u.lastPower=u.pendingPower;u.pendingPower=-1;}}
    void next(){finishPower();check();if(over)return;if(deferNext){pendingNext=true;charging=false;return;}Unit previous=units[active];if(previous.invisible>0)previous.invisible--;if(previous.frozen==2){previous.freezeTurns=Math.max(0,previous.freezeTurns-1);previous.frozen=previous.freezeTurns>0?1:0;}do{active=(active+1)%unitCount;}while(units[active].hp<=0);turn++;units[active].energy=240;if(units[active].flyCooldown>0)units[active].flyCooldown--;units[active].assist=false;if(units[active].frozen==1)units[active].frozen=2;countdown=units[active].frozen==2?30:turnLimit;botWait=settle=0;lastWind=wind;wind=rand(61)-30;clear();}
    int error(int angle,int power){
        Ball b=make(angle,power);Unit t=units[enemyOf(active)];int best=999999;
        // One coarse sample per frame is enough for AI; gameplay uses four swept substeps.
        for(int n=0;n<360;n++){
            b.vx+=windStep(wind,b)*4;b.vy+=gravity(b)*4;b.x+=b.vx;b.y+=b.vy;int x=b.x/256,y=b.y/256,dx=x-t.x,dy=y-t.y+12;
            int distance=dx*dx+dy*dy;if(distance<best)best=distance;
            if(ground.solid(x,y)||x<0||x>=W||y>H+20)break;
        }return best;
    }
    void aim(){
        Unit u=units[active];u.face=units[enemyOf(active)].x<u.x?-1:1;refreshAim(u);int best=999999,ba=u.angle,bp=65;
        for(int a=minLocalAngle(u);a<=maxLocalAngle(u);a+=4)for(int p=28;p<=100;p+=4){int score=error(a,p);if(score<best){best=score;ba=a;bp=p;}}
        u.angle=clamp(ba+rand(3)-1,minLocalAngle(u),maxLocalAngle(u));u.power=Math.max(1,Math.min(100,bp+rand(3)-1));
    }
    int botPositionScore(int x,int y){Unit enemy=units[enemyOf(active)];int distance=Math.abs(x-enemy.x);return Math.abs(distance-(units[active].botStyle>=0?(units[active].botStyle==0?220:units[active].botStyle==1?125:units[active].botStyle==2?185:205):170))+Math.max(0,y-enemy.y)*2+(distance<65?400:0)+(x<30||x>W-30?120:0);}
    boolean botFlight(){
        Unit u=units[active];if(u.energy<150)return false;int best=botPositionScore(u.x,u.y)-25,angle=u.angle,power=u.power,face=u.face;boolean found=false,oldFly=fly;fly=true;
        for(int direction=-1;direction<=1;direction+=2){u.face=direction;
            for(int a=minLocalAngle(u);a<=maxLocalAngle(u);a+=6)for(int p=32;p<=96;p+=8){
                Ball b=make(a,p);int landingX=-1,landingY=0;
                for(int step=0;step<1200;step++){b.vx+=windStep(wind,b);b.vy+=gravity(b);b.x+=b.vx/4;b.y+=b.vy/4;int x=b.x/256,y=b.y/256;if(x<12||x>=W-12||y>H+20)break;if(ground.solid(x,y)){int floor=ground.surface(x,Math.max(0,y-18));if(floor<H&&!occupied(x,active,35)){landingX=x;landingY=floor;}break;}}
                if(landingX>=0&&Math.abs(landingX-u.x)>35){int score=botPositionScore(landingX,landingY);if(score<best){best=score;angle=a;power=p;face=direction;found=true;}}
            }
        }
        fly=oldFly;u.face=face;if(!found)return false;if(!prop(4))return false;u.angle=angle;u.power=power;return true;
    }
    void botTurn(){
        if(botPowQueued)return;
        botActing=true;
        try{
            Unit u=units[active],enemy=units[enemyOf(active)];botWait++;
            if(botWait==1){botBlocked=0;if(u.hp<=MAXHP-(u.botStyle==3?350:600)&&u.heals>0)prop(3);}
            if(botWait<=12&&u.energy>=92){
                int distance=Math.abs(u.x-enemy.x),toward=enemy.x<u.x?-1:1,near=u.botStyle==1?65:u.botStyle==0?145:95,far=u.botStyle==1?160:u.botStyle==0?260:220,direction=distance<near?-toward:distance>far?toward:0;
                if(direction==0){int left=ground.surface(u.x-10,Math.max(0,u.y-18)),right=ground.surface(u.x+10,Math.max(0,u.y-18));if(left+4<right)direction=-1;else if(right+4<left)direction=1;}
                if(direction!=0){int before=u.x,nx=u.x+direction,floor=ground.surface(nx,Math.max(0,u.y-18));if(nx>=12&&nx<W-12&&floor<H&&floor<=u.y+6)move(direction);if(u.x==before)botBlocked++;}
            }
            if(botWait==14){
                int oldAngle=u.angle;aim();boolean badPosition=Math.abs(u.x-enemy.x)<85||u.y-enemy.y>35||botBlocked>=6||error(u.angle,u.power)>1600;
                if(badPosition)botFlight();
                botTargetAngle=u.angle;botTargetPower=u.power;u.angle=clamp(oldAngle,minLocalAngle(u),maxLocalAngle(u));u.power=0;
                if(!fly){botTargetAngle=clamp(botTargetAngle+(u.botStyle==0?rand(7)-3:rand(11)-5),minLocalAngle(u),maxLocalAngle(u));botTargetPower=clamp(botTargetPower+(u.botStyle==0?rand(11)-5:rand(17)-8),18,100);if(rand(5)==0)botTargetPower=clamp(botTargetPower+rand(15)-7,18,100);}
            }
            if(botWait==21&&!fly){
                if(u.rage>=100)prop(5);
                if(enemy.hp<450){prop(2);if(u.energy>=90)prop(2);}
                else{prop(u.botStyle>=0?(u.botStyle==0||u.botStyle==3?2:u.botStyle==1?1:0):Math.abs(u.x-enemy.x)<150?1:0);if(u.energy>=90)prop(2);}
            }
            if(botWait>=22){
                if(botPhase==0){botPhase=1;charging=true;}
                if(botPhase==1){int difference=botTargetAngle-u.angle;if(botWait%2==0)u.angle+=clamp(difference,-1,1);if(u.angle==botTargetAngle&&botWait>=52){botPhase=2;u.power=0;}}
                else if(botPhase==2){if(botWait%2==0)u.power=Math.min(botTargetPower,u.power+2);if(u.power>=botTargetPower){botPhase=3;charging=false;if(pow&&deferBotPow)botPowQueued=true;else fire();}}
            }
        }finally{botActing=false;}
    }
}
