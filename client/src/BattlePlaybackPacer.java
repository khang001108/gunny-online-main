/** Client-only time pacing. One original simulation/snapshot step per flight render. */
final class BattlePlaybackPacer {
    static final int NORMAL_MS=33, FLIGHT_MS=39;
    private long clock, debt;
    private boolean initialized;
    static boolean projectile(int balls,int bursts){return balls>0||bursts>0;}
    static int period(boolean flight){return flight?FLIGHT_MS:NORMAL_MS;}
    void reset(){initialized=false;clock=debt=0;}
    int steps(long now,boolean flight){
        // No accumulated debt across a flight, freeze, or transition. Batching two
        // projectile snapshots into a single paint produces a visible position jump.
        if(flight){reset();return 1;}
        if(!initialized){debt=NORMAL_MS;initialized=true;}
        else debt=Math.min(2*NORMAL_MS,debt+Math.max(0L,now-clock));
        clock=now;
        int count=(int)(debt/NORMAL_MS);
        debt-=count*NORMAL_MS;
        return count;
    }
}
