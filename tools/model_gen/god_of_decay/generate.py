"""Deterministic, code-native God of Decay assets. Requires Pillow and ffmpeg.
Run: python3 tools/model_gen/god_of_decay/generate.py <assets root> [preview.png]
No downloaded artwork or audio. Incubation deliberately has no cast sound.
"""
import json
import math
from pathlib import Path
import random
import subprocess
import sys
import tempfile
import wave
from array import array
from PIL import Image, ImageDraw
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from modelkit import Model, Texture

ABILITIES = 'ulcerous_burst carriers_touch red_tape official_capacity corruption hollow_out laziness obesity degeneration incubation rot division pathogen_form certain_death sure_hit decay swollen_form left_hand_of_decay withering_palm gravel_shot slow_time segmented_time senescence entropy virus_incarnation ragnarok'.split()
EXTRAS = 'outbreak contagion collapse reform time_giant ancient_god pathogen_pool sickness weathering'.split()
GREEN, PUS, BLUE, GOLD, TIME, ASH, DARK = (154,184,46), (181,196,58), (31,51,80), (168,144,60), (127,184,200), (154,150,144), (42,58,42)

def color(name):
    if name in ('slow_time', 'segmented_time', 'time_giant'): return TIME
    if name in ('senescence', 'ancient_god', 'weathering', 'gravel_shot', 'collapse'): return ASH
    if name in ('red_tape', 'official_capacity', 'corruption', 'hollow_out', 'sure_hit'): return GOLD
    if name in ('swollen_form', 'left_hand_of_decay', 'withering_palm'): return BLUE
    if name in ('entropy', 'ragnarok', 'certain_death'): return DARK
    return GREEN

def save_image(root, rel, im):
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    im.save(p)

def icon(name, size=32):
    # Draw at 32px so the wheel and status glyphs share the same silhouette.
    im = Image.new('RGBA', (32,32)); d = ImageDraw.Draw(im)
    c = color(name) + (255,); light = tuple(min(255,x+65) for x in c[:3]) + (255,)
    d.ellipse((2,2,29,29), fill=(12,19,16,220), outline=c, width=1)
    i = (ABILITIES+EXTRAS).index(name)
    if name in ('left_hand_of_decay','withering_palm','carriers_touch'):
        d.rounded_rectangle((10,14,23,26), radius=3, fill=c, outline=light)
        for k,h in enumerate((9,5,6,9)): d.rounded_rectangle((9+k*4,h,12+k*4,20), radius=1, fill=light)
        d.line((11,23,7,17), fill=light, width=4)
        d.line((16,24,18,15,15,9), fill=PUS+(255,), width=1)
    elif name in ('slow_time','segmented_time','senescence','entropy','ragnarok'):
        d.arc((7,6,25,25), 5, 105, fill=light, width=2)
        d.arc((7,6,25,25), 125, 225, fill=light, width=2)
        d.arc((7,6,25,25), 245, 345, fill=light, width=2)
        d.line((16,8,16,16,22,19), fill=c, width=2)
        if name in ('senescence','entropy','ragnarok'): d.line((12,4,19,12,13,19,20,28), fill=ASH+(255,), width=2)
    elif name in ('red_tape','official_capacity','corruption','hollow_out'):
        d.polygon(((9,6),(21,6),(25,10),(25,25),(9,25)), fill=c, outline=light)
        for y in (10,14,18): d.line((12,y,21,y), fill=(35,38,27), width=1)
        if name=='red_tape': d.line((7,23,25,10), fill=(180,45,37), width=3)
        elif name=='hollow_out': d.ellipse((13,12,22,22), fill=(8,12,10), outline=light)
        elif name=='official_capacity': d.ellipse((17,19,27,28), fill=GOLD, outline=light)
    elif name in ('division','swollen_form','time_giant','ancient_god','pathogen_form','virus_incarnation'):
        for x in ((10,21) if name=='division' else (16,)):
            d.ellipse((x-3,6,x+3,12), fill=light)
            d.polygon(((x-4,13),(x+4,13),(x+7,24),(x+2,26),(x,21),(x-2,26),(x-7,24)), fill=c, outline=light)
        if name in ('pathogen_form','virus_incarnation'):
            for k in range(10): d.ellipse((5+k*7%23,9+k*11%19,7+k*7%23,11+k*11%19), fill=PUS)
    elif name in ('certain_death','sure_hit','gravel_shot','weathering'):
        d.line((7,25,23,8), fill=c, width=5)
        d.line((8,24,24,7), fill=light, width=1)
        d.polygon(((22,5),(28,9),(23,14),(19,10)), fill=light)
    else:
        # Disease glyph: membrane, branching veins and distinct cores.
        d.ellipse((8,8,24,24), fill=c, outline=light, width=2)
        for k in range(6):
            a=k*math.tau/6
            d.line((16+7*math.cos(a),16+7*math.sin(a),16+11*math.cos(a),16+11*math.sin(a)), fill=light, width=2)
        for k in range(1+i%5):
            x=12+k*5%10; y=12+k*3%9
            d.rectangle((x,y,x+2,y+2), fill=(20,36,23))
        if name=='laziness': d.line((10,13,21,13,11,21,22,21), fill=(20,36,23), width=2)
        if name=='rot': d.line((16,8,13,16,20,20,15,25), fill=ASH, width=2)
    # Small ability-specific notch also keeps adjacent glyphs distinguishable at 16px.
    d.rectangle((4+i%6*4,28,6+i%6*4,29), fill=light)
    return im.resize((size,size), Image.Resampling.LANCZOS)

