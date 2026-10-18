"""Procedural material cells for the Prince Of Abolition (Black Emperor pathway) model atlases.

Same layout as tools/model_gen/knight_of_misfortune/textures.py: every model texture is an atlas of 4-wide rows of
cells, 32x32 texture pixels each (one cell = 16 model pixels at 2 texture pixels per model pixel). A cube face samples
the part of its material's cell that matches its size, so the texel density stays even; a face listed in a cube's
'full' shows its whole cell, stretched (a coin's face, a door, an eye).

The palette: jet black (0x16121F, the Pathway's color) for the mausoleums, the gavel and the door, imperial gold
(0xE0B04A) for every order, seal and the crown, ash grey for Disorder and Entropy, and the colorless fog of a world
that is not the real one for Abolition.
"""
import math
import random

from PIL import Image, ImageDraw

CELL = 32
GRID = 4
SIZE = CELL * GRID


def rgba(hexstr, a=255):
    hexstr = hexstr.lstrip('#')
    return (int(hexstr[0:2], 16), int(hexstr[2:4], 16), int(hexstr[4:6], 16), a)


def mix(c1, c2, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(c1[i] + (c2[i] - c1[i]) * t)) for i in range(4))


def value_noise(seed, scale):
    rnd = random.Random(seed)
    n = CELL // max(1, scale) + 3
    grid = [[rnd.random() for _ in range(n)] for _ in range(n)]

    def at(x, y):
        gx, gy = x / scale, y / scale
        x0, y0 = int(gx), int(gy)
        fx, fy = gx - x0, gy - y0
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        a = grid[y0][x0] + (grid[y0][x0 + 1] - grid[y0][x0]) * fx
        b = grid[y0 + 1][x0] + (grid[y0 + 1][x0 + 1] - grid[y0 + 1][x0]) * fx
        return a + (b - a) * fy
    return at


def noise_fill(img, dark, light, seed, scale=4, fine=0.25):
    big = value_noise(seed, scale)
    rnd = random.Random(seed * 7 + 1)
    for y in range(CELL):
        for x in range(CELL):
            t = big(x, y) * (1 - fine) + rnd.random() * fine
            img.putpixel((x, y), mix(dark, light, t))


def cracks(img, color, seed, count=3, width=1, step=4):
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(count):
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        angle = rnd.random() * math.tau
        pts = [(x, y)]
        for _ in range(7):
            angle += rnd.uniform(-1.0, 1.0)
            x += math.cos(angle) * step
            y += math.sin(angle) * step
            pts.append((x, y))
        d.line(pts, fill=color, width=width)


def new():
    return Image.new('RGBA', (CELL, CELL), (0, 0, 0, 0))


def vertical_glow(img, dark, light, core_width=0.25):
    """Brightest along the middle column, darker toward both sides."""
    for y in range(CELL):
        for x in range(CELL):
            d = abs((x + 0.5) / CELL - 0.5) * 2
            t = max(0.0, 1 - d / (1 - core_width)) if d > core_width else 1.0
            img.putpixel((x, y), mix(dark, light, t))


# =====================================================================================================
# Jet black and imperial gold: the mausoleums, the gavel, the crown
# =====================================================================================================

def m_jet(seed):
    """Jet black stone: near-black with a faint violet sheen and fine grey veins."""
    img = new()
    noise_fill(img, rgba('0c0a12'), rgba('2a2238'), seed, 4, 0.2)
    cracks(img, rgba('3c3450', 160), seed + 2, 3, 1, 4)
    return img


