"""Screen pictures for the Beauty Goddess (Moon pathway).

python3 tools/model_gen/beauty_goddess/icons.py src/main/resources/assets/circle_of_inevitability [preview.png]

  textures/gui/ability_menus/beauty_goddess/*.png   32x32, the two wheels (Potions on key 4, the Moon wheel on key 7),
                                                    in a round silver frame like the Apocalypse wheels' violet one
  textures/gui/beauty_goddess/*.png                 16x16, the statuses shown above the hotbar
  textures/gui/pathway_icons/beauty_goddess.png     16x16, the Pathway's icon beside the hotbar

Every picture is drawn in flat colors and then outlined automatically, so the shapes stay simple to change.
Replace any of them with a hand-drawn PNG of the same name.
"""
import math
import os
import sys

from PIL import Image, ImageDraw

# ------------------------------------------------------------------ colors
OUTLINE = (20, 12, 30, 255)
SILVER = (200, 208, 228, 255)
SILVER_LIGHT = (244, 248, 255, 255)
SILVER_DARK = (110, 116, 146, 255)
MOON = (232, 238, 255, 255)
MOON_SHADE = (160, 172, 206, 255)
CRIMSON = (150, 16, 36, 255)
RED = (214, 42, 58, 255)
RED_LIGHT = (255, 140, 150, 255)
NIGHT = (34, 24, 60, 255)
VIOLET = (120, 80, 200, 255)
VIOLET_LIGHT = (196, 170, 255, 255)
GOLD = (240, 196, 72, 255)
GOLD_LIGHT = (255, 240, 170, 255)
GREEN = (80, 160, 60, 255)
GREEN_LIGHT = (160, 220, 100, 255)
GLASS = (190, 220, 240, 255)
FIRE = (255, 140, 40, 255)
FIRE_LIGHT = (255, 220, 120, 255)
SHADOW = (40, 30, 52, 255)
SHADOW_LIGHT = (80, 66, 100, 255)
ROSE = (240, 150, 190, 255)
ROSE_LIGHT = (255, 210, 230, 255)
SPIRIT = (110, 150, 255, 255)
SPIRIT_LIGHT = (200, 230, 255, 255)
BAT = (36, 22, 34, 255)


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


def crescent(d, cx, cy, r, color, cut_dx, cut_dy=0.0, cut=(0, 0, 0, 0)):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color)
    d.ellipse([cx - r + cut_dx, cy - r + cut_dy, cx + r + cut_dx, cy + r + cut_dy], fill=cut)


def flask(d, cx, cy, s, liquid, liquid_light):
    """A round flask, s = body radius."""
    d.ellipse([cx - s, cy - s, cx + s, cy + s], fill=GLASS)
    d.rectangle([cx - s * 0.35, cy - s * 1.9, cx + s * 0.35, cy - s * 0.7], fill=GLASS)
    d.rectangle([cx - s * 0.5, cy - s * 2.3, cx + s * 0.5, cy - s * 1.9], fill=(150, 100, 60, 255))
    d.ellipse([cx - s * 0.8, cy - s * 0.45, cx + s * 0.8, cy + s * 0.8], fill=liquid)
    d.rectangle([cx - s * 0.8, cy - s * 0.1, cx + s * 0.8, cy + s * 0.1], fill=liquid_light)
    d.point((cx - s * 0.5, cy - s * 0.6), fill=(255, 255, 255, 255))


def bat_shape(d, cx, cy, s, color, eye=RED):
    """A bat with spread wings, s = half its wingspan."""
    pts = [(cx, cy - s * 0.25), (cx + s * 0.3, cy - s * 0.55), (cx + s, cy - s * 0.6), (cx + s * 0.8, cy - s * 0.1),
           (cx + s * 0.55, cy + s * 0.05), (cx + s * 0.35, cy - s * 0.05), (cx + s * 0.2, cy + s * 0.3), (cx, cy + s * 0.15),
           (cx - s * 0.2, cy + s * 0.3), (cx - s * 0.35, cy - s * 0.05), (cx - s * 0.55, cy + s * 0.05), (cx - s * 0.8, cy - s * 0.1),
           (cx - s, cy - s * 0.6), (cx - s * 0.3, cy - s * 0.55)]
    d.polygon(pts, fill=color)
    d.polygon([(cx - s * 0.18, cy - s * 0.25), (cx - s * 0.12, cy - s * 0.55), (cx, cy - s * 0.3), (cx + s * 0.12, cy - s * 0.55),
               (cx + s * 0.18, cy - s * 0.25)], fill=color)
    if eye:
        d.point((round(cx - 1), round(cy - s * 0.3)), fill=eye)
        d.point((round(cx + 1), round(cy - s * 0.3)), fill=eye)


