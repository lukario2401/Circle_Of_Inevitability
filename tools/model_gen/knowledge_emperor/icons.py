"""Screen pictures for the Knowledge Emperor (Hermit pathway).

python3 tools/model_gen/knowledge_emperor/icons.py src/main/resources/assets/circle_of_inevitability [preview.png]

  textures/gui/ability_menus/knowledge_emperor/*.png   32x32, the three wheels (Stars on key 5, Retellings and
                                                       Prophecy on key 6, Knowledge on key 7), in a round frame of
                                                       starlit silver
  textures/gui/knowledge_emperor/*.png                 16x16, the statuses shown above the hotbar, and the three
                                                       scrolls of the pouch (scroll_freezing, scroll_paralysis,
                                                       scroll_inscribed) shown beside it
  textures/gui/pathway_icons/knowledge_emperor.png     16x16, the Pathway's icon beside the hotbar

Every picture is drawn in flat colors and then outlined automatically, so the shapes stay simple to change.
Replace any of them with a hand-drawn PNG of the same name.
"""
import math
import os
import sys

from PIL import Image, ImageDraw

# ------------------------------------------------------------------ colors
OUTLINE = (14, 8, 26, 255)
ORCHID = (192, 112, 232, 255)
ORCHID_LIGHT = (232, 196, 255, 255)
ORCHID_DARK = (106, 50, 150, 255)
STAR = (246, 244, 255, 255)
WHITE = (255, 255, 255, 255)
SILVER = (196, 204, 222, 255)
SILVER_LIGHT = (244, 248, 255, 255)
SILVER_DARK = (104, 112, 136, 255)
INDIGO = (42, 26, 90, 255)
INDIGO_LIGHT = (84, 64, 160, 255)
INK = (20, 12, 32, 255)
GOLD = (236, 200, 110, 255)
GOLD_LIGHT = (255, 238, 176, 255)
GOLD_DARK = (150, 110, 40, 255)
PARCHMENT = (236, 214, 160, 255)
PARCHMENT_DARK = (176, 140, 84, 255)
AMBER = (240, 150, 40, 255)
AMBER_LIGHT = (255, 214, 120, 255)
ICE = (190, 236, 255, 255)
ICE_DARK = (90, 160, 210, 255)
PALE_GREEN = (170, 255, 170, 255)
GREEN = (80, 190, 110, 255)
GREEN_DARK = (30, 100, 50, 255)
RED = (210, 50, 60, 255)
RED_LIGHT = (255, 140, 140, 255)
BLACK = (24, 18, 28, 255)
WOOD = (140, 92, 54, 255)


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


def eye(d, cx, cy, w, h, white, iris, pupil):
    d.polygon([(cx - w, cy), (cx - w * 0.4, cy - h), (cx + w * 0.4, cy - h), (cx + w, cy), (cx + w * 0.4, cy + h),
               (cx - w * 0.4, cy + h)], fill=white)
    d.ellipse([cx - h, cy - h, cx + h, cy + h], fill=iris)
    if pupil is not None:
        d.point((cx, cy), fill=pupil)


def scroll(d, x0, y0, x1, y1, paper, rod, writing=None):
    """A scroll unrolled from top to bottom: two rods and a sheet between them."""
    d.rectangle([x0, y0 + 1, x1, y1 - 1], fill=paper)
    d.rectangle([x0 - 1, y0, x1 + 1, y0 + 1], fill=rod)
    d.rectangle([x0 - 1, y1 - 1, x1 + 1, y1], fill=rod)
    if writing:
        for y in range(y0 + 3, y1 - 2, 2):
            d.line([(x0 + 1, y), (x1 - 1 - (y % 3), y)], fill=writing)


def book(d, cx, cy, w, h, cover, page, line):
    """An open book seen from above: two pages and a spine."""
    d.polygon([(cx - w, cy - h + 1), (cx, cy - h + 2), (cx + w, cy - h + 1), (cx + w, cy + h), (cx, cy + h + 1),
               (cx - w, cy + h)], fill=cover)
    d.polygon([(cx - w + 1, cy - h + 1), (cx, cy - h + 2), (cx, cy + h - 1), (cx - w + 1, cy + h - 2)], fill=page)
    d.polygon([(cx + w - 1, cy - h + 1), (cx, cy - h + 2), (cx, cy + h - 1), (cx + w - 1, cy + h - 2)], fill=page)
    for i in range(1, h, 2):
        d.line([(cx - w + 2, cy - h + 2 + i), (cx - 2, cy - h + 2 + i)], fill=line)
        d.line([(cx + 2, cy - h + 2 + i), (cx + w - 2, cy - h + 2 + i)], fill=line)


