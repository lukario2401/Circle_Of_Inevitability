package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Later;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stored damage: for a while, everything an entity takes is written down, and when the time is up the whole sum
 * comes due at once. A curse that is paid back, a debt of pain, a time bomb fed by its own wounds.
 * Works from any class/ability, on players and mobs:
 *
 *   StoredDamage.start(target, player, 200,
 *           (victim, amount, total, sl) -> { ...a throb of its mark: it took 'amount', 'total' so far... },
 *           (victim, total, sl) -> StoredDamage.deal(victim, player, total));   // time is up: hit it with all of it
 *
 *   StoredDamage.isStoring(entity)     StoredDamage.total(entity)     StoredDamage.ticksLeft(entity)
 *   StoredDamage.release(entity)       // comes due now, early
 *   StoredDamage.cancel(entity)        // forgotten, nothing comes due
 *
 * - 200 = how long it writes the damage down (ticks).
 * - the second part runs for every hit it writes down, the third once when it comes due. Either may be null; with
 *   no third part nothing comes due (the total is only there to be asked for, with total(entity)).
 * - when it comes due you decide what happens: deal(...) hits it with the sum (through armor that already took its
 *   share once), and you can do it a few ticks later, after a look of your own (Later.run).
 *
 * What is written down is what the entity really lost (after armor, resistance and everything else that changes a
 * hit). Damage dealt by deal(...) is never written down, so a curse does not feed on its own payment.
 * One per entity: starting it again on someone who carries one replaces the old one (and the old total is lost).
 * It ends early, with nothing due, when the entity dies. Not saved: gone after a server restart.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StoredDamage {

    /** One hit was written down: 'amount' just now, 'total' all of it so far. */
    @FunctionalInterface
    public interface Stored {
        void run(LivingEntity victim, float amount, float total, ServerLevel level);
    }

    /** The time is up (or release(...) was called): 'total' is everything it took meanwhile. */
    @FunctionalInterface
    public interface Due {
        void run(LivingEntity victim, float total, ServerLevel level);
    }

    private static final class Debt {
        final long expires;
        final Stored onStored;
        final Due onDue;
        float total;

        Debt(long expires, Stored onStored, Due onDue) {
            this.expires = expires;
            this.onStored = onStored;
            this.onDue = onDue;
        }
    }

    private static final Map<UUID, Debt> DEBTS = new HashMap<>();
    private static boolean dealing = false;          // damage deal(...) is doing right now is not written down

    private StoredDamage() {}

    // =========================
    // API
    // =========================

    /** Writes down everything 'victim' takes for 'ticks' ticks; 'onDue' gets the sum when the time is up. */
    public static void start(LivingEntity victim, Player owner, int ticks, Stored onStored, Due onDue) {
        if (!(victim.level() instanceof ServerLevel sl) || ticks <= 0) return;
        long now = sl.getGameTime();
        DEBTS.values().removeIf(old -> old.expires < now);
        Debt debt = new Debt(now + ticks, onStored, onDue);
        DEBTS.put(victim.getUUID(), debt);
        Later.run(sl, ticks, () -> {
            Debt current = DEBTS.get(victim.getUUID());
            if (current != debt) return;                 // replaced, released or cancelled meanwhile
            comeDue(victim, debt, sl);
        });
    }

    public static boolean isStoring(Entity entity) {
        return ticksLeft(entity) > 0;
    }

    /** Everything written down so far (0 = it carries none). */
    public static float total(Entity entity) {
        Debt debt = live(entity);
        return debt == null ? 0f : debt.total;
    }

    /** Ticks until it comes due (0 = it carries none). */
    public static int ticksLeft(Entity entity) {
        Debt debt = live(entity);
        return debt == null ? 0 : (int) (debt.expires - entity.level().getGameTime());
    }

    /** It comes due now, before its time. */
    public static void release(LivingEntity entity) {
        Debt debt = live(entity);
        if (debt != null && entity.level() instanceof ServerLevel sl) comeDue(entity, debt, sl);
    }

    /** It is forgotten: nothing comes due. */
    public static void cancel(Entity entity) {
        DEBTS.remove(entity.getUUID());
    }

    /**
     * Hits 'victim' with 'amount' as magic from 'owner' (owner may be null): armor does not take its share again,
     * the short invulnerability after a hit does not swallow it, and it is never written down by a StoredDamage.
     */
    public static void deal(LivingEntity victim, LivingEntity owner, float amount) {
        if (amount <= 0f || !victim.isAlive() || !(victim.level() instanceof ServerLevel sl)) return;
        DamageSource source = owner == null ? sl.damageSources().magic() : sl.damageSources().indirectMagic(owner, owner);
        dealing = true;
        try {
            victim.invulnerableTime = 0;
            victim.hurt(source, amount);
        } finally {
            dealing = false;
        }
        if (owner != null) owner.setLastHurtMob(victim);
    }

    // =========================
    // Inside
    // =========================

    private static Debt live(Entity entity) {
        if (entity == null) return null;
        Debt debt = DEBTS.get(entity.getUUID());
        if (debt == null) return null;
        if (debt.expires < entity.level().getGameTime()) {
            DEBTS.remove(entity.getUUID());
            return null;
        }
        return debt;
    }

    private static void comeDue(LivingEntity victim, Debt debt, ServerLevel sl) {
        DEBTS.remove(victim.getUUID());
        if (debt.onDue == null || !victim.isAlive() || victim.level() != sl) return;
        try {
            debt.onDue.run(victim, debt.total, sl);
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("Stored damage failed to come due", e);
        }
    }

    // =========================
    // Events
    // =========================

    // LOWEST: last of all, so the amount is what the entity really loses
    @SubscribeEvent(priority = Priority.LOWEST)
    public static void onStoredDamageDamage(LivingDamageEvent event) {
        if (dealing) return;
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel sl)) return;
        float amount = event.getAmount();
        if (amount <= 0f) return;
        Debt debt = live(victim);
        if (debt == null) return;

        debt.total += amount;
        if (debt.onStored == null) return;
        try {
            debt.onStored.run(victim, amount, debt.total, sl);
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("Stored damage failed to write a hit down", e);
        }
    }

    @SubscribeEvent
    public static void onStoredDamageDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) DEBTS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStoredDamageLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DEBTS.remove(event.getEntity().getUUID());
    }
}
