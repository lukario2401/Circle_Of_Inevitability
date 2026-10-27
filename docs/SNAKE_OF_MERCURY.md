# Snake Of Mercury

Living design and implementation reference for the Fate / Wheel of Fortune pathway.
Source priority: the player's latest instructions, current code, then the older
`Snake of Mercury ability design.pdf`. This document supersedes the PDF. Proposed
abilities below are design only unless explicitly marked implemented.

Theme: premonition, calculation, good and bad luck, calamity, prophecy and cycles of
fate. Chrome silver/mercury `0xC9D3E0`, gold fortune, cold red misfortune and violet-black
chaos. Calamities never alter blocks, ignite real fires or destroy equipment; luck
nudges existing outcomes rather than creating items.

## Implemented scope

Select `Snake Of Mercury` using `/sequence` at Sequence 9 or stronger. G casts Danger
Premonition; sneak-G casts Spirit Vision. Both cost 250 spirituality. Like Worm Of
Time's regular keys, neither has a fixed cooldown: recasting replaces its duration
rather than accumulating dodge charges. Future menu wheels still own their entry cooldowns.

## The material Wheel

The hotbar shows the current material tier, Fortune strength and seconds until its
next roll. The tier remains active until the next roll: automatically every eight
seconds or immediately after a successful native cast. Payment failure leaves it
unchanged. A native cast snapshots its tier's duration/range/area before rolling
again. Combat damage and passive regeneration use the currently displayed tier.
Changing Sequence refreshes the distribution immediately. Old three-Omen saves
initialize a material tier on first use.

| Tier | Fortune | Damage dealt | Range / AoE / duration | Damage taken | Spirituality regeneration |
| --- | --- | --- | --- | --- | --- |
| Netherite | 150% | 3× | 2× | 0.5× | 2× |
| Diamond | 125% | 2× | 1.75× | 1/1.5× | 1.75× |
| Emerald | 100% | 1.5× | 1.5× | 1/1.25× | 1.5× |
| Gold | 100% | 1× | 1× | 1× | 1× |
| Iron | 75% | 0.75× | 0.75× | 1/0.75× | 0.75× |
| Stone | 50% | 0.5× | 0.5× | 2× | 0.5× |
| Wood | 25% | 0.25× | 0.25× | 4× | 0.25× |

The table's Fortune percentage is effect strength, separate from the signed luck reserve and roll probability. It scales
Snake's applied misfortune buildup with unbiased fractional rounding: a one-level
application at 150% gives one or two levels with equal chances; at 25% it gives one
level one quarter of the time. Diamond/Netherite also grant Luck I/II; Iron/Stone/Wood
grant Unluck I/II/III. Emerald and above grant Speed/Haste (II for Netherite).
Netherite grants Fire Resistance; Stone/Wood grant Slowness I/II. These short utility
effects fade within half a second of a tier change; existing stronger/longer potion
effects are preserved. Damage is scaled directly, without stacking Strength or
Resistance on top. The old Fortune spirituality refund and red-Omen stumble are replaced.

Damage scaling covers melee, owned projectiles, `Spells.strike` and anonymous ability
damage explicitly attributed with `DamageFx.credit`. It does not use the extended
visual-only burn/poison credit to guess who caused unrelated damage. Range, area and
duration scaling is wired into the implemented Snake Key 1: vision radius and target
range, the sidestep and the danger-warning radius. Snake-applied curse lifetime, new
mishap debuff durations and seismic splash radius also scale with the tier. Other
pathways' geometry is unchanged.

Netherite prevents one lethal hit, restores half maximum health, clears fire and
provides two seconds of grace. This consumes the tier, setting it to Gold until the
scheduled next roll. An existing DeathWard gets first use; vanilla totems retain their
normal behavior. Void, `/kill`, piercing attacks and resurrection-prohibiting Decrees
are not bypassed. Revival runs before ordinary death listeners.

### Signed luck balance (implemented)

Every living entity can hold a persistent balance from **-100 to +100**. Positive
means lucky; negative means unlucky; zero is neutral. Additions of opposite signs
cancel before the result is clamped. This is separate from vanilla Luck/Unluck,
material Fortune strength, spirituality and the 0–10 Misfortune buildup meter.
Balances have no timed expiry and are saved in entity persistent data; there is no
new death/respawn carry-over rule or offline generation.

