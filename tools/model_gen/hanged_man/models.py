"""Builds the Hanged Man ModelFx models (Java block-model JSON) and their atlases.

Run: python3 models.py <assets/circle_of_inevitability dir>
Rotations follow the right-hand rule, the same as the generated pale_emperor models (rib_cage):
  axis x: +Y tilts toward +Z for a positive angle;  axis z: +Y tilts toward -X for a positive angle.
"""
import json
import math
import os
import sys

from textures import CELL, GRID, atlas

NAMESPACE = 'circle_of_inevitability'
ALLOWED = (-45.0, -22.5, 0.0, 22.5, 45.0)
TEX_PX_PER_MODEL_PX = CELL / 16.0          # 2 texture pixels per model pixel


class Model:
    def __init__(self, name, credit, materials):
        self.name = name
        self.credit = credit
        self.materials = materials
        self.elements = []

    def box(self, name, frm, to, mat, rot=None, full=None):
        """rot = (axis, angle, origin). full = faces that show their whole cell (a face, a sigil)."""
        if mat not in self.materials:
            raise ValueError(f'{self.name}: material {mat} is not in its atlas')
        frm = [round(v, 4) for v in frm]
        to = [round(v, 4) for v in to]
        for a, b in zip(frm, to):
            if a > b:
                raise ValueError(f'{self.name}/{name}: from > to')
            if a < -16 or b > 32:
                raise ValueError(f'{self.name}/{name}: outside -16..32 ({frm} {to})')
        if rot is not None and rot[1] not in ALLOWED:
            raise ValueError(f'{self.name}/{name}: angle {rot[1]}')
        self.elements.append(dict(name=name, frm=frm, to=to, mat=mat, rot=rot, full=full or ()))

    def json(self, cells, tex_w, tex_h):
        out = []
        for e in self.elements:
            cx, cy = cells[e['mat']]
            (x0, y0, z0), (x1, y1, z1) = e['frm'], e['to']
            sizes = {   # (width, height) in model pixels of each face
                'north': (x1 - x0, y1 - y0), 'south': (x1 - x0, y1 - y0),
                'east': (z1 - z0, y1 - y0), 'west': (z1 - z0, y1 - y0),
                'up': (x1 - x0, z1 - z0), 'down': (x1 - x0, z1 - z0),
            }
            faces = {}
            for face, (w, h) in sizes.items():
                if face in e['full']:
                    pw, ph = CELL, CELL
                else:
                    pw = min(CELL, max(1.0, w * TEX_PX_PER_MODEL_PX))
                    ph = min(CELL, max(1.0, h * TEX_PX_PER_MODEL_PX))
                u0 = cx * CELL / tex_w * 16
                v0 = cy * CELL / tex_h * 16
                faces[face] = {
                    'uv': [round(u0, 4), round(v0, 4), round(u0 + pw / tex_w * 16, 4), round(v0 + ph / tex_h * 16, 4)],
                    'texture': '#0',
                }
            el = {'name': e['name'], 'from': e['frm'], 'to': e['to']}
            if e['rot'] is not None:
                axis, angle, origin = e['rot']
                el['rotation'] = {'angle': angle, 'axis': axis, 'origin': [round(v, 4) for v in origin]}
            el['faces'] = faces
            out.append(el)
        return out


def write(assets, models, tex_name, materials, seed):
    """Writes every model (several = the frames of one) sharing one atlas called tex_name."""
    img, cells = atlas(materials, seed)
    rows = (len(materials) + GRID - 1) // GRID
    img = img.crop((0, 0, CELL * GRID, CELL * rows))
    tex_w, tex_h = img.size
    tex_path = os.path.join(assets, 'textures/model_fx', tex_name + '.png')
    os.makedirs(os.path.dirname(tex_path), exist_ok=True)
    img.save(tex_path)
    for m in models:
        data = {
            'credit': m.credit,
            'texture_size': [tex_w, tex_h],
            'textures': {'0': NAMESPACE + ':model_fx/' + tex_name, 'particle': NAMESPACE + ':model_fx/' + tex_name},
            'elements': m.json(cells, tex_w, tex_h),
        }
        path = os.path.join(assets, 'models/model_fx', m.name + '.json')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w') as f:
            json.dump(data, f, indent=2)
    return [m.name for m in models]


def centered(cx, cy, cz, sx, sy, sz):
    return [cx - sx / 2, cy - sy / 2, cz - sz / 2], [cx + sx / 2, cy + sy / 2, cz + sz / 2]


