"""Screen pictures for the Abomination (Chained pathway).

python3 tools/model_gen/abomination/icons.py src/main/resources/assets/circle_of_inevitability [preview.png]

  textures/gui/ability_menus/abomination/*.png   32x32, the four wheels (Zombie on key 4, Wraith on key 5, Chains on
                                                 key 6, Curses on key 7), in a round frame of black iron and purple
  textures/gui/abomination/*.png                 16x16, the statuses shown above the hotbar
  textures/gui/pathway_icons/abomination.png     16x16, the Pathway's icon beside the hotbar

Every picture is drawn in flat colors and then outlined automatically (the outline helper is borrowed from
../hand_of_god/icons.py), so the shapes stay simple to change. Replace any of them with a hand-drawn PNG of the
same name.
"""
import importlib.util
import math
import os
import sys

from PIL import Image

_here = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location('hog_icons', os.path.join(_here, '..', 'hand_of_god', 'icons.py'))
_hog = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_hog)

canvas = _hog.canvas
OUTLINE = (16, 8, 20, 255)


def outlined(layer):
    return _hog.outlined(layer, OUTLINE)


# ------------------------------------------------------------------ colors
PURPLE = (108, 48, 130, 255)
PURPLE_LIGHT = (190, 130, 230, 255)
PURPLE_DARK = (54, 20, 70, 255)
IRON = (58, 52, 66, 255)
IRON_LIGHT = (120, 108, 136, 255)
BLOOD = (170, 16, 30, 255)
BLOOD_LIGHT = (240, 80, 90, 255)
BLOOD_DARK = (90, 6, 14, 255)
MOON = (232, 228, 214, 255)
MOON_DARK = (170, 164, 150, 255)
ICE = (150, 210, 255, 255)
ICE_LIGHT = (220, 244, 255, 255)
FUR = (60, 76, 64, 255)
FUR_LIGHT = (120, 140, 124, 255)
WOLF_EYE = (220, 255, 120, 255)
ROT = (110, 128, 86, 255)
ROT_DARK = (60, 74, 46, 255)
BONE = (226, 220, 196, 255)
GHOST = (200, 224, 250, 255)
GHOST_DARK = (110, 140, 190, 255)
BANDAGE = (214, 190, 110, 255)
BANDAGE_DARK = (140, 116, 50, 255)
THORN = (84, 40, 100, 255)
ROSE = (230, 24, 50, 255)
ROSE_LIGHT = (255, 120, 130, 255)
LEAF = (40, 110, 50, 255)
WOOD = (74, 46, 30, 255)
WOOD_LIGHT = (120, 80, 52, 255)
CLOTH = (150, 124, 96, 255)
MIST = (190, 170, 180, 255)
GREEN_GLOW = (120, 230, 120, 255)
BLACK = (8, 6, 10, 255)
WHITE = (255, 255, 255, 255)


# ------------------------------------------------------------------ shapes shared by the icons

def link(d, cx, cy, w, h, color, light=None, flat=False):
    """One chain link, an upright oval w x h; flat = seen edge on (a bar)."""
    if flat:
        d.rectangle([cx - 0.5, cy - h / 2, cx + 0.5, cy + h / 2], fill=color)
    else:
        d.rounded_rectangle([cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2], radius=w / 2, outline=color, width=1)
        if light:
            d.point((cx - w / 2 + 1, cy - h / 4), fill=light)


def chain_line(d, x0, y0, x1, y1, color, light=None, step=3.0, size=2.0):
    """Links along a line, alternating oval and edge on."""
    length = math.hypot(x1 - x0, y1 - y0)
    n = max(1, int(length / step))
    for i in range(n + 1):
        t = i / n
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        if i % 2 == 0:
            d.ellipse([x - size, y - size, x + size, y + size], outline=color)
            if light:
                d.point((x - size + 1, y), fill=light)
        else:
            d.point((x, y), fill=color)


def thorns(d, pts, color, every=2):
    """A vine along points with a thorn sticking out every 'every' points."""
    d.line(pts, fill=color, width=1)
    for i, (x, y) in enumerate(pts[1:-1], 1):
        if i % every == 0:
            px, py = pts[i - 1]
            dx, dy = x - px, y - py
            n = math.hypot(dx, dy) or 1
            side = 1 if i % 4 == 0 else -1
            d.point((x - dy / n * 1.5 * side, y + dx / n * 1.5 * side), fill=color)


