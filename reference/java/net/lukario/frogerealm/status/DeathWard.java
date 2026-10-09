package net.lukario.frogerealm.status;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Death ward: the next time this entity would die, it doesn't. Like a totem of undying that an ability hands out.
 * Works from any class/ability, on players and mobs:
 *
 *   DeathWard.arm(player, 1200, 0.5f, (saved, source, level) -> {
 *       // runs at the moment death was cheated: effects, sounds, a blast, a teleport...
 *   });
 *
 *   - 1200  = how long the ward waits (ticks). If nothing kills them in that time it just fades.
 *   - 0.5f  = the health they come back with, as a part of their maximum (0.5 = half).
 *   - the last part may be null if nothing else should happen.
 *
 *   DeathWard.isArmed(entity)     DeathWard.ticksLeft(entity)     DeathWard.disarm(entity)
 *
 * It saves from everything except the void and /kill. A totem of undying in the hand is used first.
 * For GRACE_TICKS afterwards nothing can hurt them, so whatever killed them does not simply do it again.
 * Arming again replaces the old ward. Not saved: wards are gone after a server restart or when the player logs out.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DeathWard {

    /** What happens when the ward saves someone. 'source' is what would have killed them. */
    @FunctionalInterface
    public interface Revive {
        void run(LivingEntity saved, DamageSource source, ServerLevel level);
    }

    /** How long someone who was just saved cannot be hurt (ticks). */
    public static int GRACE_TICKS = 40;

    private record Ward(long expires, float health, Revive onRevive) {}

    private static final Map<UUID, Ward> WARDS = new HashMap<>();
    private static final String GRACE_TAG = "forgerealm_death_ward_grace";   // on the saved entity: the game time it ends

    private DeathWard() {}

    // =========================
    // API
    // =========================

    public static void arm(LivingEntity entity, int ticks, float healthFraction, Revive onRevive) {
        if (!(entity.level() instanceof ServerLevel sl) || ticks <= 0) return;
        long now = sl.getGameTime();
        WARDS.values().removeIf(old -> old.expires() < now);        // the ones that faded unused
        WARDS.put(entity.getUUID(), new Ward(now + ticks, healthFraction, onRevive));
    }

    public static boolean isArmed(Entity entity) {
        return ticksLeft(entity) > 0;
    }

    /** Ticks until the ward fades (0 = no ward). */
    public static int ticksLeft(Entity entity) {
        if (entity == null) return 0;
        Ward ward = WARDS.get(entity.getUUID());
        if (ward == null) return 0;
        long left = ward.expires() - entity.level().getGameTime();
        if (left <= 0) {
            WARDS.remove(entity.getUUID());
            return 0;
        }
        return (int) left;
    }

    public static void disarm(Entity entity) {
        WARDS.remove(entity.getUUID());
    }

    // =========================
    // Events
    // =========================

    // HIGHEST: the death is undone before anything else reacts to it (drops, death effects of other abilities...)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeathWardDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel sl)) return;
        if (!isArmed(entity)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;   // the void and /kill still kill

        Ward ward = WARDS.remove(entity.getUUID());
        event.setCanceled(true);
        entity.setHealth(Math.max(1f, entity.getMaxHealth() * ward.health()));
        entity.clearFire();
        if (GRACE_TICKS > 0) entity.getPersistentData().putLong(GRACE_TAG, sl.getGameTime() + GRACE_TICKS);   // a moment to get away
        if (ward.onRevive() == null) return;
        try {
            ward.onRevive().run(entity, event.getSource(), sl);
        } catch (Exception e) {
            ForgeRealm.LOGGER.error("A death ward's revive effect failed", e);   // they are saved all the same
        }
    }

    // HIGH: before Substitute and the like, so a hit that cannot land anyway does not use those up
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeathWardGraceAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !entity.getPersistentData().contains(GRACE_TAG)) return;
        if (entity.getPersistentData().getLong(GRACE_TAG) < entity.level().getGameTime()) {
            entity.getPersistentData().remove(GRACE_TAG);
            return;
        }
        if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDeathWardPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        WARDS.remove(event.getEntity().getUUID());
    }
}
