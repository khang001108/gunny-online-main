/** HF39: clock jumps must never wedge the render thread or cause a burst of physics steps. */
public final class HF39ClockCheck {
 static int checks;static void ok(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
 public static void main(String[] args){
  BattlePlaybackPacer p=new BattlePlaybackPacer();
  ok(p.steps(10000,false)==1,"initial");ok(p.steps(10033,false)==1,"normal");
  ok(p.steps(500,false)==1,"clock rollback resets debt");ok(p.steps(533,false)==1,"normal after rollback");
  ok(p.steps(900000,false)==1,"large forward jump no catch-up");ok(p.steps(900033,false)==1,"normal after jump");
  for(int i=0;i<1000;i++){long t=100000L+i*33;ok(p.steps(t,true)==1,"single shot frame");ok(BattlePlaybackPacer.sleepDelay(33,t-100000,t)==33,"rollback sleep bounded");ok(BattlePlaybackPacer.sleepDelay(39,t+100000,t)==2,"long frame bounded");}
  ok(BattlePlaybackPacer.sleepDelay(33,1005,1000)==28,"normal sleep");
  ok(BattlePlaybackPacer.sleepDelay(33,Long.MAX_VALUE,Long.MIN_VALUE)==2,"overflow safe");
  System.out.println("PASS "+checks+" HF39 clock checks");
 }
}
