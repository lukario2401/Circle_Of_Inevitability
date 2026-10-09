package net.lukario.frogerealm.status;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Flight: a player can fly for a while, the way a creative player does (jump twice to take off, jump and sneak
 * to go up and down). Works from any class/ability:
 *
 *   Flight.grant(player, 400);          // 20 seconds. They are lifted off the ground at once
 *   Flight.has(player)
 *   Flight.ticksLeft(player)
 *   Flight.end(player);                 // lands them now
 *
 * However it ends (time up, or end(...)), the fall after it does no damage (FallGuard, for FALL_GUARD_TICKS).
 * Granting it to someone who has it already only makes it last longer.
 *
 * Creative players and spectators fly anyway: for them the time is counted, so has(...) and ticksLeft(...)
 * work the same, but their own flying is never touched.
 *
 * It is only the flying. Wings or anything else to show for it are yours to add, e.g. PaleEmperorFx.wings(...).
 *
 * Saved with the player: a flight goes on after logging out and in. It ends when they die.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Flight {

    /** How long the fall after a flight is safe. */
    public static int FALL_GUARD_TICKS = 400;

    private static final String TAG = "forgerealm_flight";      // ticks left

    private Flight() {}

    // =========================
    // API
    // =========================

    public static void grant(Player player, int ticks) {
        if (player.level().isClientSide() || ticks <= 0) return;

        int left = player.getPersistentData().getInt(TAG);
        player.getPersistentData().putInt(TAG, Math.max(left, ticks));

        if (fliesAnyway(player)) return;

        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();

        // off the ground at once: a player who is told to fly while standing lands again right away
        if (player.onGround()) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, Math.max(motion.y, 0.5), motion.z);
            player.hurtMarked = true;
        }
    }

    public static boolean has(Entity entity) {
        return entity != null && entity.getPersistentData().getInt(TAG) > 0;
    }

    public static int ticksLeft(Entity entity) {
        return entity == null ? 0 : entity.getPersistentData().getInt(TAG);
    }

    /** Ends the flight now. Does nothing if they have none. */
    public static void end(Player player) {
        if (!has(player)) return;
        player.getPersistentData().remove(TAG);

        if (fliesAnyway(player)) return;

        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        FallGuard.protect(player, FALL_GUARD_TICKS);             // the way down does no harm
    }

    // =========================
    // Internals
    // =========================

    private static boolean fliesAnyway(Player player) {
        return player.isCreative() || player.isSpectator();
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onFlightPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide() || !has(player)) return;

        int left = player.getPersistentData().getInt(TAG) - 1;
        if (left <= 0) {
            end(player);
            return;
        }
        player.getPersistentData().putInt(TAG, left);

        // a change of game mode takes flying away: give it back for as long as the flight lasts
        if (!fliesAnyway(player) && !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }
}
