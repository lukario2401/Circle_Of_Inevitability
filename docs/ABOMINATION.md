# Abomination: ability design

The Abomination Pathway is the Chained Pathway of Lord of the Mysteries, the Temperance tarot card: Prisoner (9), Lunatic (8), Werewolf (7), Zombie (6), Wraith (5), Puppet (4), Disciple of Silence (3), Ancient Bane (2), Abomination (1), Chained (0). Source: the Chained Pathway and Chained Pathway/Advancement pages of the Lord of the Mysteries wiki. The Chained Pathway/Abilities page could not be fetched when this was written, so every ability below is designed from the theme and from the general abilities the Pathway page lists, not copied from a per-Sequence list. See "Open questions".

The abilities are not written as code yet; their looks and sounds are (see "Looks and sounds"). This file is the plan: what every ability does, the numbers to start from, which existing parts of the [spell kit](SPELL_KIT.md) build it, and which new reusable parts are worth writing first. When the code is written, move "Planned" to "What is built" (see [HAND_OF_GOD.md](HAND_OF_GOD.md)).

Pathway name in code: `"Abomination"`.

## Theme

- **Binding and restraint.** The Pathway's own line is that the mind is a prisoner of the body and the body a prisoner of the world. Chains, thorns, bandages, coffins and seals are its tools, and Temperance (balance, patience, holding back) is its card.
- **A strong body that is hard to keep under control.** A Beyonder here has great strength, speed and defense, regenerates, and trades rationality for power. It is weaker or wilder under the full moon.
- **The dead and the spirit side.** Zombies, ghosts, possession, passing through reflections, death spells.
- **Curses.** From the high Sequences: binding foes, controlling inanimate objects in a radius, dreadful curses, cursing through a connection, and the Abomination form.
- **The Mythical Creature Form** is an invisible, intangible collection of curses, bound together by a visible vessel. The wiki's own example of a vessel is a doll bound by thorny vines.
- **Sequence 0** is made of black thorns, yellow oily bandages and bright sharp roses; its ritual is to mummify yourself and lie sealed in a coffin and a mausoleum.
- Colors: eminence purple for the Pathway, grey-white and icy blue for the Lunatic and the Zombie, dark green for the Werewolf, crimson and black for the Prisoner's blood and the Abomination's slime, bandage yellow and rose red for Chained. Pathway color: `0x6C3082`.
- Nothing here breaks blocks. Chains, thorns, coffins and mist are looks and holds; the ground is never changed. Animate Objects moves only loose things (dropped items, projectiles, armor stands, boats, minecarts), never blocks.

## How it is laid out

It follows the Hand Of God pattern: seven keys (G H J K L N M), every plain key has a normal cast and a sneak cast. Four keys (4, 5, 6, 7) are wheels, where every entry has its own Sequence, cost and cooldown; on a wheel key, sneak casts the last pick again (`AbilityWheel.quickCast`).

The abilities do not each get a Sequence of their own. Sequences 9, 8, 7, 4 and 2 have two abilities, Sequences 5 and 3 have three, Sequence 6 has four, Sequence 0 has three, and Sequence 1 has one but strengthens much of what is already there (see "Grows with the Sequence"). Sequences 2, 1 and 0 are not left without abilities. In all, 24.

| Sequence | Abilities |
| --- | --- |
| Prisoner (9) | Blood Chain, Restraint |
| Lunatic (8) | Moonstruck, Lunar Frenzy |
| Werewolf (7) | Rending Pounce, Werewolf Form |
| Zombie (6) | Raise the Dead, Grave Chill, Undying Flesh, Death Sentence |
| Wraith (5) | Mirror Step, Possession, Ghost Host |
| Puppet (4) | Animate Objects, Grave Shackles |
| Disciple of Silence (3) | Curse by Connection, Cursed Artifact, Rite of Silence |
| Ancient Bane (2) | Bane Mist, Ancient Blight |
| Abomination (1) | Abomination Form |
| Chained (0) | Thorned Mausoleum, Coffin of Ages, Thorn Garden |

## Keys

