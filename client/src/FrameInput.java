import java.io.InputStream;
/** Rebindable, non-owning reader for decoded CLDC snapshot bytes. */
final class FrameInput extends InputStream {
    private byte[] data;private int position;
    void bind(byte[] value){data=value;position=0;}
    public int available(){return data==null?0:data.length-position;}
    public int read(){return available()==0?-1:data[position++]&255;}
    public int read(byte[] dst,int offset,int length){
        if(dst==null)throw new NullPointerException();
        if(offset<0||length<0||offset>dst.length-length)throw new IndexOutOfBoundsException();
        if(length==0)return 0;int count=Math.min(length,available());if(count==0)return -1;
        System.arraycopy(data,position,dst,offset,count);position+=count;return count;
    }
    public long skip(long n){int count=(int)Math.min(Math.max(0L,n),available());position+=count;return count;}
}
