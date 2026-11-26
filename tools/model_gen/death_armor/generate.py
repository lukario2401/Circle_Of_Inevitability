"""Deterministic rig, painted texture atlas, 3-D item models, recipes and tags for the Vestments of Death.

Run from any directory: python tools/model_gen/death_armor/generate.py
Same conventions as tools/model_gen/inevitability_armor: model pixels, Y down, rotations in radians.

Unlike the older sets (one flat 4x4 swatch per material), every cube here gets its own island of the atlas, laid
out like Minecraft's box UV, and each face is painted on its own: bevelled edges, bone grain, cloth folds, bandage
wraps, rivets, and decals (a skull face, the Ferryman's obol, the death sigil). Add a cube with cube(...) and it
is packed and painted automatically; give it decal='...' for one of the painted faces below.

Animated bones (looked up by name in DeathArmorModel, keep these names):
  head: hood_tip, jaw, eye_left, eye_right        body: cape_0..cape_4, lantern, heart, shroud_front, shroud_back, obol
"""
import json
import math
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'src/main/resources/assets/circle_of_inevitability'
DATA = ROOT / 'src/main/resources/data/circle_of_inevitability'
NS = 'circle_of_inevitability:'
SET = 'death'
ATLAS_W, ATLAS_H = 256, 256      # UV space, in model pixels (what the model and the rig's uv use)
DENSITY = 2                       # texels per model pixel: the PNG is ATLAS_W*DENSITY wide

# Base colors. 'soul' is the Pale Emperor's pale green (PaleEmperorFx glows 0x9CFFD2).
COLORS = {
    'cloth': (31, 28, 36),      # the black robe
    'shroud': (40, 46, 44),     # the Ferryman's grave-green shroud
    'wrap': (139, 131, 112),    # old linen bandages
    'bone': (221, 212, 186),
    'iron': (54, 56, 64),       # blackened iron
    'leather': (48, 36, 30),
    'gold': (168, 132, 58),     # tarnished
    'soul': (150, 255, 212),
    'void': (9, 9, 12),
}
COLORS['cape'] = COLORS['cloth']   # the cape: cloth, with each strip lit along its edges



def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def png(path, width, height, pixels):
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data))
    raw = b''.join(b'\0' + bytes(pixels[y*width*4:(y+1)*width*4]) for y in range(height))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
                     + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


# =====================================================================================================
# Rig
# =====================================================================================================

bones = []
cubes = []          # (bone, cube dict, material, decal) in creation order; uv filled in by pack()


def bone(name, parent='root', pivot=(0, 0, 0), rotation=(0, 0, 0)):
    b = dict(name=name, parent=parent, pivot=[round(v, 4) for v in pivot],
             rotation=[round(v, 4) for v in rotation], cubes=[])
    bones.append(b)
    return b


def cube(b, slot, origin, size, material='cloth', decal=None):
    c = dict(slot=slot, origin=[round(v, 4) for v in origin], size=[round(v, 4) for v in size], uv=[0, 0])
    b['cubes'].append(c)
    cubes.append((b, c, material, decal))
    return c


def build_rig():
    bones.clear()
    cubes.clear()
    head = bone('head')
    bone('hat', 'head')
    body = bone('body')
    arms = [bone('right_arm', pivot=(-5, 2, 0)), bone('left_arm', pivot=(5, 2, 0))]
    legs = [bone('right_leg', pivot=(-1.9, 12, 0)), bone('left_leg', pivot=(1.9, 12, 0))]
    helmet(head)
    mantle(body, arms)
    shroud(body, legs)
    treads(legs)
    return bones


