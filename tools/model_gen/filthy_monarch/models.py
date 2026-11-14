"""Original cuboid geometry with compatible four-pose forms and two painted atlases."""
import math
import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from modelkit import Model,Texture
from catalog import ABILITY_MODELS,EXTRA_MODELS,ANIMATED
from icons import BLUE,PALE,BLACK,RED,BLOOD,FILTH,EDGE

def material(animated=False):
 tex=Texture('filthy_monarch/'+('abyss_atlas' if animated else 'flesh_atlas'),64,64,frames=4 if animated else 1,frametime=4)
 for i,(name,c) in enumerate(zip(('blue','pale','black','red','blood','filth','edge','bone'),(BLUE,PALE,BLACK,RED,BLOOD,FILTH,EDGE,(185,179,157,255)))):
  def paint(tx,ty,x,y,frame,c=c,n=name):
   vein=(x+int(3*math.sin(y*.4)))%11==0
   shade=8*math.sin(tx*6)-14*ty+(16 if vein else 0)
   if animated:shade+=12*math.sin(frame*math.pi/2+tx*5+ty*4)
   return tuple(max(0,min(255,int(v+shade))) for v in c[:3])+(255,)
  u=i%4*4;v=i//4*8;tex.zone(name,u,v,u+4,v+8,paint)
 return tex

def ring(m,name='ring',radius=7,y=.5,zone='blue'):
 for i in range(16):
  a=i*math.tau/16;x=8+radius*math.cos(a);z=8+radius*math.sin(a)
  length=radius*math.tan(math.pi/16)+.15;degrees=i*22.5;q=math.floor((degrees+45)/90);angle=-degrees+q*90
  dx,dz=(length,.35) if q%2 else (.35,length)
  m.cube(f'{name}_{i}',(x-dx,y,z-dz),(x+dx,y+.5,z+dz),zone,rot=(angle,'y',(x,y,z)))

def flame(m,name='flame',x=8,y=0,z=8,height=16,r=4):
 for i in range(5):
  w=r*(1-i*.16);drift=math.sin(i*1.9)*r*.35
  m.cube(f'{name}_{i}',(x-w+drift,y+i*height/5,z-w),(x+w+drift,y+(i+1)*height/5,z+w),'blue' if i%2==0 else 'pale')
 m.cube(name+'_black_core',(x-r*.45,y,z-r*1.01),(x+r*.45,y+height*.66,z-r*.75),'black')

def humanoid(m,kind,frame):
 pulse=(0,1.2,0,-1.2)[frame];liquid=kind in ('desire_liquid','abyss_form','filth_phantom','shadow_afterimage')
 zone='filth' if kind=='filth_phantom' else 'black';wide=kind=='demon_form'
 m.cube('torso',(4,10,5),(12,22+pulse*.2,11),zone)
 m.cube('head',(5,22,5),(11,28,11),zone)
 for side in (-1,1):
  x=8+side*2.5
  m.cube('leg_'+str(side),(x-1.4,0,6),(x+1.4,10+side*pulse,10),zone)
  x=8+side*6
  m.cube('arm_'+str(side),(x-1.5,10+side*pulse,6),(x+1.5,21+side*pulse,10),zone)
  m.cube('eye_'+str(side),(8+side*1.8-.6,25,4.7),(8+side*1.8+.6,25.7,5.2),'red' if liquid else 'pale')
  if not liquid:
   x=8+side*3
   m.cube('horn_'+str(side),(x-.6,27,7),(x+.6,31,9),'bone',rot=(side*22.5,'z',(x,27,8)))
   for i in range(5 if wide else 4):
    x=8+side*(6+i*3);y=19+pulse*(i+1)*.6
    m.cube(f'wing_{side}_{i}',(x-1.6,y-i*1.1,10+i*.3),(x+1.6,y+4,12+i*.3),'blue')
    m.cube(f'wing_rib_{side}_{i}',(x-.3,y-i*1.1,9.7+i*.3),(x+.3,y+4.4,10.2+i*.3),'black')
   m.cube('claw_'+str(side),(8+side*6-.6,7+side*pulse,6),(8+side*6+.6,11+side*pulse,7),'bone')
  else:
   for i in range(3):
    x=8+side*(3+i*2)
    m.cube(f'tendril_{side}_{i}',(x-.7,1,6+side*pulse),(x+.7,5+i*3+pulse,9+side*pulse),zone)
 if liquid:ring(m,'puddle',8,.1,'filth' if kind=='filth_phantom' else 'black')
 if kind=='abyss_form':
  for i in range(5):
   x=1+i*3.3;m.cube(f'crown_{i}',(x,27,7),(x+.7,29+(i+frame)%3,8),'red')
 if kind=='devil_projection':ring(m,'summon',9,.2,'blue')

