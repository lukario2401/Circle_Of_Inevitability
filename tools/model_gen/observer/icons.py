"""Screen pictures for the Observer (Sublunary Eye pathway).

python3 tools/model_gen/observer/icons.py src/main/resources/assets/circle_of_inevitability [preview.png]

  textures/gui/ability_menus/observer/*.png   32x32, one per wheel entry (39), in a round frame like an iris with
                                              a painted rim; each of the seven wheels has its own inside colour
  textures/gui/observer/*.png                 16x16, the statuses shown above the hotbar (ObserverFx.Status)
  textures/gui/pathway_icons/observer.png     16x16, the Pathway's icon beside the hotbar

Every picture is drawn in flat colors and then outlined automatically, so the shapes stay simple to change.
Replace any of them with a hand-drawn PNG of the same name. To add one: a function under @w(WHEEL_NAME) or i_<name>,
and its name in WHEEL or STATUS (the order there is the order in the preview).
"""
import math
import os
import sys

from PIL import Image, ImageDraw

# ------------------------------------------------------------------ colors
OUTLINE = (12, 10, 16, 255)
INK = (20, 16, 14, 255)
PAPER = (236, 226, 200, 255)
PAPER_DIM = (190, 176, 146, 255)
BROWN = (122, 80, 46, 255)
BROWN_DARK = (74, 46, 26, 255)
MOSS = (106, 154, 58, 255)
MOSS_DARK = (58, 90, 34, 255)
TEAL = (140, 216, 208, 255)
TEAL_LIGHT = (220, 255, 250, 255)
VIOLET = (108, 92, 231, 255)
VIOLET_LIGHT = (190, 180, 255, 255)
VIOLET_DARK = (52, 40, 120, 255)
SHADOW = (26, 16, 44, 255)
GOLD = (255, 214, 110, 255)
GOLD_LIGHT = (255, 246, 214, 255)
GOLD_DEEP = (214, 150, 40, 255)
RED = (214, 60, 60, 255)
RED_DARK = (120, 24, 24, 255)
BLUE = (90, 122, 154, 255)
SKY = (150, 190, 220, 255)
GREEN = (90, 180, 90, 255)
GREY = (150, 150, 160, 255)
WHITE = (255, 255, 255, 255)
HUES = [(255, 42, 106, 255), (255, 210, 26, 255), (26, 224, 255, 255), (122, 42, 255, 255), (42, 255, 106, 255),
        (255, 122, 26, 255)]


def outlined(layer, color=OUTLINE):
    """Every empty pixel next to a filled one becomes the outline."""
    w, h = layer.size
    src = layer.load()
    out = layer.copy()
    dst = out.load()
    for y in range(h):
        for x in range(w):
            if src[x, y][3] > 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3] > 0 and src[nx, ny] != color:
                    dst[x, y] = color
                    break
    return out


def canvas(size):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


# ------------------------------------------------------------------ shapes shared by the icons

def eye(d, cx, cy, hw, hh, white=PAPER, iris_c=VIOLET, pupil=INK, open_=1.0):
    """An almond eye, hw/hh its half width and height; open_ 0 draws it shut (a lid line with lashes)."""
    if open_ < 0.3:
        d.arc([cx - hw, cy - hh, cx + hw, cy + hh], 20, 160, fill=white, width=2)
        for k in (-0.5, 0.0, 0.5):
            x = cx + k * hw
            d.line([(x, cy + hh * 0.9), (x + k * 2, cy + hh * 0.9 + 2)], fill=white)
        return
    hh = max(0.5, hh * open_)
    top = [(cx - hw + i * hw / 4, cy - math.sin(i / 8 * math.pi) * hh) for i in range(9)]
    low = [(cx + hw - i * hw / 4, cy + math.sin(i / 8 * math.pi) * hh) for i in range(9)]
    d.polygon(top + low, fill=white)
    if open_ > 0.3:
        r = min(hh, hw * 0.45)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=iris_c)
        if pupil:
            p = max(0.5, r * 0.45)
            d.ellipse([cx - p, cy - p, cx + p, cy + p], fill=pupil)


def frame(d, x0, y0, x1, y1, inside, edge=INK, gilt=GOLD):
    """A framed canvas."""
    d.rectangle([x0, y0, x1, y1], fill=edge)
    d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], fill=inside)
    for x, y in ((x0, y0), (x1, y0), (x0, y1), (x1, y1)):
        d.point((x, y), fill=gilt)


def figure(d, cx, top, s, fill):
    """A standing person: a head and a body."""
    d.ellipse([cx - s * 0.5, top, cx + s * 0.5, top + s], fill=fill)
    d.polygon([(cx - s * 0.6, top + s * 1.1), (cx + s * 0.6, top + s * 1.1), (cx + s * 0.9, top + s * 3.4),
               (cx - s * 0.9, top + s * 3.4)], fill=fill)


