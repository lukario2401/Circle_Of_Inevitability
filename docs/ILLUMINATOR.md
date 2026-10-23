# Illuminator: ability design

The Illuminator Pathway is the Paragon Pathway of Lord of the Mysteries, the High Priestess tarot card: Savant (9), Archaeologist (8), Appraiser (7), Artisan (6), Astronomer (5), Alchemist (4), Arcane Scholar (3), Knowledge Magister (2), Illuminator (1), Paragon (0). Source: the Paragon Pathway and Paragon Pathway/Abilities pages of the Lord of the Mysteries wiki.

The abilities are not written as code yet; their looks and sounds are (see "Looks, sounds and pictures" at the end: `IlluminatorFx` and everything it plays). The rest of this file is the plan: what every ability does, the numbers to start from, which existing parts of the spell kit build it, and which new reusable parts are worth writing first. When the code is written, move "Planned" to "What is built" (see BEAUTY_GODDESS.md).

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

Costs follow the Attendant Of Mysteries scale (250 at Sequence 9, 12000 at Sequence 3) and keep climbing; check them against the spirituality pool each Sequence gives. A cooldown of 0 means "cost only", like Attendant. Damage numbers are in the tables above. A boss is anything with more than 150 maximum health (the same line Marionette uses).

| Ability | Sequence | Cost | Cooldown (s) |
|---|---|---|---|
| Compound Flask | 9 | 250 | 2 |
| Recall | 9 | 400 | 25 |
| Survey | 8 | 600 | 15 |
| Ritual Circle | 8 | 900 | 25 |
| Appraise | 7 | 1250 | 3 |
| Exploit Flaw | 7 | 1500 | 10 |
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

## Built from what already exists

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

## New reusable parts worth writing first

Written once, used by many abilities, and by other pathways too (Hand Of God already needs several of these; Knowledge Emperor is the neighboring pathway, so any mark or information status built for it should be shared rather than duplicated).

| Part | What it is | Used by |
|---|---|---|
| Appraisal (status/) | The mark: who marked it, until when, its weakness. Questions: is it marked by me, what is its weakness. Adds the 15% damage. | Appraise, Exploit Flaw, Rifle, Cannon, Flask, Dissociation, Hand Of God's Eye of Demon Hunting |
| Weakness table | Data-driven: what a target is (undead, armored, flying, mechanical, player) maps to a weakness kind and the matching bonus. | Appraisal, Hand Of God's Alchemy coating |
| Vulnerable (status/) | Takes X% more damage for N ticks, stackable, with a source. | Flask (Corroded), Star of Curses (Mutation), Appraisal, Hand Of God's Sword of Dawn |
| Deployable (generalized SpiritFlame) | Owner, level, spot, expiry, an optional pulse every N ticks, an optional ModelFx. Cleared on level unload and when the owner is gone. Attendant's flames could move onto it. | Spirit Cannon, Ritual Circle, Blessing of Stars, False Constellations, Principles, Scrolls, Mechanical Heaven |
| Zone | A Deployable with an area: who is inside on each pulse, and "is this player inside a zone of type T" (for the Rite bonus). | Ritual Circle, Blessing of Stars, False Constellations, Principles, Masterwork, Mechanical Heaven |
| Construct (status/) | Spawns a vanilla mob, hides it and dresses it with a ModelFx, hands it to Marionette.control, tracks per-owner limits (one Golem, up to three dolls), removes it on death, timer or owner far away. Flags it "mechanical" for Exploit Flaw and Dissociation. Holds the material table for dolls. | Living Doll, Golem, Mechanical Heaven bonus, Exploit Flaw, Dissociation |
| Recall buffer | A small ring buffer per player: position, facing and health for the last 8 seconds. Restoring takes a number of seconds. | Recall (and Recall on an ally); reusable for any history or time effect |
| Ability lock | Blocks a target's Beyonder abilities (players) and ranged and special attacks (mobs) for N ticks, with a scope (spells, special, all). Checked at the start of every ability. | Mathematical Maze, Reality Knowledge, Knowledge Spirit, Hand Of God's Decay |
| Unreadable (status) | Cannot be picked out from further than N blocks: mobs forget, abilities that need to see or read cannot choose. | False Constellations, Hand Of God's Mind Concealment |
| Gravity (status) | Per-entity gravity factor for N ticks (lift, crush, normal) applied by a tick event, with no potion swirls. | Blessing of Stars, Principles, Mechanical Heaven |
| Held model | A ModelFx held in your hand for N seconds. | Clockwork Rifle, Mechanical Body arm cannon, Hand Of God's Sword of Dawn |
| Beam helper | Spells.enemiesAlong(from, to, width) plus a SpellFx.beam that draws a SlashFx line with a flash. | Star of Curses, Dissociation, Fusion Beam, Exploit Flaw |
| Shot options | Arc (gravity) and pierce (does not stop at the first enemy). | Flask, cannonballs, Fusion rounds |
| Residue (later) | Remembers where Beyonder abilities were cast (spot, pathway, Sequence, tick) for 60 seconds. | Survey at Arcane Scholar (3) |
| Assets | Done: see "Looks, sounds and pictures" below. | all of the above |

## Build order when the code is written

1. Appraisal, Weakness table and Vulnerable (keys 1 to 3 need them).
2. Recall buffer (key 1 sneak).
3. Deployable and Zone, then the Beam helper and Shot options (keys 2, 4, 5).
4. Construct (keys 4 and 6).
5. Gravity, Unreadable and Ability lock (keys 5, 6, 7).
6. Held model and the jetpack (the assets are done).
7. Then the abilities themselves, key by key: constants grouped per ability at the top of the class like Attendant, methods named illuminator plus the ability name, the same (Player, Level, ServerLevel, bypassClassCheck) arguments, and the melee combo last.

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

## Assumptions to check

- Constructs are vanilla mobs run by Marionette and dressed with a ModelFx. If custom entities come later, only Construct changes.
- Concealment is used on enemy mobs for Illusory Book and Knowledge Spirit; check that it accepts any living entity, not only players.
- Outlines (Survey) use the Glowing effect, so everyone sees them; a private outline would be a client-side part.
- The Flask flies flat until Shot has an arc option.
- Costs and cooldowns are first guesses, to be balanced against the spirituality pool.

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
