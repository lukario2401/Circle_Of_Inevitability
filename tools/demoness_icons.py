"""
Draws the Demoness (Apocalypse) wheel icons: 32x32 pixel art in a round violet frame, one per silk and disaster.
Run from the repository root:  python3 tools/demoness_icons.py
They go to src/main/resources/assets/circle_of_inevitability/textures/gui/ability_menus/apocalypse/ and are named
as the wheel entries' icons in Apocalypse.java. Replace any of them with a hand-drawn PNG of the same name.
"""
import math
import os
import random
from PIL import Image, ImageDraw

OUT = "src/main/resources/assets/circle_of_inevitability/textures/gui/ability_menus/apocalypse"
S = 32
C = 15.5

FRAME_DARK = (58, 22, 84, 255)
FRAME = (176, 80, 255, 255)
FRAME_LIGHT = (222, 170, 255, 255)
INSIDE = (22, 14, 34, 255)
INSIDE_EDGE = (34, 20, 52, 255)


def base(inside=INSIDE, edge=INSIDE_EDGE):
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    px = im.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - C, y - C)
            if d <= 15.6:
                if d > 13.6:
                    # the frame: lit from the top left
                    light = (x - C) * -0.7 + (y - C) * -0.7
                    px[x, y] = FRAME_LIGHT if light > 9 else FRAME if light > -6 else FRAME_DARK
                elif d > 12.6:
                    px[x, y] = FRAME_DARK
                else:
                    t = d / 12.6
                    px[x, y] = tuple(int(inside[i] * (1 - t) + edge[i] * t) for i in range(3)) + (255,)
    return im


def inside(x, y):
    return math.hypot(x - C, y - C) <= 12.4


def put(im, x, y, color):
    x, y = int(round(x)), int(round(y))
    if 0 <= x < S and 0 <= y < S and inside(x, y):
        im.putpixel((x, y), color)


def line(im, a, b, color, step=0.25):
    length = math.hypot(b[0] - a[0], b[1] - a[1])
    n = max(1, int(length / step))
    for i in range(n + 1):
        t = i / n
        put(im, a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, color)


def disc(im, cx, cy, r, color):
    for y in range(S):
        for x in range(S):
            if math.hypot(x - cx, y - cy) <= r:
                put(im, x, y, color)


def web(im, cx, cy, r, color, spokes=8, rings=3, faint=None):
    for k in range(spokes):
        a = math.pi * 2 * k / spokes + 0.2
        line(im, (cx, cy), (cx + math.cos(a) * r, cy + math.sin(a) * r), color)
    for j in range(1, rings + 1):
        rr = r * j / rings
        pts = [(cx + math.cos(math.pi * 2 * k / spokes + 0.2) * rr, cy + math.sin(math.pi * 2 * k / spokes + 0.2) * rr)
               for k in range(spokes)]
        for k in range(spokes):
            line(im, pts[k], pts[(k + 1) % spokes], faint or color)


WHITE = (240, 236, 255, 255)
SILK = (214, 206, 236, 255)
SILK_DIM = (150, 140, 180, 255)
VIOLET = (190, 110, 255, 255)
RED = (224, 32, 58, 255)
DARK_RED = (120, 14, 30, 255)
GREEN = (156, 255, 74, 255)
DARK_GREEN = (60, 110, 30, 255)
ICE = (184, 228, 255, 255)
ICE_DARK = (90, 150, 210, 255)
GREY = (150, 150, 168, 255)
GREY_DARK = (90, 90, 110, 255)
BROWN = (110, 74, 52, 255)
BROWN_DARK = (70, 46, 34, 255)
BLACK = (12, 6, 16, 255)
ORANGE = (255, 140, 60, 255)


def silk_snare():
    im = base()
    web(im, 15.5, 17, 10, WHITE, spokes=8, rings=3, faint=SILK_DIM)
    disc(im, 15.5, 17, 1.2, VIOLET)
    return im


