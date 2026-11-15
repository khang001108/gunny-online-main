/** Shared shop catalog; prices are also enforced by the checkout SQL transaction. */
final class ShopCatalog {
 static final int[] PRICES={0,650,950,450,550,800,1100,700,600,1200};
 static boolean weapon(int id){return id==0||id==1||id==2||id==3||id==4||id==5||id==7||id==9;}
 static int price(int kind,int item){return item==0?0:kind==0?180:kind==1?350:kind==2?200:kind==3?130:kind==5?35:100;}
 static void validate(int[] cart){if(cart.length!=7)throw new IllegalArgumentException("Sai giỏ hàng");for(int k=0;k<7;k++){if(cart[k]<0||(cart[k]&~1023)!=0)throw new IllegalArgumentException("Sai món hàng");for(int i=0;i<10;i++)if((cart[k]&(1<<i))!=0&&k==0&&!weapon(i))throw new IllegalArgumentException("Sai vũ khí");}}
 static int total(int sex,int[] cart,int owned,int[] wardrobe){validate(cart);if(sex<0||sex>1)throw new IllegalArgumentException("Sai nhân vật");int total=0;for(int k=0;k<7;k++)for(int i=0;i<10;i++)if((cart[k]&(1<<i))!=0&&((k==0?owned:wardrobe[sex*6+k-1])&(1<<i))==0)total+=price(k,i);return total;}
 static void grant(int sex,int[] cart,int[] wardrobe){for(int k=1;k<7;k++)wardrobe[sex*6+k-1]|=cart[k];}
}
