"""Screen pictures for the Hand Of God (Twilight Giant pathway).

python3 tools/model_gen/hand_of_god/icons.py src/main/resources/assets/circle_of_inevitability [preview.png]

  textures/gui/ability_menus/hand_of_god/*.png   32x32, the three wheels (Dawn on key 4, Demon Hunting on key 6,
                                                 Silver and Twilight on key 7), in a round gold frame
  textures/gui/hand_of_god/*.png                 16x16, the statuses shown above the hotbar
  textures/gui/pathway_icons/hand_of_god.png     16x16, the Pathway's icon beside the hotbar

Every picture is drawn in flat colors and then outlined automatically, so the shapes stay simple to change.
Replace any of them with a hand-drawn PNG of the same name.
"""
import math
import os
import sys

from PIL import Image, ImageDraw

# ------------------------------------------------------------------ colors
OUTLINE = (28, 14, 10, 255)
GOLD = (240, 190, 70, 255)
GOLD_LIGHT = (255, 240, 170, 255)
GOLD_DARK = (150, 100, 30, 255)
DAWN = (255, 246, 210, 255)
WHITE = (255, 255, 255, 255)
SILVER = (196, 204, 222, 255)
SILVER_LIGHT = (244, 248, 255, 255)
SILVER_DARK = (104, 112, 136, 255)
GLORY = (255, 122, 26, 255)
GLORY_LIGHT = (255, 200, 120, 255)
GLORY_DARK = (180, 50, 20, 255)
GREEN = (50, 190, 90, 255)
GREEN_LIGHT = (150, 255, 170, 255)
GREEN_DARK = (16, 70, 34, 255)
SKIN = (106, 122, 142, 255)
SKIN_DARK = (40, 52, 72, 255)
RIFT = (160, 230, 255, 255)
RED = (210, 40, 40, 255)
RED_LIGHT = (255, 130, 120, 255)
BROWN = (120, 84, 44, 255)
BROWN_LIGHT = (176, 130, 74, 255)
STEEL = (170, 176, 190, 255)
SHADOW = (50, 44, 60, 255)


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

def sword(d, x, top, bottom, blade, edge, guard, width=1):
    """A sword standing point up: blade from 'top' to the guard, a guard, a grip and a pommel."""
    g = bottom - max(3, (bottom - top) // 4)
    d.polygon([(x - width, top + 2), (x, top), (x + width, top + 2), (x + width, g), (x - width, g)], fill=blade)
    d.line([(x, top + 1), (x, g - 1)], fill=edge)
    d.rectangle([x - width - 2, g, x + width + 2, g + 1], fill=guard)
    d.rectangle([x, g + 2, x, bottom - 1], fill=BROWN)
    d.point((x, bottom), fill=guard)


def sun(d, cx, cy, r, color, ray_color, rays=8, ray_len=2.5):
    for i in range(rays):
        a = math.tau * i / rays
        d.line([(cx + math.cos(a) * (r + 1), cy + math.sin(a) * (r + 1)),
                (cx + math.cos(a) * (r + ray_len), cy + math.sin(a) * (r + ray_len))], fill=ray_color)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color)


def eye(d, cx, cy, w, h, white, iris, pupil):
    d.polygon([(cx - w, cy), (cx - w * 0.4, cy - h), (cx + w * 0.4, cy - h), (cx + w, cy), (cx + w * 0.4, cy + h),
               (cx - w * 0.4, cy + h)], fill=white)
    d.ellipse([cx - h, cy - h, cx + h, cy + h], fill=iris)
    d.line([(cx, cy - h + 1), (cx, cy + h - 1)], fill=pupil)


def fist(d, cx, cy, s, skin, light):
    """A clenched fist seen from the front, s = half its width."""
    d.rounded_rectangle([cx - s, cy - s * 0.6, cx + s, cy + s], radius=max(1, s * 0.3), fill=skin)
    for i in range(4):
        x = cx - s + (i + 0.5) * (2 * s / 4)
        d.line([(x, cy - s * 0.5), (x, cy + s * 0.1)], fill=light)
    d.rectangle([cx - s * 0.2, cy + s, cx + s * 0.6, cy + s * 1.6], fill=skin)


