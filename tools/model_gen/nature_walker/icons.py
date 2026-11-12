"""Native 32-pixel silhouettes, readable at wheel size; no fonts in gameplay textures."""
import math
from PIL import Image, ImageDraw
from catalog import ABILITIES, KEYS, UPGRADES, STATUSES, PARTICLES

DARK=(24,40,33,255); GREEN=(73,143,78,255); LIGHT=(163,202,111,255)
GOLD=(227,184,87,255); PALE=(229,236,207,255); BROWN=(119,78,49,255)
PINK=(225,139,150,255); RED=(190,69,92,255); SPIRIT=(133,211,191,255)
PURPLE=(167,123,188,255); STONE=(132,151,146,255); WHITE=(255,255,255,255)


def icon(name):
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    def line(points,c=GREEN,w=2):d.line(points,fill=c,width=w)
    def leaf(x,y,c=GREEN):
        d.polygon([(x-5,y),(x-3,y-4),(x+3,y-4),(x+5,y-6),(x+4,y),(x,y+3)],fill=c)
        line([(x-3,y+1),(x+3,y-3)],LIGHT,1)
    def seed(x=16,y=18,c=GOLD):
        d.polygon([(x,y-7),(x+5,y-2),(x+4,y+4),(x,y+7),(x-4,y+3),(x-5,y-2)],fill=c)
        line([(x,y-5),(x-2,y),(x,y+4)],PALE,1)
    def sprout(x=16,y=25):
        line([(x,y),(x,y-14)],BROWN);leaf(x-4,y-9);leaf(x+4,y-14,LIGHT)
    def heart(x=16,y=16,c=RED):
        d.polygon([(x-8,y-4),(x-5,y-7),(x-1,y-7),(x,y-5),(x+2,y-7),(x+6,y-7),(x+8,y-4),(x+7,y+1),(x,y+8),(x-7,y+1)],fill=c)
        line([(x-5,y-4),(x-3,y-5)],PINK,2)
    def flower(x=16,y=16):
        for i in range(5):
            a=i*math.tau/5;xx=x+5*math.cos(a);yy=y+5*math.sin(a)
            d.ellipse((xx-3,yy-3,xx+3,yy+3),fill=PINK)
        d.rectangle((x-2,y-2,x+2,y+2),fill=GOLD)
    def ring(c=GREEN,box=(4,4,27,27)):d.ellipse(box,outline=c,width=2)
    def mushroom(x=16,y=21):
        d.rectangle((x-1,y-9,x+2,y),fill=PALE)
        d.polygon([(x-8,y-8),(x-6,y-14),(x-2,y-17),(x+3,y-17),(x+7,y-12),(x+8,y-8)],fill=PURPLE)
        d.rectangle((x-3,y-14,x-1,y-12),fill=PINK);d.point((x+4,y-11),fill=PALE)
    def roots(c=BROWN):
        for i,x in enumerate((5,11,19,26)):
            line([(16,27),(x,22),(x+(-1)**i*2,14),(x+(-1)**i*4,7+i%2*3)],c,3)
            line([(x,20),(x-3,16)],PALE,1)
    def scythe():
        line([(11,28),(14,6)],BROWN,3)
        d.polygon([(12,5),(20,5),(26,9),(28,15),(23,11),(17,9),(12,9)],fill=GOLD)
        line([(12,21),(16,20)],GREEN,3)
    def golem(c):
        d.rectangle((11,4,20,11),fill=c);d.rectangle((8,12,23,23),fill=c)
        d.rectangle((3,13,6,23),fill=c);d.rectangle((25,13,28,23),fill=c)
        for x in (9,19):d.rectangle((x,23,x+4,28),fill=c)
        for x in (13,18):d.point((x,8),fill=RED)
        d.rectangle((14,15,17,19),fill=SPIRIT)
    def skull():
        d.polygon([(9,5),(22,5),(26,10),(24,20),(20,20),(20,25),(12,25),(12,20),(7,20),(5,11)],fill=PALE)
        for x in (10,19):d.rectangle((x,12,x+3,15),fill=DARK)
        d.polygon([(16,17),(14,20),(18,20)],fill=DARK)
    def tree(c=GREEN):
        line([(16,28),(16,10)],BROWN,3)
        d.polygon([(16,2),(22,7),(21,10),(28,15),(24,20),(7,20),(3,15),(10,9),(9,7)],fill=c)
        line([(16,12),(16,21),(10,16)],LIGHT,1)
    def sun():
        d.ellipse((10,8,22,20),fill=GOLD)
        for i in range(8):
            a=i*math.pi/4;line([(16+9*math.cos(a),14+9*math.sin(a)),(16+12*math.cos(a),14+12*math.sin(a))],GOLD,1)
    if name in ('thorn_seed','sow'):
        seed(c=BROWN);leaf(19,8);line([(8,25),(6,18),(9,20)],PALE)
    elif name=='heartfruit_seed':seed(c=RED);leaf(19,9);d.point((16,17),fill=PALE)
    elif name in ('treatment','given_life'):
        heart(c=GREEN);d.rectangle((14,8,17,22),fill=PALE);d.rectangle((9,13,22,16),fill=PALE)
        if name=='given_life':ring(GOLD)
    elif name=='soul_suture':
        for i in range(6):
            y=5+i*4;line([(8,y),(21,y+3)],SPIRIT,2)
        line([(9,3),(9,27)],PALE,1);line([(23,4),(23,29)],PALE,1)
    elif name in ('cross_breed','biology'):
        leaf(11,10);mushroom(22,25);line([(5,24),(11,18),(18,23),(25,15)],GOLD,2)
    elif name=='toxin_bloom':
        mushroom();d.ellipse((3,23,28,27),fill=GREEN)
        for x,y in ((5,9),(26,5),(25,18),(7,3)):d.rectangle((x,y,x+1,y+1),fill=LIGHT)
    elif name=='seed_catalyzation':sprout();line([(24,4),(19,13),(24,13),(20,22)],GOLD,2)
    elif name=='quicken_life':sprout();d.arc((3,3,28,28),20,295,fill=GOLD,width=2);d.polygon([(5,5),(4,13),(11,10)],fill=GOLD)
    elif name=='command_flora':
        leaf(16,23);line([(4,5),(7,13),(16,10),(24,13),(28,5)],GOLD,3)
        for x in (4,16,28):d.rectangle((x-1,3,x+1,5),fill=PALE)
    elif name in ('weather_ritual','weather_storm'):
        for x,y in ((8,11),(16,7),(23,11)):d.ellipse((x-5,y-4,x+5,y+5),fill=STONE)
        for x in (7,14,24):line([(x,20),(x-2,25)],SPIRIT)
        if name=='weather_storm':line([(18,14),(14,21),(20,21),(15,29)],GOLD)
    elif name=='weather_clear':sun();d.ellipse((3,21,14,25),fill=PALE)
    elif name in ('reap','harvest'):scythe()
    elif name=='blossoming_land':
        d.polygon([(2,25),(16,19),(30,25),(16,30)],fill=GREEN)
        flower(10,14);flower(23,10)
    elif name=='barren_land':
        d.polygon([(2,25),(16,19),(30,25),(16,30)],fill=BROWN)
        line([(16,24),(16,4)],BROWN,3);line([(16,13),(9,7)],BROWN);line([(16,18),(24,11)],BROWN)
    elif name=='great_harvest':scythe();ring(GOLD);seed(6,21,GREEN)
    elif name in ('underground_travel','earth'):
        d.polygon([(2,14),(16,9),(29,14),(28,25),(17,29),(2,24)],fill=BROWN)
        d.arc((8,13,25,28),170,360,fill=DARK,width=5)
        line([(16,3),(16,16),(12,12),(16,16),(20,12)],GOLD)
    elif name=='nature_walk':
        tree();d.ellipse((12,15,20,28),fill=DARK);line([(13,21),(24,21),(21,18),(24,21),(21,24)],SPIRIT)
    elif name=='swamp':
        for i in range(3):d.ellipse((3+i*3,12+i*3,28-i*3,25-i),outline=BROWN,width=2)
        line([(9,12),(9,5),(5,3)],GREEN);d.ellipse((17,8,21,12),fill=LIGHT)
    elif name=='wrath_of_nature':roots(GREEN);line([(16,2),(12,10),(18,10),(13,18)],GOLD)
    elif name=='child_of_the_oak':
        d.polygon([(5,5),(16,2),(27,5),(25,20),(16,29),(7,20)],fill=BROWN)
        line([(16,26),(16,9)],GOLD);leaf(13,12);leaf(20,17)
    elif name in ('stone_golem','mud_golem','steel_golem'):golem({'stone_golem':STONE,'mud_golem':BROWN,'steel_golem':SPIRIT}[name])
    elif name=='bear_form':
        for x in (7,24):d.ellipse((x-4,3,x+3,10),fill=BROWN)
        d.rectangle((5,7,26,25),fill=BROWN);d.rectangle((10,19,21,27),fill=GOLD)
        for x in (10,21):d.rectangle((x,13,x+1,14),fill=PALE)
        d.rectangle((13,19,18,21),fill=DARK)
    elif name=='verdant_barrier':
        for x in (5,14,23):line([(x,28),(x,5)],BROWN,3)
        for y in (10,20):line([(3,y),(28,y)],GREEN,3)
        flower(10,10);mushroom(23,29)
    elif name in ('world_of_nature','bountiful_earth'):
        ring(SPIRIT if name=='bountiful_earth' else LIGHT);tree(GOLD if name=='bountiful_earth' else GREEN)
        d.arc((3,3,28,28),0,180,fill=BROWN,width=3)
        if name=='bountiful_earth':d.rectangle((14,23,17,27),fill=SPIRIT)
    elif name=='earthquake':
        d.polygon([(2,13),(15,7),(30,13),(29,25),(15,30),(2,25)],fill=BROWN)
        line([(18,9),(13,15),(19,18),(11,28)],DARK,3)
        for x in (4,26):line([(x,4),(x-2,7),(x,9)],GOLD)
    elif name=='stone_bird':
        d.polygon([(2,6),(11,13),(16,10),(21,13),(30,6),(26,20),(20,20),(16,27),(12,20),(6,20)],fill=STONE)
        d.rectangle((13,8,18,16),fill=STONE);d.point((14,10),fill=RED);d.polygon([(18,12),(23,14),(18,15)],fill=GOLD)
    elif name=='life_aura':ring(LIGHT);heart(c=GREEN);d.arc((7,7,24,24),180,320,fill=GOLD,width=1)
    elif name in ('mutate','alchemy'):
        for i in range(7):
            y=4+i*4;x=16+int(6*math.sin(i));line([(x,y),(32-x,y)],PINK,1)
            d.rectangle((x-1,y-1,x+1,y+1),fill=GREEN);d.rectangle((30-x,y-1,33-x,y+1),fill=PURPLE)
    elif name in ('brood_form','brood_form_complete'):
        roots(GREEN);heart(16,18,PINK);mushroom(9,18)
        if name=='brood_form_complete':ring(GOLD);d.rectangle((24,5,25,16),fill=GOLD)
    elif name=='seed_of_rebirth':seed(c=SPIRIT);d.arc((3,3,28,28),15,320,fill=GOLD,width=2);leaf(23,7)
    elif name=='awaken':tree();d.rectangle((14,12,17,17),fill=SPIRIT);ring(GOLD)
    elif name in ('evil_spirits','return'):
        d.polygon([(9,5),(22,5),(25,11),(25,23),(29,29),(21,25),(17,29),(12,25),(5,29),(7,13)],fill=SPIRIT)
        for x in (11,19):d.rectangle((x,12,x+2,15),fill=DARK)
    elif name=='descent_of_death':skull();line([(3,5),(3,19),(1,16),(3,19),(5,16)],BROWN);line([(29,5),(29,19)],BROWN)
    elif name=='return_to_earth':skull();d.rectangle((3,24,28,28),fill=BROWN);leaf(24,25)
    elif name=='maternal_embrace':
        d.polygon([(10,3),(22,3),(27,10),(23,29),(9,29),(5,10)],fill=BROWN)
        line([(9,8),(24,13),(8,18),(23,23)],GREEN,3);d.rectangle((15,10,17,22),fill=GOLD)
    elif name=='desolation':tree(BROWN);d.ellipse((11,9,21,18),fill=DARK);d.arc((3,3,28,28),20,290,fill=PALE,width=1)
    elif name=='root_eruption':roots();line([(2,28),(9,25),(15,29),(23,25),(30,28)],GOLD)
    elif name=='embrace_of_the_earth':
        roots();d.polygon([(16,2),(20,12),(17,26),(14,26),(12,12)],fill=PALE);d.rectangle((15,16,17,23),fill=SPIRIT)
    elif name in ('growths','verdancy','nourished_by_earth'):sprout();ring(GOLD if name=='verdancy' else BROWN)
    elif name.startswith('mutation_'):
        kind=name[9:]
        if kind=='organs':heart(c=PINK)
        elif kind=='fungus':mushroom()
        elif kind=='vines':roots(GREEN)
        else:
            for x in (9,16,23):
                line([(x,28),(x,7)],GOLD)
                for y in (8,12,16):line([(x-3,y-2),(x,y),(x+3,y-2)],GOLD,2)
    elif name=='brood_constructs':golem(STONE)
    elif name=='brood_hybrids':leaf(10,12);heart(21,22,GREEN)
    elif name=='brood_spirits':skull();ring(SPIRIT)
    elif name=='reaped':scythe();line([(3,25),(8,29),(26,12)],RED,2)
    elif name=='rebirth_ward':seed(c=SPIRIT);ring(GOLD)
    elif name=='mutagen':seed(c=PURPLE);d.rectangle((15,10,17,24),fill=PINK)
    elif name=='madness':
        for i in range(30):
            a=i*.6;r=i*.35;d.point((16+int(r*math.cos(a)),16+int(r*math.sin(a))),fill=PURPLE)
    elif name=='life_sense':
        d.polygon([(2,16),(10,9),(21,9),(30,16),(21,23),(10,23)],fill=LIGHT);d.ellipse((12,10,20,22),fill=DARK);d.point((16,14),fill=PALE)
    elif name=='grave_sight':skull();d.rectangle((3,27,28,29),fill=SPIRIT)
    elif name=='weather_forecast':sun();line([(5,25),(26,25)],SPIRIT)
    elif name=='mothers_domain':tree();ring(GOLD)
    else:raise ValueError('Missing icon motif: '+name)
    # One-pixel dark silhouette separates bright details from Minecraft skies and terrain.
    alpha=im.getchannel('A');outline=Image.new('RGBA',im.size)
    for dx,dy in ((-1,0),(1,0),(0,-1),(0,1)):
        outline.paste(DARK,(dx,dy,32+dx,32+dy),alpha)
    return Image.alpha_composite(outline,im)


