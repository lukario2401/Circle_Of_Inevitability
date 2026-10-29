package net.lukario.circleofinevitability.boss.star_dragon;

import com.google.gson.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Immutable, atomically replaced settings. Missing attacks inherit the shared characteristic's values. */
public record StarDragonBossSettings(double health, double meleeDamage, double abilityDamageMultiplier, double speed, double followRange,
        int thinkTicks, int recoveryTicks, double enragedBelow, double enragedWeightMultiplier,
        boolean passives, boolean radiantFlight, double hoverHeight, double enragedWindupMultiplier,
        double enragedRecoveryMultiplier, Singularity singularity, Map<String, Attack> attacks) {
    public static final String RESOURCE = "/data/circle_of_inevitability/bosses/star_dragon.json";
    public static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("circle_of_inevitability/star_dragon_boss.json");
    public static final String RESOURCE_ZERO = "/data/circle_of_inevitability/bosses/star_dragon_condenser.json";
    public static final Path FILE_ZERO = FMLPaths.CONFIGDIR.get().resolve("circle_of_inevitability/star_dragon_condenser_boss.json");
    private static volatile Map<Integer, StarDragonBossSettings> current;

    public record Singularity(double healthCostFraction, int spentTicks, float damageCap) {}

    public record Attack(boolean enabled, int weight, int windupTicks, int recoveryTicks,
                         int cooldownTicks, float cost, double range, double minHealth, double maxHealth,
                         String animation, List<String> followups) {
        public MobAbility tune(MobAbility base) {
            return base.copy().weight(weight).cooldown(cooldownTicks).cost(cost).range(range);
        }
    }
    public static StarDragonBossSettings get() { return get(2); }
    public static StarDragonBossSettings get(int sequence) {
        if (current == null) current = Map.of(2, bundled(RESOURCE), 0, bundled(RESOURCE_ZERO));
        return Objects.requireNonNull(current.get(sequence), "Unsupported boss Sequence: " + sequence);
    }
    private static StarDragonBossSettings bundled(String resource) {
        try (Reader r = new InputStreamReader(Objects.requireNonNull(StarDragonBossSettings.class.getResourceAsStream(resource)), java.nio.charset.StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(r).getAsJsonObject());
        } catch (IOException e) { throw new IllegalStateException("Cannot read bundled boss settings", e); }
    }
    /** Both profiles are parsed and validated before either replaces the live settings. */
    public static void reload() throws IOException {
        get();
        var angel = read(FILE, RESOURCE, 2);
        var condenser = read(FILE_ZERO, RESOURCE_ZERO, 0);
        current = Map.of(2, angel, 0, condenser);
    }
    private static StarDragonBossSettings read(Path file, String resource, int sequence) throws IOException {
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            try (InputStream in = StarDragonBossSettings.class.getResourceAsStream(resource)) {
                Files.copy(Objects.requireNonNull(in), file);
            }
        }
        try (Reader r = Files.newBufferedReader(file)) {
            var loaded = parse(JsonParser.parseReader(r).getAsJsonObject());
            validate(loaded, sequence);
            return loaded;
        }
    }
    static void validate(StarDragonBossSettings loaded, int sequence) {
        for (var entry : loaded.attacks().entrySet()) {
            validateAbility(entry.getKey(), sequence);
            for (String id : entry.getValue().followups()) validateAbility(id, sequence);
        }
    }
    private static void validateAbility(String id, int sequence) {
        var ability = net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities.byId(id);
        if (ability == null || !ability.pathway().equals("Star Dragon") || ability.sequence() < sequence)
            throw new IllegalArgumentException("Unknown or above-Sequence-" + sequence + " attack: " + id);
    }
    public Attack attack(MobAbility a) {
        return attacks.getOrDefault(a.id(), new Attack(true, a.weight(), 24, recoveryTicks,
                a.cooldown(), a.cost(), a.range(), 0, 1, "cast", List.of()));
    }
    public static StarDragonBossSettings parse(JsonObject j) {
        double hp = number(j, "health", 600, 1, 1024);
        double damage = number(j, "melee_damage", 14, 0, 2048);
        double abilityDamage = number(j, "ability_damage_multiplier", 1, 0, 100);
        double speed = number(j, "movement_speed", .25, 0, 2);
        double range = number(j, "follow_range", 48, 1, 128);
        int think = integer(j, "think_ticks", 10, 1, 1200);
        int recovery = integer(j, "recovery_ticks", 45, 0, 12000);
        double enrage = number(j, "enraged_below", .4, 0, 1);
        double boost = number(j, "enraged_weight_multiplier", 1.5, 1, 20);
        Map<String, Attack> entries = new LinkedHashMap<>();
        if (j.has("attacks")) for (var e : j.getAsJsonObject("attacks").entrySet()) {
            if (!e.getKey().startsWith("Star Dragon/")) throw new IllegalArgumentException("Attack must use a Star Dragon ability id: " + e.getKey());
            JsonObject a = e.getValue().getAsJsonObject();
            double min = number(a, "min_health", 0, 0, 1), max = number(a, "max_health", 1, min, 1);
            String animation = a.has("animation") ? a.get("animation").getAsString() : "cast";
            if (!Set.of("cast", "ray", "surge", "ward", "transform", "bite").contains(animation))
                throw new IllegalArgumentException("Unknown attack animation: " + animation);
            List<String> followups = new ArrayList<>();
            if (a.has("followups")) for (var id : a.getAsJsonArray("followups")) followups.add(id.getAsString());
            entries.put(e.getKey(), new Attack(bool(a, "enabled", true), integer(a, "weight", 10, 0, 10000),
                    integer(a, "windup_ticks", 24, 1, 1200), integer(a, "recovery_ticks", recovery, 0, 12000),
                    integer(a, "cooldown_ticks", 200, 0, 72000), (float)number(a, "cost", 0, 0, 100000000),
                    number(a, "range", 24, 0, 128), min, max, animation, List.copyOf(followups)));
        }
        JsonObject singularity = j.has("singularity") ? j.getAsJsonObject("singularity") : new JsonObject();
        return new StarDragonBossSettings(hp, damage, abilityDamage, speed, range, think, recovery, enrage, boost,
                bool(j, "passives", true), bool(j, "radiant_flight", true), number(j, "hover_height", 4, 1, 16),
                number(j, "enraged_windup_multiplier", 1, .1, 2), number(j, "enraged_recovery_multiplier", 1, 0, 2),
                new Singularity(number(singularity, "health_cost_fraction", .06, 0, .5),
                        integer(singularity, "spent_ticks", 6, 0, 1200), (float)number(singularity, "damage_cap", 24, 0, 2048)),
                Map.copyOf(entries));
    }
    private static boolean bool(JsonObject j, String key, boolean fallback) {
        return j.has(key) ? j.get(key).getAsBoolean() : fallback;
    }
    private static double number(JsonObject j, String key, double fallback, double min, double max) {
        double v = j.has(key) ? j.get(key).getAsDouble() : fallback;
        if (!Double.isFinite(v) || v < min || v > max) throw new IllegalArgumentException(key + " must be in " + min + ".." + max);
        return v;
    }
    private static int integer(JsonObject j, String key, int fallback, int min, int max) {
        double v = number(j, key, fallback, min, max);
        if (v != Math.rint(v)) throw new IllegalArgumentException(key + " must be an integer");
        return (int)v;
    }
}
