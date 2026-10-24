"""Screen pictures for the Hand Of Order (Justiciar pathway).

python3 tools/model_gen/hand_of_order/icons.py src/main/resources/assets/circle_of_inevitability [preview.png]

  textures/gui/ability_menus/hand_of_order/*.png   32x32, the four wheels (Interrogation on key 3, Verdict on key 4,
                                                   Decree on key 5, Order on key 7), in a round frame of brass. The
                                                   three old pictures there (explosion, speed, judgement) belong to
                                                   the first HandOfOrder class and are left alone.
  textures/gui/hand_of_order/*.png                 16x16, the statuses shown above the hotbar, and the seven clause
                                                   pictures of the Court (clause_1 .. clause_7); court_clauses.png
                                                   (64x64) is the whole tablet of clauses
  textures/gui/pathway_icons/hand_of_order.png     16x16, the Pathway's icon beside the hotbar

Every picture is drawn in flat colors and then outlined automatically, so the shapes stay simple to change.
Replace any of them with a hand-drawn PNG of the same name.
"""
import math
import os
import sys

from PIL import Image, ImageDraw

# ------------------------------------------------------------------ colors
OUTLINE = (20, 14, 8, 255)
BRASS = (201, 162, 75, 255)
BRASS_LIGHT = (240, 214, 140, 255)
BRASS_DARK = (122, 90, 36, 255)
WHITE_GOLD = (255, 243, 208, 255)
WHITE = (255, 255, 255, 255)
LIGHTNING = (168, 200, 255, 255)
LIGHTNING_DARK = (80, 110, 200, 255)
IRON = (62, 67, 77, 255)
IRON_LIGHT = (130, 138, 152, 255)
IRON_DARK = (34, 36, 42, 255)
RED = (176, 48, 48, 255)
RED_LIGHT = (232, 110, 100, 255)
RED_DARK = (100, 20, 20, 255)
VIOLET = (90, 58, 122, 255)
VIOLET_LIGHT = (160, 120, 200, 255)
GLASS = (210, 232, 255, 255)
PAPER = (236, 222, 184, 255)
PAPER_DARK = (170, 140, 96, 255)
SKIN = (230, 200, 170, 255)
GREEN = (90, 190, 110, 255)


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

def figure(d, cx, top, s, fill):
    """A standing person: a head and a body."""
    d.ellipse([cx - s * 0.5, top, cx + s * 0.5, top + s], fill=fill)
    d.polygon([(cx - s * 0.6, top + s * 1.1), (cx + s * 0.6, top + s * 1.1), (cx + s * 0.9, top + s * 3.2),
               (cx - s * 0.9, top + s * 3.2)], fill=fill)


def scales(d, cx, cy, s, fill, pan=None):
    pan = pan or fill
    d.line([(cx, cy - s), (cx, cy + s)], fill=fill)
    d.line([(cx - s * 0.5, cy + s), (cx + s * 0.5, cy + s)], fill=fill)
    d.line([(cx - s, cy - s * 0.6), (cx + s, cy - s * 0.6)], fill=fill)
    for sx in (-1, 1):
        px = cx + sx * s
        d.line([(px, cy - s * 0.6), (px, cy)], fill=fill)
        d.rectangle([px - s * 0.35, cy, px + s * 0.35, cy + s * 0.2], fill=pan)


def bolt(d, pts, fill, width=1):
    d.line(pts, fill=fill, width=width)


def zigzag(x0, y0, x1, y1, n=4, amp=2.0):
    pts = []
    for i in range(n + 1):
        t = i / n
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        off = 0 if i in (0, n) else (amp if i % 2 else -amp)
        dx, dy = y1 - y0, -(x1 - x0)
        ln = math.hypot(dx, dy) or 1
        pts.append((x + dx / ln * off, y + dy / ln * off))
    return pts


def prohibit(d, cx, cy, r, fill):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=fill, width=2)
    d.line([(cx - r * 0.7, cy - r * 0.7), (cx + r * 0.7, cy + r * 0.7)], fill=fill, width=2)


def gavel(d, cx, cy, s, head, handle):
    d.line([(cx, cy), (cx + s, cy + s)], fill=handle, width=2)
    d.polygon([(cx - s * 0.9, cy - s * 0.2), (cx - s * 0.2, cy - s * 0.9), (cx + s * 0.3, cy - s * 0.4),
               (cx - s * 0.4, cy + s * 0.3)], fill=head)


