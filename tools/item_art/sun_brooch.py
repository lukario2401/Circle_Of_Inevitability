"""Regenerate the original 32px gold sun brooch icon. Requires Pillow; run from any directory."""
from pathlib import Path
from PIL import Image, ImageDraw
import math

image = Image.new('RGBA', (32, 32))
draw = ImageDraw.Draw(image)
# Twelve tapered gold rays, outlined with aged bronze.
for i in range(12):
    angle = i * math.tau / 12 - math.pi / 2
    points = []
    for radius, delta in [(7, -.20), (13, -.06), (14, 0), (13, .06), (7, .20)]:
        points.append((round(15.5 + math.cos(angle + delta) * radius), round(15.5 + math.sin(angle + delta) * radius)))
    draw.polygon(points, fill='#754022', outline='#44291e')
    draw.line([(round(15.5 + math.cos(angle) * 8), round(15.5 + math.sin(angle) * 8)),
               (round(15.5 + math.cos(angle) * 12), round(15.5 + math.sin(angle) * 12))], fill='#f3c85e', width=1)
# Raised bezel, amber enamel and a bright central solar jewel.
draw.ellipse((7, 7, 24, 24), fill='#44291e')
draw.ellipse((8, 8, 23, 23), fill='#a66a30', outline='#ffe39a')
draw.ellipse((10, 10, 21, 21), fill='#fff0a2', outline='#d39b3e')
draw.ellipse((11, 11, 20, 20), fill='#e78b26')
draw.ellipse((12, 12, 19, 19), fill='#ffbd42')
draw.ellipse((13, 12, 18, 17), fill='#ffe796')
draw.rectangle((14, 12, 15, 14), fill='#fff9d5')
draw.point((21, 12), fill='#ffffff')
draw.line((11, 22, 19, 22), fill='#664022')
output = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/circle_of_inevitability/textures/item/sun_brooch.png'
output.parent.mkdir(parents=True, exist_ok=True)
image.save(output)
print(output)
