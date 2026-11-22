from pathlib import Path
import sys,math
from PIL import Image
sys.path.insert(0,'client/tools')
from wing_timelines795 import Movie,render,NAMES
root=Path('client')
for scale in (1,2):
 for wing,name in enumerate(NAMES):
  movie=Movie(Path('D:/gunny/Resource/image/equip/wing')/name/'wings.swf');game=next(v for k,v in movie.symbols.items() if k.startswith('wing_game'))
  children=movie.states(game)[0]
  for angle in range(5):
   rad=math.radians(angle*15-30);factor=.60*scale;a=math.cos(rad)*factor;b=math.sin(rad)*factor
   for layer,(depth,(child,mat,start)) in enumerate(sorted(children.items())):
    atlas=Image.new('RGBA',(96*scale*19,96*scale))
    for frame in range(19):
     parts=movie.flat(child,frame,mat,pose=0);im,transform=parts[0]
     roots=(((48,55),(42,52)),((60,48),(59,48)),((60,48),(59,48)),((77,62),(77,66)),((35,43),(59,49)),((77,62),(77,66)))
     px,py=roots[wing][layer]
     aa,bb,cc,dd,tx,ty=transform;anchorX=aa*px+cc*py+tx;anchorY=bb*px+dd*py+ty
     matrix=(a,b,-b,a,48*scale-a*anchorX+b*anchorY,48*scale-b*anchorX-a*anchorY)
     atlas.paste(render(parts,(96*scale,96*scale),matrix),(frame*96*scale,0))
    atlas.save(root/f'assets{scale}'/f'wing795-game-{wing}-{angle}-{layer}.png',optimize=True)
print('Rebaked battle wings around opaque roots')
