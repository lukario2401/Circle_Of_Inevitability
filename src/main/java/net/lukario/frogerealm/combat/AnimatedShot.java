package net.lukario.frogerealm.combat;

import net.lukario.frogerealm.particles.fx.ModelFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A model that hits things along the animation YOU gave it: a spear that hovers, tips over and flies, a skull that
 * swells and drifts off, a blade that swings out in a curve. You animate the ModelFx with keys as always; this
 * plays it and follows it tick by tick, so the damage is always where the model is seen.
 *
 *   private static final ModelFx SPEAR_MODEL = ModelFx.of("my_class/spear").pivot(8, 8, 8).glow().lifetime(80)
 *           .during(50, 60, ModelFx.pose().pitch(90), ModelFx.Ease.IN_OUT)       // tips over: the point looks forward
 *           .during(64, 76, ModelFx.pose().forward(26f), ModelFx.Ease.IN);       // and flies
 *
 *   private static final AnimatedShot SPEAR = AnimatedShot.of(SPEAR_MODEL)
 *           .width(0.6)              // how close its path must pass to an enemy's body to hit it
 *           .tip(1.8)                // blocks from the model's pivot to its front end
 *           .pierce();               // goes through enemies instead of stopping at the first
 *
 *   SPEAR.onHit((target, at) -> Spells.strike(player, target, 40f))
 *           .onMove(at -> ParticleShapes.burst(sl, SPARK, at, 2, 0.0, 0.03))     // a trail while it flies (optional)
 *           .fire(player, sl, from);                                             // flies at what the crosshair is on
 *
 * Nothing here names a tick. Change the keys, the timing or the lifetime of the model and the hits follow: it
 * asks the model itself where it is (ModelFx.poseAt) every tick, from the first to the last.
 *
 * The model's FRONT flies at the target: forward(...) in its keys is "toward what was aimed at", up(...) and
 * right(...) are across it. It only hits while it moves forward, so hovering, turning on the spot and drawing
 * back before the throw hurt no one (.anyDirection() changes that).
 *
 * It stops at the first enemy it reaches and at blocks, and the model is taken away at that moment.
 * .pierce() lets it go on through enemies, .throughBlocks() through walls.
 *
 * Shot or AnimatedShot? Shot works out a straight flight for you from a speed and a range. AnimatedShot follows
 * an animation you made yourself, so it also sees enemies that walk into the way while it flies.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class AnimatedShot {

    @FunctionalInterface
    public interface Hit {
        /** 'at' = the point of its path nearest to the target. */
        void hit(LivingEntity target, Vec3 at);
    }

    @FunctionalInterface
    public interface Stop {
        /** 'at' = where it was stopped; hitBlock = by a block (false: by an enemy). */
        void stop(Vec3 at, boolean hitBlock);
    }

    @FunctionalInterface
    public interface Expire {
        /** 'at' = where it was when its animation ended. */
        void expire(Vec3 at);
    }

    @FunctionalInterface
    public interface Move {
        /** 'at' = where its pivot is this tick. */
        void move(Vec3 at);
    }

    /** Less movement than this in one tick (in blocks) counts as standing still. */
    private static final double STILL = 1.0E-3;

    /** Every shot that is fired gets a tag of its own, so that stopping one never removes the model of another. */
    private static int fired;

    private ModelFx model;
    private double width = 0.4;
    private double tip = 0.0;
    private boolean pierce = false;
    private boolean throughBlocks = false;
    private boolean anyDirection = false;
    private Hit onHit = null;
    private Stop onStop = null;
    private Expire onExpire = null;
    private Move onMove = null;

    private AnimatedShot() {}

    private AnimatedShot copy() {
        AnimatedShot c = new AnimatedShot();
        c.model = model; c.width = width; c.tip = tip; c.pierce = pierce; c.throughBlocks = throughBlocks;
        c.anyDirection = anyDirection; c.onHit = onHit; c.onStop = onStop; c.onExpire = onExpire;
        c.onMove = onMove;
        return c;
    }

    /** A shot that looks and moves like this model. Its front (north in Blockbench) faces what it is fired at. */
    public static AnimatedShot of(ModelFx model) {
        AnimatedShot shot = new AnimatedShot();
        shot.model = model;
        return shot;
    }

    // ---------- customization (each returns a copy) ----------

    /** Another model for the same shot (a bigger or tinted copy, or one with other keys). */
    public AnimatedShot model(ModelFx model) { AnimatedShot c = copy(); c.model = model; return c; }

    /** How fat its path is: it hits enemies whose body comes within this many blocks of the line it moves along. */
    public AnimatedShot width(double blocks) { AnimatedShot c = copy(); c.width = Math.max(0.0, blocks); return c; }

    /**
     * Blocks from the model's pivot to its front end (at the scale you play it). With it the point of a spear is
     * what reaches the enemy and the wall; without it the pivot is.
     */
    public AnimatedShot tip(double blocks) { AnimatedShot c = copy(); c.tip = Math.max(0.0, blocks); return c; }

    /** Goes through enemies instead of stopping at the first one. Every enemy on its way is hit once. */
    public AnimatedShot pierce() { AnimatedShot c = copy(); c.pierce = true; return c; }

    /** Blocks do not stop it. */
    public AnimatedShot throughBlocks() { AnimatedShot c = copy(); c.throughBlocks = true; return c; }

    /** Also hits while it moves sideways or backward (a blade that swings round, something that comes back). */
    public AnimatedShot anyDirection() { AnimatedShot c = copy(); c.anyDirection = true; return c; }

    /** What happens to each enemy it reaches. */
    public AnimatedShot onHit(Hit hit) { AnimatedShot c = copy(); c.onHit = hit; return c; }

    /**
     * What happens where it is stopped: by a block, or by the first enemy when it does not pierce (after onHit for
     * that enemy). The model is already gone then. This is where an explosion goes.
     */
    public AnimatedShot onStop(Stop stop) { AnimatedShot c = copy(); c.onStop = stop; return c; }

    /** What happens when its animation runs out and nothing has stopped it. */
    public AnimatedShot onExpire(Expire expire) { AnimatedShot c = copy(); c.onExpire = expire; return c; }

    /**
     * Runs every tick it FLIES, with where it is: a trail of particles, a hum. Like the hits it only counts
     * forward movement (any movement with .anyDirection()), so nothing trails while it hovers, turns on the
     * spot or draws back. The first time it runs is the moment it is let fly: the place for a "whoosh".
     *   .onMove(at -> ParticleShapes.burst(sl, SPARK, at, 2, 0.0, 0.03))
     */
    public AnimatedShot onMove(Move move) { AnimatedShot c = copy(); c.onMove = move; return c; }

    // ---------- what the animation says ----------

    /** How many ticks its animation lasts. */
    public int ticks() {
        return model.delayTicks() + model.lifetimeTicks();
    }

    /** The furthest forward its front end gets in its whole animation, in blocks. */
    public double reach() {
        double furthest = 0.0;
        for (int time = 0; time <= model.lifetimeTicks(); time++) {
            furthest = Math.max(furthest, model.poseAt(time).forward());
        }
        return furthest + tip;
    }

    // ---------- firing ----------

    /**
     * Plays it at 'from', aimed at what the owner's crosshair is on: the enemy under it, else the block, else the
     * point as far out as this shot reaches. So it lands where the owner looks, also when 'from' is above their head
     * or beside them. If 'from' may be inside a wall, wrap it in Spells.clearStart(...).
     * @return how many ticks from now its animation ends
     */
    public int fire(Player owner, ServerLevel sl, Vec3 from) {
        return fire(owner, sl, from, Spells.aimFrom(owner, sl, from, Math.max(1.0, reach())));
    }

    /**
     * Plays it at 'from' with its front along 'direction' (up and down too). 'owner' is who it belongs to: it
     * decides who counts as an enemy.
     * @return how many ticks from now its animation ends
     */
    public int fire(Player owner, ServerLevel sl, Vec3 from, Vec3 direction) {
        Vec3 dir = direction.normalize();
        float yaw = Spells.yawOf(dir);
        float pitch = (float) -Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, dir.y))));   // looking up = negative

        String tag = "animated_shot_" + (fired++);
        ModelFx placed = model.tag(tag).rotated(yaw, pitch, 0f);
        ParticleShapes.spawnFar(sl, placed, from, Vec3.ZERO);

        Flight flight = new Flight(this, owner, sl, from, placed, tag);
        Later.run(sl, 1, () -> flight.step(1));
        return ticks();
    }

    /**
     * 'x' blocks to the right, 'y' up and 'z' back, as seen by a model played with this yaw, pitch and roll:
     * the same turns, in the same order, as ModelFxRenderer makes when it draws it.
     */
    private static Vec3 turned(double x, double y, double z, float yaw, float pitch, float roll) {
        double c = Math.toRadians(-roll);
        double x1 = x * Math.cos(c) - y * Math.sin(c);
        double y1 = x * Math.sin(c) + y * Math.cos(c);

        double a = Math.toRadians(-pitch);
        double y2 = y1 * Math.cos(a) - z * Math.sin(a);
        double z2 = y1 * Math.sin(a) + z * Math.cos(a);

        double b = Math.toRadians(180.0 - yaw);
        double x3 = x1 * Math.cos(b) + z2 * Math.sin(b);
        double z3 = -x1 * Math.sin(b) + z2 * Math.cos(b);

        return new Vec3(x3, y2, z3);
    }

    /** One shot on its way. */
    private static final class Flight {

        private final AnimatedShot shot;
        private final Player owner;
        private final ServerLevel sl;
        private final Vec3 from;
        private final ModelFx placed;
        private final String tag;

        // the three directions its keys move it in
        private final Vec3 right;
        private final Vec3 up;
        private final Vec3 forward;

        private final int last;
        private final Set<UUID> alreadyHit = new HashSet<>();

        private Flight(AnimatedShot shot, Player owner, ServerLevel sl, Vec3 from, ModelFx placed, String tag) {
            this.shot = shot;
            this.owner = owner;
            this.sl = sl;
            this.from = from;
            this.placed = placed;
            this.tag = tag;
            this.right = turned(1, 0, 0, placed.yaw(), placed.pitch(), placed.roll());
            this.up = turned(0, 1, 0, placed.yaw(), placed.pitch(), placed.roll());
            this.forward = turned(0, 0, -1, placed.yaw(), placed.pitch(), placed.roll());
            this.last = placed.delayTicks() + placed.lifetimeTicks();
        }

        /** Where the model's pivot is 'tick' ticks after it was fired. */
        private Vec3 positionAt(int tick) {
            ModelFx.Pose pose = placed.poseAt(Math.max(0, tick - placed.delayTicks()));
            return from
                    .add(right.scale(pose.right()))
                    .add(up.scale(pose.up()))
                    .add(forward.scale(pose.forward()));
        }

        private void step(int tick) {
            Vec3 now = positionAt(tick);

            if (!Spells.casterStillHere(owner, sl)) {          // its owner died or left: it is gone, and nothing happens
                ParticleShapes.clearModels(sl, now, tag);
                return;
            }

            Vec3 before = positionAt(tick - 1);

            if (stoppedBetween(before, now)) return;

            if (shot.onMove != null && flies(before, now)) shot.onMove.move(now);

            if (tick >= last) {
                if (shot.onExpire != null) shot.onExpire.expire(now);
                return;
            }

            Later.run(sl, 1, () -> step(tick + 1));
        }

        /** True if what it did in one tick counts as flying: it moved, and forward (any way with .anyDirection()). */
        private boolean flies(Vec3 before, Vec3 now) {
            Vec3 move = now.subtract(before);
            if (move.length() < STILL) return false;
            return shot.anyDirection || move.dot(forward) >= STILL;
        }

        /** What it meets on the way it went in one tick. True when that was the end of it. */
        private boolean stoppedBetween(Vec3 before, Vec3 now) {
            if (!flies(before, now)) return false;

            Vec3 move = now.subtract(before);
            double length = move.length();

            Vec3 heading = move.scale(1.0 / length);
            Vec3 end = now.add(heading.scale(shot.tip));          // its front end is ahead of its pivot

            boolean hitBlock = false;
            if (!shot.throughBlocks) {
                BlockHitResult block = sl.clip(new ClipContext(before, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
                if (block.getType() != HitResult.Type.MISS) {
                    hitBlock = true;
                    end = block.getLocation();                    // what stands behind the block is safe
                }
            }

            List<LivingEntity> inTheWay = Spells.enemiesOnLine(owner, sl, before, end, shot.width);   // nearest first
            for (LivingEntity target : inTheWay) {
                if (!alreadyHit.add(target.getUUID())) continue;

                Vec3 at = nearestOnLine(before, end, target.getBoundingBox().getCenter());
                if (shot.onHit != null) shot.onHit.hit(target, at);

                if (!shot.pierce) {
                    stop(at, false);
                    return true;
                }
            }

            if (hitBlock) {
                stop(end, true);
                return true;
            }
            return false;
        }

        private void stop(Vec3 at, boolean hitBlock) {
            ParticleShapes.clearModels(sl, at, tag);
            if (shot.onStop != null) shot.onStop.stop(at, hitBlock);
        }

        /** The point of the line a..b that is nearest to 'point'. */
        private static Vec3 nearestOnLine(Vec3 a, Vec3 b, Vec3 point) {
            Vec3 line = b.subtract(a);
            double lengthSqr = line.lengthSqr();
            if (lengthSqr < 1.0E-8) return a;
            double along = Math.max(0.0, Math.min(1.0, point.subtract(a).dot(line) / lengthSqr));
            return a.add(line.scale(along));
        }
    }
}
