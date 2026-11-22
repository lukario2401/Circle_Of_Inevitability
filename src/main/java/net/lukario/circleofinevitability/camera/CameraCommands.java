package net.lukario.circleofinevitability.camera;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CameraCommands {
    private CameraCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        var preset = Commands.argument("preset", IdentifierArgument.id())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(CameraPresets.loaded.keySet(), b))
                .executes(ctx -> result(ctx.getSource(), DetachedCamera.start(ctx.getSource().getPlayerOrException(),
                        IdentifierArgument.getId(ctx, "preset").toString())))
                .then(Commands.literal("at").then(Commands.argument("position", Vec3Argument.vec3())
                        .executes(ctx -> result(ctx.getSource(), DetachedCamera.at(ctx.getSource().getPlayerOrException(),
                                IdentifierArgument.getId(ctx, "preset").toString(), Vec3Argument.getVec3(ctx, "position"))))))
                .then(Commands.literal("follow").then(Commands.argument("anchor", EntityArgument.entity())
                        .executes(ctx -> result(ctx.getSource(), DetachedCamera.follow(ctx.getSource().getPlayerOrException(),
                                IdentifierArgument.getId(ctx, "preset").toString(), EntityArgument.getEntity(ctx, "anchor"))))));
        event.getDispatcher().register(Commands.literal("coicamera")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("start").then(preset))
                .then(Commands.literal("move").then(Commands.argument("position", Vec3Argument.vec3())
                        .then(Commands.argument("rotation", RotationArgument.rotation())
                                .executes(ctx -> move(ctx, 1))
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                                        .executes(ctx -> move(ctx, IntegerArgumentType.getInteger(ctx, "ticks")))))))
                .then(Commands.literal("stop").executes(ctx -> {
                    var player = ctx.getSource().getPlayerOrException();
                    if (!DetachedCamera.active(player)) {
                        ctx.getSource().sendFailure(Component.literal("You have no active camera."));
                        return 0;
                    }
                    DetachedCamera.stop(player);
                    return 1;
                })));
    }

    private static int move(CommandContext<CommandSourceStack> ctx, int ticks) throws CommandSyntaxException {
        var source = ctx.getSource();
        var camera = DetachedCamera.handle(source.getPlayerOrException());
        var rotation = RotationArgument.getRotation(ctx, "rotation").getRotation(source);
        var position = Vec3Argument.getVec3(ctx, "position");
        if (camera.isPresent() && camera.get().moveTo(position, rotation.y, rotation.x, ticks)) return 1;
        source.sendFailure(Component.literal("Start a scripted camera and choose a position in tracked terrain."));
        return 0;
    }

    private static int result(CommandSourceStack source, boolean opened) {
        if (!opened) source.sendFailure(Component.literal(
                "Camera could not start. Check the preset, an existing view, and whether the anchor is visible to your client and clear of obstructions."));
        return opened ? 1 : 0;
    }
}
