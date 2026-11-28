"""The Observer's framed canvases: one frame, many Paintings.

    python3 tools/model_gen/observer/paintings.py src/main/resources/assets/circle_of_inevitability

Every canvas is the same model: a lacquered black frame with gilt corners round a stretched canvas, the back showing
raw linen and stretcher bars. The Painting on its front is the model's second texture ("1"), so each Painting is one
picture in textures/model_fx/observer/paintings/ and one small JSON, canvas_<name>.json, that names it.

  Wide canvases (1.5 x 1 block at scale 1; ObserverFx plays them at 2, the 3 x 2 canvas of the plan): riot_of_colours
  (animated), itching_house, golden_sun, boat_in_a_storm (animated), giants_fist, painting_world (animated), blank.
  Tall canvases (1 x 1.5 block): self_portrait, hide_in_painting, hide_in_painting_empty, fantasy.

The front is north (-Z); the pivot is the middle of the canvas (8, 8, 8), so a canvas hung on a wall faces away from
it with the wall's yaw, and a canvas lying on the ground is pitched 90.

To add a Painting: a function p_<name>(w, h) returning one RGBA image (or a list of frames), one line in PAINTINGS
(name, painter, 'wide' or 'tall', frametime), and ObserverFx.Painting gets a constant with the same name.
Each picture is drawn at three times its size and brought down, so the brush strokes stay soft.
"""
import json
import math
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import models  # noqa: E402
from textures import atlas  # noqa: E402

NAMESPACE = models.NAMESPACE
FOLDER = models.FOLDER
FRAME_MATS = ['frame_wood', 'frame_gold', 'canvas_back']
UP = 3                                       # drawn at 3x, brought down


def c(hexstr, a=255):
    hexstr = hexstr.lstrip('#')
    return (int(hexstr[0:2], 16), int(hexstr[2:4], 16), int(hexstr[4:6], 16), a)


def board(w, h, color):
    img = Image.new('RGBA', (w * UP, h * UP), color)
    return img, ImageDraw.Draw(img)


def down(img, w, h):
    return img.resize((w, h), Image.LANCZOS)


def brush(d, pts, color, width, rnd=None, jitter=0.0):
    """A brush stroke through the points, a little wobble in it."""
    rnd = rnd or random.Random(0)
    pts = [(x + rnd.uniform(-jitter, jitter), y + rnd.uniform(-jitter, jitter)) for x, y in pts]
    d.line(pts, fill=color, width=int(width), joint='curve')
    for x, y in (pts[0], pts[-1]):
        r = width / 2
        d.ellipse([x - r, y - r, x + r, y + r], fill=color)


def dabs(d, rnd, box, colors, count, size):
    x0, y0, x1, y1 = box
    for _ in range(count):
        x, y = rnd.uniform(x0, x1), rnd.uniform(y0, y1)
        r = rnd.uniform(size * 0.5, size)
        a = rnd.uniform(0, math.pi)
        dx, dy = math.cos(a) * r, math.sin(a) * r
        d.line([(x - dx, y - dy), (x + dx, y + dy)], fill=colors[rnd.randrange(len(colors))], width=int(r * 0.7))


def canvas_grain(img, strength=18, seed=0):
    """The weave of the canvas showing through the paint."""
    px = img.load()
    w, h = img.size
    rnd = random.Random(seed)
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            k = ((x % 2) ^ (y % 2)) * strength // 2 - strength // 4 + rnd.randint(-3, 3)
            px[x, y] = (max(0, min(255, r + k)), max(0, min(255, g + k)), max(0, min(255, b + k)), a)
    return img


