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

## Wire a held forward step to another ability

1. **Choose the controls.** Key of Stars uses Camera combo modifier + Detach camera (default **Z+C**) to detach. The player can immediately fly and aim with the normal controls. Camera combo modifier + Step camera forward (default **Z+V**) jumps forward once per completed half-second. Releasing either key discards the unfinished interval. Backspace returns to the body. The three camera bindings are separate entries under Controls. Either key can go down first. If a normal mod ability shares a camera key, a solo tap casts on release. Completing the chord consumes both presses.
2. **Create a preset.** Copy the [Key of Stars preset](../src/main/resources/data/circle_of_inevitability/camera_presets/key_of_stars_scouting.json) to `data/<namespace>/camera_presets/<ability>.json`. Set `motion.mode` to `free` and add `"forward_step": { "blocks": 1, "interval_ticks": 10 }` inside `motion`. At 20 ticks per second, an interval of 10 gives steps at ticks 10, 20, 30 and so on. For two blocks every second, use 2 and 20. Omitting `forward_step` keeps ordinary free flight.
3. **Choose where the camera can go.** Set `collision.mode`, `range` and `duration` in that preset. Both flight and jumps respect these limits. Key of Stars uses a 48-block tether, lasts 600 ticks, and passes through ordinary blocks until a tagged ward stops it. The camera starts at eye level. Each jump starts at the current camera position and follows its current yaw and pitch, including straight up or down.
4. **Open the preset from the ability’s existing server cast.** Check the caster’s pathway, sequence and cast restrictions, then call `DetachedCamera.start(player, "your_namespace:your_preset")`. It returns `true` when the view opens. Use that result before spending resources or starting a cooldown. Key of Stars uses the pathway’s existing cast gate and is available from Apprentice (9). See [KeyOfStars.openScoutingCamera](../src/main/java/net/lukario/circleofinevitability/blasphemy_slate/beyonder_characteristics/key_of_stars/KeyOfStars.java).
5. **Connect a custom activation chord only if the ability needs one.** An existing ability slot or wheel can call the cast directly. For a custom chord, read remappable keys on the client while the game has focus and menus are closed. Send one activation request when the chord first goes down. The server checks the sender and opens that ability’s preset. Key of Stars is wired in [CameraChordInput](../src/main/java/net/lukario/circleofinevitability/client/CameraChordInput.java) and [SOpenStarCameraPacket](../src/main/java/net/lukario/circleofinevitability/network/SOpenStarCameraPacket.java).
6. **Reuse the held input and cleanup.** Any free-flight preset with `forward_step` uses the existing Z+V control. The client sends the held state with the current camera session ID. The server counts ticks and sends the resulting position to the viewer. Each session owns its timer. For an early return from a delayed callback, capture `DetachedCamera.handle(player)` when the cast opens and later call `handle.stop()`.
7. **Test the cast and its limits.** Reload the datapack and open the camera through the actual ability. Aim back at your body, then hold the step chord for one second in open space. Expect two blocks of travel while the body stays put. Release just before the next step, re-hold, and check that it takes a full interval. Test a blocked wall, an allowed wall, Backspace and a second cast. To check input conflicts, bind a normal ability to Z or C and aim at a target. Hold Z first, add C, then repeat with C first. Both orders should detach, keep the target unharmed, and leave the normal ability available on a solo tap. Run `./gradlew runGameTestServer -PcameraTests` for the timing and collision regressions.

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