def helmet(head):
    """Death Consul's Visage: a deep reaper's hood with a drooping point, a skull mask with soul-fire eyes and a
    hinged jaw, and the Pale Emperor's crown of bone spikes around a soul gem."""
    s = 'head'
    # The hood. The front rim stands proud of the mask so the face sits in shadow.
    cube(head, s, (-4.7, -9.0, -4.0), (9.4, .9, 8.9))                 # crown of the hood
    cube(head, s, (-4.7, -8.1, 4.1), (9.4, 9.6, .8), 'cloth', 'seam')  # back, falling over the nape
    for x in (-4.7, 3.9):
        cube(head, s, (x, -8.1, -3.3), (.8, 8.7, 7.4))                # sides
    cube(head, s, (-4.95, -9.25, -4.95), (9.9, 1.35, 1.7))           # brow of the rim
    for x in (-4.95, 3.95):
        cube(head, s, (x, -7.9, -4.95), (1.0, 7.6, 1.7))             # cheeks of the rim
    # The point of the hood rises off the back of the head and droops behind it.
    tip = bone('hood_tip', 'head', (0, -8.8, 2.2), (-1.05, 0, 0))
    cube(tip, s, (-2.7, -2.8, -2.1), (5.4, 2.9, 4.2))
    end = bone('hood_tip_end', 'hood_tip', (0, -2.7, .1), (-.75, 0, 0))
    cube(end, s, (-1.75, -2.5, -1.4), (3.5, 2.6, 2.8))
    point = bone('hood_tip_point', 'hood_tip_end', (0, -2.4, .1), (-.6, 0, 0))
    cube(point, s, (-.85, -2.0, -.75), (1.7, 2.1, 1.5))
    cube(point, s, (-.35, -3.0, -.3), (.7, 1.1, .6))

    # The skull mask, built bone by bone so the sockets and the nose are real holes.
    z = -4.75
    cube(head, s, (-3.95, -8.0, z), (7.9, 2.35, .8), 'bone', 'crack')   # forehead, split by a seam of soul-light
    for side in (-1, 1):
        # Brow ridges angled down toward the nose: the skull scowls.
        brow = bone('brow_' + ('right' if side < 0 else 'left'), 'head', (side * .55, -5.75, z - .35), (0, 0, side * -.16))
        cube(brow, s, (0 if side > 0 else -3.3, -.45, -.12), (3.3, .8, .6), 'bone')
    for x in (-3.95, 3.4):
        cube(head, s, (x, -5.65, z), (.55, 2.4, .8), 'bone')          # outer walls of the sockets
    for x in (-3.4, .8):
        cube(head, s, (x, -5.65, z + .05), (2.6, 2.4, .6), 'void')    # sockets
    cube(head, s, (-.8, -5.65, z), (1.6, 1.25, .8), 'bone')            # bridge of the nose
    cube(head, s, (-.4, -4.4, z + .05), (.8, .7, .6), 'void')          # nasal cavity: narrow at the top,
    cube(head, s, (-.8, -3.7, z + .05), (1.6, 1.25, .6), 'void')       # wide at the bottom
    for x in (-1.2, .8):
        cube(head, s, (x, -4.4, z), (.4, .7, .8), 'bone')
    for x, w in ((-3.95, 3.15), (.8, 3.15)):
        cube(head, s, (x, -3.25, z), (w, 1.3, .8), 'bone')            # cheekbones
    for x in (-3.95, 3.35):
        cube(head, s, (x, -3.25, z), (.6, 2.0, .8), 'bone')            # sides of the upper jaw
    cube(head, s, (-3.35, -2.45, z), (6.7, .55, .8), 'bone')           # upper jaw
    cube(head, s, (-3.35, -2.4, z + .55), (6.7, 3.2, .25), 'void')     # the dark behind the teeth
    for i in range(6):
        cube(head, s, (-2.95 + i * 1.0, -1.9, z + .1), (.72, .8, .55), 'bone')   # upper teeth
    for x, side in ((-2.1, -1), (2.1, 1)):
        eye = bone('eye_' + ('right' if side < 0 else 'left'), 'head', (x, -4.5, z - .05))
        cube(eye, s, (-.5, -.5, -.15), (1.0, 1.0, .45), 'soul')
        cube(eye, s, (-.25, -1.25, -.08), (.5, .75, .3), 'soul')       # a lick of flame above the pupil
    jaw = bone('jaw', 'head', (0, -1.1, -1.0))
    for i in range(5):
        cube(jaw, s, (-2.45 + i * 1.0, -.05, -3.65), (.72, .75, .5), 'bone')   # lower teeth
    cube(jaw, s, (-3.0, .7, -3.8), (6.0, 1.15, .85), 'bone')            # chin
    for x in (-3.95, 3.25):
        cube(jaw, s, (x, -1.5, -3.75), (.7, 3.35, 3.3), 'bone')         # the jaw's hinges up the cheeks

    # The Pale Emperor's crown: a blackened circlet outside the hood, bone spikes leaning outward.
    for origin, size in (((-5.25, -9.5, -5.25), (10.5, .9, .4)), ((-5.25, -9.5, 4.85), (10.5, .9, .4)),
                         ((-5.25, -9.5, -4.85), (.4, .9, 9.7)), ((4.85, -9.5, -4.85), (.4, .9, 9.7))):
        cube(head, s, origin, size, 'iron')
    spikes = [((0, -5.2), 4.6, (.16, 0, 0)), ((-2.4, -5.2), 3.1, (.14, 0, -.08)), ((2.4, -5.2), 3.1, (.14, 0, .08)),
              ((-4.4, -4.1), 2.3, (.1, 0, -.2)), ((4.4, -4.1), 2.3, (.1, 0, .2)),
              ((-5.2, -.6), 1.7, (0, 0, -.22)), ((5.2, -.6), 1.7, (0, 0, .22)),
              ((-3.2, 5.2), 1.5, (-.15, 0, -.1)), ((3.2, 5.2), 1.5, (-.15, 0, .1))]
    for i, ((x, zz), h, rot) in enumerate(spikes):
        spike = bone('crown_spike_%d' % i, 'head', (x, -9.4, zz), rot)
        cube(spike, s, (-.5, -h * .6, -.4), (1.0, h * .6, .8), 'bone')
        cube(spike, s, (-.3, -h, -.25), (.6, h * .4, .5), 'bone')
    cube(head, s, (-1.0, -9.85, -5.7), (2.0, 1.6, .55), 'iron')        # the gem's setting
    cube(head, s, (-.55, -9.55, -6.0), (1.1, 1.0, .4), 'soul')


