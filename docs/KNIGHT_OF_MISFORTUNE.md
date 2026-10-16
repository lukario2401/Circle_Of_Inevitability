# Knight Of Misfortune: ability design

The Knight Of Misfortune Pathway is the Darkness Pathway of Lord of the Mysteries: Sleepless (9), Midnight Poet (8), Nightmare (7), Soul Assurer (6), Spirit Warlock (5), Nightwatcher (4), Horror Bishop (3), Servant of Concealment (2), Knight of Misfortune (1), Darkness (0). Source: the Darkness Pathway and Darkness Pathway/Abilities pages of the Lord of the Mysteries wiki.

**Status: the abilities are planned, not written; their looks, sounds and pictures are ready** (see "Looks, sounds and pictures (ready)"). This file is the plan: what every ability does, the numbers to start from, which existing parts of the [spell kit](SPELL_KIT.md) build it, and which new reusable parts are worth writing first. When the code is written, move "Planned" to "What is built" (see [BEAUTY_GODDESS.md](BEAUTY_GODDESS.md)).

Pathway name in code: `"Knight Of Misfortune"` (already in `KnightOfMisfortune.PATHWAY` and `SequenceCommands.PATHWAYS`).

## Theme

- A night Pathway: sleep, dreams, silence, spirits, fear, concealment and, at the top, Misfortune. Its authorities are Silence and Darkness; at Sequence 0 it also holds Concealment, Misfortune, Horror and mastery of spirits.
- It does not win by raw damage. It puts things to sleep, takes their luck, hides what it likes and removes what it likes from the world. Its strength is **Nocturnality**: the darker it is, the stronger the Beyonder (see "Passives").
- Colors: purpureus violet-black for Darkness (pathway color `0x5A2E8C`), pale moon-blue for dreams and Midnight Poems, bone white for the Sword of Darkness and the Demonic Wolf, ash grey with tarnished gold (`0xB8A25A`) for Misfortune.
- Nothing here changes blocks. Darkness, dreams and concealment only show, hold and hurt; the ground is never touched (same rule as Hand Of God).
- Midnight Poems are spoken. Show a faint ring of pale text-motes round the head instead of a projectile; at Sequence 7 and up the poem is "resonated" and needs no sound.

## How it is laid out

It follows the Attendant Of Mysteries pattern: seven keys (G H J K L N M), every key has a normal cast and a sneak cast. Four keys (2, 5, 6, 7) are wheels like Apocalypse's, where every entry has its own Sequence, cost and cooldown; on a wheel key, sneak casts the last pick again (`AbilityWheel.quickCast`). Keys 1, 3 and 4 are plain normal / sneak pairs.

The abilities do not each get a Sequence of their own. Sequences 9 and 3 have two, Sequences 6, 5 and 2 have three, Sequences 8, 7 and 4 have four, and Sequences 1 and 0 have two each but also **strengthen what is already there** (see "Grows with the Sequence"). Sequences 2, 1 and 0 are not left without abilities. 29 abilities in all.

| Sequence | Abilities |
| --- | --- |
| Sleepless (9) | Spirit Vision, Dowsing |
| Midnight Poet (8) | Tranquilize, Lullaby, Pacify, Fear |
| Nightmare (7) | Nightmare Limbs, Nightmare State, Dream Invasion, Dream Shaping |
| Soul Assurer (6) | Requiem, Soothing Requiem, Agitating (poem) |
| Spirit Warlock (5) | Terror Banshee, Earthen Spirit, Gale Spirit |
| Nightwatcher (4) | Night Domain, Hair Entanglement, Curse of Misfortune, Demonic Wolf Form |
| Horror Bishop (3) | Sword of Darkness, Nightmare World |
| Servant of Concealment (2) | Concealed State, Secret Concealment, Concealed World |
| Knight of Misfortune (1) | Mantle of Darkness, Command Spirits; Curse of Misfortune becomes **Misfortune**; the Sword gets its three edicts |
| Darkness (0) | Eternal Rest, Destination of All Things; Misfortune becomes **Catastrophe** |

## Keys