def skull(d, cx, cy, s, bone, hole):
    d.ellipse([cx - s, cy - s, cx + s, cy + s * 0.8], fill=bone)
    d.rectangle([cx - s * 0.55, cy + s * 0.5, cx + s * 0.55, cy + s * 1.2], fill=bone)
    d.rectangle([cx - s * 0.6, cy - s * 0.2, cx - s * 0.15, cy + s * 0.25], fill=hole)
    d.rectangle([cx + s * 0.15, cy - s * 0.2, cx + s * 0.6, cy + s * 0.25], fill=hole)


def eye(d, cx, cy, w, h, white, iris, pupil):
    d.polygon([(cx - w, cy), (cx - w * 0.4, cy - h), (cx + w * 0.4, cy - h), (cx + w, cy), (cx + w * 0.4, cy + h),
               (cx - w * 0.4, cy + h)], fill=white)
    d.ellipse([cx - h, cy - h, cx + h, cy + h], fill=iris)
    d.line([(cx, cy - h + 1), (cx, cy + h - 1)], fill=pupil)


def moon(d, cx, cy, r, face, shade, mad=False):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=face)
    d.ellipse([cx + r * 0.1, cy + r * 0.2, cx + r * 0.5, cy + r * 0.6], fill=shade)
    if mad:
        d.point((cx - r * 0.4, cy - r * 0.3), fill=BLOOD)
        d.line([(cx - r * 0.5, cy + r * 0.3), (cx, cy + r * 0.55), (cx + r * 0.5, cy + r * 0.2)], fill=BLACK)


def coffin_shape(d, cx, top, w, h, fill, edge=None):
    pts = [(cx - w * 0.35, top), (cx + w * 0.35, top), (cx + w * 0.5, top + h * 0.25), (cx + w * 0.3, top + h),
           (cx - w * 0.3, top + h), (cx - w * 0.5, top + h * 0.25)]
    d.polygon(pts, fill=fill)
    if edge:
        d.line(pts + [pts[0]], fill=edge)


def rose(d, cx, cy, r, petal, light):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=petal)
    d.arc([cx - r * 0.6, cy - r * 0.6, cx + r * 0.6, cy + r * 0.6], 180, 450, fill=light)
    d.point((cx, cy), fill=light)


def ghost(d, cx, top, s, body, dark):
    d.ellipse([cx - s, top, cx + s, top + s * 2], fill=body)
    d.polygon([(cx - s, top + s), (cx + s, top + s), (cx + s * 0.6, top + s * 3.2), (cx + s * 0.1, top + s * 2.6),
               (cx - s * 0.3, top + s * 3.4), (cx - s * 0.8, top + s * 2.8)], fill=body)
    d.rectangle([cx - s * 0.55, top + s * 0.7, cx - s * 0.2, top + s * 1.2], fill=dark)
    d.rectangle([cx + s * 0.2, top + s * 0.7, cx + s * 0.55, top + s * 1.2], fill=dark)
    d.ellipse([cx - s * 0.2, top + s * 1.4, cx + s * 0.2, top + s * 2.0], fill=dark)


def claw_hand(d, cx, bottom, s, skin, nail):
    """A hand reaching up out of the ground: a forearm and four fingers hooked."""
    d.rectangle([cx - s * 0.5, bottom - s * 1.6, cx + s * 0.5, bottom], fill=skin)
    d.rectangle([cx - s * 0.8, bottom - s * 2.4, cx + s * 0.8, bottom - s * 1.5], fill=skin)
    for i in range(4):
        x = cx - s * 0.8 + i * s * 0.53
        d.line([(x + 0.5, bottom - s * 2.4), (x + 0.5, bottom - s * 3.2)], fill=skin, width=1)
        d.point((x + 0.5, bottom - s * 3.3), fill=nail)
    d.line([(cx - s * 0.8, bottom - s * 1.8), (cx - s * 1.4, bottom - s * 2.4)], fill=skin)


