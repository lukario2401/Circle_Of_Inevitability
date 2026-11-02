"""Deterministic native-resolution pixel art; no text is baked into gameplay icons."""
import math
from PIL import Image, ImageDraw
from catalog import ABILITIES, KEYS, UPGRADES, PRINCIPLES, STATUSES, PARTICLES

GOLD = (239, 182, 55, 255)
PALE = (255, 224, 139, 255)
WHITE = (255, 253, 237, 255)
IVORY = (209, 217, 218, 255)
DARK = (42, 29, 22, 255)
ORANGE = (220, 104, 30, 255)
TEAL = (91, 179, 174, 255)


def star(d, x, y, r, color=WHITE, n=4):
    d.polygon([(x+math.cos(-math.pi/2+i*math.pi/n)*(r if i%2 == 0 else r*.36),
                y+math.sin(-math.pi/2+i*math.pi/n)*(r if i%2 == 0 else r*.36))
               for i in range(n*2)], fill=color)


def sun(d, x=16, y=15, r=6, white=False):
    for i in range(12):
        a=i*math.tau/12
        d.line([(x+math.cos(a)*(r+2), y+math.sin(a)*(r+2)),
                (x+math.cos(a)*(r+4), y+math.sin(a)*(r+4))], fill=PALE)
    d.ellipse((x-r,y-r,x+r,y+r), fill=WHITE if white else GOLD)
    d.arc((x-r+1,y-r+1,x+r-1,y+r-1),190,290,fill=WHITE)


def person(d, x=16, y=13, color=IVORY):
    d.ellipse((x-2,y-5,x+2,y-1),fill=color)
    d.polygon([(x-2,y),(x+2,y),(x+4,y+9),(x-4,y+9)],fill=color)


def halo(d, box=(5,7,27,15), white=False):
    d.ellipse(box, outline=WHITE if white else GOLD, width=2)
    d.arc((box[0]+2,box[1]+1,box[2]-2,box[3]-1),190,320,fill=PALE)


def scales(d, y=11):
    d.line((16,7,16,25),fill=WHITE,width=2)
    d.line((7,y,25,y),fill=GOLD,width=2)
    for x in (8,24):
        d.line((x,y,x-4,y+8),fill=PALE)
        d.line((x,y,x+4,y+8),fill=PALE)
        d.pieslice((x-4,y+5,x+4,y+11),0,180,fill=GOLD)
    d.line((11,25,21,25),fill=GOLD,width=2)
    star(d,16,6,3)


def scroll(d, holy=False):
    d.rectangle((8,5,23,25),fill=WHITE,outline=GOLD)
    d.line((7,5,24,5),fill=PALE,width=3)
    for y in (10,13,16):d.line((11,y,20,y),fill=ORANGE)
    sun(d,18,22,2) if holy else star(d,19,22,3,GOLD)


def flame(d, x=16, y=15, white=False):
    d.polygon([(x,y-10),(x+2,y-3),(x+6,y-6),(x+7,y+3),(x+4,y+9),
               (x-4,y+9),(x-7,y+4),(x-5,y-4),(x-2,y)],fill=GOLD)
    d.polygon([(x,y-3),(x+3,y+4),(x+1,y+8),(x-3,y+6)],fill=WHITE if white else PALE)


def bird(d, complete=True):
    for s in (-1,1):
        pts=[(16,15),(16+s*6,11),(16+s*12,7),(16+s*10,17),(16+s*4,20)]
        d.polygon(pts,fill=WHITE if complete else GOLD)
        for k in range(3):d.line((16+s*(6+k*2),12,16+s*(5+k*2),19-k),fill=GOLD)
    d.ellipse((13,9,19,21),fill=GOLD)
    d.polygon([(15,19),(12,27),(16,24),(20,27),(17,19)],fill=PALE)
    d.polygon([(15,10),(14,6),(18,8),(21,10)],fill=WHITE)
    d.point((17,9),fill=DARK)
    halo(d,(11,3,21,7))


