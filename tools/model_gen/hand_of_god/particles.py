"""The small 2D particle pictures the Hand Of God uses (textures/particle/fx/), white and grey so ParticleFx's
.color(...) can tint them, like glow, spark, sparkle and the others already there.

python3 tools/model_gen/hand_of_god/particles.py src/main/resources/assets/circle_of_inevitability

  ray            a short shaft of light (Dawn rays, the hurricane, rapiers flashing in)
  rune_0..2      three of the Demon Hunter's angular runes (one picture each: pick one per ParticleFx)
  ash            a ragged flake of ash (Decay, the Twilight Sword)
  hourglass      a tiny hourglass (Passage of Time)
  footprint      a footprint (Tracking: the trail an enemy leaves)
  feather_light  a soft four-pointed mote of Dawn, rounder than sparkle
"""
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
    'ray': [
        "...H....",
        "...L....",
        "...W....",
        "..HWH...",
        "..HWH...",
        "...W....",
        "...L....",
        "...H....",
    ],
    'rune_0': [
        "........",
        ".W...W..",
        ".WW.WW..",
        ".W.W.W..",
        ".W...W..",
        ".W.W.W..",
        "...W....",
        "........",
    ],
    'rune_1': [
        "........",
        "..WWW...",
        ".W..W...",
        "...W....",
        "..WWWW..",
        "...W....",
        "...W....",
        "........",
    ],
    'rune_2': [
        "........",
        ".WWWWW..",
        "...W....",
        "..W.W...",
        ".W...W..",
        ".WWWWW..",
        "........",
        "........",
    ],
    'ash': [
        "........",
        "..GL....",
        ".GLWL...",
        ".LWWWG..",
        "..LWWL..",
        "...GLD..",
        "....D...",
        "........",
    ],
    'hourglass': [
        ".WWWWW..",
        "..LLL...",
        "..LWL...",
        "...W....",
        "..L.L...",
        "..LWL...",
        ".WWWWW..",
        "........",
    ],
    'footprint': [
        "..W.W...",
        ".W.W....",
        "..WWW...",
        ".WWWW...",
        ".WWWW...",
        "..WW....",
        "..WWW...",
        "..WW....",
    ],
    'feather_light': [
        "...W....",
        "..LWL...",
        ".LWWWL..",
        "WWWWWWW.",
        ".LWWWL..",
        "..LWL...",
        "...W....",
        "........",
    ],
}


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


if __name__ == '__main__':
    main(sys.argv[1])
