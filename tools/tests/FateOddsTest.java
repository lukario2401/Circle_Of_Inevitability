import net.lukario.circleofinevitability.fate.FateOdds;

/** Exhaustively checks every Wheel stop at every Sequence; run with javac/java, no game runtime needed. */
public class FateOddsTest {
    public static void main(String[] args) {
        int[][] expected = {{0,0,100},{0,35,65},{0,50,50},{0,60,40},{10,60,30},
                {10,60,30},{10,60,30},{10,60,30},{15,70,15},{15,70,15}};
        for (int sequence = 0; sequence <= 9; sequence++) {
            int[] counts = new int[3];
            for (int roll = 0; roll < 100; roll++) counts[FateOdds.outcome(sequence, roll)]++;
            if (!java.util.Arrays.equals(counts, expected[sequence]))
                throw new AssertionError("Sequence " + sequence + ": " + java.util.Arrays.toString(counts));
        }
        for (int[] invalid : new int[][]{{-1,0},{10,0},{0,-1},{0,100}}) {
            try { FateOdds.outcome(invalid[0], invalid[1]); throw new AssertionError("Invalid roll accepted"); }
            catch (IllegalArgumentException expectedError) { }
        }
        System.out.println("PASS: 1,000 Wheel outcomes across all 10 Sequences and 4 invalid inputs");
    }
}
