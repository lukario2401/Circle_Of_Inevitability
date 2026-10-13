package net.lukario.circleofinevitability.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Reel: an entity is drawn along a line to a point, fast and straight, the way a grappling line or a whip pulls.
 * A player swinging to a ledge, a mob yanked in, everyone tangled dragged to the middle of a web:
 *
 *   Reel.in(player, () -> anchor)            // the point it is drawn to, asked again every tick (null = let go)
 *           .speed(1.6)                      // blocks per tick at most (default 1.4)
 *           .stopAt(1.5)                     // it has arrived this close to the point (default 1.2)
 *           .lasts(30)                       // and lets go after this long anyway (default 40)
 *           .onArrive(() -> ...)             // it got there (not when time ran out or the line was let go)
 *           .start(sl);
 *
 *   Reel.in(mob, () -> SpellFx.middleOf(player, sl))     // to a moving point: follows it
 *
 * It goes straight at the point, up and down too, slowing a little as it gets close, and keeps
 * no fall damage while drawn (the fall counter is reset every tick). It does not go through walls: what is in
 * the way stops it there, and it lets go once it has been stuck for a few ticks.
 * It also stops when the entity dies or leaves the level. Not saved.
 *
 * Spells.pull is the soft, sideways-only version for a steady drag; FallGuard.protect for the landing after.
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Reel {

    /** Ticks without moving after which it is taken as stuck on something and lets go (a player's own game lags a little). */
    public static int STUCK_TICKS = 6;

    private final LivingEntity entity;
    private final Supplier<Vec3> point;
    private double speed = 1.4;
    private double stopAt = 1.2;
    private int lasts = 40;
    private Runnable onArrive;
    private Runnable onEnd;

    private Reel(LivingEntity entity, Supplier<Vec3> point) {
        this.entity = entity;
        this.point = point;
    }

    /** Draws 'entity' to the point 'point' gives (asked every tick; null = let go). Nothing happens until start. */
    public static Reel in(LivingEntity entity, Supplier<Vec3> point) {
        return new Reel(entity, point);
    }

    /** Blocks per tick at most. */
    public Reel speed(double blocksPerTick) {
        this.speed = Math.max(0.05, blocksPerTick);
        return this;
    }

    /** It has arrived when its feet are this close to the point. */
    public Reel stopAt(double blocks) {
        this.stopAt = Math.max(0.1, blocks);
        return this;
    }

    /** Ticks it is drawn at most. */
    public Reel lasts(int ticks) {
        this.lasts = Math.max(1, ticks);
        return this;
    }

    /** Runs once it has arrived. */
    public Reel onArrive(Runnable action) {
        this.onArrive = action;
        return this;
    }

    /** Runs when it is over, however it ended (after onArrive). */
    public Reel onEnd(Runnable action) {
        this.onEnd = action;
        return this;
    }

    public void start(ServerLevel sl) {
        step(sl, 0, entity.position(), 0);
    }

    private void step(ServerLevel sl, int tick, Vec3 last, int stuck) {
        if (!entity.isAlive() || entity.level() != sl) {
            end(false);
            return;
        }
        Vec3 to = point.get();
        if (to == null || tick >= lasts || stuck >= STUCK_TICKS) {
            end(false);
            return;
        }
        Vec3 path = to.subtract(entity.position());
        double distance = path.length();
        if (distance <= stopAt) {
            entity.setDeltaMovement(entity.getDeltaMovement().scale(0.2));     // no flying on past the point
            entity.hurtMarked = true;
            entity.fallDistance = 0;
            end(true);
            return;
        }
        double now = Math.min(speed, distance * 0.5 + 0.2);
        entity.setDeltaMovement(path.scale(now / distance));
        entity.hurtMarked = true;                                              // makes players' clients accept it
        entity.fallDistance = 0;
        int stillStuck = tick > 0 && entity.position().distanceToSqr(last) < 0.0025 ? stuck + 1 : 0;
        Vec3 here = entity.position();
        Later.run(sl, 1, () -> step(sl, tick + 1, here, stillStuck));
    }

    private void end(boolean arrived) {
        if (arrived && onArrive != null) onArrive.run();
        if (onEnd != null) onEnd.run();
    }
}
