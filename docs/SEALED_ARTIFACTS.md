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

## Creeping Hunger

**Item ids:** `circle_of_inevitability:creeping_hunger_1` through `creeping_hunger_5` (one item per tier)

```text
/give @s circle_of_inevitability:creeping_hunger_1
```

A living glove inspired by LOTM's Creeping Hunger: it devours the Beyonders its bearer kills and lends
their powers to anyone, with no pathway, Sequence or spirituality needed. The price is its hunger.

| Action | Game behavior |
| --- | --- |
| Kill a Beyonder while carrying it | Tiers I-II devour one of the victim's abilities at random. Tiers III-V open a scrollable list of the victim's abilities to choose from: one at tier III, up to two at tier IV, up to three at tier V (tick them, then **Devour**). Abilities that cannot fit are greyed out; when the glove is full, a list per extra pick asks which stored ability it replaces. Esc takes nothing. |
| Right-click | Casts the selected stored ability for free. Cooldown follows the ability's Sequence: 2 s for Sequence 9 up to 20 s for Sequence 0. A cast that finds nothing to act on costs nothing. |
| Sneak + right-click | A scrollable list of the stored abilities: click one to select it. Empty slots show which Sequences they still accept. **Discard...** opens a list where you tick any number to free. |

Abilities are saved on the glove itself, so they stay with it when it is dropped, traded or stored. Only
abilities that can be copied (the same ones Key Of Stars and the Codex can learn) are taken.

| Tier | Slots | Downsides (each tier adds one; all grow stronger) |
| --- | --- | --- |
| I | 5, Sequence 5 or weaker | Daily kill or die; corruption (1 per 60 s carried, up to 40) |
| II | 7: one up to Sequence 4, six up to 5 | + hostile mobs within 8 blocks hunt you; corruption 1 per 45 s, up to 55 |
| III | 9: two up to Sequence 2, one up to 4, six up to 5 | + blood thirst; you choose what it devours; mobs within 12; corruption up to 70 |
| IV | 9: three up to Sequence 1, six up to 5 | + disorientation fits (about every 90 s); neutral mobs hunt you too, within 16; corruption up to 80 |
| V | 11, any Sequence | Everything at full strength: fits about every 45 s and darken the world, mobs within 24, corruption up to 90 |

Each tier's slots accept everything the tier below can hold.

**Daily kill or die.** The glove must be fed: kill any mob at least once per in-game day (24000 ticks)
of carrying it, or it kills you (`death.attack.circle_of_inevitability.creeping_hunger`). Warnings appear
at 50%, 75% and 90% with a timer by the hotbar, and from 75% you starve (Hunger I; Hunger III from tier
III). The clock only runs while it is carried: sealing it in a chest pauses it. Death resets it.

**Corruption.** It adds to the soul's corruption (the red tint; harmful from 66) while carried and with
every cast (1 per tier per cast), but never past the tier's cap. Only its own share is tracked: any
cleanse (cleansing abilities, a milk bucket) removes it, putting the glove away drains 2 a second, and
the soul's natural decay removes it over time too.

**Mob aggro.** From tier II, every two seconds nearby hostile mobs target you; from tier IV neutral
mobs (wolves, golems, bees...) join them. Your own pets, team and friends are spared.

**Blood thirst.** From tier III, casting a stored ability makes you thirst: drink a **Mob Blood Vial**
before that in-game day ends (at least 2 minutes). Collect one by killing a mob with an empty glass
bottle in your off hand, then right-click it; vials are only drunk while you thirst. Past the deadline
you are parched, carried or not: Hunger, Weakness and damage every 3 seconds, and the glove refuses to
cast until you drink.

**Disorientation.** From tier IV, carrying it sometimes brings a fit: veins cover the screen, Nausea and
Slowness, a stumble, and at tier V Darkness.

