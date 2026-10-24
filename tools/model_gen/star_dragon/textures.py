"""Procedural material cells for the Star Dragon (Condenser pathway) model atlases.

Same layout as tools/model_gen/illuminator/textures.py: every model texture is an atlas of 4-wide rows of cells,
32x32 texture pixels each (one cell = 16 model pixels at 2 texture pixels per model pixel). A cube face samples the
part of its material's cell that matches its size, so the texel density stays even; a face listed in a cube's
'full' (or given another material through faces={...}) shows its whole cell, stretched (a wing, a ring, a beam).

The palette (docs/STAR_DRAGON.md, "Theme"): star white and deep indigo-black for the void, dark green for the
Weakening rays, pale blue for the Piercing Ray, gold-white for sunlight and fusion, silver-teal for the Tidal
Scholar's scales and waves. Pathway color 0x6A5CFF.
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


def stars(img, seed, count, colors, soft_every=4):
    rnd = random.Random(seed)
    px = img.load()
    for i in range(count):
        x, y = rnd.randrange(1, CELL - 1), rnd.randrange(1, CELL - 1)
        c = colors[rnd.randrange(len(colors))]
        px[x, y] = c
        if soft_every and i % soft_every == 0:
            soft = c[:3] + (min(255, c[3]) // 2,)
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                px[x + dx, y + dy] = soft


def cracks(img, color, seed, count=4, step=4):
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(count):
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        a = rnd.random() * math.tau
        for _ in range(4):
            nx, ny = x + math.cos(a) * step, y + math.sin(a) * step
            d.line([(x, y), (nx, ny)], fill=color)
            x, y = nx, ny
            a += rnd.uniform(-0.8, 0.8)


def rim(img, color, width=2):
    """A brighter frame round the cell: the edge of a shell of light."""
    px = img.load()
    for y in range(CELL):
        for x in range(CELL):
            e = min(x, y, CELL - 1 - x, CELL - 1 - y)
            if e < width:
                t = 1 - e / width
                px[x, y] = mix(px[x, y], color, t * 0.8)


# =====================================================================================================
# Stone and fire: the grazed asteroid, Planetfall, the Dragon's asteroid
# =====================================================================================================

def m_asteroid_rock(seed):
    """Dark pitted stone, a few glowing seams where the heat of entry has cracked it."""
    img = new()
    noise_fill(img, rgba('2a2524'), rgba('6a605a'), seed, 4, 0.3)
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(4):                                                    # craters
        x, y, r = rnd.randrange(4, 28), rnd.randrange(4, 28), rnd.uniform(1.5, 3.5)
        d.ellipse([x - r, y - r, x + r, y + r], fill=rgba('231e1d'))
        d.arc([x - r, y - r, x + r, y + r], 200, 340, fill=rgba('8a7e76'))
    cracks(img, rgba('ff8a3a', 230), seed + 3, 3)
    cracks(img, rgba('ffd27a', 200), seed + 5, 1, 3)
    return img


def m_asteroid_ember(seed):
    """The leading face: stone glowing white-orange from the fall."""
    img = new()
    noise_fill(img, rgba('c24a12'), rgba('ffd890'), seed, 3, 0.3)
    cracks(img, rgba('fff6e0'), seed + 1, 4)
    return img


def m_flame_wrap(seed):
    """Fire streaming behind a falling rock: white-gold at the root, orange, gone at the tip (top of the cell =
    the root)."""
    img = new()
    big = value_noise(seed, 3)
    for y in range(CELL):
        for x in range(CELL):
            t = (y + 0.5) / CELL
            side = abs((x + 0.5) / CELL - 0.5) * 2
            flick = big(x, y * 0.5)
            c = mix(rgba('fff4d0', 245), rgba('ff7a20', 200), t * 1.4)
            c = mix(c, rgba('a02a08', 0), max(0.0, t * 1.2 - 0.3))
            a = c[3] * max(0.0, 1 - side ** 1.5) * (0.6 + 0.4 * flick)
            img.putpixel((x, y), c[:3] + (int(a),))
    return img


# =====================================================================================================
# Starlight: a grazed star, fusion, sunlight, the neutron star
# =====================================================================================================

def m_star_core(seed):
    """White starlight with a faint gold heart."""
    img = new()
    radial(img, rgba('ffffff', 255), rgba('fff0c0', 235), 1.6)
    return img


def m_star_spike(seed):
    """A spike of light: bright along the middle line, fading to the sides and the tip."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            side = abs((x + 0.5) / CELL - 0.5) * 2
            tip = abs((y + 0.5) / CELL - 0.5) * 2
            a = max(0.0, 1 - side * 1.6) * max(0.0, 1 - tip ** 2)
            img.putpixel((x, y), (255, 252, 236, int(255 * a)))
    return img


