public class SoloQueueControlCheck {
 static int n;
 static void eq(Object a,Object b){if(!a.equals(b))throw new AssertionError(a+" != "+b);n++;}
 public static void main(String[] args){
  SoloQueueControl q=new SoloQueueControl();long t=100000;
  q.observe(1,t,60,false);eq(q.next(1,t),"sync");eq(q.next(1,t+1),"");
  q.request(1,"botvote");eq(q.next(1,t+2),"botvote");
  q.request(1,"botvote");q.request(1,"cancelquick");q.request(1,"botvote");eq(q.next(1,t+3),"cancelquick");
  q.observe(1,t+10000,60,false);eq(q.seconds(1,t+10000),50);
  q.observe(1,t+20000,-1,false);eq(q.seconds(1,t+20000),40);
  eq(q.next(1,t+60000),"cancelquick");q.clear();eq(q.next(1,t+60001),"");
  q.observe(2,t,-1,false);eq(q.seconds(2,t),60);q.observe(2,t+1000,0,true);eq(q.next(2,t+1000),"cancelquick");
  q.clear();q.observe(3,t,60,false);q.request(2,"botvote");eq(q.next(3,t),"sync");
  q.request(3,"botvote");eq(q.next(3,t+60000),"cancelquick");
  System.out.println("PASS: "+n+" queue control checks");
 }
}
