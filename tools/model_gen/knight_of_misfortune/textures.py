"""Procedural material cells for the Knight Of Misfortune (Darkness pathway) model atlases.

Same layout as tools/model_gen/hand_of_god/textures.py: every model texture is an atlas of 4-wide rows of cells,
32x32 texture pixels each (one cell = 16 model pixels at 2 texture pixels per model pixel). A cube face samples the
part of its material's cell that matches its size, so the texel density stays even; a face listed in a cube's
'full' shows its whole cell, stretched (an eye, a face, a maw).

The palette: purpureus violet-black for Darkness, bone white for the Sword of Darkness and the Demonic Wolf's ribs,
wet dark flesh for the Nightmare's limbs, ash fur, pale moon-blue for spirits and dreams.
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
# The Sword of Darkness: bone, a violet-black core of Repose and Horror
# =====================================================================================================

def m_bone(seed):
    """Old bone: ivory with a porous grain and faint grey-violet stains."""
    img = new()
    noise_fill(img, rgba('b8ad98'), rgba('f0ead8'), seed, 3, 0.3)
    rnd = random.Random(seed)
    for _ in range(26):                                                     # pores
        img.putpixel((rnd.randrange(CELL), rnd.randrange(CELL)), rgba('8a7e70'))
    cracks(img, rgba('7a6e68', 200), seed + 3, 2, 1, 3)
    return img


def m_bone_dark(seed):
    """Bone gone grey and violet where the Darkness has soaked into it."""
    img = new()
    noise_fill(img, rgba('4a3e52'), rgba('9a8e98'), seed, 3, 0.35)
    cracks(img, rgba('241a2c'), seed + 5, 3, 1, 4)
    return img


def m_dark_core(seed):
    """The blade's heart: violet-black, brightest along its middle, with pale motes in it."""
    img = new()
    vertical_glow(img, rgba('1a0c28', 245), rgba('9a5ad8', 255), 0.15)
    rnd = random.Random(seed)
    for _ in range(12):
        img.putpixel((rnd.randrange(CELL), rnd.randrange(CELL)), rgba('e8d8ff'))
    return img


def m_vertebra(seed):
    """The grip: a column of small vertebrae."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            seg = (y % 8) / 8
            t = math.sin(seg * math.pi)
            img.putpixel((x, y), mix(rgba('5a4e48'), rgba('d8ccb8'), t))
    d = ImageDraw.Draw(img)
    for y in range(0, CELL, 8):
        d.line([(0, y), (CELL, y)], fill=rgba('2a2026'), width=1)
    return img


def m_void_gem(seed):
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            r = math.hypot(x - 11, y - 11) / 26
            img.putpixel((x, y), mix(rgba('f0e0ff'), rgba('2a0a4a'), r))
    return img


# =====================================================================================================
# The Nightmare's limbs: dark, wet flesh
# =====================================================================================================

def m_flesh_dark(seed):
    """Wet dark flesh: blackish purple with a sheen and pale suckers in two rows."""
    img = new()
    noise_fill(img, rgba('1c0e22'), rgba('4a2448'), seed, 4, 0.2)
    d = ImageDraw.Draw(img)
    for y in range(CELL):                                                   # the wet sheen down one side
        a = int(90 * max(0.0, 1 - abs(y % 16 - 4) / 6))
        if a:
            d.line([(6, y), (9, y)], fill=rgba('b88ad0', a))
    for y in range(3, CELL, 7):
        for x in (12, 22):
            d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=rgba('8a5a7a'), outline=rgba('2a1028'))
            d.point((x, y), fill=rgba('3a1430'))
    return img


def m_flesh_tip(seed):
    """The tip of a limb: darker, almost black, with a violet glow seeping through."""
    img = new()
    noise_fill(img, rgba('0e0612'), rgba('3a1a4a'), seed, 3, 0.3)
    rnd = random.Random(seed)
    for _ in range(8):
        img.putpixel((rnd.randrange(CELL), rnd.randrange(CELL)), rgba('c08aff'))
    return img


# =====================================================================================================
# The Demonic Wolf: ash fur over ribs of bone, a violet eye
# =====================================================================================================

def m_wolf_fur(seed):
    """A coat of dark ash fur, combed back in streaks."""
    img = new()
    noise_fill(img, rgba('1a1620'), rgba('3e3846'), seed, 5, 0.2)
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(40):
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        d.line([(x, y), (x + rnd.randint(-1, 1), y + rnd.randint(3, 6))], fill=rgba(rnd.choice(('5a5464', '0c0a10'))))
    return img


def m_wolf_rib(seed):
    """Exposed ribs: ash-white bone with dark gaps, no flesh over them."""
    img = new()
    noise_fill(img, rgba('a09888'), rgba('e4dccc'), seed, 3, 0.3)
    d = ImageDraw.Draw(img)
    for y in range(4, CELL, 8):
        d.rectangle([0, y, CELL, y + 3], fill=rgba('0a060e'))
    return img


def m_wolf_eye(seed):
    """A glowing violet eye with a slit pupil (shown whole on its face)."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            r = math.hypot((x - 15.5) / 1.0, (y - 15.5) / 0.7) / 16
            img.putpixel((x, y), mix(rgba('f4e0ff'), rgba('6a1ab0'), r * 1.2))
    d = ImageDraw.Draw(img)
    d.ellipse([14, 4, 17, 27], fill=rgba('0a0010'))
    return img