def doll_head(d, cx, cy, s, cloth, button, stitch):
    d.rounded_rectangle([cx - s, cy - s, cx + s, cy + s], radius=max(1, s * 0.4), fill=cloth)
    d.ellipse([cx - s * 0.7, cy - s * 0.5, cx - s * 0.1, cy + s * 0.1], fill=button)
    d.line([(cx + s * 0.15, cy - s * 0.5), (cx + s * 0.7, cy + 0.0)], fill=stitch)
    d.line([(cx + s * 0.7, cy - s * 0.5), (cx + s * 0.15, cy + 0.0)], fill=stitch)
    d.line([(cx - s * 0.6, cy + s * 0.5), (cx + s * 0.6, cy + s * 0.5)], fill=stitch)
    for i in range(4):
        x = cx - s * 0.5 + i * s * 0.33
        d.point((x, cy + s * 0.35), fill=MOON)
        d.point((x, cy + s * 0.65), fill=MOON)


def padlock(d, cx, cy, s, body, shackle_color):
    d.arc([cx - s * 0.6, cy - s * 1.2, cx + s * 0.6, cy + s * 0.2], 180, 360, fill=shackle_color, width=1)
    d.line([(cx - s * 0.6, cy - s * 0.5), (cx - s * 0.6, cy)], fill=shackle_color)
    d.line([(cx + s * 0.6, cy - s * 0.5), (cx + s * 0.6, cy)], fill=shackle_color)
    d.rectangle([cx - s, cy, cx + s, cy + s * 1.3], fill=body)
    d.point((cx, cy + s * 0.5), fill=BLACK)
    d.point((cx, cy + s * 0.8), fill=BLACK)


def lips_sewn(d, cx, cy, w, color, thread):
    d.polygon([(cx - w, cy), (cx - w * 0.4, cy - w * 0.35), (cx, cy - w * 0.2), (cx + w * 0.4, cy - w * 0.35), (cx + w, cy),
               (cx + w * 0.4, cy + w * 0.4), (cx - w * 0.4, cy + w * 0.4)], fill=color)
    d.line([(cx - w, cy), (cx + w, cy)], fill=BLACK)
    for i in range(5):
        x = cx - w * 0.6 + i * w * 0.3
        d.line([(x, cy - w * 0.3), (x, cy + w * 0.3)], fill=thread)


# ------------------------------------------------------------------ the 32x32 wheel pictures

S = 32
C = 15.5


def wheel_base(inside=(24, 10, 30, 255), edge=(54, 24, 66, 255)):
    """A round frame of black iron with purple glints and four rivets."""
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = im.load()
    for y in range(S):
        for x in range(S):
            dd = math.hypot(x - C, y - C)
            if dd <= 15.6:
                if dd > 13.6:
                    light = (x - C) * -0.7 + (y - C) * -0.7
                    px[x, y] = IRON_LIGHT if light > 9 else IRON if light > -6 else (28, 24, 34, 255)
                elif dd > 12.6:
                    px[x, y] = PURPLE_DARK
                else:
                    t = dd / 12.6
                    px[x, y] = tuple(int(inside[i] * (1 - t) + edge[i] * t) for i in range(3)) + (255,)
    for a in (45, 135, 225, 315):                                             # rivets
        x = C + math.cos(math.radians(a)) * 14.6
        y = C + math.sin(math.radians(a)) * 14.6
        px[int(round(x)), int(round(y))] = PURPLE_LIGHT
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


ZOMBIE_INSIDE = dict(inside=(14, 22, 16, 255), edge=(34, 50, 32, 255))
WRAITH_INSIDE = dict(inside=(12, 18, 34, 255), edge=(30, 44, 74, 255))
CHAINS_INSIDE = dict(inside=(22, 12, 26, 255), edge=(52, 30, 58, 255))
CURSES_INSIDE = dict(inside=(30, 8, 38, 255), edge=(70, 22, 86, 255))


# ---- key 4: Zombie

def w_raise_the_dead():
    base = wheel_base(**ZOMBIE_INSIDE)
    layer, d = canvas(S)
    d.rectangle([3, 24, 28, 28], fill=(70, 50, 32, 255))                       # the grave
    d.polygon([(4, 24), (10, 21), (16, 23), (22, 20), (28, 24)], fill=(90, 66, 42, 255))
    claw_hand(d, 10, 23, 3.0, ROT, BLACK)
    claw_hand(d, 21, 23, 3.4, ROT, BLACK)
    d.rectangle([14, 6, 17, 13], fill=MOON_DARK)                               # a headstone behind
    d.rectangle([12, 8, 19, 10], fill=MOON_DARK)
    return in_frame(base, layer)


