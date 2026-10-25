package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * An orbit: a few small bodies circling a player for a while, each of which can stop one projectile (and comes back
 * some time later) and strike enemies that come too close. For guarding stars, orbiting blades, motes of light,
 * a ring of shields. Knowledge Emperor's Star Guard is built on it:
 *
 *   Orbit.around(player, 4)                         // four bodies
 *           .radius(1.2).height(1.1).speed(6f)      // how far out, how high (from the feet), degrees a tick
 *           .lasts(600)                             // ticks (default 200)
 *           .catchesProjectiles(100)                // each stops one hostile projectile, and is back 5 s later
 *           .strikes(3.0, 40, (enemy, index) -> Spells.strike(player, enemy, 15f))   // an enemy within 3 blocks is
 *                                                   // struck by one ready body every 2 seconds
 *           .onTick(age -> MyFx.guardTick(sl, player, tag, age))           // every tick: move the models
 *           .onCatch((index, projectile, at) -> MyFx.blocked(sl, at))      // body 'index' stopped one at 'at'
 *           .onBack((index, ticksLeft, age) -> MyFx.back(sl, index, ticksLeft, age))
 *           .onEnd(() -> MyFx.guardEnd(sl, player, tag))
 *           .open(player, sl, "my_pathway_guard");
 *
 * Every part after around(...) is optional. Build a new one for every cast.
 *
 * WHERE THE BODIES ARE. Body i sits at spot(i, age): round the owner's feet at 'height', 'radius' out, at
 * age * speed + i * 360 / count degrees, so a look that uses the same three numbers (radius, height, speed) lines up
 * with it exactly. The orbit draws nothing itself: move the models in onTick (ParticleShapes.moveModel), take a
 * caught body's model away in onCatch and put it back in onBack.
 *
 * WHAT IT CATCHES. A projectile of anyone who is not on the owner's side (Spells.isFriend; a projectile of nobody
 * too) that comes within 'radius' + 1 of the owner's body, while a body is ready: the ready body nearest to it takes
 * it and is away for 'returnTicks'. A hit by a projectile that was too fast to be seen first is stopped as well.
 *
 *   Orbit.isOpen(player, name)   Orbit.ticksLeft(player, name)   Orbit.ready(player, name)   Orbit.close(player, name)
 *   Orbit.age(player, name)      Orbit.firstReady(player, name)
 *
 * SENDING ONE AWAY. Orbit.release(player, name, index, awayTicks) launches body 'index' yourself (a cluster flung at
 * an enemy, an asteroid thrown on a blow): it returns where the body was, so a Shot can leave from there, and the
 * body is back 'awayTicks' later (onBack), or never (0). Star Dragon's Fusion Cluster and the Dragon's asteroid:
 *
 *   int i = Orbit.firstReady(player, "my_pathway_rock");
 *   Vec3 from = i < 0 ? null : Orbit.release(player, "my_pathway_rock", i, 60);   // back in 3 seconds
 *   if (from != null) ROCK_SHOT.onHit(...).fire(player, sl, from, direction);
 *
 * Opening one with a name that player already has closes the old one first. It ends when its time is up or its
 * owner dies, logs out or changes dimension. Not saved: a server restart drops it.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Orbit {

    @FunctionalInterface
    public interface Strike {
        void hit(LivingEntity enemy, int index);
    }

    @FunctionalInterface
    public interface Catch {
        void caught(int index, Entity projectile, Vec3 at);
    }

    @FunctionalInterface
    public interface Back {
        void back(int index, int ticksLeft, int age);
    }

    private static final List<Orbit> OPEN = new ArrayList<>();

    // ---------- what it is ----------

    private final int count;
    private double radius = 1.2;
    private double height = 1.1;
    private float speed = 6f;
    private int lasts = 200;
    private int returnTicks = -1;            // -1: it catches nothing
    private double strikeRange;
    private int strikeEvery;
    private Strike strike;
    private java.util.function.IntConsumer onTick;
    private Catch onCatch;
    private Back onBack;
    private Runnable onEnd;

    // ---------- while it is open ----------

    private LivingEntity owner;
    private ServerLevel level;
    private String name;
    private int age;
    private boolean closed;
    private int[] awayUntil;                 // the age each body comes back at (0 = it is here)
    private int lastStrike;

    private Orbit(int count) {
        this.count = Math.max(1, count);
    }

    /** 'count' bodies round a player. Nothing happens until open(...). */
    public static Orbit around(LivingEntity owner, int count) {
        Orbit orbit = new Orbit(count);
        orbit.owner = owner;
        return orbit;
    }

    public Orbit radius(double blocks) {
        this.radius = blocks;
        return this;
    }

    /** How high the ring is above the owner's feet. */
    public Orbit height(double blocks) {
        this.height = blocks;
        return this;
    }

    /** Degrees a tick it turns (negative: the other way). */
    public Orbit speed(float degreesPerTick) {
        this.speed = degreesPerTick;
        return this;
    }

    public Orbit lasts(int ticks) {
        this.lasts = ticks;
        return this;
    }

    /** Each body stops one hostile projectile and is back 'returnTicks' later. */
    public Orbit catchesProjectiles(int returnTicks) {
        this.returnTicks = Math.max(0, returnTicks);
        return this;
    }

    /** Every 'everyTicks', one ready body strikes the nearest enemy within 'range' of the owner's body. */
    public Orbit strikes(double range, int everyTicks, Strike strike) {
        this.strikeRange = range;
        this.strikeEvery = Math.max(1, everyTicks);
        this.strike = strike;
        return this;
    }

    public Orbit onTick(java.util.function.IntConsumer action) {
        this.onTick = action;
        return this;
    }

    public Orbit onCatch(Catch action) {
        this.onCatch = action;
        return this;
    }

    public Orbit onBack(Back action) {
        this.onBack = action;
        return this;
    }

    public Orbit onEnd(Runnable action) {
        this.onEnd = action;
        return this;
    }

    /** Opens it. If this player already has one with this name, that one is closed first. */
    public void open(LivingEntity owner, ServerLevel level, String name) {
        close(owner, name);
        this.owner = owner;
        this.level = level;
        this.name = name;
        this.age = 0;
        this.closed = false;
        this.awayUntil = new int[count];
        this.lastStrike = -strikeEvery;
        OPEN.add(this);
    }

    /** Where body 'index' is at 'age' ticks. */
    public Vec3 spot(int index, int age) {
        double angle = Math.toRadians(age * speed + index * 360.0 / count);
        return owner.position().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }

    // ---------- asking / stopping ----------

    public static boolean isOpen(LivingEntity owner, String name) {
        return find(owner, name) != null;
    }

    /** Ticks until it ends (0 = there is none). */
    public static int ticksLeft(LivingEntity owner, String name) {
        Orbit orbit = find(owner, name);
        return orbit == null ? 0 : Math.max(1, orbit.lasts - orbit.age);
    }

    /** How many bodies are there right now (not away after a catch). 0 = there is no such orbit. */
    public static int ready(LivingEntity owner, String name) {
        Orbit orbit = find(owner, name);
        if (orbit == null) return 0;
        int ready = 0;
        for (int i = 0; i < orbit.count; i++) if (orbit.isHere(i)) ready++;
        return ready;
    }

    /** How many ticks it has been open (what onTick is given), or -1 if there is none. */
    public static int age(LivingEntity owner, String name) {
        Orbit orbit = find(owner, name);
        return orbit == null ? -1 : orbit.age;
    }

    /** The first body that is here (not away), or -1 (none is, or there is no such orbit). */
    public static int firstReady(LivingEntity owner, String name) {
        Orbit orbit = find(owner, name);
        if (orbit == null) return -1;
        for (int i = 0; i < orbit.count; i++) if (orbit.isHere(i)) return i;
        return -1;
    }

    /**
     * Sends body 'index' away now: thrown, launched, spent. It is back 'awayTicks' later (onBack runs, as after a
     * catch), or never in this orbit if 'awayTicks' is 0 or less. Returns where it was when it left (fire the Shot
     * from there), or null if there is no such orbit or that body is away already. The orbit draws nothing: take
     * its model away yourself.
     */
    public static Vec3 release(LivingEntity owner, String name, int index, int awayTicks) {
        Orbit orbit = find(owner, name);
        if (orbit == null || index < 0 || index >= orbit.count || !orbit.isHere(index)) return null;
        Vec3 at = orbit.spot(index, orbit.age);
        orbit.awayUntil[index] = awayTicks > 0 ? orbit.age + awayTicks : Integer.MAX_VALUE;
        return at;
    }

    /** Ends it now (its onEnd runs). Does nothing if there is none. */
    public static void close(LivingEntity owner, String name) {
        Orbit orbit = find(owner, name);
        if (orbit != null) orbit.end();
    }

    private static Orbit find(LivingEntity owner, String name) {
        for (Orbit orbit : OPEN) {
            if (!orbit.closed && orbit.name.equals(name) && orbit.owner.getUUID().equals(owner.getUUID())) return orbit;
        }
        return null;
    }

    // ---------- internals ----------

    private boolean isHere(int index) {
        return awayUntil[index] <= age;
    }

    /** The ready body nearest to a point, or -1. */
    private int nearestReady(Vec3 to) {
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            if (!isHere(i)) continue;
            double distance = spot(i, age).distanceToSqr(to);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private boolean isHostile(Entity shooter) {
        return shooter == null || !(shooter instanceof LivingEntity living) || !Spells.isFriend(owner, living);
    }

    /** Body 'index' takes this projectile: it is gone and the body is away for a while. */
    private void take(int index, Entity projectile, Vec3 at) {
        awayUntil[index] = age + Math.max(1, returnTicks);
        projectile.discard();
        if (onCatch != null) onCatch.caught(index, projectile, at);
    }

    private void tick() {
        if (!Spells.casterStillHere(owner, level)) {
            end();
            return;
        }
        age++;
        if (age >= lasts) {
            end();
            return;
        }
        for (int i = 0; i < count; i++) {                                    // back from a catch
            if (awayUntil[i] > 0 && awayUntil[i] == age) {
                awayUntil[i] = 0;
                if (onBack != null) onBack.back(i, lasts - age, age);
            }
        }
        if (onTick != null) onTick.accept(age);
        if (returnTicks >= 0) catchProjectiles();
        if (strike != null && age - lastStrike >= strikeEvery) strikeNearest();
    }

    private void catchProjectiles() {
        AABB around = owner.getBoundingBox().inflate(radius + 1.0);
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, around, Entity::isAlive)) {
            if (projectile.getDeltaMovement().lengthSqr() < 0.01) continue;           // lying still or stuck in a wall
            if (!isHostile(projectile.getOwner())) continue;
            int index = nearestReady(projectile.position());
            if (index < 0) return;                                                    // none left to catch it
            take(index, projectile, projectile.position());
        }
    }

    private void strikeNearest() {
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        Vec3 middle = owner.getBoundingBox().getCenter();
        for (LivingEntity enemy : Spells.enemiesAround(owner, level, middle, strikeRange + owner.getBbWidth() * 0.5)) {
            double distance = enemy.distanceToSqr(owner);
            if (distance < best) {
                best = distance;
                nearest = enemy;
            }
        }
        if (nearest == null) return;
        int index = nearestReady(nearest.getBoundingBox().getCenter());
        if (index < 0) return;
        lastStrike = age;
        strike.hit(nearest, index);
    }

    private void end() {
        if (closed) return;
        closed = true;
        OPEN.remove(this);
        if (onEnd == null) return;
        try {
            onEnd.run();
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("An orbit failed while ending", e);
        }
    }

    private void tickSafely() {
        try {
            tick();
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("An orbit failed", e);
            end();
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onOrbitLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel level) || OPEN.isEmpty()) return;
        for (Orbit orbit : new ArrayList<>(OPEN)) {
            if (orbit.level == level && !orbit.closed) orbit.tickSafely();
        }
    }

    // a projectile too fast to be caught on its way in: the hit on the owner does not land while a body is ready
    @SubscribeEvent(priority = Priority.HIGH)
    public static boolean onOrbitAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || OPEN.isEmpty()) return false;
        DamageSource source = event.getSource();
        if (!source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() == null) return false;
        for (Orbit orbit : OPEN) {
            if (orbit.closed || orbit.returnTicks < 0 || orbit.owner != entity) continue;
            if (!orbit.isHostile(source.getEntity())) continue;
            int index = orbit.nearestReady(source.getDirectEntity().position());
            if (index < 0) continue;
            orbit.take(index, source.getDirectEntity(), source.getDirectEntity().position());
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onOrbitLevelUnload(LevelEvent.Unload event) {
        OPEN.removeIf(orbit -> orbit.level == event.getLevel());
    }
}
