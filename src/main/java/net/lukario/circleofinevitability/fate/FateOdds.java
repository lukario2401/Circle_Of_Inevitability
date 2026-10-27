package net.lukario.circleofinevitability.fate;

/** Material progression weights; live rolls additionally obey the signed luck reserve. */
public final class FateOdds {
    private FateOdds() {}
    // Emerald, Diamond, Netherite, indexed by sequence.
    private static final int[][] LUCKY_WEIGHTS = {
        {0,35,65}, {35,30,15}, {40,22,8}, {35,15,5},
        {20,8,2}, {20,8,2}, {20,8,2}, {20,8,2}, {10,4,1}, {10,4,1}
    };
    private static final int[] UNLUCKY_WEIGHTS = {20,30,50}; // Wood, Stone, Iron
    /** Magnitude is the percent chance of leaving Gold; sign chooses the side of the Wheel. */
    public static int outcome(int sequence, double luck, double biasRoll, double tierRoll) {
        if (sequence < 0 || sequence > 9 || !Double.isFinite(biasRoll) || biasRoll < 0 || biasRoll >= 1
                || !Double.isFinite(tierRoll) || tierRoll < 0 || tierRoll >= 1)
            throw new IllegalArgumentException("Invalid wheel roll");
        double reserve = LuckBalance.bounded(luck);
        if (Math.abs(reserve) <= biasRoll * 100) return 3;
        // Retain the material progression within the lucky side. Negative luck must work at every sequence.
        int[] weights = reserve > 0 ? LUCKY_WEIGHTS[sequence] : UNLUCKY_WEIGHTS;
        int total = weights[0] + weights[1] + weights[2];
        double roll = tierRoll * total;
        for (int tier = 0; tier < weights.length; tier++) {
            roll -= weights[tier];
            if (roll < 0) return tier + (reserve > 0 ? 4 : 0);
        }
        throw new IllegalStateException("Invalid Wheel distribution");
    }
}