def pillar(d, cx, top, bottom, w, fill, dark):
    d.rectangle([cx - w, bottom - 1, cx + w, bottom], fill=dark)
    d.rectangle([cx - w + 1, top + 2, cx + w - 1, bottom - 1], fill=fill)
    d.rectangle([cx - w, top, cx + w, top + 1], fill=dark)
    for x in range(cx - w + 2, cx + w - 1, 2):
        d.line([(x, top + 3), (x, bottom - 2)], fill=dark)


def eye(d, cx, cy, w, h, white, iris):
    d.polygon([(cx - w, cy), (cx - w * 0.4, cy - h), (cx + w * 0.4, cy - h), (cx + w, cy), (cx + w * 0.4, cy + h),
               (cx - w * 0.4, cy + h)], fill=white)
    d.ellipse([cx - h, cy - h, cx + h, cy + h], fill=iris)


def paper(d, x0, y0, x1, y1, fill, line):
    d.rectangle([x0, y0, x1, y1], fill=fill)
    for y in range(y0 + 2, y1 - 1, 2):
        d.line([(x0 + 1, y), (x1 - 1 - (y % 3), y)], fill=line)


# ------------------------------------------------------------------ the 32x32 wheel pictures

S = 32
C = 15.5


def wheel_base(inside=(30, 22, 12, 255), edge=(70, 50, 22, 255)):
    """A round frame of brass round a dark field."""
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = im.load()
    for y in range(S):
        for x in range(S):
            dd = math.hypot(x - C, y - C)
            if dd <= 15.6:
                if dd > 13.6:
                    light = (x - C) * -0.7 + (y - C) * -0.7
                    px[x, y] = BRASS_LIGHT if light > 9 else BRASS if light > -6 else BRASS_DARK
                elif dd > 12.6:
                    px[x, y] = (60, 40, 14, 255)
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


INTERROGATE = dict(inside=(10, 16, 40, 255), edge=(30, 44, 90, 255))     # cold blue: Psychic lightning
VERDICT = dict(inside=(34, 12, 12, 255), edge=(80, 26, 22, 255))         # dark red: the sentence
DECREE = dict(inside=(16, 18, 22, 255), edge=(50, 54, 62, 255))          # iron: Rules
ORDER = dict(inside=(36, 28, 10, 255), edge=(96, 74, 28, 255))           # brass: Order


# ---- key 3: Interrogation

def w_psychic_lashing():
    base = wheel_base(**INTERROGATE)
    layer, d = canvas(S)
    d.line([(8, 25), (19, 11)], fill=IRON_LIGHT, width=2)                   # the iron
    d.rectangle([6, 24, 9, 27], fill=RED)
    d.line([(17, 9), (23, 15)], fill=LIGHTNING, width=2)                    # the brand, glowing
    d.line([(17, 15), (23, 9)], fill=LIGHTNING, width=2)
    bolt(d, zigzag(22, 17, 26, 26, 4, 1.5), WHITE)
    bolt(d, zigzag(14, 8, 9, 5, 3, 1.2), LIGHTNING)
    return in_frame(base, layer)


def w_psychic_piercing():
    base = wheel_base(**INTERROGATE)
    layer, d = canvas(S)
    eye(d, 9, 15, 5, 3, WHITE, LIGHTNING_DARK)
    bolt(d, zigzag(14, 15, 27, 13, 5, 1.6), LIGHTNING, 2)                    # bolts leaping from the eye
    bolt(d, zigzag(14, 15, 26, 19, 4, 1.2), WHITE)
    bolt(d, zigzag(14, 15, 25, 9, 4, 1.2), WHITE)
    return in_frame(base, layer)


def w_whip_of_pain():
    base = wheel_base(**INTERROGATE)
    layer, d = canvas(S)
    d.rectangle([6, 22, 9, 27], fill=RED_DARK)                              # the handle
    pts = [(8, 22), (11, 15), (16, 10), (22, 9), (25, 13), (23, 18), (19, 20)]
    d.line(pts, fill=LIGHTNING, width=2)
    for p in pts[1::2]:
        d.point((p[0] + 1, p[1] - 1), fill=WHITE)                            # barbs
    bolt(d, zigzag(19, 20, 25, 25, 3, 1.5), WHITE)
    return in_frame(base, layer)


