package net.lukario.circleofinevitability.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One player's damage streaks, kept separately for each target. Server thread only. */
public final class DamageTotals {
    private record Streak(double amount, long lastHit) {}

    private final Map<UUID, Streak> targets = new HashMap<>();

    /** Expired streaks are removed before adding the next positive, finite hit. */
    public float add(UUID target, float amount, long now, int resetTicks) {
        if (!Float.isFinite(amount) || amount <= 0f) throw new IllegalArgumentException("Invalid damage");
        if (resetTicks < 1) throw new IllegalArgumentException("Invalid reset duration");
        targets.values().removeIf(streak -> now < streak.lastHit() || now - streak.lastHit() >= resetTicks);
        Streak previous = targets.get(target);
        double total = amount + (previous == null ? 0d : previous.amount());
        targets.put(target, new Streak(total, now));
        return (float) Math.min(total, Float.MAX_VALUE);
    }
}
