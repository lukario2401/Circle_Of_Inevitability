package net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks;

import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Wielder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * War never rests: the wielder cannot sleep at all. From slowFrom their natural healing (food, potions, anything
 * but an ability or a kill) is multiplied by 'slowShare'; from stopFrom it stops. Abilities (Healing.apply, such as
 * Iron Blood) and the uniqueness's own kill healing always work.
 */
public final class WarNeverRestsDrawback implements UniquenessDrawback {
    private final int slowFrom, stopFrom;
    private final float slowShare;

    public WarNeverRestsDrawback(int slowFrom, float slowShare, int stopFrom) {
        this.slowFrom = slowFrom;
        this.slowShare = slowShare;
        this.stopFrom = stopFrom;
    }

    @Override
    public Component describe() {
        return Component.literal("War never rests: you cannot sleep; at High your healing is halved, at Extreme only "
                + "kills and abilities heal you.");
    }

    @Override
    public Component refusesSleep(Wielder w) {
        return Component.literal("The battlefield calls. You cannot sleep.").withStyle(ChatFormatting.DARK_RED);
    }

    @Override
    public float healed(Wielder w, float amount) {
        if (w.corruptionLevel() >= stopFrom) return 0;
        if (w.corruptionLevel() >= slowFrom) return amount * slowShare;
        return amount;
    }
}
