"""3-D held models for Creeping Hunger, one per tier: a clawed leather glove with a toothed mouth in the palm, veins,
and more eyes, bone spikes and redder light the higher the tier.

Run from any directory: python tools/model_gen/creeping_hunger/generate.py   (Pillow required)
Writes models/item/creeping_hunger_<tier>.json and textures/item/creeping_hunger_model_<tier>.png.
Model space is Minecraft's: pixels 0..16, +Y up; the fingers point up, the mouth (palm) faces south (+Z), the back
of the hand with its eyes faces north. The flat icons (tools/item_art/creeping_hunger.py) stay as the particle
texture.
"""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'src/main/resources/assets/circle_of_inevitability'
NS = 'circle_of_inevitability:'

# Per tier: leather, its shadow, veins, glow (eyes, tongue), teeth/bone.
TIERS = {
    1: dict(leather=(74, 59, 63), shade=(43, 33, 36), vein=(110, 42, 58), glow=(163, 58, 79), bone=(207, 195, 168), eyes=0, spikes=0),
    2: dict(leather=(69, 52, 58), shade=(39, 28, 32), vein=(127, 38, 56), glow=(191, 53, 80), bone=(216, 203, 176), eyes=1, spikes=0),
    3: dict(leather=(63, 45, 53), shade=(34, 23, 28), vein=(147, 34, 53), glow=(217, 58, 85), bone=(226, 212, 182), eyes=2, spikes=0),
    4: dict(leather=(56, 38, 47), shade=(30, 19, 24), vein=(168, 30, 48), glow=(238, 61, 85), bone=(236, 220, 188), eyes=4, spikes=2),
    5: dict(leather=(48, 30, 40), shade=(25, 14, 20), vein=(192, 24, 43), glow=(255, 74, 94), bone=(245, 230, 196), eyes=6, spikes=4),
}

# The texture: a 4x4 grid of 16x16 texel zones (4 uv units each).
ZONES = ['leather', 'shade', 'cuff', 'trim', 'mouth', 'teeth', 'bone', 'eye', 'vein', 'tongue', 'gum', 'knuckle']
TEX = 64