# ------------------------------------------------------------------ the 32x32 wheel pictures

S = 32
C = 15.5


def wheel_base(inside=(16, 14, 34, 255), edge=(30, 26, 56, 255)):
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = im.load()
    for y in range(S):
        for x in range(S):
            dd = math.hypot(x - C, y - C)
            if dd <= 15.6:
                if dd > 13.6:
                    light = (x - C) * -0.7 + (y - C) * -0.7
                    px[x, y] = SILVER_LIGHT if light > 9 else SILVER if light > -6 else SILVER_DARK
                elif dd > 12.6:
                    px[x, y] = (60, 56, 86, 255)
                else:
                    t = dd / 12.6
                    px[x, y] = tuple(int(inside[i] * (1 - t) + edge[i] * t) for i in range(3)) + (255,)
    return im


def in_frame(base, layer):
    """The drawing, outlined, laid into the frame and cut to its inside."""
    layer = outlined(layer)
    lp = layer.load()
    bp = base.load()
    for y in range(S):
        for x in range(S):
            if lp[x, y][3] and math.hypot(x - C, y - C) <= 12.4:
                bp[x, y] = lp[x, y]
    return base


def w_invisibility_potion():
    base = wheel_base()
    layer, d = canvas(S)
    flask(d, 15.5, 18.5, 6, (170, 220, 255, 150), (230, 250, 255, 200))
    for x, y in ((8, 9), (23, 11), (10, 24), (22, 23), (24, 17)):              # shimmer
        d.point((x, y), fill=SILVER_LIGHT)
    return in_frame(base, layer)


def w_fire_breathing_potion():
    base = wheel_base(inside=(36, 14, 14, 255), edge=(60, 24, 20, 255))
    layer, d = canvas(S)
    flask(d, 13, 19, 5.5, FIRE, FIRE_LIGHT)
    d.polygon([(19, 12), (27, 8), (24, 13), (28, 14), (23, 16)], fill=FIRE)    # a breath of fire from the neck
    d.polygon([(20, 13), (25, 11), (23, 14)], fill=FIRE_LIGHT)
    return in_frame(base, layer)


def w_sun_water():
    base = wheel_base(inside=(40, 30, 10, 255), edge=(70, 52, 20, 255))
    layer, d = canvas(S)
    for i in range(8):                                                        # rays
        a = math.tau * i / 8
        d.line([(15.5 + math.cos(a) * 7, 13.5 + math.sin(a) * 7), (15.5 + math.cos(a) * 10, 13.5 + math.sin(a) * 10)],
               fill=GOLD_LIGHT, width=1)
    d.ellipse([10, 8, 21, 19], fill=GOLD)
    d.ellipse([12, 10, 17, 15], fill=GOLD_LIGHT)
    d.polygon([(15.5, 19), (19, 25), (15.5, 27), (12, 25)], fill=(130, 200, 255, 255))   # a drop under the sun
    d.point((14, 24), fill=(230, 250, 255, 255))
    return in_frame(base, layer)


def w_shadow_potion():
    base = wheel_base(inside=(10, 8, 18, 255), edge=(24, 18, 36, 255))
    layer, d = canvas(S)
    d.ellipse([7, 22, 25, 27], fill=SHADOW)                                   # its shadow pooling under it
    flask(d, 15.5, 16.5, 5.5, SHADOW_LIGHT, VIOLET)
    for x, y in ((9, 20), (22, 19), (20, 24)):
        d.point((x, y), fill=SHADOW_LIGHT)
    return in_frame(base, layer)


