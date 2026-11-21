/** Shared deck shape and public stage numbering; saved encounter IDs stay compatible. */
final class DungeonRewards {
 static boolean crossing(int kind,int stage){return kind==3&&(stage==1||stage==3||stage==4);}
 static int cards(int kind,int stage){return kind>=2&&!crossing(kind,stage)?18:9;}
 static int picks(int kind,int stage){return kind>=2&&!crossing(kind,stage)?2:1;}
 static int nextTribal(int stage){return stage==3?4:6;}
 static int tribalNumber(int stage){return stage>=5?3:stage-2;}
}
