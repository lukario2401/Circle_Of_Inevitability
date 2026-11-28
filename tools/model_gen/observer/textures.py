"""Procedural material cells for the Observer (Sublunary Eye pathway) model atlases.

Same layout as tools/model_gen/star_dragon/textures.py: every model texture is an atlas of 4-wide rows of cells,
32x32 texture pixels each (one cell = 16 model pixels at 2 texture pixels per model pixel). A cube face samples the
part of its material's cell that matches its size, so the texel density stays even; a face listed in a cube's
'full' (or given another material through faces={...}) shows its whole cell, stretched (a wing, a petal, a blade).

The palette (docs/pathways/observer/OBSERVER.md, "Theme"): ink black and parchment cream for Paintings and the
figures of fiction, earth brown and moss green for the Totem, pale teal for the Pixie's wings, black-violet for the
Dimensional Shadow, white-gold for the Sublunary Eye. Pathway color 0x6C5CE7.

Everything is drawn as if painted: brush strokes along the grain, ink outlines, a wash of watercolor. To add a
material: a function m_<name>(seed) returning a 32x32 RGBA image, then use '<name>' in a model's material list.
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


def vertical_glow(img, dark, light, core_width=0.25):
    """Brightest along the middle column, darker toward both sides."""
    for y in range(CELL):
        for x in range(CELL):
            d = abs((x + 0.5) / CELL - 0.5) * 2
            t = max(0.0, 1 - d / (1 - core_width)) if d > core_width else 1.0
            img.putpixel((x, y), mix(dark, light, t))


def radial(img, inner, outer, power=1.0):
    for y in range(CELL):
        for x in range(CELL):
            d = math.hypot((x + 0.5) / CELL - 0.5, (y + 0.5) / CELL - 0.5) / 0.7071
            img.putpixel((x, y), mix(inner, outer, d ** power))


def strokes(img, color, seed, count=10, length=10, horizontal=False, width=1):
    """Brush strokes: short slightly curved lines, all running one way (the way the brush went)."""
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(count):
        x, y = rnd.uniform(-4, CELL), rnd.uniform(-4, CELL)
        a = (0.0 if horizontal else math.pi / 2) + rnd.uniform(-0.25, 0.25)
        pts = []
        for k in range(4):
            pts.append((x, y))
            x += math.cos(a) * length / 3
            y += math.sin(a) * length / 3
            a += rnd.uniform(-0.2, 0.2)
        d.line(pts, fill=color, width=width)


def ink_outline(img, color, width=1):
    """An ink line drawn round the cell: what makes a painted figure read as drawn."""
    px = img.load()
    for y in range(CELL):
        for x in range(CELL):
            if min(x, y, CELL - 1 - x, CELL - 1 - y) < width:
                px[x, y] = color


def soft_rim(img, color, width=3):
    px = img.load()
    for y in range(CELL):
        for x in range(CELL):
            e = min(x, y, CELL - 1 - x, CELL - 1 - y)
            if e < width:
                px[x, y] = mix(px[x, y], color, (1 - e / width) * 0.8)


def speckle(img, colors, seed, count):
    rnd = random.Random(seed)
    px = img.load()
    for _ in range(count):
        px[rnd.randrange(CELL), rnd.randrange(CELL)] = colors[rnd.randrange(len(colors))]


# =====================================================================================================
# The Totem: carved wood, its carvings, the watching eye, moss, feathers, the earth it stands in
# =====================================================================================================

def m_totem_wood(seed):
    """Weathered brown wood, the grain running up, a few split lines."""
    img = new()
    big = value_noise(seed, 8)
    for y in range(CELL):
        for x in range(CELL):
            grain = 0.5 + 0.5 * math.sin(x * 0.7 + big(x, y // 4) * 3)
            img.putpixel((x, y), mix(rgba('5a3820'), rgba('8a5e36'), grain * 0.7 + big(x, y) * 0.3))
    strokes(img, rgba('3a2414'), seed + 1, 4, 16)
    strokes(img, rgba('a8784a', 160), seed + 2, 3, 12)
    return img


def m_totem_carving(seed):
    """The carved bands: deep cut zigzags and rings, the cuts darker, the ridges worn pale."""
    img = new()
    noise_fill(img, rgba('3a2414'), rgba('6e4628'), seed, 4, 0.2)
    d = ImageDraw.Draw(img)
    for row in (4, 15, 26):
        pts = [(x, row + (3 if (x // 4) % 2 else -3)) for x in range(0, CELL + 4, 4)]
        d.line(pts, fill=rgba('1e1008'), width=2)
        d.line([(p[0], p[1] - 1) for p in pts], fill=rgba('b08050'))
    for x in range(2, CELL, 8):
        d.ellipse([x, 8, x + 3, 11], fill=rgba('1e1008'))
        d.ellipse([x, 19, x + 3, 22], fill=rgba('1e1008'))
    return img


def m_totem_face(seed):
    """A carved face for the totem's sides: two hollow eyes, a long nose, a square open mouth, painted lines."""
    img = m_totem_carving(seed)
    d = ImageDraw.Draw(img)
    d.rectangle([5, 7, 12, 12], fill=rgba('1e1008'))
    d.rectangle([19, 7, 26, 12], fill=rgba('1e1008'))
    d.point((8, 9), fill=rgba('e8d8a0'))
    d.point((22, 9), fill=rgba('e8d8a0'))
    d.rectangle([14, 8, 17, 20], fill=rgba('8a5e36'))
    d.rectangle([9, 23, 22, 28], fill=rgba('1e1008'))
    for x in range(10, 22, 3):
        d.line([(x, 23), (x, 25)], fill=rgba('e8d8a0'))
    d.line([(3, 4), (12, 4)], fill=rgba('6a8a3a'), width=2)                # painted moss-green brows
    d.line([(19, 4), (28, 4)], fill=rgba('6a8a3a'), width=2)
    return img


