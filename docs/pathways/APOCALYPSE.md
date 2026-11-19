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

## Ability key combinations

Use the normal ability slots. A chord accepts either press order within three ticks while the first key is down. For an ordered combo, tap and release the first key and press the second within eight ticks of the first press. Remapping the slots also remaps the combos.

| Technique | Sequence | Slots | Effect | Base Spirituality / cooldown |
| --- | --- | --- | --- | --- |
| Obsidian Lance | 7 | Hold 2 + 3 | Release either key to pierce enemies along your sightline up to 28 blocks, applying a flame seal to each. At 0/2/4/6 seconds it deals 18/30/42/54 damage to up to 1/2/3/4 enemies. At six seconds it fires automatically. An unaffordable installment fires the last paid tier. | 400 on activation, then 400 every two seconds (1,600 total) / 8s |
| Widow’s Crossing | 6 | 1 + 4 | Stretch a visible tripwire from your nearest shard to the aimed ground, with a clear span of 1–24 blocks. The first enemy caught gains a silk seal, is reeled toward the front of the mirror, then rooted for two seconds. The line shares the Trip-web limit and ends if its shard disappears. | 1,100 / 10s |
| Fever Thread | 5 | Tap 4, press 5 | Infect an aimed enemy carrying your silk seal. After one second, or on its death, the plague jumps once to the nearest other enemy within six blocks. Each infection applies your plague seal. | 2,200 / 12s |
| Mirror Wake | 4 | 3 + 6 | Aim at a charged shard within 24 blocks. It spends half its charge to fire a ten-block fan in the direction the shard faces. Damage scales from 20 to 50. The shard remains standing. | 3,200 / 10s |
| Omen of Ruin | 3 | Tap 5, press 7 | Claim an aimed enemy carrying your unharvested Despair. For five seconds, your next successful catastrophe within six blocks of the marked position gains 25 percentage points of power, added to any mirror bonus. Opening the wheel, a refused cast, or a distant impact preserves the omen. | 6,000 / 18s |
| False Footsteps | 7 | 1 + 3 | A reflection wearing your skin walks ahead for 3s. Hostile mobs within 8 blocks pursue it. | 700 / 10s |
| Black Wick | 6 | 2 + 4 | Ignite the nearest end of your own tripwire within 24 blocks. The flame travels along it, dealing 18 damage and applying a flame seal once per enemy. The wire can still catch an enemy. | 1,200 / 10s |
| Cruel Accounting | 5 | Tap 2, press 5 | Settle your Curse on an enemy within 24 blocks. It takes 75% of the stored damage immediately and the debt ends. | 1,800 / 12s |
| Borrowed Tomorrow | 4 | Tap 1, press 6 | Transfer your active Unaging ward to an unwarded ally within 24 blocks. They receive up to 8s of the remaining duration and revive at half health. | 3,500 / 24s |
| Eye of the Storm | 3 | 6 + 7 | Mark a 3-block circle on ground within 40 blocks for 5s. When you cast your next catastrophe, allies in the circle keep protection for that entire cast. Other players can join the refuge by crouching inside at cast time. | 5,000 / 20s |

As a Demoness, enter `/sequence` to open the ability guide. Browse Abilities, Combos, Silk, Disasters and Passives. The cards use your current bindings and show future unlocks. Hover or focus a card for costs, timings, damage and conditions. Use the arrows or mouse wheel to change pages. `/sequence info` shows your character statistics.

Combat seals, harvest windows and ambush preparations reset on death, logout and dimension changes.
They are not saved across server restarts. Existing saves discard the old counter and stack pictures.
Beyonder mobs use the same affliction, ambush and mirror-fuel rules.

Run the focused real-server checks with `./gradlew runGameTestServer -PapocalypseTests`.
