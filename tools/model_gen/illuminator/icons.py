"""Screen pictures for the Illuminator (Paragon pathway).

python3 tools/model_gen/illuminator/icons.py src/main/resources/assets/circle_of_inevitability [preview.png]

  textures/gui/ability_menus/illuminator/*.png   32x32, the four wheels (Workshop on key 4, Astronomy on key 5,
                                                 Alchemy and Arcana on key 6, Civilization on key 7), in a round
                                                 frame of toothed brass
  textures/gui/illuminator/*.png                 16x16, the statuses shown above the hotbar
  textures/gui/pathway_icons/illuminator.png     16x16, the Pathway's icon beside the hotbar

Every picture is drawn in flat colors and then outlined automatically, so the shapes stay simple to change.
Replace any of them with a hand-drawn PNG of the same name.
"""
import math
import os
import sys

from PIL import Image, ImageDraw

# ------------------------------------------------------------------ colors
OUTLINE = (20, 16, 12, 255)
BRASS = (217, 164, 65, 255)
BRASS_LIGHT = (255, 226, 150, 255)
BRASS_DARK = (122, 80, 24, 255)
COPPER = (214, 120, 70, 255)
IRON = (84, 88, 98, 255)
IRON_LIGHT = (150, 156, 168, 255)
STEEL = (180, 196, 220, 255)
BLUE = (70, 150, 240, 255)
BLUE_LIGHT = (190, 228, 255, 255)
BLUE_DARK = (24, 60, 130, 255)
STEAM = (236, 240, 246, 255)
PARCHMENT = (243, 230, 196, 255)
PARCHMENT_DARK = (190, 160, 110, 255)
INK = (27, 42, 65, 255)
STAR = (255, 236, 170, 255)
WHITE = (255, 255, 255, 255)
ACID = (200, 230, 60, 255)
ACID_DARK = (110, 140, 20, 255)
RED = (210, 60, 60, 255)
RED_LIGHT = (255, 140, 130, 255)
VIOLET = (176, 120, 255, 255)
VIOLET_DARK = (90, 50, 150, 255)
GREEN = (110, 180, 90, 255)
WOOD = (120, 72, 40, 255)
STRAW = (230, 200, 110, 255)
STONE = (150, 150, 150, 255)
NIGHT = (20, 26, 60, 255)


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

def star(d, cx, cy, r, fill, points=4, inner=0.4):
    pts = []
    for i in range(points * 2):
        a = -math.pi / 2 + math.pi * i / points
        rr = r if i % 2 == 0 else r * inner
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.polygon(pts, fill=fill)


def gear(d, cx, cy, r, teeth, fill, hole=None, turn=0.0):
    pts = []
    for i in range(teeth * 4):
        a = turn + math.tau * i / (teeth * 4)
        rr = r if (i % 4) in (1, 2) else r * 0.72
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.polygon(pts, fill=fill)
    if hole is not None:
        rh = max(0.6, r * 0.28)
        d.ellipse([cx - rh, cy - rh, cx + rh, cy + rh], fill=hole)


def figure(d, cx, top, s, fill):
    """A standing person: a head and a body."""
    d.ellipse([cx - s * 0.5, top, cx + s * 0.5, top + s], fill=fill)
    d.polygon([(cx - s * 0.6, top + s * 1.1), (cx + s * 0.6, top + s * 1.1), (cx + s * 0.9, top + s * 3.4),
               (cx - s * 0.9, top + s * 3.4)], fill=fill)


def book(d, cx, cy, w, h, cover, page, line):
    """An open book seen from above."""
    d.polygon([(cx - w, cy - h + 1), (cx, cy - h + 2), (cx + w, cy - h + 1), (cx + w, cy + h), (cx, cy + h + 1),
               (cx - w, cy + h)], fill=cover)
    d.polygon([(cx - w + 1, cy - h + 1), (cx, cy - h + 2), (cx, cy + h - 1), (cx - w + 1, cy + h - 2)], fill=page)
    d.polygon([(cx + w - 1, cy - h + 1), (cx, cy - h + 2), (cx, cy + h - 1), (cx + w - 1, cy + h - 2)], fill=page)
    for i in range(1, h, 2):
        d.line([(cx - w + 2, cy - h + 2 + i), (cx - 2, cy - h + 2 + i)], fill=line)
        d.line([(cx + 2, cy - h + 2 + i), (cx + w - 2, cy - h + 2 + i)], fill=line)


