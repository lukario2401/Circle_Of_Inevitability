"""Screen pictures for the Prince Of Abolition (Black Emperor pathway).

python3 tools/model_gen/prince_of_abolition/icons.py src/main/resources/assets/circle_of_inevitability [preview.png]

  textures/gui/ability_menus/prince_of_abolition/*.png  32x32, the three wheels (Bribe on key 3, the Fallen on key 6,
                                                        Sovereign on key 7), in a round frame of imperial gold
  textures/gui/prince_of_abolition/*.png                16x16, the statuses shown above the hotbar, and order.png,
                                                        the sigil the Order counter shows ("[sigil] Order 4/9")
  textures/gui/pathway_icons/prince_of_abolition.png    16x16, the Pathway's icon beside the hotbar

Every picture is drawn in flat colors and then outlined automatically (as in ../knight_of_misfortune/icons.py), so the
shapes stay simple to change. Replace any of them with a hand-drawn PNG of the same name.
"""
import math
import os
import sys

from PIL import Image, ImageDraw

# ------------------------------------------------------------------ colors
OUTLINE = (8, 6, 12, 255)
JET = (22, 18, 31, 255)
JET_LIGHT = (60, 50, 80, 255)
GOLD = (224, 176, 74, 255)
GOLD_LIGHT = (255, 230, 150, 255)
GOLD_DARK = (150, 106, 30, 255)
ASH = (110, 108, 120, 255)
ASH_LIGHT = (176, 174, 186, 255)
FOG = (196, 196, 200, 255)
VIOLET = (110, 70, 170, 255)
VIOLET_LIGHT = (200, 170, 255, 255)
WHITE = (255, 255, 255, 255)
RED = (200, 50, 50, 255)
RED_LIGHT = (255, 130, 120, 255)
ROSE = (240, 150, 170, 255)
BLUE = (120, 150, 210, 255)
GREEN = (60, 120, 60, 255)
BLACK = (6, 4, 10, 255)


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

def crown(d, cx, base, w, h, color, gem=None):
    d.polygon([(cx - w / 2, base), (cx - w / 2, base - h * 0.7), (cx - w / 4, base - h * 0.4), (cx, base - h),
               (cx + w / 4, base - h * 0.4), (cx + w / 2, base - h * 0.7), (cx + w / 2, base)], fill=color)
    if gem:
        d.point((round(cx), round(base - h * 0.25)), fill=gem)


def coin(d, cx, cy, r, color=GOLD, edge=GOLD_DARK):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color)
    if r >= 3:
        d.ellipse([cx - r + 1.5, cy - r + 1.5, cx + r - 1.5, cy + r - 1.5], outline=edge)


def stele(d, x, y, s, color, door=None):
    """A mausoleum: a slab with a pointed top on a base; (x, y) = the middle of its base."""
    d.rectangle([x - s * 0.9, y - s * 0.4, x + s * 0.9, y], fill=color)
    d.rectangle([x - s * 0.6, y - s * 2.6, x + s * 0.6, y - s * 0.4], fill=color)
    d.polygon([(x - s * 0.75, y - s * 2.6), (x, y - s * 3.4), (x + s * 0.75, y - s * 2.6)], fill=color)
    if door:
        d.rectangle([x - s * 0.25, y - s * 1.2, x + s * 0.25, y - s * 0.4], fill=door)


def gavel(d, x, y, s, head=JET_LIGHT, band=GOLD, handle=GOLD_DARK):
    """A gavel, its head at (x, y), tilted."""
    d.line([(x, y), (x + s * 1.2, y + s * 1.2)], fill=handle, width=max(1, int(s / 3)))
    d.polygon([(x - s * 0.9, y - s * 0.1), (x - s * 0.1, y - s * 0.9), (x + s * 0.5, y - s * 0.3),
               (x - s * 0.3, y + s * 0.5)], fill=head)
    d.line([(x - s * 0.55, y - s * 0.45), (x - s * 0.05, y + s * 0.05)], fill=band)


def eye(d, cx, cy, w, h, white, iris, pupil=BLACK):
    d.polygon([(cx - w, cy), (cx - w * 0.4, cy - h), (cx + w * 0.4, cy - h), (cx + w, cy), (cx + w * 0.4, cy + h),
               (cx - w * 0.4, cy + h)], fill=white)
    d.ellipse([cx - h, cy - h, cx + h, cy + h], fill=iris)
    d.point((round(cx), round(cy)), fill=pupil)


