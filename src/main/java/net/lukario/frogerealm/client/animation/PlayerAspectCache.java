package net.lukario.frogerealm.client.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Other players' aspects as this client knows them, so swing rules can match players other than yourself.
 * Filled by CPlayerAspectPacket (sent by AspectSync when an aspect changes and when you join).
 * Plain Java on purpose: the packet class is loaded on a dedicated server too.
 */
public final class PlayerAspectCache {

    private static final Map<UUID, String> ASPECTS = new HashMap<>();

    private PlayerAspectCache() {}

    /** Remembers a player's aspect; null or "none" forgets it. */
    public static void set(UUID player, String aspect) {
        if (player == null) return;
        if (aspect == null || aspect.equals("none")) ASPECTS.remove(player);
        else ASPECTS.put(player, aspect);
    }

    /** The player's aspect, or "none" if this client has not heard of one. */
    public static String get(UUID player) {
        String aspect = ASPECTS.get(player);
        return aspect == null ? "none" : aspect;
    }

    /** Forgets everyone (on leaving a world). */
    public static void clear() {
        ASPECTS.clear();
    }
}
