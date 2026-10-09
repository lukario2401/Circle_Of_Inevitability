package net.lukario.frogerealm.particles.fx;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * The one registered particle type (forgerealmmod:model) behind every ModelFx.
 * The particle system is only used to send the effect to players and count its ticks;
 * the model itself is drawn by ModelFxRenderer.
 * overrideLimiter = true: always shows, even with "Particles: Minimal".
 */
public class ModelFxType extends ParticleType<ModelFx> {

    public ModelFxType() {
        super(true);
    }

    @Override
    public MapCodec<ModelFx> codec() {
        return ModelFx.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ModelFx> streamCodec() {
        return ModelFx.STREAM_CODEC;
    }
}