def door(d, x0, y0, x1, y1, frame=GOLD, leaf=JET, light=None):
    d.rectangle([x0, y0, x1, y1], fill=frame)
    d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1], fill=light or leaf)
    if light:
        d.polygon([(x0 + 1, y0 + 1), (x0 + (x1 - x0) * 0.45, y0 + 3), (x0 + (x1 - x0) * 0.45, y1 + 1), (x0 + 1, y1)],
                  fill=leaf)


def section(d, x, y, s, color):
    d.arc([x - s, y - s * 2, x + s, y], 200, 520, fill=color)
    d.arc([x - s, y, x + s, y + s * 2], 20, 340, fill=color)


def clock(d, cx, cy, r, color, hands=ASH_LIGHT):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color, width=2)
    d.line([(cx, cy), (cx - r * 0.6, cy - r * 0.4)], fill=hands, width=2)
    d.line([(cx, cy), (cx + r * 0.3, cy + r * 0.8)], fill=hands)
    d.line([(cx, cy), (cx + r * 0.75, cy - r * 0.55)], fill=hands)


def bolt(d, pts, color, width=2):
    d.line(pts, fill=color, width=width)


def mask(d, cx, cy, s, color):
    d.ellipse([cx - s, cy - s * 1.25, cx + s, cy + s * 1.25], fill=color)
    for x in (cx - s * 0.45, cx + s * 0.45):
        d.rectangle([x - s * 0.22, cy - s * 0.4, x + s * 0.22, cy - s * 0.1], fill=BLACK)


# ------------------------------------------------------------------ the 32x32 wheel pictures

S = 32
C = 15.5


def wheel_base(inside, edge):
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
                    px[x, y] = (30, 20, 12, 255)
                else:
                    t = dd / 12.6
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


BRIBE_INSIDE = dict(inside=(60, 40, 18, 255), edge=(96, 66, 28, 255))        # coin-brown
FALLEN_INSIDE = dict(inside=(26, 14, 38, 255), edge=(56, 32, 74, 255))       # fallen violet
SOVEREIGN_INSIDE = dict(inside=(10, 8, 14, 255), edge=(40, 32, 26, 255))     # jet and gold


def draw(inside, fn):
    base = wheel_base(**inside)
    layer, d = canvas(S)
    fn(layer, d)
    return in_frame(base, layer)


# ---- key 3: Bribe

def w_weaken():
    def f(img, d):
        coin(d, 12, 12, 6)
        d.polygon([(19, 15), (27, 15), (23, 25)], fill=ASH_LIGHT)                         # a strength falling
        d.line([(23, 6), (23, 15)], fill=ASH_LIGHT, width=2)
    return draw(BRIBE_INSIDE, f)


def w_charm():
    def f(img, d):
        d.polygon([(15.5, 26), (5, 15), (8, 8), (13, 8), (15.5, 11), (18, 8), (23, 8), (26, 15)], fill=ROSE)  # a heart
        coin(d, 15.5, 16, 4)
    return draw(BRIBE_INSIDE, f)


def w_arrogance():
    def f(img, d):
        crown(d, 15.5, 16, 16, 10, GOLD_LIGHT, BLACK)
        d.polygon([(9, 18), (22, 18), (15.5, 27)], fill=GOLD)                             # a swelling chin, nose high
        d.line([(10, 21), (21, 21)], fill=GOLD_DARK)
    return draw(BRIBE_INSIDE, f)


def w_connect():
    def f(img, d):
        d.ellipse([4, 18, 10, 24], fill=GOLD)
        d.ellipse([21, 7, 27, 13], fill=ASH_LIGHT)
        d.line([(9, 19), (13, 15), (17, 17), (22, 11)], fill=BLACK, width=2)              # a thin black thread
        d.line([(9, 19), (13, 15), (17, 17), (22, 11)], fill=JET_LIGHT)
    return draw(BRIBE_INSIDE, f)


