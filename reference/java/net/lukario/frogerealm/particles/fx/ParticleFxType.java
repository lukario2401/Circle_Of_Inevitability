package net.lukario.frogerealm.particles.fx;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * The one registered particle type (forgerealmmod:fx) behind every ParticleFx.
 * All the look/behaviour travels inside the ParticleFx itself, so new particles
 * never need to be registered.
 */
public class ParticleFxType extends ParticleType<ParticleFx> {

    public ParticleFxType() {
        super(false);
    }

    @Override
    public MapCodec<ParticleFx> codec() {
        return ParticleFx.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ParticleFx> streamCodec() {
        return ParticleFx.STREAM_CODEC;
    }
}
