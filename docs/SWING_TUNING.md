# Live sword swing tuning

`/swinganim` is a client-only workbench for custom weapon swings. Change numbers in chat and see the
result immediately, in first person or F5, without rebuilding the mod, reconnecting, or restarting
Minecraft. It works with the shared presets, Prince Of Abolition's swings, and future animations
registered through `SwingAnimations.register`.

No OP permission is needed. Previews work on your own player with any pathway. Hold a sword in your
main hand: an empty hand, maps, crossbows, active item use, scoping and spin attacks retain vanilla
first-person rendering. Third-person previews use the same animation data. Only your client sees
these edits; damage, attack cooldowns, server hit effects and other clients' animation definitions
are unchanged. Overrides of existing ids also apply to ordinary swings using those ids on your client,
including other players you see.

## Start tuning

Enter these commands one at a time while holding a sword:

```text
/swinganim list
/swinganim copy horizontal my_cut
/swinganim loop my_cut 4
/swinganim inspect my_cut
/swinganim edit my_cut key 2 item_yaw 95
/swinganim edit my_cut key 2 arm_yaw -80
/swinganim edit my_cut ticks 18
/swinganim edit my_cut easing 2 out
/swinganim global offset_y 0.3
```

The repeating preview restarts with every edit, keeping its 4x slowdown. `item_yaw` changes the blade
in first person; `arm_yaw` changes the sword arm in third person. Press F5 to compare both views.
The numbers above refer to the copied `horizontal` preset; other animations can have different keys.

For a still view of the impact, use:

```text
/swinganim pose my_cut 0.46
/swinganim edit my_cut key 2 item_roll 15
```

The sword stays at 46% of the swing, and edits update the held pose immediately. `pose` uses a fraction
of the entire animation, rather than a keyframe index. To resume motion, use `loop` or a normal preview.

When satisfied:

```text
/swinganim save
/swinganim export my_cut
/swinganim stop
```

