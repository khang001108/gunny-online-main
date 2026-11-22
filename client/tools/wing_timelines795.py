"""Bake the original SWF wing timelines; no fabricated flap poses.
Requires Pillow and the existing audit_weapon_assets.py source-pack decoder.
"""
from pathlib import Path
import sys,struct,copy,math
from PIL import Image
sys.path.insert(0,'D:/gunny/GunnyNokia')
from audit_weapon_assets import library,tags,bitmap
ROOT=Path(__file__).resolve().parents[1]
NAMES=['wing027','wing006','wing007','wing005','wing030','wing002']
class Bits:
 def __init__(self,data,pos=0):self.d=data;self.p=pos*8
 def u(self,n):
  v=0
  for _ in range(n):v=v*2+((self.d[self.p//8]>>(7-self.p%8))&1);self.p+=1
  return v
 def s(self,n):
  v=self.u(n);return v-(1<<n) if n and v&(1<<(n-1)) else v
 def align(self):self.p=(self.p+7)//8*8;return self.p//8
 def matrix(self):
  a=d=1;b=c=0
  if self.u(1):n=self.u(5);a=self.s(n)/65536;d=self.s(n)/65536
  if self.u(1):n=self.u(5);b=self.s(n)/65536;c=self.s(n)/65536
  n=self.u(5);tx=self.s(n)/20;ty=self.s(n)/20;self.align();return a,b,c,d,tx,ty
I=(1,0,0,1,0,0)
def mul(m,n):
 a,b,c,d,x,y=m;e,f,g,h,u,v=n
 return a*e+c*f,b*e+d*f,a*g+c*h,b*g+d*h,a*u+c*v+x,b*u+d*v+y
class Movie:
 def __init__(self,path):self.defs,self.symbols=library(path);self.cache={};self.images={}
 def states(self,k):
  if k in self.cache:return self.cache[k]
  kind,b=self.defs[k];objects={};frames=[]
  for tag,data in tags(b[4:]):
   if tag in (26,70):
    flags=data[0];pos=3 if tag==26 else 4;depth=struct.unpack_from('<H',data,1 if tag==26 else 2)[0]
    obj=objects.get(depth,(None,I,0));char,mat,start=obj
    if flags&2:char=struct.unpack_from('<H',data,pos)[0];pos+=2;start=len(frames)
    if flags&4:bits=Bits(data,pos);mat=bits.matrix()
    objects[depth]=(char,mat,start)
   elif tag==28:objects.pop(struct.unpack_from('<H',data)[0],None)
   elif tag==5:objects.pop(struct.unpack_from('<H',data,2)[0],None)
   elif tag==1:frames.append(dict(objects))
  assert frames
  self.cache[k]=frames;return frames
 def shapes(self,k):
  kind,b=self.defs[k];bits=Bits(b,2);n=bits.u(5)
  for _ in range(4):bits.s(n)
  pos=bits.align();count=b[pos];pos+=1;fills=[]
  for _ in range(count):
   style=b[pos];pos+=1
   if style in (0x40,0x41,0x42,0x43):
    char=struct.unpack_from('<H',b,pos)[0];pos+=2;bits=Bits(b,pos);a,bb,c,d,x,y=bits.matrix();pos=bits.align()
    if char in self.defs:fills.append((char,(a/20,bb/20,c/20,d/20,x,y)))
   elif style==0:pos+=4 if kind in (32,83) else 3
   else:raise ValueError(('unsupported source fill',style))
  assert len(fills)==1,(k,fills)
  return fills
 def flat(self,k,t=0,m=I,pose=None):
  kind,b=self.defs[k]
  if kind in (20,21,35,36):
   if k not in self.images:self.images[k]=bitmap(self.defs[k])
   return [(self.images[k],m)]
  if kind in (2,22,32,83):
   out=[]
   for child,mat in self.shapes(k):out+=self.flat(child,t,mul(m,mat),pose)
   return out
  if kind==39:
   frames=self.states(k);frame=frames[(pose if pose is not None and len(frames)==2 else t)%len(frames)];out=[]
   for depth,(child,mat,start) in sorted(frame.items()):out+=self.flat(child,max(0,t-start),mul(m,mat),pose)
   return out
  raise ValueError(kind)
def render(parts,size,m):
 canvas=Image.new('RGBA',size)
 for image,mat in parts:
  a,b,c,d,x,y=mul(m,mat);det=a*d-b*c
  inverse=(d/det,-c/det,(c*y-d*x)/det,-b/det,a/det,(b*x-a*y)/det)
  canvas.alpha_composite(image.transform(size,Image.Transform.AFFINE,inverse,Image.Resampling.BICUBIC))
 return canvas
if __name__=='__main__':
 for scale in [1,2]:
  out=ROOT/f'assets{scale}';out.mkdir(exist_ok=True)
  for wing,name in enumerate(NAMES):
   movie=Movie(Path('D:/gunny/Resource/image/equip/wing')/name/'wings.swf');show=next(v for k,v in movie.symbols.items() if k.startswith('wing_show_'));game=next(v for k,v in movie.symbols.items() if k.startswith('wing_game_'))
   for size,height in [(112,72),(64,42),(32,22)]:
    atlas=Image.new('RGBA',(size*scale*20,height*scale));factor=size/180*scale
    m=(factor,0,0,factor,(size/2-50*size/180)*scale,(height*.42-111*size/180)*scale)
    for frame in range(20):atlas.paste(render(movie.flat(show,frame),(size*scale,height*scale),m),(frame*size*scale,0))
    atlas.save(out/f'wing796-show-{wing}-{size}.png',optimize=True)
    atlas.crop((0,0,size*scale,height*scale)).save(out/f'wing796-static-{wing}-{size}.png',optimize=True)
   children=movie.states(game)[0]
   assert len(children)==2
   for angle in range(5):
    rad=math.radians(angle*15-30);factor=.60*scale;a=math.cos(rad)*factor;b=math.sin(rad)*factor
    m=(a,b,-b,a,48*scale-a*48+b*60,48*scale-b*48-a*60)
    for layer,(depth,(child,mat,start)) in enumerate(sorted(children.items())):
     atlas=Image.new('RGBA',(96*scale*19,96*scale))
     for frame in range(19):atlas.paste(render(movie.flat(child,frame,mat,pose=0),(96*scale,96*scale),m),(frame*96*scale,0))
     atlas.save(out/f'wing795-game-{wing}-{angle}-{layer}.png',optimize=True)
 print('Baked native wing_show 20-frame and wing_game 19-frame timelines, both original layers, 5 slopes.')