def zone_uv(name):
    i = ZONES.index(name)
    u, v = (i % 4) * 4, (i // 4) * 4
    return [u, v, u + 4, v + 4]


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def paint(tier):
    c = TIERS[tier]
    rnd = random.Random(100 + tier)
    img = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
    px = img.load()

    def fill(name, fn):
        u0, v0, _, _ = zone_uv(name)
        x0, y0 = u0 * TEX // 16, v0 * TEX // 16
        for y in range(16):
            for x in range(16):
                px[x0 + x, y0 + y] = fn(x, y) + (255,) if len(fn(x, y)) == 3 else fn(x, y)

    def grain(base, amount=10):
        return lambda x, y: clamp(tuple(v + rnd.randint(-amount, amount) for v in base))

    def stitched(base, seam):
        def fn(x, y):
            if (x in (0, 15) or y in (0, 15)):
                return clamp(tuple(v * .7 for v in base))
            if y == 8 and x % 3 == 1:
                return seam
            return clamp(tuple(v + rnd.randint(-9, 9) for v in base))
        return fn

    fill('leather', stitched(c['leather'], clamp(tuple(v * 1.4 for v in c['vein']))))
    fill('shade', grain(c['shade'], 6))
    fill('cuff', lambda x, y: clamp(tuple(v * (.85 if y % 4 == 0 else 1) + rnd.randint(-5, 5) for v in c['shade'])))
    fill('trim', lambda x, y: clamp(tuple(v * (1.2 if y < 8 else .8) for v in c['vein'])))
    fill('mouth', lambda x, y: clamp((18 + 3 * abs(8 - y), 6, 9)))
    fill('teeth', lambda x, y: clamp(tuple(v * (1 - y / 40) for v in c['bone'])))
    fill('bone', lambda x, y: clamp(tuple(v * (.75 + y / 64) for v in c['bone'])))

    def eye(x, y):
        d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** .5
        if d < 2.5:
            return (20, 4, 6)
        if d < 6:
            return clamp(tuple(v * (1.1 - d / 12) for v in c['glow']))
        return clamp(tuple(v * .6 for v in c['vein']))
    fill('eye', eye)
    fill('vein', lambda x, y: clamp(tuple(v * (.8 + .3 * ((x + y) % 4 == 0)) for v in c['vein'])))
    fill('tongue', lambda x, y: clamp(tuple(v * (.9 if x % 5 else 1.15) for v in c['glow'])))
    fill('gum', grain(tuple(v * .8 for v in c['vein']), 8))
    fill('knuckle', lambda x, y: clamp(tuple(v * (1.25 if 4 < x < 12 and 4 < y < 12 else 1) + rnd.randint(-6, 6) for v in c['leather'])))
    return img


def cube(frm, to, zone, rot=None, faces=None):
    """One element; every face shows the zone ('faces' overrides some: {'south': 'mouth'})."""
    faces = faces or {}
    el = {'from': [round(v, 3) for v in frm], 'to': [round(v, 3) for v in to], 'faces': {}}
    for face in ('north', 'east', 'south', 'west', 'up', 'down'):
        el['faces'][face] = {'uv': zone_uv(faces.get(face, zone)), 'texture': '#glove'}
    if rot:
        angle, axis, origin = rot
        el['rotation'] = {'angle': angle, 'axis': axis, 'origin': [round(v, 3) for v in origin]}
    return el


def glove(tier):
    c = TIERS[tier]
    els = []
    # Cuff and its trim.
    els.append(cube((4, 0, 5), (12, 3, 11), 'cuff'))
    els.append(cube((3.75, 2.4, 4.75), (12.25, 3.4, 11.25), 'trim'))
    # Palm/back of the hand and the knuckle ridge.
    els.append(cube((4.5, 3, 6), (11.5, 9.5, 10), 'leather', faces={'up': 'shade', 'down': 'shade'}))
    els.append(cube((4.25, 8.75, 6.25), (11.75, 10.25, 9.75), 'knuckle'))
    # The mouth in the palm (south): a dark maw, gums, teeth and the tongue.
    els.append(cube((5.5, 4.3, 9.9), (10.5, 8, 10.15), 'mouth'))
    els.append(cube((5.25, 7.6, 10), (10.75, 8.3, 10.35), 'gum'))
    els.append(cube((5.25, 3.9, 10), (10.75, 4.6, 10.35), 'gum'))
    for i, x in enumerate((5.6, 6.6, 7.6, 8.6, 9.6)):
        long = 1.3 if i in (0, 4) or tier >= 4 else 0.9
        els.append(cube((x, 7.6 - long, 10.05), (x + .7, 7.7, 10.4), 'teeth'))
        els.append(cube((x + .3, 4.5, 10.05), (x + .9, 4.5 + long * .8, 10.4), 'teeth'))
    els.append(cube((6.8, 4.5, 9.95), (9.2, 5.4, 10.25), 'tongue'))
    # Fingers: two joints each, the upper one curled toward the palm, a bone claw at the tip.
    xs = (4.6, 6.45, 8.3, 10.15)
    lower = (2.6, 3.2, 3.0, 2.2)
    upper = (2.0, 2.4, 2.2, 1.8)
    for x, l1, l2 in zip(xs, lower, upper):
        base = 10.0
        els.append(cube((x, base, 7.1), (x + 1.4, base + l1, 9.1), 'leather', faces={'up': 'shade'}))
        joint = (x + .7, base + l1, 8.1)
        curl = (22.5, 'x', joint)
        els.append(cube((x + .05, base + l1, 7.15), (x + 1.35, base + l1 + l2, 9.05), 'leather', rot=curl))
        els.append(cube((x + .3, base + l1 + l2, 7.9), (x + 1.1, base + l1 + l2 + 1.3, 8.9), 'bone', rot=curl))
    # Thumb on the east side, leaning out.
    thumb = (-22.5, 'z', (11.5, 4.5, 8.1))
    els.append(cube((11.3, 4.5, 7.1), (12.9, 8.3, 9.1), 'leather', rot=thumb))
    els.append(cube((11.6, 8.3, 7.6), (12.6, 9.5, 8.6), 'bone', rot=thumb))
    # Veins over the back of the hand (north).
    els.append(cube((6.1, 3.2, 5.8), (6.5, 9, 6.05), 'vein'))
    els.append(cube((9.2, 3.5, 5.8), (9.6, 8.6, 6.05), 'vein'))
    els.append(cube((7.0, 5.5, 5.8), (9.0, 5.9, 6.05), 'vein', rot=(22.5, 'z', (8, 5.7, 6))))
    if tier >= 3:
        els.append(cube((4.4, 1.2, 4.85), (4.8, 6.5, 5.1), 'vein'))
        els.append(cube((11.2, 1.2, 4.85), (11.6, 6.5, 5.1), 'vein'))
    # Eyes opening across the back, more each tier.
    spots = [(7.3, 6.6), (9.7, 4.6), (5.0, 4.4), (10.0, 7.6), (7.6, 3.6), (4.9, 7.6)]
    for x, y in spots[:c['eyes']]:
        els.append(cube((x, y, 5.7), (x + 1.1, y + .9, 6.0), 'eye'))
    # Bone spikes along the knuckles (tier IV+), tilted back.
    for i in range(c['spikes']):
        x = 4.8 + i * 1.85
        els.append(cube((x, 10.1, 6.4), (x + .7, 11.8, 7.1), 'bone', rot=(-22.5, 'x', (x + .35, 10.1, 6.75))))
    return els


def model(tier):
    return {
        'credit': 'Generated by tools/model_gen/creeping_hunger/generate.py',
        'textures': {
            'glove': f'{NS}item/creeping_hunger_model_{tier}',
            'particle': f'{NS}item/creeping_hunger_{tier}',
        },
        'elements': glove(tier),
        'display': {
            'gui': {'rotation': [15, -30, 0], 'translation': [0, -0.75, 0], 'scale': [0.88, 0.88, 0.88]},
            'ground': {'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
            'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.8, 0.8, 0.8]},
            'head': {'translation': [0, 6, 0], 'scale': [0.8, 0.8, 0.8]},
            'thirdperson_righthand': {'rotation': [0, -90, 0], 'translation': [0, 1.5, 1.5], 'scale': [0.55, 0.55, 0.55]},
            'thirdperson_lefthand': {'rotation': [0, 90, 0], 'translation': [0, 1.5, 1.5], 'scale': [0.55, 0.55, 0.55]},
            'firstperson_righthand': {'rotation': [0, -100, 15], 'translation': [1.5, 1.5, -1], 'scale': [0.6, 0.6, 0.6]},
            'firstperson_lefthand': {'rotation': [0, 100, -15], 'translation': [1.5, 1.5, -1], 'scale': [0.6, 0.6, 0.6]},
        },
    }


def main():
    for tier in TIERS:
        tex = ASSETS / f'textures/item/creeping_hunger_model_{tier}.png'
        tex.parent.mkdir(parents=True, exist_ok=True)
        paint(tier).save(tex)
        out = ASSETS / f'models/item/creeping_hunger_{tier}.json'
        out.write_text(json.dumps(model(tier), indent=2) + '\n', encoding='utf-8')
        print(out, len(model(tier)['elements']), 'elements')


if __name__ == '__main__':
    main()
