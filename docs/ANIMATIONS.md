# Swing animations

Custom weapon swings (horizontal, diagonal, vertical cuts, a stab) that replace vanilla's
up-and-down arm swing, in first person and in third person. They are visual only: damage,
cooldowns, attack speed and hit detection are untouched. All code is client-only and lives in
`src/main/java/net/lukario/frogerealm/client/animation/`.

Today one rule uses it: a **Worm Of Time** player holding a sword (`minecraft:swords` item tag)
cycles through horizontal, diagonal-down and vertical-down cuts.

## How it works

```
 vanilla swing state (swinging / swingTime)          attack key (local player only)
   - local player: set by the click                   InputEvent.InteractionKeyMappingTriggered
   - other players: synced by vanilla's animate packet         |
                 \                                             |
                  v                                            v
            SwingPlayer  (client tick: "did this player start a new main-hand swing?")
                  |
                  |  SwingRules.find(player, main-hand stack) -- first matching SwingRule, or none
                  |        uses the player's aspect: ClientAspectData (you), PlayerAspectCache (others)
                  |
                  |  rule.sequence() -> next swing id (cycles; starts over after resetTicks)
                  |  SwingAnimations.get(id) -> SwingAnimation (keyframes)      missing id = vanilla
                  v
          "player X plays animation A since tick T with item I"
                  |
      every frame | SwingPlayer.poseFor(player, partialTick) -> SwingAnimation.Pose, or null = vanilla
          +-------+---------------------------------+
          v                                         v
   FirstPersonSwing                           ThirdPersonSwing
   RenderHandEvent (main hand):               RenderAvatarEvent.Pre (render state):
   draws the item at vanilla's resting        sword arm gets our ArmPose (Forge ArmPose.create),
   spot + the pose's item offsets, and        vanilla attackTime = 0 for that frame;
   cancels vanilla's drawing of that hand     HumanoidModel.setupAnim calls our transformer,
                                              which sets arm angles and body twist from the pose
```

| File | What it is |
| --- | --- |
| `SwingAnimation` | One swing: id, length in ticks, mirror flag, keyframes. Also `Easing` and `Pose`. Pure data. |
| `SwingAnimations` | The registry and **every definition**. The one file to edit for new swings and tuning. |
| `SwingRule` | Interface: condition on (player, main-hand stack) + sequence of swing ids + reset time. |
| `SwingRules` | The registered rules (first match wins) and helpers (`hasAspect`, `aspectOf`). |
| `SwingPlayer` | Notices swings, cycles sequences, remembers what plays; `poseFor`, `play`, `preview`. |
| `FirstPersonSwing` | First-person drawing hook. |
| `ThirdPersonSwing` | Third-person posing hook. |
| `SwingAnimCommand` | `/swinganim` client command. |
| `PlayerAspectCache` | Other players' aspects on this client. |
| `network/CPlayerAspectPacket` | Server -> client: one player's aspect (sent by `AspectSync`). |

### When vanilla still draws

A frame is vanilla unless **all** of these hold: a swing is playing for that player, the item in
their main hand is the same item as when the swing started, and vanilla would draw that hand with
its plain attack swing (first person: not an empty hand, map, crossbow, an item being used, a spin
attack or scoping; third person: the arm pose is ITEM or EMPTY and the main hand is not being used).
Other players without a matching rule, other items, other classes, the off hand: all vanilla.

Our swing lasts its own length (8 ticks for the presets). If vanilla's swing is still running after
ours ended (6 ticks normally, longer with Mining Fatigue), the rest pose is held so vanilla's tail
does not flash in.

### Detecting a swing (no extra packet)

Every client tick `SwingPlayer` reads each player's vanilla `swinging` / `swingTime`. A swing that
just started (or restarted: `swingTime` went down) on the main hand is a new swing.

- **You**: it only counts if the attack key fired that tick, so dropping an item or opening a door
  keeps the vanilla swing. Swings that hit nothing count too (vanilla swings on a miss).
- **Other players**: vanilla sends their swings (`ClientboundAnimatePacket`); the client cannot tell
  an attack from "used a block", so any main-hand swing counts.

Their aspect is the one thing the client did not know, so `AspectSync` (server) now also sends
`CPlayerAspectPacket(uuid, aspect)` to everyone when a player's aspect changes, and sends a joining
player the aspects of everyone already online. That is the only network change.

## Add a new swing animation

