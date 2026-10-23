package net.lukario.circleofinevitability.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Cooldowns for the single ability keys (the wheels have their own, in AbilityWheel). Saved on the player in game
 * time, so they survive a relog:
 *
 *   if (!Cooldowns.ready(player, "my_pathway_flask", "Compound Flask")) return;   // says "... is ready again in 1.5s."
 *   if (!pay(player, 250)) return;
 *   Cooldowns.start(player, "my_pathway_flask", 40);
 *
 *   Cooldowns.left(player, key)       // ticks until it is ready (0 = ready)
 *   Cooldowns.reset(player, key)      // ready now
 *
 * Check it first (with the other "can this be cast" checks), start it after the cast went off, as AbilityWheel does.
 * The key may be anything; start it with your Pathway.
 */
public final class Cooldowns {

    private static final String PREFIX = "circle_of_inevitability_cooldown_";

    private Cooldowns() {}

    /** True when the ability is ready; otherwise false, and the player is told when it will be. */
    public static boolean ready(Player player, String key, String name) {
        int left = left(player, key);
        if (left <= 0) return true;
        player.sendSystemMessage(Component.literal(name + " is ready again in " + seconds(left) + "."));
        return false;
    }

    /** Starts its cooldown: ready again 'ticks' from now (0 or less: ready at once). */
    public static void start(Player player, String key, int ticks) {
        if (ticks <= 0) {
            reset(player, key);
            return;
        }
        player.getPersistentData().putLong(PREFIX + key, player.level().getGameTime() + ticks);
    }

    /** Ticks until it is ready (0 = ready). */
    public static int left(Player player, String key) {
        long ready = player.getPersistentData().getLongOr(PREFIX + key, 0L);
        long left = ready - player.level().getGameTime();
        if (left > 72000L) {                                    // a save from another world's clock: forget it
            reset(player, key);
            return 0;
        }
        return (int) Math.max(0, left);
    }

    public static void reset(Player player, String key) {
        player.getPersistentData().remove(PREFIX + key);
    }

    /** "1.5s", "12s" (rounded up to a tenth), as the wheels write it. */
    public static String seconds(int ticks) {
        int tenths = (int) Math.ceil(ticks / 2.0);
        return tenths % 10 == 0 ? (tenths / 10) + "s" : (tenths / 10) + "." + (tenths % 10) + "s";
    }
}
