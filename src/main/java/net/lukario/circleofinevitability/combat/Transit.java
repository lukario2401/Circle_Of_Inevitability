package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.particles.fx.SlashFx;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.FallGuard;
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
 * Transit: the caster becomes part of something they throw, travels with it and steps out where it lands.
 * A flame they merge with, a shadow that slides along the ground, a card they fold into, lightning they ride:
 *
 *   boolean went = Transit.by(LANCE.onHit((target, at) -> Spells.strike(player, target, 30f)))   // any Shot
 *           .onDepart(from -> SpellFx.fireBurst(sl, from, 2.5))          // where they left (their feet)
 *           .onArrive((at, hitBlock) -> SpellFx.fireBurst(sl, at, 2.5))  // where they came out (their feet)
 *           .go(player, sl, hand, Spells.aimFrom(player, sl, hand, 20));
 *
 * TWO WAYS TO TRAVEL.
 *
 *   Straight (the default): the shot flies on its own, the caster is held where they stood, and when it lands
 *   they appear there, facing the way they went.
 *
 *   Steered: .steered(12) (degrees it can turn each tick; .steered() = STEER_DEGREES). The caster's body flies
 *   along with the shot and their camera with it; it turns toward wherever they look, a little every tick, so
 *   they can fly it round corners, up a tower or down onto someone. The shot's model is held out in front of
 *   their eyes (RIDE_AHEAD, RIDE_BELOW), and they see it in first person. It flies at the shot's speed for as
 *   far as the shot's range (measured along the way it really went), and ends early at a block, at the first
 *   enemy it hits (unless the shot pierces), when they are rooted (MOVE or TELEPORT), and when they sneak
 *   (.letGoOnSneak()). 'from' in go(...) is not used then: the shot starts at their body. A slower shot is
 *   easier to steer: give the ride its own speed, e.g. LANCE.speed(1.2).
 *
 * In both ways the caster is concealed while travelling (unseen, unhurt, mobs forget them; others see only the
 * shot) and takes no fall damage for a few seconds after. .visible() leaves them seen and hurtable meanwhile.
 *
 * The shot keeps its own onHit: it still hurts what it flies into. Its onEnd is taken over: put what should
 * happen at the end into onArrive instead. (.windup(...) and .stick(...) of the shot are not used when steered.)
 *
 * go(...) returns false, and does nothing, when the caster may not travel (rooted with TELEPORT). Check
 * Transit.canGo(player) first if the cast should cost nothing then. Straight: where they land is never inside a
 * block: against a wall they come out in front of it; when there is no room at all they stay where they were
 * (and onArrive still runs, there). Steered: they are wherever the flight brought them.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Transit {

    @FunctionalInterface
    public interface Depart {
        /** 'from' = the caster's feet when they left. */
        void depart(Vec3 from);
    }

    @FunctionalInterface
    public interface Arrive {
        /** 'at' = the caster's feet where they came out; hitBlock = the shot ended on a block. */
        void arrive(Vec3 at, boolean hitBlock);
    }

    /** No fall damage for this long after coming out (they may come out in the air, at a wall). */
    public static int LANDING_TICKS = 100;
    /** How far a steered shot turns in one tick toward where the caster looks, for .steered() without a number. */
    public static double STEER_DEGREES = 12.0;
    /** Steered: how far in front of the eyes the shot's model is held, in blocks. */
    public static double RIDE_AHEAD = 1.6;
    /** Steered: how far under the line of sight it is held, so the crosshair stays clear. */
    public static double RIDE_BELOW = 0.45;
    /** Steered: a little more time than the range needs, for when something slows them down on the way. */
    private static final int RIDE_SPARE_TICKS = 10;
    /**
     * A range for a shot that is ridden until something stops it (a block, an enemy, sneaking with
     * .letGoOnSneak()): Shot.range(Transit.UNLIMITED). Steered rides only; a straight shot needs a real range.
     */
    public static final double UNLIMITED = Double.POSITIVE_INFINITY;
    /** Steered: the longest a ride can last however far its range is (an hour), so an UNLIMITED one ends some day. */
    public static int MAX_RIDE_TICKS = 72000;
    /** Steered: a ride that has not got anywhere for this many ticks in a row is over (stuck on something). */
    private static final int STUCK_TICKS = 6;
    /**
     * Steered: ticks at the start when not moving yet does not count as stuck. The server only learns where a
     * player is from their game, a little late (their ping), so at first the body seems to stand still.
     */
    private static final int STUCK_GRACE = 6;
    /** How far back from where the shot stopped they come out, so they are not inside the wall it hit. */
    private static final double STEP_BACK = 0.7;
    /** How far around that spot a free place is looked for, when it is not free. */
    private static final double SEARCH = 2.5;

    /** Every ride gets a tag of its own for its model, so ending one never removes another. */
    private static int rides;

    private final Shot shot;
    private boolean hidden = true;
    private boolean steered = false;
    private double steerDegrees = STEER_DEGREES;
    private boolean letGoOnSneak = false;
    private Depart onDepart = null;
    private Arrive onArrive = null;

    private Transit(Shot shot) {
        this.shot = shot;
    }

    /** Travel inside this shot. Nothing happens until .go(...). */
    public static Transit by(Shot shot) {
        return new Transit(shot);
    }

    /** The caster stays visible and can be hurt during the flight (default: concealed). */
    public Transit visible() {
        this.hidden = false;
        return this;
    }

    /** They fly along with it and steer it with their look, STEER_DEGREES a tick. See the class comment. */
    public Transit steered() {
        return steered(STEER_DEGREES);
    }

    /** They fly along with it and steer it with their look: it turns at most this many degrees a tick (180 = at once). */
    public Transit steered(double degreesPerTick) {
        this.steered = true;
        this.steerDegrees = Math.max(0.0, degreesPerTick);
        return this;
    }

    /** Steered: sneaking during the flight ends it there (they step out where they are). */
    public Transit letGoOnSneak() {
        this.letGoOnSneak = true;
        return this;
    }

    /** Runs right before they leave, with their feet. */
    public Transit onDepart(Depart action) {
        this.onDepart = action;
        return this;
    }

    /** Runs right after they came out, with their feet there. */
    public Transit onArrive(Arrive action) {
        this.onArrive = action;
        return this;
    }

    /** False when the caster may not travel right now (rooted with TELEPORT). */
    public static boolean canGo(Player player) {
        return !Root.has(player, RootRestriction.TELEPORT);
    }

    /**
     * Fires the shot from 'from' along 'direction' and sends the caster with it (steered: the shot starts at their
     * body and 'direction' is only the way it sets off). Returns false (nothing happens, nothing is fired) when
     * they may not travel.
     */
    public boolean go(Player player, ServerLevel sl, Vec3 from, Vec3 direction) {
        if (!canGo(player)) return false;
        Vec3 way = direction.normalize();
        Vec3 left = player.position();
        if (onDepart != null) onDepart.depart(left);

        if (steered) {
            new Ride(this, player, sl, way).start();
            return true;
        }

        int flight = shot.onEnd((at, hitBlock) -> arrive(player, sl, at, way, hitBlock, left)).fire(player, sl, from, way);

        // the body goes into the shot: held where it stood (and unseen) until the shot lands
        Root.apply(player, flight + 2, RootRestriction.MOVE, RootRestriction.JUMP);
        if (hidden) Concealment.hide(player, flight + 2);
        return true;
    }

    // =========================
    // Straight
    // =========================

    private void arrive(Player player, ServerLevel sl, Vec3 stop, Vec3 way, boolean hitBlock, Vec3 left) {
        if (!Spells.casterStillHere(player, sl)) return;            // died or left on the way: nothing to bring out
        Root.remove(player, RootRestriction.MOVE, RootRestriction.JUMP);
        if (hidden) Concealment.reveal(player);

        Vec3 feet = landing(player, sl, stop, way);
        if (feet == null) feet = player.position();                  // nowhere to come out: back where they were
        player.teleportTo(feet.x, feet.y, feet.z);
        player.fallDistance = 0;
        FallGuard.protect(player, LANDING_TICKS);
        if (onArrive != null) onArrive.arrive(feet, hitBlock);
    }

    /** Where the caster's feet go for a shot that stopped at 'stop': a step back from it, never inside a block. */
    private static Vec3 landing(Player player, ServerLevel sl, Vec3 stop, Vec3 way) {
        Vec3 back = stop.subtract(way.scale(STEP_BACK));
        Vec3 feet = back.add(0, -player.getBbHeight() * 0.5, 0);    // the middle of the body where the shot was
        if (Spells.fits(player, sl, feet)) return feet;
        Vec3 ground = Spells.groundNear(player, sl, back, 1.5);       // standing on the ground right under it
        if (ground != null && Spells.fits(player, sl, ground)) return ground;
        return Spells.freeSpotNear(player, sl, back, way.scale(-1), SEARCH);
    }

    // =========================
    // Steered
    // =========================

    /**
     * One steered flight. Every tick: turn toward the look, look ahead for a block or an enemy, then give the body
     * the speed to get there (as Spells.push does, so the player's own game moves them smoothly and the camera
     * goes along). The body IS the shot: hits and blocks are worked out from where the body really is.
     */
    private static final class Ride {

        private final Transit transit;
        private final Shot shot;
        private final Player player;
        private final ServerLevel sl;
        private final String tag = "transit_ride_" + (rides++);
        private final Set<UUID> alreadyHit = new HashSet<>();
        private final int maxTicks;

        private Vec3 heading;
        private Vec3 lastCenter;
        private double travelled = 0.0;
        private int ticks = 0;
        private int stuck = 0;
        private boolean over = false;

        private Ride(Transit transit, Player player, ServerLevel sl, Vec3 heading) {
            this.transit = transit;
            this.shot = transit.shot;
            this.player = player;
            this.sl = sl;
            this.heading = heading;
            // (worked out in double and capped, so a huge or UNLIMITED range cannot overflow the int)
            double needed = Math.ceil(shot.reach() / shot.blocksPerTick()) + RIDE_SPARE_TICKS;
            this.maxTicks = (int) Math.min(needed, MAX_RIDE_TICKS);
        }

        private void start() {
            if (player.isFallFlying()) player.stopFallFlying();
            if (transit.hidden) Concealment.hide(player, maxTicks + 2);
            ModelFx model = shot.look();
            if (model != null) {
                ModelFx held = model.lifetime(maxTicks + 2).fade(0, 2).tag(tag);
                if (transit.hidden) held = held.shownWhileHidden();     // the others see the shot, not the body
                ParticleShapes.modelInFront(sl, held, player, RIDE_AHEAD, RIDE_BELOW);
            }
            lastCenter = center();
            step();
        }

        private Vec3 center() {
            return player.getBoundingBox().getCenter();
        }

        private void step() {
            if (over) return;
            if (!Spells.casterStillHere(player, sl)) {                // died or left: nothing to bring out
                over = true;
                ParticleShapes.clearModels(sl, player, tag);
                return;
            }
            if (Root.has(player, RootRestriction.MOVE) || !canGo(player)
                    || (transit.letGoOnSneak && ticks > 0 && player.isShiftKeyDown())) {
                finish(false);
                return;
            }

            Vec3 now = center();
            if (ticks > 0) {
                SlashFx streak = shot.streak();
                if (streak != null && now.distanceToSqr(lastCenter) > 0.01) {
                    ParticleShapes.slashBetween(sl, streak.sweep(1).lifetime(8), lastCenter, now);
                }
                // not getting anywhere: something holds them (a corner the look-ahead missed, a mob in the way...)
                boolean still = ticks > STUCK_GRACE && now.distanceToSqr(lastCenter) < 0.0625;
                stuck = still ? stuck + 1 : 0;
                if (stuck >= STUCK_TICKS) {
                    finish(true);
                    return;
                }
            }

            heading = Spells.turnToward(heading, player.getLookAngle(), transit.steerDegrees);
            double move = Math.min(shot.blocksPerTick(), shot.reach() - travelled);
            if (move <= 0.05 || ticks >= maxTicks) {
                finish(false);
                return;
            }

            // what is ahead this tick: the front of the body (half of it) plus the tip, plus this tick's way
            double front = player.getBbWidth() * 0.5 + shot.tipLength();
            Vec3 ahead = now.add(heading.scale(move + front));
            boolean hitBlock = false;
            BlockHitResult block = sl.clip(new ClipContext(now, ahead, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (block.getType() != HitResult.Type.MISS) {
                hitBlock = true;
                ahead = block.getLocation();                          // what stands behind the block is safe
            }

            List<LivingEntity> inTheWay = Spells.enemiesOnLine(player, sl, lastCenter, ahead, shot.pathWidth());   // nearest first
            Shot.Hit hit = shot.hitAction();
            for (LivingEntity target : inTheWay) {
                if (!alreadyHit.add(target.getUUID())) continue;
                if (hit != null) hit.hit(target, target.getBoundingBox().getCenter());
                if (!shot.goesThrough()) {
                    finish(false);
                    return;
                }
            }
            if (hitBlock) {
                finish(true);
                return;
            }

            player.setDeltaMovement(heading.scale(move));
            player.hurtMarked = true;                                 // makes the player's game take the speed
            player.fallDistance = 0;
            travelled += move;
            lastCenter = now;
            ticks++;
            Later.run(sl, 1, this::step);
        }

        private void finish(boolean hitBlock) {
            if (over) return;
            over = true;
            player.setDeltaMovement(Vec3.ZERO);                       // they step out and stand, not fly on
            player.hurtMarked = true;
            player.fallDistance = 0;
            ParticleShapes.clearModels(sl, player, tag);
            if (transit.hidden) Concealment.reveal(player);
            FallGuard.protect(player, LANDING_TICKS);
            if (transit.onArrive != null) transit.onArrive.arrive(player.position(), hitBlock);
        }
    }
}
