package net.lukario.frogerealm.status;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Fall guard: the next landing does no fall damage. For abilities that throw their own caster (or a friend)
 * into the air. Works from any class/ability:
 *
 *   player.setDeltaMovement(0, 2.0, 0);
 *   player.hurtMarked = true;
 *   FallGuard.protect(player, 400);      // safe until they land, for at most 20 seconds
 *
 *   FallGuard.isProtected(entity)
 *   FallGuard.remove(entity)
 *
 * It ends by itself at the first landing, or when the time runs out (so it is not a free "never take fall
 * damage again" if they land in water and climb a tower afterwards). Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FallGuard {

    private static final String TAG = "forgerealm_fall_guard"; // ticks left

    private FallGuard() {}

    // =========================
    // API
    // =========================

    /** No fall damage for the next landing within 'ticks' ticks. A longer guard that is already there is kept. */
    public static void protect(LivingEntity entity, int ticks) {
        if (entity.level().isClientSide() || ticks <= 0) return;
        int left = entity.getPersistentData().getInt(TAG);
        entity.getPersistentData().putInt(TAG, Math.max(left, ticks));
    }

    public static boolean isProtected(Entity entity) {
        return entity != null && entity.getPersistentData().getInt(TAG) > 0;
    }

    public static void remove(LivingEntity entity) {
        entity.getPersistentData().remove(TAG);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onFallGuardLanding(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !isProtected(entity)) return;
        event.setCanceled(true);          // no damage and no fall sound for this landing
        remove(entity);
    }

    @SubscribeEvent
    public static void onFallGuardLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !entity.getPersistentData().contains(TAG)) return;
        int left = entity.getPersistentData().getInt(TAG) - 1;
        if (left <= 0) remove(entity);
        else entity.getPersistentData().putInt(TAG, left);
    }
}