def mantle(body, arms):
    """Pale Emperor's Mantle: a black robe under an open ribcage with the soul heart burning inside it, a chain
    between two clasps, a skull and a fan of bone claws on the shoulders, tattered sleeves over iron bracers and
    finger bones, a soul lantern at the hip and a cape torn into five swaying strips."""
    s = 'chest'
    cube(body, s, (-4.6, -.3, -2.65), (9.2, 12.6, 5.3))                # robe
    cube(body, s, (-4.85, -.55, 2.5), (9.7, 1.9, .7))                  # cape yoke across the shoulders

    # Ribcage. The heart sits in an iron setting where the sternum would be.
    cube(body, s, (-1.0, .35, -3.4), (2.0, 1.9, .75), 'bone')          # manubrium
    cube(body, s, (-1.55, 2.25, -3.45), (3.1, 2.95, .55), 'iron')      # heart setting
    heart = bone('heart', 'body', (0, 3.72, -3.6))
    cube(heart, s, (-1.0, -1.05, -.35), (2.0, 2.1, .5), 'soul')
    cube(body, s, (-.6, 5.2, -3.4), (1.2, 3.4, .75), 'bone')           # lower sternum
    for i, (y, length) in enumerate(((2.5, 3.3), (3.9, 3.4), (5.3, 3.4), (6.8, 2.9))):
        for side in (-1, 1):
            rib = bone('rib_%s_%d' % ('right' if side < 0 else 'left', i), 'body', (side * 1.5, y, -3.4),
                       (0, 0, side * .24))
            cube(rib, s, (0 if side > 0 else -length, -.4, -.02), (length, .8, .75), 'bone')
            wx = 4.5 if side > 0 else -4.95
            cube(body, s, (wx, y + .45, -3.0), (.45, .8, 2.6 - i * .3), 'bone')   # ribs wrapping round the sides
    # Clasps and the chain that droops between them.
    for side in (-1, 1):
        cube(body, s, (side * 3.35 - .95, -.1, -3.45), (1.9, 1.9, .65), 'iron')
        cube(body, s, (side * 3.35 - .4, .45, -3.75), (.8, .8, .4), 'soul')
    for k in range(9):
        t = k / 8
        x = -2.4 + t * 4.8
        y = .55 + math.sin(t * math.pi) * 1.0
        wide = k % 2 == 0
        cube(body, s, (x - (.32 if wide else .22), y - (.2 if wide else .3), -3.7 if wide else -3.65),
             (.64 if wide else .44, .4 if wide else .6, .3), 'iron')

    # Shoulders.
    for i, arm in enumerate(arms):
        center = -1 if i == 0 else 1
        out = -1 if i == 0 else 1                                       # the outward side of this arm
        cube(arm, s, (center - 2.55, -2.35, -2.55), (5.1, 8.5, 5.1))   # sleeve
        # Tattered hem, uneven all round.
        for origin, size in (((center - 2.55, 6.15, -2.65), (1.7, 1.9, .3)), ((center - .5, 6.15, -2.65), (1.3, 1.2, .3)),
                             ((center + 1.0, 6.15, -2.65), (1.5, 2.3, .3)), ((center - 2.0, 6.15, 2.35), (2.3, 1.5, .3)),
                             ((center + .5, 6.15, 2.35), (2.0, 2.2, .3))):
            cube(arm, s, origin, size)
        ox = center + out * 2.55 + (0 if out > 0 else -.3)
        for z0, d, h in ((-2.5, 1.6, 2.4), (-.7, 1.4, 1.4), (.9, 1.6, 2.0)):
            cube(arm, s, (ox, 6.15, z0), (.3, h, d))
        cube(arm, s, (center - 2.7, 6.6, -2.7), (5.4, 2.7, 5.4), 'iron')   # bracer
        cube(arm, s, (center + out * 2.7 + (0 if out > 0 else -.5), 6.8, -.6), (.5, 2.3, 1.2), 'bone')
        cube(arm, s, (center - 2.25, 9.15, -2.8), (4.5, .7, .5), 'bone')   # knuckle bar
        for k in range(4):
            cube(arm, s, (center - 2.05 + k * 1.15, 9.8, -2.85), (.7, 1.05, .5), 'bone')   # finger bones
        # Layered iron pauldron.
        cube(arm, s, (center - 3.0, -3.1, -3.05), (6.0, 1.35, 6.1), 'iron')
        cube(arm, s, (center + out * 1.2 - 1.9 + (0 if out > 0 else 0), -1.85, -3.15), (3.8, 1.45, 6.3), 'iron')
        cube(arm, s, (center + out * 1.9 - 1.3, -.5, -3.2), (2.6, 1.3, 6.4), 'iron')
        cube(arm, s, (center - 3.05, -3.25, -3.15), (6.1, .35, .35), 'bone')   # bone rim
        cube(arm, s, (center - 3.05, -3.25, 2.8), (6.1, .35, .35), 'bone')
        cube(arm, s, (center + out * 3.05 + (0 if out > 0 else -.35), -3.0, -2.3), (.35, 1.15, 4.6), 'iron', 'runes')
        cube(arm, s, (center + out * 1.2 - 1.95, -.75, -3.2), (3.9, .35, 6.4), 'bone')   # lames edged in bone
        cube(arm, s, (center + out * 1.9 - 1.35, .45, -3.25), (2.7, .35, 6.5), 'bone')
        cube(arm, s, (center - .45, -3.55, -.45), (.9, .45, .9), 'soul')   # a soul stud on the crest
        if out < 0:
            # Right shoulder: three curving bone claws fanned outward.
            for k, (zz, h) in enumerate(((-1.6, 2.4), (0, 3.2), (1.6, 2.2))):
                claw = bone('claw_%d' % k, arm['name'], (center - 1.6, -3.05, zz), (0, 0, -.5))
                cube(claw, s, (-.5, -h, -.5), (1.0, h, 1.0), 'bone')
                tip = bone('claw_%d_tip' % k, claw['name'], (0, -h + .2, 0), (0, 0, -.55))
                cube(tip, s, (-.32, -1.8, -.32), (.64, 1.8, .64), 'bone')
        else:
            # Left shoulder: a small skull looking forward and out.
            skull = bone('shoulder_skull', arm['name'], (center + .6, -3.1, -.4), (-.06, -.18, .1))
            cube(skull, s, (-1.9, -3.7, -1.5), (3.8, 2.1, 3.6), 'bone')            # cranium
            cube(skull, s, (-1.8, -1.6, -1.8), (3.6, 1.35, 3.2), 'bone')           # face
            cube(skull, s, (-1.95, -1.75, -1.95), (3.9, .4, .5), 'bone')           # brow
            for x in (-1.45, .35):
                cube(skull, s, (x, -1.35, -1.9), (1.1, .8, .2), 'void')            # sockets
                cube(skull, s, (x + .35, -1.15, -1.97), (.4, .4, .12), 'soul')
            cube(skull, s, (-.25, -.5, -1.9), (.5, .35, .2), 'void')               # nose
            cube(skull, s, (-1.4, -.25, -1.7), (2.8, .55, 2.4), 'bone', 'teeth')   # teeth

    # Soul lantern hanging at the front of the left hip.
    lantern = bone('lantern', 'body', (3.3, 8.9, -3.35))
    cube(lantern, s, (-.22, 0, -.22), (.44, .9, .44), 'iron')
    cube(lantern, s, (-.3, .9, -.15), (.6, .3, .3), 'iron')
    cube(lantern, s, (-.22, 1.2, -.22), (.44, .8, .44), 'iron')
    cube(lantern, s, (-.4, 1.85, -.4), (.8, .4, .8), 'iron')            # knob
    cube(lantern, s, (-1.2, 2.25, -1.2), (2.4, .5, 2.4), 'iron')        # cap
    cube(lantern, s, (-.85, 2.75, -.85), (1.7, 2.3, 1.7), 'soul')       # the flame behind glass
    for x in (-1.05, .75):
        for zz in (-1.05, .75):
            cube(lantern, s, (x, 2.75, zz), (.3, 2.3, .3), 'iron')      # posts
    cube(lantern, s, (-1.15, 5.05, -1.15), (2.3, .5, 2.3), 'iron')      # base
    cube(lantern, s, (-.3, 5.55, -.3), (.6, .55, .6), 'iron')

    # The cape, torn into five strips that sway on their own.
    for i, (x, length, yaw) in enumerate(((-3.75, 15.2, .2), (-1.9, 17.6, .07), (0, 16.4, 0), (1.9, 18.2, -.07),
                                          (3.75, 14.6, -.2))):
        strip = bone('cape_%d' % i, 'body', (x, -.2, 2.95 + (i % 2) * .3), (.08, yaw, 0))
        cube(strip, s, (-.96, 0, 0), (1.92, length, .5), 'cape')
        # Ragged ends, stepping in from alternate sides.
        left = i % 2 == 0
        cube(strip, s, (-.96 if left else -.36, length, 0), (1.32, 1.3, .5), 'cape')
        cube(strip, s, (-.96 if left else .14, length + 1.3, 0), (.82, 1.1, .5), 'cape')


