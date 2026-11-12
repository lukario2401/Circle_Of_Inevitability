# Extending Nature Walker and reusing its spell-kit components

The gameplay entry point is `NatureWalker`; the uploaded `NATURE_WALKER.md` is a
reference, not a second configuration file. The catalog has 39 entries. Costs,
sequence gates, cooldowns, wheel placement and mob registration originate in
`NatureWalker.SPELLS`, so adding a cast does not require editing seven packet handlers.

## Add an ability

1. Add one `Spell(id, key, sequence, cost, cooldownTicks)` to `SPELLS`. Keys 3, 5,
   6 and 7 use Harvest, Earth, Alchemy and Return. Keys 1, 2 and 4 are direct casts.
2. Implement its operation in the corresponding pathway module. The core calls
   `NatureWalkerAdvanced.cast(state, id)` for additional entries. Return `false`
   before changing state if targeting or prerequisites fail. The wheel and
   `MobCasting` charge only after success; do not charge again inside the operation.
3. Use the existing `State` and `valid(state)` identity check in delayed work.
   Put an ongoing effect in the state's board and close it in `clear`. Name
   shared effects with the pathway prefix and owner-specific root/FX tags.
4. Add an icon, FX and sound through `tools/model_gen/nature_walker/catalog.py`
   and its generators if the presentation collection does not already contain it.
5. Add a real-server test for success, refusal without payment, sequence gating
   and cleanup. Include players and mobs when their behavior differs.

The `Field` helper in `NatureWalkerAdvanced` centralizes zone opening, replacement,
beats and teardown. To add another land effect, give it an entry in `cast` and
`pulse`; `close` and `finishField` handle the lifecycle. Temporary realm growths
are associated with that field and do not displace the ordinary garden.

## Growth timing without pathway dependencies

`combat.Growth` is a clock, not a Minecraft entity, renderer or global manager.
A garden owns one clock per plant and supplies the speed and mature lifetime:

```java
Growth clock = new Growth(20, 60, 20); // sprout, full maturity, wither duration
Growth.Stage stage = clock.tick(level.getGameTime(), growthSpeed, 240);
if (stage == Growth.Stage.BLOOM) { /* apply the plant's periodic effects */ }
if (stage == Growth.Stage.DEAD) { /* remove it and clear its model */ }
clock.advance(40); // accelerators share the same clock
clock.ripen();
clock.expireBy(level.getGameTime() + 100); // consumed fruit shortens life
```

Tick each clock once per server tick. Do not reset its mature expiry on every
refresh. `NatureWalkerGrowths` supplies the actual kinds, planting validation,
cross-pollination, cap, harvest effects and presentation. Add a new kind there;
other pathways can use `Growth` with an entirely different garden and renderer.

## Mutations

`status.Mutation` has no dependency on Nature Walker art or balance. Its public
API supports players and mobs and isolates each source's mutations:

```java
Mutation.hit(caster, target, Mutation.Kind.FUNGUS, 1); // 3 mutagen per level, 6 on bosses
Mutation.apply(caster, target, Mutation.Kind.VINES, 2, 240);
int level = Mutation.level(caster, target); // 0..3
Mutation.bloom(caster, target, 100); // only at level 3, capped damage + nearby roots
Mutation.clearSource(caster);     // owner teardown, preserves other sources
Mutation.clear(target);           // cleansing
```

Organs increase incoming damage, Fungus weakens and spreads Madness on death,
Wheat slows, and Vines make attacks falter and briefly root. Expiration and source
loss remove owned modifiers. Multiple Organs sources use the strongest level,
not a multiplicative stack. `Mutation.addListener` supplies presentation hooks;
Nature Walker registers its FX once in setup. The status automatically participates
in `Cleansing`. Short vanilla potion effects can remain for up to 25 ticks after
cleanup; another spell's potion effect is never stripped just to hide that tail.

## Healing and damage attribution

Use `Healing.apply(source, target, amount)`. Its return value is health actually
restored, after the existing Forge healing pipeline, including NoHeal, Decree and
Inversion. `Healing.apply(target, amount)` remains a compatible self-heal overload.

```java
Healing.addScale((source, target) -> myBlessingApplies(source, target) ? 1.2 : 1);
float restored = Healing.apply(caster, ally, 4);
```

Register scaling rules once at setup, not once per cast. They must be side-effect
free and return a finite nonnegative factor. Ordinary healing must not call
`setHealth`; resurrection and maximum-health transformations are separate operations.

`Corrosion.apply(target, caster, bite, growth, ticks)` now retains mob as well as
player attribution. `Corrosion.onBite((target, source, actualHealthLost) -> ...)`
provides a generic callback for leeching or other secondary effects. The existing
player-oriented `Corrosion.Listener` remains available. Nature Walker's Descent
uses the new hook and verifies that its owner and field are still active.

## Companions and snapshots

- `Construct.of(type).limit(group, cap).build(...)` supplies bounded summons,
  leash handling, replacement and teardown. Use separate names for spirits and
  golems. `Construct.ofOwner` returns construction order, even for same-tick casts.
- `Construct.extend` renews the construct, underlying summon control and hidden body
  together; `Marionette.extendSummon` is available for other summoned creatures.
- `Marionette.controlInGroup(mob, owner, ticks, group, cap)` gives existing creatures
  independent control caps without consuming ordinary marionette slots.
  `NatureWalkerBrood` adds its trait table and removes transient health/damage
  modifiers before releasing a hybrid. Do not discard a real creature to clean up control.
- `Fallen.Body.maxHealth()` records the health pool at death for proportional spirits.
  Consume a body with `Fallen.take` only after successful creation; never raise
  an entity that is alive again.
- `DeathWard.snapshot` returns an opaque identity. Save it when arming an owned ward
  and call `DeathWard.disarmIf(target, token)` on cleanup. This preserves a newer
  ward installed by another spell. Seed of Rebirth also owns its cocoon root and
  restores earlier concealment when the cocoon ends.

## Dynamic mob tuning

`MobAbility.costBy(mob -> value)` and `cooldownBy(mob -> ticks)` support sequence
upgrades without mutating the shared registry during a cast. `copy()` retains the
rules for encounter-specific variants. `MobCasting` snapshots cost and cooldown
before executing and charges that exact cost after success. Rules must be pure.
Nature Walk uses this for its 6,000-point higher-sequence cost; Brood Form uses it
for the complete form's longer cooldown.

## Validation

`./gradlew runGameTestServer -PnatureWalkerTests build` runs both Nature Walker
suites and ordinary build checks. Higher-sequence fixtures force separate chunk
areas and let the server promote them before ray targeting. Test behavior and
ownership boundaries rather than mirroring the implementation's formulas.
`python3 tools/model_gen/nature_walker/validate.py` verifies the presentation contract.
