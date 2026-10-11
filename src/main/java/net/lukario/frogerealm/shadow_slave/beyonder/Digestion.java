package net.lukario.frogerealm.shadow_slave.beyonder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Digestion of the current Sequence (0..100 percent) and advancing to the next one.
 * Gains: time played, spirituality spent, hostile kills. Rates: DigestionRates. Stored by SoulCore next to the Sequence.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class Digestion {

    /** What /sequence advance checks. Replace or combine it to add acting or potions later (see AdvancementRequirement). */
    public static AdvancementRequirement requirement = AdvancementRequirement.DIGESTED;

    private Digestion() {}

    public static void add(Player player, float percent) {
        if (percent <= 0) return;
        SoulCore.setDigestion(player, SoulCore.getDigestion(player) + percent * DigestionRates.SPEED);
    }

    /** Whole percent, rounded down, for messages and the HUD. */
    public static int percent(Player player) {
        return (int) Math.floor(SoulCore.getDigestion(player));
    }

    /** Called by SoulCore.setSpirituality whenever spirituality goes down. */
    public static void onSpiritualitySpent(Player player, float amount) {
        float max = Beyonder.maxSpirituality(player);
        if (max <= 0 || amount <= 0) return;
        add(player, Math.min(DigestionRates.MAX_PERCENT_PER_SPEND, DigestionRates.PERCENT_PER_FULL_BAR_SPENT * amount / max));
    }

    @SubscribeEvent
    public static void onDigestionPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (event.player().level().isClientSide()) return;
        Player player = event.player();
        if (player.tickCount % DigestionRates.TIME_STEP_TICKS != 0) return;
        add(player, DigestionRates.PERCENT_PER_MINUTE_PLAYED * DigestionRates.TIME_STEP_TICKS / 1200f);
    }

    @SubscribeEvent
    public static void onDigestionLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Enemy)) return;
        if (event.getSource().getEntity() instanceof Player killer) {
            add(killer, DigestionRates.PERCENT_PER_HOSTILE_KILL);
        }
    }

    /** Result of an advance attempt, so the command (and tests) can tell what happened. */
    public enum Result { ADVANCED, AT_STRONGEST, REQUIREMENT_NOT_MET }

    /**
     * Goes one Sequence down (stronger) if allowed: refused at Sequence 0 and when the requirement is not met.
     * On success digestion starts again at 0 and the maximum spirituality follows the new Sequence.
     */
    public static Result advance(Player player) {
        int sequence = SoulCore.getSequence(player);
        if (sequence <= Beyonder.STRONGEST_SEQUENCE) {
            player.sendSystemMessage(Component.translatable("message.circle_of_inevitability.sequence.at_strongest"));
            return Result.AT_STRONGEST;
        }
        if (!requirement.isMet(player)) {
            player.sendSystemMessage(requirement.refusal(player));
            return Result.REQUIREMENT_NOT_MET;
        }
        SoulCore.setSequence(player, sequence - 1);
        SoulCore.setDigestion(player, 0f);
        player.sendSystemMessage(Component.translatable("message.circle_of_inevitability.sequence.advanced",
                SoulCore.getSequence(player), SoulCore.getPathway(player)));
        if (player instanceof ServerPlayer sp) {
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 0.8f);
        }
        return Result.ADVANCED;
    }
}
