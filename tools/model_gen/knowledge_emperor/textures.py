"""Procedural material cells for the Knowledge Emperor (Hermit pathway) model atlases.

Same layout as tools/model_gen/hand_of_god/textures.py: every model texture is an atlas of 4-wide rows of cells,
32x32 texture pixels each (one cell = 16 model pixels at 2 texture pixels per model pixel). A cube face samples the
part of its material's cell that matches its size, so the texel density stays even; a face listed in a cube's
'full' shows its whole cell, stretched (a page of writing, a lock, an open book).

The palette (docs/KNOWLEDGE_EMPEROR.md, "Theme"): orchid purple (0xC070E8) and star-white for prying, starlight and
the cage; ink-black and deep indigo for the void and for information; pale gold for scrolls and ink.
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


def edge(img, color, width=1):
    ImageDraw.Draw(img).rectangle([0, 0, CELL - 1, CELL - 1], outline=color, width=width)


def radial(img, inner, outer, power=1.0):
    """Brightest in the middle of the cell, darker toward the corners: light seen through a crystal."""
    for y in range(CELL):
        for x in range(CELL):
            d = math.hypot((x + 0.5) / CELL - 0.5, (y + 0.5) / CELL - 0.5) / 0.7071
            img.putpixel((x, y), mix(inner, outer, d ** power))


def vertical_glow(img, dark, light, core_width=0.25):
    """Brightest along the middle column, darker toward both sides: a shaft of light seen edge on."""
    for y in range(CELL):
        for x in range(CELL):
            d = abs((x + 0.5) / CELL - 0.5) * 2
            t = max(0.0, 1 - d / (1 - core_width)) if d > core_width else 1.0
            img.putpixel((x, y), mix(dark, light, t))


def stars(img, seed, count, colors):
    """Tiny points of starlight scattered over a cell (a few of them with a cross of light)."""
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


def writing(img, color, seed, top=3, bottom=29, left=3, right=29, gap=4):
    """Lines of small hand-written script: short strokes with gaps, as on a page."""
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for y in range(top, bottom, gap):
        x = left + rnd.randrange(0, 3)
        while x < right - 2:
            w = rnd.randrange(2, 6)
            w = min(w, right - x)
            d.line([(x, y), (x + w - 1, y)], fill=color)
            if rnd.random() < 0.35:
                d.point((x + rnd.randrange(max(1, w)), y - 1), fill=color)
            x += w + rnd.randrange(1, 3)


# =====================================================================================================
# Silver, crystal and chain: the dowsing pendulum
# =====================================================================================================

def m_silver(seed):
    """Polished silver of the pendulum's cap and chain links."""
    img = new()
    noise_fill(img, rgba('7a8296'), rgba('f2f4fa'), seed, 3, 0.25)
    d = ImageDraw.Draw(img)
    for x in range(2, CELL, 6):
        d.line([(x, 0), (x + 4, CELL - 1)], fill=rgba('ffffff', 160))
    return img


def m_chain(seed):
    """A fine silver chain seen from the side: links alternating bright and shadowed."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            link = (y // 4) % 2
            t = 0.75 if link else 0.35
            t += 0.25 * (1 - abs((x + 0.5) / CELL - 0.5) * 2)
            img.putpixel((x, y), mix(rgba('4a5064'), rgba('f4f6ff'), t))
    return img


def m_pendulum_crystal(seed):
    """Pale violet crystal, a star caught in its heart."""
    img = new()
    radial(img, rgba('fbf2ff', 245), rgba('9a62d8', 220), 0.8)
    d = ImageDraw.Draw(img)
    for i in range(4):
        a = math.tau * i / 8 + 0.3
        d.line([(16, 16), (16 + math.cos(a) * 15, 16 + math.sin(a) * 15)], fill=rgba('e8d0ff', 200))
    stars(img, seed, 6, [rgba('ffffff')])
    return img


# =====================================================================================================
# Parchment, rods and wax: scrolls
# =====================================================================================================

def m_parchment(seed):
    """Old parchment with lines of ink-black writing."""
    img = new()
    noise_fill(img, rgba('cdb27c'), rgba('f2e4bc'), seed, 4, 0.3)
    writing(img, rgba('2a1a34', 220), seed)
    return img


def m_parchment_edge(seed):
    """The rolled edge of a scroll: layered parchment seen end on."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            r = math.hypot(x - 15.5, y - 15.5)
            t = 0.5 + 0.5 * math.sin(r * 1.6)
            img.putpixel((x, y), mix(rgba('a88c58'), rgba('f0e0b4'), t))
    return img