def hand_open(d, cx, cy, s, skin, light):
    """An open hand, palm forward, fingers up."""
    d.rectangle([cx - s, cy - s * 0.2, cx + s, cy + s], fill=skin)
    for i in range(4):
        x0 = cx - s + i * (2 * s / 4)
        length = s * (1.0 if i in (1, 2) else 0.8)
        d.rectangle([x0 + 0.5, cy - s * 0.2 - length, x0 + 2 * s / 4 - 1, cy - s * 0.2], fill=skin)
    d.polygon([(cx - s, cy + s * 0.2), (cx - s * 1.7, cy - s * 0.4), (cx - s * 1.5, cy - s * 0.7), (cx - s, cy - s * 0.2)],
              fill=skin)
    d.ellipse([cx - s * 0.4, cy + s * 0.1, cx + s * 0.4, cy + s * 0.8], fill=light)


def vial(d, cx, cy, s, liquid):
    d.rectangle([cx - s * 0.5, cy - s, cx + s * 0.5, cy + s], fill=(200, 230, 210, 255))
    d.rectangle([cx - s * 0.5, cy - s * 0.1, cx + s * 0.5, cy + s], fill=liquid)
    d.rectangle([cx - s * 0.6, cy - s * 1.4, cx + s * 0.6, cy - s], fill=GREEN_DARK)


def giant_head(d, cx, cy, s, skin, shade, rift):
    d.rounded_rectangle([cx - s, cy - s, cx + s, cy + s * 0.9], radius=max(1, s * 0.4), fill=skin)
    d.line([(cx - s * 0.8, cy - s * 0.05), (cx + s * 0.8, cy - s * 0.05)], fill=shade)
    d.line([(cx, cy - s * 0.85), (cx, cy + s * 0.2)], fill=rift, width=max(1, int(s * 0.25)))
    d.line([(cx - s * 0.4, cy + s * 0.55), (cx + s * 0.4, cy + s * 0.55)], fill=shade)


def clock(d, cx, cy, r, face, hand):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=face)
    d.line([(cx, cy), (cx, cy - r * 0.75)], fill=hand)
    d.line([(cx, cy), (cx + r * 0.5, cy)], fill=hand)


def shield(d, cx, top, s, fill, emblem):
    d.polygon([(cx - s, top), (cx + s, top), (cx + s, top + s), (cx, top + s * 2.2), (cx - s, top + s)], fill=fill)
    d.ellipse([cx - s * 0.4, top + s * 0.4, cx + s * 0.4, top + s * 1.2], fill=emblem)


# ------------------------------------------------------------------ the 32x32 wheel pictures

S = 32
C = 15.5


def wheel_base(inside=(30, 18, 10, 255), edge=(62, 36, 16, 255)):
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = im.load()
    for y in range(S):
        for x in range(S):
            dd = math.hypot(x - C, y - C)
            if dd <= 15.6:
                if dd > 13.6:
                    light = (x - C) * -0.7 + (y - C) * -0.7
                    px[x, y] = GOLD_LIGHT if light > 9 else GOLD if light > -6 else GOLD_DARK
                elif dd > 12.6:
                    px[x, y] = (90, 56, 20, 255)
                else:
                    t = dd / 12.6
                    px[x, y] = tuple(int(inside[i] * (1 - t) + edge[i] * t) for i in range(3)) + (255,)
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


DAWN_INSIDE = dict(inside=(44, 32, 12, 255), edge=(84, 60, 20, 255))
HUNT_INSIDE = dict(inside=(8, 24, 14, 255), edge=(20, 50, 30, 255))
SILVER_INSIDE = dict(inside=(20, 24, 36, 255), edge=(44, 52, 72, 255))
GLORY_INSIDE = dict(inside=(44, 14, 8, 255), edge=(90, 34, 14, 255))


# ---- key 4: Dawn

def w_light_of_dawn():
    base = wheel_base(**DAWN_INSIDE)
    layer, d = canvas(S)
    d.pieslice([6, 12, 25, 31], 180, 360, fill=GOLD)
    d.pieslice([10, 16, 21, 27], 180, 360, fill=DAWN)
    for i in range(9):
        a = math.pi + math.pi * i / 8
        d.line([(15.5 + math.cos(a) * 11, 21.5 + math.sin(a) * 11), (15.5 + math.cos(a) * 14, 21.5 + math.sin(a) * 14)],
               fill=GOLD_LIGHT)
    d.rectangle([4, 22, 27, 23], fill=GOLD_DARK)                              # the horizon
    return in_frame(base, layer)


