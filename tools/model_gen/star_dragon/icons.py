"""Screen pictures for the Star Dragon (Condenser pathway).

python3 tools/model_gen/star_dragon/icons.py src/main/resources/assets/circle_of_inevitability [preview.png]

  textures/gui/ability_menus/star_dragon/*.png   32x32, the three wheels (Star Sacrifice on key 2, Heavybringer on
                                                 key 5, Cosmos on key 7), in a round frame of starlight with four
                                                 points like a star's
  textures/gui/star_dragon/*.png                 16x16, the statuses shown above the hotbar
  textures/gui/pathway_icons/star_dragon.png     16x16, the Pathway's icon beside the hotbar

Every picture is drawn in flat colors and then outlined automatically, so the shapes stay simple to change.
Replace any of them with a hand-drawn PNG of the same name.
"""
import math
import os
import sys

from PIL import Image, ImageDraw

# ------------------------------------------------------------------ colors
OUTLINE = (10, 8, 24, 255)
STAR = (244, 246, 255, 255)
STAR_DIM = (180, 184, 220, 255)
VIOLET = (106, 92, 255, 255)
VIOLET_LIGHT = (184, 176, 255, 255)
VIOLET_DARK = (52, 40, 140, 255)
VOID = (16, 12, 40, 255)
VOID_LIGHT = (40, 32, 90, 255)
GREEN = (46, 138, 74, 255)
GREEN_LIGHT = (124, 224, 138, 255)
GREEN_DARK = (20, 70, 36, 255)
BLUE = (168, 216, 255, 255)
BLUE_DEEP = (70, 130, 220, 255)
GOLD = (255, 233, 176, 255)
GOLD_DEEP = (240, 170, 60, 255)
ORANGE = (255, 130, 50, 255)
TEAL = (140, 216, 208, 255)
TEAL_DARK = (40, 106, 112, 255)
FOAM = (230, 252, 250, 255)
ROCK = (90, 80, 74, 255)
ROCK_DARK = (50, 44, 42, 255)
RAD = (200, 255, 106, 255)
RAD_DARK = (110, 160, 40, 255)
FROST = (200, 236, 255, 255)
RED = (220, 70, 70, 255)
BLACK = (0, 0, 0, 255)
WHITE = (255, 255, 255, 255)


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


def wheel_base(inside=(14, 10, 40, 255), edge=(46, 36, 110, 255)):
    """A round frame of starlight with four points (a four-pointed star's) round a field of night with a few stars."""
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = im.load()
    for y in range(S):
        for x in range(S):
            dd = math.hypot(x - C, y - C)
            a = math.atan2(y - C, x - C)
            point = max(0.0, math.cos(a * 4)) ** 16                           # four points, N E S W
            outer = 14.2 + 1.6 * point
            if dd <= outer:
                if dd > 13.0:
                    light = (x - C) * -0.7 + (y - C) * -0.7
                    px[x, y] = STAR if light > 6 else STAR_DIM if light > -6 else VIOLET
                elif dd > 12.3:
                    px[x, y] = VIOLET_DARK
                else:
                    t = dd / 12.3
                    col = tuple(int(inside[i] * (1 - t) + edge[i] * t) for i in range(3)) + (255,)
                    if (x * 7 + y * 13) % 23 == 0:
                        col = (150, 150, 200, 255)
                    px[x, y] = col
    return im


def in_frame(base, layer):
    """The drawing, outlined, laid into the frame and cut to its inside."""
    layer = outlined(layer)
    lp = layer.load()
    bp = base.load()
    for y in range(S):
        for x in range(S):
            if lp[x, y][3] and math.hypot(x - C, y - C) <= 12.3:
                bp[x, y] = lp[x, y]
    return base


SACRIFICE = dict(inside=(14, 10, 40, 255), edge=(46, 36, 110, 255))
HEAVY = dict(inside=(8, 6, 22, 255), edge=(30, 24, 70, 255))
COSMOS = dict(inside=(20, 10, 30, 255), edge=(80, 40, 90, 255))


def w(inside):
    def deco(fn):
        def make():
            base = wheel_base(**inside)
            layer, d = canvas(S)
            fn(d)
            return in_frame(base, layer)
        return make
    return deco


