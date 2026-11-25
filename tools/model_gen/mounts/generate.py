"""Rebuild the two mount rigs and their pixel atlases with Python 3 and Pillow."""
import json
import math
from pathlib import Path

from PIL import Image, ImageColor, ImageDraw

ASSETS = Path(__file__).resolve().parents[3] / "src/main/resources/assets/circle_of_inevitability"
COMMON = dict(leather="#6c392a", seam="#bb7650", brass="#e5ae54", claw="#ddd2b1",
              pupil="#182034", shine="#f2fff4", ink="#262139")
PALETTES = {
    "wyvern": dict(COMMON, hide="#464261", belly="#9884a7", ridge="#a174ba", wing="#a053ad",
                   spar="#5b446d", eye="#74ecdd", horn="#c2bdcb", cloth="#624582"),
    "raptor": dict(COMMON, hide="#33786e", belly="#c8aa7c", ridge="#305459", plume="#cc653b",
                   vane="#e6ad52", eye="#ffd060", cloth="#a94e3a")
}


def box(x, y, z, w, h, d, material="hide"):
    return dict(origin=[x, y, z], size=[w, h, d], material=material)


def side_box(side, start, y, z, w, h, d, material):
    return box(start if side == 1 else -start-w, y, z, w, h, d, material)


class Rig:
    def __init__(self):
        self.bones = []

    def bone(self, name, parent, pivot, cubes, rotation=(0, 0, 0)):
        self.bones.append(dict(name=name, parent=parent, pivot=pivot, rotation=rotation, cubes=cubes))


def saddle(rig, wide):
    half = 6 if wide else 5
    cubes = [box(-half, -1, -4, half*2, 3, 10, "cloth"),
             box(-4, -2, -3, 8, 2, 8, "leather"),
             box(-4, -3, 4, 8, 3, 2, "seam"), box(-3, -3, -4, 6, 2, 2, "leather")]
    for side in (-1, 1):
        cubes += [side_box(side, half-1, 1, -3, 2, 7, 7, "cloth"),
                  side_box(side, half+0.06, 7, -3, 1, 1, 7, "brass"),
                  side_box(side, half+0.14, 0, -1, 1, 10, 2, "leather"),
                  side_box(side, half+0.22, 8, -2, 1, 2, 4, "brass")]
    rig.bone("saddle", "body", (0, -5 if wide else -4, 0), cubes)


def legs(rig, wide):
    for side, name in [(-1, "left"), (1, "right")]:
        leg = name + "_leg"
        rig.bone(leg, "body", (side*(4 if wide else 3.5), 1, 4), [
            box(-2.5, -1, -3.5, 5, 8, 6), box(-2, 2, -4, 4, 4, 2, "ridge")], (-14, 0, 0))
        rig.bone("shin", leg, (0, 6, -1), [
            box(-1.5, 0, -1.5, 3, 7, 3, "belly"), box(-1.6, 4, -1.6, 3, 2, 4, "ridge")], (32, 0, 0))
        foot = [box(-2, 0, -2, 4, 2, 5, "ridge")]
        for toe in (-1, 1):
            foot.append(box(toe*1.5-0.5, 1, -4, 1, 1, 4, "claw"))
        if not wide:  # The raptor carries its enlarged inner toe above the ground.
            foot += [side_box(-side, 1, -2, -3, 2, 3, 3, "ink"),
                     side_box(-side, 1, -2, -5, 1, 1, 3, "claw")]
        rig.bone("foot", "shin", (0, 7, 0), foot, (-18, 0, 0))


def face(rig, wide):
    if wide:
        cubes = [box(-4.5, -4, -7, 9, 7, 9), box(-3.5, -2, -13, 7, 4, 7),
                 box(-3, 2, -12.5, 6, 2, 8, "belly"), box(-3, 1.3, -13.1, 6, 1, 6, "ink")]
    else:
        cubes = [box(-4, -3.5, -7, 8, 6, 9), box(-3, -2, -12, 6, 3, 6),
                 box(-2.5, -1.6, -15, 5, 2, 4), box(-2.5, 0.5, -14.7, 5, 2, 9, "belly"),
                 box(-2.6, 0.3, -15.1, 5, 1, 7, "ink")]
    for side in (-1, 1):
        cubes += [side_box(side, 4, -2.6, -6.5, 1, 3, 4, "ink"),
                  side_box(side, 4.1, -2.4, -6, 1, 2, 3, "eye"),
                  side_box(side, 4.2, -2.4, -5.3, 1, 2, 1, "pupil"),
                  side_box(side, 4.3, -2.6, -6.1, 1, 1, 1, "shine"),
                  side_box(side, 3.1, -3.9, -6.8, 2, 2, 6, "ridge"),
                  side_box(side, 2.8, -1.3, -12.1, 1, 1, 2, "ink"),
                  side_box(side, 2.5, 1.4, -11, 1, 2, 1, "claw")]
    rig.bone("head", "neck", (0, -3, -7), cubes, (15 if wide else 20, 0, 0))