# =====================================================================================================
# 1. The spirit pendulum (Divination)
# =====================================================================================================

def pendulum():
    mats = ['crimson_crystal', 'crystal_core', 'silver', 'dark_metal']
    m = Model('hanged_man/pendulum',
              'Circle Of Inevitability - Hanged Man spirit pendulum: a crimson crystal hanging from a silver chain, 1.4 blocks long. '
              'Front = north. Pivot 8,24,8 = the top of the chain, where it is held: swing it with pose().roll(...). Generated',
              mats)
    m.box('grip', *centered(8, 24.5, 8, 2, 1, 2), 'dark_metal')
    m.box('ring', *centered(8, 23.5, 8, 1.2, 1, 1.2), 'dark_metal')
    m.box('chain', [7.75, 9, 7.75], [8.25, 23, 8.25], 'silver')
    m.box('cap', *centered(8, 8.5, 8, 2.4, 1, 2.4), 'dark_metal')
    rot = lambda y: ('y', 45.0, [8, y, 8])
    m.box('crystal_top', *centered(8, 7.25, 8, 2.4, 1.5, 2.4), 'crimson_crystal', rot(7))
    m.box('crystal_body', *centered(8, 5.5, 8, 3.2, 2, 3.2), 'crimson_crystal', rot(5.5))
    m.box('crystal_core', *centered(8, 5.5, 8, 1.4, 3.6, 1.4), 'crystal_core')
    m.box('crystal_low', *centered(8, 3.75, 8, 2.2, 1.5, 2.2), 'crimson_crystal', rot(3.75))
    m.box('crystal_tip', *centered(8, 2.25, 8, 1.2, 1.5, 1.2), 'crimson_crystal', rot(2.25))
    m.box('crystal_point', *centered(8, 1.1, 8, 0.5, 1, 0.5), 'crystal_core', rot(1.1))
    return [m], mats


# =====================================================================================================
# 2. The whispering mouths (Whispers): 4 mouths in a ring round a head, 2 frames (shut, open)
# =====================================================================================================

def rot90(frm, to, k):
    """Turns a box k quarter turns round the vertical line through (8, 8)."""
    pts = [(frm[0], frm[2]), (to[0], to[2])]
    for _ in range(k):
        pts = [(8 - (z - 8), 8 + (x - 8)) for x, z in pts]
    xs = sorted(p[0] for p in pts)
    zs = sorted(p[1] for p in pts)
    return [xs[0], frm[1], zs[0]], [xs[1], to[1], zs[1]]


def whisper_mouths():
    mats = ['lip', 'void', 'teeth', 'mouth_dark']
    frames = []
    for f, gap in enumerate((0.0, 0.7)):
        m = Model(f'hanged_man/whisper_mouths_{f}',
                  f'Circle Of Inevitability - Hanged Man whispering mouths: four mouths in a ring 14 px across, facing out, to circle a head. '
                  f'Frame {f} of 0..1 (0 = shut, 1 = open). ModelFx: .frames(2).smooth(). Pivot 8,8,8 = the middle of the ring. Generated',
                  mats)
        for k in range(4):
            def put(name, frm, to, mat):
                a, b = rot90(frm, to, k)
                m.box(f'{name}{k}', a, b, mat)
            put('lip_up', [6, 8 + gap, 0.6], [10, 9 + gap, 1.6], 'lip')
            put('lip_down', [6, 7 - gap, 0.6], [10, 8 - gap, 1.6], 'lip')
            put('corner_l', [5.4, 7.5, 0.8], [6.1, 8.5, 1.6], 'lip')
            put('corner_r', [9.9, 7.5, 0.8], [10.6, 8.5, 1.6], 'lip')
            put('teeth_up', [6.6, 7.7 + gap, 1.0], [9.4, 8.2 + gap, 1.3], 'teeth')
            put('teeth_down', [6.6, 7.8 - gap, 1.0], [9.4, 8.3 - gap, 1.3], 'teeth')
            put('throat', [6.3, 7.1 - gap * 0.6, 1.3], [9.7, 8.9 + gap * 0.6, 1.7], 'void')
            put('cheek', [6.0, 7.0, 1.6], [10.0, 9.0, 2.0], 'mouth_dark')
        frames.append(m)
    return frames, mats


# =====================================================================================================
# 3. The shadow tendrils (Shadow Binding): 4 tendrils from a pool of shadow, 5 frames (open .. closed)
# =====================================================================================================

