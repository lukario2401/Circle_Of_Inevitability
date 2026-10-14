# Sequences, Pathways, digestion

The progression system, Lord of the Mysteries style. Acting, potions, ingredients and rituals are **not** implemented yet; see "Extending" at the end.

## The model

| Word | Meaning | Where |
| --- | --- | --- |
| Pathway | The class a player follows, by its exact name: Key Of Stars, Worm Of Time, Hand Of Order, Attendant Of Mysteries, Prince Of Abolition, Pale Emperor, Conqueror, Apocalypse, Dark Angel, Beauty Goddess (`"none"` until one is set). | `SoulCore.getPathway / setPathway` |
| Sequence | 9 (weakest) down to 0 (strongest), stored directly. Everyone starts at 9. | `SoulCore.getSequence / setSequence` |
| Digestion | 0 to 100 percent of the current Sequence. At 100 the player may run `/sequence advance`. | `SoulCore.getDigestion / setDigestion`, `Digestion` |
| Spirituality | The resource abilities spend (was "soul essence"; same numbers, same costs). | `SoulCore.getSpirituality / setSpirituality`, `Spells.paySpirituality` |
| power level | 1 at Sequence 9 up to 8 at Sequence 2 (see the table below). Used only for scaling (Key Of Stars star cap), never for gates. | `Beyonder.powerLevel` |
| power scale | The power level capped at 7. | `Beyonder.powerScale` |

All of it is saved in the player's persistent data under the tag `circle_of_inevitability` (keys `pathway`, `sequence`, `digestion`, `spirituality`), which is copied on death (`SoulCore.onPlayerClone`) and saved with the player, so it survives death, respawn and relog. The client gets Pathway, Sequence and digestion (whole percent) through `CSyncPathwayPacket`, sent by `PathwaySync` whenever one of them changes.

Ability gates read naturally: `if (!Beyonder.hasSequence(player, 6)) return;` means "needs Sequence 6 or stronger".

## The numbers of every Sequence

One table in `Beyonder` (`SEQUENCES`, one row per Sequence) holds them. To rebalance a Sequence, change its row there and here.

| Sequence | Power level | Power scale | Max spirituality | Regen per tick |
| --- | --- | --- | --- | --- |
| 9 | 1 | 1 | 20 | 0.14 |
| 8 | 2 | 2 | 400 | 1.33 |
| 7 | 3 | 3 | 1980 | 5.4 |
| 6 | 4 | 4 | 6400 | 16 |
| 5 | 5 | 5 | 15500 | 41.7 |
| 4 | 6 | 6 | 32400 | 108 |
| 3 | 7 | 7 | 59780 | 343 |
| 2 | 8 | 7 | 78400 | refills at once |
| 1 | 8 | 7 | 78400 | refills at once |
| 0 | 8 | 7 | 78400 | refills at once |

Sequences 1 and 0 have no numbers of their own yet and repeat Sequence 2; they also unlock no new abilities yet. "Refills at once" is a full bar every tick.

Where the numbers come from: the old ascension stage ran 1..8. Sequence 9 has the numbers of old stage 1, Sequence 8 of stage 2, down to Sequence 2 with stage 8 (changed on 2026-10-11; before that Sequence 9 and 8 shared the stage 1 numbers). The ability gates did not move with them: a gate `stage >= N` is still `Beyonder.hasSequence(player, 9 - N)`.

Note: most abilities cost 250 or more, so at Sequence 9 (maximum 20) they cannot be paid for. Sequence 8 (maximum 400) is the first that can.

## Required Sequence of every ability key

Keys: G = ability 1, H = 2, J = 3, K = 4, L = 5, N = 6, M = 7 (default bindings).

