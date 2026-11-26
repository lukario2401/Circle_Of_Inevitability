"""Original cuboid geometry with compatible four-pose forms, painted on two 128 x 128 material atlases.

Shapes are built from a few primitives: tapered flame stacks, stepped voxel strokes (seg) for horns, claws, glyphs and
sigil lines, 16-segment rings, and scalloped membrane columns for wings. Animated forms keep every element's name,
faces and rotation identical across the four poses; only positions change, so ModelFx can blend between them.
"""
import math
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from modelkit import Model, Texture
from catalog import ABILITY_MODELS, EXTRA_MODELS, ANIMATED
import materials

SOLID_ONLY = ('grit_burst', 'blood_spray', 'blood_drop')


def material(animated=False):
    tex = Texture('filthy_monarch/' + ('abyss_atlas' if animated else 'flesh_atlas'), 128, 128,
                  frames=materials.FRAMES if animated else 1, frametime=4)
    for zone in materials.ZONES:
        tex.zone(zone, *materials.uv(zone))
    tex.img = materials.atlas(animated)
    return tex


def seg_count(p0, p1, t):
    """Boxes needed so that consecutive steps of a stroke still overlap sideways (no bead-chain look)."""
    d = [abs(p1[i] - p0[i]) for i in range(3)]
    k = max(range(3), key=lambda i: d[i])
    side = max(d[i] for i in range(3) if i != k)
    return min(8, max(1, math.ceil(side / t), math.ceil(d[k] / 4)))


class Shape:
    """Model wrapper: centred boxes, stepped strokes and an optional x stretch about the pivot."""

    def __init__(self, model, sx=1.0, counts=None, dry=False):
        # counts/dry: animated forms first run every pose dry to find the most boxes each stroke needs, then build
        # each pose with those counts, so all poses share one element list.
        self.m, self.sx, self.counts, self.dry = model, sx, counts, dry

    def _x(self, x):
        return 8 + (x - 8) * self.sx

    def box(self, name, frm, to, zone, rot=None):
        frm = [self._x(frm[0]), frm[1], frm[2]]; to = [self._x(to[0]), to[1], to[2]]
        frm, to = [min(a, b) for a, b in zip(frm, to)], [max(a, b) for a, b in zip(frm, to)]
        # Block-model limits: every coordinate stays inside -16..32.
        frm = [min(max(v, -16), 31.95) for v in frm]; to = [min(max(v, -15.95), 32) for v in to]
        to = [b if b - a > .05 else min(32, a + .05) for a, b in zip(frm, to)]
        if rot:
            rot = (rot[0], rot[1], (self._x(rot[2][0]), rot[2][1], rot[2][2]))
        return self.m.cube(name, frm, to, zone, rot=rot)

    def cube(self, name, c, s, zone, rot=None):
        return self.box(name, [c[i] - s[i] / 2 for i in range(3)], [c[i] + s[i] / 2 for i in range(3)], zone, rot)

    def seg(self, name, p0, p1, t, zone, n=None, taper=1.0):
        """A stroke from p0 to p1 as n boxes, contiguous along its dominant axis; t shrinks to t*taper at p1."""
        d = [p1[i] - p0[i] for i in range(3)]
        k = max(range(3), key=lambda i: abs(d[i]))
        if n is None:
            n = seg_count(p0, p1, t * (1 + taper) / 2)
            if self.counts is not None:
                if self.dry:
                    self.counts[name] = max(self.counts.get(name, 1), n)
                else:
                    n = self.counts[name]
        for i in range(n):
            a, b = i / n, (i + 1) / n
            tt = t * (1 + (taper - 1) * (i + .5) / n)
            c = [p0[j] + d[j] * (a + b) / 2 for j in range(3)]
            s = [tt] * 3; s[k] = max(tt * .6, abs(d[k]) / n + .02)
            self.cube(f'{name}_{i}', c, s, zone)

    def chain(self, name, points, t, zone, taper=1.0, n=None):
        """Polyline through points, thickness tapering along the whole chain. n: boxes per span (a list fixes each
        span's count, which animated forms need so every pose has the same elements)."""
        spans = len(points) - 1
        for i in range(spans):
            t0 = t * (1 + (taper - 1) * i / spans); t1 = t * (1 + (taper - 1) * (i + 1) / spans)
            count = n[i] if isinstance(n, list) else n
            self.seg(f'{name}_{i}', points[i], points[i + 1], t0, zone, n=count, taper=t1 / t0)

    def ring(self, name='ring', radius=7, y=.5, zone='blue', thick=.35, height=.5):
        for i in range(16):
            a = i * math.tau / 16; x = 8 + radius * math.cos(a); z = 8 + radius * math.sin(a)
            length = radius * math.tan(math.pi / 16) + .15; degrees = i * 22.5; q = math.floor((degrees + 45) / 90)
            angle = -degrees + q * 90
            dx, dz = (length, thick) if q % 2 else (thick, length)
            self.box(f'{name}_{i}', (x - dx, y, z - dz), (x + dx, y + height, z + dz), zone, rot=(angle, 'y', (x, y, z)))

    def flame(self, name='flame', x=8, y=0, z=8, height=16, r=4, phase=0.0, tongues=True):
        """Tapered blue tongue around a pale core with a black heart at the root; tips fade out."""
        layers = 6
        for i in range(layers):
            t = i / layers; w = r * (1 - t * .82) * (1 + .12 * math.sin(i * 1.3 + phase))
            drift = math.sin(i * 1.7 + phase) * r * .22 * t
            self.box(f'{name}_{i}', (x - w + drift, y + t * height, z - w),
                     (x + w + drift, y + (i + 1) / layers * height, z + w), 'flame_tip' if i == layers - 1 else 'blue')
        for i in range(3):
            w = r * .5 * (1 - i * .25); drift = math.sin(i * 1.7 + phase) * r * .15
            self.box(f'{name}_core_{i}', (x - w + drift, y + i * height * .2, z - w * 1.08),
                     (x + w + drift, y + (i + 1) * height * .2 + height * .05, z + w * 1.08), 'pale')
        self.box(name + '_heart', (x - r * .32, y, z - r * 1.02), (x + r * .32, y + height * .32, z - r * .72), 'black')
        if tongues:
            for i, (dx, dz, h) in enumerate(((1, .3, .55), (-.9, -.4, .45), (.2, 1, .4))):
                tx = x + dx * r * .85; tz = z + dz * r * .85; hh = height * h
                self.box(f'{name}_tongue_{i}', (tx - r * .22, y + height * .2, tz - r * .22),
                         (tx + r * .22, y + height * .2 + hh, tz + r * .22), 'flame_tip')


