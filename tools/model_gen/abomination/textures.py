"""Procedural material cells for the Abomination (Chained pathway) model atlases.

Same layout as tools/model_gen/hand_of_god/textures.py (and its helpers are borrowed from there): every model
texture is an atlas of 4-wide rows of cells, 32x32 texture pixels each (one cell = 16 model pixels at 2 texture
pixels per model pixel). A cube face samples the part of its material's cell that matches its size; a face listed
in a cube's 'full' shows its whole cell, stretched (a face, a lock, a carved lid).

The palette: eminence purple for the Pathway, dark crimson for the Prisoner's blood chains, black iron for the
shackles, dark grey-green fur for the Werewolf, grey-green rotting flesh for the Zombie, pale see-through blue for
the Wraith, and for Chained: black thorns, yellow oily bandages, bright red roses and a dark wooden coffin.
"""
import importlib.util
import math
import os
import random

from PIL import Image, ImageDraw

_here = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location('hog_textures', os.path.join(_here, '..', 'hand_of_god', 'textures.py'))
_hog = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_hog)

CELL, GRID, SIZE = _hog.CELL, _hog.GRID, _hog.SIZE
rgba, mix, value_noise, noise_fill, cracks, edge, new = (_hog.rgba, _hog.mix, _hog.value_noise, _hog.noise_fill,
                                                         _hog.cracks, _hog.edge, _hog.new)


def speckle(img, color, seed, count):
    rnd = random.Random(seed)
    for _ in range(count):
        img.putpixel((rnd.randrange(CELL), rnd.randrange(CELL)), color)


def streaks(img, dark, seed, count, vertical=True):
    """Thin uneven lines: grain of wood, strands of fur, runs of blood."""
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(count):
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        length = rnd.randrange(4, 12)
        if vertical:
            d.line([(x, y), (x + rnd.choice((-1, 0, 1)), y + length)], fill=dark)
        else:
            d.line([(x, y), (x + length, y + rnd.choice((-1, 0, 1)))], fill=dark)


def sheen(img, light, width=0.18):
    """A soft highlight running down the left third: something round and polished."""
    for y in range(CELL):
        for x in range(CELL):
            t = math.exp(-(((x + 0.5) / CELL - 0.3) / width) ** 2)
            if t > 0.05:
                c = img.getpixel((x, y))
                img.putpixel((x, y), mix(c, light, t * 0.6))


# =====================================================================================================
# Prisoner: chains of blood and of black iron
# =====================================================================================================

def m_blood_link(seed):
    """Congealed blood made into metal: near-black crimson with wet red highlights."""
    img = new()
    noise_fill(img, rgba('3a0408'), rgba('8a1018'), seed, 4, 0.2)
    sheen(img, rgba('e0404a'))
    speckle(img, rgba('ff7070'), seed, 6)
    return img


def m_blood_drip(seed):
    """Fresh blood running off the links: brighter, with darker runs."""
    img = new()
    noise_fill(img, rgba('6a0810', 235), rgba('c01820', 245), seed, 3, 0.25)
    streaks(img, rgba('3a0206', 240), seed, 10)
    return img


def m_iron_link(seed):
    """Black iron with a faint eminence-purple sheen."""
    img = new()
    noise_fill(img, rgba('121016'), rgba('3a3442'), seed, 4, 0.25)
    sheen(img, rgba('8a5aa8'))
    speckle(img, rgba('5a3a2a'), seed, 8)                                    # rust
    return img


def m_iron_dark(seed):
    img = new()
    noise_fill(img, rgba('08070a'), rgba('24202a'), seed, 3, 0.3)
    cracks(img, rgba('3a2a20'), seed, 2)
    return img


