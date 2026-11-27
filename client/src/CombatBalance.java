/** HF41: shared combat balance; integer arithmetic for Java ME. */
final class CombatBalance {
 static int critChance(int luck){return critChance(luck,BalanceSettings.values);}
 static int critChance(int luck,int[] cfg){return Math.min(cfg[10]*100,Math.max(0,luck));}
 static int penetration(int luck){return penetration(luck,BalanceSettings.values);}
 static int penetration(int luck,int[] cfg){return Math.min(cfg[11],Math.max(0,luck)/100);}
 static int mitigate(int damage,int defense,int luck){return mitigate(damage,defense,luck,false);}
 static int mitigate(int damage,int defense,int luck,boolean pvp){return mitigate(damage,defense,luck,BalanceSettings.values);}
 static int mitigate(int damage,int defense,int luck,int[] cfg){if(damage<=0)return 0;int effective=Math.max(0,defense)*(100-penetration(luck,cfg))/100;return damage-Math.min(effective,damage*cfg[12]/100);}
 static int projectileDamage(int base,int boost,boolean triple,int extra,boolean pow,boolean pvp){int value=base*(100+boost)/100;if(triple)value=value*60/100;if(extra>0)value=value*82/100;if(pow)value=value*170/100;return value;}
 static int hit(int damage,int defense,int luck,int roll){return hit(damage,defense,luck,roll,BalanceSettings.values);}
 static int hit(int damage,int defense,int luck,int roll,int[] cfg){int value=mitigate(damage,defense,luck,cfg);return roll<critChance(luck,cfg)?value*150/100:value;}
 static int roll(Battle b,int owner,int target,int x,int y,int age){int h=b.turn*0x9e3779b9+b.shotSerial*0x85ebca6b+owner*0x27d4eb2d+target*0x165667b1+x*131+y*8191+age*257+b.wind;h^=h>>>16;h*=0x7feb352d;h^=h>>>15;h*=0x846ca68b;h^=h>>>16;return (h&0x7fffffff)%10000;}
 static int attackLuck(Battle b,int owner,int target){Battle.Unit a=b.units[owner];return a.statsEnabled&&!b.ally(owner,target)?a.luck:0;}
 static int targetDefense(Battle b,int target){Battle.Unit v=b.units[target];return v.statsEnabled?v.defense:0;}
 static int damage(Battle b,int owner,int target,int raw,int x,int y,int age){int luck=attackLuck(b,owner,target);int value=mitigate(raw,targetDefense(b,target),luck,b.dungeonStage==0);return roll(b,owner,target,x,y,age)<critChance(luck)?value*150/100:value;}
 // Only actual critical hits are red; passive penetration must not flash as a critical.
 static boolean special(Battle b,int owner,int target,int raw,int x,int y,int age){return raw>0&&roll(b,owner,target,x,y,age)<critChance(attackLuck(b,owner,target));}
 // A comparable opponent at full HP gets a chance to respond. Stronger gear/level can still one-shot weaker opponents.
 static int limitTurnDamage(Battle b,int owner,int target,int value){
  if(BalanceSettings.values[13]>=100||value<=0||b.dungeonStage!=0||b.ally(owner,target))return value;
  Battle.Unit a=b.units[owner],v=b.units[target];if(!a.statsEnabled||!v.statsEnabled)return value;
  int attackA=Battle.baseDamage(a.weapon)+a.attackBonus,attackV=Battle.baseDamage(v.weapon)+v.attackBonus;
  if(Progression.level(a.experience)>Progression.level(v.experience)+5||attackA*100>Math.max(1,attackV)*125||a.maxHp*100>v.maxHp*125)return value;
  int allowed=Math.max(0,v.maxHp*BalanceSettings.values[13]/100-b.pvpTurnDamage[target]);int hit=Math.min(value,allowed);b.pvpTurnDamage[target]+=Math.min(v.hp,hit);return hit;
 }
 static int firstWeight(Battle.Unit u){return u.hp>0?Math.max(0,u.luck)+1:0;}
 static int firstActor(Battle b){int total=0;for(int i=0;i<b.unitCount;i++)total+=firstWeight(b.units[i]);if(total<=0)return 0;int roll=b.rand(total);for(int i=0;i<b.unitCount;i++){roll-=firstWeight(b.units[i]);if(roll<0)return i;}return 0;}
}
