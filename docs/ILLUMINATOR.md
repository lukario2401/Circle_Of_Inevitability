# Illuminator: ability design

The Illuminator Pathway is the Paragon Pathway of Lord of the Mysteries, the High Priestess tarot card: Savant (9), Archaeologist (8), Appraiser (7), Artisan (6), Astronomer (5), Alchemist (4), Arcane Scholar (3), Knowledge Magister (2), Illuminator (1), Paragon (0). Source: the Paragon Pathway and Paragon Pathway/Abilities pages of the Lord of the Mysteries wiki.

All of it is written: the seven keys with every growth they get at stronger Sequences, the passives of every Sequence, the melee combo, and the shared parts they lean on (see "What is built" and "New reusable parts"). The rest of this file is the design it is built from. Its looks and sounds are in `IlluminatorFx` (see "Looks, sounds and pictures" at the end).

Pathway name in code: "Illuminator".

## Theme

A scholar-engineer Pathway: it studies first, builds second and rewrites the rules last. Savants, Archaeologists and Appraisers read the fight (marks, weaknesses, hidden things). Artisans and Alchemists build what the fight needs (flasks, rifles, cannons, dolls, golems, a mechanical body). Astronomers and Arcane Scholars add starlight, gravity and Dissociation. Magisters, Illuminators and the Paragon change the rules around them: gravity, combustion, history, and knowledge itself.

It is neither a tank nor a sprinter. Its strength is preparation: marks, placed things, summoned helpers and zones. Its answer to deceit is Analysis: it sees through illusions, concealment and substitutes (the Paragon Pathway counters Error and Visionary, and wipes the information tricks of the Hermit Pathway).

Colors: brass and copper for crafting and machines, blueprint blue for marks, lines and equations, pale steam white for fog and the Knowledge Spirit, parchment cream for pages and scrolls, ink blue-black for the Paragon's depth. Pathway color: 0xD9A441.

From Sequence 4 it can wear a mechanical body, and from Sequence 2 it can dissolve into white steam (its Mythical Creature Form is an informational scroll of civilizations).

Nothing here breaks blocks or lights real fire. Dissociation, Meteor Shower, the flasks and the cannons only show and hurt; the ground is never changed. "Combustion without flames" (Rewrite Principles) only changes damage numbers.

### The pathway mechanic: Appraised

Instead of stacks, the Illuminator works with a mark. An enemy you Appraise is **Appraised** (by you, for 12 seconds): it shows its health, the effects on it and its **weakness**, it cannot hide from you, and it takes 15% more damage from your abilities. Using the ability that matches its weakness on it does 50% more on top.

Starting weakness table (data, easy to change):

| Target | Weak to | Matching abilities |
|---|---|---|
| Undead, spirits, summoned things | Starlight | Star of Curses, Meteor Shower |
| Armored (iron or diamond armor), constructs | Corrosion | Compound Flask, Dissociation |
| Flying, fast, jumpy | Gravity | Blessing of Stars, Rewrite Principles |
| Everything else, and players by default | Pierce | Clockwork Rifle, Spirit Cannon |

Many abilities read the mark: Appraise puts it on, Exploit Flaw spends it, the Rifle and Cannon pick up the weakness, and the high Sequences strengthen it (see "Grows with the Sequence").

## How it's laid out

It follows the Attendant Of Mysteries pattern: seven keys (G H J K L N M). Keys 1 to 3 are single abilities with a normal cast and a sneak cast. Keys 4 to 7 are wheels like Hand Of God's and Apocalypse's, where every entry has its own Sequence, cost and cooldown, and sneak casts the last pick again (AbilityWheel.quickCast). Wheel entries are cast where you aim.

**Cooldowns.** The single keys have no cooldown: spirituality is their only limit (`Illuminator.KEY_COOLDOWNS = false`, through `Cooldowns.addFree`; set it to true to bring back the numbers below). The wheels keep their cooldowns when you pick from the wheel; a sneak recast of the last pick has no cooldown and costs 3 times as much (`Illuminator.QUICK_CAST_COST`). The Fusion Beam keeps its own 8 second cooldown.

The abilities do not each get a Sequence of their own. Sequences 9, 8, 7, 3 and 2 have two abilities, Sequences 6, 4, 1 and 0 have three, and Sequence 5 has four (the four things the Astronomer does in the books). Sequences 2, 1 and 0 are not left thin: they also strengthen what is already there (see "Grows with the Sequence"). Key 6 holds Sequences 4 and 3 together, key 7 holds Sequences 2, 1 and 0.

| Sequence | Abilities |
|---|---|
| Savant (9) | Compound Flask, Recall |
| Archaeologist (8) | Survey, Ritual Circle |
| Appraiser (7) | Appraise, Exploit Flaw |
| Artisan (6) | Clockwork Rifle, Spirit Cannon, Mystic Brooch |
| Astronomer (5) | Star of Curses, Blessing of Stars, False Constellations, Meteor Shower |
| Alchemist (4) | Living Doll, Golem, Mechanical Body |
| Arcane Scholar (3) | Dissociation, Knowledge Spirit |
| Knowledge Magister (2) | Rewrite Principles, Colossus |
| Illuminator (1) | Scrolls of Civilization, Mathematical Maze, Masterwork |
| Paragon (0) | Reality Knowledge, Illusory Book, Mechanical Heaven |

## Keys

| Key | Ability | Sequence | Normal cast | Sneak cast |
|---|---|---|---|---|
| 1 | Compound Flask | Savant (9) | **Compound Flask:** throw a glass flask of nitroglycerin in aqua regia, 24 blocks. It bursts on the first enemy or block it meets: 36 damage to the enemy it hits, 18 to everything else within 3 blocks, and a small shove. Everything hit is Corroded for 8 seconds (takes 15% more damage). | **Recall:** you remember where you stood 3 seconds ago and step back to it, same facing. You get back half of the health you lost in those 3 seconds and take no fall damage on arrival. Fails (nothing spent) if the spot is blocked now or you are held in place. |
| 2 | Survey | Archaeologist (8) | **Survey:** a pulse of blueprint light spreads 40 blocks. For 8 seconds every hostile mob and enemy player in it is outlined through walls, concealed enemies are forced out of hiding for 3 seconds, and the strongest thing in range shows its health. | **Ritual Circle:** a chalk circle of 6 blocks lies at your feet for 20 seconds. Allies inside get Regeneration I and one harmful effect cleansed every 3 seconds; enemies inside get Slowness I. **Rite:** Workshop abilities (key 4) cast from inside last 50% longer and cost 20% less. One circle at a time; casting again moves it. |
| 3 | Appraise | Appraiser (7) | **Appraise:** the enemy you look at (24 blocks) becomes Appraised for 12 seconds (see "The pathway mechanic"). A blue marker with its weakness floats over its head. | **Exploit Flaw:** spend the mark on the Appraised enemy you look at. A hairline of blueprint light finds the flaw: 90 damage that ignores armor, and a 1 second stagger. Constructs, dolls and golems are taken apart: stunned for 3 seconds, or destroyed if they have under 150 maximum health. With no mark on it: "You do not yet know its flaw." (nothing spent) |
| 4 | Workshop | Artisan (6) | The Workshop wheel: Clockwork Rifle, Spirit Cannon, Mystic Brooch. | The last Workshop pick again, without the wheel. |
| 5 | Astronomy | Astronomer (5) | The Astronomy wheel: Star of Curses, Blessing of Stars, False Constellations, Meteor Shower. | The last Astronomy pick again, without the wheel. |
| 6 | Alchemy and Arcana | Alchemist (4) | The Alchemy and Arcana wheel: Living Doll, Golem, Mechanical Body (4), Dissociation, Knowledge Spirit (3). | The last pick again, without the wheel. |
| 7 | Civilization | Knowledge Magister (2) | The Civilization wheel, cast where you aim: Rewrite Principles, Colossus (2), Scrolls of Civilization, Mathematical Maze, Masterwork (1), Reality Knowledge, Illusory Book, Mechanical Heaven (0). | The last pick again, without the wheel. |

