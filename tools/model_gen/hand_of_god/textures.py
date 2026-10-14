"""Procedural material cells for the Hand Of God (Twilight Giant pathway) model atlases.

Same layout as tools/model_gen/beauty_goddess/textures.py: every model texture is an atlas of 4-wide rows of cells,
32x32 texture pixels each (one cell = 16 model pixels at 2 texture pixels per model pixel). A cube face samples the
part of its material's cell that matches its size, so the texel density stays even; a face listed in a cube's
'full' shows its whole cell, stretched (an emblem, an eye, a face).

The palette: Dawn gold and white light, Silver Knight silver, Glory orange-red (tangerine, the color of dusk),
Demon Hunter dark green, and the Giant's grey-blue skin over blue-black muscle.
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


def edge(img, color, width=1):
    ImageDraw.Draw(img).rectangle([0, 0, CELL - 1, CELL - 1], outline=color, width=width)


def new():
    return Image.new('RGBA', (CELL, CELL), (0, 0, 0, 0))


def vertical_glow(img, dark, light, core_width=0.25):
    """Brightest along the middle column, darker toward both sides: a blade of light seen edge on."""
    for y in range(CELL):
        for x in range(CELL):
            d = abs((x + 0.5) / CELL - 0.5) * 2
            t = max(0.0, 1 - d / (1 - core_width)) if d > core_width else 1.0
            img.putpixel((x, y), mix(dark, light, t))


# =====================================================================================================
# Dawn: the Sword of Dawn, the Dawn Armor, light
# =====================================================================================================

def m_dawn_blade(seed):
    """Solid light: pale gold at the sides, white along the middle, with faint drifting motes."""
    img = new()
    vertical_glow(img, rgba('f0c860', 235), rgba('fffbe8', 250), 0.2)
    rnd = random.Random(seed)
    for _ in range(10):
        img.putpixel((rnd.randrange(CELL), rnd.randrange(CELL)), rgba('ffffff', 255))
    return img


def m_dawn_edge(seed):
    img = new()
    noise_fill(img, rgba('fff0c0', 245), rgba('ffffff', 255), seed, 5, 0.1)
    return img


def m_dawn_fuller(seed):
    """The groove down the blade: a bright white line in a darker gold channel."""
    img = new()
    vertical_glow(img, rgba('d8a040', 240), rgba('ffffff', 255), 0.35)
    return img


def m_dawn_gold(seed):
    """Gilded metal of the guard and pommel, engraved."""
    img = new()
    noise_fill(img, rgba('a8741c'), rgba('ffd870'), seed, 3, 0.3)
    d = ImageDraw.Draw(img)
    for y in range(3, CELL, 7):
        d.line([(0, y), (CELL, y)], fill=rgba('8a5a10'), width=1)
    d.line([(0, 0), (CELL, 0)], fill=rgba('fff0b0'), width=1)
    return img


def m_grip(seed):
    """Leather wound round the grip, with a gold wire."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            band = ((y + x * 0.3) % 6) / 6
            img.putpixel((x, y), mix(rgba('4a2814'), rgba('8a5228'), math.sin(band * math.pi)))
    d = ImageDraw.Draw(img)
    for y in range(2, CELL, 6):
        d.line([(0, y), (CELL, y + 2)], fill=rgba('e0b048'), width=1)
    return img


def m_sun_gem(seed):
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            r = math.hypot(x - 11, y - 11) / 26
            img.putpixel((x, y), mix(rgba('fff6c0'), rgba('e05010'), r))
    return img


def m_dawn_plate(seed):
    """Armor made of Dawn: a pale gold shell of light, a little see-through, brighter at the rim."""
    img = new()
    noise_fill(img, rgba('e8b850', 200), rgba('fff2c0', 230), seed, 6, 0.12)
    edge(img, rgba('fffbe0', 250), 2)
    return img


def m_dawn_trim(seed):
    img = new()
    noise_fill(img, rgba('c8901c', 245), rgba('fff0a0', 255), seed, 3, 0.25)
    d = ImageDraw.Draw(img)
    for x in range(2, CELL, 6):
        d.ellipse([x - 1, 14, x + 1, 16], fill=rgba('ffffff'))                  # rivets of light
    return img