| Key | Ability | Sequence | Normal cast | Sneak cast |
| --- | --- | --- | --- | --- |
| 1 | Blood Chain | Prisoner (9) | A chain of dark blood whips from your hand to the enemy you look at (16 blocks), wraps it and drags it to within 2 blocks of you, then holds it for 1.5 seconds. The wound bleeds for 4 seconds. | Restraint: you bind yourself in your own chains for 10 seconds. You are slowed and cannot sprint, but take 50% less damage from melee and projectiles and cannot be knocked back. Cast again to take the chains off. |
| 2 | Moonstruck | Lunatic (8) | You show the enemy you look at (24 blocks) a moon that is not there. For 6 seconds it is mad: a mob forgets you and turns on the nearest other mob (or flees if it is alone); a player is slowed, sick and cannot sprint toward you. | Lunar Frenzy: for 10 seconds you give in to the moon. Strength II, Speed II and Regeneration I. When it ends you crash: Weakness and Slowness for 6 seconds. |
| 3 | Rending Pounce | Werewolf (7) | You leap onto the enemy you look at (14 blocks) and claw it three times in quick succession. Every claw bleeds; the last one throws it down. No fall damage until you land. | Werewolf Form: for 40 seconds you are a werewolf: a tenth bigger, Strength II, Speed II, Night Vision, and claws that bleed and stop the target healing. Madness slowly rises; at full the form ends and you are left weak. Cast again to end it. |
| 4 | Zombie | Zombie (6) | The Zombie wheel: Raise the Dead, Grave Chill, Undying Flesh, Death Sentence. Each has its own Sequence, cost and cooldown. | The last Zombie pick again, without the wheel. |
| 5 | Wraith | Wraith (5) | The Wraith wheel: Mirror Step, Possession, Ghost Host. | The last pick again, without the wheel. |
| 6 | Chains | Puppet (4) | The Chains wheel: Animate Objects (4), Grave Shackles (4), Thorned Mausoleum (0), Coffin of Ages (0), Thorn Garden (0). | The last pick again, without the wheel. |
| 7 | Curses | Disciple of Silence (3) | The Curses wheel: Curse by Connection (3), Cursed Artifact (3), Rite of Silence (3), Bane Mist (2), Ancient Blight (2), Abomination Form (1). | The last pick again, without the wheel. |

## Key 4 wheel: Zombie

| Entry | Sequence | What it does |
| --- | --- | --- |
| Raise the Dead | Zombie (6) | Where you aim (24 blocks) three zombies claw out of the ground and fight for you for 30 seconds. Enemies killed near the spot in the last 10 seconds rise too, up to three more, as what they were (a skeleton as a skeleton), and also serve for 30 seconds. Bosses do not rise. |
| Grave Chill | Zombie (6) | A cold, lifeless mist in an icy-blue color rolls out round you, 10 blocks, for 8 seconds. Enemies in it slow more the longer they stay, and after 3 seconds are frozen in ice for 1 second (once each). |
| Undying Flesh | Zombie (6) | For 20 seconds your body forgets pain: 35% less damage from everything and 60% less from projectiles, no knockback, bleeding and poison do not take, and you regenerate 2% of your health every second. |
| Death Sentence | Zombie (6) | The enemy you look at (24 blocks) is sentenced. A purple countdown of 5 runs; if it is still alive and within 32 blocks at 0, it takes 30% of its maximum health as magic damage (50% if it was below half health). If it dies of it, it rises for you. Sentencing someone else ends the first. |

## Key 5 wheel: Wraith

| Entry | Sequence | What it does |
| --- | --- | --- |
| Mirror Step | Wraith (5) | Step into the nearest reflection (still water, ice, glass or a block of polished metal within 6 blocks) and come out of the one you aim at, up to 32 blocks away, hidden while you travel (1 second). There has to be a reflection at both ends; nothing to step into or out of costs nothing. |
| Possession | Wraith (5) | You leave your body and ride inside the mob you look at (16 blocks; not a boss or anything with more than 150 health). For up to 60 seconds you see through its eyes and steer it: walk keys, jump, click to strike. It costs spirituality every second. Cast again to step out. |
| Ghost Host | Wraith (5) | Three ghosts rise from the Spirit World and fight for you for 20 seconds, passing through walls. Any vex or phantom within 24 blocks that is not yours is taken over for the same time, in place of one of the three. |

## Key 6 wheel: Chains

