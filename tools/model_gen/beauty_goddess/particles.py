"""The small 2D particle pictures the Beauty Goddess uses (textures/particle/fx/), white and grey so ParticleFx's
.color(...) can tint them, like glow, spark and the others already there.

python3 tools/model_gen/beauty_goddess/particles.py src/main/resources/assets/circle_of_inevitability

  bat_0, bat_1   a bat, wings up and wings down: ParticleFx.of("fx/bat_0", "fx/bat_1", "fx/bat_0", ...) flaps
  petal          a rose petal
  crescent       a small crescent moon
  bubble         a bubble with a glint
  droplet        a falling drop (blood, potion)
  sparkle        a four-pointed glint of light
  leaf           a leaf
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
    'bat_0': [
        "................",
        "................",
        ".W............W.",
        ".WW..........WW.",
        ".WWW........WWW.",
        "..WWW.W..W.WWW..",
        "..WWWWWWWWWWWW..",
        "...WWWWLLWWWW...",
        "....WGWLLWGW....",
        ".....G.WW.G.....",
        ".......WW.......",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    'bat_1': [
        "................",
        "................",
        "................",
        "................",
        "......W..W......",
        ".....WWWWWW.....",
        "....WWWLLWWW....",
        "..WWWWWLLWWWWW..",
        ".WWWWWWWWWWWWWW.",
        "WWWG..WWWW..GWWW",
        "WWG....WW....GWW",
        "WG............GW",
        "G..............G",
        "................",
        "................",
        "................",
    ],
    'petal': [
        "........",
        "....WW..",
        "...WWWW.",
        "..WWWLW.",
        ".WWWLL..",
        ".WWLG...",
        "..GG....",
        "........",
    ],
    'crescent': [
        "..WWW...",
        ".WW.....",
        "WW......",
        "WL......",
        "WL......",
        "WW......",
        ".WW.....",
        "..WWW...",
    ],
    'bubble': [
        "..LLLL..",
        ".L....L.",
        "L.WW...L",
        "L.W....L",
        "L......L",
        "L......L",
        ".L....L.",
        "..LLLL..",
    ],
    'droplet': [
        "...W....",
        "...W....",
        "..WWW...",
        "..WWW...",
        ".WWWWW..",
        ".WLWWW..",
        ".WWWWW..",
        "..WWW...",
    ],
    'sparkle': [
        "...W....",
        "...W....",
        "...L....",
        "WWLWLWW.",
        "...L....",
        "...W....",
        "...W....",
        "........",
    ],
    'leaf': [
        "......WW",
        "....WWWL",
        "...WWWLW",
        "..WWWLWW",
        ".WWLWWW.",
        ".WLWWW..",
        ".GWW....",
        "G.......",
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
