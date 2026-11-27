/** Client-only time pacing. One original simulation/snapshot step per flight render. */
final class BattlePlaybackPacer {
    static final int NORMAL_MS=33, FLIGHT_MS=39;
    private long clock, debt;
    // HF39: the emulator's wall clock can jump when its speed multiplier changes.
    static final long CLOCK_GAP_MS=1500;
    static boolean discontinuity(long now,long previous){return now<previous||now-previous<0||now-previous>CLOCK_GAP_MS;}
    static long elapsed(long now,long previous){if(now<previous)return 0;long d=now-previous;return d<0?Long.MAX_VALUE:d;}
    static long sleepDelay(int period,long now,long began){return Math.max(2L,period-Math.min((long)period,elapsed(now,began)));}
    private boolean initialized;
    static boolean projectile(int balls,int bursts){return balls>0||bursts>0;}
    static int period(boolean flight){return flight?FLIGHT_MS:NORMAL_MS;}
    void reset(){initialized=false;clock=debt=0;}
    int steps(long now,boolean flight){
        // No accumulated debt across a flight, freeze, or transition. Batching two
        // projectile snapshots into a single paint produces a visible position jump.
        if(flight){reset();return 1;}
        if(!initialized||discontinuity(now,clock)){debt=NORMAL_MS;initialized=true;}
        else debt=Math.min(2*NORMAL_MS,debt+elapsed(now,clock));
        clock=now;
        int count=(int)(debt/NORMAL_MS);
        debt-=count*NORMAL_MS;
        return count;
    }
}