`save` keeps local overrides across game restarts. `export` writes a Java expression for use in source.
A copied id is available to previews immediately; to make normal attacks automatically select that new
id, add it to a `SwingRule` in source (see [ANIMATIONS.md](ANIMATIONS.md#add-a-rule-for-another-weapon-or-class)).
To tune an existing attack chain directly, edit its existing ids instead of copying them.

## Commands

Tab completion suggests registered ids, keyframe indices, editable fields, easing names and globals.
`/swinganim` or `/swinganim help` displays a quick reference.

| Command after `/swinganim` | Result |
| --- | --- |
| `list` | List built-in and custom ids, with effective durations at the current global speed. |
| `<id> [slow]` | Play once; optional slowdown is an integer from 1 to 20. |
| `loop <id> [slow]` | Repeat from rest until stopped. |
| `pose <id> <progress>` | Hold a pose at progress 0..1. |
| `stop` | End the local preview, repeat or held pose. Keep edited values. |
| `inspect <id>` | Show duration, mirror flag and every key with its zero-based index and numbers. |
| `copy <id> <new_id>` | Create and preview a new id from a complete copy; existing ids cannot be overwritten by copy. |
| `edit <id> ticks <ticks>` | Set total length, 1..1200 ticks; 20 ticks is one second before speed/slowdown. |
| `edit <id> key <index> <field> <value>` | Set one key's timing or pose field. |
| `edit <id> easing <index> <easing>` | Change how the movement **into** that key accelerates. |
| `edit <id> add <at>` | Insert a key at time 0..1, sampling the current pose there, with linear easing. |
| `edit <id> remove <index>` | Delete a key; at least two must remain. |
| `edit <id> mirror` | Toggle whole-animation mirroring. |
| `global` | Show the five current global settings. |
| `global <name> <value>` | Set one global value; refresh the selected preview. |
| `global reset` | Restore global values from the compiled defaults. |
| `reset <id>` | Remove that override: a built-in returns to its compiled definition; a custom-only id is removed. |
| `save` | Write all current overrides and global edits to the client config file. |
| `load` | Reload that file, replacing in-memory edits, then refresh the selected preview. |
| `export <id>` | Write a fluent Java definition and current global values to a text file. |

Editing another id starts a new once-only preview. Editing the selected id preserves its slowdown,
repeat mode or held progress. Preview restarts begin from rest so earlier test poses do not influence
the result. Ordinary attacks still blend between swings as before. A real local attack, changing the
held item type, or leaving the world ends an active repeat/held preview. `stop` does not disable the
normal pathway attack rules or discard edits.

Keys are sorted by time. Moving or adding a key can change indices; run `inspect` again before your
next edit. Times must be distinct and there must be 2..128 keys. Ids use lowercase letters, digits
and underscores, start with a letter and have at most 80 characters. Prefer names such as `my_cut`
rather than command names such as `list`, which are treated as literal subcommands in the short
preview syntax (the `loop` and `pose` forms accept those ids).

## Fields and units

| Field | Meaning | Accepted range |
| --- | --- | --- |
| `at` | Keyframe time as a fraction of the animation. Changing ticks preserves these fractions. | 0..1, unique |
| `item_x`, `item_y`, `item_z` | First-person hand offsets in blocks: +x right, +y up, +z toward the camera. | -16..16 |
| `item_pitch`, `item_yaw`, `item_roll` | First-person blade rotation in degrees. Applied roll, yaw, pitch. | -3600..3600 |
| `item_scale` | First-person item size multiplier, before global scale. | 0.01..10 |
| `arm_pitch`, `arm_yaw`, `arm_roll` | Third-person sword-arm rotation in degrees. | -3600..3600 |
| `body_yaw` | Third-person torso twist in degrees. | -3600..3600 |
| `weight` | Blend weight for global offsets/scale and suppressing vanilla's post-attack dip. | 0..1 |

Easings: `linear`, `in`, `out`, `in_out`, `out_expo`, `out_back` (case-insensitive).
A pose field edit activates a REST key by giving it weight 1; otherwise its weight is preserved.
`weight` can also be set explicitly. Keep the start and final key at REST for a smooth return to
vanilla: zero translations/angles, scale 1 and weight 0. Leave those keys untouched while tuning the
middle phases. The mirror flag flips x, yaws and rolls at playback; `inspect` and edits show the
underlying, unmirrored definition. Left-handed rendering adds its usual hand mirroring.

| Global name | Compiled default | Effect |
| --- | --- | --- |
| `offset_x` | -0.18 | First-person horizontal offset during swings, mirrored for the left hand. |
| `offset_y` | 0.24 | First-person vertical offset. |
| `offset_z` | -0.25 | First-person depth offset, negative away from the camera. |
| `scale` | 1.2 | First-person size multiplier during swings. |
| `speed` | 1.0 | Playback multiplier in both views; 0.8 is slower, 1.25 faster. |

Global offsets accept -16..16; global scale and speed accept 0.05..10. Non-finite values and invalid
indices, durations, duplicate times and other invalid data are rejected. Edits are visual only.
Speed is captured when playback starts; changing it restarts the selected preview and applies to
subsequent normal attacks. Offsets/scale are read each rendered frame.

## Save, reload and bring changes into source

Local settings live in the Minecraft instance's directory:

```text
config/circle_of_inevitability-swinganim.json
config/swinganim-export/<id>.java.txt
```

For `runClient`, these paths are under `run/`. The tuning JSON is loaded automatically on first use
in each client session. Edits stay in memory across world changes, but **save explicitly** before
closing the game to retain them. Resetting also needs `save` if the reset should survive a restart.
`load` replaces unsaved edits with the file's contents. You can edit the generated JSON externally
and run `load` without restarting; it is not automatically watched.

The JSON has `version: 1`, a `globals` object containing changed settings and an `animations` object
mapping ids to complete definitions (`ticks`, `mirror`, `keys`). Each key contains `at`, `easing`
and the pose fields listed above. Use `save` to generate the format. A reload validates every entry
before replacing anything; a malformed file leaves the current session's definitions in place.
A bad file at first startup logs a warning and uses built-in defaults; `/swinganim load` reports the
error in chat. A missing file on an explicit `load` also reports an error.

To make a tuned animation the default for everyone:

1. Run `export <id>` and open the reported `.java.txt` file. It contains the current complete
   definition, mirror flag and weights, plus comments listing current global values.
2. Paste the expression into a `SwingAnimation` field in `SwingAnimations` or another class in the
   same `client.animation` package. Import `SwingAnimation.Pose` and `SwingAnimation.Easing` if needed.
   The exported `.withWeight(...)` helper is package-private so REST/blend values survive exactly.
3. Register new fields with `SwingAnimations.register(...)`, then reference new ids from `SwingRules`
   if normal attacks should select them. Existing ids already used by a rule need no rule change.
4. Copy global values into the corresponding `FIRST_PERSON_OFFSET_*`, `FIRST_PERSON_SCALE` and `SPEED`
   defaults if desired. Rebuild and distribute the mod for other clients to receive these defaults.
5. Use `reset <id>`, `global reset`, then `save` on the tuning client before checking compiled defaults;
   otherwise its saved overrides continue to take precedence over source changes.

Java exports and the tuning file are local development files; commit the resulting Java source,
not your instance's `run/config` files.

## Verification

`./gradlew build` includes `swingAnimationEditsTest`. It checks immutable edits, sorted timing,
validation failures, mirrored key insertion, JSON round trips, and compiles a Java export before
comparing it with the original animation. Run only that check with:

```text
./gradlew swingAnimationEditsTest
```

For visual checks, use the quick-start commands in first person and both F5 views. Confirm the held
pose changes while editing; inspect left-handed rendering; try adding/moving/removing a key; save,
change values, load, and restart the client to confirm persistence. Check `reset` on both a built-in
and a copied id. Verify a duplicate key time, out-of-range index and negative scale report errors
while leaving the last valid edit intact. Finally stop the preview, attack normally, switch items
and change worlds to check cleanup. Another client should keep seeing its own original definitions.
