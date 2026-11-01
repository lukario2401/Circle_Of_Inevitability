"""Bundle owned Thunder God resources and integration sources; shared JSON is filtered.
This does not overwrite existing mod resource files. See THUNDER_GOD_ASSETS.md.
"""
import argparse
import json
from pathlib import Path
import zipfile
from generate import ASSETS, REPO, PREFIX, NS


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output',type=Path,required=True);args=p.parse_args()
    manifest=json.loads((ASSETS/f'{PREFIX}/asset_manifest.json').read_text())
    args.output.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(args.output,'w',zipfile.ZIP_DEFLATED) as z:
        for rel in manifest['files']:
            z.write(ASSETS/rel,f'assets/{NS}/{rel}')
        z.write(ASSETS/f'{PREFIX}/asset_manifest.json',f'assets/{NS}/{PREFIX}/asset_manifest.json')
        for rel in ('sounds.json','lang/en_us.json'):
            data=json.loads((ASSETS/rel).read_text())
            only={k:v for k,v in data.items() if k.startswith(f'{PREFIX}.') or k.startswith(f'subtitles.{NS}.{PREFIX}.')}
            z.writestr(f'assets/{NS}/{rel}',json.dumps(only,indent=2)+'\n')
        meta=json.loads((ASSETS.parent.parent/'pack.mcmeta').read_text())
        meta['pack']['description']='Thunder God: storm, sea and silver lightning resources'
        z.writestr('pack.mcmeta',json.dumps(meta,indent=2)+'\n')
        for folder in ('tools/model_gen/thunder_god','docs/assets/thunder_god'):
            for file in (REPO/folder).rglob('*'):
                if file.is_file() and '__pycache__' not in file.parts:z.write(file,file.relative_to(REPO))
        for rel in ('LICENSE.txt','tools/model_gen/modelkit.py','docs/THUNDER_GOD.md','docs/THUNDER_GOD_ASSETS.md',
                    'src/main/java/net/lukario/circleofinevitability/blasphemy_slate/beyonder_characteristics/thunder_god/ThunderGodFx.java'):
            if rel=='docs/THUNDER_GOD_ASSETS.md':
                z.writestr(rel,(REPO/rel).read_text().replace('../src/main/resources/assets/','../assets/'))
            else:z.write(REPO/rel,rel)
        events='\n'.join(f'Map.entry("{key}", sound("{PREFIX}.{key}"))' for key in manifest['sound_events'])
        z.writestr('integration/sound-registrations.txt','// Entries for ModSounds.THUNDER_GOD = Map.ofEntries(...):\n'+events.replace('\n',',\n')+'\n')
        z.writestr('integration/pathway-icon.txt','ICONS.put("Thunder God", icon("thunder_god"));\n')
        z.writestr('README.txt','Thunder God asset pack\n\nStart with docs/THUNDER_GOD_ASSETS.md.\nAssets are at assets/circle_of_inevitability/.\nThe bundled sounds.json and lang/en_us.json contain only Thunder God entries. Merge them into existing mod JSON; do not overwrite other pathways.\nForge sound registration and cosmetic adapter source are included under integration/ and src/.\nAbility gameplay remains unimplemented.\n')
    with zipfile.ZipFile(args.output) as z:
        assert z.testzip() is None
        for rel,digest in manifest['files'].items():
            import hashlib
            assert hashlib.sha256(z.read(f'assets/{NS}/{rel}')).hexdigest()==digest
        assert len(json.loads(z.read(f'assets/{NS}/sounds.json')))==len(manifest['sound_events'])
        print(f'Packaged and verified {len(z.namelist())} entries: {args.output} ({args.output.stat().st_size:,} bytes)')


if __name__=='__main__':main()
