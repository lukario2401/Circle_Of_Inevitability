# Knowledge Emperor: ability design

Circle Of Inevitability · Pathway plan

The Knowledge Emperor Pathway is the Hermit Pathway (also called the Mystery Pryer Pathway) of Lord of the Mysteries, the Hermit tarot card: Mystery Pryer (9), Melee Scholar (8), Warlock (7), Scrolls Professor (6), Constellations Master (5), Mysticologist (4), Clairvoyant (3), Sage (2), Knowledge Emperor (1), Hermit (0). Source: the Hermit Pathway page of the Lord of the Mysteries wiki. The Hermit Pathway/Abilities page could not be opened while this was written, so the lower Sequences follow summaries of it and the series itself. Check names against the wiki before coding.

The abilities are not written as code yet. This file is the plan: what every ability does, the numbers to start from, which existing parts of the spell kit build it, and which new reusable parts are worth writing first. Its looks, sounds and pictures are ready (see "Looks, sounds and pictures (ready)" at the end). When the code is written, move "Planned" to "What is built" (see [BEAUTY_GODDESS.md](BEAUTY_GODDESS.md)).

Pathway name in code: "Knowledge Emperor".

## Theme

An occult scholar's Pathway: it sees what is hidden, casts ritual magic and spells of its own making, keeps spells in scrolls, pulls down stars, retells legends as magic, reads fate, and at the top turns into information itself. Its real weapon is understanding: the more you know about an enemy the harder you hit it, and at the Emperor's level knowledge is thrown straight into minds.

The Hidden Sage whispers knowledge into every Hermit Beyonder (the "Knowledge Pursuit"). Here that is only flavor: a small gift, never a danger.

