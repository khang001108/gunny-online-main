"""Bake supplied PC timelines and video-verified three-stage Tà Thần assets.
Video crops are loading cards only; no screenshot is used as playable terrain.
Requires Pillow, the source pack and JPEXS 26.3 from its official release.
"""
from pathlib import Path
import argparse,sys,struct,subprocess,json,hashlib
from PIL import Image,ImageOps,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1];WORK=ROOT.parents[2]
sys.path.insert(0,str(WORK/'GunnyNokia'))
from audit_weapon_assets import library,tags,bitmap
MAPS={8:1122,9:1124,10:1124,11:1126}
NPC={4:115,5:115,6:116,7:123,8:117,9:117,10:125,11:127,12:124,13:126,14:121,15:122}
ROWS=('stand','walk','attack','cast','special','hit','die','born')
LABELS={4:('stand','walk','beatA','born','beatA','cry','die','born'),6:('stand','walk','shoot','born','beatB','cry','die','born'),7:('stand','stand','renew','grow','grow','cry','die','born'),8:('stand','walk','beatA','call','castA','cry','die','out'),10:('stand','stand','cry','born','cry','cry','die','born'),11:('stand','stand','cry','born','cry','cry','die','born'),12:('stand','stand','stand','born','stand','stand','die','born'),13:('stand','stand','beatA','born','beatA','cry','die','born'),14:('stand','walk','beatA','callA','callB','cry','die','stand'),15:('stand','walk','beatB','beatC','beatA','cry','die','born')}
SOURCE=ROOT/'source-assets/tribal797';RAW=ROOT/'build/tribal-review/native';SOURCE.mkdir(parents=True,exist_ok=True);RAW.mkdir(parents=True,exist_ok=True)

def png(im,out,scale):
    if scale==1 and im.mode=='RGBA':im=im.quantize(colors=128,method=Image.Quantize.FASTOCTREE)
    im.save(out,optimize=True)
def ranges(path):
    defs,symbols=library(path);root=next(v for k,v in symbols.items() if k.startswith('game.living.'));frame=0;labels={}
    for tag,data in tags(defs[root][1][4:]):
        if tag==43:labels[data.rstrip(b'\0').decode()]=frame
        if tag==1:frame+=1
    starts=sorted(set(labels.values())|{frame})
    return root,{k:(v,starts[starts.index(v)+1]) for k,v in labels.items()}
def samples(span):
    a,b=span;return [a+round((b-a-1)*i/7)+1 for i in range(8)]
def extract(living,labels,ffdec):
    swf=next((WORK/'Resource/image/game/living').glob('*'+str(living)+'.swf'));root,spans=ranges(swf)
    chosen=[k if k in spans else 'stand' for k in labels];frames=[samples(spans[k]) for k in chosen];wanted=sorted(set(sum(frames,[])))
    folder=RAW/str(living);dest=folder/f'DefineSprite_{root}_game.living.Living{living}'
    if not all((dest/f'{n}.png').exists() for n in wanted):
        log=subprocess.run(['java','-Djava.awt.headless=true','-jar',str(ffdec),'-selectid',str(root),'-select',str(root)+':'+','.join(map(str,wanted)),'-export','sprite',str(folder),str(swf)],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,check=True)
        (RAW/f'{living}.log').write_bytes(log.stdout)
    return swf,chosen,frames,dest
