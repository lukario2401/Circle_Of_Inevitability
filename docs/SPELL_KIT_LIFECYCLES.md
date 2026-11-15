# Reusable combat lifecycles

These additions extend the [spell kit](SPELL_KIT.md). Prefer the existing `Zone`, `Form`, `Construct`, `Marks`, `Root.applyOwned` and `Teleport` primitives before making a new per-pathway event system. Helpers below operate on `LivingEntity` unless stated otherwise. Run casts and ticks on the server thread.

## Channels

```java
Channel channel = new Channel(caster, 60, 20, 0, true,
    () -> castIsStillCurrent(),
    pulseIndex -> dealPulse(pulseIndex),
    () -> clearBeamVisuals());
// In the owner's server tick:
channel.tick();
// On replacement, death, logout, dimension/pathway/Sequence change:
channel.close();
```

Duration, pulse period and first delay are ticks. Pulses are zero-indexed; a 60-tick channel with first delay zero pulses at 0, 20 and 40. Use duration 81 and first delay 20 for four pulses at 20, 40, 60 and 80. `true` adds an owned MOVE/JUMP root. Death, removal, dimension change, Silence, an ATTACK restriction, or a false condition interrupts it. Close is idempotent and releases only this instance's root. The helper does not register its own tick loop: the ability owns scheduling and cleanup.

Filthy Monarch's earlier Flames of the Abyss and new Corroder share this implementation. Damage, aiming, visuals and resource costs remain the caller's responsibility.

## Conditional wards and links

```java
Object link = DamageLink.redirectWhen(caster, anchor, .25f, 240,
    () -> anchor.isAlive() && anchor.level() == caster.level()
        && caster.distanceToSqr(anchor) <= 1600 && castIsStillCurrent());
Object ward = DeathWard.armWhen(caster, 240, .4f, 40,
    () -> anchor.isAlive() && anchor.level() == caster.level()
        && castIdentityIsCurrent(),
    (saved, source, level) -> showRebirth(saved));
// Later: remove only these applications, not replacements from other spells.
DamageLink.clearRedirectIf(caster, link);
DeathWard.disarmIf(caster, ward);
```

Conditions are checked on use, including at the lethal event, so killing an anchor just before its owner's tick cannot leave a stale protection. A ward condition must **not require the protected caster to be alive**: health is already zero when a cancellable death event fires. Validate cast identity, pathway, dimension and anchor instead. A ward callback runs after health is restored.

Tokens use object identity: even two identical same-tick casts remain distinct. Existing `arm`, `redirect`, snapshots and callers keep their behavior. The existing resurrection prohibition, piercing-hit and void rules still apply. Only one ward and one redirect occupy an entity's shared slot; choose explicit precedence when a pathway offers several. Preserve another pathway's existing ward where appropriate.

## Health, durability and damage

```java
if (!Spells.payBloodFraction(caster, .2f, 6)) return REFUSED;
// Or an absolute cost:
if (!Spells.payBlood(caster, 4, 6)) return REFUSED;
Spells.wearArmor(target, 3);
DamageLimit.run(target, target.getMaxHealth() * .03f,
    () -> strikeTarget(target, damage));
```

Blood payment directly reduces health, cannot leave less than one health, rejects non-finite values, and makes no change on refusal. Check all other cast prerequisites first. Health payment is not incoming damage: it does not trigger armor, links or hit effects. Dark Angel's existing ritual now uses the same absolute-price helper.

`wearArmor` affects the four armor slots through normal durability/Unbreaking/break handling. It is shared by Acid Swamp, the earlier Corrosive Fog, Thunder God's corrosive rain and its higher storm powers. It does not affect held equipment or terrain.

`DamageLimit` applies a scoped ceiling at the final damage event, after armor and ordinary damage modifiers. Nested scopes keep the stricter cap and restore the previous scope on return or exception. Use it for boss percent-health limits; it does not bypass immunity or protection. Listeners that change damage after this LOWEST-priority event can still alter the outcome.

## Barriers

```java
BarrierErosion erosion = new BarrierErosion();
// Every tick while the beam remains valid:
erosion.tick(level, from, to, owner -> Spells.isEnemy(caster, owner), 40);
// When the beam ends:
erosion.clear();
```

Crossed `Dome` and `Wall` instances weaken on contact and close after forty continuous ticks. Losing contact resets exposure; a replacement with the same name starts fresh. Allies are filtered by the caller. Temporary weakening expires three ticks after the last contact. For custom interactions, `Wall.crossedBy`, `Wall.weaken`, `Wall.isWeakened`, and identity-checked `Dome.closeIf`/`Wall.closeIf` are available. Domes expose `snapshot`; wall crossing records include the token. No terrain is altered.

## Attack tempo and healing

```java
AttackTempo.apply(target, uniqueCastTag, .5, 160);
AttackTempo.remove(target, uniqueCastTag);
Healing.apply(caster, target, 12);
```

Attack tempo changes players' real attack-speed attribute; mobs receive a minimum interval between melee attempts. The strongest active named slowdown wins, and removing one source preserves the rest. Expiry, death, removal and logout clean up the attribute. Unlike `Falter`, attacks do not randomly miss. `Falter.addDurationScale` lets innate mind resistance reduce future application durations without replacing other rules.

Use `Healing.apply` for attributable healing abilities. It still posts Forge's heal event, honors NoHeal and other vetoes, and exposes `Healing.isSpellHealing()` during the call. The King of Filth aura blocks unattributed recovery but permits these healing spells. White Angel is exempt from the aura; new purification effects can register `FilthyMonarchPassives.addPurificationExemption` once at setup. Vanilla events lack a universal heal-source field, so direct `entity.heal(...)` calls are treated as unattributed.

## Ownership and verification

Keep a unique cast tag, snapshot or token whenever cleanup could overlap another cast. Validate identity before delayed work. Clear per-caster collections on death, logout, dimension change and pathway/Sequence loss. Give every visual a bounded lease as a fallback. Avoid storing entity references after their effect expires.

The 22 Filthy Monarch GameTests cover casts and these contracts, including foreign-root preservation, same-tick ward replacement, dead anchors, boss ceilings and barrier erosion. Existing Thunder God and Author tests provide regressions for the shared armor and ward changes. Run the relevant pathway suites and `./gradlew build` after extending the helpers.