# ------------------------------------------------------------------ the 32x32 wheel pictures

S = 32
C = 15.5


def wheel_base(inside=(16, 30, 60, 255), edge=(30, 60, 110, 255)):
    """A round frame of toothed brass round a field of blueprint blue with a faint grid."""
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = im.load()
    for y in range(S):
        for x in range(S):
            dd = math.hypot(x - C, y - C)
            a = math.atan2(y - C, x - C)
            tooth = 15.6 if (int((a + math.pi) / math.tau * 24) % 2 == 0) else 14.4
            if dd <= tooth:
                if dd > 13.2:
                    light = (x - C) * -0.7 + (y - C) * -0.7
                    px[x, y] = BRASS_LIGHT if light > 8 else BRASS if light > -6 else BRASS_DARK
                elif dd > 12.4:
                    px[x, y] = (60, 40, 16, 255)
                else:
                    t = dd / 12.4
                    col = tuple(int(inside[i] * (1 - t) + edge[i] * t) for i in range(3)) + (255,)
                    if (x % 6 == 3 or y % 6 == 3):
                        col = tuple(min(255, c + 16) for c in col[:3]) + (255,)
                    px[x, y] = col
    return im


def in_frame(base, layer):
    """The drawing, outlined, laid into the frame and cut to its inside."""
    layer = outlined(layer)
    lp = layer.load()
    bp = base.load()
    for y in range(S):
        for x in range(S):
            if lp[x, y][3] and math.hypot(x - C, y - C) <= 12.4:
                bp[x, y] = lp[x, y]
    return base


WORK = dict(inside=(40, 26, 14, 255), edge=(80, 52, 24, 255))
ASTRO = dict(inside=(10, 14, 40, 255), edge=(30, 40, 90, 255))
ARCANA = dict(inside=(16, 30, 60, 255), edge=(30, 60, 110, 255))
CIVIL = dict(inside=(14, 20, 34, 255), edge=(60, 50, 40, 255))


def w(inside):
    def deco(fn):
        def make():
            base = wheel_base(**inside)
            layer, d = canvas(S)
            fn(d)
            return in_frame(base, layer)
        return make
    return deco


# ---- key 4: Workshop

@w(WORK)
def w_clockwork_rifle(d):
    d.polygon([(4, 20), (20, 12), (21, 14), (6, 22)], fill=IRON_LIGHT)         # barrel
    d.polygon([(16, 16), (24, 12), (28, 20), (24, 22), (19, 19)], fill=WOOD)    # stock
    d.rectangle([14, 13, 19, 18], fill=BRASS)                                    # receiver
    gear(d, 15, 20, 3, 8, BRASS_LIGHT, BRASS_DARK)                               # the spring drum
    d.line([(10, 13), (16, 10)], fill=COPPER)                                    # scope
    d.point((4, 19), fill=BLUE_LIGHT)
    d.line([(1, 22), (3, 21)], fill=BLUE_LIGHT)


@w(WORK)
def w_spirit_cannon(d):
    d.polygon([(6, 16), (21, 9), (24, 14), (9, 21)], fill=IRON)                  # barrel
    d.polygon([(4, 15), (7, 14), (10, 21), (7, 22)], fill=BRASS)                 # muzzle swell
    d.line([(11, 13), (20, 9)], fill=BLUE_LIGHT)                                 # runes
    d.rectangle([11, 18, 25, 22], fill=WOOD)                                     # carriage
    gear(d, 13, 23, 4, 8, BRASS, BRASS_DARK)                                     # a gear-wheel
    gear(d, 23, 23, 4, 8, BRASS, BRASS_DARK)
    d.ellipse([1, 13, 4, 16], fill=STEAM)


