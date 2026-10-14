"""16x16 pixel-art screen pictures for the Hanged Man (textures/gui/hanged_man/).

python3 icons.py <assets/circle_of_inevitability dir> [preview.png]
"""
import os
import sys

from PIL import Image

PALETTE = {
    '.': (0, 0, 0, 0),
    'k': (26, 10, 16, 255),       # outline
    'r': (122, 15, 30, 255),      # crimson
    'R': (208, 40, 60, 255),      # bright red
    'H': (255, 150, 160, 255),    # red highlight
    'p': (170, 80, 90, 255),      # flesh
    'P': (232, 168, 172, 255),    # light flesh
    's': (42, 26, 38, 255),       # shadow
    'S': (78, 52, 72, 255),       # light shadow
    'w': (236, 230, 216, 255),    # bone white
    'g': (110, 106, 124, 255),    # grey
    'G': (200, 198, 208, 255),    # light grey
    'y': (200, 160, 80, 255),     # candle flame
    'Y': (255, 230, 150, 255),    # flame core
    'b': (120, 160, 200, 255),    # soul
    'B': (220, 236, 248, 255),    # soul light
    'd': (50, 74, 104, 255),      # soul dark
    'x': (0, 0, 0, 120),          # dim (empty slot)
    'X': (120, 120, 132, 160),    # dim outline
}

