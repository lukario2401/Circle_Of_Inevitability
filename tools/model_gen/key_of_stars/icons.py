"""Screen pictures for the Key Of Stars (Door pathway).

python3 tools/model_gen/key_of_stars/icons.py src/main/resources/assets/circle_of_inevitability [preview.png]

  textures/gui/ability_menus/key_of_stars/*.png   32x32, the four wheels (Tricks on key 2, Travel on key 5, Secrets
                                                  and Space on key 6, Starlight on key 7), in a round frame of
                                                  silver-blue starlight
  textures/gui/key_of_stars/*.png                 16x16, the statuses shown above the hotbar, the Recorded Powers'
                                                  pages (recorded, recorded_angel) and Transfiguration's layers left
                                                  (layers_1 .. layers_8)
  textures/gui/pathway_icons/key_of_stars.png     16x16, the Pathway's icon beside the hotbar

Every picture is drawn in flat colors and then outlined automatically, so the shapes stay simple to change.
Replace any of them with a hand-drawn PNG of the same name.
"""
import math
import os
import sys

from PIL import Image, ImageDraw

# ------------------------------------------------------------------ colors
OUTLINE = (10, 8, 26, 255)
STAR = (157, 180, 255, 255)
STAR_LIGHT = (214, 224, 255, 255)
STAR_DARK = (80, 96, 170, 255)
WHITE = (255, 255, 255, 255)
GOLD = (255, 217, 138, 255)
GOLD_DARK = (190, 140, 60, 255)
INDIGO = (52, 52, 128, 255)
INDIGO_LIGHT = (96, 96, 190, 255)
NIGHT = (20, 18, 52, 255)
VIOLET = (122, 90, 216, 255)
VIOLET_LIGHT = (180, 160, 255, 255)
FROST = (190, 236, 255, 255)
GREASE = (220, 200, 120, 255)
RED = (220, 70, 70, 255)
RED_DARK = (130, 30, 30, 255)
PAPER = (232, 236, 252, 255)
INK = (70, 90, 200, 255)
FIGURE = (200, 190, 220, 255)
GREY = (120, 120, 140, 255)


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

def star(d, x, y, r, fill, points=4, inner=0.35, turn=0.0):
    pts = []
    for i in range(points * 2):
        rr = r if i % 2 == 0 else r * inner
        a = -math.pi / 2 + turn + i * math.pi / points
        pts.append((x + math.cos(a) * rr, y + math.sin(a) * rr))
    d.polygon(pts, fill=fill)