@w(WORK)
def w_mystic_brooch(d):
    gear(d, 15.5, 15.5, 10, 12, BRASS, None)
    d.ellipse([8, 8, 23, 23], fill=BRASS_DARK)
    d.polygon([(15.5, 9), (21, 15.5), (15.5, 22), (10, 15.5)], fill=BLUE)       # the gem
    d.polygon([(15.5, 9), (21, 15.5), (15.5, 15.5)], fill=BLUE_LIGHT)
    d.point((14, 13), fill=WHITE)


# ---- key 5: Astronomy

@w(ASTRO)
def w_star_of_curses(d):
    star(d, 21, 10, 5, STAR, 5, 0.45)
    d.line([(20, 12), (8, 22)], fill=STAR, width=2)                              # the ray
    figure(d, 8, 17, 2.4, VIOLET_DARK)
    for x, y in ((5, 15), (11, 15), (8, 26)):
        d.point((x, y), fill=VIOLET)


@w(ASTRO)
def w_blessing_of_stars(d):
    star(d, 15.5, 7, 4, STAR)
    for x in (9, 15.5, 22):                                                      # pressing down
        d.line([(x, 11), (x, 19)], fill=VIOLET)
        d.polygon([(x - 2, 17), (x + 2, 17), (x, 20)], fill=VIOLET)
    d.arc([3, 20, 28, 34], 200, 340, fill=BLUE_LIGHT)
    star(d, 6, 9, 1.4, WHITE)
    star(d, 25, 11, 1.4, WHITE)


@w(ASTRO)
def w_false_constellations(d):
    d.pieslice([4, 6, 27, 29], 180, 360, fill=NIGHT)                             # a dome of fake sky
    pts = [(8, 15), (12, 11), (16, 13), (20, 9), (24, 14)]
    d.line(pts, fill=BLUE_LIGHT)
    for p in pts:
        d.point(p, fill=WHITE)
    for x in (7, 11, 15, 19, 23):
        d.point((x, 17), fill=(80, 110, 170, 255))                               # the grid that gives it away
    figure(d, 15.5, 19, 2.2, (60, 70, 100, 255))


@w(ASTRO)
def w_meteor_shower(d):
    for x, y, r in ((11, 10, 2.6), (20, 14, 3.0), (13, 21, 2.2)):
        d.line([(x - 7, y - 6), (x, y)], fill=BLUE_LIGHT, width=2)
        star(d, x, y, r, STAR)
    d.line([(4, 26), (27, 26)], fill=BRASS_DARK)


# ---- key 6: Alchemy and Arcana

@w(ARCANA)
def w_living_doll(d):
    figure(d, 15.5, 6, 4, STRAW)
    d.line([(13, 8), (14, 9)], fill=INK)
    d.line([(17, 8), (18, 9)], fill=INK)
    d.rectangle([10, 22, 21, 26], fill=STONE)                                    # rising out of the ground
    d.point((11, 13), fill=BRASS_LIGHT)
    d.point((20, 13), fill=BRASS_LIGHT)


@w(ARCANA)
def w_golem(d):
    d.rectangle([9, 9, 22, 21], fill=IRON)                                       # body
    d.rectangle([12, 5, 19, 9], fill=BRASS)                                      # head
    d.line([(13, 7), (18, 7)], fill=(255, 200, 100, 255))
    d.rectangle([13, 12, 18, 17], fill=(255, 180, 60, 255))                      # furnace heart
    d.rectangle([5, 10, 8, 20], fill=BRASS_DARK)
    d.rectangle([23, 10, 26, 20], fill=BRASS_DARK)
    d.rectangle([10, 21, 14, 27], fill=IRON)
    d.rectangle([17, 21, 21, 27], fill=IRON)
    gear(d, 7, 9, 2.5, 6, BRASS_LIGHT, None)
    gear(d, 24, 9, 2.5, 6, BRASS_LIGHT, None)


@w(ARCANA)
def w_mechanical_body(d):
    figure(d, 15.5, 4, 4.4, BRASS)
    d.rectangle([12, 5, 19, 7], fill=BLUE_LIGHT)                                 # the goggles
    gear(d, 15.5, 14, 3, 8, BRASS_LIGHT, BRASS_DARK)
    d.rectangle([20, 10, 23, 18], fill=COPPER)                                   # a tank of the jetpack
    for x, y in ((21, 21), (22, 24), (20, 26)):
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=STEAM)


