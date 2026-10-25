package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * Study: how well one caster knows one entity, from 0 to 100 percent. Every caster has a gauge of their own on
 * every entity; two casters never share theirs. The more a caster knows, the harder everything they do to it hits:
 * every 10% is BONUS_PER_TEN more damage (50% at most), for melee and abilities alike. Left alone for IDLE_TICKS,
 * the gauge falls DECAY_PER_SECOND a second. Knowledge Emperor is built on it (docs/KNOWLEDGE_EMPEROR.md, "Study").
 *
 *   Study.add(player, target, 25);                 // a Pry: 25% more (100 at most). Returns the new value
 *   Study.strike(player, target, 20f, 10);         // Spells.strike (with the bonus), then 10% more
 *   int known = Study.get(player, target);         // 0..100
 *   if (Study.knowsWeakness(player, target)) ...   // 50% or more
 *   if (Study.isUnderstood(player, target)) ...    // 100%
 *   Study.hold(player, target, 100, 160);          // at least 100% for 8 seconds, whatever it was (a weak spot named)
 *   Study.set(player, target, 100);                // straight to a value (a pulse of knowledge)
 *   int had = Study.take(player, target);          // all of it gathered up and gone (it was condensed)
 *   List<Study.Known> all = Study.takeAround(player, sl, center, 32);
 *   List<Study.Known> known = Study.knownBy(player);   // everything this caster has studied now
 *   Study.forget(player, target);   Study.forgetAll(player);
 *
 * Rules other code adds once, in a static block:
 *   Study.addImmunity(entity -> ...)               // nobody can build Study on it (a Sage's mind, a hidden body)
 *   Study.addDecayScale(caster -> 0.5)             // this caster's gauges fall at half the speed
 *   Study.addListener(new Study.Listener() {...})  // the look of it: an eye over the head that opens as it grows
 *
 * The bonus is put on in LivingHurtEvent for every hit whose attacker (also the owner of a projectile) has a gauge
 * on the one hurt, so nothing else has to multiply anything. Not saved: a restart, a death or a logout forgets it.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Study {

    // =========================
    // Settings
    // =========================

    /** Full knowledge. */
    public static final int FULL = 100;
    /** From here the caster knows its weakness. */
    public static final int WEAKNESS = 50;
    /** Left alone this long (no gain), the gauge starts to fall... */
    public static int IDLE_TICKS = 200;
    /** ...this much every second. */
    public static float DECAY_PER_SECOND = 5f;
    /** Every full 10% the caster knows adds this much to the damage they deal it... */
    public static float BONUS_PER_TEN = 0.05f;
    /** ...up to this much. */
    public static float MOST_BONUS = 0.5f;

    // =========================
    // Hooks
    // =========================

    /** What a caster knows about one entity, as handed out by knownBy and takeAround. */
    public record Known(LivingEntity target, int percent) {}

    /** The look and sound of Study (the ability decides; Study itself shows nothing). */
    public interface Listener {
        /** The gauge moved from one whole percent to another (also when it fell). */
        default void changed(LivingEntity caster, LivingEntity target, int before, int after) {}

        /** The caster forgot it: taken, forgotten, it fell to 0, or it died. */
        default void forgotten(LivingEntity caster, LivingEntity target) {}
    }

    private static final List<Listener> LISTENERS = new ArrayList<>();
    private static final List<Predicate<LivingEntity>> IMMUNITIES = new ArrayList<>();
    private static final List<ToDoubleFunction<Player>> DECAY_SCALES = new ArrayList<>();

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    /** Nobody can build Study on what this names (it can still be hit, and what is known already falls as usual). */
    public static void addImmunity(Predicate<LivingEntity> immune) {
        IMMUNITIES.add(immune);
    }

    /** How fast this caster's gauges fall, times the usual (0.5 = half as fast). Several rules multiply. */
    public static void addDecayScale(ToDoubleFunction<Player> scale) {
        DECAY_SCALES.add(scale);
    }

    // =========================
    // What is known: target -> (caster -> gauge). Not saved.
    // =========================

    private static final class Gauge {
        final LivingEntity target;
        float percent;
        long lastGain;
        int floor;                  // Study.hold: at least this much...
        long floorUntil;            // ...until this game time
        int shown;                  // the value the listeners were last told

        Gauge(LivingEntity target) {
            this.target = target;
        }

        int value(long now) {
            int base = (int) Math.floor(percent + 1.0E-4f);
            return now < floorUntil ? Math.max(base, floor) : base;
        }
    }

    private static final Map<UUID, Map<UUID, Gauge>> KNOWN = new HashMap<>();

    private Study() {}

    // =========================
    // Asking
    // =========================

    /** What this caster knows of it, 0..100. */
    public static int get(LivingEntity caster, Entity target) {
        if (caster == null || target == null) return 0;
        Gauge gauge = gauge(caster, target, false);
        return gauge == null ? 0 : gauge.value(target.level().getGameTime());
    }

    /** 50% or more: the caster knows its weakness. */
    public static boolean knowsWeakness(LivingEntity caster, Entity target) {
        return get(caster, target) >= WEAKNESS;
    }

    /** 100%: Understood. */
    public static boolean isUnderstood(LivingEntity caster, Entity target) {
        return get(caster, target) >= FULL;
    }

    /** What everything this caster does to it is multiplied by: 1 + BONUS_PER_TEN for every full 10%, MOST_BONUS at most. */
    public static float bonus(LivingEntity caster, Entity target) {
        int tens = get(caster, target) / 10;
        return 1f + Math.min(MOST_BONUS, tens * BONUS_PER_TEN);
    }

    /** True if nobody can build Study on it right now (addImmunity). */
    public static boolean isImmune(LivingEntity target) {
        for (Predicate<LivingEntity> immune : IMMUNITIES) {
            if (immune.test(target)) return true;
        }
        return false;
    }

    /** Everything this caster knows something about right now (alive, in the caster's world), most known first. */
    public static List<Known> knownBy(LivingEntity caster) {
        List<Known> found = new ArrayList<>();
        long now = caster.level().getGameTime();
        for (Map<UUID, Gauge> byCaster : KNOWN.values()) {
            Gauge gauge = byCaster.get(caster.getUUID());
            if (gauge == null || !gauge.target.isAlive() || gauge.target.level() != caster.level()) continue;
            int value = gauge.value(now);
            if (value > 0) found.add(new Known(gauge.target, value));
        }
        found.sort((a, b) -> Integer.compare(b.percent(), a.percent()));
        return found;
    }

    // =========================
    // Changing
    // =========================

    /** 'percent' more (or less, when negative), kept within 0..100. Returns the new value. Nothing on the immune or on yourself. */
    public static int add(LivingEntity caster, LivingEntity target, float percent) {
        if (!canStudy(caster, target)) return get(caster, target);
        Gauge gauge = gauge(caster, target, true);
        long now = target.level().getGameTime();
        gauge.percent = Math.max(0f, Math.min(FULL, gauge.percent + percent));
        if (percent > 0) gauge.lastGain = now;
        return settle(caster, gauge, now);
    }

    /** Straight to a value (0..100), as if just studied. */
    public static int set(LivingEntity caster, LivingEntity target, int percent) {
        if (!canStudy(caster, target)) return get(caster, target);
        Gauge gauge = gauge(caster, target, true);
        long now = target.level().getGameTime();
        gauge.percent = Math.max(0, Math.min(FULL, percent));
        gauge.lastGain = now;
        return settle(caster, gauge, now);
    }

    /**
     * At least 'percent' for 'ticks', whatever the gauge itself says; afterwards it is what it was (it did not fall
     * meanwhile either). For "the weak spot is named for 8 seconds", "Understood for 10 seconds". Works on the immune too:
     * it is a power over that one entity, not study.
     */
    public static void hold(LivingEntity caster, LivingEntity target, int percent, int ticks) {
        if (caster == null || target == null || caster == target) return;
        Gauge gauge = gauge(caster, target, true);
        long now = target.level().getGameTime();
        gauge.floor = Math.max(0, Math.min(FULL, percent));
        gauge.floorUntil = now + ticks;
        settle(caster, gauge, now);
    }

    /** All of it gathered up: returns what was known and forgets it. */
    public static int take(LivingEntity caster, LivingEntity target) {
        Gauge gauge = gauge(caster, target, false);
        if (gauge == null) return 0;
        int had = gauge.value(target.level().getGameTime());
        forget(caster, target);
        return had;
    }

    /** take() for everything within 'radius' of a point (its body). Returns what was known of each, most known first. */
    public static List<Known> takeAround(LivingEntity caster, ServerLevel sl, Vec3 center, double radius) {
        List<Known> taken = new ArrayList<>();
        for (Known known : knownBy(caster)) {
            if (known.target().level() != sl || Spells.distanceToBody(known.target(), center) > radius) continue;
            taken.add(new Known(known.target(), take(caster, known.target())));
        }
        return taken;
    }

    /** Spells.strike (with the Study bonus, as every hit gets), then 'gain' percent more. */
    public static void strike(LivingEntity caster, LivingEntity target, float damage, float gain) {
        Spells.strike(caster, target, damage);
        if (target.isAlive()) add(caster, target, gain);
    }

    public static void forget(LivingEntity caster, LivingEntity target) {
        Map<UUID, Gauge> byCaster = KNOWN.get(target.getUUID());
        if (byCaster == null || byCaster.remove(caster.getUUID()) == null) return;
        if (byCaster.isEmpty()) KNOWN.remove(target.getUUID());
        for (Listener listener : LISTENERS) listener.forgotten(caster, target);
    }

    /** Everything this caster knows is forgotten (they left, died, or are no longer of the Pathway). */
    public static void forgetAll(LivingEntity caster) {
        for (Known known : knownBy(caster)) forget(caster, known.target());
        // what is in another world or already dead is dropped without a word
        Iterator<Map<UUID, Gauge>> iterator = KNOWN.values().iterator();
        while (iterator.hasNext()) {
            Map<UUID, Gauge> byCaster = iterator.next();
            byCaster.remove(caster.getUUID());
            if (byCaster.isEmpty()) iterator.remove();
        }
    }

    // =========================
    // Internals
    // =========================

    private static boolean canStudy(LivingEntity caster, LivingEntity target) {
        if (caster == null || target == null || target.level().isClientSide()) return false;
        if (caster == target || caster.getUUID().equals(target.getUUID()) || !target.isAlive()) return false;
        return !isImmune(target);
    }

    private static Gauge gauge(LivingEntity caster, Entity target, boolean make) {
        Map<UUID, Gauge> byCaster = KNOWN.get(target.getUUID());
        if (byCaster == null) {
            if (!make || !(target instanceof LivingEntity living)) return null;
            byCaster = new HashMap<>();
            KNOWN.put(target.getUUID(), byCaster);
            Gauge gauge = new Gauge(living);
            byCaster.put(caster.getUUID(), gauge);
            return gauge;
        }
        Gauge gauge = byCaster.get(caster.getUUID());
        if (gauge == null && make && target instanceof LivingEntity living) {
            gauge = new Gauge(living);
            byCaster.put(caster.getUUID(), gauge);
        }
        return gauge;
    }

    /** Tells the listeners what changed since they were last told, and forgets a gauge that is empty. Returns the value now. */
    private static int settle(LivingEntity caster, Gauge gauge, long now) {
        int after = gauge.value(now);
        int before = gauge.shown;
        gauge.shown = after;
        if (after != before) {
            for (Listener listener : LISTENERS) listener.changed(caster, gauge.target, before, after);
        }
        if (after <= 0 && gauge.percent <= 0f && now >= gauge.floorUntil) forget(caster, gauge.target);
        return after;
    }

    private static double decayScale(LivingEntity caster) {
        double scale = 1.0;
        if (!(caster instanceof Player player)) return scale;                 // the rules are a player's
        for (ToDoubleFunction<Player> rule : DECAY_SCALES) scale *= Math.max(0.0, rule.applyAsDouble(player));
        return scale;
    }

    /** Once a second for each world: what was left alone falls, what ran out of its hold drops back. */
    private static void decay(ServerLevel sl) {
        long now = sl.getGameTime();
        List<Runnable> after = new ArrayList<>();             // listeners run after the walk (they may forget things)
        Iterator<Map.Entry<UUID, Map<UUID, Gauge>>> targets = KNOWN.entrySet().iterator();
        while (targets.hasNext()) {
            Map.Entry<UUID, Map<UUID, Gauge>> entry = targets.next();
            Iterator<Map.Entry<UUID, Gauge>> casters = entry.getValue().entrySet().iterator();
            while (casters.hasNext()) {
                Map.Entry<UUID, Gauge> known = casters.next();
                Gauge gauge = known.getValue();
                if (gauge.target.level() != sl) continue;
                if (!gauge.target.isAlive() || gauge.target.isRemoved()) {
                    casters.remove();
                    continue;
                }
                LivingEntity caster = sl.getEntity(known.getKey()) instanceof LivingEntity found ? found : null;   // a player or a Beyonder mob
                if (caster == null) continue;                      // in another world for now: it waits
                long idleSince = Math.max(gauge.lastGain, gauge.floorUntil);   // a hold keeps it from falling
                if (now >= gauge.floorUntil && now - idleSince >= IDLE_TICKS) {
                    gauge.percent = Math.max(0f, gauge.percent - (float) (DECAY_PER_SECOND * decayScale(caster)));
                }
                if (gauge.value(now) != gauge.shown || gauge.value(now) <= 0) after.add(() -> settle(caster, gauge, now));
            }
            if (entry.getValue().isEmpty()) targets.remove();
        }
        for (Runnable run : after) run.run();
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onStudyLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel sl) || KNOWN.isEmpty() || sl.getGameTime() % 20 != 0) return;
        decay(sl);
    }

    /** The bonus: everything a caster does to what they know hits harder. */
    @SubscribeEvent
    public static void onStudyHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || KNOWN.isEmpty()) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity caster) || caster == target) return;
        float bonus = bonus(caster, target);
        if (bonus > 1f) event.setAmount(event.getAmount() * bonus);
    }

    @SubscribeEvent
    public static void onStudyDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide() || KNOWN.isEmpty()) return;
        Map<UUID, Gauge> byCaster = KNOWN.remove(dead.getUUID());
        if (byCaster != null && dead.level() instanceof ServerLevel sl) {
            for (UUID id : byCaster.keySet()) {
                LivingEntity caster = sl.getEntity(id) instanceof LivingEntity found ? found : null;
                if (caster != null) for (Listener listener : LISTENERS) listener.forgotten(caster, dead);
            }
        }
        forgetAll(dead);                                              // a dead scholar forgets too
    }

    @SubscribeEvent
    public static void onStudyLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        forgetAll(player);
        KNOWN.remove(player.getUUID());                               // and what others knew of them
    }
}
