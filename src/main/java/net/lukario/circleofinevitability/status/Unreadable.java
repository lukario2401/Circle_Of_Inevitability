package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Unreadable: an entity cannot be picked out from further than a few blocks for a while. A false sky full of wrong
 * stars, a mind behind fog, a blur that only resolves up close. Works from any class/ability, on players and mobs:
 *
 *   Unreadable.apply(ally, 40, 6.0);     // 2 seconds: nobody further than 6 blocks can pick it out
 *   Unreadable.isUnreadable(entity)    Unreadable.range(entity)    Unreadable.ticksLeft(entity)    Unreadable.end(ally)
 *
 * While it lasts:
 *   - mobs further than 'range' cannot pick it as a target, and those hunting it from further away lose it
 *   - abilities cannot CHOOSE it from further than 'range' (Spells.canBeChosen: aimEnemy, enemiesInSight, the aiming
 *     of aimFrom / aimGround). With nobody named as the chooser it cannot be chosen at all. What only catches whoever
 *     is there (a blast, a zone, a wave) still does.
 *   - it is seen, drawn and can be hurt as usual; unlike Unnoticed it does not end when it strikes
 *
 * Put on again every second by a zone, it lasts as long as the zone does. Applying again keeps the longer time and
 * the shorter range. Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Unreadable {

    /** Ticks between reminders to the mobs around. */
    public static int REFRESH = 10;
    /** Mobs this close that hunt it are looked at (from further than 'range' they lose it). */
    public static double FORGET_RANGE = 48.0;

    private static final String TAG = "circle_of_inevitability_unreadable";      // {Ticks, Range}
    private static final String TICKS = "Ticks";
    private static final String RANGE = "Range";

    static {
        Spells.addUnchoosableCheck((chooser, target) -> isUnreadable(target)
                && (chooser == null || chooser.distanceTo(target) > range(target)));
    }

    private Unreadable() {}

    // =========================
    // API
    // =========================

    /** For 'ticks': nothing further than 'range' blocks away can pick it out. */
    public static void apply(LivingEntity target, int ticks, double range) {
        if (ticks <= 0 || !target.isAlive() || !(target.level() instanceof ServerLevel sl)) return;
        CompoundTag had = target.getPersistentData().getCompoundOrEmpty(TAG);
        CompoundTag tag = new CompoundTag();
        tag.putInt(TICKS, Math.max(ticks, had.getIntOr(TICKS, 0)));
        tag.putDouble(RANGE, had.contains(RANGE) ? Math.min(range, had.getDoubleOr(RANGE, range)) : range);
        target.getPersistentData().put(TAG, tag);
        forget(target, sl);
    }

    public static boolean isUnreadable(Entity entity) {
        return entity != null && entity.getPersistentData().contains(TAG);
    }

    /** How close one must be to pick it out (0 = it is not unreadable). */
    public static double range(Entity entity) {
        return isUnreadable(entity) ? entity.getPersistentData().getCompoundOrEmpty(TAG).getDoubleOr(RANGE, 0.0) : 0.0;
    }

    public static int ticksLeft(Entity entity) {
        return isUnreadable(entity) ? entity.getPersistentData().getCompoundOrEmpty(TAG).getIntOr(TICKS, 0) : 0;
    }

    public static void end(Entity entity) {
        if (entity != null) entity.getPersistentData().remove(TAG);
    }

    /** The mobs hunting it from further than its range lose it. */
    private static void forget(LivingEntity entity, ServerLevel sl) {
        double range = range(entity);
        for (Mob mob : sl.getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(FORGET_RANGE),
                hunter -> hunter.getTarget() == entity && hunter.distanceTo(entity) > range)) {
            mob.setTarget(null);
            if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onUnreadableLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel sl) || !isUnreadable(entity)) return;
        CompoundTag tag = entity.getPersistentData().getCompoundOrEmpty(TAG);
        int left = tag.getIntOr(TICKS, 0) - 1;
        if (left <= 0) {
            end(entity);
            return;
        }
        tag.putInt(TICKS, left);
        entity.getPersistentData().put(TAG, tag);
        if (left % REFRESH == 0) forget(entity, sl);
    }

    @SubscribeEvent
    public static boolean onUnreadableTargeted(LivingChangeTargetEvent event) {
        LivingEntity wanted = event.getNewTarget();
        if (wanted == null || !isUnreadable(wanted) || event.getEntity().level().isClientSide()) return false;
        return event.getEntity().distanceTo(wanted) > range(wanted);
    }

    @SubscribeEvent
    public static void onUnreadableDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) end(event.getEntity());
    }
}
