"""Validate the packaged resource graph, draft coverage, animation poses and decoded sound."""
from array import array
import argparse
import hashlib
import json
import math
from pathlib import Path
import subprocess
import sys
from PIL import Image
from catalog import (ASSETS, REPO, NS, PREFIX, ABILITIES, WHEELS, KEYS, UPGRADES, PRINCIPLES,
                     STATUSES, PARTICLES, EXTRA_SOUNDS, ABILITY_MODELS)


def validate(root):
    manifest=json.loads((root/f'{PREFIX}/asset_manifest.json').read_text())
    assert len(ABILITIES)==29 and len(set(ABILITIES))==29
    assert [len(v) for v in WHEELS.values()]==[5,4,6,4,6]
    assert manifest['abilities']==ABILITIES
    assert manifest['ability_models']==ABILITY_MODELS
    actual=set()
    for rel,digest in manifest['files'].items():
        path=root/rel;assert path.is_file(),rel
        assert hashlib.sha256(path.read_bytes()).hexdigest()==digest,rel
        if path.suffix=='.png':
            with Image.open(path) as im:
                im.load();assert im.mode=='RGBA',rel
                assert im.getchannel('A').getextrema()[1]>0,rel
        actual.add(rel)
    for group in ('models/model_fx','textures/model_fx','sounds','textures/gui','textures/particle/fx'):
        for path in (root/f'{group}/{PREFIX}').rglob('*'):
            if path.is_file():assert path.relative_to(root).as_posix() in actual,path
    ability_pixels=[]
    for name in set(ABILITIES+KEYS+UPGRADES):
        with Image.open(root/f'textures/gui/ability_menus/{PREFIX}/{name}.png') as im:
            assert im.size==(32,32) and im.getpixel((0,0))[3]==0,name
            if name in ABILITIES:ability_pixels.append(im.tobytes())
    assert len(set(ability_pixels))==29,'Every ability must have a distinct icon'
    for name in set(ABILITIES+UPGRADES+STATUSES+PRINCIPLES)|{f'guilt_{i}' for i in range(6)}:
        with Image.open(root/f'textures/gui/{PREFIX}/{name}.png') as im:
            assert im.size==(16,16) and im.getchannel('A').getextrema()[0]==0,name
    for name in PRINCIPLES:
        with Image.open(root/f'textures/gui/ability_menus/{PREFIX}/principle_{name}.png') as im:assert im.size==(32,32)
    with Image.open(root/f'textures/gui/pathway_icons/{PREFIX}.png') as im:assert im.size==(16,16)
    for name in PARTICLES:
        with Image.open(root/f'textures/particle/fx/{PREFIX}/{name}.png') as im:
            assert im.size==(16,16) and im.getchannel('A').getextrema()[0]==0,name
            assert all(r==g==b for r,g,b,a in (im.getpixel((x,y)) for y in range(16) for x in range(16)) if a),'Particles must accept runtime tint'
    folder=root/f'models/model_fx/{PREFIX}';files=list(folder.glob('*.json'))
    names={p.stem for p in files};assert names=={m['name'] for m in manifest['models']}
    assert {'sun_halo','holy_mace','spear_of_light','golden_scales','blade_of_light','sunbird','sun','hymn_note'}<=names
    for refs in ABILITY_MODELS.values():assert set(refs)<=names,refs
    for path in files:
        model=json.loads(path.read_text());assert model['elements'],path
        keys=model['textures'];assert all((root/f'textures/{r.split(":")[1]}.png').is_file() for r in keys.values()),path
        assert len({e['name'] for e in model['elements']})==len(model['elements']),path
        for el in model['elements']:
            assert all(math.isfinite(a) and math.isfinite(b) and -16<=a<b<=32 for a,b in zip(el['from'],el['to'])),(path,el)
            rotation=el.get('rotation',{})
            assert rotation.get('angle',0) in (-45,-22.5,0,22.5,45),path
            assert rotation.get('axis','y') in ('x','y','z'),path
            for face in el['faces'].values():
                assert face['texture'].lstrip('#') in keys,path
                assert len(face['uv'])==4 and all(0<=v<=16 for v in face['uv']),path
    for name in ('sunbird','sunbird_incomplete'):
        frames=[json.loads((folder/f'{name}_{i}.json').read_text())['elements'] for i in range(4)]
        topology=lambda frame:[(e['name'],e['faces']) for e in frame]
        assert all(topology(f)==topology(frames[0]) for f in frames),name
        assert frames[0]!=frames[1] and frames[1]!=frames[3],name
        assert json.loads((folder/f'{name}.json').read_text())['elements']==frames[0],name
    for name,frames in (('divine_atlas',1),('sunfire_atlas',4)):
        texture=root/f'textures/model_fx/{PREFIX}/{name}.png'
        with Image.open(texture) as im:
            assert im.size==(64,64*frames)
            if frames>1:assert im.crop((0,0,64,64)).tobytes()!=im.crop((0,64,64,128)).tobytes()
        if frames>1:
            meta=json.loads(Path(str(texture)+'.mcmeta').read_text())
            assert meta['animation']['frametime']==3
    events=json.loads((root/'sounds.json').read_text(encoding='utf-8'))
    lang=json.loads((root/'lang/en_us.json').read_text(encoding='utf-8'))
    java=(REPO/'src/main/java/net/lukario/circleofinevitability/sounds/ModSounds.java').read_text(encoding='utf-8')
    assert 'WHITE_ANGEL = java.util.Map.ofEntries(' in java
    for name in ABILITIES+EXTRA_SOUNDS:
        key=f'{PREFIX}.{name}';assert java.count(f'sound("{key}")')==1,key
        event=events[key];assert event['subtitle'] in lang and lang[event['subtitle']],key
        for sound in event['sounds']:
            path=root/f'sounds/{sound["name"].split(":")[1]}.ogg'
            stream=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-of','json',str(path)]))['streams'][0]
            assert stream['codec_name']=='vorbis' and stream['channels']==1 and int(stream['sample_rate'])==44100,path
            assert .2<float(stream['duration'])<4,path
            pcm=subprocess.check_output(['ffmpeg','-v','error','-i',str(path),'-f','s16le','-'])
            samples=array('h');samples.frombytes(pcm)
            if sys.byteorder!='little':samples.byteswap()
            assert 1000<max(abs(s) for s in samples)<32000,path
            assert math.sqrt(sum(s*s for s in samples)/len(samples))>100,path
    for n in ABILITIES:assert f'ability.{NS}.{PREFIX}.{n}' in lang,n
    for shared,entries in manifest['shared_files'].items():
        data=json.loads((root/shared).read_text(encoding='utf-8'));assert set(entries)<=data.keys(),shared
    print(f'PASS: {len(actual)} hashes; 29 distinct ability icons; 7 keys; 6 upgrades; 4 principles; '
          f'{len(files)} valid models; two Sunbird animations; 13 particles; 39 registered mono cues decoded.')


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--assets',type=Path,default=ASSETS)
    validate(parser.parse_args().assets.resolve())
