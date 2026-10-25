package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A summoned model that HUNTS: a raven, a ghost, a wisp, a homing bolt. It flies at its enemy, turns after it
 * when it runs, and hits it on contact. Unlike Shot (worked out once, flies straight) a Seeker is steered by the
 * server every tick, so its path bends toward wherever its enemy is now.
 *
 * The quick way, one line (it flies from 'from' at 'target' and deals 'damage' when it touches it):
 *
 *   Seeker.send(player, sl, GHOST_MODEL, from, target, 10f);
 *
 * Or describe it once like a Shot (every method returns a NEW copy) and launch it from any ability:
 *
 *   private static final Seeker RAVEN = Seeker.of(RAVEN_MODEL)   // its FRONT (north in Blockbench) looks where it flies
 *           .speed(0.9)              // blocks per tick
 *           .turn(14)                // degrees a tick it can turn toward its enemy (small = wide curves, can be outrun)
 *           .life(100)               // ticks before it gives up and bursts
 *           .width(0.5)              // how close it must come to an enemy's body to hit it
 *           .flap(6)                 // a model with .frames(n): goes 0 -> n-1 -> 0 every 6 ticks (wings beating)
 *           .trail(EMBER, 1)         // particles it leaves behind, per tick
 *           .burst(EMBER, 12);       // particles where it ends (on an enemy, a wall, or when it gives up)
 *
 *   RAVEN.onHit((target, at) -> Spells.strike(player, target, 8f))
 *        .onEnd((at, why) -> { if (why == Seeker.Ending.WALL) ... })
 *        .launch(player, sl, from, startDirection, target);        // startDirection null = straight at it
 *
 *   RAVEN.onHit(...).launchAt(player, sl, from, enemies, 6, 2);    // a flock of 6 shared out over the enemies
 *
 * What it does on the way:
 *   - It hits the first enemy it touches (not only its own), then it is gone.
 *   - Its enemy dies, hides (Concealment) or gets too far away: it turns to the nearest enemy left (.keepTarget()
 *     makes it give up instead). With no enemy at all for a moment it bursts (Ending.LOST).
 *   - A block in the way stops it (Ending.WALL); .throughWalls() lets it fly through.
 *   - It cannot turn on the spot, so a fast enemy that dodges sideways can make it miss and come round again.
 *     Close to its enemy it turns much harder, so it never circles it forever.
 *
 * How it is drawn: the model is played once (ParticleShapes.modelFacing, with its own tag) and then moved every
 * tick with ParticleShapes.moveModel; the client glides it between those spots like an entity, and keeps playing
 * the model's keys and frames meanwhile. The model must NOT have .following(...): give it a plain ModelFx.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Seeker {

    /** How a Seeker's flight ended. */
    public enum Ending {
        /** it reached an enemy (onHit has run) */
        HIT,
        /** a block was in the way */
        WALL,
        /** no enemy left to go for */
        LOST,
        /** its life() ran out before it got anywhere */
        EXPIRED,
        /** its caster died, logged out or left the dimension */
        GONE
    }

    @FunctionalInterface
    public interface Hit {
        /** 'at' = where it was when it touched the target. */
        void hit(LivingEntity target, Vec3 at);
    }

    @FunctionalInterface
    public interface End {
        /** Runs once where the flight ended, whatever the reason (after onHit when it hit something). */
        void end(Vec3 at, Ending why);
    }

    /** Ticks it keeps flying straight with no enemy in reach before it bursts (Ending.LOST). */
    private static final int LOST_TICKS = 12;
    /** An enemy further than seek() x this from it is out of reach: it looks for a closer one. */
    private static final double TOO_FAR = 2.0;
    /** Most keys a model can carry (ModelFx.MAX_KEYS is 64; some room is left for the model's own). */
    private static final int MAX_FLAP_KEYS = 56;

    private static int nextId = 0;

    private ModelFx model;
    private boolean topFirst = false;
    private double speed = 0.8;
    private double turn = 12.0;
    private int life = 100;
    private int windup = 0;
    private double width = 0.5;
    private double seek = 24.0;
    private boolean retarget = true;
    private boolean throughWalls = false;
    private int flap = 0;
    private ParticleOptions trail = null;
    private int trailCount = 1;
    private ParticleOptions burst = null;
    private int burstCount = 10;
    private Hit onHit = null;
    private End onEnd = null;

    private Seeker() {}

    private Seeker copy() {
        Seeker c = new Seeker();
        c.model = model; c.topFirst = topFirst; c.speed = speed; c.turn = turn; c.life = life; c.windup = windup;
        c.width = width; c.seek = seek; c.retarget = retarget; c.throughWalls = throughWalls; c.flap = flap;
        c.trail = trail; c.trailCount = trailCount; c.burst = burst; c.burstCount = burstCount;
        c.onHit = onHit; c.onEnd = onEnd;
        return c;
    }

    /** A Seeker that looks like this model. Its FRONT (north in Blockbench) looks the way it flies. */
    public static Seeker of(ModelFx model) {
        Seeker seeker = new Seeker();
        seeker.model = model;
        return seeker;
    }

    /**
     * The one-line version: 'model' flies from 'from' at 'target' and deals 'damage' (from 'owner') when it
     * touches it, with the default speed, turn and life. It goes for another enemy if 'target' dies first.
     */
    public static void send(LivingEntity owner, ServerLevel sl, ModelFx model, Vec3 from, LivingEntity target, float damage) {
        of(model).onHit((hit, at) -> Spells.strike(owner, hit, damage)).launch(owner, sl, from, null, target);
    }

    // ---------- customization (each returns a copy) ----------

    /** Another look for the same Seeker (a bigger or tinted copy of the model). */
    public Seeker model(ModelFx model) { Seeker c = copy(); c.model = model; return c; }

    /** For models that point with their TOP (up in Blockbench), like Shot and modelAlong: spears, bolts, comets. */
    public Seeker pointsWithTop() { Seeker c = copy(); c.topFirst = true; return c; }

    /** Blocks per tick. */
    public Seeker speed(double blocksPerTick) { Seeker c = copy(); c.speed = Math.max(0.05, blocksPerTick); return c; }

    /** Degrees a tick it can turn toward its enemy. 5 = wide, lazy curves; 20 = sharp; 180 = turns on the spot. */
    public Seeker turn(double degreesPerTick) { Seeker c = copy(); c.turn = Math.max(0.5, degreesPerTick); return c; }

    /** Ticks it flies at most (after the windup) before it gives up and bursts. */
    public Seeker life(int ticks) { Seeker c = copy(); c.life = Math.max(1, ticks); return c; }

    /** Hangs in the air for this many ticks, drifting slowly the way it starts, before it goes for its enemy. */
    public Seeker windup(int ticks) { Seeker c = copy(); c.windup = Math.max(0, ticks); return c; }

    /** How close (in blocks) it must come to an enemy's body to hit it. */
    public Seeker width(double blocks) { Seeker c = copy(); c.width = Math.max(0.0, blocks); return c; }

    /** How far around it it looks for a new enemy when it has none (and how far it chases one, times 2). */
    public Seeker seek(double blocks) { Seeker c = copy(); c.seek = Math.max(1.0, blocks); return c; }

    /** Only ever goes for the target it was launched at: if that one is gone, it bursts instead of finding another. */
    public Seeker keepTarget() { Seeker c = copy(); c.retarget = false; return c; }

    /** Flies through blocks instead of bursting on them. */
    public Seeker throughWalls() { Seeker c = copy(); c.throughWalls = true; return c; }

    /**
     * For a model made of frames (ModelFx.frames(n), e.g. wings up / level / down): it goes from frame 0 to the last
     * and back every 'ticks' ticks while it flies, gliding between them if the model has .smooth(). 0 = no flapping.
     */
    public Seeker flap(int ticks) { Seeker c = copy(); c.flap = Math.max(0, ticks); return c; }

    /** 'perTick' of these particles are left behind it every tick. null = none. */
    public Seeker trail(ParticleOptions particle, int perTick) { Seeker c = copy(); c.trail = particle; c.trailCount = Math.max(1, perTick); return c; }

    /** 'count' of these particles burst out where it ends (on an enemy, a block, or when it gives up). null = none. */
    public Seeker burst(ParticleOptions particle, int count) { Seeker c = copy(); c.burst = particle; c.burstCount = Math.max(1, count); return c; }

    /** What happens to the enemy it reaches. */
    public Seeker onHit(Hit hit) { Seeker c = copy(); c.onHit = hit; return c; }

    /** What happens where it ends, whatever the reason (see Ending). */
    public Seeker onEnd(End end) { Seeker c = copy(); c.onEnd = end; return c; }

    // ---------- launching ----------

    /**
     * Sends it from 'from' after 'target'. 'owner' decides who counts as an enemy (and is who the damage comes from
     * in onHit). 'startDirection' = the way it sets off (it then curves round to its enemy): null = straight at it.
     * 'target' may be null: it then flies along startDirection and takes the first enemy it finds within seek().
     */
    public void launch(LivingEntity owner, ServerLevel sl, Vec3 from, Vec3 startDirection, LivingEntity target) {
        Vec3 heading = startDirection;
        if (heading == null || heading.lengthSqr() < 1.0E-6) {
            heading = target != null ? target.getBoundingBox().getCenter().subtract(from) : owner.getLookAngle();
        }
        if (heading.lengthSqr() < 1.0E-6) heading = owner.getLookAngle();
        new Flight(this, owner, sl, from, heading.normalize(), target).start();
    }

    /**
     * A flock: 'count' of them leave 'from' one after another ('everyTicks' apart), fanned out around where the
     * owner looks, and are shared out over 'enemies' (each enemy gets its first before any gets a second).
     * An empty list sends them out to find enemies themselves.
     */
    public void launchAt(LivingEntity owner, ServerLevel sl, Vec3 from, List<LivingEntity> enemies, int count, int everyTicks) {
        Vec3 look = owner.getLookAngle();
        for (int i = 0; i < count; i++) {
            LivingEntity target = enemies.isEmpty() ? null : enemies.get(i % enemies.size());
            // fanned out left and right of the look, and a little upward: they burst out, then curve in
            double side = count == 1 ? 0.0 : -70.0 + 140.0 * i / (count - 1);
            Vec3 out = Spells.turned(look, side).add(0, 0.45, 0).normalize();
            Later.run(sl, i * Math.max(0, everyTicks), () -> {
                if (Spells.casterStillHere(owner, sl)) launch(owner, sl, from, out, target);
            });
        }
    }

    // ---------- one flight ----------

    private static final class Flight {
        private final Seeker kind;
        private final LivingEntity owner;
        private final ServerLevel sl;
        private final String tag;
        private Vec3 at;
        private Vec3 heading;
        private LivingEntity target;
        private int age = 0;
        private int alone = 0;

        Flight(Seeker kind, LivingEntity owner, ServerLevel sl, Vec3 from, Vec3 heading, LivingEntity target) {
            this.kind = kind;
            this.owner = owner;
            this.sl = sl;
            this.at = from;
            this.heading = heading;
            this.target = target;
            this.tag = "seeker_" + (nextId++);
        }

        void start() {
            ModelFx shown = kind.model.notFollowing().tag(tag)
                    .lifetime(kind.windup + kind.life + 4)
                    .fadeIn(Math.min(3, kind.windup + kind.life));
            shown = withFlaps(shown, kind.windup + kind.life + 4);
            if (kind.topFirst) ParticleShapes.modelAlong(sl, shown, at, heading);
            else ParticleShapes.modelFacing(sl, shown, at, heading);
            Later.run(sl, 1, this::step);
        }

        /** Wing beats: frame 0 -> last -> 0 every 'flap' ticks, as .during(...) keys (they don't disturb the model's own). */
        private ModelFx withFlaps(ModelFx shown, int ticks) {
            if (kind.flap <= 0 || shown.frameCount() < 2) return shown;
            int half = Math.max(1, kind.flap / 2);
            int room = Math.max(0, MAX_FLAP_KEYS - shown.keys().size());
            int beats = Math.min(room, ticks / half + 1);
            float last = shown.frameCount() - 1;
            for (int i = 0; i < beats; i++) {
                float frame = i % 2 == 0 ? last : 0f;
                shown = shown.during(i * half, (i + 1) * half, ModelFx.pose().frame(frame), ModelFx.Ease.IN_OUT);
            }
            return shown;
        }

        private void step() {
            if (!Spells.casterStillHere(owner, sl)) { finish(Ending.GONE, null); return; }
            age++;
            boolean hunting = age > kind.windup;
            if (age > kind.windup + kind.life) { finish(Ending.EXPIRED, null); return; }

            double move = kind.speed;
            if (hunting) {
                if (!stillPrey(target)) target = kind.retarget ? nearestPrey() : null;
                if (target == null) {
                    if (!kind.retarget) { finish(Ending.LOST, null); return; }          // .keepTarget(): its one enemy is gone
                    if (++alone > LOST_TICKS) { finish(Ending.LOST, null); return; }
                } else {
                    alone = 0;
                    heading = Spells.turnToward(heading, target.getBoundingBox().getCenter().subtract(at), turnNow());
                }
            } else {
                move = kind.speed * 0.15;                                   // the windup: drifts slowly
            }

            Vec3 next = at.add(heading.scale(move));

            // an enemy in the way (any enemy, not only its own)
            LivingEntity touched = Spells.firstEnemyOnLine(owner, sl, at, next, kind.width);
            if (touched == null && target != null && Spells.distanceToBody(target, next) <= kind.width) touched = target;

            // a block in the way: it stops on it (unless the enemy is nearer)
            if (!kind.throughWalls) {
                BlockHitResult wall = sl.clip(new ClipContext(at, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
                if (wall.getType() != HitResult.Type.MISS) {
                    double toWall = at.distanceTo(wall.getLocation());
                    if (touched == null || Spells.distanceToBody(touched, at) > toWall) {
                        at = wall.getLocation().subtract(heading.scale(0.15));
                        finish(Ending.WALL, null);
                        return;
                    }
                }
            }

            if (touched != null) {
                at = closestTo(touched, at, next);
                finish(Ending.HIT, touched);
                return;
            }

            if (kind.trail != null) ParticleShapes.burst(sl, kind.trail, at, kind.trailCount, 0.0, 0.03);
            at = next;
            if (kind.topFirst) ParticleShapes.moveModelAlong(sl, tag, at, heading);
            else ParticleShapes.moveModelFacing(sl, tag, at, heading);
            Later.run(sl, 1, this::step);
        }

        /**
         * How hard it may turn this tick. Close to its enemy it turns much harder: with a fixed turn a fast Seeker
         * would circle round an enemy standing still and never reach it.
         */
        private double turnNow() {
            double radius = kind.speed / Math.toRadians(kind.turn);       // the tightest circle it can fly
            double distance = Math.max(0.1, Spells.distanceToBody(target, at));
            double closeness = radius * 2.0 / distance;                     // above 1 = inside that circle (twice over)
            return closeness <= 1.0 ? kind.turn : Math.min(180.0, kind.turn * closeness * closeness);
        }

        private boolean stillPrey(LivingEntity enemy) {
            if (enemy == null || !enemy.isAlive() || enemy.isRemoved() || enemy.level() != sl) return false;
            if (!Spells.isEnemy(owner, enemy)) return false;
            return Spells.distanceToBody(enemy, at) <= kind.seek * TOO_FAR;
        }

        /** The nearest enemy within seek() it can get at (not behind blocks, unless it flies through them). */
        private LivingEntity nearestPrey() {
            LivingEntity best = null;
            double nearest = Double.MAX_VALUE;
            for (LivingEntity enemy : Spells.enemiesAround(owner, sl, at, kind.seek)) {
                Vec3 middle = enemy.getBoundingBox().getCenter();
                if (!kind.throughWalls && !Spells.clearLine(owner, sl, at, middle)) continue;
                double distance = middle.distanceToSqr(at);
                if (distance < nearest) {
                    nearest = distance;
                    best = enemy;
                }
            }
            return best;
        }

        /** The point between from and to nearest to the enemy's middle: where it touched it. */
        private static Vec3 closestTo(LivingEntity enemy, Vec3 from, Vec3 to) {
            Vec3 line = to.subtract(from);
            double length = line.lengthSqr();
            if (length < 1.0E-8) return from;
            double t = enemy.getBoundingBox().getCenter().subtract(from).dot(line) / length;
            return from.add(line.scale(Math.max(0.0, Math.min(1.0, t))));
        }

        private void finish(Ending why, LivingEntity hit) {
            ParticleShapes.clearModels(sl, at, tag);
            if (kind.burst != null) ParticleShapes.burst(sl, kind.burst, at, kind.burstCount, 0.04, 0.22);
            if (why == Ending.GONE) return;                              // its caster is not here to own the hit
            if (hit != null && kind.onHit != null) kind.onHit.hit(hit, at);
            if (kind.onEnd != null) kind.onEnd.end(at, why);
        }
    }
}
