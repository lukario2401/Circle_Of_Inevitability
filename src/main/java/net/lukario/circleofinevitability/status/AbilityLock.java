package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * AbilityLock: an entity's special powers are locked for a while, but it can still fight with its hands. A mind lost
 * in a maze of equations, knowledge that rejects someone, a spell forgotten. Unlike Silence (which stops a mob from
 * attacking at all) a mob keeps its melee here. Works from any class/ability, on players and mobs:
 *
 *   AbilityLock.apply(target, 160);       // 8 seconds
 *   AbilityLock.isLocked(entity)    AbilityLock.ticksLeft(entity)    AbilityLock.lift(target)
 *   AbilityLock.addImmunity(entity -> ...)   // some cannot be locked (a mind no knowledge can reach)
 *
 * While it lasts:
 *   - a player cannot use the ability keys (status/Root's ABILITIES, which every key asks first); weapons still work
 *   - a mob's projectiles vanish as they are fired, and every hit it makes that is not its own body striking (fangs,
 *     breath, a thrown thing, magic) is stopped: only its melee lands
 *
 * apply returns false for the immune and the dead. Applying again keeps the longer time. Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AbilityLock {

    private static final String TAG = "circle_of_inevitability_ability_lock";      // ticks left

    private static final List<Predicate<LivingEntity>> IMMUNE = new ArrayList<>();

    private AbilityLock() {}

    // =========================
    // API
    // =========================

    /** Locked for 'ticks'. False if it could not be (immune, dead, client side). */
    public static boolean apply(LivingEntity target, int ticks) {
        if (target.level().isClientSide() || ticks <= 0 || !target.isAlive() || isImmune(target)) return false;
        target.getPersistentData().putInt(TAG, Math.max(ticks, ticksLeft(target)));
        if (target instanceof Player) Root.apply(target, ticks, RootRestriction.ABILITIES);
        return true;
    }

    public static boolean isLocked(Entity entity) {
        return entity != null && entity.getPersistentData().getIntOr(TAG, 0) > 0;
    }

    public static int ticksLeft(Entity entity) {
        return entity == null ? 0 : entity.getPersistentData().getIntOr(TAG, 0);
    }

    /** Unlocked now (and a player's ABILITIES root with it). */
    public static void lift(LivingEntity target) {
        if (!isLocked(target)) return;
        target.getPersistentData().remove(TAG);
        if (target instanceof Player) Root.remove(target, RootRestriction.ABILITIES);
    }

    public static void addImmunity(Predicate<LivingEntity> immune) {
        IMMUNE.add(immune);
    }

    public static boolean isImmune(LivingEntity entity) {
        for (Predicate<LivingEntity> immune : IMMUNE) if (immune.test(entity)) return true;
        return false;
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onAbilityLockLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !entity.getPersistentData().contains(TAG)) return;
        int left = ticksLeft(entity) - 1;
        if (left <= 0) {
            entity.getPersistentData().remove(TAG);
            return;
        }
        entity.getPersistentData().putInt(TAG, left);
    }

    /** A locked mob's hit that is not its own body striking does not land. */
    @SubscribeEvent
    public static boolean onAbilityLockAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker == null || attacker instanceof Player || event.getEntity().level().isClientSide()) return false;
        return isLocked(attacker) && event.getSource().getDirectEntity() != attacker;
    }

    /** A locked mob's projectile is gone as it is fired. */
    @SubscribeEvent
    public static void onAbilityLockShot(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Projectile projectile) || !(event.getLevel() instanceof ServerLevel sl)) return;
        if (!(projectile.getOwner() instanceof LivingEntity shooter) || shooter instanceof Player || !isLocked(shooter)) return;
        Later.run(sl, 1, projectile::discard);
    }

    @SubscribeEvent
    public static void onAbilityLockDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) event.getEntity().getPersistentData().remove(TAG);
    }
}
