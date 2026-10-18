package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A dome: a shell round a spot that holds a line. Things cannot cross it the wrong way: enemies are kept out
 * (or in), projectiles are stopped at it, and those it shelters take less harm. For a shield of light, a barrier,
 * a cage, the walls of a kingdom:
 *
 *   Dome.at(ground, 6)                      // a half sphere standing on the ground, 6 blocks round (Dome.sphere: a whole one)
 *           .lasts(300)                     // ticks (default 200)
 *           .keepsEnemiesOut()              // enemies cannot come in, and those inside are put out
 *           .stopsProjectiles()             // projectiles from outside end at the shell
 *           .shields(0.6f)                  // allies inside take 60% less damage...
 *           .wardsEffects()                 // ...and no harmful potion effects can be put on them
 *           .holdsOwner()                   // the owner cannot leave it (braced)
 *           .onBlock(at -> ...)             // something was stopped at the shell, at this spot: a ripple, a sound
 *           .onEnd(() -> ...)
 *           .raise(player, sl, "my_pathway_dome");
 *
 * Every part after at(...) is optional. Build a new one for every cast, so don't store one in a constant.
 *
 * WHO IS WHO. Its "outsiders" are the owner's enemies (Spells.isEnemy) unless .outsiders(...) names others, e.g.
 * entity -> !Spells.isFriend(player, entity) to keep out concealed enemies as well. Everyone else is an ally:
 * allies cross the shell freely both ways and so do their projectiles.
 *
 *   .keepsEnemiesOut()   an outsider inside is put out at the shell, one outside cannot come in
 *   .keepsEnemiesIn()    nothing crosses either way: those inside stay in, those outside stay out (a cage)
 *   .stopsProjectiles()  a projectile of an outsider (or of nobody) that would cross the shell ends there;
 *                        with keepsEnemiesIn also on its way out
 *
 * Crossing is checked every tick from where each one is, so walking, being knocked, an ender pearl or any other
 * teleport all count. Someone put back stands just on their side of the shell and is pushed off it.
 *
 *   Dome.isRaised(player, name)      Dome.ticksLeft(player, name)      Dome.close(player, name)
 *   Dome.isInside(player, name, entity)                                Dome.center(player, name)
 *   Dome.weaken(player, name, 60)    // for 3 seconds: projectiles go through and the shield is halved
 *   Dome.renew(player, name)         // its whole time again
 *   Dome.raisedAt(sl, spot)          Dome.crossedBy(sl, from, to)       // anyone's domes at a spot / on a line
 *   Dome.flip(player, name)          // turned inside out: now a cage for its own owner (Duke of Entropy's Distort)
 *
 * Raising one with a name that player already has closes the old one first. It ends by itself when its time is
 * up, when .keepWhile(...) says false, or when its owner dies, logs out or changes dimension. It has no look of
 * its own: draw it when you raise it (HandOfGodFx.dome and sphere are made for this) and clear the look in onEnd.
 * Not saved: a server restart drops it.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Dome {

    /** How far outside the shell things are looked at (blocks). */
    private static final double MARGIN = 4.0;
    /** Ticks between two onBlock calls for the same entity being put back. */
    private static final int BLOCK_SHOW_EVERY = 8;

    private static final List<Dome> RAISED = new ArrayList<>();

    /** A dome standing somewhere, as Dome.raisedAt and Dome.crossedBy hand it out: whose it is and where. */
    public record Raised(Player owner, String name, Vec3 center, double radius, boolean whole) {}

    // ---------- what it is ----------

    private final Vec3 center;              // the ground under a half sphere, the middle of a whole one
    private final double radius;
    private final boolean whole;
    private int lasts = 200;
    private boolean keepsOut;
    private boolean keepsIn;
    private boolean stopsProjectiles;
    private boolean holdsOwner;
    private boolean flipped;
    private boolean wardsEffects;
    private float shield;
    private Predicate<LivingEntity> outsiders;
    private BooleanSupplier keepWhile;
    private Consumer<Vec3> onBlock;
    private Runnable onEnd;

    // ---------- while it stands ----------

    private Player owner;
    private ServerLevel level;
    private String name;
    private int age;
    private int weakUntil;
    private boolean closed = true;
    private final Map<UUID, Boolean> wasInside = new HashMap<>();
    private final Map<UUID, Integer> lastShown = new HashMap<>();

    private Dome(Vec3 center, double radius, boolean whole) {
        this.center = center;
        this.radius = Math.max(1.0, radius);
        this.whole = whole;
    }

    /** A half sphere standing on the ground: 'ground' is the middle of its floor. Nothing happens until .raise(...). */
    public static Dome at(Vec3 ground, double radius) {
        return new Dome(ground, radius, false);
    }

    /** A whole sphere round 'middle' (it may reach into the ground). */
    public static Dome sphere(Vec3 middle, double radius) {
        return new Dome(middle, radius, true);
    }

    // ---------- setting it up ----------

    public Dome lasts(int ticks) {
        this.lasts = Math.max(1, ticks);
        return this;
    }

    /** Outsiders cannot come in, and those inside when it rises are put out. */
    public Dome keepsEnemiesOut() {
        this.keepsOut = true;
        return this;
    }

    /** Outsiders cannot cross it either way: a cage. */
    public Dome keepsEnemiesIn() {
        this.keepsIn = true;
        return this;
    }

    /** Projectiles of outsiders (or of nobody) end at the shell. */
    public Dome stopsProjectiles() {
        this.stopsProjectiles = true;
        return this;
    }

    /** The owner cannot leave it (they are braced in it). */
    public Dome holdsOwner() {
        this.holdsOwner = true;
        return this;
    }

    /** Allies inside take this much less damage (0.6 = 60% less). The owner is not shielded. */
    public Dome shields(float fraction) {
        this.shield = Math.max(0f, Math.min(1f, fraction));
        return this;
    }

    /** No harmful potion effect can be put on an ally inside (the owner too). */
    public Dome wardsEffects() {
        this.wardsEffects = true;
        return this;
    }

    /** Who is kept out or in, instead of the owner's enemies. The owner never is. */
    public Dome outsiders(Predicate<LivingEntity> who) {
        this.outsiders = who;
        return this;
    }

    /** Checked every tick: the dome falls the moment it is false (a guardian who walked away). */
    public Dome keepWhile(BooleanSupplier condition) {
        this.keepWhile = condition;
        return this;
    }

    /** Runs with the spot on the shell where something was stopped: a projectile, someone put back. */
    public Dome onBlock(Consumer<Vec3> action) {
        this.onBlock = action;
        return this;
    }

    /** Runs when it falls, however it ended. */
    public Dome onEnd(Runnable action) {
        this.onEnd = action;
        return this;
    }

    /** Raises it. If this player already has one with this name, that one falls first. */
    public void raise(Player owner, ServerLevel level, String name) {
        close(owner, name);
        this.owner = owner;
        this.level = level;
        this.name = name;
        this.age = 0;
        this.weakUntil = 0;
        this.closed = false;
        this.wasInside.clear();
        RAISED.add(this);
        tickSafely();                       // those inside are sorted out at once
    }

    // ---------- asking / changing ----------

    public static boolean isRaised(Player owner, String name) {
        return find(owner, name) != null;
    }

    public static int ticksLeft(Player owner, String name) {
        Dome dome = find(owner, name);
        return dome == null ? 0 : Math.max(1, dome.lasts - dome.age);
    }

    /** The ground under it (a half sphere) or its middle (a whole one), or null. */
    public static Vec3 center(Player owner, String name) {
        Dome dome = find(owner, name);
        return dome == null ? null : dome.center;
    }

    public static boolean isInside(Player owner, String name, Entity entity) {
        Dome dome = find(owner, name);
        return dome != null && dome.contains(entity);
    }

    /** It falls now (its onEnd runs). */
    public static void close(Player owner, String name) {
        Dome dome = find(owner, name);
        if (dome != null) dome.end();
    }

    /** For 'ticks': projectiles are not stopped and the shield is halved. */
    public static void weaken(Player owner, String name, int ticks) {
        Dome dome = find(owner, name);
        if (dome != null) dome.weakUntil = Math.max(dome.weakUntil, dome.age + ticks);
    }

    public static boolean isWeakened(Player owner, String name) {
        Dome dome = find(owner, name);
        return dome != null && dome.weak();
    }

    /** Its whole time again, from now. */
    public static void renew(Player owner, String name) {
        Dome dome = find(owner, name);
        if (dome != null) {
            dome.age = 0;
            dome.weakUntil = 0;
        }
    }

    /** Every dome (anyone's) whose inside holds 'point' in this level. */
    public static List<Raised> raisedAt(ServerLevel level, Vec3 point) {
        List<Raised> found = new ArrayList<>();
        for (Dome dome : RAISED) {
            if (!dome.closed && dome.level == level && dome.contains(point)) found.add(dome.raised());
        }
        return found;
    }

    /**
     * Every dome (anyone's) whose shell the line from..to passes through, nearest first: what a player aims at
     * when they aim at a shell.  Dome.crossedBy(sl, player.getEyePosition(), Spells.aimPoint(player, sl, 24))
     */
    public static List<Raised> crossedBy(ServerLevel level, Vec3 from, Vec3 to) {
        List<Raised> found = new ArrayList<>();
        List<Double> at = new ArrayList<>();
        double length = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(length / 0.25));
        for (Dome dome : RAISED) {
            if (dome.closed || dome.level != level) continue;
            boolean start = dome.contains(from);
            for (int i = 1; i <= steps; i++) {
                if (dome.contains(from.add(to.subtract(from).scale(i / (double) steps))) != start) {
                    int place = 0;
                    while (place < at.size() && at.get(place) <= i) place++;
                    at.add(place, (double) i);
                    found.add(place, dome.raised());
                    break;
                }
            }
        }
        return found;
    }

    /**
     * The dome is turned inside out: what it kept out now keeps its owner in. From now on it is a cage for its owner
     * and the owner's friends (neither can cross it, nor can their projectiles), it no longer shields or wards anyone,
     * and everyone else crosses it freely. It keeps its time and its look. False if there is no such dome or it was
     * flipped already. For an ability that turns a rule against the one who made it.
     */
    public static boolean flip(Player owner, String name) {
        Dome dome = find(owner, name);
        if (dome == null || dome.flipped) return false;
        dome.flipped = true;
        dome.keepsOut = false;
        dome.keepsIn = true;
        dome.holdsOwner = true;
        dome.stopsProjectiles = true;
        dome.shield = 0f;
        dome.wardsEffects = false;
        Player caged = dome.owner;
        dome.outsiders = entity -> Spells.isFriend(caged, entity);             // the owner's side, hidden or not
        dome.wasInside.clear();
        dome.wasInside.put(caged.getUUID(), dome.contains(caged));
        return true;
    }

    public static boolean isFlipped(Player owner, String name) {
        Dome dome = find(owner, name);
        return dome != null && dome.flipped;
    }

    private Raised raised() {
        return new Raised(owner, name, center, radius, whole);
    }

    private static Dome find(Player owner, String name) {
        for (Dome dome : RAISED) {
            if (!dome.closed && dome.name.equals(name) && dome.owner.getUUID().equals(owner.getUUID())) return dome;
        }
        return null;
    }

    // ---------- internals ----------

    private boolean weak() {
        return age < weakUntil;
    }

    private boolean isOwner(Entity entity) {
        return entity == owner || entity.getUUID().equals(owner.getUUID());
    }

    private boolean isOutsider(LivingEntity entity) {
        if (isOwner(entity)) return false;
        return outsiders != null ? outsiders.test(entity) : Spells.isEnemy(owner, entity);
    }

    /** Whether a point is inside the shell. */
    private boolean contains(Vec3 at) {
        if (!whole && at.y < center.y - 1.5) return false;
        return at.distanceToSqr(center) <= radius * radius;
    }

    private boolean contains(Entity entity) {
        return contains(entity.getBoundingBox().getCenter());
    }

    private void tick() {
        if (!Spells.casterStillHere(owner, level) || (keepWhile != null && !keepWhile.getAsBoolean())) {
            end();
            return;
        }
        if (age >= lasts) {
            end();
            return;
        }
        boolean first = age == 0;
        age++;

        AABB box = new AABB(center, center).inflate(radius + MARGIN);
        Set<UUID> seen = new HashSet<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && !e.isPassenger())) {
            boolean owner = isOwner(entity);
            if (!owner && entity instanceof Player player && player.isCreative()) continue;
            UUID id = entity.getUUID();
            seen.add(id);
            boolean inside = contains(entity);
            Boolean seenBefore = wasInside.get(id);
            // first seen: where it is when the dome rises, or outside if it turns up later (from beyond the margin
            // in one step: a teleport, so it came from outside)
            boolean before = seenBefore != null ? seenBefore : (first || owner) ? inside : false;

            if (owner) {
                if (holdsOwner && before && !inside) putBack(entity, false);          // braced: back in
                else wasInside.put(id, inside);
                continue;
            }
            if (!isOutsider(entity) || (!keepsOut && !keepsIn)) {
                wasInside.put(id, inside);
                continue;
            }
            if (keepsIn) {
                if (before != inside) putBack(entity, !before);                       // back to its own side
                else wasInside.put(id, inside);
                continue;
            }
            if (inside) putBack(entity, true);                                        // kept out
            else wasInside.put(id, false);
        }
        wasInside.keySet().retainAll(seen);

        if (stopsProjectiles && !weak()) stopProjectiles(box);
    }

    /** Puts an entity back on its side of the shell: outside = just outside it, else just inside. */
    private void putBack(LivingEntity entity, boolean outside) {
        Vec3 body = entity.getBoundingBox().getCenter();
        double halfHeight = entity.getBbHeight() * 0.5;
        double y = body.y;
        if (!outside) {                     // back in: also no higher (or lower) than the shell has room for
            double low = whole ? center.y - radius * 0.7 : center.y + halfHeight;
            y = Math.max(low, Math.min(center.y + radius * 0.7, y));
        }
        double dy = y - center.y;
        double wall = Math.sqrt(Math.max(0, radius * radius - dy * dy));        // how far out the shell is at this height
        Vec3 way = new Vec3(body.x - center.x, 0, body.z - center.z);
        way = way.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : way.normalize();
        double half = entity.getBbWidth() * 0.5;
        double reach = outside ? wall + half + 0.6 : Math.max(0.3, wall - half - 0.8);
        entity.teleportTo(center.x + way.x * reach, y - halfHeight, center.z + way.z * reach);
        entity.setDeltaMovement(way.x * (outside ? 0.35 : -0.35), 0.1, way.z * (outside ? 0.35 : -0.35));
        entity.hurtMarked = true;
        wasInside.put(entity.getUUID(), !outside);
        show(entity, new Vec3(center.x + way.x * wall, y, center.z + way.z * wall));
    }

    /** Projectiles whose next step crosses the shell the wrong way end there. */
    private void stopProjectiles(AABB box) {
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box, Entity::isAlive)) {
            Entity shooter = projectile.getOwner();
            if (shooter != null && isOwner(shooter) && !flipped) continue;
            if (shooter != null && !isOwner(shooter) && shooter instanceof LivingEntity living && !isOutsider(living)) continue;
            Vec3 now = projectile.position();
            Vec3 next = now.add(projectile.getDeltaMovement());
            boolean in = contains(now), willBeIn = contains(next);
            boolean comingIn = !in && willBeIn, goingOut = in && !willBeIn;
            if (!comingIn && !(goingOut && keepsIn)) continue;
            projectile.discard();
            if (onBlock != null) onBlock.accept(crossing(now, next));
        }
    }

    /** About where the line now..next meets the shell. */
    private Vec3 crossing(Vec3 now, Vec3 next) {
        Vec3 point = now;
        for (int i = 1; i <= 8; i++) {
            Vec3 step = now.add(next.subtract(now).scale(i / 8.0));
            if (contains(step) != contains(now)) return step;
            point = step;
        }
        return point;
    }

    private void show(Entity entity, Vec3 at) {
        if (onBlock == null) return;
        Integer last = lastShown.get(entity.getUUID());
        if (last != null && age - last < BLOCK_SHOW_EVERY) return;
        lastShown.put(entity.getUUID(), age);
        onBlock.accept(at);
    }

    private void end() {
        if (closed) return;
        closed = true;
        RAISED.remove(this);
        wasInside.clear();
        lastShown.clear();
        if (onEnd != null) {
            try {
                onEnd.run();
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A dome failed while falling", e);
            }
        }
    }

    private void tickSafely() {
        try {
            tick();
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("A dome failed", e);
            end();
        }
    }

    /** The domes that shelter this entity right now (it is an ally inside them). */
    private static List<Dome> shelters(LivingEntity entity) {
        List<Dome> found = new ArrayList<>();
        for (Dome dome : RAISED) {
            if (dome.closed || dome.level != entity.level()) continue;
            if (dome.isOutsider(entity) || !dome.contains(entity)) continue;
            found.add(dome);
        }
        return found;
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onDomeLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel level) || RAISED.isEmpty()) return;
        for (Dome dome : new ArrayList<>(RAISED)) {
            if (dome.level == level && !dome.closed) dome.tickSafely();
        }
    }

    // a projectile fast enough to get through between two ticks, or a shot from outside at point blank: the hit
    // on someone sheltered does not land when it came from an outsider standing outside
    @SubscribeEvent
    public static boolean onDomeAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || RAISED.isEmpty()) return false;
        DamageSource source = event.getSource();
        if (!source.is(DamageTypeTags.IS_PROJECTILE)) return false;
        Entity shooter = source.getEntity();
        for (Dome dome : shelters(entity)) {
            if (!dome.stopsProjectiles || dome.weak()) continue;
            if (shooter == null || dome.contains(shooter)) continue;
            if (shooter instanceof LivingEntity living && !dome.isOutsider(living)) continue;
            if (source.getDirectEntity() != null) {
                if (dome.onBlock != null) dome.onBlock.accept(dome.crossing(source.getDirectEntity().position(), entity.getBoundingBox().getCenter()));
                if (source.getDirectEntity() instanceof Projectile projectile) projectile.discard();
            }
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onDomeHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || RAISED.isEmpty()) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (Spells.isPiercing(entity)) return;                       // Spells.strikeThrough: no shield softens it
        float best = 0f;
        for (Dome dome : shelters(entity)) {
            if (dome.isOwner(entity)) continue;
            best = Math.max(best, dome.weak() ? dome.shield * 0.5f : dome.shield);
        }
        if (best > 0f) event.setAmount(event.getAmount() * (1f - best));
    }

    @SubscribeEvent
    public static void onDomeEffect(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || RAISED.isEmpty()) return;
        if (event.getEffectInstance().getEffect().value().isBeneficial()) return;
        for (Dome dome : shelters(entity)) {
            if (dome.wardsEffects) {
                event.setResult(Result.DENY);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onDomeLevelUnload(LevelEvent.Unload event) {
        RAISED.removeIf(dome -> dome.level == event.getLevel());
    }
}
