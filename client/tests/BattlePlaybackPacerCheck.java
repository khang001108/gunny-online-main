/** Checks flight timing and no catch-up jump on a simulated slow handset. */
public final class BattlePlaybackPacerCheck {
    static void check(boolean value,String msg){if(!value)throw new AssertionError(msg);}
    public static void main(String[] args){
        BattlePlaybackPacer p=new BattlePlaybackPacer();
        check(BattlePlaybackPacer.period(false)==33,"normal timing");
        check(BattlePlaybackPacer.period(true)==39,"flight timing");
        check(BattlePlaybackPacer.projectile(1,0)&&BattlePlaybackPacer.projectile(0,2),"projectile and consecutive shots");
        check(!BattlePlaybackPacer.projectile(0,0),"not a shot");
        check(p.steps(1000,false)==1,"first normal frame");
        check(p.steps(1033,false)==1,"normal 33ms");
        check(p.steps(1040,false)==0,"normal debt");
        check(p.steps(1120,false)==2,"normal catch-up bounded");
        p.reset(); // transition after first step launches a projectile
        for(int i=0;i<1000;i++)check(p.steps(1120L+i*39+(i%3==0?90:0),true)==1,"no double flight step");
        check(p.steps(50000,false)==1,"no inherited debt after shot");
        check(p.steps(50033,false)==1,"normal cadence restored");
        p.reset();check(p.steps(90000,false)==1,"resume without catch-up");
        check(!BattleNetworkPolicy.lowBufferDue(32,true,16,true),"minimum urgent refill interval");
        check(BattleNetworkPolicy.lowBufferDue(33,true,16,true),"flight refill reserve");
        check(!BattleNetworkPolicy.lowBufferDue(33,true,17,true),"healthy flight buffer");
        check(BattleNetworkPolicy.lowBufferDue(33,true,18,false),"normal refill unchanged");
        check(!BattleNetworkPolicy.lowBufferDue(33,false,0,true),"window closed");
        System.out.println("PASS: flight pace, consecutive shots, bounded catch-up, resume, flight refill");
    }
}
