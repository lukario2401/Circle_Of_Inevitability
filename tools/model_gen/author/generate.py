"""Regenerate Author assets and merge only owned keys in shared JSON resources."""
import argparse
import hashlib
import json
from pathlib import Path
import icons
import models
import sounds
from catalog import ASSETS, NS, ABILITIES, BASIC, WHEELS, KEYS, STATUSES, PARTICLES, SOUNDS, NAMES, SEQUENCES, ABILITY_MODELS


def write_json(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')


def merge(path,entries):
    current=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
    current.update(entries);write_json(path,current)


def generate(root,skip_audio=False):
    icons.generate(root);records=models.generate(root)
    if not skip_audio:merge(root/'sounds.json',sounds.generate(root))
    lang={f'ability.{NS}.author.{n}':v for n,v in NAMES.items()}
    lang.update({f'subtitles.{NS}.author.{n}':v for n,v in sounds.SUBTITLES.items()})
    lang.update({f'pathway.{NS}.author':'Author',f'hud.{NS}.author.insight':'Insight %s/5',
                 f'hud.{NS}.author.strain':'Strain %s/100',f'hud.{NS}.author.resolve':'Resolve %s s'})
    merge(root/'lang/en_us.json',lang)
    files=[]
    for folder in ('textures/gui/ability_menus/author','textures/gui/author','textures/model_fx/author',
                   'models/model_fx/author','textures/particle/fx/author','sounds/author'):
        files.extend(p for p in (root/folder).rglob('*') if p.is_file())
    files.append(root/'textures/gui/pathway_icons/author.png')
    manifest={'schema_version':1,'namespace':NS,'pathway':'Author','source_design':'docs/AUTHOR.md',
              'scope':'Registered Author pathway with all 26 playable abilities, connected visual assets and sounds.',
              'abilities':ABILITIES,'sequences':SEQUENCES,'basic':BASIC,
              'wheels':{'author_'+k:v for k,v in WHEELS.items()},
              'key_icons':dict(enumerate(KEYS,1)),'status_icons':STATUSES,'particles':PARTICLES,
              'sounds':SOUNDS,'ability_models':ABILITY_MODELS,'models':records,
              'animations':{n:{'frames':[f'{n}_{i}' for i in range(4)],'pivot_pixels':[8,0,8]}
                            for n in ('mind_dragon','mind_dragon_incomplete','imagined_drake')},
              'texture_animations':{'dream_atlas':{'frames':4,'size':[64,64],'frametime':4}},
              'shared_files':{'sounds.json':['author.'+n for n in SOUNDS],'lang/en_us.json':list(lang)},
              'files':{p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(files)}}
    write_json(root/'author/asset_manifest.json',manifest)
    print(f'Generated {len(files)} resources: {len(ABILITIES)} abilities, {len(records)} models, {len(SOUNDS)} sounds.')


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--assets',type=Path,default=ASSETS)
    parser.add_argument('--skip-audio',action='store_true',help='Keep existing sounds for visual iteration.')
    args=parser.parse_args();generate(args.assets.resolve(),args.skip_audio)
