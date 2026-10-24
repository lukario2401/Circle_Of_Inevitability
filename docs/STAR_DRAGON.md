# Star Dragon: ability design

The Star Dragon Pathway is the Condenser Pathway of Lord of the Mysteries, the only Pathway under the Outer Deity Supernova Dominator: Astronomy Aficionado (9), Star Worshipper (8), Star Sacrificer (7), Navigator (6), Tidal Scholar (5), Heavybringer (4), Star Shepherd (3), Radiant Angel (2), Star Dragon (1), Condenser (0). Source: the Condenser Pathway and Condenser Pathway/Abilities pages of the Lord of the Mysteries wiki, laid out like Attendant Of Mysteries, Prince Of Abolition and Hand Of God.

All of it is written: the seven keys with every growth they get at stronger Sequences, the passives of Sequences 9 to 0, and the reusable parts they lean on (see "What is built", and "Adding to it" for a new piece). The rest of this file is the design it is built from. Its looks and sounds are in `StarDragonFx` (see "Looks, sounds and pictures" at the end).

Pathway name in code: "Star Dragon".

## Theme

A cosmic Pathway: gravity, the fundamental forces and the sky itself. The low Sequences watch and calculate the stars and read Fate in them. The middle Sequences sacrifice to the stars and borrow their gravity, rays and void. A Heavybringer is a source of gravity, heavy enough to bend Fate and the void around it. A Star Shepherd grazes asteroids and works the electromagnetic and nuclear forces. Radiant Angel and Star Dragon are first a small sun and then gravity itself, bending Space and, through it, Time. The Condenser holds Weight, Density and the Fundamental Forces.

Two rules carry the whole design: **Heaviness slows** (gravity is the crowd control: pressing, lifting, pulling, slowing) and **Starlight disorders** (rays, voids and Fate make enemies miss, stumble and lose their way). The power comes mainly from the stars and the land under them; only a small part comes from the sea, so tides and waves are flavor for the Tidal Scholar, not the core.

Gameplay identity: control first, damage second. Rays travel at light speed and cannot be dodged once fired, but they show a short warning line. The big sky abilities (Star Grazing, Planetfall, Singularity) always show a warning circle first, so they can be punished or escaped. It starts slow and expensive and gets overwhelming at the top.

Colors: star white and deep indigo-black for the void; dark green for the Weakening and Series rays; pale blue for the Piercing Ray; gold-white for sunlight and fusion; silver-teal for the Tidal Scholar's scales and waves. Pathway color: 0x6A5CFF.

Nothing here breaks blocks or lights real fire. Asteroids, fusion, black holes and radiation only show and hurt; the ground is never changed.

## How it is laid out

It follows the Attendant Of Mysteries pattern: seven keys (G H J K L N M), every ability has a normal cast and a sneak cast. Three keys (2, 5, 7) are wheels like Hand Of God's: every entry has its own Sequence, cost and cooldown, and on a wheel key sneak casts the last pick again (`AbilityWheel.quickCast`). Plain keys cost spirituality only, as in Attendant Of Mysteries.

The abilities do not each get a Sequence of their own. Sequence 7 has four abilities, 5 has three, 4 has five, 6, 3 and 2 have two each, 1 and 0 have three each, 9 and 8 have one each. Sequences 2, 1 and 0 are not left without abilities (eight entries between them), and they also strengthen what is already there (see "Grows with the Sequence").

| Sequence | Abilities |
|---|---|
| Astronomy Aficionado (9) | Star Chart |
| Star Worshipper (8) | Fate Glimpse |
| Star Sacrificer (7) | Gravity Abnormality, Electromagnetic Attraction, Cosmic Void, Weakening Ray |
| Navigator (6) | Void Passage, Piercing Ray |
| Tidal Scholar (5) | Tidal Surge, Scaled Form, Sunlight Ray |
| Heavybringer (4) | Heavybringing, Fate Convergence, Heavy Sphere, Series of Rays, Remote Sight |
| Star Shepherd (3) | Star Grazing, Fission Pulse |
| Radiant Angel (2) | Fusion Cluster, Radiant Angel Form |
| Star Dragon (1) | Gravitational Time Dilation, Planetfall, Star Dragon Form |
| Condenser (0) | Neutron Star, Singularity, Quantum Uncertainty |

## Numbers to start from

Every number below is a first value, to be tuned in play. Damage words: light 20, medium 40, heavy 80, very heavy 150, huge 300 per hit; the top Sequences also use a share of the target's maximum health. Spirituality costs follow Attendant Of Mysteries (250 at Sequence 9 up to 12000 at Sequence 3) and are extrapolated above that. Ticks: 20 = 1 second. Distances are in blocks.

## Keys

| Key | Ability | Sequence | Normal cast | Sneak cast |
|---|---|---|---|---|
| 1 | Astronomy | Astronomy Aficionado (9), Star Worshipper (8) | **Star Chart** (250): read the sky over an enemy. The enemy you look at (24 blocks) is Charted for 10 seconds: a faint constellation hangs over it that only you see, through walls. It cannot hide from you (Concealment and Unnoticed end on it), your aimed abilities never lose it, and the first ability you land on it while it is Charted does 20% more, because the shot was calculated. | **Fate Glimpse** (600): read the language of the stars. For 10 seconds you feel danger a heartbeat early: the first two attacks aimed at you (melee, projectile or ability) are dodged. A ripple of starlight marks each one and you slip a step aside. It does not stop what is not aimed at you (areas, falls, poison). |
| 2 | Star Sacrifice | Star Sacrificer (7), Tidal Scholar (5) | The Star Sacrifice wheel: Gravity Abnormality, Electromagnetic Attraction, Cosmic Void, Weakening Ray, Sunlight Ray. Each has its own Sequence, cost and cooldown. | The last Star Sacrifice pick again, without the wheel. |
| 3 | Navigator | Navigator (6) | **Void Passage** (2450): you see the hidden passages in the void and slip into one. You travel up to 24 blocks to where you aim in half a second and step out unnoticed: no mob can find you for 3 seconds (you can still be hurt, and hitting something ends it). Hidden enemies within 12 blocks of where you left and where you arrived are forced out of hiding. Not allowed while you are held in place (`Root` TELEPORT). | **Piercing Ray** (2450): two beams of blue light leave your eyes and pass through everyone in a line 28 blocks long. Each enemy takes medium damage and is Dimmed for 8 seconds: its blows and abilities do 25% less and its spirituality drains slowly (players). |
| 4 | Tidal Scholar | Tidal Scholar (5) | **Tidal Surge** (3000): gravity whips the air (and any water in view) into a wave. A crest 5 blocks tall and 7 wide rolls 18 blocks forward in about a second; everything it touches takes heavy damage, is thrown back and made Heavy (slowed) for 3 seconds. While it is still beside you (first half second), the first melee blow aimed at you is parried and its sender staggered. | **Scaled Form** (3000): starlight scales spread over your body and your eyes turn vertical. For 30 seconds you are a humanoid lizard: Strength II, Resistance II, longer reach, and every blow you land makes the target Heavy for 2 seconds. Casting it again turns you back. |
| 5 | Heavybringing | Heavybringer (4) | The Heavybringer wheel: Heavybringing, Fate Convergence, Heavy Sphere, Series of Rays, Remote Sight. | The last pick again, without the wheel. |
| 6 | Star Shepherd | Star Shepherd (3) | **Star Grazing** (12000): you graze a small asteroid out of its orbit. A warning circle (8 blocks) opens where you aim (28 blocks); 1.2 seconds later a rock wrapped in fire falls through the sky onto it: heavy damage in the circle, knockback and a burst of dust. No crater, no fire on the ground. A grazed star stays hanging above the spot for 30 seconds: allies under it (10 blocks) regain spirituality, the way a Star Shepherd helps others digest a potion. | **Fission Pulse** (9000): you loosen the nuclei in the enemy you look at (24 blocks). It glows for 2 seconds, then splits in a 6 block burst of radiation: heavy damage, and everything caught is Irradiated for 8 seconds (damage over time, no healing, weakness). A faint radiation zone stays where it burst for 8 seconds. |
| 7 | Radiant, Dragon, Condenser | Radiant Angel (2), Star Dragon (1), Condenser (0) | The Cosmos wheel, cast where you aim: Fusion Cluster, Radiant Angel Form, Gravitational Time Dilation, Planetfall, Star Dragon Form, Neutron Star, Singularity, Quantum Uncertainty. | The last pick again, without the wheel. |

