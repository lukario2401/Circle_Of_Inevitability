package net.lukario.frogerealm.combat;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * A zone: a round piece of ground that keeps doing something to whoever stands in it, and knows how long each
 * of them has been there. For cursed land, a healing circle, a storm, a slowing field, an aura around a player:
 *
 *   Zone.at(ground, 8)                              // its middle on the ground, and how far it reaches (blocks)
 *           .height(5)                              // how far above the ground it still catches someone (default 4)
 *           .lasts(300)                             // ticks (default 200)
 *           .every(10)                              // how often it acts, in ticks (default 10)
 *           .onEnter(enemy -> ...)                  // someone stepped in
 *           .onInside((enemy, ticksInside) -> ...)  // every 10 ticks, for each one in it
 *           .onLeave(enemy -> ...)                  // someone stepped out, or the zone ended with them in it
 *           .onPulse(ticksLeft -> ...)              // every 10 ticks, once: the look and the sound of it
 *           .onEnd(() -> ...)                       // its time is up, or it was closed
 *           .open(player, sl, "my_class_land");
 *
 * Every part after at(...) is optional. Build a new one for every cast (it keeps its own count), so don't
 * store one in a constant.
 *
 * WHO IT ACTS ON. The owner's enemies (Spells.isEnemy), as every other spell. For something else:
 *   .affects(entity -> !Spells.isEnemy(player, entity))     // a zone for allies. The owner is never in it;
 *                                                           // do the owner's part in onPulse if you want one
 *
 * HOW LONG THEY HAVE BEEN IN IT. 'ticksInside' starts at 0 when someone steps in and grows while they stay, so
 * a zone can get worse (or better) the longer they stay:   int stage = 1 + ticksInside / 40;
 * Stepping out does not wipe it: it falls back as fast as it grew, so someone who hops out and in again is
 * where they were. .countsTo(200) stops the count at 200, so it starts falling from there.
 *
 * A ZONE THAT MOVES. Zone.around(entity, 6) has its middle at that entity's feet and goes where it goes (an
 * aura). It ends when the entity dies.
 *
 *   Zone.isOpen(player, "my_class_land")       Zone.ticksLeft(player, "my_class_land")
 *   Zone.close(player, "my_class_land");       // ends it now (its onLeave and onEnd run)
 *   Zone.ticksInside(player, "my_class_land", enemy)
 *
 * The name tells one player's zones apart: opening one with a name that player already has open closes the old
 * one first. Start the name with your class.
 *
 * A zone ends by itself when its owner dies, logs out or goes to another dimension. It is not saved: if the
 * server stops, the zones are gone.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Zone {

    /** What a zone does to one of those inside it. ticksInside: how long it has been in the zone (0 = just came in). */
    @FunctionalInterface
    public interface Inside {
        void run(LivingEntity entity, int ticksInside);
    }

    private static final class Stay {
        LivingEntity entity;
        int ticks;
        boolean inside;
    }

    private static final List<Zone> OPEN = new ArrayList<>();

    // ---------- what it is ----------

    private final Vec3 spot;                // where it lies (a zone that does not move)
    private final Entity follows;           // or the entity it goes with
    private final double radius;
    private double above = 4.0;
    private double below = 1.5;
    private int lasts = 200;
    private int every = 10;
    private int countsTo = Integer.MAX_VALUE;
    private Predicate<LivingEntity> affects = null;
    private Consumer<LivingEntity> onEnter = null;
    private Inside onInside = null;
    private Consumer<LivingEntity> onLeave = null;
    private IntConsumer onPulse = null;
    private Runnable onEnd = null;

    // ---------- while it is open ----------

    private Player owner;
    private ServerLevel level;
    private String name;
    private int age;
    private boolean closed = true;
    private final Map<UUID, Stay> stays = new HashMap<>();

    private Zone(Vec3 spot, Entity follows, double radius) {
        this.spot = spot;
        this.follows = follows;
        this.radius = Math.max(0.5, radius);
    }

    /** A zone lying on the ground: 'ground' is its middle, 'radius' how far it reaches. Nothing happens until .open(...). */
    public static Zone at(Vec3 ground, double radius) {
        return new Zone(ground, null, radius);
    }

    /** A zone that goes with an entity: its middle is always at that entity's feet. */
    public static Zone around(Entity entity, double radius) {
        return new Zone(entity.position(), entity, radius);
    }

    // ---------- setting it up ----------

    /** How far above its ground it still catches someone, in blocks. (It always reaches 1.5 blocks below, for slopes.) */
    public Zone height(double blocks) {
        this.above = Math.max(0.5, blocks);
        return this;
    }

    /** How long it stays, in ticks. */
    public Zone lasts(int ticks) {
        this.lasts = Math.max(1, ticks);
        return this;
    }

    /** How often it acts, in ticks: onInside for each one in it, then onPulse. */
    public Zone every(int ticks) {
        this.every = Math.max(1, ticks);
        return this;
    }

    /** The time someone has been inside is not counted past this many ticks. */
    public Zone countsTo(int ticks) {
        this.countsTo = Math.max(0, ticks);
        return this;
    }

    /** Who the zone acts on, instead of the owner's enemies. The owner themself is never one of them. */
    public Zone affects(Predicate<LivingEntity> who) {
        this.affects = who;
        return this;
    }

    /** Runs when someone steps into the zone (also for those in it when it opens). */
    public Zone onEnter(Consumer<LivingEntity> action) {
        this.onEnter = action;
        return this;
    }

    /** Runs every 'every' ticks for each one in the zone, with how long it has been there. */
    public Zone onInside(Inside action) {
        this.onInside = action;
        return this;
    }

    /** Runs when someone steps out of the zone, and for everyone still in it when it ends. */
    public Zone onLeave(Consumer<LivingEntity> action) {
        this.onLeave = action;
        return this;
    }

    /** Runs every 'every' ticks, once, with the ticks the zone has left: particles, rings, sounds. The first time at once. */
    public Zone onPulse(IntConsumer action) {
        this.onPulse = action;
        return this;
    }

    /** Runs when the zone is over, however it ended. */
    public Zone onEnd(Runnable action) {
        this.onEnd = action;
        return this;
    }

    /** Opens the zone. If this player already has one open with this name, that one is closed first. */
    public void open(Player owner, ServerLevel level, String name) {
        close(owner, name);
        this.owner = owner;
        this.level = level;
        this.name = name;
        this.age = 0;
        this.closed = false;
        this.stays.clear();
        OPEN.add(this);
        actSafely();                        // those standing in it are caught at once
    }

    /** Where its middle is right now. */
    public Vec3 center() {
        return follows != null ? follows.position() : spot;
    }

    // ---------- asking / stopping ----------

    public static boolean isOpen(Player owner, String name) {
        return find(owner, name) != null;
    }

    /** Ticks until the zone ends (0 = this player has no zone with this name). */
    public static int ticksLeft(Player owner, String name) {
        Zone zone = find(owner, name);
        return zone == null ? 0 : Math.max(1, zone.lasts - zone.age);
    }

    /** How long this entity has been in the zone, in ticks (0 = it is not in it, or there is no such zone). */
    public static int ticksInside(Player owner, String name, Entity entity) {
        Zone zone = find(owner, name);
        if (zone == null || entity == null) return 0;
        Stay stay = zone.stays.get(entity.getUUID());
        return stay == null || !stay.inside ? 0 : stay.ticks;
    }

    /** Ends the zone now (its onLeave and onEnd run). Does nothing if there is none. */
    public static void close(Player owner, String name) {
        Zone zone = find(owner, name);
        if (zone != null) zone.end();
    }

    private static Zone find(Player owner, String name) {
        for (Zone zone : OPEN) {
            if (!zone.closed && zone.name.equals(name) && zone.owner.getUUID().equals(owner.getUUID())) return zone;
        }
        return null;
    }

    // ---------- internals ----------

    private boolean isAffected(LivingEntity entity) {
        if (entity == owner || entity.getUUID().equals(owner.getUUID())) return false;
        if (!entity.isAlive() || entity.isSpectator()) return false;
        return affects != null ? affects.test(entity) : Spells.isEnemy(owner, entity);
    }

    private void tick() {
        boolean goesOn = Spells.casterStillHere(owner, level)
                && (follows == null || (follows.isAlive() && !follows.isRemoved() && follows.level() == level));
        if (!goesOn) {
            end();
            return;
        }
        age++;
        if (age >= lasts) {
            end();
            return;
        }
        if (age % every == 0) act();
    }

    /** One beat of the zone: who is in it, what happens to them, then the pulse. */
    private void act() {
        Vec3 center = center();
        AABB box = new AABB(center.x - radius, center.y - below, center.z - radius,
                center.x + radius, center.y + above, center.z + radius);

        Set<UUID> here = new HashSet<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, this::isAffected)) {
            double dx = entity.getX() - center.x;
            double dz = entity.getZ() - center.z;
            double reach = radius + entity.getBbWidth() * 0.5;          // the edge of its body counts
            if (dx * dx + dz * dz > reach * reach) continue;

            UUID id = entity.getUUID();
            here.add(id);
            Stay stay = stays.get(id);
            if (stay == null) {
                stay = new Stay();
                stays.put(id, stay);
            }
            stay.entity = entity;
            if (!stay.inside) {
                stay.inside = true;
                if (onEnter != null) onEnter.accept(entity);
                if (closed) return;                                     // what just ran closed the zone
            }
            if (onInside != null) onInside.run(entity, stay.ticks);
            if (closed) return;
            stay.ticks = Math.min(countsTo, stay.ticks + every);
        }

        // those who are not in it (any more): their time falls back as fast as it grew
        Iterator<Map.Entry<UUID, Stay>> iterator = stays.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Stay> entry = iterator.next();
            if (here.contains(entry.getKey())) continue;
            Stay stay = entry.getValue();
            if (stay.inside) {
                stay.inside = false;
                if (onLeave != null) onLeave.accept(stay.entity);
                if (closed) return;
            }
            stay.ticks -= every;
            if (stay.ticks <= 0) iterator.remove();
        }

        if (onPulse != null) onPulse.accept(lasts - age);
    }

    private void end() {
        if (closed) return;
        closed = true;
        OPEN.remove(this);
        try {
            if (onLeave != null) {
                for (Stay stay : stays.values()) {
                    if (stay.inside) onLeave.accept(stay.entity);
                }
            }
            if (onEnd != null) onEnd.run();
        } catch (Exception e) {
            ForgeRealm.LOGGER.error("A zone failed while ending", e);
        }
        stays.clear();
    }

    /** One beat. If one of its parts fails, it is logged and the zone ends (the game goes on). */
    private void actSafely() {
        try {
            act();
        } catch (Exception e) {
            ForgeRealm.LOGGER.error("A zone failed", e);
            end();
        }
    }

    private void tickSafely() {
        try {
            tick();
        } catch (Exception e) {
            ForgeRealm.LOGGER.error("A zone failed", e);
            end();
        }
    }

    @SubscribeEvent
    public static void onZoneLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (OPEN.isEmpty()) return;
        // a copy: what a zone does may open or close zones
        for (Zone zone : new ArrayList<>(OPEN)) {
            if (zone.level == level && !zone.closed) zone.tickSafely();
        }
    }

    @SubscribeEvent
    public static void onZoneLevelUnload(LevelEvent.Unload event) {
        OPEN.removeIf(zone -> zone.level == event.getLevel());
    }
}
