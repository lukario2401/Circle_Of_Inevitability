"""
Small toolkit for generating ModelFx models (Blockbench "Java Block/Item" JSON + a painted PNG) from code.

A model is a list of Boxes (cubes). Each box has a material: a function that paints its faces. The painter is
handed, for every texel of a face, where that texel sits on the model (in Blockbench pixels), which way the face
looks, and the frame number for animated textures, so patterns run on across faces instead of starting again
on each one. The faces are packed into one texture, the UVs are written into the JSON, and a preview PNG is
rendered so the model can be checked without starting the game.

    python3 tools/model_fx/demoness_models.py         (writes the Demoness models; see that file)

Rules the models keep, so they also open in Blockbench: every cube inside -16..32, turned on one axis by
-45, -22.5, 0, 22.5 or 45 degrees.
"""

import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw

MOD = "circle_of_inevitability"
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
MODELS = os.path.join(ROOT, "src/main/resources/assets", MOD, "models/model_fx")
TEXTURES = os.path.join(ROOT, "src/main/resources/assets", MOD, "textures/model_fx")

FACES = ("north", "south", "east", "west", "up", "down")
NORMALS = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0),
           "up": (0, 1, 0), "down": (0, -1, 0)}
ALLOWED_ANGLES = (-45, -22.5, 0, 22.5, 45)


# =====================================================================================
# Noise and colors (all vectorised over numpy arrays)
# =====================================================================================

def _hash(ix, iy, iz, seed):
    h = (ix.astype(np.int64) * 73856093) ^ (iy.astype(np.int64) * 19349663) ^ (iz.astype(np.int64) * 83492791) ^ (seed * 2654435761)
    h &= 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    h ^= h >> 16
    return (h & 0xFFFF) / 65535.0


def noise(x, y, z, seed=0):
    """Value noise, 0..1, one bump per unit."""
    x, y, z = np.asarray(x, float), np.asarray(y, float), np.asarray(z, float)
    x, y, z = np.broadcast_arrays(x, y, z)
    ix, iy, iz = np.floor(x), np.floor(y), np.floor(z)
    fx, fy, fz = x - ix, y - iy, z - iz
    fx, fy, fz = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy), fz * fz * (3 - 2 * fz)
    out = 0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (fx if dx else 1 - fx) * (fy if dy else 1 - fy) * (fz if dz else 1 - fz)
                out = out + w * _hash(ix + dx, iy + dy, iz + dz, seed)
    return out


def fbm(x, y, z, seed=0, octaves=4):
    total, amp, norm = 0, 1.0, 0
    for o in range(octaves):
        f = 2 ** o
        total = total + amp * noise(np.asarray(x) * f, np.asarray(y) * f, np.asarray(z) * f, seed + o * 101)
        norm += amp
        amp *= 0.5
    return total / norm


def looped(fn, t, length):
    """fn(offset) blended so that t = 0..1 loops seamlessly (for animated textures): fn(t*L) fading into fn((t-1)*L)."""
    return (1 - t) * fn(t * length) + t * fn((t - 1) * length)


def rgb(hex_color):
    return np.array([(hex_color >> 16) & 255, (hex_color >> 8) & 255, hex_color & 255], float) / 255.0


def mix(a, b, t):
    t = np.clip(np.asarray(t, float), 0, 1)[..., None]
    return a * (1 - t) + b * t


def ramp(t, stops):
    """stops = [(position, 0xRRGGBB), ...] ascending. Returns colors for t."""
    t = np.asarray(t, float)
    out = np.zeros(t.shape + (3,)) + rgb(stops[0][1])
    for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
        k = np.clip((t - p0) / max(p1 - p0, 1e-6), 0, 1)
        out = np.where((t >= p0)[..., None], mix(rgb(c0), rgb(c1), k), out)
    return out


