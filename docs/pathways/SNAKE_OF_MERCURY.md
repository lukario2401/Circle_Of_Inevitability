# Snake Of Mercury

Current implementation and design reference for the Fate / Wheel of Fortune pathway.
Authority order: the player's latest requests, current code, then the older
`Snake of Mercury ability design.pdf`. This document supersedes that PDF.

All seven player keys and their ability menus are implemented. Select `Snake Of
Mercury` using `/sequence`. Silver/mercury `0xC9D3E0`, gold fortune, cold red
misfortune and violet chaos are the visual theme. Effects use existing rune, particle
and glow assets; bespoke serpent/coin/sky-wheel models remain visual polish.
Calamities never edit terrain, ignite real fires or destroy equipment.

## Controls, costs and cooldowns

Numbers are base values; material scaling is described below. Keys 5–7 open radial
menus and sneak quick-casts the last pick. Opening a menu costs nothing. Every pick
rechecks Sequence, pathway, spirituality and cooldown on the server.

| Key | Unlock | Normal | Sneak | Spirituality |
| --- | --- | --- | --- | --- |
| G / 1 | Monster, 9 | Danger Premonition | Spirit Vision | 10 |
| H / 2 | Robot, 8 | Calculation | Machine Mind | 200 |
| J / 3 | Lucky One, 7 | Bestow signed luck menu | Spin the Wheel | 1,250 / 250 |
| K / 4 | Calamity Priest, 6 | Calamity Redirection | Calamity Attraction | 2,000 |
| L / 5 | Winner, 5 | Misfortune menu | Last pick | Per entry |
| N / 6 | Chaoswalker, 3 | Chaos and Prophecy menu | Last pick | Per entry |
| M / 7 | Snake of Mercury, 1 | Cycle menu | Last pick | Per entry |

Keys 1, 2 and 4 have no fixed cooldown. Spin has six seconds at Sequences 7–5 and
**no cooldown from Sequence 4**. Its price and signed spending still apply. The
PDF's 250/600 costs for Sequences 9/8 were unaffordable with the current 20/400
maximum spirituality; the implemented 10/200 costs replace them.

## Signed luck reserve

Every living entity can hold persistent luck in **[-100, +100]**. Above zero is
lucky; below zero is unlucky; zero is neutral. Opposite additions cancel and the
result clamps to the cap. Luck has no timed expiry. It is separate from vanilla
Luck/Unluck, the material's Fortune strength, and the shared 0–10 Misfortune meter.

The next Wheel roll reads its owner's current reserve:

- Zero always rolls Gold and spends nothing.
- +L gives L% chance of a lucky material, otherwise Gold.
- -L gives L% chance of an unlucky material, otherwise Gold.
- Emerald/Diamond/Netherite spend 1/2/3 positive luck.
- Iron/Stone/Wood consume 1/2/3 negative luck, moving the reserve toward zero.
- Gold spends nothing. Spending uses up a fractional remainder without crossing zero.

A paid material stays active until the next roll even if spending left zero luck.
Changing luck does not rewrite the currently displayed material. Reading a valid
Wheel does not charge again. Initial, scheduled, Sequence-change and successful
native-cast rolls all use the same rules. Failed casts do not roll. Spin rolls
exactly once; Gambit exactly three times; free dismissals/swaps never roll.

Conditional lucky weights (normalize within each row):

| Sequence | Emerald | Diamond | Netherite |
| --- | --- | --- | --- |
| 9–8 | 10 | 4 | 1 |
| 7–4 | 20 | 8 | 2 |
| 3 | 35 | 15 | 5 |
| 2 | 40 | 22 | 8 |
| 1 | 35 | 30 | 15 |
| 0 | 0 | 35 | 65 |

Negative luck at **every** Sequence uses Iron 50%, Stone 30%, Wood 20%, conditional
on leaving Gold. Thus -25 gives 75% Gold, 12.5% Iron, 7.5% Stone and 5% Wood.
The old Sequence-only guarantees against bad outcomes are removed, including at
Sequence 0. Fate Authority must obey the sign too.

