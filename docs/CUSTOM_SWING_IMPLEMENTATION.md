# Custom swing animation implementation

This describes the existing Java animation system shared by Worm Of Time and Prince Of Abolition. For pose tuning and examples, see [ANIMATIONS.md](ANIMATIONS.md).

For live in-game edits without rebuilding, see [SWING_TUNING.md](SWING_TUNING.md). `/swinganim edit`
rebuilds immutable keyframe data and refreshes the local preview; `loop` repeats it and `pose` holds a
sampled pose. `SwingTuning` overlays client-local JSON definitions on the built-in registry and supplies
runtime global values to first-person rendering and playback. `save` persists edits; `load` validates
the entire file before replacing state; `export` writes a Java expression for the same animation package.
Resetting an override reveals its compiled default. Preview state is cleared on a real local attack,
item-type change or leaving the world. Ordinary pathway rules and server combat are unchanged.

## Selection and playback

The implementation lives in `src/main/java/net/lukario/circleofinevitability/client/animation/`.

- `SwingAnimations` registers named keyframe animations. The eight shared presets remain available for Worm Of Time; `PrinceSwordSwings` supplies twelve dedicated Prince animations and registers them through `registerAll`. Each definition contains its duration and normalized keyframe times.
- `SwingRule` pairs a player/item predicate with an ordered sequence and reset timeout. `SwingRules.find` uses the first matching rule. Both pathways require the main-hand item to belong to `minecraft:swords`; modded swords work when tagged correctly.
- `SwingRules.pathwayOf` reads `ClientPathwayData` for the local player and `PlayerPathwayCache` for remote players. Pathway synchronization supplies those values.
- `SwingPlayer` watches vanilla `swinging` and `swingTime` each client tick. A start or restart selects the next animation. Local playback also requires an attack-key event, so local block interactions do not advance the chain. Remote playback uses vanilla swing synchronization; no custom swing packet is required.

Worm Of Time cycles eight moves and resets after more than 30 ticks without a swing. Prince Of Abolition cycles twelve moves and resets after more than 40 ticks. Each viewer maintains its own counters. A rule change restarts the sequence; switching the held item invalidates playback. Leaving the world clears playback and cached player data.

## Pose sampling and blending

`SwingAnimation.Pose` stores first-person item translation, rotation and scale, plus third-person arm rotation and body yaw. Definitions start and end at `Pose.REST`. Keyframe easing controls interpolation into each pose: wind-up, fast cut, impact hold, then recovery. Times are fractions of the animation duration.

`SwingPlayer.poseFor` samples using elapsed client ticks plus render partial ticks. If a new animation interrupts the previous one, playback captures the current pose. `SwingAnimation.sample(progress, from)` blends from it during the new wind-up, then follows the new animation normally. Pose weight fades the custom transform in and out. Left-handed rendering mirrors the appropriate offsets and angles. These poses do not change attack cooldown, hit timing or damage.

## First-person rendering

`FirstPersonSwing` handles Forge `RenderHandEvent`. During an eligible main-hand swing it draws the held item using vanilla's resting hand placement plus the sampled transform, then suppresses that hand's vanilla draw. Global offset and scale in `SwingAnimations` fade with pose weight. Vanilla's post-attack downward dip fades out while the custom pose is active and returns during recovery. Item use and unsupported cases keep normal rendering; a rendering failure logs an error and falls back to vanilla.

## Third-person rendering

`ThirdPersonSwing` handles `RenderAvatarEvent.Pre`, samples the pose into a per-frame map and assigns a custom Forge `HumanoidModel.ArmPose`. It clears vanilla attack time for the main attack arm to prevent a second swing being applied. The custom arm transformer applies arm/body angles; the held item follows the arm. Vanilla rebuilds the render state and resets model parts each frame. Special arm poses and active main-hand use retain normal rendering. If Forge cannot create the custom arm pose, third person falls back to vanilla.

## Server hit effects

`MeleeCombos` dispatches uncancelled living-target attack events to the pathway's registered `MeleeCombo`. `MeleeCombo` checks attack charge and stores the next hit, last qualifying hit time and pathway in player persistent data. Prince's twelve hit callbacks call `PrinceOfAbolitionFx.combo` with indices 0 through 11. `SlashFx` descriptions are immutable copies sent through `ParticleShapes`; their delay, sweep and lifetime animate particle ribbons on clients.

Client swings count attack swings, including misses. Server effects count qualifying target attacks. Consequently matching sequence lengths and reset windows do not guarantee alignment. Particle trails approximate the move direction; they do not track the rendered sword tip. Exact alignment would require explicit server-selected animation synchronization.

## Verification in game

1. Set the pathway to `Prince Of Abolition`, equip a sword, and attack through all twelve moves in first person and F5. Pause over two seconds and check the restart. Compare Worm Of Time's eight-move chain.
2. Test the left main hand, a tagged modded sword, a non-sword, item use, rapid interruptions, misses and partially charged attacks.
3. Use `/swinganim list` and `/swinganim spin_slash 5` for slow local pose previews. Use `/abolitionfx combo` for the full particle chain; repeat to inspect all three finisher variants.
4. Check another player observing the attacks, bright daylight visibility and cleanup after changing worlds. Viewer sequences may differ because their swing histories differ.

Build with `gradlew.bat build`. In-game visual verification is separate from compilation.

## Prince choreography and previews

Prince now uses twelve unique definitions instead of repeating the eight shared presets. Each has a compact first-person hand path and its own third-person arm and torso movement. Cut easing uses `OUT` instead of the shared presets' sharper `OUT_EXPO`; wind-up occupies 28%, impact arrives at 48%, follow-through ends at 66%, and recovery returns to rest at 100%. Durations range from 13 to 18 ticks. Eclipse Sweep and Final Decree have an intermediate passing pose; the two thrusts have a recoil pose. All twelve retain interruption blending and automatic left-hand mirroring.

The ordered preview IDs (prefix each with `/swinganim `, optionally followed by `5` for slow playback) are:

1. `abolition_imperial_draw` — measured horizontal cut.
2. `abolition_silk_return` — compact backhand.
3. `abolition_royal_lunge` — thrust from a low guard.
4. `abolition_guillotine` — descending diagonal.
5. `abolition_crown_lift` — rising backhand.
6. `abolition_eclipse_sweep` — rounded sweep through a raised passing pose.
7. `abolition_oblique_rise` — shallow rising forehand.
8. `abolition_sovereign_fall` — overhead cut angled across the center.
9. `abolition_needle_decree` — thrust from a high guard.
10. `abolition_low_reprisal` — low backhand.
11. `abolition_ascendant_reap` — steep rising forehand.
12. `abolition_final_decree` — broad diagonal finisher.

The particle chain uses thinner silhouettes, two main streak layers, shorter lifetimes, smaller orientation jitter and a fine gold aftertrail. Finisher spirals and crossed cuts are narrower so they leave the target visible. The Crown Lift trail now follows the rising backhand direction. These are visual tuning changes; damage and attack charge rules remain the same.
