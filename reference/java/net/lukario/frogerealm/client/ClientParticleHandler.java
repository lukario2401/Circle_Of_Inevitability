package net.lukario.frogerealm.client;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.particles.ModParticles;
import net.lukario.frogerealm.particles.VoidRiftParticle;
import net.lukario.frogerealm.particles.fx.ModelFxParticle;
import net.lukario.frogerealm.particles.fx.ParticleFxParticle;
import net.lukario.frogerealm.particles.fx.SlashParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ForgeRealm.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ClientParticleHandler {

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                ModParticles.VOID_RIFT.get(),
                VoidRiftParticle.Provider::new
        );

        // all ParticleFx particles (no JSON needed: textures come straight from textures/particle/)
        event.registerSpecial(ModParticles.FX.get(), ParticleFxParticle::create);
        event.registerSpecial(ModParticles.SLASH.get(), SlashParticle::create);
        event.registerSpecial(ModParticles.MODEL.get(), ModelFxParticle::create);
    }
}
