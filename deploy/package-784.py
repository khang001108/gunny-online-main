"""Create the Pi package from current sources; never include local secrets or Git/build caches."""
from pathlib import Path
import argparse, hashlib, json, zipfile

root=Path(__file__).resolve().parent.parent
p=argparse.ArgumentParser()
p.add_argument('--out',type=Path,required=True)
p.add_argument('--server-jar',type=Path,required=True)
args=p.parse_args();args.out.mkdir(parents=True,exist_ok=True)
target=args.out/'Gunny-online-main-Pi5-7.8.4.zip'
exact={'Dockerfile','.dockerignore','.gitignore','.env.example','README.md','verify.ps1','verify.sh',
       'backend/tribal.properties','client/build.py','client/README.md',
       'supabase/GUNNY-ALL.sql','supabase/UPDATE-7.7.0-TO-7.8.4.sql',
       'docs/BAO-CAO-7.8.4.md','docs/FILE-CHANGES-7.8.4.txt','deploy/package-784.py'}
prefixes=('backend/src/','backend/assets/','backend/tests/','client/src/','client/tests/',
          'deploy/pi5/','deploy/portainer/','public/')
rows=[]
with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED,compresslevel=7,allowZip64=False) as z:
    for file in sorted(root.rglob('*')):
        if not file.is_file():continue
        name=file.relative_to(root).as_posix()
        if name not in exact and not name.startswith(prefixes):continue
        if any(part in ('.git','__pycache__','backups','build','.codex') for part in file.relative_to(root).parts):continue
        if file.name in ('.env','gunny.env') or file.suffix.lower() in ('.log','.class','.pyc','.pem','.key'):continue
        if file.suffix.lower() in ('.jar','.jad','.zip') and not ('784' in file.name or '7.8.4' in file.name):continue
        if name.startswith('public/downloads/') and file.suffix.lower()=='.zip' and '7.8.4' not in file.name:continue
        data=file.read_bytes();z.writestr('gunny-online-main/'+name,data)
        rows.append({'file':name,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()})
    data=args.server_jar.read_bytes();z.writestr('gunny-online-main/gunny-server-7.8.4.jar',data)
    rows.append({'file':'gunny-server-7.8.4.jar','bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()})
    z.writestr('gunny-online-main/RELEASE-7.8.4.json',json.dumps(rows,ensure_ascii=False,indent=2))
with zipfile.ZipFile(target) as z:
    assert z.testzip() is None
    names=z.namelist()
    assert all('/.git/' not in n and not n.endswith(('/.env','/gunny.env','.log','.class')) for n in names)
    assert any(n.endswith('/supabase/UPDATE-7.7.0-TO-7.8.4.sql') for n in names)
    assert any(n.endswith('/backend/assets/land11.png') for n in names)
digest=hashlib.sha256(target.read_bytes()).hexdigest()
(args.out/'SHA256SUMS.txt').write_text(digest+'  '+target.name+'\n',encoding='ascii')
print(json.dumps({'file':str(target),'bytes':target.stat().st_size,'files':len(rows),'sha256':digest}))
