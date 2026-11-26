package net.lukario.circleofinevitability.item.artifacts.storing;

import net.lukario.circleofinevitability.combat.AbilityCatalog;
import net.lukario.circleofinevitability.combat.HudTimer;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.artifacts.SealedArtifactItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/**
 * Using its power makes the bearer thirst: after a stored ability is cast, they must drink Mob Blood (the Mob Blood
 * Vial: kill a mob with an empty bottle in the off hand) before that in-game day ends (at least 'minWindowTicks'
 * later). Past the deadline they are parched until they drink, whether it is still carried or not: Hunger and
 * Weakness, damage[tier] hearts every three seconds, and no stored ability can be cast.
 */
public final class BloodThirstDrawback implements ArtifactDrawback {
    public static final String UNTIL = "thirst_until";
    private static final String TIER = "thirst_tier";
    private static final String TOLD = "thirst_told";
    public static final int DAY = 24000;

    private final int minTier;
    private final float[] damage;
    private final int minWindowTicks;

    public BloodThirstDrawback(int minTier, float[] damage, int minWindowTicks) {
        this.minTier = minTier;
        this.damage = damage;
        this.minWindowTicks = minWindowTicks;
    }

    @Override public int minTier() { return minTier; }

    @Override
    public Component describe(int tier) {
        return Component.literal("Cast with it, and you must drink Mob Blood before the day ends, or wither.");
    }

    public boolean thirsty(Bearer b) {
        return b.state().getLongOr(UNTIL, 0L) > 0;
    }

    public boolean parched(Bearer b) {
        long until = b.state().getLongOr(UNTIL, 0L);
        return until > 0 && b.level().getGameTime() >= until;
    }

    @Override
    public void cast(Bearer b, AbilityCatalog.Power power) {
        if (b.exempt() || thirsty(b)) return;
        long now = b.level().getGameTime();
        long endOfDay = (now / DAY + 1) * DAY;
        long until = Math.max(endOfDay, now + minWindowTicks);
        b.state().putLong(UNTIL, until);
        b.state().putInt(TIER, b.tier());
        b.state().putBoolean(TOLD, false);
        HudTimer.show(b.player(), b.key("thirst"), "Drink Mob Blood", (int) (until - now), 0xB0102A);
        b.player().sendSystemMessage(Component.literal(b.artifact().name()
                + " drank of you to cast. Drink Mob Blood before the day ends.").withStyle(ChatFormatting.RED));
    }

    @Override
    public Component blocksCast(Bearer b) {
        return parched(b) ? Component.literal("You are parched. Drink Mob Blood before " + b.artifact().name()
                + " will answer again.").withStyle(ChatFormatting.RED) : null;
    }

    @Override
    public void second(Bearer b) {
        if (!thirsty(b)) return;
        if (b.exempt()) {
            quench(b);
            return;
        }
        if (!parched(b)) return;
        ServerPlayer p = b.player();
        int tier = Math.max(minTier, b.state().getIntOr(TIER, minTier));
        if (!b.state().getBooleanOr(TOLD, false)) {
            b.state().putBoolean(TOLD, true);
            p.sendSystemMessage(Component.literal("The day is over and you have not drunk. Your veins dry out.")
                    .withStyle(ChatFormatting.DARK_RED));
        }
        Spells.keepEffect(p, MobEffects.HUNGER, 1, 60);
        Spells.keepEffect(p, MobEffects.WEAKNESS, Math.max(0, tier - minTier), 60);
        if (b.level().getGameTime() / 20 % 3 == 0) {
            p.invulnerableTime = 0;
            p.hurtServer(b.level(), b.artifact().damage(b.level()), Bearer.at(damage, tier) * 2f);
        }
        if (p.tickCount % 200 < 20) p.sendOverlayMessage(Component.literal("Parched: drink Mob Blood.").withStyle(ChatFormatting.RED));
    }

    @Override
    public void bearerDied(Bearer b) {
        quench(b);
    }

    public void quench(Bearer b) {
        b.state().remove(UNTIL);
        b.state().remove(TIER);
        b.state().remove(TOLD);
        HudTimer.hide(b.player(), b.key("thirst"));
    }

    /**
     * The player drank Mob Blood: every storing artifact's thirst is quenched. False if none was thirsty (then the
     * vial is not used up).
     */
    public static boolean quenchAll(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        boolean any = false;
        for (StoringArtifact artifact : StoringArtifact.all()) {
            for (BloodThirstDrawback thirst : artifact.drawbacksOf(BloodThirstDrawback.class)) {
                Bearer b = new Bearer(artifact, level, player, ItemStack.EMPTY, 0, SealedArtifactItem.state(player, artifact.id()));
                if (!thirst.thirsty(b)) continue;
                thirst.quench(b);
                any = true;
            }
        }
        return any;
    }
}
