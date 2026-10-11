package net.lukario.frogerealm.shadow_slave.beyonder;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * /sequence: Pathway, Sequence and spirituality (docs/SEQUENCES.md).
 * The setters need op level 2 like the old /soul setters; info and advance are for everyone.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public class SequenceCommands {

    /** The Pathway names exactly as the classes check them. */
    public static final List<String> PATHWAYS = List.of(
            "Key Of Stars", "Worm Of Time", "Hand Of Order", "Attendant Of Mysteries", "Prince Of Abolition", "Pale Emperor");

    @SubscribeEvent
    public static void onRegisterSequenceCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sequence")
                // /sequence info
                .then(Commands.literal("info")
                        .executes(ctx -> {
                            Player player = ctx.getSource().getPlayerOrException();
                            ctx.getSource().sendSuccess(() -> info(player), false);
                            return 1;
                        }))

                // /sequence pathway <player> "<Pathway name>"
                .then(Commands.literal("pathway")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("pathway_name", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                PATHWAYS.stream().map(name -> "\"" + name + "\""), builder))
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            SoulCore.setPathway(player, StringArgumentType.getString(ctx, "pathway_name"));
                                            ctx.getSource().sendSuccess(() -> Component.literal("Pathway is set to: " + SoulCore.getPathway(player)), false);
                                            return 1;
                                        }))))

                // /sequence set <player> <0-9>
                .then(Commands.literal("set")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("sequence", IntegerArgumentType.integer(Beyonder.STRONGEST_SEQUENCE, Beyonder.WEAKEST_SEQUENCE))
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            SoulCore.setSequence(player, IntegerArgumentType.getInteger(ctx, "sequence"));
                                            ctx.getSource().sendSuccess(() -> Component.literal("Set Sequence to " + SoulCore.getSequence(player)), false);
                                            return 1;
                                        }))))

                // /sequence spirituality <player> <amount>
                .then(Commands.literal("spirituality")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            SoulCore.setSpirituality(player, FloatArgumentType.getFloat(ctx, "amount"));
                                            ctx.getSource().sendSuccess(() -> Component.literal("Set Spirituality to " + SoulCore.getSpirituality(player)), false);
                                            return 1;
                                        }))))
        );
    }

    private static Component info(Player player) {
        return Component.literal("Pathway: " + SoulCore.getPathway(player)
                + " / Sequence: " + SoulCore.getSequence(player)
                + " / Spirituality: " + SoulCore.getSpirituality(player)
                + " / " + Beyonder.maxSpirituality(player));
    }
}
