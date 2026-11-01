"""Validate the actual packaged assets, animation topology and decoded audio.
Also guards the silent/invisible casts and preservation of other pathways' resources.
"""
import hashlib
import json
import math
import subprocess
from pathlib import Path
import numpy as np
from PIL import Image
from generate import ASSETS, REPO, PREFIX, ABILITIES, WHEELS, BASIC, UPGRADES, STATUSES, PARTICLES, SOUNDS, SILENT


def main():
    root=ASSETS;data=json.loads((root/f'{PREFIX}/asset_manifest.json').read_text())
    assert len(ABILITIES)==48 and len(set(ABILITIES))==48 and len(BASIC)==4
    assert [len(v) for v in WHEELS.values()]==[9,9,9,9,8]
    assert set(data['abilities'])==set(ABILITIES+UPGRADES)
    for rel,digest in data['files'].items():
        p=root/rel;assert p.is_file(),rel
        assert hashlib.sha256(p.read_bytes()).hexdigest()==digest,rel
        if p.suffix=='.png':
            with Image.open(p) as im:
                im.load();assert im.mode=='RGBA',rel
                assert im.getchannel('A').getextrema()[1]>0,rel
        if p.name.endswith('.mcmeta'):
            meta=json.loads(p.read_text())['animation']
            with Image.open(str(p)[:-7]) as im:
                assert im.height==im.width*4,(p,im.size)
            assert meta['frametime']==3 and meta['interpolate'] is True,p
    for n in ABILITIES+UPGRADES:
        for folder,size in [('ability_menus/'+PREFIX,32),(PREFIX,16)]:
            with Image.open(root/f'textures/gui/{folder}/{n}.png') as im:assert im.size==(size,size),n
    images=[(root/f'textures/gui/ability_menus/{PREFIX}/{n}.png').read_bytes() for n in sum(WHEELS.values(),[])]
    assert len(set(images))==44,'Wheel icons must have distinct pixels'
    for n in STATUSES:
        with Image.open(root/f'textures/gui/{PREFIX}/{n}.png') as im:assert im.size==(16,16)
    with Image.open(root/f'textures/gui/pathway_icons/{PREFIX}.png') as im:assert im.size==(16,16)
    for n in PARTICLES:
        with Image.open(root/f'textures/particle/fx/{PREFIX}/{n}.png') as im:
            assert im.size==(16,16) and im.getchannel('A').getextrema()[0]==0,n
    folder=root/f'models/model_fx/{PREFIX}';paths=list(folder.glob('*.json'));assert len(paths)==27
    names={p.stem for p in paths};assert names=={m['name'] for m in data['models']}
    for n,ability in data['abilities'].items():assert set(ability['models'])<=names,n
    for p in paths:
        m=json.loads(p.read_text());assert m['elements'],p
        for ref in m['textures'].values():assert (root/('textures/'+ref.split(':')[1]+'.png')).is_file(),ref
        if p.stem in ('water_sphere','kingdom_chasm','kingdom_dreadful'):
            for axis in range(3):
                extent=max(e['to'][axis] for e in m['elements'])-min(e['from'][axis] for e in m['elements'])
                assert abs(extent-24)<1e-6,(p,'diameter',extent)
        for e in m['elements']:
            assert all(math.isfinite(a) and math.isfinite(b) and -16<=a<b<=32 for a,b in zip(e['from'],e['to'])),(p,e['name'])
            assert e.get('rotation',{}).get('angle',0) in (-45,-22.5,0,22.5,45),p
            for f in e['faces'].values():
                assert f['texture'].lstrip('#') in m['textures'],p
                assert len(f['uv'])==4 and all(0<=v<=16 for v in f['uv']),p
    frames=[json.loads((folder/f'wave_crest_{i}.json').read_text())['elements'] for i in range(4)]
    def topology(f):return [(e['name'],e['faces'],e.get('rotation')) for e in f]
    assert all(topology(f)==topology(frames[0]) for f in frames)
    assert all(frames[i]!=frames[(i+1)%4] for i in range(4))
    events=json.loads((root/'sounds.json').read_text());lang=json.loads((root/'lang/en_us.json').read_text())
    java=(REPO/'src/main/java/net/lukario/circleofinevitability/sounds/ModSounds.java').read_text()
    fx=(REPO/'src/main/java/net/lukario/circleofinevitability/blasphemy_slate/beyonder_characteristics/thunder_god/ThunderGodFx.java').read_text()
    for n in ABILITIES+UPGRADES:assert f'"{n}"' in fx,n
    assert 'LightningBolt' not in fx.replace('Never spawns a LightningBolt entity.','')
    for n in SILENT:
        assert f'{PREFIX}.{n}' not in events and data['abilities'][n]['sound'] is None
        assert data['abilities'][n]['models']==[]
    for n in SOUNDS:
        key=f'{PREFIX}.{n}';assert f'sound("{key}")' in java,n
        event=events[key];assert event['subtitle'] in lang,n
        for sound in event['sounds']:
            p=root/('sounds/'+sound['name'].split(':')[1]+'.ogg')
            stream=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-of','json',str(p)]))['streams'][0]
            assert stream['codec_name']=='vorbis' and stream['channels']==1 and stream['sample_rate']=='22050',p
            assert .2<float(stream['duration'])<4,p
            pcm=subprocess.check_output(['ffmpeg','-v','error','-i',str(p),'-f','f32le','-'])
            samples=np.frombuffer(pcm,dtype='<f4');peak=float(np.max(np.abs(samples)))
            assert .02<float(np.sqrt(np.mean(samples*samples))) and .2<peak<.99,(p,peak)
            if n.endswith('_loop'):assert abs(float(samples[0]-samples[-1]))<.12,(p,'audible loop seam')
    # Existing events/translations must survive generation. Ignore this pathway on later commits.
    for rel in ('sounds.json','lang/en_us.json'):
        old=json.loads(subprocess.check_output(['git','show',f'HEAD:src/main/resources/assets/{NS_PATH}/{rel}'],cwd=REPO))
        new=json.loads((root/rel).read_text())
        for key,val in old.items():
            if PREFIX not in key:assert new.get(key)==val,('unrelated resource changed',rel,key)
    print(f'PASS: {len(data["files"])} hashes; 48 abilities + 5 upgrades; 44 unique wheel icons; 27 legal model graphs; '
          f'4 compatible wave poses; 17 particles; {len(SOUNDS)} registered, subtitled, decodable mono cues; silent casts preserved.')


NS_PATH='circle_of_inevitability'
if __name__=='__main__':main()