## Key 4 wheel: Workshop

| Entry | Sequence | What it does |
|---|---|---|
| Clockwork Rifle | Artisan (6) | A brass rifle appears in your hands and fires 3 rounds, 2 ticks apart, at what your crosshair is on: 24 damage each, 48 blocks, very fast. Against an Appraised enemy the rounds carry its weakness (the matching bonus applies to the whole volley). The rifle fades after the last round. |
| Spirit Cannon | Artisan (6) | A brass cannon unfolds on the spot you aim at (12 blocks at most) and for 20 seconds fires a cannonball every 2 seconds at the nearest enemy within 24 blocks: 30 damage, a 2.5 block blast, half damage to the rest. One at a time; a new cannon replaces the old one. |
| Mystic Brooch | Artisan (6) | A brooch with a Beyonder effect pins itself to you, or to the ally you aim at, for 60 seconds. It absorbs the next 60 damage. When it is used up or runs out it bursts: enemies within 5 blocks are shoved away and take 20. (Made things last a limited time.) |

## Key 5 wheel: Astronomy

| Entry | Sequence | What it does |
|---|---|---|
| Star of Curses | Astronomer (5) | A ray of condensed starlight from your hand to the enemy you look at (32 blocks): 40 damage and the next curse on the ladder, if the same enemy was cursed in the last 12 seconds: first Weakness II (8 seconds), then Trauma (bleeding, 4 per second for 8 seconds, cannot heal), then Mutation (it deals 30% less, takes 20% more and glows for 10 seconds). After Mutation the ladder starts again. |
| Blessing of Stars | Astronomer (5) | Starlight gathers over the spot you aim at (28 blocks). For 8 seconds, inside 8 blocks: enemies are pressed down (Slowness III, cannot jump, 3 damage per second) and allies feel weightless (Jump Boost II, Slow Falling, Speed I). |
| False Constellations | Astronomer (5) | A fake starry sky opens over a 14 block circle where you aim (24 blocks) for 20 seconds. Your allies inside cannot be picked out from more than 6 blocks: mobs forget them, and abilities that need to see or read them (Appraise, Survey, Gaze, Divination) cannot choose them. A Meteor Shower cast inside it is 50% stronger. |
| Meteor Shower | Astronomer (5) | Astronomy, simulated: 8 meteors fall over 4 seconds at random spots within 8 blocks of where you aim (32 blocks). Each does 28 to enemies within 2.5 blocks and a small shove; allies take nothing. Streaks of light only, no real fire. |

## Key 6 wheel: Alchemy and Arcana

| Entry | Sequence | What it does |
|---|---|---|
| Living Doll | Alchemist (4) | A doll rises out of what you stand on and fights for you for 25 seconds: it follows you and attacks the enemy you last hit. It is made from the ground under you. Stone, deepslate, ores: **Stone Doll**, 80 health, slow, no knockback. Wood, leaves, grass, dirt (the default): **Straw Doll**, 30 health, fast, shoves everything within 3 blocks when it falls. Iron, copper, rails, metal blocks: **Iron Doll**, 50 health, throws bolts (14 damage). Sand, gravel, clay: **Dust Doll**, 20 health, its hits blind for 2 seconds. Snow, ice: **Frost Doll**, 30 health, its hits slow. |
| Golem | Alchemist (4) | Brass and iron fly together over 4 seconds (a countdown, silent, with gears) into a golem: 200 health, hits for 20, taunts the enemies within 10 blocks, stays 90 seconds. One at a time. Gone if you get more than 40 blocks away. |
| Mechanical Body | Alchemist (4) | Brass plates and gears cover you for 40 seconds: Resistance I and 20 absorption, hold jump to rise on a jetpack (5 seconds of fuel, refilled on the ground), cybernetic eyes (Night Vision, and Appraise and Survey reach +50%), and the Clockwork Rifle becomes an arm cannon: no cooldown between volleys and half the cost. Cast again to put it away early. |
| Dissociation | Arcane Scholar (3) | A clear, misty beam from your eye, 24 blocks, 1.5 blocks wide: 70 damage along the line. Constructs, dolls and golems it touches are destroyed (not those over 150 maximum health). Shields, Substitutes and concealment on what it hits end. Armor pieces of what it hits lose a quarter of their durability. Blocks it crosses flash and shed pale fragments, their color draining away, and are not changed. |
| Knowledge Spirit | Arcane Scholar (3) | You turn into drifting pages for 4 seconds: hidden, untargetable, fast. You fly to the enemy you look at (30 blocks) and enter its mind, then step out beside it in a burst of pages, hidden for 2 more seconds. It is Eroded for 8 seconds: nausea, it deals 30% less and its abilities cost 50% more. If no enemy is in view, you step out where you aim instead (20 blocks), like a jump. |

## Key 7 wheel: Civilization

| Entry | Sequence | What it does |
|---|---|---|
| Rewrite Principles | Knowledge Magister (2) | A 14 block circle where you aim (28 blocks) for 12 seconds: the earth bears no weight. Enemies inside are lifted off the ground (they cannot sprint, jump to dodge or land a combo) and fall without damage when it ends; allies get Slow Falling and Speed I. Combustion makes no flame: fire and explosions from enemies do not hurt allies inside, and yours still hurt enemies. |
| Colossus | Knowledge Magister (2) | A 6 block brass and steam mech assembles around you at a gesture, for 40 seconds: Strength III, Resistance II, 40 absorption, longer reach, every step shakes the ground and slows enemies within 6 blocks. While it stands, casting this again fires the **Fusion Beam**: an instant line of white-gold light, 40 blocks, 4 blocks wide, 120 damage, 8 second cooldown. It ends early when its absorption is gone. |
| Scrolls of Civilization | Illuminator (1) | Murals of human history unfold in a ring of 5 blocks around you for 10 seconds: projectiles stop at the ring and melee from outside does 50% less. When it ends, the scrolls fly outward: every enemy they hit is **immersed** in a vision of a lost civilization, held for 3 seconds and hurt for 40. |
| Mathematical Maze | Illuminator (1) | Aimed at an enemy: a lattice of equations closes around it. For 8 seconds it is held inside a 3 block cube (touching the lattice hurts 10 and pushes back), and lost in thought: it cannot use abilities. Aimed at an ally: **Enlightenment** for 30 seconds: Haste II, abilities cost 20% less, harmful effects cleansed. |
| Masterwork | Illuminator (1) | You conduct a symphony for up to 10 seconds (casting anything else stops it). Everyone within 20 blocks hears it. Allies: Strength I, Haste I, Regeneration I. Enemies: Slowness II, Weakness I, cannot sprint. |
| Reality Knowledge | Paragon (0) | Aimed at an enemy (32 blocks): knowledge rejects it for 10 seconds (5 for bosses). A player cannot use Beyonder abilities ("You cannot remember how"); a mob loses its ranged and special attacks and can only fight in melee. It also ends information abilities that are running on you (Gaze, Divination, Eye-type reads). |
| Illusory Book | Paragon (0) | Aimed at an enemy (32 blocks): it, and up to two others within 4 blocks of it, are turned into words and written into a floating book. They are gone from the world for 8 seconds (5 for players): hidden, untouchable, unable to act or heal. Then they step out where the book hangs, unwritten: 20% of their current health lost. Bosses cannot be written. |
| Mechanical Heaven | Paragon (0) | The sky turns white-gold, slow gears and steam turn overhead, and a Divine Kingdom of Mechanical Heaven covers 40 blocks around you for 60 seconds. Your constructs (dolls, golem, cannons, Colossus) are 1.5 times stronger and do not run out. Allies spend 30% less on abilities. Enemies get Slowness I, their cooldowns are 50% longer, their armor and weapons lose 1 durability per second, and one of their projectiles collapses into static every 2 seconds. Cast again to end it early. |

## Grows with the Sequence

