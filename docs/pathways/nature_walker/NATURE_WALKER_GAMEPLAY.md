# Nature Walker gameplay

All 39 active ability entries are playable, from Planter through Mother, including
the higher-sequence upgrades and combat passives. The original uploaded
[NATURE_WALKER.md](NATURE_WALKER.md) remains an unchanged design reference, including
its proposals for later work. [Presentation assets](NATURE_WALKER_ASSETS.md) cover all 39.

## Controls and costs

Set the pathway to `Nature Walker` through the existing sequence command/menu.
The existing ability key bindings unlock as the sequence number decreases.
Normal presses open Harvest, Earth and Alchemy; sneaking repeats the last wheel
selection without its cooldown, at double cost. The direct casts keep their normal
cooldowns and costs. Press key 3 or 5 again to cancel its active channel for free,
even at zero spirituality. Key 7 opens Return at Sequence 3.

| Key | Sequence | Cast | Spirituality | Cooldown |
| --- | --- | --- | ---: | ---: |
| 1 | 9 | Thorn Seed / sneak: Heartfruit Seed | 5 each | 2 s |
| 2 | 8 | Treatment / sneak: Soul Suture | 100 / 200 | 3 / 8 s |
| 3 | 7 | Seed Catalyzation | 700 | 6 s |
| 3 | 7 | Quicken Life | 1,250 | 20 s |
| 3 | 7 | Command Flora | 1,250 | 15 s |
| 3 | 7 | Weather Ritual | 1,250 | 60 s |
| 3 | 7 | Reap | 700 | 4 s |
| 4 | 6 | Cross-breed / sneak: Toxin Bloom | 2,450 / 1,500 | 10 / 6 s |
| 5 | 5 | Underground Travel | 3,000 | 10 s |
| 5 | 5 | Swamp | 3,000 | 20 s |
| 5 | 5 | Wrath of Nature | 3,000 | 30 s |
| 5 | 5 | Child of the Oak | 2,450 | 20 s |
| 5 | 5 | Stone Golem | 3,500 | 30 s |
| 5 | 5 | Bear Form | 3,000 | 40 s |
| 6 | 4 | Stone Bird | 4,500 | 20 s |
| 6 | 4 | Mud Golem | 6,000 | 40 s |
| 6 | 4 | Steel Golem | 7,500 | 60 s |
| 6 | 4 | Life Aura / Mutate | 6,000 / 3,000 | 40 / 8 s |
| 6 | 4 | Brood Form (complete from 2) | 6,000 | 90 / 180 s |
| 7 | 3 | Evil Spirits / Descent of Death | 8,000 / 10,000 | 30 / 45 s |
| 7 | 3 | Return to Earth / Maternal Embrace | 6,000 / 9,000 | 30 / 40 s |
| 3 | 2 | Blossoming Land / Barren Land | 10,000 each | 90 s |
| 7 | 2 | Desolation / Root Eruption | 15,000 / 12,000 | 60 / 20 s |
| 6 | 2 | Seed of Rebirth | 12,000 | 60 s |
| 5 | 1 | Nature Walk (upgraded Underground Travel) | 6,000 | 10 s |
| 5 | 1 | World of Nature / Verdant Barrier | 25,000 / 9,000 | 300 / 60 s |
| 3 | 0 | Great Harvest | 20,000 | 60 s |
| 6 | 0 | Awaken | 20,000 | 90 s |
| 5 | 0 | Earthquake | 30,000 | 180 s |
| 7 | 0 | Embrace of the Earth / Bountiful Earth | 15,000 / 60,000 | 60 / 300 s |

The seed and Sequence 8 costs replace the brief's placeholders: actual base
spirituality is 20 at Sequence 9 and 400 at Sequence 8. All casts refuse without
payment when locked, cooling down, missing required targets, or lacking valid
planting/summon terrain. These entries currently require the owner's pathway;
copying and stealing their live state is disabled.

## Implemented systems

- Seeds grow, bloom, wither and respect a per-owner cap. Adjacent Thornling and
  Heartfruit combine into a Briar Orchard. Fruit automatically heals a nearby
  injured ally or its owner once; Reap harvests plants and refunds up to 500.
- Verdancy scales growth speed, healing, life-effect duration/radius and construct
  health/damage. Rain accelerates growth. Quicken channels and advances growths.
