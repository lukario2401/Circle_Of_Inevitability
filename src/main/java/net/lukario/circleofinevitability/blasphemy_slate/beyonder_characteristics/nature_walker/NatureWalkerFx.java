package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.sounds.ModSounds;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Presentation only: no damage, healing, movement, weather, terrain or summon AI changes.
 * Callers own durations and periodic beats. All models share a unique cast tag and expire.
 * Geometry and resource contracts: docs/NATURE_WALKER_ASSETS.md.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class NatureWalkerFx {
    private NatureWalkerFx() {}
    public static final int LIFE = 0xFFA3CA6F, EARTH = 0xFF775031, GOLD = 0xFFE3B857,
            SPIRIT = 0xFF85D3BF, MUTATION = 0xFFA77BBC, PETAL = 0xFFE18B96;

    public enum Ability {
        THORN_SEED("seed", "thornling_bloom"),
        HEARTFRUIT_SEED("seed", "heartfruit_sprout", "heartfruit_ripe"),
        TREATMENT("healing_blossom"),
        SOUL_SUTURE("soul_suture"),
        CROSS_BREED("hybrid_crown"),
        TOXIN_BLOOM("sporebed_bloom"),
        SEED_CATALYZATION("vine_burst"),
        QUICKEN_LIFE("life_ring"),
        COMMAND_FLORA("flora_sigil"),
        WEATHER_RITUAL("rain_cloud", "sun_disc", "storm_cloud"),
        REAP("harvest_scythe"),
        BLOSSOMING_LAND("heartfruit_bloom", "life_ring"),
        BARREN_LAND("wither_ring"),
        GREAT_HARVEST("harvest_scythe", "harvest_crown"),
        UNDERGROUND_TRAVEL("earth_portal", "nature_portal"),
        SWAMP("mire_disc"),
        WRATH_OF_NATURE("vine_burst", "falling_stone", "undertow", "lightning"),
        CHILD_OF_THE_OAK("bark_skin_wood", "bark_skin_stone"),
        STONE_GOLEM("stone_golem"),
        BEAR_FORM("bear_form"),
        VERDANT_BARRIER("verdant_barrier"),
        WORLD_OF_NATURE("world_of_nature_tile"),
        EARTHQUAKE("rift"),
        STONE_BIRD("stone_bird"),
        MUD_GOLEM("mud_golem"),
        STEEL_GOLEM("steel_golem"),
        LIFE_AURA("life_ring"),
        MUTATE("mutation_organs", "mutation_fungus", "mutation_wheat", "mutation_vines"),
        BROOD_FORM("brood_form", "brood_form_complete"),
        SEED_OF_REBIRTH("rebirth_seed"),
        AWAKEN("mother_tree_ripe", "awaken_sigil"),
        EVIL_SPIRITS("evil_spirit", "grave_mark"),
        DESCENT_OF_DEATH("death_canopy"),
        RETURN_TO_EARTH("root_burst"),
        MATERNAL_EMBRACE("coffin"),
        DESOLATION("wither_ring"),
        ROOT_ERUPTION("root_eruption", "warning_ring"),
        EMBRACE_OF_THE_EARTH("earth_lance"),
        BOUNTIFUL_EARTH("bountiful_earth_tile");
        private final List<String> models;
        Ability(String... models) { this.models = List.of(models); }
        public String id() { return name().toLowerCase(Locale.ROOT); }
        public String icon() { return "ability_menus/nature_walker/" + id(); }
        public List<String> models() { return models; }
    }
    public static final Set<String> ANIMATED = Set.of("stone_bird", "stone_golem", "mud_golem", "steel_golem", "bear_form", "brood_form", "brood_form_complete", "evil_spirit");
    private static final Set<String> LUMINOUS = Set.of("evil_spirit", "grave_mark", "life_ring", "warning_ring",
            "nature_portal", "awaken_sigil", "healing_blossom", "soul_suture", "rebirth_seed", "lightning", "sun_disc");
    public static final List<String> MODELS = List.of(
            "thornling_seed",
            "thornling_sprout",
            "thornling_bloom",
            "thornling_withered",
            "heartfruit_seed",
            "heartfruit_sprout",
            "heartfruit_bloom",
            "heartfruit_ripe",
            "heartfruit_withered",
            "briar_orchard_seed",
            "briar_orchard_sprout",
            "briar_orchard_bloom",
            "briar_orchard_withered",
            "sporebed_seed",
            "sporebed_sprout",
            "sporebed_bloom",
            "sporebed_withered",
            "mother_tree_seed",
            "mother_tree_sprout",
            "mother_tree_bloom",
            "mother_tree_ripe",
            "mother_tree_withered",
            "seed",
            "heartfruit",
            "vine_burst",
            "root_burst",
            "root_eruption",
            "mire_disc",
            "coffin",
            "rift",
            "grave_mark",
            "bark_skin_wood",
            "bark_skin_stone",
            "stone_bird",
            "stone_bird_0",
            "stone_bird_1",
            "stone_bird_2",
            "stone_bird_3",
            "stone_golem",
            "stone_golem_0",
            "stone_golem_1",
            "stone_golem_2",
            "stone_golem_3",
            "mud_golem",
            "mud_golem_0",
            "mud_golem_1",
            "mud_golem_2",
            "mud_golem_3",
            "steel_golem",
            "steel_golem_0",
            "steel_golem_1",
            "steel_golem_2",
            "steel_golem_3",
            "bear_form",
            "bear_form_0",
            "bear_form_1",
            "bear_form_2",
            "bear_form_3",
            "brood_form",
            "brood_form_0",
            "brood_form_1",
            "brood_form_2",
            "brood_form_3",
            "brood_form_complete",
            "brood_form_complete_0",
            "brood_form_complete_1",
            "brood_form_complete_2",
            "brood_form_complete_3",
            "evil_spirit",
            "evil_spirit_0",
            "evil_spirit_1",
            "evil_spirit_2",
            "evil_spirit_3",
            "world_of_nature_tile",
            "bountiful_earth_tile",
            "verdant_barrier",
            "life_ring",
            "wither_ring",
            "warning_ring",
            "earth_portal",
            "nature_portal",
            "awaken_sigil",
            "flora_sigil",
            "harvest_crown",
            "healing_blossom",
            "soul_suture",
            "hybrid_crown",
            "rain_cloud",
            "storm_cloud",
            "sun_disc",
            "harvest_scythe",
            "falling_stone",
            "undertow",
            "lightning",
            "mutation_organs",
            "mutation_fungus",
            "mutation_wheat",
            "mutation_vines",
            "rebirth_seed",
            "death_canopy",
            "earth_lance");
    public static final List<String> GROWTHS = List.of("thornling", "heartfruit", "briar_orchard", "sporebed", "mother_tree");
    public static final List<String> STAGES = List.of("seed", "sprout", "bloom", "ripe", "withered");
    public static final List<String> MUTATIONS = List.of("organs", "fungus", "wheat", "vines");

    /** Model space: 16 pixels/block, ground pivot. Animated props have a 360-tick lease; refresh before it expires. */
    public static ModelFx model(String name, int ticks, String tag) {
        requireTag(tag);
        if (!MODELS.contains(name)) throw new IllegalArgumentException("Unknown Nature Walker model: " + name);
        int life = Math.max(1, Math.min(ANIMATED.contains(name) ? 360 : 6000, ticks));
        ModelFx fx = ModelFx.of("nature_walker/" + name).pivot(8, 0, 8).lifetime(life)
                .fade(Math.min(5, life / 4), Math.min(10, life / 4)).tag(tag);
        if (ANIMATED.contains(name)) {
            fx = fx.frames(4).smooth();
            // At most 61 keys, below ModelFx's 64-key wire limit.
            for (int tick = 0; tick <= life; tick += 6)
                fx = fx.key(tick, ModelFx.pose().frame((tick / 6) % 4));
        }
        return LUMINOUS.contains(name) ? fx.glow().unshaded() : fx;
    }

    private static void requireTag(String tag) {
        if (tag == null || tag.isBlank()) throw new IllegalArgumentException("A nonempty cast tag is required");
    }

    public static ParticleFx particle(String name, int color) {
        return ParticleFx.of("fx/nature_walker/" + name).color(color).endColor(color & 0x00FFFFFF)
                .size(.14f).endSize(.035f).lifetime(24, 6).friction(.94f).randomRotation();
    }

    public static void sound(ServerLevel sl, Vec3 at, String name) {
        var event = ModSounds.NATURE_WALKER.get(name);
        if (event == null) throw new IllegalArgumentException("Unknown Nature Walker sound: " + name);
        Spells.sound(sl, at, event.get(), .7f, 1f);
    }

    private static void at(ServerLevel sl, Vec3 at, String name, int ticks, String tag, float scale) {
        ParticleShapes.model(sl, model(name, ticks, tag).scale(scale), at, 0, 0, 0);
    }

    private static void burst(ServerLevel sl, Vec3 at, String sprite, int color, int count) {
        ParticleShapes.burst(sl, particle(sprite, color), at, count, .025, .16);
    }

    /** Growth stage replacement; use one tag per growth. Invalid kind/stage pairs are rejected. */
    public static void growth(ServerLevel sl, Vec3 at, String kind, String stage, int ticks, String tag) {
        if (!GROWTHS.contains(kind) || !STAGES.contains(stage) || !MODELS.contains(kind + "_" + stage))
            throw new IllegalArgumentException("Unknown growth stage: " + kind + "/" + stage);
        requireTag(tag);
        ParticleShapes.clearModels(sl, at, tag);
        at(sl, at, kind + "_" + stage, ticks, tag, kind.equals("mother_tree") ? 3f : 1f);
        burst(sl, at.add(0, .3, 0), stage.equals("withered") ? "soil_crumb" : "pollen",
                stage.equals("withered") ? EARTH : LIFE, 12);
    }

    /** Visual skin for a construct/form. The caller owns the real entity, hitbox and movement. */
    public static void form(ServerLevel sl, LivingEntity entity, String name, int ticks, String tag, float scale) {
        requireTag(tag);
        ParticleShapes.clearModels(sl, entity, tag);
        ParticleShapes.modelOnTurning(sl, model(name, ticks, tag).scale(scale), entity, Vec3.ZERO, 0, 0, 0);
    }

    public static void mutation(ServerLevel sl, LivingEntity entity, String kind, int level, int ticks, String tag) {
        if (!MUTATIONS.contains(kind)) throw new IllegalArgumentException("Unknown mutation: " + kind);
        int stacks = Math.max(1, Math.min(3, level));
        requireTag(tag);
        ParticleShapes.clearModels(sl, entity, tag);
        for (int i = 0; i < stacks; i++) {
            double a = i * Math.PI * 2 / stacks;
            ParticleShapes.modelOnTurning(sl, model("mutation_" + kind, ticks, tag).scale(.35f), entity,
                    new Vec3(Math.cos(a) * .35, entity.getBbHeight() * .45, Math.sin(a) * .35), i * 120, 0, 0);
        }
    }

    /** Reusable bounded realm shell. A fixed 32 tiles avoids cost increasing with the gameplay radius. */
    public static void realm(ServerLevel sl, Vec3 center, float radius, boolean bountiful, int ticks, String tag) {
        requireTag(tag);
        float r = Math.max(1, Math.min(40, radius));
        String tile = bountiful ? "bountiful_earth_tile" : "world_of_nature_tile";
        ParticleShapes.clearModels(sl, center, tag);
        for (int row = 0; row < 4; row++) {
            double elevation = (row + .5) * Math.PI / 4 - Math.PI / 2;
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                Vec3 offset = new Vec3(Math.cos(a) * Math.cos(elevation), Math.sin(elevation),
                        Math.sin(a) * Math.cos(elevation)).scale(r);
                ParticleShapes.modelFacing(sl, model(tile, ticks, tag).pivot(8, 8, 8).scale(r * .55f),
                        center.add(offset), offset.normalize());
            }
        }
        ParticleShapes.sphere(sl, particle("pollen", bountiful ? GOLD : LIFE), center, r, 64);
    }

    /** Cast presentation at a supplied target point. Targeting and gameplay remain with the caller. */
    public static void cast(ServerLevel sl, LivingEntity caster, Vec3 target, Ability ability, int ticks, String tag) {
        requireTag(tag);
        int life = Math.max(1, Math.min(6000, ticks));
        sound(sl, target, ability.id());
        switch (ability) {
            case THORN_SEED -> growth(sl, target, "thornling", "sprout", life, tag);
            case HEARTFRUIT_SEED -> growth(sl, target, "heartfruit", "sprout", life, tag);
            case TREATMENT -> {
                at(sl, target, "healing_blossom", life, tag, 1.2f);
                burst(sl, target.add(0, .8, 0), "healing_spark", LIFE, 20);
            }
            case SOUL_SUTURE -> {
                at(sl, target, "soul_suture", life, tag, 1.3f);
                ParticleShapes.line(sl, particle("soul_thread", SPIRIT), caster.getEyePosition(), target.add(0, 1, 0), 4);
            }
            case CROSS_BREED -> {
                at(sl, target, "hybrid_crown", life, tag, 1.2f);
                burst(sl, target.add(0, 1, 0), "mutation_cell", MUTATION, 24);
            }
            case TOXIN_BLOOM -> {
                at(sl, target, "sporebed_bloom", life, tag, 2);
                ParticleShapes.line(sl, particle("spore", MUTATION).size(.25f), caster.getEyePosition(), target, 6);
            }
            case SEED_CATALYZATION, WRATH_OF_NATURE -> {
                at(sl, target, "vine_burst", life, tag, 2);
                burst(sl, target, "thorn", LIFE, 24);
            }
            case QUICKEN_LIFE, LIFE_AURA -> {
                at(sl, target, "life_ring", life, tag, 5);
                ParticleShapes.ring(sl, particle("pollen", GOLD), target.add(0, .2, 0), 2, 32, .04);
            }
            case COMMAND_FLORA -> {
                at(sl, target, "flora_sigil", life, tag, 3);
                ParticleShapes.line(sl, particle("leaf", LIFE), caster.getEyePosition(), target.add(0, .5, 0), 4);
            }
            case WEATHER_RITUAL -> weather(sl, target, "rain", life, tag);
            case REAP, GREAT_HARVEST -> {
                ParticleShapes.modelOnTurning(sl, model("harvest_scythe", 20, tag)
                        .key(0, ModelFx.pose().yaw(-70)).key(20, ModelFx.pose().yaw(80)), caster, Vec3.ZERO, 0, 0, 0);
                ParticleShapes.ring(sl, particle("wheat_grain", GOLD), target.add(0, .5, 0), 2, 30, .08);
                if (ability == Ability.GREAT_HARVEST) at(sl, target, "harvest_crown", life, tag, 4);
            }
            case BLOSSOMING_LAND -> {
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3;
                    at(sl, target.add(Math.cos(a) * 2, 0, Math.sin(a) * 2), "heartfruit_bloom", life, tag, 1);
                }
                at(sl, target, "life_ring", life, tag, 6);
            }
            case BARREN_LAND, DESOLATION -> {
                at(sl, target, "wither_ring", life, tag, 6);
                burst(sl, target, "soil_crumb", EARTH, 36);
            }
            case UNDERGROUND_TRAVEL -> {
                at(sl, target, "earth_portal", life, tag, 3);
                burst(sl, target, "soil_crumb", EARTH, 24);
            }
            case SWAMP -> at(sl, target.add(0, .03, 0), "mire_disc", life, tag, 6);
            case CHILD_OF_THE_OAK -> form(sl, caster, "bark_skin_wood", life, tag, 1);
            case BEAR_FORM -> form(sl, caster, "bear_form", life, tag, 1.8f);
            case BROOD_FORM -> form(sl, caster, "brood_form", life, tag, 1.6f);
            case STONE_GOLEM, MUD_GOLEM, STEEL_GOLEM, STONE_BIRD, EVIL_SPIRITS ->
                    at(sl, target, ability == Ability.EVIL_SPIRITS ? "evil_spirit" : ability.id(), life, tag, 1.5f);
            case VERDANT_BARRIER -> {
                for (int i = -2; i <= 2; i++) at(sl, target.add(i * 2, 0, 0), "verdant_barrier", life, tag, 2);
            }
            case WORLD_OF_NATURE, BOUNTIFUL_EARTH -> realm(sl, target.add(0, 2, 0), 4,
                    ability == Ability.BOUNTIFUL_EARTH, life, tag);
            case EARTHQUAKE -> {
                Vec3 direction = new Vec3(caster.getLookAngle().x, 0, caster.getLookAngle().z).normalize();
                ParticleShapes.modelFacing(sl, model("rift", life, tag).scale(2, 1, 4), target, direction);
                burst(sl, target, "soil_crumb", EARTH, 40);
            }
            case MUTATE -> {
                at(sl, target.add(0, .5, 0), "mutation_organs", life, tag, 1);
                burst(sl, target.add(0, 1, 0), "mutation_cell", MUTATION, 24);
            }
            case SEED_OF_REBIRTH -> {
                at(sl, target, "rebirth_seed", life, tag, 2);
                ParticleShapes.ring(sl, particle("petal", PETAL), target.add(0, .8, 0), 1, 24, .03);
            }
            case AWAKEN -> growth(sl, target, "mother_tree", "ripe", life, tag);
            case DESCENT_OF_DEATH -> {
                at(sl, target, "death_canopy", life, tag, 3);
                burst(sl, target.add(0, 2, 0), "grave_mote", SPIRIT, 32);
            }
            case RETURN_TO_EARTH -> {
                at(sl, target, "root_burst", life, tag, 2);
                ParticleShapes.ring(sl, particle("brown_ring", EARTH), target.add(0, .1, 0), 2.5, 16, .04);
            }
            case MATERNAL_EMBRACE -> at(sl, target, "coffin", life, tag, 1.5f);
            case ROOT_ERUPTION -> {
                at(sl, target, "warning_ring", Math.min(life, 40), tag, 5);
                // One timed model needs no server callback; end() can cancel the entire effect.
                ParticleShapes.model(sl, model("root_eruption", life, tag).scale(2)
                        .key(0, ModelFx.pose().scale(0)).key(39, ModelFx.pose().scale(0))
                        .key(48, ModelFx.pose().scale(1)), target, 0, 0, 0);
            }
            case EMBRACE_OF_THE_EARTH -> ParticleShapes.modelAlong(sl,
                    model("earth_lance", life, tag).scale(1.5f), caster.getEyePosition(),
                    target.subtract(caster.getEyePosition()));
        }
    }

    /** Variants share the same ability entry; the caller selects the appropriate weather/Sequence. */
    public static void weather(ServerLevel sl, Vec3 at, String variant, int ticks, String tag) {
        String name = switch (variant) {
            case "rain" -> "rain_cloud";
            case "clear" -> "sun_disc";
            case "storm" -> "storm_cloud";
            default -> throw new IllegalArgumentException("Unknown weather variant: " + variant);
        };
        at(sl, at.add(0, 3, 0), name, ticks, tag, 3);
        if (!variant.equals("clear")) burst(sl, at.add(0, 2, 0), "rain_drop", SPIRIT, 28);
    }

    public static void melee(ServerLevel sl, LivingEntity caster, Vec3 hit, boolean impact) {
        if (impact) burst(sl, hit, "thorn", LIFE, 16);
        else ParticleShapes.line(sl, particle("petal", PETAL), caster.getEyePosition(), hit, 8);
        sound(sl, hit, impact ? "melee_impact" : "melee_swing");
    }

    public static void end(ServerLevel sl, LivingEntity entity, Vec3 center, String tag) {
        if (tag == null || tag.isBlank()) return;
        ParticleShapes.clearModels(sl, entity, tag);
        ParticleShapes.clearModels(sl, center, tag);
    }

    private static List<String> abilityNames() { return Arrays.stream(Ability.values()).map(Ability::id).toList(); }
    private static Vec3 previewPoint(ServerPlayer p) {
        Vec3 v = p.getLookAngle();
        return p.position().add(new Vec3(v.x, 0, v.z).normalize().scale(4));
    }
    private static String previewTag(ServerPlayer p) { return "nature_preview_" + p.getUUID(); }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("naturefx")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "/naturefx <ability> | model <id> | sound <id> | growth <kind> <stage> | particles | stop"), false);
                    return 1;
                })
                .then(Commands.literal("stop").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    end(p.level(), p, previewPoint(p), previewTag(p)); return 1;
                }))
                .then(Commands.literal("model").then(Commands.argument("model", StringArgumentType.word())
                        .suggests((ctx,b) -> SharedSuggestionProvider.suggest(MODELS,b)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx,"model");
                            if (!MODELS.contains(name)) { ctx.getSource().sendFailure(Component.literal("Unknown model: " + name)); return 0; }
                            end(p.level(),p,previewPoint(p),previewTag(p));
                            at(p.level(),previewPoint(p),name,160,previewTag(p),1.5f);return 1;
                        })))
                .then(Commands.literal("sound").then(Commands.argument("sound", StringArgumentType.word())
                        .suggests((ctx,b) -> SharedSuggestionProvider.suggest(ModSounds.NATURE_WALKER.keySet(),b)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();String name=StringArgumentType.getString(ctx,"sound");
                            if (!ModSounds.NATURE_WALKER.containsKey(name)) { ctx.getSource().sendFailure(Component.literal("Unknown sound: " + name));return 0; }
                            sound(p.level(),p.position(),name);return 1;
                        })))
                .then(Commands.literal("growth").then(Commands.argument("kind", StringArgumentType.word())
                        .suggests((ctx,b) -> SharedSuggestionProvider.suggest(GROWTHS,b))
                        .then(Commands.argument("stage", StringArgumentType.word())
                                .suggests((ctx,b) -> SharedSuggestionProvider.suggest(STAGES.stream()
                                        .filter(s -> MODELS.contains(StringArgumentType.getString(ctx,"kind")+"_"+s)),b))
                                .executes(ctx -> {
                                    ServerPlayer p=ctx.getSource().getPlayerOrException();
                                    String kind=StringArgumentType.getString(ctx,"kind"), stage=StringArgumentType.getString(ctx,"stage");
                                    if (!GROWTHS.contains(kind) || !STAGES.contains(stage) || !MODELS.contains(kind+"_"+stage)) {
                                        ctx.getSource().sendFailure(Component.literal("Unknown growth stage"));return 0;
                                    }
                                    end(p.level(),p,previewPoint(p),previewTag(p));
                                    growth(p.level(),previewPoint(p),kind,stage,160,previewTag(p));return 1;
                                }))))
                .then(Commands.literal("particles").executes(ctx -> {
                    ServerPlayer p=ctx.getSource().getPlayerOrException();
                    String[] sprites={"petal","spore","pollen","soil_crumb","green_ring","brown_ring","leaf","thorn",
                            "root_splinter","soul_thread","healing_spark","rain_drop","wheat_grain","grave_mote","mutation_cell"};
                    for(int i=0;i<sprites.length;i++) burst(p.level(),previewPoint(p).add((i%5-2)*.7,1+i/5*.7,0),
                            sprites[i],i%2==0?LIFE:GOLD,6);
                    return 1;
                }))
                .then(Commands.argument("ability",StringArgumentType.word())
                        .suggests((ctx,b) -> SharedSuggestionProvider.suggest(abilityNames(),b)).executes(ctx -> {
                            String name=StringArgumentType.getString(ctx,"ability");
                            if (!abilityNames().contains(name)) { ctx.getSource().sendFailure(Component.literal("Unknown ability: " + name));return 0; }
                            ServerPlayer p=ctx.getSource().getPlayerOrException();
                            end(p.level(),p,previewPoint(p),previewTag(p));
                            cast(p.level(),p,previewPoint(p),Ability.valueOf(name.toUpperCase(Locale.ROOT)),160,previewTag(p));
                            return 1;
                        })));
    }
}
