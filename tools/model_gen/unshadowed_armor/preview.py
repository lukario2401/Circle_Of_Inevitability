"""Dependency-free orthographic geometry preview of the Unshadowed rig and idle Spears of Light.

This is a model preview, not a Minecraft screenshot. It uses the rig's flat material colors.
"""
import json
import math
from generate import ASSETS, ROOT, COLORS, UV, SET, png


def rotate(p, r):
    x,y,z=p; a,b,c=r
    y,z=y*math.cos(a)-z*math.sin(a),y*math.sin(a)+z*math.cos(a)
    x,z=x*math.cos(b)+z*math.sin(b),-x*math.sin(b)+z*math.cos(b)
    return (x*math.cos(c)-y*math.sin(c),x*math.sin(c)+y*math.cos(c),z)


def add(a,b): return tuple(x+y for x,y in zip(a,b))


rig=json.loads((ASSETS/f'armor/{SET}.rig.json').read_text())
bones={b['name']:b for b in rig['bones']}
def transform(p,b):
    p=add(rotate(p,b['rotation']),b['pivot'])
    return transform(p,bones[b['parent']]) if b['parent']!='root' else p


def vertices(p,s):
    return [tuple(p[k]+s[k]*v[k] for k in range(3)) for v in
            [(0,0,0),(1,0,0),(1,1,0),(0,1,0),(0,0,1),(1,0,1),(1,1,1),(0,1,1)]]


cubes=[]
for b in rig['bones']:
    for c in b['cubes']:
        mat=next(k for k,uv in UV.items() if uv==c['uv'])
        cubes.append(([transform(v,b) for v in vertices(c['origin'],c['size'])],COLORS[mat]))
# Idle Spears of Light, mirroring UnshadowedArmorModel.animateSpears at ageInTicks 0.
SPEAR=[((-.75,-.5,-.75),(1.5,1,1.5),'light'),((-.5,0,-.5),(1,12.5,1),'gold'),((-1.75,11.5,-.5),(3.5,1,1),'gold'),
       ((-1,12.5,-.6),(2,2.5,1.2),'light'),((-.5,15,-.4),(1,1,.8),'light')]
for i in range(3):
    angle=i*math.pi*2/3+.6; s,c=math.sin(angle),math.cos(angle)
    centre=(s*12,9+math.sin(i*2.1)*1.5,c*12)
    d=(s*.25,-1,c*.25);n=math.sqrt(sum(v*v for v in d));d=tuple(v/n for v in d)
    base=tuple(centre[k]-d[k]*8 for k in range(3))
    r=(math.acos(d[1]),math.atan2(d[0],d[2]),0)
    for origin,size,mat in SPEAR:
        cubes.append(([add(rotate(v,r),base) for v in vertices(origin,size)],COLORS[mat]))

WIDTH,HEIGHT=1000,700
pixels=bytearray([24,22,30,255]*(WIDTH*HEIGHT));depth=[float('inf')]*(WIDTH*HEIGHT)
faces=[([0,3,2,1],.98),([4,5,6,7],.7),([0,4,7,3],.76),([1,2,6,5],.86),([0,1,5,4],1.22),([3,7,6,2],.58)]


def triangle(a,b,c,color):
    x0=max(0,int(min(a[0],b[0],c[0])));x1=min(WIDTH-1,math.ceil(max(a[0],b[0],c[0])))
    y0=max(0,int(min(a[1],b[1],c[1])));y1=min(HEIGHT-1,math.ceil(max(a[1],b[1],c[1])))
    den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
    if abs(den)<.0001:return
    rgba=bytes([*color,255])
    for y in range(y0,y1+1):
        for x in range(x0,x1+1):
            u=((b[1]-c[1])*(x-c[0])+(c[0]-b[0])*(y-c[1]))/den
            v=((c[1]-a[1])*(x-c[0])+(a[0]-c[0])*(y-c[1]))/den
            if u<0 or v<0 or u+v>1:continue
            z=u*a[2]+v*b[2]+(1-u-v)*c[2];i=y*WIDTH+x
            if z<depth[i]:depth[i]=z;pixels[i*4:i*4+4]=rgba


for center,yaw in [(250,.4),(750,math.pi+.4)]:
    for verts,color in cubes:
        points=[]
        for p in verts:
            x,y,z=rotate(p,(0,yaw,0));y,z=y*math.cos(.15)+z*math.sin(.15),-y*math.sin(.15)+z*math.cos(.15)
            points.append((center+x*12,330+y*12,z))
        for face,shade in faces:
            col=tuple(min(255,int(v*shade)) for v in color)
            v=[points[i] for i in face];triangle(v[0],v[1],v[2],col);triangle(v[0],v[2],v[3],col)

output=ROOT/f'docs/images/{SET}_armor_preview.png'
png(output,WIDTH,HEIGHT,pixels)
print(output)
