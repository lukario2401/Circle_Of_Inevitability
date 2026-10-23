"""Procedural material cells for the Illuminator (Paragon pathway) model atlases.

Same layout as tools/model_gen/knowledge_emperor/textures.py: every model texture is an atlas of 4-wide rows of cells,
32x32 texture pixels each (one cell = 16 model pixels at 2 texture pixels per model pixel). A cube face samples the
part of its material's cell that matches its size, so the texel density stays even; a face listed in a cube's
'full' (or given another material through faces={...}) shows its whole cell, stretched (a gauge, a lens, a page).

The palette (docs/ILLUMINATOR.md, "Theme"): brass and copper for crafting and machines, blueprint blue for marks,
lines and equations, pale steam white for fog, parchment cream for pages and scrolls, ink blue-black for the
Paragon's depth. Pathway color 0xD9A441.
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
    """Brightest in the middle of the cell, darker toward the corners."""
    for y in range(CELL):
        for x in range(CELL):
            d = math.hypot((x + 0.5) / CELL - 0.5, (y + 0.5) / CELL - 0.5) / 0.7071
            img.putpixel((x, y), mix(inner, outer, d ** power))


def vertical_glow(img, dark, light, core_width=0.25):
    """Brightest along the middle column, darker toward both sides."""
    for y in range(CELL):
        for x in range(CELL):
            d = abs((x + 0.5) / CELL - 0.5) * 2
            t = max(0.0, 1 - d / (1 - core_width)) if d > core_width else 1.0
            img.putpixel((x, y), mix(dark, light, t))


def sheen(img, color, step=7, slope=4):
    """Diagonal highlights across polished metal."""
    d = ImageDraw.Draw(img)
    for x in range(-CELL, CELL, step):
        d.line([(x, 0), (x + slope, CELL - 1)], fill=color)


def rivets(img, color, shadow, spacing=8, margin=3):
    d = ImageDraw.Draw(img)
    for y in range(margin, CELL, spacing):
        for x in range(margin, CELL, spacing):
            d.point((x, y), fill=color)
            d.point((x + 1, y + 1), fill=shadow)


def writing(img, color, seed, top=3, bottom=29, left=3, right=29, gap=4):
    """Lines of small hand-written script."""
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for y in range(top, bottom, gap):
        x = left + rnd.randrange(0, 3)
        while x < right - 2:
            w = min(rnd.randrange(2, 6), right - x)
            d.line([(x, y), (x + w - 1, y)], fill=color)
            if rnd.random() < 0.35:
                d.point((x + rnd.randrange(max(1, w)), y - 1), fill=color)
            x += w + rnd.randrange(1, 3)


def stars(img, seed, count, colors):
    rnd = random.Random(seed)
    px = img.load()
    for i in range(count):
        x, y = rnd.randrange(1, CELL - 1), rnd.randrange(1, CELL - 1)
        c = colors[rnd.randrange(len(colors))]
        px[x, y] = c
        if i % 4 == 0:
            soft = c[:3] + (min(255, c[3]) // 2,)
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                px[x + dx, y + dy] = soft


def cracks(img, color, seed, count=4):
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(count):
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        a = rnd.random() * math.tau
        for _ in range(4):
            nx, ny = x + math.cos(a) * 4, y + math.sin(a) * 4
            d.line([(x, y), (nx, ny)], fill=color)
            x, y = nx, ny
            a += rnd.uniform(-0.8, 0.8)


# =====================================================================================================
# Metals: brass, copper, iron, steel; gears and gauges
# =====================================================================================================

def m_brass(seed):
    """Polished brass, finely engraved with scrollwork."""
    img = new()
    noise_fill(img, rgba('9a6a1e'), rgba('f6d27a'), seed, 3, 0.25)
    sheen(img, rgba('fff0c0', 150))
    d = ImageDraw.Draw(img)
    for y in range(4, CELL, 10):
        for x in range(3, CELL, 10):
            d.arc([x, y, x + 6, y + 6], 200, 340, fill=rgba('6a4410'))
    return img


def m_brass_dark(seed):
    """Aged, darker brass for the parts in shadow: frames, carriages, joints."""
    img = new()
    noise_fill(img, rgba('5a3a12'), rgba('b07c2c'), seed, 3, 0.3)
    return img


def m_copper(seed):
    """Warm copper with a green bloom of verdigris here and there."""
    img = new()
    noise_fill(img, rgba('8a4020'), rgba('e88a54'), seed, 3, 0.25)
    rnd = random.Random(seed)
    for _ in range(10):
        img.putpixel((rnd.randrange(CELL), rnd.randrange(CELL)), rgba('5ab08a'))
    sheen(img, rgba('ffc8a0', 120), 9, 3)
    return img


def m_iron(seed):
    """Dark riveted iron plate."""
    img = new()
    noise_fill(img, rgba('2a2c32'), rgba('5e626c'), seed, 4, 0.3)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, CELL - 1, CELL - 1], outline=rgba('1a1c20'))
    rivets(img, rgba('a8acb6'), rgba('16181c'))
    return img


def m_steel(seed):
    """Bright blued steel: barrels and pistons."""
    img = new()
    vertical_glow(img, rgba('3a4a62'), rgba('b8c8de'), 0.15)
    sheen(img, rgba('e8f0ff', 90), 11, 2)
    return img


def m_gear(seed):
    """The face of a brass gear: a toothed rim, spokes and a hub, shown whole on its flat faces."""
    img = new()
    d = ImageDraw.Draw(img)
    c = 15.5
    for i in range(12):
        a = math.tau * i / 12
        d.polygon([(c + math.cos(a - 0.14) * 12, c + math.sin(a - 0.14) * 12),
                   (c + math.cos(a + 0.14) * 12, c + math.sin(a + 0.14) * 12),
                   (c + math.cos(a + 0.1) * 15.5, c + math.sin(a + 0.1) * 15.5),
                   (c + math.cos(a - 0.1) * 15.5, c + math.sin(a - 0.1) * 15.5)], fill=rgba('c89a3c'))
    d.ellipse([c - 12.5, c - 12.5, c + 12.5, c + 12.5], fill=rgba('e0b45a'))
    d.ellipse([c - 9.5, c - 9.5, c + 9.5, c + 9.5], fill=rgba('7a5418'))
    for i in range(5):
        a = math.tau * i / 5
        d.line([(c, c), (c + math.cos(a) * 10, c + math.sin(a) * 10)], fill=rgba('e8c070'), width=3)
    d.ellipse([c - 3.5, c - 3.5, c + 3.5, c + 3.5], fill=rgba('f6d27a'))
    d.ellipse([c - 1.2, c - 1.2, c + 1.2, c + 1.2], fill=rgba('3a2408'))
    return img


def m_gauge(seed):
    """A pressure gauge: a cream dial with ticks and a needle behind brass."""
    img = new()
    d = ImageDraw.Draw(img)
    d.ellipse([1, 1, 30, 30], fill=rgba('c8962e'))
    d.ellipse([4, 4, 27, 27], fill=rgba('f4ead0'))
    for i in range(9):
        a = math.pi * 0.75 + math.pi * 1.5 * i / 8
        d.line([(15.5 + math.cos(a) * 9, 15.5 + math.sin(a) * 9), (15.5 + math.cos(a) * 11, 15.5 + math.sin(a) * 11)],
               fill=rgba('2a1a10'))
    d.line([(15.5, 15.5), (22, 9)], fill=rgba('b02020'), width=2)
    d.ellipse([14, 14, 17, 17], fill=rgba('2a1a10'))
    return img


def m_pipe(seed):
    """Copper pipe seen from the side: rounded light down its length, joint rings."""
    img = new()
    vertical_glow(img, rgba('7a3818'), rgba('f0a070'), 0.2)
    d = ImageDraw.Draw(img)
    for y in (6, 22):
        d.line([(0, y), (CELL - 1, y)], fill=rgba('5a2810'))
        d.line([(0, y + 1), (CELL - 1, y + 1)], fill=rgba('ffd0a8'))
    return img


def m_wood(seed):
    """Dark walnut for the rifle's stock and the cannon's carriage, its grain running lengthwise."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            t = 0.5 + 0.5 * math.sin(x * 0.55 + math.sin(y * 0.25 + seed) * 2.2)
            img.putpixel((x, y), mix(rgba('3a1e0e'), rgba('7a4424'), t))
    return img


