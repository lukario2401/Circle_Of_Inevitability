# Filthy Monarch gameplay

All **35 abilities** in the [supplied design](FILTHY_MONARCH.md) are playable: six direct casts and four wheels, with passive progression and a visual melee combo. Their icons, models, particles and original sounds are connected to server combat. Lower Sequence numbers unlock stronger abilities.

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
| N | Repeat last Foul Tongue selection | Open Foul Tongue wheel | 6 |
| M | Repeat last Blood and Filth selection | Open Blood and Filth wheel | 4 |

With no prior selection, a normal wheel key opens its wheel. Direct casts have no cooldown. Selecting a wheel entry uses the cooldown below; normal-key repeats bypass that cooldown and pay the same spirituality cost. Failed targeted casts do not spend spirituality. Selecting an active form again dismisses it for free, including during its cooldown or with no spirituality.

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

| Desire Symbiosis | 5 | 4500 | 30 | Twelve-second life link within 40 blocks: redirect 25% of incoming damage and survive one lethal blow at one health, with 1.5 seconds of invulnerability. |
| Filthy Illusion | 4 | 6000 | 18 | Three pursuing decoys draw existing mob aggression and burst for 50 damage and a Seed each. |
| Seed of Malice | 2 | 25000 | 60 | 40 damage, two Seeds and fear; a 90-second living anchor permits rebirth beside it at 40% health. The anchor loses half current health, capped at 10% maximum for bosses. |
| Slow | 6 | 1000 | 5 | 15 damage and Slowness V to enemies within eight blocks. |
| Die | 6 | 2000 | 6 | 60 + 30 per spent Seed, bleeding; executes non-bosses at or below 30% health with at least three Seeds. |
| Corruption | 6 | 2000 | 8 | 45 damage and two Seeds in a four-block radius; fog deals six per second for five seconds. |
| Confusion | 4 | 5000 | 15 | Six seconds of 40% attack failure, nausea and redirected mob aggression within twelve blocks. |
| Sloth | 4 | 4500 | 12 | Eight seconds of Slowness III, Weakness II, half attack speed and twelve damage per second. |
| Blasphemy | 4 | 6000 | 10 | 130 armor-piercing damage plus twenty per spent Seed; silence for three seconds plus one per Seed. |
| Blather | 3 | 12000 | 8 | Aim within forty blocks or use the most-marked enemy within 100, through terrain. 150 + 40 per spent Seed, confusion, then +20% damage from this caster for twelve seconds. |
| Acid Swamp | 4 | 6000 | 15 | Twelve-second field: fourteen damage per second, slow, three durability wear per pulse and escalating corrosion after continuous exposure. |
| Blood Ignition | 2 | 18000 | 12 | Six seconds of 25 + 10 per spent Seed per second, half splash and no healing. Boss damage is capped at 3% maximum health per pulse after multipliers. |
| Blood Burst | 2 | 15000 | 10 | Pay 20% current health, refused below six: 120 damage, knockback, poison and corrosion in a 4.5-block radius; Regeneration II. |
| Tide of Filth | 1 | 30000 | 20 | Advancing 20-block-wide wave travels 24 blocks: 150 damage once per target, corrosion and knockback. Eight-second sludge field slows and deals twelve per second; six hands hold enemies. |
| The Corroder | 1 | 30000 | 15 | Four-second rooted channel: 30, 60, 120, 240 damage. Continuously crossed hostile domes/walls weaken immediately and break after two seconds; replacement barriers start fresh. |
| Abyssal Sea | 0 | 50000 | 120 | Sixty-second sealed sphere, radius sixteen: growing Seeds and fifteen damage per Seed each second; periodic fear at five Seeds. All entries cost half inside. Free dismissal. |
| The Abyss | 0 | 40000 | 90 | Thirty-second form: +50% damage, 40% reduction, Seeds and 5% lifesteal on damage, recurring fear, and 25% lower costs. Free dismissal. |

## Shared mechanics

Depravity belongs to its caster, caps at five Seeds and increases that caster's damage by 4% per Seed. One Seed falls off every six seconds (12 at Sequence 0); applying Seeds refreshes that interval. Cleansing removes Seeds and the pathway's burns. From Sequence 5, every third ordinary melee hit against the same enemy adds a Seed.

Enemy kills from Sequence 7 grant Offerings, capped at five, each lasting 60 seconds independently. Devil Worship reads the current count without consuming it. With no Offerings, a projection has half strength and lasts ten seconds; otherwise it lasts twenty seconds. The Offering count appears in the HUD. Projections use the existing construct ownership, ally targeting and no-drop systems.

