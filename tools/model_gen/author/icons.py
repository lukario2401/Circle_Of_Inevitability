"""Original native-resolution pixel art; no raster resizing of existing pathway art."""
import math
from PIL import Image, ImageDraw
from catalog import ABILITIES, KEYS, STATUSES, PARTICLES

GOLD = (227,186,95,255)
PALE = (255,234,180,255)
JADE = (69,182,154,255)
DARK = (26,55,66,255)
IVORY = (229,224,207,255)
VIOLET = (151,125,212,255)
INK = (39,27,63,255)


def icon(name, size=32):
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    def line(points,c=GOLD,w=1):d.line(points,fill=c,width=w)
    def eye(cx=16,cy=15,r=11,c=GOLD):
        d.polygon([(cx-r,cy),(cx-r//2,cy-5),(cx+3,cy-6),(cx+r,cy),
                   (cx+r//2,cy+5),(cx-3,cy+6)],fill=DARK,outline=c)
        d.ellipse((cx-4,cy-4,cx+4,cy+4),fill=JADE,outline=PALE)
        d.rectangle((cx-1,cy-3,cx+1,cy+3),fill=INK)
        d.point((cx+2,cy-2),fill=PALE)
    def mask(cracked=False):
        d.polygon([(8,5),(23,5),(25,17),(21,25),(16,28),(10,24),(6,15)],fill=IVORY,outline=GOLD)
        line([(9,13),(13,12),(14,14)],DARK,2);line([(18,14),(19,12),(23,13)],DARK,2)
        line([(13,22),(18,23),(20,21)],VIOLET)
        if cracked:line([(17,5),(14,12),(18,17),(14,22),(17,27)],INK,2)
    def ring(c=JADE):
        d.ellipse((3,7,28,25),outline=c,width=2);d.arc((7,10,24,22),20,310,fill=GOLD)
    def star(x,y,c=PALE):
        line([(x-2,y),(x+2,y)],c);line([(x,y-2),(x,y+2)],c)
    def book():
        d.polygon([(3,8),(12,6),(16,9),(20,6),(28,8),(28,25),(19,23),(16,26),(12,23),(3,25)],fill=IVORY,outline=GOLD)
        line([(16,10),(16,24)],INK)
        for y in (12,16,20):line([(6,y),(12,y-1)],VIOLET);line([(20,y-1),(25,y)],JADE)
    def quill():
        d.polygon([(8,25),(14,8),(24,3),(27,9),(22,19)],fill=JADE,outline=GOLD)
        line([(6,28),(23,7)],PALE,2)
        for x,y in ((14,16),(17,12),(20,8)):line([(x,y),(x+5,y+1)],DARK)
    def gate():
        d.rectangle((6,10,9,28),fill=JADE);d.rectangle((23,10,26,28),fill=JADE)
        line([(7,11),(7,6),(16,2),(25,6),(25,11)],GOLD,3)
        d.rectangle((11,10,21,27),fill=INK);star(16,15,VIOLET)
    def dragon():
        d.polygon([(3,5),(13,10),(24,5),(29,3),(26,14),(28,19),(22,24),(13,26),(8,21),(11,17),(5,16)],fill=JADE,outline=GOLD)
        d.polygon([(7,15),(13,13),(17,18),(11,19)],fill=DARK)
        d.rectangle((20,12,23,13),fill=PALE);d.point((22,13),fill=INK)
        line([(17,22),(23,21)],DARK,2)
    def lotus():
        for points,c in [([(16,25),(4,18),(3,10),(13,15)],JADE),
                         ([(16,25),(28,18),(29,10),(19,15)],JADE),
                         ([(16,25),(10,15),(16,4),(22,15)],IVORY)]:
            d.polygon(points,fill=c,outline=GOLD)
        d.ellipse((6,25,26,28),outline=VIOLET)
    if name in ('observe','observation','insight','discern','eye'):
        eye()
        if name=='discern':
            for x in (8,16,24):star(x,4)
        elif name=='observe':line([(7,25),(12,25),(14,23)],JADE)
    elif name in ('spectator_state','psychological_invisibility','persona','virtual_persona'):
        mask()
        if name=='psychological_invisibility':
            for y in (11,16,21):d.rectangle((0,y,31,y+1),fill=(0,0,0,0))
        elif name in ('persona','virtual_persona'):line([(4,23),(3,9),(8,3),(21,3)],VIOLET,2);star(26,25)
        else:ring()
    elif name in ('awe','fear','emotions'):
        eye(c=VIOLET)
        for x in (6,16,26):line([(x,4),(x,8)],PALE)
        line([(12,24),(16,28),(20,24)],VIOLET)
    elif name in ('frenzy','exposed','psychoanalysis'):
        mask(True)
        if name=='psychoanalysis':d.ellipse((18,17,28,27),outline=JADE,width=2);line([(26,26),(30,30)],GOLD,2)
        elif name=='exposed':star(26,5)
        else:line([(3,7),(1,13),(4,20)],VIOLET,2)
    elif name in ('placate','therapy','lucidity','dream_anchor'):
        lotus()
        if name=='dream_anchor':line([(16,1),(16,7)],VIOLET,2);ring(VIOLET)
        if name=='lucidity':star(26,5)
        if name=='therapy':line([(1,4),(7,4)],PALE);line([(4,1),(4,7)],PALE)
    elif name in ('suggestion','hypnosis','battle_hypnosis'):
        line([(16,2),(16,19)],GOLD)
        d.polygon([(16,15),(21,21),(16,28),(11,21)],fill=JADE,outline=PALE)
        d.arc((4,9,28,29),5,175,fill=VIOLET,width=2)
        if name=='battle_hypnosis':
            for x in (4,27):line([(x,6),(x,27)],GOLD)
        if name=='hypnosis':d.arc((1,3,30,31),200,345,fill=JADE)
    elif name in ('sleep','dreams'):
        d.polygon([(10,9),(22,9),(25,24),(7,24)],fill=INK,outline=GOLD)
        d.ellipse((12,12,20,21),fill=VIOLET);d.ellipse((16,11,22,19),fill=INK)
        line([(12,8),(12,4),(20,4),(20,8)],JADE);star(26,6)
        if name=='dreams':ring(VIOLET)
    elif name in ('dream_entry','mind_walk'):
        gate()
        if name=='mind_walk':line([(11,24),(20,24),(17,21)],PALE,2)
    elif name=='nightmare':
        eye(c=VIOLET)
        for x in (10,16,22):d.polygon([(x-1,21),(x+1,21),(x,28)],fill=PALE)
    elif name=='dream_maze':
        for box in ((3,3,28,28),(7,7,24,24),(11,11,20,20)):d.rectangle(box,outline=VIOLET,width=2)
        d.rectangle((2,22,9,25),fill=(0,0,0,0));d.rectangle((23,7,29,10),fill=(0,0,0,0));star(16,16)
    elif name in ('dragon_form','life_creation','mind','scale'):
        dragon()
        if name=='life_creation':star(5,26);star(26,27)
        if name=='mind':d.arc((1,1,30,30),185,350,fill=VIOLET)
    elif name=='mind_storm':
        for r in (13,9,5):d.arc((16-r,16-r,16+r,16+r),35+r*4,320+r,fill=JADE,width=2)
        for x,y in ((5,4),(27,7),(6,26)):star(x,y)
    elif name=='manipulate':
        line([(4,6),(16,9),(28,6)],GOLD,2)
        for x,y in ((6,18),(16,25),(26,19)):
            line([(x,8),(x,y-3)],VIOLET);d.ellipse((x-2,y-2,x+2,y+2),fill=JADE)
    elif name in ('foreshadow','fate_script','authorship','scripted'):
        book()
        if name=='foreshadow':eye(16,14,8)
        elif name=='fate_script':line([(9,12),(23,23),(26,19)],INK,2);star(24,3)
        elif name=='scripted':d.rectangle((13,11,19,18),fill=JADE)
        else:quill()
    elif name=='plot_twist':
        quill();line([(3,10),(7,4),(13,4)],VIOLET,2);line([(4,5),(7,4),(8,8)],PALE)
    elif name=='envision':
        d.polygon([(15,3),(19,7),(18,20),(15,23),(12,20),(13,7)],fill=JADE,outline=PALE)
        line([(8,22),(23,22)],GOLD,3);line([(15,23),(15,29)],IVORY,3);star(25,10)
    elif name=='sea_of_minds':
        gate()
        for y in (21,25,29):line([(2,y),(7,y-2),(13,y),(19,y-2),(26,y),(30,y-2)],VIOLET,2)
    elif name=='strain':
        dragon();line([(17,3),(13,13),(20,18),(16,29)],VIOLET,2)
    elif name=='resistance':
        d.polygon([(16,3),(27,8),(25,22),(16,29),(7,22),(5,8)],fill=DARK,outline=GOLD)
        lotus()
    elif name in PARTICLES:
        if name=='ripple':ring()
        elif name=='thought':eye(r=7)
        elif name=='thread':line([(4,27),(10,18),(19,15),(27,3)],PALE,2)
        elif name=='ink':
            d.polygon([(16,3),(8,18),(9,25),(16,29),(23,25),(24,18)],fill=VIOLET)
        elif name=='page':book()
        elif name=='dream_shard':d.polygon([(17,3),(24,12),(14,29),(7,19)],fill=JADE,outline=PALE)
        elif name=='spiral':
            for r in (12,8,4):d.arc((16-r,16-r,16+r,16+r),30,300,fill=VIOLET,width=2)
        elif name=='quill_star':star(16,16);star(7,7);star(25,25)
    else:raise ValueError('Missing art: '+name)
    if size!=32:im=im.resize((size,size),Image.Resampling.NEAREST)
    return im


def save(im,path):
    path.parent.mkdir(parents=True,exist_ok=True);im.save(path)


def generate(root):
    for name in dict.fromkeys(ABILITIES+KEYS):save(icon(name),root/f'textures/gui/ability_menus/author/{name}.png')
    for name in dict.fromkeys(ABILITIES+STATUSES):save(icon(name,16),root/f'textures/gui/author/{name}.png')
    for count in range(6):
        im=Image.new('RGBA',(32,8));d=ImageDraw.Draw(im)
        for i in range(5):d.polygon([(3+i*6,1),(5+i*6,4),(3+i*6,6),(1+i*6,4)],fill=JADE if i<count else DARK,outline=GOLD)
        save(im,root/f'textures/gui/author/insight_{count}.png')
    save(icon('observe',16),root/'textures/gui/pathway_icons/author.png')
    for name in PARTICLES:
        im=icon(name,16);alpha=im.getchannel('A');im=im.convert('L').convert('RGBA');im.putalpha(alpha)
        save(im,root/f'textures/particle/fx/author/{name}.png')
