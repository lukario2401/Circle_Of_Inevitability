# Author / Spectator asset collection

The [ability design](AUTHOR.md) covers Spectator (9) through Visionary (0), with **Author** as the
Sequence 1 class title. All 26 abilities are now playable through the registered Author pathway,
with these resources connected through `AuthorFx` and registered sound events.

## Review

- [All 26 ability icons, arranged by key](assets/icons.png)
- [Actual model geometry and UV textures](assets/models.png)
- [Complete/incomplete Mind Dragons and imagined drake in motion](assets/dragons.gif)
- [HUD states, six Insight levels and particles](assets/hud_particles.png)

These are software renders of the shipped geometry. Model scales are fitted independently in the preview.
Client-side glow, final placement, player/model fit, flight motion, animation timing and sound mixing need
an in-game review with the connected gameplay FX class. The dragons are ModelFx cube props with
interpolatable wing poses, not living entity rigs or a replacement player renderer.

## Inventory

Resource paths below are relative to `src/main/resources/assets/circle_of_inevitability/`.
The [manifest](../../../src/main/resources/assets/circle_of_inevitability/author/asset_manifest.json) records
every owned resource hash, model bounds/pivot, ability-to-model mapping and stable wheel/ability IDs.

| Resource | Path | Format |
| --- | --- | --- |
| 26 ability and 7 key icons | `textures/gui/ability_menus/author/` | 32 × 32 RGBA, transparent |
| 26 ability and 8 additional status icons | `textures/gui/author/` | 16 × 16 RGBA; three status names overlap abilities |
| Six Insight gauges | `textures/gui/author/insight_0..5.png` | 32 × 8 RGBA; five gold-edged jade pips |
| Pathway icon | `textures/gui/pathway_icons/author.png` | 16 × 16 eye |
| 35 models | `models/model_fx/author/` | Java Block/Item JSON; 23 base models plus 12 animation poses |
| Two material atlases | `textures/model_fx/author/` | 64 × 64 static `mind_atlas`; four vertical frames in `dream_atlas` |
| Ten particle sprites | `textures/particle/fx/author/` | 16 × 16 grayscale RGBA for runtime tint |
| 34 audio cues | `sounds/author/` | Mono 44.1 kHz Vorbis; 26 ability and eight support cues |
| English labels/subtitles | `lang/en_us.json` | Ability/key/status names, HUD placeholders and subtitles |
| Sound definitions | `sounds.json` | `author.<name>`, registered in `ModSounds.AUTHOR` |

All art and sounds are original procedural assets. Native pixel shapes and textured cube models are authored
in the included generators; audio is synthesized without recordings or sample libraries. Preview labels
use the system DejaVu font, which is not bundled. The preview renderer is adapted from this repository's
White Angel renderer; the art and models are specific to Author.

## Stable IDs and visuals

| Key | IDs | Primary visuals |
| --- | --- | --- |
| G | `observe`, `spectator_state` | Gold iris, ivory observer mask, five Insight pips |
| H | `awe`, `frenzy` | Terror eye, fractured mask |
| J | `psychoanalysis`, `placate` | Mind eye/shard, jade and ivory lotus |
| `author_hypnosis` | `suggestion`, `sleep`, `psychological_invisibility`, `battle_hypnosis` | Pendulum, dream lantern, mask, thought cage |
| `author_dreams` | `dream_entry`, `nightmare`, `dream_maze`, `dream_anchor` | Dream gate, terror eye, shards, cage, lantern/lotus |
| `author_mind` | `dragon_form`, `mind_storm`, `manipulate`, `virtual_persona`, `mind_walk`, `discern` | Mind Dragon, ripples, crown/threads, persona mask, gate, eye |
| `author_authorship` | `foreshadow`, `plot_twist`, `fate_script`, `envision`, `life_creation`, `sea_of_minds` | Book/quill/seal, imagined blade, drake, sea arch |

Ability localization keys: `ability.circle_of_inevitability.author.<id>`.
HUD keys: `hud.circle_of_inevitability.author.insight`, `.strain`, `.resolve`, each with one `%s` argument.
Use text rendering for numbers/countdowns; the artwork does not bake in names or statistics.

