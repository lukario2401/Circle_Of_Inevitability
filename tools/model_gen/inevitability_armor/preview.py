"""Dependency-free orthographic geometry preview of the actual rig and idle tentacle curves.

This is a model preview, not a Minecraft screenshot. It uses the rig's material colors.
"""
import json
import math
from generate import ASSETS, ROOT, COLORS, UV, png


def rotate(p, r):
    x,y,z=p; a,b,c=r
    y,z=y*math.cos(a)-z*math.sin(a),y*math.sin(a)+z*math.cos(a)
    x,z=x*math.cos(b)+z*math.sin(b),-x*math.sin(b)+z*math.cos(b)
    return (x*math.cos(c)-y*math.sin(c),x*math.sin(c)+y*math.cos(c),z)


def add(a,b): return tuple(x+y for x,y in zip(a,b))


def bezier(points,t):
    u=1-t
    return tuple(points[0][k]*u**3+3*points[1][k]*u*u*t+3*points[2][k]*u*t*t+points[3][k]*t**3 for k in range(3))


rig=json.loads((ASSETS/'armor/inevitability.rig.json').read_text())
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
for i in range(4):
    side=-1 if i%2==0 else 1; height=2 if i<2 else 8; wave=math.sin(i*1.7)
    a=(side*3.8,height,3.4);d=(side*(13+wave*2),height-10+wave*3,7+math.cos(i)*4)
    b=add(a,(side*12,-9-wave*2,9));c=add(d,(side*7,-8,6))
    for j in range(12):
        p=bezier([a,b,c,d],j/12);q=bezier([a,b,c,d],(j+1)/12)
        delta=tuple(q[k]-p[k] for k in range(3));length=math.sqrt(sum(v*v for v in delta));w=2.9-j*.205
        r=(math.acos(delta[1]/length),math.atan2(delta[0],delta[2]),0)
        for origin,size,mat in [((-w/2,0,-w/2),(w,length+.1,w),'flesh'),((-w*.3,length*.2,-w*.5-.3),(w*.6,.65,.5),'eye')]:
            cubes.append(([add(rotate(v,r),p) for v in vertices(origin,size)],COLORS[mat]))

WIDTH,HEIGHT=1000,700
pixels=bytearray([18,15,26,255]*(WIDTH*HEIGHT));depth=[float('inf')]*(WIDTH*HEIGHT)
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
            points.append((center+x*12,310+y*12,z))
        for face,shade in faces:
            col=tuple(min(255,int(v*shade)) for v in color)
            v=[points[i] for i in face];triangle(v[0],v[1],v[2],col);triangle(v[0],v[2],v[3],col)

output=ROOT/'docs/images/inevitability_armor_preview.png'
png(output,WIDTH,HEIGHT,pixels)
print(output)
