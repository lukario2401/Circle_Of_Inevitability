package net.lukario.circleofinevitability.ritual;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class RitualCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("favour")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> {
                                    var player = EntityArgument.getPlayer(ctx, "player");
                                    Worship faith = new Worship(player);
                                    int granted = faith.grant(IntegerArgumentType.getInteger(ctx, "amount"));
                                    if (granted == 0) {
                                        ctx.getSource().sendFailure(Component.literal("The player needs a covenant with room for favour or friendship."));
                                        return 0;
                                    }
                                    String pool = faith.exalted() ? " favour" : " friendship";
                                    ctx.getSource().sendSuccess(() -> Component.literal("Granted " + player.getScoreboardName()
                                            + " " + granted + pool + "."), true);
                                    return granted;
                                })))));
    }
}
