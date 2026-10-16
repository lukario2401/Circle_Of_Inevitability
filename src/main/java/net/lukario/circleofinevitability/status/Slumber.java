package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Slumber: sleep that works on players and mobs alike. Lullabies, a soul put to rest, a body that will not get up.
 * Works from any class/ability:
 *
 *   Slumber.put(target, 160, Slumber.Kind.SLEEP);     // 8 seconds; the first blow wakes it
 *   Slumber.put(target, 160, Slumber.Kind.DEEP);      // the first DEEP_HITS blows do not wake it
 *   Slumber.put(target, 160, Slumber.Kind.LIMP);      // awake, but it cannot attack or use abilities and crawls
 *   Slumber.put(target, 160, Slumber.Kind.ETERNAL);   // only the time or Slumber.wake ends it
 *   Slumber.isAsleep(entity)   Slumber.isLimp(entity)   Slumber.kind(entity)   Slumber.ticksLeft(entity)
 *   Slumber.wake(target)
 *   Slumber.addImmunity(entity -> ...)                // some cannot be put to sleep
 *   Slumber.addWakeListener((entity, kind) -> ...)    // it woke (hurt, its time is up, or wake): clear a look
 *
 * Asleep (SLEEP, DEEP, ETERNAL): it cannot move, jump, attack, use items or abilities (root/Root, EVERYTHING),
 * a player's screen goes dark (status/Blind) and a mob forgets what it was hunting and picks no one.
 * Limp (LIMP): it cannot attack or use abilities, jump or sprint, and moves very slowly; it is not woken by blows.
 *
 * Putting something to sleep again never makes it shorter; a new kind replaces the old one. The root part is
 * root/Root's, so wake(...) also lifts a root of the same kinds that something else put on it at the same time.
 * Saved with the entity (the root too).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Slumber {

    public enum Kind { SLEEP, DEEP, LIMP, ETERNAL }

    /** How many blows a DEEP sleep takes before it wakes (the next one after these wakes it). */
    public static int DEEP_HITS = 2;

    private static final String TAG = "circle_of_inevitability_slumber";
    private static final String UNTIL = "until";              // game time
    private static final String KIND = "kind";
    private static final String HITS = "hits";

    private static final List<Predicate<LivingEntity>> IMMUNE = new ArrayList<>();
    private static final List<BiConsumer<LivingEntity, Kind>> WAKE_LISTENERS = new ArrayList<>();

    private Slumber() {}

    // =========================
    // API
    // =========================

    /** False (and nothing happens) when it cannot be put to sleep: dead, a creative player, or immune. */
    public static boolean put(LivingEntity target, int ticks, Kind kind) {
        if (target.level().isClientSide() || ticks <= 0 || !target.isAlive()) return false;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        for (Predicate<LivingEntity> immune : IMMUNE) if (immune.test(target)) return false;

        long now = target.level().getGameTime();
        CompoundTag tag = target.getPersistentData().getCompoundOrEmpty(TAG);
        long until = Math.max(now + ticks, tag.getLongOr(UNTIL, 0L));
        tag.putLong(UNTIL, until);
        tag.putString(KIND, kind.name());
        tag.putInt(HITS, 0);
        target.getPersistentData().put(TAG, tag);

        int left = (int) (until - now);
        if (kind == Kind.LIMP) {
            Root.apply(target, left, RootRestriction.ATTACK, RootRestriction.ABILITIES, RootRestriction.JUMP,
                    RootRestriction.USE_ITEMS);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, left, 3, false, false));
            target.setSprinting(false);
        } else {
            Root.apply(target, left, RootRestriction.EVERYTHING);
            if (target instanceof Player) Blind.apply(target, left);
            if (target instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
                if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }
        }
        return true;
    }

    /** Asleep: SLEEP, DEEP or ETERNAL. */
    public static boolean isAsleep(Entity entity) {
        Kind kind = kind(entity);
        return kind != null && kind != Kind.LIMP;
    }

    public static boolean isLimp(Entity entity) {
        return kind(entity) == Kind.LIMP;
    }

    /** What sleep it is in, or null when it is awake. */
    public static Kind kind(Entity entity) {
        if (entity == null || !entity.getPersistentData().contains(TAG)) return null;
        CompoundTag tag = entity.getPersistentData().getCompoundOrEmpty(TAG);
        if (tag.getLongOr(UNTIL, 0L) <= entity.level().getGameTime()) return null;
        try {
            return Kind.valueOf(tag.getStringOr(KIND, Kind.SLEEP.name()));
        } catch (IllegalArgumentException e) {
            return Kind.SLEEP;
        }
    }

    public static int ticksLeft(Entity entity) {
        if (kind(entity) == null) return 0;
        long until = entity.getPersistentData().getCompoundOrEmpty(TAG).getLongOr(UNTIL, 0L);
        return (int) Math.max(0, until - entity.level().getGameTime());
    }

    /** Wakes it now (its wake listeners run). Does nothing when it is awake. */
    public static void wake(LivingEntity target) {
        Kind kind = kind(target);
        target.getPersistentData().remove(TAG);
        if (kind == null) return;
        lift(target, kind);
        woke(target, kind);
    }

    /** Some cannot be put to sleep (a Pathway with authority over sleep, a soothed ally). */
    public static void addImmunity(Predicate<LivingEntity> immune) {
        IMMUNE.add(immune);
    }

    /** Runs whenever something wakes: hurt, its time up, or wake(...). For clearing the look of its sleep. */
    public static void addWakeListener(BiConsumer<LivingEntity, Kind> listener) {
        WAKE_LISTENERS.add(listener);
    }

    // =========================
    // Internals
    // =========================

    private static void lift(LivingEntity target, Kind kind) {
        if (kind == Kind.LIMP) {
            Root.remove(target, RootRestriction.ATTACK, RootRestriction.ABILITIES, RootRestriction.JUMP, RootRestriction.USE_ITEMS);
            target.removeEffect(MobEffects.SLOWNESS);
        } else {
            Root.remove(target, RootRestriction.EVERYTHING.toArray(new RootRestriction[0]));
            if (target instanceof Player) Blind.remove(target);
        }
    }

    private static void woke(LivingEntity target, Kind kind) {
        for (BiConsumer<LivingEntity, Kind> listener : WAKE_LISTENERS) {
            try {
                listener.accept(target, kind);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A slumber wake listener failed", e);
            }
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onSlumberLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity.tickCount % 5 != 0 || !entity.getPersistentData().contains(TAG)) return;
        CompoundTag tag = entity.getPersistentData().getCompoundOrEmpty(TAG);
        if (tag.getLongOr(UNTIL, 0L) > entity.level().getGameTime()) return;
        Kind kind;
        try {
            kind = Kind.valueOf(tag.getStringOr(KIND, Kind.SLEEP.name()));
        } catch (IllegalArgumentException e) {
            kind = Kind.SLEEP;
        }
        entity.getPersistentData().remove(TAG);                           // its time is up (the root ran out with it)
        woke(entity, kind);
    }

    // a blow wakes a light sleeper, and a deep one after DEEP_HITS
    @SubscribeEvent
    public static void onSlumberHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || event.getAmount() <= 0f) return;
        Kind kind = kind(entity);
        if (kind == Kind.SLEEP) {
            wake(entity);
        } else if (kind == Kind.DEEP) {
            CompoundTag tag = entity.getPersistentData().getCompoundOrEmpty(TAG);
            int hits = tag.getIntOr(HITS, 0) + 1;
            if (hits > DEEP_HITS) {
                wake(entity);
            } else {
                tag.putInt(HITS, hits);
                entity.getPersistentData().put(TAG, tag);
            }
        }
    }

    // asleep, a mob picks no one
    @SubscribeEvent
    public static boolean onSlumberTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        return event.getNewTarget() != null && !mob.level().isClientSide() && isAsleep(mob);
    }

    @SubscribeEvent
    public static void onSlumberDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) event.getEntity().getPersistentData().remove(TAG);
    }
}
