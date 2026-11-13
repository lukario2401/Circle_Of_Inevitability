# Apocalypse: weave suffering, harvest it, feed the mirrors

The old “20 abilities cause a catastrophe” passive has been replaced by four interacting combat mechanics.
The seven keys, Sequence unlocks, spirituality prices and catastrophe selection remain available.

## Weave Despair

Put **black flame, hostile silk and plague** on the same enemy within eight seconds. Each family adds one
seal, regardless of how often you repeat it. Completing all three creates **Despair for six seconds**:
the victim cannot heal and suffers Weakness II. Colored rune circles show each seal; a curse mark and
an action-bar message announce completion. Each caster weaves their own seals.

Black flame includes creeping burns and flame rain. Silk includes Silk Snare catching someone,
Trip-web snapping, hostile Cocoon, Web of Fate tangling someone, and a successful Mirror Ambush.
Plague contributes when someone catches the disease, including its later jumps. Curse is still its
separate damage-storage ability. Defensive self-Cocoon and empty casts contribute no seal.

After Despair forms, that caster cannot start another weave on that victim for ten seconds.
Repeating one family cannot trigger Despair by itself. Enemies can break the setup by letting a seal expire.

## Harvest with Fatal Step

Land Fatal Step's empowered melee hit on an enemy carrying **your** Despair before its six seconds run out.
The hit gains **24 damage**, you heal **4 health**, and each of your live mirror shards within **12 blocks
of the victim** gains **25% charge**, capped at full. This spends the harvest window once. Despair's
healing suppression and Weakness retain their original duration.

This lets a single dangerous enemy fuel your mirrors through a successful combo. Allies cannot supply
seals or harvests, and another Apocalypse caster cannot spend your Despair.

## Mirror Ambush

Spend at least **two seconds inside Mirror World**, then exit. Your next melee hit within **three seconds**
gains **12 damage** and lays a silk seal. The HUD shows the opening. Spell damage does not spend it;
an actual melee hit does. An ally hit grants no bonus. Preparing an ambush has a **ten-second cooldown**.
It can combine with Fatal Step on the same hit. Exiting immediately gives no ambush.

## Feed a catastrophe or shatter the mirrors

A successfully cast catastrophe drains your live mirrors within **12 blocks of its aimed impact point**.
Each full shard adds **15% power**, partial shards contribute proportionally, and three full shards give
**45% extra power**. Only that cast gains the bonus. Drained shards stay standing and can recharge.
The action bar reports the bonus. Refused casts and opening a wheel consume no charge.

You now choose between **Shatter's immediate burst**, which destroys the shards, and using their charge
to strengthen a catastrophe while keeping the mirrors for later. Charge earned from enemy positioning
and Despair harvests feeds either choice. Catastrophes are always deliberately cast.

For example: place a mirror facing the fight, ignite a target, catch it with Cocoon, and infect it.
Fatal Step and land the backstab during Despair to heal and charge the mirror. Aim a catastrophe near
that mirror to spend its charge, or save the charged shard for Shatter.

Combat seals, harvest windows and ambush preparations reset on death, logout and dimension changes.
They are not saved across server restarts. Existing saves discard the old counter and stack pictures.
Beyonder mobs use the same affliction, ambush and mirror-fuel rules.

Run the focused real-server checks with `./gradlew runGameTestServer -PapocalypseTests`.
