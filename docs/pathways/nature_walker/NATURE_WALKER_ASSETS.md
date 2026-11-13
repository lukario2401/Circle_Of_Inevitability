# Nature Walker assets and FX

This collection supplies presentation resources for all **39 abilities** in the supplied
[Nature Walker design](NATURE_WALKER.md). The uploaded design is preserved verbatim for reference.
The collection adds icons, models, particles, sound registrations, reusable `NatureWalkerFx` helpers
and an operator-only `/naturefx` preview command. [Gameplay integration](NATURE_WALKER_GAMEPLAY.md)
now makes all 39 ability entries playable; the preview command itself only displays FX.

## Preview

- [Ability icons and higher forms](assets/icons.png)
- [All growth stages](assets/growths.png)
- [Models rendered from their geometry and UVs](assets/models.png)
- [Eight animated summons and forms](assets/forms.gif)
- [HUD and particle sprites](assets/hud_particles.png)
- [Twelve-cue sound sampler](assets/sound_sampler.ogg) with [cue timing](assets/sound_sampler.json)

The contact sheets use independent scales. The software renderer is useful for geometry and texture
review; it does not certify Minecraft lighting, player fit, visibility, sound balance or performance.
Models are cube props for the existing ModelFx renderer, not entity rigs. Summon AI, collision and
hiding the underlying entity are owned by gameplay code, separate from these preview helpers.

## Inventory

Paths are relative to `src/main/resources/assets/circle_of_inevitability/`.

| Resource | Location | Contents |
| --- | --- | --- |
| Wheel/key art | `textures/gui/ability_menus/nature_walker/` | 50 distinct filenames: 39 ability icons, seven key icons (Treatment is shared), five upgrade/variant icons; 32 × 32 RGBA |
| HUD art | `textures/gui/nature_walker/` | 62 ability, variant and status icons, plus 12 mutation-level icons; 16 × 16 RGBA |
| Pathway art | `textures/gui/pathway_icons/nature_walker.png` | Mother Tree / life-and-return emblem; 16 × 16 RGBA |
| Models | `models/model_fx/nature_walker/` | 69 base models and 32 animation poses, 101 JSON files total |
| Materials | `textures/model_fx/nature_walker/` | 64 × 64 earth atlas; animated cycle atlas with four 64 × 64 frames and `.mcmeta` |
| Particles | `textures/particle/fx/nature_walker/` | 15 grayscale RGBA sprites, 16 × 16, accepting runtime tint |
| Audio | `sounds/nature_walker/` | 59 original mono Vorbis cues at 44.1 kHz |
| Manifest | `nature_walker/asset_manifest.json` | Resource hashes, ability mapping, sequences, growth states, animation sets, model bounds and pivots |

The art uses leaf green, wheat gold, soil brown, rosy Heartfruit, violet mutation and pale green spirit
light. All gameplay art, geometry and audio are generated from original source in
`tools/model_gen/nature_walker/`; no external recordings, samples, fonts or downloaded art are bundled.
Audio uses synthesis: woody impacts, earth rumbles, rustling noise, wet modulation, chimes and spectral
tones. These are short one-shot cues, not recorded Foley, speech or seamless ambience loops.

## Ability coverage

Each ID below has a menu icon, HUD icon, subtitle, registered sound event and Java cast preview.
Sequence values describe the complete reference; all 39 entries have gameplay registrations.

| Input | IDs |
| --- | --- |
| G / Sow | `thorn_seed`, `heartfruit_seed` |
| H / Treatment | `treatment`, `soul_suture` |
| J / Harvest | `seed_catalyzation`, `quicken_life`, `command_flora`, `weather_ritual`, `reap`, `blossoming_land`, `barren_land`, `great_harvest` |
| K / Biology | `cross_breed`, `toxin_bloom` |
| L / Earth | `underground_travel`, `swamp`, `wrath_of_nature`, `child_of_the_oak`, `stone_golem`, `bear_form`, `verdant_barrier`, `world_of_nature`, `earthquake` |
| N / Alchemy | `stone_bird`, `mud_golem`, `steel_golem`, `life_aura`, `mutate`, `brood_form`, `seed_of_rebirth`, `awaken` |
| M / Return | `evil_spirits`, `descent_of_death`, `return_to_earth`, `maternal_embrace`, `desolation`, `root_eruption`, `embrace_of_the_earth`, `bountiful_earth` |

The wheel groups retain the reference's 8 / 9 / 8 / 8 entries. `nature_walk`, `brood_form_complete`,
`given_life`, `weather_clear` and `weather_storm` are extra visual variants, not additional wheel entries.
`ability.circle_of_inevitability.nature_walker.<id>` supplies English labels. HUD localization includes
`growths` and `brood` with two arguments, `rebirth` with one argument, and `mutation` with two arguments.
Numbers and timers are rendered by gameplay HUD helpers, not baked into icons.

## Growths, creatures and supporting models

Every kind (`thornling`, `heartfruit`, `briar_orchard`, `sporebed`, `mother_tree`) has `_seed`, `_sprout`,
`_bloom` and `_withered` models. Heartfruit and Mother Tree additionally have `_ripe` models. The separate
`seed` and `heartfruit` props cover planting, pickups and harvest animations. Mother Tree uses a larger
runtime scale; its ripe fruit hangs below the canopy to remain visible.

`stone_bird`, `stone_golem`, `mud_golem`, `steel_golem`, `bear_form`, `brood_form`,
`brood_form_complete` and `evil_spirit` each have four compatible poses (`_0` through `_3`). The stone
bird has ruby eyes. The Brood Forms mix soil, flesh, wheat, mushrooms and living tendrils. Hybrid mobs
reuse their existing bodies with `hybrid_crown` and the four `mutation_*` overlays.

