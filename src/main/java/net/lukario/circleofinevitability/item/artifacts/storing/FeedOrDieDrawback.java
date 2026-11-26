package net.lukario.circleofinevitability.item.artifacts.storing;

import net.lukario.circleofinevitability.combat.HudTimer;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * It must be fed: the bearer has to kill a mob every 'periodTicks' of carrying it (a Minecraft day is 24000), or it
 * kills them. The clock only runs while it is carried, so sealing it away (a chest) pauses the hunger. From 75% of
 * the period the bearer starves (Hunger; stronger from 'strongHungerFromTier').
 */
public final class FeedOrDieDrawback implements ArtifactDrawback {
    public static final String HUNGER = "hunger_ticks";
    private static final String WARNED = "hunger_warned";
    private static final int[] WARN_AT_PERCENT = {50, 75, 90};

    private final int minTier;
    private final int periodTicks;
    private final int strongHungerFromTier;

    public FeedOrDieDrawback(int minTier, int periodTicks, int strongHungerFromTier) {
        this.minTier = minTier;
        this.periodTicks = Math.max(20, periodTicks);
        this.strongHungerFromTier = strongHungerFromTier;
    }

    @Override public int minTier() { return minTier; }
    public int periodTicks() { return periodTicks; }

    @Override
    public Component describe(int tier) {
        return Component.literal("Kill a mob every in-game day it is carried, or it kills you.");
    }

    /** Ticks of carrying since it was last fed. */
    public int hunger(Bearer bearer) {
        return Math.max(0, bearer.state().getIntOr(HUNGER, 0));
    }

    @Override
    public void second(Bearer b) {
        if (!b.carried() || b.exempt() || !appliesAt(b.tier())) {
            if (b.state().getIntOr(WARNED, 0) > 0) HudTimer.hide(b.player(), b.key("hunger"));
            b.state().putInt(WARNED, 0);                       // shown again (from the saved hunger) when picked up
            return;
        }
        int hunger = hunger(b) + 20;
        b.state().putInt(HUNGER, hunger);
        if (hunger >= periodTicks) {
            reset(b);
            b.player().sendSystemMessage(Component.literal(b.artifact().name() + " was not fed. It devours you instead.")
                    .withStyle(ChatFormatting.DARK_RED));
            b.player().invulnerableTime = 0;
            b.player().hurtServer(b.level(), b.artifact().damage(b.level()), Float.MAX_VALUE);
            return;
        }
        int percent = (int) (100L * hunger / periodTicks);
        int warned = b.state().getIntOr(WARNED, 0);
        for (int stage = WARN_AT_PERCENT.length; stage >= 1; stage--) {
            if (percent < WARN_AT_PERCENT[stage - 1]) continue;
            if (warned < stage) {
                b.state().putInt(WARNED, stage);
                HudTimer.show(b.player(), b.key("hunger"), "Feed " + b.artifact().name(), periodTicks - hunger, 0x9E1B1B);
                b.player().sendSystemMessage(Component.literal(switch (stage) {
                    case 1 -> b.artifact().name() + " grows hungry. Kill a mob before the day is out.";
                    case 2 -> b.artifact().name() + " is starving, and so are you. Feed it.";
                    default -> b.artifact().name() + " turns toward you. Feed it NOW.";
                }).withStyle(ChatFormatting.RED));
            }
            break;
        }
        if (percent >= 75) Spells.keepEffect(b.player(), MobEffects.HUNGER, b.tier() >= strongHungerFromTier ? 2 : 0, 60);
    }

    @Override
    public void killed(Bearer b, LivingEntity victim) {
        boolean wasHungry = b.state().getIntOr(WARNED, 0) > 0;
        reset(b);
        b.player().sendOverlayMessage(Component.literal(b.artifact().name() + " feeds on " + victim.getName().getString()
                + (wasHungry ? ". It is sated, for now." : ".")).withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    public void bearerDied(Bearer b) {
        reset(b);
    }

    private void reset(Bearer b) {
        b.state().putInt(HUNGER, 0);
        b.state().putInt(WARNED, 0);
        HudTimer.hide(b.player(), b.key("hunger"));
    }
}
