"""Procedural material cells for the Beauty Goddess (Moon pathway) model atlases.

Same layout as tools/model_gen/hanged_man/textures.py: every model texture is an atlas of 4-wide rows of cells,
32x32 texture pixels each (one cell = 16 model pixels at 2 texture pixels per model pixel). A cube face samples
the part of its material's cell that matches its size, so the texel density stays even; a face listed in a
cube's 'full' shows its whole cell, stretched (wing membranes, an iris, a face).

The palette: moonlight silver and pale blue, night violet, vampire crimson, and the gold-green of Creation.
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


def veins(img, color, seed, count=3, width=1, step=4):
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(count):
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        angle = rnd.random() * math.tau
        pts = [(x, y)]
        for _ in range(9):
            angle += rnd.uniform(-0.8, 0.8)
            x += math.cos(angle) * step
            y += math.sin(angle) * step
            pts.append((x, y))
        d.line(pts, fill=color, width=width)


def edge(img, color, width=1):
    ImageDraw.Draw(img).rectangle([0, 0, CELL - 1, CELL - 1], outline=color, width=width)


def new():
    return Image.new('RGBA', (CELL, CELL), (0, 0, 0, 0))


# =====================================================================================================
# Glass, cork and the potion inside (Medicine Concoction, the Potions wheel)
# =====================================================================================================

def m_glass(seed):
    img = new()
    noise_fill(img, rgba('a8c4d8', 70), rgba('e8f4ff', 110), seed, 6, 0.1)
    d = ImageDraw.Draw(img)
    d.line([(4, 3), (4, 22)], fill=rgba('ffffff', 210), width=2)        # the highlight down one side
    d.line([(8, 4), (8, 10)], fill=rgba('ffffff', 150), width=1)
    d.line([(26, 20), (26, 28)], fill=rgba('d8ecff', 120), width=1)
    return img


def m_glass_rim(seed):
    img = new()
    noise_fill(img, rgba('8aa8c0', 170), rgba('dceeff', 210), seed, 3, 0.2)
    edge(img, rgba('ffffff', 230))
    return img


def m_cork(seed):
    img = new()
    noise_fill(img, rgba('6a4424'), rgba('b8844e'), seed, 2, 0.45)
    rnd = random.Random(seed)
    for _ in range(14):
        img.putpixel((rnd.randrange(CELL), rnd.randrange(CELL)), rgba('4a2c14'))
    return img


def m_liquid(seed):
    """Nearly white so .color(...) tints it: a potion of any color. Brighter toward the top, with bubbles."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            t = y / (CELL - 1)
            img.putpixel((x, y), mix(rgba('ffffff', 235), rgba('c8c8c8', 235), t))
    d = ImageDraw.Draw(img)
    rnd = random.Random(seed)
    for _ in range(7):
        x, y, r = rnd.randrange(3, 29), rnd.randrange(6, 29), rnd.choice((1, 1, 2))
        d.ellipse([x - r, y - r, x + r, y + r], outline=rgba('ffffff', 255))
    return img


def m_label(seed):
    """A paper label with an inked moon on it."""
    img = new()
    noise_fill(img, rgba('d8c8a0'), rgba('f4ead0'), seed, 3, 0.3)
    d = ImageDraw.Draw(img)
    d.ellipse([9, 9, 23, 23], fill=rgba('3a2a48'))
    d.ellipse([13, 8, 26, 21], fill=rgba('e8dcc0'))                    # the dark disc bitten into a crescent
    edge(img, rgba('8a7450'))
    return img


# =====================================================================================================
# Bats (Wings of Darkness, Bat Swarm) and the big bat wings
# =====================================================================================================

def m_bat_fur(seed):
    img = new()
    noise_fill(img, rgba('120a10'), rgba('3a2630'), seed, 2, 0.45)
    return img


def m_bat_membrane(seed):
    img = new()
    noise_fill(img, rgba('1a0810', 235), rgba('4a1624', 235), seed, 5, 0.2)
    veins(img, rgba('7a1a2a', 240), seed + 3, 4)
    return img


def m_red_eye(seed):
    img = new()
    noise_fill(img, rgba('ff2030'), rgba('ffb0b4'), seed, 3, 0.2)
    return img