# ---- key 4: Verdict

def w_imprison():
    base = wheel_base(**VERDICT)
    layer, d = canvas(S)
    figure(d, 15.5, 10, 3, (190, 150, 140, 255))
    for x in (8, 23):
        d.rectangle([x - 1, 7, x + 1, 25], fill=GLASS)                      # panes closing in
    d.rectangle([9, 6, 22, 7], fill=GLASS)
    d.rectangle([7, 25, 24, 26], fill=IRON_LIGHT)
    return in_frame(base, layer)


def w_exile():
    base = wheel_base(**VERDICT)
    layer, d = canvas(S)
    figure(d, 21, 9, 2.6, (190, 150, 140, 255))
    for y in (11, 15, 19):
        d.line([(6, y), (15, y)], fill=WHITE_GOLD)                          # thrown away at speed
    d.polygon([(13, 11), (17, 15), (13, 19)], fill=WHITE_GOLD)
    return in_frame(base, layer)


def w_death():
    base = wheel_base(**VERDICT)
    layer, d = canvas(S)
    d.rectangle([9, 11, 19, 20], fill=WHITE_GOLD)                           # a fist striking
    for i in range(3):
        d.line([(19, 12 + i * 3), (21, 12 + i * 3)], fill=BRASS)
    d.rectangle([4, 13, 9, 18], fill=BRASS)
    d.polygon([(22, 8), (27, 15), (22, 22), (24, 15)], fill=RED_LIGHT)       # the blow
    return in_frame(base, layer)


def w_maintain_secrecy():
    base = wheel_base(**VERDICT)
    layer, d = canvas(S)
    d.ellipse([8, 8, 23, 23], fill=(200, 170, 150, 255))                    # a face, mouth sealed
    eye(d, 12, 13, 2, 1, WHITE, VIOLET)
    eye(d, 19, 13, 2, 1, WHITE, VIOLET)
    d.rectangle([10, 17, 21, 20], fill=IRON_LIGHT)                          # the seal
    d.line([(10, 17), (21, 20)], fill=IRON)
    d.line([(10, 20), (21, 17)], fill=IRON)
    return in_frame(base, layer)


def w_deprivation():
    base = wheel_base(**VERDICT)
    layer, d = canvas(S)
    d.ellipse([9, 9, 18, 18], fill=VIOLET_LIGHT)                            # a power, taken in hand
    d.ellipse([11, 11, 16, 16], fill=WHITE)
    d.polygon([(16, 17), (24, 25), (21, 26), (14, 19)], fill=SKIN)
    d.line([(19, 7), (26, 14)], fill=RED_LIGHT, width=2)
    d.line([(19, 14), (26, 7)], fill=RED_LIGHT, width=2)
    return in_frame(base, layer)


def w_execution():
    base = wheel_base(**VERDICT)
    layer, d = canvas(S)
    d.ellipse([7, 15, 24, 27], outline=RED_LIGHT, width=2)                  # the ring closing
    d.rectangle([11, 4, 20, 12], fill=WHITE_GOLD)                           # the raised fist
    d.rectangle([13, 12, 18, 16], fill=BRASS)
    d.line([(15, 19), (15, 23)], fill=RED_LIGHT)
    d.line([(13, 21), (17, 21)], fill=RED_LIGHT)
    return in_frame(base, layer)


# ---- key 5: Decree

def w_prohibition_passage():
    base = wheel_base(**DECREE)
    layer, d = canvas(S)
    for i, (x, y) in enumerate(((8, 20), (11, 17), (14, 14))):             # a dash, stopped
        d.rectangle([x, y, x + 2, y + 2], fill=(150, 130, 200, 255 - i * 40))
    d.polygon([(17, 8), (25, 12), (20, 14), (22, 20)], fill=VIOLET_LIGHT)
    prohibit(d, 18, 15, 9, RED_LIGHT)
    return in_frame(base, layer)


def w_prohibition_concealment():
    base = wheel_base(**DECREE)
    layer, d = canvas(S)
    figure(d, 15.5, 9, 3, (150, 150, 170, 255))
    for x in range(8, 24, 3):
        d.point((x, 24), fill=WHITE_GOLD)
    prohibit(d, 15.5, 15.5, 10, RED_LIGHT)
    return in_frame(base, layer)


