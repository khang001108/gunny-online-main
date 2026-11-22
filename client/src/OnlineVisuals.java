/** Presentation clocks survive repeated/stale snapshots; never drive physics. */
final class OnlineVisuals {
 final long[] numericKeys=new long[128],numericStarts=new long[128],landKeys=new long[Battle.MAX_UNITS],flashKeys=new long[Battle.MAX_UNITS];
 final boolean[] numericUsed=new boolean[128],landUsed=new boolean[Battle.MAX_UNITS],flashUsed=new boolean[Battle.MAX_UNITS];int numericNext;
 static long mix(long key,int value){return key*1000003L+value;}
 int elapsedNumber(long key,int age,long now){for(int i=0;i<128;i++)if(numericUsed[i]&&numericKeys[i]==key)return Math.max(age,(int)Math.max(0,(now-numericStarts[i])*30/1000));int at=numericNext++%128;numericUsed[at]=true;numericKeys[at]=key;numericStarts[at]=now-age*1000L/30;return age;}
 int remainingNumber(int value,int total,long key,int actor,boolean land,long now){if(value<=0)return 0;long[] names=land?landKeys:flashKeys,clocks=land?landing:flash;boolean[] used=land?landUsed:flashUsed;if(!used[actor]||names[actor]!=key){used[actor]=true;names[actor]=key;clocks[actor]=now-(total-value)*1000L/30;}return Math.min(value,Math.max(0,total-(int)Math.max(0,(now-clocks[actor])*30/1000)));}
 final long[] landing=new long[Battle.MAX_UNITS],flash=new long[Battle.MAX_UNITS];
 void reset(){numericNext=0;for(int i=0;i<128;i++)numericUsed[i]=false;for(int i=0;i<Battle.MAX_UNITS;i++){landUsed[i]=flashUsed[i]=false;landing[i]=flash[i]=0;}}
 void age(Battle b,long now){
  long event=mix(mix(b.turn,b.shotSerial),b.hitSerial);
  for(int i=0;i<b.unitCount;i++){Battle.Unit u=b.units[i];u.landingTicks=remainingNumber(u.landingTicks,32,mix(mix(mix(b.shotSerial,i),u.x),u.y),i,true,now);u.flashTicks=remainingNumber(u.flashTicks,36,mix(mix(mix(mix(b.turn,i),u.lastBuff),b.usedPropCount),u.npc>=3?b.hitSerial:0),i,false,now);}
  for(int i=b.damagePopupCount-1;i>=0;i--){Battle.DamagePopup p=b.damagePopups[i];int ordinal=0;for(int j=0;j<i;j++)if(b.damagePopups[j].x==p.x&&b.damagePopups[j].y==p.y&&b.damagePopups[j].value==p.value)ordinal++;p.age=elapsedNumber(mix(mix(mix(mix(mix(event,1),p.x),p.y),p.value),ordinal),p.age,now);if(p.age>=42){for(int j=i+1;j<b.damagePopupCount;j++)b.damagePopups[j-1]=b.damagePopups[j];b.damagePopups[--b.damagePopupCount]=null;}}
  for(int i=b.blastCount-1;i>=0;i--){Battle.Blast p=b.blasts[i];p.time=elapsedNumber(mix(mix(mix(mix(event,2),p.x),p.y),p.weapon),p.time,now);if(p.time>24){b.blasts[i]=b.blasts[--b.blastCount];b.blasts[b.blastCount]=null;}}
 }
}