entries=[];atlases={}
parser=argparse.ArgumentParser();parser.add_argument('--ffdec',type=Path,default=WORK/'GunnyNokia/tools/ffdec/ffdec.jar');args=parser.parse_args()
for kind,living in NPC.items():
    alias={5:4,9:8}.get(kind,kind)
    if alias in atlases:atlases[kind]=atlases[alias];continue
    labels=LABELS[alias];swf,chosen,frames,folder=extract(living,labels,args.ffdec)
    stand=Image.open(folder/f'{frames[0][0]}.png').convert('RGBA');bbox=stand.getchannel('A').point(lambda v:255 if v>128 else 0).getbbox()
    target=62 if kind>=14 else 36 if kind in (8,9) else 30 if kind in (10,11,12,13) else 29 if kind==7 else 31
    cell=96 if kind>=14 else 72 if kind in (8,9) else 56
    factor=target/(bbox[3]-bbox[1]);anchor=((bbox[0]+bbox[2])/2,bbox[3])
    atlases[kind]=(cell,folder,frames,factor,anchor)
    entries.append(dict(kind=kind,living=living,source=str(swf.relative_to(WORK)),sha256=hashlib.sha256(swf.read_bytes()).hexdigest(),rows=dict(zip(ROWS,chosen)),frames=frames,registration=anchor,source_scale=factor,cell=cell))
