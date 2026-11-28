"""The small 2D particle pictures the Observer (Sublunary Eye pathway) uses (textures/particle/fx/observer/), white
and grey so ParticleFx's .color(...) can tint them, like the others there.

python3 tools/model_gen/observer/particles.py src/main/resources/assets/circle_of_inevitability

  brush_0, 1, 2   three brush strokes (painting: the strokes drawn in the air)
  splat           a splash of paint (Riot of Colours, every Painting's colour)
  blot            a round ink blot with a drip (ink: the servants, the figures of fiction)
  eye_shut, eye_open  an eye closing and opening (Observation, the Sight wheel; play them as an animation)
  glyph           a carved totem glyph (the Territory)
  shard           a thin sliver of a higher dimension (Phase, the Shadow, Dimensional Passage)
  fold            a small folded square of paper-space (Fold Space)
  crumb           an eraser crumb (Erase)
  notes           two beamed notes (Harmony, Taut Chord)
  broken_note     a note with a snapped stem (Dissonance)
  wing_glint      a long thin diamond, a glint of a wing (Pixie Wings)
  bud             a closed flower bud (Bloom)

fx/glow, fx/spark, fx/sparkle, fx/smoke, fx/petal, fx/leaf, fx/note, fx/ink, fx/page, fx/feather_light and fx/eye,
already there, are used too.
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw

W = (255, 255, 255, 255)
L = (225, 225, 225, 255)
G = (180, 180, 180, 255)
D = (130, 130, 130, 255)
H = (255, 255, 255, 120)
COLORS = {'W': W, 'L': L, 'G': G, 'D': D, 'H': H}

ART = {
    'eye_shut': [
        "........",
        "........",
        "........",
        "WLLLLLLW",
        ".W.W.W..",
        "........",
        "........",
        "........",
    ],
    'eye_open': [
        "........",
        "..WWWW..",
        ".WL..LW.",
        "WL.GG.LW",
        "WL.GG.LW",
        ".WL..LW.",
        "..WWWW..",
        "........",
    ],
    'glyph': [
        "W......W",
        ".W....W.",
        "..WLLW..",
        "..L..L..",
        "..L..L..",
        "..WLLW..",
        ".W....W.",
        "W......W",
    ],
    'fold': [
        "WWWWWWW.",
        "WLLLLLW.",
        "WLLLLW..",
        "WLLLW.G.",
        "WLLW.GG.",
        "WLW.GGG.",
        "WW.DDDD.",
        "........",
    ],
    'crumb': [
        "......",
        ".LW...",
        ".WWL..",
        "..LGD.",
        "...D..",
        "......",
    ],
    'notes': [
        "..WWWWW.",
        "..W...W.",
        "..W...W.",
        "..W...W.",
        "..W...W.",
        "WWW.WWW.",
        "WWW.WWW.",
        "........",
    ],
    'broken_note': [
        "....W...",
        ".....W..",
        "........",
        "...W....",
        "...W....",
        ".WWW....",
        ".WWW....",
        "........",
    ],
    'wing_glint': [
        "...W....",
        "...W....",
        "..HWH...",
        "..LWL...",
        "..HWH...",
        "...W....",
        "...H....",
        "........",
    ],
    'bud': [
        "...WW...",
        "..WLLW..",
        "..WLLW..",
        "...WW...",
        "...G....",
        "..GG....",
        "...G....",
        "........",
    ],
}


def brush(seed):
    """A brush stroke, 16 px: thick in the middle, dry and broken at the end."""
    size = 16
    img = Image.new('RGBA', (size * 4, size * 4), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rnd = random.Random(seed)
    pts = [(6 + i * 13, 32 + math.sin(i * 0.9 + seed) * 10) for i in range(5)]
    for k in range(9):                                                         # bristles
        off = (k - 4) * 1.6
        dry = rnd.uniform(0.7, 1.0)
        line = [(x, y + off * (0.6 + 0.4 * i / 4)) for i, (x, y) in enumerate(pts)]
        line = line[:max(2, int(len(line) * dry) + 1)]
        d.line(line, fill=(255, 255, 255, rnd.randint(170, 255)), width=3)
    return img.resize((size, size), Image.LANCZOS)


def splat(seed=3):
    size = 16
    img = Image.new('RGBA', (size * 4, size * 4), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rnd = random.Random(seed)
    c = size * 2
    d.ellipse([c - 14, c - 12, c + 13, c + 14], fill=W)
    for _ in range(9):
        a, r = rnd.uniform(0, math.tau), rnd.uniform(14, 28)
        x, y, s = c + math.cos(a) * r, c + math.sin(a) * r, rnd.uniform(2, 6)
        d.line([(c, c), (x, y)], fill=W, width=int(s))
        d.ellipse([x - s, y - s, x + s, y + s], fill=W)
    return img.resize((size, size), Image.LANCZOS)


def blot():
    size = 16
    img = Image.new('RGBA', (size * 4, size * 4), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([10, 6, 54, 46], fill=W)
    d.ellipse([24, 40, 34, 58], fill=W)
    d.ellipse([18, 12, 30, 22], fill=(255, 255, 255, 140))
    return img.resize((size, size), Image.LANCZOS)


def shard():
    size = 16
    img = Image.new('RGBA', (size * 4, size * 4), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(30, 2), (40, 26), (34, 62), (26, 28)], fill=W)
    d.line([(30, 2), (34, 62)], fill=G, width=2)
    return img.resize((size, size), Image.LANCZOS)


def main(assets):
    folder = os.path.join(assets, 'textures/particle/fx/observer')
    os.makedirs(folder, exist_ok=True)
    out = []
    for name, rows in ART.items():
        size = len(rows)
        img = Image.new('RGBA', (len(rows[0]), size), (0, 0, 0, 0))
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in COLORS:
                    img.putpixel((x, y), COLORS[ch])
        img.save(os.path.join(folder, name + '.png'))
        out.append(name)
    for i in range(3):
        brush(i + 1).save(os.path.join(folder, f'brush_{i}.png'))
        out.append(f'brush_{i}')
    for name, make in (('splat', splat), ('blot', blot), ('shard', shard)):
        make().save(os.path.join(folder, name + '.png'))
        out.append(name)
    return ['textures/particle/fx/observer/' + n + '.png' for n in out]


if __name__ == '__main__':
    for p in main(sys.argv[1]):
        print(p)