def w_imperative_reality():
    base = wheel_base(**DECREE)
    layer, d = canvas(S)
    d.rectangle([7, 7, 24, 24], outline=IRON_LIGHT)                         # the plain grid of Reality
    for p in (11, 15, 20):
        d.line([(p, 8), (p, 23)], fill=IRON)
        d.line([(8, p), (23, p)], fill=IRON)
    d.ellipse([10, 10, 21, 21], outline=VIOLET_LIGHT)
    d.line([(9, 22), (22, 9)], fill=WHITE_GOLD, width=2)
    return in_frame(base, layer)


def w_contract():
    base = wheel_base(**DECREE)
    layer, d = canvas(S)
    paper(d, 8, 6, 21, 25, PAPER, PAPER_DARK)
    d.line([(10, 22), (13, 20), (15, 22), (18, 20)], fill=(40, 30, 80, 255))
    d.ellipse([17, 19, 23, 25], fill=RED)
    d.line([(24, 6), (18, 16)], fill=WHITE)                                  # the quill
    d.polygon([(24, 5), (27, 7), (21, 12)], fill=WHITE_GOLD)
    return in_frame(base, layer)


# ---- key 7: Order

def w_balance():
    base = wheel_base(**ORDER)
    layer, d = canvas(S)
    d.line([(15, 7), (15, 24)], fill=BRASS_LIGHT)
    d.line([(11, 24), (20, 24)], fill=BRASS_LIGHT)
    d.line([(7, 12), (24, 10)], fill=BRASS_LIGHT, width=2)                  # level, just about
    for x, y in ((8, 12), (23, 10)):
        d.line([(x, y), (x, y + 7)], fill=BRASS)
        d.rectangle([x - 3, y + 7, x + 3, y + 8], fill=WHITE_GOLD)
    return in_frame(base, layer)


def w_divide_battlefield():
    base = wheel_base(**ORDER)
    layer, d = canvas(S)
    d.ellipse([6, 14, 25, 26], outline=WHITE_GOLD, width=2)                 # the ring of Order
    for x in (7, 11, 20, 24):
        d.line([(x, 9), (x, 20)], fill=BRASS_LIGHT)
    figure(d, 15.5, 10, 2.4, IRON_LIGHT)
    d.polygon([(26, 6), (28, 11), (24, 9)], fill=RED_LIGHT)                  # a shot turned back
    return in_frame(base, layer)


def w_law_no_return():
    base = wheel_base(**ORDER)
    layer, d = canvas(S)
    d.rectangle([10, 7, 21, 25], fill=BRASS_DARK)                           # a shut door, barred
    d.ellipse([10, 4, 21, 12], fill=BRASS_DARK)
    d.line([(7, 13), (24, 13)], fill=RED_LIGHT, width=2)
    d.line([(7, 20), (24, 20)], fill=RED_LIGHT, width=2)
    d.point((18, 17), fill=BRASS_LIGHT)
    return in_frame(base, layer)


def w_order_proxy():
    base = wheel_base(**ORDER)
    layer, d = canvas(S)
    pillar(d, 22, 5, 26, 3, BRASS, BRASS_DARK)                              # the Pillar behind
    figure(d, 12, 8, 3.2, WHITE_GOLD)
    for x, y in ((5, 8), (6, 18), (18, 4)):
        d.point((x, y), fill=WHITE)
    return in_frame(base, layer)


def w_quell_disorder():
    base = wheel_base(**ORDER)
    layer, d = canvas(S)
    for i, r in enumerate((10, 7, 4)):
        pts = [(15.5 + math.cos(math.pi / 4 + k * math.pi / 2 + i * 0.4) * r,
                15.5 + math.sin(math.pi / 4 + k * math.pi / 2 + i * 0.4) * r) for k in range(4)]
        d.line(pts + [pts[0]], fill=WHITE_GOLD if i % 2 == 0 else BRASS)
    for x, y in ((6, 6), (25, 7), (6, 25), (25, 24)):
        d.point((x, y), fill=VIOLET_LIGHT)                                  # Disorder scattering
    return in_frame(base, layer)


