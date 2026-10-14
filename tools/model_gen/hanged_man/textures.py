"""Procedural material cells for the Hanged Man model atlases.

Every model texture is a 128x128 atlas of 4x4 cells, 32x32 texture pixels each (one cell = 16 model pixels at
2 texture pixels per model pixel). A cube face samples the part of its material's cell that matches its size,
so the texel density stays even.
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
    return tuple(int(round(c1[i] + (c2[i] - c1[i]) * t)) for i in range(4))


def value_noise(seed, scale):
    rnd = random.Random(seed)
    grid = [[rnd.random() for _ in range(CELL // scale + 2)] for _ in range(CELL // scale + 2)]

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
            img.putpixel((x, y), mix(dark, light, max(0.0, min(1.0, t))))


def veins(img, color, seed, count=3, width=1):
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(count):
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        angle = rnd.random() * math.tau
        pts = [(x, y)]
        for _ in range(9):
            angle += rnd.uniform(-0.8, 0.8)
            x += math.cos(angle) * 4
            y += math.sin(angle) * 4
            pts.append((x, y))
        d.line(pts, fill=color, width=width)


def edge(img, color, width=1):
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, CELL - 1, CELL - 1], outline=color, width=width)


# ---------------------------------------------------------------- the materials

def m_crimson_crystal(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('4a0612'), rgba('d0303f'), seed, 6, 0.1)
    d = ImageDraw.Draw(img)
    for i in range(0, CELL, 8):                      # facet lines
        d.line([(i, 0), (i + 16, CELL)], fill=rgba('ff8a90', 200), width=1)
    d.line([(2, 2), (10, 2)], fill=rgba('ffd0d0'), width=1)
    return img


def m_crystal_core(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('d01830'), rgba('ff9aa4'), seed, 4, 0.2)
    return img


def m_dark_metal(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('1c1a1e'), rgba('3e3a42'), seed, 8, 0.15)
    edge(img, rgba('6a6470'))
    return img


def m_silver(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('6c6a74'), rgba('c8c6d0'), seed, 2, 0.4)
    for y in range(0, CELL, 4):                      # chain links
        ImageDraw.Draw(img).line([(0, y), (CELL, y)], fill=rgba('3a3840'), width=1)
    return img


def m_shadow(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('050307'), rgba('241420'), seed, 5, 0.3)
    return img


def m_shadow_vein(seed):
    img = m_shadow(seed)
    veins(img, rgba('6a0c1a'), seed + 3, 4)
    veins(img, rgba('a01426'), seed + 9, 1)
    return img


def m_shadow_pool(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('020103', 230), rgba('1a0c16', 200), seed, 6, 0.2)
    return img


def m_flesh(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('7a2632'), rgba('d07a80'), seed, 4, 0.3)
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(6):                               # darker blotches
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        r = rnd.randint(1, 3)
        d.ellipse([x - r, y - r, x + r, y + r], fill=rgba('5a141e'))
    veins(img, rgba('4a0a16'), seed + 5, 3)
    return img


def m_flesh_glass(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('5e1220', 120), rgba('a83a4a', 160), seed, 5, 0.2)
    veins(img, rgba('5a0a18', 220), seed + 2, 5)
    veins(img, rgba('b01c30', 230), seed + 4, 2)
    edge(img, rgba('c04050', 190))
    return img


def m_vein(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('3a0610'), rgba('8a1424'), seed, 2, 0.4)
    return img


def m_bone(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('b8ae96'), rgba('ede6d4'), seed, 4, 0.2)
    return img


def m_lip(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('5a1020'), rgba('a83444'), seed, 3, 0.3)
    return img


def m_void(seed):
    return Image.new('RGBA', (CELL, CELL), rgba('060104'))


def m_teeth(seed):
    img = Image.new('RGBA', (CELL, CELL), rgba('e4dcc4'))
    d = ImageDraw.Draw(img)
    for x in range(0, CELL, 3):
        d.line([(x, 0), (x, CELL)], fill=rgba('8a7e66'), width=1)
    return img


def m_black_steel(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('08080c'), rgba('22222c'), seed, 8, 0.12)
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(5):                               # scratches
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        d.line([(x, y), (x + rnd.randint(2, 6), y + rnd.randint(-2, 2))], fill=rgba('3a3a48'), width=1)
    edge(img, rgba('4a4658'))
    return img


def m_crimson_trim(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('3e0610'), rgba('8a1424'), seed, 6, 0.15)
    edge(img, rgba('c02a3a'))
    return img


def m_sigil(seed):
    img = m_black_steel(seed)
    d = ImageDraw.Draw(img)
    c = rgba('e0283c')
    # the sigil: a ring, a hanging line from its top and an inverted triangle (the Hanged Man), in the 12x12 corner
    # that a 6x6-pixel plate shows
    d.ellipse([1, 1, 10, 10], outline=c, width=1)
    d.line([(5, 0), (5, 4)], fill=c, width=1)
    d.polygon([(3, 4), (8, 4), (5, 9)], outline=c)
    return img


def m_red_glow(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('d01020'), rgba('ff7080'), seed, 4, 0.2)
    return img


def m_shadow_face(seed):
    """A whole-cell face (mapped stretched onto the front of a head): two slit eyes and a jagged grin."""
    img = m_shadow(seed)
    d = ImageDraw.Draw(img)
    eye = rgba('ff2a3a')
    core = rgba('ffd0d0')
    for x0 in (5, 19):
        d.polygon([(x0, 12), (x0 + 8, 10), (x0 + 8, 14), (x0, 15)], fill=eye)
        d.line([(x0 + 2, 13), (x0 + 6, 12)], fill=core, width=1)
    pts = [(6, 22), (9, 25), (12, 22), (15, 26), (18, 22), (21, 25), (24, 22), (26, 23)]
    d.line(pts, fill=rgba('b01426'), width=2)
    return img


def m_mouth_dark(seed):
    img = Image.new('RGBA', (CELL, CELL))
    noise_fill(img, rgba('140208'), rgba('3a0812'), seed, 3, 0.3)
    return img


MATERIALS = {
    'crimson_crystal': m_crimson_crystal,
    'crystal_core': m_crystal_core,
    'dark_metal': m_dark_metal,
    'silver': m_silver,
    'shadow': m_shadow,
    'shadow_vein': m_shadow_vein,
    'shadow_pool': m_shadow_pool,
    'flesh': m_flesh,
    'flesh_glass': m_flesh_glass,
    'vein': m_vein,
    'bone': m_bone,
    'lip': m_lip,
    'void': m_void,
    'teeth': m_teeth,
    'black_steel': m_black_steel,
    'crimson_trim': m_crimson_trim,
    'sigil': m_sigil,
    'red_glow': m_red_glow,
    'shadow_face': m_shadow_face,
    'mouth_dark': m_mouth_dark,
}


def atlas(names, seed=1):
    """An atlas with the named materials, in order, one per cell. Returns (image, {name: (cx, cy)})."""
    if len(names) > GRID * GRID:
        raise ValueError('too many materials')
    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    cells = {}
    for i, name in enumerate(names):
        cx, cy = i % GRID, i // GRID
        img.paste(MATERIALS[name](seed * 31 + i), (cx * CELL, cy * CELL))
        cells[name] = (cx, cy)
    return img, cells