def m_scroll_rod(seed):
    """The dark lacquered rod a scroll is rolled on."""
    img = new()
    noise_fill(img, rgba('1a0e1e'), rgba('4a2a44'), seed, 3, 0.2)
    d = ImageDraw.Draw(img)
    d.line([(0, 6), (CELL - 1, 6)], fill=rgba('6a4060'))
    return img


def m_gold(seed):
    """Pale gold of the scroll caps and the box's trim, finely engraved."""
    img = new()
    noise_fill(img, rgba('a07c30'), rgba('fff0b0'), seed, 3, 0.3)
    d = ImageDraw.Draw(img)
    for y in range(3, CELL, 8):
        for x in range(2, CELL, 8):
            d.arc([x, y, x + 5, y + 5], 0, 270, fill=rgba('6a4a14'))
    return img


def m_wax_seal(seed):
    """Orchid wax with a pressed eye."""
    img = new()
    radial(img, rgba('d48af0'), rgba('6a2a8a'), 1.2)
    d = ImageDraw.Draw(img)
    d.ellipse([7, 11, 24, 20], outline=rgba('3a104a'))
    d.ellipse([13, 12, 18, 19], fill=rgba('3a104a'))
    return img


# =====================================================================================================
# Starlight: a small star, amber, a ball of illusory light, a bolt of a spell
# =====================================================================================================

def m_star_core(seed):
    """White-hot starlight, a breath of orchid at the edges."""
    img = new()
    radial(img, rgba('ffffff', 255), rgba('f0d8ff', 245), 1.6)
    return img


def m_star_point(seed):
    """A point of a star: bright at the root, fading to lilac at the tip."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            t = (y + 0.5) / CELL
            side = abs((x + 0.5) / CELL - 0.5) * 2
            img.putpixel((x, y), mix(rgba('ffffff', 250), rgba('d6aaff', 230), max(t, side * 0.6)))
    return img


def m_amber(seed):
    """Clear amber: honey gold, see-through, with a few motes of starlight frozen in it."""
    img = new()
    noise_fill(img, rgba('e8901a', 150), rgba('ffd27a', 170), seed, 6, 0.15)
    for y in range(CELL):
        for x in range(CELL):
            if x in (0, CELL - 1) or y in (0, CELL - 1):
                img.putpixel((x, y), rgba('fff0c0', 220))
    d = ImageDraw.Draw(img)
    d.line([(4, 26), (12, 4)], fill=rgba('fff6dc', 190))
    d.line([(7, 28), (15, 6)], fill=rgba('fff6dc', 110))
    stars(img, seed, 5, [rgba('ffffff', 230), rgba('ffe8c0', 230)])
    return img


def m_amber_core(seed):
    """The deeper, darker heart of the amber."""
    img = new()
    noise_fill(img, rgba('b85a10', 140), rgba('e8941e', 160), seed, 5, 0.2)
    return img


def m_illusory_light(seed):
    """Soft white light with a pearly shimmer: Condense Knowledge's ball, the Knowledge Orbs. Tint it."""
    img = new()
    radial(img, rgba('ffffff', 230), rgba('e4e4ee', 120), 1.4)
    stars(img, seed, 8, [rgba('ffffff', 255)])
    return img


def m_bolt_light(seed):
    """A spell's bolt: a white core with a pale rim, white so .color(...) gives the element."""
    img = new()
    vertical_glow(img, rgba('c8c8d4', 220), rgba('ffffff', 255), 0.35)
    return img


# =====================================================================================================
# Knowledge: the white spear
# =====================================================================================================

def m_knowledge_white(seed):
    """Condensed knowledge: pure white light with faint lilac script running through it."""
    img = new()
    vertical_glow(img, rgba('e6dcff', 240), rgba('ffffff', 255), 0.3)
    writing(img, rgba('c6b0f0', 150), seed, 2, 31, 2, 30, 5)
    return img


def m_knowledge_edge(seed):
    img = new()
    noise_fill(img, rgba('f4f0ff', 250), rgba('ffffff', 255), seed, 5, 0.1)
    return img


