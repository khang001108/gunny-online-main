/** Charge timer and finite one-bounce behavior in the battle shared by client/server. */
public final class Charge770Check {
 static final Battle.Ground GROUND=new Battle.Ground(){public boolean solid(int x,int y){return y>=170;}public int surface(int x,int y){return Math.max(170,y);}public void crater(int x,int y,int radius){}};
 static Battle battle(){Battle b=new Battle(GROUND);b.start(true);for(int i=0;i<2;i++)b.units[i].y=170;return b;}
 static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);System.out.println("PASS: "+message);}
 public static void main(String[] args){
  Battle b=battle();b.countdown=2;b.charge();for(int i=0;i<60;i++)b.tick();
  check(b.countdown==2&&b.turn==1,"Holding force pauses the clock even with two ticks left");
  check(b.units[0].power==60,"Force increases by one unit per 30Hz tick");
  b.charge();check(b.units[0].power==60,"Repeated charge input cannot reset the charge cycle");
  for(int i=0;i<40;i++)b.tick();check(b.units[0].power==100&&b.chargeDir==-1,"Force reaches 100 and reverses once");
  for(int i=0;i<99;i++)b.tick();check(b.turn==1&&b.units[0].power==1,"One return leg remains playable before zero");
  b.tick();check(b.turn==2&&b.active==1&&!b.charging&&b.shotSerial==0,"Reaching zero skips the turn without firing or starting a second cycle");
  b=battle();b.deferNext=true;b.countdown=1;b.charge();for(int i=0;i<200;i++)b.tick();
  check(b.pendingNext&&b.turn==1&&!b.charging,"Online overcharge waits at the shared turn barrier");
  b.completeTurn();check(b.turn==2,"Acknowledging the overcharge advances exactly one turn");
  b=battle();b.countdown=100;b.charge();for(int i=0;i<47;i++)b.tick();b.fire();
  check(!b.charging&&b.units[0].pendingPower==47&&b.countdown==100,"Releasing uses the selected force and preserves paused time");
  b=battle();b.countdown=2;b.tick();b.tick();check(b.turn==2,"The clock still expires normally when the player is not charging");
 }
}
