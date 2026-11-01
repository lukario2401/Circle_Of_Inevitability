"""Render actual generated ModelFx geometry/UVs to a contact sheet and turntable.
Uses the same UV projection as the Omniscient Eye preview.
Software orthographic preview; Minecraft lighting and translucency differ.
"""
import math
from pathlib import Path
import json
from PIL import Image, ImageDraw, ImageFont
from generate import ASSETS, REPO, PREFIX, SILVER, FOAM, PARTICLES

ROOT=ASSETS
OUT=REPO/'docs/assets/thunder_god'


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
    canvas=Image.new('RGBA',size,(8,19,37,255))
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
    fontpath='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
    font=ImageFont.truetype(fontpath,14) if Path(fontpath).exists() else ImageFont.load_default()
    names=[m['name'] for m in json.loads((ROOT/f'{PREFIX}/asset_manifest.json').read_text())['models']]
    sheet=Image.new('RGB',(1200,80+math.ceil(len(names)/4)*285),(8,19,37));d=ImageDraw.Draw(sheet)
    d.text((24,20),'THUNDER GOD / actual geometry and authored UV textures',font=font,fill=SILVER)
    d.text((24,44),'Independent scales for inspection. World dimensions and pivots are in the manifest.',font=font,fill=FOAM)
    for i,name in enumerate(names):
        x=i%4*300;y=80+i//4*285
        sheet.paste(render(name,(300,252)),(x,y));d.text((x+16,y+257),name,font=font,fill=SILVER)
    sheet.save(OUT/'models.png')
    frames=[]
    for f in range(16):
        im=Image.new('RGB',(960,350),(8,19,37));draw=ImageDraw.Draw(im)
        for i,name in enumerate((f'wave_crest_{f%4}','whirlpool','kingdom_chasm')):
            im.paste(render(name,(320,300),yaw=-25+f*8,inkframe=f%4),(320*i,30))
            draw.text((320*i+20,8),name,font=font,fill=SILVER)
        frames.append(im)
    frames[0].save(OUT/'motion.gif',save_all=True,append_images=frames[1:],duration=140,loop=0)
    # Texture inspection includes alpha over a checkerboard and every animated frame.
    names=['water','cloud','lightning','foam','whirlpool','cloud_face','kingdom_chasm','kingdom_dreadful']
    sheet=Image.new('RGB',(1152,100+len(names)*140),(8,19,37));d=ImageDraw.Draw(sheet)
    d.text((24,20),'MATERIALS / four texture frames, alpha shown on checkerboard',font=font,fill=SILVER)
    for i,name in enumerate(names):
        im=Image.open(ROOT/f'textures/model_fx/{PREFIX}/{name}.png').convert('RGBA')
        for f in range(im.height//im.width):
            tile=Image.new('RGB',(128,128));td=ImageDraw.Draw(tile)
            for yy in range(0,128,16):
                for xx in range(0,128,16):td.rectangle((xx,yy,xx+15,yy+15),fill=(42,55,69) if (xx+yy)//16%2 else (24,35,49))
            crop=im.crop((0,f*im.width,im.width,(f+1)*im.width)).resize((128,128),Image.Resampling.NEAREST)
            tile.paste(crop,(0,0),crop);sheet.paste(tile,(260+f*150,70+i*140))
        d.text((24,100+i*140),name,font=font,fill=SILVER)
    sheet.save(OUT/'materials.png')
    print('Rendered 27 model views, animated motion preview and material contact sheet.')


if __name__=='__main__':main()
