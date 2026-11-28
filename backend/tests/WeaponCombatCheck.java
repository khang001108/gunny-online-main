/** Mixed Common/Special impacts, bounded visual trails, and compatible snapshot flag. */
public final class WeaponCombatCheck {
    static int checks;
    static void ok(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
    public static void main(String[] args){
        Battle.Ground ground=new Battle.Ground(){public boolean solid(int x,int y){return y>=200;}public int surface(int x,int from){return 200;}public void crater(int x,int y,int radius){}};
        for(int weapon:new int[]{0,1,2,3,4,5,7,9}){
            Battle b=new Battle(ground);b.start(true);b.units[0].weapon=weapon;
            for(boolean powered:new boolean[]{false,true}){
                b.pow=powered;Battle.Ball shot=b.make(45,60);int damage=shot.damage,radius=shot.radius;
                b.stampTrail(shot);ok(b.trailPowered[b.trailCount-1]==powered,"Trail retains firing mode");
                shot.x=250*256;shot.y=200*256;b.impact(shot);
                Battle.Blast blast=b.blasts[b.blastCount-1];ok(blast.powered==powered,"Impact retains firing mode");
                ok(shot.damage==damage&&shot.radius==radius,"Art does not mutate combat values");
                Battle.Blast decoded=new Battle.Blast();decoded.powered=!powered;
                Battle.readBlastRadius(decoded,Battle.blastWireRadius(blast));
                ok(decoded.powered==powered&&decoded.radius==radius,"Snapshot mode and radius round trip/reset");
                ok((Battle.blastWireRadius(blast)&127)==radius,"Existing low radius bits stay identical");
            }
            for(int tick=0;tick<40;tick++){Battle.Ball p=b.make(45,60);p.age=tick;int frame=Battle.visualFrame(p);ok(frame>=0&&frame<16,"16-direction frame in range");}
        }
        Battle b=new Battle(ground);b.start(true);Battle.Ball shot=b.make(45,50);
        shot.weapon=9;int[][] v={{100,0,0},{0,-100,4},{-100,0,8},{0,100,12}};
        for(int[] a:v){shot.vx=a[0];shot.vy=a[1];ok(Battle.visualFrame(shot)==a[2],"Electricity follows real velocity");}
        shot.weapon=0;b.stampTrail(shot);b.trailAge[0]=14;shot.powered=true;b.stampTrail(shot);b.ageTrails();
        ok(b.trailCount==1&&b.trailPowered[0],"Trail compaction retains powered metadata");
        for(int i=0;i<20;i++){shot.powered=i%2==0;shot.x=(200+i)*256;b.impact(shot);}
        ok(b.blastCount==16,"Multi-projectile impacts stay bounded");
        for(int i=0;i<25;i++)b.tickVisuals();ok(b.blastCount==0,"Impact art keeps original lifetime");
        System.out.println("PASS WeaponCombatCheck: "+checks+" checks");
    }
}
