package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Exposed: an entity cannot hide for a while. A survey that drags the hidden into the open, a lens that will not let
 * go, a light no shadow survives. Works from any class/ability, on players and mobs:
 *
 *   Exposed.apply(target, 60);           // 3 seconds
 *   Exposed.isExposed(entity)    Exposed.ticksLeft(entity)    Exposed.end(target)
 *   Exposed.addListener(one -> ...)      // someone was just pulled out of hiding: its look
 *   Exposed.addException(one -> ...)     // some hiding is not undone (an Exile is a banishment, not a hiding place)
 *
 * While it lasts, every tick: a Concealment on it ends (status/Concealment.reveal), an Unnoticed ends, and the
 * invisibility potion effect comes off and cannot be put on. Hiding again simply ends again the next tick.
 * An Exile is left alone (Exile.isExiled), and so is whatever an addException rule names.
 *
 * Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Exposed {

    private static final String TAG = "circle_of_inevitability_exposed";        // ticks left

    private static final List<Consumer<LivingEntity>> LISTENERS = new ArrayList<>();
    private static final List<Predicate<LivingEntity>> EXCEPTIONS = new ArrayList<>();

    static {
        addException(Exile::isExiled);
    }

    private Exposed() {}

    // =========================
    // API
    // =========================

    /** Cannot hide for 'ticks' (a longer exposure already on it is kept). Anything hidden now comes out at once. */
    public static void apply(LivingEntity target, int ticks) {
        if (target.level().isClientSide() || ticks <= 0 || !target.isAlive()) return;
        target.getPersistentData().putInt(TAG, Math.max(ticks, ticksLeft(target)));
        drag(target);
    }

    public static boolean isExposed(Entity entity) {
        return entity != null && entity.getPersistentData().getIntOr(TAG, 0) > 0;
    }

    public static int ticksLeft(Entity entity) {
        return entity == null ? 0 : entity.getPersistentData().getIntOr(TAG, 0);
    }

    public static void end(LivingEntity target) {
        target.getPersistentData().remove(TAG);
    }

    /** Runs whenever it pulls someone out of hiding (the look). */
    public static void addListener(Consumer<LivingEntity> pulledOut) {
        LISTENERS.add(pulledOut);
    }

    /** While this says true for an entity, its hiding is left alone. */
    public static void addException(Predicate<LivingEntity> leaveAlone) {
        EXCEPTIONS.add(leaveAlone);
    }

    // =========================
    // Internals
    // =========================

    /** Out of hiding, if it is hiding (and nothing says to leave it be). */
    private static void drag(LivingEntity target) {
        for (Predicate<LivingEntity> exception : EXCEPTIONS) if (exception.test(target)) return;
        boolean was = false;
        if (Concealment.isHidden(target)) {
            Concealment.reveal(target);
            was = true;
        }
        if (Unnoticed.isUnnoticed(target)) {
            Unnoticed.end(target);
            was = true;
        }
        if (target.hasEffect(MobEffects.INVISIBILITY)) {
            target.removeEffect(MobEffects.INVISIBILITY);
            was = true;
        }
        if (!was) return;
        for (Consumer<LivingEntity> listener : LISTENERS) {
            try {
                listener.accept(target);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("An exposure listener failed", e);
            }
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onExposedLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !entity.getPersistentData().contains(TAG)) return;
        int left = ticksLeft(entity) - 1;
        if (left <= 0) {
            end(entity);
            return;
        }
        entity.getPersistentData().putInt(TAG, left);
        drag(entity);
    }

    @SubscribeEvent
    public static void onExposedEffect(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !isExposed(entity)) return;
        if (event.getEffectInstance().getEffect() == MobEffects.INVISIBILITY) event.setResult(Result.DENY);
    }

    @SubscribeEvent
    public static void onExposedDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) end(event.getEntity());
    }
}