def rings(d, cx, cy, radii, fill):
    for r in radii:
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=fill)


def chevron(d, cx, cy, s, fill):
    d.line([(cx - s, cy - s), (cx, cy), (cx + s, cy - s)], fill=fill)


def ray(d, x0, y0, x1, y1, core, sheath, width=3):
    d.line([(x0, y0), (x1, y1)], fill=sheath, width=width)
    d.line([(x0, y0), (x1, y1)], fill=core)


def rock(d, cx, cy, r, fill=ROCK, hot=GOLD_DEEP):
    pts = []
    for i in range(9):
        a = math.tau * i / 9
        rr = r * (0.8 + 0.25 * math.sin(i * 2.3))
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.polygon(pts, fill=fill)
    d.point((cx - 1, cy - 1), fill=ROCK_DARK)
    d.line([(cx + 1, cy), (cx + r * 0.6, cy + r * 0.4)], fill=hot)


# ---- key 2: Star Sacrifice

@w(SACRIFICE)
def w_gravity_abnormality(d):
    for x in (9, 15.5, 22):                                                     # pressed down
        d.line([(x, 6), (x, 15)], fill=VIOLET_LIGHT)
        chevron(d, x, 17, 3, VIOLET_LIGHT)
    d.ellipse([5, 21, 26, 27], fill=VOID_LIGHT)                                 # the well
    d.ellipse([10, 22.5, 21, 25.5], fill=VIOLET)
    figure(d, 15.5, 17, 1.6, STAR_DIM)


@w(SACRIFICE)
def w_electromagnetic_attraction(d):
    figure(d, 7, 10, 2.2, STAR_DIM)
    figure(d, 24, 10, 2.2, STAR_DIM)
    for k in range(3):                                                          # field lines between them
        d.arc([8, 8 + k * 3, 23, 22 - k * 3], 200, 340, fill=BLUE)
    d.line([(12, 15), (14, 13), (16, 16), (18, 13), (20, 15)], fill=STAR)


@w(SACRIFICE)
def w_cosmic_void(d):
    d.ellipse([6, 6, 25, 25], fill=VOID)
    d.ellipse([6, 6, 25, 25], outline=VIOLET_LIGHT)
    for x, y in ((11, 11), (19, 13), (14, 19), (21, 20), (16, 9)):
        d.point((x, y), fill=STAR)
    star(d, 15.5, 15.5, 2.5, STAR_DIM)


@w(SACRIFICE)
def w_weakening_ray(d):
    ray(d, 4, 24, 26, 8, GREEN_LIGHT, GREEN, 3)
    star(d, 5, 23, 3, GREEN_LIGHT)
    for x, y in ((22, 16), (24, 19), (20, 21)):                                 # drips off the target
        d.line([(x, y), (x, y + 3)], fill=GREEN)


@w(SACRIFICE)
def w_sunlight_ray(d):
    d.ellipse([3, 5, 12, 14], fill=GOLD)
    for k in range(8):
        a = math.tau * k / 8
        d.line([(7.5 + math.cos(a) * 6, 9.5 + math.sin(a) * 6), (7.5 + math.cos(a) * 8, 9.5 + math.sin(a) * 8)],
               fill=GOLD_DEEP)
    d.polygon([(10, 12), (28, 22), (26, 27), (8, 15)], fill=GOLD)
    d.line([(10, 13), (27, 24)], fill=WHITE)


# ---- key 5: Heavybringer

@w(HEAVY)
def w_heavybringing(d):
    rings(d, 15.5, 15.5, (11, 8, 5), VIOLET)
    for k in range(6):                                                          # drawn in
        a = math.tau * k / 6
        d.line([(15.5 + math.cos(a) * 11, 15.5 + math.sin(a) * 11), (15.5 + math.cos(a + 0.5) * 6,
                                                                      15.5 + math.sin(a + 0.5) * 6)], fill=VIOLET_LIGHT)
    figure(d, 15.5, 10, 2.4, STAR)


