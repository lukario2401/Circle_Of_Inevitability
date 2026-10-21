package net.lukario.circleofinevitability.combat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Tellings: "less each time". A player's use history of something, kept for a while, turned into a multiplier.
 * The Knowledge Emperor's Obscurity is built on it (a story told often loses its power), and it fits any effect that
 * should weaken when it is repeated: a trick that works once, a charm people get used to.
 *
 *   float strength = Tellings.strength(player, "my_pathway_tale", 6000, 0.2f, 0.4f);   // 1 untold, 0.2 less for each
 *                                                                                  // telling in the last 5 minutes,
 *                                                                                  // never under 0.4
 *   Tellings.tell(player, "my_pathway_tale");                                     // one more telling, now
 *   int told = Tellings.count(player, "my_pathway_tale", 6000);
 *   Tellings.forget(player, "my_pathway_tale");
 *
 * Ask strength before you tell, so the first telling is at full power. Kept on the player (persistent data), so it
 * survives a relog; game times older than the longest window anyone asked for are dropped when it is read again.
 * Give the key your Pathway's name first. A key may hold anything but commas.
 */
public final class Tellings {

    private static final String TAG = "circle_of_inevitability_tellings";
    /** At most this many tellings are kept for one key (older ones count for nothing anyway). */
    private static final int MOST_KEPT = 32;

    private Tellings() {}

    /** One more telling of 'key', now. */
    public static void tell(Player player, String key) {
        List<Long> times = times(player, key);
        times.add(player.level().getGameTime());
        while (times.size() > MOST_KEPT) times.remove(0);
        save(player, key, times);
    }

    /** How often 'key' was told within the last 'window' ticks. */
    public static int count(Player player, String key, int window) {
        long since = player.level().getGameTime() - window;
        int told = 0;
        for (long time : times(player, key)) if (time > since) told++;
        return told;
    }

    /** 1 minus 'step' for every telling within the last 'window' ticks, never under 'floor'. */
    public static float strength(Player player, String key, int window, float step, float floor) {
        return Math.max(floor, 1f - step * count(player, key, window));
    }

    /** Ticks since 'key' was last told (Integer.MAX_VALUE: never). */
    public static int sinceLast(Player player, String key) {
        List<Long> times = times(player, key);
        if (times.isEmpty()) return Integer.MAX_VALUE;
        long since = player.level().getGameTime() - times.get(times.size() - 1);
        return since < 0 || since > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) since;
    }

    public static void forget(Player player, String key) {
        CompoundTag all = player.getPersistentData().getCompoundOrEmpty(TAG);
        all.remove(key);
        player.getPersistentData().put(TAG, all);
    }

    private static List<Long> times(Player player, String key) {
        String saved = player.getPersistentData().getCompoundOrEmpty(TAG).getStringOr(key, "");
        List<Long> times = new ArrayList<>();
        if (saved.isEmpty()) return times;
        long now = player.level().getGameTime();
        for (String part : saved.split(",")) {
            try {
                long time = Long.parseLong(part);
                if (time <= now) times.add(time);                    // a time ahead of now: another world's clock
            } catch (NumberFormatException ignored) {
                // edited save data: skip it
            }
        }
        return times;
    }

    private static void save(Player player, String key, List<Long> times) {
        CompoundTag all = player.getPersistentData().getCompoundOrEmpty(TAG);
        StringBuilder text = new StringBuilder();
        for (long time : times) {
            if (!text.isEmpty()) text.append(',');
            text.append(time);
        }
        all.putString(key, text.toString());
        player.getPersistentData().put(TAG, all);
    }
}
