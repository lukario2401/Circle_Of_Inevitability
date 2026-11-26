"""Painted 128 x 128 material atlases for the Filthy Monarch models.

Sixteen 32 x 32 px materials (4 x 4 uv each). The static flesh atlas is frame 0 of the same painting; the abyss atlas
stacks four frames whose noise scrolls by exactly one period, so the 4-tick-per-frame loop is seamless.
Every zone is stretched across each face it is put on, so each material carries a darker 2 px rim that keeps cube edges
readable, and the translucent ones (flame tips, membranes, liquid) fade out instead of ending in a hard block.
"""
import math
import numpy as np
from PIL import Image

SIZE = 32
FRAMES = 4

# Pale sulfur blue, icy core, abyss black, venetian red, congealed blood, filth, scum and bone.
BLUE = np.array((127, 216, 255)); BLUE_D = np.array((34, 92, 168)); PALE = np.array((230, 250, 255))
BLACK = np.array((18, 10, 14)); ABYSS = np.array((40, 18, 44)); RED = np.array((200, 8, 21))
RED_L = np.array((255, 92, 70)); BLOOD = np.array((138, 15, 26)); BLOOD_D = np.array((58, 4, 12))
FILTH = np.array((92, 90, 30)); FILTH_D = np.array((40, 38, 14)); EDGE = np.array((172, 169, 80))
BONE = np.array((206, 198, 170)); BONE_D = np.array((120, 108, 84))

ZONES = ['blue', 'pale', 'black', 'red',
         'blood', 'filth', 'edge', 'bone',
         'flame_tip', 'membrane', 'ember', 'glyph',
         'scum', 'eye', 'flesh', 'liquid']


def _lattice(period, seed):
    return np.random.default_rng(seed).random((period, period))


def noise(period, seed, shift=0.0, octaves=3):
    """Tileable value noise on a SIZE grid; shift scrolls it vertically by pixels (wraps every SIZE)."""
    out = np.zeros((SIZE, SIZE)); amp = 1.0; total = 0.0
    for o in range(octaves):
        p = period * 2 ** o
        grid = _lattice(p, seed + o * 31)
        ys, xs = np.mgrid[0:SIZE, 0:SIZE].astype(float)
        fx = xs * p / SIZE; fy = ((ys + shift) % SIZE) * p / SIZE
        x0 = np.floor(fx).astype(int); y0 = np.floor(fy).astype(int)
        tx = fx - x0; ty = fy - y0
        tx = tx * tx * (3 - 2 * tx); ty = ty * ty * (3 - 2 * ty)
        x1 = (x0 + 1) % p; y1 = (y0 + 1) % p; x0 %= p; y0 %= p
        a = grid[y0, x0] * (1 - tx) + grid[y0, x1] * tx
        b = grid[y1, x0] * (1 - tx) + grid[y1, x1] * tx
        out += amp * (a * (1 - ty) + b * ty); total += amp; amp *= .5
    return out / total


def ramp(stops, t):
    t = np.clip(t, 0, 1)[..., None]
    out = np.zeros(t.shape[:-1] + (3,))
    for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
        k = np.clip((t - t0) / max(1e-6, t1 - t0), 0, 1)
        inside = (t >= t0) & (t <= t1)
        out = np.where(inside, np.asarray(c0) + (np.asarray(c1) - np.asarray(c0)) * k, out)
    return out


def rim(width=2):
    ys, xs = np.mgrid[0:SIZE, 0:SIZE]
    d = np.minimum(np.minimum(xs, SIZE - 1 - xs), np.minimum(ys, SIZE - 1 - ys))
    return np.clip(1 - d / width, 0, 1)