One definition in `SwingAnimations`, one `register(...)` line. No other file changes.

```java
/** Low sweep from left to right. */
public static final SwingAnimation LOW_SWEEP_CUT = SwingAnimation.of("low_sweep", 9)   // id, ticks
        .key(0.00f, Easing.LINEAR, Pose.REST)
        // wind-up: hand low and left, blade drawn back to the left
        .key(0.25f, Easing.OUT, Pose.REST.item(-0.15f, -0.20f, 0.05f).itemRot(-85, 55, -10)
                .arm(-30, -35, 0).body(-15))
        // the cut: across to the right
        .key(0.60f, Easing.OUT_EXPO, Pose.REST.item(0.25f, -0.22f, -0.10f).itemRot(-85, -70, -10)
                .arm(-35, 50, 0).body(20))
        .key(1.00f, Easing.IN_OUT, Pose.REST);
```

and in the `static { ... }` block: `register(LOW_SWEEP_CUT);`

Rules:
- Start and end at `Pose.REST` (vanilla's own resting pose), so the swing blends in and out.
- Keyframe times are fractions of the swing (0..1), so `ticks` can change without touching them.
- The easing on a keyframe shapes the movement that ends there. The house style is
  `OUT` (wind-up) -> `OUT_EXPO` (cut) -> `IN_OUT` (follow-through).
- `Pose` fields: `item(x, y, z)`, `itemRot(pitch, yaw, roll)`, `scale(s)` are first person;
  `arm(pitch, yaw, roll)` and `body(yaw)` are third person. What each one does and which sign goes
  where is in the Javadoc of `SwingAnimation.Pose`.
- A mirrored copy is one line: `HORIZONTAL_CUT.mirrored(HORIZONTAL_REVERSE)` flips every yaw, roll
  and sideways offset. Left-handed players get the mirror automatically on top of that.
- Ids are lower case words with underscores (they are typed in `/swinganim`).

## Add a rule for another weapon or class

One constant in `SwingRules`, one `register(...)` line in its `static` block:

```java
/** Hand Of Order with an axe: two heavy overhead chops, then a horizontal cut. */
public static final SwingRule HAND_OF_ORDER_AXE = SwingRule.of(
        (player, held) -> hasAspect(player, "Hand Of Order") && held.is(ItemTags.AXES),
        DEFAULT_RESET_TICKS,                                   // 1.5 s pause -> back to the first
        SwingAnimations.VERTICAL_DOWN, SwingAnimations.VERTICAL_DOWN, SwingAnimations.HORIZONTAL);
```

- The condition gets the player and their **main-hand** stack. Use any test: an item tag
  (`ItemTags.SWORDS`, `ItemTags.AXES`, `ItemTags.SPEARS`...), an item (`held.is(Items.MACE)`),
  `held.isEmpty()` for fists, the aspect, a status, anything the client can see.
- Fists: `held.isEmpty()` works for third person; first person keeps vanilla's punch (only items are
  redrawn, the bare arm is not).
- Rules are checked in order; the first match wins. Put specific rules before general ones.
- Any class works for remote players too, because every aspect is synced.
- A condition that throws counts as "no match"; it never crashes the game.

## Link a rule to a MeleeCombos step

The selection layer does not depend on `MeleeCombos` (the five classes with combos keep theirs as
they are; Worm Of Time has no combo in this port, which is why its rule triggers on the plain swing).
Two ways to line the animations up with a combo's steps:

