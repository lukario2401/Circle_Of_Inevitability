"""Render actual generated ModelFx geometry/UVs to a contact sheet and turntable.
Software orthographic preview; Minecraft's lighting and translucency differ.
"""
import math
from pathlib import Path
import json
from PIL import Image, ImageDraw, ImageFont
from generate import DEFAULT_ASSETS, REPO, PREFIX, GOLD, CREAM

ROOT=DEFAULT_ASSETS
OUT=REPO/'docs/assets/omniscient_eye'


def rotated(p,rotation):
    if not rotation: return p
    x,y,z=[p[i]-rotation['origin'][i] for i in range(3)]
    a=math.radians(rotation['angle']); c,s=math.cos(a),math.sin(a)
    if rotation['axis']=='y': x,z=x*c+z*s,-x*s+z*c
    elif rotation['axis']=='x': y,z=y*c-z*s,y*s+z*c
    else: x,y=x*c-y*s,x*s+y*c
    return [v+rotation['origin'][i] for i,v in enumerate((x,y,z))]


def render(name,size=(320,300),yaw=-25,inkframe=0):
    model=json.loads((ROOT/f'models/model_fx/{PREFIX}/{name}.json').read_text())
    textures={}
    for key,ref in model['textures'].items():
        im=Image.open(ROOT/('textures/'+ref.split(':')[1]+'.png')).convert('RGBA')
        if im.height>im.width: im=im.crop((0,(inkframe%4)*im.width,im.width,(inkframe%4+1)*im.width))
        textures[key]=im
    a=math.radians(yaw); c,s=math.cos(a),math.sin(a)
    def project(p):
        x,y,z=p; x-=8; z-=8
        xx=x*c+z*s; zz=-x*s+z*c
        return (xx,-y*.92-zz*.39,zz*.92-y*.39)
    faces=[]; allpts=[]
    # Face order starts at UV top-left, then bottom-left, bottom-right, top-right.
    definitions={
        'north':[(1,1,0),(1,0,0),(0,0,0),(0,1,0)],
        'south':[(0,1,1),(0,0,1),(1,0,1),(1,1,1)],
        'west':[(0,1,0),(0,0,0),(0,0,1),(0,1,1)],
        'east':[(1,1,1),(1,0,1),(1,0,0),(1,1,0)],
        'up':[(0,1,0),(0,1,1),(1,1,1),(1,1,0)],
        'down':[(0,0,1),(0,0,0),(1,0,0),(1,0,1)]}
    for el in model['elements']:
        for side,face in el['faces'].items():
            pts=[]
            for corner in definitions[side]:
                p=[el['from'][i]+corner[i]*(el['to'][i]-el['from'][i]) for i in range(3)]
                pts.append(project(rotated(p,el.get('rotation'))))
            # Back-face culling (sign follows the winding above).
            cross=(pts[1][0]-pts[0][0])*(pts[2][1]-pts[0][1])-(pts[1][1]-pts[0][1])*(pts[2][0]-pts[0][0])
            if cross<=1e-6: continue
            faces.append((sum(p[2] for p in pts)/4,pts,face))
            allpts.extend(pts)
    canvas=Image.new('RGBA',size,(22,20,27,255))
    if not allpts: return canvas
    xmin,xmax=min(p[0] for p in allpts),max(p[0] for p in allpts)
    ymin,ymax=min(p[1] for p in allpts),max(p[1] for p in allpts)
    scale=min((size[0]-50)/max(1,xmax-xmin),(size[1]-45)/max(1,ymax-ymin))
    def screen(p):return ((p[0]-(xmin+xmax)/2)*scale+size[0]/2,(p[1]-(ymin+ymax)/2)*scale+size[1]/2)
    for _,pts,face in sorted(faces,key=lambda f:-f[0]):
        pp=[screen(p) for p in pts]
        tex=textures[face['texture'].lstrip('#')]
        u0,v0,u1,v1=face['uv']
        # Inverse affine map from screen to source UV; projected cube faces are parallelograms.
        p0,p1,p2,p3=pp; ax,ay=p3[0]-p0[0],p3[1]-p0[1]; bx,by=p1[0]-p0[0],p1[1]-p0[1]
        det=ax*by-ay*bx
        if abs(det)<1e-5:continue
        ux,uy=by/det,-bx/det; vx,vy=-ay/det,ax/det
        du,dv=(u1-u0)*tex.width/16,(v1-v0)*tex.height/16
        affine=(ux*du,uy*du,u0*tex.width/16-(ux*p0[0]+uy*p0[1])*du,
                vx*dv,vy*dv,v0*tex.height/16-(vx*p0[0]+vy*p0[1])*dv)
        layer=tex.transform(size,Image.Transform.AFFINE,affine,Image.Resampling.NEAREST)
        mask=Image.new('L',size); ImageDraw.Draw(mask).polygon(pp,fill=255)
        # Multiply alpha by polygon mask to avoid painting the whole source image's bounding box.
        from PIL import ImageChops
        layer.putalpha(ImageChops.multiply(layer.getchannel('A'),mask))
        canvas=Image.alpha_composite(canvas,layer)
    return canvas


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    font=ImageFont.truetype('C:/Windows/Fonts/consola.ttf',17)
    names=['reading_eye','brass_mirror','info_card','living_tome_closed','living_tome_open','tome_creature_0',
           'bookshelf_form','phantom_tower','spiral_tower','kingdom_bookshelf','ritual_sigil','golden_armor']
    sheet=Image.new('RGB',(1280,1070),(22,20,27)); d=ImageDraw.Draw(sheet)
    d.text((22,16),'OMNISCIENT EYE / actual ModelFx geometry and UV textures',font=font,fill=GOLD)
    for i,name in enumerate(names):
        x=i%4*320; y=55+i//4*335
        im=render(name)
        sheet.paste(im,(x,y)); d.text((x+15,y+305),name,font=font,fill=CREAM)
    sheet.save(OUT/'models.png')
    frames=[]
    for i in range(24):
        im=Image.new('RGB',(720,420),(22,20,27)); d=ImageDraw.Draw(im)
        d.text((20,15),'LIVING TOME / SPIRAL BOOKSHELF',font=font,fill=GOLD)
        for x,name in [(0,f'tome_creature_{i//2%4}'),(360,'spiral_tower')]:
            view=render(name,(360,340),yaw=-25+i*15,inkframe=i//2)
            im.paste(view,(x,55))
        frames.append(im)
    frames[0].save(OUT/'turntable.gif',save_all=True,append_images=frames[1:],duration=100,loop=0,disposal=2)
    print('Rendered actual model textures, geometry, four creature poses and 360-degree turntable.')


if __name__=='__main__':main()
