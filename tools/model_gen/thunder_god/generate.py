"""Original Thunder God pixel art, ModelFx geometry, animation and synthesized audio.
Run from any directory with Python 3, Pillow, NumPy and ffmpeg installed.
No external artwork, recordings, game rules or balance decisions are included.
"""
import argparse
import hashlib
import json
import math
from pathlib import Path
import subprocess
import sys
import tempfile
import wave

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from modelkit import Model, Texture

REPO = Path(__file__).resolve().parents[3]
ASSETS = REPO / 'src/main/resources/assets/circle_of_inevitability'
PREFIX = 'thunder_god'
NS = 'circle_of_inevitability'
SEA = (16, 41, 74, 255)
SILVER = (214, 236, 255, 255)
TEAL = (31, 111, 139, 255)
FOAM = (139, 213, 231, 255)
RED = (192, 57, 43, 255)
EMBER = (255, 141, 99, 255)
DARK = (8, 19, 37, 245)
WHEELS = {
    'tides': 'water_veil sweeping_wave tideheal water_curtain water_sphere corrosive_rain sea_master maelstrom destructive_droplet'.split(),
    'sky': 'windblade gale_burst gale_ride wind_binding windblade_legion sonic_boom cloud_sword incarnation_of_light starfall'.split(),
    'thunder': 'lightning_strike lightning_bolt_arrow chain_lightning lightning_storm magnetic_pole_pull electromagnetic_disruption heavenly_punishment ocean_of_lightning lightning_forest'.split(),
    'voice': 'siren_song discord thunderclap roar death_dirge sound_of_horror requiem roar_of_the_thunder_god tyrants_presence'.split(),
    'storm': 'hurricane tsunami wrath_of_the_sea storm_incarnation storm_hell chasm_of_storms dreadful_heaven banish_into_the_storm'.split(),
}
BASIC = 'harpoon illusory_scales raging_blow wrath'.split()
ABILITIES = BASIC + sum(WHEELS.values(), [])
UPGRADES = 'source_of_wrath water_wall torrential_downpour thunder_strike dance_of_thunder'.split()
STATUSES = 'rage rage_full soaked shock tipsy spent parched fear silence madness corrosion spiritual_intuition aquatic_affinity fear_resistance balance physical_enhancement seas_favor wind_blessed ocean_songster scale_armor home_turf storm_born authority_of_thunder tyrants_aura'.split()
PARTICLES = 'spark arc_0 arc_1 arc_2 raindrop splash foam ripple wind_streak windblade scale cloud rage_ember sound_ring spirit_hand crown rope'.split()
EXTRA_SOUNDS = 'bolt_crack thunder_roll harpoon_hit rope_reel scales_break wave_break rain_loop wind_loop storm_loop rage_gain rage_full wrath_end banish_return kingdom_end melee_swing'.split()
# Information disruption and Dance of Thunder are deliberately silent and invisible.
SILENT = {'electromagnetic_disruption', 'dance_of_thunder'}
SOUNDS = [n for n in ABILITIES + UPGRADES + EXTRA_SOUNDS if n not in SILENT]
CUE_TEXT = {
    'harpoon': 'A harpoon whistles', 'illusory_scales': 'Scales shimmer',
    'raging_blow': 'A furious blow lands', 'wrath': 'Wrath rises',
    'siren_song': 'An ethereal melody calls', 'discord': 'Dissonant voices clash',
    'death_dirge': 'A low dirge tolls', 'requiem': 'A spectral lament rises',
    'sound_of_horror': 'An unnatural shriek echoes',
    'rain_loop': 'Heavy rain falls', 'wind_loop': 'A gale howls', 'storm_loop': 'A storm rumbles',
    'bolt_crack': 'Lightning cracks', 'thunder_roll': 'Thunder rolls',
    'rope_reel': 'A rope draws taut', 'kingdom_end': 'The storm kingdom fades',
    'banish_return': 'The storm releases its captive', 'wrath_end': 'Wrath subsides',
}


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def save(root, path, im):
    path = root / path
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)


def bolt(d, x=16, y=5, s=1, color=SILVER):
    d.polygon([(x+3*s,y),(x-5*s,y+12*s),(x,y+12*s),(x-3*s,y+23*s),
               (x+7*s,y+9*s),(x+2*s,y+9*s)], fill=color)


def wave_icon(d, y=18, tall=False):
    d.polygon([(5,26),(5,y+3),(10,y+1),(12,y-4),(17,y-8 if tall else y-5),
               (23,y-7 if tall else y-4),(27,y-2),(22,y-3),(18,y),(18,y+4),(26,y+6),(27,26)], fill=TEAL)
    d.line([(6,y+3),(10,y+1),(12,y-4),(17,y-8 if tall else y-5),(23,y-7 if tall else y-4),(27,y-2)],fill=FOAM,width=2)
    d.line((7,24,13,23,19,25,25,24),fill=FOAM)


def cloud_icon(d, y=9, face=False):
    d.rectangle((6,y,26,y+6),fill=(77,101,127))
    for x,yy,r in [(10,y,4),(17,y-2,5),(23,y+1,4)]:
        d.ellipse((x-r,yy-r,x+r,yy+r),fill=(77,101,127),outline=(122,151,174))
    if face:
        d.line((9,y+1,13,y+3,15,y+1),fill=SILVER)
        d.line((18,y+1,20,y+3,24,y+1),fill=SILVER)
        d.rectangle((14,y+4,19,y+6),fill=DARK)


def rings(d, cx=16, cy=17, count=3, color=SILVER):
    for i in range(count):
        r=3+i*4; d.ellipse((cx-r,cy-r*.7,cx+r,cy+r*.7),outline=color)


def crown(d, y=7, color=SILVER):
    d.polygon([(7,y),(11,y+4),(16,y-3),(21,y+4),(25,y),(23,y+10),(9,y+10)],fill=color)
    d.line((10,y+7,22,y+7),fill=TEAL,width=2)


def droplet(d, x=16, y=7, size=1, color=FOAM):
    pts=[(x,y),(x-5*size,y+8*size),(x-5*size,y+12*size),(x-2*size,y+15*size),
         (x+3*size,y+15*size),(x+6*size,y+11*size),(x+5*size,y+7*size)]
    d.polygon(pts,fill=color); d.line((x-2*size,y+7*size,x-3*size,y+11*size),fill=SILVER)


def eye(d, red=False):
    c=RED if red else FOAM
    d.polygon([(6,16),(11,12),(21,12),(26,16),(21,20),(11,20)],fill=c)
    d.rectangle((14,12,17,20),fill=DARK)
    d.point((13,14),fill=SILVER)


