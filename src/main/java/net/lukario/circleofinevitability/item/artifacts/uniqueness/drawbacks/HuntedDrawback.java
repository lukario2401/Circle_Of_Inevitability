package net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks;

import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.ShadowSoldier;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessItem;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Wielder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Hunted: every hostile mob within radius[level] blocks knows where the wielder is and comes for them (every two
 * seconds), and the wielder glows, for mobs and other players alike.
 */
public final class HuntedDrawback implements UniquenessDrawback {
    private final int minLevel;
    private final int[] radius;

    public HuntedDrawback(int minLevel, int[] radius) {
        this.minLevel = minLevel;
        this.radius = radius;
    }

    @Override public int minLevel() { return minLevel; }

    @Override
    public Component describe() {
        return Component.literal("Hunted: every hostile mob far around you comes for you, and you glow for everyone to see.");
    }

    @Override
    public void second(Wielder w) {
        if (!UniquenessItem.active(this, w)) return;
        var player = w.player();
        if (!player.hasEffect(MobEffects.GLOWING) || player.getEffect(MobEffects.GLOWING).getDuration() < 30)
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false, true));
        if (w.level().getGameTime() / 20 % 2 == 0) hunt(w);
    }

    /** Sets the mobs on them now. Returns how many. */
    public int hunt(Wielder w) {
        var player = w.player();
        int turned = 0;
        for (Mob mob : w.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(w.at(radius)),
                m -> m instanceof Enemy && m.isAlive() && !m.isNoAi() && !(m instanceof ShadowSoldier)
                        && !Spells.isFriend(player, m))) {
            if (mob.getTarget() == null) {
                mob.setTarget(player);
                if (++turned >= 24) break;
            }
        }
        return turned;
    }
}