def w_dawn_armor():
    base = wheel_base(**DAWN_INSIDE)
    layer, d = canvas(S)
    d.polygon([(9, 9), (22, 9), (24, 13), (22, 25), (9, 25), (7, 13)], fill=GOLD)          # cuirass
    d.polygon([(4, 9), (10, 7), (11, 13), (5, 14)], fill=GOLD_LIGHT)                          # pauldrons
    d.polygon([(27, 9), (21, 7), (20, 13), (26, 14)], fill=GOLD_LIGHT)
    sun(d, 15.5, 16, 2.5, DAWN, DAWN, 8, 2)
    d.rectangle([9, 22, 22, 23], fill=GOLD_DARK)
    return in_frame(base, layer)


def w_sword_of_dawn():
    base = wheel_base(**DAWN_INSIDE)
    layer, d = canvas(S)
    sun(d, 15.5, 11, 4, (255, 220, 120, 120), GOLD_LIGHT, 12, 3)
    sword(d, 15, 4, 28, DAWN, WHITE, GOLD, 2)
    return in_frame(base, layer)


def w_hurricane_of_light():
    base = wheel_base(**DAWN_INSIDE)
    layer, d = canvas(S)
    for i, (y, w) in enumerate(((7, 11), (11, 9), (15, 7), (19, 5), (23, 3), (26, 1.5))):
        d.ellipse([15.5 - w, y - 1.5, 15.5 + w, y + 1.5], outline=GOLD_LIGHT if i % 2 == 0 else DAWN)
    d.line([(9, 9), (21, 13)], fill=WHITE)
    d.line([(11, 17), (19, 20)], fill=WHITE)
    return in_frame(base, layer)


# ---- key 6: Demon Hunting

def w_eye_of_demon_hunting():
    base = wheel_base(**HUNT_INSIDE)
    layer, d = canvas(S)
    eye(d, 15.5, 15.5, 11, 6, (220, 240, 220, 255), GREEN, GREEN_DARK)
    for x, y in ((7, 6), (24, 7), (6, 25), (25, 24)):                          # symbols round it
        d.line([(x - 1, y - 1), (x + 1, y + 1)], fill=GREEN_LIGHT)
        d.line([(x - 1, y + 1), (x + 1, y - 1)], fill=GREEN_LIGHT)
    return in_frame(base, layer)


def w_alchemy():
    base = wheel_base(**HUNT_INSIDE)
    layer, d = canvas(S)
    vial(d, 11, 17, 6, (120, 255, 160, 255))
    d.polygon([(19, 26), (27, 8), (28, 9), (21, 27)], fill=STEEL)              # a blade being coated
    d.line([(20, 24), (26, 10)], fill=GREEN_LIGHT)
    d.point((24, 20), fill=GREEN_LIGHT)
    return in_frame(base, layer)


def w_mind_concealment():
    base = wheel_base(**HUNT_INSIDE)
    layer, d = canvas(S)
    d.ellipse([7, 6, 24, 24], fill=SHADOW)                                     # a head in shadow
    d.arc([10, 12, 21, 20], 20, 160, fill=GREEN_LIGHT)                         # closed eyes
    d.rectangle([5, 14, 26, 16], fill=(30, 30, 40, 255))                       # a band over the mind
    d.line([(6, 15), (25, 15)], fill=GREEN)
    return in_frame(base, layer)


def w_giant_form():
    base = wheel_base(inside=(14, 20, 32, 255), edge=(30, 42, 62, 255))
    layer, d = canvas(S)
    giant_head(d, 15.5, 14, 9, SKIN, SKIN_DARK, RIFT)
    d.rectangle([7, 22, 24, 28], fill=SKIN_DARK)
    return in_frame(base, layer)


# ---- key 7: Silver and Twilight