TENDRIL_LENGTHS = (8.0, 7.0, 6.0, 5.0)
TENDRIL_WIDTHS = (3.0, 2.5, 2.0, 1.4)
# the tilt of each segment toward the middle, per frame (0 = open, leaning out; 4 = closed over the middle)
TENDRIL_FRAMES = (
    (-22.5, -22.5, -22.5, 0.0),
    (0.0, 0.0, 0.0, 22.5),
    (0.0, 0.0, 22.5, 22.5),
    (0.0, 22.5, 22.5, 45.0),
    (0.0, 22.5, 45.0, 45.0),
)


def shadow_tendrils():
    mats = ['shadow', 'shadow_vein', 'shadow_pool', 'red_glow']
    frames = []
    for f, tilts in enumerate(TENDRIL_FRAMES):
        m = Model(f'hanged_man/shadow_tendrils_{f}',
                  f'Circle Of Inevitability - Hanged Man shadow tendrils: four tendrils rising from a pool of shadow round a 16 px middle, '
                  f'1.4 blocks tall. Frame {f} of 0..4 (0 = leaning out, 4 = curled shut over the middle). '
                  f'ModelFx: .frames(5).smooth(). Pivot 8,-16,8 = the middle of the pool. Generated',
                  mats)
        m.box('pool', [-2, -16, -2], [18, -15.6, 18], 'shadow_pool')
        # (where it starts, the way "in" points as (dx, dz), the axis it bends round, the sign of its angle)
        sides = (
            ((8, 0), (0, 1), 'x', 1),      # north side, bends toward +z
            ((8, 16), (0, -1), 'x', -1),   # south side, toward -z
            ((16, 8), (-1, 0), 'z', 1),    # east side, toward -x
            ((0, 8), (1, 0), 'z', -1),     # west side, toward +x
        )
        for s, ((bx, bz), (ix, iz), axis, sign) in enumerate(sides):
            px, py, pz = float(bx), -15.8, float(bz)
            for i, (length, width, tilt) in enumerate(zip(TENDRIL_LENGTHS, TENDRIL_WIDTHS, tilts)):
                mat = 'shadow_vein' if i % 2 == 0 else 'shadow'
                m.box(f't{s}_{i}', [px - width / 2, py, pz - width / 2], [px + width / 2, py + length, pz + width / 2],
                      mat, (axis, sign * tilt if tilt else 0.0, [px, py, pz]))
                rad = math.radians(tilt)
                px += ix * math.sin(rad) * length
                pz += iz * math.sin(rad) * length
                py += math.cos(rad) * length
            # a glowing barb at the tip, following the last segment
            m.box(f't{s}_barb', [px - 0.4, py, pz - 0.4], [px + 0.4, py + 1.4, pz + 0.4], 'red_glow',
                  (axis, sign * tilts[-1] if tilts[-1] else 0.0, [px, py, pz]))
        frames.append(m)
    return frames, mats


# =====================================================================================================
# 4. The lump of flesh (Flesh Bomb): 2 frames (resting, swollen) so it can throb
# =====================================================================================================