def figure(d, cx, foot, height, color):
    """A standing person, the way a painter blocks one in."""
    s = height / 7.0
    d.ellipse([cx - s, foot - height, cx + s, foot - height + 2 * s], fill=color)
    d.polygon([(cx - s * 1.4, foot - height + 2.3 * s), (cx + s * 1.4, foot - height + 2.3 * s),
               (cx + s * 1.8, foot - 2.6 * s), (cx - s * 1.8, foot - 2.6 * s)], fill=color)
    d.rectangle([cx - s * 1.2, foot - 2.8 * s, cx - s * 0.2, foot], fill=color)
    d.rectangle([cx + s * 0.2, foot - 2.8 * s, cx + s * 1.2, foot], fill=color)


# =====================================================================================================
# The Paintings
# =====================================================================================================

def p_riot_of_colours(w, h, frames=4):
    """A riot of vivid colour: swirls of every hue that will not sit still (animated), too much to look at for long."""
    out = []
    hues = [c('ff2a6a'), c('ffd21a'), c('1ae0ff'), c('7a2aff'), c('2aff6a'), c('ff7a1a'), c('ff1ad8'), c('ffffff')]
    for f in range(frames):
        rnd = random.Random(31 + f)
        img, d = board(w, h, c('1a0a2a'))
        W, H = w * UP, h * UP
        for k in range(26):                                                   # swirls
            cx, cy = rnd.uniform(0, W), rnd.uniform(0, H)
            r0 = rnd.uniform(H * 0.08, H * 0.4)
            turn = f * math.pi / 2 * (1 if k % 2 else -1) / 3
            pts = [(cx + math.cos(t * 0.35 + turn) * r0 * (1 - t / 40), cy + math.sin(t * 0.35 + turn) * r0 *
                    (1 - t / 40)) for t in range(0, 36)]
            brush(d, pts, hues[(k + f) % len(hues)], rnd.uniform(4, 10), rnd, 1.5)
        dabs(d, rnd, (0, 0, W, H), hues, 120, 7)                              # splatters
        for _ in range(14):
            x, y, r = rnd.uniform(0, W), rnd.uniform(0, H), rnd.uniform(3, 9)
            d.ellipse([x - r, y - r, x + r, y + r], fill=hues[rnd.randrange(len(hues))])
        out.append(canvas_grain(down(img, w, h), 10, f))
    return out


def p_itching_house(w, h):
    """A triangular house, grey-blue, between leaning trees, under rain that falls like mud."""
    rnd = random.Random(7)
    img, d = board(w, h, c('6a6a58'))
    W, H = w * UP, h * UP
    for y in range(0, H, 3):                                                  # a sour olive sky
        t = y / H
        d.line([(0, y), (W, y)], fill=(int(120 - 40 * t), int(118 - 30 * t), int(90 - 20 * t), 255), width=3)
    d.rectangle([0, H * 0.72, W, H], fill=c('4a3a24'))                         # muddy ground
    dabs(d, rnd, (0, H * 0.72, W, H), [c('5a4428'), c('3a2a18'), c('6a5030')], 60, 8)
    cx = W * 0.5
    d.polygon([(cx - W * 0.2, H * 0.78), (cx + W * 0.2, H * 0.78), (cx, H * 0.18)], fill=c('5a7a9a'))
    d.polygon([(cx - W * 0.2, H * 0.78), (cx, H * 0.18), (cx - W * 0.05, H * 0.78)], fill=c('4a6684'))
    d.line([(cx - W * 0.2, H * 0.78), (cx, H * 0.18), (cx + W * 0.2, H * 0.78)], fill=c('1a2a3a'), width=4)
    d.rectangle([cx - W * 0.03, H * 0.6, cx + W * 0.03, H * 0.78], fill=c('1a1a22'))        # a black door
    d.rectangle([cx - W * 0.1, H * 0.46, cx - W * 0.05, H * 0.54], fill=c('c8c070'))        # a sickly lit window
    for x, lean in ((W * 0.12, -1), (W * 0.86, 1), (W * 0.22, 1)):            # leaning bare trees
        top = (x + lean * W * 0.05, H * 0.2)
        brush(d, [(x, H * 0.8), (x + lean * W * 0.02, H * 0.5), top], c('2a2018'), 8, rnd)
        for k in range(4):
            y = H * (0.3 + k * 0.1)
            brush(d, [(x + lean * W * 0.02 * (1 - k / 4), y), (x + lean * W * 0.02 + rnd.uniform(-W * 0.08, W * 0.08),
                                                                y - H * 0.1)], c('2a2018'), 3, rnd)
    for _ in range(140):                                                      # rain like mud
        x, y = rnd.uniform(0, W), rnd.uniform(0, H * 0.8)
        d.line([(x, y), (x - 3, y + rnd.uniform(10, 22))], fill=c('5a4026', 220), width=2)
    return canvas_grain(down(img, w, h), 14)


