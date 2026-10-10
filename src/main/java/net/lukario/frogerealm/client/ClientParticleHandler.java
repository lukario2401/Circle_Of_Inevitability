package net.lukario.frogerealm.client;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.particles.ModParticles;
import net.lukario.frogerealm.particles.VoidRiftParticle;
import net.lukario.frogerealm.particles.fx.ModelFxParticle;
import net.lukario.frogerealm.particles.fx.ModelFxRenderer;
import net.lukario.frogerealm.particles.fx.ParticleFxParticle;
import net.lukario.frogerealm.particles.fx.SlashParticle;
import net.lukario.frogerealm.particles.fx.SlashRenderTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ForgeRealm.MOD_ID,
        value = Dist.CLIENT
)
public class ClientParticleHandler {

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        // 26.2: slashes and models are drawn by their own particle groups (fired once, on the main thread, from Minecraft's constructor)
        SlashRenderTypes.register();
        ModelFxRenderer.register();

        event.registerSpriteSet(
                ModParticles.VOID_RIFT.get(),
                VoidRiftParticle.Provider::new
        );

        // all ParticleFx particles (no JSON needed: textures come straight from textures/particle/)
        // (26.2: ParticleProvider.createParticle also gets a RandomSource, which these don't use)
        event.registerSpecial(ModParticles.FX.get(),
                (fx, level, x, y, z, xd, yd, zd, random) -> ParticleFxParticle.create(fx, level, x, y, z, xd, yd, zd));
        event.registerSpecial(ModParticles.SLASH.get(),
                (fx, level, x, y, z, xd, yd, zd, random) -> SlashParticle.create(fx, level, x, y, z, xd, yd, zd));
        event.registerSpecial(ModParticles.MODEL.get(),
                (fx, level, x, y, z, xd, yd, zd, random) -> ModelFxParticle.create(fx, level, x, y, z, xd, yd, zd));
    }
}