Colors: orchid purple (the Pathway's color on the wiki) and star-white for prying, starlight and the cage; ink-black and deep indigo for the void and for information; pale gold for scrolls and ink. Pathway color: `0xC070E8`. The symbol is a pair of cold eyes full of stars, used for the Study marker. The Mythical Creature form is an information being like a giant serpent or eyeballs, so it only shows up as a visual (Informatization, Divine Kingdom).

Nothing here breaks blocks or lights real fire. Stars, scrolls and knowledge only show and hurt; the ground is never changed.

## How it is laid out

It follows the Attendant Of Mysteries and Hand Of God pattern: seven keys (G H J K L N M), every ability has a normal cast and a sneak cast. Keys 5, 6 and 7 are wheels where every entry has its own Sequence, cost and cooldown; on a wheel key, sneak casts the last pick again (`AbilityWheel.quickCast`). Keys 1 to 6 unlock at Sequences 9 to 4 and key 7 at Sage (2); Clairvoyant (3) adds entries to key 6 instead of getting a key.

The abilities do not each get a Sequence of their own. Sequences 9, 8, 7, 6 and 2 have two abilities, Sequences 4, 3, 1 and 0 have three, Constellations Master (5) has four. Sequences 2, 1 and 0 are not left without abilities, and they also strengthen what is already there (see "Grows with the Sequence").

| Sequence | Abilities |
| --- | --- |
| Mystery Pryer (9) | Mystery Prying, Dowsing |
| Melee Scholar (8) | Weak-Point Strike, Sidestep |
| Warlock (7) | Hex, Spellbolts |
| Scrolls Professor (6) | Read Scroll, Inscribe Scroll |
| Constellations Master (5) | Starlight Cage, Stellar Self, Star Fall, Star Guard |
| Mysticologist (4) | Sleeping Beauty, Avalon, Achilles' Heel |
| Clairvoyant (3) | Pandora's Box, Foresight, Prophecy of Ruin |
| Sage (2) | Informatization, Erase |
| Knowledge Emperor (1) | Knowledge Overload, Lance of Knowledge, Condense Knowledge |
| Hermit (0) | Embodiment of Knowledge, Indoctrination, Divine Kingdom |

## Shared mechanics

Three small systems tie the Pathway together. Each one is worth writing once and reusing (see "New reusable parts").

**Study.** What you know about an enemy. Every enemy has a Study gauge from 0 to 100% that only you see (an eye over its head: closed, half open, wide open). It rises when you Pry it (+25%), Hex it (+20%), hit it with an ability (+10% unless listed otherwise) or land a melee hit (+5%), and falls 5% a second once it has been left alone for 10 seconds. Every 10% adds 5% to the damage of everything you do to it (up to +50%). At 50% its weakness is known (Spellbolts pick the right element, Prying shows the weakness). At 100% it is Understood and Knowledge Overload can kill it. Study is per caster; two Hermits do not share it.

**Scroll pouch.** Holds 3 scrolls, shown as pictures next to the hotbar like the Attendant's stacks. A scroll is one stored cast of a spell: reading it is free, instant, with no chant and no cooldown of its own. While there is room, the pouch adds a Scroll of Freezing or a Scroll of Paralysis, in turn, every 25 seconds. Freezing: a transparent ray falls on the enemy, Slowness V and no jumping for 3 seconds. Paralysis: a pale green ray flies at it, held (`Root` EVERYTHING) for 1.5 seconds. Every ability marked "Scrollable" can be inscribed.

**Obscurity.** The fewer who know a story, the stronger it is. Each retelling on key 6 remembers how often it was told in the last 5 minutes: untold is full power, every recent telling takes 20% off (never under 40%). Rotating through the wheel always beats repeating one story. Each retelling also has a long cooldown of its own and all share a 10 second one.

## Keys

Costs for keys 1 to 4 (spirituality, both casts): 250, 1250, 1250 and 2450. Reading a scroll costs nothing. These follow the Attendant's scale; the wheel entries carry their own costs below.

| Key | Ability | Sequence | Normal cast | Sneak cast |
| --- | --- | --- | --- | --- |
| 1 | Mystery Prying | Mystery Pryer (9) | Pry: for 10 seconds hidden things within 30 blocks (invisible, concealed, spirits, marionettes) glow through walls. The enemy you look at shows its health, its mood (calm, afraid, furious) and every effect on it, and gains 25% Study. | Dowsing: a silver pendulum swings toward the nearest enemy you cannot see (behind walls or hidden) within 64 blocks, and a thread of light points the way for 12 seconds. With nothing hidden near, it hangs still and costs nothing. |
| 2 | Melee Scholar | Melee Scholar (8) | Weak-Point Strike: step forward about 3 blocks and strike the enemy in front where it is weakest: 30 damage that ignores armor, staggers it and adds 10% Study. Works with fists or any weapon. | Sidestep: for 2 seconds the next melee hit or projectile aimed at you is slipped by a quick 4 block sidestep that leaves you at the attacker's side. Your next hit within 2 seconds is a free Weak-Point Strike. |
| 3 | Spells | Warlock (7) | Hex: curse the enemy you look at for 15 seconds: it takes 20% more damage from everything, with Weakness I and Slowness I. If it dies, the hex jumps to the nearest enemy within 6 blocks for the time left. Adds 20% Study. Scrollable. | Spellbolts: a spell of your own making. Three bolts at the enemy in front, 20 damage each, with Frost (slow 3 s), Lightning (stun 0.5 s) or Fire (burns 4 s). The element is random below 50% Study and the one it is weakest to from 50% on. With nothing in sight, Lightning flies where you aim. Scrollable. |
| 4 | Scrolls | Scrolls Professor (6) | Read Scroll: burn the oldest scroll in the pouch and cast its spell at once where you aim. If the pouch is empty nothing happens and nothing is spent. | Inscribe Scroll: write a scroll of the last Scrollable spell you cast. You pay its cost plus 25% now; later it fires at full strength for free. A full pouch drops its oldest scroll. |
| 5 | Stars | Constellations Master (5) | The Stars wheel: Starlight Cage, Stellar Self, Star Fall, Star Guard. Each has its own Sequence, cost and cooldown. | The last Stars pick again, without the wheel. |
| 6 | Retellings and Prophecy | Mysticologist (4) | The Retellings wheel: Sleeping Beauty, Avalon, Achilles' Heel, Pandora's Box, Foresight, Prophecy of Ruin. | The last pick again, without the wheel. |
| 7 | Knowledge | Sage (2) | The Knowledge wheel: Informatization, Erase, Knowledge Overload, Lance of Knowledge, Condense Knowledge, Embodiment of Knowledge, Indoctrination, Divine Kingdom. | The last pick again, without the wheel. |

### Key 5 wheel: Stars

| Entry | Sequence | Cost / cooldown | What it does |
| --- | --- | --- | --- |
| Starlight Cage | Constellations Master (5) | 3000 / 14 s | Starlight gathers around you, flies out and lands on the enemy you look at, then sets into a clear amber that holds it and everything within 3 blocks of it for 5 seconds (bosses 2). The amber breaks after 100 damage. Adds 15% Study. Scrollable. |
| Stellar Self | Constellations Master (5) | 2500 / 10 s | Your body breaks into starlight and flows through any gap, even a one block crack, to re-form where you aim, up to 16 blocks away. It takes a second, you cannot be hurt or targeted on the way, and the flash you leave behind blinds enemies within 4 blocks for 2 seconds. Solid walls stop it; fences, bars, windows and open doors do not. Not while held in place. |
| Star Fall | Constellations Master (5) | 3500 / 18 s | Five small stars fall one after another over 2 seconds around the spot you aim at (28 blocks at most), each bursting in 3 blocks for 30 damage. Faint rings show where they will land a second before. Enemies at 50% Study or more in the area are chosen first. Scrollable. |
| Star Guard | Constellations Master (5) | 2800 / 30 s | Four small stars orbit you for 30 seconds. Each one stops a projectile (and comes back 5 seconds later); an enemy that comes within 3 blocks is struck by one for 15 damage every 2 seconds. |

### Key 6 wheel: Retellings and Prophecy

The Mysticologist's Mystical Re-enactment is a menu of legends, so the wheel is that menu. Obscurity applies to the first four entries.

| Entry | Sequence | Cost / cooldown | What it does |
| --- | --- | --- | --- |
| Sleeping Beauty | Mysticologist (4) | 6000 / 40 s | You tell the tale and a golden spindle turns in the air: every enemy within 8 blocks falls asleep for 6 seconds (held, cannot act). Damage wakes the sleeper and that first hit does +50%. Bosses and players are only slowed hard for 3 seconds. Scrollable. |
| Avalon | Mysticologist (4) | 6000 / 40 s | The Isle of the Blessed rises as mist in a 10 block ring for 12 seconds. Allies inside regenerate about a heart a second and lose harmful effects; enemies inside are slowed and cannot heal. |
| Achilles' Heel | Mysticologist (4) | 5000 / 40 s | The tale of the single weak spot: the enemy you look at is at 100% Study for 8 seconds, takes 40% more damage, and your hits on it ignore armor. |
| Pandora's Box | Clairvoyant (3) | 9000 / 45 s | A black box opens before you: every enemy within 10 blocks suffers a different misfortune (blindness, slowness, weakness, poison, nausea or no healing) for 8 seconds. The last thing out is hope: you and allies within 10 blocks get Regeneration II for 8 seconds. Scrollable. |
| Foresight | Clairvoyant (3) | 5000 / 25 s | You glimpse the next moments. For 10 seconds enemy attacks show as pale ghost outlines where they will land, and the next 3 hits on you are dodged, each one sliding you 2 blocks aside. |
| Prophecy of Ruin | Clairvoyant (3) | 9000 / 45 s | You pronounce a fate on the enemy you look at (24 blocks): a 4 second countdown. At 0 it comes true: 80 damage that cannot be dodged or blocked, and the target is Understood (100% Study) for 10 seconds. Against a boss or a higher Sequence it is half as strong and 10% of the damage rebounds on you. If the target dies or gets out of reach, it fails. |

### Key 7 wheel: Knowledge

| Entry | Sequence | Cost / cooldown | What it does |
| --- | --- | --- | --- |
| Informatization | Sage (2) | 20000 / 30 s | For 6 seconds you turn into a stream of information: you cannot be hurt, targeted or held, you flow fast along the ground (Speed III), and a false copy of you stands where you were. Enemies attack it, and it bursts into text for 40 damage within 3 blocks. What hit you is absorbed, and 30% of it is released around you when you re-form. |
| Erase | Sage (2) | 15000 / 20 s | Deletion. Aimed at an enemy, it loses every beneficial effect and every kit status at once (Substitute, death wards, damage links, concealment, marionette strings). Aimed at a marionette, projection, decoy or other summoned thing, it is deleted outright. |
| Knowledge Overload | Knowledge Emperor (1) | 32000 / 40 s | Ancient symbols fill your eyes and you pour a flood of useless knowledge into the enemy you look at: its thoughts stop (held 2 seconds), then it takes 120 damage. At 100% Study a non-boss mind detonates: it dies and bursts for 60 damage within 4 blocks. Bosses and players are held only 1 second. |
| Lance of Knowledge | Knowledge Emperor (1) | 26000 / 25 s | Hurl a white spear condensed from pure knowledge. It flies 40 blocks and bursts in 5 blocks on impact: 200 damage at the center, half at the edge, a 1 second stun, and nausea for 30 seconds, because knowledge was forced into their minds. Damage grows with the Study of what it hits. |
| Condense Knowledge | Knowledge Emperor (1) | 10000 / 60 s | Clasp your hands: all the Study you built on enemies within 32 blocks gathers into a ball of illusory light and becomes yours. Each 100% restores 10% of your spirituality and gives Strength II for 10 seconds. With no Study around it works the other way: your next hit within 10 seconds counts as full Study. |
| Embodiment of Knowledge | Hermit (0) | 45000 / 60 s | You become your own information and hide it: for 15 seconds you cannot be seen, targeted, read, divined or Studied, not even by Erase or by Prying. No footprints, nothing remembers where you are. Casting any other ability ends it. |
| Indoctrination | Hermit (0) | 40000 / 50 s | A pulse of knowledge over 24 blocks. Hermit Beyonders among your allies get the positive kind: double spirituality regeneration and 30% shorter cooldowns for 30 seconds. Enemies get the negative kind: nausea, blindness and Weakness for 8 seconds, and their Study is set to 100%. |
| Divine Kingdom | Hermit (0) | 60000 / 180 s | The Knowledge Moor opens: a boundless library of orchid light covers 40 blocks around you for 60 seconds (cast again to end early). Inside it nothing hides (concealed enemies are forced out), enemy Study rises 5% a second, and every enemy ability shows as a ghost a second before it lands. You and your allies regenerate spirituality three times as fast, your abilities cost half and your scroll pouch is always full. |

## Melee combo

Hitting mobs as a Knowledge Emperor (registered in `combat/MeleeCombos` like the other Pathways). Three hits that repeat, each adding 5% Study:

1. A ring of drifting glyphs sweeping around you.
2. A tilted ring of star points, mirrored left or right each time.
3. Either a ground circle of constellation lines winding outward, or crossed page-edge slashes on the target with an eye that opens above it.

Colors: pale violet-white head (`0xF2F0E6FF`), orchid middle (`0xEEC070E8`), ink-indigo tail (`0xA02A1A5A`).

## Grows with the Sequence

These do not get their own key or entry. The same ability is stronger from the Sequence on.

| Ability | From | What changes |
| --- | --- | --- |
| Mystery Prying | Constellations Master (5) | Eyes of Secret Inquiry: reach 60 blocks, and everything alive within 30 blocks shows through walls. |
| Mystery Prying | Mysticologist (4) | Also sees through concealment, substitute figurines and marionette strings. |
| Mystery Prying | Clairvoyant (3) | Reach 80 blocks; also shows each enemy's Pathway, its Sequence and which abilities it has ready. |
| Scroll pouch | Mysticologist (4) | Holds 5 scrolls and adds a free one every 15 seconds. |
| Stellar Self | Clairvoyant (3) | Reach 40 blocks, and it carries allies within 3 blocks with you. |
| Spellbolts | Sage (2) | Become Knowledge Orbs: five homing orbs that each stun 0.5 seconds and briefly shut off spirit-draining abilities. |
| Starlight Cage | Sage (2) | Radius 5, and it holds bosses for the full time. |
| Foresight | Knowledge Emperor (1) | Lasts 20 seconds and dodges 6 hits. |
| Informatization | Knowledge Emperor (1) | Lasts 10 seconds and flows through any gap like Stellar Self. |
| Knowledge Overload | Hermit (0) | Hits every enemy within 16 blocks at once. |
| Lance of Knowledge | Hermit (0) | Three lances in a fan. |

## Passives

| Passive | From Sequence |
| --- | --- |
| Spiritual Intuition: a soft chime when something starts hunting you from out of sight; hidden things within 6 blocks show as faint outlines. Whisper of the Hidden Sage: about every 5 minutes a whisper arrives and your next ability costs nothing | Mystery Pryer (9) |
| Fighting Knowledge: slightly faster attacks and a little more reach with anything in your main hand; every melee hit adds 5% Study | Melee Scholar (8) |
| Warlock's Mind: harmful magic effects (hexes, curses, nausea, blindness) end on you 25% sooner | Warlock (7) |
| Starlight: at night under open sky, spirituality returns 25% faster | Constellations Master (5) |
| Information Skin: damage with no attacker (magic, explosions, abilities) is 25% less, and knockback 25% less | Mysticologist (4) |
| Clairaudience: a concealed enemy within 32 blocks that acts makes a chime from its direction | Clairvoyant (3) |
| Sage's Mind: nausea, blindness and confusion do not take, and nobody can build Study on you | Sage (2) |
| Law of Three (the symbolism of numbers): every third ability you cast within 20 seconds of the last is empowered, with +50% damage and no cost | Knowledge Emperor (1) |
| Embodiment: enemies within 24 blocks gain 2% Study a second just for being near you, and your Study on them decays half as fast | Hermit (0) |

## Planned: built from the existing kit

| Ability | Built from |
| --- | --- |
| Mystery Prying, Dowsing | `Spells.aimEnemy`, `Concealment.isHidden` (to find hidden things), `ModelFx` eyes and pendulum, `SpellFx.tether`, Study |
| Weak-Point Strike, Sidestep | `Spells.strike`, `Spells.push`, `Spells.freeSpotNear`, `Substitute.arm` (one hit), `MeleeCombo` |
| Hex, Spellbolts | `Spells.aimEnemy`, `Shot.of(...)` for the bolts, `ParticleShapes.burst`; Hex needs a timed mark (new `Marks`) |
| Scrolls | `Shot` for the rays, `Root.apply`, `ScreenImages` for the pouch, new `Scrolls` |
| Starlight Cage, Avalon, Divine Kingdom | `SpellFx.runeCircle`, `SpellFx.closingRing`, `Root.apply`, `Later.run`, new `Zone` |
| Stellar Self | `Concealment.hide` (short), `Root.has(TELEPORT)`, `Spells.fits`, `Spells.freeSpotNear` |
| Star Fall | `SpellFx.warningCircle`, `Later.run`, `Spells.enemiesAround`, `ParticleShapes.model` |
| Sleeping Beauty, Pandora's Box | `Root.apply`, `Spells.cleanse`, potion effects, new `Tellings` |
| Foresight | `Substitute.arm(player, ticks, 3, handler)`, ghost outlines with `ParticleShapes.model` |
| Prophecy of Ruin, Knowledge Overload | `Countdown.seconds(...)`, `SpellFx.tether`, `Spells.strike`, `Root.apply` |
| Lance of Knowledge | `Shot.of(...).onEnd` blast like airCannon, `SpellFx.shockRing` |
| Informatization | `Concealment.hide`, a decoy like paperFigurineDecoy, new `Invulnerable` |
| Erase | `Substitute`, `DeathWard`, `DamageLink`, `Concealment`, `Marionette`, through a new `Dispel` |
| Embodiment, Indoctrination | `Concealment.hide` with a "cannot be read" flag; `Spells.enemiesAround` plus an allies version |

## New reusable parts worth writing first

In this order. Most of them also help other Pathways.

1. `combat/Study`: get, add, bonus (the damage multiplier), understood, setFull, drainAround, decay on tick, and the eye model above the head (`KnowledgeEmperorFx.studyEye` already draws it). Nearly every ability touches it.
2. `Spells.strikeStudied(player, target, damage)`: `Spells.strike` plus the Study bonus plus the Study gain, so every ability calls one method.
3. `status/Scrolls`: the pouch and a registry of Scrollable spells. Write every spell's effect as `effect(player, sl, aim, power)` with no payment inside, the way the Attendant's `echo(...)` takes a runnable. Inscribe stores it, Read calls it, and Law of Three and Divine Kingdom reuse the same call.
4. `combat/Zone`: an area that lasts, with onEnter, onTick and onLeave. Avalon, the Cage and the Kingdom need it, and so do Hand Of God's Protection and Light of Dawn. (`combat/Zone` exists now: check it covers these before writing another.)
5. `combat/Tellings`: a use history with decay that returns a multiplier. It is the Obscurity rule, but works for any "less each time" effect.
6. `status/Dispel`: strips kit statuses and potion effects by kind. Erase uses it; Light of Dawn-style abilities could too.
7. `status/Invulnerable`: no damage and untargetable for N ticks, with an absorbed-damage counter (Informatization, Embodiment).
8. `combat/Orbit`: ModelFx bodies circling an entity that can intercept projectiles (Star Guard). Until it exists, `KnowledgeEmperorFx.starGuardTick` moves the stars round every tick.
9. `status/Marks` and `Spells.alliesAround`: a small timed mark on an entity (Hex), and the mirror of enemiesAround. (Both exist now.)

Build order: Study and strikeStudied, then keys 1 to 3, then Scrolls, then Zone, then the wheels (Tellings, Dispel, Invulnerable and Orbit as each entry needs them).

## To confirm before coding

- Should Hex and Study work on other players, or only on mobs?
- Costs for Sequences 2, 1 and 0 extend the Attendant's scale (250 to 12000); adjust them to your spirituality pools.
- Cooldowns are only given for wheel entries; keys 1 to 4 follow the Attendant and have none.
- Three wheels plus four single keys mirrors Hand Of God. Say if you want more or fewer wheels.

## Looks, sounds and pictures (ready)

Everything the abilities need to look and sound right is in `KnowledgeEmperorFx.java` (`beyonder_characteristics/knowledge_emperor/`). It only shows things; the ability does the damage, holding, hiding, healing and moving, at the ticks its constants name. `/knowledgefx all` plays every display in game, `/knowledgefx <name>` one of them (TAB lists the names; there are also `secret_inquiry`, `dowse_still`, `knowledge_orbs`, `tale_retold`, `deleted`, `mind_burst`, `overload_all`, `three_lances`, `condense_inverse`, `passives`, `combo` and `statuses`).

| Ability | Normal cast | Sneak cast |
| --- | --- | --- |
| Study (shared) | `studyEye(viewer, target, percent, ticks)`, only the Hermit sees it (shut below 34%, half open, wide open from 67%); `studyGain(viewer, target)` when it rises, `understood(viewer, target)` at 100%, `studyEyeHide(viewer, target)` | |
| 1 Mystery Prying | `pry(sl, player, reach, secretInquiry)` (`secretInquiry` = the Constellations Master form), `pryRead(viewer, target)` on the one looked at, `pryGlimpse(sl, hidden)` on each hidden thing (the glowing itself is the ability's) | Dowsing: `dowse(sl, player, target, ticks, tag)` (`target` null = it hangs still), `dowseEnd` |
| 2 Melee Scholar | `weakPointStrike(sl, player, from, target)` (lands at `STRIKE_LANDS`) | Sidestep: `sidestep(sl, player, ticks, tag)`, `sidestepSlip(sl, player, from, to)` when a blow is slipped, `sidestepEnd` |
| 3 Spells | Hex: `hex(sl, player, target, ticks, tag)` (lands at `HEX_LANDS`), `hexJump(sl, from, to, ticks, tag)` when the hexed dies, `hexEnd`; `hexed(...)` puts the mark on by itself | Spellbolts: `spellbolts(sl, player, targets, Element.X)` (bolt i lands at `BOLT_FLIGHT + i * BOLT_STEP`), `boltHit(sl, target, element)`; with nothing in sight `BOLT(element)` is the model for a `Shot`, with `boltTrail` and `boltBurst`. The Sage's Knowledge Orbs: `knowledgeOrbs(sl, player, targets)` (orb i lands at `ORB_FLIGHT + i * BOLT_STEP`), `orbHit` |
| 4 Scrolls | Read Scroll: `readScroll(sl, player, Scroll.X)` (the spell leaves at `READ_FIRES`), then `freezingRay(sl, target, ticks, tag)` (falls at `RAY_FALLS`) or `paralysisRay(sl, player, target, ticks, tag)` (hits at `RAY_HITS`); `frozen` and `paralyzed` on their own for the holds | Inscribe Scroll: `inscribe(sl, player)` (written at `INSCRIBED`), `scrollDropped(sl, player)` when a full pouch loses its oldest |
| 5 Stars (wheel) | Starlight Cage: `starlightCage(sl, player, target)` (lands at `CAGE_LANDS`), then for each one held `caged(sl, held, ticks, tag)`, `cageCrack` on a blow, `cageBreak` at 100 damage, `cageEnd`. Stellar Self: `stellarSelf(sl, player, from, to)` (whole at `STELLAR_ARRIVES`), `stellarCarry(sl, ally, from, to)` for each ally carried. Star Fall: `starFallSky(sl, center, ticks, tag)`, `starFall(sl, ground, radius)` per star (its warning ring shows for `STAR_LANDS`, it lands then), `starFallEnd`. Star Guard: `starGuard(sl, player, ticks, tag)`, `starGuardTick(sl, player, tag, age)` every tick, `starGuardBlock(sl, player, tag, index, at)`, `starGuardBack(sl, player, tag, index, ticksLeft, age)`, `starGuardStrike(sl, player, target)`, `starGuardEnd`. Icons `WHEEL_STARLIGHT_CAGE`, `WHEEL_STELLAR_SELF`, `WHEEL_STAR_FALL`, `WHEEL_STAR_GUARD` | the last pick again |
| 6 Retellings and Prophecy (wheel) | Every Retelling opens with `tale(sl, player, Tale.X, strength)` (`strength` = Obscurity's multiplier: a story told too often looks paler). Sleeping Beauty: `sleepingBeauty(sl, player, center, radius)` (asleep at `SPINDLE_SLEEPS`), `asleep(sl, sleeper, ticks, tag)`, `awaken(sl, sleeper, tag)`. Avalon: `avalon(sl, center, radius, ticks, tag)` (risen at `AVALON_RISEN`), `avalonTick` every second, `avalonBlessed(sl, ally)`, `avalonEnd`. Achilles' Heel: `achillesHeel(sl, player, target, ticks, tag)` (the arrow lands at `ARROW_LANDS`), `achillesEnd`. Pandora's Box: `pandorasBox(sl, player, radius)` (open at `BOX_OPENS`, hope at `HOPE_RISES`), `misfortune(sl, enemy, Misfortune.X, ticks, tag)`, `hope(sl, ally)`. Foresight: `foresight(sl, player, ticks, tag)`, `foresightGhost(sl, attacker, at)`, `foresightDodge(sl, player, from, to)`, `foresightEnd`. Prophecy of Ruin: `prophecy(sl, player, target, seconds, tag)`, `prophecyTick(sl, target, secondsLeft)` each second, then `prophecyFulfilled` or `prophecyFails`, and `prophecyRebound(sl, player)` against the stronger. Icons `WHEEL_SLEEPING_BEAUTY` ... `WHEEL_PROPHECY_OF_RUIN` | the last pick again |
| 7 Knowledge (wheel) | Informatization: `informatization(sl, player, ticks, tag)`, `informatizationTick` every few ticks, `decoy(sl, at, yaw, ticks, tag)` for the false copy, `decoyBurst(sl, at, tag)`, `informatizationEnd(sl, player, tag, released)`. Erase: `erase(sl, player, target)` (strikes at `ERASE_LANDS`), `deleted(sl, thing)` for a summon deleted outright. Knowledge Overload: `knowledgeOverload(sl, player, target, ticks, tag)` (the mind is held from `OVERLOAD_REACHES`), `overloadHit(sl, target, tag)` at the damage, `mindBurst(sl, target, radius)` when it detonates; the Hermit's `overloadAll(sl, player, targets, ticks, tag)`. Lance of Knowledge: `lanceForm(sl, player, count)` (thrown at `LANCE_THROWN`; count 3 = the Hermit's fan), `LANCE` for the `Shot`, `lanceTrail`, `lanceImpact(sl, at, radius)`. Condense Knowledge: `condenseKnowledge(sl, player, sources)` (taken in at `CONDENSED`), `condenseInverse(sl, player, ticks, tag)`. Embodiment: `embodiment(sl, player)`, `embodimentEnd(sl, player)`. Indoctrination: `indoctrination(sl, player, radius)`, `enlightened(sl, ally, ticks, tag)`, `indoctrinated(sl, enemy, ticks, tag)`. Divine Kingdom: `divineKingdom(sl, player, radius, ticks, tag)` (risen at `KINGDOM_RISEN`), `kingdomTick` every second, `kingdomForcedOut(sl, hidden)`, `kingdomGhost(sl, caster, at)`, `kingdomEnd(sl, center, radius, tag)`. Icons `WHEEL_INFORMATIZATION` ... `WHEEL_DIVINE_KINGDOM` | the last pick again |
| Passives | `intuition(hunted)` and `intuitionOutline(viewer, hidden)` (only the Hermit hears and sees them), `whisper(player)` (shows the Whisper status until the free cast is used: `statusEnd(player, Status.WHISPER)`), `clairaudience(viewer, at)`, `informationSkin`, `starlight` now and then at night, `lawOfThree(sl, player)` on an empowered third cast | |
| Melee | `combo(sl, player, target, step)` for hits 0, 1 and 2 | |

**The scroll pouch** is shown in the row beside the hotbar, where the Attendant's stacks are: `KnowledgeEmperorFx.pouch(player, scrolls)` whenever it changes (oldest first: the one Read Scroll burns next is on the left), `pouchHide(player)`. `scrollReady(sl, player, Scroll.X)` shows a free scroll arriving. The three pictures are `scroll_freezing`, `scroll_paralysis` and `scroll_inscribed`.

Every timed cast can show its picture and clock above the hotbar: `KnowledgeEmperorFx.status(player, Status.X, ticks)`, and `statusEnd` to take it away (24 statuses: 13 for what the Hermit holds, 11 for what is done to others when they are players). The Pathway's icon beside the hotbar is registered in `PathwayHudIcon` under `"Knowledge Emperor"`.

Things to know when the abilities are written:
- Star Guard's four stars do not follow the caster by themselves: a model's own keys cannot orbit a moving entity, so `starGuardTick` moves them with `ParticleShapes.moveModel` and must run every tick (the planned `combat/Orbit` can take that over).
- The Study eye is sent to one player only (`ParticleShapes.spawnFor`), so other players never see your Study. It lasts the ticks you give it: show it again every few seconds while the Study is above 0.
- The melee combo's step 1 alternates its lean on every call, shared by everyone; give it per-player state if two Emperors fighting side by side ever look odd.

What is in the repository, all of it generated by the scripts in `tools/model_gen/knowledge_emperor/` (see "Knowledge Emperor: ready-made displays" in [MODEL_FX.md](MODEL_FX.md)):

| What | Where | Made by |
| --- | --- | --- |
| 28 model files: the Study eye (3 steps), the dowsing pendulum, a rolled scroll, a small star, the Cage's amber, Sleeping Beauty's spindle, Pandora's box and its lid, the Lance of Knowledge, a ball of illusory light, a spell's bolt, the book of tales; and the pictures: the Hermit's eyes, a ring of glyphs, a circle of constellations, the Knowledge Moor's floor, a library shelf (animated), an unrolled scroll, the hex mark, the wheel of fate, a stream of information (animated), the erase sigil, Avalon's seal, the heel mark, a weak point and a column of light (animated) | `models/model_fx/knowledge_emperor/`, `textures/model_fx/knowledge_emperor/` | `models.py` (+ `textures.py`) |
| 18 wheel pictures (32 px), 24 status pictures and the 3 pouch scrolls (16 px), the Pathway icon | `textures/gui/ability_menus/knowledge_emperor/`, `textures/gui/knowledge_emperor/`, `textures/gui/pathway_icons/knowledge_emperor.png` | `icons.py` |
| 9 particle pictures: `fx/glyph_0` to `fx/glyph_3`, `fx/star5`, `fx/ink`, `fx/page`, `fx/quill`, `fx/text_line` | `textures/particle/fx/` | `particles.py` |
| 49 sounds (50 files): pry, dowse, the Sage's whisper, the intuition chime, weak point, sidestep, glyph swing, hex and the hex passing on, frost, lightning and fire bolts, scroll read, inscribed and ready, the freezing and paralysing rays, starlight gathering, amber setting and shattering, Stellar Self, a star falling and bursting, Star Guard and a shot turned aside, a tale told, the Sleeping Beauty lullaby and a sleeper waking, Avalon, Achilles' Heel, Pandora's Box, Foresight and a dodge, the prophecy, its countdown and ruin, Informatization and re-forming, the false copy bursting, Erase, Knowledge Overload and a mind bursting, the lance forming and striking, Condense Knowledge, Embodiment, Indoctrination, the Knowledge Moor and the Law of Three | `sounds/knowledge_emperor/`, `sounds.json`, subtitles in `lang/en_us.json`, registered in `sounds/ModSounds.java` (`KNOWLEDGE_...`) | `sounds.py` |
