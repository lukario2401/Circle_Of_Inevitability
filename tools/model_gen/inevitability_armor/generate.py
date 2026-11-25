"""Deterministic native armor rig, palette atlas, 3-D item models, recipes and tags. No dependencies.

Run from any directory: python tools/model_gen/inevitability_armor/generate.py
Geometry uses Minecraft model pixels, Y down. UV regions are named below; edit the rig
with a resource pack, or this source when adding a permanent new silhouette.
"""
import json
import math
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'src/main/resources/assets/circle_of_inevitability'
DATA = ROOT / 'src/main/resources/data/circle_of_inevitability'
NS = 'circle_of_inevitability:'
UV = {'plate': [0, 0], 'flesh': [64, 0], 'silver': [96, 0], 'violet': [64, 32], 'eye': [96, 32]}
COLORS = {'plate': (29, 22, 43), 'flesh': (115, 17, 48), 'silver': (151, 126, 170),
          'violet': (76, 40, 113), 'eye': (239, 74, 121)}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def png(path, width, height, pixels):
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data))
    raw = b''.join(b'\0' + bytes(pixels[y*width*4:(y+1)*width*4]) for y in range(height))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
                     + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


bones = []


def bone(name, parent='root', pivot=(0, 0, 0), rotation=(0, 0, 0)):
    b = dict(name=name, parent=parent, pivot=pivot, rotation=rotation, cubes=[])
    bones.append(b)
    return b


def cube(b, slot, origin, size, material='plate'):
    b['cubes'].append(dict(slot=slot, origin=origin, size=size, uv=UV[material]))