def m_jet_polished(seed):
    """Lacquered black: smooth, with one soft highlight running across it."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            h = math.exp(-((x - y * 0.6 - 8) / 5.0) ** 2)
            img.putpixel((x, y), mix(rgba('100c18'), rgba('5a4e70'), h * 0.8))
    return img


def m_jet_cracked(seed):
    """Jet black stone breaking up: wide grey cracks, edges flaking to ash."""
    img = m_jet(seed)
    cracks(img, rgba('6a6474'), seed + 9, 5, 1, 4)
    cracks(img, rgba('a09aa8', 200), seed + 11, 2, 1, 3)
    return img


def m_gold(seed):
    """Imperial gold, brushed: warm, with streaks along its length and a bright edge at the top."""
    img = new()
    rnd = random.Random(seed)
    for y in range(CELL):
        for x in range(CELL):
            streak = 0.5 + 0.5 * math.sin(y * 0.9 + rnd.random() * 0.4)
            t = 0.35 + 0.45 * streak + (0.2 if y < 3 else 0.0)
            img.putpixel((x, y), mix(rgba('8a6420'), rgba('ffe08a'), t))
    return img


def m_gold_dark(seed):
    """Old gold: deeper, a little tarnished."""
    img = new()
    noise_fill(img, rgba('5a3e14'), rgba('c8963a'), seed, 3, 0.3)
    return img


def m_gold_engraved(seed):
    """Gold with a band of engraved script (the law, written round a mausoleum or a crown)."""
    img = m_gold(seed)
    d = ImageDraw.Draw(img)
    rnd = random.Random(seed)
    for row in (8, 20):
        x = 1
        while x < CELL - 2:
            w = rnd.randint(2, 4)
            k = rnd.randrange(4)
            if k == 0:
                d.line([(x, row), (x + w, row + 4)], fill=rgba('5a3e14'))
            elif k == 1:
                d.arc([x, row, x + w, row + 4], 0, 300, fill=rgba('5a3e14'))
            elif k == 2:
                d.line([(x, row + 2), (x + w, row + 2)], fill=rgba('5a3e14'))
                d.point((x + w // 2, row), fill=rgba('5a3e14'))
            else:
                d.line([(x, row + 4), (x + w // 2, row), (x + w, row + 4)], fill=rgba('5a3e14'))
            x += w + 2
    return img


def m_onyx(seed):
    """A black gem with a gold glint in one corner and a violet depth."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            r = math.hypot(x - 10, y - 10) / 30
            img.putpixel((x, y), mix(rgba('c8a0ff'), rgba('0a0610'), r * 2.2))
    d = ImageDraw.Draw(img)
    d.line([(5, 6), (11, 6)], fill=rgba('fff0c0'))
    return img


def m_velvet(seed):
    """The crown's cap: deep violet velvet."""
    img = new()
    noise_fill(img, rgba('1a0e28'), rgba('3e2a5a'), seed, 3, 0.35)
    return img


def m_ebony(seed):
    """The gavel's handle: black wood with a fine straight grain."""
    img = new()
    rnd = random.Random(seed)
    for x in range(CELL):
        shade = rnd.uniform(0, 1)
        for y in range(CELL):
            t = shade * 0.6 + 0.2 * math.sin(y * 0.3 + x)
            img.putpixel((x, y), mix(rgba('0e0a0c'), rgba('3a2a26'), t))
    return img


# =====================================================================================================
# The illusory door: black fog behind a gold frame, light behind it
# =====================================================================================================

def m_door_fog(seed):
    """The leaf of the illusory door: black fog with gold flecks, thicker toward the middle."""
    img = new()
    big = value_noise(seed, 5)
    rnd = random.Random(seed)
    for y in range(CELL):
        for x in range(CELL):
            t = big(x, y)
            img.putpixel((x, y), mix(rgba('08060e', 235), rgba('2e2440', 210), t))
    for _ in range(14):
        img.putpixel((rnd.randrange(CELL), rnd.randrange(CELL)), rgba('e0b04a'))
    return img


def m_door_panel(seed):
    """The door's face (shown whole): two raised panels outlined in gold, a gold keyhole between them."""
    img = m_door_fog(seed)
    d = ImageDraw.Draw(img)
    gold = rgba('e0b04a')
    d.rectangle([4, 3, 27, 13], outline=gold)
    d.rectangle([4, 18, 27, 28], outline=gold)
    d.ellipse([14, 13, 18, 17], fill=gold)
    d.polygon([(15, 16), (17, 16), (18, 20), (14, 20)], fill=gold)
    return img


def m_door_light(seed):
    """What is behind the door: pale gold light, brightest in the middle."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            r = math.hypot((x - 15.5) / 16, (y - 15.5) / 16)
            img.putpixel((x, y), mix(rgba('fff4d0', 240), rgba('a07020', 200), r))
    return img


def m_mausoleum_door(seed):
    """A mausoleum's door (shown whole): black, a gold crown over a keyhole, a gold border."""
    img = new()
    noise_fill(img, rgba('06040a'), rgba('1c1626'), seed, 3, 0.2)
    d = ImageDraw.Draw(img)
    gold = rgba('e0b04a')
    d.rectangle([1, 1, 30, 30], outline=gold)
    d.polygon([(9, 12), (9, 6), (12, 9), (16, 4), (20, 9), (23, 6), (23, 12)], fill=gold)          # a crown
    d.ellipse([14, 16, 18, 20], fill=gold)                                                         # a keyhole
    d.polygon([(15, 19), (17, 19), (18, 25), (14, 25)], fill=gold)
    return img


