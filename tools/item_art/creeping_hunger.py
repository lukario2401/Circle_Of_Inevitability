"""Regenerate the Creeping Hunger art: one 32px glove icon per tier, the storing-artifact wheel icons and the
full-screen "hunger vision" overlay used by its disorientation. Requires Pillow; run from any directory."""
from pathlib import Path
import math
import random
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/circle_of_inevitability/textures'

# Darker leather and brighter, redder veins/eyes as the tier rises.
TIERS = {
    1: dict(leather='#4a3b3f', shade='#2b2124', vein='#6e2a3a', glow='#a33a4f', eyes=0, teeth='#cfc3a8'),
    2: dict(leather='#45343a', shade='#271c20', vein='#7f2638', glow='#bf3550', eyes=1, teeth='#d8cbb0'),
    3: dict(leather='#3f2d35', shade='#22171c', vein='#932235', glow='#d93a55', eyes=2, teeth='#e2d4b6'),
    4: dict(leather='#38262f', shade='#1e1318', vein='#a81e30', glow='#ee3d55', eyes=3, teeth='#ecdcbc'),
    5: dict(leather='#301e28', shade='#190e14', vein='#c0182b', glow='#ff4a5e', eyes=5, teeth='#f5e6c4'),
}


def glove(tier):
    c = TIERS[tier]
    img = Image.new('RGBA', (32, 32))
    d = ImageDraw.Draw(img)
    # Fingers (four, slightly splayed and hooked like claws) and the thumb.
    fingers = [(9, 3, 12, 14), (13, 2, 16, 13), (17, 3, 20, 14), (21, 5, 24, 15)]
    for x0, y0, x1, y1 in fingers:
        d.rounded_rectangle((x0, y0, x1, y1), radius=1, fill=c['leather'], outline=c['shade'])
        d.point((x0 + 1, y0), fill=c['teeth'])                       # a nail turned into a fang
    d.polygon([(6, 14), (4, 11), (3, 12), (5, 17), (8, 19)], fill=c['leather'], outline=c['shade'])
    # Palm and the cuff.
    d.rounded_rectangle((7, 12, 25, 24), radius=3, fill=c['leather'], outline=c['shade'])
    d.rectangle((9, 24, 23, 29), fill=c['shade'])
    d.line((9, 26, 23, 26), fill=c['vein'])
    d.line((9, 28, 23, 28), fill=c['leather'])
    # The mouth in the palm, with teeth.
    d.ellipse((11, 15, 21, 22), fill='#120709', outline=c['vein'])
    for x in range(12, 21, 2):
        d.polygon([(x, 16), (x + 1, 16), (x + .5, 18)], fill=c['teeth'])
        d.polygon([(x, 21), (x + 1, 21), (x + .5, 19)], fill=c['teeth'])
    d.line((13, 19, 19, 19), fill=c['glow'])                        # the tongue's glow
    # Veins creeping up the fingers.
    rnd = random.Random(tier)
    for x0, y0, x1, y1 in fingers[:2 + min(tier, 2)]:
        x = (x0 + x1) // 2
        for y in range(y1, y0 + 2, -1):
            if rnd.random() < .7:
                d.point((x + rnd.choice((-1, 0, 0, 1)), y), fill=c['vein'])
    # Eyes opening across the back of the hand, more with each tier.
    spots = [(9, 13), (23, 13), (15, 25), (19, 25), (24, 19)]
    for x, y in spots[:c['eyes']]:
        d.point((x, y), fill=c['glow'])
        d.point((x + 1, y), fill='#ffd7dc')
    return img


def icon_empty():
    img = Image.new('RGBA', (32, 32))
    d = ImageDraw.Draw(img)
    d.ellipse((5, 5, 26, 26), outline='#5b3a44', width=2)
    for a in range(0, 360, 45):
        r = math.radians(a)
        d.point((15.5 + math.cos(r) * 13, 15.5 + math.sin(r) * 13), fill='#7a4452')
    return img


def icon_stored():
    img = Image.new('RGBA', (32, 32))
    d = ImageDraw.Draw(img)
    d.ellipse((4, 4, 27, 27), fill='#2a1218', outline='#8a2a3c', width=2)
    d.ellipse((10, 10, 21, 21), fill='#a33a4f')
    d.ellipse((13, 12, 18, 17), fill='#ffb6c1')
    return img


def icon_discard():
    img = Image.new('RGBA', (32, 32))
    d = ImageDraw.Draw(img)
    d.ellipse((4, 4, 27, 27), fill='#1a0c10', outline='#8a2a3c', width=2)
    d.line((10, 10, 21, 21), fill='#e05a6a', width=3)
    d.line((21, 10, 10, 21), fill='#e05a6a', width=3)
    return img


def hunger_vision():
    """A transparent centre ringed by dark-red veins: stretched over the whole screen."""
    w, h = 320, 180
    shade = Image.new('L', (w, h))
    px = shade.load()
    for y in range(h):
        for x in range(w):
            dx, dy = (x - w / 2) / (w / 2), (y - h / 2) / (h / 2)
            dist = math.sqrt(dx * dx * .9 + dy * dy * 1.1)
            px[x, y] = int(max(0, min(1, (dist - .45) / .6)) ** 1.6 * 225)
    veins = Image.new('L', (w, h))
    d = ImageDraw.Draw(veins)
    rnd = random.Random(7)
    for _ in range(46):
        edge = rnd.randrange(4)
        x, y = [(rnd.uniform(0, w), 0), (rnd.uniform(0, w), h), (0, rnd.uniform(0, h)), (w, rnd.uniform(0, h))][edge]
        angle = math.atan2(h / 2 - y, w / 2 - x) + rnd.uniform(-.6, .6)
        width = 3
        for step in range(rnd.randrange(8, 20)):
            nx, ny = x + math.cos(angle) * 4, y + math.sin(angle) * 4
            d.line((x, y, nx, ny), fill=255, width=max(1, width))
            if rnd.random() < .12:
                width -= 1
            angle += rnd.uniform(-.45, .45)
            x, y = nx, ny
    veins = veins.filter(ImageFilter.GaussianBlur(.6))
    img = Image.new('RGBA', (w, h))
    out = img.load()
    sp, vp = shade.load(), veins.load()
    for y in range(h):
        for x in range(w):
            s, v = sp[x, y], vp[x, y]
            if v > 20:
                out[x, y] = (150 + v // 4, 10, 24, min(255, 120 + v // 2))
            else:
                out[x, y] = (40, 0, 8, s)
    return img


def save(img, relative):
    path = ROOT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print(path)


for tier in TIERS:
    save(glove(tier), f'item/creeping_hunger_{tier}.png')
save(icon_empty(), 'gui/storing_artifact/empty.png')
save(icon_stored(), 'gui/storing_artifact/stored.png')
save(icon_discard(), 'gui/storing_artifact/discard.png')
save(hunger_vision(), 'gui/creeping_hunger/hunger_vision.png')