def w_coin_shot():
    def f(img, d):
        for i in range(4):                                                                # its flight, dotted
            x, y = 5 + i * 3.4, 26 - i * 3.4
            d.rectangle([x, y, x + 1, y + 1], fill=GOLD_LIGHT if i % 2 else GOLD)
        coin(d, 21, 11, 5, GOLD_LIGHT)
        d.line([(25, 7), (28, 4)], fill=WHITE)
    return draw(BRIBE_INSIDE, f)


# ---- key 6: the Fallen

def w_raise_mausoleum():
    def f(img, d):
        stele(d, 15.5, 27, 6, JET_LIGHT, GOLD)
        d.line([(9, 27), (22, 27)], fill=GOLD)
        for x in (6, 25):
            d.line([(x, 26), (x, 20)], fill=GOLD_LIGHT)                                    # rising
            d.point((x, 19), fill=GOLD_LIGHT)
    return draw(FALLEN_INSIDE, f)


def w_corrosion():
    def f(img, d):
        d.ellipse([6, 6, 25, 25], fill=(40, 26, 50, 255))
        coin(d, 15.5, 15.5, 6, GOLD_DARK)
        for x, y in ((9, 22), (13, 24), (20, 23), (23, 19)):                              # dripping dark
            d.line([(x, y - 3), (x, y + 1)], fill=VIOLET)
    return draw(FALLEN_INSIDE, f)


def w_step():
    def f(img, d):
        stele(d, 9, 26, 4, JET_LIGHT, GOLD)
        stele(d, 23, 26, 4, JET_LIGHT, GOLD)
        d.arc([8, 6, 24, 26], 200, 340, fill=GOLD_LIGHT, width=2)                          # through one, out the other
        d.polygon([(22, 10), (25, 13), (21, 14)], fill=GOLD_LIGHT)
    return draw(FALLEN_INSIDE, f)


def w_majesty():
    def f(img, d):
        crown(d, 15.5, 13, 16, 9, GOLD, BLACK)
        for i in range(3):                                                                # pressing down
            d.line([(7 + i * 6, 17), (7 + i * 6, 25)], fill=GOLD_LIGHT)
            d.polygon([(5 + i * 6, 23), (9 + i * 6, 23), (7 + i * 6, 27)], fill=GOLD_LIGHT)
    return draw(FALLEN_INSIDE, f)


def w_bestowment():
    def f(img, d):
        mask(d, 15.5, 15, 8, VIOLET_LIGHT)
        d.arc([10, 18, 21, 24], 200, 340, fill=BLACK)
    return draw(FALLEN_INSIDE, f)


def w_exploit():
    def f(img, d):
        d.rectangle([5, 9, 26, 22], outline=GOLD, width=2)                                # a rule...
        d.rectangle([13, 9, 18, 13], fill=FALLEN_INSIDE['inside'])                        # ...with a gap in it
        d.polygon([(15.5, 4), (19, 9), (12, 9)], fill=GOLD_LIGHT)                         # slipped through
        d.line([(15.5, 9), (15.5, 18)], fill=GOLD_LIGHT, width=2)
    return draw(FALLEN_INSIDE, f)


def w_magnify():
    def f(img, d):
        d.ellipse([5, 5, 21, 21], outline=GOLD, width=3)
        d.ellipse([8, 8, 18, 18], fill=(120, 90, 170, 255))
        crown(d, 13, 16, 8, 6, GOLD_LIGHT)
        d.line([(19, 19), (26, 26)], fill=GOLD_DARK, width=3)
    return draw(FALLEN_INSIDE, f)


def w_mausoleum_salvo():
    def f(img, d):
        stele(d, 7, 27, 3, JET_LIGHT, GOLD)
        stele(d, 15.5, 27, 3, JET_LIGHT, GOLD)
        stele(d, 24, 27, 3, JET_LIGHT, GOLD)
        for x in (7, 15.5, 24):                                                            # beams meeting
            d.line([(x, 16), (15.5, 5)], fill=GOLD_LIGHT)
        d.ellipse([13, 3, 18, 8], fill=WHITE)
    return draw(FALLEN_INSIDE, f)


# ---- key 7: Sovereign

def w_ruin():
    def f(img, d):
        d.line([(4, 26), (27, 5)], fill=GOLD, width=5)
        d.line([(4, 26), (27, 5)], fill=WHITE, width=1)
        for x, y in ((10, 13), (21, 22)):
            d.line([(x - 2, y), (x + 2, y)], fill=VIOLET_LIGHT)
    return draw(SOVEREIGN_INSIDE, f)


