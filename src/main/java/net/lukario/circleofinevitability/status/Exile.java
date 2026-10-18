package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Exile: an entity is put out of the real world for a while and comes back where it left (or where you say). A
 * banishment, a step into a parallel world, a pocket of nowhere. Works from any class/ability, on players and mobs:
 *
 *   Exile.banish(target, 120);                                    // 6 seconds; back where it stood
 *   Exile.banish(player, 80, there, (who, sl) -> ...);            // back at 'there', and this runs when it is back
 *   Exile.isExiled(entity)    Exile.ticksLeft(entity)    Exile.bringBack(target)
 *   Exile.addListener(new Exile.Listener() { ... })               // the look of every banishment and return
 *
 * While it is out: nobody sees it, nothing hurts it and nothing can target it (status/Concealment), and it cannot
 * move, attack, use items, cast or teleport (root/Root, EVERYTHING). It hangs where it was, so it does not fall.
 * Coming back takes the hiding and the holds off (every hold: one put on by something else ends too).
 *
 * Banishing what is already out only makes it stay out longer. Saved with the entity (the end and the spot), so one
 * out when the world closes still comes back after it opens; the code given to banish(...) does not survive that.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Exile {

    /** Runs once it is back in the world. */
    @FunctionalInterface
    public interface Back {
        void returned(LivingEntity who, ServerLevel level);
    }

    /** The look of every exile. Both methods are optional. */
    public interface Listener {
        default void banished(LivingEntity who, ServerLevel level) {}
        default void returned(LivingEntity who, ServerLevel level) {}
    }

    private static final String TAG = "circle_of_inevitability_exile";
    private static final String UNTIL = "until";
    private static final String X = "x", Y = "y", Z = "z";

    private static final Map<UUID, Back> BACK = new HashMap<>();
    private static final List<Listener> LISTENERS = new ArrayList<>();

    private Exile() {}

    // =========================
    // API
    // =========================

    /** Out for 'ticks', back where it stands now. False if it cannot be (dead, creative). */
    public static boolean banish(LivingEntity target, int ticks) {
        return banish(target, ticks, null, null);
    }

    /** Out for 'ticks', back at 'returnTo' (null: where it stands now); 'back' (may be null) runs when it is back. */
    public static boolean banish(LivingEntity target, int ticks, Vec3 returnTo, Back back) {
        if (!(target.level() instanceof ServerLevel sl) || ticks <= 0 || !target.isAlive()) return false;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        boolean fresh = !isExiled(target);
        CompoundTag tag = fresh ? new CompoundTag() : target.getPersistentData().getCompoundOrEmpty(TAG);
        long until = Math.max(tag.getLongOr(UNTIL, 0L), sl.getGameTime() + ticks);
        tag.putLong(UNTIL, until);
        if (fresh || returnTo != null) {
            Vec3 spot = returnTo != null ? returnTo : target.position();
            tag.putDouble(X, spot.x);
            tag.putDouble(Y, spot.y);
            tag.putDouble(Z, spot.z);
        }
        target.getPersistentData().put(TAG, tag);
        if (back != null) BACK.put(target.getUUID(), back);
        int left = (int) (until - sl.getGameTime());
        Concealment.hide(target, left + 2);
        Root.apply(target, left + 2, RootRestriction.EVERYTHING);
        target.setDeltaMovement(Vec3.ZERO);
        if (target instanceof Mob mob) mob.setTarget(null);
        if (fresh) for (Listener listener : LISTENERS) safely(() -> listener.banished(target, sl));
        return true;
    }

    public static boolean isExiled(Entity entity) {
        return entity != null && entity.getPersistentData().contains(TAG);
    }

    public static int ticksLeft(Entity entity) {
        if (!isExiled(entity)) return 0;
        long until = entity.getPersistentData().getCompoundOrEmpty(TAG).getLongOr(UNTIL, 0L);
        return (int) Math.max(0, until - entity.level().getGameTime());
    }

    /** It comes back now (to its spot; 'back' and the listeners run). */
    public static void bringBack(LivingEntity target) {
        if (!isExiled(target) || !(target.level() instanceof ServerLevel sl)) return;
        CompoundTag tag = target.getPersistentData().getCompoundOrEmpty(TAG);
        target.getPersistentData().remove(TAG);
        Concealment.reveal(target);
        Root.remove(target);
        target.teleportTo(tag.getDoubleOr(X, target.getX()), tag.getDoubleOr(Y, target.getY()), tag.getDoubleOr(Z, target.getZ()));
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0;
        Back back = BACK.remove(target.getUUID());
        if (back != null) safely(() -> back.returned(target, sl));
        for (Listener listener : LISTENERS) safely(() -> listener.returned(target, sl));
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    private static void safely(Runnable run) {
        try {
            run.run();
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("An exile hook failed", e);
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onExileLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !isExiled(entity)) return;
        int left = ticksLeft(entity);
        if (left <= 0) {
            bringBack(entity);
            return;
        }
        if (!Concealment.isHidden(entity)) Concealment.hide(entity, left + 2);   // a swing that broke the hiding
        if (!Root.isRooted(entity)) Root.apply(entity, left + 2, RootRestriction.EVERYTHING);
        entity.setDeltaMovement(Vec3.ZERO);                                       // hangs where it was
        entity.fallDistance = 0;
    }

    @SubscribeEvent
    public static void onExileDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        entity.getPersistentData().remove(TAG);
        BACK.remove(entity.getUUID());
    }

    @SubscribeEvent
    public static void onExileLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        BACK.remove(event.getEntity().getUUID());                // the saved spot brings them back on the next login
    }
}
