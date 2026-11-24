/** Tests the local client physics (not the server Battle copy) and frame-gap monitor. */
public final class OfflineBotDiagnosticCheck {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static final class Flat implements Battle.Ground {
        public boolean solid(int x,int y){return y>=170;}
        public int surface(int x,int y){return Math.max(170,y);}
        public void crater(int x,int y,int r){}
    }
    public static void main(String[] args){
        Battle b=new Battle(new Flat());b.start(false);
        check(!b.hotseat&&!b.teams&&b.unitCount==2&&b.humanTurn(),"local 1v1");
        b.units[0].weapon=1;b.units[1].weapon=1;b.units[1].botStyle=0;
        b.turnLimit=360;b.countdown=360;
        b.units[0].x=90;b.units[0].y=170;b.units[0].angle=45;b.units[0].face=1;
        b.units[1].x=370;b.units[1].y=170;b.units[1].face=-1;
        b.charge();check(b.charging,"local charge");
        b.units[0].power=55;b.fire();check(!b.charging&&b.bursts>0,"release local shot");
        boolean humanShot=false,botShot=false,botTurn=false;int maxTicks=6000;
        for(int n=0;n<maxTicks&&!b.over&&!botShot;n++){
            b.tick();
            for(int i=0;i<b.ballCount;i++){if(b.balls[i].owner==0)humanShot=true;if(b.balls[i].owner==1)botShot=true;}
            if(b.active==1){botTurn=true;check(!b.humanTurn(),"bot owns its turn");}
        }
        check(humanShot&&botTurn&&botShot,"human and BOT each fire without server");
        check(BattlePlaybackPacer.period(true)==39,"flight cadence unchanged");
        FrameGapMonitor m=new FrameGapMonitor();m.sample(1000,false);m.sample(1033,false);m.sample(1072,true);m.sample(1111,true);m.sample(1350,true);m.sample(1389,false);
        check(m.shotPeak()==239&&m.takeWindowPeak()==239&&m.takeWindowPeak()==0,"capture a flight stall and retain last-shot peak");
        m.sample(1422,true);check(m.shotPeak()==33,"reset on next shot");m.idle();m.sample(5000,false);check(m.takeWindowPeak()==33,"exclude paused time");m.reset();check(m.shotPeak()==0&&m.takeWindowPeak()==0,"new test reset");
        System.out.println("PASS: local client 1v1, player/BOT shots, 39 ms pacing and frame gap monitor");
    }
}