# -- forms -------------------------------------------------------------------------------------------------------

FORM_SKIN = {'devil_form': 'black', 'demon_form': 'black', 'devil_projection': 'liquid', 'desire_liquid': 'liquid',
             'abyss_form': 'black', 'filth_phantom': 'filth', 'shadow_afterimage': 'liquid'}


def humanoid(model, kind, frame, counts=None, dry=False):
    phase = frame * math.tau / 4
    s = Shape(model, 1.14 if kind == 'demon_form' else 1.0, counts, dry)
    skin = FORM_SKIN[kind]
    liquid = kind in ('desire_liquid', 'abyss_form', 'filth_phantom', 'shadow_afterimage')
    breath = .35 * math.sin(phase); sway = math.sin(phase)
    eye = {'desire_liquid': 'red', 'abyss_form': 'red', 'shadow_afterimage': 'red', 'filth_phantom': 'edge'}.get(kind, 'eye')

    # Legs: digitigrade devil legs, or a dripping mound for the liquid bodies.
    if liquid:
        mound = ((4.8, 2.4), (4, 3.4), (3.4, 4), (3, 4.2))
        for i, (w, h) in enumerate(mound):
            y0 = sum(hh for _, hh in mound[:i])
            wob = .3 * math.sin(phase + i)
            s.box(f'mound_{i}', (8 - w + wob, y0, 8 - w * .7), (8 + w + wob, y0 + h + .2, 8 + w * .7), skin)
    else:
        for side in (-1, 1):
            lift = max(0, side * sway) * 1.2; x = 8 + side * 2.3
            s.box(f'thigh_{side}', (x - 1.4, 8.5 + lift, 6.8), (x + 1.4, 14, 10.2), skin)
            s.box(f'shin_{side}', (x - 1.1, 3 + lift, 8.4), (x + 1.1, 9 + lift, 10.8), skin)
            s.box(f'hoof_{side}', (x - 1.15, 0 + lift, 6.9), (x + 1.15, 2.2 + lift, 9.7), 'ember')
            s.box(f'toe_{side}', (x - .9, 0 + lift, 5.8), (x + .9, 1 + lift, 6.9), 'bone')
            s.box(f'spur_{side}', (x - .4, 4 + lift, 10.6), (x + .4, 5 + lift, 12), 'bone')

    # Torso.
    s.box('pelvis', (5, 13, 6.6), (11, 15.2, 10.4), skin)
    s.box('waist', (5.6, 15, 7), (10.4, 19, 10), skin)
    s.box('chest', (4 - breath * .3, 19, 6 - breath * .2), (12 + breath * .3, 24.6, 11), skin)
    for i in range(3):
        y = 15.8 + i * 1.15
        s.box(f'rib_{i}', (5.9, y, 6.75), (10.1, y + .5, 7.05), 'bone' if not liquid else skin)
    s.box('core', (7, 20.4, 5.55 - breath * .2), (9, 22.6, 6.15 - breath * .2),
          'red' if kind in ('demon_form', 'abyss_form', 'desire_liquid') else 'pale')
    s.box('spine_plate', (7.2, 15, 10), (8.8, 24, 11.4), 'ember' if kind == 'demon_form' else skin)

    # Arms.
    for side in (-1, 1):
        swing = side * sway * .9; x = 8 + side * 5.7
        s.box(f'shoulder_{side}', (x - 1.8, 22.4, 6.2), (x + 1.8, 25.2, 10.6), 'ember' if kind == 'demon_form' else skin)
        s.box(f'upper_arm_{side}', (x - 1.3, 17.6, 7.2 + swing * .3), (x + 1.3, 23, 9.8 + swing * .3), skin)
        s.box(f'forearm_{side}', (x - 1.2, 12.6, 6.6 + swing), (x + 1.2, 18, 9.4 + swing), skin)
        s.box(f'hand_{side}', (x - 1.3, 11, 6.2 + swing), (x + 1.3, 12.8, 9.2 + swing), skin)
        if liquid:
            for i in range(3):
                drip = 1.2 + 1.6 * ((math.sin(phase + i * 2.1 + side) + 1) / 2) + i * .4
                dx = x + (i - 1) * .8
                s.box(f'drip_{side}_{i}', (dx - .35, 11 - drip, 7.4 + swing), (dx + .35, 11.1, 8.1 + swing),
                      'scum' if kind == 'filth_phantom' else skin)
        else:
            for i in range(3):
                cx = x + (i - 1) * .8
                s.seg(f'claw_{side}_{i}', (cx, 11.2, 6.8 + swing), (cx + side * .3, 9.2, 5.6 + swing), .5, 'bone', taper=.6)
            if kind != 'devil_projection':
                s.flame(f'palm_fire_{side}', x, 11.2, 7.6 + swing, 3.2 + .6 * math.sin(phase + side), .9,
                        phase=phase, tongues=False)
            s.seg(f'shoulder_spike_{side}', (x + side * .6, 25, 8.2), (x + side * 2.2, 27.4, 9.2), .8, 'bone', taper=.5)

    # Head.
    jaw = .5 + .5 * math.sin(phase * 2)
    s.box('neck', (6.8, 24.4, 7.4), (9.2, 25.8, 9.6), skin)
    s.box('head', (5.6, 25.6, 6.1), (10.4, 30, 10.4), skin)
    s.box('brow', (5.4, 28.6, 5.6), (10.6, 29.4, 6.3), skin)
    s.box('snout', (6.6, 25.9, 4.7), (9.4, 27.8, 6.2), skin)
    s.box('jaw', (6.8, 24.8 - jaw * .6, 5), (9.2, 25.8 - jaw * .6, 6.6), skin)
    for side in (-1, 1):
        s.box(f'eye_{side}', (8 + side * 1.45 - .65, 27.9, 5.85), (8 + side * 1.45 + .65, 28.6, 6.15), eye)
        s.box(f'fang_{side}', (8 + side * .9 - .25, 25.2 - jaw * .3, 4.75), (8 + side * .9 + .25, 26, 5), 'bone')

    if not liquid:
        # Curling horns, tipped with sulfur fire.
        for side in (-1, 1):
            pts = [(8 + side * 2.2, 29.6, 7.6), (8 + side * 3.6, 31, 8.2), (8 + side * 5, 31.6, 9.6), (8 + side * 5.8, 30.6, 11.4)]
            s.chain(f'horn_{side}', pts, 1.4, 'bone', taper=.4)
            s.cube(f'horn_tip_{side}', (8 + side * 5.9, 30.2, 11.9), (.5, .5, .5), 'blue')
        if kind == 'demon_form':
            for side in (-1, 1):
                pts = [(8 + side * 1.2, 30, 7.2), (8 + side * 1.6, 31.6, 6.4), (8 + side * 2.2, 31.9, 5.2)]
                s.chain(f'brow_horn_{side}', pts, .9, 'bone', taper=.5)
            for i in range(4):
                y = 16 + i * 2.2
                s.seg(f'spine_spike_{i}', (8, y, 11.4), (8, y + 1.4, 13 + i * .2), .7, 'bone', taper=.5)

        # Wings: a bone arm, three fingers and scalloped membrane columns between them.
        flap = 2.6 * math.cos(phase); span = 1.2 if kind == 'demon_form' else 1.0
        for side in (-1, 1):
            root = (8 + side * 3, 23, 11); elbow = (8 + side * 8.5 * span, 27 + flap * .6, 12.6)
            tip = (8 + side * 15.5 * span, 29.4 + flap * 1.1, 13.6)
            s.chain(f'wing_arm_{side}', [root, elbow, tip], .9, 'bone', taper=.6)
            fingers = [(8 + side * 15.8 * span, 22.5 + flap * 1.2, 14), (8 + side * 13.2 * span, 17 + flap, 13.8),
                       (8 + side * 10 * span, 14.2 + flap * .7, 13.2)]
            for i, f in enumerate(fingers):
                s.seg(f'wing_finger_{side}_{i}', elbow, f, .75, 'bone', taper=.5)
            # Membrane outline from root to tip: the arm on top, scallops between finger tips below.
            bottom = [(root[0], 16.5), (fingers[2][0], fingers[2][1]), (fingers[1][0], fingers[1][1]),
                      (fingers[0][0], fingers[0][1]), (tip[0], tip[1] - .5)]
            cols = 10
            for i in range(cols):
                a = i / cols; b = (i + 1) / cols
                x0 = root[0] + (tip[0] - root[0]) * a; x1 = root[0] + (tip[0] - root[0]) * b; xm = (x0 + x1) / 2

                def top_at(x):
                    if abs(x - root[0]) <= abs(elbow[0] - root[0]):
                        k = (x - root[0]) / (elbow[0] - root[0]); return root[1] + (elbow[1] - root[1]) * k
                    k = (x - elbow[0]) / (tip[0] - elbow[0]); return elbow[1] + (tip[1] - elbow[1]) * k

                def bottom_at(x):
                    for (xa, ya), (xb, yb) in zip(bottom, bottom[1:]):
                        if min(xa, xb) - 1e-6 <= x <= max(xa, xb) + 1e-6:
                            k = (x - xa) / ((xb - xa) or 1); scallop = math.sin(k * math.pi) * 1.6
                            return ya + (yb - ya) * k + scallop
                    return bottom[-1][1]

                y0 = bottom_at(xm); y1 = top_at(xm) - .2; z = 12.4 + .8 * a
                s.box(f'membrane_{side}_{i}', (x0, min(y0, y1 - .5), z), (x1, y1, z + .3), 'membrane')

        # Tail ending in a red spade.
        pts = []
        for i in range(6):
            t = i / 5
            pts.append((8 + math.sin(phase + t * 2.2) * 2.4 * t, 12.5 - 7 * math.sin(t * 1.6) + t * t * 6, 10.6 + t * 8))
        s.chain('tail', pts, 1.5, skin, taper=.45)
        ex, ey, ez = pts[-1]
        s.box('tail_spade', (ex - 1.3, ey - .5, ez + .2), (ex + 1.3, ey + .5, ez + 1.6), 'red')
        s.box('tail_spade_tip', (ex - .5, ey - .4, ez + 1.6), (ex + .5, ey + .4, ez + 2.6), 'red')
    else:
        # Dripping crown of tendrils on the head and trailing tendrils at the base.
        for i in range(3):
            x = 6.4 + i * 1.6; h = 1 + 1.2 * ((math.sin(phase + i * 1.9) + 1) / 2)
            s.box(f'head_drip_{i}', (x - .4, 30, 7.4), (x + .4, 30 + h, 8.4), skin)
        for side in (-1, 1):
            for i in range(3):
                x = 8 + side * (4.5 + i * 2.2); wave = math.sin(phase + i * 1.4 + side)
                s.seg(f'tendril_{side}_{i}', (8 + side * 3.5, .6, 7.5 + i), (x, 1.2 + i * 1.6 + wave, 7.5 + i + wave * .8), .8,
                      skin, taper=.4)
        puddle = 'scum' if kind == 'filth_phantom' else 'liquid'
        s.ring('puddle', 8, .05, puddle, thick=.8, height=.35)
        s.box('puddle_fill', (2.5, 0, 2.5), (13.5, .25, 13.5), 'filth' if kind == 'filth_phantom' else 'black')

    if kind == 'abyss_form':
        for i in range(5):
            x = 5.2 + i * 1.4; h = 1.4 + ((i + frame) % 3) * .9 + (1.2 if i == 2 else 0)
            s.seg(f'crown_{i}', (x, 29.6, 7.2), (x + (i - 2) * .4, 29.6 + h, 7.2 - .4), .7, 'red', taper=.5)
        for i, (x, y) in enumerate(((5.2, 22.5), (10.6, 21.2), (6.2, 17.6), (9.8, 16.6), (7.9, 19))):
            blink = .3 + .3 * ((math.sin(phase + i * 1.3) + 1) / 2)
            s.box(f'body_eye_{i}', (x - .55, y - blink, 5.8), (x + .55, y + blink, 6.15), 'eye')
        for side in (-1, 1):
            for i in range(2):
                wave = math.sin(phase + i + side) * 1.5
                s.chain(f'back_tendril_{side}_{i}', [(8 + side * 1.5, 22 - i * 3, 11), (8 + side * 5, 25 - i * 3 + wave, 15),
                                                      (8 + side * 9, 22 - i * 3 - wave, 18)], .9, 'black', taper=.4)
    if kind == 'devil_projection':
        s.ring('summon', 9, .2, 'blue', thick=.5)
        s.ring('summon_inner', 6, .2, 'glyph', thick=.4)