| Entry | Sequence | What it does |
| --- | --- | --- |
| Animate Objects | Puppet (4) | Every loose thing within 16 blocks of you (dropped items, projectiles, armor stands, boats, minecarts; never blocks) rises and is thrown at the enemies in sight, shared out, up to 12 things. Each hits once. Projectiles that enemies shot are turned round and become yours. With nothing to move it refuses. |
| Grave Shackles | Puppet (4) | Black chains rise from the ground round every enemy within 10 blocks (up to 8). They are held in place for 4 seconds, and for 8 seconds they feel each other's pain: half of what one takes, the others take too. |
| Thorned Mausoleum | Chained (0) | A sphere of black thorns and yellow bandages closes round the place you aim at, 12 blocks wide, for 30 seconds. Nothing crosses it but your allies. Enemies inside are slowed, cannot heal and bleed from the thorns; allies inside take 40% less damage. |
| Coffin of Ages | Chained (0) | Aimed at an enemy (not a boss): it is wrapped in bandages and sealed in a coffin for 12 seconds, held, asleep and untouchable, and wakes cursed. Aimed at nothing: you seal yourself for 6 seconds, untouchable, and rise healed by half and cleansed, in a burst of thorns that holds everything near you. |
| Thorn Garden | Chained (0) | A garden of black thorns and bright roses grows over 20 blocks for 20 seconds. Enemies in it are slowed, then held, and bleed from the thorns the longer they stay. Allies in it are healed by the roses and get a layer of absorption. |

## Key 7 wheel: Curses

| Entry | Sequence | What it does |
| --- | --- | --- |
| Curse by Connection | Disciple of Silence (3) | Curses an enemy you have a connection to, wherever it is in the world: Misfortune for 30 seconds and 15% more damage taken. A connection is an enemy you hit, chained, cursed or sentenced in the last 10 minutes. The one you look at (64 blocks) is picked; with none in sight, the one you connected with last. |
| Cursed Artifact | Disciple of Silence (3) | You set a relic on the ground where you aim. Only you can see it. The first enemy to step near it is cursed: heavy Misfortune and hushed for 5 seconds, and it becomes your connection. Three relics at once; each waits for 5 minutes. |
| Rite of Silence | Disciple of Silence (3) | A rite of silence over the spot you aim at, 16 blocks wide, for 8 seconds. Enemies in it cannot use abilities, and mobs cannot attack. |
| Bane Mist | Ancient Bane (2) | A pale mist stained with blood and black spots spreads 20 blocks round where you aim for 15 seconds. Everything evil in it goes mad: mobs fight each other, players flinch and slow, and every second in it takes more of their luck (Misfortune up to level 5). |
| Ancient Blight | Ancient Bane (2) | The enemy you look at carries an ancient curse. Every few seconds it jumps to the 2 or 3 nearest enemies, each worse than the last, up to 5 in a chain and 30 in all. What it carries is Misfortune, one level deeper at every jump. |
| Abomination Form | Abomination (1) | Your Mythical Creature Form. For 60 seconds you are the vessel: a doll bound by thorny vines holds a collection of curses. Strength III and 30% less damage. Whoever comes within 8 blocks of you slowly loses luck, and whoever hurts you is cursed. When the form ends, every curse you laid comes due at once: each enemy with your Misfortune in 32 blocks suffers a calamity. |

## Grows with the Sequence

These do not get their own key or entry. The same ability is stronger from the Sequence on.

| Ability | From | What changes |
| --- | --- | --- |
| Blood Chain | Puppet (4) | Two chains: the second goes to the nearest other enemy within 8 blocks of the first. |
| Blood Chain | Abomination (1) | Four chains, and each holds for 3 seconds. |
| Restraint | Disciple of Silence (3) | Damage cut 60%, and slowness does not take. |
| Werewolf Form | Puppet (4), Ancient Bane (2) | A quarter bigger at Sequence 4 (madness grows slower). At Sequence 2 no madness, and it stays until you cast it again. |
| Raise the Dead | Puppet (4), Ancient Bane (2) | Six zombies at Sequence 4. At Sequence 2 ten, and everything they hit carries a level of Misfortune. |
| Possession | Disciple of Silence (3), Ancient Bane (2) | From Sequence 3 players can be hosts too (they carry you, unseen, and are not steered). From Sequence 2 any mob can be a host, a boss included. |
| Ghost Host | Ancient Bane (2) | Six ghosts, and what they touch carries Misfortune. |
| Death Sentence | Ancient Bane (2) | Up to three enemies in sight are sentenced at once. |
| Animate Objects | Abomination (1) | 32 blocks, up to 30 things. |
| Curse by Connection | Ancient Bane (2) | A connection lasts 30 minutes, and the curse is twice as long. |
| Grave Shackles | Chained (0) | Every enemy in sight, up to 24, and the chains do not break while you hold your ground. |
| Abomination Form | Chained (0) | No timer: it stays until you cast it again. |

## Passives

