"""
The Demoness (Assassin) models: models/model_fx/demoness/*.json and textures/model_fx/demoness/*.png.

    python3 tools/model_fx/demoness_models.py            write them all (and previews into build/model_fx_preview)
    python3 tools/model_fx/demoness_models.py web cocoon only those

How each is played is in DemonessFx.java; what each one is, in docs/MODEL_FX.md (Ready-made models).
"""

import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(__file__))
from fxgen import (Box, Model, ROOT, fbm, looped, mix, noise, octagon, plate, ramp, rgb, smooth, supersample)

PREVIEW = os.path.join(ROOT, "build", "model_fx_preview")

# The pathway's colours
BLACK = 0x07040A
VIOLET_DEEP = 0x2A0F3A
VIOLET = 0x7A2CB8
VIOLET_GLOW = 0xD27BFF
SILVER = 0xD8D4E8
CRIMSON = 0x9A1028
BLOOD = 0x4A0612


# =====================================================================================
# Materials
# =====================================================================================

def dark_silver(ctx):
    """Blackened silver: the frames of the mirrors, the dagger's guard."""
    n = fbm(ctx.x * 0.35, ctx.y * 0.35, ctx.z * 0.35, 3)
    col = mix(rgb(0x231D2C), rgb(0x4A4258), n)
    groove = (np.abs(np.sin((ctx.x + ctx.y + ctx.z) * 1.7)) > 0.92)
    col = np.where(groove[..., None], col * 0.6, col)
    edge = ctx.edge()
    col = mix(col, rgb(0xA49AB8), smooth(0.7, 0.0, edge) * 0.8)
    return col, 1.0


def violet_gem(ctx):
    n = fbm(ctx.x * 0.8, ctx.y * 0.8, ctx.z * 0.8, 5)
    col = ramp(n + ctx.t * 0.25, [(0.2, 0x3A0A5A), (0.6, 0x9A3AE0), (0.95, 0xF2C8FF)])
    hi = (ctx.s < 0.35) & (ctx.t < 0.35)
    col = np.where(hi[..., None], rgb(0xFFF0FF), col)
    return col, 1.0


def mirror_glass(ctx):
    """The face of a mirror: silver-lavender, a soft violet haze, diagonal sheen."""
    if not ctx.face in ("north", "south"):
        return rgb(0x6A6080) + 0 * ctx.s[..., None], 1.0
    height = (ctx.y - 2) / 28.0
    base = mix(rgb(0x4A3E68), rgb(0xD8D2EE), smooth(-0.1, 1.0, height))
    haze = fbm(ctx.x * 0.18, ctx.y * 0.18, 4.0, 11)
    base = mix(base, rgb(0x6B3A9A), smooth(0.45, 0.8, haze) * 0.55)
    streak = np.sin((ctx.x * 0.9 + ctx.y) * 0.55)
    base = base + rgb(0xFFFFFF) * (smooth(0.86, 1.0, streak) * 0.35)[..., None]
    base = mix(base, rgb(0xFFFFFF), smooth(0.6, 0.0, ctx.edge()) * 0.3)
    return base, 1.0


def dark_mirror(ctx):
    """A shard of the dark mirror: black glass with silver cracks and a violet sheen."""
    n = fbm(ctx.x * 0.25, ctx.y * 0.25, ctx.z * 0.25, 21)
    col = mix(rgb(0x120A1A), rgb(0x3A2A52), n)
    streak = np.sin((ctx.x * 0.8 + ctx.y) * 0.7 + ctx.z)
    col = col + rgb(0xC8B8F0) * (smooth(0.9, 1.0, streak) * 0.45)[..., None]
    crack = np.abs(fbm(ctx.x * 0.18, ctx.y * 0.18, ctx.z * 0.18, 44) - 0.5) < 0.012
    col = np.where(crack[..., None], rgb(0xE8E0FF), col)
    col = mix(col, rgb(0xB8A8D8), smooth(0.5, 0.0, ctx.edge()) * 0.7)
    return col, 1.0


def shard_glow(ctx):
    """The shell over a charging shard: glowing cracks and edges, see-through between them."""
    crack = np.abs(fbm(ctx.x * 0.18, ctx.y * 0.18, ctx.z * 0.18, 44) - 0.5)
    lines = smooth(0.03, 0.0, crack)
    edge = smooth(0.9, 0.0, ctx.edge())
    a = np.maximum(lines, edge * 0.85)
    a = np.maximum(a, 0.04 + 0.08 * fbm(ctx.x * 0.4, ctx.y * 0.4, ctx.z * 0.4, 9))
    col = mix(rgb(0xB040FF), rgb(0xFFD8FF), np.maximum(lines, edge) ** 2)
    return col, a


def glass(ctx):
    """A sliver of a shattered mirror: bright, a little see-through."""
    n = fbm(ctx.x * 0.6, ctx.y * 0.6, ctx.z * 0.6, 31)
    col = mix(rgb(0x9A90C0), rgb(0xF4F0FF), n)
    col = mix(col, rgb(0xFFFFFF), smooth(0.45, 0.0, ctx.edge()))
    return col, 0.92


