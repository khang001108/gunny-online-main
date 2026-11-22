"""Sulan text variants composed from shipped game glyphs, never a replacement font.
Boss, bean, flame, staff and death animation remain the native Living121/122/123 atlases.
"""
from pathlib import Path
from PIL import Image,ImageOps
import io,zipfile,struct,hashlib,json,sys
root=Path(__file__).resolve().parents[1]
seed=max((root.parent/'public/downloads').glob('*E72*Online-*.jar'),key=lambda p:int(p.stem.rsplit('-',1)[1]))
work=root.parents[2]
sys.path.insert(0,str(work/'GunnyNokia'))
from audit_weapon_assets import library,bitmap
mask_source=work/'Resource/image/game/living/living121.swf'
definitions,_=library(mask_source)
mask=bitmap(definitions[3]).convert('RGBA')
with zipfile.ZipFile(seed) as z:
 data=z.read('font.dat');count=struct.unpack_from('>H',data)[0]
 glyphs={chr(struct.unpack_from('>H',data,2+i*3)[0]):(i,data[4+i*3]) for i in range(count)}
 proof={}
 for scale in (1,2):
  source=seed if scale==1 else max((root.parent/'public/downloads').glob('*E6*Online-*.jar'),key=lambda p:int(p.stem.rsplit('-',1)[1]))
  with zipfile.ZipFile(source) as pack:
   piece=ImageOps.contain(mask,(18*scale,23*scale),Image.Resampling.LANCZOS)
   tile=Image.new('RGBA',(18*scale,23*scale));tile.alpha_composite(piece,((tile.width-piece.width)//2,0));tile.save(root/f'assets{scale}/tribal-sulan-mask.png',optimize=True)
   raw=pack.read('font-pc-yellow.png');font=Image.open(io.BytesIO(raw)).convert('RGBA')
   text='Thủ lĩnh bộ lạc';advance=sum((glyphs[c][1]*8+5)//11 for c in text)
   line=Image.new('RGBA',((advance+2)*scale,10*scale));x=0
   for c in text:
    i,w=glyphs[c];tile=font.crop((i%16*8*scale,i//16*10*scale,(i%16*8+8)*scale,(i//16*10+10)*scale))
    line.alpha_composite(tile,(x*scale,0));x+=(w*8+5)//11
   title=Image.new('RGBA',(304*scale,34*scale));line=line.resize((min(300,advance*2)*scale,24*scale),Image.Resampling.NEAREST);title.alpha_composite(line,((title.width-line.width)//2,5*scale));title.save(root/f'assets{scale}/tribal-sulan-title.png',optimize=True)
   rawred=pack.read('font-pc-orange.png');red=Image.open(io.BytesIO(rawred)).convert('RGBA');pixels=[]
   for r,g,b,a in red.getdata():pixels.append((255,40,30,a) if a and r>150 and r>g*1.1 else (r,g,b,a))
   red.putdata(pixels);red.save(root/f'assets{scale}/font-sulan-red.png',optimize=True)
   proof[str(scale)]={'glyphs':hashlib.sha256(raw).hexdigest(),'red_palette_source':hashlib.sha256(rawred).hexdigest()}
(root/'source-assets/tribal797/sulan-text.json').write_text(json.dumps({'title':'Thủ lĩnh bộ lạc','source':'Shipped font.dat + font-pc-yellow.png; red speech is the same orange glyph atlas with red palette. Broken-mask fragments use living121.swf bitmap3, no new artwork. All actor and effect artwork is unchanged source SWF.', 'mask_source_sha256':hashlib.sha256(mask_source.read_bytes()).hexdigest(), 'hashes':proof},ensure_ascii=False,indent=2),encoding='utf8')
print('Baked Sulan title and red speech from original game glyphs.')
