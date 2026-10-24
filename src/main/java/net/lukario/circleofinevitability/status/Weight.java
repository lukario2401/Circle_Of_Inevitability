package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Weight (Heaviness): something is made heavy for a while. Starlight that presses, a wave that drags, a body too dense
 * to stand near. Works from any class/ability, on players and mobs:
 *
 *   Weight.put(enemy, 1, 60);          // Heavy for 3 seconds
 *   Weight.put(enemy, 3, 100);         // very heavy (levels 1..MAX_LEVEL) for 5 seconds
 *   Weight.isHeavy(entity)   Weight.level(entity)   Weight.ticksLeft(entity)   Weight.end(target)
 *   Weight.addImmunity(entity -> ...)   // some cannot be made heavy (a body denser than the one pressing it)
 *
 * While it is heavy (each level adds its share, every number is a public static below):
 *   - it is slowed (its movement speed attribute, so a player's own screen agrees),
 *   - it cannot jump, and it cannot fly: creative-style flight and elytra gliding stop,
 *   - it falls like a stone (status/Gravity) and its falls hurt more,
 *   - what it shoots or throws flies slower.
 *
 * Putting it on again keeps the higher level and the later end, so two abilities never cut each other short.
 * Saved with the entity. Star Dragon's Gravity Abnormality, Tidal Surge, Scaled Form, Heavybringing (docs/STAR_DRAGON.md).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Weight {

    /** The heaviest it gets. */
    public static int MAX_LEVEL = 3;
    /** Movement speed taken away per level (0.2 = 20%), and at most. */
    public static double SLOW_PER_LEVEL = 0.2;
    public static double SLOW_MOST = 0.7;
    /** Gravity (status/Gravity) per level: 1 + this * level times as heavy. */
    public static double GRAVITY_PER_LEVEL = 0.5;
    /** Falls hurt this much more per level (0.3 = 30%). */
    public static float FALL_PER_LEVEL = 0.3f;
    /** What it shoots flies this fast (1 = as usual). */
    public static double SHOT_SPEED = 0.6;

    private static final String TAG = "circle_of_inevitability_weight";       // {Level, Until}
    private static final String LEVEL = "Level";
    private static final String UNTIL = "Until";
    private static final String GRAVITY = "circle_of_inevitability_weight";   // its status/Gravity reason
    private static final Identifier SLOW = Physique.id("weight_slow");
    private static final Identifier NO_JUMP = Physique.id("weight_no_jump");

    private static final List<Predicate<LivingEntity>> IMMUNE = new ArrayList<>();

    private Weight() {}

    // =========================
    // API
    // =========================

    /** Heavy at 'level' (1..MAX_LEVEL) for 'ticks'. Never lowers the level or shortens what is there. */
    public static void put(LivingEntity target, int level, int ticks) {
        if (target.level().isClientSide() || ticks <= 0 || level <= 0 || !target.isAlive() || isImmune(target)) return;
        CompoundTag tag = target.getPersistentData().getCompoundOrEmpty(TAG);
        long now = target.level().getGameTime();
        boolean running = tag.getLongOr(UNTIL, 0L) > now;
        int kept = running ? tag.getIntOr(LEVEL, 0) : 0;
        tag.putInt(LEVEL, Math.min(MAX_LEVEL, Math.max(kept, level)));
        tag.putLong(UNTIL, Math.max(now + ticks, running ? tag.getLongOr(UNTIL, 0L) : 0L));
        target.getPersistentData().put(TAG, tag);
        update(target);
    }

    public static boolean isHeavy(Entity entity) {
        return level(entity) > 0;
    }

    /** Its level now (0 = not heavy). */
    public static int level(Entity entity) {
        if (entity == null || !entity.getPersistentData().contains(TAG)) return 0;
        CompoundTag tag = entity.getPersistentData().getCompoundOrEmpty(TAG);
        return tag.getLongOr(UNTIL, 0L) > entity.level().getGameTime() ? tag.getIntOr(LEVEL, 0) : 0;
    }

    public static int ticksLeft(Entity entity) {
        if (entity == null || !entity.getPersistentData().contains(TAG)) return 0;
        long until = entity.getPersistentData().getCompoundOrEmpty(TAG).getLongOr(UNTIL, 0L);
        return (int) Math.max(0, until - entity.level().getGameTime());
    }

    public static void end(LivingEntity target) {
        target.getPersistentData().remove(TAG);
        update(target);
    }

    /** Once, in a static block: while it says true for an entity, nothing can make it heavy (and what it had ends). */
    public static void addImmunity(Predicate<LivingEntity> immune) {
        IMMUNE.add(immune);
    }

    public static boolean isImmune(LivingEntity entity) {
        for (Predicate<LivingEntity> rule : IMMUNE) if (rule.test(entity)) return true;
        return false;
    }

    // =========================
    // Internals
    // =========================

    private static void update(LivingEntity entity) {
        int level = level(entity);
        if (level > 0 && isImmune(entity)) {
            entity.getPersistentData().remove(TAG);
            level = 0;
        }
        if (level == 0) entity.getPersistentData().remove(TAG);
        double slow = Math.min(SLOW_MOST, SLOW_PER_LEVEL * level);
        Physique.keep(entity, Attributes.MOVEMENT_SPEED, SLOW, -slow, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, level > 0);
        Physique.keep(entity, Attributes.JUMP_STRENGTH, NO_JUMP, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, level > 0);
        if (level == 0) {
            Gravity.clear(entity, GRAVITY);
            return;
        }
        Gravity.set(entity, GRAVITY, 1.0 + GRAVITY_PER_LEVEL * level, 3);
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            if (player.isFallFlying()) player.stopFallFlying();
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onWeightLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (entity.getPersistentData().contains(TAG) || Physique.has(entity, Attributes.MOVEMENT_SPEED, SLOW)) update(entity);
    }

    /** Its falls hurt more. */
    @SubscribeEvent
    public static void onWeightHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || !event.getSource().is(DamageTypes.FALL)) return;
        int level = level(event.getEntity());
        if (level > 0) event.setAmount(event.getAmount() * (1f + FALL_PER_LEVEL * level));
    }

    /** What a heavy one shoots flies slower. */
    @SubscribeEvent
    public static void onWeightShot(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Projectile projectile) || !(event.getLevel() instanceof ServerLevel)) return;
        if (!(projectile.getOwner() instanceof LivingEntity shooter) || !isHeavy(shooter)) return;
        projectile.setDeltaMovement(projectile.getDeltaMovement().scale(SHOT_SPEED));
        projectile.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onWeightDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) end(event.getEntity());
    }
}
