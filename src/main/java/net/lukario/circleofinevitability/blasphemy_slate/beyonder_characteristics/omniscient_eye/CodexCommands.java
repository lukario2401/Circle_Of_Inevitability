package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class CodexCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("codex")
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
            })))));
    }
}
