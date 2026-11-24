import java.io.*;
/** Bounded delta decoder for CLDC, no ZIP/inflater dependency. */
final class SnapshotDelta {
 static byte[] read(DataInputStream in,byte[] previous)throws IOException{
  return decode(in,previous,false);
 }
 // Caller owns the buffer and has validated the segment. Never use for retained snapshots.
 static byte[] readReusable(DataInputStream in,byte[] previous)throws IOException{
  return decode(in,previous,true);
 }
 private static byte[] decode(DataInputStream in,byte[] previous,boolean reuse)throws IOException{
  int header=in.readUnsignedShort(),length=header&32767;
  if(length==0||length>in.available())throw new IOException("Khung hình không đầy đủ");
  if((header&32768)==0){byte[] full=reuse&&previous!=null&&previous.length==length?previous:new byte[length];in.readFully(full);return full;}
  if(length<2)throw new IOException("Invalid delta length");int end=in.available()-length,size=in.readUnsignedShort();
  if(previous==null||previous.length!=size||size>32767)throw new IOException("Thiếu khung hình gốc");
  byte[] result=reuse?previous:new byte[size];if(!reuse)System.arraycopy(previous,0,result,0,size);int last=0;
  while(in.available()>end){if(in.available()-end<4)throw new IOException("Sai dữ liệu thay đổi");int start=in.readUnsignedShort(),count=in.readUnsignedShort();if(start<last||count==0||start+count>size||count>in.available()-end)throw new IOException("Sai vùng thay đổi");in.readFully(result,start,count);last=start+count;}
  return result;
 }
 static int validate(DataInputStream in,int previousSize)throws IOException{
  int header=in.readUnsignedShort(),length=header&32767;
  if(length==0||length>in.available())throw new IOException("Khung hình không đầy đủ");
  if((header&32768)==0){in.skipBytes(length);return length;}
  int end=in.available()-length;if(length<2)throw new IOException("Sai dữ liệu thay đổi");int size=in.readUnsignedShort(),last=0;
  if(previousSize!=size)throw new IOException("Thiếu khung hình gốc");
  while(in.available()>end){if(in.available()-end<4)throw new IOException("Sai dữ liệu thay đổi");int start=in.readUnsignedShort(),count=in.readUnsignedShort();if(start<last||count==0||start+count>size||count>in.available()-end)throw new IOException("Sai vùng thay đổi");in.skipBytes(count);last=start+count;}
  return size;
 }
}
