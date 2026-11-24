"""One bold title face and palette for all three boss intro banners."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import argparse
p=argparse.ArgumentParser();p.add_argument('--font',type=Path,required=True);a=p.parse_args()
root=Path(__file__).resolve().parents[1]
for scale in (1,2):
 for name,text,w,h in [('npc780-queen-title','Nữ vương kiến',304,34),('tribal-bulan-title','Anh em Bulan',288,35),('tribal-sulan-title','Thủ lĩnh bộ lạc',304,34)]:
  aa=3;factor=aa*scale
  font=ImageFont.truetype(str(a.font),25*factor)
  im=Image.new('RGBA',(w*factor,h*factor));d=ImageDraw.Draw(im)
  l,t,r,b=d.textbbox((0,0),text,font=font,stroke_width=factor)
  d.text(((im.width-r-l)//2,(im.height-b-t)//2),text,font=font,fill='#ffe35a',stroke_width=factor,stroke_fill='#4c290b')
  im.resize((w*scale,h*scale),Image.Resampling.LANCZOS).save(root/f'assets{scale}'/(name+'.png'),optimize=True)
