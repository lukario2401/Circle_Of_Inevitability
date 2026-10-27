package net.lukario.circleofinevitability.fate;

/** Material tiers in Wood..Netherite order. Every row is a percentage distribution summing to 100. */
public final class FateOdds {
    private FateOdds() {}
    private static final int[][] WEIGHTS = {
        {0,0,0,0,0,35,65}, {0,0,0,20,35,30,15}, {0,0,0,30,40,22,8}, {0,0,0,45,35,15,5},
        {5,10,15,40,20,8,2}, {5,10,15,40,20,8,2}, {5,10,15,40,20,8,2}, {5,10,15,40,20,8,2},
        {15,15,20,35,10,4,1}, {15,15,20,35,10,4,1}
    };
    public static int outcome(int sequence, int roll) {
        if (sequence < 0 || sequence > 9 || roll < 0 || roll >= 100) throw new IllegalArgumentException("Invalid wheel roll");
        int total = 0;
        for (int tier = 0; tier < 7; tier++) {
            total += WEIGHTS[sequence][tier];
            if (roll < total) return tier;
        }
        throw new IllegalStateException("Invalid Wheel distribution");
    }
}