def w_grave_chill():
    base = wheel_base(**ZOMBIE_INSIDE)
    layer, d = canvas(S)
    for i in range(6):                                                          # a snowflake of ice
        a = math.tau * i / 6
        x, y = 15.5 + math.cos(a) * 10, 15.5 + math.sin(a) * 10
        d.line([(15.5, 15.5), (x, y)], fill=ICE, width=2)
        mx, my = 15.5 + math.cos(a) * 6, 15.5 + math.sin(a) * 6
        for s in (-0.6, 0.6):
            d.line([(mx, my), (mx + math.cos(a + s) * 3, my + math.sin(a + s) * 3)], fill=ICE_LIGHT)
    skull(d, 15.5, 14.5, 4, ICE_LIGHT, (40, 70, 110, 255))
    return in_frame(base, layer)


def w_undying_flesh():
    base = wheel_base(**ZOMBIE_INSIDE)
    layer, d = canvas(S)
    d.polygon([(15.5, 26), (5, 15), (5, 9), (9, 5), (13, 5), (15.5, 8), (18, 5), (22, 5), (26, 9), (26, 15)],
              fill=ROT)                                                         # a stitched heart
    d.polygon([(15.5, 22), (8, 14), (8, 10), (11, 8), (15.5, 11)], fill=ROT_DARK)
    d.line([(15.5, 8), (15.5, 25)], fill=BLACK)
    for y in range(10, 24, 3):
        d.line([(13.5, y), (17.5, y + 1)], fill=BONE)
    return in_frame(base, layer)


def w_death_sentence():
    base = wheel_base(**ZOMBIE_INSIDE)
    layer, d = canvas(S)
    coffin_shape(d, 15.5, 4, 16, 24, (40, 20, 50, 255), PURPLE_LIGHT)
    skull(d, 15.5, 11, 4, BONE, BLACK)
    d.polygon([(11, 18), (20, 18), (15.5, 22)], fill=PURPLE_LIGHT)               # an hourglass
    d.polygon([(15.5, 22), (20, 26), (11, 26)], fill=PURPLE)
    return in_frame(base, layer)


# ---- key 5: Wraith

def w_mirror_step():
    base = wheel_base(**WRAITH_INSIDE)
    layer, d = canvas(S)
    d.ellipse([5, 4, 17, 27], fill=(150, 180, 220, 255))
    d.ellipse([7, 6, 15, 25], fill=(70, 90, 140, 255))
    d.ellipse([15, 4, 27, 27], fill=(150, 180, 220, 255))
    d.ellipse([17, 6, 25, 25], fill=(70, 90, 140, 255))
    ghost(d, 11, 9, 2.4, GHOST, GHOST_DARK)
    d.line([(12, 16), (21, 16)], fill=WHITE)
    d.polygon([(21, 14), (24, 16), (21, 18)], fill=WHITE)
    return in_frame(base, layer)


def w_possession():
    base = wheel_base(**WRAITH_INSIDE)
    layer, d = canvas(S)
    d.rectangle([12, 14, 20, 27], fill=(90, 110, 70, 255))                        # a body
    d.rectangle([12, 6, 20, 13], fill=(140, 160, 110, 255))
    d.rectangle([13, 9, 15, 10], fill=GHOST)
    d.rectangle([17, 9, 19, 10], fill=GHOST)
    ghost(d, 8, 3, 3, GHOST, GHOST_DARK)
    d.line([(10, 12), (14, 18)], fill=GHOST, width=2)                              # diving in
    return in_frame(base, layer)


def w_ghost_host():
    base = wheel_base(**WRAITH_INSIDE)
    layer, d = canvas(S)
    ghost(d, 9, 9, 3, GHOST, GHOST_DARK)
    ghost(d, 22, 9, 3, GHOST, GHOST_DARK)
    ghost(d, 15.5, 4, 3.6, (230, 244, 255, 255), GHOST_DARK)
    return in_frame(base, layer)


# ---- key 6: Chains

def w_animate_objects():
    base = wheel_base(**CHAINS_INSIDE)
    layer, d = canvas(S)
    for x0, y0, x1, y1 in ((15.5, 4, 8, 15), (15.5, 4, 23, 13), (15.5, 4, 16, 21)):   # strings
        d.line([(x0, y0), (x1, y1)], fill=PURPLE_LIGHT)
    d.rectangle([5, 15, 10, 19], fill=WOOD_LIGHT)                                  # a crate, a sword, an arrow
    d.line([(21, 13), (26, 22)], fill=IRON_LIGHT, width=2)
    d.line([(19, 17), (23, 15)], fill=WOOD_LIGHT)
    d.line([(13, 21), (19, 27)], fill=WOOD_LIGHT)
    d.polygon([(19, 27), (17, 27), (19, 25)], fill=IRON_LIGHT)
    d.rectangle([12, 2, 19, 4], fill=WOOD)
    return in_frame(base, layer)