def w_mercury_liquefaction():
    base = wheel_base(**SILVER_INSIDE)
    layer, d = canvas(S)
    d.ellipse([5, 19, 26, 27], fill=SILVER)
    d.ellipse([9, 21, 20, 25], fill=SILVER_LIGHT)
    d.polygon([(12, 20), (14, 9), (17, 6), (19, 10), (18, 20)], fill=SILVER)    # a figure melting into it
    d.ellipse([14, 4, 19, 9], fill=SILVER)
    d.point((22, 15), fill=SILVER_LIGHT)
    return in_frame(base, layer)


def w_silver_rapier():
    base = wheel_base(**SILVER_INSIDE)
    layer, d = canvas(S)
    for x0, y0, x1, y1 in ((5, 26, 22, 6), (10, 28, 27, 12), (4, 18, 20, 4)):
        d.line([(x0, y0), (x1, y1)], fill=SILVER_LIGHT, width=1)
        d.point((x1, y1), fill=WHITE)
    d.ellipse([12, 13, 19, 20], fill=(150, 170, 220, 255))
    return in_frame(base, layer)


def w_light_concealment():
    base = wheel_base(**SILVER_INSIDE)
    layer, d = canvas(S)
    d.ellipse([6, 6, 25, 25], fill=(255, 250, 220, 120))
    d.ellipse([10, 10, 21, 21], fill=DAWN)
    d.polygon([(13, 26), (15, 15), (18, 15), (20, 26)], fill=(220, 225, 240, 140))   # a figure fading into it
    d.ellipse([14, 10, 19, 15], fill=(220, 225, 240, 140))
    return in_frame(base, layer)


def w_twilight_sword():
    base = wheel_base(**GLORY_INSIDE)
    layer, d = canvas(S)
    d.ellipse([6, 22, 25, 28], fill=(20, 10, 10, 255))                          # the void it bursts from
    sword(d, 15, 3, 24, GLORY, GLORY_LIGHT, (70, 40, 30, 255), 2)
    d.point((9, 9), fill=GLORY_LIGHT)
    d.point((22, 12), fill=GLORY_LIGHT)
    return in_frame(base, layer)


def w_divine_right_hand():
    base = wheel_base(**GLORY_INSIDE)
    layer, d = canvas(S)
    hand_open(d, 16.5, 17, 6, GLORY, GLORY_LIGHT)
    return in_frame(base, layer)


def w_passage_of_time():
    base = wheel_base(**GLORY_INSIDE)
    layer, d = canvas(S)
    clock(d, 15.5, 15.5, 10, (255, 220, 160, 255), GLORY_DARK)
    for i in range(12):
        a = math.tau * i / 12
        d.point((15.5 + math.cos(a) * 8.5, 15.5 + math.sin(a) * 8.5), fill=GLORY)
    return in_frame(base, layer)


def w_decay():
    base = wheel_base(inside=(30, 22, 12, 255), edge=(64, 44, 22, 255))
    layer, d = canvas(S)
    d.polygon([(15, 27), (15, 14), (9, 8), (11, 7), (15, 11), (16, 5), (18, 6), (17, 12), (22, 8), (23, 10), (17, 16),
               (17, 27)], fill=BROWN)                                          # a withered branch
    for x, y in ((8, 20), (21, 18), (12, 24), (24, 24), (6, 14)):
        d.point((x, y), fill=BROWN_LIGHT)
    d.ellipse([18, 19, 22, 23], fill=GLORY)
    return in_frame(base, layer)


def w_divine_kingdom():
    base = wheel_base(**GLORY_INSIDE)
    layer, d = canvas(S)
    d.pieslice([5, 8, 26, 29], 180, 360, fill=GLORY)                           # the setting sun
    d.rectangle([4, 19, 27, 27], fill=(70, 30, 20, 255))                       # the walls
    for x in (5, 11, 17, 23):
        d.rectangle([x, 16, x + 3, 19], fill=(70, 30, 20, 255))
    d.polygon([(11, 10), (11, 6), (13, 8), (15.5, 4), (18, 8), (20, 6), (20, 10)], fill=GOLD)   # a crown above
    return in_frame(base, layer)