def web_swing():
    im = base()
    web(im, 22, 9, 5, SILK, spokes=6, rings=2, faint=SILK_DIM)
    line(im, (22, 9), (10, 22), WHITE)
    # a small figure hanging from the end of the line
    disc(im, 9.5, 21.5, 1.6, VIOLET)
    line(im, (9, 23), (8, 27), VIOLET)
    line(im, (8, 25), (6, 24), VIOLET)
    line(im, (8, 25), (10, 26), VIOLET)
    return im


def trip_web():
    im = base()
    for x in (6, 25):
        line(im, (x, 10), (x, 26), BROWN)
        disc(im, x, 18, 1.2, SILK)
    line(im, (6, 18), (25, 18), (210, 204, 230, 200))
    # someone falling over it
    disc(im, 17, 12, 1.6, RED)
    line(im, (16, 13), (12, 16), RED)
    line(im, (14, 14), (16, 17), RED)
    line(im, (12, 16), (10, 15), RED)
    return im


def cocoon():
    im = base()
    for y in range(S):
        for x in range(S):
            if ((x - 15.5) / 6.5) ** 2 + ((y - 16) / 10) ** 2 <= 1:
                put(im, x, y, SILK if (x + y) % 5 else SILK_DIM)
    for k in range(-3, 4):
        line(im, (9, 12 + k * 3), (22, 16 + k * 3), SILK_DIM)
    for y in range(S):
        for x in range(S):
            d = ((x - 15.5) / 6.5) ** 2 + ((y - 16) / 10) ** 2
            if 0.82 < d <= 1.0:
                put(im, x, y, WHITE)
    return im


def threads_of_life():
    im = base()
    disc(im, 8, 16, 3, GREY_DARK)
    # a heart on the right
    disc(im, 21, 14, 2.6, RED)
    disc(im, 25, 14, 2.6, RED)
    for y in range(14, 22):
        w = (22 - y) * 0.75
        line(im, (23 - w - 1.5, y), (23 + w + 1.5, y), RED)
    for k, off in enumerate((-3, 0, 3)):
        pts = [(8 + t, 16 + off * (1 - t / 13) + math.sin(t * 0.7 + k) * 1.2) for t in range(0, 14)]
        for i in range(len(pts) - 1):
            line(im, pts[i], pts[i + 1], DARK_RED if k == 1 else RED)
    return im


def web_of_fate():
    im = base()
    web(im, 15.5, 15.5, 11, VIOLET, spokes=10, rings=4, faint=(120, 60, 170, 255))
    disc(im, 15.5, 15.5, 2, WHITE)
    for a in (0.6, 2.4, 4.3):
        put(im, 15.5 + math.cos(a) * 7, 15.5 + math.sin(a) * 7, WHITE)
    return im


def earthquake():
    im = base()
    for y in range(18, S):
        for x in range(S):
            put(im, x, y, BROWN if (x * 3 + y) % 7 else BROWN_DARK)
    pts = [(4, 18), (9, 20), (12, 17), (16, 22), (19, 19), (23, 24), (28, 20)]
    for i in range(len(pts) - 1):
        line(im, pts[i], pts[i + 1], RED)
        line(im, (pts[i][0], pts[i][1] + 1), (pts[i + 1][0], pts[i + 1][1] + 1), ORANGE)
    for (x, y, r) in ((9, 11, 1.8), (16, 8, 2.2), (22, 12, 1.6)):
        disc(im, x, y, r, GREY)
        put(im, x - 0.6, y - 0.6, (200, 200, 210, 255))
    return im


def hailstorm():
    im = base()
    for (x, y, r) in ((10, 10, 4), (16, 8, 5), (22, 10, 4), (13, 12, 4), (19, 12, 4)):
        disc(im, x, y, r, GREY_DARK)
    for (x, y, r) in ((10, 9, 3), (16, 7, 4), (22, 9, 3)):
        disc(im, x, y, r, GREY)
    for (x, y) in ((9, 19), (15, 22), (21, 18), (12, 25), (19, 25), (24, 22)):
        disc(im, x, y, 1.3, ICE)
        put(im, x - 0.5, y - 0.5, WHITE)
        line(im, (x - 1, y - 3), (x - 0.4, y - 1.5), ICE_DARK)
    return im


