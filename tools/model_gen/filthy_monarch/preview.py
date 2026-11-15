"""Filthy Monarch contact sheets; software UV renderer adapted from this repo's White Angel preview."""
import json
import math
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFont
from catalog import ASSETS, PREVIEW, ABILITIES, WHEELS, BASIC, KEYS, STATUSES, NAMES, PARTICLES
from icons import BLUE, PALE, RED
from catalog import ANIMATED
GOLD=RED;IVORY=PALE
BG=(23,19,26)

def font(size):
    for path in ('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf','C:/Windows/Fonts/consola.ttf'):
        if Path(path).is_file():return ImageFont.truetype(path,size)
    return ImageFont.load_default(size=size)


def rotated(p,rotation):
    if not rotation:return p
    x,y,z=[p[i]-rotation['origin'][i] for i in range(3)]
    a=math.radians(rotation['angle']);c,s=math.cos(a),math.sin(a)
    if rotation['axis']=='y':x,z=x*c+z*s,-x*s+z*c
    elif rotation['axis']=='x':y,z=y*c-z*s,y*s+z*c
    else:x,y=x*c-y*s,x*s+y*c
    return [v+rotation['origin'][i] for i,v in enumerate((x,y,z))]


def render(name,size=(300,270),yaw=-25,frame=0):
    # Same cube winding and affine UV projection as the existing pathway preview scripts.
    model=json.loads((ASSETS/f'models/model_fx/filthy_monarch/{name}.json').read_text())
    textures={}
    for key,ref in model['textures'].items():
        with Image.open(ASSETS/f'textures/{ref.split(":")[1]}.png') as source:im=source.convert('RGBA')
        if im.height>im.width:im=im.crop((0,(frame%4)*im.width,im.width,(frame%4+1)*im.width))
        textures[key]=im
    a=math.radians(yaw);c,s=math.cos(a),math.sin(a)
    def project(p):
        x,y,z=p;x-=8;z-=8;xx=x*c+z*s;zz=-x*s+z*c
        return (xx,-y*.92-zz*.39,zz*.92-y*.39)
    definitions={
        'north':[(1,1,0),(1,0,0),(0,0,0),(0,1,0)],'south':[(0,1,1),(0,0,1),(1,0,1),(1,1,1)],
        'west':[(0,1,0),(0,0,0),(0,0,1),(0,1,1)],'east':[(1,1,1),(1,0,1),(1,0,0),(1,1,0)],
        'up':[(0,1,0),(0,1,1),(1,1,1),(1,1,0)],'down':[(0,0,1),(0,0,0),(1,0,0),(1,0,1)]}
    faces=[];allpts=[]
    for el in model['elements']:
        for side,face in el['faces'].items():
            pts=[project(rotated([el['from'][i]+corner[i]*(el['to'][i]-el['from'][i]) for i in range(3)],
                                 el.get('rotation'))) for corner in definitions[side]]
            cross=(pts[1][0]-pts[0][0])*(pts[2][1]-pts[0][1])-(pts[1][1]-pts[0][1])*(pts[2][0]-pts[0][0])
            if cross<=1e-6:continue
            faces.append((sum(p[2] for p in pts)/4,pts,face));allpts.extend(pts)
    canvas=Image.new('RGBA',size,BG+(255,))
    if not allpts:return canvas
    xmin,xmax=min(p[0] for p in allpts),max(p[0] for p in allpts)
    ymin,ymax=min(p[1] for p in allpts),max(p[1] for p in allpts)
    scale=min((size[0]-50)/max(1,xmax-xmin),(size[1]-45)/max(1,ymax-ymin))
    def screen(p):return ((p[0]-(xmin+xmax)/2)*scale+size[0]/2,(p[1]-(ymin+ymax)/2)*scale+size[1]/2)
    for _,pts,face in sorted(faces,key=lambda f:-f[0]):
        pp=[screen(p) for p in pts];tex=textures[face['texture'].lstrip('#')];u0,v0,u1,v1=face['uv']
        p0,p1,p2,p3=pp;ax,ay=p3[0]-p0[0],p3[1]-p0[1];bx,by=p1[0]-p0[0],p1[1]-p0[1]
        det=ax*by-ay*bx
        if abs(det)<1e-5:continue
        ux,uy=by/det,-bx/det;vx,vy=-ay/det,ax/det
        du,dv=(u1-u0)*tex.width/16,(v1-v0)*tex.height/16
        affine=(ux*du,uy*du,u0*tex.width/16-(ux*p0[0]+uy*p0[1])*du,
                vx*dv,vy*dv,v0*tex.height/16-(vx*p0[0]+vy*p0[1])*dv)
        layer=tex.transform(size,Image.Transform.AFFINE,affine,Image.Resampling.NEAREST)
        mask=Image.new('L',size);ImageDraw.Draw(mask).polygon(pp,fill=255)
        layer.putalpha(ImageChops.multiply(layer.getchannel('A'),mask));canvas=Image.alpha_composite(canvas,layer)
    return canvas


