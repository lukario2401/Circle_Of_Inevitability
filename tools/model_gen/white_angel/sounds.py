"""Original mono Vorbis cues, synthesized with fixed seeds; no recorded samples or speech."""
import math
import subprocess
import numpy as np
from catalog import ABILITIES, EXTRA_SOUNDS

SR=44100
SUBTITLES={
    'hymn_of_valor':'A hymn inspires courage', 'hymn_of_renewal':'A hymn restores vitality',
    'sunshine':'Sunlight flashes', 'daytime':'Daylight spreads', 'holy_light':'Holy light descends',
    'sun_halo':'A sun halo forms', 'holy_oath':'A holy oath rings out', 'cleave':'A purifying edge awakens',
    'fire_of_light':'Golden flames roar', 'contract':'A pact is sealed', 'authenticate':'Falsehood is dispelled',
    'amplify':'A blessing is amplified', 'nullify':'A power is nullified', 'purification':'Corruption is cleansed',
    'domain':'An unshadowed domain opens', 'flaring_sun':'A blazing sun falls', 'spear':'A spear of sunlight flies',
    'white_ray':'Pure white radiance pierces', 'holy_equipment':'Holy armor and a mace form',
    'define_justice':'A principle is chosen', 'justice_halo':'The scales of justice awaken',
    'judgment':'Judgment falls', 'faith_appraisal':'Faith reveals guilt', 'solar_envoy':'A Sunbird awakens',
    'praise_the_sun':'A body becomes light', 'holy_kingdom':'The holy kingdom manifests',
    'servant_of_faith':'Faith burns with fervor', 'pure_white_light':'Pure white light unravels all',
    'sun':'The Sun rises', 'warning':'Light gathers', 'countdown':'A seal counts down',
    'pact_breach':'A broken pact burns', 'believer_convert':'A believer embraces the Sun',
    'intuition':'An unseen hunter approaches', 'wingbeat':'Solar wings beat',
    'field_end':'Radiance recedes', 'inextinguishable_light':'Inextinguishable light rekindles',
    'melee_swing':'A crescent of light sweeps', 'melee_impact':'A purifying blow lands',
}


def synth(name,index):
    rng=np.random.default_rng(90100+index)
    duration=2.6 if name in ('sun','holy_kingdom','solar_envoy','hymn_of_valor','hymn_of_renewal') else 1.3
    if name in ('countdown','intuition','melee_swing','melee_impact'):duration=.65
    t=np.arange(int(SR*duration))/SR
    out=np.zeros(len(t))
    root=(261.626,293.665,329.628,391.995,440)[index%5]
    def add(signal,start=0):
        offset=int(start*SR);n=min(len(signal),len(out)-offset)
        if n>0:out[offset:offset+n]+=signal[:n]
    def bell(freq,d=.9):
        tt=np.arange(int(SR*d))/SR; v=np.zeros(len(tt))
        for ratio,amp in ((1,1),(2,.3),(2.76,.15),(4.1,.08)):
            v+=amp*np.sin(math.tau*freq*ratio*tt)*np.exp(-tt*(3+ratio))
        return v*np.minimum(1,tt/.004)
    # Distinct envelopes express hymn, verdict, fire, movement and quiet support.
    if name.startswith('hymn') or name in ('holy_oath','servant_of_faith','holy_kingdom','sun','solar_envoy'):
        minor=name=='hymn_of_renewal'
        for j,ratio in enumerate((1,1.2 if minor else 1.25,1.5,2)):
            phase=math.tau*root*ratio*t+.012*np.sin(t*math.tau*4.8)
            out+=(np.sin(phase)+.22*np.sin(phase*2)+.07*np.sin(phase*3))*.12
            add(bell(root*ratio*2,1.1)*.16,j*.16)
        out*=np.sin(np.minimum(t/.4,1)*math.pi/2)*np.minimum(1,(duration-t)/.6)
    elif name in ('judgment','flaring_sun','pure_white_light','pact_breach','melee_impact'):
        out+=rng.normal(0,.2,len(t))*np.exp(-t*7)
        out+=np.sin(math.tau*(65*t+35*(1-np.exp(-t*8))))*np.exp(-t*6)*.4
        add(bell(root,.9)*.35,.025);add(bell(root*1.5,.9)*.2,.09)
    elif name in ('spear','white_ray','praise_the_sun','melee_swing','wingbeat','fire_of_light'):
        noise=rng.normal(0,1,len(t))
        smooth=np.convolve(noise,np.ones(11)/11,mode='same')
        out+=smooth*.6*np.sin(math.pi*t/duration)**1.4
        out+=np.sin(math.tau*(root*t+900*t*t))*.1*np.sin(math.pi*t/duration)**2
        if name=='fire_of_light':out+=rng.normal(0,.1,len(t))*np.exp(-t*2)
        else:add(bell(root*3,.55)*.18,.08)
    else:
        descending=name in ('nullify','field_end')
        for j in range(3):add(bell(root*2**((2-j if descending else j)/3),.75)*.3,j*.14)
        out+=np.sin(math.tau*root*.5*t)*np.sin(math.pi*t/duration)**2*.045
    # Short synthetic room tail, DC removal, soft peak limiting and explicit silent endpoints.
    for delay,gain in ((.061,.18),(.113,.11),(.179,.065)):
        offset=int(delay*SR);out[offset:]+=out[:-offset].copy()*gain
    out-=np.mean(out)
    out=np.tanh(out*1.3)
    out*=min(1,.72/max(.001,float(np.max(np.abs(out)))))
    fade=int(SR*.015);out[:fade]*=np.linspace(0,1,fade);out[-fade:]*=np.linspace(1,0,fade)
    return np.asarray(out*32767,dtype='<i2').tobytes()


def generate(assets):
    folder=assets/'sounds/white_angel';folder.mkdir(parents=True,exist_ok=True)
    for i,name in enumerate(ABILITIES+EXTRA_SOUNDS):
        target=folder/(name+'.ogg')
        subprocess.run(['ffmpeg','-v','error','-y','-f','s16le','-ar',str(SR),'-ac','1','-i','pipe:0',
                        '-c:a','libvorbis','-q:a','5','-map_metadata','-1',str(target)],
                       input=synth(name,i),check=True)
    return {f'white_angel.{n}':{'subtitle':f'subtitles.circle_of_inevitability.white_angel.{n}',
            'sounds':[{'name':f'circle_of_inevitability:white_angel/{n}'}]} for n in ABILITIES+EXTRA_SOUNDS}
