package net.lukario.circleofinevitability.fate;

/** Pure Wheel distribution: 0 Misfortune, 1 Even, 2 Fortune; roll is uniform in [0, 100). */
public final class FateOdds {
    private FateOdds() {}
    public static int outcome(int sequence, int roll) {
        if (sequence < 0 || sequence > 9 || roll < 0 || roll >= 100) throw new IllegalArgumentException("Invalid wheel roll");
        int fortune = sequence == 0 ? 100 : sequence == 1 ? 65 : sequence == 2 ? 50 : sequence == 3 ? 40 : sequence <= 7 ? 30 : 15;
        int misfortune = sequence <= 3 ? 0 : sequence <= 7 ? 10 : 15;
        return roll < fortune ? 2 : roll >= 100 - misfortune ? 0 : 1;
    }
}
