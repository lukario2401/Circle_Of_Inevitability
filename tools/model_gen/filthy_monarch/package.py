"""Validated, self-contained asset handoff. Shared JSON contains only owned entries."""
import argparse
import hashlib
import json
import zipfile
from pathlib import Path
from catalog import ASSETS,REPO,PREVIEW
from validate import validate

def package(output):
 validate(ASSETS)
 manifest=json.loads((ASSETS/'filthy_monarch/asset_manifest.json').read_text());entries={}
 prefix='assets/circle_of_inevitability/'
 for rel in manifest['files']:entries[prefix+rel]=(ASSETS/rel).read_bytes()
 entries[prefix+'filthy_monarch/asset_manifest.json']=(ASSETS/'filthy_monarch/asset_manifest.json').read_bytes()
 for rel,keys in manifest['shared_files'].items():
  data=json.loads((ASSETS/rel).read_text());entries[prefix+rel]=(json.dumps({k:data[k] for k in keys},indent=2,ensure_ascii=False)+'\n').encode()
 for folder in (Path(__file__).resolve().parent,PREVIEW):
  for p in sorted(folder.iterdir()):
   if p.is_file():entries[p.relative_to(REPO).as_posix()]=p.read_bytes()
 for rel in ('tools/model_gen/modelkit.py','LICENSE.txt','docs/pathways/filthy_monarch/FILTHY_MONARCH.md','docs/pathways/filthy_monarch/FILTHY_MONARCH_ASSETS.md',
             'src/main/java/net/lukario/circleofinevitability/blasphemy_slate/beyonder_characteristics/filthy_monarch/FilthyMonarchFx.java'):
  entries[rel]=(REPO/rel).read_bytes()
 java=(REPO/'src/main/java/net/lukario/circleofinevitability/sounds/ModSounds.java').read_text()
 start=java.index('    /** Filthy Monarch presentation');end=java.index('    /** Author (Spectator)',start)
 entries['integration/ModSounds_FILTHY_MONARCH.txt']=java[start:end].encode()
 entries['integration/PathwayHudIcon_FILTHY_MONARCH.txt']=b'        ICONS.put("Filthy Monarch", icon("filthy_monarch"));\n'
 entries['README.txt']=(
  'Filthy Monarch: 35 ability presentations, models, particles, icons and 59 original sounds.\n'
  'Presentation only: no combat or playable pathway is included.\n'
  'Copy assets into src/main/resources/assets/circle_of_inevitability/.\n'
  'MERGE the supplied sounds.json and lang/en_us.json keys; do not replace the project files.\n'
  'The manifest hashes refer to asset paths, relative to assets/circle_of_inevitability/.\n'
  'FilthyMonarchFx.java requires the existing Circle Of Inevitability ModelFx/ParticleFx/Spells APIs.\n'
  'Merge the sound and HUD integration snippets into the corresponding existing classes.\n'
  'This archive is a source asset handoff, not a standalone mod or Minecraft resource pack.\n').encode()
 output.parent.mkdir(parents=True,exist_ok=True)
 with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as archive:
  for name,data in sorted(entries.items()):archive.writestr(name,data)
 with zipfile.ZipFile(output) as archive:
  assert set(archive.namelist())==set(entries)
  for name,data in entries.items():assert archive.read(name)==data,name
  for rel,digest in manifest['files'].items():assert hashlib.sha256(archive.read(prefix+rel)).hexdigest()==digest,rel
 print(f'Packaged and verified {len(entries)} entries: {output}')

if __name__=='__main__':
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',type=Path,required=True)
 package(parser.parse_args().output.resolve())
