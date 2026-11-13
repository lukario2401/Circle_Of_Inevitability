# Nature Walker

The Mother Pathway of Lord of the Mysteries, Planter (Sequence 9) up to Mother (Sequence 0). This is a design only, with no code. It has the same shape as HAND_OF_GOD.md: seven keys, every Sequence written, numbers named so they can become constants, and the spell-kit parts each ability is built from (docs/SPELL_KIT.md).

Sources: the Mother Pathway and Mother Pathway/Abilities pages of the Lord of the Mysteries wiki, plus AttendantOfMysteries, HandOfGod and PrinceOfAbolition for structure. SEQUENCES.md was not readable while writing this, so every cost is a placeholder on the Attendant's ladder (250 at Sequence 9, 2450 at 6, 3000 at 5, 6000 at 4, 12000 at 3) and needs one balance pass against the real spirituality pools.

Pathway name: `Nature Walker` (the exact string for `PATHWAY`, `SequenceCommands.PATHWAYS` and `AbilityWheel.pathway(...)`).

---

## 1. The idea: the Cycle

The Mother Pathway is the cycle of reality: coming from the earth and returning to the earth. Nature Walker plays it as two hands that feed each other.

- **Life**: plants, healing, creation, mutation.
- **Return**: death, desolation, taking life back into the earth.

The player builds a small board and plays on it: **Growths** (things planted in the world), **Brood** (golems, hybrids and spirits that fight for them) and **Mutation** (what their power does to enemies). Death feeds growth, and growth is harvested back into healing and power.

| Step | What you do | Abilities |
|---|---|---|
| Plant | Put Growths in the world | Thorn Seed, Heartfruit Seed, Seed Catalyzation |
| Quicken | Make them grow faster | Quicken Life, Weather Ritual, Life Aura, Blossoming Land |
| Harvest | Cash them in for healing and damage | Reap, Great Harvest, Heartfruit |
| Return | Take life from enemies and give it to the earth, which blooms | Descent of Death, Return to Earth, Desolation |
| Rebirth | What dies comes back | Evil Spirits, Seed of Rebirth, Bountiful Earth |

How it plays next to the other pathways: Hand Of God is a fighter and Prince Of Abolition is a rule-bender. Nature Walker is a support, summoner and zone-control pathway that is slow to set up and very hard to dislodge once the board is built. Its burst damage is lower than most, and its healing is the best in the mod.

---

## 2. Key map

Seven keys. Key N is gated at Sequence 10 − N like every other Pathway; Sequences 2, 1 and 0 live inside the wheels and as higher forms of earlier abilities (the Hand Of God pattern).

| Key | Name | Gate | Normal | Sneak |
|---|---|---|---|---|
| 1 | Sow | 9 Planter | Thorn Seed | Heartfruit Seed |
| 2 | Treatment | 8 Doctor | Treatment | Soul Suture |
| 3 | Harvest wheel | 7 Harvest Priest | opens the wheel | the last pick again |
| 4 | Biology | 6 Biologist | Cross-breed | Toxin Bloom |
| 5 | Earth wheel | 5 Druid | opens the wheel | the last pick again |
| 6 | Alchemy wheel | 4 Ancient Alchemist | opens the wheel | the last pick again |
| 7 | Return wheel | 3 Pallbearer | opens the wheel | the last pick again |

Wheel convention: normal opens the wheel; sneak casts the last pick again with no cooldown at double cost (`.quickCastCooldown(false).quickCastCost(2f)`, as in Beauty Goddess). The Prince Of Abolition does it the other way round, so this is a one-line switch (question 6).