def p_golden_sun(w, h):
    """A golden-red sun in a ring of colours, its rays painted in thick strokes."""
    rnd = random.Random(3)
    img, d = board(w, h, c('2a1a4a'))
    W, H = w * UP, h * UP
    cx, cy = W / 2, H / 2
    ring = [c('ff3a3a'), c('ff8a2a'), c('ffd23a'), c('7ad24a'), c('3aa8ff'), c('8a5aff')]
    for i, col in enumerate(reversed(ring)):                                   # the ring of colours
        r = H * 0.48 - i * H * 0.035
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col)
    d.ellipse([cx - H * 0.27, cy - H * 0.27, cx + H * 0.27, cy + H * 0.27], fill=c('2a1a4a'))
    for k in range(24):                                                       # rays
        a = math.tau * k / 24
        brush(d, [(cx + math.cos(a) * H * 0.14, cy + math.sin(a) * H * 0.14),
                  (cx + math.cos(a) * H * 0.26, cy + math.sin(a) * H * 0.26)],
              c('ffe07a') if k % 2 else c('ff9a3a'), 5, rnd)
    for r, col in ((H * 0.16, c('ff6a2a')), (H * 0.13, c('ffb03a')), (H * 0.08, c('fff0a0'))):
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col)
    dabs(d, rnd, (0, 0, W, H), [c('3a2a6a'), c('1a1030')], 40, 6)
    return canvas_grain(down(img, w, h), 12)


def p_boat_in_a_storm(w, h, frames=4):
    """A small boat on huge waves under a black storm, lightning on the horizon (animated: the waves roll)."""
    out = []
    for f in range(frames):
        rnd = random.Random(19)
        img, d = board(w, h, c('1a2230'))
        W, H = w * UP, h * UP
        for y in range(0, int(H * 0.55), 3):
            t = y / H
            d.line([(0, y), (W, y)], fill=(int(20 + 30 * t), int(26 + 40 * t), int(40 + 50 * t), 255), width=3)
        dabs(d, rnd, (0, 0, W, H * 0.45), [c('2a3446'), c('10141e'), c('3a4458')], 50, 12)
        if f == 1:                                                            # a flash on the horizon
            brush(d, [(W * 0.8, 0), (W * 0.76, H * 0.15), (W * 0.82, H * 0.22), (W * 0.78, H * 0.42)], c('f0f4ff'), 3)
        for layer in range(4):                                                # the waves, rolling with the frame
            base = H * (0.5 + layer * 0.13)
            ph = f * math.pi / 2 + layer
            pts = [(x, base + math.sin(x / W * math.tau * 1.5 + ph) * H * 0.08) for x in range(0, W + 6, 6)]
            col = [c('1e4a5a'), c('2a6a78'), c('1a3a4a'), c('0e2a36')][layer]
            d.polygon(pts + [(W, H), (0, H)], fill=col)
            d.line(pts, fill=c('d8f0f0'), width=3)                           # foam crests
        bx, by = W * 0.42, H * 0.52 + math.sin(f * math.pi / 2) * H * 0.04   # the boat, pitching
        d.polygon([(bx - W * 0.08, by), (bx + W * 0.08, by - H * 0.02), (bx + W * 0.05, by + H * 0.06),
                   (bx - W * 0.06, by + H * 0.06)], fill=c('4a2a14'))
        d.line([(bx, by), (bx + 2, by - H * 0.24)], fill=c('2a1a0a'), width=3)
        d.polygon([(bx + 3, by - H * 0.22), (bx + W * 0.07, by - H * 0.05), (bx + 3, by - H * 0.04)], fill=c('e8dcc0'))
        out.append(canvas_grain(down(img, w, h), 12, f))
    return out


