/** Shared screen-space direction and grip geometry; no allocation in paint. */
final class SpearVisual {
 static final int DIRECTIONS=32, CELL=32;
 static int degrees(int dir){return (dir*360+DIRECTIONS/2)/DIRECTIONS;}
 static int direction(int vx,int vy){int best=0;long score=Long.MIN_VALUE;for(int i=0;i<DIRECTIONS;i++){int a=degrees(i);long dot=(long)vx*Battle.cos(a)+(long)vy*Battle.sin(a);if(dot>score){score=dot;best=i;}}return best;}
 static int heldDirection(int face,int pose,int aim){int angle=pose<=18?-135:pose==19?-90:-Math.max(10,Math.min(85,aim));return direction(face*Battle.cos(angle),Battle.sin(angle));}
 static int gripX(int dir){return Battle.cos(degrees(dir))*5/1024;}
 static int gripY(int dir){return Battle.sin(degrees(dir))*5/1024;}
}