| Pathway | G (1) | H (2) | J (3) | K (4) | L (5) | N (6) | M (7) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Key Of Stars | Star Burst 9 | Dash 8 | Buff 7 | Seal 7 | Transport 7 | Orbital 5 | Cosmic Plague 3 |
| Worm Of Time | Life Theft 9 | Theft 8 | Parasite 4 | - | - | - | - |
| Hand Of Order | Buff 9 | Decree 8 | Edict 7 | Ability 4: 6 | Ability 5: 5 | Words 4 | Bind 3 |
| Attendant Of Mysteries | Paper Dagger 9 | Flaming Jump 8 | Damage Transfer 7 | Air Cannon 6 | Countdown (Spirit Body Threads) 5 | Historical Void 4 | Grafting 3 |
| Prince Of Abolition | Spear 9 | Flame 8 | Frost 7 | Hammer 6 | Meteor 5 | Entropy 4 | Abolition 3 |
| Pale Emperor | Root 9 | Door 8 | Serpent 7 | Spear Skull 7 | Spikes 6 | Wings 5 | Wraiths 4 |
| Conqueror | Hunter's Trap 9 | Provocation 8 | Fire Ravens 7 | Flame Transformation 6 | Cull 5 | Galvanization 4 | Fog of War 3 |
| Apocalypse | Fatal Step 9 | Black Flame 8 | Mirror World 7 | Silk Weaver 6 (wheel: silks at 6, 5, 4, 3) | Plague 5 | Unaging 4 | Catastrophe 3 (wheel: disasters at 3, 2, 1) |
| Dark Angel | Divination 9 | Whispers 8 | Shadow Binding 7 | Flesh Bomb 6 | Grazing 5 (7 slots of stolen abilities) | Cull of Spiritual Flesh 4 | Degeneration 3 |
| Beauty Goddess | Medicine Concoction 9 | Taming 8 | Wings of Darkness 7 | Potions 6 (wheel: potions at 6, 6, 5, 4) | Lunar Battlefield 5 | Gaze of Darkness 4 | Moon 3 (wheel: Door of Summoning 3, Creation 2, Beauty 1, Blood Moon 0) |

The number is the weakest Sequence that can use it (that Sequence and every stronger one). Beauty Goddess's two wheels (K, M): sneak recasts the last pick with no cooldown, for twice its cost; see [BEAUTY_GODDESS.md](BEAUTY_GODDESS.md).

A stolen ability is the exception: Worm Of Time's Theft (H) casts what it stole whatever its Sequence, at no cost (`Beyonder.castUnbound`; see "Another Pathway's abilities" in [SPELL_KIT.md](SPELL_KIT.md)). The same table is in code as `PathwayAbilities.ALL`; a new ability gets a line in both.

Worm Of Time's Parasite (J) is the only key that works while its player is inside a host: J again puts them back out (`status/Parasite`; see "Parasite" in [SPELL_KIT.md](SPELL_KIT.md)). Inside a mob, the walk keys, jump, sprint, mouse and both clicks steer it instead (`status/ParasiteControl`).

Worm Of Time's Theft (H) holds `THEFT_MAX_HELD` (5) stolen abilities at once and releases the newest first (sneak + H).

Dark Angel's Grazing (L) keeps an ability from every hostile mob or player its owner kills, in 7 slots that are kept for good (through death too). It casts the selected one whatever its Sequence, for three times that ability's own cost (`Beyonder.castUnbound(player, 3f, ...)`). Sneak + L picks the slot; by a soul that waits on its corpse (all slots full), it picks the slot that soul replaces.

## Renaming a Pathway

A player's Pathway is saved as its name, so a new name has to be the same text in every place that checks it:

| Where | What |
| --- | --- |
| The Pathway's class, `PATHWAY` near the top (e.g. `DarkAngel.java`) | the name its abilities check |
| `SequenceCommands.PATHWAYS` | the names `/sequence pathway` suggests (tab completion) |
| `PathwayAbilities.ALL` | the first text of each of its seven lines (theft, Grazing) |
| `SequencePassives` | any `effect("...")` / `custom("...")` line of that Pathway |
| `PathwayHudIcon` | its icon, if it has one |
| `DarkAngel.pathwayColor` | the color of its souls for Grazing |
| this file | the tables above |

Players who already have the old name keep it until `/sequence pathway <player> "<new name>"` sets the new one. The class and method names (`hangedManDivination`, `HangedManFx`) can stay as they are; only the text matters.

## Passives

What a Beyonder always has from a Sequence on (that Sequence and every stronger one), with no key press and no spirituality cost. All of them are one list at the top of `blasphemy_slate/beyonder/SequencePassives.java`.

| Passive | Who | From Sequence |
| --- | --- | --- |
| Saturation I (never hungry; vanilla also heals quickly while saturated) | every Pathway | 4 |
| Poison does not take (removed at every check) | Beauty Goddess | 9 |
| Strength I | Beauty Goddess | 8 |
| Night Vision, and half a heart back every second while hurt | Beauty Goddess | 7 |
| Its own potions and medicine last 1.5 times as long (in `BeautyGoddess`, `POTION_MASTERY`) | Beauty Goddess | 6 |
| Speed II while it is dark outside (the last one given runs out up to 30 s into the day); afterimages of moonlight while sprinting (in `BeautyGoddess`) | Beauty Goddess | 5 |
| Regeneration I | Beauty Goddess | 4 |
| A mob hunting you loses you now and then (30% each second) | Beauty Goddess | 1 |

