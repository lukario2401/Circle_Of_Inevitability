package net.lukario.circleofinevitability.blasphemy_slate.beyonder;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
/** /soul corruption <player> [amount]. The progression commands are /sequence (SequenceCommands). */
public class SoulCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("soul")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)) // OP only

                // =========================
                // /soul corruption <player> <amount>
                // =========================
                .then(Commands.literal("corruption")
                        .then(Commands.argument("player", EntityArgument.player())
                                // /soul corruption <player>: shows it (was part of the removed /soul get)
                                .executes(ctx -> {
                                    Player player = EntityArgument.getPlayer(ctx, "player");
                                    ctx.getSource().sendSuccess(() -> Component.literal("Corruption of " + player.getName().getString() + ": " + SoulCore.getCorruption(player)), false);
                                    return 1;
                                })
                                // Restrict command inputs to exactly 0 - 100
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                            SoulCore.setCorruption(player, amount);

                                            ctx.getSource().sendSuccess(() ->
                                                            Component.literal("Set Corruption for " + player.getName().getString() + " to " + SoulCore.getCorruption(player)),
                                                    true // Changed to true so server admins see the command execution
                                            );
                                            return 1;
                                        })
                                )
                        )
                )

        );
    }
}
