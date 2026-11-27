/** Visual-only attachment in the 32x40 body cell coordinate system.
 * Origin is the same foot pivot used by PhoneCanvas.drawSlopedBody (16,38).
 * The wing atlas has a separate fixed joint at (48,48), NOT its cropped corner.
 * Calibrated against the stock male/female cloth and full costume frames.
 */
final class WingAttachment {
    // Facing right, offsets from the foot pivot. Frames 1..9 are crouch/walk;
    // 10..15 shoot upright; 16..21 throw/recover. Preserve the old flat crouch root.
    private static final int[] X={-3,-1,-2,-1,0,-1,-1,-2,-1,-1,-3,-3,-3,-3,-3,-3,-2,-2,-2,-3,-3,-3};
    private static final int[] Y={-8,-3,-3,-3,-3,-3,-3,-3,-3,-3,-8,-8,-8,-8,-8,-8,-8,-9,-9,-8,-8,-8};
    private static final int[] LEAN={0,60,60,60,60,60,60,60,60,60,0,0,0,0,0,0,-15,-15,-15,0,0,0};
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
    // Use only already-loaded five-angle atlases. MIDP can quarter-turn a region
    // without decoding, allocating, or resampling another image. Rotation is
    // around the wing joint, not the cropped image's top-left.
    static int select(int mask,int target){
        int best=-1,error=361;
        for(int q=0;q<4;q++)for(int a=0;a<5;a++)if((mask&(1<<a))!=0){
            int d=(target-(a*15-30+q*90))%360;if(d>180)d-=360;if(d< -180)d+=360;
            d=Math.abs(d);if(d<error){error=d;best=q*5+a;}
        }
        return best;
    }
    static int left(int x,int y,int w,int h,int q,boolean flip){
        int l=q==0?x:q==1?96-y-h:q==2?96-x-w:y;
        int rw=(q&1)==0?w:h;
        return flip?96-l-rw:l;
    }
    static int top(int x,int y,int w,int h,int q){return q==0?y:q==1?x:q==2?96-y-h:96-x-w;}
}