def shroud(body, legs):
    """Ferryman's Shroud: an iron belt with a skull buckle and the obol on a chain, grave-green panels front and back
    marked with the death sigil, iron tassets, and legs bound in old bandages with bone kneecaps."""
    s = 'legs'
    cube(body, s, (-4.85, 9.75, -2.95), (9.7, 1.8, 5.9), 'iron')       # belt
    for x in (-3.9, -2.6, 1.85, 3.15):
        cube(body, s, (x, 10.3, -3.15), (.7, .7, .3), 'bone')           # studs
    cube(body, s, (-1.15, 9.45, -3.45), (2.3, 2.35, .6), 'bone', 'skull_face')   # buckle
    obol = bone('obol', 'body', (-3.25, 11.35, -3.2))
    cube(obol, s, (-.15, 0, -.15), (.3, .7, .3), 'gold')
    cube(obol, s, (-.2, .7, -.1), (.4, .3, .2), 'gold')
    cube(obol, s, (-1.1, 1.0, -.2), (2.2, 2.2, .35), 'gold', 'coin')

    front = bone('shroud_front', 'body', (0, 11.45, -2.95), (-.04, 0, 0))
    cube(front, s, (-2.3, 0, -.25), (4.6, 6.4, .5), 'shroud', 'sigil')
    for x, w, h in ((-2.3, 1.5, 1.4), (-.8, 1.4, .7), (.6, 1.7, 1.7)):
        cube(front, s, (x, 6.4, -.25), (w, h, .5), 'shroud')
    back = bone('shroud_back', 'body', (0, 11.45, 2.95), (.05, 0, 0))
    cube(back, s, (-2.7, 0, -.25), (5.4, 7.2, .5), 'shroud')
    for x, w, h in ((-2.7, 1.6, 1.0), (-1.1, 1.9, 1.8), (.8, 1.9, .8)):
        cube(back, s, (x, 7.2, -.25), (w, h, .5), 'shroud')
    for side in (-1, 1):
        tasset = bone('tasset_%s' % ('right' if side < 0 else 'left'), 'body', (side * 4.85, 10.9, 0), (0, 0, side * .2))
        cube(tasset, s, (0 if side > 0 else -.55, 0, -2.3), (.55, 3.3, 4.6), 'iron')
        cube(tasset, s, (0 if side > 0 else -.6, 3.0, -2.35), (.6, .4, 4.7), 'bone')

    for i, leg in enumerate(legs):
        out = -1 if i == 0 else 1
        cube(leg, s, (-2.3, -.1, -2.3), (4.6, 8.6, 4.6), 'wrap')        # bandaged legs
        cube(leg, s, (-2.15, .5, -2.6), (4.3, 3.3, .45), 'iron')       # thigh plate
        cube(leg, s, (-1.1, 4.55, -2.8), (2.2, 2.0, .6), 'bone')       # kneecap
        cube(leg, s, (-.7, 4.15, -2.65), (1.4, .5, .5), 'bone')
        cube(leg, s, (out * 2.3 + (0 if out > 0 else -.4), 1.0, -1.2), (.4, 3.0, 2.4), 'bone')   # outer thigh bone


