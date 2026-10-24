package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Seal: a lock that keeps closing itself. Key Of Stars' Starlight Seal; a binding rune, a sealed door, a curse that
 * mends. Two kinds, both on players and mobs, from any class/ability:
 *
 *   Seal.apply(target, 160);          // Sealed for 8 seconds: no abilities and no teleports
 *   Seal.isSealed(entity)    Seal.ticksLeft(entity)    Seal.lift(target)
 *
 *   Seal.guard(ally, 240);            // their connections sealed for 12 seconds: no enemy ability can pick them
 *                                     // (status/Unreadable's gate, Spells.canBeChosen) and nothing can Starlit them
 *   Seal.isGuarded(entity)    Seal.guardTicksLeft(entity)
 *
 *   Seal.addListener((target, sl) -> ...)    // once, in a static block: something tried to open a Seal and it
 *                                            // closed again (the look of the repair)
 *
 * A Seal is not a root: it holds as one (Root.addHold: ABILITIES and TELEPORT), so nothing that takes roots off opens
 * it. Not Slip Free (status/Unbound), not a cleanse (Spells.cleanse: the Seal stays, and the listeners are told), not
 * milk. A sealed mob's powers are locked by status/AbilityLock (its projectiles and magic hits do not land, its melee
 * does); taken off by anything, the lock is put back within a second, and the listeners are told.
 *
 * Applying again keeps the longer time. Saved with the entity. Only lift (or the time running out, or death) ends it.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Seal {

    /** A Seal closed again after something tried to open it. */
    @FunctionalInterface
    public interface Repaired {
        void repaired(LivingEntity target, ServerLevel level);
    }

    private static final String TAG = "circle_of_inevitability_seal";            // ticks left
    private static final String GUARD = "circle_of_inevitability_seal_guard";    // ticks left

    private static final List<Repaired> LISTENERS = new ArrayList<>();

    static {
        Root.addHold((entity, restriction) -> (restriction == RootRestriction.ABILITIES || restriction == RootRestriction.TELEPORT)
                && isSealed(entity));
        Cleansing.addListener(entity -> {
            if (isSealed(entity) && entity.level() instanceof ServerLevel sl) repaired(entity, sl);
        });
        Spells.addUnchoosableCheck((chooser, target) -> chooser != null && chooser != target && isGuarded(target)
                && Spells.isEnemy(chooser, target));
        Starlit.addImmunity(Seal::isGuarded);
    }

    private Seal() {}

    // =========================
    // API
    // =========================

    /** Sealed for 'ticks' (see the class comment). False for the dead. */
    public static boolean apply(LivingEntity target, int ticks) {
        if (target.level().isClientSide() || ticks <= 0 || !target.isAlive()) return false;
        target.getPersistentData().putInt(TAG, Math.max(ticks, ticksLeft(target)));
        if (!(target instanceof Player)) AbilityLock.apply(target, ticksLeft(target));
        return true;
    }

    public static boolean isSealed(Entity entity) {
        return entity != null && entity.getPersistentData().getIntOr(TAG, 0) > 0;
    }

    public static int ticksLeft(Entity entity) {
        return entity == null ? 0 : entity.getPersistentData().getIntOr(TAG, 0);
    }

    /** Opens it now (the one way to). */
    public static void lift(LivingEntity target) {
        target.getPersistentData().remove(TAG);
        if (!(target instanceof Player)) AbilityLock.lift(target);
    }

    /** Their connections sealed for 'ticks' (see the class comment). */
    public static void guard(LivingEntity target, int ticks) {
        if (target.level().isClientSide() || ticks <= 0 || !target.isAlive()) return;
        target.getPersistentData().putInt(GUARD, Math.max(ticks, guardTicksLeft(target)));
    }

    public static boolean isGuarded(Entity entity) {
        return entity != null && entity.getPersistentData().getIntOr(GUARD, 0) > 0;
    }

    public static int guardTicksLeft(Entity entity) {
        return entity == null ? 0 : entity.getPersistentData().getIntOr(GUARD, 0);
    }

    public static void addListener(Repaired listener) {
        LISTENERS.add(listener);
    }

    private static void repaired(LivingEntity target, ServerLevel sl) {
        for (Repaired listener : List.copyOf(LISTENERS)) {
            try {
                listener.repaired(target, sl);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A seal listener failed", e);
            }
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onSealLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel sl)) return;
        var data = entity.getPersistentData();
        if (data.contains(TAG)) {
            int left = data.getIntOr(TAG, 0) - 1;
            if (left <= 0) {
                data.remove(TAG);
            } else {
                data.putInt(TAG, left);
                // a mob's lock taken off by something: it closes again
                if (!(entity instanceof Player) && entity.tickCount % 20 == 0 && !AbilityLock.isLocked(entity)) {
                    AbilityLock.apply(entity, left);
                    repaired(entity, sl);
                }
            }
        }
        if (data.contains(GUARD)) {
            int left = data.getIntOr(GUARD, 0) - 1;
            if (left <= 0) data.remove(GUARD);
            else data.putInt(GUARD, left);
        }
    }

    @SubscribeEvent
    public static void onSealDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        event.getEntity().getPersistentData().remove(TAG);
        event.getEntity().getPersistentData().remove(GUARD);
    }
}