def tornado():
    im = base()
    for i, y in enumerate(range(6, 27)):
        w = 10 - i * 0.42
        cx = 15.5 + math.sin(i * 0.35) * 2
        line(im, (cx - w, y), (cx + w, y), GREY if i % 3 else GREY_DARK)
        put(im, cx - w + (i * 3) % int(max(2, w * 2)), y, (200, 200, 214, 255))
    for (x, y) in ((6, 22), (25, 14), (24, 24)):
        disc(im, x, y, 0.8, BROWN)
    return im


def ice_age():
    im = base(inside=(20, 30, 46, 255), edge=(30, 46, 70, 255))
    for k in range(6):
        a = math.pi * 2 * k / 6 - math.pi / 2
        end = (15.5 + math.cos(a) * 10, 15.5 + math.sin(a) * 10)
        line(im, (15.5, 15.5), end, ICE)
        for t in (0.5, 0.75):
            p = (15.5 + math.cos(a) * 10 * t, 15.5 + math.sin(a) * 10 * t)
            for side in (-0.6, 0.6):
                b = a + side
                line(im, p, (p[0] + math.cos(b) * 3, p[1] + math.sin(b) * 3), ICE_DARK)
    disc(im, 15.5, 15.5, 1.5, WHITE)
    return im


def black_death():
    im = base(inside=(16, 24, 12, 255), edge=(26, 40, 20, 255))
    rnd = random.Random(4)
    for (x, y, r) in ((11, 17, 6), (20, 15, 6), (15, 11, 5), (16, 20, 5)):
        disc(im, x, y, r, DARK_GREEN)
    for (x, y, r) in ((11, 16, 4), (20, 14, 4), (15, 10, 3)):
        disc(im, x, y, r, (100, 170, 50, 255))
    # a skull in the cloud
    disc(im, 15.5, 15, 3.5, (220, 230, 200, 255))
    line(im, (13.5, 19), (17.5, 19), (220, 230, 200, 255))
    put(im, 14, 15, BLACK)
    put(im, 17, 15, BLACK)
    put(im, 14, 14, BLACK)
    put(im, 17, 14, BLACK)
    put(im, 15.5, 17, BLACK)
    for _ in range(12):
        put(im, rnd.uniform(5, 27), rnd.uniform(5, 27), GREEN)
    return im


def black_flame_rain():
    im = base(inside=(30, 12, 40, 255), edge=(44, 18, 60, 255))
    for (x, y) in ((9, 10), (18, 7), (24, 14), (12, 20), (20, 22), (15, 27)):
        line(im, (x + 5, y - 7), (x + 1, y - 1.5), (150, 80, 210, 255))
        disc(im, x, y, 2.4, VIOLET)
        disc(im, x, y + 0.3, 1.4, BLACK)
        put(im, x - 0.4, y - 2.2, (236, 200, 255, 255))
    return im


def doomsday_sky():
    im = base(inside=(70, 10, 18, 255), edge=(110, 18, 30, 255))
    for (x, y, r) in ((8, 8, 3), (14, 6, 4), (22, 7, 4), (26, 10, 3)):
        disc(im, x, y, r, DARK_RED)
    line(im, (24, 4), (14, 20), ORANGE)
    line(im, (25, 5), (15, 21), RED)
    disc(im, 13.5, 21.5, 2.6, (60, 40, 40, 255))
    put(im, 12.5, 20.5, ORANGE)
    # a red bolt
    for a, b in (((6, 10), (9, 16)), ((9, 16), (7, 17)), ((7, 17), (10, 24))):
        line(im, a, b, (255, 80, 90, 255))
    return im


ICONS = {
    "silk_snare": silk_snare, "web_swing": web_swing, "trip_web": trip_web, "cocoon": cocoon,
    "threads_of_life": threads_of_life, "web_of_fate": web_of_fate,
    "earthquake": earthquake, "hailstorm": hailstorm, "tornado": tornado, "ice_age": ice_age,
    "black_death": black_death, "black_flame_rain": black_flame_rain, "doomsday_sky": doomsday_sky,
}

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, draw in ICONS.items():
        draw().save(os.path.join(OUT, name + ".png"))
    print("wrote", len(ICONS), "icons to", OUT)
