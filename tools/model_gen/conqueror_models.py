"""
Makes the Conqueror models and their textures (models/model_fx/conqueror/*, textures/model_fx/conqueror/*).

    python3 tools/model_gen/conqueror_models.py                 # writes into src/main/resources/assets/circle_of_inevitability
    python3 tools/model_gen/conqueror_models.py some/other/root

Change a number, run it again, press F3+T in game. Front = north (-Z), up = +Y, 16 px = 1 block.
Every model here uses Blockbench's own turns (-45..45 in 22.5 steps) except the shut bear trap (90),
which Blockbench may show at 45 but the mod draws as written.
The fire raven (conqueror/fire_raven) was made the same way before this script; it is not rewritten here.
"""
import math, os, random, sys

from PIL import ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from modelkit import Texture, Model, lerp, ramp, rotate2

ROOT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "../../src/main/resources/assets/circle_of_inevitability")

R = random.Random(26)

# ---------------- palette: war, blood, iron, fire ----------------
BLACK = (14, 10, 10, 255)
IRON_DARK = (42, 40, 44, 255)
IRON = (86, 84, 90, 255)
STEEL = (150, 152, 160, 255)
STEEL_HI = (214, 218, 226, 255)
RUST = (112, 54, 30, 255)
BLOOD_DARK = (64, 4, 6, 255)
BLOOD = (138, 10, 14, 255)
BLOOD_HI = (214, 34, 30, 255)
GOLD_DARK = (120, 82, 24, 255)
GOLD = (206, 158, 58, 255)
WOOD_DARK = (46, 28, 18, 255)
WOOD = (92, 58, 34, 255)
WOOD_HI = (132, 88, 52, 255)
ROPE = (176, 148, 98, 255)
EMBER = (232, 92, 22, 255)
ORANGE = (255, 146, 34, 255)
YELLOW = (255, 214, 92, 255)
WHITE_HOT = (255, 248, 222, 255)
CLEAR = (0, 0, 0, 0)


def noisy(c, amount, rng=R):
    n = rng.uniform(-amount, amount)
    return tuple(max(0, min(255, int(c[i] + n * 255))) if i < 3 else c[3] for i in range(4))


def metal(base, hi, rust=0.0, rivets=0):
    def paint(tx, ty, x, y, f):
        c = lerp(base, hi, 0.25 + 0.5 * (1 - ty) * (0.6 + 0.4 * math.sin(tx * 3.1)))
        if rust and R.random() < rust:
            c = lerp(c, RUST, 0.6)
        if rivets and (x % rivets == 1 and y % rivets == 1):
            c = STEEL_HI
        return noisy(c, 0.04)
    return paint


def solid(c, amount=0.03):
    return lambda tx, ty, x, y, f: noisy(c, amount)


def gradient(stops, along="y", amount=0.03):
    def paint(tx, ty, x, y, f):
        return noisy(ramp(stops, ty if along == "y" else tx), amount)
    return paint


def flame(frame_shift=True, base=(BLOOD, EMBER, ORANGE, YELLOW, WHITE_HOT), alpha_edge=True):
    """Flames licking upward (ty = 0 is the top of the zone): hot at the bottom, flickering per frame."""
    def paint(tx, ty, x, y, f):
        wave = 0.5 + 0.5 * math.sin((tx * 9 + f * 1.7) + ty * 6)
        heat = (1 - ty) * 0.15 + ty * 0.85
        heat = max(0.0, min(1.0, heat * (0.7 + 0.45 * wave)))
        c = ramp([(0.0, base[0]), (0.35, base[1]), (0.6, base[2]), (0.85, base[3]), (1.0, base[4])], heat)
        a = 255
        if alpha_edge:
            edge = min(tx, 1 - tx)
            a = int(255 * max(0.25, min(1.0, edge * 6 + heat * 0.6)))
        return (c[0], c[1], c[2], a)
    return paint