def m_dawn_emblem(seed):
    """Whole-cell: a sun rising over a sword, on the breastplate."""
    img = m_dawn_plate(seed)
    d = ImageDraw.Draw(img)
    for i in range(9):
        a = math.pi + math.pi * i / 8
        d.line([(16, 18), (16 + math.cos(a) * 13, 18 + math.sin(a) * 13)], fill=rgba('fffbe8'), width=1)
    d.pieslice([8, 10, 24, 26], 180, 360, fill=rgba('ffffff'))
    d.rectangle([15, 8, 17, 29], fill=rgba('b07a18'))
    d.rectangle([11, 22, 21, 23], fill=rgba('b07a18'))
    return img


# =====================================================================================================
# Silver Knight: the Silver Armor (the Dawn Armor made solid) and the Silver Rapier
# =====================================================================================================

def m_silver_plate(seed):
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            t = 0.5 + 0.5 * math.sin((x * 0.6 + y * 0.25))
            img.putpixel((x, y), mix(rgba('8a94a8'), rgba('eef2fa'), 0.35 + t * 0.45))
    ImageDraw.Draw(img).line([(3, 0), (12, CELL)], fill=rgba('ffffff', 220), width=2)    # a glint
    edge(img, rgba('5a6274'))
    return img


def m_silver_trim(seed):
    img = new()
    noise_fill(img, rgba('6a7488'), rgba('d8e0f0'), seed, 3, 0.2)
    d = ImageDraw.Draw(img)
    for x in range(2, CELL, 6):
        d.ellipse([x - 1, 14, x + 1, 16], fill=rgba('ffffff'))
    return img


def m_silver_emblem(seed):
    """Whole-cell: the same sun and sword, engraved in dark silver."""
    img = m_silver_plate(seed)
    d = ImageDraw.Draw(img)
    for i in range(9):
        a = math.pi + math.pi * i / 8
        d.line([(16, 18), (16 + math.cos(a) * 13, 18 + math.sin(a) * 13)], fill=rgba('4a5264'), width=1)
    d.pieslice([8, 10, 24, 26], 180, 360, fill=rgba('5a6274'))
    d.rectangle([15, 8, 17, 29], fill=rgba('2a3040'))
    d.rectangle([11, 22, 21, 23], fill=rgba('2a3040'))
    return img


def m_visor(seed):
    img = new()
    noise_fill(img, rgba('0a0a12'), rgba('202030'), seed, 4, 0.2)
    return img


def m_silver_beam(seed):
    """The rapier's blade: a beam of silver-white light."""
    img = new()
    vertical_glow(img, rgba('b8c8ff', 230), rgba('ffffff', 255), 0.3)
    return img


def m_silver_metal(seed):
    img = new()
    noise_fill(img, rgba('9aa4b8'), rgba('f4f8ff'), seed, 3, 0.25)
    return img


def m_silver_dark(seed):
    img = new()
    noise_fill(img, rgba('3a4252'), rgba('6a7488'), seed, 3, 0.25)
    return img


# =====================================================================================================
# Glory and Twilight: the Twilight Sword, the Divine Right Hand
# =====================================================================================================

def m_glory_blade(seed):
    """Orange-red light of dusk, white-gold along the middle, a few dark cracks where it decays."""
    img = new()
    vertical_glow(img, rgba('e0401a', 235), rgba('fff0b0', 250), 0.18)
    cracks(img, rgba('6a1a08', 220), seed, 2, 1, 3)
    return img


def m_glory_edge(seed):
    img = new()
    noise_fill(img, rgba('ffb050', 245), rgba('fff4d0', 255), seed, 4, 0.15)
    return img


def m_decay_metal(seed):
    """Dark, rotting metal of the Twilight Sword's hilt: rust and verdigris."""
    img = new()
    noise_fill(img, rgba('2a1c14'), rgba('6a4a2a'), seed, 2, 0.45)
    rnd = random.Random(seed)
    for _ in range(18):
        img.putpixel((rnd.randrange(CELL), rnd.randrange(CELL)), rgba(rnd.choice(('8a5a2a', '4a6a4a', '1a120c'))))
    return img


def m_ember_gem(seed):
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            r = math.hypot(x - 12, y - 12) / 24
            img.putpixel((x, y), mix(rgba('fff4c8'), rgba('a01808'), r))
    return img


def m_twilight_skin(seed):
    """The Divine Right Hand: made of glowing orange dusk, darker in the folds."""
    img = new()
    noise_fill(img, rgba('d0481c', 230), rgba('ffb860', 245), seed, 5, 0.2)
    cracks(img, rgba('ffe8a0', 250), seed + 1, 3, 1, 4)
    return img