def w_grave_shackles():
    base = wheel_base(**CHAINS_INSIDE)
    layer, d = canvas(S)
    d.ellipse([8, 10, 23, 16], outline=IRON_LIGHT, width=2)                         # a cuff
    d.line([(9, 13), (22, 13)], fill=PURPLE_LIGHT)
    chain_line(d, 5, 27, 10, 15, IRON_LIGHT, None, 3, 1.5)
    chain_line(d, 26, 27, 21, 15, IRON_LIGHT, None, 3, 1.5)
    d.rectangle([3, 26, 28, 28], fill=(70, 50, 32, 255))
    return in_frame(base, layer)


def w_thorned_mausoleum():
    base = wheel_base(**CHAINS_INSIDE)
    layer, d = canvas(S)
    d.pieslice([4, 6, 27, 34], 180, 360, fill=(20, 8, 24, 255))
    for y in (11, 16):
        d.line([(5, y), (26, y + 1)], fill=BANDAGE, width=2)
    thorns(d, [(5, 20), (9, 13), (15.5, 7), (22, 13), (26, 20)], THORN, 1)
    thorns(d, [(7, 20), (12, 16), (15.5, 11), (19, 16), (24, 20)], THORN, 1)
    d.rectangle([3, 20, 28, 22], fill=(60, 40, 60, 255))
    return in_frame(base, layer)


def w_coffin_of_ages():
    base = wheel_base(**CHAINS_INSIDE)
    layer, d = canvas(S)
    coffin_shape(d, 15.5, 3, 15, 26, WOOD, WOOD_LIGHT)
    d.line([(10, 10), (21, 14)], fill=BANDAGE, width=2)
    d.line([(9, 19), (22, 16)], fill=BANDAGE, width=2)
    d.line([(15.5, 6), (15.5, 26)], fill=BLACK)
    d.line([(11, 11), (20, 11)], fill=BLACK)
    rose(d, 15.5, 11, 2, ROSE, ROSE_LIGHT)
    return in_frame(base, layer)


def w_thorn_garden():
    base = wheel_base(**CHAINS_INSIDE)
    layer, d = canvas(S)
    thorns(d, [(4, 27), (7, 20), (6, 14), (9, 9)], THORN, 1)
    thorns(d, [(27, 27), (24, 21), (26, 15), (22, 11)], THORN, 1)
    thorns(d, [(15.5, 28), (14, 21), (16, 15)], THORN, 1)
    d.line([(15, 22), (19, 20)], fill=LEAF, width=2)
    rose(d, 9, 9, 2.5, ROSE, ROSE_LIGHT)
    rose(d, 22, 11, 2.5, ROSE, ROSE_LIGHT)
    rose(d, 16, 13, 3.4, ROSE, ROSE_LIGHT)
    return in_frame(base, layer)


# ---- key 7: Curses

def w_curse_by_connection():
    base = wheel_base(**CURSES_INSIDE)
    layer, d = canvas(S)
    d.ellipse([3, 18, 11, 26], fill=PURPLE_LIGHT)                                   # you
    d.ellipse([21, 5, 29, 13], fill=BLOOD_LIGHT)                                    # them, far away
    for i in range(7):
        t = i / 6
        x = 8 + (24 - 8) * t + math.sin(t * 9) * 2
        y = 21 + (9 - 21) * t
        d.point((x, y), fill=PURPLE_LIGHT)
        d.point((x + 1, y), fill=PURPLE)
    eye(d, 25, 9, 3.5, 2, MOON, PURPLE, BLACK)
    return in_frame(base, layer)


def w_cursed_artifact():
    base = wheel_base(**CURSES_INSIDE)
    layer, d = canvas(S)
    d.ellipse([7, 23, 25, 28], fill=(60, 30, 70, 255))
    doll_head(d, 15.5, 11, 6, CLOTH, BLACK, BLACK)
    d.rectangle([12, 17, 19, 25], fill=CLOTH)
    thorns(d, [(9, 22), (15, 19), (22, 23)], THORN, 1)
    return in_frame(base, layer)


