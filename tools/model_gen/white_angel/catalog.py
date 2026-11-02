"""White Angel resource IDs. Wheel entry IDs match the attached design draft."""
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
ASSETS = REPO / 'src/main/resources/assets/circle_of_inevitability'
PREVIEW = REPO / 'docs/assets/white_angel'
NS = 'circle_of_inevitability'
PREFIX = 'white_angel'
BASIC = 'hymn_of_valor hymn_of_renewal sunshine daytime'.split()
WHEELS = {
    'theurgy': 'holy_light sun_halo holy_oath cleave fire_of_light'.split(),
    'notary': 'contract authenticate amplify nullify'.split(),
    'radiance': 'purification domain flaring_sun spear white_ray holy_equipment'.split(),
    'justice': 'define_justice justice_halo judgment faith_appraisal'.split(),
    'sun': 'solar_envoy praise_the_sun holy_kingdom servant_of_faith pure_white_light sun'.split(),
}
ABILITIES = BASIC + sum(WHEELS.values(), [])
KEYS = 'hymn light theurgy notarization radiance justice sun'.split()
UPGRADES = 'light_of_holiness purification_halo solar_oath holy_contract spear_of_light justice_trial'.split()
PRINCIPLES = 'mercy retribution purity order'.split()
STATUSES = ('pacts believers blessing courage oath fervor guilt condemned appraised amplified '
            'holy_equipment sunlight night_vision evil_detection horror_immunity fortune nemesis '
            'purified_body holy_eye spiritual_intuition light_body incarnation_of_order '
            'inextinguishable_light anchored pact_breach').split()
PARTICLES = ('note_0 note_1 sun_mote golden_flame feather seal oath_glyph eye '
             'ray_streak light_crescent purify_spark guilt_mark white_ring').split()
EXTRA_SOUNDS = ('warning countdown pact_breach believer_convert intuition wingbeat '
                'field_end inextinguishable_light melee_swing melee_impact').split()
NAMES = {name: name.replace('_', ' ').title() for name in ABILITIES + KEYS + UPGRADES + PRINCIPLES + STATUSES}
NAMES.update(holy_light='Holy Light Summoning', cleave='Cleave of Purification',
             domain='Unshadowed Domain', spear='Unshadowed Spear', white_ray='Pure White Ray',
             judgment='Judgment of Justice')

# Every ability maps to an authored visual, even when its final placement uses shared kit primitives.
ABILITY_MODELS = {
    'hymn_of_valor': ['hymn_note'], 'hymn_of_renewal': ['hymn_note'],
    'sunshine': ['sun'], 'daytime': ['sun_halo'], 'holy_light': ['blade_of_light', 'warning_sigil'],
    'sun_halo': ['sun_halo'], 'holy_oath': ['oath_sigil'], 'cleave': ['blade_of_light'],
    'fire_of_light': ['flame_plume'], 'contract': ['contract_scroll'],
    'authenticate': ['holy_eye'], 'amplify': ['notary_seal'], 'nullify': ['notary_seal'],
    'purification': ['purification_ring'], 'domain': ['domain_boundary'],
    'flaring_sun': ['sun', 'warning_sigil', 'flame_plume'], 'spear': ['spear_of_light'],
    'white_ray': ['ray_core'], 'holy_equipment': ['holy_armor', 'holy_mace'],
    'define_justice': ['golden_scales'], 'justice_halo': ['justice_halo'],
    'judgment': ['golden_scales', 'blade_of_light'], 'faith_appraisal': ['holy_eye'],
    'solar_envoy': ['sunbird'], 'praise_the_sun': ['ray_core'],
    'holy_kingdom': ['kingdom_gate', 'domain_boundary'], 'servant_of_faith': ['believer_sun'],
    'pure_white_light': ['purification_ring', 'warning_sigil'], 'sun': ['sun'],
}
