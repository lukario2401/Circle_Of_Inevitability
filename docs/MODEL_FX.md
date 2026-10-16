# Adding 3D model effects (ModelFx)

ModelFx lets any Pathway show a 3D model in the world for a while: a summoned weapon, ice on a frozen mob, a floating rune, a shield... The Hand Of Order hammer, the freeze crystals and every Prince Of Abolition ability are built with it.

Nothing needs registering. You make a model, drop two files into the right folders, describe the effect in code and play it.

This guide is about the models themselves. What to do *with* them (make one fly and hit things, count down, link damage, take over a mob...) is in [SPELL_KIT.md](SPELL_KIT.md).

---

## 1. Make the model in Blockbench

1. **File → New → Java Block/Item.**
2. Build it out of cubes and paint it.
3. Keep these rules in mind:
   - The model's **front is North (-Z)**. When played, the front points the way you tell it to face (usually the player's yaw). Up is +Y.
   - Blockbench keeps this model type between **-16 and 32 pixels** on every axis (3 blocks) and, depending on its version, turns a cube only by **-45, -22.5, 0, 22.5 or 45 degrees** on one axis. Make big things small and enlarge them with `.scale(...)` in code. The mod reads the files itself and has neither limit, but every model in it stays inside them.
   - Only the cubes count: their place, their turn and what each face shows. A `parent`, `display` settings and a cube's `shade` setting are not read.
4. Decide the **pivot**: the point the model turns and scales around and that sits exactly on the spawn position.
   - Default: bottom center `(8, 0, 8)`, which is right for things standing on the ground (crystals, pillars).
   - Weapons: put it at the grip (the hammer uses `(8, -10, 8)`).
   - Note it down; you'll give it to `.pivot(x, y, z)`.

## 2. Put the files in place

| What | Where |
|---|---|
| Model (File → Export → Export Block/Item Model) | `src/main/resources/assets/circle_of_inevitability/models/model_fx/<name>.json` |
| Texture PNG | `src/main/resources/assets/circle_of_inevitability/textures/model_fx/<name>.png` |

Open the JSON and check the `textures` part points at the PNG like this:

```json
"textures": {
  "0": "circle_of_inevitability:model_fx/<name>",
  "particle": "circle_of_inevitability:model_fx/<name>"
}
```

Blockbench usually writes this for you if the PNG is saved in that folder. Subfolders work too (`model_fx/swords/blade.json` → `"circle_of_inevitability:model_fx/swords/blade"`).

A model can use several textures (`"1"`, `"2"`, `"3"`...), as the Hand Of Order sword does; each one just needs its PNG in `textures/model_fx/` (or a folder inside it). A face whose texture is not found there is not drawn. The PNG can have any size. Animated textures work like on blocks: stack the frames vertically in the PNG and put a `<name>.png.mcmeta` next to it, e.g. `{"animation":{"frametime":3}}`.

Every model in `models/model_fx/` is loaded automatically when the game starts, and again whenever the resources are reloaded (F3+T). So a changed model or texture can be looked at without restarting; in the dev client, build the project first so the changed file reaches the running game.

**Quick check in game** (shows it at your feet for 5 seconds):

```
/particle circle_of_inevitability:model{model:"circle_of_inevitability:model_fx/<name>",glow:1b,lifetime:100} ~ ~ ~ 0 0 0 0 1
```

If nothing shows, either the model name is wrong, or the file could not be read; in the second case the game log says `Could not read the model_fx model`. If the model shows with faces missing, those faces name a texture that is not in `textures/model_fx/`. The yes/no options from section 3 can be added the same way: `see_through:1b`, `unshaded:1b`.

You can try an animation the same way, without restarting the game. `keys` takes the same things as the code in section 4 (`from` + `tick` is a `.during`):

```
/particle circle_of_inevitability:model{model:"circle_of_inevitability:model_fx/<name>",glow:1b,lifetime:100,keys:[{tick:20,pitch:90f,ease:"in"},{tick:30,spin:90f},{tick:100,forward:12f},{from:30,tick:100,yaw:1440f}]} ~ ~2 ~ 0 0 0 0 1
```

## 3. Describe the effect in code

Make it a constant at the top of your Pathway class, like `ParticleFx` and `SlashFx`. Every method returns a new copy, so you can make variants freely.

```java
private static final ModelFx RUNE = ModelFx.of("rune")      // models/model_fx/rune.json
        .scale(1.5f)                  // 1 = Blockbench size (16 px = 1 block)
        .pivot(8, 8, 8)               // pixels, Blockbench coordinates
        .glow()                       // full bright, visible at night
        .aura(0xFF80C0FF, 0.1f, 3)    // glowing outline: ARGB color, thickness in blocks, softness
        .lifetime(60)                 // ticks (20 = 1 second)
        .fade(5, 10)                  // fade in 5 ticks, fade out the last 10
        .spin(6f);                    // turns around its own up axis the whole time, degrees per tick
```

### All options

