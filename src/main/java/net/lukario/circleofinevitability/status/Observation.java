package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Prediction;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Observation: "I have studied you". One watcher's levels on one enemy (status/Marks with an owner, so two watchers
 * keep their own levels on the same enemy), built by keeping it in sight and worn off one level at a time once the
 * watching stops. The Observer Pathway's rule (docs/pathways/observer/OBSERVER.md, "Observation"):
 *
 *   Observation.watch(viewer, target, 5);        // every check (here every 5 ticks) while 'target' is in its sight:
 *                                                // a level every BUILD_TICKS, and the levels it has stop wearing off
 *   Observation.raise(viewer, target, 1);        // a level at once (a power that reads it)
 *   Observation.level(viewer, target)            // 0..MAX
 *   int spent = Observation.spend(viewer, target);   // all of them, for a finisher
 *   Observation.watchedBy(viewer)                // every enemy it has levels on, with the level
 *
 * What the levels do, by themselves, for any watcher:
 *   1 Watched       it cannot hide from the watcher: Concealment, Unnoticed and invisibility end every second (Exposed)
 *   2 Studied       the watcher's Shots and Rays lead it (Prediction), and the watcher's ability hits on it do STUDIED_BONUS more
 *   3 Seen through  ability hits do SEEN_THROUGH_BONUS more, and it cannot teleport while within HOLD_RANGE of the watcher
 *
 * The look is the caller's: addListener((viewer, target, level) -> ...) is told every time a level changes (up or
 * down; 0 when the last one wore off). addSpeed((viewer) -> 2.0) makes a watcher build it faster (the Observer's
 * Eyes Everywhere). Saved with the entity (Marks); the "who is watching what" for the effects is rebuilt as watchers
 * watch again after a restart.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Observation {

    public static final String NAME = "observation";
    public static final int MAX = 3;
    /** Ticks of keeping it in sight for one level. */
    public static int BUILD_TICKS = 40;
    /** Ticks for one level to wear off once the watching stops. */
    public static int DROP_TICKS = 160;
    public static float STUDIED_BONUS = 0.10f;
    public static float SEEN_THROUGH_BONUS = 0.20f;
    public static double HOLD_RANGE = 24.0;

    /** Told when a watcher's level on a target changes (level 0: it wore off). */
    @FunctionalInterface
    public interface Listener {
        void changed(LivingEntity viewer, LivingEntity target, int level);
    }

    /** How much faster a watcher builds it (1 = normal). */
    @FunctionalInterface
    public interface Speed {
        double of(LivingEntity viewer);
    }

    private static final List<Listener> LISTENERS = new ArrayList<>();
    private static final List<Speed> SPEEDS = new ArrayList<>();

    /** One watcher: who it is, how long it has kept its current target in sight, and every target it has levels on. */
    private static final class Watcher {
        LivingEntity viewer;
        UUID current;
        double ticks;
        final Map<UUID, LivingEntity> targets = new LinkedHashMap<>();
        final Map<UUID, Integer> shown = new HashMap<>();
    }

    private static final Map<UUID, Watcher> WATCHERS = new HashMap<>();

    static {
        // Seen through: no teleporting near the watcher
        Root.addHold((entity, restriction) -> restriction == RootRestriction.TELEPORT && heldInSight(entity));
    }

    private Observation() {}

    // =========================
    // API
    // =========================

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    public static void addSpeed(Speed speed) {
        SPEEDS.add(speed);
    }

    /** The watcher's level on 'target' (0..MAX). */
    public static int level(Entity viewer, Entity target) {
        if (viewer == null || target == null) return 0;
        return Math.min(MAX, Marks.count(target, viewer, NAME));
    }

    /**
     * 'target' is in the watcher's sight for 'step' more ticks. Every BUILD_TICKS of it in a row (another target in
     * between starts again) is a level, and while it is watched its levels do not wear off. Returns the level now.
     */
    public static int watch(LivingEntity viewer, LivingEntity target, int step) {
        if (viewer.level().isClientSide() || target == null || !target.isAlive()) return 0;
        Watcher watcher = watcher(viewer);
        if (!target.getUUID().equals(watcher.current)) {
            watcher.current = target.getUUID();
            watcher.ticks = 0;
        }
        double speed = 1.0;
        for (Speed rule : SPEEDS) speed *= Math.max(0.0, rule.of(viewer));
        watcher.ticks += step * speed;
        int level = level(viewer, target);
        if (level > 0) Marks.add(target, viewer, NAME, 0, MAX, DROP_TICKS);   // watched: the clock starts again
        if (watcher.ticks >= BUILD_TICKS) {
            watcher.ticks -= BUILD_TICKS;
            level = raise(viewer, target, 1);
        }
        return level;
    }

    /** Stops counting toward the next level (the watcher looked away). The levels it has wear off by themselves. */
    public static void lookAway(LivingEntity viewer) {
        Watcher watcher = WATCHERS.get(viewer.getUUID());
        if (watcher == null) return;
        watcher.current = null;
        watcher.ticks = 0;
    }

    /** How far toward the next level on its current target the watcher is, 0..1. */
    public static float progress(LivingEntity viewer) {
        Watcher watcher = WATCHERS.get(viewer.getUUID());
        return watcher == null ? 0f : (float) Math.min(1.0, watcher.ticks / BUILD_TICKS);
    }

    /** 'amount' levels more at once (MAX at most). Returns the level now. */
    public static int raise(LivingEntity viewer, LivingEntity target, int amount) {
        if (viewer.level().isClientSide() || target == null || amount <= 0) return level(viewer, target);
        int before = level(viewer, target);
        int now = Marks.add(target, viewer, NAME, amount, MAX, DROP_TICKS);
        Watcher watcher = watcher(viewer);
        watcher.targets.put(target.getUUID(), target);
        if (now != before) changed(watcher, target, now);
        return now;
    }

    /** Takes all of the watcher's levels off 'target' and returns how many there were. */
    public static int spend(LivingEntity viewer, LivingEntity target) {
        int had = Math.min(MAX, Marks.take(target, viewer, NAME));
        Watcher watcher = WATCHERS.get(viewer.getUUID());
        if (watcher != null && had > 0) {
            watcher.targets.remove(target.getUUID());
            changed(watcher, target, 0);
        }
        return had;
    }

    /** Every target the watcher has levels on, with the level (alive, in its level). */
    public static Map<LivingEntity, Integer> watchedBy(LivingEntity viewer) {
        Map<LivingEntity, Integer> result = new LinkedHashMap<>();
        Watcher watcher = WATCHERS.get(viewer.getUUID());
        if (watcher == null) return result;
        for (LivingEntity target : watcher.targets.values()) {
            int level = level(viewer, target);
            if (level > 0 && target.isAlive() && target.level() == viewer.level()) result.put(target, level);
        }
        return result;
    }

    /** The highest level any watcher has on 'target' (0: nobody watches it). */
    public static int highest(Entity target) {
        int best = 0;
        for (Watcher watcher : WATCHERS.values()) {
            if (watcher.targets.containsKey(target.getUUID())) best = Math.max(best, level(watcher.viewer, target));
        }
        return best;
    }

    /** Forgets everything a watcher was watching (it died, logged out): the levels on the enemies wear off by themselves. */
    public static void forget(LivingEntity viewer) {
        Watcher watcher = WATCHERS.remove(viewer.getUUID());
        if (watcher == null) return;
        for (LivingEntity target : watcher.targets.values()) {
            for (Listener listener : LISTENERS) listener.changed(viewer, target, 0);
        }
    }

    /** True while some watcher has Seen through it and stands within HOLD_RANGE. */
    public static boolean heldInSight(Entity entity) {
        if (WATCHERS.isEmpty()) return false;
        for (Watcher watcher : WATCHERS.values()) {
            if (!watcher.targets.containsKey(entity.getUUID())) continue;
            LivingEntity viewer = watcher.viewer;
            if (viewer == null || !viewer.isAlive() || viewer.level() != entity.level()) continue;
            if (viewer.distanceToSqr(entity) > HOLD_RANGE * HOLD_RANGE) continue;
            if (level(viewer, entity) >= MAX) return true;
        }
        return false;
    }

    // =========================
    // Inside
    // =========================

    private static Watcher watcher(LivingEntity viewer) {
        Watcher watcher = WATCHERS.computeIfAbsent(viewer.getUUID(), id -> new Watcher());
        watcher.viewer = viewer;
        return watcher;
    }

    private static void changed(Watcher watcher, LivingEntity target, int level) {
        watcher.shown.put(target.getUUID(), level);
        for (Listener listener : List.copyOf(LISTENERS)) listener.changed(watcher.viewer, target, level);
    }

    /** Once a second: what the levels do, and telling the listeners about levels that wore off. */
    @SubscribeEvent
    public static void onObservationLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel sl) || sl.getGameTime() % 20 != 0 || WATCHERS.isEmpty()) return;
        for (Iterator<Watcher> it = WATCHERS.values().iterator(); it.hasNext(); ) {
            Watcher watcher = it.next();
            LivingEntity viewer = watcher.viewer;
            if (viewer == null || viewer.isRemoved() && !(viewer instanceof net.minecraft.world.entity.player.Player)) {
                it.remove();
                continue;
            }
            if (viewer.level() != sl) continue;
            boolean leads = false;
            for (Iterator<Map.Entry<UUID, LivingEntity>> t = watcher.targets.entrySet().iterator(); t.hasNext(); ) {
                LivingEntity target = t.next().getValue();
                int level = target.isAlive() ? level(viewer, target) : 0;
                Integer was = watcher.shown.get(target.getUUID());
                if (was == null || was != level) {
                    watcher.shown.put(target.getUUID(), level);
                    for (Listener listener : List.copyOf(LISTENERS)) listener.changed(viewer, target, level);
                }
                if (level <= 0) {
                    t.remove();
                    watcher.shown.remove(target.getUUID());
                    continue;
                }
                Exposed.apply(target, 30);                                  // Watched: nothing hides it
                if (level >= 2 && target.level() == viewer.level() && viewer.distanceToSqr(target) < 48 * 48) leads = true;
            }
            if (leads) Prediction.grant(viewer, 30);                         // Studied: shots lead it
        }
    }

    /** Studied and Seen through: the watcher's ability hits do more. */
    @SubscribeEvent
    public static void onObservationHurt(LivingHurtEvent event) {
        LivingEntity hurt = event.getEntity();
        if (hurt.level().isClientSide() || WATCHERS.isEmpty()) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity viewer) || !Spells.isSpellHit(hurt)) return;
        int level = level(viewer, hurt);
        if (level >= 3) event.setAmount(event.getAmount() * (1f + SEEN_THROUGH_BONUS));
        else if (level >= 2) event.setAmount(event.getAmount() * (1f + STUDIED_BONUS));
    }

    @SubscribeEvent
    public static void onObservationLevelUnload(LevelEvent.Unload event) {
        WATCHERS.values().removeIf(watcher -> watcher.viewer == null || watcher.viewer.level() == event.getLevel());
    }
}
