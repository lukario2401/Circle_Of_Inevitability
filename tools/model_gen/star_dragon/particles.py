"""The small 2D particle pictures the Star Dragon (Condenser pathway) uses (textures/particle/fx/), white and grey so
ParticleFx's .color(...) can tint them (star white, void violet, ray green, sunlight gold), like the others there.

python3 tools/model_gen/star_dragon/particles.py src/main/resources/assets/circle_of_inevitability

  star4      a four-pointed twinkle, 7 px (Star Chart's constellation, Fate Glimpse, grazed stars, the Dragon)
  chevron    a chevron pointing down (Heaviness: Pressed, Heavy, Heavybringing, the Neutron Star)
  arc_0, 1   two small zigzag arcs of lightning (Electromagnetic Attraction, the storm, Fission Touch)
  nucleus    a cluster of round nucleons (Fission Pulse: the nuclei loosening)
  scale      one scale with a bright rim (Scaled Form shedding, Starlight Scales)
  rock       a chip of stone (the grazed asteroid, Planetfall's debris)
  flare      a soft six-sided lens flare (Sunlight Ray, Radiant Angel, Fusion Cluster)

fx/glow, fx/spark, fx/sparkle, fx/smoke, fx/flake and fx/hourglass, already there, are used too.
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
    'star4': [
        "...W...",
        "...W...",
        "..HWH..",
        "WWWWWWW",
        "..HWH..",
        "...W...",
        "...W...",
    ],
    'chevron': [
        "........",
        "W......W",
        "LW....WL",
        ".LW..WL.",
        "..LWWL..",
        "...WW...",
        "........",
        "........",
    ],
    'arc_0': [
        "...W....",
        "..WL....",
        ".WL.....",
        "WWWWW...",
        "...LW...",
        "...WL...",
        "..WL....",
        "..W.....",
    ],
    'arc_1': [
        ".....W..",
        "....WL..",
        "...WL...",
        "..WWWWW.",
        ".....W..",
        "....WL..",
        "....W...",
        "...W....",
    ],
    'nucleus': [
        "..LW....",
        ".LWWGL..",
        ".WWGGGL.",
        "LWGWWGD.",
        ".GGWWLD.",
        ".LGGDD..",
        "..DD....",
        "........",
    ],
    'scale': [
        "..WWWW..",
        ".WLLLLW.",
        "WLGGGGLW",
        "WLGGGGLW",
        ".WLGGLW.",
        "..WLLW..",
        "...WW...",
        "........",
    ],
    'rock': [
        "........",
        "..GLL...",
        ".GLWLG..",
        "DGLLLGG.",
        "DDGGGGD.",
        ".DDGDD..",
        "..DD....",
        "........",
    ],
}


def flare():
    """A soft hexagon of light, brightest in the middle."""
    size = 16
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for y in range(size):
        for x in range(size):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            a = math.atan2(dy, dx)
            r = math.hypot(dx, dy)
            edge = 7.5 * math.cos(math.pi / 6) / math.cos((a % (math.pi / 3)) - math.pi / 6)
            if r > edge:
                continue
            t = r / edge
            alpha = (1 - t) ** 0.7 * 0.8 + (0.35 if t > 0.85 else 0)
            img.putpixel((x, y), (255, 255, 255, int(255 * min(1.0, alpha))))
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
    flare().save(os.path.join(folder, 'flare.png'))
    print('textures/particle/fx/flare.png')


if __name__ == '__main__':
    main(sys.argv[1])
