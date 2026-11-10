"""Verify design coverage, resource graph, animation compatibility, Java IDs and decoded audio."""
import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path
import numpy as np
from PIL import Image
from catalog import (ASSETS, REPO, ABILITIES, BASIC, WHEELS, KEYS, UPGRADES, STATUSES,
                     PARTICLES, SOUNDS, SEQUENCES, ABILITY_MODELS, ANIMATED, GROWTHS, NS)


def validate(root):
    manifest=json.loads((root/'nature_walker/asset_manifest.json').read_text())
    assert len(ABILITIES)==39 and len(set(ABILITIES))==39
    assert manifest['abilities']==ABILITIES and manifest['sequences']==SEQUENCES
    assert manifest['ability_models']==ABILITY_MODELS
    assert manifest['wheels']=={'nature_walker_'+k:v for k,v in WHEELS.items()}
    for rel,digest in manifest['files'].items():
        p=root/rel;assert p.is_file(),rel
        assert hashlib.sha256(p.read_bytes()).hexdigest()==digest,rel+' hash mismatch'
    images={}
    def rgba(rel,size):
        with Image.open(root/rel) as im:
            assert im.size==size and im.mode=='RGBA',rel
            assert im.getchannel('A').getextrema()==(0,255),rel
            return im.copy()
    for name in dict.fromkeys(ABILITIES+KEYS+UPGRADES):
        im=rgba(f'textures/gui/ability_menus/nature_walker/{name}.png',(32,32))
        images[name]=hashlib.sha256(im.tobytes()).hexdigest()
    assert len({images[n] for n in ABILITIES})==39,'Ability icons must be distinct'
    for n in dict.fromkeys(ABILITIES+UPGRADES+STATUSES):rgba(f'textures/gui/nature_walker/{n}.png',(16,16))
    for kind in ('organs','fungus','wheat','vines'):
        states=[rgba(f'textures/gui/nature_walker/mutation_{kind}_{i}.png',(16,16)).tobytes() for i in range(1,4)]
        assert len(set(states))==3,'Mutation levels must be distinguishable'
    rgba('textures/gui/pathway_icons/nature_walker.png',(16,16))
    for n in PARTICLES:
        im=rgba(f'textures/particle/fx/nature_walker/{n}.png',(16,16));pixels=np.asarray(im)
        assert np.array_equal(pixels[:,:,0],pixels[:,:,1]) and np.array_equal(pixels[:,:,1],pixels[:,:,2]),n
    models={}
    for record in manifest['models']:
        n=record['name'];m=json.loads((root/f'models/model_fx/nature_walker/{n}.json').read_text());models[n]=m
        assert len(m['elements'])==record['elements'] and m['elements'],n
        assert record['pivot_pixels']==[8,0,8],n
        for ref in m['textures'].values():assert (root/f'textures/{ref.split(":")[1]}.png').is_file(),ref
        names=[]
        for el in m['elements']:
            names.append(el['name'])
            assert all(-16<=el['from'][i]<el['to'][i]<=32 for i in range(3)),(n,el)
            rot=el.get('rotation')
            if rot:assert rot['angle'] in (-45,-22.5,0,22.5,45) and rot['axis'] in 'xyz',n
            for face in el['faces'].values():
                assert face['texture'][1:] in m['textures'] and len(face['uv'])==4
                assert all(0<=v<=16 for v in face['uv'])
        assert len(names)==len(set(names)),n+' duplicate elements'
        bounds=[[min(e['from'][i] for e in m['elements']) for i in range(3)],
                [max(e['to'][i] for e in m['elements']) for i in range(3)]]
        assert bounds==record['bounds'],n+' bounds'
    for ability,names in ABILITY_MODELS.items():assert all(n in models for n in names),ability
    for kind,stages in GROWTHS.items():
        assert manifest['growths'][kind]=={s:kind+'_'+s for s in stages}
        assert all(kind+'_'+s in models for s in stages)
        assert len({json.dumps(models[kind+'_'+s]['elements'],sort_keys=True) for s in stages})==len(stages)
    for base in ANIMATED:
        frames=manifest['animations'][base]['frames']
        assert frames==[f'{base}_{i}' for i in range(4)]
        def topology(m):return [(e['name'],e['faces'],e.get('rotation')) for e in m['elements']]
        for n in frames:assert topology(models[n])==topology(models[base]),n+' topology'
        assert len({json.dumps(models[n]['elements'],sort_keys=True) for n in frames})>=3,base+' stationary'
    with Image.open(root/'textures/model_fx/nature_walker/cycle_atlas.png') as im:
        assert im.size==(64,256)
        assert len({im.crop((0,f*64,64,(f+1)*64)).tobytes() for f in range(4)})==4
    assert json.loads((root/'textures/model_fx/nature_walker/cycle_atlas.png.mcmeta').read_text())['animation']['frametime']==4
    fx=(REPO/'src/main/java/net/lukario/circleofinevitability/blasphemy_slate/beyonder_characteristics/nature_walker/NatureWalkerFx.java').read_text()
    enum=fx.split('public enum Ability {',1)[1].split('private final List',1)[0]
    for ability,refs in ABILITY_MODELS.items():
        match=re.search(r'\b'+ability.upper()+r'\(([^)]+)\)',enum)
        assert match and re.findall(r'"([^"]+)"',match[1])==refs,ability+' Java model mapping'
    listed=fx.split('public static final List<String> MODELS = List.of(',1)[1].split(');',1)[0]
    assert set(re.findall(r'"([^"]+)"',listed))==set(models),'Java model preview list'
    assert all(f'"{n}"' in fx for n in PARTICLES),'Missing particle preview'
    events=json.loads((root/'sounds.json').read_text());lang=json.loads((root/'lang/en_us.json').read_text())
    java=(REPO/'src/main/java/net/lukario/circleofinevitability/sounds/ModSounds.java').read_text()
    decoded=[]
    for n in SOUNDS:
        assert f'sound("nature_walker.{n}")' in java,n+' registration'
        event=events['nature_walker.'+n];assert event['subtitle'] in lang
        assert event['sounds']==[{'name':f'{NS}:nature_walker/{n}'}]
        path=root/f'sounds/nature_walker/{n}.ogg'
        info=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-of','json',str(path)]))['streams'][0]
        assert info['codec_name']=='vorbis' and info['channels']==1 and int(info['sample_rate'])==44100,n
        data=subprocess.check_output(['ffmpeg','-v','error','-i',str(path),'-f','f32le','-acodec','pcm_f32le','pipe:1'])
        wave=np.frombuffer(data,dtype='<f4');assert np.isfinite(wave).all()
        assert .4<len(wave)/44100<3 and .01<float(np.max(np.abs(wave)))<.99,n+' headroom or length'
        assert float(np.sqrt(np.mean(wave*wave)))>.005,n+' silent'
        assert np.max(np.abs(wave[:100]))<.12 and np.max(np.abs(wave[-100:]))<.12,n+' unfaded boundary'
        decoded.append(hashlib.sha256(data).hexdigest())
    assert len(set(decoded))==len(SOUNDS),'Duplicated audio'
    for rel,keys in manifest['shared_files'].items():
        data=json.loads((root/rel).read_text());assert all(k in data for k in keys),rel
    docs=(REPO/'docs/NATURE_WALKER_ASSETS.md').read_text()
    assert all('`'+n+'`' in docs for n in ABILITIES),'Ability missing from coverage table'
    print(f'PASS: {len(manifest["files"])} resource hashes; 39 distinct ability icons; {len(models)} legal models; '
          f'{len(ANIMATED)} compatible animations; {len(SOUNDS)} registered, decoded mono cues; Java and design coverage.')


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--assets',type=Path,default=ASSETS)
    validate(p.parse_args().assets.resolve())
