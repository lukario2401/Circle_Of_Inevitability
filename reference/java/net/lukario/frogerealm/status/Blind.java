package net.lukario.frogerealm.status;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Blind: blindness that works on mobs too. Works from any class/ability:
 *
 *   Blind.apply(target, 80);            // 4 seconds
 *   Blind.apply(target, 80, 5.0);       // ...and a mob still sees what is within 5 blocks of it
 *   Blind.isBlind(entity)
 *   Blind.ticksLeft(entity)
 *   Blind.remove(target);
 *
 * A player gets the blindness effect: their screen goes dark. A mob does not care about that effect, so it
 * also loses what it was hunting, and until it can see again it only goes after what is right next to it
 * (MOB_SEES blocks, or the distance you give).
 *
 * Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Blind {

    /** How near something must be for a blinded mob to go after it, when apply(...) is not given a distance. */
    public static double MOB_SEES = 3.0;

    private static final String UNTIL = "forgerealm_blind_until";       // game time
    private static final String SEES = "forgerealm_blind_sees";         // blocks

    private Blind() {}

    // =========================
    // API
    // =========================

    public static void apply(LivingEntity target, int ticks) {
        apply(target, ticks, MOB_SEES);
    }

    /** Blinds it for 'ticks' ticks. Blinding something that is blind already only makes it last longer. */
    public static void apply(LivingEntity target, int ticks, double mobSees) {
        if (target.level().isClientSide() || ticks <= 0) return;

        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0));

        long until = target.level().getGameTime() + ticks;
        if (until > target.getPersistentData().getLong(UNTIL)) target.getPersistentData().putLong(UNTIL, until);
        target.getPersistentData().putDouble(SEES, mobSees);

        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            // mobs with a brain (piglins, wardens...) remember their target there
            if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
                mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }
        }
    }

    public static boolean isBlind(Entity entity) {
        return entity != null && entity.level().getGameTime() < entity.getPersistentData().getLong(UNTIL);
    }

    public static int ticksLeft(Entity entity) {
        if (entity == null) return 0;
        return (int) Math.max(0, entity.getPersistentData().getLong(UNTIL) - entity.level().getGameTime());
    }

    public static void remove(LivingEntity target) {
        target.getPersistentData().remove(UNTIL);
        target.getPersistentData().remove(SEES);
        target.removeEffect(MobEffects.BLINDNESS);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onBlindTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        LivingEntity target = event.getNewTarget();
        if (target == null || mob.level().isClientSide() || !isBlind(mob)) return;

        double sees = mob.getPersistentData().getDouble(SEES);
        if (mob.distanceToSqr(target) > sees * sees) event.setCanceled(true);
    }
}
