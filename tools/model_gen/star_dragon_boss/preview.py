"""Render the actual rig and keyframes to a turntable PNG/GIF without launching Minecraft.
The preview uses flat material colors; the game uses the supplied UV textures and emissive maps.
"""
from pathlib import Path
import math,json,random
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[3]
A=ROOT/'src/main/resources/assets/circle_of_inevitability/boss/star_dragon'
RIG=json.loads((A/'star_dragon.rig.json').read_text())
CLIPS=json.loads((A/'star_dragon.animations.json').read_text())['clips']
OUT=ROOT/'docs/assets/star_dragon_boss';OUT.mkdir(parents=True,exist_ok=True)
def eye():return [[int(i==j) for j in range(4)] for i in range(4)]
def mul(a,b):return [[sum(a[i][k]*b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]
def transform(p,r,s):
    a=eye()
    for i in range(3):a[i][3]=p[i]
    for axis in (2,1,0):
        m=eye();c=math.cos(math.radians(r[axis]));sn=math.sin(math.radians(r[axis]));i,j={0:(1,2),1:(2,0),2:(0,1)}[axis]
        m[i][i]=m[j][j]=c;m[i][j]=-sn;m[j][i]=sn;a=mul(a,m)
    m=eye()
    for i in range(3):m[i][i]=s[i]
    return mul(a,m)
def point(m,p):return tuple(sum(m[i][j]*p[j] for j in range(3))+m[i][3] for i in range(3))
def sample(frames,t):
    a=b=frames[0]
    for b in frames:
        if b[0]>=t:break
        a=b
    u=max(0,min(1,(t-a[0])/(b[0]-a[0]))) if b[0]>a[0] else 0;u=u*u*(3-2*u)
    return [a[i]+(b[i]-a[i])*u for i in (1,2,3)]
def render(im,box,radiant=False,t=0,clip='idle',yaw=-38):
    mats={};faces=[]
    colors={'scale':(40,54,82),'belly':(79,143,151),'trim':(217,189,121),'horn':(170,200,218),'eye':(147,243,255),'star':(199,207,255),'wing':(65,65,110)}
    if radiant:colors.update(scale=(216,230,240),belly=(131,192,197),trim=(251,215,130),horn=(255,242,200),wing=(156,186,210),star=(255,245,198),eye=(255,255,229))
    for b in RIG['bones']:
        p=b['pivot'][:];r=b['rotation'][:];s=[1,1,1]
        for name in (['idle',clip]+(['radiant'] if radiant else [])):
            if name=='idle' and clip=='idle' and name==clip:continue
            c=CLIPS[name];nt=(t/c['seconds'])%1 if c['loop'] else min(1,t/c['seconds'])
            for ch,frames in c['bones'].get(b['name'],{}).items():
                v=sample(frames,nt)
                if ch=='position':p=[p[i]+v[i] for i in range(3)]
                if ch=='rotation':r=[r[i]+v[i] for i in range(3)]
                if ch=='scale':s=[s[i]*v[i] for i in range(3)]
        mats[b['name']]=mul(mats.get(b['parent'],eye()),transform(p,r,s))
        if (b['name']=='halo' or b['name'].startswith('angel_')) and not radiant:continue
        for cube in b['cubes']:
            x,y,z=cube['origin'];w,h,d=cube['size'];vertices=[point(mats[b['name']],(x+dx*w,y+dy*h,z+dz*d)) for dx,dy,dz in [(0,0,0),(1,0,0),(1,1,0),(0,1,0),(0,0,1),(1,0,1),(1,1,1),(0,1,1)]]
            for ids,shade in [((0,1,2,3),.9),((4,7,6,5),.6),((0,4,5,1),1.2),((3,2,6,7),.5),((0,3,7,4),.75),((1,5,6,2),1)]:
                pts=[vertices[i] for i in ids]
                def project(v):
                    a=math.radians(yaw);xx=v[0]*math.cos(a)-v[2]*math.sin(a);zz=v[0]*math.sin(a)+v[2]*math.cos(a)
                    return (xx, v[1]*.92-zz*.38, zz*.92+v[1]*.38)
                pp=[project(v) for v in pts];col=tuple(min(255,int(v*shade)) for v in colors[cube['material']])
                faces.append((sum(v[2] for v in pp)/4,pp,col,cube['material']))
    # Fixed viewport keeps scale stable across frames.
    x0,y0,x1,y1=box;scale=min((x1-x0)/145,(y1-y0)/100);cx=(x0+x1)/2;cy=(y0+y1)/2+15
    draw=ImageDraw.Draw(im)
    draw.ellipse((cx-200,cy+95,cx+200,cy+140),fill=(9,16,31))
    for depth,points,col,material in sorted(faces,key=lambda f:-f[0]):
        coords=[(cx+p[0]*scale,cy+p[1]*scale) for p in points]
        draw.polygon(coords,fill=col,outline=tuple(max(0,int(v*.65)) for v in col))
        if material=='wing':
            rng=random.Random(int(depth*100))
            for k in range(7):
                u,v=rng.random(),rng.random();a,b,c,d=coords
                px=(a[0]*(1-u)+b[0]*u)*(1-v)+(d[0]*(1-u)+c[0]*u)*v
                py=(a[1]*(1-u)+b[1]*u)*(1-v)+(d[1]*(1-u)+c[1]*u)*v
                draw.ellipse((px-1,py-1,px+1,py+1),fill=(218,233,255))

def font(size):
    try:return ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',size)
    except OSError:return ImageFont.load_default()
im=Image.new('RGB',(1600,850),(16,23,39));draw=ImageDraw.Draw(im)
draw.text((60,30),'STAR DRAGON',font=font(38),fill=(234,242,254));draw.text((62,83),'SEQUENCE 2  /  RADIANT ANGEL',font=font(17),fill=(145,178,219))
render(im,(20,140,800,740),False,.3);render(im,(800,140,1580,740),True,.3,clip='radiant')
draw.text((280,760),'CELESTIAL DRAGON',font=font(20),fill=(171,194,231));draw.text((1050,760),'RADIANT ANGEL FORM',font=font(20),fill=(255,221,160))
draw.text((60,817),'Actual cuboid rig and keyframes • Flat material preview • In-game textures include scale detail and emissive stars',font=font(13),fill=(120,145,176))
im.save(OUT/'preview.png')
frames=[]
for i in range(64):
    im=Image.new('RGB',(800,600),(16,23,39));d=ImageDraw.Draw(im);radiant=i>=32;clip='idle' if i<16 else 'ray' if i<32 else 'transform' if i<48 else 'radiant';t=(i%16)/16*CLIPS[clip]['seconds']
    render(im,(0,50,800,530),radiant,t,clip,-38+10*math.sin(i/64*math.tau))
    d.text((30,20),'STAR DRAGON  /  '+clip.upper(),font=font(20),fill=(240,226,192) if radiant else (193,216,252))
    d.text((30,565),'Rig animation preview — native Minecraft rendering may differ',font=font(14),fill=(130,154,187))
    frames.append(im)
frames[0].save(OUT/'animations.gif',save_all=True,append_images=frames[1:],duration=80,loop=0,optimize=True)
print(OUT/'preview.png')
