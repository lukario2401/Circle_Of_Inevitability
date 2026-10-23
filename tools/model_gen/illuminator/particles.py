"""The small 2D particle pictures the Illuminator (Paragon pathway) uses (textures/particle/fx/), white and grey so
ParticleFx's .color(...) can tint them (brass, blueprint blue, steam white), like the others already there.

python3 tools/model_gen/illuminator/particles.py src/main/resources/assets/circle_of_inevitability

  gear           a spoked gear wheel, 10 px (golems assembling, Mechanical Body, Mechanical Heaven, the melee combo);
                 fx/cog is the small solid one, both look good mixed
  steam          a soft puff of steam, 16 px (the Mechanical Body's jet, Colossus, the Mythical Creature Form, Heaven)
  equation_0..2  three scraps of written mathematics: an integral, a sum, a square root (Mathematical Maze, Rewrite
                 Principles, Exploit Flaw, the melee combo: pick one per ParticleFx, or pass all three)
  spring         a little coiled spring (a construct taken apart)
  screw          a screw (a construct taken apart, the Golem assembling)
  lens           a magnifying lens (Appraise, Survey, the Appraiser's Eye)
  compass        a drafting compass (Survey, Ritual Circle, the melee combo's blueprint spiral)

fx/page (made by ../knowledge_emperor/particles.py) is the Illuminator's page too.
"""
import math
import os
import sys

from PIL import Image

W = (255, 255, 255, 255)
L = (225, 225, 225, 255)
G = (180, 180, 180, 255)
D = (130, 130, 130, 255)
H = (255, 255, 255, 120)
COLORS = {'W': W, 'L': L, 'G': G, 'D': D, 'H': H}

ART = {
    'gear': [
        "....WW....",
        ".W.WLLW.W.",
        "..WG..GW..",
        ".WG.LL.GW.",
        "WL.LDDL.LW",
        "WL.LDDL.LW",
        ".WG.LL.GW.",
        "..WG..GW..",
        ".W.WLLW.W.",
        "....WW....",
    ],
    'equation_0': [
        "...W....",
        "..W.W...",
        "..W.....",
        "..W..W.W",
        "..W...W.",
        "..W..W.W",
        "W.W.....",
        ".W......",
    ],
    'equation_1': [
        "WWWWW...",
        ".W......",
        "..W..W.W",
        "...W..W.",
        "..W..W.W",
        ".W......",
        "WWWWW...",
        "........",
    ],
    'equation_2': [
        "...WWWWW",
        "...W....",
        "...W.W.W",
        "W..W..W.",
        ".W.W.W.W",
        "..WW....",
        "...W....",
        "........",
    ],
    'spring': [
        "..WWWW..",
        ".W....L.",
        "..LWWW..",
        ".W....L.",
        "..LWWW..",
        ".W....L.",
        "..LWWW..",
        "........",
    ],
    'screw': [
        ".WWWW...",
        ".WDDW...",
        "..WW....",
        "..LW....",
        "..WL....",
        "..LW....",
        "..WL....",
        "...W....",
    ],
    'lens': [
        "..WWW...",
        ".W.HHW..",
        "W.HH..W.",
        "W.H...W.",
        "W.....W.",
        ".W...WL.",
        "..WWWLL.",
        "......LL",
    ],
    'compass': [
        "...WW...",
        "...WW...",
        "...LL...",
        "..W..W..",
        "..W..W..",
        ".W....W.",
        ".W....G.",
        "W.....G.",
    ],
}


def steam():
    """A soft round puff: brightest in the middle, ragged at the edge, a darker underside."""
    size = 16
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    blobs = [(8, 8.5, 5.5), (5, 9.5, 3.5), (11, 9.5, 3.5), (7, 5.5, 3.2), (10.5, 6.5, 2.8)]
    for y in range(size):
        for x in range(size):
            a = 0.0
            for bx, by, r in blobs:
                d = math.hypot(x + 0.5 - bx, y + 0.5 - by) / r
                a = max(a, 1 - d)
            if a <= 0:
                continue
            a = min(1.0, a * 1.8)
            v = int(255 - max(0, (y - 9)) * 9)                              # the shadowed underside
            img.putpixel((x, y), (v, v, v, int(255 * a ** 0.8)))
    return img


def main(assets):
    folder = os.path.join(assets, 'textures/particle/fx')
    os.makedirs(folder, exist_ok=True)
    for name, rows in ART.items():
        size = len(rows)
        img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in COLORS:
                    img.putpixel((x, y), COLORS[ch])
        img.save(os.path.join(folder, name + '.png'))
        print('textures/particle/fx/' + name + '.png')
    steam().save(os.path.join(folder, 'steam.png'))
    print('textures/particle/fx/steam.png')


if __name__ == '__main__':
    main(sys.argv[1])