Each automatic, initial, Sequence-change or successful native-cast Wheel roll reads
the owner's current balance:

- At zero, stop on Gold: no luck modifier and no reserve spending.
- At +L, there is L% chance of a lucky tier, otherwise Gold.
- At -L, there is L% chance of an unlucky tier, otherwise Gold.
- Lucky stops consume positive luck: Emerald 1, Diamond 2, Netherite 3.
- Unlucky stops consume negative luck: Iron 1, Stone 2, Wood 3, moving toward zero.
- Gold consumes nothing. Spending cannot cross zero; a fractional remainder can be
  spent completely. There is no second charge for an already displayed tier or its
  combat effects. Reading a still-valid Wheel does not spend again.

The balance influences **the next roll**, not an already active material. A zero
balance after spending does not cancel the paid tier; it lasts until the next roll.
Existing saves without a balance begin at zero; their displayed tier lasts until
its next ordinary roll. Native casts use their snapshotted material first, then roll.
Failed payment and copied casts do not roll or spend the owner's reserve.

Conditional weights once a lucky roll succeeds (normalized within each row):

| Sequence | Emerald | Diamond | Netherite |
| --- | --- | --- | --- |
| 9–8 | 10 | 4 | 1 |
| 7–4 | 20 | 8 | 2 |
| 3 | 35 | 15 | 5 |
| 2 | 40 | 22 | 8 |
| 1 | 35 | 30 | 15 |
| 0 | 0 | 35 | 65 |

All negative balances, at every Sequence, use conditional weights Iron 50%, Stone
30%, Wood 20%. Higher Sequences cannot ignore negative reserves: the latest signed
luck rule replaces the PDF's guaranteed removal of bad outcomes and the code's old
Sequence-only roll distributions. Sequence 0 also needs positive luck for good rolls.
Example: +25 gives 75% Gold and 25% shared among the lucky materials; -25 gives 75%
Gold, 12.5% Iron, 7.5% Stone, 5% Wood. At either ±100 there is no neutral roll.

Living Snake players generate positive luck every five server ticks. This continues
while negative, paying down bad luck first, and caps at +100. It is independent of
the current Wheel tier and stops when dead or outside this pathway.

| Sequence | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Luck / second | 0.1 | 0.2 | 0.3 | 0.4 | 0.5 | 0.6 | 0.7 | 0.8 | 0.9 | 1.0 |

These caps, costs and rates are starting balance numbers. The HUD shows the signed
reserve to one decimal place and its absolute next-roll bias as a rounded percentage.
Spirit Vision colors positive balances gold, negative red, zero silver and shows the
aimed enemy's signed luck alongside its separate Misfortune buildup. Readouts never
roll another entity's Wheel merely to inspect its luck.

Reusable code: `status/Luck.balance`, `set`, `add` and `spendRoll`; `LuckBalance` owns
pure reserve math and generation rates, while `FateOdds` owns roll selection. The API
accepts any living target, including enemies. Bestowal targeting and combat/drop
hooks on recipients without a Wheel remain future work; storing luck on such an
entity does not currently create dodge, crit, loot or mishap effects.

## Key 1

Danger Premonition arms one avoided combat hit for eight base seconds (three from
Sequence 3). A visible mob targeting you flickers red. On a hit, a collision-checked
sidestep attempts to move you two base blocks; teleport restrictions are respected.
The attacker stumbles and gains one base shared Misfortune level, modified by Fortune.
Environmental damage still follows Substitute rules: ordinary falls, lava and drowning
are not avoided.

Spirit Vision lasts 15 base seconds, revealing nearby living entities with the existing
Eye of Demon Hunting glowing-effect mechanism. Its base area is 32 blocks, 64 from
Sequence 2, multiplied by the cast tier's AoE. Silver/red motes indicate ordinary/high
misfortune; above-100% Fortune players have gold motes. These world effects are visible
to other nearby players. Aimed enemy misfortune appears beside the hotbar, including
zero, and clears when you look away. Borrowed Spirit Vision also reads it for its duration.

## Violent misfortune

