# White Angel assets

This collection covers all 29 abilities in the supplied `WHITE_ANGEL.md` design draft: the four plain-key
casts and five wheels containing 5, 4, 6, 4 and 6 entries. Gold, pure white, solar fire and feather motifs
carry Light, Order and Faith through the collection. The design's wheel IDs are preserved.

The collection provides resources and registers their sound events. [All 29 abilities](WHITE_ANGEL.md) use
these resources through `WhiteAngelFx`, with `/whitefx` previews and Pact/Believer HUD counters.
Gameplay costs, consent and passives live in the Java implementation.

## Inspect the assets

- [Ability, key, upgrade and principle icons](assets/white_angel/icons.png)
- [Models rendered from their actual geometry and UVs](assets/white_angel/models.png)
- [Complete/incomplete Sunbird animation and Sun turntable](assets/white_angel/sunbird.gif)
- [HUD and particle sheet](assets/white_angel/hud_particles.png)

The previews use a software renderer. In-game brightness, size, placement, armor fit, animation speed
and audio balance still need a client review. Model contact sheets scale each prop independently.

## Resource inventory

All paths below are relative to `src/main/resources/assets/circle_of_inevitability/`.
The complete resource graph, per-ability model mapping, cube bounds, recommended pivots and SHA-256
hashes are in [the asset manifest](../src/main/resources/assets/circle_of_inevitability/white_angel/asset_manifest.json).

| Resource | Location and use |
| --- | --- |
| 29 ability icons | `textures/gui/ability_menus/white_angel/<ability>.png`, 32 x 32 RGBA |
| Seven key icons | Same directory: `hymn`, `light`, `theurgy`, `notarization`, `radiance`, `justice`, `sun`; Sun also serves its ability |
| Six upgrade icons | `light_of_holiness`, `purification_halo`, `solar_oath`, `holy_contract`, `spear_of_light`, `justice_trial` |
| Four principle icons | Same directory: `principle_mercy`, `principle_retribution`, `principle_purity`, `principle_order` |
| HUD icons | `textures/gui/white_angel/`, 16 x 16 RGBA; abilities, upgrades, principles, passives and status/counter symbols |
| Guilt levels | `textures/gui/white_angel/guilt_0.png` through `guilt_5.png`; five pips show the level without baked text |
| Pathway icon | `textures/gui/pathway_icons/white_angel.png`, 16 x 16 Sunbird |
| 33 models | `models/model_fx/white_angel/`, Java Block/Item cube JSON consumed by ModelFx |
| Materials | `textures/model_fx/white_angel/divine_atlas.png` and animated `sunfire_atlas.png` plus `.mcmeta` |
| 13 particle sprites | `textures/particle/fx/white_angel/`, 16 x 16 grayscale with transparent backgrounds for runtime tint |
| 39 sounds | `sounds/white_angel/`, original synthesized mono 44.1 kHz Vorbis |
| Localizations | White Angel ability/key names, HUD format strings and 39 subtitles in `lang/en_us.json` |

No external art, recordings, fonts or sample libraries are bundled in gameplay resources.
Pillow draws native pixel art and materials, cube geometry is authored in Python, and NumPy synthesizes
the cues. The preview labels use the system font and are not gameplay textures.

## Stable ability IDs

| Input | IDs (also icon and sound basenames) |
| --- | --- |
| Hymn and Light | `hymn_of_valor`, `hymn_of_renewal`, `sunshine`, `daytime` |
| `white_angel_theurgy` | `holy_light`, `sun_halo`, `holy_oath`, `cleave`, `fire_of_light` |
| `white_angel_notary` | `contract`, `authenticate`, `amplify`, `nullify` |
| `white_angel_radiance` | `purification`, `domain`, `flaring_sun`, `spear`, `white_ray`, `holy_equipment` |
| `white_angel_justice` | `define_justice`, `justice_halo`, `judgment`, `faith_appraisal` |
| `white_angel_sun` | `solar_envoy`, `praise_the_sun`, `holy_kingdom`, `servant_of_faith`, `pure_white_light`, `sun` |

Upgrades use the same gameplay entry ID; the additional icon can be selected when the Sequence changes.
`ability.circle_of_inevitability.white_angel.<id>` supplies the English name. Dynamic Pact/Believer counts
use `hud.circle_of_inevitability.white_angel.pacts` and `.believers`, both with two format arguments.
The guilt label has one argument. Proclamations and countdown text should be rendered dynamically;
the scrolls, seals and glyphs provide their surrounding visuals.

## Models and animation

The eight requested core models are `sun_halo`, `holy_mace`, `spear_of_light`, `golden_scales`,
`blade_of_light`, `sunbird`, `sun` and `hymn_note`. Supporting props are:

- `holy_armor`, `holy_armor_chest`, `holy_armor_head`, `holy_armor_legs`: hollow light armor;
  separate pieces let later ability code choose body/head placement. These are ModelFx props, not an entity rig.
- `sunbird_incomplete`: smaller wings for the incomplete form.
- `sunbird_0..3` and `sunbird_incomplete_0..3`: four poses per form, matching element names, faces and UVs.
- `contract_scroll`, `notary_seal`, `oath_sigil`, `holy_eye`: pacts, authentication, blessings and appraisal.
- `warning_sigil`, `purification_ring`, `domain_boundary`, `justice_halo`: hollow world-space circles.
- `kingdom_gate`, `believer_sun`, `flame_plume`, `ray_core`: kingdom landmarks, overhead markers,
  golden fire and a scalable beam core.