def m_fusion_core(seed):
    """White-hot plasma, gold at the edge, slowly churning."""
    img = new()
    big = value_noise(seed, 4)
    for y in range(CELL):
        for x in range(CELL):
            d = math.hypot((x + 0.5) / CELL - 0.5, (y + 0.5) / CELL - 0.5) / 0.7071
            t = d * 0.8 + big(x, y) * 0.3
            img.putpixel((x, y), mix(rgba('ffffff'), rgba('ffb43a', 230), t))
    return img


def m_magnetic_ring(seed):
    """The magnetic bottle round a fusion cluster: violet-blue bands with bright pulses running along them."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            band = 0.5 + 0.5 * math.sin((x + 0.5) / CELL * math.tau * 3)
            side = abs((y + 0.5) / CELL - 0.5) * 2
            c = mix(rgba('5a4cff', 150), rgba('d8e4ff', 255), band * (1 - side))
            img.putpixel((x, y), c)
    return img


def m_neutron_core(seed):
    """Blinding blue-white: the densest thing there is."""
    img = new()
    radial(img, rgba('ffffff'), rgba('9ec8ff', 245), 2.2)
    return img


def m_pulsar_beam(seed):
    """A beam of the pulsar: blue-white along the middle, gone at the sides, fading toward the tip (bottom)."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            side = abs((x + 0.5) / CELL - 0.5) * 2
            t = (y + 0.5) / CELL
            a = max(0.0, 1 - side * 1.3) ** 1.4 * (1 - t * 0.8)
            c = mix(rgba('ffffff'), rgba('8ab8ff'), side * 1.4)
            img.putpixel((x, y), c[:3] + (int(240 * a),))
    return img


def m_pulsar_beam_up(seed):
    """The same beam turned over: brightest at the bottom of the cell (the root, by the core)."""
    return m_pulsar_beam(seed).transpose(Image.FLIP_TOP_BOTTOM)


# =====================================================================================================
# The void: Cosmic Void, the Heavy Sphere, the Singularity
# =====================================================================================================

def m_void_inside(seed):
    """Deep indigo-black with a scatter of far-off glimmers."""
    img = new()
    noise_fill(img, rgba('05040e', 235), rgba('1c1640', 235), seed, 5, 0.15)
    stars(img, seed, 14, [rgba('ffffff'), rgba('c8c0ff'), rgba('8a7cff')], 5)
    return img


def m_void_rim(seed):
    """A thin skin of violet light round the void, see-through in the middle."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            img.putpixel((x, y), rgba('3a2c9a', 60))
    rim(img, rgba('8a7cff', 220), 4)
    stars(img, seed, 5, [rgba('ffffff', 200)], 0)
    return img


def m_frost_shell(seed):
    """Ice near absolute zero: pale and see-through, cracked white."""
    img = new()
    noise_fill(img, rgba('9ad4ff', 90), rgba('e8f8ff', 150), seed, 4, 0.2)
    cracks(img, rgba('ffffff', 220), seed + 2, 5, 5)
    rim(img, rgba('f4fcff', 200), 2)
    return img


def m_dense_air(seed):
    """Air so dense it shows: smoky indigo, see-through, a faint grid bent by the weight."""
    img = new()
    noise_fill(img, rgba('12103a', 110), rgba('3a2e8a', 140), seed, 5, 0.1)
    d = ImageDraw.Draw(img)
    for i in range(0, CELL, 8):
        d.line([(i, 0), (i + 2, CELL)], fill=rgba('8a7cff', 120))
        d.line([(0, i), (CELL, i + 2)], fill=rgba('8a7cff', 120))
    rim(img, rgba('b0a8ff', 200), 2)
    return img


def m_black(seed):
    """Nothing comes back: black."""
    img = new()
    noise_fill(img, rgba('000000'), rgba('06040c'), seed, 6, 0.05)
    return img


def m_photon_ring(seed):
    """Light caught circling the black hole: a thin gold-white ring on clear (shows its whole cell)."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            r = math.hypot((x + 0.5) / CELL - 0.5, (y + 0.5) / CELL - 0.5) * 2
            a = max(0.0, 1 - abs(r - 0.82) / 0.1)
            inner = max(0.0, 1 - abs(r - 0.66) / 0.05) * 0.4
            c = mix(rgba('ffffff'), rgba('ffc070'), (x + y) / (2 * CELL))
            img.putpixel((x, y), c[:3] + (int(255 * min(1.0, a + inner)),))
    return img