| Key | Ability | Sequence | Normal cast | Sneak cast |
| --- | --- | --- | --- | --- |
| 1 | Night Sight | Sleepless (9) | **Spirit Vision**: for 12 seconds every living thing within 32 blocks (48 in the dark) glows through walls, only for you. Spirits, summons and marionettes glow in a second color, and hidden traps show as faint runes. You see that something is hurt, not by how much (no health numbers until Sequence 6). | **Dowsing**: a limited divination. A pendulum of pale light swings toward whatever hunts you (a mob that has you as its target, or the last player who hit you), else toward the nearest Beyonder player. A line of light points the way for 5 seconds and chat tells you the distance. |
| 2 | Midnight Poem | Midnight Poet (8) | The Midnight Poem wheel: Tranquilize, Lullaby, Pacify, Fear, Agitating. Each has its own Sequence, cost and cooldown. | The last poem again, without the wheel. |
| 3 | Nightmare | Nightmare (7) | **Nightmare Limbs**: tentacles of dark flesh grow from your back for 12 seconds, four of them. Each lashes the nearest enemy within 6 blocks once a second. They are part of you: the damage they take is taken from a layer on you, and when it is gone they tear off and you take a backlash. | **Nightmare State**: your soul steps out of your body and drifts, hidden and untouchable, up to 30 seconds. **While in it**, the normal cast is Dream Invasion (and Dream Shaping after it); sneak returns you to your body. See "Key 3 inside the Nightmare State". |
| 4 | Requiem | Soul Assurer (6) | **Requiem**: a field of lullaby round you, 10 blocks, 10 seconds. Enemies in it slow more the longer they stay and at last freeze; their abilities are silenced; ghosts, spirits and undead are held. | **Soothing Requiem**: your allies within 12 blocks are cleansed, calmed (taunts, fear and sleep end and cannot be put on them for 10 seconds), regenerate, and your own spirits and marionettes are healed to full. |
| 5 | Spirit Commanding | Spirit Warlock (5) | The Spirit Commanding wheel: Terror Banshee, Earthen Spirit, Gale Spirit (and Command Spirits at Sequence 1). | The last pick again, without the wheel. |
| 6 | Nightwatcher | Nightwatcher (4) | The Nightwatcher wheel: Night Domain, Hair Entanglement, Curse of Misfortune, Demonic Wolf Form. | The last pick again, without the wheel. |
| 7 | Darkness and Concealment | Horror Bishop (3) | The Darkness wheel, cast where you aim: Sword of Darkness (3), Nightmare World (3), Concealed State (2), Secret Concealment (2), Concealed World (2), Mantle of Darkness (1), Eternal Rest (0), Destination of All Things (0). | The last pick again, without the wheel. |

## Key 2 wheel: Midnight Poem

A poem is a short word of night. A target hears it if it is in front of you and in earshot; a player can **shake a poem off** by sneaking while it is spoken (the "cogitating" of the books) and ends it in half the time. Hearing poems again soon after makes them bite deeper: each poem the same target heard in the last 30 seconds adds 25% to the next one's time (up to four times). Bosses only hear Fear and Tranquilize.

| Entry | Sequence | What it does |
| --- | --- | --- |
| Tranquilize | Midnight Poet (8) | Body, heart and soul go quiet. You and your allies within 10 blocks are cleansed of fear, nausea, blindness and taunts. Aimed at one target, it also **shows whether a Beyonder is close to losing control** (the madness bar of a giant or wolf form, and every effect on it). |
| Lullaby | Midnight Poet (8) | A cone of sleep, 8 blocks long. Enemies in it fall asleep for 8 seconds and wake the moment they are hurt. Those that resist stay on their feet but are calm to the point of blankness: no sprint, no abilities for 3 seconds. |
| Pacify | Midnight Poet (8) | One target, 16 blocks. Its will to live is sapped for 8 seconds: it cannot attack or use abilities and crawls. Hurting it does not break the poem. |
| Fear | Midnight Poet (8) | A cone, 12 blocks, 6 seconds. Mobs run from you, players are slowed and sick with fear and cannot sprint toward you. |
| Agitating | Soul Assurer (6) | One target, 16 blocks: its frustration and destructive urges are stirred. A mob goes after the nearest thing, friend or foe, and everyone it meets takes more from it. Anything wrong with it (every effect, its health) is shown to you, as the books say the poem reveals hidden problems. It takes 15% more damage from everything meanwhile. |

## Key 3 inside the Nightmare State

The state gives the Nightmare its two other abilities, so they have no key of their own.

| Part | What it does |
| --- | --- |
| Nightmare State | You step out as a pale spirit: hidden, untouchable, floating at walking pace (you cannot pass through blocks) within 48 blocks of your body (72 at Sequence 6, 96 at 5, 250 at 4). Your body stays behind as a still sleeping figure. You cannot attack or take anything. If enemies stand next to the body for 3 seconds it is wrecked: the state ends and you take 30% of your health. Sneak to return, or it ends by itself after 30 seconds. |
| Dream Invasion | Normal cast while in the state, aimed at a **sleeper**: a player asleep in a bed, anything in Slumber (Lullaby, Hair Entanglement, Pacify), or a mob that has been looked at, unseen, for 5 seconds (the slow lulling). Its dream is entered without a sound: it is held for 6 seconds and you see through its eyes. Reach: 24 blocks (36 at Sequence 6, 48 at 5, 96 at 4; at 4 several sleepers can be pulled into the same dream). Costs less than a normal ability, and you stay a spirit. |
| Dream Shaping | The moment you are in a dream, a three-way ring opens (an `AbilityMenu`): **Secrets** (interrogation: for a player its Pathway and Sequence and what it last cast show in chat; for a mob what it carries), **Terror** (the dream breaks its spirit: it takes 25% of its current health and wakes in Fear), **Obedience** (a mob wakes as your marionette for 15 seconds, if its health allows; a player wakes pacified, unable to hurt you, for 8 seconds). Only one is picked; the ring closes after 5 seconds with Secrets by default. |

## Key 5 wheel: Spirit Commanding

You seal spirits in your teeth and call them out. Four spirits at once at Sequence 5; at Sequence 4 **each tooth holds one more**, so eight. A spirit that fades (its time, or it is killed) frees its tooth. Spirits are `Marionette.summon`s: they use none of the slots of other Pathways' marionettes and vanish without drops.

