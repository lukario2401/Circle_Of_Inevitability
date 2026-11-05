# Author — the Spectator pathway

**Status: ability and mechanic design, with generated assets. Playable Java abilities are not implemented.**
The pathway identifier proposed for gameplay is `"Author"`, resource prefix `author`.
Sequence titles: Spectator (9), Telepathist (8), Psychiatrist (7), Hypnotist (6), Dreamwalker (5),
Manipulator (4), Dream Weaver (3), Discerner (2), Author (1), Visionary (0).
These titles and the mind, dream, dragon and envisioning themes follow Lord of the Mysteries;
the numbers, Insight resource, keys, restrictions and individual combat translations below are game design.

## Reference classes and identity

This follows the inspected implementations of Attendant Of Mysteries, Prince Of Abolition and Hand Of God:
seven keys, paired basic casts, sequence-gated wheels, shared spell/status helpers, a dedicated FX class,
and explicit cleanup. Attendant supplies the readable setup/payoff rhythm; Prince supplies enemy marks
and conditional delayed effects; Hand Of God supplies support powers, separate wheel modules and a
mythical form. Author's distinguishing loop is **observe → understand → influence → write an outcome**.
It does not copy Attendant's global five-cast burst or Knowledge Emperor's passive Study damage bonus.

Visual language: jade scales, pale gold irises, ivory masks/pages and deep violet dreams. Early powers are
small eyes, ripples and pendulums; higher sequences add a horned Mind Dragon, thought structures, floating
quills and a sea of minds. No terrain destruction or real block placement is required.

## Inputs and progression

Normal G/H/J casts the first entry; sneak casts the second. K/L/N/M opens its wheel; sneak repeats its
last successful selection, matching Hand Of God. Opening/canceling a wheel is free. Locked entries show
their required sequence and never charge. Last selections are independent for each wheel.

| Key | Group | Unlock | Normal / sneak or wheel entries |
| --- | --- | --- | --- |
| G | Observation | 9 | Observe / Spectator State |
| H | Emotions | 8 | Awe / Frenzy |
| J | Therapy | 7 | Psychoanalysis / Placate |
| K | Hypnosis | 6 | Suggestion, Sleep, Psychological Invisibility, Battle Hypnosis |
| L | Dreams | 5 | Dream Entry, Nightmare, Dream Maze, Dream Anchor |
| N | Mind | 4 | Mind Dragon, Mind Storm, Manipulate, Virtual Persona; Mind Walk at 3, Discerner's Gaze at 2 |
| M | Authorship | 1 | Foreshadow, Plot Twist, Fate Script; Envision, Life Creation, Sea of Collective Subconscious at 0 |

Every stronger sequence retains earlier abilities. The existing spirituality capacities are respected:
20 at 9, 400 at 8, 1,980 at 7, 6,400 at 6, 15,500 at 5, 32,400 at 4, 59,780 at 3 and 78,400 at 2–0.
The last three refill every tick, so their costs are descriptive; cooldowns, slots and Insight enforce limits.

## Insight and control rules

**Insight is owned by a caster about one target, from 0 to 5.** Observe gains one immediately and one
after each uninterrupted second of looking at that target, up to three per cast. Range is 24 blocks;
blocks, concealment, changing target or taking damage interrupt the channel. Moving is allowed.
No automatic attack, chat, inventory or private player information is revealed. The HUD shows health,
active combat statuses, mob aggression and the caster's own five Insight pips for the aimed target only.

At three pips the target is Exposed to this caster. Selected powers require and consume pips as listed.
Insight grants no universal damage multiplier. Twelve seconds without a gain starts decay of one pip
every four seconds. Maximum eight target records per caster; evict the least recently observed. Acquisition
checks `Spells` targetability and friendship. Higher sequence does not bypass concealment. Insight cannot
be transferred, farmed from allies, or shared by another Author.

**Resistance:** ordinary hostile mobs receive the listed control duration. Bosses (boss tag, boss encounter
entity, or maximum health above 150) and players never become puppets. Their sleep/hold is at most 1.5 s,
fear at most 2 s, and control-based attack reduction at most 20%. A stronger Beyonder halves these durations
again. Existing immunity hooks always win. Mental damage still uses normal damage/resistance handling.
Do not use true damage merely because an attack is mental.

After an Author hard control ends, the target has 6 s of Resolve against **all Authors**: sleep, forced movement,
target redirection and roots cannot reapply; the attempted cast fails before paying. Sleep wakes on the first
positive damage, and grants Resolve then. Cleanse/milk wakes, removes suggestions and scripts, and grants
Resolve. Existing unrelated roots must not be cleared by Author cleanup: source-owned restrictions are needed.

