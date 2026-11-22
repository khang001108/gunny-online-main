"""Rebuild CLDC client classes and preserve the shipped handset assets."""
import argparse
import copy
from pathlib import Path
import subprocess
import zipfile
import re
import os
import time

root = Path(__file__).resolve().parent
parser = argparse.ArgumentParser()
parser.add_argument('--tools', type=Path, required=True, help='Directory containing ecj.jar, proguard.jar, cldcapi11.jar, midpapi20.jar')
parser.add_argument('--code-only', action='store_true', help='Compile and preverify without replacing release installers')
args = parser.parse_args()
tools = args.tools.resolve()
build = root / 'build'
classes = build / 'classes'
classes.mkdir(parents=True, exist_ok=True)
for stale in classes.glob('*.class'):
    stale.unlink()
subprocess.run(['java', '-jar', str(tools/'ecj.jar'), '-nowarn', '-source', '1.3', '-target', '1.2', '-encoding', 'UTF-8', '-bootclasspath', str(tools/'cldcapi11.jar')+os.pathsep+str(tools/'midpapi20.jar'), '-sourcepath', str(root/'src'), '-d', str(classes), str(root/'src/GunnyMIDlet.java')], check=True)
with zipfile.ZipFile(build/'classes.jar', 'w', zipfile.ZIP_DEFLATED) as archive:
    for path in classes.glob('*.class'):
        archive.write(path, path.name)
config = build/'proguard.pro'
config.write_text('\n'.join([
    '-injars "'+str(build/'classes.jar')+'"',
    '-outjars "'+str(build/'preverified.jar')+'"',
    '-libraryjars "'+str(tools/'cldcapi11.jar')+'"',
    '-libraryjars "'+str(tools/'midpapi20.jar')+'"',
    '-microedition', '-target 1.2', '-dontshrink', '-dontoptimize', '-dontobfuscate', '-keepattributes Exceptions,InnerClasses',
]), encoding='utf-8')
subprocess.run(['java', '-jar', str(tools/'proguard.jar'), '@'+str(config)], check=True)
if args.code_only:
    print('Compiled and preverified client classes.')
    raise SystemExit(0)
downloads = root.parent/'public/downloads'
release_errors=[]
with zipfile.ZipFile(build/'preverified.jar') as code:
    for name in ('Gunny-E6-Online-797', 'Gunny-E72-2009-Online-797'):
        jar = downloads/(name+'.jar')
        temp = build/(name+'.jar')
        overlays = {p.name:p.read_bytes() for p in (root/('assets2' if 'E6' in name else 'assets1')).iterdir() if p.is_file() and p.suffix in ('.png','.dat') and p.name!='tribal797-loading-bg.png'}
        seed = jar if jar.exists() else downloads/(name.replace('797','796')+'.jar')
        with zipfile.ZipFile(seed) as original, zipfile.ZipFile(temp, 'w', zipfile.ZIP_DEFLATED) as output:
            for info in original.infolist():
                if (info.filename=='tribal797-loading-bg.png' or info.filename.startswith(('tribal-npc-', 'tribal-cover-')) or info.filename.startswith(('wing792-','wing793-','wing795-show-')) or re.match(r'vip-wing-[0-5]-(24|44|72)\.png$', info.filename)) or info.filename.startswith('vip-progress') or 'vip-ring-' in info.filename and '-216.png' in info.filename or info.filename.endswith('.class') or info.filename in overlays or info.filename in ('stone780.png','forge780-success.png','forge780-success-slot.png'):
                    continue
                data = original.read(info.filename)
                if info.filename.upper() == 'META-INF/MANIFEST.MF':
                    data = data.replace(b'7.9.6',b'7.9.7').replace(b'7.9.5',b'7.9.7').replace(b'7.9.4',b'7.9.7').replace(b'7.9.3',b'7.9.7').replace(b'7.9.2',b'7.9.7').replace(b'7.9.1',b'7.9.7').replace(b'7.9.0',b'7.9.7').replace(b'7.8.8',b'7.9.7').replace(b'7.8.9',b'7.9.7').replace(b'socket://gunnyjava.xubi.org:22122', b'socket://gunnyjava.xubi.org:22123')
                output.writestr(info, data)
            for filename,data in overlays.items():
                entry=zipfile.ZipInfo(filename,(2000,1,1,0,0,0));entry.create_system=0;entry.extra=b"";entry.flag_bits=0;entry.external_attr=0x20
                entry.compress_type=zipfile.ZIP_STORED if "E72" in name else zipfile.ZIP_DEFLATED
                entry.create_version=entry.extract_version=10 if "E72" in name else 20
                output.writestr(entry,data)
            for info in code.infolist():
                if info.filename.endswith('.class'):
                    entry=copy.copy(info)
                    entry.extra=b""
                    entry.flag_bits=0
                    entry.create_system=0
                    entry.compress_type=zipfile.ZIP_STORED if "E72" in name else zipfile.ZIP_DEFLATED
                    entry.create_version=entry.extract_version=10 if "E72" in name else 20
                    entry.external_attr=0x20
                    output.writestr(entry, code.read(info.filename))
        for attempt in range(6):
            try:
                os.replace(temp, jar)
                break
            except OSError:
                if attempt == 5:
                    release_errors.append(str(jar))
                    print("Khong the cap nhat "+str(jar)+". Hay dong gia lap/chuong trinh dang mo file JAR, roi chay lai client/Build.bat.")
                    break
                time.sleep(0.3)

        if str(jar) in release_errors:
            continue
        with zipfile.ZipFile(jar) as installed:
            for entry in code.infolist():
                if entry.filename.endswith('.class') and installed.read(entry.filename)!=code.read(entry.filename):
                    raise SystemExit('Sai ma nguon trong JAR: '+str(jar))
        jad = downloads/(name+'.jad')
        jad_seed = jad if jad.exists() else downloads/(name.replace('797','796')+'.jad')
        text = jad_seed.read_text(encoding='utf-8').replace('7.9.6','7.9.7').replace('Online-796','Online-797').replace('7.9.5','7.9.7').replace('7.9.4','7.9.7').replace('7.9.3','7.9.7').replace('7.9.2','7.9.7').replace('Online-795','Online-797').replace('Online-794','Online-797').replace('Online-793','Online-797').replace('Online-792','Online-797').replace('7.9.1','7.9.7').replace('Online-791','Online-797').replace('7.9.0','7.9.7').replace('Online-790','Online-797').replace(':22122', ':22123').replace('7.8.8','7.9.7').replace('7.8.9','7.9.7').replace('Online-788','Online-797').replace('Online-789','Online-797')
        text = re.sub(r'MIDlet-Jar-Size: \d+', 'MIDlet-Jar-Size: '+str(jar.stat().st_size), text)
        jad.write_text(text, encoding='utf-8')
if release_errors:
    raise SystemExit('CHUA cap nhat cac ban cai dang bi khoa: '+', '.join(release_errors))
print('Rebuilt and verified current E72/E6 JAR and JAD.')