def m_chain_band(seed):
    """A band of chain seen from the side: links drawn on see-through, for chains wound round a body."""
    img = new()
    d = ImageDraw.Draw(img)
    for i in range(-1, 5):
        x = i * 8 + 2
        if i % 2 == 0:
            d.rounded_rectangle([x, 9, x + 11, 22], radius=5, outline=rgba('2a2430'), width=3)
            d.line([(x + 2, 11), (x + 2, 19)], fill=rgba('9a7ab8'))
        else:
            d.rectangle([x, 14, x + 11, 17], fill=rgba('1a161e'))
            d.line([(x + 1, 14), (x + 10, 14)], fill=rgba('6a5a7a'))
    return img


def m_padlock(seed):
    """The face of a heavy padlock: dark iron, a keyhole, rivets."""
    img = new()
    noise_fill(img, rgba('1a1620'), rgba('3a3242'), seed, 4, 0.2)
    d = ImageDraw.Draw(img)
    d.rectangle([1, 1, 30, 30], outline=rgba('5a4a6a'), width=2)
    d.ellipse([12, 8, 19, 15], fill=rgba('050307'))
    d.polygon([(13, 13), (18, 13), (19, 23), (12, 23)], fill=rgba('050307'))
    for x, y in ((4, 4), (27, 4), (4, 27), (27, 27)):
        d.point((x, y), fill=rgba('a08ab8'))
    return img


def m_shackle_rune(seed):
    """The cuff's outside: iron with a glowing purple rune band."""
    img = m_iron_link(seed)
    d = ImageDraw.Draw(img)
    for x in range(2, CELL, 8):
        d.line([(x, 12), (x + 3, 19), (x + 6, 12)], fill=rgba('b070ff'))
    return img


# =====================================================================================================
# Werewolf
# =====================================================================================================

def m_fur(seed):
    img = new()
    noise_fill(img, rgba('1c2420'), rgba('46524a'), seed, 3, 0.35)
    streaks(img, rgba('101612'), seed, 24)
    streaks(img, rgba('6a7a6e'), seed + 1, 8)
    return img


def m_fur_dark(seed):
    img = new()
    noise_fill(img, rgba('0c100e'), rgba('2a322c'), seed, 3, 0.35)
    streaks(img, rgba('050706'), seed, 20)
    return img


def m_fur_light(seed):
    """The chest and belly: lighter, rougher."""
    img = new()
    noise_fill(img, rgba('4a564c'), rgba('8a9688'), seed, 3, 0.35)
    streaks(img, rgba('3a443c'), seed, 18)
    return img


def m_wolf_skin(seed):
    """The nose, the pads, the lips: wet black."""
    img = new()
    noise_fill(img, rgba('0a0a0c'), rgba('2a2830'), seed, 5, 0.15)
    sheen(img, rgba('6a6a7a'), 0.12)
    return img


def m_claw(seed):
    """Bone-yellow at the root to black at the point (the point is the cell's top)."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            img.putpixel((x, y), mix(rgba('0c0a08'), rgba('d8cca0'), y / CELL))
    return img


def m_wolf_eye(seed):
    """A glowing yellow-green eye with a slit."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            dd = math.hypot(x - 15.5, y - 15.5) / 16
            img.putpixel((x, y), mix(rgba('f8ff9a'), rgba('6ab020'), dd))
    ImageDraw.Draw(img).rectangle([14, 4, 17, 27], fill=rgba('0a0a04'))
    return img


def m_teeth(seed):
    img = new()
    noise_fill(img, rgba('c8bc98'), rgba('f4eed8'), seed, 4, 0.2)
    d = ImageDraw.Draw(img)
    for x in range(0, CELL, 6):
        d.line([(x, 0), (x, CELL)], fill=rgba('7a6a4a'))
    return img


def m_wolf_mouth(seed):
    """Inside the jaws: dark red."""
    img = new()
    noise_fill(img, rgba('2a0608'), rgba('6a1418'), seed, 4, 0.2)
    return img


# =====================================================================================================
# Zombie: rotting flesh and bone
# =====================================================================================================

