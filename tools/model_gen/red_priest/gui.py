"""Small pictures for the Red Priest uniqueness: the HUD corruption sigil, the red war flash (full screen) and the
icons of its four own abilities (32x32, the ability list's size).

Run from any directory: python tools/model_gen/red_priest/gui.py   (Pillow required)
Writes textures/gui/red_priest/*.png.
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ASSETS = Path(__file__).resolve().parents[3] / 'src/main/resources/assets/circle_of_inevitability'
OUT = ASSETS / 'textures/gui/red_priest'
CRIMSON, BLOOD, GOLD, EMBER, DARK = (200, 26, 40), (110, 10, 20), (230, 178, 72), (255, 130, 50), (24, 10, 12)


def corruption():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(8, 1), (10, 6), (8, 15), (6, 6)], fill=CRIMSON)          # a spearhead / drop of blood
    d.line([(8, 3), (8, 12)], fill=EMBER)
    d.line([(4, 6), (6, 7)], fill=GOLD)
    d.line([(12, 6), (10, 7)], fill=GOLD)
    return img


def war_flash():
    w, h = 256, 144
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(5)
    for y in range(h):
        for x in range(w):
            dx, dy = (x - w / 2) / (w / 2), (y - h / 2) / (h / 2)
            d = math.sqrt(dx * dx + dy * dy)
            a = max(0.0, min(1.0, (d - .45) / .65)) ** 1.6
            a *= .85 + rnd.random() * .15
            px[x, y] = (150 + int(60 * a), 8, 16, int(200 * a))
    return img.filter(ImageFilter.GaussianBlur(2))


def icon(draw_fn):
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([1, 1, 30, 30], fill=DARK, outline=BLOOD, width=2)
    draw_fn(d)
    return img


def iron_blood(d):
    d.polygon([(16, 6), (23, 16), (16, 26), (9, 16)], fill=CRIMSON, outline=(120, 120, 130))
    d.line([(16, 10), (16, 22)], fill=(200, 200, 210), width=2)
    d.line([(12, 16), (20, 16)], fill=(200, 200, 210), width=2)


def storm_of_war(d):
    d.ellipse([6, 6, 26, 14], fill=(70, 66, 80))
    d.line([(17, 12), (13, 19), (18, 19), (14, 27)], fill=(250, 230, 120), width=2)


def conquest(d):
    d.polygon([(7, 22), (7, 12), (11, 16), (16, 8), (21, 16), (25, 12), (25, 22)], fill=GOLD, outline=(140, 100, 40))
    d.rectangle([7, 22, 25, 25], fill=(170, 120, 50))
    d.point([(16, 14)], fill=CRIMSON)


def crimson_dominion(d):
    for x in (9, 16, 23):
        d.line([(x, 5), (x, 22)], fill=EMBER, width=2)
        d.polygon([(x - 2, 22), (x + 2, 22), (x, 27)], fill=CRIMSON)
    d.arc([3, 3, 28, 28], 200, 340, fill=CRIMSON, width=2)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    corruption().save(OUT / 'corruption.png')
    war_flash().save(OUT / 'war_flash.png')
    for name, fn in (('iron_blood', iron_blood), ('storm_of_war', storm_of_war), ('conquest', conquest),
                     ('crimson_dominion', crimson_dominion)):
        icon(fn).save(OUT / f'{name}.png')
    print('wrote', OUT)


if __name__ == '__main__':
    main()
