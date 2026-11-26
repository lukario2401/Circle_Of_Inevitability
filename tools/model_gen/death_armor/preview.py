"""Dependency-free textured preview of the Vestments of Death rig at rest, front and back, plus a close-up of the hood.

This is a model preview, not a Minecraft screenshot: each face samples the generated atlas with simple
directional shading. Run python tools/model_gen/death_armor/preview.py after generate.py.
"""
import json
import math
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate import ASSETS, ROOT, SET, png  # noqa: E402


def read_png(path):
    data = path.read_bytes()
    pos, chunks = 8, []
    width = height = 0
    while pos < len(data):
        length = int.from_bytes(data[pos:pos + 4], 'big')
        tag = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        if tag == b'IHDR':
            width, height = int.from_bytes(body[:4], 'big'), int.from_bytes(body[4:8], 'big')
        elif tag == b'IDAT':
            chunks.append(body)
        pos += 12 + length
    raw = zlib.decompress(b''.join(chunks))
    stride = width * 4
    out = bytearray(width * height * 4)
    prev = bytearray(stride)
    for y in range(height):
        f = raw[y * (stride + 1)]
        line = bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        for i in range(stride):
            a = line[i - 4] if i >= 4 else 0
            b = prev[i]
            c = prev[i - 4] if i >= 4 else 0
            if f == 1:
                line[i] = (line[i] + a) & 255
            elif f == 2:
                line[i] = (line[i] + b) & 255
            elif f == 3:
                line[i] = (line[i] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        out[y * stride:(y + 1) * stride] = line
        prev = line
    return width, height, out


def rotate(p, r):
    x, y, z = p
    a, b, c = r
    y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    x, z = x * math.cos(b) + z * math.sin(b), -x * math.sin(b) + z * math.cos(b)
    return (x * math.cos(c) - y * math.sin(c), x * math.sin(c) + y * math.cos(c), z)


rig = json.loads((ASSETS / f'armor/{SET}.rig.json').read_text())
TW, TH, TEX = read_png(ASSETS / f'textures/entity/equipment/humanoid/{SET}.png')
bones = {b['name']: b for b in rig['bones']}
DENSITY = TW / rig['texture_size'][0]          # texels per model pixel


def transform(p, b):
    p = rotate(p, b['rotation'])
    p = (p[0] + b['pivot'][0], p[1] + b['pivot'][1], p[2] + b['pivot'][2])
    return transform(p, bones[b['parent']]) if b['parent'] != 'root' else p


# A plain grey mannequin under the armor, so gaps read as body rather than background.
BODY = [('head', (-4, -8, -4), (8, 8, 8)), ('body', (-4, 0, -2), (8, 12, 4)), ('right_arm', (-3, -2, -2), (4, 12, 4)),
        ('left_arm', (-1, -2, -2), (4, 12, 4)), ('right_leg', (-2, 0, -2), (4, 12, 4)), ('left_leg', (-2, 0, -2), (4, 12, 4))]

# Each face: corner order (as indices of the 8 box corners) running along u then v, the UV rect key, light.
CORNERS = [(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0), (0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)]
FACES = {'north': ((0, 1, 3), 1.0), 'south': ((5, 4, 6), .62), 'west': ((4, 0, 7), .8), 'east': ((1, 5, 2), .8),
         'top': ((4, 5, 0), 1.15), 'bottom': ((3, 2, 7), .5)}


def uv_rects(c):
    u, v = c['uv']
    w, h, d = c['size']
    return {'top': (u + d, v, u + d + w, v + d), 'bottom': (u + d + w, v, u + d + 2 * w, v + d),
            'west': (u, v + d, u + d, v + d + h), 'north': (u + d, v + d, u + d + w, v + d + h),
            'east': (u + d + w, v + d, u + 2 * d + w, v + d + h), 'south': (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}


quads = []      # (three corner points: origin, u-end, v-end) in model space, uv rect or flat color, light
for b in rig['bones']:
    for c in b['cubes']:
        o, s = c['origin'], c['size']
        pts = [transform(tuple(o[k] + s[k] * v[k] for k in range(3)), b) for v in CORNERS]
        for face, ((i0, iu, iv), light) in FACES.items():
            quads.append(((pts[i0], pts[iu], pts[iv]), uv_rects(c)[face], light))
for name, o, s in BODY:
    b = bones[name]
    pts = [transform(tuple(o[k] + s[k] * v[k] for k in range(3)), b) for v in CORNERS]
    for face, ((i0, iu, iv), light) in FACES.items():
        quads.append(((pts[i0], pts[iu], pts[iv]), None, light * .55))


def render(width, height, views):
    pixels = bytearray([30, 31, 36, 255] * (width * height))
    depth = [float('inf')] * (width * height)
    for cx, cy, scale, yaw, pitch in views:
        for (p0, pu, pv), rect, light in quads:
            proj = []
            for p in (p0, pu, pv):
                x, y, z = rotate(p, (0, yaw, 0))
                y, z = y * math.cos(pitch) + z * math.sin(pitch), -y * math.sin(pitch) + z * math.cos(pitch)
                proj.append((cx + x * scale, cy + y * scale, z))
            a, b, c = proj
            ux, uy, uz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
            vx, vy, vz = c[0] - a[0], c[1] - a[1], c[2] - a[2]
            det = ux * vy - uy * vx
            if abs(det) < 1e-6:
                continue
            xs = (a[0], b[0], c[0], b[0] + vx)
            ys = (a[1], b[1], c[1], b[1] + vy)
            x0, x1 = max(0, int(min(xs))), min(width - 1, int(math.ceil(max(xs))))
            y0, y1 = max(0, int(min(ys))), min(height - 1, int(math.ceil(max(ys))))
            for py in range(y0, y1 + 1):
                for px in range(x0, x1 + 1):
                    dx, dy = px + .5 - a[0], py + .5 - a[1]
                    s = (dx * vy - dy * vx) / det
                    t = (ux * dy - uy * dx) / det
                    if s < 0 or s > 1 or t < 0 or t > 1:
                        continue
                    z = a[2] + uz * s + vz * t
                    i = py * width + px
                    if z >= depth[i]:
                        continue
                    if rect is None:
                        color = (150, 150, 156)
                    else:
                        tu = min(TW - 1, max(0, int((rect[0] + (rect[2] - rect[0]) * s) * DENSITY - 1e-4)))
                        tv = min(TH - 1, max(0, int((rect[1] + (rect[3] - rect[1]) * t) * DENSITY - 1e-4)))
                        k = (tv * TW + tu) * 4
                        color = TEX[k:k + 3]
                    depth[i] = z
                    pixels[i * 4:i * 4 + 3] = bytes(min(255, int(ch * light)) for ch in color)
    return pixels


if __name__ == '__main__':
    W, H = 1000, 700
    pixels = render(W, H, [(250, 330, 12, .45, .12), (750, 330, 12, math.pi + .45, .12)])
    out = ROOT / f'docs/images/{SET}_armor_preview.png'
    png(out, W, H, pixels)
    print(out)
    if len(sys.argv) > 1:
        # Close-ups for design work: python preview.py out.png [model y to centre on] [zoom] [yaw]
        y = float(sys.argv[2]) if len(sys.argv) > 2 else -4
        zoom = float(sys.argv[3]) if len(sys.argv) > 3 else 34
        yaw = float(sys.argv[4]) if len(sys.argv) > 4 else .35
        pixels = render(W, H, [(260, 350 - y * zoom, zoom, yaw, .05), (760, 350 - y * zoom, zoom, math.pi + yaw, .05)])
        png(Path(sys.argv[1]), W, H, pixels)
