package net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks;

import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.artifacts.MonocleItem;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Uniqueness;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessItem;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Wielder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Losing yourself (from minLevel): the body acts on its own (the Monocle's acts, plus a war cry) and some inputs never
 * register (attacks, using items and blocks, casts). Both grow from the start of minLevel to full corruption:
 * acts from 1 in actOneInFrom to 1 in actOneInTo each second, swallowed inputs from swallowFrom to swallowTo.
 */
public final class LoseYourselfDrawback implements UniquenessDrawback {
    private static final List<MonocleItem.Act> ACTS = List.of(MonocleItem.Act.GLANCE, MonocleItem.Act.TWITCH,
            MonocleItem.Act.STEP, MonocleItem.Act.JUMP, MonocleItem.Act.SWAP, MonocleItem.Act.WANDER,
            MonocleItem.Act.STARE, MonocleItem.Act.DROP, MonocleItem.Act.STRIKE);

    private final int minLevel, actOneInFrom, actOneInTo;
    private final float swallowFrom, swallowTo;

    public LoseYourselfDrawback(int minLevel, int actOneInFrom, int actOneInTo, float swallowFrom, float swallowTo) {
        this.minLevel = minLevel;
        this.actOneInFrom = actOneInFrom;
        this.actOneInTo = actOneInTo;
        this.swallowFrom = swallowFrom;
        this.swallowTo = swallowTo;
    }

    @Override public int minLevel() { return minLevel; }

    @Override
    public Component describe() {
        return Component.literal("At Extreme you lose yourself: your body acts on its own and some of your inputs never register.");
    }

    /** 0 at the start of minLevel, 1 at full corruption. */
    private float depth(Wielder w) {
        double from = w.uniqueness().level(minLevel).from();
        return (float) Math.clamp((w.corruption() - from) / Math.max(1, Uniqueness.MAX - from), 0, 1);
    }

    public float swallowChance(Wielder w) {
        return swallowFrom + (swallowTo - swallowFrom) * depth(w);
    }

    @Override
    public void second(Wielder w) {
        if (!UniquenessItem.active(this, w)) return;
        int oneIn = Math.round(actOneInFrom + (actOneInTo - actOneInFrom) * depth(w));
        if (w.player().getRandom().nextInt(Math.max(1, oneIn)) == 0) act(w);
    }

    /** One thing the body does on its own, now. */
    public void act(Wielder w) {
        ServerPlayer player = w.player();
        if (player.getRandom().nextInt(ACTS.size() + 1) == ACTS.size()) {
            Spells.sound(w.level(), player.position(), SoundEvents.RAVAGER_ROAR, .8f, 1.3f);
            Component cry = Component.literal(player.getName().getString() + " roars a war cry in a tongue no one speaks any more.")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
            for (ServerPlayer near : w.level().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(16)))
                near.sendSystemMessage(cry);
            return;
        }
        MonocleItem.act(w.level(), player, ACTS.get(player.getRandom().nextInt(ACTS.size())));
    }

    @Override
    public boolean swallows(Wielder w, Entity target) {
        if (w.player().getRandom().nextFloat() >= swallowChance(w)) return false;
        w.player().sendOverlayMessage(Component.literal("Your body does not obey.").withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }
}