@w(ARCANA)
def w_dissociation(d):
    d.ellipse([3, 12, 10, 18], fill=WHITE)                                       # the eye
    d.ellipse([5, 13, 8, 17], fill=BLUE)
    d.polygon([(10, 13), (28, 9), (28, 21), (10, 17)], fill=(220, 236, 246, 255))  # the clear misty beam
    for x, y in ((18, 8), (23, 22), (26, 7), (15, 21)):
        d.point((x, y), fill=STEAM)


@w(ARCANA)
def w_knowledge_spirit(d):
    for x, y in ((6, 20), (10, 15), (14, 11), (19, 9), (23, 12)):               # pages flying
        d.polygon([(x, y), (x + 4, y - 1), (x + 5, y + 3), (x + 1, y + 4)], fill=PARCHMENT)
        d.line([(x + 1, y + 1), (x + 3, y + 1)], fill=INK)
    d.ellipse([20, 18, 27, 25], fill=VIOLET_DARK)                                # the mind it enters
    d.point((23, 21), fill=WHITE)


# ---- key 7: Civilization

@w(CIVIL)
def w_rewrite_principles(d):
    for x in (8, 15.5, 23):
        d.line([(x, 24), (x, 10)], fill=BLUE_LIGHT)
        d.polygon([(x - 2, 12), (x + 2, 12), (x, 8)], fill=BLUE_LIGHT)
    figure(d, 15.5, 13, 2, (180, 160, 120, 255))
    d.line([(4, 27), (27, 27)], fill=BRASS)
    d.line([(6, 5), (25, 5)], fill=PARCHMENT_DARK)
    d.line([(10, 3), (21, 7)], fill=RED)                                         # the old law struck out


@w(CIVIL)
def w_colossus(d):
    d.rectangle([8, 6, 23, 16], fill=IRON)                                       # body
    d.rectangle([11, 3, 20, 6], fill=BRASS)
    d.rectangle([12, 9, 19, 14], fill=STAR)                                      # the fusion core
    d.rectangle([4, 7, 8, 17], fill=BRASS)
    d.rectangle([23, 7, 27, 17], fill=BRASS)
    d.rectangle([8, 16, 11, 27], fill=IRON_LIGHT)                                # legs
    d.rectangle([20, 16, 23, 27], fill=IRON_LIGHT)
    figure(d, 15.5, 18, 1.8, (90, 100, 120, 255))                                # the pilot in the cage
    d.ellipse([9, 0, 12, 3], fill=STEAM)


@w(CIVIL)
def w_scrolls_of_civilization(d):
    for i in range(6):                                                           # murals in a ring
        a = math.tau * i / 6
        x, y = 15.5 + math.cos(a) * 9, 15.5 + math.sin(a) * 9
        d.rectangle([x - 2, y - 3, x + 2, y + 3], fill=PARCHMENT)
        d.point((x, y), fill=(200, 120, 50, 255) if i % 2 else BLUE)
    figure(d, 15.5, 12, 2.2, BRASS)


@w(CIVIL)
def w_mathematical_maze(d):
    d.rectangle([7, 8, 24, 25], outline=BLUE_LIGHT)
    d.polygon([(7, 8), (12, 4), (29, 4), (24, 8)], outline=BLUE_LIGHT)
    d.line([(24, 25), (29, 21), (29, 4)], fill=BLUE_LIGHT)
    for x in (13, 18):
        d.line([(x, 8), (x, 25)], fill=BLUE)
    d.line([(7, 16), (24, 16)], fill=BLUE)
    figure(d, 15.5, 12, 2.2, VIOLET_DARK)
    d.line([(20, 19), (22, 19), (20, 22), (22, 22)], fill=WHITE)                 # a sum


