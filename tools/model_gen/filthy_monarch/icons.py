"""Pixel-art ability icons on a framed abyss plate, 16 px HUD sprites and soft tintable particle masks.

Art is drawn at 32 px with hand-placed three-tone ramps (shadow, base, light), then finished by one pass that adds a
near-black outline and a thin glow in the art's accent colour. Wheel icons sit on an octagonal plate whose border
colour marks the wheel; HUD sprites are 2 x 2 reductions of the same art, re-outlined at 16 px. Particles are drawn
separately as soft grayscale masks so runtime tint reads clearly.
"""
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from catalog import ABILITIES, KEYS, STATUSES, PARTICLES, BASIC, WHEELS

# Shared palette (the model tools import these names too).
BLUE = (127, 216, 255, 255); PALE = (230, 250, 255, 255); BLACK = (18, 10, 14, 255)
RED = (200, 8, 21, 255); BLOOD = (138, 15, 26, 255); FILTH = (92, 90, 30, 255); EDGE = (172, 169, 80, 255)

BLUE_D = (34, 92, 168, 255); BLUE_M = (70, 150, 225, 255); WHITE = (255, 255, 255, 255)
RED_D = (96, 4, 14, 255); RED_L = (255, 92, 70, 255)
BLOOD_D = (62, 4, 12, 255); BLOOD_L = (188, 36, 44, 255)
FILTH_D = (44, 42, 14, 255); FILTH_L = (204, 200, 112, 255)
ABYSS = (44, 22, 50, 255); ABYSS_L = (78, 46, 96, 255); INK = (7, 3, 9, 255)
BONE = (206, 198, 170, 255); BONE_D = (124, 112, 88, 255); BONE_L = (244, 238, 220, 255)
VIOLET = (120, 44, 110, 255); VIOLET_L = (186, 90, 160, 255)

ACCENT = {'basic': RED, 'devil_arts': BLUE, 'desire_and_malice': VIOLET_L, 'foul_tongue': BLOOD_L, 'blood_and_filth': EDGE}


