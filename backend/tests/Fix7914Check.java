public final class Fix7914Check {
 public static void main(String[] args){
  Battle b=Sulan7912Check.raid(1,0);TribalRules.projectile(b,1,0,TribalRules.CHIEF_SHOT,0);if(b.balls[0].vy>=-400)throw new AssertionError("Chief spear must rise on a visible arc");
  b=Sulan7912Check.raid(3,0);Sulan7912Check.red(b);b.beginTurn(1);for(int i=0;i<55;i++)b.tick();if(b.ballCount!=3)throw new AssertionError("One simultaneous rain projectile per player");
  int turn=b.turn;for(int i=0;i<600&&turn==b.turn;i++)b.tick();if(b.shotSerial!=12||b.active!=6)throw new AssertionError("Four three-ball volleys finish before pea's turn");
  for(int stage=1;stage<=2;stage++){b=new Battle(TribalCheck.flat);b.start(true);b.dungeonStage=stage;b.turn=201;b.check();if(!b.over||b.winner!=1)throw new AssertionError("Ant condition turn limit is enforced");}
  System.out.println("PASS chief spear arc, three-player falling fire volleys and Ant turn limits");
 }
}
