"""Package the Author design/assets handoff, filtering shared resources to owned entries."""
import argparse
import hashlib
import json
import zipfile
from pathlib import Path
from catalog import ASSETS, REPO, PREVIEW
from validate import validate


def package(output):
    validate(ASSETS)
    manifest=json.loads((ASSETS/'author/asset_manifest.json').read_text())
    entries={}
    for rel in manifest['files']:
        entries['assets/circle_of_inevitability/'+rel]=(ASSETS/rel).read_bytes()
    entries['assets/circle_of_inevitability/author/asset_manifest.json']=(ASSETS/'author/asset_manifest.json').read_bytes()
    for rel,keys in manifest['shared_files'].items():
        data=json.loads((ASSETS/rel).read_text())
        entries['assets/circle_of_inevitability/'+rel]=(json.dumps({k:data[k] for k in keys},indent=2,ensure_ascii=False)+'\n').encode()
    for folder in (Path(__file__).resolve().parent,PREVIEW):
        for p in sorted(folder.iterdir()):
            if p.is_file():entries[p.relative_to(REPO).as_posix()]=p.read_bytes()
    for rel in ('tools/model_gen/modelkit.py','docs/AUTHOR.md','docs/AUTHOR_ASSETS.md'):
        entries[rel]=(REPO/rel).read_bytes()
    java=(REPO/'src/main/java/net/lukario/circleofinevitability/sounds/ModSounds.java').read_text()
    start=java.index('    /** Author (Spectator)');end=java.index('    /** White Angel',start)
    entries['integration/ModSounds_AUTHOR.txt']=java[start:end].encode()
    entries['README.txt']=(
        'Author / Spectator asset handoff. All 26 abilities are implemented in the repository; Java gameplay is not included in this archive.\n'
        'Copy owned assets into src/main/resources/assets/circle_of_inevitability/.\n'
        'MERGE sounds.json and lang/en_us.json; do not replace the project files.\n'
        'Register sound events using integration/ModSounds_AUTHOR.txt in ModSounds.\n'
        'See docs/AUTHOR.md for mechanics and docs/AUTHOR_ASSETS.md for formats.\n'
        'This handoff is not a standalone mod or resource pack.\n').encode()
    output.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as archive:
        for name,data in sorted(entries.items()):archive.writestr(name,data)
    with zipfile.ZipFile(output) as archive:
        assert set(archive.namelist())==set(entries)
        for name,data in entries.items():assert archive.read(name)==data,name
        for rel,digest in manifest['files'].items():
            assert hashlib.sha256(archive.read('assets/circle_of_inevitability/'+rel)).hexdigest()==digest
    print(f'Packaged and verified {len(entries)} entries: {output}')


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',type=Path,required=True)
    package(parser.parse_args().output.resolve())
