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
    private static final int[] ONE_X={-2,-4,-5,-4,-4,-4,-4,-5,-4,-4,-3,-4,-3,-4,-3,-3,-3,-4,-5,-4,-3,-3};
    private static final int[] ONE_Y={-9,-2,-2,-2,-2,-2,-2,-2,-2,-2,-8,-8,-8,-8,-8,-8,-9,-8,-7,-8,-8,-8};
    private static final int[] ONE_LEAN={-71,15,15,15,15,15,15,15,15,15,-102,-102,-102,-102,-102,-99,-98,-102,-104,-102,-98,-97};
    private static final int[] GROUP_X={-3,0,-1,0,-1,0,-1,0,-1,0,-4,-4,-4,-4,-4,-4,-4,-3,-2,-4,-4,-4};
    private static final int[] GROUP_Y={-8,-5,-5,-5,-5,-5,-5,-5,-5,-5,-7,-7,-7,-7,-7,-7,-7,-7,-7,-7,-7,-7};
    private static final int[] GROUP_LEAN={0,15,15,15,15,15,15,15,15,15,-5,-6,-12,-13,-12,-10,-15,-29,-34,0,5,7};
    private static final int[] FIVE_X={-6,-6,-5,-6,-5,-6,-5,-6,-5,-6,-6,-6,-6,-6,-6,-6,-6,-6,-6,-6,-6,-6};
    private static final int[] FIVE_Y={-7,-4,-4,-4,-4,-4,-4,-4,-4,-4,-7,-7,-7,-7,-7,-7,-7,-7,-7,-7,-7,-7};
    private static final int[] FIVE_LEAN={-40,15,15,15,15,15,15,15,15,15,-50,-45,-55,-50,-50,-45,-60,-70,-75,-55,-50,-50};
    private static final int[] FOUR_X={2,3,2,3,2,3,2,3,2,3,2,2,2,2,2,2,2,2,2,2,2,2};
    private static final int[] FOUR_Y={-5,0,0,0,0,0,0,0,0,0,-8,-8,-8,-8,-8,-8,-9,-10,-11,-9,-6,-7};
    private static final int[] FOUR_LEAN={-35,0,0,0,0,0,0,5,0,-5,-40,-40,-50,-50,-50,-45,-60,-70,-80,-55,-45,-40};
    static boolean group(int wing){return wing>=1&&wing<=4;}
    static int jointX(int wing,int pose,int face,int slope){int[] x=wing==0?ONE_X:wing==4?FIVE_X:wing==3?FOUR_X:group(wing)?GROUP_X:X;int[] y=wing==0?ONE_Y:wing==4?FIVE_Y:wing==3?FOUR_Y:group(wing)?GROUP_Y:Y;return round(x[pose]*face*Battle.cos(slope)+y[pose]*Battle.sin(slope));}
    static int jointY(int wing,int pose,int face,int slope){int[] x=wing==0?ONE_X:wing==4?FIVE_X:wing==3?FOUR_X:group(wing)?GROUP_X:X;int[] y=wing==0?ONE_Y:wing==4?FIVE_Y:wing==3?FOUR_Y:group(wing)?GROUP_Y:Y;return round(-x[pose]*face*Battle.sin(slope)+y[pose]*Battle.cos(slope));}
    static int tilt(int wing,int pose,int face,int slope){return (wing==0?ONE_LEAN:wing==4?FIVE_LEAN:wing==3?FOUR_LEAN:group(wing)?GROUP_LEAN:LEAN)[pose]-face*slope;}
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
