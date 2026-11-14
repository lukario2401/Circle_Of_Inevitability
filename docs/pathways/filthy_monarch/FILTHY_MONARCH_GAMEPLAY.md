# Filthy Monarch gameplay

The first **18 of 35 abilities** in the [supplied design](FILTHY_MONARCH.md) are playable: six direct casts, all eight Devil Arts entries and the first four Desire and Malice entries. Their icons, models, particles and original sounds are connected to server combat. Lower Sequence numbers unlock stronger abilities.

## Setup and controls

Operators can select the pathway and a testing Sequence with:

```text
/sequence pathway @s "Filthy Monarch"
/sequence set @s 5
/sequence spirituality @s 100000
```

Use Sequence 4 to try Demon upgrades and Hellfire, or Sequence 1 for Flames of the Abyss. The normal spirituality cap and regeneration still apply. `/filthyfx` remains a separate, harmless asset preview.

| Default key | Normal press | Sneak + key | Unlock |
|---|---|---|---|
| G | Lawless Lunge | Dirty Trick | 9 |
| H | Sulfur Flame | Sink | 8 |
| J | Killing Spree | Devil Worship | 7 |
| K | Repeat last Devil Arts selection | Open Devil Arts wheel | 6 |
| L | Repeat last Desire and Malice selection | Open Desire and Malice wheel | 5 |

N and M report that their wheels are not implemented. Select an entry before using a wheel's repeat key. Direct casts have no cooldown. Selecting a wheel entry uses the cooldown below; normal-key repeats bypass that cooldown and pay the same spirituality cost. Failed targeted casts do not spend spirituality. Selecting an active form again dismisses it for free, including during its cooldown or with no spirituality.

## Implemented abilities

Damage below is the base value before armor, Seed amplification and other modifiers. Wheel cooldowns are in seconds.

| Ability | Sequence | Cost | Cooldown | Behavior |
|---|---:|---:|---:|---|
| Lawless Lunge | 9 | 10 | 0 | Safe lunge to a visible enemy; 40 + held weapon damage, scaled by Sequence; brief stagger. Burning strikes from 6. |
| Dirty Trick | 9 | 10 | 0 | 15 damage, three seconds of blindness and 40% attack failure. |
| Sulfur Flame | 8 | 200 | 0 | Blue flame cone: 45 damage, burn, poison and Seeds. Range and Seed count improve with Sequence. |
| Sink | 8 | 200 | 0 | 10 damage, another 20 if airborne; downward slam, Slowness III and jump restriction. Nearby splash from 5. |
| Killing Spree | 7 | 700 | 0 | Three 18-damage cuts with bleeding, followed by Speed II and +25% damage. Kills extend bloodlust; from 3 they also heal. |
| Devil Worship | 7 | 900 | 0 | A temporary allied projection, strengthened by Offerings. Three Offerings grant fireballs. Summon cap rises from one to two at 4 and three at 2. |
| Sulfur Fireball | 6 | 1200 | 4 | Three moving, colliding fireballs for 28 each. Devil Form: 12 × 16; Demon Form: 20 × 14. One Seed per target per volley. |
| Devil Form | 6 | 2500 | 30 | 60 seconds of increased size, reach, knockback resistance, Strength, Resistance and Speed; burning melee. Demon upgrade from 4. Natural expiry causes five seconds of fatigue. |
| Sword of Lava | 6 | 1800 | 6 | Wide blue blade sweep: 85 armor-piercing damage and burn. |
| Flame Cage | 6 | 1800 | 10 | Holds a target and burns it for 10 per second; projectile screens surround it. Without a target, creates a screen in front of the caster. |
| Volcanic Eruption | 6 | 2500 | 12 | One-second warning, then 80–120 damage, launch and lingering fire. |
| Toxic Vapor | 6 | 1500 | 10 | Ten-second poison field, entry blindness and 8 damage per second. From 4, continuous exposure corrodes and damages armor. |
| Hellfire | 4 | 6000 | 15 | One-second ground warnings beneath up to eight visible enemies, then 110 damage and soul burn. Moving away dodges the strike. |
| Flames of the Abyss | 1 | 30000 | 20 | Rooted three-second channel with 90-damage pulses, poison, corrosion and Seeds. Sequence 0 extends duration, range and width. |
| Shadow-Shift | 5 | 2500 | 6 | Safe blink up to 24 blocks, 30 damage and a Seed along the route; old-position afterimage bursts for 40 after 1.5 seconds. |
| Desire Control | 5 | 3000 | 10 | 40 damage and three Seeds, attack failure and sprint suppression. At five Seeds, another 100 damage and brief root. Redirects mob aggression; area effect from 4. |
| Mental Shock | 5 | 3500 | 12 | 55 armor-piercing cone damage, stagger, fear and a Seed. |
| Desire Incarnation | 5 | 4000 | 20 | Six-second liquid form: 80% damage reduction, Speed II, mob targeting prevention and a 25-damage-per-second aura with slow and Seeds. |

## Shared mechanics

Depravity belongs to its caster, caps at five Seeds and increases that caster's damage by 4% per Seed. One Seed falls off every six seconds (12 at Sequence 0); applying Seeds refreshes that interval. Cleansing removes Seeds and the pathway's burns. From Sequence 5, every third ordinary melee hit against the same enemy adds a Seed.

Enemy kills from Sequence 7 grant Offerings, capped at five, each lasting 60 seconds independently. Devil Worship reads the current count without consuming it. With no Offerings, a projection has half strength and lasts ten seconds; otherwise it lasts twenty seconds. The Offering count appears in the HUD. Projections use the existing construct ownership, ally targeting and no-drop systems.

Ordinary burn damage respects fire immunity, Fire Resistance and water/rain. Hellfire soul burn bypasses those protections; the Abyss channel retains its non-fire damage against protected targets. Fire and eruption visuals do not place or destroy terrain. Flame Cage uses the shared Wall system for entity projectiles; custom spell beams are governed by their own collision systems. The Abyss torrent uses a fixed initial direction and can penetrate terrain.

All attacks use shared enemy checks and respect player teams, creative/spectator protection and friendly constructs. Delayed damage, summons, fields, forms and owned controls are cleared on death, logout, dimension change, or pathway/Sequence change. Clearing a caster preserves other casters' Seeds, roots and newer taunts. Silence interrupts the Abyss channel; it releases only the channel's own movement restriction. No other ability can be cast during the channel.

The first three direct keys are registered for existing stolen/borrowed ability systems. Wheels require the native pathway and their individual entries are not copyable. No autonomous Beyonder mob casting profile is added in this increment.

## Remaining scope

The last 17 abilities, including the remaining Desire and Malice entries and both later wheels, retain assets only. The complete passive progression table, optional six-hit melee combo, full mythical creature forms and viewer-specific fear/desire appearances are not implemented. The supplied design remains unchanged as the reference for later work.

## Validation

Ten dedicated GameTests exercise the 18 casts on a real server, including payment and unlocks, borrowed direct casts, Seed ownership, ally protection, projectiles, summons, forms, fields, warning timing, channel interruption and cleanup. Run with the repository's required Java toolchain:

```sh
./gradlew runGameTestServer -PfilthyMonarchTests --no-configuration-cache
./gradlew build --no-configuration-cache
python tools/model_gen/filthy_monarch/validate.py
```

Client-side visual quality and live multiplayer balance still need playtesting.