def w_court_of_judgment():
    base = wheel_base(**ORDER)
    layer, d = canvas(S)
    d.chord([4, 6, 27, 30], 180, 360, fill=(110, 84, 34, 255))              # the dome
    d.arc([4, 6, 27, 30], 180, 360, fill=WHITE_GOLD)
    pillar(d, 15, 9, 24, 2, BRASS_LIGHT, BRASS_DARK)
    gavel(d, 8, 20, 4, (120, 70, 40, 255), BRASS_DARK)
    d.rectangle([5, 24, 26, 25], fill=BRASS)
    return in_frame(base, layer)


WHEEL = {
    'psychic_lashing': w_psychic_lashing,
    'psychic_piercing': w_psychic_piercing,
    'whip_of_pain': w_whip_of_pain,
    'imprison': w_imprison,
    'exile': w_exile,
    'death': w_death,
    'maintain_secrecy': w_maintain_secrecy,
    'deprivation': w_deprivation,
    'execution': w_execution,
    'prohibition_passage': w_prohibition_passage,
    'prohibition_concealment': w_prohibition_concealment,
    'imperative_reality': w_imperative_reality,
    'contract': w_contract,
    'balance': w_balance,
    'divide_battlefield': w_divide_battlefield,
    'law_no_return': w_law_no_return,
    'order_proxy': w_order_proxy,
    'quell_disorder': w_quell_disorder,
    'court_of_judgment': w_court_of_judgment,
}


# ------------------------------------------------------------------ the 16x16 status pictures

def s(fn):
    img, d = canvas(16)
    fn(d)
    return outlined(img)


# what the Justiciar holds

def i_jurisdiction(d):
    d.ellipse([1, 4, 14, 13], outline=BRASS)
    scales(d, 7.5, 7, 4, BRASS_LIGHT, WHITE_GOLD)


def i_recognition(d):
    for x0, y0, sx, sy in ((1, 1, 1, 1), (14, 1, -1, 1), (1, 14, 1, -1), (14, 14, -1, -1)):
        d.line([(x0, y0), (x0 + sx * 3, y0)], fill=BRASS_LIGHT)
        d.line([(x0, y0), (x0, y0 + sy * 3)], fill=BRASS_LIGHT)
    eye(d, 7.5, 7.5, 5, 2.5, WHITE, BRASS_DARK)


def i_lashing(d):
    d.line([(3, 13), (11, 4)], fill=IRON_LIGHT, width=2)
    d.line([(9, 3), (13, 7)], fill=LIGHTNING, width=2)
    d.line([(9, 7), (13, 3)], fill=LIGHTNING, width=2)
    d.rectangle([2, 12, 4, 14], fill=RED)


def i_convict(d):
    d.line([(7, 2), (7, 13)], fill=RED_LIGHT, width=2)                     # a sword point down
    d.line([(3, 5), (12, 5)], fill=RED_LIGHT, width=2)
    d.ellipse([6, 0, 9, 3], fill=RED_LIGHT)


def i_decree(d):
    d.ellipse([1, 1, 14, 14], outline=IRON_LIGHT, width=2)
    for y in (5, 8, 11):
        d.line([(5, y), (10, y)], fill=WHITE_GOLD)


def i_proxy(d):
    d.rectangle([9, 2, 13, 14], fill=BRASS)
    d.line([(10, 3), (10, 13)], fill=BRASS_DARK)
    d.line([(12, 3), (12, 13)], fill=BRASS_DARK)
    figure(d, 5, 3, 2.4, WHITE_GOLD)


def i_spent(d):
    d.rectangle([4, 3, 11, 13], fill=BRASS_DARK)
    d.line([(4, 13), (11, 3)], fill=(60, 50, 40, 255))
    d.polygon([(2, 9), (6, 13), (2, 13)], fill=IRON_LIGHT)


def i_court(d):
    d.chord([1, 2, 14, 16], 180, 360, fill=BRASS_DARK)
    d.rectangle([6, 4, 9, 13], fill=BRASS_LIGHT)
    d.rectangle([1, 13, 14, 14], fill=BRASS)


def i_balance(d):
    scales(d, 7.5, 7.5, 5, BRASS_LIGHT, WHITE_GOLD)


def i_divided(d):
    d.ellipse([1, 6, 14, 14], outline=WHITE_GOLD, width=2)
    for x in (2, 5, 10, 13):
        d.line([(x, 2), (x, 10)], fill=BRASS_LIGHT)


# what is done to others (when they are players)

def i_halted(d):
    d.polygon([(5, 1), (10, 1), (14, 5), (14, 10), (10, 14), (5, 14), (1, 10), (1, 5)], fill=RED)
    d.line([(4, 7), (11, 7)], fill=WHITE, width=2)


