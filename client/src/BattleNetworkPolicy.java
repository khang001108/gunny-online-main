/** Transport pacing only; no changes to render or simulation cadence. */
final class BattleNetworkPolicy {
    static final int INPUT_MS=100, RECEIVE_MS=100, EVENT_MS=150;
    static boolean due(long elapsed,boolean urgent,boolean input,boolean changed,boolean prefetch){
        return urgent||elapsed>=INPUT_MS&&input||elapsed>=RECEIVE_MS&&(changed||prefetch);
    }
    static boolean lowBufferDue(long elapsed,boolean prefetch,int buffered){return prefetch&&buffered<=12&&elapsed>=40;}
    static boolean urgent(int[] kinds,int count){for(int i=0;i<count;i++)if(kinds[i]!=6)return true;return false;}
}