| Passive | From Sequence |
| --- | --- |
| Strength I | Prisoner (9) |
| Lunatic Mind: Fear and nausea do not take. Full Moon: on a full-moon night under the open sky, Strength one level higher and Speed I; Lunar Frenzy and Werewolf Form fill their madness twice as fast and Restraint cuts only 35% | Lunatic (8) |
| Keen Senses: Speed I, Night Vision, and a soft chime when something starts hunting you from out of sight | Werewolf (7) |
| Unliving Body: Regeneration I; poison, hunger and freezing do not take; projectiles do 25% less | Zombie (6) |
| Weightless: no fall damage; spirits, ghosts and summons within 16 blocks are marked for you alone | Wraith (5) |
| Puppeteer: everything you summon or take over lasts 1.5 times as long | Puppet (4) |
| Whispering Curse: whoever hits you in melee has a 15% chance to be cursed (Misfortune I); a hush on you does not stop your abilities | Disciple of Silence (3) |
| Bane Aura: enemies within 10 blocks carry Misfortune I; Misfortune, Fear and Decay do not take on you | Ancient Bane (2) |
| Collection of Curses: curses you lay last 1.5 times as long; whoever hurts you in melee has a 25% chance to be cursed | Abomination (1) |
| Chained Aura: enemies within 16 blocks are slowed and cannot teleport; whoever hits you in melee takes 4 thorn damage | Chained (0) |

## Starting numbers

Spirituality is what each Sequence can pay: 9 holds 20, 8 holds 400, 7 holds 1980, 6 holds 6400, 5 holds 15500, 4 holds 32400, 3 holds 59780, 2 and below hold 78400. Costs follow the other Pathways (250 at Sequence 9, climbing to 30000 to 40000 at Sequence 0). Ticks: 20 are one second. Every number is a start and becomes a constant at the top of the class.

| Ability | Cost | Cooldown | Lasts | Reach / radius | Damage |
| --- | --- | --- | --- | --- | --- |
| Blood Chain | 250 | | 30 (held) | 16 | 14, bleeds 2 a second for 80 |
| Restraint | 250 | | 200 | | 50% less |
| Moonstruck | 300 | | 120 | 24 | |
| Lunar Frenzy | 400 | | 200, crash 120 | | |
| Rending Pounce | 700 | | | 14 | 3 x 14, bleeds |
| Werewolf Form | 1000 | 1200 | 800 | 1.1 times | claws +5, bleed |
| Raise the Dead | 1500 | 200 | 600 | 24 | 3 + 3 zombies |
| Grave Chill | 2000 | 300 | 160 | 10 | slow; ice after 60 |
| Undying Flesh | 1800 | 300 | 400 | | heals 2% a second |
| Death Sentence | 2500 | 600 | 100 (count) | 24 | 30% of maximum health |
| Mirror Step | 3000 | 200 | 20 | 6 in, 32 out | |
| Possession | 4000 | 600 | 1200 | 16 | 100 a second upkeep |
| Ghost Host | 3500 | 400 | 400 | 24 | 3 ghosts |
| Animate Objects | 3000 | 200 | | 16 | 10 each, 12 things |
| Grave Shackles | 4000 | 400 | 80 (held), 160 (link) | 10 | 50% shared, 8 enemies |
| Curse by Connection | 8000 | 600 | 600 | 64 in sight | 15% more taken |
| Cursed Artifact | 6000 | 100 | 6000 (relic) | 1.5 | 3 relics |
| Rite of Silence | 7000 | 600 | 160 | 16 | |
| Bane Mist | 20000 | 900 | 300 | 20 | Misfortune to 5 |
| Ancient Blight | 25000 | 1200 | 100 each | 8 a jump | 5 jumps, 30 victims |
| Abomination Form | 30000 | 3600 | 1200 | 8 | 30% less, calamity at end |
| Thorned Mausoleum | 40000 | 2400 | 600 | 12 | thorns 8 a second |
| Coffin of Ages | 30000 | 1800 | 240 | 32 | |
| Thorn Garden | 35000 | 1800 | 400 | 20 | thorns 6 a second |

## Planned: what each is built from

Most of it is parts that exist already; the new ones are listed under "New reusable parts" below.