The shared Misfortune cap is 10. Snake-applied buildup starts decaying after five
seconds without reapplication, losing one level every three seconds. Accidents do
not add buildup themselves. Existing curse API, immunity, half-level and visual hooks
remain available; these changes also affect other pathways using shared Misfortune.

At levels 1–6, rolls remain 5% per level each second, with three seconds of recovery.
Mobs at levels 7/8/9/10 roll with 60/70/80/90% chances and 2.5/2/1.5/1-second recovery;
75% of their rolls select from the severe effects available at that level. Players
retain the former capped frequency and mild random table.

- Stumble (1), weakened next strike without equipment loss (2), harmless projectile fizzle (3).
- Falling stone (4), calamity (5).
- Gravity crush: heavy impact, movement lock and Slowness IV (6); bosses have shorter locks.
- Withering: Wither II/III and Weakness II (6).
- Lightning: thunder, a tall spark bolt, damage and interrupted attacks (7).
- Meteor storm: three explosions twelve ticks apart; cancelled if the target dies or leaves (8).
- Seismic burst: damaging shockwave and launch on nearby enemy mobs (9).
- Doom: catastrophic impact, Wither III and attack interruption (10).

High-level mob calamity damage scales with buildup and maximum health, capped at
160 before caster multipliers/defenses. On a 100-health mob, level-10 calamity deals
62 base damage, or 93 for catastrophe/Doom. Meteor storm delivers three 32-damage
impacts at level 10. Player calamity retains its previous 10/16 damage caps. No real
explosions, fires, lightning entities, block changes or equipment destruction are
used. Secondary seismic damage is restricted to enemy mobs, with normal damage
invulnerability respected. Sounds, particles and delayed impacts have fixed bounds.

The Wheel, signed luck reserve, passive luck generation, counters, Key 1 and shared
Misfortune improvements are implemented. All ability designs below remain future
work, including the Key 3 bestowal/spin controls.

## Validation

Run `./gradlew build` with Java 25 and the environment's HTTPS proxy/trust configuration.
Run the dependency-free behavioral checks separately:

```sh
javac -d /tmp/wheel-tests src/main/java/net/lukario/circleofinevitability/fate/{FateOdds,WheelTier,DisasterBalance,LuckBalance}.java tools/tests/FateOddsTest.java
java -cp /tmp/wheel-tests FateOddsTest
```

In a development game, check each material's damage/defense, vision area, regeneration
and status expiry; Netherite revival consumption and its exclusions; aimed-counter
updates and severe enemy-only mishaps. These runtime checks require a Minecraft game;
the standalone checks validate signed roll probabilities, neutrality, spending, generation
progression, multipliers and disaster growth/bounds.


## Full ability plan (not yet implemented)

All durations, ranges and damage below are base values before the material table.
The PDF's three Omens, fixed Sequence odds, automatic spirituality refunds, stumble
penalty, luck drift and self-snowballing accident buildup are obsolete. Keep current
code's material Wheel, Key 1 behavior and shared calamity rules above.

### Sequence progression and controls

Seven keys use the existing G/H/J/K/L/N/M ability layout. Keys 1–4 pair a normal and
sneak cast. Keys 5–7 open `AbilityWheel` menus; sneak quick-casts the last selection.
Each future menu entry owns its Sequence requirement, spirituality cost and cooldown.
Do not add a blanket cooldown to the implemented Key 1.

| Sequence | Title | Unlocks |
| --- | --- | --- |
| 9 | Monster | Danger Premonition, Spirit Vision |
| 8 | Robot | Calculation, Machine Mind |
| 7 | Lucky One | Bestow Luck, Spin the Wheel |
| 6 | Calamity Priest | Calamity Redirection, Calamity Attraction |
| 5 | Winner | Misfortune Bestowal, Winner's Gambit |
| 4 | Misfortune Mage | Misfortune Chain, Silver Serpent Form; Spin cooldown removed |
| 3 | Chaoswalker | Chaos Walk, Aura of Chaos |
| 2 | Soothsayer | Words of Fortune and Misfortune, Fate Concealment, Arranged Opportunity |
| 1 | Snake of Mercury | Reboot, Fate Loop, Fated Connection, Reincarnation |
| 0 | Wheel of Fortune | The Wheel of Fortune |

