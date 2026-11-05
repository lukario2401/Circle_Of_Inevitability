"""Validate owned resources, actual model references, animation topology and decoded audio."""
import argparse
import hashlib
import json
import subprocess
from pathlib import Path
import numpy as np
from PIL import Image
from catalog import ASSETS, REPO, ABILITIES, KEYS, STATUSES, PARTICLES, SOUNDS, SEQUENCES, ABILITY_MODELS


def validate(root):
    manifest=json.loads((root/'author/asset_manifest.json').read_text())
    assert manifest['abilities']==ABILITIES and manifest['sequences']==SEQUENCES
    assert manifest['ability_models']==ABILITY_MODELS
    for rel,digest in manifest['files'].items():
        p=root/rel;assert p.is_file(),rel
        assert hashlib.sha256(p.read_bytes()).hexdigest()==digest,rel+' hash mismatch'
    images={}
    for name in dict.fromkeys(ABILITIES+KEYS):
        with Image.open(root/f'textures/gui/ability_menus/author/{name}.png') as im:
            assert im.size==(32,32) and im.mode=='RGBA',name
            assert im.getchannel('A').getextrema()==(0,255),name
            images[name]=hashlib.sha256(im.tobytes()).hexdigest()
    assert len({images[n] for n in ABILITIES})==len(ABILITIES),'Ability icons must be distinct'
    for name in dict.fromkeys(ABILITIES+STATUSES):
        with Image.open(root/f'textures/gui/author/{name}.png') as im:
            assert im.size==(16,16) and im.mode=='RGBA',name
            assert im.getchannel('A').getextrema()==(0,255),name
    for i in range(6):
        with Image.open(root/f'textures/gui/author/insight_{i}.png') as im:assert im.size==(32,8)
    with Image.open(root/'textures/gui/pathway_icons/author.png') as im:assert im.size==(16,16)
    for name in PARTICLES:
        with Image.open(root/f'textures/particle/fx/author/{name}.png') as im:
            assert im.size==(16,16) and im.mode=='RGBA'
            pixels=np.asarray(im);assert np.array_equal(pixels[:,:,0],pixels[:,:,1]) and np.array_equal(pixels[:,:,1],pixels[:,:,2])
            assert im.getchannel('A').getextrema()==(0,255)
    models={}
    for record in manifest['models']:
        name=record['name'];model=json.loads((root/f'models/model_fx/author/{name}.json').read_text());models[name]=model
        assert len(model['elements'])==record['elements'] and model['elements']
        for ref in model['textures'].values():assert (root/f'textures/{ref.split(":")[1]}.png').is_file(),ref
        names=[]
        for el in model['elements']:
            names.append(el['name'])
            assert all(-16<=el['from'][i]<el['to'][i]<=32 for i in range(3)),(name,el)
            rot=el.get('rotation')
            if rot:assert rot['angle'] in (-45,-22.5,0,22.5,45) and rot['axis'] in 'xyz',name
            for face in el['faces'].values():
                assert face['texture'][1:] in model['textures'] and len(face['uv'])==4
                assert all(0<=v<=16 for v in face['uv'])
        assert len(names)==len(set(names)),name+' duplicate elements'
        actual=[[min(e['from'][i] for e in model['elements']) for i in range(3)],
                [max(e['to'][i] for e in model['elements']) for i in range(3)]]
        assert actual==record['bounds'],name+' bounds'
    for ability,names in ABILITY_MODELS.items():
        assert all(n in models for n in names),ability
    for base,animation in manifest['animations'].items():
        def topology(m):return [(e['name'],e['faces'],e.get('rotation')) for e in m['elements']]
        for name in animation['frames']:assert topology(models[name])==topology(models[base]),name
        assert len({json.dumps(models[n]['elements'],sort_keys=True) for n in animation['frames']})>=3,base
    with Image.open(root/'textures/model_fx/author/dream_atlas.png') as im:
        assert im.size==(64,256)
        assert len({im.crop((0,f*64,64,(f+1)*64)).tobytes() for f in range(4)})==4
    assert json.loads((root/'textures/model_fx/author/dream_atlas.png.mcmeta').read_text())['animation']['frametime']==4
    events=json.loads((root/'sounds.json').read_text());lang=json.loads((root/'lang/en_us.json').read_text())
    java=(REPO/'src/main/java/net/lukario/circleofinevitability/sounds/ModSounds.java').read_text()
    decoded=[]
    for name in SOUNDS:
        assert f'sound("author.{name}")' in java,name+' Java sound registration'
        event=events['author.'+name];assert event['subtitle'] in lang
        assert event['sounds']==[{'name':f'circle_of_inevitability:author/{name}'}]
        path=root/f'sounds/author/{name}.ogg'
        info=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-of','json',str(path)]))['streams'][0]
        assert info['codec_name']=='vorbis' and info['channels']==1 and int(info['sample_rate'])==44100,name
        data=subprocess.check_output(['ffmpeg','-v','error','-i',str(path),'-f','f32le','-acodec','pcm_f32le','pipe:1'])
        wave=np.frombuffer(data,dtype='<f4');assert np.isfinite(wave).all()
        assert .4<len(wave)/44100<3 and .01<float(np.max(np.abs(wave)))<.99,name
        assert float(np.sqrt(np.mean(wave*wave)))>.005,name+' silent cue'
        decoded.append(hashlib.sha256(data).hexdigest())
    assert len(set(decoded))==len(SOUNDS),'Duplicated audio'
    for rel,keys in manifest['shared_files'].items():
        data=json.loads((root/rel).read_text());assert all(k in data for k in keys),rel
    doc=(REPO/'docs/AUTHOR.md').read_text()
    assert all('`'+n+'`' in doc for n in ABILITIES),'Undocumented ability'
    print(f'PASS: {len(manifest["files"])} hashes; 26 distinct ability icons; {len(models)} legal models; '
          f'3 compatible animations; {len(SOUNDS)} registered, decoded mono cues; design coverage.')


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--assets',type=Path,default=ASSETS)
    validate(parser.parse_args().assets.resolve())