@w(CIVIL)
def w_masterwork(d):
    for k in range(5):                                                           # a staff
        d.line([(4, 7 + k * 3), (27, 7 + k * 3)], fill=PARCHMENT_DARK)
    for x, y in ((8, 13), (13, 10), (18, 16), (23, 12)):
        d.ellipse([x - 2, y - 1, x + 1, y + 2], fill=STAR)
        d.line([(x + 1, y), (x + 1, y - 6)], fill=STAR)
    d.line([(6, 27), (22, 20)], fill=(30, 26, 30, 255), width=2)                 # the baton
    d.point((22, 20), fill=WHITE)


@w(CIVIL)
def w_reality_knowledge(d):
    book(d, 15.5, 15.5, 9, 7, INK, PARCHMENT, PARCHMENT_DARK)
    d.ellipse([4, 4, 27, 27], outline=RED)
    d.line([(7, 24), (24, 7)], fill=RED, width=2)


@w(CIVIL)
def w_illusory_book(d):
    book(d, 15.5, 19, 11, 6, INK, PARCHMENT, PARCHMENT_DARK)
    for x, y in ((9, 10), (13, 7), (18, 6), (22, 9)):                            # someone turning into words
        d.line([(x, y), (x + 3, y)], fill=WHITE)
    figure(d, 15.5, 4, 1.6, (150, 150, 180, 255))


@w(CIVIL)
def w_mechanical_heaven(d):
    gear(d, 11, 13, 7, 10, BRASS, BRASS_DARK)
    gear(d, 21, 19, 5, 8, BRASS_LIGHT, BRASS_DARK, 0.3)
    gear(d, 22, 8, 3.5, 6, BRASS, BRASS_DARK)
    for x, y in ((5, 24), (9, 26), (14, 25)):
        d.ellipse([x - 2, y - 1, x + 2, y + 1], fill=STEAM)
    star(d, 27, 13, 1.6, WHITE)


WHEEL = {
    'clockwork_rifle': w_clockwork_rifle,
    'spirit_cannon': w_spirit_cannon,
    'mystic_brooch': w_mystic_brooch,
    'star_of_curses': w_star_of_curses,
    'blessing_of_stars': w_blessing_of_stars,
    'false_constellations': w_false_constellations,
    'meteor_shower': w_meteor_shower,
    'living_doll': w_living_doll,
    'golem': w_golem,
    'mechanical_body': w_mechanical_body,
    'dissociation': w_dissociation,
    'knowledge_spirit': w_knowledge_spirit,
    'rewrite_principles': w_rewrite_principles,
    'colossus': w_colossus,
    'scrolls_of_civilization': w_scrolls_of_civilization,
    'mathematical_maze': w_mathematical_maze,
    'masterwork': w_masterwork,
    'reality_knowledge': w_reality_knowledge,
    'illusory_book': w_illusory_book,
    'mechanical_heaven': w_mechanical_heaven,
}


# ------------------------------------------------------------------ the 16x16 status pictures

def s(fn):
    img, d = canvas(16)
    fn(d)
    return outlined(img)


# what the Illuminator holds

def i_survey(d):
    d.ellipse([1, 1, 14, 14], outline=BLUE)
    d.ellipse([4, 4, 11, 11], outline=BLUE_LIGHT)
    d.line([(7, 2), (7, 13)], fill=BLUE_LIGHT)
    d.line([(2, 7), (13, 7)], fill=BLUE_LIGHT)
    d.point((10, 4), fill=RED_LIGHT)


def i_ritual(d):
    d.ellipse([1, 4, 14, 13], outline=WHITE)
    d.polygon([(7.5, 5), (11, 11), (4, 11)], outline=(220, 220, 220, 255))
    d.point((7, 9), fill=BRASS_LIGHT)


def i_brooch(d):
    gear(d, 7.5, 7.5, 6.5, 8, BRASS, None)
    d.polygon([(7.5, 3.5), (11, 7.5), (7.5, 11.5), (4, 7.5)], fill=BLUE)
    d.point((7, 6), fill=WHITE)


def i_mechanical_body(d):
    figure(d, 7.5, 1, 3.2, BRASS)
    d.rectangle([6, 2, 9, 3], fill=BLUE_LIGHT)
    gear(d, 7.5, 8, 2, 6, BRASS_LIGHT, BRASS_DARK)
    d.point((12, 13), fill=STEAM)


