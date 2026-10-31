"""Original, deterministic Omniscient Eye resources (Pillow + ffmpeg).

python tools/model_gen/omniscient_eye/generate.py [--assets PATH] [--preview PATH]
Geometry follows the mod's ModelFx/Blockbench cube format, not an entity rig.
No network access, downloaded art, or gameplay implementation is involved.
"""
import argparse
from array import array
import hashlib
import json
import math
from pathlib import Path
import random
import subprocess
import sys
import tempfile
import wave

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from modelkit import Model, Texture

REPO = Path(__file__).resolve().parents[3]
DEFAULT_ASSETS = REPO / 'src/main/resources/assets/circle_of_inevitability'
NS = 'circle_of_inevitability'
PREFIX = 'omniscient_eye'
BRASS = (209, 169, 75, 255)
GOLD = (250, 222, 145, 255)
CREAM = (244, 235, 208, 255)
INK = (34, 27, 20, 255)
VIOLET = (115, 61, 157, 255)
PALE = (218, 231, 241, 150)
WOOD = (72, 45, 34, 255)

BASIC = 'reading ritual_circle deduction keen_eye reconstruction deductive_strike analyze imitate'.split()
WHEELS = {
    'mystic_arts': 'mirror_curse vile_thoughts improvise living_tome golden_armor_art'.split(),
    'fate': 'prophecy bookshelf_form cognize leverage decipher'.split(),
    'omniscience': 'evade_disaster wise_counsel omniscience exploit redefine calamity_of_knowledge open_source tower_of_omniscience'.split(),
}
ABILITIES = BASIC + sum(WHEELS.values(), [])
STATUSES = 'analysis codex madness rite_focus exposed foreseen creation knowledge_addiction sharp_mind weapon_proficiency polymaths_eye mystic_mind demigods_body discernment cognizing_sight foreseen_shifts omniscient_gaze open_book'.split()
EXTRA_SOUNDS = 'page_turn codex_learn tome_break madness_break tower_end'.split()
PARTICLES = 'page_0 page_1 page_2 ink_drop eye glyph_0 glyph_1 glyph_2 glyph_3 madness_pip ghost_line thread page_edge'.split()


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf-8', newline='\n')


def save(root, path, im):
    target = root / path
    target.parent.mkdir(parents=True, exist_ok=True)
    im.save(target)


def eye(d, box, iris=BRASS, pupil=INK):
    x0, y0, x1, y1 = box
    cx, cy = (x0+x1)/2, (y0+y1)/2
    w, h = (x1-x0)/2, (y1-y0)/2
    d.polygon([(x0,cy),(cx-w*.45,y0),(cx+w*.45,y0),(x1,cy),
               (cx+w*.45,y1),(cx-w*.45,y1)], fill=iris)
    d.ellipse((cx-h*.8,cy-h*.8,cx+h*.8,cy+h*.8), fill=GOLD)
    d.ellipse((cx-h*.46,cy-h*.8,cx+h*.46,cy+h*.8), fill=pupil)
    d.point((int(cx-1),int(cy-2)), fill=CREAM)


def book(d, box, opened=False, color=BRASS):
    x,y,X,Y=box
    if opened:
        c=(x+X)//2
        d.polygon([(x,y),(c,y+2),(X,y),(X,Y-1),(c,Y),(x,Y-1)],fill=color)
        d.polygon([(x+1,y+1),(c-1,y+3),(c-1,Y-2),(x+1,Y-3)],fill=CREAM)
        d.polygon([(c+1,y+3),(X-1,y+1),(X-1,Y-3),(c+1,Y-2)],fill=CREAM)
        d.line((c,y+3,c,Y),fill=INK)
        for yy in range(y+5,Y-2,3):
            d.line((x+3,yy,c-3,yy+1),fill=WOOD)
            d.line((c+3,yy+1,X-3,yy),fill=WOOD)
    else:
        d.rectangle(box,fill=color)
        d.rectangle((x+3,y+1,X-1,Y-2),fill=CREAM)
        d.line((x+1,y,x+1,Y),fill=GOLD)
        for yy in range(y+4,Y-2,3): d.line((x+5,yy,X-3,yy),fill=WOOD)