Starting spirituality costs by unlock Sequence: 9: 250; 8: 600; 7: 1,250; 6: 2,000;
5: 3,000; 4: 6,000; 3: 9,000; 2: 12,000; 1: 20,000; 0: 40,000. These are proposal
values for unimplemented abilities, to tune against SoulCore. Spin costs one fifth
of its Sequence-7 base cost (250). Explicit capstone cooldowns are given below.

### Key 2: Calculation (Sequence 8)

Normal: read the aimed enemy for 12 seconds, telegraphing its next attack. Each hit
you land adds 10% damage against that target, up to +50%. Sneak: Machine Mind for
eight seconds, clearing and resisting nausea, blindness, slowness, confusion, fear
and hit flinch. These abilities do not need an extra luck charge: the Wheel already
scales native casts and rolls afterward.

### Key 3: Luck (Sequence 7)

**Bestow Luck:** explicitly choose a positive or negative signed amount, and a living
target (self, ally or enemy). Hostility must never choose or reverse the sign. Use
an aimed-living-entity selector rather than enemy-only or friend-only targeting;
no target defaults to self. The eventual sign/amount selector must expose the signed
transfer before casting, rather than reusing sneak, which is already Spin.

Transfer up to 20 points of the caster's existing matching-sign reserve. A caster
with +30 can send +20 and retain +10; a caster with -30 can send -20 and retain -10.
The caster cannot manufacture either sign. Opposite balances cancel on the target:
sending -10 to +6 produces -4. Accept only the amount that fits the target's ±100
cap; reject a zero transfer without charging spirituality. Self-targeting is a
no-transfer inspection/no-op, not a way to duplicate luck. Bestowing positive luck
on an enemy is valid, as is passing one's bad luck to them. Future positive-luck
dodge/crit/drop hooks must spend the recipient's reserve when they actually trigger;
negative mishaps must consume negative reserve too. Do not keep the PDF's independent
30-second Fortune buff alongside this transfer.

At Sequence 2, optional multi-target bestowal reaches chosen living recipients within
eight blocks of the primary target. Split one total transfer budget among recipients;
do not clone the full reserve per target. Explicit selection still controls sign and
recipients, including enemies. Signed balance itself has no 60-second expiry.

**Spin the Wheel:** immediately replace the shown material using the signed balance,
with the same spending rules as every other roll. Six-second cooldown at Sequences
7–5; **no cooldown at Sequence 4 or stronger**. Spirituality and reserve rules still
apply. Spin produces exactly one roll: do not also run the generic post-cast reroll.
A neutral reserve produces Gold. It cannot randomly create good luck from zero.

### Key 4: Calamity (Sequence 6)

**Calamity Redirection:** for six seconds record harmful effects and half of incoming
hit damage in a ledger. On expiry, return the recorded total at ×1.5 to the enemy
responsible for the largest damage share; ×2 from Sequence 4. Keep damage accounting
separate from reserve transfer; no arbitrary luck conversion is needed.

**Calamity Attraction:** for 15 seconds enemies within ten blocks gain one base
Misfortune per second. A quarter of their mishap damage returns as absorption.
Replace the PDF's forced 50/50 bad/neutral Wheel with a one-time -10 reserve addition
to the caster on activation. This can cancel positive luck or create a negative
balance; subsequent rolls obey and consume it normally. Mark this as a fixed spell
side effect, not a repeated cost per enemy or tick.

### Key 5 wheel: Misfortune

- **Misfortune Bestowal (5):** aimed enemy within 24 blocks receives four base
  Misfortune levels, two for bosses above 150 maximum health, and an immediate mishap
  subject to immunity/recovery rules. Separately transfer up to ten existing negative
  luck points from caster to target, if available and accepted. The spell still works
  without negative reserves; it never creates a transfer from an empty reserve.
- **Winner's Gambit (5):** stake 20% current health without dying. Resolve three actual
  material rolls in sequence, each using and spending the updated reserve. Emerald,
  Diamond or Netherite count as lucky; Gold is neutral; Iron/Stone/Wood are unlucky.
  Successive lucky stops heal twice the stake, burst for 30 base damage in six blocks,
  then reduce cooldowns by ten seconds. Three lucky stops reset cooldowns and add +10
  luck once. Unlucky stops lose the stake without additional health penalties. No
  fourth generic post-cast roll; display the final result. The three rolls cannot
  repeatedly spend the original pre-Gambit balance.