def w_door_of_summoning():
    base = wheel_base(inside=(12, 10, 34, 255), edge=(30, 24, 70, 255))
    layer, d = canvas(S)
    d.polygon([(8, 27), (8, 12), (15.5, 5), (23, 12), (23, 27)], fill=(60, 56, 80, 255))      # the frame
    d.polygon([(10.5, 27), (10.5, 13), (15.5, 8.5), (20.5, 13), (20.5, 27)], fill=VIOLET)    # the open doorway
    d.polygon([(12, 27), (12, 14), (15.5, 11), (19, 14), (19, 27)], fill=VIOLET_LIGHT)
    d.ellipse([14, 16, 17, 19], fill=(255, 255, 255, 255))
    d.point((15, 6), fill=SILVER_LIGHT)
    return in_frame(base, layer)


def w_creation():
    base = wheel_base(inside=(14, 30, 14, 255), edge=(30, 54, 24, 255))
    layer, d = canvas(S)
    d.line([(15.5, 27), (15.5, 15)], fill=GREEN, width=2)
    d.polygon([(15, 22), (9, 19), (11, 24)], fill=GREEN_LIGHT)
    d.polygon([(16, 20), (22, 17), (21, 22)], fill=GREEN_LIGHT)
    for i in range(5):                                                        # the flower
        a = math.tau * i / 5 - math.pi / 2
        x, y = 15.5 + math.cos(a) * 4, 11 + math.sin(a) * 4
        d.ellipse([x - 2.5, y - 2.5, x + 2.5, y + 2.5], fill=(255, 250, 220, 255))
    d.ellipse([13.5, 9, 17.5, 13], fill=GOLD)
    return in_frame(base, layer)


def w_beauty():
    base = wheel_base(inside=(34, 16, 40, 255), edge=(60, 30, 70, 255))
    layer, d = canvas(S)
    crescent(d, 15.5, 15.5, 10, MOON, 4, -2)
    d.ellipse([6, 8, 13, 15], fill=MOON)
    for x, y in ((21, 9), (23, 18), (19, 24)):                                # stars of light
        d.point((x, y), fill=ROSE_LIGHT)
        d.point((x + 1, y), fill=ROSE)
        d.point((x - 1, y), fill=ROSE)
        d.point((x, y + 1), fill=ROSE)
        d.point((x, y - 1), fill=ROSE)
    d.ellipse([13, 16, 17, 20], fill=RED)                                     # the moonstone
    d.point((14, 17), fill=RED_LIGHT)
    return in_frame(base, layer)


def w_blood_moon():
    base = wheel_base(inside=(40, 6, 12, 255), edge=(80, 14, 22, 255))
    layer, d = canvas(S)
    d.ellipse([7, 6, 24, 23], fill=RED)
    d.ellipse([10, 9, 15, 14], fill=CRIMSON)
    d.ellipse([17, 14, 21, 18], fill=CRIMSON)
    d.point((11, 18), fill=CRIMSON)
    d.point((9, 10), fill=RED_LIGHT)
    for x in (9, 14, 19, 23):                                                 # blood dripping from it
        d.line([(x, 23), (x, 26 + (x % 3))], fill=RED, width=1)
    return in_frame(base, layer)


WHEEL = {
    'invisibility_potion': w_invisibility_potion,
    'fire_breathing_potion': w_fire_breathing_potion,
    'sun_water': w_sun_water,
    'shadow_potion': w_shadow_potion,
    'door_of_summoning': w_door_of_summoning,
    'creation': w_creation,
    'beauty': w_beauty,
    'blood_moon': w_blood_moon,
}


# ------------------------------------------------------------------ the 16x16 status pictures

def s(fn):
    img, d = canvas(16)
    fn(d)
    return outlined(img)


def i_medicine(d):
    flask(d, 7.5, 9.5, 3.5, GREEN, GREEN_LIGHT)
    d.line([(12, 3), (12, 7)], fill=GREEN_LIGHT)
    d.line([(10, 5), (14, 5)], fill=GREEN_LIGHT)


def i_spirit_vision(d):
    d.ellipse([1, 4, 14, 11], fill=SILVER_LIGHT)
    d.ellipse([5, 4, 10, 11], fill=VIOLET)
    d.ellipse([6.5, 6, 8.5, 9], fill=(20, 10, 40, 255))
    d.point((6, 5), fill=(255, 255, 255, 255))


