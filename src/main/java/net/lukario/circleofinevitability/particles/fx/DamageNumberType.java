package net.lukario.circleofinevitability.particles.fx;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * The registered particle type (circle_of_inevitability:damage_number) behind every DamageNumber.
 * overrideLimiter = true: a number always shows, even with "Particles: Minimal".
 */
public class DamageNumberType extends ParticleType<DamageNumber> {

    public DamageNumberType() {
        super(true);
    }

    @Override
    public MapCodec<DamageNumber> codec() {
        return DamageNumber.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, DamageNumber> streamCodec() {
        return DamageNumber.STREAM_CODEC;
    }
}
