import java.util.*;
import java.util.concurrent.*;

public class CraftingChanceCheck {
    static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError(message);
        System.out.println("PASS: "+message);
    }
    static Map<String,Object> attempt(int rank,int grade,int quantity,boolean luck,boolean protect) {
        return Map.of("sex",0,"kind",0,"item",0,"rank",rank,"grade",grade,
                      "quantity",quantity,"lucky",luck,"protect",protect);
    }
    static void reject(Runnable action,String label) {
        try{action.run();}catch(IllegalArgumentException expected){check(true,label);return;}
        throw new AssertionError(label);
    }
    public static void main(String[] args)throws Exception {
        for(int rank=0;rank<15;rank++) {
            for(int grade=1;grade<=5;grade++) {
                int chance=ForgeRules.chance(rank,grade,1,false);
                check(chance>0&&chance<=10000,"Bounded probability at +"+rank+" stone "+grade);
                check(ForgeRules.chance(rank,grade,3,true)>=chance,"Additional stones and luck do not reduce success");
            }
        }
        check(ForgeRules.chance(2,1,1,true)==5000,"40% with a 25% charm becomes 50%");
        var store=new CloudStore.Memory();
        store.register("craft@test.local","test-password-123","Crafter");
        var profile=store.profile(store.login("craft@test.local","test-password-123").userId);
        profile.shoppingMoney=20000;profile.stones=100;profile.owned=1;
        store.crafting(profile.id,"buy",Map.of("material",5,"quantity",2),"buy-luck",0);
        check(profile.luckyCharms==2&&profile.shoppingMoney==19400,"Luck charms cost 300 each");
        store.crafting(profile.id,"buy",Map.of("material",5,"quantity",2),"buy-luck",99);
        check(profile.luckyCharms==2&&profile.shoppingMoney==19400,"Purchase retry cannot charge twice");
        profile.forgeLevels[0]=2;
        store.crafting(profile.id,"forge",attempt(2,1,1,true,false),"fail-low",9999);
        check(profile.forgeLevels[0]==2&&profile.stones==99&&profile.luckyCharms==1,"Low-level failure consumes stone and charm, retaining rank");
        store.crafting(profile.id,"forge",attempt(2,1,1,true,false),"fail-low",0);
        check(profile.forgeLevels[0]==2&&profile.stones==99&&profile.luckyCharms==1,"Retry uses the stored failed roll");
        profile.forgeLevels[0]=5;profile.protectionCharms=1;
        store.crafting(profile.id,"forge",attempt(5,1,1,false,true),"protected",9999);
        check(profile.forgeLevels[0]==5&&profile.protectionCharms==0,"Protection prevents downgrade and is consumed");
        store.crafting(profile.id,"forge",attempt(5,1,1,false,false),"downgrade",9999);
        check(profile.forgeLevels[0]==4,"Unprotected failure from +5 loses one rank");
        profile.craftStones[3]=3;
        store.crafting(profile.id,"forge",attempt(4,5,3,false,false),"win",0);
        check(profile.forgeLevels[0]==5&&profile.craftStones[3]==0,"Grade 5 stones are consumed instead of base stones");
        int stones=profile.stones;
        reject(()->store.crafting(profile.id,"forge",attempt(4,1,1,false,false),"stale",0),"Stale expected rank rejects before spending");
        reject(()->store.crafting(profile.id,"forge",attempt(5,5,3,false,false),"empty",0),"Unavailable material rejects before spending");
        check(profile.stones==stones,"Rejected attempts keep inventory");
        profile.forgeLevels[0]=0;
        var pool=Executors.newFixedThreadPool(2);
        try {
            var calls=new ArrayList<Callable<Void>>();
            for(int i=0;i<2;i++)calls.add(()->{
                store.crafting(profile.id,"forge",attempt(0,1,1,false,false),"race",0);
                return null;
            });
            for(var result:pool.invokeAll(calls))result.get();
        }finally{pool.shutdownNow();}
        check(profile.forgeLevels[0]==1&&profile.stones==stones-1,"Concurrent retry upgrades exactly once");

        var ground=new Battle.Ground(){
            public boolean solid(int x,int y){return y>=200;}
            public int surface(int x,int from){return Math.max(from,200);}
            public void crater(int x,int y,int r){}
        };
        var starts=new HashSet<Integer>();
        for(int seed=1;seed<30;seed++){
            var battle=new Battle(ground);battle.seed=seed;battle.start(true);
            starts.add(battle.units[0].x);
            check(battle.units[0].x<240&&battle.units[1].x>240,"Random PvP spawn preserves opposite sides");
        }
        check(starts.size()>10,"PvP start position changes across matches");
    }
}