def m_leather(seed):
    img = new()
    noise_fill(img, rgba('2e1a12'), rgba('5e3a24'), seed, 2, 0.4)
    d = ImageDraw.Draw(img)
    for x in range(2, CELL, 6):
        d.point((x, 2), fill=rgba('c89a3c'))
        d.point((x, CELL - 3), fill=rgba('c89a3c'))
    return img


# =====================================================================================================
# Light: blueprint glow, lenses, lamps, starlight, steam
# =====================================================================================================

def m_blueprint_glow(seed):
    """Blueprint-blue light with a fine grid: runes on a cannon, the rifle's sight, a construct's heart."""
    img = new()
    radial(img, rgba('e8f6ff', 250), rgba('3a8ae0', 230), 1.0)
    d = ImageDraw.Draw(img)
    for i in range(0, CELL, 6):
        d.line([(i, 0), (i, CELL - 1)], fill=rgba('bfe4ff', 120))
        d.line([(0, i), (CELL - 1, i)], fill=rgba('bfe4ff', 120))
    return img


def m_lens(seed):
    """A glass lens: deep blue going clear in the middle, a white glint."""
    img = new()
    radial(img, rgba('d8f0ff', 240), rgba('1a4a9a', 240), 0.8)
    d = ImageDraw.Draw(img)
    d.ellipse([7, 6, 13, 11], fill=rgba('ffffff', 230))
    d.ellipse([20, 20, 22, 22], fill=rgba('ffffff', 160))
    return img


