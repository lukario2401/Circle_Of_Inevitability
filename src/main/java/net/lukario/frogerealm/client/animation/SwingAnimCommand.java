package net.lukario.frogerealm.client.animation;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client command to preview swings on yourself, no mobs needed (only you see it):
 *   /swinganim list              every id with its length in ticks
 *   /swinganim horizontal        plays it once with whatever is in your main hand (F5 to see third person)
 *   /swinganim horizontal 5      five times slower, to tune keyframes
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SwingAnimCommand {

    private SwingAnimCommand() {}

    @SubscribeEvent
    public static void onSwingAnimRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("swinganim")
                .then(Commands.literal("list").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(list()), false);
                    return 1;
                }))
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SwingAnimations.ids(), builder))
                        .executes(ctx -> play(ctx, 1))
                        .then(Commands.argument("slow", IntegerArgumentType.integer(1, 20))
                                .executes(ctx -> play(ctx, IntegerArgumentType.getInteger(ctx, "slow"))))));
    }

    /** "Swings (8): horizontal 14t, ..." with each length in ticks at the global speed. */
    private static String list() {
        float speed = SwingAnimations.SPEED > 0f ? SwingAnimations.SPEED : 1f;
        StringBuilder text = new StringBuilder("Swings (" + SwingAnimations.ids().size() + "):");
        String separator = " ";
        for (String id : SwingAnimations.ids()) {
            text.append(separator).append(id).append(' ').append(Math.round(SwingAnimations.get(id).ticks() / speed)).append('t');
            separator = ", ";
        }
        return text.toString();
    }

    private static int play(CommandContext<CommandSourceStack> ctx, int slow) {
        String id = StringArgumentType.getString(ctx, "id");
        if (!SwingPlayer.preview(id, slow)) {
            ctx.getSource().sendFailure(Component.literal("No swing called " + id + ". There are: " + String.join(", ", SwingAnimations.ids())));
            return 0;
        }
        return 1;
    }
}