- **Misfortune Chain (4):** an accident jumps to the nearest enemy within eight blocks,
  up to five links for 20/25/30/35/40 base damage. Each adds one base Misfortune using
  current Fortune scaling. From Sequence 2: eight links and jumps through walls.
  Keep reserve separate; no extra transfer per chain link.
- **Silver Serpent Form (4):** a half-formed scaleless silver serpent of connected
  wheels coils around the player for 40 seconds, granting Speed II and Resistance I.
  Melee coils root for one second, once per three seconds per target. Every ten
  seconds, add -1 luck to the caster rather than the PDF's self-Misfortune/fray meter.
  Normal rolls spend it and passive generation can offset it. Sequence 2 completes
  the eight-block form, grants Speed III/Resistance II and removes this periodic
  negative addition. Sequence 1 removes the timer; recasting ends it. Its future
  preview shows probabilities/reserve forecasts, never guaranteed random outcomes.
  Reconcile form Resistance with material defense to avoid applying either twice.

### Key 6 wheel: Chaos and Prophecy

- **Chaos Walk (3):** six seconds of Speed II, targeting concealment and one projectile
  avoidance. Leave bounded eight-second patches applying one base Misfortune per
  second to enemies within three blocks. Patches do not create or drain reserve.
- **Aura of Chaos (3):** ten-block aura for 12 seconds, two base Misfortune per second
  on non-boss enemies and 25% attack/ability misfire. Use shared recovery/immunity;
  do not deduct luck for every aura tick.
- **Words of Fortune and Misfortune (2):** explicit blessing or curse mode, independent
  of friendship, on self or an aimed living target. Blessing adds +30 luck once and
  protects for 60 seconds with up to one-in-three dodge and +20% crit chance, with
  successful luck procs spending positive reserve. Curse adds -30 once, applies ten
  base Misfortune and attempts one mishap/second for ten seconds; bosses receive half
  the curse strength. This higher-tier ability creates signed luck through its
  spirituality cost, unlike reserve-conserving Bestow Luck. No guaranteed immunity
  to bad Wheel stops: positive additions first cancel existing negative luck.
- **Fate Concealment (2):** eight seconds of protection from gaze, divination and
  targeting for self or ally; nullify the next three hits. No added reserve cost.
- **Arranged Opportunity (2):** mark an eight-block area for 20 seconds. The first
  enemy attack inside is turned aside; its attacker receives a top-tier mishap and
  four base Misfortune; allies inside lose eight seconds of cooldown. The single
  trigger also adds +5 luck to its caster. Do not award per target or per hit.

### Key 7 wheel: Cycle

- **Reboot (1):** restore positions, health and effects within 24 blocks to eight
  seconds ago, including projectiles where supported. Dead entities stay dead,
  blocks remain unchanged. Cooldown 120 seconds. Sequence 0: 48 blocks, 20 seconds
  back. Luck, spirituality, paid cooldowns and Wheel spending are not rewound;
  snapshots must not refund resources or reproduce bestowals.
- **Fate Loop (1):** a broken silver-wheel medium with 40 health holds a ten-block
  area for 15 seconds. Every three seconds, restore hostile non-boss occupants'
  position and health from three seconds ago; caster/allies are exempt and mobs
  do not notice. Destroying the medium ends it. Sequence 0 includes bosses and
  doubles medium health. Luck reserves are not looped.
- **Fated Connection (1):** throw a coin onto an entity or ground; at most three
  anchors for 60 seconds. Native eligible effects echo from each at half power;
  aiming at an existing coin swaps places with it. Echoes do not pay, reroll,
  regenerate or duplicate signed transfers, reserve creation, Gambit prizes,
  capstones or timeline operations. Carry cast power/context explicitly.
- **Reincarnation (1):** arm for ten minutes; lethal damage instead restores half
  health at the position from ten seconds ago, clears effects and conceals for
  three seconds. Consume the ward; cooldown 15 minutes. Add +10 luck and roll once
  using the resulting balance; replace the PDF's forced Fortune with this signed
  rule. Keep existing resurrection restrictions and ward priority coherent.
