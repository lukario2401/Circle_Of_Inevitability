"""Validate generated resource links, geometry, sound registration and decodable mono audio."""
import json
from pathlib import Path
import re
import subprocess
from PIL import Image
from generate import ABILITIES, EXTRAS

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT/'src/main/resources/assets/circle_of_inevitability'
JAVA = ROOT/'src/main/java/net/lukario/circleofinevitability'

def main():
    for name in ABILITIES+EXTRAS:
        for folder,size in [('ability_menus/god_of_decay',32),('god_of_decay',16)]:
            with Image.open(ASSETS/f'textures/gui/{folder}/{name}.png') as im:
                assert im.size==(size,size), name
                assert im.mode=='RGBA', name
    models=list((ASSETS/'models/model_fx/god_of_decay').glob('*.json'))
    assert len(models)==12
    for p in models:
        model=json.loads(p.read_text())
        for ref in model['textures'].values():
            assert (ASSETS/'textures'/ (ref.split(':')[1]+'.png')).exists(), ref
        for el in model['elements']:
            assert all(-16<=a<=b<=32 for a,b in zip(el['from'],el['to'])), p
            assert el.get('rotation',{}).get('angle',0) in (-45,-22.5,0,22.5,45), p
            for face in el['faces'].values():
                assert face['texture']=='#0'
                assert all(0<=v<=16 for v in face['uv']), p
    fx=(JAVA/'blasphemy_slate/beyonder_characteristics/god_of_decay/GodOfDecayFx.java').read_text()
    for name in re.findall(r'model\("([a-z_]+)"',fx):
        assert (ASSETS/f'models/model_fx/god_of_decay/{name}.json').exists(), name
    for name in re.findall(r'ParticleFx.of\("([a-z_/]+)"',fx):
        assert (ASSETS/f'textures/particle/{name}.png').exists(), name
    events=json.loads((ASSETS/'sounds.json').read_text())
    lang=json.loads((ASSETS/'lang/en_us.json').read_text())
    registered=(JAVA/'sounds/ModSounds.java').read_text()
    for name in ABILITIES+EXTRAS:
        if name in ('incubation','sickness'): continue
        key='god_of_decay.'+name
        assert 'sound("'+key+'")' in registered, name
        event=events[key]
        assert event['subtitle'] in lang, name
        for ref in event['sounds']:
            p=ASSETS/'sounds'/(ref['name'].split(':')[1]+'.ogg')
            metadata=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-of','json',str(p)]))
            stream=metadata['streams'][0]
            assert stream['channels']==1 and stream['codec_name']=='vorbis', p
            assert float(stream['duration'])>0, p
            subprocess.run(['ffmpeg','-v','error','-i',str(p),'-f','null','-'],check=True)
    assert 'god_of_decay.incubation' not in events
    assert len(ABILITIES)==26
    for name in ABILITIES+EXTRAS:
        assert '"'+name+'"' in fx, name
    print('PASS: 26 abilities, 12 models, 70 glyphs, 3 particles, 33 registered mono sounds; resource links and audio decode clean.')

if __name__=='__main__': main()
