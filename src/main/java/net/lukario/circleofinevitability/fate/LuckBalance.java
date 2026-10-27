package net.lukario.circleofinevitability.fate;

/** Pure signed reserve rules. Gold is neutral; spending either sign approaches zero. */
public final class LuckBalance {
    public static final double LIMIT = 100;
    private LuckBalance() {}
    public static double bounded(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Luck must be finite");
        return Math.max(-LIMIT, Math.min(LIMIT, value));
    }
    public static double perSecond(int sequence) {
        if (sequence < 0 || sequence > 9) throw new IllegalArgumentException("Invalid sequence");
        return (10 - sequence) * .1;
    }
    /** Signed transfer accepted by both reserves; resistance reduces acceptance before the debit. */
    public static double transfer(double source, double target, double requested, double resistance) {
        source = bounded(source); target = bounded(target);
        if (!Double.isFinite(requested) || !Double.isFinite(resistance) || resistance <= 0 || resistance > 1)
            throw new IllegalArgumentException("Invalid luck transfer");
        if (requested == 0 || Math.signum(source) != Math.signum(requested)) return 0;
        double room = requested > 0 ? LIMIT - target : LIMIT + target;
        return Math.copySign(Math.min(room, Math.min(Math.abs(source), Math.abs(requested)) * resistance), requested);
    }
    public static double afterRoll(double balance, int tier) {
        if (tier < 0 || tier > 6) throw new IllegalArgumentException("Invalid tier");
        double value = bounded(balance);
        if (tier == 3) return value;
        if ((value > 0 && tier < 3) || (value < 0 && tier > 3) || value == 0)
            throw new IllegalArgumentException("Tier disagrees with luck sign");
        double remaining = Math.max(0, Math.abs(value) - Math.abs(tier - 3));
        return remaining == 0 ? 0 : Math.copySign(remaining, value);
    }
}
