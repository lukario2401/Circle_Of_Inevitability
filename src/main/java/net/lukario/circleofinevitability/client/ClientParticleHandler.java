package net.lukario.circleofinevitability.client;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.particles.ModParticles;
import net.lukario.circleofinevitability.particles.VoidRiftParticle;
import net.lukario.circleofinevitability.particles.fx.DamageNumberParticle;
import net.lukario.circleofinevitability.particles.fx.DamageNumberRenderer;
import net.lukario.circleofinevitability.particles.fx.ModelFxParticle;
import net.lukario.circleofinevitability.particles.fx.ModelFxRenderer;
import net.lukario.circleofinevitability.particles.fx.ParticleFxParticle;
import net.lukario.circleofinevitability.particles.fx.SlashParticle;
import net.lukario.circleofinevitability.particles.fx.SlashRenderTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = CircleOfInevitability.MOD_ID,
        value = Dist.CLIENT
)
public class ClientParticleHandler {

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        // 26.2: slashes, models and damage numbers are drawn by their own particle groups (fired once, on the main thread, from Minecraft's constructor)
        SlashRenderTypes.register();
        ModelFxRenderer.register();
        DamageNumberRenderer.register();

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
        // the numbers of /damagefx
        event.registerSpecial(ModParticles.DAMAGE_NUMBER.get(),
                (number, level, x, y, z, xd, yd, zd, random) -> DamageNumberParticle.create(number, level, x, y, z, xd, yd, zd));
    }
}
