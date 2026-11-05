"""Original jade dragon and mind/dream/script props in Java Block/Item cube format."""
import math
import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from modelkit import Model, Texture
from icons import JADE, GOLD, PALE, IVORY, VIOLET, DARK, INK


def material(animated=False):
    tex=Texture('author/'+('dream_atlas' if animated else 'mind_atlas'),64,64,
                frames=4 if animated else 1,frametime=4)
    for i,(name,c) in enumerate(zip(('jade','gold','ivory','violet','dark','ink','light','scale'),
                                   (JADE,GOLD,IVORY,VIOLET,DARK,INK,PALE,JADE))):
        def paint(tx,ty,x,y,frame,c=c,n=name):
            mark=(y%6==0 or (x+(y//6)*3)%8==0) if n=='scale' else (x+y*2)%13==0
            shade=12*math.sin(tx*math.pi)-18*ty+(16 if mark else 0)
            if animated:shade+=23*math.sin(frame*math.pi/2+tx*5+ty*3)
            return tuple(max(0,min(255,int(v+shade))) for v in c[:3])+(255,)
        u=i%4*4;v=i//4*8;tex.zone(name,u,v,u+4,v+8,paint)
    return tex


def ring(m,name='ring',radius=7,y=8,zone='gold'):
    for i in range(16):
        a=i*math.tau/16;x=8+radius*math.cos(a);z=8+radius*math.sin(a)
        length=radius*math.tan(math.pi/16)+.12
        degrees=i*22.5;q=math.floor((degrees+45)/90);angle=-degrees+q*90
        dx,dz=(length,.4) if q%2 else (.4,length)
        m.cube(f'{name}_{i}',(x-dx,y-.3,z-dz),(x+dx,y+.3,z+dz),zone,rot=(angle,'y',(x,y,z)))


def eye(m,terror=False):
    for i in range(9):
        x=1+i*1.5;h=1+3.4*math.sin(i*math.pi/8)
        m.cube(f'eye_{i}',(x,8-h,7),(x+1.5,8+h,9),'violet' if terror else 'ivory')
    m.cube('iris',(5.5,5.5,6.5),(10.5,10.5,7),'jade')
    m.cube('pupil',(7.4,5.8,6.1),(8.6,10.2,6.6),'ink')
    m.cube('glint',(9,9,6),(9.8,10,6.4),'light')
    if terror:
        for i in range(3):m.cube(f'tear_{i}',(4+i*3,0,7),(4.8+i*3,4,8),'gold')


def mask(m,fractured=False):
    for i,(x,y,w,h) in enumerate(((3,8,10,8),(4,4,8,4),(6,1,4,3))):
        m.cube(f'mask_{i}',(x,y,7),(x+w,y+h,9),'ivory')
    for x in (4,10):m.cube('eye_'+str(x),(x,10,6.6),(x+2,11.4,7.1),'ink')
    m.cube('lips',(6.5,4,6.7),(9.5,4.6,7.1),'violet')
    if fractured:
        for i in range(5):m.cube(f'crack_{i}',(7+i%2,2+i*2.7,6.5),(8+i%2,5+i*2.7,7),'ink')


def dragon(m,frame=0,complete=True,small=False):
    # All poses retain names/order/faces for ModelFx smooth() interpolation.
    m.cube('body',(4,4,4),(12,13,15),'scale')
    m.cube('breast',(5,5,3.5),(11,12,5),'gold')
    m.cube('neck',(5.5,11,2),(10.5,17,7),'jade')
    m.cube('head',(4.5,15,-2),(11.5,20,5),'scale')
    m.cube('muzzle',(5.5,14,-5),(10.5,17,0),'jade')
    for side in (-1,1):
        x=8+side*3.5
        m.cube(f'eye_{side}',(x-.3,17,-1.5),(x+.3,18.2,.5),'light')
        m.cube(f'horn_{side}',(x-.5,19,2),(x+.5,24,3.5),'ivory',rot=(side*22.5,'z',(x,19,2)))
        m.cube(f'leg_{side}',(x-1,0,10),(x+1,5,14),'jade')
        for j in range(3):m.cube(f'claw_{side}_{j}',(x-1+j*.7,-.3,8),(x-.6+j*.7,.6,11),'ivory')
        # Membranes are broad stepped panels, supported by thin golden fingers.
        span=.68 if not complete else (0.75 if small else 1)
        lift=(0,3,0,-3)[frame]
        for i in range(6):
            x=8+side*(5+i*2.7)*span;y=11+i*.6+lift*(i+1)/6
            m.cube(f'wing_membrane_{side}_{i}',(x-1.45,y,6+i*.7),(x+1.45,y+.45,15+i*.1),'violet')
            m.cube(f'wing_finger_{side}_{i}',(x-.25,y+.4,5+i*.7),(x+.25,y+1,17-i*.25),'gold')
    for i in range(5):
        m.cube(f'tail_{i}',(6+i*.4,5-i*.6,14+i*2.8),(10-i*.4,8-i*.6,17+i*2.8),'scale')
    for i in range(4):m.cube(f'spine_{i}',(7.5,13,5+i*2),(8.5,15,6+i*2),'gold')
    if complete:ring(m,'mind_halo',5,25,'gold')


def generate(root):
    solid,animated=material(),material(True);solid.save(root);animated.save(root)
    records=[]
    def make(name,fn,texture=solid,pivot=(8,8,8)):
        m=Model('author/'+name,texture,'Author / original Spectator pathway geometry.');fn(m);m.save(root)
        records.append({'name':name,'elements':len(m.elements),'pivot_pixels':list(pivot),
                        'bounds':[[min(e['from'][i] for e in m.elements) for i in range(3)],
                                  [max(e['to'][i] for e in m.elements) for i in range(3)]]})
    make('mind_eye',eye);make('terror_eye',lambda m:eye(m,True),animated)
    make('observer_mask',mask);make('fractured_mask',lambda m:mask(m,True))
    make('mind_ripple',lambda m:[ring(m,'ripple_'+str(i),4+i*3,8,'jade' if i%2 else 'gold') for i in range(3)],animated)
    def lotus(m):
        for i in range(8):
            a=i*math.tau/8;x=8+4*math.cos(a);z=8+4*math.sin(a)
            m.cube(f'petal_{i}',(x-1,6,z-1),(x+1,10,z+1),'ivory' if i%2 else 'jade')
        m.cube('heart',(6,6,6),(10,11,10),'gold');ring(m,'water',7,5,'violet')
    make('lotus',lotus)
    def pendulum(m):
        m.cube('chain',(7.8,9,7.8),(8.2,24,8.2),'gold')
        m.cube('gem',(5,4,6),(11,10,10),'jade',rot=(45,'z',(8,7,8)))
        ring(m,'swing',7,3,'violet')
    make('pendulum',pendulum)
    def lantern(m):
        for y in (1,13):m.cube(f'cap_{y}',(3,y,3),(13,y+1,13),'gold')
        for x in (3,12):
            for z in (3,12):m.cube(f'post_{x}_{z}',(x,2,z),(x+1,13,z+1),'jade')
        m.cube('dream_light',(6,4,6),(10,11,10),'violet');ring(m,'handle',3,17)
    make('dream_lantern',lantern,animated)
    def gate(m,sea=False):
        for x in (0,14):
            m.cube('pillar_'+str(x),(x,0,6),(x+2,20,10),'jade')
            m.cube('foot_'+str(x),(x-1,0,5),(x+3,2,11),'gold')
        for i in range(7):
            x=2+i*1.7;y=20+3*math.sin(i*math.pi/6)
            m.cube('arch_'+str(i),(x,y,6),(x+2,y+2,10),'gold')
        if sea:
            for i in range(5):ring(m,'sea_'+str(i),5+i*2,1-i*.5,'violet')
    make('dream_gate',gate,animated,pivot=(8,0,8));make('sea_arch',lambda m:gate(m,True),animated,pivot=(8,0,8))
    def cage(m):
        ring(m,'base',7,1);ring(m,'top',7,15)
        for i in range(8):
            a=i*math.tau/8;x=8+7*math.cos(a);z=8+7*math.sin(a)
            m.cube(f'bar_{i}',(x-.25,1,z-.25),(x+.25,15,z+.25),'violet')
    make('thought_cage',cage,animated)
    def shard(m):
        m.cube('shard',(6,2,6),(10,14,10),'violet',rot=(22.5,'z',(8,8,8)))
        m.cube('heart',(7,6,5.7),(9,10,6.2),'light')
    make('dream_shard',shard,animated);make('thought_shard',shard)
    def crown(m):
        ring(m,'rim',6,7)
        for i in range(8):
            a=i*math.tau/8;x=8+6*math.cos(a);z=8+6*math.sin(a)
            m.cube(f'tine_{i}',(x-.5,7,z-.5),(x+.5,12+(i%2)*2,z+.5),'jade')
    make('mind_crown',crown)
    def thread(m):
        for i in range(12):
            x=8+math.sin(i*.6)*1.5
            m.cube(f'thread_{i}',(x-.2,-4+i*2,7.8),(x+.2,-2+i*2,8.2),'gold')
    make('thought_thread',thread,animated)
    def persona(m):
        mask(m);ring(m,'orbit',9,9,'violet')
        for i in range(3):m.cube(f'thought_{i}',(5+i*2,-2-i,7),(6+i*2,1-i,8),'jade')
    make('persona',persona,animated)
    def book(m):
        for side in (-1,1):
            x=8+side*4
            m.cube('cover_'+str(side),(x-4,3,2),(x+4,4,14),'jade',rot=(side*22.5,'z',(8,4,8)))
            m.cube('pages_'+str(side),(x-3.7,4,2.5),(x+3.7,5.5,13.5),'ivory',rot=(side*22.5,'z',(8,4,8)))
        m.cube('spine',(7.5,2.8,2),(8.5,4.2,14),'gold')
        for i in range(5):m.cube(f'floating_glyph_{i}',(4+i*2,10+i%2,7),(4.6+i*2,11+i%2,8),'violet')
    make('open_book',book)
    def quill(m):
        m.cube('shaft',(7.7,0,7.7),(8.3,22,8.3),'gold',rot=(-22.5,'z',(8,0,8)))
        for i in range(7):m.cube(f'barb_{i}',(6-i*.4,8+i*2,7.5),(10+i*.2,9+i*2,8.5),'jade',rot=(-22.5,'z',(8,0,8)))
    make('quill',quill,pivot=(8,0,8))
    def seal(m):
        ring(m,'seal',7,8,'gold');ring(m,'seal_inner',4.5,8,'jade')
        m.cube('stroke',(7.5,8,-.5),(8.5,8.5,16.5),'ivory')
    make('script_seal',seal)
    def blade(m):
        m.cube('grip',(7.2,0,7.2),(8.8,6,8.8),'ivory')
        m.cube('guard',(3,6,7),(13,7.5,9),'gold')
        m.cube('blade',(6.5,7,7.5),(9.5,25,8.5),'jade')
        m.cube('edge',(6,8,7.5),(6.6,24,8.5),'light')
        m.cube('tip',(7,25,7.5),(9,28,8.5),'gold')
    make('envisioned_blade',blade,pivot=(8,0,8))
    for base,complete,small in (('mind_dragon',True,False),('mind_dragon_incomplete',False,False),('imagined_drake',True,True)):
        make(base,lambda m,c=complete,s=small:dragon(m,0,c,s),pivot=(8,0,8))
        for f in range(4):make(f'{base}_{f}',lambda m,f=f,c=complete,s=small:dragon(m,f,c,s),pivot=(8,0,8))
    return records
