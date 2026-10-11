package net.lukario.circleofinevitability.blasphemy_slate.beyonder;

import net.minecraft.world.entity.player.Player;

/**
 * What a Sequence gives a Beyonder: the gate check that abilities use, and the numbers that grow with the
 * Sequence (power level, power scale, maximum spirituality, spirituality regained per tick).
 * See docs/SEQUENCES.md. The Sequence itself is stored by SoulCore.
 *
 * Every number comes from one table, SEQUENCES, with one row per Sequence. To rebalance a Sequence, change its row.
 * Sequences 9 to 2 each have their own numbers. Sequences 1 and 0 have none of their own yet and repeat Sequence 2.
 * Each number can be asked for a player or for a Sequence number (for code that has no Player: HUD, commands, tests).
 */
public final class Beyonder {

    public static final int WEAKEST_SEQUENCE = 9;
    public static final int STRONGEST_SEQUENCE = 0;

    /**
     * The numbers of one Sequence.
     * powerLevel: 1..8, for scaling only (Key Of Stars caps its stars at it). Never use it for gates; use hasSequence.
     * powerScale: 1..7, the power level capped at 7.
     * maxSpirituality: the size of the spirituality bar.
     * regenPerTick: spirituality regained each tick (20 ticks = 1 second).
     */
    private record SequenceStats(int powerLevel, int powerScale, float maxSpirituality, float regenPerTick) {}

    /** Index = Sequence number, 0 (strongest) to 9 (weakest). */
    private static final SequenceStats[] SEQUENCES = {
            //                level  scale  maxSpirituality  regenPerTick
            new SequenceStats(8,     7,     78400,           78400),     // Sequence 0: same as Sequence 2 for now
            new SequenceStats(8,     7,     78400,           78400),     // Sequence 1: same as Sequence 2 for now
            new SequenceStats(8,     7,     78400,           78400),     // Sequence 2: a full bar each tick, refills at once
            new SequenceStats(7,     7,     59780,           343),       // Sequence 3
            new SequenceStats(6,     6,     32400,           108),       // Sequence 4
            new SequenceStats(5,     5,     15500,           125f / 3),  // Sequence 5
            new SequenceStats(4,     4,     6400,            16),        // Sequence 6
            new SequenceStats(3,     3,     1980,            27f / 5),   // Sequence 7
            new SequenceStats(2,     2,     400,             8f / 6),    // Sequence 8
            new SequenceStats(1,     1,     20,              1f / 7),    // Sequence 9
    };

    private Beyonder() {}

    // =========================
    // Gates
    // =========================

    /** True if the player is at that Sequence or a stronger one (a lower number): hasSequence(p, 6) = "Sequence 6 or better". */
    public static boolean hasSequence(Player player, int sequence) {
        if (isCastingUnbound(player)) return true;      // a stolen ability: see castUnbound below
        return SoulCore.getSequence(player) <= sequence;
    }

    // =========================
    // Unbound casts: an ability cast with borrowed power (Worm Of Time's theft)
    // =========================

    private static Player unboundCaster;        // set only while castUnbound runs (abilities run on the server thread)
    private static boolean unboundCostWaived;

    /**
     * Runs 'cast' as if this player had every Sequence and all the spirituality they need: while it runs,
     * hasSequence is true for them and nothing is taken from their spirituality.
     *
     *   boolean wentOff = Beyonder.castUnbound(player, () -> ability.cast().run(player, level, sl));
     *
     * Returns true if the ability went off. That is known from its cost: every ability checks first and pays
     * after, so one that tried to pay has been cast, and one that did not has refused (no target, no room).
     * Only what runs inside 'cast' is free. What the ability does later (Later.run, an upkeep each tick) is paid as usual.
     */
    public static boolean castUnbound(Player player, Runnable cast) {
        Player casterBefore = unboundCaster;
        boolean waivedBefore = unboundCostWaived;
        unboundCaster = player;
        unboundCostWaived = false;
        try {
            cast.run();
            return unboundCostWaived;
        } finally {
            unboundCaster = casterBefore;
            unboundCostWaived = waivedBefore;
        }
    }

    /** True while castUnbound is running for this player. */
    public static boolean isCastingUnbound(Player player) {
        return player != null && player == unboundCaster;
    }

    /** SoulCore calls this when it leaves a cost unpaid during castUnbound. */
    static void costWaived() {
        unboundCostWaived = true;
    }

    // =========================
    // Scaling
    // =========================

    public static int powerLevel(Player player) {
        return powerLevel(SoulCore.getSequence(player));
    }

    public static int powerLevel(int sequence) {
        return stats(sequence).powerLevel();
    }

    public static int powerScale(Player player) {
        return powerScale(SoulCore.getSequence(player));
    }

    public static int powerScale(int sequence) {
        return stats(sequence).powerScale();
    }

    // =========================
    // Spirituality
    // =========================

    public static float maxSpirituality(Player player) {
        return maxSpirituality(SoulCore.getSequence(player));
    }

    public static float maxSpirituality(int sequence) {
        return stats(sequence).maxSpirituality();
    }

    public static float spiritualityRegenPerTick(Player player) {
        return spiritualityRegenPerTick(SoulCore.getSequence(player));
    }

    public static float spiritualityRegenPerTick(int sequence) {
        return stats(sequence).regenPerTick();
    }

    /** The row of a Sequence. A number outside 0..9 (edited save data) uses the nearest valid Sequence instead of crashing. */
    private static SequenceStats stats(int sequence) {
        return SEQUENCES[Math.min(WEAKEST_SEQUENCE, Math.max(STRONGEST_SEQUENCE, sequence))];
    }
}