Ordinary burn damage respects fire immunity, Fire Resistance and water/rain. Hellfire soul burn bypasses those protections; the Abyss channel retains its non-fire damage against protected targets. Fire and eruption visuals do not place or destroy terrain. Flame Cage uses the shared Wall system for entity projectiles; custom spell beams are governed by their own collision systems. The Abyss torrent uses a fixed initial direction and can penetrate terrain.

All attacks use shared enemy checks and respect player teams, creative/spectator protection and friendly constructs. Delayed damage, summons, fields, forms and owned controls are cleared on death, logout, dimension change, or pathway/Sequence change. Clearing a caster preserves other casters' Seeds, roots and newer taunts. Silence interrupts the Abyss channel; it releases only the channel's own movement restriction. No other ability can be cast during either rooted channel. The Corroder requires a visible target and ends when it dies, leaves range or the channel is interrupted.

The first three direct keys are registered for existing stolen/borrowed ability systems. Wheels require the native pathway and their individual entries are not copyable. The shared helpers accept living entities where applicable; this pathway currently exposes player casts and owned summons, without an autonomous Beyonder mob casting profile.

## Passive progression and adaptations

| From Sequence | Passive |
|---|---|
| 9 | Strength I; private instinct warning when an unseen mob starts hunting the caster. Six visual melee beats reset after forty ticks without a hit. |
| 8 | Four extra maximum health; fear and nausea durations halve. |
| 7 | Interference blocks the shared Information queries. |
| 6 | Resistance I, Speed I, poison immunity and half fire/curse-like magic damage. Private danger warnings track hunters within 32 blocks; marked attackers deal 10% less damage. |
| 5 | Every third ordinary melee hit on the same enemy plants a Seed. |
| 4 | Attack-failure mind effects halve in duration. Danger detection extends to 48 blocks and also detects approaching hostile mobs; the common Sequence saturation passive applies. |
| 3 | Each owned Seed rots its target for two base damage per second. |
| 2 | Bleeding/corrosion immunity and one health per second below half health. Single Drop prevents death once per five minutes, grants three seconds of invulnerable reformation, then restores 30% health. |
| 1 | King of Filth suppresses nearby enemies' unattributed healing and drains one base health per second. Attributed `Healing.apply` spells still work; White Angel targets are exempt, and other purification effects can register an exemption. |
| 0 | Nearby enemy deaths heal 5% maximum health; the first incoming hostile blow per ten seconds fears its source. Seeds decay every twelve seconds. |

Desire Symbiosis resolves before Seed of Malice, then Single Drop. An existing ward from another pathway is preserved. Links validate their target immediately when damage/death occurs. Malice needs a safe landing beside its living anchor in the same dimension. Wards respect the spell kit's resurrection prohibitions and do not defeat void or `/kill`. Single Drop's cooldown survives logout and player cloning.

Abyssal Sea and The Abyss discounts multiply: together, abilities cost 37.5%, rounded up. The kingdom contains its owner, allies, enemies and entity projectiles. The Corroder weakens shared `Dome`/`Wall` barriers; custom barriers must integrate with those APIs to participate. Recasting a field replaces the previous field of the same kind.

Danger prediction uses current mob targeting and approach rather than attempting to simulate future decisions. Minecraft does not distinguish every vanilla healing cause; the aura treats calls outside `Healing.apply` as natural/unattributed recovery. New healing abilities should use that shared entry point. The Abyss uses the animated shifting silhouette for every viewer; individualized fear/desire appearances and the full mythical forms intentionally excluded by the supplied design are not added.

## Extending the implementation

See [reusable combat helpers](../../SPELL_KIT_LIFECYCLES.md) for channel, blood-price, ward/link ownership, attack-tempo, boss-cap, armor-wear and barrier examples. The earlier flame channel and fog now use those helpers, as do Dark Angel's ritual blood cost and Thunder God's armor-wearing rain and storms. The original APIs remain compatible with older pathways.

## Validation

Twenty-two dedicated GameTests exercise all 35 casts on a real server, including payment and unlocks, borrowed direct casts, Seed ownership, ally protection, projectiles, summons, forms, fields, warning timing, channels, conditional resurrection, boss caps, barriers, discounts, passive progression and cleanup. Run with the repository's required Java toolchain:

```sh
./gradlew runGameTestServer -PfilthyMonarchTests --no-configuration-cache
./gradlew build --no-configuration-cache
python tools/model_gen/filthy_monarch/validate.py
```

Client-side visual quality and live multiplayer balance still need playtesting.