| Entry | Sequence | What it does |
| --- | --- | --- |
| Terror Banshee | Spirit Warlock (5) | A wailing spirit follows you for 30 seconds. Every 3 seconds it shrieks: enemies within 10 blocks fall into Fear and have their consciousness drained (blind and slowed for 2 seconds). Ordinary weapons do not hurt it, only magic damage does. |
| Earthen Spirit | Spirit Warlock (5) | You fuse with the earth for 8 seconds: hidden, sliding along the ground at about 18 blocks a second, untouchable. You come out where you aim, up to 24 blocks away. It only works on earth (grass, dirt, stone and the like): not on wood floors, water or in the air. |
| Gale Spirit | Spirit Warlock (5) | A wind spirit lifts you: you fly for 20 seconds, fast, with wings of dark air. You cannot attack with weapons while flying, only cast. |
| Command Spirits | Knight of Misfortune (1) | Every animal and undead creature within 40 blocks falls asleep, then the nearest eight (health up to 80 and bosses excepted) wake as your marionettes for 40 seconds. The "natural and undead spirits" of the books. |

## Key 6 wheel: Nightwatcher

| Entry | Sequence | What it does |
| --- | --- | --- |
| Night Domain | Nightwatcher (4) | A tide of pure Darkness spreads 20 blocks round you for 40 seconds. Inside it it is night for you: Nocturnality is at its fullest, you and your allies get Strength, Resistance and Regeneration, and your spirits fight harder. Enemies are **corroded** (armor and weapons lose durability each second), lose vitality (less maximum health the longer they stay) and gain a level of **Misfortune** every 3 seconds inside (up to 3). The sky inside looks like deep night. |
| Hair Entanglement | Nightwatcher (4) | Countless hairs lash out in a cone, 8 blocks. Enemies caught are bound for 4 seconds (Root). The touch is cold and quiet: anyone not hurt while held falls into Slumber for 4 seconds when the hairs let go. |
| Curse of Misfortune | Nightwatcher (4) | You inflict Misfortune on what you look at: a set amount, 3 levels for 20 seconds (see "Misfortune"). From Sequence 1 it is **Misfortune**, from Sequence 0 **Catastrophe**. |
| Demonic Wolf Form | Nightwatcher (4) | Your Mythical Creature Form, at great cost: for 30 seconds you become an incomplete eight-legged demonic wolf, 3 blocks high, ash-white ribs under a dark coat. Strength II, Speed II, Resistance I, longer reach, a bite that tears. It costs 400 spirituality a second more, and the longer it lasts the madder you get; at full madness the form ends and you are left weakened. Grows with every Sequence (see below). |

## Key 7 wheel: Darkness and Concealment

| Entry | Sequence | What it does |
| --- | --- | --- |
| Sword of Darkness | Horror Bishop (3) | A bone sword of Repose and Horror forms in your hand for 40 seconds. Every full swing sends a **tide of Darkness** forward, 10 blocks in a wide cone: everything it reaches is submerged and melts for 4 seconds. It goes through Substitutes and death wards and cannot be sidestepped once it is spread. Short swings do the sword's own damage only. |
| Nightmare World | Horror Bishop (3) | A dream world about the size of a small city opens round you, 24 blocks, for 30 seconds: everyone in range is pulled in and the shell holds. Your allies are protected (less damage, no mental or evil effects put on them) and you move freely. Enemies inside are slow, dull, and slide into Slumber after 8 seconds in it. |
| Concealed State | Servant of Concealment (2) | Aimed at someone, **it is wrapped in dark fog and taken out of the world for 8 seconds**: unseen, untouchable, and unable to act or be acted on. Aimed at an ally it hides them safe; aimed at nothing, it hides you. Aimed at someone **already hidden** (Concealment, Unnoticed, Moonlight, Mercury) it is **Reverse Concealment**: it ends their hiding and lights them up for 10 seconds. |
| Secret Concealment | Servant of Concealment (2) | A 16 block area is veiled for 45 seconds. Allies inside are Unnoticed, show no names, and no ability that needs to see or read someone (Gaze, Divination, Eye of Demon Hunting) can pick anyone inside. No walls: people may walk in and out. |
| Concealed World | Servant of Concealment (2) | A sealed sphere, 16 blocks, 20 seconds. Nothing crosses it (not even projectiles), it is opaque from outside, and nothing outside can pick anyone inside. Enemies caught in it are inside **with** you, cut from the world; your allies are untouched. Battles inside never change the real world's blocks. |
| Mantle of Darkness | Knight of Misfortune (1) | The Servant of Darkness: for 25 seconds your body half-dissolves into living Darkness. Archangel-level Strength III, Speed II, Resistance II, a quarter of all damage you take simply dissolves, and two arms of shadow follow your swings: longer reach, and every hit also hits the two nearest enemies for half and slows them. |
| Eternal Rest | Darkness (0) | The word of Darkness: aimed at an enemy (not a boss), it is wrapped in dark. If it is asleep, pacified or in Slumber it **simply stops** (killed, past any ward). Otherwise it takes 40% of its health and falls into an Eternal Slumber for 8 seconds that nothing can wake. Aimed at nothing, it is **your** rest: your next death within 60 seconds is cancelled and you are healed to 60%. |
| Destination of All Things | Darkness (0) | A black sphere, 24 blocks, opens where you aim for 12 seconds. Everything is drawn to its middle and dissolves: heavy, growing damage that nothing stops. It is not safe for allies (a quarter of the damage). You cannot die in it: the Darkness will not take you. |

