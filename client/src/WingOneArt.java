/** Wing Studio V3.1 export for wing ID 0 only. Bounds relative to attachment joint. */
final class WingOneArt {
 static final int[] ANGLES={-102,-104,-71,-98,-99,-97,-41,-131};
 static final int[] B1={-28,-13,33,33,-28,-18,38,38,-28,-13,33,33,-28,-18,38,38,-25,-17,34,32,-30,-15,46,30,-28,-13,33,32,-29,-18,40,37,-28,-13,33,33,-29,-18,40,38,-28,-13,33,32,-29,-18,40,37,-20,-24,33,32,-24,-21,42,34,-24,-13,32,33,-21,-18,34,42};
 static final int[] B2={-28,-13,33,33,-29,-18,39,38,-28,-13,33,34,-28,-18,39,39,-26,-17,35,33,-30,-14,46,29,-28,-13,33,33,-29,-18,40,38,-28,-13,33,33,-29,-18,40,38,-28,-13,33,33,-29,-18,40,38,-20,-25,33,33,-25,-22,44,35,-25,-13,33,33,-22,-19,35,44};
 static int[] bounds(int scale){return scale==2?B2:B1;}
 static int index(int a,int layer){return (a*2+layer)*4;}
 static int bytes(int scale,int a){int[] b=bounds(scale);int p=index(a,0);return (b[p+2]*b[p+3]+b[p+6]*b[p+7])*19*4*scale*scale;}
 static int distance(int a,int b){int d=Math.abs((a-b)%360);return Math.min(d,360-d);}
 // Stock indices 0..4; custom indices 5..12. Prefer exact angle when loaded.
 static int select(int stock,int extra,int wanted){int best=-1,dist=361;for(int a=0;a<5;a++)if((stock&(1<<a))!=0){int d=distance(a*15-30,wanted);if(d<dist){dist=d;best=a;}}for(int a=0;a<ANGLES.length;a++)if((extra&(1<<a))!=0){int d=distance(ANGLES[a],wanted);if(d<dist){dist=d;best=5+a;}}return best;}
}