def m_knowledge_core(seed):
    """The orchid heart of the spear, along its shaft."""
    img = new()
    vertical_glow(img, rgba('9050c8', 230), rgba('f6e8ff', 255), 0.2)
    return img


# =====================================================================================================
# Gold and wood: the spindle of Sleeping Beauty
# =====================================================================================================

def m_spindle_gold(seed):
    img = new()
    noise_fill(img, rgba('b8862a'), rgba('fff2a8'), seed, 3, 0.25)
    d = ImageDraw.Draw(img)
    for y in range(0, CELL, 5):
        d.line([(0, y), (CELL - 1, y + 2)], fill=rgba('fff8d8', 180))
    return img


def m_spindle_wood(seed):
    """Pale turned wood, its rings going round the shaft."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            t = 0.5 + 0.5 * math.sin(y * 0.9 + math.sin(x * 0.3) * 0.8)
            img.putpixel((x, y), mix(rgba('8a5a34'), rgba('d8a87a'), t))
    return img


def m_thread(seed):
    """Wound golden thread."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            t = 0.5 + 0.5 * math.sin((x + y * 0.4) * 1.9)
            img.putpixel((x, y), mix(rgba('d09a2a'), rgba('fff0a0'), t))
    return img


# =====================================================================================================
# Ebony and the void: Pandora's box
# =====================================================================================================

def m_box_ebony(seed):
    """Black ebony with thin orchid inlay lines."""
    img = new()
    noise_fill(img, rgba('0c0810'), rgba('2a1e30'), seed, 4, 0.2)
    d = ImageDraw.Draw(img)
    d.rectangle([3, 3, CELL - 4, CELL - 4], outline=rgba('8a4ab0'))
    d.line([(3, 3), (CELL - 4, CELL - 4)], fill=rgba('5a2a7a'))
    d.line([(CELL - 4, 3), (3, CELL - 4)], fill=rgba('5a2a7a'))
    return img


def m_box_lock(seed):
    """The gold lock on the box's front: a keyhole under an eye."""
    img = m_gold(seed)
    d = ImageDraw.Draw(img)
    d.ellipse([10, 5, 21, 12], outline=rgba('3a2008'))
    d.ellipse([14, 6, 17, 11], fill=rgba('3a2008'))
    d.ellipse([13, 15, 18, 20], fill=rgba('120808'))
    d.polygon([(14, 19), (17, 19), (18, 27), (13, 27)], fill=rgba('120808'))
    return img


def m_box_void(seed):
    """What is inside the box: deep indigo dark full of stars."""
    img = new()
    noise_fill(img, rgba('05030c'), rgba('241040'), seed, 5, 0.2)
    stars(img, seed, 14, [rgba('ffffff'), rgba('e0c0ff'), rgba('c070e8')])
    return img


# =====================================================================================================
# Leather and paper: the book of tales
# =====================================================================================================

def m_book_cover(seed):
    """Deep indigo leather with a gold border and a gold eye of stars."""
    img = new()
    noise_fill(img, rgba('1a1238'), rgba('34265e'), seed, 3, 0.25)
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, CELL - 3, CELL - 3], outline=rgba('e8c870'))
    d.ellipse([9, 12, 22, 19], outline=rgba('e8c870'))
    d.ellipse([14, 13, 17, 18], fill=rgba('e8c870'))
    return img


def m_page_edges(seed):
    """The edge of a stack of pages: fine cream lines."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            t = 0.6 + 0.4 * ((y % 2) == 0)
            img.putpixel((x, y), mix(rgba('b8a478'), rgba('f8eccc'), t))
    return img


def m_book_page(seed):
    """An open page with writing and a small star map: shown whole on the top of each half of the open book."""
    img = new()
    noise_fill(img, rgba('e2d2a6'), rgba('fbf3da'), seed, 4, 0.2)
    writing(img, rgba('2a1a34', 210), seed, 3, 17, 3, 29, 3)
    d = ImageDraw.Draw(img)
    pts = [(6, 26), (11, 21), (16, 24), (21, 19), (26, 23)]
    d.line(pts, fill=rgba('7a3aa0', 200))
    for p in pts:
        d.point(p, fill=rgba('c070e8'))
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