def figure(d, cx, top, s, fill):
    """A standing person: a head and a robed body."""
    d.ellipse([cx - s * 0.5, top, cx + s * 0.5, top + s], fill=fill)
    d.polygon([(cx - s * 0.6, top + s * 1.1), (cx + s * 0.6, top + s * 1.1), (cx + s * 1.1, top + s * 3.4),
               (cx - s * 1.1, top + s * 3.4)], fill=fill)


# ------------------------------------------------------------------ the 32x32 wheel pictures

S = 32
C = 15.5


def wheel_base(inside=(18, 10, 34, 255), edge=(40, 24, 72, 255)):
    """A round frame of starlit silver round a dark violet field."""
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = im.load()
    for y in range(S):
        for x in range(S):
            dd = math.hypot(x - C, y - C)
            if dd <= 15.6:
                if dd > 13.6:
                    light = (x - C) * -0.7 + (y - C) * -0.7
                    px[x, y] = SILVER_LIGHT if light > 9 else SILVER if light > -6 else SILVER_DARK
                elif dd > 12.6:
                    px[x, y] = (70, 40, 110, 255)
                else:
                    t = dd / 12.6
                    px[x, y] = tuple(int(inside[i] * (1 - t) + edge[i] * t) for i in range(3)) + (255,)
    for i, (x, y) in enumerate(((5, 9), (25, 6), (27, 21), (6, 24), (16, 27))):    # a few faint stars in the field
        if math.hypot(x - C, y - C) < 12:
            px[x, y] = (150, 130, 200, 255)
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


STARS_INSIDE = dict(inside=(10, 12, 40, 255), edge=(30, 34, 84, 255))
TALE_INSIDE = dict(inside=(30, 14, 30, 255), edge=(70, 34, 60, 255))
KNOW_INSIDE = dict(inside=(8, 6, 16, 255), edge=(48, 26, 80, 255))


# ---- key 5: Stars

def w_starlight_cage():
    base = wheel_base(**STARS_INSIDE)
    layer, d = canvas(S)
    d.rectangle([9, 10, 22, 25], fill=AMBER)                                  # the amber block
    d.polygon([(9, 10), (13, 6), (26, 6), (22, 10)], fill=AMBER_LIGHT)
    d.polygon([(22, 10), (26, 6), (26, 21), (22, 25)], fill=(200, 110, 30, 255))
    figure(d, 15.5, 13, 3, (120, 60, 20, 255))                                # someone held inside
    star(d, 11, 13, 2, WHITE)
    star(d, 24, 9, 1.6, WHITE)
    return in_frame(base, layer)


def w_stellar_self():
    base = wheel_base(**STARS_INSIDE)
    layer, d = canvas(S)
    for i, (x, y) in enumerate(((8, 22), (11, 19), (14, 17), (17, 14), (20, 12), (23, 9))):
        star(d, x, y, 1.2 + i * 0.35, STAR if i % 2 else ORCHID_LIGHT)         # a body flowing away as stars
    figure(d, 9, 13, 2.6, (150, 140, 210, 255))
    return in_frame(base, layer)


def w_star_fall():
    base = wheel_base(**STARS_INSIDE)
    layer, d = canvas(S)
    for x, y, r in ((11, 9, 2.6), (20, 13, 3.2), (14, 19, 2.2)):
        d.line([(x - 5, y - 6), (x, y)], fill=ORCHID_LIGHT, width=2)          # their trails
        star(d, x, y, r, STAR)
    d.arc([6, 22, 26, 29], 180, 360, fill=ORCHID)                              # where they land
    d.line([(6, 25), (26, 25)], fill=ORCHID_DARK)
    return in_frame(base, layer)