def m_twilight_core(seed):
    img = new()
    noise_fill(img, rgba('ffd890', 250), rgba('ffffff', 255), seed, 4, 0.1)
    return img


def m_twilight_nail(seed):
    img = new()
    noise_fill(img, rgba('ffe0b0', 250), rgba('fff8e8', 255), seed, 3, 0.15)
    edge(img, rgba('c06020', 250))
    return img


# =====================================================================================================
# The Giant (Giant Form): grey-blue skin, blue-black muscle, the black vertical eye
# =====================================================================================================

def m_giant_skin(seed):
    img = new()
    noise_fill(img, rgba('4a5a6e'), rgba('7a8aa0'), seed, 4, 0.3)
    return img


def m_giant_muscle(seed):
    """Blue-black muscle, with symbols of power faintly carved into it."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            fiber = 0.5 + 0.5 * math.sin(x * 0.9 + math.sin(y * 0.25) * 2)
            img.putpixel((x, y), mix(rgba('121a28'), rgba('2e3e56'), fiber))
    d = ImageDraw.Draw(img)
    d.arc([6, 6, 26, 26], 30, 150, fill=rgba('4a7aa0'), width=1)
    d.line([(16, 4), (16, 12)], fill=rgba('4a7aa0'), width=1)
    return img


def m_giant_face(seed):
    """Whole-cell: no eyes, smooth skin, a heavy brow and a wide mouth; the eye rift is a cube of its own."""
    img = m_giant_skin(seed)
    d = ImageDraw.Draw(img)
    d.line([(4, 13), (28, 13)], fill=rgba('34404e'), width=2)                     # brow
    d.line([(9, 24), (23, 24)], fill=rgba('141a22'), width=2)                     # mouth
    d.line([(14, 16), (14, 20)], fill=rgba('3a4656'), width=1)                    # nose
    d.line([(18, 16), (18, 20)], fill=rgba('3a4656'), width=1)
    return img


def m_giant_rift(seed):
    """The black vertical rift on the forehead, glowing cold blue inside."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            d = abs((x + 0.5) / CELL - 0.5) * 2
            img.putpixel((x, y), mix(rgba('a8e8ff'), rgba('04060c'), min(1.0, d * 1.6)))
    return img


def m_loincloth(seed):
    img = new()
    noise_fill(img, rgba('3a2416'), rgba('6a4a2c'), seed, 2, 0.4)
    d = ImageDraw.Draw(img)
    for y in range(4, CELL, 8):
        d.line([(0, y), (CELL, y)], fill=rgba('2a180c'), width=1)
    return img


def m_giant_nail(seed):
    img = new()
    noise_fill(img, rgba('8a8a7a'), rgba('c8c4b0'), seed, 3, 0.2)
    return img


# =====================================================================================================
# Alchemy: the Demon Hunter's vial
# =====================================================================================================

def m_vial_glass(seed):
    img = new()
    noise_fill(img, rgba('a8d8b8', 70), rgba('e8fff0', 110), seed, 6, 0.1)
    d = ImageDraw.Draw(img)
    d.line([(5, 3), (5, 26)], fill=rgba('ffffff', 210), width=2)
    return img


def m_vial_liquid(seed):
    """Nearly white so .color(...) gives the coating's color."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            img.putpixel((x, y), mix(rgba('ffffff', 235), rgba('c4c4c4', 235), y / (CELL - 1)))
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(6):
        x, y = rnd.randrange(3, 29), rnd.randrange(6, 29)
        d.ellipse([x - 1, y - 1, x + 1, y + 1], outline=rgba('ffffff'))
    return img


def m_vial_cap(seed):
    """A dark green wax seal with a hunter's mark."""
    img = new()
    noise_fill(img, rgba('0e3a1e'), rgba('2a7a42'), seed, 3, 0.3)
    d = ImageDraw.Draw(img)
    d.line([(10, 16), (22, 16)], fill=rgba('7aff9a'), width=1)
    d.line([(16, 10), (16, 22)], fill=rgba('7aff9a'), width=1)
    return img


def m_vial_band(seed):
    img = new()
    noise_fill(img, rgba('5a4a2a'), rgba('a88a4a'), seed, 3, 0.3)
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