def blade(ctx):
    """Black steel with a violet edge burning along both sides."""
    n = fbm(ctx.x * 0.5, ctx.y * 0.2, ctx.z * 0.5, 7)
    col = mix(rgb(0x0E0C12), rgb(0x2C2836), n)
    if ctx.face in ("north", "south"):
        across = np.minimum(ctx.s, 1 - ctx.s) * ctx.fw            # pixels from the blade's side edge
        col = mix(col, rgb(0x050407), smooth(0.25, 0.0, np.abs(ctx.s - 0.5)) * 0.8)   # the fuller
        col = mix(col, rgb(VIOLET_GLOW), smooth(0.45, 0.05, across))
    else:
        col = mix(col, rgb(VIOLET_GLOW), 0.85)
    return col, 1.0


def grip(ctx):
    wrap = (np.sin((ctx.y * 2.2 + (ctx.x + ctx.z) * 0.9)) > 0.3)
    col = np.where(wrap[..., None], rgb(0x1A0F22), rgb(0x3E2050))
    return col * (0.85 + 0.3 * fbm(ctx.x, ctx.y, ctx.z, 2))[..., None], 1.0


def black_fire(ctx):
    """
    Black flame, animated. Layer 0 is the core (almost black, violet at the rim), layers 1 and 2 are see-through
    shells of violet tongues. The pattern rises and loops.
    """
    layer = ctx.p.get("layer", 0)
    cx, cy, cz = 8.0, ctx.p.get("cy", 8.0), 8.0
    r = np.sqrt((ctx.x - cx) ** 2 + (ctx.y - cy) ** 2 + (ctx.z - cz) ** 2) / ctx.p.get("r", 6.0)

    def field(off):
        return fbm(ctx.x * 0.28, ctx.y * 0.28 - off, ctx.z * 0.28, 60 + layer)

    v = looped(field, ctx.time, 3.0)
    heat = (1.2 - r) + (v - 0.5) * 1.3 - layer * 0.18
    col = ramp(heat, [(0.0, 0x5A1A90), (0.15, 0xB050FF), (0.3, 0x2A0A40), (0.45, 0x0A030F), (0.7, 0x020103)])
    if layer == 0:
        return col, 1.0
    a = smooth(0.05, 0.35, heat) * (0.95 if layer == 1 else 0.75)
    return col, a


def black_fire_pillar(ctx):
    """Black flames rising up a pillar, animated: tongues that thin out toward the top."""
    layer = ctx.p["layer"]
    up = ctx.y / 32.0

    def field(off):
        return fbm(ctx.x * 0.22 + ctx.z * 0.22, ctx.y * 0.16 - off, layer * 3.1, 80 + layer)

    v = looped(field, ctx.time, 4.0)
    heat = v * 1.25 - up * (0.85 + layer * 0.15) + 0.35 - layer * 0.12
    col = ramp(heat, [(0.0, 0x5A1A90), (0.12, 0xB050FF), (0.26, 0x2A0A40), (0.4, 0x0A030F), (0.7, 0x020103)])
    a = smooth(0.0, 0.18, heat)
    return col, a * (1.0, 0.9, 0.7)[layer]


def silk(ctx):
    """Spider silk wound round and round: off-white strands with a faint violet sheen."""
    angle = np.arctan2(ctx.z - 8, ctx.x - 8)
    strands = np.sin(ctx.y * 2.4 + angle * 6.0 + fbm(ctx.x * 0.3, ctx.y * 0.3, ctx.z * 0.3, 5) * 6)
    cross = np.sin(ctx.y * 1.3 - angle * 4.0)
    col = mix(rgb(0xBDB4CC), rgb(0xF6F2FF), smooth(-0.2, 0.9, strands))
    col = mix(col, rgb(0x9A8AB8), smooth(0.85, 1.0, cross) * 0.5)
    shade = 0.82 + 0.18 * fbm(ctx.x * 0.6, ctx.y * 0.6, ctx.z * 0.6, 17)
    return col * shade[..., None], 1.0


def rose_petal(ctx):
    n = fbm(ctx.x * 0.7, ctx.y * 0.7, ctx.z * 0.7, 13)
    col = mix(rgb(0x14030A), rgb(0x3A0614), n)
    vein = np.abs(np.sin((ctx.s - 0.5) * 9.0)) < 0.12
    col = np.where(vein[..., None], col * 0.6, col)
    col = mix(col, rgb(0xC0183A), smooth(0.7, 0.0, ctx.edge()) * 0.9)
    return col, 1.0


def rose_core(ctx):
    n = fbm(ctx.x * 0.9, ctx.y * 0.9, ctx.z * 0.9, 14)
    return mix(rgb(0x0A0206), rgb(0x6A0A20), n), 1.0


