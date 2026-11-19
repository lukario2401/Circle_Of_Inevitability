package net.lukario.circleofinevitability.combat;

import net.minecraft.world.entity.player.Player;

/** The server-measured result of holding an ability key. Direct casts use NONE. */
public record AbilityCharge(int ticks, int maxTicks, boolean prepaid) {
    public static final AbilityCharge NONE = new AbilityCharge(0, 0);

    public AbilityCharge(int ticks, int maxTicks) {
        this(ticks, maxTicks, false);
    }

    public float progress() {
        return maxTicks == 0 ? 0f : (float) ticks / maxTicks;
    }

    /** Records an already funded cast, or pays normally for a borrowed / unmetered cast. */
    public boolean payBase(Player player, float cost) {
        if (!prepaid) return Spells.paySpirituality(player, cost);
        Casts.spent(player);
        return true;
    }

    /** Metered holds pay once on press and once per interval, and fire when full or unable to pay. */
    public record Rule(int maxTicks, int intervalTicks, int installmentCost) {
        public static final Rule INSTANT = onRelease(0);

        public Rule {
            if (maxTicks < 0 || intervalTicks < 0 || intervalTicks > maxTicks || installmentCost < 0
                    || (intervalTicks == 0) != (installmentCost == 0)) {
                throw new IllegalArgumentException("Invalid charge duration or installment");
            }
        }

        public static Rule onRelease(int ticks) {
            return new Rule(ticks, 0, 0);
        }

        public boolean metered() {
            return intervalTicks > 0;
        }
    }
}
