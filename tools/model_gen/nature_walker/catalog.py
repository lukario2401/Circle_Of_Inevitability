"""Stable presentation IDs for the supplied Nature Walker design; no balance decisions."""
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
ASSETS = REPO / 'src/main/resources/assets/circle_of_inevitability'
PREVIEW = REPO / 'docs/pathways/nature_walker/assets'
NS = 'circle_of_inevitability'
BASIC = 'thorn_seed heartfruit_seed treatment soul_suture cross_breed toxin_bloom'.split()
WHEELS = {
    'harvest': 'seed_catalyzation quicken_life command_flora weather_ritual reap blossoming_land barren_land great_harvest'.split(),
    'earth': 'underground_travel swamp wrath_of_nature child_of_the_oak stone_golem bear_form verdant_barrier world_of_nature earthquake'.split(),
    'alchemy': 'stone_bird mud_golem steel_golem life_aura mutate brood_form seed_of_rebirth awaken'.split(),
    'return': 'evil_spirits descent_of_death return_to_earth maternal_embrace desolation root_eruption embrace_of_the_earth bountiful_earth'.split(),
}
ABILITIES = BASIC + sum(WHEELS.values(), [])
KEYS = 'sow treatment harvest biology earth alchemy return'.split()
UPGRADES = 'nature_walk brood_form_complete given_life weather_clear weather_storm'.split()
STATUSES = ('growths brood_constructs brood_hybrids brood_spirits verdancy reaped rebirth_ward '
            'mutation_organs mutation_fungus mutation_wheat mutation_vines mutagen madness '
            'life_sense grave_sight weather_forecast nourished_by_earth mothers_domain').split()
PARTICLES = ('petal spore pollen soil_crumb green_ring brown_ring leaf thorn root_splinter '
             'soul_thread healing_spark rain_drop wheat_grain grave_mote mutation_cell').split()
EXTRA_SOUNDS = ('growth_sprout growth_bloom growth_wither heartfruit_eat cross_pollinate mutation_roll '
                'mutation_bloom rebirth_trigger rebirth_bloom warning root_impact construct_step '
                'bird_wingbeat melee_swing melee_impact field_end weather_clear weather_storm '
                'nature_walk brood_form_complete').split()
SOUNDS = ABILITIES + EXTRA_SOUNDS
NAMES = {n:n.replace('_', ' ').title() for n in ABILITIES + KEYS + UPGRADES + STATUSES}
NAMES.update(cross_breed='Cross-breed', child_of_the_oak='Child of the Oak', return_to_earth='Return to Earth',
             embrace_of_the_earth='Embrace of the Earth', brood_form='Brood Form (partial)',
             brood_form_complete='Brood Form (complete)', mothers_domain="Mother’s Domain")
SEQUENCES = dict(zip(ABILITIES, [9,9,8,8,6,6, 7,7,7,7,7,2,2,0, 5,5,5,5,5,5,1,1,0,
                               4,4,4,4,4,4,2,0, 3,3,3,3,2,2,0,0]))
ABILITY_MODELS = dict(zip(ABILITIES, [
    ['seed','thornling_bloom'], ['seed','heartfruit_sprout','heartfruit_ripe'],
    ['healing_blossom'], ['soul_suture'], ['hybrid_crown'], ['sporebed_bloom'],
    ['vine_burst'], ['life_ring'], ['flora_sigil'], ['rain_cloud','sun_disc','storm_cloud'],
    ['harvest_scythe'], ['heartfruit_bloom','life_ring'], ['wither_ring'], ['harvest_scythe','harvest_crown'],
    ['earth_portal','nature_portal'], ['mire_disc'], ['vine_burst','falling_stone','undertow','lightning'],
    ['bark_skin_wood','bark_skin_stone'], ['stone_golem'], ['bear_form'], ['verdant_barrier'],
    ['world_of_nature_tile'], ['rift'], ['stone_bird'], ['mud_golem'], ['steel_golem'], ['life_ring'],
    ['mutation_organs','mutation_fungus','mutation_wheat','mutation_vines'],
    ['brood_form','brood_form_complete'], ['rebirth_seed'], ['mother_tree_ripe','awaken_sigil'],
    ['evil_spirit','grave_mark'], ['death_canopy'], ['root_burst'], ['coffin'], ['wither_ring'],
    ['root_eruption','warning_ring'], ['earth_lance'], ['bountiful_earth_tile'],
]))
ANIMATED = 'stone_bird stone_golem mud_golem steel_golem bear_form brood_form brood_form_complete evil_spirit'.split()
GROWTHS = {kind: ['seed','sprout','bloom'] + (['ripe'] if kind in ('heartfruit','mother_tree') else []) + ['withered']
           for kind in ('thornling','heartfruit','briar_orchard','sporebed','mother_tree')}
