import javax.microedition.lcdui.Image;
/** Native artwork, shared bounds and text alignment for ordinary menu buttons. */
final class NativeUi {
    void releaseArt(){source=null;for(int i=0;i<buttons.length;i++)buttons[i]=null;}

 final PhoneCanvas c;Image source;final Image[] buttons=new Image[8];final int[] widths=new int[8],heights=new int[8];int next;
 NativeUi(PhoneCanvas canvas){c=canvas;}
 static Image fit(Image image,int w,int h,int edge){int sw=image.getWidth(),sh=image.getHeight(),b=Math.min(edge,Math.min(Math.min(sw,sh),Math.min(w,h))/2-1);int[] src=new int[sw*sh],dst=new int[w*h];image.getRGB(src,0,sw,0,0,sw,sh);for(int y=0;y<h;y++){int sy=y<b?y:y>=h-b?sh-h+y:b+(y-b)*(sh-2*b)/(h-2*b);for(int x=0;x<w;x++){int sx=x<b?x:x>=w-b?sw-w+x:b+(x-b)*(sw-2*b)/(w-2*b);dst[y*w+x]=src[sy*sw+sx];}}return Image.createRGBImage(dst,w,h,true);}
 Image button(int w,int h){if(source==null)source=c.combatRoomArt("ui752-button");for(int i=0;i<buttons.length;i++)if(buttons[i]!=null&&widths[i]==w&&heights[i]==h)return buttons[i];int id=next++%buttons.length;widths[id]=w;heights[id]=h;return buttons[id]=fit(source,w*c.s,h*c.s,5*c.s);}
 void button(String label,int x,int y,int w,int h,boolean selected){c.img(button(w,h),x,y);c.pcCenter(c.pcShort(label,w-6),x+w/2,y+(h-10)/2);if(selected)c.drawSelectionGlowPhase(x,y,w,h,0);}
}
