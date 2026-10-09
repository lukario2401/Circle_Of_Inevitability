package net.lukario.frogerealm.status;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
 * Substitute: the next hit on this entity does not land. Something else takes it and YOU decide what happens
 * instead: a decoy appears, the caster blinks away, the attacker is punished... Works from any class/ability:
 *
 *   Substitute.arm(player, 200, 1, (saved, source, amount, level) -> {
 *       // the hit was cancelled. 'source' is what tried to hurt them (source.getEntity() = the attacker, may be null),
 *       // 'amount' is how much it would have done.
 *   });
 *
 *   - 200 = how long it stays ready (ticks)
 *   - 1   = how many hits it takes before it is used up
 *
 *   Substitute.isArmed(entity)     Substitute.charges(entity)     Substitute.disarm(entity)
 *
 * What counts as a hit: damage from an attacker (melee, arrows, abilities), projectiles and explosions.
 * Fire, falling, drowning, poison and the like go through and do not use it up; neither do hits the game would
 * ignore anyway (creative mode, the short invulnerability right after another hit).
 * Arming again replaces the old one. Not saved: gone after a server restart, on logout and on death.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Substitute {

    /** What happens instead of the hit. */
    @FunctionalInterface
    public interface Escape {
        void run(LivingEntity saved, DamageSource source, float amount, ServerLevel level);
    }

    private static final class Armed {
        final long expires;
        final Escape escape;
        int charges;

        Armed(long expires, int charges, Escape escape) {
            this.expires = expires;
            this.charges = charges;
            this.escape = escape;
        }
    }

    private static final Map<UUID, Armed> ARMED = new HashMap<>();

    private Substitute() {}

    // =========================
    // API
    // =========================

    public static void arm(LivingEntity entity, int ticks, int charges, Escape escape) {
        if (!(entity.level() instanceof ServerLevel sl) || ticks <= 0 || charges <= 0 || escape == null) return;
        long now = sl.getGameTime();
        ARMED.values().removeIf(old -> old.expires < now);         // the ones nobody ever hit
        ARMED.put(entity.getUUID(), new Armed(now + ticks, charges, escape));
    }

    public static boolean isArmed(Entity entity) {
        return charges(entity) > 0;
    }

    /** Hits it can still take (0 = not armed, or it ran out of time). */
    public static int charges(Entity entity) {
        if (entity == null) return 0;
        Armed armed = ARMED.get(entity.getUUID());
        if (armed == null) return 0;
        if (armed.expires < entity.level().getGameTime()) {
            ARMED.remove(entity.getUUID());
            return 0;
        }
        return armed.charges;
    }

    /** Ticks until it stops being ready (0 = not armed). */
    public static int ticksLeft(Entity entity) {
        if (!isArmed(entity)) return 0;
        return (int) (ARMED.get(entity.getUUID()).expires - entity.level().getGameTime());
    }

    public static void disarm(Entity entity) {
        ARMED.remove(entity.getUUID());
    }

    /** Whether this damage is the kind a substitute steps in for (see the class comment). */
    public static boolean isRealHit(LivingEntity entity, DamageSource source, float amount) {
        if (amount <= 0f) return false;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;                       // the void, /kill
        if (entity.isInvulnerableTo(source)) return false;
        if (entity instanceof Player player && player.getAbilities().invulnerable) return false;    // creative mode
        if (entity.invulnerableTime > 10) return false;       // still flashing red from the last hit: the game ignores this one
        return source.getEntity() != null
                || source.is(DamageTypeTags.IS_PROJECTILE)
                || source.is(DamageTypeTags.IS_EXPLOSION);
    }

    // =========================
    // Events
    // =========================

    // LOW: things that cancel the hit outright (a rooted attacker, a concealed target) get their say first,
    // so a hit that would never have landed does not use the substitute up.
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onSubstituteAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel sl)) return;
        if (!isArmed(entity)) return;
        if (!isRealHit(entity, event.getSource(), event.getAmount())) return;

        Armed armed = ARMED.get(entity.getUUID());
        armed.charges--;
        if (armed.charges <= 0) ARMED.remove(entity.getUUID());

        event.setCanceled(true);
        try {
            armed.escape.run(entity, event.getSource(), event.getAmount(), sl);
        } catch (Exception e) {
            ForgeRealm.LOGGER.error("A substitute's escape failed", e);     // the hit stays cancelled, the game goes on
        }
    }

    @SubscribeEvent
    public static void onSubstituteDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) ARMED.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onSubstitutePlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ARMED.remove(event.getEntity().getUUID());
    }
}
