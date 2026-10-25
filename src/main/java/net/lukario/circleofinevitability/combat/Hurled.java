package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Hurled: a loose thing in the world (a dropped item, an arrow, an armor stand, a boat) lifted and thrown at an
 * enemy by a power, the way a poltergeist or a puppeteer throws things. Find them with Spells.looseThingsAround:
 *
 *   for (Entity thing : Spells.looseThingsAround(player, sl, player.position(), 16)) {
 *       Hurled.at(thing, player, enemy)
 *               .risesFor(12)                          // it floats up first, then flies (default 0)
 *               .speed(1.4)                            // blocks a tick (default 1.2)
 *               .damage(10f)                           // what it does to the first enemy it touches (default 6)
 *               .lasts(60)                             // ticks of flight before it gives up and falls (default 60)
 *               .onLaunch(t -> ...)                    // it starts flying: a sound
 *               .onTrail(t -> ...)                     // every tick it flies: a trail
 *               .onHit((t, victim) -> ...)             // it hit (after the damage): a burst
 *               .launch(sl);
 *   }
 *
 * Every part after at(...) is optional; build a new one for every thing thrown.
 *
 * WHAT IT DOES. A thing it carries itself flies straight at the target's middle every tick, without gravity, and
 * hits the first of the thrower's enemies it touches (Spells.strike, so the hit is the thrower's), then drops where
 * it hit. It falls when it bumps into a wall, when its time is up or when the target is gone. A dropped item cannot
 * be picked up while it flies. A projectile is different: it becomes the thrower's (its owner changes) and is shot
 * at the target to fly and hit as it always would, so damage and onHit are not used for it (onLaunch and onTrail
 * are). An arrow stuck in a wall is pulled out first. Nothing here touches blocks.
 *
 *   Hurled.isFlying(entity)      // being thrown right now (looseThingsAround leaves these out)
 *
 * Not saved: if the server stops mid-flight, the thing simply falls where it was.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Hurled {

    /** How fast a thing floats up while it rises (blocks a tick). */
    private static final double RISE_SPEED = 0.06;
    /** Projectiles are shot again for this many ticks, so an arrow pulled from a wall flies straight. */
    private static final int RESHOOT_TICKS = 3;

    private static final List<Hurled> FLYING = new ArrayList<>();
    private static final Set<UUID> FLYING_IDS = new HashSet<>();

    private final Entity thing;
    private final LivingEntity owner;
    private final LivingEntity target;
    private int rises;
    private double speed = 1.2;
    private float damage = 6f;
    private int lasts = 60;
    private Consumer<Entity> onLaunch;
    private Consumer<Entity> onTrail;
    private BiConsumer<Entity, LivingEntity> onHit;

    private ServerLevel level;
    private int age;
    private boolean hadNoGravity;

    private Hurled(Entity thing, LivingEntity owner, LivingEntity target) {
        this.thing = thing;
        this.owner = owner;
        this.target = target;
    }

    /** 'thing' is thrown by 'owner' at 'target'. Nothing happens until .launch(...). */
    public static Hurled at(Entity thing, LivingEntity owner, LivingEntity target) {
        return new Hurled(thing, owner, target);
    }

    public Hurled risesFor(int ticks) { this.rises = Math.max(0, ticks); return this; }
    public Hurled speed(double blocksPerTick) { this.speed = Math.max(0.1, blocksPerTick); return this; }
    public Hurled damage(float amount) { this.damage = Math.max(0f, amount); return this; }
    public Hurled lasts(int ticks) { this.lasts = Math.max(1, ticks); return this; }
    public Hurled onLaunch(Consumer<Entity> action) { this.onLaunch = action; return this; }
    public Hurled onTrail(Consumer<Entity> action) { this.onTrail = action; return this; }
    public Hurled onHit(BiConsumer<Entity, LivingEntity> action) { this.onHit = action; return this; }

    /** Lifts it now and throws it after risesFor(...). False if it cannot be thrown (gone, already flying). */
    public boolean launch(ServerLevel sl) {
        if (thing == null || owner == null || target == null || !thing.isAlive() || thing.level() != sl) return false;
        if (FLYING_IDS.contains(thing.getUUID())) return false;
        this.level = sl;
        this.hadNoGravity = thing.isNoGravity();
        thing.setNoGravity(true);
        if (thing instanceof ItemEntity item) item.setPickUpDelay(rises + lasts + 20);
        if (thing instanceof Projectile projectile) projectile.setOwner(owner);
        FLYING.add(this);
        FLYING_IDS.add(thing.getUUID());
        return true;
    }

    /** True while the entity is being thrown. */
    public static boolean isFlying(Entity entity) {
        return entity != null && FLYING_IDS.contains(entity.getUUID());
    }

    // =========================
    // Internals
    // =========================

    /** One tick. False when it is over. */
    private boolean tick() {
        if (!thing.isAlive() || thing.level() != level || !Spells.casterStillHere(owner, level)) return false;
        int flying = age - rises;
        age++;
        if (flying < 0) {                                             // still rising
            thing.setDeltaMovement(0, RISE_SPEED, 0);
            if (thing instanceof Projectile) thing.setPos(thing.getX(), thing.getY() + RISE_SPEED, thing.getZ());
            thing.hurtMarked = true;
            return true;
        }
        if (!target.isAlive() || target.level() != level || flying >= lasts) return false;
        Vec3 aim = target.getBoundingBox().getCenter().subtract(middleOf(thing));
        if (aim.lengthSqr() < 1.0E-4) aim = Spells.UP;
        Vec3 way = aim.normalize();

        if (thing instanceof Projectile projectile) {
            if (flying == 0 && onLaunch != null) onLaunch.accept(thing);
            if (flying < RESHOOT_TICKS) {
                projectile.shoot(way.x, way.y, way.z, (float) speed, 0f);
                if (flying == RESHOOT_TICKS - 1) projectile.setNoGravity(hadNoGravity);
            }
            if (onTrail != null) onTrail.accept(thing);
            return flying < lasts;                                    // from here on it flies by itself
        }

        if (flying == 0 && onLaunch != null) onLaunch.accept(thing);
        if (flying > 0 && (thing.horizontalCollision || thing.verticalCollision && thing.getDeltaMovement().y > 0)) return false;
        thing.setDeltaMovement(way.scale(speed));
        thing.hurtMarked = true;
        if (onTrail != null) onTrail.accept(thing);

        AABB reach = thing.getBoundingBox().inflate(0.35).expandTowards(way.scale(speed * 0.5));
        LivingEntity hit = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity one : level.getEntitiesOfClass(LivingEntity.class, reach, LivingEntity::isAlive)) {
            if (one == owner || one == thing || !Spells.isEnemy(owner, one)) continue;
            double distance = one.distanceToSqr(thing);
            if (distance < best) {
                best = distance;
                hit = one;
            }
        }
        if (hit == null) return true;
        if (damage > 0f) Spells.strike(owner, hit, damage);
        if (onHit != null) onHit.accept(thing, hit);
        thing.setDeltaMovement(way.scale(-0.15).add(0, 0.2, 0));          // it bounces off and drops
        return false;
    }

    private void end() {
        FLYING_IDS.remove(thing.getUUID());
        if (!thing.isAlive()) return;
        if (!(thing instanceof Projectile)) {
            thing.setNoGravity(hadNoGravity);
            thing.hurtMarked = true;
        }
        if (thing instanceof ItemEntity item) item.setPickUpDelay(10);
    }

    private static Vec3 middleOf(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.5, 0);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onHurledLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (FLYING.isEmpty() || !(event.level() instanceof ServerLevel level)) return;
        for (Hurled hurled : new ArrayList<>(FLYING)) {
            if (hurled.level != level) continue;
            boolean going;
            try {
                going = hurled.tick();
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A hurled thing failed", e);
                going = false;
            }
            if (!going) {
                FLYING.remove(hurled);
                hurled.end();
            }
        }
    }

    @SubscribeEvent
    public static void onHurledLevelUnload(LevelEvent.Unload event) {
        FLYING.removeIf(hurled -> {
            if (hurled.level != event.getLevel()) return false;
            FLYING_IDS.remove(hurled.thing.getUUID());
            return true;
        });
    }
}
