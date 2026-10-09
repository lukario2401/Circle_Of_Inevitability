package net.lukario.frogerealm.shadow_slave.soul_shards;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "forgerealmmod")
public class SoulShardCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("soul")
                .requires(source -> source.hasPermission(2)) // OP only

                // =========================
                // /soul get <player>
                // =========================
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    Player player = EntityArgument.getPlayer(ctx, "player");

                                    int shards = SoulCore.getSoulShards(player);
                                    int tier = SoulCore.getAspectTier(player);
                                    int stage = SoulCore.getAscensionStage(player);
                                    int corruption = SoulCore.getCorruption(player);
                                    float soulEssence = SoulCore.getSoulEssence(player);
                                    String aspectName = SoulCore.getAspect(player);

                                    ctx.getSource().sendSuccess(() ->
                                                    Component.literal("Player: " + player.getName().getString()
                                                            + " / Ascension Stage: " + stage
                                                            + " / Aspect Name: " + aspectName
                                                            + " / Tier: " + tier
                                                            + " / Corruption: " + corruption
                                                            + " / SoulShards: " + shards
                                                            + " / Soul Essence: " + soulEssence
                                                    ),
                                            false
                                    );
                                    return 1;
                                })
                        )
                )

                // =========================
                // /soul setshards <player> <amount>
                // =========================
                .then(Commands.literal("setshards")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                            SoulCore.setSoulShards(player, amount);

                                            ctx.getSource().sendSuccess(() ->
                                                            Component.literal("Set SoulShards to " + SoulCore.getSoulShards(player)),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
                )

                // =========================
                // /soul addshards <player> <amount>
                // =========================
                .then(Commands.literal("addshards")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                            SoulCore.addSoulShards(player, amount);

                                            ctx.getSource().sendSuccess(() ->
                                                            Component.literal("Added shards. New value = " + SoulCore.getSoulShards(player)),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
                )
                // =========================
                // /soul addSoulEssence <player> <amount>
                // =========================
                .then(Commands.literal("addSoulEssence")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            float amount = FloatArgumentType.getFloat(ctx, "amount");

                                            SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) + amount);

                                            ctx.getSource().sendSuccess(() ->
                                                            Component.literal("Added Soul Essence. New value = " + SoulCore.getSoulEssence(player)),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
                )
                // =========================
                // /soul settier <player> <tier>
                // =========================
                .then(Commands.literal("settier")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("tier", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            int tier = IntegerArgumentType.getInteger(ctx, "tier");

                                            SoulCore.setAspectTier(player, tier);

                                            ctx.getSource().sendSuccess(() ->
                                                            Component.literal("Set Aspect Tier to " + SoulCore.getAspectTier(player)),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
                )
                // =========================
                // /soul corruption <player> <amount>
                // =========================
                .then(Commands.literal("corruption")
                        .then(Commands.argument("player", EntityArgument.player())
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
                // =========================
                // /soul setstage <player> <stage>
                // =========================
                .then(Commands.literal("setstage")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("stage", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            int tier = IntegerArgumentType.getInteger(ctx, "stage");

                                            SoulCore.setAscensionStage(player, tier);

                                            ctx.getSource().sendSuccess(() ->
                                                            Component.literal("Set Ascension Stage to " + SoulCore.getAscensionStage(player)),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
                )
                // =========================
                // /soul setAspect <player> <tier>
                // =========================
                .then(Commands.literal("setAspect")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("aspect_name", StringArgumentType.string())
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            String aspectName = StringArgumentType.getString(ctx, "aspect_name");

                                            SoulCore.setAspect(player, aspectName);

                                            ctx.getSource().sendSuccess(() ->
                                                            Component.literal("Aspect is set to: " + SoulCore.getAspect(player)),
                                                    false
                                            );

                                            return 1;
                                        })
                                )
                        )
                )

        );
    }
}