def hand(model, frame, counts=None, dry=False):
    s = Shape(model, 1.0, counts, dry)
    bend = (0, 1.3, 2.5, .9)[frame]; phase = frame * math.tau / 4
    s.box('mire', (1, 0, 1), (15, .3, 15), 'filth')
    s.ring('mire_ring', 8, 0, 'scum', thick=.7, height=.45)
    for i in range(5):
        a = i * math.tau / 5 + phase * .2; r = 5.5
        s.box(f'bubble_{i}', (8 + r * math.cos(a) - .5, .2, 8 + r * math.sin(a) - .5),
              (8 + r * math.cos(a) + .5, .7 + .4 * ((frame + i) % 2), 8 + r * math.sin(a) + .5), 'scum')
    s.box('forearm', (5.4, 0, 6.2), (10.6, 6, 10.8), 'filth')
    s.box('wrist', (5, 6, 6), (11, 9, 11), 'filth')
    s.box('palm', (3.4, 8.6, 6.2), (12.6, 15.6, 10.2), 'filth')
    for i in range(4):
        x = 3.6 + i * 2.35; h = 9.6 - abs(i - 1.5) * 1.3
        pts = [(x + .8, 15.4, 7.6), (x + .8, 15.4 + h * .45, 7.4 - bend * .3), (x + .8, 15.4 + h * .8 - bend * .4, 6.6 - bend),
               (x + .8, 15.4 + h - bend * 1.1, 5.4 - bend * 1.6)]
        s.chain(f'finger_{i}', pts, 1.7, 'filth', taper=.6)
        s.box(f'knuckle_{i}', (x + .1, 15, 6), (x + 1.5, 16.2, 7.2), 'scum')
        tx, ty, tz = pts[-1]
        s.seg(f'nail_{i}', (tx, ty, tz), (tx, ty - .3 - bend * .4, tz - 1.2), .6, 'bone')
    s.chain('thumb', [(12.4, 10, 7), (14.6, 12 + bend * .3, 6.2), (15, 14.2 + bend * .2, 5 - bend * .5)], 1.6, 'filth', taper=.6)
    for i in range(4):
        x = 4 + i * 2.6; drip = 1.4 + 1.8 * ((math.sin(phase + i * 1.7) + 1) / 2)
        s.box(f'drip_{i}', (x - .3, 8.6 - drip, 5.6), (x + .3, 8.8, 6.1), 'scum')


