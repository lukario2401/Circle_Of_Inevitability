"""Native pixel silhouettes, transparent HUD sprites and tintable FX masks."""
import math
from PIL import Image, ImageDraw
from catalog import ABILITIES,KEYS,STATUSES,PARTICLES
BLUE=(127,216,255,255);PALE=(230,250,255,255);BLACK=(18,10,14,255)
RED=(200,8,21,255);BLOOD=(138,15,26,255);FILTH=(92,90,30,255);EDGE=(172,169,80,255)

def icon(name,size=32):
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 def line(p,c=BLUE,w=2):d.line(p,fill=c,width=w)
 def ring(c=BLUE):d.ellipse((3,8,28,27),outline=c,width=2)
 def flame(x=16,y=16,r=10):
  d.polygon([(x,y-r-3),(x+2,y-3),(x+7,y-7),(x+r,y+5),(x+5,y+r),(x-5,y+r),(x-r,y+4),(x-6,y-6),(x-4,y)],fill=BLUE,outline=PALE)
  d.polygon([(x,y-4),(x+4,y+7),(x,y+10),(x-4,y+5)],fill=BLACK)
 def drop(c=BLOOD):d.polygon([(16,3),(25,18),(25,24),(20,28),(12,28),(7,24),(7,18)],fill=c,outline=RED if c==BLOOD else EDGE)
 def horns():
  d.polygon([(6,3),(5,13),(10,19),(22,19),(27,13),(26,3),(22,11),(20,8),(12,8),(10,11)],fill=BLACK,outline=BLUE)
  line([(10,20),(16,28),(22,20)],RED);line([(10,14),(13,15)],PALE);line([(19,15),(22,14)],PALE)
 def seed():
  d.polygon([(16,4),(23,14),(21,22),(16,27),(11,22),(9,14)],fill=BLACK,outline=RED)
  line([(16,8),(13,15),(19,19),(16,24)],BLUE,1)
 def wave(c=FILTH):
  for y in (10,16,22):line([(3,y+3),(8,y),(14,y+4),(20,y),(28,y+3)],c,3)
 def heart():
  d.polygon([(16,9),(11,5),(5,8),(4,15),(16,28),(28,15),(27,8),(21,5)],fill=BLOOD,outline=RED)
  line([(16,10),(13,16),(18,18),(16,25)],PALE,1)
 def blade():
  d.polygon([(23,3),(27,4),(24,12),(12,24),(8,20)],fill=BLUE,outline=PALE)
  line([(5,18),(15,27)],BLOOD,3);line([(8,23),(3,29)],EDGE,3)
 aliases=dict(zip(KEYS,['lawless_lunge','sulfur_flame','killing_spree','devil_form','desire_control','blasphemy','tide_of_filth']))
 name=aliases.get(name,name)
 if name in ('lawless_lunge','sword_of_lava','killing_spree','claw'):
  if name=='killing_spree':
   for x in (4,12,20):line([(x+7,5),(x,25)],BLOOD,3)
  else:blade()
  if name=='lawless_lunge':line([(3,10),(10,10),(7,6)],RED)
  if name=='sword_of_lava':flame(23,19,4)
 elif name in ('sulfur_flame','sulfur_fireball','hellfire','flames_of_the_abyss','black_ember','spark'):
  flame()
  if name=='sulfur_fireball':d.ellipse((4,5,28,29),outline=BLUE);line([(1,2),(7,8)],PALE)
  if name=='hellfire':ring(BLOOD)
  if name=='flames_of_the_abyss':line([(3,29),(26,3)],PALE,3)
  if name=='black_ember':d.rectangle((11,14,20,24),fill=BLACK)
  if name=='spark':line([(2,16),(29,16)],PALE,1)
 elif name in ('devil_form','demon_form','mental_shock','horn','danger_premonition','danger_eye','interference'):
  horns()
  if name in ('mental_shock','danger_premonition','danger_eye'):d.arc((1,1,30,30),190,350,fill=RED,width=2)
  if name=='interference':line([(2,29),(29,2)],RED,3)
  if name=='demon_form':ring(RED)
 elif name in ('devil_worship','offerings','offering','bloodlust'):
  ring(RED);seed()
  for x in (4,16,28):line([(x,4),(x,8)],BLUE,2)
  if name=='devil_worship':horns()
  if name=='bloodlust':line([(2,3),(6,8),(4,13)],BLOOD,3)
 elif name in ('depravity','seed','seed_of_malice','malice_ward'):
  seed()
  if name!='seed':ring(RED if name=='depravity' else BLUE)
  if name=='seed_of_malice':line([(3,2),(9,7),(5,13)],RED)
  if name=='malice_ward':line([(23,2),(29,7),(25,13)],BLUE)
 elif name in ('desire_control','desire_symbiosis','symbiosis','blood_ignition'):
  heart()
  if name=='desire_control':line([(3,3),(16,3),(29,3)],BLUE);line([(16,3),(16,9)],BLUE)
  elif name in ('desire_symbiosis','symbiosis'):d.ellipse((1,12,13,24),outline=BLUE,width=2);d.ellipse((19,12,31,24),outline=BLUE,width=2)
  else:flame(16,12,5)
 elif name in ('blood_burst','blood_drop','bleeding','single_drop'):
  drop()
  if name=='blood_burst':
   for x,y in ((3,4),(28,5),(3,26),(28,27)):line([(x,y),(x,y+3)],RED)
  if name=='bleeding':line([(3,3),(28,10)],PALE,2)
  if name=='single_drop':ring(BLUE)
 elif name in ('shadow_shift','desire_incarnation','filthy_illusion','the_abyss','shadow'):
  d.ellipse((12,4,20,12),fill=BLACK,outline=BLUE)
  d.polygon([(12,13),(20,13),(27,28),(19,25),(16,29),(12,25),(5,28)],fill=BLACK,outline=FILTH)
  if name=='shadow_shift':line([(2,18),(10,18),(7,15)],BLUE)
  if name=='desire_incarnation':wave()
  if name=='filthy_illusion':line([(4,5),(2,16),(6,23)],EDGE);line([(27,5),(30,16),(26,23)],EDGE)
  if name=='the_abyss':horns();ring(BLOOD)
 elif name in ('flame_cage','sink','slow'):
  ring(BLUE)
  for x in (7,15,23):line([(x,6),(x,24)],BLUE)
  if name=='sink':line([(16,2),(16,20),(12,16)],RED);line([(16,20),(20,16)],RED)
  if name=='slow':d.rectangle((11,12,20,19),fill=BLACK);line([(16,12),(16,16),(19,16)],PALE,1)
 elif name=='volcanic_eruption':
  d.polygon([(3,27),(11,15),(21,15),(29,27)],fill=BLACK,outline=FILTH);flame(16,10,6)
 elif name in ('acid_swamp','tide_of_filth','abyssal_sea','sludge','king_of_filth'):
  wave(BLUE if name=='abyssal_sea' else FILTH)
  if name=='acid_swamp':d.ellipse((3,22,29,29),outline=EDGE);drop(FILTH)
  if name=='tide_of_filth':line([(9,25),(9,10),(5,7)],EDGE);line([(9,13),(13,6)],EDGE)
  if name=='abyssal_sea':d.arc((2,1,30,29),180,360,fill=PALE,width=2)
  if name=='king_of_filth':line([(4,3),(9,8),(16,2),(23,8),(28,3)],RED,3)
 elif name in ('toxic_vapor','corruption','fog','corrosive_fog'):
  for x,y in ((9,18),(16,10),(23,18)):d.ellipse((x-6,y-6,x+6,y+6),fill=BLACK,outline=FILTH)
  if name=='corruption':seed()
  if name=='toxic_vapor':line([(9,24),(9,29)],EDGE);line([(23,24),(23,29)],EDGE)
  if name=='corrosive_fog':line([(5,7),(26,25)],EDGE)
 elif name in ('the_corroder','corrosion','corrosion_streak'):
  line([(2,28),(29,2)],FILTH,5);line([(3,27),(28,2)],EDGE,1)
  if name!='corrosion_streak':line([(6,7),(15,15),(12,21),(24,29)],BLUE,2)
  if name=='corrosion':d.rectangle((5,5,26,26),outline=BLOOD)
 elif name in ('die','confusion','sloth','blasphemy','blather','foul_glyph','silenced','fear'):
  # Distinct, nonlinguistic Devil-language sigils; no real religious glyphs.
  variant=['die','confusion','sloth','blasphemy','blather','foul_glyph','silenced','fear'].index(name)
  ring(BLOOD)
  if variant==0:line([(8,7),(24,25)],PALE);line([(24,7),(8,25)],RED);heart()
  elif variant==1:
   for r in (10,6,2):d.arc((16-r,16-r,16+r,16+r),25,305,fill=BLUE,width=2)
  elif variant==2:line([(7,7),(24,7),(10,24),(24,24)],EDGE,3)
  elif variant==3:line([(8,5),(8,24),(24,16),(8,9)],RED,3);line([(23,3),(16,28)],BLUE)
  elif variant==4:
   for x in (6,12,18,24):line([(x,8+x%5),(x,24-x%5)],BLUE)
  elif variant==5:line([(6,7),(25,7),(16,25),(6,7)],PALE)
  elif variant==6:line([(5,16),(27,16)],PALE,3);line([(7,7),(25,25)],RED)
  else:horns();line([(12,26),(16,22),(20,26)],PALE)
 elif name=='dirty_trick':
  for x,y in ((4,8),(13,5),(23,8),(7,18),(20,19),(27,25),(12,27)):d.rectangle((x,y,x+3,y+3),fill=FILTH if x%2 else BLOOD)
  line([(2,28),(14,18)],PALE)
 elif name=='ripple':ring();d.ellipse((9,13,23,23),outline=PALE)
 else:raise ValueError('Missing art: '+name)
 return im if size==32 else im.resize((size,size),Image.Resampling.NEAREST)

def save(im,path):path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
def generate(root):
 for n in dict.fromkeys(ABILITIES+KEYS):save(icon(n),root/f'textures/gui/ability_menus/filthy_monarch/{n}.png')
 for n in dict.fromkeys(ABILITIES+STATUSES):save(icon(n,16),root/f'textures/gui/filthy_monarch/{n}.png')
 for counter in ('depravity','offerings'):
  for count in range(6):
   im=Image.new('RGBA',(32,8));d=ImageDraw.Draw(im)
   for i in range(5):d.polygon([(3+i*6,1),(5+i*6,4),(3+i*6,6),(1+i*6,4)],fill=(RED if counter=='offerings' else BLUE) if i<count else BLACK,outline=BLOOD)
   save(im,root/f'textures/gui/filthy_monarch/{counter}_{count}.png')
 save(icon('devil_form',16),root/'textures/gui/pathway_icons/filthy_monarch.png')
 for n in PARTICLES:
  im=icon(n,16);alpha=im.getchannel('A');im=im.convert('L').convert('RGBA');im.putalpha(alpha)
  save(im,root/f'textures/particle/fx/filthy_monarch/{n}.png')