def m_wolf_maw(seed):
    """Inside the maw: red-black, with bone-white teeth along the top and bottom."""
    img = new()
    noise_fill(img, rgba('1a0408'), rgba('5a1020'), seed, 3, 0.3)
    d = ImageDraw.Draw(img)
    for x in range(0, CELL, 4):
        d.polygon([(x, 0), (x + 3, 0), (x + 1.5, 6)], fill=rgba('e8e0d0'))
        d.polygon([(x + 2, CELL), (x + 5, CELL), (x + 3.5, CELL - 6)], fill=rgba('e8e0d0'))
    return img


def m_wolf_claw(seed):
    img = new()
    noise_fill(img, rgba('0a080c'), rgba('4a4450'), seed, 2, 0.3)
    return img


# =====================================================================================================
# Dowsing: a pendulum of pale crystal on a fine chain
# =====================================================================================================

def m_chain(seed):
    img = new()
    noise_fill(img, rgba('6a6a7a'), rgba('d8d8e8'), seed, 2, 0.4)
    d = ImageDraw.Draw(img)
    for y in range(0, CELL, 6):
        d.line([(0, y), (CELL, y)], fill=rgba('3a3a48'))
    return img


def m_crystal(seed):
    """Pale moon-blue crystal, brighter toward one corner."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            t = (x + y) / (2 * CELL)
            img.putpixel((x, y), mix(rgba('f4faff', 235), rgba('5a7ad0', 220), t))
    d = ImageDraw.Draw(img)
    d.line([(4, 4), (14, 14)], fill=rgba('ffffff'), width=2)
    return img


# =====================================================================================================
# Spirits: the Terror Banshee and a soul out of its body
# =====================================================================================================

def m_ghost_cloth(seed):
    """Tattered, see-through grey-blue cloth, fading out toward the bottom of the cell."""
    img = new()
    big = value_noise(seed, 4)
    for y in range(CELL):
        for x in range(CELL):
            t = big(x, y)
            a = int(200 * (1 - (y / CELL) ** 1.6) * (0.7 + 0.3 * t))
            img.putpixel((x, y), mix(rgba('5a6a8a', a), rgba('c8d4ec', a), t))
    d = ImageDraw.Draw(img)
    for x in range(1, CELL, 5):
        d.line([(x, CELL - 9), (x + 1, CELL - 1)], fill=(0, 0, 0, 0), width=2)    # torn hem
    return img


def m_ghost_hair(seed):
    """Long white hair, strand by strand, see-through."""
    img = new()
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for x in range(CELL):
        shade = rnd.choice(('e8eef8', 'c0c8d8', 'ffffff', 'a8b0c4'))
        d.line([(x, 0), (x + rnd.randint(-1, 1), CELL)], fill=rgba(shade, rnd.randint(150, 230)))
    return img


def m_ghost_face(seed):
    """The banshee's face: a pale oval with two black hollow eyes and an open, screaming mouth."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            r = math.hypot((x - 15.5) / 13, (y - 15.5) / 15)
            if r <= 1:
                img.putpixel((x, y), mix(rgba('f0f4ff', 230), rgba('8a96b4', 200), r))
    d = ImageDraw.Draw(img)
    d.ellipse([7, 9, 13, 16], fill=rgba('06040c'))
    d.ellipse([19, 9, 25, 16], fill=rgba('06040c'))
    d.ellipse([12, 19, 20, 29], fill=rgba('06040c'))
    return img


def m_soul_body(seed):
    """A soul's body: pale moon-blue, see-through, with a brighter grain drifting up it."""
    img = new()
    big = value_noise(seed, 5)
    for y in range(CELL):
        for x in range(CELL):
            t = big(x, y)
            img.putpixel((x, y), mix(rgba('7a9ad8', 120), rgba('e0ecff', 190), t))
    return img


def m_soul_face(seed):
    """A soul's face: the soul's grain, with two soft lights for eyes."""
    img = m_soul_body(seed)
    d = ImageDraw.Draw(img)
    for x in (9, 22):
        d.ellipse([x - 3, 12, x + 3, 17], fill=rgba('ffffff', 240))
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