class Pen:
    def __init__(self):
        self.im = Image.new('RGBA', (32, 32)); self.d = ImageDraw.Draw(self.im)

    def poly(self, pts, c):
        self.d.polygon(pts, fill=c)

    def line(self, pts, c, w=1):
        self.d.line(pts, fill=c, width=w)

    def ell(self, box, c=None, outline=None, w=1):
        self.d.ellipse(box, fill=c, outline=outline, width=w)

    def rect(self, box, c):
        self.d.rectangle(box, fill=c)

    def px(self, pts, c):
        for p in pts:
            self.d.point(p, fill=c)

    def arc(self, box, a0, a1, c, w=1):
        self.d.arc(box, a0, a1, fill=c, width=w)

    # -- motifs ----------------------------------------------------------------------------------------------
    def flame(self, x=16, y=17, r=10, dark=BLUE_D, mid=BLUE, light=PALE, core=BLACK):
        """Black-hearted sulfur flame: three nested tongues and a dark ember at the root."""
        def tongue(s):
            return [(x, y - r * s - 3 * s), (x + 2 * s, y - 4 * s), (x + 6 * s, y - 8 * s), (x + r * s, y + 3 * s),
                    (x + r * .8 * s, y + 7 * s), (x + 4 * s, y + r * s), (x - 4 * s, y + r * s), (x - r * .85 * s, y + 6 * s),
                    (x - r * s, y + 1 * s), (x - 7 * s, y - 6 * s), (x - 4 * s, y - 1 * s), (x - 3 * s, y - 7 * s)]
        self.poly(tongue(1), dark); self.poly([(px, py + .8) for px, py in tongue(.78)], mid)
        self.poly([(px - .5, py + 2.6) for px, py in tongue(.5)], light)
        if core:
            self.poly([(x, y + 1), (x + 3, y + r * .55), (x + 1.5, y + r * .85), (x - 1.5, y + r * .85), (x - 3, y + r * .55)], core)

    def heart(self, x=16, y=17, s=1.0, base=BLOOD, dark=BLOOD_D, light=RED):
        P = lambda pts: [(x + px * s, y + py * s) for px, py in pts]
        self.poly(P([(0, -6), (-4, -10), (-10, -8), (-12, -2), (-9, 4), (0, 12), (9, 4), (12, -2), (10, -8), (4, -10)]), base)
        self.poly(P([(0, 12), (9, 4), (12, -2), (10, -6), (6, 2)]), dark)
        self.poly(P([(-9, -6), (-6, -8), (-3, -7), (-6, -4), (-9, -2)]), light)
        self.px(P([(-8, -5), (-7, -6)]), PALE)

    def drop(self, x=16, y=16, s=1.0, base=BLOOD, dark=BLOOD_D, light=RED):
        P = lambda pts: [(x + px * s, y + py * s) for px, py in pts]
        self.poly(P([(0, -13), (5, -4), (8, 2), (8, 6), (5, 10), (0, 12), (-5, 10), (-8, 6), (-8, 2), (-5, -4)]), base)
        self.poly(P([(8, 2), (8, 6), (5, 10), (0, 12), (2, 8), (5, 4)]), dark)
        self.poly(P([(-4, -2), (-6, 3), (-5, 6), (-3, 3)]), light)
        self.px(P([(-4, -1)]), PALE)

    def devil(self, x=16, y=16, eyes=PALE, mouth=RED, skin=BLACK, hl=ABYSS_L, horn=BONE):
        """Horned devil head: curling horns, brow, glowing eyes and a red grin."""
        for sd in (-1, 1):
            self.poly([(x + sd * 5, y - 5), (x + sd * 9, y - 9), (x + sd * 11, y - 13), (x + sd * 10, y - 7), (x + sd * 7, y - 3)], horn)
            self.line([(x + sd * 9, y - 9), (x + sd * 11, y - 13)], BONE_L if sd < 0 else BONE_D)
        self.poly([(x - 7, y - 5), (x + 7, y - 5), (x + 8, y + 1), (x + 5, y + 7), (x + 2, y + 11), (x - 2, y + 11), (x - 5, y + 7), (x - 8, y + 1)], skin)
        self.poly([(x - 7, y - 5), (x - 2, y - 5), (x - 5, y - 1), (x - 8, y + 1)], hl)
        for sd in (-1, 1):
            self.poly([(x + sd * 2, y - 1), (x + sd * 6, y - 2), (x + sd * 5, y + 1), (x + sd * 2, y + 1)], eyes)
        self.line([(x - 4, y + 5), (x - 2, y + 7), (x + 2, y + 7), (x + 4, y + 5)], mouth)
        self.px([(x - 2, y + 6), (x + 2, y + 6)], BONE_L)

    def seed(self, x=16, y=16, s=1.0, crack=RED):
        P = lambda pts: [(x + px * s, y + py * s) for px, py in pts]
        self.poly(P([(0, -11), (6, -4), (7, 3), (4, 9), (0, 11), (-4, 9), (-7, 3), (-6, -4)]), BLACK)
        self.poly(P([(-6, -4), (0, -11), (-2, -3), (-5, 4), (-7, 3)]), ABYSS_L)
        self.line(P([(0, -7), (-2, -1), (2, 2), (0, 8)]), crack)
        self.px(P([(1, -1), (-1, 4)]), RED_L)

    def blade(self, tip=(27, 4), hilt=(10, 21), width=3, edge=PALE, body=BLUE, dark=BLUE_D):
        tx, ty = tip; hx, hy = hilt
        dx, dy = tx - hx, ty - hy; ln = math.hypot(dx, dy); nx, ny = -dy / ln, dx / ln
        self.poly([(hx + nx * width, hy + ny * width), (tx, ty), (hx - nx * width, hy - ny * width)], dark)
        self.poly([(hx + nx * width * .4, hy + ny * width * .4), (tx, ty), (hx + nx * width, hy + ny * width)], body)
        self.line([(hx + nx * width, hy + ny * width), (tx, ty)], edge)
        gx, gy = hx - dx / ln * 1, hy - dy / ln * 1
        self.line([(gx + nx * 4, gy + ny * 4), (gx - nx * 4, gy - ny * 4)], BONE_D, 2)
        self.line([(gx + nx * 4, gy + ny * 4), (gx - nx * 4, gy - ny * 4)], BONE, 1)
        self.line([(gx - dx / ln, gy - dy / ln), (gx - dx / ln * 5, gy - dy / ln * 5)], BLOOD, 2)
        self.px([(round(gx - dx / ln * 6), round(gy - dy / ln * 6))], RED_L)

    def waves(self, rows=(11, 17, 23), base=FILTH, light=FILTH_L, dark=FILTH_D):
        for i, y in enumerate(rows):
            pts = [(3 + k, y + 2.5 * math.sin((k + i * 3) * .55)) for k in range(0, 27)]
            self.line([(px, py + 1) for px, py in pts], dark, 3); self.line(pts, base, 2)
            self.line([(px, py - 1) for px, py in pts[::2]], light, 1)

    def puff(self, x, y, r, base=FILTH, light=FILTH_L, dark=FILTH_D):
        self.ell((x - r, y - r, x + r, y + r), dark); self.ell((x - r, y - r, x + r - 1, y + r - 2), base)
        self.ell((x - r + 2, y - r + 1, x, y - 1), light)

    def figure(self, x=16, y=16, body=BLACK, rim=VIOLET_L, eyes=RED_L):
        """Hooded silhouette: head, shoulders and a ragged hem."""
        self.ell((x - 4, y - 11, x + 4, y - 3), body)
        self.poly([(x - 4, y - 3), (x + 4, y - 3), (x + 9, y + 11), (x + 5, y + 9), (x + 2, y + 12), (x - 1, y + 9), (x - 5, y + 12), (x - 9, y + 11)], body)
        self.poly([(x - 4, y - 3), (x - 1, y - 3), (x - 3, y + 9), (x - 5, y + 12), (x - 9, y + 11)], ABYSS)
        self.arc((x - 4, y - 11, x + 4, y - 3), 160, 260, rim)
        self.line([(x - 4, y - 3), (x - 9, y + 11)], rim)
        self.px([(x - 2, y - 7), (x + 2, y - 7)], eyes)

    def ring(self, c=BLUE, box=(4, 4, 27, 27), w=2, dark=None):
        if dark:
            self.ell((box[0], box[1] + 1, box[2], box[3] + 1), outline=dark, w=w)
        self.ell(box, outline=c, w=w)