def glyph(name, size=32):
    """Hand-defined silhouettes; wheel identity is carried by shape and frame color."""
    im=Image.new('RGBA',(32,32)); d=ImageDraw.Draw(im)
    family=next((k for k,v in WHEELS.items() if name in v),'basic')
    accent={'tides':TEAL,'sky':FOAM,'thunder':SILVER,'voice':(120,163,196),'storm':(78,116,162),'basic':RED}.get(family,SILVER)
    d.polygon([(7,1),(24,1),(30,7),(30,24),(24,30),(7,30),(1,24),(1,7)],fill=DARK,outline=accent)
    d.line((6,4,12,4),fill=SILVER); d.line((20,27,26,27),fill=accent)
    if name=='harpoon':
        d.line((7,26,23,8),fill=SILVER,width=2)
        d.polygon([(26,5),(23,15),(22,10),(17,10)],fill=SILVER)
        d.arc((4,18,17,28),20,290,fill=(156,132,101))
    elif name in ('illusory_scales','scale_armor'):
        for row in range(3):
            for col in range(3):
                x=7+col*7+(3 if row%2 else 0); y=7+row*6
                d.polygon([(x,y),(x+6,y),(x+6,y+3),(x+3,y+6),(x,y+3)],fill=TEAL,outline=FOAM)
    elif name in ('raging_blow','physical_enhancement'):
        d.polygon([(7,13),(9,9),(12,9),(12,7),(16,7),(16,9),(21,9),(24,13),(23,21),(18,26),(11,23)],fill=RED,outline=EMBER)
        for x in (11,15,19): d.line((x,11,x,16),fill=DARK)
        d.line((8,22,4,24),fill=SILVER); d.line((24,8,27,5),fill=SILVER)
        if name=='physical_enhancement': d.line((21,23,27,23,24,20,24,26),fill=FOAM)
    elif name in ('wrath','source_of_wrath','rage','rage_full'):
        d.polygon([(8,25),(5,16),(10,19),(9,9),(15,14),(19,4),(21,14),(26,10),(25,22),(20,27)],fill=RED)
        d.polygon([(11,25),(10,19),(15,20),(18,12),(19,21),(22,19),(20,26)],fill=EMBER)
        if name in ('source_of_wrath','rage_full'): crown(d,6,EMBER)
        if name=='wrath': d.line((8,16,12,18,14,16),fill=SILVER); d.line((19,16,21,18,25,16),fill=SILVER)
    elif name in ('water_veil','silence'):
        d.ellipse((9,5,23,25),fill=(66,117,149),outline=SILVER)
        d.rectangle((7,15,25,22),fill=TEAL,outline=FOAM)
        d.line((11,18,21,18),fill=DARK,width=2)
        if name=='water_veil': d.line((9,8,7,22,10,26),fill=FOAM)
    elif name in ('sweeping_wave','tsunami','wrath_of_the_sea','seas_favor','home_turf'):
        wave_icon(d,19,name=='tsunami')
        if name=='wrath_of_the_sea': d.ellipse((12,5,23,16),outline=RED,width=2); d.line((17,4,17,18),fill=RED)
        elif name=='seas_favor': d.line((14,9,20,9,17,6,17,12),fill=SILVER)
        elif name=='home_turf': crown(d,7)
        elif name=='sweeping_wave': d.line((6,9,11,9,9,6,9,12),fill=SILVER)
    elif name=='tideheal':
        droplet(d,16,5,1,TEAL)
        d.rectangle((14,13,18,25),fill=SILVER); d.rectangle((10,17,22,21),fill=SILVER)
    elif name in ('water_curtain','water_wall'):
        d.polygon([(6,7),(24,5),(26,25),(8,27)],fill=TEAL,outline=FOAM)
        for x in (10,15,20): d.line((x,10,x+1,23),fill=FOAM)
        if name=='water_wall': d.line((5,5,5,27,27,27,27,5),fill=SILVER,width=2)
    elif name=='water_sphere':
        d.ellipse((5,5,27,27),fill=TEAL,outline=SILVER,width=2)
        d.arc((8,6,21,23),90,270,fill=FOAM,width=2)
        d.ellipse((14,11,18,15),fill=DARK); d.line((16,16,16,22,12,24,16,22,20,24),fill=DARK,width=2)
    elif name in ('corrosive_rain','torrential_downpour','soaked'):
        if name=='soaked': droplet(d); d.line((5,26,26,26),fill=TEAL,width=2)
        else:
            cloud_icon(d,10)
            for x in (9,16,23): d.line((x,18,x-2,25),fill=FOAM,width=2)
            if name=='corrosive_rain': d.rectangle((13,25,20,27),fill=(132,180,143))
            else: d.line((5,17,5,24),fill=SILVER); d.line((27,17,27,24),fill=SILVER)
    elif name=='sea_master':
        crown(d,7); wave_icon(d,25)
        d.line((16,16,16,24),fill=SILVER)
    elif name in ('maelstrom','hurricane','storm_born'):
        for i in range(5):
            y=7+i*4; w=11-i*2
            d.arc((16-w,y-2,16+w,y+4),0,310,fill=FOAM if i%2 else TEAL,width=2)
        if name=='maelstrom': d.ellipse((13,22,19,26),fill=DARK,outline=SILVER)
        elif name=='storm_born': bolt(d,20,8,.6)
        else: d.line((6,9,4,14,9,13),fill=SILVER)
    elif name in ('destructive_droplet','parched'):
        droplet(d,16,5,1,SILVER if name=='destructive_droplet' else (158,132,105))
        if name=='destructive_droplet':
            for x,y in [(5,10),(25,13),(7,24),(25,24)]: d.line((x,y,x+2,y-2),fill=FOAM)
        else: d.line((16,12,14,17,18,21,16,26),fill=DARK,width=2)
    elif name in ('windblade','windblade_legion'):
        offsets=[0] if name=='windblade' else [-7,0,7]
        for off in offsets:
            d.arc((6+off/3,6+off,27,26+off/2),190,300,fill=SILVER,width=2)
            d.line((9,22+off/2,24,9+off/2),fill=FOAM)
    elif name=='gale_burst':
        rings(d,count=3,color=FOAM)
        for x,y,dx,dy in [(4,16,5,0),(27,16,-5,0),(16,5,0,5),(16,28,0,-5)]:
            d.line((x,y,x+dx,y+dy),fill=SILVER,width=2)
    elif name in ('gale_ride','wind_blessed','balance'):
        d.line((10,8,13,18,23,18),fill=SILVER,width=3)
        for yy in (22,26): d.arc((4,yy-4,26,yy+2),0,160,fill=FOAM,width=2)
        if name=='wind_blessed': d.line((19,7,23,4,26,7),fill=FOAM)
        elif name=='balance': d.line((6,10,25,10,16,22,6,10),fill=TEAL)
    elif name=='wind_binding':
        for y in (8,14,20): d.ellipse((8,y-3,24,y+5),outline=FOAM,width=2)
        d.line((16,7,16,26),fill=SILVER)
    elif name in ('sonic_boom','thunderclap'):
        for i in range(3):
            r=4+i*4; d.arc((16-r,16-r,16+r,16+r),210,510,fill=SILVER,width=2)
        if name=='sonic_boom': d.polygon([(4,10),(15,16),(4,22),(8,16)],fill=FOAM)
        else: bolt(d,12,7,.65)
    elif name=='cloud_sword':
        cloud_icon(d,22)
        d.polygon([(16,3),(20,10),(18,21),(14,21),(12,10)],fill=SILVER)
        d.line((10,20,22,20),fill=TEAL,width=3); d.line((16,21,16,27),fill=SILVER,width=2)
    elif name=='incarnation_of_light':
        d.line((5,25,23,7),fill=FOAM,width=5); d.line((6,26,24,8),fill=SILVER,width=2)
        d.line((7,14,12,9),fill=SILVER); d.line((18,25,24,19),fill=SILVER)
        d.ellipse((20,4,27,11),fill=SILVER)
    elif name=='starfall':
        d.ellipse((16,5,25,14),fill=SILVER,outline=FOAM)
        d.line((20,12,11,23),fill=SILVER,width=3)
        d.line((12,7,5,15),fill=FOAM); d.line((26,16,19,24),fill=FOAM)
        d.ellipse((6,22,26,27),outline=FOAM)
    elif name in ('lightning_strike','thunder_strike','authority_of_thunder','shock'):
        bolt(d,color=SILVER)
        if name=='thunder_strike': cloud_icon(d,7); bolt(d,16,9,.7)
        elif name=='authority_of_thunder': crown(d,6); bolt(d,16,12,.6)
        elif name=='shock': d.line((5,12,8,16,5,20),fill=FOAM); d.line((26,12,23,16,26,20),fill=FOAM)
    elif name=='lightning_bolt_arrow':
        d.arc((5,5,22,28),265,455,fill=FOAM,width=2)
        d.line((12,6,12,26),fill=TEAL)
        d.line((5,20,26,10),fill=SILVER,width=2)
        d.polygon([(28,8),(23,15),(23,11),(19,9)],fill=SILVER)
    elif name=='chain_lightning':
        d.line((6,7,12,13,8,17,21,14,18,21,26,26),fill=SILVER,width=2)
        for x,y in [(6,7),(8,17),(26,26)]:d.ellipse((x-2,y-2,x+2,y+2),fill=FOAM)
    elif name in ('lightning_storm','storm_hell','storm_incarnation'):
        cloud_icon(d,9,name=='storm_incarnation'); bolt(d,16,14,.55)
        if name=='lightning_storm': bolt(d,7,16,.4); bolt(d,25,16,.4)
        elif name=='storm_hell': d.ellipse((5,22,27,28),outline=FOAM); d.line((5,15,4,23),fill=FOAM); d.line((27,15,28,23),fill=FOAM)
        else: d.line((7,18,5,25),fill=FOAM); d.line((25,18,23,25),fill=FOAM)
    elif name=='magnetic_pole_pull':
        d.arc((6,6,25,25),0,180,fill=FOAM,width=4)
        d.rectangle((6,6,10,15),fill=SILVER); d.rectangle((21,6,25,15),fill=TEAL)
        d.line((15,5,15,15,12,12,15,15,18,12),fill=SILVER)
    elif name in ('electromagnetic_disruption','dance_of_thunder'):
        for y in (9,15,21): d.line((5,y,10,y,13,y-3,17,y+4,21,y,27,y),fill=FOAM)
        d.line((9,26,24,5),fill=SILVER,width=2)
        if name=='dance_of_thunder': d.rectangle((6,5,8,7),fill=SILVER); d.rectangle((23,25,25,27),fill=SILVER)
    elif name=='heavenly_punishment':
        d.rectangle((11,5,21,21),fill=(54,74,98)); cloud_icon(d,7); bolt(d,16,8,.8)
        d.ellipse((6,24,26,27),outline=SILVER)
    elif name=='ocean_of_lightning':
        wave_icon(d,22)
        for x in (9,19,26):bolt(d,x,6,.55)
    elif name=='lightning_forest':
        for x,y in [(7,6),(16,3),(25,6)]:bolt(d,x,y,.7)
        d.line((5,27,27,27),fill=FOAM,width=2)
    elif name in ('siren_song','ocean_songster','discord','death_dirge','requiem'):
        for x,y in [(10,12),(22,7)]:
            d.line((x,y,x,y+13),fill=FOAM,width=2); d.ellipse((x-4,y+11,x,y+14),fill=SILVER)
        d.line((10,12,22,7),fill=SILVER,width=2)
        if name in ('discord','death_dirge'):d.line((6,5,11,10,16,6,22,12,27,8),fill=RED if name=='discord' else (120,150,180),width=2)
        elif name=='requiem':d.arc((4,12,27,29),0,180,fill=SILVER);d.line((7,26,7,18,10,15),fill=SILVER,width=2)
        elif name=='ocean_songster':d.line((5,27,11,25,18,27,26,25),fill=TEAL,width=2)
        else:d.arc((3,5,28,28),20,80,fill=FOAM)
    elif name in ('roar','roar_of_the_thunder_god','sound_of_horror','fear','fear_resistance'):
        d.polygon([(8,9),(14,6),(20,9),(21,22),(15,27),(8,22)],fill=TEAL,outline=SILVER)
        d.polygon([(11,12),(14,14),(11,15)],fill=SILVER); d.polygon([(18,12),(15,14),(18,15)],fill=SILVER)
        d.rectangle((12,18,17,23),fill=DARK)
        for i in range(2): d.arc((17+i*4,9-i*3,25+i*4,24+i*3),280,440,fill=SILVER)
        if name=='roar_of_the_thunder_god':bolt(d,7,14,.5)
        elif name=='fear_resistance':d.line((6,27,25,5),fill=FOAM,width=2)
        elif name=='sound_of_horror':d.line((10,5,8,2),fill=RED); d.line((23,5,26,2),fill=RED)
        elif name=='fear':d.line((7,5,4,3),fill=FOAM)
    elif name in ('tyrants_presence','tyrants_aura','dreadful_heaven'):
        crown(d,8)
        d.line((5,21,16,27,27,21),fill=FOAM,width=2)
        if name=='dreadful_heaven':d.arc((4,3,28,28),180,360,fill=SILVER,width=2)
        elif name=='tyrants_aura': d.ellipse((12,20,20,24),outline=SILVER)
        else: d.line((16,19,16,25),fill=SILVER)
    elif name=='chasm_of_storms':
        d.arc((4,4,28,28),180,360,fill=FOAM,width=2)
        d.line((5,17,5,26,27,26,27,17),fill=TEAL,width=2);bolt(d,16,5,.9)
    elif name=='banish_into_the_storm':
        d.ellipse((6,4,26,28),outline=FOAM,width=2)
        d.line((7,16,24,16,19,11,24,16,19,21),fill=SILVER,width=2)
        d.arc((11,6,23,26),260,440,fill=TEAL,width=2)
    elif name=='tipsy':
        d.polygon([(10,5),(20,5),(19,10),(24,15),(23,26),(8,26),(7,15),(11,10)],fill=TEAL,outline=FOAM)
        d.rectangle((11,4,19,8),fill=(156,132,101));d.rectangle((10,18,22,23),fill=EMBER)
    elif name=='spent':
        d.line((7,7,25,25),fill=RED,width=3); d.line((25,7,7,25),fill=RED,width=3)
        d.line((17,5,12,15,19,17,14,27),fill=FOAM)
    elif name=='madness':
        eye(d,True);d.line((16,5,12,10,20,22,15,27),fill=EMBER,width=2)
    elif name=='corrosion':
        d.polygon([(7,6),(25,6),(23,23),(16,27),(9,23)],fill=TEAL,outline=FOAM)
        for x,y in [(12,12),(21,16),(16,22)]:d.rectangle((x-1,y-1,x+1,y+1),fill=DARK)
    elif name=='spiritual_intuition':
        eye(d);d.line((16,4,16,8),fill=EMBER,width=2);d.rectangle((15,24,17,27),fill=EMBER)
    elif name=='aquatic_affinity':
        d.polygon([(6,15),(20,9),(25,11),(27,16),(24,21),(20,22),(6,17),(4,22),(4,10)],fill=TEAL,outline=FOAM)
        d.point((23,14),fill=SILVER)
    else:raise ValueError('Missing icon: '+name)
    return im if size==32 else im.resize((size,size),Image.Resampling.NEAREST)