Other models cover vine/root bursts and eruption, mire, coffin, rift, grave markers, hollow bark/stone
armor, healing flowers, soul sutures, scythe, rain and storm clouds, sun, falling stone, undertow,
lightning, rebirth seed, earth lance and life/return rings. `world_of_nature_tile`,
`bountiful_earth_tile` and `verdant_barrier` supply realm and wall geometry.

## Runtime contract

Use `NatureWalkerFx` in
`blasphemy_slate/beyonder_characteristics/nature_walker/NatureWalkerFx.java`:

```java
String tag = "nature_" + java.util.UUID.randomUUID();
NatureWalkerFx.cast(level, caster, point, NatureWalkerFx.Ability.ROOT_ERUPTION, 160, tag);
NatureWalkerFx.growth(level, point, "heartfruit", "ripe", 200, tag);
NatureWalkerFx.mutation(level, target, "vines", 3, 120, tag);
NatureWalkerFx.form(level, summon, "stone_bird", 240, tag, 1f);
NatureWalkerFx.realm(level, center, 20f, false, 200, tag);
NatureWalkerFx.end(level, target, point, tag);
```

Use a unique, nonempty tag for each concurrent effect; the examples are alternative calls.
`growth`, `mutation`, `form` and `realm` replace their tagged visual. `end` removes attached and
world-space models with that tag. Short particle bursts and one-shot audio finish naturally.
All models expire even if the caller fails to clear them. Callers own gameplay durations, resource
consumption, targeting, real radius, repeated beats, dispels and cancellation on death or unload.

The convenience `cast` method uses small preview compositions and supplied durations. The realm
cast preview is radius 4; use `realm` with the actual ability radius in gameplay. The sphere shell
uses 32 lattice tiles plus 64 motes regardless of radius, capped at radius 40. Life/return circles and
walls in `cast` likewise illustrate the look rather than read gameplay constants. Root Eruption has
40 ticks of warning before the roots grow; no scheduled damage or world mutation is involved.

Models use 16 pixels per block, legal Java cube rotations and pivot `(8,0,8)`. Sphere tiles override
this to `(8,8,8)` for placement. Animated models repeat a four-pose cycle with six ticks per pose;
they have a maximum 360-tick visual lease to stay below ModelFx's 64-key packet limit. Refresh them
before that lease expires for long-lived summons. Static models permit up to 6000 ticks. `form` follows
the entity's position and body rotation; it does not alter player size, visibility or armor slots.

`weather(..., "rain" / "clear" / "storm", ...)` draws a local weather prop without changing world
weather. `form(..., "brood_form_complete", ...)` selects the full form. For Nature Walk, draw
`model("nature_portal", ...)` at entrance and exit. Wood and stone armor use `bark_skin_wood` and
`bark_skin_stone`. Surface hazards are separate `falling_stone`, `undertow`, `lightning`, `vine_burst`
and `root_burst` props; gameplay must choose the correct one for the terrain.
`melee` exposes petal trails, thorn impacts and matching cues for the future melee combo.

Particle IDs are `circle_of_inevitability:fx/nature_walker/<name>`. Sprites are discovered by the
existing particle loader; model materials are included by the existing model_fx atlas source.
Sound IDs are `circle_of_inevitability:nature_walker.<name>`, registered as
`ModSounds.NATURE_WALKER.get("<name>").get()`. All 39 cast cues and 20 supporting cues have subtitles.
Use `NatureWalkerFx.sound` once per event; avoid replaying cast audio on every aura tick.

## In-game inspection

The command requires operator permission (game master level). It only shows transient visuals and
plays audio. It does not grant a pathway, spawn combat mobs, deal damage or modify blocks.
Tab completion exposes every ability, model, sound and valid growth stage.

```mcfunction
/naturefx treatment
/naturefx root_eruption
/naturefx bountiful_earth
/naturefx model brood_form_complete
/naturefx model stone_bird
/naturefx growth heartfruit ripe
/naturefx sound rebirth_bloom
/naturefx particles
/naturefx stop
```

Previews last eight seconds, four blocks ahead on the player's current horizontal plane. For a
consistent view, use flat ground and third-person camera for worn forms. `stop` removes the player's
preview models; emitted short-lived particles and audio are not cancelled.

## Regenerate, validate and package

Requires Python 3, Pillow, NumPy and `ffmpeg`/`ffprobe`. Runtime build requires Java 25, downloaded by
this project's Gradle toolchain when absent. From the repository root:

```sh
python3 tools/model_gen/nature_walker/generate.py
python3 tools/model_gen/nature_walker/validate.py
python3 tools/model_gen/nature_walker/preview.py
python3 tools/model_gen/nature_walker/package.py --output /tmp/nature_walker_assets.zip
./gradlew build
```

`generate.py --assets PATH` produces an isolated asset tree. `--skip-audio` requires an existing audio
collection and is useful for visual iteration. Generation merges only Nature Walker entries into
shared `sounds.json` and `lang/en_us.json`. It preserves other pathways. The manifest hashes owned
resources, not shared files, because other pathways can update those independently. Ogg container
serials may vary on re-encoding; the synthesized source waveform is deterministic.

Validation checks all hashes, every ability mapping and icon, all model references, cube bounds,
rotations, UVs, unique element names, growth-stage coverage, eight compatible animation topologies,
texture animation metadata, Java sound/model registration, subtitles and decoded audio (format,
length, non-silence, fades, distinctness and headroom). Packaging verifies the written ZIP and merges
only owned keys into its shared JSON excerpts. The ZIP is a source handoff, not a standalone mod.
