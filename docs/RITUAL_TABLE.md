# Ritual Table

Right-click a placed Ritual Table to choose a deity. Worship belongs to the player, so several people can use the same sanctuary. Changing faith returns the maximum hearts paid to the previous deity and clears its blessing ranks, friendship, favour, and assignment. Personal cooldowns carry across a change of faith.

## Build the sanctuary

| Altar rank | Build | Friendship cap |
| --- | --- | --- |
| 1 | Place the table | 25 |
| 2 | Center a solid 3×3 floor of polished deepslate immediately beneath it | 60 |
| 3 | Keep the floor. Place chiseled deepslate two blocks north, east, south and west of the table at table height. Put a soul lantern on each pedestal | 100, then higher favour |

Select **Altar structure** to see the next pattern and the coordinates of each missing block. Sparks mark the missing positions in the world. Removing part of the structure lowers the cap for future rites at that table. Earned ranks and paid hearts stay with the worshipper.

## Sacrificial Knife

Craft the knife on a crafting table. Place an iron ingot in the top right, an amethyst shard in the center, and a copper ingot in the bottom left. The knife deals four attack damage, has 128 durability, and accepts iron ingots for anvil repairs.

Hold the knife in your main hand to donate blood at the table or deliver the final blow in a procession. If you carry glass bottles in your offhand, each credited kill fills one Ritual Blood Vial until the assignment is complete. Select the vial under **Offerings** to dedicate it to any deity and recover the empty bottle. Permanent heart sacrifices use the table’s separate confirmation screen.

## Earn friendship

| Action | Reward | Personal cooldown |
| --- | --- | --- |
| Give one preferred item | 2 | 45 seconds, shared across offerings |
| Give five preferred items | 8 | Same offering cooldown |
| Give a rare tribute | 12 | Same offering cooldown |
| Give a Ritual Blood Vial | 4 | Same offering cooldown |
| Donate blood with the knife | 3 when the damage draws current health | 90 seconds |
| Offer one experience level in prayer | 6 | 5 minutes |
| Report a completed assignment | 10–20 for the starting deities | 3–8 minutes, according to the assignment |

God Almighty accepts gold ingots or one golden apple. Lord of the Mysteries accepts amethyst shards or one eye of ender. The Anarchy accepts copper ingots or three emeralds. The offering menu shows the actual gain at the current cap before consuming anything.

An assignment binds to a table when you open it after the previous cooldown. Read **Ritual task** for the objective. Reporting advances the rotation. **Move assignment** starts the current objective over at a different altar. Stat-based tasks begin counting when assigned.

- For a procession, come within 16 blocks of an eligible living mob outside the eight-block ring, within 24 blocks of the table. Give it a moment to be observed, lead it inside, and personally deliver the killing blow with the knife while you are also inside. The altar remembers a quarry for five minutes after observing it outside. Each deity names its eligible creatures.
- For a congregation, gather ten living villagers within eight blocks at the same time. Report while all ten are present.
- Other assignments include deliveries, fishing, using ender pearls, and villager trades. Deliveries consume the listed items when reported.

## Blessings and higher favour

Friendship unlocks the first three ranks at 10, 25, and 50. At 60, 80, and 100, confirm one maximum-heart sacrifice per rank. Those three hearts stay sacrificed across deaths and sessions until a change of faith.

| Covenant | Early blessings | Heart ranks | Replenishable seventh rank |
| --- | --- | --- | --- |
| God Almighty | Night vision in darkness, saturation when hungry, regeneration when wounded | Speed while sprinting, water breathing underwater, strength and resistance in danger | Divine mercy can heal two current hearts |
| Lord of the Mysteries | 4% movement speed, spiritual intuition that outlines a nearby hostile, safer falls | Better movement and sneaking, a brief slow fall, more safe falling distance. Permanent movement speed tops out at +8% | A healing wish and access to the revival miracle |
| The Anarchy | Melee reach, knockback resistance, armor | Block and melee reach, attack damage, a judgment mark, armor toughness and attack speed | Coronation grants extra knockback resistance and can briefly grant strength |

Situational effects are level I. Each worshipper receives at most one pulse every 45 seconds, with eligible effects taking turns. For example, a wounded worshipper of God Almighty can receive regeneration while a submerged one can receive water breathing. Select **Blessings** to see individual rank rewards and durations.

After the third heart sacrifice, report tasks at a rank-three sanctuary to fill a separate 100-point favour pool. One point fades every two minutes of online play. Rank seven is active at 50 or more. Lower favour leaves the six permanent covenant ranks intact.

At 100 favour, a Lord of the Mysteries worshipper can select **Higher favour → Invoke miracle** at a complete sanctuary. The original three sacrificed hearts qualify. The armed miracle lasts while favour remains and rescues its worshipper once from an eligible lethal hit, restoring half of current maximum health. It consumes the remaining favour and starts a two-hour real-time cooldown. The mod’s prohibitions on resurrection and ward-piercing damage still apply. Refill the pool and invoke the miracle again for a later rescue.

## Operators and data packs

Operators can use `/favour add <player> <amount>` with 1–100 points. Before the third heart, the command grants friendship up to the next heart gate. After it, the command grants replenishable favour. The player confirms each heart sacrifice at a suitable altar.

To add a deity, copy a [deity definition](../src/main/resources/data/circle_of_inevitability/circle_of_inevitability/ritual_deity/lord_of_the_mysteries.json) into a data pack at `data/<namespace>/circle_of_inevitability/ritual_deity/<id>.json`. Reopen the world after installing it.

- Set `offering` to the preferred item. Add `tributes` with an item, count, and points. Extra offerings paginate automatically. Every deity accepts one Ritual Blood Vial for four friendship by default. A single-vial tribute can set a different reward.
- Set the first `task` and any subsequent entries in `rotation`. Each sets a count, reward, cooldown in seconds, and player-facing description. A `procession` or `assembly` lists entity types in `targets`. A `delivery` specifies an item. A `statistic` uses a Minecraft scoreboard statistic.
- Give the blessing a name and summary in `per_rank`. Each entry in `stages` unlocks at its specified rank. Add player attribute modifiers or conditional pulses with an effect, duration in ticks, and optional hostile target. Conditions include danger, hunger, injury, darkness, submersion, sprinting, and falling.
- The optional `revival` setting grants access to the miracle. Of the three bundled deities, only Lord of the Mysteries has it. Original data-pack definitions with one statistic and a per-rank attribute remain supported.