These do not get their own key or entry. The same ability is stronger from the Sequence on.

| Ability | From | What changes |
|---|---|---|
| Compound Flask | Artisan (6) | Two flasks per throw, the second 4 ticks after the first. |
| Compound Flask | Arcane Scholar (3) | The burst leaves an acid puddle (3 block radius, 5 seconds) that corrodes whoever stands in it. |
| Recall | Astronomer (5) | Goes back 5 seconds. |
| Recall | Illuminator (1) | 8 seconds, and it also works on the ally you aim at: they step back instead. |
| Survey | Astronomer (5) | Reach 64 blocks. |
| Survey | Arcane Scholar (3) | Environment Controller: also shows where Beyonder abilities were cast in the last 60 seconds, with the caster's pathway and Sequence (needs Residue, see new parts). |
| Ritual Circle | Alchemist (4) | The Rite bonus covers the Alchemy and Arcana wheel as well. At Arcane Scholar (3) the circle grows to 10 blocks. |
| Appraise | Knowledge Magister (2) | Analysis: it sees the essence. Substitutes, concealment and disguises on an Appraised enemy are stripped, and the mark gives +25% instead of +15%. |
| Appraise | Paragon (0) | The mark lasts 24 seconds, and the Appraised enemy can be Exploited from anywhere you can see it. |
| Exploit Flaw | Arcane Scholar (3) | Also strips Substitutes and shields from the target. |
| Exploit Flaw | Illuminator (1) | Hits every Appraised enemy within 24 blocks at once, each for 60% of the damage. |
| Clockwork Rifle | Alchemist (4) | Five rounds per volley instead of three. |
| Spirit Cannon | Alchemist (4) | Two cannons at once. |
| Living Doll | Arcane Scholar (3) | Two dolls. One of them looks like you (a decoy: mobs pick it first). At Magister (2) three dolls. |
| Golem | Arcane Scholar (3) | Two golems. At Paragon (0) a Golem has no timer. |
| Mechanical Body | Knowledge Magister (2) | Mythical Creature Form: cast it again to become white steam for 4 seconds: hidden, untouchable, fast along the ground, and you re-form where you aim (24 blocks at most). |
| Mechanical Body | Illuminator (1) | No timer: it stays until you cast it again. |
| Dissociation | Knowledge Magister (2) | The beam is 3 blocks wide. |
| Dissociation | Paragon (0) | Information Shredder: also wipes what the target is showing: Appraised marks of others, Unreadable, concealment, and Hermit-style information tricks. |

## Passives

| Passive | From Sequence |
|---|---|
| Quick Study (Recall): an ability you cast again within 20 seconds of the last time costs 10% less | Savant (9) |
| Field Physique: two extra hearts, and 25% less damage from the environment (falling, drowning, cactus, suffocation) | Archaeologist (8) |
| Appraiser's Eye: you always see the health of the enemy you look at, and Beyonder items you hold show their full powers and drawbacks | Appraiser (7) |
| Craftsman's Hands: Workshop (key 4) abilities have 15% shorter cooldowns and 25% longer durations | Artisan (6) |
| Starlight: under an open night sky your spirituality comes back 20% faster and you see in the dark | Astronomer (5) |
| Constructor's Bond: your dolls, golems and cannons heal 1% per second while near you, and you see them through walls | Alchemist (4) |
| Absorption: every enemy within 6 blocks loses 1 health per second and you gain half of it (2 health per second at most) | Arcane Scholar (3) |
| Analysis: concealed, disguised and substituted things are plain to you within 16 blocks; Substitute figurines show as figurines | Knowledge Magister (2) |
| Enlightenment Aura: allies within 16 blocks spend 10% less spirituality, and confusion, blindness and nausea on them wear off in half the time | Illuminator (1) |
| Reality Knowledge: your Appraise marks last twice as long, and nobody can erase or read what you know (Reality Knowledge and information abilities do not take on you) | Paragon (0) |

## Melee combo

Hitting mobs as the Illuminator, registered in combat/MeleeCombos like the others. Colors: parchment cream front 0xF3E6C4, brass middle 0xD9A441, ink blue-black fading tail 0x1B2A41.

| Hit | What it does |
|---|---|
| 1 | A brass ring of gear teeth sweeping around you |
| 2 | A tilted ring of drifting pages around you, sweeping the other way |
| 3 (a or b at random) | (a) A blueprint spiral lying on the ground and winding outward, like a compass drawing. (b) Two crossed lines of equations in an X on the target, with a small lattice that flashes around it. |

## Numbers to start from: cost and cooldown

Recall, Survey and Ritual Circle cost 300, 350 and 400 rather than the planned 400, 600 and 900, so an Archaeologist can pay them (Sequence 8 holds 400); the code's constants are the truth now. Costs follow the Attendant Of Mysteries scale (250 at Sequence 9, 12000 at Sequence 3) and keep climbing; check them against the spirituality pool each Sequence gives. A cooldown of 0 means "cost only", like Attendant. Damage numbers are in the tables above. A boss is anything with more than 150 maximum health (the same line Marionette uses).

| Ability | Sequence | Cost | Cooldown (s) |
|---|---|---|---|
| Compound Flask | 9 | 250 | none (was 2) |
| Recall | 9 | 300 | none (was 25) |
| Survey | 8 | 350 | none (was 15) |
| Ritual Circle | 8 | 400 | none (was 25) |
| Appraise | 7 | 1250 | none (was 3) |
| Exploit Flaw | 7 | 1500 | none (was 10) |
| Clockwork Rifle | 6 | 1800 | 5 |
| Spirit Cannon | 6 | 2450 | 30 |
| Mystic Brooch | 6 | 1500 | 40 |
| Star of Curses | 5 | 2500 | 3 |
| Blessing of Stars | 5 | 3000 | 20 |
| False Constellations | 5 | 3500 | 40 |
| Meteor Shower | 5 | 5000 | 25 |
| Living Doll | 4 | 4000 | 20 |
| Golem | 4 | 6000 | 60 |
| Mechanical Body | 4 | 6000 | 60 |
| Dissociation | 3 | 9000 | 12 |
| Knowledge Spirit | 3 | 12000 | 25 |
| Rewrite Principles | 2 | 15000 | 40 |
| Colossus | 2 | 20000 | 90 (the Fusion Beam 8) |
| Scrolls of Civilization | 1 | 24000 | 30 |
| Mathematical Maze | 1 | 28000 | 30 |
| Masterwork | 1 | 30000 | 60 |
| Reality Knowledge | 0 | 40000 | 30 |
| Illusory Book | 0 | 45000 | 45 |
| Mechanical Heaven | 0 | 60000 | 120 |

## Built from the existing kit