# -- props -------------------------------------------------------------------------------------------------------

GLYPHS = {
    # Abstract Devil-tongue strokes on a 16 x 16 plane (x, y). Nonlinguistic, and no real religious symbols.
    'slow': [((4, 15), (12, 15)), ((4, 15), (8, 9)), ((12, 15), (8, 9)), ((8, 9), (4, 3)), ((8, 9), (12, 3)), ((4, 3), (12, 3))],
    'die': [((8, 16), (8, 3)), ((4, 12), (12, 12)), ((4, 12), (3, 7)), ((12, 12), (13, 7)), ((5, 3), (11, 3))],
    'confusion': [((8, 9), (11, 10)), ((11, 10), (10, 13)), ((10, 13), (6, 13)), ((6, 13), (4, 9)), ((4, 9), (6, 5)),
                  ((6, 5), (12, 5)), ((12, 5), (14, 10)), ((14, 10), (12, 16))],
    'sloth': [((3, 15), (13, 15)), ((13, 15), (3, 5)), ((3, 5), (13, 5)), ((11, 5), (12, 2))],
    'blasphemy': [((3, 10), (5, 14)), ((5, 14), (11, 14)), ((11, 14), (13, 10)), ((13, 10), (12, 7)), ((4, 7), (3, 10)),
                  ((14, 16), (2, 2))],
    'blather': [((3, 11), (8, 15)), ((8, 15), (13, 11)), ((3, 11), (8, 7)), ((8, 7), (13, 11)), ((6, 6), (5, 2)),
                ((8, 6), (8, 1)), ((10, 6), (11, 2))],
}


