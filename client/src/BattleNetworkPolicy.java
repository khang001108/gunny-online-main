/** Transport pacing only; no changes to render or simulation cadence. */
final class BattleNetworkPolicy {
    static final int INPUT_MS=100, RECEIVE_MS=100, EVENT_MS=220;
    static boolean due(long elapsed,boolean urgent,boolean input,boolean changed,boolean prefetch){
        return urgent||elapsed>=INPUT_MS&&input||elapsed>=RECEIVE_MS&&(changed||prefetch);
    }
    static boolean lowBufferDue(long elapsed,boolean prefetch,int buffered){return lowBufferDue(elapsed,prefetch,buffered,false);}
    // 16 * 39ms = 624ms, slightly more reserve than 18 * 33ms = 594ms.
    static boolean lowBufferDue(long elapsed,boolean prefetch,int buffered,boolean flight){return prefetch&&buffered<=(flight?16:18)&&elapsed>=33;}
    static boolean urgent(int[] kinds,int count){for(int i=0;i<count;i++)if(kinds[i]!=6)return true;return false;}
}
