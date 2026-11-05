"""Original spatial mono cues: glass thoughts, muted heartbeats, paper and dragon breath."""
import math
import subprocess
import numpy as np
from catalog import SOUNDS, NAMES

SR=44100
SUBTITLES={n:NAMES.get(n,n.replace('_',' ').title()) for n in SOUNDS}
SUBTITLES.update(insight_gain='A thought becomes clear',insight_spent='An insight breaks',resist='A mind resists',
                 wake='A dream breaks',warning='A thought gathers',wingbeat='Draconic wings beat',
                 script_trigger='A written outcome unfolds',field_end='The dream recedes')


def synth(name,index):
    duration=2.4 if name in ('dragon_form','sea_of_minds','life_creation') else 1.2
    if name in ('insight_gain','insight_spent','resist','wake','warning'):duration=.55
    t=np.arange(int(SR*duration))/SR;rng=np.random.default_rng(7300+index)
    root=(196,220,246.94,293.66,329.63,369.99,415.3)[index%7]*2**(index/120)
    out=np.zeros(len(t))
    def bell(freq,at=0,gain=.3):
        tt=np.maximum(0,t-at);env=(t>=at)*np.minimum(1,tt/.006)*np.exp(-tt*5)
        return gain*(np.sin(math.tau*freq*tt)+.23*np.sin(math.tau*freq*2.76*tt))*env
    if name in ('dragon_form','life_creation','wingbeat','mind_storm'):
        noise=np.convolve(rng.normal(0,1,len(t)),np.ones(29)/29,mode='same')
        out+=noise*.8*np.sin(math.pi*t/duration)**1.2
        out+=.22*np.sin(math.tau*(65*t+17*(1-np.exp(-t*4))))*np.sin(math.pi*t/duration)**2
        out+=bell(root,.1,.15)
    elif name in ('foreshadow','plot_twist','fate_script','envision','script_trigger'):
        noise=rng.normal(0,.10,len(t));out+=noise*(.5+.5*np.sin(70*t))*np.exp(-t*5)
        out+=bell(root,.05)+bell(root*1.5,.22,.2)
        out+=.12*np.sin(math.tau*85*t)*np.exp(-((t-.35)/.06)**2)
    elif name in ('awe','frenzy','nightmare','psychoanalysis','battle_hypnosis','resist'):
        out+=.16*np.sin(math.tau*(root*t-45*t*t))*np.exp(-t*4)
        out+=bell(root*1.059,.02,.22)+bell(root*2,.08,.12)
        out+=rng.normal(0,.045,len(t))*np.exp(-t*8)
    else:
        descending=name in ('sleep','dream_entry','psychological_invisibility','field_end')
        for j in range(3):out+=bell(root*2**((2-j if descending else j)/5),j*(.10+.01*(index%5)),.23)
        out+=.05*np.sin(math.tau*root*.5*t)*np.sin(math.pi*t/duration)**2
    for delay,gain in ((.079,.17),(.151,.09)):
        n=int(delay*SR);out[n:]+=out[:-n].copy()*gain
    out-=out.mean();out*=.68/max(.001,np.max(np.abs(out)))
    fade=int(.02*SR);out[:fade]*=np.linspace(0,1,fade);out[-fade:]*=np.linspace(1,0,fade)
    return np.asarray(out*32767,dtype='<i2').tobytes()


def generate(root):
    folder=root/'sounds/author';folder.mkdir(parents=True,exist_ok=True)
    for i,name in enumerate(SOUNDS):
        subprocess.run(['ffmpeg','-v','error','-y','-f','s16le','-ar',str(SR),'-ac','1','-i','pipe:0',
                        '-c:a','libvorbis','-q:a','5','-map_metadata','-1',str(folder/(name+'.ogg'))],
                       input=synth(name,i),check=True)
    return {f'author.{n}':{'subtitle':f'subtitles.circle_of_inevitability.author.{n}',
                         'sounds':[{'name':f'circle_of_inevitability:author/{n}'}]} for n in SOUNDS}