WHEEL = {
    'light_of_dawn': w_light_of_dawn,
    'dawn_armor': w_dawn_armor,
    'sword_of_dawn': w_sword_of_dawn,
    'hurricane_of_light': w_hurricane_of_light,
    'eye_of_demon_hunting': w_eye_of_demon_hunting,
    'alchemy': w_alchemy,
    'mind_concealment': w_mind_concealment,
    'giant_form': w_giant_form,
    'mercury_liquefaction': w_mercury_liquefaction,
    'silver_rapier': w_silver_rapier,
    'light_concealment': w_light_concealment,
    'twilight_sword': w_twilight_sword,
    'divine_right_hand': w_divine_right_hand,
    'passage_of_time': w_passage_of_time,
    'decay': w_decay,
    'divine_kingdom': w_divine_kingdom,
}


# ------------------------------------------------------------------ the 16x16 status pictures

def s(fn):
    img, d = canvas(16)
    fn(d)
    return outlined(img)


def i_divine_blood(d):
    d.polygon([(7.5, 1), (12, 8), (12, 11), (9, 14), (6, 14), (3, 11), (3, 8)], fill=RED)
    d.ellipse([5, 8, 8, 11], fill=RED_LIGHT)
    d.point((12, 2), fill=GOLD_LIGHT)


def i_resistance(d):
    shield(d, 7.5, 1, 5.5, STEEL, SILVER_LIGHT)
    d.line([(4, 6), (11, 6)], fill=SILVER_DARK)


def i_grandmaster(d):
    d.line([(2, 13), (13, 2)], fill=SILVER_LIGHT, width=2)
    d.line([(3, 9), (6, 12)], fill=GOLD)
    d.arc([1, 1, 14, 14], 200, 320, fill=WHITE)


def i_riposte(d):
    d.line([(2, 2), (13, 13)], fill=SILVER_LIGHT, width=1)
    d.line([(13, 2), (2, 13)], fill=SILVER_LIGHT, width=1)
    d.ellipse([5, 5, 10, 10], fill=GOLD_LIGHT)


def i_light_of_dawn(d):
    d.pieslice([2, 5, 13, 16], 180, 360, fill=GOLD)
    d.pieslice([5, 8, 10, 13], 180, 360, fill=DAWN)
    d.line([(7.5, 1), (7.5, 3)], fill=GOLD_LIGHT)
    d.line([(1, 7), (3, 8)], fill=GOLD_LIGHT)
    d.line([(14, 7), (12, 8)], fill=GOLD_LIGHT)


def i_dawn_armor(d):
    d.polygon([(4, 3), (11, 3), (12, 6), (11, 14), (4, 14), (3, 6)], fill=GOLD)
    d.polygon([(1, 3), (4, 2), (5, 6), (1, 7)], fill=GOLD_LIGHT)
    d.polygon([(14, 3), (11, 2), (10, 6), (14, 7)], fill=GOLD_LIGHT)
    d.ellipse([6, 6, 9, 9], fill=DAWN)


def i_sword_of_dawn(d):
    sword(d, 7, 1, 15, DAWN, WHITE, GOLD, 1)


def i_protection(d):
    d.pieslice([1, 3, 14, 18], 180, 360, fill=(255, 220, 120, 160))
    d.arc([1, 3, 14, 18], 180, 360, fill=GOLD_LIGHT)
    sword(d, 7, 4, 13, DAWN, WHITE, GOLD, 0)
    d.line([(1, 13), (14, 13)], fill=GOLD_DARK)


def i_transference(d):
    shield(d, 7.5, 2, 4.5, GOLD, DAWN)
    d.line([(1, 2), (3, 4)], fill=RED)
    d.line([(14, 2), (12, 4)], fill=RED)
    d.point((2, 12), fill=RED_LIGHT)
    d.point((13, 12), fill=RED_LIGHT)


def i_eye(d):
    eye(d, 7.5, 7.5, 6.5, 3.5, (220, 240, 220, 255), GREEN, GREEN_DARK)


def i_alchemy(d):
    vial(d, 7.5, 9, 4, (120, 255, 160, 255))
    d.point((12, 3), fill=GREEN_LIGHT)
    d.point((3, 4), fill=GREEN_LIGHT)


def i_concealment(d):
    d.ellipse([3, 2, 12, 12], fill=SHADOW)
    d.arc([5, 5, 10, 9], 20, 160, fill=GREEN_LIGHT)
    d.rectangle([2, 7, 13, 8], fill=(30, 30, 40, 255))


def i_giant(d):
    giant_head(d, 7.5, 7.5, 6, SKIN, SKIN_DARK, RIFT)


