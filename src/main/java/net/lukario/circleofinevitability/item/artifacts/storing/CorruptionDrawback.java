package net.lukario.circleofinevitability.item.artifacts.storing;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.AbilityCatalog;
import net.minecraft.network.chat.Component;

/**
 * Corrupts the bearer (the mod's soul corruption, SoulCore: red tint, harm from 66, death at 100). It adds a point
 * every secondsPerPoint[tier] while carried and perCast[tier] per stored ability cast, never past cap[tier].
 * Only the share it added is its own: a cleanse removes all of it, putting it away drains it 'recoverPerSecond' a
 * second, and the soul's own slow decay works on it as on any corruption.
 */
public final class CorruptionDrawback implements ArtifactDrawback {
    public static final String SHARE = "corruption_share";
    private static final String CLOCK = "corruption_clock";

    private final int minTier;
    private final int[] secondsPerPoint;
    private final int[] cap;
    private final int[] perCast;
    private final int recoverPerSecond;

    public CorruptionDrawback(int minTier, int[] secondsPerPoint, int[] cap, int[] perCast, int recoverPerSecond) {
        this.minTier = minTier;
        this.secondsPerPoint = secondsPerPoint;
        this.cap = cap;
        this.perCast = perCast;
        this.recoverPerSecond = recoverPerSecond;
    }

    @Override public int minTier() { return minTier; }

    @Override
    public Component describe(int tier) {
        return Component.literal("Corrupts you while carried and with each cast (up to " + Bearer.at(cap, tier)
                + "). Cleanse it, put it away or wait it out.");
    }

    /** How much of the bearer's corruption this artifact put there. */
    public int share(Bearer b) {
        int share = Math.clamp(b.state().getIntOr(SHARE, 0), 0, SoulCore.getCorruption(b.player()));
        b.state().putInt(SHARE, share);                       // the soul's own decay also eats the share
        return share;
    }

    @Override
    public void second(Bearer b) {
        int share = share(b);
        if (!b.carried() || !appliesAt(b.tier())) {
            if (share > 0) remove(b, recoverPerSecond);
            b.state().putInt(CLOCK, 0);
            return;
        }
        if (b.exempt()) return;
        int clock = b.state().getIntOr(CLOCK, 0) + 1;
        if (clock >= b.at(secondsPerPoint)) {
            clock = 0;
            add(b, 1);
        }
        b.state().putInt(CLOCK, clock);
    }

    @Override
    public void cast(Bearer b, AbilityCatalog.Power power) {
        if (!b.exempt()) add(b, b.at(perCast));
    }

    @Override
    public void cleansed(Bearer b) {
        remove(b, Integer.MAX_VALUE);
    }

    /** Adds up to 'amount', never past this tier's cap. */
    public void add(Bearer b, int amount) {
        int now = SoulCore.getCorruption(b.player());
        int added = Math.min(amount, b.at(cap) - now);
        if (added <= 0) return;
        SoulCore.setCorruption(b.player(), now + added);
        b.state().putInt(SHARE, share(b) + added);
    }

    /** Takes back up to 'amount' of its own share. */
    public void remove(Bearer b, int amount) {
        int share = share(b);
        int removed = Math.min(amount, share);
        if (removed <= 0) return;
        SoulCore.setCorruption(b.player(), SoulCore.getCorruption(b.player()) - removed);
        b.state().putInt(SHARE, share - removed);
    }
}
