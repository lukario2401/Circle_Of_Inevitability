"""The small 2D particle pictures the Hand Of Order uses (textures/particle/fx/), white and grey so ParticleFx's
.color(...) can tint them, like glow, spark, gavel, chain_link and the others already there.

python3 tools/model_gen/hand_of_order/particles.py src/main/resources/assets/circle_of_inevitability

  law_0..law_2   three letters of the law: a section mark, a pillar, three bars (Rules being written, Decree edges,
                 the melee combo's ring of glyphs, the Court's clauses)
  prohibit       a ring struck through (a refused teleport, flight, hiding or stand-in; a Violation)
  shackle        an open iron cuff (Imprison, Halt, Contract's hold)
  brand          a glowing cross-brand (Psychic Lashing's hits, the branding iron's sparks)
  bolt           a small fork of lightning (Psychic lightning: Lashing, Piercing, Whip of Pain)
  verdict        a seal stamp (a Verdict delivered, the Contract signed)
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
    'law_0': [
        "...WW...",
        "..W..W..",
        "...WW...",
        "..W..W..",
        "...WW...",
        "..W..W..",
        "...WW...",
        "........",
    ],
    'law_1': [
        ".WWWWWW.",
        "..W..W..",
        "..W..W..",
        "..W..W..",
        "..W..W..",
        "..W..W..",
        ".WWWWWW.",
        "........",
    ],
    'law_2': [
        "........",
        ".WWWWWW.",
        "........",
        ".WWWWWW.",
        "........",
        ".WWWWWW.",
        "........",
        "........",
    ],
    'prohibit': [
        "..WWWW..",
        ".WW...W.",
        "W.WW...W",
        "W..WW..W",
        "W...WW.W",
        "W....WWW",
        ".W...WW.",
        "..WWWW..",
    ],
    'shackle': [
        "..LWWL..",
        ".W....W.",
        "W......W",
        "W......W",
        "GG....GG",
        "WWW..WWW",
        "DGG..GGD",
        "........",
    ],
    'brand': [
        "...HH...",
        "...WW...",
        "...WW...",
        "HWWWWWWH",
        "HWWWWWWH",
        "...WW...",
        "...WW...",
        "...HH...",
    ],
    'bolt': [
        "....WW..",
        "...WW...",
        "..WW....",
        ".WWWWW..",
        "....WW..",
        "...WW...",
        "..WL....",
        "..H.....",
    ],
    'verdict': [
        "..GGGG..",
        "..GLLG..",
        "...GG...",
        "...GG...",
        ".WWWWWW.",
        "WWLWWLWW",
        "WWWWWWWW",
        ".DDDDDD.",
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