def gui(root):
    for name in ABILITIES+UPGRADES:
        save(root,f'textures/gui/ability_menus/{PREFIX}/{name}.png',glyph(name))
    for name in ABILITIES+UPGRADES+STATUSES:
        save(root,f'textures/gui/{PREFIX}/{name}.png',glyph(name,16))
    save(root,f'textures/gui/pathway_icons/{PREFIX}.png',glyph('authority_of_thunder',16))
    for state in ('empty','filled','critical'):
        im=Image.new('RGBA',(8,8));d=ImageDraw.Draw(im)
        d.polygon([(4,0),(7,4),(6,7),(1,7),(0,4),(3,5)],fill=DARK if state=='empty' else RED,outline=EMBER if state=='critical' else (121,70,62))
        if state=='critical':d.line((4,3,3,6,5,6),fill=SILVER)
        save(root,f'textures/gui/{PREFIX}/rage_pip_{state}.png',im)
    # Optional HUD overlays: alpha only; no text, values or gameplay changes baked in.
    for name in ('wrath_eyes','soaked_vignette','storm_vignette','horror_vignette'):
        im=Image.new('RGBA',(256,128));px=im.load()
        col=RED if name=='wrath_eyes' else TEAL if name=='soaked_vignette' else SEA
        for y in range(128):
            for x in range(256):
                edge=max(abs(x-127.5)/127.5,abs(y-63.5)/63.5)
                alpha=int(max(0,(edge-.56)/.44)**2*130)
                if name=='horror_vignette':alpha=int(alpha*(.7+.3*math.sin(x*.22+y*.17)**2))
                px[x,y]=(*col[:3],alpha)
        save(root,f'textures/gui/{PREFIX}/{name}.png',im)


