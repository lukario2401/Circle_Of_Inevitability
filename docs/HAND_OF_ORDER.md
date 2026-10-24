# Hand Of Order: ability design

Circle Of Inevitability · Pathway plan

The Hand Of Order Pathway is the Justiciar Pathway of Lord of the Mysteries, the Judgment tarot card: Arbiter (9), Sheriff (8), Interrogator (7), Judge (6), Disciplinary Paladin (5), Imperative Mage (4), Chaos Hunter (3), Balancer (2), Hand of Order (1), Justiciar (0). Source: the Justiciar Pathway and Justiciar Pathway/Abilities pages of the Lord of the Mysteries wiki.

The abilities of this design are not written as code yet; the first `HandOfOrder` class (`beyonder_characteristics/HandOfOrder.java`) is still the one in game. This file is the plan: what every ability does, the numbers to start from, which existing parts of the spell kit build it, and which new reusable parts are worth writing first. Its looks, sounds and pictures are ready (see "Looks, sounds and pictures" at the end). When the code is written, move "Planned" to "What is built" (see [BEAUTY_GODDESS.md](BEAUTY_GODDESS.md)).

Pathway name in code: `"Hand Of Order"`.

## Theme

A pathway of Order: the rule, the verdict and the punishment. It watches (Jurisdiction, Recognition), interrogates (illusory lightning that hits the Spirit Body), judges (Verdicts spoken as one word), and writes Rules for a whole area that enemies break at their peril (Prohibition, Imperatives, Laws). Its answer to every "trick" in the game is a Rule that forbids the trick: teleporting, flying, hiding, stand-ins, resurrection, marionettes.

Its signature mechanic is not a stack counter like Attendant Of Mysteries. It is the **Convict** (Punishment marks one enemy) and the **Decree** (a zone with Rules; breaking a Rule is a Violation and every Violation is punished harder than the last).