| Option | What it does |
|---|---|
| `ModelFx.of("name")` | Model from `models/model_fx/name.json` |
| `.scale(s)` / `.scale(x, y, z)` | Size. Per-axis version for stretching (e.g. fit a mob's box) |
| `.pivot(x, y, z)` | Turn/scale point in Blockbench pixels (default `8, 0, 8`) |
| `.rotation(yaw, pitch, roll)` | Fixed starting orientation (added to what you pass when playing it) |
| `.rotated(yaw, pitch, roll)` | Turns a copy further: added to the orientation it already has |
| `.color(argb)` | Tint and opacity. `0x80FFFFFF` = half see-through |
| `.glow()` | Full bright: not darkened by night or caves |
| `.unshaded()` | Every face equally bright, whichever way it points. For fire, light and energy (see below) |
| `.seeThrough()` | For textures with transparent pixels (ice, glass, energy). Drawn last so things inside stay visible |
| `.aura(argb, blocks, layers)` | Glow around it. Alpha of the color = strength. 1 layer = hard edge, 3-4 = soft |
| `.aura(argb, blocks)` | The same with the layers it already has (3 unless you changed them) |
| `.noAura()` | Remove the aura from a copy |
| `.lifetime(ticks)` | How long it stays |
| `.delay(ticks)` | Wait before appearing |
| `.fade(in, out)` | Fade in / fade out ticks |
| `.fadeIn(ticks)` / `.fadeOut(ticks)` | Change one of the two and keep the other |
| `.spin(degPerTick)` | Keeps turning around its own up axis the whole time |
| `.key(tick, pose)` / `.key(tick, pose, ease)` | Animation keyframe (see below). A model sends at most 64 keys and `.during` animations; more are left out |
| `.during(from, to, pose)` / `.during(from, to, pose, ease)` | Extra animation that runs at the same time as the keyframes (see below) |
| `.noKeys()` | Remove all keyframes and `.during` animations |
| `.frames(n)` | The model is `n` models, `<name>_0` .. `<name>_(n-1)`, and changes shape (see "Models that change shape"). 64 at most |
| `.smooth()` | With `.frames(n)`: glide between the frames instead of jumping |
| `.mirrored()` | Flipped left-right, like in a mirror: a left hand out of a right hand |
| `.tag("name")` | A name for a model, so it can be removed on its own (see section 5) |

### Solid things, or fire and light?

Minecraft shades every model: the top is brightest, the sides are darker and the underside is darkest. That is what makes a hammer or an ice spike look solid, and `.glow()` does not turn it off.

On fire it looks wrong: a white-hot flame turns grey on two of its sides and changes brightness while it spins. So:

| The model is | Use |
|---|---|
| A solid thing: weapon, rock, ice | `.glow()` (and an `.aura(...)` if it should shine) |
| Fire, light, energy, magic circles | `.glow().unshaded().seeThrough()` |

Two things to know about `.aura(...)`:

- It lights up the **whole box** of every cube and ignores see-through pixels. On a flat plate or a flame texture you get a glowing rectangle. Leave the aura off those. If a model needs both (a rock with flames), split it into two models and play them together with the same keys, like `meteor` + `meteor_tail`.
- Every cube adds its own glow, so it piles up on models made of cubes inside cubes. A strong aura turns a dark layered model into one bright blob. Use a low alpha there (the entropy sphere uses `0x40......`).

Models built in layers (a core with see-through shells around it) need nothing special: the faces are sorted by distance, so the order of the cubes in Blockbench does not matter. The inside only shows through pixels of the outer cubes that are see-through in the texture.

A face is only drawn when it is seen from its front. From behind (from inside a cube) it is not there.

## 4. Animate it with keyframes

A keyframe says: **by this tick, these things have these values.** Ticks count from when the model appears.

```java
ModelFx sword = SWORD
        .key(20, ModelFx.pose().pitch(90), ModelFx.Ease.IN)     // ticks 0-20:  tips forward
        .key(30, ModelFx.pose().yaw(90))                        // ticks 20-30: turns right, STILL tipped forward
        .key(60, ModelFx.pose().forward(10), ModelFx.Ease.IN);  // ticks 30-60: flies forward, still tipped + turned
```

The rules:

- **A key only changes what it names.** `pose().yaw(90)` touches the yaw and nothing else, so the pitch from the key before stays. Keys "stick".
- **A key starts moving when the key before it is reached** (the first one starts at tick 0), and uses its own ease.
- **Everything starts at rest**: no offset, no rotation, scale 1, alpha 1. Use `.key(0, ...)` to start from something else, e.g. `.key(0, ModelFx.pose().scale(0.3f).alpha(0f))` to start small and invisible.
- After the last key it holds what it has.
- `ModelFx.rest()` is a pose that puts **everything** back to the start in one go: `.key(80, ModelFx.rest())`.

What a pose can change:

```java
ModelFx.pose()            // changes nothing until you name something
    .forward(2f)          // blocks in front (the way the effect faces, however the model is turned)
    .up(1f)               // blocks up
    .right(0.5f)          // blocks to the right
    .pitch(90)            // + tips the top forward (a downward swing)
    .yaw(45)              // + turns right, around the world's up
    .roll(30)             // + tips the top to the right
    .spin(360)            // + turns around its OWN up axis (up in Blockbench), however it is tilted
    .scale(0.5f)          // multiplies .scale(...)
    .alpha(0f)            // 0 = invisible
```

Two short forms: `pose().move(forward, up, right)` and `pose().rotation(yaw, pitch, roll)`.

All rotations happen around the pivot. Go past 360 to keep turning: `yaw(720)` is two full turns.

**How rotations combine.** They are always applied in the same order, whatever order you write them in: `spin` first, then `roll`, then `pitch`, then `yaw`. For a sword modelled standing up:

- `pitch(90)` tips it over so the blade points forward. It is now lying on its **edge**.
- adding `spin(90)` gives it a quarter turn around its own length: it still points forward, and now lies **flat** against the ground.
- adding `yaw(90)` instead swings the forward-pointing blade round to the right. It ends up sideways-on and still on its edge.

### Making a model lie flat

For a thin model that stands upright in Blockbench (a sword, a card, a rune plate):

| You want | Use |
|---|---|
| Lying flat, top pointing forward | `pose().pitch(90).spin(90)` |
| Lying flat, top pointing right | `pose().roll(90)` |
| Lying on its edge, top pointing forward | `pose().pitch(90)` |
| Flat and spinning like a thrown disc | one of the flat ones, then animate `yaw` (it turns around the world's up, so it stays flat) |

Once `pitch` is 90, no amount of `yaw` or `roll` will lay it flat: `yaw` only turns it around the world's up, and `roll` happens before the pitch. `spin` is the one that turns it around its own length. If the flat side ends up facing the wrong way, use `-90`.

To have it flat from the first frame, with no rotating into place, put it in a tick-0 key: `.key(0, ModelFx.pose().pitch(90).spin(90))`.

### Several things at the same time

**Same timing:** put them in one key. They move together.

```java
.key(60, ModelFx.pose().forward(10).up(2).scale(2f))    // flies forward, rises and grows, all during the same ticks
```

**Their own timing:** `.during(fromTick, toTick, pose, ease)` runs next to the keyframes. Add as many as you want; they can overlap the keys and each other.

```java
ModelFx sword = SWORD
        .key(20, ModelFx.pose().pitch(90), ModelFx.Ease.IN)
        .key(30, ModelFx.pose().yaw(90))
        .key(100, ModelFx.pose().forward(12), ModelFx.Ease.IN)      // flies forward during ticks 30-100...
        .during(30, 100, ModelFx.pose().spin(1440))                 // ...and spins 4 full turns on the way
        .during(0, 10, ModelFx.pose().scale(1.5f), ModelFx.Ease.OUT_BACK);  // ...and pops bigger in the first 10 ticks
```

Which spin to animate:

| You want | Animate |
|---|---|
| Turn around its own length/up axis (a drill, a spinning top), whatever way it's tilted | `spin` |
| Flat spin around the world's up (a thrown blade, a rotor) | `yaw` |
| Tumble end over end | `pitch` |

If a `.during` and a key change the *same* thing during the same ticks, the one that started later takes over.

Example: a sword that tips forward, turns flat, then flies forward while it spins flat like a thrown disc. The Hand Of Order sword (`HAND_OF_ORDER_SWORD` in `HandOfOrder.java`) is built this way, with ticks of its own.

```java
ModelFx sword = HAND_OF_ORDER_SWORD
        .key(20, ModelFx.pose().pitch(90), ModelFx.Ease.IN)       // ticks 0-20: tips forward (on its edge)
        .key(30, ModelFx.pose().spin(90), ModelFx.Ease.IN)        // 20-30: quarter turn around its own length -> flat
        .key(40, ModelFx.pose().forward(5), ModelFx.Ease.IN)      // 30-40: lunges forward (still flat)
        .key(100, ModelFx.pose().forward(12), ModelFx.Ease.IN)    // 40-100: flies on
        .during(30, 100, ModelFx.pose().yaw(1440));               // ticks 30-100: 4 full turns, staying flat

ParticleShapes.model(sl, sword, player.position().add(0, 3, 0), player.getYRot(), 0f, 0f);
```

| Tick | pitch | spin | yaw | forward |
|---|---|---|---|---|
| 0 | 0 | 0 | 0 | 0 |
| 20 | 90 | 0 | 0 | 0 |
| 30 | 90 | 90 | 0 | 0 |
| 40 | 90 | 90 | about 206 (spinning) | 5 |
| 100 | 90 | 90 | 1440 | 12 |

A `.during` carries on from wherever that value already is. If an earlier key had left the yaw at 90, four more turns would be `yaw(90 + 1440)`.

**Easing** (how it gets to a keyframe from the previous one):

| Ease | Feels like |
|---|---|
| `LINEAR` | constant speed |
| `IN` | slow then fast: falling, slamming |
| `OUT` | fast then slow: landing softly |
| `IN_OUT` | slow, fast, slow: winding up |
| `OUT_BACK` | overshoots then settles: popping into existence |
| `OUT_BOUNCE` | bounces at the end |

> **Effects written before keys stuck:** a key used to be a complete pose, so anything it didn't name snapped back to rest. If an old effect now stays small, invisible or rotated, add the value it should return to in the next key.

Example: the Hand Of Order hammer swing (`ORDER_HAMMER` in `HandOfOrder.java`, where the ticks of the slam are constants). It starts small and invisible, so the next key says what size and alpha to go to.

```java
ORDER_HAMMER
    .key(0,  ModelFx.pose().pitch(-20).scale(0.3f).alpha(0f))
    .key(6,  ModelFx.pose().pitch(-35).scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK)   // appears behind you
    .key(13, ModelFx.pose().pitch(-55), ModelFx.Ease.IN_OUT)     // winds up
    .key(18, ModelFx.pose().pitch(90),  ModelFx.Ease.IN)         // slams down
    .key(20, ModelFx.pose().pitch(85),  ModelFx.Ease.OUT)        // small bounce
    .key(23, ModelFx.pose().pitch(90),  ModelFx.Ease.IN);
```

### Models that change shape

A keyframe moves, turns and scales the whole model. For something that changes its *shape* (a hand closing, jaws snapping, wings beating), make one model per step of the movement and let the effect go through them:

1. Build the first step and export it as `<name>_0.json`.
2. Copy it, move or turn the cubes for the next step, and export that as `<name>_1.json`, and so on. Don't add or delete cubes in between and don't repaint them, only move and turn them. All steps can share one texture.
3. Describe it with `.frames(count)` and animate `frame` in the keys, like any other value:

```java
private static final ModelFx HAND = ModelFx.of("pale_emperor/skeletal_hand_grasp")
        .frames(5).smooth()                 // skeletal_hand_grasp_0 .. _4, gliding from one to the next
        .scale(1.5f).pivot(8, -16, 8).lifetime(60).fade(3, 8)
        .key(0,  ModelFx.pose().frame(3).up(-4.5f))                     // starts half closed, under the ground
        .key(10, ModelFx.pose().up(0), ModelFx.Ease.OUT)                // rises out of it
        .during(4, 14,  ModelFx.pose().frame(0), ModelFx.Ease.OUT)      // while rising, opens wide
        .during(26, 32, ModelFx.pose().frame(4), ModelFx.Ease.IN);      // snaps shut
```

- `frame(n)` is which step shows. It is animated like everything else: it eases, it can hold, it can run backwards (`frame(4)` then `frame(0)` closes and reopens).
- Without `.smooth()` the model jumps from step to step, like a flip book. With it the cubes travel from where they are in one step to where they are in the next, so 5 steps are enough for a fluid movement.
- `.smooth()` needs steps made of the same cubes in the same order (point 2 above). Steps that don't match just jump. Keep each cube's turn between two steps at 45 degrees or less, or it looks squashed half way.
- Test it: `/particle circle_of_inevitability:model{model:"circle_of_inevitability:model_fx/pale_emperor/skeletal_hand_grasp",frames:5,smooth:1b,glow:1b,lifetime:100,pivot:[8.0,-16.0,8.0],keys:[{tick:20,frame:4f},{tick:60,frame:4f},{tick:80,frame:0f}]} ~ ~1 ~ 0 0 0 0 1`

## 5. Play it

Works from server code (everyone nearby sees it). Call it inside your ability method:

```java
// at a spot, facing the player's direction
ParticleShapes.model(sl, RUNE, position, player.getYRot(), 0f, 0f);

// facing the way an entity faces
ParticleShapes.model(sl, RUNE, position, player);

// stuck to an entity: moves with it, disappears when it dies or unloads
// offset is from its feet, in world directions
ParticleShapes.modelOn(sl, RUNE, mob, new Vec3(0, mob.getBbHeight(), 0), 0f, 0f, 0f);

// worn by an entity: stuck to it AND turning when it turns its body (wings, a shield)
// yaw 0 = the model's front looks the way the entity does
ParticleShapes.modelOnTurning(sl, WING, player, new Vec3(0, player.getBbHeight() * 0.75, 0), 0f, 0f, 0f);

// worn on the HEAD: it sits on top of the head and looks where the head looks, up and down too
// (a crown, a halo, horns, a mask). The number = blocks above the top of the head
ParticleShapes.modelOnHead(sl, CROWN, player, 0, 0f, 0f, 0f);

// with its top (up in Blockbench) pointing along a direction: spears, arrows, meteors
ParticleShapes.modelAlong(sl, SPEAR, from, player.getLookAngle());

// remove everything stuck to an entity
ParticleShapes.clearModels(sl, mob);

// remove only the models with a certain tag
ParticleShapes.clearModels(sl, mob, "my_mark");
```

**Tags.** One mob can carry several effects at once: ice, a mark from your Pathway, a marionette's strings. `clearModels(sl, mob)` wipes all of them. So give your model a tag when you make it, and clear by that tag:

```java
private static final ModelFx MARK = ModelFx.of("rune").tag("my_mark");

ParticleShapes.clearModels(sl, mob, "my_mark");        // cast again: one mark, not two
ParticleShapes.modelOn(sl, MARK, mob, new Vec3(0, mob.getBbHeight() + 0.4, 0), 0f, 0f, 0f);
```

`Freeze` does this (its ice is tagged `freeze`), so thawing a mob no longer removes other effects from it.

A tagged model that is **not** stuck to an entity can be removed early too: `ParticleShapes.clearModels(sl, position, "tag")` removes every such model with that tag (the position only says from where the message is sent). Give every cast a tag of its own for this; see "Several models on one entity: tags" in [SPELL_KIT.md](SPELL_KIT.md).

**Stuck or worn?** `modelOn` keeps the direction you gave it: a mark over a head, ice round a body, strings going up. `modelOnTurning` turns with the entity's body, in the air and while flying too: wings on a back, something held. Its offset does not turn, so use the offset for the height only and put anything sideways into the model's keys (`pose().right(...)`, `pose().forward(...)`), which do turn. It follows the body, not the head: wings stay on the back while the player looks around.

**On the head.** `modelOnHead` is for what the head wears. It follows the head left and right and up and down (it nods around the neck, as a head does), and it sits on top of the head however tall the entity is or stands, so it comes down with a player who sneaks. Instead of an offset it takes one number, the blocks above the top of the head: `0` rests on it, `0.4` floats over it (a halo), a negative number brings it down in front of the face (a mask). The wearer does not see it in first person, like a helmet. Everyone else does, and so does the wearer in third person (F5). `.seenByWearer()` on the model shows it to the wearer too, for what is held in front of the eyes on purpose; `ParticleShapes.modelInFront(sl, MODEL, player, ahead, below)` does that for a model whose top is its front (a lance, as `Shot` and `modelAlong` use them), pointing where they look. What is stuck to someone concealed is hidden too, unless the model has `.shownWhileHidden()`.

Models are sent to every player within 512 blocks, so a big one (a meteor, a falling blade) is seen from far away. The small 2D particles and slash trails are still only sent within 32 blocks.

Two things about how they are drawn: a model is drawn before water and stained glass, so its aura can look dimmer where water is behind it in the picture, and model textures get no smaller copies for the distance, so a finely patterned one can shimmer a little far away.

> **Watch out:** methods never change the effect you call them on, they return a new one.
> `RUNE.key(...)` on its own line does nothing; keep the result:
> `ModelFx moving = RUNE.key(...).key(...);` and play `moving`.

Changing something per cast is just another copy:

```java
ParticleShapes.model(sl, RUNE.scale(3f).lifetime(100), position, yaw, 0f, 0f);
```

### Doing something when the animation reaches a point

The animation runs on the players' screens, so damage and sounds go on the server with `Later.run`, using the same tick as the keyframe:

```java
ParticleShapes.model(sl, hammer, grip, yaw, 0f, 0f);
Later.run(sl, 18, () -> hammerImpact(player, sl, impact));   // 18 = the slam keyframe
```

---

### Models the server moves

A model can be steered tick by tick from the server, the way an entity moves: a raven that chases someone, a ghost that follows a path you work out as you go. Play it once with a tag of its own, then tell it where to be every tick:

```java
String tag = "my_ghost_" + UUID.randomUUID();                                  // its own tag
ParticleShapes.modelFacing(sl, GHOST.tag(tag).lifetime(200), from, direction);   // front (north) looks along direction
// every tick after that:
ParticleShapes.moveModelFacing(sl, tag, where, heading);                        // it glides there during the next tick
// when it is done:
ParticleShapes.clearModels(sl, where, tag);
```

| Call | What it does |
|---|---|
| `ParticleShapes.modelFacing(level, model, position, direction)` | Plays a model with its front (north in Blockbench) looking along `direction`, tipped up or down with it |
| `ParticleShapes.moveModelFacing(level, tag, position, direction)` | Moves the free models with this tag to `position`, front along `direction` |
| `ParticleShapes.moveModelAlong(level, tag, position, direction)` | The same for models played with `modelAlong` (their top points the way) |
| `ParticleShapes.moveModel(level, tag, position, yaw, pitch, roll)` | The same with the angles given yourself |

- Only models that are **not** stuck to an entity move this way (not `modelOn`, `modelOnHead`...).
- The model keeps playing its own keys and frames while it moves: offsets in its keys count from the spot it was moved to, so leave them out (or keep them small) and let the moves do the travelling.
- Each move is one small message to every player in the level, so a few dozen models moved every tick are fine; hundreds are not.
- Angles take the short way round: from yaw 170 to -170 it turns 20 degrees, not 340.
- `combat/Seeker` does all of this for you (see docs/SPELL_KIT.md, "Seeker").

## Ready-made models

These are in `models/model_fx/` and any Pathway can use them. Sizes are at `.scale(1)`.

| Model | What it is | Pivot to use | Notes |
|---|---|---|---|
| `rune_circle` | Flat magic circle, 1 block wide | default (its middle) | White: tint it with `.color(...)`. `.scale(8)` = 8 blocks wide |
| `shock_ring` | Flat ring, 1 block wide | default | White, tint it. Animate `scale` for a shockwave |
| `fireball` | Ball of fire, 0.75 blocks | `(8, 8, 8)` | |
| `fire_blast` | Round burst of flames, about 1 block | `(8, 8, 8)` | For explosions: scale it up fast and fade it out |
| `flame_pillar` | Flames, 1 block wide, 2 tall, animated | default (the base) | Stretch it with `.scale(width, height, width)` |
| `ice_spike` | Tall ice spike with small ones around it, 2 blocks tall | default (the base) | |
| `meteor` + `meteor_tail` | Burning rock and the flames behind it | `(8, 6, 8)` for both | Play both with the same keys. The tail points up, so aim the model's up back along the flight |
| `ice_shell`, `ice_crystals` | The freeze look | default | Used by `Freeze`, see below |
| `order_hammer` | Hand Of Order's golden hammer | grip `(8, -10, 8)` | |
| `hand_of_order/sword` | Hand Of Order's sword standing upright, 2.4 blocks tall | its middle `(8, 8, 8)` | Three textures, two of them animated |
| `prince_of_abolition/spear` | Spear, 3 blocks long, tip up | middle of the shaft `(8, 8, 8)`, tip `(8, 32, 8)` | |
| `prince_of_abolition/hammer` | Dark hammer | grip `(8, -10, 8)`, flat top of the head `(8, 30, 8)` | |
| `prince_of_abolition/blade` | Sword hanging point down, 3 blocks tall | its point `(8, -16, 8)` | |
| `prince_of_abolition/entropy_orb` + `entropy_disc` | Dark sphere and the ring around it | `(8, 8, 8)` for both | Spin them opposite ways |
| `orb` | Soft ball of light, 0.75 blocks | `(8, 8, 8)` | White: tint it with `.color(...)`. A core with see-through shells. For shells of air, energy, souls |
| `marionette_cross` | Wooden control bar with five strings hanging from it, 1 block wide | default = the lower ends of the strings; the bar is 1.75 blocks above | Used by `Marionette`. Stretch it to the mob with `.scale(width, height, width)` |
| `attendant_of_mysteries/paper_dagger` | Folded paper dagger, 1.3 blocks long, tip up | its middle `(8, 10, 8)`, tip `(8, 20, 8)` | |
| `attendant_of_mysteries/paper_figurine` | Flat paper doll, 2 blocks tall | default (its feet) | `.scale(0.3f)` for a small one |
| `attendant_of_mysteries/projection` | A figure of fog in a long coat and top hat, 2.35 blocks tall | its feet `(8, -6, 8)` | Fades toward the ground. Tint it with `.color(...)` |
| `attendant_of_mysteries/needle` | Sewing needle hanging point down, 3 blocks long | its point `(8, -16, 8)`, eye `(8, 32, 8)` | |
| `attendant_of_mysteries/thread` | A taut thread standing up, 3 blocks long, fading toward its top | its lower end `(8, -16, 8)` | White, tint it. Stretch it with `.scale(width, length, width)` |
| `attendant_of_mysteries/fool_card` | Tarot card standing upright, 10 x 16 px | its middle `(8, 8, 8)` | Face and back are different |
| `pale_emperor/gate` | An early, plain gate of obsidian and gold blocks, 3 blocks wide and deep, 2 tall | default (the middle of its base) | No ability uses it; `underworld_gate` took its place |
| `pale_emperor/underworld_gate` | Dark stone gate frame with a pointed arch, 3 blocks wide and tall | the middle of its threshold `(8, -16, 8)` | Both faces are the same |
| `pale_emperor/underworld_gate_door_left`, `_right` | Its two bronze doors | their hinges `(-7, -16, 8)` and `(23, -16, 8)` | Played with the frame's pivot they sit closed in it. To swing them, use the hinge pivots, move them there with `pose().right(-/+ 15/16 * scale)` and animate `yaw` |
| `pale_emperor/underworld_void` | The inside of the doorway: animated mist with eyes | `(8, -16, 8)` | `.glow().unshaded()` |
| `pale_emperor/skeletal_hand`, `skeletal_hand_grab` | The first skeletal hand, open and closed, 3 blocks tall with its forearm | the end of the forearm `(8, -16, 8)` | Palm = front |
| `pale_emperor/skeletal_hand_spread` | Skeletal hand wide open: fingers fanned, tips hooked. 3 blocks tall | the end of the forearm `(8, -16, 8)` | Palm = front, thumb on the model's +X side. `.mirrored()` gives the other hand |
| `pale_emperor/skeletal_hand_grasp` | The same hand closing, in 5 steps: 0 = wide open (the same as `skeletal_hand_spread`), 4 = clenched | `(8, -16, 8)` | `.frames(5).smooth()`, then animate `frame` (see "Models that change shape") |
| `pale_emperor/emperor_hand` | The hand of the Pale Emperor, closing in 5 steps: bleached bone, gold talons and rings, a sigil in the palm, a feathered serpent coiled round the wrist with its head on the back of the hand | the cut end of the forearm `(8, -16, 8)` | `.frames(5).smooth()`. Without keys it shows step 0, the open hand. `.glow()` suits the gold |
| `pale_emperor/underworld_arm` | An arm from behind the Door to the Underworld, closing in 5 steps: pale dead flesh torn to the bone, burial wrappings, black nails, a coin in the palm | `(8, -16, 8)` | `.frames(5).smooth()`. Play several from the gate with different yaw, roll and timing; `.mirrored()` for left arms |
| `pale_emperor/feathered_serpent` | The Feathered Serpent rearing out of the ground, 3 blocks tall, in 3 steps: 0 = mouth shut, its collar of feathers laid back; 2 = jaws wide, collar flared. Gold mask, pale-green eyes, fangs, forked tongue | where the neck leaves the ground `(8, -16, 12)` | `.frames(3).smooth()` |
| `pale_emperor/feathered_serpent_coil` | Its body, coiled in three rings on the ground, 2.5 blocks across | the middle of the coils `(8, -16, 8)` | Play it at the same spot as the head, each with its own pivot: the neck rises from the middle |
| `pale_emperor/pale_wing` | A wing of bleached bone and pale feathers stained yellow-green, in 5 steps: 0 = folded, 4 = spread, 2.5 blocks from shoulder to tip | the shoulder `(-10, 11.5, 8)` | `.frames(5).smooth()`. It grows toward the model's +X (the player's right); `.mirrored()` gives the other wing |
| `pale_emperor/death_skull` | A giant skull, 1.4 blocks wide and 2 tall: a band of pale-green mosaic edged in gold, a light in each eye. 3 steps: 0 = teeth clenched, 2 = jaw dropped wide | its middle `(8, 8, 8)` | `.frames(3).smooth()` |
| `pale_emperor/rib_cage` | Six pairs of ribs on a spine that lies along the ground, in 5 steps: 0 = spread open, 4 = closed into a cage. Inside it is 1.3 blocks wide and 1.7 tall, 2.9 long | the middle of the spine `(8, -16, 8)` | `.frames(5).smooth()`. The spine runs front to back. `.scale(2f)` closes round a player |
| `pale_emperor/bone_spikes` | A cluster of bone spikes breaking out of the ground, 2.75 blocks tall | the base of the tallest `(8, -16, 8)` | Start it under the ground with `pose().up(...)` and bring it up fast |
| `pale_emperor/bone_spear` | A spear of bone, 3 blocks long, tip up: a notched blade with barbs, a gold socket, two feathers | middle of the shaft `(8, 8, 8)`, tip `(8, 32, 8)` | For `modelAlong` |
| `pale_emperor/coffin` | A black coffin standing upright, 2.75 blocks tall, gold pictures on its lid. 4 steps: 0 = shut, 3 = the lid swung wide open on its left edge. A skeleton lies inside, a coin on each eye | the middle of its foot `(8, -16, 8)` | `.frames(4).smooth()`. Lid = front |
| `pale_emperor/tombstone` | A weathered headstone, 1.75 blocks tall, with a glowing hourglass mark and a candle; a grave mound in front of it with three bone fingers coming out | under the stone `(8, -16, 8)` | The grave reaches 1.2 blocks toward the front: a good place to play a `skeletal_hand` |
| `pale_emperor/underworld_chain` | A chain of dark iron, 2.9 blocks long, a four-clawed grab at its upper end | the far end `(8, -14, 8)`, or the tips of the claws `(8, 32, 8)` | For `modelAlong`. `.scale(1, length, 1)` stretches it |
| `pale_emperor/pale_crown` | The crown of the Pale Emperor: a gold band with pale-green stones, a serpent rearing at its front, a fan of pale feathers behind | default `(8, 0, 8)` = the middle of the band, at its lower edge | The band is 10 px wide: at `.scale(1)` it fits a player's head |
| `pale_emperor/pale_feather` | One pale feather, 1.5 blocks long, tip up | its middle `(8, 8, 8)` | `.scale(0.4f)` for a feather that drifts down; full size and `modelAlong` for one that is thrown |
| `pale_emperor/wraith` | A hooded spirit of the dead, 2.7 blocks tall: a skull deep in the hood with lights for eyes, a robe worn to rags, clawed arms of bare bone. 3 steps: 0 = arms hanging, 2 = reaching out | under it `(8, -16, 8)` | `.frames(3).smooth()`. Face = front |
| `pale_emperor/death_sigil` | A flat seal for the ground, 3 blocks wide: a ring of runes, a feathered serpent biting its tail, eight feathers and an hourglass | default (its middle) | `.glow().unshaded().seeThrough()` |
| `pale_emperor/death_sigil_ring`, `death_sigil_core` | The outer and the inner half of that seal | default | Play both and give them opposite `.spin(...)` |
| `conqueror/bear_trap` | A steel bear trap, 1 block wide, teeth red with old blood. 3 steps: 0 = open flat, 2 = snapped shut | default (the middle of its base) | `.frames(3).smooth()`. Step 2 turns the jaws 90 degrees (Blockbench may show 45) |
| `conqueror/powder_keg` | A powder keg with a burning fuse, 0.6 blocks wide, a red flame on its front | default (its bottom) | |
| `conqueror/flame_spear` | A spear of fire, 2.6 blocks long, tip up; the texture flickers | `(8, -6, 8)` near the back, tip `(8, 32, 8)` | `.glow().unshaded().seeThrough()`. For `Shot`, `modelAlong` and riding it (`Transit`) |
| `conqueror/flame_figure` | A figure of flame in a person's shape, 2 blocks tall, white-hot eyes; flickers | default (its feet) | `.glow().unshaded().seeThrough()`. Face = front |
| `conqueror/war_banner` | A tattered blood-red war banner, crossed swords under a crown, 3 blocks tall. 3 steps: blown back, still, blown forward | the foot of the pole `(8, -16, 8)` | `.frames(3).smooth()`, swing `frame` 0 <-> 2 to wave it. Cloth = front |
| `conqueror/reaper_scythe` | The Reaper's scythe, 3 blocks tall: black blade curving down to the right, blood-red edge, skull and red gem on its head | the lower grip `(4, -12, 8)` (the shaft stands at x 4) | `.glow()`; the edge is what shines |
| `conqueror/iron_mantle` | A mantle of dark iron with red-hot veins over a player's body: pauldrons, breastplate (a red sword on it), faulds, vambraces | default (between the feet) | Play it with `modelOnTurning`; `.scale(height / 1.8)` for other bodies |
| `conqueror/war_sigil` | The War Bishop's seal: a flat circle of gold and blood-red runes, eight swords pointing in, a mitre | default (its middle) | Lies flat like `rune_circle`. `.glow().unshaded().seeThrough()` |
| `conqueror/fire_raven` | A raven of fire, 1.5 blocks from wing tip to wing tip: charred body, burning wings that brighten toward the tips, white-hot beak, eyes and talons. 3 steps: 0 = wings up, 1 = level, 2 = down | the middle of its body `(8, 2.5, 9)` | `.frames(3).smooth().glow().unshaded()`. Beak = front. Made for `combat/Seeker` with `.flap(...)` |
| `hanged_man/pendulum` | A spirit pendulum: a crimson crystal on a silver chain, 1.4 blocks long | the top of the chain `(8, 24, 8)` | Swing it with `pose().roll(...)` |
| `hanged_man/whisper_mouths` | Four mouths in a ring 0.9 blocks across, facing out, to circle a head. 2 steps: 0 = shut, 1 = open | the middle of the ring `(8, 8, 8)` | `.frames(2).smooth()`; swing `frame` 0 <-> 1 to make them whisper |
| `hanged_man/shadow_tendrils` | Four shadow tendrils rising from a pool, 1.4 blocks tall, round a 1-block middle. 5 steps: 0 = leaning out, 4 = curled shut over the middle | the middle of the pool `(8, -16, 8)` | `.frames(5).smooth()`. Start it under the ground with `pose().up(...)` |
| `hanged_man/flesh_lump` | A veined lump of flesh with two bone shards, 0.6 blocks wide. 2 steps: 0 = resting, 1 = swollen | its middle `(8, 8, 8)` | `.frames(2).smooth()` |
| `hanged_man/flesh_cloak` | See-through veined shells round a standing player, 2 blocks tall | default (between the feet) | `.seeThrough()`. Worn with `modelOnTurning` |
| `hanged_man/black_armor` | Black plates with crimson trim round a player's body and limbs. 2 steps: 0 = flung apart, 1 = locked on | default (between the feet) | `.frames(2).smooth()`. Worn with `modelOnTurning` |
| `hanged_man/black_armor_helm` | A black knight's helm with a glowing visor slit and a crest | the top of the head inside it `(8, 0, 8)` | `modelOnHead(..., 0, ...)` puts it on |
| `hanged_man/shadow_head` | A head of shadow, 0.45 blocks wide, slit red eyes and a jagged grin, wisps off its back | the bottom of its neck `(8, 0, 8)` | Face = front |
| `beauty_goddess/flask` + `flask_liquid` | A round-bottomed potion flask with a cork and a crescent label, 0.75 blocks tall, and the liquid inside it (white: `.color(...)` is the potion) | `(8, 6, 8)` for both, the middle of its body | Play both with the same keys, `.glow().seeThrough()` |
| `beauty_goddess/bat` | A black bat with red eyes, 1.2 blocks wingspan, 3 steps: 0 = wings up, 1 = level, 2 = down | its body `(8, 4, 8.6)` | `.frames(3).smooth()`. Face = front. For `Seeker` with `.flap(...)` |
| `beauty_goddess/bat_wing` | A bat wing of darkness: black leather between five finger bones, 2.1 blocks long, 5 steps: 0 = folded, 4 = spread | the shoulder `(-10, 15, 8)` | `.frames(5).smooth().glow().seeThrough()`. Grows toward +X (the right); `.mirrored()` last for the left |
| `beauty_goddess/abyss_shackle` | A cuff of black iron with violet crescents, a ring 1.1 blocks across round a 1-block middle | its middle `(8, 8, 8)` | Scale it to the bound one's width |
| `beauty_goddess/abyss_chain` | A chain of darkness, 3 blocks long, standing up | the lower end `(8, -16, 8)` | For `modelAlong`; `.scale(1, length / 3, 1)`. `.glow().seeThrough()` |
| `beauty_goddess/abyss_pool` | A ragged pool of darkness with a violet glowing rim, 1 block wide, flat | default (its middle) | `.glow().unshaded().seeThrough()` |
| `beauty_goddess/full_moon`, `blood_moon` | A cratered moon in a faint halo, 1.5 blocks across; the blood moon is the same in red | its middle `(8, 8, 8)` | `.scale(6f)` and more for the sky. `.glow().unshaded().seeThrough()` |
| `beauty_goddess/moon_beam` | A column of moonlight, 1 block wide and 3 tall, fading toward its top. White: tint it | its foot `(8, -16, 8)` | `.scale(width, height / 3, width)`. `.glow().unshaded().seeThrough()` |
| `beauty_goddess/moon_sigil_ring`, `moon_sigil_core` | A flat moon seal in two halves: the twelve phases of the moon among runes, and a six-pointed star round a crescent and a bat. 1 block wide, white | default (its middle) | Tint both, give them opposite `.spin(...)`. `BeautyGoddessFx.seal(...)` does it |
| `beauty_goddess/moon_halo` | A standing ring of light with rays and four crescents, 1 block wide, white | its middle `(8, 8, 8)` | Faces north and south. `.glow().unshaded().seeThrough()` |
| `beauty_goddess/moonlight_figure` | A person made of moonlight, 2 blocks tall, bright eyes, mist trailing | default (between the feet) | Worn with `modelOnTurning`, `.scale(height / 2)`. `.glow().unshaded().seeThrough()` |
| `beauty_goddess/gaze_eye` | A crimson-irised eyeball, 0.7 blocks, its nerve trailing behind. 3 steps: 0 = lids shut, 2 = wide open | its middle `(8, 8, 8)` | `.frames(3).smooth()`. Iris = front |
| `beauty_goddess/darkness_prison` | A shell of black glass round a 1 x 2 block body, violet cracks and spikes. 2 steps: 0 = flung apart, 1 = closed | default (the middle of its base) | `.frames(2).smooth().seeThrough()`; `.scale(width, height / 2, width)` |
| `beauty_goddess/spirit_door` | The Door of Summoning's frame: dark stone carved with silver-violet runes, a pointed arch with a crescent and an eye. 3 blocks wide and tall | its threshold `(8, -16, 8)` | Both faces are the same |
| `beauty_goddess/spirit_door_left`, `_right` | Its two doors of black wood framed in silver | their hinges `(-10, -16, 8)` and `(26, -16, 8)` | Move them with `pose().right(-/+ 1.125 * scale)` and swing `yaw` |
| `beauty_goddess/spirit_door_void` | The Spirit World inside the doorway, swirling violet with stars, animated | `(8, -16, 8)` | `.glow().unshaded().seeThrough()`; scale it from 0 to open it |
| `beauty_goddess/spirit_beast` | A hound of the Spirit World, see-through blue, bone horns and ribs, four white eyes. 2 blocks long, 3 steps of a gallop | default (under its middle) | `.frames(3).smooth()`, swing `frame` 0 <-> 2. Face = front |
| `beauty_goddess/seed_of_life` | A golden seed that splits, sprouts and blooms into a glowing flower 1.6 blocks tall. 5 steps: 0 = closed seed, 4 = full bloom | default (where it stands) | `.frames(5).smooth()`. In step 0 the sprout is under the ground |
| `beauty_goddess/crescent_crown` | A silver circlet with a crescent and a crimson moonstone | default `(8, 0, 8)`, the band's lower edge | The band is 10 px wide: `modelOnHead(..., 0, ...)` |
| `beauty_goddess/goddess` | The goddess of the moon: a woman of light, 3 blocks tall, a flaring dress, silver hair, a crescent over her head | the middle of her hem `(8, -16, 8)` | `.glow().unshaded().seeThrough()`. Face = front |
| `beauty_goddess/tame_mark` | The blood pact's mark: a drop between two fangs in a ring, 1 block wide, standing up, white | its middle `(8, 8, 8)` | Tint it crimson; over a head with `.spin(...)` |

The fire and light ones (`orb`, `thread` and `projection` too) want `.glow().unshaded().seeThrough()`; the solid ones `.glow()`.

Flat plates (`rune_circle`, `shock_ring`) lie on the ground as they are. Play them a little above it so they don't flicker inside the block: `ground.add(0, 0.05, 0)`.

### Pale Emperor: ready-made displays

`PaleEmperorFx` (next to `PaleEmperor`) shows every Pale Emperor model the way it looks best: size, pivot, timing and animation are already chosen. Each display is one call.

**See them in game** (needs cheats, like `/sequence set`). Stand on flat ground:

| Command | What you get |
|---|---|
| `/palefx all` | Every display at once in a ring 16 blocks around you, played three times. `/palefx all 5` plays them five times |
| `/palefx serpent` | Just that one, 7 blocks in front of you and turned toward you. Press TAB after `/palefx` for the names |

**Use them in an ability.** `ground` is a spot on the floor, `yaw` the way the front looks (`player.getYRot()` = away from the player):

```java
PaleEmperorFx.ribCage(sl, enemy.position(), player.getYRot());
Later.run(sl, PaleEmperorFx.RIB_CAGE_SHUT, () -> cageShut(player, sl, enemy));    // the tick the cage is shut
```

| Call | What happens | Ticks to hang damage on |
|---|---|---|
| `serpent(sl, ground, yaw)` | A seal opens, the coils rise, the Feathered Serpent rears up 6 blocks, roars, strikes once and sinks | `SERPENT_ROARS`, `SERPENT_BITES` (4 blocks in front of it) |
| `skull(sl, ground, yaw)` | A skull 4 blocks tall hangs in the air, throws its head back with its jaw wide, chatters its teeth and fades | `SKULL_SCREAMS` |
| `ribCage(sl, ground, yaw)` | Ribs break out of the ground, snap shut, hold, open and sink | `RIB_CAGE_SHUT`, `RIB_CAGE_OPENS` |
| `boneSpikes(sl, ground, yaw)` | Spikes burst out, stand and sink back | `SPIKES_OUT` |
| `boneSpear(sl, ground, yaw)` | A spear appears point up, turns once, tips forward and flies 26 blocks | `SPEAR_FLIES` |
| `boneSpearThrow(sl, from, direction)` | A spear thrown along a direction: 24 blocks in 16 ticks | |
| `coffin(sl, ground, yaw)` | A coffin rises, its lid swings open, a wraith comes out; the lid slams and it sinks | `COFFIN_OPEN` |
| `wraith(sl, ground, yaw)` | A wraith rises, drifts forward with its arms out and fades | `WRAITH_REACHES` |
| `tombstone(sl, ground, yaw)` | A headstone rises and a skeletal hand breaks out of its grave and grasps | `GRAVE_HAND_GRASPS` |
| `chain(sl, from, to)` | One chain shoots from a spot and its claws reach another; it is stretched to fit | `CHAIN_ARRIVES` |
| `chains(sl, ground, yaw)` | A seal opens and four chains shoot from its rim to its middle | `CHAIN_ARRIVES` |
| `seal(sl, ground, yaw, blocksWide, ticks)` | The seal, its two halves turning against each other | |
| `featherFall(sl, ground, radius, count)` | Feathers drift down onto a round patch, swaying and turning | |
| `emperorHand(sl, ground, yaw)` | The Emperor's hand rises 6 blocks, opens, closes and sinks | `EMPEROR_HAND_GRASPS` |
| `underworldArms(sl, ground, yaw, count)` | Arms of the dead rise in a ring, lean in and clutch at the middle | |
| `underworldArm(sl, ground, yaw, tag)` | One arm rises at a spot, its palm toward where `yaw` looks, clutches twice and sinks. Put it beside someone, looking at them | `ARMS_TICKS` |
| `ribsAround(sl, entity)` | Ribs break out of the ground round an entity, snap shut over it and fade into it: a ward closing. Sized to the entity, and they go with it | `RIBS_AROUND_SHUT` |
| `tombstoneFor(ticks)` | Not played but handed back: a headstone (no hand) that rises, stands for as long as you want and sinks in its last second. Add `.delay(...)` / `.tag(...)` and play it with `ParticleShapes.model` | |
| `wings(sl, entity)` | Wings on its back: they unfold, beat four times and fold away | |
| `wings(sl, entity, ticks, tag)` | The same for as long as you want (a flight, a transformation): they keep beating and fold in the last second. Calling it again with the same tag replaces them | `WINGS_SHORTEST` (fewer ticks than this shows none) |
| `foldWings(sl, entity, tag)` | Folds away, now, the wings that were put on with that tag | |
| `crown(sl, entity)` | The crown comes down turning and settles on its head. It follows the head wherever it looks | |
| `crown(sl, entity, ticks, tag)` | The same for as long as you want. Calling it again with the same tag replaces it | |
| `takeCrown(sl, entity, tag)` | Takes off, now, the crown that was put on with that tag | |

Each display also has a `..._TICKS` number: how long it lasts.

The effects themselves are public constants (`PaleEmperorFx.SKULL`, `RIB_CAGE`, `COFFIN`, `WING_RIGHT` ...). To change one, copy it into your Pathway class and edit it, or adjust a copy on the spot: `PaleEmperorFx.SKULL.scale(4f)`.

The wings and the crown are worn: they move with the entity they are on. The wings turn when its body turns (`modelOnTurning`); the crown follows its head (`modelOnHead`).

### Conqueror: ready-made displays

`ConquerorFx` (next to `Conqueror`) holds the Conqueror's looks: war, blood, iron and fire, each one call, for the abilities that exist and for those still to be built. The models come from `tools/model_gen/conqueror_models.py`: change a number there, run `python3 tools/model_gen/conqueror_models.py`, press F3+T.

**See them in game** (cheats on), on flat ground: `/conquerorfx all` (every display in a ring 16 blocks out, three times; `/conquerorfx all 5` five times) or `/conquerorfx cull` (just that one, 7 blocks ahead; TAB lists the names: trap_set, trap_snap, keg, spear, figure, banner, cull, fog, battle_cry, galvanize, ravens).

| Call | What happens | Ticks to hang things on |
|---|---|---|
| `trapSet(sl, ground, yaw)` | The bear trap drops on the ground, its jaws spring open, it fades into hiding | `TRAP_SET_TICKS` |
| `trapSnap(sl, ground, yaw, ticks)` | Open trap, the jaws slam shut, it jolts, holds `ticks`, fades; sparks and blood | `TRAP_SNAPS` |
| `powderKeg(sl, ground, yaw, fuse)` | A keg drops with a bounce and shakes harder and harder for `fuse` ticks, swelling at the end. `kegFuse(ground)` = where the fuse burns | |
| `FLAME_SPEAR` | The flame spear as a `ModelFx` (what Flame Transformation's lance looks like) | |
| `flameFigure(sl, spot, yaw, ticks)` | A figure of flame rises out of fire, stands swaying, swells at the end | |
| `warBanner(sl, ground, yaw, ticks, tag)` | A banner thrust up out of the ground, waving, sinking at the end (`BANNER_SHORTEST`) | |
| `battleCry(sl, who, ticks)` | A banner rises behind someone, red rings roll out | |
| `cull(sl, prey, yaw)` / `cull(sl, feet, yaw, height)` | Blood circle and mist drawn in; a giant scythe rises behind the prey, drawn back, and cuts down diagonally through it: a crimson crescent, blood thrown along the cut and up in a fountain, red rays, a dark pool; the scythe swings on and crumbles into embers | `CULL_HITS` (16), `CULL_TICKS` |
| `galvanize(sl, wearer, ticks, tag)` | Molten iron spirals up the body, a mantle of iron closes over it (it turns with it), sparks drop off it, at the end it cracks into flakes. `galvanizeEnds(sl, wearer, tag)` ends it early, `ironClang(sl, at)` for a blow glancing off it | `MANTLE_FORMED`, `MANTLE_SHORTEST` |
| `fogOfWar(sl, center, radius, ticks, tag)` | The War Bishop's seal opens and turns; fog rolls out in rings to a dome `radius` wide, thickest at its edge, embers in it; six banners rise round the edge; it thins away at the end. `fogOfWarEnds(sl, center, tag)` | `FOG_ROLLED_OUT`, `FOG_SHORTEST` |
| `ravensErupt(sl, who, from)` | Rings of fire round the caster, embers where the ravens leave. `FIRE_RAVEN` is the raven model | |

They only show things: none of them hurts, roots or moves anyone. Put the damage at the tick they name with `Later.run`.

### Hanged Man: ready-made displays

`HangedManFx` (next to the other pathway classes) holds the Hanged Man's looks for all seven keys, both casts, and the Grazing soul theft (key 5): crimson, shadow, flesh and stolen souls. They only show things; the damage, roots and timing are the ability's, at the ticks the constants name (`BIND_GRIPS`, `STEAL_ARRIVES`, `DEGENERATION_ARRIVES`, `ARMOR_LOCKS`...). The class comment lists every call by key. The models come from `tools/model_gen/hanged_man/models.py` (`python3 tools/model_gen/hanged_man/models.py src/main/resources/assets/circle_of_inevitability`, then F3+T); `render.py` there draws any model to a PNG without the game, and `icons.py` makes the 16x16 pictures in `textures/gui/hanged_man/`.

**See them in game** (cheats on): `/hangedfx all` (every display one after another; those on a spot in a ring 8 blocks out) or `/hangedfx binding` (TAB lists the names: divination, ritual, whispers, listen, binding, lurking, flesh_bomb, flesh_cloak, grazing, cull, black_armor, degeneration, trinity). Displays that need a target use the nearest mob within 12 blocks, or you.

Statuses: `HangedManFx.status(player, Status.FLESH_CLOAK, ticks)` shows the picture in the row above the hotbar stacks and a `HudTimer`; `statusEnd(...)` takes both away.

Grazing souls are free-standing models, and clearing by tag clears that tag everywhere, so every soul has its own tag: `graveSoul(...)` returns it, keep it with the stolen ability and hand it to `soulStolen(...)` or `graveSoulFades(...)`.


### Beauty Goddess: ready-made displays

`BeautyGoddessFx` (next to the other pathway classes) holds the Beauty Goddess (Moon pathway) looks for all seven keys, both casts, the four potions of key 4 and the four entries of the Moon wheel on key 7: moonlight, blood, bats, darkness, potions and the Spirit World. They only show things; the damage, healing, taming and summoning are the ability's, at the ticks the constants name (`TAME_LANDS`, `SHACKLES_LOCK`, `DRINK_DONE`, `MOON_RISEN`, `GAZE_GRIPS`, `DOOR_OPEN`, `SEED_BLOOMS`...). The class comment lists every call by key; [BEAUTY_GODDESS.md](BEAUTY_GODDESS.md) says which call goes with which ability.

Everything it uses is generated, from `tools/model_gen/beauty_goddess/` (run each from the repository root with `src/main/resources/assets/circle_of_inevitability` as the argument, then F3+T):

| Script | Makes |
|---|---|
| `models.py` | The models and their textures in `models/model_fx/beauty_goddess/` and `textures/model_fx/beauty_goddess/` (`../hanged_man/render.py` draws any of them without the game) |
| `icons.py` | The wheel pictures (`textures/gui/ability_menus/beauty_goddess/`), the status pictures (`textures/gui/beauty_goddess/`) and the Pathway icon |
| `particles.py` | The particle pictures `fx/bat_0`, `bat_1`, `petal`, `crescent`, `bubble`, `droplet`, `sparkle`, `leaf` in `textures/particle/fx/` (white: any Pathway can tint them) |
| `sounds.py` | The sounds in `sounds/beauty_goddess/`, their events in `sounds.json` and their subtitles; the events are registered in `sounds/ModSounds` |

**See them in game** (cheats on): `/beautyfx all` (every display one after another; those on a spot in a ring 8 blocks out) or `/beautyfx moon` (TAB lists the names: medicine, spirit_vision, tame, animal_sense, wings, shackles, invisibility, fire_breath, sun_water, shadow, moon, moonlight_form, gaze, bat_swarm, door, creation, beauty, blood_moon). Displays that need a target use the nearest mob within 12 blocks, or you.

Statuses: `BeautyGoddessFx.status(player, Status.MOONLIGHT_FORM, ticks)` shows the picture above the hotbar and a `HudTimer`; `statusEnd(...)` takes both away.

The flock of bats from Wings of Darkness is a ready `Seeker`: `BeautyGoddessFx.BAT_SEEKER.onHit((target, at) -> Spells.strike(player, target, 6f)).launchAt(player, sl, from, enemies, 8, 1)`.

### Hand Of God: ready-made displays

`HandOfGodFx` (next to the other pathway classes) holds the Hand Of God (Twilight Giant pathway) looks for all seven keys, both casts, the three wheels (Dawn on key 4, Demon Hunting on key 6, Silver and Twilight on key 7) and what grows at higher Sequences (Silver Armor, the Cage, the Glory hurricane, the bigger Giant). They only show things; the damage, holding, hiding and moving are the ability's, at the ticks the constants name (`LIGHT_RISEN`, `HURRICANE_GATHERS`, `PROTECTION_RAISED`, `EYE_OPENS`, `VIAL_POURED`, `GIANT_RISEN`, `RAPIER_STRIKES`, `TWILIGHT_SWORD_HITS`, `HAND_GRIPS`, `TIME_FROZEN`, `KINGDOM_RISEN`...). The class comment lists every call by key; [HAND_OF_GOD.md](HAND_OF_GOD.md) says which call goes with which ability.

Everything it uses is generated, from `tools/model_gen/hand_of_god/` (run each from the repository root with `src/main/resources/assets/circle_of_inevitability` as the argument, then F3+T):

| Script | Makes |
|---|---|
| `models.py` | The models and their textures in `models/model_fx/hand_of_god/` and `textures/model_fx/hand_of_god/` (`../hanged_man/render.py` draws any of them without the game) |
| `icons.py` | The wheel pictures (`textures/gui/ability_menus/hand_of_god/`), the status pictures (`textures/gui/hand_of_god/`) and the Pathway icon |
| `particles.py` | The particle pictures `fx/ray`, `rune_0`, `rune_1`, `rune_2`, `ash`, `hourglass`, `footprint`, `feather_light` in `textures/particle/fx/` (white: any Pathway can tint them) |
| `sounds.py` | The sounds in `sounds/hand_of_god/`, their events in `sounds.json` and their subtitles; the events are registered in `sounds/ModSounds` (`HAND_...`). It borrows the building blocks of `../beauty_goddess/sounds.py` |

**See them in game** (cheats on): `/handfx all` or `/handfx hurricane` (TAB lists the names). Displays that need a target use the nearest mob within 12 blocks, or you.

Two parts are worth using for other Pathways too:
- **A shell of light**: `HandOfGodFx.dome(sl, center, radius, color, ticks, tag)` and `sphere(...)` lay `DAWN_WALL` tiles into a dome or a whole sphere, row by row, any color (one model per tile, so about 45 for a dome 6 blocks across). A barrier, a ward, a cage.
- **A seal**: `HandOfGodFx.seal(sl, ground, radius, color, ticks, tag)`, the two halves of the dawn seal turning against each other, white so any color works.

Statuses: `HandOfGodFx.status(player, Status.DAWN_ARMOR, ticks)` shows the picture above the hotbar and a `HudTimer`; `statusEnd(...)` takes both away.

### Knight Of Misfortune: ready-made displays

`KnightOfMisfortuneFx` (next to the other pathway classes) holds the Knight Of Misfortune (Darkness pathway) looks for all seven keys, both casts, the four wheels (Midnight Poem on key 2, Spirit Commanding on key 5, Nightwatcher on key 6, Darkness and Concealment on key 7) and what grows at higher Sequences (the complete Demonic Wolf, Misfortune and Catastrophe, the Sword's three edicts). They only show things; the sleep, fear, damage, hiding and moving are the ability's, at the ticks the constants name (`POEM_LANDS`, `LIMBS_GROWN`, `SOUL_OUT`, `DREAM_OPENS`, `REQUIEM_RISEN`, `EARTH_SUNK`, `DOMAIN_RISEN`, `HAIR_BINDS`, `CURSE_LANDS`, `WOLF_RISEN`, `SWORD_FORMED`, `WORLD_CLOSED`, `CONCEALED`, `MANTLE_RISEN`, `REST_FALLS`, `DESTINATION_OPENS`). The class comment lists every call by key; [KNIGHT_OF_MISFORTUNE.md](KNIGHT_OF_MISFORTUNE.md) says which call goes with which ability.

Everything it uses is generated, from `tools/model_gen/knight_of_misfortune/` (run each from the repository root with `src/main/resources/assets/circle_of_inevitability` as the argument, then F3+T):

| Script | Makes |
|---|---|
| `models.py` | The models and their textures in `models/model_fx/knight_of_misfortune/` and `textures/model_fx/knight_of_misfortune/` (`../hanged_man/render.py` draws any of them without the game) |
| `icons.py` | The wheel pictures (`textures/gui/ability_menus/knight_of_misfortune/`), the status pictures (`textures/gui/knight_of_misfortune/`) and the Pathway icon |
| `particles.py` | The particle pictures `fx/note`, `sleep_z`, `wisp`, `hair`, `tooth`, `horseshoe`, `skull`, `eye` in `textures/particle/fx/` (white: any Pathway can tint them) |
| `sounds.py` | The sounds in `sounds/knight_of_misfortune/`, their events in `sounds.json` and their subtitles; the events are registered in `sounds/ModSounds` (`KNIGHT_...`). It borrows the building blocks of `../beauty_goddess/sounds.py` |

**See them in game** (cheats on): `/knightfx all` or `/knightfx night_domain` (TAB lists the names). Displays that need a target use the nearest mob within 12 blocks, or you.

Three parts are worth using for other Pathways too:
- **A shell of fog**: `KnightOfMisfortuneFx.veilShell(sl, center, radius, color, ticks, tag)` lays `VEIL_WALL` tiles into a whole sphere, as `HandOfGodFx.sphere` does with light. The tile is dark and drawn in its own colors, so it is played without `.glow()`; tint it to change the fog. A wall of darkness, a sealed room, a dream.
- **A dark seal**: `KnightOfMisfortuneFx.seal(sl, ground, radius, color, ticks, tag)`, a crescent and star in a ring of script, white so any color works.
- **The moving tide**: `DARK_TIDE` tiles side by side with `.during(0, ticks, pose().forward(length))` (`darkTide`) is a wave that rolls forward; any liquid wall can be made the same way.

Statuses: `KnightOfMisfortuneFx.status(player, Status.MANTLE, ticks)` shows the picture above the hotbar and a `HudTimer`; `statusEnd(...)` takes both away.

## Tricks worth copying

All of these are in `PrinceOfAbolition.java`, with the helper named in brackets.

The helpers mentioned below no longer need copying: `warningCircle`, `shockRing` and `fireBurst` are in `combat/SpellFx`, and `clearStart`, `groundAt` and `aimGround` are in `combat/Spells`, ready to call from any Pathway. A model that flies and hits things is one line with `combat/Shot`. See [SPELL_KIT.md](SPELL_KIT.md).

**Flying along a direction** (`spearThrow`, `launchFireball`). `modelAlong` turns the model so its up points the way you give it. From then on `up(...)` in the keys moves it along that line:

```java
ModelFx spear = SPEAR.lifetime(12)
        .key(4, ModelFx.pose().up(-1f), ModelFx.Ease.OUT_BACK)     // drawn back a block
        .key(12, ModelFx.pose().up(20f));                          // flies 20 blocks
ParticleShapes.modelAlong(sl, spear, from, player.getLookAngle());
```

**Dropping out of the sky, head first** (`hammerFall`, `spearFromSky`). Play the model upside down (pitch 180) with the pivot on the part that should land. Its up now points down, so `up(-22)` is 22 blocks above the spot and `up(0)` is the landing:

```java
ModelFx hammer = HAMMER.pivot(8, 30, 8)                            // the flat top of the head
        .key(0, ModelFx.pose().up(-22f))
        .key(7, ModelFx.pose().up(0f), ModelFx.Ease.IN);
ParticleShapes.model(sl, hammer, ground, yaw, 180f, 0f);           // pitch 180 = upside down
Later.run(sl, 7, () -> hammerImpact(player, sl, ground));          // your own method: damage, sounds, particles
```

**A circle that warns where it will land** (`warningCircle`), **a ring racing outward** (`shockRing`), **an explosion** (`fireBurst`) and **something heavy hitting the ground** (`groundSlam`) are small helpers in that file, under "Looks shared by the abilities". The first three are also in `combat/SpellFx`, ready to call; `groundSlam` is only in that file, so copy it if you need it.

**One hit per enemy from many models** (`iceSpikes`). Ten spikes in a row would hurt a mob three times. They share one `Set<LivingEntity>` and skip whoever is already in it.

**Starting beside the player** (`clearStart`). A model that appears next to you (a spear at your shoulder) starts inside the wall when you stand against one, and a ray that starts inside a block stops at once. `clearStart` moves the start back to just in front of the wall.

**Finding the ground under a spot** (`groundAt`, `aimGround`). They also work when the player aims at a ceiling or at the side of a hill.

---

## Recipe: a new effect from start to finish

Say you want a spear that falls from the sky onto a target.

1. Blockbench: model a spear standing upright, tip at the bottom. Pivot at the tip, e.g. `(8, 0, 8)`.
2. Save `models/model_fx/sky_spear.json` and `textures/model_fx/sky_spear.png`.
3. Test with `/particle circle_of_inevitability:model{model:"circle_of_inevitability:model_fx/sky_spear",lifetime:100} ~ ~ ~ 0 0 0 0 1`.
4. In your Pathway class:

```java
private static final ModelFx SKY_SPEAR = ModelFx.of("sky_spear")
        .scale(2f).glow().aura(0xFFFF4040, 0.1f, 3)
        .lifetime(40).fade(0, 10)
        .key(0,  ModelFx.pose().up(12f).alpha(0f))             // starts 12 blocks up, invisible
        .key(4,  ModelFx.pose().alpha(1f))                     // fades in (still 12 up)
        .key(12, ModelFx.pose().up(0f), ModelFx.Ease.IN);      // hits the ground at tick 12

// in the ability:
ParticleShapes.model(sl, SKY_SPEAR, target.position(), player.getYRot(), 0f, 0f);
Later.run(sl, 12, () -> Spells.strike(player, target, 20f));
```

---

## Freezing (bonus)

The ice look from Hand Of Order is its own reusable status:

```java
Freeze.apply(target, 80);     // frozen 4 seconds: no AI for mobs, rooted for players
Freeze.isFrozen(target);
Freeze.ticksLeft(target);
Freeze.thaw(target);          // break the ice early (with the shatter effect)
```

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Nothing shows at all | The model name is wrong (check the spelling and the folder), or the file could not be read (the game log then has `Could not read the model_fx model`) |
| The model shows, but faces are missing | Those faces name a texture that is not there. Check the `textures` part of the JSON and that the PNG is in `textures/model_fx/` |
| I changed the model or texture and the game shows the old one | Reload the resources with F3+T (in the dev client, build the project first) |
| Keyframes do nothing / it doesn't move | You called `.key(...)` but played the original. Save the result in a variable and play that |
| It never comes back from a key (stays small / invisible / rotated) | Keys stick. Name the value to return to in a later key (`.scale(1f).alpha(1f)`), or use `ModelFx.rest()` |
| Can't get it to lie flat | After `pitch(90)` use `spin(90)`, not `yaw` or `roll`. See "Making a model lie flat" in section 4 |
| Spin goes around the wrong axis | `spin` = its own up axis, `yaw` = the world's up, `pitch` = end over end. See the table in section 4 |
| Aura invisible | The color needs an alpha: `0xFFFF0000`, not `0xFF0000`. No model has an aura when `textures/model_fx/glow_white.png` is missing |
| Aura dim where water is behind it | Normal: water is drawn after the models and their auras |
| Points the wrong way | The front must be North (-Z) in Blockbench, or add `.rotation(180, 0, 0)` |
| Swings around the wrong spot | Set `.pivot(...)` to the right pixel coordinates |
| Things inside it disappear | Add `.seeThrough()` |
| Aura too bright / dim | Lower / raise the alpha of the aura color (`0x80...` is half strength) |
| Fire or light looks dull, grey on some sides, or flickers while it spins | Add `.unshaded()` |
| A glowing square around a flat plate or a flame | That is the aura. Remove it (`.noAura()`) |
| A dark model with shells turns into one bright blob | The aura piles up on layered models. Lower its alpha a lot (`0x40...`) |
| Inner part of a layered model is hidden | The outer cubes' texture is too solid there. Make those pixels see-through in the PNG |
| Faint parts vanish before the rest when it fades | Normal: the game drops pixels that are nearly see-through (below about 10% opacity) |
| A face is gone when I look at it from behind or from inside | Normal: a face is only drawn from its front |
| Not visible from far away | Models are sent within 512 blocks, but 2D particles and trails only within 32 |
| Ending one effect removes another effect's model from the same mob | Give each its own `.tag(...)` and use `clearModels(sl, mob, "tag")` |

The code lives in `src/main/java/net/lukario/circleofinevitability/particles/fx/`: `ModelFx` (the options and the animation), `ModelFxModels` (reads the model files), `ModelFxRenderer` (draws them) and `ModelFxParticle` (keeps one alive and in place). A new option goes into `ModelFx`, and into `ModelFxRenderer` when it changes what is drawn. `src/main/resources/assets/minecraft/atlases/particles.json` is what makes the game load the PNGs in `textures/model_fx/`.
