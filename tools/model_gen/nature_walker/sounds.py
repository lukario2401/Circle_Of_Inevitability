"""Original mono synthesis: seed cracks, wood, earth, wet spores, spirits and life chimes."""
import math
import subprocess
import numpy as np
from catalog import SOUNDS, NAMES

SR=44100
SUBTITLES={n:NAMES.get(n,n.replace('_',' ').title()) for n in SOUNDS}
SUBTITLES.update(growth_sprout='A seed cracks and takes root',growth_bloom='Leaves unfurl',growth_wither='Dry leaves crumble',
    heartfruit_eat='Heartfruit is eaten',cross_pollinate='Briars and blossoms entwine',mutation_roll='Life twists',
    mutation_bloom='Unnatural growth bursts',rebirth_trigger='A heartbeat sinks into the earth',
    rebirth_bloom='A new life blossoms',warning='Roots stir beneath the ground',root_impact='Roots tear through earth',
    construct_step='A heavy construct steps',bird_wingbeat='Stone wings beat',melee_swing='Thorns whip through air',
    melee_impact='Thorns strike',field_end='The living earth settles',weather_clear='Clouds part',
    weather_storm='A storm gathers',nature_walk='Nature opens a path',brood_form_complete='The Mother’s brood awakens')
EARTH=set('underground_travel swamp stone_golem mud_golem steel_golem earthquake root_eruption maternal_embrace return_to_earth embrace_of_the_earth root_impact construct_step'.split())
DEATH=set('evil_spirits descent_of_death desolation barren_land field_end growth_wither'.split())
MUTATION=set('cross_breed toxin_bloom mutate brood_form brood_form_complete mutation_roll mutation_bloom'.split())
MAJOR=set('great_harvest awaken world_of_nature bountiful_earth brood_form_complete earthquake'.split())


def synth(name,index):
    duration=2.6 if name in MAJOR else (.55 if name in ('construct_step','bird_wingbeat','melee_impact','growth_sprout') else 1.35)
    t=np.arange(int(SR*duration))/SR;rng=np.random.default_rng(91000+index)
    white=rng.normal(0,1,len(t));low=np.convolve(white,np.ones(151)/151,mode='same')
    rustle=np.convolve(white,np.ones(9)/9,mode='same')
    root=(146.83,164.81,196,220,246.94,293.66,329.63)[index%7]*2**(index/180)
    out=np.zeros(len(t))
    def pluck(freq,at=0,gain=.3,decay=7):
        tt=np.maximum(0,t-at);env=(t>=at)*np.minimum(1,tt/.005)*np.exp(-tt*decay)
        return gain*(np.sin(math.tau*freq*tt)+.18*np.sin(math.tau*freq*2.7*tt))*env
    def thud(at,gain=.35):
        tt=np.maximum(0,t-at)
        return gain*(t>=at)*np.minimum(1,tt/.008)*np.exp(-tt*10)*(np.sin(math.tau*(65*tt+7*(1-np.exp(-tt*20))))+low*3)
    if name in EARTH:
        out+=thud(.025,.48)+thud(.19,.23)
        out+=rustle*.18*np.exp(-t*4)*(1+.5*np.sin(83*t))
        if name=='steel_golem':
            for f in (317,543,901,1433):out+=pluck(f,.035,.12,4)
        elif name in ('mud_golem','swamp'):
            out+=.19*np.sin(math.tau*(80*t+13*np.sin(t*17)))*np.exp(-t*4)
        else:out+=pluck(root*.5,.07,.16,9)
    elif name in DEATH:
        out+=.26*np.sin(math.tau*(root*.5*t-14*t*t))*np.sin(math.pi*t/duration)**2
        out+=.12*np.sin(math.tau*(root*.503*t-14*t*t))*np.sin(math.pi*t/duration)**2
        out+=rustle*.12*np.exp(-t*2)+pluck(root*2,.12,.10,3)
    elif name in MUTATION:
        out+=.19*np.sin(math.tau*root*t+6*np.sin(math.tau*19*t))*np.exp(-t*3)
        for j in range(5):out+=thud(.035+j*.17,.13)+pluck(root*(1+j*.23),j*.13,.12,14)
        out+=rustle*.17*np.sin(t*29)**2*np.exp(-t*3)
    elif name in ('bear_form','wrath_of_nature','weather_storm'):
        out+=low*3.5*np.sin(math.pi*t/duration)**.8
        out+=.2*np.sin(math.tau*(52*t+4*np.sin(t*8)))*np.sin(math.pi*t/duration)**2
        out+=rustle*.12*np.exp(-t*2)
    elif name in ('reap','great_harvest','melee_swing','melee_impact','bird_wingbeat','stone_bird'):
        out+=rustle*.8*np.exp(-((t-.20)/.13)**2)+thud(.27,.3)
        out+=pluck(root*1.5,.28,.15,7)
    elif name in ('weather_ritual','weather_clear'):
        out+=rustle*.23*np.sin(math.pi*t/duration)**2
        for j in range(7):out+=pluck(root*(2+j/4),.05+j*.13,.09,18)
    else:
        for j,ratio in enumerate((1,1.25,1.5)):
            out+=pluck(root*ratio,.04+j*(.12+.008*(index%5)),.23,5)
        out+=rustle*.15*np.exp(-t*3)*(np.sin(48*t)**8)
        out+=thud(.025,.10)
    if name in MAJOR:
        for j,ratio in enumerate((.5,1,1.5,2)):
            out+=.09*np.sin(math.tau*root*ratio*t)*np.sin(math.pi*t/duration)**2
    # Short reflections; fixed seed makes decoded waveforms repeatable across regeneration.
    for delay,gain in ((.071,.14),(.143,.07)):
        n=int(delay*SR);out[n:]+=out[:-n].copy()*gain
    out-=out.mean();out*=.66/max(.001,np.max(np.abs(out)))
    fade=int(.025*SR);out[:fade]*=np.linspace(0,1,fade);out[-fade:]*=np.linspace(1,0,fade)
    return np.asarray(out*32767,dtype='<i2').tobytes()


def generate(root):
    folder=root/'sounds/nature_walker';folder.mkdir(parents=True,exist_ok=True)
    for i,n in enumerate(SOUNDS):
        subprocess.run(['ffmpeg','-v','error','-y','-f','s16le','-ar',str(SR),'-ac','1','-i','pipe:0',
                        '-c:a','libvorbis','-q:a','5','-map_metadata','-1',str(folder/(n+'.ogg'))],input=synth(n,i),check=True)
    return {'nature_walker.'+n:{'subtitle':'subtitles.circle_of_inevitability.nature_walker.'+n,
                              'sounds':[{'name':'circle_of_inevitability:nature_walker/'+n}]} for n in SOUNDS}
