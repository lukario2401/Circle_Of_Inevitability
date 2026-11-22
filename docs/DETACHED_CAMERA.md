# Detached cameras for abilities

From an ability's server-side cast handler, open a camera with one call.

```java
import net.lukario.circleofinevitability.camera.DetachedCamera;

// player is a ServerPlayer. Charge the ability only if this succeeds.
boolean opened = DetachedCamera.start(player, "spirit_scouting");
```

The player can look back at their own body. Their movement keys and mouse control the view, while their body keeps its facing, game mode, inventory and vulnerability. Gravity and external forces still act on it. The server calculates the viewpoint and checks collision and range. The player can explore terrain already tracked around their body in the current dimension.

## Try it in game

An operator can run these commands in chat. Tab completes preset IDs.

```mcfunction
/coicamera start circle_of_inevitability:spirit_scouting
/coicamera start circle_of_inevitability:clairvoyance at ~ ~3 ~6
/coicamera start circle_of_inevitability:orbit follow @e[type=minecraft:cow,sort=nearest,limit=1]
/coicamera stop
```

Scouting lasts 30 seconds and reaches 48 blocks. Fly with the movement keys, ascend with Jump, descend with Sneak, and boost with Sprint. Try flying through stone and then obsidian. The scouting preset treats obsidian and crying obsidian as wards. Clairvoyance gives eight seconds of mouse look from the chosen point. Moving the mouse in Orbit circles the selected entity. **Return to body** defaults to Backspace and can be rebound in Controls.

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

All three start methods return false if a preset is missing, the player already has a view, the anchor is unavailable, or the initial point intersects a blocked shape. Riding, sleeping and possession also prevent opening. A bare preset name uses the mod namespace. A fully qualified ID can refer to any datapack.

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
| `motion.mode` | `free` | `free` flies with the keys. `fixed` stays at the anchor. `orbit` circles the anchor using mouse look. |
| `motion.speed` | 0.35 | Free flight speed in blocks per tick, from 0.01 to 4. |
| `motion.sprint_multiplier` | 2 | Free flight boost from 1 to 4. |
| `motion.acceleration`, `motion.braking` | 0.5, 0.7 | Fraction of the remaining change in speed applied each tick, from 0.01 to 1. A value of 1 responds immediately. |
| `motion.orbit_radius` | 4 | Desired orbit radius in blocks, from 0.5 to 32. Walls and the range limit can shorten it. |
| `collision.mode` | `solid` | `solid` stops at block collision shapes, `phase` passes through them all, `allowlist` passes only tagged shapes, and `denylist` stops only at tagged shapes. |
| `collision.tag` | omitted | Required for either list mode. Supply a block tag ID such as `circle_of_inevitability:camera_wards`, without a leading `#`. |
| `range` | 48 | Maximum distance in blocks from the anchor, from 0.5 to 512. The world border, build height and tracked chunks also limit travel. |
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
