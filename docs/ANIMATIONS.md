# Swing animations

Custom weapon swings (horizontal, diagonal and vertical cuts, a stab, an uppercut, a wide spin slash)
that replace vanilla's up-and-down arm swing, in first person and in third person. They are visual
only: damage, cooldowns, attack speed and hit detection are untouched. All code is client-only and
lives in `src/main/java/net/lukario/circleofinevitability/client/animation/`.

Today one rule uses it: a **Worm Of Time** player holding a sword (`minecraft:swords` item tag)
cycles through all eight swings (see [The eight swings](#the-eight-swings)).

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
                  |        uses the player's Pathway: ClientPathwayData (you), PlayerPathwayCache (others)
                  |
                  |  rule.sequence() -> next swing id (cycles; starts over after resetTicks)
                  |  SwingAnimations.get(id) -> SwingAnimation (keyframes)      missing id = vanilla
                  |  a swing still on screen? remember its pose: the new one blends out of it
                  v
          "player X plays animation A since tick T with item I, blending from pose P"
                  |
      every frame | SwingPlayer.poseFor(player, partialTick) -> SwingAnimation.Pose, or null = vanilla
          +-------+---------------------------------+
          v                                         v
   FirstPersonSwing                           ThirdPersonSwing
   RenderHandEvent (main hand):               RenderAvatarEvent.Pre (render state):
   draws the item at vanilla's resting        sword arm gets our ArmPose (Forge ArmPose.create),
   spot + the global first-person offset      vanilla attackTime = 0 for that frame;
   and scale + the pose's item offsets,       HumanoidModel.setupAnim calls our transformer,
   and cancels vanilla's drawing of that hand which sets arm angles and body twist from the pose
```

| File | What it is |
| --- | --- |
| `SwingAnimation` | One swing: id, length in ticks, mirror flag, keyframes. Also `Easing` and `Pose`. Pure data. |
| `SwingAnimations` | The registry, the **global tuning constants** and **every definition**. The one file to edit for new swings and tuning. |
| `SwingRule` | Interface: condition on (player, main-hand stack) + sequence of swing ids + reset time. |
| `SwingRules` | The registered rules (first match wins) and helpers (`hasPathway`, `pathwayOf`). |
| `SwingPlayer` | Notices swings, cycles sequences, remembers what plays and what it blends from; `poseFor`, `play`, `preview`. |
| `FirstPersonSwing` | First-person drawing hook (`applyItemTransform` is the whole first-person transform). |
| `ThirdPersonSwing` | Third-person posing hook. |
| `SwingAnimCommand` | `/swinganim` client command. |
| `PlayerPathwayCache` | Other players' Pathways on this client. |
| `network/CPlayerPathwayPacket` | Server -> client: one player's Pathway (sent by `PathwaySync`). |

### When vanilla still draws

A frame is vanilla unless **all** of these hold: a swing is playing for that player, the item in
their main hand is the same item as when the swing started, and vanilla would draw that hand with
its plain attack swing (first person: not an empty hand, map, crossbow, an item being used, a spin
attack or scoping; third person: the arm pose is ITEM or EMPTY and the main hand is not being used).
Other players without a matching rule, other items, other classes, the off hand: all vanilla.

Our swing lasts its own length (12 to 16 ticks for the presets, vanilla's is 6). If vanilla's swing is
still running after ours ended (only with strong Mining Fatigue), the rest pose is held so vanilla's
tail does not flash in.

### Detecting a swing (no extra packet)

Every client tick `SwingPlayer` reads each player's vanilla `swinging` / `swingTime`. A swing that
just started (or restarted: `swingTime` went down) on the main hand is a new swing.

- **You**: it only counts if the attack key fired that tick, so dropping an item or opening a door
  keeps the vanilla swing. Swings that hit nothing count too (vanilla swings on a miss).
- **Other players**: vanilla sends their swings (`ClientboundAnimatePacket`); the client cannot tell
  an attack from "used a block", so any main-hand swing counts.

Their Pathway is the one thing the client did not know, so `PathwaySync` (server) now also sends
`CPlayerPathwayPacket(uuid, Pathway)` to everyone when a player's Pathway changes, and sends a joining
player the Pathways of everyone already online. That is the only network change.

## The eight swings

All lengths are at `SPEED = 1`. "Screen" is first person for a right-handed player (left-handed
players see everything mirrored); "model" is third person.

| id | Ticks | Screen: travel of the cut, start -> end | Blade at the impact | Model (arm) |
| --- | --- | --- | --- | --- |
| `horizontal` | 14 | right -> left, level at chest height | level, pointing left | arm swings from out to across, chest twists |
| `stab` | 12 | pulled back low right (near the hip) -> thrust forward to the middle, small recoil | pointing at the crosshair, flat facing up | arm drawn back behind the hip, then forward and low |
| `diagonal_down` | 14 | upper right -> lower left | pointing down-left | arm high, then low and across |
| `uppercut` | 14 | lower left (the side away from the sword) -> upper right, steep | pointing up, finishing high | arm low and across, then raised and out |
| `horizontal_reverse` | 14 | left -> right (backhand, you see the other side of the blade) | level, pointing right | arm across, then swings out |
| `vertical_down` | 14 | top middle -> centre (overhead chop) | pointing straight ahead | arm overhead, then low in front |
| `diagonal_up` | 14 | lower right -> upper left | pointing up-left | arm low and out, then high and across |
| `spin_slash` | 16 | low right -> over the top of the middle -> low left, nearly the full width, with the wrist rolling (roll -22 -> +21), then a follow-through | pointing down-left, turning back past the left side | arm far out, then far across with the body twisted |

The Worm Of Time rule plays them in that order (the table's order), so each swing travels a
different way from the one before and no swing follows itself, also when the cycle wraps around. After
1.5 seconds (30 ticks) without a swing it starts again at `horizontal`.

## Global tuning (one place)

At the top of `SwingAnimations`, in the block marked **GLOBAL TUNING**. They apply to every swing;
no definition needs to change when you nudge them.

| Constant | What it does | Now | Typical |
| --- | --- | --- | --- |
| `FIRST_PERSON_OFFSET_X` | Where the sword sits during a swing, sideways, added to vanilla's resting spot. `+` = right (mirrored for left-handed players), so negative moves it toward the centre. | `-0.18` | `-0.10 .. -0.25` |
| `FIRST_PERSON_OFFSET_Y` | Same, up (`+`) and down. | `0.24` | `0.10 .. 0.25` |
| `FIRST_PERSON_OFFSET_Z` | Same, depth: negative = further from the camera (smaller on screen, more room to move). | `-0.25` | `-0.10 .. -0.30` |
| `FIRST_PERSON_SCALE` | Size of the sword during a swing (1 = vanilla). | `1.2` | `1.1 .. 1.4` |
| `SPEED` | Playback speed of every swing, first and third person: 1 = the lengths in the definitions, `0.8` = 25% slower, `1.25` = 25% faster. Visual only. | `1.0` | `0.75 .. 1.25` |

Offset and scale are faded in at the start of the swing and out at the end (by `Pose.weight`, see
below), so a swing still leaves from and returns to exactly where vanilla holds the item. If you raise
the scale or move the sword closer (`Z` toward 0), check the widest swings (`spin_slash`,
`horizontal_reverse`, `uppercut`) with `/swinganim <id> 5`: their tips come closest to the edges.

Why the offset matters: after every attack vanilla lowers the held item by up to 0.6 blocks until the
attack cooldown has refilled (`ItemInHandRenderer.tick`, the "dip" you see under a sword). A swing
runs entirely inside that dip, so the first-person hook ignores it while a swing is on screen and only
lets it back in as the swing returns to rest.

## Phases

Every swing has the same four phases. Keyframe times are fractions of the swing (0 = start,
1 = end), so changing the length keeps the rhythm; the easing on a keyframe shapes the movement that
**ends** there.

| Phase | Keyframe (fraction, easing) | 14-tick swing | What it is for |
| --- | --- | --- | --- |
| wind-up | `0.28f, Easing.IN_OUT` | ~4 ticks | Anticipation: the blade is drawn back, starting and ending slowly, so you see it coming. |
| cut | `0.46f, Easing.OUT_EXPO` | ~2.5 ticks | Very fast: most of the travel happens in the first tick. The fastest part of every swing. |
| hold | `0.64f, Easing.OUT` | ~2.5 ticks | A short stop at the impact pose, with a few degrees of drift so it does not look frozen. The sword is fully on screen here. |
| recovery | `1.00f, Easing.IN_OUT, Pose.REST` | ~5 ticks | Smooth return to vanilla's resting pose. |

Variations: the stab has an extra recoil keyframe between the cut and the hold, the spin slash has an
extra keyframe at the top of its arc (`Easing.IN` into it, `Easing.OUT` out of it, so the arc speeds up
then slows down without a stop) and a follow-through keyframe before its hold.

The sword's attack cooldown is 12.5 ticks, so if you click again as soon as the sword is charged,
the next swing starts during the recovery of the last one; that is what blending is for.

## Blending (clicking before a swing has finished)

When a new swing starts while one is still on screen (a click during the cut, hold or recovery, or a
`/swinganim` while a swing plays), `SwingPlayer` remembers the pose the old swing has at that moment
and the new swing's **wind-up starts from that pose** instead of from rest
(`SwingAnimation.sample(progress, from)`). Rules:

- Only the wind-up is blended. From the wind-up keyframe on, the new swing is exactly its own path,
  then it ends and is removed like any other: nothing can stay stuck in a pose.
- The blended wind-up has the wind-up's own length and easing, so it is never faster than a normal
  wind-up (checked: 143 vs 124 degrees per tick at most, over all 8 x 8 interruptions).
- Vanilla only restarts its swing (and so ours) once its own swing is half done (3 ticks); clicks
  before that do nothing, as in vanilla.
- Attack speed, cooldown and damage never change: the blend only changes the picture.
- A missing swing id still means "play vanilla", without a blend.

`Pose.weight` (set for you, never typed) is what makes the start and end seamless: `REST` has weight 0,
every pose built from it with `item`, `itemRot`, `scale`, `arm` or `body` has weight 1, and between
keyframes it rises fast as a swing leaves rest and falls late as it returns, whatever the keyframe's
easing. First person multiplies the global offset and scale by it, and vanilla's dip by `1 - weight`.

## Add a new swing animation

One definition in `SwingAnimations`, one `register(...)` line. No other file changes.

```java
public static final String LOW_SWEEP = "low_sweep";

/** Low sweep from left to right. */
public static final SwingAnimation LOW_SWEEP_CUT = SwingAnimation.of(LOW_SWEEP, 14)   // id, ticks
        .key(0.00f, Easing.LINEAR, Pose.REST)
        // wind-up: hand low on the left, blade level and pointing left
        .key(0.28f, Easing.IN_OUT, Pose.REST.item(-0.35f, -0.06f, 0.04f).itemRot(-110, 75, 10)
                .arm(-30, -35, 0).body(-15))
        // the cut: across to the right, blade ends pointing right
        .key(0.46f, Easing.OUT_EXPO, Pose.REST.item(0.04f, -0.08f, -0.06f).itemRot(-110, -70, -5)
                .arm(-35, 50, 0).body(20))
        // hold
        .key(0.64f, Easing.OUT, Pose.REST.item(0.06f, -0.09f, -0.07f).itemRot(-110, -75, -6)
                .arm(-35, 55, 0).body(22))
        .key(1.00f, Easing.IN_OUT, Pose.REST);
```

and in the `static { ... }` block: `register(LOW_SWEEP_CUT);`

Rules:
- Start and end at `Pose.REST` (vanilla's own resting pose), so the swing blends in and out.
- Keep the four phases (wind-up, cut, hold, recovery) and their easings; see [Phases](#phases).
- `Pose` fields: `item(x, y, z)`, `itemRot(pitch, yaw, roll)`, `scale(s)` are first person;
  `arm(pitch, yaw, roll)` and `body(yaw)` are third person. What each one does and which sign goes
  where is in the Javadoc of `SwingAnimation.Pose`.
- First person, the recipe all eight presets use: `itemRot(-110, yaw, roll)` lays the blade level and
  pointing straight ahead; yaw then swings it (`+80` = pointing left, `-70` = pointing right; between
  +-70 and +-90 its flat faces you, so it reads as a sword and is lit), and roll tilts that sweep on
  screen (`+` = left end lower). Put the travel in `item(x, y, z)`: the hand's path is what you read.
  Avoid ending a key pose with the blade pointing into the screen or along your line of sight
  (yaw near 0 with roll near 0 or 90): you would see its dark edge.
- A mirrored copy is one line, `SOME_CUT.mirrored("some_cut_reverse")`: it flips every yaw, roll and
  sideways offset. It mirrors around the hand, which is right of the centre, so for wide cuts in first
  person write the reverse as its own definition (as `horizontal_reverse` does). Left-handed players
  get the mirror automatically on top of that.
- Ids are lower case words with underscores (they are typed in `/swinganim`).

## Add a rule for another weapon or class

One constant in `SwingRules`, one `register(...)` line in its `static` block:

```java
/** Hand Of Order with an axe: two heavy overhead chops, then a horizontal cut. */
public static final SwingRule HAND_OF_ORDER_AXE = SwingRule.of(
        (player, held) -> hasPathway(player, "Hand Of Order") && held.is(ItemTags.AXES),
        DEFAULT_RESET_TICKS,                                   // 1.5 s pause -> back to the first
        SwingAnimations.VERTICAL_DOWN, SwingAnimations.HORIZONTAL, SwingAnimations.VERTICAL_DOWN,
        SwingAnimations.HORIZONTAL_REVERSE);
```

- The condition gets the player and their **main-hand** stack. Use any test: an item tag
  (`ItemTags.SWORDS`, `ItemTags.AXES`, `ItemTags.SPEARS`...), an item (`held.is(Items.MACE)`),
  `held.isEmpty()` for fists, the Pathway, a status, anything the client can see.
- Order the sequence so the same swing never comes twice in a row (also from the last id back to the
  first) and the direction changes from one swing to the next.
- Fists: `held.isEmpty()` works for third person; first person keeps vanilla's punch (only items are
  redrawn, the bare arm is not).
- Rules are checked in order; the first match wins. Put specific rules before general ones.
- Any class works for remote players too, because every Pathway is synced.
- A condition that throws counts as "no match"; it never crashes the game.

## Link a rule to a MeleeCombos step

The selection layer does not depend on `MeleeCombos`. Worm Of Time and Prince Of Abolition
have both a client sword-swing rule and a separate server hit combo.
Two ways to line the animations up with a combo's steps:

1. **Mirror the combo on the client (no network).** Give the rule as many ids as the combo has steps,
   in the same order, and the same reset time: a `MeleeCombo` resets after 40 ticks by default, so
   pass `40` as the rule's reset ticks. It drifts when a swing does not count as a combo hit on the
   server (a miss, or an attack below the combo's `minCharge`), because the client counts every
   attack swing.
2. **Exact (one small packet per step).** In the combo step (server), send a packet such as
   `CPlaySwingPacket(entityId, swingId)` to the players tracking the attacker; in its client handler
   (behind `FMLEnvironment.dist.isClient()`, like the other C-packets) call
   `SwingPlayer.play(player, swingId, 1)`. Do not register a `SwingRule` for that Pathway/weapon, or
   register one with the first step only. Cost: the animation starts one round trip after the click.

Either way the rule or packet only chooses the picture; the combo's effects stay on the server.

## Tune timings

Everything is in `SwingAnimations`: the global `SPEED`, each swing's length
(`SwingAnimation.of(id, ticks)`), the keyframe times, the easings and the pose numbers, each with a
comment. The sequence and its reset time are in `SwingRules`. 20 ticks = 1 second; the sword's attack
cooldown is 12.5 ticks and vanilla's swing is 6. The presets are 12 to 16 ticks: slower than the
cooldown on purpose (weighty), the next click blends out of the recovery. Below about 10 ticks the
hold and the wind-up get too short to read. Rebuild and run `./gradlew runClient`; there is no hot
reload.

## How to tune a move that looks wrong

1. Watch it slowly: `/swinganim <id> 5` (five times slower), in first person and with F5 (third
   person, front view).
2. **The whole sword is too low / high / small / big in every swing**: change the global
   `FIRST_PERSON_OFFSET_*` or `FIRST_PERSON_SCALE`, not the definitions.
3. **All swings too fast or too slow**: `SPEED`. One swing only: its `ticks`.
4. **The tip leaves the screen at one moment**: find the keyframe just before that moment. Move the
   hand toward the centre with `item(x, y, ...)`, or turn the blade so it points less toward that edge
   (yaw closer to 0 shortens it on screen; less roll keeps it level).
5. **The sword looks dark or thin**: at that pose you see it edge-on or its underside. Keep the
   impact and wind-up poses at yaw between +-70 and +-95 with `itemRot(-110, ...)`; avoid a key pose
   with the blade pointing straight into the screen or along your line of sight. A blade raised and
   leaning back toward you shows its underside (darker): lean it slightly forward instead.
6. **Two swings look alike**: make them differ in the direction the hand travels during the cut
   (`item` of the wind-up key vs the cut key), in where they start and end, and in the roll at the
   impact.
7. **No visible stop at the impact**: the hold keyframe (after the cut) should change the pose only a
   little; give it 0.15 to 0.20 of the swing (2 to 3 ticks). Too stiff: add a few degrees of drift.
8. **It snaps at the start or the end**: the first and last keyframes must be `Pose.REST`.
9. **It jerks when you click fast**: that is the blend into the next wind-up; a wind-up shorter than
   about 3 ticks makes it abrupt. Lengthen the wind-up (its keyframe time) or the swing.
10. **Third person**: the held blade sticks out of the fist at a right angle, so arm pitch -30..-45
    points it forward and -90 points it up; `body` twists the chest. Check the swing from the front
    with F5 twice.
11. Left-handed: everything is mirrored automatically; check one swing with Options > Skin
    Customization > Main Hand: Left.

## Debug with /swinganim

A client command, so it needs no op rights and only you see the result:

- `/swinganim list` lists every id with its length in ticks at the current `SPEED`
  (`Swings (8): horizontal 14t, horizontal_reverse 14t, ...`).
- `/swinganim horizontal` plays it on yourself once, with whatever is in your main hand, whatever
  your Pathway (F5 for third person). Tab completes every id, including `stab`, `uppercut` and
  `spin_slash`.
- `/swinganim horizontal 5` plays it five times slower (1 to 20) so you can see each keyframe.

A real swing (left click) replaces a preview that is still playing, blending out of it.

## Worked example: a stab for spears, for every class

Goal: any player holding a spear (`minecraft:spears`) stabs twice, then cuts horizontally.

1. The swing exists already (`SwingAnimations.STAB`). To make a longer, deeper one instead, add to
   `SwingAnimations`:

   ```java
   public static final String DEEP_STAB = "deep_stab";

   /** A slower, deeper thrust for long weapons. */
   public static final SwingAnimation DEEP_STAB_THRUST = SwingAnimation.of(DEEP_STAB, 16)
           .key(0.00f, Easing.LINEAR, Pose.REST)
           // pull back further: hand low and close, blade aimed at the middle of the screen
           .key(0.32f, Easing.IN_OUT, Pose.REST.item(0.10f, -0.12f, 0.20f).itemRot(-130, 29, -90)
                   .arm(25, 15, 0).body(20))
           // thrust: hand far forward
           .key(0.46f, Easing.OUT_EXPO, Pose.REST.item(-0.14f, 0.10f, -0.42f).itemRot(-130, 29, -90)
                   .arm(-30, -5, 0).body(-15))
           // hold
           .key(0.62f, Easing.OUT, Pose.REST.item(-0.13f, 0.09f, -0.36f).itemRot(-130, 29, -90)
                   .arm(-25, -5, 0).body(-12))
           .key(1.00f, Easing.IN_OUT, Pose.REST);
   ```

   and `register(DEEP_STAB_THRUST);` in the `static` block.
2. Add the rule to `SwingRules` (after `WORM_OF_TIME_SWORD`):

   ```java
   /** Anyone with a spear: stab, cut, stab, backhand. */
   public static final SwingRule ANY_SPEAR = SwingRule.of(
           (player, held) -> held.is(ItemTags.SPEARS),
           DEFAULT_RESET_TICKS,
           SwingAnimations.DEEP_STAB, SwingAnimations.HORIZONTAL, SwingAnimations.DEEP_STAB,
           SwingAnimations.HORIZONTAL_REVERSE);
   ```

   and `register(ANY_SPEAR);` in its `static` block.
3. `./gradlew build`, then `./gradlew runClient`: `/swinganim deep_stab 5` to look at it, then hold a
   spear and click: stab, cut, stab, backhand, and after a 1.5 s pause it starts again at the first.

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
- First-person item offsets (`item`, `itemRot`, `scale`) and the global first-person offset and scale
  only apply to first person; third person moves the arm and body and the held item follows the arm.
- The model's arm cannot bend at the wrist, so in third person the stab's blade points forward-up
  rather than straight ahead during the thrust.
- The off hand is never animated. Clicking again during the first half of a running vanilla swing
  does not restart it (vanilla behaviour), so no new animation starts either.
- While a swing plays, a modded sword with its own first-person transform
  (`IClientItemExtensions.applyForgeHandTransform`) is drawn with ours instead.
- The local player's model in the inventory screen shows the swing too.
- Swing length does not follow Haste or Mining Fatigue (only `SPEED`).
- First-person brightness follows the world's lighting like vanilla's (light level and the two fixed
  light directions of the level), so at night or in caves the sword is as dark as a vanilla one.
- Server hit slashes and client sword animations advance independently. Misses, weak attacks and
  different viewer histories can make their sequences differ; trails are not attached to the blade.

## Prince Of Abolition variants

`PrinceSwordSwings.java` contains twelve dedicated animations registered by `SwingAnimations`.
See [the implementation guide](CUSTOM_SWING_IMPLEMENTATION.md#prince-choreography-and-previews)
for their order and preview IDs. These use a softer `OUT` cut easing, smaller hand paths and
restrained body yaw; the shared presets above remain the Worm Of Time animations.
