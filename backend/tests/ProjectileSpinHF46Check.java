/** Visual policy for both native and legacy projectile atlases. */
public final class ProjectileSpinHF46Check {
    static int checks;
    static void ok(boolean b,String msg){if(!b)throw new AssertionError(msg);checks++;}
    public static void main(String[] args){
        Battle.Ball b=new Battle.Ball();
        int[][] cardinal={{1024,0,0},{0,-1024,4},{-1024,0,8},{0,1024,12}};
        for(int w=0;w<10;w++)for(int p=0;p<2;p++){
            b.weapon=w;b.powered=p!=0;
            if(!Battle.nativeWeapon(w))continue;
            boolean direction=w==9||p==1&&(w==0||w==2);
            int step=w==1||w==7||p==1&&(w==3||w==4)?25:10;
            ok(Battle.directionalProjectile(w,b.powered)==direction,"direction policy");
            for(int[] v:cardinal){b.vx=v[0];b.vy=v[1];
                for(int age=0;age<=1600;age++){
                    b.age=age;b.x=12345;b.y=45678;b.damage=120;b.radius=15;
                    int f=direction?v[2]:age/step%16;
                    ok(Battle.visualFrame(b)==f,"native frame w="+w+" age="+age);
                    ok(Battle.ballFrame(b)==f/2,"fallback frame");
                    ok(b.age==age&&b.x==12345&&b.y==45678&&b.vx==v[0]&&b.vy==v[1]&&b.damage==120&&b.radius==15,"visual policy cannot mutate physics");
                }
            }
        }
        b.weapon=6;b.powered=false;
        for(int[] v:cardinal){b.vx=v[0];b.vy=v[1];ok(Battle.visualFrame(b)==v[2]/2,"bow unchanged");}
        System.out.println("PASS ProjectileSpinHF46Check: "+checks+" checks");
    }
}