def prop(model, n):
    s = Shape(model)
    if n == 'claw_slash':
        # Three curved claw arcs with pale leading edges.
        for k in range(3):
            x0 = 2 + k * 5
            pts = [(x0 + 1, 15 - k * .5, 8), (x0 + 3.5, 10, 8), (x0 + 4.2, 5, 8), (x0 + 3.4, .5 + k * .5, 8)]
            s.chain(f'claw_{k}', pts, 1.5, 'blood', taper=.3)
            s.chain(f'edge_{k}', [(p[0] - .7, p[1], p[2] - .4) for p in pts], .5, 'pale', taper=.4)
    elif n == 'grit_burst':
        for i in range(12):
            a = i * 2.4; r = 2 + (i * 7 % 5)
            x = 8 + r * math.cos(a); y = 6 + r * math.sin(a) * .8; z = 8 + (i % 3 - 1) * 2
            sz = .8 + (i % 3) * .5
            s.cube(f'grit_{i}', (x, y, z), (sz, sz, sz), ('filth', 'edge', 'blood', 'bone')[i % 4])
        s.box('pouch', (6.6, 0, 7), (9.4, 2.6, 9.6), 'flesh')
        s.box('pouch_tie', (7, 2.6, 7.4), (9, 3.2, 9.2), 'bone')
    elif n == 'blood_spray':
        for i in range(14):
            a = i * 2.39996; r = 1.5 + (i * 5 % 7) * .9
            x = 8 + r * math.cos(a); y = 4 + r * .8 + (i % 3); z = 8 + r * math.sin(a) * .6
            sz = 1.6 - (i % 4) * .3
            s.cube(f'drop_{i}', (x, y, z), (sz, sz * 1.3, sz), 'blood')
            if i % 3 == 0:
                s.cube(f'trail_{i}', (x - math.cos(a) * sz, y - .8, z - math.sin(a) * sz * .6), (sz * .5, sz * .5, sz * .5), 'red')
    elif n in ('flame_jet', 'eruption_pillar', 'hellfire_pillar', 'abyss_flame_beam'):
        s.flame('flame', height=26, r=3.4 if n != 'abyss_flame_beam' else 3)
        if n == 'eruption_pillar':
            for i in range(6):
                a = i * math.tau / 6; x = 8 + 5.5 * math.cos(a); z = 8 + 5.5 * math.sin(a)
                s.box(f'rock_{i}', (x - 1.4, 0, z - 1.4), (x + 1.4, 1.6 + (i % 3) * .8, z + 1.4), 'ember')
            s.ring('base', 8, 0, 'ember', thick=.8)
        elif n == 'hellfire_pillar':
            s.ring('base', 8, 0, 'red', thick=.6)
            for i in range(4):
                a = i * math.tau / 4 + .4
                s.flame(f'ground_{i}', 8 + 5.6 * math.cos(a), 0, 8 + 5.6 * math.sin(a), 7, 1.2, phase=i, tongues=False)
        elif n == 'abyss_flame_beam':
            for i in range(4):
                y = 3 + i * 6
                s.ring(f'band_{i}', 3.8, y, 'black', thick=.3, height=.6)
    elif n == 'sulfur_fireball':
        # Layered sphere: black heart, pale core, blue shell, trailing tongues.
        R = 5.6; slices = 7
        for i in range(slices):
            y0 = 7 - R + i * 2 * R / slices; y1 = y0 + 2 * R / slices; yc = (y0 + y1) / 2 - 7
            w = R * math.sqrt(max(.08, 1 - (yc / R) ** 2))
            s.box(f'shell_{i}', (8 - w, y0, 8 - w * .62), (8 + w, y1, 8 + w * .62), 'blue')
            s.box(f'shell_side_{i}', (8 - w * .62, y0, 8 - w), (8 + w * .62, y1, 8 + w), 'blue')
        s.box('core', (6, 4.6, 4.6), (10, 9.4, 11.4), 'pale')
        s.box('core_side', (4.6, 4.6, 6), (11.4, 9.4, 10), 'pale')
        for i in range(4):
            a = i * math.tau / 4
            s.box(f'tail_{i}', (8 + 2 * math.cos(a) - .9, 11.5, 8 + 2 * math.sin(a) - .9),
                  (8 + 2 * math.cos(a) + .9, 14 + (i % 2) * 1.5, 8 + 2 * math.sin(a) + .9), 'flame_tip')
    elif n == 'lava_sword':
        s.box('pommel', (7, 0, 7), (9, 1.4, 9), 'red')
        s.box('grip', (7.3, 1.4, 7.3), (8.7, 7, 8.7), 'flesh')
        for i in range(3):
            s.box(f'wrap_{i}', (7.1, 2 + i * 1.6, 7.1), (8.9, 2.6 + i * 1.6, 8.9), 'bone')
        s.box('guard', (3.2, 7, 6.6), (12.8, 8.8, 9.4), 'ember')
        for side in (-1, 1):
            s.seg(f'guard_horn_{side}', (8 + side * 4.4, 8.4, 8), (8 + side * 6.4, 11.4, 8), .9, 'bone', taper=.5)
        s.box('blade', (5.8, 8.8, 7.3), (10.2, 26, 8.7), 'ember')
        s.box('edge_l', (5.2, 9.5, 7.6), (5.8, 25, 8.4), 'blue')
        s.box('edge_r', (10.2, 9.5, 7.6), (10.8, 25, 8.4), 'blue')
        s.box('fuller', (7.6, 9.5, 7.1), (8.4, 24, 8.9), 'pale')
        for i in range(4):
            y = 11 + i * 3.6; side = 1 if i % 2 else -1
            s.box(f'serration_{i}', (8 + side * 2.8 - .5, y, 7.7), (8 + side * 2.8 + .5, y + 1.4, 8.3), 'blue')
        s.seg('tip', (8, 26, 8), (8, 31.5, 8), 4, 'ember', taper=.2)
        for i in range(3):
            s.flame(f'lick_{i}', 8 + (i - 1) * 2.2, 12 + i * 4.5, 8, 4, .8, phase=i, tongues=False)
    elif n == 'flame_cage':
        s.ring('bottom', 9, 0, 'ember', thick=.6)
        s.ring('top', 9, 24, 'blue', thick=.45)
        for i in range(8):
            a = i * math.tau / 8
            s.flame('bar_' + str(i), 8 + 9 * math.cos(a), .5, 8 + 9 * math.sin(a), 23.5, 1, phase=i, tongues=False)
    elif n in ('toxic_cloud', 'corrosive_cloud', 'corrupted_fog'):
        main = {'toxic_cloud': 'filth', 'corrosive_cloud': 'black', 'corrupted_fog': 'liquid'}[n]
        accent = {'toxic_cloud': 'scum', 'corrosive_cloud': 'edge', 'corrupted_fog': 'red'}[n]
        for i in range(11):
            a = i * 2.4; r = 2.5 + (i * 3 % 5)
            x = 8 + r * math.cos(a); z = 8 + r * math.sin(a); y = 2 + (i % 4) * 1.6
            w = 3.4 - (i % 3) * .6; h = 3 + (i % 3)
            s.box(f'puff_{i}', (x - w, y, z - w), (x + w, y + h, z + w), main)
            if i % 2 == 0:
                s.box(f'wisp_{i}', (x - .3, y + h, z - .3), (x + .3, y + h + 1.5 + i % 3, z + .3), accent)
        for i in range(4):
            x = 4 + i * 2.8
            s.box(f'drip_{i}', (x - .3, 0, 7.7), (x + .3, 2.2, 8.3), accent)
    elif n in ('desire_heart', 'burning_heart'):
        # Rounded heart from offset lobes with an aorta, veins and a highlight.
        flesh = 'flesh'
        s.box('lobe_l', (2.6, 8, 5), (8.4, 15, 11), flesh)
        s.box('lobe_r', (7.6, 8, 5.4), (13.4, 14.4, 10.6), flesh)
        s.box('body', (3.6, 5, 5.2), (12.4, 9, 10.8), flesh)
        s.box('lower', (5, 2.6, 5.8), (11, 5.2, 10.2), flesh)
        s.box('apex', (6.6, .8, 6.6), (9.4, 2.8, 9.4), flesh)
        s.box('aorta', (5.4, 14.4, 6.6), (7.6, 18, 8.8), 'blood')
        s.box('artery', (9, 14, 7.4), (10.8, 16.6, 9.2), 'blood')
        s.box('vein_v', (7.6, 3.4, 4.8), (8.4, 14.4, 5.2), 'red')
        s.box('vein_h', (4.4, 10.6, 4.9), (11.4, 11.3, 5.3), 'blood')
        s.box('highlight', (3.6, 12, 4.7), (5.4, 14, 5.05), 'pale')
        if n == 'burning_heart':
            s.flame('burn', 8, 14, 8, 9, 2.2)
        else:
            for i in range(4):
                a = i * math.tau / 4 + .6
                s.seg(f'string_{i}', (8 + 4 * math.cos(a), 15, 8 + 3 * math.sin(a)), (8 + 6 * math.cos(a), 26, 8 + 5 * math.sin(a)),
                      .25, 'pale')
    elif n == 'horn_crown':
        s.ring('rim', 6, 8, 'ember', thick=.6, height=1.2)
        for side in (-1, 1):
            pts = [(8 + side * 5.6, 9, 8), (8 + side * 7.4, 13, 8.4), (8 + side * 7.6, 18, 9.2), (8 + side * 6, 22, 10)]
            s.chain(f'horn_{side}', pts, 1.8, 'blue', taper=.3)
        for i in range(6):
            a = i * math.tau / 6 + math.pi / 6
            s.seg(f'thorn_{i}', (8 + 6 * math.cos(a), 9.2, 8 + 6 * math.sin(a)), (8 + 6.4 * math.cos(a), 11.5, 8 + 6.4 * math.sin(a)),
                  .6, 'bone', taper=.4)
        s.box('gem', (7.2, 8.6, 1.6), (8.8, 10.4, 2.3), 'eye')
    elif n == 'symbiosis_link':
        for i in range(12):
            y = .6 + i * 2.2; x = 8 + math.sin(i * .7) * 2.4; z = 8 + math.cos(i * .7) * 1.2
            if i % 2:
                s.box(f'link_{i}', (x - .9, y, z - .25), (x + .9, y + 2.2, z + .25), 'red')
            else:
                s.box(f'link_{i}', (x - .25, y, z - .9), (x + .25, y + 2.2, z + .9), 'flesh')
        s.box('end_a', (6.8, 0, 6.8), (9.2, .8, 9.2), 'blood')
    elif n in ('depravity_seed', 'malice_seed', 'blood_drop'):
        zone = 'blood' if n == 'blood_drop' else 'black'
        prof = ((2.2, 1.6), (3.6, 2.2), (4.2, 2.4), (3.8, 2.2), (2.8, 1.8), (1.6, 1.6), (.8, 1.6))
        y = 0
        for i, (w, h) in enumerate(prof):
            s.box(f'body_{i}', (8 - w, y, 8 - w * .9), (8 + w, y + h, 8 + w * .9), zone); y += h
        s.box('shine', (5.2, 5.5, 4.2), (6.2, 8, 4.5), 'pale' if n == 'blood_drop' else 'eye')
        if n != 'blood_drop':
            s.box('scar', (7.5, 2, 3.7), (8.5, 11, 4.1), 'red')
            s.box('scar_branch', (8.4, 6, 3.75), (10, 6.8, 4.05), 'red')
        if n == 'malice_seed':
            s.ring('ward', 7, 7, 'blue', thick=.35)
            for i in range(3):
                a = i * math.tau / 3
                s.seg(f'root_{i}', (8, .5, 8), (8 + 5 * math.cos(a), -1.5, 8 + 5 * math.sin(a)), .6, 'red', taper=.4)
    elif n in ('acid_pool', 'sludge_pool', 'black_sea', 'shadow_trail'):
        liquid = {'acid_pool': 'filth', 'sludge_pool': 'filth', 'black_sea': 'black', 'shadow_trail': 'liquid'}[n]
        foam = {'acid_pool': 'edge', 'sludge_pool': 'scum', 'black_sea': 'blue', 'shadow_trail': 'black'}[n]
        for i in range(8):
            z = i * 2; inset = abs(i - 3.5) ** 2 * .18
            s.box(f'liquid_{i}', (inset - .5, .05, z), (16.5 - inset, .4 + (i % 3) * .15, z + 2), liquid)
        s.ring('edge', 8, .35, foam, thick=.5, height=.35)
        for i in range(5):
            x = 3 + (i * 7 % 10); z = 3 + (i * 5 % 10)
            sz = .8 + (i % 2) * .6
            s.box(f'bubble_{i}', (x, .4, z), (x + sz, .4 + sz * .7, z + sz), foam if n != 'black_sea' else 'pale')
        if n == 'acid_pool':
            s.box('bone', (5, .3, 9), (10, 1.1, 10), 'bone')
            s.box('bone_knob', (9.6, .3, 8.4), (10.8, 1.3, 10.6), 'bone')
    elif n == 'sludge_wave':
        for i in range(10):
            x = -5 + i * 2.7; y = 5 + 3 * math.sin(i * .65)
            s.box('wave_' + str(i), (x, 0, 5), (x + 2.8, y, 10.5), 'filth')
            s.box('crest_' + str(i), (x, y - .3, 3.4), (x + 2.8, y + 1.6, 8.6), 'scum')
            if i % 2:
                s.box('lip_' + str(i), (x + .4, y - 1.4, 2.6), (x + 2.4, y + .4, 3.6), 'scum')
            s.box('drip_' + str(i), (x + 1.2, y - 2.4 - i % 3, 2.8), (x + 1.7, y - 1.2, 3.3), 'edge')
    elif n == 'corrosion_beam':
        s.box('beam', (7, 0, 7), (9, 32, 9), 'filth')
        s.box('core', (7.6, 0, 6.8), (8.4, 32, 7.2), 'edge')
        for i in range(8):
            a = i * math.tau / 4; y = i * 4
            s.box(f'spiral_{i}', (8 + 1.4 * math.cos(a) - .4, y, 8 + 1.4 * math.sin(a) - .4),
                  (8 + 1.4 * math.cos(a) + .4, y + 2.2, 8 + 1.4 * math.sin(a) + .4), 'scum')
    elif n == 'barrier_fracture':
        cracks = [((8, 8), (4, 13)), ((4, 13), (2, 12)), ((8, 8), (12, 14)), ((12, 14), (11, 16)), ((8, 8), (13, 6)),
                  ((13, 6), (15, 2)), ((8, 8), (3, 4)), ((3, 4), (4, 0.5))]
        for i, ((x0, y0), (x1, y1)) in enumerate(cracks):
            s.seg(f'crack_{i}', (x0, y0, 7.6), (x1, y1, 7.6), .7, 'edge', taper=.6)
        for i in range(5):
            a = i * 1.3; x = 8 + 5 * math.cos(a); y = 8 + 5 * math.sin(a)
            s.cube(f'shard_{i}', (x, y, 6.8 - i * .3), (1.2, 1.4, .4), 'pale')
        s.box('impact', (7, 7, 7.2), (9, 9, 8), 'scum')
    elif n == 'kingdom_tile':
        # Open lattice, so the kingdom is readable from inside and outside; thorn studs on the joints.
        for x in (0, 7.5, 15):
            s.box('rib_' + str(x), (x, 0, 7.8), (x + .5, 16, 8.2), 'black')
        for y in (0, 7.5, 15):
            s.box('cross_' + str(y), (0, y, 7.7), (16, y + .5, 8.3), 'blue')
        for i, (x, y) in enumerate(((0, 0), (7.5, 7.5), (15, 15), (0, 15), (15, 0))):
            s.box(f'stud_{i}', (x - .3, y - .3, 7.4), (x + .8, y + .8, 8.6), 'ember')
    elif n.startswith('word_'):
        key = n[5:]
        for i, ((x0, y0), (x1, y1)) in enumerate(GLYPHS[key]):
            s.seg(f'stroke_{i}', (x0, y0 + 4, 8), (x1, y1 + 4, 8), .9, 'red' if i % 3 else 'pale')
        s.ring('voice', 8, 1, 'blood', thick=.4)
        s.ring('echo', 5.5, 2.2, 'glyph', thick=.3, height=.3)
    elif n == 'danger_marker':
        s.box('stem', (7, 5, 7), (9, 15, 9), 'red')
        s.box('stem_top', (6.6, 15, 6.6), (9.4, 17, 9.4), 'red')
        s.box('dot', (7, 0, 7), (9, 2.4, 9), 'eye')
        for side in (-1, 1):
            s.seg(f'horn_{side}', (8 + side * 1.2, 16.6, 8), (8 + side * 3.4, 19.4, 8), .8, 'bone', taper=.4)
    elif n == 'shadow_afterimage':
        humanoid(model, n, 0)  # a single still pose
    else:
        radial(s, n)