@w(HEAVY)
def w_fate_convergence(d):
    for k in range(10):
        a = math.tau * k / 10
        d.line([(15.5 + math.cos(a) * 12, 15.5 + math.sin(a) * 12), (15.5, 15.5)], fill=VIOLET)
    for k in range(5):
        a = math.tau * k / 5 + 0.3
        star(d, 15.5 + math.cos(a) * 8, 15.5 + math.sin(a) * 8, 1.6, STAR)
    star(d, 15.5, 15.5, 4, GOLD)


@w(HEAVY)
def w_heavy_sphere(d):
    d.ellipse([5, 5, 26, 26], fill=VOID_LIGHT)
    d.ellipse([5, 5, 26, 26], outline=VIOLET_LIGHT)
    d.line([(9, 9), (22, 22)], fill=VIOLET)
    d.line([(22, 9), (9, 22)], fill=VIOLET)
    figure(d, 15.5, 11, 2.0, STAR_DIM)
    d.line([(1, 15), (4, 15)], fill=STAR)                                       # a shot stopped at the shell


@w(HEAVY)
def w_series_of_rays(d):
    for k in range(6):
        y = 6 + k * 3.5
        ray(d, 4, 24 - k * 0.8, 26, y, GREEN_LIGHT, GREEN_DARK, 1)
    star(d, 4, 22, 3, GREEN_LIGHT)


@w(HEAVY)
def w_remote_sight(d):
    d.polygon([(4, 15.5), (15.5, 8), (27, 15.5), (15.5, 23)], fill=STAR_DIM)
    d.ellipse([11, 11, 20, 20], fill=VIOLET)
    star(d, 15.5, 15.5, 3, STAR)
    d.line([(4, 27), (10, 25), (16, 27), (22, 25), (28, 27)], fill=VIOLET_LIGHT)  # a passage far off


# ---- key 7: Cosmos

@w(COSMOS)
def w_fusion_cluster(d):
    for cx, cy in ((10, 11), (21, 11), (15.5, 21)):
        d.ellipse([cx - 4, cy - 4, cx + 4, cy + 4], outline=VIOLET_LIGHT)
        d.ellipse([cx - 2, cy - 2, cx + 2, cy + 2], fill=GOLD)
        d.point((cx, cy), fill=WHITE)


@w(COSMOS)
def w_radiant_angel_form(d):
    for k, a in enumerate((-50, -25, 0, 25, 50)):
        rad = math.radians(a)
        for sign in (-1, 1):
            d.line([(15.5, 14), (15.5 + sign * math.cos(rad) * 12, 14 - math.sin(rad) * 10)], fill=GOLD, width=2)
    figure(d, 15.5, 8, 2.6, WHITE)
    d.ellipse([12, 4, 19, 7], outline=GOLD_DEEP)


@w(COSMOS)
def w_time_dilation(d):
    d.ellipse([5, 5, 26, 26], outline=VIOLET_LIGHT)
    d.ellipse([9, 9, 22, 22], outline=VIOLET)
    d.line([(15.5, 15.5), (15.5, 7)], fill=STAR)
    d.line([(15.5, 15.5), (21, 18)], fill=STAR_DIM)
    for k in range(12):
        a = math.tau * k / 12
        d.point((15.5 + math.cos(a) * 9.5, 15.5 + math.sin(a) * 9.5), fill=STAR)


@w(COSMOS)
def w_planetfall(d):
    d.ellipse([2, 22, 29, 30], fill=VOID_LIGHT)
    d.polygon([(10, 4), (24, 6), (24, 14), (12, 16)], fill=ORANGE)              # its fire
    rock(d, 15.5, 14, 8)
    d.arc([2, 22, 29, 30], 180, 360, fill=STAR)


@w(COSMOS)
def w_star_dragon_form(d):
    pts = [(25, 8), (21, 12), (17, 11), (13, 15), (10, 20), (7, 24)]
    d.line(pts, fill=VIOLET_LIGHT, width=4)
    d.line(pts, fill=STAR_DIM)
    d.polygon([(21, 12), (27, 4), (24, 14)], fill=VIOLET)                       # a wing
    d.polygon([(25, 6), (30, 8), (26, 10)], fill=VIOLET_LIGHT)                  # the head
    d.point((27, 7), fill=GOLD)
    star(d, 9, 9, 2.4, STAR)


