"""Contact sheets rendered from the packaged Nature Walker geometry and UVs."""
import argparse
import json
import math
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFont
from catalog import ASSETS, PREVIEW, ABILITIES, WHEELS, BASIC, KEYS, UPGRADES, STATUSES, NAMES, PARTICLES, ANIMATED
from icons import GOLD, PALE
BG=(18,30,26)


def font(size):
    for path in ('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf','C:/Windows/Fonts/consola.ttf'):
        if Path(path).is_file(): return ImageFont.truetype(path,size)
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
    model=json.loads((ASSETS/f'models/model_fx/nature_walker/{name}.json').read_text())
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


def sheet_icons():
    groups=[('G / SOW',BASIC[:2]),('H / TREATMENT',BASIC[2:4]),('K / BIOLOGY',BASIC[4:])]
    groups += [(k.upper(),v) for k,v in WHEELS.items()]+[('HIGHER FORMS / VARIANTS',UPGRADES)]
    rows=sum(math.ceil(len(v)/6) for _,v in groups)
    im=Image.new('RGB',(1200,125+rows*150),BG);d=ImageDraw.Draw(im)
    d.text((28,20),'NATURE WALKER / THE MOTHER PATHWAY',font=font(29),fill=PALE)
    d.text((28,65),'39 abilities · life, mutation and return · native 32 px art',font=font(18),fill=GOLD)
    row=0
    for label,names in groups:
        for start in range(0,len(names),6):
            y=120+row*150;row+=1
            d.line((28,y,1172,y),fill=(52,72,58));d.text((28,y+8),label if start==0 else label+' / CONTINUED',font=font(12),fill=GOLD)
            for i,n in enumerate(names[start:start+6]):
                x=28+i*195
                with Image.open(ASSETS/f'textures/gui/ability_menus/nature_walker/{n}.png') as icon:
                    icon=icon.resize((80,80),Image.Resampling.NEAREST)
                im.paste(icon,(x+40,y+30),icon)
                words=NAMES[n].split();lines=['']
                for w in words:
                    if len(lines[-1]+' '+w)>23:lines.append(w)
                    else:lines[-1]+=(' ' if lines[-1] else '')+w
                for j,l in enumerate(lines):d.text((x,y+113+j*15),l,font=font(12),fill=PALE)
    im.save(PREVIEW/'icons.png')
    names=[('gui',n) for n in STATUSES]+[('particle/fx',n) for n in PARTICLES]
    im=Image.new('RGB',(1200,100+math.ceil(len(names)/8)*105),BG);d=ImageDraw.Draw(im)
    d.text((28,20),'NATURE WALKER / HUD & PARTICLES',font=font(27),fill=PALE)
    d.text((28,61),'16 px status symbols · particles accept runtime color',font=font(17),fill=GOLD)
    for i,(group,n) in enumerate(names):
        x=24+i%8*147;y=100+i//8*105
        with Image.open(ASSETS/f'textures/{group}/nature_walker/{n}.png') as icon:icon=icon.resize((48,48),Image.Resampling.NEAREST)
        im.paste(icon,(x+30,y),icon);d.text((x,y+57),n.replace('_',' '),font=font(11),fill=PALE)
    im.save(PREVIEW/'hud_particles.png')


def audio_sampler():
    """Audition packaged cues with 350 ms silence between them; sidecar records start times."""
    import subprocess
    import numpy as np
    names=('thorn_seed','growth_bloom','treatment','reap','toxin_bloom','stone_golem',
           'stone_bird','mutate','root_eruption','evil_spirits','rebirth_bloom','bountiful_earth')
    segments=[];timeline=[];elapsed=0
    for name in names:
        data=subprocess.check_output(['ffmpeg','-v','error','-i',str(ASSETS/f'sounds/nature_walker/{name}.ogg'),
                                      '-f','f32le','-acodec','pcm_f32le','pipe:1'])
        wave=np.frombuffer(data,dtype='<f4');timeline.append({'cue':name,'start_seconds':round(elapsed,3)})
        segments.extend((wave,np.zeros(int(.35*44100),dtype='<f4')));elapsed+=len(wave)/44100+.35
    subprocess.run(['ffmpeg','-v','error','-y','-f','f32le','-ar','44100','-ac','1','-i','pipe:0',
                    '-c:a','libvorbis','-q:a','5',str(PREVIEW/'sound_sampler.ogg')],
                   input=np.concatenate(segments).astype('<f4').tobytes(),check=True)
    (PREVIEW/'sound_sampler.json').write_text(json.dumps(timeline,indent=2)+'\n')


def main():
    PREVIEW.mkdir(parents=True,exist_ok=True);sheet_icons();audio_sampler()
    manifest=json.loads((ASSETS/'nature_walker/asset_manifest.json').read_text())
    names=[r['name'] for r in manifest['models'] if not r['name'][-1:].isdigit()]
    # Split long inventories into useful pages: growth lifecycle and all other props.
    for filename,subset in [('growths',[n for n in names if any(n.startswith(k+'_') for k in manifest['growths'])]),
                            ('models',[n for n in names if not any(n.startswith(k+'_') for k in manifest['growths'])])]:
        im=Image.new('RGB',(1200,100+math.ceil(len(subset)/4)*260),BG);d=ImageDraw.Draw(im)
        d.text((28,20),'NATURE WALKER / '+filename.upper(),font=font(28),fill=PALE)
        d.text((28,62),'Actual geometry and UVs · independent display scales · 16 model pixels = 1 block',font=font(16),fill=GOLD)
        for i,n in enumerate(subset):
            x=i%4*300;y=100+i//4*260;im.paste(render(n,(300,230)),(x,y))
            d.text((x+15,y+232),n.replace('_',' '),font=font(13),fill=PALE)
        im.save(PREVIEW/(filename+'.png'))
    frames=[]
    for f in range(16):
        im=Image.new('RGB',(1200,650),BG);d=ImageDraw.Draw(im)
        for i,n in enumerate(ANIMATED):
            x=i%4*300;y=i//4*325
            im.paste(render(f'{n}_{f%4}',(300,290),yaw=-25+f*22.5,frame=f%4),(x,y+30))
            d.text((x+15,y+10),n.replace('_',' '),font=font(14),fill=PALE)
        frames.append(im)
    frames[0].save(PREVIEW/'forms.gif',save_all=True,append_images=frames[1:],duration=150,loop=0,disposal=2)
    print('Rendered icon, HUD/particle, growth and model sheets; eight animated forms.')


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--assets',type=Path,default=ASSETS)
    p.add_argument('--output',type=Path,default=PREVIEW);a=p.parse_args();ASSETS=a.assets.resolve();PREVIEW=a.output.resolve();main()
