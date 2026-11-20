/** Deterministic equipment growth, shared by server, MIDP and PostgreSQL tests. */
final class ForgeRules {
 static final int SLOTS=130,MAX_RANK=15,STONE_PRICE=80;
 static boolean allowed(int kind){return kind==0||kind==2||kind==3||kind==6;}
 static int index(int sex,int kind,int item){if(sex<0||sex>1||kind<0||kind>6||item<0||item>9)throw new IllegalArgumentException("Sai trang bị");return kind==0?item:10+sex*60+(kind-1)*10+item;}
 static int cost(int rank){if(rank<0||rank>=MAX_RANK)throw new IllegalArgumentException("Đã đạt cấp rèn tối đa");return 250*(rank+1);}
 static final int[] BASE_CHANCE={8000,6000,4000,2500,1500,900,500,250,120,60,30,15,8,4,2};
 static int chance(int rank,int grade,int quantity,boolean lucky){
  if(rank<0||rank>=MAX_RANK||grade<1||grade>5||quantity<1||quantity>3)return 0;
  int value=BASE_CHANCE[rank]*(1<<(grade-1))*quantity;
  return Math.min(10000,lucky?value*125/100:value);
 }
 static int price(int material){return material<5?80*(1<<material):material==5?300:500;}
 static int result(int rank,boolean success,boolean protection){
  return success?Math.min(MAX_RANK,rank+1):rank>=5&&!protection?rank-1:rank;
 }
 static int bonus(int[] ranks,int sex,int kind,int item){return 5*ranks[index(sex,kind,item)];}
 static int[] equipped(int[] ranks,int sex,int mask,int weapon,int skin,int[] look){int[] b=new int[3];for(int k=0;k<7;k++)if((mask&(1<<k))!=0){int n=bonus(ranks,sex,k,k==0?weapon:k==1?skin:look[k-2]);if(k==0)b[0]+=n;else if(k==1){b[0]+=n;b[1]+=n;b[2]+=n;}else b[1]+=n;}return b;}
 static void apply(Battle.Unit u,int[] bonuses){u.forgeAttack=bonuses[0];u.forgeDefense=bonuses[1];u.forgeLuck=bonuses[2];}
 static void apply(Battle.Unit u,int[] bonuses,int[] ranks){apply(u,bonuses);for(int i=0;i<10;i++)u.weaponForge[i]=ranks[i]*5;u.forgeWeaponBonus=u.weaponForge[u.weapon];}
 static int[] gear(int mask,int weapon,int[] forge){int clothes=ItemRules.clothes(mask)*5;return new int[]{clothes+((mask&1)!=0?Battle.baseDamage(weapon):0)+forge[0],clothes+((mask&1)!=0?ItemRules.DEFENSE[weapon]:0)+forge[1],clothes+((mask&1)!=0?ItemRules.LUCK[weapon]:0)+forge[2]};}
}
