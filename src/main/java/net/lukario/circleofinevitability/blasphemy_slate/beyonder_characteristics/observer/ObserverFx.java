package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * How the Observer (Sublunary Eye) pathway looks and sounds: every wheel entry on the seven keys, the two rules that
 * carry it (Observation and Paintings), the growth at higher Sequences, the marks it leaves on others, the statuses,
 * the two Mythical Creature Forms and the passives that show. Visual only: nothing here hurts, holds, hides, moves or
 * heals anyone. The ability code does that, at the ticks named by the constants below (..._READY, ..._LANDS...), with
 * Later.run. See docs/pathways/observer/OBSERVER.md: the design, and under "Looks, sounds and pictures" this class entry by entry.
 *
 * ---------- See them in game ----------
 *   /observerfx all                every display one after another (/observerfx all 2 = twice over)
 *   /observerfx giants_fist        just that one (press TAB after /observerfx for the names)
 * Displays that need a target use the nearest mob within 12 blocks, or you when there is none (ObserverFxGallery).
 *
 * ---------- What is where ----------
 *   Shared        Observation: observed(viewer, target, level, ticks), observationRises(viewer, target, level),
 *                 staring(viewer, target), notWatchedEnough(player). Paintings: paint(...), hang(...),
 *                 paintingStare(...), paintingSeen(...), paintingEnd(...), foldPainting(...).
 *   Key 1         totemRitual / totemRitualCancel / totem / territoryPulse / territoryShell / territoryShellBlock /
 *   Territory     intruder / territoryEnd; borrow(..., Craft.X) / gale / earthSpikes / waterLash / thornSnare /
 *                 beastCall; inkServantsArrive / servantStrike / inkServantsLeave (the servants themselves are
 *                 INK_SERVANT under FollowerSummon.hover at servantOffset(i), or inkServantLooks); paintingWorld / worldBarrierHit /
 *                 paintingWorldEnd
 *   Key 2 Sight   trueSight / trueSightSees / trueSightThread / trueSightWard / trueSightTrap / trueSightEnd;
 *                 manyAngles / manyAnglesLine / manyAnglesEnd; readIntent
 *   Key 3 Canvas  riot / madness / madnessCollapse / riotEnd; itching / itchingEnd; goldenSun / sunWarm / sunBurn /
 *                 exorcise / goldenSunEnd; boatInAStorm / stormWave / boatInAStormEnd; selfPortraitLook /
 *                 portraitEcho; giantsFist (lands at GIANT_FIST_LANDS)
 *   Key 4 Fiction paintedMark / markDrive / paintedMarkEnd; hideInPainting / hideInPaintingEnd; paintedFace /
 *                 paintedFaceEnd; manifest(...Figure.X, share...) / knightStrike / archerShot / manifestFade;
 *                 fantasyDouble / fantasyLink / fantasyDoubleEnd
 *   Key 5         phaseIn / phaseTick / phaseOut; pixieWings / pixieWingsKeep / pixieWingsEnd; paintedLightning /
 *   Dimension     boltJump; passage (out at PASSAGE_ARRIVES); hostelLodge / hostelRelease; shadowForm /
 *                 shadowAfterimage / ascension / shadowFormEnd / shadowWeakness; observerForm / observerFormEnd
 *   Key 6 Strings tautChord (played at TAUT_PLAYS) / tautChordPlay; resonance / resonanceAdd / resonanceBurst;
 *                 harmony / harmonyArmed / harmonySpent; dissonance / dissonant; shadowFromAbove /
 *                 shadowBladesRise; foldSpace (arrives at FOLD_ARRIVES) / foldStruck
 *   Key 7         foregoneLoss / foregoneFall / foregoneSpared; foregoneSurvival / wardSpent; revivalRitual /
 *   Sublunary     revivalArmed / higherRevival; bloom / bloomOn / bloomEnd; fallSilent / silenced / woken /
 *                 fallSilentEnd; erase / erasedReturn / eraseArea / unmade
 *   Passives      spiritChime, featherfall, painterEye, prayer, eyesEverywhere, sublunarySight, foreseen
 *   Statuses      status(player, Status.X, ticks), statusForever, statusEnd (pictures in textures/gui/observer/)
 *   Wheel icons   WHEEL_TOTEM_TERRITORY ... WHEEL_ERASE (textures/gui/ability_menus/observer/)
 *
 * Every timed look takes a 'tag' (make one per cast: tagFor("name")) and has an ...End that takes it away early.
 * Looks that show only to one player take a ServerPlayer viewer (they go through walls for that player alone).
 *
 * The models are in models/model_fx/observer/ (tools/model_gen/observer/: models.py, paintings.py, pictures.py; each
 * JSON's "credit" gives its size and pivot), the particles in textures/particle/fx/observer/ (particles.py), the
 * pictures in textures/gui/observer/ and textures/gui/ability_menus/observer/ (icons.py) and the sounds in
 * sounds/observer/ (sounds.py, ModSounds.OBSERVER). Run tools/model_gen/observer/generate.py to remake them all.
 */
public final class ObserverFx {

    private ObserverFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // Colors (ARGB)
    // =====================================================================================

    /** The Pathway's own color. */
    public static final int PATHWAY_COLOR = 0xFF6C5CE7;
    public static final int INK = 0xFF14100E;
    public static final int PARCHMENT = 0xFFECE2C8;
    public static final int EARTH = 0xFF7A5030;
    public static final int MOSS = 0xFF6A9A3A;
    public static final int MOSS_LIGHT = 0xFFB8E07A;
    public static final int PIXIE_TEAL = 0xFF8CD8D0;
    public static final int SHADOW_VIOLET = 0xFF1A102C;
    public static final int VIOLET = 0xFF6C5CE7;
    public static final int VIOLET_LIGHT = 0xFFBEB4FF;
    public static final int SUBLUNARY_GOLD = 0xFFFFE9B0;
    public static final int GOLD = 0xFFFFD66E;
    public static final int STRING_GOLD = 0xFFFFE6A0;
    public static final int ENDING_RED = 0xFFC83030;
    public static final int SILENT_GREY = 0xFF8A8A96;
    /** The colours of Riot of Colours and of every Painting's paint. */
    public static final int[] HUES = {0xFFFF2A6A, 0xFFFFD21A, 0xFF1AE0FF, 0xFF7A2AFF, 0xFF2AFF6A, 0xFFFF7A1A};
    /** Observation's eye, by level (1 watched, 2 studied, 3 seen through). */
    public static final int[] OBSERVATION_COLORS = {0xFFFFFFFF, 0xFFD8D4F0, 0xFF8CD8D0, 0xFF8A7AFF};

    /** How long something "until ended" is shown at most (an hour): show it again after that. */
    public static final int FOREVER = 72000;

    // =====================================================================================
    // The particles everything shares
    // =====================================================================================

    private static final ParticleFx BRUSH = ParticleFx.of("fx/observer/brush_0", "fx/observer/brush_1", "fx/observer/brush_2")
            .color(0xFF14100E).endColor(0x0014100E)
            .size(0.22f).endSize(0.16f).sizeRandom(0.4f)
            .lifetime(16, 8).gravity(0f).friction(0.85f).randomRotation();
    private static final ParticleFx SPLAT = ParticleFx.of("fx/observer/splat")
            .color(0xFFFFFFFF).endColor(0x00FFFFFF)
            .size(0.14f).endSize(0.1f).sizeRandom(0.5f)
            .lifetime(18, 8).gravity(0.04f).friction(0.94f).glow().randomRotation().collide();
    private static final ParticleFx INK_BLOT = ParticleFx.of("fx/observer/blot")
            .color(0xF014100E).endColor(0x0014100E)
            .size(0.16f).endSize(0.24f).sizeRandom(0.4f)
            .lifetime(22, 8).gravity(0.02f).friction(0.9f).randomRotation();
    private static final ParticleFx INK_SMOKE = ParticleFx.of("fx/smoke")
            .color(0xC014100E).endColor(0x00141020)
            .size(0.3f).endSize(0.8f).sizeRandom(0.3f)
            .lifetime(26, 10).gravity(-0.005f).friction(0.9f).spin(2f).randomRotation();
    private static final ParticleFx EYE = ParticleFx.of("fx/observer/eye_shut", "fx/observer/eye_open", "fx/observer/eye_open",
                    "fx/observer/eye_open", "fx/observer/eye_shut")
            .color(0xFFFFFFFF).endColor(0x80BEB4FF)
            .size(0.12f).endSize(0.12f)
            .lifetime(20, 0).gravity(-0.01f).friction(0.9f).glow();
    private static final ParticleFx GLYPH = ParticleFx.of("fx/observer/glyph")
            .color(0xFFB8E07A).endColor(0x006A9A3A)
            .size(0.1f).endSize(0.08f).sizeRandom(0.3f)
            .lifetime(24, 8).gravity(-0.02f).friction(0.92f).glow().spin(3f).randomRotation();
    private static final ParticleFx MOSS_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFB8E07A).endColor(0x002E4A1A)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(24, 10).gravity(-0.015f).friction(0.92f).glow();
    private static final ParticleFx LEAF = ParticleFx.of("fx/leaf")
            .color(0xFF6A9A3A).endColor(0x803A5A22)
            .size(0.08f).endSize(0.07f).sizeRandom(0.4f)
            .lifetime(30, 10).gravity(0.02f).friction(0.95f).spin(8f).randomRotation().collide();
    private static final ParticleFx FEATHER = ParticleFx.of("fx/feather_light")
            .color(0xFFE8DCC0).endColor(0x808A5A30)
            .size(0.1f).endSize(0.09f).sizeRandom(0.3f)
            .lifetime(40, 10).gravity(0.01f).friction(0.96f).spin(4f).randomRotation();
    private static final ParticleFx DUST = ParticleFx.of("fx/smoke")
            .color(0xC08A7A66).endColor(0x00403630)
            .size(0.4f).endSize(1.3f).sizeRandom(0.4f)
            .lifetime(36, 14).gravity(-0.004f).friction(0.9f).spin(1.5f).randomRotation();
    private static final ParticleFx ROCK = ParticleFx.of("fx/rock")
            .color(0xFF7A6A5A).endColor(0xC0403630)
            .size(0.09f).endSize(0.08f).sizeRandom(0.5f)
            .lifetime(24, 10).gravity(0.06f).friction(0.96f).spin(12f).randomRotation().collide();
    private static final ParticleFx DROP = ParticleFx.of("fx/droplet")
            .color(0xFFC8F0F0).endColor(0x802A6A78)
            .size(0.07f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(18, 8).gravity(0.06f).friction(0.97f).collide();
    private static final ParticleFx WIND = ParticleFx.of("fx/smoke")
            .color(0x60E8F4FF).endColor(0x00A8C8E0)
            .size(0.3f).endSize(0.9f).sizeRandom(0.3f)
            .lifetime(14, 6).gravity(0f).friction(0.96f).randomRotation();
    private static final ParticleFx TEAL_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFE8FFFC).endColor(0x002A6A70)
            .size(0.07f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(20, 8).gravity(-0.01f).friction(0.92f).glow();
    private static final ParticleFx GLINT = ParticleFx.of("fx/observer/wing_glint")
            .color(0xFFE8FFFC).endColor(0x008CD8D0)
            .size(0.08f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(14, 6).gravity(0f).friction(0.9f).glow().randomRotation();
    private static final ParticleFx VIOLET_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFD8CCFF).endColor(0x002A1A4A)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(-0.01f).friction(0.92f).glow();
    private static final ParticleFx SHARD = ParticleFx.of("fx/observer/shard")
            .color(0xFFD8CCFF).endColor(0x006C5CE7)
            .size(0.12f).endSize(0.06f).sizeRandom(0.4f)
            .lifetime(18, 8).gravity(0f).friction(0.9f).glow().spin(6f).randomRotation();
    private static final ParticleFx DARK = ParticleFx.of("fx/smoke")
            .color(0xC01A102C).endColor(0x00060410)
            .size(0.3f).endSize(0.8f).sizeRandom(0.3f)
            .lifetime(26, 10).gravity(0f).friction(0.9f).spin(2f).randomRotation();
    private static final ParticleFx FOLD = ParticleFx.of("fx/observer/fold")
            .color(0xFFF4F0FF).endColor(0x006C5CE7)
            .size(0.12f).endSize(0.08f).sizeRandom(0.3f)
            .lifetime(16, 6).gravity(0f).friction(0.88f).glow().spin(10f).randomRotation();
    private static final ParticleFx GOLD_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFFF4D0).endColor(0x00FFA030)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(-0.02f).friction(0.92f).glow();
    private static final ParticleFx SPARKLE = ParticleFx.of("fx/sparkle")
            .color(0xFFFFFFFF).endColor(0x00FFE9B0)
            .size(0.07f).endSize(0.02f).sizeRandom(0.5f)
            .lifetime(16, 8).gravity(-0.005f).friction(0.9f).glow().randomRotation();
    private static final ParticleFx NOTES = ParticleFx.of("fx/observer/notes", "fx/note")
            .color(0xFFFFE6A0).endColor(0x00FFC860)
            .size(0.12f).endSize(0.1f).sizeRandom(0.3f)
            .lifetime(26, 8).gravity(-0.03f).friction(0.94f).glow();
    private static final ParticleFx BROKEN_NOTE = ParticleFx.of("fx/observer/broken_note")
            .color(0xFFBEB4FF).endColor(0x006C5CE7)
            .size(0.12f).endSize(0.1f).sizeRandom(0.3f)
            .lifetime(22, 8).gravity(0.02f).friction(0.94f).glow().spin(9f).randomRotation();
    private static final ParticleFx PETAL = ParticleFx.of("fx/petal")
            .color(0xFFFFF8E8).endColor(0x80FFC850)
            .size(0.08f).endSize(0.07f).sizeRandom(0.4f)
            .lifetime(40, 12).gravity(0.01f).friction(0.96f).spin(6f).randomRotation().glow();
    private static final ParticleFx BUD = ParticleFx.of("fx/observer/bud")
            .color(0xFFFFF4D0).endColor(0x00FFC850)
            .size(0.1f).endSize(0.08f)
            .lifetime(20, 6).gravity(-0.01f).friction(0.9f).glow();
    private static final ParticleFx GREY_MOTE = ParticleFx.of("fx/glow")
            .color(0xC0A0A0AA).endColor(0x00404048)
            .size(0.07f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(28, 10).gravity(0.01f).friction(0.94f);
    private static final ParticleFx CRUMB = ParticleFx.of("fx/observer/crumb")
            .color(0xFFFFFFFF).endColor(0x00D8D4F0)
            .size(0.07f).endSize(0.05f).sizeRandom(0.5f)
            .lifetime(20, 8).gravity(0.04f).friction(0.94f).spin(10f).randomRotation();
    private static final ParticleFx PAGE = ParticleFx.of("fx/page")
            .color(0xFFECE2C8).endColor(0x80B8A888)
            .size(0.14f).endSize(0.12f).sizeRandom(0.3f)
            .lifetime(30, 10).gravity(0.005f).friction(0.95f).spin(5f).randomRotation();
    private static final ParticleFx WRITING = ParticleFx.of("fx/text_line")
            .color(0xFFFFFFFF).endColor(0x00FFFFFF)
            .size(0.12f).endSize(0.1f)
            .lifetime(24, 6).gravity(-0.01f).friction(0.9f).glow();
    private static final ParticleFx SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFFFFFF).endColor(0x00BEB4FF)
            .size(0.06f).endSize(0.02f)
            .lifetime(8, 6).gravity(0.02f).friction(0.86f).glow();
    private static final ParticleFx MARK_DOT = ParticleFx.of("fx/glow")
            .color(0xFFFFFFFF).endColor(0x00FFFFFF)
            .size(0.05f).endSize(0.04f).lifetime(8, 0).gravity(0f).friction(0f).glow();

    // =====================================================================================
    // Lines
    // =====================================================================================

    private static final SlashFx BRUSH_LINE = SlashFx.line("slash/streaks")
            .color(0xE014100E).core(0xFF3E3430).width(0.18f).taper(SlashFx.Taper.CRESCENT).lifetime(10).sweep(4)
            .translucent();
    private static final SlashFx STRING = SlashFx.line("slash/smooth")
            .color(0xC0FFE6A0).core(0xFFFFFFFF).width(0.05f).taper(SlashFx.Taper.UNIFORM).lifetime(6).sweep(1);
    private static final SlashFx SIGHT_LINE = SlashFx.line("slash/dashed_fine")
            .color(0x90BEB4FF).core(0xC0FFFFFF).width(0.04f).taper(SlashFx.Taper.UNIFORM).lifetime(8).sweep(2);
    private static final SlashFx JET = SlashFx.line("slash/streaks")
            .color(0xD0A8F0F0).core(0xFFFFFFFF).width(0.35f).taper(SlashFx.Taper.COMET).lifetime(8).sweep(3);
    private static final SlashFx WIND_LINE = SlashFx.line("slash/streaks")
            .color(0x80E8F4FF).core(0xA0FFFFFF).width(0.25f).taper(SlashFx.Taper.COMET).lifetime(8).sweep(3);
    private static final SlashFx BOLT_LINE = SlashFx.line("slash/edge")
            .color(0xF0FFD83C).core(0xFFFFFAE0).width(0.12f).taper(SlashFx.Taper.CRESCENT).lifetime(5).sweep(1);
    private static final SlashFx SHADOW_TRAIL = SlashFx.line("slash/streaks")
            .color(0xC06C5CE7).core(0xE0D8CCFF).width(0.4f).taper(SlashFx.Taper.COMET).lifetime(10).sweep(3);
    private static final SlashFx QUILL = SlashFx.line("slash/edge")
            .color(0xE0C83030).core(0xFFFFE0E0).width(0.06f).taper(SlashFx.Taper.CRESCENT).lifetime(10).sweep(5);

    // =====================================================================================
    // Sounds and small helpers
    // =====================================================================================

    /** One of the Observer's sounds (sounds/observer/, ModSounds.OBSERVER), heard by everyone near. */
    public static void sound(ServerLevel sl, Vec3 at, String name, float volume, float pitch) {
        Spells.sound(sl, at, ModSounds.observer(name), volume, pitch);
    }

    /** A sound only this player hears, coming from 'at'. */
    public static void soundFor(ServerPlayer player, Vec3 at, String name, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.observer(name)),
                SoundSource.PLAYERS, at.x, at.y, at.z, volume, pitch, player.getRandom().nextLong()));
    }

    private static float wobble(float pitch) {
        return pitch * (0.95f + ThreadLocalRandom.current().nextFloat() * 0.1f);
    }

    static Vec3 chestOf(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.6, 0);
    }

    static Vec3 headOf(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() + 0.1, 0);
    }

    static Vec3 handOf(LivingEntity player, ServerLevel sl) {
        Vec3 hand = SpellFx.handOf(player, sl);
        return hand == null ? chestOf(player) : hand;
    }

    /** One block the way a yaw looks, level with the ground. */
    static Vec3 ahead(float yaw) {
        double angle = Math.toRadians(yaw);
        return new Vec3(-Math.sin(angle), 0, Math.cos(angle));
    }

    static float yawToward(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    private static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
    }

    private static float randomYaw(ServerLevel sl) {
        return sl.getRandom().nextFloat() * 360f;
    }

    private static int hue(int i) {
        return HUES[Math.floorMod(i, HUES.length)];
    }

    /** A random spot on the ground within 'radius' of 'center'. */
    static Vec3 within(Vec3 center, double radius, double up) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double a = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
        return center.add(Math.cos(a) * far, up, Math.sin(a) * far);
    }

    /** Yaw and pitch that turn a model's front (north) to look along 'facing' (pitch -90 = lying face up). */
    static float[] facingAngles(Vec3 facing) {
        Vec3 f = facing.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : facing.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-f.x, f.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, f.y))));
        return new float[]{yaw, pitch};
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
    private static void wear(ServerLevel sl, Entity entity, ModelFx model, int ticks, String tag) {
        ParticleShapes.clearModels(sl, entity, tag);
        if (ticks <= 0) return;
        ParticleShapes.modelOnTurning(sl, model.lifetime(ticks).tag(tag), entity, Vec3.ZERO, 0f, 0f, 0f);
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

    /** Points only 'viewer' sees along a line (a thread, a gaze). */
    private static void lineFor(ServerPlayer viewer, ParticleFx dot, Vec3 from, Vec3 to, double perBlock) {
        Vec3 way = to.subtract(from);
        int count = Math.max(1, (int) Math.ceil(way.length() * perBlock));
        for (int i = 0; i <= count; i++) ParticleShapes.spawnFor(viewer, dot, from.add(way.scale((double) i / count)), Vec3.ZERO);
    }

    /** A tag of its own for one cast of a look: tagFor("totem"). */
    public static String tagFor(String name) {
        return "obs_fx_" + name + "_" + java.util.UUID.randomUUID();
    }

    // =====================================================================================
    // Statuses: a picture in the row above the hotbar and a clock beside it
    // =====================================================================================

    public enum Status {
        // what the Observer holds
        TERRITORY("territory", "Territory", 0x6A9A3A),
        TRUE_SIGHT("true_sight", "True Sight", 0xBEB4FF),
        MANY_ANGLES("many_angles", "Many Angles", 0x8CD8D0),
        HIDDEN("hidden", "In a Painting", 0x8AA8B8),
        PAINTED_FACE("painted_face", "Painted Face", 0xECE2C8),
        SELF_PORTRAIT("self_portrait", "Self Portrait", 0x6C5CE7),
        INK_SERVANTS("ink_servants", "Ink Servants", 0x8CD8D0),
        PHASE("phase", "Phase", 0x8CD8D0),
        PIXIE_WINGS("pixie_wings", "Pixie Wings", 0x8CD8D0),
        SHADOW_FORM("shadow_form", "Shadow Form", 0x6C5CE7),
        SHADOW_WEAKNESS("shadow_weakness", "Weakened Form", 0x8A84B0),
        HARMONY("harmony", "Harmony", 0xFFE6A0),
        FOREGONE_SURVIVAL("foregone_survival", "Foregone Survival", 0xFFD66E),
        HIGHER_REVIVAL("higher_revival", "Higher Revival", 0xFFE9B0),
        BLOOM("bloom", "Bloom", 0xFFE9B0),
        FALL_SILENT("fall_silent", "Fall Silent", 0xFFE9B0),
        FANTASY_DOUBLE("fantasy_double", "Fantasy Double", 0xBEB4FF),
        // what is done to others (when they are players)
        WATCHED("watched", "Watched", 0xD8D4F0),
        STUDIED("studied", "Studied", 0x8CD8D0),
        SEEN_THROUGH("seen_through", "Seen Through", 0x8A7AFF),
        MARKED("marked", "Painted Mark", 0x6C5CE7),
        RIOT("riot", "Riot of Colours", 0xFF2A6A),
        ITCHING("itching", "Itching", 0x5A7A9A),
        TRAPPED("trapped", "In a Painting World", 0x6C5CE7),
        FOREGONE_LOSS("foregone_loss", "Foregone Loss", 0xC83030),
        RESONATING("resonating", "Resonating", 0xFFE6A0),
        DISSONANT("dissonant", "Dissonant", 0xBEB4FF),
        SILENT("silent", "Fallen Silent", 0x8A8A96),
        ERASED("erased", "Erased", 0xECE2C8);

        final String image;
        final String label;
        final int rgb;

        Status(String image, String label, int rgb) {
            this.image = image;
            this.label = label;
            this.rgb = rgb;
        }

        String id() {
            return "obs_status_" + name().toLowerCase();
        }
    }

    /** Shows the status's picture (in the row above the hotbar, rows of nine) and a clock beside the hotbar. */
    public static void status(LivingEntity player, Status status, int ticks) {
        if (!(player instanceof Player viewer)) return;
        int slot = status.ordinal();
        ScreenImages.show(viewer, status.id(), "observer/" + status.image, ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18,
                -21 - (slot / 9) * 18, 16, ticks);
        HudTimer.show(viewer, status.id(), status.label, ticks, status.rgb);
    }

    /** Only the picture, no clock: for what lasts until it is ended (Territory, Phase, Shadow Form...). */
    public static void statusForever(LivingEntity player, Status status) {
        if (!(player instanceof Player viewer)) return;
        int slot = status.ordinal();
        ScreenImages.show(viewer, status.id(), "observer/" + status.image, ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18,
                -21 - (slot / 9) * 18, 16, FOREVER);
        HudTimer.hide(viewer, status.id());
    }

    /** It ended early. */
    public static void statusEnd(LivingEntity player, Status status) {
        if (!(player instanceof Player viewer)) return;
        ScreenImages.hide(viewer, status.id());
        HudTimer.hide(viewer, status.id());
    }

    // =====================================================================================
    // The wheels' pictures (textures/gui/ability_menus/observer/, for AbilityWheel.entry's icon)
    // =====================================================================================

    private static final String WHEEL = "ability_menus/observer/";
    // key 1: Territory
    public static final String WHEEL_TOTEM_TERRITORY = WHEEL + "totem_territory";
    public static final String WHEEL_BORROWED_POWER = WHEEL + "borrowed_power";
    public static final String WHEEL_INK_SERVANTS = WHEEL + "ink_servants";
    public static final String WHEEL_PAINTING_WORLD = WHEEL + "painting_world";
    // key 2: Sight
    public static final String WHEEL_TRUE_SIGHT = WHEEL + "true_sight";
    public static final String WHEEL_MANY_ANGLES = WHEEL + "many_angles";
    public static final String WHEEL_READ_INTENT = WHEEL + "read_intent";
    // key 3: Canvas
    public static final String WHEEL_RIOT_OF_COLOURS = WHEEL + "riot_of_colours";
    public static final String WHEEL_ITCHING_HOUSE = WHEEL + "itching_house";
    public static final String WHEEL_GOLDEN_SUN = WHEEL + "golden_sun";
    public static final String WHEEL_BOAT_IN_A_STORM = WHEEL + "boat_in_a_storm";
    public static final String WHEEL_SELF_PORTRAIT = WHEEL + "self_portrait";
    public static final String WHEEL_GIANTS_FIST = WHEEL + "giants_fist";
    // key 4: Fiction
    public static final String WHEEL_PAINTED_MARK = WHEEL + "painted_mark";
    public static final String WHEEL_HIDE_IN_PAINTING = WHEEL + "hide_in_painting";
    public static final String WHEEL_PAINTED_FACE = WHEEL + "painted_face";
    public static final String WHEEL_MANIFEST_KNIGHT = WHEEL + "manifest_knight";
    public static final String WHEEL_MANIFEST_ARCHER = WHEEL + "manifest_archer";
    public static final String WHEEL_FANTASY_DOUBLE = WHEEL + "fantasy_double";
    // key 5: Dimension
    public static final String WHEEL_PHASE = WHEEL + "phase";
    public static final String WHEEL_PIXIE_WINGS = WHEEL + "pixie_wings";
    public static final String WHEEL_PAINTED_LIGHTNING = WHEEL + "painted_lightning";
    public static final String WHEEL_DIMENSIONAL_PASSAGE = WHEEL + "dimensional_passage";
    public static final String WHEEL_HOSTEL = WHEEL + "hostel";
    public static final String WHEEL_SHADOW_FORM = WHEEL + "shadow_form";
    // key 6: Strings
    public static final String WHEEL_TAUT_CHORD = WHEEL + "taut_chord";
    public static final String WHEEL_RESONANCE = WHEEL + "resonance";
    public static final String WHEEL_HARMONY = WHEEL + "harmony";
    public static final String WHEEL_DISSONANCE = WHEEL + "dissonance";
    public static final String WHEEL_SHADOW_FROM_ABOVE = WHEEL + "shadow_from_above";
    public static final String WHEEL_FOLD_SPACE = WHEEL + "fold_space";
    // key 7: Sublunary
    public static final String WHEEL_FOREGONE_LOSS = WHEEL + "foregone_loss";
    public static final String WHEEL_FOREGONE_SURVIVAL = WHEEL + "foregone_survival";
    public static final String WHEEL_HIGHER_REVIVAL = WHEEL + "higher_revival";
    public static final String WHEEL_BLOOM = WHEEL + "bloom";
    public static final String WHEEL_FALL_SILENT = WHEEL + "fall_silent";
    public static final String WHEEL_ERASE = WHEEL + "erase";

    // =====================================================================================
    // The models (models/model_fx/observer/, see each JSON's "credit")
    // =====================================================================================

    /** Totem Territory's totem, 2 blocks, pivot its foot. */
    public static final ModelFx TOTEM = ModelFx.of("observer/totem").glow();
    /** The Giant's Fist, 1 x 2 blocks at scale 1, pivot under its knuckles. */
    public static final ModelFx GIANT_FIST = ModelFx.of("observer/giant_fist").glow();
    /** Pixie Wings in three steps (raised, level, lowered), worn on the back. */
    public static final ModelFx PIXIE_WINGS = ModelFx.of("observer/pixie_wings").frames(3).smooth().glow().unshaded().seeThrough();
    /** An Ink Servant, 0.9 blocks, pivot its middle: FollowerSummon.hover(INK_SERVANT, servantOffset(i)). */
    public static final ModelFx INK_SERVANT = ModelFx.of("observer/ink_servant").pivot(8, 8, 8).glow();
    /** The figures of fiction (two steps each: on guard / striking, bow raised / drawn), pivot their feet. */
    public static final ModelFx KNIGHT = ModelFx.of("observer/manifest_knight").frames(2).smooth().glow();
    public static final ModelFx ARCHER = ModelFx.of("observer/manifest_archer").frames(2).smooth().glow();
    /** The two Mythical Creature Forms, worn over the player. */
    public static final ModelFx OBSERVER_FIGURE = ModelFx.of("observer/observer_figure").pivot(8, -16, 8).glow().unshaded().seeThrough();
    public static final ModelFx SHADOW = ModelFx.of("observer/shadow_form").glow().seeThrough();
    /** Bloom's flower in three steps (bud, half open, open), 0.7 blocks, pivot where it grows. */
    public static final ModelFx FLOWER = ModelFx.of("observer/bloom_flower").frames(3).smooth().glow().unshaded().seeThrough();
    /** Shadow From Above's blade, 2 blocks, its point at the top, pivot its base. */
    public static final ModelFx SHADOW_BLADE = ModelFx.of("observer/shadow_blade").glow().seeThrough();
    /** Standing pictures (pivot their middle unless said). */
    public static final ModelFx OBSERVATION_EYE = ModelFx.of("observer/observation_eye").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx DOSSIER_PAGE = ModelFx.of("observer/dossier_page").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx REVIVAL_WARD = ModelFx.of("observer/revival_ward").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx SUBLUNARY_EYE = ModelFx.of("observer/sublunary_eye").frames(3).pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx ERASE_SMEAR = ModelFx.of("observer/erase_smear").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx RIOT_SWIRL = ModelFx.of("observer/riot_swirl").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx PAINTED_FACE = ModelFx.of("observer/painted_face").pivot(8, 8, 8).glow().seeThrough();
    public static final ModelFx HOSTEL_DOOR = ModelFx.of("observer/hostel_door").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** Standing pictures with their pivot at the foot. */
    public static final ModelFx RIFT = ModelFx.of("observer/dimensional_rift").glow().unshaded().seeThrough();
    public static final ModelFx FOLD_CREASE = ModelFx.of("observer/fold_crease").glow().unshaded().seeThrough();
    public static final ModelFx INK_WAVE = ModelFx.of("observer/ink_wave").glow().unshaded().seeThrough();
    /** Flat pictures, 1 block wide, pivot their middle. White unless said. */
    public static final ModelFx SIGHT_RING = ModelFx.of("observer/sight_ring").glow().unshaded().seeThrough();
    public static final ModelFx TERRITORY_CIRCLE = ModelFx.of("observer/territory_circle").glow().unshaded().seeThrough();
    public static final ModelFx PAINTED_MARK = ModelFx.of("observer/painted_mark").glow().unshaded().seeThrough();
    public static final ModelFx GOLDEN_SUN_FIELD = ModelFx.of("observer/golden_sun_field").glow().unshaded().seeThrough();
    public static final ModelFx SHADOW_CIRCLE = ModelFx.of("observer/shadow_circle").glow().unshaded().seeThrough();
    public static final ModelFx PHASE_VEIL = ModelFx.of("observer/phase_veil").glow().unshaded().seeThrough();
    public static final ModelFx RESONANCE_RING = ModelFx.of("observer/resonance_ring").glow().unshaded().seeThrough();
    public static final ModelFx CHORD_RING = ModelFx.of("observer/chord_ring").glow().unshaded().seeThrough();
    public static final ModelFx DISSONANCE_RING = ModelFx.of("observer/dissonance_ring").glow().unshaded().seeThrough();
    public static final ModelFx FOREGONE_SCRIPT = ModelFx.of("observer/foregone_script").glow().unshaded().seeThrough();
    /** Beams: two crossed planes 1 block long, start at the bottom. Lay them with modelAlong. */
    public static final ModelFx STRING_BEAM = ModelFx.of("observer/string_beam").glow().unshaded().seeThrough();
    public static final ModelFx PAINTED_BOLT = ModelFx.of("observer/painted_bolt").glow().unshaded().seeThrough();
    /** The Painting World: its barrier (a ring of walls, 1 x 1 block at scale 1) and its sky (flat). */
    public static final ModelFx WORLD_BARRIER = ModelFx.of("observer/world_barrier").glow().unshaded().seeThrough();
    public static final ModelFx WORLD_SKY = ModelFx.of("observer/world_sky").glow().unshaded().seeThrough();
    /** The shared stone spike (Borrowed Power's Earth Spike, tinted). */
    private static final ModelFx SPIKE = ModelFx.of("ice_spike").glow();
    /** A soft ball of light (the shared "orb"). White, tint it. */
    private static final ModelFx ORB = ModelFx.of("orb").pivot(8, 8, 8).glow().unshaded().seeThrough();

    // =====================================================================================
    // Paintings: the framed canvases (canvas_<name>.json, pictures in textures/model_fx/observer/paintings/)
    // =====================================================================================

    /**
     * Every Painting there is. The canvas is the model, its picture is the Painting. A wide canvas is 1.5 x 1 block
     * at scale 1 and a tall one 1 x 1.5; CANVAS_SCALE makes them the plan's 3 x 2. To add one: a painter in
     * tools/model_gen/observer/paintings.py and a line here with the same name.
     */
    public enum Painting {
        RIOT_OF_COLOURS("riot_of_colours", false, 0xFFFF2A6A),
        ITCHING_HOUSE("itching_house", false, 0xFF5A7A9A),
        GOLDEN_SUN("golden_sun", false, 0xFFFFD23A),
        BOAT_IN_A_STORM("boat_in_a_storm", false, 0xFF2A6A78),
        GIANTS_FIST("giants_fist", false, 0xFFE07A3A),
        PAINTING_WORLD("painting_world", false, 0xFF6C5CE7),
        BLANK("blank", false, 0xFFECE2C8),
        SELF_PORTRAIT("self_portrait", true, 0xFF6C5CE7),
        HIDE_IN_PAINTING("hide_in_painting", true, 0xFF8AA8B8),
        HIDE_IN_PAINTING_EMPTY("hide_in_painting_empty", true, 0xFF8AA8B8),
        FANTASY("fantasy", true, 0xFFBEB4FF);

        /** The framed canvas showing this Painting. */
        public final ModelFx canvas;
        /** True for the upright canvases (1 x 1.5 at scale 1). */
        public final boolean tall;
        /** The Painting's main colour, for its paint and its motes. */
        public final int accent;

        Painting(String name, boolean tall, int accent) {
            this.canvas = ModelFx.of("observer/canvas_" + name).pivot(8, 8, 8).glow();
            this.tall = tall;
            this.accent = accent;
        }
    }

    /** The scale that makes a canvas the plan's 3 x 2 blocks. */
    public static final float CANVAS_SCALE = 2f;
    /** How long painting takes before the canvas hangs: 1 second; PAINT_TICKS_PIXIE from Pixie (5). */
    public static final int PAINT_TICKS = 20;
    public static final int PAINT_TICKS_PIXIE = 8;

    /**
     * Painting in the air: brush strokes of ink and colour drawn at 'spot' for 'ticks' (PAINT_TICKS, PAINT_TICKS_PIXIE,
     * 0 for instant), the brush in the painter's hand. Hang the canvas when it is done.
     */
    public static void paint(ServerLevel sl, LivingEntity painter, Vec3 spot, Painting painting, int ticks) {
        Vec3 hand = handOf(painter, sl);
        Vec3 right = Spells.rightOf(painter);
        int steps = Math.max(1, ticks / 3);
        for (int i = 0; i < steps; i++) {
            int k = i;
            Later.run(sl, i * 3, () -> {
                double a = k * 1.3;
                Vec3 from = spot.add(right.scale(Math.cos(a) * 1.2)).add(0, Math.sin(a) * 0.8, 0);
                Vec3 to = spot.add(right.scale(Math.cos(a + 2.1) * 1.3)).add(0, Math.sin(a + 2.1) * 0.9, 0);
                ParticleShapes.slashBetween(sl, BRUSH_LINE.color(k % 2 == 0 ? 0xE014100E : withAlpha(hue(k), 0xD0)), from, to);
                ParticleShapes.spawn(sl, SPLAT.color(hue(k)), to, Vec3.ZERO);
                if (painter.isAlive()) ParticleShapes.line(sl, BRUSH.size(0.08f).lifetime(4, 0), handOf(painter, sl), from, 2.0);
            });
        }
        ParticleShapes.burst(sl, BRUSH, hand, 3, 0.01, 0.04);
        if (ticks > 0) sound(sl, spot, ticks > PAINT_TICKS_PIXIE ? "painting" : "painting_quick", 0.9f, wobble(1.0f));
    }

    /**
     * A Painting hangs at 'spot' with its front looking along 'facing' (toward who should see it: on a wall, the
     * wall's outward side; on the ground, straight up) for 'ticks', under 'tag'. Its strength fades over its life
     * (the plan's one-third fade): the canvas dims a little toward the end.
     */
    public static void hang(ServerLevel sl, Painting painting, Vec3 spot, Vec3 facing, int ticks, String tag) {
        float[] angles = facingAngles(facing);
        ModelFx canvas = painting.canvas.scale(CANVAS_SCALE).lifetime(ticks).fade(0, 16).tag(tag)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(6, pose().scale(1.05f).alpha(1f), Ease.OUT_BACK)
                .key(10, pose().scale(1f))
                .key(Math.max(11, ticks), pose().alpha(0.67f));
        ParticleShapes.model(sl, canvas, spot, angles[0], angles[1], 0f);
        ParticleShapes.burst(sl, SPLAT.color(painting.accent), spot, 8, 0.03, 0.1);
        sound(sl, spot, "canvas_hang", 0.8f, wobble(1.0f));
    }

    /**
     * Someone is looking at a hung Painting and it is taking hold: a thread of the Painting's colour from the canvas to
     * their eyes, brighter as 'progress' (0..1) fills. Call it every 5 ticks while they stare.
     */
    public static void paintingStare(ServerLevel sl, Painting painting, Vec3 canvas, LivingEntity looker, float progress) {
        Vec3 eyes = looker.getEyePosition();
        int alpha = (int) (0x30 + 0xA0 * Math.max(0f, Math.min(1f, progress)));
        ParticleShapes.slashBetween(sl, SIGHT_LINE.color(withAlpha(painting.accent, alpha)).lifetime(6), canvas, eyes);
        if (progress > 0.5f) ParticleShapes.spawn(sl, SPLAT.color(painting.accent).size(0.06f), eyes, Vec3.ZERO);
    }

    /** It took hold: the canvas flares and its paint streams into the eyes of whoever saw it. */
    public static void paintingSeen(ServerLevel sl, Painting painting, Vec3 canvas, LivingEntity victim) {
        Vec3 eyes = victim.getEyePosition();
        for (int i = 0; i < 6; i++) {
            int k = i;
            Later.run(sl, i, () -> ParticleShapes.line(sl, SPLAT.color(painting == Painting.RIOT_OF_COLOURS ? hue(k) : painting.accent)
                    .size(0.05f).lifetime(6, 2), canvas, eyes, 1.0));
        }
        SpellFx.shockRing(sl, canvas, 1.8, withAlpha(painting.accent, 0xC0), 8, 0);
        ParticleShapes.burst(sl, SPARKLE, eyes, 8, 0.02, 0.06);
        sound(sl, canvas, "painting_seen", 1.0f, wobble(1.0f));
    }

    /** The canvas goes: spent (its paint runs off it) or faded (it simply dims away). */
    public static void paintingEnd(ServerLevel sl, Painting painting, Vec3 canvas, String tag, boolean spent) {
        ParticleShapes.clearModels(sl, canvas, tag);
        if (spent) {
            for (int i = 0; i < 12; i++) {
                ParticleShapes.spawn(sl, SPLAT.color(painting.accent), within(canvas, 1.2, 0).add(0, (Math.random() - 0.5) * 1.6, 0),
                        new Vec3(0, -0.05, 0));
            }
            ParticleShapes.burst(sl, PAGE, canvas, 4, 0.02, 0.06);
        } else {
            ParticleShapes.burst(sl, INK_SMOKE.color(withAlpha(painting.accent, 0x60)), canvas, 8, 0.01, 0.04);
        }
        sound(sl, canvas, "painting_fade", 0.7f, wobble(spent ? 0.9f : 1.1f));
    }

    /** The Gallery: a hung canvas folds away into the painter's hand. */
    public static void foldPainting(ServerLevel sl, Painting painting, Vec3 canvas, LivingEntity painter, String tag) {
        ParticleShapes.clearModels(sl, canvas, tag);
        ModelFx folding = painting.canvas.scale(CANVAS_SCALE).lifetime(8).fade(0, 4)
                .key(8, pose().scale(0.1f).alpha(0.4f), Ease.IN);
        ParticleShapes.model(sl, folding, canvas, yawToward(canvas, painter.position()) + 180f, 0f, 0f);
        ParticleShapes.line(sl, FOLD, canvas, handOf(painter, sl), 2.0);
        sound(sl, canvas, "fold_space", 0.5f, 1.4f);
    }

    // =====================================================================================
    // Observation: what you keep looking at, you change
    // =====================================================================================

    /**
     * The eyes over a watched enemy, seen by the watcher alone (through walls): one eye per level of Observation,
     * coloured by level, for 'ticks'. level 0 takes them away. Call it again when the level changes.
     */
    public static void observed(ServerPlayer viewer, LivingEntity target, int level, int ticks) {
        String tag = "obs_eye_" + viewer.getUUID();
        ParticleShapes.clearModels(target.level(), target, tag);
        if (level <= 0) return;
        int color = OBSERVATION_COLORS[Math.min(3, level)];
        double radius = level <= 1 ? 0.0 : Math.max(0.3, target.getBbWidth() * 0.5);
        for (int i = 0; i < level; i++) {
            double a = Math.PI * 2 * i / level;
            ModelFx eye = OBSERVATION_EYE.color(color).scale(0.35f).lifetime(ticks).fade(4, 8).spin(3f).tag(tag)
                    .key(0, pose().scale(0.2f)).key(6, pose().scale(1f), Ease.OUT_BACK);
            ParticleShapes.modelOnFor(viewer, eye, target,
                    new Vec3(Math.cos(a) * radius, target.getBbHeight() + 0.5, Math.sin(a) * radius), 0f, 0f, 0f);
        }
    }

    /** A level of Observation is gained: a soft click for the watcher, and the chime of being seen through at 3. */
    public static void observationRises(ServerPlayer viewer, LivingEntity target, int level) {
        Vec3 top = headOf(target).add(0, 0.5, 0);
        ParticleShapes.spawnFor(viewer, EYE.color(OBSERVATION_COLORS[Math.min(3, level)]), top, new Vec3(0, 0.02, 0));
        if (level >= 3) {
            soundFor(viewer, top, "seen_through", 0.8f, 1.0f);
            for (int i = 0; i < 10; i++) {
                double a = Math.PI * 2 * i / 10;
                ParticleShapes.spawnFor(viewer, VIOLET_MOTE, chestOf(target).add(Math.cos(a) * 0.8, 0, Math.sin(a) * 0.8),
                        new Vec3(-Math.cos(a) * 0.04, 0.01, -Math.sin(a) * 0.04));
            }
        } else {
            soundFor(viewer, top, "observed", 0.6f, 0.9f + level * 0.15f);
        }
    }

    /** While Observation builds: a faint line of sight from the watcher's eyes to the enemy, for the watcher alone. */
    public static void staring(ServerPlayer viewer, LivingEntity target) {
        Vec3 from = viewer.getEyePosition().add(viewer.getLookAngle().scale(0.8)).add(0, -0.15, 0);
        lineFor(viewer, MARK_DOT.color(0x60BEB4FF).endColor(0x00BEB4FF).lifetime(6, 0), from, chestOf(target), 1.2);
    }

    /** Foregone Loss or Erase without Observation 3: a closed eye flickers before the caster, and nothing happens. */
    public static void notWatchedEnough(ServerPlayer player) {
        Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(1.2));
        ParticleShapes.modelFor(player, OBSERVATION_EYE.color(0xC08A8A96).scale(0.4f).lifetime(14).fade(0, 8)
                .key(0, pose().scale(1f)).key(14, pose().scale(0.6f).alpha(0f)), front, player.getYRot() + 180f, 0f, 0f);
        soundFor(player, front, "observed", 0.6f, 0.6f);
    }

    /** Level 3 holds a teleport or a passage on the enemy: a violet ring snaps round it. */
    public static void heldInSight(ServerLevel sl, LivingEntity target) {
        SpellFx.closingRing(sl, target.position(), 2.0, 0xC08A7AFF, 8, 0);
        ParticleShapes.burst(sl, VIOLET_MOTE, chestOf(target), 8, 0.02, 0.06);
    }

    // =====================================================================================
    // Key 1: Territory
    // =====================================================================================

    /** The tick the totem stands up out of the ground after totem(...). */
    public static final int TOTEM_RISEN = 14;

    /**
     * The ritual of Totem Territory (3 seconds of standing still): the drum, the circle drawing itself under the
     * Shaman, glyphs rising, motes of moss drawn in. Under 'tag'; totemRitualCancel if it is broken.
     */
    public static void totemRitual(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        Vec3 ground = player.position();
        ModelFx circle = TERRITORY_CIRCLE.color(0xB06A9A3A).scale(3f).lifetime(ticks + 4).fade(0, 4).spin(1.5f).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(ticks, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, circle, ground.add(0, 0.05, 0), randomYaw(sl), 0f, 0f);
        for (int t = 0; t < ticks; t += 8) {
            int at = t;
            Later.run(sl, t, () -> {
                if (!player.isAlive()) return;
                double r = 2.2 * (1 - (double) at / ticks) + 0.6;
                ParticleShapes.ring(sl, MOSS_MOTE, ground.add(0, 0.2, 0), r, 6, -0.04);
                ParticleShapes.spawn(sl, GLYPH, within(ground, 1.5, 0.3), new Vec3(0, 0.04, 0));
            });
        }
        sound(sl, ground, "totem_ritual", 1.0f, 1.0f);
    }

    public static void totemRitualCancel(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player.position(), tag);
        ParticleShapes.burst(sl, LEAF, player.position().add(0, 0.4, 0), 8, 0.02, 0.08);
        sound(sl, player.position(), "territory_end", 0.6f, 1.3f);
    }

    /**
     * The totem rises and the Territory opens: the carved totem out of the ground (standing at TOTEM_RISEN), the circle
     * of the Territory 'radius' blocks out, a ring of moss racing to its edge. Under 'tag' for 'ticks' (FOREVER: show it
     * again after an hour). shell = Sequence 6 and above: the shell of light rises too (territoryShell).
     */
    public static void totem(ServerLevel sl, Vec3 ground, double radius, int ticks, String tag) {
        ModelFx pole = TOTEM.lifetime(ticks).fade(0, 20).tag(tag)
                .key(0, pose().up(-2.2f))
                .key(TOTEM_RISEN, pose().up(0f), Ease.OUT_BACK);
        ParticleShapes.model(sl, pole, ground, randomYaw(sl), 0f, 0f);
        floor(sl, TERRITORY_CIRCLE, ground, radius, 0x906A9A3A, ticks, 0.15f, tag, TOTEM_RISEN);
        ParticleShapes.burst(sl, DUST, ground.add(0, 0.3, 0), 12, 0.03, 0.1);
        ParticleShapes.burst(sl, ROCK, ground.add(0, 0.3, 0), 10, 0.1, 0.25);
        Later.run(sl, TOTEM_RISEN, () -> {
            SpellFx.shockRing(sl, ground, radius, 0xB0B8E07A, 16, 0);
            ParticleShapes.burst(sl, FEATHER, ground.add(0, 2.0, 0), 6, 0.02, 0.06);
            ParticleShapes.spawn(sl, EYE.color(0xFFB8E07A).size(0.3f).endSize(0.3f), ground.add(0, 1.6, 0), Vec3.ZERO);
        });
        sound(sl, ground, "totem_rise", 1.2f, 1.0f);
    }

    /** Now and then (every 5 seconds or so) while the Territory stands: moss motes drift along its edge. */
    public static void territoryPulse(ServerLevel sl, Vec3 ground, double radius) {
        for (int i = 0; i < 10; i++) {
            double a = Math.random() * Math.PI * 2;
            ParticleShapes.spawn(sl, MOSS_MOTE, ground.add(Math.cos(a) * radius, 0.2, Math.sin(a) * radius), new Vec3(0, 0.03, 0));
        }
        ParticleShapes.spawn(sl, GLYPH, ground.add(0, 2.2, 0), new Vec3(0, 0.02, 0));
    }

    /** Sequence 6 and above: a shell of soft green light round the Territory, 'radius' wide and as high. */
    public static void territoryShell(ServerLevel sl, Vec3 ground, double radius, int ticks, String tag) {
        float w = (float) (radius * 2);
        ModelFx wall = WORLD_BARRIER.color(0x50B8E07A).scale(w, (float) Math.min(radius, 12), w).lifetime(ticks).fade(0, 20).tag(tag)
                .key(0, pose().scale(0.95f).alpha(0f))
                .key(20, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, wall, ground, 0f, 0f, 0f);
        SpellFx.risingRings(sl, ground, radius, Math.min(radius, 12), 4, 0x80B8E07A);
    }

    /** An enemy projectile stops at the shell. */
    public static void territoryShellBlock(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, MOSS_MOTE, at, 10, 0.03, 0.1);
        ParticleShapes.spawn(sl, GLYPH.size(0.25f), at, Vec3.ZERO);
        sound(sl, at, "world_barrier", 0.6f, 1.3f);
    }

    /** An enemy walks into the Territory: a soft chime for the Shaman alone, and it glows for them for 2 seconds. */
    public static void intruder(ServerPlayer owner, LivingEntity enemy) {
        SpellFx.markFor(owner, enemy, 0xFFB8E07A, 40);
        soundFor(owner, chestOf(enemy), "territory_chime", 0.8f, 1.0f);
    }

    /** The Territory closes (a new totem, death, logout): the totem sinks back into the ground. */
    public static void territoryEnd(ServerLevel sl, Vec3 ground, String tag) {
        ParticleShapes.clearModels(sl, ground, tag);
        ModelFx sinking = TOTEM.lifetime(16).fade(0, 10).key(16, pose().up(-2.2f), Ease.IN);
        ParticleShapes.model(sl, sinking, ground, randomYaw(sl), 0f, 0f);
        ParticleShapes.burst(sl, DUST, ground.add(0, 0.3, 0), 8, 0.02, 0.06);
        ParticleShapes.burst(sl, LEAF, ground.add(0, 1.0, 0), 8, 0.02, 0.06);
        sound(sl, ground, "territory_end", 1.0f, 1.0f);
    }

    // ---------- Borrowed Power

    /** The five crafts of Borrowed Power, by what was asked for power. */
    public enum Craft {
        GALE(0xFFE8F4FF), EARTH_SPIKE(0xFF8A6A4A), WATER_LASH(0xFFA8F0F0), THORN_SNARE(0xFF6A9A3A), BEAST_CALL(0xFFE8DCC0);

        public final int color;

        Craft(int color) {
            this.color = color;
        }
    }

    /** The Shaman asks: motes of the source's colour drawn from it into their hands. boosted = inside the Territory. */
    public static void borrow(ServerLevel sl, LivingEntity player, Vec3 source, Craft craft, boolean boosted) {
        Vec3 hand = handOf(player, sl);
        int count = boosted ? 14 : 8;
        for (int i = 0; i < count; i++) {
            Vec3 from = within(source, 1.2, Math.random());
            ParticleShapes.spawn(sl, MOSS_MOTE.color(craft.color).lifetime(12, 0), from, hand.subtract(from).scale(1 / 12.0));
        }
        if (boosted) ParticleShapes.spawn(sl, GLYPH.size(0.2f), hand.add(0, 0.4, 0), new Vec3(0, 0.02, 0));
        sound(sl, hand, "borrow", 0.8f, wobble(1.0f));
    }

    /** Gale: a cone of wind 'length' blocks long out of the Shaman's look. */
    public static void gale(ServerLevel sl, LivingEntity player, double length) {
        Vec3 eye = player.getEyePosition().add(0, -0.3, 0);
        Vec3 look = player.getLookAngle();
        ParticleShapes.cone(sl, WIND, eye, look, 50, 40, length / 18, length / 12);
        for (int i = 0; i < 5; i++) {
            Vec3 dir = ParticleShapes.randomDirectionInCone(sl.getRandom(), look, 40);
            ParticleShapes.slashBetween(sl, WIND_LINE, eye.add(dir.scale(1.5)), eye.add(dir.scale(length)));
        }
        ParticleShapes.cone(sl, LEAF, eye, look, 40, 10, 0.4, 0.8);
        sound(sl, eye, "gale", 1.1f, wobble(1.0f));
    }

    /** Ticks between one Earth Spike and the next along the line. */
    public static final int EARTH_SPIKE_STEP = 1;

    /** Earth Spike: stone spikes burst up one after another along the ground from 'from' along 'direction'. */
    public static void earthSpikes(ServerLevel sl, Vec3 from, Vec3 direction, double length) {
        Vec3 step = new Vec3(direction.x, 0, direction.z).normalize().scale(1.4);
        int count = Math.max(1, (int) (length / 1.4));
        for (int i = 0; i < count; i++) {
            Vec3 at = from.add(step.scale(i + 1));
            Later.run(sl, i * EARTH_SPIKE_STEP, () -> earthSpikeAt(sl, at));
        }
        sound(sl, from, "earth_spike", 1.2f, wobble(1.0f));
    }

    /** One stone spike at a spot on the ground. */
    public static void earthSpikeAt(ServerLevel sl, Vec3 ground) {
        ModelFx spike = SPIKE.color(0xFF8A6A4A).scale(0.8f, 1.6f, 0.8f).lifetime(30).fade(0, 12)
                .key(0, pose().up(-1.6f))
                .key(3, pose().up(0f), Ease.OUT_BACK)
                .key(30, pose().up(-1.6f), Ease.IN);
        ParticleShapes.model(sl, spike, ground, randomYaw(sl), 0f, (float) ((Math.random() - 0.5) * 20));
        ParticleShapes.burst(sl, ROCK, ground.add(0, 0.2, 0), 5, 0.08, 0.2);
        ParticleShapes.spawn(sl, DUST, ground.add(0, 0.3, 0), new Vec3(0, 0.02, 0));
    }

    /** Water Lash: a jet from the Shaman's hand to 'to', and a splash there. */
    public static void waterLash(ServerLevel sl, LivingEntity player, Vec3 to) {
        Vec3 from = handOf(player, sl);
        ParticleShapes.slashBetween(sl, JET, from, to);
        ParticleShapes.line(sl, DROP, from, to, 2.0);
        ParticleShapes.burst(sl, DROP, to, 14, 0.06, 0.18);
        SpellFx.shockRing(sl, to.add(0, -0.8, 0), 1.6, 0xC0A8F0F0, 8, 0);
        sound(sl, from, "water_lash", 1.0f, wobble(1.0f));
    }

    /** Thorn Snare: a green ring of thorns and leaves at 'center' for 'ticks', plants twisting up in it. */
    public static void thornSnare(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        floor(sl, TERRITORY_CIRCLE, center, radius, 0xC03A8A3A, ticks, -1f, tag, 0);
        for (int t = 0; t < ticks; t += 10) {
            Later.run(sl, t, () -> {
                ParticleShapes.ring(sl, LEAF, center.add(0, 0.3, 0), radius * 0.9, 6, 0.0);
                ParticleShapes.spawn(sl, MOSS_MOTE, within(center, radius, 0.2), new Vec3(0, 0.05, 0));
            });
        }
        sound(sl, center, "thorn_snare", 1.0f, wobble(1.0f));
    }

    public static void thornSnareEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.burst(sl, LEAF, center.add(0, 0.4, 0), 10, 0.02, 0.08);
    }

    /** Beast Call: the totem's eye opens over the beast and a moss-green thread ties it to the Shaman. */
    public static void beastCall(ServerLevel sl, LivingEntity player, LivingEntity beast, int ticks, String tag) {
        ParticleShapes.modelOn(sl, OBSERVATION_EYE.color(0xFFB8E07A).scale(0.5f).lifetime(ticks).fade(6, 10).spin(2f).tag(tag),
                beast, new Vec3(0, beast.getBbHeight() + 0.6, 0), 0f, 0f, 0f);
        SpellFx.circleUnder(sl, beast, Math.max(0.8, beast.getBbWidth()), 0xA06A9A3A, ticks, tag + "_c");
        SpellFx.tether(sl, SIGHT_LINE.color(0x80B8E07A), player, beast, Math.min(ticks, 40));
        ParticleShapes.burst(sl, FEATHER, headOf(beast), 4, 0.02, 0.05);
        sound(sl, chestOf(beast), "beast_call", 1.0f, wobble(1.0f));
    }

    public static void beastCallEnd(ServerLevel sl, LivingEntity beast, String tag) {
        ParticleShapes.clearModels(sl, beast, tag);
        ParticleShapes.clearModels(sl, beast, tag + "_c");
        ParticleShapes.burst(sl, LEAF, chestOf(beast), 6, 0.02, 0.06);
    }

    // ---------- Ink Servants

    /** Where servant 'index' (0..2) hovers round its master, for FollowerSummon.hover(INK_SERVANT, servantOffset(i)). */
    public static Vec3 servantOffset(int index) {
        double a = Math.PI * 2 * index / 3.0;
        return new Vec3(Math.cos(a) * 1.4, 1.6 + 0.2 * index, Math.sin(a) * 1.4);
    }

    /** The servants arrive: three blots of ink burst open round the Pixie. */
    public static void inkServantsArrive(ServerLevel sl, LivingEntity player) {
        for (int i = 0; i < 3; i++) {
            Vec3 at = player.position().add(servantOffset(i));
            ParticleShapes.burst(sl, INK_BLOT, at, 6, 0.02, 0.06);
            ParticleShapes.burst(sl, TEAL_MOTE, at, 4, 0.01, 0.04);
        }
        sound(sl, player.position(), "ink_servants", 1.0f, 1.0f);
    }

    /**
     * The three servants as looks only, hovering and bobbing round 'player' for 'ticks' under 'tag' (for the preview, or
     * when the servants should not be real entities). Real servants are INK_SERVANT on FollowerSummon.hover.
     */
    public static void inkServantLooks(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        for (int i = 0; i < 3; i++) {
            ModelFx servant = INK_SERVANT.scale(0.9f).lifetime(ticks).fade(6, 10).tag(tag)
                    .key(0, pose().scale(0.3f).alpha(0f))
                    .key(8, pose().scale(1f).alpha(1f), Ease.OUT_BACK)
                    .delay(i * 3);
            for (int t = 8, k = 0; t + 12 <= ticks && k < 24; t += 12, k++) {        // bobbing, 12 ticks each way
                servant = servant.during(t, t + 12, pose().up(k % 2 == 0 ? 0.15f : -0.15f), Ease.IN_OUT);
            }
            ParticleShapes.modelOn(sl, servant, player, servantOffset(i), i * 120f, 0f, 0f);
        }
    }

    /** A servant strikes: a dart of ink from it into the enemy. */
    public static void servantStrike(ServerLevel sl, Vec3 from, LivingEntity target) {
        Vec3 to = chestOf(target);
        ParticleShapes.slashBetween(sl, BRUSH_LINE.width(0.12f).lifetime(5), from, to);
        ParticleShapes.burst(sl, INK_BLOT, to, 5, 0.03, 0.08);
        sound(sl, to, "servant_strike", 0.7f, wobble(1.0f));
    }

    /** They dissolve back into ink. */
    public static void inkServantsLeave(ServerLevel sl, LivingEntity player) {
        for (int i = 0; i < 3; i++) {
            Vec3 at = player.position().add(servantOffset(i));
            ParticleShapes.burst(sl, INK_SMOKE, at, 5, 0.01, 0.04);
            ParticleShapes.spawn(sl, INK_BLOT, at, new Vec3(0, -0.06, 0));
        }
    }

    // ---------- Painting World

    /** The tick the Painting World is open after paintingWorld(...). */
    public static final int WORLD_OPEN = 20;

    /**
     * Painting World: a canvas opens at 'center' and grows into a pocket world 'radius' blocks round it, its formless
     * barrier rising at the edge and a black void closing over it as a sky, for 'ticks'. complete = from Dimensional
     * Shadow (2): a whole world, its barrier edged in gold, the void higher.
     */
    public static void paintingWorld(ServerLevel sl, Vec3 center, double radius, int ticks, boolean complete, String tag) {
        ModelFx canvas = Painting.PAINTING_WORLD.canvas.scale(CANVAS_SCALE).lifetime(WORLD_OPEN + 4).fade(0, 6)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK)
                .key(WORLD_OPEN, pose().scale((float) radius).alpha(0f), Ease.IN);
        ParticleShapes.model(sl, canvas, center.add(0, 2, 0), randomYaw(sl), 0f, 0f);
        Vec3 ground = center;
        float w = (float) (radius * 2);
        float height = (float) (complete ? Math.min(radius, 24) : Math.min(radius * 0.8, 10));
        int color = complete ? 0x90FFE9B0 : 0x906C5CE7;
        ModelFx wall = WORLD_BARRIER.color(color).scale(w, height, w).delay(WORLD_OPEN - 4).lifetime(ticks).fade(0, 20).tag(tag)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(8, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, wall, ground, 0f, 0f, 0f);
        ModelFx sky = WORLD_SKY.scale(w).delay(WORLD_OPEN).lifetime(ticks).fade(10, 20).tag(tag);
        ParticleShapes.model(sl, sky, ground.add(0, height, 0), 0f, 0f, 0f);
        Later.run(sl, WORLD_OPEN, () -> {
            SpellFx.shockRing(sl, ground, radius, withAlpha(color, 0xC0), 12, 0);
            ParticleShapes.ring(sl, INK_SMOKE, ground.add(0, 0.5, 0), radius, 30, 0.0);
        });
        sound(sl, center, "painting_world_open", 1.4f, complete ? 0.85f : 1.0f);
    }

    /** Someone tries to leave or teleport out and is turned back at the edge. */
    public static void worldBarrierHit(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, VIOLET_MOTE, at, 10, 0.03, 0.08);
        ParticleShapes.burst(sl, BRUSH, at, 4, 0.01, 0.04);
        sound(sl, at, "world_barrier", 0.8f, wobble(1.0f));
    }

    /** The Painting World ends: the sky lifts away and the barrier sinks. */
    public static void paintingWorldEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        float w = (float) (radius * 2);
        ModelFx wall = WORLD_BARRIER.color(0x906C5CE7).scale(w, (float) Math.min(radius * 0.8, 10), w).lifetime(14).fade(0, 10)
                .key(14, pose().scale(1.1f).alpha(0f), Ease.IN);
        ParticleShapes.model(sl, wall, center, 0f, 0f, 0f);
        ParticleShapes.ring(sl, PAGE, center.add(0, 1, 0), radius * 0.8, 16, 0.05);
        sound(sl, center, "painting_world_close", 1.2f, 1.0f);
    }

    // =====================================================================================
    // Key 2: Sight
    // =====================================================================================

    /** True Sight opens: an eye opens before the Reporter and the ring of eyes turns under them, for them alone. */
    public static void trueSight(ServerLevel sl, ServerPlayer player, int ticks, String tag) {
        Vec3 front = player.getEyePosition().add(Spells.flatLook(player).scale(1.2));
        ParticleShapes.modelFor(player, OBSERVATION_EYE.color(0xF0BEB4FF).scale(0.9f).lifetime(18).fade(0, 8)
                .key(0, pose().scale(0.3f).alpha(0f)).key(5, pose().scale(1f).alpha(1f), Ease.OUT_BACK)
                .key(18, pose().scale(1.4f).alpha(0f)), front, player.getYRot() + 180f, 0f, 0f);
        ParticleShapes.modelOnFor(player, SIGHT_RING.color(0x60BEB4FF).scale(3f).lifetime(ticks).fade(8, 10).spin(0.8f).tag(tag),
                player, new Vec3(0, 0.06, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, EYE, player.getEyePosition(), 3, 0.01, 0.03);
        sound(sl, player.getEyePosition(), "true_sight", 0.9f, 1.0f);
    }

    /** Something hidden, seen through True Sight: an outline for the Reporter alone. Call it every 10 ticks. */
    public static void trueSightSees(ServerPlayer viewer, LivingEntity hidden) {
        SpellFx.markFor(viewer, hidden, 0xFFBEB4FF, 12);
    }

    /** A Marionette string or a Damage Link, drawn as a thread for the Reporter alone. Every 10 ticks. */
    public static void trueSightThread(ServerPlayer viewer, Vec3 from, Vec3 to) {
        lineFor(viewer, MARK_DOT.color(0xC0FFE6A0).endColor(0x00FFE6A0).lifetime(12, 0), from, to, 3.0);
    }

    /** An armed Substitute or Death Ward: a small ward over its owner's head, for the Reporter alone. */
    public static void trueSightWard(ServerPlayer viewer, LivingEntity owner) {
        ParticleShapes.modelOnFor(viewer, REVIVAL_WARD.color(0xC0FFE9B0).scale(0.4f).lifetime(12).fade(2, 4).spin(4f),
                owner, new Vec3(0, owner.getBbHeight() + 0.6, 0), 0f, 0f, 0f);
    }

    /** A hidden Trap, Zone or Painting: a small ring of glyphs where it is, for the Reporter alone. */
    public static void trueSightTrap(ServerPlayer viewer, Vec3 at) {
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            ParticleShapes.spawnFor(viewer, MARK_DOT.color(0xFFBEB4FF).lifetime(12, 0), at.add(Math.cos(a) * 0.6, 0.1, Math.sin(a) * 0.6), Vec3.ZERO);
        }
        ParticleShapes.spawnFor(viewer, EYE.color(0xFFBEB4FF), at.add(0, 0.4, 0), Vec3.ZERO);
    }

    public static void trueSightEnd(ServerLevel sl, ServerPlayer player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.spawnFor(player, EYE.color(0xFFBEB4FF), player.getEyePosition().add(player.getLookAngle()), Vec3.ZERO);
    }

    /** Many Angles: eyes open all round the Reporter; the ring flares out to 'radius'. */
    public static void manyAngles(ServerLevel sl, ServerPlayer player, double radius, int ticks, String tag) {
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            Vec3 at = player.getEyePosition().add(Math.cos(a) * 1.4, 0.3 + (i % 2) * 0.4, Math.sin(a) * 1.4);
            ParticleShapes.spawn(sl, EYE.color(0xFF8CD8D0).size(0.16f).endSize(0.16f), at, Vec3.ZERO);
        }
        ParticleShapes.modelOnFor(player, SIGHT_RING.color(0x608CD8D0).scale(2.4f).lifetime(ticks).fade(6, 10).spin(-1.2f).tag(tag),
                player, new Vec3(0, 0.06, 0), 0f, 0f, 0f);
        SpellFx.shockRing(sl, player.position(), radius, 0x608CD8D0, 14, 0);
        sound(sl, player.getEyePosition(), "many_angles", 1.0f, 1.0f);
    }

    /**
     * Many Angles shows one living thing to the Reporter alone: it glows, and a thin line runs from its eyes to what
     * it looks at or hunts, red when that is the Reporter. Call it every 10 ticks.
     */
    public static void manyAnglesLine(ServerPlayer viewer, LivingEntity looker, Vec3 lookingAt, boolean atViewer) {
        SpellFx.markFor(viewer, looker, atViewer ? 0xFFE05050 : 0xFF8CD8D0, 12);
        if (lookingAt != null) {
            lineFor(viewer, MARK_DOT.color(atViewer ? 0xC0E05050 : 0x908CD8D0).lifetime(12, 0), looker.getEyePosition(), lookingAt, 2.0);
        }
    }

    public static void manyAnglesEnd(ServerLevel sl, ServerPlayer player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
    }

    /** Read Intent: a page of what it was made to do opens before the Reporter, a thread to the thing read. */
    public static void readIntent(ServerLevel sl, LivingEntity player, Vec3 thing) {
        Vec3 front = player.getEyePosition().add(Spells.flatLook(player).scale(1.0)).add(Spells.rightOf(player).scale(0.5));
        flash(sl, DOSSIER_PAGE, front, player.getYRot() + 180f, 0xFFFFFFFF, 0.7f, 30);
        ParticleShapes.slashBetween(sl, SIGHT_LINE.lifetime(14), player.getEyePosition(), thing);
        ParticleShapes.burst(sl, WRITING, thing, 6, 0.01, 0.04);
        sound(sl, player.getEyePosition(), "read_intent", 0.9f, wobble(1.0f));
    }

    // =====================================================================================
    // Key 3: Canvas (what each Painting does once seen; the canvases are under Paintings)
    // =====================================================================================

    /** Riot of Colours takes hold: a swirl of vivid paint before the victim's eyes and colour bursting round its head. */
    public static void riot(ServerLevel sl, LivingEntity victim, int ticks, String tag) {
        ParticleShapes.modelOnHead(sl, RIOT_SWIRL.scale(0.7f).lifetime(ticks).fade(4, 10).spin(12f).tag(tag),
                victim, -0.35, 0f, 0f, 0f);
        for (int t = 0; t < ticks; t += 10) {
            int k = t;
            Later.run(sl, t, () -> {
                if (!victim.isAlive()) return;
                ParticleShapes.burst(sl, SPLAT.color(hue(k / 10)), headOf(victim), 3, 0.03, 0.07);
            });
        }
        sound(sl, chestOf(victim), "riot", 1.0f, wobble(1.0f));
    }

    /** Every Madness it gains while it keeps looking: the swirl spins faster and colour drips off it. */
    public static void madness(ServerLevel sl, LivingEntity victim, int madness) {
        ParticleShapes.burst(sl, SPLAT.color(hue(madness)), victim.getEyePosition(), 2 + madness / 2, 0.02, 0.05);
        ParticleShapes.spawn(sl, SPARKLE.color(hue(madness + 3)), headOf(victim).add(0, 0.3, 0), new Vec3(0, 0.03, 0));
    }

    /** At 10 Madness it collapses into slumber: the colours drain out of it in a grey rush. */
    public static void madnessCollapse(ServerLevel sl, LivingEntity victim, String tag) {
        ParticleShapes.clearModels(sl, victim, tag);
        for (int i = 0; i < 6; i++) ParticleShapes.spawn(sl, SPLAT.color(hue(i)), headOf(victim), new Vec3(0, -0.08, 0));
        ParticleShapes.burst(sl, GREY_MOTE, chestOf(victim), 12, 0.02, 0.06);
        sound(sl, chestOf(victim), "madness_collapse", 1.0f, 1.0f);
    }

    public static void riotEnd(ServerLevel sl, LivingEntity victim, String tag) {
        ParticleShapes.clearModels(sl, victim, tag);
    }

    /** Itching House takes hold: mud-like rain falls on the victim, and it scratches and scratches. */
    public static void itching(ServerLevel sl, LivingEntity victim, int ticks, String tag) {
        SpellFx.circleUnder(sl, victim, Math.max(0.6, victim.getBbWidth()), 0x805A4026, ticks, tag);
        for (int t = 0; t < ticks; t += 6) {
            int k = t;
            Later.run(sl, t, () -> {
                if (!victim.isAlive()) return;
                ParticleShapes.spawn(sl, DROP.color(0xFF6A5030).endColor(0x803A2A18), headOf(victim).add(within(Vec3.ZERO, 0.4, 0.6)),
                        new Vec3(0, -0.15, 0));
                if (k % 18 == 0) {
                    ParticleShapes.slash(sl, BRUSH_LINE.color(0xC08A4A30).width(0.06f).arc(40f).radius(0.3f).lifetime(5),
                            chestOf(victim), randomYaw(sl), 30f, 0f);
                }
            });
        }
        sound(sl, chestOf(victim), "itching", 0.9f, wobble(1.0f));
    }

    public static void itchingEnd(ServerLevel sl, LivingEntity victim, String tag) {
        ParticleShapes.clearModels(sl, victim, tag);
    }

    /** Golden Sun: a ring of colours round a small sun on the ground, 'radius' wide, warm light rising, for 'ticks'. */
    public static void goldenSun(ServerLevel sl, Vec3 ground, double radius, int ticks, String tag) {
        floor(sl, GOLDEN_SUN_FIELD, ground, radius, 0xD0FFFFFF, ticks, 0.6f, tag, 0);
        for (int t = 0; t < ticks; t += 10) {
            Later.run(sl, t, () -> {
                ParticleShapes.spawn(sl, GOLD_MOTE, within(ground, radius, 0.2), new Vec3(0, 0.05, 0));
                ParticleShapes.spawn(sl, GOLD_MOTE, within(ground, radius * 0.4, 0.2), new Vec3(0, 0.07, 0));
            });
        }
        sound(sl, ground, "golden_sun", 1.2f, 1.0f);
    }

    /** An ally warmed (every second): soft gold rising off them. */
    public static void sunWarm(ServerLevel sl, LivingEntity ally) {
        ParticleShapes.burst(sl, GOLD_MOTE, chestOf(ally), 3, 0.01, 0.03);
    }

    /** Evil or undead burned (every second): white-gold fire flickers on it. */
    public static void sunBurn(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, SpellFx.FIRE_EMBER, chestOf(target), 6, 0.02, 0.06);
        ParticleShapes.burst(sl, GOLD_MOTE, chestOf(target), 4, 0.02, 0.05);
        if (target.tickCount % 40 < 20) sound(sl, chestOf(target), "sun_burn", 0.6f, wobble(1.0f));
    }

    /** A summon or marionette unmade by the simple exorcism: a bell and a column of light. */
    public static void exorcise(ServerLevel sl, Vec3 at) {
        SpellFx.risingRings(sl, at, 1.0, 2.5, 4, 0xE0FFE9B0);
        ParticleShapes.burst(sl, SPARKLE, at.add(0, 1, 0), 14, 0.03, 0.1);
        sound(sl, at, "exorcise", 1.0f, 1.0f);
    }

    public static void goldenSunEnd(ServerLevel sl, Vec3 ground, String tag) {
        ParticleShapes.clearModels(sl, ground, tag);
        ParticleShapes.ring(sl, GOLD_MOTE, ground.add(0, 0.3, 0), 2.0, 12, 0.05);
    }

    /** Boat in a Storm: the field opens (a dark storm wash on the ground); call stormWave every second. */
    public static void boatInAStorm(ServerLevel sl, Vec3 ground, double radius, int ticks, String tag) {
        floor(sl, PHASE_VEIL, ground, radius, 0xA02A6A78, ticks, 0.3f, tag, 0);
        for (int t = 0; t < ticks; t += 10) {
            Later.run(sl, t, () -> ParticleShapes.spawn(sl, DROP, within(ground, radius, 3.0), new Vec3(0, -0.2, 0)));
        }
        sound(sl, ground, "boat_storm", 1.2f, 1.0f);
    }

    /** A wave rolls out of the canvas, 'radius' blocks, toward 'yaw' (or round, call it four times). */
    public static void stormWave(ServerLevel sl, Vec3 ground, double radius, float yaw) {
        int ticks = 14;
        ModelFx wave = INK_WAVE.scale(3f, 1.6f, 1f).lifetime(ticks).fade(2, 6)
                .key(0, pose().scale(0.6f).alpha(0f))
                .key(4, pose().scale(1f).alpha(1f), Ease.OUT)
                .key(ticks, pose().forward((float) radius).scale(1.1f), Ease.OUT);
        ParticleShapes.model(sl, wave, ground, yaw, 0f, 0f);
        ParticleShapes.ring(sl, DROP, ground.add(0, 0.5, 0), 1.0, 10, radius / ticks);
        sound(sl, ground, "wave", 0.7f, wobble(1.0f));
    }

    public static void boatInAStormEnd(ServerLevel sl, Vec3 ground, String tag) {
        ParticleShapes.clearModels(sl, ground, tag);
    }

    /** Self Portrait: the painter looks through their portrait (an eye opens on the canvas). */
    public static void selfPortraitLook(ServerLevel sl, Vec3 canvas, Vec3 facing) {
        float[] a = facingAngles(facing);
        flash(sl, OBSERVATION_EYE, canvas.add(facing.normalize().scale(0.3)), a[0], 0xF0BEB4FF, 0.8f, 24);
        sound(sl, canvas, "self_portrait", 0.8f, 1.0f);
    }

    /** The portrait repeats a cast at half strength: a brush stroke of light from the canvas toward 'to'. */
    public static void portraitEcho(ServerLevel sl, Vec3 canvas, Vec3 to) {
        ParticleShapes.slashBetween(sl, BRUSH_LINE.color(0xC06C5CE7).core(0xFFBEB4FF).lifetime(8), canvas, to);
        ParticleShapes.burst(sl, VIOLET_MOTE, canvas, 6, 0.02, 0.06);
        sound(sl, canvas, "portrait_echo", 0.7f, wobble(1.0f));
    }

    /** The tick the Giant's Fist strikes the ground after giantsFist(...). */
    public static final int GIANT_FIST_LANDS = 14;

    /** Giant's Fist: the painted giant's fist falls from the sky onto 'ground', 'radius' wide, and smashes down. */
    public static void giantsFist(ServerLevel sl, Vec3 ground, double radius) {
        float size = (float) Math.max(2, radius * 1.6);
        ModelFx fist = GIANT_FIST.scale(size).lifetime(GIANT_FIST_LANDS + 24).fade(4, 14)
                .key(0, pose().up(size * 2.5f).alpha(0f))
                .key(4, pose().up(size * 2f).alpha(1f))
                .key(GIANT_FIST_LANDS, pose().up(0f), Ease.IN)
                .key(GIANT_FIST_LANDS + 6, pose().up(0.3f), Ease.OUT)
                .key(GIANT_FIST_LANDS + 24, pose().up(-size * 0.2f).alpha(0f), Ease.IN);
        ParticleShapes.model(sl, fist, ground, randomYaw(sl), 0f, 0f);
        SpellFx.warningCircle(sl, ground, radius, 0xC0E07A3A, GIANT_FIST_LANDS);
        sound(sl, ground.add(0, size * 2, 0), "giant_fall", 1.6f, 1.0f);
        Later.run(sl, GIANT_FIST_LANDS, () -> {
            SpellFx.shockRing(sl, ground, radius * 1.4, 0xF0E8B07A, 12, 0);
            SpellFx.shockRing(sl, ground, radius * 2.2, 0x90E07A3A, 18, 2);
            ParticleShapes.ring(sl, DUST, ground.add(0, 0.3, 0), 1.0, 24, radius / 14);
            ParticleShapes.burst(sl, ROCK, ground.add(0, 0.3, 0), 24, 0.15, 0.4);
            for (int i = 0; i < 12; i++) ParticleShapes.spawn(sl, SPLAT.color(hue(i)), within(ground, radius, 0.3), new Vec3(0, 0.2, 0));
            sound(sl, ground, "giant_impact", 2.0f, 1.0f);
        });
    }

    // =====================================================================================
    // Key 4: Fiction
    // =====================================================================================

    /** Painted Mark: a brush stroke flicks from the painter to the creature and a circle of paint stays under it. */
    public static void paintedMark(ServerLevel sl, LivingEntity painter, LivingEntity target, int ticks, String tag) {
        ParticleShapes.slashBetween(sl, BRUSH_LINE.color(0xE06C5CE7), handOf(painter, sl), chestOf(target));
        ModelFx mark = PAINTED_MARK.color(0xC06C5CE7).scale((float) Math.max(1.2, target.getBbWidth() * 1.8)).lifetime(ticks)
                .fade(4, 10).spin(2f).tag(tag);
        ParticleShapes.modelOn(sl, mark, target, new Vec3(0, 0.07, 0), randomYaw(sl), 0f, 0f);
        ParticleShapes.burst(sl, SPLAT.color(VIOLET), chestOf(target), 6, 0.03, 0.08);
        sound(sl, chestOf(target), "painted_mark", 0.9f, wobble(1.0f));
    }

    /** The marked one is driven: a violet line from it to where it is sent. */
    public static void markDrive(ServerLevel sl, LivingEntity target, Vec3 to) {
        ParticleShapes.slashBetween(sl, SIGHT_LINE.color(0xC06C5CE7).lifetime(16), chestOf(target), to);
        ParticleShapes.spawn(sl, EYE.color(0xFF6C5CE7).size(0.2f).endSize(0.2f), to.add(0, 0.6, 0), Vec3.ZERO);
        sound(sl, chestOf(target), "mark_drive", 0.8f, wobble(1.0f));
    }

    public static void paintedMarkEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, SPLAT.color(VIOLET), target.position().add(0, 0.2, 0), 4, 0.02, 0.05);
    }

    /**
     * Hide in Painting: a canvas opens on the wall or ground at 'spot' (front looking along 'facing') and the painter
     * steps into it; the picture shows them standing in a doorway. For 'ticks' under 'tag'.
     */
    public static void hideInPainting(ServerLevel sl, LivingEntity painter, Vec3 spot, Vec3 facing, int ticks, String tag) {
        hang(sl, Painting.HIDE_IN_PAINTING, spot, facing, ticks, tag);
        Vec3 from = chestOf(painter);
        for (int i = 0; i < 10; i++) {
            Vec3 at = from.add(within(Vec3.ZERO, 0.4, (Math.random() - 0.5) * 1.4));
            ParticleShapes.spawn(sl, INK_BLOT, at, spot.subtract(at).scale(1 / 10.0));
        }
        sound(sl, spot, "hide_enter", 1.0f, 1.0f);
    }

    /** They step out: the doorway in the picture is empty for a moment, then the canvas fades. */
    public static void hideInPaintingEnd(ServerLevel sl, LivingEntity painter, Vec3 spot, Vec3 facing, String tag) {
        ParticleShapes.clearModels(sl, spot, tag);
        float[] a = facingAngles(facing);
        ModelFx empty = Painting.HIDE_IN_PAINTING_EMPTY.canvas.scale(CANVAS_SCALE).lifetime(24).fade(0, 14);
        ParticleShapes.model(sl, empty, spot, a[0], a[1], 0f);
        ParticleShapes.line(sl, INK_BLOT, spot, chestOf(painter), 3.0);
        sound(sl, spot, "hide_exit", 1.0f, 1.0f);
    }

    /** Painted Face: a calm painted mask settles over the face, then fades into it; a faint wash stays on them. */
    public static void paintedFace(ServerLevel sl, LivingEntity who, int ticks, String tag) {
        ModelFx mask = PAINTED_FACE.scale(0.55f).lifetime(30).fade(4, 14).seenByWearer()
                .key(0, pose().forward(0.6f).alpha(0f))
                .key(10, pose().forward(0f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOnHead(sl, mask, who, -0.5, 0f, 0f, 0f);
        ParticleShapes.burst(sl, BRUSH.color(0xFFECE2C8), who.getEyePosition(), 5, 0.01, 0.04);
        SpellFx.circleUnder(sl, who, 0.6, 0x40ECE2C8, ticks, tag);
        sound(sl, who.getEyePosition(), "painted_face", 0.8f, wobble(1.0f));
    }

    public static void paintedFaceEnd(ServerLevel sl, LivingEntity who, String tag) {
        ParticleShapes.clearModels(sl, who, tag);
        ParticleShapes.burst(sl, PAGE, who.getEyePosition(), 3, 0.02, 0.05);
    }

    /** The two figures of fiction. */
    public enum Figure {
        KNIGHT, ARCHER
    }

    /** The tick a manifested figure is whole after manifest(...). */
    public static final int MANIFEST_READY = 20;

    /**
     * A figure of fiction steps into reality over its construct (worn, turning with it) for 'ticks': ink and paper
     * swirl up and it fills in from transparent. 'share' is its power (0.5 at Sequence 6 ... 1.0 at Sequence 0): the
     * figure is as solid as it is strong. Call knightStrike / archerShot when it attacks.
     */
    public static void manifest(ServerLevel sl, LivingEntity construct, Figure figure, float share, int ticks, String tag) {
        float solid = 0.45f + 0.55f * Math.max(0f, Math.min(1f, (share - 0.5f) / 0.5f));
        ModelFx look = (figure == Figure.KNIGHT ? KNIGHT.scale(1.25f) : ARCHER.scale(1.2f))
                .color(withAlpha(0xFFFFFFFF, (int) (255 * solid))).fade(0, 14)
                .key(0, pose().alpha(0f).scale(0.9f))
                .key(MANIFEST_READY, pose().alpha(1f).scale(1f), Ease.OUT);
        if (solid < 1f) look = look.seeThrough();
        wear(sl, construct, look, ticks, tag);
        ParticleShapes.spiral(sl, PAGE, construct.position().add(0, 0.2, 0), 3, 18, 0.3, 1.4, 0.8, 0.06);
        ParticleShapes.burst(sl, INK_BLOT, chestOf(construct), 10, 0.02, 0.07);
        sound(sl, chestOf(construct), "manifest", 1.0f, figure == Figure.KNIGHT ? 0.9f : 1.1f);
    }

    /** The Knight strikes: it swings (its second step) and ink sprays off the blow. */
    public static void knightStrike(ServerLevel sl, LivingEntity knight, LivingEntity target, String tag, int ticksLeft) {
        ModelFx swing = KNIGHT.scale(1.25f).fade(0, 14)
                .key(0, pose().frame(0f)).key(4, pose().frame(1f), Ease.IN).key(12, pose().frame(0f), Ease.OUT);
        wear(sl, knight, swing, ticksLeft, tag);
        ParticleShapes.slash(sl, BRUSH_LINE.color(0xE014100E).arc(120f).radius(1.4f).width(0.2f).lifetime(6), chestOf(knight),
                knight.getYRot(), 10f, 0f);
        ParticleShapes.burst(sl, INK_BLOT, chestOf(target), 6, 0.03, 0.08);
        sound(sl, chestOf(target), "knight_strike", 0.9f, wobble(1.0f));
    }

    /** The Archer looses: it draws (its second step) and an inked arrow streaks to the target. */
    public static void archerShot(ServerLevel sl, LivingEntity archer, Vec3 to, String tag, int ticksLeft) {
        ModelFx draw = ARCHER.scale(1.2f).fade(0, 14)
                .key(0, pose().frame(0f)).key(6, pose().frame(1f), Ease.OUT).key(8, pose().frame(0f), Ease.IN);
        wear(sl, archer, draw, ticksLeft, tag);
        Later.run(sl, 7, () -> {
            if (!archer.isAlive()) return;
            Vec3 from = archer.getEyePosition();
            ParticleShapes.slashBetween(sl, BRUSH_LINE.width(0.06f).lifetime(5), from, to);
            ParticleShapes.burst(sl, PAGE.size(0.06f), to, 3, 0.02, 0.05);
            sound(sl, from, "archer_shot", 0.9f, wobble(1.0f));
        });
    }

    /** A figure of fiction goes back into the page: it fades into ink and paper. */
    public static void manifestFade(ServerLevel sl, LivingEntity construct, String tag) {
        ParticleShapes.clearModels(sl, construct, tag);
        ParticleShapes.burst(sl, PAGE, chestOf(construct), 8, 0.02, 0.06);
        ParticleShapes.burst(sl, INK_SMOKE, chestOf(construct), 8, 0.01, 0.04);
        sound(sl, chestOf(construct), "manifest_fade", 0.9f, 1.0f);
    }

    /** Fantasy Double: a double of ink and light forms where the Observer stood; a thread ties it to them. */
    public static void fantasyDouble(ServerLevel sl, LivingEntity player, LivingEntity dbl, int ticks, String tag) {
        ParticleShapes.spiral(sl, VIOLET_MOTE, dbl.position().add(0, 0.2, 0), 2, 20, 0.2, 1.2, 1.0, 0.05);
        SpellFx.circleUnder(sl, dbl, 0.8, 0x60BEB4FF, ticks, tag);
        flash(sl, Painting.FANTASY.canvas, chestOf(dbl), player.getYRot() + 180f, 0xFFFFFFFF, CANVAS_SCALE * 0.8f, 18);
        sound(sl, chestOf(dbl), "fantasy_double", 1.0f, 1.0f);
    }

    /** The connection to the true body, faint (call every second for whoever can see connections, or for all). */
    public static void fantasyLink(ServerLevel sl, LivingEntity dbl, LivingEntity player) {
        ParticleShapes.line(sl, MARK_DOT.color(0x60BEB4FF).lifetime(14, 0), chestOf(dbl), chestOf(player), 0.8);
    }

    public static void fantasyDoubleEnd(ServerLevel sl, LivingEntity dbl, String tag) {
        ParticleShapes.clearModels(sl, dbl, tag);
        ParticleShapes.burst(sl, VIOLET_MOTE, chestOf(dbl), 14, 0.02, 0.08);
        ParticleShapes.burst(sl, PAGE, chestOf(dbl), 5, 0.02, 0.06);
    }

    // =====================================================================================
    // Key 5: Dimension
    // =====================================================================================

    /** Phase: the Pixie slips between the Spirit World and reality. Seen by all: a ripple and they are gone. */
    public static void phaseIn(ServerLevel sl, LivingEntity player) {
        floor(sl, PHASE_VEIL, player.position(), 1.6, 0xC08CD8D0, 20, 0f, null, 0);
        ParticleShapes.burst(sl, SHARD.color(0xFFE8FFFC), chestOf(player), 10, 0.03, 0.1);
        ParticleShapes.burst(sl, TEAL_MOTE, chestOf(player), 12, 0.02, 0.06);
        sound(sl, chestOf(player), "phase_in", 1.0f, 1.0f);
    }

    /** While phased (every 10 ticks): ripples of the Spirit World round them, seen by the phased one alone. */
    public static void phaseTick(ServerPlayer phased) {
        Vec3 at = phased.position().add(0, 0.1, 0);
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * 2 * i / 10;
            ParticleShapes.spawnFor(phased, TEAL_MOTE.lifetime(14, 0), at.add(Math.cos(a) * 0.9, 0, Math.sin(a) * 0.9),
                    new Vec3(Math.cos(a) * 0.04, 0.01, Math.sin(a) * 0.04));
        }
        ParticleShapes.spawnFor(phased, GLINT, phased.getEyePosition().add(phased.getLookAngle().scale(1.5)).add(within(Vec3.ZERO, 1.0, 0)),
                Vec3.ZERO);
    }

    /** They come back into reality. */
    public static void phaseOut(ServerLevel sl, LivingEntity player) {
        SpellFx.closingRing(sl, player.position(), 1.6, 0xC08CD8D0, 8, 0);
        ParticleShapes.burst(sl, SHARD.color(0xFFE8FFFC), chestOf(player), 6, 0.02, 0.06);
        sound(sl, chestOf(player), "phase_out", 1.0f, 1.0f);
    }

    /** The tick the wings are spread after pixieWings(...). */
    public static final int WINGS_READY = 8;
    /** One wingbeat. A worn model holds 64 keys, so pixieWings beats for WINGS_KEPT ticks; call pixieWingsKeep after. */
    private static final int WING_BEAT = 4;
    public static final int WINGS_KEPT = WINGS_READY + 28 * WING_BEAT;

    /** Pixie Wings: translucent dragonfly wings unfold from the back and beat fast; they last 'ticks' under 'tag'. */
    public static void pixieWings(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        ModelFx wings = beating(PIXIE_WINGS.fade(0, 10).key(0, pose().frame(1f).scale(0.2f).alpha(0f))
                .key(WINGS_READY, pose().frame(0f).scale(1f).alpha(1f), Ease.OUT_BACK), WINGS_READY);
        wear(sl, player, wings, ticks, tag);
        ParticleShapes.burst(sl, GLINT, chestOf(player), 10, 0.02, 0.08);
        sound(sl, chestOf(player), "wings_unfold", 1.0f, 1.0f);
    }

    /** Every WINGS_KEPT ticks while they fly: the wings go on beating ('ticksLeft' of the flight remain). */
    public static void pixieWingsKeep(ServerLevel sl, LivingEntity player, int ticksLeft, String tag) {
        wear(sl, player, beating(PIXIE_WINGS.fade(0, 10), 0), ticksLeft, tag);
        if (ThreadLocalRandom.current().nextInt(3) == 0) sound(sl, chestOf(player), "wings_buzz", 0.5f, wobble(1.0f));
    }

    private static ModelFx beating(ModelFx wings, int from) {
        for (int i = 0; i < 28; i++) {
            int t = from + i * WING_BEAT;
            wings = wings.key(t + WING_BEAT / 2, pose().frame(2f), Ease.IN_OUT).key(t + WING_BEAT, pose().frame(0f), Ease.IN_OUT);
        }
        return wings;
    }

    /** The wings fold away (pressed again, or the time is up). */
    public static void pixieWingsEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ModelFx folding = PIXIE_WINGS.lifetime(10).fade(0, 8).key(0, pose().frame(1f)).key(10, pose().frame(2f).scale(0.3f).alpha(0f), Ease.IN);
        ParticleShapes.modelOnTurning(sl, folding, player, Vec3.ZERO, 0f, 0f, 0f);
        ParticleShapes.burst(sl, GLINT, chestOf(player), 6, 0.02, 0.05);
        sound(sl, chestOf(player), "wings_fold", 0.8f, 1.0f);
    }

    /** Painted Lightning: bolts painted on the sleeve leap from the hand to each target at once. */
    public static void paintedLightning(ServerLevel sl, LivingEntity player, List<Vec3> to) {
        Vec3 hand = handOf(player, sl);
        ParticleShapes.burst(sl, SPLAT.color(0xFFFFD83C), hand, 6, 0.02, 0.06);
        for (Vec3 end : to) {
            beam(sl, PAINTED_BOLT, hand, end, 0.6f, 0xFFFFFFFF, 8);
            ParticleShapes.slashBetween(sl, BOLT_LINE, hand, end);
            ParticleShapes.burst(sl, SPARK.color(0xFFFFF4A0), end, 10, 0.05, 0.15);
        }
        sound(sl, hand, "painted_lightning", 1.3f, wobble(1.0f));
    }

    /** A bolt jumps on to a second enemy. */
    public static void boltJump(ServerLevel sl, Vec3 from, Vec3 to) {
        beam(sl, PAINTED_BOLT, from, to, 0.4f, 0xFFFFFFFF, 6);
        ParticleShapes.burst(sl, SPARK.color(0xFFFFF4A0), to, 6, 0.04, 0.1);
    }

    /** The tick the traveller steps out of the far rift after passage(...). */
    public static final int PASSAGE_ARRIVES = 6;

    /** Dimensional Passage: a rift tears open where they stand and another where they come out. */
    public static void passage(ServerLevel sl, LivingEntity who, Vec3 from, Vec3 to) {
        float yaw = yawToward(from, to);
        ModelFx rift = RIFT.scale(1.2f, 2.4f, 1.2f).lifetime(PASSAGE_ARRIVES + 14).fade(0, 10)
                .key(0, pose().scale(0.1f)).key(3, pose().scale(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, rift, from, yaw + 90f, 0f, 0f);
        ParticleShapes.model(sl, rift.delay(PASSAGE_ARRIVES - 3), to, yaw + 90f, 0f, 0f);
        ParticleShapes.burst(sl, SHARD, from.add(0, 1, 0), 10, 0.04, 0.1);
        ParticleShapes.slashBetween(sl, SHADOW_TRAIL.lifetime(PASSAGE_ARRIVES + 2), from.add(0, 1, 0), to.add(0, 1, 0));
        Later.run(sl, PASSAGE_ARRIVES, () -> ParticleShapes.burst(sl, SHARD, to.add(0, 1, 0), 10, 0.04, 0.1));
        sound(sl, from, "passage", 1.0f, wobble(1.0f));
    }

    /** Hostel: a door of the Visitor's own Dimension opens at the creature and it is drawn into their chest. */
    public static void hostelLodge(ServerLevel sl, LivingEntity player, LivingEntity guest) {
        Vec3 at = chestOf(guest);
        flash(sl, HOSTEL_DOOR, at, yawToward(at, player.position()), 0xFFFFFFFF, Math.max(1.2f, guest.getBbHeight()), 20);
        for (int i = 0; i < 16; i++) {
            Vec3 from = at.add(within(Vec3.ZERO, 0.6, (Math.random() - 0.5)));
            ParticleShapes.spawn(sl, VIOLET_MOTE.lifetime(14, 0), from, chestOf(player).subtract(from).scale(1 / 14.0));
        }
        sound(sl, at, "hostel_lodge", 1.0f, 1.0f);
    }

    /** A guest is let out at 'at'. */
    public static void hostelRelease(ServerLevel sl, LivingEntity player, Vec3 at) {
        flash(sl, HOSTEL_DOOR, at.add(0, 1, 0), yawToward(at, player.position()), 0xFFFFFFFF, 1.6f, 18);
        ParticleShapes.line(sl, VIOLET_MOTE, chestOf(player), at.add(0, 1, 0), 2.0);
        sound(sl, at, "hostel_release", 1.0f, 1.0f);
    }

    /** The tick the Shadow Form is whole after shadowForm(...). */
    public static final int SHADOW_READY = 20;

    /**
     * Shadow Form: the Dimensional Shadow closes over the Visitor (worn) for 'ticks'. complete = from Dimensional
     * Shadow (2): darker, its seams brighter. Leave shadowAfterimage every few ticks while they move.
     */
    public static void shadowForm(ServerLevel sl, LivingEntity player, int ticks, boolean complete, String tag) {
        ModelFx body = SHADOW.scale(complete ? 1.1f : 1.05f).color(complete ? 0xFFFFFFFF : 0xD0FFFFFF).fade(0, 16)
                .aura(complete ? 0x606C5CE7 : 0x306C5CE7, 0.1f, 3)
                .key(0, pose().scale(0.4f).alpha(0f))
                .key(SHADOW_READY, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        wear(sl, player, body, ticks, tag);
        floor(sl, SHADOW_CIRCLE, player.position(), 1.6, 0xFFFFFFFF, SHADOW_READY + 10, 2f, null, 0);
        ParticleShapes.spiral(sl, SHARD, player.position().add(0, 0.2, 0), 3, 24, 0.3, 1.6, 1.0, 0.08);
        ParticleShapes.burst(sl, DARK, chestOf(player), 14, 0.02, 0.06);
        sound(sl, chestOf(player), "shadow_form", 1.4f, complete ? 0.85f : 1.0f);
    }

    /** An afterimage of the Shadow left where they were, fading (every 4 ticks while they move). */
    public static void shadowAfterimage(ServerLevel sl, LivingEntity player) {
        ModelFx ghost = SHADOW.scale(1.05f).color(0x806C5CE7).lifetime(10).fade(0, 10);
        ParticleShapes.model(sl, ghost, player.position(), player.getYRot(), 0f, 0f);
    }

    /** A level of Ascension in Shadow Form (the "Dimension n/11" counter rises). */
    public static void ascension(ServerLevel sl, LivingEntity player, int level) {
        SpellFx.risingRings(sl, player.position(), 1.2, 2.4, Math.min(5, 1 + level / 3), 0xC0BEB4FF);
        ParticleShapes.burst(sl, SHARD, player.position().add(0, 2.2, 0), 4 + level, 0.02, 0.06);
        sound(sl, chestOf(player), "ascension", 0.8f, 0.8f + level * 0.05f);
    }

    public static void shadowFormEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.burst(sl, DARK, chestOf(player), 12, 0.02, 0.06);
        ParticleShapes.burst(sl, SHARD, chestOf(player), 8, 0.02, 0.06);
        sound(sl, chestOf(player), "shadow_form_end", 1.0f, 1.0f);
    }

    /** The weakness after an incomplete form ended at full madness: a dull haze clings for 'ticks'. */
    public static void shadowWeakness(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        SpellFx.circleUnder(sl, player, 0.7, 0x608A84B0, ticks, tag);
        for (int t = 0; t < ticks; t += 20) {
            Later.run(sl, t, () -> {
                if (player.isAlive()) ParticleShapes.spawn(sl, DARK.color(0x608A84B0), chestOf(player), new Vec3(0, -0.01, 0));
            });
        }
    }

    /** From Observer (1): the faint, unattainably tall figure stands over them in Shadow Form, for 'ticks'. */
    public static void observerForm(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        ModelFx figure = OBSERVER_FIGURE.scale(0.9f).color(0xC0FFFFFF).fade(0, 20)
                .key(0, pose().scale(0.6f).alpha(0f))
                .key(SHADOW_READY, pose().scale(1f).alpha(1f), Ease.OUT);
        wear(sl, player, figure, ticks, tag);
        SpellFx.risingRings(sl, player.position(), 1.4, 4.0, 5, 0x80FFE9B0);
        ParticleShapes.burst(sl, GOLD_MOTE, player.position().add(0, 3.6, 0), 12, 0.02, 0.06);
    }

    public static void observerFormEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.burst(sl, GOLD_MOTE, player.position().add(0, 2, 0), 12, 0.02, 0.06);
    }

    // =====================================================================================
    // Key 6: Strings
    // =====================================================================================

    /** The tick the strings are played (the damage) after tautChord(...): pulled for 8, held for 30. */
    public static final int TAUT_PULLS = 8;
    public static final int TAUT_PLAYS = TAUT_PULLS + 30;

    /** Taut Chord: pale-gold strings from the String Player to everyone caught, humming, until TAUT_PLAYS. */
    public static void tautChord(ServerLevel sl, LivingEntity player, List<LivingEntity> caught) {
        for (LivingEntity target : caught) {
            SpellFx.tether(sl, STRING, () -> SpellFx.handOf(player, sl), () -> SpellFx.middleOf(target, sl), TAUT_PLAYS);
            ParticleShapes.burst(sl, NOTES, chestOf(target), 2, 0.01, 0.03);
        }
        sound(sl, chestOf(player), "taut_chord_draw", 1.0f, 1.0f);
    }

    /** The strings are played: each one flashes and rings on whoever it holds. */
    public static void tautChordPlay(ServerLevel sl, LivingEntity player, List<LivingEntity> caught) {
        for (LivingEntity target : caught) {
            beam(sl, STRING_BEAM, handOf(player, sl), chestOf(target), 0.5f, 0xFFFFE6A0, 6);
            SpellFx.shockRing(sl, target.position(), 1.4, 0xD0FFE6A0, 8, 0);
            ParticleShapes.burst(sl, NOTES, chestOf(target), 4, 0.03, 0.08);
        }
        sound(sl, chestOf(player), "taut_chord_play", 1.3f, 1.0f);
    }

    /** Resonance: one enemy's string is plucked and a trembling ring stands round it, for 'ticks'. */
    public static void resonance(ServerLevel sl, LivingEntity player, LivingEntity target, int ticks, String tag) {
        ParticleShapes.slashBetween(sl, STRING.lifetime(8), handOf(player, sl), chestOf(target));
        ModelFx ring = RESONANCE_RING.color(0xC0FFE6A0).scale((float) Math.max(1.6, target.getBbWidth() * 2.2)).lifetime(ticks)
                .fade(4, 10).spin(6f).tag(tag);
        ParticleShapes.modelOn(sl, ring, target, new Vec3(0, target.getBbHeight() * 0.5, 0), randomYaw(sl), -90f, 0f);
        sound(sl, chestOf(target), "resonance", 1.0f, 1.0f);
    }

    /** A Resonance is added (count = how many now): notes over its head, one per Resonance. */
    public static void resonanceAdd(ServerLevel sl, LivingEntity target, int count, int ticks, String tag) {
        SpellFx.overHead(sl, target, OBSERVATION_EYE.color(0xFFFFE6A0).scale(0.22f).lifetime(ticks).spin(5f), count, tag + "_n");
        ParticleShapes.burst(sl, NOTES, headOf(target), 2, 0.02, 0.05);
        sound(sl, chestOf(target), "resonance_add", 0.7f, 0.9f + count * 0.1f);
    }

    /** It bursts: 'count' Resonances ring out in a circle 'radius' wide. */
    public static void resonanceBurst(ServerLevel sl, LivingEntity target, double radius, int count, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.clearModels(sl, target, tag + "_n");
        for (int i = 0; i < Math.max(1, count); i++) SpellFx.shockRing(sl, target.position(), radius, 0xE0FFE6A0, 8 + i * 2, i * 2);
        ParticleShapes.burst(sl, NOTES, chestOf(target), 6 + count * 2, 0.05, 0.15);
        sound(sl, chestOf(target), "resonance_burst", 1.2f, 1.0f);
    }

    /** Harmony: a chord of gold under the String Player and a thread of it to every ally it reaches. */
    public static void harmony(ServerLevel sl, LivingEntity player, double radius, List<LivingEntity> allies) {
        floor(sl, CHORD_RING, player.position(), 2.0, 0xC0FFE6A0, 40, 1.5f, null, 0);
        SpellFx.steadyRing(sl, player.position(), radius, 0x80FFE6A0, 16, 0);
        for (LivingEntity ally : allies) {
            ParticleShapes.line(sl, NOTES.lifetime(10, 2), chestOf(player), chestOf(ally), 1.0);
            ParticleShapes.burst(sl, GOLD_MOTE, chestOf(ally), 6, 0.02, 0.05);
        }
        sound(sl, player.position(), "harmony", 1.2f, 1.0f);
    }

    /** The next cast of this ally is free and stronger: a small chord turns over their head until it is spent. */
    public static void harmonyArmed(ServerLevel sl, LivingEntity ally, int ticks, String tag) {
        ParticleShapes.modelOn(sl, CHORD_RING.color(0xB0FFE6A0).scale(0.7f).lifetime(ticks).fade(4, 8).spin(3f).tag(tag),
                ally, new Vec3(0, ally.getBbHeight() + 0.35, 0), 0f, 0f, 0f);
    }

    public static void harmonySpent(ServerLevel sl, LivingEntity ally, String tag) {
        ParticleShapes.clearModels(sl, ally, tag);
        ParticleShapes.burst(sl, NOTES, headOf(ally), 4, 0.03, 0.07);
    }

    /** Dissonance: a broken chord spreads out 'radius' blocks, notes knocked askew. */
    public static void dissonance(ServerLevel sl, LivingEntity player, double radius) {
        ModelFx broken = DISSONANCE_RING.color(0xD0BEB4FF).lifetime(16).fade(0, 10)
                .key(0, pose().scale(1f)).key(16, pose().scale((float) (radius * 2)), Ease.OUT);
        ParticleShapes.model(sl, broken, player.position().add(0, 0.1, 0), randomYaw(sl), 0f, 0f);
        ParticleShapes.ring(sl, BROKEN_NOTE, player.position().add(0, 1.0, 0), 1.0, 16, radius / 16);
        sound(sl, player.position(), "dissonance", 1.3f, 1.0f);
    }

    /** Someone the broken chord shakes: broken notes round their head. */
    public static void dissonant(ServerLevel sl, LivingEntity victim) {
        ParticleShapes.burst(sl, BROKEN_NOTE, headOf(victim), 4, 0.02, 0.06);
    }

    /** Shadow From Above: a blade of shadow appears inside the enemy with no warning and is gone. */
    public static void shadowFromAbove(ServerLevel sl, LivingEntity target) {
        float h = (float) Math.max(1.6, target.getBbHeight() * 1.3);
        ModelFx blade = SHADOW_BLADE.scale(1f, h / 2f, 1f).lifetime(10).fade(0, 6)
                .key(0, pose().up(-0.3f).scale(1.2f)).key(10, pose().up(0.4f).alpha(0f), Ease.OUT);
        ParticleShapes.model(sl, blade, target.position(), randomYaw(sl), 0f, (float) ((Math.random() - 0.5) * 20));
        ParticleShapes.burst(sl, SHARD, chestOf(target), 10, 0.04, 0.12);
        ParticleShapes.burst(sl, DARK, chestOf(target), 6, 0.02, 0.05);
        sound(sl, chestOf(target), "shadow_blade", 1.2f, wobble(1.0f));
    }

    /** Aimed at the ground: blades of shadow rise from below across a circle 'radius' wide. */
    public static void shadowBladesRise(ServerLevel sl, Vec3 ground, double radius) {
        floor(sl, SHADOW_CIRCLE, ground, radius, 0xFFFFFFFF, 30, 3f, null, 0);
        for (int i = 0; i < (int) (radius * 2); i++) {
            Vec3 at = within(ground, radius, 0);
            Later.run(sl, 4 + i, () -> {
                ModelFx blade = SHADOW_BLADE.scale(0.7f, 0.9f, 0.7f).lifetime(14).fade(0, 8)
                        .key(0, pose().up(-1.8f)).key(3, pose().up(0f), Ease.OUT_BACK).key(14, pose().up(-1.8f), Ease.IN);
                ParticleShapes.model(sl, blade, at, randomYaw(sl), 0f, (float) ((Math.random() - 0.5) * 30));
                ParticleShapes.spawn(sl, SHARD, at.add(0, 0.5, 0), new Vec3(0, 0.1, 0));
            });
        }
        sound(sl, ground, "shadow_rise", 1.3f, 1.0f);
    }

    /** The tick the String Player stands at the far side after foldSpace(...). */
    public static final int FOLD_ARRIVES = 4;

    /** Fold Space: the plane folds like paper from 'from' to 'to'; creases stand along the line. */
    public static void foldSpace(ServerLevel sl, LivingEntity player, Vec3 from, Vec3 to) {
        Vec3 way = to.subtract(from);
        float yaw = yawToward(from, to);
        int creases = Math.max(2, (int) (way.length() / 4));
        for (int i = 0; i <= creases; i++) {
            Vec3 at = from.add(way.scale((double) i / creases));
            ModelFx crease = FOLD_CREASE.color(0xA0D8CCFF).scale(2.2f).delay(i).lifetime(14).fade(0, 8)
                    .key(0, pose().spin(0f).alpha(0f)).key(3, pose().alpha(1f)).key(14, pose().spin(i % 2 == 0 ? 80f : -80f), Ease.IN);
            ParticleShapes.model(sl, crease, at, yaw + 90f, 0f, 0f);
        }
        ParticleShapes.slashBetween(sl, SHADOW_TRAIL.color(0xA0D8CCFF), from.add(0, 1, 0), to.add(0, 1, 0));
        ParticleShapes.line(sl, FOLD, from.add(0, 1, 0), to.add(0, 1, 0), 1.0);
        sound(sl, from, "fold_space", 1.2f, wobble(1.0f));
    }

    /** Someone along the fold is struck and held as if folded in. */
    public static void foldStruck(ServerLevel sl, LivingEntity victim) {
        ParticleShapes.burst(sl, FOLD, chestOf(victim), 6, 0.03, 0.08);
        SpellFx.closingRing(sl, victim.position(), 1.4, 0xC0D8CCFF, 8, 0);
    }

    // =====================================================================================
    // Key 7: Sublunary
    // =====================================================================================

    /** Foregone Loss: the ending is written. A quill stroke to the enemy and a circle of writing under it for 'ticks'. */
    public static void foregoneLoss(ServerLevel sl, LivingEntity player, LivingEntity target, int ticks, String tag) {
        ParticleShapes.slashBetween(sl, QUILL, handOf(player, sl), chestOf(target));
        ModelFx script = FOREGONE_SCRIPT.color(0xD0C83030).scale((float) Math.max(2.0, target.getBbWidth() * 2.6)).lifetime(ticks)
                .fade(6, 10).spin(-1.5f).tag(tag)
                .key(0, pose().scale(0.3f).alpha(0f)).key(12, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, script, target, new Vec3(0, 0.07, 0), randomYaw(sl), 0f, 0f);
        ParticleShapes.burst(sl, WRITING.color(0xFFE05050), headOf(target), 6, 0.02, 0.05);
        sound(sl, chestOf(target), "foregone_write", 1.1f, 1.0f);
    }

    /** The ending comes true: a toll, the writing flares red and collapses inward. */
    public static void foregoneFall(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        SpellFx.closingRing(sl, target.position(), 2.4, 0xF0C83030, 10, 0);
        ParticleShapes.burst(sl, WRITING.color(0xFFE05050), chestOf(target), 12, 0.04, 0.1);
        sound(sl, chestOf(target), "foregone_fall", 1.3f, 1.0f);
    }

    /** It ended above the line and was spared: the writing simply fades. */
    public static void foregoneSpared(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, PAGE, chestOf(target), 4, 0.02, 0.05);
    }

    /** Foregone Survival: the ending of an ally is written the other way: gold writing, a ward over their head. */
    public static void foregoneSurvival(ServerLevel sl, LivingEntity player, LivingEntity ally, int ticks, String tag) {
        if (ally != player) ParticleShapes.slashBetween(sl, QUILL.color(0xE0FFD66E).core(0xFFFFFFFF), handOf(player, sl), chestOf(ally));
        floor(sl, FOREGONE_SCRIPT, ally.position(), 1.2, 0xD0FFD66E, 30, 2f, null, 0);
        ParticleShapes.modelOn(sl, REVIVAL_WARD.color(0x90FFD66E).scale(0.35f).lifetime(ticks).fade(6, 10).spin(2f).tag(tag),
                ally, new Vec3(0, ally.getBbHeight() + 0.5, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, GOLD_MOTE, chestOf(ally), 10, 0.02, 0.06);
        sound(sl, chestOf(ally), "foregone_survival", 1.1f, 1.0f);
    }

    /** A death that does not happen (a ward spent): a burst of gold and they rise. */
    public static void wardSpent(ServerLevel sl, LivingEntity who, String tag) {
        ParticleShapes.clearModels(sl, who, tag);
        SpellFx.risingRings(sl, who.position(), 1.2, 2.4, 4, 0xE0FFE9B0);
        ParticleShapes.burst(sl, GOLD_MOTE, chestOf(who), 20, 0.04, 0.12);
        ParticleShapes.burst(sl, SPARKLE, chestOf(who), 10, 0.03, 0.08);
        sound(sl, chestOf(who), "ward_spent", 1.2f, 1.0f);
    }

    /** Higher Revival's ritual (8 seconds, hidden in Phase): the folded ward turns over them, a drone rises. */
    public static void revivalRitual(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        ModelFx ward = REVIVAL_WARD.color(0xC0FFE9B0).scale(1.2f).lifetime(ticks).fade(10, 6).spin(1.5f).tag(tag)
                .key(0, pose().scale(0.2f).alpha(0f)).key(ticks, pose().scale(1f).alpha(1f), Ease.IN_OUT);
        ParticleShapes.modelOn(sl, ward, player, new Vec3(0, player.getBbHeight() + 0.8, 0), 0f, 0f, 0f);
        for (int t = 0; t < ticks; t += 10) {
            Later.run(sl, t, () -> {
                if (player.isAlive()) ParticleShapes.spawn(sl, GOLD_MOTE, within(player.position(), 1.2, 0.2), new Vec3(0, 0.08, 0));
            });
        }
        sound(sl, chestOf(player), "revival_ritual", 1.0f, 1.0f);
    }

    /** The ritual is done: the ward folds into them (it is not shown again until it is spent). */
    public static void revivalArmed(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.line(sl, GOLD_MOTE, player.position().add(0, player.getBbHeight() + 0.8, 0), chestOf(player), 6.0);
        SpellFx.closingRing(sl, player.position(), 1.4, 0xC0FFE9B0, 8, 0);
    }

    /** They would have died and return: unfolding out of a higher Dimension at 'at'. */
    public static void higherRevival(ServerLevel sl, LivingEntity player, Vec3 at) {
        flash(sl, REVIVAL_WARD, at.add(0, 1, 0), randomYaw(sl), 0xF0FFE9B0, 2.0f, 30);
        SpellFx.risingRings(sl, at, 1.4, 3.0, 5, 0xE0FFE9B0);
        ParticleShapes.burst(sl, SHARD.color(0xFFFFF4D0), at.add(0, 1, 0), 16, 0.04, 0.12);
        sound(sl, at, "higher_revival", 1.4f, 1.0f);
    }

    /** The tick the Sublunary Eye is open after bloom(...) or fallSilent(...) begins it. */
    public static final int EYE_OPEN = 20;

    /** The vast eye over the Sublunary Eye's head, opening (or closing) over EYE_OPEN ticks, for 'ticks'. */
    private static void sublunaryEye(ServerLevel sl, LivingEntity player, int ticks, boolean opening, String tag) {
        ModelFx eye = SUBLUNARY_EYE.scale(4f).lifetime(ticks).fade(6, 16).tag(tag)
                .key(0, pose().frame(opening ? 0f : 2f).alpha(0f))
                .key(6, pose().alpha(1f))
                .key(EYE_OPEN, pose().frame(opening ? 2f : 0f));
        ParticleShapes.modelOn(sl, eye, player, new Vec3(0, player.getBbHeight() + 4.5, 0), 0f, -90f + 25f, 0f);
    }

    /**
     * Bloom: "When I see, the flowers bloom." The vast eye opens over the Sublunary Eye, rays of gold fall, and
     * flowers open under them; call bloomOn for every ally in sight each second.
     */
    public static void bloom(ServerLevel sl, LivingEntity player, double radius, int ticks, String tag) {
        sublunaryEye(sl, player, ticks, true, tag);
        SpellFx.shockRing(sl, player.position(), radius, 0x90FFE9B0, 20, EYE_OPEN);
        Later.run(sl, EYE_OPEN, () -> {
            ParticleShapes.burst(sl, GOLD_MOTE, player.position().add(0, 4, 0), 30, 0.05, 0.2);
            bloomOn(sl, player);
        });
        sound(sl, player.position(), "bloom", 1.6f, 1.0f);
    }

    /** An ally seen while Bloom lasts (every second): flowers open round their feet, petals and gold rise. */
    public static void bloomOn(ServerLevel sl, LivingEntity ally) {
        for (int i = 0; i < 3; i++) {
            Vec3 at = within(ally.position(), 1.4, 0.02);
            ModelFx flower = FLOWER.scale(0.9f + (float) Math.random() * 0.5f).lifetime(40).fade(0, 12)
                    .key(0, pose().frame(0f).scale(0.2f)).key(12, pose().frame(2f).scale(1f), Ease.OUT_BACK);
            ParticleShapes.model(sl, flower, at, randomYaw(sl), 0f, 0f);
        }
        ParticleShapes.burst(sl, PETAL, chestOf(ally), 4, 0.02, 0.05);
        ParticleShapes.spawn(sl, BUD, headOf(ally), new Vec3(0, 0.02, 0));
        if (ThreadLocalRandom.current().nextInt(3) == 0) sound(sl, chestOf(ally), "bloom_flower", 0.5f, wobble(1.0f));
    }

    public static void bloomEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.burst(sl, PETAL, player.position().add(0, 2, 0), 14, 0.03, 0.08);
    }

    /**
     * Fall Silent: "When I do not, the flowers and the heart fall silent together." The vast eye closes over the
     * Sublunary Eye and colour drains from the world round them for 'ticks'.
     */
    public static void fallSilent(ServerLevel sl, LivingEntity player, double radius, int ticks, String tag) {
        sublunaryEye(sl, player, ticks, false, tag);
        SpellFx.steadyRing(sl, player.position(), radius, 0x808A8A96, 30, 0);
        ParticleShapes.ring(sl, GREY_MOTE, player.position().add(0, 0.4, 0), 2.0, 30, radius / 40);
        sound(sl, player.position(), "fall_silent", 1.6f, 1.0f);
    }

    /** One out of sight falls silent: a wilted grey flower over its head, grey motes falling. Until woken. */
    public static void silenced(ServerLevel sl, LivingEntity victim, int ticks, String tag) {
        ModelFx wilted = FLOWER.color(0xC08A8A96).scale(0.7f).lifetime(ticks).fade(4, 10).tag(tag)
                .key(0, pose().frame(2f)).key(10, pose().frame(0f).roll(40f), Ease.IN);
        ParticleShapes.modelOn(sl, wilted, victim, new Vec3(0, victim.getBbHeight() + 0.3, 0), randomYaw(sl), 0f, 0f);
        ParticleShapes.burst(sl, GREY_MOTE, chestOf(victim), 6, 0.01, 0.03);
        sound(sl, chestOf(victim), "silenced", 0.7f, wobble(1.0f));
    }

    /** It comes into sight and wakes: its flower blooms gold. */
    public static void woken(ServerLevel sl, LivingEntity victim, String tag) {
        ParticleShapes.clearModels(sl, victim, tag);
        ModelFx flower = FLOWER.scale(0.7f).lifetime(20).fade(0, 10).key(0, pose().frame(0f)).key(8, pose().frame(2f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, flower, victim, new Vec3(0, victim.getBbHeight() + 0.3, 0), randomYaw(sl), 0f, 0f);
        ParticleShapes.burst(sl, PETAL, headOf(victim), 4, 0.02, 0.05);
        sound(sl, chestOf(victim), "wake", 0.7f, wobble(1.0f));
    }

    public static void fallSilentEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
    }

    /** The tick the enemy is gone after erase(...). */
    public static final int ERASE_GONE = 8;

    /** Erase: a line is rubbed out of the painting. A smear wipes across the enemy and it crumbles into crumbs. */
    public static void erase(ServerLevel sl, LivingEntity player, LivingEntity target) {
        Vec3 at = chestOf(target);
        float yaw = yawToward(at, player.position());
        ModelFx smear = ERASE_SMEAR.color(0xF0FFFFFF).scale((float) Math.max(1.6, target.getBbHeight() * 1.2)).lifetime(ERASE_GONE + 10)
                .fade(0, 8).key(0, pose().right(-0.6f)).key(ERASE_GONE, pose().right(0.6f), Ease.IN_OUT);
        ParticleShapes.model(sl, smear, at, yaw, 0f, 0f);
        Later.run(sl, ERASE_GONE, () -> {
            ParticleShapes.burst(sl, CRUMB, at, 20, 0.04, 0.12);
            ParticleShapes.burst(sl, PAGE, at, 4, 0.02, 0.06);
        });
        sound(sl, at, "erase", 1.2f, 1.0f);
    }

    /** The erased one returns where it stood: it is painted back in. */
    public static void erasedReturn(ServerLevel sl, Vec3 at) {
        ParticleShapes.spiral(sl, BRUSH, at.add(0, 0.2, 0), 3, 18, 0.2, 1.0, 1.0, -0.04);
        ParticleShapes.burst(sl, CRUMB, at.add(0, 1, 0), 10, 0.02, 0.05);
        sound(sl, at, "erase_return", 1.0f, 1.0f);
    }

    /** Erase aimed at nothing: a sweep of rubbing-out across a circle 'radius' wide. Call unmade for each thing undone. */
    public static void eraseArea(ServerLevel sl, Vec3 center, double radius) {
        for (int i = 0; i < 4; i++) {
            float yaw = i * 45f;
            ModelFx smear = ERASE_SMEAR.color(0xC0FFFFFF).scale((float) radius).delay(i * 2).lifetime(16).fade(0, 8)
                    .key(0, pose().pitch(90f).spin(90f));
            ParticleShapes.model(sl, smear, center.add(0, 0.3, 0), yaw, 0f, 0f);
        }
        ParticleShapes.ring(sl, CRUMB, center.add(0, 0.5, 0), 1.0, 30, radius / 16);
        sound(sl, center, "unmade", 1.3f, 1.0f);
    }

    /** One conjured thing unmade (a summon, a construct, a hung Painting, a Dome, a Zone). */
    public static void unmade(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, CRUMB, at, 12, 0.03, 0.1);
        ParticleShapes.burst(sl, PAGE, at, 3, 0.02, 0.05);
    }

    // =====================================================================================
    // Passives
    // =====================================================================================

    /** Spirit Sense: something starts hunting the Shaman from where they cannot see it. A chime from there, for them. */
    public static void spiritChime(ServerPlayer viewer, Vec3 from) {
        soundFor(viewer, from, "spirit_chime", 0.9f, 1.0f);
        Vec3 toward = from.subtract(viewer.getEyePosition()).normalize();
        ParticleShapes.spawnFor(viewer, EYE.color(0xFFB8E07A), viewer.getEyePosition().add(toward.scale(1.5)), Vec3.ZERO);
    }

    /** Featherfall: sneaking in the air slows the fall, a few soft feathers drift off them. */
    public static void featherfall(ServerLevel sl, LivingEntity player) {
        ParticleShapes.spawn(sl, GLINT, player.position().add(0, 0.2, 0), new Vec3(0, 0.02, 0));
        if (player.tickCount % 10 == 0) ParticleShapes.spawn(sl, FEATHER, chestOf(player), new Vec3(0, 0.02, 0));
    }

    /** Painter's Eye: an enemy's hung Painting, Trap or Zone shows who made it (a small eye there, for the Painter). */
    public static void painterEye(ServerPlayer viewer, Vec3 at) {
        ParticleShapes.spawnFor(viewer, EYE.color(0xFFECE2C8), at.add(0, 0.6, 0), Vec3.ZERO);
    }

    /** Prayers Heard: an ally in need prays. A chime for the String Player, and the ally glows gold for them. */
    public static void prayer(ServerPlayer viewer, LivingEntity ally) {
        SpellFx.markFor(viewer, ally, 0xFFFFD66E, 100);
        soundFor(viewer, chestOf(ally), "prayer", 0.9f, 1.0f);
    }

    /** Eyes Everywhere: everything living in the Territory, seen through walls (every 10 ticks). */
    public static void eyesEverywhere(ServerPlayer viewer, LivingEntity seen) {
        SpellFx.markFor(viewer, seen, 0xC0B8E07A, 12);
    }

    /** Sublunary Sight: everything living near, seen through walls (every 10 ticks). */
    public static void sublunarySight(ServerPlayer viewer, LivingEntity seen) {
        SpellFx.markFor(viewer, seen, 0xC0FFE9B0, 12);
    }

    /** An attack on the Sublunary Eye was foreseen and dulled: a gold eye blinks where it came from. */
    public static void foreseen(ServerLevel sl, LivingEntity player, Vec3 from) {
        Vec3 toward = from.subtract(chestOf(player)).normalize();
        ParticleShapes.spawn(sl, EYE.color(0xFFFFE9B0), chestOf(player).add(toward.scale(0.8)), Vec3.ZERO);
    }
}
