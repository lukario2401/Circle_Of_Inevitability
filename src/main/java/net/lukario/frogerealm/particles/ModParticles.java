package net.lukario.frogerealm.particles;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.particles.fx.ModelFxType;
import net.lukario.frogerealm.particles.fx.ParticleFxType;
import net.lukario.frogerealm.particles.fx.SlashFxType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ForgeRealm.MOD_ID);

    public static final RegistryObject<SimpleParticleType> VOID_RIFT =
            PARTICLES.register("void_rift",
                    () -> new SimpleParticleType(true));

    // One type for every ParticleFx (see particles/fx/ParticleFx) — new particles don't need registering
    public static final RegistryObject<ParticleFxType> FX =
            PARTICLES.register("fx", ParticleFxType::new);

    // One type for every SlashFx (see particles/fx/SlashFx) — slash trails, also no registering needed
    public static final RegistryObject<SlashFxType> SLASH =
            PARTICLES.register("slash", SlashFxType::new);

    // One type for every ModelFx (see particles/fx/ModelFx) — 3D model effects, also no registering needed
    public static final RegistryObject<ModelFxType> MODEL =
            PARTICLES.register("model", ModelFxType::new);
}
