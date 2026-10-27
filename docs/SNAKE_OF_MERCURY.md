# Snake Of Mercury — material Wheel and Premonition

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

Fortune is luck strength, not the probability of rolling that material. It scales
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

### Roll percentages

| Sequence | Wood | Stone | Iron | Gold | Emerald | Diamond | Netherite |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 9–8 | 15 | 15 | 20 | 35 | 10 | 4 | 1 |
| 7–4 | 5 | 10 | 15 | 40 | 20 | 8 | 2 |
| 3 | 0 | 0 | 0 | 45 | 35 | 15 | 5 |
| 2 | 0 | 0 | 0 | 30 | 40 | 22 | 8 |
| 1 | 0 | 0 | 0 | 20 | 35 | 30 | 15 |
| 0 | 0 | 0 | 0 | 0 | 0 | 35 | 65 |

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

Only the Wheel, Key 1 and shared misfortune improvements are implemented. The other
keys, silver-serpent form, timeline and capstone are future work.

## Validation

Run `./gradlew build` with Java 25 and the environment's HTTPS proxy/trust configuration.
Run the dependency-free behavioral checks separately:

```sh
javac -d /tmp/wheel-tests src/main/java/net/lukario/circleofinevitability/fate/{FateOdds,WheelTier,DisasterBalance}.java tools/tests/FateOddsTest.java
java -cp /tmp/wheel-tests FateOddsTest
```

In a development game, check each material's damage/defense, vision area, regeneration
and status expiry; Netherite revival consumption and its exclusions; aimed-counter
updates and severe enemy-only mishaps. These runtime checks require a Minecraft game;
the standalone checks validate distribution, multipliers and disaster growth/bounds.
