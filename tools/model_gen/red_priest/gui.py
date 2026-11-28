"""Small pictures for the Red Priest uniqueness: the HUD corruption sigil, the red war flash (full screen) and the
icons of its own abilities (32x32, the ability list's size).

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


def hailstorm(d):
    d.ellipse([6, 5, 26, 13], fill=(120, 130, 150))
    for x, y in ((10, 16), (16, 19), (22, 15), (13, 23), (20, 24)):
        d.polygon([(x, y - 2), (x + 2, y), (x, y + 2), (x - 2, y)], fill=(200, 235, 255), outline=(120, 180, 220))


def war_cyclone(d):
    for i, (w, y) in enumerate(((20, 7), (16, 12), (12, 17), (8, 22), (4, 26))):
        d.arc([16 - w // 2 + i, y - 2, 16 + w // 2 + i, y + 3], 0, 300, fill=(220, 220, 230) if i % 2 == 0 else GOLD, width=2)


def banner_of_conquest(d):
    d.line([(10, 5), (10, 27)], fill=(150, 110, 50), width=2)
    d.polygon([(11, 6), (24, 6), (24, 18), (17, 15), (11, 18)], fill=CRIMSON, outline=BLOOD)
    d.polygon([(14, 12), (14, 9), (16, 11), (17.5, 8), (19, 11), (21, 9), (21, 12)], fill=GOLD)
    d.point([(10, 4)], fill=GOLD)


def subjugation(d):
    d.polygon([(10, 9), (10, 5), (13, 7), (16, 4), (19, 7), (22, 5), (22, 9)], fill=GOLD)
    d.ellipse([11, 10, 21, 19], fill=(60, 40, 44))
    d.polygon([(8, 27), (11, 19), (21, 19), (24, 27)], fill=(60, 40, 44))
    d.line([(6, 14), (26, 24)], fill=CRIMSON, width=2)


def spear_of_judgement(d):
    d.line([(6, 26), (22, 10)], fill=(150, 30, 30), width=2)
    d.polygon([(21, 7), (27, 5), (25, 11), (22, 12), (20, 10)], fill=EMBER, outline=GOLD)
    for x, y in ((8, 21), (11, 26), (5, 18)):
        d.point([(x, y)], fill=EMBER)


def endless_war(d):
    for x, h in ((9, 10), (16, 14), (23, 10)):
        d.ellipse([x - 3, 25 - h - 6, x + 3, 25 - h], fill=(40, 20, 24), outline=CRIMSON)
        d.rectangle([x - 3, 25 - h, x + 3, 26], fill=(40, 20, 24), outline=CRIMSON)
        d.point([(x - 1, 25 - h - 3), (x + 1, 25 - h - 3)], fill=(255, 60, 60))
    d.line([(4, 27), (28, 27)], fill=EMBER, width=1)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    corruption().save(OUT / 'corruption.png')
    war_flash().save(OUT / 'war_flash.png')
    for name, fn in (('iron_blood', iron_blood), ('storm_of_war', storm_of_war), ('conquest', conquest),
                     ('crimson_dominion', crimson_dominion), ('hailstorm', hailstorm), ('war_cyclone', war_cyclone),
                     ('banner_of_conquest', banner_of_conquest), ('subjugation', subjugation),
                     ('spear_of_judgement', spear_of_judgement), ('endless_war', endless_war)):
        icon(fn).save(OUT / f'{name}.png')
    print('wrote', OUT)


if __name__ == '__main__':
    main()