- **The Wheel of Fortune (0):** giant silver sky wheel for 30 seconds, stoppable by
  recasting; cooldown five minutes. Every four seconds, creatures within 30 blocks
  receive one pulse: allies gain +10 luck and heal 15%, enemies gain -10 and suffer
  one top-tier mishap. These are explicit capstone additions, subject to caps. A
  Snake recipient's next Wheel obeys its resulting balance; pulses do not force
  arbitrary material results. Recasting to stop pays nothing and triggers no roll.

### Passive and inherited growth plan

- **Monster (9):** Spirit Perception warns when an unseen threat starts hunting;
  slightly faster spirituality regeneration. Signed luck generation already starts
  here as implemented, replacing the PDF's later oscillating Passive Luck.
- **Robot (8):** Strength I and modest movement enhancement, avoiding duplicate
  application of material combat scaling.
- **Lucky One (7):** unlock intentional bestowal and Spin; no separate random luck
  drift or free dodge/drop buff alongside the reserve.
- **Calamity Priest (6):** Calamity Sense exposes enemy Misfortune. The implemented
  aimed counter already exposes this; richer overhead marks are future visuals.
- **Winner (5):** Winner's Edge supplies a 10% crit opportunity, gated by positive
  reserve and spending one luck point on success. Avoid double rolls with other
  reserve-based crit effects.
- **Misfortune Mage (4):** enemies within six blocks gain one base Misfortune every
  five seconds; Spin loses its cooldown.
- **Chaoswalker (3):** Premonition already gains three avoided hits. Wider area-hit
  support is future work. Ripple of Misfortune adds one base Misfortune to the
  nearest enemy within six blocks when hurt. Calamity Defiance no longer removes
  unlucky materials: it is represented by stronger passive recovery/progression,
  and cannot bypass a negative balance.
- **Soothsayer (2):** Body of Mercury, once per 90 seconds, avoids a hit exceeding
  one third maximum health and provides five seconds of untouchability, then adds
  +10 luck once. Complete serpent form, upgraded Chain and multi-target Bestow
  described above. Spirit Vision already has 64-block base area; reading other
  entities' signed balances replaces the PDF's three-Omen labels.
- **Snake of Mercury (1):** Cycle records nearby timeline history for Reboot and
  Reincarnation. Fate Resistance halves signed luck changes imposed by other
  casters (positive or negative), not passive generation or the caster's own costs;
  conserved transfers debit only the accepted amount. These resistance hooks are
  future work, not silently built into the low-level reserve API.
- **Wheel of Fortune (0):** Fate Authority may choose a material only on the side
  allowed by current luck (or Gold); choosing a non-neutral tier pays its normal
  reserve cost. Zero permits only Gold, negative cannot choose a lucky tier. This
  authority control is future work and never silently replaces signed rolls.

### Presentation and implementation notes

Future melee combo visuals: a forward silver wheel; red/violet arcs; a gold coin
flipping over the target with visual-only heads/tails sparks. Do not spend luck for
cosmetic randomness. Register through `combat/MeleeCombos` when implemented.

Reuse `Substitute`, `Spells`, `HudCounter`, `HudTimer`, `Misfortune`, `Concealment`,
`Root`, `Later`, `DeathWard` and existing spell FX. Add reusable ledger, timeline,
loop-medium and fated-link components when those abilities are implemented. Timeline
recording should be bounded to relevant nearby entities and active users, with enough
history for Sequence 0's 20-second rewind; do not retain the PDF's insufficient
12-second buffer. Spell echoes carry explicit origin, power and resource ownership.

Future model assets: small material Wheel, sky wheel, connected-wheel serpent, coin,
anvil, dice, broken medium and thread; silver droplets, gold motes, red sparks.
Existing material names/icons and shared FX take precedence over obsolete three-Omen
asset names. The form's head/tail wheels are incomplete until Sequence 2.

Persistent implemented keys: `snake_luck_balance`, `snake_wheel_tier`,
`snake_wheel_sequence`, `snake_of_mercury_next_spin`, Key 1 vision/premonition state
and existing Netherite grace state. Do not restore obsolete omen, luck-drift or fray
keys from the PDF. New saved state for future abilities belongs with its component.
