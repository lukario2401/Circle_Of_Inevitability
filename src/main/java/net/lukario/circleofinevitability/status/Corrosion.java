package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.DamageFx;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Corrosion: a rot that eats deeper the longer it is left. The entity loses health once a second, a little more
 * every second, until it is cleansed or dies; after NO_HEAL_AFTER it cannot heal at all (status/NoHeal). The damage
 * is counted as the one who put it there (DamageFx.credit). Works from any class/ability, on players and mobs:
 *
 *   Corrosion.apply(target, player, 2f, 1f);           // 2 a second, 1 more every second, until cleansed
 *   Corrosion.apply(target, player, 2f, 1f, 200);      // the same, but it also wears off by itself after 10 seconds
 *   Corrosion.isCorroding(entity)   Corrosion.perSecond(entity)   Corrosion.ticksCorroding(entity)
 *   Corrosion.remove(target)                           // gone, quietly (Listener.ended with End.REMOVED)
 *   Corrosion.addImmunity(entity -> ...)               // some cannot rot (a golem, a body of light)
 *   Corrosion.addListener(new Corrosion.Listener() { ... })   // its look: when it starts, each bite, when it ends
 *
 * How it is taken off: anything that calls Spells.cleanse (every cleanse ability), a milk bucket (status/Cleansing),
 * death, or its own time limit when one was given. Applying it again keeps the bigger of the two bites and the
 * faster of the two growths, never restarts the clock (so the no-healing time counts from the first touch), and the
 * newest source gets the credit. Every bite is at most MAX_PER_SECOND. Magic damage: armor does not soften it.
 * Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Corrosion {

    /** Why a corrosion ended (for Listener.ended). */
    public enum End { CLEANSED, TIME, DIED, REMOVED }

    /** Its look and sound. Every method is optional. 'source' may be null (gone, or nobody). */
    public interface Listener {
        /** It was put on an entity that did not corrode yet. */
        default void started(LivingEntity victim, Player source) {}
        /** One bite: 'amount' was dealt just now. */
        default void bit(LivingEntity victim, float amount, Player source) {}
        /** It is over. */
        default void ended(LivingEntity victim, End why) {}
    }

    /** After this many ticks of corroding the entity cannot heal any more (100 = 5 seconds). */
    public static int NO_HEAL_AFTER = 100;
    /** Ticks between two bites. */
    public static int EVERY = 20;
    /** No single bite (a second's worth) does more than this, however long it has grown. */
    public static float MAX_PER_SECOND = 40f;

    private static final String TAG = "circle_of_inevitability_corrosion";
    private static final String STARTED = "started";            // game time of the first touch
    private static final String UNTIL = "until";                // game time it wears off (0 = only a cleanse ends it)
    private static final String BITE = "bite";                  // what the next bite does (a second's worth)
    private static final String GROWTH = "growth";              // how much more each bite does than the one before
    private static final String SOURCE = "source";

    private static final List<Predicate<LivingEntity>> IMMUNE = new ArrayList<>();
    private static final List<Listener> LISTENERS = new ArrayList<>();

    static {
        Cleansing.addListener(entity -> end(entity, End.CLEANSED));
    }

    private Corrosion() {}

    // =========================
    // API
    // =========================

    /** Corrodes 'perSecond' a second, 'growth' more each second, until cleansed. False if it cannot. */
    public static boolean apply(LivingEntity target, Player source, float perSecond, float growth) {
        return apply(target, source, perSecond, growth, 0);
    }

    /** The same, and it wears off by itself after 'ticks' (0 = never). Applying again only ever makes that later. */
    public static boolean apply(LivingEntity target, Player source, float perSecond, float growth, int ticks) {
        if (target.level().isClientSide() || perSecond <= 0f || !target.isAlive()) return false;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        if (isImmune(target)) return false;
        long now = target.level().getGameTime();
        boolean fresh = !isCorroding(target);
        CompoundTag tag = fresh ? new CompoundTag() : target.getPersistentData().getCompoundOrEmpty(TAG);
        if (fresh) tag.putLong(STARTED, now);
        long until = tag.getLongOr(UNTIL, 0L);
        if (ticks <= 0) until = 0L;                                        // forever wins
        else if (fresh || until != 0L) until = Math.max(until, now + ticks);
        tag.putLong(UNTIL, until);
        tag.putFloat(BITE, Math.min(MAX_PER_SECOND, Math.max(perSecond, tag.getFloatOr(BITE, 0f))));
        tag.putFloat(GROWTH, Math.max(Math.max(0f, growth), tag.getFloatOr(GROWTH, 0f)));
        if (source != null) tag.store(SOURCE, UUIDUtil.CODEC, source.getUUID());
        target.getPersistentData().put(TAG, tag);
        if (fresh) for (Listener listener : LISTENERS) safely(() -> listener.started(target, source));
        return true;
    }

    public static boolean isCorroding(Entity entity) {
        if (entity == null || !entity.getPersistentData().contains(TAG)) return false;
        long until = entity.getPersistentData().getCompoundOrEmpty(TAG).getLongOr(UNTIL, 0L);
        return until == 0L || until > entity.level().getGameTime();
    }

    /** What the next bite does (0 when it does not corrode). */
    public static float perSecond(Entity entity) {
        if (!isCorroding(entity)) return 0f;
        return entity.getPersistentData().getCompoundOrEmpty(TAG).getFloatOr(BITE, 0f);
    }

    /** How long it has been corroding, in ticks (0 when it does not). */
    public static int ticksCorroding(Entity entity) {
        if (!isCorroding(entity)) return 0;
        long started = entity.getPersistentData().getCompoundOrEmpty(TAG).getLongOr(STARTED, 0L);
        return (int) Math.max(0, entity.level().getGameTime() - started);
    }

    /** True once it has corroded long enough that it cannot heal. */
    public static boolean blocksHealing(Entity entity) {
        return isCorroding(entity) && ticksCorroding(entity) >= NO_HEAL_AFTER;
    }

    /** Takes it off now (Listener.ended runs with End.REMOVED). */
    public static void remove(LivingEntity target) {
        end(target, End.REMOVED);
    }

    public static boolean isImmune(LivingEntity entity) {
        for (Predicate<LivingEntity> immune : IMMUNE) if (immune.test(entity)) return true;
        return false;
    }

    public static void addImmunity(Predicate<LivingEntity> immune) {
        IMMUNE.add(immune);
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    // =========================
    // Internals
    // =========================

    private static void end(LivingEntity target, End why) {
        if (target == null || !target.getPersistentData().contains(TAG)) return;
        target.getPersistentData().remove(TAG);
        for (Listener listener : LISTENERS) safely(() -> listener.ended(target, why));
    }

    private static void safely(Runnable run) {
        try {
            run.run();
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("A corrosion listener failed", e);
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onCorrosionLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel sl) || !entity.getPersistentData().contains(TAG)) return;
        if (!isCorroding(entity)) {
            end(entity, End.TIME);
            return;
        }
        if (isImmune(entity)) {
            end(entity, End.REMOVED);
            return;
        }
        CompoundTag tag = entity.getPersistentData().getCompoundOrEmpty(TAG);
        long corroding = sl.getGameTime() - tag.getLongOr(STARTED, sl.getGameTime());
        if (corroding >= NO_HEAL_AFTER) NoHeal.apply(entity, 2);          // kept on a tick at a time: a cleanse ends it at once
        if (corroding <= 0 || corroding % EVERY != 0) return;

        float bite = tag.getFloatOr(BITE, 0f);
        float amount = bite * EVERY / 20f;
        tag.putFloat(BITE, Math.min(MAX_PER_SECOND, bite + tag.getFloatOr(GROWTH, 0f) * EVERY / 20f));
        entity.getPersistentData().put(TAG, tag);
        UUID id = tag.read(SOURCE, UUIDUtil.CODEC).orElse(null);
        Player source = id == null ? null : sl.getPlayerByUUID(id);
        if (source != null) DamageFx.credit(source, entity);
        entity.invulnerableTime = 0;
        entity.hurt(sl.damageSources().magic(), amount);
        for (Listener listener : LISTENERS) safely(() -> listener.bit(entity, amount, source));
    }

    @SubscribeEvent
    public static void onCorrosionDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide()) end(entity, End.DIED);
    }
}
