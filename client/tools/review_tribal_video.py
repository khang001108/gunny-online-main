from pathlib import Path
import subprocess, json
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
VIDEOS=ROOT.parents[2]/'tà thần phó bản/playertest'
OUT=ROOT/'build/tribal-review'
FF=ROOT.parents[2]/'GunnyNokia/tools/video_review/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v9.0.2.exe'
DURATIONS=(145,144,137,86)
def sheet(paths,target,columns=2,width=624):
    thumb_h=round(width*576/1248)
    canvas=Image.new('RGB',(width*columns,(thumb_h+24)*((len(paths)+columns-1)//columns)),(22,25,33))
    d=ImageDraw.Draw(canvas)
    for n,p in enumerate(paths):
        im=Image.open(p).convert('RGB');im.thumbnail((width,thumb_h))
        x=(n%columns)*width;y=(n//columns)*(thumb_h+24)
        canvas.paste(im,(x,y+24));d.text((x+6,y+5),p.parent.name+' / '+str(int(p.stem)-1)+'s',fill='white')
    canvas.save(target)
if __name__=='__main__':
    OUT.mkdir(parents=True,exist_ok=True)
    manifest=[]
    for index,path in enumerate(sorted(VIDEOS.glob('*.mp4'))):
        key='gold-boss' if 'vàng' in path.name else 'red-boss' if 'đỏ' in path.name else 'stage1' if '1.mp4' in path.name else 'stage2';folder=OUT/key;folder.mkdir(exist_ok=True)
        if not (folder/'00001.jpg').exists():
            subprocess.run([str(FF),'-hide_banner','-loglevel','error','-i',str(path),'-vf','fps=1','-q:v','2',str(folder/'%05d.jpg')],check=True)
        frames=sorted(folder.glob('*.jpg'))
        for start in range(0,len(frames),48):
            sheet(frames[start:start+48:4],OUT/f'{key}-overview-{start//48}.jpg')
        manifest.append({'key':key,'video':str(path),'frames_1fps':len(frames)})
    (OUT/'videos.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(manifest,ensure_ascii=True))
