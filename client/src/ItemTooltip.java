import javax.microedition.lcdui.Image;
/** PC goods-tip asset, placed beside the selected item without covering it. */
final class ItemTooltip {
    void releaseArt(){source=panel=purple=orange=green=null;for(int i=0;i<strength.length;i++)strength[i]=null;}

 final PhoneCanvas c;Image source,panel,purple,orange,green;final Image[] strength=new Image[16];int width,height;int[] lastBox=new int[4];
 ItemTooltip(PhoneCanvas owner){c=owner;}
 int[] bounds(int[] anchor,int kind){int right=316-(anchor[0]+anchor[2]+4),left=anchor[0]-8;boolean after=right>=138;int w=Math.min(180,after?right:left),h=kind==0?150:134;w=Math.max(128,w);int x=after?anchor[0]+anchor[2]+4:anchor[0]-4-w;return new int[]{Battle.clamp(x,4,316-w),Battle.clamp(anchor[1],38,226-h),w,h};}
 void colored(String text,int x,int y,Image font){for(int n=0;n<text.length();n++){int id=c.glyph(text.charAt(n));c.part(font,id%16*8,id/16*10,8,10,x,y,false);x+=(c.widths[id]*8+5)/11;}}
 void draw(int kind,int item,int[] anchor){draw(kind,item,anchor,c.inventory.owns(kind,item)?"Đã sở hữu":"Chưa sở hữu");}
 void stat(String label,int value,int bonus,int x,int y){String base=label+": "+value;c.pcText(base,x,y);if(bonus>0)colored("(+"+bonus+")",x+c.textWidth(base)+3,y,green);}
 void draw(int kind,int item,int[] anchor,String footer){draw(kind,item,anchor,footer,c.gender,c.economy.ranks()[ForgeRules.index(c.gender,kind,item)]);}
 void draw(int kind,int item,int[] anchor,String footer,int sex,int rank){if(source==null){source=c.combatRoomArt("item752-tooltip");purple=c.combatRoomArt("font-pc-purple");orange=c.combatRoomArt("font-pc-orange");green=c.combatRoomArt("font-pc-green");}int[] b=bounds(anchor,kind);lastBox=b;if(panel==null||width!=b[2]||height!=b[3]){width=b[2];height=b[3];panel=NativeUi.fit(source,width*c.s,height*c.s,5*c.s);}int x=b[0]+8,y=b[1]+8;c.img(panel,b[0],b[1]);colored(c.pcShort(c.bag.itemName(kind,item),width-16),x,y,purple);c.pcText("Loại hình: "+(kind==0?"Vũ khí":PhoneCanvas.BAG_TABS[kind]),x,y+17);int stats=y+35;if(kind==0){c.pcText("Góc: "+Battle.weaponMinAngle(item)+"° ~ "+Battle.weaponMaxAngle(item)+"°",x,y+31);stats+=15;}c.line(x,stats-5,b[0]+width-9,stats-5,0x73716c);int bonus=rank*(kind==2||kind==3||kind==6?13:5);stat("Công",ItemRules.attack(kind,item),kind==0||kind==1?bonus:0,x,stats);stat("Thủ",ItemRules.defense(kind,item),kind>0?bonus:0,x,stats+15);stat("May",ItemRules.luck(kind,item),kind==1?bonus:0,x,stats+30);if(rank>0){if(strength[rank]==null)strength[rank]=c.combatRoomArt("strength780-"+rank);c.img(strength[rank],b[0]+width-58,stats+38);}colored(footer,x,stats+64,footer.equals("Chưa sở hữu")?orange:green);}
}
