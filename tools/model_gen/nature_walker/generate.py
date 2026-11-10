"""Generate the complete Nature Walker presentation collection, preserving shared resource entries."""
import argparse
import hashlib
import json
from pathlib import Path
import icons
import models
import sounds
from catalog import (ASSETS, NS, ABILITIES, BASIC, WHEELS, KEYS, UPGRADES, STATUSES,
                     PARTICLES, SOUNDS, NAMES, SEQUENCES, ABILITY_MODELS, ANIMATED, GROWTHS)


def write_json(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')


def merge(path,entries):
    current=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
    current.update(entries);write_json(path,current)


def generate(root,skip_audio=False):
    icons.generate(root);records=models.generate(root)
    if not skip_audio:merge(root/'sounds.json',sounds.generate(root))
    elif not all((root/f'sounds/nature_walker/{n}.ogg').is_file() for n in SOUNDS):
        raise ValueError('--skip-audio requires a previously generated audio collection')
    lang={f'ability.{NS}.nature_walker.{n}':v for n,v in NAMES.items()}
    lang.update({f'subtitles.{NS}.nature_walker.{n}':v for n,v in sounds.SUBTITLES.items()})
    lang.update({f'pathway.{NS}.nature_walker':'Nature Walker',
                 f'hud.{NS}.nature_walker.growths':'Growths %s/%s',
                 f'hud.{NS}.nature_walker.brood':'Brood %s/%s',
                 f'hud.{NS}.nature_walker.rebirth':'Rebirth %s s',
                 f'hud.{NS}.nature_walker.mutation':'%s Mutation %s/3'})
    merge(root/'lang/en_us.json',lang)
    files=[]
    for folder in ('textures/gui/ability_menus','textures/gui','textures/model_fx','models/model_fx',
                   'textures/particle/fx','sounds'):
        files.extend(p for p in (root/folder/'nature_walker').rglob('*') if p.is_file())
    files.append(root/'textures/gui/pathway_icons/nature_walker.png')
    manifest={'schema_version':1,'namespace':NS,'pathway':'Nature Walker','source_design':'docs/NATURE_WALKER.md',
              'scope':'Presentation assets and preview FX only; gameplay and balance are not implemented.',
              'abilities':ABILITIES,'sequences':SEQUENCES,'basic':BASIC,
              'wheels':{'nature_walker_'+k:v for k,v in WHEELS.items()},
              'key_icons':dict(enumerate(KEYS,1)),'upgrade_icons':UPGRADES,'status_icons':STATUSES,
              'particles':PARTICLES,'sounds':SOUNDS,'ability_models':ABILITY_MODELS,'models':records,
              'growths':{k:{s:k+'_'+s for s in stages} for k,stages in GROWTHS.items()},
              'animations':{n:{'frames':[f'{n}_{i}' for i in range(4)],'pivot_pixels':[8,0,8]} for n in ANIMATED},
              'texture_animations':{'cycle_atlas':{'frames':4,'size':[64,64],'frametime':4}},
              'shared_files':{'sounds.json':['nature_walker.'+n for n in SOUNDS],'lang/en_us.json':list(lang)},
              'files':{p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(files)}}
    write_json(root/'nature_walker/asset_manifest.json',manifest)
    print(f'Generated {len(files)} resources: {len(ABILITIES)} abilities, {len(records)} models, {len(SOUNDS)} sounds.')


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--assets',type=Path,default=ASSETS)
    p.add_argument('--skip-audio',action='store_true');a=p.parse_args();generate(a.assets.resolve(),a.skip_audio)