def w_frenzy():
    def f(img, d):
        for a in range(6):
            ang = math.tau * a / 6 + 0.3
            pts = [(15.5 + math.cos(ang) * r + math.sin(r) * 1.5, 15.5 + math.sin(ang) * r) for r in (3, 6, 9, 12)]
            bolt(d, pts, VIOLET_LIGHT, 1)
        d.ellipse([12, 12, 19, 19], fill=BLACK)
    return draw(SOVEREIGN_INSIDE, f)


def w_entropy():
    def f(img, d):
        clock(d, 15.5, 15.5, 9, ASH_LIGHT)
        for x, y in ((6, 7), (25, 9), (24, 24), (7, 23)):                                   # pieces coming loose
            d.rectangle([x, y, x + 1, y + 1], fill=ASH)
    return draw(SOVEREIGN_INSIDE, f)


def w_abolition():
    def f(img, d):
        d.ellipse([5, 5, 26, 26], fill=FOG)
        d.ellipse([8, 8, 23, 23], fill=(150, 150, 156, 255))
        d.line([(9, 9), (22, 22)], fill=BLACK, width=2)                                    # struck out
        d.line([(22, 9), (9, 22)], fill=BLACK, width=2)
    return draw(SOVEREIGN_INSIDE, f)


def w_exile():
    def f(img, d):
        d.rectangle([12, 7, 19, 13], fill=ASH_LIGHT)                                        # a figure...
        d.rectangle([11, 14, 20, 26], fill=ASH_LIGHT)
        for y in range(7, 27, 3):                                                           # ...fading out of the world
            d.line([(8, y), (23, y)], fill=SOVEREIGN_INSIDE['inside'])
        d.arc([4, 4, 27, 27], 300, 60, fill=FOG, width=2)
    return draw(SOVEREIGN_INSIDE, f)


def w_inversion():
    def f(img, d):
        crown(d, 15.5, 15, 14, 8, GOLD)
        up, ud = canvas(S)
        crown(ud, 15.5, 15, 14, 8, JET_LIGHT)
        img.alpha_composite(up.transpose(Image.FLIP_TOP_BOTTOM))                    # and its shadow, upside down
        d.line([(6, 16), (25, 16)], fill=WHITE)
    return draw(SOVEREIGN_INSIDE, f)


def w_coronation():
    def f(img, d):
        crown(d, 15.5, 20, 20, 13, GOLD, BLACK)
        d.rectangle([5, 20, 26, 23], fill=GOLD_DARK)
        for x in (4, 27):
            d.line([(x, 10), (x, 14)], fill=GOLD_LIGHT)
        d.point((15, 4), fill=WHITE)
    return draw(SOVEREIGN_INSIDE, f)


def w_decree():
    def f(img, d):
        d.line([(15.5, 4), (15.5, 27)], fill=GOLD_LIGHT, width=3)
        d.line([(8, 10), (23, 10)], fill=GOLD_LIGHT, width=2)
        d.arc([8, 16, 17, 26], 90, 270, fill=GOLD_LIGHT, width=2)
        section(d, 21, 19, 2, WHITE)
    return draw(SOVEREIGN_INSIDE, f)


WHEEL = {
    'weaken': w_weaken, 'charm': w_charm, 'arrogance': w_arrogance, 'connect': w_connect, 'coin_shot': w_coin_shot,
    'raise_mausoleum': w_raise_mausoleum, 'corrosion': w_corrosion, 'step': w_step, 'majesty': w_majesty,
    'bestowment': w_bestowment, 'exploit': w_exploit, 'magnify': w_magnify, 'mausoleum_salvo': w_mausoleum_salvo,
    'ruin': w_ruin, 'frenzy': w_frenzy, 'entropy': w_entropy, 'abolition': w_abolition, 'exile': w_exile,
    'inversion': w_inversion, 'coronation': w_coronation, 'decree': w_decree,
}


# ------------------------------------------------------------------ the 16x16 status pictures

def s(fn):
    img, d = canvas(16)
    fn(img, d)
    return outlined(img)