Models use 16 pixels per block. Honor `pivot_pixels` in the manifest: dragons, gate/sea arch, quill and blade
use `(8,0,8)`; other props use `(8,8,8)`. Dragon head points toward **−Z**, blade/quill/thread along **+Y**.
The complete dragon has a halo and larger wings; the incomplete form has shorter wings and no halo.
The imagined drake has reduced wing span; use a smaller runtime scale for the summoned creature.

```java
// ModelFx integration patterns used by AuthorFx.
ModelFx dragon = ModelFx.of("author/mind_dragon")
        .frames(4).smooth().pivot(8, 0, 8).glow().unshaded();
ModelFx incomplete = ModelFx.of("author/mind_dragon_incomplete")
        .frames(4).smooth().pivot(8, 0, 8).glow().unshaded();
ModelFx eye = ModelFx.of("author/mind_eye").pivot(8, 8, 8).glow();
ModelFx blade = ModelFx.of("author/envisioned_blade").pivot(8, 0, 8).glow();
Spells.sound(sl, at, ModSounds.AUTHOR.get("observe").get(), 0.7f, 1.0f);
```

The ModelFx loader discovers these model files, and the existing particle atlas includes model textures.
Material cubes are opaque; ghostliness/fades should use ModelFx color/alpha rather than transparent atlas
pixels. `dream_atlas.png.mcmeta` advances every four ticks, interpolated, for a 16-tick cycle. Frame models
must be animated separately with `frames(4)`; the texture metadata does not animate wing geometry.

Particle IDs: `circle_of_inevitability:fx/author/<id>` with `eye`, `ripple`, `scale`, `thought`, `thread`,
`ink`, `page`, `dream_shard`, `spiral`, `quill_star`. The existing ParticleFx loader discovers this directory.
Apply jade (`45B69A`), gold (`E3BA5F`) or violet (`977DD4`) tint at runtime. Pale page and thread particles
provide warnings even when the surrounding field is violet.

Audio events are `circle_of_inevitability:author.<id>`. Support IDs are `insight_gain`, `insight_spent`,
`resist`, `wake`, `warning`, `wingbeat`, `script_trigger`, `field_end`. They are one-shots, not loops:
quiet glass tones for observation/therapy, dissonant tones for attacks, paper scratches for authorship,
and low breath/wing noise for dragons. No spoken or subliminal content is used.

Client resource smoke checks after building the mod:

```mcfunction
/particle circle_of_inevitability:model{model:"circle_of_inevitability:model_fx/author/mind_eye",glow:1b,lifetime:200} ~ ~2 ~ 0 0 0 0 1
/particle circle_of_inevitability:fx{textures:["circle_of_inevitability:fx/author/eye"],color:"FFE3BA5F",glow:1b,size:0.3f} ~ ~1 ~ 1 1 1 0 20
/playsound circle_of_inevitability:author.observe master @s ~ ~ ~ 0.7 1
```

## Rebuild and validate

Requires Python 3, Pillow, NumPy, `ffmpeg` and `ffprobe` on PATH. Run from the repository root:

```sh
python3 tools/model_gen/author/generate.py
python3 tools/model_gen/author/validate.py
python3 tools/model_gen/author/preview.py
python3 tools/model_gen/author/package.py --output /tmp/author_assets.zip
./gradlew build
```

`generate.py --assets PATH` creates an isolated resource tree; `validate.py --assets PATH` validates it.
Generation preserves unrelated keys in shared sound/translation files. `--skip-audio` is for visual
iteration on an already complete tree. PNG/model generation is deterministic; Ogg container serials may
vary between encodes, so the manifest records the actual bytes. Preview generation reads the repository tree.

Validation checks hashes, distinct ability icons, sizes/alpha/grayscale, referenced textures, model bounds,
cube rotations/UVs, animation topology and changing frames, sound registry entries, subtitles and design ID
coverage. Every cue is fully decoded and checked for mono Vorbis, rate, duration, finite samples, headroom,
non-silence and uniqueness. The ZIP packages owned resources, filtered shared JSON, generators and previews;
it is a handoff/overlay for this updated mod, not a playable standalone pathway or a Minecraft resource pack.
