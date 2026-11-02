"""Create a verified handoff ZIP with owned resources, filtered shared JSON, tools and previews."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile
from catalog import ASSETS, REPO, NS, PREFIX


def package(output):
    manifest=json.loads((ASSETS/f'{PREFIX}/asset_manifest.json').read_text(encoding='utf-8'))
    output.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as archive:
        for rel in manifest['files']:
            archive.write(ASSETS/rel,f'assets/{NS}/{rel}')
        archive.write(ASSETS/f'{PREFIX}/asset_manifest.json',f'assets/{NS}/{PREFIX}/asset_manifest.json')
        for rel,keys in manifest['shared_files'].items():
            data=json.loads((ASSETS/rel).read_text(encoding='utf-8'))
            archive.writestr(f'assets/{NS}/{rel}',json.dumps({k:data[k] for k in keys},indent=2,ensure_ascii=False)+'\n')
        metadata=json.loads((REPO/'src/main/resources/pack.mcmeta').read_text(encoding='utf-8'))
        metadata['pack']['description']='White Angel: sunlight, order and faith assets'
        archive.writestr('pack.mcmeta',json.dumps(metadata,indent=2)+'\n')
        for folder in ('tools/model_gen/white_angel','docs/assets/white_angel'):
            for path in sorted((REPO/folder).rglob('*')):
                if path.is_file() and '__pycache__' not in path.parts:archive.write(path,path.relative_to(REPO))
        archive.write(REPO/'tools/model_gen/modelkit.py','tools/model_gen/modelkit.py')
        for name in ('LICENSE.txt','LICENSE'):
            if (REPO/name).is_file():archive.write(REPO/name,name)
        text=(REPO/'docs/WHITE_ANGEL_ASSETS.md').read_text(encoding='utf-8')
        archive.writestr('docs/WHITE_ANGEL_ASSETS.md',text.replace('../src/main/resources/assets/','../assets/'))
        registrations=',\n'.join(f'    java.util.Map.entry("{n}", sound("{PREFIX}.{n}"))' for n in manifest['sounds'])
        archive.writestr('integration/sound-registration.txt',
                        '// Add to ModSounds after its SOUNDS register is initialized.\n'
                        'public static final java.util.Map<String, RegistryObject<SoundEvent>> WHITE_ANGEL =\n'
                        '    java.util.Map.ofEntries(\n'+registrations+');\n')
        archive.writestr('README.txt',
                        'White Angel assets\n\n'
                        'Start with docs/WHITE_ANGEL_ASSETS.md and its preview links.\n'
                        'assets/circle_of_inevitability/ contains only White Angel resources.\n'
                        'When importing source files, merge the bundled sounds.json and lang/en_us.json; '
                        'do not overwrite other pathways.\n'
                        'integration/sound-registration.txt records the required Forge sound event registration.\n'
                        'This can overlay the updated mod as a resource pack; it requires the mod for ModelFx '
                        'rendering and registered sound events. Abilities are not implemented.\n'
                        'To use the generators, copy assets into src/main/resources/assets/ in a checkout '
                        'of Circle_Of_Inevitability, as documented.\n')
    with zipfile.ZipFile(output) as archive:
        assert archive.testzip() is None
        for rel,digest in manifest['files'].items():
            assert hashlib.sha256(archive.read(f'assets/{NS}/{rel}')).hexdigest()==digest,rel
        for rel,keys in manifest['shared_files'].items():
            assert set(json.loads(archive.read(f'assets/{NS}/{rel}')))==set(keys),rel
        print(f'Packaged and verified {len(archive.namelist())} entries: {output} ({output.stat().st_size:,} bytes)')


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path,required=True)
    package(parser.parse_args().output.resolve())