def texture(root,name,painter,frames=1,size=64):
    tex=Texture(f'{PREFIX}/{name}',size,size,frames=frames,frametime=3,interpolate=True)
    tex.zone('all',0,0,16,16,painter);tex.save(str(root));return tex


def materials(root):
    def sea(tx,ty,x,y,f):
        glint=(math.sin(tx*22+ty*13-f*math.tau/4)+math.sin(ty*32-f*math.tau/4))*.5
        return (max(0,int(31+glint*11)),int(111+glint*28),int(139+glint*27),int(120+glint*24))
    def foam(tx,ty,x,y,f):
        return (*FOAM[:3],int(150+80*math.sin(tx*16+ty*13+f*math.tau/4)**2))
    def metal(tx,ty,x,y,f):
        t=.55+.4*math.sin(tx*math.pi);return tuple(int(v*t) for v in SILVER[:3])+(255,)
    def cloud(tx,ty,x,y,f):
        n=(math.sin(tx*13+f*math.tau/4)+math.cos(ty*19+tx*7))*8
        return (int(36+n),int(53+n),int(75+n),210)
    def lightning(tx,ty,x,y,f):
        v=int(225+25*math.sin(ty*30+f*math.tau/4)**2);return(v,min(255,v+8),255,245)
    def scales(tx,ty,x,y,f):
        seam=(int(ty*6)%2)*.0833
        local=((tx+seam)*6)%1
        lit=abs((ty*6)%1-(.7-.45*math.cos((local-.5)*math.pi)))<.09
        return (*(FOAM[:3] if lit else TEAL[:3]),190 if lit else 95)
    return {
        'water':texture(root,'water',sea,4), 'foam':texture(root,'foam',foam,4),
        'silver':texture(root,'silver',metal), 'cloud':texture(root,'cloud',cloud,4),
        'light_cloud':texture(root,'light_cloud',lambda tx,ty,x,y,f:(int(183+math.sin(tx*13+ty*17+f*math.tau/4)*18),int(207+math.sin(tx*13+ty*17+f*math.tau/4)*16),235,210),4),
        'lightning':texture(root,'lightning',lightning,4),
        'scales':texture(root,'scales',scales),
        'rope':texture(root,'rope',lambda tx,ty,x,y,f:(int(109+math.sin(tx*28+ty*12)*20),92,65,255),size=16),
    }


