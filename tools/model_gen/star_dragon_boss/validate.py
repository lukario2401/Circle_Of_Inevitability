"""Validate the boss's actual asset contract: topology, box unwraps, texture coverage and animation tracks."""
from pathlib import Path
import json,math
from PIL import Image
ROOT=Path(__file__).resolve().parents[3]
A=ROOT/'src/main/resources/assets/circle_of_inevitability'
rig=json.loads((A/'boss/star_dragon/star_dragon.rig.json').read_text())
clips=json.loads((A/'boss/star_dragon/star_dragon.animations.json').read_text())['clips']
names=set();occupied=set()
for b in rig['bones']:
    assert b['name'] not in names,b['name']
    assert b['parent'] is None or b['parent'] in names, 'Parent must precede child'
    names.add(b['name'])
    assert all(math.isfinite(v) for v in b['pivot']+b['rotation'])
    for c in b['cubes']:
        assert all(math.isfinite(v) for v in c['origin']+c['size'])
        assert all(v>0 for v in c['size'])
        u,v=c['uv'];w,h,d=[math.ceil(n) for n in c['size']]
        pixels={(x,y) for x in range(u,u+2*(w+d)) for y in range(v,v+d+h)}
        assert not occupied & pixels,'Overlapping box unwraps'
        occupied|=pixels
        assert u>=0 and v>=0 and u+2*(w+d)<=rig['texture_size'][0] and v+d+h<=rig['texture_size'][1]
for form in ('normal','radiant'):
    im=Image.open(A/f'textures/entity/star_dragon_boss/{form}.png').convert('RGBA')
    em=Image.open(A/f'textures/entity/star_dragon_boss/{form}_emissive.png').convert('RGBA')
    assert list(im.size)==rig['texture_size'] and em.size==im.size
    assert all(im.getpixel(p)[3]==255 for p in occupied),'Missing texels'
    assert any(em.getpixel(p)[3] for p in occupied),'Missing emissive mask'
for name,c in clips.items():
    assert c['seconds']>0
    for b,channels in c['bones'].items():
        assert b in names, 'Animated bone missing from rig'
        for ch,keys in channels.items():
            assert ch in ('rotation','position','scale')
            times=[k[0] for k in keys]
            assert times==sorted(set(times)) and times[0]==0 and times[-1]==1
            assert all(len(k)==4 and all(math.isfinite(x) for x in k) for k in keys)
            if ch=='scale':assert all(x>0 for k in keys for x in k[1:])
assert {a['name'] for a in json.loads((A/'boss/star_dragon/star_dragon.geo.json').read_text())['minecraft:geometry'][0]['bones']}==names
assert len([n for n in names if n.startswith('angel_') and not n.endswith('_tip')])==10
settings=json.loads((ROOT/'src/main/resources/data/circle_of_inevitability/bosses/star_dragon.json').read_text())
assert len(settings['attacks'])==20
assert all(a['animation'] in clips for a in settings['attacks'].values())
condenser=json.loads((ROOT/'src/main/resources/data/circle_of_inevitability/bosses/star_dragon_condenser.json').read_text())
assert len(condenser['attacks'])==26
assert settings['attacks'].keys() <= condenser['attacks'].keys()
for a in condenser['attacks'].values():
    assert a['animation'] in clips
    assert all(id in condenser['attacks'] for id in a.get('followups',[]))
    assert 0<a['windup_ticks']<=32 and 0<=a['recovery_ticks']<=8
print(f'Validated {len(names)} bones, {len(clips)} clips, non-overlapping UVs and four PNG maps; all 20 Sequence 2 and 26 Sequence 0 attacks have animation clips and valid chains.')