def i_colossus(d):
    d.rectangle([3, 2, 12, 8], fill=IRON)
    d.rectangle([6, 4, 9, 6], fill=STAR)
    d.rectangle([3, 8, 5, 14], fill=IRON_LIGHT)
    d.rectangle([10, 8, 12, 14], fill=IRON_LIGHT)
    d.rectangle([1, 3, 2, 8], fill=BRASS)
    d.rectangle([13, 3, 14, 8], fill=BRASS)


def i_heaven(d):
    gear(d, 6, 6, 5, 8, BRASS, BRASS_DARK)
    gear(d, 11.5, 11.5, 3.5, 6, BRASS_LIGHT, BRASS_DARK, 0.3)


def i_false_sky(d):
    d.pieslice([1, 3, 14, 16], 180, 360, fill=NIGHT)
    d.point((5, 7), fill=WHITE)
    d.point((8, 5), fill=WHITE)
    d.point((11, 8), fill=WHITE)
    d.line([(5, 7), (8, 5), (11, 8)], fill=BLUE_LIGHT)


def i_masterwork(d):
    for k in range(3):
        d.line([(1, 4 + k * 3), (14, 4 + k * 3)], fill=PARCHMENT_DARK)
    d.ellipse([3, 8, 6, 11], fill=STAR)
    d.line([(6, 9), (6, 3)], fill=STAR)
    d.ellipse([9, 5, 12, 8], fill=STAR)
    d.line([(12, 6), (12, 1)], fill=STAR)


def i_steam_form(d):
    for x, y, r in ((5, 9, 3.5), (10, 8, 4), (8, 4, 3)):
        d.ellipse([x - r, y - r, x + r, y + r], fill=STEAM)
    d.rectangle([4, 11, 11, 13], fill=PARCHMENT)


def i_spirit_form(d):
    for x, y in ((2, 9), (6, 5), (10, 8), (8, 11)):
        d.polygon([(x, y), (x + 4, y - 1), (x + 5, y + 2), (x + 1, y + 3)], fill=PARCHMENT)


def i_enlightened(d):
    for i in range(8):
        a = math.tau * i / 8
        d.line([(7.5 + math.cos(a) * 4, 7.5 + math.sin(a) * 4), (7.5 + math.cos(a) * 7, 7.5 + math.sin(a) * 7)],
               fill=STAR)
    d.ellipse([4, 7, 11, 11], fill=BRASS)
    d.polygon([(9, 7), (10, 3), (11, 7)], fill=WHITE)


def i_weightless(d):
    figure(d, 7.5, 2, 2.6, STEAM)
    for x in (3, 12):
        d.line([(x, 13), (x, 7)], fill=BLUE_LIGHT)
        d.polygon([(x - 1, 8), (x + 1, 8), (x, 6)], fill=BLUE_LIGHT)


# what is done to others (when they are players)

def i_appraised(d):
    d.ellipse([1, 1, 12, 12], fill=BLUE_DARK, outline=BLUE_LIGHT)
    d.line([(11, 11), (14, 14)], fill=BRASS)
    d.line([(4, 6), (9, 6)], fill=BLUE_LIGHT)
    d.line([(6, 4), (6, 9)], fill=BLUE_LIGHT)


def i_corroded(d):
    d.polygon([(7.5, 1), (12, 8), (7.5, 14), (3, 8)], fill=ACID)
    d.ellipse([3, 6, 12, 14], fill=ACID)
    d.point((6, 9), fill=WHITE)
    d.point((9, 11), fill=ACID_DARK)


def i_cursed_weak(d):
    star(d, 7.5, 7.5, 6, (200, 180, 240, 255), 5, 0.45)
    d.line([(2, 13), (13, 2)], fill=VIOLET_DARK, width=2)


def i_trauma(d):
    star(d, 7.5, 5, 5, RED_LIGHT, 5, 0.45)
    for x in (5, 8, 11):
        d.line([(x, 9), (x, 13)], fill=RED)


def i_mutation(d):
    star(d, 7.5, 7.5, 7, (150, 200, 90, 255), 5, 0.35)
    d.ellipse([5, 6, 10, 9], fill=WHITE)
    d.point((7, 7), fill=INK)