def hand(m,frame):
 bend=(0,1.3,2.5,.8)[frame]
 m.cube('wrist',(5,0,6),(11,9,11),'filth');m.cube('palm',(3,8,5),(13,16,11),'filth')
 for i in range(4):
  x=3+i*2.5;h=23-abs(i-1.5)
  m.cube(f'finger_{i}',(x,15,6),(x+1.7,h-bend,8),'filth')
  m.cube(f'tip_{i}',(x,h-bend-1,4-bend),(x+1.7,h-bend+1,6.5),'edge')
 m.cube('thumb',(12,10,4),(16,15,7),'filth');ring(m,'mire',8,0,'black')

def prop(m,n):
 if n in ('claw_slash','grit_burst','blood_spray'):
  for i in range(9 if n!='claw_slash' else 3):
   x=1+i%3*6;y=3+i//3*6
   m.cube('shard_'+str(i),(x,y,6+i%2),(x+1.3,y+(12 if n=='claw_slash' else 3),7.5+i%2),'blood' if n!='grit_burst' else 'filth',rot=(-22.5,'z',(x,y,8)))
 elif n in ('flame_jet','sulfur_fireball','eruption_pillar','hellfire_pillar','abyss_flame_beam'):
  flame(m,height=12 if n=='sulfur_fireball' else 26,r=5 if n=='sulfur_fireball' else 3)
  if n in ('eruption_pillar','hellfire_pillar'):ring(m,'base',8,0,'black')
 elif n=='lava_sword':
  m.cube('grip',(7,0,7),(9,7,9),'black');m.cube('guard',(2,7,6),(14,9,10),'bone')
  m.cube('blade',(5.5,9,7),(10.5,28,9),'blue');m.cube('core',(7,9,6.7),(9,29,7.1),'black')
  m.cube('tip',(6.5,28,7),(9.5,31,9),'pale')
 elif n=='flame_cage':
  ring(m,'bottom',9,0);ring(m,'top',9,24)
  for i in range(8):
   a=i*math.tau/8;flame(m,'bar_'+str(i),8+9*math.cos(a),0,8+9*math.sin(a),24,1)
 elif n in ('toxic_cloud','corrosive_cloud','corrupted_fog'):
  for i in range(9):
   x=2+i%3*5;y=2+(i%2)*3;z=2+i//3*5
   m.cube('cloud_'+str(i),(x-3,y,z-3),(x+4,y+5+(i%3),z+4),'filth' if n=='toxic_cloud' else 'black')
   m.cube('vapor_'+str(i),(x,y+5,z),(x+.5,y+7,z+.5),'edge')
 elif n in ('desire_heart','burning_heart'):
  for i,(x,y,w,h) in enumerate(((3,9,5,7),(8,9,5,7),(4,5,8,5),(6,2,4,4))):m.cube('heart_'+str(i),(x,y,5),(x+w,y+h,11),'blood')
  m.cube('vein',(7.5,4,4.7),(8.5,15,5.2),'red')
  if n=='burning_heart':flame(m,'burn',8,15,8,12,2)
 elif n=='horn_crown':
  ring(m,'rim',6,8,'black')
  for side in (-1,1):m.cube('horn_'+str(side),(8+side*6-.8,8,7),(8+side*6+.8,22,9),'blue',rot=(side*22.5,'z',(8+side*6,8,8)))
 elif n=='symbiosis_link':
  for i in range(10):
   y=1+i*2;x=8+math.sin(i*.7)*2
   m.cube('link_'+str(i),(x-.4,y,7.5),(x+.4,y+1.8,8.5),'red' if i%2 else 'blue')
 elif n in ('depravity_seed','malice_seed','blood_drop'):
  for i in range(5):
   w=(2,3.5,4,2.5,1)[i];m.cube('seed_'+str(i),(8-w,i*3,8-w),(8+w,i*3+3,8+w),'blood' if n=='blood_drop' else 'black')
  m.cube('scar',(7.6,3,3.8),(8.4,12,4.4),'red')
  if n=='malice_seed':ring(m,'ward',7,7,'blue')
 elif n in ('acid_pool','sludge_pool','black_sea','shadow_trail'):
  for i in range(8):
   z=i*2;m.cube('liquid_'+str(i),(-1+i%2,.05,z),(17-i%2,.4+(i%3)*.2,z+2),'black' if n in ('black_sea','shadow_trail') else 'filth')
  ring(m,'edge',8,.5,'blue' if n=='black_sea' else 'edge')
 elif n=='sludge_wave':
  for i in range(9):
   x=-5+i*3;y=5+3*math.sin(i*.7)
   m.cube('wave_'+str(i),(x,0,5),(x+3,y,10),'filth');m.cube('crest_'+str(i),(x,y,3),(x+3,y+2,8),'edge')
 elif n in ('corrosion_beam','barrier_fracture'):
  if n=='corrosion_beam':
   m.cube('beam',(7,0,7),(9,32,9),'filth');m.cube('core',(7.7,0,6.8),(8.3,32,7.2),'edge')
  else:
   for i in range(6):m.cube('fracture_'+str(i),(4+i%2*4,i*4,7),(6+i%2*4,i*4+4,8),'edge')
 elif n=='kingdom_tile':
  # Open lattice, so the kingdom is readable from inside and outside.
  for x in (0,7.5,15):m.cube('rib_'+str(x),(x,0,7.8),(x+.5,16,8.2),'black')
  for y in (0,7.5,15):m.cube('cross_'+str(y),(0,y,7.7),(16,y+.5,8.3),'blue')
 elif n.startswith('word_'):
  variant=['slow','die','confusion','sloth','blasphemy','blather'].index(n[5:])
  for i in range(4+variant):
   x=2+(i*3)%12;y=3+(i*5)%17
   m.cube('glyph_'+str(i),(x,y,7),(x+1,y+3,8),'red' if i%2 else 'blue')
  ring(m,'voice',8,1,'blood')
 elif n=='danger_marker':
  m.cube('stem',(7,5,7),(9,17,9),'red');m.cube('dot',(7,0,7),(9,2,9),'pale')
 elif n=='shadow_afterimage':humanoid(m,n,0)
 else:
  # Radial symbols have distinct spoke counts and two contrasting material bands.
  variants=['sink_seal','bloodlust_crown','ritual_sigil','warning_ring','mind_wave','offering_sigil','rebirth_ring','corrosion_mark']
  index=variants.index(n);ring(m,'outer',8,0,'red' if index%2 else 'blue');ring(m,'inner',5,0,'black')
  for i in range(3+index):
   a=i*math.tau/(3+index);x=8+5*math.cos(a);z=8+5*math.sin(a)
   m.cube('sigil_'+str(i),(x-.4,.5,z-.4),(x+.4,2+(index%3),z+.4),'blood' if index%2 else 'pale')

def generate(root):
 solid,animated=material(),material(True);solid.save(root);animated.save(root);records=[]
 names=list(dict.fromkeys(sum(ABILITY_MODELS.values(),[])+EXTRA_MODELS))
 def make(name,base,frame=0):
  m=Model('filthy_monarch/'+name,animated if base not in ('grit_burst','blood_spray','blood_drop') else solid,'Original Filthy Monarch geometry')
  if base=='filth_hand':hand(m,frame)
  elif base in ANIMATED:humanoid(m,base,frame)
  else:prop(m,base)
  m.save(root);records.append({'name':name,'elements':len(m.elements),'pivot_pixels':[8,0,8],
   'bounds':[[min(e['from'][i] for e in m.elements) for i in range(3)],[max(e['to'][i] for e in m.elements) for i in range(3)]]})
 for n in names:
  make(n,n)
  if n in ANIMATED:
   for f in range(4):make(f'{n}_{f}',n,f)
 return records
