package net.lukario.frogerealm.status;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * TODO(stage5): placeholder with the same public API as the real Flight (reference/.../status/Flight.java),
 * so callers compile. Nothing happens: grant(...) does not make anyone fly, has(...) is always false.
 * Its only user is PaleEmperor (stage 5): re-copy the real class then, run scripts/renames.sh on it
 * (TickEvent.PlayerTickEvent -> TickEvent.PlayerTickEvent.Post is already one of its rules), and put
 * the @Mod.EventBusSubscriber back together with onFlightPlayerTick.
 */
public final class Flight {

    /** How long the fall after a flight is safe. */
    public static int FALL_GUARD_TICKS = 400;

    private Flight() {}

    public static void grant(Player player, int ticks) {
    }

    public static boolean has(Entity entity) {
        return false;
    }

    public static int ticksLeft(Entity entity) {
        return 0;
    }

    public static void end(Player player) {
    }
}
