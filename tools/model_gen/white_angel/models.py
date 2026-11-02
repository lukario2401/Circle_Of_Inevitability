"""Authored cube geometry, UV materials and four compatible Sunbird wing poses."""
import math
import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from modelkit import Model, Texture
from icons import GOLD, PALE, WHITE, IVORY, ORANGE


def material(animated=False):
    tex=Texture('white_angel/'+('sunfire_atlas' if animated else 'divine_atlas'),64,64,
                frames=4 if animated else 1,frametime=3,interpolate=True)
    colors={'gold':GOLD,'ivory':IVORY,'light':WHITE,'flame':ORANGE,
            'feather':WHITE,'inscription':PALE,'sun':GOLD,'shadow':(135,87,28,255)}
    for i,(name,color) in enumerate(colors.items()):
        def paint(tx,ty,x,y,frame,c=color,n=name):
            # Feather barbs, hammered metal and script bands stay legible on small cubes.
            line=(x+y//2)%6==0 if n=='feather' else ((x//3+y//3)%4==0 if n=='inscription' else y%8==0)
            shade=12*math.sin(tx*math.pi)-22*ty+(14 if line else 0)
            if animated:shade+=24*math.sin(frame*math.pi/2+tx*math.tau+ty*3)
            if n=='sun':shade+=24*math.sin(x*.7+y*.8+frame*math.pi/2)
            return tuple(max(0,min(255,int(c[j]+shade))) for j in range(3))+(255,)
        x=i%4*4;y=i//4*8
        tex.zone(name,x,y,x+4,y+8,paint)
    return tex


def ring(m, name, radius, y, zone='gold', thickness=.8, count=16):
    for i in range(count):
        a=i*math.tau/count;x=8+radius*math.cos(a);z=8+radius*math.sin(a)
        # Tangent segments meet at the corners of a regular polygon; legal Java rotations only.
        length=radius*math.tan(math.pi/count)+.12
        degrees=i*360/count;quarter=math.floor((degrees+45)/90)
        angle=-degrees+quarter*90
        dx,dz=(length,thickness/2) if quarter%2 else (thickness/2,length)
        m.cube(f'{name}_{i}',(x-dx,y,z-dz),(x+dx,y+.6,z+dz),zone,
               rot=(angle,'y',(x,y,z)))


def orb(m, radius=7):
    # Voxel latitude strips follow the sphere in both X and Z, leaving a rounded silhouette.
    for i in range(-5,6):
        y=i*radius/6; r=math.sqrt(max(0,radius*radius-y*y))
        for j in range(-4,5):
            z=j*r/5;half_z=r/10+.02;half_x=math.sqrt(max(0,r*r-z*z))
            m.cube(f'solar_band_{i+5}_{j+4}',(8-half_x,8+y-radius/12,8+z-half_z),
                   (8+half_x,8+y+radius/12,8+z+half_z),'sun')
    for i in range(8):
        a=i*math.tau/8;x=8+math.cos(a)*(radius+1.3);z=8+math.sin(a)*(radius+1.3)
        m.cube(f'corona_{i}',(x-.5,7,z-.5),(x+.5,9,z+.5),'light')


def sunbird(m, frame=0, complete=True):
    m.cube('breast',(5.5,4,5),(10.5,12,11),'gold')
    m.cube('neck',(6.5,11,4.5),(9.5,16,8),'feather')
    m.cube('head',(6,15,2.5),(10,19,7),'light')
    m.cube('beak',(7,15.5,.5),(9,17,3),'gold')
    for x in (5.8,9.8):m.cube('eye_'+str(x),(x,17,3.5),(x+.4,17.6,4.5),'shadow')
    for i in range(3):m.cube(f'crest_{i}',(6.5+i*.7,18,5),(7+i*.7,21+i*.5,6),'flame')
    ring(m,'crown',4,22,'gold',.6)
    # Topology is identical across poses; only feather positions change.
    flap=(0,3,0,-3)[frame];span=1 if complete else .64
    for side in (-1,1):
        for i in range(9):
            x=8+side*(3+i*2.1)*span
            y=10+i*.7+flap*(i+1)/9
            reach=(12-i*.35)*(1 if complete else .65)
            m.cube(f'wing_{side}_{i}',(x-1.1,y,6+i*.32),(x+1.1,y+1.1,6+i*.32+reach),
                   'feather' if i%2==0 else 'gold')
        for i in range(3):
            x=8+side*(1+i*.9)
            m.cube(f'tail_{side}_{i}',(x-.55,2-i,10),(x+.55,3-i,18+i*2),'flame' if i==2 else 'feather')
    for x in (6,9):m.cube(f'talon_{x}',(x,2,4),(x+1,5,8),'gold')


def armor(m, part):
    # Hollow pieces leave the wearer visible; body pivot is feet at (8,0,8).
    if part in ('all','chest'):
        for z in (5.2,10.2):m.cube(f'breast_{z}',(3.7,12,z),(12.3,23.4,z+.6),'ivory')
        for x in (3.5,11.9):m.cube(f'flank_{x}',(x,12,5.8),(x+.6,23.4,10.2),'ivory')
        for x in (1.3,11.7):m.cube(f'pauldron_{x}',(x,21,5),(x+3,24.5,11),'feather')
        m.cube('sun_emblem',(6.5,17,4.7),(9.5,20,5.3),'gold')
        m.cube('belt',(3.5,12,5),(12.5,13,11),'gold')
    if part in ('all','head'):
        for x in (3.6,11.8):m.cube(f'helm_side_{x}',(x,24,3.6),(x+.6,31.4,12.4),'ivory')
        m.cube('helm_back',(4.2,24,11.8),(11.8,31.4,12.4),'ivory')
        m.cube('brow',(4.2,29.5,3.6),(11.8,30.4,4.2),'gold')
        m.cube('crest',(7.5,30,6),(8.5,32,10),'light')
    if part in ('all','legs'):
        for x in (3.6,8.1):
            for z in (5.2,10.2):m.cube(f'greave_{x}_{z}',(x,3,z),(x+4.3,11.8,z+.6),'ivory')
            m.cube(f'knee_{x}',(x,9,4.8),(x+4.3,11.5,5.5),'gold')


def generate(assets):
    solid,animated=material(),material(True)
    solid.save(assets);animated.save(assets)
    records=[]
    def make(name,fn,texture=solid,pivot='center (8,8,8)',note=''):
        m=Model('white_angel/'+name,texture,'White Angel / original solar, ivory and gold geometry.')
        fn(m);m.save(assets)
        records.append({'name':name,'elements':len(m.elements),'pivot':pivot,
                        'pivot_pixels':[8,0,8] if 'grip' in pivot or 'feet' in pivot else [8,8,8],'notes':note,
                        'bounds':[list(map(min,zip(*(e['from'] for e in m.elements)))),
                                  list(map(max,zip(*(e['to'] for e in m.elements))))]})
    make('sun_halo',lambda m:ring(m,'halo',7,8))
    def mace(m):
        m.cube('grip',(7.2,0,7.2),(8.8,13,8.8),'gold')
        for y in (1,4,7,10):m.cube(f'grip_band_{y}',(7, y,7),(9,y+.6,9),'ivory')
        m.cube('pommel',(6.5,-1,6.5),(9.5,1,9.5),'light')
        m.cube('heart',(5.5,14,5.5),(10.5,20,10.5),'light')
        for x,z in ((3,7),(11,7),(7,3),(7,11)):m.cube(f'flange_{x}_{z}',(x,12,z),(x+2,22,z+2),'gold')
        m.cube('crown',(7,20,7),(9,24,9),'light')
    make('holy_mace',mace,pivot='grip at (8,0,8); top points +Y')
    def spear(m):
        m.cube('shaft',(7.5,-8,7.5),(8.5,20,8.5),'gold')
        m.cube('head',(6.5,19,7.3),(9.5,26,8.7),'light')
        m.cube('tip',(7.4,26,7.4),(8.6,30,8.6),'light')
        for s in (-1,1):
            for i in range(4):
                x=8+s*(2+i*.9)
                m.cube(f'barb_{s}_{i}',(x-.6,17-i*1.2,7.5),(x+.6,22-i*1.6,8.5),'feather',
                       rot=(s*22.5,'z',(x,20-i,8)))
    make('spear_of_light',spear,pivot='shaft center (8,8,8); tip +Y',note='Use modelAlong for travel.')
    def scales(m):
        m.cube('base',(4,0,5),(12,1,11),'gold');m.cube('pillar',(7.5,1,7.5),(8.5,17,8.5),'gold')
        m.cube('beam',(-2,15,7.5),(18,16,8.5),'ivory')
        for x in (-1,17):
            for z in (5,11):m.cube(f'chain_{x}_{z}',(x-.2,7,z-.2),(x+.2,15,z+.2),'gold')
            m.cube(f'pan_{x}',(x-3,6,4),(x+3,7,12),'gold')
            for z in (4,11.5):m.cube(f'pan_rim_{x}_{z}',(x-3,7,z),(x+3,8,z+.5),'ivory')
        m.cube('finial',(7,17,7),(9,20,9),'light')
    make('golden_scales',scales)
    def blade(m):
        m.cube('blade',(6,5,7.5),(10,25,8.5),'light')
        m.cube('point',(7,25,7.5),(9,30,8.5),'light')
        m.cube('fuller',(7.7,5,7.3),(8.3,24,7.5),'gold')
        m.cube('guard',(1,4,6.5),(15,6,9.5),'gold');m.cube('hilt',(7,0,7),(9,4,9),'ivory')
        m.cube('pommel',(6.5,-2,6.5),(9.5,0,9.5),'gold')
    make('blade_of_light',blade,pivot='grip (8,0,8); blade +Y')
    make('sun',lambda m:orb(m),animated,note='Scale in world; animated sunfire material. Diameter 14/16 blocks.')
    def note(m):
        for x,y in ((4,2),(12,6)):
            m.cube(f'head_{x}',(x-2,y,7),(x+2,y+2,9),'gold')
            m.cube(f'stem_{x}',(x+1,y+1,7.5),(x+2,y+13,8.5),'light')
        m.cube('beam',(5,15,7.5),(14,17,8.5),'gold')
    make('hymn_note',note)
    for complete,name in ((True,'sunbird'),(False,'sunbird_incomplete')):
        make(name,lambda m,c=complete:sunbird(m,0,c),pivot='body center (8,8,8); front -Z')
        for frame in range(4):
            make(f'{name}_{frame}',lambda m,f=frame,c=complete:sunbird(m,f,c),
                 note=f'Matching topology for ModelFx.of("white_angel/{name}").frames(4).smooth().')
    for part,suffix in (('all',''),('chest','_chest'),('head','_head'),('legs','_legs')):
        make('holy_armor'+suffix,lambda m,p=part:armor(m,p),pivot='feet (8,0,8)',
             note='Attach at feet. Separate pieces available; final limb tracking belongs to ability code.')
    def scroll(m):
        m.cube('parchment',(3,1,7.7),(13,17,8.3),'ivory')
        for y in (0,17):m.cube(f'roller_{y}',(2,y,7),(14,y+1,9),'gold')
        for y in (5,8,11):m.cube(f'script_{y}',(5,y,7.4),(11,y+.5,7.7),'inscription')
        m.cube('seal',(8,2,7),(11,5,7.7),'sun')
    make('contract_scroll',scroll)
    for name,r,zone in (('warning_sigil',11,'gold'),('purification_ring',10,'light'),
                        ('domain_boundary',14,'inscription'),('justice_halo',9,'gold'),('oath_sigil',5,'ivory')):
        def sigil(m,r=r,zone=zone):
            ring(m,'outer',r,8,zone);ring(m,'inner',r*.78,8,'light',.5)
            for i in range(8):
                a=i*math.tau/8;x=8+math.cos(a)*r*.9;z=8+math.sin(a)*r*.9
                m.cube(f'rune_{i}',(x-.35,8,z-.35),(x+.35,8.9,z+.35),'gold')
        make(name,sigil,note='Horizontal ring; place on ground or above head, then scale by radius.')
    def gate(m):
        for x in (0,13):
            m.cube(f'pillar_{x}',(x,0,6),(x+3,25,10),'ivory')
            for y in (1,20,24):m.cube(f'capital_{x}_{y}',(x-.4,y,5.5),(x+3.4,y+1,10.5),'gold')
        m.cube('lintel',(-1,25,5.5),(17,27,10.5),'ivory')
        m.cube('crown',(5,27,6),(11,31,10),'sun')
        for x in (3.5,6.5,9.5,12):m.cube(f'light_bar_{x}',(x,2,7.8),(x+.35,24,8.2),'light')
    make('kingdom_gate',gate,note='Illusory repeated prop; scale/placement supplied by Holy Kingdom.')
    def eye(m):
        m.cube('eye',(2,6,7.5),(14,10,8.5),'ivory')
        m.cube('iris',(6,5,7.2),(10,11,7.5),'gold');m.cube('pupil',(7.5,6,7),(8.5,10,7.2),'light')
        for x in (0,14):m.cube(f'corner_{x}',(x,7,7.5),(x+2,9,8.5),'gold')
    make('holy_eye',eye)
    make('notary_seal',lambda m:(ring(m,'seal',6,8),m.cube('stamp',(6,8,6),(10,8.7,10),'inscription')))
    make('ray_core',lambda m:(m.cube('core',(7.7,0,7.7),(8.3,16,8.3),'light'),
                             m.cube('mantle',(7.3,0,7.3),(8.7,16,8.7),'gold')),
         note='Unit beam aligned +Y; scale length to aim, glow/unshaded at runtime.')
    make('believer_sun',lambda m:orb(m,3),note='Small overhead marker; attach above Believers.')
    def plume(m):
        for i in range(7):
            a=i*math.tau/7;x=8+math.cos(a)*2.5;z=8+math.sin(a)*2.5
            m.cube(f'tongue_{i}',(x-.6,0,z-.6),(x+.6,8+(i%3)*3,z+.6),'flame')
        m.cube('heart',(7,0,7),(9,17,9),'light')
    make('flame_plume',plume,animated)
    return records
