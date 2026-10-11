package net.lukario.circleofinevitability.client.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Other players' pathways as this client knows them, so swing rules can match players other than yourself.
 * Filled by CPlayerPathwayPacket (sent by PathwaySync when a pathway changes and when you join).
 * Plain Java on purpose: the packet class is loaded on a dedicated server too.
 */
public final class PlayerPathwayCache {

    private static final Map<UUID, String> PATHWAYS = new HashMap<>();

    private PlayerPathwayCache() {}

    /** Remembers a player's pathway; null or "none" forgets it. */
    public static void set(UUID player, String pathway) {
        if (player == null) return;
        if (pathway == null || pathway.equals("none")) PATHWAYS.remove(player);
        else PATHWAYS.put(player, pathway);
    }

    /** The player's pathway, or "none" if this client has not heard of one. */
    public static String get(UUID player) {
        String pathway = PATHWAYS.get(player);
        return pathway == null ? "none" : pathway;
    }

    /** Forgets everyone (on leaving a world). */
    public static void clear() {
        PATHWAYS.clear();
    }
}