# what the Beyonder holds
def i_iron_will(img, d):
    d.polygon([(2, 2), (13, 2), (13, 8), (7.5, 14), (2, 8)], fill=ASH)                     # a shield...
    d.rectangle([6, 4, 9, 10], fill=GOLD)                                                  # ...with a bar of iron


def i_seal(img, d):
    door(d, 3, 1, 12, 14, GOLD, JET)
    d.point((10, 8), fill=GOLD_LIGHT)


def i_paces(img, d):
    for i, x in enumerate((2, 7, 12)):
        d.ellipse([x, 10 - i * 3, x + 3, 14 - i * 3], fill=ASH_LIGHT if i < 2 else WHITE)


def i_connect(img, d):
    d.line([(1, 13), (5, 9), (9, 11), (14, 3)], fill=JET_LIGHT, width=2)
    d.point((14, 3), fill=GOLD)
    d.point((1, 13), fill=GOLD)


def i_corrosion(img, d):
    coin(d, 7.5, 6.5, 5, GOLD_DARK)
    d.line([(5, 11), (5, 14)], fill=VIOLET)
    d.line([(10, 11), (10, 15)], fill=VIOLET)


def i_exploit(img, d):
    d.rectangle([1, 4, 14, 11], outline=GOLD)
    d.rectangle([6, 4, 9, 6], fill=(0, 0, 0, 0))
    d.line([(7.5, 0), (7.5, 9)], fill=GOLD_LIGHT, width=2)


def i_magnify(img, d):
    d.ellipse([1, 1, 11, 11], outline=GOLD, width=2)
    d.line([(10, 10), (14, 14)], fill=GOLD_DARK, width=2)
    d.point((6, 6), fill=WHITE)


def i_entropy(img, d):
    clock(d, 7.5, 7.5, 6, ASH_LIGHT)


def i_abolition(img, d):
    d.ellipse([1, 1, 14, 14], fill=FOG)
    d.line([(4, 4), (11, 11)], fill=BLACK)
    d.line([(11, 4), (4, 11)], fill=BLACK)


def i_exile(img, d):
    d.rectangle([5, 2, 10, 6], fill=FOG)
    d.rectangle([4, 7, 11, 14], fill=FOG)
    for y in (4, 8, 11):
        d.line([(3, y), (12, y)], fill=(0, 0, 0, 0))


def i_inversion(img, d):
    crown(d, 7.5, 7, 12, 6, GOLD)
    crown(d, 7.5, 9, 12, -6, JET_LIGHT)


def i_coronation(img, d):
    crown(d, 7.5, 12, 14, 10, GOLD, BLACK)


def i_sanctuary(img, d):
    stele(d, 7.5, 14, 3, JET_LIGHT, GOLD)
    d.arc([0, 3, 15, 18], 200, 340, fill=GOLD_LIGHT)


def i_rebirth(img, d):
    stele(d, 7.5, 15, 3, JET_LIGHT)
    d.polygon([(7.5, 0), (11, 4), (4, 4)], fill=GOLD_LIGHT)                                # rising from it


# what is done to others
def i_compromised(img, d):
    d.line([(7.5, 2), (7.5, 13)], fill=GOLD)
    d.line([(2, 4), (13, 7)], fill=GOLD)
    d.chord([0, 5, 5, 10], 0, 180, fill=GOLD)
    d.chord([10, 8, 15, 13], 0, 180, fill=GOLD)
    d.rectangle([5, 13, 10, 14], fill=GOLD)


def i_brainwashed(img, d):
    d.ellipse([2, 2, 13, 13], fill=VIOLET_LIGHT)
    d.arc([4, 4, 11, 11], 0, 270, fill=VIOLET)
    d.arc([6, 6, 9, 9], 90, 360, fill=VIOLET)


def i_weakened(img, d):
    coin(d, 5, 5, 4, GOLD_DARK)
    d.polygon([(9, 9), (15, 9), (12, 15)], fill=ASH_LIGHT)


def i_charmed(img, d):
    d.polygon([(7.5, 14), (1, 7), (3, 2), (6, 2), (7.5, 4), (9, 2), (12, 2), (14, 7)], fill=ROSE)


def i_arrogant(img, d):
    crown(d, 7.5, 9, 12, 7, GOLD_LIGHT)
    d.polygon([(3, 10), (12, 10), (7.5, 15)], fill=GOLD)


