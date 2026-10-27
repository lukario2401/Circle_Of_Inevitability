import net.lukario.circleofinevitability.fate.FateOdds;
import net.lukario.circleofinevitability.fate.WheelTier;
import net.lukario.circleofinevitability.fate.DisasterBalance;
import net.lukario.circleofinevitability.fate.LuckBalance;

/** No game runtime needed: exhaustive distributions, exact tier multipliers and bounded disaster growth. */
public class FateOddsTest {
    private static void close(float actual, float expected) {
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > .0001f) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        for (int sequence = 0; sequence <= 9; sequence++) {
            for (int magnitude : new int[]{0, 1, 25, 50, 100}) {
                for (int sign : new int[]{-1, 1}) {
                    int influenced = 0;
                    for (int roll = 0; roll < 100; roll++) {
                        for (int material = 0; material < 100; material++) {
                            int tier = FateOdds.outcome(sequence, sign * magnitude, roll / 100d, material / 100d);
                            if (magnitude == 0 && tier != 3) throw new AssertionError("Zero must be neutral");
                            if (sign > 0 && tier < 3 || sign < 0 && tier > 3) throw new AssertionError("Wrong sign");
                            double after = LuckBalance.afterRoll(sign * magnitude, tier);
                            if (after * sign < 0 || Math.abs(after) > magnitude) throw new AssertionError("Spending crossed zero");
                            if (tier != 3) influenced++;
                        }
                    }
                    if (influenced != magnitude * 100) throw new AssertionError("Incorrect bias probability");
                }
            }
            if (sequence < 9 && LuckBalance.perSecond(sequence) <= LuckBalance.perSecond(sequence + 1))
                throw new AssertionError("Generation must grow with sequence");
        }
        // Check conditional material shares independently of the reserve influence probability.
        int[][] lucky = {{0,35,65},{35,30,15},{40,22,8},{35,15,5},
                {20,8,2},{20,8,2},{20,8,2},{20,8,2},{10,4,1},{10,4,1}};
        for (int sequence = 0; sequence <= 9; sequence++) {
            for (int sign : new int[]{-1, 1}) {
                int[] weights = sign > 0 ? lucky[sequence] : new int[]{20,30,50};
                int total = weights[0] + weights[1] + weights[2];
                int[] counts = new int[7];
                for (int roll = 0; roll < total; roll++)
                    counts[FateOdds.outcome(sequence, sign * 100, .5, (roll + .5) / total)]++;
                for (int i = 0; i < 3; i++)
                    if (counts[i + (sign > 0 ? 4 : 0)] != weights[i]) throw new AssertionError("Wrong material weights");
            }
        }
        close((float) LuckBalance.afterRoll(.25, 6), 0);
        close((float) LuckBalance.afterRoll(-.25, 0), 0);
        close((float) LuckBalance.afterRoll(10, 5), 8);
        close((float) LuckBalance.afterRoll(-10, 1), -8);
        close((float) LuckBalance.afterRoll(-10, 3), -10);
        close((float) LuckBalance.bounded(200), 100);
        close((float) LuckBalance.bounded(-200), -100);
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            try { LuckBalance.bounded(invalid); throw new AssertionError("Nonfinite luck accepted"); }
            catch (IllegalArgumentException expectedError) { }
        }
        for (double[] invalid : new double[][]{{-1,0,0,0},{10,0,0,0},{0,0,-1,0},{0,0,1,0},{0,0,0,1},{0,0,Double.NaN,0}}) {
            try { FateOdds.outcome((int) invalid[0], invalid[1], invalid[2], invalid[3]); throw new AssertionError("Invalid roll accepted"); }
            catch (IllegalArgumentException expectedError) { }
        }
        float[][] multipliers = {{.25f,.25f,.25f,4f},{.5f,.5f,.5f,2f},{.75f,.75f,.75f,1f/.75f},
            {1,1,1,1},{1,1.5f,1.5f,1f/1.25f},{1.25f,2,1.75f,1f/1.5f},{1.5f,3,2,.5f}};
        for (WheelTier tier : WheelTier.values()) {
            float[] row = multipliers[tier.ordinal()];
            close(tier.fortune,row[0]); close(tier.damage,row[1]); close(tier.range,row[2]); close(tier.aoe,row[2]);
            close(tier.duration,row[2]); close(tier.regen,row[2]); close(tier.incoming,row[3]);
            int buildup = 0;
            for (int roll = 0; roll < 100; roll++) buildup += tier.fortuneLevels(1, roll / 100f);
            close(buildup, row[0] * 100);
            for (WheelTier defender : WheelTier.values())
                close(WheelTier.scaleDamage(10, tier, defender), 10 * row[1] * multipliers[defender.ordinal()][3]);
        }
        close(WheelTier.scaleDamage(10, WheelTier.NETHERITE, WheelTier.GOLD), 30);
        close(WheelTier.scaleDamage(10, WheelTier.GOLD, WheelTier.NETHERITE), 5);
        close(WheelTier.scaleDamage(10, WheelTier.WOOD, WheelTier.WOOD), 10);
        for (int level = 0; level <= 10; level++) {
            if (DisasterBalance.chance(level,true) > 1 || DisasterBalance.recovery(level,true) < 20) throw new AssertionError("Unbounded scheduler");
            if (DisasterBalance.damage(10000,level,true,true) > 160) throw new AssertionError("Unbounded damage");
            if (level > 7 && DisasterBalance.damage(100,level,false,true) <= DisasterBalance.damage(100,level-1,false,true)) throw new AssertionError("No escalation");
        }
        close(DisasterBalance.damage(100,10,false,true),62);
        close(DisasterBalance.damage(100,10,true,true),93);
        close(DisasterBalance.damage(100,10,true,false),16);
        if (DisasterBalance.chance(10,true) <= DisasterBalance.chance(6,true)) throw new AssertionError("No frequency escalation");
        System.out.println("PASS: 1,000,000 signed Wheel outcomes, neutral/bias/consumption, passive progression, invalid inputs, tier stats and disaster bounds");
    }
}
