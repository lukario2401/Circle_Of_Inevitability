package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scrolls: spells kept on paper. A player carries a pouch of them, oldest first; reading one casts its spell at
 * once, for free, with no chant and no cooldown of its own. Any spell can be a scroll: it is registered once with
 * a key and written as a function of who reads it and how strong it is, with no payment inside, so the same code
 * runs when it is cast by hand, read from a scroll, repeated by an echo or empowered by a passive. Knowledge Emperor's
 * Scrolls Professor is built on it (docs/KNOWLEDGE_EMPEROR.md, "Scroll pouch").
 *
 *   // once, in a static block: the key (no commas), its name, what it costs to cast by hand, and the spell
 *   Scrolls.register("my_pathway_hex", "Hex", 1250, (reader, sl, power) -> hex(reader, sl, power));
 *
 *   // in the ability, when it is cast by hand: remembered as the last Scrollable spell this player cast
 *   if (hex(player, sl, 1f)) Scrolls.cast(player, "my_pathway_hex");
 *
 *   Scrolls.add(player, "my_pathway_hex", 3);       // into the pouch (3 at most); returns the key that fell out, or null
 *   String oldest = Scrolls.takeOldest(player);      // out of the pouch (null: it is empty)
 *   Scrolls.Kind kind = Scrolls.kind(oldest);        // what it is: kind.spell().cast(reader, sl, 1f)
 *   Scrolls.putBack(player, oldest);                 // the spell found nothing to do: back in front, unspent
 *   Scrolls.pouch(player)   Scrolls.size(player)   Scrolls.lastCast(player)   Scrolls.clear(player)
 *   Scrolls.addListener((player, pouch) -> ...)      // the pouch changed: show it (pictures by the hotbar)
 *
 * A spell returns true when it went off and false when it found nothing to do (no target): a scroll that is read is
 * only used up when it went off. The pouch is saved with the player (relogging keeps it); dying burns it.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scrolls {

    /** A spell as it is kept on a scroll. 'power' is 1 for a plain cast (1.5 = half again as strong). */
    @FunctionalInterface
    public interface Spell {
        boolean cast(ServerPlayer reader, ServerLevel sl, float power);
    }

    /** One kind of scroll: its key, what players are told it is called, what casting it by hand costs, and the spell. */
    public record Kind(String key, String name, int cost, Spell spell) {}

    /** The pouch changed (a scroll in, read, dropped, or the pouch emptied): its keys, oldest first. */
    @FunctionalInterface
    public interface Listener {
        void changed(Player player, List<String> pouch);
    }

    private static final Map<String, Kind> KINDS = new LinkedHashMap<>();
    private static final List<Listener> LISTENERS = new ArrayList<>();

    private static final String POUCH = "circle_of_inevitability_scrolls";
    private static final String LAST_CAST = "circle_of_inevitability_scrolls_last";

    private Scrolls() {}

    // =========================
    // Kinds
    // =========================

    /** Registers a kind of scroll (call it once, in a static block). Registering a key again replaces it. */
    public static Kind register(String key, String name, int cost, Spell spell) {
        if (key.contains(",")) throw new IllegalArgumentException("Scrolls: a key may not hold a comma: " + key);
        Kind kind = new Kind(key, name, cost, spell);
        KINDS.put(key, kind);
        return kind;
    }

    /** The kind with this key, or null. */
    public static Kind kind(String key) {
        return key == null ? null : KINDS.get(key);
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    // =========================
    // The last spell cast by hand (what an Inscribe would write)
    // =========================

    /** A Scrollable spell was just cast by hand: it is what this player cast last. */
    public static void cast(Player player, String key) {
        if (KINDS.containsKey(key)) player.getPersistentData().putString(LAST_CAST, key);
    }

    /** The kind this player cast last by hand, or null. */
    public static Kind lastCast(Player player) {
        return kind(player.getPersistentData().getStringOr(LAST_CAST, ""));
    }

    // =========================
    // The pouch
    // =========================

    /** The keys in the pouch, oldest first (a copy). Keys no longer registered are left out. */
    public static List<String> pouch(Player player) {
        String saved = player.getPersistentData().getStringOr(POUCH, "");
        if (saved.isEmpty()) return new ArrayList<>();
        List<String> keys = new ArrayList<>(Arrays.asList(saved.split(",")));
        keys.removeIf(key -> !KINDS.containsKey(key));
        return keys;
    }

    public static int size(Player player) {
        return pouch(player).size();
    }

    public static boolean isFull(Player player, int capacity) {
        return size(player) >= capacity;
    }

    /**
     * A scroll into the pouch, newest last. A full pouch drops its oldest first: that key is returned (null when
     * nothing fell out). A pouch fuller than 'capacity' (it was made smaller) drops until it fits.
     */
    public static String add(Player player, String key, int capacity) {
        if (!KINDS.containsKey(key) || capacity <= 0) return null;
        List<String> keys = pouch(player);
        String dropped = null;
        while (keys.size() >= capacity) dropped = keys.remove(0);
        keys.add(key);
        save(player, keys);
        return dropped;
    }

    /** The oldest scroll out of the pouch (null when it is empty). */
    public static String takeOldest(Player player) {
        List<String> keys = pouch(player);
        if (keys.isEmpty()) return null;
        String oldest = keys.remove(0);
        save(player, keys);
        return oldest;
    }

    /** A scroll that was taken but not used goes back in front, as the oldest. */
    public static void putBack(Player player, String key) {
        if (!KINDS.containsKey(key)) return;
        List<String> keys = pouch(player);
        keys.add(0, key);
        save(player, keys);
    }

    /** Everything in the pouch is gone. */
    public static void clear(Player player) {
        if (player.getPersistentData().getStringOr(POUCH, "").isEmpty()) return;
        save(player, Collections.emptyList());
    }

    /** Tells the listeners again what is in the pouch (after a relog, when its pictures were lost). */
    public static void show(Player player) {
        List<String> keys = pouch(player);
        for (Listener listener : LISTENERS) listener.changed(player, keys);
    }

    private static void save(Player player, List<String> keys) {
        if (keys.isEmpty()) player.getPersistentData().remove(POUCH);
        else player.getPersistentData().putString(POUCH, String.join(",", keys));
        List<String> copy = List.copyOf(keys);
        for (Listener listener : LISTENERS) listener.changed(player, copy);
    }

    // a new body after death starts with an empty pouch (the persistent data is not carried over); say so
    @SubscribeEvent
    public static void onScrollsRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) show(player);
    }
}