def w_star_guard():
    base = wheel_base(**STARS_INSIDE)
    layer, d = canvas(S)
    d.ellipse([7, 10, 24, 22], outline=ORCHID)
    figure(d, 15.5, 9, 2.6, SILVER)
    for x, y in ((7, 16), (24, 16), (12, 21), (19, 11)):
        star(d, x, y, 2.4, STAR)
    return in_frame(base, layer)


# ---- key 6: Retellings and Prophecy

def w_sleeping_beauty():
    base = wheel_base(**TALE_INSIDE)
    layer, d = canvas(S)
    d.line([(15, 4), (15, 27)], fill=WOOD, width=1)                           # the spindle
    d.ellipse([10, 21, 21, 24], fill=GOLD)
    d.ellipse([12, 11, 19, 18], fill=GOLD_LIGHT)
    d.polygon([(15, 3), (16, 3), (15.5, 0)], fill=GOLD_LIGHT)
    for i, (x, y) in enumerate(((22, 7), (25, 11), (27, 15))):                # z z z
        s = 1 + i
        d.line([(x, y), (x + s, y), (x, y + s), (x + s, y + s)], fill=ORCHID_LIGHT)
    return in_frame(base, layer)


def w_avalon():
    base = wheel_base(**TALE_INSIDE)
    layer, d = canvas(S)
    d.ellipse([5, 20, 26, 27], fill=(150, 200, 230, 255))                     # the lake
    d.pieslice([8, 16, 23, 28], 180, 360, fill=GREEN)                          # the isle
    d.line([(15, 21), (15, 12)], fill=WOOD, width=2)                           # the apple tree
    d.ellipse([9, 6, 22, 15], fill=GREEN)
    for x, y in ((11, 9), (16, 8), (20, 11), (13, 12)):
        d.point((x, y), fill=RED_LIGHT)
    d.line([(4, 18), (27, 18)], fill=(230, 240, 250, 255))                    # mist
    return in_frame(base, layer)


def w_achilles_heel():
    base = wheel_base(**TALE_INSIDE)
    layer, d = canvas(S)
    d.polygon([(12, 4), (17, 4), (17, 19), (25, 22), (25, 26), (12, 26)], fill=PARCHMENT)   # a foot from the side
    d.line([(4, 28), (14, 22)], fill=SILVER_LIGHT, width=1)                   # the arrow
    d.polygon([(15, 21), (12, 21), (14, 24)], fill=SILVER_LIGHT)
    d.line([(4, 28), (6, 25)], fill=RED_LIGHT)
    star(d, 14, 22, 3, GOLD_LIGHT)
    return in_frame(base, layer)


def w_pandoras_box():
    base = wheel_base(**TALE_INSIDE)
    layer, d = canvas(S)
    d.rectangle([7, 17, 24, 27], fill=BLACK)
    d.rectangle([7, 17, 24, 18], fill=GOLD)
    d.rectangle([14, 20, 17, 23], fill=GOLD)
    d.polygon([(7, 16), (24, 16), (26, 9), (9, 9)], fill=(50, 40, 60, 255))    # the lid thrown open
    for x, y, c in ((10, 6, RED_LIGHT), (15, 4, ORCHID_LIGHT), (21, 6, PALE_GREEN), (18, 11, GOLD_LIGHT)):
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=c)                        # the misfortunes escaping
    star(d, 15.5, 13, 2.5, WHITE)                                              # and hope, last
    return in_frame(base, layer)


def w_foresight():
    base = wheel_base(**TALE_INSIDE)
    layer, d = canvas(S)
    eye(d, 15.5, 16, 11, 6, (230, 228, 255, 255), INDIGO_LIGHT, None)
    star(d, 15.5, 16, 3, STAR)
    for x in (8, 23):                                                          # ghosts of what is coming
        d.arc([x - 4, 5, x + 4, 12], 200, 340, fill=(200, 200, 230, 160))
    return in_frame(base, layer)