| Ability | Built from |
| --- | --- |
| Blood Chain | `Reel.in` on the enemy toward you, `SpellFx.tether` with a dark-blood `SlashFx.line`, `Root.apply` (short), `Spells.strike`, new `Bleeding` |
| Restraint | `ConjuredArmor` (cut, `steady()`), `Spells.keepEffect` for the slowness, `ParticleShapes.modelOnTurning` for the chains round the body |
| Moonstruck | `Spells.aimEnemy`, `Taunt.brawl` on mobs near the target (`Fear.apply` if it is alone or a player), `Spells.keepEffect` for nausea, `SpellFx.circleUnder` for the moon sigil |
| Lunar Frenzy | `Spells.keepEffect` (Strength, Speed, Regeneration), `Later.run` for the crash, a `HudTimer` |
| Rending Pounce | `Reel.in` on yourself toward the target, `Later.run` for the three claws, `Spells.strike`, `Bleeding`, `FallGuard` |
| Werewolf Form | `GiantForm` (scale, reach, madness, ending), `ConjuredWeapon` for the claws (`Bleeding` and `NoHeal` in `onHit`), `Spells.keepEffect`, new `Spells.isFullMoonNight` |
| Raise the Dead | `Marionette.spawnSummon`, new `Fallen` for those who just died (as `riseAsServant` in `Apocalypse.java` does for one) |
| Grave Chill | `Zone.around` (`Spells.keepEffect` slowness by `ticksInside`, `Freeze.apply` at 60, once per enemy in a `Set<UUID>`), `SpellFx.shockRing`, mist particles |
| Undying Flesh | `ConjuredArmor` (cut, `steady()`, new projectile cut), `Spells.keepEffect` Regeneration, `Spells.cleanse` for poison |
| Death Sentence | `Countdown.seconds(5)` with `keepWhile` and `onFinish`, `StoredDamage.deal` for the magic hit, `Fallen` or `Marionette` if it dies |
| Mirror Step | `Concealment.hide`, `Spells.blinkSpot` / `freeSpotNear`, new `Spells.reflectionNear` / `reflectionAimed` (as `lightNear` / `lightAimed` do for Light Concealment) |
| Possession | `Parasite.enter` with `Hooks` (steering settings for a mob, `allowsKey` for key 5 to step out), an upkeep paid once a second and a `HudTimer`, as `parasitize` in `WormOfTime.java` |
| Ghost Host | `Marionette.spawnSummon` with vexes, one after another; `Marionette.control` on other vexes and phantoms near you |
| Animate Objects | new `Spells.looseThingsAround` and `Hurled`, `Spells.enemiesInSight`, `Spells.strike` |
| Grave Shackles | `Root.apply` (movement), `DamageLink.share`, `SpellFx.tether` from ground points (the chain gang recipe in the kit), `Spells.enemiesAround` |
| Thorned Mausoleum | `Dome.sphere` (`keepsEnemiesIn`, `stopsProjectiles`, `shields`), a `Zone` inside for slow, `NoHeal` and `Bleeding`, `HandOfGodFx.sphere`-style shell in thorns and bandages |
| Coffin of Ages | `Slumber.put` (ETERNAL), `Root.apply` (EVERYTHING), `Concealment.hide` (so nothing can reach it), `Misfortune.add` and `Vulnerable.add` when it wakes; the self-seal is `Concealment.hide` on you plus `Spells.cleanse`, then `Root.apply` round you |
| Thorn Garden | Two `Zone`s: one on enemies (`Spells.keepEffect` slowness, `Root.apply` after `ticksInside`, `Bleeding`), one on allies (`.affects(one -> !Spells.isEnemy(player, one))`, Regeneration, absorption), `Spells.groundNear` and `SpellFx.risingRings` for the roses |
| Curse by Connection | new `Connection`, `Misfortune.add`, `Vulnerable.add`, `SpellFx.circleUnder` on the far enemy, a `HudTimer` |
| Cursed Artifact | `Trap.at(...).set(player, sl, "abomination_relic", 3)`, `ParticleShapes.spawnFor` so only you see it, `Misfortune.add`, `Silence.apply`, `Connection` |
| Rite of Silence | `Zone.at` (`Silence.apply` every beat to whoever is inside), `SpellFx.runeCircle`, `Spells.sound` |
| Bane Mist | `Zone.at` (`Taunt.brawl` on mobs every 3 seconds, `Fear.apply` on players, `Misfortune.add` growing with `ticksInside`), `Nocturnality.forceNight` for the dark look, mist particles |
| Ancient Blight | `Contagion.spread()` (`every` adds Misfortune by `stage`, `passesTo(2, 3)`, `generations(5)`, `maxVictims(30)`), `SpellFx.circleUnder` |
| Abomination Form | `GiantForm` (reach, ending, no growth), `ConjuredArmor`, `Zone.around` (Misfortune for those near), a `LivingHurtEvent` hook for "whoever hurts you", `Misfortune.befall(victim, CALAMITY)` on everyone with your Misfortune at the end, the vessel as a model round you |
| Passives | `SequencePassives` (`effect(...)` and `custom(...)` lines), `Physique` for the bigger body, `Fear.addImmunity`, `Silence.addCastsThrough`, `Decay.addImmunity`, `Misfortune.addImmunity` |

