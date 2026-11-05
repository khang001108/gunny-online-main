/** Presentation clocks survive repeated/stale snapshots; never drive physics. */
final class OnlineVisuals {
 final long[] landing=new long[6],flash=new long[6];final String[] landingKey=new String[6],flashKey=new String[6];
 final String[] keys=new String[128];final long[] starts=new long[128];int next;
 void reset(){for(int i=0;i<6;i++){landingKey[i]=flashKey[i]=null;landing[i]=flash[i]=0;}for(int i=0;i<keys.length;i++)keys[i]=null;next=0;}
 int elapsed(String key,int age,long now){for(int i=0;i<keys.length;i++)if(key.equals(keys[i]))return Math.max(age,(int)Math.max(0,(now-starts[i])*30/1000));int id=next++%keys.length;keys[id]=key;starts[id]=now-age*1000L/30;return age;}
 int remaining(int value,int total,String key,int actor,boolean land,long now){if(value<=0)return 0;String[] names=land?landingKey:flashKey;long[] clocks=land?landing:flash;if(!key.equals(names[actor])){names[actor]=key;clocks[actor]=now-(total-value)*1000L/30;}return Math.min(value,Math.max(0,total-(int)Math.max(0,(now-clocks[actor])*30/1000)));}
 void age(Battle b,long now){
  String event=b.turn+":"+b.shotSerial+":"+b.hitSerial;
  for(int i=0;i<b.unitCount;i++){Battle.Unit u=b.units[i];u.landingTicks=remaining(u.landingTicks,32,b.shotSerial+":"+i+":"+u.x+":"+u.y,i,true,now);u.flashTicks=remaining(u.flashTicks,36,b.turn+":"+i+":"+u.lastBuff+":"+b.usedPropCount,i,false,now);}
  for(int i=b.damagePopupCount-1;i>=0;i--){Battle.DamagePopup p=b.damagePopups[i];int ordinal=0;for(int j=0;j<i;j++)if(b.damagePopups[j].x==p.x&&b.damagePopups[j].y==p.y&&b.damagePopups[j].value==p.value)ordinal++;p.age=elapsed("d:"+event+":"+p.x+":"+p.y+":"+p.value+":"+ordinal,p.age,now);if(p.age>=42){for(int j=i+1;j<b.damagePopupCount;j++)b.damagePopups[j-1]=b.damagePopups[j];b.damagePopups[--b.damagePopupCount]=null;}}
  for(int i=b.blastCount-1;i>=0;i--){Battle.Blast p=b.blasts[i];p.time=elapsed("b:"+event+":"+p.x+":"+p.y+":"+p.weapon,p.time,now);if(p.time>24){b.blasts[i]=b.blasts[--b.blastCount];b.blasts[b.blastCount]=null;}}
 }
}
