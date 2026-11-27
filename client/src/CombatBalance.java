/** HF41: shared combat balance; integer arithmetic for Java ME. */
final class CombatBalance {
 static int critChance(int luck){return Math.min(4000,Math.max(0,luck)*4);}
 static int penetration(int luck){return Math.min(20,Math.max(0,luck)/50);}
 static int mitigate(int damage,int defense,int luck){if(damage<=0)return 0;int effective=Math.max(0,defense)*(100-penetration(luck))/100;return damage-Math.min(effective,damage*40/100);}
 static int hit(int damage,int defense,int luck,int roll){int value=mitigate(damage,defense,luck);return roll<critChance(luck)?value*150/100:value;}
 static int roll(Battle b,int owner,int target,int x,int y,int age){int h=b.turn*0x9e3779b9+b.shotSerial*0x85ebca6b+owner*0x27d4eb2d+target*0x165667b1+x*131+y*8191+age*257+b.wind;h^=h>>>16;h*=0x7feb352d;h^=h>>>15;h*=0x846ca68b;h^=h>>>16;return (h&0x7fffffff)%10000;}
 static int attackLuck(Battle b,int owner,int target){Battle.Unit a=b.units[owner];return a.statsEnabled&&!b.ally(owner,target)?a.luck:0;}
 static int targetDefense(Battle b,int target){Battle.Unit v=b.units[target];return v.statsEnabled?v.defense:0;}
 static int damage(Battle b,int owner,int target,int raw,int x,int y,int age){return hit(raw,targetDefense(b,target),attackLuck(b,owner,target),roll(b,owner,target,x,y,age));}
 // Same deterministic roll as damage(); penetration is highlighted only when it changes the hit.
 static boolean special(Battle b,int owner,int target,int raw,int x,int y,int age){int luck=attackLuck(b,owner,target),def=targetDefense(b,target);return raw>0&&(roll(b,owner,target,x,y,age)<critChance(luck)||mitigate(raw,def,luck)>mitigate(raw,def,0));}
 static int firstWeight(Battle.Unit u){return u.hp>0?Math.max(0,u.luck)+1:0;}
 static int firstActor(Battle b){int total=0;for(int i=0;i<b.unitCount;i++)total+=firstWeight(b.units[i]);if(total<=0)return 0;int roll=b.rand(total);for(int i=0;i<b.unitCount;i++){roll-=firstWeight(b.units[i]);if(roll<0)return i;}return 0;}
}
