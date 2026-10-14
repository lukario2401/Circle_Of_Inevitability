"""A small software renderer for Java block-model JSON, to check models without the game.

python3 render.py <assets dir> <out.png> model[,frame...] ...
Each argument is a model path under models/model_fx (without .json). Several models joined by commas are
drawn side by side in one row (frames). Each row is shown from three sides: front-left, front-right, back.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'east': 0.6, 'west': 0.6}


def rot_matrix(axis, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    if axis == 'x':
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if axis == 'y':
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def face_corners(f, t, face):
    """Corners (top-left, top-right, bottom-right, bottom-left) as seen on the texture, MC defaults."""
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {
        'north': [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        'south': [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        'east': [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        'west': [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        'up': [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        'down': [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
    }[face]


NORMALS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}


def load(assets, name):
    with open(os.path.join(assets, 'models/model_fx', name + '.json')) as f:
        model = json.load(f)
    tex_id = model['textures']['0'].split(':')[1]
    tex = Image.open(os.path.join(assets, 'textures', tex_id + '.png')).convert('RGBA')
    return model, np.asarray(tex).astype(float)


def quads(model, tex, steps=6):
    """Every face cut into small coloured quads: (points[4] in model space, normal, rgba)."""
    out = []
    th, tw = tex.shape[0], tex.shape[1]
    for el in model['elements']:
        f, t = el['from'], el['to']
        R, o = np.eye(3), np.zeros(3)
        if 'rotation' in el:
            R = rot_matrix(el['rotation']['axis'], el['rotation']['angle'])
            o = np.array(el['rotation']['origin'], float)
        for face, data in el['faces'].items():
            c = [np.array(p, float) for p in face_corners(f, t, face)]
            c = [R @ (p - o) + o for p in c]
            n = R @ np.array(NORMALS[face], float)
            u0, v0, u1, v1 = data['uv']
            for i in range(steps):
                for j in range(steps):
                    a, b = i / steps, (i + 1) / steps
                    p, q = j / steps, (j + 1) / steps

                    def at(s, r):
                        top = c[0] + (c[1] - c[0]) * s
                        bot = c[3] + (c[2] - c[3]) * s
                        return top + (bot - top) * r
                    pts = [at(a, p), at(b, p), at(b, q), at(a, q)]
                    us = u0 + (u1 - u0) * (a + b) / 2
                    vs = v0 + (v1 - v0) * (p + q) / 2
                    px = min(tw - 1, max(0, int(us / 16 * tw)))
                    py = min(th - 1, max(0, int(vs / 16 * th)))
                    col = tex[py, px] * np.array([SHADE[face]] * 3 + [1])
                    out.append((pts, n, col))
    return out


def draw(model_quads, yaw, pitch, center, scale, size=260):
    img = Image.new('RGBA', (size, size), (48, 52, 60, 255))
    d = ImageDraw.Draw(img, 'RGBA')
    R = rot_matrix('x', pitch) @ rot_matrix('y', yaw)
    view = np.array([0, 0, -1.0])                    # the camera looks toward +z (from -z)
    polys = []
    for pts, n, col in model_quads:
        if col[3] < 8:
            continue
        nn = R @ n
        if nn @ view <= 0:                           # back face
            continue
        tp = [R @ (p - center) for p in pts]
        depth = sum(p[2] for p in tp) / 4
        screen = [(size / 2 - p[0] * scale, size / 2 - p[1] * scale) for p in tp]
        polys.append((depth, screen, tuple(int(v) for v in col)))
    polys.sort(key=lambda x: -x[0])
    for _, screen, col in polys:
        d.polygon(screen, fill=col)
    return img


def main():
    assets, out = sys.argv[1], sys.argv[2]
    rows = [arg.split(',') for arg in sys.argv[3:]]
    views = [(-35, 20), (35, 20), (180, 15)]
    cell = 260
    width = max(len(r) for r in rows) * len(views) * cell
    sheet = Image.new('RGBA', (width, len(rows) * (cell + 16)), (30, 30, 34, 255))
    d = ImageDraw.Draw(sheet)
    for ri, names in enumerate(rows):
        for ni, name in enumerate(names):
            model, tex = load(assets, name)
            q = quads(model, tex)
            allp = np.array([p for pts, _, _ in q for p in pts])
            lo, hi = allp.min(0), allp.max(0)
            center = (lo + hi) / 2
            scale = (cell * 0.8) / max(1e-3, np.linalg.norm(hi - lo))
            for vi, (yaw, pitch) in enumerate(views):
                img = draw(q, yaw, pitch, center, scale, cell)
                x = (ni * len(views) + vi) * cell
                sheet.paste(img, (x, ri * (cell + 16) + 16))
            d.text(((ni * len(views)) * cell + 4, ri * (cell + 16) + 2), name, fill=(220, 220, 220, 255))
    sheet.save(out)


if __name__ == '__main__':
    main()
