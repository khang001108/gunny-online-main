import java.io.*;
import java.util.*;

/** Regression: a validated playback segment reuses storage without changing decoded bytes. */
public final class DeltaReusableCheck {
    static DataInputStream input(byte[] packet) { return new DataInputStream(new ByteArrayInputStream(packet)); }
    public static void main(String[] args) throws Exception {
        Random random=new Random(7818);byte[] original=null,decoded=null;int allocated=0;
        for(int tick=0;tick<2000;tick++) {
            byte[] frame=original==null||tick%100==0?new byte[1200+tick%3*100]:original.clone();
            for(int k=0;k<20;k++)frame[random.nextInt(frame.length)]=(byte)random.nextInt(256);
            byte[] delta=FrameDelta.encode(original,frame);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            out.writeShort((delta==null?frame.length:delta.length)|(delta==null?0:32768));out.write(delta==null?frame:delta);
            byte[] packet=bytes.toByteArray();
            SnapshotDelta.validate(input(packet),decoded==null?0:decoded.length);
            byte[] old=decoded;decoded=SnapshotDelta.readReusable(input(packet),decoded);
            if(!Arrays.equals(frame,decoded))throw new AssertionError("Incorrect frame at "+tick);
            if(old!=null&&old.length==frame.length&&old!=decoded)throw new AssertionError("Unnecessary buffer allocation");
            if(old!=decoded)allocated++;
            original=frame;
        }
        if(allocated>=100)throw new AssertionError("Too many buffers: "+allocated);
        System.out.println("PASS: 2000 frames; "+allocated+" decoder array allocations");
    }
}