def p_giants_fist(w, h):
    """A giant, painted from below, its fist raised over a tiny village: what becomes real when it is seen."""
    rnd = random.Random(5)
    img, d = board(w, h, c('c86a3a'))
    W, H = w * UP, h * UP
    for y in range(0, H, 3):                                                  # a burning orange sky
        t = y / H
        d.line([(0, y), (W, y)], fill=(int(230 - 60 * t), int(140 - 60 * t), int(60 - 20 * t), 255), width=3)
    cx = W * 0.55
    d.polygon([(cx - W * 0.16, H), (cx + W * 0.16, H), (cx + W * 0.1, H * 0.45), (cx - W * 0.1, H * 0.45)],
              fill=c('5a3020'))                                              # the giant's body, dark against the sky
    d.ellipse([cx - W * 0.07, H * 0.24, cx + W * 0.07, H * 0.48], fill=c('5a3020'))
    d.ellipse([cx - W * 0.03, H * 0.32, cx - W * 0.01, H * 0.35], fill=c('ffe07a'))
    d.ellipse([cx + W * 0.01, H * 0.32, cx + W * 0.03, H * 0.35], fill=c('ffe07a'))
    brush(d, [(cx - W * 0.12, H * 0.55), (cx - W * 0.26, H * 0.3), (cx - W * 0.28, H * 0.12)], c('5a3020'), 22, rnd)
    d.ellipse([cx - W * 0.36, H * 0.0, cx - W * 0.2, H * 0.2], fill=c('6a3a24'))                    # the fist
    for k in range(3):
        d.arc([cx - W * 0.35 + k * W * 0.045, H * 0.12, cx - W * 0.3 + k * W * 0.045, H * 0.2], 0, 180,
              fill=c('2a140a'), width=3)
    d.rectangle([0, H * 0.86, W, H], fill=c('3a2418'))                         # the village at its feet
    for k in range(8):
        x = rnd.uniform(0, W * 0.4) if k < 4 else rnd.uniform(W * 0.72, W)
        d.rectangle([x, H * 0.8, x + W * 0.03, H * 0.88], fill=c('2a1a10'))
        d.polygon([(x - 2, H * 0.8), (x + W * 0.015, H * 0.76), (x + W * 0.03 + 2, H * 0.8)], fill=c('2a1a10'))
    dabs(d, rnd, (0, 0, W, H * 0.4), [c('f0a050'), c('e07a3a')], 40, 8)
    return canvas_grain(down(img, w, h), 12)


def p_painting_world(w, h, frames=4):
    """A window into the pocket world: a black void for a sky, a pale ink horizon, a formless shimmer at its edge."""
    out = []
    for f in range(frames):
        rnd = random.Random(41)
        img, d = board(w, h, c('06040a'))
        W, H = w * UP, h * UP
        for _ in range(60):
            x, y = rnd.uniform(0, W), rnd.uniform(0, H * 0.6)
            a = int(80 + 120 * (0.5 + 0.5 * math.sin(f * math.pi / 2 + x)))
            d.point((x, y), fill=(220, 210, 255, a))
        pts = [(x, H * 0.66 + math.sin(x / W * 9) * H * 0.03) for x in range(0, W + 6, 6)]
        d.polygon(pts + [(W, H), (0, H)], fill=c('d8ccae'))
        d.line(pts, fill=c('14100e'), width=4)
        for k in range(6):                                                    # ink strokes of a world being drawn
            y = H * (0.74 + k * 0.04)
            brush(d, [(rnd.uniform(0, W * 0.4), y), (rnd.uniform(W * 0.6, W), y + rnd.uniform(-3, 3))],
                  c('3a3028', 140), 2, rnd)
        for x in range(0, W, 2):                                              # the barrier at the edges, shimmering
            e = min(x, W - x) / (W * 0.12)
            if e < 1:
                a = int(180 * (1 - e) * (0.6 + 0.4 * math.sin(f * math.pi / 2 + x * 0.3)))
                d.line([(x, 0), (x, H)], fill=(108, 92, 231, a), width=2)
        out.append(canvas_grain(down(img, w, h), 8, f))
    return out