def flesh_lump():
    mats = ['flesh', 'vein', 'bone', 'lip']
    frames = []
    for f, swell in enumerate((0.0, 0.8)):
        m = Model(f'hanged_man/flesh_lump_{f}',
                  f'Circle Of Inevitability - Hanged Man lump of flesh: a lumpy, veined ball about 0.6 blocks wide with two bone shards. '
                  f'Frame {f} of 0..1 (1 = swollen). ModelFx: .frames(2).smooth() and swing frame to make it throb. '
                  f'Pivot 8,8,8 = its middle. Generated',
                  mats)
        g = swell * 0.5
        m.box('core', [5 - g, 5 - g, 5 - g], [11 + g, 11 + g, 11 + g], 'flesh')
        s = swell
        m.box('lump_e', [10.5 + s, 6, 6.5], [12.5 + s * 1.3, 10, 10.5], 'flesh')
        m.box('lump_w', [3.5 - s * 1.3, 7, 5.5], [5.5 - s, 11, 9.5], 'flesh')
        m.box('lump_u', [6, 10.5 + s, 6], [10, 12.5 + s * 1.3, 10], 'flesh')
        m.box('lump_d', [6.5, 3.5 - s * 1.3, 6.5], [10.5, 5.5 - s, 9.5], 'flesh')
        m.box('lump_s', [5.5, 6.5, 10.5 + s], [9.5, 10, 12 + s * 1.3], 'flesh')
        m.box('lump_n', [7, 6, 4 - s * 1.3], [11, 9.5, 5.5 - s], 'flesh')
        m.box('lump_ne', [9.8 + s * 0.7, 9.6 + s * 0.7, 4.6 - s * 0.7], [11.8 + s, 11.6 + s, 6.6 - s * 0.7], 'lip')
        m.box('lump_sw', [4.2 - s, 4.4 - s, 9.6 + s * 0.7], [6.2 - s * 0.7, 6.4 - s * 0.7, 11.6 + s], 'lip')
        m.box('vein_a', [4.6 - g, 8.6, 4.7 - g], [11.4 + g, 9.0, 5.0 - g], 'vein')
        m.box('vein_b', [11.0 + g, 4.6 - g, 7.6], [11.3 + g, 11.4 + g, 8.0], 'vein')
        m.box('vein_c', [6.6, 11.0 + g, 4.6 - g], [7.0, 11.3 + g, 11.4 + g], 'vein')
        m.box('vein_d', [4.7 - g, 6.0, 4.6 - g], [5.0 - g, 6.4, 11.4 + g], 'vein')
        m.box('shard_a', [7.4, 11.5 + s, 8.4], [8.4, 14.5 + s, 9.4], 'bone', ('z', -22.5, [7.9, 11.5 + s, 8.9]))
        m.box('shard_b', [2.6 - s, 9.2, 6.4], [5.0 - s, 10.0, 7.2], 'bone', ('x', 22.5, [5.0 - s, 9.6, 6.8]))
        frames.append(m)
    return frames, mats


# =====================================================================================================
# 5. The flesh cloak (Flesh Cloak): see-through shells round a player's body, worn
# =====================================================================================================

def flesh_cloak():
    mats = ['flesh_glass', 'vein', 'flesh']
    m = Model('hanged_man/flesh_cloak',
              'Circle Of Inevitability - Hanged Man flesh cloak: see-through veined shells round a standing player (1.95 blocks tall). '
              'Worn with modelOnTurning. Pivot 8,0,8 = between the feet. Play it .seeThrough(). Generated',
              mats)
    m.box('legs', [3.2, 0.2, 5.2], [12.8, 12.2, 10.8], 'flesh_glass')
    m.box('body', [3.0, 12.0, 4.8], [13.0, 24.4, 11.2], 'flesh_glass')
    m.box('arm_w', [-0.8, 11.6, 5.2], [3.4, 24.6, 10.8], 'flesh_glass')
    m.box('arm_e', [12.6, 11.6, 5.2], [16.8, 24.6, 10.8], 'flesh_glass')
    m.box('head', [3.2, 23.8, 3.2], [12.8, 31.6, 12.8], 'flesh_glass')
    # cords of flesh that bind it
    m.box('cord_a', [2.8, 18.0, 4.6], [13.2, 18.6, 11.4], 'vein')
    m.box('cord_b', [2.9, 7.0, 5.0], [13.1, 7.6, 11.0], 'vein')
    m.box('cord_c', [7.6, 12.0, 4.5], [8.4, 31.8, 4.9], 'vein')
    m.box('cord_d', [-1.0, 20.0, 5.0], [3.6, 20.6, 11.0], 'vein')
    m.box('cord_e', [12.4, 15.0, 5.0], [17.0, 15.6, 11.0], 'vein')
    # growths on the shoulders and back
    m.box('growth_w', [0.0, 24.2, 6.0], [3.4, 26.2, 10.0], 'flesh', ('z', 22.5, [3.4, 24.2, 8.0]))
    m.box('growth_e', [12.6, 24.2, 6.4], [15.6, 25.8, 9.6], 'flesh', ('z', -22.5, [12.6, 24.2, 8.0]))
    m.box('growth_back', [5.0, 17.0, 11.0], [9.0, 22.0, 12.6], 'flesh')
    return [m], mats


# =====================================================================================================
# 6. The black armor (Black Armor): body plates in 2 frames (flung apart, locked on) + a helm worn on the head
# =====================================================================================================

