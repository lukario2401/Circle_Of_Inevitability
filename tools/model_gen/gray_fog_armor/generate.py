"""Deterministic rig, texture atlas, 3-D item models, recipes and tags for the Vestments of the Gray Fog.

Run from any directory: python tools/model_gen/gray_fog_armor/generate.py
Same conventions as tools/model_gen/inevitability_armor: model pixels, Y down, rotations in radians.
The floating paper figurine is built in code (GrayFogArmorModel) because it bobs and vanishes while the
Paper Substitute recharges; its texture region is 'paper'. The two coat tails are rig bones the model sways.
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
SET = 'gray_fog'
# 'cloth' fills the top-left 64x32, 'harlequin' (the Clown's diamonds) the bottom-left 64x32.
UV = {'cloth': [0, 0], 'harlequin': [0, 32], 'silver': [64, 0], 'gold': [96, 0], 'paper': [64, 32], 'lens': [96, 32]}
COLORS = {'cloth': (30, 28, 36), 'harlequin': (30, 28, 36), 'silver': (168, 170, 182), 'gold': (201, 162, 72),
          'paper': (234, 226, 204), 'lens': (206, 228, 255)}
PALE = (196, 198, 210)


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


def cube(b, slot, origin, size, material='cloth'):
    b['cubes'].append(dict(slot=slot, origin=list(origin), size=list(size), uv=UV[material]))


def build_rig():
    bones.clear()
    head = bone('head')
    bone('hat', 'head')
    body = bone('body')
    arms = [bone('right_arm', pivot=(-5, 2, 0)), bone('left_arm', pivot=(5, 2, 0))]
    legs = [bone('right_leg', pivot=(-1.9, 12, 0)), bone('left_leg', pivot=(1.9, 12, 0))]

    # Seer's Top Hat: an open-faced dark cowl, a tall top hat worn at a slight tilt, and a monocle on the
    # right eye (model -X) with a gold chain running back to the ear.
    cube(head, 'head', (-4.6, -8.6, -4.6), (9.2, .9, 9.2))            # cowl crown
    cube(head, 'head', (-4.6, -7.7, 3.8), (9.2, 8.2, .8))             # back of the cowl
    for x in (-4.6, 3.8):
        cube(head, 'head', (x, -7.7, -3.6), (.8, 8.2, 7.4))           # sides, face left open
    cube(head, 'head', (-4.8, -.2, -2), (9.6, 1, 6.6), 'silver')       # cowl hem
    top_hat = bone('top_hat', 'head', (0, -8.6, 0), (0, 0, -.12))
    cube(top_hat, 'head', (-5.6, -.7, -5.6), (11.2, .7, 11.2))        # brim
    cube(top_hat, 'head', (-4.1, -7.6, -4.1), (8.2, 6.9, 8.2))        # crown
    cube(top_hat, 'head', (-4.3, -2.2, -4.3), (8.6, 1.3, 8.6), 'silver')   # hat band
    cube(top_hat, 'head', (-4.25, -7.8, -4.25), (8.5, .4, 8.5))       # flat top lip
    # A tarot card (the Fool) tucked into the band, leaning out.
    card = bone('tarot_card', 'top_hat', (2.4, -2.2, -4.4), (-.12, 0, .22))
    cube(card, 'head', (-.8, -2.6, -.15), (1.6, 2.6, .3), 'paper')
    cube(card, 'head', (-.35, -1.9, -.3), (.7, .9, .2), 'gold')
    # Monocle: gold rim around a pale glowing lens over the right eye, and a chain to the right ear.
    cube(head, 'head', (-3.5, -4.6, -4.9), (2.6, .45, .5), 'gold')
    cube(head, 'head', (-3.5, -2.45, -4.9), (2.6, .45, .5), 'gold')
    cube(head, 'head', (-3.5, -4.15, -4.9), (.45, 1.7, .5), 'gold')
    cube(head, 'head', (-1.35, -4.15, -4.9), (.45, 1.7, .5), 'gold')
    cube(head, 'head', (-3.05, -4.15, -4.75), (1.7, 1.7, .25), 'lens')
    for k in range(4):
        cube(head, 'head', (-3.6 - k*.3, -2.2 + k*.55, -4.6 + k*1.1), (.3, .3, 1.1), 'gold')

    # Magician's Tailcoat: black coat, silver lapels and cuffs, gold buttons, a standing collar and two
    # coat tails (bones the model sways with the legs).
    cube(body, 'chest', (-4.6, -.4, -2.6), (9.2, 12.6, 5.2))
    cube(body, 'chest', (-4.8, -2.2, .6), (9.6, 2.4, 2.4))             # standing collar
    for side in (-1, 1):
        x = -3.4 if side < 0 else 1.6
        cube(body, 'chest', (x, -.2, -2.95), (1.8, 6.2, .5), 'silver')  # lapels
        cube(body, 'chest', (x + (1.2 if side < 0 else -.4), 6, -2.95), (.6, 2.4, .5), 'silver')
    cube(body, 'chest', (-.8, -.2, -2.75), (1.6, 5.4, .3), 'paper')     # shirt front
    cube(body, 'chest', (-1.1, -.3, -3.05), (2.2, .9, .4), 'cloth')      # bow tie
    for y in (5.6, 7.6, 9.6):
        cube(body, 'chest', (-.5, y, -3.1), (1, 1, .5), 'gold')         # buttons
    cube(body, 'chest', (2.4, 2.6, -3.0), (1.2, .5, .4), 'silver')      # pocket square
    for i, side in enumerate((-1, 1)):
        tail = bone('coat_tail_%s' % ('right' if side < 0 else 'left'), 'body', (side*2.3, 11.4, 2.3), (.12, 0, 0))
        cube(tail, 'chest', (-2.3, 0, -.3), (4.4, 9.2, .6))
        cube(tail, 'chest', (-2.3, 8.6, -.35), (4.4, .6, .7), 'silver')
    for i, arm in enumerate(arms):
        center = -1 if i == 0 else 1
        cube(arm, 'chest', (center-2.5, -2.5, -2.5), (5, 12.6, 5))
        cube(arm, 'chest', (center-2.75, -2.75, -2.75), (5.5, 2.4, 5.5))  # shoulders
        cube(arm, 'chest', (center-2.7, 7.2, -2.7), (5.4, 1.6, 5.4), 'silver')  # cuffs
        cube(arm, 'chest', (center-2.6, 8.8, -2.6), (5.2, .7, 5.2), 'paper')    # shirt cuffs

    # Clown's Trousers: harlequin-diamond legs, a black belt with a gold buckle, silver ruffles at the hem.
    cube(body, 'legs', (-4.5, 10.2, -2.5), (9, 1.8, 5))
    cube(body, 'legs', (-1, 10.4, -2.9), (2, 1.4, .5), 'gold')
    for leg in legs:
        cube(leg, 'legs', (-2.3, -.1, -2.3), (4.6, 8.4, 4.6), 'harlequin')
        cube(leg, 'legs', (-2.6, 7.6, -2.6), (5.2, .9, 5.2), 'silver')
        cube(leg, 'legs', (-.6, 2.4, -2.65), (1.2, 1.2, .5), 'gold')        # pompom
        # Marionettist's Shoes: pale spats, polished black shoes, a silver buckle, and two cut strings
        # rising from the heel like a puppet's.
        cube(leg, 'feet', (-2.45, 8.4, -2.55), (4.9, 2.6, 5.1), 'silver')
        cube(leg, 'feet', (-2.6, 10.8, -3.8), (5.2, 1.7, 6.4))
        cube(leg, 'feet', (-2.7, 12.05, -3.95), (5.4, .45, 6.8), 'gold')
        cube(leg, 'feet', (-.7, 10.7, -3.95), (1.4, .9, .4), 'silver')
        for x in (-1.4, 1.1):
            cube(leg, 'feet', (x, 4.2, 2.5), (.25, 4.4, .25), 'paper')
    return bones


def atlas():
    pixels = []
    for y in range(64):
        for x in range(128):
            if x < 64:
                mat = 'cloth' if y < 32 else 'harlequin'
            else:
                mat = ('silver' if y < 32 else 'paper') if x < 96 else ('gold' if y < 32 else 'lens')
            color = COLORS[mat]
            if mat == 'harlequin':
                # 4-pixel diamonds alternating black and pale gray.
                if (((x + y) // 4) + ((x - y) // 4)) % 2 == 0:
                    color = PALE
            n = ((x*13+y*29+x*y*5) % 9)-4
            edge = 8 if mat in ('silver', 'gold') and (x+2*y) % 12 == 0 else 0
            pixels.extend([max(0, min(255, c+n+edge)) for c in color]+[255])
    return pixels


def generate():
    build_rig()
    write(ASSETS/f'armor/{SET}.rig.json', dict(texture_size=[128, 64], bones=bones))
    pixels = atlas()
    for layer in ('humanoid', 'humanoid_leggings', 'humanoid_baby'):
        png(ASSETS/f'textures/entity/equipment/{layer}/{SET}.png', 128, 64, pixels)
    # The block/item atlas only discovers item textures; worn equipment uses direct texture lookup.
    png(ASSETS/f'textures/item/{SET}_armor.png', 128, 64, pixels)
    write(ASSETS/f'equipment/{SET}.json', {'layers': {l: [{'texture': NS+SET}]
          for l in ('humanoid', 'humanoid_leggings', 'humanoid_baby')}})

    # Volumetric inventory/hand models derived from the same rig. Item elements cannot nest rotations, so
    # rotated bones contribute their cube centres transformed through the full hierarchy.
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
          'pattern': ['AEA', 'PSP', 'AEA'], 'key': {'A': 'minecraft:amethyst_block', 'E': 'minecraft:ender_eye',
          'P': 'minecraft:phantom_membrane', 'S': 'minecraft:nether_star'}, 'result': {'id': NS+f'{SET}_core', 'count': 4}})
    unlock(f'{SET}_core', 'minecraft:nether_star')
    write(DATA/f'tags/item/repairs_{SET}_armor.json', {'replace': False, 'values': ['minecraft:phantom_membrane']})
    write(ASSETS/f'models/item/{SET}_core.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': NS+f'item/{SET}_core'}})
    write(ASSETS/f'items/{SET}_core.json', {'model': {'type': 'minecraft:model', 'model': NS+f'item/{SET}_core'}})
    # The Fool's tarot card: paper face, black border, a gold eye over a wisp of gray fog.
    pixels = []
    for y in range(32):
        for x in range(32):
            inside = 7 <= x <= 24 and 3 <= y <= 28
            border = inside and (x in (7, 8, 23, 24) or y in (3, 4, 27, 28))
            if not inside:
                pixels.extend([0, 0, 0, 0])
                continue
            if border:
                pixels.extend([*COLORS['cloth'], 255])
                continue
            color = COLORS['paper']
            dx, dy = (x-15.5)/5.5, (y-13)/3.2
            if dx*dx + dy*dy <= 1:
                color = COLORS['gold'] if dx*dx + dy*dy > .35 else COLORS['cloth']
            if 19 <= y <= 25 and abs(math.sin((x+y*.7)*.55)) * 3 > abs(y-22):
                color = PALE
            pixels.extend([*color, 255])
    png(ASSETS/f'textures/item/{SET}_core.png', 32, 32, pixels)
    lang_path = ASSETS/'lang/en_us.json'
    lang = json.loads(lang_path.read_text(encoding='utf-8'))
    for item, name in [('helmet', "Seer's Top Hat"), ('chestplate', "Magician's Tailcoat"),
                       ('leggings', "Clown's Trousers"), ('boots', "Marionettist's Shoes")]:
        lang[f'item.circle_of_inevitability.{SET}_{item}'] = name
    lang[f'item.circle_of_inevitability.{SET}_core'] = 'Card of the Fool'
    lang[f'tooltip.circle_of_inevitability.{SET}.helmet'] = 'Spirit Vision: mobs hunting you within 24 blocks glow, and so does anything invisible nearby. A chime warns you when a new one locks on.'
    lang[f'tooltip.circle_of_inevitability.{SET}.chestplate'] = 'Paper Substitute: a hit that would kill you or take 40% of your health burns a paper figurine instead, and you blink away. Recharges in 60s.'
    lang[f'tooltip.circle_of_inevitability.{SET}.leggings'] = 'Acrobatics: 20% chance to dodge a melee or projectile hit, then a burst of speed.'
    lang[f'tooltip.circle_of_inevitability.{SET}.boots'] = 'Strings from Above: no fall damage. Hold sneak in midair to drift down slowly.'
    lang[f'tooltip.circle_of_inevitability.{SET}.full_set'] = 'Full set, Above the Gray Fog: crouch still for a second to vanish into fog for up to 10s. Dodge chance 30%, figurine recharges in 40s. Repairs with Phantom Membrane.'
    lang[f'message.circle_of_inevitability.{SET}.substituted'] = 'A paper figurine burned in your place.'
    write(lang_path, lang)
    print('Generated Gray Fog armor rig, 3-D items, textures, recipes and tags.')


def unlock(item, ingredient):
    write(DATA/f'advancement/recipes/combat/{item}.json', {'parent': 'minecraft:recipes/root',
          'criteria': {'has_material': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': ingredient}]}},
                       'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked', 'conditions': {'recipe': NS+item}}},
          'requirements': [['has_material', 'has_the_recipe']], 'rewards': {'recipes': [NS+item]}})


if __name__ == '__main__':
    generate()
