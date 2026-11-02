"""Review sheets and an animated turntable rendered from the actual model geometry and UVs."""
import json
import math
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFont
from catalog import ASSETS, PREVIEW, ABILITIES, WHEELS, BASIC, KEYS, UPGRADES, PRINCIPLES, STATUSES, NAMES, PARTICLES
from icons import GOLD, PALE, WHITE

BG=(22,20,27)


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
    model=json.loads((ASSETS/f'models/model_fx/white_angel/{name}.json').read_text())
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


def icons():
    groups=[('PLAIN KEYS / Hymn and Light',BASIC)]+[(k.upper(),v) for k,v in WHEELS.items()]
    groups += [('UPGRADES',UPGRADES),('JUSTICE PRINCIPLES',['principle_'+n for n in PRINCIPLES]),('SEVEN KEYS',KEYS)]
    width=1280;rowheight=150
    sheet=Image.new('RGB',(width,85+sum(38+math.ceil(len(v)/7)*rowheight for _,v in groups)),BG)
    d=ImageDraw.Draw(sheet);d.text((24,18),'WHITE ANGEL / ability and menu icons',font=font(27),fill=PALE)
    d.text((24,54),'Original 32 px pixel art. Gold, pure white, sunlight, order and faith.',font=font(16),fill=WHITE)
    y=85
    for label,names in groups:
        d.text((24,y+4),label,font=font(16),fill=GOLD);y+=38
        for i,name in enumerate(names):
            x=24+(i%7)*180;yy=y+i//7*rowheight
            with Image.open(ASSETS/f'textures/gui/ability_menus/white_angel/{name}.png') as source:im=source.resize((96,96),Image.Resampling.NEAREST)
            sheet.paste(im,(x+30,yy),im)
            label=NAMES.get(name,name.replace('principle_','').replace('_',' ').title())
            words=label.split();lines=['']
            for word in words:
                if len(lines[-1])+len(word)>19:lines.append(word)
                else:lines[-1]+=(' ' if lines[-1] else '')+word
            for j,line in enumerate(lines):d.text((x,yy+102+j*18),line,font=font(13),fill=WHITE)
        y+=math.ceil(len(names)/7)*rowheight
    sheet.save(PREVIEW/'icons.png')
    names=list(dict.fromkeys(STATUSES+['guilt_'+str(i) for i in range(6)]+PARTICLES))
    sheet=Image.new('RGB',(1280,85+math.ceil(len(names)/8)*100),BG);d=ImageDraw.Draw(sheet)
    d.text((24,18),'WHITE ANGEL / HUD and tintable particle sprites',font=font(25),fill=PALE)
    for i,name in enumerate(names):
        x=24+i%8*158;y=85+i//8*100
        group='particle/fx' if name in PARTICLES else 'gui'
        with Image.open(ASSETS/f'textures/{group}/white_angel/{name}.png') as source:im=source.resize((48,48),Image.Resampling.NEAREST)
        sheet.paste(im,(x+45,y),im)
        d.text((x,y+53),name.replace('_',' '),font=font(11),fill=WHITE)
    sheet.save(PREVIEW/'hud_particles.png')


def main():
    PREVIEW.mkdir(parents=True,exist_ok=True);icons()
    names=['sun_halo','holy_mace','spear_of_light','golden_scales','blade_of_light','sunbird','sun','hymn_note',
           'holy_armor','sunbird_incomplete','contract_scroll','kingdom_gate','warning_sigil','holy_eye',
           'flame_plume','believer_sun']
    sheet=Image.new('RGB',(1200,90+len(names)//4*305),BG);d=ImageDraw.Draw(sheet)
    d.text((24,16),'WHITE ANGEL / actual ModelFx geometry and UVs',font=font(25),fill=PALE)
    d.text((24,52),'Individual scales for inspection. Runtime glow and final placement require an in-game review.',font=font(15),fill=WHITE)
    for i,name in enumerate(names):
        x=i%4*300;y=90+i//4*305;im=render(name)
        sheet.paste(im,(x,y));d.text((x+18,y+275),name,font=font(16),fill=WHITE)
    sheet.save(PREVIEW/'models.png')
    frames=[]
    for f in range(16):
        im=Image.new('RGB',(960,350),BG);d=ImageDraw.Draw(im)
        for i,name in enumerate((f'sunbird_{f%4}',f'sunbird_incomplete_{f%4}','sun')):
            im.paste(render(name,(320,305),yaw=-25+f*22.5,frame=f%4),(i*320,35))
            d.text((i*320+15,10),name,font=font(15),fill=PALE)
        frames.append(im)
    frames[0].save(PREVIEW/'sunbird.gif',save_all=True,append_images=frames[1:],duration=160,loop=0,disposal=2)
    print('Rendered ability/HUD sheets, actual model geometry and Sunbird animation turntable.')


if __name__=='__main__':main()