| Ability | Existing parts | New parts it needs |
|---|---|---|
| Compound Flask | Shot (flat throw for now), Spells.enemiesAround, strike, push, ParticleShapes.burst, SpellFx.shockRing | Vulnerable (Corroded), Shot arc |
| Recall | FallGuard.protect, Root.has(TELEPORT), Spells.freeSpotNear, ParticleShapes (pages) | Recall buffer |
| Survey | SpellFx.shockRing, Concealment.reveal, the Glowing effect for outlines | none |
| Ritual Circle | SpellFx.runeCircle, Spells.enemiesAround, Spells.cleanse, Later.run pulses | Deployable (Zone) |
| Appraise | Spells.aimEnemy, ParticleShapes.modelOn (marker), ScreenImages, Concealment.reveal | Appraisal, Weakness table |
| Exploit Flaw | Spells.aimEnemy, strike, ParticleShapes.slashBetween, Root.apply | Appraisal, Construct tag |
| Clockwork Rifle | Shot with windup (like the juggled daggers), Spells.aimFrom, SlashFx trail | Held model (optional), Weakness table |
| Spirit Cannon | The SpiritFlame pattern (record list plus Later.run), Shot, Spells.enemiesAround | Deployable |
| Mystic Brooch | The Absorption effect, ParticleShapes.modelOn, Later.run, Spells.push | none |
| Star of Curses | Spells.aimEnemy, ParticleShapes.slashBetween, SpellFx.tether, persistent data (like the juggle count) | Vulnerable (Mutation), Beam helper |
| Blessing of Stars | SpellFx.runeCircle, warningCircle, Spells.pull, Later.run | Zone, Gravity |
| False Constellations | SpellFx.runeCircle, a ModelFx dome, Later.run | Zone, Unreadable |
| Meteor Shower | Shot (downward), ParticleShapes.modelAlong, SpellFx.shockRing, Later.run | none |
| Living Doll | Marionette.control, ParticleShapes.modelOn, Later.run, the block under your feet | Construct (with the material table) |
| Golem | Countdown (silent), Marionette.control, ParticleShapes | Construct |
| Mechanical Body | MobEffects, FallGuard, ScreenImages status, ParticleShapes.modelOn | Jetpack (jump-hold fuel), Held model |
| Dissociation | ParticleShapes.slashBetween, Spells.enemiesAround, Substitute and Concealment ending | Beam helper, Construct tag |
| Knowledge Spirit | Concealment.hide, Shot (pages), Spells.freeSpotNear, ParticleShapes | Ability lock (Eroded) |
| Rewrite Principles | SpellFx.runeCircle, Spells.enemiesAround, Later.run | Zone, Gravity |
| Colossus | ParticleShapes.modelOn, MobEffects, FallGuard, SpellFx.shockRing | Beam helper |
| Scrolls of Civilization | ModelFx ring, Shot-like outward flight, Root.apply | Deployable that follows you |
| Mathematical Maze | ModelFx lattice, SpellFx.tether, Root.apply, Spells.cleanse | Ability lock |
| Masterwork | Countdown, SpellFx.shockRing pulses, MobEffects | Zone on the caster |
| Reality Knowledge | Spells.aimEnemy, ScreenImages | Ability lock (Forgotten) |
| Illusory Book | Concealment.hide, Root.apply, a ModelFx book, Later.run | Concealment on mobs (check) |
| Mechanical Heaven | SpellFx.runeCircle, a ModelFx sky dome, Later.run, MobEffects | Zone, Ability lock, Construct bonus |

## New reusable parts

Written (each has a section in [SPELL_KIT.md](SPELL_KIT.md)):

| Part | Where | What it does | Used by |
| --- | --- | --- | --- |
| `Appraisal` | `status/` | One caster's mark on an entity: its weakness (a word), how long, and a bonus on every **ability** hit of that caster on it, `MATCH_BONUS` (50%) more when the hit's kind is the weakness. `mark`, `isMarked`, `weakness`, `spend`, `wipe`, `markedBy`, `strike` / `strikeTrue` / `as(kind, ...)` (hits counted as ability hits of a kind), `addListener` (the look). | Appraise, Exploit Flaw, every Illuminator hit (`Illuminator.hit`); Hand Of God's Eye of Demon Hunting could use it |
| `WeaknessTable` | `status/` | What something is weak to, as data: `otherwise("pierce").rule("starlight", one -> ...)...`, the first rule from the top that fits. Each Pathway keeps its own. | `Illuminator.WEAKNESS`; Hand Of God's Alchemy coating could use one |
| `Exposed` | `status/` | Cannot hide for a while: `Concealment`, `Unnoticed` and invisibility end every tick and cannot take hold. `addException` for hiding that must be left alone (Exile is). | Survey, Appraise; Analysis (2) |
| `Enfeebled` | `status/` | Deals less damage for a while, named reasons, the biggest counts. The other half of `Vulnerable`. | Mutation; Eroded (3) |
| `Gravity` | `status/` | Heavier or lighter through the game's gravity attribute, named reasons that multiply; below 0 lifts. | Blessing of Stars; Rewrite Principles (2), Mechanical Heaven (0) |
| `Unreadable` | `status/` | Cannot be picked out from further than N blocks: far mobs forget, far abilities cannot choose (`Spells.addUnchoosableCheck`). | False Constellations |
| `Construct` | `status/` | What is made, not born: iron and snow golems, anything `mark`ed, `addCheck` rules. | Exploit Flaw; Dissociation (3); the dolls and golems (4) mark themselves |
| `History` | `combat/` | Where every player stood and how hurt they were, for the last 10 seconds. `ago`, `asFarAs`, `healthLost`, `forget`. | Recall (and Recall on an ally from 1) |
| `Cooldowns` | `combat/` | A cooldown on a single key with the wheels' "ready again in" message; `addFree` takes it away. | keys 1 to 3 (switched off), the Fusion Beam |
| `Spells.aimAlly` | `combat/` | The ally under the crosshair. | Mystic Brooch, Recall on an ally (1); Enlightenment (1) |
| `AbilityWheel.resetCooldown` | `menu/` | One entry ready again now. | the arm cannon (4) |

