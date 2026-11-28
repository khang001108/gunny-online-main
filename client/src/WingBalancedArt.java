/** Shared calibrated angle catalog. Frame count is independent of the chosen angle. */
final class WingBalancedArt {
 static int count(int w){return 5+(w==0?WingOneArt.ANGLES.length:w<=4?WingGroupArt.angles(w).length:0);}
 static int angle(int w,int a){return a<5?a*15-30:w==0?WingOneArt.ANGLES[a-5]:WingGroupArt.angles(w)[a-5];}
 static int select(int w,int wanted){int best=0,dist=361;for(int a=0;a<count(w);a++){int d=WingOneArt.distance(angle(w,a),wanted);if(d<dist){best=a;dist=d;}}return best;}
 static int[] bounds(int scale,int w,int a){return a<5?WingBattleArt.bounds(scale):w==0?WingOneArt.bounds(scale):WingGroupArt.bounds(scale);}
 static int index(int w,int a,int layer){return a<5?WingBattleArt.index(w,a,layer):w==0?WingOneArt.index(a-5,layer):WingGroupArt.index(w,a-5,layer);}
 static int bytes(int scale,int w,int a){int[] b=bounds(scale,w,a);int i=index(w,a,0);return (b[i+2]*b[i+3]+b[i+6]*b[i+7])*4*scale*scale;}
}