Single-target hostile powers require line of sight on cast and on delayed resolution. Fields require sight
from their center to each affected target. Cap AoE selection at 12 nearest eligible enemies, with UUID as a
tie breaker. Recheck friendship, PvP permission, dimension, life and concealment at execution time.
Invalid casts spend no spirituality, Insight or cooldown. Successful casts spend once on the server;
an effect cleansed or interrupted afterwards is not refunded. Damage has the caster as its source.

## Basic casts

Numbers are initial tuning values, in health points (2 = one heart), blocks and seconds. `SE / CD` means
spirituality cost / cooldown. Each plain cast has its own cooldown; no wheel-wide cooldown is added.

| ID / name | Seq | SE / CD | Effect and payoff |
| --- | --- | --- | --- |
| `observe` Observe | 9 | 4 / 4 s | Up to 2 s channel, 24-block target, gains 1–3 Insight as above. Eye opens one step per gain; target receives a subtle chime on reaching Exposed. |
| `spectator_state` Spectator State | 9 | 6 / 12 s | 8 s of composure: 20% less incoming mob melee damage and one extra Insight on the next completed Observe (still capped at 5). Does not hide or prevent damage. |
| `awe` Awe | 8 | 80 / 8 s | Fear aimed enemy within 18 blocks for 3 s. Optional automatic spend of 2 Insight adds 12 mental damage; without pips it only fears. Tell: expanding gold eye for 0.3 s. |
| `frenzy` Frenzy | 8 | 100 / 10 s | Requires/spends 2 Insight. Aimed target within 18 blocks has 25% attack falter for 6 s; a mob loses its present target once. Never forces it to attack allies. Players retain movement/camera. |
| `psychoanalysis` Psychoanalysis | 7 | 350 / 12 s | Spend 3 Insight on an enemy within 24 blocks: 36 mental damage and Weakness I for 5 s. Cracked mask signals the opening. Does not amplify other pathways' damage. |
| `placate` Placate | 7 | 400 / 14 s | Self and up to 7 allies within 8 blocks: remove fear, sleep and frenzy, restore 8 health, grant 4 s of Author Resolve. Cannot cure soul corruption or reset mythical-form Strain. |

## Hypnosis wheel — K

| ID / name | Seq | SE / CD | Effect and counterplay |
| --- | --- | --- | --- |
| `suggestion` Suggestion | 6 | 900 / 12 s | Spend 2 Insight, 20-block target. Plant a visible pendulum for 8 s: its next attack on you deals 30% less (20% against players/bosses), then the mark ends. One suggestion per caster/target; no stacking with itself. |
| `sleep` Sleep | 6 | 1,000 / 16 s | Spend 3 Insight, 20-block target, 0.75 s visible induction; sleep 5 s, interrupted by damage. Moving out of sight/range during induction cancels the effect. |
| `psychological_invisibility` Psychological Invisibility | 6 | 1,200 / 22 s | `Unnoticed` for 8 s: mobs forget you and aimed powers cannot choose you. Physical body remains visible and vulnerable to collisions/AoE. Attacking or casting a hostile power ends it. |
| `battle_hypnosis` Battle Hypnosis | 6 | 1,400 / 18 s | Spend 3 Insight, 20-block target. Ordinary mob is rooted and cannot attack for 3 s; player/boss for 1.5 s. A wireframe thought cage shows the remaining time. Damage ends this control too. |

## Dreams wheel — L

| ID / name | Seq | SE / CD | Effect and counterplay |
| --- | --- | --- | --- |
| `dream_entry` Dream Entry | 5 | 2,200 / 18 s | A sleeping or Exposed enemy within 24 blocks is marked for 8 s; spend 3 Insight unless already sleeping. One Dream Link per caster. It breaks beyond 32 blocks, on cleanse, concealment or lost sight for 1 s. No player is teleported to another dimension. |
| `nightmare` Nightmare | 5 | 2,800 / 12 s | Requires Dream Link; 0.75 s warning, then 48 mental damage and 2 s fear, consumes the link. It wakes a sleeping victim before fear; Resolve prevents fear when waking grants immunity. Damage remains valid. |
| `dream_maze` Dream Maze | 5 | 3,200 / 28 s | 1 s warning at a visible ground point within 20 blocks, 6-block radius for 6 s. Enemies suffer Slowness II and 20% falter while inside, refreshed for at most 1 s after leaving. Non-solid dream gates mark the edge; players can walk out. |
| `dream_anchor` Dream Anchor | 5 | 2,600 / 24 s | At your position, a 6-block support field lasts 10 s. Self/allies gain 20% damage reduction and wake/remove fear on entry. Once per entity per cast; no repeated cleanse spam. One anchor per caster. |