def eye(d):
    d.polygon([(4,15),(10,10),(21,10),(28,15),(21,20),(10,20)],fill=WHITE)
    d.ellipse((11,10,21,20),fill=GOLD)
    star(d,16,15,4,DARK)


def sword(d, spear=False):
    d.line((9,26,23,5),fill=GOLD,width=3)
    d.polygon([(23,4),(24,13),(19,11)],fill=WHITE)
    if spear:
        for x,y in ((13,16),(17,10)):
            d.polygon([(x,y),(x-6,y-3),(x-3,y+3)],fill=WHITE)
    else:d.line((9,17,19,23),fill=PALE,width=2)


def motif(name):
    im=Image.new('RGBA',(32,32)); d=ImageDraw.Draw(im)
    if name in ('hymn_of_valor','hymn_of_renewal','hymn','courage'):
        for x,y in ((11,20),(21,15)):
            d.ellipse((x-4,y-1,x+1,y+3),fill=GOLD)
            d.line((x+1,y,x+1,y-11),fill=WHITE,width=2)
        if name=='hymn_of_renewal':
            d.line((23,21,23,27),fill=TEAL,width=2); d.line((20,24,26,24),fill=TEAL,width=2)
        else:star(d,8,9,3)
    elif name in ('sunshine','light','sun','sunlight','believers','night_vision'):
        sun(d,16,15,6 if name!='sun' else 8,name=='sunshine')
        if name=='believers':person(d,16,23,WHITE)
    elif name in ('daytime','domain','radiance'):
        sun(d,16,11,4)
        d.arc((4,10,28,33),180,360,fill=WHITE,width=2)
        d.line((4,25,28,25),fill=GOLD,width=2)
        if name=='domain':
            for x in (5,26):d.line((x,14,x,25),fill=PALE,width=2)
            d.ellipse((11,19,21,27),outline=WHITE)
    elif name in ('holy_light','light_of_holiness','theurgy','white_ray','pure_white_light'):
        if name=='white_ray':
            d.line((5,27,27,5),fill=GOLD,width=5)
            d.line((5,27,27,5),fill=WHITE,width=2)
            star(d,25,7,5)
            return im
        w=5 if name in ('light_of_holiness','pure_white_light') else 2
        d.polygon([(16-w,5),(16+w,5),(16+w+3,24),(16-w-3,24)],fill=GOLD)
        d.line((16,5,16,24),fill=WHITE,width=max(2,w))
        halo(d,(5,21,27,27),name=='pure_white_light')
        star(d,16,5,4)
    elif name in ('sun_halo','purification_halo','justice_halo','blessing','horror_immunity'):
        halo(d,(5,5,27,13),name=='purification_halo');person(d,16,16)
        if name=='justice_halo':d.line((9,16,23,16),fill=GOLD);star(d,16,17,4)
        elif name=='purification_halo':star(d,24,23,4)
    elif name in ('holy_oath','solar_oath','oath','fervor'):
        d.polygon([(7,5),(25,5),(23,21),(16,27),(9,21)],fill=GOLD)
        d.polygon([(10,8),(22,8),(20,19),(16,23),(12,19)],fill=WHITE)
        flame(d,16,15) if name=='fervor' else star(d,16,15,5,GOLD)
        if name=='solar_oath':sun(d,24,7,2)
    elif name in ('cleave','spear','spear_of_light','retribution'):
        sword(d,name in ('spear','spear_of_light'))
        if name=='spear_of_light':star(d,7,9,4)
    elif name in ('fire_of_light','nemesis','pact_breach'):
        flame(d)
        if name=='pact_breach':d.line((7,23,25,7),fill=WHITE,width=2)
    elif name in ('contract','holy_contract','notarization','pacts','order'):
        scroll(d,name=='holy_contract')
        if name=='pacts':d.arc((2,17,14,29),20,320,fill=GOLD,width=2)
    elif name in ('authenticate','faith_appraisal','holy_eye','evil_detection','appraised'):
        eye(d)
        if name in ('faith_appraisal','appraised'):d.line((5,25,26,25),fill=GOLD);star(d,25,6,3)
        if name=='authenticate':star(d,24,7,4)
    elif name in ('amplify','nullify','amplified'):
        halo(d,(5,5,27,27))
        if name=='nullify':d.line((8,24,24,8),fill=WHITE,width=3)
        else:
            d.polygon([(16,7),(24,17),(19,17),(19,25),(13,25),(13,17),(8,17)],fill=WHITE)
    elif name in ('purification','purified_body','purity'):
        d.polygon([(16,5),(24,19),(23,25),(16,28),(9,25),(8,19)],fill=WHITE)
        star(d,16,19,5,GOLD); star(d,25,8,3)
    elif name in ('flaring_sun','inextinguishable_light'):
        sun(d,16,13,6)
        for x in (8,13,19,24):d.line((x,24,x-2,28),fill=WHITE)
        if name=='inextinguishable_light':d.arc((4,4,28,28),30,320,fill=WHITE,width=2)
    elif name in ('holy_equipment','horror_ward'):
        d.polygon([(10,6),(13,9),(19,9),(22,6),(28,11),(25,16),(22,14),
                   (23,26),(9,26),(10,14),(7,16),(4,11)],fill=WHITE)
        d.line((16,10,16,25),fill=GOLD,width=2);star(d,16,16,4,GOLD)
    elif name in ('define_justice','justice','judgment','justice_trial','guilt','condemned','incarnation_of_order'):
        scales(d,10 if name!='judgment' else 14)
        if name in ('judgment','justice_trial'):d.line((16,3,16,17),fill=WHITE,width=3)
        if name=='condemned':d.line((5,26,27,5),fill=ORANGE,width=2)
    elif name in ('solar_envoy','light_body'):
        bird(d)
    elif name in ('praise_the_sun','anchored'):
        for k in range(3):d.line((4,10+k*5,20-k*3,10+k*5),fill=GOLD,width=2)
        d.polygon([(19,5),(28,15),(19,25),(22,15)],fill=WHITE)
        if name=='anchored':halo(d,(17,20,28,27))
    elif name=='holy_kingdom':
        for x in (6,22):d.rectangle((x,10,x+4,26),fill=WHITE)
        d.arc((6,3,26,21),180,360,fill=GOLD,width=3)
        sun(d,16,8,2);d.line((5,27,27,27),fill=GOLD,width=2)
    elif name in ('servant_of_faith','mercy'):
        person(d,16,15,WHITE)
        halo(d,(10,4,22,9))
        for x in (7,24):d.line((x,16,x,23),fill=GOLD,width=2)
        if name=='mercy':d.arc((6,9,26,29),0,180,fill=TEAL,width=2)
    elif name=='fortune':
        for x,y in ((12,11),(20,11),(12,19),(20,19)):d.ellipse((x-4,y-4,x+3,y+3),fill=GOLD)
        d.line((16,16,19,27),fill=WHITE,width=2)
    elif name=='spiritual_intuition':
        d.arc((8,6,24,26),180,360,fill=WHITE,width=3)
        d.line((8,16,8,22,24,22,24,16),fill=GOLD,width=2)
        d.ellipse((14,24,18,27),fill=PALE);star(d,25,7,3)
    else:raise ValueError('Missing authored icon: '+name)
    return im


