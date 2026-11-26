package net.lukario.circleofinevitability.item.artifacts.storing;

import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Mobs smell it: every two seconds, mobs within radius[tier] blocks turn on the bearer. Hostile mobs from minTier;
 * from 'neutralFromTier' every other mob that can fight joins them (wolves, golems, bees...). The bearer's own pets,
 * team and friends (Spells.isFriend) are spared.
 */
public final class AggroDrawback implements ArtifactDrawback {
    private static final int MOST_PER_PULSE = 16;

    private final int minTier;
    private final int[] radius;
    private final int neutralFromTier;

    public AggroDrawback(int minTier, int[] radius, int neutralFromTier) {
        this.minTier = minTier;
        this.radius = radius;
        this.neutralFromTier = neutralFromTier;
    }

    @Override public int minTier() { return minTier; }

    @Override
    public Component describe(int tier) {
        return Component.literal((tier >= neutralFromTier ? "Hostile and neutral mobs" : "Hostile mobs") + " within "
                + Bearer.at(radius, tier) + " blocks hunt you.");
    }

    @Override
    public void second(Bearer b) {
        if (!b.carried() || b.exempt() || !appliesAt(b.tier()) || b.level().getGameTime() / 20 % 2 != 0) return;
        pulse(b);
    }

    /** Turns the nearby mobs on the bearer now. Returns how many. */
    public int pulse(Bearer b) {
        int r = b.at(radius);
        boolean neutral = b.tier() >= neutralFromTier;
        int turned = 0;
        for (Mob mob : b.level().getEntitiesOfClass(Mob.class, b.player().getBoundingBox().inflate(r),
                m -> m.isAlive() && !m.isNoAi() && !Spells.isFriend(b.player(), m))) {
            if (!(mob instanceof Enemy) && !neutral) continue;
            if (mob.getTarget() != b.player()) mob.setTarget(b.player());
            if (++turned >= MOST_PER_PULSE) break;
        }
        return turned;
    }
}