def door(d, x0, y0, x1, y1, frame, panel=None, keyhole=True):
    """A small arched door filling x0..x1, y0..y1."""
    w = x1 - x0
    if panel:
        d.rectangle([x0 + 1, y0 + w // 2, x1 - 1, y1], fill=panel)
        d.pieslice([x0 + 1, y0 + 1, x1 - 1, y0 + w - 1], 180, 360, fill=panel)
    d.line([(x0, y1), (x0, y0 + w / 2)], fill=frame)
    d.line([(x1, y1), (x1, y0 + w / 2)], fill=frame)
    d.arc([x0, y0, x1, y0 + w], 180, 360, fill=frame)
    if keyhole:
        kx, ky = x0 + w * 0.7, y0 + (y1 - y0) * 0.55
        d.point((kx, ky), fill=GOLD)
        d.point((kx, ky + 1), fill=GOLD)


def figure(d, cx, top, s, fill):
    """A standing person: a head and a body."""
    d.ellipse([cx - s * 0.5, top, cx + s * 0.5, top + s], fill=fill)
    d.polygon([(cx - s * 0.6, top + s * 1.1), (cx + s * 0.6, top + s * 1.1), (cx + s * 0.9, top + s * 3.2),
               (cx - s * 0.9, top + s * 3.2)], fill=fill)


def key(d, x, y, s, fill, bow=None):
    """A key lying from its bow at x, y toward the lower right, 's' long."""
    bow = bow or fill
    d.ellipse([x - s * 0.22, y - s * 0.22, x + s * 0.22, y + s * 0.22], outline=bow, width=max(1, int(s / 10)))
    ex, ey = x + s * 0.7, y + s * 0.7
    d.line([(x + s * 0.15, y + s * 0.15), (ex, ey)], fill=fill, width=max(1, int(s / 10)))
    d.line([(ex - s * 0.2, ey), (ex - s * 0.08, ey + s * 0.14)], fill=fill)
    d.line([(ex - s * 0.38, ey - s * 0.18), (ex - s * 0.26, ey - s * 0.04)], fill=fill)


def spiral(d, cx, cy, r, turns, fill, width=1, arms=1, start=0.0):
    for arm in range(arms):
        pts = []
        for i in range(40):
            t = i / 39
            a = start + arm * math.tau / arms + t * turns * math.tau
            pts.append((cx + math.cos(a) * r * t, cy + math.sin(a) * r * t))
        d.line(pts, fill=fill, width=width)


# ------------------------------------------------------------------ the 32x32 wheel pictures

S = 32
C = 15.5


def wheel_base(inside=(14, 14, 40, 255), edge=(40, 44, 100, 255)):
    """A round frame of silver-blue starlight round a dark field, with a gold glint at the top left."""
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = im.load()
    for y in range(S):
        for x in range(S):
            dd = math.hypot(x - C, y - C)
            if dd <= 15.6:
                if dd > 13.6:
                    light = (x - C) * -0.7 + (y - C) * -0.7
                    px[x, y] = STAR_LIGHT if light > 9 else STAR if light > -6 else STAR_DARK
                elif dd > 12.6:
                    px[x, y] = (24, 24, 60, 255)
                else:
                    t = dd / 12.6
                    px[x, y] = tuple(int(inside[i] * (1 - t) + edge[i] * t) for i in range(3)) + (255,)
    px[6, 6] = GOLD
    px[7, 5] = GOLD
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


TRICKS = dict(inside=(30, 10, 40, 255), edge=(80, 34, 96, 255))           # a stage magician's plum
TRAVEL = dict(inside=(10, 16, 50, 255), edge=(36, 56, 130, 255))          # the blue of the Spirit World
SECRETS = dict(inside=(18, 8, 34, 255), edge=(64, 40, 110, 255))          # violet: hidden space
STARLIGHT = dict(inside=(8, 8, 24, 255), edge=(60, 50, 40, 255))          # the night, edged with gold


# ---- key 2: Tricks

def w_flash():
    base = wheel_base(**TRICKS)
    layer, d = canvas(S)
    star(d, 15.5, 15.5, 11, WHITE, 8, 0.3)
    star(d, 15.5, 15.5, 6, (255, 250, 210, 255), 4, 0.4, math.pi / 4)
    d.ellipse([12, 12, 19, 19], fill=WHITE)
    return in_frame(base, layer)


def w_black_screen():
    base = wheel_base(**TRICKS)
    layer, d = canvas(S)
    d.rectangle([7, 7, 25, 8], fill=GREY)                                   # the rail
    for x in range(8, 25, 3):                                               # the folds
        d.rectangle([x, 9, x + 2, 25], fill=(26, 22, 60, 255) if (x // 3) % 2 else (40, 36, 86, 255))
    figure(d, 26, 12, 2.4, FIGURE)                                          # someone slipping out behind it
    return in_frame(base, layer)


def w_loud_noise():
    base = wheel_base(**TRICKS)
    layer, d = canvas(S)
    star(d, 11, 16, 6, (255, 200, 120, 255), 8, 0.45)
    d.ellipse([9, 14, 13, 18], fill=WHITE)
    for r in (8, 12):                                                       # waves of the bang
        d.arc([11 - r, 16 - r, 11 + r, 16 + r], -50, 50, fill=STAR_LIGHT, width=2)
    return in_frame(base, layer)


def w_frost_ray():
    base = wheel_base(**TRICKS)
    layer, d = canvas(S)
    d.line([(6, 25), (22, 9)], fill=FROST, width=3)
    d.line([(6, 25), (22, 9)], fill=WHITE, width=1)
    cx, cy = 23, 8                                                          # a snowflake where it strikes
    for a in range(3):
        ang = a * math.pi / 3
        dx, dy = math.cos(ang) * 4, math.sin(ang) * 4
        d.line([(cx - dx, cy - dy), (cx + dx, cy + dy)], fill=WHITE)
    return in_frame(base, layer)


def w_gale():
    base = wheel_base(**TRICKS)
    layer, d = canvas(S)
    for i, y in enumerate((10, 15, 20)):
        d.arc([4, y - 6, 22, y + 4], 200, 340, fill=STAR_LIGHT if i % 2 else WHITE, width=2)
        d.line([(13, y - 1), (26 - i * 2, y - 1)], fill=STAR_LIGHT)
    d.line([(20, 24), (26, 22)], fill=GOLD, width=1)                        # an arrow blown aside
    return in_frame(base, layer)


def w_tumble():
    base = wheel_base(**TRICKS)
    layer, d = canvas(S)
    d.ellipse([5, 18, 27, 27], fill=GREASE)
    spiral(d, 16, 22.5, 9, 1.4, (255, 240, 180, 255), 1)
    figure(d, 16, 4, 3, FIGURE)                                             # someone slipping, tilted
    d.line([(11, 17), (8, 20)], fill=FIGURE, width=2)
    return in_frame(base, layer)


def w_escape_trick():
    base = wheel_base(**TRICKS)
    layer, d = canvas(S)
    door(d, 5, 9, 13, 25, STAR_DARK, (36, 30, 70, 255), False)              # the crumbling door left behind
    d.point((8, 14), fill=(0, 0, 0, 0))
    d.line([(7, 18), (10, 20)], fill=(10, 8, 26, 255))
    for y in (13, 17, 21):
        d.line([(15, y), (21, y)], fill=STAR_LIGHT)
    figure(d, 24, 9, 2.6, FIGURE)
    return in_frame(base, layer)


# ---- key 5: Travel

def w_travelers_door():
    base = wheel_base(**TRAVEL)
    layer, d = canvas(S)
    door(d, 4, 8, 13, 25, STAR_LIGHT, INDIGO)
    door(d, 18, 8, 27, 25, STAR_LIGHT, INDIGO)
    for x in (14, 16):
        d.point((x, 16), fill=STAR_LIGHT)
    star(d, 22.5, 16, 3, WHITE, 4, 0.3)
    return in_frame(base, layer)


def w_blink():
    base = wheel_base(**TRAVEL)
    layer, d = canvas(S)
    figure(d, 9, 9, 3, (110, 120, 170, 160))                                # where you were
    figure(d, 23, 9, 3, FIGURE)                                             # where you are
    for x in (13, 15, 17):
        d.point((x, 15), fill=STAR_LIGHT)
    star(d, 16, 21, 3, WHITE, 4, 0.3)
    return in_frame(base, layer)


def w_invisible_hand():
    base = wheel_base(**TRAVEL)
    layer, d = canvas(S)
    d.rectangle([6, 13, 14, 21], fill=(170, 190, 255, 255))                 # palm
    for i in range(4):
        d.rectangle([14, 13 + i * 2, 21 - abs(i - 1), 13 + i * 2], fill=(170, 190, 255, 255))
    d.rectangle([8, 10, 9, 13], fill=(170, 190, 255, 255))                  # thumb
    d.ellipse([22, 14, 27, 19], fill=GOLD)                                  # what it grabs
    return in_frame(base, layer)


def w_star_anchor():
    base = wheel_base(**TRAVEL)
    layer, d = canvas(S)
    star(d, 15.5, 15.5, 11, STAR_LIGHT, 4, 0.2)
    star(d, 15.5, 15.5, 7, STAR, 4, 0.25, math.pi / 4)
    door(d, 13, 12, 18, 19, GOLD, NIGHT, False)
    return in_frame(base, layer)


def w_wandering():
    base = wheel_base(**TRAVEL)
    layer, d = canvas(S)
    pts = [(5, 25), (10, 21), (15, 19), (20, 14), (25, 8)]
    d.line(pts, fill=STAR_DARK, width=4)
    d.line(pts, fill=STAR_LIGHT, width=2)
    star(d, 25, 8, 4, WHITE, 4, 0.35)
    for x, y in ((8, 12), (20, 24), (13, 9)):
        d.point((x, y), fill=WHITE)
    return in_frame(base, layer)


# ---- key 6: Secrets and Space

def w_space_concealment():
    base = wheel_base(**SECRETS)
    layer, d = canvas(S)
    d.ellipse([5, 5, 26, 26], outline=STAR_LIGHT, width=2)
    figure(d, 15.5, 10, 3, (140, 130, 180, 255))
    door(d, 22, 12, 27, 21, GOLD, None, False)                              # the one Door in the shell
    return in_frame(base, layer)


def w_transfiguration():
    base = wheel_base(**SECRETS)
    layer, d = canvas(S)
    for i, col in enumerate((STAR_DARK, STAR, STAR_LIGHT)):
        o = i * 3
        door(d, 5 + o, 6 + o, 17 + o, 26, col, None, False)
    star(d, 23, 9, 3, WHITE, 4, 0.3)
    return in_frame(base, layer)


def w_exile():
    base = wheel_base(**SECRETS)
    layer, d = canvas(S)
    door(d, 6, 6, 18, 26, STAR_LIGHT, NIGHT, False)
    spiral(d, 12, 18, 5, 1.3, VIOLET_LIGHT, 1, 2)
    figure(d, 23, 11, 2.4, FIGURE)
    d.line([(19, 17), (21, 16)], fill=VIOLET_LIGHT)
    return in_frame(base, layer)


def w_pocket_space():
    base = wheel_base(**SECRETS)
    layer, d = canvas(S)
    door(d, 9, 5, 22, 26, GOLD, INDIGO, False)
    d.rectangle([12, 15, 19, 21], fill=(150, 110, 60, 255))                 # a chest waiting inside
    d.line([(12, 17), (19, 17)], fill=GOLD_DARK)
    d.point((15, 18), fill=GOLD)
    return in_frame(base, layer)


def w_space_tearing():
    base = wheel_base(**SECRETS)
    layer, d = canvas(S)
    pts = [(16, 4), (13, 10), (18, 14), (12, 19), (17, 23), (14, 28)]
    d.line(pts, fill=WHITE, width=3)
    d.line(pts, fill=NIGHT, width=1)
    for p in pts[1:-1]:
        d.point((p[0] + 3, p[1]), fill=VIOLET_LIGHT)
        d.point((p[0] - 3, p[1] + 1), fill=STAR_LIGHT)
    return in_frame(base, layer)


# ---- key 7: Starlight

def w_toy_box():
    base = wheel_base(**STARLIGHT)
    layer, d = canvas(S)
    d.rectangle([9, 13, 20, 22], fill=(200, 70, 70, 255))                   # a little tin toy
    d.rectangle([9, 16, 20, 17], fill=GOLD)
    d.ellipse([11, 7, 18, 13], fill=(200, 70, 70, 255))
    d.rectangle([11, 22, 12, 25], fill=GREY)
    d.rectangle([17, 22, 18, 25], fill=GREY)
    d.ellipse([21, 13, 25, 17], outline=GOLD)                               # its wind-up key
    d.ellipse([21, 18, 25, 22], outline=GOLD)
    d.line([(20, 17), (22, 17)], fill=GOLD)
    return in_frame(base, layer)


def w_replication():
    base = wheel_base(**STARLIGHT)
    layer, d = canvas(S)
    d.rectangle([5, 9, 14, 22], fill=PAPER)
    d.rectangle([17, 9, 26, 22], fill=PAPER)
    for y in (12, 15, 18):
        d.line([(7, y), (12, y)], fill=INK)
        d.line([(19, y), (24, y)], fill=STAR_DARK)
    d.line([(13, 25), (18, 25)], fill=GOLD)
    d.polygon([(18, 23), (21, 25), (18, 27)], fill=GOLD)
    return in_frame(base, layer)


def w_space_fragmentation():
    base = wheel_base(**STARLIGHT)
    layer, d = canvas(S)
    d.ellipse([5, 9, 26, 22], outline=(120, 120, 170, 255))
    for i, col in enumerate(((240, 120, 90, 255), (120, 200, 255, 255), (250, 210, 110, 255),
                             (170, 130, 250, 255), (120, 230, 160, 255))):
        a = math.tau * i / 5 + 0.3
        x, y = 15.5 + math.cos(a) * 10, 15.5 + math.sin(a) * 6
        d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=col)
    star(d, 15.5, 15.5, 3, WHITE, 4, 0.3)
    return in_frame(base, layer)


def w_starlight_seal():
    base = wheel_base(**STARLIGHT)
    layer, d = canvas(S)
    d.rectangle([9, 15, 22, 25], fill=GOLD)                                 # a padlock
    d.arc([11, 7, 20, 19], 180, 360, fill=STAR_LIGHT, width=2)
    d.line([(11, 13), (11, 15)], fill=STAR_LIGHT, width=2)
    d.line([(20, 13), (20, 15)], fill=STAR_LIGHT, width=2)
    star(d, 15.5, 20, 3, WHITE, 4, 0.3)
    for x, y in ((6, 8), (25, 10), (5, 22)):
        d.point((x, y), fill=STAR_LIGHT)
    return in_frame(base, layer)


def w_labyrinth():
    base = wheel_base(**STARLIGHT)
    layer, d = canvas(S)
    for r, gap in ((10, 40), (7, 200), (4, 300)):
        d.arc([15.5 - r, 15.5 - r, 15.5 + r, 15.5 + r], gap + 20, gap + 340, fill=STAR_LIGHT)
    d.line([(15, 5), (15, 8)], fill=STAR_LIGHT)
    d.line([(22, 15), (25, 15)], fill=STAR_LIGHT)
    d.point((15, 15), fill=GOLD)
    d.point((16, 16), fill=GOLD)
    return in_frame(base, layer)


def w_alternate_world():
    base = wheel_base(**STARLIGHT)
    layer, d = canvas(S)
    d.rectangle([5, 19, 26, 26], fill=(30, 28, 80, 255))                    # the ground of the kingdom
    for x in (6, 12, 18, 23):
        door(d, x, 13, x + 4, 22, STAR_LIGHT, INDIGO, False)
    for x, y in ((8, 7), (15, 5), (22, 8), (12, 10), (19, 11)):
        d.point((x, y), fill=WHITE)
    star(d, 15.5, 7, 2, GOLD, 4, 0.3)
    return in_frame(base, layer)


def w_omnipresence():
    base = wheel_base(**STARLIGHT)
    layer, d = canvas(S)
    for i in range(6):
        a = -math.pi / 2 + math.tau * i / 6
        figure(d, 15.5 + math.cos(a) * 9, 13 + math.sin(a) * 8 - 3, 1.6, STAR_DARK)
    figure(d, 15.5, 10, 2.4, FIGURE)
    return in_frame(base, layer)


def w_close_the_door():
    base = wheel_base(**STARLIGHT)
    layer, d = canvas(S)
    door(d, 8, 5, 23, 26, STAR_LIGHT, INDIGO, False)
    d.rectangle([5, 15, 26, 17], fill=GOLD)                                 # the bar across
    d.rectangle([14, 18, 17, 21], fill=GOLD_DARK)
    return in_frame(base, layer)


WHEEL = {
    'flash': w_flash,
    'black_screen': w_black_screen,
    'loud_noise': w_loud_noise,
    'frost_ray': w_frost_ray,
    'gale': w_gale,
    'tumble': w_tumble,
    'escape_trick': w_escape_trick,
    'travelers_door': w_travelers_door,
    'blink': w_blink,
    'invisible_hand': w_invisible_hand,
    'star_anchor': w_star_anchor,
    'wandering': w_wandering,
    'space_concealment': w_space_concealment,
    'transfiguration': w_transfiguration,
    'exile': w_exile,
    'pocket_space': w_pocket_space,
    'space_tearing': w_space_tearing,
    'toy_box': w_toy_box,
    'replication': w_replication,
    'space_fragmentation': w_space_fragmentation,
    'starlight_seal': w_starlight_seal,
    'labyrinth': w_labyrinth,
    'alternate_world': w_alternate_world,
    'omnipresence': w_omnipresence,
    'close_the_door': w_close_the_door,
}


# ------------------------------------------------------------------ the 16x16 status pictures

def s(fn):
    img, d = canvas(16)
    fn(d)
    return outlined(img)


# what the Key Of Stars Beyonder holds

def i_interference(d):
    for r, a0 in ((6, 20), (4, 200)):
        d.arc([7.5 - r, 7.5 - r, 7.5 + r, 7.5 + r], a0, a0 + 230, fill=STAR_LIGHT)
    figure(d, 7.5, 4, 2, (140, 130, 180, 255))


def i_record(d):
    d.rectangle([1, 4, 7, 13], fill=PAPER)
    d.rectangle([8, 4, 14, 13], fill=PAPER)
    for y in (6, 8, 10):
        d.line([(2, y), (6, y)], fill=INK)
    d.line([(13, 1), (10, 9)], fill=WHITE)
    d.point((10, 10), fill=GOLD)


def i_wandering(d):
    d.line([(1, 14), (6, 10), (10, 7), (14, 2)], fill=STAR_LIGHT, width=2)
    star(d, 13, 3, 3, WHITE, 4, 0.35)


def i_space_concealment(d):
    d.ellipse([1, 1, 14, 14], outline=STAR_LIGHT)
    figure(d, 7.5, 4, 2.2, (140, 130, 180, 255))
    d.line([(14, 6), (14, 10)], fill=GOLD)


def i_transfiguration(d):
    for i, col in enumerate((STAR_DARK, STAR, STAR_LIGHT)):
        door(d, 2 + i * 2, 2 + i * 2, 9 + i * 2, 14, col, None, False)


def i_omnipresence(d):
    for i in range(6):
        a = math.tau * i / 6
        d.point((7.5 + math.cos(a) * 6, 7.5 + math.sin(a) * 6), fill=STAR_LIGHT)
    figure(d, 7.5, 3, 2.2, FIGURE)


def i_alternate_world(d):
    d.rectangle([1, 11, 14, 14], fill=(30, 28, 80, 255))
    for x in (2, 7, 12):
        door(d, x - 1, 5, x + 2, 12, STAR_LIGHT, INDIGO, False)
    d.point((4, 2), fill=WHITE)
    d.point((11, 1), fill=WHITE)


def i_labyrinth(d):
    for r, gap in ((6, 40), (3, 220)):
        d.arc([7.5 - r, 7.5 - r, 7.5 + r, 7.5 + r], gap + 30, gap + 330, fill=STAR_LIGHT)
    d.point((7, 7), fill=GOLD)


def i_sealed_ally(d):
    d.ellipse([1, 1, 14, 14], outline=GOLD)
    figure(d, 7.5, 3, 2.2, FIGURE)
    star(d, 12, 12, 2, WHITE, 4, 0.3)


def i_quickened(d):
    figure(d, 9, 3, 2.2, FIGURE)
    for y in (6, 9, 12):
        d.line([(1, y), (4, y)], fill=STAR_LIGHT)


def i_unbound(d):
    d.arc([3, 1, 12, 10], 180, 360, fill=STAR_LIGHT, width=2)               # an open lock
    d.line([(12, 5), (12, 7)], fill=STAR_LIGHT, width=2)
    d.rectangle([2, 8, 11, 14], fill=GOLD)
    d.point((6, 11), fill=OUTLINE)


def i_dimension_sight(d):
    d.ellipse([2, 4, 13, 11], fill=WHITE)
    d.ellipse([5, 5, 10, 10], fill=STAR_DARK)
    star(d, 7.5, 7.5, 2, STAR_LIGHT, 4, 0.3)


# what is done to others (when they are players)

def i_starlit(d):
    star(d, 7.5, 7.5, 7, STAR_LIGHT, 4, 0.25)
    star(d, 7.5, 7.5, 3, WHITE, 4, 0.35, math.pi / 4)


def i_sealed(d):
    d.arc([4, 1, 11, 9], 180, 360, fill=STAR_LIGHT, width=2)
    d.rectangle([2, 7, 13, 14], fill=GOLD)
    star(d, 7.5, 10.5, 2, WHITE, 4, 0.3)


def i_exiled(d):
    door(d, 3, 1, 12, 14, STAR_LIGHT, NIGHT, False)
    spiral(d, 7.5, 9, 3.5, 1.2, VIOLET_LIGHT, 1, 2)


def i_toy(d):
    d.rectangle([3, 6, 10, 12], fill=(200, 70, 70, 255))
    d.ellipse([4, 2, 9, 6], fill=(200, 70, 70, 255))
    d.ellipse([11, 5, 14, 8], outline=GOLD)
    d.ellipse([11, 9, 14, 12], outline=GOLD)


def i_blinded(d):
    star(d, 7.5, 7.5, 7, WHITE, 8, 0.35)
    d.line([(3, 8), (12, 8)], fill=OUTLINE)


def i_slipping(d):
    d.ellipse([1, 10, 14, 14], fill=GREASE)
    figure(d, 9, 1, 2.2, FIGURE)
    d.line([(6, 8), (3, 10)], fill=FIGURE)


def i_frozen(d):
    for a in range(3):
        ang = a * math.pi / 3
        dx, dy = math.cos(ang) * 6, math.sin(ang) * 6
        d.line([(7.5 - dx, 7.5 - dy), (7.5 + dx, 7.5 + dy)], fill=FROST, width=2)
    d.point((7, 7), fill=WHITE)


def i_deafened(d):
    d.ellipse([3, 3, 9, 12], fill=(220, 190, 170, 255))                     # an ear
    d.arc([5, 5, 8, 10], 270, 90, fill=RED_DARK)
    d.line([(11, 3), (14, 6)], fill=STAR_LIGHT)
    d.line([(11, 8), (14, 8)], fill=STAR_LIGHT)
    d.line([(11, 13), (14, 10)], fill=STAR_LIGHT)


def i_lost(d):
    for r, gap in ((6, 120), (3, 300)):
        d.arc([7.5 - r, 7.5 - r, 7.5 + r, 7.5 + r], gap + 30, gap + 330, fill=STAR_LIGHT)
    d.point((7, 6), fill=RED)
    d.point((8, 9), fill=RED)


def i_doors_closed(d):
    door(d, 3, 1, 12, 14, STAR_LIGHT, INDIGO, False)
    d.rectangle([1, 7, 14, 8], fill=GOLD)


def i_storm(d):
    spiral(d, 7.5, 7.5, 7, 1.4, VIOLET_LIGHT, 1, 3)
    d.point((7, 7), fill=WHITE)


def i_banished(d):
    door(d, 3, 1, 12, 14, VIOLET_LIGHT, NIGHT, False)
    star(d, 7.5, 8, 2, WHITE, 4, 0.3)


# the Recorded Powers' pages (in the row right of the hotbar)

def i_recorded(d):
    d.polygon([(3, 1), (13, 2), (13, 14), (2, 13)], fill=PAPER)
    for y in (4, 6, 8):
        d.line([(4, y), (11, y)], fill=INK)
    star(d, 10, 11, 2.4, GOLD, 4, 0.35)


def i_recorded_angel(d):
    d.polygon([(3, 1), (13, 2), (13, 14), (2, 13)], fill=(255, 236, 190, 255))
    d.line([(3, 1), (13, 2), (13, 14), (2, 13), (3, 1)], fill=GOLD)
    for y in (4, 6, 8):
        d.line([(4, y), (11, y)], fill=(170, 120, 50, 255))
    star(d, 10, 11, 2.6, WHITE, 4, 0.35)


def layers(n):
    """Transfiguration's layers left: a Door with one bright pip for every layer still standing (up to 8)."""
    def draw(d):
        door(d, 1, 2, 9, 14, STAR_LIGHT, INDIGO, False)
        for i in range(n):
            x = 11 + (i // 4) * 3
            y = 2 + (i % 4) * 3
            d.rectangle([x, y, x + 1, y + 1], fill=GOLD)
    return draw


STATUS = {
    # what the Beyonder holds
    'interference': i_interference,
    'record': i_record,
    'wandering': i_wandering,
    'space_concealment': i_space_concealment,
    'transfiguration': i_transfiguration,
    'omnipresence': i_omnipresence,
    'alternate_world': i_alternate_world,
    'labyrinth': i_labyrinth,
    'sealed_ally': i_sealed_ally,
    'quickened': i_quickened,
    'unbound': i_unbound,
    'dimension_sight': i_dimension_sight,
    # what is done to others
    'starlit': i_starlit,
    'sealed': i_sealed,
    'exiled': i_exiled,
    'toy': i_toy,
    'blinded': i_blinded,
    'slipping': i_slipping,
    'frozen': i_frozen,
    'deafened': i_deafened,
    'lost': i_lost,
    'doors_closed': i_doors_closed,
    'storm': i_storm,
    'banished': i_banished,
    # the Recorded Powers
    'recorded': i_recorded,
    'recorded_angel': i_recorded_angel,
}
for _n in range(1, 9):
    STATUS[f'layers_{_n}'] = layers(_n)


def pathway_icon():
    """A key of stars: a gold key whose bow is a four-pointed star of light."""
    img, d = canvas(16)
    d.line([(7, 8), (13, 14)], fill=GOLD, width=2)
    d.line([(11, 14), (12, 12)], fill=GOLD)
    d.line([(13, 12), (14, 11)], fill=GOLD)
    star(d, 5.5, 5.5, 5.5, STAR_LIGHT, 4, 0.3)
    d.point((5, 5), fill=WHITE)
    d.point((6, 5), fill=WHITE)
    d.point((5, 6), fill=WHITE)
    d.point((6, 6), fill=WHITE)
    return outlined(img)


def main(assets, preview=None):
    out = []
    for name, fn in WHEEL.items():
        path = os.path.join(assets, 'textures/gui/ability_menus/key_of_stars', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fn().save(path)
        out.append(path)
    for name, fn in STATUS.items():
        path = os.path.join(assets, 'textures/gui/key_of_stars', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        s(fn).save(path)
        out.append(path)
    path = os.path.join(assets, 'textures/gui/pathway_icons/key_of_stars.png')
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
