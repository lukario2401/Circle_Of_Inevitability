package net.lukario.frogerealm.status;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Vulnerable: an entity takes more damage from everything for a while. For curses, marks of weakness, armor
 * that was broken. Works from any class/ability, on players and mobs:
 *
 *   Vulnerable.add(target, 0.10f, 0.60f, 200);   // 10% more damage, on top of what is already there, 60% at most.
 *                                                // All of it lasts 10 seconds from now
 *   Vulnerable.extra(target)                     // how much more it takes right now: 0.3f = 30% more
 *   Vulnerable.ticksLeft(target)
 *   Vulnerable.remove(target);
 *
 * Calling add again stacks, so three hits of 0.10f make 30%. For a fixed amount that does not stack, give the
 * same number as 'extra' and as 'max': Vulnerable.add(target, 0.25f, 0.25f, 100). An add never lowers or
 * shortens what is already on the target, so different abilities can use it on the same enemy.
 *
 * The damage is raised before armor is counted. Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Vulnerable {

    private static final String TAG = "forgerealm_vulnerable";  // {Extra, Until}
    private static final String EXTRA = "Extra";
    private static final String UNTIL = "Until";                // game time

    private Vulnerable() {}

    // =========================
    // API
    // =========================

    /**
     * 'extra' more damage taken (0.10f = 10% more), added to what is already there, never more than 'max' in all.
     * All of it lasts 'ticks' ticks from now. Returns how much more it takes now.
     *
     * It never makes things better for the target: if something else already made it take more than 'max', or
     * for longer than 'ticks', that stays as it is. So two abilities can both use this on the same enemy.
     */
    public static float add(LivingEntity target, float extra, float max, int ticks) {
        if (target.level().isClientSide() || ticks <= 0) return extra(target);

        float had = extra(target);
        float now = Math.max(had, Math.min(max, had + extra));
        long until = Math.max(target.level().getGameTime() + ticks, had > 0f ? until(target) : 0L);

        CompoundTag tag = new CompoundTag();
        tag.putFloat(EXTRA, now);
        tag.putLong(UNTIL, until);
        target.getPersistentData().put(TAG, tag);
        return now;
    }

    private static long until(Entity entity) {
        return entity.getPersistentData().getCompound(TAG).getLong(UNTIL);
    }

    /** How much more damage it takes right now: 0 = none, 0.3f = 30% more. */
    public static float extra(Entity entity) {
        if (entity == null || !entity.getPersistentData().contains(TAG)) return 0f;
        CompoundTag tag = entity.getPersistentData().getCompound(TAG);
        return entity.level().getGameTime() >= tag.getLong(UNTIL) ? 0f : tag.getFloat(EXTRA);
    }

    public static int ticksLeft(Entity entity) {
        if (entity == null || !entity.getPersistentData().contains(TAG)) return 0;
        long until = entity.getPersistentData().getCompound(TAG).getLong(UNTIL);
        return (int) Math.max(0, until - entity.level().getGameTime());
    }

    public static void remove(LivingEntity target) {
        target.getPersistentData().remove(TAG);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onVulnerableHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        float extra = extra(victim);
        if (extra > 0f) event.setAmount(event.getAmount() * (1f + extra));
    }
}
