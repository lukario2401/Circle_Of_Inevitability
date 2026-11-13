# God of Decay

Implemented from the ability design: 26 abilities from Patient (Sequence 9) through
Second Law (Sequence 0), with disease tracking, contagion, passives, doubles,
mythical forms and the existing visual/audio kit. Select `God Of Decay` through the
pathway command, then set the desired Sequence using the existing sequence command.

## Controls

| Ability key | Normal | Sneak |
| --- | --- | --- |
| 1 | Ulcerous Burst | Carrier's Touch |
| 2 | Red Tape | Official Capacity |
| 3 | Corruption | Hollow Out |
| 4 | Disease wheel | Repeat last successful Disease pick |
| 5 | Division | Pathogen Form |
| 6 | Doom wheel | Repeat last successful Doom pick |
| 7 | Time and Entropy wheel | Repeat last successful Time pick |

Wheels use the same `AbilityWheel` interaction as Snake of Mercury. A normal
selection pays its ordinary cost and starts that entry's cooldown. Sneak recasts
pay **3×** the ordinary cost and neither check nor start a cooldown. Normal cost
modifiers still apply. Failed casts spend nothing and do not replace the last pick.
Swollen Form, Virus Incarnation and Ragnarök can be dismissed for free; recasting
Division while a double stands is a free no-op.

Disease contains Laziness, Obesity, Degeneration, Incubation and Rot. Doom contains
Certain Death, Sure Hit, Decay, Swollen Form, Left Hand of Decay, Withering Palm and
Gravel Shot. Time contains Slow Time, Segmented Time, Senescence, Entropy, Virus
Incarnation and Ragnarök. Each entry unlocks at its design Sequence. Numeric costs
and cooldowns are centralized in `GodOfDecay.SPECS` (durations use server ticks).

## Gameplay and ownership

`GodOfDecayMarks` stores marks per source UUID and disease kind. Reapplication does
not duplicate Sickness or restart damage timers. Incubation remains hidden and
uncleansable until its outbreak. Health/armor/attack-speed modifiers are transient
and removed when their marks expire. Second Law marks resist cleansing and dispel.
The seven sickness kinds count toward six displayed pips and a maximum 30% damage
bonus. Friendly targets are excluded from hostile applications and contagion.

`GodOfDecayPowers` owns live cast lifetimes, destructible Division constructs, safe
reforming, forms and moving domains. Death, logout, dimension changes and level
unload end active casts. Large Sequence 2/1 forms use tall visual models with a
practical six-block body. Terrain is not damaged. The FX preview command remains
available; see [GOD_OF_DECAY_FX.md](GOD_OF_DECAY_FX.md).

Certain Death captures Substitute and DeathWard identities before the beam lands.
A ward armed afterward cannot replace the captured protection. Senescence and
Ragnarök use unavoidable, owner-credited damage; creative and spectator players
remain protected. Named vanilla bosses are excluded from weak-mob execution.

## Shared APIs for future pathways

These helpers live in `combat`, independent of God of Decay's rules:

- `HeldActions.addRule(entity -> delayTicks)` registers a delay rule. Key packets,
  wheel casts and normal mob casts call `defer(entity, action)` after validation.
  Deferred casts revalidate before paying. Player aim/sneak state is captured,
  pending work is bounded, and logout/dimension/unload cancel it. Use `replay` for
  delayed damage so that the same hit is not held recursively.
- `Weathering.area(owner, level, center, radius, extraTicks)` ages hostile potion
  effects, marks, constructs, summons, zones, domes and walls. Custom typed status
  systems can register `addAdapter((entity, ticks) -> ...)`. Do not scan arbitrary
  persistent data for timer-like names.
- `Support.heal(provider, receiver, amount)` and `Support.effect(...)` retain the
  source of allied aid. `Support.blocked(...)` gates composite aid such as cleansing
  plus healing. New ally-support abilities should use these instead of calling
  `receiver.heal` directly. Dome shelter and damage interception also use the gate.
- `ProjectileTempo.addRule(projectile -> multiplier)` scales actual tick travel and
  collision traces temporarily. The slowest overlapping rule wins. The original
  velocity is restored after the tick, retaining the projectile's acceleration;
  repeatedly applying the rule does not compound velocity toward zero.
- `CertainDamage.strike(caster, target, amount, priorWard)` delivers unavoidable
  damage with normal damage/death events and kill attribution. Capture
  `DeathWard.snapshot(target)` before windup; pass null for irreversible damage.
  Check `Substitute.takeSnapshot` separately if a pre-existing substitute is allowed.
  Supply the final damage amount including pathway-specific bonuses.
- `Spells.isNaturalBoss` identifies named vanilla bosses independently of a spell's
  execution-health threshold. `Spells.isBoss` retains its existing health heuristic.

Register shared rules once in initialization. Keep pathway-specific conditions,
Sickness calculations and FX in the pathway package. New effects that grant allied
healing without a source must be migrated to `Support` to participate in Corruption.

## Validation

`GodOfDecayGameTests` registers server regression tests covering all three wheels,
fixed recast accounting, failed casts, free dismissal, latent outbreak, owner scope,
cleansing, temporary attributes, prior wards, expiry damage, Division, weathering,
delayed execution, allied support, projectile travel and friendly-target safety.
Run alongside Snake of Mercury's existing tests:

```sh
JAVA_HOME=/path/to/jdk25 ./gradlew runGameTestServer --offline
python3 tools/model_gen/god_of_decay/validate.py
```

Visual appearance and multiplayer feel still require an in-client playthrough;
headless tests verify mechanics and asset references, not rendering quality.