# =====================================================================================================
# Hunter's Trap: a steel bear trap. Frames: 0 = jaws open flat, 1 = half shut, 2 = snapped shut.
# =====================================================================================================
def bear_trap():
    tex = Texture("conqueror/bear_trap", 64, 64)
    tex.zone("steel", 0, 0, 8, 8, metal(IRON, STEEL, rust=0.12))
    tex.zone("plate", 8, 0, 16, 8, metal(IRON_DARK, IRON, rust=0.08, rivets=6))

    def teeth(tx, ty, x, y, f):           # bright steel, the points (top) red with old blood
        c = lerp(STEEL_HI, STEEL, ty)
        if ty < 0.45:
            c = lerp(BLOOD_HI, c, ty / 0.45 * 0.8)
        return noisy(c, 0.03)
    tex.zone("teeth", 0, 8, 4, 12, teeth)
    tex.zone("chain", 4, 8, 8, 12, metal(BLACK, IRON_DARK, rivets=3))
    tex.zone("spring", 8, 8, 16, 12, metal(IRON_DARK, STEEL, rust=0.2))
    tex.save(ROOT)

    hinge_y, hinge_z = 1.5, 8.0
    north_teeth = [2.6, 4.8, 7.0, 9.2, 11.4]
    south_teeth = [3.7, 5.9, 8.1, 10.3, 12.5]

    def jaw(m, side, angle):
        """side -1 = north jaw (lies toward -Z), +1 = south. angle = how far it has turned up (0..90)."""
        sign = -side                      # north turns +angle around X, south -angle
        origin = (8, hinge_y, hinge_z)
        far = hinge_z + side * 7          # its outer bar
        z_lo, z_hi = sorted([hinge_z + side * 0.5, far])
        bar_lo, bar_hi = sorted([far, far - side * 1.0])
        parts = [
            ("arm_a", (2.0, 1.0, z_lo), (3.0, 2.0, z_hi), "steel"),
            ("arm_b", (13.0, 1.0, z_lo), (14.0, 2.0, z_hi), "steel"),
            ("bar", (2.0, 1.0, bar_lo), (14.0, 2.0, bar_hi), "steel"),
        ]
        for i, tx in enumerate(north_teeth if side < 0 else south_teeth):
            tz = far - side * 0.5
            parts.append(("tooth%d" % i, (tx - 0.45, 2.0, tz - 0.3), (tx + 0.45, 3.6, tz + 0.3), "teeth"))
        name = "north" if side < 0 else "south"
        for part, a, b, zone in parts:
            m.cube("jaw_%s_%s" % (name, part), a, b, zone,
                   rot=(sign * angle, "x", origin) if angle else None)

    for frame, angle in enumerate([0.0, 45.0, 90.0]):
        m = Model("conqueror/bear_trap", tex,
                  "a steel bear trap for the Conqueror's Hunter's Trap, 1 block wide, its teeth red with old blood. "
                  "Frame %d of 0..2 (0 = open flat, 2 = snapped shut). ModelFx.of(\"conqueror/bear_trap\").frames(3).smooth(). "
                  "Pivot 8,0,8 = the middle of its base." % frame)
        m.cube("pan", (5, 0, 5), (11, 0.6, 11), "plate")
        m.cube("axle", (1, 1.0, 7.5), (15, 2.0, 8.5), "steel")
        m.cube("spring_east", (13.5, 0, 6), (16, 0.9, 10), "spring")
        m.cube("spring_west", (0, 0, 6), (2.5, 0.9, 10), "spring")
        m.cube("chain_a", (7.6, 0, 11), (8.4, 0.6, 13.5), "chain")
        m.cube("chain_b", (7.3, 0, 13.5), (8.7, 0.6, 16), "chain")
        jaw(m, -1, angle)
        jaw(m, +1, angle)
        m.save(ROOT, "_%d" % frame)