def w_prophecy_of_ruin():
    base = wheel_base(**TALE_INSIDE)
    layer, d = canvas(S)
    d.ellipse([6, 6, 25, 25], outline=RED_LIGHT)
    for i in range(8):
        a = math.tau * i / 8
        d.line([(15.5 + math.cos(a) * 3, 15.5 + math.sin(a) * 3), (15.5 + math.cos(a) * 9, 15.5 + math.sin(a) * 9)],
               fill=RED if i % 2 else RED_LIGHT)
    d.ellipse([13, 12, 18, 19], fill=STAR)
    d.line([(15.5, 13), (15.5, 18)], fill=INK)
    return in_frame(base, layer)


# ---- key 7: Knowledge

def w_informatization():
    base = wheel_base(**KNOW_INSIDE)
    layer, d = canvas(S)
    for x in range(7, 26, 3):                                                  # a body made of streaming script
        for y in range(5 + (x % 2), 28, 3):
            inside = abs(x - 15.5) < 4 + (y > 12) * 2 and y > 5
            if inside:
                d.point((x, y), fill=ORCHID_LIGHT if (x + y) % 3 else WHITE)
    d.ellipse([12, 4, 19, 11], outline=STAR)
    return in_frame(base, layer)


def w_erase():
    base = wheel_base(**KNOW_INSIDE)
    layer, d = canvas(S)
    for i, x in enumerate(range(7, 25, 4)):
        d.rectangle([x, 13, x + 2, 18], fill=(200, 200, 220, 255 - i * 30))
    d.line([(5, 22), (26, 9)], fill=WHITE, width=2)
    return in_frame(base, layer)


def w_knowledge_overload():
    base = wheel_base(**KNOW_INSIDE)
    layer, d = canvas(S)
    d.ellipse([9, 8, 22, 23], fill=(120, 100, 160, 255))                     # a head
    for i in range(9):                                                         # knowledge pouring into it
        a = math.tau * i / 9
        d.line([(15.5 + math.cos(a) * 13, 15.5 + math.sin(a) * 13), (15.5 + math.cos(a) * 8, 15.5 + math.sin(a) * 8)],
               fill=ORCHID_LIGHT)
    star(d, 15.5, 15.5, 4, WHITE, 6, 0.3)
    return in_frame(base, layer)


def w_lance_of_knowledge():
    base = wheel_base(**KNOW_INSIDE)
    layer, d = canvas(S)
    d.line([(7, 25), (22, 10)], fill=STAR, width=2)
    d.polygon([(21, 8), (27, 4), (24, 11)], fill=WHITE)
    d.line([(6, 22), (9, 26)], fill=ORCHID)
    d.line([(10, 18), (13, 21)], fill=ORCHID_LIGHT)
    return in_frame(base, layer)


def w_condense_knowledge():
    base = wheel_base(**KNOW_INSIDE)
    layer, d = canvas(S)
    d.ellipse([10, 10, 21, 21], fill=GOLD_LIGHT)
    d.ellipse([12, 12, 19, 19], fill=WHITE)
    for i in range(6):                                                         # drawn in from around
        a = math.tau * i / 6 + 0.3
        d.line([(15.5 + math.cos(a) * 13, 15.5 + math.sin(a) * 13), (15.5 + math.cos(a) * 9, 15.5 + math.sin(a) * 9)],
               fill=ORCHID_LIGHT)
    return in_frame(base, layer)


def w_embodiment_of_knowledge():
    base = wheel_base(**KNOW_INSIDE)
    layer, d = canvas(S)
    figure(d, 15.5, 6, 4, (70, 60, 110, 255))
    for y in range(8, 27, 3):                                                  # it fades into lines of script
        d.line([(10 + (y % 2), y), (21 - (y % 3), y)], fill=(170, 150, 220, 200))
    return in_frame(base, layer)


def w_indoctrination():
    base = wheel_base(**KNOW_INSIDE)
    layer, d = canvas(S)
    book(d, 15.5, 18, 8, 5, INDIGO_LIGHT, PARCHMENT, PARCHMENT_DARK)
    for r in (9, 12):
        d.arc([15.5 - r, 18 - r, 15.5 + r, 18 + r], 210, 330, fill=ORCHID_LIGHT)
    return in_frame(base, layer)


