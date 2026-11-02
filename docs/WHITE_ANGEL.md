# White Angel — Sun Pathway

All 29 abilities are implemented, with seven keys and no ability cooldowns. Select the pathway with
`/sequence pathway <player> "White Angel"`, then choose an unlocked Sequence using the standard commands.

| Key | Unlock | Normal press | Sneak press |
| --- | --- | --- | --- |
| 1 | Bard (9) | Hymn of Valor | Hymn of Renewal |
| 2 | Light Suppliant (8) | Sunshine | Daytime |
| 3 | Solar High Priest (7) | Theurgy wheel | Last Theurgy pick |
| 4 | Notary (6) | Notarization wheel | Last Notarization pick |
| 5 | Unshadowed (4) | Radiance wheel | Last Radiance pick |
| 6 | Justice Mentor (3) | Justice wheel | Last Justice pick |
| 7 | Lightseeker (2) | Sun wheel | Last Sun pick |

Wheel entries retain their saved asset IDs. Opening menus is free; quick casting costs the same as choosing
an entry. Refused targets consume nothing. Repeating Daytime, Sun Halo, Domain, Justice Halo, Solar Envoy,
Holy Kingdom or Sun lowers it for free, including with an empty spirituality pool. Named fields, coats,
equipment and forms replace existing instances. Fields and owned equipment end on death, logout,
dimension, Sequence or pathway changes. Delayed attacks require a living caster in the original dimension.

## Abilities and costs

| Wheel | Entries (spirituality) |
| --- | --- |
| Theurgy, Sequence 7 | Holy Light Summoning 900; Sun Halo 700; Holy Oath 600; Cleave of Purification 500; Fire of Light 1100 |
| Notarization, Sequence 6 | Contract 1500; Authenticate 800; Amplify 600; Nullify 700 |
| Radiance, Sequence 4 | Purification 3000; Unshadowed Domain 4000; Flaring Sun 4500; Unshadowed Spear 3000; Pure White Ray 3500; Holy Equipment 3500 |
| Justice, Sequence 3 | Define Justice 0; Justice Halo 8000; Judgment of Justice 6000; Faith Appraisal 1500 |
| Sun, Sequence 2 | Solar Envoy 12000 + 400/s; Praise the Sun 8000 |
| Sun, Sequence 1 | Holy Kingdom 40000 + 600/s; Servant of Faith 15000; Pure White Light 25000 |
| Sun, Sequence 0 | Sun 50000 + 1500/s |

Costs account for the actual `Beyonder` pools. Hymns cost the smaller of 250 and 25% of maximum spirituality
(5 at Bard, 100 at Light Suppliant). Sunshine costs the smaller of 600 and 25% (100 at Light Suppliant).
Daytime costs the smaller of 1250 and 62.5% (250 at Light Suppliant). The draft's 90000 Sun price exceeded
the real 78400 pool; its implemented price is 50000. Upkeep begins after opening, without a second initial
charge. The mod currently refills Sequence 2–0 spirituality every tick; upkeep uses that existing economy.

Radius and buff duration grow 5% per Sequence gained. Natural daylight and light fields grant +15% to
White Angel purification strikes. Undead take ×2.5, other evil ×1.5, with another ×1.5 for Corrosion,
Decay or Madness. Evil follows `Spells.isEvil`, including vanilla hostile monsters.

Radiance supplies a warned Flaring Sun with burning afterglow, a travelling spear (piercing and teleport
blocking at Sequence 2), a warned piercing ray, and renewable armor with a non-item mace. Shared armor
at Sequence 3 protects allied minds and ends with the caster's equipment set. Equipment cannot be dropped.

## Justice and pacts

Define Justice chooses Mercy, Retribution, Purity or Order. The choice is saved and cannot change while
your Justice Halo or a pact stands. Combat history lasts 30 seconds. Harming bystanders violates Mercy;
initiating hostilities violates Retribution; evil/unclean attacks violate Purity; pact breaches and Decree
violations violate Order. Hostile mobs hunting your side have at least Guilt 1. Guilt is capped at 5.
Violating your own principle costs 10% spirituality and locks abilities for five seconds.

Justice Halo applies live guilt-based damage weakness, reduced damage dealt and healing denial at Guilt 3.
At Sequence 2, new violations trigger true-damage trials, capped at five per ten-tick beat. Judgment has a
30-tick warning and range leash, refuses innocent targets for free, then condemns and seals the two most
recent keys. Faith Appraisal reports weaknesses, grants an attack mark and shows guilt to your allies.
Guilt labels and outlines are sent only to the caster and allies.

Contract automatically binds eligible mobs. Players must explicitly use `/whitepact accept` within 20
seconds and 16 blocks. Sending an offer costs spirituality; accepting it does not charge again. Ordinary
pacts last 30 seconds before Might scaling; slots are 2/3/5/9 at Sequences 6/4/3/1. Holy Contract can bind
up to eight eligible hostile mobs as one slot when aimed at empty space. Duplicate bindings are refused.

A protected stronger Sequence cannot violate the truce (one tier stronger, or two for Holy Contract).
An additional tier can break it but pays spirituality; stronger targets are refused. Breaches end that
binding and deliver six unavoidable golden-flame pulses. Holy breaches also lose 25% maximum health.
Pacts are outside ordinary Dispel/Unbound status lists. Nullify prioritizes projectiles, then weakened
shells at Sequence 4, then enemy boons/wards and the last key. Purification's strongest-key selection
includes stored stolen powers, recorded powers, completed Codex powers and a borrowed power's container.

## Sun authorities and passives

Solar Envoy grants Sunbird flight, damage reduction, cleansing and a burning/revealing field. Praise the
Sun crosses a collision-safe route, damages enemies along it and grants temporary invulnerability,
concealment and an anchor against pulls, teleport roots and enclosures. Kingdom forbids evil concealment,
stand-ins and resurrection, unmaking small evil mobs even when allied; larger evils lose health each second.

Servant of Faith grants Fervor to allies, or channels five seconds to convert an eligible hostile mob.
There are four believer slots, independent of ordinary Marionettes; a fifth replaces the oldest. Believers
last five minutes and vanish when released. The caster's Nemesis aura leaves the current conversion target
alone. Pure White Light warns for two seconds, strips enemy protections, destroys constructs and summons,
closes enemy zones, decrees, enclosures and shells, and completely cleans allies. Sun prioritizes eight guilty
evil targets each second, doubles enemy ability costs and leaves two minutes of forced daylight when lowered.

Passives include extra health, private Evil Detection, Night Vision, fire/fear resistance, Notary Fortune,
Nemesis, Holy Eye, an unseen-hunter warning chime, Purified Body and Light Body immunities, supernatural
damage reduction and pact/believer recovery. Inextinguishable Light spends half the maximum spirituality
to survive an ordinary fatal hit at half health, with a purifying burst and three seconds of invulnerability.
It cannot stop damage that bypasses invulnerability, including the void and `/kill`.

## Validation and previews

Run `./gradlew build` and `./gradlew runGameTestServer -PwhiteAngelTests`. Tests exercise costs, unlocks,
free lowering, flight cleanup, innocence/guilt, warned Judgment, player pact consent, shared equipment
cleanup, copied-power key ranking, believer conversion and the fatal-hit resource gate.

`/whitefx all` previews the authored models in sequence; `/whitefx <model>` previews one. These commands
require gamemaster permission. See [asset documentation](WHITE_ANGEL_ASSETS.md) for generation and validation.
Client placement, multiplayer feel and final balance still need an in-game playtest; server tests do not
establish those visual or balance judgments.
