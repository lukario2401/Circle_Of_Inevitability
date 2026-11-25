"""Deterministic rig, palette atlas, 3-D item models, recipes and tags for the Vestments of the Unshadowed.

Run from any directory: python tools/model_gen/unshadowed_armor/generate.py
Same conventions as tools/model_gen/inevitability_armor: model pixels, Y down, rotations in radians.
The three Spears of Light are built in code (UnshadowedArmorModel) because they move freely; their
texture regions are 'gold' (shaft, crossguard) and 'light' (pommel, blade).
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
SET = 'unshadowed'
UV = {'plate': [0, 0], 'gold': [64, 0], 'light': [96, 0], 'cloth': [64, 32], 'bronze': [96, 32]}
COLORS = {'plate': (228, 218, 190), 'gold': (214, 158, 46), 'light': (255, 241, 168),
          'cloth': (176, 62, 30), 'bronze': (88, 60, 32)}


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
    b = dict(name=name, parent=parent, pivot=list(pivot), rotation=list(rotation), cubes=[])
    bones.append(b)
    return b


def cube(b, slot, origin, size, material='plate'):
    b['cubes'].append(dict(slot=slot, origin=list(origin), size=list(size), uv=UV[material]))


def build_rig():
    bones.clear()
    head = bone('head')
    bone('hat', 'head')
    body = bone('body')
    arms = [bone('right_arm', pivot=(-5, 2, 0)), bone('left_arm', pivot=(5, 2, 0))]
    legs = [bone('right_leg', pivot=(-1.9, 12, 0)), bone('left_leg', pivot=(1.9, 12, 0))]

    # Helm: ivory dome, gold brow band with a bronze visor slit, a sun disc on the brow and a short gold crown.
    cube(head, 'head', (-4.6, -8.6, -4.6), (9.2, 9.2, 9.2))
    cube(head, 'head', (-4.9, -6.2, -5.0), (9.8, 1.3, 1), 'gold')
    cube(head, 'head', (-3.6, -4.6, -5.05), (7.2, .8, .5), 'bronze')
    cube(head, 'head', (-1.3, -8.4, -5.3), (2.6, 2.6, .8), 'gold')
    cube(head, 'head', (-.7, -7.8, -5.6), (1.4, 1.4, .5), 'light')
    cube(head, 'head', (-4.9, -9.0, -4.9), (9.8, .9, 9.8), 'gold')
    for k, x in enumerate((-3.6, -1.8, 0, 1.8, 3.6)):
        h = 2.6 if k == 2 else 1.6 if k in (1, 3) else 1.1
        cube(head, 'head', (x-.45, -9-h, -4.7), (.9, h, .9), 'gold')
    # Sun halo behind the head: twelve alternating rays on a ring. UnshadowedArmorModel spins the halo bone.
    halo = bone('halo', 'head', (0, -4.5, 5.4))
    cube(halo, 'head', (-1.1, -1.1, -.4), (2.2, 2.2, .8), 'light')
    for k in range(12):
        ray = bone('halo_ray_%d' % k, 'halo', (0, 0, 0), (0, 0, k*math.pi/6))
        cube(ray, 'head', (-1.6, -6.4, -.3), (3.2, .8, .6), 'gold')        # ring segment
        length = 4.2 if k % 2 == 0 else 2.6
        cube(ray, 'head', (-.45, -6.4-length, -.25), (.9, length, .5), 'light')

    # Cuirass: ivory breastplate, gold collar and a radiant sun emblem, crimson tabard below the waist.
    cube(body, 'chest', (-4.7, -.5, -2.7), (9.4, 12.5, 5.4))
    cube(body, 'chest', (-4.9, -.8, -2.9), (9.8, 1.4, 5.8), 'gold')
    cube(body, 'chest', (-2, 2.2, -3.5), (4, 4, .9), 'gold')
    cube(body, 'chest', (-1.2, 3.0, -4.0), (2.4, 2.4, .6), 'light')
    for ox, oy, sx, sy in ((-.4, .6, .8, 1.5), (-.4, 6.3, .8, 1.5), (-3.6, 3.8, 1.5, .8), (2.1, 3.8, 1.5, .8)):
        cube(body, 'chest', (ox, oy, -3.3), (sx, sy, .6), 'gold')
    for ox, oy in ((-2.9, 1.3), (2.1, 1.3), (-2.9, 6.1), (2.1, 6.1)):
        cube(body, 'chest', (ox, oy, -3.2), (.8, .8, .5), 'light')
    cube(body, 'chest', (-4.8, 10.2, -2.8), (9.6, 1.2, 5.6), 'gold')
    cube(body, 'chest', (-2.2, 11.2, -3.1), (4.4, 5.5, .5), 'cloth')
    cube(body, 'chest', (-2.2, 16.2, -3.15), (4.4, .5, .5), 'gold')
    cube(body, 'chest', (-3.8, -.3, 2.6), (7.6, 15, .5), 'cloth')       # cape
    for i, arm in enumerate(arms):
        center = -1 if i == 0 else 1
        out = -1 if i == 0 else 1
        cube(arm, 'chest', (center-2.55, -2.7, -2.55), (5.1, 12.9, 5.1))
        cube(arm, 'chest', (center-3.2, -3.4, -3.2), (6.4, 3.6, 6.4))
        cube(arm, 'chest', (center-3.35, -.2, -3.35), (6.7, .7, 6.7), 'gold')
        cube(arm, 'chest', (center-2.8, 6.3, -2.8), (5.6, 2.4, 5.6), 'gold')
        cube(arm, 'chest', (center-.6, 6.6, -3.1), (1.2, 1.6, .5), 'light')
        # Three flared sun rays rising from each pauldron.
        for k in range(3):
            flare = bone('pauldron_ray_%d_%d' % (i, k), arm['name'], (center + out*1.2, -3.4, (k-1)*2.1),
                         (0, 0, out*(.55 + .2*abs(k-1))))
            cube(flare, 'chest', (-.45, -2.8 + .8*abs(k-1), -.45), (.9, 2.8 - .8*abs(k-1), .9), 'gold')

    # Tassets: gold belt with a sun buckle, ivory leg plates, gold knee discs.
    cube(body, 'legs', (-4.55, 9.4, -2.55), (9.1, 2.8, 5.1), 'gold')
    cube(body, 'legs', (-1.2, 9.8, -3.0), (2.4, 2.0, .6), 'light')
    for i, leg in enumerate(legs):
        cube(leg, 'legs', (-2.35, -.1, -2.35), (4.7, 7.9, 4.7))
        cube(leg, 'legs', (-2.55, .3, -2.7), (5.1, 3.6, .8), 'gold')
        cube(leg, 'legs', (-1.4, 5.2, -2.9), (2.8, 2.6, .9), 'gold')
        cube(leg, 'legs', (-.7, 5.8, -3.2), (1.4, 1.4, .5), 'light')
        # Sabatons: ivory greaves, gold toe caps and a light sole rim.
        cube(leg, 'feet', (-2.55, 7.8, -2.6), (5.1, 4.7, 5.2))
        cube(leg, 'feet', (-2.65, 9.6, -2.75), (5.3, .8, 5.5), 'gold')
        cube(leg, 'feet', (-2.6, 10.6, -3.7), (5.2, 1.9, 1.4), 'gold')
        cube(leg, 'feet', (-2.7, 12, -3.9), (5.4, .6, 6.8), 'light')
        cube(leg, 'feet', (-.5, 8.4, -2.95), (1, 1.6, .5), 'light')
    return bones


def generate():
    build_rig()
    write(ASSETS/f'armor/{SET}.rig.json', dict(texture_size=[128, 64], bones=bones))
    pixels = []
    for y in range(64):
        for x in range(128):
            mat = 'plate' if x < 64 else ('gold' if y < 32 else 'cloth') if x < 96 else ('light' if y < 32 else 'bronze')
            color = COLORS[mat]
            n = ((x*13+y*29+x*y*5) % 11)-5
            edge = 10 if (x+2*y) % 14 == 0 else 0
            pixels.extend([max(0, min(255, c+n+edge)) for c in color]+[255])
    for layer in ('humanoid', 'humanoid_leggings', 'humanoid_baby'):
        png(ASSETS/f'textures/entity/equipment/{layer}/{SET}.png', 128, 64, pixels)
    # The block/item atlas only discovers item textures; worn equipment uses direct texture lookup.
    png(ASSETS/f'textures/item/{SET}_armor.png', 128, 64, pixels)
    write(ASSETS/f'equipment/{SET}.json', {'layers': {l: [{'texture': NS+SET}]
          for l in ('humanoid', 'humanoid_leggings', 'humanoid_baby')}})

    # Volumetric inventory/hand models derived from the same rig. Item elements cannot nest rotations, so
    # rotated bones contribute their cube positions transformed through the full hierarchy.
    positions = {b['name']: b for b in bones}

    def world(p, b):
        x, y, z = p
        rx, ry, rz = b['rotation']
        y, z = y*math.cos(rx)-z*math.sin(rx), y*math.sin(rx)+z*math.cos(rx)
        x, z = x*math.cos(ry)+z*math.sin(ry), -x*math.sin(ry)+z*math.cos(ry)
        x, y = x*math.cos(rz)-y*math.sin(rz), x*math.sin(rz)+y*math.cos(rz)
        p = [x+b['pivot'][0], y+b['pivot'][1], z+b['pivot'][2]]
        return world(p, positions[b['parent']]) if b['parent'] != 'root' else p
    names = {'head': 'helmet', 'chest': 'chestplate', 'legs': 'leggings', 'feet': 'boots'}
    tags = {'head': 'head_armor', 'chest': 'chest_armor', 'legs': 'leg_armor', 'feet': 'foot_armor'}
    for slot, name in names.items():
        shapes = []
        for b in bones:
            for c in b['cubes']:
                if c['slot'] != slot:
                    continue
                # Use the transformed cube centre so rotated rays stay balanced around their pivot.
                s = c['size']
                centre = world([c['origin'][k]+s[k]/2 for k in range(3)], b)
                shapes.append(([centre[k]-s[k]/2 for k in range(3)], s, c['uv']))
        lo = [min(p[k] for p, s, uv in shapes) for k in range(3)]
        hi = [max(p[k]+s[k] for p, s, uv in shapes) for k in range(3)]
        scale = 13/max(hi[k]-lo[k] for k in range(3))
        center = [(lo[k]+hi[k])/2 for k in range(3)]
        elements = []
        for p, s, uv in shapes:
            a = [8+(p[0]-center[0])*scale, 8-(p[1]+s[1]-center[1])*scale, 8+(p[2]-center[2])*scale]
            b = [a[k]+s[k]*scale for k in range(3)]
            elements.append({'from': [round(v, 4) for v in a], 'to': [round(v, 4) for v in b],
                             'faces': {face: {'texture': '#armor', 'uv': [uv[0]/8, uv[1]/4, (uv[0]+4)/8, (uv[1]+4)/4]}
                                       for face in ('north', 'south', 'east', 'west', 'up', 'down')}})
        display = {'gui': {'rotation': [15, 210, 0], 'translation': [0, 0, 0], 'scale': [.9, .9, .9]},
                   'ground': {'translation': [0, 3, 0], 'scale': [.45, .45, .45]},
                   'fixed': {'rotation': [0, 180, 0], 'scale': [.8, .8, .8]},
                   'thirdperson_righthand': {'rotation': [75, 45, 0], 'scale': [.55, .55, .55]},
                   'firstperson_righthand': {'rotation': [0, 200, 0], 'translation': [1, 1, 0], 'scale': [.65, .65, .65]}}
        item = f'{SET}_{name}'
        write(ASSETS/f'models/item/{item}.json', {'textures': {'armor': NS+f'item/{SET}_armor', 'particle': NS+f'item/{SET}_armor'},
                                                  'elements': elements, 'display': display})
        write(ASSETS/f'items/{item}.json', {'model': {'type': 'minecraft:model', 'model': NS+'item/'+item}})
        write(DATA/f'recipe/{item}.json', {'type': 'minecraft:smithing_transform',
              'template': 'minecraft:netherite_upgrade_smithing_template', 'base': 'minecraft:netherite_'+name,
              'addition': NS+f'{SET}_core', 'result': {'id': NS+item}})
        path = ROOT/f'src/main/resources/data/minecraft/tags/item/{tags[slot]}.json'
        data = json.loads(path.read_text()) if path.exists() else {'replace': False, 'values': []}
        if NS+item not in data['values']:
            data['values'].append(NS+item)
        write(path, data)
        unlock(item, NS+f'{SET}_core')
    write(DATA/f'recipe/{SET}_core.json', {'type': 'minecraft:crafting_shaped', 'category': 'equipment',
          'pattern': ['GLG', 'BSB', 'GLG'], 'key': {'G': 'minecraft:gold_block', 'L': 'minecraft:glowstone',
          'B': 'minecraft:blaze_rod', 'S': 'minecraft:nether_star'}, 'result': {'id': NS+f'{SET}_core', 'count': 4}})
    unlock(f'{SET}_core', 'minecraft:nether_star')
    write(DATA/f'tags/item/repairs_{SET}_armor.json', {'replace': False, 'values': ['minecraft:gold_block']})
    write(ASSETS/f'models/item/{SET}_core.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': NS+f'item/{SET}_core'}})
    write(ASSETS/f'items/{SET}_core.json', {'model': {'type': 'minecraft:model', 'model': NS+f'item/{SET}_core'}})
    # A small radiant sun: gold rays, a light disc and a bronze rim.
    pixels = []
    for y in range(32):
        for x in range(32):
            dx, dy = x-15.5, y-15.5
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            ray = r < 9 + 5*max(0, math.cos(a*8))**6
            if r < 6.5:
                color = COLORS['light'] if r < 5.2 else COLORS['bronze']
                pixels.extend([*color, 255])
            elif ray:
                pixels.extend([*COLORS['gold'], 255])
            else:
                pixels.extend([0, 0, 0, 0])
    png(ASSETS/f'textures/item/{SET}_core.png', 32, 32, pixels)
    lang_path = ASSETS/'lang/en_us.json'
    lang = json.loads(lang_path.read_text(encoding='utf-8'))
    for item, name in [('helmet', 'Halo Helm'), ('chestplate', 'Cuirass'), ('leggings', 'Tassets'), ('boots', 'Sabatons')]:
        lang[f'item.circle_of_inevitability.{SET}_{item}'] = 'Unshadowed '+name
    lang[f'item.circle_of_inevitability.{SET}_core'] = 'Sunfire Core'
    lang[f'tooltip.circle_of_inevitability.{SET}.power'] = 'Three Spears of Light strike nearby hostiles: 5 damage, 5 blocks. Undead take double and burn.'
    lang[f'tooltip.circle_of_inevitability.{SET}.set'] = 'A piece of the Vestments of the Unshadowed.'
    lang[f'tooltip.circle_of_inevitability.{SET}.full_set'] = 'Full set: 7 damage, 6-block reach, faster strikes, immune to Blindness and Darkness. Repairs with Gold Blocks.'
    write(lang_path, lang)
    print('Generated Unshadowed armor rig, 3-D items, textures, recipes and tags.')


def unlock(item, ingredient):
    write(DATA/f'advancement/recipes/combat/{item}.json', {'parent': 'minecraft:recipes/root',
          'criteria': {'has_material': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': ingredient}]}},
                       'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked', 'conditions': {'recipe': NS+item}}},
          'requirements': [['has_material', 'has_the_recipe']], 'rewards': {'recipes': [NS+item]}})


if __name__ == '__main__':
    generate()