def tail(rig, wide):
    rig.bone("tail", "body", (0, -1, 8), [
        box(-3, -2.5, 0, 6, 5, 11), box(-2, -1.5, 9, 4, 3, 7),
        box(-1, -4, 4, 2, 2, 4, "ridge")], (-4, 0, 0))
    cubes = [box(-1.5, -1.5, 0, 3, 3, 10), box(-1, -1, 9, 2, 2, 6, "ridge")]
    if wide:
        cubes += [box(-0.5, -3, 8, 1, 5, 7, "ridge"), box(-0.5, -2, 14, 1, 2, 4, "horn")]
    else:
        cubes += [box(-2.5, -1, 6, 5, 1, 9, "plume"), box(-3.5, -0.8, 9, 7, 1, 4, "vane"),
                  box(-2, -0.8, 14, 4, 1, 3, "plume")]
    rig.bone("tip", "tail", (0, 0, 15), cubes, (-6, 0, 0))


def wyvern():
    rig = Rig()
    rig.bone("body", None, (0, 7, 0), [
        box(-5.5, -4, -9, 11, 9, 19), box(-4, 2, -8, 8, 4, 15, "belly"),
        box(-5, -5, -8, 10, 3, 5, "ridge"), box(-3.5, -3, 8, 7, 6, 5)])
    rig.bone("neck", "body", (0, -1, -8), [
        box(-3, -4, -9, 6, 7, 11), box(-2.5, 1, -8, 5, 3, 9, "belly"),
        box(-1, -6, -4, 2, 3, 4, "ridge")], (-23, 0, 0))
    face(rig, True)
    for side, name in [(-1, "left"), (1, "right")]:
        rig.bone(name+"_horn", "head", (side*3.1, -3, -0.8), [
            box(-1.5, -4, -1, 3, 5, 3, "ridge"), box(-1, -7, 0, 2, 4, 2, "horn"),
            box(-0.5, -9, 1, 1, 3, 2, "claw")], (-35, 0, side*18))
        rig.bone(name+"_ear", "head", (side*4, -0.5, 0), [
            side_box(side, 0, -1, 0, 2, 2, 5, "ridge"), side_box(side, 1, -1, 4, 1, 1, 4, "horn")
        ], (-12, side*16, 0))
        # Wing fingers divide the membrane into pointed, stepped panels.
        def span(x, y, z, w, h, d, material):
            return side_box(side, x, y, z, w, h, d, material)
        wing = name + "_wing"
        rig.bone(wing, "body", (side*4.5, -3, -5), [
            span(0, -1, -1, 9, 3, 4, "spar"), span(7, -2, -3, 9, 3, 4, "spar"),
            span(0, 0, 2, 5, 1, 14, "wing"), span(5, 0, 1, 5, 1, 13, "wing"),
            span(10, -0.5, 0, 5, 1, 16, "wing"), span(14, -1, -2, 2, 2, 20, "spar"),
            span(14, -1, 16, 1, 1, 3, "horn")], (0, side*8, -side*12))
        rig.bone("tip", wing, (side*15, -1, -3), [
            span(0, -1, -1, 8, 2, 3, "spar"), span(7, -0.5, 1, 8, 2, 2, "spar"),
            span(14, 0, 2, 6, 1, 2, "horn"), span(0, -2, -3, 2, 3, 3, "claw"),
            span(0, 0, 1, 4, 1, 16, "wing"), span(4, 0, 2, 4, 1, 12, "wing"),
            span(8, 0, 3, 3, 1, 12, "wing"), span(11, 0, 3, 4, 1, 9, "wing"),
            span(15, 0, 4, 3, 1, 5, "wing"), span(9, -0.3, 2, 1, 1, 14, "spar")
        ], (0, -side*29, side*19))
    legs(rig, True)
    tail(rig, True)
    saddle(rig, True)
    return rig


def raptor():
    rig = Rig()
    rig.bone("body", None, (0, 8, 0), [
        box(-4, -3.5, -8, 8, 7, 17), box(-3.5, 1, -7, 7, 4, 13, "belly"),
        box(-3, -4.5, -8, 6, 3, 5, "ridge"), box(-3, -2, 7, 6, 5, 5)])
    rig.bone("neck", "body", (0, -1, -7), [
        box(-2.5, -3, -8, 5, 6, 9), box(-2, 1, -8, 4, 3, 8, "belly")], (-32, 0, 0))
    face(rig, False)
    for side, name in [(-1, "left"), (1, "right")]:
        for index, (y, length, angle) in enumerate([(-3.8, 10, 28), (-1.8, 8, 14), (0.2, 6, 3)]):
            rig.bone(name+"_crest_"+str(index), "head", (side*2.8, y, -0.3), [
                box(-1, -1, -1, 2, 2, length, "plume"), box(-1.5, -1.3, 2, 3, 1, length-3, "plume"),
                box(-1, -1.4, length-3, 2, 1, 3, "vane"), box(-0.5, -1.1, length-1, 1, 1, 3, "vane")
            ], (angle, side*15, 0))
        rig.bone(name+"_arm", "body", (side*3.5, 1, -6), [
            box(-1, 0, -1, 2, 5, 3), box(-1, 3, -4, 2, 2, 4, "belly"),
            box(-1, 4, -5, 1, 2, 2, "claw"), box(0.5, 4, -5, 1, 1, 2, "claw")], (-9, 0, side*-13))
    legs(rig, False)
    tail(rig, False)
    saddle(rig, False)
    return rig