def particle(name):
    im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im);c=WHITE
    if name in ('green_ring','brown_ring'):d.ellipse((1,1,14,14),outline=c,width=2 if name=='green_ring' else 1)
    elif name in ('petal','leaf'):
        d.polygon([(3,12),(2,7),(6,2),(13,1),(13,7),(9,12)],fill=c)
        d.line((3,12,12,3),fill=(170,170,170,255),width=1)
    elif name=='spore':
        d.rectangle((7,8,9,14),fill=c);d.pieslice((1,1,14,13),180,360,fill=c)
    elif name in ('pollen','grave_mote'):
        d.ellipse((5,5,10,10),fill=c)
        if name=='grave_mote':d.line((8,2,8,13),fill=c)
    elif name=='soil_crumb':d.polygon([(3,4),(9,2),(13,6),(10,12),(4,13),(2,8)],fill=c)
    elif name in ('thorn','root_splinter'):d.polygon([(2,14),(7,7),(8,1),(12,12)],fill=c)
    elif name=='soul_thread':d.line([(3,14),(9,11),(5,7),(11,3)],fill=c,width=2)
    elif name=='healing_spark':
        d.rectangle((7,2,8,13),fill=c);d.rectangle((2,7,13,8),fill=c)
    elif name=='rain_drop':d.line((10,1,5,14),fill=c,width=2)
    elif name=='wheat_grain':d.ellipse((5,2,10,13),fill=c)
    elif name=='mutation_cell':
        d.ellipse((2,2,13,13),outline=c,width=2);d.rectangle((6,6,9,9),fill=c)
    return im


def generate(root):
    def save(im,rel):
        p=root/rel;p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
    for n in dict.fromkeys(ABILITIES+KEYS+UPGRADES):save(icon(n),f'textures/gui/ability_menus/nature_walker/{n}.png')
    for n in dict.fromkeys(ABILITIES+UPGRADES+STATUSES):
        save(icon(n).resize((16,16),Image.Resampling.NEAREST),f'textures/gui/nature_walker/{n}.png')
    for kind in ('organs','fungus','wheat','vines'):
        for level in range(1,4):
            im=icon('mutation_'+kind).resize((16,16),Image.Resampling.NEAREST);d=ImageDraw.Draw(im)
            for i in range(3):d.rectangle((2+i*5,14,4+i*5,15),fill=GOLD if i<level else DARK)
            save(im,f'textures/gui/nature_walker/mutation_{kind}_{level}.png')
    save(icon('bountiful_earth').resize((16,16),Image.Resampling.NEAREST),'textures/gui/pathway_icons/nature_walker.png')
    for n in PARTICLES:save(particle(n),f'textures/particle/fx/nature_walker/{n}.png')
