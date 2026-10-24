package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Toy: a creature becomes a little wind-up toy for a while. Key Of Stars' Toy Box; a doll curse, a shrinking hex.
 * Works from any class/ability:
 *
 *   Toy.make(target, 160);         // 8 seconds: SIZE of its size, much slower (Slowness SLOW_LEVEL + 1), and its hits
 *                                  // land at DAMAGE of their strength
 *   Toy.isToy(entity)    Toy.ticksLeft(entity)    Toy.release(target)
 *   Toy.addListener(target -> ...) // once, in a static block: a toy is itself again (time up, released, died)
 *
 * The body is status/GiantForm (of(SIZE)), so it is one form at a time: a giant made a toy is a small toy, and
 * growing it again ends the toy. Which creatures may become toys is the caller's to decide (Toy Box spares players and
 * big creatures). Not saved: after a restart nobody is a toy.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Toy {

    public static double SIZE = 0.25;
    public static int SLOW_LEVEL = 3;          // Slowness IV: 60% slower
    public static float DAMAGE = 0.2f;

    private static final String TAG = "circle_of_inevitability_toy";        // ticks left

    private static final List<Consumer<LivingEntity>> LISTENERS = new ArrayList<>();

    private Toy() {}

    // =========================
    // API
    // =========================

    /** A toy for 'ticks'. False for the dead. Made again: its time starts over. */
    public static boolean make(LivingEntity target, int ticks) {
        if (target.level().isClientSide() || ticks <= 0 || !target.isAlive()) return false;
        target.getPersistentData().putInt(TAG, ticks);
        GiantForm.of(SIZE).lasts(ticks).onEnd((who, why) -> {
            if (why != GiantForm.Ending.TIME && why != GiantForm.Ending.DIED) end(who);   // grown into something else
        }).grow(target);
        Spells.keepEffect(target, MobEffects.SLOWNESS, SLOW_LEVEL, 25);
        return true;
    }

    public static boolean isToy(Entity entity) {
        return entity != null && entity.getPersistentData().getIntOr(TAG, 0) > 0;
    }

    public static int ticksLeft(Entity entity) {
        return entity == null ? 0 : entity.getPersistentData().getIntOr(TAG, 0);
    }

    /** Itself again now. */
    public static void release(LivingEntity target) {
        if (!isToy(target)) return;
        end(target);
        if (GiantForm.isGiant(target)) GiantForm.end(target);
    }

    public static void addListener(Consumer<LivingEntity> listener) {
        LISTENERS.add(listener);
    }

    private static void end(LivingEntity target) {
        if (!target.getPersistentData().contains(TAG)) return;
        target.getPersistentData().remove(TAG);
        target.removeEffect(MobEffects.SLOWNESS);
        for (var listener : List.copyOf(LISTENERS)) {
            try {
                listener.accept(target);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A toy listener failed", e);
            }
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onToyLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel) || !entity.getPersistentData().contains(TAG)) return;
        int left = entity.getPersistentData().getIntOr(TAG, 0) - 1;
        if (left <= 0) {
            end(entity);
            return;
        }
        entity.getPersistentData().putInt(TAG, left);
        if (entity.tickCount % 20 == 0) Spells.keepEffect(entity, MobEffects.SLOWNESS, SLOW_LEVEL, 25);
    }

    /** A toy's hits are a toy's. */
    @SubscribeEvent
    public static void onToyHurts(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        Entity attacker = event.getSource().getEntity();
        if (isToy(attacker)) event.setAmount(event.getAmount() * DAMAGE);
    }

    @SubscribeEvent
    public static void onToyDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) end(event.getEntity());
    }
}
