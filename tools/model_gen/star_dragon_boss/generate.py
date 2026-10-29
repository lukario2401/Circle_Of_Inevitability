"""Rebuild the native rig, Blockbench-importable Bedrock geometry, keyframes and UV-mapped atlases.
Run from any directory: python tools/model_gen/star_dragon_boss/generate.py (Pillow required).
No external model, texture, or animation dependencies. Coordinates use Java model pixels (16 = 1 block).
"""
from pathlib import Path
import json, math, random
from PIL import Image, ImageDraw
ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'src/main/resources/assets/circle_of_inevitability'
OUT = ASSETS / 'boss/star_dragon'
OUT.mkdir(parents=True, exist_ok=True)
BONES=[]
def bone(name, parent, pivot, cubes=(), rotation=(0,0,0)):
    B={'name':name,'parent':parent,'pivot':list(pivot),'rotation':list(rotation),'cubes':[]}
    for origin,size,material in cubes:
        B['cubes'].append({'origin':list(origin),'size':list(size),'material':material})
    BONES.append(B)
def cube(x,y,z,w,h,d,m='scale'):return ((x,y,z),(w,h,d),m)
bone('body',None,(0,8,3),[cube(-9,-8,-16,18,16,30),cube(-7,-4,-18,14,13,27,'belly'),cube(-10,-6,-12,2,10,22,'trim'),cube(8,-6,-12,2,10,22,'trim')])
bone('chest','body',(0,-2,-13),[cube(-7,-7,-8,14,15,12,'scale'),cube(-4,-5,-9,8,11,1,'star')],(-12,0,0))
bone('neck','chest',(0,-4,-5),[cube(-5,-6,-14,10,11,17),cube(-4,3,-13,8,3,13,'belly')],(-16,0,0))
bone('head','neck',(0,-2,-12),[cube(-7,-6,-11,14,11,14),cube(-5,-1,-22,10,5,12),cube(-5,0,-23,10,1,12,'trim'),cube(-7.5,-4,-12,1,3,7,'eye'),cube(6.5,-4,-12,1,3,7,'eye'),cube(-8,-7,-5,16,3,6,'trim')])
bone('jaw','head',(0,3,-5),[cube(-5,0,-17,10,3,18,'belly'),cube(-5,-1,-17,2,2,3,'horn'),cube(3,-1,-17,2,2,3,'horn')])
for sign,label in [(-1,'left'),(1,'right')]:
    bone(label+'_horn','head',(sign*5,-5,-1),[cube(-1,-13,-1,2,14,3,'horn'),cube(-1,-17,1,2,6,3,'trim')],(-28,0,sign*20))
    bone(label+'_cheek','head',(sign*6,0,0),[cube(-1,-2,0,2,4,12,'horn')],(-10,sign*25,0))
    bone(label+'_wing','body',(sign*8,-6,-7),[cube(-1,-2,-3,2,4,28,'horn'),cube(min(0,sign*27),-1,-2,27,2,26,'wing')],(0,sign*12,sign*-12))
    bone(label+'_wing_tip',label+'_wing',(sign*26,0,0),[cube(min(0,sign*24),-1,-2,24,2,36,'wing'),cube(min(0,sign*25),-2,-3,25,3,3,'trim'),cube(min(0,sign*22),-1,12,22,2,2,'trim')],(0,sign*-18,sign*8))
    for i in range(3):
        bone(label+'_wing_finger'+str(i),label+'_wing_tip',(sign*(7+i*7),0,0),[cube(-.5,-1,-2,1,2,32-i*7,'horn')],(0,sign*(-6+i*7),0))
    for fore,z in [(True,-11),(False,12)]:
        limb=label+('_foreleg' if fore else '_hindleg')
        bone(limb,'body',(sign*8,3,z),[cube(-3,-2,-4,6,11,8),cube(-3,0,-4,6,3,2,'trim')],(-12 if fore else 18,0,sign*-8))
        bone(limb+'_foot',limb,(0,8,0),[cube(-3,0,-6,6,6,9,'belly')])
        for toe in range(3):
            BONES[-1]['cubes'].append({'origin':[-2.5+toe*2,3,-10],'size':[1,2,5],'material':'horn'})
