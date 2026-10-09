package net.lukario.frogerealm.status;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Burial: an entity is pulled down into the ground, held there and then put back where it stood. For hands
 * that drag something under, quicksand, a grave that opens. Works from any class/ability:
 *
 *   if (Burial.bury(target, 20, 40)) { ... }   // goes under in 1 second, stays under for 2. false if it cannot be buried
 *   Burial.isBuried(entity)
 *   Burial.ticksLeft(entity)
 *   Burial.release(target);                     // back on the surface now
 *
 * While it is under it can do nothing (it is rooted for that time), and the ground chokes it the way it chokes
 * anything that is buried: that damage is the game's own, about 2 a second.
 *
 * It sinks slowly at first and then faster (the same curve as ModelFx.Ease.IN). When something visible drags
 * it down, give that thing's own depth as 'pull' and the two go down together:
 *
 *   // arms that sink 3.4 blocks with Ease.IN in 20 ticks: what they hold goes down exactly as fast as they do,
 *   // and stops once all of it is under
 *   Burial.bury(target, 20, 40, 3.4);
 *
 * What cannot be buried: something already buried, anything taller than MAX_HEIGHT or wider than MAX_WIDTH,
 * creative players and spectators.
 *
 * Saved with the entity: one that is under when the world closes still comes back up after it opens again.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Burial {

    /** Bigger things than this are not pulled under. */
    public static double MAX_HEIGHT = 4.0;
    public static double MAX_WIDTH = 3.0;

    /** How far below the surface the top of its head ends up. */
    private static final double COVER = 0.2;

    private static final String TAG = "forgerealm_burial";      // {X, Y, Z, Sink, Hold, Pull, Age}
    private static final String X = "X";
    private static final String Y = "Y";
    private static final String Z = "Z";
    private static final String SINK = "Sink";
    private static final String HOLD = "Hold";
    private static final String PULL = "Pull";
    private static final String AGE = "Age";

    private Burial() {}

    // =========================
    // API
    // =========================

    public static boolean canBury(LivingEntity target) {
        if (!target.isAlive() || isBuried(target)) return false;
        if (target.getBbHeight() > MAX_HEIGHT || target.getBbWidth() > MAX_WIDTH) return false;
        return !(target instanceof Player player && (player.getAbilities().invulnerable || player.isSpectator()));
    }

    /** Pulls it under over 'sinkTicks' ticks, keeps it there for 'holdTicks' and puts it back. False if it cannot be buried. */
    public static boolean bury(LivingEntity target, int sinkTicks, int holdTicks) {
        return bury(target, sinkTicks, holdTicks, target.getBbHeight() + COVER);
    }

    /**
     * The same, dragged by something that itself goes 'pull' blocks down in 'sinkTicks' ticks (with Ease.IN).
     * The entity goes down with it and stops once all of it is under.
     */
    public static boolean bury(LivingEntity target, int sinkTicks, int holdTicks, double pull) {
        if (target.level().isClientSide() || !canBury(target)) return false;

        int sink = Math.max(1, sinkTicks);
        int hold = Math.max(0, holdTicks);

        CompoundTag tag = new CompoundTag();
        tag.putDouble(X, target.getX());
        tag.putDouble(Y, target.getY());
        tag.putDouble(Z, target.getZ());
        tag.putInt(SINK, sink);
        tag.putInt(HOLD, hold);
        tag.putDouble(PULL, pull);
        tag.putInt(AGE, 0);
        target.getPersistentData().put(TAG, tag);

        Root.apply(target, sink + hold, RootRestriction.EVERYTHING);
        return true;
    }

    public static boolean isBuried(Entity entity) {
        return entity != null && entity.getPersistentData().contains(TAG);
    }

    /** Ticks until it is put back (0 = it is not buried). */
    public static int ticksLeft(Entity entity) {
        if (!isBuried(entity)) return 0;
        CompoundTag tag = entity.getPersistentData().getCompound(TAG);
        return Math.max(0, tag.getInt(SINK) + tag.getInt(HOLD) - tag.getInt(AGE));
    }

    /**
     * Puts it back where it stood, now. Does nothing if it is not buried. (When this is called early, the root
     * that came with the burial still runs to its end: Root.remove(target) frees it at once.)
     */
    public static void release(LivingEntity target) {
        if (!isBuried(target)) return;
        CompoundTag tag = target.getPersistentData().getCompound(TAG);
        target.getPersistentData().remove(TAG);
        place(target, tag.getDouble(X), tag.getDouble(Y), tag.getDouble(Z));
    }

    // =========================
    // Internals
    // =========================

    private static void place(LivingEntity entity, double x, double y, double z) {
        entity.teleportTo(x, y, z);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0;
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onBurialLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !isBuried(entity)) return;

        CompoundTag tag = entity.getPersistentData().getCompound(TAG);
        int sink = Math.max(1, tag.getInt(SINK));
        int age = tag.getInt(AGE) + 1;

        if (age >= sink + tag.getInt(HOLD)) {
            release(entity);
            return;
        }
        tag.putInt(AGE, age);

        double deepest = entity.getBbHeight() + COVER;           // all of it under the ground
        double depth = deepest;
        if (age < sink) {
            double t = age / (double) sink;
            depth = Math.min(deepest, tag.getDouble(PULL) * t * t * t);      // slowly at first, then faster
        }
        place(entity, tag.getDouble(X), tag.getDouble(Y) - Math.max(0.0, depth), tag.getDouble(Z));
    }
}
