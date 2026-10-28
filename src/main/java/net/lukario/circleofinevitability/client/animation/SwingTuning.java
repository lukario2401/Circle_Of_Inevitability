package net.lukario.circleofinevitability.client.animation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Client-local overrides. Disk loading replaces state only after the whole document validates. */
public final class SwingTuning {
    public static final Map<String, Float> DEFAULTS = Map.of(
            "offset_x", SwingAnimations.FIRST_PERSON_OFFSET_X, "offset_y", SwingAnimations.FIRST_PERSON_OFFSET_Y,
            "offset_z", SwingAnimations.FIRST_PERSON_OFFSET_Z, "scale", SwingAnimations.FIRST_PERSON_SCALE,
            "speed", SwingAnimations.SPEED);
    private static Map<String, SwingAnimation> overrides = new LinkedHashMap<>();
    private static Map<String, Float> globals = new LinkedHashMap<>();
    private static boolean loaded;

    private SwingTuning() {}

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("circle_of_inevitability-swinganim.json");
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(file())) return;
        try {
            load();
        } catch (IOException | RuntimeException e) {
            CircleOfInevitability.LOGGER.warn("Could not load swing tuning from {}; using built-in animations. Use /swinganim load for details.", file(), e);
        }
    }

    public static SwingAnimation override(String id) {
        ensureLoaded();
        return overrides.get(id);
    }

    public static Set<String> ids() {
        ensureLoaded();
        return java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(overrides.keySet()));
    }

    public static void put(SwingAnimation animation) {
        ensureLoaded();
        overrides.put(animation.id(), animation);
    }

    public static void reset(String id) {
        ensureLoaded();
        overrides.remove(id);
    }

    public static float global(String name) {
        ensureLoaded();
        if (!DEFAULTS.containsKey(name)) throw new IllegalArgumentException("Unknown global: " + name);
        return globals.getOrDefault(name, DEFAULTS.get(name));
    }

    public static void global(String name, float value) {
        global(name); // validates name and initializes storage
        globals.put(name, validateGlobal(name, value));
    }

    private static float validateGlobal(String name, float value) {
        if (!DEFAULTS.containsKey(name)) throw new IllegalArgumentException("Unknown global: " + name);
        return SwingAnimationEdits.bounded(value, name.equals("scale") || name.equals("speed") ? .05f : -16,
                name.equals("speed") ? 10 : name.equals("scale") ? 10 : 16);
    }

    public static void resetGlobals() {
        ensureLoaded();
        globals.clear();
    }

    public static void load() throws IOException {
        JsonObject root;
        try (var reader = Files.newBufferedReader(file())) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        if (SwingAnimationEdits.number(root, "version") != 1) throw new IllegalArgumentException("Unsupported swing tuning version (expected 1).");
        Map<String, Float> newGlobals = new LinkedHashMap<>();
        for (var entry : root.getAsJsonObject("globals").entrySet())
            newGlobals.put(entry.getKey(), validateGlobal(entry.getKey(), SwingAnimationEdits.number(root.getAsJsonObject("globals"), entry.getKey())));
        Map<String, SwingAnimation> newOverrides = new LinkedHashMap<>();
        for (var entry : root.getAsJsonObject("animations").entrySet())
            newOverrides.put(entry.getKey(), SwingAnimationEdits.decode(entry.getKey(), entry.getValue().getAsJsonObject()));
        globals = newGlobals;
        overrides = newOverrides;
        loaded = true;
    }

    public static void save() throws IOException {
        ensureLoaded();
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonObject settings = new JsonObject();
        globals.forEach(settings::addProperty);
        root.add("globals", settings);
        JsonObject animations = new JsonObject();
        overrides.forEach((id, animation) -> animations.add(id, SwingAnimationEdits.encode(animation)));
        root.add("animations", animations);
        Files.createDirectories(file().getParent());
        Path temp = Files.createTempFile(file().getParent(), "swinganim-", ".tmp");
        try {
            Files.writeString(temp, new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n");
            Files.move(temp, file(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static Path export(SwingAnimation animation) throws IOException {
        Path target = file().getParent().resolve("swinganim-export").resolve(SwingAnimationEdits.validId(animation.id()) + ".java.txt");
        Files.createDirectories(target.getParent());
        StringBuilder text = new StringBuilder("// Global tuning (copy into SwingAnimations):\n");
        for (String name : DEFAULTS.keySet()) text.append("// ").append(name).append(" = ").append(global(name)).append("f\n");
        text.append(SwingAnimationEdits.javaSource(animation));
        Files.writeString(target, text);
        return target;
    }
}
