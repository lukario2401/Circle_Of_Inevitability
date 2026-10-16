"""Screen pictures for the Knight Of Misfortune (Darkness pathway).

python3 tools/model_gen/knight_of_misfortune/icons.py src/main/resources/assets/circle_of_inevitability [preview.png]

  textures/gui/ability_menus/knight_of_misfortune/*.png  32x32, the four wheels (Midnight Poem on key 2, Spirit
                                                         Commanding on key 5, Nightwatcher on key 6, Darkness and
                                                         Concealment on key 7), in a round frame of tarnished silver
  textures/gui/knight_of_misfortune/*.png                16x16, the statuses shown above the hotbar
  textures/gui/pathway_icons/knight_of_misfortune.png    16x16, the Pathway's icon beside the hotbar

Every picture is drawn in flat colors and then outlined automatically (the outline() of ../hand_of_god/icons.py),
so the shapes stay simple to change. Replace any of them with a hand-drawn PNG of the same name.
"""
import math
import os
import sys

from PIL import Image, ImageDraw

# ------------------------------------------------------------------ colors
OUTLINE = (10, 6, 16, 255)
VIOLET = (130, 70, 200, 255)
VIOLET_LIGHT = (200, 160, 255, 255)
VIOLET_DARK = (60, 28, 100, 255)
NIGHT = (34, 20, 58, 255)
MOON = (214, 228, 255, 255)
MOON_DARK = (120, 140, 200, 255)
WHITE = (255, 255, 255, 255)
BONE = (232, 224, 206, 255)
BONE_DARK = (150, 140, 124, 255)
FLESH = (70, 30, 72, 255)
FLESH_LIGHT = (150, 90, 140, 255)
ASH = (70, 64, 80, 255)
ASH_LIGHT = (120, 114, 130, 255)
GOLD = (184, 162, 90, 255)
GOLD_LIGHT = (230, 214, 150, 255)
RED = (190, 40, 50, 255)
RED_LIGHT = (255, 120, 120, 255)
GREEN = (90, 170, 100, 255)
EARTH = (120, 88, 56, 255)
EARTH_LIGHT = (170, 130, 86, 255)
SKY = (150, 190, 230, 255)
SILVER = (190, 190, 210, 255)
SILVER_LIGHT = (236, 236, 248, 255)
SILVER_DARK = (96, 92, 120, 255)
BLACK = (14, 8, 22, 255)


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

def crescent(img, cx, cy, r, color, cut=0.55, turn=-0.7):
    mask = Image.new('L', img.size, 0)
    md = ImageDraw.Draw(mask)
    md.ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)
    ox, oy = cx + math.cos(turn) * r * cut, cy + math.sin(turn) * r * cut
    md.ellipse([ox - r * 0.9, oy - r * 0.9, ox + r * 0.9, oy + r * 0.9], fill=0)
    img.paste(Image.new('RGBA', img.size, color), (0, 0), mask)


def star(d, x, y, r, color, points=4, inner=0.4):
    pts = []
    for i in range(points * 2):
        a = math.pi * i / points - math.pi / 2
        rr = r if i % 2 == 0 else r * inner
        pts.append((x + math.cos(a) * rr, y + math.sin(a) * rr))
    d.polygon(pts, fill=color)


def note(d, x, y, s, color):
    """A musical note, its head at (x, y)."""
    d.ellipse([x - s, y - s * 0.7, x + s * 0.6, y + s * 0.7], fill=color)
    d.line([(x + s * 0.5, y), (x + s * 0.5, y - s * 3)], fill=color)
    d.line([(x + s * 0.5, y - s * 3), (x + s * 1.8, y - s * 2.2)], fill=color)


def closed_eye(d, cx, cy, w, color, lashes=True):
    d.arc([cx - w, cy - w * 0.9, cx + w, cy + w * 0.5], 20, 160, fill=color, width=max(1, int(w / 5)))
    if lashes:
        for i in range(-2, 3):
            x = cx + i * w * 0.35
            d.line([(x, cy + w * 0.45), (x + i * 0.4, cy + w * 0.8)], fill=color)


def open_eye(d, cx, cy, w, h, white, iris, pupil):
    d.polygon([(cx - w, cy), (cx - w * 0.4, cy - h), (cx + w * 0.4, cy - h), (cx + w, cy), (cx + w * 0.4, cy + h),
               (cx - w * 0.4, cy + h)], fill=white)
    d.ellipse([cx - h, cy - h, cx + h, cy + h], fill=iris)
    d.line([(cx, cy - h + 1), (cx, cy + h - 1)], fill=pupil)


