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

A model can use several textures (`"1"`, `"2"`, `"3"`...); each one just needs its PNG in `textures/model_fx/` (or a folder inside it). A face whose texture is not found there is not drawn. The PNG can have any size. Animated textures work like on blocks: stack the frames vertically in the PNG and put a `<name>.png.mcmeta` next to it, e.g. `{"animation":{"frametime":3}}`.

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

Example: a sword that tips forward, turns flat, then flies forward while it spins flat like a thrown disc (the first Hand Of Order class had one like this; `HandOfOrderFx.JUDGMENT_SWORD` would do as the model).

```java
ModelFx sword = HandOfOrderFx.JUDGMENT_SWORD
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

Example: a hammer swing on the `order_hammer` model (the first Hand Of Order class had it, with the ticks of the slam as constants). It starts small and invisible, so the next key says what size and alpha to go to.

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

### Prince Of Abolition: ready-made displays

`PrinceOfAbolitionFx` (in `beyonder_characteristics/prince_of_abolition/`) holds the Prince Of Abolition (Black Emperor pathway) looks for all seven keys, both casts, the three wheels (Bribe on key 3, the Fallen on key 6, Sovereign on key 7), the mausoleums, Rebirth, the Order counter and what grows at higher Sequences (the gate, the flipped shell, the Moon leap). They only show things; the damage, holding, sealing, hiding and reviving are the ability's, at the ticks the constants name (`VERDICT_LANDS`, `WORDS_LAND`, `BRUTE_LANDS`, `BRIBE_LANDS`, `SEAL_SLAMS`, `GATE_SLAMS`, `MAUSOLEUM_RISEN`, `STEP_OUT`, `BESTOW_LANDS`, `SALVO_STEP`, `RUIN_FIRES`, `FRENZY_FIRST`, `ABOLITION_CLOSED`, `CROWNED`, `DECREE_LANDS`). The class comment lists every call by key; [PRINCE_OF_ABOLITION.md](PRINCE_OF_ABOLITION.md) says which call goes with which ability.

Everything it uses is generated, from `tools/model_gen/prince_of_abolition/` (run each from the repository root with `src/main/resources/assets/circle_of_inevitability` as the argument, then F3+T):

| Script | Makes |
|---|---|
| `models.py` | The models and their textures in `models/model_fx/prince_of_abolition/` and `textures/model_fx/prince_of_abolition/` (`../hanged_man/render.py` draws any of them without the game). The five older models in that folder (`blade`, `entropy_disc`, `entropy_orb`, `hammer`, `spear`) are the Frenzied Mage's; the script never writes those names |
| `icons.py` | The wheel pictures (`textures/gui/ability_menus/prince_of_abolition/`), the status pictures and the Order sigil (`textures/gui/prince_of_abolition/`) and the Pathway icon |
| `particles.py` | The particle pictures `fx/coin`, `section`, `crown`, `cog`, `keyhole`, `stele`, `twist`, `gavel` in `textures/particle/fx/` (white: any Pathway can tint them) |
| `sounds.py` | The sounds in `sounds/prince_of_abolition/`, their events in `sounds.json` and their subtitles; the events are registered in `sounds/ModSounds` (`ABOLITION_...`). It borrows the building blocks of `../beauty_goddess/sounds.py` and `../knight_of_misfortune/sounds.py` |

**See them in game** (cheats on): `/abolitionfx all` or `/abolitionfx seal` (TAB lists the names). Displays that need a target use the nearest mob within 12 blocks, or you.

Three parts are worth using for other Pathways too:
- **A shell of tiles**: `PrinceOfAbolitionFx.shell(sl, center, radius, Shell.SEAL or Shell.ABOLITION, color, ticks, tag)` lays gold lattice or grey fog into a whole sphere, like `KnightOfMisfortuneFx.veilShell`.
- **A beam**: `PrinceOfAbolitionFx.beam(sl, from, to, width, ticks)`, two crossed planes stretched between two points; anything that fires a ray can use it.
- **A door that slams**: the `illusory_door` model has four frames from closed to wide open; key `frame` 0 -> 3 -> 0 to swing it.

Statuses: `KnightOfMisfortuneFx.status(player, Status.MANTLE, ticks)` shows the picture above the hotbar and a `HudTimer`; `statusEnd(...)` takes both away.

### Abomination: ready-made displays

`AbominationFx` (in `beyonder_characteristics/abomination/`) holds the Abomination (Chained pathway) looks for all seven keys, both casts, the four wheels (Zombie on key 4, Wraith on key 5, Chains on key 6, Curses on key 7) and the passives that show. The abilities are not written yet; the displays are ready for them. They only show things; the binding, damage, curses, hiding and moving are the ability's, at the ticks the constants name (`CHAIN_REACHES`, `RESTRAINT_LOCKED`, `MOON_SHOWN`, `WOLF_RISEN`, `HANDS_UP`, `CHILL_SPREAD`, `SENTENCE_LANDS`, `MIRROR_OPENS`, `GHOST_ARRIVES`, `GHOSTS_RISEN`, `OBJECTS_RISE`, `SHACKLES_LOCK`, `MAUSOLEUM_SEALED`, `COFFIN_SHUT`, `COFFIN_OPENED`, `GARDEN_GROWN`, `CURSE_LANDS`, `RITE_FALLS`, `FORM_RISEN`). The class comment lists every call by key; [ABOMINATION.md](ABOMINATION.md) says which call goes with which ability.

Everything it uses is generated, from `tools/model_gen/abomination/` (run each from the repository root with `src/main/resources/assets/circle_of_inevitability` as the argument, then F3+T):

| Script | Makes |
|---|---|
| `models.py` (+ `textures.py`) | The models and their textures in `models/model_fx/abomination/` and `textures/model_fx/abomination/` (`../hanged_man/render.py` draws any of them without the game) |
| `icons.py` | The wheel pictures (`textures/gui/ability_menus/abomination/`), the status pictures (`textures/gui/abomination/`) and the Pathway icon |
| `particles.py` | The particle pictures `fx/chain_link`, `thorn`, `bandage`, `curse_0`, `curse_1`, `claw`, `padlock`, `stitch` in `textures/particle/fx/` (white: any Pathway can tint them) |
| `sounds.py` | The sounds in `sounds/abomination/`, their events in `sounds.json` and their subtitles; the events are registered in `sounds/ModSounds` (`ABOMINATION_...`). It borrows the building blocks of `../beauty_goddess/sounds.py` and `../hand_of_god/sounds.py` |

**See them in game** (cheats on): `/abominationfx all` or `/abominationfx coffin` (TAB lists the names). Displays that need a target use the nearest mob within 12 blocks, or you.

| Model | What it is | Pivot | Notes |
|---|---|---|---|
| `abomination/blood_chain`, `iron_chain` | A chain of congealed blood with a barbed hook / of black iron with an open cuff, 3 blocks, standing up | the lower end `(8, -16, 8)` | For `modelAlong`; `.scale(1, length / 3, 1)` |
| `abomination/restraint` | Chains wound round a standing player, a padlock on the chest | default (the feet) | Worn with `modelOnTurning`, `.seeThrough()` |
| `abomination/shackle` | An eight-sided cuff with glowing purple runes and a broken chain, 1.2 blocks across | its middle `(8, 8, 8)` | `.color(...)` turns it to blood for Blood Chain |
| `abomination/werewolf` | A hunched wolf-man, 2 blocks tall | default (the feet) | Worn with `modelOnTurning`, `.scale(height / 2)` |
| `abomination/grave_hand` | A rotting hand clawing out of a grave, 3 steps (0 = straight, 2 = hooked) | the forearm's end `(8, -16, 8)` | `.frames(3).smooth()`; start it under the ground |
| `abomination/coffin` | A dark coffin bound in iron and crowned with thorns, a rose on its lid, 2.4 blocks tall, 4 steps (0 = lid wide open, 3 = shut) | the middle of its foot `(8, -16, 8)` | `.frames(4).smooth()`. Lid = front |
| `abomination/bandage_wrap` | A body mummified in yellow oily bandages, player-sized | default (the feet) | Worn with `modelOnTurning` |
| `abomination/doll_vessel` | The Mythical Creature Form's vessel: a rag doll bound by thorny vines, 2.75 blocks tall | between its feet `(8, -16, 8)` | Worn with `modelOnTurning`; `.scale(0.3f)` is Cursed Artifact's relic |
| `abomination/thorn_spikes`, `rose` | Black thorny vines bursting from the ground (1.9 blocks); one rose on a thorny stem (0.8 blocks) | the roots `(8, -16, 8)`; the rose's foot (default) | |
| `abomination/ghost` | A see-through ghost, 2 blocks, 2 steps (arms hanging, reaching) | the tail's end (default) | `.frames(2).smooth().glow().seeThrough()` |
| `abomination/seal_ring`, `seal_core` | The Pathway's seal: a ring of chain and thorns, and an eye crossed by two chains | default | Flat, white: tint them. `AbominationFx.seal(...)` plays both |
| `abomination/silence_sigil`, `frost_ring` | Sewn lips in a ring; a jagged ring of grave-frost | default | Flat, white |
| `abomination/sentence_mark`, `curse_mark`, `claw_marks` | A coffin with skull and hourglass; an eye in thorns; three gashes | their middle `(8, 8, 8)` | Standing, white |
| `abomination/mad_moon` | A pale moon with a staring eye and a crooked grin | its middle `(8, 8, 8)` | Standing, its own colors |
| `abomination/blood_pool` | A pool of blood | default | Flat, its own colors, no glow |
| `abomination/mirror_ripple` | An oval of reflection with ripples (animated) | the middle of its bottom edge | Standing, white; `.scale(1.2f, 2.2f, 1f)` for a doorway |
| `abomination/thorn_wall` | A tile of thorns over bandages | the middle of its bottom edge | Standing, its own colors. The Mausoleum's sphere is made of these |

Statuses: `AbominationFx.status(player, Status.RESTRAINT, ticks)` shows the picture above the hotbar and a `HudTimer`; `statusEnd(...)` takes both away.

### Knowledge Emperor: ready-made displays

`KnowledgeEmperorFx` (in `beyonder_characteristics/knowledge_emperor/`) holds the Knowledge Emperor (Hermit pathway) looks for all seven keys, both casts, the three wheels (Stars on key 5, Retellings and Prophecy on key 6, Knowledge on key 7), the Study eye, the scroll pouch, the passives that show, the melee combo and what grows at higher Sequences (Eyes of Secret Inquiry, the Knowledge Orbs, the Hermit's three lances and Overload on everyone). The abilities are not written yet; the displays are ready for them. They only show things; the damage, holding, hiding, healing and moving are the ability's, at the ticks the constants name (`STRIKE_LANDS`, `HEX_LANDS`, `BOLT_FLIGHT`, `BOLT_STEP`, `ORB_FLIGHT`, `READ_FIRES`, `RAY_FALLS`, `RAY_HITS`, `INSCRIBED`, `CAGE_LANDS`, `STELLAR_ARRIVES`, `STAR_LANDS`, `SPINDLE_SLEEPS`, `AVALON_RISEN`, `ARROW_LANDS`, `BOX_OPENS`, `HOPE_RISES`, `ERASE_LANDS`, `OVERLOAD_REACHES`, `LANCE_THROWN`, `CONDENSED`, `KINGDOM_RISEN`). The class comment lists every call by key; [KNOWLEDGE_EMPEROR.md](KNOWLEDGE_EMPEROR.md) says which call goes with which ability.

Everything it uses is generated, from `tools/model_gen/knowledge_emperor/` (run each from the repository root with `src/main/resources/assets/circle_of_inevitability` as the argument, then F3+T):

| Script | Makes |
|---|---|
| `models.py` (+ `textures.py`) | The models and their textures in `models/model_fx/knowledge_emperor/` and `textures/model_fx/knowledge_emperor/` (`../hanged_man/render.py` draws any of them without the game) |
| `icons.py` | The wheel pictures (`textures/gui/ability_menus/knowledge_emperor/`), the status pictures and the three scrolls of the pouch (`textures/gui/knowledge_emperor/`) and the Pathway icon |
| `particles.py` | The particle pictures `fx/glyph_0` to `glyph_3`, `star5`, `ink`, `page`, `quill`, `text_line` in `textures/particle/fx/` (white: any Pathway can tint them) |
| `sounds.py` | The sounds in `sounds/knowledge_emperor/`, their events in `sounds.json` and their subtitles; the events are registered in `sounds/ModSounds` (`KNOWLEDGE_...`). It borrows the building blocks of `../beauty_goddess/sounds.py` |

**See them in game** (cheats on): `/knowledgefx all` or `/knowledgefx star_fall` (TAB lists the names). Displays that need a target use the nearest mob within 12 blocks, or you.

| Model | What it is | Pivot | Notes |
|---|---|---|---|
| `knowledge_emperor/study_eye_0`, `_1`, `_2` | The Study eye: shut, half open, wide open; orchid lids, an indigo iris full of stars | its middle `(8, 8, 8)` | Standing, its own colors. `KnowledgeEmperorFx.studyEye(viewer, target, percent, ticks)` shows it to the Hermit only. `ModelFx.of("knowledge_emperor/study_eye").frames(3)` opens it |
| `knowledge_emperor/hermit_eyes` | The Pathway's symbol: a pair of cold eyes full of stars in a ring of glyphs | its middle | Standing; the rings and lids are white (tint them), the irises keep their stars |
| `knowledge_emperor/pendulum` | Dowsing's violet crystal on a silver chain, 1.2 blocks | the ring on top `(8, 16, 8)` | Swing it with `roll` and `pitch` keys |
| `knowledge_emperor/scroll` | A rolled scroll on a dark rod with gold caps and an orchid seal, 1 block, lying east-west | its middle | |
| `knowledge_emperor/star` | A small star, six long points and four short ones, 0.9 blocks | its heart `(8, 8, 8)` | `.glow().unshaded()`; Star Fall, Star Guard, Stellar Self |
| `knowledge_emperor/amber` | The Starlight Cage: a block of clear amber with crystal facets and frozen starlight | default (bottom middle) | `.seeThrough()`; `.scale(width, height, width)` round the one held |
| `knowledge_emperor/spindle` | Sleeping Beauty's golden spindle, wound with thread, 1.6 blocks | its middle `(8, 10, 8)` | |
| `knowledge_emperor/black_box`, `black_box_lid` | Pandora's ebony box with gold corners and an eye-lock; its lid | default; the lid's hinge `(8, 7, 13)` | Play both at one spot; a key of `pitch(-105)` throws the lid open. Lock = front |
| `knowledge_emperor/white_spear` | The Lance of Knowledge, white light with faint script, 2.8 blocks | its middle `(8, 8, 8)` | Top = the point: `modelAlong` or a `Shot` |
| `knowledge_emperor/light_ball` | A ball of illusory light, 0.75 blocks | its middle | White: tint it (gold for Condense, orchid for the Knowledge Orbs) |
| `knowledge_emperor/spell_bolt` | A bolt of a spell, a crystal of light pointed at both ends, 0.9 blocks | its middle | Top = its front; white: tint it per element |
| `knowledge_emperor/tale_book` | The book of tales lying open in a V, indigo and gold | the bottom of the spine | `modelOnTurning` before the teller |
| `knowledge_emperor/glyph_ring`, `constellation_circle`, `knowledge_moor`, `avalon_seal` | A ring of glyphs; six constellations in a zodiac ring; the Knowledge Moor's floor; Avalon's isle and apple tree | default | Flat, white: tint them |
| `knowledge_emperor/hex_mark`, `fate_wheel`, `erase_sigil`, `heel_mark`, `weak_point`, `scroll_sheet` | A hexagram round a downcast eye; a wheel of fate; symbols struck out; a heel struck by an arrow; cross hairs on a crack; an unrolled scroll | their middle `(8, 8, 8)` | Standing; white (tint them), except `scroll_sheet` which has its own colors |
| `knowledge_emperor/library_shelf`, `data_stream` | A tile of shelves of books of light; a panel of glyphs streaming down (both animated) | the middle of their bottom edge | Standing, white. The Divine Kingdom's walls; Informatization's body |
| `knowledge_emperor/light_column` | A column of light, two crossed planes, bands running down it (animated) | default | `.scale(width, height, width)`: the Scroll of Freezing, Prophecy of Ruin, Indoctrination |

Star Guard's stars circle the caster with `ParticleShapes.moveModel`: call `starGuardTick(sl, player, tag, age)` every tick while it lasts (a model's own keys cannot orbit a moving entity; the planned `combat/Orbit` can take this over).

Statuses: `KnowledgeEmperorFx.status(player, Status.FORESIGHT, ticks)` shows the picture above the hotbar and a `HudTimer`; `statusEnd(...)` takes both away. `pouch(player, scrolls)` shows the scrolls in the row beside the hotbar, where the Attendant's stacks are.

### Illuminator: ready-made displays

`IlluminatorFx` (in `beyonder_characteristics/illuminator/`) holds the Illuminator (Paragon pathway) looks for all seven keys, both casts, the four wheels (Workshop on key 4, Astronomy on key 5, Alchemy and Arcana on key 6, Civilization on key 7), the Appraised mark, the statuses, the passives that show, the melee combo and what grows at higher Sequences (the acid puddle, Recall on an ally, Exploit Flaw on every mark, the arm cannon, the steam form). Every key uses them (`Illuminator`, `IlluminatorWorkshop`, `IlluminatorAstronomy`, `IlluminatorAlchemy`, `IlluminatorCivilization`). They only show things; the damage, holding, hiding, healing and moving are the ability's, at the ticks the constants name (`SURVEY_PULSE`, `RITUAL_DRAWN`, `APPRAISE_LANDS`, `FLAW_LANDS`, `RIFLE_FIRES`, `RIFLE_STEP`, `CANNON_READY`, `CURSE_LANDS`, `BLESSING_RISEN`, `METEOR_LANDS`, `DOLL_STANDS`, `GOLEM_STANDS`, `GOLEM_STRIKE_LANDS`, `MECH_ON`, `PRINCIPLES_RISEN`, `COLOSSUS_READY`, `FUSION_FIRES`, `SCROLLS_FLY`, `MAZE_CLOSED`, `REALITY_LANDS`, `BOOK_OPEN`, `BOOK_RELEASES`, `HEAVEN_RISEN`). The class comment lists every call by key; [ILLUMINATOR.md](ILLUMINATOR.md) says which call goes with which ability.

Everything it uses is generated, from `tools/model_gen/illuminator/` (run each from the repository root with `src/main/resources/assets/circle_of_inevitability` as the argument, then F3+T):

| Script | Makes |
|---|---|
| `models.py` (+ `textures.py`, `pictures.py`) | The models and their textures in `models/model_fx/illuminator/` and `textures/model_fx/illuminator/` (`../hanged_man/render.py` draws the box models without the game) |
| `icons.py` | The wheel pictures (`textures/gui/ability_menus/illuminator/`), the status pictures (`textures/gui/illuminator/`) and the Pathway icon |
| `particles.py` | The particle pictures `fx/gear`, `steam`, `equation_0` to `equation_2`, `spring`, `screw`, `lens`, `compass` in `textures/particle/fx/` (white: any Pathway can tint them) |
| `sounds.py` | The sounds in `sounds/illuminator/`, their events in `sounds.json` and their subtitles; the events are registered in `sounds/ModSounds` (`ILLUMINATOR_...`). It borrows the building blocks of `../beauty_goddess/sounds.py` and `../knowledge_emperor/sounds.py` |

**See them in game** (cheats on): `/illuminatorfx all` or `/illuminatorfx golem` (TAB lists the names). Displays that need a target use the nearest mob within 12 blocks, or you.

| Model | What it is | Pivot | Notes |
|---|---|---|---|
| `illuminator/flask` | The Compound Flask: a round-bottomed flask of glowing yellow-green acid, cork, brass collar, 0.85 blocks | its belly `(8, 5, 8)` | `.glow().seeThrough()`; `FLASK_FLYING` tumbles it for a `Shot` |
| `illuminator/clockwork_rifle` | A brass-and-walnut rifle with a spring drum, a gear, a winding key, a copper scope, a gauge and a line of blueprint light; 2.3 blocks | the grip `(8, 6, 13)` | Muzzle north at `(8, 10, -13)`. `.scale(0.55f)` in the hands |
| `illuminator/rifle_round` | A brass bullet with a ring of blue light and a short streak | `(8, 8, 8)` | Top = its front: `Shot`, `modelAlong` |
| `illuminator/spirit_cannon`, `spirit_cannon_barrel` | The cannon's carriage on two gear-wheels with a little boiler; its barrel with blue runes and a glowing bore | default; the trunnions `(8, 10, 9)` | Play both at one spot; aim the barrel with `moveModel` (pitch: negative raises it) |
| `illuminator/cannonball` | An iron ball girdled with blue light, 0.4 blocks | `(8, 8, 8)` | |
| `illuminator/mystic_brooch` | Brass filigree with a toothed rim round a blue gem with a gear inside, 0.5 blocks, standing | `(8, 8, 8)` | Gem = north |
| `illuminator/doll_stone`, `doll_straw`, `doll_iron`, `doll_dust`, `doll_frost` | The five Living Dolls, about 1.45 blocks, jointed, each of its own material (the straw one with a hat and stitched face, the iron one with a bolt-thrower, the dust one hooded, the frost one crowned with ice) | between the feet | `modelOnTurning` on the doll's mob, `.scale(height / 1.45)`. Frost: `.seeThrough()` |
| `illuminator/golem` | A brass-and-iron golem 3 blocks tall in 3 steps: 0 standing, 1 arms drawn back, 2 striking | between the feet `(8, -16, 8)` | `.frames(3).smooth()` |
| `illuminator/mechanical_body`, `mechanical_goggles`, `arm_cannon` | Brass plates over a player with a jetpack of two copper tanks; goggles with a magnifier; the right arm's cannon | default; the goggles `(8, 0, 8)` = the top of the head | `modelOnTurning`; the goggles with `modelOnHead(..., 0, ...)` |
| `illuminator/colossus` | A walking machine 3 blocks tall at scale 1, in 3 steps: 0 standing, 1 a stride, 2 the Fusion Beam raised; the pilot stands in the cage between its legs | the cage floor `(8, -16, 8)` | `.frames(3).smooth().scale(2f)` (6 blocks) |
| `illuminator/gear` | A brass gear, 16 teeth, 1 block, lying flat | its hub `(8, 8, 8)` | `.spin(...)`; pitch 90 stands it up |
| `illuminator/scroll` | A rolled scroll with brass caps and a band of blue light, lying east-west | `(8, 8, 8)` | |
| `illuminator/baton` | A conductor's baton with a point of white-gold light, tip up | the grip `(8, 2, 8)` | |
| `illuminator/star_meteor` | A meteor of starlight with two crossed planes of tail | the core `(8, 8, 8)` | Top = its front; drop it head first (pitch 180) |
| `illuminator/illusory_book` | A great book in 3 steps: 0 shut on its spine, 1 falling open, 2 wide open | the bottom of the spine | `.frames(3).smooth()` |
| `illuminator/appraisal_starlight`, `_corrosion`, `_gravity`, `_pierce` | The Appraised lens with the weakness in it; their own colors | `(8, 8, 8)` | Standing |
| `illuminator/flaw_mark`, `curse_weakness`, `curse_trauma`, `curse_mutation`, `eroded_mark`, `rejection_seal`, `recall_clock`, `illuminator_emblem` | A measured crack; the three steps of the curse ladder; a dissolving page; a struck-out book; a clock turning back (animated); the lamp in its gear | `(8, 8, 8)` | Standing, white: tint them |
| `illuminator/blueprint_circle`, `chalk_circle`, `blueprint_spiral`, `star_chart`, `principle_circle`, `symphony_ring` | Flat circles: blueprint, chalk, a compass-drawn spiral, a star chart, struck-out laws with rising arrows, a ring of music | default | Flat, white: tint them |
| `illuminator/acid_pool`, `false_sky`, `gear_sky` | The acid's puddle, a disc of fake night sky, the sky of gears (all animated) | default | Flat, their own colors |
| `illuminator/equation_lattice` | A cube of lattice bars with equations drifting along them (animated), six faces | default | `.scale(3f)` is the maze's cube |
| `illuminator/mist_beam`, `fusion_beam` | Two crossed planes of misty light / a white-hot core with rings (animated) | default = the middle of their start | `modelAlong` and `.scale(width, length, width)` |
| `illuminator/mural_0` to `mural_3` | Murals of the ancients, the seafarers, a cathedral and the age of steam between brass rods; their own colors | default | Standing, `.scale(width, height, 1)` |

### Star Dragon: ready-made displays

`StarDragonFx` (in `beyonder_characteristics/star_dragon/`) holds the Star Dragon (Condenser pathway) looks for all seven keys, both casts, the three wheels (Star Sacrifice on key 2, Heavybringer on key 5, Cosmos on key 7), Heaviness and the rays they share, the marks on others, the statuses, the three forms, the passives that show and what grows at higher Sequences (the lightening and the slam, the true and the frozen void, the Heavy Body, three asteroids, the storm, the imploding clusters). The abilities are not written yet; the displays are ready for them. They only show things; the damage, holding, pulling and moving are the ability's, at the ticks the constants name (`RAY_WARNING`, `CHART_LANDS`, `GRAVITY_RISEN`, `VOID_CLOSED`, `SUNLIGHT_FIRES`, `PASSAGE_ARRIVES`, `SURGE_TICKS`, `SCALES_ON`, `SPHERE_CLOSED`, `SERIES_FIRES`, `SERIES_STEP`, `GRAZE_LANDS`, `FISSION_SPLITS`, `FUSION_READY`, `ANGEL_READY`, `DILATION_RISEN`, `PLANET_LANDS`, `DRAGON_READY`, `NEUTRON_READY`, `SINGULARITY_GATHER`). The class comment lists every call by key; [STAR_DRAGON.md](STAR_DRAGON.md) says which call goes with which ability.

Everything it uses is generated, from `tools/model_gen/star_dragon/` (run each from the repository root with `src/main/resources/assets/circle_of_inevitability` as the argument, then F3+T):

| Script | Makes |
|---|---|
| `models.py` (+ `textures.py`, `pictures.py`) | The models and their textures in `models/model_fx/star_dragon/` and `textures/model_fx/star_dragon/` (`../hanged_man/render.py` draws the box models without the game) |
| `icons.py` | The wheel pictures (`textures/gui/ability_menus/star_dragon/`), the status pictures (`textures/gui/star_dragon/`) and the Pathway icon |
| `particles.py` | The particle pictures `fx/star4`, `chevron`, `arc_0`, `arc_1`, `nucleus`, `scale`, `rock`, `flare` in `textures/particle/fx/` (white: any Pathway can tint them) |
| `sounds.py` | The sounds in `sounds/star_dragon/`, their events in `sounds.json` and their subtitles; the events are registered in `sounds/ModSounds` (`STAR_DRAGON_...`). It borrows the building blocks of `../beauty_goddess/sounds.py`, `../knowledge_emperor/sounds.py` and `../illuminator/sounds.py` |

**See them in game** (cheats on): `/stardragonfx all` or `/stardragonfx planetfall` (TAB lists the names). Displays that need a target use the nearest mob within 12 blocks, or you.

| Model | What it is | Pivot | Notes |
|---|---|---|---|
| `star_dragon/asteroid`, `asteroid_flame` | A pitted lump of stone with a glowing leading face, about 1 block; the fire wrapped round it, streaming behind | `(8, 8, 8)` | Top = its front: drop it head first with `modelAlong`; play both with the same keys and scale |
| `star_dragon/grazed_star` | A white core with six rays of light, 1.5 blocks across | `(8, 8, 8)` | `.spin(...)` slowly |
| `star_dragon/fusion_cluster` | White-hot plasma inside two rings of violet-blue magnetic light, 1 block | `(8, 8, 8)` | `FUSION_FLYING` for a `Shot` |
| `star_dragon/neutron_star` | A blinding core with two pulsar beams, 1.4 blocks each way | `(8, 8, 8)` | Tip it (roll) and spin it so the beams sweep |
| `star_dragon/void_sphere`, `void_frost_shell` | The half-real void (indigo-black with glimmers, a violet skin); the ice that wraps the highest one | `(8, 8, 8)` | `.scale(diameter)` |
| `star_dragon/heavy_sphere` | Dense air: a dark see-through ball with a bent grid in it | `(8, 8, 8)` | |
| `star_dragon/black_hole` | A black ball ringed with caught light | `(8, 8, 8)` | With `accretion_disk` round it, a little tipped |
| `star_dragon/tidal_wave` | A curling crest of silver-teal water with a foam lip, 1 block | the middle of its foot | `.scale(7, 5, 3)`, front = north |
| `star_dragon/scaled_form` | A humanoid lizard of starlit scales with gold slit eyes, a crest of fins and a tail, 2 blocks | between the feet | `modelOnTurning`; `.scale(2.5f)` for the Heavy Body |
| `star_dragon/radiant_angel` | A being of starlight 3 blocks tall with a halo, a sun-ring and six pairs of wings | between the feet `(8, -16, 8)` | `modelOnTurning` |
| `star_dragon/star_dragon` | A dragon of transparent starlight, 3 blocks at scale 1, in 3 steps: 0 wings raised, 1 level, 2 beating down | under the chest `(8, 3, 8)` | `.frames(3).smooth().scale(3.5f)` (10.5 blocks) |
| `star_dragon/star_eye`, `misfortune_mark`, `weakened_mark`, `star_dragon_emblem` | An eye of stars; a cracked falling star; a ring melting open; a dragon coiled round a star | `(8, 8, 8)` | Standing, white: tint them |
| `star_dragon/void_door` | A tall almond slit into the void (animated), its own colors | the middle of its foot | Standing, `.scale(1.4f, 2.4f, 1f)` |
| `star_dragon/astrolabe`, `fate_ring`, `fate_web`, `impact_circle`, `light_cracks` | Flat: an astrolabe, a ring of stars and threads, threads of Fate drawn to a point, the warning of a sky ability, cracks of light | default | Flat, white: tint them |
| `star_dragon/gravity_well`, `time_field`, `quantum_field`, `field_lines` | Flat and animated: rings sinking into a well, the rings of a slowing clock over bent space-time, interfering waves and jumping points, field lines between two poles | default | Flat, white: tint them |
| `star_dragon/radiation_ground`, `accretion_disk` | Flat and animated, their own colors: a radiation zone, the black hole's disc | default | |
| `star_dragon/ray_beam`, `pulse_beam` | Two crossed planes of light (animated): a ray, and a heavier one with rings | default = the middle of their start | `modelAlong` and `.scale(width, length, width)` |

### Hand Of Order: ready-made displays

`HandOfOrderFx` (in `beyonder_characteristics/hand_of_order/`) holds the Hand Of Order (Justiciar pathway) looks for all seven keys, both casts, the four wheels (Interrogation on key 3, Verdict on key 4, Decree on key 5, Order on key 7), the spoken words of the Verdicts and Decrees, the Convict, the Decree zones and their Violations, the Court of Judgment and its clauses, the statuses, the passives that show, the melee combo and what grows at higher Sequences (Intimidation, Confinement, the void Exile and its hurricane, Death on the latest wound, Prohibition layers, the Brass Pillar). All seven keys use them (`HandOfOrder`, `HandOfOrderInterrogation`, `HandOfOrderVerdicts`, `HandOfOrderDecrees`, `HandOfOrderOrder`). They only show things; the damage, holding, hiding, healing and moving are the ability's, at the ticks the constants name (`HALT_LANDS`, `JURISDICTION_CLAIMED`, `WHIP_LANDS`, `IMPRISON_LANDS`, `EXILE_LANDS`, `DEATH_LANDS`, `DEPRIVED`, `EXECUTION_LANDS`, `DECREE_RISEN`, `CONTRACT_SIGNED`, `SWORD_LANDS`, `BALANCE_SETTLED`, `DIVIDE_RISEN`, `COURT_RISEN`). The class comment lists every call by key; [HAND_OF_ORDER.md](HAND_OF_ORDER.md) says which call goes with which ability.

Everything it uses is generated, from `tools/model_gen/hand_of_order/` (run each from the repository root with `src/main/resources/assets/circle_of_inevitability` as the argument, then F3+T):

| Script | Makes |
|---|---|
| `models.py` (+ `textures.py`) | The models, pictures and spoken words and their textures in `models/model_fx/hand_of_order/` and `textures/model_fx/hand_of_order/` (`../hanged_man/render.py` draws any of them without the game). The words need a bold TrueType font (DejaVu Serif Bold, Times New Roman Bold on Windows) |
| `icons.py` | The wheel pictures (`textures/gui/ability_menus/hand_of_order/`), the status pictures, the seven clause pictures and the Court's tablet (`textures/gui/hand_of_order/`) and the Pathway icon |
| `particles.py` | The particle pictures `fx/law_0` to `law_2`, `prohibit`, `shackle`, `brand`, `bolt`, `verdict` in `textures/particle/fx/` (white: any Pathway can tint them) |
| `sounds.py` | The sounds in `sounds/hand_of_order/`, their events in `sounds.json` and their subtitles; the events are registered in `sounds/ModSounds` (`ORDER_...`). It borrows the building blocks of `../beauty_goddess/sounds.py` and `../knowledge_emperor/sounds.py` |

**See them in game** (cheats on): `/orderfx all` or `/orderfx court` (TAB lists the names). Displays that need a target use the nearest mob within 12 blocks, or you.

| Model | What it is | Pivot | Notes |
|---|---|---|---|
| `hand_of_order/brass_pillar` | The Brass Pillar: a fluted brass column on a stepped plinth, bands of engraved law, a crown of white-gold light, 2 blocks | default (bottom middle) | Behind the caster: the Imperative Mage's form (1.8), Order Proxy (3.2), the Court (4.5) |
| `hand_of_order/scales_stand`, `scales_beam` | Balance's scales: a brass post; a beam with a pan on three chains at each end | default; the beam's hinge `(8, 24, 8)` | Play the beam 1.5 blocks times its scale above the stand's foot; `roll` tips it |
| `hand_of_order/branding_iron` | An Illusory Torture Device: leather grip, iron rod, a cross-brand glowing white-hot, 1.9 blocks | the grip `(8, 4, 8)` | Tint it lightning blue; along the weapon in hand |
| `hand_of_order/spectral_fist` | A spectral right fist and forearm of white-gold light, knuckles north, 1.5 blocks | the wrist `(8, 8, 14)` | Death and Execution |
| `hand_of_order/glass_pane`, `confinement_cell` | One transparent wall in an iron frame, 1 x 2 blocks; a glass cell with iron posts and brass studs, 1 x 2 x 1 | default | `.seeThrough()`; scale them to the one held |
| `hand_of_order/gavel` | A gavel: dark wood bound in brass, 1.25 blocks, handle south | the handle's end `(8, 8, 19)` | Pitch it down to strike: clauses, sentences |
| `hand_of_order/judgment_sword` | The Sword of Judgment: a blade of white-gold light, a scale-armed brass guard with a red stone, 3 blocks | the grip `(8, -10, 8)` | Point up; pitch it forward to cut |
| `hand_of_order/order_seal`, `decree_circle`, `court_floor`, `reality_seal`, `no_return_sigil`, `quell_seal` | Jurisdiction's seal; a Decree's chained ring with seven clause seats; the Court's floor; Reality's grid; a shut, barred door; nested squares | default | Flat, white: tint them |
| `hand_of_order/prohibition_sigil`, `convict_brand`, `recognition_mark`, `execution_ring`, `exile_void`, `dawn_halo` | A ring struck through; a sword point down hung with chains; an eye framed by closing corners; chevrons closing on a point; a torn hole; a sunburst | their middle `(8, 8, 8)` | Standing, white: tint them |
| `hand_of_order/contract_paper` | The Contract: paper headed CONTRACT, clauses, a signature, a red wax seal | its middle | Standing, its own colors |
| `hand_of_order/order_wall` | A panel of brass bars with letters of the law climbing them (animated) | default | Standing; set in a ring: Divide the Battlefield, the Court's dome |
| `hand_of_order/word_halt` ... `word_judgment` | The spoken words (17): engraved capitals between two rules, 3 blocks wide | default | Standing, white; `HandOfOrderFx.speak(sl, who, Word.X)` |

Statuses: `HandOfOrderFx.status(player, Status.CONVICT, ticks)` shows the picture above the hotbar and a `HudTimer`; `statusEnd(...)` takes both away. `courtClauses(player, written, ticks)` shows the Court's clauses in the row beside the hotbar and the tablet in the top right.

### Key Of Stars: ready-made displays

`KeyOfStarsFx` (in `beyonder_characteristics/key_of_stars/`) holds the Key Of Stars (Door pathway) looks for all seven keys, both casts, the four wheels (Tricks on key 2, Travel on key 5, Secrets and Space on key 6, Starlight on key 7), the Starlit mark, the Recorded Powers' pages, the statuses, the passives that show and what grows at higher Sequences (people coming through Door Opening, the Spatial Cage, Symbolization and Conceptualization, Dimension Sight, Blink's lingering afterimages). Keys 1, 2, 3 and 5 use them (`KeyOfStars`, `KeyOfStarsTricks`, `KeyOfStarsTravel`); they only show things, and the damage, holding, hiding and moving are the ability's, at the ticks the constants name (`DOOR_OPENED`, `SCREEN_DOWN`, `DIVINED`, `TRAVEL_ENTERED`, `HAND_GRABS`, `CONCEALED`, `EXILE_SWALLOWED`, `TEAR_LANDS`, `TOY_SHRUNK`, `SEALED_AT`, `LABYRINTH_RISEN`, `WORLD_UNFOLDED`, `DOORS_SHUT`). The class comment lists every call by key; [KEY_OF_STARS.md](KEY_OF_STARS.md) says which call goes with which ability.

Everything it uses is generated, from `tools/model_gen/key_of_stars/` (run each from the repository root with `src/main/resources/assets/circle_of_inevitability` as the argument, then F3+T):

| Script | Makes |
|---|---|
| `models.py` (+ `textures.py`) | The models and pictures and their textures in `models/model_fx/key_of_stars/` and `textures/model_fx/key_of_stars/` (`../hanged_man/render.py` draws any of them without the game) |
| `icons.py` | The wheel pictures (`textures/gui/ability_menus/key_of_stars/`), the status pictures, the Recorded Powers' pages and Transfiguration's layers left (`textures/gui/key_of_stars/`) and the Pathway icon |
| `particles.py` | The particle pictures `fx/star_door`, `star_worm`, `star_key`, `wind_key`, `sound_wave`, `planet`, `rift_crack`, `maze` in `textures/particle/fx/` (white: any Pathway can tint them) |
| `sounds.py` | The sounds in `sounds/key_of_stars/`, their events in `sounds.json` and their subtitles; the events are registered in `sounds/ModSounds` (`STARS_...`). It borrows the building blocks of `../beauty_goddess/sounds.py` and `../knowledge_emperor/sounds.py` |

**See them in game** (cheats on): `/starsfx all` or `/starsfx alternate_world` (TAB lists the names). Displays that need a target use the nearest mob within 12 blocks, or you.

| Model | What it is | Pivot | Notes |
|---|---|---|---|
| `key_of_stars/door_frame`, `door_leaf` | An illusory Door: silver-blue posts set with gold stars, a lintel with a star keystone, 1 x 2 blocks; the door itself, deep indigo dusted with stars, gold hinges and keyhole | default; the leaf's hinge `(2, 0, 8)` | Play both at one spot; yaw the leaf to swing it. One model for every Door: Door Opening, Traveler's Door, Exile, Space Concealment, Pocket Space, the Alternate World |
| `key_of_stars/crystal_ball` | A ball of starlit glass with the night and one star inside, on a gold stand, 0.8 blocks | default | Star Divination, in the hand |
| `key_of_stars/worm_of_star` | A crescent worm of gold and white beads, 0.7 blocks | its curl `(8, 6, 8)` | Orbit them with a far pivot and `spin` (see `wormAura`) |
| `key_of_stars/star_key`, `wind_up_key` | The Key of Stars: a gold key with a star in its bow, 1.7 blocks; a toy's butterfly key, 0.6 blocks, shaft north | the shaft `(8, 15, 8)`; the shaft's tip `(8, 8, 8)` | `roll` turns them |
| `key_of_stars/star_hand` | The Invisible Hand: an open hand of see-through starlight, palm north, 1.4 blocks | default (the wrist) | Pitch 90 points it forward |
| `key_of_stars/record_book`, `quill` | The Scribe's open book, 1 block; the quill, 1.1 blocks, nib down | default | Record |
| `key_of_stars/star_gate`, `constellation`, `star_anchor`, `tumble_circle`, `labyrinth`, `toy_veil`, `seal_ring`, `door_kingdom`, `planet_ring` | Eight doors round a keyhole; a sky map; a compass rose with a door; a slick swirl; a round maze; a music-box carousel; twelve chained keyholes round a padlock; a kingdom of Doors; a planet's ring | default | Flat, white: tint them |
| `key_of_stars/door_sigil`, `starlit_mark`, `interference`, `storm_vortex` | Three doors one inside the other; the Starlit star; a turning veil of broken arcs (animated); a four-armed spiral (animated) | their middle `(8, 8, 8)` | Standing, white: tint them |
| `key_of_stars/space_tear`, `record_page` | A jagged rift with stars inside; a written page with a gold seal | their middle | Standing, their own colors |
| `key_of_stars/black_screen`, `space_shell`, `closed_door`, `afterimage` | A dark curtain (animated, its own colors); a panel of hexagons with drifting stars (animated); a barred door with a lock; a person's silhouette, 1 x 2 | default | Standing; set `space_shell` in rings for walls and domes |

Statuses: `KeyOfStarsFx.status(player, Status.STARLIT, ticks)` shows the picture above the hotbar and a `HudTimer`; `statusEnd(...)` takes both away. `layersLeft(player, layers, ticks)` shows Transfiguration's layers, `recordedPages(player, angel[])` the Recorded Powers in the row right of the hotbar.

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