@w(COSMOS)
def w_neutron_star(d):
    d.line([(9, 4), (22, 27)], fill=BLUE, width=3)                              # the pulsar's beams
    d.line([(9, 4), (22, 27)], fill=WHITE)
    d.ellipse([11, 11, 20, 20], fill=BLUE)
    d.ellipse([13, 13, 18, 18], fill=WHITE)
    for x in (6, 25):
        chevron(d, x, 25, 2, VIOLET_LIGHT)


@w(COSMOS)
def w_singularity(d):
    d.ellipse([3, 11, 28, 20], fill=GOLD_DEEP)                                  # the disc
    d.ellipse([6, 12.5, 25, 18.5], fill=GOLD)
    d.ellipse([10, 9, 21, 22], fill=BLACK)
    d.arc([9, 8, 22, 23], 200, 340, fill=WHITE)


@w(COSMOS)
def w_quantum_uncertainty(d):
    for k, (x, y) in enumerate(((9, 10), (16, 8), (22, 13), (12, 18), (20, 21), (8, 23), (24, 24))):
        d.ellipse([x - 1.5, y - 1.5, x + 1.5, y + 1.5], fill=STAR if k % 2 else VIOLET_LIGHT)
    d.arc([5, 5, 26, 26], 0, 360, fill=VIOLET)
    d.text((13, 11), '?', fill=STAR)


WHEEL = {
    'gravity_abnormality': w_gravity_abnormality,
    'electromagnetic_attraction': w_electromagnetic_attraction,
    'cosmic_void': w_cosmic_void,
    'weakening_ray': w_weakening_ray,
    'sunlight_ray': w_sunlight_ray,
    'heavybringing': w_heavybringing,
    'fate_convergence': w_fate_convergence,
    'heavy_sphere': w_heavy_sphere,
    'series_of_rays': w_series_of_rays,
    'remote_sight': w_remote_sight,
    'fusion_cluster': w_fusion_cluster,
    'radiant_angel_form': w_radiant_angel_form,
    'time_dilation': w_time_dilation,
    'planetfall': w_planetfall,
    'star_dragon_form': w_star_dragon_form,
    'neutron_star': w_neutron_star,
    'singularity': w_singularity,
    'quantum_uncertainty': w_quantum_uncertainty,
}


# ------------------------------------------------------------------ the 16x16 status pictures

def s(fn):
    img, d = canvas(16)
    fn(d)
    return outlined(img)


# what the Star Dragon holds

def i_fate_glimpse(d):
    d.polygon([(1, 7.5), (7.5, 3), (14, 7.5), (7.5, 12)], fill=STAR_DIM)
    star(d, 7.5, 7.5, 3.5, VIOLET)
    d.point((7, 7), fill=WHITE)


def i_lightened(d):
    figure(d, 7.5, 1, 2.4, STAR)
    for x in (3, 12):
        d.line([(x, 13), (x, 8)], fill=BLUE)
        d.polygon([(x - 1, 9), (x + 1, 9), (x, 7)], fill=BLUE)


