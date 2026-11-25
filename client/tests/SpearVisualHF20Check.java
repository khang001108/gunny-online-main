public final class SpearVisualHF20Check {
 static void ok(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 public static void main(String[] args){int n=0;
  for(int i=0;i<32;i++) {int a=SpearVisual.degrees(i);ok(SpearVisual.direction(Battle.cos(a),Battle.sin(a))==i,"direction "+i);n++;}
  for(int aim=0;aim<=90;aim++)for(int pose=18;pose<=20;pose++){
   int r=SpearVisual.heldDirection(1,pose,aim),l=SpearVisual.heldDirection(-1,pose,aim);
   ok(Math.abs(SpearVisual.gripX(r)+SpearVisual.gripX(l))<=1,"mirror grip X");
   ok(Math.abs(SpearVisual.gripY(r)-SpearVisual.gripY(l))<=1,"mirror grip Y");n+=2;
  }
  System.out.println("PASS "+n+" spear angle and mirrored grip checks");
 }
}
