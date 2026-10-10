package net.lukario.frogerealm.combat;

import net.lukario.frogerealm.particles.fx.ModelFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.lukario.frogerealm.particles.fx.SlashFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A model that flies and hits things: thrown daggers, bolts, shells, cards...
 * Describe it once like a ModelFx (every method returns a NEW copy), then fire it from any ability:
 *
 *   private static final Shot DAGGER = Shot.of(DAGGER_MODEL)   // a ModelFx whose TOP (up in Blockbench) is its front
 *           .speed(3.0)              // blocks per tick
 *           .range(32)               // how far it flies if nothing is in the way
 *           .width(0.4)              // how close its path must pass to an enemy's body to hit it
 *           .tip(0.6)                // blocks from the model's pivot to its tip, so the tip (not the middle) stops at the target
 *           .trail(DAGGER_STREAK)    // a SlashFx.line drawn along its path (optional)
 *           .stick(40);              // stays stuck in a block it hits for 2 seconds (optional)
 *
 *   int arrives = DAGGER
 *           .onHit((target, at) -> Spells.strike(player, target, 12f))      // when it reaches an enemy
 *           .onEnd((at, hitBlock) -> ParticleShapes.burst(sl, DUST, at, 8, 0.05, 0.2))   // where its flight ends
 *           .fire(player, sl, from, direction);
 *
 * It stops at the first enemy in its way; .pierce() makes it fly through all of them (each is hit once).
 * .windup(4) makes it hang in the air for 4 ticks before it leaves.
 *
 * How it works: the whole flight is worked out when it is fired (where the first block or enemy is), the model is
 * told to fly exactly that far, and the hits are delivered with Later.run when it gets there. An enemy that has
 * moved well out of the way by then is missed. Give it a model without keys of its own.
 * For a model that has an animation of its own (hovers first, flies in a curve...) use AnimatedShot.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Shot {

    @FunctionalInterface
    public interface Hit {
        /** 'at' = the point of the flight path nearest to the target. */
        void hit(LivingEntity target, Vec3 at);
    }

    @FunctionalInterface
    public interface End {
        /** 'at' = where the flight ended; hitBlock = it ended on a block (false: on an enemy, or at the end of its range). */
        void end(Vec3 at, boolean hitBlock);
    }

    /** An enemy further than this (plus the shot's width) from the path when the shot arrives has dodged it. */
    private static final double DODGE_ROOM = 2.0;

    private ModelFx model;
    private double speed = 2.5;
    private double range = 24.0;
    private double width = 0.4;
    private double tip = 0.0;
    private boolean pierce = false;
    private int windup = 0;
    private int stick = 0;
    private SlashFx trail = null;
    private Hit onHit = null;
    private End onEnd = null;

    private Shot() {}

    private Shot copy() {
        Shot c = new Shot();
        c.model = model; c.speed = speed; c.range = range; c.width = width; c.tip = tip;
        c.pierce = pierce; c.windup = windup; c.stick = stick; c.trail = trail; c.onHit = onHit; c.onEnd = onEnd;
        return c;
    }

    /** A shot that looks like this model. Its top (up in Blockbench) points the way it flies. */
    public static Shot of(ModelFx model) {
        Shot shot = new Shot();
        shot.model = model;
        return shot;
    }

    // ---------- customization (each returns a copy) ----------

    /** Another look for the same shot (e.g. a bigger or tinted copy of the model). */
    public Shot model(ModelFx model) { Shot c = copy(); c.model = model; return c; }

    /** Blocks per tick (20 = one block per tick for a whole second). */
    public Shot speed(double blocksPerTick) { Shot c = copy(); c.speed = Math.max(0.05, blocksPerTick); return c; }

    /** How far it flies when nothing stops it. */
    public Shot range(double blocks) { Shot c = copy(); c.range = Math.max(0.5, blocks); return c; }

    /** How fat its path is: it hits enemies whose body comes within this many blocks of the line it flies along. */
    public Shot width(double blocks) { Shot c = copy(); c.width = Math.max(0.0, blocks); return c; }

    /** Blocks from the model's pivot to its tip (at the scale you play it), so the tip is what reaches the target. */
    public Shot tip(double blocks) { Shot c = copy(); c.tip = blocks; return c; }

    /** Flies through enemies instead of stopping at the first one. Every enemy on the path is hit once. */
    public Shot pierce() { Shot c = copy(); c.pierce = true; return c; }

    /** Appears and hangs in the air for this many ticks before it flies. */
    public Shot windup(int ticks) { Shot c = copy(); c.windup = Math.max(0, ticks); return c; }

    /** Stays stuck in the block it hits for this many ticks (0 = disappears on impact). A model with .spin(...) stops turning there. */
    public Shot stick(int ticks) { Shot c = copy(); c.stick = Math.max(0, ticks); return c; }

    /** A streak drawn along its path as it flies (a SlashFx.line). null = none. */
    public Shot trail(SlashFx line) { Shot c = copy(); c.trail = line; return c; }

    /** What happens to each enemy it reaches. */
    public Shot onHit(Hit hit) { Shot c = copy(); c.onHit = hit; return c; }

    /** What happens where its flight ends (on a block, on the enemy that stopped it, or at the end of its range). */
    public Shot onEnd(End end) { Shot c = copy(); c.onEnd = end; return c; }

    // ---------- firing ----------

    /**
     * Fires it from 'from' along 'direction'. 'owner' is who it belongs to: it decides who counts as an enemy.
     * @return how many ticks from now its flight ends
     */
    public int fire(Player owner, ServerLevel sl, Vec3 from, Vec3 direction) {
        Vec3 dir = direction.normalize();
        Vec3 far = from.add(dir.scale(range));
        BlockHitResult wall = sl.clip(new ClipContext(from, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        boolean hitBlock = wall.getType() != HitResult.Type.MISS;
        Vec3 stop = wall.getLocation();

        List<LivingEntity> inTheWay = Spells.enemiesOnLine(owner, sl, from, stop, width);   // nearest first
        if (!pierce && !inTheWay.isEmpty()) {
            LivingEntity first = inTheWay.get(0);
            stop = from.add(dir.scale(alongPath(first, from, dir, from.distanceTo(stop))));
            hitBlock = false;
            inTheWay = List.of(first);
        }

        double distance = Math.max(0.3, from.distanceTo(stop));
        int flight = Math.max(1, (int) Math.ceil(distance / speed));
        int arrive = windup + flight;

        // The model: modelAlong points its top along the flight, so up(...) in its keys moves it along that line.
        boolean sticks = hitBlock && stick > 0;
        float travel = (float) Math.max(0.0, distance - tip + (sticks ? 0.3 : 0.0));   // a stuck one ends a little way in
        ModelFx flying = model.lifetime(arrive + (sticks ? stick : 2)).fade(0, sticks ? Math.min(stick, 8) : 2);
        if (sticks && model.spinDegrees() != 0f) {
            // something stuck in a wall must not keep whirling: the spin becomes an animation that ends on arrival
            flying = flying.spin(0f).during(0, arrive, ModelFx.pose().spin(model.spinDegrees() * arrive));
        }
        if (windup > 0) {
            flying = flying
                    .key(0, ModelFx.pose().up(-0.2f).scale(0.3f).alpha(0f))
                    .key(windup, ModelFx.pose().up(-0.6f).scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK)   // appears, drawn back
                    .key(arrive, ModelFx.pose().up(travel));
        } else {
            flying = flying.key(arrive, ModelFx.pose().up(travel));
        }
        ParticleShapes.modelAlong(sl, flying, from, dir);
        if (trail != null) {
            ParticleShapes.slashBetween(sl, trail.delay(windup).sweep(flight).lifetime(flight * 2 + 4), from, stop);
        }

        // The hits, each at the tick the shot gets that far.
        if (onHit != null) {
            Hit hit = onHit;
            for (LivingEntity target : inTheWay) {
                double along = alongPath(target, from, dir, distance);
                Vec3 at = from.add(dir.scale(along));
                int when = windup + (int) Math.round(flight * along / distance);
                Later.run(sl, when, () -> {
                    if (!target.isAlive() || target.level() != sl) return;
                    if (Spells.distanceToBody(target, at) > width + DODGE_ROOM) return;    // it got out of the way
                    hit.hit(target, at);
                });
            }
        }
        if (onEnd != null) {
            End end = onEnd;
            Vec3 where = stop;
            boolean onBlock = hitBlock;
            Later.run(sl, arrive, () -> end.end(where, onBlock));
        }
        return arrive;
    }

    /** How far along the path (0..max) the target's middle is. */
    private static double alongPath(LivingEntity target, Vec3 from, Vec3 dir, double max) {
        return Mth.clamp(target.getBoundingBox().getCenter().subtract(from).dot(dir), 0.0, max);
    }
}
