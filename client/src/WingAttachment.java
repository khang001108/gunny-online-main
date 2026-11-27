/** Visual-only attachment in the 32x40 body cell coordinate system.
 * Origin is the same foot pivot used by PhoneCanvas.drawSlopedBody (16,38).
 * The wing atlas has a separate fixed joint at (48,48), NOT its cropped corner.
 * Calibrated against the stock male/female cloth and full costume frames.
 */
final class WingAttachment {
    // Facing right, offsets from the foot pivot. Frames 1..9 are crouch/walk;
    // 10..15 shoot upright; 16..21 throw/recover. Crouch roots are shifted rearward.
    private static final int[] X={-3,-4,-5,-4,-4,-4,-4,-5,-4,-4,-3,-3,-3,-3,-3,-3,-2,-2,-2,-3,-3,-3};
    private static final int[] Y={-8,-5,-5,-5,-5,-5,-5,-5,-5,-5,-8,-8,-8,-8,-8,-8,-8,-9,-9,-8,-8,-8};
    private static final int[] LEAN={0,15,15,15,15,15,15,15,15,15,0,0,0,0,0,0,-15,-15,-15,0,0,0};
    static int pose(int anim,boolean compact){
        if(anim<0||anim>=X.length)return 0;
        // E72 layered costume sheets repeat the five walking cells, unlike the full hero sheet.
        if(compact&&anim>=1&&anim<10)return 1+(anim-1)%5;
        if(compact&&anim>=10&&anim<16)return 10+anim%2;
        return anim;
    }
    static int round(int n){return n<0?-((-n+512)/1024):(n+512)/1024;}
    static int jointX(int pose,int face,int slope){return round(X[pose]*face*Battle.cos(slope)+Y[pose]*Battle.sin(slope));}
    static int jointY(int pose,int face,int slope){return round(-X[pose]*face*Battle.sin(slope)+Y[pose]*Battle.cos(slope));}
    static int tilt(int pose,int face,int slope){return LEAN[pose]-face*slope;}
    // Do not quarter-turn the atlas: a 60/90 degree turn swings the feather
    // tips over the face. Keep the original rear-facing five angles in RAM.
    static int select(int mask,int target){
        int a=Battle.clamp((Battle.clamp(target,-30,30)+30+7)/15,0,4);
        return WingBattleArt.nearest(mask,a);
    }
}
