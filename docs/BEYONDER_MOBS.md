# Beyonder mobs

Some mobs carry a Beyonder Characteristic of one Pathway and Sequence. When one dies, it drops that characteristic (the `beyonder_characteristic` item, see `item/BeyonderCharacteristicItem`). Abilities that steal or copy powers only find something in a Beyonder mob, and only from its Pathway, up to its Sequence.

Code: `blasphemy_slate/beyonder_mobs/` (`BeyonderMobRules`, `BeyonderMobs`, `BeyonderMobCommands`) and `blasphemy_slate/beyonder/Beyonders`.

## Who is one

| Mobs | Chance to be a Beyonder | Sequences | Pathways |
| --- | --- | --- | --- |
| Every hostile mob (anything that is an `Enemy`) | 5% | 9 to 5 | Key Of Stars, Hand Of Order, Star Dragon |
| Wither, Warden, Ender Dragon | 50% | 4 to 0 | the same |

Within each row, a stronger Sequence is rarer:

| Hostile mobs | 9 | 8 | 7 | 6 | 5 |
| --- | --- | --- | --- | --- | --- |
| weight (out of 100) | 55 | 25 | 12 | 6 | 2 |
| extra health | +25% | +50% | +100% | +150% | +200% |
| extra damage | +15% | +30% | +50% | +75% | +100% |

| Bosses | 4 | 3 | 2 | 1 | 0 |
| --- | --- | --- | --- | --- | --- |
| weight (out of 100) | 55 | 25 | 12 | 6 | 2 |
| extra health | +25% | +50% | +75% | +100% | +150% |
| extra damage | +15% | +30% | +50% | +75% | +100% |

So about 1 hostile mob in 2000 is a Sequence 5 (5% x 2%), and about 1 boss in 100 is a Sequence 0 (50% x 2%).

Mobs called up by an ability (`Marionette.summon`: The Fallen, risen bodies, summoned zombies...) are never Beyonders and drop nothing (`SUMMONS_NEVER`).

## How it looks