def i_pressed(d):
    for x in (3, 7, 11):
        d.line([(x, 1), (x, 8)], fill=VIOLET)
        d.polygon([(x - 2, 7), (x + 2, 7), (x, 10)], fill=VIOLET)
    d.rectangle([1, 12, 14, 14], fill=STONE)


def i_lifted(d):
    figure(d, 7.5, 1, 2.4, (190, 170, 130, 255))
    d.line([(2, 14), (13, 14)], fill=BRASS_DARK)
    d.line([(4, 12), (11, 12)], fill=BLUE_LIGHT)


def i_eroded(d):
    d.rectangle([3, 1, 12, 14], fill=PARCHMENT)
    for y in (3, 5, 7):
        d.line([(4, y), (11 - y // 2, y)], fill=INK)
    d.ellipse([6, 9, 9, 12], fill=(0, 0, 0, 0))
    d.point((10, 11), fill=PARCHMENT_DARK)


def i_maze(d):
    d.rectangle([1, 1, 14, 14], outline=BLUE_LIGHT)
    d.line([(5, 1), (5, 14)], fill=BLUE)
    d.line([(10, 1), (10, 14)], fill=BLUE)
    d.line([(1, 7), (14, 7)], fill=BLUE)
    d.point((7, 10), fill=WHITE)


def i_forgotten(d):
    book(d, 7.5, 8, 6, 5, INK, PARCHMENT, PARCHMENT_DARK)
    d.line([(2, 13), (13, 2)], fill=RED, width=2)


def i_written(d):
    book(d, 7.5, 10, 7, 4, INK, PARCHMENT, PARCHMENT_DARK)
    figure(d, 7.5, 0, 1.6, (150, 150, 180, 255))


def i_immersed(d):
    d.rectangle([2, 2, 13, 13], fill=PARCHMENT)
    d.polygon([(3, 12), (7, 5), (11, 12)], fill=(220, 150, 70, 255))
    d.ellipse([9, 3, 12, 6], fill=STAR)


STATUS = {
    'survey': i_survey,
    'ritual': i_ritual,
    'brooch': i_brooch,
    'mechanical_body': i_mechanical_body,
    'colossus': i_colossus,
    'heaven': i_heaven,
    'false_sky': i_false_sky,
    'masterwork': i_masterwork,
    'steam_form': i_steam_form,
    'spirit_form': i_spirit_form,
    'enlightened': i_enlightened,
    'weightless': i_weightless,
    'appraised': i_appraised,
    'corroded': i_corroded,
    'cursed_weak': i_cursed_weak,
    'trauma': i_trauma,
    'mutation': i_mutation,
    'pressed': i_pressed,
    'lifted': i_lifted,
    'eroded': i_eroded,
    'maze': i_maze,
    'forgotten': i_forgotten,
    'written': i_written,
    'immersed': i_immersed,
}


def pathway_icon():
    """A lamp of knowledge in a brass gear."""
    img, d = canvas(16)
    gear(d, 7.5, 7.5, 7.5, 10, BRASS, None)
    d.ellipse([2, 2, 13, 13], fill=BLUE_DARK)
    d.ellipse([4, 8, 11, 11], fill=BRASS_LIGHT)                                 # the lamp
    d.line([(11, 9), (13, 8)], fill=BRASS_LIGHT)
    d.polygon([(12, 7), (13, 3), (14, 7)], fill=STAR)                           # its flame
    d.point((6, 5), fill=BLUE_LIGHT)
    d.point((9, 4), fill=BLUE_LIGHT)
    return outlined(img)


def main(assets, preview=None):
    out = []
    for name, fn in WHEEL.items():
        path = os.path.join(assets, 'textures/gui/ability_menus/illuminator', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fn().save(path)
        out.append(path)
    for name, fn in STATUS.items():
        path = os.path.join(assets, 'textures/gui/illuminator', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        s(fn).save(path)
        out.append(path)
    path = os.path.join(assets, 'textures/gui/pathway_icons/illuminator.png')
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
    for p in out:
        print(os.path.relpath(p, assets))


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None)