def m_wing_bone(seed):
    img = new()
    noise_fill(img, rgba('1e1418'), rgba('4a3a40'), seed, 3, 0.3)
    for y in range(0, CELL, 6):
        ImageDraw.Draw(img).line([(0, y), (CELL, y)], fill=rgba('0c0608'), width=1)   # joints
    return img


def m_claw(seed):
    img = new()
    noise_fill(img, rgba('a89c8c'), rgba('e4dccc'), seed, 3, 0.2)
    return img


def m_wing_membrane(seed):
    """Whole-cell (stretched over a wing panel that hangs below a finger): the finger along the top row, a
    scalloped free edge at the bottom, veins fanning down from the finger. Dark leather, a little see-through,
    crimson where the light comes through near the edge."""
    img = new()
    rnd = random.Random(seed)
    big = value_noise(seed, 6)

    def bottom_edge(x):                                                  # three scallops cut into the bottom
        p = (x % 11) / 11.0
        return CELL - 2 - 7 * (1 - math.sin(p * math.pi))
    for y in range(CELL):
        for x in range(CELL):
            if y > bottom_edge(x):
                continue
            t = big(x, y) * 0.8 + rnd.random() * 0.2
            depth = 1 - y / (CELL - 1)                                   # thinner (more crimson) toward the edge
            c = mix(rgba('5a1424', 205), rgba('1a060c', 245), depth * 0.7 + t * 0.3)
            img.putpixel((x, y), c)
    d = ImageDraw.Draw(img)
    for i in range(5):                                                   # veins from the finger
        x0 = 3 + i * 6.5
        d.line([(x0, 2), (x0 + rnd.uniform(-4, 4), 16 + rnd.uniform(0, 6))], fill=rgba('8a2232', 230), width=1)
    d.rectangle([0, 0, CELL - 1, 2], fill=rgba('2a1a20'))                # the finger bone
    d.line([(0, 2), (CELL, 2)], fill=rgba('5a4048'), width=1)
    return img


# =====================================================================================================
# Abyss Shackles: black iron and chains of condensed darkness
# =====================================================================================================

def m_abyss_iron(seed):
    img = new()
    noise_fill(img, rgba('0a0812'), rgba('2c2440'), seed, 6, 0.15)
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(4):
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        d.line([(x, y), (x + rnd.randint(3, 7), y + rnd.randint(-1, 1))], fill=rgba('5a4a7a'), width=1)
    edge(img, rgba('6a5a90'))
    return img


def m_chain_link(seed):
    """Whole-cell: one oval chain link of black iron with a violet glint, see-through in the middle and around it."""
    img = new()
    d = ImageDraw.Draw(img)
    d.ellipse([3, 0, 28, 31], outline=rgba('120c1c'), width=6)
    d.ellipse([5, 2, 26, 29], outline=rgba('2c2440'), width=2)
    d.arc([4, 1, 27, 30], 190, 260, fill=rgba('a888e0'), width=2)
    d.arc([4, 1, 27, 30], 20, 60, fill=rgba('5a4a7a'), width=1)
    return img


def m_abyss(seed):
    img = new()
    noise_fill(img, rgba('02010a', 235), rgba('1c0c30', 215), seed, 6, 0.2)
    return img


def m_abyss_glow(seed):
    img = new()
    noise_fill(img, rgba('6a1aa0'), rgba('d07aff'), seed, 4, 0.3)
    return img


def m_shackle_rune(seed):
    img = m_abyss_iron(seed)
    d = ImageDraw.Draw(img)
    c = rgba('b070ff')
    for x in range(2, CELL, 8):                                          # a row of small crescents along the band
        d.arc([x, 3, x + 6, 9], 300, 160, fill=c, width=1)
    return img


# =====================================================================================================
# The moon (Lunar Battlefield), the blood moon (Blood Moon) and moonlight (Spectral Moonlight Form)
# =====================================================================================================

def craters(img, seed, dark, light, count=6):
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(count):
        x, y, r = rnd.randrange(CELL), rnd.randrange(CELL), rnd.randint(2, 5)
        d.ellipse([x - r, y - r, x + r, y + r], fill=dark)
        d.arc([x - r, y - r, x + r, y + r], 200, 330, fill=light, width=1)


def maria(img, seed, dark, strength):
    """The big dark 'seas' of a moon: soft patches laid over the rock."""
    big = value_noise(seed + 11, 10)
    for y in range(CELL):
        for x in range(CELL):
            t = max(0.0, big(x, y) - 0.45) * strength
            img.putpixel((x, y), mix(img.getpixel((x, y)), dark, t))