def contact_icons():
 groups=[('G / CRIMINAL\'S KIT',BASIC[:2]),('H / DEMONIC ATTRIBUTE',BASIC[2:4]),('J / SERIAL KILLER',BASIC[4:])]
 groups += [(k.replace('_',' ').upper(),v) for k,v in WHEELS.items()]
 sheet=Image.new('RGB',(1440,100+len(groups)*158),BG);d=ImageDraw.Draw(sheet)
 d.text((24,18),'FILTHY MONARCH / THE ABYSS PATHWAY',font=font(29),fill=PALE)
 d.text((24,58),'35 abilities / pale sulfur flame, black liquid, blood and filth / original 32 px art',font=font(17),fill=BLUE)
 for row,(label,names) in enumerate(groups):
  y=100+row*158;d.line((24,y,1416,y),fill=(65,39,45));d.text((24,y+10),label,font=font(14),fill=RED)
  for i,n in enumerate(names):
   x=24+i*176
   with Image.open(ASSETS/f'textures/gui/ability_menus/filthy_monarch/{n}.png') as src:im=src.resize((80,80),Image.Resampling.NEAREST)
   sheet.paste(im,(x+35,y+30),im)
   words=NAMES[n].split();lines=['']
   for word in words:
    if len(lines[-1])+len(word)>21:lines.append(word)
    else:lines[-1]+=(' ' if lines[-1] else '')+word
   for j,line in enumerate(lines):d.text((x,y+114+j*16),line,font=font(12),fill=PALE)
 sheet.save(PREVIEW/'icons.png')
 names=[('gui',n) for n in STATUSES]+[('particle/fx',n) for n in PARTICLES]
 sheet=Image.new('RGB',(1200,100+math.ceil(len(names)/8)*105+150),BG);d=ImageDraw.Draw(sheet)
 d.text((24,18),'FILTHY MONARCH / HUD AND PARTICLE MASKS',font=font(26),fill=PALE)
 d.text((24,58),'16 px sprites / grayscale particles accept runtime tint / Depravity and Offerings 0–5',font=font(16),fill=BLUE)
 for i,(group,n) in enumerate(names):
  x=24+i%8*147;y=100+i//8*105
  with Image.open(ASSETS/f'textures/{group}/filthy_monarch/{n}.png') as src:im=src.resize((48,48),Image.Resampling.NEAREST)
  sheet.paste(im,(x+30,y),im);d.text((x,y+55),n.replace('_',' '),font=font(11),fill=PALE)
 for row,counter in enumerate(('depravity','offerings')):
  for i in range(6):
   with Image.open(ASSETS/f'textures/gui/filthy_monarch/{counter}_{i}.png') as src:im=src.resize((128,32),Image.Resampling.NEAREST)
   x=30+i*195;y=sheet.height-140+row*70
   sheet.paste(im,(x,y),im);d.text((x,y+36),f'{counter.title()} {i}/5',font=font(12),fill=PALE)
 sheet.save(PREVIEW/'hud_particles.png')

def sampler():
 import subprocess
 import numpy as np
 from sounds import synth,SR
 from catalog import SOUNDS
 names=['sulfur_flame','devil_form','danger_premonition','die','blather','blood_burst','tide_of_filth','abyssal_sea']
 chunks=[];timeline=[];cursor=0
 for name in names:
  raw=synth(name,SOUNDS.index(name));duration=len(raw)/(SR*2)
  timeline.append({'cue':name,'start_seconds':round(cursor,3),'duration_seconds':round(duration,3)})
  chunks += [raw,np.zeros(int(SR*.35),dtype='<i2').tobytes()];cursor+=duration+.35
 subprocess.run(['ffmpeg','-v','error','-y','-f','s16le','-ar',str(SR),'-ac','1','-i','pipe:0','-c:a','libvorbis','-q:a','5',str(PREVIEW/'sound_sampler.ogg')],input=b''.join(chunks),check=True)
 (PREVIEW/'sound_sampler.json').write_text(json.dumps(timeline,indent=2)+'\n')

def main():
 PREVIEW.mkdir(parents=True,exist_ok=True);contact_icons();sampler()
 manifest=json.loads((ASSETS/'filthy_monarch/asset_manifest.json').read_text())
 names=[r['name'] for r in manifest['models'] if not r['name'][-1:].isdigit()]
 sheet=Image.new('RGB',(1200,95+math.ceil(len(names)/4)*280),BG);d=ImageDraw.Draw(sheet)
 d.text((24,18),'FILTHY MONARCH / ACTUAL MODEL GEOMETRY AND UVs',font=font(25),fill=PALE)
 d.text((24,57),'Independent preview scales. In-game lighting and timing need client review.',font=font(16),fill=BLUE)
 for i,n in enumerate(names):
  x=i%4*300;y=95+i//4*280;sheet.paste(render(n,(300,250)),(x,y));d.text((x+18,y+250),n.replace('_',' '),font=font(14),fill=PALE)
 sheet.save(PREVIEW/'models.png')
 frames=[]
 for f in range(8):
  im=Image.new('RGB',(1200,650),BG);d=ImageDraw.Draw(im)
  for i,n in enumerate(ANIMATED):
   x=i%4*300;y=i//4*325
   im.paste(render(f'{n}_{f%4}',(300,285),yaw=-25,frame=f%4),(x,y+35));d.text((x+15,y+10),n.replace('_',' '),font=font(17),fill=PALE)
  frames.append(im)
 frames[0].save(PREVIEW/'forms.gif',save_all=True,append_images=frames[1:],duration=220,loop=0,disposal=2)
 print(f'Rendered {len(names)} models, 35 icons, HUD/particles, 7 animations and sound sampler.')
if __name__=='__main__':main()