def models(root):
    def build(name, tint):
        t=Texture('god_of_decay/'+name,32,32)
        def skin(tx,ty,x,y,f):
            vein = abs(math.sin(x*.48 + math.sin(y*.35)*2)) < .14
            n = ((x*17+y*31)%13)-6
            if name=='time_ring': return (255,255,255,230)
            if name=='time_giant':
                wave = int(18*math.sin(y*.55+x*.15))
                return tuple(max(0,min(255,v+wave)) for v in tint)+(145,)
            if name=='ancient_god':
                wrinkle = -35 if math.sin(y*.9+math.sin(x*.3)*2)>.65 else n
                return tuple(max(0,min(255,v+wrinkle)) for v in tint)+(235,)
            if name in ('gravel','decay_cracks','red_tape','hollow_shell'): return tuple(max(0,min(255,v+n)) for v in tint)+(230,)
            return (PUS+(210,)) if vein else tuple(max(0,min(255,v+n)) for v in tint)+(210,)
        t.zone('skin',0,0,16,16,skin)
        m=Model('god_of_decay/'+name,t, name+'; pivot (8,0,8); units 16 per block.')
        return m,t
    for name,c in [('left_hand',BLUE),('swollen_body',BLUE),('time_giant',TIME),('ancient_god',ASH),('double',GREEN),('gravel',ASH),('pathogen',PUS),('time_ring',TIME),('red_tape',GOLD),('hollow_shell',GOLD),('decay_cracks',ASH),('pool',GREEN)]:
        m,t=build(name,c)
        if name=='left_hand':
            m.cube('wrist',(5,0,6),(12,6,11),'skin')
            m.cube('swollen palm',(2,5,5),(14,17,12),'skin')
            for k,h in enumerate((25,29,27,23)):
                x=2+k*3
                m.cube('finger '+str(k),(x,15,6),(x+2.6,h,10.5),'skin')
                m.cube('knuckle '+str(k),(x-.3,17,5.7),(x+2.9,20,11),'skin')
            m.cube('left thumb',(13,6,6),(17,15,10),'skin',rot=(-22.5,'z',(13,8,8)))
        elif name in ('swollen_body','time_giant','ancient_god','double'):
            m.cube('left leg',(3,0,5),(7,11,11),'skin'); m.cube('right leg',(9,0,5),(13,11,11),'skin')
            m.cube('abdomen',(1,9,3),(15,22,13),'skin')
            m.cube('shoulders',(-1,19,4),(17,25,12),'skin')
            m.cube('head',(4,24,5 if name!='ancient_god' else 1),(12,32,12 if name!='ancient_god' else 9),'skin')
            for x in (-4,17): m.cube('arm',(x,8,5),(x+3,23,11),'skin')
            if name=='swollen_body':
                for x,y,z in ((0,15,2),(11,12,1),(5,21,2),(14,18,10)):
                    m.cube('ulcer',(x,y,z),(x+3,y+3,z+3),'skin')
            if name=='ancient_god': m.cube('hunched back',(0,16,9),(16,28,16),'skin')
        elif name=='time_ring':
            for k in range(24):
                if k%8==7: continue
                a=k*math.tau/24; x=8+7*math.cos(a); z=8+7*math.sin(a)
                m.cube('segment',(x-.6,0,z-.6),(x+.6,1,z+.6),'skin')
        elif name=='red_tape':
            for y in (2,8,14):
                for x in (0,15): m.cube('side',(x,y,0),(x+1,y+1,16),'skin')
                for z in (0,15): m.cube('ribbon',(0,y,z),(16,y+1,z+1),'skin')
        elif name=='hollow_shell':
            for x in (1,14): m.cube('shell',(x,0,5),(x+1,25,11),'skin')
            for y in (0,24): m.cube('shell',(1,y,5),(15,y+1,11),'skin')
        elif name in ('pool','decay_cracks'):
            for k in range(12):
                x=(k*7)%15; z=(k*11)%15
                m.cube('crust',(x,0,z),(min(16,x+4),.25,min(16,z+3)),'skin')
        else:
            m.cube('core',(4,4,4),(12,12,12),'skin',rot=(22.5,'z',(8,8,8)))
            if name=='pathogen':
                for x,y,z in ((0,7,7),(12,7,7),(7,0,7),(7,12,7),(7,7,0),(7,7,12)):
                    m.cube('spike',(x,y,z),(x+3,y+3,z+3),'skin')
        m.save(root); t.save(root)

