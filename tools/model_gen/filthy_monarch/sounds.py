"""Original deterministic synthesis; mono Vorbis. Foul Tongue uses synthetic formants, not recorded speech."""
import math
import subprocess
import numpy as np
from catalog import SOUNDS,NAMES,WHEELS
SR=44100
SUBTITLES={n:NAMES[n]+' sounds' for n in SOUNDS}
SUBTITLES.update(instinct='An unseen threat stirs',danger_premonition='Malice rings nearby',seed_plant='A seed of depravity takes root',seed_spend='Depravity shatters',offering_gain='An offering is claimed',single_drop='Blood gathers into a body',malice_rebirth='Malice refuses death',wingbeat='Sulfur wings beat',barrier_crack='A barrier corrodes',field_end='The filth subsides')
FIRE=set('sulfur_flame sulfur_fireball sword_of_lava flame_cage volcanic_eruption hellfire flames_of_the_abyss fireball_impact cage_close eruption_warning'.split())
BLOOD=set('blood_ignition blood_burst seed_of_malice single_drop malice_rebirth dissolve reform'.split())
MAJOR=set('devil_worship devil_form demon_form abyssal_sea the_abyss tide_of_filth flames_of_the_abyss'.split())

def synth(name,index):
 duration=2.6 if name in MAJOR else (.65 if name in ('instinct','danger_premonition','melee_swing','melee_impact','seed_plant','offering_gain') else 1.5)
 t=np.arange(int(SR*duration))/SR;rng=np.random.default_rng(151500+index);white=rng.normal(0,1,len(t))
 low=np.convolve(white,np.ones(141)/141,mode='same');hiss=white-np.convolve(white,np.ones(13)/13,mode='same')
 base=62+index*1.13;out=np.zeros(len(t))
 def tone(freq,at=0,gain=.25,decay=6):
  tt=np.maximum(0,t-at);return gain*(t>=at)*np.minimum(1,tt/.008)*np.exp(-tt*decay)*np.sin(math.tau*freq*tt)
 def thud(at=0,gain=.3):
  tt=np.maximum(0,t-at);return gain*(t>=at)*np.minimum(1,tt/.005)*np.exp(-tt*11)*(np.sin(math.tau*(48*tt+4*(1-np.exp(-tt*25))))+low*2)
 if name in WHEELS['foul_tongue']:
  # Seven distinct voiced/formant patterns and syllable rhythms evoke a forbidden language.
  voice=np.zeros(len(t));fundamental=70+index%7*5
  for harmonic in range(1,35):
   f=fundamental*harmonic
   gain=sum(np.exp(-((f-center)/width)**2) for center,width in ((380+index%4*90,130),(1000+index%5*120,220),(2300,300)))
   voice+=gain/max(1,harmonic)*np.sin(math.tau*f*t+.12*np.sin(37*t))
  syllables=np.zeros(len(t))
  for j in range(2+index%3):syllables+=np.exp(-((t-(.18+j*.25))/(.09+.02*(j%2)))**2)
  out+=voice*syllables*2+hiss*.035*syllables+thud(.1,.24)
 elif name in FIRE:
  env=np.exp(-t*(2 if name=='flames_of_the_abyss' else 5))*np.minimum(1,t/.04)
  out+=hiss*.18*env+low*2.6*env+thud(.06,.38)
  out+=.16*np.sin(math.tau*(160*t-32*t*t))*env
  for j in range(7):out+=tone(900+j*137,.05+j*.07,.045,50)
 elif name in BLOOD or name in ('acid_swamp','tide_of_filth','toxic_vapor','corrosive_fog','desire_incarnation'):
  for j in range(5):
   tt=np.maximum(0,t-.08-j*.16);env=(t>=.08+j*.16)*np.exp(-tt*18)
   out+=.17*np.sin(math.tau*(170*tt+7*np.sin(19*tt)))*env+low*env*.7
  out+=thud(.04,.4)+hiss*.045*np.exp(-t*3)
 elif name in ('instinct','danger_premonition','offering_gain','seed_plant','seed_spend'):
  for j,ratio in enumerate((1,1.41,2.13)):out+=tone((520+index*7)*ratio,.03+j*.09,.18,8)
 elif name in ('lawless_lunge','killing_spree','dirty_trick','melee_swing','melee_impact','projection_swipe','wingbeat'):
  for j in range(3 if name=='killing_spree' else 1):
   out+=hiss*.25*np.exp(-((t-.16-j*.24)/.055)**2)+thud(.23+j*.24,.4)
 elif name in MAJOR:
  env=np.sin(math.pi*t/duration)**2
  out+=low*2*env
  for ratio in (1,1.04,1.5,2.01):out+=.13*np.sin(math.tau*base*ratio*t)*env
  out+=thud(.1,.4)
 else:
  env=np.sin(math.pi*t/duration)**2
  out+=.24*np.sin(math.tau*(base*t+18*t*t))*env+low*1.5*env
  out+=tone(base*3,.08,.19,4)+tone(base*4.17,.2,.12,5)+thud(.03,.2)
 for delay,gain in ((.073,.16),(.167,.09)):
  n=int(SR*delay);out[n:]+=out[:-n].copy()*gain
 out-=out.mean();out*=.62/max(.001,np.max(np.abs(out)))
 fade=int(.025*SR);out[:fade]*=np.linspace(0,1,fade);out[-fade:]*=np.linspace(1,0,fade)
 return np.asarray(out*32767,dtype='<i2').tobytes()

def generate(root):
 folder=root/'sounds/filthy_monarch';folder.mkdir(parents=True,exist_ok=True)
 for i,n in enumerate(SOUNDS):
  subprocess.run(['ffmpeg','-v','error','-y','-f','s16le','-ar',str(SR),'-ac','1','-i','pipe:0','-c:a','libvorbis','-q:a','5','-map_metadata','-1',str(folder/(n+'.ogg'))],input=synth(n,i),check=True)
 return {'filthy_monarch.'+n:{'subtitle':'subtitles.circle_of_inevitability.filthy_monarch.'+n,'sounds':[{'name':'circle_of_inevitability:filthy_monarch/'+n}]} for n in SOUNDS}