1. **Mirror the combo on the client (no network).** Give the rule as many ids as the combo has steps,
   in the same order, and the same reset time: a `MeleeCombo` resets after 40 ticks by default, so
   pass `40` as the rule's reset ticks. It drifts when a swing does not count as a combo hit on the
   server (a miss, or an attack below the combo's `minCharge`), because the client counts every
   attack swing.
2. **Exact (one small packet per step).** In the combo step (server), send a packet such as
   `CPlaySwingPacket(entityId, swingId)` to the players tracking the attacker; in its client handler
   (behind `FMLEnvironment.dist.isClient()`, like the other C-packets) call
   `SwingPlayer.play(player, swingId, 1)`. Do not register a `SwingRule` for that aspect/weapon, or
   register one with the first step only. Cost: the animation starts one round trip after the click.

Either way the rule or packet only chooses the picture; the combo's effects stay on the server.

## Tune timings

Everything is in `SwingAnimations`: the length (`SwingAnimation.of(id, ticks)`), the keyframe times,
the easings and the pose numbers, each with a comment. The sequence and its reset time are in
`SwingRules`. 20 ticks = 1 second; the sword's attack cooldown is about 12.5 ticks and vanilla's swing
is 6, so 6 to 10 ticks feels right. Rebuild and run `./gradlew runClient`; there is no hot reload.

## Debug with /swinganim

A client command, so it needs no op rights and only you see the result:

- `/swinganim list` lists the ids.
- `/swinganim horizontal` plays it on yourself once, with whatever is in your main hand, whatever
  your aspect (F5 for third person).
- `/swinganim horizontal 5` plays it five times slower (1 to 20) so you can see each keyframe.

A real swing (left click) replaces a preview that is still playing.

## Worked example: a stab for spears, for every class

Goal: any player holding a spear (`minecraft:spears`) stabs twice, then cuts horizontally.

1. The swing exists already (`SwingAnimations.STAB`). To make a longer, deeper one instead, add to
   `SwingAnimations`:

   ```java
   public static final String DEEP_STAB = "deep_stab";

   /** A slower, deeper thrust for long weapons. */
   public static final SwingAnimation DEEP_STAB_THRUST = SwingAnimation.of(DEEP_STAB, 10)
           .key(0.00f, Easing.LINEAR, Pose.REST)
           // pull back further: hand close to the camera, blade levelled forward
           .key(0.35f, Easing.OUT, Pose.REST.item(0.05f, -0.05f, 0.25f).itemRot(-80, 10, 0)
                   .arm(-55, 20, 0).body(15))
           // thrust: hand far forward
           .key(0.60f, Easing.OUT_EXPO, Pose.REST.item(-0.05f, 0.02f, -0.60f).itemRot(-88, 5, 0)
                   .arm(-90, -5, 0).body(-12))
           .key(1.00f, Easing.IN_OUT, Pose.REST);
   ```

   and `register(DEEP_STAB_THRUST);` in the `static` block.
2. Add the rule to `SwingRules` (after `WORM_OF_TIME_SWORD`):

   ```java
   /** Anyone with a spear: stab, stab, cut. */
   public static final SwingRule ANY_SPEAR = SwingRule.of(
           (player, held) -> held.is(ItemTags.SPEARS),
           DEFAULT_RESET_TICKS,
           SwingAnimations.DEEP_STAB, SwingAnimations.DEEP_STAB, SwingAnimations.HORIZONTAL);
   ```

   and `register(ANY_SPEAR);` in its `static` block.
3. `./gradlew build`, then `./gradlew runClient`: `/swinganim deep_stab 5` to look at it, then hold a
   spear and click: stab, stab, cut, and after a 1.5 s pause it starts again at the first stab.

Note: vanilla spears in 26.2 have their own stab animation (`SwingAnimationType.STAB`) and a
SPEAR arm pose. First person replaces it; in third person our pose is only applied over the ITEM
or EMPTY arm pose, so a spear would keep vanilla's third-person spear pose. Allowing it there is a
one-line change in `ThirdPersonSwing` (the `current != ITEM && current != EMPTY` check).

## Known limitations

- **Third person relies on Forge extending `HumanoidModel.ArmPose` at runtime** (`ArmPose.create`,
  done by Forge's `RuntimeEnumExtender`). If that ever fails, the log says "could not add an arm
  pose" and third person stays vanilla; first person is not affected.
- Other players: any main-hand swing they make (also using a block or dropping an item) plays the
  next swing; the client cannot tell why they swung. For yourself only attack-key swings count.
- Each client counts a player's sequence on its own, so two viewers can show a different cut for the
  same swing (one of them missed a swing because the player was out of view, or the swing came from
  using a block).
- First-person item offsets (`item`, `itemRot`, `scale`) only apply to first person; third person
  moves the arm and body and the held item follows the arm.
- The off hand is never animated. Clicking again during the first half of a running vanilla swing
  does not restart it (vanilla behaviour), so no new animation starts either.
- While a swing plays, a modded sword with its own first-person transform
  (`IClientItemExtensions.applyForgeHandTransform`) is drawn with ours instead.
- The local player's model in the inventory screen shows the swing too.
- Swing length does not follow Haste or Mining Fatigue.
- The slash particles of the other classes do not follow these arcs (Worm Of Time has no slash on a
  plain swing). Follow-up.
