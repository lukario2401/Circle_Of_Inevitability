"""Asset contract for the supplied Filthy Monarch design; no gameplay registration."""
from pathlib import Path
REPO=Path(__file__).resolve().parents[3]
ASSETS=REPO/'src/main/resources/assets/circle_of_inevitability'
PREVIEW=REPO/'docs/pathways/filthy_monarch/assets'
NS='circle_of_inevitability'
BASIC='lawless_lunge dirty_trick sulfur_flame sink killing_spree devil_worship'.split()
WHEELS={
 'devil_arts':'sulfur_fireball devil_form sword_of_lava flame_cage volcanic_eruption toxic_vapor hellfire flames_of_the_abyss'.split(),
 'desire_and_malice':'shadow_shift desire_control mental_shock desire_incarnation desire_symbiosis filthy_illusion seed_of_malice'.split(),
 'foul_tongue':'slow die corruption confusion sloth blasphemy blather'.split(),
 'blood_and_filth':'acid_swamp blood_ignition blood_burst tide_of_filth the_corroder abyssal_sea the_abyss'.split()}
ABILITIES=BASIC+sum(WHEELS.values(),[])
KEYS=['criminals_kit','demonic_attribute','serial_killer']+list(WHEELS)
STATUSES='depravity offerings danger_premonition bloodlust bleeding corrosion fear silenced symbiosis malice_ward single_drop king_of_filth interference'.split()
PARTICLES='sulfur_flame black_ember blood_drop sludge fog seed claw spark ripple foul_glyph shadow corrosion_streak danger_eye offering horn'.split()
ANIMATED='devil_form demon_form devil_projection desire_liquid filth_phantom abyss_form filth_hand'.split()
EXTRA_SOUNDS='demon_form corrosive_fog seed_plant seed_spend offering_gain instinct danger_premonition wingbeat projection_swipe fireball_impact cage_close eruption_warning illusion_burst link_break malice_rebirth single_drop dissolve reform barrier_crack kingdom_close form_end field_end melee_swing melee_impact'.split()
SOUNDS=ABILITIES+EXTRA_SOUNDS
NAMES={n:n.replace('_',' ').title() for n in dict.fromkeys(ABILITIES+KEYS+STATUSES+SOUNDS)}
NAMES.update(criminals_kit="Criminal's Kit",shadow_shift='Shadow-Shift',the_corroder='The Corroder',the_abyss='The Abyss')
SEQUENCES=dict(zip(ABILITIES,[9,9,8,8,7,7,6,6,6,6,6,6,4,1,5,5,5,5,5,4,2,6,6,6,4,4,4,3,4,2,2,1,1,0,0]))
ABILITY_MODELS=dict(zip(ABILITIES,[
 ['claw_slash'],['grit_burst'],['flame_jet'],['sink_seal'],['claw_slash','bloodlust_crown'],['ritual_sigil','devil_projection'],
 ['sulfur_fireball'],['devil_form','demon_form'],['lava_sword'],['flame_cage'],['warning_ring','eruption_pillar'],['toxic_cloud','corrosive_cloud'],['hellfire_pillar'],['abyss_flame_beam'],
 ['shadow_trail','shadow_afterimage'],['desire_heart'],['horn_crown','mind_wave'],['desire_liquid'],['symbiosis_link'],['filth_phantom'],['malice_seed'],
 ['word_slow'],['word_die','blood_spray'],['corrupted_fog'],['word_confusion'],['word_sloth'],['word_blasphemy'],['word_blather'],
 ['acid_pool'],['burning_heart'],['blood_spray'],['sludge_wave','filth_hand','sludge_pool'],['corrosion_beam','barrier_fracture'],['kingdom_tile','black_sea'],['abyss_form']]))
EXTRA_MODELS='depravity_seed offering_sigil danger_marker blood_drop rebirth_ring corrosion_mark'.split()
