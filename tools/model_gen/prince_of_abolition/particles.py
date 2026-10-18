"""The small 2D particle pictures the Prince Of Abolition uses (textures/particle/fx/), white and grey so ParticleFx's
.color(...) can tint them, like glow, spark, sparkle and the others already there.

python3 tools/model_gen/prince_of_abolition/particles.py src/main/resources/assets/circle_of_inevitability

  coin      a coin seen face on (Bribes, Coin Shot, Avaricious)
  section   the section sign of the law (Lawyer: Verdict, Brainwash, Decree)
  crown     a small crown (Majesty, Coronation, the Order)
  cog       a broken cog (Disorder, Entropy: the order of things coming apart)
  keyhole   a keyhole (Seal, the loophole Verdict finds, a mausoleum's door)
  stele     a tiny mausoleum (raising and crumbling mausoleums, Rebirth)
  twist     a twisted arrow turning back on itself (Distort)
  gavel     a small gavel (Verdict)
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
    'coin': [
        "..LWWL..",
        ".WLGGLW.",
        "WLG..GLW",
        "WG.WW.GW",
        "WG.WW.GW",
        "WLG..GLW",
        ".WLGGLW.",
        "..LWWL..",
    ],
    'section': [
        "..LWWL..",
        ".W....W.",
        ".LWL....",
        ".W..WL..",
        "..LW..W.",
        "....LWL.",
        ".W....W.",
        "..LWWL..",
    ],
    'crown': [
        "........",
        "W..W..W.",
        "WL.W.LW.",
        "WWLWLWW.",
        "WWWWWWW.",
        "WGWGWGW.",
        "WWWWWWW.",
        "........",
    ],
    'cog': [
        "..W.W...",
        ".WWWWW..",
        "WWL.LW..",
        ".WL..L.W",
        "WW...WWW",
        ".WL.LWW.",
        "..WWWW..",
        "...W.W..",
    ],
    'keyhole': [
        "..LWWL..",
        ".WWWWWW.",
        ".WW..WW.",
        ".WW..WW.",
        ".WWW.WW.",
        ".WW...W.",
        ".WW...W.",
        ".WWWWWW.",
    ],
    'stele': [
        "...WL...",
        "..WWWL..",
        "..WWWL..",
        "..WWWL..",
        "..WGWL..",
        "..WGWL..",
        ".LWWWWL.",
        ".WWWWWW.",
    ],
    'twist': [
        "...LWWL.",
        "..W....W",
        "..W....W",
        "...L..W.",
        "..W.LW..",
        ".WWW....",
        "WWWWW...",
        "..W.....",
    ],
    'gavel': [
        "WWW.....",
        "WGWW....",
        "WWWWW...",
        ".WWWWL..",
        "...LWW..",
        "....LWL.",
        ".....LWL",
        "......LW",
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