def flower(d, cx, cy, r, petal=GOLD_LIGHT, heart=GOLD_DEEP, petals=6):
    for k in range(petals):
        a = math.tau * k / petals
        x, y = cx + math.cos(a) * r * 0.6, cy + math.sin(a) * r * 0.6
        d.ellipse([x - r * 0.45, y - r * 0.45, x + r * 0.45, y + r * 0.45], fill=petal)
    d.ellipse([cx - r * 0.35, cy - r * 0.35, cx + r * 0.35, cy + r * 0.35], fill=heart)


def note(d, x, y, fill=GOLD_LIGHT, stem=6):
    d.ellipse([x - 2, y - 1.5, x + 2, y + 1.5], fill=fill)
    d.line([(x + 2, y), (x + 2, y - stem)], fill=fill)


def bolt(d, pts, fill=GOLD, core=GOLD_LIGHT):
    d.line(pts, fill=fill, width=3)
    d.line(pts, fill=core, width=1)


# ------------------------------------------------------------------ the 32x32 wheel pictures

S = 32
C = 15.5

# the inside of each wheel's frame: (center, edge)
TERRITORY = dict(inside=(30, 24, 14, 255), edge=(76, 60, 30, 255))
SIGHT = dict(inside=(16, 14, 34, 255), edge=(54, 46, 110, 255))
CANVAS = dict(inside=(34, 28, 22, 255), edge=(110, 92, 60, 255))
FICTION = dict(inside=(28, 24, 30, 255), edge=(90, 80, 96, 255))
DIMENSION = dict(inside=(10, 8, 22, 255), edge=(60, 44, 130, 255))
STRINGS = dict(inside=(30, 22, 12, 255), edge=(120, 96, 40, 255))
SUBLUNARY = dict(inside=(40, 32, 14, 255), edge=(150, 120, 50, 255))


def wheel_base(inside, edge):
    """A round frame like an iris: a cream painted rim with ink ticks, round a field of the wheel's colour."""
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = im.load()
    for y in range(S):
        for x in range(S):
            dd = math.hypot(x - C, y - C)
            a = math.atan2(y - C, x - C)
            if dd <= 15.2:
                if dd > 13.2:
                    tick = (int((a + math.pi) / math.tau * 24) % 2) == 0
                    light = (x - C) * -0.7 + (y - C) * -0.7
                    px[x, y] = PAPER if light > 5 else (PAPER_DIM if tick else (150, 130, 96, 255))
                elif dd > 12.4:
                    px[x, y] = INK
                else:
                    t = dd / 12.4
                    px[x, y] = tuple(int(inside[i] * (1 - t) + edge[i] * t) for i in range(3)) + (255,)
    return im


def in_frame(base, layer):
    layer = outlined(layer)
    lp = layer.load()
    bp = base.load()
    for y in range(S):
        for x in range(S):
            if lp[x, y][3] and math.hypot(x - C, y - C) <= 12.4:
                bp[x, y] = lp[x, y]
    return base


def w(inside):
    def deco(fn):
        def make():
            base = wheel_base(**inside)
            layer, d = canvas(S)
            fn(d)
            return in_frame(base, layer)
        return make
    return deco


# ---- key 1: Territory

@w(TERRITORY)
def totem_territory(d):
    d.ellipse([5, 22, 26, 28], fill=MOSS_DARK)
    d.rectangle([13, 7, 18, 25], fill=BROWN)
    d.rectangle([7, 10, 24, 12], fill=BROWN_DARK)
    eye(d, 15.5, 16, 3, 2, PAPER, MOSS)
    d.rectangle([13, 20, 18, 22], fill=BROWN_DARK)
    d.polygon([(14, 7), (15.5, 3), (17, 7)], fill=PAPER)


@w(TERRITORY)
def borrowed_power(d):
    d.polygon([(4, 24), (12, 14), (17, 21), (22, 12), (28, 24)], fill=BROWN)      # earth
    d.arc([5, 4, 17, 16], 200, 340, fill=SKY, width=2)                           # wind
    d.ellipse([20, 5, 25, 10], fill=TEAL)                                         # water
    d.polygon([(8, 24), (10, 19), (12, 24)], fill=MOSS)                          # leaf
    d.line([(15.5, 28), (15.5, 19)], fill=GOLD, width=2)                         # power flowing up
    d.polygon([(13, 19), (15.5, 15), (18, 19)], fill=GOLD)