def art(name):
    p = Pen()
    if name == 'lawless_lunge':
        for i, (x, y) in enumerate(((4, 15), (8, 22), (12, 27))):
            p.line([(x - 2, y + 2), (x + 5, y - 5)], (BLUE_D, BLUE_M, BLUE_D)[i])
        p.blade((28, 4), (13, 19), 3)
        p.px([(28, 4), (26, 6)], WHITE)
    elif name == 'dirty_trick':
        p.poly([(4, 22), (9, 19), (12, 23), (10, 28), (5, 28)], (98, 62, 40, 255))
        p.poly([(4, 22), (9, 19), (8, 21), (5, 24)], (150, 104, 70, 255)); p.line([(6, 20), (9, 18)], BONE)
        for i, (x, y) in enumerate(((14, 16), (17, 12), (21, 9), (24, 14), (19, 18), (26, 7), (27, 18), (22, 4), (15, 9), (11, 13))):
            c = (FILTH_L, FILTH, BLOOD, EDGE, BONE_D)[i % 5]
            p.rect((x, y, x + 1 + i % 2, y + 1 + (i + 1) % 2), c)
        p.line([(12, 19), (20, 12)], PALE); p.line([(13, 22), (24, 16)], BLUE_D)
    elif name == 'sulfur_flame':
        p.flame(16, 17, 11)
    elif name == 'sink':
        p.ell((2, 17, 29, 29), BLACK); p.ell((5, 19, 26, 26), ABYSS); p.arc((2, 17, 29, 29), 200, 340, BLUE_M)
        p.arc((7, 20, 24, 25), 190, 350, VIOLET)
        p.ell((12, 13, 19, 20), BLACK); p.arc((12, 13, 19, 20), 180, 270, BLUE_M); p.px([(14, 16), (17, 16)], PALE)
        p.rect((11, 19, 20, 21), BLACK)
        for sd in (-1, 1):
            p.line([(16 + sd * 6, 20), (16 + sd * 8, 13), (16 + sd * 7, 10)], BLACK, 2)
            p.px([(16 + sd * 7, 9), (16 + sd * 8, 10)], BONE)
        p.line([(16, 2), (16, 7)], RED, 2); p.poly([(12, 7), (20, 7), (16, 11)], RED)
    elif name == 'killing_spree':
        for k, x in enumerate((5, 12, 19)):
            pts = [(x + 6, 3 + k), (x + 4, 10 + k), (x + 1, 18 + k), (x - 1, 27)]
            p.line(pts, BLOOD_D, 4); p.line(pts, RED, 2); p.line([(a + 1, b) for a, b in pts[:3]], RED_L, 1)
            p.px([(x - 1, 29), (x + 1, 28)], BLOOD)
    elif name == 'devil_worship':
        p.ell((2, 20, 29, 30), RED_D); p.ell((4, 21, 27, 28), INK); p.arc((2, 20, 29, 30), 180, 360, RED)
        for x, y in ((6, 24), (16, 27), (26, 24)):
            p.rect((x - 1, y - 3, x, y), BONE); p.poly([(x - 1, y - 7), (x + 1, y - 4), (x - 1, y - 3), (x - 2, y - 4)], BLUE)
            p.px([(x - 1, y - 5)], PALE)
        p.devil(16, 13)
    elif name == 'sulfur_fireball':
        p.poly([(2, 29), (6, 20), (9, 22), (11, 15), (15, 20), (20, 24), (14, 26), (11, 30)], BLUE_D)
        p.poly([(5, 27), (8, 23), (10, 24), (12, 19), (15, 22), (12, 26)], BLUE)
        p.line([(4, 22), (8, 18)], BLUE_M); p.line([(10, 28), (15, 27)], BLUE_M)
        p.ell((11, 4, 28, 21), BLUE_D); p.ell((12, 4, 27, 19), BLUE); p.ell((15, 6, 25, 15), PALE)
        p.ell((18, 8, 24, 14), WHITE); p.px([(16, 7), (17, 7), (15, 8)], WHITE)
        p.arc((11, 4, 28, 21), 300, 60, BLUE_D)
    elif name == 'devil_form':
        for sd in (-1, 1):
            p.poly([(16 + sd * 6, 14), (16 + sd * 13, 5), (16 + sd * 13, 12), (16 + sd * 14, 20), (16 + sd * 11, 17), (16 + sd * 9, 22), (16 + sd * 7, 19)], RED_D)
            p.line([(16 + sd * 13, 6), (16 + sd * 11, 17)], BLOOD)
            p.line([(16 + sd * 6, 14), (16 + sd * 13, 5)], BONE)
        p.devil(16, 17)
    elif name == 'sword_of_lava':
        p.blade((28, 3), (11, 21), 3, PALE, BLUE, BLACK)
        for x, y in ((22, 9), (18, 13), (14, 17)):
            p.px([(x, y), (x + 1, y - 1)], BLUE)
        p.flame(25, 9, 4, core=None); p.flame(18, 15, 3, core=None)
    elif name == 'flame_cage':
        p.ell((4, 24, 27, 29), BLACK); p.ell((4, 3, 27, 8), outline=BLUE_D)
        for x in (7, 12, 16, 20, 25):
            p.line([(x, 6), (x, 26)], BLUE_D, 2); p.line([(x, 7), (x, 25)], BLUE if x != 16 else PALE)
        p.arc((4, 24, 27, 29), 0, 180, BLUE)
        p.flame(16, 18, 4)
    elif name == 'volcanic_eruption':
        p.poly([(2, 29), (10, 15), (13, 17), (19, 17), (22, 15), (30, 29)], BLACK)
        p.poly([(2, 29), (10, 15), (13, 17), (9, 29)], ABYSS_L)
        p.line([(15, 18), (13, 23), (14, 28)], BLUE); p.line([(19, 18), (21, 24), (24, 29)], BLUE_M)
        p.flame(16, 9, 7, core=None)
        for x, y in ((6, 6), (26, 5), (9, 2), (24, 11)):
            p.rect((x, y, x + 1, y + 1), BLUE if x % 2 else PALE)
    elif name == 'toxic_vapor':
        for x in (9, 16, 23):
            p.line([(x, 12), (x - 2, 8), (x, 5), (x - 1, 2)], EDGE)
        p.puff(9, 20, 6); p.puff(23, 20, 6); p.puff(16, 17, 8)
        p.rect((5, 24, 27, 26), FILTH_D); p.line([(6, 27), (26, 27)], FILTH_D)
        for x, y, r in ((12, 18, 1), (19, 20, 1), (16, 14, 1)):
            p.ell((x - r, y - r, x + r, y + r), outline=FILTH_L)
    elif name == 'hellfire':
        p.ell((2, 21, 29, 30), RED_D); p.ell((4, 22, 27, 28), BLACK)
        p.flame(16, 15, 9)
        p.flame(6, 22, 3, core=None); p.flame(26, 22, 3, core=None)
    elif name == 'flames_of_the_abyss':
        p.line([(3, 29), (28, 4)], BLACK, 7); p.line([(3, 29), (28, 4)], BLUE_D, 5); p.line([(3, 29), (28, 4)], BLUE, 3)
        p.line([(4, 28), (28, 4)], PALE, 1)
        for x, y in ((8, 18), (14, 12), (20, 7), (11, 25), (18, 19)):
            p.poly([(x, y), (x + 2, y - 4), (x + 3, y)], BLUE_M)
        p.ell((24, 1, 31, 8), PALE)
    elif name == 'shadow_shift':
        p.figure(11, 16, (40, 20, 46, 150), VIOLET, BLOOD)
        p.figure(19, 16)
        for y in (11, 16, 21):
            p.line([(1, y), (6, y)], VIOLET)
    elif name == 'desire_control':
        p.line([(4, 3), (28, 3)], BONE_D, 2)
        for x in (8, 16, 24):
            p.line([(x, 4), (x if x == 16 else (x + 16) // 2, 11)], PALE)
        p.heart(16, 19, .95)
    elif name == 'mental_shock':
        for r in (14, 11):
            p.arc((16 - r, 16 - r, 16 + r, 16 + r), 200, 340, VIOLET_L if r == 14 else RED)
        p.devil(16, 19, eyes=RED_L, mouth=RED)
        p.line([(4, 18), (7, 16), (5, 14)], PALE); p.line([(28, 18), (25, 16), (27, 14)], PALE)
    elif name == 'desire_incarnation':
        p.ell((3, 23, 28, 30), BLACK); p.ell((5, 24, 26, 28), VIOLET)
        p.figure(16, 13)
        p.heart(16, 13, .3)
        for x in (9, 13, 20, 24):
            p.line([(x, 22), (x, 25 + x % 3)], ABYSS, 2)
        p.px([(6, 25), (22, 26)], VIOLET_L)
    elif name == 'desire_symbiosis':
        p.heart(10, 15, .55); p.heart(22, 19, .55)
        for i in range(5):
            x = 12 + i * 2; y = 15 + i
            p.ell((x - 1, y - 1, x + 1, y + 1), outline=BONE if i % 2 else BONE_D)
    elif name == 'filthy_illusion':
        p.figure(8, 17, (52, 40, 30, 180), FILTH, FILTH_L)
        p.figure(24, 17, (52, 40, 30, 180), FILTH, FILTH_L)
        p.figure(16, 15)
    elif name == 'seed_of_malice':
        p.ring(BLUE_D, (3, 4, 28, 29), 1)
        p.seed(16, 16, 1)
        for sd in (-1, 1):
            p.line([(16 + sd * 4, 24), (16 + sd * 9, 28)], RED)
        p.poly([(24, 3), (27, 7), (24, 11), (21, 7)], RED); p.px([(24, 6)], RED_L)
    elif name in ('slow', 'die', 'corruption', 'confusion', 'sloth', 'blasphemy', 'blather'):
        # Foul Tongue: a spoken Devil word, each with its own nonlinguistic sigil inside a blood circle.
        p.ell((3, 3, 28, 28), BLOOD_D); p.ell((5, 5, 26, 26), INK)
        if name == 'slow':
            p.poly([(10, 8), (22, 8), (16, 16)], BONE_D); p.poly([(16, 16), (10, 24), (22, 24)], BONE_D)
            p.poly([(12, 9), (20, 9), (16, 14)], PALE); p.poly([(16, 18), (12, 23), (20, 23)], BLUE)
            p.line([(9, 7), (23, 7)], RED, 2); p.line([(9, 25), (23, 25)], RED, 2)
        elif name == 'die':
            p.ell((9, 7, 23, 20), BONE); p.rect((11, 18, 21, 23), BONE)
            p.ell((11, 12, 15, 16), INK); p.ell((17, 12, 21, 16), INK); p.px([(13, 14), (19, 14)], RED_L)
            p.line([(16, 17), (16, 18)], INK); p.line([(12, 21), (20, 21)], INK); p.px([(14, 22), (18, 22)], INK)
            p.line([(10, 8), (14, 11)], BONE_L)
        elif name == 'corruption':
            for a in range(5):
                t = a * math.tau / 5 - math.pi / 2
                pts = [(16 + r * math.cos(t + r * .09), 16 + r * math.sin(t + r * .09)) for r in range(4, 12)]
                p.line(pts, VIOLET, 2); p.line(pts[-3:], VIOLET_L)
            p.seed(16, 16, .6)
        elif name == 'confusion':
            pts = [(16 + .9 * k * math.cos(k * .55), 16 + .9 * k * math.sin(k * .55)) for k in range(1, 13)]
            p.line(pts, BLUE, 2); p.line(pts[:7], PALE)
        elif name == 'sloth':
            p.poly([(6, 18), (11, 13), (16, 12), (21, 13), (26, 18), (16, 20)], BONE_D)
            p.line([(6, 18), (11, 20), (16, 21), (21, 20), (26, 18)], BONE_L, 2)
            p.line([(11, 13), (16, 12), (21, 13)], BONE)
            for i, x in enumerate((9, 13, 17, 21)):
                p.line([(x + 1, 21), (x, 24)], BONE)
            p.px([(16, 22)], RED_L)
            p.line([(18, 5), (23, 5), (18, 10), (23, 10)], PALE); p.line([(11, 6), (14, 6), (11, 9), (14, 9)], BLUE_M)
        elif name == 'blasphemy':
            p.ell((8, 6, 24, 12), outline=BONE_L, w=2)
            p.rect((14, 4, 17, 14), INK)
            p.line([(9, 26), (23, 8)], RED, 3); p.line([(10, 26), (23, 9)], RED_L)
            p.px([(12, 9), (19, 7)], BONE_D)
        elif name == 'blather':
            p.poly([(6, 12), (16, 8), (26, 12), (22, 20), (10, 20)], BLOOD)
            p.poly([(9, 13), (16, 10), (23, 13), (20, 18), (12, 18)], INK)
            for x in (11, 14, 18, 21):
                p.px([(x, 12), (x, 13)], BONE_L)
            p.poly([(13, 16), (19, 16), (19, 22), (16, 24), (13, 22)], RED); p.line([(16, 17), (16, 22)], BLOOD_D)
            for i, (x, y) in enumerate(((9, 24), (14, 26), (19, 24), (24, 26))):
                p.line([(x, y), (x + 1, y - 2)], (BLUE, RED_L, PALE, BLUE_M)[i])
        p.ell((3, 3, 28, 28), outline=BLOOD, w=2); p.arc((3, 3, 28, 28), 200, 290, RED_L)
    elif name == 'acid_swamp':
        p.ell((2, 17, 29, 30), FILTH_D); p.ell((3, 17, 28, 27), FILTH); p.arc((3, 17, 28, 27), 190, 330, FILTH_L)
        for x, y, r in ((9, 20, 2), (18, 22, 3), (23, 19, 1)):
            p.ell((x - r, y - r, x + r, y + r), outline=FILTH_L)
        p.line([(11, 24), (20, 23)], BONE, 2); p.ell((19, 21, 22, 25), BONE)
        for x, y in ((8, 10), (15, 6), (22, 11), (12, 2)):
            p.ell((x - 1, y - 1, x + 1, y + 1), EDGE)
    elif name == 'blood_ignition':
        p.heart(16, 19, .9)
        p.flame(16, 12, 6, core=None)
    elif name == 'blood_burst':
        p.drop(16, 17, .75)
        for a in range(8):
            t = a * math.pi / 4 + .2; r0, r1 = 11, 14 + a % 2
            p.line([(16 + r0 * math.cos(t), 17 + r0 * math.sin(t)), (16 + r1 * math.cos(t), 17 + r1 * math.sin(t))], RED, 2)
            p.px([(round(16 + (r1 + 1) * math.cos(t)), round(17 + (r1 + 1) * math.sin(t)))], BLOOD)
    elif name == 'tide_of_filth':
        p.poly([(2, 29), (2, 18), (7, 10), (15, 5), (24, 6), (29, 11), (25, 11), (21, 10), (18, 14), (22, 18), (29, 29)], FILTH)
        p.poly([(2, 29), (2, 18), (7, 10), (8, 18), (6, 29)], FILTH_D)
        p.line([(7, 10), (15, 5), (24, 6), (29, 11)], FILTH_L, 2)
        p.px([(26, 12), (27, 14), (22, 12)], FILTH_L)
        for x in (10, 13, 16):
            p.line([(x, 25), (x - 1, 20), (x + (x - 13) // 2, 17)], BONE_D, 2)
    elif name == 'the_corroder':
        p.poly([(15, 6), (27, 10), (27, 22), (15, 26), (8, 22), (8, 10)], (60, 80, 110, 160))
        p.line([(15, 6), (27, 10), (27, 22), (15, 26), (8, 22), (8, 10), (15, 6)], BLUE_M)
        p.line([(17, 16), (22, 9)], PALE); p.line([(17, 16), (24, 20)], PALE); p.line([(17, 16), (12, 24)], PALE)
        p.line([(2, 29), (17, 16)], FILTH_D, 5); p.line([(2, 29), (17, 16)], EDGE, 3); p.line([(3, 28), (16, 17)], FILTH_L)
        for x, y in ((14, 21), (10, 17), (19, 20)):
            p.px([(x, y), (x, y + 1)], EDGE)
    elif name == 'abyssal_sea':
        p.ell((3, 3, 28, 22), ABYSS); p.arc((3, 3, 28, 22), 180, 360, BLUE_M, 2)
        p.ell((12, 6, 20, 14), PALE); p.ell((14, 6, 22, 13), ABYSS)
        p.rect((2, 15, 29, 29), BLACK)
        p.waves((18, 23, 28), BLUE_D, BLUE, INK)
    elif name == 'the_abyss':
        p.ell((2, 2, 29, 29), INK); p.ring(BLOOD, (2, 2, 29, 29), 2, RED_D)
        p.devil(16, 17, eyes=RED_L, mouth=RED_L, skin=BLACK, hl=ABYSS)
        for x, y in ((7, 9), (25, 9), (6, 21), (26, 21), (16, 27)):
            p.px([(x, y), (x + 1, y)], PALE)
    else:
        status(p, name)
    return p.im


def status(p, name):
    """Statuses and the art reused by key icons."""
    if name in ('depravity', 'seed'):
        p.seed(16, 16, 1.1)
        if name == 'depravity':
            p.ring(RED, (3, 3, 28, 28), 1)
    elif name in ('offerings', 'offering'):
        p.ring(RED, (3, 6, 28, 30), 2, RED_D)
        p.drop(16, 15, .7)
        for x in (5, 16, 27):
            p.line([(x, 2), (x, 6)], BLUE, 2)
    elif name in ('danger_premonition', 'danger_eye'):
        p.poly([(2, 16), (9, 9), (16, 7), (23, 9), (30, 16), (23, 23), (16, 25), (9, 23)], BONE)
        p.ell((10, 10, 22, 22), RED); p.ell((13, 13, 19, 19), BLOOD_D); p.line([(16, 10), (16, 22)], INK, 2)
        p.px([(13, 12)], WHITE)
    elif name == 'bloodlust':
        p.devil(16, 18, eyes=RED_L)
        for x in (6, 26):
            p.line([(x, 2), (x - 1, 7), (x, 11)], BLOOD, 2)
    elif name == 'bleeding':
        p.drop(16, 17, .9); p.line([(4, 4), (27, 11)], PALE, 2); p.line([(4, 5), (27, 12)], BONE_D)
    elif name == 'corrosion':
        p.rect((5, 5, 26, 26), (60, 80, 110, 200)); p.line([(5, 5), (26, 5), (26, 26), (5, 26), (5, 5)], BLUE_M)
        p.line([(3, 28), (28, 3)], EDGE, 3)
        for x, y in ((9, 22), (14, 17), (20, 11)):
            p.line([(x, y + 2), (x, y + 6)], FILTH_L)
    elif name == 'fear':
        p.devil(16, 16, eyes=WHITE, mouth=PALE)
        p.line([(12, 26), (16, 22), (20, 26)], PALE)
    elif name == 'silenced':
        p.poly([(7, 13), (16, 10), (25, 13), (21, 21), (11, 21)], BLOOD)
        p.line([(10, 16), (22, 16)], INK, 2)
        p.line([(4, 28), (28, 4)], RED, 3); p.line([(4, 27), (27, 4)], RED_L)
    elif name in ('symbiosis',):
        p.heart(10, 15, .55); p.heart(22, 19, .55); p.line([(12, 15), (20, 19)], BONE, 2)
    elif name == 'malice_ward':
        p.seed(16, 16, .9); p.ring(BLUE, (3, 3, 28, 28), 2, BLUE_D)
    elif name in ('single_drop', 'blood_drop'):
        p.drop(16, 16, 1.0)
        if name == 'single_drop':
            p.ring(BLUE, (2, 2, 29, 29), 1)
    elif name == 'king_of_filth':
        p.waves((17, 23, 28))
        p.poly([(5, 12), (5, 4), (10, 8), (16, 2), (22, 8), (27, 4), (27, 12)], RED)
        p.line([(5, 12), (27, 12)], BLOOD_D, 2); p.px([(16, 7)], PALE); p.px([(10, 10), (22, 10)], BLUE)
    elif name == 'interference':
        p.devil(16, 17); p.line([(3, 29), (29, 3)], RED, 3); p.line([(3, 28), (28, 3)], RED_L)
    elif name in ('demonic_attribute',):
        art_into(p, 'sulfur_flame')
    else:
        raise ValueError('Missing art: ' + name)


def art_into(p, name):
    p.im.alpha_composite(art(name))


KEY_ART = dict(zip(KEYS, ['lawless_lunge', 'sulfur_flame', 'killing_spree', 'devil_form', 'desire_control', 'blasphemy', 'tide_of_filth']))


def group(name):
    if name in BASIC or name in KEYS[:3]:
        return 'basic'
    for wheel, members in WHEELS.items():
        if name in members or name == wheel:
            return wheel
    return 'basic'


# -- finishing -------------------------------------------------------------------------------------------------

def _shift(mask, dx, dy):
    out = np.zeros_like(mask)
    h, w = mask.shape
    out[max(0, dy):h + min(0, dy), max(0, dx):w + min(0, dx)] = mask[max(0, -dy):h + min(0, -dy), max(0, -dx):w + min(0, -dx)]
    return out


def finish(im, glow=None, outline=INK):
    """Near-black outline around the art plus a two-step glow in the accent colour."""
    a = np.asarray(im).astype(float); solid = a[..., 3] > 0
    near = np.zeros_like(solid)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        near |= _shift(solid, dx, dy)
    edge = near & ~solid
    out = a.copy()
    out[edge] = outline
    if glow is not None:
        ring1 = np.zeros_like(solid)
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                ring1 |= _shift(solid | edge, dx, dy)
        ring1 &= ~(solid | edge)
        out[ring1] = list(glow[:3]) + [110]
    return Image.fromarray(out.astype(np.uint8), 'RGBA')


def plate(accent, key=False):
    """Octagonal abyss plate with a bevelled border in the wheel's accent colour."""
    im = Image.new('RGBA', (32, 32)); d = ImageDraw.Draw(im)
    oct_ = [(4, 0), (27, 0), (31, 4), (31, 27), (27, 31), (4, 31), (0, 27), (0, 4)]
    d.polygon(oct_, fill=INK)
    inner = [(5, 2), (26, 2), (29, 5), (29, 26), (26, 29), (5, 29), (2, 26), (2, 5)]
    for y in range(2, 30):
        t = (y - 2) / 27
        c = tuple(int(30 * (1 - t) + 10 * t + (8 if y < 6 else 0)) for _ in range(3))
        d.line([(1, y), (30, y)], fill=(c[0] + 8, c[1] - 6, c[2] + 10, 240))
    mask = Image.new('L', (32, 32)); ImageDraw.Draw(mask).polygon(inner, fill=255)
    bg = Image.new('RGBA', (32, 32)); bg.paste(im, (0, 0), mask)
    out = Image.new('RGBA', (32, 32)); ImageDraw.Draw(out).polygon(oct_, fill=INK)
    out.alpha_composite(bg)
    dk = tuple(int(v * .45) for v in accent[:3]) + (255,)
    ImageDraw.Draw(out).line(inner + [inner[0]], fill=dk, width=1)
    ImageDraw.Draw(out).line(inner[6:] + inner[:2], fill=accent, width=1)
    if key:
        for x, y in ((1, 1), (29, 1), (1, 29), (29, 29)):
            ImageDraw.Draw(out).rectangle((x, y, x + 1, y + 1), fill=accent)
    return out


def wheel_icon(name):
    source = KEY_ART.get(name, name)
    accent = ACCENT[group(name)]
    out = plate(accent, key=name in KEYS)
    lit = np.asarray(finish(art(source), accent)).copy()
    inside = np.asarray(_inner_mask()) > 0
    lit[(lit[..., 3] == 110) & ~inside] = 0  # keep the glow off the bevelled border
    out.alpha_composite(Image.fromarray(lit, 'RGBA'))
    return out


def _inner_mask():
    mask = Image.new('L', (32, 32))
    ImageDraw.Draw(mask).polygon([(6, 3), (25, 3), (28, 6), (28, 25), (25, 28), (6, 28), (3, 25), (3, 6)], fill=255)
    return mask


def reduce16(im):
    """2 x 2 reduction that keeps thin bright strokes and drops lone dark pixels."""
    a = np.asarray(im).astype(float)
    out = np.zeros((16, 16, 4))
    for y in range(16):
        for x in range(16):
            block = a[y * 2:y * 2 + 2, x * 2:x * 2 + 2].reshape(4, 4)
            solid = block[block[:, 3] > 0]
            if len(solid) >= 2 or (len(solid) == 1 and solid[0, :3].max() > 150):
                lum = solid[:, :3].max(axis=1)
                out[y, x, :3] = solid[np.argsort(lum)][len(solid) // 2:, :3].mean(axis=0)
                out[y, x, 3] = 255
    return Image.fromarray(out.astype(np.uint8), 'RGBA')


def hud_icon(name):
    return finish(reduce16(art(KEY_ART.get(name, name))))


# -- particles ---------------------------------------------------------------------------------------------------

def particle(name):
    """Soft grayscale mask: drawn at 64 px, reduced to 16 px with a guaranteed opaque core."""
    S = 64; im = Image.new('L', (S, S)); d = ImageDraw.Draw(im); a = Image.new('L', (S, S)); da = ImageDraw.Draw(a)

    def both(fn, *args, value=255, alpha=255, **kw):
        fn(d)(*args, fill=value, **kw); fn(da)(*args, fill=alpha, **kw)
    P = lambda dd: dd.polygon; E = lambda dd: dd.ellipse; L = lambda dd: dd.line
    if name in ('sulfur_flame', 'spark'):
        if name == 'sulfur_flame':
            both(P, [(32, 2), (40, 22), (50, 30), (50, 46), (40, 60), (24, 60), (14, 46), (14, 32), (24, 22)], value=170, alpha=200)
            both(P, [(32, 18), (40, 36), (40, 50), (32, 58), (24, 50), (24, 36)], value=255)
        else:
            both(P, [(32, 2), (37, 27), (62, 32), (37, 37), (32, 62), (27, 37), (2, 32), (27, 27)], value=255)
            both(E, (24, 24, 40, 40), value=255)
    elif name == 'black_ember':
        both(P, [(20, 14), (46, 10), (54, 34), (38, 54), (14, 46), (10, 26)], value=90)
        both(L, [(22, 20), (32, 32), (44, 24)], value=255, width=5); both(L, [(32, 32), (30, 48)], value=230, width=4)
    elif name == 'blood_drop':
        both(P, [(32, 2), (46, 28), (50, 42), (44, 56), (32, 62), (20, 56), (14, 42), (18, 28)], value=200)
        both(E, (20, 32, 30, 44), value=255)
    elif name in ('sludge', 'fog'):
        for i, (x, y, r) in enumerate(((22, 36, 16), (40, 32, 18), (30, 22, 14), (46, 44, 12))):
            both(E, (x - r, y - r, x + r, y + r), value=150 + i * 25, alpha=170 if name == 'fog' else 230)
        if name == 'sludge':
            both(L, [(20, 48), (20, 60)], value=200, width=5); both(L, [(42, 52), (42, 62)], value=200, width=5)
        both(E, (26, 26, 38, 38), value=255)
    elif name == 'seed':
        both(P, [(32, 4), (46, 20), (48, 40), (40, 56), (32, 60), (24, 56), (16, 40), (18, 20)], value=140)
        both(L, [(32, 12), (26, 30), (36, 38), (32, 54)], value=255, width=5)
    elif name == 'claw':
        for k in range(3):
            x = 12 + k * 14
            both(L, [(x + 10, 4), (x + 6, 22), (x, 42), (x - 4, 60)], value=255 - k * 20, width=7 - k)
    elif name == 'ripple':
        both(E, (4, 4, 60, 60), value=255, alpha=255); both(E, (12, 12, 52, 52), value=0, alpha=0)
        both(E, (22, 22, 42, 42), value=200, alpha=200); both(E, (27, 27, 37, 37), value=0, alpha=0)
    elif name == 'foul_glyph':
        for pts in (((14, 10), (14, 54)), ((14, 18), (34, 8)), ((34, 8), (34, 30)), ((34, 30), (52, 30)), ((44, 30), (44, 56)), ((22, 46), (36, 56))):
            both(L, list(pts), value=255, width=8)
    elif name == 'shadow':
        both(E, (22, 4, 42, 24), value=180)
        both(P, [(20, 22), (44, 22), (56, 60), (44, 52), (32, 62), (20, 52), (8, 60)], value=150, alpha=200)
    elif name == 'corrosion_streak':
        both(L, [(6, 58), (58, 6)], value=255, width=8)
        for x, y in ((20, 44), (32, 32), (44, 20)):
            both(L, [(x, y), (x, y + 14)], value=200, width=4)
    elif name == 'offering':
        both(P, [(32, 4), (58, 32), (32, 60), (6, 32)], value=170)
        both(P, [(32, 16), (46, 32), (32, 48), (18, 32)], value=255)
    elif name == 'horn':
        for sd in (-1, 1):
            both(P, [(32 + sd * 10, 46), (32 + sd * 26, 28), (32 + sd * 30, 2), (32 + sd * 16, 22), (32 + sd * 2, 34)], value=255)
    elif name == 'danger_eye':
        both(P, [(2, 32), (18, 16), (32, 12), (46, 16), (62, 32), (46, 48), (32, 52), (18, 48)], value=150)
        both(E, (20, 20, 44, 44), value=255)
        both(L, [(32, 20), (32, 44)], value=40, width=6)
    else:
        raise ValueError('Missing particle: ' + name)
    small = Image.merge('LA', (im.resize((16, 16), Image.Resampling.LANCZOS), a.resize((16, 16), Image.Resampling.LANCZOS)))
    arr = np.asarray(small).astype(int).copy()
    arr[..., 1] = np.where(arr[..., 1] < 24, 0, arr[..., 1])
    arr[..., 1] = np.where(arr[..., 1] > 200, 255, arr[..., 1])
    arr[..., 0] = np.where(arr[..., 1] == 0, 0, arr[..., 0])
    arr[np.unravel_index(np.argmax(arr[..., 1] + arr[..., 0] * 1e-3), arr.shape[:2])] = (255, 255)
    rgba = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'LA').convert('RGBA')
    return rgba


def strip(counter, count):
    """32 x 8 counter: five seeds (Depravity) or five blood drops (Offerings), lit up to count."""
    im = Image.new('RGBA', (32, 8)); d = ImageDraw.Draw(im)
    for i in range(5):
        x = 1 + i * 6; lit = i < count
        if counter == 'depravity':
            d.polygon([(x + 2, 0), (x + 4, 2), (x + 4, 5), (x + 2, 7), (x, 5), (x, 2)], fill=INK)
            d.polygon([(x + 2, 1), (x + 3, 2), (x + 3, 5), (x + 2, 6), (x + 1, 5), (x + 1, 2)], fill=BLACK if not lit else ABYSS_L)
            d.line([(x + 2, 2), (x + 2, 5)], fill=BLUE if lit else BLOOD_D)
            if lit:
                d.point((x + 2, 2), fill=PALE)
        else:
            d.polygon([(x + 2, 0), (x + 4, 3), (x + 4, 5), (x + 3, 7), (x + 1, 7), (x, 5), (x, 3)], fill=INK)
            d.polygon([(x + 2, 1), (x + 3, 3), (x + 3, 5), (x + 2, 6), (x + 1, 5), (x + 1, 3)], fill=RED if lit else BLOOD_D)
            if lit:
                d.point((x + 1, 3), fill=RED_L); d.point((x + 3, 5), fill=BLOOD)
    return im


def save(im, path):
    path.parent.mkdir(parents=True, exist_ok=True); im.save(path)


def generate(root):
    for n in dict.fromkeys(ABILITIES + KEYS):
        save(wheel_icon(n), root / f'textures/gui/ability_menus/filthy_monarch/{n}.png')
    for n in dict.fromkeys(ABILITIES + STATUSES):
        save(hud_icon(n), root / f'textures/gui/filthy_monarch/{n}.png')
    for counter in ('depravity', 'offerings'):
        for count in range(6):
            save(strip(counter, count), root / f'textures/gui/filthy_monarch/{counter}_{count}.png')
    save(hud_icon('devil_form'), root / 'textures/gui/pathway_icons/filthy_monarch.png')
    for n in PARTICLES:
        save(particle(n), root / f'textures/particle/fx/filthy_monarch/{n}.png')