for i,(width,length) in enumerate([(10,19),(7,18),(4,18),(2,14)]):
    bone('tail'+str(i),'body' if i==0 else 'tail'+str(i-1),(0,-1,14 if i==0 else [19,18,18][i-1]),[cube(-width/2,-width/2,0,width,width,length),cube(-1,-width/2-4,3,2,4,5,'trim')],(-7,0,0))
for i in range(5):
    bone('spine'+str(i),'body',(0,-8,-12+i*6),[cube(-1,-5,-2,2,6,4,'star')],(-20,0,0))
# Halo has real geometry and a separate articulated bone; it expands in the angel form.
halo=[]
for i in range(12):
    a=i*math.tau/12
    halo.append(cube(math.cos(a)*12-1,-1,math.sin(a)*12-1,2,2,2,'star'))
bone('halo','body',(0,-20,-18),halo,(22,0,0))
# Five additional wing pairs unfurl only in the Radiant Angel form (six pairs in total).
for pair in range(5):
    for sign,label in [(-1,'left'),(1,'right')]:
        wing='angel_'+label+'_wing'+str(pair)
        bone(wing,'body',(sign*6,-9-pair*2,-5+pair*3),
             [cube(min(0,sign*27),-1,0,27,2,9,'wing'),cube(min(0,sign*29),-2,0,29,2,2,'trim')],
             (-15+pair*12,sign*(-35+pair*16),sign*(-35-pair*9)))
        bone(wing+'_tip',wing,(sign*25,0,0),
             [cube(min(0,sign*16),-1,0,16,2,6,'wing'),cube(min(0,sign*18),-2,0,18,2,2,'horn')],(0,sign*15,sign*-12))
# Assign a unique box unwrap to each cube, including fractional geometry with ceil texel bounds.
X=Y=0;ROW=0;SIZE=512
for b in BONES:
    for c in b['cubes']:
        w,h,d=map(lambda n:max(1,math.ceil(n)),c['size']);tw,th=2*(w+d),h+d
        if X+tw+2>SIZE:X=0;Y+=ROW+2;ROW=0
        c['uv']=[X,Y];X+=tw+2;ROW=max(ROW,th)
assert Y+ROW<SIZE
rig={'format_version':1,'texture_size':[SIZE,SIZE],'bones':BONES}
(OUT/'star_dragon.rig.json').write_text(json.dumps(rig,indent=2)+'\n')
palettes={
 'normal':{'scale':('#13192e','#394b72'),'belly':('#235568','#6fafa9'),'trim':('#a2884f','#f2d99a'),'horn':('#667b94','#d2e8f2'),'eye':('#4dd6ff','#f1ffff'),'star':('#91a6ff','#f3eeff'),'wing':('#1a1737','#514f8c')},
 'radiant':{'scale':('#aabed0','#eef6ff'),'belly':('#4b8e9b','#b9e5dc'),'trim':('#b67c25','#fff0a2'),'horn':('#c99b4b','#fff9da'),'eye':('#f4cb6a','#ffffff'),'star':('#f8d28b','#ffffff'),'wing':('#6b87aa','#e4f2fc')}}
