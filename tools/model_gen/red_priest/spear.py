"""3-D held model of the Red Priest uniqueness: a war spear.

A long blackened shaft spiralled with crimson, a red leather grip between gold rings, a gold crown around the socket
with a burning gem, two hooked gold wings, a leaf-shaped crimson blade with a glowing ember channel down its middle,
a small war pennant under the head and a pointed gold-and-iron butt.

Run from any directory: python tools/model_gen/red_priest/spear.py   (Pillow required)
Writes models/item/red_priest_uniqueness.json, textures/item/red_priest_uniqueness_model.png and the flat
textures/item/red_priest_uniqueness.png (particles and anything that needs a 2-D picture).
Model space is Minecraft's: pixels, +Y up, every coordinate inside -16..32. The spear stands along Y through the
middle (x = z = 8), the blade's flat faces look north and south. The grip is around y = 6, near the model's middle,
so the display transforms only have to tilt and scale it.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'src/main/resources/assets/circle_of_inevitability'
NS = 'circle_of_inevitability:'
NAME = 'red_priest_uniqueness'

# The texture is a 4x4 grid of 16x16 texel zones (4 uv units each).
ZONES = ['shaft', 'grip', 'gold', 'gold_dark', 'blade', 'blade_edge', 'ember', 'gem',
         'iron', 'pennant', 'tassel', 'blade_dark']
TEX = 64

CRIMSON = (150, 26, 34)
BLOOD = (98, 12, 20)
GOLD = (220, 172, 70)
GOLD_DARK = (146, 98, 38)
WOOD = (40, 22, 24)
IRON = (62, 56, 64)


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def zone_uv(name):
    i = ZONES.index(name)
    u, v = (i % 4) * 4, (i // 4) * 4
    return [u, v, u + 4, v + 4]


def paint():
    rnd = random.Random(1907)
    img = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
    px = img.load()

    def fill(name, fn):
        u0, v0, _, _ = zone_uv(name)
        x0, y0 = u0 * TEX // 16, v0 * TEX // 16
        for y in range(16):
            for x in range(16):
                c = fn(x, y)
                px[x0 + x, y0 + y] = clamp(c) + ((255,) if len(c) == 3 else (int(c[3]),))

    def noise(amount):
        return rnd.uniform(-amount, amount)

    # Shaft: blackened wood, a crimson ribbon spiralling down it, fine grain.
    def shaft(x, y):
        base = mix(WOOD, (58, 34, 34), (x % 5) / 8)
        if (x + y) % 8 in (0, 1):
            base = mix(CRIMSON, BLOOD, (x + y) % 8 * .6)
        return tuple(v + noise(5) for v in base)
    fill('shaft', shaft)

    # Grip: red leather wrapped in overlapping bands, darker where they overlap.
    def grip(x, y):
        band = (y + x // 2) % 4
        base = mix((150, 30, 36), (88, 14, 20), band / 4)
        if band == 0:
            base = (176, 52, 52)
        return tuple(v + noise(6) for v in base)
    fill('grip', grip)

    # Gold, polished: brighter towards the top of each face, a hard highlight line.
    fill('gold', lambda x, y: tuple(v + noise(6) for v in
                                    ((255, 224, 140) if y == 3 else mix(GOLD, GOLD_DARK, y / 18))))
    fill('gold_dark', lambda x, y: tuple(v + noise(6) for v in mix(GOLD_DARK, (92, 56, 22), y / 16)))

    # Blade: crimson steel, brighter towards its middle, hammer marks.
    def blade(x, y):
        d = abs(x - 7.5) / 7.5
        base = mix((206, 58, 56), (118, 18, 26), d)
        if rnd.random() < .06:
            base = mix(base, (60, 6, 12), .35)
        return tuple(v + noise(5) for v in base)
    fill('blade', blade)
    fill('blade_dark', lambda x, y: tuple(v + noise(5) for v in mix((112, 16, 24), (70, 8, 14), y / 16)))
    fill('blade_edge', lambda x, y: tuple(v + noise(8) for v in mix((255, 170, 150), (226, 96, 86), abs(x - 7.5) / 8)))

    # Ember channel: white-hot in the middle, red at the sides, flickering specks.
    def ember(x, y):
        d = abs(x - 7.5) / 7.5
        base = mix((255, 226, 150), (238, 60, 26), d * 1.2)
        if rnd.random() < .1:
            base = (255, 244, 200)
        return base
    fill('ember', ember)

    def gem(x, y):
        d = math.hypot(x - 7.5, y - 7.5) / 8
        if d < .25:
            return (255, 236, 210)
        return mix((255, 96, 70), (120, 0, 16), d)
    fill('gem', gem)

    fill('iron', lambda x, y: tuple(v + noise(6) for v in
                                    ((150, 140, 150) if x in (3, 4) else mix(IRON, (32, 28, 34), x / 16))))

    # The pennant: crimson with a gold hem, a black crown, a swallowtail and ragged holes.
    pen = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(pen)
    d.rectangle([0, 0, 15, 15], fill=(150, 20, 28, 255))
    for y in range(16):
        for x in range(16):
            r, g, b, a = pen.getpixel((x, y))
            shade = 1 - y / 40 + rnd.uniform(-.05, .05)
            pen.putpixel((x, y), clamp((r * shade, g * shade, b * shade)) + (255,))
    d.rectangle([0, 0, 15, 1], fill=(222, 174, 70, 255))                      # top hem
    d.line([(0, 0), (0, 15)], fill=(222, 174, 70, 255))                       # the side at the shaft
    crown = [(4, 9), (4, 5), (6, 7), (8, 4), (10, 7), (12, 5), (12, 9)]       # the Conqueror's crown
    d.polygon(crown, fill=(22, 6, 8, 255))
    d.line([(4, 10), (12, 10)], fill=(22, 6, 8, 255))
    d.point([(8, 6)], fill=(255, 120, 80, 255))
    d.polygon([(6, 16), (8, 11), (10, 16)], fill=(0, 0, 0, 0))                # swallowtail
    for x, y in [(2, 15), (3, 14), (13, 15), (14, 13), (12, 14), (1, 13)]:     # tatters
        pen.putpixel((x, y), (0, 0, 0, 0))
    u0, v0, _, _ = zone_uv('pennant')
    img.paste(pen, (u0 * TEX // 16, v0 * TEX // 16))

    fill('tassel', lambda x, y: tuple(v + noise(6) for v in
                                      ((222, 174, 70) if y < 3 else mix((176, 32, 38), (96, 12, 18), (x % 3) / 3))))
    return img


def cube(frm, to, zone, rot=None, faces=None, light=0):
    """One element; every face shows the zone ('faces' overrides some: {'up': 'gold'})."""
    faces = faces or {}
    el = {'from': [round(v, 3) for v in frm], 'to': [round(v, 3) for v in to], 'faces': {}}
    for face in ('north', 'east', 'south', 'west', 'up', 'down'):
        el['faces'][face] = {'uv': zone_uv(faces.get(face, zone)), 'texture': '#spear'}
    if rot:
        angle, axis, origin = rot
        el['rotation'] = {'angle': angle, 'axis': axis, 'origin': [round(v, 3) for v in origin]}
    if light:
        el['light_emission'] = light
    return el


def centred(half_x, y0, y1, zone, half_z=None, **kw):
    half_z = half_x if half_z is None else half_z
    return cube((8 - half_x, y0, 8 - half_z), (8 + half_x, y1, 8 + half_z), zone, **kw)


def spear():
    els = []
    # Butt: an iron spike under a gold pommel.
    els.append(cube((7.45, -15.2, 7.45), (8.55, -14.1, 8.55), 'iron', rot=(45, 'z', (8, -15.2, 8))))
    els.append(centred(.55, -14.6, -13.3, 'iron'))
    els.append(centred(1.0, -13.5, -11.8, 'gold_dark', faces={'up': 'gold'}))
    els.append(centred(.8, -11.9, -11.2, 'gold'))
    # Shaft.
    els.append(centred(.65, -11.2, 14.6, 'shaft'))
    # Grip between two gold rings, a third ring under the pennant.
    els.append(centred(.82, 0.4, 7.6, 'grip'))
    els.append(centred(.9, -0.2, 0.5, 'gold'))
    els.append(centred(.9, 7.5, 8.2, 'gold'))
    els.append(centred(.85, -6.4, -5.8, 'gold_dark'))
    # Socket: a dark gold collar, the crown ring with five points, the gem burning in it (front and back).
    els.append(centred(1.0, 13.6, 16.6, 'gold_dark'))
    els.append(centred(1.45, 16.0, 17.0, 'gold'))
    for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1), (0, -1.15), (0, 1.15)):
        x, z = 8 + dx * 1.1, 8 + dz * 1.1
        h = 1.9 if dx == 0 else 1.3
        els.append(cube((x - .3, 17.0, z - .3), (x + .3, 17.0 + h, z + .3), 'gold'))
    els.append(cube((7.4, 14.3, 6.9), (8.6, 15.5, 9.1), 'gem', light=15))
    # Wings: hooked gold blades curving up from the crown, a smaller pair of barbs under them.
    for side in (-1, 1):
        inner = 8 + side * 1.2
        outer = 8 + side * 5.4
        lo, hi = sorted((inner, outer))
        r = (side * 24, 'z', (inner, 17.2, 8))
        els.append(cube((lo, 16.7, 7.72), (hi, 17.7, 8.28), 'gold', rot=r))
        hook_x = 8 + side * 4.9
        hlo, hhi = sorted((hook_x, hook_x + side * .9))
        els.append(cube((hlo, 17.6, 7.76), (hhi, 20.4, 8.24), 'gold', rot=r))
        barb = 8 + side * 1.0
        blo, bhi = sorted((barb, barb + side * 2.6))
        els.append(cube((blo, 15.0, 7.8), (bhi, 15.7, 8.2), 'gold_dark', rot=(-side * 30, 'z', (barb, 15.35, 8))))
    # Blade: a long leaf of crimson steel, widest above the middle, a diamond point; the ember channel inside.
    els.append(cube((6.9, 17.0, 7.7), (9.1, 19.4, 8.3), 'blade_dark'))
    els.append(cube((6.4, 19.4, 7.68), (9.6, 21.6, 8.32), 'blade'))
    els.append(cube((5.9, 21.6, 7.66), (10.1, 26.4, 8.34), 'blade'))
    els.append(cube((6.4, 26.4, 7.68), (9.6, 28.4, 8.32), 'blade'))
    els.append(cube((6.9, 28.4, 7.7), (9.1, 29.6, 8.3), 'blade'))
    els.append(cube((6.86, 28.46, 7.71), (9.14, 30.74, 8.29), 'blade', rot=(45, 'z', (8, 29.6, 8))))
    els.append(cube((5.88, 21.6, 7.74), (6.1, 26.4, 8.26), 'blade_edge'))
    els.append(cube((9.9, 21.6, 7.74), (10.12, 26.4, 8.26), 'blade_edge'))
    els.append(cube((7.64, 17.4, 7.56), (8.36, 30.0, 8.44), 'ember', light=13))
    # The war pennant on a gold crossbar, hanging from the shaft below the head.
    els.append(cube((8.6, 13.0, 7.85), (14.4, 13.4, 8.15), 'gold'))
    els.append(cube((8.65, 7.6, 7.96), (14.1, 13.0, 8.04), 'pennant', rot=(-12, 'y', (8.65, 10.3, 8)),
                    faces={'east': 'pennant', 'west': 'pennant'}))
    # A tassel on the other side, hanging from the crown.
    els.append(cube((5.95, 12.4, 7.86), (6.25, 16.4, 8.14), 'tassel'))
    els.append(cube((5.7, 10.8, 7.8), (6.5, 12.6, 8.2), 'tassel'))
    return els


def model():
    return {
        'credit': 'Generated by tools/model_gen/red_priest/spear.py',
        'textures': {
            'spear': f'{NS}item/{NAME}_model',
            'particle': f'{NS}item/{NAME}',
        },
        'elements': spear(),
        'display': {
            'gui': {'rotation': [0, 0, -45], 'translation': [0.25, 0.25, 0], 'scale': [0.36, 0.36, 0.36]},
            'ground': {'translation': [0, 2, 0], 'scale': [0.35, 0.35, 0.35]},
            'fixed': {'rotation': [0, 0, -45], 'scale': [0.5, 0.5, 0.5]},
            'head': {'translation': [0, 10, 0], 'scale': [0.6, 0.6, 0.6]},
            # Held upright at the side, the blade above the head (tuned in game).
            'thirdperson_righthand': {'rotation': [90, 90, -15], 'translation': [-4, 4, 1], 'scale': [1.4, 1.4, 1.4]},
            'thirdperson_lefthand': {'rotation': [90, -90, 15], 'translation': [-4, 4, 1], 'scale': [1.4, 1.4, 1.4]},
            'firstperson_righthand': {'rotation': [-10, 90, 25], 'translation': [1.5, 3.2, 0.5], 'scale': [0.45, 0.45, 0.45]},
            'firstperson_lefthand': {'rotation': [-10, -90, -25], 'translation': [1.5, 3.2, 0.5], 'scale': [0.45, 0.45, 0.45]},
        },
    }


def icon():
    """The flat 16x16 picture: the spear from corner to corner."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.line([(1, 14), (9, 6)], fill=(40, 22, 24, 255), width=1)       # shaft
    d.point([(2, 13), (4, 11), (6, 9)], fill=(150, 26, 34, 255))
    d.point([(0, 15)], fill=(90, 84, 94, 255))
    d.line([(3, 12), (4, 11)], fill=(150, 30, 36, 255))
    d.point([(8, 7)], fill=(220, 172, 70, 255))
    d.line([(7, 6), (8, 5)], fill=(220, 172, 70, 255))                # wings
    d.line([(9, 9), (10, 8)], fill=(220, 172, 70, 255))
    d.polygon([(9, 6), (11, 3), (14, 1), (13, 4), (10, 7)], fill=(170, 34, 40, 255))   # blade
    d.line([(10, 6), (13, 2)], fill=(255, 170, 110, 255))
    d.point([(9, 7)], fill=(255, 110, 80, 255))
    d.polygon([(10, 9), (12, 10), (11, 12)], fill=(150, 20, 28, 255))   # pennant
    return img


def main():
    tex = ASSETS / f'textures/item/{NAME}_model.png'
    tex.parent.mkdir(parents=True, exist_ok=True)
    paint().save(tex)
    icon().save(ASSETS / f'textures/item/{NAME}.png')
    out = ASSETS / f'models/item/{NAME}.json'
    m = model()
    out.write_text(json.dumps(m, indent=2) + '\n', encoding='utf-8')
    (ASSETS / f'items/{NAME}.json').write_text(json.dumps(
        {'model': {'type': 'minecraft:model', 'model': f'{NS}item/{NAME}'}}, indent=2) + '\n', encoding='utf-8')
    print(out, len(m['elements']), 'elements')


if __name__ == '__main__':
    main()