def w_divine_kingdom():
    base = wheel_base(**KNOW_INSIDE)
    layer, d = canvas(S)
    for x in (6, 10, 21, 25):                                                  # the shelves of the Knowledge Moor
        d.rectangle([x - 1, 9, x + 1, 26], fill=ORCHID_DARK)
        for y in range(10, 26, 3):
            d.line([(x - 1, y), (x + 1, y)], fill=ORCHID_LIGHT)
    eye(d, 15.5, 13, 5, 3, STAR, INDIGO_LIGHT, None)
    d.rectangle([5, 26, 26, 27], fill=ORCHID)
    return in_frame(base, layer)


WHEEL = {
    'starlight_cage': w_starlight_cage,
    'stellar_self': w_stellar_self,
    'star_fall': w_star_fall,
    'star_guard': w_star_guard,
    'sleeping_beauty': w_sleeping_beauty,
    'avalon': w_avalon,
    'achilles_heel': w_achilles_heel,
    'pandoras_box': w_pandoras_box,
    'foresight': w_foresight,
    'prophecy_of_ruin': w_prophecy_of_ruin,
    'informatization': w_informatization,
    'erase': w_erase,
    'knowledge_overload': w_knowledge_overload,
    'lance_of_knowledge': w_lance_of_knowledge,
    'condense_knowledge': w_condense_knowledge,
    'embodiment_of_knowledge': w_embodiment_of_knowledge,
    'indoctrination': w_indoctrination,
    'divine_kingdom': w_divine_kingdom,
}


# ------------------------------------------------------------------ the 16x16 status pictures

def s(fn):
    img, d = canvas(16)
    fn(d)
    return outlined(img)


# what the Hermit holds

def i_prying(d):
    eye(d, 7.5, 7.5, 7, 4, STAR, INDIGO_LIGHT, None)
    d.point((7, 7), fill=WHITE)
    d.point((8, 8), fill=ORCHID_LIGHT)


def i_dowsing(d):
    d.line([(7, 1), (7, 8)], fill=SILVER)
    d.polygon([(5, 9), (9, 9), (7, 14)], fill=ORCHID_LIGHT)
    d.arc([1, 4, 13, 14], 20, 160, fill=ORCHID_DARK)


def i_sidestep(d):
    d.line([(2, 10), (12, 10)], fill=SILVER_LIGHT)
    d.polygon([(11, 7), (14, 10), (11, 13)], fill=SILVER_LIGHT)
    figure(d, 4, 1, 1.8, (150, 140, 200, 140))
    figure(d, 11, 1, 1.8, ORCHID_LIGHT)


def i_foresight(d):
    eye(d, 7.5, 9, 6.5, 3.5, STAR, INDIGO_LIGHT, None)
    d.arc([2, 1, 13, 8], 200, 340, fill=(220, 220, 250, 200))


def i_star_guard(d):
    d.ellipse([2, 4, 13, 12], outline=ORCHID_DARK)
    for x, y in ((2, 8), (13, 8), (7.5, 3), (7.5, 13)):
        star(d, x, y, 2.2, STAR)


def i_avalon(d):
    d.line([(7, 13), (7, 7)], fill=WOOD)
    d.ellipse([2, 1, 13, 9], fill=GREEN)
    d.point((5, 4), fill=RED_LIGHT)
    d.point((10, 5), fill=RED_LIGHT)
    d.pieslice([1, 11, 14, 17], 180, 360, fill=GREEN_DARK)


def i_informatization(d):
    for x in range(3, 13, 2):
        for y in range(1 + x % 2, 15, 2):
            if abs(x - 7.5) < 3 + (y > 5) * 1.5:
                d.point((x, y), fill=ORCHID_LIGHT if (x + y) % 3 else WHITE)


def i_embodiment(d):
    figure(d, 7.5, 1, 2.6, (90, 80, 140, 200))
    for y in (6, 9, 12):
        d.line([(4, y), (11, y)], fill=(190, 170, 240, 160))


def i_kingdom(d):
    for x in (2, 13):
        d.rectangle([x - 1, 3, x + 1, 14], fill=ORCHID_DARK)
        for y in (5, 8, 11):
            d.point((x, y), fill=ORCHID_LIGHT)
    eye(d, 7.5, 7, 3.5, 2, STAR, INDIGO_LIGHT, None)
    d.rectangle([1, 14, 14, 14], fill=ORCHID)