@w(TERRITORY)
def ink_servants(d):
    for x, y in ((9, 10), (22, 10), (15.5, 20)):
        d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=(40, 40, 52, 255))
        d.polygon([(x - 2, y + 2), (x + 2, y + 2), (x, y + 6)], fill=(40, 40, 52, 255))
        d.point((x - 1, y - 1), fill=TEAL_LIGHT)
        d.point((x + 1, y - 1), fill=TEAL_LIGHT)


@w(TERRITORY)
def painting_world(d):
    frame(d, 5, 7, 26, 24, (8, 6, 14, 255))
    d.polygon([(6, 20), (12, 17), (19, 19), (25, 16), (25, 23), (6, 23)], fill=PAPER)
    for x, y in ((9, 10), (17, 12), (22, 9), (13, 14)):
        d.point((x, y), fill=VIOLET_LIGHT)


# ---- key 2: Sight

@w(SIGHT)
def true_sight(d):
    eye(d, 15.5, 15.5, 11, 6, PAPER, VIOLET)
    d.line([(4, 6), (8, 9)], fill=VIOLET_LIGHT)
    d.line([(27, 6), (23, 9)], fill=VIOLET_LIGHT)
    d.line([(15.5, 2), (15.5, 7)], fill=VIOLET_LIGHT)


@w(SIGHT)
def many_angles(d):
    for x, y in ((9, 9), (22, 9), (9, 22), (22, 22)):
        eye(d, x, y, 5, 3, PAPER, TEAL)
    d.line([(9, 9), (22, 22)], fill=RED)
    d.line([(22, 9), (9, 22)], fill=VIOLET_LIGHT)


@w(SIGHT)
def read_intent(d):
    d.polygon([(8, 5), (23, 6), (24, 27), (7, 26)], fill=PAPER)
    for y in (13, 16, 19, 22):
        d.line([(10, y), (21, y)], fill=INK)
    eye(d, 15.5, 9, 5, 2.5, PAPER, VIOLET)


# ---- key 3: Canvas

@w(CANVAS)
def riot_of_colours(d):
    frame(d, 4, 7, 27, 24, (30, 10, 40, 255))
    for k, col in enumerate(HUES):
        a0 = math.tau * k / 6
        pts = [(15.5 + math.cos(a0 + t * 0.5) * t * 1.1, 15.5 + math.sin(a0 + t * 0.5) * t * 0.75) for t in range(9)]
        d.line(pts, fill=col)


@w(CANVAS)
def itching_house(d):
    frame(d, 4, 7, 27, 24, (110, 108, 84, 255))
    d.polygon([(10, 22), (21, 22), (15.5, 10)], fill=BLUE)
    d.rectangle([15, 18, 16, 22], fill=INK)
    for x in (7, 24):
        d.line([(x, 22), (x + 1, 11)], fill=BROWN_DARK)
    for x in range(6, 26, 3):
        d.line([(x, 9), (x - 1, 12)], fill=BROWN)


@w(CANVAS)
def golden_sun(d):
    frame(d, 4, 7, 27, 24, (40, 26, 74, 255))
    for i, col in enumerate(HUES):
        r = 7.5 - i * 0.7
        d.ellipse([15.5 - r, 15.5 - r, 15.5 + r, 15.5 + r], outline=col)
    d.ellipse([12, 12, 19, 19], fill=GOLD)
    d.ellipse([14, 14, 17, 17], fill=GOLD_LIGHT)


@w(CANVAS)
def boat_in_a_storm(d):
    frame(d, 4, 7, 27, 24, (30, 38, 52, 255))
    d.polygon([(5, 19), (10, 16), (15, 19), (20, 15), (26, 18), (26, 23), (5, 23)], fill=(40, 100, 120, 255))
    d.polygon([(12, 16), (18, 16), (17, 18), (13, 18)], fill=BROWN)
    d.line([(15, 16), (15, 10)], fill=BROWN_DARK)
    d.polygon([(15, 10), (19, 15), (15, 15)], fill=PAPER)
    bolt(d, [(23, 8), (21, 11), (23, 12)], GOLD, GOLD_LIGHT)


@w(CANVAS)
def self_portrait(d):
    frame(d, 8, 4, 23, 27, (58, 52, 72, 255))
    d.ellipse([12, 8, 19, 16], fill=PAPER)
    d.chord([11, 7, 20, 18], 180, 360, fill=INK)
    d.polygon([(10, 26), (21, 26), (19, 17), (12, 17)], fill=INK)
    d.point((14, 12), fill=VIOLET)


