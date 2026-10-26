package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time;

import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Error's cyan-black clockwork. Visuals are separate from the powers and use bounded particle counts. */
public final class WormOfTimeFx {
    public static final int CYAN = 0xFF72E8DC;
    private static final ParticleFx MOTE = ParticleFx.of("fx/spark").color(CYAN).fadeOut()
            .size(0.09f).endSize(0.02f).lifetime(16, 5).glow();
    private static final SlashFx THREAD = SlashFx.line("slash/smooth").color(0xC052E3C4)
            .core(0xD0FFFFFF).width(0.07f).lifetime(10).sweep(3);
    private WormOfTimeFx() {}

    public static void steal(ServerLevel sl, LivingEntity thief, LivingEntity target) {
        ParticleShapes.slashBetween(sl, THREAD, target.getBoundingBox().getCenter(), thief.getBoundingBox().getCenter());
        burst(sl, target.position().add(0, 1, 0));
        Spells.sound(sl, thief.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.5f);
    }

    public static void burst(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, MOTE, at, 12, 0.04, 0.15);
    }

    public static void clock(ServerLevel sl, Vec3 at, float scale, int ticks, boolean reverse) {
        ModelFx face = ModelFx.of("worm_of_time/ancient_clock").pivot(8, 8, 8)
                .color(0xD090CCC4).scale(scale).glow().unshaded().lifetime(ticks).fade(4, 8);
        ParticleShapes.model(sl, face, at, 0, 0, 0);
        ModelFx hand = ModelFx.of("worm_of_time/clock_hand").pivot(8, 8, 8)
                .color(0xFFF0FFFF).scale(scale).glow().unshaded().lifetime(ticks).fade(4, 8)
                .during(0, 0, ModelFx.pose().roll(0))
                .during(0, ticks, ModelFx.pose().roll(reverse ? -300 : 300), ModelFx.Ease.LINEAR);
        ParticleShapes.model(sl, hand, at.add(0, 0, -0.03), 0, 0, 0);
        ParticleShapes.ring(sl, MOTE, at.add(0, -0.8, 0), scale, 24, 0);
        Spells.sound(sl, at, SoundEvents.BEACON_POWER_SELECT, 0.7f, reverse ? 0.6f : 1.2f);
    }

    public static void avatar(ServerLevel sl, LivingEntity avatar, int ticks) {
        ParticleShapes.modelOn(sl, ModelFx.of("worm_of_time/avatar").pivot(8, 0, 8)
                .color(0xB070DED0).scale(1f).glow().unshaded().lifetime(ticks).fade(5, 12),
                avatar, Vec3.ZERO, 0, 0, 0);
        burst(sl, avatar.position().add(0, 1, 0));
    }
}