for scale in (1,2):
    out=ROOT/f'assets{scale}';out.mkdir(exist_ok=True)
    if (SOURCE/'loading-backdrop.png').exists():Image.open(SOURCE/'loading-backdrop.png').resize((320*scale,240*scale),Image.Resampling.LANCZOS).save(out/'tribal797-loading-bg.png',optimize=True)
    for kind,(cell,folder,frames,factor,anchor) in atlases.items():
        atlas=Image.new('RGBA',(cell*8*scale,cell*8*scale))
        for row,seq in enumerate(frames):
            for column,n in enumerate(seq):
                im=Image.open(folder/f'{n}.png').convert('RGBA');f=factor*scale
                tile=im.transform((cell*scale,cell*scale),Image.Transform.AFFINE,(1/f,0,anchor[0]-cell*scale/(2*f),0,1/f,anchor[1]-(cell-5)*scale/f),Image.Resampling.BICUBIC)
                atlas.paste(tile,(column*cell*scale,row*cell*scale))
        png(atlas,out/f'tribal797-npc-{kind}.png',scale)
        face=Image.open(folder/f'{frames[0][0]}.png').convert('RGBA');face=face.crop(face.getchannel('A').point(lambda v:255 if v>96 else 0).getbbox())
        face=ImageOps.contain(face,(44*scale,49*scale),Image.Resampling.LANCZOS);portrait=Image.new('RGBA',(48*scale,50*scale));portrait.alpha_composite(face,((48*scale-face.width)//2,50*scale-face.height));png(portrait,out/f'tribal797-portrait-{kind}.png',scale)
        if scale==1:atlas.crop((0,0,cell,cell)).save(SOURCE/f'npc-{kind}-stand.png')
    for ident,source in MAPS.items():
        src=WORK/'Resource/image/map'/str(source)
        land=Image.open(src/'dead.png').convert('RGBA').resize((480,264),Image.Resampling.LANCZOS)
        alpha=land.getchannel('A').tobytes();bits=bytearray(15840)
        for n,a in enumerate(alpha):
            if a>96:bits[n>>3]|=1<<(n&7)
        if scale==1:
            (ROOT.parent/'backend/assets'/f'collision{ident}.dat').write_bytes(bits)
            (ROOT.parent/'backend/assets'/f'hard{ident}.dat').write_bytes(bytes([255])*15840)
            land.save(ROOT.parent/'backend/assets'/f'land{ident}.png')
        (out/f'collision{ident}.dat').write_bytes(bits);(out/f'hard{ident}.dat').write_bytes(bytes([255])*15840)
        land.resize((480*scale,264*scale),Image.Resampling.NEAREST).save(out/f'land{ident}.png',optimize=True)
        sky=Image.open(src/'back.jpg').convert('RGB');sky.resize((320*scale,240*scale),Image.Resampling.LANCZOS).save(out/f'bg{ident}.png')
        preview=sky.resize((480,264));preview.paste(land,(0,0),land)
        for name,size in [(f'thumb{ident}',(88,44)),(f'room-map{ident}',(77,30))]:preview.resize((size[0]*scale,size[1]*scale),Image.Resampling.LANCZOS).save(out/(name+'.png'))
    original=WORK/'Resource/image/map/3'
    Image.open(original/'samll_map.png').convert('RGBA').resize((88*scale,32*scale),Image.Resampling.LANCZOS).save(out/'tribal797-banner.png')
    Image.open(original/'show1.jpg').resize((151*scale,187*scale),Image.Resampling.LANCZOS).save(out/'tribal-picker-cover.png')
    for stage,key in [(1,'stage1'),(2,'stage2'),(3,'gold-boss')]:
        # The golden-form clip includes the stage-three loading card.
        frame=Image.open(ROOT/'build/tribal-review'/key/'00003.jpg')
        card=frame.crop((746,150,929,378))
        card.resize((80*scale,100*scale),Image.Resampling.LANCZOS).save(out/f'tribal797-cover-{stage}.png')
        if scale==1:card.save(SOURCE/f'loading-card-{stage}.png')
    # Actual thrown objects, never a whole basket or the boss's weapon.
    defs116,_=library(WORK/'Resource/image/game/living/Living116.swf');pike=bitmap(defs116[58]).rotate(-90,expand=True);pike=pike.crop(pike.getbbox())
    # Native bone is bitmap 13 from Bulan, selected below from the extracted sheet.
    defs,_=library(WORK/'Resource/image/game/living/Living117.swf')
    images=[]
    for char,data in defs.items():
        if data[0] in (20,21,35,36):
            im=bitmap(data)
            if im:images.append((char,im))
    # Keep a source contact sheet for manual confirmation before choosing the bone.
    if scale==1:
        sheet=Image.new('RGB',(800,((len(images)+7)//8)*110),(110,126,141));d=ImageDraw.Draw(sheet)
        for j,(char,im) in enumerate(images):
            im=im.copy();im.thumbnail((94,85));x=j%8*100;y=j//8*110;sheet.paste(im,(x,y+20),im);d.text((x,y),str(char),fill='white')
        sheet.save(RAW/'bulan-bitmaps.png')
    # Original payload images: spear, Bulan's green staff, flame, Sulan's thrown staff.
    defs117,_=library(WORK/'Resource/image/game/living/Living117.swf')
    defs121,_=library(WORK/'Resource/image/game/living/living121.swf')
    defs126,_=library(WORK/'Resource/image/game/living/living126.swf')
    staff=bitmap(defs117[77]);fire=bitmap(defs126[56]);chief_staff=bitmap(defs121[81])
    fx=Image.new('RGBA',(32*8*scale,32*4*scale))
    for row,base in enumerate([pike,staff,fire,chief_staff]):
        base=base.crop(base.getbbox())
        for n in range(8):
            im=base.rotate(-n*45,expand=True) if row!=2 else base
            art=ImageOps.contain(im,(27*scale,27*scale),Image.Resampling.LANCZOS)
            fx.alpha_composite(art,(n*32*scale+(32*scale-art.width)//2,row*32*scale+(32*scale-art.height)//2))
    png(fx,out/'tribal797-projectiles.png',scale)
manifest=dict(loading_backdrop='Original outer boy/dragon artwork from video stage2 at2s, faded into smooth code-rendered blue/red panels. Avatars, VS, cards and progress remain dynamic.',projectiles={'11':'Living116.swf bitmap58 spear','12':'Living117.swf bitmap77 staff','13':'living126.swf bitmap56 flame','14':'living121.swf bitmap81 staff'},video_folder='tà thần phó bản/playertest',maps=MAPS,npcs=entries,poses='8 original timeline samples per labeled row, shared registration; no bitmap rotation as actor animation',loading_cards='User supplied video frame at 2 seconds, crop746,150,929,378; PC source show1/3/4 artwork corresponds, Mobi numbers2/3 retained',terrain='Original alpha only. No invented floor. Server collision sampled from the same 480x264 alpha as the client.',limitations='MIDP adaptation, not Flash playback. HP, damage and exact skill intervals remain explicit local tuning; footage cannot establish server formulas.')
(SOURCE/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf8')
print('Baked original NPC timelines, matching maps/collision, room/loading cards for both Nokia sizes.')
