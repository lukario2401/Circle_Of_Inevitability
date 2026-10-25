package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.lukario.circleofinevitability.combat.HudTimer;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ModelFx.Ease;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.particles.fx.SlashFx;
import net.lukario.circleofinevitability.screen.ScreenAnchor;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.lukario.circleofinevitability.sounds.ModSounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * How the Star Dragon (Condenser) pathway looks and sounds: every key, both casts, every wheel entry, the growth at
 * higher Sequences (the self-lightening and the slam of Gravity Abnormality, the true and the frozen Cosmic Void, the
 * Heavy Body, the three asteroids, the electromagnetic storm, the imploding clusters), the marks it leaves on others,
 * the statuses, the three forms and the passives that show. Visual only: nothing here hurts, holds, pulls, hides or
 * moves anyone. The ability code does that, at the ticks named by the constants below (..._LANDS, ..._READY,
 * ..._RISEN...), with Later.run. See docs/STAR_DRAGON.md.
 *
 * ---------- See them in game ----------
 *   /stardragonfx all              every display one after another (/stardragonfx all 3 = three times over)
 *   /stardragonfx planetfall       just that one (press TAB after /stardragonfx for the names)
 * Displays that need a target use the nearest mob within 12 blocks, or you when there is none.
 *
 * ---------- Use them in an ability ----------
 *   Shared  heavy(sl, target, ticks, tag) / heavyEnd: Heaviness on anyone (Tidal Surge, Scaled Form, Heavybringing...);
 *           rayWarning(sl, from, to, RayKind.X) (fires RAY_WARNING ticks later), rayFire(sl, from, to, RayKind.X, width)
 *   Key 1   starChart(sl, player, target) (the chart settles at CHART_LANDS), charted(viewer, target) every 10 ticks
 *               while it lasts (only the Aficionado sees it), calculatedHit(sl, target) when the bonus is spent,
 *               chartedEnd(viewer, target)
 *           sneak: fateGlimpse(sl, player, ticks, tag), glimpseDodge(sl, player, from, to) per dodge, fateGlimpseEnd
 *   Key 2   the Star Sacrifice wheel's pictures: WHEEL_GRAVITY_ABNORMALITY ... WHEEL_SUNLIGHT_RAY
 *           gravityAbnormality(sl, center, radius, ticks, lifted, tag) (risen at GRAVITY_RISEN), gravityTick(sl,
 *               center, radius, lifted) every second, pressed(sl, enemy), lifted(sl, enemy, height, ticks, tag),
 *               gravityEnd(sl, center, radius, lifted, tag); Tidal Scholar: lightened(sl, player, ticks, tag),
 *               airWave(sl, player, radius), lightenedEnd; Heavybringer: slammed(sl, enemy, ground),
 *               collapse(sl, enemy)
 *           magnetPull(sl, a, b, ticks) as two bodies are drawn together, magnetCollide(sl, at) as they meet,
 *               hammerBlow(sl, player, target) when the caster is drawn in
 *           cosmicVoid(sl, target, radius, ticks, VoidDepth.X, tag) (closed at VOID_CLOSED), voidManifest(sl, target)
 *               (Tidal Scholar: a ray manifests inside), voidFreeze(sl, target, radius) (Star Dragon, every 2 s),
 *               voidEnd(sl, target, tag)
 *           Weakening Ray: rayWarning + rayFire with RayKind.WEAKENING, then weakened(sl, target, stacks, ticks, tag),
 *               weakenedEnd; melting(sl, target, ticks, tag) / meltingEnd (the Tidal Scholar's melting, three Series stacks)
 *           sunlightCharge(sl, player) (fires at SUNLIGHT_FIRES), rayFire with RayKind.SUNLIGHT, sunburnt(sl, target)
 *   Key 3   voidPassage(sl, who, from, to) (they step out at PASSAGE_ARRIVES), voidForcedOut(sl, hidden),
 *               unnoticed(sl, player, ticks, tag) / unnoticedEnd (their faint shimmer while no mob can find them)
 *           sneak: piercingRay(sl, player, to) (both eyes), dimmed(sl, target, ticks, tag), dimmedEnd
 *   Key 4   tidalSurge(sl, player) (the crest rolls SURGE_REACH blocks in SURGE_TICKS), surgeHit(sl, target),
 *               surgeParry(sl, player, attacker)
 *           sneak: scaledForm(sl, player, ticks, heavyBody, tag) (scales closed at SCALES_ON), scaledBlow(sl,
 *               target), heavyBodyTick(sl, player) every second for the Heavy Body, scaledFormEnd(sl, player, tag)
 *   Key 5   the Heavybringer wheel's pictures: WHEEL_HEAVYBRINGING ... WHEEL_REMOTE_SIGHT
 *           heavybringing(sl, player, radius, ticks, tag), heavybringingTick(sl, player, radius) every 10 ticks,
 *               bent(sl, from, to) for something drawn off its course, heavybringingEnd
 *           fateConvergence(sl, player, radius, ticks, tag), misfortune(sl, target, ticks, tag), stumble(sl, target),
 *               misfire(sl, at), narrowed(sl, target, ticks, tag), luck(sl, ally), fateConvergenceEnd
 *           heavySphere(sl, center, radius, ticks, tag) (closed at SPHERE_CLOSED), sphereBlock(sl, at),
 *               yanked(sl, entity), heavySphereEnd(sl, center, tag)
 *           seriesOfRays(sl, player) as the light gathers, then rayFire(..., RayKind.WEAKENING, ...) per ray (ray i
 *               fires at SERIES_FIRES + i * SERIES_STEP)
 *           remoteSight(sl, player, ticks, tag), sighted(viewer, entity) and passage(viewer, from, to) every second
 *               (only the Heavybringer sees them), remoteSightEnd
 *   Key 6   starGrazing(sl, ground, radius) (the rock lands at GRAZE_LANDS), grazedStar(sl, above, ticks, tag),
 *               grazedStarFeeds(sl, star, ally) every few seconds, grazedStarEnd(sl, above, tag)
 *           sneak: fissionPulse(sl, player, target) (it splits at FISSION_SPLITS), fissionBurst(sl, at, radius),
 *               irradiated(sl, target, ticks, tag) / irradiatedEnd, radiationZone(sl, ground, radius, ticks, tag) /
 *               radiationZoneEnd; Star Dragon: emStorm(sl, ground, radius, ticks, tag), emArc(sl, from, to),
 *               stormStop(sl, at), emStormEnd
 *   Key 7   the Cosmos wheel's pictures: WHEEL_FUSION_CLUSTER ... WHEEL_QUANTUM_UNCERTAINTY
 *           fusionCluster(sl, player, count, ticks, tag) (ready at FUSION_READY), then fusionOrbit(sl, player, count,
 *               tag, age) EVERY tick while they circle, fusionLaunch(sl, player, index, count, tag, age) returns where
 *               it leaves,
 *               FUSION_FLYING for the Shot, fusionBurst(sl, at, radius, implode)
 *           radiantAngel(sl, player, ticks, tag) (unfolded at ANGEL_READY), radiance(sl, player, radius) every
 *               10 ticks, radiantBurn(sl, target), radiantAngelEnd(sl, player, tag)
 *           timeDilation(sl, center, radius, ticks, tag) (risen at DILATION_RISEN), dilated(sl, target, ticks, tag),
 *               timeHang(sl, at) for a projectile hanging, timeDilationEnd(sl, center, radius, tag)
 *           planetfall(sl, ground, radius) (it strikes at PLANET_LANDS), lightCracks(sl, ground, radius, ticks,
 *               tag), crackBurn(sl, target), lightCracksEnd(sl, ground, tag)
 *           starDragonForm(sl, player, ticks, tag) (whole at DRAGON_READY), dragonBeat(sl, player, ticksLeft, tag)
 *               while flying, dragonAsteroid(sl, player, ticks, tag, age) and dragonAsteroidOrbit(sl, player, tag,
 *               age) every tick, dragonShot(sl, player, tag, age) returns where it leaves (ASTEROID_FLYING for the
 *               Shot), dragonShotHit(sl, at),
 *               dragonAsteroidBack(sl, player, ticksLeft, tag, age), starDragonFormEnd(sl, player, tag)
 *           neutronStar(sl, player, radius, ticks, tag) (dense at NEUTRON_READY), neutronCrush(sl, target),
 *               neutronTick(sl, player, radius) every 10 ticks, neutronStarEnd(sl, player, tag)
 *           singularityGather(sl, player) (the hole opens at SINGULARITY_GATHER), singularity(sl, center, radius,
 *               ticks, tag), singularityTick(sl, center, radius) every 5 ticks, swallowed(sl, from, center),
 *               singularityEnd(sl, center, tag), spent(sl, player)
 *           quantumUncertainty(sl, player, radius, ticks, tag), uncertain(sl, target), quantumSlip(sl, from, to),
 *               quantumEnd(sl, player, tag)
 *   Passives: stargazing(sl, player), intuition(viewer, from), embraceLanding(sl, player),
 *             senseOfPassages(viewer, hidden), starlightScales(sl, player), denseBody(sl, player),
 *             fissionTouch(sl, from, to), radianceAura(sl, player, radius), gravityAuthority(sl, at),
 *             condenserStep(sl, player)
 *   Forms that end: formWeakness(sl, player, ticks, tag) for the weakness after a Mythical Creature Form.
 *   Every timed cast: status(player, Status.X, ticks) shows its picture and its clock; statusEnd(player, Status.X).
 *
 * The models are in models/model_fx/star_dragon/ (made by tools/model_gen/star_dragon/models.py and pictures.py;
 * each JSON's "credit" gives its size and pivot), the pictures in textures/gui/star_dragon/ and
 * textures/gui/ability_menus/star_dragon/ (icons.py), the particles in textures/particle/fx/ (particles.py), the
 * sounds in sounds/star_dragon/ (sounds.py) and sounds/ModSounds (STAR_DRAGON_...).
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class StarDragonFx {

    private StarDragonFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // Colors (ARGB)
    // =====================================================================================

    /** The Pathway's own color: starlit violet. */
    public static final int PATHWAY_COLOR = 0xFF6A5CFF;
    public static final int STAR_WHITE = 0xFFF4F6FF;
    public static final int VIOLET = 0xFF6A5CFF;
    public static final int VIOLET_LIGHT = 0xFFB8B0FF;
    public static final int VOID_INDIGO = 0xFF140E3A;
    public static final int WEAK_GREEN = 0xFF2E8A4A;
    public static final int WEAK_GREEN_LIGHT = 0xFF7CE08A;
    public static final int PIERCE_BLUE = 0xFFA8D8FF;
    public static final int SUN_GOLD = 0xFFFFE9B0;
    public static final int FUSION_GOLD = 0xFFFFC860;
    public static final int TEAL = 0xFF8CD8D0;
    public static final int RADIATION = 0xFFC8FF6A;
    public static final int STORM_BLUE = 0xFFB8D4FF;
    public static final int FROST = 0xFFD8F4FF;
    public static final int MISFORTUNE_RED = 0xFFE05050;

    /** The rays, with the color of their beam and their core (docs/STAR_DRAGON.md, "Theme"). */
    public enum RayKind {
        WEAKENING(0xE02E8A4A, 0xFFB8FFC0, 0xFF7CE08A),
        PIERCING(0xE0A8D8FF, 0xFFFFFFFF, 0xFFD8F0FF),
        SUNLIGHT(0xF0FFE9B0, 0xFFFFFFFF, 0xFFFFF4D0),
        DRAGON(0xF0D8D0FF, 0xFFFFFFFF, 0xFFF4F6FF);

        public final int color;
        final int core;
        final int spark;

        RayKind(int color, int core, int spark) {
            this.color = color;
            this.core = core;
            this.spark = spark;
        }
    }

    /** The three Cosmic Voids: the first (Star Sacrificer), the true pocket (Heavybringer), the frozen (Star Dragon). */
    public enum VoidDepth {
        HALF_REAL, TRUE_POCKET, ABSOLUTE_ZERO
    }

    // =====================================================================================
    // The small particles everything shares
    // =====================================================================================

    private static final ParticleFx STAR_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFF4F6FF).endColor(0x006A5CFF)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(-0.01f).friction(0.92f).glow();
    private static final ParticleFx VIOLET_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFB8B0FF).endColor(0x002A1E8A)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(24, 10).gravity(-0.01f).friction(0.92f).glow();
    private static final ParticleFx TWINKLE = ParticleFx.of("fx/star4")
            .color(0xFFFFFFFF).endColor(0x00B8B0FF)
            .size(0.09f).endSize(0.03f).sizeRandom(0.5f)
            .lifetime(20, 10).gravity(-0.005f).friction(0.9f).glow().spin(4f).randomRotation();
    private static final ParticleFx STILL_STAR = TWINKLE.lifetime(14, 0).gravity(0f).friction(0.5f).spin(0f);
    private static final ParticleFx CHEVRON = ParticleFx.of("fx/chevron")
            .color(0xFFB8B0FF).endColor(0x002A1E8A)
            .size(0.14f).endSize(0.1f)
            .lifetime(14, 4).gravity(0.06f).friction(0.95f).glow();
    private static final ParticleFx CHEVRON_UP = CHEVRON.color(0xFFD8F0FF).endColor(0x00A8D8FF).gravity(-0.05f);
    private static final ParticleFx DARK_DUST = ParticleFx.of("fx/smoke")
            .color(0xC0140E3A).endColor(0x00060418)
            .size(0.3f).endSize(0.8f).sizeRandom(0.3f)
            .lifetime(28, 10).gravity(0.0f).friction(0.9f).spin(2f).randomRotation();
    private static final ParticleFx GREEN_DROP = ParticleFx.of("fx/droplet")
            .color(0xFF7CE08A).endColor(0x80144A24)
            .size(0.06f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(20, 8).gravity(0.06f).friction(0.97f).glow().collide();
    private static final ParticleFx GREEN_MOTE = ParticleFx.of("fx/glow")
            .color(0xFF7CE08A).endColor(0x00144A24)
            .size(0.07f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(18, 8).gravity(-0.01f).friction(0.92f).glow();
    private static final ParticleFx BLUE_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFD8F0FF).endColor(0x004682DC)
            .size(0.07f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(18, 8).gravity(-0.01f).friction(0.92f).glow();
    private static final ParticleFx SUN_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFFF4D0).endColor(0x00FFA030)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(-0.02f).friction(0.92f).glow();
    private static final ParticleFx FLARE = ParticleFx.of("fx/flare")
            .color(0xC0FFF4D0).endColor(0x00FFC060)
            .size(0.25f).endSize(0.5f).sizeRandom(0.3f)
            .lifetime(12, 4).gravity(0f).friction(0.8f).glow();
    private static final ParticleFx SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFFFFFF).endColor(0x00B8B0FF)
            .size(0.06f).endSize(0.02f)
            .lifetime(8, 6).gravity(0.02f).friction(0.86f).glow();
    private static final ParticleFx FIRE_SPARK = SPARK.color(0xFFFFE6A0).endColor(0x00FF6A20).gravity(0.04f);
    private static final ParticleFx EMBER = ParticleFx.of("fx/glow")
            .color(0xFFFFB050).endColor(0x00A02A08)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(20, 10).gravity(-0.02f).friction(0.92f).glow();
    private static final ParticleFx DUST = ParticleFx.of("fx/smoke")
            .color(0xC08A7E76).endColor(0x00403630)
            .size(0.4f).endSize(1.3f).sizeRandom(0.4f)
            .lifetime(40, 16).gravity(-0.004f).friction(0.9f).spin(1.5f).randomRotation();
    private static final ParticleFx ROCK = ParticleFx.of("fx/rock")
            .color(0xFF8A7E76).endColor(0xC0403630)
            .size(0.09f).endSize(0.08f).sizeRandom(0.5f)
            .lifetime(26, 10).gravity(0.06f).friction(0.96f).spin(12f).randomRotation().collide();
    private static final ParticleFx ARC_A = ParticleFx.of("fx/arc_0", "fx/arc_1")
            .color(0xFFE8F0FF).endColor(0x00B8D4FF)
            .size(0.14f).endSize(0.1f).sizeRandom(0.4f)
            .lifetime(5, 3).gravity(0f).friction(0.8f).glow().randomRotation();
    private static final ParticleFx NUCLEUS = ParticleFx.of("fx/nucleus")
            .color(0xFFE8FFC0).endColor(0x0060A020)
            .size(0.1f).endSize(0.06f).sizeRandom(0.3f)
            .lifetime(20, 8).gravity(0f).friction(0.9f).glow().spin(6f).randomRotation();
    private static final ParticleFx RAD_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFE8FF9A).endColor(0x0060A020)
            .size(0.07f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(24, 10).gravity(-0.015f).friction(0.92f).glow();
    private static final ParticleFx RAD_SMOKE = ParticleFx.of("fx/smoke")
            .color(0x70C8FF6A).endColor(0x00406010)
            .size(0.3f).endSize(0.9f).sizeRandom(0.3f)
            .lifetime(30, 10).gravity(-0.01f).friction(0.92f).spin(2f).randomRotation();
    private static final ParticleFx SCALE = ParticleFx.of("fx/scale")
            .color(0xFFA8F0E8).endColor(0x802A6A70)
            .size(0.08f).endSize(0.07f).sizeRandom(0.4f)
            .lifetime(24, 10).gravity(0.04f).friction(0.95f).spin(10f).randomRotation().glow().collide();
    private static final ParticleFx SPRAY = ParticleFx.of("fx/glow")
            .color(0xF0E8FFFC).endColor(0x002A6A70)
            .size(0.1f).endSize(0.03f).sizeRandom(0.5f)
            .lifetime(16, 8).gravity(0.05f).friction(0.94f);
    private static final ParticleFx FOAM = ParticleFx.of("fx/smoke")
            .color(0xA0E8FFFC).endColor(0x0060B0B0)
            .size(0.3f).endSize(0.7f).sizeRandom(0.3f)
            .lifetime(18, 6).gravity(0.01f).friction(0.9f).randomRotation();
    private static final ParticleFx FROST_FLAKE = ParticleFx.of("fx/flake")
            .color(0xFFF4FCFF).endColor(0x409AD4FF)
            .size(0.07f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(26, 10).gravity(0.01f).friction(0.94f).spin(6f).randomRotation().glow();
    private static final ParticleFx HOURGLASS = ParticleFx.of("fx/hourglass")
            .color(0xFFD8D0FF).endColor(0x006A5CFF)
            .size(0.12f).endSize(0.1f)
            .lifetime(30, 6).gravity(-0.005f).friction(0.9f).glow();
    private static final ParticleFx SPARKLE = ParticleFx.of("fx/sparkle")
            .color(0xFFFFFFFF).endColor(0x00B8B0FF)
            .size(0.07f).endSize(0.02f).sizeRandom(0.5f)
            .lifetime(16, 8).gravity(-0.005f).friction(0.9f).glow().randomRotation();

    // =====================================================================================
    // Lines
    // =====================================================================================

    private static final SlashFx AIM_LINE = SlashFx.line("slash/dashed_fine")
            .color(0x90FFFFFF).core(0xC0FFFFFF).width(0.05f).taper(SlashFx.Taper.UNIFORM).lifetime(10).sweep(2);
    private static final SlashFx RAY_LINE = SlashFx.line("slash/smooth")
            .color(0xE0FFFFFF).core(0xFFFFFFFF).width(0.25f).taper(SlashFx.Taper.UNIFORM).lifetime(8).sweep(1);
    private static final SlashFx FIELD_LINE = SlashFx.line("slash/streaks")
            .color(0xA0B8D4FF).core(0xC0FFFFFF).width(0.12f).taper(SlashFx.Taper.UNIFORM).lifetime(6).sweep(1);
    private static final SlashFx BOLT = SlashFx.line("slash/edge")
            .color(0xF0E8F0FF).core(0xFFFFFFFF).width(0.08f).taper(SlashFx.Taper.CRESCENT).lifetime(4).sweep(1);
    private static final SlashFx STAR_TRAIL = SlashFx.line("slash/streaks")
            .color(0xB0B8B0FF).core(0xD0FFFFFF).width(0.35f).taper(SlashFx.Taper.COMET).lifetime(10).sweep(3);
    private static final SlashFx THREAD = SlashFx.line("slash/dashed_fine")
            .color(0xA0B8B0FF).core(0xC0FFFFFF).width(0.04f).taper(SlashFx.Taper.UNIFORM).lifetime(8).sweep(3);

    // =====================================================================================
    // Sounds and small helpers
    // =====================================================================================

    /** One of the pathway's own sounds (sounds/ModSounds), heard by everyone near. */
    private static void sound(ServerLevel sl, Vec3 at, RegistryObject<SoundEvent> sound, float volume, float pitch) {
        Spells.sound(sl, at, sound.get(), volume, pitch);
    }

    /** A sound only this player hears, coming from 'at'. */
    private static void soundFor(ServerPlayer player, Vec3 at, RegistryObject<SoundEvent> sound, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound.get()),
                SoundSource.PLAYERS, at.x, at.y, at.z, volume, pitch, player.getRandom().nextLong()));
    }

    private static float wobble(float pitch) {
        return pitch * (0.95f + ThreadLocalRandom.current().nextFloat() * 0.1f);
    }

    private static Vec3 chestOf(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.6, 0);
    }

    private static Vec3 headOf(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() + 0.1, 0);
    }

    /** Where a player's right hand is (their chest when they are gone). */
    private static Vec3 handOf(LivingEntity player, ServerLevel sl) {
        Vec3 hand = SpellFx.handOf(player, sl);
        return hand == null ? chestOf(player) : hand;
    }

    /** One block the way a yaw looks, level with the ground. */
    private static Vec3 ahead(float yaw) {
        double angle = Math.toRadians(yaw);
        return new Vec3(-Math.sin(angle), 0, Math.cos(angle));
    }

    private static float yawToward(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    private static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
    }

    private static float randomYaw(ServerLevel sl) {
        return sl.getRandom().nextFloat() * 360f;
    }

    /** A random spot on the ground within 'radius' of 'center'. */
    private static Vec3 within(Vec3 center, double radius, double up) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double a = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
        return center.add(Math.cos(a) * far, up, Math.sin(a) * far);
    }

    /** A flash of a standing picture at a spot, facing a yaw: pops up, then fades. */
    private static void flash(ServerLevel sl, ModelFx picture, Vec3 at, float yaw, int color, float size, int ticks) {
        ModelFx one = picture.color(color).scale(size).lifetime(ticks).fade(0, Math.max(2, ticks / 2))
                .key(0, pose().scale(0.3f).alpha(0f))
                .key(4, pose().scale(1.1f).alpha(1f), Ease.OUT_BACK)
                .key(ticks, pose().scale(0.9f).alpha(0f), Ease.IN);
        ParticleShapes.model(sl, one, at, yaw, 0f, 0f);
    }

    /** A flat picture lying on the ground, 'radius' wide, popping open and turning, for 'ticks'. */
    private static void floor(ServerLevel sl, ModelFx picture, Vec3 ground, double radius, int color, int ticks, float spin,
                              String tag, int delay) {
        ModelFx seal = picture.color(color).scale((float) (radius * 2.0)).delay(delay).lifetime(ticks)
                .fade(0, Math.min(14, ticks / 2)).spin(spin)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(10, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        if (tag != null) seal = seal.tag(tag);
        ParticleShapes.model(sl, seal, ground.add(0, 0.05, 0), randomYaw(sl), 0f, 0f);
    }

    /** A model worn by an entity for 'ticks' under 'tag' (what was worn under that tag before is taken off). */
    private static void wear(ServerLevel sl, Entity entity, ModelFx model, Vec3 offset, int ticks, String tag) {
        ParticleShapes.clearModels(sl, entity, tag);
        if (ticks <= 0) return;
        ParticleShapes.modelOnTurning(sl, model.lifetime(ticks).tag(tag), entity, offset, 0f, 0f, 0f);
    }

    /** A beam picture model laid from one point to another, 'width' wide, for 'ticks'. */
    private static void beam(ServerLevel sl, ModelFx beam, Vec3 from, Vec3 to, float width, int color, int ticks) {
        Vec3 way = to.subtract(from);
        float length = (float) Math.max(0.1, way.length());
        ModelFx one = beam.color(color).scale(width, length, width).lifetime(ticks).fade(0, Math.max(2, ticks - 2))
                .key(0, pose().scale(0.4f))
                .key(2, pose().scale(1f), Ease.OUT);
        ParticleShapes.modelAlong(sl, one, from, way);
    }

    // =====================================================================================
    // Statuses: a picture in the row above the hotbar stacks, and a clock beside the hotbar
    // =====================================================================================

    public enum Status {
        // what the Star Dragon holds
        FATE_GLIMPSE("fate_glimpse", "Fate Glimpse", 0xB8B0FF),
        LIGHTENED("lightened", "Lightened", 0xD8F0FF),
        SCALED_FORM("scaled_form", "Scaled Form", 0x8CD8D0),
        HEAVY_BODY("heavy_body", "Heavy Body", 0x8CD8D0),
        HEAVYBRINGING("heavybringing", "Heavybringing", 0x6A5CFF),
        FATE_CONVERGENCE("fate_convergence", "Fate Convergence", 0xB8B0FF),
        REMOTE_SIGHT("remote_sight", "Remote Sight", 0xB8B0FF),
        RADIANT_ANGEL("radiant_angel", "Radiant Angel", 0xFFE9B0),
        STAR_DRAGON("star_dragon", "Star Dragon", 0x6A5CFF),
        NEUTRON_STAR("neutron_star", "Neutron Star", 0xA8D8FF),
        QUANTUM("quantum", "Quantum Uncertainty", 0xB8B0FF),
        FORM_WEAKNESS("form_weakness", "Weakened Form", 0x8A84B0),
        SPENT("spent", "Spent", 0xE05050),
        UNNOTICED("unnoticed", "Unnoticed", 0x6A5CFF),
        // what is done to others (when they are players)
        CHARTED("charted", "Charted", 0xF4F6FF),
        PRESSED("pressed", "Pressed", 0x6A5CFF),
        LIFTED("lifted", "Lifted", 0xA8D8FF),
        HEAVY("heavy", "Heavy", 0x6A5CFF),
        VOIDED("voided", "In the Void", 0x6A5CFF),
        WEAKENED("weakened", "Weakened", 0x7CE08A),
        MELTING("melting", "Melting", 0x2E8A4A),
        DIMMED("dimmed", "Dimmed", 0xA8D8FF),
        MISFORTUNE("misfortune", "Misfortune", 0xB8B0FF),
        NARROWED("narrowed", "Future Narrowed", 0xE05050),
        IRRADIATED("irradiated", "Irradiated", 0xC8FF6A),
        DILATED("dilated", "Time Dilated", 0xD8D0FF),
        SEALED("sealed", "Sealed", 0x6A5CFF),
        UNCERTAIN("uncertain", "Uncertain", 0xB8B0FF);

        final String image;
        final String label;
        final int rgb;

        Status(String image, String label, int rgb) {
            this.image = image;
            this.label = label;
            this.rgb = rgb;
        }

        String id() {
            return "sd_status_" + name().toLowerCase();
        }
    }

    /** Shows the status's picture (in the row above the hotbar stacks, rows of nine) and a clock beside the hotbar. */
    public static void status(LivingEntity player, Status status, int ticks) {
        int slot = status.ordinal();
        if (player instanceof Player viewerOfHud) ScreenImages.show(viewerOfHud, status.id(), "star_dragon/" + status.image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, ticks);
        if (player instanceof Player viewerOfHud) HudTimer.show(viewerOfHud, status.id(), status.label, ticks, status.rgb);
    }

    /** How long something "until ended" is shown at most (an hour): show it again after that. */
    public static final int FOREVER = 72000;

    /** Only the picture, no clock: for what lasts until it is ended (Scaled Form is cast again to end it). */
    public static void statusForever(LivingEntity player, Status status) {
        int slot = status.ordinal();
        if (player instanceof Player viewerOfHud) ScreenImages.show(viewerOfHud, status.id(), "star_dragon/" + status.image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, FOREVER);
        if (player instanceof Player viewerOfHud) HudTimer.hide(viewerOfHud, status.id());
    }

    /** It ended early. */
    public static void statusEnd(LivingEntity player, Status status) {
        if (player instanceof Player viewerOfHud) ScreenImages.hide(viewerOfHud, status.id());
        if (player instanceof Player viewerOfHud) HudTimer.hide(viewerOfHud, status.id());
    }

    // =====================================================================================
    // The wheels' pictures (textures/gui/ability_menus/star_dragon/, for AbilityWheel.entry's icon)
    // =====================================================================================

    public static final String WHEEL_GRAVITY_ABNORMALITY = "ability_menus/star_dragon/gravity_abnormality";
    public static final String WHEEL_ELECTROMAGNETIC_ATTRACTION = "ability_menus/star_dragon/electromagnetic_attraction";
    public static final String WHEEL_COSMIC_VOID = "ability_menus/star_dragon/cosmic_void";
    public static final String WHEEL_WEAKENING_RAY = "ability_menus/star_dragon/weakening_ray";
    public static final String WHEEL_SUNLIGHT_RAY = "ability_menus/star_dragon/sunlight_ray";
    public static final String WHEEL_HEAVYBRINGING = "ability_menus/star_dragon/heavybringing";
    public static final String WHEEL_FATE_CONVERGENCE = "ability_menus/star_dragon/fate_convergence";
    public static final String WHEEL_HEAVY_SPHERE = "ability_menus/star_dragon/heavy_sphere";
    public static final String WHEEL_SERIES_OF_RAYS = "ability_menus/star_dragon/series_of_rays";
    public static final String WHEEL_REMOTE_SIGHT = "ability_menus/star_dragon/remote_sight";
    public static final String WHEEL_FUSION_CLUSTER = "ability_menus/star_dragon/fusion_cluster";
    public static final String WHEEL_RADIANT_ANGEL_FORM = "ability_menus/star_dragon/radiant_angel_form";
    public static final String WHEEL_TIME_DILATION = "ability_menus/star_dragon/time_dilation";
    public static final String WHEEL_PLANETFALL = "ability_menus/star_dragon/planetfall";
    public static final String WHEEL_STAR_DRAGON_FORM = "ability_menus/star_dragon/star_dragon_form";
    public static final String WHEEL_NEUTRON_STAR = "ability_menus/star_dragon/neutron_star";
    public static final String WHEEL_SINGULARITY = "ability_menus/star_dragon/singularity";
    public static final String WHEEL_QUANTUM_UNCERTAINTY = "ability_menus/star_dragon/quantum_uncertainty";

    // =====================================================================================
    // The models (models/model_fx/star_dragon/, see each JSON's "credit")
    // =====================================================================================

    /** A grazed asteroid, 1 block, its hot face = top; and the fire round it (play both with the same keys). */
    public static final ModelFx ASTEROID = ModelFx.of("star_dragon/asteroid").pivot(8, 8, 8).glow();
    public static final ModelFx ASTEROID_FLAME = ModelFx.of("star_dragon/asteroid_flame").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** A grazed star hanging in the air, 1.5 blocks across its rays. */
    public static final ModelFx GRAZED_STAR = ModelFx.of("star_dragon/grazed_star").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** A fusion cluster in its magnetic rings, 1 block. */
    public static final ModelFx FUSION = ModelFx.of("star_dragon/fusion_cluster").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** The Neutron Star: a blinding core with the two beams of a pulsar. */
    public static final ModelFx NEUTRON = ModelFx.of("star_dragon/neutron_star").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** Cosmic Void's sphere and the frozen shell of the highest one, 1 block. */
    public static final ModelFx VOID_SPHERE = ModelFx.of("star_dragon/void_sphere").pivot(8, 8, 8).glow().seeThrough();
    public static final ModelFx VOID_FROST = ModelFx.of("star_dragon/void_frost_shell").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** The Heavy Sphere, 1 block. */
    public static final ModelFx HEAVY_SPHERE = ModelFx.of("star_dragon/heavy_sphere").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** The Singularity's black hole (1 block across the ball) and its accretion disc (flat, its own colors). */
    public static final ModelFx BLACK_HOLE = ModelFx.of("star_dragon/black_hole").pivot(8, 8, 8).glow();
    public static final ModelFx ACCRETION = ModelFx.of("star_dragon/accretion_disk").glow().unshaded().seeThrough();
    /** Tidal Surge's crest, 1 block at scale 1, curling north; pivot the middle of its foot. */
    public static final ModelFx WAVE = ModelFx.of("star_dragon/tidal_wave").glow().seeThrough();
    /** Scaled Form, 2 blocks, worn over the player; pivot between the feet. */
    public static final ModelFx SCALED = ModelFx.of("star_dragon/scaled_form").glow();
    /** Radiant Angel Form, 3 blocks; pivot between the feet. */
    public static final ModelFx ANGEL = ModelFx.of("star_dragon/radiant_angel").pivot(8, -16, 8).glow().unshaded().seeThrough();
    /** Star Dragon Form, three steps (wings raised, level, down), 3 blocks long at scale 1; pivot under the chest. */
    public static final ModelFx DRAGON = ModelFx.of("star_dragon/star_dragon").frames(3).smooth().pivot(8, 3, 8)
            .glow().unshaded().seeThrough();
    /** Standing pictures, 1 block; pivot their middle. White unless said. */
    public static final ModelFx STAR_EYE = ModelFx.of("star_dragon/star_eye").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx MISFORTUNE_MARK = ModelFx.of("star_dragon/misfortune_mark").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx WEAKENED_MARK = ModelFx.of("star_dragon/weakened_mark").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx EMBLEM = ModelFx.of("star_dragon/star_dragon_emblem").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** Void Passage's door (its own colors); pivot the middle of its foot. */
    public static final ModelFx VOID_DOOR = ModelFx.of("star_dragon/void_door").glow().unshaded().seeThrough();
    /** Flat pictures, 1 block; pivot their middle. White unless said. */
    public static final ModelFx ASTROLABE = ModelFx.of("star_dragon/astrolabe").glow().unshaded().seeThrough();
    public static final ModelFx FATE_RING = ModelFx.of("star_dragon/fate_ring").glow().unshaded().seeThrough();
    public static final ModelFx GRAVITY_WELL = ModelFx.of("star_dragon/gravity_well").glow().unshaded().seeThrough();
    public static final ModelFx FATE_WEB = ModelFx.of("star_dragon/fate_web").glow().unshaded().seeThrough();
    public static final ModelFx IMPACT_CIRCLE = ModelFx.of("star_dragon/impact_circle").glow().unshaded().seeThrough();
    public static final ModelFx LIGHT_CRACKS = ModelFx.of("star_dragon/light_cracks").glow().unshaded().seeThrough();
    public static final ModelFx FIELD_LINES = ModelFx.of("star_dragon/field_lines").glow().unshaded().seeThrough();
    public static final ModelFx TIME_FIELD = ModelFx.of("star_dragon/time_field").glow().unshaded().seeThrough();
    public static final ModelFx QUANTUM_FIELD = ModelFx.of("star_dragon/quantum_field").glow().unshaded().seeThrough();
    /** Its own colors: the radiation zone. */
    public static final ModelFx RADIATION_GROUND = ModelFx.of("star_dragon/radiation_ground").glow().unshaded().seeThrough();
    /** Beams: two crossed planes, 1 block long; pivot the middle of their start. Lay them with modelAlong. */
    public static final ModelFx RAY_BEAM = ModelFx.of("star_dragon/ray_beam").glow().unshaded().seeThrough();
    public static final ModelFx PULSE_BEAM = ModelFx.of("star_dragon/pulse_beam").glow().unshaded().seeThrough();
    /** A soft ball of light (the shared "orb"). White, tint it. */
    private static final ModelFx ORB = ModelFx.of("orb").pivot(8, 8, 8).glow().unshaded().seeThrough();

    // =====================================================================================
    // Shared: Heaviness and the rays
    // =====================================================================================

    /**
     * Heavy (status/Weight in the plan): a dark gravity well under the target and violet chevrons pressing it down
     * every half second, for 'ticks'. Tidal Surge, Scaled Form's blows, Heavybringing's inner ring, the Neutron Star.
     */
    public static void heavy(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        if (ticks <= 0) return;
        double w = Math.max(0.6, target.getBbWidth() * 0.9);
        ModelFx well = GRAVITY_WELL.color(0xC06A5CFF).scale((float) (w * 2)).lifetime(ticks).fade(3, 8).spin(-4f).tag(tag);
        ParticleShapes.modelOn(sl, well, target, new Vec3(0, 0.06, 0), randomYaw(sl), 0f, 0f);
        for (int t = 0; t < ticks; t += 10) {
            Later.run(sl, t, () -> {
                if (!target.isAlive()) return;
                ParticleShapes.spawn(sl, CHEVRON, headOf(target).add(0, 0.3, 0), new Vec3(0, -0.08, 0));
            });
        }
    }

    public static void heavyEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
    }

    /** How long a ray's aim line shows before it fires (0.4 seconds). */
    public static final int RAY_WARNING = 8;

    /** The faint line of a ray about to fire: it shows where it will go, then the ray follows RAY_WARNING ticks later. */
    public static void rayWarning(ServerLevel sl, Vec3 from, Vec3 to, RayKind ray) {
        SpellFx.tether(sl, AIM_LINE.color(withAlpha(ray.color, 0x90)), () -> from, () -> to, RAY_WARNING);
        ParticleShapes.spawn(sl, BLUE_MOTE.color(ray.spark), from, Vec3.ZERO);
        sound(sl, from, ModSounds.STAR_DRAGON_RAY_WARN, 0.6f, 1.0f);
    }

    /**
     * A ray at light speed from 'from' to 'to', 'width' blocks thick: the beam is there at once (it cannot be dodged),
     * flashes at both ends and fades. The ray's own sound plays (Weakening, Piercing, Sunlight; the Dragon's is the
     * Piercing one lower).
     */
    public static void rayFire(ServerLevel sl, Vec3 from, Vec3 to, RayKind ray, float width) {
        ModelFx look = ray == RayKind.SUNLIGHT || ray == RayKind.DRAGON ? PULSE_BEAM : RAY_BEAM;
        beam(sl, look, from, to, width, ray.color | 0xFF000000, ray == RayKind.SUNLIGHT ? 14 : 10);
        ParticleShapes.slashBetween(sl, RAY_LINE.color(ray.color).core(ray.core).width(width * 0.4f), from, to);
        ParticleShapes.burst(sl, SPARK.color(ray.spark), from, 6, 0.03, 0.1);
        ParticleShapes.burst(sl, SPARK.color(ray.spark), to, 10, 0.05, 0.18);
        ParticleShapes.line(sl, STILL_STAR.color(ray.spark), from, to, 0.6);
        switch (ray) {
            case WEAKENING -> sound(sl, from, ModSounds.STAR_DRAGON_RAY_WEAKENING, 1.0f, wobble(1.0f));
            case PIERCING -> sound(sl, from, ModSounds.STAR_DRAGON_RAY_PIERCING, 1.0f, wobble(1.0f));
            case SUNLIGHT -> {
                sound(sl, from, ModSounds.STAR_DRAGON_SUNLIGHT_RAY, 1.4f, 1.0f);
                ParticleShapes.line(sl, FLARE, from, to, 0.5);
            }
            case DRAGON -> sound(sl, from, ModSounds.STAR_DRAGON_RAY_PIERCING, 1.3f, wobble(0.7f));
        }
    }

    // =====================================================================================
    // Key 1: Star Chart / Fate Glimpse (Astronomy Aficionado, Star Worshipper)
    // =====================================================================================

    /** The tick the chart settles on the enemy. */
    public static final int CHART_LANDS = 10;

    /**
     * The Aficionado reads the sky over an enemy: an astrolabe opens under their feet and turns, a thread of starlight
     * reaches the target. What the target gets (the constellation) is for the Aficionado alone: charted(...).
     */
    public static void starChart(ServerLevel sl, LivingEntity player, LivingEntity target) {
        floor(sl, ASTROLABE, player.position(), 1.4, 0xE0F4F6FF, 30, 3f, null, 0);
        ParticleShapes.ring(sl, TWINKLE, player.position().add(0, 2.4, 0), 1.0, 8, 0.0);
        Vec3 eye = player.getEyePosition();
        Later.run(sl, 4, () -> ParticleShapes.slashBetween(sl, THREAD.lifetime(CHART_LANDS), eye, headOf(target).add(0, 0.6, 0)));
        Later.run(sl, CHART_LANDS, () -> {
            if (target.isAlive()) ParticleShapes.burst(sl, TWINKLE, headOf(target).add(0, 0.6, 0), 4, 0.01, 0.03);
        });
        sound(sl, player.position(), ModSounds.STAR_DRAGON_STAR_CHART, 1.0f, 1.0f);
    }

    /**
     * Charted: a faint constellation over the enemy, seen by the Aficionado alone and through walls (particles are
     * drawn through nothing, so it shows wherever it is in sight range). Call it every 10 ticks while it lasts.
     */
    public static void charted(ServerPlayer viewer, LivingEntity target) {
        Vec3 top = headOf(target).add(0, 0.6, 0);
        long seed = target.getUUID().getLeastSignificantBits();
        Vec3 last = null;
        for (int i = 0; i < 5; i++) {
            double a = (seed >>> (i * 6) & 63) / 63.0 * Math.PI * 2;
            Vec3 star = top.add(Math.cos(a) * (0.3 + i * 0.12), (i % 3) * 0.18, Math.sin(a) * (0.3 + i * 0.12));
            ParticleShapes.spawnFor(viewer, STILL_STAR.lifetime(12, 0), star, Vec3.ZERO);
            if (last != null) {
                for (int k = 1; k < 4; k++) {
                    ParticleShapes.spawnFor(viewer, STAR_MOTE.lifetime(12, 0).size(0.03f).endSize(0.02f).gravity(0f),
                            last.add(star.subtract(last).scale(k / 4.0)), Vec3.ZERO);
                }
            }
            last = star;
        }
    }

    /** The calculated shot lands on a Charted enemy (its bonus spent): the constellation flares for everyone. */
    public static void calculatedHit(ServerLevel sl, LivingEntity target) {
        flash(sl, ASTROLABE.rotated(0f, 90f, 0f), chestOf(target), randomYaw(sl), 0xE0F4F6FF, 1.4f, 12);
        ParticleShapes.burst(sl, TWINKLE, chestOf(target), 12, 0.06, 0.16);
        sound(sl, chestOf(target), ModSounds.STAR_DRAGON_STARGAZING, 0.7f, 1.4f);
    }

    /** The chart fades (the Aficionado sees the stars scatter). */
    public static void chartedEnd(ServerPlayer viewer, LivingEntity target) {
        for (int i = 0; i < 6; i++) {
            ParticleShapes.spawnFor(viewer, TWINKLE, headOf(target).add(0, 0.6, 0),
                    new Vec3((Math.random() - 0.5) * 0.1, 0.03, (Math.random() - 0.5) * 0.1));
        }
    }

    // ---------- sneak: Fate Glimpse

    /** Fate Glimpse: an eye of stars opens before the Worshipper; a ring of stars circles over their head for 'ticks'. */
    public static void fateGlimpse(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        Vec3 front = player.getEyePosition().add(Spells.flatLook(player).scale(1.2));
        flash(sl, STAR_EYE, front, player.getYRot() + 180f, 0xF0D8D0FF, 1.2f, 22);
        ModelFx halo = FATE_RING.color(0xD0B8B0FF).scale(0.9f).lifetime(ticks).fade(6, 10).spin(2f).tag(tag)
                .key(0, pose().scale(2.4f).alpha(0f))
                .key(8, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, halo, player, new Vec3(0, player.getBbHeight() + 0.35, 0), 0f, 0f, 0f);
        ParticleShapes.spiral(sl, TWINKLE, player.position().add(0, 0.1, 0), 3, 24, 0.4, 1.8, 0.6, -0.05);
        sound(sl, player.position(), ModSounds.STAR_DRAGON_FATE_GLIMPSE, 1.0f, 1.0f);
    }

    /** An aimed attack is dodged: a ripple of starlight where the Worshipper stood, a streak to where they slipped. */
    public static void glimpseDodge(ServerLevel sl, LivingEntity player, Vec3 from, Vec3 to) {
        SpellFx.shockRing(sl, from, 1.6, 0xD0B8B0FF, 8, 0);
        Vec3 a = from.add(0, player.getBbHeight() * 0.55, 0), b = to.add(0, player.getBbHeight() * 0.55, 0);
        ParticleShapes.slashBetween(sl, STAR_TRAIL.lifetime(6), a, b);
        ParticleShapes.burst(sl, TWINKLE, a, 8, 0.04, 0.1);
        sound(sl, to, ModSounds.STAR_DRAGON_GLIMPSE_DODGE, 0.9f, wobble(1.0f));
    }

    public static void fateGlimpseEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.ring(sl, TWINKLE, headOf(player).add(0, 0.3, 0), 0.5, 8, 0.05);
    }

    // =====================================================================================
    // Key 2 wheel: Star Sacrifice (Star Sacrificer, Tidal Scholar)
    // =====================================================================================

    /** The tick Gravity Abnormality's field has opened (pressed or lifted from then on). */
    public static final int GRAVITY_RISEN = 12;

    /**
     * Gravity changes over an area for 'ticks'. Pressed (lifted = false): a dark violet well on the ground, the air
     * pressing down. Lifted: a pale well turning the other way and rings rising off the ground.
     */
    public static void gravityAbnormality(ServerLevel sl, Vec3 center, double radius, int ticks, boolean lifted, String tag) {
        int color = lifted ? 0xC0A8D8FF : 0xD06A5CFF;
        floor(sl, GRAVITY_WELL, center, radius, color, ticks, lifted ? 2f : -2f, tag, 0);
        if (lifted) {
            SpellFx.risingRings(sl, center, radius * 0.9, 3.2, 4, 0xA0D8F0FF);
            sound(sl, center, ModSounds.STAR_DRAGON_GRAVITY_LIFT, 1.2f, 1.0f);
        } else {
            SpellFx.closingRing(sl, center, radius, 0xC06A5CFF, GRAVITY_RISEN, 0);
            sound(sl, center, ModSounds.STAR_DRAGON_GRAVITY_PRESS, 1.3f, 1.0f);
        }
        Later.run(sl, GRAVITY_RISEN, () -> gravityTick(sl, center, radius, lifted));
    }

    /** Every second the field holds: chevrons fall (or rise) all over it. */
    public static void gravityTick(ServerLevel sl, Vec3 center, double radius, boolean lifted) {
        for (int i = 0; i < (int) (radius * 3); i++) {
            Vec3 at = within(center, radius, lifted ? 0.2 : 3.0);
            ParticleShapes.spawn(sl, lifted ? CHEVRON_UP : CHEVRON, at, new Vec3(0, lifted ? 0.08 : -0.1, 0));
        }
        if (!lifted) ParticleShapes.ring(sl, DARK_DUST, center.add(0, 0.2, 0), radius * 0.8, 10, 0.0);
    }

    /** An enemy Pressed: a burst of chevrons on its head and dust squeezed out round its feet. */
    public static void pressed(ServerLevel sl, LivingEntity enemy) {
        ParticleShapes.burst(sl, CHEVRON, headOf(enemy).add(0, 0.4, 0), 3, 0.0, 0.02);
        ParticleShapes.ring(sl, DARK_DUST, enemy.position().add(0, 0.1, 0), 0.5, 6, 0.06);
    }

    /** An enemy Lifted 'height' blocks and held: a pale ring under it, motes falling off it, for 'ticks'. */
    public static void lifted(ServerLevel sl, LivingEntity enemy, double height, int ticks, String tag) {
        SpellFx.circleUnder(sl, enemy, Math.max(0.6, enemy.getBbWidth()), 0xA0A8D8FF, ticks, tag);
        SpellFx.risingRings(sl, enemy.position(), 0.9, height, 3, 0xA0D8F0FF);
        for (int t = 0; t < ticks; t += 8) {
            Later.run(sl, t, () -> {
                if (enemy.isAlive()) ParticleShapes.spawn(sl, BLUE_MOTE, enemy.position(), new Vec3(0, -0.04, 0));
            });
        }
    }

    public static void gravityEnd(ServerLevel sl, Vec3 center, double radius, boolean lifted, String tag) {
        ParticleShapes.clearModels(sl, center.add(0, 0.05, 0), tag);
        ParticleShapes.ring(sl, lifted ? BLUE_MOTE : VIOLET_MOTE, center.add(0, 0.3, 0), radius, 16, 0.05);
    }

    /** Tidal Scholar: aimed at their own feet, the Scholar is lightened and flies: a pale ring under them, for 'ticks'. */
    public static void lightened(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        ModelFx well = GRAVITY_WELL.color(0x90D8F0FF).scale(1.6f).lifetime(ticks).fade(4, 10).spin(3f).tag(tag);
        ParticleShapes.modelOn(sl, well, player, new Vec3(0, -0.2, 0), 0f, 0f, 0f);
        sound(sl, player.position(), ModSounds.STAR_DRAGON_GRAVITY_LIFT, 1.0f, 1.2f);
    }

    /** The wave of air the lightened Scholar sheds: a ring bursting out 'radius' wide. */
    public static void airWave(ServerLevel sl, LivingEntity player, double radius) {
        SpellFx.shockRing(sl, player.position(), radius, 0xC0D8F0FF, 8, 0);
        ParticleShapes.ring(sl, FOAM.color(0x80F4F6FF), player.position().add(0, 0.5, 0), 0.8, 14, 0.25);
    }

    public static void lightenedEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
    }

    /** Heavybringer: a Lifted enemy slammed back down onto 'ground'. */
    public static void slammed(ServerLevel sl, LivingEntity enemy, Vec3 ground) {
        ParticleShapes.slashBetween(sl, STAR_TRAIL.color(0xC06A5CFF), chestOf(enemy), ground.add(0, 0.5, 0));
        SpellFx.shockRing(sl, ground, 3.0, 0xD06A5CFF, 10, 0);
        ParticleShapes.burst(sl, DARK_DUST, ground.add(0, 0.3, 0), 10, 0.05, 0.15);
        ParticleShapes.burst(sl, ROCK, ground.add(0, 0.3, 0), 8, 0.1, 0.25);
        sound(sl, ground, ModSounds.STAR_DRAGON_GRAVITY_SLAM, 1.2f, wobble(1.0f));
    }

    /** Heavybringer: a Pressed enemy under half health collapses inward. */
    public static void collapse(ServerLevel sl, LivingEntity enemy) {
        Vec3 at = chestOf(enemy);
        SpellFx.closingRing(sl, enemy.position(), 2.0, 0xE06A5CFF, 6, 0);
        ModelFx crush = ORB.color(0xC0140E3A).lifetime(8)
                .key(0, pose().scale(2.4f).alpha(0.2f))
                .key(8, pose().scale(0.1f).alpha(1f), Ease.IN);
        ParticleShapes.model(sl, crush, at, 0f, 0f, 0f);
        ParticleShapes.burst(sl, CHEVRON, at.add(0, 1, 0), 6, 0.02, 0.06);
        sound(sl, at, ModSounds.STAR_DRAGON_NEUTRON_CRUSH, 1.0f, wobble(1.1f));
    }

    // ---------- Electromagnetic Attraction

    /** Two bodies drawn together for 'ticks': field lines crackle between them all the way. */
    public static void magnetPull(ServerLevel sl, Entity a, Entity b, int ticks) {
        SpellFx.tether(sl, FIELD_LINE, a, b, ticks);
        for (int t = 0; t < ticks; t += 4) {
            Later.run(sl, t, () -> {
                Vec3 pa = SpellFx.middleOf(a, sl), pb = SpellFx.middleOf(b, sl);
                if (pa == null || pb == null) return;
                ParticleShapes.spawn(sl, ARC_A, pa.add(pb.subtract(pa).scale(Math.random())), Vec3.ZERO);
            });
        }
        sound(sl, a.position(), ModSounds.STAR_DRAGON_MAGNET_PULL, 1.0f, 1.0f);
    }

    /** They meet: a clap of force and a ring of arcs. */
    public static void magnetCollide(ServerLevel sl, Vec3 at) {
        SpellFx.blast(sl, at, 1.6, 0xFFB8D4FF, SPARK.color(0xFFE8F0FF));
        ParticleShapes.burst(sl, ARC_A, at, 8, 0.05, 0.15);
        sound(sl, at, ModSounds.STAR_DRAGON_MAGNET_COLLIDE, 1.2f, wobble(1.0f));
    }

    /** With no second enemy: the caster is drawn in and a hammer-like blow of force lands on the target. */
    public static void hammerBlow(ServerLevel sl, LivingEntity player, LivingEntity target) {
        Vec3 at = chestOf(target);
        ModelFx fist = ORB.color(0xE0B8D4FF).lifetime(8)
                .key(0, pose().scale(0.3f))
                .key(3, pose().scale(1.6f), Ease.OUT)
                .key(8, pose().scale(2.2f).alpha(0f));
        ParticleShapes.model(sl, fist, at, 0f, 0f, 0f);
        magnetCollide(sl, at);
        SpellFx.shockRing(sl, target.position(), 2.4, 0xC0B8D4FF, 8, 0);
    }

    // ---------- Cosmic Void

    /** The tick the void has closed round the target. */
    public static final int VOID_CLOSED = 10;

    /**
     * The target falls into a half-real void 'radius' wide for 'ticks' (the void follows it: its body stays in the
     * real world). TRUE_POCKET (Heavybringer) has a brighter, harder skin; ABSOLUTE_ZERO (Star Dragon) is wrapped in
     * ice near absolute zero and gives off frost and radiation.
     */
    public static void cosmicVoid(ServerLevel sl, LivingEntity target, double radius, int ticks, VoidDepth kind, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        float size = (float) (radius * 2.0);
        Vec3 offset = new Vec3(0, target.getBbHeight() * 0.5, 0);
        int tint = kind == VoidDepth.TRUE_POCKET ? 0xFFD8D0FF : 0xFFFFFFFF;
        ModelFx sphere = VOID_SPHERE.color(tint).scale(size).lifetime(ticks).fade(0, 8).spin(0.6f).tag(tag)
                .key(0, pose().scale(0.05f).alpha(0f))
                .key(VOID_CLOSED, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, sphere, target, offset, randomYaw(sl), 0f, 0f);
        if (kind == VoidDepth.ABSOLUTE_ZERO) {
            ModelFx ice = VOID_FROST.scale(size).lifetime(ticks).fade(0, 8).tag(tag)
                    .key(0, pose().scale(0.05f).alpha(0f))
                    .key(VOID_CLOSED + 4, pose().scale(1f).alpha(1f), Ease.OUT);
            ParticleShapes.modelOn(sl, ice, target, offset, randomYaw(sl), 0f, 0f);
        }
        SpellFx.closingRing(sl, target.position(), radius + 1.0, 0xC06A5CFF, VOID_CLOSED, 0);
        ParticleShapes.sphere(sl, VIOLET_MOTE, chestOf(target), radius + 0.6, 18);
        for (int t = VOID_CLOSED; t < ticks; t += 10) {
            Later.run(sl, t, () -> {
                if (!target.isAlive()) return;
                Vec3 middle = target.position().add(offset);
                ParticleShapes.spawn(sl, STILL_STAR, within(middle, radius * 0.7, 0), Vec3.ZERO);
                if (kind == VoidDepth.ABSOLUTE_ZERO) ParticleShapes.burst(sl, FROST_FLAKE, middle, 3, 0.02, 0.06);
            });
        }
        sound(sl, target.position(), ModSounds.STAR_DRAGON_VOID_OPEN, 1.0f, kind == VoidDepth.ABSOLUTE_ZERO ? 0.8f : 1.0f);
    }

    /** Tidal Scholar: a ray fired at someone inside manifests in the void: a burst of light round them. */
    public static void voidManifest(ServerLevel sl, LivingEntity target) {
        Vec3 at = chestOf(target);
        ParticleShapes.sphere(sl, SPARK.color(0xFFF4F6FF), at, 1.2, 14);
        ParticleShapes.burst(sl, TWINKLE, at, 8, 0.04, 0.12);
    }

    /** Star Dragon: every 2 seconds the frozen void freezes the target over: frost bursts off the sphere. */
    public static void voidFreeze(ServerLevel sl, LivingEntity target, double radius) {
        Vec3 at = chestOf(target);
        ParticleShapes.sphere(sl, FROST_FLAKE, at, radius * 0.8, 20);
        ParticleShapes.burst(sl, RAD_MOTE, at, 6, 0.03, 0.08);
        sound(sl, at, ModSounds.STAR_DRAGON_VOID_FREEZE, 0.9f, wobble(1.0f));
    }

    public static void voidEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, VIOLET_MOTE, chestOf(target), 14, 0.05, 0.14);
        sound(sl, target.position(), ModSounds.STAR_DRAGON_VOID_CLOSE, 0.9f, 1.0f);
    }

    // ---------- Weakening Ray

    /**
     * Weakened: green drips run off the target and one melting ring per stack hangs over its head, for 'ticks'.
     * Call again with the new count when a stack is added (the old ring of marks is replaced).
     */
    public static void weakened(ServerLevel sl, LivingEntity target, int stacks, int ticks, String tag) {
        SpellFx.overHead(sl, target, WEAKENED_MARK.color(0xE07CE08A).scale(0.35f).lifetime(ticks).fade(2, 8),
                Math.max(1, stacks), tag);
        for (int t = 0; t < ticks; t += 10) {
            Later.run(sl, t, () -> {
                if (target.isAlive()) ParticleShapes.burst(sl, GREEN_DROP, chestOf(target), 2, 0.01, 0.03);
            });
        }
    }

    public static void weakenedEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
    }

    /** Its skin melts (the Tidal Scholar's Weakening Ray, three stacks of Series of Rays): green mist boils off it. */
    public static void melting(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        SpellFx.circleUnder(sl, target, Math.max(0.7, target.getBbWidth()), 0xB02E8A4A, ticks, tag);
        for (int t = 0; t < ticks; t += 6) {
            Later.run(sl, t, () -> {
                if (!target.isAlive()) return;
                ParticleShapes.spawn(sl, RAD_SMOKE.color(0x902E8A4A).endColor(0x00144A24), chestOf(target), new Vec3(0, 0.03, 0));
                ParticleShapes.burst(sl, GREEN_DROP, chestOf(target), 2, 0.01, 0.04);
            });
        }
        sound(sl, target.position(), ModSounds.STAR_DRAGON_WEAKENED_MELT, 0.9f, wobble(1.0f));
    }

    /** The melting is over (it ran out or was cleansed). */
    public static void meltingEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
    }

    // ---------- Sunlight Ray

    /** The tick the condensed sunlight fires (after the 0.6 second charge). */
    public static final int SUNLIGHT_FIRES = 12;

    /** Sunlight condenses in the Scholar's hands: a small sun grows, flares drawn into it. Fire the ray at SUNLIGHT_FIRES. */
    public static void sunlightCharge(ServerLevel sl, LivingEntity player) {
        ModelFx sun = ORB.color(0xF0FFF4D0).lifetime(SUNLIGHT_FIRES + 2).fade(0, 2)
                .key(0, pose().scale(0.1f))
                .key(SUNLIGHT_FIRES, pose().scale(0.9f), Ease.IN);
        ParticleShapes.modelInFront(sl, sun, player, 1.0, 0.35);
        for (int t = 0; t < SUNLIGHT_FIRES; t += 2) {
            Later.run(sl, t, () -> {
                Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(1.0)).add(0, -0.35, 0);
                Vec3 from = within(hand, 2.0, 0.6);
                ParticleShapes.spawn(sl, SUN_MOTE, from, hand.subtract(from).scale(0.12));
            });
        }
        sound(sl, player.position(), ModSounds.STAR_DRAGON_SUNLIGHT_CHARGE, 1.0f, 1.0f);
    }

    /** Someone caught in the sunlight is blinded: a white flash on their face. */
    public static void sunburnt(ServerLevel sl, LivingEntity target) {
        ParticleShapes.spawn(sl, FLARE.size(0.4f).endSize(0.8f), headOf(target).add(0, -0.2, 0), Vec3.ZERO);
        ParticleShapes.burst(sl, SUN_MOTE, chestOf(target), 6, 0.03, 0.08);
    }

    // =====================================================================================
    // Key 3: Void Passage / Piercing Ray (Navigator)
    // =====================================================================================

    /** The tick the Navigator steps out of the far door. */
    public static final int PASSAGE_ARRIVES = 10;

    /**
     * Void Passage: a door of void opens where 'who' stands, they slip in, a faint path of stars runs to 'to' and a
     * second door lets them out at PASSAGE_ARRIVES. 'who' is the Navigator or (Heavybringer) the ally brought along.
     */
    public static void voidPassage(ServerLevel sl, LivingEntity who, Vec3 from, Vec3 to) {
        float yaw = who.getYRot();
        door(sl, from, yaw, 16);
        Vec3 a = from.add(0, 1.0, 0), b = to.add(0, 1.0, 0);
        ParticleShapes.line(sl, STILL_STAR.lifetime(PASSAGE_ARRIVES + 6, 0), a, b, 0.7);
        Later.run(sl, PASSAGE_ARRIVES - 4, () -> door(sl, to, yaw, 16));
        Later.run(sl, PASSAGE_ARRIVES, () -> ParticleShapes.burst(sl, VIOLET_MOTE, b, 10, 0.03, 0.1));
        sound(sl, from, ModSounds.STAR_DRAGON_VOID_PASSAGE, 1.0f, 1.0f);
    }

    private static void door(ServerLevel sl, Vec3 at, float yaw, int ticks) {
        ModelFx d = VOID_DOOR.scale(1.4f, 2.4f, 1f).lifetime(ticks).fade(0, 6)
                .key(0, pose().scale(0.05f))
                .key(4, pose().scale(1f), Ease.OUT_BACK)
                .key(ticks - 4, pose().scale(1f))
                .key(ticks, pose().scale(0.05f), Ease.IN);
        ParticleShapes.model(sl, d, at, yaw, 0f, 0f);
    }

    /** A hidden enemy forced out by the Navigator passing near: a violet ring closes on it. */
    public static void voidForcedOut(ServerLevel sl, LivingEntity hidden) {
        SpellFx.closingRing(sl, hidden.position(), 2.0, 0xE06A5CFF, 8, 0);
        ParticleShapes.burst(sl, TWINKLE, chestOf(hidden), 10, 0.04, 0.1);
    }

    /** While no mob can find the Navigator: a faint shimmer of stars round them, for 'ticks'. */
    public static void unnoticed(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        for (int t = 0; t < ticks; t += 6) {
            Later.run(sl, t, () -> {
                if (player.isAlive()) ParticleShapes.spawn(sl, STILL_STAR.color(0x80B8B0FF), within(player.position(), 0.6, 0.4 + Math.random() * 1.4), Vec3.ZERO);
            });
        }
    }

    public static void unnoticedEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.burst(sl, VIOLET_MOTE, chestOf(player), 6, 0.02, 0.06);
    }

    // ---------- sneak: Piercing Ray

    /** Two beams of blue light leave the Navigator's eyes and run to 'to' (28 blocks). */
    public static void piercingRay(ServerLevel sl, LivingEntity player, Vec3 to) {
        Vec3 eye = player.getEyePosition();
        Vec3 side = Spells.rightOf(player).scale(0.12);
        flash(sl, STAR_EYE, eye.add(Spells.flatLook(player).scale(0.6)), player.getYRot() + 180f, 0xE0A8D8FF, 0.6f, 10);
        rayFire(sl, eye.add(side), to.add(side), RayKind.PIERCING, 0.3f);
        rayFire(sl, eye.subtract(side), to.subtract(side), RayKind.PIERCING, 0.3f);
    }

    /** Dimmed: a blue-grey haze clings to the target and its glow fades, for 'ticks'. */
    public static void dimmed(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), 0x804682DC, ticks, tag);
        for (int t = 0; t < ticks; t += 12) {
            Later.run(sl, t, () -> {
                if (target.isAlive()) ParticleShapes.spawn(sl, DARK_DUST.color(0x604682DC), chestOf(target), new Vec3(0, 0.01, 0));
            });
        }
    }

    public static void dimmedEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
    }

    // =====================================================================================
    // Key 4: Tidal Surge / Scaled Form (Tidal Scholar)
    // =====================================================================================

    /** How long the crest takes to roll its whole way, and how far it goes. */
    public static final int SURGE_TICKS = 20;
    public static final double SURGE_REACH = 18.0;

    /** Tidal Surge: a crest 7 wide and 5 tall rises beside the Scholar and rolls SURGE_REACH blocks forward. */
    public static void tidalSurge(ServerLevel sl, LivingEntity player) {
        float yaw = player.getYRot();
        Vec3 start = player.position().add(ahead(yaw).scale(1.2));
        ModelFx crest = WAVE.scale(7f, 5f, 3f).lifetime(SURGE_TICKS + 8).fade(0, 8)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(5, pose().scale(1f).alpha(1f), Ease.OUT_BACK)
                .key(SURGE_TICKS, pose().forward((float) SURGE_REACH), Ease.LINEAR)
                .key(SURGE_TICKS + 8, pose().forward((float) SURGE_REACH + 1.5f).scale(0.6f).alpha(0f), Ease.IN);
        ParticleShapes.model(sl, crest, start, yaw, 0f, 0f);
        Vec3 right = ahead(yaw + 90f);
        for (int t = 0; t < SURGE_TICKS; t += 2) {
            double along = SURGE_REACH * t / SURGE_TICKS;
            Later.run(sl, t, () -> {
                Vec3 front = start.add(ahead(yaw).scale(along));
                for (int k = -3; k <= 3; k++) {
                    Vec3 at = front.add(right.scale(k)).add(0, 0.4, 0);
                    ParticleShapes.spawn(sl, FOAM, at, new Vec3(0, 0.05, 0));
                    if (k % 2 == 0) ParticleShapes.spawn(sl, SPRAY, at.add(0, 3.5, 0), ahead(yaw).scale(0.15).add(0, 0.1, 0));
                }
            });
        }
        sound(sl, player.position(), ModSounds.STAR_DRAGON_TIDAL_SURGE, 1.4f, 1.0f);
    }

    /** Someone the crest hits: spray bursts off them. */
    public static void surgeHit(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, SPRAY, chestOf(target), 10, 0.1, 0.25);
        ParticleShapes.burst(sl, FOAM, chestOf(target), 3, 0.02, 0.06);
    }

    /** While the crest is still beside the Scholar, a melee blow is parried: a slap of water at the attacker. */
    public static void surgeParry(ServerLevel sl, LivingEntity player, LivingEntity attacker) {
        Vec3 between = chestOf(player).add(chestOf(attacker)).scale(0.5);
        ParticleShapes.burst(sl, SPRAY, between, 14, 0.08, 0.22);
        SpellFx.shockRing(sl, attacker.position(), 1.4, 0xC08CD8D0, 6, 0);
        sound(sl, between, ModSounds.STAR_DRAGON_SURGE_PARRY, 1.0f, wobble(1.0f));
    }

    // ---------- sneak: Scaled Form

    /** The tick the scales have closed over the Scholar. */
    public static final int SCALES_ON = 16;
    /** The Heavy Body's size against Scaled Form's 2 blocks: 5 blocks tall. */
    public static final float HEAVY_BODY_SCALE = 2.5f;

    /**
     * Scaled Form for 'ticks': starlight scales spread over the Scholar into a humanoid lizard (worn over them). Heavy
     * Body (Heavybringer): 5 blocks tall and the ground pressed round it. Scaled Form ends when cast again, so give it
     * FOREVER and end it with scaledFormEnd.
     */
    public static void scaledForm(ServerLevel sl, LivingEntity player, int ticks, boolean heavyBody, String tag) {
        float size = heavyBody ? HEAVY_BODY_SCALE : 1f;
        ModelFx body = SCALED.scale(size).fade(0, 10)
                .key(0, pose().scale(0.85f).alpha(0f))
                .key(SCALES_ON, pose().scale(1f).alpha(1f), Ease.OUT);
        wear(sl, player, body, Vec3.ZERO, ticks, tag);
        for (int t = 0; t < SCALES_ON; t += 2) {
            Later.run(sl, t, () -> {
                if (player.isAlive()) ParticleShapes.spawn(sl, SCALE, within(player.position(), 0.5, 0.2 + Math.random() * 1.8 * size), new Vec3(0, 0.02, 0));
            });
        }
        if (heavyBody) {
            Later.run(sl, SCALES_ON, () -> {
                SpellFx.shockRing(sl, player.position(), 6.0, 0xC08CD8D0, 12, 0);
                ParticleShapes.burst(sl, ROCK, player.position().add(0, 0.2, 0), 14, 0.1, 0.3);
            });
        }
        sound(sl, player.position(), ModSounds.STAR_DRAGON_SCALES_ON, 1.0f, heavyBody ? 0.75f : 1.0f);
    }

    /** A blow of the Scaled Form lands: scales chip off and the target is pressed heavy. */
    public static void scaledBlow(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, SCALE, chestOf(target), 5, 0.05, 0.12);
        ParticleShapes.burst(sl, CHEVRON, headOf(target).add(0, 0.3, 0), 2, 0.0, 0.02);
    }

    /** Every second the Heavy Body stands: the ground round it looks pressed down. */
    public static void heavyBodyTick(ServerLevel sl, LivingEntity player) {
        ParticleShapes.ring(sl, DARK_DUST.color(0x802A6A70), player.position().add(0, 0.15, 0), 3.0, 12, 0.02);
        ParticleShapes.ring(sl, CHEVRON.color(0xFF8CD8D0), player.position().add(0, 1.2, 0), 6.0, 10, 0.0);
    }

    public static void scaledFormEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        for (double y = 0.2; y < 2.0; y += 0.4) ParticleShapes.ring(sl, SCALE, player.position().add(0, y, 0), 0.5, 6, 0.06);
        sound(sl, player.position(), ModSounds.STAR_DRAGON_SCALES_OFF, 1.0f, 1.0f);
    }

    // =====================================================================================
    // Key 5 wheel: Heavybringer
    // =====================================================================================

    /** Heavybringing: the Heavybringer becomes a source of gravity, a great well turning under them, for 'ticks'. */
    public static void heavybringing(ServerLevel sl, LivingEntity player, double radius, int ticks, String tag) {
        ModelFx well = GRAVITY_WELL.color(0xC06A5CFF).scale((float) (radius * 2.0)).lifetime(ticks).fade(10, 14).spin(-3f).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(14, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, well, player, new Vec3(0, 0.06, 0), 0f, 0f, 0f);
        ModelFx inner = GRAVITY_WELL.color(0xE0140E3A).scale(8f).lifetime(ticks).fade(6, 10).spin(-8f).tag(tag);
        ParticleShapes.modelOn(sl, inner, player, new Vec3(0, 0.08, 0), 0f, 0f, 0f);
        SpellFx.closingRing(sl, player.position(), radius, 0xD06A5CFF, 14, 0);
        sound(sl, player.position(), ModSounds.STAR_DRAGON_HEAVYBRINGING, 1.5f, 1.0f);
    }

    /** Every 10 ticks: motes and dust slide in toward the Heavybringer from the edge. */
    public static void heavybringingTick(ServerLevel sl, LivingEntity player, double radius) {
        ParticleShapes.spiral(sl, VIOLET_MOTE, player.position().add(0, 0.3, 0), 3, 18, 2.0, radius, 0.3, -0.25);
        ParticleShapes.ring(sl, DARK_DUST, player.position().add(0, 0.2, 0), radius * 0.6, 8, -0.15);
        ParticleShapes.burst(sl, CHEVRON, headOf(player).add(0, 0.6, 0), 2, 0.0, 0.02);
    }

    /** Something (a ray, a fireball, energy) drawn off its course from 'from' toward the Heavybringer at 'to'. */
    public static void bent(ServerLevel sl, Vec3 from, Vec3 to) {
        ParticleShapes.slashBetween(sl, STAR_TRAIL.color(0x906A5CFF).lifetime(6), from, to);
        ParticleShapes.burst(sl, SPARK, from, 4, 0.02, 0.06);
    }

    public static void heavybringingEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        SpellFx.shockRing(sl, player.position(), 8.0, 0xA06A5CFF, 10, 0);
    }

    // ---------- Fate Convergence

    /** Fate Convergence: threads of Fate draw in from 'radius' to the Heavybringer for 'ticks'. */
    public static void fateConvergence(ServerLevel sl, LivingEntity player, double radius, int ticks, String tag) {
        ModelFx web = FATE_WEB.color(0xC0B8B0FF).scale((float) (radius * 2.0)).lifetime(ticks).fade(10, 14).spin(-0.6f).tag(tag)
                .key(0, pose().scale(1.4f).alpha(0f))
                .key(16, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, web, player, new Vec3(0, 0.07, 0), 0f, 0f, 0f);
        ModelFx ring = FATE_RING.color(0xE0FFE9B0).scale(1.4f).lifetime(ticks).fade(6, 10).spin(3f).tag(tag);
        ParticleShapes.modelOn(sl, ring, player, new Vec3(0, player.getBbHeight() + 0.5, 0), 0f, 0f, 0f);
        ParticleShapes.spiral(sl, TWINKLE, player.position().add(0, 0.4, 0), 6, 40, 1.0, radius, 0.4, -0.3);
        sound(sl, player.position(), ModSounds.STAR_DRAGON_FATE_CONVERGENCE, 1.3f, 1.0f);
    }

    /** Misfortune on an enemy: a cracked falling star over its head for 'ticks'. */
    public static void misfortune(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        if (ticks <= 0) return;
        ModelFx mark = MISFORTUNE_MARK.color(0xD08A84B0).scale(0.45f).lifetime(ticks).fade(4, 8).spin(2f).tag(tag)
                .key(0, pose().scale(1.8f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.55, 0), 0f, 0f, 0f);
    }

    /** It stumbles (the half-second root): its feet are tangled in a thread of Fate. */
    public static void stumble(ServerLevel sl, LivingEntity target) {
        SpellFx.closingRing(sl, target.position(), 1.0, 0xC0B8B0FF, 6, 0);
        ParticleShapes.ring(sl, STILL_STAR, target.position().add(0, 0.2, 0), 0.5, 6, 0.0);
        sound(sl, target.position(), ModSounds.STAR_DRAGON_MISFORTUNE, 0.6f, wobble(1.2f));
    }

    /** An attack or aimed ability misfires: it fizzles at 'at' in a puff of violet. */
    public static void misfire(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, VIOLET_MOTE, at, 8, 0.03, 0.1);
        ParticleShapes.burst(sl, DARK_DUST, at, 2, 0.01, 0.03);
        sound(sl, at, ModSounds.STAR_DRAGON_MISFORTUNE, 0.8f, wobble(1.0f));
    }

    /** Its future narrowed to one outcome (under 25% health): the mark turns red and a single thread pins it. */
    public static void narrowed(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        if (ticks <= 0) return;
        ModelFx mark = MISFORTUNE_MARK.color(0xF0E05050).scale(0.5f).lifetime(ticks).fade(2, 8).spin(4f).tag(tag);
        ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.55, 0), 0f, 0f, 0f);
        Vec3 top = headOf(target);
        ParticleShapes.slashBetween(sl, THREAD.color(0xC0E05050).lifetime(16), top.add(0, 6, 0), top);
    }

    /** An ally in range gets Luck: a little gold star twinkles over them. */
    public static void luck(ServerLevel sl, LivingEntity ally) {
        ParticleShapes.spawn(sl, TWINKLE.color(0xFFFFE9B0), headOf(ally).add(0, 0.4, 0), new Vec3(0, 0.02, 0));
    }

    public static void fateConvergenceEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.ring(sl, TWINKLE, player.position().add(0, 0.5, 0), 3.0, 14, 0.15);
    }

    // ---------- Heavy Sphere

    /** The tick the dense shell has closed. */
    public static final int SPHERE_CLOSED = 12;

    /** The air round 'center' turns dense: a dark sphere 'radius' wide closes over it for 'ticks'. */
    public static void heavySphere(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        ModelFx shell = HEAVY_SPHERE.scale((float) (radius * 2.0)).lifetime(ticks).fade(0, 10).spin(0.4f).tag(tag)
                .key(0, pose().scale(2.2f).alpha(0f))
                .key(SPHERE_CLOSED, pose().scale(1f).alpha(1f), Ease.IN);
        ParticleShapes.model(sl, shell, center, randomYaw(sl), 0f, 0f);
        SpellFx.closingRing(sl, center.add(0, -radius * 0.9, 0), radius * 2.0, 0xC06A5CFF, SPHERE_CLOSED, 0);
        Later.run(sl, SPHERE_CLOSED, () -> ParticleShapes.sphere(sl, VIOLET_MOTE, center, radius, 30));
        sound(sl, center, ModSounds.STAR_DRAGON_HEAVY_SPHERE, 1.2f, 1.0f);
    }

    /** A projectile stops at the shell. */
    public static void sphereBlock(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, VIOLET_MOTE, at, 8, 0.03, 0.08);
        ParticleShapes.burst(sl, SPARK, at, 4, 0.02, 0.06);
        sound(sl, at, ModSounds.STAR_DRAGON_SPHERE_BLOCK, 0.9f, wobble(1.0f));
    }

    /** Someone teleporting is yanked back out of it. */
    public static void yanked(ServerLevel sl, Entity entity) {
        SpellFx.closingRing(sl, entity.position(), 2.0, 0xE06A5CFF, 6, 0);
        ParticleShapes.burst(sl, CHEVRON, chestOf(entity), 4, 0.02, 0.06);
        sound(sl, entity.position(), ModSounds.STAR_DRAGON_SPHERE_BLOCK, 1.0f, 0.7f);
    }

    public static void heavySphereEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.burst(sl, DARK_DUST, center, 8, 0.05, 0.12);
    }

    // ---------- Series of Rays

    /** When the first ray of the series fires, and the gap between rays (six rays in 1.2 seconds). */
    public static final int SERIES_FIRES = 6;
    public static final int SERIES_STEP = 4;

    /** Dark-green light gathers in the Heavybringer's hands before the series. Fire ray i at SERIES_FIRES + i * SERIES_STEP. */
    public static void seriesOfRays(ServerLevel sl, LivingEntity player) {
        Vec3 hand = handOf(player, sl);
        ModelFx glow = ORB.color(0xD07CE08A).lifetime(SERIES_FIRES + 6 * SERIES_STEP).fade(2, 6)
                .key(0, pose().scale(0.1f))
                .key(SERIES_FIRES, pose().scale(0.5f), Ease.OUT);
        ParticleShapes.modelInFront(sl, glow, player, 0.8, 0.4);
        ParticleShapes.burst(sl, GREEN_MOTE, hand, 10, 0.02, 0.06);
        sound(sl, player.position(), ModSounds.STAR_DRAGON_SERIES_OF_RAYS, 1.2f, 1.0f);
    }

    // ---------- Remote Sight

    /** Remote Sight: an eye of stars opens over the Heavybringer and a ring of sight spreads out, for 'ticks'. */
    public static void remoteSight(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        ModelFx eye = STAR_EYE.color(0xC0D8D0FF).scale(0.8f).lifetime(ticks).fade(8, 10).tag(tag)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(10, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOnTurning(sl, eye, player, new Vec3(0, player.getBbHeight() + 0.8, 0), 0f, 0f, 0f);
        SpellFx.shockRing(sl, player.position(), 32.0, 0x806A5CFF, 20, 0);
        sound(sl, player.position(), ModSounds.STAR_DRAGON_REMOTE_SIGHT, 1.0f, 1.0f);
    }

    /** A living thing seen through walls, by the Heavybringer alone: a ring of stars round it. */
    public static void sighted(ServerPlayer viewer, LivingEntity entity) {
        double w = Math.max(0.4, entity.getBbWidth() * 0.6);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            ParticleShapes.spawnFor(viewer, STILL_STAR.lifetime(20, 0), entity.position().add(Math.cos(a) * w,
                    entity.getBbHeight() * (0.2 + 0.15 * (i % 3)), Math.sin(a) * w), Vec3.ZERO);
        }
    }

    /** A spatial connection (a door, a passage, where a teleport leads), by the Heavybringer alone: a dotted line. */
    public static void passage(ServerPlayer viewer, Vec3 from, Vec3 to) {
        Vec3 way = to.subtract(from);
        int count = (int) Math.max(2, Math.min(40, way.length() * 1.5));
        for (int i = 0; i <= count; i++) {
            ParticleShapes.spawnFor(viewer, STAR_MOTE.color(0xFFB8B0FF).lifetime(20, 0).gravity(0f),
                    from.add(way.scale(i / (double) count)), Vec3.ZERO);
        }
    }

    public static void remoteSightEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
    }

    // =====================================================================================
    // Key 6: Star Grazing / Fission Pulse (Star Shepherd)
    // =====================================================================================

    /** The tick a grazed asteroid strikes the ground (1.2 seconds after the warning circle opens). */
    public static final int GRAZE_LANDS = 24;
    /** How high the asteroid starts. */
    static final float GRAZE_HEIGHT = 30f;

    /**
     * Star Grazing: a warning circle 'radius' wide opens on 'ground', and a small asteroid wrapped in fire falls onto
     * it, striking at GRAZE_LANDS (the strike's dust and shock are played here too). Star Dragon's three asteroids: call
     * it three times, 10 ticks apart.
     */
    public static void starGrazing(ServerLevel sl, Vec3 ground, double radius) {
        warning(sl, ground, radius, GRAZE_LANDS);
        skyfall(sl, ground, 1.6f, GRAZE_HEIGHT, GRAZE_LANDS);
        sound(sl, ground.add(0, 10, 0), ModSounds.STAR_DRAGON_ASTEROID_FALL, 2.0f, wobble(1.0f));
        Later.run(sl, GRAZE_LANDS, () -> {
            SpellFx.blast(sl, ground.add(0, 0.6, 0), radius, 0xFFFFB050, FIRE_SPARK);
            SpellFx.shockRing(sl, ground, radius * 1.4, 0xC08A7E76, 14, 0);
            ParticleShapes.burst(sl, DUST, ground.add(0, 0.5, 0), (int) (radius * 3), 0.05, 0.2);
            ParticleShapes.burst(sl, ROCK, ground.add(0, 0.5, 0), (int) (radius * 4), 0.15, 0.4);
            ParticleShapes.burst(sl, EMBER, ground.add(0, 0.5, 0), 14, 0.05, 0.2);
            sound(sl, ground, ModSounds.STAR_DRAGON_ASTEROID_IMPACT, 2.0f, wobble(1.0f));
        });
    }

    /** The warning circle of a sky ability, open from now to the strike at 'ticks'. */
    private static void warning(ServerLevel sl, Vec3 ground, double radius, int ticks) {
        ModelFx circle = IMPACT_CIRCLE.color(0xD0FFB050).scale((float) (radius * 2.0)).lifetime(ticks + 2).fade(4, 2).spin(1.5f)
                .key(0, pose().scale(0.3f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, circle, ground.add(0, 0.06, 0), randomYaw(sl), 0f, 0f);
        SpellFx.closingRing(sl, ground, radius, 0xA0FFB050, ticks, 0);
    }

    /** A rock falling head first from 'height' onto 'ground', in fire, landing at 'ticks'. */
    private static void skyfall(ServerLevel sl, Vec3 ground, float size, float height, int ticks) {
        Vec3 top = ground.add(0, height, 0);
        ModelFx[] parts = {ASTEROID, ASTEROID_FLAME};
        for (ModelFx part : parts) {
            ModelFx falling = part.scale(size).lifetime(ticks + 1).fade(2, 1).spin(9f)
                    .key(0, pose().alpha(0f))
                    .key(3, pose().alpha(1f))
                    .key(ticks, pose().up(height - size * 0.5f), Ease.IN);
            ParticleShapes.modelAlong(sl, falling, top, new Vec3(0, -1, 0));
        }
        for (int t = 2; t < ticks; t += 2) {
            double h = height * (1 - Math.pow(t / (double) ticks, 2));
            Later.run(sl, t, () -> {
                Vec3 at = ground.add(0, h + size, 0);
                ParticleShapes.spawn(sl, EMBER, at, Vec3.ZERO);
                ParticleShapes.spawn(sl, DUST.color(0x80403630), at.add(0, size, 0), Vec3.ZERO);
            });
        }
    }

    /** A grazed star left hanging over the spot for 'ticks', a column of soft light under it. */
    public static void grazedStar(ServerLevel sl, Vec3 above, int ticks, String tag) {
        ModelFx star = GRAZED_STAR.scale(1.2f).lifetime(ticks).fade(10, 20).spin(1.5f).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(14, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, star, above, 0f, 0f, 0f);
        sound(sl, above, ModSounds.STAR_DRAGON_GRAZED_STAR, 1.0f, 1.0f);
    }

    /** The grazed star helps an ally under it: a mote of starlight drifts down into them. */
    public static void grazedStarFeeds(ServerLevel sl, Vec3 star, LivingEntity ally) {
        Vec3 to = chestOf(ally);
        ParticleShapes.spawn(sl, STAR_MOTE.lifetime(30, 4).gravity(0f).friction(1f), star, to.subtract(star).scale(1 / 30.0));
        ParticleShapes.spawn(sl, TWINKLE, to, new Vec3(0, 0.02, 0));
    }

    public static void grazedStarEnd(ServerLevel sl, Vec3 above, String tag) {
        ParticleShapes.clearModels(sl, above, tag);
        ParticleShapes.burst(sl, TWINKLE, above, 12, 0.03, 0.1);
    }

    // ---------- sneak: Fission Pulse

    /** The tick the loosened nuclei split. */
    public static final int FISSION_SPLITS = 40;

    /** Fission Pulse: the target glows sickly green, nuclei loosening in it, faster and faster until FISSION_SPLITS. */
    public static void fissionPulse(ServerLevel sl, LivingEntity player, LivingEntity target) {
        ParticleShapes.slashBetween(sl, THREAD.color(0xC0C8FF6A).lifetime(10), handOf(player, sl), chestOf(target));
        SpellFx.circleUnder(sl, target, Math.max(0.7, target.getBbWidth()), 0xC0C8FF6A, FISSION_SPLITS, "sd_fission");
        for (int t = 0; t < FISSION_SPLITS; t += Math.max(2, 8 - t / 6)) {
            int step = t;
            Later.run(sl, t, () -> {
                if (!target.isAlive()) return;
                ParticleShapes.burst(sl, NUCLEUS, chestOf(target), 1 + step / 12, 0.02, 0.05);
                ParticleShapes.spawn(sl, RAD_MOTE, chestOf(target), Vec3.ZERO);
            });
        }
        sound(sl, target.position(), ModSounds.STAR_DRAGON_FISSION_GLOW, 1.0f, 1.0f);
    }

    /** The split: a burst of radiation 'radius' wide at 'at'. */
    public static void fissionBurst(ServerLevel sl, Vec3 at, double radius) {
        SpellFx.blast(sl, at, radius, 0xFFC8FF6A, RAD_MOTE);
        ParticleShapes.burst(sl, NUCLEUS, at, 12, 0.1, 0.3);
        ParticleShapes.burst(sl, RAD_SMOKE, at, 8, 0.03, 0.1);
        sound(sl, at, ModSounds.STAR_DRAGON_FISSION_BURST, 1.6f, wobble(1.0f));
    }

    /** Irradiated: green motes rise off the target and a faint ring sits under it, for 'ticks'. */
    public static void irradiated(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), 0x90C8FF6A, ticks, tag);
        for (int t = 0; t < ticks; t += 8) {
            Later.run(sl, t, () -> {
                if (target.isAlive()) ParticleShapes.spawn(sl, RAD_MOTE, within(chestOf(target), 0.4, 0), new Vec3(0, 0.03, 0));
            });
        }
    }

    public static void irradiatedEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
    }

    /** The radiation zone left where it burst, 'radius' wide, for 'ticks'. */
    public static void radiationZone(ServerLevel sl, Vec3 ground, double radius, int ticks, String tag) {
        floor(sl, RADIATION_GROUND, ground, radius, 0xFFFFFFFF, ticks, 0.2f, tag, 0);
        sound(sl, ground, ModSounds.STAR_DRAGON_RADIATION, 0.8f, 1.0f);
        for (int t = 10; t < ticks; t += 10) {
            Later.run(sl, t, () -> ParticleShapes.spawn(sl, RAD_SMOKE, within(ground, radius, 0.2), new Vec3(0, 0.02, 0)));
        }
    }

    public static void radiationZoneEnd(ServerLevel sl, Vec3 ground, String tag) {
        ParticleShapes.clearModels(sl, ground.add(0, 0.05, 0), tag);
    }

    /** Star Dragon: the electromagnetic storm left after the burst, field lines on the ground for 'ticks'. */
    public static void emStorm(ServerLevel sl, Vec3 ground, double radius, int ticks, String tag) {
        floor(sl, FIELD_LINES, ground, radius, 0xD0B8D4FF, ticks, 1f, tag, 0);
        for (int t = 0; t < ticks; t += 5) {
            Later.run(sl, t, () -> ParticleShapes.spawn(sl, ARC_A, within(ground, radius, 0.3 + Math.random() * 2), Vec3.ZERO));
        }
    }

    /** An arc of the storm (or Fission Touch) strikes from 'from' to 'to'. */
    public static void emArc(ServerLevel sl, Vec3 from, Vec3 to) {
        ParticleShapes.slashBetween(sl, BOLT, from, to);
        ParticleShapes.burst(sl, ARC_A, to, 3, 0.02, 0.06);
        sound(sl, to, ModSounds.STAR_DRAGON_ARC, 0.8f, wobble(1.0f));
    }

    /** A projectile stopped dead in the storm. */
    public static void stormStop(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, ARC_A, at, 5, 0.02, 0.08);
        sound(sl, at, ModSounds.STAR_DRAGON_ARC, 0.7f, 1.3f);
    }

    public static void emStormEnd(ServerLevel sl, Vec3 ground, String tag) {
        ParticleShapes.clearModels(sl, ground.add(0, 0.05, 0), tag);
    }

    // =====================================================================================
    // Key 7 wheel: Cosmos (Radiant Angel, Star Dragon, Condenser)
    // =====================================================================================

    // ---------- Fusion Cluster

    /** The tick the clusters have condensed (they circle from then on until launched). */
    public static final int FUSION_READY = 20;
    /** Where the clusters circle: radius, height from the feet, degrees a tick (the same as an Orbit with these). */
    public static final double FUSION_RADIUS = 1.6;
    public static final double FUSION_HEIGHT = 1.3;
    public static final float FUSION_SPEED = 12f;
    /** A cluster for the Shot that flies at the enemy. */
    public static final ModelFx FUSION_FLYING = FUSION.scale(0.6f).spin(30f).lifetime(40).fade(0, 2);

    private static String fusionTag(String tag, int index) {
        return tag + "_" + index;
    }

    /** Where cluster 'index' of 'count' is, 'age' ticks after it condensed. */
    public static Vec3 fusionSpot(LivingEntity player, int index, int count, int age) {
        double a = Math.toRadians(age * FUSION_SPEED + index * 360.0 / count);
        return player.position().add(Math.cos(a) * FUSION_RADIUS, FUSION_HEIGHT, Math.sin(a) * FUSION_RADIUS);
    }

    /**
     * 'count' fusion clusters (3, five from the Condenser) condense round the Radiant Angel and circle for 'ticks'.
     * They do not move by themselves: call fusionOrbit every tick. The Condenser's have no windup: launch them at once
     * (they are small for the first ticks, which reads as condensing in flight).
     */
    public static void fusionCluster(ServerLevel sl, LivingEntity player, int count, int ticks, String tag) {
        for (int i = 0; i < count; i++) {
            ModelFx one = FUSION.scale(0.55f).lifetime(ticks).fade(0, 4).spin(20f).tag(fusionTag(tag, i))
                    .key(0, pose().scale(0.05f))
                    .key(FUSION_READY, pose().scale(1f), Ease.OUT_BACK);
            ParticleShapes.model(sl, one, fusionSpot(player, i, count, 0), 0f, 0f, 0f);
        }
        for (int t = 0; t < FUSION_READY; t += 2) {
            Later.run(sl, t, () -> {
                Vec3 at = player.position().add(0, FUSION_HEIGHT, 0);
                Vec3 from = within(at, 3.0, Math.random() * 2 - 1);
                ParticleShapes.spawn(sl, SUN_MOTE, from, at.subtract(from).scale(0.1));
            });
        }
        sound(sl, player.position(), ModSounds.STAR_DRAGON_FUSION_CONDENSE, 1.2f, 1.0f);
    }

    /** Every tick while the clusters circle: they move round ('age' = ticks since they were made). */
    public static void fusionOrbit(ServerLevel sl, LivingEntity player, int count, String tag, int age) {
        for (int i = 0; i < count; i++) {
            ParticleShapes.moveModel(sl, fusionTag(tag, i), fusionSpot(player, i, count, age), 0f, 0f, 0f);
        }
        if (age % 6 == 0) ParticleShapes.spawn(sl, ARC_A.color(0xFFD8E4FF), fusionSpot(player, age / 6 % count, count, age), Vec3.ZERO);
    }

    /** Cluster 'index' is launched (its circling model goes): returns where the Shot leaves from. */
    public static Vec3 fusionLaunch(ServerLevel sl, LivingEntity player, int index, int count, String tag, int age) {
        Vec3 at = fusionSpot(player, index, count, age);
        ParticleShapes.clearModels(sl, at, fusionTag(tag, index));
        ParticleShapes.burst(sl, SUN_MOTE, at, 6, 0.03, 0.08);
        sound(sl, at, ModSounds.STAR_DRAGON_FUSION_LAUNCH, 1.0f, wobble(1.0f));
        return at;
    }

    /** A cluster bursts 'radius' wide. implode (Star Dragon's Inertial Confinement): it falls in first, then bursts. */
    public static void fusionBurst(ServerLevel sl, Vec3 at, double radius, boolean implode) {
        if (implode) {
            ModelFx crush = ORB.color(0xF0FFF4D0).lifetime(6)
                    .key(0, pose().scale((float) radius))
                    .key(6, pose().scale(0.2f), Ease.IN);
            ParticleShapes.model(sl, crush, at, 0f, 0f, 0f);
            SpellFx.closingRing(sl, at.add(0, -0.5, 0), radius, 0xE0FFE9B0, 6, 0);
        }
        int delay = implode ? 6 : 0;
        Later.run(sl, delay, () -> {
            SpellFx.blast(sl, at, radius, 0xFFFFE9B0, SUN_MOTE);
            ParticleShapes.burst(sl, FLARE, at, 6, 0.05, 0.15);
            sound(sl, at, ModSounds.STAR_DRAGON_FUSION_BURST, 1.5f, wobble(implode ? 0.8f : 1.0f));
        });
    }

    // ---------- Radiant Angel Form

    /** The tick the Radiant Angel has unfolded its wings. */
    public static final int ANGEL_READY = 20;

    /** Radiant Angel Form for 'ticks': a being of starlight 3 blocks tall with six pairs of wings, worn over the player. */
    public static void radiantAngel(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        ModelFx body = ANGEL.fade(0, 14)
                .key(0, pose().scale(0.3f).alpha(0f))
                .key(ANGEL_READY, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        wear(sl, player, body, Vec3.ZERO, ticks, tag);
        SpellFx.risingRings(sl, player.position(), 2.0, 3.0, 5, 0xC0FFE9B0);
        ParticleShapes.cone(sl, SUN_MOTE, player.position().add(0, 1, 0), Spells.UP, 60, 30, 0.1, 0.3);
        Later.run(sl, ANGEL_READY, () -> {
            if (!player.isAlive()) return;
            ParticleShapes.burst(sl, FLARE, player.position().add(0, 2, 0), 10, 0.05, 0.2);
            SpellFx.shockRing(sl, player.position(), 8.0, 0xC0FFE9B0, 12, 0);
        });
        sound(sl, player.position(), ModSounds.STAR_DRAGON_RADIANT_ANGEL, 1.6f, 1.0f);
    }

    /** Every 10 ticks of the form (and Radiance, the passive): a glow of motes out to 'radius'. */
    public static void radiance(ServerLevel sl, LivingEntity player, double radius) {
        ParticleShapes.ring(sl, SUN_MOTE, player.position().add(0, 0.6, 0), radius * 0.5, 10, 0.1);
        ParticleShapes.spawn(sl, FLARE.size(0.4f).endSize(0.7f).color(0x60FFF4D0), player.position().add(0, 1.6, 0), Vec3.ZERO);
    }

    /** An enemy burned by the glow: a flicker of white-gold on it. */
    public static void radiantBurn(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, SUN_MOTE, chestOf(target), 3, 0.02, 0.05);
        if (target.tickCount % 20 == 0) sound(sl, chestOf(target), ModSounds.STAR_DRAGON_RADIANCE, 0.5f, wobble(1.0f));
    }

    public static void radiantAngelEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        for (double y = 0.2; y < 3.0; y += 0.5) ParticleShapes.ring(sl, SUN_MOTE, player.position().add(0, y, 0), 1.2, 8, 0.08);
        sound(sl, player.position(), ModSounds.STAR_DRAGON_FORM_END, 1.0f, 1.1f);
    }

    /** The weakness after a Mythical Creature Form: a dull grey haze clings to the player for 'ticks'. */
    public static void formWeakness(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        SpellFx.circleUnder(sl, player, 0.7, 0x608A84B0, ticks, tag);
        for (int t = 0; t < ticks; t += 20) {
            Later.run(sl, t, () -> {
                if (player.isAlive()) ParticleShapes.spawn(sl, DARK_DUST.color(0x608A84B0), chestOf(player), new Vec3(0, -0.01, 0));
            });
        }
    }

    // ---------- Gravitational Time Dilation

    /** The tick the field of slowed time has spread. */
    public static final int DILATION_RISEN = 14;

    /** Time Dilation: a clock of slowed time 'radius' wide where the Star Dragon aims, for 'ticks'. */
    public static void timeDilation(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        ModelFx field = TIME_FIELD.color(0xC0D8D0FF).scale((float) (radius * 2.0)).lifetime(ticks).fade(0, 14).spin(0.15f).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(DILATION_RISEN, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, field, center.add(0, 0.06, 0), randomYaw(sl), 0f, 0f);
        ModelFx well = GRAVITY_WELL.color(0x606A5CFF).scale((float) (radius * 1.2)).lifetime(ticks).fade(10, 14).spin(-0.3f).tag(tag);
        ParticleShapes.model(sl, well, center.add(0, 0.08, 0), randomYaw(sl), 0f, 0f);
        SpellFx.steadyRing(sl, center, radius, 0xA0D8D0FF, DILATION_RISEN, 0);
        for (int t = DILATION_RISEN; t < ticks; t += 10) {
            Later.run(sl, t, () -> {
                for (int i = 0; i < 4; i++) {
                    ParticleShapes.spawn(sl, HOURGLASS.gravity(0f).friction(0.5f).lifetime(30, 10), within(center, radius, 0.5 + Math.random() * 2.5), Vec3.ZERO);
                }
            });
        }
        sound(sl, center, ModSounds.STAR_DRAGON_TIME_DILATION, 1.3f, 1.0f);
    }

    /** An enemy slowed in time: a faint clock ring round it for 'ticks'. */
    public static void dilated(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        if (ticks <= 0) return;
        ModelFx clock = TIME_FIELD.color(0xA0D8D0FF).scale(Math.max(1.4f, target.getBbWidth() * 2.2f)).lifetime(ticks).fade(4, 8).tag(tag);
        ParticleShapes.modelOn(sl, clock, target, new Vec3(0, target.getBbHeight() * 0.5, 0), 0f, 0f, 0f);
    }

    /** A projectile hanging in the slowed air: a still ring of stars round it. */
    public static void timeHang(ServerLevel sl, Vec3 at) {
        ParticleShapes.ring(sl, STILL_STAR.lifetime(20, 0), at, 0.4, 6, 0.0);
        if (ThreadLocalRandom.current().nextInt(4) == 0) sound(sl, at, ModSounds.STAR_DRAGON_TIME_HANG, 0.6f, wobble(1.0f));
    }

    public static void timeDilationEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center.add(0, 0.06, 0), tag);
        ParticleShapes.clearModels(sl, center.add(0, 0.08, 0), tag);
        SpellFx.shockRing(sl, center, radius, 0x80D8D0FF, 10, 0);
    }

    // ---------- Planetfall

    /** The tick Planetfall strikes (4 seconds of descent). */
    public static final int PLANET_LANDS = 80;
    static final float PLANET_HEIGHT = 60f;

    /**
     * Planetfall: a circle 'radius' wide (24) opens on 'ground' and a huge asteroid lowers through the sky, its warning
     * growing, striking at PLANET_LANDS: a blinding flash, rings, everything thrown out. Open the cracks of light
     * after (lightCracks).
     */
    public static void planetfall(ServerLevel sl, Vec3 ground, double radius) {
        warning(sl, ground, radius, PLANET_LANDS);
        for (int i = 1; i <= 3; i++) SpellFx.closingRing(sl, ground, radius, 0xC0FFB050, PLANET_LANDS / 3, (i - 1) * PLANET_LANDS / 3);
        float size = (float) Math.max(6.0, radius * 0.5);
        skyfall(sl, ground, size, PLANET_HEIGHT, PLANET_LANDS);
        sound(sl, ground.add(0, 20, 0), ModSounds.STAR_DRAGON_PLANET_DESCEND, 4.0f, 1.0f);
        Later.run(sl, PLANET_LANDS, () -> {
            ModelFx flashBall = ORB.color(0xFFFFFFFF).lifetime(16)
                    .key(0, pose().scale(2f))
                    .key(6, pose().scale((float) radius * 1.6f), Ease.OUT)
                    .key(16, pose().scale((float) radius * 2f).alpha(0f), Ease.IN);
            ParticleShapes.model(sl, flashBall, ground.add(0, 1, 0), 0f, 0f, 0f);
            for (int k = 0; k < 3; k++) SpellFx.shockRing(sl, ground, radius * (1.2 + k * 0.4), 0xE0FFE9B0, 14 + k * 4, k * 3);
            SpellFx.blast(sl, ground.add(0, 1, 0), Math.min(radius, 16), 0xFFFFB050, FIRE_SPARK);
            ParticleShapes.burst(sl, DUST, ground.add(0, 1, 0), 60, 0.2, 0.6);
            ParticleShapes.burst(sl, ROCK, ground.add(0, 1, 0), 50, 0.3, 0.8);
            ParticleShapes.burst(sl, EMBER, ground.add(0, 1, 0), 40, 0.1, 0.5);
            sound(sl, ground, ModSounds.STAR_DRAGON_PLANET_IMPACT, 6.0f, 1.0f);
        });
    }

    /** Planetfall's cracks of white light, 'radius' wide, for 'ticks' (10 seconds). */
    public static void lightCracks(ServerLevel sl, Vec3 ground, double radius, int ticks, String tag) {
        floor(sl, LIGHT_CRACKS, ground, radius, 0xF0FFFFFF, ticks, 0f, tag, 0);
        for (int t = 0; t < ticks; t += 10) {
            Later.run(sl, t, () -> {
                for (int i = 0; i < 4; i++) ParticleShapes.spawn(sl, SPARK.gravity(-0.03f), within(ground, radius, 0.1), new Vec3(0, 0.05, 0));
            });
        }
        sound(sl, ground, ModSounds.STAR_DRAGON_LIGHT_CRACKS, 1.4f, 1.0f);
    }

    /** Someone standing on a crack is burned. */
    public static void crackBurn(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, SPARK, target.position().add(0, 0.2, 0), 4, 0.03, 0.08);
    }

    public static void lightCracksEnd(ServerLevel sl, Vec3 ground, String tag) {
        ParticleShapes.clearModels(sl, ground.add(0, 0.05, 0), tag);
    }

    // ---------- Star Dragon Form

    /** The tick the Star Dragon is whole; its size; and one wingbeat's length. */
    public static final int DRAGON_READY = 30;
    public static final float DRAGON_SCALE = 3.5f;
    public static final int DRAGON_BEAT = 20;

    /**
     * Star Dragon Form for 'ticks': a colossal dragon of transparent starlight (10.5 blocks long), worn by the player,
     * formed out of falling stars, beating its wings a few times and then gliding. Call dragonBeat while it flies.
     */
    public static void starDragonForm(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        ModelFx body = DRAGON.scale(DRAGON_SCALE).fade(0, 16)
                .key(0, pose().frame(1f).scale(0.2f).alpha(0f))
                .key(DRAGON_READY, pose().frame(0f).scale(1f).alpha(1f), Ease.OUT_BACK);
        for (int i = 0; i < 4; i++) {
            int t0 = DRAGON_READY + i * DRAGON_BEAT;
            body = body.key(t0 + DRAGON_BEAT / 2, pose().frame(2f), Ease.IN_OUT).key(t0 + DRAGON_BEAT, pose().frame(0f), Ease.IN_OUT);
        }
        body = body.key(DRAGON_READY + 4 * DRAGON_BEAT + 10, pose().frame(1f), Ease.IN_OUT);
        wear(sl, player, body, Vec3.ZERO, ticks, tag);
        for (int t = 0; t < DRAGON_READY; t += 2) {
            Later.run(sl, t, () -> {
                if (!player.isAlive()) return;
                Vec3 at = within(player.position(), 5.0, 1 + Math.random() * 4);
                ParticleShapes.spawn(sl, TWINKLE, at.add(0, 4, 0), new Vec3(0, -0.2, 0));
                ParticleShapes.spawn(sl, VIOLET_MOTE, at, player.position().add(0, 1.5, 0).subtract(at).scale(0.08));
            });
        }
        for (int i = 0; i < 4; i++) {
            Later.run(sl, DRAGON_READY + i * DRAGON_BEAT + DRAGON_BEAT / 2, () -> {
                if (player.isAlive()) sound(sl, player.position(), ModSounds.STAR_DRAGON_DRAGON_WING, 1.4f, wobble(0.9f));
            });
        }
        Later.run(sl, DRAGON_READY, () -> {
            if (!player.isAlive()) return;
            SpellFx.shockRing(sl, player.position(), 12.0, 0xC06A5CFF, 16, 0);
            ParticleShapes.burst(sl, TWINKLE, player.position().add(0, 2, 0), 30, 0.1, 0.4);
        });
        sound(sl, player.position(), ModSounds.STAR_DRAGON_STAR_DRAGON, 2.5f, 1.0f);
    }

    /** One wingbeat (DRAGON_BEAT ticks) and back to gliding; 'ticksLeft' = how long the form still lasts. */
    public static void dragonBeat(ServerLevel sl, LivingEntity player, int ticksLeft, String tag) {
        ModelFx body = DRAGON.scale(DRAGON_SCALE).fade(0, 16)
                .key(0, pose().frame(1f))
                .key(DRAGON_BEAT / 3, pose().frame(0f), Ease.IN_OUT)
                .key(DRAGON_BEAT, pose().frame(2f), Ease.IN_OUT)
                .key(DRAGON_BEAT + 10, pose().frame(1f), Ease.IN_OUT);
        wear(sl, player, body, Vec3.ZERO, ticksLeft, tag);
        Later.run(sl, DRAGON_BEAT * 2 / 3, () -> {
            if (!player.isAlive()) return;
            sound(sl, player.position(), ModSounds.STAR_DRAGON_DRAGON_WING, 1.4f, wobble(0.9f));
            ParticleShapes.burst(sl, TWINKLE, player.position().add(0, 1, 0), 10, 0.1, 0.25);
        });
    }

    /** Where the Dragon's small asteroid circles: radius, height from the feet, degrees a tick. */
    public static final double ASTEROID_RADIUS = 3.2;
    public static final double ASTEROID_HEIGHT = 3.0;
    public static final float ASTEROID_SPEED = 6f;
    /** The asteroid for the Shot at the enemy. */
    public static final ModelFx ASTEROID_FLYING = ASTEROID.scale(0.6f).spin(24f).lifetime(40).fade(0, 2);

    private static String asteroidTag(String tag) {
        return tag + "_asteroid";
    }

    public static Vec3 asteroidSpot(LivingEntity player, int age) {
        double a = Math.toRadians(age * ASTEROID_SPEED);
        return player.position().add(Math.cos(a) * ASTEROID_RADIUS, ASTEROID_HEIGHT, Math.sin(a) * ASTEROID_RADIUS);
    }

    /** The Dragon's small asteroid takes its place for 'ticks'; move it with dragonAsteroidOrbit every tick. */
    public static void dragonAsteroid(ServerLevel sl, LivingEntity player, int ticks, String tag, int age) {
        ModelFx rock = ASTEROID.scale(0.6f).lifetime(ticks).fade(4, 6).spin(8f).tag(asteroidTag(tag))
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(8, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, rock, asteroidSpot(player, age), 0f, 0f, 0f);
    }

    public static void dragonAsteroidOrbit(ServerLevel sl, LivingEntity player, String tag, int age) {
        Vec3 at = asteroidSpot(player, age);
        ParticleShapes.moveModel(sl, asteroidTag(tag), at, 0f, 0f, 0f);
        if (age % 4 == 0) ParticleShapes.spawn(sl, EMBER, at, Vec3.ZERO);
    }

    /** The asteroid is launched (its circling model goes): returns where the Shot leaves from. */
    public static Vec3 dragonShot(ServerLevel sl, LivingEntity player, String tag, int age) {
        Vec3 at = asteroidSpot(player, age);
        ParticleShapes.clearModels(sl, at, asteroidTag(tag));
        ParticleShapes.burst(sl, EMBER, at, 8, 0.05, 0.12);
        sound(sl, at, ModSounds.STAR_DRAGON_DRAGON_SHOT, 1.2f, wobble(1.0f));
        return at;
    }

    /** The asteroid strikes (the Shot hit at 'at'). */
    public static void dragonShotHit(ServerLevel sl, Vec3 at) {
        SpellFx.blast(sl, at, 2.0, 0xFFFFB050, FIRE_SPARK);
        ParticleShapes.burst(sl, ROCK, at, 10, 0.1, 0.3);
        sound(sl, at, ModSounds.STAR_DRAGON_ASTEROID_IMPACT, 1.0f, wobble(1.3f));
    }

    /** The asteroid comes back after its recharge, for the 'ticksLeft' the form still lasts. */
    public static void dragonAsteroidBack(ServerLevel sl, LivingEntity player, int ticksLeft, String tag, int age) {
        if (ticksLeft <= 0) return;
        dragonAsteroid(sl, player, ticksLeft, tag, age);
        ParticleShapes.spawn(sl, SPARKLE, asteroidSpot(player, age), Vec3.ZERO);
    }

    public static void starDragonFormEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.clearModels(sl, player.position(), asteroidTag(tag));
        for (int i = 0; i < 40; i++) {
            ParticleShapes.spawn(sl, TWINKLE, within(player.position(), 5.0, Math.random() * 3), new Vec3(0, 0.05, 0));
        }
        sound(sl, player.position(), ModSounds.STAR_DRAGON_FORM_END, 1.4f, 0.8f);
    }

    // ---------- Neutron Star

    /** The tick the body has reached the density of a neutron star. */
    public static final int NEUTRON_READY = 22;

    /** Neutron Star for 'ticks': light crushed into a blinding core at the chest, pulsar beams sweeping, a well under. */
    public static void neutronStar(ServerLevel sl, LivingEntity player, double radius, int ticks, String tag) {
        Vec3 chest = new Vec3(0, player.getBbHeight() * 0.6, 0);
        ModelFx core = NEUTRON.scale(0.8f).lifetime(ticks).fade(0, 10).spin(14f).tag(tag)
                .key(0, pose().scale(4f).alpha(0f).roll(22.5f))
                .key(NEUTRON_READY, pose().scale(1f).alpha(1f), Ease.IN);
        ParticleShapes.modelOn(sl, core, player, chest, 0f, 0f, 0f);
        ModelFx well = GRAVITY_WELL.color(0xE06A5CFF).scale((float) (radius * 2.0)).lifetime(ticks).fade(NEUTRON_READY, 12).spin(-5f).tag(tag);
        ParticleShapes.modelOn(sl, well, player, new Vec3(0, 0.06, 0), 0f, 0f, 0f);
        SpellFx.closingRing(sl, player.position(), radius * 1.5, 0xE0A8D8FF, NEUTRON_READY, 0);
        ParticleShapes.sphere(sl, BLUE_MOTE, chestOf(player), 3.0, 30);
        Later.run(sl, NEUTRON_READY, () -> {
            if (!player.isAlive()) return;
            SpellFx.shockRing(sl, player.position(), radius, 0xE0A8D8FF, 10, 0);
            ParticleShapes.burst(sl, ROCK, player.position().add(0, 0.2, 0), 16, 0.1, 0.3);
        });
        sound(sl, player.position(), ModSounds.STAR_DRAGON_NEUTRON_STAR, 1.6f, 1.0f);
    }

    /** Every 10 ticks of the Neutron Star: chevrons press in and the ground round it shakes. */
    public static void neutronTick(ServerLevel sl, LivingEntity player, double radius) {
        ParticleShapes.ring(sl, CHEVRON, player.position().add(0, 2.4, 0), radius * 0.7, 10, 0.0);
        ParticleShapes.ring(sl, DARK_DUST, player.position().add(0, 0.1, 0), radius * 0.5, 8, -0.05);
    }

    /** An enemy within it is crushed. */
    public static void neutronCrush(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, CHEVRON, headOf(target).add(0, 0.3, 0), 3, 0.0, 0.03);
        ParticleShapes.ring(sl, DARK_DUST, target.position().add(0, 0.1, 0), 0.4, 5, 0.06);
        if (target.tickCount % 20 < 10) sound(sl, target.position(), ModSounds.STAR_DRAGON_NEUTRON_CRUSH, 0.7f, wobble(1.0f));
    }

    public static void neutronStarEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.burst(sl, BLUE_MOTE, chestOf(player), 20, 0.05, 0.2);
    }

    // ---------- Singularity

    /** The tick the black hole opens, after the 3 second gathering. */
    public static final int SINGULARITY_GATHER = 60;

    /** The Condenser gathers for SINGULARITY_GATHER ticks: all light drains toward them, lines of it streaming in. */
    public static void singularityGather(ServerLevel sl, LivingEntity player) {
        for (int t = 0; t < SINGULARITY_GATHER; t += 2) {
            int step = t;
            Later.run(sl, t, () -> {
                if (!player.isAlive()) return;
                Vec3 to = chestOf(player);
                for (int i = 0; i < 2 + step / 15; i++) {
                    Vec3 from = within(to, 10.0, Math.random() * 6 - 3);
                    ParticleShapes.spawn(sl, STAR_MOTE.lifetime(12, 2), from, to.subtract(from).scale(1 / 12.0));
                }
                if (step % 10 == 0) SpellFx.closingRing(sl, player.position(), 8.0, 0xA06A5CFF, 10, 0);
            });
        }
        ModelFx dark = ORB.color(0xF0000000).lifetime(SINGULARITY_GATHER + 4).fade(0, 4)
                .key(0, pose().scale(0.1f))
                .key(SINGULARITY_GATHER, pose().scale(1.2f), Ease.IN);
        ParticleShapes.modelOn(sl, dark, player, new Vec3(0, player.getBbHeight() * 0.6, 0), 0f, 0f, 0f);
        sound(sl, player.position(), ModSounds.STAR_DRAGON_SINGULARITY_GATHER, 1.6f, 1.0f);
    }

    /** The black hole opens at 'center' for 'ticks', its disc turning; it drags 'radius' blocks in. */
    public static void singularity(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        ModelFx hole = BLACK_HOLE.scale(3f).lifetime(ticks).fade(0, 6).spin(6f).tag(tag)
                .key(0, pose().scale(0.05f))
                .key(8, pose().scale(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, hole, center, 0f, 0f, 0f);
        ModelFx disc = ACCRETION.scale(11f).lifetime(ticks).fade(4, 8).spin(10f).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(12, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, disc, center, randomYaw(sl), 15f, 0f);
        floor(sl, GRAVITY_WELL, center.add(0, -2.5, 0), radius, 0xC06A5CFF, ticks, -6f, tag, 0);
        ParticleShapes.burst(sl, DARK_DUST, center, 20, 0.1, 0.3);
        sound(sl, center, ModSounds.STAR_DRAGON_SINGULARITY_OPEN, 4.0f, 1.0f);
    }

    /** Every 5 ticks: matter and light spiral in from 'radius'. */
    public static void singularityTick(ServerLevel sl, Vec3 center, double radius) {
        ParticleShapes.spiral(sl, SUN_MOTE, center, 3, 16, 2.0, radius * 0.6, 0.5, -0.6);
        ParticleShapes.spiral(sl, DARK_DUST, center.add(0, -1, 0), 2, 6, 3.0, radius * 0.5, 0.3, -0.4);
    }

    /** Something swallowed (or a ray bent into it) from 'from'. */
    public static void swallowed(ServerLevel sl, Vec3 from, Vec3 center) {
        ParticleShapes.slashBetween(sl, STAR_TRAIL.color(0xC0FFE9B0).lifetime(8), from, center);
    }

    /** It closes: the disc falls in, a last flash of light let go. */
    public static void singularityEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.clearModels(sl, center.add(0, -2.45, 0), tag);
        ModelFx last = ORB.color(0xFFFFFFFF).lifetime(12)
                .key(0, pose().scale(0.2f))
                .key(4, pose().scale(4f), Ease.OUT)
                .key(12, pose().scale(5f).alpha(0f));
        ParticleShapes.model(sl, last, center, 0f, 0f, 0f);
        SpellFx.shockRing(sl, center.add(0, -2.5, 0), 16, 0xE0FFE9B0, 14, 0);
        sound(sl, center, ModSounds.STAR_DRAGON_SINGULARITY_COLLAPSE, 3.0f, 1.0f);
    }

    /** The price paid: the Condenser is spent, grey ash falling off them. */
    public static void spent(ServerLevel sl, LivingEntity player) {
        ParticleShapes.burst(sl, DARK_DUST.color(0x808A84B0), chestOf(player), 10, 0.02, 0.06);
        sound(sl, player.position(), ModSounds.STAR_DRAGON_FORM_END, 1.0f, 0.7f);
    }

    // ---------- Quantum Uncertainty

    /** Quantum Uncertainty for 'ticks': a field of interference and wandering points 'radius' round the Condenser. */
    public static void quantumUncertainty(ServerLevel sl, LivingEntity player, double radius, int ticks, String tag) {
        ModelFx field = QUANTUM_FIELD.color(0xB0D8D0FF).scale((float) (radius * 2.0)).lifetime(ticks).fade(10, 14).spin(0.5f).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(12, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, field, player, new Vec3(0, 0.06, 0), 0f, 0f, 0f);
        SpellFx.shockRing(sl, player.position(), radius, 0xA0B8B0FF, 12, 0);
        sound(sl, player.position(), ModSounds.STAR_DRAGON_QUANTUM, 1.4f, 1.0f);
    }

    /** An enemy inside flickers: two faint ghosts of where else it might be. */
    public static void uncertain(ServerLevel sl, LivingEntity target) {
        for (int i = 0; i < 2; i++) {
            Vec3 at = within(target.position(), 0.8, target.getBbHeight() * 0.5);
            ParticleShapes.spawn(sl, STILL_STAR.color(0x90B8B0FF).size(0.3f).endSize(0.1f), at, Vec3.ZERO);
        }
    }

    /** Something that teleported (or an aimed ability) slipped from where it meant to land ('from') to 'to'. */
    public static void quantumSlip(ServerLevel sl, Vec3 from, Vec3 to) {
        ParticleShapes.burst(sl, SPARKLE, from, 6, 0.02, 0.06);
        ParticleShapes.slashBetween(sl, THREAD.lifetime(8), from, to);
        ParticleShapes.burst(sl, VIOLET_MOTE, to, 8, 0.03, 0.08);
        sound(sl, to, ModSounds.STAR_DRAGON_QUANTUM_SLIP, 0.9f, wobble(1.0f));
    }

    public static void quantumEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
    }

    // =====================================================================================
    // Passives
    // =====================================================================================

    /** Stargazing (Astronomy Aficionado): standing still under open sky, faint stars drift down onto them. */
    public static void stargazing(ServerLevel sl, LivingEntity player) {
        Vec3 above = player.position().add(sl.getRandom().nextDouble() - 0.5, 3.0, sl.getRandom().nextDouble() - 0.5);
        ParticleShapes.spawn(sl, TWINKLE.gravity(0.01f).lifetime(40, 10), above, new Vec3(0, -0.04, 0));
        if (player.tickCount % 200 == 0) sound(sl, player.position(), ModSounds.STAR_DRAGON_STARGAZING, 0.4f, wobble(1.0f));
    }

    /** Spiritual Intuition (Star Worshipper): something hunts them from out of sight; a chime and ring for them alone. */
    public static void intuition(ServerPlayer viewer, Vec3 from) {
        Vec3 feet = viewer.position().add(0, 0.1, 0);
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            ParticleShapes.spawnFor(viewer, STILL_STAR.lifetime(20, 0), feet.add(Math.cos(a) * 1.2, 0, Math.sin(a) * 1.2), Vec3.ZERO);
        }
        Vec3 toward = from.subtract(viewer.position()).normalize();
        ParticleShapes.spawnFor(viewer, TWINKLE.lifetime(30, 0), feet.add(toward.scale(1.4)).add(0, 0.4, 0), Vec3.ZERO);
        soundFor(viewer, viewer.position(), ModSounds.STAR_DRAGON_INTUITION, 0.7f, 1.0f);
    }

    /** Star's Embrace (Star Sacrificer): a fall softened: a puff of starlight where they land. */
    public static void embraceLanding(ServerLevel sl, LivingEntity player) {
        ParticleShapes.ring(sl, STAR_MOTE, player.position().add(0, 0.1, 0), 0.6, 10, 0.06);
    }

    /** Sense of Passages (Navigator): a hidden enemy within 12 blocks, outlined for the Navigator alone. */
    public static void senseOfPassages(ServerPlayer viewer, LivingEntity hidden) {
        sighted(viewer, hidden);
    }

    /** Starlight Scales (Tidal Scholar): now and then, a scale glints on them as they swim. */
    public static void starlightScales(ServerLevel sl, LivingEntity player) {
        ParticleShapes.spawn(sl, SCALE.gravity(-0.01f), within(chestOf(player), 0.3, 0), Vec3.ZERO);
    }

    /** Dense Body (Heavybringer): a pull or lift of a lower Sequence fails on them: a flash of violet round the feet. */
    public static void denseBody(ServerLevel sl, LivingEntity player) {
        SpellFx.shockRing(sl, player.position(), 1.6, 0xC06A5CFF, 6, 0);
        ParticleShapes.burst(sl, CHEVRON, headOf(player).add(0, 0.3, 0), 2, 0.0, 0.02);
    }

    /** Fission Touch (Star Shepherd): a blow arcs on to the nearest enemy. */
    public static void fissionTouch(ServerLevel sl, Vec3 from, Vec3 to) {
        ParticleShapes.slashBetween(sl, BOLT.color(0xF0E8FFC0), from, to);
        ParticleShapes.burst(sl, NUCLEUS, to, 2, 0.02, 0.05);
        sound(sl, to, ModSounds.STAR_DRAGON_ARC, 0.5f, wobble(1.3f));
    }

    /** Radiance (Radiant Angel), every second: a soft glow round them out to 'radius'. */
    public static void radianceAura(ServerLevel sl, LivingEntity player, double radius) {
        ParticleShapes.ring(sl, SUN_MOTE, player.position().add(0, 0.8, 0), radius * 0.3, 6, 0.06);
    }

    /** Gravity Authority (Star Dragon): a projectile slowed within 6 blocks: a ripple round it. */
    public static void gravityAuthority(ServerLevel sl, Vec3 at) {
        ParticleShapes.ring(sl, VIOLET_MOTE.lifetime(8, 2), at, 0.3, 5, 0.02);
    }

    /** Weight and Density (Condenser): their steps shake the ground. */
    public static void condenserStep(ServerLevel sl, LivingEntity player) {
        SpellFx.shockRing(sl, player.position(), 2.4, 0x806A5CFF, 8, 0);
        ParticleShapes.ring(sl, DARK_DUST, player.position().add(0, 0.1, 0), 0.5, 6, 0.08);
        sound(sl, player.position(), ModSounds.STAR_DRAGON_DENSE_STEP, 0.6f, wobble(1.0f));
    }

    // =====================================================================================
    // /stardragonfx: every display, to look at in game
    // =====================================================================================

    @FunctionalInterface
    private interface Show {
        void play(ServerLevel sl, Player viewer, LivingEntity target, Vec3 spot);
    }

    /** onViewer = it is shown on the viewer (worn), not on a spot or a target. */
    private record Case(String name, int ticks, boolean onViewer, Show show) {}

    private static String tagFor(String name) {
        return "sd_fx_" + name + "_" + UUID.randomUUID();
    }

    private static Vec3 aimAt(LivingEntity v, LivingEntity t, Vec3 spot) {
        return t == v ? spot.add(0, 1, 0) : chestOf(t);
    }

    private static final List<Case> CASES = List.of(
            new Case("star_chart", 90, false, (sl, v, t, spot) -> {
                if (t == v) return;
                starChart(sl, v, t);
                if (v instanceof ServerPlayer viewer) {
                    for (int i = 1; i < 8; i++) Later.run(sl, CHART_LANDS + i * 10, () -> charted(viewer, t));
                    Later.run(sl, CHART_LANDS + 80, () -> chartedEnd(viewer, t));
                }
                Later.run(sl, 60, () -> calculatedHit(sl, t));
            }),
            new Case("fate_glimpse", 80, true, (sl, v, t, spot) -> {
                String tag = tagFor("glimpse");
                fateGlimpse(sl, v, 70, tag);
                Later.run(sl, 25, () -> glimpseDodge(sl, v, v.position().add(Spells.rightOf(v).scale(-1.2)), v.position()));
                Later.run(sl, 70, () -> fateGlimpseEnd(sl, v, tag));
            }),
            new Case("gravity_pressed", 110, false, (sl, v, t, spot) -> {
                String tag = tagFor("press");
                gravityAbnormality(sl, spot, 4, 100, false, tag);
                for (int i = 1; i < 5; i++) Later.run(sl, GRAVITY_RISEN + i * 20, () -> {
                    gravityTick(sl, spot, 4, false);
                    if (t != v) pressed(sl, t);
                });
                if (t != v) Later.run(sl, 90, () -> collapse(sl, t));
                Later.run(sl, 100, () -> gravityEnd(sl, spot, 4, false, tag));
            }),
            new Case("gravity_lifted", 110, false, (sl, v, t, spot) -> {
                String tag = tagFor("lift");
                gravityAbnormality(sl, spot, 4, 100, true, tag);
                for (int i = 1; i < 5; i++) Later.run(sl, GRAVITY_RISEN + i * 20, () -> gravityTick(sl, spot, 4, true));
                if (t != v) {
                    Later.run(sl, GRAVITY_RISEN, () -> lifted(sl, t, 3, 80, tagFor("lifted")));
                    Later.run(sl, 95, () -> slammed(sl, t, t.position()));
                }
                Later.run(sl, 100, () -> gravityEnd(sl, spot, 4, true, tag));
            }),
            new Case("lightened", 70, true, (sl, v, t, spot) -> {
                String tag = tagFor("light");
                lightened(sl, v, 60, tag);
                airWave(sl, v, 4);
                Later.run(sl, 60, () -> lightenedEnd(sl, v, tag));
            }),
            new Case("electromagnetic_attraction", 50, false, (sl, v, t, spot) -> {
                if (t == v) return;
                magnetPull(sl, v, t, 16);
                Later.run(sl, 16, () -> hammerBlow(sl, v, t));
            }),
            new Case("cosmic_void", 100, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("void");
                cosmicVoid(sl, t, 2, 80, VoidDepth.HALF_REAL, tag);
                Later.run(sl, 40, () -> voidManifest(sl, t));
                Later.run(sl, 80, () -> voidEnd(sl, t, tag));
            }),
            new Case("cosmic_void_frozen", 110, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("void");
                cosmicVoid(sl, t, 5, 90, VoidDepth.ABSOLUTE_ZERO, tag);
                for (int i = 1; i < 3; i++) Later.run(sl, i * 40, () -> voidFreeze(sl, t, 5));
                Later.run(sl, 90, () -> voidEnd(sl, t, tag));
            }),
            new Case("weakening_ray", 80, false, (sl, v, t, spot) -> {
                Vec3 from = v.getEyePosition().add(0, -0.3, 0);
                Vec3 to = aimAt(v, t, spot);
                rayWarning(sl, from, to, RayKind.WEAKENING);
                Later.run(sl, RAY_WARNING, () -> {
                    rayFire(sl, from, to, RayKind.WEAKENING, 0.25f);
                    if (t != v) {
                        String tag = tagFor("weak");
                        weakened(sl, t, 1, 60, tag);
                        Later.run(sl, 20, () -> melting(sl, t, 40, tagFor("melt")));
                    }
                });
            }),
            new Case("sunlight_ray", 50, false, (sl, v, t, spot) -> {
                sunlightCharge(sl, v);
                Later.run(sl, SUNLIGHT_FIRES, () -> {
                    Vec3 from = v.getEyePosition().add(v.getLookAngle().scale(1.0)).add(0, -0.35, 0);
                    rayFire(sl, from, from.add(aimAt(v, t, spot).subtract(from).normalize().scale(24)), RayKind.SUNLIGHT, 2f);
                    if (t != v) sunburnt(sl, t);
                });
            }),
            new Case("void_passage", 50, true, (sl, v, t, spot) -> {
                Vec3 to = v.position().add(Spells.flatLook(v).scale(8));
                voidPassage(sl, v, v.position(), to);
                Later.run(sl, PASSAGE_ARRIVES, () -> unnoticed(sl, v, 30, tagFor("unnoticed")));
            }),
            new Case("piercing_ray", 50, false, (sl, v, t, spot) -> {
                Vec3 to = v.getEyePosition().add(aimAt(v, t, spot).subtract(v.getEyePosition()).normalize().scale(28));
                piercingRay(sl, v, to);
                if (t != v) dimmed(sl, t, 40, tagFor("dimmed"));
            }),
            new Case("tidal_surge", 50, true, (sl, v, t, spot) -> {
                tidalSurge(sl, v);
                if (t != v) Later.run(sl, SURGE_TICKS / 3, () -> {
                    surgeHit(sl, t);
                    heavy(sl, t, 60, tagFor("heavy"));
                });
            }),
            new Case("scaled_form", 120, true, (sl, v, t, spot) -> {
                String tag = tagFor("scales");
                scaledForm(sl, v, 110, false, tag);
                if (t != v) Later.run(sl, 50, () -> scaledBlow(sl, t));
                Later.run(sl, 110, () -> scaledFormEnd(sl, v, tag));
            }),
            new Case("heavy_body", 120, true, (sl, v, t, spot) -> {
                String tag = tagFor("heavy_body");
                scaledForm(sl, v, 110, true, tag);
                for (int i = 1; i < 5; i++) Later.run(sl, SCALES_ON + i * 20, () -> heavyBodyTick(sl, v));
                Later.run(sl, 110, () -> scaledFormEnd(sl, v, tag));
            }),
            new Case("heavybringing", 130, true, (sl, v, t, spot) -> {
                String tag = tagFor("heavybringing");
                heavybringing(sl, v, 16, 120, tag);
                for (int i = 1; i < 12; i++) Later.run(sl, i * 10, () -> heavybringingTick(sl, v, 16));
                Later.run(sl, 40, () -> bent(sl, chestOf(v).add(Spells.flatLook(v).scale(8)).add(4, 1, 0), chestOf(v)));
                if (t != v) Later.run(sl, 20, () -> heavy(sl, t, 80, tagFor("heavy")));
                Later.run(sl, 120, () -> heavybringingEnd(sl, v, tag));
            }),
            new Case("fate_convergence", 130, true, (sl, v, t, spot) -> {
                String tag = tagFor("fate");
                fateConvergence(sl, v, 12, 120, tag);
                if (t != v) {
                    Later.run(sl, 16, () -> misfortune(sl, t, 50, tagFor("misfortune")));
                    Later.run(sl, 40, () -> stumble(sl, t));
                    Later.run(sl, 70, () -> narrowed(sl, t, 40, tagFor("narrowed")));
                }
                Later.run(sl, 55, () -> misfire(sl, chestOf(v).add(Spells.flatLook(v).scale(4))));
                Later.run(sl, 30, () -> luck(sl, v));
                Later.run(sl, 120, () -> fateConvergenceEnd(sl, v, tag));
            }),
            new Case("heavy_sphere", 110, false, (sl, v, t, spot) -> {
                String tag = tagFor("sphere");
                Vec3 center = (t == v ? spot : t.position()).add(0, 1, 0);
                heavySphere(sl, center, 3, 100, tag);
                Later.run(sl, 40, () -> sphereBlock(sl, center.add(Spells.flatLook(v).scale(-3))));
                Later.run(sl, 100, () -> heavySphereEnd(sl, center, tag));
            }),
            new Case("series_of_rays", 70, false, (sl, v, t, spot) -> {
                seriesOfRays(sl, v);
                Vec3 to = aimAt(v, t, spot);
                for (int i = 0; i < 6; i++) {
                    int k = i;
                    Later.run(sl, SERIES_FIRES + i * SERIES_STEP, () -> {
                        Vec3 from = handOf(v, sl);
                        Vec3 spread = Spells.rightOf(v).scale((k - 2.5) * 0.25);
                        rayFire(sl, from, to.add(spread), RayKind.WEAKENING, 0.2f);
                        if (t != v && k % 2 == 0) weakened(sl, t, k / 2 + 1, 40, "sd_fx_series");
                    });
                }
                if (t != v) Later.run(sl, SERIES_FIRES + 6 * SERIES_STEP, () -> melting(sl, t, 30, tagFor("melt")));
            }),
            new Case("remote_sight", 80, true, (sl, v, t, spot) -> {
                String tag = tagFor("sight");
                remoteSight(sl, v, 70, tag);
                if (v instanceof ServerPlayer viewer) {
                    for (int i = 1; i < 4; i++) Later.run(sl, i * 20, () -> {
                        if (t != v) sighted(viewer, t);
                        passage(viewer, v.position().add(0, 1, 0), spot.add(0, 1, 0));
                    });
                }
                Later.run(sl, 70, () -> remoteSightEnd(sl, v, tag));
            }),
            new Case("star_grazing", 120, false, (sl, v, t, spot) -> {
                starGrazing(sl, spot, 4);
                String tag = tagFor("grazed");
                Vec3 above = spot.add(0, 6, 0);
                Later.run(sl, GRAZE_LANDS + 10, () -> grazedStar(sl, above, 80, tag));
                Later.run(sl, GRAZE_LANDS + 40, () -> grazedStarFeeds(sl, above, v));
                Later.run(sl, GRAZE_LANDS + 90, () -> grazedStarEnd(sl, above, tag));
            }),
            new Case("star_grazing_three", 70, false, (sl, v, t, spot) -> {
                for (int i = 0; i < 3; i++) {
                    Later.run(sl, i * 10, () -> starGrazing(sl, within(spot, 3, 0), 3));
                }
            }),
            new Case("fission_pulse", 130, false, (sl, v, t, spot) -> {
                if (t == v) return;
                fissionPulse(sl, v, t);
                Later.run(sl, FISSION_SPLITS, () -> {
                    Vec3 at = chestOf(t), ground = t.position();
                    fissionBurst(sl, at, 6);
                    irradiated(sl, t, 60, tagFor("irradiated"));
                    String zone = tagFor("zone");
                    radiationZone(sl, ground, 3, 80, zone);
                    Later.run(sl, 80, () -> radiationZoneEnd(sl, ground, zone));
                });
            }),
            new Case("em_storm", 110, false, (sl, v, t, spot) -> {
                String tag = tagFor("storm");
                emStorm(sl, spot, 5, 100, tag);
                for (int i = 0; i < 8; i++) {
                    Later.run(sl, 10 + i * 10, () -> emArc(sl, within(spot, 4, 3), t == v ? within(spot, 4, 0.2) : chestOf(t)));
                }
                Later.run(sl, 50, () -> stormStop(sl, spot.add(0, 1.5, 0)));
                Later.run(sl, 100, () -> emStormEnd(sl, spot, tag));
            }),
            new Case("fusion_cluster", 80, true, (sl, v, t, spot) -> {
                String tag = tagFor("fusion");
                int count = 3;
                fusionCluster(sl, v, count, 60, tag);
                for (int age = 0; age < 40; age++) {
                    int a = age;
                    Later.run(sl, age, () -> fusionOrbit(sl, v, count, tag, a));
                }
                for (int i = 0; i < count; i++) {
                    int k = i;
                    Later.run(sl, 40 + i * 3, () -> {
                        Vec3 from = fusionLaunch(sl, v, k, count, tag, 40 + k * 3);
                        Vec3 to = aimAt(v, t, spot).add(Spells.rightOf(v).scale((k - 1) * 2.0));
                        Vec3 way = to.subtract(from);
                        int flight = Math.max(4, (int) (way.length() / 1.5));
                        ParticleShapes.modelAlong(sl, FUSION_FLYING.lifetime(flight + 1).key(flight, pose().up((float) way.length())), from, way);
                        Later.run(sl, flight, () -> fusionBurst(sl, to, 3, k == 2));
                    });
                }
            }),
            new Case("radiant_angel", 140, true, (sl, v, t, spot) -> {
                String tag = tagFor("angel");
                radiantAngel(sl, v, 120, tag);
                for (int i = 1; i < 10; i++) Later.run(sl, ANGEL_READY + i * 10, () -> {
                    radiance(sl, v, 12);
                    if (t != v) radiantBurn(sl, t);
                });
                Later.run(sl, 120, () -> {
                    radiantAngelEnd(sl, v, tag);
                    formWeakness(sl, v, 20, tagFor("weak"));
                });
            }),
            new Case("time_dilation", 130, false, (sl, v, t, spot) -> {
                String tag = tagFor("time");
                timeDilation(sl, spot, 6, 120, tag);
                if (t != v) Later.run(sl, DILATION_RISEN, () -> dilated(sl, t, 100, tagFor("dilated")));
                for (int i = 0; i < 5; i++) Later.run(sl, 20 + i * 15, () -> timeHang(sl, spot.add(1, 1.6, 0)));
                Later.run(sl, 120, () -> timeDilationEnd(sl, spot, 6, tag));
            }),
            new Case("planetfall", 170, false, (sl, v, t, spot) -> {
                Vec3 ground = v.position().add(Spells.flatLook(v).scale(20));
                planetfall(sl, ground, 12);
                String tag = tagFor("cracks");
                Later.run(sl, PLANET_LANDS + 4, () -> lightCracks(sl, ground, 12, 80, tag));
                Later.run(sl, PLANET_LANDS + 84, () -> lightCracksEnd(sl, ground, tag));
            }),
            new Case("star_dragon_form", 200, true, (sl, v, t, spot) -> {
                String tag = tagFor("dragon");
                starDragonForm(sl, v, 180, tag);
                Later.run(sl, DRAGON_READY, () -> dragonAsteroid(sl, v, 150, tag, DRAGON_READY));
                for (int age = DRAGON_READY; age < 180; age++) {
                    int a = age;
                    Later.run(sl, age, () -> dragonAsteroidOrbit(sl, v, tag, a));
                }
                Later.run(sl, 110, () -> {
                    Vec3 from = dragonShot(sl, v, tag, 110);
                    Vec3 to = aimAt(v, t, spot);
                    Vec3 way = to.subtract(from);
                    int flight = Math.max(4, (int) (way.length() / 1.6));
                    ParticleShapes.modelAlong(sl, ASTEROID_FLYING.lifetime(flight + 1).key(flight, pose().up((float) way.length())), from, way);
                    Later.run(sl, flight, () -> dragonShotHit(sl, to));
                });
                Later.run(sl, 140, () -> dragonAsteroidBack(sl, v, 40, tag, 140));
                Later.run(sl, 150, () -> rayFire(sl, v.getEyePosition(), v.getEyePosition().add(v.getLookAngle().scale(24)), RayKind.DRAGON, 0.8f));
                Later.run(sl, 180, () -> starDragonFormEnd(sl, v, tag));
            }),
            new Case("neutron_star", 140, true, (sl, v, t, spot) -> {
                String tag = tagFor("neutron");
                neutronStar(sl, v, 6, 120, tag);
                for (int i = 1; i < 10; i++) Later.run(sl, NEUTRON_READY + i * 10, () -> {
                    neutronTick(sl, v, 6);
                    if (t != v) neutronCrush(sl, t);
                });
                Later.run(sl, 120, () -> neutronStarEnd(sl, v, tag));
            }),
            new Case("singularity", 260, false, (sl, v, t, spot) -> {
                Vec3 center = spot.add(Spells.flatLook(v).scale(4)).add(0, 3, 0);
                singularityGather(sl, v);
                String tag = tagFor("hole");
                Later.run(sl, SINGULARITY_GATHER, () -> singularity(sl, center, 16, 160, tag));
                for (int i = 1; i < 32; i++) Later.run(sl, SINGULARITY_GATHER + i * 5, () -> singularityTick(sl, center, 16));
                if (t != v) Later.run(sl, SINGULARITY_GATHER + 40, () -> swallowed(sl, chestOf(t), center));
                Later.run(sl, SINGULARITY_GATHER + 160, () -> {
                    singularityEnd(sl, center, tag);
                    spent(sl, v);
                });
            }),
            new Case("quantum_uncertainty", 130, true, (sl, v, t, spot) -> {
                String tag = tagFor("quantum");
                quantumUncertainty(sl, v, 10, 120, tag);
                if (t != v) for (int i = 1; i < 10; i++) Later.run(sl, i * 10, () -> uncertain(sl, t));
                Later.run(sl, 60, () -> quantumSlip(sl, spot.add(0, 1, 0), within(spot, 6, 1)));
                Later.run(sl, 120, () -> quantumEnd(sl, v, tag));
            }),
            new Case("heaviness", 70, false, (sl, v, t, spot) -> {
                if (t != v) heavy(sl, t, 60, tagFor("heavy"));
            }),
            new Case("passives", 100, true, (sl, v, t, spot) -> {
                for (int i = 0; i < 6; i++) Later.run(sl, i * 6, () -> stargazing(sl, v));
                if (v instanceof ServerPlayer viewer) {
                    Later.run(sl, 10, () -> intuition(viewer, spot));
                    if (t != v) Later.run(sl, 30, () -> senseOfPassages(viewer, t));
                }
                Later.run(sl, 20, () -> embraceLanding(sl, v));
                Later.run(sl, 35, () -> starlightScales(sl, v));
                Later.run(sl, 45, () -> denseBody(sl, v));
                if (t != v) Later.run(sl, 55, () -> fissionTouch(sl, chestOf(v), chestOf(t)));
                Later.run(sl, 65, () -> radianceAura(sl, v, 8));
                Later.run(sl, 75, () -> gravityAuthority(sl, chestOf(v).add(Spells.flatLook(v).scale(3))));
                Later.run(sl, 85, () -> condenserStep(sl, v));
            }),
            new Case("statuses", 100, true, (sl, v, t, spot) -> {
                for (Status s : Status.values()) status(v, s, 90);
            }));

    /** The names /stardragonfx knows. */
    public static List<String> names() {
        List<String> names = new ArrayList<>();
        for (Case c : CASES) names.add(c.name());
        return names;
    }

    /** The nearest living thing within 12 blocks that is not the viewer, or the viewer. */
    private static LivingEntity nearestTarget(ServerLevel sl, Player viewer) {
        LivingEntity best = viewer;
        double bestDistance = 12 * 12;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, viewer.getBoundingBox().inflate(12))) {
            if (e == viewer || !e.isAlive()) continue;
            double d = e.distanceToSqr(viewer);
            if (d < bestDistance) {
                bestDistance = d;
                best = e;
            }
        }
        return best;
    }

    /** One display, 6 blocks in front of the viewer. False if there is none of that name. */
    public static boolean show(ServerLevel sl, Player viewer, String name) {
        for (Case c : CASES) {
            if (!c.name().equals(name)) continue;
            Vec3 spot = viewer.position().add(ahead(viewer.getYRot()).scale(6));
            c.show().play(sl, viewer, nearestTarget(sl, viewer), spot);
            return true;
        }
        return false;
    }

    /** Every display one after the other (those on a spot in a ring 8 blocks out), 'rounds' times over. */
    public static void gallery(ServerLevel sl, Player viewer, int rounds) {
        List<Case> placed = new ArrayList<>();
        for (Case c : CASES) if (!c.onViewer()) placed.add(c);
        int at = 0;
        for (int round = 0; round < rounds; round++) {
            for (Case c : CASES) {
                int place = placed.indexOf(c);
                Later.run(sl, at, () -> {
                    if (!viewer.isAlive() || viewer.level() != sl) return;
                    float out = viewer.getYRot() + Math.max(0, place) * 360f / Math.max(1, placed.size());
                    Vec3 spot = viewer.position().add(ahead(out).scale(8));
                    c.show().play(sl, viewer, nearestTarget(sl, viewer), spot);
                });
                at += c.ticks() + 10;
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("stardragonfx")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("all")
                        .executes(ctx -> showAll(ctx.getSource(), 1))
                        .then(Commands.argument("rounds", IntegerArgumentType.integer(1, 10))
                                .executes(ctx -> showAll(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "rounds")))))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(names(), builder))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "name");
                            if (!show(ctx.getSource().getLevel(), player, name)) {
                                ctx.getSource().sendFailure(Component.literal("No display called " + name + ". There are: " + String.join(", ", names())));
                                return 0;
                            }
                            return 1;
                        })));
    }

    private static int showAll(CommandSourceStack source, int rounds) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        gallery(source.getLevel(), source.getPlayerOrException(), rounds);
        return 1;
    }
}