Living Snake players regenerate positive luck every five server ticks, including
while negative (paying down bad luck first). No offline generation occurs.

| Sequence | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Luck / second | 0.1 | 0.2 | 0.3 | 0.4 | 0.5 | 0.6 | 0.7 | 0.8 | 0.9 | 1.0 |

Recipients without a Wheel also benefit/suffer: positive reserves can dodge combat
hits (0.2% per point, capped at 20%), grant a 5% critical opportunity and improve an
existing ordinary mob-drop stack (0.2% per point, capped at 20%). Each successful
proc spends up to one positive point. No equipment, unique component-bearing stack
or new item type is duplicated. Negative reserves have a 0.2%-per-point chance each
second to cause a short stumble, consuming up to one negative point. These are
separate from the Misfortune scheduler; accidents do not recursively create luck.
Blessing and Winner's Edge replace the base crit chance rather than adding another
crit roll. Luck procs are never applied to void, `/kill` or piercing damage.

## Material Wheel

The HUD shows material, Fortune strength, time to the next roll, signed luck and
rounded next-roll bias. The Wheel rolls every eight seconds and after successful
native abilities. Cast geometry/duration is taken before the post-cast roll;
ongoing combat and spirituality regeneration use the displayed material.

| Tier | Fortune strength | Damage dealt | Range / AoE / duration | Damage taken | Spirituality regeneration |
| --- | --- | --- | --- | --- | --- |
| Netherite | 150% | 3× | 2× | 0.5× | 2× |
| Diamond | 125% | 2× | 1.75× | 1/1.5× | 1.75× |
| Emerald | 100% | 1.5× | 1.5× | 1/1.25× | 1.5× |
| Gold | 100% | 1× | 1× | 1× | 1× |
| Iron | 75% | 0.75× | 0.75× | 1/0.75× | 0.75× |
| Stone | 50% | 0.5× | 0.5× | 2× | 0.5× |
| Wood | 25% | 0.25× | 0.25× | 4× | 0.25× |

Fortune strength scales Snake-applied Misfortune with unbiased fractional rounding,
not roll odds. Diamond/Netherite also grant vanilla Luck I/II. Iron/Stone/Wood grant
Unluck I/II/III; Emerald and above grant Speed/Haste, Netherite Fire Resistance,
Stone/Wood Slowness. Short utility effects preserve longer/stronger outside effects.
Material damage/defense is applied directly; no material Strength/Resistance is added.
The separate Robot and serpent buffs are pathway abilities.

Netherite prevents one lethal hit, restores half maximum health and grants two
seconds' grace, consuming the tier to Gold until the next roll. DeathWard has
priority; void, `/kill`, piercing attacks and resurrection-prohibiting Decrees
remain excluded. The PDF's three Omens, refunds and red-Omen stumble are obsolete.

## Low and middle Sequences

**Premonition (9):** avoid one combat hit for eight base seconds, three hits from
Sequence 3. Try a collision-checked two-block sidestep, respecting teleport roots;
the attacker stumbles and gains one base Misfortune. Visible mobs targeting the
caster flash red. Ordinary environmental damage is excluded; combat explosions
already use the shared Substitute rules.

**Spirit Vision (9):** 15 base seconds of living-entity glow in 32 blocks, 64 from
Sequence 2. Positive luck produces gold motes, negative red, zero silver. Aimed
luck and the independent Misfortune level appear in the HUD. World glow/particles
are visible to nearby players and use the existing reveal mechanism.

**Calculation (8):** read the aimed enemy for 12 seconds and mark the direction it
is facing. Each hit against the read target grants another 10% damage, capped at
+50%. Native recasting replaces the read; Fated Connection echoes may read up to
three extra targets at half bonus/duration. **Machine Mind:** eight seconds of
cleansing/immunity to nausea, blindness, slowness, darkness and Fear, plus knockback
immunity. There is no distinct confusion status in this codebase.