def m_rot_skin(seed):
    img = new()
    noise_fill(img, rgba('3e4a34'), rgba('7a8a62'), seed, 3, 0.35)
    speckle(img, rgba('2a1e18'), seed, 20)
    cracks(img, rgba('4a2a24'), seed, 2)
    return img


def m_rot_dark(seed):
    """Torn flesh where the skin is gone."""
    img = new()
    noise_fill(img, rgba('3a1414'), rgba('6a2a24'), seed, 3, 0.35)
    streaks(img, rgba('8a4a3a'), seed, 10)
    return img


def m_bone(seed):
    img = new()
    noise_fill(img, rgba('b0a888'), rgba('e8e2c8'), seed, 4, 0.2)
    cracks(img, rgba('8a8068'), seed, 1)
    return img


def m_black_nail(seed):
    img = new()
    noise_fill(img, rgba('0a0806'), rgba('2a241e'), seed, 4, 0.2)
    return img


def m_grave_dirt(seed):
    img = new()
    noise_fill(img, rgba('241a12'), rgba('4a3624'), seed, 2, 0.45)
    speckle(img, rgba('6a6a62'), seed, 10)
    return img


# =====================================================================================================
# Wraith: a ghost, see-through
# =====================================================================================================

def m_ghost_body(seed):
    img = new()
    noise_fill(img, rgba('9ab8d8', 150), rgba('e0f0ff', 190), seed, 4, 0.15)
    return img


def m_ghost_tail(seed):
    """Mist fading toward the cell's bottom (the end of the tail)."""
    img = new()
    rnd = random.Random(seed)
    for y in range(CELL):
        for x in range(CELL):
            a = int(170 * (1 - y / CELL) ** 1.2 * (0.8 + 0.2 * rnd.random()))
            img.putpixel((x, y), (190, 214, 240, max(0, a)))
    return img


def m_ghost_hair(seed):
    img = new()
    noise_fill(img, rgba('c8d8f0', 140), rgba('ffffff', 200), seed, 2, 0.3)
    streaks(img, rgba('7a90b8', 170), seed, 20)
    return img


def m_ghost_face(seed):
    """A pale face: two hollow black eyes with a cold light deep in them, and a long open mouth."""
    img = m_ghost_body(seed)
    d = ImageDraw.Draw(img)
    for cx in (9, 22):
        d.ellipse([cx - 4, 8, cx + 4, 17], fill=(6, 8, 20, 245))
        d.point((cx, 12), fill=rgba('a0f0ff'))
    d.ellipse([12, 20, 19, 30], fill=(6, 8, 20, 240))
    return img


# =====================================================================================================
# Chained: coffin, bandages, thorns, roses, the doll
# =====================================================================================================

def m_coffin_wood(seed):
    """Dark, old planks with their grain running lengthwise."""
    img = new()
    noise_fill(img, rgba('1a100c'), rgba('3e2618'), seed, 4, 0.2)
    streaks(img, rgba('120a06'), seed, 26)
    d = ImageDraw.Draw(img)
    for x in (10, 21):
        d.line([(x, 0), (x, CELL)], fill=rgba('0a0604'))
    return img


def m_coffin_inside(seed):
    """Inside: black velvet gone purple, a faint glow."""
    img = new()
    noise_fill(img, rgba('0a0610'), rgba('2a1636'), seed, 5, 0.25)
    return img


def m_coffin_trim(seed):
    img = new()
    noise_fill(img, rgba('2a2430'), rgba('6a5a72'), seed, 3, 0.3)
    edge(img, rgba('120e16'))
    return img