## Key 2 wheel: Star Sacrifice

| Entry | Sequence | Cost / cooldown | What it does |
|---|---|---|---|
| Gravity Abnormality | Star Sacrificer (7) | 1250 / 12 s | Where you aim (20 blocks) the gravity of an 8 block area changes for 5 seconds. Aimed at the ground, enemies are **Pressed**: slowed hard, cannot jump, their projectiles drop, and falls hurt them 30% more. Aimed up (more than 45 degrees over the horizon), they are **Lifted**: held 3 blocks off the ground and unable to move until it ends. Allies are not touched. Bosses are only slowed. |
| Electromagnetic Attraction | Star Sacrificer (7) | 1250 / 10 s | An unseen magnet pulls two bodies together. Aimed at an enemy that has another enemy within 12 blocks, the two are drawn into each other and collide: both take medium damage and are staggered. With no second enemy, you and the target are drawn together and a hammer-like blow lands on it as you meet. |
| Cosmic Void | Star Sacrificer (7) | 2450 / 18 s | The enemy you look at (20 blocks) falls into a half-real void: a black sphere 4 blocks across with glimmers of light far in the dark. It lasts 4 seconds. Inside, the target is blinded, slowed and cannot find the way out; its body stays in the real world and can still be hurt. It ends early if the target takes damage equal to 40% of its maximum health. |
| Weakening Ray | Star Sacrificer (7) | 1250 / 6 s | A thin dark-green ray at light speed, 30 blocks. A faint line shows the aim for 0.4 seconds before it fires; after that it cannot be dodged. The first enemy it hits takes medium damage and is **Weakened** for 6 seconds: damage over time, and no healing except from a cleanse. |
| Sunlight Ray | Tidal Scholar (5) | 3000 / 10 s | Sunlight condenses in an instant into a thick ray (2 blocks wide, 24 long) after a 0.6 second charge. Every enemy in the line takes heavy damage and is blinded for 2 seconds; undead and creatures of darkness take double. In real daylight under open sky it costs half. |

## Key 5 wheel: Heavybringer

| Entry | Sequence | Cost / cooldown | What it does |
|---|---|---|---|
| Heavybringing | Heavybringer (4) | 6000 / 25 s | You become a source of gravity for 8 seconds. Everything within 16 blocks is slowly pulled toward you: enemies slip across the ground, rays, fireballs and other energy bend toward you, hidden enemies are exposed. Enemies within 4 blocks feel space invert (nausea) and are Heavy: slowed, no teleports, no leaps. You cannot be moved. Allies are not pulled. |
| Fate Convergence | Heavybringer (4) | 6000 / 40 s | Fate bends toward you for 10 seconds. Every enemy within 20 blocks is under **Misfortune**: it stumbles (a half-second root every 3 seconds), its attacks and aimed abilities misfire 20% of the time, and those below 25% health have their future narrowed to one outcome: they take 50% more damage. Allies in range get Luck and the first hit on each of them is halved. |
| Heavy Sphere | Heavybringer (4) | 6000 / 30 s | The air around the enemy you look at (24 blocks) turns dense: a dark transparent sphere (6 blocks) closes and cuts the Spirit World inside from outside for 6 seconds. No teleport in or out (an enemy in the middle of one is yanked out of it), no concealment, and projectiles stop at the shell. |
| Series of Rays | Heavybringer (4) | 6000 / 15 s | Dark-green light in your hands descends as a series: six Weakening Rays in 1.2 seconds, in a narrow fan on the enemy you look at. Each stacks Weakened; at three stacks its skin melts (visual) and it takes very heavy damage over the next 5 seconds. |
| Remote Sight | Heavybringer (4) | 2450 / 20 s | For 15 seconds you perceive far past your sight: every living thing within 64 blocks shows through walls with its health, and every door, passage and teleport destination shows as a spatial connection, including where an enemy's teleport is about to lead. |

## Key 7 wheel: Cosmos