**Bestow Luck (7):** aim at a living entity and choose +5/+10/+20 or -5/-10/-20
explicitly. Ally/enemy status never selects or reverses the
sign. Only the caster's existing matching-sign reserve is transferable. Only what
the recipient accepts is debited, including capacity limits and Fate Resistance.
Self/no-target transfer is refused without payment. At Sequence 2, optional ±20
shared casts split one budget among up to 16 same-allegiance recipients within
eight blocks of the primary target. Positive gifts to enemies and negative gifts
to allies are valid. **Spin:** immediately replace the material using the reserve;
six-second cooldown before Sequence 4, none thereafter.

**Calamity Redirection (6):** six seconds of deferring half incoming damage and up
to 32 harmful effect applications in a ledger. On expiry, return the recorded
amount ×1.5 to the enemy responsible for the largest share, ×2 from Sequence 4,
and pass on the recorded effects. An already open ledger cannot be overwritten.
If its attacker leaves/dies, no replacement target is fabricated.

**Calamity Attraction (6):** for 15 seconds, enemies within ten base blocks gain one
base Misfortune each second. A quarter of their actual mishap damage becomes
absorption, capped at the caster's maximum health. Activation adds -10 luck once;
there is no forced 50/50 Wheel distribution and no repeated per-enemy reserve cost.

## Misfortune menu

| Entry | Sequence | Cost | Cooldown |
| --- | --- | --- | --- |
| Misfortune Bestowal | 5 | 3,000 | 5 seconds |
| Winner's Gambit | 5 | 3,000 | 15 seconds |
| Misfortune Chain | 4 | 6,000 | 8 seconds |
| Silver Serpent Form | 4 | 6,000 | None; free dismissal |

**Bestowal:** aimed enemy within 24 blocks receives four base Misfortune levels,
two for bosses, plus an immediate mishap attempt respecting immunity/recovery.
Transfer up to ten existing negative luck points too, if available; the main spell
still works without a negative reserve.

**Gambit:** stake 20% current health without dying. Roll three times, spending the
updated reserve each time. Emerald and above count as lucky. The first lucky stop
heals twice the stake; the second bursts for 30 base damage in six blocks; the
third cuts cooldowns by ten seconds. Three lucky stops are a jackpot: reset paid
ability cooldowns, including Gambit, and add +10 luck once. Gold/unlucky stops add
no further health cost. There is no fourth post-cast roll.

**Chain:** up to five targets, jumping within eight blocks, for 20/25/30/35/40 base
damage and one base Misfortune per link. From Sequence 2: eight links, through walls,
continuing the +5 damage progression. Jumps have bounded delays and never repeat a
target; death, removal or dimension change of the caster cancels later jumps.

**Serpent:** 40 base seconds of silver coils, Speed II and Resistance I. Melee hits
root for one second, once per three seconds per victim. At Sequences 4–3, add -1
luck every ten seconds in form. Sequence 2 completes the form, gives Speed III /
Resistance II and removes that negative addition. Sequence 1 stays transformed
until dismissed. The visual is currently a moving particle coil; a bespoke long
serpent model and a three-roll probability forecast are not implemented.

## Chaos and Prophecy menu

| Entry | Sequence | Cost | Cooldown |
| --- | --- | --- | --- |
| Chaos Walk | 3 | 9,000 | 6 seconds |
| Aura of Chaos | 3 | 9,000 | 12 seconds |
| Words of Fortune and Misfortune | 2 | 12,000 | 20 seconds |
| Fate Concealment | 2 | 12,000 | 8 seconds |
| Arranged Opportunity | 2 | 12,000 | 20 seconds |

**Walk:** six seconds of Speed II, concealment/unnoticed movement and one projectile
avoidance. Leave eight-second patches within three blocks; overlapping patches from
one caster add at most one base Misfortune per victim per second. Cap patches at 32
per caster. Attacking follows the shared concealment break rules.

**Aura:** ten-block aura for 12 seconds, adding two base Misfortune per second to
non-boss enemies. Falter gives 25% attack misfire; a separate one-second, 25% silence
pulse interrupts ability use. This is an interruption pulse, not a generic hook that
randomly cancels every individual ability invocation.