def framed(name):
    im=Image.new('RGBA',(32,32));p=im.load()
    for y in range(32):
        for x in range(32):
            r=math.hypot(x-15.5,y-15.5)
            if r<15.5:
                p[x,y]=(PALE if x+y<30 else GOLD) if r>14 else DARK if r>12.7 else (53+int(r),48+int(r*.4),44,255)
    layer=motif(name)
    # One dark pixel of keyline keeps bright subjects distinct from the bright rim.
    a=layer.getchannel('A');out=Image.new('RGBA',(32,32));op=out.load()
    for y in range(1,31):
        for x in range(1,31):
            if a.getpixel((x,y))==0 and any(a.getpixel((x+dx,y+dy)) for dx,dy in ((1,0),(-1,0),(0,1),(0,-1))):op[x,y]=DARK
    im.alpha_composite(out); im.alpha_composite(layer)
    return im


def save(assets, rel, im):
    target=assets/rel;target.parent.mkdir(parents=True,exist_ok=True);im.save(target)


def generate(assets):
    for name in dict.fromkeys(ABILITIES+KEYS+UPGRADES):
        save(assets,f'textures/gui/ability_menus/white_angel/{name}.png',framed(name))
    for name in dict.fromkeys(ABILITIES+UPGRADES+STATUSES+PRINCIPLES):
        # Dedicated nearest-neighbour HUD raster with no wheel frame.
        save(assets,f'textures/gui/white_angel/{name}.png',motif(name).resize((16,16),Image.Resampling.NEAREST))
    save(assets,'textures/gui/pathway_icons/white_angel.png',motif('solar_envoy').resize((16,16),Image.Resampling.NEAREST))
    for name in PRINCIPLES:
        save(assets,f'textures/gui/ability_menus/white_angel/principle_{name}.png',framed(name))
    for level in range(6):
        im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
        d.line((3,4,12,4),fill=GOLD);d.line((7,2,7,12),fill=WHITE)
        for i in range(5):d.rectangle((1+i*3,13,2+i*3,15),fill=GOLD if i<level else (90,74,50,255))
        save(assets,f'textures/gui/white_angel/guilt_{level}.png',im)
    for name in PARTICLES:
        im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im);w=(255,255,255,255);g=(180,180,180,255)
        if name.startswith('note'):
            d.ellipse((3,10,8,13),fill=w);d.line((8,11,8,2,13,4),fill=w,width=2)
            if name=='note_1':d.line((12,4,12,9),fill=w);d.ellipse((9,8,13,11),fill=w)
        elif name in ('sun_mote','purify_spark'):star(d,7.5,7.5,7,w,8 if name=='sun_mote' else 4)
        elif name=='golden_flame':
            d.polygon([(8,0),(10,5),(13,4),(14,10),(10,15),(4,14),(1,9),(5,3),(6,8)],fill=g)
            d.polygon([(8,6),(11,11),(8,14),(5,12)],fill=w)
        elif name=='feather':
            d.polygon([(13,1),(14,7),(7,13),(2,13),(3,8),(9,2)],fill=g)
            d.line((2,15,12,3),fill=w)
            for i in range(4):d.line((5+i*2,12-i*2,4+i*2,7-i),fill=w)
        elif name in ('seal','white_ring'):
            d.ellipse((1,1,14,14),outline=w,width=2)
            if name=='seal':star(d,7.5,7.5,5,w,6)
        elif name=='oath_glyph':
            d.polygon([(3,1),(12,1),(11,11),(7,15),(4,11)],outline=w);star(d,7,7,4,w)
        elif name=='eye':
            d.polygon([(0,7),(5,3),(10,3),(15,7),(10,11),(5,11)],outline=w);d.ellipse((6,4,9,10),fill=w)
        elif name=='ray_streak':d.polygon([(7,0),(9,0),(8,15),(6,15)],fill=w)
        elif name=='light_crescent':
            d.arc((0,0,15,15),220,55,fill=w,width=2);d.arc((3,2,15,13),230,35,fill=g)
        elif name=='guilt_mark':d.line((7,1,7,10),fill=w,width=2);d.rectangle((6,13,8,15),fill=w)
        save(assets,f'textures/particle/fx/white_angel/{name}.png',im)
