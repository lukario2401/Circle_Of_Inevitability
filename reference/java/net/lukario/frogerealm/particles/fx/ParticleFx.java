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

import java.util.ArrayList;
import java.util.List;

/**
 * A fully customizable particle. No registering, no JSON — just:
 *
 * 1. Put a PNG in  src/main/resources/assets/forgerealmmod/textures/particle/
 *    (usually 8x8 or 16x16; make it WHITE/grey so .color() can tint it; .png.mcmeta animation works too)
 * 2. Describe the particle once, e.g. as a constant:
 *
 *    public static final ParticleFx GOLD_SPARK = ParticleFx.of("fx/spark")
 *            .color(0xFFFFD86B)           // ARGB start color (tints the texture)
 *            .endColor(0x00FF8A2A)        // fades to this over its life (alpha 00 = invisible)
 *            .size(0.2f).endSize(0.02f)   // half-width in blocks, shrinks over its life
 *            .sizeRandom(0.3f)            // each particle up to 30% bigger/smaller
 *            .lifetime(20, 10)            // 20 ticks + up to 10 random extra
 *            .gravity(0.05f)              // positive falls, negative rises
 *            .friction(0.9f)              // speed multiplier per tick (1 = never slows)
 *            .glow()                      // full bright, ignores darkness
 *            .collide()                   // bounces/stops on blocks
 *            .spin(15f)                   // degrees per tick
 *            .randomRotation();           // starts at a random angle
 *
 * 3. Spawn it with ParticleShapes (cone, burst, line, ring, sphere...) or like any vanilla particle:
 *    serverLevel.sendParticles(GOLD_SPARK, x, y, z, count, dx, dy, dz, speed)
 *
 * Every method returns a NEW copy, so variants are easy:  GOLD_SPARK.size(0.5f).lifetime(40)
 * Several textures = an animation over the particle's lifetime:  ParticleFx.of("a", "b", "c")
 *
 * Test in game with the /particle command, e.g.
 *   /particle forgerealmmod:fx{textures:["forgerealmmod:fx/spark"],color:"FFFFD86B",glow:1b,size:0.3f} ~ ~1 ~ 1 1 1 0 40
 */
public final class ParticleFx implements ParticleOptions {

    private final List<ResourceLocation> textures;
    private final int color;
    private final int endColor;
    private final float size;
    private final float endSize;
    private final float sizeRandom;
    private final int lifetime;
    private final int lifetimeRandom;
    private final float gravity;
    private final float friction;
    private final boolean glow;
    private final boolean collide;
    private final float spin;
    private final boolean randomRotation;

    private ParticleFx(List<ResourceLocation> textures, int color, int endColor, float size, float endSize,
                       float sizeRandom, int lifetime, int lifetimeRandom, float gravity, float friction,
                       boolean glow, boolean collide, float spin, boolean randomRotation) {
        this.textures = List.copyOf(textures);
        this.color = color;
        this.endColor = endColor;
        this.size = size;
        this.endSize = endSize;
        this.sizeRandom = sizeRandom;
        this.lifetime = Math.max(1, lifetime);
        this.lifetimeRandom = Math.max(0, lifetimeRandom);
        this.gravity = gravity;
        this.friction = friction;
        this.glow = glow;
        this.collide = collide;
        this.spin = spin;
        this.randomRotation = randomRotation;
    }