def w_rite_of_silence():
    base = wheel_base(**CURSES_INSIDE)
    layer, d = canvas(S)
    d.ellipse([4, 4, 27, 27], outline=PURPLE_LIGHT)
    for i in range(8):
        a = math.tau * i / 8
        d.point((15.5 + math.cos(a) * 9, 15.5 + math.sin(a) * 9), fill=PURPLE_LIGHT)
    lips_sewn(d, 15.5, 15.5, 7, BLOOD, MOON)
    return in_frame(base, layer)


def w_bane_mist():
    base = wheel_base(**CURSES_INSIDE)
    layer, d = canvas(S)
    for x, y, r in ((9, 18, 6), (16, 14, 7), (23, 18, 6), (14, 21, 6), (20, 22, 5)):
        d.ellipse([x - r, y - r, x + r, y + r], fill=MIST)
    for x, y in ((10, 16), (17, 12), (22, 19), (13, 22), (19, 23)):                   # blood and black spots
        d.point((x, y), fill=BLOOD)
        d.point((x + 2, y + 1), fill=BLACK)
    moon(d, 22, 7, 3, MOON, MOON_DARK, True)
    return in_frame(base, layer)


def w_ancient_blight():
    base = wheel_base(**CURSES_INSIDE)
    layer, d = canvas(S)
    spots = ((7, 22), (15.5, 10), (24, 21))
    for (x0, y0), (x1, y1) in ((spots[0], spots[1]), (spots[1], spots[2])):
        d.line([(x0, y0), (x1, y1)], fill=(120, 60, 140, 255))
    for i, (x, y) in enumerate(spots):
        c = (PURPLE_LIGHT, PURPLE, PURPLE_DARK)[i]
        d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=c)
        d.point((x, y), fill=BLACK)
        d.point((x - 1, y - 1), fill=BLACK)
    return in_frame(base, layer)


def w_abomination_form():
    base = wheel_base(**CURSES_INSIDE)
    layer, d = canvas(S)
    doll_head(d, 15.5, 10, 7, CLOTH, BLACK, BLACK)
    d.rectangle([11, 17, 20, 27], fill=CLOTH)
    d.rectangle([14, 19, 17, 22], fill=PURPLE_LIGHT)
    thorns(d, [(4, 12), (10, 7), (18, 16), (27, 10)], THORN, 1)
    thorns(d, [(5, 24), (13, 20), (21, 25), (27, 20)], THORN, 1)
    rose(d, 7, 9, 2, ROSE, ROSE_LIGHT)
    return in_frame(base, layer)


WHEEL = {
    'raise_the_dead': w_raise_the_dead,
    'grave_chill': w_grave_chill,
    'undying_flesh': w_undying_flesh,
    'death_sentence': w_death_sentence,
    'mirror_step': w_mirror_step,
    'possession': w_possession,
    'ghost_host': w_ghost_host,
    'animate_objects': w_animate_objects,
    'grave_shackles': w_grave_shackles,
    'thorned_mausoleum': w_thorned_mausoleum,
    'coffin_of_ages': w_coffin_of_ages,
    'thorn_garden': w_thorn_garden,
    'curse_by_connection': w_curse_by_connection,
    'cursed_artifact': w_cursed_artifact,
    'rite_of_silence': w_rite_of_silence,
    'bane_mist': w_bane_mist,
    'ancient_blight': w_ancient_blight,
    'abomination_form': w_abomination_form,
}


# ------------------------------------------------------------------ the 16x16 status pictures

def s(fn):
    img, d = canvas(16)
    fn(d)
    return outlined(img)


def i_restraint(d):
    chain_line(d, 1, 3, 14, 12, IRON_LIGHT, None, 2.5, 1.5)
    chain_line(d, 1, 12, 14, 3, IRON_LIGHT, None, 2.5, 1.5)
    padlock(d, 7.5, 8, 3, IRON, IRON_LIGHT)


def i_bleeding(d):
    d.polygon([(7.5, 1), (12, 9), (10.5, 13), (7.5, 14), (4.5, 13), (3, 9)], fill=BLOOD)
    d.point((5.5, 9), fill=BLOOD_LIGHT)


def i_moonstruck(d):
    moon(d, 7.5, 7.5, 6, MOON, MOON_DARK, True)


