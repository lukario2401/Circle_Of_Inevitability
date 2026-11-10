"""Original cube geometry and textured materials for Nature Walker's life/return cycle."""
import math
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from modelkit import Model, Texture
from catalog import GROWTHS, ANIMATED

COLORS = {'bark':(91,56,38),'soil':(65,47,38),'leaf':(59,121,65),'lightleaf':(135,184,78),
          'gold':(221,178,79),'petal':(242,199,173),'fruit':(192,66,89),'ivory':(223,227,194),
          'stone':(119,133,126),'steel':(135,159,166),'mud':(87,76,49),'ruby':(221,52,66),
          'flesh':(166,103,101),'fungus':(141,105,166),'spirit':(138,203,183),'dark':(35,49,40)}


def material(animated=False):
    t=Texture('nature_walker/'+('cycle_atlas' if animated else 'earth_atlas'),64,64,
              frames=4 if animated else 1,frametime=4)
    for i,(name,color) in enumerate(COLORS.items()):
        def paint(tx,ty,x,y,f,c=color,n=name):
            noise=((x*17+y*31+x*y*3)%23)-11
            grain=(x%5==0)*-22 if n in ('bark','gold') else ((x//3+y//3)%2)*9
            shine=22*math.sin(f*math.pi/2+tx*5+ty*3) if animated else 0
            return tuple(max(0,min(255,int(v+noise+grain+shine+12*(1-ty)))) for v in c)+(255,)
        u=i%4*4;v=i//4*4;t.zone(name,u,v,u+4,v+4,paint)
    return t


def ring(m,name='ring',radius=7,y=.5,zone='leaf'):
    for i in range(16):
        a=i*math.tau/16;x=8+radius*math.cos(a);z=8+radius*math.sin(a)
        half=radius*math.tan(math.pi/16)+.12;q=math.floor((i*22.5+45)/90)
        dx,dz=(half,.4) if q%2 else (.4,half)
        m.cube(f'{name}_{i}',(x-dx,y-.3,z-dz),(x+dx,y+.3,z+dz),zone,
               rot=(-i*22.5+q*90,'y',(x,y,z)))


def plant(m,kind,stage):
    dead=stage=='withered';leaf='bark' if dead else 'leaf';fruit='soil' if dead else 'fruit'
    m.cube('earth',(3,0,3),(13,1.2,13),'soil')
    if stage=='seed':
        m.cube('seed',(6,1,6),(10,3.4,10),'gold' if kind=='heartfruit' else 'bark')
        m.cube('seam',(7.7,2.8,5.9),(8.2,3.6,10.1),'ivory');return
    h=6 if stage=='sprout' else (23 if kind=='mother_tree' else 14)
    if kind=='sporebed':
        for i,(x,z) in enumerate(((5,5),(10,9),(5,11),(11,4))):
            hh=(3+i%3*1.5)*(0.65 if stage=='sprout' else 1)
            m.cube(f'stalk_{i}',(x-.6,1,z-.6),(x+.6,hh+1,z+.6),'ivory' if not dead else 'bark')
            m.cube(f'cap_{i}',(x-2,hh,z-2),(x+2,hh+1.5,z+2),'fungus' if not dead else 'soil')
            m.cube(f'spot_{i}',(x-.4,hh+1.5,z-.5),(x+.6,hh+1.7,z+.5),'petal')
        return
    m.cube('trunk',(6.8,1,6.8),(9.2,h,9.2),'bark')
    for i in range(6 if h>10 else 2):
        side=(-1)**i;y=3+i*(h-4)/6;x=8+side*(3+i%2)
        lo,hi=sorted((8,x));m.cube(f'branch_{i}',(lo,y,7.4),(hi+.5,y+.8,8.6),'bark')
        if kind=='thornling':
            m.cube(f'thorn_{i}',(x-.6,y,7.4),(x+.6,y+2.7,8.6),'ivory' if not dead else 'bark',rot=(side*22.5,'z',(x,y,8)))
        m.cube(f'leaf_{i}',(x-1.5,y+.7,6.7),(x+1.5,y+1.3,9.3),leaf,rot=(side*22.5,'z',(x,y+1,8)))
    if kind in ('heartfruit','mother_tree','briar_orchard') and stage!='sprout':
        for i,(x,z) in enumerate(((4,6),(10,5),(8,11))):
            m.cube(f'crown_{i}',(x-3,h-4,z-3),(x+3,h,z+3),leaf)
            if not dead:
                m.cube(f'flower_{i}',(x-1,h,z-1),(x+1,h+.6,z+1),'petal')
            if stage=='ripe' or kind=='briar_orchard':
                m.cube(f'fruit_{i}',(x-1.2,h-7,z-1.2),(x+1.2,h-3.5,z+1.2),fruit)
    if kind=='briar_orchard':roots(m,4,8,leaf)


def roots(m,count=7,height=15,zone='bark',prefix='root'):
    for i in range(count):
        a=i*math.tau/count;x=8+5*math.cos(a);z=8+5*math.sin(a)
        for j in range(3):
            xx=x+math.cos(a)*j;zz=z+math.sin(a)*j;w=1.2-j*.3;y=j*height/3
            m.cube(f'{prefix}_{i}_{j}',(xx-w,y,zz-w),(xx+w,y+height/3+.5,zz+w),zone,
                   rot=((1 if i%2 else -1)*22.5,'z',(xx,y,zz)))
        m.cube(f'{prefix}_thorn_{i}',(x-.4,3,z-2),(x+.4,5,z+2),'ivory')


def golem(m,kind,frame):
    zone={'stone_golem':'stone','mud_golem':'mud','steel_golem':'steel'}[kind]
    m.cube('body',(3,8,4),(13,21,12),zone);m.cube('head',(5,21,5),(11,27,11),zone)
    for s in (-1,1):
        x=8+s*8;y=(0,1.4,0,-1.4)[frame]*s
        m.cube(f'arm_{s}',(x-2,6+y,4),(x+2,20+y,11),zone)
        m.cube(f'shoulder_{s}',(x-2.6,18+y,3.5),(x+2.6,22+y,11.5),zone)
        x=8+s*3;z=(0,1.4,0,-1.4)[frame]*s
        m.cube(f'leg_{s}',(x-2,0,4+z),(x+2,9,11+z),zone)
        m.cube(f'eye_{s}',(8+s*1.6-.5,23,4.6),(8+s*1.6+.5,24,5.1),'ruby')
    m.cube('heart',(6.8,14,3.6),(9.2,17,4.2),'spirit')
    if kind=='stone_golem':
        for i in range(5):m.cube(f'moss_{i}',(3+i*2,18-i%3,3.6),(4+i*2,21-i%3,4.3),'leaf')
    elif kind=='mud_golem':
        for i in range(4):m.cube(f'drip_{i}',(3+i*2.5,5+i%2,3.5),(4.5+i*2.5,10,5),'mud')
    else:
        for y in (9,19):m.cube(f'band_{y}',(2.8,y,3.8),(13.2,y+1,12.2),'gold')


def bird(m,frame):
    m.cube('body',(5,6,4),(11,12,13),'stone');m.cube('head',(5.5,11,1),(10.5,16,6),'stone')
    m.cube('beak',(7,11,-2),(9,13,2),'gold')
    for s in (-1,1):
        m.cube(f'eye_{s}',(8+s*2.5-.25,13,2),(8+s*2.5+.25,14,3.5),'ruby')
        for i in range(5):
            x=8+s*(5+i*2);y=10+(0,3,0,-3)[frame]*(i+1)/5
            m.cube(f'feather_{s}_{i}',(x-1.2,y,5+i*.7),(x+1.2,y+1,14+i*.4),'stone')
        m.cube(f'talon_{s}',(8+s*2-.4,3,7),(8+s*2+.4,7,10),'gold')
    for i in range(3):m.cube(f'tail_{i}',(5+i*2,7,12),(6.7+i*2,8,20),'stone')


def bear(m,frame):
    m.cube('body',(2,7,4),(14,18,21),'bark');m.cube('head',(2,13,-2),(14,22,7),'bark')
    m.cube('muzzle',(5,13,-5),(11,17,0),'mud');m.cube('nose',(6,16,-5.4),(10,18,-4.8),'dark')
    for s in (-1,1):
        x=8+s*4
        m.cube(f'ear_{s}',(x-1.4,21,0),(x+1.4,24,3),'bark')
        m.cube(f'eye_{s}',(x-.5,18,-2.4),(x+.5,19,-1.8),'gold')
        for j,z in enumerate((4,16)):
            shift=(0,1.5,0,-1.5)[frame]*s*(-1)**j
            m.cube(f'leg_{s}_{j}',(x-2,0,z+shift),(x+2,9,z+5+shift),'bark')
            for k in range(3):m.cube(f'claw_{s}_{j}_{k}',(x-1.6+k,0,z-1+shift),(x-1+k,1,z+1+shift),'ivory')


def brood(m,frame,complete=False):
    h=22 if complete else 15
    m.cube('soil_body',(2,1,2),(14,h,14),'soil');m.cube('living_core',(4,5,1.5),(12,h-2,4),'flesh')
    m.cube('heart',(6,h/2,1),(10,h/2+3,2),'fruit')
    for i in range(8):
        a=i*math.tau/8;x=8+7*math.cos(a);z=8+7*math.sin(a)
        sway=(0,1,0,-1)[frame]*(-1)**i
        m.cube(f'tendril_{i}',(x-1,0,z-1),(x+1,7+i%3+sway,z+1),'leaf' if i%2 else 'bark')
        m.cube(f'wheat_{i}',(x-.3,h-1,z-.3),(x+.3,h+5+i%3,z+.3),'gold')
        for k in range(3):m.cube(f'grain_{i}_{k}',(x-1,h+1+k,z-.5),(x+1,h+1.7+k,z+.5),'gold')
    for i in range(4 if complete else 2):
        x=3+i*3.3;y=h-3+(.5 if frame==1 else 0)
        m.cube(f'mushroom_stem_{i}',(x-.4,y,7),(x+.4,y+4,8),'ivory')
        m.cube(f'mushroom_cap_{i}',(x-2,y+3,5.5),(x+2,y+4.5,9.5),'fungus')
    if complete:
        ring(m,'halo',10,h+6,'gold')
        for i in range(3):m.cube(f'face_{i}',(4+i*3,h-5,1.1),(5+i*3,h-4,1.6),'spirit')


def spirit(m,frame):
    bob=(0,1,0,-1)[frame]
    m.cube('head',(5,13+bob,5),(11,19+bob,11),'spirit')
    m.cube('veil',(3,5+bob,5),(13,14+bob,11),'ivory')
    for s in (-1,1):
        m.cube(f'eye_{s}',(8+s*1.8-.5,15+bob,4.6),(8+s*1.8+.5,16+bob,5),'dark')
        x=8+s*7;m.cube(f'arm_{s}',(x-1,7+bob,6),(x+1,14+bob,9),'spirit')
    for i in range(5):m.cube(f'tatter_{i}',(3+i*2,1+(i%3)+bob,6),(4.5+i*2,6+bob,10),'spirit')


def generate(root):
    solid,magic=material(),material(True);solid.save(root);magic.save(root);records=[]
    def make(name,fn,animated=False,pivot=(8,0,8)):
        m=Model('nature_walker/'+name,magic if animated else solid,'Nature Walker / original geometry.');fn(m);m.save(root)
        records.append({'name':name,'elements':len(m.elements),'pivot_pixels':list(pivot),
                        'bounds':[[min(e['from'][i] for e in m.elements) for i in range(3)],
                                  [max(e['to'][i] for e in m.elements) for i in range(3)]]})
    for kind,stages in GROWTHS.items():
        for stage in stages:make(kind+'_'+stage,lambda m,k=kind,s=stage:plant(m,k,s))
    make('seed',lambda m:plant(m,'thornling','seed'))
    def fruit(m):
        for s in (-1,1):m.cube('lobe_'+str(s),(8+s*1.5-2,4,5),(8+s*1.5+2,9,11),'fruit')
        m.cube('tip',(6.5,2,6),(9.5,5,10),'fruit');m.cube('stem',(7.5,9,7.5),(8.5,12,8.5),'bark')
        m.cube('leaf',(8,10,7),(12,11,9),'lightleaf')
    make('heartfruit',fruit)
    for name,h,z in (('vine_burst',11,'leaf'),('root_burst',14,'bark'),('root_eruption',24,'bark')):
        make(name,lambda m,h=h,z=z:roots(m,8,h,z))
    def disc(m):
        for i in range(9):
            x=i*1.7;w=5+3*math.sin(i*math.pi/8)
            m.cube(f'mire_{i}',(x,0,8-w),(x+1.8,.4,8+w),'mud')
        for i in range(3):ring(m,'ripple_'+str(i),2+i*2,.5,'soil')
    make('mire_disc',disc)
    def coffin(m):
        for x in (2,12):m.cube('side_'+str(x),(x,0,3),(x+2,22,13),'soil')
        for z in (2,12):m.cube('wall_'+str(z),(4,0,z),(12,22,z+2),'bark')
        for y in (0,22):m.cube('cap_'+str(y),(2,y,2),(14,y+1.5,14),'soil')
        for i in range(4):m.cube(f'root_band_{i}',(1.8,3+i*5,1.7),(14.2,4+i*5,2.2),'leaf')
    make('coffin',coffin)
    def rift(m):
        for i in range(10):
            z=-8+i*3;x=8+math.sin(i*1.8)*2
            m.cube(f'depth_{i}',(x-1.3,0,z),(x+1.3,.3,z+3.5),'dark')
            for s in (-1,1):m.cube(f'lip_{i}_{s}',(x+s*2-.6,.2,z),(x+s*2+.6,1.3+i%2,z+3),'soil')
    make('rift',rift)
    def grave(m):
        m.cube('marker',(5,0,7),(11,9,9),'ivory');m.cube('cap',(6,9,7),(10,11,9),'ivory')
        m.cube('rune',(7.5,3,6.7),(8.5,8,7.1),'spirit');ring(m,'grave',5,.3,'spirit')
    make('grave_mark',grave,True)
    def armor(m,zone):
        for x in (3,12):m.cube('side_'+str(x),(x,7,5),(x+1,20,11),zone)
        for z in (4,11):m.cube('plate_'+str(z),(3,7,z),(13,20,z+1),zone)
        for s in (-1,1):
            x=8+s*7;m.cube('shoulder_'+str(s),(x-2,17,4),(x+2,21,12),zone)
        for i in range(4):m.cube(f'vein_{i}',(4+i*2,9+i%2,3.8),(4.6+i*2,18-i%2,4.2),'leaf')
    make('bark_skin_wood',lambda m:armor(m,'bark'));make('bark_skin_stone',lambda m:armor(m,'stone'))
    for name in ANIMATED:
        def pose(m,f=0,n=name):
            if n.endswith('golem'):golem(m,n,f)
            elif n=='stone_bird':bird(m,f)
            elif n=='bear_form':bear(m,f)
            elif n=='evil_spirit':spirit(m,f)
            else:brood(m,f,n.endswith('complete'))
        make(name,pose,name=='evil_spirit')
        for f in range(4):make(f'{name}_{f}',lambda m,f=f:pose(m,f),name=='evil_spirit')
    def lattice(m,death=False):
        for x in (1,7.5,14):m.cube('trunk_'+str(x),(x,0,7),(x+1,16,9),'bark')
        for i in range(4):
            y=2+i*4;m.cube(f'bough_{i}',(0,y,7),(16,y+.8,9),'gold' if death else 'leaf')
            m.cube(f'bloom_{i}',(3+i%2*7,y,6.5),(6+i%2*7,y+2,9.5),'ivory' if death else 'petal')
        if death:m.cube('return_rune',(7,5,6.7),(9,11,7),'spirit')
    make('world_of_nature_tile',lattice);make('bountiful_earth_tile',lambda m:lattice(m,True),True)
    make('verdant_barrier',lambda m:(lattice(m),plant(m,'sporebed','bloom')))
    for name,zone in (('life_ring','lightleaf'),('wither_ring','bark'),('warning_ring','gold'),
                      ('earth_portal','soil'),('nature_portal','spirit'),('awaken_sigil','gold'),
                      ('flora_sigil','leaf'),('harvest_crown','gold')):
        def sigil(m,n=name,z=zone):
            ring(m,'outer',7,.5,z);ring(m,'inner',4,.7,z)
            for i in range(4):
                a=i*math.pi/2;x=8+5.5*math.cos(a);zz=8+5.5*math.sin(a)
                m.cube(f'seed_{i}',(x-.5,1,zz-.5),(x+.5,2.5,zz+.5),'ivory')
            if n in ('nature_portal','harvest_crown'):roots(m,4,8,z)
        make(name,sigil,True)
    def blossom(m):
        for i in range(8):
            a=i*math.pi/4;x=8+4*math.cos(a);z=8+4*math.sin(a)
            m.cube(f'petal_{i}',(x-1.7,3,z-1.7),(x+1.7,5,z+1.7),'petal')
        m.cube('heart',(6.5,3,6.5),(9.5,6,9.5),'gold')
    make('healing_blossom',blossom,True)
    def suture(m):
        for i in range(10):
            y=i*1.6;x=8+3*math.sin(i*.8)
            m.cube(f'thread_{i}',(x-.3,y,7.7),(x+.3,y+1.8,8.3),'spirit')
            if i%2==0:m.cube(f'stitch_{i}',(5,y,7.6),(11,y+.5,8.4),'ivory')
    make('soul_suture',suture,True)
    make('hybrid_crown',lambda m:(ring(m,'crown',5,12,'leaf'),roots(m,4,15,'leaf')))
    def cloud(m,storm=False):
        for i in range(4):m.cube(f'cloud_{i}',(i*3,13+i%2*2,4),(i*3+6,18+i%2*2,12),'stone' if storm else 'ivory')
        for i in range(6):m.cube(f'rain_{i}',(2+i*2,3+i%3*2,7),(2.4+i*2,7+i%3*2,7.4),'spirit')
    make('rain_cloud',cloud);make('storm_cloud',lambda m:cloud(m,True))
    make('sun_disc',lambda m:(ring(m,'sun',6,8,'gold'),m.cube('sun',(4,7,4),(12,9,12),'ivory')),True)
    def scythe(m):
        m.cube('haft',(7.3,0,7.3),(8.7,25,8.7),'bark')
        for i in range(6):m.cube(f'blade_{i}',(8+i*2,24-i*.7,7.5),(11+i*2,26-i*.7,8.5),'gold')
        m.cube('grip',(7,6,7),(9,11,9),'leaf')
    make('harvest_scythe',scythe)
    make('falling_stone',lambda m:m.cube('rock',(3,2,3),(13,14,13),'stone',rot=(22.5,'z',(8,8,8))))
    make('undertow',lambda m:[ring(m,'current_'+str(i),7-i*2,i*3,'spirit') for i in range(3)],True)
    def bolt(m):
        for i in range(5):m.cube(f'bolt_{i}',(7+i%2*2,i*4,7.5),(9+i%2*2,i*4+5,8.5),'gold')
    make('lightning',bolt,True)
    for kind in ('organs','fungus','wheat','vines'):
        def mutation(m,k=kind):
            if k=='fungus':plant(m,'sporebed','bloom')
            elif k=='vines':roots(m,4,12,'leaf')
            elif k=='wheat':
                for i in range(5):
                    x=3+i*2.5;m.cube(f'stem_{i}',(x,0,7),(x+.4,14,7.5),'gold')
                    for j in range(4):m.cube(f'grain_{i}_{j}',(x-.8,7+j*1.5,6.5),(x+1.2,8+j*1.5,8),'gold')
            else:
                for i in range(5):m.cube(f'growth_{i}',(3+i*2,2+i%2*4,5),(6+i*2,7+i%2*4,11),'flesh')
        make('mutation_'+kind,mutation)
    make('rebirth_seed',lambda m:(fruit(m),ring(m,'ward',7,1,'spirit')),True)
    make('death_canopy',lambda m:(roots(m,6,22,'soil'),ring(m,'canopy',9,22,'spirit')),True)
    make('earth_lance',lambda m:(m.cube('shaft',(6.5,0,6.5),(9.5,20,9.5),'bark'),
                               m.cube('point',(7.4,20,7.4),(8.6,29,8.6),'ivory'),roots(m,3,12)),False)
    return records
