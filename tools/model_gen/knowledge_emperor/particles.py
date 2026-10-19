"""The small 2D particle pictures the Knowledge Emperor uses (textures/particle/fx/), white and grey so ParticleFx's
.color(...) can tint them, like glow, spark, sparkle and the others already there.

python3 tools/model_gen/knowledge_emperor/particles.py src/main/resources/assets/circle_of_inevitability

  glyph_0..3     four letters of the Hermit's ancient script (Knowledge Overload's flood, Indoctrination, the
                 melee combo, Informatization: pick one per ParticleFx, or pass all four to animate through them)
  star5          a small five-pointed star (Star Fall's sparks, Star Guard, Constellations)
  ink            a drop of ink (Inscribe Scroll, Spellbolts' trail, the Scrolls Professor)
  page           a tiny written page (Read Scroll burning away, the Knowledge Moor's pages drifting)
  quill          a quill pen (Inscribe Scroll)
  text_line      two short lines of writing (information streaming off a body)
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
    'glyph_0': [
        "........",
        "...W....",
        "..W.W...",
        "..WLW...",
        ".W.W.W..",
        ".WWWWW..",
        "........",
        "........",
    ],
    'glyph_1': [
        "........",
        ".W.W.W..",
        ".WWWWW..",
        "...W....",
        "...W....",
        "..WWW...",
        "........",
        "........",
    ],
    'glyph_2': [
        "........",
        "..WWW...",
        ".W...W..",
        ".W.W.W..",
        "..WWW...",
        "...W....",
        ".WWWWW..",
        "........",
    ],
    'glyph_3': [
        "........",
        ".WWWWW..",
        "..W.W...",
        "...W....",
        "..W.W...",
        ".WWWWW..",
        "........",
        "........",
    ],
    'star5': [
        "...W....",
        "...W....",
        "WWWWWWW.",
        ".LWWWL..",
        "..WWW...",
        ".WL.LW..",
        ".W...W..",
        "........",
    ],
    'ink': [
        "...D....",
        "...G....",
        "..GLG...",
        ".GLWLG..",
        ".LWWWL..",
        ".GLWLG..",
        "..GGG...",
        "........",
    ],
    'page': [
        ".WWWWL..",
        ".WGGWLL.",
        ".WWWWWL.",
        ".WGGGWL.",
        ".WWWWWL.",
        ".WGGWWL.",
        ".WWWWWL.",
        "........",
    ],
    'quill': [
        "......LW",
        ".....LWL",
        "....LWL.",
        "...LWL..",
        "..LWL...",
        "..WL....",
        ".D......",
        "D.......",
    ],
    'text_line': [
        "........",
        "WW.WWW.W",
        "........",
        "........",
        "W.WW.WWW",
        "........",
        "........",
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