Allies are never bound by your Decrees and Verdicts (the book's "Immunity"): everything here only affects enemies (`Spells.isEnemy`).

Colors: brass and white-gold for Order and the Brass Pillar, cold blue-white for Psychic lightning, iron grey for Rules and chains, dark red for Punishment and Death, dark violet for Disorder. Pathway color: `0xC9A24B` (brass). Other colors: white-gold `0xFFF3D0`, lightning `0xA8C8FF`, iron `0x3E434D`, verdict red `0xB03030`, Disorder `0x5A3A7A`.

From Sequence 4 a Brass Pillar can rise behind you as a visual (the incomplete Mythical Creature Form), and it stands behind the Court of Judgment at Sequence 0.

Nothing here breaks blocks or lights real fire. Rules, Verdicts and Judgment only affect entities; the ground is never changed.

## How it is laid out

It follows the Attendant Of Mysteries pattern: seven keys (G H J K L N M), every ability has a normal cast and a sneak cast. Four keys (3, 4, 5, 7) are wheels like Apocalypse's, where every entry has its own Sequence, cost and cooldown; on a wheel key, sneak casts the last pick again (`AbilityWheel.quickCast`). Keys 1, 2 and 6 are plain keys with a normal and a sneak ability.

The abilities do not each get a Sequence of their own. Sequence 6 has six abilities, Sequences 7 and 4 have three and four, Sequence 2 has three, and Sequences 5 and 3 have one each (but see "Grows with the Sequence": most abilities get stronger or new forms as you climb, so no Sequence is empty of change). Sequences 2, 1 and 0 are not left without abilities.

| Sequence | Abilities |
| --- | --- |
| Arbiter (9) | Halt, Presence |
| Sheriff (8) | Jurisdiction, Recognition |
| Interrogator (7) | Psychic Lashing, Psychic Piercing, Whip of Pain |
| Judge (6) | Imprison, Exile, Death, Maintain Secrecy, Prohibition: Passage, Prohibition: Concealment |
| Disciplinary Paladin (5) | Punishment |
| Imperative Mage (4) | Deprivation, Execution, Imperative: Reality, Contract |
| Chaos Hunter (3) | Sword of Judgment |
| Balancer (2) | Balance, Divide the Battlefield, Law: No Return |
| Hand of Order (1) | Order Proxy, Quell Disorder |
| Justiciar (0) | Court of Judgment |

Costs are spirituality, in the same scale as Attendant Of Mysteries (250 at the bottom, 12000 at Sequence 3). Costs for Sequences 2, 1 and 0 are placeholders (roughly 1.7x per Sequence): set them once the real spirituality pool at those Sequences is known. Cooldowns are in seconds and are a starting guess.

## Keys

| Key | Ability | Sequence | Normal cast | Sneak cast |
| --- | --- | --- | --- | --- |
| 1 | Authority | Arbiter (9) | **Halt**: one word at the enemy you look at (16 blocks). It is stopped for 2 seconds (held, cannot act; a player for 1 second) and every mob that was hunting you forgets you. Cost 250, cooldown 8. | **Presence**: a wave of Authority 8 blocks around you. Enemies get Slowness II and Weakness I for 6 seconds, and mobs under 60 max health are cowed: they cannot attack for 3 seconds. Players are only slowed. Cost 400, cooldown 14. |
| 2 | Jurisdiction | Sheriff (8) | **Jurisdiction**: claim a circle of ground, 24 blocks radius, centered where you stand, for 120 seconds (cast again to move it; one at a time). Inside it you sense every enemy that steps in (a soft chime and a glow through walls that only you see), and the place helps you: your Order abilities cost 20% less and your Decrees last 25% longer. Outside it you are on your own. Cost 1250, cooldown 20. | **Recognition**: memorize the enemy you look at (48 blocks). For 60 seconds it glows through walls for you, leaves footprints only you see, and its distance is always shown while it is within 64 blocks. One at a time. Cost 600, cooldown 6. |
| 3 | Interrogation | Interrogator (7) | Wheel: Psychic Lashing, Psychic Piercing, Whip of Pain. | The last pick again, without the wheel. |
| 4 | Verdict | Judge (6) and up | Wheel: Imprison, Exile, Death, Maintain Secrecy, Deprivation, Execution. | The last pick again, without the wheel. |
| 5 | Decree | Judge (6) and up | Wheel: Prohibition: Passage, Prohibition: Concealment, Imperative: Reality, Contract. | The last pick again, without the wheel. |
| 6 | Judgment | Disciplinary Paladin (5), Chaos Hunter (3) | **Punishment**: name the Convict: the enemy you look at (24 blocks). For 25 seconds a dawn-like brass glow rises around you, you deal 30% more damage to it, it is slowed by 15% and glows through walls, and to it you seem to grow taller. One Convict at a time. If you cast it at nothing, the next enemy to hit you within 8 seconds becomes the Convict. Cost 3000, cooldown 15. | **Sword of Judgment**: you step in a single stride to the most Disorderly enemy in view (the Convict, if you have one and it is within range), 32 blocks at most, and cut down. The Judgment cannot be dodged and deals 80 damage, plus 15% per point of its Disorder (up to x2.5). It also strips the harmful effects off you. Cost 12000, cooldown 30. |
| 7 | Order | Balancer (2) and up | Wheel: Balance, Divide the Battlefield, Law: No Return, Order Proxy, Quell Disorder, Court of Judgment. | The last pick again, without the wheel. |

### Key 3 wheel: Interrogation

| Entry | Sequence | Cost | Cooldown | What it does |
| --- | --- | --- | --- | --- |
| Psychic Lashing | Interrogator (7) | 1250 | 20 | For 20 seconds or 12 hits, whatever is in your hand (or your bare fist) is an Illusory Torture Device: an illusory branding iron flickers along it and every hit adds 6 lightning damage that ignores armor, staggers the target, and lands on spirits and summoned things that physical hits pass through (marionettes, spirit bodies). |
| Psychic Piercing | Interrogator (7) | 1250 | 10 | Bolts of lightning leap from your eyes in a thin beam, 12 blocks (the book says 5 meters; 12 plays better). Everything it touches in the line, up to 3 enemies, takes 30 damage that ignores armor, and spirits and marionettes take double. You can keep walking while you fire it. |
| Whip of Pain | Interrogator (7) | 1500 | 25 | A barbed whip of lightning lashes the mind of the enemy you look at (16 blocks): 20 damage that ignores armor, then for 4 seconds it trembles (Slowness III, Weakness, cannot sprint, screen shakes for a player). It cannot lie: Concealment on it ends at once, and every effect on it is shown to you. |

### Key 4 wheel: Verdict

Verdicts are one spoken word. Each shows its word above you when cast.

| Entry | Sequence | Cost | Cooldown | What it does |
| --- | --- | --- | --- | --- |
| Imprison | Judge (6) | 2450 | 20 | Layers of transparent walls close around the enemy you look at (20 blocks): it is held for 5 seconds, spirits too. Casting it again on the same target while it holds says "Release" and frees it for free (also works on an ally you held by mistake). |
| Exile | Judge (6) | 2450 | 20 | One word and an invisible force throws the enemy you look at away from you at great speed: about 20 blocks, Slowness II for 2 seconds. It works on spirits too. |
| Death | Judge (6) | 2450 | 25 | You declare the target dead. Your body blurs into a rush (up to 14 blocks, afterimages) and your fist strikes its head with a blow it cannot dodge: 60 damage and 1.5 seconds stunned. |
| Maintain Secrecy | Judge (6) | 1800 | 30 | Your eyes flash with a strange light as you look at an enemy (12 blocks): for 8 seconds it must obey and reveal nothing, so it cannot use abilities (Silence). A mob loses its special attacks if it has any, otherwise it forgets you for 4 seconds. |
| Deprivation | Imperative Mage (4) | 6000 | 40 | Strips one power from the enemy you look at: the ability it used last (a mob: its special attack) is dead for 30 seconds (a boss: 15). It is not taken away, only made unusable. |
| Execution | Imperative Mage (4) | 6000 | 60 | You raise your right fist (1.5 seconds, a ring closes on the target, any damage you take breaks it) and swing it down. The enemy you looked at takes 120 damage from any distance in view (32 blocks). If it is under 30% health it is simply killed (players and bosses take double damage instead). |

### Key 5 wheel: Decree

A Decree is a zone fixed where you stand (aimed up to 64 blocks away from Hand of Order (1)). Radius 14 unless stated. Allies are exempt. Every Rule broken inside is a Violation (see "New reusable parts: Decree").

| Entry | Sequence | Cost | Cooldown | What it does |
| --- | --- | --- | --- | --- |
| Prohibition: Passage | Judge (6) | 2450 | 25 | "Teleportation, flight and dashing are prohibited here!" For 12 seconds enemies inside cannot teleport (they are rooted against teleporting, so abilities like Flaming Jump say "You cannot leave this place"), cannot fly, glide or levitate, and dash and leap abilities are refused. |
| Prohibition: Concealment | Judge (6) | 2450 | 25 | "Concealment is prohibited here!" For 12 seconds every enemy inside is revealed (Concealment ends, invisibility is stripped, a brief outline) and cannot hide again while inside it. This is the direct answer to hiding in the fog of history. |
| Imperative: Reality | Imperative Mage (4) | 6000 | 45 | "Weaken Mysticism, enhance Reality." 18 blocks radius, 30 seconds. All ability damage inside is halved (your allies' too, as in the book) and abilities cost 50% more spirituality, while plain weapon, arrow and fall damage is 25% higher. Your own Verdicts, Decrees and Order abilities are Order and are not weakened. |
| Contract | Imperative Mage (4) | 6000 | 40 | A paper contract appears and the enemy you look at (16 blocks) signs it, with no say in the matter. For 30 seconds it cannot harm you: each attempt (melee, arrow or ability) is cancelled and counts as a Violation of the Contract (1st: staggered, 2nd: 40 damage, 3rd: held 3 seconds). If you strike it, the paper tears and the Contract ends. |

### Key 7 wheel: Order

| Entry | Sequence | Cost | Cooldown | What it does |
| --- | --- | --- | --- | --- |
| Balance | Balancer (2) | 20000 | 40 | A one-on-one scale. The enemy you look at (24 blocks) is pulled down to your level for up to 20 seconds: its damage, armor, speed and max health are lowered to match yours. The stronger it is compared to you, the shorter it lasts (20 seconds x your max health / its max health, never under 3; a boss: 3). It ends if either of you dies. (The book also raises the weaker side; here weaker enemies are simply left alone.) |
| Divide the Battlefield | Balancer (2) | 20000 | 60 | A ring of Order, 20 blocks wide, closes around you for 25 seconds. Nothing from outside gets in (enemy projectiles and teleports are stopped, enemies are pushed back) and no enemy inside can teleport out. Any enemy whose max health is more than 3 times yours is thrown out at once and cannot return while the ring lasts; bosses never fit. Allies cross freely. |
| Law: No Return | Balancer (2) | 20000 | 60 | "Resurrection of the dead is prohibited here!" 20 blocks radius, 45 seconds. Death wards, miracles, totems and every revival on enemies inside are void (armed ones do not fire), and marionettes of enemies inside stop moving. The dead stay dead. |
| Order Proxy | Hand of Order (1) | 30000 | 120 | You act as the Nation of Disorder's proxy. For 6 seconds: Strength IV, Resistance III, Speed II, immune to Rules, roots and Silence, and every Hand Of Order ability costs nothing and has no cooldown. When it ends you are left in Weakness III and Slowness II for 30 seconds and half your spirituality is gone. |
| Quell Disorder | Hand of Order (1) | 30000 | 45 | Order restores itself everywhere within 24 blocks. Everything enemies made with abilities is quelled: Concealment ends, marionettes are freed, substitutes and death wards drop, damage links are cut, enemy Decrees and zones end. Allies are untouched and are cleansed of harmful effects. |
| Court of Judgment | Justiciar (0) | 60000 | 180 | The Ancient Court of Judgment. A dome of brass and white light, 40 blocks radius, with a Brass Pillar behind you, for 60 seconds. Rules are written clause by clause: one at once, a new one every 10 seconds: 1 No Passage, 2 No Concealment, 3 No Resurrection, 4 No Stand-ins, 5 No Healing, 6 No Projectiles, 7 No Abilities. An enemy that breaks a Rule is Judged, and the sentence grows with each breach: 1st a warning (staggered, marked), 2nd 60 damage and held 2 seconds, 3rd 150 damage and held 4 seconds, 4th Death (200 damage at once, no healing). You and your allies are immune. Cast it again to adjourn early. |

## Grows with the Sequence

These do not get their own key or entry. The same ability is stronger, or a different form, from the Sequence on.

| Ability | From | What changes |
| --- | --- | --- |
| Halt | Judge (6) | Holds 3 seconds (a player 1.5). |
| Presence | Judge (6) | 12 blocks. |
| Presence | Imperative Mage (4) | Intimidation: enemies in the wave are also rooted for 1.5 seconds. |
| Jurisdiction | Judge (6) | 48 blocks radius. |
| Jurisdiction | Chaos Hunter (3) | 96 blocks (a whole city). Inside it Sword of Judgment's range doubles to 64. |
| Jurisdiction | Hand of Order (1) | Two Jurisdictions at once. |
| Recognition | Imperative Mage (4) | Also shows its pathway and Sequence, and the colors of what it has active. Concealed enemies are still seen as a faint outline within 6 blocks. |
| Recognition | Chaos Hunter (3) | Also shows its Disorder. |
| Imprison becomes Confinement | Imperative Mage (4) | A transparent cell closes around the target, 8 seconds. Nothing leaves or enters; teleports and substitutes fail inside. |
| Exile | Imperative Mage (4) | Sends the target into a void: for 4 seconds it is gone (untouchable, cannot act), then returns on the same spot. |
| Exile | Chaos Hunter (3) | A hurricane forms at the target and also throws away everything within 5 blocks of it. |
| Death | Disciplinary Paladin (5) | Aimed at its latest wound: +50% damage and the wound opens (Wither for 5 seconds). |
| Deprivation | Chaos Hunter (3) | Strips up to 3 abilities. |
| Prohibition (both) | Disciplinary Paladin (5) | Casting the same one again within 10 seconds adds a layer (up to 3): each adds duration and makes stronger enemies, who shrug the first layer off, obey at two thirds strength. |
| Prohibition: Concealment | Imperative Mage (4) | Also forbids stand-ins: figurine and marionette swaps fail. |
| Prohibition (both) | Chaos Hunter (3) | Also freezes marionettes inside. |
| Decrees | Balancer (2) | Two Decrees can be active at once ("Laws"). |
| Decrees | Hand of Order (1) | Three at once, last twice as long, and can be placed anywhere in view up to 64 blocks. |
| Punishment | Chaos Hunter (3) | Violations by the Convict inside your Decree make your next hit on it a sure hit for +50% ("Violators will be punished!"). |
| Sword of Judgment | Balancer (2) | Range doubled. |
| Order Proxy | Justiciar (0) | 10 seconds, and the weakness afterwards is half as long. |
| Court of Judgment | Justiciar (0) | Sword of Judgment is free and instant on any enemy that gets a 3rd breach inside it. |

## Passives

| Passive | From Sequence |
| --- | --- |
| Order: Strength I; weak mobs (under 30 max health) within 5 blocks keep their distance | Arbiter (9) |
| Weapon Proficiency: faster attacks and a little more reach with any weapon; Sheriff's Intuition: a soft chime when something starts hunting you from out of sight | Sheriff (8) |
| Demolition Proficiency: explosions hurt you 50% less; your weapon hits reach spirit-bodied things | Interrogator (7) |
| Judge's Aura: enemies within 6 blocks deal 10% less damage to you | Judge (6) |
| Self-Restraint: mind effects on you (Silence, Whip of Pain, hypnosis-like effects) last half as long; Resistance I | Disciplinary Paladin (5) |
| Beyonder Sense: you can tell Beyonders from ordinary mobs and players on sight; recoil of your Rules on you is halved | Imperative Mage (4) |
| Disorder Sense: enemies with high Disorder glow for you within 32 blocks | Chaos Hunter (3) |
| Sense Imbalance: Concealed enemies within 24 blocks cannot hide from you | Balancer (2) |
| Sense of Order: a chime when anyone casts an ability or writes a Rule within 48 blocks; Rules written by an enemy of a lower Sequence cannot bind you | Hand of Order (1) |
| Law of Order: your Decrees cost 25% less and last 25% longer | Justiciar (0) |

## What each ability is built from

Existing parts are the ones in the Attendant Of Mysteries class and [SPELL_KIT.md](SPELL_KIT.md). "New" points to the reusable parts in the next section. Every look and sound is a call on `HandOfOrderFx` (see the last section).

Key 1 and 2
- **Halt**: `Spells.aimEnemy`, `Root.apply(.., RootRestriction.EVERYTHING)` for 2 seconds, mob forgetting like Presence below. Word above you: `HandOfOrderFx.speak` (the planned `SpellFx.speak`).
- **Presence**: `Spells.enemiesAround`, potion effects like stiffen in the Spirit Body Threads code, `SpellFx.shockRing`.
- **Jurisdiction**: New `Zone` (the Flames list in Attendant is the same idea done by hand), `SpellFx.runeCircle` for the edge, `Later.run` for the entry check each second.
- **Recognition**: New `Mark`, glow like Grafting's `MobEffects.GLOWING`, footprints with `ParticleShapes`.

Key 3
- **Psychic Lashing**: New `WeaponCoat`, `Spells.strike`, `SlashFx` for the lightning arc, `ModelFx` for the branding iron.
- **Psychic Piercing**: instant beam with `SpellFx.tether` for 4 ticks, enemies along the line (see `Rush` and line below), `Spells.strike`.
- **Whip of Pain**: `Spells.strike`, `Concealment.reveal`, `Spells.harmfulEffects`-style list of what is on it, `Root`-free: slowness only.

Key 4
- **Imprison/Confinement**: `Root.apply(.., EVERYTHING)`, `SpellFx.closingRing`, `ParticleShapes.model` for panes, `Countdown` is not needed.
- **Exile**: `Spells.push`, `FallGuard` is not applied to the enemy; the void form uses `Concealment.hide` plus `Root`.
- **Death**: New `Rush`, `Spells.strike`, `Root.apply` for the stun.
- **Maintain Secrecy and Deprivation**: New `Seal`.
- **Execution**: `Countdown.seconds(..).keepWhile(..).onFinish(..)` exactly like `seizeThreads`, `SpellFx.closingRing` on the target while it counts.

Key 5
- **All four**: New `Zone` + `Decree`. Passage uses `Root.apply(.., TELEPORT)` for the teleport ban (Flaming Jump already checks it). Concealment uses `Concealment.reveal` plus a Rule check in `Concealment.hide`.
- **Imperative: Reality**: a power multiplier read inside `Spells.strike` (see Decree).
- **Contract**: `Mark` plus the Decree Violation ladder; the cancelled hits need a small damage hook (the one `DamageLink` and `Substitute` already use).

Key 6
- **Punishment**: New `Mark`, `TempModifier` for the slow, glow with `MobEffects.GLOWING`.
- **Sword of Judgment**: New `Rush` (a blink variant), New `Disorder`, `Spells.cleanse`, `Spells.strike`, `Root.has(.., TELEPORT)` check like Flaming Jump (so Passage stops your own Judgment too if an enemy Decree covers you).

Key 7
- **Balance**: New `TempModifier`.
- **Divide the Battlefield**: New `Zone` with a SEALED Rule, `SpellFx.runeCircle` and `shockRing` for the wall, `Spells.push`.
- **Law: No Return**: Decree Rule RESURRECTION and MARIONETTES, `DeathWard` and `Marionette` check it.
- **Order Proxy**: `TempModifier`, a "free and no cooldown" flag read by the cooldown code, `Root` immunity flag.
- **Quell Disorder**: a `clear(entity)` on each status (`Concealment`, `Marionette`, `Substitute`, `DeathWard`, `DamageLink`, `Decree`, `Zone`), then `Spells.cleanse` on allies.
- **Court of Judgment**: `Zone` + `Decree` with a clause timeline, `ScreenImages` for the clause list, `SpellFx.warningCircle` for the dome edge, `ModelFx` Brass Pillar.

## New reusable parts (write these first, in this order)

Written as parts of the spell kit, not of this pathway, so other pathways can use them. The Hand Of God plan needs several of the same ones.

1. **Zone** (`status/Zone`): an area with an owner, a level, a center (fixed, or following an entity), a radius, a lifetime and a per-second callback, kept in one list the way `FLAMES` is kept now. `Zone.at(level, pos)` returns the zones covering a point and `Zone.inside(entity)` the ones an entity stands in. Used by: Jurisdiction, every Decree, Divide the Battlefield, Court of Judgment. Also by Hand Of God: Light of Dawn, Protection, Decay, Divine Kingdom. Attendant's flames could move onto it later.
2. **Decree** (`status/Decree`, built on Zone): a Zone with a set of Rule flags: `PASSAGE`, `CONCEALMENT`, `STAND_INS`, `RESURRECTION`, `MARIONETTES`, `HEALING`, `PROJECTILES`, `ABILITIES`, `SEALED`. It has: `Decree.forbids(entity, Rule)` (false for the owner's allies), `Decree.powerOf(pos, kind)` (the Imperative multipliers, read inside `Spells.strike` and `paySpirituality`), and the Violation ladder: `Decree.violate(entity, owner, rule)` runs the next step of a list of consequences (staggered, damage, held, death) and remembers how many times that entity has broken Rules. One line in each of `Concealment.hide`, `Substitute.arm`, `Marionette.control`, `DeathWard.arm` asks `Decree.forbids` first. This is the heart of the pathway.
3. **Mark** (`status/Mark`): a named mark one caster puts on one target for a time: `Mark.set(player, "convict", target, ticks)`, `Mark.of(player, "convict")`, `Mark.is(target, "convict")`. Used by Punishment, Recognition, Contract. Also useful for Hand Of God's Eye of Demon Hunting and any "the enemy I looked at" ability that lasts.
4. **TempModifier** (`combat/TempModifier`): a timed attribute change that is removed cleanly and can be refreshed: `TempModifier.add(entity, attribute, amount, ticks, tag)`. Used by Balance, Punishment, Order Proxy, Whip of Pain. Also by Hand Of God: Dawn Armor, Giant Form, Supernatural Resistance.
5. **Seal** (`combat/Seal`) and an ability tag: `Seal.block(entity, tag, ticks)`. Abilities pass a small tag (movement, concealment, summon, projectile, anything) into `Spells.paySpirituality`, and one check there refuses a sealed tag. Used by Maintain Secrecy (everything), Deprivation (one tag), Decree `ABILITIES`. This is a small change to the shared kit, so it is worth deciding on before the first ability is written.
6. **WeaponCoat** (`combat/WeaponCoat`): the weapon in hand carries an effect for N hits or N seconds: `WeaponCoat.apply(player, ticks, hits, (target, amount) -> ...)`. Used by Psychic Lashing. Also by Hand Of God: Alchemy and the Sword of Dawn's purifying hits.
7. **Disorder** (`combat/Disorder`): a score from 0 up for an entity: +1 for each active Concealment, Substitute, Marionette, Death ward, damage link or root on it, +1 for each ability it cast in the last 10 seconds (at most 3), +1 for each Violation. `Disorder.of(entity)`. Used by Sword of Judgment, Disorder Sense, Recognition at Sequence 3.
8. **Rush** (`combat/Rush`): a short body-rush toward a point or entity with afterimages and an on-contact callback: `Rush.toward(player, target, blocks, onContact)`. Also a "line" check, `Spells.enemiesAlong(player, sl, from, direction, range)`, for Psychic Piercing. Used by Death, Sword of Judgment, Psychic Piercing. Also by Hand Of God: Warrior's Charge, Mercury Liquefaction.
9. **Quell** (`clear(entity)` on every status, and `Quell.all(entity)` that calls them): one place that ends everything ability-made on an entity. Used by Quell Disorder; also good for any future "dispel".
10. **SpellFx.speak**: shows a spoken word above a caster or target and plays a low sound. Done for this Pathway as `HandOfOrderFx.speak(sl, who, Word.X)` with 17 word models; move it into `SpellFx` when another Pathway needs words.

## How Decrees stop things in the repo

| Rule | What it stops |
| --- | --- |
| PASSAGE | Flaming Jump (already checks `Root.has(.., TELEPORT)`), the sneak Air Cannon leap, any future teleport, blink or dash |
| CONCEALMENT | `Concealment.hide` (the Historical Void sneak, Miracle's return, Light Concealment); and `Concealment.reveal` on entry |
| STAND_INS | `Substitute.arm` and `figurineEscape` (paper figurine, marionette swap) |
| RESURRECTION | `DeathWard.arm` and `miracleReturn`; armed wards do not fire inside |
| MARIONETTES | Marionette puppets stop moving, `Marionette.control` refuses |
| HEALING | healing and regeneration |
| PROJECTILES | Shot projectiles from enemies are stopped at the edge |
| ABILITIES | `Seal` on every tag |
| SEALED | nothing crosses the edge but the owner's allies |

## Settings and names for the code

Keep every number in the tables above as a `private static final` at the top of the class under "Ability settings", named `ABILITY_THING` with a comment for units, like Attendant Of Mysteries (`HALT_COST`, `HALT_RANGE`, `HALT_TICKS`, `PRESENCE_RADIUS`, ...). Saved-on-player keys start with `hand_of_order_`. Status icons next to the hotbar are `HandOfOrderFx.Status` (`JURISDICTION`, `CONVICT`, `DECREE`, `SILENCED`, `CONTRACT`, `PROXY`, `COURT`, and the rest). Each ability checks `ready(player, bypassClassCheck, sequence)` first, then `paySpirituality` last, like the other pathways; the wheel entries carry their own Sequence for the same check.

## Melee combo

`MeleeCombo.forPathway("Hand Of Order")`: 1 = a brass ring sweeping around you with branding-iron sparks, 2 = a crossed pair of lightning slashes on the target, 3 = random between a ring of Rule glyphs spreading on the ground and a straight slash slamming down on the target like a gavel. The look is `HandOfOrderFx.combo(sl, player, target, step)`; register the steps in the new class the way `KnowledgeEmperor` does.

## Left out on purpose

- Flog: covered by Psychic Piercing and Whip of Pain.
- Illusory Torture Devices: lives in Psychic Lashing and in the melee combo's branding iron.
- Intimidation: folded into Presence at Sequence 4.
- Confinement: the grown form of Imprison.
- Imperative: Mysticism (the opposite of Reality): easy to add later as one more wheel entry on the same Decree code with the multipliers swapped.
- The Brass Pillar Mythical Creature Form as its own ability: only a visual for now (`HandOfOrderFx.brassPillar`), it could become a Giant-Form-style entry later.
- Resurrection through Rules and granting Immunity to chosen people: Immunity is built into every Decree (allies exempt).
- Official appointment bigger Jurisdiction: not worth a mechanic.

## Looks, sounds and pictures

Everything the abilities need to look and sound right is in `HandOfOrderFx.java` (`beyonder_characteristics/hand_of_order/`). It only shows things; the ability does the damage, holding, hiding, healing and moving, at the ticks its constants name. `/orderfx all` plays every display in game, `/orderfx <name>` one of them (TAB lists the names; there are also `words`, `intimidation`, `confinement`, `exile_void`, `exile_hurricane`, `death_wound`, `execution_slain`, `execution_broken`, `prohibition_layers`, `violations`, `brass_pillar`, `passives`, `combo` and `statuses`).

**Words.** Every Verdict and Decree is spoken: `speak(sl, who, Word.X)` raises the word over 'who' in its color and a low voice says it (`speak(sl, who, word, size, false)` without the voice). The words: HALT, IMPRISON, RELEASE, EXILE, DEATH, SILENCE, DEPRIVE, EXECUTE, GUILTY, SIGN, PROHIBITED, REALITY, NO RETURN, BALANCE, ORDER, VIOLATION, JUDGMENT. The ability displays below already speak their own word.

| Ability | Normal cast | Sneak cast |
| --- | --- | --- |
| 1 Authority | Halt: `halt(sl, player, target)` (holds at `HALT_LANDS`), `halted(sl, target, ticks, tag)`, `forgets(sl, mob)` for each mob that stops hunting | Presence: `presence(sl, player, radius, intimidation)` (`intimidation` = the Imperative Mage's form: the Pillar flashes up and shackles fall), `cowed(sl, mob, ticks, tag)` |
| 2 Jurisdiction | `jurisdiction(sl, player, center, radius, ticks, tag)` (claimed at `JURISDICTION_CLAIMED`), `jurisdictionTick(sl, center, radius)` every few seconds, `jurisdictionEnter(owner, who)` (only the owner sees and hears), `jurisdictionEnd(sl, center, radius, tag)` | Recognition: `recognition(owner, target, ticks, tag)`, `recognitionTrack(owner, target)` each second (footprints and a mark only the owner sees), `recognitionEnd` |
| 3 Interrogation (wheel) | Psychic Lashing: `psychicLashing(sl, player, ticks, tag)`, `lashingTick` every few ticks, `lashHit(sl, player, target)`, `lashingEnd`. Psychic Piercing: `psychicPiercing(sl, player, end, hits)`, `piercingHit(sl, target, spirit)`. Whip of Pain: `whipOfPain(sl, player, target)` (lands at `WHIP_LANDS`), `trembling(sl, target, ticks, tag)`, `truthOut(sl, target)`. Icons `WHEEL_PSYCHIC_LASHING`, `WHEEL_PSYCHIC_PIERCING`, `WHEEL_WHIP_OF_PAIN` | the last pick again |
| 4 Verdict (wheel) | Imprison: `imprison(sl, player, target, ticks, tag)` (closed at `IMPRISON_LANDS`), `release(sl, target, tag)`, `imprisonEnd`; Confinement: `confinement(...)`. Exile: `exile(sl, player, target, to)` (thrown at `EXILE_LANDS`); the void `exileVoid(sl, target, ticks, tag)` and `exileReturn(sl, at, tag)`; the hurricane `exileHurricane(sl, center, radius)`. Death: `death(sl, player, from, target, wound)` (the blow at `DEATH_LANDS`; `wound` = the Paladin's form). Maintain Secrecy: `maintainSecrecy(sl, player, target, ticks, tag)`, `secrecyEnd`. Deprivation: `deprivation(sl, player, target, count)` (locked away at `DEPRIVED`), `deprived(sl, target, ticks, tag)`, `deprivedEnd`. Execution: `execution(sl, player, target, tag)` (falls at `EXECUTION_LANDS`), then `executionStrike(sl, player, target, slain)` or `executionBroken(sl, player, target, tag)`. Icons `WHEEL_IMPRISON` ... `WHEEL_EXECUTION` | the last pick again |
| 5 Decree (wheel) | Every Decree: `decree(sl, player, center, radius, ticks, Rule.X, tag)` (in force at `DECREE_RISEN`), `decreeTick(sl, center, radius, rule)` now and then, `decreeEnd(sl, center, radius, tag)`; layers `decreeLayer(sl, center, radius, layer, tag)`. What it stops: `refused(sl, who)` (a teleport, flight, dash, hiding or stand-in refused), `revealed(sl, who)` (Concealment broken), `violation(sl, who, count)` (each Rule broken, redder each time). Imperative: Reality: `reality(sl, player, center, radius, ticks, tag)`, `realityEnd`. Contract: `contract(sl, player, target, ticks, tag)` (signed at `CONTRACT_SIGNED`), `contractViolation(sl, target, step)`, `contractTear(sl, target, tag)`. Icons `WHEEL_PROHIBITION_PASSAGE` ... `WHEEL_CONTRACT` | the last pick again |
| 6 Judgment | Punishment: `punishment(sl, player, target, ticks, tag)` (target null = no Convict yet), `convictHit(sl, target)`, `convictPunished(sl, target)` (the Chaos Hunter's sure hit), `punishmentEnd(sl, player, target, tag)` | Sword of Judgment: `swordOfJudgment(sl, player, from, target)` (the cut at `SWORD_LANDS`), `judgmentCleanse(sl, player)` |
| 7 Order (wheel) | Balance: `balance(sl, player, target, ticks, tag)` (level at `BALANCE_SETTLED`), `balanceEnd`. Divide the Battlefield: `divideBattlefield(sl, player, center, radius, ticks, tag)` (risen at `DIVIDE_RISEN`), `divideBlocked(sl, at)`, `thrownOut(sl, who)`, `divideEnd`. Law: No Return: `lawNoReturn(sl, player, center, radius, ticks, tag)`, `revivalVoided(sl, who)`, `puppetFrozen(sl, puppet)`, `decreeEnd`. Order Proxy: `orderProxy(sl, player, ticks, tag)`, `proxyTick` every few ticks, `proxyEnd(sl, player, tag)` (shows Spent; `statusEnd(player, Status.SPENT)` when the weakness ends). Quell Disorder: `quellDisorder(sl, player, radius)`, `quelled(sl, who)`, `cleansed(sl, ally)`. Court of Judgment: `court(sl, player, center, radius, ticks, tag)` (in session at `COURT_RISEN`), `clause(sl, center, radius, index)` every 10 seconds, `judged(sl, who, breach)` for the 1st to 4th breach, `adjourn(sl, center, radius, tag)`; on each player's screen `courtClauses(player, written, ticks)` and `courtClausesHide(player)`. Icons `WHEEL_BALANCE` ... `WHEEL_COURT_OF_JUDGMENT` | the last pick again |
| Form | `brassPillar(sl, player, ticks, tag)`: the Imperative Mage's Brass Pillar behind them | |
| Passives | `orderPresence(sl, player)` now and then, `intuition(owner)`, `senseOrder(owner, at)`, `disorderSense(owner, who)`, `beyonderSense(owner, who)` (only the owner sees or hears these), `selfRestraint(sl, player)` | |
| Melee | `combo(sl, player, target, step)` for hits 0, 1 and 2 | |

Every timed cast can show its picture and clock above the hotbar: `HandOfOrderFx.status(player, Status.X, ticks)`, and `statusEnd` to take it away (25 statuses: 10 for what the Justiciar holds, 15 for what is done to others when they are players; most displays above already show theirs). The Pathway's icon beside the hotbar is registered in `PathwayHudIcon` under `"Hand Of Order"`.

Things to know when the abilities are written:
- Models stuck to an entity (Imprison's walls, the Convict's brand, the halo) are taken off with `ParticleShapes.clearModels(sl, entity, tag)`; the ones on the ground (Decrees, the Court, the void) with the same tag from any spot, so give every cast its own tag.
- The Court's dome is three rings of `order_wall` panels, up to 40 per ring; at radius 40 that is the most models any display here makes at once.
- The old `textures/gui/ability_menus/hand_of_order/explosion`, `speed` and `judgement` and `models/model_fx/hand_of_order/sword` belong to the first `HandOfOrder` class. Delete them with it.

What is in the repository, all of it generated by the scripts in `tools/model_gen/hand_of_order/` (see "Hand Of Order: ready-made displays" in [MODEL_FX.md](MODEL_FX.md)):

| What | Where | Made by |
| --- | --- | --- |
| 40 model files: the Brass Pillar, the scales (stand and beam), the branding iron, the spectral fist, a glass pane and the Confinement cell, a gavel, the Sword of Judgment; the pictures: Jurisdiction's seal, a Decree's circle, the Court's floor, Reality's seal, No Return's door, the Quell seal, the Prohibition sigil, the Convict's brand, the Recognition mark, the Execution ring, the void, the dawn halo, the Contract, the wall of Order (animated); and 17 spoken words | `models/model_fx/hand_of_order/`, `textures/model_fx/hand_of_order/` | `models.py` (+ `textures.py`) |
| 19 wheel pictures (32 px), 25 status pictures, 7 clause pictures (16 px), the Court's tablet (64 px), the Pathway icon | `textures/gui/ability_menus/hand_of_order/`, `textures/gui/hand_of_order/`, `textures/gui/pathway_icons/hand_of_order.png` | `icons.py` |
| 8 particle pictures: `fx/law_0` to `fx/law_2`, `fx/prohibit`, `fx/shackle`, `fx/brand`, `fx/bolt`, `fx/verdict` | `textures/particle/fx/` | `particles.py` |
| 42 sounds (45 files): halt, presence, jurisdiction and someone entering it, recognition, the intuition chime, Psychic Lashing and its hits, Psychic Piercing, the Whip of Pain, a Verdict spoken, Imprison and Release, Exile and the void, Death, Maintain Secrecy, Deprivation, Execution raised and falling, a Prohibition, a refusal, a Violation, Punishment, Reality, the Contract and its tearing, the Brass Pillar, the Sword of Judgment, Balance, Divide the Battlefield, No Return, Order Proxy and its end, Quell Disorder, Sense of Order, the Court convening, a clause, a sentence, adjourning, and the combo's swings and gavel | `sounds/hand_of_order/`, `sounds.json`, subtitles in `lang/en_us.json`, registered in `sounds/ModSounds.java` (`ORDER_...`) | `sounds.py` |
