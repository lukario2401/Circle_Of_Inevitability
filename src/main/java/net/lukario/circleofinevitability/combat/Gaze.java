package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Gaze: who is looking at a point or an entity, and for how long. For pictures that take hold of whoever looks at them
 * (Observer's Paintings), "do not look at it" mechanics, evil eyes, a stare that builds up:
 *
 *   Gaze.looking(viewer, point, 55, 20)              // within 20 blocks, no more than 55 degrees off its look, clear line
 *   Gaze.lookingAt(viewer, entity, 30, 24)           // the same for an entity (its body or its eyes showing is enough)
 *   Gaze.watchers(sl, point, 20, 55, who)            // every living thing within 20 blocks looking at the point
 *
 * A stare that has to be held: call it on every check with whether the look is still there. It adds 'step' ticks
 * while it is, forgets everything the moment it is not, and says how full it is (1 or more: held long enough).
 *
 *   float full = Gaze.stare(viewer, "my_pathway_picture_" + id, Gaze.looking(viewer, spot, 55, 20), 5, 60);
 *   if (full >= 1f) { ...; Gaze.forget(viewer, "my_pathway_picture_" + id); }
 *
 * A mob's look is where its head points (it looks at what it hunts); a player's is the crosshair. Not saved: a stare
 * not checked for 10 seconds is forgotten, and a level that unloads forgets its stares.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Gaze {

    /** How long a stare nobody asks about is kept, in ticks. */
    private static final int FORGET_AFTER = 200;

    private static final class Held {
        int ticks;
        long at;
    }

    /** viewer -> key -> how long it has been held. */
    private static final Map<UUID, Map<String, Held>> STARES = new HashMap<>();

    private Gaze() {}

    // =========================
    // API
    // =========================

    /** True when 'viewer' is within 'range' of 'point', no more than 'halfAngle' degrees off its look, with a clear line. */
    public static boolean looking(LivingEntity viewer, Vec3 point, double halfAngle, double range) {
        Vec3 eye = viewer.getEyePosition();
        Vec3 toIt = point.subtract(eye);
        double distance = toIt.length();
        if (distance > range) return false;
        if (distance > 0.3) {
            double cos = viewer.getLookAngle().normalize().dot(toIt.scale(1.0 / distance));
            if (cos < Math.cos(Math.toRadians(halfAngle))) return false;
        }
        return clearLine(viewer, eye, point);
    }

    /** The same for an entity: its body or its eyes within the look and in clear sight. */
    public static boolean lookingAt(LivingEntity viewer, Entity subject, double halfAngle, double range) {
        if (subject == viewer || subject.level() != viewer.level()) return false;
        return looking(viewer, subject.getBoundingBox().getCenter(), halfAngle, range)
                || looking(viewer, subject.getEyePosition(), halfAngle, range);
    }

    /** Every living thing within 'radius' of 'point' that is looking at it ('who' picks which count; null: all). */
    public static List<LivingEntity> watchers(ServerLevel sl, Vec3 point, double radius, double halfAngle,
                                              Predicate<LivingEntity> who) {
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity one : sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(point, point).inflate(radius),
                e -> e.isAlive() && !e.isSpectator())) {
            if (who != null && !who.test(one)) continue;
            if (looking(one, point, halfAngle, radius)) result.add(one);
        }
        return result;
    }

    /**
     * A stare held under 'key': adds 'step' ticks while 'stillLooking', forgets it at once when not. Returns how full
     * it is: ticks held / 'needed' (1 or more: held long enough). 'needed' 0 or less: full at once.
     */
    public static float stare(Entity viewer, String key, boolean stillLooking, int step, int needed) {
        if (!stillLooking) {
            forget(viewer, key);
            return 0f;
        }
        if (needed <= 0) return 1f;
        Held held = STARES.computeIfAbsent(viewer.getUUID(), id -> new HashMap<>()).computeIfAbsent(key, k -> new Held());
        held.ticks += Math.max(0, step);
        held.at = viewer.level().getGameTime();
        return held.ticks / (float) needed;
    }

    /** How full a stare is now without adding to it (0: none). */
    public static int held(Entity viewer, String key) {
        Map<String, Held> mine = STARES.get(viewer.getUUID());
        Held held = mine == null ? null : mine.get(key);
        return held == null ? 0 : held.ticks;
    }

    /** Forgets one stare. */
    public static void forget(Entity viewer, String key) {
        Map<String, Held> mine = STARES.get(viewer.getUUID());
        if (mine == null) return;
        mine.remove(key);
        if (mine.isEmpty()) STARES.remove(viewer.getUUID());
    }

    /** Forgets every stare whose key starts with 'prefix' (a picture that is gone), whoever held it. */
    public static void forgetAll(String prefix) {
        for (Iterator<Map<String, Held>> it = STARES.values().iterator(); it.hasNext(); ) {
            Map<String, Held> mine = it.next();
            mine.keySet().removeIf(key -> key.startsWith(prefix));
            if (mine.isEmpty()) it.remove();
        }
    }

    // =========================
    // Inside
    // =========================

    private static boolean clearLine(Entity viewer, Vec3 from, Vec3 to) {
        return viewer.level().clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, viewer)).getType()
                == HitResult.Type.MISS;
    }

    @SubscribeEvent
    public static void onGazeLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel sl) || sl.getGameTime() % 100 != 0 || STARES.isEmpty()) return;
        if (sl != sl.getServer().overworld()) return;
        long now = sl.getGameTime();
        for (Iterator<Map<String, Held>> it = STARES.values().iterator(); it.hasNext(); ) {
            Map<String, Held> mine = it.next();
            mine.values().removeIf(held -> now - held.at > FORGET_AFTER);
            if (mine.isEmpty()) it.remove();
        }
    }

    @SubscribeEvent
    public static void onGazeLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel sl && sl == sl.getServer().overworld()) STARES.clear();
    }
}