@w(CANVAS)
def giants_fist(d):
    d.rectangle([9, 13, 22, 24], fill=(214, 160, 110, 255))
    for x in (9, 12.5, 16, 19.5):
        d.rectangle([x, 22, x + 3, 26], fill=(232, 186, 140, 255))
    d.rectangle([12, 4, 20, 13], fill=(196, 140, 96, 255))
    d.line([(4, 28), (27, 28)], fill=PAPER_DIM, width=2)
    for x in (6, 25):
        d.line([(x, 26), (x - 2, 24)], fill=PAPER)


# ---- key 4: Fiction

@w(FICTION)
def painted_mark(d):
    figure(d, 15.5, 5, 4, (90, 90, 100, 255))
    d.ellipse([6, 22, 25, 28], outline=VIOLET, width=2)
    d.line([(22, 4), (17, 13)], fill=BROWN, width=2)
    d.line([(17, 13), (16, 15)], fill=VIOLET, width=2)


@w(FICTION)
def hide_in_painting(d):
    frame(d, 7, 4, 24, 27, SKY)
    d.rectangle([8, 20, 23, 26], fill=MOSS)
    d.rectangle([12, 10, 19, 22], fill=PAPER_DIM)
    d.rectangle([14, 13, 17, 22], fill=INK)
    d.point((15, 14), fill=VIOLET)


@w(FICTION)
def painted_face(d):
    d.ellipse([8, 5, 23, 26], fill=PAPER)
    d.arc([10, 12, 15, 16], 200, 340, fill=INK)
    d.arc([16, 12, 21, 16], 200, 340, fill=INK)
    d.line([(10, 10), (14, 10)], fill=VIOLET)
    d.line([(17, 10), (21, 10)], fill=VIOLET)
    d.arc([12, 18, 19, 23], 20, 160, fill=RED)
    d.line([(24, 3), (19, 9)], fill=BROWN, width=2)


@w(FICTION)
def manifest_knight(d):
    d.rectangle([12, 10, 19, 22], fill=PAPER)
    d.rectangle([12, 5, 19, 10], fill=GREY)
    d.line([(13, 7), (18, 7)], fill=INK)
    d.line([(15.5, 2), (15.5, 5)], fill=RED, width=2)
    d.rectangle([12, 22, 14, 27], fill=PAPER)
    d.rectangle([17, 22, 19, 27], fill=PAPER)
    d.polygon([(6, 11), (11, 11), (11, 18), (8.5, 21), (6, 18)], fill=RED)
    d.line([(22, 22), (26, 4)], fill=GREY, width=2)


@w(FICTION)
def manifest_archer(d):
    d.rectangle([13, 11, 18, 22], fill=(100, 112, 130, 255))
    d.ellipse([13, 5, 18, 11], fill=PAPER)
    d.chord([12, 4, 19, 12], 180, 360, fill=(70, 80, 96, 255))
    d.rectangle([13, 22, 15, 27], fill=PAPER)
    d.rectangle([16, 22, 18, 27], fill=PAPER)
    d.arc([18, 4, 28, 26], 270, 90, fill=BROWN, width=2)
    d.line([(23, 4), (23, 26)], fill=PAPER)
    d.line([(10, 15), (27, 15)], fill=INK)


@w(FICTION)
def fantasy_double(d):
    figure(d, 10, 6, 4, PAPER)
    figure(d, 21, 6, 4, (0, 0, 0, 0))
    d.ellipse([19, 6, 23, 10], outline=VIOLET_LIGHT)
    d.polygon([(18.6, 10.4), (23.4, 10.4), (24.6, 19.6), (17.4, 19.6)], outline=VIOLET_LIGHT)
    d.line([(13, 14), (18, 14)], fill=GOLD)


# ---- key 5: Dimension

@w(DIMENSION)
def phase(d):
    figure(d, 15.5, 5, 5, (190, 230, 226, 120))
    for r in (5, 9, 12):
        d.ellipse([15.5 - r, 25 - r * 0.35, 15.5 + r, 25 + r * 0.35], outline=TEAL)


@w(DIMENSION)
def pixie_wings(d):
    for sign in (-1, 1):
        d.ellipse([15.5 + sign * 1 - (12 if sign < 0 else 0), 6, 15.5 + sign * 1 + (12 if sign > 0 else 0), 13],
                  fill=(200, 250, 245, 200))
        d.ellipse([15.5 + sign * 1 - (9 if sign < 0 else 0), 14, 15.5 + sign * 1 + (9 if sign > 0 else 0), 20],
                  fill=(160, 230, 224, 200))
    d.rectangle([14.5, 6, 16.5, 25], fill=TEAL)


@w(DIMENSION)
def painted_lightning(d):
    for k, x in enumerate((8, 15.5, 23)):
        bolt(d, [(x - 1, 4), (x + 2, 11), (x - 2, 15), (x + 1, 22), (x - 1, 27)], GOLD, GOLD_LIGHT)