def m_lamp(seed):
    """Warm lamplight: a construct's eye, a boiler's window."""
    img = new()
    radial(img, rgba('fffbe8', 255), rgba('ffb040', 245), 1.3)
    return img


def m_star_core(seed):
    """White-gold starlight."""
    img = new()
    radial(img, rgba('ffffff', 255), rgba('ffe6a0', 245), 1.5)
    return img


def m_star_tail(seed):
    """A meteor's tail: bright at the root, fading to a pale blue at the tip."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            t = (y + 0.5) / CELL
            side = abs((x + 0.5) / CELL - 0.5) * 2
            c = mix(rgba('fff6d8', 240), rgba('8ac0ff', 40), t)
            img.putpixel((x, y), c[:3] + (int(c[3] * (1 - side * 0.6)),))
    return img


def m_steam(seed):
    """Soft see-through white steam."""
    img = new()
    noise_fill(img, rgba('d8e0e8', 110), rgba('ffffff', 190), seed, 5, 0.15)
    return img


# =====================================================================================================
# Glass and acid: the Compound Flask
# =====================================================================================================

def m_glass(seed):
    """Thin clear glass: almost invisible, bright edges and a long glint."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            edge = min(x, y, CELL - 1 - x, CELL - 1 - y)
            img.putpixel((x, y), rgba('e8f8ff', 200 if edge == 0 else 60))
    d = ImageDraw.Draw(img)
    d.line([(6, 26), (11, 4)], fill=rgba('ffffff', 210))
    d.line([(9, 27), (13, 12)], fill=rgba('ffffff', 110))
    return img


def m_acid(seed):
    """Nitroglycerin in aqua regia: an oily yellow-green that glows, bubbles rising in it."""
    img = new()
    noise_fill(img, rgba('a8c018', 220), rgba('f0ff8a', 235), seed, 4, 0.2)
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(7):
        x, y, r = rnd.randrange(3, 29), rnd.randrange(3, 29), rnd.choice((1, 1, 2))
        d.ellipse([x - r, y - r, x + r, y + r], outline=rgba('ffffe0', 230))
    return img


def m_cork(seed):
    img = new()
    noise_fill(img, rgba('8a6034'), rgba('d8a874'), seed, 1, 0.6)
    return img