# =====================================================================================================
# Explosive Charge: a powder keg with a burning fuse.
# =====================================================================================================
def powder_keg():
    tex = Texture("conqueror/powder_keg", 64, 64)

    def staves(tx, ty, x, y, f):
        c = lerp(WOOD, WOOD_HI, 0.5 + 0.5 * math.sin(tx * 20))
        if x % 4 == 0:
            c = WOOD_DARK
        return noisy(c, 0.05)
    tex.zone("wood", 0, 0, 8, 8, staves)

    def marked(tx, ty, x, y, f):          # the front stave: a red flame mark
        c = staves(tx, ty, x, y, f)
        dx, dy = tx - 0.5, ty - 0.55
        if dx * dx * 3 + dy * dy * 1.2 < 0.06 - 0.12 * max(0, -dy) * abs(dx) * 4:
            c = noisy(BLOOD, 0.04)
        return c
    tex.zone("wood_mark", 8, 0, 16, 8, marked)
    tex.zone("band", 0, 8, 8, 10, metal(BLACK, IRON, rust=0.2, rivets=5))
    tex.zone("lid", 8, 8, 16, 16, lambda tx, ty, x, y, f: noisy(WOOD_DARK if (x + y) % 7 == 0 else WOOD, 0.05))
    tex.zone("fuse", 0, 10, 4, 14, lambda tx, ty, x, y, f: noisy(ROPE if (x + y) % 3 else WOOD_HI, 0.04))
    tex.zone("fuse_tip", 4, 10, 8, 14, lambda tx, ty, x, y, f: lerp(WHITE_HOT, ORANGE, ty))
    tex.save(ROOT)

    m = Model("conqueror/powder_keg", tex,
              "a powder keg with a burning fuse, for the Conqueror's Explosive Charge, 0.6 blocks wide. "
              "A red flame is painted on its front. Pivot 8,0,8 = the middle of its bottom.")
    m.cube("staves", (4, 0, 4), (12, 11, 12), "wood", faces={"north": "wood_mark"})
    m.cube("belly_x", (3.4, 1.5, 5), (12.6, 9.5, 11), "wood", faces={"north": "wood_mark"})
    m.cube("belly_z", (5, 1.5, 3.4), (11, 9.5, 12.6), "wood", faces={"north": "wood_mark"})
    for i, y in enumerate([1.2, 8.8]):
        m.cube("band_%d" % i, (3.2, y, 3.2), (12.8, y + 1.1, 12.8), "band")
    m.cube("lid", (4.4, 11, 4.4), (11.6, 11.5, 11.6), "lid")
    m.cube("fuse", (7.6, 11.5, 7.6), (8.4, 14.5, 8.4), "fuse", rot=(22.5, "z", (8, 11.5, 8)))
    m.cube("fuse_tip", (7.4, 14.5, 7.4), (8.6, 15.4, 8.6), "fuse_tip", rot=(22.5, "z", (8, 11.5, 8)))
    m.save(ROOT)


# =====================================================================================================
# Flame Transformation: a spear of fire, tip up (for Shot / Transit: its top is its front).
# =====================================================================================================
def flame_spear():
    tex = Texture("conqueror/flame_spear", 32, 32, frames=4, frametime=2)
    tex.zone("core", 0, 0, 4, 16, lambda tx, ty, x, y, f: lerp(WHITE_HOT, YELLOW, abs(tx - 0.5) * 2))
    tex.zone("head", 4, 0, 10, 16, flame(base=(EMBER, ORANGE, YELLOW, WHITE_HOT, WHITE_HOT)))
    tex.zone("shaft", 10, 0, 13, 16, gradient([(0.0, ORANGE), (0.5, EMBER), (1.0, BLOOD)]))
    tex.zone("flame", 13, 0, 16, 16, flame())
    tex.save(ROOT)

    m = Model("conqueror/flame_spear", tex,
              "a spear of fire for the Conqueror's Flame Transformation, 2.6 blocks long, tip up: a white-hot "
              "leaf-shaped head, a burning shaft, flames streaming back from it. The texture flickers. "
              "Pivot 8,8,8 = the middle of the shaft, tip 8,32,8.")
    m.cube("shaft", (7.3, -10, 7.3), (8.7, 18, 8.7), "shaft")
    m.cube("core", (7.6, -6, 7.6), (8.4, 30, 8.4), "core")
    for i, (y0, y1, half) in enumerate([(18, 21, 2.2), (21, 24.5, 1.7), (24.5, 27.5, 1.2), (27.5, 30.5, 0.75), (30.5, 32, 0.35)]):
        m.cube("head_%d" % i, (8 - half, y0, 7.55), (8 + half, y1, 8.45), "head")
        m.cube("head_cross_%d" % i, (7.55, y0, 8 - half * 0.7), (8.45, y1, 8 + half * 0.7), "head")
    m.cube("guard", (5.5, 16.5, 7), (10.5, 18, 9), "head")
    for i, (yaw, y0) in enumerate([(45, 13), (-45, 13), (45, 4), (-45, 4)]):
        m.cube("wisp_%d" % i, (5, y0, 7.75), (11, y0 + 4, 8.25), "flame", rot=(yaw, "y", (8, y0, 8)))
    m.cube("tail", (6.5, -16, 6.5), (9.5, -8, 9.5), "flame")
    m.cube("tail_end", (7.2, -16, 7.2), (8.8, -12, 8.8), "flame")
    m.save(ROOT)