for form,palette in palettes.items():
    im=Image.new('RGBA',(SIZE,SIZE),(0,0,0,0));em=im.copy();rng=random.Random(9821)
    for b in BONES:
        for c in b['cubes']:
            u,v=c['uv'];w,h,d=map(lambda n:max(1,math.ceil(n)),c['size']);tw,th=2*(w+d),h+d
            lo,hi=[tuple(bytes.fromhex(s[1:])) for s in palette[c['material']]]
            for py in range(th):
                for px in range(tw):
                    t=.2+rng.random()*.18 + .25*((px//3+(py//3)%2)%2)+.2*(1-py/max(1,th))
                    col=tuple(round(lo[i]+(hi[i]-lo[i])*t) for i in range(3))+(255,)
                    if py%6==0 and px%3!=0:col=tuple(round(n*.65) for n in col[:3])+(255,)
                    if c['material']=='wing' and rng.random()<.035:col=(201,218,255,255)
                    im.putpixel((u+px,v+py),col)
                    if c['material'] in ('eye','star') or (c['material']=='wing' and col[0]==201):em.putpixel((u+px,v+py),col)
    tex=ASSETS/'textures/entity/star_dragon_boss';tex.mkdir(parents=True,exist_ok=True)
    im.save(tex/(form+'.png'));em.save(tex/(form+'_emissive.png'))
# Blockbench: File > Import > Bedrock Model, then select this .geo.json and normal.png.
global_pivots={};bb=[]
for b in BONES:
    parent=global_pivots.get(b['parent'],[0,0,0]);p=[parent[i]+b['pivot'][i] for i in range(3)];global_pivots[b['name']]=p
    item={'name':b['name'],'pivot':[p[0],24-p[1],p[2]],'rotation':[-b['rotation'][0],b['rotation'][1],-b['rotation'][2]],'cubes':[]}
    if b['parent']:item['parent']=b['parent']
    for c in b['cubes']:
        x,y,z=c['origin'];w,h,d=c['size'];item['cubes'].append({'origin':[p[0]+x,24-(p[1]+y+h),p[2]+z],'size':[w,h,d],'uv':c['uv']})
    bb.append(item)
(OUT/'star_dragon.geo.json').write_text(json.dumps({'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.coi.star_dragon_boss','texture_width':SIZE,'texture_height':SIZE,'visible_bounds_width':16,'visible_bounds_height':10,'visible_bounds_offset':[0,2,0]},'bones':bb}]},indent=2)+'\n')
# Keyframes: normalized time in [0,1], additive XYZ rotations (degrees), position (model pixels), scale.
# Native animation engine consumes this exact file; edit in a resource pack and press F3+T.
clips={}
def clip(name,seconds,loop,channels):clips[name]={'seconds':seconds,'loop':loop,'bones':channels}
def rot(values):return {'rotation':[[t,*v] for t,v in values]}
def wave(bone,xyz):return rot([(0,[0,0,0]),(.25,xyz),(.5,[0,0,0]),(.75,[-v for v in xyz]),(1,[0,0,0])])
idle={'body':{'position':[[0,0,0,0],[.5,0,-.8,0],[1,0,0,0]]},'head':wave('head',[2,0,0]),'tail0':wave('',[0,6,0]),'tail1':wave('',[0,9,0]),'tail2':wave('',[0,12,0]),'tail3':wave('',[0,16,0])}
for label,sign in [('left',-1),('right',1)]:idle[label+'_wing']=wave('',[0,0,sign*4]);idle[label+'_wing_tip']=wave('',[0,0,sign*6])
clip('idle',3,True,idle)
walk={}
for label,sign in [('left',-1),('right',1)]:
    walk[label+'_foreleg']=wave('',[sign*24,0,0]);walk[label+'_hindleg']=wave('',[-sign*24,0,0]);walk[label+'_wing']=wave('',[0,0,sign*7])
