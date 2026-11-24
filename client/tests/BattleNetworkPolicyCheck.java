public class BattleNetworkPolicyCheck {
 static int n;static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 public static void main(String[] args){ok(!BattleNetworkPolicy.due(33,false,true,false,false));ok(!BattleNetworkPolicy.due(99,false,true,true,true));ok(BattleNetworkPolicy.due(100,false,true,false,false));ok(BattleNetworkPolicy.due(1,true,false,false,false));ok(!BattleNetworkPolicy.due(100,false,false,false,false));ok(BattleNetworkPolicy.due(100,false,false,true,false));ok(BattleNetworkPolicy.due(100,false,false,false,true));ok(!BattleNetworkPolicy.urgent(new int[]{6,6},2));ok(BattleNetworkPolicy.urgent(new int[]{6,2},2));ok(BattleNetworkPolicy.urgent(new int[]{3},1));System.out.println("PASS: "+n+" pacing checks");}
}
