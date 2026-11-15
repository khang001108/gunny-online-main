"""Rebuild CLDC client classes and preserve the shipped handset assets."""
import argparse
import copy
from pathlib import Path
import subprocess
import zipfile
import re

root = Path(__file__).resolve().parent
parser = argparse.ArgumentParser()
parser.add_argument('--tools', type=Path, required=True, help='Directory containing ecj.jar, proguard.jar, cldcapi11.jar, midpapi20.jar')
args = parser.parse_args()
tools = args.tools.resolve()
build = root / 'build'
classes = build / 'classes'
classes.mkdir(parents=True, exist_ok=True)
subprocess.run(['java', '-jar', str(tools/'ecj.jar'), '-nowarn', '-source', '1.3', '-target', '1.2', '-encoding', 'UTF-8', '-bootclasspath', str(tools/'cldcapi11.jar')+';'+str(tools/'midpapi20.jar'), '-d', str(classes), *map(str, sorted((root/'src').glob('*.java')))], check=True)
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
downloads = root.parent/'public/downloads'
with zipfile.ZipFile(build/'preverified.jar') as code:
    for name in ('Gunny-E6-Online-783', 'Gunny-E72-2009-Online-783'):
        jar = downloads/(name+'.jar')
        temp = build/(name+'.jar')
        with zipfile.ZipFile(jar) as original, zipfile.ZipFile(temp, 'w', zipfile.ZIP_DEFLATED) as output:
            for info in original.infolist():
                if info.filename.endswith('.class'):
                    continue
                data = original.read(info.filename)
                if info.filename.upper() == 'META-INF/MANIFEST.MF':
                    data = data.replace(b'socket://gunnyjava.xubi.org:22122', b'socket://gunnyjava.xubi.org:22123')
                output.writestr(info, data)
            for info in code.infolist():
                if info.filename.endswith('.class'):
                    output.writestr(copy.copy(info), code.read(info.filename))
        jar.write_bytes(temp.read_bytes())
        jad = downloads/(name+'.jad')
        text = jad.read_text(encoding='utf-8').replace(':22122', ':22123')
        text = re.sub(r'MIDlet-Jar-Size: \d+', 'MIDlet-Jar-Size: '+str(jar.stat().st_size), text)
        jad.write_text(text, encoding='utf-8')
installer = downloads/'Gunny-E6-Online-CaiDat-7.8.3.zip'
temp = build/installer.name
with zipfile.ZipFile(installer) as original, zipfile.ZipFile(temp, 'w', zipfile.ZIP_DEFLATED) as output:
    for info in original.infolist():
        replacement = downloads/Path(info.filename).name
        data = replacement.read_bytes() if info.filename.endswith(('.jar', '.jad')) and replacement.is_file() else original.read(info.filename)
        if info.filename.endswith('.txt'):
            data = data.replace(b':22122', b':22123')
        output.writestr(info, data)
installer.write_bytes(temp.read_bytes())
print('Rebuilt E72/E6 JAR, JAD and E6 installer ZIP.')
