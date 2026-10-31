# Omniscient Eye: ability design

Circle Of Inevitability · Pathway plan

The Omniscient Eye Pathway is the White Tower Pathway (also called the Reader Pathway) of Lord of the Mysteries, the Tower tarot card: Reader (9), Student of Ratiocination (8), Detective (7), Polymath (6), Mysticism Magister (5), Prophet (4), Cognizer (3), Wisdom Angel (2), Omniscient Eye (1), White Tower (0). Source: the White Tower Pathway and White Tower Pathway/Abilities pages of the Lord of the Mysteries wiki. (The wiki also calls Sequence 7 "Knowledge Keeper".)

Nothing here is written as code yet. This file is the plan: what every ability does, the numbers to start from, which parts of the [spell kit](SPELL_KIT.md) build it, which new reusable parts are worth writing first, and a checklist for wiring it in. When the code is written, replace the plan with a "What is built" section, as [KNOWLEDGE_EMPEROR.md](KNOWLEDGE_EMPEROR.md) does. The code's constants are the truth then.

Pathway name in code: `"Omniscient Eye"`.

## Theme

- A scholar's Pathway that fights by understanding. Its loop is **observe, analyze, then exploit or imitate**. The low Sequences read and reason (small, exact, cheap). From Polymath (6) it analyzes the abilities it sees and casts them back as imitations. Mysticism Magister (5) invents spells of its own. Prophet and Cognizer look ahead and lay enemies bare. The Angels and the Tower know everything and use it.
- The wiki's symbolism is "knowledge leads to calamity": the more you know, the closer you stand to madness and destruction. That is the Pathway's price (Madness) and, at Sequence 0, its weapon (Calamity of Knowledge).
- It is **not** the Knowledge Emperor. That Pathway builds a Study gauge on enemies and throws knowledge as spells. This one studies *abilities*, borrows them, and finds the flaw. The plan avoids `Study` on purpose and uses `Appraisal` plus a new `Codex` instead.
- Colors: brass for eyes and the Tower, ink-black for the pools between the books, aged cream for pages, deep violet for madness. Pathway color: `0xD1A94B`.
- Mythical Creature Form: illusory bookshelves with pools of black eyes (Prophet, incomplete), a phantom tower of books each bearing a brass eye (Wisdom Angel, complete), a spiral bookshelf with an Omniscient Eye on top that shines like a lighthouse (the pinnacle). It only shows up as Bookshelf Form and its growth.
- Ingredients on the wiki (lavender, pepper, mint) are flavor only.
- Nothing here breaks blocks or lights real fire. Ink, pages and light only show and hurt; the ground is never changed.

## How it is laid out

It follows the Attendant Of Mysteries, Hand Of God and Knowledge Emperor pattern: seven keys (G H J K L N M), every ability has a normal cast and a sneak cast. Keys 1 to 4 are single keys (normal and sneak are two different abilities). Keys 5, 6 and 7 are wheels where every entry has its own Sequence, cost and cooldown; on a wheel key, sneak casts the last pick again (`AbilityWheel.quickCast`). Keys unlock at Sequences 9, 8, 7, 6, 5 and 4, and key 7 at Wisdom Angel (2); Cognizer (3) adds entries to key 6 instead of getting a key.

The abilities do not each get a Sequence of their own. Sequences 9, 8, 7, 6 and 2 have two abilities, Sequences 4, 3, 1 and 0 have three, Mysticism Magister (5) has four. Sequences 2, 1 and 0 are not left without abilities, and they also strengthen what is already there (see "Grows with the Sequence").

| Sequence | Abilities |
| --- | --- |
| Reader (9) | Reading, Ritual Circle |
| Student of Ratiocination (8) | Deduction, Keen Eye |
| Detective (7) | Reconstruction, Deductive Strike |
| Polymath (6) | Analyze, Imitate |
| Mysticism Magister (5) | Mirror Curse, Vile Thoughts, Improvise, Living Tome |
| Prophet (4) | Golden Armor Art, Prophecy, Bookshelf Form |
| Cognizer (3) | Cognize, Leverage, Decipher |
| Wisdom Angel (2) | Evade Disaster, Wise Counsel |
| Omniscient Eye (1) | Omniscience, Exploit, Redefine |
| White Tower (0) | Calamity of Knowledge, Open Source, Tower of Omniscience |

