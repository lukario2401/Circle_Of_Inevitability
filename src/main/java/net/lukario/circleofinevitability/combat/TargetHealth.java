package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** OP opt-in to Illuminator's Appraiser's Eye health counter, regardless of pathway. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TargetHealth {
    private static final String KEY = "circle_of_inevitability_target_health";

    private TargetHealth() {}

    private static boolean enabled(ServerPlayer player) {
        return player.getPersistentData().getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG).getBooleanOr(KEY, false);
    }

    /** Recheck permission while viewing, so de-opping also removes command-granted access. */
    public static boolean isOn(LivingEntity viewer) {
        return viewer instanceof ServerPlayer player && enabled(player)
                && Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("targethealth")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    return turn(player, !enabled(player));
                })
                .then(Commands.literal("on")
                        .executes(ctx -> turn(ctx.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("off")
                        .executes(ctx -> turn(ctx.getSource().getPlayerOrException(), false))));
    }

    private static int turn(ServerPlayer player, boolean on) {
        CompoundTag data = player.getPersistentData();
        CompoundTag kept = data.getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG);
        kept.putBoolean(KEY, on);
        data.put(ServerPlayer.PERSISTED_NBT_TAG, kept);
        player.sendSystemMessage(Component.translatable(
                on ? "message.circle_of_inevitability.target_health.on" : "message.circle_of_inevitability.target_health.off"));
        return 1;
    }
}
