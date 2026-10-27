package net.lukario.circleofinevitability.fate;

/** The Wheel's complete multiplier table. Fortune is luck strength, not a roll probability. */
public enum WheelTier {
    WOOD("Wood", 0xB08355, .25f, .25f, .25f, 4f),
    STONE("Stone", 0x939BA5, .5f, .5f, .5f, 2f),
    IRON("Iron", 0xDEE0DF, .75f, .75f, .75f, 1f / .75f),
    GOLD("Gold", 0xF4CE62, 1f, 1f, 1f, 1f),
    EMERALD("Emerald", 0x45DB83, 1f, 1.5f, 1.5f, 1f / 1.25f),
    DIAMOND("Diamond", 0x72E5F2, 1.25f, 2f, 1.75f, 1f / 1.5f),
    NETHERITE("Netherite", 0xC3A6DE, 1.5f, 3f, 2f, .5f);

    public static float scaleDamage(float amount, WheelTier attacker, WheelTier defender) {
        return amount * attacker.damage * defender.incoming;
    }

    /** Fractional luck strength preserves its expected buildup without rounding every cast upward. */
    public int fortuneLevels(int base, float roll) {
        float scaled = Math.max(0, base) * fortune;
        int whole = (int) scaled;
        return whole + (roll < scaled - whole ? 1 : 0);
    }

    public final String label;
    public final int color;
    public final float fortune, damage, range, aoe, duration, incoming, regen;
    WheelTier(String label, int color, float fortune, float damage, float size, float incoming) {
        this.label = label; this.color = color; this.fortune = fortune; this.damage = damage;
        this.range = size; this.aoe = size; this.duration = size; this.incoming = incoming; this.regen = size;
    }
}