To add one, add a line to that list (and a row here):

```java
effect(4, MobEffects.SATURATION);                           // every Pathway, from Sequence 4, level I
effect(6, MobEffects.NIGHT_VISION, 1);                      // the last number is the level: 0 = I, 1 = II
effect("Pale Emperor", 5, MobEffects.FIRE_RESISTANCE, 0);   // one Pathway only (the exact Pathway name)
custom(3, player -> player.clearFire());                    // anything that is not a potion effect
custom("Key Of Stars", 2, player -> { ... });               // the same for one Pathway
```

How they behave:
- Checked once a second (`CHECK_EVERY_TICKS`) on the server for every living player. A `custom` action runs at every check while the player has the Sequence, so write it so that running it again changes nothing.
- An effect is given for `EFFECT_TICKS` (30 seconds) and given again when half is left, with no particles; `SHOW_ICON` decides whether its icon shows. Milk, death or an ability that strips effects only removes it until the next check.
- A stronger or longer effect of the same kind from somewhere else (a potion, an ability) is left alone.
- When the player no longer has the Sequence or the Pathway (only commands can do that), the effect runs out on its own within `EFFECT_TICKS`. A `custom` action just stops, so use it for things that need no undoing.
- A misspelt Pathway name stops the game with a message that lists the valid names, instead of a passive that silently never works.

## Digestion

Digestion rises from three sources, all in `blasphemy_slate/beyonder/DigestionRates.java`:

| Constant | Default | Meaning |
| --- | --- | --- |
| `SPEED` | 1.0 | Multiplies every rate. 2.0 halves the time, 0.5 doubles it. |
| `PERCENT_PER_MINUTE_PLAYED` | 0.9 | Time online, given every `TIME_STEP_TICKS` (20 = each second). |
| `PERCENT_PER_FULL_BAR_SPENT` | 0.9 | Spirituality spent on abilities, proportional to the cost as a share of the maximum. |
| `MAX_PERCENT_PER_SPEND` | 0.5 | Cap for a single spend. |
| `PERCENT_PER_HOSTILE_KILL` | 0.25 | Killing a mob that is an `Enemy` (zombies, skeletons...). |

Target: a full bar in about 45 minutes of normal active play, roughly 40% from time (0.9 x 45), 40% from spending about one full bar of spirituality a minute, 20% from a kill every 30 seconds. To tune, change `SPEED` first; change a single source to shift the balance. Any decrease of spirituality through `SoulCore.setSpirituality` counts as spending; commands and Sequence changes use `setSpiritualityQuietly` and give nothing.

## Advancing

`/sequence advance` (anyone, for themselves): at 100 percent the Sequence goes down by one, digestion resets to 0, the maximum spirituality follows the new Sequence, with a chat message and the level-up sound. Refused at Sequence 0 ("You are already at Sequence 0...") and below 100 percent ("Your current Sequence is only P% digested..."). There is no loss of control.

## Commands

| Command | Who | Does |
| --- | --- | --- |
| `/sequence info` | everyone | Your Pathway, Sequence, digestion and spirituality (current of maximum). |
| `/sequence advance` | everyone | Advance yourself (see above). |
| `/sequence pathway <player> "<Pathway name>"` | op (level 2) | Set the Pathway. Tab completes every Pathway name (quotes needed). `"none"` clears it. |
| `/sequence set <player> <0-9>` | op | Set the Sequence. |
| `/sequence digestion <player> <0-100>` | op | Set digestion. |
| `/sequence spirituality <player> <amount>` | op | Set spirituality (clamped to the maximum). |
| `/soul corruption <player> [0-100]` | op | Show or set corruption (unchanged). |

The old `/soul get, setAspect, setstage, settier, setshards, addshards, addSoulEssence` are gone.

## Extending with acting or potions later

`AdvancementRequirement` is the extension point; `Digestion.requirement` is the one `/sequence advance` checks (default `AdvancementRequirement.DIGESTED`: digestion at 100 percent). Look for `TODO(acting/potions)`. To add a potion step, write a requirement (for example "drank the potion of the next Sequence") and combine it:

```java
Digestion.requirement = AdvancementRequirement.DIGESTED.and(new DrankPotionRequirement());
```

Acting would add a fourth digestion source: call `Digestion.add(player, percent)` from wherever an acting rule is followed, with its rate in `DigestionRates`.
