"""Package verified owned assets, source, previews and integration snippets as a handoff ZIP."""
import argparse
import hashlib
import json
import zipfile
from pathlib import Path
from catalog import ASSETS, REPO, PREVIEW
from validate import validate


def package(output):
    validate(ASSETS)
    manifest=json.loads((ASSETS/'nature_walker/asset_manifest.json').read_text());entries={}
    for rel in manifest['files']:entries['assets/circle_of_inevitability/'+rel]=(ASSETS/rel).read_bytes()
    entries['assets/circle_of_inevitability/nature_walker/asset_manifest.json']=(ASSETS/'nature_walker/asset_manifest.json').read_bytes()
    for rel,keys in manifest['shared_files'].items():
        data=json.loads((ASSETS/rel).read_text())
        entries['assets/circle_of_inevitability/'+rel]=(json.dumps({k:data[k] for k in keys},indent=2,ensure_ascii=False)+'\n').encode()
    for folder in (Path(__file__).resolve().parent,PREVIEW):
        for p in sorted(folder.iterdir()):
            if p.is_file():entries[p.relative_to(REPO).as_posix()]=p.read_bytes()
    for rel in ('tools/model_gen/modelkit.py','docs/NATURE_WALKER.md','docs/NATURE_WALKER_ASSETS.md',
                'src/main/java/net/lukario/circleofinevitability/blasphemy_slate/beyonder_characteristics/nature_walker/NatureWalkerFx.java'):
        entries[rel]=(REPO/rel).read_bytes()
    java=(REPO/'src/main/java/net/lukario/circleofinevitability/sounds/ModSounds.java').read_text()
    start=java.index('    /** Nature Walker presentation');end=java.index(');',start)+2
    entries['integration/ModSounds_NATURE_WALKER.txt']=java[start:end].encode()
    entries['README.txt']=(
        'Nature Walker presentation handoff: 39 abilities, 101 model files, 59 original sound cues.\n'
        'Copy owned assets into src/main/resources/assets/circle_of_inevitability/.\n'
        'MERGE sounds.json and lang/en_us.json; never replace the existing shared files.\n'
        'Copy NatureWalkerFx.java into the matching package; it requires this repository’s ModelFx, ParticleFx and combat helpers.\n'
        'Merge integration/ModSounds_NATURE_WALKER.txt into ModSounds.java.\n'
        'See docs/NATURE_WALKER_ASSETS.md for contracts, regeneration and /naturefx previews.\n'
        'This asset ZIP excludes gameplay code. The repository implements the first 20 abilities.\n'
        'This is not a standalone mod or resource pack.\n').encode()
    output.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as archive:
        for n,data in sorted(entries.items()):archive.writestr(n,data)
    with zipfile.ZipFile(output) as archive:
        assert set(archive.namelist())==set(entries)
        for n,data in entries.items():assert archive.read(n)==data,n
        for rel,digest in manifest['files'].items():
            assert hashlib.sha256(archive.read('assets/circle_of_inevitability/'+rel)).hexdigest()==digest
    print(f'Packaged and verified {len(entries)} entries: {output}')


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True)
    package(p.parse_args().output.resolve())
