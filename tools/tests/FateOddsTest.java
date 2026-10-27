import net.lukario.circleofinevitability.fate.FateOdds;
import net.lukario.circleofinevitability.fate.WheelTier;
import net.lukario.circleofinevitability.fate.DisasterBalance;

/** No game runtime needed: exhaustive distributions, exact tier multipliers and bounded disaster growth. */
public class FateOddsTest {
    private static void close(float actual, float expected) {
        if (Math.abs(actual - expected) > .0001f) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        int[][] expected = {{0,0,0,0,0,35,65},{0,0,0,20,35,30,15},{0,0,0,30,40,22,8},{0,0,0,45,35,15,5},
            {5,10,15,40,20,8,2},{5,10,15,40,20,8,2},{5,10,15,40,20,8,2},{5,10,15,40,20,8,2},
            {15,15,20,35,10,4,1},{15,15,20,35,10,4,1}};
        for (int sequence = 0; sequence <= 9; sequence++) {
            int[] counts = new int[7];
            for (int roll = 0; roll < 100; roll++) counts[FateOdds.outcome(sequence, roll)]++;
            if (!java.util.Arrays.equals(counts, expected[sequence])) throw new AssertionError("Sequence " + sequence);
        }
        for (int[] invalid : new int[][]{{-1,0},{10,0},{0,-1},{0,100}}) {
            try { FateOdds.outcome(invalid[0], invalid[1]); throw new AssertionError("Invalid roll accepted"); }
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
        System.out.println("PASS: 1,000 tier outcomes, invalid inputs, all tier stats, 49 damage matchups, 700 luck rolls and disaster bounds/escalation");
    }
}
