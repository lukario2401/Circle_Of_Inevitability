package net.lukario.frogerealm.particles.fx;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * The one registered particle type (forgerealmmod:slash) behind every SlashFx.
 * overrideLimiter = true: slashes always show, even with "Particles: Minimal" and from far away.
 */
public class SlashFxType extends ParticleType<SlashFx> {

    public SlashFxType() {
        super(true);
    }

    @Override
    public MapCodec<SlashFx> codec() {
        return SlashFx.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, SlashFx> streamCodec() {
        return SlashFx.STREAM_CODEC;
    }
}