## Grows with the Sequence

These do not get their own key or entry. The same ability is stronger from the Sequence on.

| Ability | From | What changes |
| --- | --- | --- |
| Spirit Vision | Soul Assurer (6) | Also shows health and spirit state of what it shows; no cooldown when you are standing in the dark. |
| Nightmare State | Soul Assurer (6) / Spirit Warlock (5) / Nightwatcher (4) | Reach 72 / 96 / 250 blocks. |
| Dream Invasion | Soul Assurer (6) / Spirit Warlock (5) / Nightwatcher (4) | Reach 36 / 48 / 96 blocks; at 4 you pull several sleepers into one dream. |
| Requiem | Nightwatcher (4) | Darkness-stained and full of chanting: the field also puts every enemy that stays 5 seconds into Slumber and pacifies undead and wraiths outright. |
| Spirit Commanding | Nightwatcher (4) | Each tooth holds one more spirit: eight at once. |
| Lullaby and Pacify | Horror Bishop (3) | Reach a whole city: 40 blocks, all round you. |
| Demonic Wolf Form | Horror Bishop (3) / Servant of Concealment (2) | 4.5 blocks high at 3, and allies who sneak near you are healed (prayers are answered). At 2 the **complete** giant eight-legged wolf with six arms (four from the ribs): a huge, mostly visual form, reach and Strength grow with it, and no madness. |
| Nightmare World | Servant of Concealment (2) | 48 blocks: a large city. |
| Requiem (presence) | Servant of Concealment (2) | You no longer need to cast it: enemies within 10 blocks of you are always slowed and cannot sprint (extreme peace). |
| Curse of Misfortune becomes **Misfortune** | Knight of Misfortune (1) | An area, 12 blocks where you aim, 20 seconds: every enemy inside gets level 2 at once and one more every 5 seconds (up to 5). Mishaps hit harder. |
| Sword of Darkness | Knight of Misfortune (1) | Three swings in a row within 2 seconds carry three **edicts**: **Eternal Sleep** (deep Slumber, 6 seconds), **Despair** (Fear and Pacify, silenced, 30% weaker) and **Destruction** (heavy damage, armor and weapons lose a good part of their durability). |
| Misfortune becomes **Catastrophe** | Darkness (0) | Level 5 mishaps are catastrophes: lightning (no fire), collapsing footing, a fatal fall, a bolt of falling stone. Misfortune is also always on a weaker enemy that hurts you. |
| Concealed State | Darkness (0) | Also works on bosses (for 3 seconds), and a target hidden inside your Concealed World stays erased for 20 seconds. |

## Passives