def i_law_of_three(d):
    for x, y in ((7.5, 2.5), (2.5, 12), (12.5, 12)):
        d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=GOLD_LIGHT)
    d.polygon([(7.5, 4), (3.5, 11), (11.5, 11)], outline=GOLD)


def i_condensed(d):
    d.ellipse([3, 3, 12, 12], fill=GOLD_LIGHT)
    d.ellipse([5, 5, 10, 10], fill=WHITE)
    d.point((1, 1), fill=ORCHID_LIGHT)
    d.point((14, 14), fill=ORCHID_LIGHT)


def i_enlightened(d):
    book(d, 7.5, 9, 6, 4, INDIGO_LIGHT, PARCHMENT, PARCHMENT_DARK)
    d.line([(7, 0), (7, 3)], fill=GOLD_LIGHT)
    d.line([(2, 2), (4, 4)], fill=GOLD_LIGHT)
    d.line([(13, 2), (11, 4)], fill=GOLD_LIGHT)


def i_whisper(d):
    d.arc([2, 2, 13, 13], 120, 420, fill=ORCHID_LIGHT)
    d.arc([5, 5, 10, 10], 120, 420, fill=STAR)
    d.polygon([(2, 12), (5, 10), (5, 14)], fill=ORCHID_LIGHT)


# what is done to others (shown when they are players)

def i_hexed(d):
    for k in range(2):
        pts = [(7.5 + math.cos(-math.pi / 2 + k * math.pi + math.tau * i / 3) * 6.5,
                7.5 + math.sin(-math.pi / 2 + k * math.pi + math.tau * i / 3) * 6.5) for i in range(3)]
        d.polygon(pts, outline=(170, 90, 220, 255))
    d.line([(5, 8), (10, 8)], fill=ORCHID_LIGHT)


def i_asleep(d):
    for i, (x, y) in enumerate(((2, 9), (6, 5), (10, 1))):
        s = 2 + i
        d.line([(x, y), (x + s, y), (x, y + s), (x + s, y + s)], fill=GOLD_LIGHT)


def i_misfortune(d):
    d.rectangle([2, 8, 13, 14], fill=BLACK)
    d.rectangle([2, 8, 13, 8], fill=GOLD)
    d.polygon([(2, 7), (13, 7), (14, 2), (3, 2)], fill=(60, 50, 70, 255))
    d.point((6, 0), fill=RED_LIGHT)
    d.point((10, 1), fill=PALE_GREEN)


def i_doomed(d):
    d.ellipse([1, 1, 14, 14], outline=RED)
    for i in range(8):
        a = math.tau * i / 8
        d.line([(7.5 + math.cos(a) * 2, 7.5 + math.sin(a) * 2), (7.5 + math.cos(a) * 5.5, 7.5 + math.sin(a) * 5.5)],
               fill=RED_LIGHT if i % 2 else RED)
    d.ellipse([6, 6, 9, 9], fill=STAR)


def i_achilles(d):
    d.polygon([(5, 1), (8, 1), (8, 10), (13, 11), (13, 14), (5, 14)], fill=PARCHMENT)
    d.line([(0, 15), (6, 11)], fill=SILVER_LIGHT)
    star(d, 6, 11, 2, GOLD_LIGHT)


def i_understood(d):
    eye(d, 7.5, 7.5, 7, 5, STAR, INDIGO_LIGHT, None)
    d.point((6, 6), fill=WHITE)
    d.point((9, 9), fill=ORCHID_LIGHT)
    d.point((8, 6), fill=GOLD_LIGHT)


def i_overloaded(d):
    d.ellipse([3, 3, 12, 13], fill=(120, 100, 160, 255))
    for x, y in ((1, 3), (14, 4), (2, 13), (13, 12), (7, 0)):
        d.point((x, y), fill=ORCHID_LIGHT)
    star(d, 7.5, 8, 3, WHITE, 6, 0.3)


def i_indoctrinated(d):
    d.ellipse([3, 4, 12, 14], fill=(110, 96, 140, 255))
    for i, x in enumerate((4, 7, 10)):
        d.line([(x, 0), (x + 1, 3)], fill=ORCHID_LIGHT if i % 2 else WHITE)
    d.line([(5, 9), (10, 9)], fill=INK)


