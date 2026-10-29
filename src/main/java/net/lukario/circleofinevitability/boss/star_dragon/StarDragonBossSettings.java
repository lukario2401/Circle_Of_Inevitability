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
        boolean passives, boolean radiantFlight, double hoverHeight, Map<String, Attack> attacks) {
    public static final String RESOURCE = "/data/circle_of_inevitability/bosses/star_dragon.json";
    public static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("circle_of_inevitability/star_dragon_boss.json");
    private static volatile StarDragonBossSettings current;

    public record Attack(boolean enabled, int weight, int windupTicks, int recoveryTicks,
                         int cooldownTicks, float cost, double range, double minHealth, double maxHealth,
                         String animation) {
        public MobAbility tune(MobAbility base) {
            return base.copy().weight(weight).cooldown(cooldownTicks).cost(cost).range(range);
        }
    }
    public static StarDragonBossSettings get() {
        if (current == null) {
            try (Reader r = new InputStreamReader(Objects.requireNonNull(StarDragonBossSettings.class.getResourceAsStream(RESOURCE)), java.nio.charset.StandardCharsets.UTF_8)) {
                current = parse(JsonParser.parseReader(r).getAsJsonObject());
            } catch (IOException e) { throw new IllegalStateException("Cannot read bundled boss settings", e); }
        }
        return current;
    }
    /** Parse completely before replacing live settings. A rejected edit leaves the running boss intact. */
    public static void reload() throws IOException {
        get();
        if (!Files.exists(FILE)) {
            Files.createDirectories(FILE.getParent());
            try (InputStream in = StarDragonBossSettings.class.getResourceAsStream(RESOURCE)) {
                Files.copy(Objects.requireNonNull(in), FILE);
            }
        }
        try (Reader r = Files.newBufferedReader(FILE)) {
            StarDragonBossSettings loaded = parse(JsonParser.parseReader(r).getAsJsonObject());
            for (String id : loaded.attacks().keySet()) {
                var ability = net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities.byId(id);
                if (ability == null || ability.sequence() < 2) throw new IllegalArgumentException("Unknown or above-Sequence-2 attack: " + id);
            }
            current = loaded;
        }
    }
    public Attack attack(MobAbility a) {
        return attacks.getOrDefault(a.id(), new Attack(true, a.weight(), 24, recoveryTicks,
                a.cooldown(), a.cost(), a.range(), 0, 1, "cast"));
    }
    public static StarDragonBossSettings parse(JsonObject j) {
        double hp = number(j, "health", 600, 1, 1000000);
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
            entries.put(e.getKey(), new Attack(bool(a, "enabled", true), integer(a, "weight", 10, 0, 10000),
                    integer(a, "windup_ticks", 24, 1, 1200), integer(a, "recovery_ticks", recovery, 0, 12000),
                    integer(a, "cooldown_ticks", 200, 0, 72000), (float)number(a, "cost", 0, 0, 100000000),
                    number(a, "range", 24, 0, 128), min, max, animation));
        }
        return new StarDragonBossSettings(hp, damage, abilityDamage, speed, range, think, recovery, enrage, boost,
                bool(j, "passives", true), bool(j, "radiant_flight", true), number(j, "hover_height", 4, 1, 16), Map.copyOf(entries));
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
