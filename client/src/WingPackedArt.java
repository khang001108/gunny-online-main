import java.io.DataInputStream;
import java.io.InputStream;
import java.io.IOException;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.Sprite;

/** V4: lossless cropped frames, duplicate tiles shared, physical-pixel anchors.
 * Native atlas is created once in Loading; compact fallback retains all 19 frames.
 * Neither path loads a resource or allocates a pixel buffer in paint. */
final class WingPackedArt {
    static final int FRAMES=19, RECORDS=38, STRIDE=8, OVERHEAD=1408;
    final int width,height,maxPixels;
    final int[] meta=new int[RECORDS*STRIDE];
    byte[] data;
    Image nativeAtlas;
    private WingPackedArt(int w,int h,int max){width=w;height=h;maxPixels=max;}
    static WingPackedArt load(int wing,int angle,int available) throws IOException {
        InputStream raw=WingPackedArt.class.getResourceAsStream("/wingv4-"+wing+"-"+angle+".dat");
        if(raw==null)return null;
        DataInputStream in=new DataInputStream(raw);
        try {
            if(in.readInt()!=0x574E4734)throw new IOException("Bad wing magic");
            int w=in.readUnsignedShort(),h=in.readUnsignedShort(),max=in.readInt(),len=in.readInt();
            if(w<1||w>512||h<1||h>1024||max<1||max>8000||len<1||len>2000000)throw new IOException("Bad wing dimensions");
            if(len+OVERHEAD>available)return null;
            WingPackedArt a=new WingPackedArt(w,h,max);
            for(int i=0;i<RECORDS;i++){
                int p=i*STRIDE;
                int x=in.readUnsignedShort(),y=in.readUnsignedShort(),tw=in.readUnsignedShort(),th=in.readUnsignedShort();
                int rx=in.readShort(),ry=in.readShort(),off=in.readInt(),size=in.readInt();
                if(tw<1||th<1||x+tw>w||y+th>h||tw*th>max||off<0||size<1||off>len-size)throw new IOException("Bad wing tile");
                a.meta[p]=x;a.meta[p+1]=y;a.meta[p+2]=tw;a.meta[p+3]=th;
                a.meta[p+4]=rx;a.meta[p+5]=ry;a.meta[p+6]=off;a.meta[p+7]=size;
            }
            a.data=new byte[len];in.readFully(a.data);return a;
        }finally{in.close();}
    }
    int bytes(){return OVERHEAD+(nativeAtlas!=null?width*height*4:data.length);}
    int nativeBytes(){return width*height*4;}
    /** LZ4 block, no native dependency. Blocks are validated offline during generation. */
    static void decode(byte[] src,int offset,int len,byte[] dst,int expected){
        int p=offset,end=offset+len,q=0;
        while(p<end){
            int token=src[p++]&255,literals=token>>>4;
            if(literals==15){int n;do{n=src[p++]&255;literals+=n;}while(n==255);}
            System.arraycopy(src,p,dst,q,literals);p+=literals;q+=literals;
            if(p==end)break;
            int back=(src[p]&255)|((src[p+1]&255)<<8);p+=2;
            int count=token&15;
            if(count==15){int n;do{n=src[p++]&255;count+=n;}while(n==255);}
            count+=4;
            if(back<=0||back>q||count>expected-q)throw new IllegalArgumentException("Bad wing match");
            for(int i=0;i<count;i++)dst[q+i]=dst[q-back+i];q+=count;
        }
        if(q!=expected||p!=end)throw new IllegalArgumentException("Bad wing block");
    }
    static final class Work {
        final byte[] raw;
        final int[][] pixels;
        final WingPackedArt[] previous=new WingPackedArt[2];
        final int[] record={-1,-1};
        final boolean[] mirror=new boolean[2];
        Work(int scale){int max=scale==1?1974:7636;raw=new byte[max*4];pixels=new int[][]{new int[max],new int[max]};}
        void invalidate(){previous[0]=previous[1]=null;record[0]=record[1]=-1;}
    }
    static int workBytes(int scale){return (scale==1?1974:7636)*12+256;}
    void pixels(int p,boolean flip,int layer,Work work){
        int w=meta[p+2],h=meta[p+3];
        if(work.previous[layer]==this&&work.record[layer]==meta[p+6]&&work.mirror[layer]==flip)return;
        decode(data,meta[p+6],meta[p+7],work.raw,w*h*4);
        int[] dst=work.pixels[layer];byte[] raw=work.raw;int b=0;
        for(int y=0;y<h;y++){
            int x=flip?y*w+w-1:y*w,step=flip?-1:1;
            for(int i=0;i<w;i++,x+=step){dst[x]=((raw[b]&255)<<24)|((raw[b+1]&255)<<16)|((raw[b+2]&255)<<8)|(raw[b+3]&255);b+=4;}
        }
        work.previous[layer]=this;work.record[layer]=meta[p+6];work.mirror[layer]=flip;
    }
    /** Optional acceleration, no loss of frame count if memory is insufficient. */
    void promote(Work work){
        if(nativeAtlas!=null)return;
        int[] atlas=new int[width*height];
        for(int i=0;i<RECORDS;i++){
            int p=i*STRIDE;boolean duplicate=false;
            for(int j=0;j<i;j++)if(meta[j*STRIDE+6]==meta[p+6]){duplicate=true;break;}
            if(duplicate)continue;
            pixels(p,false,0,work);int w=meta[p+2],h=meta[p+3],x=meta[p],y=meta[p+1];
            for(int row=0;row<h;row++)System.arraycopy(work.pixels[0],row*w,atlas,(y+row)*width+x,w);
        }
        Image image=Image.createRGBImage(atlas,width,height,true);
        if(image!=null){nativeAtlas=image;data=null;}
        work.invalidate();
    }
    void draw(Graphics g,int frame,int layer,boolean flip,int anchorX,int anchorY,Work work){
        int p=(layer*FRAMES+frame)*STRIDE;
        int w=meta[p+2],h=meta[p+3];
        int x=anchorX+(flip?-meta[p+4]-w:meta[p+4]),y=anchorY+meta[p+5];
        if(nativeAtlas!=null)g.drawRegion(nativeAtlas,meta[p],meta[p+1],w,h,flip?Sprite.TRANS_MIRROR:Sprite.TRANS_NONE,x,y,Graphics.TOP|Graphics.LEFT);
        else{pixels(p,flip,layer,work);g.drawRGB(work.pixels[layer],0,w,x,y,w,h,true);}
    }
}