def generate():
    bones.clear()
    head = bone('head')
    bone('hat', 'head')
    body = bone('body')
    arms = [bone('right_arm', pivot=(-5, 2, 0)), bone('left_arm', pivot=(5, 2, 0))]
    legs = [bone('right_leg', pivot=(-1.9, 12, 0)), bone('left_leg', pivot=(1.9, 12, 0))]
    # Mask, crown, pronounced brow, and an eye-shaped sealed gem.
    cube(head, 'head', (-4.6, -8.6, -4.6), (9.2, 9.2, 9.2))
    cube(head, 'head', (-4.9, -7.8, -5.1), (9.8, 1.2, 1), 'silver')
    cube(head, 'head', (-3.7, -5.6, -5.25), (2.4, .6, .5), 'eye')
    cube(head, 'head', (1.3, -5.6, -5.25), (2.4, .6, .5), 'eye')
    cube(head, 'head', (-.6, -6.7, -5.4), (1.2, 5.9, .8), 'violet')
    for side in (-1, 1):
        horn = bone('crown_' + str(side), 'head', (side*3.6, -8, 0), (0, 0, side*.28))
        cube(horn, 'head', (-.8, -5, -1), (1.6, 5, 2), 'violet')
        cube(horn, 'head', (-.35, -7, -.5), (.7, 2.5, 1), 'silver')
    cube(head, 'head', (-.65, -11.5, -3.6), (1.3, 3.5, 1), 'eye')
    # Overlapping breastplate, rib plates and a central red eye in an octagonal silver seal.
    cube(body, 'chest', (-4.7, -.5, -2.7), (9.4, 12.5, 5.4))
    for side in (-1, 1):
        for row in range(3):
            cube(body, 'chest', (side*2.3-1.8, 2+row*2.5, -3.5), (3.6, 1.6, 1.1), 'violet')
    cube(body, 'chest', (-1.9, 2, -4), (3.8, 4, .8), 'silver')
    cube(body, 'chest', (-1.3, 2.6, -4.5), (2.6, 2.8, .7), 'flesh')
    cube(body, 'chest', (-.4, 2.8, -4.9), (.8, 2.4, .5), 'eye')
    for i, arm in enumerate(arms):
        center = -1 if i == 0 else 1
        cube(arm, 'chest', (center-2.55, -2.7, -2.55), (5.1, 12.9, 5.1))
        cube(arm, 'chest', (center-3.1, -3.5, -3.1), (6.2, 3.8, 6.2), 'violet')
        cube(arm, 'chest', (center-3.25, -3.7, -3.25), (6.5, .7, 6.5), 'silver')
        cube(arm, 'chest', (center-2.7, 6.5, -2.8), (5.4, 2.8, 1.1), 'violet')
        for k in range(2):
            spike=bone('pauldron_%d_%d' % (i,k), arm['name'], (center+(k-.5)*2.4,-3.5,0), (0,0,(1 if i==0 else -1)*.35))
            cube(spike, 'chest', (-.55,-3-k,-.6),(1.1,3+k,1.2),'silver')
    # Waist plates deliberately belong to leggings, so each of the four slots has distinct geometry.
    cube(body, 'legs', (-4.5, 9.4, -2.5), (9, 3.2, 5), 'violet')
    cube(body, 'legs', (-1.6, 10, -3), (3.2, 2, .7), 'silver')
    for i, leg in enumerate(legs):
        cube(leg, 'legs', (-2.35, -.1, -2.35), (4.7, 7.9, 4.7))
        cube(leg, 'legs', (-2.6, .5, -2.8), (5.2, 4.8, 1), 'violet')
        cube(leg, 'legs', (-1.6, 5.4, -2.9), (3.2, 2.5, 1.3), 'silver')
        cube(leg, 'legs', (-.55, 5.7, -3.2), (1.1, 1.8, .5), 'eye')
        cube(leg, 'feet', (-2.55, 7.8, -2.6), (5.1, 4.7, 5.2))
        cube(leg, 'feet', (-2.65, 10.2, -3.8), (5.3, 2.5, 6.5), 'violet')
        cube(leg, 'feet', (-2.7, 12, -4), (5.4, .8, 6.9), 'silver')
        for x in (-1.5, 0, 1.5):
            cube(leg, 'feet', (x-.4, 11.1, -5), (.8, 1.2, 2), 'silver')
    write(ASSETS/'armor/inevitability.rig.json', dict(texture_size=[128,64], bones=bones))
    pixels=[]
    for y in range(64):
        for x in range(128):
            mat = 'plate' if x<64 else ('flesh' if y<32 else 'violet') if x<96 else ('silver' if y<32 else 'eye')
            color=COLORS[mat]
            n=((x*17+y*31+x*y*7)%13)-6
            edge = 12 if (x+y)%16==0 else 0
            pixels.extend([max(0,min(255,c+n+edge)) for c in color]+[255])
    for layer in ('humanoid','humanoid_leggings','humanoid_baby'):
        png(ASSETS/f'textures/entity/equipment/{layer}/inevitability.png',128,64,pixels)
    # The block/item atlas only discovers item textures; worn equipment uses direct texture lookup.
    png(ASSETS/'textures/item/inevitability_armor.png',128,64,pixels)
    write(ASSETS/'equipment/inevitability.json', {'layers': {l:[{'texture':NS+'inevitability'}]
          for l in ('humanoid','humanoid_leggings','humanoid_baby')}})

    # Actual volumetric inventory/hand models, derived from the same rig (including crown/shoulder spikes).
    # All cube centers are transformed by the complete bone hierarchy; item format allows one-axis rotations.
    positions={b['name']:b for b in bones}
    def world(p,b):
        x,y,z=p
        rx,ry,rz=b['rotation']
        y,z=y*math.cos(rx)-z*math.sin(rx),y*math.sin(rx)+z*math.cos(rx)
        x,z=x*math.cos(ry)+z*math.sin(ry),-x*math.sin(ry)+z*math.cos(ry)
        x,y=x*math.cos(rz)-y*math.sin(rz),x*math.sin(rz)+y*math.cos(rz)
        p=[x+b['pivot'][0],y+b['pivot'][1],z+b['pivot'][2]]
        return world(p,positions[b['parent']]) if b['parent']!='root' else p
    names={'head':'helmet','chest':'chestplate','legs':'leggings','feet':'boots'}
    for slot,name in names.items():
        shapes=[]
        for b in bones:
            for c in b['cubes']:
                if c['slot']!=slot: continue
                p=world(c['origin'],b); s=c['size']
                shapes.append((p,s,c['uv']))
        lo=[min(p[k] for p,s,uv in shapes) for k in range(3)]
        hi=[max(p[k]+s[k] for p,s,uv in shapes) for k in range(3)]
        scale=13/max(hi[k]-lo[k] for k in range(3)); center=[(lo[k]+hi[k])/2 for k in range(3)]
        elements=[]
        for p,s,uv in shapes:
            a=[8+(p[0]-center[0])*scale,8-(p[1]+s[1]-center[1])*scale,8+(p[2]-center[2])*scale]
            b=[a[k]+s[k]*scale for k in range(3)]
            elements.append({'from':a,'to':b,'faces':{face:{'texture':'#armor','uv':[uv[0]/8,uv[1]/4,(uv[0]+4)/8,(uv[1]+4)/4]}
                             for face in ('north','south','east','west','up','down')}})
        display={'gui':{'rotation':[15,210,0],'translation':[0,0,0],'scale':[.9,.9,.9]},
                 'ground':{'translation':[0,3,0],'scale':[.45,.45,.45]},
                 'fixed':{'rotation':[0,180,0],'scale':[.8,.8,.8]},
                 'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.55,.55,.55]},
                 'firstperson_righthand':{'rotation':[0,200,0],'translation':[1,1,0],'scale':[.65,.65,.65]}}
        item='inevitability_'+name
        write(ASSETS/f'models/item/{item}.json', {'textures':{'armor':NS+'item/inevitability_armor','particle':NS+'item/inevitability_armor'},'elements':elements,'display':display})
        write(ASSETS/f'items/{item}.json',{'model':{'type':'minecraft:model','model':NS+'item/'+item}})
        write(DATA/f'recipe/{item}.json',{'type':'minecraft:smithing_transform',
              'template':'minecraft:netherite_upgrade_smithing_template','base':'minecraft:netherite_'+name,
              'addition':NS+'inevitability_core','result':{'id':NS+item}})
        # Vanilla enchantment, equipment and armor-slot tags.
        for tag in ('head_armor' if slot=='head' else 'chest_armor' if slot=='chest' else 'leg_armor' if slot=='legs' else 'foot_armor',):
            path=ROOT/f'src/main/resources/data/minecraft/tags/item/{tag}.json'
            data=json.loads(path.read_text()) if path.exists() else {'replace':False,'values':[]}
            if NS+item not in data['values']: data['values'].append(NS+item)
            write(path,data)
        unlock(item,NS+'inevitability_core')
    write(DATA/'recipe/inevitability_core.json',{'type':'minecraft:crafting_shaped','category':'equipment',
          'pattern':['EAE','PSP','EAE'],'key':{'E':'minecraft:echo_shard','A':'minecraft:amethyst_shard',
          'P':'minecraft:ender_pearl','S':'minecraft:nether_star'},'result':{'id':NS+'inevitability_core','count':4}})
    unlock('inevitability_core','minecraft:nether_star')
    write(DATA/'tags/item/repairs_inevitability_armor.json',{'replace':False,'values':['minecraft:echo_shard']})
    # A faceted core with a red sealed eye, generated directly as native cube geometry.
    write(ASSETS/'models/item/inevitability_core.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+'item/inevitability_core'}})
    write(ASSETS/'items/inevitability_core.json',{'model':{'type':'minecraft:model','model':NS+'item/inevitability_core'}})
    pixels=[]
    for y in range(32):
        for x in range(32):
            dx,dy=x-15.5,y-15.5
            inside=abs(dx)*.8+abs(dy)<12
            color=COLORS['silver'] if abs(dx)*.8+abs(dy)>9 else COLORS['violet']
            if dx*dx/55+dy*dy/16<1: color=COLORS['eye'] if abs(dx)>1.5 else COLORS['plate']
            pixels.extend([*color,255] if inside else [0,0,0,0])
    png(ASSETS/'textures/item/inevitability_core.png',32,32,pixels)
    lang_path=ASSETS/'lang/en_us.json'; lang=json.loads(lang_path.read_text(encoding='utf-8'))
    for item,name in [('helmet','Crown'),('chestplate','Carapace'),('leggings','Greaves'),('boots','Sabatons')]:
        lang['item.'+NS.replace(':','.')+'inevitability_'+item]='Inevitability '+name
    lang['item.circle_of_inevitability.inevitability_core']='Sealed Inevitability Core'
    lang['tooltip.circle_of_inevitability.inevitability.tentacles']='Four living tendrils lash nearby hostiles: 6 damage, 3 blocks.'
    lang['tooltip.circle_of_inevitability.inevitability.set']='A piece of the Vestments of the Inevitability.'
    lang['tooltip.circle_of_inevitability.inevitability.full_set']='Full set: 8 damage, 4-block reach, faster strikes. Repairs with Echo Shards.'
    write(lang_path,lang)
    print('Generated Inevitability armor rig, 3-D items, textures, recipes and tags.')


def unlock(item,ingredient):
    write(DATA/f'advancement/recipes/combat/{item}.json',{'parent':'minecraft:recipes/root',
          'criteria':{'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':ingredient}]}},
                      'has_the_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':NS+item}}},
          'requirements':[['has_material','has_the_recipe']],'rewards':{'recipes':[NS+item]}})


if __name__=='__main__': generate()
