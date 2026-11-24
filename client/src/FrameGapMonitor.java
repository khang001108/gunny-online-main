/** Primitive-only frame interval monitor; no allocation on simulation ticks. */
final class FrameGapMonitor {
    private long previous;
    private int window, shot;
    private boolean flying;
    void reset(){previous=0;window=shot=0;flying=false;}
    void idle(){previous=0;flying=false;}
    void sample(long now,boolean flight){
        if(flight&&!flying)shot=0;
        if(previous!=0&&now>=previous){
            int gap=(int)Math.min(99999L,now-previous);
            if(gap>window)window=gap;
            if((flight||flying)&&gap>shot)shot=gap;
        }
        previous=now;flying=flight;
    }
    int takeWindowPeak(){int value=window;window=0;return value;}
    int shotPeak(){return shot;}
}