- **Name**: "Zombie (Hand Of Order, Sequence 7)" above its head, in its Pathway's color. A mob that already had a name (a name tag) keeps it. `SHOW_NAME`, `NAME_ALWAYS_VISIBLE`.
- **Aura**: a few motes of its Pathway's color around its body every half second. None while it is invisible. `AURA_EVERY_TICKS`, `AURA_MOTES`.
- **Stronger**: the extra health and damage in the tables above, as permanent attribute modifiers (saved with the mob). It spawns at full health.
- The abilities that read someone (Hand Of Order's Recognition and Beyonder Sense, Knowledge Emperor's Mystery Prying, Key Of Stars' crystal ball, Knight Of Misfortune's dream secrets, Hand Of God's Demon Hunting) show its Pathway and Sequence as they do a player's. Hand Of Order's Rules count a Beyonder mob of a stronger Sequence as stronger.

## Its characteristic

When it dies, the characteristic of its Pathway and Sequence drops where it fell. If it fell over the void (no ground within 64 blocks below, like the Ender Dragon over the End), it drops at the killer's feet instead. One body drops one characteristic. A death undone by a death ward drops nothing.

## What can be taken from it

| Ability | From a player with a Pathway | From a Beyonder mob | From any other mob |
| --- | --- | --- | --- |
| Worm Of Time: Theft (H) | one of their abilities, up to their Sequence | one of its Pathway's, up to its Sequence (then empty for 30 s, as before) | nothing |
| Dark Angel: Grazing (L) | the same, when you kill them | the same, when you kill it | nothing |
| Key Of Stars: Replication (Starlight wheel) | what they just cast | aimed at it: one of its Pathway's, up to its Sequence, cast at once | nothing |
| Key Of Stars: Record (K) | the first thing they cast while you watch | looking at it when you press Record: one of its abilities, rolled for at once | nothing |

Only abilities marked `STEALABLE` in `PathwayAbilities` can be taken, as before. Mobs do not cast their Pathway's abilities (yet).

## Changing the numbers

Everything is at the top of `BeyonderMobRules.java`:

```java
public static final Rule HOSTILE = Rule.chance(0.05)                           // 5% of hostile mobs
        .pathways(MOB_PATHWAYS)
        //        Sequence  weight  extra health  extra damage   (0.5 = +50%)
        .sequence(9,        55,     0.25,         0.15)
        .sequence(8,        25,     0.50,         0.30)
        ...
```

- **How many are Beyonders**: the number in `Rule.chance(...)` (0.05 = 5%).
- **How rare a Sequence is**: its weight. Weights are compared with each other in the same Rule, so 55 / 25 / 12 / 6 / 2 means 55%, 25%, 12%, 6%, 2%. Make a weight smaller to make it rarer, 0 to switch it off.
- **Which Sequences**: add or remove `.sequence(...)` lines.
- **Which Pathways**: `MOB_PATHWAYS`. Add a Pathway's name and mobs start being it.
- **Bosses**: `BOSS` the same way.

## A custom mob later

- A hostile one (its class implements `Enemy`) is already a `HOSTILE` Beyonder at 5%; nothing to do.
- A boss, or a mob with odds of its own, gets one line in the static block of `BeyonderMobRules`:

```java
register(ModEntities.MY_BOSS.get(), BOSS);
register(ModEntities.MY_GHOUL.get(), Rule.chance(0.20).pathways(HandOfOrder.PATHWAY)
        .sequence(8, 70, 0.5, 0.3)
        .sequence(7, 30, 1.0, 0.5));
register(EntityTypes.SILVERFISH, Rule.NEVER);          // a hostile mob that is never one
```

`register` can also be called from common setup or from the mob's own registration, any time before it spawns.

## For code

```java
Beyonders.isBeyonder(target)               // a player with a Pathway, or a Beyonder mob
Beyonders.pathwayOf(target)                // "none" when not
Beyonders.sequenceOf(target)               // 10 (Beyonders.NO_SEQUENCE) when not
Beyonders.abilitiesOf(target)              // what its Sequence gives it
Beyonders.takeableFrom(target)             // the stealable ones of those
Beyonders.randomTakeable(target, random, null)
Beyonders.describe(target)                 // "Hand Of Order, Sequence 7"

BeyonderMobs.make(mob, "Hand Of Order", 7) // whatever its Rule says
BeyonderMobs.clear(mob)
BeyonderMobs.roll(mob)                     // by its Rule
BeyonderMobs.castOf(mob, sl, random)       // a cast of one of its abilities, for Replication-like copies
```

Use `Beyonders` (not `SoulCore`) in any new ability that reads, steals or judges someone else's powers, so it works on mobs too.

Each mob is rolled for once, on the first tick it is seen (by then a summon is already marked as one). What it is, and that it was rolled, is saved in the mob's persistent data (`circle_of_inevitability_beyonder_mob`, `circle_of_inevitability_beyonder_rolled`). Mobs that were in the world before this existed are rolled the first time they are loaded.

## Commands

Op level 2:

| Command | What it does |
| --- | --- |
| `/beyondermob set <targets> "<Pathway>" <sequence>` | Makes them Beyonders of that Pathway and Sequence |
| `/beyondermob clear <targets>` | Plain mobs again |
| `/beyondermob reroll <targets>` | Clears them and rolls again by their Rule |
| `/beyondermob info <target>` | Its Pathway, Sequence, and what can be taken from it |

To summon one for a test: `/summon zombie`, then `/beyondermob set @e[type=zombie,sort=nearest,limit=1] "Hand Of Order" 7`.

## Not done yet

- Beyonder mobs do not cast their Pathway's abilities. `BeyonderMobs` is the place to give them that (a tick that picks one of `Beyonders.abilitiesOf(mob)`), but the abilities are written for players, so each one would need a mob version.
- Mobs from spawners can be Beyonders too, so a spawner farm yields characteristics. If that is too generous, the roll can skip `EntitySpawnReason.SPAWNER` mobs.