def paint_face(image, rect, material, base, shade, seed):
    x, y, w, h = rect
    for j in range(h):
        for i in range(w):
            grain = ((i*31 + j*47 + i*j*7 + seed*13) % 13 - 6) * 0.007
            light = shade + 0.09*(1-j/max(1, h-1)) + grain
            if material == "hide":
                band = (i + j//2 + seed) % 12
                light += -0.18 if band < 2 and j < h*0.7 else 0.04*((i//3+j//2) % 2)
            elif material == "wing":
                light += 0.19*math.sin((i+j*0.52)*0.43) - (0.24 if j == h-1 else 0)
            elif material == "cloth":
                if j == h-2 or ((i+j) % 8 == 0 and j > h//2):
                    image.putpixel((x+i, y+j), (207, 153, 75, 255))
                    continue
            elif material in ("horn", "claw", "belly"):
                light -= 0.12 if j % 4 == 3 else 0
            image.putpixel((x+i, y+j), tuple(max(0, min(255, round(c*light))) for c in base) + (255,))


def export(name, rig):
    palette = PALETTES[name]
    size = 256
    atlas = Image.new("RGBA", (size, size))
    x = y = row = 0
    for bone in rig.bones:
        for cube in bone["cubes"]:
            w, h, d = cube["size"]
            uw, uh = 2*(w+d), d+h
            if x+uw > size:
                x, y, row = 0, y+row+1, 0
            assert y+uh <= size
            cube["uv"] = [x, y]
            mat = cube["material"]
            base = ImageColor.getrgb(palette[mat])
            faces = [(d, 0, w, d, 1.08), (d+w, 0, w, d, 0.88),
                     (0, d, d, h, 0.95), (d, d, w, h, 1.0),
                     (d+w, d, d, h, 0.96), (d+w+d, d, w, h, 0.94)]
            for u, v, fw, fh, shade in faces:
                paint_face(atlas, (x+u, y+v, fw, fh), mat, base, shade, x+y)
            x += uw+1
            row = max(row, uh)
    tex = ASSETS / "textures/entity/mount"
    tex.mkdir(parents=True, exist_ok=True)
    atlas.save(tex / (name+".png"), optimize=True)
    out = ASSETS / "mounts"
    out.mkdir(exist_ok=True)
    # One cube per line keeps the JSON convenient to edit and review.
    bones = []
    for bone in rig.bones:
        header = {k:v for k,v in bone.items() if k != "cubes"}
        cubes = ",\n".join("        "+json.dumps(c) for c in bone["cubes"])
        bones.append("    "+json.dumps(header)[:-1]+', "cubes": [\n'+cubes+'\n    ]}')
    (out / (name+".rig.json")).write_text('{\n  "texture_size": [256, 256],\n  "bones": [\n'+
                                            ",\n".join(bones)+'\n  ]\n}\n')


def whistle(name):
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    colors = PALETTES[name]
    draw.line([(2, 8), (1, 11), (3, 14), (6, 14), (8, 12)], fill="#57382d", width=2)
    draw.line([(2, 10), (3, 13), (6, 13)], fill="#cb9664")
    draw.polygon([(3, 7), (6, 4), (9, 4), (12, 1), (14, 2), (15, 4), (12, 7),
                  (12, 10), (9, 13), (6, 12), (3, 10)], fill=colors["ink"])
    draw.polygon([(4, 7), (7, 5), (9, 5), (12, 2), (14, 4), (11, 7), (11, 9),
                  (8, 12), (5, 10)], fill=colors["hide"])
    draw.line([(4, 8), (7, 5), (9, 5)], fill=colors["ridge"], width=2)
    draw.line([(10, 4), (12, 6)], fill=colors["brass"], width=2)
    draw.line([(12, 2), (14, 4)], fill=colors["claw"])
    draw.line([(5, 10), (8, 12), (11, 9)], fill=colors["brass"])
    draw.point((7, 7), fill=colors["eye"])
    draw.line([(8, 8), (9, 7)], fill=colors["ink"])
    image.save(ASSETS / "textures/item" / (name+"_whistle.png"))


if __name__ == "__main__":
    for name, rig in [("wyvern", wyvern()), ("raptor", raptor())]:
        export(name, rig)
        whistle(name)