def stem(ctx):
    n = fbm(ctx.x * 0.8, ctx.y * 0.5, ctx.z * 0.8, 15)
    return mix(rgb(0x0C140C), rgb(0x24301C), n), 1.0


def thorn(ctx):
    return mix(rgb(0x2A0A10), rgb(0x8A1A2A), ctx.t), 1.0


def leaf(ctx):
    n = fbm(ctx.x * 0.8, ctx.y * 0.8, ctx.z * 0.8, 16)
    col = mix(rgb(0x0E1A10), rgb(0x2A3A20), n)
    vein = np.abs(ctx.s - 0.5) < 0.06
    return np.where(vein[..., None], rgb(0x3A1018), col), 1.0


def stone(ctx):
    n = fbm(ctx.x * 0.45, ctx.y * 0.45, ctx.z * 0.45, 23)
    col = mix(rgb(0x2E2622), rgb(0x6A5C52), n)
    crack = np.abs(fbm(ctx.x * 0.5, ctx.y * 0.5, ctx.z * 0.5, 24) - 0.5) < 0.03
    col = np.where(crack[..., None], rgb(0xC8301A), col)            # it glows red where it split
    return col * (0.8 + 0.25 * noise(ctx.x * 2, ctx.y * 2, ctx.z * 2, 25))[..., None], 1.0


def ice(ctx):
    n = fbm(ctx.x * 0.5, ctx.y * 0.5, ctx.z * 0.5, 33)
    col = mix(rgb(0x8EB4E0), rgb(0xF0FAFF), n)
    streak = np.abs(np.sin((ctx.x - ctx.y + ctx.z) * 0.9)) > 0.93
    col = np.where(streak[..., None], rgb(0xFFFFFF), col)
    col = mix(col, rgb(0xFFFFFF), smooth(0.5, 0.0, ctx.edge()) * 0.6)
    return col, 0.88


def storm(ctx):
    """The tornado: grey-violet wind streaks racing round, see-through between them. Animated."""
    angle = np.arctan2(ctx.z - 8, ctx.x - 8)

    def field(off):
        return fbm(angle * 2.2 + off, ctx.y * 0.35 - angle * 0.6, 1.7, 90)

    v = looped(field, ctx.time, 6.0)
    streak = smooth(0.42, 0.75, v)
    col = mix(rgb(0x2A2430), rgb(0xB8B0C8), streak)
    col = mix(col, rgb(0x5A2A7A), smooth(0.6, 0.35, v) * 0.4)
    a = 0.18 + streak * 0.75
    a = a * smooth(0.0, 3.0, ctx.y + 1.5)                           # thins out at the very bottom
    return col, a


def plague_smoke(ctx):
    n = fbm(ctx.x * 0.22, ctx.y * 0.22, ctx.z * 0.22, 40)
    m = fbm(ctx.x * 0.5, ctx.y * 0.5, ctx.z * 0.5, 41)
    col = ramp(n, [(0.2, 0x0C1006), (0.5, 0x2E3A10), (0.75, 0x6A8A1E), (0.95, 0xB8E04A)])
    a = smooth(0.25, 0.65, n) * 0.8 + 0.1 * m
    return col, a


def bolt(ctx):
    """Lightning: white-hot core, crimson glow around it."""
    if ctx.p.get("core"):
        return rgb(0xFFF4F8) + 0 * ctx.s[..., None], 1.0
    across = np.minimum(ctx.s, 1 - ctx.s) * 2
    return mix(rgb(0xFF1A3A), rgb(0xFF8090), across), 0.25 + 0.35 * across


# =====================================================================================
# Pictures for the plates
# =====================================================================================

