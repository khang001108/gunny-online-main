from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
for scale in (1,2):
 out=ROOT/f'assets{scale}'
 title=Image.new('RGBA',(288*scale,35*scale))
 font=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf',25*scale)
 ImageDraw.Draw(title).text((3*scale,0),'Anh em Bulan',font=font,fill='#ffe35a',stroke_width=scale,stroke_fill='#7d4206')
 title.save(out/'tribal-bulan-title.png',optimize=True)
 im=Image.new('RGBA',(24*scale,64*scale));d=ImageDraw.Draw(im)
 def polygon(points,fill):d.polygon([(x*scale,y*scale) for x,y in points],fill=fill)
 polygon([(12,0),(16,9),(15,13),(19,16),(16,19),(19,24),(16,27),(19,32),(16,35),(19,40),(15,44),(15,51),(20,53),(19,57),(14,56),(14,63),(9,63),(9,56),(4,57),(3,53),(9,51),(9,44),(5,40),(8,35),(5,32),(8,27),(5,24),(8,19),(5,16),(9,13),(8,9)],'#4e351d')
 polygon([(12,3),(14,10),(13,15),(16,17),(13,20),(16,24),(13,28),(16,32),(13,36),(16,40),(12,46),(9,40),(7,38),(10,35),(7,31),(10,27),(7,23),(10,19),(7,16),(11,14),(10,10)],'#e6b951')
 polygon([(12,5),(12,43),(10,37),(11,15)],'#fff2a5')
 polygon([(5,54),(19,54),(18,55),(13,54),(13,61),(10,61),(10,54),(5,55)],'#bb8329')
 im.save(out/'tribal-bulan-sword.png',optimize=True)
