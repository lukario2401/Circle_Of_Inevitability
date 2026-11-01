package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.lukario.circleofinevitability.combat.HudCounter;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.lukario.circleofinevitability.sounds.ModSounds;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

/** Cosmetic asset adapter. No damage, statuses, weather changes, real blocks or movement.
 * /thunderfx <ability> previews its component assets, cue and icon, at small gallery scale.
 * /thunderfx all schedules every preview; /thunderfx bolt shows the branching bolt helper.
 * Asset dimensions, animation and production placement are documented in THUNDER_GOD_ASSETS.md.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class ThunderGodFx {
    public static final int PATHWAY_COLOR = 0xFF10294A;
    public static final int SILVER = 0xFFD6ECFF;
    public static final int WATER = 0xFF1F6F8B;
    public static final int RAGE = 0xFFC0392B;
    private ThunderGodFx() {}

    /** Models are the components of a cast, not a gameplay implementation of it. */
    public static final Map<String, List<String>> ABILITY_MODELS = Map.ofEntries(
            Map.entry("harpoon", List.of("harpoon", "rope_segment")),
            Map.entry("illusory_scales", List.of("scale_armor")),
            Map.entry("raging_blow", List.of("sound_wave")),
            Map.entry("wrath", List.of("tyrant_crown")),
            Map.entry("water_veil", List.of("water_film")),
            Map.entry("sweeping_wave", List.of("wave_crest_0", "wave_foam")),
            Map.entry("tideheal", List.of("droplet")),
            Map.entry("water_curtain", List.of("water_curtain")),
            Map.entry("water_sphere", List.of("water_sphere")),
            Map.entry("corrosive_rain", List.of("storm_cloud", "rain_sheet")),
            Map.entry("sea_master", List.of("whirlpool")),
            Map.entry("maelstrom", List.of("whirlpool")),
            Map.entry("destructive_droplet", List.of("droplet")),
            Map.entry("windblade", List.of("windblade")),
            Map.entry("gale_burst", List.of("sound_wave")),
            Map.entry("gale_ride", List.of("windblade")),
            Map.entry("wind_binding", List.of("sound_wave")),
            Map.entry("windblade_legion", List.of("windblade")),
            Map.entry("sonic_boom", List.of("sound_wave")),
            Map.entry("cloud_sword", List.of("cloud_sword")),
            Map.entry("incarnation_of_light", List.of("ball_lightning")),
            Map.entry("starfall", List.of("ball_lightning", "warning_ring")),
            Map.entry("lightning_strike", List.of("bolt")),
            Map.entry("lightning_bolt_arrow", List.of("lightning_arrow")),
            Map.entry("chain_lightning", List.of("bolt")),
            Map.entry("lightning_storm", List.of("ball_lightning", "bolt")),
            Map.entry("magnetic_pole_pull", List.of("whirlpool")),
            Map.entry("electromagnetic_disruption", List.of()),
            Map.entry("heavenly_punishment", List.of("heavenly_pillar", "bolt")),
            Map.entry("ocean_of_lightning", List.of("whirlpool", "bolt")),
            Map.entry("lightning_forest", List.of("bolt")),
            Map.entry("siren_song", List.of("sound_wave")),
            Map.entry("discord", List.of("sound_wave")),
            Map.entry("thunderclap", List.of("sound_wave")),
            Map.entry("roar", List.of("sound_wave")),
            Map.entry("death_dirge", List.of("sound_wave")),
            Map.entry("sound_of_horror", List.of("sound_wave")),
            Map.entry("requiem", List.of("sound_wave")),
            Map.entry("roar_of_the_thunder_god", List.of("sound_wave", "bolt")),
            Map.entry("tyrants_presence", List.of("tyrant_crown")),
            Map.entry("hurricane", List.of("whirlpool", "windblade")),
            Map.entry("tsunami", List.of("wave_crest_0", "wave_foam")),
            Map.entry("wrath_of_the_sea", List.of("wave_crest_0")),
            Map.entry("storm_incarnation", List.of("storm_cloud", "cloud_face", "rain_sheet", "bolt")),
            Map.entry("storm_hell", List.of("storm_cloud", "rain_sheet", "bolt")),
            Map.entry("chasm_of_storms", List.of("kingdom_chasm")),
            Map.entry("dreadful_heaven", List.of("kingdom_dreadful")),
            Map.entry("banish_into_the_storm", List.of("whirlpool")),
            Map.entry("source_of_wrath", List.of("tyrant_crown")),
            Map.entry("water_wall", List.of("water_curtain")),
            Map.entry("torrential_downpour", List.of("storm_cloud", "rain_sheet")),
            Map.entry("thunder_strike", List.of("bolt")),
            Map.entry("dance_of_thunder", List.of()));

    /** A safe reusable model preset, with the authored pivot and translucent render settings. */
    public static ModelFx model(String name, int ticks) {
        ModelFx fx = ModelFx.of("thunder_god/" + name).lifetime(ticks).seeThrough();
        if (List.of("water_sphere", "ball_lightning", "kingdom_chasm", "kingdom_dreadful").contains(name)) {
            fx = fx.pivot(8, 12, 8);
        } else if (List.of("water_film", "rope_segment", "droplet", "windblade", "sound_wave", "cloud_face", "rain_sheet").contains(name)) {
            fx = fx.pivot(8, 8, 8);
        } else {
            fx = fx.pivot(8, 0, 8);
        }
        if (List.of("bolt", "ball_lightning", "lightning_arrow", "wave_foam", "warning_ring",
                "sound_wave", "windblade", "cloud_face", "kingdom_chasm", "kingdom_dreadful").contains(name)) {
            fx = fx.glow().unshaded();
        }
        return fx;
    }

    /** Draw a branching silver bolt between arbitrary points. Never spawns a LightningBolt entity. */
    public static void bolt(ServerLevel level, Vec3 from, Vec3 to, float thickness, int life) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.01 || length > 256) return;
        Vec3 direction = delta.normalize();
        Vec3 side = direction.cross(Math.abs(direction.y) > .9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 other = direction.cross(side).normalize();
        ParticleFx spark = ParticleFx.of("fx/thunder_god/spark").color(SILVER).endColor(0x00D6ECFF)
                .size(Math.max(.02f, Math.min(.4f, thickness))).endSize(.01f)
                .lifetime(Math.max(1, Math.min(40, life)), 0).glow();
        int steps = Math.min(24, Math.max(4, (int) Math.ceil(length * 1.5)));
        Vec3 last = from;
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double jitter = i == steps ? 0 : Math.min(1.2, length * .06);
            Vec3 point = from.add(delta.scale(t))
                    .add(side.scale((level.getRandom().nextDouble() - .5) * jitter))
                    .add(other.scale((level.getRandom().nextDouble() - .5) * jitter));
            ParticleShapes.line(level, spark, last, point, 4);
            if (i % 4 == 0 && i < steps) {
                Vec3 branch = point.add(side.scale((i % 8 == 0 ? 1 : -1) * Math.min(2, length * .15)))
                        .add(direction.scale(Math.min(1.2, length * .12)));
                ParticleShapes.line(level, spark.size(thickness * .6f), point, branch, 4);
            }
            last = point;
        }
        cue(level, to, "bolt_crack");
    }

    /** Four matching geometry poses, rolling forward; adds the separate foam crest. */
    public static void wave(ServerLevel level, Vec3 ground, float yaw, float scale, float distance, int ticks) {
        ModelFx water = model("wave_crest", ticks).frames(4).smooth().scale(scale)
                .key(ticks, ModelFx.pose().forward(distance).frame(3).alpha(0));
        ParticleShapes.model(level, water, ground, yaw, 0, 0);
        ParticleShapes.model(level, model("wave_foam", ticks).scale(scale)
                .key(ticks, ModelFx.pose().forward(distance).alpha(0)), ground, yaw, 0, 0);
    }

    /** Place a local cloud and its optional angry face; rain sheets use their own authored material. */
    public static void cloud(ServerLevel level, Vec3 center, float yaw, float scale, int ticks, boolean face) {
        ParticleShapes.model(level, model("storm_cloud", ticks).scale(scale), center, yaw, 0, 0);
        if (face) {
            Vec3 front = new Vec3(0, 0, -.42 * scale).yRot((float) Math.toRadians(-yaw));
            ParticleShapes.model(level, model("cloud_face", ticks).scale(scale * .7f), center.add(front).add(0, .35 * scale, 0), yaw, 0, 0);
        }
        ParticleShapes.model(level, model("rain_sheet", ticks).scale(scale).color(0x80FFFFFF), center.add(0, -scale, 0), yaw, 0, 0);
    }

    /** Visual shell only; an ability's Dome is responsible for the boundary rules. */
    public static void kingdom(ServerLevel level, Vec3 center, float radius, int ticks, boolean dreadful) {
        ParticleShapes.model(level, model(dreadful ? "kingdom_dreadful" : "kingdom_chasm", ticks)
                .scale(radius / .75f), center, 0, 0, 0);
    }

    public static void cue(ServerLevel level, Vec3 at, String name) {
        var sound = ModSounds.THUNDER_GOD.get(name);
        if (sound != null) Spells.sound(level, at, sound.get(), .8f, 1f);
    }

    /** Displays only the supplied count; Rage gain, spend and decay belong to gameplay. */
    public static void rage(Player player, int count) {
        int value = Math.max(0, Math.min(10, count));
        HudCounter.show(player, "thunder_god_rage", "thunder_god/" + (value == 10 ? "rage_full" : "rage"), "Rage", value, 10, 0xC0392B);
    }

    public static void rageEnd(Player player) {
        HudCounter.hide(player, "thunder_god_rage");
    }

    private static void preview(ServerLevel level, ServerPlayer player, String name) {
        Vec3 spot = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(5));
        if (name.equals("bolt")) {
            bolt(level, spot.add(0, 8, 0), spot, .12f, 10);
            return;
        }
        List<String> parts = ABILITY_MODELS.get(name);
        // Disruption and Dance have a menu/HUD icon, but no world visuals or sound.
        if (parts == null) return;
        ScreenImages.show(player, "thunderfx_preview", "thunder_god/" + name, 32, 35);
        cue(level, spot, name);
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            Vec3 at = spot.add((i - (parts.size() - 1) * .5) * 2, 0, 0);
            if (part.equals("wave_crest_0")) {
                wave(level, at, player.getYRot(), 1f, 2f, 35);
            } else if (part.equals("wave_foam")) {
                // The wave helper already includes this overlay.
                continue;
            } else if (part.equals("storm_cloud")) {
                cloud(level, at.add(0, 2, 0), player.getYRot(), 1.4f, 35, name.equals("storm_incarnation"));
            } else if (part.equals("cloud_face") || part.equals("rain_sheet")) {
                continue;
            } else {
                ParticleShapes.model(level, model(part, 35), at.add(0, .8, 0), player.getYRot(), 0, 0);
            }
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("thunderfx")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("all").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ServerLevel level = ctx.getSource().getLevel();
                    List<String> names = ABILITY_MODELS.keySet().stream().sorted().toList();
                    for (int i = 0; i < names.size(); i++) {
                        String name = names.get(i);
                        Later.run(level, i * 40, () -> {
                            if (player.isRemoved() || player.level() != level) return;
                            player.sendSystemMessage(Component.literal("Thunder God assets: " + name));
                            preview(level, player, name);
                        });
                    }
                    return 1;
                }))
                .then(Commands.argument("ability", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                java.util.stream.Stream.concat(ABILITY_MODELS.keySet().stream(), java.util.stream.Stream.of("bolt")), builder))
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "ability");
                            if (!name.equals("bolt") && !ABILITY_MODELS.containsKey(name)) {
                                ctx.getSource().sendFailure(Component.literal("Unknown Thunder God asset: " + name));
                                return 0;
                            }
                            preview(ctx.getSource().getLevel(), ctx.getSource().getPlayerOrException(), name);
                            return 1;
                        })));
    }
}