def m_moon_surface(seed):
    img = new()
    noise_fill(img, rgba('b8c2d8'), rgba('f8faff'), seed, 7, 0.12)
    maria(img, seed, rgba('7a86a4'), 1.6)
    craters(img, seed, rgba('8a96b0'), rgba('ffffff'), 8)
    return img


def m_moon_glow(seed):
    img = new()
    noise_fill(img, rgba('c8dcff', 40), rgba('ffffff', 80), seed, 5, 0.2)
    return img


def m_blood_moon_surface(seed):
    img = new()
    noise_fill(img, rgba('6a0812'), rgba('e03a36'), seed, 7, 0.12)
    maria(img, seed, rgba('2a0206'), 1.6)
    craters(img, seed, rgba('3a0208'), rgba('ff7060'), 8)
    return img


def m_blood_glow(seed):
    img = new()
    noise_fill(img, rgba('a0101a', 50), rgba('ff4a40', 90), seed, 5, 0.2)
    return img


def m_moonlight(seed):
    img = new()
    noise_fill(img, rgba('9ab8ff', 130), rgba('e8f0ff', 170), seed, 4, 0.25)
    rnd = random.Random(seed)
    for _ in range(10):                                                  # glints
        img.putpixel((rnd.randrange(CELL), rnd.randrange(CELL)), rgba('ffffff', 240))
    return img


def m_moonlight_core(seed):
    img = new()
    noise_fill(img, rgba('dce8ff', 220), rgba('ffffff', 250), seed, 3, 0.2)
    return img


def m_moonlight_face(seed):
    """Whole-cell face of the moonlight figure: two bright eyes, nothing else."""
    img = m_moonlight(seed)
    d = ImageDraw.Draw(img)
    for x0 in (8, 19):
        d.rectangle([x0, 13, x0 + 4, 16], fill=rgba('ffffff', 255))
    return img


