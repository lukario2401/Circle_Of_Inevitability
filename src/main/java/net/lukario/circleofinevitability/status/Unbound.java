package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Unbound: every lock on an entity springs open at once, and for a moment nothing of that kind takes on it again.
 * For a lock picked, a chain slipped, a mind shaken free. Key Of Stars' Slip Free is built on it. Works from any
 * class/ability, on players and mobs:
 *
 *   Unbound.free(target, 30, false);      // its roots and holds, harmful effects, fire and control end; for 1.5 s
 *                                         // no root (but a Teleport root) and no harmful effect takes on it
 *   Unbound.free(target, 30, true);       // the same, Teleport roots included
 *   int broken = Unbound.breakControl(target);   // only the control on it (mind control, strings): see addBreaker
 *   Unbound.isUnbound(entity)    Unbound.ticksLeft(entity)
 *
 * What "control" is, each Pathway says once, in a static block. A breaker ends its own kind of control on the entity
 * and says whether there was any:
 *
 *   Unbound.addBreaker(entity -> MyCharm.isCharmed(entity) && MyCharm.end(entity));
 *
 * Who has one today: nobody yet. Spirit Body Threads (Attendant Of Mysteries) run on the Attendant's own countdown,
 * so they cannot be found from the one they hold; give them a breaker when that changes.
 *
 * free(...) takes off: every root put on it (root/Root; a Teleport root only when asked), every harmful potion effect,
 * fire and every status that listens to status/Cleansing (Spells.cleanse), and runs every breaker. A hold that is not a
 * root (a Decree's Rule) is not its to open. Saved with the entity while it lasts.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Unbound {

    private static final String TAG = "circle_of_inevitability_unbound";               // ticks left
    private static final String TELEPORT_TOO = "circle_of_inevitability_unbound_teleport";

    private static final List<Predicate<LivingEntity>> BREAKERS = new ArrayList<>();

    static {
        // while it lasts, no root takes (a Teleport root only when that was freed too)
        Root.addImmunity((entity, restriction) -> isUnbound(entity)
                && (restriction != RootRestriction.TELEPORT || entity.getPersistentData().getBooleanOr(TELEPORT_TOO, false)));
    }

    private Unbound() {}

    // =========================
    // API
    // =========================

    /**
     * Frees the entity (see the class comment) and keeps it free of new roots and harmful effects for 'ticks'.
     * teleportToo: Teleport roots end and are kept off as well.
     */
    public static void free(LivingEntity target, int ticks, boolean teleportToo) {
        if (target.level().isClientSide()) return;
        if (teleportToo) {
            Root.remove(target);
        } else {
            for (RootRestriction restriction : RootRestriction.values()) {
                if (restriction != RootRestriction.TELEPORT) Root.remove(target, restriction);
            }
        }
        Spells.cleanse(target);
        breakControl(target);
        if (ticks > 0) {
            target.getPersistentData().putInt(TAG, Math.max(ticks, ticksLeft(target)));
            if (teleportToo) target.getPersistentData().putBoolean(TELEPORT_TOO, true);
        }
    }

    /** Ends every kind of control a Pathway registered with addBreaker. Returns how many let go. */
    public static int breakControl(LivingEntity target) {
        int broken = 0;
        for (Predicate<LivingEntity> breaker : List.copyOf(BREAKERS)) {
            if (breaker.test(target)) broken++;
        }
        return broken;
    }

    /** Once, in a static block: one more kind of control that free and breakControl end (see the class comment). */
    public static void addBreaker(Predicate<LivingEntity> breaker) {
        BREAKERS.add(breaker);
    }

    public static boolean isUnbound(Entity entity) {
        return entity != null && entity.getPersistentData().getIntOr(TAG, 0) > 0;
    }

    public static int ticksLeft(Entity entity) {
        return entity == null ? 0 : entity.getPersistentData().getIntOr(TAG, 0);
    }

    private static void end(Entity entity) {
        entity.getPersistentData().remove(TAG);
        entity.getPersistentData().remove(TELEPORT_TOO);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onUnboundLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !entity.getPersistentData().contains(TAG)) return;
        int left = entity.getPersistentData().getIntOr(TAG, 0) - 1;
        if (left <= 0) end(entity);
        else entity.getPersistentData().putInt(TAG, left);
    }

    /** No harmful effect takes on it while it is Unbound. */
    @SubscribeEvent
    public static void onUnboundEffect(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !isUnbound(entity)) return;
        if (!event.getEffectInstance().getEffect().value().isBeneficial()) event.setResult(Result.DENY);
    }

    @SubscribeEvent
    public static void onUnboundDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) end(event.getEntity());
    }
}