def p_blank(w, h):
    """An unfinished canvas: primed cream, a few charcoal lines of a sketch. Shown while a Painting is being drawn."""
    rnd = random.Random(2)
    img, d = board(w, h, c('ece2c8'))
    W, H = w * UP, h * UP
    for _ in range(9):
        x, y = rnd.uniform(W * 0.1, W * 0.9), rnd.uniform(H * 0.1, H * 0.9)
        brush(d, [(x, y), (x + rnd.uniform(-W * 0.2, W * 0.2), y + rnd.uniform(-H * 0.2, H * 0.2))],
              c('6a6258', 120), 2, rnd)
    return canvas_grain(down(img, w, h), 10)


def p_self_portrait(w, h):
    """A portrait of the painter: a cloaked figure with one open eye, brushed in ink and parchment colours."""
    rnd = random.Random(13)
    img, d = board(w, h, c('3a3448'))
    W, H = w * UP, h * UP
    dabs(d, rnd, (0, 0, W, H), [c('463e56'), c('2e2a3a'), c('504868')], 80, 10)
    cx = W / 2
    d.polygon([(cx - W * 0.42, H), (cx + W * 0.42, H), (cx + W * 0.26, H * 0.5), (cx - W * 0.26, H * 0.5)],
              fill=c('14100e'))                                              # shoulders and cloak
    d.ellipse([cx - W * 0.22, H * 0.18, cx + W * 0.22, H * 0.56], fill=c('d8c8a0'))           # the face
    d.chord([cx - W * 0.28, H * 0.1, cx + W * 0.28, H * 0.62], 180, 360, fill=c('14100e'))     # the hood
    d.ellipse([cx - W * 0.12, H * 0.34, cx - W * 0.02, H * 0.39], fill=c('ffffff'))
    d.ellipse([cx - W * 0.09, H * 0.345, cx - W * 0.05, H * 0.385], fill=c('6c5ce7'))           # the one open eye
    d.line([(cx + W * 0.03, H * 0.37), (cx + W * 0.12, H * 0.37)], fill=c('14100e'), width=3)  # the other, shut
    d.line([(cx - W * 0.05, H * 0.48), (cx + W * 0.05, H * 0.48)], fill=c('8a5a40'), width=2)
    for k in range(6):                                                         # gold lines of the brush on the cloak
        brush(d, [(cx - W * 0.3 + k * W * 0.1, H * 0.98), (cx - W * 0.2 + k * W * 0.08, H * 0.66)], c('c8a050', 150),
              2, rnd)
    return canvas_grain(down(img, w, h), 12)


def _scene_with_door(w, h, rnd):
    img, d = board(w, h, c('8aa8b8'))
    W, H = w * UP, h * UP
    for y in range(0, H, 3):
        t = y / H
        d.line([(0, y), (W, y)], fill=(int(150 - 40 * t), int(178 - 40 * t), int(196 - 50 * t), 255), width=3)
    d.rectangle([0, H * 0.7, W, H], fill=c('6a7a4a'))
    dabs(d, rnd, (0, H * 0.7, W, H), [c('5a6a3a'), c('7a8a5a')], 50, 8)
    d.rectangle([W * 0.28, H * 0.2, W * 0.72, H * 0.72], fill=c('a89878'))      # a wall with an arch
    d.chord([W * 0.36, H * 0.28, W * 0.64, H * 0.56], 180, 360, fill=c('3a3028'))
    d.rectangle([W * 0.36, H * 0.42, W * 0.64, H * 0.72], fill=c('3a3028'))
    return img, d, W, H


