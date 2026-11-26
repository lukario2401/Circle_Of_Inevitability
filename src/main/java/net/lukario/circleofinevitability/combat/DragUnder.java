package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pathway_fx.PaleEmperorFx;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ModelFx.Ease;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Burial;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * DragUnder: arms of the dead break out of the ground round someone, seize them, drag them under and crush them
 * there; whoever survives is thrown back out of the ground. A finisher for marks, a grave that opens, a hand from the
 * underworld. Define it once and drag whoever you like:
 *
 *   static final DragUnder CLAIM = DragUnder.of(30f)      // 30 damage under the ground, in three crushes
 *           .sink(14).hold(36)                            // goes down in 0.7 s, stays down 1.8 s
 *           .throwsUp(1.0)                                // and is flung out at this speed if it lives
 *           .arms(4);                                     // how many arms come for it
 *
 *   if (CLAIM.drag(player, enemy)) { ... }                // false: it is being dragged already, or cannot be
 *
 * The damage is the owner's (they get the kill), lands through Domes and Substitutes, and is split into crushes
 * while it is under. Whatever dies down there leaves its loot on the surface (status/Burial puts it back first).
 * Something too big to bury (Burial.MAX_HEIGHT/MAX_WIDTH) is still seized and crushed where it stands, and not
 * thrown. Uses the Pale Emperor's underworld arms (PaleEmperorFx.UNDERWORLD_ARM).
 */
public record DragUnder(float damage, int sinkTicks, int holdTicks, double throwSpeed, int arms, int crushes) {
    /** The tick the arms close on it, after they burst out of the ground. */
    public static final int SEIZE = 10;
    private static final double ARM_DEPTH = 3.4;
    private static final Set<UUID> DRAGGING = new HashSet<>();

    public DragUnder {
        if (!Float.isFinite(damage) || damage < 0 || sinkTicks < 1 || holdTicks < 1 || !Double.isFinite(throwSpeed)
                || arms < 0 || crushes < 1)
            throw new IllegalArgumentException("Invalid drag");
    }

    public static DragUnder of(float damage) { return new DragUnder(damage, 14, 36, 1.0, 4, 3); }
    public DragUnder sink(int ticks) { return new DragUnder(damage, ticks, holdTicks, throwSpeed, arms, crushes); }
    public DragUnder hold(int ticks) { return new DragUnder(damage, sinkTicks, ticks, throwSpeed, arms, crushes); }
    public DragUnder throwsUp(double speed) { return new DragUnder(damage, sinkTicks, holdTicks, speed, arms, crushes); }
    public DragUnder arms(int count) { return new DragUnder(damage, sinkTicks, holdTicks, throwSpeed, count, crushes); }
    public DragUnder crushes(int count) { return new DragUnder(damage, sinkTicks, holdTicks, throwSpeed, arms, count); }

    /** Ticks from the drag starting to the target being back on the surface. */
    public int totalTicks() { return SEIZE + sinkTicks + holdTicks; }

    public static boolean isDragging(LivingEntity target) { return DRAGGING.contains(target.getUUID()); }

    /** Starts the drag. False if the target is already being dragged or buried, or is not alive. */
    public boolean drag(LivingEntity owner, LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level) || !target.isAlive() || Burial.isBuried(target)
                || !DRAGGING.add(target.getUUID()))
            return false;
        Vec3 ground = target.position();
        Root.apply(target, SEIZE + 2, RootRestriction.EVERYTHING);
        armsRise(level, ground, target.getBbWidth());
        level.playSound(null, target.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, .7f, .55f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(target.blockPosition().below())),
                ground.x, ground.y + .1, ground.z, 30, .6, .05, .6, .15);

        boolean buried = Burial.canBury(target);
        Later.run(level, SEIZE, () -> {
            if (!target.isAlive() || target.level() != level) { DRAGGING.remove(target.getUUID()); return; }
            level.playSound(null, target.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.2f, .6f);
            if (buried && Burial.bury(target, sinkTicks, holdTicks + 40, ARM_DEPTH)) return;
            Root.apply(target, sinkTicks + holdTicks, RootRestriction.EVERYTHING);   // too big: held where it stands
        });
        for (int i = 0; i < crushes; i++) {
            int at = SEIZE + sinkTicks + (holdTicks - 4) * i / crushes;
            Later.run(level, at, () -> crush(owner, target, level));
        }
        Later.run(level, totalTicks(), () -> {
            DRAGGING.remove(target.getUUID());
            if (!target.isAlive() || target.level() != level) return;
            boolean wasUnder = Burial.isBuried(target);
            Burial.release(target);
            Root.remove(target);
            if (wasUnder) thrown(target, level);
        });
        return true;
    }

    private void crush(LivingEntity owner, LivingEntity target, ServerLevel level) {
        if (!target.isAlive() || target.level() != level) return;
        Vec3 at = Burial.isBuried(target) ? target.position().add(0, target.getBbHeight() + .4, 0) : target.position();
        level.sendParticles(ParticleTypes.SCULK_SOUL, at.x, at.y + .3, at.z, 6, .4, .2, .4, .03);
        level.playSound(null, target.blockPosition(), SoundEvents.SKELETON_HURT, SoundSource.HOSTILE, .9f, .5f);
        Spells.strikeThrough(owner, target, damage / crushes);
    }

    /** Back on the surface and flung up and a little away, with a burst of soil and souls. */
    private void thrown(LivingEntity target, ServerLevel level) {
        double angle = target.getRandom().nextDouble() * Math.PI * 2;
        target.setDeltaMovement(Math.cos(angle) * .22, throwSpeed, Math.sin(angle) * .22);
        target.hurtMarked = true;
        Vec3 at = target.position();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(target.blockPosition().below())),
                at.x, at.y + .2, at.z, 40, .5, .2, .5, .25);
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + .5, at.z, 14, .4, .4, .4, .06);
        level.playSound(null, target.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.2f, .7f);
    }

    /** Arms burst out in a ring, close on the middle, and sink with what they hold, as fast as Burial pulls it. */
    private void armsRise(ServerLevel level, Vec3 ground, float width) {
        if (arms <= 0) return;
        double radius = Math.max(.9, width * .5 + .5);
        ModelFx arm = PaleEmperorFx.UNDERWORLD_ARM.noKeys().scale(.8f).lifetime(SEIZE + sinkTicks + 8).fade(0, 4)
                .key(0, ModelFx.pose().frame(3).up(-2.7f))
                .key(7, ModelFx.pose().up(0), Ease.OUT)                         // bursts out
                .during(2, 7, ModelFx.pose().frame(0), Ease.OUT)                // fingers spread
                .during(SEIZE - 3, SEIZE, ModelFx.pose().frame(4), Ease.IN)     // and close on it
                .during(SEIZE, SEIZE + sinkTicks, ModelFx.pose().up((float)-ARM_DEPTH), Ease.IN);
        float start = level.getRandom().nextFloat() * 360f;
        for (int i = 0; i < arms; i++) {
            float around = start + i * 360f / arms;
            double rad = Math.toRadians(around);
            Vec3 at = ground.add(-Math.sin(rad) * radius, 0, Math.cos(rad) * radius);
            ParticleShapes.model(level, i % 2 == 0 ? arm : arm.mirrored(), at, around + 180f, 22f, 0f);
        }
    }
}
