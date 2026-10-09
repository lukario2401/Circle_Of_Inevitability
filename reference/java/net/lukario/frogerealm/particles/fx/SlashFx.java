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
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * A glowing slash trail: a curved (or straight) ribbon that sweeps into existence, lingers and
 * fades — like a sword swing, claw marks, a spinning vortex or quick straight cuts.
 * Like ParticleFx: describe it once, every method returns a NEW copy, no registering needed.
 *
 *   public static final SlashFx SWING = SlashFx.arc("slash/streaks")   // texture in textures/particle/
 *           .color(0xE070F0E0)          // glow color, ARGB (alpha = brightness)
 *           .core(0xFFFFFFFF)           // bright thin center line (core(0) = none)
 *           .radius(1.8f)               // how far the arc is from its center, in blocks
 *           .arc(170)                   // how much of a circle it covers, degrees (negative = sweeps the other way)
 *           .width(0.9f)                // thickness in blocks
 *           .taper(SlashFx.Taper.CRESCENT)  // CRESCENT = thin ends, COMET = thick at the front, UNIFORM
 *           .layers(3).spread(0.25f)    // extra thinner streaks next to the main one
 *           .lifetime(9).sweep(3);      // total ticks / ticks it takes to draw itself
 *
 *   ParticleShapes.slash(serverLevel, SWING, center, player.getYRot(), 0, 0);   // plays it facing that yaw
 *
 * Moving / changing (so it's not static):
 *   .tailColor(..).headColor(..)  color fades along the slash: tail -> color -> head
 *   .spin(12)                     keeps rotating around its center, degrees per tick
 *   .fromCenter()                 grows out from the middle in both directions (bursts, crystals)
 *
 * Shapes:
 *   SlashFx.arc(...)   curve around a center. endRadius(...) makes it a spiral (vortex).
 *   SlashFx.line(...)  straight cut through the center, radius = half its length.
 *
 * The ribbon always turns its full width toward the camera (like real sword trails), so it reads well
 * from any angle. .flat() makes it lie flat in its plane instead (ground rings, shockwaves...).
 *
 * Random but sensible orientation: .varied(random, yaw, pitch, roll) (maybe mirrored + small random tilt),
 * .mirrored() (sweep the other way), .jittered(...) (tilt only).
 *
 * Orientation (rotation(yaw, pitch, roll), in degrees, added to what you pass when playing it):
 *   yaw   = which way it faces (like an entity's yaw)
 *   pitch = tilt up/down: -90 stands the arc up so it faces the viewer (a "moon")
 *   roll  = tilt around the facing direction: 0 = flat/horizontal swing, 90 = vertical chop, -40 = rising diagonal
 *
 * Test in game:
 *   /particle forgerealmmod:slash{color:"E070F0E0",core_color:"FFFFFFFF",arc:170f,radius:1.8f,width:0.9f,layers:3} ~ ~1 ~ 0 0 0 0 1
 */
public final class SlashFx implements ParticleOptions {

    public enum Shape { ARC, LINE }

    public enum Taper {
        /** thin at both ends, thick in the middle (classic sword swing) */
        CRESCENT,
        /** thin tail, thick sharp front (claws, lunges) */
        COMET,
        /** same thickness everywhere, soft ends (cuts, beams) */
        UNIFORM
    }

    private ResourceLocation texture;
    private int color;
    private int tailColor;
    private int headColor;
    private int coreColor;
    private boolean additive;
    private boolean flat;
    private Shape shape;
    private float radius;
    private float endRadius;   // < 0 = same as radius
    private float arc;         // degrees
    private float width;
    private Taper taper;
    private int lifetime;
    private int sweep;
    private int delay;
    private float spin;        // degrees per tick
    private boolean fromCenter;
    private int layers;
    private float spread;
    private float yaw, pitch, roll;

    private SlashFx() {}

    private SlashFx copy() {
        SlashFx c = new SlashFx();
        c.texture = texture; c.color = color; c.tailColor = tailColor; c.headColor = headColor; c.coreColor = coreColor; c.additive = additive; c.flat = flat;
        c.shape = shape; c.radius = radius; c.endRadius = endRadius; c.arc = arc; c.width = width;
        c.taper = taper; c.lifetime = lifetime; c.sweep = sweep; c.delay = delay; c.layers = layers;
        c.spin = spin; c.fromCenter = fromCenter;
        c.spread = spread; c.yaw = yaw; c.pitch = pitch; c.roll = roll;
        return c;
    }

    private static SlashFx base(String textureName, Shape shape) {
        SlashFx fx = new SlashFx();
        fx.texture = textureName.contains(":")
                ? ResourceLocation.parse(textureName)
                : ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, textureName);
        fx.color = 0xFFFFFFFF;
        fx.tailColor = 0xFFFFFFFF;
        fx.headColor = 0xFFFFFFFF;
        fx.coreColor = 0;
        fx.additive = true;
        fx.flat = false;
        fx.shape = shape;
        fx.radius = 1.5f;
        fx.endRadius = -1f;
        fx.arc = 160f;
        fx.width = 0.8f;
        fx.taper = shape == Shape.LINE ? Taper.UNIFORM : Taper.CRESCENT;
        fx.lifetime = 10;
        fx.sweep = 3;
        fx.delay = 0;
        fx.spin = 0f;
        fx.fromCenter = false;
        fx.layers = 1;
        fx.spread = 0.2f;
        return fx;
    }

    /** Curved slash. Texture = PNG in textures/particle/ (without .png). Starter textures: "slash/streaks", "slash/smooth". */
    public static SlashFx arc(String texture) {
        return base(texture, Shape.ARC);
    }

    /** Straight cut through the center. radius() = half its length. */
    public static SlashFx line(String texture) {
        return base(texture, Shape.LINE).radius(1.5f).width(0.15f);
    }

    // ---------- customization (each returns a copy) ----------

    /** Color of the whole slash, ARGB (also resets tailColor/headColor). With additive blending, alpha = brightness. */
    public SlashFx color(int argb) { SlashFx c = copy(); c.color = argb; c.tailColor = argb; c.headColor = argb; return c; }

    /** Color of the back quarter of the slash (start); fades into color() by the middle. */
    public SlashFx tailColor(int argb) { SlashFx c = copy(); c.tailColor = argb; return c; }

    /** Color at the front of the slash (head); fades in from color() between 60% and 90% of its length. */
    public SlashFx headColor(int argb) { SlashFx c = copy(); c.headColor = argb; return c; }

    /** Color of a thin bright line along the middle of the slash. core(0) removes it. */
    public SlashFx core(int argb) { SlashFx c = copy(); c.coreColor = argb; return c; }

    /** Normal see-through blending instead of glowing (additive) — use for dark/colored slashes in daylight. */
    public SlashFx translucent() { SlashFx c = copy(); c.additive = false; return c; }

    /** Lie flat in the slash's plane instead of turning toward the camera (for ground rings, shockwaves). */
    public SlashFx flat() { SlashFx c = copy(); c.flat = true; return c; }

    public SlashFx radius(float blocks) { SlashFx c = copy(); c.radius = blocks; return c; }

    /** Radius at the end of the arc — different from radius() makes a spiral. */
    public SlashFx endRadius(float blocks) { SlashFx c = copy(); c.endRadius = blocks; return c; }

    /** Degrees of circle covered (360+ allowed for spirals). Negative = sweeps the opposite way. */
    public SlashFx arc(float degrees) { SlashFx c = copy(); c.arc = degrees; return c; }

    public SlashFx width(float blocks) { SlashFx c = copy(); c.width = blocks; return c; }

    public SlashFx taper(Taper taper) { SlashFx c = copy(); c.taper = taper; return c; }

    /** Total ticks the slash exists (after the delay). */
    public SlashFx lifetime(int ticks) { SlashFx c = copy(); c.lifetime = Math.max(1, ticks); return c; }

    /** Ticks it takes to draw itself from start to end (the same speed is used to wipe it away at the end). */
    public SlashFx sweep(int ticks) { SlashFx c = copy(); c.sweep = Math.max(1, ticks); return c; }

    /** Keep rotating around the center, degrees per tick (negative = other way). Lines spin like a propeller. */
    public SlashFx spin(float degreesPerTick) { SlashFx c = copy(); c.spin = degreesPerTick; return c; }

    /** Grow out from the middle towards both ends instead of drawing from start to end. */
    public SlashFx fromCenter() { SlashFx c = copy(); c.fromCenter = true; return c; }

    /** Wait this many ticks before appearing (for staggered flurries). */
    public SlashFx delay(int ticks) { SlashFx c = copy(); c.delay = Math.max(0, ticks); return c; }

    /** Number of streaks: 1 = just the main one, more = thinner shorter streaks beside it. */
    public SlashFx layers(int count) { SlashFx c = copy(); c.layers = Math.max(1, count); return c; }

    /** Distance between the streaks, in blocks. */
    public SlashFx spread(float blocks) { SlashFx c = copy(); c.spread = blocks; return c; }

    /** Set the orientation (degrees). See the class comment. */
    public SlashFx rotation(float yaw, float pitch, float roll) {
        SlashFx c = copy(); c.yaw = yaw; c.pitch = pitch; c.roll = roll; return c;
    }

    /**
     * The same slash mirrored left/right: sweeps the other way, tilts to the other side, spins the other way.
     * Still looks like the same move, just done with the other hand.
     */
    public SlashFx mirrored() {
        SlashFx c = copy();
        c.arc = -arc; c.spin = -spin; c.yaw = -yaw; c.roll = -roll;
        return c;
    }

    /**
     * A believable random version: 50% chance to be mirrored, then turned by a random amount of
     * up to +-yaw / +-pitch / +-roll degrees. Small numbers = subtle variety, big numbers = wild.
     *   SWING.varied(random, 20, 10, 15)
     */
    public SlashFx varied(RandomSource random, float yaw, float pitch, float roll) {
        SlashFx fx = random.nextBoolean() ? mirrored() : this;
        return fx.jittered(random, yaw, pitch, roll);
    }

    /** Like varied(...) but never mirrored (keeps the sweep direction). */
    public SlashFx jittered(RandomSource random, float yaw, float pitch, float roll) {
        return rotated((random.nextFloat() * 2f - 1f) * yaw,
                (random.nextFloat() * 2f - 1f) * pitch,
                (random.nextFloat() * 2f - 1f) * roll);
    }

    /** Add to the current orientation. */
    public SlashFx rotated(float yaw, float pitch, float roll) {
        return rotation(this.yaw + yaw, this.pitch + pitch, this.roll + roll);
    }

    /**
     * Takes another slash's orientation and sweep direction, so layers of one attack line up even after
     * varied()/mirrored(). E.g. a dotted lead-in that the main slash follows:
     *   SlashFx main = SWING.varied(random, 20, 6, 12);
     *   SlashFx lead = SWING_DOTS.alignedWith(main);
     */
    public SlashFx alignedWith(SlashFx other) {
        SlashFx c = rotation(other.yaw, other.pitch, other.roll);
        if (c.arc * other.arc < 0) {   // the other one was mirrored
            c.arc = -c.arc;
            c.spin = -c.spin;
        }
        return c;
    }

    // ---------- geometry (works on the server too, e.g. to spawn shards along a slash) ----------

    /**
     * The slash's orientation as unit vectors {forward, right, up}, before any spin.
     * Arcs lie in the forward/right plane with their middle at forward; lines run along right.
     */
    public Vec3[] axes() {
        double yawRad = Math.toRadians(yaw);
        double rollRad = Math.toRadians(roll);
        Vec3 f = Vec3.directionFromRotation(pitch, yaw);
        Vec3 r0 = new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));
        Vec3 u0 = r0.cross(f).normalize();
        Vec3 r = r0.scale(Math.cos(rollRad)).add(u0.scale(Math.sin(rollRad)));
        Vec3 u = u0.scale(Math.cos(rollRad)).subtract(r0.scale(Math.sin(rollRad)));
        return new Vec3[]{f, r, u};
    }

    /**
     * Where the middle of the slash is at position t (0 = start, 1 = end), relative to its center.
     * ticksIn = how long it has been visible (only matters if it spins).
     */
    public Vec3 pointAt(float t, float ticksIn) {
        Vec3[] axes = axes();
        double spinRad = Math.toRadians(spin * ticksIn);
        if (shape == Shape.LINE) {
            Vec3 lineRight = axes[1].scale(Math.cos(spinRad)).add(axes[2].scale(Math.sin(spinRad)));
            return lineRight.scale((t * 2f - 1f) * radius);
        }
        double r = radius + (endRadiusBlocks() - radius) * t;
        return radialAt(axes, t, spinRad).scale(r);
    }

    /** Direction pointing away from the slash at position t: away from the arc's center, or to one side of a line. */
    public Vec3 outwardAt(float t, float ticksIn) {
        Vec3[] axes = axes();
        double spinRad = Math.toRadians(spin * ticksIn);
        if (shape == Shape.LINE) {
            return axes[2].scale(Math.cos(spinRad)).subtract(axes[1].scale(Math.sin(spinRad)));
        }
        return radialAt(axes, t, spinRad);
    }

    private Vec3 radialAt(Vec3[] axes, float t, double spinRad) {
        double angle = Math.toRadians(-arc / 2.0 + t * arc) + spinRad;
        return axes[1].scale(Math.sin(angle)).add(axes[0].scale(Math.cos(angle)));
    }

    // ---------- getters ----------

    public ResourceLocation texture() { return texture; }
    public int glowColor() { return color; }
    public int tailGlowColor() { return tailColor; }
    public int headGlowColor() { return headColor; }
    public int coreColor() { return coreColor; }
    public boolean isAdditive() { return additive; }
    public boolean isFlat() { return flat; }
    public Shape shape() { return shape; }
    public float radiusBlocks() { return radius; }
    public float endRadiusBlocks() { return endRadius < 0 ? radius : endRadius; }
    public float arcDegrees() { return arc; }
    public float widthBlocks() { return width; }
    public Taper taperStyle() { return taper; }
    public int lifetimeTicks() { return lifetime; }
    public int sweepTicks() { return sweep; }
    public int delayTicks() { return delay; }
    public float spinDegrees() { return spin; }
    public boolean growsFromCenter() { return fromCenter; }
    public int layerCount() { return layers; }
    public float spreadBlocks() { return spread; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public float roll() { return roll; }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.SLASH.get();
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

    // The command/NBT format is split into groups only because a codec can hold at most 16 fields;
    // the fields themselves are all at the same level: {color:"...", arc:170f, lifetime:9, ...}
    private record Look(ResourceLocation texture, int color, int tailColor, int headColor, int coreColor,
                        boolean additive, boolean flat) {}
    private record Geometry(Shape shape, float radius, float endRadius, float arc, float width, Taper taper,
                            int layers, float spread) {}
    private record Timing(int lifetime, int sweep, int delay, float spin, boolean fromCenter) {}

    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "slash/streaks");

    private static final MapCodec<Look> LOOK_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("texture", DEFAULT_TEXTURE).forGetter(Look::texture),
            ParticleFx.COLOR_CODEC.optionalFieldOf("color", 0xFFFFFFFF).forGetter(Look::color),
            ParticleFx.COLOR_CODEC.optionalFieldOf("tail_color", 0).forGetter(Look::tailColor),
            ParticleFx.COLOR_CODEC.optionalFieldOf("head_color", 0).forGetter(Look::headColor),
            ParticleFx.COLOR_CODEC.optionalFieldOf("core_color", 0).forGetter(Look::coreColor),
            Codec.BOOL.optionalFieldOf("additive", true).forGetter(Look::additive),
            Codec.BOOL.optionalFieldOf("flat", false).forGetter(Look::flat)
    ).apply(i, Look::new));

    private static final MapCodec<Geometry> GEOMETRY_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            enumCodec(Shape.class).optionalFieldOf("shape", Shape.ARC).forGetter(Geometry::shape),
            Codec.FLOAT.optionalFieldOf("radius", 1.5f).forGetter(Geometry::radius),
            Codec.FLOAT.optionalFieldOf("end_radius", -1f).forGetter(Geometry::endRadius),
            Codec.FLOAT.optionalFieldOf("arc", 160f).forGetter(Geometry::arc),
            Codec.FLOAT.optionalFieldOf("width", 0.8f).forGetter(Geometry::width),
            enumCodec(Taper.class).optionalFieldOf("taper", Taper.CRESCENT).forGetter(Geometry::taper),
            Codec.INT.optionalFieldOf("layers", 1).forGetter(Geometry::layers),
            Codec.FLOAT.optionalFieldOf("spread", 0.2f).forGetter(Geometry::spread)
    ).apply(i, Geometry::new));

    private static final MapCodec<Timing> TIMING_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("lifetime", 10).forGetter(Timing::lifetime),
            Codec.INT.optionalFieldOf("sweep", 3).forGetter(Timing::sweep),
            Codec.INT.optionalFieldOf("delay", 0).forGetter(Timing::delay),
            Codec.FLOAT.optionalFieldOf("spin", 0f).forGetter(Timing::spin),
            Codec.BOOL.optionalFieldOf("from_center", false).forGetter(Timing::fromCenter)
    ).apply(i, Timing::new));

    private static SlashFx fromParts(Look look, Geometry geometry, Timing timing, Vec3 rotation) {
        SlashFx fx = new SlashFx();
        fx.texture = look.texture(); fx.color = look.color(); fx.coreColor = look.coreColor();
        // in commands, a missing tail/head color (0) means "same as color"
        fx.tailColor = look.tailColor() == 0 ? look.color() : look.tailColor();
        fx.headColor = look.headColor() == 0 ? look.color() : look.headColor();
        fx.additive = look.additive(); fx.flat = look.flat();
        fx.shape = geometry.shape(); fx.radius = geometry.radius(); fx.endRadius = geometry.endRadius();
        fx.arc = geometry.arc(); fx.width = geometry.width(); fx.taper = geometry.taper();
        fx.layers = Math.max(1, Math.min(geometry.layers(), 16)); fx.spread = geometry.spread();
        fx.lifetime = Math.max(1, timing.lifetime()); fx.sweep = Math.max(1, timing.sweep());
        fx.delay = Math.max(0, timing.delay());
        fx.spin = timing.spin(); fx.fromCenter = timing.fromCenter();
        fx.yaw = (float) rotation.x; fx.pitch = (float) rotation.y; fx.roll = (float) rotation.z;
        return fx;
    }

    public static final MapCodec<SlashFx> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            LOOK_CODEC.forGetter((SlashFx fx) -> new Look(fx.texture, fx.color, fx.tailColor, fx.headColor,
                    fx.coreColor, fx.additive, fx.flat)),
            GEOMETRY_CODEC.forGetter((SlashFx fx) -> new Geometry(fx.shape, fx.radius, fx.endRadius, fx.arc, fx.width,
                    fx.taper, fx.layers, fx.spread)),
            TIMING_CODEC.forGetter((SlashFx fx) -> new Timing(fx.lifetime, fx.sweep, fx.delay, fx.spin, fx.fromCenter)),
            Vec3.CODEC.optionalFieldOf("rotation", Vec3.ZERO).forGetter((SlashFx fx) -> new Vec3(fx.yaw, fx.pitch, fx.roll))
    ).apply(i, SlashFx::fromParts));

    public static final StreamCodec<FriendlyByteBuf, SlashFx> STREAM_CODEC =
            StreamCodec.ofMember(SlashFx::write, SlashFx::read);

    private void write(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(texture);
        buffer.writeInt(color);
        buffer.writeInt(tailColor);
        buffer.writeInt(headColor);
        buffer.writeInt(coreColor);
        buffer.writeBoolean(additive);
        buffer.writeBoolean(flat);
        buffer.writeEnum(shape);
        buffer.writeFloat(radius);
        buffer.writeFloat(endRadius);
        buffer.writeFloat(arc);
        buffer.writeFloat(width);
        buffer.writeEnum(taper);
        buffer.writeVarInt(lifetime);
        buffer.writeVarInt(sweep);
        buffer.writeVarInt(delay);
        buffer.writeFloat(spin);
        buffer.writeBoolean(fromCenter);
        buffer.writeVarInt(layers);
        buffer.writeFloat(spread);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
        buffer.writeFloat(roll);
    }

    private static SlashFx read(FriendlyByteBuf buffer) {
        SlashFx fx = new SlashFx();
        fx.texture = buffer.readResourceLocation();
        fx.color = buffer.readInt();
        fx.tailColor = buffer.readInt();
        fx.headColor = buffer.readInt();
        fx.coreColor = buffer.readInt();
        fx.additive = buffer.readBoolean();
        fx.flat = buffer.readBoolean();
        fx.shape = buffer.readEnum(Shape.class);
        fx.radius = buffer.readFloat();
        fx.endRadius = buffer.readFloat();
        fx.arc = buffer.readFloat();
        fx.width = buffer.readFloat();
        fx.taper = buffer.readEnum(Taper.class);
        fx.lifetime = buffer.readVarInt();
        fx.sweep = buffer.readVarInt();
        fx.delay = buffer.readVarInt();
        fx.spin = buffer.readFloat();
        fx.fromCenter = buffer.readBoolean();
        fx.layers = Math.min(buffer.readVarInt(), 16); // safety cap
        fx.spread = buffer.readFloat();
        fx.yaw = buffer.readFloat();
        fx.pitch = buffer.readFloat();
        fx.roll = buffer.readFloat();
        return fx;
    }
}