def treads(legs):
    """Gravedigger's Treads: iron boots caked with grave dirt, tattered cuffs, bone shin guards, clawed toes and
    a heel spur."""
    s = 'feet'
    for leg in legs:
        cube(leg, s, (-2.6, 7.55, -2.6), (5.2, 1.35, 5.2))               # cuff
        for x, w, h in ((-2.6, 1.4, .9), (-.3, 1.2, 1.3), (1.4, 1.2, .7)):
            cube(leg, s, (x, 8.9, -2.7), (w, h, .3))
        cube(leg, s, (-2.5, 8.9, -2.55), (5.0, 2.8, 5.1), 'iron')        # shaft
        cube(leg, s, (-2.6, 10.6, -3.95), (5.2, 1.5, 6.55), 'iron')      # foot
        cube(leg, s, (-2.7, 12.0, -4.15), (5.4, .5, 6.95), 'leather')    # sole, caked with dirt
        cube(leg, s, (-2.68, 10.05, -2.68), (5.36, .55, 5.36), 'leather')   # ankle strap
        cube(leg, s, (-.5, 9.95, -2.95), (1.0, .75, .3), 'gold')
        cube(leg, s, (-1.3, 8.25, -2.95), (2.6, 1.75, .5), 'bone')       # shin guard
        for k, x in enumerate((-1.6, 0, 1.6)):
            claw = bone('toe_%s_%d' % (leg['name'], k), leg['name'], (x, 11.35, -3.85), (.32, 0, 0))
            cube(claw, s, (-.35, -.35, -1.6), (.7, .7, 1.6), 'bone')
            cube(claw, s, (-.2, -.2, -2.4), (.4, .4, .8), 'bone')
        spur = bone('spur_%s' % leg['name'], leg['name'], (0, 11.0, 2.5), (.55, 0, 0))
        cube(spur, s, (-.3, -.3, 0), (.6, .6, 1.6), 'bone')
        cube(spur, s, (-.15, -.15, 1.6), (.3, .3, .8), 'bone')


# =====================================================================================================
# Atlas: box-UV islands, packed in shelves, each face painted
# =====================================================================================================

def footprint(size):
    w, h, d = size
    return math.ceil(2 * (w + d)) + 1, math.ceil(d + h) + 1        # +1: a pixel of padding against bleeding


def pack():
    order = sorted(range(len(cubes)), key=lambda i: (-footprint(cubes[i][1]['size'])[1], -footprint(cubes[i][1]['size'])[0], i))
    x = y = shelf = 0
    for i in order:
        fw, fh = footprint(cubes[i][1]['size'])
        if x + fw > ATLAS_W:
            x, y, shelf = 0, y + shelf, 0
        if y + fh > ATLAS_H:
            raise SystemExit('Death armor atlas is full: enlarge ATLAS_H')
        cubes[i][1]['uv'] = [x, y]
        x += fw
        shelf = max(shelf, fh)


def faces(c):
    """Box-UV rectangles (u0, v0, u1, v1) of each face, as ModelPart.Cube lays them out."""
    u, v = c['uv']
    w, h, d = c['size']
    return {'top': (u + d, v, u + d + w, v + d), 'bottom': (u + d + w, v, u + d + 2 * w, v + d),
            'west': (u, v + d, u + d, v + d + h), 'north': (u + d, v + d, u + d + w, v + d + h),
            'east': (u + d + w, v + d, u + 2 * d + w, v + d + h), 'south': (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}


def noise(x, y, seed=0):
    n = (x * 374761393 + y * 668265263 + seed * 982451653) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def shade(color, k):
    return tuple(max(0, min(255, int(round(c * k)))) for c in color)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[k] + (b[k] - a[k]) * t)) for k in range(3))


T = 1 / DENSITY                   # one texel, in model pixels


