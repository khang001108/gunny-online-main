/** Percent weights for Lv1..5: higher difficulty increases the rare-stone share. */
final class StoneDrops {
 static final int[][] WEIGHTS={{65,25,8,2,0},{25,35,25,12,3},{5,15,30,35,15}};
 static int grade(int difficulty,int roll){if(difficulty<0||difficulty>2||roll<0||roll>=100)throw new IllegalArgumentException("Invalid stone roll");int total=0;for(int i=0;i<5;i++){total+=WEIGHTS[difficulty][i];if(roll<total)return i+1;}throw new IllegalStateException();}
 static int type(int grade){return grade==1?1:grade+2;}
}