def p_hide_in_painting(w, h):
    """A quiet scene with a doorway, and a human figure standing in it: someone hiding inside the picture."""
    rnd = random.Random(17)
    img, d, W, H = _scene_with_door(w, h, rnd)
    figure(d, W * 0.5, H * 0.72, H * 0.24, c('14100e'))
    d.point((W * 0.5 - 2, H * 0.72 - H * 0.24 + 6), fill=c('6c5ce7'))
    return canvas_grain(down(img, w, h), 12)


def p_hide_in_painting_empty(w, h):
    """The same scene with the doorway empty: they have stepped out."""
    rnd = random.Random(17)
    img, d, W, H = _scene_with_door(w, h, rnd)
    return canvas_grain(down(img, w, h), 12)


def p_fantasy(w, h):
    """Fantasy Double's canvas: two figures, one solid and one only outlined, a thread between their hands."""
    rnd = random.Random(23)
    img, d = board(w, h, c('2a2440'))
    W, H = w * UP, h * UP
    dabs(d, rnd, (0, 0, W, H), [c('3a3458'), c('1e1a30'), c('6c5ce7', 120)], 70, 10)
    figure(d, W * 0.32, H * 0.86, H * 0.5, c('e8dcc0'))
    fig = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    figure(ImageDraw.Draw(fig), W * 0.68, H * 0.86, H * 0.5, c('e8dcc0'))
    edge = fig.filter(ImageFilter.FIND_EDGES)
    img.alpha_composite(edge)
    brush(d, [(W * 0.4, H * 0.55), (W * 0.5, H * 0.5), (W * 0.6, H * 0.55)], c('ffd880'), 2, rnd)
    return canvas_grain(down(img, w, h), 10)


SIZES = {'wide': (96, 64), 'tall': (64, 96)}

# name, painter, shape, frametime (animated ones only)
PAINTINGS = [
    ('riot_of_colours', p_riot_of_colours, 'wide', 4),
    ('itching_house', p_itching_house, 'wide', 0),
    ('golden_sun', p_golden_sun, 'wide', 0),
    ('boat_in_a_storm', p_boat_in_a_storm, 'wide', 6),
    ('giants_fist', p_giants_fist, 'wide', 0),
    ('painting_world', p_painting_world, 'wide', 5),
    ('blank', p_blank, 'wide', 0),
    ('self_portrait', p_self_portrait, 'tall', 0),
    ('hide_in_painting', p_hide_in_painting, 'tall', 0),
    ('hide_in_painting_empty', p_hide_in_painting_empty, 'tall', 0),
    ('fantasy', p_fantasy, 'tall', 0),
]


# =====================================================================================================
# The frame
# =====================================================================================================

