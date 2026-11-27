/** HF44: fixed PvE balance; all stats fit existing unsigned-short snapshot fields. */
final class DungeonBalance {
 static final int MAX_DIFFICULTY=3;
 // NPC IDs 1..15. Rows: red ant, blue ant, queen, spear, javelin, elite, bean,
 // Bulan brother/sister, chain/fatigue/heal/bomb totems, Sulan gold/red.
 private static final int[] HP={0,360,480,5800,650,750,2800,1800,6500,6500,850,850,1200,1000,7500,9000};
 private static final int[] ATK={0,210,260,650,320,360,570,0,740,740,0,0,0,450,740,900};
 private static final int[] DEF={0,35,50,170,75,90,180,70,220,220,90,90,130,110,260,320};
 private static final int[] LUCK={0,20,30,90,45,50,100,0,140,140,0,0,0,60,180,240};
 private static final int[] HP_TIER={100,145,205,310}, ATK_TIER={100,140,200,300}, DEF_TIER={100,150,220,330}, LUCK_TIER={100,170,260,420};
 static int tier(int d){return Battle.clamp(d,0,MAX_DIFFICULTY);}
 // Rewards stay on the old 0..2 contract, including Supabase RPCs.
 static int rewardTier(int d){return Math.min(2,tier(d));}
 static boolean boss(int npc){return npc==3||npc==TribalRules.ELITE||npc==TribalRules.BROTHER||npc==TribalRules.SISTER||npc==TribalRules.SULAN||npc==TribalRules.RAGE;}
 static int party(Battle b){int n=0;for(int i=0;i<6;i+=2)if(b.units[i].hp>0&&b.units[i].npc==0)n++;return Battle.clamp(n,1,3);}
 static int stageFactor(int stage,int npc){if(npc==1||npc==2)return stage==2?125:100;if(npc==TribalRules.SPEAR||npc==TribalRules.JAVELIN||npc==TribalRules.ELITE)return stage>=6?135:stage>=4?120:100;return 100;}
 static int hp(int stage,int npc,int d,int party){int value=HP[npc]*HP_TIER[tier(d)]/100*stageFactor(stage,npc)/100;if(boss(npc))value=value*(100+60*(Battle.clamp(party,1,3)-1))/100;return Math.min(65000,value);}
 static int attack(int stage,int npc,int d){return ATK[npc]*ATK_TIER[tier(d)]/100*stageFactor(stage,npc)/100;}
 static int defense(int stage,int npc,int d){return DEF[npc]*DEF_TIER[tier(d)]/100*stageFactor(stage,npc)/100;}
 static int luck(int stage,int npc,int d){return Math.min(1000,LUCK[npc]*LUCK_TIER[tier(d)]/100);}
 static int level(int stage,int d){return Math.min(70,(stage<=2?15:30)+(stage==2?5:stage>=3?(stage-3)*5:0)+tier(d)*10);}
 static void stats(Battle b,Battle.Unit u){if(u.npc<=0||u.npc>=HP.length)return;u.statsEnabled=true;u.attackBonus=attack(b.dungeonStage,u.npc,b.dungeonDifficulty)-Battle.baseDamage(u.weapon);u.defense=defense(b.dungeonStage,u.npc,b.dungeonDifficulty);u.luck=luck(b.dungeonStage,u.npc,b.dungeonDifficulty);u.experience=Progression.experienceForLevel(level(b.dungeonStage,b.dungeonDifficulty));}
 static void spawn(Battle b,Battle.Unit u){stats(b,u);u.maxHp=hp(b.dungeonStage,u.npc,b.dungeonDifficulty,b.dungeonParty);u.hp=u.maxHp;u.maxEnergy=u.energy=240;u.forgeAttack=u.forgeDefense=u.forgeLuck=u.forgeWeaponBonus=u.wingHp=0;for(int i=0;i<u.weaponForge.length;i++)u.weaponForge[i]=0;}
 static int attackOf(Battle b,int actor){Battle.Unit u=b.units[actor];return Math.max(0,Battle.baseDamage(u.weapon)+u.attackBonus);}
 static int heal10(Battle.Unit u){return u.maxHp/10;}
}