def sounds(root):
    events=json.loads((root/'sounds.json').read_text())
    lang=json.loads((root/'lang/en_us.json').read_text())
    folder=root/'sounds/god_of_decay'; folder.mkdir(parents=True,exist_ok=True)
    names=[n for n in ABILITIES+EXTRAS if n not in ('incubation','sickness')]
    for i,name in enumerate(names):
        rng=random.Random(410+i); sr=22050
        duration=1.7 if name in ('ragnarok','time_giant','ancient_god','entropy') else .65+(i%3)*.15
        samples=array('h'); prev=0
        for j in range(int(sr*duration)):
            t=j/sr; u=t/duration
            prev=.85*prev+.15*rng.uniform(-1,1)
            f=65+i*9
            tone=math.sin(math.tau*(f*t-18*t*t))
            if color(name)==TIME: tone=math.sin(math.tau*440*t)*math.exp(-4*t)+.4*math.sin(math.tau*659*t)
            env=min(1,t/.025)*((1-u)**2)
            pulse=.65+.35*math.sin(math.tau*(6+i%5)*t)
            samples.append(int(15000*env*(.55*prev+.3*tone*pulse)))
        with tempfile.TemporaryDirectory() as temp:
            wav=Path(temp)/'sound.wav'
            with wave.open(str(wav),'wb') as w:
                w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr); w.writeframes(samples.tobytes())
            subprocess.run(['ffmpeg','-v','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','4',str(folder/(name+'.ogg'))],check=True)
        key='god_of_decay.'+name; sub='subtitles.circle_of_inevitability.'+key
        events[key]={'subtitle':sub,'sounds':[{'name':'circle_of_inevitability:god_of_decay/'+name}]}
        lang[sub]='God of Decay: '+name.replace('_',' ').capitalize()
    (root/'sounds.json').write_text(json.dumps(events,indent=2)+'\n')
    (root/'lang/en_us.json').write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n')

def main():
    root=Path(sys.argv[1]); models(root)
    for name in ABILITIES+EXTRAS:
        save_image(root,'textures/gui/ability_menus/god_of_decay/'+name+'.png',icon(name))
        save_image(root,'textures/gui/god_of_decay/'+name+'.png',icon(name,16))
    save_image(root,'textures/gui/pathway_icons/god_of_decay.png',icon('left_hand_of_decay',16))
    for name in ('pathogen','ash','time_shard'):
        im=Image.new('RGBA',(16,16)); d=ImageDraw.Draw(im)
        if name=='pathogen':
            d.ellipse((4,4,11,11), fill=(255,255,255,235))
            for a in range(0,360,60):
                r=math.radians(a); d.line((8,8,8+7*math.cos(r),8+7*math.sin(r)),fill=(255,255,255,180),width=2)
        elif name=='ash': d.polygon(((3,3),(12,5),(9,9),(11,13),(4,11),(6,7)),fill=(255,255,255,210))
        else: d.polygon(((8,1),(11,8),(8,15),(6,8)),fill=(255,255,255,230))
        save_image(root,'textures/particle/fx/decay_'+name+'.png',im)
    # Translucent first-person hand and dusk sky; no full opaque screen cover.
    hand=Image.new('RGBA',(256,256)); d=ImageDraw.Draw(hand)
    d.rounded_rectangle((70,110,190,250),radius=35,fill=BLUE+(175,),outline=PUS+(190,),width=3)
    for k,h in enumerate((60,20,30,65)): d.rounded_rectangle((65+k*33,h,94+k*33,155),radius=12,fill=BLUE+(185,),outline=PUS+(160,),width=2)
    d.line((130,245,145,170,110,120,115,60),fill=PUS+(200,),width=4)
    save_image(root,'textures/gui/god_of_decay/hand_vision.png',hand)
    sky=Image.new('RGBA',(256,128)); d=ImageDraw.Draw(sky)
    for y in range(128): d.line((0,y,255,y),fill=(25+y//9,45+y//12,28, max(0,145-y)))
    for k in range(18):
        x=k*47%256; d.line((x,0,x-8,20,x+3,35,x-5,54),fill=(154,150,110,75),width=1)
    save_image(root,'textures/gui/god_of_decay/dusk_sky.png',sky)
    sounds(root)
    if len(sys.argv)>2:
        sheet=Image.new('RGB',(560,((len(ABILITIES+EXTRAS)+6)//7)*100),(20,25,23)); d=ImageDraw.Draw(sheet)
        for i,name in enumerate(ABILITIES+EXTRAS):
            x=i%7*80; y=i//7*100; sheet.paste(icon(name).resize((48,48),Image.Resampling.NEAREST),(x+16,y+5),icon(name).resize((48,48),Image.Resampling.NEAREST))
            words=name.split('_'); d.multiline_text((x+2,y+56),'\n'.join(words),fill=(215,220,201),spacing=1)
        sheet.save(sys.argv[2])
    print('Generated 12 models, 35 wheel/status glyphs, 3 particles, 2 overlays and 33 sounds.')

if __name__=='__main__': main()