def radial(s, n):
    """Floor sigils: each has its own outline, star and raised elements."""
    star = lambda name, k, step, r, zone, y=.4, rot=0.0: [
        s.seg(f'{name}_{i}', (8 + r * math.cos(rot + i * step * math.tau / k), y, 8 + r * math.sin(rot + i * step * math.tau / k)),
              (8 + r * math.cos(rot + (i + 1) * step * math.tau / k), y, 8 + r * math.sin(rot + (i + 1) * step * math.tau / k)),
              .45, zone) for i in range(k)]
    if n == 'sink_seal':
        s.ring('outer', 8, 0, 'blue'); s.ring('mid', 5.5, -.6, 'black', thick=.5); s.ring('inner', 3, -1.2, 'black', thick=.5)
        for i in range(4):
            a = i * math.tau / 4
            s.seg(f'spoke_{i}', (8 + 7.5 * math.cos(a), .3, 8 + 7.5 * math.sin(a)), (8 + 3 * math.cos(a), -1, 8 + 3 * math.sin(a)), .5, 'pale')
        s.box('drain', (6.8, -2, 6.8), (9.2, -1, 9.2), 'black')
    elif n == 'bloodlust_crown':
        s.ring('band', 7, 0, 'red', thick=.5, height=1.4)
        for i in range(8):
            a = i * math.tau / 8; h = 4 if i % 2 == 0 else 2.4
            x = 8 + 7 * math.cos(a); z = 8 + 7 * math.sin(a)
            s.seg(f'spike_{i}', (x, 1.4, z), (8 + 7.4 * math.cos(a), 1.4 + h, 8 + 7.4 * math.sin(a)), 1, 'blood', taper=.35)
        for i in range(4):
            a = i * math.tau / 4 + math.pi / 8
            s.box(f'drop_{i}', (8 + 7 * math.cos(a) - .4, -1.4, 8 + 7 * math.sin(a) - .4),
                  (8 + 7 * math.cos(a) + .4, 0, 8 + 7 * math.sin(a) + .4), 'red')
    elif n == 'ritual_sigil':
        s.ring('outer', 8, 0, 'blue'); s.ring('inner', 6.6, 0, 'glyph', thick=.3)
        star('tri', 3, 1, 6.4, 'red', rot=math.pi / 2)
        star('tri_in', 3, 1, 3.2, 'pale', rot=-math.pi / 2)
        for i in range(3):
            a = math.pi / 2 + i * math.tau / 3
            x = 8 + 6.4 * math.cos(a); z = 8 + 6.4 * math.sin(a)
            s.flame(f'candle_{i}', x, .5, z, 3.4, .6, phase=i, tongues=False)
        s.box('eye', (7.3, .4, 7.3), (8.7, 1.2, 8.7), 'eye')
    elif n == 'warning_ring':
        s.ring('outer', 8, 0, 'red', thick=.5); s.ring('inner', 6, 0, 'black')
        for i in range(8):
            a = i * math.tau / 8; ca, sa = math.cos(a), math.sin(a)
            s.seg(f'chev_a_{i}', (8 + 6.8 * ca - 1 * sa, .5, 8 + 6.8 * sa + 1 * ca), (8 + 7.8 * ca, .5, 8 + 7.8 * sa), .5, 'blue')
            s.seg(f'chev_b_{i}', (8 + 6.8 * ca + 1 * sa, .5, 8 + 6.8 * sa - 1 * ca), (8 + 7.8 * ca, .5, 8 + 7.8 * sa), .5, 'blue')
    elif n == 'mind_wave':
        for i, (r, y) in enumerate(((7.6, 0), (5.8, 1.6), (4, 3.2), (2.2, 4.8))):
            s.ring(f'wave_{i}', r, y, ('blue', 'pale', 'blue', 'eye')[i], thick=.3, height=.4)
        s.box('mind', (7, 5.6, 7), (9, 7.2, 9), 'eye')
    elif n == 'offering_sigil':
        s.ring('outer', 8, 0, 'red'); s.ring('inner', 4.6, 0, 'black')
        star('square', 4, 1, 6.5, 'pale')
        for i in range(4):
            a = i * math.tau / 4; x = 8 + 6.5 * math.cos(a); z = 8 + 6.5 * math.sin(a)
            s.box(f'bowl_{i}', (x - 1, .2, z - 1), (x + 1, 1.4, z + 1), 'bone')
            s.box(f'blood_{i}', (x - .7, 1.2, z - .7), (x + .7, 1.5, z + .7), 'blood')
        s.box('altar', (6.4, 0, 6.4), (9.6, 1.6, 9.6), 'ember')
    elif n == 'rebirth_ring':
        s.ring('outer', 8, 0, 'blue'); s.ring('inner', 5, 0, 'black')
        for i in range(8):
            a = i * math.tau / 8
            s.seg(f'rise_{i}', (8 + 6.5 * math.cos(a), .4, 8 + 6.5 * math.sin(a)),
                  (8 + 4.5 * math.cos(a + .6), 3 + i % 3 * 1.4, 8 + 4.5 * math.sin(a + .6)), .5, 'pale' if i % 2 else 'blue', taper=.4)
        s.box('heart', (7.2, .4, 7.2), (8.8, 2, 8.8), 'red')
    elif n == 'corrosion_mark':
        s.ring('outer', 7.6, 0, 'filth', thick=.6); s.ring('inner', 4.6, 0, 'black')
        for i in range(7):
            a = i * math.tau / 7 + .3; r0 = 2 + i % 2; r1 = 7 + (i % 3) * .5
            s.seg(f'crack_{i}', (8 + r0 * math.cos(a), .3, 8 + r0 * math.sin(a)), (8 + r1 * math.cos(a + .25), .3, 8 + r1 * math.sin(a + .25)),
                  .45, 'edge', taper=.5)
        for i in range(4):
            a = i * math.tau / 4 + .8
            s.box(f'drip_{i}', (8 + 7.6 * math.cos(a) - .3, -1.6, 8 + 7.6 * math.sin(a) - .3),
                  (8 + 7.6 * math.cos(a) + .3, .2, 8 + 7.6 * math.sin(a) + .3), 'scum')
    else:
        raise ValueError('Missing model: ' + n)


def generate(root):
    solid, animated = material(), material(True)
    solid.save(root); animated.save(root); records = []
    names = list(dict.fromkeys(sum(ABILITY_MODELS.values(), []) + EXTRA_MODELS))

    counts = {}
    for base in ANIMATED:
        counts[base] = {}
        for f in range(4):
            dry = Model('dry', animated, '')
            (hand(dry, f, counts[base], True) if base == 'filth_hand' else humanoid(dry, base, f, counts[base], True))

    def make(name, base, frame=0):
        m = Model('filthy_monarch/' + name, solid if base in SOLID_ONLY else animated, 'Original Filthy Monarch geometry')
        if base == 'filth_hand':
            hand(m, frame, counts[base])
        elif base in ANIMATED:
            humanoid(m, base, frame, counts[base])
        else:
            prop(m, base)
        m.save(root)
        records.append({'name': name, 'elements': len(m.elements), 'pivot_pixels': [8, 0, 8],
                        'bounds': [[min(e['from'][i] for e in m.elements) for i in range(3)],
                                   [max(e['to'][i] for e in m.elements) for i in range(3)]]})

    for n in names:
        make(n, n)
        if n in ANIMATED:
            for f in range(4):
                make(f'{n}_{f}', n, f)
    return records