def i_frenzy(d):
    moon(d, 7.5, 7.5, 6, (255, 200, 200, 255), (200, 120, 130, 255))
    for x in (4, 7, 10):
        d.line([(x, 3), (x + 2, 12)], fill=BLOOD)


def i_frenzy_crash(d):
    moon(d, 7.5, 7.5, 6, MOON_DARK, (120, 116, 108, 255))
    d.line([(4, 3), (11, 12)], fill=BLACK)


def i_werewolf(d):
    d.polygon([(2, 2), (5, 6), (10, 6), (13, 2), (13, 9), (10, 14), (5, 14), (2, 9)], fill=FUR)
    d.polygon([(5, 10), (10, 10), (7.5, 14)], fill=FUR_LIGHT)
    d.point((5, 8), fill=WOLF_EYE)
    d.point((10, 8), fill=WOLF_EYE)


def i_madness(d):
    d.ellipse([1, 2, 14, 13], fill=(200, 60, 120, 255))
    d.arc([3, 4, 12, 11], 0, 300, fill=WHITE)
    d.arc([5, 6, 10, 9], 60, 360, fill=WHITE)


def i_full_moon(d):
    moon(d, 7.5, 7.5, 6, MOON, MOON_DARK)


def i_risen(d):
    claw_hand(d, 7.5, 14, 2.6, ROT, BLACK)


def i_grave_chill(d):
    for i in range(3):
        a = math.pi * i / 3
        d.line([(7.5 - math.cos(a) * 6, 7.5 - math.sin(a) * 6), (7.5 + math.cos(a) * 6, 7.5 + math.sin(a) * 6)],
               fill=ICE, width=2)
    d.point((7.5, 7.5), fill=ICE_LIGHT)


def i_undying(d):
    d.polygon([(7.5, 14), (1, 7), (1, 4), (4, 1), (7.5, 4), (11, 1), (14, 4), (14, 7)], fill=ROT)
    d.line([(7.5, 4), (7.5, 13)], fill=BLACK)
    for y in (6, 9, 12):
        d.point((6, y), fill=BONE)
        d.point((9, y), fill=BONE)


def i_sentence(d):
    coffin_shape(d, 7.5, 1, 9, 14, (40, 20, 50, 255), PURPLE_LIGHT)
    skull(d, 7.5, 6, 2.5, BONE, BLACK)


def i_mirror(d):
    d.ellipse([3, 1, 12, 14], fill=(150, 180, 220, 255))
    d.ellipse([5, 3, 10, 12], fill=(70, 90, 140, 255))
    d.line([(6, 5), (8, 3)], fill=WHITE)


def i_possession(d):
    ghost(d, 7.5, 1, 4, GHOST, GHOST_DARK)


def i_ghost_host(d):
    ghost(d, 4.5, 3, 2.6, GHOST, GHOST_DARK)
    ghost(d, 10.5, 2, 3, (230, 244, 255, 255), GHOST_DARK)


def i_shackles(d):
    d.ellipse([2, 3, 13, 8], outline=IRON_LIGHT, width=2)
    chain_line(d, 3, 14, 4, 7, IRON_LIGHT, None, 2.5, 1.2)
    chain_line(d, 12, 14, 11, 7, IRON_LIGHT, None, 2.5, 1.2)


def i_mausoleum(d):
    d.pieslice([1, 3, 14, 20], 180, 360, fill=(40, 20, 46, 255))
    d.line([(1, 8), (14, 9)], fill=BANDAGE, width=1)
    thorns(d, [(2, 11), (5, 6), (7.5, 4), (10, 6), (13, 11)], THORN, 1)
    d.rectangle([1, 11, 14, 13], fill=(60, 40, 60, 255))


def i_coffin(d):
    coffin_shape(d, 7.5, 1, 9, 14, WOOD, WOOD_LIGHT)
    d.line([(4, 6), (11, 8)], fill=BANDAGE)
    rose(d, 7.5, 5, 1.2, ROSE, ROSE_LIGHT)


def i_thorn_garden(d):
    thorns(d, [(3, 15), (4, 9), (3, 5)], THORN, 1)
    thorns(d, [(12, 15), (11, 9), (12, 6)], THORN, 1)
    rose(d, 7.5, 6, 3, ROSE, ROSE_LIGHT)
    d.line([(7.5, 9), (7.5, 15)], fill=LEAF)


