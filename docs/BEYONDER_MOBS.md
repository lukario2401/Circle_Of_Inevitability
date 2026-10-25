# Beyonder mobs

Some mobs carry a Beyonder Characteristic of one Pathway and Sequence. When one dies, it drops that characteristic (the `beyonder_characteristic` item, see `item/BeyonderCharacteristicItem`). Abilities that steal or copy powers only find something in a Beyonder mob, and only from its Pathway, up to its Sequence.

Code: `blasphemy_slate/beyonder_mobs/` (`BeyonderMobRules`, `BeyonderMobs`, `BeyonderMobCommands`, and for casting `MobCasting`, `MobAbilities`, `MobAbility`, `MobSpirituality`, `MobSpells`), `blasphemy_slate/beyonder/Beyonders`, and one `...Mobs` class per Pathway next to its player abilities (`hand_of_order/HandOfOrderMobs`, `key_of_stars/KeyOfStarsMobs`, `star_dragon/StarDragonMobs`).

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
| Key Of Stars: Record (K) | the first thing they cast while you watch | looking at it when you press Record: one of its abilities, rolled for at once; or, while you watch, the first thing it really casts (Quick Study glints at its casts too) | nothing |

Only abilities marked `STEALABLE` in `PathwayAbilities` can be taken, as before.

## Mobs casting

In a fight, a Beyonder mob casts its Pathway's abilities. It can cast one only if:

- it is of that ability's **Pathway**,
- its **Sequence** is the ability's or stronger (the same Sequence a player needs),
- it has the **spirituality** the ability costs (the player's cost),
- the ability's cooldown for this mob is over, and it did not just cast something,
- it is not bound by Order (a root on abilities, which a hold on everything is too),
- its target is in the ability's range (and in sight, if it needs that), and the ability is worth casting now.

Its spirituality is half a player's of the same Sequence, and comes back at half a player's speed:

| Sequence | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 to 0 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| a mob's bar | 10 | 200 | 990 | 3200 | 7750 | 16200 | 29890 | 39200, full again at once |

So a Sequence 9 or 8 mob cannot pay for any of the abilities below yet (a player of that Sequence has twice as much). A new Beyonder mob starts full.

"A fight": it has a target (or something hurt it in the last 5 seconds) within 32 blocks. Once a second it may cast (35%), picking by weight among what it can cast right now. After any cast it waits 3 seconds.

The abilities mobs have so far:

| Pathway | Ability | Sequence | Cost | Mob cooldown | When a mob casts it |
| --- | --- | --- | --- | --- | --- |
| Hand Of Order | Halt | 9 | 250 | 8 s | its target is within 16 blocks, in sight and not held already: held 2 s (a player 1 s; from Judge 6: 3 s / 1.5 s) |
| Hand Of Order | Presence | 9 | 400 | 14 s | its target is within the wave (8 blocks, Judge 12): enemies slowed; non-players weakened, and cowed if weak; Intimidation from 4 |
| Key Of Stars | Door Opening | 9 | 250 | 5 s | its target is 6 to 16 blocks away, or behind a wall: it steps out of a Door beside it |
| Key Of Stars | Slip Free | 9 | 400 | 20 s | it is rooted, burning or under a harmful effect: all of it ends (Teleport roots too from 4) |
| Star Dragon | Piercing Ray | 6 | 2450 | 8 s | its target is within 28 blocks and in sight: 40 damage to each enemy on the line, and Dimmed |

What a mob's ability deals to a player is halved (`DAMAGE_TO_PLAYERS`): the player numbers were made against mobs. A mob's cast is remembered like a player's: the crystal ball shows it as its last power, Replication can copy it, and a watching Record writes it down.

`/beyondermob info <mob>` shows its spirituality and each ability it knows, with "ready" or why not.

### Tuning casting

At the top of `BeyonderMobRules.java`, "Casting":

| Setting | Default | What it does |
| --- | --- | --- |
| `MOBS_CAST` | true | false: no mob casts |
| `SPIRITUALITY_SHARE` | 0.5 | its bar, as a share of a player's of the same Sequence |
| `REGEN_SHARE` | 0.5 | how fast it comes back, as a share of a player's |
| `THINK_EVERY` | 20 | ticks between its chances to cast |
| `CAST_CHANCE` | 0.35 | the chance it casts at each of those, when it can |
| `CAST_PAUSE` | 60 | ticks after any cast before the next |
| `COOLDOWN_SCALE` | 1.0 | every mob cooldown times this |
| `FIGHT_RANGE` | 32 | how far its target can be |
| `DAMAGE_TO_PLAYERS` | 0.5 | what its abilities deal to players, times this |

An ability's own cooldown and weight are in its Pathway's `...Mobs` class.

### Giving mobs another ability (one that is already made for players)

1. Open the Pathway's `...Mobs` class (for a Pathway without one, copy `HandOfOrderMobs` into its package, and add `NewPathwayMobs.register();` to the static block of `MobAbilities`).
2. Make the constants the ability needs package-private (remove `private`), so the mob version uses the same numbers.
3. Register it in `register()`:

```java
MobAbilities.register(MobAbility.of(PATHWAY, 4, MobAbility.NORMAL)   // key 4 of PathwayAbilities, not sneaking
        .cost(VERDICT_COST)                 // the player's cost
        .cooldown(200)                      // ticks, a mob's
        .range(16).sight()                  // or .anyRange() for a cast on itself
        .when((mob, target, sl) -> ...)     // optional: only when it is worth it
        .weight(10)                         // optional: how often it is picked (default 10)
        .cast(MyPathwayMobs::verdict));
```

   Its name and Sequence come from `PathwayAbilities` (key 4). A sneak form gets `.named("...")`, and `.sequence(n)` if it needs a stronger Sequence than the key.
4. Write the cast: `private static boolean verdict(Mob mob, LivingEntity target, ServerLevel sl)`. Copy the player version's body and swap the player parts:

| Player version | Mob version |
| --- | --- |
| `Spells.aimEnemy(player, sl, range)` | `target` (already checked for range and sight) |
| `Spells.enemiesAround(player, ...)`, `enemiesOnLine` | `MobSpells.enemiesAround(mob, ...)`, `MobSpells.enemiesOnLine` |
| `Spells.strike(player, target, dmg)` | `MobSpells.strike(mob, target, dmg)` |
| `player.getLookAngle()` | `MobSpells.face(mob, target)`, then `MobSpells.toward(mob, target)` |
| `Beyonder.hasSequence(player, n)` | `MobSpells.hasSequence(mob, n)` |
| `Spells.casterStillHere(player, sl)` | `MobSpells.casterStillHere(mob, sl)` |
| `Cooldowns`, `pay(...)`, messages to the player | nothing: `MobCasting` does the paying and the cooldown |

   Return true when it went off, false when it refused (nothing is spent then).
5. The ability's looks (`...Fx`) usually take a `Player`. If the method only uses where the caster is and where it faces, change its parameter to `LivingEntity` (the player calls still compile). That is how `HandOfOrderFx.halt`, `KeyOfStarsFx.doorOpening` and `StarDragonFx.piercingRay` were done.

Some abilities are not worth porting as they are: ones that open a wheel, need the player's screen or items, or are a toggle the same key ends. Give mobs a simple version of those instead.

For code:

```java
MobCasting.whyNot(mob, ability)            // NONE, or why it cannot cast it now
MobCasting.tryCast(mob, ability, target, sl)  // cast now, with every check
MobCasting.addBlocker((mob, ability) -> ...)  // one more thing that keeps mobs from casting
MobAbilities.knownBy(mob)                  // what its Pathway and Sequence give it
MobSpirituality.get(mob) / spend(mob, n) / add(mob, n)
Casts.mobCast(mob, sl, cast)               // remember a mob's cast and tell the AnyCasterListeners (MobCasting does this)
Casts.addAnyCasterListener((caster, sl, cast) -> ...)   // hear players' and mobs' casts alike
```

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

BeyonderMobs.make(mob, "Hand Of Order", 7) // whatever its Rule says (and a full spirituality bar)
BeyonderMobs.summon(sl, EntityTypes.ZOMBIE, pos, "Star Dragon", 3)  // a new mob that already is one (rituals, structures, boss fights)
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
| `/beyondermob summon <entity> "<Pathway>" <sequence> [pos] [count]` | Summons new mobs that already are Beyonders of that Pathway and Sequence (any living mob, Sequence 9 to 0, up to 64 at once) |
| `/beyondermob set <targets> "<Pathway>" <sequence>` | Makes them Beyonders of that Pathway and Sequence |
| `/beyondermob clear <targets>` | Plain mobs again |
| `/beyondermob reroll <targets>` | Clears them and rolls again by their Rule |
| `/beyondermob info <target>` | Its Pathway, Sequence, what can be taken from it, its spirituality, and each ability it can cast (ready, or why not) |

Examples:

```
/beyondermob summon zombie "Hand Of Order" 7                 a Sequence 7 Hand Of Order zombie where you stand
/beyondermob summon skeleton "Key Of Stars" 5 ~ ~ ~5 3       three Sequence 5 Key Of Stars skeletons, 5 blocks away
/beyondermob summon wither "Star Dragon" 0                   a Sequence 0 Star Dragon Wither
/beyondermob summon villager "Hand Of Order" 9               any living mob works, not only hostile ones
```

A summoned one spawns the way `/summon` spawns it (a skeleton has its bow), with the health, damage, name and aura of its Sequence, and drops its characteristic when it dies like any other. A mob from another mod is named by its full id (`mymod:ghoul`). The Sequence is not limited by the mob's Rule: a zombie can be Sequence 0.

## Not done yet

- Mobs cast only the five abilities above. The rest of each Pathway's abilities need their mob versions (see "Giving mobs another ability").
- Mobs from spawners can be Beyonders too, so a spawner farm yields characteristics. If that is too generous, the roll can skip `EntitySpawnReason.SPAWNER` mobs.
