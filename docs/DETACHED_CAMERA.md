# Detached cameras for abilities

From an ability's server-side cast handler, open a camera with one call.

```java
import net.lukario.circleofinevitability.camera.DetachedCamera;

// player is a ServerPlayer. Charge the ability only if this succeeds.
boolean opened = DetachedCamera.start(player, "spirit_scouting");
```

In free flight the player can use the movement keys and mouse to look back at their own body. In scripted mode the ability sets the camera’s position and aim. In both modes the body keeps its facing, game mode, inventory and vulnerability. Gravity and external forces still act on it. The server calculates the viewpoint and checks collision and range. The player can explore terrain already tracked around their body in the current dimension.

## Try it in game

An operator can run these commands in chat. Tab completes preset IDs.

```mcfunction
/coicamera start circle_of_inevitability:spirit_scouting
/coicamera start circle_of_inevitability:clairvoyance at ~ ~3 ~6
/coicamera start circle_of_inevitability:orbit follow @e[type=minecraft:cow,sort=nearest,limit=1]
/coicamera stop
```

Scouting lasts 30 seconds and reaches 48 blocks. Fly with the movement keys, ascend with Jump, descend with Sneak, and boost with Sprint. Try flying through stone and then obsidian. The scouting preset treats obsidian and crying obsidian as wards. Clairvoyance gives eight seconds of mouse look from the chosen point. Moving the mouse in Orbit circles the selected entity. **Return to body** defaults to Backspace and can be rebound in Controls.

## Wire a charge-and-release camera to an ability

1. **Choose a chord.** Key of Stars uses the camera modifier and the charge key (default **Z+V**) from the normal player view. Either key can go down first. Completing the chord starts a charge. Releasing either key casts it once. **Z+C** independently opens free flight. All three bindings are in Controls.
2. **Measure the hold on the server.** Send begin, release and cancel events from the client. On begin, validate the pathway and store the current server player tick. On release, consume the stored tick before checking the result. A duplicate release then has no effect. [Input handling](../src/main/java/net/lukario/circleofinevitability/client/CameraChordInput.java) · [Packet handling](../src/main/java/net/lukario/circleofinevitability/network/SStarCameraPacket.java)
3. **Convert hold time to distance.** Key of Stars computes whole blocks with `heldTicks / 10`. A tap under half a second leaves the player in their body. A 1.5-second hold gives one three-block teleport on release. The client cancels the charge when gameplay loses focus or a menu opens. [Key of Stars cast](../src/main/java/net/lukario/circleofinevitability/blasphemy_slate/beyonder_characteristics/key_of_stars/KeyOfStars.java)
4. **Choose the landing rules.** Copy the [projection preset](../src/main/resources/data/circle_of_inevitability/camera_presets/key_of_stars_projection.json). Its fixed motion allows mouse look at the landing point. Its range caps the cast at 48 blocks and its collision tag makes obsidian and crying obsidian stop the camera. Zero duration lets the player stay until Backspace or another exit condition.
5. **Open the view on release.** Call `DetachedCamera.atForward(player, "your_namespace:your_preset", blocks)`. It starts from the player's eyes and uses their aim at release. It clips the path against the preset and tracked terrain. Charge costs or start cooldowns only if it returns true.
6. **Check both abilities separately.** Hold Z+V for one second. The view stays with the player while charging, then jumps two blocks on release. Press Backspace and test Z+C with the movement keys. Run `./gradlew runGameTestServer -PcameraTests` for the server regressions.

## Let the ability direct the camera

Open a scripted view from the ability's server-side cast and keep its handle for the rest of the cast. Here the camera rises six blocks over one second and turns straight down, leaving the caster visible below.

```java
DetachedCamera.startScripted(player).ifPresent(camera -> {
    Vec3 overhead = player.getEyePosition().add(0, 6, 0);
    camera.moveTo(overhead, player.getYRot(), 90, 20);
});
```

`moveTo(position, yaw, pitch, ticks)` moves in a straight line and turns along the shortest yaw arc. Coordinates are absolute, angles are Minecraft degrees, and durations are game ticks. To animate a curve, follow a moving target, or rotate in place, call `camera.setPose(position, yaw, pitch)` from the ability's server tick. Each call replaces the previous destination. The view holds its last pose until the next instruction or the end of the session. When the cast ends, call `camera.stop()`.

The player can use the configured exit key during a scripted view. Their movement and mouse input leave both the body and the shot alone. To use another preset's block collision, range and exit rules, pass its ID to `startScripted(player, "spirit_scouting")`. A move returns false for a stale handle, a player-controlled view, invalid values, or an unavailable destination. Accepted moves still obey collision and range each tick.

Preview the same ability API in-game with these operator commands. The second command moves the camera six blocks above your feet and turns it downward over one second.

