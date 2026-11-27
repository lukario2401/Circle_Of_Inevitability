"""The shadow soldiers of the Red Priest uniqueness: the dead of old wars, clawing out of the ground.

One rig for every rank; the model (client/red_priest/ShadowSoldierModel) shows or hides bones by rank:
  0 Levy       kettle hat, rag tunic, wrapped legs, a chipped short sword
  1 Legionary  closed helm, breastplate, greaves, a spear and a round shield
  2 Centurion  + crest, pauldrons, a tattered cape
  3 Warlord    a crown of spikes instead of the crest, no shield, drawn larger
The bodies are smoke given shape: near-black, veined with dull red, with burning eyes (an emissive layer).
Every foot is ONE cube (no separate toes).

Run from any directory: python tools/model_gen/red_priest/shadow_soldier.py   (Pillow required)
Writes red_priest/shadow_soldier.rig.json (CubeRig, Java model pixels: y down, feet at y 24) and the atlases
textures/entity/red_priest/<palette>.png and <palette>_emissive.png for each palette (shadow, usurper, loyal).
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parents[3] / 'src/main/resources/assets/circle_of_inevitability'
SIZE = 256

# Materials per palette. 'eye' is the only one drawn again, full bright, in the emissive atlas.
PALETTES = {
    # the shadows that hunt the bearer
    'shadow': dict(shadow=(22, 18, 24), vein=(110, 20, 28), plate=(46, 40, 46), rim=(120, 36, 40),
                   helm=(40, 35, 41), visor=(6, 4, 6), eye=(255, 52, 36), tabard=(92, 14, 22), leather=(44, 30, 28),
                   plume=(150, 22, 30), gold=(136, 98, 46), cape=(58, 10, 16), shield=(40, 28, 26),
                   sigil=(150, 24, 30), shaft=(36, 26, 24), blade=(92, 88, 96), edge=(190, 70, 64)),
    # the one that rises in the bearer's place (Final Conquest)
    'usurper': dict(shadow=(30, 8, 12), vein=(200, 40, 36), plate=(84, 16, 22), rim=(230, 160, 70),
                    helm=(74, 14, 20), visor=(10, 0, 2), eye=(255, 210, 120), tabard=(150, 20, 28),
                    leather=(52, 22, 20), plume=(220, 170, 70), gold=(228, 176, 72), cape=(120, 12, 20),
                    shield=(70, 20, 22), sigil=(230, 170, 70), shaft=(40, 20, 22), blade=(170, 36, 40),
                    edge=(255, 180, 120)),
    # the legion a Seq 0 cast raises for its caster
    'loyal': dict(shadow=(18, 18, 26), vein=(170, 120, 50), plate=(48, 44, 52), rim=(210, 164, 72),
                  helm=(42, 40, 48), visor=(4, 4, 8), eye=(255, 206, 92), tabard=(120, 22, 28), leather=(46, 34, 30),
                  plume=(214, 168, 70), gold=(222, 172, 70), cape=(92, 14, 22), shield=(44, 32, 30),
                  sigil=(222, 172, 70), shaft=(38, 28, 26), blade=(120, 116, 126), edge=(255, 214, 140)),
}


def box(x, y, z, w, h, d, material):
    return dict(origin=[x, y, z], size=[w, h, d], material=material)


class Rig:
    def __init__(self):
        self.bones = []

    def bone(self, name, parent, pivot, cubes, rotation=(0, 0, 0)):
        self.bones.append(dict(name=name, parent=parent, pivot=list(pivot), rotation=list(rotation), cubes=cubes))


def soldier():
    rig = Rig()
    # ---- body
    rig.bone('body', None, (0, 0, 0), [box(-4, 0, -2, 8, 12, 4, 'shadow'),
                                        box(-4.5, 9.5, -2.5, 9, 2, 5, 'leather'),       # belt
                                        box(-1, 9.8, -2.9, 2, 2, 1, 'gold')])           # buckle
    rig.bone('skirt', 'body', (0, 11, 0), [box(-3.5, 0, -2.8, 7, 7, 1, 'tabard'),
                                           box(-3.5, 0, 1.8, 7, 6, 1, 'tabard')])
    rig.bone('tunic', 'body', (0, 0, 0), [box(-4.5, -0.2, -2.5, 9, 10, 5, 'tabard'),
                                          box(-2, 0.5, -2.8, 4, 3, 1, 'leather')])        # laced collar
    rig.bone('breastplate', 'body', (0, 0, 0), [box(-4.5, -0.3, -2.6, 9, 8, 5, 'plate'),
                                                box(-3.5, 1, -3.1, 7, 5, 1, 'rim'),       # raised chest ridge
                                                box(-4.5, 7.7, -2.6, 9, 2, 5, 'plate')])
    rig.bone('cape', 'body', (0, 0.5, 2.6), [box(-4.5, 0, 0, 9, 18, 1, 'cape'),
                                             box(-5, -0.8, -0.6, 10, 2, 2, 'gold')], (8, 0, 0))
    # ---- head (the shadow head, then whichever helmet the rank wears)
    rig.bone('head', None, (0, 0, 0), [box(-4, -8, -4, 8, 8, 8, 'shadow')])
    rig.bone('face_eyes', 'head', (0, 0, 0), [box(-3, -5, -4.2, 2, 1, 1, 'eye'), box(1, -5, -4.2, 2, 1, 1, 'eye')])
    rig.bone('kettle', 'head', (0, 0, 0), [box(-4.5, -9, -4.5, 9, 3, 9, 'helm'),
                                           box(-6, -6.5, -6, 12, 1, 12, 'helm')])          # wide brim
    rig.bone('helm', 'head', (0, 0, 0), [box(-4.6, -8.6, -4.6, 9, 9, 9, 'helm'),
                                         box(-3.5, -5.6, -5.1, 7, 2, 1, 'visor'),         # eye slit
                                         box(-0.5, -6.1, -5.4, 1, 4, 1, 'rim'),           # nose guard
                                         box(-4.6, -3.6, -5.1, 2, 4, 1, 'helm'),          # cheek guards
                                         box(2.6, -3.6, -5.1, 2, 4, 1, 'helm'),
                                         box(-4.6, -9.2, -0.5, 9, 1, 1, 'rim')])           # brow band over the top
    rig.bone('helm_eyes', 'head', (0, 0, 0), [box(-3, -5.3, -5.25, 2, 1, 1, 'eye'), box(1, -5.3, -5.25, 2, 1, 1, 'eye')])
    rig.bone('crest', 'head', (0, -9, 0), [box(-1, -1, -4.5, 2, 1, 9, 'rim'),
                                           box(-0.5, -5, -5.5, 1, 4, 11, 'plume')])
    rig.bone('crown', 'head', (0, -8, 0), [box(-5, -1.5, -5, 10, 2, 10, 'gold')]
             + [box(x - .5, -4.5, z - .5, 1, 3, 1, 'gold') for x, z in ((-4.5, -4.5), (4.5, -4.5), (-4.5, 4.5), (4.5, 4.5))]
             + [box(-1, -6, -5.4, 2, 5, 1, 'gold'), box(-0.5, -4, -5.7, 1, 1, 1, 'eye'),
                box(-4.5, -4, -1, 1, 3, 2, 'gold'), box(3.5, -4, -1, 1, 3, 2, 'gold')])
    # ---- arms
    for side, name in ((-1, 'right'), (1, 'left')):
        arm = name + '_arm'
        out = -3 if side < 0 else -1          # the arm cube's left edge in its own space
        rig.bone(arm, None, (side * 5, 2, 0), [box(out, -2, -2, 4, 12, 4, 'shadow')])
        rig.bone(name + '_bracer', arm, (0, 0, 0), [box(out - .5, 5, -2.5, 5, 4, 5, 'leather')])
        rig.bone(name + '_gauntlet', arm, (0, 0, 0), [box(out - .5, 5, -2.5, 5, 5, 5, 'plate'),
                                                      box(out - .6, 5, -2.6, 5, 1, 5, 'rim')])
        px = -4.5 if side < 0 else -1.5
        rig.bone(name + '_pauldron', arm, (0, 0, 0), [box(px, -3.2, -3, 6, 4, 6, 'plate'),
                                                      box(px + (0 if side < 0 else 1), -3.6, -2, 5, 1, 4, 'rim')],
                 (0, 0, side * 10))
    # ---- legs (each foot is a single cube)
    for side, name in ((-1, 'right'), (1, 'left')):
        leg = name + '_leg'
        rig.bone(leg, None, (side * 1.9, 12, 0), [box(-2, 0, -2, 4, 12, 4, 'shadow')])
        rig.bone(name + '_wraps', leg, (0, 0, 0), [box(-2.4, 6, -2.4, 5, 5, 5, 'leather'),
                                                   box(-2.4, 10, -3.4, 5, 2, 6, 'leather')])        # foot
        rig.bone(name + '_greave', leg, (0, 0, 0), [box(-2.5, 4.5, -2.6, 5, 6, 5, 'plate'),
                                                    box(-2.6, 4.2, -2.8, 5, 1, 1, 'rim'),
                                                    box(-2.5, 10, -3.6, 5, 2, 6, 'plate')])       # foot
    # ---- weapons, carried by the arms (the hand is at y 10 of the arm)
    rig.bone('spear', 'right_arm', (-1, 9.5, 0), [box(-0.5, -0.5, -17, 1, 1, 27, 'shaft'),
                                                  box(-0.5, -1.5, -23, 1, 3, 6, 'blade'),
                                                  box(-0.5, -0.5, -25, 1, 1, 2, 'edge'),
                                                  box(-2, -0.5, -17.5, 4, 1, 1, 'gold'),
                                                  box(-0.5, 0.5, -16.5, 1, 3, 1, 'plume')])
    rig.bone('sword', 'right_arm', (-1, 9.5, 0), [box(-0.5, -0.5, -1.5, 1, 1, 3, 'leather'),
                                                  box(-2, -0.5, -2.5, 4, 1, 1, 'rim'),
                                                  box(-0.5, -1, -11.5, 1, 2, 9, 'blade'),
                                                  box(-0.5, -0.5, -12.5, 1, 1, 1, 'edge')])
    rig.bone('shield', 'left_arm', (3, 5, 0), [box(0, -6, -5, 1, 12, 10, 'shield'),
                                               box(0.5, -6.5, -5.5, 1, 1, 11, 'rim'),
                                               box(0.5, 5.5, -5.5, 1, 1, 11, 'rim'),
                                               box(1, -1.5, -1.5, 1, 3, 3, 'gold')])
    return rig


def paint_face(img, rect, mat, pal, face, seed):
    x0, y0, w, h = rect
    rnd = random.Random(seed)
    base = pal[mat]
    light = {'up': 1.12, 'down': .7, 'front': 1.0, 'back': .9, 'side': .94}[face]
    for j in range(h):
        for i in range(w):
            t = j / max(1, h - 1)
            c = [v * light for v in base]
            n = rnd.uniform(-1, 1)
            a = 255
            if mat == 'shadow':
                # smoke: dark mottling, a few veins of dull red, lighter where the smoke thins
                c = [v * (1 + .25 * n) for v in c]
                if (i * 7 + j * 3 + seed) % 23 == 0 or ((i + seed) % 9 == 0 and (j + i // 3) % 5 == 0):
                    c = list(pal['vein'])
            elif mat in ('plate', 'helm'):
                c = [v * (1.15 - .3 * t + .06 * n) for v in c]
                if j == 0 or i == 0 or i == w - 1:
                    c = [v * .85 + r * .15 for v, r in zip(c, pal['rim'])]
                if rnd.random() < .05:
                    c = [v * .6 for v in c]                                       # dents and pits
            elif mat == 'rim':
                c = [v * (1.1 - .25 * t + .05 * n) for v in c]
            elif mat in ('tabard', 'cape'):
                c = [v * (1 - .3 * t + .08 * n) for v in c]
                if (i + seed) % 4 == 0:
                    c = [v * .82 for v in c]                                       # folds
                if face in ('front', 'back') and j >= h - 3 and rnd.random() < (.35 if j == h - 1 else .15):
                    a = 0                                                          # ragged hem
            elif mat == 'plume':
                c = [v * (1.1 - .3 * ((i + j) % 3 == 0) + .05 * n) for v in c]
            elif mat == 'eye':
                c = list(pal['eye'])
            elif mat == 'visor':
                c = [v + 2 * n for v in c]
            elif mat == 'shield':
                c = [v * (1 + .1 * n) for v in c]
                if face in ('side',) and w >= 8:
                    cx, cy = (w - 1) / 2, (h - 1) / 2
                    # the war sigil of the old army: a burning ring and a crossed spear
                    r = math.hypot(i - cx, j - cy)
                    if abs(r - min(w, h) * .32) < .7 or abs(i - cx) < .6 or abs(j - cy) < .6 and r < min(w, h) * .32:
                        c = list(pal['sigil'])
            elif mat == 'blade':
                c = [v * (1.1 - .2 * (i % 2) + .05 * n) for v in c]
                if j in (0, h - 1):
                    c = list(pal['edge'])
            elif mat == 'leather':
                c = [v * (1 + .12 * n - .15 * ((i + j) % 4 == 0)) for v in c]
            elif mat == 'gold':
                c = [v * (1.2 - .35 * t + .05 * n) for v in c]
            elif mat == 'shaft':
                c = [v * (1 + .1 * n - .12 * (i % 3 == 0)) for v in c]
            img.putpixel((x0 + i, y0 + j), tuple(max(0, min(255, int(v))) for v in c) + (a,))


def export(rig):
    # Pack every cube's box-uv area once (shared by every palette).
    cubes = [c for bone in rig.bones for c in bone['cubes']]
    dims = {id(c): tuple(int(math.ceil(v)) for v in c['size']) for c in cubes}
    order = sorted(cubes, key=lambda c: -(dims[id(c)][2] + dims[id(c)][1]))     # tallest first, on shelves
    x = y = row = 0
    layout = []
    for cube in order:
        w, h, d = dims[id(cube)]
        uw, uh = 2 * (w + d), d + h
        if x + uw > SIZE:
            x, y, row = 0, y + row + 1, 0
        assert y + uh <= SIZE // 2, 'atlas full'
        cube['uv'] = [x, y]
        layout.append((cube, x, y, w, h, d))
        x += uw + 1
        row = max(row, uh)
    out_tex = ASSETS / 'textures/entity/red_priest'
    out_tex.mkdir(parents=True, exist_ok=True)
    for name, pal in PALETTES.items():
        atlas = Image.new('RGBA', (SIZE, SIZE // 2))
        glow = Image.new('RGBA', (SIZE, SIZE // 2))
        for n, (cube, x, y, w, h, d) in enumerate(layout):
            mat = cube['material']
            faces = [((x + d, y, w, d), 'up'), ((x + d + w, y, w, d), 'down'),
                     ((x, y + d, d, h), 'side'), ((x + d, y + d, w, h), 'front'),
                     ((x + d + w, y + d, d, h), 'side'), ((x + d + w + d, y + d, w, h), 'back')]
            for rect, face in faces:
                paint_face(atlas, rect, mat, pal, face, n * 31 + rect[0] + rect[1])
                if mat == 'eye':
                    paint_face(glow, rect, mat, pal, face, n)
        atlas.save(out_tex / f'{name}.png', optimize=True)
        glow.save(out_tex / f'{name}_emissive.png', optimize=True)
    out = ASSETS / 'red_priest'
    out.mkdir(exist_ok=True)
    bones = []
    for bone in rig.bones:
        header = {k: v for k, v in bone.items() if k != 'cubes'}
        cubes = ',\n'.join('        ' + json.dumps({k: v for k, v in c.items() if k != 'material'} | {'material': c['material']})
                           for c in bone['cubes'])
        bones.append('    ' + json.dumps(header)[:-1] + ', "cubes": [\n' + cubes + '\n    ]}')
    (out / 'shadow_soldier.rig.json').write_text('{\n  "texture_size": [%d, %d],\n  "bones": [\n' % (SIZE, SIZE // 2)
                                                 + ',\n'.join(bones) + '\n  ]\n}\n')
    print(out / 'shadow_soldier.rig.json', sum(len(b['cubes']) for b in rig.bones), 'cubes')


if __name__ == '__main__':
    export(soldier())