def black_armor():
    mats = ['black_steel', 'crimson_trim', 'sigil', 'dark_metal']
    # each piece: name, from, to, material, rotation, the way it is flung in frame 0 (dx, dy, dz)
    pieces = [
        ('chest', [3.4, 13.0, 5.0], [12.6, 24.6, 11.0], 'black_steel', None, (0, 2, -7)),
        ('ridge', [7.4, 14.0, 4.6], [8.6, 24.2, 5.2], 'crimson_trim', None, (0, 3, -9)),
        ('sigil', [5.0, 18.0, 4.7], [11.0, 23.0, 5.0], 'sigil', None, (0, 4, -10)),
        ('belt', [3.2, 11.4, 4.8], [12.8, 13.2, 11.2], 'crimson_trim', None, (0, -2, -6)),
        ('fauld_n', [3.6, 6.8, 4.4], [12.4, 11.6, 5.4], 'black_steel', ('x', 22.5, [8, 11.6, 5.0]), (0, -3, -8)),
        ('fauld_s', [3.6, 6.8, 10.6], [12.4, 11.6, 11.6], 'black_steel', ('x', -22.5, [8, 11.6, 11.0]), (0, -3, 8)),
        ('fauld_w', [2.6, 7.4, 5.4], [3.6, 11.6, 10.6], 'black_steel', ('z', -22.5, [3.2, 11.6, 8]), (-8, -3, 0)),
        ('fauld_e', [12.4, 7.4, 5.4], [13.4, 11.6, 10.6], 'black_steel', ('z', 22.5, [12.8, 11.6, 8]), (8, -3, 0)),
        ('pauldron_w', [-1.4, 22.0, 4.4], [4.6, 25.4, 11.6], 'black_steel', ('z', 22.5, [4.0, 23.6, 8]), (-8, 4, 0)),
        ('pauldron_w2', [-0.8, 20.4, 4.8], [3.6, 22.4, 11.2], 'crimson_trim', ('z', 22.5, [3.6, 21.4, 8]), (-9, 3, 0)),
        ('pauldron_e', [11.4, 22.0, 4.4], [17.4, 25.4, 11.6], 'black_steel', ('z', -22.5, [12.0, 23.6, 8]), (8, 4, 0)),
        ('pauldron_e2', [12.4, 20.4, 4.8], [16.8, 22.4, 11.2], 'crimson_trim', ('z', -22.5, [12.4, 21.4, 8]), (9, 3, 0)),
        ('spike_w', [0.2, 25.0, 7.4], [1.4, 28.0, 8.6], 'dark_metal', ('z', 22.5, [0.8, 25.0, 8]), (-9, 3, 0)),
        ('spike_e', [14.6, 25.0, 7.4], [15.8, 28.0, 8.6], 'dark_metal', ('z', -22.5, [15.2, 25.0, 8]), (9, 3, 0)),
        ('vambrace_w', [-0.4, 12.6, 5.6], [4.0, 17.4, 10.4], 'black_steel', None, (-7, -1, 0)),
        ('vambrace_e', [12.0, 12.6, 5.6], [16.4, 17.4, 10.4], 'black_steel', None, (7, -1, 0)),
        ('cuff_w', [-0.6, 17.0, 5.4], [4.2, 17.8, 10.6], 'crimson_trim', None, (-7, 0, 0)),
        ('cuff_e', [11.8, 17.0, 5.4], [16.6, 17.8, 10.6], 'crimson_trim', None, (7, 0, 0)),
        ('greave_w', [3.6, 0.0, 5.4], [8.0, 7.0, 10.6], 'black_steel', None, (-5, -2, -3)),
        ('greave_e', [8.0, 0.0, 5.4], [12.4, 7.0, 10.6], 'black_steel', None, (5, -2, -3)),
        ('knee_w', [3.8, 6.0, 5.0], [7.8, 8.2, 6.0], 'dark_metal', None, (-5, -1, -6)),
        ('knee_e', [8.2, 6.0, 5.0], [12.2, 8.2, 6.0], 'dark_metal', None, (5, -1, -6)),
        ('backplate', [4.0, 14.0, 11.0], [12.0, 24.0, 11.8], 'black_steel', None, (0, 2, 8)),
        ('back_ridge', [7.4, 14.0, 11.8], [8.6, 24.0, 12.4], 'crimson_trim', None, (0, 3, 9)),
    ]
    frames = []
    for f in (0, 1):
        m = Model(f'hanged_man/black_armor_{f}',
                  f'Circle Of Inevitability - Hanged Man black armor: plates round a standing player\'s body and limbs (not the head; '
                  f'see black_armor_helm). Frame {f} of 0..1 (0 = flung apart, 1 = locked on). ModelFx: .frames(2).smooth(). '
                  f'Worn with modelOnTurning. Pivot 8,0,8 = between the feet. Generated',
                  mats)
        for name, frm, to, mat, rot, (dx, dy, dz) in pieces:
            k = 1.0 if f == 0 else 0.0
            off = (dx * k, dy * k, dz * k)
            a = [frm[i] + off[i] for i in range(3)]
            b = [to[i] + off[i] for i in range(3)]
            r = None
            if rot is not None:
                r = (rot[0], rot[1], [rot[2][i] + off[i] for i in range(3)])
            m.box(name, a, b, mat, r)
        frames.append(m)

    helm = Model('hanged_man/black_armor_helm',
                 'Circle Of Inevitability - Hanged Man black knight\'s helm with a glowing crimson visor slit and a crest. Worn with '
                 'modelOnHead(..., 0, ...): it is built round a head whose top is at y=0. Pivot 8,0,8 = the top of that head. Generated',
                 mats)
    helm.box('helm', [3.4, -8.6, 3.4], [12.6, 0.8, 12.6], 'black_steel')
    helm.box('visor', [4.4, -4.8, 3.0], [11.6, -3.4, 3.5], 'crimson_trim')
    helm.box('visor_glow', [4.8, -4.5, 2.9], [11.2, -3.7, 3.1], 'sigil')
    helm.box('cheek_w', [3.0, -8.8, 3.0], [5.0, -5.4, 7.0], 'dark_metal')
    helm.box('cheek_e', [11.0, -8.8, 3.0], [13.0, -5.4, 7.0], 'dark_metal')
    helm.box('brow', [3.2, -3.4, 3.0], [12.8, -2.4, 4.0], 'dark_metal')
    helm.box('crest', [7.3, 0.6, 3.0], [8.7, 4.6, 12.0], 'crimson_trim', ('x', -22.5, [8, 0.6, 7.5]))
    helm.box('crest_base', [7.0, 0.4, 4.0], [9.0, 1.4, 12.4], 'dark_metal')
    return frames + [helm], mats


