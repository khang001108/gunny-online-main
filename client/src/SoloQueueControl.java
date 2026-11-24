/** Independent wall-clock deadline and priority commands for a solo ticket. CLDC 1.1. */
final class SoloQueueControl {
    private int room;
    private long deadline, nextPoll;
    private String action="";
    synchronized void clear(){room=0;deadline=nextPoll=0;action="";}
    synchronized void observe(int id, long now, int seconds, boolean expired){
        if(id<=0){clear();return;}
        if(room!=id){room=id;deadline=now+60000L;nextPoll=0;action="";}
        if(expired)deadline=now;
        else if(seconds>=0)deadline=Math.min(deadline,now+Math.min(60,seconds)*1000L);
    }
    synchronized int seconds(int id,long now){return id!=room||room==0?0:(int)Math.max(0,Math.min(60,(deadline-now+999)/1000));}
    synchronized void request(int id,String op){if(id!=room||room==0)return;if(op.equals("cancelquick")||action.length()==0)action=op;}
    synchronized String next(int id,long now){
        if(id!=room||room==0)return "";
        if(now>=deadline)action="cancelquick";
        if(action.length()>0){String op=action;action="";return op;}
        if(now>=nextPoll){nextPoll=now+500;return "sync";}
        return "";
    }
}