Wheel contents (each entry's Sequence, cost and numbers are in section 4):

- **Harvest wheel (key 3), 8 entries**: Seed Catalyzation 7, Quicken Life 7, Command Flora 7, Weather Ritual 7, Reap 7, Blossoming Land 2, Barren Land 2, Great Harvest 0.
- **Earth wheel (key 5), 9 entries**: Underground Travel 5 (becomes Nature Walk at Sequence 1 inside the same entry), Swamp 5, Wrath of Nature 5, Child of the Oak 5, Stone Golem 5, Bear Form 5, Verdant Barrier 1, World of Nature 1, Earthquake 0.
- **Alchemy wheel (key 6), 8 entries**: Stone Bird 4, Mud Golem 4, Steel Golem 4, Life Aura 4, Mutate 4, Brood Form 4 (partial form, complete from Sequence 2 in the same entry), Seed of Rebirth 2, Awaken 0.
- **Return wheel (key 7), 8 entries**: Evil Spirits 3, Descent of Death 3, Return to Earth 3, Maternal Embrace 3, Desolation 2, Root Eruption 2, Embrace of the Earth 0, Bountiful Earth 0.

That is 39 abilities. Entries that change at a higher Sequence do it inside one entry (the cast branches), the way Hand Of God's Protection becomes a Cage from Glory.

---

## 3. Shared systems

### 3.1 Growths

A Growth is something you plant that grows through stages on its own and can be harvested. It is the Nature Walker's board, like the mausoleums are the Prince Of Abolition's.

- **Stages**: seed, sprout, bloom, then withered when its time is up. Some kinds have a ripe stage after bloom (Heartfruit).
- **Kinds** (each defined once, with a stage length, a model per stage, a beat and what it does when harvested):

| Kind | Made by | Does |
|---|---|---|
| Thornling | Thorn Seed, Seed Catalyzation | Slows, pricks and bleeds enemies near it |
| Heartfruit | Heartfruit Seed | Bears a fruit that heals whoever eats it |
| Briar Orchard | Thornling and Heartfruit planted within 3 blocks | Both at once, a little stronger (cross-pollination) |
| Sporebed | Toxin Bloom (from Sequence 5) | Leaves a poison and Madness cloud |
| Mother Tree | Awaken, on a Growth (Sequence 0) | A big Heartfruit tree with a thorn aura |

- **Cap**: `3 + 2 × (9 − Sequence)` Growths per Beyonder (3 at Sequence 9, 21 at Sequence 0), shown as a HudCounter `Growths 4/7`.
- **Speed**: every Growth reads one speed rule, so Verdancy, Quicken Life, rain, Life Aura, Blossoming Land and Her Presence all just add to it.
- **Harvest**: Reap, Great Harvest and Heartfruit being eaten each call the kind's harvest effect. Harvesting returns a small spirituality refund (question 4).
- **Rules**: an owner is any `LivingEntity` (so Beyonder mobs can plant), it ends with its owner, and it is not saved. Enemies cannot destroy a Growth in this design (question 3). It is a `Zone` underneath, as `Trap` is.

### 3.2 Mutation (and Madness)

Mutation is what the Ancient Alchemist's power does to enemies: every ability that hurts something leaves it changed. It is a status on the enemy with 1 to 3 levels, built on the existing `Madness` ladder.

- **How it rolls**: every damaging ability of yours that lands adds 1 Madness (`Madness.add`) and 1 Mutagen. Every 3 Mutagen rolls one Mutation level on that enemy for 12 s. Bosses roll at half rate. From Sequence 4 on.
- **Which Mutation** depends on what hit it: poison gives Fungus, thorns give Vines, healing-side hits give Wheat, anything else gives Organs.

| Mutation | Effect, per level |
|---|---|
| Organs | Takes 15% more damage (`Vulnerable`) |
| Fungus | Weakness I; on death its spores spread to the nearest enemies (`Contagion`, Madness +1) |
| Wheat | Slowness; Reap or Great Harvest on it leaves a Heartfruit where it stood |
| Vines | 25% of its attacks miss (`Falter`) and it is rooted for a moment every few seconds |

- **Bloom**: an enemy that is already at level 3 and is hit by Mutate (or Great Harvest) blooms: it takes 15% of its maximum health (capped), and everything within 4 blocks is rooted.
- **Madness break**: `Madness.onBreak` is where a Mother Pathway enemy "loses control": it forgets its target and wanders for 4 s (one callback, registered once).

### 3.3 Brood

Everything made to fight for you is Brood. It is capped by Sequence in three groups so the pathway cannot flood the world (and so `Marionette.MAX_PER_MASTER` does not fight with it).

| Sequence | Constructs (golems, birds) | Hybrids | Spirits |
|---|---|---|---|
| 6 | 0 | 1 | 0 |
| 5 | 1 | 1 | 0 |
| 4 | 2 | 2 | 0 |
| 3 | 3 | 3 | 6 |
| 2 | 9 (the wiki's nine demigod-level constructs) | 5 | 10 |
| 1 | 12 | 8 | 14 |
| 0 | 18 | 12 | 20 |

A new one past the cap ends the oldest (`Construct.limit(...)` already works this way). Hybrids and spirits are `Marionette`s, so the cap needs a small helper that counts them.

### 3.4 Verdancy

The Mother's version of the Prince's Sequence Might. Every Sequence reached adds 6% to healing, Growth speed, the radius and duration of life abilities, and Brood health and damage (Sequence 9 = +6%, Sequence 0 = +60%). One function, `verdancy(player)`, read wherever those numbers are used.

### 3.5 Healing and Terrain (small helpers)

- **Healing**: every heal goes through one call so that `NoHeal`, a Decree's HEALING rule and Inversion all apply, healing numbers show, and later pathways can add rules (see section 6).
- **Terrain**: classifies the ground an entity stands on or looks at as soil, stone, wood, water, sand, snow, other or none. Child of the Oak, Wrath of Nature, Nourished by Earth and Nature Walk all ask it, so it is written once.

---

## 4. Sequence by Sequence

Format: **Name** (key and form, Sequence, cost, cooldown). *Numbers* are in ticks (20 = 1 second) and blocks, before Verdancy. *Built from* names the spell-kit parts.

### Sequence 9: Planter

**Traits** (SequencePassives, a check once a second):

- **Physical Enhancement**: Strength I.
- **Farming Proficiency**: every 5 s, 2 spots within 6 blocks grow as if bone-mealed (`Spells.growPlants`).
- **Weather Forecasting**: a quiet HudTimer shows how long until the weather next changes (`HudTimer`, blue).

**Abilities**

- **Thorn Seed** (key 1 normal, 250, 2 s). Throw a seed where you aim (range 20); it plants a Thornling. Thornling: sprout 20, bloom after 40 more, then stands 240. While it stands, enemies within 2.5 blocks take 3 damage every 10 ticks, Slowness I and 1 bleed a second for 3 s. Cap per section 3.1. *Built from*: `Shot` (seed), Growth, `Zone`, `Bleeding`.
- **Heartfruit Seed** (key 1 sneak, 250, 2 s). Plant a sapling at your feet, or at the ground you aim at within 6 blocks. After 80 ticks it bears one fruit that stays ripe for 300. An ally, or you, within 1.5 blocks eats it: heals 30% of maximum health at once and Regeneration I for 4 s. The sapling withers 100 ticks after the fruit is gone. *Built from*: Growth, `Zone`, Healing.

### Sequence 8: Doctor (Healing Pastor)

**Traits**

- **Spirit Vision**: twice a second a small mark floats over each ally within 24 blocks who is under 50% health (green) or carries a curse (violet: Misfortune, Corrosion, Decay, Madness), seen only by you (`ParticleShapes.spawnFor`). Sneak-looking at any creature for 1 s tells you its health and effects in chat (`Dossier.tell(viewer, target, 1)`, once per 5 s per target).
- **Disease Resistance**: poison and hunger last half as long on you (`EffectDuration.addRule`), nausea does not take (the Twilight Resistance event pattern).

**Abilities**

- **Treatment** (key 2 normal, 300, 3 s). The ally under the crosshair within 12 blocks (`Spells.aimAlly`), or you if none is aimed. Heals 30% of maximum health and cleanses every harmful effect (`Spells.cleanse`). **Given Life**: a target under 30% health is healed 60%, gets Resistance I for 5 s and 4 absorption. *Built from*: `Spells.aimAlly`, Healing, `Spells.cleanse`.
- **Soul Suture** (key 2 sneak, 600, 8 s). On an ally within 12 blocks, or you: ends every curse and mind effect on them (every `Cleansing` listener, `Fear`, `Slumber`, `Silence`, `Blind`, `Corrosion`, `Decay`, `Bleeding`, `NoHeal`) and clears their Madness. On an aimed enemy within 6 blocks it cuts instead of mends: Silence for 3 s and every beneficial effect off (`Dispel.boons`); half the Silence on a boss. *Built from*: `Cleansing`, `Madness.clear`, `Silence`, `Dispel`.

### Sequence 7: Harvest Priest

**Traits**

- **Green Thumb**: thorns, sweet berry bushes and cactus do not hurt you; Heartfruit heals 25% more when you planted it.

**Abilities (Harvest wheel, key 3)**

- **Seed Catalyzation** (700, 6 s). Hands to the ground at the spot you aim at (range 24): every Growth of yours within 6 blocks of it is at full bloom at once, and thorned vines burst from the spot: enemies within 5 blocks take 12 damage and are rooted (MOVE and JUMP) for 60. With no Growth near, a Thornling blooms there instead, so the cast always does something. *Built from*: Growth, `Zone`, `Root`.
- **Quicken Life** (1250, 20 s). A 100-tick aura around you, radius 12: your Growths advance one stage a second, crops grow (`Spells.growPlants`, 6 spots a second), allies get Regeneration I, enemies Slowness I. You cannot move while it lasts, and pressing the key again ends it for free. *Built from*: `Zone.around`, `Root` on yourself.
- **Command Flora** (1250, 15 s). Up to 6 arthropods (spiders, cave spiders, silverfish, endermites, bees) with 40 health or less, within 30 blocks, become your marionettes for 400 (`Marionette.control`). Every Growth of yours within 30 blocks focuses the enemy you aim at for 160: its damage is 50% higher. *Built from*: `Marionette`, Growth.
- **Weather Ritual** (1250, 60 s). A toggle by current weather. With no rain it calls rain over the spot you aim at: radius 24 for 800 ticks, fire goes out, Growths grow twice as fast, allies get Regeneration I. Under rain it clears the sky instead: radius 24 for 600, allies are cleansed of Blind and Darkness and get Speed I. Local or world-wide weather is question 1. *Built from*: `Zone`, `Spells.cleanse`.
- **Reap** (700, 4 s). A scythe sweep: cone range 5, 120 degrees, 30 damage. Every Growth in the cone is harvested (Heartfruit ripens and heals those near it, a Thornling bursts for 15 damage around itself) and 100 spirituality comes back for each, 500 at most. Every enemy hit is Reaped for 160: your Growths and Brood do 15% more to it. *Built from*: `Spells.enemiesInCone`, Growth harvest, `Marks`.

**Strengthened**: Treatment now heals your Brood and your Growths too.

### Sequence 6: Biologist

**Traits**

- **Knowledge (Life)**: the Dossier you read by sneak-looking goes to depth 2 (defenses and weaknesses).
- **Life Sense**: living things within 16 blocks show through walls for you only, every 2 s.
- **Poison Immunity**: poison never takes (the Biological Poison Bottle).

**Abilities**

- **Cross-breed** (key 4 normal, 2450, 10 s). Aim at a mob within 16 blocks that has 80 health or less (not a player, not a boss). It must have one of your Growths within 10 blocks of you; the Growth is consumed. The mob becomes a Hybrid and a marionette for 600 ticks, with 50% more health and a trait from the Growth's kind and the creature's class (table below). With no Growth it refuses and costs nothing. When its time runs out it withers (until Sequence 4 gives it a soul). *Built from*: `Marionette.control`, Growth harvest, a trait table.
- **Toxin Bloom** (key 4 sneak, 1500, 6 s). Toxin from your hair as a cone, range 7, 90 degrees: Poison II for 120 and Weakness I for 120. From Sequence 5 it leaves a Sporebed Growth at the end of the cone: radius 3 for 160, Poison and Nausea to enemies inside. From Sequence 4 each beat of it adds 1 Madness. *Built from*: `Spells.enemiesInCone`, `Spells.keepEffect`, Growth.

**Hybrid traits** (data, like `WeaknessTable`):

| From the Growth | Trait |
|---|---|
| Thornling | Briar: reflects 20% of melee damage taken; its hits cause bleeding |
| Heartfruit | Verdant: heals allies within 4 blocks, 1 health a second |
| Briar Orchard | Both of the above |
| Sporebed | Spore: its hits add 1 Madness; on death it bursts into a `Contagion` of Madness |

The creature's class only changes numbers: melee fighters keep their damage, shooters' arrows cause bleeding, animals get 50% more health again.

### Sequence 5: Druid

**Traits**

- **Nourished by Earth**: while you stand on natural ground (soil, stone, sand) you regenerate 1 health every 3 s and food drains half as fast.

**Abilities (Earth wheel, key 5)**, one for one with the Druid's wiki list:

- **Underground Travel** (3000, 10 s). You sink into the ground in 20 ticks, hidden and unhurt, and travel along your look for up to 24 blocks in short hops (2.4 blocks every 2 ticks, hidden throughout, as the Pale Emperor's Underworld does). You come out where the travel ends: enemies within 3 blocks take 15 damage and are rooted for 30, and you have 60 ticks without fall damage. Pressing the key again surfaces you early. Nothing breaks blocks; a spot with no room to come out ends the travel at the last free spot. At Sequence 1 this same entry becomes Nature Walk. *Built from*: `Concealment`, `Teleport`, `Spells.freeSpotNear`, `FallGuard`.
- **Swamp** (3000, 20 s). The ground at the spot you aim at (range 24, radius 10) turns to mire for 200 ticks. Enemies in it: Slowness I that deepens to III with time in it (`1 + ticksInside / 40`), no jumping after 40 ticks, and at 80 ticks each is buried once (`Burial.bury(e, 20, 40)`). A mud disc is drawn, no blocks change. *Built from*: `Zone`, `Burial`.
- **Wrath of Nature** (3000, 30 s). Name enemies as adversaries: the ones in sight (`Spells.enemiesInSight`, range 24, 45 degrees), nearest first, 5 at Sequence 5, 8 at 4, 12 at 3, 20 at 2, 50 at 1. For 300 ticks each adversary suffers one hazard a second, chosen by the ground around them, so only dangers that fit the place happen:

| Where the adversary is | Hazard |
|---|---|
| On soil or near plants | Vines: 8 damage and rooted for 40 |
| Under stone, in a cave or under a roof | Falling stone: 25 damage |
| Within 2 blocks of a drop of 4 or more | Shoved toward the edge |
| Within 2 blocks of water | Undertow: Heavy II for 60 and 6 damage |
| Raining overhead | Lightning: 30 damage |
| Anywhere else | Sinkhole: buried for 10 |

*Built from*: `Spells.enemiesInSight`, Terrain, `Root`, `Weight`, `Burial`, `Marks`.
- **Child of the Oak** (2450, 20 s). For 600 ticks a layer of bark covers you. Standing on soil or plants it is wood: 30% less damage and a layer of 6 absorption that regrows every 2 s. On rock it is stone: 45% less damage and no knockback. *Built from*: `ConjuredArmor`, Terrain.
- **Stone Golem** (3500, 30 s). A golem rises from the ground where you aim (range 12): 150 health, 18 damage, no knockback, 1800 ticks, leashed to 40 blocks. *Built from*: `Construct.of(...).build(...)`, Brood cap.
- **Bear Form** (3000, 40 s). Twice your height for 400 ticks: more reach and step, 12 more maximum health, 20% less damage taken, claws that add 10 damage and a block of reach. The roar on casting frightens mobs with 60 health or less within 6 blocks for 40. 100 ticks of Weakness I afterwards. *Built from*: `Form`, `GiantForm`, `ConjuredWeapon`, `Fear`.

**Strengthened**: Seed Catalyzation's vines come out dark green and mutated: radius 7, rooted for 100, and they make the enemy bleed.

### Sequence 4: Ancient Alchemist

**Traits**

- **Mutation**: see 3.2. Every damaging ability now mutates.
- **Souls for Cross-breeds**: a Hybrid no longer withers when its time runs out; it stays as a permanent companion and counts toward the Hybrid cap.
- **Creature Commanding**: Command Flora now takes any mob with 60 health or less that is not humanoid or a boss, and it lowers the Madness of your allies near it.
- **Strengthened Healing**: Treatment heals 60% (Given Life 100%), also ends `NoHeal` and `Decay`, and gives Regeneration III for 3 s.

**Abilities (Alchemy wheel, key 6)**

- **Stone Bird** (4500, 20 s). A flying stone bird with ruby eyes: 60 health, 10 damage, 1800 ticks; what it hits glows for your allies. *Built from*: `Construct` (a vex body), `Spells.keepEffect`.
- **Mud Golem** (6000, 40 s). Slow and tough: 300 health, 14 damage, its blows give Slowness II and Heavy I; it heals 3 health a second while it stands in your Swamp or in water. *Built from*: `Construct`, `Weight`.
- **Steel Golem** (7500, 60 s). 450 health, 30 damage, no knockback, 2400 ticks. *Built from*: `Construct`.
- **Life Aura** (6000, 40 s). An aura round you, radius 14, 400 ticks: allies get Regeneration II and Speed I, your Brood heals 2 health a second, Growths grow four times as fast. Enemies have a Mutation rolled on them every 80 ticks and gain 1 Madness a beat; life so strong that it deforms. *Built from*: `Zone.around`, Mutation.
- **Mutate** (3000, 8 s). The enemy you aim at (range 20) gets a level 2 Mutation at once and 3 Madness. If it is already at level 3 it blooms (see 3.2). *Built from*: Mutation, `Madness`.
- **Brood Form, partial** (6000, 90 s). The incomplete Mythical Creature Form: 160 ticks, 1.6 times your size, every enemy within 6 blocks gets a Mutation roll each second. Its madness builds 1/8 a second and breaks the form at full. *Built from*: `Form`, `GiantForm.madness(...)`.

### Sequence 3: Pallbearer

**Traits**

- **Life Deprivation**: every hit you land heals you for 12% of the damage and starts a small `Decay` on the target. A mob under 20% health has a 50% chance on each hit to fail the Trial of Death and die (players are not executed: question 5).
- **Pallbearer's Sight**: where something fell within 48 blocks in the last 20 s, a pale grave mark shows for you (`Fallen.near`).

**Abilities (Return wheel, key 7)**

- **Evil Spirits** (8000, 30 s). The recently dead within 30 blocks (up to 6 bodies from the last 400 ticks) rise as Evil Spirits: vex-bodied marionettes for 900 ticks with 40% of what the body had (at most 120 health) and 10 damage. Different from the Abomination's Raise The Dead, which raises the body itself. *Built from*: `Fallen.near` and `take`, `Marionette.spawnSummon`, Brood cap.
- **Descent of Death** (10000, 45 s). Death descends over the spot you aim at (range 24, radius 10, 160 ticks): enemies rot (`Decay` with `0.1 + ticksInside / 400`), cannot heal, and take `Corrosion` damage counted as yours; you heal 25% of every bite. Every Growth inside advances a stage a second, because the life goes to the earth. *Built from*: `Zone`, `Decay`, `NoHeal`, `Corrosion` listener, Growth.
- **Return to Earth** (6000, 30 s). Everything evil or undead within 24 blocks (`Spells.isEvil`) takes 60 damage and is buried for 20 ticks; undead bosses take 10% of their maximum health instead. Others within range are only made Heavy I for 40. *Built from*: `Spells.isEvil`, `Burial`, `Weight`.
- **Maternal Embrace** (9000, 40 s). An earthen coffin closes on the enemy you aim at (range 20): held with everything rooted for 160 ticks (half on a boss, double on undead), sealed (no abilities, no teleports), cannot heal and takes 25% more damage. It is let out with Weakness I for 100. *Built from*: `Root`, `Seal`, `NoHeal`, `Vulnerable`, `Spells.holdLength`.

**Strengthened**: golem cap 3, hybrid cap 3. The wiki's "answers prayers" is left out (question 7).

### Sequence 2: Desolate Matriarch

**Traits**

- **Mother's Domain** (aura): within 16 blocks allies get Regeneration I, and enemies cannot heal and rot slowly (`Decay` 0.05).
- **Gift of the Land**: you never go hungry, and Growths can be planted on any ground.
- **Gender**: the wiki changes the Beyonder to female at this Sequence; see question 8.

**Abilities**

- **Blossoming Land** (Harvest wheel, 10000, 90 s). Land blooms around the spot you aim at: radius 32, 800 ticks. Growths grow four times as fast, a Heartfruit sapling sprouts every 40 ticks (up to 6), allies get Regeneration II and 2 absorption hearts, crops grow. *Built from*: `Zone`, Growth.
- **Barren Land** (Harvest wheel, 10000, 90 s). The reverse, same size and time: enemies cannot heal, are weakened (Weakness I) and starve, `Decay` at 0.1. *Built from*: `Zone`, `NoHeal`, `Decay`.
- **Desolation** (Return wheel, 15000, 60 s). An aura round you, radius 32, 400 ticks: all life drains. Living enemies rot (`Decay` rising to 0.6) and lose 2% of maximum health a second, undead return to the earth at once (as Return to Earth), nothing heals, and every 40 ticks they gain 1 Madness. *Built from*: `Zone.around`, `Decay`, `Corrosion`, `Madness`.
- **Root Eruption** (Return wheel, 12000, 20 s). The ground cracks at the spot you aim at (range 28, radius 14, 40 ticks of warning), then brown roots cover earth and sky: 80 damage, rooted for 60. Each enemy hit rolls the Trial of Death (a mob under 30% health dies) and gains 3 Madness. *Built from*: `SpellFx.warningCircle`, `Zone`, `Root`, `Madness`.
- **Seed of Rebirth** (Alchemy wheel, 12000, 60 s). The Mother's rebirth. On you, or an ally you aim at (range 12), for 2400 ticks: the next time they would die they turn into a seed in the earth instead, hidden and held for 120 ticks, then bloom with 60% health and heal allies within 6 blocks for 20%. A HudTimer shows the ward. *Built from*: `DeathWard`, `Concealment`, `Root`, Growth visual.
- **Brood Form, complete** (the same Alchemy entry). The full Mythical Creature Form, a mass of flesh, wheat, mushrooms, vines and soil: 400 ticks, three times your size, 30 more health, reach 2.5, tendril sweeps that add 20 damage, and an aura that rolls a Mutation each second on every enemy within 10 blocks. Madness builds 1/30 a second and breaks the form at full; 180 s cooldown. *Built from*: `Form`, `GiantForm`, `ConjuredWeapon`, Mutation.

**Strengthened**: Evil Spirits takes the dead within 48 blocks, 10 bodies, at 70% of what they had. Life Deprivation also rolls a Trial of Losing Control: each hit adds 2 Madness. Golem cap 9, hybrid cap 5, Cross-breed may take a mob with up to 400 health and consume two Growths for two traits.

### Sequence 1: Naturewalker

**Traits**

- **Child of Nature**: no fall damage, no fire or lightning damage, and you can breathe underwater. Lightning called by Wrath of Nature and Weather Ritual in the rain is yours to command.

**Abilities**

- **Nature Walk** (Earth wheel, the Underground Travel entry, 6000, 10 s). Become one with the natural element under you and traverse it: earth and ore underground, forest through logs and leaves, water at double speed, up to 60 blocks, hidden throughout. You can also step into one of your Growths and come out of another within 100 blocks, so your Growths are a travel network. *Built from*: `Concealment`, `Teleport`, Terrain, Growth.
- **World of Nature** (Earth wheel, 25000, 300 s). Nurture a realm of your own: a sphere of radius 20 for 800 ticks that enemies cannot leave (stepping out brings them in at the far side). Inside, allies get Regeneration II and Strength I, enemies get Slowness II and 1 Madness every 40 ticks, and a Thornling sprouts under an enemy every 40 ticks (these do not count toward your cap, 8 at most). It falls if you die or leave it by more than 10 blocks. *Built from*: `Dome.sphere`, `Enclosure`, `Zone`, Growth.
- **Verdant Barrier** (Earth wheel, 9000, 60 s). A wall of wheat, flowers, mushrooms and trees across your view, 12 wide and 6 tall, 400 ticks. Enemies cannot cross and their projectiles end at it. Every time it stops something it grows again (`Wall.renew`, 5 times at most). *Built from*: `Wall`.

**Strengthened**: Wrath of Nature names up to 50 adversaries. Weather Ritual's rain becomes a thunderstorm: lightning strikes an enemy in the zone every 2 s for 30 damage. Golem cap 12.

### Sequence 0: Mother

**Traits**

- **Her Presence**: round you plants grow wildly. Every second, 6 spots within 24 blocks grow, your Growths gain a stage every 80 ticks, allies within 12 get Regeneration I, and an enemy within 12 has a Mutation rolled on it every 100 ticks.

**Abilities**

- **Great Harvest** (Harvest wheel, 20000, 60 s). The Harvest authority: within 20 blocks of you, every enemy under 25% health dies (a boss takes 15% of maximum health instead), the rest are Reaped, every Growth is harvested at full yield, and Wheat-mutated enemies leave a Heartfruit. Allies within range heal 10% for every 3 things harvested, up to a full heal. *Built from*: Reap, `Spells.strikeTrue`, Mutation.
- **Awaken** (Alchemy wheel, 20000, 90 s). The Mother activates what is lifeless. On one of your constructs, hybrids or Growths within 16 blocks: a construct gets double health and damage, never runs out (`Construct.extend`) and heals 2% a second; a hybrid gets two traits and stays; a Growth becomes a Mother Tree (a Heartfruit every 40 ticks within 6 blocks, a thorn aura, 6000 ticks, one at a time). *Built from*: `Construct`, Growth.
- **Earthquake** (Earth wheel, 30000, 180 s). The earth shakes: every enemy within 40 blocks of you is unsteady (half their attacks miss, Slowness III, Heavy I, all for 100 ticks), and a rift opens along your look, 30 long and 4 wide: enemies on it take 100 damage and are buried for 60. *Built from*: `Falter`, `Weight`, `Spells.enemiesOnLine`, `Burial`.
- **Embrace of the Earth** (Return wheel, 15000, 60 s). The Death authority. Pierce the enemy in front of you (range 5): 25% of its maximum health (at least 100) that nothing softens, healed to you, it cannot heal for 200, and it corrodes (4 a second, growing 2 more each second, 200 ticks). It refuses if nothing is in reach. *Built from*: `Spells.strikeTrue`, `NoHeal`, `Corrosion`.
- **Bountiful Earth** (Return wheel, 60000, 300 s). The Mother's Divine Kingdom, a land of life and death: a sphere of radius 40 for 800 ticks. Allies inside get Regeneration III and Resistance I, and a free `DeathWard` at 50% health once each; enemies rot (`Decay`), cannot heal, have a Mutation rolled every 60 ticks and a Thornling sprout under them every 40 ticks (up to 12); every enemy that dies inside becomes a free Evil Spirit. Your Brood is 50% stronger in it. It falls if you die or leave it by more than 10 blocks. *Built from*: `Dome.sphere`, `Zone`, `DeathWard`, Mutation, Evil Spirits.

---

## 5. Passives at a glance

| Sequence | Passive | Lives in |
|---|---|---|
| 9 | Physical Enhancement, Farming Proficiency, Weather Forecasting | SequencePassives (once a second); HudTimer |
| 8 | Spirit Vision, Disease Resistance | A tick check (like Weakness Detection); `EffectDuration` rule and an effect event |
| 7 | Green Thumb | A hurt event |
| 6 | Knowledge (Life), Life Sense, Poison Immunity | A tick check; an effect event |
| 5 | Nourished by Earth | SequencePassives |
| 4 | Mutation, Souls for Cross-breeds, Creature Commanding, Strengthened Healing | Hurt events; the abilities themselves |
| 3 | Life Deprivation, Pallbearer's Sight | A hurt event; a tick check |
| 2 | Mother's Domain, Gift of the Land | SequencePassives |
| 1 | Child of Nature | A fall event; SequencePassives |
| 0 | Her Presence | SequencePassives |

---

## 6. New parts for the spell kit (so the code is easy later)

Each is written once in the shared code, not in `NatureWalker`, so other pathways can use it.

| Part | What it does | Used here | Could be used by |
|---|---|---|---|
| `Growth` (+ kinds) | A planted thing with stages, a cap, a speed rule, harvest and wither hooks, a Zone when it blooms | Almost every Life ability | Beauty Goddess (a garden), any trap-like pathway |
| `Mutation` | A 1 to 3 level status with kinds, on top of `Madness` | Alchemist onwards | Abomination, any curse pathway |
| `Brood` | A count and cap for constructs, hybrids and spirits across all three | Golems, birds, hybrids, spirits | Any summoner |
| `Healing` | One call for every heal: respects NoHeal, Decrees and Inversion, shows a number, and has `addScale(...)` rules like `Spells.addCostScale` | Treatment, Heartfruit, auras | White Angel, Hand Of God's Divine Blood |
| `Terrain` | Classifies the ground an entity stands on or looks at | Child of the Oak, Wrath of Nature, Nature Walk, Nourished by Earth | Thunder God, Abomination |
| Trait table | Growth kind by creature class gives traits, data-driven like `WeaknessTable` | Cross-breed | Any "combine two things" ability |
| `verdancy(player)` | One multiplier for life abilities | Everything | (pathway-specific, like the Prince's Might) |
| HudCounter `Growths` | Shows the Growth count against the cap | Section 3.1 | - |

---

## 7. Files and registration

- Classes in `blasphemy_slate/beyonder_characteristics/nature_walker/`: `NatureWalker` (keys 1, 2 and 4, passives, events), `NatureWalkerHarvest`, `NatureWalkerEarth`, `NatureWalkerAlchemy`, `NatureWalkerReturn` (one per wheel), `NatureWalkerGrowths` (the kinds), `NatureWalkerMutations`, `NatureWalkerFx` (every look and sound, previewed with a `/naturefx` command).
- The four new wheels call `.pathway("Nature Walker")` so the catalog owns them, and their classes are initialized at mod setup.
- Key packets `SKeyPressAbilityOneUsed` to `SevenUsed` get one import and one line each.
- `PathwayAbilities.ALL` gets a line per ability. Mark as own-pathway-only (not stealable): Reap, Great Harvest, Awaken, Cross-breed (they read Growth state). Mark `.notCopyable()`: Quicken Life, Bear Form, Brood Form and World of Nature (they control an ongoing effect).
- `SequenceCommands.PATHWAYS`, `SequencePassives`, SEQUENCES.md (add this pathway to the table), and a melee combo registered in `combat/MeleeCombos` (petals and thorn lashes, about 8 steps, as the Prince has 12).
- Beyonder mobs: `ready(...)` checks `Beyonders.pathwayOf`, and every owner (Growth, Brood, Zone) is a `LivingEntity`, so a mob of this pathway can cast the same abilities.
- Two-press abilities (Quicken Life and Underground Travel end early on a second press) use the wheel's `.free(player -> ...)` so the second press costs nothing and has no cooldown.
- Cooldowns are written per ability. If question 9 is answered "none", register the pathway's keys with `Cooldowns.addFree(Cooldowns.keysStartingWith("nature_walker_"))` and give the wheels `.quickCastCooldown(false)`; nothing else changes.

---

## 8. Models and looks needed

All under `models/model_fx/nature_walker/` unless stated. The ability icons for the wheels go under `textures/gui/ability_menus/nature_walker/`, one per entry key.

- **Seeds and Growths**: seed, thornling (3 stages), heartfruit sapling, heartfruit tree, heartfruit, briar orchard, sporebed, mother tree.
- **Vines and earth**: vine burst, root burst, root eruption, mire disc, coffin, rift, grave mark.
- **Worn**: bark skin (wood and stone), bear form, brood form.
- **Made things**: stone golem, stone bird, mud golem, steel golem, evil spirit.
- **Realms**: the World of Nature shell tile, the Bountiful Earth shell tile, verdant barrier lattice.
- **Particles**: petal, spore, pollen, soil crumb, green and brown shock rings.

---

## 9. Open questions

1. **Weather Ritual**: real weather in the whole dimension, or a local zone only (the current plan)? A real change affects everyone on the server.
2. **Pathway name**: `Nature Walker` is used throughout. The wiki calls the pathway Mother and Naturewalker is its Sequence 1 name; say if you want it changed.
3. **Can enemies destroy Growths?** The plan says no (they only run out of time). Letting them trample or hit Growths would add counterplay.
4. **Spirituality refund on harvest**: 100 per Growth, 500 at most per cast. Keep it, or remove it?
5. **Trial of Death on players**: executes only mobs. Should a player under 20% health be able to die to it too?
6. **Wheel convention**: normal opens, sneak recasts (as here and in Hand Of God), or reversed (the Prince)?
7. **"Answers prayers" at Sequence 3** is left out. Do you want anything for it?
8. **Gender change at Sequence 2**: skip it, or do something (skin, model, a flag)?
9. **Cooldowns**: the plan gives each ability a cooldown. Use none at all (as White Angel does) with spirituality as the only limit?
10. **Underground Travel** moves you in hidden hops through blocks. Is that acceptable, or should it stay on open ground only?
11. **Costs**: all are placeholders until SEQUENCES.md is checked.
