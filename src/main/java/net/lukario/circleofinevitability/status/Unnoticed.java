package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Unnoticed: minds slide off an entity for a while, but eyes do not. Concealed intentions, a mind shut tight, being
 * beneath notice. Works from any class/ability, on players and mobs:
 *
 *   Unnoticed.hide(player, 300);         // 15 seconds
 *   Unnoticed.isUnnoticed(entity)     Unnoticed.ticksLeft(entity)     Unnoticed.end(player)
 *
 * While it lasts:
 *   - mobs forget it and cannot pick it as a target
 *   - abilities cannot CHOOSE it: Spells.aimEnemy, Spells.enemiesInSight and the aiming of Spells.aimFrom /
 *     aimGround pass over it (Spells.canBeChosen). What only catches whoever is there (enemiesAround, a blast,
 *     a wave, a shot that flies into it) still does.
 *   - unlike Concealment it is seen, drawn and can be hurt like anyone else
 *   - it ends the moment it hurts something (a swing or an ability)
 *
 * Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Unnoticed {

    /** Mobs this close that were hunting it lose track of it. */
    public static double FORGET_RANGE = 48.0;
    /** Ticks between reminders to the mobs around. */
    public static int REFRESH = 10;

    private static final String TAG = "circle_of_inevitability_unnoticed";       // ticks left

    private Unnoticed() {}

    // =========================
    // API
    // =========================

    /** For 'ticks' ticks. Hiding something already unnoticed only makes it last longer. */
    public static void hide(LivingEntity entity, int ticks) {
        if (ticks <= 0 || !entity.isAlive() || !(entity.level() instanceof ServerLevel sl)) return;
        if (ticks <= ticksLeft(entity)) return;
        entity.getPersistentData().putInt(TAG, ticks);
        forget(entity, sl);
    }

    public static boolean isUnnoticed(Entity entity) {
        return entity != null && entity.getPersistentData().contains(TAG);
    }

    public static int ticksLeft(Entity entity) {
        return isUnnoticed(entity) ? entity.getPersistentData().getIntOr(TAG, 0) : 0;
    }

    public static void end(Entity entity) {
        if (entity != null) entity.getPersistentData().remove(TAG);
    }

    /** False while it is unnoticed: abilities that choose a target must pass over it. */
    public static boolean canBeChosen(Entity entity) {
        return !isUnnoticed(entity);
    }

    private static void forget(LivingEntity entity, ServerLevel sl) {
        for (Mob mob : sl.getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(FORGET_RANGE),
                hunter -> hunter.getTarget() == entity)) {
            mob.setTarget(null);
            if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onUnnoticedLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel sl) || !isUnnoticed(entity)) return;
        int left = ticksLeft(entity) - 1;
        if (left <= 0) {
            end(entity);
            return;
        }
        entity.getPersistentData().putInt(TAG, left);
        if (left % REFRESH == 0) forget(entity, sl);
    }

    @SubscribeEvent
    public static boolean onUnnoticedTargeted(LivingChangeTargetEvent event) {
        return event.getNewTarget() != null && isUnnoticed(event.getNewTarget());
    }

    @SubscribeEvent
    public static void onUnnoticedSwing(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) end(player);
    }

    // whatever it hurts, with a weapon or an ability, notices it
    @SubscribeEvent
    public static void onUnnoticedHurts(LivingHurtEvent event) {
        Entity source = event.getSource().getEntity();
        if (source != null && source != event.getEntity() && !source.level().isClientSide()) end(source);
    }

    @SubscribeEvent
    public static void onUnnoticedDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) end(event.getEntity());
    }
}
