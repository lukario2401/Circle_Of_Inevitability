"""Procedural material cells for the Hand Of Order (Justiciar pathway) model atlases.

Same layout as tools/model_gen/knowledge_emperor/textures.py: every model texture is an atlas of 4-wide rows of cells,
32x32 texture pixels each (one cell = 16 model pixels at 2 texture pixels per model pixel). A cube face samples the
part of its material's cell that matches its size, so the texel density stays even; a face listed in a cube's
'full' shows its whole cell, stretched.

The palette (docs/HAND_OF_ORDER.md, "Theme"): brass (0xC9A24B) and white-gold (0xFFF3D0) for Order and the Brass
Pillar, cold blue-white (0xA8C8FF) for Psychic lightning, iron grey (0x3E434D) for Rules and chains, dark red
(0xB03030) for Punishment and Death, dark violet (0x5A3A7A) for Disorder.
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


def new():
    return Image.new('RGBA', (CELL, CELL), (0, 0, 0, 0))


def radial(img, inner, outer, power=1.0):
    for y in range(CELL):
        for x in range(CELL):
            d = math.hypot((x + 0.5) / CELL - 0.5, (y + 0.5) / CELL - 0.5) / 0.7071
            img.putpixel((x, y), mix(inner, outer, d ** power))


def sheen(img, color, every=7, alpha=120):
    """Diagonal streaks of polish across a metal."""
    d = ImageDraw.Draw(img)
    for x in range(-CELL, CELL, every):
        d.line([(x, CELL - 1), (x + CELL, -1)], fill=color[:3] + (alpha,))


def law_lines(img, color, seed, top=4, bottom=28, gap=5):
    """Engraved lines of law: short strokes with a section mark now and then."""
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for y in range(top, bottom, gap):
        x = 4 + rnd.randrange(0, 2)
        while x < 27:
            w = min(rnd.randrange(2, 6), 28 - x)
            d.line([(x, y), (x + w - 1, y)], fill=color)
            x += w + rnd.randrange(1, 3)


# =====================================================================================================
# Metals: brass, white-gold, iron
# =====================================================================================================

def m_brass(seed):
    """Polished brass: warm, a little uneven, with streaks of polish."""
    img = new()
    noise_fill(img, rgba('8a6a24'), rgba('e8c870'), seed, 4, 0.2)
    sheen(img, rgba('fff0b8'), 8, 90)
    return img


def m_brass_dark(seed):
    """Old, darkened brass for the deep parts: bases, the backs of things."""
    img = new()
    noise_fill(img, rgba('4a3612'), rgba('9a7a34'), seed, 3, 0.25)
    return img


def m_brass_rune(seed):
    """Brass with lines of law cut into it, the cuts glowing faintly white-gold."""
    img = m_brass(seed)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, CELL - 1, CELL - 1], outline=rgba('5a4214'))
    law_lines(img, rgba('fff3d0'), seed, 5, 28, 6)
    for y in (2, 29):
        d.line([(2, y), (29, y)], fill=rgba('5a4214'))
    return img


def m_brass_fluted(seed):
    """The pillar's shaft: vertical flutes of brass, light and shade in turn."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            flute = 0.5 + 0.5 * math.cos((x + 0.5) / CELL * math.tau * 4)
            img.putpixel((x, y), mix(rgba('6a4c18'), rgba('f0d080'), 0.25 + 0.7 * flute))
    rnd = random.Random(seed)
    for _ in range(18):
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        img.putpixel((x, y), rgba('fff0b8'))
    return img


def m_white_gold(seed):
    """White-gold light: the glow of Order, brightest at the heart."""
    img = new()
    radial(img, rgba('ffffff', 255), rgba('ffd27a', 235), 1.3)
    return img


def m_iron(seed):
    """Cold iron grey for Rules, chains and frames."""
    img = new()
    noise_fill(img, rgba('2a2e36'), rgba('6a707c'), seed, 3, 0.3)
    sheen(img, rgba('a8b0c0'), 9, 60)
    return img