def i_madness(d):
    giant_head(d, 7.5, 7.5, 6, (150, 90, 110, 255), (60, 20, 30, 255), RED_LIGHT)
    d.line([(1, 1), (3, 4)], fill=RED)
    d.line([(14, 1), (12, 4)], fill=RED)


def i_mercury(d):
    d.ellipse([1, 9, 14, 15], fill=SILVER)
    d.ellipse([4, 10, 10, 13], fill=SILVER_LIGHT)
    d.polygon([(6, 10), (7, 4), (9, 2), (10, 5), (9, 10)], fill=SILVER)


def i_light_concealment(d):
    d.ellipse([2, 2, 13, 13], fill=(255, 250, 220, 140))
    d.ellipse([5, 5, 10, 10], fill=DAWN)


def i_divine_hand(d):
    fist(d, 7.5, 6.5, 5, GLORY, GLORY_LIGHT)


def i_time_stop(d):
    clock(d, 7.5, 7.5, 6.5, (255, 220, 160, 255), GLORY_DARK)


def i_decay(d):
    d.polygon([(7, 15), (7, 8), (3, 4), (4, 3), (7, 6), (8, 1), (9, 1), (9, 6), (12, 3), (13, 4), (9, 9), (9, 15)],
              fill=BROWN)
    d.point((3, 11), fill=BROWN_LIGHT)
    d.point((12, 12), fill=BROWN_LIGHT)


def i_kingdom(d):
    d.pieslice([1, 4, 14, 17], 180, 360, fill=GLORY)
    d.rectangle([1, 11, 14, 15], fill=(90, 40, 24, 255))
    d.polygon([(4, 6), (4, 2), (6, 4), (7.5, 1), (9, 4), (11, 2), (11, 6)], fill=GOLD)


def i_cage(d):
    d.ellipse([1, 1, 14, 14], fill=(255, 140, 60, 90))
    d.ellipse([1, 1, 14, 14], outline=GLORY_LIGHT)
    d.line([(7.5, 1), (7.5, 14)], fill=GLORY)
    d.line([(1, 7.5), (14, 7.5)], fill=GLORY)


def i_stack_sun(d):
    """The stack picture (one per ability cast, if the Pathway gets stacks): a small sun in a ring."""
    d.ellipse([1, 1, 14, 14], outline=GOLD_DARK)
    sun(d, 7.5, 7.5, 3, GOLD_LIGHT, GOLD, 8, 2)


STATUS = {
    'divine_blood': i_divine_blood,
    'resistance': i_resistance,
    'grandmaster': i_grandmaster,
    'riposte': i_riposte,
    'light_of_dawn': i_light_of_dawn,
    'dawn_armor': i_dawn_armor,
    'sword_of_dawn': i_sword_of_dawn,
    'protection': i_protection,
    'transference': i_transference,
    'eye': i_eye,
    'alchemy': i_alchemy,
    'concealment': i_concealment,
    'giant': i_giant,
    'madness': i_madness,
    'mercury': i_mercury,
    'light_concealment': i_light_concealment,
    'divine_hand': i_divine_hand,
    'time_stop': i_time_stop,
    'decay': i_decay,
    'kingdom': i_kingdom,
    'cage': i_cage,
    'stack_sun': i_stack_sun,
}


def pathway_icon():
    """A sword standing before the setting sun of Twilight."""
    img, d = canvas(16)
    d.pieslice([1, 4, 14, 17], 180, 360, fill=GLORY)
    d.pieslice([4, 7, 11, 14], 180, 360, fill=GLORY_LIGHT)
    d.line([(1, 11), (14, 11)], fill=GLORY_DARK)
    sword(d, 7, 1, 15, SILVER_LIGHT, WHITE, GOLD, 0)
    return outlined(img)


def main(assets, preview=None):
    out = []
    for name, fn in WHEEL.items():
        path = os.path.join(assets, 'textures/gui/ability_menus/hand_of_god', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fn().save(path)
        out.append(path)
    for name, fn in STATUS.items():
        path = os.path.join(assets, 'textures/gui/hand_of_god', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        s(fn).save(path)
        out.append(path)
    path = os.path.join(assets, 'textures/gui/pathway_icons/hand_of_god.png')
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