def models(root):
    mats=materials(root); records=[]
    def model(name,mat,description,pivot=(8,0,8)):
        m=Model(f'{PREFIX}/{name}',mats[mat],f'Thunder God: {description} Pivot {pivot}; 16 units = 1 block; front north (-Z).')
        records.append({'name':name,'pivot':list(pivot),'material':mat,'description':description,
                        'render':{'see_through':mat not in ('silver','rope'),'glow':mat in ('lightning','foam')},'model':m})
        return m
    def cube(m,name,cx,cy,cz,sx,sy,sz,rot=None):
        m.cube(name,(cx-sx/2,cy-sy/2,cz-sz/2),(cx+sx/2,cy+sy/2,cz+sz/2),'all',rot)
    def shell(m,r=12,step=3):
        # Cell centers are inset by half a voxel so the outside spans exactly 2*r.
        for x in np.arange(-r+step/2,r,step):
            for y in np.arange(-r+step/2,r,step):
                for z in np.arange(-r+step/2,r,step):
                    dist=math.sqrt(x*x+y*y+z*z)
                    if r-step<dist<=r:
                        cube(m,f'cell_{x}_{y}_{z}',8+x,12+y,8+z,step,step,step)
    def cloud_shape(m,face=False):
        puffs=[(-9,7,0,9,7,9),(-3,10,0,11,9,12),(5,9,1,12,10,12),(11,6,0,7,7,9),
               (-4,4,-5,13,6,9),(4,4,5,13,6,8),(-8,4,5,8,6,8)]
        for i,(x,y,z,sx,sy,sz) in enumerate(puffs):cube(m,f'puff_{i}',8+x,y,8+z,sx,sy,sz)
    m=model('harpoon','silver','Barbed iron harpoon; tip at y=28; rope anchor at y=0.')
    cube(m,'shaft',8,11,8,1.2,22,1.2)
    for j in range(4):cube(m,f'tip_{j}',8,21.5+j*2,8,4-j,2,1.3)
    cube(m,'barb_left',5.9,21.5,8,1,6,1.3,(45,'z',(5.9,21.5,8)))
    cube(m,'barb_right',10.1,21.5,8,1,6,1.3,(-45,'z',(10.1,21.5,8)))
    cube(m,'collar',8,2,8,2.2,2,2.2)
    m=model('rope_segment','rope','Thin straight rope section; repeat or use ParticleShapes.line.',(8,8,8))
    cube(m,'rope',8,8,8,.45,16,.45)
    m=model('scale_armor','scales','Open torso shell; translucent, worn using modelOnTurning.',(8,0,8))
    for n,frm,to in [('front',(3,4,4),(13,19,4.4)),('back',(3,4,11.6),(13,19,12)),('left',(2.8,4,4.4),(3.2,18,11.6)),('right',(12.8,4,4.4),(13.2,18,11.6))]:m.cube(n,frm,to,'all')
    for f in range(4):
        m=model(f'wave_crest_{f}','water','Four compatible rolling-wave poses; ground pivot. Scale uniformly or tile sideways.')
        for i in range(12):
            x=-5+i*2.35;y=4+7*math.exp(-((i-7)/4)**2);z=8+math.sin(i*.42+f*math.tau/4)*2
            cube(m,f'wave_{i}',x,y/2,z,2.6,y,5)
            cube(m,f'curl_{i}',x,y+1,z-2.3,2.6,2.3,4)
    m=model('wave_foam','foam','Foam lip for wave_crest; layer above the moving crest.')
    for i in range(12):
        y=4+7*math.exp(-((i-7)/4)**2);cube(m,f'foam_{i}',-5+i*2.35,y+2,5.7,2.6,.8,4.5)
    m=model('water_film','water','Face-size translucent double-sided water panel.',(8,8,8))
    cube(m,'film',8,8,8,8,10,.15)
    m=model('water_curtain','water','One-block water panel; tile and scale for curtain or upgraded wall.')
    cube(m,'curtain',8,8,8,16,16,.3)
    m=model('water_sphere','water','Hollow voxel sphere, 24 units across; scale 3.33 for five blocks.',(8,12,8));shell(m)
    m=model('droplet','water','Small pointed water projectile with a round lower bulb.',(8,8,8))
    cube(m,'bulb',8,7,8,7,7,7);cube(m,'neck',8,12,8,4,4,4);cube(m,'tip',8,15,8,2,3,2)
    # Transparent double-sided cards and ground surfaces use animated painted textures.
    def flat(name,painter,horizontal=False,frames=4,description='Animated double-sided effect card.'):
        tex=texture(root,name,painter,frames)
        mats[name]=tex;m=model(name,name,description,(8,0,8) if horizontal else (8,8,8))
        if horizontal:m.cube('surface',(-8,0,-8),(24,.04,24),'all',skip=('north','south','east','west'))
        else:m.cube('card',(-8,-8,7.98),(24,24,8.02),'all',skip=('up','down','east','west'))
        # Cards use translucent unshaded rendering; aura would illuminate the rectangle.
        records[-1]['render']={'see_through':True,'glow':name not in ('rain_sheet','cloud_face'),'unshaded':True}
    def whirl(tx,ty,x,y,f):
        dx,dy=tx-.5,ty-.5;r=math.hypot(dx,dy);a=math.atan2(dy,dx)
        if r>.48:return(0,0,0,0)
        line=math.sin(a*4+r*45-f*math.tau/4)
        c=FOAM if line>.65 else TEAL
        return(*c[:3],int((1-r/.5)*170+35) if r>.07 else 30)
    flat('whirlpool',whirl,True,description='Animated whirlpool/flood disc, two blocks wide; tint sea-blue, spin with ModelFx keys.')
    def ring(tx,ty,x,y,f):
        r=math.hypot(tx-.5,ty-.5);width=.016
        lit=abs(r-.44)<width or abs(r-.34)<width*.6
        a=math.atan2(ty-.5,tx-.5)
        ticks=abs(math.sin(a*12))>.94 and .36<r<.47
        return (*SILVER[:3],220 if lit or ticks else 0)
    flat('warning_ring',ring,True,1,'High-contrast target warning ring; two-block diameter before scaling; no damage or targeting.')
    def rain(tx,ty,x,y,f):
        s=(x*17+y*3)%31;return (*FOAM[:3],150 if s<2 and (y+f*5+x*3)%19<12 else 0)
    flat('rain_sheet',rain,False,description='Local rain sheet; repeat within a zone, never change world weather.')
    def shimmer(tx,ty,x,y,f):
        arc=abs(math.hypot((tx-.1)*.9,ty-.9)-.8)
        return (*SILVER[:3],int(85*(1-arc/.025)) if arc<.025 else 0)
    flat('windblade',shimmer,False,description='Faint curved windblade shimmer; deliberately low opacity.')
    def sound_ring(tx,ty,x,y,f):
        r=math.hypot(tx-.5,ty-.5);delta=abs(r-(.26+f*.055))
        return (*SILVER[:3],int(200*(1-delta/.025)) if delta<.025 else 0)
    flat('sound_wave',sound_ring,False,description='Expanding concentric voice-wave card; tint silver/blue for songs and roars.')
    def face(tx,ty,x,y,f):
        brow=(.16<tx<.42 and abs(ty-(.35+tx*.25))<.035) or (.58<tx<.84 and abs(ty-(.6-tx*.25))<.035)
        mouth=((tx-.5)/.13)**2+((ty-.68)/.1)**2<1
        return (*SILVER[:3],230 if brow else 75 if mouth else 0)
    flat('cloud_face',face,False,1,'Angry brows and mouth; layer in front of storm_cloud at a matching scale.')
    m=model('storm_cloud','cloud','Seven puffs of dark cloud; rain and lightning originate underneath.');cloud_shape(m)
    m=model('cloud_sword','light_cloud','Massive pointed cloud sword; upright tip at y=29; animate downward for impact.')
    cube(m,'blade',8,16,8,5,16,2.8)
    for j in range(3):cube(m,f'tip_{j}',8,24+j*2,8,4-j,2,2)
    cube(m,'guard',8,7,8,16,2.8,5)
    cube(m,'grip',8,2,8,2,8,2);cube(m,'pommel',8,-2,8,4,2,4)
    m=model('lightning_arrow','lightning','Silver-white arrow, aligned +Y; point along flight with modelAlong.',(8,0,8))
    cube(m,'shaft',8,12,8,.8,24,.8)
    for j in range(3):cube(m,f'tip_{j}',8,24+j*2,8,3-j,2,1.4)
    for i,x in enumerate((6.5,9.5)):cube(m,f'fletching_{i}',x,3,8,1,6,3)
    m=model('bolt','lightning','Zigzag silver bolt; cosmetic only, never a vanilla LightningBolt.',(8,0,8))
    for i in range(6):
        x=7 if i%2 else 9;cube(m,f'segment_{i}',x,2+i*4,8,1.3,6,1.3,((45 if i%2 else -45),'z',(x,2+i*4,8)))
    for i,(x,y) in enumerate(((5,9),(11,17))):cube(m,f'branch_{i}',x,y,8,1,6,1,(45,'z',(x,y,8)))
    m=model('ball_lightning','lightning','Faceted lightning orb for Starfall and Lightning Storm.',(8,12,8));shell(m,9,3)
    m=model('heavenly_pillar','cloud','Segment of the dark sky pillar; repeat vertically above the target.')
    for i in range(4):cube(m,f'cloud_{i}',8,2+i*7,8,7+i%2*3,7,7+i%2*3)
    def shell_painter(kind):
        def paint(tx,ty,x,y,f):
            line=abs(math.sin(tx*math.pi*8))<.12 or abs(math.sin(ty*math.pi*8))<.1
            if kind=='chasm':
                flash=abs(tx-(.5+.08*math.sin(ty*24+f*math.tau/4)))<.018
                return (*(SILVER[:3] if flash else TEAL[:3]),220 if flash else 95 if line else 24)
            crown_mark=.35<tx<.65 and .3<ty<.5 and int(tx*32+ty*12)%7<3
            return (*(SILVER[:3] if crown_mark else SEA[:3]),190 if crown_mark else 95 if line else 36)
        return paint
    for kind in ('chasm','dreadful'):
        name='kingdom_'+kind
        mats[name]=texture(root,name,shell_painter(kind),4)
        m=model(name,name,'Translucent kingdom shell; 24 units wide; scale 20 for a 30-block diameter.',(8,12,8));shell(m)
        records[-1]['render']={'see_through':True,'glow':True,'unshaded':True}
    m=model('tyrant_crown','silver','Five-point silver crown for oppression and the pathway emblem.')
    for i,x in enumerate((0,4,8,12,16)):
        h=9 if i==2 else 6 if i%2 else 4;cube(m,f'point_{i}',x,4+h/2,8,2,h,2)
    cube(m,'band',8,3,8,19,3,3)
    for rec in records:
        m=rec.pop('model');m.save(str(root))
        rec['id']=f'{NS}:model_fx/{PREFIX}/{rec["name"]}'
    return records