@w(DIMENSION)
def dimensional_passage(d):
    d.rectangle([4, 6, 10, 26], fill=(90, 90, 100, 255))
    d.polygon([(13, 5), (19, 5), (18, 16), (20, 27), (12, 27), (14, 16)], fill=SHADOW)
    d.line([(13, 5), (14, 16), (12, 27)], fill=VIOLET_LIGHT)
    d.line([(19, 5), (18, 16), (20, 27)], fill=VIOLET_LIGHT)
    figure(d, 24, 10, 3, VIOLET_LIGHT)


@w(DIMENSION)
def hostel(d):
    for k in range(12):
        a = k * 0.7
        r = 11 - k * 0.8
        d.point((15.5 + math.cos(a) * r, 15.5 + math.sin(a) * r), fill=VIOLET_LIGHT)
    d.ellipse([10, 10, 21, 21], fill=SHADOW)
    d.rectangle([13, 12, 18, 19], outline=GOLD)


@w(DIMENSION)
def shadow_form(d):
    figure(d, 15.5, 4, 6, SHADOW)
    d.line([(15.5, 11), (15.5, 24)], fill=VIOLET)
    d.point((14, 7), fill=TEAL_LIGHT)
    d.point((17, 7), fill=TEAL_LIGHT)
    d.polygon([(22, 6), (25, 10), (23, 11)], fill=VIOLET_LIGHT)
    d.polygon([(7, 9), (9, 13), (6, 12)], fill=VIOLET_LIGHT)


# ---- key 6: Strings

@w(STRINGS)
def taut_chord(d):
    d.ellipse([12, 12, 19, 19], fill=GOLD)
    for x, y in ((5, 6), (26, 7), (6, 25), (25, 24), (15.5, 3)):
        d.line([(15.5, 15.5), (x, y)], fill=GOLD_LIGHT)
        d.ellipse([x - 1.5, y - 1.5, x + 1.5, y + 1.5], fill=RED)


@w(STRINGS)
def resonance(d):
    for r in (4, 7, 10):
        pts = [(15.5 + math.cos(a / 20 * math.tau) * (r + math.sin(a) * 0.8),
                15.5 + math.sin(a / 20 * math.tau) * (r + math.sin(a) * 0.8)) for a in range(21)]
        d.line(pts, fill=GOLD_LIGHT if r != 7 else GOLD)
    d.ellipse([14, 14, 17, 17], fill=WHITE)


@w(STRINGS)
def harmony(d):
    for y in (8, 12, 16, 20, 24):
        d.line([(4, y), (27, y)], fill=PAPER_DIM)
    note(d, 10, 18, GOLD_LIGHT, 8)
    note(d, 16, 14, GOLD_LIGHT, 8)
    note(d, 22, 20, GOLD_LIGHT, 8)
    d.line([(12, 10), (18, 6)], fill=GOLD_LIGHT)


@w(STRINGS)
def dissonance(d):
    for y in (8, 12, 16, 20, 24):
        d.line([(4, y), (13, y + 1)], fill=PAPER_DIM)
        d.line([(18, y - 1), (27, y)], fill=PAPER_DIM)
    note(d, 9, 16, VIOLET_LIGHT, 7)
    d.ellipse([20, 18, 24, 21], fill=VIOLET_LIGHT)
    d.line([(24, 19), (26, 12)], fill=VIOLET_LIGHT)
    d.line([(14, 4), (17, 28)], fill=RED)


@w(STRINGS)
def shadow_from_above(d):
    d.ellipse([6, 22, 25, 28], fill=SHADOW)
    for x, h in ((10, 10), (15.5, 18), (21, 12)):
        d.polygon([(x - 2, 25), (x + 2, 25), (x, 25 - h)], fill=VIOLET_DARK)
        d.line([(x, 25), (x, 25 - h)], fill=VIOLET_LIGHT)


@w(STRINGS)
def fold_space(d):
    d.polygon([(4, 8), (16, 5), (16, 26), (4, 23)], fill=PAPER_DIM)
    d.polygon([(16, 5), (27, 9), (27, 24), (16, 26)], fill=PAPER)
    d.line([(16, 5), (16, 26)], fill=VIOLET, width=2)
    figure(d, 9, 11, 2.5, VIOLET)
    figure(d, 22, 11, 2.5, VIOLET)


# ---- key 7: Sublunary

@w(SUBLUNARY)
def foregone_loss(d):
    d.polygon([(7, 5), (24, 5), (24, 26), (7, 26)], fill=PAPER)
    for y in (9, 12, 15, 18):
        d.line([(9, y), (22, y)], fill=INK)
    d.line([(9, 23), (22, 20)], fill=RED, width=2)
    d.line([(25, 2), (20, 12)], fill=RED_DARK, width=2)