# =====================================================================================================
# 7. The shadow head (Trinity Body): a head of shadow with red eyes and wisps rising from it
# =====================================================================================================

def shadow_head():
    mats = ['shadow', 'shadow_face', 'shadow_vein', 'red_glow']
    m = Model('hanged_man/shadow_head',
              'Circle Of Inevitability - Hanged Man shadow head: a head of shadow 7 px wide with slit red eyes and a jagged grin, '
              'wisps rising off its back. Face = north. Pivot 8,0,8 = the bottom of its neck. Generated',
              mats)
    m.box('neck', [6.6, 0.0, 6.8], [9.4, 2.0, 9.6], 'shadow_vein')
    m.box('head', [4.5, 2.0, 4.5], [11.5, 9.0, 11.5], 'shadow', full=())
    m.box('face', [4.6, 2.2, 4.3], [11.4, 8.8, 4.5], 'shadow_face', full=('north',))
    m.box('jaw', [5.0, 1.4, 4.8], [11.0, 2.4, 9.0], 'shadow_vein')
    m.box('brow', [4.3, 7.2, 4.1], [11.7, 8.0, 5.2], 'shadow')
    for i, (x, h, ang) in enumerate(((5.6, 3.5, 22.5), (8.0, 5.0, 45.0), (10.4, 3.0, 22.5))):
        m.box(f'wisp{i}', [x - 0.6, 8.6, 9.0], [x + 0.6, 8.6 + h, 10.2], 'shadow_vein', ('x', ang, [x, 8.6, 9.6]))
    m.box('wisp_back', [7.2, 3.0, 11.4], [8.8, 8.0, 13.6], 'shadow', ('x', 22.5, [8, 8.0, 11.5]))
    return [m], mats


BUILDERS = {
    'pendulum': (pendulum, 11),
    'whisper_mouths': (whisper_mouths, 12),
    'shadow_tendrils': (shadow_tendrils, 13),
    'flesh_lump': (flesh_lump, 14),
    'flesh_cloak': (flesh_cloak, 15),
    'black_armor': (black_armor, 16),
    'shadow_head': (shadow_head, 17),
}


def main(assets):
    written = []
    for tex, (build, seed) in BUILDERS.items():
        models, mats = build()
        written += write(assets, models, 'hanged_man/' + tex, mats, seed)
    for w in written:
        print(w)


if __name__ == '__main__':
    main(sys.argv[1])
