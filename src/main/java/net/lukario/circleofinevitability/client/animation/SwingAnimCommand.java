package net.lukario.circleofinevitability.client.animation;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

/** Client-only animation workbench. See docs/SWING_TUNING.md for a complete walkthrough. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SwingAnimCommand {
    private static final String HELP = "Swing tuning: /swinganim list | <id> [slow] | inspect <id> | loop <id> [slow] | pose <id> <0..1> | stop\n"
            + "edit <id> ticks <1..1200> | key <index> <field> <value> | easing <index> <easing> | add <time> | remove <index> | mirror\n"
            + "copy <id> <new_id> | global [<name> <value> | reset] | reset <id> | save | load | export <id>\n"
            + "Edits preview immediately. Save explicitly to keep changes. Guide: docs/SWING_TUNING.md";

    private SwingAnimCommand() {}

    @SubscribeEvent
    public static void onSwingAnimRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("swinganim")
                .executes(ctx -> message(ctx, HELP))
                .then(Commands.literal("help").executes(ctx -> message(ctx, HELP)))
                .then(Commands.literal("list").executes(ctx -> message(ctx, list())))
                .then(Commands.literal("stop").executes(ctx -> run(ctx, () -> {
                    SwingPlayer.stopPreview();
                    return "Swing preview stopped.";
                })))
                .then(Commands.literal("loop").then(id().executes(ctx -> preview(ctx, 1, true))
                        .then(Commands.argument("slow", IntegerArgumentType.integer(1, 20))
                                .executes(ctx -> preview(ctx, IntegerArgumentType.getInteger(ctx, "slow"), true)))))
                .then(Commands.literal("pose").then(id()
                        .then(Commands.argument("progress", FloatArgumentType.floatArg(0, 1)).executes(ctx -> run(ctx, () -> {
                            if (!SwingPlayer.hold(text(ctx, "id"), number(ctx, "progress"))) throw new IllegalArgumentException("Unknown swing or no local player.");
                            return "Holding pose. Edits update it immediately; /swinganim stop releases it.";
                        })))))
                .then(Commands.literal("inspect").then(id().executes(ctx -> run(ctx, () -> inspect(animation(ctx))))))
                .then(Commands.literal("copy").then(id().then(Commands.argument("new_id", StringArgumentType.word())
                        .executes(ctx -> run(ctx, () -> {
                            SwingAnimation original = animation(ctx);
                            String target = SwingAnimationEdits.validId(text(ctx, "new_id"));
                            if (SwingAnimations.get(target) != null) throw new IllegalArgumentException("That id already exists; edit it or choose a new id.");
                            return update(SwingAnimationEdits.copy(original, target, original.ticks(), original.keyframes()));
                        })))))
                .then(Commands.literal("edit").then(id()
                        .then(Commands.literal("ticks").then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1200))
                                .executes(ctx -> run(ctx, () -> update(animation(ctx).ticks(IntegerArgumentType.getInteger(ctx, "ticks")))))))
                        .then(Commands.literal("mirror").executes(ctx -> run(ctx, () -> update(animation(ctx).mirrored(text(ctx, "id"))))))
                        .then(Commands.literal("key").then(index()
                                .then(Commands.argument("field", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SwingAnimationEdits.FIELDS, builder))
                                        .then(Commands.argument("value", FloatArgumentType.floatArg()).executes(ctx -> run(ctx, () ->
                                                update(SwingAnimationEdits.key(animation(ctx), index(ctx), text(ctx, "field"), number(ctx, "value")))))))))
                        .then(Commands.literal("easing").then(index()
                                .then(Commands.argument("easing", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(SwingAnimation.Easing.values())
                                                .map(e -> e.name().toLowerCase(Locale.ROOT)), builder))
                                        .executes(ctx -> run(ctx, () -> update(SwingAnimationEdits.easing(animation(ctx), index(ctx), text(ctx, "easing"))))))))
                        .then(Commands.literal("add").then(Commands.argument("at", FloatArgumentType.floatArg(0, 1))
                                .executes(ctx -> run(ctx, () -> update(SwingAnimationEdits.add(animation(ctx), number(ctx, "at")))))))
                        .then(Commands.literal("remove").then(index().executes(ctx -> run(ctx, () ->
                                update(SwingAnimationEdits.remove(animation(ctx), index(ctx)))))))))
                .then(Commands.literal("global")
                        .executes(ctx -> run(ctx, () -> {
                            StringBuilder result = new StringBuilder("Swing globals:");
                            for (String name : SwingTuning.DEFAULTS.keySet()) result.append(" ").append(name).append("=").append(SwingTuning.global(name));
                            return result.toString();
                        }))
                        .then(Commands.literal("reset").executes(ctx -> run(ctx, () -> {
                            SwingTuning.resetGlobals();
                            SwingPlayer.refreshPreview();
                            return "Global tuning restored to built-in values. Use save to keep this reset.";
                        })))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SwingTuning.DEFAULTS.keySet(), builder))
                                .then(Commands.argument("value", FloatArgumentType.floatArg()).executes(ctx -> run(ctx, () -> {
                                    SwingTuning.global(text(ctx, "name"), number(ctx, "value"));
                                    SwingPlayer.refreshPreview();
                                    return "Global " + text(ctx, "name") + " = " + number(ctx, "value") + ". Use save to keep changes.";
                                })))))
                .then(Commands.literal("reset").then(id().executes(ctx -> run(ctx, () -> {
                    animation(ctx); // reject typos
                    SwingTuning.reset(text(ctx, "id"));
                    SwingPlayer.refreshPreview();
                    return "Reset " + text(ctx, "id") + " (custom-only ids are removed). Use save to keep this reset.";
                }))))
                .then(Commands.literal("save").executes(ctx -> run(ctx, () -> {
                    SwingTuning.save();
                    return "Saved swing overrides to " + SwingTuning.file();
                })))
                .then(Commands.literal("load").executes(ctx -> run(ctx, () -> {
                    SwingTuning.load();
                    SwingPlayer.refreshPreview();
                    return "Loaded swing overrides from " + SwingTuning.file();
                })))
                .then(Commands.literal("export").then(id().executes(ctx -> run(ctx, () -> "Exported Java to " + SwingTuning.export(animation(ctx))))))
                .then(id().executes(ctx -> preview(ctx, 1, false))
                        .then(Commands.argument("slow", IntegerArgumentType.integer(1, 20))
                                .executes(ctx -> preview(ctx, IntegerArgumentType.getInteger(ctx, "slow"), false)))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> id() {
        return Commands.argument("id", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SwingAnimations.ids(), builder));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> index() {
        return Commands.argument("index", IntegerArgumentType.integer(0, 127))
                .suggests((ctx, builder) -> {
                    SwingAnimation animation = SwingAnimations.get(text(ctx, "id"));
                    return SharedSuggestionProvider.suggest(animation == null ? java.util.stream.Stream.empty()
                            : java.util.stream.IntStream.range(0, animation.keyframes().size()).mapToObj(Integer::toString), builder);
                });
    }

    private static int index(CommandContext<CommandSourceStack> ctx) {
        return IntegerArgumentType.getInteger(ctx, "index");
    }

    private static String text(CommandContext<CommandSourceStack> ctx, String name) {
        return StringArgumentType.getString(ctx, name);
    }

    private static float number(CommandContext<CommandSourceStack> ctx, String name) {
        return FloatArgumentType.getFloat(ctx, name);
    }

    private static SwingAnimation animation(CommandContext<CommandSourceStack> ctx) {
        SwingAnimation animation = SwingAnimations.get(text(ctx, "id"));
        if (animation == null) throw new IllegalArgumentException("No swing called " + text(ctx, "id") + ". Use /swinganim list.");
        return animation;
    }

    private static String update(SwingAnimation animation) {
        SwingTuning.put(animation);
        SwingPlayer.edited(animation.id());
        return "Updated " + animation.id() + "; preview refreshed. Use inspect to check key indices; save to keep changes.";
    }

    private static String list() {
        StringBuilder result = new StringBuilder("Swings (" + SwingAnimations.ids().size() + "):");
        for (String id : SwingAnimations.ids()) result.append(" ").append(id).append(" ")
                .append(Math.round(SwingAnimations.get(id).ticks() / SwingTuning.global("speed"))).append("t;");
        return result.toString();
    }

    private static String inspect(SwingAnimation animation) {
        StringBuilder result = new StringBuilder(animation.id() + ": " + animation.ticks() + " ticks, mirror=" + animation.mirror());
        var json = SwingAnimationEdits.encode(animation).getAsJsonArray("keys");
        for (int i = 0; i < json.size(); i++) result.append("\n[").append(i).append("] ").append(json.get(i));
        return result.toString();
    }

    private static int preview(CommandContext<CommandSourceStack> ctx, int slow, boolean loop) {
        return run(ctx, () -> {
            animation(ctx);
            boolean ok = loop ? SwingPlayer.loop(text(ctx, "id"), slow) : SwingPlayer.preview(text(ctx, "id"), slow);
            if (!ok) throw new IllegalArgumentException("No local player to preview.");
            return "Previewing " + text(ctx, "id") + " at " + slow + "x slower" + (loop ? " on repeat. /swinganim stop ends it." : ".");
        });
    }

    @FunctionalInterface
    private interface Action { String run() throws IOException; }

    private static int run(CommandContext<CommandSourceStack> ctx, Action action) {
        try {
            return message(ctx, action.run());
        } catch (IndexOutOfBoundsException e) {
            ctx.getSource().sendFailure(Component.literal("No keyframe at that index. Use /swinganim inspect <id>; indices start at 0."));
        } catch (IOException | RuntimeException e) {
            ctx.getSource().sendFailure(Component.literal("Swing tuning failed: " + e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
        return 0;
    }

    private static int message(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
