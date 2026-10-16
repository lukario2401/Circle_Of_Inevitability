package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Fear: the other half of status/Taunt. Something is afraid of one entity for a while. Works from any
 * class/ability, on players and mobs:
 *
 *   Fear.apply(victim, caster, 120);       // 6 seconds afraid of the caster
 *   Fear.isAfraid(entity)   Fear.sourceOf(entity)   Fear.ticksLeft(entity)   Fear.end(victim)
 *   Fear.addImmunity(entity -> ...)        // some know no fear
 *
 * A mob runs from what it fears (FLEE_DISTANCE away, faster than it walks), drops it as a target and cannot pick it.
 * A player is slowed and sees the dark close in (Slowness I and Darkness), and cannot sprint toward what they fear.
 *
 * Afraid again never makes it shorter; a new source replaces the old one. Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Fear {

    /** How far a frightened mob tries to get from what it fears. */
    public static double FLEE_DISTANCE = 10.0;
    /** How fast it runs (1 = its walking speed). */
    public static double FLEE_SPEED = 1.4;
    /** Further than this from what it fears, a mob stops running (it is still afraid and still cannot target it). */
    public static double SAFE_DISTANCE = 16.0;
    /** Ticks between two checks (running, a player's sprint). */
    public static int REFRESH = 5;

    private static final String TAG = "circle_of_inevitability_fear";
    private static final String UNTIL = "until";              // game time
    private static final String SOURCE = "source";

    private static final List<Predicate<LivingEntity>> IMMUNE = new ArrayList<>();

    private Fear() {}

    // =========================
    // API
    // =========================

    /** False (and nothing happens) when it cannot be frightened: dead, itself, a creative player, or immune. */
    public static boolean apply(LivingEntity victim, LivingEntity source, int ticks) {
        if (victim.level().isClientSide() || ticks <= 0 || !victim.isAlive() || source == null || source == victim) return false;
        if (victim instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        for (Predicate<LivingEntity> immune : IMMUNE) if (immune.test(victim)) return false;

        long now = victim.level().getGameTime();
        CompoundTag old = victim.getPersistentData().getCompoundOrEmpty(TAG);
        long until = Math.max(now + ticks, old.getLongOr(UNTIL, 0L));
        CompoundTag tag = new CompoundTag();
        tag.putLong(UNTIL, until);
        tag.store(SOURCE, UUIDUtil.CODEC, source.getUUID());
        victim.getPersistentData().put(TAG, tag);

        int left = (int) (until - now);
        if (victim instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, left, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, Math.min(left, 100), 0, false, false));
            player.setSprinting(false);
        } else if (victim instanceof Mob mob) {
            forgetSource(mob, source);
            run(mob, source);
        }
        return true;
    }

    public static boolean isAfraid(Entity entity) {
        return entity != null && entity.getPersistentData().contains(TAG)
                && entity.getPersistentData().getCompoundOrEmpty(TAG).getLongOr(UNTIL, 0L) > entity.level().getGameTime();
    }

    /** What it is afraid of, or null (none, or it is gone from this level). */
    public static LivingEntity sourceOf(Entity entity) {
        if (!isAfraid(entity) || !(entity.level() instanceof ServerLevel sl)) return null;
        UUID id = entity.getPersistentData().getCompoundOrEmpty(TAG).read(SOURCE, UUIDUtil.CODEC).orElse(null);
        return id != null && sl.getEntity(id) instanceof LivingEntity living ? living : null;
    }

    public static int ticksLeft(Entity entity) {
        if (!isAfraid(entity)) return 0;
        return (int) Math.max(0, entity.getPersistentData().getCompoundOrEmpty(TAG).getLongOr(UNTIL, 0L) - entity.level().getGameTime());
    }

    public static void end(LivingEntity victim) {
        if (!victim.getPersistentData().contains(TAG)) return;
        victim.getPersistentData().remove(TAG);
        if (victim instanceof Player player) {
            player.removeEffect(MobEffects.DARKNESS);
            player.removeEffect(MobEffects.SLOWNESS);
        }
    }

    public static void addImmunity(Predicate<LivingEntity> immune) {
        IMMUNE.add(immune);
    }

    // =========================
    // Internals
    // =========================

    private static void forgetSource(Mob mob, LivingEntity source) {
        if (mob.getTarget() == source) mob.setTarget(null);
        if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                && mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == source) {
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }

    /** Sends the mob running straight away from the source. */
    private static void run(Mob mob, LivingEntity source) {
        Vec3 away = mob.position().subtract(source.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(mob.getRandom().nextDouble() - 0.5, 0, mob.getRandom().nextDouble() - 0.5);
        Vec3 to = mob.position().add(flat.normalize().scale(FLEE_DISTANCE));
        mob.getNavigation().moveTo(to.x, to.y, to.z, FLEE_SPEED);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onFearLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity.tickCount % REFRESH != 0 || !entity.getPersistentData().contains(TAG)) return;
        if (!isAfraid(entity)) {
            entity.getPersistentData().remove(TAG);
            return;
        }
        LivingEntity source = sourceOf(entity);
        if (source == null || !source.isAlive()) {
            end(entity);
            return;
        }
        if (entity instanceof Mob mob) {
            forgetSource(mob, source);
            if (mob.distanceToSqr(source) < SAFE_DISTANCE * SAFE_DISTANCE) run(mob, source);
        } else if (entity instanceof Player player && player.isSprinting()) {
            Vec3 toSource = source.position().subtract(player.position());
            Vec3 motion = player.getDeltaMovement();
            if (toSource.x * motion.x + toSource.z * motion.z > 0) player.setSprinting(false);   // not toward it
        }
    }

    // a mob may not go after what it fears
    @SubscribeEvent
    public static boolean onFearTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        LivingEntity wanted = event.getNewTarget();
        if (wanted == null || mob.level().isClientSide() || !isAfraid(mob)) return false;
        return wanted == sourceOf(mob);
    }

    @SubscribeEvent
    public static void onFearDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) event.getEntity().getPersistentData().remove(TAG);
    }
}