- Weather Ritual creates a local support area; it never changes dimension weather.
  Growths cannot be attacked and all terrain effects leave blocks intact.
- Flora, hybrids and constructs have independent caps. Hybrid growth traits give
  bleeding/reflection, nearby healing, or Madness. Animals gain an additional
  health multiplier. From Sequence 4 hybrids stay until their owner/session ends.
- Golems use iron-golem bodies and the bird a vex body, with invisible base bodies
  covered by animated FX models. Construct caps replace the oldest across types.
- Underground Travel hides the caster, waits 20 ticks, then makes ten safe 2.4-block
  hops. Each destination must be loaded, supported, inside the border and have room
  for the caster. Obstruction ends travel at the last safe location. Early surfacing
  and natural completion apply the emergence hit and fall protection.
- Swamp applies increasingly deep slowness, jump restriction and one burial per
  enemy. Wrath selects terrain hazards each second. Oak switches wood/stone armor;
  wood regrows one absorption point every seven undamaged ticks. Bear changes size,
  health, damage, reach, step height and damage resistance, with a fear roar.
- Lower-sequence passives include Strength, crop growth, weather forecasting,
  spirit vision, observation dossiers, poison/disease resistance, Green Thumb,
  Life Sense and healing on natural ground. Healing uses the existing event pipeline
  so NoHeal, Decree and Inversion remain effective.
- Players and assigned Beyonder mobs use the same spell implementations. Each
  active entry has mob-casting metadata; natural random mob pathway tables are
  unchanged. Use the existing `/beyondermob` controls to assign or test this pathway.
- Gardens, channels, armor, forms and companions end on death, logout, removal,
  dimension/pathway/sequence changes or level unload. They are session state, not
  persistent progression. Hybrid cleanup restores original maximum health and
  releases the controlled creature. Independent roots and other owners survive.

## Higher-sequence behavior and design choices

Mutation is source-owned: three mutagen roll one level (six on bosses), with
Organs, Fungus, Wheat and Vines variants. Life Aura and Brood Form roll levels
directly. Reap and Great Harvest leave Heartfruit from Wheat-mutated enemies.

Return raises consumed fallen bodies as capped spirits; its fields apply decay,
corrosion and healing suppression. Maternal Embrace owns its restrictions. Root
Eruption waits 40 ticks before striking and is cancelled if its owner is lost.
Seed of Rebirth consumes its ward on a lethal hit, holds a hidden seed for 120
ticks, then restores 60% health and heals nearby allies. Realm wards are granted
once per ally and cleanup preserves wards installed later by other abilities.

Nature Walk reaches 60 blocks, doubles travel speed in water, and connects owned
growths within 100 blocks: stand within 3 blocks of one and look closely toward
another. Every exit must still be loaded and safe. World of Nature loops escaping
enemies; both realms collapse if the owner leaves their radius by more than ten
blocks. Their temporary thorns have separate caps and disappear with the realm.
Verdant Barrier renews on at most five blocks of an enemy or projectile.

Awaken doubles a construct's health and damage and keeps it alive while the owner
remains; hybrids gain a second trait, and plants can become a Mother Tree. The
higher Cross-breed accepts non-boss creatures up to 400 health and consumes two
growths. Growths, spirits, awakened companions and wards remain session-owned.

Life Deprivation and the death trials never execute players. Natural bosses and
high-health bosses receive bounded damage instead of execution. Mother's Domain,
Her Presence, natural damage immunity and underwater breathing use the existing
status pipeline. From Druid, every second lost food point is refunded; from
Desolate Matriarch food is kept full. This does not change vanilla saturation.
Weather remains local; no gender/skin change or prayer messaging is introduced.

[Extension guide](NATURE_WALKER_EXTENSION_GUIDE.md) documents the shared growth,
mutation, healing, corrosion, companion and ward APIs, with examples for future
abilities and other pathways.

## Validation

Run `./gradlew runGameTestServer -PnatureWalkerTests` for the focused server suite.
It covers all 39 entries across nineteen scenarios, payment/refusal, channels, growth lifecycle, healing,
control-group isolation, terrain hazards, safe travel, construct replacement,
forms, realms, rebirth, mutations, upgrades, owner cleanup and mob registration. Run `./gradlew build` for the ordinary
checks and `python3 tools/model_gen/nature_walker/validate.py` for asset contracts.
Client rendering, audio balance and live multiplayer playtesting remain manual.
