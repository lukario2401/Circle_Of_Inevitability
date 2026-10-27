package net.lukario.circleofinevitability.blasphemy_slate.beyonder;

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
 * /sequence: Pathway, Sequence, digestion and spirituality (docs/SEQUENCES.md).
 * The setters need op level 2 like the old /soul setters; info and advance are for everyone.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public class SequenceCommands {

    /** The Pathway names exactly as the classes check them. */
    public static final List<String> PATHWAYS = List.of(
            "Wanderer", "Worm Of Time", "Hand Of Order", "Attendant Of Mysteries", "Frenzied Mage", "Pale Emperor",
            "Conqueror", "Apocalypse", "Dark Angel", "Beauty Goddess", "Hand Of God","Knight Of Misfortune",
            "Abomination", "Prince Of Abolition", "Knowledge Emperor", "Illuminator", "Star Dragon", "Key Of Stars", "Snake Of Mercury");

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

                // /sequence advance
                .then(Commands.literal("advance")
                        .executes(ctx -> Digestion.advance(ctx.getSource().getPlayerOrException()) == Digestion.Result.ADVANCED ? 1 : 0))

                // /sequence digestion <player> <0-100>
                .then(Commands.literal("digestion")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("percent", FloatArgumentType.floatArg(0, 100))
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            SoulCore.setDigestion(player, FloatArgumentType.getFloat(ctx, "percent"));
                                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                                    "message.circle_of_inevitability.sequence.digestion_set", Digestion.percent(player)), false);
                                            return 1;
                                        }))))

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
                                            ctx.getSource().sendSuccess(() -> Component.translatable("message.circle_of_inevitability.sequence.pathway_set", SoulCore.getPathway(player)), false);
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
                                            ctx.getSource().sendSuccess(() -> Component.translatable("message.circle_of_inevitability.sequence.sequence_set", SoulCore.getSequence(player)), false);
                                            return 1;
                                        }))))

                // /sequence spirituality <player> <amount>
                .then(Commands.literal("spirituality")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            SoulCore.setSpiritualityQuietly(player, FloatArgumentType.getFloat(ctx, "amount"));
                                            ctx.getSource().sendSuccess(() -> Component.translatable("message.circle_of_inevitability.sequence.spirituality_set", SoulCore.getSpirituality(player)), false);
                                            return 1;
                                        }))))
        );
    }

    private static Component info(Player player) {
        return Component.translatable("message.circle_of_inevitability.sequence.info",
                SoulCore.getPathway(player), SoulCore.getSequence(player), Digestion.percent(player),
                SoulCore.getSpirituality(player), Beyonder.maxSpirituality(player));
    }
}
