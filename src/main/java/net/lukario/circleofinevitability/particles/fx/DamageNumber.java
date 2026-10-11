package net.lukario.circleofinevitability.particles.fx;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.lukario.circleofinevitability.particles.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * One damage number on its way to a client: how much, and above which entity.
 * Sent by combat/DamageFx (that is where who sees what is decided); drawn by DamageNumberRenderer.
 * It travels as a particle, like ParticleFx, SlashFx and ModelFx, so it needs no packet of its own.
 */
public final class DamageNumber implements ParticleOptions {

    /** Bigger hits are written as this (a kill command deals billions). */
    private static final float MOST_SHOWN = 9_999_999f;

    private final float amount;
    private final int entityId;

    /** entityId = the entity it stays above (Entity.getId()); -1 = it stays where it was spawned. */
    public DamageNumber(float amount, int entityId) {
        this.amount = amount;
        this.entityId = entityId;
    }

    public float amount() { return amount; }
    public int entityId() { return entityId; }

    /**
     * What is written above the head: "7", "12", and one decimal for hits under 100 that are not whole ("7.5").
     * Only the characters 0-9 and the point are ever used: those are the ones in textures/particle/fx/damage_digits.png.
     */
    public String text() {
        float shown = amount > 0f ? Math.min(amount, MOST_SHOWN) : 0f;      // (NaN and negatives read "0")
        if (shown >= 100f) return Integer.toString(Math.round(shown));
        int tenths = Math.round(shown * 10f);
        return tenths % 10 == 0 ? Integer.toString(tenths / 10) : tenths / 10 + "." + tenths % 10;
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.DAMAGE_NUMBER.get();
    }

    // ---------- saving / networking (you don't need to touch this) ----------

    public static final MapCodec<DamageNumber> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("amount").forGetter(DamageNumber::amount),
            Codec.INT.optionalFieldOf("entity", -1).forGetter(DamageNumber::entityId)
    ).apply(instance, DamageNumber::new));

    public static final StreamCodec<FriendlyByteBuf, DamageNumber> STREAM_CODEC =
            StreamCodec.ofMember(DamageNumber::write, DamageNumber::read);

    private void write(FriendlyByteBuf buffer) {
        buffer.writeFloat(amount);
        buffer.writeInt(entityId);
    }

    private static DamageNumber read(FriendlyByteBuf buffer) {
        return new DamageNumber(buffer.readFloat(), buffer.readInt());
    }
}