def paint(material, face, x, y, w, h, gx, gy):
    """Color of one texel. (x, y): its centre inside a face of w by h model pixels; (gx, gy): its atlas texel."""
    base = COLORS[material]
    n = noise(gx, gy, len(material)) - .5
    vertical = face not in ('top', 'bottom')
    left, right, upper, lower = x < T, x > w - T, y < T, y > h - T
    edged = w >= 1.5 and h >= 1.5
    if material in ('cloth', 'shroud', 'cape'):
        fold = math.sin(gx * .62 + math.sin(gy * .09) * 1.8)             # long vertical folds
        k = 1 + fold * .12 + n * .14
        if vertical:
            k *= 1.12 - .32 * min(1, y / max(1, h))                     # darker toward the hem
            if y > h - 2 * T and h > 1.2:
                k *= .55 if noise(gx // 2, 7, 3) > .5 else 1.4           # frayed threads at the hem
        if material == 'cape' and vertical:
            if left:
                k *= 1.75                                                 # each strip catches the light on one edge
            elif x < 2 * T:
                k *= 1.3
            elif right:
                k *= .5
        color = shade(base, k)
        if material == 'shroud' and noise(gx // 3, gy // 3, 11) > .9:
            color = mix(color, (74, 64, 46), .45)                        # grave stains
        return color
    if material == 'wrap':
        band = int(gy + gx * .5) % 6                                     # windings, slanting round the leg
        k = .66 if band == 0 else (1.1 if band in (2, 3) else .97)
        color = shade(base, k + n * .16)
        if noise(gx // 5, gy // 4, 5) > .78:
            color = mix(color, (98, 80, 58), .35)                        # old stains
        return color
    if material == 'bone':
        k = 1 + n * .1 + math.sin(gy * .85 + gx * .12) * .035            # growth lines
        if edged:
            if upper or left:
                k *= 1.1
            elif lower or right:
                k *= .76
            elif y > h - 2 * T or x > w - 2 * T:
                k *= .92
        elif lower:
            k *= .86
        color = shade(base, k)
        if noise(gx, gy, 9) > .97:
            color = mix(color, (105, 92, 66), .65)                       # pits
        return color
    if material in ('iron', 'leather'):
        streak = noise(0, gy, 21) - .5                                   # brushed horizontally
        k = 1 + n * .08 + streak * .18
        if edged:
            if upper:
                k *= 1.6
            elif lower:
                k *= .6
            elif left:
                k *= 1.25
            elif right:
                k *= .78
        color = shade(base, k)
        if material == 'iron' and w >= 3.5 and h >= 2 and vertical:
            cx = min(x, w - x)
            cy = min(y, h - y)
            if abs(cx - .75) < T * .9 and abs(cy - .75) < T * .9:
                color = shade(base, 2.3)                                 # rivets in the corners
        if material == 'leather' and face == 'bottom':
            color = mix(color, (78, 58, 40), .55 + n)                    # grave dirt on the sole
        return color
    if material == 'gold':
        k = 1 + n * .14
        if w >= 1.2 and h >= 1.2:
            k *= 1.35 if upper or left else (.68 if lower or right else 1)
        color = shade(base, k)
        if noise(gx, gy, 2) > .86:
            color = mix(color, (72, 96, 74), .45)                        # verdigris
        return color
    if material == 'soul':
        cx, cy = x / max(w, T) - .5, y / max(h, T) - .5
        r = min(1, math.sqrt(cx * cx + cy * cy) * 2)
        return mix((238, 255, 248), (40, 184, 150), r ** 1.2)
    # void: almost black, the faintest teal deep inside
    cx, cy = x / max(w, T) - .5, y / max(h, T) - .5
    return mix((20, 38, 36), base, min(1, math.sqrt(cx * cx + cy * cy) * 2.2))


SOUL_LINE = (170, 255, 222)


def decal(name, face, x, y, w, h, gx, gy):
    """Painted detail over the material, or None. (x, y) as in paint()."""
    if name == 'seam':
        # A stitched seam down the middle of the hood's back, and a frayed hem.
        if face == 'south' and abs(x - w / 2) < T * .9:
            return shade(COLORS['cloth'], .55) if int(y / T) % 3 else shade(COLORS['bone'], .45)
        return None
    if name == 'runes':
        # Small engraved notches along an iron edge.
        return mix(COLORS['iron'], SOUL_LINE, .5) if face in ('west', 'east') and int(y / T) % 3 == 1 \
            and int(x / T) % 4 == 1 else None
    if face != 'north':
        return None
    if name == 'crack':
        # A jagged split down the forehead, soul-light seeping out of it.
        path = w * .62 + math.sin(y * 2.4) * .45 + (y - h / 2) * .25
        if abs(x - path) < T * .6 and y > h * .15:
            return SOUL_LINE
        if abs(x - path) < T * 1.6 and y > h * .15:
            return shade(COLORS['bone'], .55)
        return None
    if name == 'skull_face':
        fx, fy = x / w, y / h
        for cx in (.3, .7):
            if (fx - cx) ** 2 + (fy - .38) ** 2 < .028:
                return SOUL_LINE if (fx - cx) ** 2 + (fy - .38) ** 2 < .006 else COLORS['void']
        if abs(fx - .5) < .08 and .55 < fy < .7:
            return COLORS['void']
        if fy > .8:
            return COLORS['void'] if int(x / T) % 2 else None
        return None
    if name == 'teeth':
        if y > h * .45:
            return shade(COLORS['void'], 1) if int(x / T) % 3 == 2 else None
        return None
    if name == 'coin':
        fx, fy = x / w - .5, y / h - .5
        r = math.sqrt(fx * fx + fy * fy)
        if r > .5:
            return COLORS['void']
        if r > .38:
            return shade(COLORS['gold'], .7 if fx + fy > 0 else 1.3)
        if abs(fx) < .1 and abs(fy) < .3 or abs(fy + .12) < .08 and abs(fx) < .25:
            return shade(COLORS['gold'], .55)                            # the ferryman's mark
        return None
    if name == 'sigil':
        # The death rune (Yr): a stave with two branches falling from it, glowing faintly round its strokes.
        cx = w / 2
        top, mid, bottom = h * .12, h * .42, h * .86
        branch = 0 if not mid <= y <= h * .78 else (y - mid) * .85     # how far a branch has spread at this height
        d = min(abs(x - cx) if top <= y <= bottom else 9,
                abs(abs(x - cx) - branch) if mid <= y <= h * .78 else 9,
                math.sqrt((x - cx) ** 2 + (y - top) ** 2))
        if d < T * .65:
            return SOUL_LINE
        if d < T * 1.7:
            return mix(COLORS['shroud'], SOUL_LINE, .2)
        return None
    return None


def atlas():
    width, height = ATLAS_W * DENSITY, ATLAS_H * DENSITY
    pixels = [0] * (width * height * 4)
    for b, c, material, dec in cubes:
        for face, (u0, v0, u1, v1) in faces(c).items():
            w, h = max(u1 - u0, T), max(v1 - v0, T)
            for gy in range(int(math.floor(v0 * DENSITY)), max(int(math.ceil(v1 * DENSITY)), int(math.floor(v0 * DENSITY)) + 1)):
                for gx in range(int(math.floor(u0 * DENSITY)), max(int(math.ceil(u1 * DENSITY)), int(math.floor(u0 * DENSITY)) + 1)):
                    x = min(max((gx + .5) * T - u0, 0), w)
                    y = min(max((gy + .5) * T - v0, 0), h)
                    color = None if dec is None else decal(dec, face, x, y, w, h, gx, gy)
                    if color is None:
                        color = paint(material, face, x, y, w, h, gx, gy)
                    i = (gy * width + gx) * 4
                    pixels[i:i + 4] = [*color, 255]
    return pixels


# =====================================================================================================
# Output
# =====================================================================================================

def generate():
    build_rig()
    pack()
    write(ASSETS/f'armor/{SET}.rig.json', dict(texture_size=[ATLAS_W, ATLAS_H], bones=bones))
    pixels = atlas()
    for layer in ('humanoid', 'humanoid_leggings', 'humanoid_baby'):
        png(ASSETS/f'textures/entity/equipment/{layer}/{SET}.png', ATLAS_W * DENSITY, ATLAS_H * DENSITY, pixels)
    # The block/item atlas only discovers item textures; worn equipment uses direct texture lookup.
    png(ASSETS/f'textures/item/{SET}_armor.png', ATLAS_W * DENSITY, ATLAS_H * DENSITY, pixels)
    write(ASSETS/f'equipment/{SET}.json', {'layers': {l: [{'texture': NS+SET}]
          for l in ('humanoid', 'humanoid_leggings', 'humanoid_baby')}})

    # Volumetric inventory/hand models from the same rig, each face mapped to its own painted island. Item
    # elements cannot nest rotations, so rotated bones contribute their cube centres through the full hierarchy.
    positions = {b['name']: b for b in bones}

    def world(p, b):
        x, y, z = p
        rx, ry, rz = b['rotation']
        y, z = y*math.cos(rx)-z*math.sin(rx), y*math.sin(rx)+z*math.cos(rx)
        x, z = x*math.cos(ry)+z*math.sin(ry), -x*math.sin(ry)+z*math.cos(ry)
        x, y = x*math.cos(rz)-y*math.sin(rz), x*math.sin(rz)+y*math.cos(rz)
        p = [x+b['pivot'][0], y+b['pivot'][1], z+b['pivot'][2]]
        return world(p, positions[b['parent']]) if b['parent'] != 'root' else p
    su, sv = 16 / ATLAS_W, 16 / ATLAS_H
    item_face = {'north': 'north', 'south': 'south', 'west': 'west', 'east': 'east', 'top': 'up', 'bottom': 'down'}
    names = {'head': 'helmet', 'chest': 'chestplate', 'legs': 'leggings', 'feet': 'boots'}
    tags = {'head': 'head_armor', 'chest': 'chest_armor', 'legs': 'leg_armor', 'feet': 'foot_armor'}
    for slot, name in names.items():
        shapes = []
        for b in bones:
            for c in b['cubes']:
                if c['slot'] != slot:
                    continue
                s = c['size']
                centre = world([c['origin'][k]+s[k]/2 for k in range(3)], b)
                shapes.append(([centre[k]-s[k]/2 for k in range(3)], s, c))
        lo = [min(p[k] for p, s, c in shapes) for k in range(3)]
        hi = [max(p[k]+s[k] for p, s, c in shapes) for k in range(3)]
        scale = 13/max(hi[k]-lo[k] for k in range(3))
        center = [(lo[k]+hi[k])/2 for k in range(3)]
        elements = []
        for p, s, c in shapes:
            a = [8+(p[0]-center[0])*scale, 8-(p[1]+s[1]-center[1])*scale, 8+(p[2]-center[2])*scale]
            b = [a[k]+s[k]*scale for k in range(3)]
            elements.append({'from': [round(v, 4) for v in a], 'to': [round(v, 4) for v in b],
                             'faces': {item_face[f]: {'texture': '#armor', 'uv': [round(u0*su, 4), round(v0*sv, 4),
                                                                                   round(u1*su, 4), round(v1*sv, 4)]}
                                       for f, (u0, v0, u1, v1) in faces(c).items()}})
        display = {'gui': {'rotation': [15, 210, 0], 'translation': [0, 0, 0], 'scale': [.9, .9, .9]},
                   'ground': {'translation': [0, 3, 0], 'scale': [.45, .45, .45]},
                   'fixed': {'rotation': [0, 180, 0], 'scale': [.8, .8, .8]},
                   'thirdperson_righthand': {'rotation': [75, 45, 0], 'scale': [.55, .55, .55]},
                   'firstperson_righthand': {'rotation': [0, 200, 0], 'translation': [1, 1, 0], 'scale': [.65, .65, .65]}}
        item = f'{SET}_{name}'
        write(ASSETS/f'models/item/{item}.json', {'textures': {'armor': NS+f'item/{SET}_armor', 'particle': NS+f'item/{SET}_armor'},
                                                  'elements': elements, 'display': display})
        write(ASSETS/f'items/{item}.json', {'model': {'type': 'minecraft:model', 'model': NS+'item/'+item}})
        write(DATA/f'recipe/{item}.json', {'type': 'minecraft:smithing_transform',
              'template': 'minecraft:netherite_upgrade_smithing_template', 'base': 'minecraft:netherite_'+name,
              'addition': NS+f'{SET}_core', 'result': {'id': NS+item}})
        path = ROOT/f'src/main/resources/data/minecraft/tags/item/{tags[slot]}.json'
        data = json.loads(path.read_text()) if path.exists() else {'replace': False, 'values': []}
        if NS+item not in data['values']:
            data['values'].append(NS+item)
        write(path, data)
        unlock(item, NS+f'{SET}_core')
    write(DATA/f'recipe/{SET}_core.json', {'type': 'minecraft:crafting_shaped', 'category': 'equipment',
          'pattern': ['BSB', 'ENE', 'BSB'], 'key': {'B': 'minecraft:bone_block', 'S': 'minecraft:wither_skeleton_skull',
          'E': 'minecraft:echo_shard', 'N': 'minecraft:nether_star'}, 'result': {'id': NS+f'{SET}_core', 'count': 4}})
    unlock(f'{SET}_core', 'minecraft:wither_skeleton_skull')
    write(DATA/f'tags/item/repairs_{SET}_armor.json', {'replace': False, 'values': ['minecraft:bone_block']})
    write(ASSETS/f'models/item/{SET}_core.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': NS+f'item/{SET}_core'}})
    write(ASSETS/f'items/{SET}_core.json', {'model': {'type': 'minecraft:model', 'model': NS+f'item/{SET}_core'}})
    png(ASSETS/f'textures/item/{SET}_core.png', 32, 32, obol_icon())
    damage_types()
    lang()
    print('Generated Death armor rig (%d cubes), painted atlas, 3-D items, recipes and tags.' % len(cubes))


def obol_icon():
    """The Ferryman's Obol: a worn gold coin with a skull struck on it and a pale soul-light about its rim."""
    gold, dark, light = COLORS['gold'], shade(COLORS['gold'], .55), shade(COLORS['gold'], 1.35)
    pixels = []
    for y in range(32):
        for x in range(32):
            dx, dy = x - 15.5, y - 15.5
            r = math.sqrt(dx * dx + dy * dy)
            if r > 13.5:
                glow = max(0, 1 - (r - 13.5) / 2.2)
                pixels.extend([150, 255, 212, int(150 * glow)] if glow > 0 else [0, 0, 0, 0])
                continue
            if r > 12.2:
                c = dark if (dx + dy) > 0 else light
            elif r > 11.2:
                c = shade(gold, .85)
            else:
                c = shade(gold, .88 + (noise(x, y, 1) - .5) * .14 - dy * .01)
                # A struck skull: cranium and cheeks raised and lit from the upper left, outlined, with sockets,
                # a nose and a row of teeth.
                def inside(px, py):
                    sx, sy = (px - 15.5) / 7.2, (py - 14.0) / 6.6
                    return sx * sx + sy * sy < 1 and py < 18.5 or 18.5 <= py < 22 and abs(px - 15.5) < 4.6
                if inside(x, y):
                    c = shade(gold, 1.38 - (dx + dy) * .015)
                    if not inside(x + 1, y + 1):
                        c = shade(gold, .62)                              # its shadowed edge
                    if ((abs(dx) - 3.1) ** 2 + (dy + .2) ** 2) < 4.2:
                        c = shade(gold, .42)                              # sockets
                    if abs(dx) < 1.2 and 2.5 < dy < 4.5 and abs(dx) < (dy - 2) * .8:
                        c = shade(gold, .42)                              # nose
                    if 18.5 <= y < 22 and (int(x) % 2 == 0 or y < 19.5):
                        c = shade(gold, .5)                               # teeth
                elif inside(x - 1, y - 1):
                    c = shade(gold, .55)                                  # cast shadow
                if noise(x, y, 6) > .95:
                    c = mix(c, (72, 96, 74), .5)
            pixels.extend([*c, 255])
    return pixels


def damage_types():
    """'death_toll' takes an armor's unfed wearer; 'death_curse' is the armor's own bite, never lethal by itself."""
    for name, bypass in (('death_toll', True), ('death_curse', False)):
        write(DATA/f'damage_type/{name}.json', {'message_id': f'circle_of_inevitability.{name}', 'exhaustion': 0.0,
                                                'scaling': 'never'})
    for tag, names in (('bypasses_armor', ['death_toll', 'death_curse']), ('bypasses_enchantments', ['death_toll', 'death_curse']),
                       ('bypasses_resistance', ['death_toll']), ('bypasses_effects', ['death_toll']),
                       ('bypasses_invulnerability', ['death_toll']), ('bypasses_shield', ['death_toll', 'death_curse']),
                       ('no_knockback', ['death_toll', 'death_curse'])):
        path = ROOT/f'src/main/resources/data/minecraft/tags/damage_type/{tag}.json'
        data = json.loads(path.read_text()) if path.exists() else {'replace': False, 'values': []}
        for name in names:
            if NS+name not in data['values']:
                data['values'].append(NS+name)
        write(path, data)


def lang():
    lang_path = ASSETS/'lang/en_us.json'
    entries = json.loads(lang_path.read_text(encoding='utf-8'))
    for item, name in [('helmet', "Death Consul's Visage"), ('chestplate', "Pale Emperor's Mantle"),
                       ('leggings', "Ferryman's Shroud"), ('boots', "Gravedigger's Treads")]:
        entries[f'item.circle_of_inevitability.{SET}_{item}'] = name
    entries[f'item.circle_of_inevitability.{SET}_core'] = "Ferryman's Obol"
    t = f'tooltip.circle_of_inevitability.{SET}.'
    entries[t+'power'] = "Reaping: nearby hostiles are struck and marked again and again, the closest the most. Past 6 marks the dead drag them underground."
    entries[t+'set'] = 'Cursed: endless hunger and sudden pains. It must be fed a kill every 5 minutes, or it takes you instead.'
    entries[t+'full_set'] = 'Full set: a Spirit Guide fights at your side and reaps marks, but your life ebbs away until you take a piece off. Repairs with Bone Blocks.'
    m = f'message.circle_of_inevitability.{SET}.'
    entries[m+'hungry'] = 'The Vestments of Death hunger. Feed them a kill within %s.'
    entries[m+'starving'] = 'The Vestments of Death are starving: %s left.'
    entries[m+'fed'] = 'The Vestments of Death are fed.'
    entries[m+'claimed'] = 'The underworld claims %s.'
    entries[m+'ebbing'] = 'Your life ebbs into the Vestments of Death.'
    entries[m+'toll_hud'] = 'Toll'
    entries[f'death.attack.circle_of_inevitability.death_toll'] = '%1$s was taken by their own Vestments of Death'
    entries[f'death.attack.circle_of_inevitability.death_toll.player'] = '%1$s was taken by their own Vestments of Death'
    entries[f'death.attack.circle_of_inevitability.death_curse'] = '%1$s was consumed by the Vestments of Death'
    entries[f'death.attack.circle_of_inevitability.death_curse.player'] = '%1$s was consumed by the Vestments of Death'
    write(lang_path, entries)


def unlock(item, ingredient):
    write(DATA/f'advancement/recipes/combat/{item}.json', {'parent': 'minecraft:recipes/root',
          'criteria': {'has_material': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': ingredient}]}},
                       'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked', 'conditions': {'recipe': NS+item}}},
          'requirements': [['has_material', 'has_the_recipe']], 'rewards': {'recipes': [NS+item]}})


if __name__ == '__main__':
    generate()
