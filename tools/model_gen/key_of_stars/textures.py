"""Procedural material cells for the Key Of Stars (Door pathway) model atlases.

Same layout as tools/model_gen/hand_of_order/textures.py: every model texture is an atlas of 4-wide rows of cells,
32x32 texture pixels each (one cell = 16 model pixels at 2 texture pixels per model pixel). A cube face samples the
part of its material's cell that matches its size, so the texel density stays even; a face listed in a cube's
'full' shows its whole cell, stretched.

The palette (docs/KEY_OF_STARS.md, "Theme"): pale blue-white starlight (0x9DB4FF, the Pathway's color) with gold
highlights (0xFFD98A), deep indigo (0x2A2A6E) for the doors and the dark between the stars (0x0E0C2A), a violet
space-time storm (0x7A5AD8).
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


def stars(img, seed, count, colors, big=2):
    """Pin-prick stars scattered over a cell; 'big' of them get four little rays."""
    rnd = random.Random(seed)
    for i in range(count):
        x, y = rnd.randrange(1, CELL - 1), rnd.randrange(1, CELL - 1)
        c = colors[rnd.randrange(len(colors))]
        img.putpixel((x, y), c)
        if i < big:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                img.putpixel((x + dx, y + dy), c[:3] + (c[3] // 2,))


def edge_glow(img, color, width=3):
    """A brighter rim: the outline an illusion has."""
    for y in range(CELL):
        for x in range(CELL):
            e = min(x, y, CELL - 1 - x, CELL - 1 - y)
            if e < width:
                base = img.getpixel((x, y))
                img.putpixel((x, y), mix(base, color, 1 - e / width))


# =====================================================================================================
# Starlight
# =====================================================================================================

def m_starlight(seed):
    """Starlight itself: white at the heart, pale blue at the rim."""
    img = new()
    radial(img, rgba('ffffff', 255), rgba('9db4ff', 235), 1.2)
    return img


def m_starlight_soft(seed):
    """See-through starlight for illusions: pale blue, brighter at the outline, a few stars inside."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            e = min(x, y, CELL - 1 - x, CELL - 1 - y) / 7.0
            t = max(0.0, 1 - e)
            img.putpixel((x, y), mix(rgba('9db4ff', 120), rgba('f0f4ff', 230), t))
    stars(img, seed, 6, [rgba('ffffff')], 1)
    return img


def m_star_gold(seed):
    """The gold of the highlights: warm, polished, a streak of shine."""
    img = new()
    noise_fill(img, rgba('8a6420'), rgba('ffe7a8'), seed, 4, 0.2)
    d = ImageDraw.Draw(img)
    for x in range(-CELL, CELL, 9):
        d.line([(x, CELL - 1), (x + CELL, -1)], fill=(255, 248, 220, 90))
    return img


def m_star_silver(seed):
    """Pale silver-blue metal: the frames of the doors."""
    img = new()
    noise_fill(img, rgba('6a78a8'), rgba('e8eeff'), seed, 3, 0.25)
    d = ImageDraw.Draw(img)
    for x in range(-CELL, CELL, 8):
        d.line([(x, CELL - 1), (x + CELL, -1)], fill=(255, 255, 255, 80))
    return img


def m_star_silver_rune(seed):
    """Silver-blue with a line of small stars set into it: a door's frame, its lintel."""
    img = m_star_silver(seed)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, CELL - 1, CELL - 1], outline=rgba('3a4478'))
    for y in range(4, CELL - 2, 7):
        cx = 16 + (y % 3) - 1
        d.line([(cx - 2, y), (cx + 2, y)], fill=rgba('ffe7a8'))
        d.line([(cx, y - 2), (cx, y + 2)], fill=rgba('ffe7a8'))
        d.point((cx, y), fill=rgba('ffffff'))
    return img


# =====================================================================================================
# The dark between the stars: door panels, the night inside the ball, the storm
# =====================================================================================================

def m_door_indigo(seed):
    """An illusory door's panel: deep indigo, a little see-through, dusted with stars, its rim glowing."""
    img = new()
    noise_fill(img, rgba('14123a', 215), rgba('3a3a8e', 225), seed, 5, 0.15)
    stars(img, seed, 18, [rgba('ffffff'), rgba('c8d4ff'), rgba('ffe7a8')], 3)
    edge_glow(img, rgba('9db4ff', 240), 3)
    return img