def sword(d, x, top, bottom, blade, edge, guard, width=1):
    g = bottom - max(3, (bottom - top) // 4)
    d.polygon([(x - width, top + 2), (x, top), (x + width, top + 2), (x + width, g), (x - width, g)], fill=blade)
    d.line([(x, top + 1), (x, g - 1)], fill=edge)
    d.rectangle([x - width - 2, g, x + width + 2, g + 1], fill=guard)
    d.rectangle([x, g + 2, x, bottom - 1], fill=BONE_DARK)
    d.point((x, bottom), fill=guard)


def ghost(d, cx, top, s, body, eyes):
    """A small hooded ghost: a dome head, a body that frays into points at the bottom."""
    d.pieslice([cx - s, top, cx + s, top + s * 2], 180, 360, fill=body)
    d.rectangle([cx - s, top + s, cx + s, top + s * 2.4], fill=body)
    n = 3
    for i in range(n):
        x0 = cx - s + i * (2 * s / n)
        d.polygon([(x0, top + s * 2.4), (x0 + 2 * s / n, top + s * 2.4), (x0 + s / n, top + s * 3.1)], fill=body)
    d.rectangle([cx - s * 0.55, top + s * 0.9, cx - s * 0.2, top + s * 1.3], fill=eyes)
    d.rectangle([cx + s * 0.2, top + s * 0.9, cx + s * 0.55, top + s * 1.3], fill=eyes)


def horseshoe(d, cx, top, s, color, broken=True):
    d.arc([cx - s, top, cx + s, top + s * 2], 180, 360, fill=color, width=max(1, int(s / 2.5)))
    w = max(1, int(s / 2.5))
    d.rectangle([cx - s, top + s, cx - s + w - 1, top + s * 2.2], fill=color)
    if broken:
        d.rectangle([cx + s - w + 1, top + s, cx + s, top + s * 1.5], fill=color)
        d.rectangle([cx + s - w + 2, top + s * 1.8, cx + s + 1, top + s * 2.3], fill=color)
    else:
        d.rectangle([cx + s - w + 1, top + s, cx + s, top + s * 2.2], fill=color)


def wolf_head(d, cx, cy, s, fur, eye):
    d.polygon([(cx - s, cy - s * 0.4), (cx - s * 0.7, cy - s * 1.2), (cx - s * 0.3, cy - s * 0.6), (cx + s * 0.3, cy - s * 0.6),
               (cx + s * 0.7, cy - s * 1.2), (cx + s, cy - s * 0.4), (cx + s * 0.8, cy + s * 0.3),
               (cx + s * 0.3, cy + s * 1.1), (cx - s * 0.3, cy + s * 1.1), (cx - s * 0.8, cy + s * 0.3)], fill=fur)
    for x in (cx - s * 0.45, cx + s * 0.45):
        d.rectangle([x - 0.5, cy - s * 0.1, x + 0.5, cy + s * 0.1], fill=eye)
    d.point((cx, cy - s * 0.35), fill=eye)                                   # the third eye


def hair_strands(d, x0, y0, x1, y1, color, count=4, wave=1.5):
    for i in range(count):
        off = (i - (count - 1) / 2) * 1.5
        pts = []
        for k in range(9):
            t = k / 8
            x = x0 + (x1 - x0) * t + math.sin(t * 9 + i) * wave
            y = y0 + (y1 - y0) * t + off
            pts.append((x, y))
        d.line(pts, fill=color)


# ------------------------------------------------------------------ the 32x32 wheel pictures

S = 32
C = 15.5


def wheel_base(inside=(30, 18, 44, 255), edge=(60, 36, 86, 255)):
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
                    px[x, y] = (40, 30, 56, 255)
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


POEM_INSIDE = dict(inside=(20, 26, 52, 255), edge=(44, 56, 96, 255))       # moon-blue night
SPIRIT_INSIDE = dict(inside=(16, 30, 40, 255), edge=(36, 64, 78, 255))      # cold spirit green-blue
WATCH_INSIDE = dict(inside=(26, 14, 36, 255), edge=(56, 30, 74, 255))       # violet dark
DARK_INSIDE = dict(inside=(8, 4, 14, 255), edge=(36, 18, 52, 255))          # deepest dark


def draw(inside, fn):
    base = wheel_base(**inside)
    layer, d = canvas(S)
    fn(layer, d)
    return in_frame(base, layer)


# ---- key 2: Midnight Poem

def w_tranquilize():
    def f(img, d):
        crescent(img, 15.5, 14, 8, MOON)
        for i in range(3):
            d.line([(6 + i * 2, 25 + i), (25 - i * 2, 25 + i)], fill=MOON_DARK)          # still water
    return draw(POEM_INSIDE, f)


def w_lullaby():
    def f(img, d):
        crescent(img, 11, 13, 6, MOON)
        for i, (x, y, s) in enumerate(((19, 20, 2.0), (22, 13, 2.6), (26, 6, 3.2))):     # z z z
            d.line([(x - s, y - s), (x + s, y - s), (x - s, y + s), (x + s, y + s)], fill=WHITE)
        note(d, 9, 25, 1.6, MOON_DARK)
    return draw(POEM_INSIDE, f)


def w_pacify():
    def f(img, d):
        d.polygon([(7, 20), (15.5, 26), (24, 20), (22, 18), (15.5, 22), (9, 18)], fill=MOON_DARK)   # a lowered hand
        closed_eye(d, 15.5, 12, 7, MOON)
    return draw(POEM_INSIDE, f)


def w_fear():
    def f(img, d):
        open_eye(d, 15.5, 15.5, 10, 6, WHITE, RED, BLACK)
        for a in range(8):
            ang = math.tau * a / 8
            d.line([(15.5 + math.cos(ang) * 11, 15.5 + math.sin(ang) * 11),
                    (15.5 + math.cos(ang) * 13, 15.5 + math.sin(ang) * 13)], fill=RED_LIGHT)
    return draw(POEM_INSIDE, f)


def w_agitating():
    def f(img, d):
        pts = [(15.5 + math.cos(math.tau * i / 14) * (10 if i % 2 == 0 else 5),
                15.5 + math.sin(math.tau * i / 14) * (10 if i % 2 == 0 else 5)) for i in range(14)]
        d.polygon(pts, fill=(200, 60, 120, 255))
        d.ellipse([11, 11, 20, 20], fill=(255, 150, 200, 255))
        note(d, 15, 17, 1.6, BLACK)
    return draw(POEM_INSIDE, f)


# ---- key 5: Spirit Commanding

def w_terror_banshee():
    def f(img, d):
        ghost(d, 15.5, 6, 7, (200, 214, 240, 255), BLACK)
        d.ellipse([13.5, 16, 17.5, 20], fill=BLACK)                                      # the scream
        for i in range(3):
            d.arc([4 - i * 2, 6 - i * 2, 27 + i * 2, 29 + i * 2], 300, 340, fill=WHITE)
    return draw(SPIRIT_INSIDE, f)


def w_earthen_spirit():
    def f(img, d):
        d.rectangle([3, 19, 28, 28], fill=EARTH)
        d.line([(3, 19), (28, 19)], fill=EARTH_LIGHT)
        ghost(d, 15.5, 8, 5, (180, 220, 200, 200), BLACK)
        d.rectangle([9, 19, 22, 22], fill=EARTH)                                         # sinking into it
        for x in (7, 24):
            d.point((x, 23), fill=EARTH_LIGHT)
    return draw(SPIRIT_INSIDE, f)


def w_gale_spirit():
    def f(img, d):
        for i, y in enumerate((9, 15, 21)):
            d.arc([4 + i * 2, y - 5, 26 - i, y + 5], 200, 340, fill=SKY if i != 1 else WHITE, width=2)
        ghost(d, 21, 11, 3.5, (200, 230, 255, 220), BLACK)
    return draw(SPIRIT_INSIDE, f)


def w_command_spirits():
    def f(img, d):
        ghost(d, 9, 13, 3.5, (180, 200, 230, 255), BLACK)
        ghost(d, 22, 13, 3.5, (180, 200, 230, 255), BLACK)
        ghost(d, 15.5, 7, 4, (230, 240, 255, 255), BLACK)
        crescent(img, 15.5, 25, 3, VIOLET_LIGHT, 0.55, -math.pi / 2)
    return draw(SPIRIT_INSIDE, f)


# ---- key 6: Nightwatcher

def w_night_domain():
    def f(img, d):
        d.ellipse([4, 4, 27, 27], fill=NIGHT)
        crescent(img, 15.5, 13, 6, VIOLET_LIGHT)
        for x, y in ((8, 9), (23, 8), (24, 20), (9, 21), (15, 24)):
            d.point((x, y), fill=WHITE)
        d.rectangle([4, 22, 27, 27], fill=VIOLET_DARK)                                    # the tide rising
    return draw(WATCH_INSIDE, f)


def w_hair_entanglement():
    def f(img, d):
        hair_strands(d, 4, 8, 27, 24, (220, 220, 236, 255), 4, 2.2)
        hair_strands(d, 4, 24, 27, 8, (170, 170, 200, 255), 3, 2.0)
    return draw(WATCH_INSIDE, f)


def w_curse_of_misfortune():
    def f(img, d):
        horseshoe(d, 15.5, 7, 8, GOLD)
        d.line([(20, 20), (24, 22), (21, 23), (25, 26)], fill=GOLD_LIGHT)
    return draw(WATCH_INSIDE, f)


def w_demonic_wolf():
    def f(img, d):
        wolf_head(d, 15.5, 16, 9, ASH, VIOLET_LIGHT)
        d.polygon([(13, 21), (15.5, 25), (18, 21)], fill=BONE)                             # fangs
    return draw(WATCH_INSIDE, f)


# ---- key 7: Darkness and Concealment

def w_sword_of_darkness():
    def f(img, d):
        sword(d, 15.5, 3, 28, BONE, VIOLET, BONE_DARK, 2)
        d.line([(15.5, 6), (15.5, 20)], fill=VIOLET_DARK)
    return draw(DARK_INSIDE, f)


def w_nightmare_world():
    def f(img, d):
        d.ellipse([5, 5, 26, 26], fill=VIOLET_DARK)
        d.ellipse([9, 9, 22, 22], fill=NIGHT)
        open_eye(d, 15.5, 15.5, 6, 3, (230, 200, 255, 255), VIOLET, BLACK)
    return draw(DARK_INSIDE, f)


def w_concealed_state():
    def f(img, d):
        ghost(d, 15.5, 7, 6, (90, 70, 120, 180), (200, 180, 255, 255))
        for y in (11, 16, 21):
            d.line([(4, y), (27, y + 2)], fill=(60, 40, 90, 255), width=2)                 # fog wrapping it
    return draw(DARK_INSIDE, f)


def w_secret_concealment():
    def f(img, d):
        closed_eye(d, 15.5, 15, 9, VIOLET_LIGHT)
        d.arc([4, 3, 27, 20], 200, 340, fill=SILVER, width=2)                              # the veil
        d.line([(15.5, 22), (15.5, 27)], fill=VIOLET_LIGHT)
    return draw(DARK_INSIDE, f)


def w_concealed_world():
    def f(img, d):
        d.ellipse([5, 5, 26, 26], fill=(50, 30, 80, 255))
        d.ellipse([5, 5, 26, 26], outline=VIOLET_LIGHT)
        d.arc([5, 11, 26, 20], 0, 180, fill=(110, 80, 160, 255))
        d.rectangle([14, 13, 17, 18], fill=BLACK)                                          # a lock: sealed
        d.arc([13, 9, 18, 15], 180, 360, fill=SILVER)
    return draw(DARK_INSIDE, f)


def w_mantle_of_darkness():
    def f(img, d):
        d.polygon([(15.5, 4), (25, 27), (6, 27)], fill=(40, 20, 64, 255))                  # a cloak of darkness
        d.ellipse([12, 6, 19, 13], fill=BLACK)
        d.point((14, 9), fill=VIOLET_LIGHT)
        d.point((17, 9), fill=VIOLET_LIGHT)
        d.line([(6, 16), (2, 24)], fill=VIOLET, width=2)                                   # shadow arms
        d.line([(25, 16), (29, 24)], fill=VIOLET, width=2)
    return draw(DARK_INSIDE, f)


def w_eternal_rest():
    def f(img, d):
        closed_eye(d, 15.5, 12, 8, VIOLET_LIGHT)
        crescent(img, 15.5, 22, 5, MOON, 0.5, -math.pi / 2)
        star(d, 7, 21, 2, WHITE)
        star(d, 24, 21, 2, WHITE)
    return draw(DARK_INSIDE, f)


def w_destination_of_all_things():
    def f(img, d):
        for k in range(5):
            a0 = k * 72
            d.arc([4, 4, 27, 27], a0, a0 + 50, fill=VIOLET, width=2)
            d.arc([8, 8, 23, 23], a0 + 30, a0 + 80, fill=VIOLET_LIGHT, width=1)
        d.ellipse([12, 12, 19, 19], fill=BLACK)
        d.ellipse([12, 12, 19, 19], outline=WHITE)
    return draw(DARK_INSIDE, f)


WHEEL = {
    'tranquilize': w_tranquilize,
    'lullaby': w_lullaby,
    'pacify': w_pacify,
    'fear': w_fear,
    'agitating': w_agitating,
    'terror_banshee': w_terror_banshee,
    'earthen_spirit': w_earthen_spirit,
    'gale_spirit': w_gale_spirit,
    'command_spirits': w_command_spirits,
    'night_domain': w_night_domain,
    'hair_entanglement': w_hair_entanglement,
    'curse_of_misfortune': w_curse_of_misfortune,
    'demonic_wolf': w_demonic_wolf,
    'sword_of_darkness': w_sword_of_darkness,
    'nightmare_world': w_nightmare_world,
    'concealed_state': w_concealed_state,
    'secret_concealment': w_secret_concealment,
    'concealed_world': w_concealed_world,
    'mantle_of_darkness': w_mantle_of_darkness,
    'eternal_rest': w_eternal_rest,
    'destination_of_all_things': w_destination_of_all_things,
}


# ------------------------------------------------------------------ the 16x16 status pictures

def s(fn):
    img, d = canvas(16)
    fn(img, d)
    return outlined(img)


def i_spirit_vision(img, d):
    open_eye(d, 7.5, 7.5, 7, 4, MOON, (110, 180, 255, 255), BLACK)


def i_dowsing(img, d):
    d.line([(7.5, 0), (7.5, 9)], fill=SILVER)
    d.polygon([(7.5, 9), (10, 12), (7.5, 15), (5, 12)], fill=(150, 190, 255, 255))


def i_limbs(img, d):
    for x, bend in ((4, -2), (8, 0), (12, 2)):
        d.line([(x, 15), (x + bend * 0.5, 9), (x + bend, 3)], fill=FLESH_LIGHT, width=2)
        d.point((x + bend, 2), fill=VIOLET_LIGHT)


def i_nightmare_state(img, d):
    ghost(d, 7.5, 1, 4, (190, 210, 255, 220), BLACK)


def i_dream(img, d):
    d.ellipse([3, 1, 12, 14], fill=(170, 190, 255, 200))
    d.ellipse([5, 4, 10, 11], fill=(230, 236, 255, 255))


def i_requiem(img, d):
    crescent(img, 7.5, 7.5, 6, MOON_DARK)
    note(d, 10, 11, 1.4, MOON)


def i_soothing(img, d):
    d.ellipse([2, 4, 13, 13], fill=MOON_DARK)
    d.line([(4, 8), (7, 11), (11, 5)], fill=WHITE)


def i_banshee(img, d):
    ghost(d, 7.5, 1, 5, (210, 220, 245, 255), BLACK)
    d.ellipse([6, 7, 9, 10], fill=BLACK)


def i_earth(img, d):
    d.rectangle([0, 9, 15, 15], fill=EARTH)
    d.line([(0, 9), (15, 9)], fill=EARTH_LIGHT)
    ghost(d, 7.5, 2, 3, (180, 220, 200, 200), BLACK)


def i_gale(img, d):
    for y in (4, 8, 12):
        d.arc([1, y - 3, 14, y + 3], 200, 340, fill=SKY)


def i_spirits(img, d):
    for x, y in ((3, 2), (8, 5), (11, 1)):
        d.rectangle([x, y, x + 2, y + 5], fill=BONE)                                        # spirit teeth
        d.point((x + 1, y + 6), fill=BONE_DARK)
    d.line([(1, 13), (14, 13)], fill=(170, 200, 230, 255))


def i_night_domain(img, d):
    d.ellipse([1, 1, 14, 14], fill=NIGHT)
    crescent(img, 7.5, 7, 4, VIOLET_LIGHT)


def i_misfortune(img, d):
    horseshoe(d, 7.5, 2, 5, GOLD)


def i_wolf(img, d):
    wolf_head(d, 7.5, 8, 5.5, ASH, VIOLET_LIGHT)


def i_madness(img, d):
    for r, col in ((7, VIOLET_DARK), (5, VIOLET), (3, (220, 80, 160, 255))):
        d.ellipse([7.5 - r, 7.5 - r, 7.5 + r, 7.5 + r], outline=col)
    d.point((7, 7), fill=WHITE)


def i_dark_sword(img, d):
    sword(d, 7.5, 0, 15, BONE, VIOLET, BONE_DARK, 1)


def i_nightmare_world(img, d):
    d.ellipse([1, 1, 14, 14], fill=VIOLET_DARK)
    open_eye(d, 7.5, 7.5, 5, 2.5, (230, 200, 255, 255), VIOLET, BLACK)


def i_concealed(img, d):
    ghost(d, 7.5, 1, 4, (90, 70, 120, 200), (200, 180, 255, 255))
    d.line([(1, 9), (14, 11)], fill=(70, 50, 100, 255))


def i_secret(img, d):
    d.ellipse([0, 2, 15, 14], fill=VIOLET_DARK)
    closed_eye(d, 7.5, 8, 5, VIOLET_LIGHT)
    d.arc([1, 1, 14, 9], 200, 340, fill=SILVER)


def i_concealed_world(img, d):
    d.ellipse([1, 1, 14, 14], fill=(50, 30, 80, 255))
    d.ellipse([1, 1, 14, 14], outline=VIOLET_LIGHT)
    d.rectangle([6, 6, 9, 10], fill=BLACK)


def i_mantle(img, d):
    d.polygon([(7.5, 0), (14, 15), (1, 15)], fill=(50, 26, 80, 255))
    d.point((6, 4), fill=VIOLET_LIGHT)
    d.point((9, 4), fill=VIOLET_LIGHT)


def i_eternal_rest(img, d):
    d.ellipse([0, 0, 15, 15], fill=NIGHT)
    closed_eye(d, 7.5, 6, 5, VIOLET_LIGHT)
    crescent(img, 7.5, 12, 3, MOON, 0.5, -math.pi / 2)


def i_slumber(img, d):
    for x, y, s_ in ((4, 11, 1.5), (8, 7, 2.0), (12, 2, 2.5)):
        d.line([(x - s_, y - s_), (x + s_, y - s_), (x - s_, y + s_), (x + s_, y + s_)], fill=MOON)


def i_fear(img, d):
    open_eye(d, 7.5, 7.5, 7, 4, WHITE, RED, BLACK)


STATUS = {
    'spirit_vision': i_spirit_vision,
    'dowsing': i_dowsing,
    'limbs': i_limbs,
    'nightmare_state': i_nightmare_state,
    'dream': i_dream,
    'requiem': i_requiem,
    'soothing': i_soothing,
    'banshee': i_banshee,
    'earth': i_earth,
    'gale': i_gale,
    'spirits': i_spirits,
    'night_domain': i_night_domain,
    'misfortune': i_misfortune,
    'wolf': i_wolf,
    'madness': i_madness,
    'dark_sword': i_dark_sword,
    'nightmare_world': i_nightmare_world,
    'concealed': i_concealed,
    'secret': i_secret,
    'concealed_world': i_concealed_world,
    'mantle': i_mantle,
    'eternal_rest': i_eternal_rest,
    'slumber': i_slumber,
    'fear': i_fear,
}


def pathway_icon():
    """The Star of the tarot cradled in a crescent moon of Darkness."""
    img, d = canvas(16)
    d.ellipse([0, 0, 15, 15], fill=NIGHT)
    crescent(img, 7.5, 7.5, 6.5, VIOLET_LIGHT, 0.55, -0.8)
    star(d, 10, 5.5, 3.5, WHITE, 4, 0.35)
    return outlined(img)


def main(assets, preview=None):
    out = []
    for name, fn in WHEEL.items():
        path = os.path.join(assets, 'textures/gui/ability_menus/knight_of_misfortune', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fn().save(path)
        out.append(path)
    for name, fn in STATUS.items():
        path = os.path.join(assets, 'textures/gui/knight_of_misfortune', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        s(fn).save(path)
        out.append(path)
    path = os.path.join(assets, 'textures/gui/pathway_icons/knight_of_misfortune.png')
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