**Words:** the menu opens a second explicit Bless/Curse choice; opening/cancelling it
is free. Bless any aimed living target or self with +30 luck and 60 base seconds of
one-in-three dodge / 20% crit opportunities, spending positive reserve on success.
Curse adds -30 luck, ten base Misfortune and repeated severe mishap attempts for ten
base seconds. Boss curse strength/duration is halved. Explicit curses can affect
allies; ordinary automatic Misfortune retains friendly protection. Signed additions
first cancel the target's opposite reserve. No guaranteed lucky Wheel outcome.

**Concealment:** hide self or an aimed ally for eight seconds and arm three avoided
combat hits, respecting Decrees and normal concealment break rules. An echo does
not overwrite an already armed substitute with a shorter one.

**Opportunity:** mark an eight-block area for 20 seconds. Its first hostile attack
against anyone inside is turned aside, the attacker receives four base Misfortune
and a severe available mishap attempt, allies lose eight seconds of cooldown, and
the caster gains +5 luck once. Consume the trigger before retaliation to prevent
recursive rewards. One active opportunity per caster.

## Cycle menu

| Entry | Sequence | Cost | Cooldown |
| --- | --- | --- | --- |
| Reboot | 1 | 20,000 | 120 seconds |
| Fate Loop | 1 | 20,000 | 20 seconds |
| Fated Connection | 1 | 20,000 | None; live-coin swaps free |
| Reincarnation | 1 | 20,000 | 15 minutes |
| The Wheel of Fortune | 0 | 40,000 | 5 minutes; free early stop |
| Fate Authority | 0 | Free | None |

**Reboot:** restore living entities and existing projectiles within 24 base blocks
to eight seconds ago. Sequence 0: 48 blocks and 20 seconds. Restore position, motion,
health and potion effects; never recreate dead entities, blocks or projectiles.
Skip missing history, blocked destinations, unloaded chunks, borders, mounts and
teleport-rooted entities. Luck, spirituality, inventory, cooldowns and ability-state
resources are never restored. A cast with no restorable entity is refused freely.

**Loop:** a named, destructible broken-wheel stand anchors a ten-block area for 15
base seconds. Every three seconds from creation, hostile non-boss entities return
to their position/health/effects three seconds earlier. Caster/allies are exempt;
Sequence 0 includes bosses. The medium has 40 health, 80 at Sequence 0. Melee
subtracts charged attack strength; projectile impacts subtract a bounded 2–20 points
based on speed. The stand is protected from vanilla instant break/drop behavior.
There is one loop per caster. Destroying/removing its medium, caster logout/death,
Sequence/pathway loss or level unload releases it. Existing mob AI is retained.

**Connection:** place a gold-marked coin anchor on a living entity or aimed ground;
three anchors maximum, 60 base seconds. Aim at a live coin and recast to swap,
respecting roots and safe space, without paying or rolling. Ground anchors move to
the old caster position; entity anchors follow their entity. Native effects with a
useful origin currently echo targeted Calculation, Misfortune Chain and Fate
Concealment from the anchors at half strength/duration. Self-only buffs, signed
transfers, Gambit, resource awards, timeline operations and capstones do not echo.
Echoes are marked copies so they cannot recurse or charge/reroll the caster again.

**Reincarnation:** arm for ten minutes; lethal damage restores half maximum health,
tries to return to the position from ten seconds ago, clears effects/fire and hides
for three seconds. Add +10 luck and roll once using the resulting reserve. Uses the
shared DeathWard exclusions and priority; no forced positive result and no duplicate
ward. Insufficient/unsafe history skips teleport without cancelling revival.

**Sky Wheel:** a large overhead rune lasts 30 base seconds. Every four seconds from
activation, allies/self within 30 base blocks gain +10 luck and heal 15% maximum
health; enemies gain -10 luck and a severe available mishap. Explicit capstone luck
additions respect caps and Fate Resistance. Recasting stops it for free.

