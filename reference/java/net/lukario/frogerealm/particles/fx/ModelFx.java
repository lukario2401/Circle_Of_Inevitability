package net.lukario.frogerealm.particles.fx;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.particles.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A 3D model shown in the world for a while: a summoned weapon, ice crystals on a frozen mob, a floating rune...
 * Like ParticleFx / SlashFx: describe it once, every method returns a NEW copy, nothing to register.
 *
 * ---------- 1. Make the model ----------
 * In Blockbench: File > New > "Java Block/Item". Build it, paint it, then File > Export > "Export Block/Item Model".
 *   - model JSON  ->  src/main/resources/assets/forgerealmmod/models/model_fx/<name>.json
 *   - texture PNG ->  src/main/resources/assets/forgerealmmod/textures/model_fx/<name>.png
 *   - in the JSON, the texture must be "forgerealmmod:model_fx/<name>"  (Blockbench writes the path for you
 *     if you saved the PNG in that folder; otherwise fix the "textures" part by hand).
 * Every model in models/model_fx/ is loaded automatically. Java models can only use cubes, coordinates
 * -16..32 pixels, and per-cube rotations of -45/-22.5/0/22.5/45 — make it small and use .scale(...) for big things.
 *
 * Orientation in Blockbench: the model's FRONT is NORTH (-Z), up is +Y. When played facing a yaw, the front
 * points that way. The pivot (what it turns/scales around and what sits on the spawn position) is the
 * bottom center (8, 0, 8) unless you set .pivot(...) — e.g. the grip of a weapon.
 *
 * ---------- 2. Describe the effect ----------
 *   public static final ModelFx HAMMER = ModelFx.of("order_hammer")   // models/model_fx/order_hammer.json
 *           .scale(1.6f)                  // 1 = Blockbench size (16 px = 1 block)
 *           .pivot(8, -10, 8)             // pixels, in Blockbench coordinates
 *           .glow()                       // full bright
 *           .aura(0xFFFFB82E, 0.14f, 3)   // glowing outline: ARGB color, thickness in blocks, softness (layers)
 *           .lifetime(40).fade(3, 8)      // ticks; fade in / fade out ticks
 *           .key(0,  ModelFx.pose().scale(0.3f).alpha(0f))                 // animation keyframes (see below)
 *           .key(6,  ModelFx.pose().scale(1f).alpha(1f).pitch(-30), ModelFx.Ease.OUT_BACK)
 *           .key(18, ModelFx.pose().pitch(90), ModelFx.Ease.IN);
 *
 * Other options: .color(argb) tint/opacity, .seeThrough() for textures with transparent pixels (ice, glass),
 * .unshaded() for fire, light and energy (every face equally bright),
 * .spin(deg/tick) (spins the whole time), .delay(ticks), .rotation(yaw, pitch, roll).
 *
 * ---------- 3. Play it (server or client) ----------
 *   ParticleShapes.model(sl, HAMMER, position, player.getYRot(), 0, 0);           // at a spot, facing a yaw
 *   ParticleShapes.modelOn(sl, CRYSTALS, mob, offset, yaw, 0, 0);                // stuck to an entity, gone when it dies
 *   ParticleShapes.clearModels(sl, mob);                                         // remove everything stuck to it
 *   ParticleShapes.modelOn(sl, CRYSTALS.tag("ice"), mob, offset, yaw, 0, 0);     // ...give it a name...
 *   ParticleShapes.clearModels(sl, mob, "ice");                                  // ...and remove only the ones with that name
 *
 * ---------- Animation ----------
 * A keyframe says: "by this tick, these things have these values". Ticks count from when it appears.
 *   .key(20, ModelFx.pose().pitch(90), ModelFx.Ease.IN)     // ticks 0-20: tips forward
 *   .key(30, ModelFx.pose().yaw(90))                        // ticks 20-30: turns right, STILL pitched 90
 *   .key(60, ModelFx.pose().forward(10), ModelFx.Ease.IN)   // ticks 30-60: flies forward, still pitched + turned
 *
 * A pose only changes what you name in it. Everything else keeps the value the earlier keys gave it
 * (keys "stick"). ModelFx.rest() puts everything back to the start in one go.
 * Each key starts moving when the key before it is reached, and uses its own Ease.
 * Several things in one key move together:  .key(60, ModelFx.pose().forward(10).up(2).scale(2f))
 *
 * Things with their own timing run next to the keys with .during(fromTick, toTick, pose, ease):
 *   .during(30, 60, ModelFx.pose().spin(720))    // while it flies (ticks 30-60) it also spins 2 full turns
 *   .during(0, 10, ModelFx.pose().scale(1f))     // ...and grows during the first 10 ticks
 * Use as many as you like; they can overlap the keys and each other.
 *
 * What a pose can set: forward/up/right (blocks, relative to the way it faces), scale, alpha, and in degrees:
 *   yaw   + = turns right (around the world's up)      pitch + = tips the top forward (a downward swing)
 *   roll  + = tips the top to the right                spin  + = turns around its OWN up axis, however it is tilted
 * Go past 360 to keep turning: yaw(720) = two full turns. All rotations happen around the pivot.
 *
 * ---------- Models that change shape (a hand closing, wings beating) ----------
 * Make one model per stage of the movement and name them <name>_0, <name>_1, <name>_2 ... Then:
 *   ModelFx.of("pale_emperor/skeletal_hand_grasp").frames(5)       // the files skeletal_hand_grasp_0 .. _4
 *           .smooth()                                              // glide between the frames instead of jumping
 *           .key(8,  ModelFx.pose().frame(4), ModelFx.Ease.IN)     // ticks 0-8: frame 0 -> 4 (the hand closes)
 *           .key(30, ModelFx.pose().frame(4))                      // holds
 *           .key(36, ModelFx.pose().frame(0), ModelFx.Ease.OUT);   // opens again
 * "frame" is animated like everything else: keys, .during(...), every Ease, and it can run backwards.
 * .smooth() needs frames made of the same cubes in the same order (copy the model and only move/turn its cubes);
 * frames that don't match simply jump. .mirrored() flips a model left-right (a left hand out of a right hand).
 *
 * Test in game (shows the model at your feet for 100 ticks):
 *   /particle forgerealmmod:model{model:"forgerealmmod:model_fx/order_hammer",glow:1b,lifetime:100} ~ ~ ~ 0 0 0 0 1
 */
public final class ModelFx implements ParticleOptions {

    /** How a keyframe is reached from the one before it. */
    public enum Ease {
        /** constant speed */
        LINEAR,
        /** starts slow, ends fast (falling, slamming) */
        IN,
        /** starts fast, ends slow (landing softly) */
        OUT,
        /** slow - fast - slow */
        IN_OUT,
        /** goes a bit too far, then settles (popping into existence) */
        OUT_BACK,
        /** hits the end and bounces a few times */
        OUT_BOUNCE;

        public float apply(float t) {
            t = Mth.clamp(t, 0f, 1f);
            switch (this) {
                case IN: return t * t * t;
                case OUT: { float u = 1f - t; return 1f - u * u * u; }
                case IN_OUT: return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
                case OUT_BACK: {
                    float c1 = 1.70158f;
                    float c3 = c1 + 1f;
                    float u = t - 1f;
                    return 1f + c3 * u * u * u + c1 * u * u;
                }
                case OUT_BOUNCE: {
                    float n1 = 7.5625f;
                    float d1 = 2.75f;
                    if (t < 1f / d1) return n1 * t * t;
                    if (t < 2f / d1) { t -= 1.5f / d1; return n1 * t * t + 0.75f; }
                    if (t < 2.5f / d1) { t -= 2.25f / d1; return n1 * t * t + 0.9375f; }
                    t -= 2.625f / d1;
                    return n1 * t * t + 0.984375f;
                }
                default: return t;
            }
        }
    }

    /**
     * What a keyframe changes: offset in blocks (forward / up / right of the direction it faces),
     * rotation in degrees, scale, alpha. Build with ModelFx.pose().pitch(90).up(0.5f)... (each method returns a copy).
     *
     * A pose only changes the things you set. Everything you leave out keeps the value it already had, so
     * .key(20, pose().pitch(90)) followed by .key(30, pose().yaw(90)) ends up pitched AND turned.
     */
    public static final class Pose {

        private static final int FORWARD = 0, UP = 1, RIGHT = 2, YAW = 3, PITCH = 4, ROLL = 5, SPIN = 6, SCALE = 7, ALPHA = 8,
                FRAME = 9;
        private static final int COUNT = 10;
        private static final int EVERYTHING = (1 << COUNT) - 1;
        private static final float[] START = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f};

        /** Changes nothing (what ModelFx.pose() starts from). */
        static final Pose NOTHING = new Pose(START.clone(), 0);
        /** Sets everything back to the start: no offset, no rotation, scale 1, alpha 1, frame 0. */
        public static final Pose REST = new Pose(START.clone(), EVERYTHING);

        private final float[] values;
        private final int set;   // bit n = this pose changes value n

        private Pose(float[] values, int set) {
            this.values = values;
            this.set = set;
        }

        private Pose with(int index, float value) {
            float[] copy = values.clone();
            copy[index] = value;
            return new Pose(copy, set | (1 << index));
        }

        private boolean sets(int index) {
            return (set & (1 << index)) != 0;
        }

        // ----- what to change -----

        /** blocks in front (of the direction the effect faces, not the way the model is turned) */
        public Pose forward(float blocks) { return with(FORWARD, blocks); }
        public Pose up(float blocks) { return with(UP, blocks); }
        public Pose right(float blocks) { return with(RIGHT, blocks); }
        public Pose move(float forward, float up, float right) { return forward(forward).up(up).right(right); }
        /** + turns right, around the world's up */
        public Pose yaw(float degrees) { return with(YAW, degrees); }
        /** + tips the top forward */
        public Pose pitch(float degrees) { return with(PITCH, degrees); }
        /** + tips the top to the right */
        public Pose roll(float degrees) { return with(ROLL, degrees); }
        public Pose rotation(float yaw, float pitch, float roll) { return yaw(yaw).pitch(pitch).roll(roll); }
        /** + turns right around its OWN up axis (the model's up in Blockbench), however it is tilted. 360 = one full turn. */
        public Pose spin(float degrees) { return with(SPIN, degrees); }
        /** multiplies the effect's own scale */
        public Pose scale(float scale) { return with(SCALE, scale); }
        /** 0 = invisible, 1 = normal */
        public Pose alpha(float alpha) { return with(ALPHA, alpha); }
        /**
         * For models with .frames(n): which of the models <name>_0 .. <name>_(n-1) shows. 0 = the first.
         * Halves are fine: with .smooth(), frame(1.5f) is half way between frame 1 and frame 2.
         */
        public Pose frame(float frame) { return with(FRAME, frame); }

        // ----- values (meaningful on the pose that poseAt(...) returns) -----

        public float forward() { return values[FORWARD]; }
        public float up() { return values[UP]; }
        public float right() { return values[RIGHT]; }
        public float yaw() { return values[YAW]; }
        public float pitch() { return values[PITCH]; }
        public float roll() { return values[ROLL]; }
        public float spin() { return values[SPIN]; }
        public float scale() { return values[SCALE]; }
        public float alpha() { return values[ALPHA]; }
        public float frame() { return values[FRAME]; }
    }

    /**
     * One step of the animation: between 'from' and 'tick' the things set in 'pose' move to those values, with 'ease'.
     * from = -1 means "from the keyframe before this one" (what .key(...) makes); .during(...) gives a real start tick.
     */
    public record Key(int from, int tick, Pose pose, Ease ease) {
        public boolean isSequenced() { return from < 0; }
    }

    /** Start for keyframes: changes nothing until you name what to change, e.g. ModelFx.pose().pitch(90). */
    public static Pose pose() {
        return Pose.NOTHING;
    }

    /** A pose that puts EVERYTHING back to the start (no offset, no rotation, scale 1, alpha 1). */
    public static Pose rest() {
        return Pose.REST;
    }

    public static final String FOLDER = "model_fx";
    private static final int MAX_KEYS = 64;
    private static final int MAX_FRAMES = 64;

    private ResourceLocation model;
    private int color;
    private boolean glow;
    private boolean seeThrough;
    private boolean unshaded;
    private int auraColor;
    private float auraSize;
    private int auraLayers;
    private float scaleX, scaleY, scaleZ;
    private float pivotX, pivotY, pivotZ;   // pixels
    private float yaw, pitch, roll;         // degrees
    private int lifetime;
    private int delay;
    private int fadeIn;
    private int fadeOut;
    private float spin;                     // degrees per tick, around the model's own up
    private int frames;                     // 1 = one model; more = the models <model>_0 .. <model>_(frames-1)
    private boolean smooth;                 // glide between frames instead of jumping
    private ResourceLocation[] frameModels; // made when needed
    private int followId;                   // -1 = stays where it was played
    private Vec3 followOffset;
    private boolean turns;                  // turns with the entity it follows (wings, a shield)
    private boolean head;                   // ...with its HEAD: looks where it looks, nods when it nods (a crown, a halo)
    private boolean clear;                  // special: removes models stuck to followId
    private String tag;                     // name for clearModels(entity, tag); "" = no name
    private List<Key> keys;                 // sorted by tick
    private Key[] timeline;                 // keys with their real start tick, in the order they apply (made when needed)

    private ModelFx() {}

    private ModelFx copy() {
        ModelFx c = new ModelFx();
        c.model = model; c.color = color; c.glow = glow; c.seeThrough = seeThrough; c.unshaded = unshaded;
        c.auraColor = auraColor; c.auraSize = auraSize; c.auraLayers = auraLayers;
        c.scaleX = scaleX; c.scaleY = scaleY; c.scaleZ = scaleZ;
        c.pivotX = pivotX; c.pivotY = pivotY; c.pivotZ = pivotZ;
        c.yaw = yaw; c.pitch = pitch; c.roll = roll;
        c.lifetime = lifetime; c.delay = delay; c.fadeIn = fadeIn; c.fadeOut = fadeOut; c.spin = spin;
        c.frames = frames; c.smooth = smooth;
        c.followId = followId; c.followOffset = followOffset; c.turns = turns; c.head = head; c.clear = clear; c.tag = tag;
        c.keys = keys;
        return c;
    }

    /**
     * A model from models/model_fx/ (file name without .json), e.g. "order_hammer".
     * "namespace:path" uses any model, e.g. "forgerealmmod:model_fx/sub/thing".
     * Defaults: white, scale 1, pivot (8, 0, 8), 20 ticks, no glow, no aura.
     */
    public static ModelFx of(String name) {
        ModelFx fx = new ModelFx();
        fx.model = name.contains(":")
                ? ResourceLocation.parse(name)
                : ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, FOLDER + "/" + name);
        fx.color = 0xFFFFFFFF;
        fx.glow = false;
        fx.seeThrough = false;
        fx.unshaded = false;
        fx.auraColor = 0;
        fx.auraSize = 0.1f;
        fx.auraLayers = 3;
        fx.scaleX = fx.scaleY = fx.scaleZ = 1f;
        fx.pivotX = 8f; fx.pivotY = 0f; fx.pivotZ = 8f;
        fx.lifetime = 20;
        fx.frames = 1;
        fx.followId = -1;
        fx.followOffset = Vec3.ZERO;
        fx.tag = "";
        fx.keys = List.of();
        return fx;
    }

    /** Special "effect" that removes every model stuck to this entity (see ParticleShapes.clearModels). */
    public static ModelFx clearing(Entity entity) {
        return clearing(entity, "");
    }

    /** Same, but only the models that were given this tag with .tag(...). "" = all of them. */
    public static ModelFx clearing(Entity entity, String tag) {
        ModelFx fx = of("clear").following(entity).lifetime(1).tag(tag);
        fx.clear = true;
        return fx;
    }

    /**
     * Special "effect" that removes every model that is NOT stuck to an entity and was given this tag
     * (see ParticleShapes.clearModels(level, position, tag)). For a model that must be able to vanish
     * before its time is up: a skull that bursts when it hits something.
     */
    public static ModelFx clearingTag(String tag) {
        ModelFx fx = of("clear").lifetime(1).tag(tag);
        fx.clear = true;
        return fx;
    }

    // ---------- look ----------

    /** Tint (multiplies the texture) and opacity, ARGB. 0x80FFFFFF = half see-through. */
    public ModelFx color(int argb) { ModelFx c = copy(); c.color = argb; return c; }

    /** Full brightness: visible in the dark, not affected by the world's light. */
    public ModelFx glow() { ModelFx c = copy(); c.glow = true; return c; }

    /**
     * For textures with see-through pixels (ice, glass, energy shells): drawn after the other models
     * so whatever is inside it stays visible.
     */
    public ModelFx seeThrough() { ModelFx c = copy(); c.seeThrough = true; return c; }

    /**
     * Every face is equally bright, whichever way it points: for fire, light and energy.
     * Without it the sides are darker than the top and the underside is darkest, like every Minecraft model.
     * That shading is what makes solid things (a hammer, ice) look 3D, but it makes flames look dull.
     */
    public ModelFx unshaded() { ModelFx c = copy(); c.unshaded = true; return c; }

    /**
     * Glowing outline around the model (light added on top, like the hammer's golden glow).
     * @param argb   color; alpha = strength
     * @param blocks how far the glow reaches out from the model
     */
    public ModelFx aura(int argb, float blocks) { return aura(argb, blocks, auraLayers); }

    /** layers = how soft the glow is (1 = one hard shell, 3-4 = soft). Each layer draws the model again. */
    public ModelFx aura(int argb, float blocks, int layers) {
        ModelFx c = copy(); c.auraColor = argb; c.auraSize = blocks; c.auraLayers = Mth.clamp(layers, 1, 6); return c;
    }

    public ModelFx noAura() { ModelFx c = copy(); c.auraColor = 0; return c; }

    // ---------- shape ----------

    /** 1 = the size it has in Blockbench (16 px = 1 block). */
    public ModelFx scale(float scale) { return scale(scale, scale, scale); }

    /** Different scale per axis (x = width, y = height, z = depth/length). */
    public ModelFx scale(float x, float y, float z) {
        ModelFx c = copy(); c.scaleX = x; c.scaleY = y; c.scaleZ = z; return c;
    }

    /**
     * Flipped left-right, like in a mirror (a left hand out of a right hand). The pivot stays where it is.
     * Calling it twice flips it back.
     */
    public ModelFx mirrored() { return scale(-scaleX, scaleY, scaleZ); }

    /** The point (in Blockbench pixels) that sits on the spawn position and everything turns around. */
    public ModelFx pivot(float x, float y, float z) {
        ModelFx c = copy(); c.pivotX = x; c.pivotY = y; c.pivotZ = z; return c;
    }

    /** Orientation in degrees (yaw = facing like an entity, pitch + = tipped forward, roll + = tipped right). */
    public ModelFx rotation(float yaw, float pitch, float roll) {
        ModelFx c = copy(); c.yaw = yaw; c.pitch = pitch; c.roll = roll; return c;
    }

    /** Add to the current orientation. */
    public ModelFx rotated(float yaw, float pitch, float roll) {
        return rotation(this.yaw + yaw, this.pitch + pitch, this.roll + roll);
    }

    // ---------- timing ----------

    /** Ticks it stays (after the delay). */
    public ModelFx lifetime(int ticks) { ModelFx c = copy(); c.lifetime = Math.max(1, ticks); return c; }

    /** Wait this many ticks before appearing. */
    public ModelFx delay(int ticks) { ModelFx c = copy(); c.delay = Math.max(0, ticks); return c; }

    /** Fade in over the first 'in' ticks and out over the last 'out' ticks (0 = pop). */
    public ModelFx fade(int in, int out) { ModelFx c = copy(); c.fadeIn = Math.max(0, in); c.fadeOut = Math.max(0, out); return c; }

    public ModelFx fadeIn(int ticks) { return fade(ticks, fadeOut); }

    public ModelFx fadeOut(int ticks) { return fade(fadeIn, ticks); }

    /**
     * Keeps turning around its own up axis the whole time, degrees per tick (negative = other way).
     * To spin only for a while, animate it instead: .during(30, 100, ModelFx.pose().spin(1440))
     */
    public ModelFx spin(float degreesPerTick) { ModelFx c = copy(); c.spin = degreesPerTick; return c; }

    /**
     * A model that changes shape: instead of one model there are 'count' of them, named like this one with
     * _0, _1, _2 ... at the end (ModelFx.of("hand").frames(5) = hand_0.json .. hand_4.json).
     * It shows frame 0 until a key says otherwise:  .key(10, ModelFx.pose().frame(4))  goes through 0..4 in 10 ticks.
     */
    public ModelFx frames(int count) { ModelFx c = copy(); c.frames = Mth.clamp(count, 1, MAX_FRAMES); return c; }

    /**
     * With .frames(n): glide from one frame to the next instead of jumping, so a few frames give a fluid movement.
     * Works when the frames are the same cubes in the same order, only moved or turned (and painted the same way);
     * frames that don't match just jump. Keep each cube's turn between two frames small (45 degrees or less).
     */
    public ModelFx smooth() { ModelFx c = copy(); c.smooth = true; return c; }

    /** Add a keyframe reached at constant speed (Ease.LINEAR). */
    public ModelFx key(int tick, Pose pose) { return key(tick, pose, Ease.LINEAR); }

    /**
     * Add a keyframe: at 'tick' the things set in 'pose' have reached those values. They start moving at the
     * keyframe before this one (or at tick 0 if this is the first), following 'ease'.
     * Things the pose doesn't set are left alone - they keep what earlier keys gave them.
     * A keyframe at the same tick is replaced.
     */
    public ModelFx key(int tick, Pose pose, Ease ease) {
        int at = Math.max(0, tick);
        List<Key> list = new ArrayList<>(keys);
        list.removeIf(k -> k.isSequenced() && k.tick() == at);
        list.add(new Key(-1, at, pose, ease));
        return withKeys(list);
    }

    /** Like the one below, at constant speed (Ease.LINEAR). */
    public ModelFx during(int fromTick, int toTick, Pose pose) { return during(fromTick, toTick, pose, Ease.LINEAR); }

    /**
     * An extra animation that runs at the same time as the keyframes: between fromTick and toTick the things
     * set in 'pose' move to those values. Add as many as you want, they may overlap the keys and each other:
     *   .key(100, ModelFx.pose().forward(12), ModelFx.Ease.IN)     // flies forward...
     *   .during(30, 100, ModelFx.pose().spin(1440))                // ...and spins 4 turns on the way
     * fromTick == toTick makes it jump there at that tick.
     */
    public ModelFx during(int fromTick, int toTick, Pose pose, Ease ease) {
        int from = Math.max(0, fromTick);
        List<Key> list = new ArrayList<>(keys);
        list.add(new Key(from, Math.max(from, toTick), pose, ease));
        return withKeys(list);
    }

    /** Remove all keyframes and .during(...) animations (stays still in its rest pose). */
    public ModelFx noKeys() { ModelFx c = copy(); c.keys = List.of(); return c; }

    private ModelFx withKeys(List<Key> list) {
        list.sort((a, b) -> Integer.compare(a.tick(), b.tick())); // stable: same tick keeps the order they were added
        ModelFx c = copy();
        c.keys = List.copyOf(list);
        return c;
    }

    // ---------- sticking to an entity ----------

    /** Moves along with the entity (offset from its feet) and disappears when the entity dies or unloads. */
    public ModelFx following(Entity entity, Vec3 offset) {
        ModelFx c = copy(); c.followId = entity.getId(); c.followOffset = offset; return c;
    }

    public ModelFx following(Entity entity) { return following(entity, Vec3.ZERO); }

    public ModelFx notFollowing() { ModelFx c = copy(); c.followId = -1; c.followOffset = Vec3.ZERO; c.turns = false; c.head = false; return c; }

    /**
     * For a model stuck to an entity: it also TURNS with it, like something the entity wears (wings, a shield).
     * Its yaw is then counted from the way the entity's body faces: 0 = it looks the way the body does.
     * Use ParticleShapes.modelOnTurning(...), which sets this for you.
     */
    public ModelFx turning() { ModelFx c = copy(); c.turns = true; c.head = false; return c; }

    /**
     * For a model stuck to an entity: it is worn on the HEAD (a crown, a halo, horns, a mask). It looks where
     * the head looks, left and right and up and down, and it sits on top of the head however tall the entity
     * is or stands (a sneaking player is lower). The wearer does not see it in first person, like a helmet.
     * Use ParticleShapes.modelOnHead(...), which sets this for you.
     */
    public ModelFx turningWithHead() { ModelFx c = copy(); c.turns = true; c.head = true; return c; }

    /**
     * A name for this model, so it can be removed on its own: ParticleShapes.clearModels(sl, entity, "ice")
     * removes the models on that entity tagged "ice" and leaves everything else stuck to it alone.
     * Only matters for models stuck to an entity (modelOn). Give every status its own tag.
     */
    public ModelFx tag(String tag) { ModelFx c = copy(); c.tag = tag == null ? "" : tag; return c; }

    // ---------- animation math (client renderer uses this, the server can too) ----------

    /**
     * Where the animation has brought the model 'ticks' after it appeared (partial ticks allowed):
     * every value filled in, starting from the rest pose and applying the keys in order.
     */
    public Pose poseAt(float ticks) {
        float[] values = Pose.START.clone();
        for (Key key : timeline()) {
            if (ticks < key.from()) break;   // sorted by start: nothing after this has begun either
            boolean reached = ticks >= key.tick();
            float amount = reached ? 1f : key.ease().apply((ticks - key.from()) / (key.tick() - key.from()));
            Pose target = key.pose();
            for (int i = 0; i < Pose.COUNT; i++) {
                if (!target.sets(i)) continue;
                // values[i] is what the earlier keys left it at, so this key carries on from there
                values[i] = reached ? target.values[i] : values[i] + (target.values[i] - values[i]) * amount;
            }
        }
        return new Pose(values, Pose.EVERYTHING);
    }

    /** The keys with their real start ticks, sorted by when they start. */
    private Key[] timeline() {
        Key[] result = timeline;
        if (result == null) {
            List<Key> list = new ArrayList<>(keys.size());
            int previous = 0;
            for (Key key : keys) {
                if (key.isSequenced()) {
                    list.add(new Key(previous, key.tick(), key.pose(), key.ease()));
                    previous = key.tick();
                } else {
                    list.add(key);
                }
            }
            list.sort((a, b) -> a.from() != b.from() ? Integer.compare(a.from(), b.from())
                    : Integer.compare(a.tick(), b.tick()));
            result = list.toArray(new Key[0]);
            timeline = result;
        }
        return result;
    }

    /** Opacity from fade in / fade out only (0..1). */
    public float fadeAt(float ticks) {
        float in = fadeIn > 0 ? Mth.clamp(ticks / fadeIn, 0f, 1f) : 1f;
        float out = fadeOut > 0 ? Mth.clamp((lifetime - ticks) / fadeOut, 0f, 1f) : 1f;
        return in * out;
    }

    // ---------- getters ----------

    public ResourceLocation model() { return model; }
    public int tintColor() { return color; }
    public boolean glows() { return glow; }
    public boolean isSeeThrough() { return seeThrough; }
    public boolean isUnshaded() { return unshaded; }
    public int auraColor() { return auraColor; }
    public float auraBlocks() { return auraSize; }
    public int auraLayerCount() { return auraLayers; }
    public float scaleX() { return scaleX; }
    public float scaleY() { return scaleY; }
    public float scaleZ() { return scaleZ; }
    public float pivotX() { return pivotX; }
    public float pivotY() { return pivotY; }
    public float pivotZ() { return pivotZ; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public float roll() { return roll; }
    public int lifetimeTicks() { return lifetime; }
    public int delayTicks() { return delay; }
    public int fadeInTicks() { return fadeIn; }
    public int fadeOutTicks() { return fadeOut; }
    public float spinDegrees() { return spin; }
    public int followId() { return followId; }
    public boolean followsEntity() { return followId >= 0; }
    public Vec3 followOffset() { return followOffset; }
    public boolean turnsWithEntity() { return turns; }
    public boolean turnsWithHead() { return turns && head; }
    public boolean isClear() { return clear; }
    public String tagName() { return tag; }
    public List<Key> keys() { return keys; }
    /** 1 = an ordinary model; more = it is made of the models frameModel(0) .. frameModel(frameCount() - 1). */
    public int frameCount() { return frames; }
    public boolean isSmooth() { return smooth; }

    /** The model of one frame: this model's name with _<index> at the end. */
    public ResourceLocation frameModel(int index) {
        ResourceLocation[] all = frameModels;
        if (all == null) {
            all = new ResourceLocation[frames];
            for (int i = 0; i < frames; i++) {
                all[i] = ResourceLocation.fromNamespaceAndPath(model.getNamespace(), model.getPath() + "_" + i);
            }
            frameModels = all;
        }
        return all[Mth.clamp(index, 0, all.length - 1)];
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.MODEL.get();
    }

    // ---------- saving / networking (you don't need to touch this) ----------

    private static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
        return Codec.STRING.comapFlatMap(
                name -> {
                    try {
                        return DataResult.success(Enum.valueOf(type, name.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException e) {
                        return DataResult.error(() -> "Unknown " + type.getSimpleName() + ": " + name);
                    }
                },
                value -> value.name().toLowerCase(Locale.ROOT));
    }

    // In commands a pose is just the values it changes: {tick:20, pitch:90f}. Left out = not changed (NaN in here).
    private static MapCodec<Float> poseField(String name) {
        return Codec.FLOAT.optionalFieldOf(name, Float.NaN);
    }

    private static float fieldOf(Pose pose, int index) {
        return pose.sets(index) ? pose.values[index] : Float.NaN;
    }

    private static Pose poseFromFields(float forward, float up, float right, float yaw, float pitch, float roll,
                                       float spin, float scale, float alpha, float frame) {
        float[] given = {forward, up, right, yaw, pitch, roll, spin, scale, alpha, frame};
        float[] values = Pose.START.clone();
        int set = 0;
        for (int i = 0; i < Pose.COUNT; i++) {
            if (Float.isNaN(given[i])) continue;
            values[i] = given[i];
            set |= 1 << i;
        }
        return new Pose(values, set);
    }

    private static final MapCodec<Pose> POSE_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            poseField("forward").forGetter((Pose p) -> fieldOf(p, Pose.FORWARD)),
            poseField("up").forGetter((Pose p) -> fieldOf(p, Pose.UP)),
            poseField("right").forGetter((Pose p) -> fieldOf(p, Pose.RIGHT)),
            poseField("yaw").forGetter((Pose p) -> fieldOf(p, Pose.YAW)),
            poseField("pitch").forGetter((Pose p) -> fieldOf(p, Pose.PITCH)),
            poseField("roll").forGetter((Pose p) -> fieldOf(p, Pose.ROLL)),
            poseField("spin").forGetter((Pose p) -> fieldOf(p, Pose.SPIN)),
            poseField("scale").forGetter((Pose p) -> fieldOf(p, Pose.SCALE)),
            poseField("alpha").forGetter((Pose p) -> fieldOf(p, Pose.ALPHA)),
            poseField("frame").forGetter((Pose p) -> fieldOf(p, Pose.FRAME))
    ).apply(i, ModelFx::poseFromFields));

    // {tick:20, pitch:90f} = a .key(...);  {from:30, tick:100, spin:1440f} = a .during(...)
    private static final Codec<Key> KEY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("from", -1).forGetter(Key::from),
            Codec.INT.fieldOf("tick").forGetter(Key::tick),
            POSE_CODEC.forGetter(Key::pose),
            enumCodec(Ease.class).optionalFieldOf("ease", Ease.LINEAR).forGetter(Key::ease)
    ).apply(i, ModelFx::keyFromCommand));

    private static Key keyFromCommand(int from, int tick, Pose pose, Ease ease) {
        int start = from < 0 ? -1 : from;
        return new Key(start, Math.max(Math.max(0, start), tick), pose, ease);
    }

    // The command/NBT format is split into groups only because a codec can hold at most 16 fields;
    // the fields themselves are all at the same level: {model:"...", glow:1b, lifetime:40, ...}
    private record Look(ResourceLocation model, int color, boolean glow, boolean seeThrough, boolean unshaded,
                        int auraColor, float auraSize, int auraLayers) {}
    private record Shape(Vec3 scale, Vec3 pivot, Vec3 rotation) {}
    private record Timing(int lifetime, int delay, int fadeIn, int fadeOut, float spin, int frames, boolean smooth) {}
    private record Link(int followId, Vec3 followOffset, boolean clear, String tag, boolean turns, boolean head) {}

    private static final ResourceLocation DEFAULT_MODEL =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, FOLDER + "/order_hammer");

    private static final MapCodec<Look> LOOK_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("model", DEFAULT_MODEL).forGetter(Look::model),
            ParticleFx.COLOR_CODEC.optionalFieldOf("color", 0xFFFFFFFF).forGetter(Look::color),
            Codec.BOOL.optionalFieldOf("glow", false).forGetter(Look::glow),
            Codec.BOOL.optionalFieldOf("see_through", false).forGetter(Look::seeThrough),
            Codec.BOOL.optionalFieldOf("unshaded", false).forGetter(Look::unshaded),
            ParticleFx.COLOR_CODEC.optionalFieldOf("aura_color", 0).forGetter(Look::auraColor),
            Codec.FLOAT.optionalFieldOf("aura_size", 0.1f).forGetter(Look::auraSize),
            Codec.INT.optionalFieldOf("aura_layers", 3).forGetter(Look::auraLayers)
    ).apply(i, Look::new));

    private static final MapCodec<Shape> SHAPE_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Vec3.CODEC.optionalFieldOf("scale", new Vec3(1, 1, 1)).forGetter(Shape::scale),
            Vec3.CODEC.optionalFieldOf("pivot", new Vec3(8, 0, 8)).forGetter(Shape::pivot),
            Vec3.CODEC.optionalFieldOf("rotation", Vec3.ZERO).forGetter(Shape::rotation)
    ).apply(i, Shape::new));

    private static final MapCodec<Timing> TIMING_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("lifetime", 20).forGetter(Timing::lifetime),
            Codec.INT.optionalFieldOf("delay", 0).forGetter(Timing::delay),
            Codec.INT.optionalFieldOf("fade_in", 0).forGetter(Timing::fadeIn),
            Codec.INT.optionalFieldOf("fade_out", 0).forGetter(Timing::fadeOut),
            Codec.FLOAT.optionalFieldOf("spin", 0f).forGetter(Timing::spin),
            Codec.INT.optionalFieldOf("frames", 1).forGetter(Timing::frames),
            Codec.BOOL.optionalFieldOf("smooth", false).forGetter(Timing::smooth)
    ).apply(i, Timing::new));

    private static final MapCodec<Link> LINK_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("follow", -1).forGetter(Link::followId),
            Vec3.CODEC.optionalFieldOf("follow_offset", Vec3.ZERO).forGetter(Link::followOffset),
            Codec.BOOL.optionalFieldOf("clear", false).forGetter(Link::clear),
            Codec.STRING.optionalFieldOf("tag", "").forGetter(Link::tag),
            Codec.BOOL.optionalFieldOf("turns", false).forGetter(Link::turns),
            Codec.BOOL.optionalFieldOf("head", false).forGetter(Link::head)
    ).apply(i, Link::new));

    private static ModelFx fromParts(Look look, Shape shape, Timing timing, Link link, List<Key> keys) {
        ModelFx fx = new ModelFx();
        fx.model = look.model(); fx.color = look.color(); fx.glow = look.glow(); fx.seeThrough = look.seeThrough();
        fx.unshaded = look.unshaded();
        fx.auraColor = look.auraColor(); fx.auraSize = look.auraSize(); fx.auraLayers = Mth.clamp(look.auraLayers(), 1, 6);
        fx.scaleX = (float) shape.scale().x; fx.scaleY = (float) shape.scale().y; fx.scaleZ = (float) shape.scale().z;
        fx.pivotX = (float) shape.pivot().x; fx.pivotY = (float) shape.pivot().y; fx.pivotZ = (float) shape.pivot().z;
        fx.yaw = (float) shape.rotation().x; fx.pitch = (float) shape.rotation().y; fx.roll = (float) shape.rotation().z;
        fx.lifetime = Math.max(1, timing.lifetime()); fx.delay = Math.max(0, timing.delay());
        fx.fadeIn = Math.max(0, timing.fadeIn()); fx.fadeOut = Math.max(0, timing.fadeOut()); fx.spin = timing.spin();
        fx.frames = Mth.clamp(timing.frames(), 1, MAX_FRAMES); fx.smooth = timing.smooth();
        fx.followId = link.followId(); fx.followOffset = link.followOffset(); fx.clear = link.clear();
        fx.tag = link.tag();
        fx.turns = link.turns();
        fx.head = link.head();
        List<Key> sorted = new ArrayList<>(keys);
        sorted.sort((a, b) -> Integer.compare(a.tick(), b.tick()));
        fx.keys = List.copyOf(sorted);
        return fx;
    }

    public static final MapCodec<ModelFx> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            LOOK_CODEC.forGetter((ModelFx fx) -> new Look(fx.model, fx.color, fx.glow, fx.seeThrough, fx.unshaded,
                    fx.auraColor, fx.auraSize, fx.auraLayers)),
            SHAPE_CODEC.forGetter((ModelFx fx) -> new Shape(new Vec3(fx.scaleX, fx.scaleY, fx.scaleZ),
                    new Vec3(fx.pivotX, fx.pivotY, fx.pivotZ), new Vec3(fx.yaw, fx.pitch, fx.roll))),
            TIMING_CODEC.forGetter((ModelFx fx) -> new Timing(fx.lifetime, fx.delay, fx.fadeIn, fx.fadeOut, fx.spin,
                    fx.frames, fx.smooth)),
            LINK_CODEC.forGetter((ModelFx fx) -> new Link(fx.followId, fx.followOffset, fx.clear, fx.tag, fx.turns, fx.head)),
            KEY_CODEC.listOf().optionalFieldOf("keys", List.of()).forGetter(ModelFx::keys)
    ).apply(i, ModelFx::fromParts));

    public static final StreamCodec<FriendlyByteBuf, ModelFx> STREAM_CODEC =
            StreamCodec.ofMember(ModelFx::write, ModelFx::read);

    private void write(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(model);
        buffer.writeInt(color);
        buffer.writeBoolean(glow);
        buffer.writeBoolean(seeThrough);
        buffer.writeBoolean(unshaded);
        buffer.writeInt(auraColor);
        buffer.writeFloat(auraSize);
        buffer.writeVarInt(auraLayers);
        buffer.writeFloat(scaleX);
        buffer.writeFloat(scaleY);
        buffer.writeFloat(scaleZ);
        buffer.writeFloat(pivotX);
        buffer.writeFloat(pivotY);
        buffer.writeFloat(pivotZ);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
        buffer.writeFloat(roll);
        buffer.writeVarInt(lifetime);
        buffer.writeVarInt(delay);
        buffer.writeVarInt(fadeIn);
        buffer.writeVarInt(fadeOut);
        buffer.writeFloat(spin);
        buffer.writeVarInt(frames);
        buffer.writeBoolean(smooth);
        buffer.writeInt(followId);
        buffer.writeDouble(followOffset.x);
        buffer.writeDouble(followOffset.y);
        buffer.writeDouble(followOffset.z);
        buffer.writeBoolean(clear);
        buffer.writeUtf(tag);
        buffer.writeBoolean(turns);
        buffer.writeBoolean(head);
        int count = Math.min(keys.size(), MAX_KEYS);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            Key key = keys.get(i);
            buffer.writeVarInt(key.from() + 1);   // 0 = "from the key before"
            buffer.writeVarInt(key.tick());
            buffer.writeEnum(key.ease());
            Pose pose = key.pose();
            buffer.writeVarInt(pose.set);
            for (int n = 0; n < Pose.COUNT; n++) {
                if (pose.sets(n)) buffer.writeFloat(pose.values[n]);
            }
        }
    }

    private static ModelFx read(FriendlyByteBuf buffer) {
        ModelFx fx = new ModelFx();
        fx.model = buffer.readResourceLocation();
        fx.color = buffer.readInt();
        fx.glow = buffer.readBoolean();
        fx.seeThrough = buffer.readBoolean();
        fx.unshaded = buffer.readBoolean();
        fx.auraColor = buffer.readInt();
        fx.auraSize = buffer.readFloat();
        fx.auraLayers = Mth.clamp(buffer.readVarInt(), 1, 6);
        fx.scaleX = buffer.readFloat();
        fx.scaleY = buffer.readFloat();
        fx.scaleZ = buffer.readFloat();
        fx.pivotX = buffer.readFloat();
        fx.pivotY = buffer.readFloat();
        fx.pivotZ = buffer.readFloat();
        fx.yaw = buffer.readFloat();
        fx.pitch = buffer.readFloat();
        fx.roll = buffer.readFloat();
        fx.lifetime = Math.max(1, buffer.readVarInt());
        fx.delay = buffer.readVarInt();
        fx.fadeIn = buffer.readVarInt();
        fx.fadeOut = buffer.readVarInt();
        fx.spin = buffer.readFloat();
        fx.frames = Mth.clamp(buffer.readVarInt(), 1, MAX_FRAMES);
        fx.smooth = buffer.readBoolean();
        fx.followId = buffer.readInt();
        fx.followOffset = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        fx.clear = buffer.readBoolean();
        fx.tag = buffer.readUtf();
        fx.turns = buffer.readBoolean();
        fx.head = buffer.readBoolean();
        int count = Math.min(buffer.readVarInt(), MAX_KEYS); // safety cap
        List<Key> keys = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int from = buffer.readVarInt() - 1;
            int tick = buffer.readVarInt();
            Ease ease = buffer.readEnum(Ease.class);
            int set = buffer.readVarInt() & Pose.EVERYTHING;
            float[] values = Pose.START.clone();
            for (int n = 0; n < Pose.COUNT; n++) {
                if ((set & (1 << n)) != 0) values[n] = buffer.readFloat();
            }
            keys.add(new Key(from, tick, new Pose(values, set), ease));
        }
        fx.keys = List.copyOf(keys);
        return fx;
    }
}