def i_cowed(d):
    figure(d, 7.5, 5, 2.4, IRON_LIGHT)
    d.line([(1, 1), (14, 1)], fill=BRASS_LIGHT, width=2)
    for x in (3, 7, 11):
        d.line([(x, 2), (x + 1, 4)], fill=BRASS_LIGHT)


def i_trembling(d):
    bolt(d, zigzag(2, 2, 13, 13, 5, 2), LIGHTNING, 2)
    bolt(d, zigzag(13, 3, 4, 13, 4, 1.5), WHITE)


def i_imprisoned(d):
    d.rectangle([2, 1, 13, 14], fill=GLASS)
    d.rectangle([4, 3, 11, 12], fill=(160, 190, 230, 255))
    for x in (2, 7, 13):
        d.line([(x, 1), (x, 14)], fill=IRON)


def i_exiled(d):
    d.ellipse([2, 2, 13, 13], fill=VIOLET)
    d.ellipse([5, 5, 10, 10], fill=(20, 10, 30, 255))


def i_stunned(d):
    d.polygon([(7, 1), (9, 6), (14, 7), (9, 9), (7, 14), (5, 9), (1, 7), (5, 6)], fill=WHITE_GOLD)
    d.point((7, 7), fill=RED)


def i_silenced(d):
    d.ellipse([1, 1, 14, 14], fill=(200, 170, 150, 255))
    d.rectangle([3, 8, 12, 11], fill=IRON_LIGHT)
    d.line([(3, 8), (12, 11)], fill=IRON)


def i_deprived(d):
    d.ellipse([2, 2, 13, 13], fill=VIOLET_LIGHT)
    d.line([(3, 3), (12, 12)], fill=RED, width=2)
    d.line([(3, 12), (12, 3)], fill=RED, width=2)


def i_sentenced(d):
    d.ellipse([1, 1, 14, 14], outline=RED_LIGHT, width=2)
    d.ellipse([6, 6, 9, 9], fill=RED_LIGHT)
    for x0, y0, x1, y1 in ((7, 1, 7, 4), (7, 11, 7, 14), (1, 7, 4, 7), (11, 7, 14, 7)):
        d.line([(x0, y0), (x1, y1)], fill=RED_LIGHT)


def i_prohibited(d):
    prohibit(d, 7.5, 7.5, 6, RED_LIGHT)


def i_revealed(d):
    eye(d, 7.5, 7.5, 6, 3, WHITE, BRASS)
    d.point((7, 7), fill=OUTLINE)


def i_reality(d):
    d.rectangle([1, 1, 14, 14], outline=IRON_LIGHT)
    for p in (5, 10):
        d.line([(p, 2), (p, 13)], fill=IRON_LIGHT)
        d.line([(2, p), (13, p)], fill=IRON_LIGHT)


def i_contract(d):
    paper(d, 3, 1, 12, 14, PAPER, PAPER_DARK)
    d.ellipse([8, 10, 13, 15], fill=RED)


def i_no_return(d):
    d.rectangle([4, 4, 11, 14], fill=BRASS_DARK)
    d.ellipse([4, 1, 11, 7], fill=BRASS_DARK)
    d.line([(1, 7), (14, 7)], fill=RED_LIGHT, width=2)
    d.line([(1, 11), (14, 11)], fill=RED_LIGHT, width=2)


def i_judged(d):
    gavel(d, 6, 6, 6, (130, 80, 44, 255), BRASS)
    d.line([(1, 14), (8, 14)], fill=BRASS)


# the clauses of the Court of Judgment, in the order they are written

def c1_no_passage(d):
    d.polygon([(2, 9), (8, 3), (13, 6), (8, 8)], fill=VIOLET_LIGHT)
    prohibit(d, 7.5, 7.5, 6, RED_LIGHT)


def c2_no_concealment(d):
    figure(d, 7.5, 2, 2.4, IRON_LIGHT)
    prohibit(d, 7.5, 7.5, 6, RED_LIGHT)


def c3_no_resurrection(d):
    d.line([(7, 2), (7, 13)], fill=WHITE_GOLD, width=2)
    d.line([(4, 5), (11, 5)], fill=WHITE_GOLD, width=2)
    prohibit(d, 7.5, 7.5, 6, RED_LIGHT)


