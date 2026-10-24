package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * An enclosure: a round place that the ones it keeps cannot walk out of. Whoever steps out of its edge comes back in
 * at the opposite side, as if space folded round. Key Of Stars' Space-Time Labyrinth and Alternate World; a maze, a
 * looping road, a pocket world. Not a wall (combat/Dome stops people at a shell): here they walk on and are simply
 * somewhere else. Works from any class/ability:
 *
 *   Enclosure.at(ground, 14)                        // its middle on the ground, and how far it reaches (blocks)
 *           .height(8)                              // how far above the ground it still holds someone (default 6)
 *           .lasts(300)                             // ticks (default 200)
 *           .keeps(entity -> Spells.isEnemy(player, entity))     // who it holds (default: the owner's enemies)
 *           .every(20)                              // how often onInside runs, in ticks (default 20)
 *           .onInside(enemy -> ...)                 // for each one held inside: a slow, a root, a mark
 *           .onLoop((enemy, from, to) -> ...)       // someone walked out at 'from' and is back in at 'to': the look
 *           .onPulse(ticksLeft -> ...)              // every 'every' ticks, once: the look and the sound of it
 *           .onEnd(() -> ...)
 *           .open(player, sl, "my_pathway_maze");
 *
 *   Enclosure.isOpen(player, name)     Enclosure.ticksLeft(player, name)     Enclosure.close(player, name)
 *   Enclosure.holds(entity)            // true while some enclosure keeps it inside
 *   Enclosure.ownersAround(sl, spot, name)   // the owners of the enclosures of that name (null: any) covering a spot
 *
 * Someone held is turned back only once they were inside: a creature that never came in can still walk in (and then
 * it is held). Only walking counts: someone who flies over its height or is carried out by a teleport is out. The
 * turning back is not their own teleport, so a Teleport root on them does not stop it.
 *
 * Opening one with a name that player already has open closes the old one first. It ends by itself when its owner
 * dies, logs out or goes to another dimension. It has no look of its own. Not saved.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Enclosure {

    /** Someone walked out at 'from' and was put back in at 'to'. */
    @FunctionalInterface
    public interface Loop {
        void looped(LivingEntity who, Vec3 from, Vec3 to);
    }

    private static final List<Enclosure> OPEN = new ArrayList<>();

    private final Vec3 center;
    private final double radius;
    private double height = 6.0;
    private int lasts = 200;
    private int every = 20;
    private Predicate<LivingEntity> keeps;
    private Consumer<LivingEntity> onInside;
    private Loop onLoop;
    private IntConsumer onPulse;
    private Runnable onEnd;

    private Player owner;
    private ServerLevel level;
    private String name;
    private int age;
    private boolean closed;
    private final Set<UUID> inside = new HashSet<>();

    private Enclosure(Vec3 center, double radius) {
        this.center = center;
        this.radius = Math.max(2.0, radius);
    }

    public static Enclosure at(Vec3 ground, double radius) {
        return new Enclosure(ground, radius);
    }

    public Enclosure height(double blocks) { this.height = Math.max(1.0, blocks); return this; }
    public Enclosure lasts(int ticks) { this.lasts = Math.max(1, ticks); return this; }
    public Enclosure every(int ticks) { this.every = Math.max(1, ticks); return this; }
    public Enclosure keeps(Predicate<LivingEntity> who) { this.keeps = who; return this; }
    public Enclosure onInside(Consumer<LivingEntity> action) { this.onInside = action; return this; }
    public Enclosure onLoop(Loop action) { this.onLoop = action; return this; }
    public Enclosure onPulse(IntConsumer action) { this.onPulse = action; return this; }
    public Enclosure onEnd(Runnable action) { this.onEnd = action; return this; }

    public void open(Player owner, ServerLevel level, String name) {
        close(owner, name);
        this.owner = owner;
        this.level = level;
        this.name = name;
        OPEN.add(this);
    }

    public Vec3 center() { return center; }
    public double radius() { return radius; }

    // =========================
    // Asking
    // =========================

    public static boolean isOpen(Player owner, String name) {
        return find(owner, name) != null;
    }

    public static int ticksLeft(Player owner, String name) {
        Enclosure open = find(owner, name);
        return open == null ? 0 : Math.max(0, open.lasts - open.age);
    }

    public static void close(Player owner, String name) {
        Enclosure open = find(owner, name);
        if (open != null) open.end();
    }

    /** True while some enclosure keeps this entity inside. */
    public static boolean holds(LivingEntity entity) {
        for (Enclosure open : OPEN) if (!open.closed && open.inside.contains(entity.getUUID())) return true;
        return false;
    }

    /** The owners of the enclosures in this level that cover 'spot' and go by 'name' (null: any name). */
    public static List<Player> ownersAround(ServerLevel sl, Vec3 spot, String name) {
        List<Player> owners = new ArrayList<>();
        for (Enclosure open : OPEN) {
            if (!open.closed && open.level == sl && open.covers(spot) && (name == null || open.name.equals(name))) owners.add(open.owner);
        }
        return owners;
    }

    private static Enclosure find(Player owner, String name) {
        for (Enclosure open : OPEN) {
            if (!open.closed && open.name.equals(name) && open.owner.getUUID().equals(owner.getUUID())) return open;
        }
        return null;
    }

    // =========================
    // Inside
    // =========================

    private boolean covers(Vec3 at) {
        double dx = at.x - center.x, dz = at.z - center.z;
        return dx * dx + dz * dz <= radius * radius && at.y >= center.y - 2.0 && at.y <= center.y + height;
    }

    private boolean isKept(LivingEntity entity) {
        if (!entity.isAlive() || entity == owner) return false;
        return keeps != null ? keeps.test(entity) : Spells.isEnemy(owner, entity);
    }

    private void tick() {
        if (closed) return;
        if (!Spells.casterStillHere(owner, level) || age >= lasts) {
            end();
            return;
        }
        age++;
        AABB box = new AABB(center, center).inflate(radius + 4.0, height + 4.0, radius + 4.0);
        Set<UUID> seen = new HashSet<>();
        for (LivingEntity one : level.getEntitiesOfClass(LivingEntity.class, box, this::isKept)) {
            UUID id = one.getUUID();
            if (covers(one.position())) {
                inside.add(id);
                seen.add(id);
                continue;
            }
            if (!inside.contains(id)) continue;
            Vec3 from = one.position();
            double dy = from.y - center.y;
            if (dy > height + 1.0 || dy < -3.0) {                       // over it or under it: out
                inside.remove(id);
                continue;
            }
            Vec3 flat = new Vec3(center.x - from.x, 0, center.z - from.z);
            if (flat.lengthSqr() < 1.0E-4) continue;
            Vec3 wanted = center.add(flat.normalize().scale(radius - 1.5)).add(0, Math.max(0.0, dy), 0);
            Vec3 to = Teleport.standNear(one, level, wanted, 3.0);
            if (to != null && Teleport.of(one).ignoringTeleportRoots().go(level, to)) {
                seen.add(id);
                if (onLoop != null) safely(() -> onLoop.looped(one, from, to));
            } else {
                inside.remove(id);                                     // nowhere to put it back: it got out
            }
        }
        inside.retainAll(seen);
        if (age % every == 0) {
            if (onPulse != null) safely(() -> onPulse.accept(Math.max(0, lasts - age)));
            if (onInside != null) {
                for (LivingEntity one : level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> inside.contains(e.getUUID()) && isKept(e))) {
                    safely(() -> onInside.accept(one));
                }
            }
        }
    }

    private void end() {
        if (closed) return;
        closed = true;
        inside.clear();
        OPEN.remove(this);
        if (onEnd != null) safely(onEnd);
    }

    private static void safely(Runnable run) {
        try {
            run.run();
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("An enclosure hook failed", e);
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onEnclosureLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel sl) || OPEN.isEmpty()) return;
        for (Enclosure open : List.copyOf(OPEN)) {
            if (open.level == sl) open.tick();
        }
    }

    @SubscribeEvent
    public static void onEnclosureLevelUnload(LevelEvent.Unload event) {
        for (Enclosure open : List.copyOf(OPEN)) {
            if (open.level == event.getLevel()) open.end();
        }
    }
}