@w(SUBLUNARY)
def foregone_survival(d):
    d.polygon([(7, 5), (24, 5), (24, 26), (7, 26)], fill=PAPER)
    for y in (9, 12, 15, 18):
        d.line([(9, y), (22, y)], fill=INK)
    d.ellipse([12, 19, 19, 26], outline=GOLD_DEEP, width=2)
    d.line([(25, 2), (20, 12)], fill=GOLD_DEEP, width=2)


@w(SUBLUNARY)
def higher_revival(d):
    d.polygon([(15.5, 3), (28, 15.5), (15.5, 28), (3, 15.5)], outline=GOLD_LIGHT)
    d.rectangle([8, 8, 23, 23], outline=GOLD)
    eye(d, 15.5, 15.5, 6, 3, GOLD_LIGHT, GOLD_DEEP)


@w(SUBLUNARY)
def bloom(d):
    eye(d, 15.5, 9, 9, 4, GOLD_LIGHT, GOLD_DEEP)
    flower(d, 9, 22, 6)
    flower(d, 22, 22, 6, PAPER, GOLD)


@w(SUBLUNARY)
def fall_silent(d):
    eye(d, 15.5, 9, 9, 4, GOLD_LIGHT, GOLD_DEEP, open_=0.0)
    d.line([(9, 28), (9, 19)], fill=MOSS_DARK)
    d.polygon([(9, 19), (5, 23), (8, 22)], fill=GREY)
    d.polygon([(9, 19), (13, 23), (10, 22)], fill=GREY)
    d.line([(22, 28), (22, 18)], fill=MOSS_DARK)
    d.polygon([(22, 18), (19, 22), (21, 22)], fill=GREY)


@w(SUBLUNARY)
def erase(d):
    figure(d, 11, 6, 5, (110, 110, 120, 255))
    d.polygon([(14, 4), (26, 4), (26, 28), (14, 28)], fill=(0, 0, 0, 0))
    d.rectangle([17, 14, 27, 20], fill=PAPER)
    d.rectangle([17, 14, 20, 20], fill=RED)
    for y in (8, 12, 24):
        d.line([(15, y), (27, y)], fill=(255, 255, 255, 120))


WHEEL = {
    'totem_territory': totem_territory, 'borrowed_power': borrowed_power, 'ink_servants': ink_servants,
    'painting_world': painting_world,
    'true_sight': true_sight, 'many_angles': many_angles, 'read_intent': read_intent,
    'riot_of_colours': riot_of_colours, 'itching_house': itching_house, 'golden_sun': golden_sun,
    'boat_in_a_storm': boat_in_a_storm, 'self_portrait': self_portrait, 'giants_fist': giants_fist,
    'painted_mark': painted_mark, 'hide_in_painting': hide_in_painting, 'painted_face': painted_face,
    'manifest_knight': manifest_knight, 'manifest_archer': manifest_archer, 'fantasy_double': fantasy_double,
    'phase': phase, 'pixie_wings': pixie_wings, 'painted_lightning': painted_lightning,
    'dimensional_passage': dimensional_passage, 'hostel': hostel, 'shadow_form': shadow_form,
    'taut_chord': taut_chord, 'resonance': resonance, 'harmony': harmony, 'dissonance': dissonance,
    'shadow_from_above': shadow_from_above, 'fold_space': fold_space,
    'foregone_loss': foregone_loss, 'foregone_survival': foregone_survival, 'higher_revival': higher_revival,
    'bloom': bloom, 'fall_silent': fall_silent, 'erase': erase,
}


# ------------------------------------------------------------------ the 16x16 status pictures

def s(fn):
    img, d = canvas(16)
    fn(d)
    return outlined(img)


def i_territory(d):
    d.rectangle([6, 3, 9, 13], fill=BROWN)
    d.rectangle([2, 5, 13, 6], fill=BROWN_DARK)
    eye(d, 7.5, 8.5, 2, 1.2, PAPER, MOSS, None)
    d.line([(1, 14), (14, 14)], fill=MOSS)


def i_true_sight(d):
    eye(d, 7.5, 7.5, 6.5, 3.5, PAPER, VIOLET)


def i_many_angles(d):
    eye(d, 4.5, 4.5, 3, 1.8, PAPER, TEAL, None)
    eye(d, 11, 4.5, 3, 1.8, PAPER, TEAL, None)
    eye(d, 7.5, 11, 3, 1.8, PAPER, RED, None)


def i_hidden(d):
    frame(d, 3, 1, 12, 14, SKY)
    figure(d, 7.5, 5, 2.4, INK)