# =====================================================================================================
# Misdirection: a figure of flame in your shape, 2 blocks tall.
# =====================================================================================================
def flame_figure():
    tex = Texture("conqueror/flame_figure", 64, 64, frames=4, frametime=2)
    tex.zone("body", 0, 0, 8, 8, flame(base=(BLOOD_DARK, BLOOD, EMBER, ORANGE, YELLOW)))
    tex.zone("limb", 8, 0, 16, 8, flame(base=(BLOOD, EMBER, ORANGE, YELLOW, WHITE_HOT)))

    def face(tx, ty, x, y, f):
        c = flame(base=(BLOOD_DARK, BLOOD, EMBER, ORANGE, YELLOW))(tx, ty, x, y, f)
        for ex in (0.3, 0.7):             # two white-hot eyes
            if abs(tx - ex) < 0.1 and abs(ty - 0.45) < 0.07:
                return WHITE_HOT
        return c
    tex.zone("face", 0, 8, 8, 16, face)
    tex.zone("tongue", 8, 8, 16, 16, flame(base=(EMBER, ORANGE, YELLOW, WHITE_HOT, WHITE_HOT)))
    tex.save(ROOT)

    m = Model("conqueror/flame_figure", tex,
              "a figure of flame in a person's shape, 2 blocks tall, for the Conqueror's Misdirection: the decoy "
              "left behind. White-hot eyes; the texture flickers. Face = north. Pivot 8,0,8 = its feet.")
    m.cube("leg_west", (4.2, 0, 6), (7.8, 12, 10), "limb")
    m.cube("leg_east", (8.2, 0, 6), (11.8, 12, 10), "limb")
    m.cube("torso", (4, 12, 6), (12, 24, 10), "body")
    m.cube("arm_west", (0.3, 12.5, 6.3), (4, 24, 9.7), "limb", rot=(-22.5, "z", (4, 24, 8)))
    m.cube("arm_east", (12, 12.5, 6.3), (15.7, 24, 9.7), "limb", rot=(22.5, "z", (12, 24, 8)))
    m.cube("head", (4.5, 24, 4.5), (11.5, 31, 11.5), "body", faces={"north": "face"})
    for i, (x0, z0, h, ang, axis) in enumerate([(5, 6, 3, 22.5, "z"), (9, 7, 4, -22.5, "z"), (7, 5, 3, 22.5, "x"),
                                                (6.5, 9, 3, -22.5, "x")]):
        m.cube("crown_%d" % i, (x0, 29, z0), (x0 + 2, 29 + h, z0 + 2), "tongue", rot=(ang, axis, (x0 + 1, 29, z0 + 1)))
    m.cube("shoulder_west", (3, 22, 6.5), (6, 27, 9.5), "tongue", rot=(-22.5, "z", (4.5, 22, 8)))
    m.cube("shoulder_east", (10, 22, 6.5), (13, 27, 9.5), "tongue", rot=(22.5, "z", (11.5, 22, 8)))
    m.cube("trail", (5, 0, 9.5), (11, 9, 12.5), "tongue", rot=(22.5, "x", (8, 0, 9.5)))
    m.save(ROOT)