```mcfunction
/coicamera start circle_of_inevitability:scripted
/coicamera move ~ ~6 ~ 0 90 20
/coicamera stop
```

## Anchors, actions and cleanup

```java
// Open at a specific eye position in the player's dimension.
DetachedCamera.at(player, "clairvoyance", doorway);

// Follow an entity. The preset decides whether the view tracks, orbits or flies.
DetachedCamera.follow(player, "orbit", target);

// Run ability-specific code on a use-key press.
DetachedCamera.start(player, "spirit_scouting", (caster, view) -> {
    Vec3 from = view.position();
    Vec3 to = from.add(view.direction().scale(6));
    var hit = caster.level().clip(new ClipContext(from, to,
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
    if (hit.getType() == HitResult.Type.BLOCK) {
        caster.sendOverlayMessage(Component.literal("Marked " + hit.getBlockPos().toShortString()));
    }
});
```

The action runs on the server with a validated origin and look direction. The example uses Minecraft's Vec3, ClipContext, HitResult and Component. Ordinary attacking, mining and item use are suppressed while observing. Each ability chooses its own target rules, reach, cost and cooldown.

The boolean entry points (`start`, `at`, and `follow`) return false if a preset is missing, the player already has a view, the anchor is unavailable, or the initial point intersects a blocked shape. Under the same conditions, `startScripted` returns an empty Optional. Riding, sleeping and possession also prevent opening. A bare preset name uses the mod namespace. A fully qualified ID can refer to any datapack.

Call `DetachedCamera.stop(player)` for an immediate ability decision. For delayed callbacks, capture the handle when the view opens and call `handle.stop()` when the ability expires. A handle only affects the session that created it. Its `view()` returns an empty Optional once that session ends.

```java
if (DetachedCamera.start(player, "spirit_scouting")) {
    var handle = DetachedCamera.handle(player).orElseThrow();
    // Keep handle in the ability's state. Later, call handle.stop().
}
```

Death, logout, a dimension change, loss of the anchor, or another system taking over the camera ends the session. Damage interrupts immediately when enabled. Manual exit, timeout and ability-requested stops honor the return animation. If a solid barrier blocks the route home, the view returns immediately. The client restores the previous perspective and HUD on exit.

## Datapack presets

Place a JSON file at `data/<namespace>/camera_presets/<name>.json` and run `/reload`. New sessions use the reloaded values. An active session keeps the settings it opened with. The bundled examples live in [camera_presets](../src/main/resources/data/circle_of_inevitability/camera_presets).

| Field | Default | Meaning |
| --- | --- | --- |
| `motion.mode` | `free` | `free` flies with the keys. `fixed` stays at the anchor. `orbit` circles the anchor using mouse look. `scripted` takes position and rotation from the ability. |
| `motion.speed` | 0.35 | Free flight speed in blocks per tick, from 0.01 to 4. |
| `motion.sprint_multiplier` | 2 | Free flight boost from 1 to 4. |
| `motion.acceleration`, `motion.braking` | 0.5, 0.7 | Fraction of the remaining change in speed applied each tick, from 0.01 to 1. A value of 1 responds immediately. |
| `motion.forward_step` | omitted | In free flight, jump `blocks` (0.01–64) in the current view direction every `interval_ticks` (1–72000) while the step chord is held. |
| `motion.orbit_radius` | 4 | Desired orbit radius in blocks, from 0.5 to 32. Walls and the range limit can shorten it. |
| `collision.mode` | `solid` | `solid` stops at block collision shapes, `phase` passes through them all, `allowlist` passes only tagged shapes, and `denylist` stops only at tagged shapes. |
| `collision.tag` | omitted | Required for either list mode. Supply a block tag ID such as `circle_of_inevitability:camera_wards`, without a leading `#`. |
| `range` | 48 | Maximum distance in blocks from the anchor, up to 512. Zero removes the distance tether for any mode, including player flight. The world border, build height and tracked chunks still limit travel. |
| `return_at_boundary` | false | End the view when flight exceeds its bounds. Otherwise the camera stops at the limit. |
| `duration` | 200 | Session length in ticks, from 0 to 72000. Zero runs until another exit condition. |
| `manual_exit`, `cancel_on_damage` | true, true | Allow the return key and interrupt on damage to the body, respectively. |
| `display.fov` | 0 | Use the player's FOV or set an angle from 30 to 150 degrees. |
| `display.hide_hud` | false | Hide the HUD for this view. |
| `display.return_ticks` | 0 | Return instantly or glide back over 1 to 60 ticks. |

For a camera that can pass through leaves but stops at other solid blocks, use the following preset.

```json
{
  "motion": { "mode": "free", "speed": 0.22 },
  "collision": { "mode": "allowlist", "tag": "minecraft:leaves" },
  "range": 24,
  "duration": 120
}
```

Run the focused server regressions with `./gradlew runGameTestServer -PcameraTests`.