def i_tamed(d):
    d.polygon([(7.5, 2), (11, 8), (4, 8)], fill=RED)
    d.ellipse([4, 6, 11, 13], fill=RED)
    d.point((6, 8), fill=RED_LIGHT)
    d.polygon([(1, 2), (3, 2), (2, 7)], fill=SILVER_LIGHT)
    d.polygon([(12, 2), (14, 2), (13, 7)], fill=SILVER_LIGHT)


def i_animal_sense(d):
    d.ellipse([4, 8, 11, 14], fill=(150, 110, 80, 255))                       # a paw
    for x, y in ((2.5, 5), (5.5, 2), (9.5, 2), (12.5, 5)):
        d.ellipse([x - 1.2, y, x + 1.2, y + 2.6], fill=(150, 110, 80, 255))
    d.point((6, 10), fill=GOLD_LIGHT)
    d.point((9, 10), fill=GOLD_LIGHT)


BAT_ART = [
    "................",
    "................",
    "................",
    ".B....B..B....B.",
    ".BB..BBBBBB..BB.",
    ".BBB.BrBBrB.BBB.",
    ".BBBBBBBBBBBBBB.",
    "BBBBBBBBBBBBBBBB",
    "BBB.BBBBBBBB.BBB",
    "B.....BBBB.....B",
    ".......BB.......",
    "................",
]


def i_wings(d):
    colors = {'B': BAT, 'r': RED}
    for y, row in enumerate(BAT_ART):
        for x, ch in enumerate(row):
            if ch in colors:
                d.point((x, y + 1), fill=colors[ch])


def i_shackles(d):
    d.ellipse([1, 4, 8, 11], outline=VIOLET, width=2)
    d.ellipse([7, 4, 14, 11], outline=SHADOW_LIGHT, width=2)
    d.point((4, 5), fill=VIOLET_LIGHT)


def i_invisibility(d):
    d.ellipse([4, 1, 11, 7], outline=GLASS, width=1)                          # a dotted figure, fading
    for y in range(8, 15, 2):
        d.point((4, y), fill=GLASS)
        d.point((11, y), fill=GLASS)
    d.point((6, 4), fill=SILVER_LIGHT)
    d.point((9, 4), fill=SILVER_LIGHT)


def i_fire_breath(d):
    d.polygon([(1, 8), (9, 3), (15, 4), (12, 8), (15, 12), (9, 13)], fill=FIRE)
    d.polygon([(3, 8), (9, 5), (11, 8), (9, 11)], fill=FIRE_LIGHT)


def i_sun_water(d):
    d.ellipse([3, 2, 12, 11], fill=GOLD)
    d.ellipse([5, 4, 9, 8], fill=GOLD_LIGHT)
    d.polygon([(7.5, 10), (10, 13), (7.5, 15), (5, 13)], fill=(130, 200, 255, 255))


def i_shadow(d):
    d.ellipse([1, 9, 14, 14], fill=SHADOW)
    d.ellipse([4, 1, 11, 8], fill=SHADOW_LIGHT)
    d.rectangle([5, 6, 10, 11], fill=SHADOW_LIGHT)
    d.point((6, 4), fill=VIOLET_LIGHT)
    d.point((9, 4), fill=VIOLET_LIGHT)


def i_lunar_battlefield(d):
    d.ellipse([3, 1, 12, 10], fill=MOON)
    d.ellipse([5, 3, 7, 5], fill=MOON_SHADE)
    d.ellipse([8, 6, 10, 8], fill=MOON_SHADE)
    d.ellipse([1, 11, 14, 15], outline=SILVER, width=1)


def i_moonlight_form(d):
    d.ellipse([5, 1, 10, 6], fill=(220, 232, 255, 255))
    d.rectangle([4, 6, 11, 11], fill=(190, 210, 255, 255))
    d.rectangle([5, 11, 10, 15], fill=(160, 186, 250, 255))
    d.point((6, 3), fill=(255, 255, 255, 255))
    d.point((9, 3), fill=(255, 255, 255, 255))


