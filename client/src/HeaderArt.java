import javax.microedition.lcdui.Image;
/** Small native-style title sprites, shared by both handset resolutions. */
final class HeaderArt {
    static final int BAG=0, MATCH=1, TEAM=2, DUNGEON=3, FRIENDS=4, SETTINGS=5, CHAT=6, INVITE=7;
    private static final String[] ART={"header-hf11-bag","header-hf11-match","header-hf11-team","header-hf11-dungeon","home-label-9","home-label-3","header-hf11-chat","header-hf11-invite"};
    private static final String[] TEXT={"Túi (B)","Thi đấu","Tổ đội","Ải viễn chinh","Bạn bè","Thiết lập","Trò chuyện","Mời"};
    private static final Image[] cache=new Image[ART.length];
    private static Image image(PhoneCanvas c,int id){if(cache[id]==null)cache[id]=c.combatRoomArt(ART[id]);return cache[id];}
    static void left(PhoneCanvas c,int id,int x,int cy){Image im=image(c,id);if(im!=null)c.img(im,x,cy-im.getHeight()/(2*c.s));else c.text(TEXT[id],x,cy-7);}
    static void center(PhoneCanvas c,int id,int cx,int cy){Image im=image(c,id);if(im!=null)c.img(im,cx-im.getWidth()/(2*c.s),cy-im.getHeight()/(2*c.s));else c.center(TEXT[id],cx,cy-7);}
}