# =====================================================================================================
# Jewelry: the Mystic Brooch
# =====================================================================================================

def m_brooch_gem(seed):
    """A cut gem of blueprint blue with a tiny gear frozen inside it."""
    img = new()
    radial(img, rgba('cfeaff', 250), rgba('1a3a8a', 250), 0.9)
    d = ImageDraw.Draw(img)
    for i in range(6):                                                     # the facets
        a = math.tau * i / 6
        d.line([(15.5, 15.5), (15.5 + math.cos(a) * 15, 15.5 + math.sin(a) * 15)], fill=rgba('9ad0ff', 150))
    c = 15.5
    for i in range(8):
        a = math.tau * i / 8
        d.rectangle([c + math.cos(a) * 5 - 1, c + math.sin(a) * 5 - 1, c + math.cos(a) * 5 + 1, c + math.sin(a) * 5 + 1],
                    fill=rgba('f6d27a'))
    d.ellipse([c - 4, c - 4, c + 4, c + 4], fill=rgba('f6d27a'))
    d.ellipse([c - 1.5, c - 1.5, c + 1.5, c + 1.5], fill=rgba('1a3a8a'))
    return img


def m_filigree(seed):
    """Brass filigree: thin curls of gold wire on a dark ground."""
    img = new()
    noise_fill(img, rgba('3a2410', 255), rgba('5a3a18', 255), seed, 3, 0.2)
    d = ImageDraw.Draw(img)
    for y in range(-4, CELL, 8):
        for x in range(-4, CELL, 8):
            d.arc([x, y, x + 9, y + 9], 0, 270, fill=rgba('f6d27a'))
    return img


# =====================================================================================================
# The dolls: stone, straw, iron, dust, frost; and their stitched joints
# =====================================================================================================

def m_doll_stone(seed):
    """Grey stone with dark cracks and a little moss."""
    img = new()
    noise_fill(img, rgba('5a5c60'), rgba('a8a8a8'), seed, 3, 0.35)
    cracks(img, rgba('2a2a2c'), seed, 4)
    rnd = random.Random(seed)
    for _ in range(8):
        img.putpixel((rnd.randrange(CELL), rnd.randrange(CELL)), rgba('5a7a3a'))
    return img


def m_doll_straw(seed):
    """Bundled straw, its stalks running down."""
    img = new()
    rnd = random.Random(seed)
    for x in range(CELL):
        base = rnd.random()
        for y in range(CELL):
            t = base * 0.7 + 0.3 * math.sin(y * 0.4 + x)
            img.putpixel((x, y), mix(rgba('a8862e'), rgba('f0d888'), t))
    return img


def m_burlap(seed):
    """Coarse burlap: the straw doll's head, two stitched crosses for eyes."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            t = 0.55 + 0.25 * ((x // 2 + y // 2) % 2) + random.Random(x * 31 + y).random() * 0.2
            img.putpixel((x, y), mix(rgba('7a5a34'), rgba('c8a874'), t))
    d = ImageDraw.Draw(img)
    for cx in (9, 22):
        d.line([(cx - 3, 11), (cx + 3, 17)], fill=rgba('1a1008'), width=2)
        d.line([(cx + 3, 11), (cx - 3, 17)], fill=rgba('1a1008'), width=2)
    d.line([(8, 24), (24, 24)], fill=rgba('1a1008'))
    for x in range(9, 24, 3):
        d.line([(x, 23), (x, 25)], fill=rgba('1a1008'))
    return img


def m_doll_iron(seed):
    """Riveted iron doll plates, a stripe of brass."""
    img = m_iron(seed)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 13, CELL - 1, 18], fill=rgba('b07c2c'))
    return img


def m_doll_dust(seed):
    """Packed sand, grains coming loose."""
    img = new()
    noise_fill(img, rgba('a88a52'), rgba('ecd49a'), seed, 2, 0.6)
    rnd = random.Random(seed)
    for _ in range(20):
        img.putpixel((rnd.randrange(CELL), rnd.randrange(CELL)), rgba('fff4d0'))
    return img


def m_doll_frost(seed):
    """Clear blue ice with white frost at its edges."""
    img = new()
    noise_fill(img, rgba('6ab0e8', 210), rgba('d8f4ff', 230), seed, 5, 0.15)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, CELL - 1, CELL - 1], outline=rgba('ffffff', 240))
    d.line([(5, 26), (12, 5)], fill=rgba('ffffff', 200))
    return img


def m_doll_joint(seed):
    """A doll's joint: a brass pin through stitched cloth."""
    img = new()
    noise_fill(img, rgba('3a2a1c'), rgba('6a5038'), seed, 2, 0.3)
    d = ImageDraw.Draw(img)
    d.ellipse([10, 10, 21, 21], fill=rgba('e0b45a'))
    d.ellipse([14, 14, 17, 17], fill=rgba('6a4410'))
    for x in range(2, CELL, 4):
        d.line([(x, 3), (x + 2, 5)], fill=rgba('f0e0c0'))
    return img


