package net.lukario.circleofinevitability.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

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
 * Cooldowns.addScale(player -> ...) changes every cooldown a player starts (the wheels' too): scaled(player, ticks).
 * The key may be anything; start it with your Pathway.
 * A copy of an ability (combat/Casts: Record's Replay, an echo) neither waits for these cooldowns nor starts them.
 */
public final class Cooldowns {

    private static final String PREFIX = "circle_of_inevitability_cooldown_";

    /** A rule that makes every cooldown of a player longer or shorter: 1.5f = half again as long. */
    @FunctionalInterface
    public interface Scale {
        float scale(Player player);
    }

    private static final List<Scale> SCALES = new ArrayList<>();

    private Cooldowns() {}

    /**
     * One more rule for every cooldown a player starts, single keys and every wheel alike (an enemy in a Divine
     * Kingdom waits half again as long). Once, in a static block.
     */
    public static void addScale(Scale rule) {
        SCALES.add(rule);
    }

    /** 'ticks' after every addScale rule: what a cooldown of 'ticks' really is for this player now. */
    public static int scaled(Player player, int ticks) {
        if (ticks <= 0 || SCALES.isEmpty()) return ticks;
        float scale = 1f;
        for (Scale rule : SCALES) scale *= Math.max(0f, rule.scale(player));
        return Math.round(ticks * scale);
    }

    /** True when the ability is ready; otherwise false, and the player is told when it will be. */
    public static boolean ready(Player player, String key, String name) {
        int left = left(player, key);
        if (left <= 0) return true;
        player.sendSystemMessage(Component.literal(name + " is ready again in " + seconds(left) + "."));
        return false;
    }

    /** Starts its cooldown: ready again 'ticks' from now (0 or less: ready at once). */
    public static void start(Player player, String key, int ticks) {
        if (Casts.isCopying(player)) return;                    // a copy (combat/Casts) starts no cooldown...
        if (ticks <= 0) {
            reset(player, key);
            return;
        }
        player.getPersistentData().putLong(PREFIX + key, player.level().getGameTime() + scaled(player, ticks));
    }

    /** Ticks until it is ready (0 = ready). */
    public static int left(Player player, String key) {
        if (Casts.isCopying(player)) return 0;                  // ...and waits for none
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