def frame_model(shape):
    """The frame and the back of the canvas as a Model (texture "0"); the front face is added by write_canvas."""
    w, h = (24.0, 16.0) if shape == 'wide' else (16.0, 24.0)
    x0, y0 = 8 - w / 2, 8 - h / 2
    x1, y1 = x0 + w, y0 + h
    m = models.Model('canvas_' + shape, '', FRAME_MATS)
    m.box('back', [x0 + 0.6, y0 + 0.6, 8.2], [x1 - 0.6, y1 - 0.6, 8.6], 'canvas_back', skip=('north',))
    t, z0, z1 = 1.4, 7.2, 8.8
    m.box('top', [x0, y1 - t, z0], [x1, y1, z1], 'frame_wood')
    m.box('bottom', [x0, y0, z0], [x1, y0 + t, z1], 'frame_wood')
    m.box('left', [x0, y0 + t, z0], [x0 + t, y1 - t, z1], 'frame_wood')
    m.box('right', [x1 - t, y0 + t, z0], [x1, y1 - t, z1], 'frame_wood')
    for i, (x, y) in enumerate(((x0, y0), (x1 - 2.2, y0), (x0, y1 - 2.2), (x1 - 2.2, y1 - 2.2))):
        m.box(f'corner_{i}', [x - 0.2, y - 0.2, z0 - 0.3], [x + 2.4, y + 2.4, z0 + 0.5], 'frame_gold')
    m.box('lip', [x0 + t, y1 - t - 0.4, z0 + 0.2], [x1 - t, y1 - t, z0 + 0.4], 'frame_gold')
    return m, (x0 + t - 0.2, y0 + t - 0.2, x1 - t + 0.2, y1 - t + 0.2)


def write_canvas(assets, name, shape, frames, frametime, cells, frame_tex, tex_w, tex_h):
    m, (fx0, fy0, fx1, fy1) = frame_model(shape)
    pw, ph = SIZES[shape]
    pic_path = os.path.join(assets, 'textures/model_fx', FOLDER, 'paintings', name + '.png')
    os.makedirs(os.path.dirname(pic_path), exist_ok=True)
    if isinstance(frames, list) and len(frames) > 1:
        sheet = Image.new('RGBA', (pw, ph * len(frames)))
        for i, fr in enumerate(frames):
            sheet.paste(fr, (0, i * ph))
        sheet.save(pic_path)
        with open(pic_path + '.mcmeta', 'w') as f:
            json.dump({'animation': {'frametime': frametime, 'interpolate': True, 'width': pw, 'height': ph}}, f)
    else:
        (frames[0] if isinstance(frames, list) else frames).save(pic_path)
        if os.path.exists(pic_path + '.mcmeta'):
            os.remove(pic_path + '.mcmeta')
    pic_id = f'{NAMESPACE}:model_fx/{FOLDER}/paintings/{name}'
    elements = m.json(cells, tex_w, tex_h)
    elements.insert(0, {'name': 'painting', 'from': [fx0, fy0, 8.0], 'to': [fx1, fy1, 8.2], 'shade': False,
                        'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#1'}}})
    size = 'wide: 1.5 x 1 block' if shape == 'wide' else 'tall: 1 x 1.5 block'
    models.save_json(assets, f'{FOLDER}/canvas_{name}', {
        'credit': f'{models.CREDIT}A framed canvas ({size} at scale 1, the plan\'s 3 x 2 canvas at scale 2) showing '
                  f'the Painting "{name}" (textures/model_fx/observer/paintings/{name}.png). Lacquered black frame, '
                  f'gilt corners, raw linen at the back. Front = north, pivot 8,8,8 = the middle of the canvas. '
                  f'.glow(){models.MADE_BY}',
        'texture_size': [tex_w, tex_h],
        'textures': {'0': frame_tex, '1': pic_id, 'particle': frame_tex},
        'elements': elements,
    })
    return f'{FOLDER}/canvas_{name}'


def main(assets):
    img, cells = atlas(FRAME_MATS, 81)
    tex_w, tex_h = img.size
    path = os.path.join(assets, 'textures/model_fx', FOLDER, 'canvas_frame.png')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    frame_tex = f'{NAMESPACE}:model_fx/{FOLDER}/canvas_frame'
    written = []
    for name, painter, shape, frametime in PAINTINGS:
        pw, ph = SIZES[shape]
        written.append(write_canvas(assets, name, shape, painter(pw, ph), frametime, cells, frame_tex, tex_w, tex_h))
    return written


if __name__ == '__main__':
    for w in main(sys.argv[1]):
        print(w)