# =====================================================================================================
# Coin, flower, bolt
# =====================================================================================================

def m_coin_face(seed):
    """A gold coin (shown whole): a rim, a crown stamped in the middle, a ring of tiny marks."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            r = math.hypot(x - 15.5, y - 15.5) / 16
            img.putpixel((x, y), mix(rgba('ffe9a0'), rgba('9a6c1e'), r))
    d = ImageDraw.Draw(img)
    d.ellipse([1, 1, 30, 30], outline=rgba('7a5414'), width=2)
    for i in range(16):
        a = math.tau * i / 16
        d.point((15.5 + math.cos(a) * 11, 15.5 + math.sin(a) * 11), fill=rgba('7a5414'))
    d.polygon([(10, 19), (10, 12), (13, 15), (16, 10), (19, 15), (22, 12), (22, 19)], fill=rgba('7a5414'))
    return img


def m_coin_edge(seed):
    """A coin's milled edge."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            img.putpixel((x, y), rgba('c8963a') if x % 3 else rgba('7a5414'))
    return img


def m_petal(seed):
    """A black petal edged in gold."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            edge = min(x, y, CELL - 1 - x, CELL - 1 - y) / (CELL / 2)
            col = rgba('e0b04a') if edge < 0.14 else mix(rgba('3a2a50'), rgba('0a0610'), edge)
            img.putpixel((x, y), col)
    return img


def m_stem(seed):
    img = new()
    noise_fill(img, rgba('0a1a10'), rgba('2a3a24'), seed, 2, 0.3)
    return img


def m_bolt(seed):
    """A black bolt of Frenzy: black at the heart, a violet-white rim of light."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            d = abs((x + 0.5) / CELL - 0.5) * 2
            img.putpixel((x, y), mix(rgba('06040a'), rgba('e8d8ff'), max(0.0, (d - 0.55) / 0.45)))
    return img


# =====================================================================================================
# The fake self of Paces: ash shadow with gold eyes
# =====================================================================================================

def m_shadow(seed):
    """A body of shadow: ash-black with grains of grey drifting up through it, a little see-through."""
    img = new()
    big = value_noise(seed, 4)
    rnd = random.Random(seed)
    for y in range(CELL):
        for x in range(CELL):
            t = big(x, y)
            img.putpixel((x, y), mix(rgba('08060c', 235), rgba('2e2a38', 210), t))
    for _ in range(18):
        img.putpixel((rnd.randrange(CELL), rnd.randrange(CELL)), rgba('5a5468', 220))
    return img


def m_shadow_face(seed):
    """The shadow's face: no features but two gold eyes."""
    img = m_shadow(seed)
    d = ImageDraw.Draw(img)
    for x in (9, 20):
        d.rectangle([x, 13, x + 3, 15], fill=rgba('ffd870'))
    return img


def m_mantle(seed):
    """The shadow's mantle: black with a gold hem."""
    img = new()
    noise_fill(img, rgba('06040a'), rgba('201a2a'), seed, 4, 0.2)
    d = ImageDraw.Draw(img)
    d.rectangle([0, CELL - 4, CELL, CELL], fill=rgba('b8862e'))
    return img


MATERIALS = {name[2:]: fn for name, fn in list(globals().items()) if name.startswith('m_') and callable(fn)}


def atlas(names, seed=1):
    """An atlas with the named materials, in order, one per cell. Returns (image, {name: (cx, cy)})."""
    if len(names) > GRID * GRID:
        raise ValueError('too many materials')
    rows = (len(names) + GRID - 1) // GRID
    img = Image.new('RGBA', (SIZE, CELL * rows), (0, 0, 0, 0))
    cells = {}
    for i, name in enumerate(names):
        cx, cy = i % GRID, i // GRID
        img.paste(MATERIALS[name](seed * 31 + i), (cx * CELL, cy * CELL))
        cells[name] = (cx, cy)
    return img, cells