    /**
     * Start a particle from one or more textures in textures/particle/ (file name without .png,
     * subfolders allowed: "fx/spark"). Use "namespace:name" for another mod's or vanilla's
     * particle textures, e.g. "minecraft:flame".
     * Defaults: white, size 0.1, 20 ticks, no gravity, friction 0.98, no glow, no collision.
     */
    public static ParticleFx of(String... textureNames) {
        List<ResourceLocation> list = new ArrayList<>();
        for (String name : textureNames) {
            list.add(name.contains(":")
                    ? ResourceLocation.parse(name)
                    : ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, name));
        }
        if (list.isEmpty()) list.add(ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "fx/glow"));
        return new ParticleFx(list, 0xFFFFFFFF, 0xFFFFFFFF, 0.1f, 0.1f, 0f, 20, 0,
                0f, 0.98f, false, false, 0f, false);
    }

    // ---------- customization (each returns a copy) ----------

    /** Start AND end color, ARGB (0xAARRGGBB). Use endColor() afterwards to fade to something else. */
    public ParticleFx color(int argb) {
        return new ParticleFx(textures, argb, argb, size, endSize, sizeRandom, lifetime, lifetimeRandom,
                gravity, friction, glow, collide, spin, randomRotation);
    }

    /** Color at the end of its life, ARGB. Alpha 0x00 = fully faded out. */
    public ParticleFx endColor(int argb) {
        return new ParticleFx(textures, color, argb, size, endSize, sizeRandom, lifetime, lifetimeRandom,
                gravity, friction, glow, collide, spin, randomRotation);
    }

    /** Fade out to invisible (keeps the current end color's RGB). */
    public ParticleFx fadeOut() {
        return endColor(endColor & 0x00FFFFFF);
    }

    /** Start AND end size (half-width in blocks). Use endSize() afterwards to grow/shrink. */
    public ParticleFx size(float size) {
        return new ParticleFx(textures, color, endColor, size, size, sizeRandom, lifetime, lifetimeRandom,
                gravity, friction, glow, collide, spin, randomRotation);
    }

    public ParticleFx endSize(float endSize) {
        return new ParticleFx(textures, color, endColor, size, endSize, sizeRandom, lifetime, lifetimeRandom,
                gravity, friction, glow, collide, spin, randomRotation);
    }

    /** 0.3 = each particle's size is randomly up to 30% bigger or smaller. */
    public ParticleFx sizeRandom(float fraction) {
        return new ParticleFx(textures, color, endColor, size, endSize, fraction, lifetime, lifetimeRandom,
                gravity, friction, glow, collide, spin, randomRotation);
    }

    public ParticleFx lifetime(int ticks) {
        return lifetime(ticks, 0);
    }

    /** ticks + a random extra of 0..randomExtra ticks per particle. */
    public ParticleFx lifetime(int ticks, int randomExtra) {
        return new ParticleFx(textures, color, endColor, size, endSize, sizeRandom, ticks, randomExtra,
                gravity, friction, glow, collide, spin, randomRotation);
    }

    /** Like vanilla: 1 = falls like a normal particle, negative = floats up. */
    public ParticleFx gravity(float gravity) {
        return new ParticleFx(textures, color, endColor, size, endSize, sizeRandom, lifetime, lifetimeRandom,
                gravity, friction, glow, collide, spin, randomRotation);
    }

    /** Velocity is multiplied by this every tick. 1 = keeps its speed, 0.8 = slows quickly. */
    public ParticleFx friction(float friction) {
        return new ParticleFx(textures, color, endColor, size, endSize, sizeRandom, lifetime, lifetimeRandom,
                gravity, friction, glow, collide, spin, randomRotation);
    }

    /** Full brightness (visible in the dark). */
    public ParticleFx glow() {
        return new ParticleFx(textures, color, endColor, size, endSize, sizeRandom, lifetime, lifetimeRandom,
                gravity, friction, true, collide, spin, randomRotation);
    }

    /** Collide with blocks instead of flying through them. */
    public ParticleFx collide() {
        return new ParticleFx(textures, color, endColor, size, endSize, sizeRandom, lifetime, lifetimeRandom,
                gravity, friction, glow, true, spin, randomRotation);
    }

    /** Rotation speed in degrees per tick (negative = other way). */
    public ParticleFx spin(float degreesPerTick) {
        return new ParticleFx(textures, color, endColor, size, endSize, sizeRandom, lifetime, lifetimeRandom,
                gravity, friction, glow, collide, degreesPerTick, randomRotation);
    }

    /** Each particle starts at a random angle. */
    public ParticleFx randomRotation() {
        return new ParticleFx(textures, color, endColor, size, endSize, sizeRandom, lifetime, lifetimeRandom,
                gravity, friction, glow, collide, spin, true);
    }

    // ---------- getters (used by the client particle and ParticleShapes) ----------

    public List<ResourceLocation> textures() { return textures; }
    public int startColor() { return color; }
    public int finalColor() { return endColor; }
    public float startSize() { return size; }
    public float finalSize() { return endSize; }
    public float sizeRandomness() { return sizeRandom; }
    public int lifetimeTicks() { return lifetime; }
    public int lifetimeRandomTicks() { return lifetimeRandom; }
    public float gravityStrength() { return gravity; }
    public float frictionFactor() { return friction; }
    public boolean glows() { return glow; }
    public boolean collides() { return collide; }
    public float spinDegrees() { return spin; }
    public boolean hasRandomRotation() { return randomRotation; }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.FX.get();
    }

    // ---------- saving / networking (you don't need to touch this) ----------

    /** Colors in /particle commands are hex strings: "FFD86B" or "80FFD86B" (with alpha). */
    static final Codec<Integer> COLOR_CODEC = Codec.STRING.comapFlatMap(
            text -> {
                String hex = text.startsWith("#") ? text.substring(1) : text;
                try {
                    long value = Long.parseLong(hex, 16);
                    if (hex.length() <= 6) value |= 0xFF000000L;
                    return DataResult.success((int) value);
                } catch (NumberFormatException e) {
                    return DataResult.error(() -> "Not a hex color: " + text);
                }
            },
            argb -> String.format("%08X", argb));

    public static final MapCodec<ParticleFx> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.listOf().fieldOf("textures").forGetter(ParticleFx::textures),
            COLOR_CODEC.optionalFieldOf("color", 0xFFFFFFFF).forGetter(ParticleFx::startColor),
            COLOR_CODEC.optionalFieldOf("end_color", 0xFFFFFFFF).forGetter(ParticleFx::finalColor),
            Codec.FLOAT.optionalFieldOf("size", 0.1f).forGetter(ParticleFx::startSize),
            Codec.FLOAT.optionalFieldOf("end_size", 0.1f).forGetter(ParticleFx::finalSize),
            Codec.FLOAT.optionalFieldOf("size_random", 0f).forGetter(ParticleFx::sizeRandomness),
            Codec.INT.optionalFieldOf("lifetime", 20).forGetter(ParticleFx::lifetimeTicks),
            Codec.INT.optionalFieldOf("lifetime_random", 0).forGetter(ParticleFx::lifetimeRandomTicks),
            Codec.FLOAT.optionalFieldOf("gravity", 0f).forGetter(ParticleFx::gravityStrength),
            Codec.FLOAT.optionalFieldOf("friction", 0.98f).forGetter(ParticleFx::frictionFactor),
            Codec.BOOL.optionalFieldOf("glow", false).forGetter(ParticleFx::glows),
            Codec.BOOL.optionalFieldOf("collide", false).forGetter(ParticleFx::collides),
            Codec.FLOAT.optionalFieldOf("spin", 0f).forGetter(ParticleFx::spinDegrees),
            Codec.BOOL.optionalFieldOf("random_rotation", false).forGetter(ParticleFx::hasRandomRotation)
    ).apply(instance, ParticleFx::new));

    public static final StreamCodec<FriendlyByteBuf, ParticleFx> STREAM_CODEC =
            StreamCodec.ofMember(ParticleFx::write, ParticleFx::read);

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(textures.size());
        for (ResourceLocation texture : textures) buffer.writeResourceLocation(texture);
        buffer.writeInt(color);
        buffer.writeInt(endColor);
        buffer.writeFloat(size);
        buffer.writeFloat(endSize);
        buffer.writeFloat(sizeRandom);
        buffer.writeVarInt(lifetime);
        buffer.writeVarInt(lifetimeRandom);
        buffer.writeFloat(gravity);
        buffer.writeFloat(friction);
        buffer.writeBoolean(glow);
        buffer.writeBoolean(collide);
        buffer.writeFloat(spin);
        buffer.writeBoolean(randomRotation);
    }

    private static ParticleFx read(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<ResourceLocation> textures = new ArrayList<>();
        for (int i = 0; i < count; i++) textures.add(buffer.readResourceLocation());
        return new ParticleFx(textures, buffer.readInt(), buffer.readInt(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat(), buffer.readFloat(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readFloat(), buffer.readBoolean());
    }
}