def smooth(e0, e1, x):
    t = np.clip((np.asarray(x, float) - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


# =====================================================================================
# Boxes and models
# =====================================================================================

class Box:
    """
    One cube. frm/to in Blockbench pixels. rot = (axis, angle, origin) or None. faces = which faces are drawn
    (default all six). mat = material(ctx) -> (rgb, alpha). Extra keyword arguments are kept in .p for the
    material to read (e.g. which layer of a flame this is).
    """

    def __init__(self, frm, to, mat, rot=None, faces=None, name="", **p):
        self.frm = [float(v) for v in frm]
        self.to = [float(v) for v in to]
        self.mat = mat
        self.rot = rot
        self.faces = tuple(faces) if faces else FACES
        self.name = name
        self.p = p
        if rot is not None:
            assert rot[0] in "xyz" and rot[1] in ALLOWED_ANGLES, (name, rot)

    def moved(self, frm=None, to=None, rot="keep"):
        """A copy for another frame: same material and faces, new place/turn."""
        b = Box(frm or self.frm, to or self.to, self.mat, self.rot if rot == "keep" else rot, self.faces, self.name, **self.p)
        return b

    def dims(self, face):
        dx, dy, dz = (self.to[i] - self.frm[i] for i in range(3))
        return {"up": (dx, dz), "down": (dx, dz), "north": (dx, dy), "south": (dx, dy),
                "east": (dz, dy), "west": (dz, dy)}[face]

    def point(self, face, s, t):
        """3D point (Blockbench px) of face coordinates s, t in 0..1, using the vanilla UV orientation."""
        (fx, fy, fz), (tx, ty, tz) = self.frm, self.to
        s, t = np.asarray(s, float), np.asarray(t, float)
        if face == "up":
            return fx + s * (tx - fx), ty + 0 * s, fz + t * (tz - fz)
        if face == "down":
            return fx + s * (tx - fx), fy + 0 * s, tz - t * (tz - fz)
        if face == "north":
            return tx - s * (tx - fx), ty - t * (ty - fy), fz + 0 * s
        if face == "south":
            return fx + s * (tx - fx), ty - t * (ty - fy), tz + 0 * s
        if face == "west":
            return fx + 0 * s, ty - t * (ty - fy), fz + s * (tz - fz)
        return tx + 0 * s, ty - t * (ty - fy), tz - s * (tz - fz)        # east

    def check(self):
        for v in self.frm + self.to:
            assert -16 <= v <= 32, (self.name, self.frm, self.to)


class Ctx:
    """What a material gets for one face: arrays x, y, z (texel centres), s, t (0..1 on the face), and more."""

    def __init__(self, box, face, x, y, z, s, t, w, h, frame, frames):
        self.box, self.face, self.x, self.y, self.z, self.s, self.t = box, face, x, y, z, s, t
        self.w, self.h = w, h                      # texels
        self.fw, self.fh = box.dims(face)          # pixels
        self.normal = NORMALS[face]
        self.frame, self.frames = frame, frames
        self.time = frame / frames if frames > 1 else 0.0
        self.p = box.p

    @property
    def shape(self):
        return self.s.shape

    def edge(self):
        """Distance in pixels from the nearest edge of this face."""
        return np.minimum(np.minimum(self.s * self.fw, (1 - self.s) * self.fw),
                          np.minimum(self.t * self.fh, (1 - self.t) * self.fh))

    def side(self):
        return self.face in ("north", "south", "east", "west")


class Model:
    """
    path: e.g. "demoness/mirror" (-> models/model_fx/demoness/mirror.json and the PNG of the same name).
    frames: a list of boxes, or a list of such lists for a model that changes shape (written as path_0 .. path_n).
    res: texels per pixel. anim: number of texture animation frames (0 = still), frametime: ticks per frame.
    paint_frame: which shape the texture is painted for (the faces keep their UVs in every shape).
    """

    def __init__(self, path, frames, res=1.0, credit="", anim=0, frametime=2, paint_frame=0, preview_unshaded=False,
                 min_width=16):
        self.path = path
        self.frames = frames if isinstance(frames[0], list) else [frames]
        self.res = res
        self.credit = credit
        self.anim = anim
        self.frametime = frametime
        self.paint_frame = paint_frame
        self.preview_unshaded = preview_unshaded
        self.min_width = min_width
        n = len(self.frames[0])
        for f in self.frames:
            assert len(f) == n, path + ": every shape needs the same cubes"
            for b in f:
                b.check()

    # ---------- packing ----------

    def _pack(self):
        boxes = self.frames[self.paint_frame]
        items = []
        for i, b in enumerate(boxes):
            for face in b.faces:
                fw, fh = b.dims(face)
                w, h = max(1, round(fw * self.res)), max(1, round(fh * self.res))
                items.append((i, face, w, h))
        area = sum(w * h for _, _, w, h in items)
        width = self.min_width
        while width < max(w for _, _, w, _ in items) or width * width * 2 < area:
            width *= 2
        while True:
            placed, x, y, row = {}, 0, 0, 0
            for i, face, w, h in sorted(items, key=lambda it: (-it[3], -it[2])):
                if x + w > width:
                    x, y, row = 0, y + row, 0
                placed[(i, face)] = (x, y, w, h)
                x += w
                row = max(row, h)
            height = y + row
            if not self.anim or height <= width:
                break
            width *= 2
        if self.anim:
            height = width                         # animation frames are square
        else:
            height = max(1, height)
        return placed, width, height

    # ---------- writing ----------

    def build(self):
        placed, width, height = self._pack()
        boxes = self.frames[self.paint_frame]
        anim = max(1, self.anim)
        image = np.zeros((height * anim, width, 4))
        for frame in range(anim):
            for (i, face), (px, py, w, h) in placed.items():
                b = boxes[i]
                s = (np.arange(w) + 0.5) / w
                t = (np.arange(h) + 0.5) / h
                S, T = np.meshgrid(s, t)
                X, Y, Z = b.point(face, S, T)
                ctx = Ctx(b, face, X, Y, Z, S, T, w, h, frame, anim)
                color, alpha = b.mat(ctx)
                color = np.broadcast_to(np.asarray(color, float), (h, w, 3))
                alpha = np.broadcast_to(np.asarray(alpha, float), (h, w))
                oy = frame * height + py
                image[oy:oy + h, px:px + w, :3] = np.clip(color, 0, 1)
                image[oy:oy + h, px:px + w, 3] = np.clip(alpha, 0, 1)
        self.image = Image.fromarray((image * 255 + 0.5).astype(np.uint8), "RGBA")
        self.placed, self.width, self.height = placed, width, height
        return self

    def _json(self, boxes, name):
        tex = f"{MOD}:model_fx/{self.path}"
        elements = []
        for i, b in enumerate(boxes):
            faces = {}
            for face in b.faces:
                px, py, w, h = self.placed[(i, face)]
                uv = [px * 16 / self.width, py * 16 / self.height, (px + w) * 16 / self.width, (py + h) * 16 / self.height]
                faces[face] = {"uv": [round(v, 5) for v in uv], "texture": "#0"}
            e = {"name": b.name or f"cube{i}", "from": [round(v, 4) for v in b.frm], "to": [round(v, 4) for v in b.to]}
            if b.rot is not None:
                e["rotation"] = {"angle": b.rot[1], "axis": b.rot[0], "origin": [round(v, 4) for v in b.rot[2]]}
            e["faces"] = faces
            elements.append(e)
        lines = ",\n    ".join(json.dumps(e, separators=(",", ":")) for e in elements)
        head = {"credit": f"Circle Of Inevitability - {self.credit} Generated by tools/model_fx",
                "texture_size": [self.width, self.height], "textures": {"0": tex, "particle": tex}}
        text = json.dumps(head, indent=2)[:-2] + ',\n  "elements": [\n    ' + lines + "\n  ]\n}\n"
        return text

    def write(self):
        os.makedirs(os.path.dirname(os.path.join(MODELS, self.path)), exist_ok=True)
        os.makedirs(os.path.dirname(os.path.join(TEXTURES, self.path)), exist_ok=True)
        if len(self.frames) == 1:
            with open(os.path.join(MODELS, self.path + ".json"), "w") as f:
                f.write(self._json(self.frames[0], self.path))
        else:
            for n, boxes in enumerate(self.frames):
                with open(os.path.join(MODELS, f"{self.path}_{n}.json"), "w") as f:
                    f.write(self._json(boxes, f"{self.path}_{n}"))
        png = os.path.join(TEXTURES, self.path + ".png")
        self.image.save(png, optimize=True)
        meta = png + ".mcmeta"
        if self.anim:
            with open(meta, "w") as f:
                json.dump({"animation": {"frametime": self.frametime}}, f)
        elif os.path.exists(meta):
            os.remove(meta)
        return self

    # ---------- preview ----------

    def preview(self, yaw=-35.0, pitch=22.0, ppx=6, tex_frame=0, bg=(38, 36, 44)):
        """Renders every shape side by side, seen from the front-left and a little above."""
        shots = [self._render(boxes, yaw, pitch, ppx, tex_frame) for boxes in self.frames]
        w = sum(s.width for s in shots) + 8 * (len(shots) - 1)
        h = max(s.height for s in shots)
        out = Image.new("RGBA", (w, h), bg + (255,))
        x = 0
        for s in shots:
            out.alpha_composite(s, (x, h - s.height))
            x += s.width + 8
        return out

    def _render(self, boxes, yaw, pitch, ppx, tex_frame):
        cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
        cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

        def cam(p):
            x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
            x, z = x * cy - z * sy, x * sy + z * cy                 # turn round the up axis
            y, z = y * cp - z * sp, y * sp + z * cp                 # tip toward the camera
            return -x, y, z                                         # camera at -Z looking +Z: right = -X

        def turn(b, p):
            if b.rot is None:
                return p
            axis, angle, o = b.rot
            a = math.radians(angle)
            c, s = math.cos(a), math.sin(a)
            x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
            if axis == "x":
                y, z = y * c - z * s, y * s + z * c
            elif axis == "y":
                x, z = x * c + z * s, -x * s + z * c
            else:
                x, y = x * c - y * s, x * s + y * c
            return x + o[0], y + o[1], z + o[2]

        shade = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}
        polys = []
        tex = np.asarray(self.image, float) / 255.0
        for i, b in enumerate(boxes):
            for face in b.faces:
                n = NORMALS[face]
                # turned normal: difference of two turned points
                p0 = turn(b, (0, 0, 0)) if b.rot else (0, 0, 0)
                p1 = turn(b, n) if b.rot else n
                nn = cam((p1[0] - p0[0] + 8, p1[1] - p0[1] + 8, p1[2] - p0[2] + 8))
                if nn[2] >= -1e-6:
                    continue                                         # faces away from the camera
                px, py, w, h = self.placed[(i, face)]
                k = 1.0 if self.preview_unshaded else shade[face]
                for a in range(w):
                    for c in range(h):
                        col = tex[tex_frame * self.height + py + c, px + a]
                        if col[3] < 0.05:
                            continue
                        corners = []
                        for (ss, tt) in ((a / w, c / h), ((a + 1) / w, c / h), ((a + 1) / w, (c + 1) / h), (a / w, (c + 1) / h)):
                            q = b.point(face, ss, tt)
                            q = cam(turn(b, (float(q[0]), float(q[1]), float(q[2]))))
                            corners.append(q)
                        depth = sum(q[2] for q in corners) / 4
                        color = tuple(int(v * 255) for v in (col[0] * k, col[1] * k, col[2] * k)) + (int(col[3] * 255),)
                        polys.append((depth, [(q[0], q[1]) for q in corners], color))
        if not polys:
            return Image.new("RGBA", (16, 16))
        xs = [p[0] for _, ps, _ in polys for p in ps]
        ys = [p[1] for _, ps, _ in polys for p in ps]
        minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
        size = (int((maxx - minx) * ppx) + 8, int((maxy - miny) * ppx) + 8)
        img = Image.new("RGBA", size, (0, 0, 0, 0))
        draw = ImageDraw.Draw(img, "RGBA")
        for depth, ps, color in sorted(polys, key=lambda q: -q[0]):
            draw.polygon([((x - minx) * ppx + 4, (maxy - y) * ppx + 4) for x, y in ps], fill=color)
        return img


# =====================================================================================
# Painting flat pictures (webs, seals, cracks) at 4x and sampling them on plates
# =====================================================================================

def plate(frm, to, image, name="plate", both=True):
    """A flat plate lying on the ground whose top (and bottom) shows 'image' stretched over its x/z extent."""
    arr = np.asarray(image.convert("RGBA"), float) / 255.0
    H, W = arr.shape[:2]
    x0, z0, x1, z1 = frm[0], frm[2], to[0], to[2]

    def mat(ctx):
        u = np.clip(((ctx.x - x0) / (x1 - x0) * W).astype(int), 0, W - 1)
        v = np.clip(((ctx.z - z0) / (z1 - z0) * H).astype(int), 0, H - 1)
        px = arr[v, u]
        return px[..., :3], px[..., 3]

    return Box(frm, to, mat, faces=("up", "down") if both else ("up",), name=name)


def supersample(draw_fn, size, factor=4):
    """Draws at factor x size with draw_fn(ImageDraw, scale) and shrinks it down: smooth lines."""
    big = Image.new("RGBA", (size * factor, size * factor), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(big, "RGBA"), factor, big)
    return big.resize((size, size), Image.LANCZOS)


def octagon(y0, y1, half, mat, cx=8.0, cz=8.0, twist=0.0, faces=("north", "south", "east", "west"), name="ring", **p):
    """Two cubes, one turned 45 degrees: an eight-sided ring or block. twist turns both by 22.5 (or -22.5)."""
    a = Box((cx - half, y0, cz - half), (cx + half, y1, cz + half), mat,
            rot=("y", twist, (cx, y0, cz)) if twist else None, faces=faces, name=name + "_a", **p)
    second = 45 if twist == 0 else (-22.5 if twist > 0 else 22.5)
    b = Box((cx - half, y0, cz - half), (cx + half, y1, cz + half), mat,
            rot=("y", second, (cx, y0, cz)), faces=faces, name=name + "_b", **p)
    return [a, b]