def web_picture(size=128, spokes=14, rings=11, seed=4, dew=True):
    rnd = random.Random(seed)

    def draw(d, k, img):
        c = size * k / 2
        R = size * k * 0.48
        angles = sorted((i + rnd.uniform(-0.25, 0.25)) * 2 * math.pi / spokes for i in range(spokes))
        line = (240, 236, 255, 235)
        for a in angles:
            d.line([(c, c), (c + math.cos(a) * R, c + math.sin(a) * R)], fill=line, width=int(1.1 * k))
        for r in range(1, rings + 1):
            rr = R * (r / rings) ** 0.9
            pts = []
            for a in angles + [angles[0] + 2 * math.pi]:
                wob = rr * rnd.uniform(0.96, 1.03)
                pts.append((c + math.cos(a) * wob, c + math.sin(a) * wob))
            for p0, p1 in zip(pts, pts[1:]):
                # a little sag toward the middle between two spokes
                mx, my = (p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2
                mx, my = c + (mx - c) * 0.96, c + (my - c) * 0.96
                d.line([p0, (mx, my), p1], fill=line, width=max(1, int(0.8 * k)))
        hub = R * 0.07
        d.ellipse([c - hub, c - hub, c + hub, c + hub], fill=(250, 248, 255, 240))
        if dew:
            for _ in range(28):
                a = rnd.choice(angles)
                rr = rnd.uniform(0.15, 0.95) * R
                x, y, s = c + math.cos(a) * rr, c + math.sin(a) * rr, rnd.uniform(0.6, 1.2) * k
                d.ellipse([x - s, y - s, x + s, y + s], fill=(255, 255, 255, 255))

    return supersample(draw, size)


def crack_picture(size, seed, crack, core, haze, arms=7, reach=0.47, branch=0.35):
    """Cracks running out from the middle; 'crack' colour, a thinner bright 'core' in them, a blurred 'haze'."""
    rnd = random.Random(seed)
    k = 4
    S = size * k
    lines = []

    def walk(x, y, a, length, width):
        while length > 0 and width > 0.3:
            step = rnd.uniform(3, 7) * k
            a += rnd.uniform(-0.45, 0.45)
            nx, ny = x + math.cos(a) * step, y + math.sin(a) * step
            if math.hypot(nx - S / 2, ny - S / 2) > S * reach:
                break
            lines.append(((x, y), (nx, ny), width))
            if rnd.random() < branch * 0.25:
                walk(nx, ny, a + rnd.choice((-1, 1)) * rnd.uniform(0.5, 1.1), length * 0.5, width * 0.6)
            x, y = nx, ny
            length -= step
            width *= 0.97

    for i in range(arms):
        walk(S / 2, S / 2, i * 2 * math.pi / arms + rnd.uniform(-0.3, 0.3), S * 0.6, 3.2)

    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow, "RGBA")
    for p0, p1, w in lines:
        gd.line([p0, p1], fill=haze, width=int(w * k * 3.2))
    glow = glow.filter(ImageFilter.GaussianBlur(3 * k))
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    img.alpha_composite(glow)
    d = ImageDraw.Draw(img, "RGBA")
    for p0, p1, w in lines:
        d.line([p0, p1], fill=crack, width=max(1, int(w * k * 1.2)))
    for p0, p1, w in lines:
        if w > 0.8:
            d.line([p0, p1], fill=core, width=max(1, int(w * k * 0.45)))
    return img.resize((size, size), Image.LANCZOS)


def plague_sigil_picture(size=96):
    def draw(d, k, img):
        c = size * k / 2
        green, dark = (156, 255, 74, 255), (20, 40, 8, 230)
        R = size * k * 0.46
        for rr, w in ((R, 2.2), (R * 0.86, 1.2)):
            d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=green, width=int(w * k))
        for i in range(36):                                           # notches between the two rings
            a = i * math.pi / 18
            r0, r1 = R * 0.88, R * (0.97 if i % 3 == 0 else 0.93)
            d.line([(c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a) * r1, c + math.sin(a) * r1)],
                   fill=green, width=int(1.2 * k))
        # a six-petalled flower of rot: r = 0.62R |cos 3t|
        pts = []
        for i in range(721):
            t = i * math.pi / 360
            r = R * 0.68 * abs(math.cos(3 * t))
            pts.append((c + math.cos(t) * r, c + math.sin(t) * r))
        d.polygon(pts, fill=(110, 200, 40, 150), outline=green)
        d.line(pts, fill=dark, width=int(0.8 * k))
        for i in range(6):                                            # a spore at the tip of each petal
            a = i * math.pi / 3
            x, y, s = c + math.cos(a) * R * 0.72, c + math.sin(a) * R * 0.72, 2.6 * k
            d.ellipse([x - s, y - s, x + s, y + s], fill=green)
        s = R * 0.13
        d.ellipse([c - s, c - s, c + s, c + s], fill=(12, 20, 4, 255), outline=green, width=int(1.4 * k))

    return supersample(draw, size)


def curse_sigil_picture(size=96):
    def draw(d, k, img):
        c = size * k / 2
        red, glow = (255, 40, 70, 255), (120, 0, 20, 200)
        R = size * k * 0.46
        thorns = []
        for i in range(48):                                           # a ring of thorns
            a = i * math.pi / 24
            r = R * (1.0 if i % 2 == 0 else 0.84)
            thorns.append((c + math.cos(a) * r, c + math.sin(a) * r))
        thorns.append(thorns[0])
        d.line(thorns, fill=red, width=int(1.4 * k))
        rr = R * 0.78
        d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=red, width=int(1.0 * k))
        # an eye: two arcs, an iris and a slit
        w, h = R * 0.62, R * 0.34
        top = [(c + x, c - h * (1 - (x / w) ** 2)) for x in np.linspace(-w, w, 60)]
        bottom = [(c + x, c + h * (1 - (x / w) ** 2)) for x in np.linspace(w, -w, 60)]
        d.polygon(top + bottom, fill=glow, outline=red)
        d.line(top + bottom + [top[0]], fill=red, width=int(1.4 * k))
        ir = h * 0.85
        d.ellipse([c - ir, c - ir, c + ir, c + ir], fill=(255, 70, 90, 255))
        d.ellipse([c - ir * 0.25, c - ir * 0.85, c + ir * 0.25, c + ir * 0.85], fill=(10, 0, 4, 255))

    return supersample(draw, size)