def m_totem_eye(seed):
    """The watching eye at the top: cream white, a moss-green iris and a black pupil, a dark lid line."""
    img = new()
    noise_fill(img, rgba('3a2414'), rgba('5a3a20'), seed, 4, 0.2)
    d = ImageDraw.Draw(img)
    d.ellipse([2, 8, 29, 23], fill=rgba('f0e6c8'))
    d.ellipse([10, 9, 21, 22], fill=rgba('5a8a3a'))
    d.ellipse([12, 11, 19, 20], fill=rgba('2e5a1e'))
    d.ellipse([14, 13, 17, 18], fill=rgba('0a0806'))
    d.point((13, 12), fill=rgba('ffffff'))
    d.arc([2, 6, 29, 25], 180, 360, fill=rgba('1e1008'), width=2)
    d.arc([2, 6, 29, 25], 0, 180, fill=rgba('1e1008'), width=1)
    return img


def m_moss(seed):
    """Moss: soft green with lighter tufts."""
    img = new()
    noise_fill(img, rgba('3a5a22'), rgba('6a9a3a'), seed, 6, 0.2)
    speckle(img, [rgba('9ac85a')], seed, 14)
    return img


def m_feather(seed):
    """A feather (shows its whole cell): cream with brown bars, a dark shaft down the middle, clear round it."""
    img = new()
    px = img.load()
    for y in range(CELL):
        for x in range(CELL):
            u, v = (x + 0.5) / CELL - 0.5, (y + 0.5) / CELL
            half = 0.42 * math.sin(math.pi * min(1.0, v * 1.05))
            if abs(u) > half:
                continue
            bar = (y // 4) % 2
            c = rgba('e8dcc0') if bar else rgba('8a5a30')
            px[x, y] = mix(c, rgba('fff6e0'), 0.2 * (1 - abs(u) / max(0.01, half)))
    d = ImageDraw.Draw(img)
    d.line([(15.5, 1), (15.5, CELL)], fill=rgba('3a2414'))
    return img


def m_earth(seed):
    """Packed earth with pebbles at the totem's foot."""
    img = new()
    noise_fill(img, rgba('3a2a1a'), rgba('6a5034'), seed, 4, 0.35)
    speckle(img, [rgba('8a7a6a'), rgba('2a1e12')], seed + 3, 20)
    return img


# =====================================================================================================
# Paintings: the frame and the canvas's back (the painting itself is the model's texture "1")
# =====================================================================================================

def m_frame_wood(seed):
    """The frame: ink-black lacquered wood, a gilt bead running along it."""
    img = new()
    noise_fill(img, rgba('14100e'), rgba('2e2620'), seed, 4, 0.2)
    d = ImageDraw.Draw(img)
    for y in (6, 25):
        d.line([(0, y), (CELL, y)], fill=rgba('c8a050'))
        d.line([(0, y + 1), (CELL, y + 1)], fill=rgba('6a5020'))
    strokes(img, rgba('3e342a'), seed, 6, 12, True)
    return img


def m_frame_gold(seed):
    """The gilt corners and edge: worn gold leaf over red bole."""
    img = new()
    noise_fill(img, rgba('a07020'), rgba('f0d080'), seed, 3, 0.3)
    speckle(img, [rgba('8a2a1a'), rgba('fff0b0')], seed, 18)
    return img


def m_canvas_back(seed):
    """The back of a canvas: raw linen and the stretcher bars."""
    img = new()
    noise_fill(img, rgba('b8a888'), rgba('d8ccae'), seed, 2, 0.5)
    d = ImageDraw.Draw(img)
    for k in range(0, CELL, 2):
        d.line([(k, 0), (k, CELL)], fill=rgba('a89878', 90))
    d.rectangle([0, 13, CELL, 18], fill=rgba('8a6a44'))
    d.rectangle([13, 0, 18, CELL], fill=rgba('8a6a44'))
    return img


# =====================================================================================================
# Fiction made real: painted flesh (the Giant's Fist), ink and paper (the Knight, the Archer, the servants)
# =====================================================================================================

def m_paint_skin(seed):
    """A painted giant's skin: warm ochre laid in broad brush strokes, rosy where it catches the light."""
    img = new()
    noise_fill(img, rgba('b07040'), rgba('e8b07a'), seed, 5, 0.15)
    strokes(img, rgba('f0c090', 220), seed + 1, 8, 12, True, 2)
    strokes(img, rgba('8a4a24', 200), seed + 2, 6, 10, True)
    strokes(img, rgba('e88a70', 140), seed + 3, 4, 8, True, 2)
    return img


def m_paint_skin_shadow(seed):
    """The giant's shadowed side, hatched in ink."""
    img = new()
    noise_fill(img, rgba('6a3a1e'), rgba('9a5a30'), seed, 5, 0.15)
    d = ImageDraw.Draw(img)
    for k in range(-CELL, CELL, 4):
        d.line([(k, CELL), (k + CELL, 0)], fill=rgba('2a160a', 160))
    return img


def m_ink_line(seed):
    """Wet black ink, a little sheen on it."""
    img = new()
    noise_fill(img, rgba('080606'), rgba('241c1a'), seed, 4, 0.2)
    strokes(img, rgba('3e3430'), seed, 4, 12)
    return img


def m_knuckle(seed):
    """The painted knuckles and nails: pale highlights and ink creases."""
    img = m_paint_skin(seed)
    d = ImageDraw.Draw(img)
    for x in (6, 15, 24):
        d.arc([x - 4, 10, x + 4, 22], 200, 340, fill=rgba('2a160a'), width=2)
        d.ellipse([x - 2, 12, x + 2, 15], fill=rgba('fff0d8'))
    return img


def m_paper(seed):
    """Parchment: cream paper with fibres and foxing."""
    img = new()
    noise_fill(img, rgba('d8c8a0'), rgba('f4ead0'), seed, 4, 0.3)
    speckle(img, [rgba('b8a070'), rgba('c8b088')], seed, 14)
    return img


def m_ink_armor(seed):
    """Armor drawn in ink on paper: cream plates, bold black outlines, hatching for the shadowed half, rivets."""
    img = m_paper(seed)
    d = ImageDraw.Draw(img)
    for y in (0, 10, 21):
        d.line([(0, y), (CELL, y)], fill=rgba('0a0806'), width=2)
    for k in range(16, CELL + 16, 3):
        d.line([(k, CELL), (k - 16, 10)], fill=rgba('2a2420', 150))
    for x in (4, 27):
        for y in (5, 16, 27):
            d.point((x, y), fill=rgba('0a0806'))
    ink_outline(img, rgba('0a0806'))
    return img


def m_ink_cloth(seed):
    """Cloth in ink wash: grey-blue folds with dark brush lines."""
    img = new()
    noise_fill(img, rgba('3a4250'), rgba('7a8698'), seed, 5, 0.2)
    strokes(img, rgba('1a1e26'), seed, 8, 16, False, 1)
    strokes(img, rgba('a8b4c4', 160), seed + 4, 4, 12)
    return img


def m_ink_wash_red(seed):
    """A wash of red watercolor (the Knight's plume and tabard, the Archer's fletching)."""
    img = new()
    noise_fill(img, rgba('8a1e1a'), rgba('d8504a'), seed, 4, 0.2)
    strokes(img, rgba('f08070', 170), seed, 6, 10)
    return img


def m_ink_steel(seed):
    """A blade or a helm's face in grey ink wash, a white streak of light along it."""
    img = new()
    vertical_glow(img, rgba('4a4e56'), rgba('e8ecf0'), 0.15)
    strokes(img, rgba('2a2e36'), seed, 4, 14)
    return img


def m_bow_wood(seed):
    """Bow wood: dark lacquer, painted gold bands."""
    img = m_totem_wood(seed)
    d = ImageDraw.Draw(img)
    for y in (6, 16, 26):
        d.line([(0, y), (CELL, y)], fill=rgba('c8a050'), width=2)
    return img


def m_ink_body(seed):
    """An Ink Servant's body: black ink with a faint teal glow in it, nearly solid (the model is small)."""
    img = new()
    big = value_noise(seed, 5)
    for y in range(CELL):
        for x in range(CELL):
            e = min(x, y, CELL - 1 - x, CELL - 1 - y) / (CELL / 2)
            n = big(x, y)
            a = 255 if e > 0.04 else 200                                     # solid ink, the outermost pixels a little soft
            c = mix(rgba('0a0a10'), rgba('2a5a60'), n * 0.6)
            img.putpixel((x, y), c[:3] + (a,))
    return img


def m_ink_eye(seed):
    """The servants' and figures' eyes: a soft pale-teal glow."""
    img = new()
    radial(img, rgba('f0ffff'), rgba('6ad8d0'), 0.8)
    return img


# =====================================================================================================
# The Pixie's wings, the faint Observer, the Dimensional Shadow, the Bloom flower, the shadow blade
# =====================================================================================================

def m_wing(seed):
    """A dragonfly wing (shows its whole cell): pale teal, see-through, a net of veins, root at the left."""
    img = new()
    px = img.load()
    for y in range(CELL):
        for x in range(CELL):
            u, v = (x + 0.5) / CELL, (y + 0.5) / CELL - 0.5
            half = 0.36 * math.sqrt(max(0.0, 1 - ((u - 0.52) / 0.5) ** 2))
            if abs(v + 0.04 * math.sin(u * 3)) > half:
                continue
            t = abs(v) / max(0.01, half)
            c = mix(rgba('e8ffff', 120), rgba('8ad8d0', 70), u)
            px[x, y] = mix(c, rgba('f8ffff', 200), max(0.0, t - 0.8) * 4)
    d = ImageDraw.Draw(img)
    d.line([(0, 15), (31, 13)], fill=rgba('d0fffa', 220))                  # the leading vein
    for k in range(5):
        x0 = 4 + k * 5.5
        d.line([(x0, 14), (x0 + 3, 4 + k)], fill=rgba('b8f0ea', 150))
        d.line([(x0, 15), (x0 + 3, 26 - k)], fill=rgba('b8f0ea', 150))
    rnd = random.Random(seed)
    for _ in range(10):
        x, y = rnd.uniform(4, 28), rnd.uniform(8, 22)
        d.line([(x, y), (x + rnd.uniform(-2, 2), y + rnd.uniform(-2, 2))], fill=rgba('d0fffa', 90))
    return img


def m_wing_flip(seed):
    return m_wing(seed).transpose(Image.FLIP_LEFT_RIGHT)


def m_faint_light(seed):
    """The faint, unattainably tall Observer: white-gold light you can hardly see, brighter down the middle."""
    img = new()
    vertical_glow(img, rgba('fff0c0', 50), rgba('fffcf0', 150), 0.2)
    speckle(img, [rgba('ffffff', 200)], seed, 8)
    return img


def m_faint_edge(seed):
    """The Observer's outline: a thin rim of gold."""
    img = new()
    vertical_glow(img, rgba('ffd880', 120), rgba('ffffff', 230), 0.4)
    return img


def m_shadow_body(seed):
    """The Dimensional Shadow: black-violet, see-through, folds of a higher dimension (lines that do not meet)."""
    img = new()
    noise_fill(img, rgba('0a0614', 180), rgba('2a1a4a', 210), seed, 5, 0.15)
    d = ImageDraw.Draw(img)
    rnd = random.Random(seed)
    for _ in range(5):
        x, y = rnd.uniform(0, CELL), rnd.uniform(0, CELL)
        a = rnd.uniform(0, math.tau)
        d.line([(x, y), (x + math.cos(a) * 9, y + math.sin(a) * 9)], fill=rgba('8a6aff', 140))
    soft_rim(img, rgba('6c5ce7', 220), 2)
    return img


def m_shadow_edge(seed):
    """The Shadow's bright violet seams."""
    img = new()
    vertical_glow(img, rgba('4a2ab0', 160), rgba('d8ccff', 255), 0.25)
    return img


def m_petal(seed):
    """A Bloom petal (shows its whole cell): white at the tip, gold toward the heart, a soft edge."""
    img = new()
    px = img.load()
    for y in range(CELL):
        for x in range(CELL):
            u, v = (x + 0.5) / CELL - 0.5, (y + 0.5) / CELL
            half = 0.46 * math.sin(math.pi * min(1.0, v)) ** 0.6
            if abs(u) > half:
                continue
            c = mix(rgba('ffffff'), rgba('ffc850'), v ** 1.4)
            e = abs(u) / max(0.01, half)
            px[x, y] = c[:3] + (int(255 * (1 - max(0.0, e - 0.75) * 3)),)
    d = ImageDraw.Draw(img)
    d.line([(15.5, 4), (15.5, 30)], fill=rgba('ffe9a0', 160))
    return img


def m_flower_heart(seed):
    """The flower's heart: gold seeds round a white-gold eye."""
    img = new()
    radial(img, rgba('fffcf0'), rgba('d89a20'), 0.9)
    speckle(img, [rgba('8a5a10'), rgba('fff0a0')], seed, 30)
    return img


def m_stem(seed):
    """A green stem."""
    img = new()
    vertical_glow(img, rgba('2e6a2a'), rgba('8ad06a'), 0.3)
    return img


def m_leaf(seed):
    """A leaf (shows its whole cell)."""
    img = new()
    px = img.load()
    for y in range(CELL):
        for x in range(CELL):
            u, v = (x + 0.5) / CELL - 0.5, (y + 0.5) / CELL
            half = 0.4 * math.sin(math.pi * v)
            if abs(u) > half:
                continue
            px[x, y] = mix(rgba('3a8a3a'), rgba('9ae07a'), 1 - abs(u) / max(0.01, half))
    ImageDraw.Draw(img).line([(15.5, 0), (15.5, 31)], fill=rgba('2a5a22'))
    return img


def m_shadow_blade(seed):
    """Shadow From Above's blade: black-violet, a violet-white edge."""
    img = new()
    vertical_glow(img, rgba('0a0614', 230), rgba('3a2a6a', 250), 0.2)
    d = ImageDraw.Draw(img)
    d.line([(0, 0), (0, CELL)], fill=rgba('e0d8ff'), width=2)
    d.line([(CELL - 1, 0), (CELL - 1, CELL)], fill=rgba('e0d8ff'), width=2)
    return img


def m_gold_light(seed):
    """Solid white-gold light (the Observer's crown of eyes, the Sublunary Eye's heart)."""
    img = new()
    noise_fill(img, rgba('ffd070'), rgba('fffbe8'), seed, 3, 0.2)
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