def m_iron_dark(seed):
    """Blackened iron, rivets in the corners."""
    img = new()
    noise_fill(img, rgba('16181e'), rgba('3e434d'), seed, 3, 0.3)
    d = ImageDraw.Draw(img)
    for x, y in ((3, 3), (28, 3), (3, 28), (28, 28)):
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=rgba('7a808c'))
    return img


def m_chain(seed):
    """Iron links seen from the side, bright and shadowed in turn."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            link = (y // 4) % 2
            t = (0.65 if link else 0.3) + 0.3 * (1 - abs((x + 0.5) / CELL - 0.5) * 2)
            img.putpixel((x, y), mix(rgba('1e2026'), rgba('a0a8b8'), t))
    return img


# =====================================================================================================
# Glass, light and lightning
# =====================================================================================================

def m_glass(seed):
    """A transparent pane: barely tinted, brighter at the edges, one streak of reflected light."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            e = min(x, y, CELL - 1 - x, CELL - 1 - y)
            a = 70 if e > 2 else 150
            img.putpixel((x, y), (220, 236, 255, a))
    d = ImageDraw.Draw(img)
    d.line([(6, 26), (22, 6)], fill=(255, 255, 255, 170), width=2)
    d.line([(12, 27), (26, 10)], fill=(255, 255, 255, 90))
    return img


def m_lightning(seed):
    """Psychic lightning: blue-white, crossed by bright forks."""
    img = new()
    radial(img, rgba('e8f2ff', 245), rgba('5a7ad8', 220), 0.9)
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(3):
        x, y = rnd.randrange(CELL), 0
        while y < CELL:
            nx, ny = x + rnd.randrange(-5, 6), y + rnd.randrange(3, 8)
            d.line([(x, y), (nx, ny)], fill=rgba('ffffff'))
            x, y = nx, ny
    return img


def m_spectral(seed):
    """A spectral body: pale white-gold, see-through, brighter at its outline."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            e = min(x, y, CELL - 1 - x, CELL - 1 - y) / 6.0
            t = max(0.0, 1 - e)
            img.putpixel((x, y), mix(rgba('fff3d0', 140), rgba('ffffff', 235), t))
    return img


def m_ember(seed):
    """The brand's head: white-hot at the heart, red at the rim (it is illusory: tint it lightning blue)."""
    img = new()
    radial(img, rgba('ffffff'), rgba('d04020'), 0.8)
    d = ImageDraw.Draw(img)
    d.line([(16, 6), (16, 26)], fill=rgba('fff0c0'), width=3)
    d.line([(6, 16), (26, 16)], fill=rgba('fff0c0'), width=3)
    return img


# =====================================================================================================
# Wood, leather, paper, wax
# =====================================================================================================

def m_leather(seed):
    """A grip wound with dark red leather."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            band = ((y + x // 3) % 6) / 6.0
            img.putpixel((x, y), mix(rgba('3a1210'), rgba('8a2a24'), 0.3 + 0.6 * band))
    return img


def m_wood(seed):
    """Dark polished wood of a gavel's head."""
    img = new()
    big = value_noise(seed, 3)
    for y in range(CELL):
        for x in range(CELL):
            grain = 0.5 + 0.5 * math.sin(y * 0.9 + big(x, y) * 5)
            img.putpixel((x, y), mix(rgba('2a140a'), rgba('6a3a1e'), grain))
    return img


def m_verdict_red(seed):
    """Dark red: the color of the sentence."""
    img = new()
    noise_fill(img, rgba('5a1010'), rgba('c03838'), seed, 3, 0.2)
    return img


def m_marble(seed):
    """Pale stone of the court, veined with brass."""
    img = new()
    noise_fill(img, rgba('cfc8b8'), rgba('f8f4ea'), seed, 5, 0.15)
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    x, y = 0, rnd.randrange(CELL)
    while x < CELL:
        nx, ny = x + rnd.randrange(3, 7), y + rnd.randrange(-4, 5)
        d.line([(x, y), (nx, ny)], fill=rgba('c9a24b', 160))
        x, y = nx, ny
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