## Mind wheel — N

| ID / name | Seq | SE / CD | Effect and counterplay |
| --- | --- | --- | --- |
| `dragon_form` Mind Dragon | 4 | 6,000 / 45 s | 20 s incomplete mythical form: 25% damage reduction, +1.5 melee reach, safe controlled flight, claw hits +12 damage (once/0.5 s). 6-block fear pulse on entry, subject to Resolve. Gain 5 Strain/s; at 100 form ends and Weakness II lasts 8 s. At 3 use the complete dragon, duration 30 s, Strain 3/s. Strain decays 2/s only outside form; no recast reset. |
| `mind_storm` Mind Storm | 4 | 5,000 / 18 s | 1 s warning, then a visible 10-block forward cone, 3 pulses one second apart, 22 mental damage each. First pulse adds 1 Insight per affected target; no control lock. Moving out of the cone or breaking sight escapes later pulses. |
| `manipulate` Manipulate | 4 | 6,500 / 30 s | Spend 5 Insight on an ordinary hostile mob within 24 blocks: 12 s allied puppet, max one; attacking it ends control. Cannot control pets, existing summons or marionettes. Player/boss fallback: 20% reduced damage against caster for 6 s, no commands/forced actions. |
| `virtual_persona` Virtual Persona | 4 | 4,500 / 30 s | 15 s mental double orbiting the caster. It intercepts one Author mental control (fear, sleep, hypnosis, manipulation or script), then shatters and grants Resolve. Does not intercept damage, arbitrary spells, or resource costs. One slot; no nested personas. |
| `mind_walk` Mind Walk | 3 | 8,000 / 24 s | Spend 3 Insight on a visible target within 32 blocks; after 0.6 s move to a collision-safe grounded point within 3 blocks of it, grant 2 s fall protection. Requires valid destination and teleport permissions; never passes sealed domes or unloaded chunks. Link is a route, not an attack. |
| `discern` Discerner's Gaze | 2 | 10,000 / 30 s | For 10 s, Observe grants one additional pip and owned hostile delayed effects warn you with an eye 0.5 s before they resolve, if their owner is visible within 24 blocks. Exposed targets cannot apply Author mental control to you. No prediction of arbitrary AI/player actions. |

## Authorship wheel — M

Authorship selects bounded combat outcomes. It never executes chat, edits inventories or world data,
creates permanent creatures/items, rewinds death, or guarantees arbitrary future events. Floating paper and
an overhead script seal make armed outcomes visible to their target. One script slot across the first three
entries; replace your previous script only after a new cast validates. Scripts last 10 s, obey their own
cooldowns and cannot trigger from reflected, scripted, environmental or self damage.

| ID / name | Seq | SE / CD | Effect and counterplay |
| --- | --- | --- | --- |
| `foreshadow` Foreshadow | 1 | 12,000 / 30 s | Spend 3 Insight, enemy within 24 blocks. Its next successful direct attack on you triggers a 40-damage mental counter after 0.5 s warning; the incoming attack still lands. Cleanse, move out of range, or break sight to prevent the counter. |
| `plot_twist` Plot Twist | 1 | 14,000 / 40 s | Self script: the next direct hostile hit is reduced by 50%, then you step up to 6 blocks backward to a safe grounded point. No destination means reduction only. Does not restore health, resurrect or bypass teleport wards. |
| `fate_script` Fate Script | 1 | 16,000 / 45 s | Spend 5 Insight, 24-block enemy. The next direct hit you land within 10 s adds 50 mental damage and consumes the script. No execute threshold or unavoidable hit; shields, range, concealment and defenses still apply. |
| `envision` Envision | 0 | 18,000 / 25 s | Imagine a jade/gold blade for 12 s: ordinary melee hits add 20 damage at most once/0.5 s. Each third landed hit emits a 6-block, 24-damage forward thought slash. One weapon; no real item/drops, no automatic recasts. |
| `life_creation` Life Creation | 0 | 22,000 / 50 s | One imagined drake for 20 s, 80 health, attacks for 14 every 1.5 s. Non-breeding, no drops/XP, cannot be captured, leashed or recursively summoned. Despawns outside 32 blocks or on owner cleanup. Asset is a ModelFx shell; later implementation needs server summon AI and ownership. |
| `sea_of_minds` Sea of Collective Subconscious | 0 | 30,000 / 90 s | 1.5 s warning, then 12-block stationary domain for 12 s. Every 2 s, gain 1 Insight on visible enemies and restore 4 health to allies. At 5 Insight, next pulse spends all 5 for 36 mental damage, at most once per target per domain. One domain; does not repeatedly sleep/possess targets. |

## Passives and growth