## New reusable parts worth writing first

These come from this Pathway and would be useful to the others too. Each is one small class in `status/` or `combat/` (or a method in `Spells`) that looks after itself, like the rest of the kit. Each is described by what it should do so that it can be coded straight from here, and each should get a section in [SPELL_KIT.md](SPELL_KIT.md) when it is written.

| Part | Where | What it does | Used by |
| --- | --- | --- | --- |
| `Bleeding` | `status/` | `Bleeding.apply(target, owner, ticks, perSecond)`: damage once a second, credited to the owner (`DamageFx.credit`). Applying again refreshes the time and keeps the bigger number. `isBleeding`, `ticksLeft`, `remove`, `addImmunity(entity -> ...)` for those that cannot bleed. Ends when the target dies. Saved with the entity. (It is the `Bleeding` of "Making a status of your own" in the kit, finished.) | Blood Chain, Rending Pounce, Werewolf Form, Thorned Mausoleum, Thorn Garden |
| `Connection` | `status/` | Who you are connected to: `Connection.touch(player, target, ticks)`, `has(player, target)`, `all(player)`, `nearestTo(player, point)`, `latest(player)`, `clear(player, target)`. `Spells.strike` touches it for a short time by itself, so a hit is a connection; abilities may touch it for longer. Not saved. | Curse by Connection, Cursed Artifact, Abomination Form, anything that tracks, marks or follows someone |
| `Fallen` | `combat/` | Remembers who died near the player's side: `Fallen.near(player, sl, center, radius, maxAgeTicks)` gives each one's entity type, where it fell and how long ago, and `Fallen.take(record)` removes it so it rises only once. Records itself on every death and forgets after 20 seconds. Bosses are left out. | Raise the Dead, Death Sentence, Ghost Host; `riseAsServant` in `Apocalypse.java` can use it |
| `Hurled` and `Spells.looseThingsAround` | `combat/` | `looseThingsAround(player, sl, center, radius)` lists what is not alive and moves: dropped items, projectiles, armor stands, boats, minecarts, nearest first. `Hurled.at(thing, owner, target).speed(1.4).damage(10f).onHit(...).launch(sl)` flies it straight at the target each tick and hits the first enemy it touches; a projectile has its owner changed to the caster and is turned round instead. Nothing here touches blocks. | Animate Objects; any telekinesis |
| `Spells.reflectionNear`, `reflectionAimed`, `isReflective` | `combat/` | The reflective spots near a point and the one nearest the crosshair, as `lightNear` and `lightAimed` are for light. Reflective: still water, ice (all kinds), glass and glass panes, and blocks of polished metal. | Mirror Step; the Apocalypse's Mirror World could use them |
| `ConjuredArmor.projectileCut(f)` | `status/` | One more option: projectiles are cut by their own number as well as by `.cut(...)`. | Undying Flesh, the Zombie's passive |
| `Spells.isFullMoonNight(sl)` | `combat/` | `true` at night when the moon is full. One call so the Pathway's full-moon rules do not each work it out. | Lunatic passive, Lunar Frenzy, Werewolf Form, Restraint |
| `AbominationFx` | `beyonder_characteristics/abomination/` | Done (see "Looks and sounds" below). Looks and sounds only, as `HandOfGodFx` does. | All |

Two are cheap and give the most: `Bleeding` and `Connection`. Five abilities use `Bleeding` and four use `Connection`. Write them first, then `Fallen`.

## Looks and sounds

These are already in the repository, ahead of the abilities: `AbominationFx.java` plays them, and everything it uses is generated by the scripts in `tools/model_gen/abomination/` (see "Abomination: ready-made displays" in [MODEL_FX.md](MODEL_FX.md)). `/abominationfx all` plays every display in game. Each call below only shows; the ability does the work at the tick its constant names, with `Later.run`.