Planned parts that turned out to exist already: **Vulnerable** (`status/Vulnerable`: Corroded, Mutation), **Deployable** and **Zone** (`combat/Zone` is both: the Spirit Cannon is a Zone that fires on its own beat; the Ritual Circle, Blessing and False Constellations are Zones; `Zone.covers` is the Rite and the false sky's bonus), the **Beam helper** (`Spells.enemiesOnLine(player, sl, from, to, width / 2)` plus the beam models in `IlluminatorFx`), the **Held model** (`IlluminatorFx.rifle` and `armCannon` hold the rifle themselves), and the **Mystic Brooch's ward** (`status/ConjuredArmor`'s absorption).

Written for the second half:

| Part | Where | What it does | Used by |
| --- | --- | --- | --- |
| The building half of `Construct` | `status/` | `Construct.of(type).health(...).damage(...).speed(...).scale(...).lasts(...).leash(...).limit(group, max).shown().steady().onEnd(...).build(owner, sl, feet, yaw)`: a `Marionette` summon with that body, hidden under your model; `ofOwner`, `ownerOf`, `isBuilt`, `ticksLeft`, `extend`, `end`, `endAll`. | Living Doll, Golem; Constructor's Bond, Mechanical Heaven |
| `AbilityLock` | `status/` | No abilities for a while: a player's keys are refused (`Root` ABILITIES), a mob fights only in melee (its projectiles vanish, its other hits are stopped). `addImmunity`. | Mathematical Maze, Reality Knowledge |
| `Residue` | `combat/` | Where spirituality was spent on abilities lately (who, Pathway, Sequence, where, when), written by `SoulCore` itself. `near(sl, center, radius, maxAge)`. | Survey at Arcane Scholar |
| `Spells.addCostScale` / `costFor` | `combat/` | A rule for what every ability of every Pathway costs a player (`paySpirituality` and the wheels ask it). | Eroded, Enlightenment, the Aura, Mechanical Heaven |
| `Cooldowns.addScale` / `scaled` | `combat/` | A rule for every cooldown a player starts, single keys and every wheel. | Mechanical Heaven |
| `Appraisal.addImmunity` | `status/` | Nobody can mark what it names. | Reality Knowledge (0) |
| `DarkAngel.pathwayColor` | `beyonder_characteristics/` | Now public: a Pathway's color. | Survey's residues |

## Ideas left on the shelf

Ready to add later, not in the seven keys:

| Idea | From | Why it waits |
|---|---|---|
| Starship Barrage: a starship in the sky fires on every Appraised enemy within 48 blocks | Knowledge Magister (2) | Needs a second Sequence 2 entry; strong with Appraisal |
| Solar Storm: a wave of charged plasma that shuts down enemy constructs and ranged attacks | Astronomer (5) | Overlaps Meteor Shower for now |
| Tracking Disk: a disc that follows an Appraised enemy anywhere | Alchemist (4) | Needs a compass or marker on screen |
| Painting: a masterwork painting that draws the viewer in | Illuminator (1) | Overlaps Masterwork and the Maze |
| Mentor and Learning: deny an enemy the ability to learn, teach an ally an ability for a while | Magister (2), Paragon (0) | Needs a way to lend abilities |
| Civilization effects (clear corruption, rebuild a city, move people) | Illuminator (1) | Not combat, and the ground is never changed |
| Spirituality Gems | Alchemist (4) | Needs a living-being cost; left out |

## To confirm

- Decided while writing the first half: the Appraised mark, Corroded and the curses work on players too (as every other curse in the kit does), and their pictures show on that player's screen. A player is always weak to pierce.
- "Appraised: it cannot hide from you" is `status/Exposed` for the mark's time, so it cannot hide from anyone meanwhile. Making it private would need a per-viewer concealment (a client-side part).
- Outlines (Survey) use the glowing effect, so everyone sees them; a private outline would be a client-side part.
- The Flask flies straight (a fast lob): `Shot` has no arc yet. An arc would be a new `Shot` option (keys that rise and fall along the flight; `modelAlong`'s frame makes "up" sideways, so it needs `ModelFx.pose()` in world space first).
- Recall on an ally (1) works on players only: `combat/History` follows players. A pet would need a record of its own.
- Constructs are vanilla mobs run by Marionette and dressed with a ModelFx. If custom entities come later, only `Construct` changes.
- Costs and cooldowns are first guesses, to be balanced against the spirituality pool.
- The jetpack is creative-style flight while the Mechanical Body is worn (jump twice to take off), limited by fuel: the server cannot see the jump key being held without a new packet. A packet like `SParasiteControlPacket` would allow a true "hold jump to rise".
- Constructs are vanilla bodies (a husk for the dolls, an iron golem for the Golem), invisible and silent under their models, so they move and fight as those mobs do. After a server restart a construct is a plain summon that runs out.
- Mechanical Heaven's longer cooldowns reach every wheel and every key that uses `combat/Cooldowns`; the Pathways whose single keys keep no cooldown are not slowed by it.
- The Decoy doll looks like you by wearing copies of your armor and what you hold (the copies vanish with it); it does not have your skin.

## What is built

Every key, its growth at stronger Sequences, the passives from Savant (9) to Paragon (0) and the melee combo. Keys 1 to 3 and the passives are in `blasphemy_slate/beyonder_characteristics/illuminator/Illuminator.java`, the Workshop wheel (key 4) in `IlluminatorWorkshop.java`, the Astronomy wheel (key 5) in `IlluminatorAstronomy.java`, the Alchemy and Arcana wheel (key 6) in `IlluminatorAlchemy.java` and the Civilization wheel (key 7) in `IlluminatorCivilization.java` beside it. Every number is a constant at the top of its class. One line each in `PathwayAbilities.ALL`, a row in [SEQUENCES.md](SEQUENCES.md), `"Illuminator"` in `SequenceCommands.PATHWAYS` (so `/sequence pathway` offers it and its Beyonder Characteristics exist), the passives of Sequences 5 to 1 in `SequencePassives`, the combo in `MeleeCombos`, the color in `DarkAngel.pathwayColor`, the icon in `PathwayHudIcon`. Set the Pathway with `/sequence pathway <player> "Illuminator"`.

How it works where the tables above do not say:
- **Every Illuminator hit** goes through `Illuminator.hit(player, target, damage, Weakness kind)`, which is `Appraisal.strike`: on an enemy you Appraised it is 15% stronger (25% from Magister), and 50% more again when `kind` is its weakness. The kinds: the Flask and its acid are corrosion, the Rifle and Cannon pierce (a Rifle round on an Appraised enemy carries its weakness instead, so it always matches), the Star of Curses and Meteor Shower starlight, Blessing of Stars gravity. Exploit Flaw and the Brooch's burst have no kind (the bonus, never the match).
- **The weakness table** is `Illuminator.WEAKNESS`, read from the top: players are pierce; the undead, vexes, allays and every summon are starlight; constructs and anything with 8 armor or more are corrosion; whatever floats (no gravity), bats, bees, blazes, ghasts, parrots, breezes, slimes, magma cubes, rabbits, frogs, endermen and anything with a walking speed of 0.3 or more are gravity; the rest pierce.
- **Costs** go through `Illuminator.costFor(player, cost, ability, group)`: Quick Study (the same ability again within 20 s, 10% less), the Rite (a Workshop cast from inside your Ritual Circle, 20% less; from Alchemist also Alchemy and Arcana), then every `Illuminator.addCostRule`. Keys pay through `pay`, wheel entries through `.costBy(...)`; both end in `afterCast`, which Quick Study reads and every `addAfterCast` hears.
- **Cooldowns**: keys 1 to 3 use `combat/Cooldowns` (Flask 2 s, Recall 25, Survey 15, Ritual Circle 25, Appraise 3, Exploit Flaw 10), checked after the target and before the cost; a cast that finds nothing to do starts none. While `Illuminator.KEY_COOLDOWNS` is false (now) a `Cooldowns.addFree` rule makes every `illuminator_` key free of them except `illuminator_fusion_beam`. The wheels' sneak recast: no cooldown, `QUICK_CAST_COST` (3) times the cost.
- **Compound Flask**: a `Shot` at 1.4 blocks a tick (so a flask can be seen coming), a drop of acid behind it every tick. The one it hits takes 36; the burst 18 to the rest within 3; everyone it caught is shoved and Corroded (`Vulnerable`, 15%, not stacking, 8 s; a green circle under them). Artisan: the second flask 4 ticks later, aimed anew. Arcane Scholar: a puddle of 3 for 5 s (a `Zone`), 4 a second and Corroded again while they stand in it; up to three puddles at once.
- **Recall**: `combat/History.asFarAs(player, 60)` (100 from 5, 160 from 1): where you stood that long ago, or as far back as it goes. Refused (free) when held (`Root` MOVE or TELEPORT), when you have not moved, or when that spot is blocked now. Half of the health lost since then comes back; no fall damage for 2 s; the record starts afresh so it cannot chain. From Illuminator, the player ally under the crosshair (24) is sent back instead of you.
- **Survey**: at `SURVEY_PULSE`, everything hostile within reach (a monster, a player not on your side, a mob hunting you) glows for 8 s and is `Exposed` for 3 (whatever hid is pulled out, with its look). Chat names how many and the one with the most maximum health, with its health and distance. Reach 40, 64 from Astronomer, half again while the Mechanical Body is worn (key 6 sets `MECH_UNTIL`).
- **Ritual Circle**: a `Zone` named `Illuminator.RITUAL`, opened when the chalk is drawn (`RITUAL_DRAWN`). Every second: enemies inside get Slowness I; allies inside, and you, Regeneration I; every 3 seconds each ally loses its first harmful effect. Casting again moves it. `Illuminator.inRite(player, group)` is the Rite, `Illuminator.lasting(player, ticks, group)` the longer durations (Craftsman's Hands and the Rite).
- **Appraise**: the mark lands at `APPRAISE_LANDS` (`status/Appraisal`, 12 s; 24 from Paragon), with the lens over the head in its weakness's picture, `Exposed` for as long, and a line in chat: health, effects, weakness and the abilities that match it. A player Appraised sees the Appraised picture. Magister: a ready Substitute is stripped as well. Appraising again renews the mark.
- **Exploit Flaw**: needs your mark on the enemy under the crosshair (24; 64 from Paragon); without one it costs nothing and says so. At `FLAW_LANDS`: 90 that nothing softens (`Appraisal.strikeTrue`, so the mark's bonus is in it), held from moving and attacking for 1 s, the mark spent. A construct (`status/Construct`) is held 3 s, or taken apart outright under 150 maximum health. Arcane Scholar: its Substitute goes first. Illuminator: every other enemy you Appraised within 24 is struck too, for 60%.
- **Clockwork Rifle**: hitscan, each round at what the crosshair is on at that moment (the volley follows your aim), 48 blocks, the first enemy on the line. 3 rounds, 5 from Alchemist. Through the Mechanical Body's arm cannon (`MECH_UNTIL`): it fires at once, half the cost, and the wheel's cooldown is reset after it (`AbilityWheel.resetCooldown`).
- **Spirit Cannon**: a `Zone` named `illuminator_cannon_0` (and `_1` from Alchemist) on the ground you aim at (12 at most), 20 s (25 with Craftsman's Hands, more under the Rite). Every 2 s after it unfolds, a cannonball (`Shot`, 1.6 a tick) at the nearest enemy within 24 that the barrel can see and that can be chosen: 30 to the one it hits, 15 to the rest within 2.5. A new cannon replaces the one with the least time left.
- **Mystic Brooch**: `status/ConjuredArmor` named `illuminator_brooch` with 60 absorption, on the ally under the crosshair (`Spells.aimAlly`, 24) or you; 60 s (75 with Craftsman's Hands). When the absorption is gone a tick after a blow, or the time runs out, or the wearer dies, it bursts: enemies within 5 are shoved and take 20. A new brooch on the same wearer replaces the old one quietly.
- **Star of Curses**: 40 at `CURSE_LANDS`. The ladder is kept per enemy (`IlluminatorAstronomy.LADDERS`): struck by the same caster within 12 s it climbs one step; the first strike and the one after Mutation are plain rays. Weakness II 8 s; Trauma: `Bleeding` 4 a second and `NoHeal` for 8 s; Mutation: `Enfeebled` 30% and `Vulnerable` 20% and glowing for 10 s.
- **Blessing of Stars**: a `Zone` from `BLESSING_RISEN` for the rest of 8 s, 8 wide, once a second. Enemies: Slowness III, no jumping (`Root` JUMP), 2.5 times as heavy (`status/Gravity`), 3 damage. Allies and you: Jump Boost II, Slow Falling, Speed I. Players inside see Pressed Down or Weightless.
- **False Constellations**: a `Zone` named `IlluminatorAstronomy.SKY`, 14 wide, 20 s. Your side inside, you too, is `Unreadable` from further than 6 (renewed every half second). A Meteor Shower whose middle is under it is 1.5 times as strong, 3.2 wide.
- **Meteor Shower**: 8 meteors, one every half second, on random ground within 8 of where you aim (32). Each lands at `METEOR_LANDS` after its warning ring: 28 and a shove to every enemy within 2.5.
- **Appraiser's Eye**: every 5 ticks, the name and health of the enemy under the crosshair within 24, as a `HudCounter` left of the hotbar (in its weakness's color once you Appraised it); it goes when you look away.
- **Field Physique**: two more hearts (`beyonder/Physique`, checked once a second, off again if the Pathway changes); fall, drowning, cactus, berry-bush, in-wall and fly-into-wall damage a quarter less.
- **Starlight**: at night under open sky, a fifth of your normal regeneration again every second, and Night Vision kept up.

**Key 6, Alchemy and Arcana** (`IlluminatorAlchemy.java`; every entry's cost through `Illuminator.costFor(..., Group.ALCHEMY)`, so the Rite of a Ritual Circle covers it, and every duration through `Illuminator.lasting`):
- **Living Doll**: a husk's body (three quarters size, invisible and silent) under the doll's model, built with `status/Construct` (group `illuminator_doll`, at most as many as you can raise: a new cast replaces the old dolls), 25 s, leash 40, 6 a blow. The material is the block under your feet (`IlluminatorAlchemy.materialUnder`): ice and snow are Frost; sand, gravel, clay Dust; iron, copper, gold, rails, chains, anvils Iron; stone, deepslate, ores, bricks, cobble Stone; everything else Straw. Stone 80 health, 0.7 speed, no knockback; Straw 30, 1.4 speed, its fall shoves enemies within 3; Iron 50, a bolt of 14 at what it fights (16 blocks) every 2 s; Dust 20, 1.2 speed, its blows blind 2 s; Frost 30, its blows give Slowness II for 3 s. Arcane Scholar: two, the second a decoy: a husk you can see wearing copies of your armor and what you hold, which the mobs within 10 are made to hunt (`Taunt`, renewed every 3 s). Magister: three.
- **Golem**: assembles in front of you over 4 s (`GOLEM_STANDS`), an iron golem's body under the model, 200 health, 20 a blow, no knockback, 90 s, leash 40; one at a time (two from Arcane Scholar; a new one replaces the oldest), no timer from Paragon. Every 3 s it calls the mobs within 10 to it (`Taunt`); its blows play `golemStrike`.
- **Mechanical Body**: `Illuminator.MECH_UNTIL` and `status/ConjuredArmor` named `illuminator_mechanical_body` with 20 absorption; 40 s, no timer from Illuminator. Resistance I and Night Vision kept up. Survey and Appraise reach half again and the Clockwork Rifle fires through the arm cannon (already in the first half). The jetpack is the game's own creative flight: while the body is worn you may fly (jump twice to take off, jump and sneak to rise and sink), 5 s of fuel in the air, refilled on the ground, no fall damage. Picking it from the wheel again takes it off (free). Knowledge Magister: a sneak recast while worn turns you to white steam for 4 s instead (`Concealment`, Speed III), and you re-form at `Spells.blinkSpot` where you aim then (24).
- **Dissociation**: from your eye, 24 long and 1.5 wide (3 from Magister), through blocks (the solid blocks it crosses flash, `blockFades`, and are not changed). It touches everything not on your side, the hidden too: their concealment, unnoticing and Substitute end, a raised shield drops, each armor piece loses a quarter of its durability, 70 (a corrosion hit). A construct under 150 maximum health is dissolved outright. Paragon, the Information Shredder: every mark on them is wiped (`Appraisal.wipe`), they are `Exposed` for 5 s, and a player forgets everything they had studied (`Study.forgetAll`).
- **Knowledge Spirit**: not while held. As pages (`Concealment`, Speed III) at 1.2 blocks a tick, 4 s at most, to the enemy under the crosshair (30); you step out beside it, still hidden 2 s more. Eroded for 8 s: Nausea, 30% less dealt (`status/Enfeebled`), and every ability of any Pathway costs it 1.5 times as much (`IlluminatorAlchemy.erodedCost`, a `Spells.addCostScale`). With no enemy in view, a jump to where you aim (20).

**Key 7, Civilization** (`IlluminatorCivilization.java`):
- **Rewrite Principles**: a `Zone` of 14 where you aim (28), from `PRINCIPLES_RISEN` for the rest of 12 s, twice a second. Enemies: no gravity (`status/Gravity`), lifted off the ground when they touch it, Slowness I; they fall without damage when they leave it or it ends. Allies and you: Slow Falling, Speed I. Fire and explosions from enemies or from nobody do not hurt your side inside (`IlluminatorCivilization.shelters`, a cancelled `LivingAttackEvent`).
- **Colossus**: 40 s. `GiantForm` 1.5 times your size with 2.5 more reach, Strength III, Resistance II and 40 plating (`ConjuredArmor`); it falls when the plating is gone or its time is up. Each second you moved a block or more it strides (`colossusStride`) and enemies within 6 get Slowness II. Picking it again while it stands fires the Fusion Beam (free, its own 8 s cooldown): at `FUSION_FIRES` a line from your eyes to what the crosshair is on (40), 4 wide, 120 to every enemy on it.
- **Scrolls of Civilization**: 10 s. Every tick, the projectiles of those not on your side within 5 of you stop (`scrollsStop`); hits on you from anyone more than 5 away do half. Then the scrolls fly out 12: every enemy they reach is held completely 3 s, immersed and hurt 40.
- **Mathematical Maze**: on the enemy under the crosshair (24), from `MAZE_CLOSED` for the rest of 8 s: `status/AbilityLock` (a player has no abilities, a mob fights only in melee), and it is put back in its 3 block cube whenever it leaves it, the lattice hurting 10 at most every half second. With no enemy but an ally under the crosshair: Enlightenment for 30 s: Haste II, cleansed, and every ability costs it 20% less (`enlightenedCost`, a `Spells.addCostScale`).
- **Masterwork**: `Zone.around` you, 20, for 10 s, twice a second. Allies and you: Strength I, Haste I, Regeneration I. Enemies: Slowness II, Weakness I. Anything else you cast ends it (`castSomething`, an `Illuminator.addAfterCast`).
- **Reality Knowledge**: on the enemy under the crosshair (32): `AbilityLock` for 10 s (5 for a boss) at `REALITY_LANDS`; a player is told "You cannot remember how." Every mark on you is wiped, and a player target forgets what it had studied of you. A Paragon cannot be locked: `realityGuard` shows and nothing is spent.
- **Illusory Book**: not on a boss. At `BOOK_OPEN` the target and up to two more enemies within 4 (not bosses) are written in: `status/Exile` for 8 s (5 for players), back on the ground under the book, then 20% of their health lost, nothing softening it.
- **Mechanical Heaven**: a `Zone` of 40 round where you stand, 60 s, once a second. Your constructs inside do not run out (`Construct.extend`) and hit 1.5 times as hard. Allies inside spend 30% less on every ability (`heavenCost`, a `Spells.addCostScale`). Enemies inside: Slowness I, each armor piece and what they hold lose 1 durability a second, every cooldown they start is half again as long (`heavenCooldown`, a `Cooldowns.addScale`: the wheels and single keys that use `Cooldowns`), and every 2 s one of their projectiles inside collapses into static. Picking it again ends it (free).

**Passives of Sequences 4 to 0** (`custom("Illuminator", N, ...)` lines in `SequencePassives`, and `IlluminatorEvents`):
- Constructor's Bond (4): once a second your constructs within 16 heal 1% and are traced for you (`constructorsBond`).
- Absorption (3): once a second every enemy within 6 takes 1, and you heal half of what was drawn, 2 at most.
- Analysis (2): once a second the concealed, unnoticed and invisible within 16 that are not on your side are `Exposed` for 1.5 s; they and those with a Substitute ready are marked for you alone.
- Enlightenment Aura (1): any player within 16 of an Illuminator who counts them as an ally (the Illuminator too) spends 10% less on every ability (`Illuminator.auraCost`), and nausea and blindness put on them last half as long; gold motes on allies now and then.
- Reality Knowledge (0): the Paragon's marks last twice as long; nobody can Appraise it, lock its abilities (`AbilityLock.addImmunity`) or build Study on it.

**Growth finished with them:** Survey at Arcane Scholar shows, for the viewer alone and every second of the Survey's 8 seconds, where anyone else spent spirituality on an ability within reach in the last minute, in their Pathway's color (`combat/Residue`, `DarkAngel.pathwayColor`). The Rite covers the Alchemy and Arcana wheel from Sequence 4.

## Looks, sounds and pictures

Everything the abilities need to look and sound right is in `IlluminatorFx.java` (`beyonder_characteristics/illuminator/`). It only shows things; the ability does the damage, holding, hiding, healing and moving, at the ticks its constants name. `/illuminatorfx all` plays every display in game, `/illuminatorfx <name>` one of them (TAB lists the names; there are also `construct_break`, `acid_puddle`, `survey_residue`, `steam_form`, `enlightenment`, `passives`, `combo` and `statuses`). Displays that need a target use the nearest mob within 12 blocks.

| Ability | Normal cast | Sneak cast |
| --- | --- | --- |
| The mark (shared) | `appraisedMark(sl, target, Weakness.X, ticks, tag)`: the blueprint lens over the head with the weakness in it (four pictures: starlight, corrosion, gravity, pierce); `appraisedEnd(sl, target, tag)` | |
| 1 Compound Flask / Recall | `flaskThrow(sl, player)`, `FLASK_FLYING` for the `Shot` (two in a row from Artisan), `flaskTrail(sl, at)` every tick, `flaskBurst(sl, at, radius)`, `corroded(sl, target, ticks, tag)` / `corrodedEnd`; Arcane Scholar: `acidPuddle(sl, ground, radius, ticks, tag)` / `acidPuddleEnd` | Recall: `recall(sl, who, from, to)` as the step back happens (`who` = the Savant, or from Illuminator the ally sent back) |
| 2 Survey / Ritual Circle | `survey(sl, player, reach)` (the pulse reaches its edge at `SURVEY_PULSE`), `surveyed(sl, target)` on each thing found (the outline is the ability's glowing), `surveyForcedOut(sl, hidden)`; Arcane Scholar: `surveyResidue(viewer, at, pathwayColor)`, seen by the Illuminator alone | Ritual Circle: `ritualCircle(sl, player, center, radius, ticks, tag)` (drawn at `RITUAL_DRAWN`), `ritualPulse(sl, center, radius)` every 3 seconds, `ritualCleansed(sl, ally)`, `ritualEnd(sl, center, radius, tag)` |
| 3 Appraise / Exploit Flaw | `appraise(sl, player, target, weakness, ticks, tag)` (the mark settles at `APPRAISE_LANDS`) | `exploitFlaw(sl, player, target)` (it strikes at `FLAW_LANDS`), `noFlaw(sl, player)` with no mark, `constructTakenApart(sl, construct, destroyed)` for constructs, dolls and golems; Illuminator: `exploitAll(sl, player, targets)` |
| 4 Workshop (wheel) | Clockwork Rifle: `rifle(sl, player, rounds, armCannon)` (round i leaves at `RIFLE_FIRES + i * RIFLE_STEP`), `rifleShot(sl, player, from, to, weakness)` per round, `ROUND` for a `Shot`, `rifleHit(sl, at, weakness)`. Spirit Cannon: `spiritCannon(sl, at, yaw, ticks, tag)` (it can fire from `CANNON_READY`), `cannonAim(sl, at, tag, target)`, `cannonFire(sl, at, tag, target)` (returns the muzzle, where the ball leaves), `CANNONBALL` for the `Shot` with `cannonballTrail`, `cannonBlast(sl, at, radius)`, `cannonEnd(sl, at, tag)`. Mystic Brooch: `mysticBrooch(sl, wearer, ticks, tag)`, `broochAbsorb(sl, wearer)` on each blow it soaks, `broochBurst(sl, wearer, radius, tag)`. Icons `WHEEL_CLOCKWORK_RIFLE`, `WHEEL_SPIRIT_CANNON`, `WHEEL_MYSTIC_BROOCH` | the last pick again |
| 5 Astronomy (wheel) | Star of Curses: `starOfCurses(sl, player, target, Curse.X or null)` (the ray lands at `CURSE_LANDS`), `cursed(sl, target, curse, ticks, tag)` (one step at a time under one tag), `curseEnd`. Blessing of Stars: `blessingOfStars(sl, center, radius, ticks, tag)` (risen at `BLESSING_RISEN`), `blessingTick` every second, `pressed(sl, enemy)`, `weightless(sl, ally)`, `blessingEnd`. False Constellations: `falseConstellations(sl, center, radius, ticks, tag)` (the sky hangs `FALSE_SKY_HEIGHT` up), `falseConstellationsEnd`. Meteor Shower: `meteorShower(sl, center, radius)` as it starts, then `meteor(sl, ground, strong)` per meteor (it lands at `METEOR_LANDS`, its warning ring shows until then; `strong` = cast inside a false sky). Icons `WHEEL_STAR_OF_CURSES` ... `WHEEL_METEOR_SHOWER` | the last pick again |
| 6 Alchemy and Arcana (wheel) | Living Doll: `dollRise(sl, at, yaw, Material.X)` (it stands at `DOLL_STANDS`), `dollBody(sl, doll, material, ticks, tag)` on its mob, `dollBolt(sl, doll, to)` for the Iron Doll, `dollFall(sl, doll, material, tag)`. Golem: `golemAssemble(sl, at, yaw)` (whole at `GOLEM_STANDS`), `golemBody(sl, golem, ticks, tag)`, `golemStrike(sl, golem, ticksLeft, tag)` (lands at `GOLEM_STRIKE_LANDS`), `golemTaunt(sl, golem, radius)`, `golemEnd`. Mechanical Body: `mechanicalBody(sl, player, ticks, tag)` (clamped on at `MECH_ON`), `armCannon(sl, player, ticks, tag)` while the rifle fires through it, `jet(sl, player)` every 2 or 3 ticks while rising, `mechanicalBodyEnd`; Magister's steam form: `steamForm`, `steamTrail` every tick or two, `steamReform`. Dissociation: `dissociation(sl, player, eye, end, width)`, `dissolved(sl, target)`, `blockFades(sl, blockCenter)` for the blocks it crosses. Knowledge Spirit: `knowledgeSpirit(sl, player, from, to, flight)`, `mindEntered(sl, target)`, `eroded(sl, target, ticks, tag)` / `erodedEnd`, `spiritStepOut(sl, player)`. Icons `WHEEL_LIVING_DOLL` ... `WHEEL_KNOWLEDGE_SPIRIT` | the last pick again |
| 7 Civilization (wheel) | Rewrite Principles: `rewritePrinciples(sl, center, radius, ticks, tag)` (risen at `PRINCIPLES_RISEN`), `principlesTick` every second, `lifted(sl, enemy, ticks, tag)`, `principlesEnd`. Colossus: `colossus(sl, player, ticks, tag)` (standing at `COLOSSUS_READY`, played at `COLOSSUS_SCALE` 2 = 6 blocks), `colossusStride(sl, player, ticksLeft, tag)` while walking (it shakes the ground with `colossusStep`), `fusionCharge(sl, player, ticksLeft, tag)` then `fusionBeam(sl, from, to, width)` at `FUSION_FIRES`, `colossusEnd`. Scrolls of Civilization: `scrollsOfCivilization(sl, player, radius, ticks, tag)`, `scrollsStop(sl, at)` for a projectile stopped, `scrollsRelease(sl, player, radius, tag)` (out at `SCROLLS_FLY`), `immersed(sl, target, ticks, tag)`. Mathematical Maze: `mathematicalMaze(sl, player, target, ticks, tag)` (closed at `MAZE_CLOSED`), `mazeTouch(sl, at)`, `mazeEnd(sl, at, tag)`; on an ally `enlightenment(sl, player, ally, ticks, tag)`. Masterwork: `masterwork(sl, player, radius, ticks, tag)`, `masterworkTick` every 10 ticks, `masterworkEnd`. Reality Knowledge: `realityKnowledge(sl, player, target, ticks, tag)` (lands at `REALITY_LANDS`), `forgottenEnd`. Illusory Book: `illusoryBook(sl, at, yaw, ticks, tag)` (open at `BOOK_OPEN`), `written(sl, victim, at)` for each one written in, `bookRelease(sl, at, tag)` (gone after `BOOK_RELEASES`). Mechanical Heaven: `mechanicalHeaven(sl, player, radius, ticks, tag)` (risen at `HEAVEN_RISEN`), `heavenTick` every second, `heavenStatic(sl, at)` for an enemy projectile collapsing, `heavenEnd`. Icons `WHEEL_REWRITE_PRINCIPLES` ... `WHEEL_MECHANICAL_HEAVEN` | the last pick again |
| Passives | `starlight(sl, player)` now and then at night, `constructorsBond(viewer, construct)` (only the Alchemist sees it), `absorption(sl, player, from)` per enemy drained, `analysis(viewer, hidden)` (only the Magister), `enlightenmentAura(sl, ally)`, `realityGuard(sl, player)` when a read or erase fails on the Paragon | |
| Melee | `combo(sl, player, target, step)` for hits 0, 1 and 2 | |

Every timed cast can show its picture and clock above the hotbar: `IlluminatorFx.status(player, Status.X, ticks)`, and `statusEnd` to take it away (24 statuses: 12 for what the Illuminator holds, 12 for what is done to others when they are players); `statusForever` for the Mechanical Body that has no timer from Illuminator on. The Pathway's icon beside the hotbar is registered in `PathwayHudIcon` under `"Illuminator"`.

Things to know when the abilities are written:
- The decoy doll of Arcane Scholar wears no doll model: dress its mob as the player instead (it is meant to look like them).
- The Spirit Cannon's carriage and barrel are two models at one spot. `cannonAim` turns only the barrel (`ParticleShapes.moveModel` on the tag `tag + "_barrel"`), so call it before each `cannonFire`, or every few ticks to track.
- The Golem and the Colossus are three-step models (`.frames(3).smooth()`). `golemStrike`, `colossusStride` and `fusionCharge` replace the worn body under the same tag with one that plays the movement, so pass how many ticks the body still has to last.
- Models worn with `modelOnTurning` take their height from the offset and anything sideways from their keys (the offset does not turn with the body): the rifle, the baton and the brooch are placed that way.
- Masterwork's baton beats in keys, and a model holds at most 64 keys: past 15 seconds (300 ticks) it holds still. Masterwork lasts 10.
- The Mathematical Maze's lattice and the Illusory Book are not stuck to anyone: end them with `mazeEnd` / `bookRelease` at the spot they were shown.

What is in the repository, all of it generated by the scripts in `tools/model_gen/illuminator/` (see "Illuminator: ready-made displays" in [MODEL_FX.md](MODEL_FX.md)):

| What | Where | Made by |
| --- | --- | --- |
| 28 box-model files (a three-step model is three files): the flask, the Clockwork Rifle and its round, the Spirit Cannon (carriage and barrel) and its cannonball, the Mystic Brooch, five Living Dolls (stone, straw, iron, dust, frost), the Golem (3 steps), the Mechanical Body with its goggles and arm cannon, the Colossus (3 steps), a gear, a rolled scroll, the conductor's baton, a meteor of starlight, the Illusory Book (3 steps) | `models/model_fx/illuminator/`, `textures/model_fx/illuminator/` | `models.py` (+ `textures.py`) |
| 28 picture models: the Appraised lens (4 weaknesses), the flaw mark, a blueprint circle, the chalk circle, a blueprint spiral, a star chart, the circle of Rewrite Principles, the symphony ring, the acid puddle (animated), the three curses, the Eroded page, the rejection seal, Recall's clock (animated), the Illuminator's emblem, the lattice of equations (animated, six faces), the mist beam and the Fusion Beam (animated), the false sky (animated), the sky of gears (animated), four murals of civilization | the same folders | `pictures.py` (run by `models.py`) |
| 20 wheel pictures (32 px), 24 status pictures (16 px), the Pathway icon | `textures/gui/ability_menus/illuminator/`, `textures/gui/illuminator/`, `textures/gui/pathway_icons/illuminator.png` | `icons.py` |
| 9 particle pictures: `fx/gear`, `fx/steam`, `fx/equation_0` to `fx/equation_2`, `fx/spring`, `fx/screw`, `fx/lens`, `fx/compass` (`fx/page` and `fx/cog` were already there) | `textures/particle/fx/` | `particles.py` |
| 56 sounds (59 files): the flask thrown and bursting, the acid's hiss, Recall, Survey, the chalk circle and its hum, Appraise, Exploit Flaw, a construct falling apart, the rifle wound and fired, the cannon unfolding, firing and its ball bursting, the brooch pinned and shattering, the Star of Curses and a curse deepening, Blessing of Stars, False Constellations, a meteor falling and striking, a doll rising and crumbling, the golem assembling, striking and calling out, the Mechanical Body on and off, the jetpack, Dissociation, the Knowledge Spirit, a mind entered, Absorption, Rewrite Principles, the Colossus assembling and stepping, the Fusion Beam charging and firing, the steam form and re-forming, Analysis, the scrolls unfolding and flying out, Immersion, the maze closing, Enlightenment, Masterwork's symphony (10 seconds), Reality Knowledge, the book writing someone in and letting them out, Mechanical Heaven, and the melee combo's gear swing, page swing and blueprint strike | `sounds/illuminator/`, `sounds.json`, subtitles in `lang/en_us.json`, registered in `sounds/ModSounds.java` (`ILLUMINATOR_...`) | `sounds.py` |