| Passive | From Sequence |
| --- | --- |
| Nocturnality: the darker it is, the stronger you are. Full dark (night, caves, the Night Domain): Strength I, Speed I, faster regeneration; **Night Vision always**; and you sense unseen danger in the dark (a soft chime when something hunts you from out of sight) | Sleepless (9) |
| Steady Hand: bows and crossbows draw 25% faster, and falls under 6 blocks do not hurt (the books' marksmanship and climbing) | Midnight Poet (8) |
| Resonance: your poems cost 25% less and can be cast while silenced; spirituality regenerates 25% faster | Nightmare (7) |
| Spirit Sight: spirits, summons and the sleepers you have put down are visible to you within 16 blocks without any ability | Soul Assurer (6) |
| Spirit Teeth: four spirit slots (see "Key 5 wheel") | Spirit Warlock (5) |
| Deep Night: Nocturnality is stronger (Strength II and Regeneration I in full dark) but bright midday sun under open sky gives you Weakness I (the Night Domain cancels it). Veil: standing still in the dark for 3 seconds hides your mind (`Unnoticed`) | Nightwatcher (4) |
| Horror Aura: within 8 blocks every enemy weaker than you trembles (mobs of low health flee; weaker players get slowness and nausea, strongest when they look at you). It cannot be turned off; sneaking only halves it | Horror Bishop (3) |
| Presence of Concealment: Divinations slip off you: you cannot be picked by abilities that need to see or read you while you stand in the dark or sneak | Servant of Concealment (2) |
| Archangel Body: six more hearts, Strength I, Speed I. Ill Omen: whoever hurts you has a 25% chance to gain a level of Misfortune | Knight of Misfortune (1) |
| Eternal Slumber: Slumber, Fear, Pacify, blindness and taunts do not take, supernatural damage is 50% less. Horror: every 6 seconds an enemy that looks at you within 24 blocks flinches and its swing, shot or ability is cancelled | Darkness (0) |

## Misfortune

Misfortune is the Pathway's signature: a number of levels (1 to 5) an entity carries for a while. Each second it rolls: chance of a mishap = 10% x level (50% at level 5). A mishap is picked from the ones its level allows:

| Mishap | From level | What happens |
| --- | --- | --- |
| Stumble | 1 | Slowness IV for 1 second and a small trip. |
| Fumble | 2 | The item in its hand and its armor lose 6 durability, and its next attack is 30% weaker. |
| Backfire | 3 | Its next shot flies wide (up to 30 degrees off). |
| Falling stone | 4 | Magic damage of 5% of its maximum health, from nobody (the number shown as the caster's, `DamageFx.credit`). |
| Calamity | 5 (and Catastrophe) | Lightning with no fire, 15% of its maximum health. |

Levels last 20 seconds and a new level makes all of them last again. Bosses carry half the levels. Allies and the caster are never struck by it. `Misfortune.addImmunity` lets a Pathway (Knight of Misfortune from Sequence 1 on) shrug it off, as `Decay` has.

## Starting numbers

Spirituality costs and cooldowns follow the scale of Hand Of God (a Sequence 9 ability costs 250 even though Sequence 9 holds only 20, as in the other Pathways). `PL` is `Beyonder.powerLevel` (1 to 8). All are starting points: the code's constants are the truth once it exists.

| Ability | Cost | Cooldown | Numbers |
| --- | --- | --- | --- |
| Spirit Vision | 250 | 15 s | 12 s, 32 blocks (48 in the dark) |
| Dowsing | 250 | 30 s | 5 s line, 128 blocks for a Beyonder |
| Tranquilize | 300 | 10 s | 10 blocks |
| Lullaby | 300 | 30 s | 8 blocks, 8 s sleep |
| Pacify | 300 | 30 s | 16 blocks, 8 s |
| Fear | 300 | 20 s | 12 blocks, 6 s |
| Nightmare Limbs | 700 | 15 s | 4 limbs, 12 s, 3 x PL a lash, backlash 20% of health |
| Nightmare State | 700 | 30 s | 35 a second, 48 blocks, 30 s |
| Dream Invasion | 400 | 10 s | 24 blocks, 6 s |
| Requiem | 2500 | 30 s | 10 blocks, 10 s, silenced for 6 s |
| Soothing Requiem | 2000 | 30 s | 12 blocks, 10 s |
| Agitating | 1500 | 30 s | 16 blocks, 10 s |
| Terror Banshee | 3000 | 30 s | 30 s, shriek every 3 s, 10 blocks |
| Earthen Spirit | 3000 | 30 s | 8 s, 24 blocks out |
| Gale Spirit | 3000 | 45 s | 20 s |
| Night Domain | 8000 | 90 s | 20 blocks, 40 s |
| Hair Entanglement | 4500 | 20 s | 8 blocks, 4 s |
| Curse of Misfortune | 4000 | 20 s | level 3, 20 s |
| Demonic Wolf Form | 8000 | 180 s | 30 s, 400 a second |
| Sword of Darkness | 10000 | 20 s | 40 s, tide 4 x PL, melts 1 x PL a second for 4 s |
| Nightmare World | 12000 | 120 s | 24 blocks, 30 s |
| Concealed State | 15000 | 45 s | 8 s, 40 blocks |
| Secret Concealment | 15000 | 60 s | 16 blocks, 45 s |
| Concealed World | 20000 | 120 s | 16 blocks, 20 s |
| Misfortune (1) / Catastrophe (0) | 25000 / 35000 | 60 s / 90 s | 12 blocks, 20 s |
| Mantle of Darkness | 30000 | 90 s | 25 s |
| Command Spirits | 30000 | 120 s | 40 blocks, 8 marionettes, 40 s |
| Eternal Rest | 40000 | 90 s | 40% health, 60 s ward |
| Destination of All Things | 50000 | 180 s | 24 blocks, 12 s |

## Built from

Every ability uses what is in the spell kit already, except the names in **bold**, which are new parts (see the next section).

| Ability | Built from |
| --- | --- |
| Spirit Vision | `Spells.livingAround`, `ParticleShapes.spawnFor` (so only you see the glow), `SpellFx.circleUnder`; hidden `Trap`s need a small "traps near here" query on `Trap` |
| Dowsing | `Spells.enemiesAround`, `Mob.getTarget`, `SpellFx.tether` between two points, `HudTimer` |
| Tranquilize | `Spells.cleanse`, `Spells.alliesAround`, `GiantForm.madness` |
| Lullaby, Pacify | **`Slumber`** (SLEEP, LIMP), `Spells.enemiesInCone`, `Marks` for "heard again" |
| Fear | **`Fear`**, `Spells.enemiesInCone` |
| Agitating | `Taunt.brawl`, `Vulnerable.add`, `Spells.harmfulEffects` |
| Nightmare Limbs | `ConjuredArmor` (the layer is the limbs' health, `onEnd` is the backlash), `AnimatedShot` or `SpellFx.tether` for the lashes, `ParticleShapes.modelOnTurning` for the limbs on your back |
| Nightmare State | `Concealment`, `Flight`, `Unnoticed`, a `ModelFx` for the body left behind, `HudTimer`, `Spells.freeSpotNear` for the way back |
| Dream Invasion, Dream Shaping | **`Slumber`**, `AbilityMenu` (the three dreams), `Marionette.control`, **`Fear`**, `SoulCore.getPathway` for Secrets |
| Requiem, Soothing Requiem | `Zone.around` (`onInside` with `ticksInside` for "slows more, then freezes"), **`Silence`**, `Root`, `Spells.isEvil`, `Spells.cleanse`, `SupernaturalResistance.grant` |
| Terror Banshee | `Marionette.spawnSummon`, **`Fear`**, `Blind`, `Spells.wave` for the shriek, `Spells.addAllyCheck` once |
| Earthen Spirit | **`GroundSlide`** (lifted from Mercury Liquefaction), `Concealment`, `Spells.blinkSpot`, `FallGuard` |
| Gale Spirit | `Flight.grant`, `PaleEmperorFx.wings` or a model of your own |
| Command Spirits | **`Slumber`**, `Marionette.control`, `Spells.isEvil`, `HudTimer` |
| Night Domain | **`Domain`** (lifted from Divine Kingdom) or two `Zone`s, `Decay`, **`Misfortune`**, **`Nocturnality.forceNight`**, `Spells.keepEffect` |
| Hair Entanglement | `Spells.enemiesInCone`, `Root`, `SpellFx.tether` (one hair each), **`Slumber`** |
| Curse of Misfortune, Misfortune, Catastrophe | **`Misfortune`**, `SpellFx.circleUnder`, `Zone.at` for the area; Catastrophe may lift small disasters from `Apocalypse.DISASTERS` |
| Demonic Wolf Form | `GiantForm` (size, reach, madness), `Physique`, a `ModelFx` wolf that grows with the Sequence |
| Sword of Darkness | `ConjuredWeapon` (full-swing detection in `onHit`), `Spells.wave`, `Spells.strikeThrough`, **`Slumber`**, **`Silence`**, **`Fear`**, `Decay` for the wear of Destruction |
| Nightmare World | `Dome.sphere` (`keepsEnemiesIn`, `shields`, `wardsEffects`), `Zone` for the second-by-second part, **`Slumber`**, `Spells.pull`, **`Nocturnality.forceNight`** |
| Concealed State | `Concealment.hide` + `Root` + `Blind` as one package, `Concealment.reveal` for Reverse Concealment |
| Secret Concealment | `Unnoticed`, **`Spells.addUnchoosableCheck`**, `Zone.at` |
| Concealed World | `Dome.sphere` (`keepsEnemiesIn`, `stopsProjectiles`), **`Spells.addUnchoosableCheck`**, `HandOfGodFx.sphere`-style opaque shell |
| Mantle of Darkness | `ConjuredWeapon` (reach, damage, the two nearest hit for half), `ConjuredArmor` (a quarter of damage shed), `Physique` |
| Eternal Rest | **`Slumber`** (ETERNAL), `Spells.strikeThrough`, `DeathWard.arm` for your own rest |
| Destination of All Things | `Dome.sphere`, `Zone`, `Spells.pull`, `Spells.strikeThrough`, `DeathWard` re-armed once a second so you cannot die inside |
| Passives | `SequencePassives` (`effect` / `custom`), `Physique`, `Unnoticed`, `SupernaturalResistance.grant(..., false, ...)` |

## New reusable parts worth writing first

Every one of these is wanted by more than one ability, and by other Pathways too. In order:

| Part | What it is | Used by |
| --- | --- | --- |
| `status/Slumber` | Sleep with kinds: SLEEP (wakes when hurt), DEEP (the first hits do not wake it), LIMP (awake but cannot attack or cast: Pacify), ETERNAL (only time or `wake` ends it). Players: held and screen dark (`Root` EVERYTHING + `Blind`); mobs: AI off. `Slumber.put`, `isAsleep`, `kind`, `wake`, and an `addImmunity` hook like `Decay`'s. | Lullaby, Pacify, Dream Invasion, Hair Entanglement, Requiem, Nightmare World, Sword edicts, Eternal Rest, Command Spirits |
| `status/Fear` | The other half of `Taunt`: a mob flees from a source, a player cannot sprint toward it, is slowed and nauseous, with a dark vignette. `Fear.apply(entity, from, ticks)`, `isAfraid`, `addImmunity`. | Fear poem, Terror Banshee, Horror Aura, Despair edict, Terror dream |
| `status/Misfortune` | Levels, the mishap roll and the table above; `Misfortune.add(target, level, ticks)`, `level`, `clear`, `addImmunity`. | Curse, Night Domain, Misfortune, Catastrophe, Ill Omen |
| `status/Silence` | Ability keys do nothing for a while (asked next to `StolenAbilities.blocksKey` in every key packet); summons and marionettes stop attacking; ranged mobs cannot shoot. | Requiem, Despair edict, the "Silence" authority |
| `status/Nocturnality` | One number 0 to 1 for "how dark is it for this entity" (light level, time of day, open sky), and `forceNight(entity or area, ticks)`. `Spells.inDaylight` is its other half. | The whole passive ladder, Night Domain, Nightmare World |
| `Spells.addUnchoosableCheck((caster, target) -> ...)` | Extends `canBeChosen`, which only knows `Unnoticed`: an area or a Pathway may say "this one cannot be picked". | Secret Concealment, Concealed World; `Unnoticed` itself could use it |
| `combat/Domain` | **A lift, not new:** Divine Kingdom is written inline in `HandOfGodTwilight` (`divineKingdom`, `kingdomTouches`, `kingdomCut`). Pull the shared shape out: allies buffed, enemies weakened, an ambient look, cast again to end. | Night Domain, Divine Kingdom, Nightmare World |
| `status/GroundSlide` | **A lift:** Mercury Liquefaction's sliding movement is private in `HandOfGodTwilight` (`mercuryStep`). Hidden, fast along the ground, re-forming where aimed. | Earthen Spirit, Mercury Liquefaction |

`Slumber`, `Fear` and `Misfortune` first: Midnight Poem, Requiem, the sword and half of the top Sequences cannot be written without them. The two lifts are optional and can wait; until then Night Domain can use two `Zone`s and Earthen Spirit can copy Mercury's loop.

## What is already in the repository

`KnightOfMisfortune.java` is a work-in-progress file: its one ability, `knightOfMisfortuneRush`, is a copy of Hand Of God's Warrior's Charge (using `HandOfGodFx`) with a "listen" pulse (built on `HangedManFx`) on the sneak cast, registered as a single placeholder line in `PathwayAbilities.ALL` and called from key 1 only. It is **replaced** by Spirit Vision and Dowsing when the code is written; keep the naming `knightOfMisfortune<Name>`. Following the Hand Of God split, the code can go in `KnightOfMisfortune` (keys 1, 3, 4, passives), `KnightOfMisfortunePoems` (key 2), `KnightOfMisfortuneSpirits` (key 5), `KnightOfMisfortuneNightwatcher` (key 6), `KnightOfMisfortuneDarkness` (key 7) and `KnightOfMisfortuneFx`.

When it is written, these places need a line (the checklist from [SPELL_KIT.md](SPELL_KIT.md) and [SEQUENCES.md](SEQUENCES.md)):

| Where | What |
| --- | --- |
| `network/SKeyPressAbility...Used` (keys 2 to 7) | one `import static` and one line each (key 1 already has one) |
| `PathwayAbilities.ALL` | seven lines (replace the placeholder); wheel keys: stolen casts take the last pick or a random entry; Spirit Commanding and Nightmare State are own-Pathway-only (they need your teeth and your body) |
| `SequencePassives` | the passives above |
| `PathwayHudIcon` | the Pathway's icon |
| [SEQUENCES.md](SEQUENCES.md) | a row in "Required Sequence of every ability key", a row per passive, and the Pathway name list |

## Looks, sounds and pictures (ready)

The abilities are not written yet, but everything they need to look and sound right is: `KnightOfMisfortuneFx.java` (next to the other pathway classes). It only shows things; the ability does the sleeping, holding, hiding, damage and moving, at the ticks its constants name. `/knightfx all` plays every display in game, `/knightfx <name>` one of them.

| Ability | Normal cast | Sneak cast |
| --- | --- | --- |
| 1 Night Sight | Spirit Vision: `spiritVision(viewer, ticks)`, then `spiritVisionMark(viewer, seen, spirit)` on everything seen (only the viewer sees either) | Dowsing: `dowsing(sl, player, toward, ticks, tag)`, `dowsingEnd` |
| 2 Midnight Poem (wheel) | Every poem: `recite(sl, player, Poem.X, tag)`. Then `tranquilize` + `tranquilized` + `tranquilizeReveal`; `lullaby` + `slumber`/`wake` on each sleeper at `POEM_LANDS`; `pacify`; `fear` + `afraid`; `agitate`. Icons `WHEEL_TRANQUILIZE`, `WHEEL_LULLABY`, `WHEEL_PACIFY`, `WHEEL_FEAR`, `WHEEL_AGITATING` | the same, without the wheel |
| 3 Nightmare | Nightmare Limbs: `nightmareLimbs` (grown at `LIMBS_GROWN`), `limbLash(sl, player, target, index)`, `limbsTear` | Nightmare State: `nightmareState` + `sleepingBody`, `nightmareTick`, `nightmareReturn`. Dream Invasion: `dreamInvade` (in at `DREAM_OPENS`), `dreamShape(sl, sleeper, Dream.X)`, `dreamEnd` |
| 4 Requiem | `requiem` (sung at `REQUIEM_RISEN`), `requiemPulse` every second, `requiemOn(sl, enemy, depth)`, `requiemEnd` | Soothing Requiem: `soothingRequiem`, `soothed` on each ally |
| 5 Spirit Commanding (wheel) | `spiritRelease` before each spirit. Banshee: `banshee` on the summoned body, `bansheeWail`, `bansheeEnd`. Earthen Spirit: `earthMerge` (sunk at `EARTH_SUNK`), `earthTick`, `earthEmerge`. Gale Spirit: `gale`, `galeTick`, `galeEnd`. Command Spirits: `commandSpirits`, `spiritBound`. Icons `WHEEL_TERROR_BANSHEE`, `WHEEL_EARTHEN_SPIRIT`, `WHEEL_GALE_SPIRIT`, `WHEEL_COMMAND_SPIRITS` | the last pick again |
| 6 Nightwatcher (wheel) | Night Domain: `nightDomain` (spread at `DOMAIN_RISEN`), `domainPulse`, `domainCorrode`, `domainEnd`. Hair: `hairEntangle`, `hairBind` at `HAIR_BINDS`. Curse: `curse`, `misfortune(sl, target, level, ticks, tag)` at `CURSE_LANDS`, `misfortuneArea` (Sequence 1), `mishap(sl, victim, Mishap.X)`, `catastrophe`. Wolf: `wolfForm(..., complete)` (full size at `WOLF_RISEN`), `wolfStep`, `wolfHowl`, `wolfBite`, `wolfMadness`, `wolfFormEnd`. Icons `WHEEL_NIGHT_DOMAIN`, `WHEEL_HAIR_ENTANGLEMENT`, `WHEEL_CURSE_OF_MISFORTUNE`, `WHEEL_DEMONIC_WOLF` | the last pick again |
| 7 Darkness and Concealment (wheel) | Sword: `darkSword` (formed at `SWORD_FORMED`), `darkSwing`, `darkTide` (returns its ticks), `tideMelt`, `edict(sl, player, target, Edict.X)`, `darkSwordEnd`. `nightmareWorld` (closed at `WORLD_CLOSED`), `nightmareWorldPulse`. `concealedState` (gone at `CONCEALED`), `concealedStateEnd`, `reverseConcealment`. `secretConcealment`, `secretPulse`. `concealedWorld`, `concealedWorldEnd`. `mantle`, `mantleTick`, `mantleSwing`, `mantleEnd`. `eternalRest` (falls at `REST_FALLS`), `restWard`, `restWake`. `destination` (open at `DESTINATION_OPENS`), `destinationTick`, `dissolve`, `destinationEnd`. Icons `WHEEL_SWORD_OF_DARKNESS` ... `WHEEL_DESTINATION_OF_ALL_THINGS` | the last pick again |
| Passives | `nocturnality`, `dangerChime`, `horrorAura`, `horrorFlinch`, `illOmen` | |

Every timed cast can show its picture and clock above the hotbar: `KnightOfMisfortuneFx.status(player, Status.X, ticks)`, and `statusEnd` to take it away (24 statuses, `SLUMBER` and `FEAR` included for whoever carries them). The Pathway's icon beside the hotbar is registered in `PathwayHudIcon` under `"Knight Of Misfortune"`.

What is in the repository, all of it generated by the scripts in `tools/model_gen/knight_of_misfortune/` (see "Knight Of Misfortune: ready-made displays" in [MODEL_FX.md](MODEL_FX.md)):

| What | Where | Made by |
| --- | --- | --- |
| 20 model files: the Sword of Darkness, a Nightmare Limb (4 frames, straight to curled), the Demonic Wolf (incomplete, and complete with six arms), the Dowsing pendulum, the Terror Banshee, a soul out of its body; and the pictures: the seal of Darkness, a poem's script, a dream's oval, the Misfortune horseshoe, the Horror eye, the secret seal, the Eternal Rest sigil, a wall of fog (animated), the tide of Darkness (animated) and the vortex of the Destination of All Things (animated) | `models/model_fx/knight_of_misfortune/`, `textures/model_fx/knight_of_misfortune/` | `models.py` (+ `textures.py`) |
| 21 wheel pictures (32 px), 24 status pictures (16 px), the Pathway icon | `textures/gui/ability_menus/knight_of_misfortune/`, `textures/gui/knight_of_misfortune/`, `textures/gui/pathway_icons/knight_of_misfortune.png` | `icons.py` |
| 8 particle pictures: `fx/note`, `fx/sleep_z`, `fx/wisp`, `fx/hair`, `fx/tooth`, `fx/horseshoe`, `fx/skull`, `fx/eye` | `textures/particle/fx/` | `particles.py` |
| 47 sounds (52 files): Spirit Vision, dowsing, whispered poems, every poem, limbs, lashes and tearing, the soul leaving and returning, dreams, Requiem, the spirits in the teeth, the banshee, earth, gale, the command, the Night Domain, hair, the curse, mishaps, Catastrophe, the wolf (form, howl, bite, step), the Sword of Darkness, its tide and three edicts, the Nightmare World, Horror, concealment and revealing, secrets, the Concealed World, the Mantle and its shadow swings, Eternal Rest, the Destination of All Things and dissolving | `sounds/knight_of_misfortune/`, `sounds.json`, subtitles in `lang/en_us.json`, registered in `sounds/ModSounds.java` (`KNIGHT_...`) | `sounds.py` |

## Left out on purpose

- **Beyonder Extraction** (Servant of Concealment): taking a Beyonder's characteristics has no counterpart in the game and Worm Of Time's Theft already covers stealing abilities.
- **Rituals and divination beyond Dowsing**: the books' "limited divination" and "ritualistic magic" are left to Dowsing; a bigger ritual system would be a Pathway-wide feature, not an ability.
- Anything that changes blocks (a real Concealed World that moves a whole area out of the world, a real dream dimension).

## Questions

1. **Sequence 9 costs.** I followed the other Pathways (250), which Sequence 9 (20 spirituality) cannot pay: Spirit Vision and Dowsing only work from Sequence 8. Keep that, or set them to about 10?
2. **Spirits.** Spirit Commanding has three fixed spirits. The books seal them from real spirits; doing that (sealing a spirit from a killed ghost or undead, and keeping it between casts) would be a bigger system. Fixed spirits for now, or the sealing system?
3. **Wheels.** Four of seven keys are wheels. I can move Nightmare State/Dream Invasion onto a wheel of their own, or fold Requiem into the poem wheel, if you want fewer key types.