def m_coffin_lid(seed):
    """The lid's face: planks carved with a cross of thorns round a rose, framed in iron."""
    img = m_coffin_wood(seed)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, CELL - 1, CELL - 1], outline=rgba('3a3242'), width=2)
    d.line([(15, 3), (15, 28)], fill=rgba('060404'), width=2)
    d.line([(7, 11), (24, 11)], fill=rgba('060404'), width=2)
    for y in range(4, 28, 3):
        d.point((13, y), fill=rgba('060404'))
        d.point((18, y + 1), fill=rgba('060404'))
    d.ellipse([12, 8, 19, 14], fill=rgba('c01428'))
    d.point((15, 10), fill=rgba('ff5a6a'))
    return img


def m_bandage(seed):
    """Yellowed linen strips soaked in oil: overlapping bands with darker edges and stains."""
    img = new()
    noise_fill(img, rgba('a08840'), rgba('e0cc80'), seed, 3, 0.25)
    d = ImageDraw.Draw(img)
    rnd = random.Random(seed)
    y = 0
    while y < CELL:
        h = rnd.randrange(4, 7)
        d.line([(0, y), (CELL, y + rnd.choice((-1, 0, 1)))], fill=rgba('6a5420'))
        y += h
    over = new()
    od = ImageDraw.Draw(over)
    for _ in range(4):
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        od.ellipse([x - 2, y - 2, x + 3, y + 2], fill=rgba('5a4410', 150))
    img.alpha_composite(over)
    return img


def m_bandage_dark(seed):
    """Old bandage, darker with oil and dirt (the trailing ends)."""
    img = new()
    noise_fill(img, rgba('5a4a1e'), rgba('9a8440'), seed, 3, 0.3)
    streaks(img, rgba('3a2e10'), seed, 14, vertical=False)
    return img


def m_thorn_vine(seed):
    """A black thorny vine: near-black with a purple sheen and small dark spurs."""
    img = new()
    noise_fill(img, rgba('0a060c'), rgba('2a1a30'), seed, 3, 0.3)
    sheen(img, rgba('6a3a82'), 0.15)
    speckle(img, rgba('000000'), seed, 14)
    return img


def m_thorn(seed):
    """A thorn: black at the root to a pale point (the point is the cell's top)."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            img.putpixel((x, y), mix(rgba('a090a8'), rgba('080408'), y / CELL * 1.6))
    return img


def m_rose_petal(seed):
    """Bright, sharp red: the roses of Chained."""
    img = new()
    noise_fill(img, rgba('9a0414'), rgba('ff2a40'), seed, 3, 0.2)
    d = ImageDraw.Draw(img)
    for i in range(4):
        d.arc([2 + i * 3, 2 + i * 3, 29 - i * 3, 29 - i * 3], 200, 340, fill=rgba('5a0008'))
    return img


def m_rose_dark(seed):
    img = new()
    noise_fill(img, rgba('4a0008'), rgba('8a0a18'), seed, 3, 0.2)
    return img


def m_leaf(seed):
    img = new()
    noise_fill(img, rgba('0e2a14'), rgba('2a5a2a'), seed, 4, 0.2)
    ImageDraw.Draw(img).line([(0, 16), (CELL, 16)], fill=rgba('5a8a4a'))
    return img


def m_doll_cloth(seed):
    """Sackcloth, coarsely woven, stained (the weave and stains are blended over it, so it stays opaque)."""
    img = new()
    noise_fill(img, rgba('4a3a2c'), rgba('86705a'), seed, 3, 0.4)
    over = new()
    d = ImageDraw.Draw(over)
    for i in range(0, CELL, 2):
        d.line([(i, 0), (i, CELL)], fill=rgba('2a1e14', 70))
        d.line([(0, i + 1), (CELL, i + 1)], fill=rgba('2a1e14', 45))
    rnd = random.Random(seed)
    for _ in range(3):
        x, y = rnd.randrange(CELL), rnd.randrange(CELL)
        d.ellipse([x - 3, y - 2, x + 3, y + 3], fill=rgba('2a1a10', 150))
    img.alpha_composite(over)
    return img


def m_doll_patch(seed):
    """A sewn-on patch of faded purple, its stitches showing."""
    img = new()
    noise_fill(img, rgba('3a2048'), rgba('6c3082'), seed, 4, 0.3)
    d = ImageDraw.Draw(img)
    for i in range(1, CELL - 1, 4):
        for (x, y) in ((i, 1), (i, CELL - 2), (1, i), (CELL - 2, i)):
            d.line([(x - 1, y - 1), (x + 1, y + 1)], fill=rgba('d8cca0'))
    return img


def m_doll_face(seed):
    """The doll's face: a black button eye, an eye sewn shut with an X, a long mouth stitched closed, a tear."""
    img = m_doll_cloth(seed)
    d = ImageDraw.Draw(img)
    d.ellipse([3, 5, 14, 16], fill=rgba('060406'))                          # the button
    d.ellipse([5, 7, 12, 14], outline=rgba('4a3e56'))
    d.point((7, 9), fill=rgba('c0b0d0'))
    d.point((10, 9), fill=rgba('c0b0d0'))
    d.line([(18, 6), (28, 16)], fill=rgba('120408'), width=3)                # the X
    d.line([(28, 6), (18, 16)], fill=rgba('120408'), width=3)
    d.line([(3, 23), (29, 22)], fill=rgba('120408'), width=2)                # the mouth
    for x in range(4, 29, 4):
        d.line([(x, 19), (x + 1, 26)], fill=rgba('e8dcb0'), width=1)
    d.line([(8, 17), (8, 22)], fill=rgba('c01020'), width=2)                 # a red tear
    return img