Use namespace-relative model names such as `ModelFx.of("white_angel/holy_mace")`.
Everything is within Java model coordinates -16..32 and uses legal cube rotations. Models are automatically
loaded by the existing ModelFx resource loader. The existing particle atlas includes all `textures/model_fx/`.

Model space is 16 pixels per block. ModelFx defaults to pivot `(8,0,8)`: this suits the armor and the mace/blade
grips. Set `.pivot(8,8,8)` for the other models so rings sit at the chosen location, the Sun rotates around
its center and Sunbird forms turn around the body. Numeric `pivot_pixels` entries in the manifest give the
recommendation per model. Cube bounds are recorded before element rotations.

```java
// Runtime placement and animated poses are wired through WhiteAngelFx.
ModelFx sunbird = ModelFx.of("white_angel/sunbird")
        .frames(4).smooth().pivot(8, 8, 8).glow().unshaded();
ModelFx incomplete = ModelFx.of("white_angel/sunbird_incomplete")
        .frames(4).smooth().pivot(8, 8, 8).glow().unshaded();
ModelFx sun = ModelFx.of("white_angel/sun").pivot(8, 8, 8).glow().unshaded();
ModelFx mace = ModelFx.of("white_angel/holy_mace").glow();
```

Use `ParticleShapes.modelOnTurning` for forms/armor, `modelOnHead` for halos/Believer markers, and
`modelAlong` for the spear and beam (their long axis is +Y). The spear's feathered head supplies the
Spear of Light upgrade; the base Unshadowed Spear can use the same mesh. Scale beam length from its
one-block core. The Sun has a 14-pixel luminous core diameter; set runtime scale for the desired world size.
The Sunfire atlas has four 64 x 64 vertical frames, three ticks per frame with interpolation.

Rings use actual hollow geometry and opaque textures. `glow()`/`unshaded()` are suitable for energy;
no transparency flag is required for the materials. Use `.color(...)` to fade the armor or field props.
The grayscale particle textures accept gold or white tint. `purification_ring` supplies a pure-white
warning for Pure White Light; `warning_sigil` supplies the gold warning for Flaring Sun/Judgment.
Shared SpellFx/Ray/SlashFx primitives still handle growing fields, warning lines, trails and melee arcs.

## Particles and sounds

Particle IDs are `circle_of_inevitability:fx/white_angel/<name>`:
`note_0`, `note_1`, `sun_mote`, `golden_flame`, `feather`, `seal`, `oath_glyph`, `eye`,
`ray_streak`, `light_crescent`, `purify_spark`, `guilt_mark`, `white_ring`.
The existing ParticleFx loader discovers their directory automatically.

Each ability has a short sound cue. Ten supporting cues cover `warning`, `countdown`, `pact_breach`,
`believer_convert`, `intuition`, `wingbeat`, `field_end`, `inextinguishable_light`, `melee_swing` and
`melee_impact`. Cue IDs are `circle_of_inevitability:white_angel.<name>` and are registered through
`ModSounds.WHITE_ANGEL.get("<name>").get()`. The sounds are nonverbal chimes, synthetic choir-like
chords, light sweeps and impacts. They are one-shots, not speech or seamless ambience loops.
Use the support cues when the relevant event occurs; repeat countdown ticks only at the intended beats.

Development-client checks:

```mcfunction
/particle circle_of_inevitability:model{model:"circle_of_inevitability:model_fx/white_angel/sun",glow:1b,lifetime:200} ~ ~2 ~ 0 0 0 0 1
/particle circle_of_inevitability:fx{textures:["circle_of_inevitability:fx/white_angel/feather"],color:"FFFFD98A",glow:1b,size:0.3f} ~ ~1 ~ 1 1 1 0 30
/playsound circle_of_inevitability:white_angel.hymn_of_valor master @s ~ ~ ~ 1 1
```

## Regenerate and validate

Requires Python 3, Pillow, NumPy, and `ffmpeg`/`ffprobe` on PATH. Run from any directory:

```sh
python3 tools/model_gen/white_angel/generate.py
python3 tools/model_gen/white_angel/validate.py
python3 tools/model_gen/white_angel/preview.py
python3 tools/model_gen/white_angel/package.py --output /tmp/white_angel_assets.zip
./gradlew build
```

`generate.py --assets PATH` builds an isolated resource tree. `--skip-audio` is for visual iteration
on an existing generated tree. Re-encoding may change Ogg container serials; the manifest records the
actual packaged bytes. Generating preserves other pathways' shared sound events and translations.
The asset manifest excludes hashes of shared JSON files because other pathways can update them independently.

Validation checks all asset hashes, 29 distinct ability images, menu/HUD sizes and alpha, complete model
references, cube bounds/rotations/UVs, both compatible four-pose animations, texture animation metadata,
sound-event registration and subtitles. It fully decodes all 39 cues and checks mono Vorbis format,
sample rate, duration, non-silence and headroom.

The optional handoff ZIP includes owned resources, previews, generator sources, the shared ModelFx helper
and a sound registration snippet. Shared sound/translation files are filtered to White Angel entries.
Merge those entries when importing into a source checkout. The archive also works as a resource overlay
with this updated mod; it does not provide abilities on its own. Packaging verifies every resource hash
and the filtered shared JSON contents after writing the ZIP.