def particles(root):
    for i,name in enumerate(PARTICLES):
        im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im);white=(255,255,255,255);gray=(210,225,240,200)
        if name=='spark':d.line((8,2,8,13),fill=white);d.line((2,8,13,8),fill=white);d.rectangle((6,6,10,10),fill=white)
        elif name.startswith('arc_'):
            j=int(name[-1]);d.line([(2,13),(6+j,8),(4+j,6),(12,2)],fill=white,width=2)
        elif name=='raindrop':d.line((9,1,6,13),fill=gray,width=2)
        elif name=='splash':d.arc((2,7,14,13),180,360,fill=white);d.line((4,6,2,3),fill=gray);d.line((12,5,14,2),fill=gray)
        elif name=='foam':
            for x,y in [(3,8),(7,7),(11,9),(8,11)]:d.ellipse((x-2,y-2,x+2,y+2),fill=gray)
        elif name in ('ripple','sound_ring'):d.ellipse((1,5 if name=='ripple' else 1,14,12 if name=='ripple' else 14),outline=white)
        elif name in ('wind_streak','windblade'):d.arc((1,1,15,15),210,310 if name=='windblade' else 355,fill=gray,width=1)
        elif name=='scale':d.polygon([(3,3),(13,3),(13,8),(8,13),(3,8)],fill=gray,outline=white)
        elif name=='cloud':
            for x,y in [(5,8),(8,5),(11,8)]:d.ellipse((x-4,y-3,x+3,y+4),fill=(225,225,225,110))
        elif name=='rage_ember':d.polygon([(4,13),(3,8),(6,10),(9,2),(11,10),(13,7),(12,13)],fill=white)
        elif name=='spirit_hand':
            d.rectangle((5,8,11,12),fill=gray)
            for x in (5,7,9,11):d.line((x,9,x,3+x%3),fill=white)
            d.line((5,10,2,7),fill=white);d.line((7,12,6,15,10,15,10,12),fill=gray)
        elif name=='crown':d.polygon([(2,4),(5,7),(8,2),(11,7),(14,4),(12,12),(4,12)],fill=white)
        elif name=='rope':d.line((7,0,8,15),fill=white,width=2)
        else:raise ValueError(name)
        save(root,f'textures/particle/fx/{PREFIX}/{name}.png',im)