Creative and spectator players are spared every downside. All numbers are in `item/artifacts/CreepingHunger.java`.
In the hand it is a 3D clawed glove with a fanged mouth on the palm; higher tiers grow more eyes and bone
spikes. The models and their textures are generated by `python tools/model_gen/creeping_hunger/generate.py`.
The flat icons (used as the model's particle texture) and the screen overlay come from
`python tools/item_art/creeping_hunger.py`. Tests: `./gradlew runGameTestServer -PcreepingHungerTests`.

## The Monocle

**Item id:** `circle_of_inevitability:monocle`

```text
/give @s circle_of_inevitability:monocle
```

A gold monocle worn in the **helmet slot** (right-click it in hand to put it on, or place it in the head slot).
Through it you see more the longer you wear it, and you are slowly replaced by whatever looks back. Its powers
grow with time worn; so does the price.

| Time worn | Power |
| --- | --- |
| From the start: **Spirit Vision** | Night Vision; living things within 20 blocks glow; whatever you look at (up to 40 blocks, through walls) is read on the action bar: health, whether it is hunting you or is a friend, its Pathway and Sequence, and whether its strings are held. **Gaze** (key, default **B**) reads a target within 24 blocks in full in chat, including its abilities, and makes it glow for 30 s. 3 s cooldown. |
| 5 minutes: **Mystery Prying** | Ores within 7 blocks shimmer purple, for the wearer only. **Foresight**: a hit of a third of your max health or more is seen coming and never lands, once a minute. |
| 10 minutes: **Spirit Body Threads** | Gaze on a mob (not a friend, at most 100 max health) pulls its threads: it becomes your marionette for 60 s (status/Marionette, up to its usual cap). 30 s cooldown. Sneak + Gaze still reads instead. A marionette control bar now hangs above *your* head. |

**Losing yourself.** "Lost" time rises by one second for every second the Monocle is worn and falls by one second
for every second it is not (carried in the inventory or stored, it does nothing). Each Gaze adds 5 s and each pull of
threads 30 s. From 2 minutes your body starts acting on its own: the chance each second grows from 1 in 60 to 1 in 3
at the end. What it does gets worse as you go:

| Lost | Acts that can happen |
| --- | --- |
| 2 min+ | glance somewhere else, twitch an arm, mutter (only you hear it) |
| 5 min+ | + a stumbling step, a jump, switching the held hotbar slot |
| 10 min+ | + staring straight up, wandering off, dropping what you hold, laughing (players within 16 blocks see it) |
| 15 min+ | + striking whatever is nearest within 3.5 blocks, friend or foe |

Warnings and a "Yourself" timer appear at 50%, 75% and 90%. At **20 minutes** the Monocle kills its wearer
(`death.attack.circle_of_inevitability.monocle`); death resets the clock. Creative and spectator wearers still
progress (to try the powers) but never act on their own or die of it.

All numbers are constants in `item/artifacts/MonocleItem.java`. The worn model is `client/armor/MonocleModel`;
its texture, the 3D item model and the equipment asset come from `python tools/model_gen/monocle/generate.py`.
Tests: `./gradlew runGameTestServer -PmonocleTests`.

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

### Ability-storing, tiered artifacts (like Creeping Hunger)

`item/artifacts/storing/` is a framework for artifacts that store abilities taken from kills, with tiers
and downsides. A new one is a single declaration, with no new item class or event code:

```java
public static final StoringArtifact MY_RELIC = StoringArtifact.create("my_relic", "My Relic")
        .tier(AbilitySlots.of(3, 7))                         // tier 1: three slots, Sequence 7 or weaker
        .tier(AbilitySlots.of(1, 5).and(3, 7))               // tier 2: one up to Sequence 5, three up to 7
        .tier(AbilitySlots.any(6))                           // tier 3: six of any Sequence
        .chooseOnKillFrom(2)                                 // tier 2+: the bearer picks what to take
        .picksPerKill(1, 1, 2)                               // optional: how many per kill, by tier
        .cooldown(sequence -> 20 * (10 - sequence))          // optional, ticks per cast by Sequence
        .drawback(new FeedOrDieDrawback(1, 24000, 3))        // each downside from some tier,
        .drawback(new AggroDrawback(2, new int[]{0, 6, 10}, 3))   // values indexed by tier
        .effects(new StoringArtifact.Effects() { ... });    // optional particles/sounds
```

Register it in `ModItems` with `MY_RELIC.register(ITEMS, name -> properties(name).rarity(Rarity.EPIC))`;
that creates `my_relic_1`, `my_relic_2`, ... Then add for each tier `items/` and `models/item/` JSON and a
texture, the lang keys `item.circle_of_inevitability.my_relic_<tier>`,
`tooltip.circle_of_inevitability.my_relic.power` and `.containment`, and a damage type
`data/circle_of_inevitability/damage_type/my_relic.json` with its `death.attack` message (drawbacks that
hurt the bearer use it). The tooltip lists the slots and that tier's downsides by itself.

| Piece | Responsibility |
| --- | --- |
| `StoringArtifact` | The declaration: tiers, slots, choose-from tier, picks per kill, cooldowns, drawbacks, effects, lore, registration. |
| `StoringArtifactItem` | Shared item: casting the selected ability, the select/discard/devour/replace lists, feeding on kills. |
| `AbilitySlots` | Slot caps per tier and the fit check (the strongest ability takes the tightest slot that accepts it). |
| `StoredAbilities` | The abilities saved on the stack (catalog ids), selection, add/replace/remove. |
| `ArtifactDrawback` | The downside interface: `second`, `killed`, `cast`, `blocksCast`, `cleansed`, `bearerDied`. |
| `FeedOrDieDrawback`, `CorruptionDrawback`, `AggroDrawback`, `BloodThirstDrawback`, `DisorientationDrawback` | Ready-made downsides with per-tier strengths. |
| `StoringArtifacts` | Events: kills, the bearer's death, cleansing. |

Tiers of one artifact share one family id: saved state and cooldown are per player and family, and
only the highest carried tier receives the once-per-second carrying tick (`SealedArtifactItem.rank()`).
The pickers use `menu/AbilityList`, a generic scrollable list that any feature can open (it never overlaps,
however many rows): `AbilityList.create(id).title(...).maxPicks(n).row(icon, name, detail)...onPick((player,
picks) -> ...).open(player)`. With `maxPicks(1)` a click picks; above that rows are ticked and confirmed.
The server re-checks every answer (count, duplicates, locked rows), so `onPick` only sees valid picks.

A new downside implements `ArtifactDrawback`, keeps its values in `Bearer.state()` under its own key
names, and reads its per-tier strength with `bearer.at(values)`.

The Sun Brooch's original 32px icon can be regenerated with `python tools/item_art/sun_brooch.py`
(Pillow required). This affects only that icon; it does not generate other artifacts' resources.

## Validation

Build with `./gradlew build`. Run the artifact game tests with:

```text
./gradlew runGameTestServer -PartifactTests
./gradlew runGameTestServer -PcreepingHungerTests
./gradlew runGameTestServer -PmonocleTests
```

They check no-pathway activation, undead damage/reveal, neutral-animal safety, self cleansing,
shared duplicate/offhand cooldowns, refused-use costs, persisted state placement, cooldown expiry,
once-per-second duplicate heat, offhand carrying, heat penalties/caps, removal cooling, real-water cooling, creative
exemption, ability-root blocking, walls and allied undead safety.
The Creeping Hunger tests check every tier's slot limits (and that upgrading keeps what is stored),
random and chosen devouring, replacing in a full glove, casting without a pathway, Sequence-based
cooldowns, blood thirst and drinking Mob Blood, the daily kill or die clock (pausing when stored),
corruption with cleansing and draining, mob aggro by tier, disorientation fits, and that two carried
tiers tick once at the stronger tier's strength.

For manual play, obtain the brooch in survival, use both hands, inspect the golden pulse and item
texture, and wait for the heat thresholds. Enter water or stand in exposed rain to cool it, then
put it in a chest and pick it back up. Reconnect and die/respawn with keepInventory enabled to check
saved heat/cooldown behavior. Confirm a teammate's undead companion remains unaffected.
