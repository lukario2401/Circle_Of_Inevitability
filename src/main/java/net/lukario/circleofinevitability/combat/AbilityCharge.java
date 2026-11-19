package net.lukario.circleofinevitability.combat;

/** The server-measured time an ability key was held, capped at its full charge. */
public record AbilityCharge(int ticks, int maxTicks) {
    public static final AbilityCharge NONE = new AbilityCharge(0, 0);

    public float progress() {
        return maxTicks == 0 ? 0f : (float) ticks / maxTicks;
    }
}
