package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class CodexCommands {
    private CodexCommands() {}
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }
    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("codex")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("study").then(Commands.argument("player",EntityArgument.player())
                .then(Commands.argument("ability",StringArgumentType.greedyString())
                    .suggests((ctx,b)->SharedSuggestionProvider.suggest(Codex.catalog().keySet(),b))
                    .executes(ctx->{
                        var p=EntityArgument.getPlayer(ctx,"player");
                        var power=Codex.catalog().get(StringArgumentType.getString(ctx,"ability"));
                        if(!Codex.learn(p,power,100)) {
                            ctx.getSource().sendFailure(Component.literal("Choose a copyable ability at the target's Sequence or weaker; target must be an Omniscient Eye at Sequence 6 or stronger."));return 0;
                        }
                        ctx.getSource().sendSuccess(()->Component.literal("Fully studied "+power.name()+" for "+p.getName().getString()),true);return 1;
                    }))))
            .then(Commands.literal("studyall").then(Commands.argument("player",EntityArgument.player())
                .executes(ctx->{
                    var p=EntityArgument.getPlayer(ctx,"player");
                    if(!Codex.enabled(p)) {
                        ctx.getSource().sendFailure(Component.literal("Target must be an Omniscient Eye at Sequence 6 or stronger."));return 0;
                    }
                    int count=Codex.learnAll(p);
                    ctx.getSource().sendSuccess(()->Component.literal("Fully studied "+count+" eligible abilities for "+p.getName().getString()),true);
                    return count;
                })))
            .then(Commands.literal("forgetall").then(Commands.argument("player",EntityArgument.player())
                .executes(ctx->forgetAll(ctx.getSource(),EntityArgument.getPlayer(ctx,"player")))))
            .then(Commands.literal("forget").then(Commands.argument("player",EntityArgument.player())
                .then(Commands.literal("all").executes(ctx->forgetAll(ctx.getSource(),EntityArgument.getPlayer(ctx,"player"))))
                .then(Commands.argument("ability",StringArgumentType.greedyString())
                    .suggests((ctx,b)->{
                        var p=EntityArgument.getPlayer(ctx,"player");
                        return SharedSuggestionProvider.suggest(Codex.catalog().keySet().stream().filter(id->Codex.study(p,id)>0),b);
                    })
                    .executes(ctx->{
                        var p=EntityArgument.getPlayer(ctx,"player");String id=StringArgumentType.getString(ctx,"ability");
                        if(!Codex.LIBRARY.forget(p,id)) {
                            ctx.getSource().sendFailure(Component.literal("That ability is not stored in the target's Codex."));return 0;
                        }
                        ctx.getSource().sendSuccess(()->Component.literal("Forgot "+id+" for "+p.getName().getString()),true);return 1;
                    })))));
    }
    private static int forgetAll(CommandSourceStack source,ServerPlayer p) {
        int count=Codex.LIBRARY.forgetAll(p);
        source.sendSuccess(()->Component.literal("Forgot "+count+" abilities and cleared Codex favorites/selection for "+p.getName().getString()),true);
        return 1;
    }
}
