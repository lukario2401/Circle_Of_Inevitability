# Sealed artifacts

This is the shared guide for sealed artifacts and dangerous mystical items adapted from *Lord of the
Mysteries* and *Circle of Inevitability*. Simple artifacts belong here; give an artifact its own guide
only when its mechanics are large enough to need one. Exact combat values and containment rules below
are game adaptations rather than claims about the novels' numerical rules.

## Obtaining and using artifacts

Open the **Sealed Artifacts** creative tab or use `/give` with an item's id. Artifacts currently have
no crafting recipes, survival loot source or Curios requirement. They work with any pathway, including
players without a pathway. Right-click in either hand to activate. Tooltips explain powers and risks.
They stack to one and resist fire as dropped items; they can still be lost to other normal item hazards.

The server decides every activation. Ability locks and roots blocking abilities or item use prevent
activation. Rejected uses do not consume cooldown or apply activation costs. Cooldowns belong to the
bearer and artifact type: extra copies, switching hands or replacing the stack do not bypass them.
Cooldown deadlines and drawback state are saved in Forge's persisted player data, including across
death and reconnects. Each new bearer has their own state. Ordinary pathway cooldown modifiers and
copied spells do not shorten, bypass or reset artifact cooldowns.

## Sun Brooch

**Item id:** `circle_of_inevitability:sun_brooch`

```text
/give @s circle_of_inevitability:sun_brooch
```

A gold Sun-pathway brooch inspired by Klein's mystical item in LOTM: purification powers accompanied
by oppressive heat. It has no Church serial number here; **3-078 is the Mutated Sun Sacred Emblem,
not this brooch**. This implementation selects a compact set of powers rather than reproducing every
spell the novel's brooch can provide.

| Action or condition | Game behavior |
| --- | --- |
| Right-click | A bright purification pulse reaches bodies within 6 blocks, requiring line of sight. |
| Valid targets | Undead and monsters recognized by `Spells.isEvil`, plus future registered evil types. |
| Damage | 16 base damage to undead, 8 to other valid targets, before normal defenses. |
| Reveal | Affected enemies glow for 5 seconds. |
| Self purification | Removes vanilla Poison and Wither; gives 10 seconds of Night Vision. |
| Cooldown | 8 seconds, including when there is nothing nearby to damage. |
| Carrying | Builds 1 heat per second in inventory, either hand or equipment. |
| Activation | Adds 20 heat, capped at 100. |
| Heat at 60 or higher | Weakness I, plus 0.4 food exhaustion per second while still carrying it. |
| Heat at 100 | Also Slowness I while carrying it. |
| Water/rain while carrying | Reduces heat by 5 per second instead of increasing it. |
| No longer carrying | Lingering heat falls by 2 per second. Store it in a chest, ender chest or other external storage. |
| Creative/spectator | Heat is cleared; creative activation has no heat cost. |

The pulse excludes the bearer, teammates, owned companions and other allies recognized by `Spells`.
Neutral animals and ordinary players are not targets. It does not break blocks or start fires.
Its 8/16 damage is a spell strike, so ability-hit rules and ordinary defenses still apply. Cleansing
Poison/Wither does not remove artifact heat, weakness or slowness. Purification remains usable when hot;
manage the accumulated heat rather than expecting the artifact to lock itself.

Heat is shared across copies on one player and ticks only once per second, even when duplicates are
held in both hands. The heat meter appears above the hotbar while held; a chat warning appears when
crossing 60. Putting it away stops renewing the adverse potion effects, which expire within 2 seconds,
while heat itself cools gradually. Retaking it before cooling can immediately renew those effects.
Disconnected/dead players do not receive carrying ticks. Inventory items inside another container
item are not recursively inspected; containment there suppresses carrying heat in this adaptation.

At 0 heat, dry carrying alone reaches 60 in about one minute. Three activations can reach that threshold
sooner. Water cools a fully heated bearer below 60 in nine seconds, provided they do not activate it
again. Normal food/exhaustion rules and peaceful difficulty can affect the hunger consequence.

## Adding another artifact

The reusable implementation is in `item/artifacts/`:

| File | Responsibility |
| --- | --- |
| `SealedArtifactItem` | Shared right-click checks, fixed server cooldown, item properties/lore and saved state. |
| `SealedArtifacts` | Once-per-second inventory/equipment discovery; deduplicates by item type. |
| `SunBroochItem` | This artifact's power, visuals and heat rules. |
| `SealedArtifactGameTests` | Real-server tests for powers, gating, cooldowns, inventory effects and allies. |

1. Subclass `SealedArtifactItem`. Pass registered item properties, a unique artifact id, a cooldown
   in ticks and translated tooltip keys to its constructor. All artifacts automatically stack to one,
   resist fire as item entities and receive static tooltip lore.
2. Implement `activate(ServerLevel, ServerPlayer, ItemStack)`. Return `true` only after activation
   succeeds. The base starts the fixed cooldown and client cooldown overlay afterward. Return `false`
   on a refused cast; do target/payment checks before mutating state. Use `player.isShiftKeyDown()`
   inside the override if an artifact needs an alternate action.
3. For a carrying drawback, override `carriedTick(ServerLevel, ServerPlayer, boolean carried)`.
   It runs once per second per player for every registered artifact type. It also runs with
   `carried == false` so lingering drawbacks can recover. Handle creative/spectator exceptions in
   that override. It may run before the player has ever owned the artifact, so absent/default state
   must do nothing harmful.
4. Use `state(player)` for an artifact-specific mutable `CompoundTag`. This data is already namespaced
   by artifact id and lives in `ServerPlayer.PERSISTED_NBT_TAG`; no new global player map or clone
   event handler is needed. **Reserve `ready_at` for the base cooldown.** Store your own fields under
   other names. Do not store player references in static fields.
5. Register the item in `ModItems` using `properties(name)`, then add `items/<name>.json`,
   `models/item/<name>.json`, `textures/item/<name>.png` and translated names/tooltips in `lang/en_us.json`.
   `SealedArtifacts` discovers the subclass automatically; the Sealed Artifacts creative tab includes
   every registered subclass automatically as well. No extra ticking/tab registration is needed.
6. Reuse `Spells` targeting/ally/damage helpers instead of inventing another relationship system.
   Add an entry to this file describing acquisition, targeting, costs, drawbacks and containment.
   Add focused real-server tests for the new gameplay.

The Sun Brooch's original 32px icon can be regenerated with `python tools/item_art/sun_brooch.py`
(Pillow required). This affects only that icon; it does not generate other artifacts' resources.

## Validation

Build with `./gradlew build`. Run the artifact game tests with:

```text
./gradlew runGameTestServer --args="--tests circle_of_inevitability:artifact_*"
```

They check no-pathway activation, undead damage/reveal, neutral-animal safety, self cleansing,
shared duplicate/offhand cooldowns, refused-use costs, persisted state placement, cooldown expiry,
once-per-second duplicate heat, offhand carrying, heat penalties/caps, removal cooling, real-water cooling, creative
exemption, ability-root blocking, walls and allied undead safety.

For manual play, obtain the brooch in survival, use both hands, inspect the golden pulse and item
texture, and wait for the heat thresholds. Enter water or stand in exposed rain to cool it, then
put it in a chest and pick it back up. Reconnect and die/respawn with keepInventory enabled to check
saved heat/cooldown behavior. Confirm a teammate's undead companion remains unaffected.