def c4_no_stand_ins(d):
    paper(d, 3, 2, 8, 12, PAPER, PAPER_DARK)
    figure(d, 10, 3, 2, IRON_LIGHT)
    prohibit(d, 7.5, 7.5, 6, RED_LIGHT)


def c5_no_healing(d):
    d.rectangle([6, 2, 9, 13], fill=GREEN)
    d.rectangle([2, 6, 13, 9], fill=GREEN)
    prohibit(d, 7.5, 7.5, 6, RED_LIGHT)


def c6_no_projectiles(d):
    d.line([(2, 13), (12, 3)], fill=IRON_LIGHT)
    d.polygon([(13, 2), (9, 3), (12, 6)], fill=WHITE)
    prohibit(d, 7.5, 7.5, 6, RED_LIGHT)


def c7_no_abilities(d):
    d.polygon([(7, 1), (9, 6), (14, 7), (9, 9), (7, 14), (5, 9), (1, 7), (5, 6)], fill=VIOLET_LIGHT)
    prohibit(d, 7.5, 7.5, 6, RED_LIGHT)


CLAUSES = [c1_no_passage, c2_no_concealment, c3_no_resurrection, c4_no_stand_ins, c5_no_healing,
           c6_no_projectiles, c7_no_abilities]

STATUS = {
    # what the Justiciar holds
    'jurisdiction': i_jurisdiction,
    'recognition': i_recognition,
    'lashing': i_lashing,
    'convict': i_convict,
    'decree': i_decree,
    'proxy': i_proxy,
    'spent': i_spent,
    'court': i_court,
    'balance': i_balance,
    'divided': i_divided,
    # what is done to others
    'halted': i_halted,
    'cowed': i_cowed,
    'trembling': i_trembling,
    'imprisoned': i_imprisoned,
    'exiled': i_exiled,
    'stunned': i_stunned,
    'silenced': i_silenced,
    'deprived': i_deprived,
    'sentenced': i_sentenced,
    'prohibited': i_prohibited,
    'revealed': i_revealed,
    'reality': i_reality,
    'contract': i_contract,
    'no_return': i_no_return,
    'judged': i_judged,
}
for _i, _fn in enumerate(CLAUSES):
    STATUS[f'clause_{_i + 1}'] = _fn


def court_clauses():
    """The tablet of the Court: seven clauses in a column on a brass plate, numbered by notches."""
    size = 64
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 0, 59, 63], fill=BRASS_DARK)
    d.rectangle([6, 2, 57, 61], fill=(70, 52, 20, 255))
    for i, fn in enumerate(CLAUSES):
        y = 2 + i * 8 + 1
        icon = s(fn).resize((8, 8), Image.NEAREST)
        img.alpha_composite(icon, (9, y))
        for k in range(i + 1):                                              # notches: the clause's number
            d.line([(20 + k * 3, y + 2), (20 + k * 3, y + 5)], fill=BRASS_LIGHT)
        d.line([(43, y + 4), (55, y + 4)], fill=BRASS)
    return img


def pathway_icon():
    """A balance of brass before the Brass Pillar."""
    img, d = canvas(16)
    d.rectangle([7, 1, 8, 14], fill=BRASS_DARK)
    d.line([(4, 14), (11, 14)], fill=BRASS)
    d.line([(1, 4), (14, 4)], fill=BRASS_LIGHT)
    for x in (2, 13):
        d.line([(x, 4), (x, 8)], fill=BRASS)
        d.rectangle([x - 2, 9, x + 2, 10], fill=WHITE_GOLD)
    d.point((7, 0), fill=WHITE_GOLD)
    d.point((8, 0), fill=WHITE_GOLD)
    return outlined(img)


def main(assets, preview=None):
    out = []
    for name, fn in WHEEL.items():
        path = os.path.join(assets, 'textures/gui/ability_menus/hand_of_order', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fn().save(path)
        out.append(path)
    for name, fn in STATUS.items():
        path = os.path.join(assets, 'textures/gui/hand_of_order', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        s(fn).save(path)
        out.append(path)
    path = os.path.join(assets, 'textures/gui/hand_of_order/court_clauses.png')
    court_clauses().save(path)
    out.append(path)
    path = os.path.join(assets, 'textures/gui/pathway_icons/hand_of_order.png')
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