def i_painted_face(d):
    d.ellipse([3, 2, 12, 14], fill=PAPER)
    d.line([(5, 7), (7, 7)], fill=INK)
    d.line([(9, 7), (11, 7)], fill=INK)
    d.line([(6, 11), (9, 11)], fill=RED)


def i_self_portrait(d):
    frame(d, 3, 1, 12, 14, (58, 52, 72, 255))
    d.ellipse([5, 3, 10, 8], fill=PAPER)
    d.polygon([(4, 13), (11, 13), (10, 9), (5, 9)], fill=INK)


def i_ink_servants(d):
    for x, y in ((4, 5), (11, 5), (7.5, 11)):
        d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(40, 40, 52, 255))
        d.point((x, y - 0.5), fill=TEAL_LIGHT)


def i_phase(d):
    figure(d, 7.5, 2, 3, (190, 230, 226, 255))
    d.ellipse([2, 12, 13, 15], outline=TEAL)


def i_pixie_wings(d):
    d.ellipse([1, 3, 7, 7], fill=(200, 250, 245, 255))
    d.ellipse([8, 3, 14, 7], fill=(200, 250, 245, 255))
    d.ellipse([2, 8, 7, 11], fill=TEAL)
    d.ellipse([8, 8, 13, 11], fill=TEAL)
    d.line([(7.5, 2), (7.5, 14)], fill=TEAL)


def i_shadow_form(d):
    figure(d, 7.5, 1, 3.6, SHADOW)
    d.line([(7.5, 5), (7.5, 13)], fill=VIOLET)


def i_shadow_weakness(d):
    figure(d, 7.5, 2, 3.2, (90, 80, 110, 255))
    d.line([(3, 13), (12, 13)], fill=GREY)
    d.polygon([(11, 3), (14, 3), (12.5, 6)], fill=GREY)


def i_harmony(d):
    note(d, 5, 11, GOLD_LIGHT, 7)
    note(d, 10, 10, GOLD_LIGHT, 7)
    d.line([(7, 4), (12, 3)], fill=GOLD_LIGHT)


def i_foregone_survival(d):
    d.rectangle([3, 2, 12, 13], fill=PAPER)
    d.line([(5, 5), (10, 5)], fill=INK)
    d.line([(5, 8), (10, 8)], fill=INK)
    d.ellipse([5, 9, 10, 13], outline=GOLD_DEEP)


def i_higher_revival(d):
    d.polygon([(7.5, 1), (14, 7.5), (7.5, 14), (1, 7.5)], outline=GOLD_LIGHT)
    eye(d, 7.5, 7.5, 3.5, 2, GOLD_LIGHT, GOLD_DEEP, None)


def i_bloom(d):
    flower(d, 7.5, 7.5, 6)


def i_fall_silent(d):
    eye(d, 7.5, 7.5, 6, 3, GOLD_LIGHT, GOLD_DEEP, open_=0.0)
    d.point((4, 10), fill=GOLD_LIGHT)
    d.point((7.5, 11), fill=GOLD_LIGHT)
    d.point((11, 10), fill=GOLD_LIGHT)


def i_fantasy_double(d):
    figure(d, 4.5, 3, 2.6, PAPER)
    d.ellipse([9.5, 3, 12.5, 6], outline=VIOLET_LIGHT)
    d.polygon([(9, 6.5), (13, 6.5), (13.8, 12), (8.2, 12)], outline=VIOLET_LIGHT)


def i_watched(d):
    eye(d, 7.5, 7.5, 6, 3, PAPER, (150, 140, 200, 255), None)


def i_studied(d):
    eye(d, 7.5, 7.5, 6, 3, PAPER, TEAL)
    d.point((2, 2), fill=TEAL)
    d.point((13, 2), fill=TEAL)


def i_seen_through(d):
    eye(d, 7.5, 7.5, 6.5, 3.6, PAPER, VIOLET)
    d.line([(7.5, 0), (7.5, 3)], fill=VIOLET_LIGHT)
    d.line([(7.5, 12), (7.5, 15)], fill=VIOLET_LIGHT)
    d.line([(0, 7.5), (1, 7.5)], fill=VIOLET_LIGHT)
    d.line([(14, 7.5), (15, 7.5)], fill=VIOLET_LIGHT)


def i_marked(d):
    d.ellipse([1, 4, 14, 12], outline=VIOLET, width=2)
    eye(d, 7.5, 8, 3, 1.6, PAPER, VIOLET, None)


