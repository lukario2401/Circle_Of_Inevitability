package net.lukario.circleofinevitability.blasphemy_slate.beyonder;

import net.minecraft.world.entity.player.Player;

/**
 * Readable Sequence checks and the numbers derived from the Sequence (see docs/SEQUENCES.md).
 * The Sequence itself is stored by SoulCore.
 */
public final class Beyonder {

    public static final int WEAKEST_SEQUENCE = 9;
    public static final int STRONGEST_SEQUENCE = 0;

    private Beyonder() {}

    /** True if the player is at that Sequence or a stronger one (a lower number): hasSequence(p, 6) = "Sequence 6 or better". */
    public static boolean hasSequence(Player player, int sequence) {
        return SoulCore.getSequence(player) <= sequence;
    }

    /**
     * The old ascension level (9 - Sequence), kept inside the range the old formulas were written for (1..8):
     * Sequence 9 scales like Sequence 8, and Sequences 1 and 0 like Sequence 1. Only for scaling, never for gates.
     */
    public static int powerLevel(Player player) {
        return Math.min(8, Math.max(1, 9 - SoulCore.getSequence(player)));
    }

    /** The old power multiplier (1..7, set by hand before), now derived from the Sequence: 9 - Sequence, at most 7. */
    public static int powerScale(Player player) {
        return Math.min(7, Math.max(1, 9 - SoulCore.getSequence(player)));
    }

    /**
     * Maximum spirituality: the old formula with a full bar of the old progression resource,
     * (250 * scale * level / 100) * (level * 10) * scale, with the same integer steps as before.
     */
    public static float maxSpirituality(Player player) {
        int level = powerLevel(player);
        int scale = powerScale(player);
        return (float) (((250 * scale * level) / 100) * (level * 10)) * scale;
    }

    /** Spirituality regained per tick: the old formula scale * scale * level / (8 - level) (level 8 = refills at once). */
    public static float spiritualityRegenPerTick(Player player) {
        int level = powerLevel(player);
        int scale = powerScale(player);
        return (float) (scale * scale * level) / (8 - level);
    }
}
