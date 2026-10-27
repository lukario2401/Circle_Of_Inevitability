package net.lukario.circleofinevitability.fate;

/** Pure, bounded combat numbers shared by the mishap scheduler and its checks. */
public final class DisasterBalance {
    private DisasterBalance() {}
    public static int bounded(int level) { return Math.clamp(level, 0, 10); }
    public static float chance(int level, boolean mob) {
        int n = bounded(level);
        return mob && n >= 7 ? Math.min(.9f, .5f + (n - 6) * .1f) : Math.min(.5f, n * .05f);
    }
    public static int recovery(int level, boolean mob) { return mob && level >= 7 ? Math.max(20, 60 - (bounded(level) - 6) * 10) : 60; }
    public static float damage(float maxHealth, int level, boolean catastrophe, boolean mob) {
        int n = bounded(level);
        if (!mob || n < 7) return Math.min(catastrophe ? 16f : 10f, maxHealth * (catastrophe ? .3f : .15f));
        return Math.min(160f, (12f + n * 2f + maxHealth * (.1f + (n - 6) * .05f)) * (catastrophe ? 1.5f : 1f));
    }
}
