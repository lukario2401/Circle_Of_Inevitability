"""The small 2D particle pictures the Knight Of Misfortune uses (textures/particle/fx/), white and grey so ParticleFx's
.color(...) can tint them, like glow, spark, sparkle and the others already there.

python3 tools/model_gen/knight_of_misfortune/particles.py src/main/resources/assets/circle_of_inevitability

  note        a musical note (Midnight Poems, Requiem)
  sleep_z     a "z" of sleep (Lullaby, Slumber, Eternal Rest)
  wisp        a curling spirit wisp (spirits, the soul out of its body, Concealment's fog)
  hair        a curling strand of hair (Hair Entanglement)
  tooth       a small tooth (Spirit Commanding: spirits sealed in teeth)
  horseshoe   a broken horseshoe (Misfortune and its mishaps)
  skull       a tiny skull (Horror, the Sword of Darkness's Despair)
  eye         a small open eye (Spirit Vision, Horror Aura, Fear)
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
    'note': [
        "....WWW.",
        "....W.LW",
        "....W...",
        "....W...",
        "..LWW...",
        ".WWWW...",
        ".WWWL...",
        "..LL....",
    ],
    'sleep_z': [
        "........",
        ".WWWWW..",
        "....LW..",
        "...WL...",
        "..LW....",
        ".WL.....",
        ".WWWWW..",
        "........",
    ],
    'wisp': [
        "...HLH..",
        "..LW.W..",
        "..W..L..",
        "..LW....",
        "...LWH..",
        "....LW..",
        "...HW...",
        "..HL....",
    ],
    'hair': [
        "W.......",
        ".L......",
        ".W......",
        "..L.....",
        "...W....",
        "...L....",
        "....WL..",
        "......LW",
    ],
    'tooth': [
        "........",
        "..LWWL..",
        ".LWWWWL.",
        ".WWWWWW.",
        ".LWWWWL.",
        "..WLLW..",
        "..W..W..",
        "..G..G..",
    ],
    'horseshoe': [
        "..LWWL..",
        ".WG..GW.",
        "WG....GW",
        "W......W",
        "W.......",
        "W.....W.",
        "WW....W.",
        "........",
    ],
    'skull': [
        "..LWWL..",
        ".WWWWWW.",
        "WWWWWWWW",
        "W..WW..W",
        "W..WW..W",
        ".WWW.WW.",
        "..WLWL..",
        "..L.L...",
    ],
    'eye': [
        "........",
        "..LWWL..",
        ".WGWWGW.",
        "WG.DD.GW",
        "WG.DD.GW",
        ".WGWWGW.",
        "..LWWL..",
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
