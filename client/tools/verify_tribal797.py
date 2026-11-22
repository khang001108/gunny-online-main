"""Validate the actual 7.9.7 installers, native NPC grids and terrain agreement."""
from pathlib import Path
import zipfile,io,json,hashlib,re,struct
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
checks=0
releases=[]
def ok(test,message):
 global checks
 if not test:raise AssertionError(message)
 checks+=1
for scale,name in [(1,'Gunny-E72-2009-Online-797'),(2,'Gunny-E6-Online-797')]:
 jar=ROOT/'public/downloads'/(name+'.jar');jad=jar.with_suffix('.jad');text=jad.read_text(encoding='utf8')
 ok('MIDlet-Version: 7.9.7' in text,'JAD version')
 ok('MIDlet-Jar-Size: '+str(jar.stat().st_size) in text,'Exact JAD byte size')
 ok(name+'.jar' in text,'Matching JAD URL')
 with zipfile.ZipFile(jar) as archive:
  ok(archive.testzip() is None,'ZIP CRC')
  names=archive.namelist();ok(len(names)==len(set(names)),'No duplicate ZIP entries')
  ok(not any(n.startswith(('tribal-npc-','tribal-cover-')) for n in names),'No provisional NPC/card sprites')
  ok(b'MIDlet-Version: 7.9.7' in archive.read('META-INF/MANIFEST.MF'),'Manifest version')
  code=archive.read('PhoneCanvas.class');ok(struct.unpack('>H',code[6:8])[0]==46 and b'StackMap' in code,'CLDC target and preverification')
  for cls in ['Battle','TribalRules']:
   ok(hashlib.sha256((ROOT/'client/src'/(cls+'.java')).read_bytes()).digest()==hashlib.sha256((ROOT/'backend/src'/(cls+'.java')).read_bytes()).digest(),'Client/server model equality')
  if scale==1:ok(all(i.compress_type==zipfile.ZIP_STORED and i.extract_version<=20 for i in archive.infolist()),'Legacy E72 stored ZIP entries')
  for kind in range(4,16):
   cell=(96 if kind>=14 else 72 if kind in [8,9] else 56)*scale
   im=Image.open(io.BytesIO(archive.read(f'tribal797-npc-{kind}.png'))).convert('RGBA')
   ok(im.size==(cell*8,cell*8),'Native pose grid size')
   ok(im.getchannel('A').getextrema()[0]==0,'Transparent NPC atlas')
   if kind in [4,6,8,9,14,15]:
    variants={hashlib.sha256(im.crop((n*cell,2*cell,(n+1)*cell,3*cell)).tobytes()).digest() for n in range(8)}
    ok(len(variants)>=4,'Distinct native attack frames')
  for ident in [8,9,10,11]:
   server=Image.open(ROOT/f'backend/assets/land{ident}.png').convert('RGBA')
   client=Image.open(io.BytesIO(archive.read(f'land{ident}.png'))).convert('RGBA').resize((480,264),Image.Resampling.NEAREST)
   alpha=client.getchannel('A').tobytes();original=server.getchannel('A').tobytes();bits=archive.read(f'collision{ident}.dat');hard=archive.read(f'hard{ident}.dat')
   ok(all((a>96)==(b>96)==bool(bits[n>>3]&(1<<(n&7))) for n,(a,b) in enumerate(zip(alpha,original))),'Exact handset/server collision agreement')
   ok(all(v==255 for v in hard),'All Tribal terrain is indestructible; collision alpha unchanged')
   ok(Image.open(io.BytesIO(archive.read(f'bg{ident}.png'))).size==(320*scale,240*scale),'Native-sized background')
  for stage in [1,2,3]:ok(Image.open(io.BytesIO(archive.read(f'tribal797-cover-{stage}.png'))).size==(80*scale,100*scale),'Video loading card')
 releases.append(dict(jar=str(jar.relative_to(ROOT)),jad=str(jad.relative_to(ROOT)),bytes=jar.stat().st_size,sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),entries=len(names),resolution=[320*scale,240*scale]))
report=dict(version='7.9.7',installer_asset_checks=checks,releases=releases,validation=dict(server='verify.ps1 passed; backend/build/verify797.log',tribal_rules=84,video_behavior=30,tcp_campaign=118,midp='UiTribal797Check both sizes; heap32MB/128MB',ui_regression='Ui796Check both sizes',hardware='Not tested on physical handsets',production='Not deployed'))
(ROOT/'docs/BUILD-7.9.7.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
print('PASS:',checks,'installer, native-motion and collision checks')
print(json.dumps(releases,indent=2))
