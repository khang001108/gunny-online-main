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
APP_VERSION = "7.9.20"
APP_VENDOR = "Ronin-Lap"
APP_DESCRIPTION = "Gunny Java Online - Tac gia: Ronin-Lap"

def repair_manifest(data):
    # These unsigned Java ME installers use a single main attribute section.
    # Repair old releases that appended MIDlet-Description after a blank line.
    lines = data.splitlines()
    attrs = []
    seen = set()
    for line in lines:
        if not line.strip():
            continue
        if line.startswith(b" "):
            if not attrs:
                raise ValueError("Invalid manifest continuation")
            attrs[-1] += line[1:]
            continue
        key, sep, value = line.partition(b": ")
        if not sep or not re.fullmatch(rb"[A-Za-z0-9_-]+", key):
            raise ValueError("Invalid manifest attribute: " + repr(line))
        if key.lower() == b"name":
            raise ValueError("Unexpected named manifest section; refusing to flatten it")
        attrs.append(line)
    updates = {
        b"midlet-version": (b"MIDlet-Version", APP_VERSION.encode("ascii")),
        b"midlet-vendor": (b"MIDlet-Vendor", APP_VENDOR.encode("ascii")),
        b"midlet-description": (b"MIDlet-Description", APP_DESCRIPTION.encode("ascii")),
    }
    result = []
    for line in attrs:
        key = line.split(b":", 1)[0].lower()
        if key in seen:
            if key in updates:
                continue
            raise ValueError("Duplicate manifest attribute: " + repr(key))
        seen.add(key)
        if key in updates:
            name, value = updates[key]
            line = name + b": " + value
        if key == b"gunny-server-url":
            line = line.replace(b"socket://gunnyjava.xubi.org:22122", b"socket://gunnyjava.xubi.org:22123")
        result.append(line)
    for key, (name, value) in updates.items():
        if key not in seen:
            result.append(name + b": " + value)
    if not result or not result[0].lower().startswith(b"manifest-version: "):
        raise ValueError("Missing Manifest-Version at start of manifest")
    required = (b"midlet-name", b"midlet-1", b"microedition-configuration", b"microedition-profile")
    if any(key not in seen for key in required):
        raise ValueError("Missing required Java ME manifest attribute")
    # Fold long lines without changing the value; use CRLF throughout.
    physical = []
    for line in result:
        physical.append(line[:70])
        rest = line[70:]
        while rest:
            physical.append(b" " + rest[:69])
            rest = rest[69:]
    return b"\r\n".join(physical) + b"\r\n\r\n"

def upsert_prop_text(text, key, value):
    line = key + ": " + value
    pattern = r"(?m)^" + re.escape(key) + r": [^\r\n]+"
    if re.search(pattern, text):
        return re.sub(pattern, line, text)
    ending = "" if text.endswith(("\n", "\r")) else "\n"
    return text + ending + line + "\n"
# These sizes/effects have no runtime references; do not reintroduce them via overlays.
def retired_asset(name):
    return name.startswith('hf39-npc-') or name == 'fx-pow-ring.png' or name.startswith('wing795-game-') or bool(re.fullmatch(r'vip-ring-[0-2]-(144|216)\.png', name)) or bool(re.fullmatch(r'vip-ring-hf18-[0-2]-8\.png', name))

with zipfile.ZipFile(build/'preverified.jar') as code:
    for name in ('Gunny-E6-Online-7918', 'Gunny-E72-2009-Online-7918'):
        jar = downloads/(name+'.jar')
        temp = build/(name+'.jar')
        overlays = {p.name:p.read_bytes() for p in (root/('assets2' if 'E6' in name else 'assets1')).iterdir() if p.is_file() and p.suffix in ('.png','.dat') and p.name!='tribal797-loading-bg.png' and not retired_asset(p.name)}
        seed = jar if jar.exists() else downloads/(name.replace('7918','7917')+'.jar')
        with zipfile.ZipFile(seed) as original, zipfile.ZipFile(temp, 'w', zipfile.ZIP_DEFLATED) as output:
            for info in original.infolist():
                if retired_asset(info.filename) or (info.filename=='tribal797-loading-bg.png' or info.filename.startswith(('tribal-npc-', 'tribal-cover-')) or info.filename.startswith(('wing792-','wing793-','wing795-show-')) or re.match(r'vip-wing-[0-5]-(24|44|72)\.png$', info.filename)) or info.filename.startswith('vip-progress') or 'vip-ring-' in info.filename and '-216.png' in info.filename or info.filename.endswith('.class') or info.filename in overlays or info.filename in ('stone780.png','forge780-success.png','forge780-success-slot.png'):
                    continue
                data = original.read(info.filename)
                if info.filename.upper() == 'META-INF/MANIFEST.MF':
                    data = repair_manifest(data)
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
        with zipfile.ZipFile(temp) as candidate:
            manifest = candidate.read('META-INF/MANIFEST.MF')
            if repair_manifest(manifest) != manifest:
                raise SystemExit('Manifest normalization failed: '+str(temp))
            if 'GunnyMIDlet.class' not in candidate.namelist():
                raise SystemExit('Missing GunnyMIDlet.class: '+str(temp))
            bad_entry = candidate.testzip()
            if bad_entry is not None:
                raise SystemExit('CRC error: '+bad_entry)
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
        jad_seed = jad if jad.exists() else downloads/(name.replace('7918','7917')+'.jad')
        text = jad_seed.read_text(encoding="utf-8").replace(":22122", ":22123")
        text = upsert_prop_text(text, "MIDlet-Version", APP_VERSION)
        text = upsert_prop_text(text, "MIDlet-Vendor", APP_VENDOR)
        text = upsert_prop_text(text, "MIDlet-Description", APP_DESCRIPTION)
        text = upsert_prop_text(text, "MIDlet-Jar-URL", name+".jar")
        text = upsert_prop_text(text, "MIDlet-Jar-Size", str(jar.stat().st_size))
        jad.write_text(text, encoding='utf-8')
if release_errors:
    raise SystemExit('CHUA cap nhat cac ban cai dang bi khoa: '+', '.join(release_errors))
print('Rebuilt and verified current E72/E6 JAR and JAD.')