def sky_picture(size=128):
    y, x = np.mgrid[0:size, 0:size] / size
    r = np.sqrt((x - 0.5) ** 2 + (y - 0.5) ** 2) * 2
    angle = np.arctan2(y - 0.5, x - 0.5)
    swirl = fbm(x * 5 + np.cos(angle * 2 + r * 6) * 0.6, y * 5 + np.sin(angle * 2 + r * 6) * 0.6, 0.5, 70)
    col = ramp(swirl + (1 - r) * 0.15, [(0.2, 0x050102), (0.45, 0x3A0408), (0.65, 0x8A0E14), (0.85, 0xE0502A)])
    a = smooth(1.0, 0.55, r) * (0.55 + 0.45 * swirl)
    arr = np.concatenate([col, a[..., None]], axis=2)
    return Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8), "RGBA")


def frost_picture(size=128):
    img = crack_picture(size, 9, (220, 240, 255, 255), (255, 255, 255, 255), (150, 200, 255, 110), arms=10, branch=0.9)
    return img


# =====================================================================================
# The models
# =====================================================================================

def mirror():
    """A tall mirror standing on the ground: dark silver frame with stepped shoulders and a violet gem."""
    b = []
    zf, zb = 6.5, 9.5                       # frame depth
    zg0, zg1 = 7.5, 8.5                     # glass
    # glass, stepping in toward the top
    b += [Box((-2, 2, zg0), (18, 25, zg1), mirror_glass, name="glass"),
          Box((-1, 25, zg0), (17, 27, zg1), mirror_glass, name="glass_step1"),
          Box((1, 27, zg0), (15, 29, zg1), mirror_glass, name="glass_step2"),
          Box((4, 29, zg0), (12, 30, zg1), mirror_glass, name="glass_step3")]
    # frame
    b += [Box((-4.5, 0, zf - 0.5), (20.5, 2, zb + 0.5), dark_silver, name="foot"),
          Box((-4, 2, zf), (-2, 25, zb), dark_silver, name="post_left"),
          Box((18, 2, zf), (20, 25, zb), dark_silver, name="post_right"),
          Box((-3, 25, zf), (-1, 27, zb), dark_silver, name="shoulder_l1"),
          Box((17, 25, zf), (19, 27, zb), dark_silver, name="shoulder_r1"),
          Box((-1, 27, zf), (1, 29, zb), dark_silver, name="shoulder_l2"),
          Box((15, 27, zf), (17, 29, zb), dark_silver, name="shoulder_r2"),
          Box((1, 29, zf), (4, 31, zb), dark_silver, name="shoulder_l3"),
          Box((12, 29, zf), (15, 31, zb), dark_silver, name="shoulder_r3"),
          Box((4, 30, zf), (12, 32, zb), dark_silver, name="crest"),
          Box((6.5, 29.5, zf - 1), (9.5, 32, zf), violet_gem, name="gem"),
          Box((-5, 0, 5), (-1, 1, 11), dark_silver, name="claw_left"),
          Box((17, 0, 5), (21, 1, 11), dark_silver, name="claw_right")]
    return Model("demoness/mirror", b, res=2, credit="a tall standing mirror, frame of blackened silver, 1.6 blocks wide and 2 tall. Front = north; both faces are glass. Pivot 8,0,8 = the middle of its foot.")


SHARD_CUBES = [
    ((3, -3, 7.4), (13, 10, 8.6), None, "base"),
    ((4, 9, 7.5), (12, 17, 8.5), ("z", 22.5, (8, 9, 8)), "mid"),
    ((6, 15, 7.55), (11, 23, 8.45), ("z", -22.5, (8, 15, 8)), "upper"),
    ((7, 21, 7.6), (9.5, 29, 8.4), ("z", 22.5, (8, 21, 8)), "tip"),
    ((12, -3, 7.0), (16, 7, 8.0), ("z", -22.5, (12, -3, 8)), "splinter_right"),
    ((0, -3, 8.0), (4, 5, 9.0), ("z", 22.5, (4, -3, 8)), "splinter_left"),
    ((6, -3, 4.5), (9, 6, 5.4), ("x", -22.5, (8, -3, 5)), "splinter_front"),
]


def mirror_shard():
    b = [Box(f, t, dark_mirror, rot=r, name=n) for f, t, r, n in SHARD_CUBES]
    return Model("demoness/mirror_shard", b, res=3, credit="a jagged shard of dark mirror stuck in the ground, 1.8 blocks tall. Pivot 8,0,8 = where it meets the ground (3 px of it are under it).")


def mirror_shard_glow():
    """The same shard a little bigger, painted only with glowing cracks: played over it while it charges."""
    b = []
    for f, t, r, n in SHARD_CUBES:
        f2 = tuple(v - 0.35 for v in f)
        t2 = tuple(v + 0.35 for v in t)
        b.append(Box(f2, t2, shard_glow, rot=r, name=n))
    return Model("demoness/mirror_shard_glow", b, res=3, preview_unshaded=True,
                 credit="the glowing cracks of mirror_shard, a hair bigger than it. .glow().unshaded().seeThrough(), same pivot.")