def i_frozen(d):
    d.rectangle([5, 0, 10, 15], fill=(190, 236, 255, 170))
    star(d, 7.5, 9, 4, ICE, 6, 0.3)
    d.point((7, 9), fill=WHITE)


def i_paralyzed(d):
    d.line([(7, 0), (7, 15)], fill=PALE_GREEN)
    d.ellipse([3, 6, 12, 15], outline=GREEN)
    d.line([(4, 9), (11, 12)], fill=PALE_GREEN)
    d.line([(4, 12), (11, 9)], fill=PALE_GREEN)


def i_caged(d):
    d.rectangle([2, 2, 13, 14], fill=AMBER)
    d.rectangle([3, 3, 12, 13], fill=AMBER_LIGHT)
    figure(d, 7.5, 4, 1.8, (150, 80, 20, 255))
    d.point((4, 4), fill=WHITE)


# the scroll pouch: one picture per scroll held, in the row beside the hotbar

def i_scroll_freezing(d):
    scroll(d, 3, 1, 12, 14, (220, 244, 255, 255), SILVER, None)
    star(d, 7.5, 7.5, 3.6, ICE_DARK, 6, 0.35)
    d.point((7, 7), fill=WHITE)


def i_scroll_paralysis(d):
    scroll(d, 3, 1, 12, 14, (226, 250, 220, 255), SILVER, None)
    d.polygon([(8, 3), (5, 8), (8, 8), (6, 12), (10, 6), (7, 6)], fill=GREEN)


def i_scroll_inscribed(d):
    scroll(d, 3, 1, 12, 14, PARCHMENT, GOLD_DARK, PARCHMENT_DARK)
    d.ellipse([8, 9, 12, 13], fill=ORCHID)


STATUS = {
    'prying': i_prying,
    'dowsing': i_dowsing,
    'sidestep': i_sidestep,
    'foresight': i_foresight,
    'star_guard': i_star_guard,
    'avalon': i_avalon,
    'informatization': i_informatization,
    'embodiment': i_embodiment,
    'kingdom': i_kingdom,
    'law_of_three': i_law_of_three,
    'condensed': i_condensed,
    'enlightened': i_enlightened,
    'whisper': i_whisper,
    'hexed': i_hexed,
    'asleep': i_asleep,
    'misfortune': i_misfortune,
    'doomed': i_doomed,
    'achilles': i_achilles,
    'understood': i_understood,
    'overloaded': i_overloaded,
    'indoctrinated': i_indoctrinated,
    'frozen': i_frozen,
    'paralyzed': i_paralyzed,
    'caged': i_caged,
    'scroll_freezing': i_scroll_freezing,
    'scroll_paralysis': i_scroll_paralysis,
    'scroll_inscribed': i_scroll_inscribed,
}


def pathway_icon():
    """A pair of cold eyes full of stars."""
    img, d = canvas(16)
    for cx in (3.5, 11.5):
        d.polygon([(cx - 3.5, 8), (cx - 1.5, 6), (cx + 1.5, 6), (cx + 3.5, 8), (cx + 1.5, 10), (cx - 1.5, 10)],
                  fill=(222, 226, 250, 255))                                   # the almond
        d.rectangle([cx - 1, 7, cx + 1, 9], fill=INDIGO)                        # a narrow iris
        d.point((cx, 8), fill=WHITE)                                           # a star in it
    d.line([(1, 4), (6, 5)], fill=ORCHID)                                      # cold, level brows
    d.line([(9, 5), (14, 4)], fill=ORCHID)
    for x, y in ((2, 13), (7, 14), (13, 12)):                                  # faint stars scattered below
        d.point((x, y), fill=ORCHID_LIGHT)
    return outlined(img)


def main(assets, preview=None):
    out = []
    for name, fn in WHEEL.items():
        path = os.path.join(assets, 'textures/gui/ability_menus/knowledge_emperor', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fn().save(path)
        out.append(path)
    for name, fn in STATUS.items():
        path = os.path.join(assets, 'textures/gui/knowledge_emperor', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        s(fn).save(path)
        out.append(path)
    path = os.path.join(assets, 'textures/gui/pathway_icons/knowledge_emperor.png')
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