# =====================================================================================================
# Provocation / Fog of War: a tattered war banner on a pole, 3 blocks tall. Frames: swaying in the wind.
# =====================================================================================================
def war_banner():
    tex = Texture("conqueror/war_banner", 128, 128)
    tex.zone("pole", 0, 0, 1, 16, lambda tx, ty, x, y, f: noisy(lerp(WOOD_DARK, WOOD, 0.5 + 0.5 * math.sin(y * 0.7)), 0.04))
    tex.zone("iron", 1, 0, 2, 4, metal(IRON_DARK, STEEL))

    def cloth(tx, ty, x, y, f):
        c = lerp(BLOOD, BLOOD_DARK, ty * 0.7 + 0.15 * math.sin(tx * 13))
        border = min(tx, 1 - tx, ty) < 0.07
        if border:
            c = GOLD_DARK if min(tx, 1 - tx, ty) > 0.025 else BLACK
        if R.random() < 0.02:
            c = BLOOD_DARK
        return noisy(c, 0.03)
    tex.zone("cloth", 2, 0, 9, 12, cloth)
    tex.zone("strip", 9, 0, 11, 12, lambda tx, ty, x, y, f: noisy(lerp(BLOOD, BLOOD_DARK, ty), 0.04)
             if not (ty > 0.85 and R.random() < 0.5) else noisy(BLOOD_DARK, 0.04))
    # the emblem on the cloth: two crossed swords under a crown, in black and gold
    draw = ImageDraw.Draw(tex.img)
    u0, v0, u1, v1 = tex.zones["cloth"]
    X0, Y0, X1, Y1 = u0 * 8, v0 * 8, u1 * 8, v1 * 8       # 128 px / 16 uv = 8 px per uv unit
    cx, cy = (X0 + X1) / 2, Y0 + (Y1 - Y0) * 0.5
    for s in (-1, 1):
        draw.line([(cx - s * 18, cy + 22), (cx + s * 14, cy - 18)], fill=BLACK, width=5)
        draw.line([(cx - s * 18, cy + 22), (cx + s * 14, cy - 18)], fill=GOLD, width=2)
        draw.line([(cx - s * 14 - 6, cy + 12), (cx - s * 14 + 6, cy + 18)], fill=GOLD, width=3)   # the hilts' guards
    draw.polygon([(cx - 12, cy - 22), (cx - 12, cy - 32), (cx - 6, cy - 26), (cx, cy - 34), (cx + 6, cy - 26),
                  (cx + 12, cy - 32), (cx + 12, cy - 22)], fill=GOLD, outline=BLACK)
    draw.ellipse([cx - 4, cy + 26, cx + 4, cy + 34], fill=BLOOD_HI, outline=BLACK)               # a drop of blood
    tex.save(ROOT)

    for frame, sway in enumerate([-22.5, 0.0, 22.5]):
        m = Model("conqueror/war_banner", tex,
                  "a tattered war banner, 3 blocks tall: blood-red cloth with a gold border, two crossed swords under "
                  "a crown, on a dark pole with an iron point. Frame %d of 0..2 (0 and 2 = blown back and forth). "
                  "ModelFx.of(\"conqueror/war_banner\").frames(3).smooth(). Cloth = north. Pivot 8,-16,8 = the foot "
                  "of the pole." % frame)
        m.cube("pole", (7.3, -16, 7.3), (8.7, 29, 8.7), "pole")
        m.cube("point", (7.6, 29, 7.6), (8.4, 32, 8.4), "iron")
        m.cube("crossbar", (1, 26.5, 7.5), (15, 27.7, 8.5), "pole")
        m.cube("knob_west", (0.4, 26.3, 7.3), (1.4, 27.9, 8.7), "iron")
        m.cube("knob_east", (14.6, 26.3, 7.3), (15.6, 27.9, 8.7), "iron")
        rot = (sway, "x", (8, 26.5, 8.4)) if sway else None
        m.cube("cloth", (1.6, 6, 8.6), (14.4, 26.5, 9.0), "cloth", rot=rot,
               faces={"south": tex.uv("cloth", flip_u=True)})
        for i, (x0, length) in enumerate([(1.6, 3.5), (4.2, 1.8), (6.8, 4.5), (9.4, 2.4), (12.0, 3.8)]):
            m.cube("strip_%d" % i, (x0, 6 - length, 8.6), (x0 + 2.4, 6, 9.0), "strip", rot=rot)
        m.save(ROOT, "_%d" % frame)