def m_doll_stitch(seed):
    """Seams: dark thread on cloth."""
    img = m_doll_cloth(seed)
    d = ImageDraw.Draw(img)
    for y in range(2, CELL, 5):
        d.line([(12, y), (19, y + 2)], fill=rgba('1a0a10'), width=1)
    return img


def m_curse_glow(seed):
    """The curses inside the vessel: purple light with darker swirls."""
    img = new()
    for y in range(CELL):
        for x in range(CELL):
            a = math.atan2(y - 15.5, x - 15.5)
            r = math.hypot(x - 15.5, y - 15.5)
            t = 0.5 + 0.5 * math.sin(a * 3 + r * 0.5)
            img.putpixel((x, y), mix(rgba('3a0a5a', 220), rgba('d8a0ff', 240), t))
    return img


MATERIALS = {
    'blood_link': m_blood_link, 'blood_drip': m_blood_drip, 'iron_link': m_iron_link, 'iron_dark': m_iron_dark,
    'chain_band': m_chain_band, 'padlock': m_padlock, 'shackle_rune': m_shackle_rune,
    'fur': m_fur, 'fur_dark': m_fur_dark, 'fur_light': m_fur_light, 'wolf_skin': m_wolf_skin, 'claw': m_claw,
    'wolf_eye': m_wolf_eye, 'teeth': m_teeth, 'wolf_mouth': m_wolf_mouth,
    'rot_skin': m_rot_skin, 'rot_dark': m_rot_dark, 'bone': m_bone, 'black_nail': m_black_nail,
    'grave_dirt': m_grave_dirt,
    'ghost_body': m_ghost_body, 'ghost_tail': m_ghost_tail, 'ghost_hair': m_ghost_hair, 'ghost_face': m_ghost_face,
    'coffin_wood': m_coffin_wood, 'coffin_inside': m_coffin_inside, 'coffin_trim': m_coffin_trim,
    'coffin_lid': m_coffin_lid, 'bandage': m_bandage, 'bandage_dark': m_bandage_dark,
    'thorn_vine': m_thorn_vine, 'thorn': m_thorn, 'rose_petal': m_rose_petal, 'rose_dark': m_rose_dark,
    'leaf': m_leaf, 'doll_cloth': m_doll_cloth, 'doll_patch': m_doll_patch, 'doll_face': m_doll_face,
    'doll_stitch': m_doll_stitch, 'curse_glow': m_curse_glow,
}


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
