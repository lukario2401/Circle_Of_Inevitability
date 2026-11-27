package net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks;

import net.lukario.circleofinevitability.combat.HudTimer;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.artifacts.SealedArtifacts;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.ShadowSoldier;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Uniqueness;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessItem;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Wielder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Final conquest: at full corruption a clock starts. After 'seconds' the uniqueness conquers its wielder: they die,
 * and a shadow Usurper with their name rises where they fell, carrying the uniqueness (kill it to take it back).
 * Each kill made at full corruption pushes the end back by 'killBuys' seconds (never past the start).
 */
public final class FinalConquestDrawback implements UniquenessDrawback {
    public static final String CLOCK = "final_clock", CONQUERED = "final_conquered";
    private static final int[] WARN_AT = {60, 30, 10};

    private final int seconds, killBuys, usurperLifetime;

    public FinalConquestDrawback(int seconds, int killBuys, int usurperLifetimeTicks) {
        this.seconds = seconds;
        this.killBuys = killBuys;
        this.usurperLifetime = usurperLifetimeTicks;
    }

    public int seconds() { return seconds; }

    @Override
    public Component describe() {
        return Component.literal("Final conquest: at 100 corruption you have " + seconds / 60 + " minutes (each kill buys "
                + killBuys + " s). Then you die, and a shadow Usurper rises in your place, carrying it.");
    }

    @Override
    public void second(Wielder w) {
        var player = w.player();
        String timer = w.key("final_conquest");
        if (!UniquenessItem.active(this, w) || w.corruption() < Uniqueness.MAX) {
            if (w.state().getIntOr(CLOCK, 0) > 0) HudTimer.hide(player, timer);
            w.state().putInt(CLOCK, 0);
            return;
        }
        int clock = w.state().getIntOr(CLOCK, 0) + 1;
        w.state().putInt(CLOCK, clock);
        int left = seconds - clock;
        if (clock == 1)
            player.sendSystemMessage(Component.literal(w.uniqueness().name() + " is complete in you. Conquer, or be conquered: "
                    + seconds / 60 + " minutes.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        HudTimer.show(player, timer, "Final conquest", Math.max(0, left) * 20, 0xFF3020);
        for (int warn : WARN_AT)
            if (left == warn) player.sendSystemMessage(Component.literal(warn + " seconds left. Kill, or be conquered.")
                    .withStyle(ChatFormatting.RED));
        if (left <= 0) conquer(w);
    }

    /** It takes them now. */
    public void conquer(Wielder w) {
        ServerPlayer player = w.player();
        w.state().putBoolean(CONQUERED, true);
        player.invulnerableTime = 0;
        player.hurtServer(w.level(), w.uniqueness().damage(w.level()), Float.MAX_VALUE);
        if (player.isAlive()) player.kill(w.level());
    }

    @Override
    public void killed(Wielder w, LivingEntity victim) {
        int clock = w.state().getIntOr(CLOCK, 0);
        if (clock > 0) w.state().putInt(CLOCK, Math.max(1, clock - killBuys));
    }

    /** Conquered: the uniqueness leaves their inventory with the Usurper, who rises where they fell. */
    @Override
    public void died(Wielder w) {
        ServerPlayer player = w.player();
        HudTimer.hide(player, w.key("final_conquest"));
        w.state().putInt(CLOCK, 0);
        if (!w.state().getBooleanOr(CONQUERED, false)) return;
        w.state().putBoolean(CONQUERED, false);
        ItemStack stack = SealedArtifacts.carried(player, w.uniqueness().id());
        ItemStack taken = stack.copy();
        if (!stack.isEmpty()) stack.setCount(0);
        ShadowSoldier usurper = ShadowSoldier.rise(w.level(), player.position(), 3, ShadowSoldier.USURPER, player,
                false, false, usurperLifetime);
        usurper.setCustomName(Component.literal(player.getName().getString() + ", Usurper").withStyle(ChatFormatting.DARK_RED));
        usurper.setCustomNameVisible(true);
        usurper.carry(taken);
        Spells.sound(w.level(), player.position(), SoundEvents.WITHER_SPAWN, 1f, .5f);
        Component news = Component.literal(player.getName().getString() + " was conquered by " + w.uniqueness().name()
                + ". Something wearing their name rises.").withStyle(ChatFormatting.DARK_RED);
        for (ServerPlayer near : w.level().players()) if (near.distanceToSqr(player) < 128 * 128) near.sendSystemMessage(news);
    }
}