def i_riot(d):
    for k, col in enumerate(HUES):
        a0 = math.tau * k / 6
        pts = [(7.5 + math.cos(a0 + t * 0.5) * t * 0.9, 7.5 + math.sin(a0 + t * 0.5) * t * 0.9) for t in range(8)]
        d.line(pts, fill=col)


def i_itching(d):
    d.polygon([(3, 13), (12, 13), (7.5, 3)], fill=BLUE)
    for x in (2, 6, 10, 13):
        d.line([(x, 1), (x - 1, 4)], fill=BROWN)


def i_trapped(d):
    frame(d, 1, 3, 14, 12, (8, 6, 14, 255))
    figure(d, 7.5, 4, 2, PAPER)


def i_foregone_loss(d):
    d.rectangle([3, 2, 12, 13], fill=PAPER)
    d.line([(5, 5), (10, 5)], fill=INK)
    d.line([(5, 8), (10, 8)], fill=INK)
    d.line([(4, 12), (11, 9)], fill=RED)


def i_resonating(d):
    d.ellipse([1, 1, 14, 14], outline=GOLD)
    d.ellipse([4, 4, 11, 11], outline=GOLD_LIGHT)
    d.point((7.5, 7.5), fill=WHITE)


def i_dissonant(d):
    note(d, 5, 11, VIOLET_LIGHT, 6)
    d.ellipse([9, 9, 12, 11], fill=VIOLET_LIGHT)
    d.line([(12, 10), (14, 5)], fill=VIOLET_LIGHT)
    d.line([(8, 1), (8, 14)], fill=RED)


def i_silent(d):
    d.line([(7.5, 14), (7.5, 7)], fill=MOSS_DARK)
    d.polygon([(7.5, 7), (3, 11), (6.5, 10)], fill=GREY)
    d.polygon([(7.5, 7), (12, 11), (8.5, 10)], fill=GREY)
    d.line([(4, 3), (11, 3)], fill=GREY)


def i_erased(d):
    figure(d, 7.5, 2, 3.4, (110, 110, 120, 255))
    d.rectangle([2, 7, 13, 10], fill=(0, 0, 0, 0))
    d.line([(1, 8.5), (14, 8.5)], fill=PAPER)


STATUS = {
    'territory': i_territory, 'true_sight': i_true_sight, 'many_angles': i_many_angles, 'hidden': i_hidden,
    'painted_face': i_painted_face, 'self_portrait': i_self_portrait, 'ink_servants': i_ink_servants,
    'phase': i_phase, 'pixie_wings': i_pixie_wings, 'shadow_form': i_shadow_form,
    'shadow_weakness': i_shadow_weakness, 'harmony': i_harmony, 'foregone_survival': i_foregone_survival,
    'higher_revival': i_higher_revival, 'bloom': i_bloom, 'fall_silent': i_fall_silent,
    'fantasy_double': i_fantasy_double,
    'watched': i_watched, 'studied': i_studied, 'seen_through': i_seen_through, 'marked': i_marked,
    'riot': i_riot, 'itching': i_itching, 'trapped': i_trapped, 'foregone_loss': i_foregone_loss,
    'resonating': i_resonating, 'dissonant': i_dissonant, 'silent': i_silent, 'erased': i_erased,
}


def pathway_icon():
    """An eye looking down from a higher dimension, a canvas frame round it, on the Pathway's violet."""
    img, d = canvas(16)
    d.ellipse([0, 0, 15, 15], fill=VIOLET_DARK)
    d.rectangle([3, 4, 12, 11], outline=GOLD)
    eye(d, 7.5, 7.5, 5.5, 2.8, PAPER, VIOLET)
    d.point((7.5, 1), fill=GOLD_LIGHT)
    d.point((7.5, 14), fill=GOLD_LIGHT)
    return outlined(img)


def main(assets, preview=None):
    out = []
    for name, fn in WHEEL.items():
        path = os.path.join(assets, 'textures/gui/ability_menus/observer', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fn().save(path)
        out.append(path)
    for name, fn in STATUS.items():
        path = os.path.join(assets, 'textures/gui/observer', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        s(fn).save(path)
        out.append(path)
    path = os.path.join(assets, 'textures/gui/pathway_icons/observer.png')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    pathway_icon().save(path)
    out.append(path)
    if preview:
        cell = 136
        sheet = Image.new('RGBA', (cell * 10, cell * ((len(out) + 9) // 10)), (40, 40, 48, 255))
        for i, p in enumerate(out):
            im = Image.open(p).convert('RGBA').resize((128, 128), Image.NEAREST)
            sheet.alpha_composite(im, ((i % 10) * cell + 4, (i // 10) * cell + 4))
        sheet.save(preview)
    return [os.path.relpath(p, assets) for p in out]


if __name__ == '__main__':
    for p in main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None):
        print(p)
