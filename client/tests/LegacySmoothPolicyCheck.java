import java.io.*;
public final class LegacySmoothPolicyCheck {
    static void check(boolean value,String name){if(!value)throw new AssertionError(name);}
    public static void main(String[] args)throws Exception{
        FrameInput source=new FrameInput();DataInputStream in=new DataInputStream(source);
        for(int i=0;i<5000;i++){
            source.bind(new byte[]{1,2,3,4,5,6});
            check(in.readInt()==0x01020304,"integer");check(in.readUnsignedShort()==0x0506,"short");
            check(in.available()==0&&in.read()==-1,"end");
            source.bind(new byte[]{7,8,9});check(in.skipBytes(1)==1,"skip");
            byte[] dst=new byte[4];in.readFully(dst,1,2);check(dst[1]==8&&dst[2]==9,"readFully");
            source.bind(null);check(in.available()==0&&in.read()==-1,"release");
        }
        check(!BattleNetworkPolicy.lowBufferDue(32,true,18),"low-buffer pacing");
        check(BattleNetworkPolicy.lowBufferDue(33,true,18),"refill early");
        check(!BattleNetworkPolicy.lowBufferDue(100,true,19),"normal buffer");
        check(!BattleNetworkPolicy.lowBufferDue(100,false,0),"no receive window");
        System.out.println("PASS: 5000 reader rebinds and low-buffer pacing");
    }
}
