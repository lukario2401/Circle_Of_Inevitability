# Testing and tuning slashes in the game (/slashfx)

`/slashfx` plays a `SlashFx` from the code and lets you change its numbers on the spot, so you can find the right values without restarting the game. It needs cheats, like `/palefx`.

What a `SlashFx` is and what each of its methods does is in the comment at the top of `particles/fx/SlashFx.java`.

---

## The short version

```
/slashfx WormOfTime.COMBO_TILTED_RING_4
```

plays that constant as it is in the code. It appears around you, turned the way you look, three times in a row.

```
/slashfx WormOfTime.COMBO_TILTED_RING_4 pitch 45 roll 20
```

plays the same slash with those two numbers changed. Everything you do not name stays as it is in the code. Press the up arrow in the chat to get the line back, change a number, press Enter, look again.

When it looks right, the chat line shows the changed part as Java:

```
Playing WormOfTime.COMBO_TILTED_RING_4 with .rotation(0f, 45f, 20f)   (the whole slash is in the log)
```

Type that into the constant. The whole slash is also written to the game log (the Run window in IntelliJ), where it can be copied:

```
[slashfx] WormOfTime.COMBO_TILTED_RING_4 pitch 45 roll 20 =
SlashFx.arc("slash/band")
        .color(0xEE52E3C4).tailColor(0xA05B3B33).headColor(0xF2F4F2C9).translucent()
        .radius(2.4f).arc(-270f).width(1.2f).taper(SlashFx.Taper.COMET)
        .layers(3).spread(0.28f)
        .lifetime(12).sweep(4).spin(-8f)
        .rotation(0f, 45f, 20f);
```

---

## The name

`Class.CONSTANT`: the name of the Pathway class, a dot, the name of the `SlashFx` constant in it. Press TAB after `/slashfx ` for the list; typing a part of the constant's name narrows it.

Two more names start a new slash that is not in the code yet:

| Name | Starts from |
|---|---|
| `arc` | `SlashFx.arc("slash/streaks")` with its defaults |
| `line` | `SlashFx.line("slash/smooth")` with its defaults |

```
/slashfx arc texture slash/band radius 2 arc 200 color FF70F0E0 core FFFFFFFF
```

## What can be changed

After the name come pairs: a word and its value. The words are the names of the `SlashFx` methods, so `.radius(2.4f)` in the code is `radius 2.4` here. TAB lists the words that are left.

| Word | In the code | Value |
|---|---|---|
| `radius` | `.radius(2.4f)` | blocks |
| `endRadius` | `.endRadius(3.6f)` | blocks. Different from `radius` makes a spiral |
| `arc` | `.arc(-270f)` | degrees. Negative sweeps the other way |
| `width` | `.width(1.2f)` | blocks |
| `taper` | `.taper(SlashFx.Taper.COMET)` | `crescent`, `comet` or `uniform` |
| `layers` | `.layers(3)` | whole number |
| `spread` | `.spread(0.28f)` | blocks between the streaks |
| `lifetime` | `.lifetime(12)` | ticks |
| `sweep` | `.sweep(4)` | ticks it takes to draw itself |
| `delay` | `.delay(2)` | ticks |
| `spin` | `.spin(-8f)` | degrees per tick |
| `yaw`, `pitch`, `roll` | `.rotation(yaw, pitch, roll)` | degrees. Name one, two or all three; the others keep their value |
| `color`, `tailColor`, `headColor`, `core` | `.color(0xEE52E3C4)` ... | `EE52E3C4`: alpha, red, green, blue, as in the code without the `0x`. Six digits get full alpha |
| `fromCenter`, `translucent`, `flat` | `.fromCenter()` ... | `true` |
| `texture` | `SlashFx.arc("slash/band")` | a texture in `textures/particle/`. Only for `arc` and `line` |

They are applied in the order you write them, like the methods in the code. So `color` after `tailColor` sets the tail back to the new color, as `.color(...)` does.

## How it is shown

These four are not part of the slash. They only say how the command shows it, and they are left out of the Java.

| Word | Does | Without it |
|---|---|---|
| `slow` | Plays it this many times slower, to look at its shape: `slow 4` | normal speed |
| `times` | How often it is played in a row, 1 to 20 | 3 |
| `height` | Blocks above your feet | 1.1, about where the melee combos play theirs |
| `ahead` | Blocks in front of you | 0: around you |

```
/slashfx WormOfTime.COMBO_GROUND_SPIRAL height 0.1 slow 3 times 5
/slashfx AttendantOfMysteries.DAGGER_STREAK ahead 3 height 1.5
```

## Good to know

- **It stays where you stood.** The place and the direction are taken when you send the command, so you can walk around the slash and look at it from the side or from above while it repeats. A ring around yourself is easier to judge in third person (F5).
- **It is the exact slash.** An ability often plays its slash with `.varied(random, ...)`, which mirrors it half the time and tilts it a little. The command never does, so what you see is the constant itself.
- **The code is not changed.** The command only shows. Put the numbers you like into the constant yourself.
- **A new or renamed constant** is known after the game is started again. The command reads the `static` `SlashFx` fields of the classes in `SOURCES` at the top of `particles/fx/SlashFxCommand.java`; a new Pathway class goes into that list.
- **`fromCenter`, `translucent` and `flat` can be switched on, not off**, like their methods. To see a slash without one of them, try the numbers on a constant that does not have it.
- A slash is sent to players within 32 blocks.

## Mistakes

| The chat says | Meaning |
|---|---|
| `No slash called ...` | The name is not `Class.CONSTANT` of a `SlashFx`, or the class is not in `SOURCES`. Press TAB |
| `"radius" has no value` | A word without its value. Write pairs: `radius 3 arc -200` |
| `radius needs a number` | The value is not a number. `2.4`, `-30` and `2.4f` all work |
| `sweep needs a whole number` | `lifetime`, `sweep`, `delay`, `layers`, `times` and `slow` take no decimals |
| `"..." cannot be changed` | Not one of the words above |
| `texture only works with arc and line` | A slash from the code keeps its own texture |

## Without the command

A slash can also be played with the game's own `/particle`, like a model. Every value must be written out, the names are different, and its yaw is not the way you look but a fixed direction (0 = south):

```
/particle circle_of_inevitability:slash{texture:"circle_of_inevitability:slash/band",color:"EE52E3C4",tail_color:"A05B3B33",head_color:"F2F4F2C9",additive:0b,radius:2.4f,arc:-270f,width:1.2f,taper:"comet",layers:3,spread:0.28f,lifetime:12,sweep:4,spin:-8f,rotation:[0.0,30.0,30.0]} ~ ~1.1 ~ 0 0 0 0 1
```

Its names: `texture`, `color`, `tail_color`, `head_color`, `core_color`, `additive` (`0b` = translucent), `flat`, `shape` (`"arc"` or `"line"`), `radius`, `end_radius`, `arc`, `width`, `taper`, `layers`, `spread`, `lifetime`, `sweep`, `delay`, `spin`, `from_center`, `rotation` (`[yaw, pitch, roll]`).