## Shared mechanics

Four small systems tie the Pathway together. Each is worth writing once and reusing (see "New reusable parts").

**Analysis of enemies.** An Analysis is an `Appraisal` mark: one caster has read an entity for a while, knows what it is weak to, and every *ability* hit on it lands harder (the mark's bonus, and 50% more when the hit is of the kind it is weak to). Reading starts one, Deduction, Analyze, Cognize and Omniscience make stronger ones. Marks should not stack: the code keeps the strongest (check how `Appraisal.mark` treats a second mark).

| Source | Mark | Time |
| --- | --- | --- |
| Reading | +10% | 10 s |
| Deduction | +10% | 10 s |
| Analyze | +15% | 20 s |
| Cognize | +25% | 20 s |
| Omniscience | +35% | 20 s |

What an enemy is weak to comes from one `WeaknessTable` (the same rules as Hand Of God's Alchemy, so keep one table in one place): frost (fire creatures), fire (arthropods, ice mobs, plants), lightning (water creatures, anyone in metal armor), light (evil, undead), decay (living things with a lot of health), unmaking (spirits, summoned things, marionettes, constructs), otherwise pierce.

**The Codex (Analysis of abilities).** This is the heart of Imitation. Every ability of every Pathway you have seen cast gets an entry in your Codex with an Analysis from 0 to 100%. It is yours alone and is saved with you.

| What happens | Analysis gained |
| --- | --- |
| You see a cast within 32 blocks, in line of sight (Polymath passive) | +15% |
| The caster carries your Analyze mark | +40% instead |
| Reconstruction rebuilds the cast | +30% |
| Cognize lists an ability of a Beyonder | +50% |
| Inside the Tower of Omniscience | 100% at once |

The Codex holds 8 abilities at Polymath, 4 more with every Sequence after it (32 at White Tower); the least Analyzed is forgotten first. Casts come from `Casts.addAnyCasterListener`, so mobs and players both count. Anything marked `notCopyable()` or own-Pathway-only is listed but cannot be imitated.

**Imitation.** The wiki's Imitation is reverse-engineering, not copying: the effect always comes out weaker than the original, the better the Analysis the closer it gets, and the Sequence sets the ceiling. Potency is `cap x (0.4 + 0.6 x Analysis)`, which at Polymath gives exactly the wiki's 20% (worst) to 50% (best).

| Sequence | Cap |
| --- | --- |
| Polymath (6) | 50% |
| Mysticism Magister (5) | 60% |
| Prophet (4) | 80% |
| Cognizer (3) | 80% |
| Wisdom Angel (2) | 90% |
| Omniscient Eye (1) | 100% |
| White Tower (0) | 125% (optimized: it can surpass the original) |

It cannot imitate authorities, so Imitate, Open Source and Tower of Omniscience are `notCopyable()` (nothing may copy Imitate, or it would copy itself). An ability from Sequence 1 or 0 of another Pathway comes out at half potency.

**Ritual Circle (Ritualistic Magic).** The Pathway's platform. A circle of brass sigils on the ground, drawn by chanting, that other Sequences build on: from Mysticism Magister, spells cast from inside it are cheaper. It is a `Zone` with a chant in front (`Countdown`).

**Madness.** A level from 0 to 10 on an entity (a `Marks` counter, shown over its head with `SpellFx.overHead`), for what knowledge does to a mind. The per-caster form of `Marks` already wears one level off at a set interval, which is the "falls by 1" below. Enemies gain it from Calamity of Knowledge and the Tower; the caster gains it from Bookshelf Form and from imitating while in it.

| Level | What it does |
| --- | --- |
| 3 | nausea for 3 s |
| 6 | half its attacks go wide (`Falter`) |
| 9 | afraid (`Fear`); a mob attacks the nearest creature of its own side |
| 10 | breaks: stunned 2 s, takes 5% of its maximum health, resets to 3 |

Madness falls by 1 every 5 seconds. On the caster, level 10 ends the form and leaves them weakened.

## Keys

Starting costs (spirituality, both casts), set so each fits what its Sequence can hold ([SEQUENCES.md](SEQUENCES.md): a Reader holds 20, a Student of Ratiocination 400, a Detective 1980, a Polymath 6400): Reading 8 and Ritual Circle 15; Deduction 120 and Keen Eye 150; Reconstruction 700 and Deductive Strike 500; Analyze 1500 and Imitate 1.5 times what the copied ability costs. Keys 1 to 4 have no cooldown of their own; spirituality limits them. The wheel entries carry their own costs and cooldowns below.

| Key | Ability | Sequence | Normal cast | Sneak cast |
| --- | --- | --- | --- | --- |
| 1 | Reading | Reader (9) | Reading: look at the creature in front of you (24 blocks) and read it. Its name, health, mood (calm, afraid, furious), who it is hunting and every effect on it appear in chat and as a small floating card over it for 6 seconds. It carries your mark for 10 seconds, so your abilities hit it 10% harder. | Ritual Circle: three seconds of chanting (cancelled if you move or are hurt), then a 4 block circle of brass sigils lies on the ground for 30 seconds. Allies inside take 15% less damage and lose harmful effects every 5 seconds, enemy projectiles that enter it slow to a crawl, and spirits and summoned things cannot cross its edge. |
| 2 | Ratiocination | Student of Ratiocination (8) | Deduction: reason out the enemy you look at (24 blocks) for 10 seconds. Its attacks show as pale outlines where they will land, its blows are 25% weaker as you read them, its weakness is named in chat, and it carries your mark (+10%). | Keen Eye: for 12 seconds you notice everything. Enemy traps and hidden hazards within 24 blocks show, the paths of enemy projectiles within 24 show as thin lines, and an enemy that is hiding is pulled out the moment you look straight at it (Exposed for 3 seconds). |
| 3 | Detective's Work | Detective (7) | Reconstruction: rebuild what happened here. Within 24 blocks, for 6 seconds, pale ghosts replay the abilities cast in the last 60 seconds where they were cast, each in its Pathway's color. The three newest are written in chat (the ability, who cast it, how long ago), and the paths players walked in the last 10 seconds are drawn. Every ability rebuilt this way is Analyzed +30% in your Codex. | Deductive Strike: step forward up to 3 blocks and strike the enemy in front (6 blocks) three times in half a second, with fists or any weapon: 12, 12 and a finishing 24 that ignores armor. If it carries your mark, every blow lands 25% harder and the mark is spent for a fourth blow of 24 that staggers it. |
| 4 | Polymath | Polymath (6) | Analyze: take an enemy apart (24 blocks). Two seconds of looking (cancelled if it leaves your sight), then it carries your Analysis for 20 seconds: weakness known, your ability hits on it +15%, and every ability it casts meanwhile is Analyzed +40% in your Codex. Chat says what it is, and the hotbar shows its Analysis. | Imitate: cast an ability you have analyzed, weaker (see "Imitation"). It copies the last ability cast by the one you look at (within 20 seconds), else the latest copyable cast near you in sight (within 10 seconds), else your best Analyzed ability. It is cast in the form it was cast in (sneak or not). Nothing to copy: nothing happens and nothing is spent. |
| 5 | Mystic Arts | Mysticism Magister (5) | The Mystic Arts wheel: Mirror Curse, Vile Thoughts, Improvise, Living Tome, Golden Armor Art. Each has its own Sequence, cost and cooldown. | The last Mystic Art again, without the wheel. |
| 6 | Fate and Cognizing | Prophet (4) | The Fate and Cognizing wheel: Prophecy, Bookshelf Form, Cognize, Leverage, Decipher. | The last pick again, without the wheel. |
| 7 | Omniscience | Wisdom Angel (2) | The Omniscience wheel: Evade Disaster (2), Wise Counsel (2), Omniscience (1), Exploit (1), Redefine (1), Calamity of Knowledge (0), Open Source (0), Tower of Omniscience (0). | The last pick again, without the wheel. |

### Key 5 wheel: Mystic Arts

Spells of the Magister's own making, mostly from the wiki's named examples. They can be cast without chant or materials; the Ritual Circle makes them cheaper.

| Entry | Sequence | Cost / cooldown | What it does |
| --- | --- | --- | --- |
| Mirror Curse | Mysticism Magister (5) | 3500 / 25 s | A brass mirror shows the enemy you look at (24 blocks). Pinching its reflection makes its wounds worse: it takes 45% of the health it is missing at once (at most 120; a boss 20%, at most 60) and bleeds for 5 seconds. The more hurt it is, the harder it lands. |
| Vile Thoughts | Mysticism Magister (5) | 3000 / 20 s | Your eyes turn black and sticky thoughts flow. You clench your right hand: the enemy you look at (24 blocks) is flooded by its own worst thoughts for 6 seconds. Nausea, afraid of you, and half its attacks go wide. Spirits and summoned things also take 40 damage. |
| Improvise | Mysticism Magister (5) | 4500 / 30 s | Spell Creation: you invent a spell on the spot for the enemy you look at (28 blocks), from what it is weak to. Frost is a ray that slows, fire a bolt that burns it, lightning an arc to up to 3 enemies with a 0.5 second stun, light burns evil and undead hard, unmaking deletes summoned things, otherwise a spear of force. 40 damage, 60 on a match. Each cast looks a little different. No enemy in sight: nothing happens, nothing is spent. |
| Living Tome | Mysticism Magister (5) | 5000 / 45 s | A thick book falls open in your hands and a creature of pages and ink with one brass eye steps out and fights for you for 30 seconds (60 health, blows of 8). When it falls it bursts into pages that blind enemies within 4 blocks for 2 seconds. One at a time. |
| Golden Armor Art | Prophet (4) | 6000 / 40 s | Gold is shaped into a thin golden armor for 30 seconds: 40% less damage, six absorption hearts and no knockback, as good as a Guardian's. Aimed at an ally within 16 blocks, it armors them instead. |

### Key 6 wheel: Fate and Cognizing

| Entry | Sequence | Cost / cooldown | What it does |
| --- | --- | --- | --- |
| Prophecy | Prophet (4) | 8000 / 40 s | You see the most likely next moments for 12 seconds. Attacks that will land on you in the next second show as ghost outlines, and your flying abilities (bolts, shots, rays) lead their targets: they go where the enemy will be instead of where it is, on a wider path. A Beyonder mob in sight shows, vaguely, the kind of ability it is about to cast. |
| Bookshelf Form | Prophet (4) | 9000 / 60 s | Your Mythical Creature Form, incomplete. For 30 seconds you are a shape of illusory bookshelves with pools of black eyes opening between the books: 30% less damage, four extra hearts, Resistance I. Every second the eyes read each enemy within 12 blocks (a trickle of Analysis on it and on every ability it casts). The longer you stay, the more your madness grows, and each Imitate adds more. At full madness the form ends and you are left weakened. Cast again to end it early. |
| Cognize | Cognizer (3) | 10000 / 30 s | You grasp the essence of what stands before you. Enemies in a cone in front of you (32 blocks) are laid bare for 6 seconds: nothing can hide, every effect, ward and weakness shows, and each carries your mark (+25%, 20 seconds). For every Beyonder among them, every ability its Pathway gives at its Sequence is read into your Codex at 50%, and the list is written in chat. |
| Leverage | Cognizer (3) | 12000 / 35 s | Find the one rock that holds the mountain up. The enemy you look at (32 blocks) loses every ward at once (Substitute, death ward, link, concealment), takes 30% more damage for 8 seconds and is Heavy for 6 seconds: slowed, falling like a stone. A non-boss you carry a +25% mark on is also held for 2 seconds as its footing collapses. |
| Decipher | Cognizer (3) | 9000 / 25 s | Read the principle behind an enemy's work and turn it. Aimed at a dome or shell: it turns inside out against its owner. Else aimed at an enemy zone, Decree or trap within 24 blocks: it ends. Else aimed at a creature with sealed keys, no abilities or a hold on it: all of it springs open. One branch per cast, in that order. |

### Key 7 wheel: Omniscience

| Entry | Sequence | Cost / cooldown | What it does |
| --- | --- | --- | --- |
| Evade Disaster | Wisdom Angel (2) | 14000 / 45 s | You see the shifts of the situation before they happen. For 15 seconds damage with no attacker (explosions, lightning, magic, falls, area abilities) does nothing to you, every hold, root and harmful effect on you springs open at once and again each second, and the next blow that would take more than half your health is slipped aside to the safest spot within 6 blocks. |
| Wise Counsel | Wisdom Angel (2) | 16000 / 60 s | Wisdom gives guidance. For 20 seconds you and your allies within 24 blocks pay 30% less for abilities, take 20% less damage from enemies you have marked, and see enemy attacks as ghost outlines a second before they land. Every enemy in range carries your mark (+20%). |
| Omniscience | Omniscient Eye (1) | 20000 / 30 s | The Eye opens. On whoever you look at (up to 200 blocks, through walls) or, with nobody in sight, the newest one you have hit or cursed anywhere in the world, you read everything: where they are (a thread of light points the way for 20 seconds), health, effects, wards, what they hold and who they fight, their Pathway, Sequence and abilities, and what they cast in the last 30 seconds. Their abilities are read into your Codex at 50%, and they carry your strongest mark (+35%, 20 seconds) wherever they go. |
| Exploit | Omniscient Eye (1) | 22000 / 40 s | You see the exploitable flaw in the enemy you look at (32 blocks) and press on it. The first flaw that fits, from the top: summoned or made (deleted), warded (every ward torn and 30% more damage taken for 10 seconds), hiding (Exposed 10 seconds), a fire creature (frozen 3 seconds), evil or undead (burned for 60 and weakened), heavy armor (corroded), a healer (no healing for 10 seconds), a Beyonder with abilities (the key it used last sealed 10 seconds), otherwise an old wound opened (bleeding 8 seconds). Chat names the flaw and how it will play out. |
| Redefine | Omniscient Eye (1) | 18000 / 50 s | Rewrite one concept, for a short while. Aimed at an enemy, the kind of ability it used last stops working for 15 seconds (a player's key is sealed; a mob loses its abilities for 6 seconds). Aimed at an ally, their next ability costs nothing and lands 30% harder. One aspect only, and never the caster. |
| Calamity of Knowledge | White Tower (0) | 40000 / 90 s | Knowledge leads to calamity. Everything you have Analyzed within 24 blocks is struck by what you know. Madness (+5 levels, which wear off as usual) and destruction: 25% of its maximum health (a boss 8%), and its armor loses a third of its durability. What you have not Analyzed only gets the nausea. |
| Open Source | White Tower (0) | 25000 / 120 s | Your spell leaves the Tower. The last ability you cast or imitated is given to every ally within 24 blocks (3 at most) to cast once in the next 60 seconds at half strength, paying triple for it from their own spirituality. It need not be their Pathway or their Sequence. |
| Tower of Omniscience | White Tower (0) | 60000 / 180 s | Your Divine Kingdom: a haze of books with brass eyes covers 40 blocks round you for 60 seconds (cast again to end it early). Inside it nothing hides, every enemy carries your strongest mark, every ability cast inside is Analyzed to 100% at once, Imitate costs only the ability's own price and always reaches full potency, your allies pay 25% less for abilities, and enemy Madness rises 1 level every 2 seconds. |

## Grows with the Sequence

These do not get their own key or entry. The same ability is stronger from the Sequence on.

| Ability | From | What changes |
| --- | --- | --- |
| Reading | Student of Ratiocination (8) | Also names the enemy's weakness. |
| Ritual Circle | Student of Ratiocination (8) | 5 blocks wide, 40 seconds, two seconds of chanting. |
| Ritual Circle | Detective (7) | Master of rites: no chanting, 6 blocks wide. |
| Reading | Polymath (6) | Also shows how far you have Analyzed its abilities (Codex %). |
| Ritual Circle | Mysticism Magister (5) | Rite Focus: spells cast from inside it (key 5 wheel, Imitate) cost 25% less. |
| Analyze | Mysticism Magister (5) | One second of looking. |
| Reading, Analyze | Prophet (4) | Tell whether the creature is a Beyonder and about what Sequence. |
| Reconstruction | Prophet (4) | 48 blocks, 120 seconds back, and it replays concealed casts. |
| Bookshelf Form | Cognizer (3) | 40 seconds; madness grows half as fast. |
| Analyze | Cognizer (3) | Every enemy in a cone in front of you at once. |
| Reading | Cognizer (3) | Also names its Pathway and Sequence. |
| All Mystic Arts | Cognizer (3) | Cost 20% less. |
| Bookshelf Form | Wisdom Angel (2) | Complete: a phantom tower of books 6 blocks tall, 60 seconds, no madness. |
| Prophecy | Wisdom Angel (2) | 20 seconds, and every ability hit leads its target. |
| Bookshelf Form | Omniscient Eye (1) | The Eye on top shines a beam where you look: whatever it touches gains 10% Analysis a second. |
| Imitate | White Tower (0) | Optimized, up to 125% (see the cap table). |
| Living Tome | White Tower (0) | Creation: three at once, twice the size, 120 seconds, no book needed. |

## Passives

| Passive | From Sequence |
| --- | --- |
| Knowledge Addiction: the first Reading of each kind of creature (once per kind every 10 minutes) gives 10 spirituality and a little experience | Reader (9) |
| Sharp Mind: nausea, blindness and darkness end 25% sooner, and invisible things within 6 blocks show as faint outlines | Student of Ratiocination (8) |
| Weapon Proficiency: faster attacks and a little more reach with anything in your main hand, and Speed I | Detective (7) |
| Polymath's Eye: every ability cast within 32 blocks and in your sight is Analyzed a little in your Codex (+15%) | Polymath (6) |
| Mystic Mind: spirituality returns 25% faster while you stand in your own Ritual Circle | Mysticism Magister (5) |
| Demigod's Body: two extra hearts; Discernment: you can tell Beyonders from ordinary people | Prophet (4) |
| Cognizing Sight: hiding enemies within 8 blocks in your sight are pulled out (Exposed) every second | Cognizer (3) |
| Foreseen Shifts: every 30 seconds the first blow that would take more than half your health is slipped aside (a Substitute that re-arms itself) | Wisdom Angel (2) |
| Omniscient Gaze: every enemy you see within 16 blocks gains 5% Analysis a second | Omniscient Eye (1) |
| Open Book: within 24 blocks of you, enemies pay 25% more for every ability you have Analyzed to 100% | White Tower (0) |

## Melee combo (optional, later)

Hitting mobs as an Omniscient Eye (registered in `combat/MeleeCombos` like the other Pathways): three hits that repeat. A ring of turning pages around you, a tilted ring of brass eye-glyphs mirrored each time, then either a ground circle of ink lines winding outward or crossed page-edge slashes on the target with an eye opening above it. Colors: cream head (`0xF2F4EBD0`), brass middle (`0xEED1A94B`), ink tail (`0xA0221B14`).

## Built from the existing kit

| Ability | Built from |
| --- | --- |
| Reading, Analyze, Deduction | `Spells.aimEnemy`, `Appraisal.mark`, `WeaknessTable`, `Enfeebled`, `HudCounter`, `Countdown` (silent), new `Dossier` and `Codex` |
| Ritual Circle | `Countdown`, `Zone.at(...).affects(allies)`, `Spells.hostileShotsAround`, `Spells.addCostScale` (Rite Focus), `SpellFx.runeCircle` |
| Keen Eye | `Exposed`, `Trap.hiddenNear` (finds every trap near a spot), `Spells.hostileShotsAround`, `SpellFx.tether` |
| Reconstruction | `Residue.near`, `History`, `ParticleShapes.model`, `DarkAngel.pathwayColor`, `Codex` |
| Deductive Strike | `Spells.strike`, `Spells.freeSpotNear`, `Appraisal.strike` and `Appraisal.spend`, `MeleeCombo` |
| Imitate | `Casts.latestNear`, `Casts.last`, `Casts.recast`, `Echo.weaker`, `Beyonder.castUnbound` (paid form), `Casts.madeUpFor` for mobs, new `Codex` |
| Mirror Curse, Vile Thoughts | `Spells.strike`, `Bleeding`, `Fear`, `Falter`, potion effects, `ParticleShapes.model` |
| Improvise | `WeaknessTable`, `Shot`, `Ray`, `Spells.enemiesAround`, `Dispel.unmake` |
| Living Tome, Creation | `Construct.of(...).build(...)` |
| Golden Armor Art | `ConjuredArmor`, `Spells.aimAlly` |
| Prophecy | `Spells.hostileShotsAround`, ghost outlines with `ParticleShapes.model`, new `Spells.lead` |
| Bookshelf Form | `Form.of(...)` (`.cutsDamage`, `.attribute`, `.everySecond`, `.aftermath`), `Madness` |
| Cognize | `Spells.enemiesInCone`, `Exposed`, `Appraisal`, `Beyonders.takeableFrom`, `Codex` |
| Leverage | `Dispel.statuses`, `Vulnerable`, `Weight`, `Root.apply` |
| Decipher | `Dome.crossedBy` and `Dome.flip`, `Zone.closeNear`, `Unbound` |
| Evade Disaster | `Unbound`, `SupernaturalResistance`, `Substitute.arm`, `Spells.freeSpotNear` |
| Wise Counsel | `Spells.alliesAround`, `Spells.addCostScale`, `Appraisal`, ghost outlines |
| Omniscience | `Connection.latest` and `Connection.find`, `Casts.last`, `History`, `SpellFx.tether`, `PathwayAbilities`, `Codex` |
| Exploit | `WeaknessTable`, `Dispel`, `Exposed`, `Corrosion`, `NoHeal`, `KeySeal.lastUsed`, `Bleeding` |
| Redefine | `Casts.last`, `KeySeal.seal`, `AbilityLock`, a one-shot cost rule like Knowledge Emperor's Whisper (`KnowledgeEmperor.costFor` and `afterCast`) |
| Calamity of Knowledge | `Madness`, `Taunt`, `Decay`, `Spells.strike`, `Appraisal` |
| Open Source | `StolenAbilities.hold`, `Beyonder.castUnbound(player, 3f, ...)`, `Echo.weaker` |
| Tower of Omniscience | `Zone.at(...).height(24)`, `Exposed`, `Madness`, `Codex`, `Spells.addCostScale` |
| Passives | `SequencePassives`, `Physique`, `Substitute.arm`, `Exposed` |

## New reusable parts

Each gets a section in [SPELL_KIT.md](SPELL_KIT.md) when it is written. They are listed in the order they are worth writing, because the rest of the Pathway stands on the first two.

| Part | Where | What it does | Used by |
| --- | --- | --- | --- |
| `Codex` | `status/` | Each caster's Analysis of abilities, saved with the player. `see(player, cast, amount)`, `level(player, cast)`, `set`, `best(player)`, `known(player)`, `capacity(player)`, `forget`, rules `addCap(player -> ...)`, and `addListener` for the look. Also `potency(player, cast)` (the cap table and the 0.4 + 0.6 x Analysis formula) and `imitate(player, sl, cast)`, the wrapper that pays, runs `Echo.weaker` and refuses what is `notCopyable()`. Hears casts through `Casts.addAnyCasterListener`. | Polymath's Eye, Analyze, Reconstruction, Cognize, Omniscience, Imitate, Open Source, the Tower; any Pathway that learns powers (a Key Of Stars Quick Study could use it) |
| `Dossier` | `combat/` | What a Beyonder can learn about an entity, by depth: `lines(player, target, depth)` returns the chat lines (name, health, mood, hunted, effects, weakness, Beyonder or not, Pathway, Sequence, abilities, recent casts) and `card(sl, viewer, target, ticks)` the floating card only that viewer sees. | Reading, Analyze, Cognize, Omniscience; also useful for any Pathway that inspects |
| `Madness` | `status/` | A 0 to 10 level on an entity with the effect ladder above: `add`, `get`, `set`, `clear`, rules `addImmunity` and `addScale`, and `addListener` for the look. Built on `Marks`. | Bookshelf Form (on the caster), Calamity, the Tower, any madness-themed Pathway |
| `Spells.lead` | `combat/` | `lead(from, target, speed)` is the point to aim at so a shot of that speed meets an entity that keeps moving, `leadAim(player, sl, from, target, speed)` the direction. A few lines on top of the entity's movement. | Prophecy, and any shot that should not miss a runner |

`Appraisal`, `WeaknessTable`, `Casts`, `Echo`, `Zone`, `Dome`, `Form`, `Construct`, `Residue`, `Connection`, `StolenAbilities`, `Marks`, `Exposed` and `KeySeal` already exist and are used as they are.

## Wiring it in (checklist for when the code is written)

1. Classes in `blasphemy_slate/beyonder_characteristics/omniscient_eye/`, laid out like Knowledge Emperor: `OmniscientEye` (keys 1 to 4, passives, the Codex hooks and Imitate), `OmniscientEyeMysticArts` (key 5), `OmniscientEyeFate` (key 6), `OmniscientEyeOmniscience` (key 7), `OmniscientEyeFx` (looks only). Every number from this file is a constant at the top of its class.
2. Every ability method starts as the kit says: the Pathway check (`bypassClassCheck`), `Beyonder.hasSequence`, "does this make sense" checks, then `Spells.paySpirituality`. Check first, pay after, so a refused cast costs nothing and `Casts` and `castUnbound` see it right.
3. One `import static` and one line each in the packets `network/SKeyPressAbilityOneUsed` to `SevenUsed`.
4. One line each in `PathwayAbilities.ALL`. Mark Imitate, Open Source and the Tower own-Pathway-only (they use the Codex), and give those wheel entries `.notCopyable()`.
5. The name in `SequenceCommands.PATHWAYS`; the color in `DarkAngel.pathwayColor`; the icon in `PathwayHudIcon`; a row for every ability in [SEQUENCES.md](SEQUENCES.md); the passives in `SequencePassives`; the combo in `MeleeCombos` if wanted. Set it with `/sequence pathway <player> "Omniscient Eye"`.
6. Wheels: `AbilityWheel.create("omniscient_eye_mystic_arts", ...)`, `"omniscient_eye_fate"`, `"omniscient_eye_omniscience"`. Give the key 7 wheel `.free(...)` on the Tower entry so casting it again to end it costs nothing.
7. Suggested order: the four new parts, then keys 1 to 3, then key 4 (Analyze, then Imitate), then wheel 5, wheel 6, wheel 7, then looks and pictures. Imitate is the one to test hardest.
8. Beyonder mob versions (`beyonder_mobs/MobSpells`) are not planned here; add them after the player abilities work ([BEYONDER_MOBS.md](BEYONDER_MOBS.md)).

## Looks and pictures (to make when the code is)

Reuse `SpellFx.runeCircle`, `closingRing`, `tether`, `overHead`, `ParticleShapes.model`, `ModelFx`. New things to make: a brass eye (three sizes: Reading's card, Cognize's, the Tower's), a floating info card, a page and an ink-drop particle, a brass mirror, a Living Tome and its creature, a bookshelf shape with black eye-pools, the phantom tower of books, the Ritual Circle sigil, the Madness pips, a ghost outline for replayed and foreseen casts, and one wheel icon for each of the 18 wheel entries (5 on key 5, 5 on key 6, 8 on key 7). Models and pictures are generated by scripts under `tools/model_gen/`, as the other Pathways' are.

## Spare ideas (not on a key)

From the wiki, kept for later: Extermination of Worms Art (kills silverfish, endermites and other "worms", and drives out a `Parasite`), Evil Spell (rips a hold off an ally and sends it berserk), Self-Seal (stops Bookshelf Form's madness for a while), Respond to Prayers (Cognizer: an ally far away can call you), Astral World Ladder (cut an enemy's links: marionettes, summons, damage links), Evans Freezing Ray and Elemental Order (two spells from the author's other novel that a Magister can reproduce).

## To confirm

- **Imitate on one sneak press.** It picks what to copy by a fixed rule (the one you look at, then the latest cast near you, then your best). A real choice from the Codex would need a menu built per player, and `AbilityWheel` entries are fixed at load. Do you want a Codex menu later (a new part), or is the rule enough?
- **What Imitate costs.** I planned 1.5 times the copied ability's own cost, through `Beyonder.castUnbound(player, 1.5f, ...)`. `Casts.recast` is free, so the code has to decide whether to wrap the paid form or charge a flat price. Also, `Echo.weaker` only weakens hits: heals, holds and durations come out whole. Is that fine, or should Imitate refuse pure support abilities?
- **Spirituality at Sequence 9 is 20** (SEQUENCES.md), so Reading and Ritual Circle cost 8 and 15. Knowledge Emperor's first key costs 250, which would not fit there; I followed the table.
- **Madness on yourself.** `GiantForm` already has a madness that breaks the form, but it grows the body too. Can `GiantForm.of(1.0)` be used for a same-size form, or should Bookshelf Form keep its own counter?
- **Open Source for other Pathways.** Allies release a held ability through the path Worm Of Time's Theft uses. Check that a player of another Pathway can release one, or Open Source needs a key of its own.
- **Codex through death.** I planned it saved and kept through death, like the Pathway itself. Say if it should be lost on death, as Knowledge Emperor's pouch is.
- **Color and names.** Pathway color `0xD1A94B`; ability names are mine where the wiki gives none (Reading, Deduction, Keen Eye, Deductive Strike, Exploit, Redefine, Calamity of Knowledge, Open Source). Change any.
- **Overlap with Knowledge Emperor** (Prying, Foresight, Study): I kept the ideas apart (Keen Eye finds traps and shots rather than hidden creatures, Evade Disaster is about no-attacker damage, no Study). Say if you want them closer.