def sound_family(name):
    if name.endswith('_loop'):return name[:-5]
    if name in WHEELS['voice'] or name in ('sonic_boom','sound_of_horror'):return 'voice'
    if name in WHEELS['thunder'] or name in ('bolt_crack','thunder_roll','thunder_strike','starfall','incarnation_of_light'):return 'thunder'
    if name in WHEELS['tides'] or name in ('tsunami','wave_break','water_wall','torrential_downpour'):return 'water'
    if name in WHEELS['sky'] or name in ('hurricane','melee_swing'):return 'wind'
    if name in ('wrath','raging_blow','source_of_wrath','rage_gain','rage_full','wrath_end'):return 'rage'
    return 'storm'


def synthesize(name, index):
    sr=22050;family=sound_family(name);loop=name.endswith('_loop')
    seconds=2.8 if loop else 2.2 if name in ('heavenly_punishment','lightning_forest','storm_hell','death_dirge','requiem') else 1.1+(index%4)*.16
    n=int(sr*seconds);t=np.arange(n)/sr;p=t/seconds
    rng=np.random.default_rng(3100+index);noise=rng.normal(0,1,n)
    # Moving-average bands yield independent low rumble, spray and air layers.
    low=np.convolve(noise,np.ones(120)/120,mode='same')*5
    mid=np.convolve(noise,np.ones(15)/15,mode='same')*2
    air=noise-np.convolve(noise,np.ones(5)/5,mode='same')
    attack=np.minimum(t/(.005 if family=='thunder' else .07),1)
    env=attack*np.maximum(0,1-p)**1.5
    base=49+index%7*7
    if family=='thunder':
        crack=air*np.exp(-t*26)*.6
        rumble=(low*.6+np.sin(2*np.pi*(base*t-12*t*t))*.18)*np.exp(-t*2.3)
        pulses=np.zeros(n)
        for delay in (.09,.19,.37):pulses+=air*np.exp(-np.maximum(0,t-delay)*35)*(t>=delay)*.09
        out=(crack+rumble+pulses)*attack
    elif family=='voice':
        root={'siren_song':293.66,'discord':207.65,'death_dirge':98,'requiem':174.61,'sound_of_horror':123.47}.get(name,65.41)
        intervals=(1,1.05946,1.4142) if name in ('discord','sound_of_horror') else (1,1.5,2)
        out=sum(np.sin(2*np.pi*root*k*t+np.sin(t*math.tau*5)*.035)/(j+2) for j,k in enumerate(intervals))
        out=(out*.45+low*.1)*(np.sin(np.pi*p)**.6)
        if name in ('roar','thunderclap','roar_of_the_thunder_god'):out+=low*np.exp(-t*2)*.7+air*np.exp(-t*24)*.12
    elif family=='water':out=(mid*.5+air*.08+np.sin(2*np.pi*(210*t+65*np.sin(t*6)))*.05)*env*(.65+.35*np.sin(t*18)**2)
    elif family=='wind':out=(mid*.35+air*.11)*(np.sin(np.pi*p)**1.5)
    elif family=='rage':out=(np.sin(2*np.pi*(base*t+80*t*t))*.22+low*.55+air*np.exp(-t*18)*.2)*env
    else:out=(low*.6+mid*.1+np.sin(2*np.pi*base*t)*.09)*env
    if loop:
        out=(low*.45+mid*.15+air*(.045 if family=='rain' else .015))*(.85+.15*np.sin(math.tau*p))
        # A circular crossfade removes the discontinuity at repeated playback.
        k=int(.12*sr);blend=np.linspace(0,1,k)
        seam=out[-k:]*(1-blend)+out[:k]*blend
        out=np.concatenate((seam,out[k:-k]));n=len(out)
    else:
        echo=int(sr*.13);out[echo:]+=out[:-echo].copy()*.18
        out*=np.minimum((np.arange(n)[::-1]/sr)/.06,1)
    out=np.tanh(out*1.4);out*=.55/max(.01,float(np.max(np.abs(out))))
    return sr,np.rint(out*32767).astype('<i2')