def glass_shard():
    b = [Box((6, 0, 7.8), (10, 7, 8.2), glass, name="body"),
         Box((7, 6, 7.85), (9.5, 11, 8.15), glass, rot=("z", 22.5, (8, 6, 8)), name="point"),
         Box((5, 0, 7.85), (7, 3, 8.15), glass, rot=("z", -22.5, (7, 0, 8)), name="chip")]
    return Model("demoness/glass_shard", b, res=4, credit="one sliver of a broken mirror, 0.7 blocks long, tip up. Pivot 8,5,8 = its middle.")


def dagger():
    b = [Box((7, -6, 7), (9, -4, 9), dark_silver, name="pommel"),
         Box((7.25, -4, 7.25), (8.75, 3, 8.75), grip, name="grip"),
         Box((4, 3, 7), (12, 4.5, 9), dark_silver, name="guard"),
         Box((3, 3.5, 7.25), (4, 6, 8.75), dark_silver, rot=("z", -22.5, (4, 3.5, 8)), name="quillon_left"),
         Box((12, 3.5, 7.25), (13, 6, 8.75), dark_silver, rot=("z", 22.5, (12, 3.5, 8)), name="quillon_right"),
         Box((7.25, 3.25, 6.5), (8.75, 4.75, 7), violet_gem, name="gem"),
         Box((6.5, 4.5, 7.6), (9.5, 13, 8.4), blade, name="blade"),
         Box((7, 13, 7.65), (9, 18, 8.35), blade, name="blade_upper"),
         Box((7.4, 17.5, 7.7), (8.6, 22, 8.3), blade, rot=("z", -22.5, (8, 17.5, 8)), name="blade_tip")]
    return Model("demoness/dagger", b, res=4, credit="the Assassin's dagger: black steel with a violet edge, 1.75 blocks long, tip up. Pivot 8,0,8 = the grip.")


def black_flame():
    b = [Box((5, 5, 5), (11, 11, 11), black_fire, name="core", layer=0),
         Box((3.5, 3.5, 3.5), (12.5, 13, 12.5), black_fire, name="shell", layer=1),
         Box((2, 2, 2), (14, 15.5, 14), black_fire, name="wisps", layer=2)]
    return Model("demoness/black_flame", b, res=2, anim=8, frametime=2, preview_unshaded=True,
                 credit="a ball of black flame, animated, 0.9 blocks. A core with see-through shells. Pivot 8,8,8. .glow().unshaded().seeThrough()")


def black_flame_pillar():
    sides = ("north", "south", "east", "west")
    b = [Box((5, 0, 5), (11, 32, 11), black_fire_pillar, faces=sides, name="inner", layer=0),
         Box((3.5, 0, 3.5), (12.5, 29, 12.5), black_fire_pillar, faces=sides, name="middle", layer=1),
         Box((2, 0, 2), (14, 25, 14), black_fire_pillar, faces=sides, name="outer", layer=2)]
    return Model("demoness/black_flame_pillar", b, res=1, anim=8, frametime=2, preview_unshaded=True,
                 credit="black flames, 1 block wide and 2 tall, animated. Base 8,0,8. .glow().unshaded().seeThrough(), stretch with .scale(w, h, w)")


def web():
    b = [plate((0, 0, 0), (16, 0.02, 16), web_picture(), name="web")]
    return Model("demoness/web", b, res=8, preview_unshaded=True,
                 credit="a round spider web lying flat, 1 block wide. Pivot 8,0,8 = its middle. White: tint with .color(). .glow().unshaded().seeThrough()")


COCOON = [(0, 3, 4.5), (3, 8, 6), (8, 16, 7), (16, 24, 6.75), (24, 29, 5.5), (29, 32, 3.5)]


