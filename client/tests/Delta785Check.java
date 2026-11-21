import java.io.*;import java.util.*;
public class Delta785Check {
 public static void main(String[] args)throws Exception{Random random=new Random(15);long full=0,encoded=0;byte[] previous=null,decoded=null;int cases=0;for(int tick=0;tick<1000;tick++){byte[] frame=previous==null?new byte[1400]:previous.clone();for(int i=0;i<24;i++)frame[random.nextInt(frame.length)]=(byte)random.nextInt(256);byte[] delta=FrameDelta.encode(previous,frame);var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeShort((delta==null?frame.length:delta.length)|(delta==null?0:32768));out.write(delta==null?frame:delta);byte[] packet=bytes.toByteArray();SnapshotDelta.validate(new DataInputStream(new ByteArrayInputStream(packet)),decoded==null?0:decoded.length);decoded=SnapshotDelta.read(new DataInputStream(new ByteArrayInputStream(packet)),decoded);if(!Arrays.equals(frame,decoded))throw new AssertionError("Frame mismatch");full+=frame.length+2;encoded+=packet.length;previous=frame;cases++;if(tick%6==5){previous=null;decoded=null;}}
  if(encoded>=full*.4)throw new AssertionError("Insufficient reduction");
  byte[][] malformed={{(byte)128,2,0,6},{(byte)128,6,0,5,0,4,0,2}};for(byte[] bad:malformed)try{SnapshotDelta.read(new DataInputStream(new ByteArrayInputStream(bad)),new byte[5]);throw new AssertionError("Accepted invalid delta");}catch(IOException expected){cases++;}
  System.out.println("PASS: "+cases+" delta round trips/validation cases; fixture bytes "+full+" -> "+encoded+" ("+(100-encoded*100/full)+"% less)");
 }
}