def glyph(name, size=32):
    """Distinct pixel silhouettes, also legible at native HUD resolution."""
    im=Image.new('RGBA',(32,32)); d=ImageDraw.Draw(im)
    d.ellipse((1,1,30,30),fill=(24,22,29,235),outline=BRASS)
    d.arc((3,3,28,28),200,320,fill=GOLD)
    if name in ('reading','knowledge_addiction','open_book','codex','living_tome','creation'):
        book(d,(6,8,26,24),True)
        if name in ('living_tome','creation'): eye(d,(10,10,22,18))
        elif name=='reading': d.line((16,4,16,7),fill=GOLD,width=2)
        elif name=='codex': d.rectangle((23,8,25,13),fill=VIOLET)
        elif name=='knowledge_addiction': d.line((24,3,24,9,21,6,27,6),fill=GOLD)
    elif name in ('ritual_circle','rite_focus'):
        d.ellipse((6,9,26,24),outline=GOLD,width=2)
        d.ellipse((9,12,23,21),outline=BRASS)
        for k in range(8):
            a=k*math.tau/8
            x,y=16+12*math.cos(a),16+9*math.sin(a)
            d.line((x,y-1,x,y+1),fill=CREAM)
        if name=='rite_focus': eye(d,(11,13,21,19))
    elif name in ('deduction','keen_eye','analyze','analysis','cognize','omniscience','omniscient_gaze','discernment','cognizing_sight','polymaths_eye','exposed'):
        eye(d,(7,11,25,21),GOLD if name in ('omniscience','omniscient_gaze') else BRASS)
        if name in ('analyze','analysis'): d.line((5,12,5,6,11,6),fill=CREAM); d.line((21,26,27,26,27,20),fill=CREAM)
        elif name in ('cognize','cognizing_sight'): d.line((16,4,16,8,12,6,20,6),fill=CREAM)
        elif name in ('omniscience','omniscient_gaze'):
            for a in range(0,360,45):
                r=math.radians(a); d.line((16+10*math.cos(r),16+10*math.sin(r),16+13*math.cos(r),16+13*math.sin(r)),fill=GOLD)
        elif name=='deduction': d.line((9,8,13,5,18,7,23,5),fill=GOLD)
        elif name=='keen_eye': d.ellipse((10,5,22,27),outline=CREAM)
        elif name=='exposed': d.line((7,25,25,7),fill=VIOLET,width=2)
        elif name=='discernment': d.line((16,5,16,8),fill=CREAM); d.line((16,24,16,27),fill=CREAM)
        elif name=='polymaths_eye': book(d,(8,22,24,27),True)
        else: d.line((6,5,11,5,11,8),fill=CREAM)
    elif name in ('reconstruction','prophecy','foreseen','foreseen_shifts'):
        d.arc((5,5,26,26),30,320,fill=PALE,width=2)
        d.polygon([(4,9),(4,3),(10,6)],fill=GOLD)
        if name=='reconstruction':
            for x,y in [(11,12),(19,16),(13,21)]: d.rectangle((x,y,x+3,y+2),fill=CREAM)
        else:
            eye(d,(9,12,23,20),PALE)
            if name=='foreseen_shifts': d.line((19,25,26,20,24,26),fill=GOLD,width=2)
    elif name in ('deductive_strike','weapon_proficiency','exploit'):
        d.line((8,25,24,7),fill=CREAM,width=3)
        d.line((9,18,16,25),fill=BRASS,width=2)
        if name=='deductive_strike':
            for x,y in [(6,9),(11,6),(19,4)]: d.line((x,y,x+3,y+4),fill=GOLD)
        elif name=='exploit': d.line((23,11,19,16,23,20,19,26),fill=VIOLET,width=2)
    elif name=='imitate':
        book(d,(6,11,17,25)); book(d,(15,6,26,20),color=VIOLET)
        d.line((7,8,11,4,18,4),fill=GOLD)
    elif name=='mirror_curse':
        d.ellipse((8,4,24,24),fill=BRASS,outline=GOLD,width=2)
        d.ellipse((11,7,21,21),fill=(71,58,91))
        d.line((16,24,16,28),fill=GOLD,width=3)
        d.line((17,7,14,13,18,16,15,21),fill=CREAM)
    elif name in ('vile_thoughts','madness'):
        d.ellipse((7,7,25,25),fill=VIOLET)
        eye(d,(8,12,24,20),INK,VIOLET)
        for x in (9,16,23): d.line((x,20,x-1,27),fill=VIOLET,width=2)
        if name=='madness': d.line((16,4,12,11,19,17,15,25),fill=CREAM)
    elif name=='improvise':
        d.line((8,25,24,7),fill=BRASS,width=3)
        for x,y,c in [(9,8,(160,221,255)),(24,17,(245,129,69)),(18,26,(177,139,239))]:
            d.line((x-3,y,x+3,y),fill=c); d.line((x,y-3,x,y+3),fill=c)
    elif name in ('golden_armor_art','demigods_body','evade_disaster'):
        d.polygon([(7,7),(16,4),(25,7),(23,21),(16,28),(9,21)],fill=BRASS,outline=GOLD)
        if name=='golden_armor_art': d.line((16,8,16,23),fill=CREAM,width=2); d.line((11,12,21,12),fill=CREAM)
        elif name=='evade_disaster': d.line((6,23,25,9),fill=INK,width=3); d.line((7,24,26,10),fill=PALE)
        else: d.polygon([(16,22),(11,16),(11,12),(15,12),(16,14),(18,12),(21,12),(21,16)],fill=CREAM)
    elif name in ('bookshelf_form','tower_of_omniscience'):
        for y in (11,18,25):
            d.rectangle((8,y,24,y+1),fill=BRASS)
            for x in (9,14,19): d.rectangle((x,y-5,x+3,y-1),fill=CREAM if x!=14 else VIOLET)
        d.line((7,6,7,27,25,27,25,6),fill=GOLD)
        if name=='tower_of_omniscience': eye(d,(9,3,23,9))
        else: eye(d,(10,12,22,17),INK)
    elif name=='leverage':
        d.polygon([(12,24),(20,24),(16,17)],fill=CREAM)
        d.line((5,21,26,11),fill=BRASS,width=3)
        d.rectangle((7,9,13,15),fill=VIOLET)
    elif name=='decipher':
        book(d,(7,6,24,26))
        d.line((13,12,19,12,16,17,20,21,12,21),fill=VIOLET,width=2)
    elif name=='wise_counsel':
        for x,y in [(10,12),(22,12),(16,22)]:
            d.ellipse((x-3,y-3,x+3,y+3),fill=CREAM)
        d.line((10,12,22,12,16,22,10,12),fill=BRASS)
        eye(d,(11,4,21,9))
    elif name=='redefine':
        book(d,(6,7,23,26))
        d.line((13,22,26,7),fill=BRASS,width=3)
        d.polygon([(25,5),(28,8),(23,11)],fill=GOLD)
    elif name=='calamity_of_knowledge':
        book(d,(5,15,27,26),True)
        d.line((16,3,12,10,20,15,14,22),fill=VIOLET,width=3)
        for x in (6,25): d.line((x,7,16,15),fill=GOLD)
    elif name=='open_source':
        book(d,(9,5,23,16),True)
        d.line((16,17,16,20,7,20,7,25,25,25,25,20,16,20),fill=BRASS)
        for x in (7,16,25): d.rectangle((x-2,24,x+2,28),fill=CREAM)
    elif name=='sharp_mind':
        d.polygon([(16,4),(25,15),(16,28),(7,15)],fill=PALE,outline=GOLD)
        d.line((16,8,16,23),fill=CREAM)
    elif name=='mystic_mind':
        book(d,(7,12,25,25),True); d.arc((9,4,23,18),180,360,fill=VIOLET,width=3)
    else: raise ValueError('Missing icon: '+name)
    return im if size==32 else im.resize((size,size),Image.Resampling.NEAREST)