| Ability | Calls | Do the work at |
| --- | --- | --- |
| Blood Chain | `bloodChain`, then `bloodChainHold` while it drags and holds, `bloodChainEnd`; `bleed` once a second while `Bleeding` lasts | `CHAIN_REACHES` |
| Restraint | `restraint`, `restraintHit` on each blow it softens, `restraintEnd` | `RESTRAINT_LOCKED` |
| Moonstruck | `moonstruck`, `madnessTick` once a second, `moonstruckEnd` | `MOON_SHOWN` |
| Lunar Frenzy | `lunarFrenzy`, `frenzyTick`, `frenzyCrash` when it ends, `frenzyEnd` after the crash | |
| Rending Pounce | `pounceLeap`, `pounceTrail` every tick in the air, `pounceClaw` for index 0, 1, 2 | |
| Werewolf Form | `werewolfForm`, `werewolfClaw` on each hit, `werewolfMadness` as it rises, `werewolfFormEnd`; `howl` on its own | `WOLF_RISEN` |
| Raise the Dead | `raiseTheDead` on the spot, `zombieRises` for each zombie, `fallenRises` for each of the fallen, `servantMark` | `HANDS_UP` after each `zombieRises` |
| Grave Chill | `graveChill`, `graveChillTick`, `chilled`, `chillFreeze`, `graveChillEnd` | `CHILL_SPREAD` |
| Undying Flesh | `undyingFlesh`, `undyingTick`, `undyingHit`, `undyingEnd` | |
| Death Sentence | `deathSentence`, `sentenceTick` each second, `sentenceFalls` at 0 (or `sentenceLifted`) | `SENTENCE_LANDS` after `sentenceFalls` |
| Mirror Step | `mirrorEnter` at the near reflection, `mirrorExit` at the far one; `reflectionHint` to show usable reflections | `MIRROR_OPENS` |
| Possession | `possessionDive`, `possessed` and `bodyLeft`, `possessionTick`, `possessionExit` | `GHOST_ARRIVES` |
| Ghost Host | `ghostHost`, `ghostServant` per ghost, `ghostTakenOver` per vex or phantom taken, `ghostFades` | `GHOSTS_RISEN` |
| Animate Objects | `animateObjects`, `hurlLaunch`, `hurledTrail` every tick, `hurledHit` | `OBJECTS_RISE` |
| Grave Shackles | `graveShackles`, `painShared` on each shared hit, `graveShacklesEnd` | `SHACKLES_LOCK` |
| Thorned Mausoleum | `mausoleum`, `mausoleumTick`, `mausoleumThorn`, `mausoleumEnd` | `MAUSOLEUM_SEALED` |
| Coffin of Ages | `coffinOfAges`, `coffinOpen`; on yourself the same, then `coffinRise` | `COFFIN_SHUT`, `COFFIN_OPENED` |
| Thorn Garden | `thornGarden`, `gardenTick`, `gardenThorn`, `roseHeal`, `thornGardenEnd` | `GARDEN_GROWN` |
| Curse by Connection | `curseByConnection`; `connectionMade` when a connection is touched (only its owner sees it) | `CURSE_LANDS` |
| Cursed Artifact | `relicPlaced`, `relicTick` (both only for the owner), `relicTriggered` | |
| Rite of Silence | `riteOfSilence`, `silenced`, `riteEnd` | `RITE_FALLS` |
| Bane Mist | `baneMist`, `baneMistTick` every few ticks, `baneMistOn`, `baneMistEnd` | |
| Ancient Blight | `blight` on each carrier with its stage, `blightJump` on each jump | |
| Abomination Form | `abominationForm`, `abominationTick`, `abominationCursed`, `calamity` on each victim, `abominationFormEnd` | `FORM_RISEN` |
| Passives | `fullMoonRises`, `keenSenses`, `whisperingCurse`, `baneAura`, `chainedAura`, `thornsBack` | |
| Every timed cast | `status(player, Status.X, ticks)` / `statusEnd`; the wheel pictures are the `WHEEL_...` constants | |

What is there:

