# God of Decay effects

The presentation kit for the planned Second Law pathway is implemented in
`god_of_decay/GodOfDecayFx.java`. It provides visuals, audio, wheel/status icons,
owner-only Sickness pips and preview commands. Gameplay abilities, disease storage,
damage, contagion rules, cooldowns and pathway selection are still planned.
This kit does not implement those mechanics or modify terrain.

## Preview

As an operator, use `/decayfx <name>` (TAB completes names), or `/decayfx all`.
The gallery plays 40 displays five seconds apart (about 3 minutes 20 seconds).
It uses the nearest living target within 12 blocks, falling back to the viewer.
Placed displays appear six blocks ahead. Models and overlays expire automatically.
Use third-person view to inspect worn effects and the giant forms.
The command requires a player and gamemaster permission.

`/decayfx incubation` deliberately produces nothing. `/decayfx outbreak` previews
its visible payoff separately; the gallery's label is command feedback only.
Extra previews cover avatars, Time Giant, the ancient God of Decay, reforming,
contagion, collapse, pathogen pools, Sickness, weathering, dissolved beams,
Sure Hit melee, held blows and madness.

## Integration contract

Every ability has an `Ability` enum entry. `Ability.icon()` returns the wheel image
path for `AbilityWheel.entry`. `status(player, ability, ticks)` shows a timed icon
and HUD timer; `statusEnd` removes them early. Incubation is excluded from both
visual status creation and sounds. Its wheel icon remains available for selection.
The HUD pathway icon and Dark Angel's pathway color recognize `God Of Decay`.

Pass a unique, nonempty tag per cast to all its persistent models. They expire at
`ticks`; call `end(level, entity, center, tag)` on each affected entity/location
for early cancellation. This also removes that player's tagged screen overlays.
Call `statusEnd` separately. Periodic accents are deliberately owned by the ability:
stop calling them when the cast expires, is dispelled, or its entities leave the level.
Do not put all future ticks into a queue for a cancelable gameplay ability.

| Ability | FX hooks |
| --- | --- |
| Ulcerous Burst | `ulcerousBurst`: five-block ring and outward pathogen spray |
| Carrier's Touch | `carriersTouch`, then `infectedHit` on contact and `contagion` on a hop |
| Red Tape | `redTape`: bands enclosing the target |
| Official Capacity | `officialCapacity`, `capacityLink` for nearby allies |
| Corruption | `corruption` with actual zone radius; `zoneTick` once a second |
| Hollow Out | `hollowOut`: hollow shell; `collapse` when it gives way |
| Laziness | `disease(..., LAZY, ...)`: slow overhead ring |
| Obesity | `disease(..., OBESE, ...)`: swollen waist ring |
| Degeneration | `disease(..., DEGENERATE, ...)`: grey cracking |
| Incubation | No cast FX; `LATENT` is a no-op; call `outbreak` only after eight hidden seconds |
| Rot | `disease(..., ROTTING, ...)`: infected residue; `contagion` when rot spreads |
| Division | `division` at the double's position; repeat for up to three avatars; `reform` when an extra life is used |
| Pathogen Form | `pathogenForm(..., false)`, `pathogenTrail` every five ticks, then `reform` |
| Certain Death | `beam(..., true)`; resolve at `BEAM_HITS` (6 ticks), call `beamImpact`, then `disease(..., DOOMED, ...)` |
| Sure Hit | `beam(..., false)` follows an arcing path; `beamImpact`, then `sureHitSwing` for extended melee |
| Decay | `decay`, `zoneTick`, `weathering` on decaying constructs; `aged=true` for the Sequence 1 version |
| Swollen Form | `form(..., SWOLLEN, height, ...)`, `formStep`, `pathogenPool`, `madness` |
| Left Hand of Decay | `leftHand(..., false)`; `HAND_RISEN` is 16 ticks; `handVision` only for affected players after it rises |
| Withering Palm | `leftHand(..., true)`, `palmBlock` at the intercepted projectile |
| Gravel Shot | Six `gravelShot` calls at `GRAVEL_INTERVAL` (5 ticks); each visual reaches the aim snapshot in 6 ticks; `weathering` on impact |
| Slow Time | `slowTime`, `zoneTick` with `TIME` |
| Segmented Time | `segmentedTime`: three separated rotating ring sections; `heldBlow` on capture/release |
| Senescence | `senescence` for the area; `disease(..., AGED, ...)` for the target; `collapse` at death |
| Entropy | `entropy`, `zoneTick` with `ENTROPY` |
| Virus Incarnation | `pathogenForm(..., true)`, `pathogenTrail(..., true)`, `contagion`, `reform` |
| Ragnarök | `ragnarok`, `zoneTick`; refresh `duskSky` every 20 ticks for players inside; `entropyCount`, `collapse` |

`diseaseTick` supplies low-density, once-per-second visible disease accents.
`sickness(owner, target, count)` draws 0–6 green pips only for the specified owner;
call every 20 ticks with the visible Sickness count, excluding latent marks.
`entropyCount` similarly draws an owner-only ring growing with the 0–100 count.
Both expire after 22 ticks so old observations disappear when refreshing stops.

`dissolvedBeam` provides the eight-block burst for beams fired while dissolved.
`Form.TIME_GIANT` and `Form.ANCIENT_GOD` provide the high-Sequence forms at a supplied
height (capped at 48 blocks). `FORM_RISEN` is 20 ticks. The Time Giant is translucent
water-blue; the ancient form is ash-grey, wrinkled and hunched.
`formStep`, `weathering` and the existing disease/zone hooks also cover visible
passive procs. Passives without visible manifestations need no cast display.

FX do not choose victims, consume spirituality, manipulate equipment durability,
hide entities, grow their collision boxes, stop projectiles or bypass death wards.
Those remain responsibilities of the future ability implementation. The curved
Sure Hit beam is a visual path, not an obstacle-routing algorithm.

## Assets and regeneration

Run from the repository root (Python with Pillow, plus `ffmpeg`):

```sh
python3 tools/model_gen/god_of_decay/generate.py src/main/resources/assets/circle_of_inevitability /tmp/decay-icons.png
python3 tools/model_gen/god_of_decay/validate.py
```

The generator contains original procedural artwork and synthesized mono audio:

- 12 block-model JSONs and textures in `model_fx/god_of_decay` (including the hand,
  three forms, double, pathogen, gravel, tape, hollow shell, ring, cracks and pool).
- 35 wheel glyphs at 32 px and 35 HUD glyphs at 16 px, plus the pathway icon.
- Three custom particle textures and two translucent screen overlays.
- 33 Ogg sound events registered through `ModSounds.GOD_OF_DECAY`, with subtitles.

The world never changes: rot, pools and cracks are model particles. Large zones use
bounded particle counts. The sky overlay uses a 25-tick TTL for prompt exit cleanup.
Models can be rendered without Minecraft using `tools/model_gen/hanged_man/render.py`.

Validation: `./gradlew build`, resource-reference checks, audio decoding checks and
software-rendered model/icon inspection. In-game camera, transparency, audio mix and
multiplayer presentation should still be checked with `/decayfx all` in a client.
