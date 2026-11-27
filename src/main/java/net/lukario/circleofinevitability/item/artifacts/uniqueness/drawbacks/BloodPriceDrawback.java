package net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks;

import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquePower;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Wielder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.IntToDoubleFunction;

/** Blood price: every cast takes health, by the ability's Sequence (costBySequence). A cast you cannot pay for fails. */
public final class BloodPriceDrawback implements UniquenessDrawback {
    private final IntToDoubleFunction costBySequence;

    public BloodPriceDrawback(IntToDoubleFunction costBySequence) {
        this.costBySequence = costBySequence;
    }

    public float cost(int sequence) {
        return (float) Math.max(0, costBySequence.applyAsDouble(sequence));
    }

    @Override
    public Component describe() {
        return Component.literal("Blood price: each cast costs a little health (" + cost(9) + " for Sequence 9, "
                + cost(0) + " for Sequence 0).");
    }

    @Override
    public Component blocksCast(Wielder w, UniquePower power) {
        float cost = cost(power.sequence());
        if (cost <= 0 || w.player().getHealth() - cost >= 1) return null;
        return Component.literal("Not enough blood for " + power.name() + ".").withStyle(ChatFormatting.DARK_RED);
    }

    @Override
    public void cast(Wielder w, UniquePower power) {
        float cost = cost(power.sequence());
        if (cost > 0) Spells.payBlood(w.player(), cost, 0);
    }
}