def sounds(root):
    events=json.loads((root/'sounds.json').read_text()) if (root/'sounds.json').exists() else {}
    lp=root/'lang/en_us.json';lang=json.loads(lp.read_text()) if lp.exists() else {}
    for i,name in enumerate(SOUNDS):
        sr,samples=synthesize(name,i);target=root/f'sounds/{PREFIX}/{name}.ogg';target.parent.mkdir(parents=True,exist_ok=True)
        with tempfile.TemporaryDirectory() as temp:
            wav=Path(temp)/'cue.wav'
            with wave.open(str(wav),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(sr);w.writeframes(samples.tobytes())
            subprocess.run(['ffmpeg','-v','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','4','-map_metadata','-1',str(target)],check=True)
        key=f'{PREFIX}.{name}';sub=f'subtitles.{NS}.{key}'
        events[key]={'subtitle':sub,'sounds':[{'name':f'{NS}:{PREFIX}/{name}','stream':False}]}
        lang[sub]=CUE_TEXT.get(name,'Thunder God: '+name.replace('_',' ').capitalize())
    write_json(root/'sounds.json',events);write_json(lp,lang)


ABILITY_MODELS = {
    'harpoon':['harpoon','rope_segment'], 'illusory_scales':['scale_armor'], 'raging_blow':['sound_wave'],
    'wrath':['tyrant_crown'], 'source_of_wrath':['tyrant_crown'],
    'water_veil':['water_film'], 'sweeping_wave':['wave_crest_0','wave_foam'], 'tideheal':['droplet'],
    'water_curtain':['water_curtain'], 'water_sphere':['water_sphere'], 'corrosive_rain':['storm_cloud','rain_sheet'],
    'sea_master':['whirlpool'], 'maelstrom':['whirlpool'], 'destructive_droplet':['droplet'],
    'windblade':['windblade'], 'gale_burst':['sound_wave'], 'gale_ride':['windblade'],
    'wind_binding':['sound_wave'], 'windblade_legion':['windblade'], 'sonic_boom':['sound_wave'],
    'cloud_sword':['cloud_sword'], 'incarnation_of_light':['ball_lightning'], 'starfall':['ball_lightning','warning_ring'],
    'lightning_strike':['bolt'], 'lightning_bolt_arrow':['lightning_arrow'], 'chain_lightning':['bolt'],
    'lightning_storm':['ball_lightning','bolt'], 'magnetic_pole_pull':['whirlpool'],
    'electromagnetic_disruption':[], 'heavenly_punishment':['heavenly_pillar','bolt'],
    'ocean_of_lightning':['whirlpool','bolt'], 'lightning_forest':['bolt'],
    'siren_song':['sound_wave'], 'discord':['sound_wave'], 'thunderclap':['sound_wave'],
    'roar':['sound_wave'], 'death_dirge':['sound_wave'], 'sound_of_horror':['sound_wave'],
    'requiem':['sound_wave'], 'roar_of_the_thunder_god':['sound_wave','bolt'], 'tyrants_presence':['tyrant_crown'],
    'hurricane':['whirlpool','windblade'], 'tsunami':['wave_crest_0','wave_foam'],
    'wrath_of_the_sea':['wave_crest_0'], 'storm_incarnation':['storm_cloud','cloud_face','rain_sheet','bolt'],
    'storm_hell':['storm_cloud','rain_sheet','bolt'], 'chasm_of_storms':['kingdom_chasm'],
    'dreadful_heaven':['kingdom_dreadful'], 'banish_into_the_storm':['whirlpool'],
    'water_wall':['water_curtain'], 'torrential_downpour':['storm_cloud','rain_sheet'],
    'thunder_strike':['bolt'], 'dance_of_thunder':[],
}


def manifest(root,records):
    data={'pathway':'Thunder God','scope':'Assets and cosmetic playback only. Ability rules and balance remain unimplemented.',
          'palette':{'sea':'#10294A','lightning':'#D6ECFF','water':'#1F6F8B','rage':'#C0392B'},
          'wheels':WHEELS,'basic':BASIC,'upgrades':UPGRADES,'statuses':STATUSES,'models':records,
          'particles':{n:f'fx/{PREFIX}/{n}' for n in PARTICLES},'abilities':{},
          'silent_invisible':sorted(SILENT),'sound_events':{n:f'{NS}:{PREFIX}.{n}' for n in SOUNDS},
          'animation':{'wave_crest':{'frames':4,'smooth':True,'ticks':12},'texture_frames':4,'texture_frametime':3},
          'optional_melee':{'steps':'Boarding Cut, Anchor Swing, Gale Slash, Squall Sweep, Tide Heave, Thunder Lunge, Rising Storm, Final Strike'.split(', '),
                            'particles':['wind_streak','windblade','spark','arc_0','arc_1','arc_2'],'sound':'melee_swing'},
          'excluded':['Mythical Creature Form (explicitly deferred in the design)','Earthquake','Snow Storm','new sea-creature models (uses vanilla summons)']}
    for n in ABILITIES+UPGRADES:
        data['abilities'][n]={'wheel_icon':f'textures/gui/ability_menus/{PREFIX}/{n}.png','hud_icon':f'textures/gui/{PREFIX}/{n}.png',
                             'models':ABILITY_MODELS[n],'sound':None if n in SILENT else f'{NS}:{PREFIX}.{n}'}
    owned=[]
    for folder in [f'models/model_fx/{PREFIX}',f'textures/model_fx/{PREFIX}',f'textures/gui/ability_menus/{PREFIX}',
                   f'textures/gui/{PREFIX}',f'textures/particle/fx/{PREFIX}',f'sounds/{PREFIX}']:
        owned.extend(p for p in (root/folder).rglob('*') if p.is_file())
    owned.append(root/f'textures/gui/pathway_icons/{PREFIX}.png')
    data['files']={p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(owned)}
    write_json(root/f'{PREFIX}/asset_manifest.json',data);return data


def preview(root,path):
    fontpath='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
    font=ImageFont.truetype(fontpath,13) if Path(fontpath).exists() else ImageFont.load_default()
    title=ImageFont.truetype(fontpath,30) if Path(fontpath).exists() else font
    sheet=Image.new('RGB',(1296,1320),DARK[:3]);d=ImageDraw.Draw(sheet)
    d.text((32,22),'THUNDER GOD  /  THE WALKING STORM',font=title,fill=SILVER)
    d.text((32,68),'44 wheel entries  /  4 single casts  /  5 upgrades  /  original pixel art + synthesized sound',font=font,fill=FOAM)
    for row,(wheel,entries) in enumerate(WHEELS.items()):
        y=108+row*156
        d.text((32,y),wheel.upper(),font=font,fill=FOAM)
        for col,n in enumerate(entries):
            x=135+col*126;im=glyph(n).resize((64,64),Image.Resampling.NEAREST)
            sheet.paste(im,(x+18,y),im)
            words=n.replace('_',' ').split();lines=['']
            for w in words:
                if len(lines[-1])+len(w)>14:lines.append(w)
                else:lines[-1]=(lines[-1]+' '+w).strip()
            d.multiline_text((x,y+70),'\n'.join(lines),font=font,fill=SILVER,spacing=2)
    d.text((32,870),'SINGLE CASTS + UPGRADES',font=font,fill=FOAM)
    for col,n in enumerate(BASIC+UPGRADES):
        x=135+col*126;im=glyph(n).resize((48,48),Image.Resampling.NEAREST);sheet.paste(im,(x+18,900),im)
        d.multiline_text((x,954),n.replace('_','\n'),font=font,fill=SILVER,spacing=1)
    d.text((32,1035),'HUD / STATUS / PASSIVES',font=font,fill=FOAM)
    for i,n in enumerate(STATUSES):
        x=32+i%12*105;y=1066+i//12*78;im=glyph(n,16).resize((32,32),Image.Resampling.NEAREST);sheet.paste(im,(x,y),im)
        d.multiline_text((x,y+35),n.replace('_','\n'),font=ImageFont.load_default(),fill=SILVER,spacing=0)
    d.text((32,1240),'SEA  #10294A       SILVER LIGHTNING  #D6ECFF       WATER  #1F6F8B       RAGE  #C0392B',font=font,fill=FOAM)
    path.parent.mkdir(parents=True,exist_ok=True);sheet.save(path)


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--assets',type=Path,default=ASSETS)
    p.add_argument('--preview',type=Path,default=REPO/'docs/pathways/thunder_god/assets/icons.png');args=p.parse_args()
    records=models(args.assets);gui(args.assets);particles(args.assets);sounds(args.assets)
    data=manifest(args.assets,records);preview(args.assets,args.preview)
    print(f'Created {len(records)} models, {len(ABILITIES)+len(UPGRADES)} menu icons, {len(STATUSES)} status glyphs, '
          f'{len(PARTICLES)} particles, {len(SOUNDS)} mono cues; {len(data["files"])} owned files.')


if __name__=='__main__':main()
