"""Build all White Angel resources. Requires Pillow, numpy, ffmpeg and ffprobe.

python3 tools/model_gen/white_angel/generate.py [--assets DIRECTORY] [--skip-audio]
Assets are original deterministic geometry/pixel art/synthesis. Audio container serials may vary.
Only this pathway's entries are merged into shared sound/translation JSON files.
"""
import argparse
import hashlib
import json
from pathlib import Path
from catalog import (ASSETS, NS, PREFIX, ABILITIES, BASIC, WHEELS, KEYS, UPGRADES, PRINCIPLES,
                     STATUSES, PARTICLES, EXTRA_SOUNDS, NAMES, ABILITY_MODELS)
import icons
import models
import sounds


def write_json(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')


def merge(path,entries):
    data=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
    data.update(entries);write_json(path,data)


def generate(root,skip_audio=False):
    icons.generate(root)
    records=models.generate(root)
    if not skip_audio:
        merge(root/'sounds.json',sounds.generate(root))
    lang={f'subtitles.{NS}.{PREFIX}.{n}':text for n,text in sounds.SUBTITLES.items()}
    lang.update({f'ability.{NS}.{PREFIX}.{n}':name for n,name in NAMES.items()})
    lang.update({f'hud.{NS}.{PREFIX}.pacts':'Pacts %s/%s',f'hud.{NS}.{PREFIX}.believers':'Believers %s/%s',
                 f'hud.{NS}.{PREFIX}.guilt':'Guilt %s',f'pathway.{NS}.{PREFIX}':'White Angel'})
    merge(root/'lang/en_us.json',lang)
    paths=[]
    for folder in (f'textures/gui/ability_menus/{PREFIX}',f'textures/gui/{PREFIX}',
                   f'textures/model_fx/{PREFIX}',f'models/model_fx/{PREFIX}',
                   f'textures/particle/fx/{PREFIX}',f'sounds/{PREFIX}'):
        paths.extend(p for p in (root/folder).rglob('*') if p.is_file())
    paths.append(root/f'textures/gui/pathway_icons/{PREFIX}.png')
    manifest={
        'schema_version':1,'namespace':NS,'pathway':'White Angel','source':'WHITE_ANGEL.md design draft',
        'scope':'Assets and sound event registration; abilities and WhiteAngelFx are not implemented here.',
        'abilities':ABILITIES,'plain_key_abilities':BASIC,
        'wheels':{f'{PREFIX}_{k}':v for k,v in WHEELS.items()},
        'key_icons':{str(i+1):n for i,n in enumerate(KEYS)},'upgrade_icons':UPGRADES,
        'principle_icons':PRINCIPLES,'status_icons':STATUSES,
        'particles':PARTICLES,'sounds':ABILITIES+EXTRA_SOUNDS,
        'ability_models':ABILITY_MODELS,'models':records,
        'animations':{n:{'frames':[f'{n}_{i}' for i in range(4)],'rest':n,'axis':'wings change height'}
                      for n in ('sunbird','sunbird_incomplete')},
        'texture_animations':{'sunfire_atlas':{'frames':4,'size':[64,64],'frametime':3}},
        'rendering':{'model_space':'16 pixels = 1 block; ModelFx centers X/Z at 8',
                     'material':'solid opaque ivory, gold, feather and sunfire; enable glow/unshaded at runtime',
                     'particle_ids':f'{NS}:fx/{PREFIX}/<name>',
                     'model_ids':f'{NS}:model_fx/{PREFIX}/<name>',
                     'sound_ids':f'{NS}:{PREFIX}.<name>'},
        'shared_files':{'sounds.json':list(f'{PREFIX}.{n}' for n in ABILITIES+EXTRA_SOUNDS),
                        'lang/en_us.json':list(lang)},
        'files':{p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(paths)},
    }
    write_json(root/f'{PREFIX}/asset_manifest.json',manifest)
    print(f'Generated {len(paths)} files, {len(records)} models, {len(ABILITIES)} abilities and '
          f'{len(ABILITIES+EXTRA_SOUNDS)} mono sound cues in {root}')
    return manifest


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--assets',type=Path,default=ASSETS)
    parser.add_argument('--skip-audio',action='store_true',help='Keep existing cues; for pixel/geometry iteration only.')
    args=parser.parse_args();generate(args.assets.resolve(),args.skip_audio)
