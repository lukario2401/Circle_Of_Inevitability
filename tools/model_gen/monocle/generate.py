"""Texture atlas, 3-D item model and equipment asset for the Monocle sealed artifact.

Run from any directory: python tools/model_gen/monocle/generate.py
The worn model (gold rim, lens, chain to the ear, and the marionette strings that appear once the wearer is far
gone) is built in code by client/armor/MonocleModel, using the same atlas regions as below.
"""
import json
import math
import random
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'src/main/resources/assets/circle_of_inevitability'
NS = 'circle_of_inevitability:'
W, H = 64, 32
# 16x16 regions of the atlas. Keep in sync with MonocleModel.
REGIONS = {'gold': (0, 0), 'lens': (16, 0), 'chain': (32, 0), 'string': (48, 0), 'wood': (0, 16), 'iris': (16, 16)}
COLORS = {'gold': (214, 172, 74), 'lens': (190, 222, 236), 'chain': (160, 124, 52), 'string': (226, 222, 210),
          'wood': (96, 64, 38), 'iris': (150, 24, 32)}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')


def png(path, width, height, pixels):
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data))
    raw = b''.join(b'\0' + bytes(pixels[y*width*4:(y+1)*width*4]) for y in range(height))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
                     + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def atlas():
    rng = random.Random(7)
    px = [0] * (W * H * 4)
    for name, (ox, oy) in REGIONS.items():
        r, g, b = COLORS[name]
        for y in range(16):
            for x in range(16):
                shade = 1 + rng.uniform(-.06, .06)
                if name == 'lens':      # a pale glassy gradient with a highlight
                    shade *= 1.12 - .02 * (x + y) / 2 + (.25 if x + y in (5, 6) else 0)
                if name == 'gold' and (x + 2 * y) % 7 == 0:
                    shade *= 1.18
                if name == 'wood' and y % 4 == 0:
                    shade *= .82
                i = ((oy + y) * W + ox + x) * 4
                px[i:i+4] = [min(255, int(c * shade)) for c in (r, g, b)] + [200 if name == 'lens' else 255]
    return px


def uv(region, w=16, h=16):
    x, y = REGIONS[region]
    return [x * 16 / W, y * 16 / H, (x + w) * 16 / W, (y + h) * 16 / H]


def box(frm, to, region):
    faces = {f: {'texture': '#atlas', 'uv': uv(region, 4, 4)} for f in ('north', 'south', 'east', 'west', 'up', 'down')}
    return {'from': [round(v, 4) for v in frm], 'to': [round(v, 4) for v in to], 'faces': faces}


def item_model():
    e = []
    # Gold rim around the lens, chamfered corners, centred on (8, 9).
    e.append(box((5, 13.2, 7.4), (11, 14, 8.6), 'gold'))
    e.append(box((5, 4, 7.4), (11, 4.8, 8.6), 'gold'))
    e.append(box((3.6, 5.4, 7.4), (4.4, 12.6, 8.6), 'gold'))
    e.append(box((11.6, 5.4, 7.4), (12.4, 12.6, 8.6), 'gold'))
    for x, y in ((4.2, 12.4), (10.8, 12.4), (4.2, 4.6), (10.8, 4.6)):
        e.append(box((x, y, 7.45), (x + 1, y + 1, 8.55), 'gold'))
    lens = box((4.4, 4.8, 7.9), (11.6, 13.2, 8.1), 'lens')
    lens['faces']['north']['uv'] = lens['faces']['south']['uv'] = uv('lens')
    e.append(lens)
    # A chain hanging from the rim, swinging out to a clasp.
    for k in range(7):
        t = k / 6
        x, y = 12.2 + t * 2.4, 4.4 - k * .95 + math.sin(t * math.pi) * .3
        e.append(box((x, y, 7.7), (x + .6, y + .7, 8.3), 'chain'))
    e.append(box((14.2, -2.6, 7.5), (15.4, -1.4, 8.5), 'gold'))
    return {
        'textures': {'atlas': NS + 'item/monocle', 'particle': NS + 'item/monocle'},
        'elements': e,
        'display': {
            'gui': {'rotation': [0, -20, 0], 'translation': [0, 1, 0], 'scale': [0.95, 0.95, 0.95]},
            'ground': {'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
            'fixed': {'scale': [0.9, 0.9, 0.9]},
            'head': {'translation': [0, 0, -7], 'scale': [0.6, 0.6, 0.6]},
            'thirdperson_righthand': {'rotation': [0, 90, 0], 'translation': [0, 1, 1], 'scale': [0.5, 0.5, 0.5]},
            'thirdperson_lefthand': {'rotation': [0, 90, 0], 'translation': [0, 1, 1], 'scale': [0.5, 0.5, 0.5]},
            'firstperson_righthand': {'rotation': [0, -40, 10], 'translation': [1.5, 4, 1], 'scale': [0.35, 0.35, 0.35]},
            'firstperson_lefthand': {'rotation': [0, -40, 10], 'translation': [1.5, 4, 1], 'scale': [0.35, 0.35, 0.35]},
        },
    }


def main():
    px = atlas()
    png(ASSETS / 'textures/item/monocle.png', W, H, px)
    png(ASSETS / 'textures/entity/equipment/humanoid/monocle.png', W, H, px)
    write(ASSETS / 'models/item/monocle.json', item_model())
    write(ASSETS / 'items/monocle.json', {'model': {'type': 'minecraft:model', 'model': NS + 'item/monocle'}})
    write(ASSETS / 'equipment/monocle.json', {'layers': {'humanoid': [{'texture': NS + 'monocle'}]}})
    print('monocle assets written')


if __name__ == '__main__':
    main()