def m_night(seed):
    """The night sky: nearly black indigo with many small stars."""
    img = new()
    noise_fill(img, rgba('06061a'), rgba('1e1c50'), seed, 6, 0.1)
    stars(img, seed, 26, [rgba('ffffff'), rgba('aebfff'), rgba('ffe7a8')], 3)
    return img


def m_nebula(seed):
    """The space-time storm: violet and indigo swirls with bright threads."""
    img = new()
    big = value_noise(seed, 5)
    for y in range(CELL):
        for x in range(CELL):
            a = math.atan2(y - 15.5, x - 15.5)
            r = math.hypot(x - 15.5, y - 15.5)
            t = 0.5 + 0.5 * math.sin(a * 3 + r * 0.45 + big(x, y) * 4)
            img.putpixel((x, y), mix(rgba('1a1050', 230), rgba('a088ff', 240), t ** 1.6))
    stars(img, seed, 8, [rgba('ffffff')], 2)
    return img


def m_crystal(seed):
    """The crystal ball's glass: clear blue, brighter at the rim, one highlight."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            e = min(x, y, CELL - 1 - x, CELL - 1 - y)
            a = 110 if e > 3 else 170
            img.putpixel((x, y), (200, 216, 255, a))
    d = ImageDraw.Draw(img)
    d.ellipse([6, 5, 13, 10], fill=(255, 255, 255, 190))
    d.line([(20, 25), (26, 18)], fill=(255, 255, 255, 120), width=2)
    return img


# =====================================================================================================
# The worm, the scribe's things, the toy
# =====================================================================================================

def m_worm(seed):
    """A Worm of Star's body: soft rings of gold and white light."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            ring = 0.5 + 0.5 * math.cos((x + 0.5) / CELL * math.tau * 3)
            core = 1 - abs((y + 0.5) / CELL - 0.5) * 2
            img.putpixel((x, y), mix(rgba('c89a40', 230), rgba('fff6dc', 255), 0.25 + 0.5 * ring * core + 0.25 * core))
    return img


def m_page(seed):
    """A page of the Scribe's book: pale, with faint lines of starlit writing."""
    img = new()
    noise_fill(img, rgba('d8dcf0'), rgba('f6f6ff'), seed, 5, 0.1)
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for y in range(4, CELL - 3, 4):
        x = 3
        while x < CELL - 4:
            w = min(rnd.randrange(2, 6), CELL - 4 - x)
            d.line([(x, y), (x + w - 1, y)], fill=rgba('6a7ad0', 200))
            x += w + rnd.randrange(1, 3)
    return img


def m_cover(seed):
    """The book's cover: indigo leather, a gold star stamped in the middle."""
    img = new()
    noise_fill(img, rgba('1c1a4a'), rgba('3a3880'), seed, 3, 0.25)
    d = ImageDraw.Draw(img)
    d.rectangle([1, 1, CELL - 2, CELL - 2], outline=rgba('c8a050'))
    c = 15.5
    pts = []
    for i in range(8):
        r = 9 if i % 2 == 0 else 3
        a = -math.pi / 2 + i * math.pi / 4
        pts.append((c + math.cos(a) * r, c + math.sin(a) * r))
    d.polygon(pts, fill=rgba('ffd98a'))
    return img


def m_feather(seed):
    """A quill's vane: white shading to starlight blue, barbs drawn slantwise."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            t = (x + 0.5) / CELL
            barb = 0.5 + 0.5 * math.sin((x + y * 1.4) * 0.9)
            img.putpixel((x, y), mix(rgba('ffffff', 245), rgba('9db4ff', 220), t * 0.7 + barb * 0.2))
    return img


def m_tin_red(seed):
    """A toy's painted tin: bright red with a lighter band."""
    img = new()
    noise_fill(img, rgba('a02424'), rgba('e05050'), seed, 4, 0.15)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 12, CELL - 1, 19], fill=rgba('f0d070'))
    return img


def m_spectral(seed):
    """An unseen hand made visible: starlight, see-through, brighter at its outline."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            e = min(x, y, CELL - 1 - x, CELL - 1 - y) / 6.0
            t = max(0.0, 1 - e)
            img.putpixel((x, y), mix(rgba('9db4ff', 110), rgba('ffffff', 230), t))
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
