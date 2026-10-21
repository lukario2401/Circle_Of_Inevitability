package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.Root;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Invulnerable: for a while nothing can hurt, choose or hold an entity, and what would have hurt it is written down.
 * Unlike Concealment it is still seen (its models are drawn) and can act. For a body turned into information, a
 * form of light, a moment out of reach. Knowledge Emperor's Informatization is built on it.
 *
 *   Invulnerable.grant(player, 120);                 // 6 seconds; granting again makes it last the longer of the two
 *   Invulnerable.isGranted(entity)   Invulnerable.ticksLeft(entity)   Invulnerable.absorbed(entity)
 *   float taken = Invulnerable.end(player);          // it ends now; returns everything it absorbed (0 if none)
 *
 * While it lasts: every hit is cancelled (not the void or /kill) and its amount added up; mobs cannot pick it as
 * their target and those that had it forget it; abilities built on Spells cannot choose it (it can still be caught
 * by what hits an area, which then does nothing); every root on it is lifted at once. When the time is up it simply
 * ends: call end(...) yourself to read what was absorbed before that, or ask absorbed(...) in a Later.run at the end.
 * Not saved.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Invulnerable {

    private static final class State {
        long until;
        float absorbed;

        State(long until) {
            this.until = until;
        }
    }

    private static final Map<UUID, State> GRANTED = new HashMap<>();

    static {
        Spells.addUnchoosableCheck((chooser, target) -> isGranted(target));
    }

    private Invulnerable() {}

    public static void grant(LivingEntity entity, int ticks) {
        if (entity.level().isClientSide() || ticks <= 0) return;
        long until = entity.level().getGameTime() + ticks;
        State state = GRANTED.get(entity.getUUID());
        if (state == null) {
            GRANTED.put(entity.getUUID(), new State(until));
        } else {
            state.until = Math.max(state.until, until);
        }
        Root.remove(entity);
        forgetMe(entity);
    }

    public static boolean isGranted(Entity entity) {
        if (entity == null || GRANTED.isEmpty()) return false;
        State state = GRANTED.get(entity.getUUID());
        return state != null && state.until > entity.level().getGameTime();
    }

    public static int ticksLeft(Entity entity) {
        State state = entity == null ? null : GRANTED.get(entity.getUUID());
        return state == null ? 0 : (int) Math.max(0, state.until - entity.level().getGameTime());
    }

    /** What it has absorbed so far (also for a moment after its time ran out, until end or the next grant). */
    public static float absorbed(Entity entity) {
        State state = entity == null ? null : GRANTED.get(entity.getUUID());
        return state == null ? 0f : state.absorbed;
    }

    /** Ends it now. Returns everything it absorbed. */
    public static float end(Entity entity) {
        State state = entity == null ? null : GRANTED.remove(entity.getUUID());
        return state == null ? 0f : state.absorbed;
    }

    /** Mobs near that were after it let it go. */
    private static void forgetMe(LivingEntity entity) {
        for (Mob mob : entity.level().getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(48.0),
                mob -> mob.getTarget() == entity)) {
            mob.setTarget(null);
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent(priority = Priority.HIGH)
    public static boolean onInvulnerableAttacked(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !isGranted(entity)) return false;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;     // the void, /kill
        GRANTED.get(entity.getUUID()).absorbed += Math.max(0f, event.getAmount());
        return true;
    }

    @SubscribeEvent
    public static boolean onInvulnerableTargeted(LivingChangeTargetEvent event) {
        if (event.getEntity().level().isClientSide()) return false;
        return isGranted(event.getNewTarget());
    }

    @SubscribeEvent
    public static void onInvulnerableTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (GRANTED.isEmpty() || entity.level().isClientSide() || !isGranted(entity)) return;
        if (Root.isRooted(entity)) Root.remove(entity);
    }

    @SubscribeEvent
    public static void onInvulnerableDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) GRANTED.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onInvulnerableLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GRANTED.remove(event.getEntity().getUUID());
    }
}
