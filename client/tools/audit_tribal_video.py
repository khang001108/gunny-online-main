from pathlib import Path
import sys, json, struct
from PIL import Image, ImageDraw, ImageFont
ROOT=Path('D:/gunny/repo/gunny-online-main'); OUT=ROOT/'client/build/tribal-review'
sys.path.insert(0,'D:/gunny/GunnyNokia')
from audit_weapon_assets import library,bitmap,tags
font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',16)
# Correct Unicode lexical ordering: vang sorts before do. Use explicit identities in the report.
keys={'stage1':'stage1','stage2':'stage2','gold':'gold-boss','red':'red-boss'}
events=[]
for key,folder in keys.items():
    for path in sorted((OUT/folder).glob('*.jpg')):
        im=Image.open(path).convert('RGB');small=im.resize((416,192));pix=small.load();seen=set();found=[]
        for y in range(20,145):
            for x in range(15,405):
                if (x,y) in seen:continue
                r,g,b=pix[x,y]
                if not (r>208 and g>159 and b<118):continue
                stack=[(x,y)];seen.add((x,y));pts=[]
                while stack:
                    a,c=stack.pop();pts.append((a,c))
                    for q in ((a-1,c),(a+1,c),(a,c-1),(a,c+1)):
                        xx,yy=q
                        if 0<=xx<416 and 20<=yy<146 and q not in seen:
                            rr,gg,bb=pix[xx,yy]
                            if rr>208 and gg>159 and bb<118:seen.add(q);stack.append(q)
                if len(pts)<170:continue
                l=min(p[0] for p in pts);r=max(p[0] for p in pts);t=min(p[1] for p in pts);b=max(p[1] for p in pts)
                if r-l>45 and b-t>12:found.append((l,t,r,b))
        for box in found:
            l,t,r,b=box;crop=im.crop((max(0,l*3-8),max(0,t*3-8),min(1248,r*3+15),min(576,b*3+15)))
            events.append((key,int(path.stem)-1,crop))
for page in range(0,len(events),18):
    canvas=Image.new('RGB',(1200,6*185),(24,28,34));draw=ImageDraw.Draw(canvas)
    for j,(key,sec,crop) in enumerate(events[page:page+18]):
        crop.thumbnail((390,155));x=j%3*400;y=j//3*185
        draw.text((x+4,y+3),key+' '+str(sec)+'s',font=font,fill='white');canvas.paste(crop,(x+4,y+24))
    canvas.save(OUT/f'speech-{page//18}.jpg')
print('Speech candidates:',len(events))
# Full-body art candidates and exported timeline/label information.
entries=[];thumbs=[]
for p in sorted(Path('D:/gunny/Resource/image/game/living').glob('*.swf')):
    import re
    number=int(re.search(r'(\d+)\.swf$',p.name).group(1))
    if not 115<=number<=132:continue
    defs,symbols=library(p);labels=[]
    for char,(kind,body) in defs.items():
        if kind==39:
            for tag,data in tags(body[4:]):
                if tag==43:labels.append([char,data.rstrip(b'\0').decode('utf8','replace')])
    entries.append(dict(file=p.name,symbols=symbols,labels=labels))
    candidates=[]
    for char,definition in defs.items():
        if definition[0] not in (20,21,35,36):continue
        try:im=bitmap(definition)
        except:continue
        if im is None:continue
        if im.width>55 and im.height>55:
            im=im.crop(im.getbbox());candidates.append((im.width*im.height,char,im))
    for _,char,im in sorted(candidates,reverse=True)[:6]:thumbs.append((p.name+':'+str(char),im))
for page in range(0,len(thumbs),36):
    canvas=Image.new('RGB',(1200,6*175),(112,126,139));d=ImageDraw.Draw(canvas)
    for j,(label,im) in enumerate(thumbs[page:page+36]):
        im.thumbnail((190,145));x=j%6*200;y=j//6*175
        d.text((x+3,y+3),label,font=font,fill='white');canvas.paste(im,(x+(200-im.width)//2,y+24),im)
    canvas.save(OUT/f'living-native-{page//36}.jpg')
(OUT/'living-symbols.json').write_text(json.dumps(entries,indent=2,ensure_ascii=False),encoding='utf8')
# Native map contact: confirm actual geometry, don't fabricate ground below it.
canvas=Image.new('RGB',(1200,4*230),(44,52,65));d=ImageDraw.Draw(canvas)
for row,ident in enumerate([1124,1125,1126,1122]):
    folder=Path('D:/gunny/Resource/image/map')/str(ident)
    back=Image.open(folder/'back.jpg').convert('RGB');land=Image.open(folder/'dead.png').convert('RGBA')
    d.text((0,row*230),str(ident)+' back '+str(back.size)+' dead '+str(land.size),font=font,fill='white')
    back.thumbnail((600,200));land.thumbnail((600,200));canvas.paste(back,(0,row*230+25));canvas.paste(land,(600,row*230+25),land)
canvas.save(OUT/'maps-native.jpg')
