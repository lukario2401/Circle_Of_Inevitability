package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Forgotten: a mob forgets ONE entity for a while. It drops it as its target at once and cannot pick it again until
 * the time is up; everyone else it may still hunt. A word that makes a hunter lose you (Hand Of Order's Halt), a
 * mind told to reveal nothing (Maintain Secrecy). Works from any class/ability:
 *
 *   Forgotten.apply(mob, player, 80);          // for 4 seconds this mob cannot go after this player
 *   Forgotten.isForgotten(mob, player)    Forgotten.ticksLeft(mob, player)    Forgotten.remember(mob, player)
 *   Forgotten.huntersOf(sl, player, 32)        // the mobs within 32 blocks hunting this player right now
 *
 * Unlike Unnoticed (every mind slides off the one hidden) and Unreadable (nobody far away can pick it out), this is
 * one mob and one target. Applying again keeps the longer time. Not saved: a server restart forgets the forgetting.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Forgotten {

    /** mob -> (who it has forgotten -> until which game time). */
    private static final Map<UUID, Map<UUID, Long>> FORGOTTEN = new HashMap<>();

    private Forgotten() {}

    // =========================
    // API
    // =========================

    /** For 'ticks' this mob cannot pick 'who' as its target; if it is hunting 'who' now, it stops. */
    public static void apply(Mob mob, LivingEntity who, int ticks) {
        if (ticks <= 0 || who == null || mob == who || !(mob.level() instanceof ServerLevel sl)) return;
        long until = sl.getGameTime() + ticks;
        FORGOTTEN.computeIfAbsent(mob.getUUID(), id -> new HashMap<>()).merge(who.getUUID(), until, Math::max);
        if (mob.getTarget() == who) drop(mob);
    }

    public static boolean isForgotten(Entity mob, Entity who) {
        return ticksLeft(mob, who) > 0;
    }

    public static int ticksLeft(Entity mob, Entity who) {
        if (mob == null || who == null) return 0;
        Map<UUID, Long> forgotten = FORGOTTEN.get(mob.getUUID());
        if (forgotten == null) return 0;
        Long until = forgotten.get(who.getUUID());
        return until == null ? 0 : (int) Math.max(0, until - mob.level().getGameTime());
    }

    /** It may pick 'who' again now. */
    public static void remember(Entity mob, Entity who) {
        Map<UUID, Long> forgotten = FORGOTTEN.get(mob.getUUID());
        if (forgotten == null) return;
        forgotten.remove(who.getUUID());
        if (forgotten.isEmpty()) FORGOTTEN.remove(mob.getUUID());
    }

    /** The mobs within 'radius' of 'who' that are hunting it right now. */
    public static List<Mob> huntersOf(ServerLevel sl, LivingEntity who, double radius) {
        return sl.getEntitiesOfClass(Mob.class, who.getBoundingBox().inflate(radius), mob -> mob.getTarget() == who);
    }

    private static void drop(Mob mob) {
        mob.setTarget(null);
        if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
    }

    // =========================
    // Events
    // =========================

    /** It tries to pick someone it has forgotten: refused. */
    @SubscribeEvent
    public static boolean onForgottenTarget(LivingChangeTargetEvent event) {
        LivingEntity wanted = event.getNewTarget();
        if (wanted == null || event.getEntity().level().isClientSide() || FORGOTTEN.isEmpty()) return false;
        return isForgotten(event.getEntity(), wanted);
    }

    /** Twice a second: what has run out is forgotten in turn. */
    @SubscribeEvent
    public static void onForgottenLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel sl) || FORGOTTEN.isEmpty() || sl.getGameTime() % 10 != 0) return;
        long now = sl.getGameTime();
        FORGOTTEN.values().removeIf(forgotten -> {
            forgotten.values().removeIf(until -> until <= now);
            return forgotten.isEmpty();
        });
    }

    @SubscribeEvent
    public static void onForgottenDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) FORGOTTEN.remove(event.getEntity().getUUID());
    }
}