| Entry | Sequence | Cost / cooldown | What it does |
|---|---|---|---|
| Fusion Cluster | Radiant Angel (2) | 25000 / 14 s | Three magnetically confined fusion clusters condense around you, circle you for 1 second, then fly at the enemy you look at (or where you aim) in a fan. Each bursts in a 5 block radius for heavy damage and burning light. |
| Radiant Angel Form | Radiant Angel (2) | 25000 / 90 s | Your Mythical Creature Form: for 45 seconds you are a being of starlight with six pairs of wings, 3 blocks tall. You can fly, you have Strength II and Resistance II, and every enemy within 12 blocks is burned by your glow (light damage every half second). Fusion Cluster and Star Grazing hit 25% harder. When it ends you are weakened for 10 seconds. |
| Gravitational Time Dilation | Star Dragon (1) | 40000 / 45 s | Heaviness causes slowness, and gravity drags on time itself. Over a 16 block field where you aim, enemies move, attack and fall at a quarter of their speed for 8 seconds and their projectiles hang in the air. You and your allies move normally. Bosses are slowed to half. |
| Planetfall | Star Dragon (1) | 40000 / 120 s | A planet-scale impact, made to fit: a 24 block circle opens where you aim (40 blocks) and a huge asteroid lowers through the sky for 4 seconds, with a warning that grows. On impact: huge damage in the circle (about 60% of a player's maximum health; it kills most mobs), a blinding flash, everything thrown outward, and cracks of white light that hurt for 10 seconds. Allies in the circle take a quarter. The ground is never changed. |
| Star Dragon Form | Star Dragon (1) | 40000 / 120 s | Your Mythical Creature Form: for 60 seconds you are a colossal transparent dragon of starlight (drawn 10 blocks long; your hitbox is 4 tall). You fly, take 40% less damage, and a small asteroid circles you: when you attack, it launches at the enemy you look at (heavy damage, 3 second recharge). Your rays are thicker and the Radiant Angel glow burns at half strength. Ends with 15 seconds of weakness. |
| Neutron Star | Condenser (0) | 60000 / 60 s | Your body takes the density of a neutron star for 12 seconds. You cannot be moved, pulled, rooted, frozen or teleported by anyone else, and take half damage. Your weight crushes: enemies within 6 blocks are pressed down and take crushing damage every half second. The weight reaches Fate as well: everyone in the field is under Fate Convergence's Misfortune. |
| Singularity | Condenser (0) | all spirituality (at least 40000) / 180 s | Self-sacrifice. You gather for 3 seconds (all light drains toward you and you cannot move), then a black hole opens where you aim (30 blocks, or on yourself if you aim at nothing) for 8 seconds. Everything within 24 blocks is dragged in, harder the closer it is; anything within 4 blocks takes crushing damage (15% of its maximum health per second). Projectiles and rays bend into it. Allies are not pulled. The price: all your spirituality and all but one heart of your health, and when it closes you are weakened and cannot cast for 20 seconds. It never kills you. |
| Quantum Uncertainty | Condenser (0) | 45000 / 40 s | A partial authority over Quantum Mechanics. For 15 seconds a 20 block field around you is uncertain: enemies inside miss 40% of the time, their aimed abilities land up to 6 blocks off, and anything that teleports ends up at a random spot within 8 blocks of where it meant to go. You and your allies are unaffected. |

## Grows with the Sequence

These do not get their own key or entry. The same ability is stronger from the Sequence on.

| Ability | From | What changes |
|---|---|---|
| Gravity Abnormality | Tidal Scholar (5) | 12 block field. Aimed straight down at your own feet it lightens you instead: you fly freely for 6 seconds and the wave of air you shed hits everything within 4 blocks. |
| Gravity Abnormality | Heavybringer (4) | 16 blocks. Lifted enemies rise 6 blocks and are slammed down (falls count double); Pressed enemies below 50% health collapse inward for heavy extra damage. |
| Weakening Ray | Tidal Scholar (5) | It melts flesh: a Weakened target loses half its armor value and takes 20% more damage while it lasts. |
| Cosmic Void | Tidal Scholar (5) | Whatever you fire at someone inside manifests there: rays always land (never blocked, never dodged) for 25% more. |
| Cosmic Void | Heavybringer (4) | A true pocket: 6 blocks, the target is abnormally fragile (30% more damage taken), teleports are sealed, and it lasts 8 seconds. If the target leaves it falls in once more. |
| Cosmic Void | Star Dragon (1) | The highest form: near absolute zero and full of radiation, 10 blocks. Freezes for 2 seconds every 2 seconds and irradiates. |
| Void Passage | Heavybringer (4) | 40 blocks, and an ally within 3 blocks of you comes through with you. |
| Scaled Form | Heavybringer (4) | Heavy Body: 5 blocks tall, Resistance III, so heavy the ground around you looks pressed down, and enemies within 6 blocks are slowed. Lasts 45 seconds. |
| Heavybringing | Star Shepherd (3) | 24 blocks, and it also bends light-based things (your own rays curve to where you want). |
| Star Grazing | Radiant Angel (2) | 12 block radius, 60% more damage, the grazed star hangs for 60 seconds. |
| Star Grazing | Star Dragon (1) | Three asteroids in 1.5 seconds over the same zone. |
| Fission Pulse | Star Dragon (1) | It leaves an Electromagnetic storm for 10 seconds: arcs strike enemies in it every half second, projectiles stop, and metal armor shocks double. |
| Fusion Cluster | Star Dragon (1) | Inertial Confinement: the clusters no longer circle you, they implode on hit: 8 block radius, 50% more damage. |
| Fusion Cluster | Condenser (0) | No confinement needed: five clusters, no windup, half the cost. |
| Fate Convergence | Condenser (0) | Weight and Density on Fate itself: it lasts twice as long, cannot be cleansed, and works on bosses. |

## Passives

| Passive | From Sequence |
|---|---|
| **Physical Enhancement:** Strength I. **Stargazing:** standing still for 3 seconds under open sky restores spirituality, twice as fast at night | Astronomy Aficionado (9) |
| **Spiritual Intuition:** a soft chime and a ring on the ground when something starts hunting you from out of sight | Star Worshipper (8) |
| **Star's Embrace:** two extra hearts and half fall damage | Star Sacrificer (7) |
| **Sense of Passages:** hidden or invisible enemies within 12 blocks glow for you and cannot stay Unnoticed | Navigator (6) |
| **Starlight Scales:** Resistance I and half knockback; you swim and dive fast | Tidal Scholar (5) |
| **Dense Body:** abilities of Sequence 5 or lower cannot pull, lift or move you, and you take no fall damage | Heavybringer (4) |
| **Fission Touch:** your blows arc to the nearest enemy within 4 blocks for 25% of the damage | Star Shepherd (3) |
| **Radiance:** enemies within 8 blocks are slowly burned (light damage every second, double for undead). Only a glow is drawn, no real light blocks | Radiant Angel (2) |
| **Gravity Authority:** Heaviness effects from lower Sequences never take on you, and projectiles within 6 blocks fly at half speed | Star Dragon (1) |
| **Weight and Density:** knockback, roots, freezes and pulls from any lower Sequence never take on you, and your steps shake the ground (visual) | Condenser (0) |

## Built from (existing parts of the spell kit)

| Abilities | Existing parts |
|---|---|
| Star Chart, Fate Glimpse | `Spells.aimEnemy`, `Concealment.reveal`, `Unnoticed.end`, `Falter` for the dodges, `SpellFx.shockRing` and `ParticleShapes.burst` for the ripples, `ScreenImages` for the status pictures |
| Gravity Abnormality, Heavybringing, Time Dilation, Neutron Star | `Spells.pull` and `Spells.push`, `Spells.enemiesAround`, `Zone`, `Root` and `RootRestriction` (MOVE, JUMP, TELEPORT), `SpellFx.runeCircle` and `closingRing`, `Later.run` |
| Electromagnetic Attraction | `Spells.pull` on two targets, `Spells.strike`, `Root` for the stagger |
| Cosmic Void, Heavy Sphere | `Dome.sphere` (keepsEnemiesIn, stopsProjectiles, onBlock, onEnd, raise), `Falter`, `ParticleShapes.model` |
| All rays | the new `Ray` part below, `SlashFx.line` for the beams, `Corrosion` for no healing, `SpellFx.tether` for the eye and hand lines |
| Void Passage | `Spells.blinkSpot`, `Root` TELEPORT check, `Unnoticed.hide`, `FallGuard.protect` |
| Tidal Surge, Star Grazing, Planetfall, Fusion Cluster | `Shot` (the crest, the clusters), `SpellFx.warningCircle`, `ModelFx` key poses (the falling rock works like the needle in Grafting), `Later.run`, `ParticleShapes` |
| The three forms | `Physique.keep` (hearts, reach, knockback), `ModelFx` on the player, Hand Of God's Giant Form if one exists |
| Singularity | `Countdown` for the 3 second gather, `Spells.pull`, setting health directly (never lethal) |
| Quantum Uncertainty | `Falter`, `Spells.addUnchoosableCheck`, `Spells.keepEffect` |
| Passives | the tick event pattern of the other Pathways, `LivingHurtEvent` for Fission Touch, `Physique.keep` |

## New reusable parts worth writing first

In the order they unlock the most abilities. Each is written so another Pathway can use it.

1. **`status/Weight` (Heaviness).** Levels, ticks and a source. Slows, stops jumping and flight, makes falls hurt more and slows projectiles. `Weight.put`, `isHeavy`, `end`, plus an immunity hook like `Fear.addImmunity` (Dense Body, Gravity Authority, Weight and Density). Used by Gravity Abnormality, Tidal Surge, Scaled Form, Heavybringing, Time Dilation, Neutron Star.
2. **`combat/GravityField`.** A timed field that pulls, presses, lifts or slows what is in it, built on `Zone`. Something like `GravityField.at(center, radius).pulls(speed).presses().lifts(height).slows(factor).sparesAllies().lasts(ticks).raise(player, sl, name)`, with `isRaised` and `close` like `Dome`. Used by Gravity Abnormality, Heavybringing, Time Dilation, Neutron Star, Singularity.
3. **`combat/Ray`.** An instant beam (`Shot` is for flying models): origin, direction, range, width, pierces, a warning line before it fires, and an `onHit` per target. Used by all five rays and by the Dragon's asteroid shot.
4. **`StarDragonMarks`.** The small statuses on others, written like `PrinceOfAbolitionMarks` (`put`, `has`, `from`, `end`, `tag`): CHARTED, WEAKENED (stacks), DIMMED, MISFORTUNE, VOIDED, IRRADIATED. If other Pathways end up with the same marks (Weakened, Irradiated), lift it into a shared `Marks` class instead of copying.
5. **`combat/Skyfall`.** Warning circle, then a falling model, then a blast that only shows. Radius, damage, fall ticks, model, count and spread are parameters. Used by Star Grazing, Planetfall and the Dragon's asteroid.
6. **`combat/Placed`.** A registry of things left on the ground (owner, level, spot, expires, kind), generalizing the SpiritFlame list in Attendant Of Mysteries including its clean-up when a world unloads. Used by grazed stars, radiation zones and the Singularity.
7. **`combat/Orbit`.** Bodies that circle a player: count, radius, speed, model, and `release(index, direction)`. Used by Fusion Cluster and the Dragon's asteroid.
8. **`status/Form`.** One manager for timed transformations: model, size, reach, attributes, the aftermath (weakness) and clean-up on logout, death and losing the Pathway (the `clearState` pattern in Prince Of Abolition). Used by Scaled Form, Radiant Angel Form, Star Dragon Form, and Hand Of God's Giant Form if that is not one already.
9. **`Spells.expose(entity)`.** `Concealment.reveal`, `Unnoticed.end` and a Glowing effect in one call, pulled out of `favorableGround` in Prince Of Abolition. Used by Star Chart, Void Passage, Heavybringing, Sense of Passages.
10. **Sky helpers.** `Spells.underOpenSky` and `isNight` for Stargazing and Sunlight Ray.
11. **Sequence Might.** Prince Of Abolition has range, area, duration, attack and defense helpers (5% per Sequence). If this Pathway uses them, move them to a shared `SequenceMight` class instead of copying them.
12. **`Spells.holdLength(target, base)`.** The boss and player rule in one place (see "Shared rules"): used by Lifted, Cosmic Void, Heavy Sphere, Time Dilation and the Singularity's pull.

## Writing it: files and settings

- `StarDragon`: events, the seven key methods, passives and shared helpers, with the key list in the class comment like Attendant Of Mysteries.
- `StarDragonFx`: every look and sound, with a test command like `/abolitionfx` (written: `/stardragonfx`, see the end of this file).
- `StarDragonMarks`: the statuses above.
- `StarDragonGravity`: Gravity Abnormality, Heavybringing, Heavy Sphere, Time Dilation, Neutron Star, Singularity.
- `StarDragonRays`: Weakening, Piercing, Sunlight, Series, and the Dragon's rays.
- `StarDragonSky`: Star Grazing, Fission Pulse, Fusion Cluster, Planetfall.
- `StarDragonForms`: Scaled Form, Radiant Angel Form, Star Dragon Form.
- Every number becomes a named constant in a settings block at the top of the class that owns it, one group per ability, in the Attendant Of Mysteries style (cost in spirituality, ticks, blocks). Saved values use an `sd_` prefix.
- The first line of every wheel entry in the code is its Sequence, cost and cooldown, so the wheel can read them.

## Shared rules

- **Allies:** use `Spells.enemiesAround` and `isEnemy`. Allies are never hurt or held unless an entry says so (Planetfall hurts them for a quarter).
- **Bosses and players:** a mob with more than 150 maximum health (`BOSS_HEALTH` in Prince Of Abolition) gets holds at half length (Lifted, Cosmic Void, Heavy Sphere, Time Dilation, the Singularity's pull). Players get them at 70%. Fate Convergence is the exception from the Condenser on.
- **Warnings:** every sky ability shows a warning circle first, every ray a 0.4 second line.
- **Pulls and pushes** always go through `Spells.pull` and `Spells.push`, so knockback immunities and `Root` are handled in one place.
- **Caster gone:** when the caster dies, logs out or loses the Pathway, every field, form and timed effect of theirs ends (the `clearState` pattern).

## Assumptions to confirm

- The keys (G H J K L N M) and the wheel behavior (normal opens the wheel, sneak casts the last pick) are copied from Hand Of God.
- The Singularity is a sacrifice that never kills the caster: it costs all spirituality and all but one heart.
- Costs above Sequence 3 (25000, 40000, 45000 to 60000) are extrapolated, because Attendant Of Mysteries only goes up to 12000.
- Sea-flavored abilities (Tidal Surge, Scaled Form) work anywhere, not only near water.
- There is no pathway-wide resource like Attendant Of Mysteries' stacks. Easy to add later if wanted: a small Starlight meter that fills with every cast and is spent by the big sky abilities.

## What is built

All seven keys, their growth at stronger Sequences and the passives of Sequences 9 to 0, in `blasphemy_slate/beyonder_characteristics/star_dragon/`:

| Class | What is in it |
|---|---|
| `StarDragon` | The keys (`starDragonAstronomy` ... `starDragonCosmos`), the three wheels (`SACRIFICE`, `HEAVYBRINGER`, `COSMOS`), Star Chart and Fate Glimpse, every passive, the events, and what the abilities share (`hit`, `rayHit`, `costFor`, `pay`, `afterCast`, `ready`, `grown`) |
| `StarDragonGravity` | Gravity Abnormality, Electromagnetic Attraction, Heavybringing, the Heavy Sphere, Gravitational Time Dilation, the Neutron Star, the Singularity |
| `StarDragonRays` | The Weakening, Sunlight, Piercing and Series rays, and how a ray looks and how wide it is in the Star Dragon Form (`rayKind`, `rayWidth`) |
| `StarDragonVoid` | Cosmic Void, Void Passage, Fate Convergence, Remote Sight, Quantum Uncertainty |
| `StarDragonTides` | Tidal Surge, Scaled Form (and the Heavy Body) |
| `StarDragonSky` | Star Grazing, Fission Pulse, Fusion Cluster, Planetfall |
| `StarDragonForms` | Radiant Angel Form, Star Dragon Form and the Dragon's asteroid |
| `StarDragonMarks` | The marks on others: CHARTED, WEAKENED, MELTING, DIMMED, MISFORTUNE, VOIDED, IRRADIATED |
| `StarDragonFx` | Every look and sound (`/stardragonfx`) |

Every number is a constant at the top of its class. One line each in `PathwayAbilities.ALL` (keys 1 to 7) and in the key packets (`SKeyPressAbilityOneUsed` ... `SevenUsed`), a row in [SEQUENCES.md](SEQUENCES.md), `"Star Dragon"` in `SequenceCommands.PATHWAYS` (so `/sequence pathway` offers it and its Beyonder Characteristics exist), the passives in `SequencePassives`, the color in `DarkAngel.pathwayColor`, the icon in `PathwayHudIcon`. Set the Pathway with `/sequence pathway <player> "Star Dragon"`.

The reusable parts written for it (each in [SPELL_KIT.md](SPELL_KIT.md)): `status/Weight` (Heaviness, 1), `combat/GravityField` (2), `combat/Ray` (3), `StarDragonMarks` (4), `status/Form` (8, the one form manager, with `.flies()`, `.cutsDamage(...)` and `.every(ticks, ...)`), `Orbit.release` / `age` / `firstReady` (7: launching an orbit's body yourself), `Root.addImmunity` and `Freeze.addImmunity` (bodies that cannot be held), and in `Spells`: `isBoss`, `holdLength` (12), `addMoveImmunity` (what nothing moves) and `hostileShotsAround` (the projectiles a field slows, stops, hangs or bends). Not written because something already does it: `Spells.expose` (9: `status/Exposed` ends concealment, unnoticing and invisibility), the sky helpers (10: `ServerLevel.canSeeSky`, `isDarkOutside`, `Spells.inDaylight`), `combat/Skyfall` (5: the warning and the fall are `StarDragonFx.starGrazing` and `planetfall`, the strike a `Later.run`), `combat/Placed` (6: the grazed star, the radiation, the storm, the cracks and the black hole are each a `Zone` or `GravityField` on the spot), a damage-cut status for the forms (`ConjuredArmor` already is one), and the Sequence Might helpers (11: the abilities grow by fixed steps instead).

How it works where the tables above do not say:
- **Every Star Dragon hit** goes through `StarDragon.hit(player, target, damage)`: `Spells.strike`, 20% more for the first hit on what you Charted (`StarDragonMarks.spendCharted`). Every ray's hit goes through `StarDragon.rayHit`, which on someone in your own Cosmic Void from the Tidal Scholar is 25% more and lands through every shield (`Spells.strikeThrough`).
- **Costs** go through `StarDragon.costFor(player, cost, ability)`; keys pay through `pay`, wheel entries through `.costBy(...)`; both end in `afterCast`. Nothing changes them yet; `addCostRule` and `addAfterCast` are the hooks for the second half (or the Starlight meter, if it is wanted).
- **Cooldowns**: the single keys use `combat/Cooldowns` (Star Chart 3 s, Fate Glimpse 20, Void Passage 5, Piercing Ray 8, Tidal Surge 10, Scaled Form 3), checked after the target and before the cost. The wheels: a sneak recast of the last pick has no cooldown and costs twice.
- **Costs the plan could not have**: Fate Glimpse costs 350, not 600 (Sequence 8 holds 400), and Cosmic Void 1800, not 2450 (Sequence 7 holds 1980).
- **Star Chart**: `StarDragonMarks.CHARTED` lands at `StarDragonFx.CHART_LANDS`, 10 s: `status/Exposed` for as long (it cannot hide from anyone), its constellation drawn every 10 ticks for the one who charted it alone, and the calculated shot.
- **Fate Glimpse**: for 10 s the first two attacks on you that have an attacker (melee, projectiles, abilities: everything `LivingAttackEvent` gives an attacker; falls, poison and the void have none) are cancelled, and you step 1.2 aside where you fit (not while held). Areas count when they name an attacker: the game cannot tell aimed from not.
- **Gravity Abnormality**: a `GravityField` from `StarDragonFx.GRAVITY_RISEN` to the end of 5 s. Pressed: `Weight` 2. Lifted (looking more than 45 degrees up): held 3 up for `Spells.holdLength` (all of it on mobs, 70% on players), bosses only slowed. Tidal Scholar: 12 wide; looking more than 75 degrees down at ground within 2.5 of you, `Flight` for 6 s and an air wave (20, a shove) instead. Heavybringer: 16 wide, Lifted rise 6 and are slammed down at the end (`slamsDown`, `Weight` 3 for the landing, so the fall hurts about double), and a Pressed enemy under half health collapses once a cast for 80.
- **Electromagnetic Attraction**: with a second enemy within 12 of the one you look at (24), both are pulled together for up to 14 ticks; when they meet (or at the end) each takes 40 and is held from moving and attacking for 1 s (`holdLength`). Alone, you are pulled to it (not while held) and the blow is 40 and a shove.
- **Cosmic Void**: a `Dome.sphere` round the target only it cannot cross, `StarDragonMarks.VOIDED` (by you), `Blind`, Slowness II, for 4 s (`holdLength`). It ends when 40% of its maximum health was lost inside (`StarDragonVoid.hurt`), when it dies, or when you cast another. Heavybringer: 8 s, 6 wide, `Vulnerable` 30%, no teleporting. Star Dragon: 10 wide, `Freeze` for 2 s and `IRRADIATED` every 2 s.
- **Weakening Ray** (and the Series): a `Ray` from your eyes, its line 0.4 s first, the first enemy on it 40 and `WEAKENED` 6 s: `Corrosion` (2 a second, 1 more for each stack) and `NoHeal`, both washed off by a cleanse (`status/Cleansing`). Tidal Scholar: also `MELTING` (armor halved, `Vulnerable` 20%).
- **Sunlight Ray**: charged `StarDragonFx.SUNLIGHT_FIRES`, then a piercing `Ray` 2 wide: 80 (160 to the undead), `Blind` 2 s. Half the cost when your block above sees daylight (`Spells.inDaylight`).
- **Void Passage**: `Spells.blinkSpot` (24, 40 from the Heavybringer), out at `StarDragonFx.PASSAGE_ARRIVES`, `Unnoticed` for 3 s, `FallGuard`; the concealed, unnoticed and invisible within 12 of either door are `Exposed` for 3 s. Refused (free) while held (`Root` MOVE or TELEPORT) or with nowhere to go. Heavybringer: the ally nearest you within 3 comes through beside you.
- **Piercing Ray**: a piercing `Ray` 28 long from your eyes: 40 each, `DIMMED` 8 s (`Enfeebled` 25%, a player loses 2% of their spirituality a second).
- **Tidal Surge**: the crest rolls 18 in 20 ticks, catching what is within 3.5 of its middle sideways and 5 up: 80 once each, a shove, `Weight` 1 for 3 s. For the first half second the first melee blow on you is cancelled and its sender held from moving and attacking 1 s.
- **Scaled Form**: a `Form` for 30 s (45 as the Heavy Body), ended free by casting again: Strength II, Resistance II (III), reach +1 (+2.5), the Heavy Body 5 blocks tall (`Attributes.SCALE`) and slowing enemies within 6. Your melee blows in it make the target Heavy for 2 s. A stolen cast keeps it on the thief.
- **Heavybringing**: a `GravityField` round you, 16 (24 from the Star Shepherd), 8 s: pulled toward you at 0.12 a tick, the hidden `Exposed`, enemy projectiles in it bent toward you; within 4, Nausea, `Weight` 2 and no teleporting. Nothing moves you while it holds (`Spells.addMoveImmunity`, full knockback resistance). Bosses are only slowed.
- **Fate Convergence**: a `Zone` round you, 20, 10 s (20 from the Condenser), once a second: enemies under `MISFORTUNE` (`Falter` 20%, held from moving half a second every 3 s, and under a quarter of their health `Vulnerable` 50%); bosses only from the Condenser. Allies in it and you have Luck, and the first hit with an attacker on each of you in this cast is halved.
- **Heavy Sphere**: a `Dome.sphere` 3 round the enemy you look at, 6 s (`holdLength`), nobody but your side crossing either way and projectiles stopped at its shell, and a `Zone` inside it where nothing can teleport and nothing not on your side stays hidden.
- **Series of Rays**: needs an enemy in sight (30). Six rays from your hand in a fan 4 degrees apart, one every `StarDragonFx.SERIES_STEP`: 20 and a `WEAKENED` stack each; at three stacks, `MELTING` and `Bleeding` 30 a second for 5 s, once a cast.
- **Remote Sight**: 15 s; every second, for you alone, every living thing within 64 is ringed, and doors, trapdoors, gates and portals within 12 and where ender pearls in flight are headed show as dotted lines. Chat names how many there are and the strongest, with its health, when it opens.
- **Star Grazing** (key 6): the rock strikes at `StarDragonFx.GRAZE_LANDS`: 80 and a shove to every enemy in the 8 block circle. The grazed star is a `Zone` for 30 s that gives the players on your side under it (you too) 3% of their spirituality bar every 2 s; one at a time. Radiant Angel: a 12 block circle, 60% more, 60 s. Star Dragon: three asteroids half a second apart on the same circle, then one star. In the Radiant Angel Form, 25% more.
- **Fission Pulse** (key 6, sneak): needs an enemy in sight (24). It splits at `FISSION_SPLITS` where the enemy is (or where it died): 80 to every enemy within 3, each `IRRADIATED` 8 s, and a radiation `Zone` 3 wide for 8 s that keeps irradiating. Star Dragon: an electromagnetic storm 5 wide for 10 s instead: every half second an arc on a random enemy in it (10, twice on iron, chain, gold, copper or netherite armor) and every projectile of the other side in it stops (it is discarded).
- **Fusion Cluster**: three clusters in an `Orbit` round you for `FUSION_READY`, then all are released (`Orbit.release`) as a `Shot` each, in a fan 9 degrees apart at the enemy you look at (or where you aim): each bursts 5 wide for 80 where its flight ends. Star Dragon: no circling, they leave at once and implode 8 wide for 50% more. Condenser: five, at half the cost (an `addCostRule`). In the Radiant Angel Form, 25% more.
- **Radiant Angel Form**: a `Form` for 45 s: Strength II, Resistance II, flying, and every 10 ticks 20 to every enemy within 12 (40 to the undead). Cast it again to turn back (the wheel's `.free`: no cooldown, no cost). Then 10 s of weakness (`Form.aftermath`, and its picture).
- **Gravitational Time Dilation**: a `GravityField` 16 wide where you aim (30), from `DILATION_RISEN` to the end of 8 s: enemies in it get Slowness IV, Mining Fatigue III and a quarter of their gravity (`status/Gravity`), players for 70% of it; bosses only Slowness III. Projectiles of the other side that come into it keep a quarter of their speed and no gravity until it ends.
- **Planetfall**: the asteroid strikes at `PLANET_LANDS` on a 24 block circle where you aim (40): players take 60% of their maximum health, bosses 25%, everything else 300; allies in it a quarter of that (as damage of nobody, so pets do not turn on them); everything is thrown outward. Then the cracks of light: a `Zone` for 10 s, 10 a second.
- **Star Dragon Form**: a `Form` for 60 s: 2.2 times as tall (`Attributes.SCALE`), flying, 40% less damage (`Form.cutsDamage`), a wingbeat every 2 s in the air, and every 10 ticks the Radiant Angel's glow at half strength. The asteroid is an `Orbit` of one: when you swing at something (`AttackEntityEvent`, so an ability's hit never sets it off) it is released at the enemy you look at (24; else the one you swung at) for 80, and is back 3 s later. Rays are twice as wide and look like the Dragon's. Cast it again to turn back. Then 15 s of weakness.
- **Neutron Star**: 12 s. Nothing moves, roots (MOVE, JUMP, TELEPORT) or freezes you, no Heaviness takes, and half damage with no knockback (a `ConjuredArmor`, `steady`). From `NEUTRON_READY`: a `GravityField` 6 round you that presses enemies (`Weight` 3; bosses only slowed), does 10 every half second and puts `MISFORTUNE` on them.
- **Singularity**: costs 40000, then everything left. You gather for `SINGULARITY_GATHER` (a `Countdown`, held in place by your own `Root`: the Condenser's immunity lets it through while `isGathering`), then a black hole 2.5 over the ground where you aim (30; on yourself if you aim at nothing) for 8 s: a `GravityField` 24 wide pulling every enemy toward it up to 0.5 a tick, harder the closer (bosses half as hard, players 70%), 15% of the maximum health a second within 4, and projectiles of the other side bent in and swallowed. When it opens: spirituality 0, health down to one heart (never lower than it was, never killing), `AbilityLock`, Weakness and the Spent picture for 20 s. Dying or leaving while gathering: it never opens (the cost is gone).
- **Quantum Uncertainty**: a `Zone` 20 round you for 15 s: enemies in it miss 40% (`Falter`), and a teleport of anyone not on your side that would end in it (an ender pearl, chorus fruit, an enderman: `EntityTeleportEvent`, never a command) lands 2 to 8 blocks off, on the ground.
- **The passives**: Stargazing counts as still when your position has not changed by a quarter block in 3 s, and needs the open sky over your head. Sense of Passages ends `Unnoticed` on the hidden and draws them for you (the concealed stay concealed: they only show). Dense Body is immune to every push and pull, not only those of Sequence 5 or lower (the kit does not know who is pushing). Fission Touch arcs only off a real blow (a swing at that target in the same tick), never off an ability's hit or off its own arc. Radiance runs once a second (4, twice on the undead, within 8). Gravity Authority slows each projectile once, as it first comes within 6. Weight and Density's steps shake the ground every 1.5 blocks walked.

Simplified from the design, because the kit cannot tell it apart: Gravity Authority and Weight and Density ignore Heaviness, roots and freezes from every Sequence, not only weaker ones (as Dense Body does); the Neutron Star does not stop teleports done to you by others (the game has no event for being teleported by someone else); Time Dilation slows a mob's blows only through its movement (Mining Fatigue slows a player's swing, not a mob's); Quantum Uncertainty does not move where an enemy's aimed ability lands (abilities aim inside their own code).

## Adding to it

Every piece is written the same way, so a new one is a few lines in the right place.

- **A new wheel entry.** Write it as `static AbilityWheel.Result myEntry(ServerPlayer player, ServerLevel sl)` in the class whose theme it fits, with `MY_SEQUENCE`, `MY_COST` and `MY_COOLDOWN` (package-private) at the top of that class and its own comment block (what it does, in the constants' names). Add `.entry("my_entry", StarDragonFx.WHEEL_..., "My Entry", ...)` and `.costBy((player, cost) -> costFor(player, cost, "my_entry"))` to the wheel in `StarDragon`. Return `REFUSED` (after a chat line) when there is no target, so nothing is spent. A form or a toggle that is cast again to end it: end it, return `FREE`, and add `.free(player -> ...)` after the entry.
- **A new single key cast.** In the key's method in `StarDragon`; check the target first, then `Cooldowns.ready`, then `pay(player, COST, CD_KEY)`, then `Cooldowns.start`.
- **Something that grows with the Sequence.** A `..._SEQUENCE` constant and `StarDragon.grown(player, ..._SEQUENCE)` where the number changes (`grown`, not `isStarDragon`, so a stolen cast grows too).
- **A hit.** Always `StarDragon.hit(player, target, damage)` (or `rayHit` for a ray), so the Star Chart's calculated shot and the void's manifestation apply by themselves.
- **A cost that changes** (a Starlight meter, a form that makes rays cheaper). One `addCostRule((player, cost, ability) -> ...)` in a static block in `StarDragon`, keyed on the ability's name; `addAfterCast` runs after every cast that went off (to fill a meter).
- **A mark on others.** One line in `StarDragonMarks.Mark`, a case in `laid` and `ended`, and, if it acts over time, one in `second`. A picture for it is a `Status` in `StarDragonFx`.
- **A passive.** Once a second: a `public static void myPassive(Player player)` in `StarDragon` and `custom("Star Dragon", sequence, StarDragon::myPassive)` in `SequencePassives`. On a blow: in `onStarDragonHurt` or `onStarDragonSwing`. A rule that something never takes on you: `Weight`, `Root`, `Freeze`, `Spells` (`addMoveImmunity`) or `AbilityLock` `addImmunity`, in the static block of `StarDragonEvents`.
- **Something left on the ground, or held, or worn.** A `Zone` or `GravityField` named `star_dragon_...` (one per player and name: opening it again closes the old one), and a line in that class's `clearState`, so death and logout end it. A form is a `Form` named `star_dragon_...` (`StarDragon.clearState` ends any of them).
- **Its look.** A display in `StarDragonFx` (`/stardragonfx` lists it once it has a `Case` in `CASES`), its tick constants public, and the ability waits for them with `Later.run`.

## Looks, sounds and pictures

Everything the abilities need to look and sound right is in `StarDragonFx.java` (`beyonder_characteristics/star_dragon/`), and the abilities play it. It only shows things; the ability does the damage, holding, pulling, hiding and moving, at the ticks its constants name. `/stardragonfx all` plays every display in game, `/stardragonfx <name>` one of them (TAB lists the names; there are also `gravity_lifted`, `lightened`, `cosmic_void_frozen`, `heavy_body`, `star_grazing_three`, `em_storm`, `heaviness`, `passives` and `statuses`). Displays that need a target use the nearest mob within 12 blocks.

| Ability | Normal cast | Sneak cast |
| --- | --- | --- |
| Shared | Heaviness: `heavy(sl, target, ticks, tag)` / `heavyEnd`. Every ray: `rayWarning(sl, from, to, RayKind.X)` (the 0.4 s line; fire `RAY_WARNING` ticks later), then `rayFire(sl, from, to, RayKind.X, width)` (`WEAKENING`, `PIERCING`, `SUNLIGHT`, `DRAGON`) | |
| 1 Star Chart / Fate Glimpse | `starChart(sl, player, target)` (it settles at `CHART_LANDS`), `charted(viewer, target)` every 10 ticks while it lasts (the constellation, for the Aficionado alone), `calculatedHit(sl, target)` when the 20% is spent, `chartedEnd(viewer, target)` | `fateGlimpse(sl, player, ticks, tag)`, `glimpseDodge(sl, player, from, to)` per dodge, `fateGlimpseEnd` |
| 2 Star Sacrifice (wheel) | Gravity Abnormality: `gravityAbnormality(sl, center, radius, ticks, lifted, tag)` (open at `GRAVITY_RISEN`), `gravityTick` every second, `pressed(sl, enemy)`, `lifted(sl, enemy, height, ticks, tag)`, `gravityEnd`; Tidal Scholar `lightened(sl, player, ticks, tag)`, `airWave`, `lightenedEnd`; Heavybringer `slammed(sl, enemy, ground)`, `collapse(sl, enemy)`. Electromagnetic Attraction: `magnetPull(sl, a, b, ticks)`, `magnetCollide(sl, at)`, `hammerBlow(sl, player, target)`. Cosmic Void: `cosmicVoid(sl, target, radius, ticks, VoidDepth.X, tag)` (closed at `VOID_CLOSED`; `HALF_REAL`, `TRUE_POCKET`, `ABSOLUTE_ZERO`), `voidManifest`, `voidFreeze(sl, target, radius)` every 2 s, `voidEnd`. Weakening Ray: the shared ray with `RayKind.WEAKENING`, `weakened(sl, target, stacks, ticks, tag)` (one melting ring per stack), `weakenedEnd`, `melting(sl, target, ticks, tag)`. Sunlight Ray: `sunlightCharge(sl, player)` (fires at `SUNLIGHT_FIRES`), `RayKind.SUNLIGHT` 2 wide, `sunburnt(sl, target)`. Icons `WHEEL_GRAVITY_ABNORMALITY` ... `WHEEL_SUNLIGHT_RAY` | the last pick again |
| 3 Void Passage / Piercing Ray | `voidPassage(sl, who, from, to)` (out at `PASSAGE_ARRIVES`; call it for the ally too from Heavybringer), `voidForcedOut(sl, hidden)`, `unnoticed(sl, player, ticks, tag)` / `unnoticedEnd` | `piercingRay(sl, player, to)` (both eyes), `dimmed(sl, target, ticks, tag)` / `dimmedEnd` |
| 4 Tidal Surge / Scaled Form | `tidalSurge(sl, player)` (the crest rolls `SURGE_REACH` blocks in `SURGE_TICKS`), `surgeHit(sl, target)`, `surgeParry(sl, player, attacker)`, then `heavy(...)` on whoever it hit | `scaledForm(sl, player, ticks, heavyBody, tag)` (closed at `SCALES_ON`; Heavy Body at `HEAVY_BODY_SCALE`), `scaledBlow(sl, target)`, `heavyBodyTick` every second, `scaledFormEnd` |
| 5 Heavybringer (wheel) | Heavybringing: `heavybringing(sl, player, radius, ticks, tag)`, `heavybringingTick` every 10 ticks, `bent(sl, from, to)`, `heavybringingEnd`. Fate Convergence: `fateConvergence(sl, player, radius, ticks, tag)`, `misfortune(sl, target, ticks, tag)`, `stumble`, `misfire(sl, at)`, `narrowed(sl, target, ticks, tag)`, `luck(sl, ally)`, `fateConvergenceEnd`. Heavy Sphere: `heavySphere(sl, center, radius, ticks, tag)` (closed at `SPHERE_CLOSED`), `sphereBlock(sl, at)`, `yanked(sl, entity)`, `heavySphereEnd(sl, center, tag)`. Series of Rays: `seriesOfRays(sl, player)`, ray i at `SERIES_FIRES + i * SERIES_STEP` with `RayKind.WEAKENING`. Remote Sight: `remoteSight(sl, player, ticks, tag)`, `sighted(viewer, entity)` and `passage(viewer, from, to)` every second, `remoteSightEnd`. Icons `WHEEL_HEAVYBRINGING` ... `WHEEL_REMOTE_SIGHT` | the last pick again |
| 6 Star Grazing / Fission Pulse | `starGrazing(sl, ground, radius)` (warning circle, the rock in fire lands at `GRAZE_LANDS` with its dust; three calls 10 ticks apart from Star Dragon), `grazedStar(sl, above, ticks, tag)`, `grazedStarFeeds(sl, star, ally)`, `grazedStarEnd` | `fissionPulse(sl, player, target)` (splits at `FISSION_SPLITS`), `fissionBurst(sl, at, radius)`, `irradiated` / `irradiatedEnd`, `radiationZone(sl, ground, radius, ticks, tag)` / `radiationZoneEnd`; Star Dragon `emStorm(sl, ground, radius, ticks, tag)`, `emArc(sl, from, to)`, `stormStop(sl, at)`, `emStormEnd` |
| 7 Cosmos (wheel) | Fusion Cluster: `fusionCluster(sl, player, count, ticks, tag)` (ready at `FUSION_READY`), `fusionOrbit(sl, player, count, tag, age)` every tick (the spots are `fusionSpot`, the same numbers as an `Orbit` with `FUSION_RADIUS`, `FUSION_HEIGHT`, `FUSION_SPEED`), `fusionLaunch(...)` returns where the `Shot` leaves, `FUSION_FLYING`, `fusionBurst(sl, at, radius, implode)`. Radiant Angel Form: `radiantAngel(sl, player, ticks, tag)` (at `ANGEL_READY`), `radiance(sl, player, radius)` every 10 ticks, `radiantBurn(sl, target)`, `radiantAngelEnd`, then `formWeakness(sl, player, ticks, tag)`. Time Dilation: `timeDilation(sl, center, radius, ticks, tag)` (at `DILATION_RISEN`), `dilated(sl, target, ticks, tag)`, `timeHang(sl, at)`, `timeDilationEnd`. Planetfall: `planetfall(sl, ground, radius)` (strikes at `PLANET_LANDS`), `lightCracks(sl, ground, radius, ticks, tag)`, `crackBurn(sl, target)`, `lightCracksEnd`. Star Dragon Form: `starDragonForm(sl, player, ticks, tag)` (whole at `DRAGON_READY`, played at `DRAGON_SCALE`), `dragonBeat(sl, player, ticksLeft, tag)` while flying, `dragonAsteroid` / `dragonAsteroidOrbit` every tick / `dragonShot` (returns where the `Shot` leaves; `ASTEROID_FLYING`) / `dragonShotHit` / `dragonAsteroidBack`, the thicker rays with `RayKind.DRAGON`, `starDragonFormEnd`, then `formWeakness`. Neutron Star: `neutronStar(sl, player, radius, ticks, tag)` (at `NEUTRON_READY`), `neutronTick` every 10 ticks, `neutronCrush(sl, target)`, `neutronStarEnd`. Singularity: `singularityGather(sl, player)` (opens at `SINGULARITY_GATHER`), `singularity(sl, center, radius, ticks, tag)`, `singularityTick` every 5 ticks, `swallowed(sl, from, center)`, `singularityEnd(sl, center, tag)`, `spent(sl, player)`. Quantum Uncertainty: `quantumUncertainty(sl, player, radius, ticks, tag)`, `uncertain(sl, target)`, `quantumSlip(sl, from, to)`, `quantumEnd`. Icons `WHEEL_FUSION_CLUSTER` ... `WHEEL_QUANTUM_UNCERTAINTY` | the last pick again |
| Passives | `stargazing(sl, player)`, `intuition(viewer, from)` (for them alone, with its chime), `embraceLanding(sl, player)`, `senseOfPassages(viewer, hidden)`, `starlightScales(sl, player)`, `denseBody(sl, player)`, `fissionTouch(sl, from, to)`, `radianceAura(sl, player, radius)`, `gravityAuthority(sl, at)`, `condenserStep(sl, player)` | |

Every timed cast can show its picture and clock above the hotbar: `StarDragonFx.status(player, Status.X, ticks)`, and `statusEnd` to take it away (28 statuses: 14 for what the Star Dragon holds, 14 for what is done to others when they are players); `statusForever` for Scaled Form, which lasts until it is cast again. The Pathway's icon beside the hotbar is registered in `PathwayHudIcon` under `"Star Dragon"`.

Things to know when the abilities are written:
- The three forms are worn models (`modelOnTurning`). The player's own body is not hidden under them: it stands inside the Radiant Angel's light and the Dragon's transparent chest. The Star Dragon is played at `DRAGON_SCALE` (10.5 blocks long) with its pivot under its chest, where the player is.
- The Dragon's wingbeats are keys, and a model holds at most 64: `starDragonForm` beats four times and then glides; call `dragonBeat` (with the ticks the form still has) whenever it should beat again, for example every 2 seconds while flying.
- Fusion clusters and the Dragon's asteroid do not move by themselves: move them every tick (`fusionOrbit`, `dragonAsteroidOrbit`), as Knowledge Emperor's Star Guard does on top of `combat/Orbit`.
- `charted`, `sighted`, `passage`, `senseOfPassages` and `intuition` are seen by one player only (particles sent to them alone), so they show through walls for that player and for nobody else.
- The Heavy Sphere, the time field, the grazed star, the radiation zone, the storm, the cracks of light and the black hole are not stuck to anyone: end them at the spot they were shown, with the same tag.

What is in the repository, all of it generated by the scripts in `tools/model_gen/star_dragon/` (see "Star Dragon: ready-made displays" in [MODEL_FX.md](MODEL_FX.md)):

| What | Where | Made by |
| --- | --- | --- |
| 15 box-model files (a three-step model is three files): the asteroid and the fire round it, a grazed star, a fusion cluster in its magnetic rings, the Neutron Star with its pulsar beams, the Cosmic Void sphere and its frozen shell, the Heavy Sphere, the black hole, the Tidal Surge crest, the Scaled Form, the Radiant Angel with six pairs of wings, the Star Dragon (3 steps) | `models/model_fx/star_dragon/`, `textures/model_fx/star_dragon/` | `models.py` (+ `textures.py`) |
| 18 picture models: the astrolabe, the fate ring, the eye of stars, the gravity well (animated), the web of Fate, the Misfortune mark, the Weakened mark, the void door (animated), two rays (animated), the impact circle, the cracks of light, the radiation zone (animated), the field lines of the storm (animated), the time field (animated), the quantum field (animated), the accretion disc (animated), the Star Dragon's emblem | the same folders | `pictures.py` (run by `models.py`) |
| 18 wheel pictures (32 px), 28 status pictures (16 px), the Pathway icon | `textures/gui/ability_menus/star_dragon/`, `textures/gui/star_dragon/`, `textures/gui/pathway_icons/star_dragon.png` | `icons.py` |
| 8 particle pictures: `fx/star4`, `fx/chevron`, `fx/arc_0`, `fx/arc_1`, `fx/nucleus`, `fx/scale`, `fx/rock`, `fx/flare` | `textures/particle/fx/` | `particles.py` |
| 60 sounds: the stars charted, Fate glimpsed and a dodge, gravity pressing, letting go and slamming, the magnetic pull and collision, the void opening, closing and freezing, a ray taking aim, the Weakening and Piercing rays, flesh melting, the Void Passage, the Tidal Surge and its parry, scales on and off, sunlight gathering and the Sunlight Ray, Heavybringing, Fate Convergence, Misfortune, the Heavy Sphere and something stopping at it, the Series of Rays, Remote Sight, an asteroid falling and striking, a grazed star, nuclei loosening, fission, radiation, an electric arc, fusion clusters condensing, flying and bursting, the Radiant Angel, its Radiance, a form fading, Time Dilation and something hanging in time, Planetfall descending and striking, the cracks of light, the Star Dragon's roar, its wings and its asteroid launched, the Neutron Star and its crushing, the Singularity gathering, opening and closing, Quantum Uncertainty and something slipping, the warning chime, Stargazing, a heavy step | `sounds/star_dragon/`, `sounds.json`, subtitles in `lang/en_us.json`, registered in `sounds/ModSounds.java` (`STAR_DRAGON_...`) | `sounds.py` |