clip('walk',1,True,walk)
clip('cast',1,False,{'body':rot([(0,[0,0,0]),(.6,[-8,0,0]),(.85,[8,0,0]),(1,[0,0,0])]),'head':rot([(0,[0,0,0]),(.6,[-16,0,0]),(.85,[12,0,0]),(1,[0,0,0])]),'jaw':rot([(0,[0,0,0]),(.5,[28,0,0]),(.85,[38,0,0]),(1,[0,0,0])])})
clip('ray',1,False,{'head':rot([(0,[0,0,0]),(.3,[-12,0,0]),(.8,[4,0,0]),(1,[0,0,0])]),'jaw':rot([(0,[0,0,0]),(.3,[32,0,0]),(.8,[38,0,0]),(1,[0,0,0])]),'neck':rot([(0,[0,0,0]),(.4,[-10,0,0]),(.85,[8,0,0]),(1,[0,0,0])])})
clip('surge',1,False,{'body':rot([(0,[0,0,0]),(.5,[-18,0,0]),(.8,[18,0,0]),(1,[0,0,0])]),'left_wing':rot([(0,[0,0,0]),(.5,[0,0,-35]),(.8,[0,0,25]),(1,[0,0,0])]),'right_wing':rot([(0,[0,0,0]),(.5,[0,0,35]),(.8,[0,0,-25]),(1,[0,0,0])])})
clip('ward',1,False,{'left_wing':rot([(0,[0,0,0]),(.5,[0,-50,-30]),(1,[0,0,0])]),'right_wing':rot([(0,[0,0,0]),(.5,[0,50,30]),(1,[0,0,0])]),'head':rot([(0,[0,0,0]),(.5,[20,0,0]),(1,[0,0,0])])})
clip('transform',1.5,False,{'body':{'position':[[0,0,0,0],[.5,0,-5,0],[1,0,0,0]]},'left_wing':rot([(0,[0,0,0]),(.5,[0,0,-50]),(1,[0,0,0])]),'right_wing':rot([(0,[0,0,0]),(.5,[0,0,50]),(1,[0,0,0])]),'halo':{'scale':[[0,1,1,1],[.5,1.8,1.8,1.8],[1,1,1,1]]}})
clip('bite',.7,False,{'neck':rot([(0,[0,0,0]),(.3,[-18,0,0]),(.6,[18,0,0]),(1,[0,0,0])]),'jaw':rot([(0,[0,0,0]),(.3,[35,0,0]),(.6,[0,0,0]),(1,[0,0,0])])})
clip('hurt',.3,False,{'body':rot([(0,[0,0,0]),(.4,[-6,0,8]),(1,[0,0,0])])})
clip('death',1,False,{'body':{'rotation':[[0,0,0,0],[1,0,0,70]],'position':[[0,0,0,0],[1,0,6,0]]},'left_wing':rot([(0,[0,0,0]),(1,[0,-50,20])]),'right_wing':rot([(0,[0,0,0]),(1,[0,50,-20])])})
clip('radiant',2,True,{'halo':{'rotation':[[0,0,0,0],[1,0,360,0]],'scale':[[0,1.3,1.3,1.3],[.5,1.5,1.5,1.5],[1,1.3,1.3,1.3]]},'left_wing':wave('',[0,0,-12]),'right_wing':wave('',[0,0,12])})
for pair in range(5):
    for sign,label in [(-1,'left'),(1,'right')]:
        bn='angel_'+label+'_wing'+str(pair)
        clips['radiant']['bones'][bn]=wave('',[0,0,sign*(6+pair*2)])
        clips['transform']['bones'][bn]=rot([(0,[0,0,sign*65]),(.6,[0,0,sign*-15]),(1,[0,0,0])])
(OUT/'star_dragon.animations.json').write_text(json.dumps({'format_version':1,'clips':clips},indent=2)+'\n')
# Also export the same animation channels in Bedrock's editable animation format.
exports={}
for name,c in clips.items():
    tracks={}
    for bn,channels in c['bones'].items():
        tracks[bn]={}
        for ch,frames in channels.items():
            keys={}
            for t,x,y,z in frames:
                if ch=='rotation':x,z=-x,-z
                if ch=='position':y=-y
                keys[str(round(t*c['seconds'],5))]=[x,y,z]
            tracks[bn][ch]=keys
    exports['animation.coi.star_dragon.'+name]={'loop':c['loop'],'animation_length':c['seconds'],'bones':tracks}
(OUT/'star_dragon.animation.json').write_text(json.dumps({'format_version':'1.8.0','animations':exports},indent=2)+'\n')
print(f'Generated {len(BONES)} bones, {sum(len(b["cubes"]) for b in BONES)} cubes, {len(clips)} clips; UV height {Y+ROW}/{SIZE}.')

# Spacious empty structure isolates ranged-boss server tests from adjacent arenas.
import gzip, struct
def nbt_string(value):
    data=value.encode(); return struct.pack('>H',len(data))+data
def nbt_tag(kind,name,payload):
    return bytes([kind])+nbt_string(name)+payload
size=nbt_tag(9,'size',bytes([3])+struct.pack('>i3i',3,32,20,32))
palette=nbt_tag(9,'palette',bytes([10])+struct.pack('>i',1)+nbt_tag(8,'Name',nbt_string('minecraft:air'))+b'\0')
empty=b''.join(nbt_tag(9,name,b'\x0a'+struct.pack('>i',0)) for name in ('entities','blocks'))
raw=b'\x0a\x00\x00'+size+palette+empty+nbt_tag(3,'DataVersion',struct.pack('>i',5000))+b'\0'
test_structure=ROOT/'src/main/resources/data/circle_of_inevitability/structure/star_dragon_test_arena.nbt'
test_structure.parent.mkdir(parents=True,exist_ok=True)
test_structure.write_bytes(gzip.compress(raw,mtime=0))