def m_beam(seed):
    """Whole-cell: a column of light, brightest in the middle, fading to nothing toward the top."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            across = 1 - abs((x + 0.5) / CELL * 2 - 1)
            up = y / (CELL - 1)
            a = int(255 * (across ** 1.5) * (0.15 + 0.85 * up))
            img.putpixel((x, y), (235, 242, 255, a))
    return img


# =====================================================================================================
# Gaze of Darkness: the eyeball and the darkness it pours over its target
# =====================================================================================================

def m_eye_white(seed):
    img = new()
    noise_fill(img, rgba('d8ccc8'), rgba('fff6f0'), seed, 5, 0.15)
    veins(img, rgba('c0303a', 220), seed + 1, 5, 1, 3)
    return img


def m_eye_iris(seed):
    """Whole-cell (the front of the eyeball): a crimson iris with a slit pupil, on veined white."""
    img = m_eye_white(seed)
    d = ImageDraw.Draw(img)
    d.ellipse([5, 5, 26, 26], fill=rgba('5a0614'))
    for r, c in ((10, '8a0f20'), (8, 'c01a2a'), (5, 'ff4040')):
        d.ellipse([15.5 - r, 15.5 - r, 15.5 + r, 15.5 + r], outline=rgba(c), width=2)
    d.ellipse([13, 6, 18, 25], fill=rgba('050104'))                      # the slit
    d.ellipse([9, 8, 12, 11], fill=rgba('ffffff', 230))                  # a glint: something is reflected in it
    return img


def m_eyelid(seed):
    img = new()
    noise_fill(img, rgba('2a0e1a'), rgba('6a2a3c'), seed, 3, 0.35)
    d = ImageDraw.Draw(img)
    for y in (6, 13, 20, 27):
        d.line([(0, y), (CELL, y + 1)], fill=rgba('1a0610'), width=1)    # wrinkles
    return img


def m_darkness(seed):
    """Solidified darkness: black glass, faintly see-through, with violet-crimson cracks."""
    img = new()
    noise_fill(img, rgba('020004', 225), rgba('16081e', 200), seed, 5, 0.2)
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(3):
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        pts = [(x, y)]
        for _ in range(5):
            x += rnd.randint(-6, 6)
            y += rnd.randint(-6, 6)
            pts.append((x, y))
        d.line(pts, fill=rgba('a020c0', 230), width=1)
    veins(img, rgba('c0202e', 200), seed + 5, 1)
    return img


def m_darkness_edge(seed):
    img = m_darkness(seed)
    edge(img, rgba('6a2a8a', 240))
    return img


# =====================================================================================================
# The Door of Summoning: dark stone, silver runes, doors of black wood
# =====================================================================================================

def m_door_stone(seed):
    img = new()
    noise_fill(img, rgba('1a1824'), rgba('3c3a4c'), seed, 5, 0.3)
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(3):
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        d.line([(x, y), (x + rnd.randint(-5, 5), y + rnd.randint(2, 6))], fill=rgba('0c0a12'), width=1)
    edge(img, rgba('4e4a62'))
    return img


GLYPHS = [
    [(0, 0), (4, 0), (4, 6), (0, 6)],                 # a box
    [(2, 0), (4, 3), (2, 6), (0, 3)],                 # a diamond
    [(0, 6), (2, 0), (4, 6)],                         # a peak
    [(0, 0), (4, 6), (4, 0), (0, 6)],                 # a cross-stroke
]


def m_door_rune(seed):
    """Dark stone carved with glowing silver-violet symbols and crescents."""
    img = m_door_stone(seed)
    d = ImageDraw.Draw(img)
    rnd = random.Random(seed)
    c = rgba('c8b8ff')
    for row in range(3):
        for col in range(3):
            x0, y0 = 3 + col * 10, 3 + row * 10
            if (row + col) % 3 == 1:
                d.arc([x0, y0, x0 + 6, y0 + 6], 300, 150, fill=c, width=1)
            else:
                g = rnd.choice(GLYPHS)
                d.line([(x0 + px, y0 + py) for px, py in g], fill=c, width=1)
    return img


def m_door_wood(seed):
    img = new()
    big = value_noise(seed, 3)
    rnd = random.Random(seed)
    for y in range(CELL):
        for x in range(CELL):
            grain = math.sin(x * 0.9 + big(x, y) * 4) * 0.5 + 0.5
            img.putpixel((x, y), mix(rgba('0e0810'), rgba('2e1e2c'), grain * 0.7 + rnd.random() * 0.3))
    for x in (0, 15, 31):
        ImageDraw.Draw(img).line([(x, 0), (x, CELL)], fill=rgba('060306'), width=1)
    return img


def m_door_panel(seed):
    """Whole-cell (stretched over one door leaf): black wood framed in silver, a crescent over an open eye."""
    img = m_door_wood(seed)
    d = ImageDraw.Draw(img)
    silver = rgba('c8c8dc')
    d.rectangle([1, 1, CELL - 2, CELL - 2], outline=silver, width=1)
    d.rectangle([4, 4, CELL - 5, CELL - 5], outline=rgba('5a5870'), width=1)
    d.arc([9, 5, 22, 18], 210, 330, fill=silver, width=2)                # the crescent
    d.ellipse([10, 19, 21, 26], outline=rgba('b8a8ff'), width=1)         # the eye
    d.ellipse([14, 20, 17, 25], fill=rgba('d8ccff'))
    return img


def m_door_silver(seed):
    img = new()
    noise_fill(img, rgba('7a7890'), rgba('e0e0f0'), seed, 2, 0.4)
    edge(img, rgba('4a4860'))
    return img


def m_spirit_glow(seed):
    img = new()
    noise_fill(img, rgba('7a5aff', 200), rgba('e0d8ff', 240), seed, 3, 0.3)
    return img


# =====================================================================================================
# A creature of the Spirit World (Door of Summoning)
# =====================================================================================================

def m_spirit_flesh(seed):
    img = new()
    noise_fill(img, rgba('1a2a5a', 190), rgba('5a7ac8', 210), seed, 4, 0.3)
    veins(img, rgba('b0e0ff', 230), seed + 2, 4)
    return img


def m_spirit_dark(seed):
    img = new()
    noise_fill(img, rgba('0a0c24', 220), rgba('24306a', 220), seed, 3, 0.3)
    return img


def m_spirit_eye(seed):
    img = new()
    noise_fill(img, rgba('a0f0ff'), rgba('ffffff'), seed, 3, 0.2)
    return img


def m_spirit_bone(seed):
    img = new()
    noise_fill(img, rgba('b8c0d8', 230), rgba('eef2ff', 240), seed, 3, 0.25)
    return img


# =====================================================================================================
# Creation: the Seed of Life
# =====================================================================================================

def m_seed_husk(seed):
    img = new()
    noise_fill(img, rgba('5a3a14'), rgba('a8823a'), seed, 3, 0.35)
    d = ImageDraw.Draw(img)
    for x in range(0, CELL, 6):
        d.line([(x, 0), (x + 3, CELL)], fill=rgba('3a2408'), width=1)
    return img


def m_seed_core(seed):
    img = new()
    noise_fill(img, rgba('f0c040'), rgba('fff8c8'), seed, 3, 0.25)
    return img


def m_stem(seed):
    img = new()
    noise_fill(img, rgba('2a5a1a'), rgba('6aa83a'), seed, 2, 0.3)
    for x in (10, 21):
        ImageDraw.Draw(img).line([(x, 0), (x, CELL)], fill=rgba('1a3a10'), width=1)
    return img


def m_leaf(seed):
    img = new()
    noise_fill(img, rgba('2e7a24'), rgba('8ad04a'), seed, 4, 0.2)
    d = ImageDraw.Draw(img)
    d.line([(16, 0), (16, CELL)], fill=rgba('c0f080'), width=1)          # the middle rib
    for y in range(4, CELL, 7):
        d.line([(16, y), (4, y - 4)], fill=rgba('a8e070'), width=1)
        d.line([(16, y), (28, y - 4)], fill=rgba('a8e070'), width=1)
    return img


def m_petal(seed):
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            t = y / (CELL - 1)
            img.putpixel((x, y), mix(rgba('fffbe8', 240), rgba('ffd27a', 240), t))
    veins(img, rgba('ffe6a8', 255), seed, 3, 1, 3)
    return img


def m_gold_glow(seed):
    img = new()
    noise_fill(img, rgba('ffd040', 200), rgba('fff6c0', 240), seed, 3, 0.3)
    return img


# =====================================================================================================
# Beauty: the crescent crown and the goddess behind the caster
# =====================================================================================================

def m_crown_silver(seed):
    img = new()
    noise_fill(img, rgba('9aa0b8'), rgba('f4f6ff'), seed, 2, 0.35)
    edge(img, rgba('6a6e88'))
    return img


def m_moonstone(seed):
    img = new()
    noise_fill(img, rgba('8a0f2a'), rgba('ff6a8a'), seed, 3, 0.25)
    d = ImageDraw.Draw(img)
    d.line([(4, 4), (10, 4)], fill=rgba('ffd0dc'), width=1)
    return img


def m_crescent(seed):
    img = new()
    noise_fill(img, rgba('d8e4ff'), rgba('ffffff'), seed, 3, 0.2)
    return img


def m_goddess(seed):
    """Her body of light: pale, luminous, a little see-through."""
    img = new()
    noise_fill(img, rgba('e8e0ff', 150), rgba('ffffff', 190), seed, 5, 0.2)
    return img


def m_goddess_dress(seed):
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            fold = math.sin(x * 0.7 + seed) * 0.5 + 0.5
            t = y / (CELL - 1)
            c = mix(rgba('c8b8f0', 170), rgba('f4eeff', 200), fold * 0.6)
            c = mix(c, rgba('8a70c8', 120), t * 0.6)                     # thinning out toward the hem
            img.putpixel((x, y), c)
    return img


def m_goddess_hair(seed):
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            strand = math.sin(x * 1.3 + math.sin(y * 0.3) * 2) * 0.5 + 0.5
            img.putpixel((x, y), mix(rgba('a0a8d8', 190), rgba('f0f2ff', 230), strand))
    return img


def m_goddess_face(seed):
    """Whole-cell: a calm face of light, eyes closed."""
    img = m_goddess(seed)
    d = ImageDraw.Draw(img)
    c = rgba('7a6aa8', 230)
    d.arc([7, 11, 14, 17], 20, 160, fill=c, width=1)                     # closed eyes
    d.arc([18, 11, 25, 17], 20, 160, fill=c, width=1)
    d.line([(14, 23), (18, 23)], fill=rgba('b07a9a', 230), width=1)      # lips
    return img


# =====================================================================================================
# The marks of a tamed beast
# =====================================================================================================

def m_blood(seed):
    img = new()
    noise_fill(img, rgba('6a0814'), rgba('d0202e'), seed, 3, 0.3)
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