def i_gaze(d):
    d.ellipse([1, 3, 14, 12], fill=(236, 226, 222, 255))
    d.ellipse([4, 3, 11, 12], fill=CRIMSON)
    d.rectangle([7, 4, 8, 11], fill=(10, 2, 6, 255))
    d.point((5, 5), fill=(255, 255, 255, 255))


def i_bat_swarm(d):
    bat_shape(d, 5, 5, 4, BAT)
    bat_shape(d, 11, 8, 4, BAT)
    bat_shape(d, 6, 12, 3.5, BAT)


def i_summoning(d):
    d.polygon([(2, 15), (2, 6), (7.5, 1), (13, 6), (13, 15)], fill=(70, 66, 92, 255))
    d.polygon([(4, 15), (4, 7), (7.5, 4), (11, 7), (11, 15)], fill=VIOLET_LIGHT)
    d.point((7, 9), fill=(255, 255, 255, 255))
    d.point((8, 9), fill=(255, 255, 255, 255))


def i_creation(d):
    d.line([(7.5, 15), (7.5, 8)], fill=GREEN, width=1)
    d.polygon([(7, 12), (2, 9), (4, 13)], fill=GREEN_LIGHT)
    for i in range(5):
        a = math.tau * i / 5 - math.pi / 2
        x, y = 7.5 + math.cos(a) * 3, 5 + math.sin(a) * 3
        d.ellipse([x - 1.5, y - 1.5, x + 1.5, y + 1.5], fill=(255, 250, 220, 255))
    d.point((7, 5), fill=GOLD)
    d.point((8, 5), fill=GOLD)


def i_beauty(d):
    crescent(d, 7.5, 7.5, 6.5, MOON, 3, -1.5)
    d.ellipse([9, 9, 12, 12], fill=RED)
    d.point((13, 3), fill=ROSE_LIGHT)
    d.point((2, 13), fill=ROSE_LIGHT)


def i_blood_moon(d):
    d.ellipse([2, 1, 13, 12], fill=RED)
    d.ellipse([4, 3, 7, 6], fill=CRIMSON)
    d.ellipse([8, 7, 11, 10], fill=CRIMSON)
    d.line([(5, 12), (5, 15)], fill=RED)
    d.line([(10, 12), (10, 14)], fill=RED)


def i_stack_moon(d):
    """The stack picture (one per ability cast, if the Pathway gets stacks): a small moon in a halo."""
    d.ellipse([1, 1, 14, 14], outline=SILVER_DARK, width=1)
    crescent(d, 7.5, 7.5, 5, MOON, 2.5, -1)


STATUS = {
    'medicine': i_medicine,
    'spirit_vision': i_spirit_vision,
    'tamed': i_tamed,
    'animal_sense': i_animal_sense,
    'wings': i_wings,
    'shackles': i_shackles,
    'invisibility': i_invisibility,
    'fire_breath': i_fire_breath,
    'sun_water': i_sun_water,
    'shadow': i_shadow,
    'lunar_battlefield': i_lunar_battlefield,
    'moonlight_form': i_moonlight_form,
    'gaze': i_gaze,
    'bat_swarm': i_bat_swarm,
    'summoning': i_summoning,
    'creation': i_creation,
    'beauty': i_beauty,
    'blood_moon': i_blood_moon,
    'stack_moon': i_stack_moon,
}


def pathway_icon():
    img, d = canvas(16)
    crescent(d, 7, 8, 6.5, MOON, 3.5, -2)
    d.point((4, 6), fill=MOON_SHADE)
    d.point((3, 10), fill=MOON_SHADE)
    d.polygon([(12, 7), (14, 11), (12, 14), (10, 11)], fill=RED)               # a drop of blood beside it
    d.point((11, 11), fill=RED_LIGHT)
    return outlined(img)


def main(assets, preview=None):
    out = []
    for name, fn in WHEEL.items():
        path = os.path.join(assets, 'textures/gui/ability_menus/beauty_goddess', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fn().save(path)
        out.append(path)
    for name, fn in STATUS.items():
        path = os.path.join(assets, 'textures/gui/beauty_goddess', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        s(fn).save(path)
        out.append(path)
    path = os.path.join(assets, 'textures/gui/pathway_icons/beauty_goddess.png')
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