# =====================================================================================================
# Cull: the Reaper's scythe, 3.5 blocks tall. Pivot at the lower grip.
# =====================================================================================================
def reaper_scythe():
    tex = Texture("conqueror/reaper_scythe", 64, 64)

    def blade(tx, ty, x, y, f):           # blackened steel; ty = 0 the back of the blade, 1 the edge
        c = lerp(BLACK, IRON, 0.3 + 0.4 * ty + 0.1 * math.sin(tx * 17))
        if ty > 0.7:
            c = lerp(c, STEEL, (ty - 0.7) / 0.3)
        return noisy(c, 0.03)
    tex.zone("blade", 0, 0, 8, 6, blade)
    tex.zone("edge", 0, 6, 8, 8, lambda tx, ty, x, y, f: lerp(WHITE_HOT, BLOOD_HI, ty * 0.6 + 0.4 * abs(math.sin(tx * 9))))
    tex.zone("snath", 8, 0, 10, 16, lambda tx, ty, x, y, f: noisy(lerp(WOOD_DARK, BLACK, 0.3 + 0.3 * math.sin(y * 0.5)), 0.04))
    tex.zone("wrap", 10, 0, 12, 8, lambda tx, ty, x, y, f: noisy(BLOOD if (y // 2) % 2 else BLOOD_DARK, 0.03))
    tex.zone("iron", 12, 0, 16, 4, metal(IRON_DARK, STEEL, rivets=4))
    tex.zone("gold", 12, 4, 16, 8, metal(GOLD_DARK, GOLD))
    tex.zone("gem", 10, 8, 12, 10, lambda tx, ty, x, y, f: lerp(WHITE_HOT, BLOOD_HI, ((tx - 0.5) ** 2 + (ty - 0.5) ** 2) * 4))
    tex.zone("bone", 0, 8, 8, 12, lambda tx, ty, x, y, f: noisy(lerp((228, 220, 196, 255), (150, 138, 112, 255), ty), 0.04))
    tex.save(ROOT)

    m = Model("conqueror/reaper_scythe", tex,
              "the Reaper's scythe for the Conqueror's Cull, 3 blocks tall: a black blade curving down to the right "
              "(+X) from the top of a dark shaft, its edge glowing blood-red, red wraps, two grips, a skull and a "
              "red gem on the head. The shaft stands at x 4. Pivot 4,-12,8 = the lower grip.")
    # the shaft stands at x = 4 (left of the middle), so the blade has room to reach far out to the right
    m.cube("snath", (3.25, -16, 7.25), (4.75, 24, 8.75), "snath")
    m.cube("grip_low", (3.5, -12.5, 3.5), (4.5, -11.5, 7.25), "snath")
    m.cube("grip_high", (3.5, 6, 3.5), (4.5, 7, 7.25), "snath")
    for i, y in enumerate([-9.0, 2.0, 16.0]):
        m.cube("wrap_%d" % i, (3.0, y, 7.0), (5.0, y + 2.2, 9.0), "wrap")
    m.cube("socket", (2.4, 21.5, 6.4), (5.6, 27.5, 9.6), "iron")
    m.cube("collar", (2.0, 21.0, 6.0), (6.0, 22.0, 10.0), "gold")
    m.cube("skull", (2.6, 27.5, 6.6), (5.4, 30.0, 9.4), "bone")
    m.cube("skull_jaw", (3.0, 26.8, 6.3), (5.0, 27.6, 7.0), "bone")
    m.cube("gem", (3.4, 23.4, 6.1), (4.6, 24.6, 6.4), "gem")
    m.cube("back_spike", (-1.0, 24.5, 7.7), (2.4, 25.8, 8.3), "iron", rot=(22.5, "z", (2.4, 25.2, 8)))
    # the blade: segments chained end to end, each turned a little more, so it curves down like a crescent
    x, y = 5.6, 27.0                       # where the blade leaves the socket (its back edge)
    for i, (length, depth, angle) in enumerate([(7.4, 4.8, 0.0), (6.6, 4.2, -22.5), (6.2, 3.6, -22.5),
                                                (5.6, 2.8, -45.0), (4.6, 1.8, -45.0)]):
        m.cube("blade_%d" % i, (x, y - depth, 7.7), (x + length, y, 8.3), "blade", rot=(angle, "z", (x, y, 8)) if angle else None)
        m.cube("edge_%d" % i, (x, y - depth - 0.8, 7.85), (x + length, y - depth, 8.15), "edge", rot=(angle, "z", (x, y, 8)) if angle else None)
        x, y = rotate2(x + length, y, x, y, angle)
    print("  scythe blade tip at", round(x, 2), round(y, 2))
    m.save(ROOT)


# =====================================================================================================
# Galvanization: a mantle of iron worn over the body (pauldrons, breastplate, faulds, vambraces).
# =====================================================================================================
def iron_mantle():
    tex = Texture("conqueror/iron_mantle", 64, 64)

    def plate(tx, ty, x, y, f):           # dark iron with red-hot veins (iron-blooded)
        c = lerp(IRON_DARK, IRON, 0.4 + 0.4 * (1 - ty))
        v = math.sin(tx * 11 + math.sin(ty * 7) * 2) * math.sin(ty * 5 + tx * 3)
        if abs(v) < 0.05:
            c = lerp(BLOOD_HI, ORANGE, R.random() * 0.4)
        if (x % 7 == 1) and (y % 7 == 1):
            c = STEEL_HI
        return noisy(c, 0.03)
    tex.zone("plate", 0, 0, 8, 8, plate)

    def chest(tx, ty, x, y, f):           # the breastplate's front: a red sword upright, a gold rim
        c = plate(tx, ty, x, y, f)
        if abs(tx - 0.5) < 0.05 and 0.15 < ty < 0.85:
            c = BLOOD_HI
        if abs(ty - 0.35) < 0.04 and abs(tx - 0.5) < 0.2:
            c = GOLD
        if min(tx, 1 - tx) < 0.06:
            c = GOLD_DARK
        return c
    tex.zone("chest", 8, 0, 16, 8, chest)
    tex.zone("dark", 0, 8, 8, 12, metal(BLACK, IRON_DARK, rivets=5))
    tex.zone("trim", 8, 8, 16, 10, metal(GOLD_DARK, GOLD))
    tex.save(ROOT)

    m = Model("conqueror/iron_mantle", tex,
              "a mantle of dark iron with red-hot veins, worn over a player's body, for the Conqueror's "
              "Galvanization: pauldrons, a breastplate with a red sword on it, faulds and vambraces. Sized to a "
              "player at scale 1. Front = north. Pivot 8,0,8 = between the feet: play it with modelOnTurning.")
    m.cube("breastplate", (3.4, 13, 5.3), (12.6, 23.6, 10.7), "plate", faces={"north": "chest"})
    m.cube("gorget", (4.4, 23.2, 4.9), (11.6, 24.8, 11.1), "dark")
    for side, sx in (("east", 1), ("west", -1)):
        lo, hi = (11.4, 16.6) if sx > 0 else (-0.6, 4.6)
        m.cube("pauldron_%s" % side, (lo, 21.4, 4.6), (hi, 25.2, 11.4), "plate",
               rot=(-sx * 22.5, "z", (12 if sx > 0 else 4, 23.5, 8)))
        lo2, hi2 = (12.0, 16.8) if sx > 0 else (-0.8, 4.0)
        m.cube("lame_%s" % side, (lo2, 19.6, 5.0), (hi2, 21.6, 11.0), "dark",
               rot=(-sx * 22.5, "z", (12 if sx > 0 else 4, 23.5, 8)))
        lo3, hi3 = (11.6, 16.4) if sx > 0 else (-0.4, 4.4)
        m.cube("vambrace_%s" % side, (lo3, 13.0, 5.6), (hi3, 17.5, 10.4), "plate")
        m.cube("trim_%s" % side, (lo3, 17.5, 5.5), (hi3, 18.3, 10.5), "trim")
    m.cube("faulds", (3.4, 10, 5.2), (12.6, 13.2, 10.8), "dark")
    m.cube("tasset_front", (4.6, 7.6, 4.9), (11.4, 10.2, 5.4), "plate")
    m.cube("belt", (3.3, 12.6, 5.1), (12.7, 13.6, 10.9), "trim")
    m.save(ROOT)


# =====================================================================================================
# Fog of War: the War Bishop's seal on the ground (a flat plate, 1 block wide at scale 1, like rune_circle).
# =====================================================================================================
def war_sigil():
    size = 256
    tex = Texture("conqueror/war_sigil", size, size)
    tex.zone("plate", 0, 0, 16, 16)
    d = ImageDraw.Draw(tex.img)
    c = size / 2

    def ring(r, w, col):
        d.ellipse([c - r, c - r, c + r, c + r], outline=col, width=w)

    ring(124, 4, GOLD)
    ring(116, 2, BLOOD_HI)
    ring(84, 3, GOLD)
    ring(78, 2, BLOOD)
    # runes between the rings: short strokes in groups
    for i in range(36):
        a = i * math.tau / 36
        for k, (r0, r1) in enumerate([(92, 108), (96, 104)]):
            if (i + k) % 3 == 0:
                continue
            off = (k - 0.5) * 0.04
            d.line([(c + math.cos(a + off) * r0, c + math.sin(a + off) * r0),
                    (c + math.cos(a + off * 2) * r1, c + math.sin(a + off * 2) * r1)], fill=BLOOD_HI, width=3)
    # eight swords pointing in, like a compass of war
    for i in range(8):
        a = i * math.tau / 8 + math.tau / 16
        tip = (c + math.cos(a) * 30, c + math.sin(a) * 30)
        hilt = (c + math.cos(a) * 74, c + math.sin(a) * 74)
        d.line([tip, hilt], fill=GOLD, width=4)
        ga = a + math.pi / 2
        gx, gy = c + math.cos(a) * 64, c + math.sin(a) * 64
        d.line([(gx + math.cos(ga) * 7, gy + math.sin(ga) * 7), (gx - math.cos(ga) * 7, gy - math.sin(ga) * 7)],
               fill=GOLD, width=3)
    # the bishop's mitre in the middle, over a drop of blood
    d.polygon([(c - 18, c + 14), (c - 18, c - 6), (c, c - 30), (c + 18, c - 6), (c + 18, c + 14)], fill=BLOOD, outline=GOLD)
    d.line([(c, c - 26), (c, c + 12)], fill=GOLD, width=3)
    d.line([(c - 10, c - 4), (c + 10, c - 4)], fill=GOLD, width=3)
    d.ellipse([c - 7, c + 18, c + 7, c + 32], fill=BLOOD_HI, outline=GOLD)
    tex.save(ROOT)

    m = Model("conqueror/war_sigil", tex,
              "the War Bishop's seal for the Conqueror's Fog of War: a flat circle, 1 block wide at scale 1, gold "
              "and blood-red rings of runes, eight swords pointing in, a mitre over a drop of blood. "
              "Lies flat like rune_circle. Pivot 8,0,8 = its middle.")
    m.cube("plate", (0, 0, 0), (16, 0, 16), "plate", skip=("north", "south", "east", "west"))
    m.save(ROOT)


if __name__ == "__main__":
    for make in (bear_trap, powder_keg, flame_spear, flame_figure, war_banner, reaper_scythe, iron_mantle, war_sigil):
        make()
        print("made", make.__name__)