# =====================================================================================================
# Scales and water: the Tidal Scholar
# =====================================================================================================

def m_scale_teal(seed):
    """Overlapping scales of silver-teal, each with a bright rim of starlight."""
    img = new()
    noise_fill(img, rgba('2a6a70'), rgba('6ab8b4'), seed, 4, 0.2)
    d = ImageDraw.Draw(img)
    for row in range(0, CELL + 4, 4):
        off = 2 if (row // 4) % 2 else 0
        for col in range(-4, CELL + 4, 4):
            x, y = col + off, row
            d.arc([x - 2, y - 3, x + 2, y + 1], 20, 160, fill=rgba('c8f4ec'))
            d.point((x, y - 1), fill=rgba('1e4a50'))
    return img


def m_scale_belly(seed):
    """The pale banded underside."""
    img = new()
    noise_fill(img, rgba('8ab8ae'), rgba('d4ece4'), seed, 4, 0.15)
    d = ImageDraw.Draw(img)
    for y in range(2, CELL, 5):
        d.line([(0, y), (CELL, y)], fill=rgba('6a9890'))
    return img


def m_crest(seed):
    """Fins and spines: silver, translucent toward the edge."""
    img = new()
    vertical_glow(img, rgba('9ad8d0', 160), rgba('f4fffc', 255), 0.2)
    d = ImageDraw.Draw(img)
    for x in range(3, CELL, 6):
        d.line([(x, 0), (x + 3, CELL)], fill=rgba('5a9a94', 200))
    return img


def m_lizard_eye(seed):
    """A gold eye with a vertical slit."""
    img = new()
    radial(img, rgba('fff0a0'), rgba('d08a10'), 1.2)
    d = ImageDraw.Draw(img)
    d.ellipse([13, 3, 18, 28], fill=rgba('0a0806'))
    d.point((12, 8), fill=rgba('ffffff'))
    return img


def m_wave_water(seed):
    """A wave of silver-teal water lit by starlight: see-through, brighter streaks along it."""
    img = new()
    big = value_noise(seed, 4)
    for y in range(CELL):
        for x in range(CELL):
            t = (y + 0.5) / CELL
            streak = 0.5 + 0.5 * math.sin(x * 0.6 + big(x, y) * 4)
            c = mix(rgba('a8f0e8', 210), rgba('1e5a64', 170), t)
            c = mix(c, rgba('ffffff', 230), streak * 0.25 * (1 - t))
            img.putpixel((x, y), c)
    return img


def m_foam(seed):
    """White foam with starry spray."""
    img = new()
    noise_fill(img, rgba('c8f4f0', 180), rgba('ffffff', 250), seed, 3, 0.4)
    stars(img, seed, 6, [rgba('ffffff')], 0)
    return img


# =====================================================================================================
# Light given a body: the Radiant Angel
# =====================================================================================================

def m_angel_light(seed):
    """The angel's body: white-gold light, see-through at the edges, brighter down the middle."""
    img = new()
    vertical_glow(img, rgba('ffd890', 150), rgba('fffcf0', 245), 0.3)
    stars(img, seed, 6, [rgba('ffffff')], 0)
    return img


def m_angel_wing(seed):
    """One wing of light (shows its whole cell): a long feather-fan, root at the left, soft at the tips."""
    img = new()
    d = ImageDraw.Draw(img)
    px = img.load()
    for y in range(CELL):
        for x in range(CELL):
            u, v = (x + 0.5) / CELL, (y + 0.5) / CELL
            edge = 0.95 - 0.45 * u                                        # narrower toward the tip
            if v > edge or v < 0.1 + 0.25 * u:
                continue
            a = (1 - u * 0.6) * (1 - max(0.0, v - edge + 0.25) * 2.5)
            c = mix(rgba('fffcf0'), rgba('ffd890'), u)
            px[x, y] = c[:3] + (int(230 * max(0.0, min(1.0, a))),)
    for k in range(6):                                                    # the feathers' shafts
        y0 = 0.3 + k * 0.1
        d.line([(1, y0 * CELL), (CELL - 2, (y0 - 0.15 + k * 0.02) * CELL)], fill=rgba('ffffff', 200))
    return img


def m_angel_wing_flip(seed):
    """The same wing turned left-right: root at the right of the cell (for the faces that see it mirrored)."""
    return m_angel_wing(seed).transpose(Image.FLIP_LEFT_RIGHT)


def m_halo(seed):
    """A halo (shows its whole cell): a ring of gold light on clear."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            r = math.hypot((x + 0.5) / CELL - 0.5, (y + 0.5) / CELL - 0.5) * 2
            a = max(0.0, 1 - abs(r - 0.8) / 0.14)
            img.putpixel((x, y), (255, 236, 170, int(255 * a)))
    return img


def m_gold(seed):
    """Light gold, for the angel's eyes and the edges of its robe."""
    img = new()
    noise_fill(img, rgba('ffc860'), rgba('fff6d0'), seed, 3, 0.2)
    return img


# =====================================================================================================
# The Star Dragon: a body of transparent starlight
# =====================================================================================================

def m_dragon_body(seed):
    """Transparent starlight: deep violet-indigo you can see through, stars inside it, a bright skin."""
    img = new()
    noise_fill(img, rgba('2a1e8a', 120), rgba('6a5cff', 170), seed, 5, 0.15)
    stars(img, seed, 12, [rgba('ffffff'), rgba('dcd6ff'), rgba('fff0c0')], 4)
    rim(img, rgba('c8c0ff', 210), 2)
    return img


def m_dragon_scales(seed):
    """The dragon's back: starlight scales picked out in white."""
    img = new()
    noise_fill(img, rgba('3a2ca0', 150), rgba('7a6cff', 190), seed, 4, 0.15)
    d = ImageDraw.Draw(img)
    for row in range(0, CELL + 6, 6):
        off = 3 if (row // 6) % 2 else 0
        for col in range(-6, CELL + 6, 6):
            x, y = col + off, row
            d.arc([x - 3, y - 4, x + 3, y + 2], 20, 160, fill=rgba('e8e4ff', 220))
    stars(img, seed + 9, 5, [rgba('ffffff')], 0)
    return img


def m_dragon_bright(seed):
    """Horns, claws and the spine ridge: near-white starlight."""
    img = new()
    vertical_glow(img, rgba('b8b0ff', 200), rgba('ffffff', 255), 0.35)
    return img


def m_dragon_wing(seed):
    """A wing membrane of thin starlight (shows its whole cell): see-through, a constellation drawn in it, a bright
    leading edge at the top."""
    img = new()
    px = img.load()
    for y in range(CELL):
        for x in range(CELL):
            v = (y + 0.5) / CELL
            px[x, y] = mix(rgba('8a7cff', 150), rgba('2a1e8a', 70), v)
    d = ImageDraw.Draw(img)
    d.line([(0, 0), (CELL, 0)], fill=rgba('ffffff'), width=2)
    rnd = random.Random(seed)
    pts = [(rnd.uniform(3, 29), rnd.uniform(5, 26)) for _ in range(5)]
    pts.sort()
    d.line(pts, fill=rgba('e8e4ff', 160))
    for p in pts:
        d.ellipse([p[0] - 1, p[1] - 1, p[0] + 1, p[1] + 1], fill=rgba('ffffff'))
    for k in range(4):                                                    # the finger bones
        d.line([(k * 9, 0), (k * 9 + 4, CELL)], fill=rgba('c8c0ff', 160))
    return img


def m_dragon_eye(seed):
    """A burning white-gold eye."""
    img = new()
    radial(img, rgba('ffffff'), rgba('ffc040'), 1.0)
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
