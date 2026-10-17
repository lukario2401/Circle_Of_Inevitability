"""The small 2D particle pictures the Abomination uses (textures/particle/fx/), white and grey so ParticleFx's
.color(...) can tint them, like glow, spark, sparkle and the others already there.

python3 tools/model_gen/abomination/particles.py src/main/resources/assets/circle_of_inevitability

  chain_link   one link of chain (Blood Chain, Restraint, Grave Shackles breaking)
  thorn        a curved thorn (Thorn Garden, the Mausoleum, Chained's thorn passive)
  bandage      a ragged strip of bandage, fluttering (Coffin of Ages, the Mausoleum)
  curse_0..1   two curse glyphs (one picture each: pick one per ParticleFx)
  claw         three short scratches (Rending Pounce, the Werewolf's claws)
  padlock      a small padlock (Restraint locking, Rite of Silence, a connection made)
  stitch       an X of thread (Rite of Silence's hush, the doll)

It also uses ones already there: glow, smoke, spark, droplet (blood), flake (frost), wisp (ghosts), petal (roses),
crescent (the moon), skull (death) and eye.
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
    'chain_link': [
        "........",
        "..LWWL..",
        ".W....W.",
        ".W....W.",
        ".W....W.",
        ".W....W.",
        "..LWWL..",
        "........",
    ],
    'thorn': [
        "......W.",
        ".....WL.",
        "....WL..",
        "...LWG..",
        "..LWWG..",
        ".GWWD...",
        ".DGD....",
        "........",
    ],
    'bandage': [
        "........",
        "WLL.....",
        "LWWLL...",
        ".GLWWLL.",
        "...GLWWL",
        ".....GLL",
        ".......G",
        "........",
    ],
    'curse_0': [
        "...W....",
        "..WHW...",
        ".W.W.W..",
        "W.WWW.W.",
        ".W.W.W..",
        "..W.W...",
        ".W...W..",
        "........",
    ],
    'curse_1': [
        "W.....W.",
        ".W...W..",
        "..WWW...",
        "..W.W...",
        "..WWW...",
        ".W...W..",
        "W..W..W.",
        "...W....",
    ],
    'claw': [
        "W..W..W.",
        "W..W..W.",
        ".L..L..L",
        ".W..W..W",
        ".W..W..W",
        "..L..L..",
        "..G..G..",
        "........",
    ],
    'padlock': [
        "..WWW...",
        ".W...W..",
        ".W...W..",
        "LWWWWWL.",
        "LWWDWWL.",
        "LWWDWWL.",
        "LLLLLLL.",
        "........",
    ],
    'stitch': [
        "W.....W.",
        ".W...W..",
        "..W.W...",
        "...W....",
        "..W.W...",
        ".W...W..",
        "W.....W.",
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
