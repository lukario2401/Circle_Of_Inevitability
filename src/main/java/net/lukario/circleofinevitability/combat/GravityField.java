package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Gravity;
import net.lukario.circleofinevitability.status.Weight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * A gravity field: a round piece of ground (or an aura round someone) where gravity is not what it should be. It can
 * pull what is in it toward its middle, press it down (status/Weight), lift it off the ground and hold it there, or
 * slow it. Built on Zone, so it has its beat, its time, its owner and its name like every zone.
 *
 *   GravityField.at(ground, 8)                // on the ground (GravityField.around(player, 16): it goes with them)
 *           .lasts(100).every(5)              // 5 seconds, it acts four times a second
 *           .presses(2)                       // those in it are Heavy, level 2 (status/Weight), while they stay
 *           .lifts(3.0)                       // or: lifted 3 blocks off the ground and held there
 *           .pulls(0.25)                      // or/and: drawn toward the middle, 0.25 blocks a tick at most
 *           .slows(2)                         // or/and: Slowness III (amplifier 2) while inside
 *           .bossesOnlySlowed(one -> big)     // these are never lifted or pulled, only slowed
 *           .onInside((one, ticksInside) -> ...)   // anything else, every beat, for each one in it
 *           .onPulse(ticksLeft -> MyFx.tick(...))  // its look, every beat
 *           .onEnd(() -> MyFx.end(...))
 *           .raise(player, sl, "my_pathway_gravity");
 *
 *   GravityField.isRaised(player, name)   GravityField.close(player, name)   (Zone.ticksLeft, Zone.covers... work too)
 *
 * Who it acts on: the owner's enemies, or .affects(...). Allies and the owner are never pulled, pressed or lifted.
 * Pulls go through Spells.pull, so whatever cannot be moved (Spells.addMoveImmunity) stays where it is. A lifted
 * one is put down gently when the field ends or it leaves (no fall damage from the lift itself). Build a new one for
 * every cast. Star Dragon's Gravity Abnormality, Heavybringing, Time Dilation, the Neutron Star and the Singularity
 * (docs/STAR_DRAGON.md).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class GravityField {

    private static final String LIFT = "circle_of_inevitability_field_lift";   // its status/Gravity reason while held up

    private final Zone zone;
    private final boolean aura;
    private final Entity follows;
    private final Vec3 ground;
    private double radius;
    private int beat = 5;
    private int pressLevel;
    private double liftHeight;
    private double pullSpeed;
    private int slowAmplifier = -1;
    private Predicate<LivingEntity> onlySlowed = one -> false;
    private java.util.function.ToIntFunction<LivingEntity> liftFor = one -> Integer.MAX_VALUE;
    private Zone.Inside inside;
    private java.util.function.Consumer<LivingEntity> leave;
    private double slam;

    private GravityField(Zone zone, Vec3 ground, Entity follows, double radius) {
        this.zone = zone.every(beat);
        this.ground = ground;
        this.follows = follows;
        this.aura = follows != null;
        this.radius = radius;
    }

    /** A field lying on the ground, its middle at 'ground'. */
    public static GravityField at(Vec3 ground, double radius) {
        return new GravityField(Zone.at(ground, radius), ground, null, radius);
    }

    /** A field round an entity, its middle at that entity's feet wherever they go. */
    public static GravityField around(Entity entity, double radius) {
        return new GravityField(Zone.around(entity, radius), null, entity, radius);
    }

    public GravityField height(double blocks) { zone.height(blocks); return this; }

    public GravityField lasts(int ticks) { zone.lasts(ticks); return this; }

    /** How often it acts, in ticks (default 5). Lifting and pulling look smoothest at 2 to 5. */
    public GravityField every(int ticks) { beat = Math.max(1, ticks); zone.every(beat); return this; }

    /** Who it acts on, instead of the owner's enemies (the owner never). */
    public GravityField affects(Predicate<LivingEntity> who) { zone.affects(who); return this; }

    /** Those in it are Heavy (status/Weight) at this level while they stay, and a moment after. */
    public GravityField presses(int level) { pressLevel = Math.max(0, level); return this; }

    /** Those in it are lifted this many blocks off the ground and held there while they stay. */
    public GravityField lifts(double blocks) { liftHeight = Math.max(0, blocks); return this; }

    /** How long each one is held up at most, in ticks spent inside (after that it drops): Spells::holdLength-style rules. */
    public GravityField liftsFor(java.util.function.ToIntFunction<LivingEntity> ticks) { liftFor = ticks; return this; }

    /** Those in it are drawn toward the middle, at most this many blocks a tick. */
    public GravityField pulls(double speed) { pullSpeed = Math.max(0, speed); return this; }

    /** Those in it have Slowness at this amplifier (0 = I) while they stay. */
    public GravityField slows(int amplifier) { slowAmplifier = amplifier; return this; }

    /** These are never lifted or pulled, only slowed (Slowness II at the least): bosses, in most Pathways. */
    public GravityField bossesOnlySlowed(Predicate<LivingEntity> big) { onlySlowed = big; return this; }

    /** More, every beat, for each one in it (after the field has acted on them). */
    public GravityField onInside(Zone.Inside action) { inside = action; return this; }

    /** A lifted one is thrown down at this speed (blocks a tick) when it leaves or the field ends, instead of being set
     *  down gently: its fall hurts as falls do (and more if it is Heavy). */
    public GravityField slamsDown(double speed) { slam = Math.max(0, speed); return this; }

    public GravityField onEnter(java.util.function.Consumer<LivingEntity> action) { zone.onEnter(action); return this; }

    /** Someone left it, or it ended with them in it (after a lifted one was put down or slammed). */
    public GravityField onLeave(java.util.function.Consumer<LivingEntity> action) { leave = action; return this; }

    public GravityField onPulse(IntConsumer action) { zone.onPulse(action); return this; }

    public GravityField onEnd(Runnable action) { zone.onEnd(action); return this; }

    /** Raises it. One of the same player and name closes first. */
    public void raise(Player owner, ServerLevel sl, String name) {
        int hold = beat + 2;
        zone.onInside((one, ticksInside) -> {
            boolean big = onlySlowed.test(one);
            if (big) {
                Spells.keepEffect(one, MobEffects.SLOWNESS, Math.max(1, slowAmplifier), hold + 10);
            } else {
                if (pressLevel > 0) Weight.put(one, pressLevel, hold);
                if (slowAmplifier >= 0) Spells.keepEffect(one, MobEffects.SLOWNESS, slowAmplifier, hold + 10);
                Vec3 middle = middle(owner, name);
                if (pullSpeed > 0 && middle != null) Spells.pull(one, middle, pullSpeed, 0.0);
                if (liftHeight > 0 && ticksInside < liftFor.applyAsInt(one) && !Spells.cannotBeMoved(one)) holdUp(owner, sl, one, hold);
            }
            if (inside != null) inside.run(one, ticksInside);
        });
        zone.onLeave(one -> {
            if (liftHeight > 0 && !onlySlowed.test(one)) putDown(one);
            if (leave != null) leave.accept(one);
        });
        zone.open(owner, sl, name);
    }

    public static boolean isRaised(Player owner, String name) {
        return Zone.isOpen(owner, name);
    }

    public static void close(Player owner, String name) {
        Zone.close(owner, name);
    }

    // =========================
    // Internals
    // =========================

    private Vec3 middle(Player owner, String name) {
        if (aura) return follows.isAlive() ? follows.position() : null;
        Vec3 now = Zone.centerOf(owner, name);
        return now != null ? now : ground;
    }

    /** Up to liftHeight over the ground under it, then held there without gravity. */
    private void holdUp(Player owner, ServerLevel sl, LivingEntity one, int hold) {
        Vec3 floor = Spells.groundUnder(owner, sl, one);
        double wanted = floor.y + liftHeight;
        double gap = wanted - one.getY();
        Gravity.set(one, LIFT, 0.0, hold);
        double up = gap > 0.15 ? Math.min(0.35, gap * 0.3) : gap < -0.3 ? -0.08 : 0.0;
        one.setDeltaMovement(0, up, 0);
        one.hurtMarked = true;
        one.fallDistance = 0;
    }

    /** Lifted no more: gravity back, and the drop from the lift itself does not hurt (or it is slammed down). */
    private void putDown(LivingEntity one) {
        Gravity.clear(one, LIFT);
        if (slam > 0) {
            one.setDeltaMovement(0, -slam, 0);
            one.hurtMarked = true;
            return;
        }
        FallGuard.protect(one, 60);
    }
}
