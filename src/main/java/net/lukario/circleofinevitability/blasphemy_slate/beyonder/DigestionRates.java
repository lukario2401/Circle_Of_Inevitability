package net.lukario.circleofinevitability.blasphemy_slate.beyonder;

/**
 * Every digestion rate in one place (docs/SEQUENCES.md, "Tuning digestion").
 * Digestion is a percent (0..100) of the current Sequence; at 100 the player can run /sequence advance.
 *
 * Target: a full bar in about 45 minutes of normal active play, made of roughly
 *   40% time played        (0.9 per minute * 45 minutes = 40.5)
 *   40% spirituality spent (about one full bar of spirituality spent per minute: 0.9 * 45 = 40.5)
 *   20% hostile kills      (about one kill every 30 seconds: 90 kills * 0.25 = 22.5)
 * To make every source faster or slower at once, change SPEED.
 */
public final class DigestionRates {

    private DigestionRates() {}

    /** Multiplies every rate below. 2.0 = a full bar in about 22 minutes, 0.5 = about 90 minutes. */
    public static final float SPEED = 1.0f;

    /** Percent gained per minute online (given in small steps, every TIME_STEP_TICKS). */
    public static final float PERCENT_PER_MINUTE_PLAYED = 0.9f;

    /** How often the time gain is applied, in ticks (20 = once a second). */
    public static final int TIME_STEP_TICKS = 20;

    /** Percent gained for spending spirituality equal to the player's whole maximum (proportional to the cost). */
    public static final float PERCENT_PER_FULL_BAR_SPENT = 0.9f;

    /**
     * At most this many percent from one single spend, so a huge cost (or a tiny maximum) cannot jump the bar.
     * Also limits farming where spirituality refills at once (Sequences 1 and 0).
     */
    public static final float MAX_PERCENT_PER_SPEND = 0.5f;

    /** Percent gained for killing a hostile mob (anything that is an Enemy, like zombies and skeletons). */
    public static final float PERCENT_PER_HOSTILE_KILL = 0.25f;
}
