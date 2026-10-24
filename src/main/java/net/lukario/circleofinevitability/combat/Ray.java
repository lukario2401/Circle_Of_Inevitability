package net.lukario.circleofinevitability.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A ray: an instant beam from a point along a direction, at the speed of light. It cannot be dodged once it fires,
 * so it can show where it will go first (a warning line) and fire a few ticks later along that same line. Shot is
 * for things that fly and can be seen coming; a Ray is there at once.
 *
 *   Ray.from(eye, player.getLookAngle())
 *           .range(30).width(0.3)                 // how far, and how fat its line is for hitting someone
 *           .warning(8)                           // the aim shows for 8 ticks, then it fires (0 = at once)
 *           .pierces()                            // through everyone on its line (default: the first enemy only)
 *           .throughBlocks()                      // walls do not stop it (default: the first block does)
 *           .onWarn((from, to) -> MyFx.aim(sl, from, to))
 *           .onFire((from, to) -> MyFx.beam(sl, from, to))      // its look, the moment it fires
 *           .onHit((target, at) -> Spells.strike(player, target, 40f))
 *           .onEnd(end -> ...)                    // where it stopped (the block, the one it hit, or its range)
 *           .fire(player, sl);                    // returns the ticks until it fires
 *
 * The line is fixed when it is cast (where the warning showed); who it hits is worked out when it fires, so someone
 * who steps off the line in time is missed, and someone who steps onto it is hit. Nothing happens if the caster
 * died, left or changed dimension meanwhile. Every method returns a new copy, so a Ray in a constant is safe:
 * keep the parts that never change there and add the rest at the cast. Star Dragon's Weakening, Piercing, Sunlight
 * and Series of Rays (docs/STAR_DRAGON.md); the Dragon's thicker rays are planned on it.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Ray {

    private Vec3 from;
    private Vec3 direction;
    private double range = 24.0;
    private double width = 0.3;
    private int warning;
    private boolean pierces;
    private boolean throughBlocks;
    private BiConsumer<Vec3, Vec3> onWarn;
    private BiConsumer<Vec3, Vec3> onFire;
    private BiConsumer<LivingEntity, Vec3> onHit;
    private Consumer<Vec3> onEnd;

    private Ray() {}

    /** A ray from 'from' along 'direction' (any length; it is made a unit vector). */
    public static Ray from(Vec3 from, Vec3 direction) {
        Ray ray = new Ray();
        ray.from = from;
        ray.direction = direction.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : direction.normalize();
        return ray;
    }

    private Ray copy() {
        Ray c = new Ray();
        c.from = from;
        c.direction = direction;
        c.range = range;
        c.width = width;
        c.warning = warning;
        c.pierces = pierces;
        c.throughBlocks = throughBlocks;
        c.onWarn = onWarn;
        c.onFire = onFire;
        c.onHit = onHit;
        c.onEnd = onEnd;
        return c;
    }

    public Ray range(double blocks) { Ray c = copy(); c.range = Math.max(0.5, blocks); return c; }

    /** How fat its line is for hitting (0.3 is a thin ray; a 2 block wide beam is width 1). */
    public Ray width(double blocks) { Ray c = copy(); c.width = Math.max(0, blocks); return c; }

    /** Ticks its aim shows before it fires. */
    public Ray warning(int ticks) { Ray c = copy(); c.warning = Math.max(0, ticks); return c; }

    /** Through every enemy on its line, each hit once. */
    public Ray pierces() { Ray c = copy(); c.pierces = true; return c; }

    /** Blocks do not stop it. */
    public Ray throughBlocks() { Ray c = copy(); c.throughBlocks = true; return c; }

    /** Another start or direction for the same ray (a fan: the same Ray turned with Spells.turned). */
    public Ray along(Vec3 newFrom, Vec3 newDirection) {
        Ray c = copy();
        c.from = newFrom;
        c.direction = newDirection.normalize();
        return c;
    }

    public Ray onWarn(BiConsumer<Vec3, Vec3> action) { Ray c = copy(); c.onWarn = action; return c; }

    public Ray onFire(BiConsumer<Vec3, Vec3> action) { Ray c = copy(); c.onFire = action; return c; }

    public Ray onHit(BiConsumer<LivingEntity, Vec3> action) { Ray c = copy(); c.onHit = action; return c; }

    public Ray onEnd(Consumer<Vec3> action) { Ray c = copy(); c.onEnd = action; return c; }

    /** Where its line would end now: the first block on it (unless throughBlocks), or its range. */
    public Vec3 end(Player caster, ServerLevel sl) {
        Vec3 far = from.add(direction.scale(range));
        if (throughBlocks) return far;
        return sl.clip(new ClipContext(from, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
    }

    /** Fires it (after its warning). Returns the ticks until it fires. */
    public int fire(Player caster, ServerLevel sl) {
        Vec3 to = end(caster, sl);
        if (warning > 0 && onWarn != null) onWarn.accept(from, to);
        if (warning <= 0) {
            go(caster, sl, to);
        } else {
            Later.run(sl, warning, () -> {
                if (Spells.casterStillHere(caster, sl)) go(caster, sl, end(caster, sl));
            });
        }
        return warning;
    }

    private void go(Player caster, ServerLevel sl, Vec3 to) {
        Vec3 stop = to;
        List<LivingEntity> onLine = Spells.enemiesOnLine(caster, sl, from, to, width);
        if (!pierces && !onLine.isEmpty()) {
            LivingEntity first = onLine.get(0);
            onLine = List.of(first);
            Vec3 middle = first.getBoundingBox().getCenter();
            stop = from.add(direction.scale(Math.max(0.0, middle.subtract(from).dot(direction))));
        }
        if (onFire != null) onFire.accept(from, stop);
        for (LivingEntity target : onLine) {
            if (onHit != null && target.isAlive()) onHit.accept(target, target.getBoundingBox().getCenter());
        }
        if (onEnd != null) onEnd.accept(stop);
    }
}