ICONS = {
    # Divination: the spirit pendulum, mid-swing
    'divination': [
        "......kkkk......",
        ".....kGGGGk.....",
        "......kGGk......",
        ".......kGk......",
        "........kGk.....",
        "........kGk.....",
        ".........kGk....",
        ".........kgk....",
        "........kkkkk...",
        ".......krRRRrk..",
        ".......kRHRRRk..",
        ".......krRRRrk..",
        "........krRrk...",
        ".........kRk....",
        "..........k.....",
        "................",
    ],
    # Ritual Circle: a ring of runes with a candle in the middle
    'ritual': [
        "................",
        ".....kkkkkk.....",
        "...kkrRrrRrkk...",
        "..krRk.kk.kRrk..",
        "..kRk..kYk.kRk..",
        ".krk...kyk..krk.",
        ".kRk..kwwk..kRk.",
        ".krk..kwwk..krk.",
        ".kRk..kwwk..kRk.",
        ".krk..kwwk..krk.",
        "..kRk.kkkk.kRk..",
        "..krRk....kRrk..",
        "...kkrRrrRrkk...",
        ".....kkkkkk.....",
        "................",
        "................",
    ],
    # Listen: an ear with sound waves
    'listen': [
        "................",
        "....kkkkk.......",
        "...kPPPPPk...k..",
        "..kPppppPPk...k.",
        "..kPpkkkpPk.k..k",
        "..kPpk..kPk..k.k",
        "..kPpk.kPPk..k.k",
        "...kPpkPPk..k..k",
        "...kPpPPk..k..k.",
        "....kPpk.......k",
        "....kPpk........",
        "...kPPk.........",
        "...kPpk.........",
        "....kk..........",
        "................",
        "................",
    ],
    # Whispers (the mark): a mouth with teeth
    'whispers': [
        "................",
        "................",
        "................",
        "....kkkkkkkk....",
        "..kkrRRRRRRrkk..",
        ".krRRRRRRRRRRrk.",
        "krrkwkwkwkwkwkrk",
        "krk..........krk",
        "krk...ssss...krk",
        "krrkwkwkwkwkwkrk",
        ".krRRRRRRRRRRrk.",
        "..kkrRRRRRRrkk..",
        "....kkkkkkkk....",
        "................",
        "................",
        "................",
    ],
    # Shadow Lurking: a figure sinking into a pool of shadow
    'shadow_lurking': [
        "................",
        "................",
        "......kkkk......",
        ".....kSSSSk.....",
        ".....kSRSRk.....",
        ".....kSSSSk.....",
        "......kkkk......",
        "....kkSSSSkk....",
        "...kSSsSSsSSk...",
        "..kSkSsSSsSkSk..",
        ".kkkkkkkkkkkkkk.",
        "kssssssssssssssk",
        "ksSsssSsssSsssSk",
        ".kssssssssssssk.",
        "..kkkkkkkkkkkk..",
        "................",
    ],
    # Flesh Cloak: a body wrapped in flesh and cords
    'flesh_cloak': [
        "......kkkk......",
        ".....kPPPPk.....",
        ".....kPrrPk.....",
        ".....kPPPPk.....",
        "...kkkkkkkkkk...",
        "..kPPpPPPPpPPk..",
        "..kprrrrrrrrpk..",
        "..kPPpPPPPpPPk..",
        "..kPkPPpPPPkPk..",
        "..kpkrrrrrrkpk..",
        "..kPkPPpPPPkPk..",
        "....kPPkkPPk....",
        "....kprk.kprk...",
        "....kPPk.kPPk...",
        "....kkkk.kkkk...",
        "................",
    ],
    # Black Armor: a black knight's helm with a red visor
    'black_armor': [
        ".......kk.......",
        "......krRk......",
        ".....krRRrk.....",
        "....kkkkkkkk....",
        "...ksSSSSSSsk...",
        "..ksSSSSSSSSsk..",
        "..ksSssssssSsk..",
        "..kRRRRRRRRRRk..",
        "..kHRRRRRRRRHk..",
        "..ksSssssssSsk..",
        "..ksSSskksSSsk..",
        "..ksSSskksSSsk..",
        "...ksSskksSsk...",
        "....kkskkskk....",
        "......kkkk......",
        "................",
    ],
    # Trinity Body: three heads of shadow
    'trinity': [
        "................",
        "......kkkk......",
        ".....kSSSSk.....",
        ".....kRSSRk.....",
        "..kkkkSSSSkkkk..",
        ".kSSSkkSSkkSSSk.",
        ".kRSRk.kk.kRSRk.",
        ".kSSSk....kSSSk.",
        "..kkSkkkkkkSkk..",
        "...kSSSSSSSSk...",
        "...kSsSSSSsSk...",
        "...kSsSSSSsSk...",
        "...kSSsSSsSSk...",
        "....kSSkkSSk....",
        "....kkk..kkk....",
        "................",
    ],
    # Grazing: a stolen soul
    'soul': [
        "................",
        ".......kk.......",
        "......kBBk......",
        ".....kBBBBk.....",
        "....kBBbbBBk....",
        "...kBbbBBbbBk...",
        "...kBbkBBkbBk...",
        "...kBbbBBbbBk...",
        "...kBbbbbbbBk...",
        "....kBbkkbBk....",
        "....kBbbbbbk....",
        ".....kbbdbk.....",
        ".....kbdkbk.....",
        "......kdkdk.....",
        ".......k........",
        "................",
    ],
    # Grazing wheel: an empty slot
    'slot_empty': [
        "................",
        "................",
        "....X.X..X.X....",
        "................",
        "..X..........X..",
        "................",
        "..X..........X..",
        "................",
        "................",
        "..X..........X..",
        "................",
        "..X..........X..",
        "................",
        "....X.X..X.X....",
        "................",
        "................",
    ],
    # Grazing wheel: the badge of a stolen sneak cast
    'sneak_badge': [
        "................",
        "................",
        "................",
        "................",
        "....kkkkkkkk....",
        "...kRRRRRRRRk...",
        "...kRwwwwwwRk...",
        "...kRwRRRRRRk...",
        "...kRwwwwwwRk...",
        "...kRRRRRRwRk...",
        "...kRwwwwwwRk...",
        "...kRRRRRRRRk...",
        "....kkkkkkkk....",
        "................",
        "................",
        "................",
    ],
    # Grazing wheel: "this one will be replaced"
    'replace': [
        "................",
        ".kk..........kk.",
        "kRRk........kRRk",
        "kRRRk......kRRRk",
        ".kRRRk....kRRRk.",
        "..kRRRk..kRRRk..",
        "...kRRRkkRRRk...",
        "....kRRRRRRk....",
        "....kRRRRRRk....",
        "...kRRRkkRRRk...",
        "..kRRRk..kRRRk..",
        ".kRRRk....kRRRk.",
        "kRRRk......kRRRk",
        "kRRk........kRRk",
        ".kk..........kk.",
        "................",
    ],
}


def build(name):
    rows = ICONS[name]
    if len(rows) != 16 or any(len(r) != 16 for r in rows):
        raise ValueError(f'{name}: not 16x16 ({[len(r) for r in rows]})')
    img = Image.new('RGBA', (16, 16))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            img.putpixel((x, y), PALETTE[ch])
    return img


def main(assets, preview=None):
    folder = os.path.join(assets, 'textures/gui/hanged_man')
    os.makedirs(folder, exist_ok=True)
    images = []
    for name in ICONS:
        img = build(name)
        img.save(os.path.join(folder, name + '.png'))
        images.append(img)
        print(name)
    if preview:
        sheet = Image.new('RGBA', (len(images) * 136, 136), (40, 40, 48, 255))
        for i, img in enumerate(images):
            big = img.resize((128, 128), Image.NEAREST)
            sheet.paste(big, (i * 136 + 4, 4), big)
        sheet.save(preview)


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None)