def i_connection(d):
    d.ellipse([1, 9, 6, 14], fill=PURPLE_LIGHT)
    d.ellipse([9, 1, 14, 6], fill=BLOOD_LIGHT)
    for i in range(5):
        t = i / 4
        d.point((4 + 7 * t, 11 - 7 * t), fill=PURPLE_LIGHT)


def i_curse(d):
    for i in range(8):
        a = math.tau * i / 8
        d.line([(7.5 + math.cos(a) * 4, 7.5 + math.sin(a) * 4), (7.5 + math.cos(a) * 7, 7.5 + math.sin(a) * 7)],
               fill=THORN)
    eye(d, 7.5, 7.5, 5, 3, MOON, PURPLE, BLACK)


def i_relic(d):
    doll_head(d, 7.5, 6, 5, CLOTH, BLACK, BLACK)
    d.rectangle([5, 11, 10, 15], fill=CLOTH)


def i_silence(d):
    lips_sewn(d, 7.5, 7.5, 6, BLOOD, MOON)


def i_bane_mist(d):
    for x, y, r in ((5, 9, 4), (10, 8, 4.5), (7.5, 11, 4)):
        d.ellipse([x - r, y - r, x + r, y + r], fill=MIST)
    d.point((5, 8), fill=BLOOD)
    d.point((10, 10), fill=BLACK)
    d.point((8, 6), fill=BLOOD)


def i_blight(d):
    for i, (x, y) in enumerate(((3, 11), (7.5, 4), (12, 11))):
        c = (PURPLE_LIGHT, PURPLE, PURPLE_DARK)[i]
        d.ellipse([x - 2.5, y - 2.5, x + 2.5, y + 2.5], fill=c)
    d.line([(4, 9), (6.5, 6)], fill=PURPLE)
    d.line([(9, 6), (11, 9)], fill=PURPLE)


def i_abomination(d):
    doll_head(d, 7.5, 7, 6, CLOTH, BLACK, BLACK)
    thorns(d, [(1, 3), (7, 9), (14, 4)], THORN, 1)


def i_stack_link(d):
    """The stack picture (one per stack, if the Pathway gets stacks): a chain link in a ring."""
    d.ellipse([1, 1, 14, 14], outline=PURPLE_DARK)
    d.rounded_rectangle([5, 3, 10, 12], radius=2, outline=PURPLE_LIGHT, width=2)


STATUS = {
    'restraint': i_restraint,
    'bleeding': i_bleeding,
    'moonstruck': i_moonstruck,
    'frenzy': i_frenzy,
    'frenzy_crash': i_frenzy_crash,
    'werewolf': i_werewolf,
    'madness': i_madness,
    'full_moon': i_full_moon,
    'risen': i_risen,
    'grave_chill': i_grave_chill,
    'undying': i_undying,
    'sentence': i_sentence,
    'mirror': i_mirror,
    'possession': i_possession,
    'ghost_host': i_ghost_host,
    'shackles': i_shackles,
    'mausoleum': i_mausoleum,
    'coffin': i_coffin,
    'thorn_garden': i_thorn_garden,
    'connection': i_connection,
    'curse': i_curse,
    'relic': i_relic,
    'silence': i_silence,
    'bane_mist': i_bane_mist,
    'blight': i_blight,
    'abomination': i_abomination,
    'stack_link': i_stack_link,
}


def pathway_icon():
    """A chain link bound by a black thorny vine, a rose on it: Temperance's restraint."""
    img, d = canvas(16)
    d.rounded_rectangle([3, 1, 12, 14], radius=4, outline=PURPLE, width=2)
    d.rounded_rectangle([5, 3, 10, 12], radius=2, outline=PURPLE_DARK, width=1)
    thorns(d, [(1, 12), (5, 9), (10, 6), (14, 3)], THORN, 1)
    rose(d, 11, 5, 1.6, ROSE, ROSE_LIGHT)
    d.point((4, 4), fill=PURPLE_LIGHT)
    return outlined(img)


def main(assets, preview=None):
    out = []
    for name, fn in WHEEL.items():
        path = os.path.join(assets, 'textures/gui/ability_menus/abomination', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fn().save(path)
        out.append(path)
    for name, fn in STATUS.items():
        path = os.path.join(assets, 'textures/gui/abomination', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        s(fn).save(path)
        out.append(path)
    path = os.path.join(assets, 'textures/gui/pathway_icons/abomination.png')
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