| From | Passive |
| --- | --- |
| 9 | Composure: Observe keeps its current pip on interruption; only the unfinished second is lost. |
| 8 | Emotional Awareness: once/5 s, a visible hostile mob beginning to target you within 16 blocks gives a quiet warning. |
| 7 | Self-therapy: Author fear/frenzy durations on you are halved, before Resolve. Does not grant general damage resistance. |
| 6 | Lucid Mind: Author sleep durations on you are halved; Psychoanalysis consumes 2 instead of 3 Insight. |
| 5 | Dream Recall: your Dream Link persists for 2 s of broken sight instead of 1 s, still bounded by dimension and distance. |
| 4 | Mental Fortress: 15% less damage from attacks explicitly tagged mental; not all magic. |
| 3 | Dream Weaver: complete dragon form; Dream Maze radius 8 and Anchor duration 12 s. |
| 2 | Discernment: Insight decay begins after 20 s instead of 12 s. Target limit remains eight. |
| 1 | Narrative Economy: a successfully triggered script refunds 2 Insight to its target record if it still exists; cannot exceed 5. Plot Twist grants none. |
| 0 | Visionary: successful domain payoff extends your active Envision by 1 s, capped at 4 extra seconds per cast. |

Native claws remain the only proposed melee change; basic untransformed attacks use the existing weapon
system. None of these passives changes another pathway's globals or grants immunity to unrelated controls.

## Implementation map and lifecycle

Suggested classes: `author/Author` (G/H/J, dispatch, passives); `AuthorMind` (Insight, Resolve, marks);
`AuthorHypnosis`, `AuthorDreams`, `AuthorManipulation`, `AuthorAuthorship` (four wheels);
`AuthorFx` (all models, particles, sounds, HUD and preview command). This separation mirrors the reference classes.
Connect through `Beyonders`, sequence names, `PathwayAbilities`, `AbilityWheel`, key requirements, pathway icon
and `SequencePassives` only when gameplay exists; this asset change must not advertise an empty selectable class.

Reuse `Spells` for target selection/allies, damage and payment; `Fear`, `Slumber`, `Falter`, `Unnoticed`,
`Form`, `ConjuredWeapon`, `Marionette`, `Zone`, `Later`, `Physique` and `Cleansing` where contracts match.
Audit their actual behavior: `Slumber.wake` currently clears restriction kinds that can overlap another source,
so source-owned sleep/root cleanup is a prerequisite, not something to ignore. Use a dedicated Insight store:
`Study` automatically amplifies all damage and therefore is not interchangeable with Insight.

All gameplay is server authoritative. Index temporary records by caster UUID, target UUID and dimension;
use the server game clock, per-cast generation IDs, bounded maps and expiration checks. Delayed closures
re-resolve UUIDs and reject stale generation IDs. A player death, logout, dimension change, pathway change
or server stop removes owned fields, links, scripts, models and summons; target death/unload removes its
records. Do not force-load targets. Sequence loss immediately clears effects whose gate is no longer met.
Cooldown/Strain remainder is saved as remaining ticks across logout so reconnecting cannot reset them;
owned effects themselves end. On death keep cooldowns/Strain but end all controls/forms. No static
Entity/Level references survive server stop. Client receives only its relevant HUD state.

## Gameplay acceptance checks for the later implementation

1. Sequence 9 can afford both G casts; every wheel rejects locked entries without payment; each sequence
   has the listed unlock/passive, and both normal/sneak bindings work.
2. Two Authors observe one target independently; spending/decay/eviction cannot change the other's pips.
   No progress through walls, on allies, creative/spectator players or concealed targets.
3. Invalid, resisted and insufficient-Insight casts spend nothing; one successful cast charges once.
   Delayed failures do not refund, and stale closures cannot activate after death/recast/logout.
4. Sleep wakes on positive damage. Resolve works across multiple casters and on cleanse; unrelated roots
   survive cleanup. Boss and player caps apply; PvP disabled prevents hostile effects entirely.
5. Dream Link breaks at its leash/sight boundary. Maze/sea cannot reach through walls; allies/pets are spared,
   including those befriended between warning and impact. Field targets never exceed the cap.
6. Form attributes/flight restore cleanly, including prior creative flight. Strain survives reconnect and
   cannot be removed with Placate; partial/complete form selection matches the sequence.
7. Scripts cannot recurse from their own damage or trigger twice; teleport destinations are collision-safe
   and respect seals. Imagined summons never yield loot, duplicate on reconnect or outlive their owner.
8. On a dedicated server, model/sound resources load without client class references. In-client review checks
   menu readability, warning timings, dragon fit, particle density, subtitles and sound volume.

See [asset inventory, previews and regeneration](AUTHOR_ASSETS.md) for the delivered resources.
