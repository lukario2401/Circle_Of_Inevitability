package net.lukario.circleofinevitability.particles.fx;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.AttendantOfMysteries;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Conqueror;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.HandOfOrder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.KeyOfStars;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PaleEmperor;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PrinceOfAbolition;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.WormOfTime;
import net.lukario.circleofinevitability.combat.Later;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * /slashfx: plays a SlashFx from the code in the game and lets you change its numbers on the spot,
 * to find the right values without restarting. Needs cheats. The guide: docs/SLASH_FX.md
 *
 *   /slashfx WormOfTime.COMBO_TILTED_RING_4                        the slash as it is in the code
 *   /slashfx WormOfTime.COMBO_TILTED_RING_4 pitch 45 roll 20       the same with these numbers changed
 *   /slashfx WormOfTime.COMBO_TILTED_RING_4 radius 3 arc -200 slow 4 times 5
 *   /slashfx arc radius 2 arc 200 color FF70F0E0                   a new one, from SlashFx.arc(...)'s defaults
 *
 * The names are the SlashFx constants of the classes in SOURCES: Class.CONSTANT. The words after the name are
 * pairs, "what" and "value"; "what" is the name of the SlashFx method (radius, arc, width, sweep, spin...),
 * plus yaw, pitch and roll for the three numbers of rotation(...). TAB lists them.
 *
 * It is played around you, turned the way you look, a few times in a row; it stays where you stood, so you can
 * walk around it. The chat line shows the changed part as Java to copy into the constant, and the whole slash
 * is written to the game log.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class SlashFxCommand {

    /** The classes whose SlashFx constants the command knows. A new Pathway class goes here. */
    private static final List<Class<?>> SOURCES = List.of(
            AttendantOfMysteries.class, HandOfOrder.class, KeyOfStars.class,
            PaleEmperor.class, PrinceOfAbolition.class, WormOfTime.class, Conqueror.class);

    // =========================
    // Tuning
    // =========================

    /** How often it is played when "times" is not given. */
    private static final int TIMES = 3;

    /** Ticks between the end of one showing and the start of the next. */
    private static final int PAUSE = 10;

    /** Blocks above your feet when "height" is not given (the melee combos play theirs at about this height). */
    private static final double HEIGHT = 1.1;

    // =========================
    // What can be changed
    // =========================

    /** The SlashFx methods, by their names. */
    private static final List<String> SLASH_KEYS = List.of(
            "radius", "endRadius", "arc", "width", "taper", "layers", "spread",
            "lifetime", "sweep", "delay", "spin", "fromCenter",
            "yaw", "pitch", "roll",
            "color", "tailColor", "headColor", "core", "translucent", "flat", "texture");

    /** Not part of the slash: where and how the command shows it. */
    private static final List<String> SHOW_KEYS = List.of("height", "ahead", "times", "slow");

    private static final List<String> TAPERS = List.of("crescent", "comet", "uniform");

    private SlashFxCommand() {}

    // =========================
    // The command
    // =========================

    @SubscribeEvent
    public static void onSlashFxRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("slashfx")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(help()), false);
                    return 1;
                })
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(names(), builder))
                        .executes(ctx -> play(ctx, ""))
                        .then(Commands.argument("changes", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> suggestChanges(builder))
                                .executes(ctx -> play(ctx, StringArgumentType.getString(ctx, "changes"))))));
    }

    private static int play(CommandContext<CommandSourceStack> ctx, String changes) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel sl = source.getLevel();
        String name = StringArgumentType.getString(ctx, "name");

        Tuned tuned;
        try {
            tuned = tune(name, changes);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }

        // where you stand now and the way you look now: it stays there, so you can walk around it
        float yaw = player.getYRot();
        double angle = Math.toRadians(yaw);
        Vec3 center = player.position().add(0, tuned.height(), 0)
                .add(new Vec3(-Math.sin(angle), 0, Math.cos(angle)).scale(tuned.ahead()));
        SlashFx shown = slowed(tuned.slash(), tuned.slow());
        int every = shown.delayTicks() + shown.lifetimeTicks() + PAUSE;
        for (int i = 0; i < tuned.times(); i++) {
            Later.run(sl, i * every, () -> ParticleShapes.slash(sl, shown, center, yaw, 0f, 0f));
        }

        String whole = java(tuned.slash());
        CircleOfInevitability.LOGGER.info("[slashfx] {}{} =\n{}", name, changes.isEmpty() ? "" : " " + changes, whole);
        String changed = javaOf(tuned.slash(), tuned.changed());
        source.sendSuccess(() -> Component.literal(changed.isEmpty()
                ? "Playing " + name + " as it is in the code. Its Java is in the log."
                : "Playing " + name + " with " + changed + "   (the whole slash is in the log)"), false);
        return 1;
    }

    /** The same slash taking 'slow' times as long, for looking at its shape. */
    private static SlashFx slowed(SlashFx slash, int slow) {
        if (slow <= 1) return slash;
        return slash.lifetime(slash.lifetimeTicks() * slow).sweep(slash.sweepTicks() * slow)
                .delay(slash.delayTicks() * slow).spin(slash.spinDegrees() / slow);
    }

    private static String help() {
        return "/slashfx <name> [what value]...   Names: Class.CONSTANT of a SlashFx in the code, or arc / line for a new one."
                + " What: " + String.join(", ", SLASH_KEYS) + ". Showing it: " + String.join(", ", SHOW_KEYS)
                + ". Example: /slashfx WormOfTime.COMBO_TILTED_RING_4 pitch 45 roll 20 slow 4";
    }

    // =========================
    // TAB
    // =========================

    private static CompletableFuture<Suggestions> suggestChanges(SuggestionsBuilder builder) {
        String typed = builder.getRemaining();
        int wordStart = typed.lastIndexOf(' ') + 1;
        List<String> before = words(typed.substring(0, wordStart));
        SuggestionsBuilder word = builder.createOffset(builder.getStart() + wordStart);
        if (before.size() % 2 == 0) {
            List<String> keys = new ArrayList<>(SLASH_KEYS);
            keys.addAll(SHOW_KEYS);
            keys.removeAll(before);   // (a value is never one of these names, so this only drops what is set already)
            return SharedSuggestionProvider.suggest(keys, word);
        }
        return SharedSuggestionProvider.suggest(valuesFor(before.get(before.size() - 1)), word);
    }

    private static List<String> valuesFor(String key) {
        return switch (key) {
            case "taper" -> TAPERS;
            case "fromCenter", "translucent", "flat" -> List.of("true");
            case "texture" -> List.of("slash/streaks", "slash/smooth", "slash/band", "slash/edge", "slash/dashed",
                    "slash/dashed_fine", "slash/shatter", "slash/crystal");
            default -> List.of();
        };
    }

    // =========================
    // The names
    // =========================

    private static Map<String, SlashFx> known;

    /** Every static SlashFx field of the SOURCES classes, as Class.FIELD, plus "arc" and "line". */
    private static synchronized Map<String, SlashFx> known() {
        if (known != null) return known;
        Map<String, SlashFx> found = new TreeMap<>();
        for (Class<?> source : SOURCES) {
            try {
                for (Field field : source.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) || field.getType() != SlashFx.class) continue;
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value != null) found.put(source.getSimpleName() + "." + field.getName(), (SlashFx) value);
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                CircleOfInevitability.LOGGER.error("/slashfx could not read the slashes of {}", source.getName(), e);
            }
        }
        found.put("arc", SlashFx.arc("slash/streaks"));
        found.put("line", SlashFx.line("slash/smooth"));
        known = found;
        return known;
    }

    static List<String> names() {
        return new ArrayList<>(known().keySet());
    }

    // =========================
    // Changing a slash
    // =========================

    /** The slash to show, which of its values the command changed, and how to show it. */
    record Tuned(SlashFx slash, Set<String> changed, double height, double ahead, int times, int slow) {}

    /** Takes the slash called 'name' and applies the pairs in 'changes' ("radius 3 arc -200"), in the order given. */
    static Tuned tune(String name, String changes) {
        SlashFx slash = known().get(name);
        if (slash == null) {
            throw new IllegalArgumentException("No slash called " + name + ". Press TAB after /slashfx for the names"
                    + " (Class.CONSTANT, or arc / line for a new one).");
        }
        List<String> words = words(changes);
        if (words.size() % 2 != 0) {
            throw new IllegalArgumentException("\"" + words.get(words.size() - 1) + "\" has no value. Write pairs: radius 3 arc -200");
        }

        Set<String> changed = new LinkedHashSet<>();
        double height = HEIGHT, ahead = 0;
        int times = TIMES, slow = 1;
        for (int i = 0; i < words.size(); i += 2) {
            String key = words.get(i), value = words.get(i + 1);
            switch (key) {
                case "height" -> height = decimal(key, value);
                case "ahead" -> ahead = decimal(key, value);
                case "times" -> times = Math.max(1, Math.min(20, whole(key, value)));
                case "slow" -> slow = Math.max(1, Math.min(20, whole(key, value)));
                case "texture" -> {
                    if (!name.equals("arc") && !name.equals("line")) {
                        throw new IllegalArgumentException("texture only works with arc and line: a slash from the code keeps its own.");
                    }
                    slash = retextured(slash, value);
                    changed.add(key);
                }
                default -> {
                    slash = changed(slash, key, value);
                    changed.add(key);
                }
            }
        }
        return new Tuned(slash, changed, height, ahead, times, slow);
    }

    private static SlashFx changed(SlashFx slash, String key, String value) {
        return switch (key) {
            case "radius" -> slash.radius(decimal(key, value));
            case "endRadius" -> slash.endRadius(decimal(key, value));
            case "arc" -> slash.arc(decimal(key, value));
            case "width" -> slash.width(decimal(key, value));
            case "taper" -> slash.taper(taper(value));
            case "layers" -> slash.layers(whole(key, value));
            case "spread" -> slash.spread(decimal(key, value));
            case "lifetime" -> slash.lifetime(whole(key, value));
            case "sweep" -> slash.sweep(whole(key, value));
            case "delay" -> slash.delay(whole(key, value));
            case "spin" -> slash.spin(decimal(key, value));
            case "yaw" -> slash.rotation(decimal(key, value), slash.pitch(), slash.roll());
            case "pitch" -> slash.rotation(slash.yaw(), decimal(key, value), slash.roll());
            case "roll" -> slash.rotation(slash.yaw(), slash.pitch(), decimal(key, value));
            case "color" -> slash.color(color(key, value));
            case "tailColor" -> slash.tailColor(color(key, value));
            case "headColor" -> slash.headColor(color(key, value));
            case "core" -> slash.core(color(key, value));
            case "fromCenter" -> switchedOn(key, value, slash.growsFromCenter()) ? slash.fromCenter() : slash;
            case "translucent" -> switchedOn(key, value, !slash.isAdditive()) ? slash.translucent() : slash;
            case "flat" -> switchedOn(key, value, slash.isFlat()) ? slash.flat() : slash;
            default -> throw new IllegalArgumentException("\"" + key + "\" cannot be changed. There are: "
                    + String.join(", ", SLASH_KEYS) + ", and for showing it: " + String.join(", ", SHOW_KEYS));
        };
    }

    /** A new arc or line with this texture and everything else of 'slash' (only used for the names arc and line). */
    private static SlashFx retextured(SlashFx slash, String texture) {
        SlashFx fresh = slash.shape() == SlashFx.Shape.LINE ? SlashFx.line(texture) : SlashFx.arc(texture);
        fresh = fresh.color(slash.glowColor()).tailColor(slash.tailGlowColor()).headColor(slash.headGlowColor())
                .core(slash.coreColor())
                .radius(slash.radiusBlocks()).arc(slash.arcDegrees()).width(slash.widthBlocks()).taper(slash.taperStyle())
                .layers(slash.layerCount()).spread(slash.spreadBlocks())
                .lifetime(slash.lifetimeTicks()).sweep(slash.sweepTicks()).delay(slash.delayTicks()).spin(slash.spinDegrees())
                .rotation(slash.yaw(), slash.pitch(), slash.roll());
        if (slash.endRadiusBlocks() != slash.radiusBlocks()) fresh = fresh.endRadius(slash.endRadiusBlocks());
        if (!slash.isAdditive()) fresh = fresh.translucent();
        if (slash.isFlat()) fresh = fresh.flat();
        if (slash.growsFromCenter()) fresh = fresh.fromCenter();
        return fresh;
    }

    // ---------- reading the values ----------

    private static List<String> words(String text) {
        List<String> words = new ArrayList<>();
        for (String word : text.trim().split("\\s+")) {
            if (!word.isEmpty()) words.add(word);
        }
        return words;
    }

    private static float decimal(String key, String value) {
        try {
            String number = value.endsWith("f") || value.endsWith("F") ? value.substring(0, value.length() - 1) : value;
            float result = Float.parseFloat(number);
            if (Float.isNaN(result) || Float.isInfinite(result)) throw new NumberFormatException();
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " needs a number, like 2.4 or -30. \"" + value + "\" is not one.");
        }
    }

    private static int whole(String key, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " needs a whole number, like 4. \"" + value + "\" is not one.");
        }
    }

    /** AARRGGBB as in the code, with or without 0x or #. Six digits (RRGGBB) get full alpha. */
    private static int color(String key, String value) {
        String hex = value.startsWith("0x") || value.startsWith("0X") ? value.substring(2)
                : value.startsWith("#") ? value.substring(1) : value;
        try {
            if (hex.length() == 6) return 0xFF000000 | (int) Long.parseLong(hex, 16);
            if (hex.length() == 8) return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            // falls through to the message
        }
        throw new IllegalArgumentException(key + " needs a color like E070F0E0 (alpha, red, green, blue). \"" + value + "\" is not one.");
    }

    private static SlashFx.Taper taper(String value) {
        try {
            return SlashFx.Taper.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("taper is one of " + String.join(", ", TAPERS) + ". \"" + value + "\" is not one.");
        }
    }

    /** SlashFx can switch these on but not off again, like the methods in the code. */
    private static boolean switchedOn(String key, String value, boolean isOn) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) {
            if (isOn) throw new IllegalArgumentException("This slash already has " + key + ", and it cannot be taken off again here."
                    + " Try it on a slash without it.");
            return false;
        }
        throw new IllegalArgumentException(key + " is true or false. \"" + value + "\" is neither.");
    }

    // =========================
    // Writing a slash as Java
    // =========================

    /** The method calls for the values in 'keys', as they are in 'slash' now: ".radius(3f).rotation(0f, 45f, 20f)". */
    static String javaOf(SlashFx slash, Set<String> keys) {
        StringBuilder java = new StringBuilder();
        boolean rotation = false;
        for (String key : keys) {
            switch (key) {
                case "radius" -> java.append(".radius(").append(number(slash.radiusBlocks())).append(')');
                case "endRadius" -> java.append(".endRadius(").append(number(slash.endRadiusBlocks())).append(')');
                case "arc" -> java.append(".arc(").append(number(slash.arcDegrees())).append(')');
                case "width" -> java.append(".width(").append(number(slash.widthBlocks())).append(')');
                case "taper" -> java.append(".taper(SlashFx.Taper.").append(slash.taperStyle().name()).append(')');
                case "layers" -> java.append(".layers(").append(slash.layerCount()).append(')');
                case "spread" -> java.append(".spread(").append(number(slash.spreadBlocks())).append(')');
                case "lifetime" -> java.append(".lifetime(").append(slash.lifetimeTicks()).append(')');
                case "sweep" -> java.append(".sweep(").append(slash.sweepTicks()).append(')');
                case "delay" -> java.append(".delay(").append(slash.delayTicks()).append(')');
                case "spin" -> java.append(".spin(").append(number(slash.spinDegrees())).append(')');
                case "yaw", "pitch", "roll" -> {
                    if (!rotation) {
                        java.append(".rotation(").append(number(slash.yaw())).append(", ").append(number(slash.pitch()))
                                .append(", ").append(number(slash.roll())).append(')');
                    }
                    rotation = true;
                }
                case "color" -> java.append(".color(").append(hex(slash.glowColor())).append(')');
                case "tailColor" -> java.append(".tailColor(").append(hex(slash.tailGlowColor())).append(')');
                case "headColor" -> java.append(".headColor(").append(hex(slash.headGlowColor())).append(')');
                case "core" -> java.append(".core(").append(hex(slash.coreColor())).append(')');
                case "fromCenter" -> { if (slash.growsFromCenter()) java.append(".fromCenter()"); }
                case "translucent" -> { if (!slash.isAdditive()) java.append(".translucent()"); }
                case "flat" -> { if (slash.isFlat()) java.append(".flat()"); }
                default -> { }   // texture: part of SlashFx.arc("...") / line("..."), see java(...)
            }
        }
        return java.toString();
    }

    /** The whole slash as Java, from SlashFx.arc("...") on. Paste it over the constant, or take the lines you need. */
    static String java(SlashFx slash) {
        boolean line = slash.shape() == SlashFx.Shape.LINE;
        Identifier texture = slash.texture();
        String name = texture.getNamespace().equals(CircleOfInevitability.MOD_ID) ? texture.getPath() : texture.toString();
        StringBuilder java = new StringBuilder("SlashFx.").append(line ? "line" : "arc").append("(\"").append(name).append("\")\n");

        java.append("        .color(").append(hex(slash.glowColor())).append(')');
        if (slash.tailGlowColor() != slash.glowColor()) java.append(".tailColor(").append(hex(slash.tailGlowColor())).append(')');
        if (slash.headGlowColor() != slash.glowColor()) java.append(".headColor(").append(hex(slash.headGlowColor())).append(')');
        if (slash.coreColor() != 0) java.append(".core(").append(hex(slash.coreColor())).append(')');
        if (!slash.isAdditive()) java.append(".translucent()");
        if (slash.isFlat()) java.append(".flat()");

        java.append("\n        .radius(").append(number(slash.radiusBlocks())).append(')');
        if (slash.endRadiusBlocks() != slash.radiusBlocks()) java.append(".endRadius(").append(number(slash.endRadiusBlocks())).append(')');
        if (!line) java.append(".arc(").append(number(slash.arcDegrees())).append(')');
        java.append(".width(").append(number(slash.widthBlocks())).append(')');
        java.append(".taper(SlashFx.Taper.").append(slash.taperStyle().name()).append(')');
        if (slash.layerCount() > 1) java.append("\n        .layers(").append(slash.layerCount()).append(").spread(").append(number(slash.spreadBlocks())).append(')');

        java.append("\n        .lifetime(").append(slash.lifetimeTicks()).append(").sweep(").append(slash.sweepTicks()).append(')');
        if (slash.delayTicks() > 0) java.append(".delay(").append(slash.delayTicks()).append(')');
        if (slash.spinDegrees() != 0f) java.append(".spin(").append(number(slash.spinDegrees())).append(')');
        if (slash.growsFromCenter()) java.append(".fromCenter()");

        if (slash.yaw() != 0f || slash.pitch() != 0f || slash.roll() != 0f) {
            java.append("\n        .rotation(").append(number(slash.yaw())).append(", ").append(number(slash.pitch()))
                    .append(", ").append(number(slash.roll())).append(')');
        }
        return java.append(';').toString();
    }

    /** 3 -> "3f", 2.4 -> "2.4f", -0.25 -> "-0.25f". */
    private static String number(float value) {
        if (value == Math.rint(value) && Math.abs(value) < 1.0E7f) return (int) value + "f";
        return value + "f";
    }

    private static String hex(int argb) {
        return String.format(Locale.ROOT, "0x%08X", argb);
    }
}
