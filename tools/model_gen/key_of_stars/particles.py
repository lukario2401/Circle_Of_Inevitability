"""The small 2D particle pictures the Key Of Stars uses (textures/particle/fx/), white and grey so ParticleFx's
.color(...) can tint them, like star4, sparkle, keyhole, padlock, page and quill already there (it uses those too).

python3 tools/model_gen/key_of_stars/particles.py src/main/resources/assets/circle_of_inevitability

  star_door    a tiny arched door (Door Opening's and Traveler's Door's sparks, Transfiguration's torn layers)
  star_worm    a crescent Worm of Star (the Secrets Sorcerer's aura, Starlight Seal, Conceptualization)
  star_key     a little key (Slip Free, Starlight Seal, Close the Door)
  wind_key     a toy's butterfly key (Toy Box)
  sound_wave   three arcs of sound (Loud Noise, Spiritual Intuition's chime)
  planet       a ringed planet (Space Fragmentation, Dimension Sight)
  rift_crack   a jagged crack in space (Space Tearing, the storm)
  maze         a square of maze (the Space-Time Labyrinth)
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
    'star_door': [
        "..WWWW..",
        ".W....W.",
        ".W....W.",
        ".W..L.W.",
        ".W..G.W.",
        ".W....W.",
        ".W....W.",
        "WWWWWWWW",
    ],
    'star_worm': [
        "........",
        "..WWW...",
        ".WL..W..",
        "WL....H.",
        "WL......",
        "WL....H.",
        ".WL.LW..",
        "..WWW...",
    ],
    'star_key': [
        ".WWW....",
        "W.H.W...",
        "WH.HW...",
        ".WWWL...",
        "...LL...",
        "...LLGG.",
        "...LL...",
        "...LLG..",
    ],
    'wind_key': [
        "WW....WW",
        "WLW..WLW",
        "WLLWWLLW",
        "WLW..WLW",
        "WW.GG.WW",
        "...GG...",
        "...DD...",
        "...DD...",
    ],
    'sound_wave': [
        "W..H....",
        ".W..H..H",
        ".W..L...",
        "..W..L.H",
        "..W..L.H",
        ".W..L...",
        ".W..H..H",
        "W..H....",
    ],
    'planet': [
        "........",
        "...LL..H",
        "..LWWLH.",
        "HGWWWWG.",
        ".GWWWWGH",
        ".HLWWL..",
        "H..LL...",
        "........",
    ],
    'rift_crack': [
        "...W....",
        "...WL...",
        "..WW....",
        "...LW...",
        "....WW..",
        "...WL...",
        "..WW....",
        "...H....",
    ],
    'maze': [
        "WWWW.WWW",
        "W......W",
        "W.WWWW.W",
        "W.W..W.W",
        "W.W.WW.W",
        "W.W....W",
        "W.WWWWWW",
        "W.......",
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
