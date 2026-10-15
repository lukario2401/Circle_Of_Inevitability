# Hand Of God: ability design

The Hand Of God Pathway is the Twilight Giant Pathway of Lord of the Mysteries, the Strength tarot card: Warrior (9), Pugilist (8), Weapon Master (7), Dawn Paladin (6), Guardian (5), Demon Hunter (4), Silver Knight (3), Glory (2), Hand of God (1), Twilight Giant (0). Source: the Twilight Giant Pathway and Twilight Giant Pathway/Abilities pages of the Lord of the Mysteries wiki.

Every key, every wheel entry and every passive is written (see "What is built"). The rest of this file is the design they were built from: what every ability does, the numbers they started from (the code's constants are the truth now), which parts of the [spell kit](SPELL_KIT.md) build each one, and the new reusable parts that came out of it.

Pathway name in code: `"Hand Of God"`.

## Theme

- A warrior Pathway: extreme strength, mastery of every weapon, resistance to supernatural harm, taking damage for others, an almost unbreakable defensive stance.
- **Dawn** is its light: it purifies, burns evil and undead, breaks illusions and hidden things, and forms silver weapons and armor. **Twilight** (from Sequence 2, as Glory) is Dawn grown old: its authorities are Twilight, Decay and Glory, its symbols Decay and the Passage of Time.
- Colors: pale gold and silver for Dawn and the Silver Knight, orange-red (tangerine) for Glory and Twilight, dark green eye-symbols for the Demon Hunter, grey-blue and black for the Giant's body. Pathway color: `0xFF7A1A`.
- From Sequence 6 the Beyonder is taller, and from Sequence 4 it can show a one-eyed Giant form that grows with every Sequence.
- Nothing here breaks blocks or lights real fire. Dawn light, Decay and Twilight only show and hurt; the ground is never changed.

## How it is laid out

It follows the Attendant Of Mysteries pattern: seven keys (G H J K L N M), every ability has a normal cast and a sneak cast. Three keys (4, 6, 7) are wheels like Apocalypse's, where every entry has its own Sequence, cost and cooldown; on a wheel key, sneak casts the last pick again (`AbilityWheel.quickCast`).

The abilities do not each get a Sequence of their own. Sequences 9, 8, 7 and 5 have two abilities, Sequences 6 and 4 have four, and Sequences 3 and 0 have three; Sequences 2 and 1 have one each but also strengthen what is already there (see "Grows with the Sequence"). Sequences 2, 1 and 0 are not left without abilities.

| Sequence | Abilities |
| --- | --- |
| Warrior (9) | Warrior's Charge, Divine Blood |
| Pugilist (8) | Gladiator's Flurry, Supernatural Resistance |
| Weapon Master (7) | Grandmaster's Edge, Riposte |
| Dawn Paladin (6) | Light of Dawn, Dawn Armor, Sword of Dawn, Hurricane of Light |
| Guardian (5) | Protection, Damage Transference |
| Demon Hunter (4) | Eye of Demon Hunting, Alchemy, Mind Concealment, Giant Form |
| Silver Knight (3) | Mercury Liquefaction, Silver Rapier, Light Concealment |
| Glory (2) | Twilight Sword |
| Hand of God (1) | Divine Right Hand |
| Twilight Giant (0) | Passage of Time, Decay, Divine Kingdom |

## Keys

| Key | Ability | Sequence | Normal cast | Sneak cast |
| --- | --- | --- | --- | --- |
| 1 | Warrior's Charge | Warrior (9) | Lower your shoulder and rush forward about 8 blocks. The first enemy you hit takes heavy damage and is thrown back; everything you brush past is shoved aside. No fall damage until you land. | Divine Blood: your blood surges. You heal about 40% of your health at once and get Strength II and Speed II for 10 seconds. |
| 2 | Gladiator's Flurry | Pugilist (8) | A burst of five very fast blows on the enemy in front of you. Every blow staggers it a little and the last one throws it back. It works with fists or any weapon, and it also lands on spirits and summoned things. | Supernatural Resistance: for 8 seconds blindness, nausea, freezing, roots and taunts end on you at once and cannot be put on you, and damage with no attacker (magic, explosions, abilities) is cut by 40%. |
| 3 | Grandmaster's Edge | Weapon Master (7) | For 15 seconds whatever you hold, sword, axe, a stick, a bucket, is used like a grandmaster's weapon: faster swings, longer reach, and every swing carves an arc that hits everything in front of you. The third swing in a row within 1.5 seconds is a finishing cut with heavy bonus damage. | Riposte: a parrying stance for 1.5 seconds. The next melee hit or projectile is turned aside, its source is staggered, and your next hit within 2 seconds is a counter that does heavy bonus damage. |
| 4 | Dawn | Dawn Paladin (6) | The Dawn wheel: Light of Dawn, Dawn Armor, Sword of Dawn, Hurricane of Light. Each has its own Sequence, cost and cooldown. | The last Dawn pick again, without the wheel. |
| 5 | Protection | Guardian (5) | Drive your sword into the ground. A dome of Dawn light rises around you and holds up to 15 seconds. Inside it your allies take much less damage, projectiles are stopped at the shell, and harmful effects cannot be put on them. You are braced and cannot leave it; every attack you make weakens the dome for a few seconds. | Damage Transference: for 20 seconds half of all damage dealt to your allies within 16 blocks lands on you instead, and you gain Resistance for each ally you cover. |
| 6 | Demon Hunting | Demon Hunter (4) | The Demon Hunting wheel: Eye of Demon Hunting, Alchemy, Mind Concealment, Giant Form. | The last pick again, without the wheel. |
| 7 | Silver and Twilight | Silver Knight (3) | The Twilight wheel, cast where you aim: Mercury Liquefaction (3), Silver Rapier (3), Light Concealment (3), Twilight Sword (2), Divine Right Hand (1), Passage of Time (0), Decay (0), Divine Kingdom (0). | The last pick again, without the wheel. |

## Key 4 wheel: Dawn

| Entry | Sequence | What it does |
| --- | --- | --- |
| Light of Dawn | Dawn Paladin (6) | Dawn light floods a 12 block radius around you for 8 seconds. Illusions break (blindness and taunts end on everyone in it who is not your enemy), concealed enemies are forced out of hiding and cannot hide inside it, evil and undead are burned and weakened, and your allies are cleansed and slowly healed. |
| Dawn Armor | Dawn Paladin (6) | A weightless silver armor of light covers you for 45 seconds: 40% less damage, no slowdown, no knockback, and a layer of absorption hearts. |
| Sword of Dawn | Dawn Paladin (6) | A two-handed greatsword of Dawn light forms in your hand for 40 seconds. Every hit purifies: it lowers the target's defense (more damage taken, stacking), and burns evil and undead extra. A full swing also throws a crescent of light a few blocks. |
| Hurricane of Light | Dawn Paladin (6) | Your strongest attack. Needs the Sword of Dawn (it forms if it is not out). Rays gather into a Dawn hurricane that rolls forward 20 blocks, shredding everything in its path and doing huge damage to evil and undead. It is hard to control: your allies in its path take a quarter of the damage. Long cooldown (the book says two minutes). |

## Key 6 wheel: Demon Hunting

| Entry | Sequence | What it does |
| --- | --- | --- |
| Eye of Demon Hunting | Demon Hunter (4) | Dark green symbols fill your eyes for 12 seconds. Evil, undead and corrupted things within 40 blocks glow through walls, and the enemy you look at shows its health, its weakness (what Alchemy would brew against it) and every effect on it. Finding the weakness makes it take 20% more damage for the duration. |
| Alchemy | Demon Hunter (4) | You brew a concoction in a moment for whatever you look at, and your weapon carries it for the next 10 hits or 20 seconds. The coating is picked from the target: Purification (evil, undead), Freeze (fire creatures), Lightning Strike (water creatures, metal armor), Burning (plants, spiders, ice creatures), Decay (high-health living things), Exorcism (spirits, summoned things and marionettes, which it ends). With nothing in sight it brews Lightning Strike. |
| Mind Concealment | Demon Hunter (4) | For 15 seconds your intentions cannot be read. Mobs forget you and cannot pick you out, and abilities that need to see or read you (Gaze, Divination, Eye of Demon Hunting) cannot choose you. You are not invisible, you can be hurt, and hitting something ends it. |
| Giant Form | Demon Hunter (4) | Your Mythical Creature Form: for 40 seconds you swell into a 4 block one-eyed Giant with blue-black skin. Strength III, Resistance II, longer reach, and every step shakes the ground and slows enemies nearby. Weaker mobs and players that see you get nausea and weakness. The longer you stay, the more your madness grows: at full madness the form ends and you are left weakened. |

## Key 7 wheel: Silver and Twilight

| Entry | Sequence | What it does |
| --- | --- | --- |
| Mercury Liquefaction | Silver Knight (3) | You melt into silver liquid for 6 seconds: hidden, untouchable, fast along the ground, wrapping the enemies you slide past (held for 2 seconds). You re-form where you aim, at most 20 blocks away; a killing blow while liquid re-forms you up to 12 blocks off instead. Aimed at an ally you become their Silver Armor (damage reduction and absorption); aimed at an enemy you become a restraining coat that holds it and chokes it. |
| Silver Rapier | Silver Knight (3) | Silver-white beams condense out of thin air around the enemy you look at, with no warning, and stab it from every side, even from inside. Three rapiers; they ignore walls and cannot be dodged. |
| Light Concealment | Silver Knight (3) | Step into the nearest light, a torch, lava, a lamp, the sun, and come out of the light source you aim at, hidden while you travel. By day any spot under open sky counts as light. Used while Mind Concealment is up, you stay fully hidden for 6 seconds afterwards. |
| Twilight Sword | Glory (2) | A broadsword of orange-red Glory light condenses in the void and bursts out where the enemy stands, ripping apart everything within 5 blocks. Heavy damage and Decay, and it goes through Protection, Substitutes and death wards. |
| Divine Right Hand | Hand of God (1) | Your right hand becomes pure Twilight. Aimed at an enemy (not a boss), a hand of dusk seizes it, holds it and squeezes for 3 seconds, then flings it. Aimed at nothing, the hand slams down where you aim: everything within 6 blocks is pressed into the ground, held and decayed. |
| Passage of Time | Twilight Giant (0) | A wave of Twilight spreads 24 blocks. Every enemy in it is frozen in the Passage of Time for 8 seconds: thoughts and actions stop. Damage they take meanwhile is written down and lands again, doubled, when time resumes. |
| Decay | Twilight Giant (0) | Twilight shines over a 12 block area for 12 seconds. Every enemy inside rots: no healing, wither, weakness, armor and weapons lose durability every second, and the longer they stay the weaker their maximum health becomes. |
| Divine Kingdom | Twilight Giant (0) | The sky turns dusk-orange and a frozen kingdom of Twilight covers 40 blocks around you for 60 seconds. Enemies inside are weak and slow, cannot heal, and their abilities are weakened. Your allies are acknowledged: Strength, Resistance and Regeneration. Cast it again to end it early. |

## Grows with the Sequence

These do not get their own key or entry. The same ability is stronger from the Sequence on.

| Ability | From | What changes |
| --- | --- | --- |
| Dawn Armor becomes Silver Armor | Silver Knight (3) | No timer: it stays until you cast it again. Absorption doubles. |
| Sword of Dawn | Silver Knight (3) | No timer and no upkeep: it stays until you cast it again or put it away. |
| Giant Form | Silver Knight (3), Glory (2) | 1.5 times your size at Sequence 3 (madness grows slower), 1.75 times from Sequence 2 with no madness; the reach grows with it. (Decided: 1.2 / 1.5 / 1.75, not the book's dozens of blocks, so the Giant still fits indoors.) |
| Protection becomes Cage | Glory (2) | The dome closes into a full sphere that also holds the enemies in it. Nothing crosses the shell but your allies. It holds as long as you stay within 10 blocks of it. |
| Hurricane of Light | Glory (2) | Twice as wide and long, and everything in its path is decayed. |
| Light Concealment | Glory (2) | You can end it by Withering: you age into rotten liquid, vanish into the Spirit World and re-form at any spot you aim at in view, hidden on the way. |
| Protection | Hand of God (1) | Teleports, Concealment and gliding cannot cross the shell; an extra layer of orange light outside it. Sneak while inside it and drive the sword in again to mend it. |
| Eye of Demon Hunting | Silver Knight (3) | Also shows who is lying and who has a Beyonder Pathway, and its reach is 80 blocks. |

## Passives

| Passive | From Sequence |
| --- | --- |
| Strength I (Divine Blood) | Warrior (9) |
| Supernatural Resistance: nausea and blindness do not take, and damage with no attacker (magic, explosions, abilities) is 25% less | Pugilist (8) |
| Weapon Mastery: faster attacks and a little more reach with anything in your main hand | Weapon Master (7) |
| Giant's Physique: you are taller, and have two extra hearts | Dawn Paladin (6) |
| Resistance I and half knockback | Guardian (5) |
| Spiritual Intuition: a soft chime when something starts hunting you from out of sight; Tracking: enemies you hit leave footprints only you see for 20 seconds | Demon Hunter (4) |
| Dawn Aura: undead and evil within 6 blocks of you are slowly burned | Silver Knight (3) |
| Twilight Resistance: you cannot be decayed, withered or stopped from healing | Glory (2) |
| Divine Touch: your bare-handed blows decay: extra damage, and the target cannot heal for 3 seconds | Hand of God (1) |
| Twilight Aura: enemies within 16 blocks are slowed and weakened and cannot heal | Twilight Giant (0) |

## Starting numbers

Spirituality is what each Sequence can pay: 9 holds 20, 8 holds 400, 7 holds 1980, 6 holds 6400, 5 holds 15500, 4 holds 32400, 3 holds 59780, 2 and below hold 78400. Costs follow the other Pathways (250 at Sequence 9, climbing to 40000 to 50000 at Sequence 0). Ticks: 20 are one second. Every number is a start and becomes a constant at the top of the class.

| Ability | Cost | Cooldown | Lasts | Reach / radius | Damage |
| --- | --- | --- | --- | --- | --- |
| Warrior's Charge | 250 | | | 8 blocks | 30 |
| Divine Blood | 250 | | 200 | | heals 40% |
| Gladiator's Flurry | 300 | | | 3 blocks | 5 x 8 |
| Supernatural Resistance | 300 | | 160 | | |
| Grandmaster's Edge | 700 | | 300 | | finisher +24 |
| Riposte | 700 | | 30 (stance) | | counter +30 |
| Light of Dawn | 1500 | 200 | 160 | 12 | 6 per second to evil |
| Dawn Armor | 1500 | 300 | 900 | | |
| Sword of Dawn | 2000 | 200 | 800 | crescent 6 | |
| Hurricane of Light | 4500 | 2400 | | 20 long, 4 wide | 60, 25% to allies |
| Protection | 3000 | 600 | 300 | 6 | |
| Damage Transference | 2500 | 600 | 400 | 16 | 50% shared |
| Eye of Demon Hunting | 3000 | 200 | 240 | 40 | |
| Alchemy | 3500 | 300 | 400 (10 hits) | | per coat |
| Mind Concealment | 4000 | 600 | 300 | | |
| Giant Form | 8000 | 1200 | 800 | 1.2 / 1.5 / 1.75 times | |
| Mercury Liquefaction | 10000 | 400 | 120 | 20 | |
| Silver Rapier | 8000 | 200 | | 32 | 3 x 20 |
| Light Concealment | 6000 | 300 | 20 | 32 | |
| Twilight Sword | 20000 | 600 | | 5 | 90 |
| Divine Right Hand | 30000 | 1200 | 60 | 6 | 80 |
| Passage of Time | 40000 | 2400 | 160 | 24 | stored, doubled |
| Decay | 35000 | 1800 | 240 | 12 | per second |
| Divine Kingdom | 50000 | 3600 | 1200 | 40 | |

## Planned: what each is built from

Most of it is parts that existed already; the new ones are listed under "New reusable parts" below, all written now.

| Ability | Built from |
| --- | --- |
| Warrior's Charge | `Reel.in` with a point 8 blocks ahead, `Spells.enemiesOnLine`, `Spells.strike`, `Spells.push`, `FallGuard` |
| Divine Blood | `Beyonder` healing, `Spells.keepEffect` |
| Gladiator's Flurry | `Later.run` five times, `Spells.strike`, `Root.apply` (short), `SpellFx` rings |
| Supernatural Resistance | `Spells.cleanse` every tick (`Later` loop), plus the damage cut: `SupernaturalResistance` |
| Grandmaster's Edge | `EmpoweredHit` for the finisher, a short status for the swing arc; `Spells.enemiesInCone` for the sweep, from the real-swing hook in `HandOfGod` (`landed`) |
| Riposte | `Substitute.arm` (its callback staggers the attacker with `Root.apply`) plus `EmpoweredHit.arm` for the counter |
| Light of Dawn | `Zone.around` (reveals concealed enemies with `Concealment.reveal`, cleanses allies with `Spells.cleanse`, burns evil), `SpellFx.shockRing` |
| Dawn Armor | `ConjuredArmor` (planned as `ArmorOfLight`) plus `ParticleShapes.modelOnTurning` for the look |
| Sword of Dawn | `ConjuredWeapon` (planned as `DawnBlade`), `ParticleShapes.modelOnTurning` beside the body, `Vulnerable.add` on each hit |
| Hurricane of Light | `HandOfGodFx.hurricane` and a sweep along its line tick by tick (`releaseHurricane`), `Spells.strike`, `Spells.isEvil`, `Decay` from Glory |
| Protection | `Dome` (stops projectiles, keeps enemies out or in, shelters allies, holds you in) |
| Damage Transference | `DamageLink.redirect` from each ally to you (planned as a new `Aegis`, not needed) |
| Eye of Demon Hunting | `Spells.livingAround` plus `SpellFx.overHead`, `Vulnerable.add`, `Marks` |
| Alchemy | `WeaponCoat` (a charged `EmpoweredHit`), then `Freeze.apply`, `Decay.apply`, fire on the target, `Marionette.release` |
| Mind Concealment | `Unnoticed` (a `Concealment` that only hides you from minds, not eyes) |
| Giant Form | `GiantForm`, `Spells.livingAround` for the nausea, a stride check every tick for the stomp slow |
| Mercury Liquefaction | `Concealment.hide`, `Glide`, `Substitute`, `Root.apply` on the wrapped, `DamageLink.redirect` (for the Silver Armor variant) |
| Silver Rapier | `Shot` fired from points around the target with `.windup(0)` and no trail, or `SpellFx.homing` |
| Light Concealment | `Concealment.hide`, `Spells.blinkSpot`, `Spells.lightNear` / `lightAimed` / `inDaylight` |
| Twilight Sword | `HandOfGodFx.twilightSword` (the falling sword), `Spells.enemiesAround`, `Spells.strikeThrough`, `Decay.apply` |
| Divine Right Hand | `Reel`, `Root.apply`, `Burial.bury` for the slam, `ModelFx` for the hand |
| Passage of Time | `Root.apply` (EVERYTHING), `StoredDamage.start`, `Freeze.apply` for the look |
| Decay | `Zone.at` with `Decay.apply` and `NoHeal` inside it |
| Divine Kingdom | `Zone.around` for the whole kingdom, `Spells.keepEffect`, `Spells.alliesAround` for the acknowledged |
| Passives | `SequencePassives`: `effect(...)` and `custom(...)` lines, plus attribute changes for Giant's Physique (`Physique`) |

## New reusable parts worth writing first

These come from this Pathway and would be useful to the others too. Each is one small class in `status/` (or a method in `Spells`) that looks after itself, like the rest of the kit. Each of them is described by what it should do so that it can be coded straight from here, and each should get a section in [SPELL_KIT.md](SPELL_KIT.md) when it is written.

All written; each has a section in [SPELL_KIT.md](SPELL_KIT.md). With keys 1, 2, 3 and 5: `Dome` (`combat/Dome`, with `keepsEnemiesOut`, `keepsEnemiesIn`, `stopsProjectiles`, `shields`, `wardsEffects`, `holdsOwner`, `outsiders`, `keepWhile`, `onBlock`, `weaken`, `renew`), `SupernaturalResistance` (`status/`), `Physique` (`blasphemy_slate/beyonder/`), `Spells.isEvil` (with `Spells.addEvilCheck`) and `Spells.isFriend`. `Aegis` turned out not to be needed: Damage Transference is `DamageLink.redirect` from each ally to the guardian. With the wheels, all in `status/` unless said: `Decay`, `WeaponCoat`, `Unnoticed`, `GiantForm`, `ConjuredArmor` (planned as `ArmorOfLight`: the name says what it is for any Pathway), `ConjuredWeapon` (planned as `DawnBlade`), and in `combat/Spells`: `lightNear`, `lightAimed`, `inDaylight`, `canBeChosen` (what `Unnoticed` hides from) and `strikeThrough` (a hit that no `Dome` shield, `Substitute` or `DeathWard` stops; those three let it through).

| Part | Where | What it does | Used by |
| --- | --- | --- | --- |
| `Dome` | `combat/` | A shell around a spot: `Dome.at(center, radius).lasts(t).stopsProjectiles().keepsEnemiesOut()` / `.keepsEnemiesIn()`, `.allyShield(0.7f)`, `.onHit(...)`, `.weakenBy(ticks)`. A `Zone` underneath for who is inside; the shell itself pushes and removes projectiles. | Protection, Cage, Divine Kingdom walls |
| `Aegis` | `status/` | `Aegis.cover(guardian, allies, share, ticks)`: part of what each ally loses is dealt to the guardian instead (after the guardian's armor), once and never passed on again. The mirror image of `DamageLink.redirect`. | Damage Transference |
| `GiantForm` | `status/` | Scale, reach and step height through attribute modifiers with a fixed id, a madness meter (`madness()`, hooks `onMad`, `onBroken`), ended and cleaned up on death, logout and dimension change (also at the next login if the server stopped). | Giant Form, the Giant's Physique passive (smaller scale) |
| `Unnoticed` | `status/` | Like `Concealment` but only minds are fooled: mobs forget and cannot target you, `Spells.isEnemy` passes over it, abilities that read you fail; the body is drawn and can be hurt. Ends when you attack. | Mind Concealment, Light Concealment (afterwards) |
| `WeaponCoat` | `status/` | `WeaponCoat.arm(player, name, charges, ticks, (attacker, target, amount, level) -> ...)`: like `EmpoweredHit` but it lasts for several swings. | Alchemy, Sword of Dawn's purification |
| `Decay` | `status/` | `Decay.apply(target, ticks, depth)`: `NoHeal`, wither, weakness, armor and held weapon lose durability every second, maximum health shrinks with `depth` (a removable attribute modifier). Stays on for the given time. | Decay, Twilight Sword, Divine Right Hand, Hurricane (from Sequence 2), Divine Touch |
| `Spells.isEvil` | `combat/` | `true` for undead, monsters and anything marked evil by another Pathway, so "burns evil" is one call. | Light of Dawn, Sword of Dawn, Hurricane, Eye |
| `Spells.lightNear`, `lightAimed` | `combat/` | The block positions that give light within a range, and the one nearest to what the crosshair is on. `null` if none. | Light Concealment |
| `ConjuredWeapon` (was `DawnBlade`) | `status/` | A weapon that is not an item: while it is drawn your normal hits are its hits, with more damage and reach and your own `onHit`. Optional upkeep, and a permanent mode. The model is the ability's. | Sword of Dawn, Hurricane of Light |
| `ConjuredArmor` (was `ArmorOfLight`) | `status/` | Armor that is not an item: less damage, a layer of absorption that can regrow, no knockback or slowness; timed or permanent. The look is the ability's. | Dawn Armor, Silver Armor, Mercury Liquefaction on an ally |
| `SupernaturalResistance` | `status/` | A cut to damage that has no attacker, and statuses that cannot be put on while it lasts. | Supernatural Resistance |
| `Physique` | `blasphemy_slate/beyonder/` | Attribute modifiers for a Pathway by Sequence (scale, health, reach, step height), idempotent so it can run from `SequencePassives` once a second. | Giant's Physique, Weapon Mastery |

Two ideas are cheap and give the most: `Decay` and `Dome`. Four of the abilities use each.

## Decisions (made)

1. **The Sword of Dawn is not an item.** It is a `ConjuredWeapon` and a model held beside the body, so nothing has to be given, taken or dropped.
2. **The Giant is 1.2 / 1.5 / 1.75 times your size** at Sequences 4 / 3 / 2 and up (`GIANT_SCALE`, `GIANT_KNIGHT_SCALE`, `GIANT_GLORY_SCALE`), and its model is as tall as you really are.
3. **The Hurricane of Light hurts allies in its path for a quarter** (`HURRICANE_ALLY_SHARE`; 0 turns friendly fire off).
4. **Alchemy picks the coating from the target** (`coatFor`), not from a sub-wheel.
5. **Stealing.** Everything is stealable (a stolen wheel casts its last or a random entry), except Protection and Damage Transference (key 5 is `OWN_PATHWAY_ONLY`) and Giant Form, which refuses anyone whose own Pathway is not Hand Of God.

## Checklist for the code

Follow the same steps as Beauty Goddess ([SPELL_KIT.md](SPELL_KIT.md), "The shape of an ability"):

1. Done: `blasphemy_slate/beyonder_characteristics/HandOfGod.java`, `PATHWAY = "Hand Of God"`, with `handOfGodWarriorsCharge`, `handOfGodGladiatorsFlurry`, `handOfGodGrandmastersEdge` and `handOfGodProtection`; the wheels each in a class of their own beside it: `HandOfGodDawn.handOfGodDawn`, `HandOfGodDemonHunting.handOfGodDemonHunting`, `HandOfGodTwilight.handOfGodTwilight`. Every number is a constant at the top of its class.
2. Done: wheels `"hand_of_god_dawn"` (key 4), `"hand_of_god_demon_hunting"` (key 6) and `"hand_of_god_twilight"` (key 7, radius 92 for its eight entries), each with `.quickCastCooldown(false).quickCastCost(2f)` like Beauty Goddess.
3. Done: one `import static` and one line in each `network/SKeyPressAbilityOneUsed` to `SevenUsed`.
4. Done: seven lines in `PathwayAbilities.ALL`: key 1 at Sequence 9, key 2 at 8, key 3 at 7, key 4 at 6, key 5 at 5, key 6 at 4, key 7 at 3.
5. Done: `"Hand Of God"` in `SequenceCommands.PATHWAYS`.
6. Done: the passives as lines in `SequencePassives`; a misspelt Pathway name stops the game on purpose.
7. Done: a row for it in the tables of [SEQUENCES.md](SEQUENCES.md) and in the Pathway list at its top, and the list in the README.
8. Done already: `HandOfGodFx.java` and the icon in `PathwayHudIcon` under `"Hand Of God"`. Done too: the Pathway color in `DarkAngel.pathwayColor`.
9. Done already: models, wheel pictures (32 px), status pictures (16 px), particles and sounds, generated by the scripts in `tools/model_gen/hand_of_god/`, and `/handfx all` to play every display.
10. Set it with `/sequence pathway <player> "Hand Of God"`.

## What is built

Every key and every passive. Keys 1, 2, 3 and 5 and the passives are in `blasphemy_slate/beyonder_characteristics/HandOfGod.java`; the wheels are in `HandOfGodDawn.java` (key 4), `HandOfGodDemonHunting.java` (key 6) and `HandOfGodTwilight.java` (key 7) beside it, which `HandOfGod` ticks and clears after a relog or a death. Every number is a constant at the top of its class. One line each in `PathwayAbilities.ALL` (Protection is not stealable: it is the Guardian's own sword), a row in [SEQUENCES.md](SEQUENCES.md), and the passives in `SequencePassives` and `HandOfGod`. Set the Pathway with `/sequence pathway <player> "Hand Of God"`.

How it works where the tables above do not say:
- Warrior's Charge rushes at the enemy you look at if one is within 9 blocks, else 8 blocks straight ahead (`combat/Reel`). The first enemy in front of you is hit and thrown, and the rush stops there; enemies beside your path are shoved aside. It cannot be started while you are held in place.
- Gladiator's Flurry needs an enemy within 4 blocks of your crosshair. Each of the first four blows holds it in place for a moment; if it gets more than 6 blocks away the rest miss.
- Supernatural Resistance and the Pugilist's passive are the new `status/SupernaturalResistance`. "Supernatural" damage is magic (which is also what abilities deal when they name no attacker), wither, explosions, lightning, freezing, sonic booms, thorns and dragon's breath. While the cast lasts it also shakes off roots, freezing, `Blind`, taunts and darkness every tick.
- Grandmaster's Edge: swings 50% faster and 1.5 blocks more reach (`beyonder/Physique`). Every swing that lands with at least 70% of the attack bar also cuts each other enemy within 4 blocks in front of you (60 degrees each side) for 60% of its damage, and counts toward the combo: the third in a row, each within 1.5 seconds of the last, is the finishing cut. Swings that miss cannot be seen by the server, so they neither cut nor count.
- Riposte uses `status/Substitute` (the next hit, arrow or explosion that would land does not) and `status/EmpoweredHit` (the counter). The attacker cannot move, jump or strike for 1.5 seconds; an arrow is destroyed.
- Protection is the new `combat/Dome`, raised at once round your feet. Enemies inside are put out and cannot come in (every tick, so a teleport or a pearl counts too); their projectiles end at the shell; you cannot leave it; allies inside take 60% less damage and no harmful potion effect. Each of your melee swings weakens it for 3 seconds: projectiles go through and the shield is halved. Press the key again to lower it early. Cooldown 30 seconds.
- The Cage (Glory, 2): the same as a whole sphere 7 blocks round, a little sunk into the ground. Nothing crosses it but your allies: enemies inside stay in, enemies outside stay out. You can move; it falls when you are more than 10 blocks from its shell, or after 30 seconds.
- Hand of God (1): the dome or cage also keeps out concealed enemies (`Spells.isFriend` decides, not `isEnemy`), an orange shell shows outside it, and sneak + L while you stand inside it costs 1500 and makes it whole and new (its full time again). Damage Transference is cast with sneak + L from outside it.
- Damage Transference is `DamageLink.redirect` from every ally within 16 blocks to you (half of what they lose, after their armor), with Resistance I, II or III for one, two or three and more allies. An ally who goes more than 24 blocks away is let go. Cooldown 30 seconds. It refuses (costing nothing) with no ally near.
- Physique and Weapon Mastery change attributes (scale, max health, attack speed, reach, knockback resistance) with fixed ids; they are checked once a second for every player and taken off again when the Pathway or Sequence no longer has them, so nothing is left behind.
- Tracking prints are particles sent only to the hunter (`ParticleShapes.spawnFor`); the Intuition chime is a sound only they hear.

The wheels (sneak on a wheel key casts the last pick again with no cooldown, for double the cost):
- Light of Dawn stands where you cast it (not round you as you walk). Once a second: enemies who are concealed or unnoticed are forced out (they can hide again, and are forced out again a second later); evil and undead take 6 and are weakened; you and your allies lose blindness, `Blind` and taunts, are cleansed and heal 1.
- Dawn Armor is `ConjuredArmor`: 40% less damage, 4 hearts of absorption, no knockback, slowness does not take, for 45 seconds; cast again for fresh plates. Silver Knight (3): the Silver Armor stays until you cast it again (free, it takes it off), with 8 hearts of absorption that regrow a point every 2 seconds without a hit.
- Sword of Dawn is `ConjuredWeapon`: +6 per hit (less for a spam click) and +1 reach, 20 spirituality a second, 40 seconds. Every hit adds 5% to the damage the target takes (30% at most, for 10 seconds after the last), evil and undead take +8; a swing with 90% of the attack bar also cuts each enemy 6 blocks ahead for 60%. Silver Knight (3): no timer and no upkeep, cast again to put it away (free). Misses, as for Grandmaster's Edge, are not seen.
- Hurricane of Light draws the Sword of Dawn first if it is not out (at no further cost). It rolls 20 blocks in 24 ticks, 4 wide; each enemy it reaches takes 60 (90 if evil) and is thrown along; each ally a quarter of 60. Glory: 40 blocks and 8 wide, and everything it reaches decays. It does not stop at walls.
- Eye of Demon Hunting: every second the evil within 40 blocks glow (the vanilla glowing effect, so everyone sees the outline) and get a green ring. Each enemy you look at while it is open is read once into chat (health, its weakness, its effects; from Silver Knight a player's Pathway and Sequence too) and takes 20% more damage until the eye closes. The book's "who is lying" has nothing in the game to read, so it is left out.
- Alchemy reads the target you look at (24 blocks): Exorcism for marionettes, summons and vexes; Freeze for fire-immune creatures; Burning for arthropods and freeze-immune creatures; Lightning Strike for water creatures and anyone in iron, chain, gold or netherite armor; Purification for the evil; Decay for anything with 40 health or more; Lightning Strike otherwise or when nothing is aimed at. The coating is `WeaponCoat`: 10 hits or 20 seconds. Each adds to the hit (see the constants) and Freeze freezes for 2 seconds, Burning sets the target alight (never the ground), Decay rots, Exorcism cuts a marionette's strings (a summon vanishes).
- Mind Concealment is `Unnoticed` for 15 seconds. Abilities that choose a target through `Spells` aiming pass over you; abilities of other Pathways that find their target some other way may not.
- Giant Form is `GiantForm`: 1.2 / 1.5 / 1.75 times your size (Sequence 4 / 3 / 2 and up), 1 / 1.5 / 2 more reach, half a block (a block from 3) more step, Strength III and Resistance II, 40 seconds. A stride every 2.2 blocks (times the size) slows enemies on the ground within 4 (times the size). Weaker ones (less maximum health than you) that see you get nausea and weakness, at most every 10 seconds. Madness fills in 30 seconds at Sequence 4 and 50 at 3 (so at 3 the 40 seconds run out first), none from Glory; full madness breaks the form and leaves you with Weakness II, slowness and nausea. Cast again to end it (free).
- Mercury Liquefaction (120 ticks at most, cast again to re-form early, free): you are concealed. Aimed at an ally, you ride along with them and they wear Silver Armor (40% less damage, 4 hearts of absorption); aimed at an enemy, you ride along with it and it is held and takes 5 a second; otherwise you slide to where you aim (`Reel`, 0.6 blocks a tick) and every enemy within 1.5 blocks of you is held 2 seconds. While concealed nothing can hurt you, so the book's "a killing blow re-forms you" has nothing to catch.
- Silver Rapier needs an enemy in sight (32 blocks); three hits of 20.
- Light Concealment enters by a light source within 6 blocks of your eyes (at least light 10: torches, lanterns, glowstone, lava...) or, by day under the open sky, the sunlight, and comes out beside the light source nearest your crosshair (within 4 of it, 32 away) or at a sunlit spot you aim at. It takes a second, hidden. With Mind Concealment still up you stay hidden 6 seconds more. Glory: with no light to go in or out by, you wither away and re-form at any spot you aim at.
- Twilight Sword lands on the enemy you aim at, or on the ground you aim at (48 blocks): 90 to each enemy within 5, through `Dome` shields, `Substitute` and `DeathWard` (`Spells.strikeThrough`), and decay.
- Divine Right Hand seizes an enemy you aim at (32 blocks) unless it is a boss (dragon, wither, warden, elder guardian, or more than 150 health): held 3 seconds, 15 a second, then flung for 35 more and decayed. Aimed at the ground or a boss, it slams down there: 80 to each enemy within 6, buried (`Burial`; held if it is too big to bury) and decayed.
- Passage of Time stands round where you cast it: at the moment the clock stops, every enemy within 24 is held completely for 8 seconds, and whatever it took meanwhile lands on it again twice over (`StoredDamage`).
- Decay is a `Zone` of 12 blocks where you aim (32 away), 12 seconds: each enemy in it rots, its health 10% lower at first and 5% more for each second it stays (50% at most).
- Divine Kingdom stands round where you cast it, 40 blocks, 60 seconds: enemies in it are slowed and weakened, cannot heal and deal 70% of their damage; you and your allies have Strength, Resistance and Regeneration. Cast again to end it (free).

## Looks, sounds and pictures (ready)

Everything an ability needs to look and sound right is in `HandOfGodFx.java` (next to the other pathway classes). It only shows things: the ability does the damage, holding, hiding and moving, at the ticks its constants name. `/handfx all` plays every display in game, `/handfx <name>` one of them (TAB lists the names; there is also `silver_armor`, `hurricane_glory`, `cage`, `withering`, `hand_slam`, `giant_twilight` and `passives`).

| Ability | Normal cast | Sneak cast |
| --- | --- | --- |
| 1 Warrior's Charge | `chargeStart(sl, player)`, `chargeTrail(sl, player)` every tick of the rush, `chargeImpact(sl, player, target)` | Divine Blood: `divineBlood(sl, player, ticks, tag)`, `divineBloodEnd` |
| 2 Gladiator's Flurry | `flurryBlow(sl, player, target, index)` for the blows 0..4 (4 is the big one) | Supernatural Resistance: `resistance(sl, player, ticks, tag)`, `resistanceShrug(sl, player)` when something is shaken off, `resistanceEnd` |
| 3 Grandmaster's Edge | `grandmastersEdge(sl, player, ticks)`, `edgeTick` every few ticks, `edgeSwing(sl, player, index)`, `edgeFinisher(sl, player, target)` | Riposte: `riposte(sl, player)`, `riposteParry(sl, player, attacker)`, `riposteCounter(sl, player, target)` |
| 4 Dawn (wheel) | Light of Dawn: `lightOfDawn(sl, center, radius, ticks, tag)` (bright at `LIGHT_RISEN`), `lightOfDawnPulse` every second, `lightOfDawnBurn`, `lightOfDawnReveal`, `lightOfDawnHeal`, `lightOfDawnEnd`. Dawn Armor: `dawnArmor(sl, player, ticks, tag, silver)` (`silver` = the Silver Armor from Sequence 3), `armorHit`, `dawnArmorEnd`. Sword of Dawn: `swordOfDawn(sl, player, ticks, tag)`, `dawnSlash(sl, player, crescent)`, `purify(sl, target)`, `swordOfDawnEnd`. Hurricane of Light: `hurricaneGather(sl, player)`, then at `HURRICANE_GATHERS` `hurricane(sl, ground, yaw, length, ticks, glory)` (`glory` = the Sequence 2 form), `hurricaneHit`. Icons `WHEEL_LIGHT_OF_DAWN`, `WHEEL_DAWN_ARMOR`, `WHEEL_SWORD_OF_DAWN`, `WHEEL_HURRICANE` | the same, without the wheel |
| 5 Protection | `protection(sl, player, center, radius, ticks, tag)` (whole at `PROTECTION_RAISED`), `domeHit(sl, at)`, `domeWeaken(sl, center, radius)`, `protectionEnd`. The Glory Cage: `cage(sl, center, radius, ticks, tag)` (closed at `CAGE_CLOSED`), `cageEnd` | Damage Transference: `transference(sl, player, allies, ticks, tag)`, `transferenceTaken(sl, ally, guardian)` for each wound passed on, `transferenceEnd` |
| 6 Demon Hunting (wheel) | Eye: `eye(sl, player, ticks, tag)` (open at `EYE_OPENS`), `eyeMark(sl, target, ticks, tag)`, `eyeEvil(sl, evil, ticks)`, `eyeEnd`. Alchemy: `alchemy(sl, player, coat)` (on the weapon at `VIAL_POURED`), `coatTick`, `coatHit(sl, target, coat)`; the coatings are `HandOfGodFx.Coat`. Mind Concealment: `mindConcealment(sl, player, ticks, tag)`, `mindConcealmentEnd`. Giant Form: `giantForm(sl, player, height, ticks, tag)` (full size at `GIANT_RISEN`), `giantStep`, `giantTerror(sl, onlooker)`, `giantMadness(sl, player, madness, height)`, `giantFormEnd`. Icons `WHEEL_EYE`, `WHEEL_ALCHEMY`, `WHEEL_MIND_CONCEALMENT`, `WHEEL_GIANT_FORM` | the same, without the wheel |
| 7 Silver and Twilight (wheel) | Mercury: `mercury(sl, player, ticks, tag)`, `mercuryTick`, `mercuryWrap(sl, enemy, ticks)`, `mercuryReform`, `mercuryArmor(sl, ally, ticks, tag)`. Silver Rapier: `silverRapiers(sl, target, count)` (they strike at `RAPIER_STRIKES`). Light Concealment: `lightEnter(sl, player, light)`, `lightExit(sl, player, at)`, Glory's `withering(sl, player)`. Twilight Sword: `twilightSword(sl, ground, radius)` (lands at `TWILIGHT_SWORD_HITS`). Divine Right Hand: `divineHand(sl, player, target, ticks, tag)` (grips at `HAND_GRIPS`), `divineHandFling`, `divineHandSlam(sl, player, ground, radius)` (lands at `HAND_SLAMS`). Passage of Time: `passageOfTime(sl, center, radius, ticks, tag)` (still at `TIME_FROZEN`), `timeFrozen`, `timeResume`. Decay: `decay(sl, center, radius, ticks, tag)`, `decayTick`, `decayOn`, `decayEnd`. Divine Kingdom: `divineKingdom(sl, player, radius, ticks, tag)` (down at `KINGDOM_RISEN`), `kingdomTick`, `kingdomAcknowledged`, `kingdomEnd`. Icons `WHEEL_MERCURY`, `WHEEL_RAPIER`, `WHEEL_LIGHT_CONCEALMENT`, `WHEEL_TWILIGHT_SWORD`, `WHEEL_DIVINE_HAND`, `WHEEL_PASSAGE_OF_TIME`, `WHEEL_DECAY`, `WHEEL_DIVINE_KINGDOM` | the same, without the wheel |
| Passives | `dawnAura(sl, player, radius)` once a second (Silver Knight), `footprint(sl, at, yaw)` (Tracking), `divineTouch(sl, target)` (Hand of God) | |

Every timed cast can show its picture and clock above the hotbar: `HandOfGodFx.status(player, Status.X, ticks)`, and `statusEnd` to take it away. The Pathway's icon beside the hotbar is registered in `PathwayHudIcon` under `"Hand Of God"`.

Reusable for any Pathway: `HandOfGodFx.dome(...)` and `sphere(...)` (a shell of light tiles in any color, the look for the planned `Dome`), and `HandOfGodFx.seal(...)`.

Two things to know when the abilities are written:
- The Sword of Dawn is shown held at the ready beside the body and turning with it; a model cannot follow the arm. Each swing is shown with `dawnSlash`.
- Giant Form shows the Giant's body round the caster, turning with them; the caster's real size (the scale attribute of the planned `GiantForm`) is the ability's job. Its limbs do not move with the caster's.

What is in the repository, all of it generated by the scripts in `tools/model_gen/hand_of_god/` (see "Hand Of God: ready-made displays" in [MODEL_FX.md](MODEL_FX.md)):

| What | Where | Made by |
| --- | --- | --- |
| 29 model files (each step of a moving model is a file): Sword of Dawn, Twilight Sword, Silver Rapier, Dawn Armor and helm, Silver Armor and helm, the Divine Right Hand (5 steps, open to a fist), the one-eyed Giant, the hunter's vial, the dawn seal, the dome tile, the hurricane funnel, the Eye of Demon Hunting, the mercury pool, the gate of light, the time dial and its hands, the decay sigil, the setting sun, the kingdom seal, the aegis shield, the flash of steel | `models/model_fx/hand_of_god/`, `textures/model_fx/hand_of_god/` | `models.py` (+ `textures.py`) |
| 16 wheel pictures (32 px), 22 status pictures (16 px), the Pathway icon | `textures/gui/ability_menus/hand_of_god/`, `textures/gui/hand_of_god/`, `textures/gui/pathway_icons/hand_of_god.png` | `icons.py` |
| 8 particle pictures: `fx/ray`, `fx/rune_0`, `fx/rune_1`, `fx/rune_2`, `fx/ash`, `fx/hourglass`, `fx/footprint`, `fx/feather_light` | `textures/particle/fx/` | `particles.py` |
| 31 sounds (32 files): charge, impact, divine blood, flurry, resist, edge swing, finisher, parry, dawn light, dawn armor, dawn sword, hurricane, protection, dome hit, transference, eye, alchemy, coat hit, conceal, giant form, giant step, mercury, rapier, light step, twilight sword, divine hand, time stop, time resume, decay, kingdom, cage | `sounds/hand_of_god/`, `sounds.json`, subtitles in `lang/en_us.json`, registered in `sounds/ModSounds.java` (`HAND_...`) | `sounds.py` |