def paint(zone, frame, animated):
    """RGBA float array (SIZE, SIZE, 4) in 0..255 for one material and frame."""
    f = frame if animated else 0
    phase = f * math.tau / FRAMES
    ys, xs = np.mgrid[0:SIZE, 0:SIZE].astype(float)
    tx = (xs + .5) / SIZE; ty = (ys + .5) / SIZE
    edge = rim()
    alpha = np.full((SIZE, SIZE), 255.0)
    if zone in ('blue', 'flame_tip'):
        # Rising sulfur flame: streaked, pale at the root, a darker blue skin; tips thin out to nothing.
        n = noise(4, 11, shift=f * SIZE / FRAMES)
        streak = .5 + .5 * np.sin(tx * math.tau * 3 + n * 5)
        heat = (1 - ty) * .15 + ty * .75 + (n - .5) * .6 + streak * .2 - edge * .35
        rgb = ramp([(0, BLUE_D), (.45, BLUE), (.8, PALE), (1, PALE)], heat)
        if zone == 'flame_tip':
            alpha = np.clip(ty * 1.35 + (n - .5) * .9, 0, 1) * 235
            alpha = np.where(alpha < 40, 0, alpha)
        else:
            alpha = np.clip(225 + 30 * ty - 40 * edge, 0, 255)
    elif zone == 'pale':
        n = noise(4, 5, shift=f * SIZE / FRAMES)
        rgb = ramp([(0, BLUE), (.55, PALE), (1, (255, 255, 255))], .55 + (n - .5) * .8 - edge * .4)
    elif zone == 'black':
        # Abyss tar: nearly black violet with a slow oily sheen and the odd sulfur spark.
        n = noise(3, 21, shift=f * SIZE / FRAMES)
        sheen = np.clip(1 - abs(n - .55) * 16, 0, 1)
        rgb = ramp([(0, BLACK), (.6, ABYSS), (1, (62, 34, 74))], n * .8) + sheen[..., None] * np.array((14, 26, 44))
        spark = noise(8, 22, shift=-f * SIZE / FRAMES, octaves=1) > .93
        rgb = np.where(spark[..., None], BLUE * .8, rgb)
        rgb = rgb * (1 - edge[..., None] * .5)
    elif zone == 'red':
        n = noise(4, 31, shift=f * SIZE / FRAMES)
        glow = 1 - np.hypot(tx - .5, ty - .5) * 1.4
        rgb = ramp([(0, BLOOD_D), (.4, RED), (.85, RED_L), (1, (255, 190, 150))], glow * .8 + (n - .5) * .5 + .2)
        rgb = rgb * (1 - edge[..., None] * .45)
    elif zone == 'blood':
        n = noise(4, 41, shift=f * SIZE / FRAMES * .5)
        clot = noise(8, 42) > .62
        rgb = ramp([(0, BLOOD_D), (.55, BLOOD), (1, RED)], n)
        rgb = np.where(clot[..., None], BLOOD_D, rgb)
        gloss = np.clip(1 - abs((tx + ty * .4) - .38) * 14, 0, 1) * (ty < .5)
        rgb = rgb + gloss[..., None] * np.array((120, 70, 70))
        rgb = rgb * (1 - edge[..., None] * .5)
    elif zone in ('filth', 'scum'):
        n = noise(4, 51, shift=f * SIZE / FRAMES * .5)
        bubble = noise(8, 52 + f) > .74
        if zone == 'filth':
            rgb = ramp([(0, FILTH_D), (.6, FILTH), (1, EDGE)], n)
            rgb = np.where(bubble[..., None], EDGE * .95, rgb)
        else:
            # Pale foam on top of sludge waves and pools.
            rgb = ramp([(0, FILTH), (.5, EDGE), (1, (222, 220, 150))], n + .2)
            alpha = np.clip((n + .25) * 300, 0, 230)
        rgb = rgb * (1 - edge[..., None] * .45)
    elif zone == 'edge':
        n = noise(4, 61, shift=f * SIZE / FRAMES)
        drip = (np.sin(xs * 1.7) > .6) & (ty > .5 + .3 * noise(4, 62))
        rgb = ramp([(0, FILTH), (.5, EDGE), (1, (214, 210, 120))], n + .1)
        rgb = np.where(drip[..., None], FILTH_D * 1.3, rgb)
        rgb = rgb * (1 - edge[..., None] * .4)
    elif zone == 'bone':
        n = noise(4, 71)
        crack = abs(noise(3, 72) - .5) < .03
        rgb = ramp([(0, BONE_D), (.7, BONE), (1, (240, 234, 214))], (1 - ty) * .6 + n * .5)
        rgb = np.where(crack[..., None], BONE_D * .7, rgb)
        rgb = rgb * (1 - edge[..., None] * .45)
    elif zone == 'membrane':
        # Translucent wing skin: dark crimson web with lighter veins, thinning away from the bone.
        n = noise(4, 81, shift=f * SIZE / FRAMES * .25)
        vein = abs(noise(4, 82, octaves=2) - .5) < .03
        rgb = ramp([(0, (26, 6, 14)), (.6, (88, 16, 34)), (1, (140, 36, 50))], n * .8 + ty * .25)
        rgb = np.where(vein[..., None], np.array((170, 52, 58)), rgb)
        edge = edge * 0  # neighbouring membrane columns must read as one sheet
        alpha = np.clip(215 - ty * 70 + (n - .5) * 40, 120, 230)
    elif zone == 'ember':
        # Cooled black crust split by blue-white magma cracks: the blue Sword of Lava and eruption rock.
        n = noise(3, 91)
        cells = noise(6, 92, octaves=1)
        crack = np.clip(1 - abs(cells - .5) * 16, 0, 1)
        pulse = .65 + .35 * math.sin(phase)
        rgb = ramp([(0, BLACK), (1, (52, 34, 46))], n) * (1 - crack[..., None])
        rgb = rgb + crack[..., None] * ramp([(0, BLUE_D), (.6, BLUE), (1, PALE)], crack * pulse + .1)
        rgb = rgb * (1 - edge[..., None] * .3)
    elif zone == 'glyph':
        # Red Devil-tongue strokes on black: an abstract pattern, not a real script.
        n = noise(4, 101)
        g = np.zeros((SIZE, SIZE), bool)
        for i, (x0, y0, x1, y1) in enumerate(((6, 5, 6, 26), (6, 8, 14, 5), (14, 5, 14, 15), (14, 15, 25, 15),
                                               (20, 15, 20, 27), (25, 6, 25, 21), (10, 21, 18, 27))):
            for t in np.linspace(0, 1, 40):
                x = int(round(x0 + (x1 - x0) * t)); y = int(round(y0 + (y1 - y0) * t))
                g[max(0, y - 1):y + 1, max(0, x - 1):x + 1] = True
        flicker = .7 + .3 * math.sin(phase + 1)
        rgb = np.where(g[..., None], ramp([(0, RED), (1, RED_L)], np.full_like(n, flicker)),
                       ramp([(0, BLACK), (1, ABYSS)], n))
        rgb = rgb * (1 - edge[..., None] * .4)
    elif zone == 'eye':
        d = np.hypot(tx - .5, (ty - .5) * 1.3)
        pupil = abs(tx - .5) < .07
        blink = .85 + .15 * math.sin(phase * 2)
        rgb = ramp([(0, (255, 255, 255)), (.25, PALE), (.5, BLUE), (1, BLUE_D)], d * 1.6 / blink)
        rgb = np.where((pupil & (d < .42))[..., None], BLACK, rgb)
    elif zone == 'flesh':
        # Raw muscle: fine striations, thin dark veins and a slow throb.
        n = noise(4, 111, shift=f * SIZE / FRAMES * .3)
        fiber = .5 + .5 * np.sin(ty * math.tau * 7 + n * 6 + tx * 2)
        vein = abs(noise(6, 112, octaves=2) - .5) < .022
        throb = .92 + .08 * math.sin(phase)
        rgb = ramp([(0, BLOOD_D), (.55, (122, 22, 34)), (1, (186, 62, 66))], (n * .6 + fiber * .4) * throb)
        rgb = np.where(vein[..., None], np.array((44, 4, 14)), rgb)
        rgb = rgb * (1 - edge[..., None] * .5)
    elif zone == 'liquid':
        # Desire liquid: glossy, half-transparent black with drifting crimson light inside.
        n = noise(3, 121, shift=f * SIZE / FRAMES)
        glow = noise(4, 122, shift=-f * SIZE / FRAMES) > .74
        rgb = ramp([(0, BLACK), (.7, ABYSS), (1, (90, 30, 70))], n)
        rgb = np.where(glow[..., None], np.array((120, 14, 34)), rgb)
        hl = np.clip(1 - abs(tx - .3) * 18, 0, 1) * (ty < .6)
        rgb = rgb + hl[..., None] * np.array((60, 60, 80))
        alpha = np.clip(200 + (n - .5) * 80, 150, 235)
    else:
        raise ValueError(zone)
    rgb = np.clip(rgb, 0, 255)
    return np.dstack([rgb, alpha[..., None] if alpha.ndim == 2 else alpha])


def atlas(animated):
    frames = FRAMES if animated else 1
    img = np.zeros((128 * frames, 128, 4))
    for f in range(frames):
        for i, zone in enumerate(ZONES):
            x = i % 4 * SIZE; y = i // 4 * SIZE + f * 128
            img[y:y + SIZE, x:x + SIZE] = paint(zone, f, animated)
    return Image.fromarray(np.round(img).astype(np.uint8), 'RGBA')


def uv(zone):
    i = ZONES.index(zone)
    u = i % 4 * 4; v = i // 4 * 4
    return [u, v, u + 4, v + 4]
