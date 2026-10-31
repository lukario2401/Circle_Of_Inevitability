"""Validate the packaged resource graph, morph topology, images and decoded audio.
Run from any working directory: python tools/model_gen/omniscient_eye/validate.py
"""
import hashlib
import json
import math
from pathlib import Path
import subprocess
from array import array
import sys
from PIL import Image
from generate import ABILITIES, BASIC, WHEELS, STATUSES, PARTICLES, EXTRA_SOUNDS, DEFAULT_ASSETS, REPO, PREFIX


def main():
    root=DEFAULT_ASSETS
    data=json.loads((root/f'{PREFIX}/asset_manifest.json').read_text(encoding='utf-8'))
    assert len(ABILITIES)==26 and len(set(ABILITIES))==26
    assert len(BASIC)==8 and [len(v) for v in WHEELS.values()]==[5,5,8]
    assert set(data['ability_models'])==set(ABILITIES)
    assert set(data['abilities'])==set(ABILITIES)
    for rel,digest in data['files'].items():
        path=root/rel
        assert path.is_file(),rel
        assert hashlib.sha256(path.read_bytes()).hexdigest()==digest,rel
        if path.suffix=='.png':
            with Image.open(path) as im:
                assert im.mode=='RGBA',rel
                assert im.getchannel('A').getextrema()[1]>0,rel
                im.load()
    for name in ABILITIES:
        for sub,size in [('ability_menus/'+PREFIX,32),(PREFIX,16)]:
            with Image.open(root/f'textures/gui/{sub}/{name}.png') as im: assert im.size==(size,size),name
    # Wheel entries must remain distinguishable; no duplicate placeholder images.
    wheel_images=[(root/f'textures/gui/ability_menus/{PREFIX}/{n}.png').read_bytes() for n in sum(WHEELS.values(),[])]
    assert len(set(wheel_images))==18
    for name in STATUSES:
        with Image.open(root/f'textures/gui/{PREFIX}/{name}.png') as im: assert im.size==(16,16)
    for name in PARTICLES:
        with Image.open(root/f'textures/particle/fx/{PREFIX}/{name}.png') as im:
            assert im.size==(16,16) and im.getchannel('A').getextrema()[0]==0
    folder=root/f'models/model_fx/{PREFIX}'
    files=list(folder.glob('*.json')); assert len(files)==19
    names={p.stem for p in files}
    assert {m['name'] for m in data['models']}==names
    for refs in data['ability_models'].values(): assert set(refs)<=names
    for path in files:
        model=json.loads(path.read_text())
        assert model['elements'],path
        keys=model['textures']
        for ref in keys.values(): assert (root/('textures/'+ref.split(':')[1]+'.png')).is_file(),ref
        for el in model['elements']:
            assert all(math.isfinite(a) and math.isfinite(b) and -16<=a<b<=32 for a,b in zip(el['from'],el['to'])),path
            rotation=el.get('rotation',{})
            assert rotation.get('angle',0) in (-45,-22.5,0,22.5,45),path
            for face in el['faces'].values():
                assert face['texture'].lstrip('#') in keys,path
                assert len(face['uv'])==4 and all(0<=v<=16 for v in face['uv']),path
    frames=[json.loads((folder/f'tome_creature_{i}.json').read_text())['elements'] for i in range(4)]
    def topology(elements):return [(e['name'],e['faces']) for e in elements]
    assert all(topology(f)==topology(frames[0]) for f in frames)
    assert frames[0]!=frames[1] and frames[1]!=frames[3]
    texture=root/f'textures/model_fx/{PREFIX}/ink_pool.png'
    with Image.open(texture) as im: assert im.size==(64,256)
    meta=json.loads(Path(str(texture)+'.mcmeta').read_text())
    assert meta['animation']['frametime']==3
    events=json.loads((root/'sounds.json').read_text(encoding='utf-8'))
    lang=json.loads((root/'lang/en_us.json').read_text(encoding='utf-8'))
    java=(REPO/'src/main/java/net/lukario/circleofinevitability/sounds/ModSounds.java').read_text(encoding='utf-8')
    for name in ABILITIES+EXTRA_SOUNDS:
        key=f'{PREFIX}.{name}'
        assert f'sound("{key}")' in java,key
        event=events[key]; assert event['subtitle'] in lang,key
        for sound in event['sounds']:
            path=root/('sounds/'+sound['name'].split(':')[1]+'.ogg')
            stream=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-of','json',str(path)]))['streams'][0]
            assert stream['codec_name']=='vorbis' and stream['channels']==1,path
            assert 0.2<float(stream['duration'])<5,path
            pcm=subprocess.check_output(['ffmpeg','-v','error','-i',str(path),'-f','s16le','-'])
            samples=array('h'); samples.frombytes(pcm)
            if sys.byteorder!='little':samples.byteswap()
            peak=max(abs(s) for s in samples)
            assert 1000<peak<32767,(path,peak)
    print(f'PASS: {len(data["files"])} asset hashes, 19 model graphs, 26 ability icons, 44 HUD icons, '
          '18 unique wheel icons, four compatible creature poses, 13 particles and 31 registered, decodable mono cues.')


if __name__=='__main__':main()
