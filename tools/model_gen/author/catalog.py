"""Spectator / Author design and asset IDs; no gameplay registration is implied."""
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
ASSETS = REPO / 'src/main/resources/assets/circle_of_inevitability'
PREVIEW = REPO / 'docs/assets/author'
NS = 'circle_of_inevitability'
BASIC = 'observe spectator_state awe frenzy psychoanalysis placate'.split()
WHEELS = {
    'hypnosis': 'suggestion sleep psychological_invisibility battle_hypnosis'.split(),
    'dreams': 'dream_entry nightmare dream_maze dream_anchor'.split(),
    'mind': 'dragon_form mind_storm manipulate virtual_persona mind_walk discern'.split(),
    'authorship': 'foreshadow plot_twist fate_script envision life_creation sea_of_minds'.split(),
}
ABILITIES = BASIC + sum(WHEELS.values(), [])
KEYS = 'observation emotions therapy hypnosis dreams mind authorship'.split()
STATUSES = 'insight exposed suggestion sleep fear frenzy lucidity strain persona scripted resistance'.split()
PARTICLES = 'eye ripple scale thought thread ink page dream_shard spiral quill_star'.split()
EXTRA_SOUNDS = 'insight_gain insight_spent resist wake warning wingbeat script_trigger field_end'.split()
SOUNDS = ABILITIES + EXTRA_SOUNDS
NAMES = {n: n.replace('_', ' ').title() for n in ABILITIES + KEYS + STATUSES}
NAMES.update(dragon_form='Mind Dragon', discern='Discerner’s Gaze', sea_of_minds='Sea of Collective Subconscious')
SEQUENCES = dict(zip(ABILITIES, [9,9,8,8,7,7,6,6,6,6,5,5,5,5,4,4,4,4,3,2,1,1,1,0,0,0]))
ABILITY_MODELS = {
    'observe':['mind_eye'], 'spectator_state':['observer_mask'],
    'awe':['terror_eye','mind_ripple'], 'frenzy':['fractured_mask'],
    'psychoanalysis':['mind_eye','thought_shard'], 'placate':['lotus','mind_ripple'],
    'suggestion':['pendulum'], 'sleep':['dream_lantern'],
    'psychological_invisibility':['observer_mask'], 'battle_hypnosis':['pendulum','thought_cage'],
    'dream_entry':['dream_gate'], 'nightmare':['terror_eye','dream_shard'],
    'dream_maze':['thought_cage','dream_gate'], 'dream_anchor':['dream_lantern','lotus'],
    'dragon_form':['mind_dragon','mind_dragon_incomplete'], 'mind_storm':['mind_ripple','thought_shard'],
    'manipulate':['mind_crown','thought_thread'], 'virtual_persona':['persona'],
    'mind_walk':['dream_gate','thought_thread'], 'discern':['mind_eye','mind_crown'],
    'foreshadow':['open_book','quill'], 'plot_twist':['quill','dream_shard'],
    'fate_script':['open_book','script_seal'], 'envision':['envisioned_blade','thought_shard'],
    'life_creation':['imagined_drake','script_seal'], 'sea_of_minds':['sea_arch','mind_ripple'],
}
