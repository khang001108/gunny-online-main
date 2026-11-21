import java.io.*;
/** Chunk-local byte ranges; the first frame remains independently decodable. */
final class FrameDelta {
 static byte[] encode(byte[] previous,byte[] current)throws IOException{
  if(previous==null||previous.length!=current.length)return null;
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeShort(current.length);
  for(int i=0;i<current.length;){if(previous[i]==current[i]){i++;continue;}int start=i++;while(i<current.length&&previous[i]!=current[i])i++;out.writeShort(start);out.writeShort(i-start);out.write(current,start,i-start);}
  return bytes.size()+2<current.length?bytes.toByteArray():null;
 }
}