**Authority:** select any material on the side allowed by the current reserve, or
Gold. Zero permits only Gold. Non-neutral selection spends its ordinary 1–3 points
and starts an eight-second display period. Menu choices recheck current sign, so a
stale open menu cannot force a contradictory material.

## Passives and shared calamities

- Monster: hidden-threat chime, +5% spirituality regeneration and the signed passive
  luck generation above.
- Robot: Strength I and modest speed, preserving longer/stronger effects.
- Winner: 10% crit opportunity while positive, spending one point on success.
- Misfortune Mage: one base Misfortune per five seconds to enemies within six blocks.
- Chaoswalker: three Premonition charges and Ripple of Misfortune when actually hurt
  (one level on the nearest enemy within six blocks). Negative luck still works.
- Soothsayer: Body of Mercury avoids a hit exceeding one third maximum health once
  per 90 seconds, grants five seconds of untouchability, then +10 luck. Its cooldown
  survives logout; existing resurrection/piercing exclusions remain.
- Snake: bounded nearby timeline recording; externally bestowed positive/negative
  luck is halved. Conserved transfers debit only the amount actually accepted.
- Wheel of Fortune: Authority and the stronger cycle effects above.

Misfortune caps at 10. Snake buildup starts decaying after five seconds without
reapplication, losing one level every three seconds. Accidents do not add levels.
At levels 1–6: 5% chance per level each second, with three-second recovery. Mob
levels 7/8/9/10 use 60/70/80/90% chance and 2.5/2/1.5/1-second recovery; severe
results are preferred. Players retain the mild table and capped recovery/damage.

Mishaps: stumble, weakened strike, projectile fizzle, falling stone, calamity,
gravity crush, withering, lightning, meteor storm, seismic burst and Doom.
High-level mob damage scales with level/health and caps at 160 before caster and
defense multipliers. On a 100-health mob, level-10 calamity is 62 base damage, or
93 for catastrophe/Doom. Meteor storm delivers three 32-damage impacts. Player
calamity retains 10/16 damage caps. Shared immunity and forced-attempt recovery are
respected. Seismic secondary damage remains enemy-mob-only.

Melee combo visuals use silver, red/violet and a cosmetic gold/red coin-flip rune;
cosmetic randomness never consumes luck.

## State, bounds and validation

`status/Luck` is the server-owned signed-reserve API. `LuckBalance` contains pure
balance/transfer math, `FateOdds` the sign-aware distribution. `FateTimeline` stores
only combat snapshots, every five ticks, for at most 512 entities per dimension and
440 ticks of history. History is recorded near high-Sequence Snake players and active
loops; absent history is never fabricated. Dimension, death and logout cleanup
release temporary ledgers, reads, areas, loops and anchors. Active buffs are not
promised to survive a restart. The signed reserve and paid cooldown timestamps are
saved normally; there is no additional luck carry-over rule on player death.

Implemented classes are `SnakeOfMercury`, `SnakeOfMercuryPowers`,
`SnakeOfMercuryEvents`, `SnakeOfMercuryCycle`, the Fate classes and shared statuses.
All seven keys are listed in `PathwayAbilities` and `docs/SEQUENCES.md`.

Run with Java 25:

```sh
./gradlew build --offline
./gradlew runGameTestServer --offline
javac -d /tmp/wheel-tests src/main/java/net/lukario/circleofinevitability/fate/{FateOdds,WheelTier,DisasterBalance,LuckBalance}.java tools/tests/FateOddsTest.java
java -cp /tmp/wheel-tests FateOddsTest
```

The headless tests cover signed transfer/caps/resistance, Spin cooldown progression,
resource-safe rewinds, free form dismissal, Machine Mind effect immunity, medium
cleanup, ledger damage/effect deferral, Reincarnation, failed payment and sky-pulse timing. Standalone checks cover one million signed
rolls, material weights, spending, generation, transfer conservation and disaster
bounds. A client playthrough is still needed for visual layout, aiming feel and
multiplayer timing/balance; automated tests do not verify rendering.
