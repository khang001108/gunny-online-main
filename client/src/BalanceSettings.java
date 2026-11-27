/** HF45: server-selected equipment maxima. Immutable array replaced only on startup / client health. */
final class BalanceSettings {
 static final String[] KEYS={"weaponAttackPct","armorAttack","armorDefense","armorLuck","hatAttack","hatDefense","hatLuck","glassesAttack","glassesDefense","glassesLuck","critCapPct","penetrationCapPct","defenseCapPct","equalTurnCapPct"};
 static final int[] DEFAULT={100,293,1500,100,293,1500,100,312,312,300,15,10,40,80};
 static final int[] MIN={25,5,10,5,5,10,5,5,5,10,0,0,0,50};
 static final int[] MAX={200,2000,5000,2000,2000,5000,2000,2000,5000,2000,40,20,80,100};
 static volatile int[] values=DEFAULT;
 static int[] validate(int[] a){if(a.length!=KEYS.length)throw new IllegalArgumentException("Sai so luong thong so");for(int i=0;i<a.length;i++)if(a[i]<MIN[i]||a[i]>MAX[i])throw new IllegalArgumentException("Thong so ngoai gioi han: "+KEYS[i]);return a;}
 static String encode(int[] a){StringBuffer b=new StringBuffer();for(int i=0;i<a.length;i++){if(i>0)b.append(',');b.append(a[i]);}return b.toString();}
 static int[] decode(String s){int[] a=new int[KEYS.length];int start=0;for(int i=0;i<a.length;i++){int end=s.indexOf(',',start);if(i==a.length-1){if(end>=0)throw new IllegalArgumentException("Sai cau hinh");end=s.length();}else if(end<0)throw new IllegalArgumentException("Thieu cau hinh");a[i]=Integer.parseInt(s.substring(start,end));start=end+1;}return validate(a);}
 static int weaponAttack(int item,int rank,int[] a){int base=Battle.baseDamage(item)/2;return base+(Battle.baseDamage(item)*(Math.max(0,Math.min(15,rank))+1)/2-base)*a[0]/100;}
 static int equipmentStat(int kind,int item,int rank,int stat,int[] a){int base=stat==0?ItemRules.attack(kind,item):stat==1?ItemRules.defense(kind,item):ItemRules.luck(kind,item);rank=Math.max(0,Math.min(15,rank));if(kind==0)return stat==0?weaponAttack(item,rank,a):base;int offset=kind==2?1:kind==3?4:kind==6?7:-1;return offset<0?base:base+(a[offset+stat]-base)*rank/15;}
}