def i_distorted(img, d):
    d.arc([1, 1, 14, 14], 30, 300, fill=VIOLET_LIGHT, width=2)
    d.polygon([(13, 3), (15, 8), (10, 7)], fill=VIOLET_LIGHT)


def i_disordered(img, d):
    clock(d, 7.5, 7.5, 6, ASH, ASH_LIGHT)
    d.point((2, 13), fill=ASH_LIGHT)
    d.point((13, 2), fill=ASH_LIGHT)


def i_corroded(img, d):
    d.ellipse([2, 1, 13, 12], fill=(60, 30, 70, 255))
    d.line([(5, 11), (5, 15)], fill=VIOLET)
    d.line([(10, 11), (10, 14)], fill=VIOLET)
    d.point((7, 5), fill=GOLD)


def i_kneeling(img, d):
    crown(d, 7.5, 6, 12, 5, GOLD)
    d.line([(7.5, 7), (7.5, 13)], fill=GOLD_LIGHT, width=2)
    d.polygon([(4, 11), (11, 11), (7.5, 15)], fill=GOLD_LIGHT)


def i_sluggish(img, d):
    mask(d, 7.5, 7.5, 5.5, BLUE)


def i_anxious(img, d):
    mask(d, 7.5, 7.5, 5.5, VIOLET_LIGHT)
    d.line([(2, 13), (4, 11), (6, 13), (8, 11), (10, 13), (12, 11)], fill=WHITE)


def i_defeated(img, d):
    mask(d, 7.5, 7.5, 5.5, ASH)
    d.arc([4, 9, 11, 14], 200, 340, fill=BLACK)


def i_rash(img, d):
    mask(d, 7.5, 7.5, 5.5, RED_LIGHT)
    d.line([(4, 3), (6, 4)], fill=BLACK)
    d.line([(11, 3), (9, 4)], fill=BLACK)


def i_avaricious(img, d):
    mask(d, 7.5, 7.5, 5.5, GOLD)
    coin(d, 12, 12, 3, GOLD_LIGHT)


STATUS = {
    'iron_will': i_iron_will, 'seal': i_seal, 'paces': i_paces, 'connect': i_connect, 'corrosion': i_corrosion,
    'exploit': i_exploit, 'magnify': i_magnify, 'entropy': i_entropy, 'abolition': i_abolition, 'exile': i_exile,
    'inversion': i_inversion, 'coronation': i_coronation, 'sanctuary': i_sanctuary, 'rebirth': i_rebirth,
    'compromised': i_compromised, 'brainwashed': i_brainwashed, 'weakened': i_weakened, 'charmed': i_charmed,
    'arrogant': i_arrogant, 'distorted': i_distorted, 'disordered': i_disordered, 'corroded': i_corroded,
    'kneeling': i_kneeling, 'sluggish': i_sluggish, 'anxious': i_anxious, 'defeated': i_defeated, 'rash': i_rash,
    'avaricious': i_avaricious,
}


def order_sigil():
    """The Order counter's sigil: a small black mausoleum with a gold crown above it."""
    img, d = canvas(16)
    stele(d, 7.5, 15, 3.2, JET_LIGHT, GOLD)
    crown(d, 7.5, 4, 9, 4, GOLD, None)
    return outlined(img)


def pathway_icon():
    """A gold crown over a black mausoleum, on jet."""
    img, d = canvas(16)
    d.ellipse([0, 0, 15, 15], fill=JET)
    stele(d, 7.5, 14, 2.6, JET_LIGHT, GOLD)
    crown(d, 7.5, 6, 11, 5, GOLD, BLACK)
    return outlined(img)


def main(assets, preview=None):
    out = []
    for name, fn in WHEEL.items():
        path = os.path.join(assets, 'textures/gui/ability_menus/prince_of_abolition', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fn().save(path)
        out.append(path)
    for name, fn in STATUS.items():
        path = os.path.join(assets, 'textures/gui/prince_of_abolition', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        s(fn).save(path)
        out.append(path)
    path = os.path.join(assets, 'textures/gui/prince_of_abolition/order.png')
    order_sigil().save(path)
    out.append(path)
    path = os.path.join(assets, 'textures/gui/pathway_icons/prince_of_abolition.png')
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