| What | Where | Made by |
| --- | --- | --- |
| 29 model files (each step of a moving model is a file): the chain of blood, the iron chain, Restraint's chains, the shackle, the Werewolf, the grave hand (3 steps), the coffin (4 steps), the bandage wrap, the doll vessel, thorns, a rose, the ghost (2 steps), and 11 pictures: the seal (two halves), the silence sigil, the sentence mark, the curse mark, the frost ring, a blood pool, claw marks, the mad moon, the mirror ripple, the thorn wall | `models/model_fx/abomination/`, `textures/model_fx/abomination/` | `models.py` (+ `textures.py`) |
| 18 wheel pictures (32 px), 27 status pictures (16 px), the Pathway icon | `textures/gui/ability_menus/abomination/`, `textures/gui/abomination/`, `textures/gui/pathway_icons/abomination.png` (already in `PathwayHudIcon` under `"Abomination"`) | `icons.py` |
| 8 particle pictures: `fx/chain_link`, `fx/thorn`, `fx/bandage`, `fx/curse_0`, `fx/curse_1`, `fx/claw`, `fx/padlock`, `fx/stitch` | `textures/particle/fx/` | `particles.py` |
| 46 sounds (48 files): chain whip, chain bind, bleed, restraint, unchain, moonstruck, madness, frenzy, frenzy crash, pounce, claw, werewolf form, howl, raise dead, groan, grave chill, freeze, undying, sentence, sentence tick, sentence fall, mirror enter, mirror exit, possession, possession exit, ghost host, ghost wail, animate, hurl, shackles, pain link, curse, relic set, relic trigger, silence, bane mist, blight jump, abomination form, calamity, thorn burst, thorn hit, mausoleum, coffin close, coffin open, thorn garden, rose heal | `sounds/abomination/`, `sounds.json`, subtitles in `lang/en_us.json`, registered in `sounds/ModSounds.java` (`ABOMINATION_...`) | `sounds.py` |

## Open questions

1. **The Abilities page.** The wiki's Chained Pathway/Abilities page failed to load when this was written, so the abilities are my design from the theme, not from its list. If you paste its text I will line the names and effects up with it.
2. **The full moon.** The Pathway loses power or control under the full moon. The design softens it: bonuses at night under a full moon, but madness fills twice as fast and Restraint is weaker. Would you rather have it as a plain drawback, or as a pure bonus?
3. **Key 6 mixes Sequences 4 and 0.** The Chains wheel holds Puppet's strings (Sequence 4) and Chained's mausoleum, coffin and thorns (Sequence 0) because both are about binding. The alternative is to put all of Sequence 0 on key 7, which would make that wheel nine entries.
4. **Abomination Form's body.** The wiki's example vessel is a doll bound by thorny vines. The design keeps your own size and shows the doll as a model round you. Should it shrink you to a doll instead?
5. **Stealing.** Proposed: everything is stealable except Werewolf Form and Abomination Form, which refuse anyone whose own Pathway is not Abomination (as Giant Form does for Hand Of God).

## Checklist for the code

Follow the same steps as Hand Of God ([SPELL_KIT.md](SPELL_KIT.md), "The shape of an ability"):

1. `blasphemy_slate/beyonder_characteristics/abomination/Abomination.java`, `PATHWAY = "Abomination"`, with `abominationBloodChain` (key 1), `abominationMoonstruck` (key 2) and `abominationRendingPounce` (key 3), each with its sneak form; the wheels each in a class of their own beside it: `AbominationZombie.abominationZombie`, `AbominationWraith.abominationWraith`, `AbominationChains.abominationChains`, `AbominationCurses.abominationCurses`. Every number a constant at the top of its class.
2. Wheels `"abomination_zombie"` (key 4), `"abomination_wraith"` (key 5), `"abomination_chains"` (key 6, radius 80 for its five entries) and `"abomination_curses"` (key 7, radius 88 for its six), each with `.quickCastCooldown(false).quickCastCost(2f)` like Hand Of God.
3. One `import static` and one line in each `network/SKeyPressAbilityOneUsed` to `SevenUsed`.
4. Seven lines in `PathwayAbilities.ALL`: key 1 at Sequence 9, key 2 at 8, key 3 at 7, key 4 at 6, key 5 at 5, key 6 at 4, key 7 at 3.
5. `"Abomination"` in `SequenceCommands.PATHWAYS`.
6. The passives as lines in `SequencePassives`, and a `bodyTick` for what is not a potion effect (as `HandOfGod` does).
7. A row for it in the tables of [SEQUENCES.md](SEQUENCES.md) (the Pathway list at its top, the key table, the passives) and in the Pathway list in the README.
8. Done: `AbominationFx.java` and its icon in `PathwayHudIcon` under `"Abomination"`. Still to do: the Pathway color in `DarkAngel.pathwayColor` (`0x6C3082`).
9. Done: models, wheel pictures (32 px), status pictures (16 px), particles and sounds, generated by the scripts in `tools/model_gen/abomination/`, and `/abominationfx all` to play every display (see "Looks and sounds").
10. Set it with `/sequence pathway <player> "Abomination"`.