def painted_texture(root, name, painter, size=64, frames=1):
    tex=Texture(PREFIX+'/'+name,size,size,frames=frames,frametime=3,interpolate=True)
    tex.zone('all',0,0,16,16,painter)
    tex.save(str(root))
    return tex


def image_texture(root, name, im, frames=1):
    tex=Texture(PREFIX+'/'+name,im.width,im.height//frames,frames=frames,frametime=3,interpolate=True)
    tex.zone('all',0,0,16,16)
    tex.img=im; tex.save(str(root))
    return tex


def materials(root):
    def brass(tx,ty,x,y,f):
        light=int(20*math.sin(tx*math.pi))-int(18*ty)+(x*13+y*7)%9
        return tuple(max(0,min(255,c+light)) for c in BRASS[:3])+(255,)
    def pages(tx,ty,x,y,f):
        shade=-22 if y%4==3 else (x*7+y*11)%9-4
        return tuple(c+shade for c in CREAM[:3])+(255,)
    def leather(tx,ty,x,y,f):
        if x%32 in (1,2) or y%32 in (1,2): return BRASS
        return (52+(x*13+y*7)%12,35,44,255)
    def wood(tx,ty,x,y,f):
        n=int(9*math.sin(y*.6+math.sin(x*.2)))
        return tuple(c+n for c in WOOD[:3])+(255,)
    def pool(tx,ty,x,y,f):
        ripple=int(8*math.sin(math.hypot(x-32,y-32)*.6-f*math.pi/2))
        return (max(0,23+ripple),max(0,18+ripple),max(0,30+ripple),205)
    return {n:painted_texture(root,n,p,frames=4 if n=='ink_pool' else 1)
            for n,p in [('brass',brass),('pages',pages),('leather',leather),('wood',wood),('ink_pool',pool)]}


def models(root):
    mats=materials(root); records=[]
    def model(name, tex, pivot=(8,0,8), use='glow; solid', frames=1):
        m=Model(PREFIX+'/'+name,tex,f'{name}; pivot {pivot}; front north (-Z)')
        records.append({'name':name,'id':f'{NS}:model_fx/{PREFIX}/{name}',
                        'pivot':list(pivot),'render':use,'frames':frames})
        return m
    def finish(m): write_json(root/f'models/model_fx/{m.name}.json',m.json())
    def cube(m,n,a,b,material):
        # modelkit owns one texture; bind each cube face to an extra material.
        m.cube(n,a,b,'all')
        for face in m.elements[-1]['faces'].values(): face['texture']='#'+material
    def finish_multi(m):
        data=m.json()
        data['textures'].update({k:t.id() for k,t in mats.items()})
        write_json(root/f'models/model_fx/{m.name}.json',data)
    # Three independently sized, double-sided brass eyes, plus a crown eye.
    for name,w,h in [('reading_eye',8,3.5),('cognize_eye',16,7),('tower_eye',28,12)]:
        im=Image.new('RGBA',(128,128)); eye(ImageDraw.Draw(im),(3,35,124,92))
        t=image_texture(root,name,im)
        if name=='reading_eye': mats['eye']=t
        m=model(name,t,(8,8,8),'glow().unshaded().seeThrough(); no aura')
        m.cube('double_sided_eye',(8-w/2,8-h/2,7.85),(8+w/2,8+h/2,8.15),'all',skip=('up','down','east','west'))
        finish(m)
    # Floating dossier: separate border and lines; actual entity text is drawn by the client.
    im=Image.new('RGBA',(128,128)); d=ImageDraw.Draw(im)
    d.rounded_rectangle((3,7,124,120),radius=7,fill=(34,27,20,215),outline=BRASS,width=3)
    eye(d,(44,15,84,37))
    for y,w in [(48,85),(62,69),(76,83),(90,56),(105,77)]:
        d.rectangle((20,y,20+w,y+2),fill=CREAM if y==48 else (209,169,75,140))
    t=image_texture(root,'info_card',im)
    m=model('info_card',t,(8,8,8),'glow().unshaded().seeThrough(); no aura')
    m.cube('dossier',(2,2,7.9),(14,14,8.1),'all',skip=('up','down','east','west')); finish(m)
    # Mirror: physical brass rim and a textured glass inset, no fake target portrait.
    im=Image.new('RGBA',(64,64)); d=ImageDraw.Draw(im)
    for y in range(64): d.line((0,y,63,y),fill=(82+y//3,74+y//4,109+y//3,180))
    d.line((5,52,47,10),fill=(232,232,255,145),width=3)
    d.line((17,58,57,18),fill=(232,232,255,70),width=2)
    mats['glass']=image_texture(root,'mirror_glass',im)
    m=model('brass_mirror',mats['brass'],(8,0,8),'glow().seeThrough(); reflective tint, not a live camera')
    for n,a,b in [('handle',(7,0,7),(9,6,9)),('left',(2,6,7),(4,21,9)),('right',(12,6,7),(14,21,9)),('top',(4,20,7),(12,22,9)),('bottom',(4,5,7),(12,7,9))]: cube(m,n,a,b,'brass')
    cube(m,'mirror',(4,7,7.4),(12,20,8.6),'glass'); finish_multi(m)
    # Closed and opened living tome, with separate page geometry and spine.
    for opened in (False,True):
        name='living_tome_open' if opened else 'living_tome_closed'
        m=model(name,mats['leather'],(8,3,8))
        if opened:
            for i,(x0,x1) in enumerate([(0,7.7),(8.3,16)]):
                cube(m,f'cover_{i}',(x0,0,3),(x1,1,13),'leather')
                cube(m,f'pages_{i}',(x0+.3,1,3.4),(x1-.3,3,12.6),'pages')
            cube(m,'spine',(7.7,0,3),(8.3,3.4,13),'brass')
        else:
            cube(m,'pages',(3,1,3.5),(13,3,12.5),'pages')
            for y in (0,3): cube(m,f'cover_{y}',(2,y,3),(14,y+1,13),'leather')
            cube(m,'spine',(2,0,3),(3,4,13),'brass')
        finish_multi(m)
    # Page/ink creature. Frame geometry is topologically identical for smooth morphs.
    for frame in range(4):
        m=model(f'tome_creature_{frame}',mats['pages'],(8,0,8),'glow().seeThrough()',frames=4)
        phase=math.sin(frame*math.pi/2)
        cube(m,'ink_body',(5,7,5),(11,17,11),'ink_pool')
        cube(m,'head',(4,17,5),(12,24,11),'pages')
        for side,x in [('left',1),('right',12)]:
            dy=phase*(1 if side=='left' else -1)
            cube(m,side+'_arm',(x,8+dy,6),(x+3,18+dy,10),'pages')
            lx=4 if side=='left' else 9
            cube(m,side+'_leg',(lx,0,6),(lx+3,8,10),'leather')
        for i in range(5):
            cube(m,f'floating_page_{i}',(-1+i*4,12+(i%2)*3+phase,11),
                 (2+i*4,13+(i%2)*3+phase,15),'pages')
        cube(m,'brass_eye',(4.5,17.5,4.7),(11.5,23.5,5),'eye')
        finish_multi(m)
    # Incomplete eye-pool bookshelf, complete tower and pinnacle spiral bookshelf.
    for variant,levels in [('bookshelf_form',3),('phantom_tower',5),('spiral_tower',7),('kingdom_bookshelf',4)]:
        m=model(variant,mats['wood'],(8,0,8),'glow().seeThrough(); suggested opacity 0.65')
        height=28/levels
        if variant=='spiral_tower':
            cube(m,'central_spine',(6.5,0,6.5),(9.5,30,9.5),'brass')
        else:
            cube(m,'left_post',(-3,0,10),(-1,30,12),'wood')
            cube(m,'right_post',(17,0,10),(19,30,12),'wood')
        for level in range(levels):
            first=len(m.elements)
            y=level*height
            shift=2*math.sin(level*math.pi/2) if variant=='spiral_tower' else 0
            cube(m,f'shelf_{level}',(-3+shift,y,3),(19+shift,y+1.2,12),'brass')
            for j in range(5):
                x=-1+j*3.5+shift; bh=height-1.8-(j+level)%3*.35
                cube(m,f'book_{level}_{j}',(x,y+1.2,5),(x+2.8,y+1.2+bh,10),'leather' if j%2 else 'pages')
                if variant!='bookshelf_form':
                    cube(m,f'eye_{level}_{j}',(x+.1,y+1.5,4.7),(x+2.7,y+3.8,5),'eye')
            if variant=='bookshelf_form':
                cube(m,f'eye_pool_{level}',(1,y+1.4,4.5),(15,y+3.5,4.9),'ink_pool')
                cube(m,f'pool_eye_{level}',(5,y+.8,4.2),(11,y+4.8,4.5),'eye')
            if variant=='spiral_tower':
                # Bake quarter turns into box coordinates, then use a legal 45-degree
                # rotation for odd levels. Every tier winds around the same spine.
                angle=(level//2)*math.pi/2
                for element in m.elements[first:]:
                    a,b=element['from'],element['to']
                    corners=[(x,z) for x in (a[0],b[0]) for z in (a[2],b[2])]
                    pts=[(8+(x-8)*math.cos(angle)+(z-8)*math.sin(angle),
                          8-(x-8)*math.sin(angle)+(z-8)*math.cos(angle)) for x,z in corners]
                    element['from']=[round(min(p[0] for p in pts),4),a[1],round(min(p[1] for p in pts),4)]
                    element['to']=[round(max(p[0] for p in pts),4),b[1],round(max(p[1] for p in pts),4)]
                    if level%2: element['rotation']={'axis':'y','angle':45,'origin':[8,0,8]}
        if variant=='spiral_tower':
            cube(m,'crown_eye',(-1,25,4),(17,32,4.3),'eye')
        finish_multi(m)
    # Ground sigil: transparent texture, horizontal geometry, no rectangular aura.
    im=Image.new('RGBA',(128,128)); d=ImageDraw.Draw(im)
    for r in (60,53,39): d.ellipse((64-r,64-r,64+r,64+r),outline=BRASS,width=2)
    for k in range(12):
        a=k*math.tau/12
        x,y=64+46*math.cos(a),64+46*math.sin(a)
        d.line((x-3,y-4,x,y+4,x+3,y-4),fill=GOLD,width=2)
        d.line((64+55*math.cos(a),64+55*math.sin(a),64+61*math.cos(a),64+61*math.sin(a)),fill=CREAM)
    eye(d,(37,52,91,76))
    t=image_texture(root,'ritual_sigil',im)
    m=model('ritual_sigil',t,(8,0,8),'glow().unshaded().seeThrough(); no aura')
    m.cube('ground_sigil',(-8,0,-8),(24,.08,24),'all',skip=('north','south','east','west')); finish(m)
    # Foreseen cast proxy: luminous wire box, never an opaque ghost.
    t=painted_texture(root,'ghost',lambda *args: PALE,16)
    m=model('ghost_outline',t,(8,0,8),'glow().unshaded().seeThrough(); tint to cast pathway')
    for x in (3,12.7):
        for z in (3,12.7): m.cube('vertical',(x,0,z),(x+.3,24,z+.3),'all')
    for y in (0,23.7):
        for z in (3,12.7): m.cube('horizontal',(3,y,z),(13,y+.3,z+.3),'all')
        for x in (3,12.7): m.cube('depth',(x,y,3),(x+.3,y+.3,13),'all')
    finish(m)
    # Thin conjured armor shell, open at the arms and neck.
    m=model('golden_armor',mats['brass'],(8,12,8),'glow().seeThrough(); color alpha 0.55')
    for n,a,b in [('breastplate',(3,8,4),(13,19,4.7)),('backplate',(3,8,11.3),(13,19,12)),('left_flank',(2.5,8,4),(3.2,19,12)),('right_flank',(12.8,8,4),(13.5,19,12)),('left_pauldron',(-1,17,4),(3,20,12)),('right_pauldron',(13,17,4),(17,20,12))]: cube(m,n,a,b,'brass')
    finish_multi(m)
    # Improvise force spear / light beam is recolorable in ModelFx.
    t=painted_texture(root,'spell_light',lambda *args:(255,255,255,220),16)
    m=model('force_spear',t,(8,0,8),'glow().unshaded().seeThrough(); tint per weakness')
    m.cube('shaft',(7.6,0,7.6),(8.4,24,8.4),'all')
    m.cube('tip',(7,23,7),(9,29,9),'all')
    finish(m)
    return records


def particles(root):
    for name in PARTICLES:
        im=Image.new('RGBA',(16,16)); d=ImageDraw.Draw(im); w=(255,255,255,255)
        if name in ('page_0','page_1','page_2'):
            k=int(name[-1]); x=2+k
            d.polygon([(x,2),(12,3),(13-k,14),(2,12)],fill=w)
            for y in (5,8,11): d.line((x+2,y,10,y+1),fill=(140,140,140,255))
        elif name=='ink_drop':
            d.polygon([(8,1),(12,8),(12,12),(9,15),(5,14),(3,10)],fill=w)
            d.line((5,9,5,11),fill=(175,175,175,255))
        elif name=='eye': eye(d,(1,4,14,11),w,(30,30,30,255))
        elif name.startswith('glyph_'):
            k=int(name[-1]); d.line((4,2,4,13,12,13),fill=w)
            for j in range(3):
                yy=3+j*4; d.line((4,yy,11-(j+k)%3,yy),fill=w)
            d.line((7+k,2,11-k,10),fill=w)
        elif name=='madness_pip': d.polygon([(8,1),(14,8),(8,14),(2,8)],fill=w); d.line((8,4,6,8,10,9,8,12),fill=(90,90,90,255))
        elif name=='ghost_line': d.rectangle((1,6,14,8),outline=(255,255,255,150))
        elif name=='thread': d.line((0,7,15,7),fill=w); d.line((0,8,15,8),fill=(255,255,255,80))
        elif name=='page_edge':
            d.polygon([(0,12),(7,7),(15,5),(8,9),(0,14)],fill=w)
        save(root,f'textures/particle/fx/{PREFIX}/{name}.png',im)


def gui(root):
    for name in ABILITIES:
        save(root,f'textures/gui/ability_menus/{PREFIX}/{name}.png',glyph(name))
    for name in ABILITIES+STATUSES:
        save(root,f'textures/gui/{PREFIX}/{name}.png',glyph(name,16))
    save(root,f'textures/gui/pathway_icons/{PREFIX}.png',glyph('tower_of_omniscience',16))
    # Separate pips and a fill strip; the renderer chooses count and percentage.
    for state,col in [('empty',(85,74,91,200)),('filled',VIOLET),('critical',(214,130,241,255))]:
        im=Image.new('RGBA',(8,8)); d=ImageDraw.Draw(im)
        d.polygon([(3,0),(7,3),(3,7),(0,3)],fill=col,outline=GOLD if state=='critical' else None)
        save(root,f'textures/gui/{PREFIX}/madness_pip_{state}.png',im)
    im=Image.new('RGBA',(104,8)); d=ImageDraw.Draw(im)
    d.rectangle((0,0,103,7),outline=BRASS); d.rectangle((2,2,101,5),fill=INK)
    save(root,f'textures/gui/{PREFIX}/analysis_bar_background.png',im)
    im=Image.new('RGBA',(100,4)); d=ImageDraw.Draw(im)
    for x in range(100): d.line((x,0,x,3),fill=tuple(int(BRASS[i]+(GOLD[i]-BRASS[i])*x/99) for i in range(4)))
    save(root,f'textures/gui/{PREFIX}/analysis_bar_fill.png',im)
    # Empty nine-slice friendly dossier/codex panels for dynamic text.
    for name,size in [('dossier_panel',(128,80)),('codex_panel',(256,192))]:
        im=Image.new('RGBA',size); d=ImageDraw.Draw(im)
        d.rounded_rectangle((0,0,size[0]-1,size[1]-1),radius=5,fill=(24,20,27,230),outline=BRASS,width=2)
        d.line((8,23,size[0]-9,23),fill=BRASS)
        eye(d,(size[0]//2-10,7,size[0]//2+10,17))
        save(root,f'textures/gui/{PREFIX}/{name}.png',im)


def synthesize(name,index):
    """Page rustles, brass chimes and ink pulses, composed by cue family."""
    sr=22050; rng=random.Random(8100+index)
    duration=2.8 if name in ('tower_of_omniscience','calamity_of_knowledge','bookshelf_form') else 1.25
    if name in ('page_turn','reading','codex_learn'): duration=.65
    n=int(sr*duration); out=array('h'); prev=0; peak=0
    base=220*2**((index%12)/12)
    dark=name in ('vile_thoughts','calamity_of_knowledge','madness_break','tome_break')
    paper=name in ('page_turn','reading','living_tome','reconstruction','open_source')
    for i in range(n):
        t=i/sr; u=t/duration
        attack=min(1,t/.025); release=min(1,(duration-t)/.12)
        noise=rng.uniform(-1,1); high=noise-prev; prev=noise
        value=0
        for j,ratio in enumerate((1,2.01,3.98)):
            onset=j*.09
            if t>=onset:
                tt=t-onset
                value+=.22/(j+1)*math.sin(math.tau*base*ratio*tt)*math.exp(-tt*(2.6+j))
        if paper:
            value+=high*.2*max(0,math.sin(t*math.pi*9))**4*math.exp(-u*2)
        elif dark:
            value+=.18*math.sin(math.tau*62*t+4*math.sin(math.tau*3*t))*math.sin(math.pi*u)**2
            value+=noise*.09*math.sin(math.pi*u)
        else:
            value+=high*.07*math.exp(-u*7)
            value+=.1*math.sin(math.tau*base*.5*t)*math.sin(math.pi*u)**2
        v=int(max(-.95,min(.95,value*attack*release))*32767)
        peak=max(peak,abs(v)); out.append(v)
    assert 1000<peak<32767
    return sr,out


def sounds(root):
    events=json.loads((root/'sounds.json').read_text(encoding='utf-8')) if (root/'sounds.json').exists() else {}
    langpath=root/'lang/en_us.json'
    lang=json.loads(langpath.read_text(encoding='utf-8')) if langpath.exists() else {}
    for i,name in enumerate(ABILITIES+EXTRA_SOUNDS):
        sr,samples=synthesize(name,i)
        dest=root/f'sounds/{PREFIX}/{name}.ogg'; dest.parent.mkdir(parents=True,exist_ok=True)
        with tempfile.TemporaryDirectory() as temp:
            wav=Path(temp)/'cue.wav'
            with wave.open(str(wav),'wb') as w:
                w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr)
                if sys.byteorder!='little': samples.byteswap()
                w.writeframes(samples.tobytes())
            subprocess.run(['ffmpeg','-v','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','4','-map_metadata','-1',str(dest)],check=True)
        key=f'{PREFIX}.{name}'; sub=f'subtitles.{NS}.{key}'
        events[key]={'subtitle':sub,'sounds':[{'name':f'{NS}:{PREFIX}/{name}'}]}
        lang[sub]='Omniscient Eye: '+name.replace('_',' ').capitalize()
    write_json(root/'sounds.json',events); write_json(langpath,lang)


def manifest(root,records):
    data={'pathway':'Omniscient Eye','palette':{'brass':'#D1A94B','cream':'#F4EBD0','ink':'#221B14','madness':'#733D9D'},
          'scope':'Resources only; ability behavior, dynamic dossier text, countdowns and runtime animation playback require gameplay wiring.',
          'wheels':WHEELS,'abilities':{},'models':records,
          'particles':{n:f'fx/{PREFIX}/{n}' for n in PARTICLES},
          'statuses':STATUSES,'sound_events':[f'{NS}:{PREFIX}.{n}' for n in ABILITIES+EXTRA_SOUNDS],
          'ability_models':{
              'reading':['reading_eye','info_card'],'ritual_circle':['ritual_sigil'],
              'deduction':['ghost_outline'],'keen_eye':['ghost_outline'],
              'reconstruction':['ghost_outline'],'deductive_strike':['cognize_eye'],
              'analyze':['cognize_eye'],'imitate':['living_tome_open'],
              'mirror_curse':['brass_mirror'],'vile_thoughts':['cognize_eye'],
              'improvise':['force_spear'],'living_tome':['living_tome_open','tome_creature_0'],
              'golden_armor_art':['golden_armor'],'prophecy':['ghost_outline'],
              'bookshelf_form':['bookshelf_form','phantom_tower','spiral_tower'],
              'cognize':['cognize_eye'],'leverage':['ritual_sigil'],'decipher':['living_tome_open'],
              'evade_disaster':['ghost_outline'],'wise_counsel':['reading_eye'],
              'omniscience':['tower_eye'],'exploit':['cognize_eye'],'redefine':['living_tome_open'],
              'calamity_of_knowledge':['tower_eye','kingdom_bookshelf'],
              'open_source':['living_tome_open'],'tower_of_omniscience':['spiral_tower','tower_eye','kingdom_bookshelf']},
          'animations':{
              'tome_creature':{'base':'tome_creature','frames':4,'smooth':True,'pivot':[8,0,8],'suggested_ticks':12},
              'ink_pool':{'type':'texture','frames':4,'frametime':3,'interpolate':True},
              'eye_scan':{'type':'ModelFx keys','model':'tower_eye','suggestion':'yaw sweep -45 to 45 degrees, paired with a thread particle tether'},
              'bookshelf_growth':{'type':'ModelFx keys','suggestion':'scale 0.1 to 1 over 12 ticks; alpha fade; scale phantom_tower to 3.2 for six blocks'},
          },
          'reused_runtime_effects':['SpellFx.runeCircle','SpellFx.closingRing','SpellFx.tether','SpellFx.overHead','ParticleShapes.model','SlashFx'],
          'optional_melee':{'particles':['page_0','page_1','page_2','eye','ink_drop','page_edge'],'head':'#F4EBD0','middle':'#D1A94B','tail':'#221B14'}}
    for name in ABILITIES:
        data['abilities'][name]={'wheel_icon':f'textures/gui/ability_menus/{PREFIX}/{name}.png',
                                 'hud_icon':f'textures/gui/{PREFIX}/{name}.png',
                                 'sound':f'{NS}:{PREFIX}.{name}'}
    owned=[]
    for folder in [f'models/model_fx/{PREFIX}',f'textures/model_fx/{PREFIX}',f'textures/gui/ability_menus/{PREFIX}',f'textures/gui/{PREFIX}',f'textures/particle/fx/{PREFIX}',f'sounds/{PREFIX}']:
        owned.extend(p for p in (root/folder).rglob('*') if p.is_file())
    owned.append(root/f'textures/gui/pathway_icons/{PREFIX}.png')
    # Ogg containers can use random stream serials; hashes record actual packaged bytes.
    data['files']={p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(owned)}
    write_json(root/f'{PREFIX}/asset_manifest.json',data)
    return data


def preview(root,path):
    font=ImageFont.truetype('C:/Windows/Fonts/consola.ttf',14) if Path('C:/Windows/Fonts/consola.ttf').exists() else ImageFont.load_default()
    title=ImageFont.truetype('C:/Windows/Fonts/georgia.ttf',28) if Path('C:/Windows/Fonts/georgia.ttf').exists() else font
    sheet=Image.new('RGB',(1120,1040),(22,20,27)); d=ImageDraw.Draw(sheet)
    d.text((28,20),'OMNISCIENT EYE  /  resource collection',font=title,fill=GOLD)
    d.text((28,61),'BRASS EYES  /  AGED PAGES  /  BLACK INK  /  VIOLET MADNESS',font=font,fill=CREAM)
    for i,name in enumerate(ABILITIES):
        x=28+i%9*122; y=104+i//9*130
        im=glyph(name).resize((64,64),Image.Resampling.NEAREST); sheet.paste(im,(x+20,y),im)
        label=name.replace('_',' ').split(); lines=[]; line=''
        for word in label:
            if len(line+' '+word)>13: lines.append(line); line=word
            else: line=(line+' '+word).strip()
        lines.append(line)
        d.multiline_text((x,y+70),'\n'.join(lines),font=font,fill=CREAM,spacing=1)
    d.text((28,505),'HUD / statuses and passives (native 16px, enlarged 3x)',font=font,fill=GOLD)
    for i,name in enumerate(STATUSES):
        im=glyph(name,16).resize((48,48),Image.Resampling.NEAREST)
        sheet.paste(im,(28+i%18*60,535),im)
    d.text((28,610),'TEXTURES / transparent eye, ritual sigil, dossier, mirror, particle vocabulary',font=font,fill=GOLD)
    for i,name in enumerate(['tower_eye','ritual_sigil','info_card','mirror_glass']):
        im=Image.open(root/f'textures/model_fx/{PREFIX}/{name}.png').convert('RGBA').resize((160,160),Image.Resampling.NEAREST)
        x=28+i*190; sheet.paste(im,(x,645),im); d.text((x,815),name,font=font,fill=CREAM)
    for i,name in enumerate(PARTICLES):
        im=Image.open(root/f'textures/particle/fx/{PREFIX}/{name}.png').resize((48,48),Image.Resampling.NEAREST)
        sheet.paste(im,(28+i*80,875),im)
        d.text((28+i*80,930),name.replace('_','\n'),font=font,fill=CREAM)
    path.parent.mkdir(parents=True,exist_ok=True); sheet.save(path)


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--assets',type=Path,default=DEFAULT_ASSETS)
    p.add_argument('--preview',type=Path,default=REPO/'docs/assets/omniscient_eye/preview.png')
    args=p.parse_args(); records=models(args.assets)
    gui(args.assets); particles(args.assets); sounds(args.assets)
    data=manifest(args.assets,records); preview(args.assets,args.preview)
    print(f'Generated {len(records)} models, {len(ABILITIES)} ability icons, {len(ABILITIES)+len(STATUSES)} HUD icons, '
          f'{len(PARTICLES)} particles and {len(ABILITIES)+len(EXTRA_SOUNDS)} mono sounds; {len(data["files"])} files.')


if __name__=='__main__': main()