def m_twine(seed):
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            t = 0.5 + 0.5 * math.sin((x + y * 0.6) * 1.6)
            img.putpixel((x, y), mix(rgba('6a4a24'), rgba('c8a060'), t))
    return img


def m_doll_eye(seed):
    """A doll's glowing eye: a blueprint-blue point in a dark socket."""
    img = new()
    radial(img, rgba('e8f6ff', 255), rgba('2a6ad0', 255), 0.7)
    return img


# =====================================================================================================
# Paper and leather: scrolls and the Illusory Book
# =====================================================================================================

def m_parchment(seed):
    img = new()
    noise_fill(img, rgba('d2b880'), rgba('f6e8c4'), seed, 4, 0.3)
    writing(img, rgba('1b2a41', 220), seed)
    return img


def m_parchment_edge(seed):
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            r = math.hypot(x - 15.5, y - 15.5)
            t = 0.5 + 0.5 * math.sin(r * 1.6)
            img.putpixel((x, y), mix(rgba('a88c58'), rgba('f3e6c4'), t))
    return img


def m_book_cover(seed):
    """Ink blue-black leather with brass corners and a brass gear-and-lamp sigil."""
    img = new()
    noise_fill(img, rgba('101a2a'), rgba('24344e'), seed, 3, 0.25)
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, CELL - 3, CELL - 3], outline=rgba('d9a441'))
    for cx, cy in ((2, 2), (29, 2), (2, 29), (29, 29)):
        d.rectangle([cx - 2, cy - 2, cx + 2, cy + 2], fill=rgba('d9a441'))
    d.ellipse([10, 10, 21, 21], outline=rgba('d9a441'))
    d.polygon([(15.5, 11.5), (17.5, 16), (15.5, 19.5), (13.5, 16)], fill=rgba('ffe6a0'))
    return img


def m_page_edges(seed):
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            t = 0.6 + 0.4 * ((y % 2) == 0)
            img.putpixel((x, y), mix(rgba('b8a478'), rgba('f8eccc'), t))
    return img


def m_book_page(seed):
    """An open page: writing above, a diagram of gears and an orbit below."""
    img = new()
    noise_fill(img, rgba('e6d6aa'), rgba('fbf3da'), seed, 4, 0.2)
    writing(img, rgba('1b2a41', 210), seed, 3, 15, 3, 29, 3)
    d = ImageDraw.Draw(img)
    d.ellipse([4, 17, 15, 28], outline=rgba('3a6ab0'))
    d.ellipse([8, 21, 11, 24], fill=rgba('3a6ab0'))
    d.ellipse([16, 18, 28, 26], outline=rgba('3a6ab0'))
    d.point((27, 21), fill=rgba('1b2a41'))
    d.line([(9, 22), (22, 22)], fill=rgba('3a6ab0', 160))
    return img


def m_ebony(seed):
    img = new()
    noise_fill(img, rgba('0c0a0e'), rgba('2a2430'), seed, 4, 0.2)
    sheen(img, rgba('6a6070', 120), 10, 2)
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