def i_scaled_form(d):
    for y in (3, 7, 11):
        for x in (3, 7, 11):
            d.pieslice([x - 3 + (y // 4) % 2, y - 3, x + 3 + (y // 4) % 2, y + 3], 0, 180, fill=TEAL)
    d.ellipse([6, 5, 9, 10], fill=GOLD)
    d.line([(7.5, 5), (7.5, 9)], fill=BLACK)


def i_heavy_body(d):
    d.rectangle([3, 2, 12, 12], fill=TEAL_DARK)
    d.rectangle([4, 3, 11, 8], fill=TEAL)
    d.rectangle([1, 13, 14, 14], fill=ROCK)
    chevron(d, 7.5, 11, 3, FOAM)


def i_heavybringing(d):
    rings(d, 7.5, 7.5, (6.5, 4), VIOLET)
    d.ellipse([5.5, 5.5, 9.5, 9.5], fill=STAR)


def i_fate_convergence(d):
    for k in range(8):
        a = math.tau * k / 8
        d.line([(7.5 + math.cos(a) * 7, 7.5 + math.sin(a) * 7), (7.5, 7.5)], fill=VIOLET)
    star(d, 7.5, 7.5, 3, GOLD)


def i_remote_sight(d):
    d.polygon([(0, 7.5), (7.5, 2), (15, 7.5), (7.5, 13)], fill=STAR_DIM)
    d.ellipse([5, 5, 10, 10], fill=VIOLET)
    d.point((7, 7), fill=WHITE)


def i_radiant_angel(d):
    for a in (-40, 0, 40):
        rad = math.radians(a)
        for sign in (-1, 1):
            d.line([(7.5, 8), (7.5 + sign * math.cos(rad) * 7, 8 - math.sin(rad) * 6)], fill=GOLD)
    figure(d, 7.5, 3, 1.8, WHITE)


def i_star_dragon(d):
    d.line([(14, 3), (10, 6), (6, 7), (3, 11), (1, 14)], fill=VIOLET_LIGHT, width=3)
    d.polygon([(10, 6), (13, 0), (12, 8)], fill=VIOLET)
    d.point((14, 3), fill=GOLD)


def i_neutron_star(d):
    d.line([(4, 1), (11, 14)], fill=BLUE)
    d.ellipse([4.5, 4.5, 10.5, 10.5], fill=BLUE)
    d.ellipse([6, 6, 9, 9], fill=WHITE)


def i_quantum(d):
    for x, y in ((3, 4), (8, 2), (12, 6), (5, 10), (11, 12), (2, 13)):
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=STAR)
    d.arc([1, 1, 14, 14], 0, 360, fill=VIOLET)


def i_form_weakness(d):
    figure(d, 7.5, 3, 2.2, STAR_DIM)
    d.line([(2, 2), (13, 13)], fill=VIOLET_DARK, width=2)


def i_spent(d):
    d.ellipse([3, 3, 12, 12], fill=BLACK)
    d.arc([2, 2, 13, 13], 200, 340, fill=GOLD)
    d.line([(2, 13), (13, 2)], fill=RED)


def i_unnoticed(d):
    d.polygon([(7.5, 0), (11, 7.5), (7.5, 15), (4, 7.5)], fill=VOID)
    d.polygon([(7.5, 0), (11, 7.5), (7.5, 15), (4, 7.5)], outline=VIOLET_LIGHT)
    d.point((7, 5), fill=STAR)
    d.point((8, 10), fill=STAR)


# what is done to others (when they are players)

def i_charted(d):
    pts = [(2, 11), (5, 5), (9, 7), (13, 2)]
    d.line(pts, fill=STAR_DIM)
    for p in pts:
        d.point(p, fill=WHITE)
    star(d, 9, 7, 2.5, STAR)


def i_pressed(d):
    for x in (3, 7.5, 12):
        d.line([(x, 1), (x, 7)], fill=VIOLET_LIGHT)
        chevron(d, x, 9, 2, VIOLET_LIGHT)
    d.rectangle([1, 12, 14, 14], fill=ROCK)


def i_lifted(d):
    figure(d, 7.5, 1, 2.2, STAR_DIM)
    d.ellipse([3, 12, 12, 14], fill=VIOLET_LIGHT)
    d.line([(1, 9), (1, 4)], fill=BLUE)
    d.line([(14, 9), (14, 4)], fill=BLUE)


def i_heavy(d):
    chevron(d, 7.5, 6, 5, VIOLET_LIGHT)
    chevron(d, 7.5, 11, 5, VIOLET)
    d.rectangle([2, 13, 13, 14], fill=ROCK)


def i_voided(d):
    d.ellipse([1, 1, 14, 14], fill=VOID)
    d.ellipse([1, 1, 14, 14], outline=VIOLET_LIGHT)
    d.point((5, 5), fill=STAR)
    d.point((10, 9), fill=STAR)


def i_weakened(d):
    d.ellipse([2, 1, 13, 11], outline=GREEN_LIGHT)
    for x in (4, 7.5, 11):
        d.line([(x, 9), (x, 14)], fill=GREEN)


def i_melting(d):
    d.polygon([(2, 2), (13, 2), (12, 8), (10, 14), (8, 9), (6, 13), (3, 8)], fill=GREEN)
    d.line([(4, 4), (11, 4)], fill=GREEN_LIGHT)


def i_dimmed(d):
    d.ellipse([3, 3, 12, 12], fill=BLUE_DEEP)
    d.pieslice([3, 3, 12, 12], 270, 90, fill=VOID_LIGHT)
    d.line([(1, 14), (14, 1)], fill=STAR_DIM)


def i_misfortune(d):
    star(d, 9, 6, 5, STAR_DIM, 5, 0.45)
    d.line([(1, 14), (5, 10)], fill=VIOLET_LIGHT)
    d.line([(5, 1), (12, 11)], fill=VIOLET_DARK)


def i_narrowed(d):
    star(d, 7.5, 7, 6, RED, 5, 0.45)
    d.line([(7.5, 0), (7.5, 15)], fill=WHITE)


def i_irradiated(d):
    d.ellipse([5.5, 5.5, 9.5, 9.5], fill=RAD)
    for k in range(3):
        a = math.tau * k / 3 - math.pi / 2
        d.pieslice([1, 1, 14, 14], math.degrees(a) - 30, math.degrees(a) + 30, fill=RAD_DARK)
    d.ellipse([5.5, 5.5, 9.5, 9.5], fill=RAD)


def i_dilated(d):
    d.ellipse([1, 1, 14, 14], outline=VIOLET_LIGHT)
    d.line([(7.5, 7.5), (7.5, 3)], fill=STAR)
    d.line([(7.5, 7.5), (10, 9)], fill=STAR_DIM)
    d.polygon([(4, 12), (11, 12), (7.5, 9)], fill=FROST)


def i_sealed(d):
    d.ellipse([1, 1, 14, 14], fill=VOID_LIGHT)
    d.ellipse([1, 1, 14, 14], outline=VIOLET_LIGHT)
    d.rectangle([5, 7, 10, 11], fill=STAR_DIM)
    d.arc([5, 4, 10, 10], 180, 360, fill=STAR_DIM)


def i_uncertain(d):
    figure(d, 5, 2, 1.8, STAR_DIM)
    figure(d, 10.5, 3, 1.8, VIOLET_LIGHT)


STATUS = {
    'fate_glimpse': i_fate_glimpse,
    'lightened': i_lightened,
    'scaled_form': i_scaled_form,
    'heavy_body': i_heavy_body,
    'heavybringing': i_heavybringing,
    'fate_convergence': i_fate_convergence,
    'remote_sight': i_remote_sight,
    'radiant_angel': i_radiant_angel,
    'star_dragon': i_star_dragon,
    'neutron_star': i_neutron_star,
    'quantum': i_quantum,
    'form_weakness': i_form_weakness,
    'spent': i_spent,
    'unnoticed': i_unnoticed,
    'charted': i_charted,
    'pressed': i_pressed,
    'lifted': i_lifted,
    'heavy': i_heavy,
    'voided': i_voided,
    'weakened': i_weakened,
    'melting': i_melting,
    'dimmed': i_dimmed,
    'misfortune': i_misfortune,
    'narrowed': i_narrowed,
    'irradiated': i_irradiated,
    'dilated': i_dilated,
    'sealed': i_sealed,
    'uncertain': i_uncertain,
}


def pathway_icon():
    """A dragon of starlight coiled round a four-pointed star, on violet."""
    img, d = canvas(16)
    d.ellipse([0, 0, 15, 15], fill=VIOLET_DARK)
    d.arc([1, 1, 14, 14], 120, 420, fill=VIOLET_LIGHT, width=2)               # the dragon's coil
    d.polygon([(3, 3), (6, 1), (6, 4)], fill=STAR)                             # its head
    d.point((5, 2), fill=GOLD)
    star(d, 7.5, 7.5, 5, STAR, 4, 0.32)
    return outlined(img)


def main(assets, preview=None):
    out = []
    for name, fn in WHEEL.items():
        path = os.path.join(assets, 'textures/gui/ability_menus/star_dragon', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fn().save(path)
        out.append(path)
    for name, fn in STATUS.items():
        path = os.path.join(assets, 'textures/gui/star_dragon', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        s(fn).save(path)
        out.append(path)
    path = os.path.join(assets, 'textures/gui/pathway_icons/star_dragon.png')
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
