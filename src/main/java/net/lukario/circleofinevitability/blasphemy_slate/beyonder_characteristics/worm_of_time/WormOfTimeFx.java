package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time;

import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    private static final SlashFx CUT = SlashFx.arc("slash/band")
            .color(0xDC53CDE0).tailColor(0x70312E62).headColor(0xFFF0FFFA)
            .core(0xA0DFFFFF).radius(3.2f).arc(190).width(0.65f)
            .taper(SlashFx.Taper.CRESCENT).layers(3).spread(0.16f)
            .lifetime(11).sweep(4);
    private static final SlashFx SCAR = SlashFx.line("slash/crystal")
            .color(0xD080DDE7).core(0xF0F1FFF5).width(0.14f)
            .taper(SlashFx.Taper.CRESCENT).lifetime(7).sweep(1).fromCenter();
    private static final SlashFx SPEAR = SlashFx.line("slash/crystal")
            .color(0xD061D8DC).tailColor(0x50463B80).headColor(0xFFF5FFEB)
            .core(0xE0FFFFFF).width(0.24f).taper(SlashFx.Taper.COMET)
            .layers(2).spread(0.08f).lifetime(10).sweep(3);
    private static final SlashFx DIAL = SlashFx.arc("slash/smooth")
            .color(0x9052B9BA).headColor(0xD0CEFFFF).radius(0.6f)
            .arc(300).width(0.035f).lifetime(8).sweep(2);
    private static final ParticleFx HIT_SPARK = MOTE.size(0.065f).endSize(0.01f).lifetime(8, 2);
    private WormOfTimeFx() {}

    public static void openingCut(ServerPlayer p, LivingEntity t, ServerLevel sl) { cut(p, t, sl, -24, false); }
    public static void returnCut(ServerPlayer p, LivingEntity t, ServerLevel sl) { cut(p, t, sl, 28, true); }
    public static void risingCut(ServerPlayer p, LivingEntity t, ServerLevel sl) { cut(p, t, sl, -62, false); }
    public static void fallingCut(ServerPlayer p, LivingEntity t, ServerLevel sl) { cut(p, t, sl, 68, true); }

    // Orient toward the actual victim, even when the attack catches a mob off the player's crosshair.
    private static float hitYaw(ServerPlayer p, LivingEntity t) {
        Vec3 direction = t.position().subtract(p.position());
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }

    private static float hitScale(LivingEntity t) {
        return (float) Math.clamp(Math.max(t.getBbWidth() * 0.85, t.getBbHeight() * 0.45), 0.45, 1.35);
    }

    private static void cut(ServerPlayer p, LivingEntity t, ServerLevel sl, float roll, boolean reverse) {
        Vec3 hit = t.getBoundingBox().getCenter();
        float scale = hitScale(t);
        float yaw = hitYaw(p, t);
        float tilt = roll + (p.getRandom().nextFloat() - 0.5f) * 10;
        float radius = 3.0f + p.getRandom().nextFloat() * 0.5f;
        SlashFx slash = CUT.radius(radius).arc(175 + p.getRandom().nextFloat() * 30);
        if (reverse) slash = slash.mirrored();
        // Broad half circle around the attacker, aimed toward the struck mob.
        Vec3 center = p.position().add(0, p.getBbHeight() * 0.55, 0);
        ParticleShapes.slash(sl, slash, center, yaw, 0, tilt);
        ParticleShapes.slash(sl, slash.radius(radius - 0.3f).layers(1).width(0.12f)
                .color(0x806BA5E0).core(0).delay(2), center, yaw, 0, tilt + 7);
        ParticleShapes.slash(sl, SCAR.radius(scale * 0.75f),
                hit, yaw, 0, tilt);
        ParticleShapes.burst(sl, HIT_SPARK, hit, 7, 0.045, 0.12);
        Spells.sound(sl, hit, SoundEvents.PLAYER_ATTACK_SWEEP, 0.55f, reverse ? 1.35f : 1.15f);
        Spells.sound(sl, hit, SoundEvents.AMETHYST_BLOCK_HIT, 0.3f, 1.7f);
    }

    public static void crossCut(ServerPlayer p, LivingEntity t, ServerLevel sl) {
        cut(p, t, sl, -42, false);
        diagonalCross(p, t, sl);
        dial(p, t, sl, false);
    }

    private static void diagonalCross(ServerPlayer p, LivingEntity t, ServerLevel sl) {
        float radius = Math.max(1.7f, hitScale(t) * 1.6f);
        float turn = (p.getRandom().nextFloat() - 0.5f) * 16;
        Vec3 hit = t.getBoundingBox().getCenter();
        for (int i = 0; i < 2; i++) {
            float roll = (i == 0 ? -43 : 43) + turn;
            ParticleShapes.slash(sl, SCAR.radius(radius).width(0.28f).layers(2).spread(0.10f)
                    .lifetime(12).sweep(2).delay(i * 2), hit, hitYaw(p, t), 0, roll);
        }
    }

    /** A long thrust with a split spearhead, from the attacker's hand through the victim. */
    public static void spearThrust(ServerPlayer p, LivingEntity t, ServerLevel sl) {
        Vec3 from = p.position().add(0, p.getBbHeight() * 0.65, 0);
        Vec3 hit = t.getBoundingBox().getCenter();
        Vec3 direction = hit.subtract(from).normalize();
        if (direction.lengthSqr() < 0.001) direction = p.getLookAngle();
        Vec3 tip = hit.add(direction.scale(1.5));
        ParticleShapes.slashBetween(sl, SPEAR, from, tip);
        Vec3 side = direction.cross(new Vec3(0, 1, 0)).normalize();
        if (side.lengthSqr() < 0.001) side = new Vec3(1, 0, 0);
        Vec3 shoulder = tip.subtract(direction.scale(1.1));
        for (int sign : new int[]{-1, 1}) {
            ParticleShapes.slashBetween(sl, SPEAR.width(0.18f).layers(1).delay(1),
                    shoulder.add(side.scale(sign * 0.5)), tip);
        }
        ParticleShapes.burst(sl, HIT_SPARK, hit, 12, 0.07, 0.18);
        Spells.sound(sl, hit, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.7f, 1.3f);
        Spells.sound(sl, hit, SoundEvents.AMETHYST_BLOCK_HIT, 0.4f, 1.8f);
    }

    /** A broken clock face contracts into the hit instead of obscuring the fight with a vortex. */
    public static void clockBreak(ServerPlayer p, LivingEntity t, ServerLevel sl) {
        cut(p, t, sl, 80, true);
        dial(p, t, sl, true);
        ParticleShapes.slash(sl, DIAL.radius(hitScale(t) * 1.25f).endRadius(0.12f)
                .arc(-330).delay(2).lifetime(10), t.getBoundingBox().getCenter(), hitYaw(p, t), -90, 0);
        Spells.sound(sl, t.position(), SoundEvents.AMETHYST_CLUSTER_BREAK, 0.65f, 1.25f);
    }

    /** Three precise afterimage cuts; particle delay keeps the whole effect at the original impact. */
    public static void timeFracture(ServerPlayer p, LivingEntity t, ServerLevel sl) {
        cut(p, t, sl, -35, false);
        diagonalCross(p, t, sl);
        Vec3 hit = t.getBoundingBox().getCenter();
        for (int i = 0; i < 3; i++) {
            ParticleShapes.slash(sl, SCAR.radius(1.9f - i * 0.15f)
                    .width(0.18f - i * 0.025f).delay(2 + i * 2),
                    hit.add(0, (i - 1) * 0.12, 0), hitYaw(p, t), 0, 40 + i * 55);
        }
        dial(p, t, sl, true);
        Spells.sound(sl, hit, SoundEvents.PLAYER_ATTACK_CRIT, 0.6f, 1.35f);
    }

    private static void dial(ServerPlayer p, LivingEntity t, ServerLevel sl, boolean finisher) {
        Vec3 hit = t.getBoundingBox().getCenter();
        float scale = hitScale(t);
        float yaw = hitYaw(p, t);
        ParticleShapes.slash(sl, DIAL.radius(scale * 0.8f), hit, yaw, -90, 0);
        // Twelve clean hour ticks, with two longer hands. All are local to the impact plane.
        Vec3 right = new Vec3(Math.cos(Math.toRadians(yaw)), 0, Math.sin(Math.toRadians(yaw)));
        int ticks = finisher ? 12 : 6;
        for (int i = 0; i < ticks; i++) {
            double angle = i * Math.PI * 2 / ticks;
            Vec3 at = hit.add(right.scale(Math.cos(angle) * scale * 0.8))
                    .add(0, Math.sin(angle) * scale * 0.8, 0);
            ParticleShapes.slash(sl, SCAR.radius(scale * 0.065f).width(0.035f).lifetime(6),
                    at, yaw, 0, (float) Math.toDegrees(angle));
        }
        ParticleShapes.slash(sl, SCAR.radius(scale * 0.4f).width(0.045f).delay(1), hit, yaw, 0, 75);
        ParticleShapes.slash(sl, SCAR.radius(scale * 0.27f).width(0.045f).delay(1), hit, yaw, 0, -20);
    }

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
