/** Display-only prediction. Damage, terrain, turns and rewards stay on the server. */
final class OnlineMotion {
    final OnlineClient net;
    final int[] x=new int[6],y=new int[6],angle=new int[6],power=new int[6];
    final int[] tx=new int[6],ty=new int[6],ta=new int[6];
    boolean ready,charging,released;int turn=-1,actor=-1,shot=-1,startSeq,releaseSeq,dir=1;
    long clock,accumulator;
    OnlineMotion(OnlineClient owner){net=owner;}
    void reset(){liveReady=false;received=0;ready=false;turn=actor=shot=-1;startSeq=releaseSeq=0;charging=released=false;clock=accumulator=0;}
    void before(){Battle b=net.c.game;for(int i=0;i<net.count;i++){Battle.Unit u=b.units[i];x[i]=u.x;y[i]=u.y;angle[i]=u.angle;power[i]=u.power;}}
    void after(){
        Battle b=net.c.game;boolean changed=!ready||turn!=b.turn||actor!=b.active||shot!=b.shotSerial||b.over||net.c.powCutinTicks>0;
        if(changed){charging=released=false;startSeq=releaseSeq=0;net.c.held=net.c.fireKey=0;}
        for(int i=0;i<net.count;i++){
            Battle.Unit u=b.units[i];tx[i]=u.x;ty[i]=u.y;ta[i]=u.angle;
            if(!changed&&!u.dead&&Math.abs(x[i]-u.x)<40&&Math.abs(y[i]-u.y)<35){u.x=x[i];u.y=y[i];u.angle=angle[i];}
        }
        if(!changed&&net.localTurn()){
            Battle.Unit u=b.units[b.active];
            if(startSeq>0){
                if(!released&&net.ack>=startSeq&&!b.charging){charging=false;startSeq=0;}
                else {u.power=power[b.active];b.charging=charging;}
            }
        }
        turn=b.turn;actor=b.active;shot=b.shotSerial;ready=true;
    }
    // Render offsets never change authoritative coordinates used by collision or damage.
    final int[] fromX=new int[Battle.MAX_UNITS],fromY=new int[Battle.MAX_UNITS];
    final int[] goalX=new int[Battle.MAX_UNITS],goalY=new int[Battle.MAX_UNITS];
    long received;int blend=75,liveTurn=-1;boolean liveReady;
    int display(int from,int to,long now){int elapsed=(int)Math.max(0,Math.min(blend,now-received));return from+(to-from)*elapsed/blend;}
    void receiveLive(long now){Battle b=net.c.game;boolean snap=!liveReady||liveTurn!=b.turn||b.over;
        for(int i=0;i<b.unitCount;i++){Battle.Unit u=b.units[i];int ax=u.x*256,ay=u.y*256;
            int px=liveReady?display(fromX[i],goalX[i],now):ax,py=liveReady?display(fromY[i],goalY[i],now):ay;
            boolean jump=snap||u.dead||Math.abs(ax-px)>48*256||Math.abs(ay-py)>48*256;if(jump){px=ax;py=ay;}
            fromX[i]=px;fromY[i]=py;goalX[i]=ax;goalY[i]=ay;
        }
        if(received>0)blend=(int)Math.max(33,Math.min(200,now-received));received=now;liveTurn=b.turn;liveReady=true;
    }
    int offset(int actor,boolean vertical,int scale,long now){if(!liveReady||net.predicting&&actor<net.count&&net.controls[actor])return 0;Battle.Unit u=net.c.game.units[actor];return (display(vertical?fromY[actor]:fromX[actor],vertical?goalY[actor]:goalX[actor],now)-(vertical?u.y:u.x)*256)*scale/256;}
    int input(int kind,int arg,int seq){
        Battle b=net.c.game;if(!ready||!b.input()||net.c.powCutinTicks>0)return arg;
        if(kind==1){charging=true;released=false;startSeq=seq;releaseSeq=0;dir=1;b.charging=true;b.units[b.active].power=0;}
        if(kind==2&&charging){charging=false;released=true;releaseSeq=seq;b.charging=false;
            Battle.Unit u=b.units[b.active];if(net.preciseControls)return -(1+u.angle*128+u.power);
        }
        return arg;
    }
    static int approach(int from,int to){int d=to-from;if(d==0)return from;return from+(Math.abs(d)<4?(d<0?-1:1):d/4);}
    void animate(long now){
        if(clock==0){clock=now;return;}long elapsed=now-clock;clock=now;
        if(elapsed<0)elapsed=0;if(elapsed>99)elapsed=99;accumulator+=elapsed;
        while(accumulator>=33){accumulator-=33;step();}
    }
    void step(){
        if(!ready)return;Battle b=net.c.game;boolean local=net.localTurn()&&b.input()&&net.c.powCutinTicks==0;
        int held=local&&!released?net.c.held:0;
        for(int i=0;i<net.count;i++){
            Battle.Unit u=b.units[i];if(u.dead)continue;
            boolean own=local&&i==b.active;
            if(!own||(held&3)==0){u.x=approach(u.x,tx[i]);u.y=approach(u.y,ty[i]);}
            if(!own||(held&12)==0)u.angle=approach(u.angle,ta[i]);
        }
        if(!local)return;
        if((held&1)!=0)b.move(-1);if((held&2)!=0)b.move(1);
        if((held&4)!=0)b.adjustAngle(1);if((held&8)!=0)b.adjustAngle(-1);
        Battle.Unit u=b.units[b.active];
        // Settle predicted footsteps on the terrain without running battle physics.
        if((held&3)!=0){int floor=b.ground.surface(u.x,u.y+1);if(floor>=u.y&&floor-u.y<12)u.y=Math.min(floor,u.y+2);}
        if(charging){u.power+=2*dir;if(u.power>=100){u.power=100;dir=-1;}else if(u.power<=0){u.power=0;dir=1;}b.charging=true;}
    }
}
