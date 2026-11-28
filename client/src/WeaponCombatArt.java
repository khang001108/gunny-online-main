import java.io.*;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.Sprite;

/** Native Common/Special art, prepacked and decoded only during Loading. */
final class WeaponCombatArt {
    static final int PROJECTILE=0,TRAIL=2,HELD=4,IMPACT=6;
    final PhoneCanvas c;
    final Pack[] weapons=new Pack[10];
    int loads;
    WeaponCombatArt(PhoneCanvas canvas){c=canvas;}
    static final class Pack {
        Image atlas;
        final short[][] frames=new short[8][];
        short[] regions;
    }
    void prepare(int mask)throws IOException {
        // Decode participant weapons while Loading; weapon switches are prepared before applying input/snapshots.
        for(int weapon=0;weapon<10;weapon++)if((mask&(1<<weapon))!=0&&Battle.nativeWeapon(weapon)&&weapons[weapon]==null){
            InputStream stream=getClass().getResourceAsStream("/weapon-combat-"+weapon+".dat");
            if(stream==null)throw new IOException("Thiếu ảnh vũ khí "+weapon);
            DataInputStream in=new DataInputStream(stream);
            try{
                if(in.readInt()!=0x47574331||in.readUnsignedByte()!=8)throw new IOException("Sai ảnh vũ khí");
                Pack p=new Pack();
                for(int section=0;section<8;section++){
                    int count=in.readUnsignedShort();
                    if(count<1||count>256)throw new IOException("Sai frame vũ khí");
                    p.frames[section]=new short[count];
                    for(int i=0;i<count;i++)p.frames[section][i]=in.readShort();
                }
                int count=in.readUnsignedShort();
                if(count<1||count>512)throw new IOException("Sai atlas vũ khí");
                p.regions=new short[count*6];
                for(int i=0;i<p.regions.length;i++)p.regions[i]=in.readShort();
                p.atlas=Image.createImage(in);weapons[weapon]=p;loads++;
            }finally{in.close();}
        }
    }
    void release(){for(int i=0;i<weapons.length;i++)weapons[i]=null;}
    boolean ready(int weapon){return weapon>=0&&weapon<10&&weapons[weapon]!=null;}
    boolean draw(int weapon,boolean powered,int section,int frame,int x,int y,int transform){
        if(!ready(weapon))return false;
        Pack p=weapons[weapon];short[] frames=p.frames[section+(powered?1:0)];
        int n=(frames[frame%frames.length]&65535)*6;
        int w=p.regions[n+2],h=p.regions[n+3];
        if(w==0||h==0)return true;
        int dx=p.regions[n+4],dy=p.regions[n+5];
        if(transform==Sprite.TRANS_MIRROR||transform==Sprite.TRANS_ROT180)dx=-dx-w;
        if(transform==Sprite.TRANS_MIRROR_ROT180||transform==Sprite.TRANS_ROT180)dy=-dy-h;
        c.g.drawRegion(p.atlas,p.regions[n]*c.s,p.regions[n+1]*c.s,w*c.s,h*c.s,transform,
            c.ox+(x+dx)*c.s,c.oy+(y+dy)*c.s,Graphics.TOP|Graphics.LEFT);
        return true;
    }
    boolean projectile(Battle.Ball b,int x,int y){
        int rows=ready(b.weapon)?weapons[b.weapon].frames[b.powered?1:0].length/16:1;
        int frame=Battle.visualFrame(b)+(b.age/2%rows)*16;
        return draw(b.weapon,b.powered,PROJECTILE,frame,x,y,Sprite.TRANS_NONE);
    }
    boolean trail(int weapon,boolean powered,int frame,int age,int life,int x,int y){
        int fade=Math.min(2,age*3/Math.max(1,life));
        return draw(weapon,powered,TRAIL,fade*16+frame,x,y,Sprite.TRANS_NONE);
    }
    boolean impact(Battle.Blast b,int x,int y){
        return draw(b.weapon,b.powered,IMPACT,Math.min(24,b.time),x,y,Sprite.TRANS_NONE);
    }
}