def cocoon():
    """A spindle of silk round something 2 blocks tall. 0 = loose rings of silk floating round it, 2 = shut."""
    shut = []
    for i, (y0, y1, half) in enumerate(COCOON):
        faces = ["north", "south", "east", "west"] + (["down"] if i == 0 else []) + (["up"] if i == len(COCOON) - 1 else [])
        shut += octagon(y0, y1, half, silk, twist=22.5 if i % 2 else 0, faces=faces, name=f"wrap{i}")
    loose, half_shut = [], []
    for i, box in enumerate(shut):
        y0, y1, half = COCOON[i // 2]
        mid = (y0 + y1) / 2
        for target, (grow, thin) in ((loose, (4.5, 0.18)), (half_shut, (1.5, 0.6))):
            h = max(0.6, (y1 - y0) * thin)
            hw = half + grow
            target.append(box.moved(frm=(8 - hw, mid - h / 2, 8 - hw), to=(8 + hw, mid + h / 2, 8 + hw),
                                    rot=(box.rot[0], box.rot[1], (8, mid - h / 2, 8)) if box.rot else None))
    return Model("demoness/cocoon", [loose, half_shut, shut], res=2, paint_frame=2,
                 credit="a cocoon of silk in 3 steps: 0 = loose rings floating round, 2 = wrapped shut, 2 blocks tall. Pivot 8,0,8. .frames(3).smooth()")


def plague_sigil():
    return Model("demoness/plague_sigil", [plate((0, 0, 0), (16, 0.02, 16), plague_sigil_picture())], res=6,
                 preview_unshaded=True, credit="a flat sickly-green seal, 1 block wide: Plague's mark. Pivot 8,0,8. .glow().unshaded().seeThrough()")


def curse_sigil():
    return Model("demoness/curse_sigil", [plate((0, 0, 0), (16, 0.02, 16), curse_sigil_picture())], res=6,
                 preview_unshaded=True, credit="a flat crimson seal with an eye in a ring of thorns, 1 block wide: the Curse's mark. Pivot 8,0,8. .glow().unshaded().seeThrough()")


def black_rose():
    """A black rose in 3 steps: 0 = a closed bud, 2 = in full bloom. The stem stands 0..10."""
    def build(outer, inner, core_lift):
        b = [Box((7.5, 0, 7.5), (8.5, 11, 8.5), stem, name="stem"),
             Box((8.5, 3, 7.75), (9.5, 4, 8.25), thorn, name="thorn1"),
             Box((6.5, 6, 7.75), (7.5, 7, 8.25), thorn, name="thorn2"),
             Box((8.5, 4, 7.9), (13, 5.5, 8.1), leaf, rot=("z", 22.5, (8.5, 4, 8)), name="leaf_right"),
             Box((3, 6.5, 7.9), (7.5, 8, 8.1), leaf, rot=("z", -22.5, (7.5, 6.5, 8)), name="leaf_left"),
             Box((6.5, 10.5 + core_lift, 6.5), (9.5, 14 + core_lift, 9.5), rose_core, rot=("y", 45, (8, 10.5, 8)), name="core")]
        # outer petals on the four sides, hinged at their base, tipped out by 'outer' degrees
        b += [Box((5.5, 10.5, 4.6), (10.5, 16, 5.4), rose_petal, rot=("x", -outer, (8, 10.5, 5)), name="petal_n"),
              Box((5.5, 10.5, 10.6), (10.5, 16, 11.4), rose_petal, rot=("x", outer, (8, 10.5, 11)), name="petal_s"),
              Box((10.6, 10.5, 5.5), (11.4, 16, 10.5), rose_petal, rot=("z", -outer, (11, 10.5, 8)), name="petal_e"),
              Box((4.6, 10.5, 5.5), (5.4, 16, 10.5), rose_petal, rot=("z", outer, (5, 10.5, 8)), name="petal_w")]
        # inner petals, a little higher and closer in
        b += [Box((6.5, 11.5, 5.9), (9.5, 16.5, 6.6), rose_petal, rot=("x", -inner, (8, 11.5, 6.25)), name="inner_n"),
              Box((6.5, 11.5, 9.4), (9.5, 16.5, 10.1), rose_petal, rot=("x", inner, (8, 11.5, 9.75)), name="inner_s"),
              Box((9.4, 11.5, 6.5), (10.1, 16.5, 9.5), rose_petal, rot=("z", -inner, (9.75, 11.5, 8)), name="inner_e"),
              Box((5.9, 11.5, 6.5), (6.6, 16.5, 9.5), rose_petal, rot=("z", inner, (6.25, 11.5, 8)), name="inner_w")]
        return b

    return Model("demoness/black_rose", [build(-22.5, -22.5, 0), build(22.5, 0, 0.5), build(45, 22.5, 1)], res=4, paint_frame=2,
                 credit="a black rose with crimson edges, 1 block tall, in 3 steps: 0 = a closed bud, 2 = open. Pivot 8,0,8 = the foot of its stem. .frames(3).smooth()")


def fissure():
    pic = crack_picture(128, 3, (16, 6, 6, 255), (255, 70, 30, 255), (200, 20, 10, 120))
    return Model("demoness/fissure", [plate((0, 0, 0), (16, 0.02, 16), pic)], res=8, preview_unshaded=True,
                 credit="cracks in the ground glowing red, 1 block wide. Pivot 8,0,8. .glow().unshaded().seeThrough(), .scale(width)")


def frost():
    return Model("demoness/frost", [plate((0, 0, 0), (16, 0.02, 16), frost_picture())], res=8, preview_unshaded=True,
                 credit="frost spreading over the ground, 1 block wide. Pivot 8,0,8. .glow().unshaded().seeThrough(), .scale(width)")


def rock():
    b = [Box((4, 4, 4), (12, 11, 12), stone, name="body"),
         Box((5, 9, 5), (11, 13, 11), stone, rot=("y", 45, (8, 9, 8)), name="top"),
         Box((3, 5, 6), (6, 9, 10), stone, rot=("z", 22.5, (4, 5, 8)), name="lump")]
    return Model("demoness/rock", b, res=2, credit="a chunk of rock split open, glowing red in its cracks, 0.6 blocks. Pivot 8,8,8.")


def hailstone():
    b = [Box((5.5, 5.5, 5.5), (10.5, 10.5, 10.5), ice, name="core"),
         Box((6, 6, 6), (10, 10, 10), ice, rot=("y", 45, (8, 8, 8)), name="facet_y"),
         Box((6, 6, 6), (10, 10, 10), ice, rot=("x", 45, (8, 8, 8)), name="facet_x")]
    return Model("demoness/hailstone", b, res=3, credit="a jagged lump of hail, 0.4 blocks. Pivot 8,8,8. .glow().seeThrough()")


def tornado():
    b = []
    levels = 9
    for i in range(levels):
        y0 = i * 32 / levels
        y1 = (i + 1) * 32 / levels + 0.6
        half = 1.6 + 14.2 * (i / (levels - 1)) ** 1.7
        b += octagon(y0, min(32, y1), half, storm, twist=22.5 if i % 2 else 0, name=f"level{i}")
    return Model("demoness/tornado", b, res=1, anim=8, frametime=2, preview_unshaded=True,
                 credit="a funnel of wind, animated, 2 blocks tall and 2 wide at the top. Pivot 8,0,8 = its foot. .glow().unshaded().seeThrough().spin(...)")


def plague_cloud():
    b = [Box((3, 3, 3), (13, 12, 13), plague_smoke, name="heart"),
         Box((-2, 2, 4), (6, 9, 11), plague_smoke, rot=("y", 22.5, (2, 2, 8)), name="puff_west"),
         Box((10, 3, 5), (18, 10, 12), plague_smoke, rot=("y", -22.5, (14, 3, 8)), name="puff_east"),
         Box((4, 2, -2), (11, 8, 5), plague_smoke, rot=("y", 45, (8, 2, 2)), name="puff_north"),
         Box((5, 2, 11), (12, 9, 18), plague_smoke, name="puff_south"),
         Box((5, 9, 5), (12, 16, 12), plague_smoke, rot=("y", 45, (8, 9, 8)), name="crown")]
    return Model("demoness/plague_cloud", b, res=2, preview_unshaded=True,
                 credit="a cloud of plague, murky green and black, about 1.25 blocks wide. Pivot 8,8,8. .glow().unshaded().seeThrough()")


def lightning():
    b = []
    L = 6.4
    angles = [22.5, -22.5, 22.5, -22.5, 22.5]
    x, y = 8.0, 0.0
    joints = []
    for i, a in enumerate(angles):
        r = math.radians(a)
        cx, cy = x - L / 2 * math.sin(r), y + L / 2 * math.cos(r)
        for core, w in ((False, 1.0), (True, 0.35)):
            b.append(Box((cx - w, cy - L / 2, 8 - w), (cx + w, cy + L / 2, 8 + w), bolt,
                         rot=("z", a, (cx, cy, 8)), name=f"{'core' if core else 'glow'}{i}", core=core))
        x, y = x - L * math.sin(r), y + L * math.cos(r)
        joints.append((x, y))
    for j, (jx, jy), a in ((0, joints[1], -45), (1, joints[3], 45)):           # two forks
        r = math.radians(a)
        cx, cy = jx + 2.2 * math.sin(r), jy - 2.2 * math.cos(r)
        for core, w in ((False, 0.7), (True, 0.25)):
            b.append(Box((cx - w, cy - 2.2, 8 - w), (cx + w, cy + 2.2, 8 + w), bolt,
                         rot=("z", a, (cx, cy, 8)), name=f"fork{j}{'core' if core else ''}", core=core))
    return Model("demoness/lightning", b, res=3, preview_unshaded=True,
                 credit="a red lightning bolt, 2 blocks tall, with two forks. Pivot 8,0,8 = where it strikes. .glow().unshaded().seeThrough()")


def blood_sky():
    return Model("demoness/blood_sky", [plate((0, 0, 0), (16, 0.02, 16), sky_picture())], res=8, preview_unshaded=True,
                 credit="a swirl of dark red cloud seen from below, 1 block wide, fading at its rim. Pivot 8,0,8. Scale it up a lot and hang it high. .glow().unshaded().seeThrough()")


ALL = {f.__name__: f for f in (mirror, mirror_shard, mirror_shard_glow, glass_shard, dagger, black_flame,
                                black_flame_pillar, web, cocoon, plague_sigil, curse_sigil, black_rose, fissure,
                                frost, rock, hailstone, tornado, plague_cloud, lightning, blood_sky)}


def main(names):
    os.makedirs(PREVIEW, exist_ok=True)
    for name in names or ALL:
        model = ALL[name]().build().write()
        model.preview().save(os.path.join(PREVIEW, name + ".png"))
        print(f"{model.path:36s} texture {model.width}x{model.height * max(1, model.anim)}  shapes {len(model.frames)}")


if __name__ == "__main__":
    main(sys.argv[1:])
