# Damage fx: red numbers above what you hit (/damagefx)

With damage fx on, the damage you deal is written in red above your target's head. Every player decides for themself; it needs no cheats and no op.

```
/damagefx          switches it
/damagefx on
/damagefx off
```

It is off until a player turns it on, and it is saved with the player: it stays on through death, a relog and a server restart.

---

## What you see

- The number appears above the head, stays there without moving for one second, and is then gone. It follows the head if the target walks.
- Several hits at once go on lines above each other, the first one lowest, up to six. A seventh takes the place of the oldest.
- It is what the target really lost: after armor, resistance and everything else in the mod (Vulnerable, DamageLink...), with yellow absorption hearts counted. Whole numbers as they are (`12`), others with one decimal (`7.5`).
- Damage that was cancelled or came to 0 shows nothing.

## Who sees it

A number is shown when the player who **dealt** the damage has damage fx on. It goes to:

- that player (up to 128 blocks from the target),
- every other player within 48 blocks of the target who has damage fx on too.

A player with damage fx off never sees numbers and never causes any. The one who is hurt does not get the number above their own head.

## What counts as your damage

| Damage | Counts |
|---|---|
| Melee, arrows and other projectiles, `Spells.strike`, explosions you set off, damage passed on by `DamageLink` | always (the game already knows it is yours) |
| What your marionettes deal | always |
| Burning, poison, wither and other magic that names no attacker | for 15 seconds after you last hurt that target |
| An ability that hurts with a source naming nobody | when the ability says so with `DamageFx.credit` (below) |

So a mob you set on fire keeps showing numbers while it burns. A mob you hit that then walks into a cactus does not.

---

## Making an ability's damage count

Most abilities need nothing: `Spells.strike(player, target, damage)` and `damageSources().playerAttack(player)` name the player.

Some hurt with a source that names nobody, such as `sl.damageSources().magic()`, an explosion with a `null` owner, or a lightning bolt. For those, say whose damage it is right before:

```java
DamageFx.credit(player, target);
target.hurt(sl.damageSources().magic(), damage);
```

`credit` changes nothing about the damage itself, who gets the kill, or who the mob gets angry at. It is only remembered for the numbers: for the next 2 seconds anything that hurts `target` without naming an attacker counts as `player`'s, and burning, poison and wither do for 15 seconds.

It is used in four places now: Key Of Stars' orbiting stars and their collapse, Hand Of Order's "The sentence was God" and "Judgement falls".

Not covered: the damage a sealed entity takes back from Key Of Stars' seals (the code there does not know the player).

---

## Changing how it looks

All at the top of `particles/fx/DamageNumberRenderer.java`:

| Constant | Now | What it is |
|---|---|---|
| `COLOR` | `0xFFFF2B2B` | the colour, as 0xAARRGGBB |
| `HOLD_TICKS` | `20` | how long a number stays (20 = one second) |
| `FADE_TICKS` | `3` | how long it takes to go away after that; `0` = gone at once |
| `PIXEL` | `0.04f` | size: blocks per pixel of a digit (a digit is 7 pixels tall) |
| `ABOVE_HEAD` | `0.35f` | how far above the head the lowest number is |
| `ABOVE_NAME` | `0.85f` | the same for players, so it clears their name |
| `GROW_FROM`, `MOST_GROWN` | `10f`, `4f` | further away than 10 blocks a number is drawn bigger so it stays readable, up to 4 times |

And at the top of `combat/DamageFx.java`: `VIEW_RANGE` (48), `OWN_RANGE` (128), `CREDIT_TICKS` (300), `ABILITY_CREDIT_TICKS` (40).

The digits are `textures/particle/fx/damage_digits.png`: `0123456789.` in white, left to right, each 5 x 7 pixels inside a 7 x 9 box (one clear pixel all round). Paint over them to change the font; keep the boxes where they are. The colour comes from `COLOR`, so keep the picture white.

---

## How it works

- `combat/DamageFx` (server): the command, the setting, and a listener that runs after every other damage listener, works out whose damage it is and sends the number to the players who should see it.
- `particles/fx/DamageNumber`: the number on its way to a client. It travels as a particle (`circle_of_inevitability:damage_number`), like `ParticleFx`, `SlashFx` and `ModelFx`, so there is no new packet.
- `particles/fx/DamageNumberParticle` and `DamageNumberRenderer` (client): keep it above the head and draw the digits.

## Checking it in the game

`./gradlew runClient`, a world with cheats:

1. Hit a mob: no number. `/damagefx`: chat says "Damage fx on: the damage you deal shows above your target."
2. Hit a pig with your fist: a red `1` above its head for a second (a zombie shows `0.9`: its armor takes a little). With a sword the number is bigger; a critical hit bigger again.
3. Shoot it with a bow from far away: the number shows, and is still readable.
4. Hit it with a Fire Aspect sword (or `/enchant`): after the hit, a `1` every second while it burns.
5. Use an ability that hits several times (any melee combo, Key Of Stars' orbit): numbers stack above each other and do not overlap.
6. Walk around the mob and look at it from above: the number always faces you, upright, not mirrored.
7. Die, respawn, relog: still on. `/damagefx off`: chat says "Damage fx off.", no more numbers.
8. With a second player: both on, each sees the other's numbers when near; one off, that one sees none and their hits show to nobody.
