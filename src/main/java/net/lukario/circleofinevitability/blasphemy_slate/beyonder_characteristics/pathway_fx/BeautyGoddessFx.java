package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pathway_fx;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.lukario.circleofinevitability.combat.HudTimer;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Seeker;
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
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
 * How the Beauty Goddess (Moon) pathway looks and sounds: every key, both casts, and the four disasters of the Moon
 * wheel. Visual only: nothing here hurts, heals, roots or summons anyone. The ability code does that, at the ticks
 * named by the constants below (..._LANDS, ..._LOCKS, ..._GRIPS...), with Later.run. See docs/BEAUTY_GODDESS.md.
 *
 * ---------- See them in game ----------
 *   /beautyfx all              every display one after another (/beautyfx all 3 = three times over)
 *   /beautyfx moon             just that one (press TAB after /beautyfx for the names)
 * Displays that need a target use the nearest mob within 12 blocks, or you when there is none.
 *
 * ---------- Use them in an ability ----------
 *   Key 1  int ticks = throwFlask(sl, from, to, color); then flaskShatter(sl, to, color, radius)
 *          medicineCloud(sl, center, radius, ticks, tag), medicineCloudTick(sl, center, radius), medicineCloudEnd(sl, center, tag)
 *          medicineHeal(sl, ally), medicinePoison(sl, enemy)
 *          sneak: spiritVision(sl, player, radius), spiritVisionMark(sl, entity, ticks, healthPart), spiritVisionEnd(sl, entity)
 *   Key 2  tame(sl, player, beast, ticks) (the mark lands at TAME_LANDS), tameResisted(sl, target), tameEnd(sl, beast)
 *          sneak: animalSense(sl, player, radius), animalSenseCalled(sl, animal, ticks), animalSenseHunt(sl, target, ticks)
 *   Key 3  wings(sl, player, ticks, tag), wingsBurst(sl, player, tag) and BAT_SEEKER.onHit(...).launchAt(...)
 *          sneak: shackles(sl, target, ticks) (locked at SHACKLES_LOCK), shacklesDrain(sl, target, player), shacklesEnd(sl, target)
 *   Key 4  drink(sl, player, color) (drunk at DRINK_DONE), then one of:
 *          invisibility(sl, player) / invisibilityEnd(sl, player)
 *          fireBreath(sl, player) once and fireBreathTick(sl, player) every 2 ticks
 *          throwFlask(..., SUN) then sunWater(sl, at, radius), sunWaterBurn(sl, undead)
 *          shadowForm(sl, player, ticks, tag), shadowFormTick(sl, player), shadowFormEnd(sl, player, tag)
 *          the wheel's pictures: WHEEL_INVISIBILITY, WHEEL_FIRE_BREATHING, WHEEL_SUN_WATER, WHEEL_SHADOW
 *   Key 5  lunarBattlefield(sl, center, radius, ticks, tag), lunarBattlefieldPulse(sl, center, radius), lunarBattlefieldEnd(sl, center, tag)
 *          flashTeleport(sl, from, to); the passive's afterimage(sl, runner) every few ticks of a sprint
 *          sneak: moonlightForm(sl, player, ticks, tag), moonlightFormTick(sl, player), moonlightFormEnd(sl, player, tag)
 *                 moonlightShatter(sl, at), moonlightReform(sl, player) (whole again at MOONLIGHT_REFORMS)
 *   Key 6  gaze(sl, player, target, ticks, tag) (the darkness grips at GAZE_GRIPS), gazeTick(sl, target),
 *          gazeCrush(sl, player, target, tag) (hit at GAZE_CRUSH_HITS), gazeEnd(sl, player, target, tag)
 *          sneak: batSwarm(sl, player, ticks, tag), batSwarmTick(sl, player), batSwarmBatLost(sl, at), batSwarmEnd(sl, player, tag)
 *   Key 7  the wheel's pictures: WHEEL_DOOR, WHEEL_CREATION, WHEEL_BEAUTY, WHEEL_BLOOD_MOON
 *          summoningDoor(sl, ground, yaw, ticks, tag) (open at DOOR_OPEN), spiritCharge(sl, ground, yaw),
 *              summonArrives(sl, summoned), summonAnomaly(sl, ground, yaw)
 *          seedOfLife(sl, ground, radius, ticks, tag) (blooms at SEED_BLOOMS), seedOfLifePulse(sl, ground, radius),
 *              seedOfLifeHeal(sl, entity), seedOfLifeRevive(sl, at), seedOfLifeEnd(sl, ground, tag)
 *          beauty(sl, player, radius, ticks, tag), beautyEnthralled(sl, enemy, ticks), beautyPull(sl, enemy, player), beautyEnd(sl, player, tag)
 *          bloodMoon(sl, center, radius, ticks, tag), bloodMoonTick(sl, center, radius), bloodMoonBurn(sl, enemy),
 *              bloodMoonEmpower(sl, player), bloodMoonEnd(sl, center, tag)
 *   Every timed cast: status(player, Status.X, ticks) shows its picture and its clock; statusEnd(player, Status.X) takes them away.
 *
 * The models are in models/model_fx/beauty_goddess/ (made by tools/model_gen/beauty_goddess/models.py; each JSON's
 * "credit" gives its size and pivot), the pictures in textures/gui/beauty_goddess/ and textures/gui/ability_menus/
 * beauty_goddess/ (icons.py), the particles in textures/particle/fx/ (particles.py), the sounds in
 * sounds/beauty_goddess/ (sounds.py) and sounds/ModSounds.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class BeautyGoddessFx {

    private BeautyGoddessFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // Colors (ARGB)
    // =====================================================================================

    public static final int MOONLIGHT = 0xE0DCE8FF;
    public static final int SILVER = 0xFFC8D0E4;
    public static final int NIGHT = 0xE0140C24;
    public static final int VIOLET = 0xFF8A50E0;
    public static final int CRIMSON = 0xFF8A0F24;
    public static final int BLOOD = 0xFFB01020;
    public static final int ROSE = 0xFFF2A0C0;
    public static final int GOLD = 0xFFFFD86A;
    public static final int LIFE = 0xFF9AD84A;
    public static final int SPIRIT = 0xFF7A9AFF;

    // the potions' colors (the liquid in the flask, the splash, the drops)
    public static final int MEDICINE = 0xFF5AC870;
    public static final int POISON = 0xFF7A9A20;
    public static final int INVISIBILITY = 0xB0C8E8FF;
    public static final int FIRE = 0xFFFF8A2A;
    public static final int SUN = 0xFFFFD040;
    public static final int SHADOW = 0xFF3A2A5A;

    // =====================================================================================
    // The small particles everything shares
    // =====================================================================================

    private static final ParticleFx MOON_MOTE = ParticleFx.of("fx/glow")
            .color(0xF0E8F0FF).endColor(0x0090A8E0)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(24, 12).gravity(-0.02f).friction(0.92f).glow();
    private static final ParticleFx SPARKLE = ParticleFx.of("fx/sparkle")
            .color(0xFFFFFFFF).endColor(0x00B8C8FF)
            .size(0.1f).endSize(0.03f).sizeRandom(0.5f)
            .lifetime(16, 10).gravity(-0.005f).friction(0.9f).glow().randomRotation();
    private static final ParticleFx CRESCENT_MOTE = ParticleFx.of("fx/crescent")
            .color(0xF0E8F0FF).endColor(0x00A0B0E0)
            .size(0.09f).endSize(0.05f).sizeRandom(0.3f)
            .lifetime(30, 10).gravity(-0.015f).friction(0.94f).glow().spin(4f).randomRotation();
    private static final ParticleFx BUBBLE = ParticleFx.of("fx/bubble")
            .color(0xE0FFFFFF).endColor(0x60FFFFFF)
            .size(0.06f).endSize(0.09f).sizeRandom(0.5f)
            .lifetime(18, 12).gravity(-0.03f).friction(0.9f);
    private static final ParticleFx DROP = ParticleFx.of("fx/droplet")
            .color(0xFFFFFFFF).endColor(0xC0FFFFFF)
            .size(0.05f).endSize(0.04f).sizeRandom(0.4f)
            .lifetime(22, 8).gravity(0.07f).friction(0.98f).collide();
    private static final ParticleFx GLASS_SHARD = ParticleFx.of("fx/splinter")
            .color(0xE0E8F4FF).endColor(0x60C8DCF0)
            .size(0.06f).endSize(0.05f).sizeRandom(0.5f)
            .lifetime(22, 10).gravity(0.07f).friction(0.95f).collide().spin(20f).randomRotation();
    private static final ParticleFx MIST = ParticleFx.of("fx/smoke")
            .color(0x90FFFFFF).endColor(0x00FFFFFF)
            .size(0.35f).endSize(0.8f).sizeRandom(0.3f)
            .lifetime(30, 14).gravity(-0.01f).friction(0.9f).spin(2f).randomRotation();
    private static final ParticleFx DARK_WISP = ParticleFx.of("fx/smoke")
            .color(0xC0100818).endColor(0x00080410)
            .size(0.25f).endSize(0.6f).sizeRandom(0.3f)
            .lifetime(24, 12).gravity(-0.015f).friction(0.9f).spin(2f).randomRotation();
    private static final ParticleFx DARK_MOTE = ParticleFx.of("fx/glow")
            .color(0xE0A050FF).endColor(0x00300850)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(18, 10).gravity(-0.01f).friction(0.9f).glow();
    private static final ParticleFx BLOOD_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFE0283C).endColor(0x00700818)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(16, 8).gravity(-0.01f).friction(0.9f).glow();
    private static final ParticleFx BLOOD_DROP = DROP.color(0xFFB01020).endColor(0xC0600010);
    private static final ParticleFx BLOOD_EMBER = ParticleFx.of("fx/glow")
            .color(0xFFFF4A40).endColor(0x00600008)
            .size(0.1f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(14, 8).gravity(-0.04f).friction(0.9f).glow();
    private static final ParticleFx HEAL_MOTE = ParticleFx.of("fx/glow")
            .color(0xF0B0FF90).endColor(0x0040A030)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(-0.03f).friction(0.92f).glow();
    private static final ParticleFx GOLD_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFFF0A0).endColor(0x00FFB020)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(24, 10).gravity(-0.025f).friction(0.92f).glow();
    private static final ParticleFx SUN_SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFFF4C0).endColor(0x00FF9A20)
            .size(0.07f).endSize(0.02f)
            .lifetime(12, 6).gravity(0.02f).friction(0.9f).glow();
    private static final ParticleFx LEAF = ParticleFx.of("fx/leaf")
            .color(0xFF8AD04A).endColor(0xC04A8A2A)
            .size(0.07f).endSize(0.06f).sizeRandom(0.4f)
            .lifetime(36, 16).gravity(0.012f).friction(0.95f).spin(6f).randomRotation();
    private static final ParticleFx PETAL = ParticleFx.of("fx/petal")
            .color(0xFFF2A0C0).endColor(0x00F2A0C0)
            .size(0.07f).endSize(0.06f).sizeRandom(0.4f)
            .lifetime(40, 16).gravity(0.008f).friction(0.95f).spin(5f).randomRotation();
    private static final ParticleFx LIFE_PETAL = PETAL.color(0xFFFFF6D8).endColor(0x00FFD27A);
    private static final ParticleFx SPIRIT_MOTE = ParticleFx.of("fx/glow")
            .color(0xE0C8DCFF).endColor(0x003A50C0)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(26, 12).gravity(-0.02f).friction(0.92f).glow();
    /** A small black bat, flapping, that flies off: give it a speed when you spawn it. */
    private static final ParticleFx BAT_FLIGHT = ParticleFx.of("fx/bat_0", "fx/bat_1", "fx/bat_0", "fx/bat_1",
                    "fx/bat_0", "fx/bat_1", "fx/bat_0", "fx/bat_1")
            .color(0xFF201018).endColor(0x80201018)
            .size(0.14f).endSize(0.12f).sizeRandom(0.3f)
            .lifetime(22, 10).gravity(-0.01f).friction(0.97f);

    // =====================================================================================
    // Sounds (some vanilla constants are a SoundEvent, others a Holder of one: both overloads take either)
    // =====================================================================================

    private static void play(ServerLevel sl, Vec3 at, SoundEvent sound, float volume, float pitch) {
        Spells.sound(sl, at, sound, volume, pitch);
    }

    private static void play(ServerLevel sl, Vec3 at, Holder<SoundEvent> sound, float volume, float pitch) {
        Spells.sound(sl, at, sound.value(), volume, pitch);
    }

    /** One of the pathway's own sounds (sounds/ModSounds). */
    private static void sound(ServerLevel sl, Vec3 at, RegistryObject<SoundEvent> sound, float volume, float pitch) {
        Spells.sound(sl, at, sound.get(), volume, pitch);
    }

    private static float wobble(float pitch) {
        return pitch * (0.95f + ThreadLocalRandom.current().nextFloat() * 0.1f);
    }

    private static Vec3 chestOf(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.6, 0);
    }

    /** 'model' swinging between two poses, 'period' ticks each way, from tick 'from' to tick 'to'. */
    private static ModelFx swinging(ModelFx model, int from, int to, int period, ModelFx.Pose a, ModelFx.Pose b) {
        ModelFx out = model;
        boolean toA = true;
        for (int t = from; t < to; t += period) {
            out = out.during(t, Math.min(to, t + period), toA ? a : b, Ease.IN_OUT);
            toA = !toA;
        }
        return out;
    }

    /** A swing period that keeps a model under ModelFx's 64 keys however long 'ticks' is. */
    private static int periodFor(int ticks, int wanted, int keysLeft) {
        return Math.max(wanted, ticks / Math.max(1, keysLeft) + 1);
    }

    /** One block the way a yaw looks, level with the ground. */
    private static Vec3 ahead(float yaw) {
        double angle = Math.toRadians(yaw);
        return new Vec3(-Math.sin(angle), 0, Math.cos(angle));
    }

    // =====================================================================================
    // Statuses: a picture in the row above the hotbar stacks, and a clock beside the hotbar
    // =====================================================================================

    /** The picture for the Pathway's stacks, if it gets them (textures/gui/beauty_goddess/stack_moon.png). */
    public static final String STACK_IMAGE = "beauty_goddess/stack_moon";

    public enum Status {
        MEDICINE("medicine", "Medicine", 0x5AC870),
        SPIRIT_VISION("spirit_vision", "Spirit Vision", 0xC8B8FF),
        TAMED("tamed", "Blood Pact", 0xD62A3A),
        ANIMAL_SENSE("animal_sense", "Animal Sense", 0xE0B070),
        WINGS("wings", "Wings", 0xB04A5A),
        SHACKLES("shackles", "Shackles", 0x9A70E0),
        INVISIBILITY("invisibility", "Invisible", 0xC8E8FF),
        FIRE_BREATH("fire_breath", "Fire Breath", 0xFF8A2A),
        SHADOW("shadow", "Shadow", 0x8A70B0),
        LUNAR_BATTLEFIELD("lunar_battlefield", "Full Moon", 0xDCE8FF),
        MOONLIGHT_FORM("moonlight_form", "Moonlight", 0xB8CCFF),
        GAZE("gaze", "Gaze", 0xC01A2A),
        BAT_SWARM("bat_swarm", "Bat Swarm", 0x8A5A6A),
        SUMMONING("summoning", "Summoning", 0xB8A8FF),
        CREATION("creation", "Creation", 0xFFE08A),
        BEAUTY("beauty", "Beauty", 0xF2A0C0),
        BLOOD_MOON("blood_moon", "Blood Moon", 0xE0283C);

        final String image;
        final String label;
        final int rgb;

        Status(String image, String label, int rgb) {
            this.image = image;
            this.label = label;
            this.rgb = rgb;
        }

        String id() {
            return "bg_status_" + name().toLowerCase();
        }
    }

    /** Shows the status's picture (in the row above the hotbar stacks, two rows of nine) and a clock beside the hotbar. */
    public static void status(Player player, Status status, int ticks) {
        int slot = status.ordinal();
        ScreenImages.show(player, status.id(), "beauty_goddess/" + status.image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, ticks);
        HudTimer.show(player, status.id(), status.label, ticks, status.rgb);
    }

    /** It ended early. */
    public static void statusEnd(Player player, Status status) {
        ScreenImages.hide(player, status.id());
        HudTimer.hide(player, status.id());
    }

    // =====================================================================================
    // The models (models/model_fx/beauty_goddess/, see each JSON's "credit")
    // =====================================================================================

    /** The flask's glass and cork; FLASK_LIQUID is what is inside, tinted with .color(...). Both 0.75 blocks tall. */
    public static final ModelFx FLASK = ModelFx.of("beauty_goddess/flask").pivot(8, 6, 8).glow().seeThrough();
    public static final ModelFx FLASK_LIQUID = ModelFx.of("beauty_goddess/flask_liquid").pivot(8, 6, 8).glow().seeThrough();
    /** A bat, 1.2 blocks wingspan at scale 1, three steps of a wingbeat (0 = up, 2 = down). Face = front. */
    public static final ModelFx BAT = ModelFx.of("beauty_goddess/bat").frames(3).smooth().pivot(8, 4, 8.6f).glow();
    /** The right wing of darkness (.mirrored() last for the left), 2.1 blocks long, five steps from folded to spread. */
    public static final ModelFx BAT_WING = ModelFx.of("beauty_goddess/bat_wing").frames(5).smooth()
            .pivot(-10, 15, 8).glow().seeThrough();
    public static final ModelFx SHACKLE = ModelFx.of("beauty_goddess/abyss_shackle").pivot(8, 8, 8).glow();
    /** A chain 3 blocks long, its top pointing the way it is played: .scale(1, length / 3, 1). */
    public static final ModelFx CHAIN = ModelFx.of("beauty_goddess/abyss_chain").pivot(8, -16, 8).glow().seeThrough();
    public static final ModelFx ABYSS_POOL = ModelFx.of("beauty_goddess/abyss_pool").glow().unshaded().seeThrough();
    public static final ModelFx FULL_MOON = ModelFx.of("beauty_goddess/full_moon").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx BLOOD_MOON = ModelFx.of("beauty_goddess/blood_moon").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** A column of light 1 block wide and 3 tall, white: .scale(width, height / 3, width) and .color(...). */
    public static final ModelFx MOON_BEAM = ModelFx.of("beauty_goddess/moon_beam").pivot(8, -16, 8).glow().unshaded().seeThrough();
    /** The moon seal's two halves, 1 block wide, white: tint them and turn them against each other. */
    public static final ModelFx MOON_SEAL_RING = ModelFx.of("beauty_goddess/moon_sigil_ring").glow().unshaded().seeThrough();
    public static final ModelFx MOON_SEAL_CORE = ModelFx.of("beauty_goddess/moon_sigil_core").glow().unshaded().seeThrough();
    /** A standing ring of light, 1 block wide, white. */
    public static final ModelFx MOON_HALO = ModelFx.of("beauty_goddess/moon_halo").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** The caster as moonlight, 2 blocks tall: .scale(height / 2). Shown while the caster is hidden. */
    public static final ModelFx MOONLIGHT_FIGURE = ModelFx.of("beauty_goddess/moonlight_figure")
            .glow().unshaded().seeThrough().shownWhileHidden();
    /** The eye of Gaze of Darkness, 0.7 blocks, three steps (0 = shut, 2 = wide open). Iris = front. */
    public static final ModelFx GAZE_EYE = ModelFx.of("beauty_goddess/gaze_eye").frames(3).smooth().pivot(8, 8, 8).glow();
    /** A shell of darkness round a 1 x 2 block body, two steps (0 = flung apart, 1 = closed). */
    public static final ModelFx DARKNESS_PRISON = ModelFx.of("beauty_goddess/darkness_prison").frames(2).smooth()
            .glow().seeThrough();
    /** The Door of Summoning, 3 blocks wide and tall at scale 1: the frame, its two doors and the Spirit World inside. */
    public static final ModelFx SPIRIT_DOOR = ModelFx.of("beauty_goddess/spirit_door").pivot(8, -16, 8).glow();
    public static final ModelFx SPIRIT_DOOR_LEFT = ModelFx.of("beauty_goddess/spirit_door_left").pivot(-10, -16, 8).glow();
    public static final ModelFx SPIRIT_DOOR_RIGHT = ModelFx.of("beauty_goddess/spirit_door_right").pivot(26, -16, 8).glow();
    public static final ModelFx SPIRIT_DOOR_VOID = ModelFx.of("beauty_goddess/spirit_door_void").pivot(8, -16, 8)
            .glow().unshaded().seeThrough();
    /** A hound of the Spirit World, 2 blocks long, three steps of a gallop. Face = front. */
    public static final ModelFx SPIRIT_BEAST = ModelFx.of("beauty_goddess/spirit_beast").frames(3).smooth().glow().seeThrough();
    /** The Seed of Life, five steps from a closed seed (0) to a flower 1.6 blocks tall in full bloom (4). */
    public static final ModelFx SEED_OF_LIFE = ModelFx.of("beauty_goddess/seed_of_life").frames(5).smooth().glow();
    /** A silver circlet with a crescent and a moonstone, worn on the head (modelOnHead). */
    public static final ModelFx CRESCENT_CROWN = ModelFx.of("beauty_goddess/crescent_crown").glow();
    /** The goddess of the moon, a woman of light 3 blocks tall. Face = front. */
    public static final ModelFx GODDESS = ModelFx.of("beauty_goddess/goddess").pivot(8, -16, 8).glow().unshaded().seeThrough();
    /** The blood pact's mark, standing up, 1 block wide, white: tint it. */
    public static final ModelFx TAME_MARK = ModelFx.of("beauty_goddess/tame_mark").pivot(8, 8, 8).glow().unshaded().seeThrough();

    /** A seal on the ground: the two halves of the moon seal turning against each other, 'radius' wide, for 'ticks'. */
    public static void seal(ServerLevel sl, Vec3 ground, double radius, int color, int ticks, String tag) {
        float width = (float) (radius * 2.0);
        ModelFx ring = MOON_SEAL_RING.color(color).scale(width).lifetime(ticks).fade(0, 12).spin(0.6f)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(10, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ModelFx core = MOON_SEAL_CORE.color(color).scale(width * 0.62f).lifetime(ticks).fade(0, 12).spin(-1.1f)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(14, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        if (tag != null) {
            ring = ring.tag(tag);
            core = core.tag(tag);
        }
        float yaw = sl.getRandom().nextFloat() * 360f;
        ParticleShapes.model(sl, ring, ground.add(0, 0.05, 0), yaw, 0f, 0f);
        ParticleShapes.model(sl, core, ground.add(0, 0.07, 0), yaw, 0f, 0f);
    }

    // =====================================================================================
    // Key 1: Medicine Concoction (Apothecary)
    // =====================================================================================

    /**
     * A flask (glass, cork and the potion's color inside) thrown in an arc from 'from' to 'to', tumbling end over
     * end and dripping. Returns the ticks it takes to land: shatter it then with flaskShatter.
     */
    public static int throwFlask(ServerLevel sl, Vec3 from, Vec3 to, int color) {
        Vec3 way = to.subtract(from);
        double flat = Math.sqrt(way.x * way.x + way.z * way.z);
        int ticks = (int) Math.max(6, Math.min(30, Math.ceil(way.length() / 0.9)));
        float rise = (float) (0.6 + flat * 0.12);
        float yaw = flat < 0.05 ? 0f : Spells.yawOf(way);
        float drop = (float) way.y;
        int half = ticks / 2;
        float top = rise + Math.max(0f, drop) * 0.5f;                  // the height the first half climbs to
        ModelFx glass = FLASK.scale(0.8f);
        ModelFx liquid = FLASK_LIQUID.color(color | 0xFF000000).scale(0.8f);
        for (ModelFx part : List.of(glass, liquid)) {
            ModelFx flying = part.lifetime(ticks + 1).fade(0, 1)
                    .key(ticks, pose().forward((float) flat))
                    .during(0, half, pose().up(top), Ease.OUT)
                    .during(half, ticks, pose().up(drop), Ease.IN)
                    .during(0, ticks, pose().pitch(-720f));
            ParticleShapes.model(sl, flying, from, yaw, 0f, 0f);
        }
        ParticleFx drip = DROP.color(color | 0xFF000000);
        for (int t = 2; t < ticks; t += 2) {
            double p = (double) t / ticks;
            double up = p < 0.5
                    ? top * (1 - (1 - 2 * p) * (1 - 2 * p))               // eased out on the way up
                    : top + (drop - top) * (2 * p - 1) * (2 * p - 1);     // eased in on the way down
            Vec3 at = from.add(way.x * p, up, way.z * p);
            Later.run(sl, t, () -> ParticleShapes.spawn(sl, drip, at, Vec3.ZERO));
        }
        sound(sl, from, ModSounds.BEAUTY_FLASK_THROW, 0.9f, wobble(1.0f));
        return ticks;
    }

    /** The flask breaks: glass flies, the potion splashes out in a ring 'radius' wide, and it fizzes. */
    public static void flaskShatter(ServerLevel sl, Vec3 at, int color, double radius) {
        int c = color | 0xFF000000;
        ParticleShapes.burst(sl, GLASS_SHARD, at, 18, 0.08, 0.25);
        ParticleShapes.burst(sl, DROP.color(c), at, 24, 0.08, 0.3);
        ParticleShapes.burst(sl, BUBBLE.color((color & 0x00FFFFFF) | 0xE0000000), at, 12, 0.02, 0.08);
        SpellFx.shockRing(sl, at.add(0, -0.4, 0), radius, (color & 0x00FFFFFF) | 0xD0000000, 10, 0);
        SpellFx.shockRing(sl, at.add(0, -0.4, 0), radius * 0.6, 0xA0FFFFFF, 8, 2);
        sound(sl, at, ModSounds.BEAUTY_FLASK_SHATTER, 1.0f, wobble(1.0f));
        play(sl, at, SoundEvents.GLASS_BREAK, 0.6f, 1.3f);
    }

    /** The lingering cloud of medicine: a green seal under it and a mist over it for 'ticks'. Heal and poison in it with medicineCloudTick. */
    public static void medicineCloud(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        seal(sl, center, radius, 0xB05AC870, ticks, tag);
        ParticleShapes.ring(sl, MIST.color(0x905AC870).endColor(0x002A6A30), center.add(0, 0.4, 0), radius * 0.8, 14, 0.01);
        sound(sl, center, ModSounds.BEAUTY_POTION_BUBBLE, 0.8f, 1.0f);
    }

    /** Every half second while the cloud lasts: mist rolls inside it and bubbles rise. */
    public static void medicineCloudTick(ServerLevel sl, Vec3 center, double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ParticleFx mist = MIST.color(0x705AC870).endColor(0x002A6A30);
        for (int i = 0; i < 6; i++) {
            double angle = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            Vec3 at = center.add(Math.cos(angle) * far, 0.3 + random.nextDouble() * 0.8, Math.sin(angle) * far);
            ParticleShapes.spawn(sl, mist, at, new Vec3(0, 0.01, 0));
            if (i % 2 == 0) ParticleShapes.spawn(sl, BUBBLE.color(0xE0A0F0B0), at, new Vec3(0, 0.03, 0));
        }
        if (random.nextInt(3) == 0) sound(sl, center, ModSounds.BEAUTY_POTION_BUBBLE, 0.4f, wobble(1.1f));
    }

    /** Someone the medicine heals: green-gold motes rise round them. */
    public static void medicineHeal(ServerLevel sl, LivingEntity ally) {
        ParticleShapes.ring(sl, HEAL_MOTE, ally.position().add(0, 0.2, 0), Math.max(0.4, ally.getBbWidth() * 0.6), 10, 0);
        ParticleShapes.burst(sl, HEAL_MOTE, chestOf(ally), 4, 0.01, 0.03);
        sound(sl, ally.position(), ModSounds.BEAUTY_HEAL_CHIME, 0.35f, wobble(1.2f));
    }

    /** An enemy in the cloud: it is poisoned instead, drips and coughs bubbles. */
    public static void medicinePoison(ServerLevel sl, LivingEntity enemy) {
        ParticleShapes.burst(sl, DROP.color(0xFF7A9A20), chestOf(enemy), 6, 0.02, 0.06);
        ParticleShapes.burst(sl, BUBBLE.color(0xE0A8C040), enemy.position().add(0, enemy.getBbHeight() * 0.9, 0), 3, 0.01, 0.03);
    }

    /** The cloud is gone early. */
    public static void medicineCloudEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.burst(sl, MIST.color(0x705AC870), center.add(0, 0.5, 0), 10, 0.02, 0.06);
    }

    // ---------- sneak: Spirit Vision

    private static final String VISION_TAG = "bg_spirit_vision";

    /** The caster's eyes open to the spirit: a halo flares behind their head and a silver ring races out 'radius'. */
    public static void spiritVision(ServerLevel sl, Player caster, double radius) {
        ModelFx halo = MOON_HALO.color(0xC0DCE8FF).scale(0.9f).lifetime(30).fade(4, 12)
                .key(0, pose().forward(-0.32f).scale(0.3f).alpha(0f))
                .key(6, pose().scale(1.2f).alpha(1f), Ease.OUT_BACK)
                .key(30, pose().scale(1.6f).alpha(0f), Ease.IN);
        ParticleShapes.modelOnHead(sl, halo, caster, -0.25, 0f, 0f, 0f);
        SpellFx.steadyRing(sl, caster.position(), radius, 0xB0C8B8FF, 16, 0);
        SpellFx.steadyRing(sl, caster.position(), radius * 0.5, 0x80FFFFFF, 12, 3);
        ParticleShapes.burst(sl, SPARKLE, caster.getEyePosition(), 12, 0.03, 0.1);
        sound(sl, caster.position(), ModSounds.BEAUTY_SPIRIT_VISION, 1.0f, 1.0f);
    }

    /** One living thing seen with the spirit: a circle under it, green when healthy down to red when nearly dead. */
    public static void spiritVisionMark(ServerLevel sl, LivingEntity seen, int ticks, float healthPart) {
        float h = Math.max(0f, Math.min(1f, healthPart));
        int red = (int) (255 * Math.min(1f, 2f * (1f - h)));
        int green = (int) (255 * Math.min(1f, 2f * h));
        int color = 0xC0000000 | (red << 16) | (green << 8) | 0x40;
        SpellFx.circleUnder(sl, seen, Math.max(0.6, seen.getBbWidth() * 0.8), color, ticks, VISION_TAG);
        ParticleShapes.burst(sl, SPARKLE, chestOf(seen), 3, 0.01, 0.03);
    }

    /** The mark is taken off early. */
    public static void spiritVisionEnd(ServerLevel sl, LivingEntity seen) {
        ParticleShapes.clearModels(sl, seen, VISION_TAG);
    }

    // =====================================================================================
    // Key 2: Taming (Beast Tamer)
    // =====================================================================================

    /** The tick the blood mark lands on the beast: it is tamed then. */
    public static final int TAME_LANDS = 10;
    private static final String TAME_TAG = "bg_tame";

    private static final SlashFx BLOOD_LINE = SlashFx.line("slash/smooth")
            .color(0xC08A0F24).core(0xA0FF4A5A).width(0.07f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx ROSE_LINE = SlashFx.line("slash/smooth")
            .color(0x80F2A0C0).core(0x60FFFFFF).width(0.05f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx DARK_LINE = SlashFx.line("slash/smooth")
            .color(0xC0100818).core(0x80A050FF).width(0.09f).taper(SlashFx.Taper.UNIFORM).translucent();
    private static final SlashFx MOON_STREAK = SlashFx.line("slash/streaks")
            .color(0xC0DCE8FF).core(0xA0FFFFFF).width(0.3f).taper(SlashFx.Taper.COMET);

    /** A blood mark spinning over a head, tinted. */
    private static void markOverHead(ServerLevel sl, LivingEntity entity, int color, int ticks, String tag, float size) {
        ModelFx mark = TAME_MARK.color(color).scale(size).lifetime(ticks).fade(4, 10).spin(4f).tag(tag)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.clearModels(sl, entity, tag);
        ParticleShapes.modelOn(sl, mark, entity, new Vec3(0, entity.getBbHeight() + 0.55, 0), 0f, 0f, 0f);
    }

    /** A crimson thread runs from the caster's hand to the beast; at TAME_LANDS the blood mark settles over its head for 'ticks'. */
    public static void tame(ServerLevel sl, Player caster, LivingEntity beast, int ticks) {
        SpellFx.tether(sl, BLOOD_LINE, () -> SpellFx.handOf(caster, sl), () -> SpellFx.middleOf(beast, sl), TAME_LANDS + 4);
        ParticleShapes.line(sl, BLOOD_MOTE, SpellFx.handOf(caster, sl) == null ? caster.getEyePosition() : SpellFx.handOf(caster, sl),
                chestOf(beast), 2);
        sound(sl, caster.position(), ModSounds.BEAUTY_BLOOD_PACT, 0.9f, 1.0f);
        Later.run(sl, TAME_LANDS, () -> {
            if (!beast.isAlive()) return;
            markOverHead(sl, beast, 0xF0E0283C, ticks, TAME_TAG, 0.45f);
            SpellFx.circleUnder(sl, beast, Math.max(0.6, beast.getBbWidth() * 0.8), 0xA0B01020, 30, TAME_TAG + "_circle");
            ParticleShapes.burst(sl, BLOOD_DROP, chestOf(beast), 10, 0.03, 0.1);
            play(sl, beast.position(), SoundEvents.ZOMBIE_VILLAGER_CURE, 0.4f, 1.6f);
        });
    }

    /** It would not be tamed (a boss, a player): the mark lands and shatters at once. */
    public static void tameResisted(ServerLevel sl, LivingEntity target) {
        ModelFx mark = TAME_MARK.color(0xF0E0283C).scale(0.45f).lifetime(10).fade(0, 4)
                .key(0, pose().scale(0.2f))
                .key(4, pose().scale(1.2f), Ease.OUT)
                .key(10, pose().scale(1.8f).alpha(0f), Ease.OUT);
        ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.55, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, GLASS_SHARD.color(0xE0E0283C), target.position().add(0, target.getBbHeight() + 0.5, 0), 10, 0.05, 0.15);
        play(sl, target.position(), SoundEvents.GLASS_BREAK, 0.6f, 0.7f);
    }

    /** The pact is over (the beast is freed or dies): its mark is gone. */
    public static void tameEnd(ServerLevel sl, LivingEntity beast) {
        ParticleShapes.clearModels(sl, beast, TAME_TAG);
        ParticleShapes.burst(sl, BLOOD_MOTE, beast.position().add(0, beast.getBbHeight() + 0.5, 0), 8, 0.02, 0.06);
    }

    // ---------- sneak: Animal Sense

    private static final String SENSE_TAG = "bg_animal_sense";

    /** The call goes out: a golden ring races out 'radius' and the caster's eyes shine like a beast's. */
    public static void animalSense(ServerLevel sl, Player caster, double radius) {
        SpellFx.steadyRing(sl, caster.position(), radius, 0xC0E0B070, 18, 0);
        SpellFx.shockRing(sl, caster.position(), radius * 0.4, 0xA0FFF0C0, 10, 2);
        ParticleShapes.burst(sl, GOLD_MOTE, caster.getEyePosition(), 10, 0.02, 0.06);
        sound(sl, caster.position(), ModSounds.BEAUTY_BLOOD_PACT, 0.8f, 0.75f);
        play(sl, caster.position(), SoundEvents.BELL_RESONATE, 0.5f, 1.5f);
    }

    /** An animal (or a tamed beast) answers: a gold mark over it, and it is quickened for 'ticks'. */
    public static void animalSenseCalled(ServerLevel sl, LivingEntity animal, int ticks) {
        markOverHead(sl, animal, 0xE0FFD27A, ticks, SENSE_TAG, 0.35f);
        ParticleShapes.burst(sl, GOLD_MOTE, chestOf(animal), 6, 0.02, 0.06);
    }

    /** What the caster points them at: a crimson hunting circle under it. */
    public static void animalSenseHunt(ServerLevel sl, LivingEntity target, int ticks) {
        SpellFx.circleUnder(sl, target, Math.max(0.7, target.getBbWidth() * 0.9), 0xD0E0283C, ticks, SENSE_TAG + "_prey");
        SpellFx.closingRing(sl, target.position(), 2.5, 0xC0E0283C, 10, 0);
    }

    // =====================================================================================
    // Key 3: Wings of Darkness (Vampire)
    // =====================================================================================

    /** Unfolding takes this long, a beat this long, folding this long: wings(...) shows none for fewer than WINGS_SHORTEST. */
    private static final int WINGS_SPREAD = 14;
    private static final int WING_BEAT = 14;
    private static final int WINGS_FOLD = 16;
    public static final int WINGS_SHORTEST = WINGS_SPREAD + WING_BEAT + WINGS_FOLD;
    private static final float WING_SCALE = 1.3f;

    private static ModelFx wingFor(int side, int ticks) {
        int foldAt = ticks - WINGS_FOLD;
        int beat = Math.max(WING_BEAT, (foldAt - WINGS_SPREAD) / 28 + 1);       // at most 64 keys
        ModelFx wing = BAT_WING.scale(WING_SCALE).lifetime(ticks).fade(2, 6)
                .key(0, pose().right(0.14f * side).forward(-0.3f).scale(0.4f).frame(0))
                .during(0, 6, pose().scale(1f), Ease.OUT_BACK)
                .during(2, 12, pose().frame(4), Ease.OUT);
        for (int t = WINGS_SPREAD; t + beat <= foldAt; t += beat) {
            int down = t + Math.round(beat * 0.35f);
            int up = t + Math.round(beat * 0.9f);
            wing = wing.during(t, down, pose().roll(28f * side).frame(3), Ease.IN)
                    .during(down, up, pose().roll(-12f * side).frame(4), Ease.OUT);
        }
        wing = wing.during(foldAt, foldAt + 6, pose().roll(0), Ease.IN_OUT)
                .during(foldAt + 2, foldAt + 14, pose().frame(0).scale(0.5f), Ease.IN);
        return side > 0 ? wing : wing.mirrored();
    }

    /**
     * Bat wings of darkness on the caster's back for 'ticks': they snap open, keep beating (with a beat of sound
     * each time) and fold away in the last second. 'tag' names them: wingsBurst(...) with it ends them early.
     */
    public static void wings(ServerLevel sl, Player wearer, int ticks, String tag) {
        ParticleShapes.clearModels(sl, wearer, tag);
        if (ticks < WINGS_SHORTEST) return;
        Vec3 shoulders = new Vec3(0, wearer.getBbHeight() * 0.72, 0);
        ParticleShapes.modelOnTurning(sl, wingFor(1, ticks).tag(tag), wearer, shoulders, 0f, 0f, 0f);
        ParticleShapes.modelOnTurning(sl, wingFor(-1, ticks).tag(tag), wearer, shoulders, 0f, 0f, 0f);
        ParticleShapes.burst(sl, DARK_WISP, chestOf(wearer), 12, 0.03, 0.1);
        sound(sl, wearer.position(), ModSounds.BEAUTY_WINGS_UNFURL, 1.0f, 1.0f);
        int foldAt = ticks - WINGS_FOLD;
        int beat = Math.max(WING_BEAT, (foldAt - WINGS_SPREAD) / 28 + 1);
        for (int t = WINGS_SPREAD; t + beat <= foldAt; t += beat) {
            Later.run(sl, t + Math.round(beat * 0.3f), () -> {
                if (Spells.casterStillHere(wearer, sl)) sound(sl, wearer.position(), ModSounds.BEAUTY_WING_FLAP, 0.6f, wobble(1.0f));
            });
        }
    }

    /**
     * The flock that bursts out of the wings: give it onHit and launch it, e.g.
     *   BeautyGoddessFx.BAT_SEEKER.onHit((target, at) -> Spells.strike(player, target, 6f)).launchAt(player, sl, from, enemies, 8, 1);
     */
    public static final Seeker BAT_SEEKER = Seeker.of(BAT.scale(0.45f))
            .speed(0.75).turn(20).life(60).width(0.6).flap(4)
            .trail(DARK_MOTE, 1).burst(BAT_FLIGHT, 4);

    /** The wings burst into a swarm of bats (the end of the glide): they fold away at once and bats scatter. */
    public static void wingsBurst(ServerLevel sl, Player wearer, String tag) {
        ParticleShapes.clearModels(sl, wearer, tag);
        batsScatter(sl, chestOf(wearer), 10, 4.0);
        ParticleShapes.burst(sl, DARK_WISP, chestOf(wearer), 14, 0.04, 0.12);
        sound(sl, wearer.position(), ModSounds.BEAUTY_BAT_SWARM, 0.9f, 1.1f);
    }

    /** 'count' bats flying away from a point in every direction, 'far' blocks, flapping and fading. Visual only. */
    public static void batsScatter(ServerLevel sl, Vec3 from, int count, double far) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count + random.nextDouble() * 0.5;
            Vec3 dir = new Vec3(Math.cos(angle), 0.25 + random.nextDouble() * 0.5, Math.sin(angle)).normalize();
            int life = 16 + random.nextInt(8);
            ModelFx bat = swinging(BAT.scale(0.4f).lifetime(life).fade(0, 6)
                            .key(life, pose().forward((float) (far * (0.7 + random.nextDouble() * 0.6))), Ease.OUT),
                    0, life, 3, pose().frame(2), pose().frame(0));
            ParticleShapes.modelFacing(sl, bat.delay(random.nextInt(3)), from, dir);
        }
        ParticleShapes.burst(sl, BAT_FLIGHT, from, count, 0.15, 0.3);
    }

    // ---------- sneak: Abyss Shackles

    /** The tick the shackles lock round the bound one. */
    public static final int SHACKLES_LOCK = 13;
    private static final String SHACKLE_TAG = "bg_shackles";

    /**
     * A pool of darkness opens under the target, four chains rise from its edge and lock a cuff round its body at
     * SHACKLES_LOCK, and they hold until 'ticks'. Stuck to the target, so they go where it is pushed.
     */
    public static void shackles(ServerLevel sl, LivingEntity target, int ticks) {
        ParticleShapes.clearModels(sl, target, SHACKLE_TAG);
        double width = Math.max(0.6, target.getBbWidth());
        double height = target.getBbHeight();
        int life = Math.max(ticks, SHACKLES_LOCK + 10);
        ModelFx pool = ABYSS_POOL.scale((float) (width * 2.6)).lifetime(life).fade(0, 10).spin(0.5f).tag(SHACKLE_TAG)
                .key(0, pose().scale(0.2f))
                .key(8, pose().scale(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, pool, target, new Vec3(0, 0.05, 0), 0f, 0f, 0f);

        double cuffHeight = height * 0.55;
        float cuffScale = (float) (width * 1.05);
        ModelFx cuff = SHACKLE.scale(cuffScale).lifetime(life).fade(0, 8).tag(SHACKLE_TAG)
                .key(0, pose().scale(1.9f).alpha(0f))
                .key(SHACKLES_LOCK - 4, pose().alpha(1f))
                .key(SHACKLES_LOCK, pose().scale(1f), Ease.IN);
        ParticleShapes.modelOn(sl, cuff, target, new Vec3(0, cuffHeight, 0), 0f, 0f, 0f);
        ParticleShapes.modelOn(sl, cuff.scale(cuffScale * 0.85f).delay(2), target, new Vec3(0, Math.min(0.35, height * 0.2), 0), 22.5f, 0f, 0f);

        double out = width * 1.1;
        for (int i = 0; i < 4; i++) {
            float yawOut = 45f + i * 90f;
            Vec3 anchor = ahead(yawOut).scale(out);
            Vec3 toCuff = new Vec3(-anchor.x, cuffHeight, -anchor.z);
            float length = (float) toCuff.length();
            float yaw = Spells.yawOf(toCuff);
            float pitch = (float) Math.toDegrees(Math.atan2(Math.sqrt(toCuff.x * toCuff.x + toCuff.z * toCuff.z), toCuff.y));
            ModelFx chain = CHAIN.scale(1f, length / 3f, 1f).lifetime(life).fade(0, 8).tag(SHACKLE_TAG)
                    .key(0, pose().scale(0.05f))
                    .key(SHACKLES_LOCK - 2, pose().scale(1f), Ease.OUT);
            ParticleShapes.modelOn(sl, chain.delay(i), target, anchor.add(0, 0.05, 0), yaw, pitch, 0f);
        }
        ParticleShapes.ring(sl, DARK_WISP, target.position().add(0, 0.1, 0), width * 1.2, 10, -0.02);
        sound(sl, target.position(), ModSounds.BEAUTY_SHACKLES, 1.0f, wobble(1.0f));
        Later.run(sl, SHACKLES_LOCK, () -> {
            if (!target.isAlive()) return;
            ParticleShapes.burst(sl, DARK_MOTE, target.position().add(0, cuffHeight, 0), 12, 0.03, 0.08);
        });
    }

    /** Once a second while bound: blood is drawn out of it along a dark thread to the caster. */
    public static void shacklesDrain(ServerLevel sl, LivingEntity target, Player caster) {
        SpellFx.tether(sl, DARK_LINE, () -> SpellFx.middleOf(target, sl), () -> SpellFx.middleOf(caster, sl), 8);
        ParticleShapes.line(sl, BLOOD_MOTE, chestOf(target), chestOf(caster), 2);
        ParticleShapes.burst(sl, BLOOD_DROP, chestOf(target), 4, 0.02, 0.05);
        sound(sl, target.position(), ModSounds.BEAUTY_DRAIN, 0.6f, wobble(1.0f));
    }

    /** It is free (early or at the end). */
    public static void shacklesEnd(ServerLevel sl, LivingEntity target) {
        ParticleShapes.clearModels(sl, target, SHACKLE_TAG);
        ParticleShapes.burst(sl, DARK_WISP, chestOf(target), 12, 0.03, 0.1);
        play(sl, target.position(), SoundEvents.CHAIN_PLACE, 0.8f, 0.5f);
    }

    // =====================================================================================
    // Key 4: Potions (Potions Professor)
    // =====================================================================================

    /** The wheel's pictures (textures/gui/...). */
    public static final String WHEEL_INVISIBILITY = "ability_menus/beauty_goddess/invisibility_potion";
    public static final String WHEEL_FIRE_BREATHING = "ability_menus/beauty_goddess/fire_breathing_potion";
    public static final String WHEEL_SUN_WATER = "ability_menus/beauty_goddess/sun_water";
    public static final String WHEEL_SHADOW = "ability_menus/beauty_goddess/shadow_potion";

    /** The tick the potion is drunk: its effect starts then. */
    public static final int DRINK_DONE = 18;

    /** A flask of this color comes up to the caster's lips and is drunk in three gulps. */
    public static void drink(ServerLevel sl, Player drinker, int color) {
        float s = 0.55f;
        ModelFx.Pose low = pose().forward(0.42f).right(0.22f).up(0f).pitch(0f);
        for (ModelFx part : List.of(FLASK.scale(s), FLASK_LIQUID.color(color | 0xFF000000).scale(s))) {
            ModelFx held = part.lifetime(DRINK_DONE + 6).fade(3, 5)
                    .key(0, low.alpha(0f))
                    .key(4, pose().alpha(1f), Ease.OUT)
                    .key(8, pose().forward(0.3f).right(0.12f).up(0.18f).pitch(-70f), Ease.IN_OUT)
                    .during(DRINK_DONE, DRINK_DONE + 6, pose().forward(0.45f).right(0.3f).up(-0.2f).pitch(0f), Ease.IN);
            ParticleShapes.modelOnTurning(sl, held, drinker, new Vec3(0, drinker.getBbHeight() * 0.8, 0), 0f, 0f, 0f);
        }
        sound(sl, drinker.position(), ModSounds.BEAUTY_POTION_DRINK, 0.8f, wobble(1.0f));
        Later.run(sl, DRINK_DONE, () -> {
            if (!Spells.casterStillHere(drinker, sl)) return;
            ParticleShapes.burst(sl, BUBBLE.color((color & 0x00FFFFFF) | 0xE0000000), drinker.getEyePosition(), 8, 0.01, 0.04);
            SpellFx.risingRings(sl, drinker.position(), 0.9, 1.8, 3, (color & 0x00FFFFFF) | 0xB0000000);
        });
    }

    /** The Invisibility Potion takes hold: the drinker shimmers and fades. */
    public static void invisibility(ServerLevel sl, Player drinker) {
        ParticleShapes.sphere(sl, SPARKLE, chestOf(drinker), 0.8, 18);
        ParticleShapes.burst(sl, MIST.color(0x60C8E8FF), chestOf(drinker), 8, 0.02, 0.06);
        sound(sl, drinker.position(), ModSounds.BEAUTY_INVISIBILITY, 0.9f, 1.0f);
    }

    /** It wears off: a shimmer where they stand. */
    public static void invisibilityEnd(ServerLevel sl, Player drinker) {
        ParticleShapes.sphere(sl, SPARKLE, chestOf(drinker), 0.6, 12);
        sound(sl, drinker.position(), ModSounds.BEAUTY_MOON_BLINK, 0.5f, 1.5f);
    }

    /** The Fire Breathing Potion: the first breath. Then fireBreathTick every 2 ticks while it lasts. */
    public static void fireBreath(ServerLevel sl, Player breather) {
        sound(sl, breather.position(), ModSounds.BEAUTY_FIRE_BREATH, 1.0f, wobble(1.0f));
        play(sl, breather.position(), SoundEvents.FIRECHARGE_USE, 0.7f, 0.8f);
        fireBreathTick(sl, breather);
    }

    /** One puff of the breath: a cone of fire from the mouth, where they look. */
    public static void fireBreathTick(ServerLevel sl, Player breather) {
        Vec3 mouth = breather.getEyePosition().add(0, -0.15, 0).add(breather.getLookAngle().scale(0.4));
        ParticleShapes.cone(sl, SpellFx.FIRE_EMBER, mouth, breather.getLookAngle(), 26, 10, 0.35, 0.7);
        ParticleShapes.cone(sl, SpellFx.FIRE_SMOKE, mouth, breather.getLookAngle(), 20, 2, 0.15, 0.3);
    }

    /** Sun Water bursts where its flask broke: golden light, a pillar of sun and sparks, 'radius' wide. */
    public static void sunWater(ServerLevel sl, Vec3 at, double radius) {
        flaskShatter(sl, at, SUN, radius);
        SpellFx.blast(sl, at, radius * 0.7, 0xFFFFE070, SUN_SPARK);
        SpellFx.risingRings(sl, at.add(0, -0.4, 0), radius * 0.8, 2.5, 3, 0xC0FFE070);
        ModelFx pillar = MOON_BEAM.color(0xB0FFE070).scale((float) (radius * 0.9), 2f, (float) (radius * 0.9)).lifetime(24).fade(2, 12)
                .key(0, pose().scale(0.2f))
                .key(5, pose().scale(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, pillar, at.add(0, -0.5, 0), 0f, 0f, 0f);
        sound(sl, at, ModSounds.BEAUTY_SUN_WATER, 1.0f, 1.0f);
        play(sl, at, SoundEvents.BEACON_ACTIVATE, 0.6f, 1.6f);
    }

    /** An undead (or vampire) hit by Sun Water: it smokes and sizzles. */
    public static void sunWaterBurn(ServerLevel sl, LivingEntity undead) {
        ParticleShapes.burst(sl, SUN_SPARK, chestOf(undead), 10, 0.04, 0.12);
        ParticleShapes.burst(sl, SpellFx.FIRE_SMOKE, chestOf(undead), 4, 0.02, 0.05);
        play(sl, undead.position(), SoundEvents.FIRE_EXTINGUISH, 0.6f, 1.2f);
    }

    /** The Shadow Potion: the drinker melts into a shadow that slides along the ground for 'ticks'. */
    public static void shadowForm(ServerLevel sl, Player drinker, int ticks, String tag) {
        ParticleShapes.clearModels(sl, drinker, tag);
        ModelFx pool = ABYSS_POOL.color(0xF0A090C0).scale(1.6f).lifetime(ticks).fade(4, 8).spin(1f).tag(tag).shownWhileHidden()
                .key(0, pose().scale(0.3f))
                .key(6, pose().scale(1f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, pool, drinker, new Vec3(0, 0.05, 0), 0f, 0f, 0f);
        SpellFx.closingRing(sl, drinker.position(), 1.6, NIGHT, 8, 0);
        ParticleShapes.burst(sl, DARK_WISP, chestOf(drinker), 16, 0.03, 0.1);
        sound(sl, drinker.position(), ModSounds.BEAUTY_SHADOW_MELT, 0.9f, 1.0f);
    }

    /** Every few ticks as a shadow: it smokes a little. */
    public static void shadowFormTick(ServerLevel sl, Player drinker) {
        ParticleShapes.burst(sl, DARK_WISP, drinker.position().add(0, 0.1, 0), 2, 0.005, 0.02);
    }

    /** They rise out of the shadow again. */
    public static void shadowFormEnd(ServerLevel sl, Player drinker, String tag) {
        ParticleShapes.clearModels(sl, drinker, tag);
        SpellFx.risingRings(sl, drinker.position(), 1.0, 2.0, 3, NIGHT);
        ParticleShapes.burst(sl, DARK_WISP, chestOf(drinker), 14, 0.04, 0.1);
        sound(sl, drinker.position(), ModSounds.BEAUTY_SHADOW_MELT, 0.6f, 1.4f);
    }

    // =====================================================================================
    // Key 5: Lunar Battlefield (Scarlet Scholar)
    // =====================================================================================

    /** The tick the moon has risen and its light reaches the ground. */
    public static final int MOON_RISEN = 30;

    /**
     * A full moon rises over 'center' and shines down on a round battlefield 'radius' wide: a moon seal on the
     * ground, a column of pale light, mist at its edge, for 'ticks'. 'tag' names it: lunarBattlefieldEnd ends it.
     */
    public static void lunarBattlefield(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        double high = 12 + radius * 0.6;
        float size = (float) Math.max(4.0, radius * 0.7);
        ModelFx moon = FULL_MOON.scale(size).lifetime(ticks).fade(0, 20).spin(0.25f).tag(tag)
                .aura(0x30DCE8FF, 0.4f, 4)
                .key(0, pose().up(-8f).alpha(0f))
                .key(MOON_RISEN, pose().up(0f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, moon, center.add(0, high, 0), 0f, 0f, 0f);
        float width = (float) (radius * 2.0);
        ModelFx light = MOON_BEAM.color(0x40DCE8FF).scale(width, (float) (high / 3.0), width).lifetime(ticks - MOON_RISEN + 10)
                .fade(10, 20).tag(tag).delay(MOON_RISEN - 10);
        ParticleShapes.model(sl, light, center.add(0, 0.05, 0), 0f, 0f, 0f);
        seal(sl, center, radius, 0xB0DCE8FF, ticks, tag);
        ParticleShapes.ring(sl, MIST.color(0x80DCE8FF), center.add(0, 0.3, 0), radius, 24, 0.0);
        ParticleShapes.ring(sl, MOON_MOTE, center.add(0, 0.4, 0), radius * 0.95, 24, 0.0);
        sound(sl, center, ModSounds.BEAUTY_MOONRISE, 1.2f, 1.0f);
    }

    /** Every 2 seconds under the moon: a silver ring and motes rising toward it. */
    public static void lunarBattlefieldPulse(ServerLevel sl, Vec3 center, double radius) {
        SpellFx.steadyRing(sl, center, radius, 0x80DCE8FF, 20, 0);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 12; i++) {
            double angle = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            ParticleShapes.spawn(sl, MOON_MOTE, center.add(Math.cos(angle) * far, 0.2, Math.sin(angle) * far), new Vec3(0, 0.05, 0));
        }
        ParticleShapes.ring(sl, MIST.color(0x60DCE8FF), center.add(0, 0.3, 0), radius, 12, 0.0);
    }

    /** The moon sets early. */
    public static void lunarBattlefieldEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.burst(sl, MOON_MOTE, center.add(0, 1, 0), 20, 0.03, 0.1);
    }

    /** Flash Teleportation: moonlight flares where the caster was and where they now are, a streak between. */
    public static void flashTeleport(ServerLevel sl, Vec3 from, Vec3 to) {
        ModelFx flare = MOON_BEAM.color(0xC0DCE8FF).scale(0.9f, 1f, 0.9f).lifetime(14).fade(0, 10)
                .key(0, pose().scale(0.3f))
                .key(3, pose().scale(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, flare, from, 0f, 0f, 0f);
        ParticleShapes.model(sl, flare, to, 0f, 0f, 0f);
        ParticleShapes.slashBetween(sl, MOON_STREAK.lifetime(8), from.add(0, 1, 0), to.add(0, 1, 0));
        ParticleShapes.burst(sl, SPARKLE, from.add(0, 1, 0), 12, 0.03, 0.12);
        ParticleShapes.burst(sl, SPARKLE, to.add(0, 1, 0), 12, 0.03, 0.12);
        SpellFx.shockRing(sl, to, 1.8, 0xC0DCE8FF, 8, 0);
        sound(sl, from, ModSounds.BEAUTY_MOON_BLINK, 0.8f, wobble(1.0f));
        sound(sl, to, ModSounds.BEAUTY_MOON_BLINK, 1.0f, wobble(1.2f));
    }

    /** The Scarlet Scholar's passive: a pale copy of moonlight left where the runner was, fading. Call it every few ticks of a sprint. */
    public static void afterimage(ServerLevel sl, Player runner) {
        ModelFx copy = MOONLIGHT_FIGURE.scale(runner.getBbHeight() / 2f).lifetime(12).fade(0, 10)
                .key(0, pose().alpha(0.4f))
                .key(12, pose().alpha(0f), Ease.OUT);
        ParticleShapes.model(sl, copy, runner.position(), runner.getYRot(), 0f, 0f);
        ParticleShapes.burst(sl, MOON_MOTE, chestOf(runner), 1, 0.005, 0.01);
    }

    // ---------- sneak: Spectral Moonlight Form

    /** The tick a scattered body of moonlight is whole again (moonlightReform). */
    public static final int MOONLIGHT_REFORMS = 19;

    /** The caster turns to moonlight for 'ticks': a figure of moonlight in their shape, flickering. */
    public static void moonlightForm(ServerLevel sl, Player caster, int ticks, String tag) {
        ParticleShapes.clearModels(sl, caster, tag);
        float scale = caster.getBbHeight() / 2f;
        int period = periodFor(ticks, 6, 50);
        ModelFx figure = swinging(MOONLIGHT_FIGURE.scale(scale).lifetime(ticks).fade(0, 8).tag(tag)
                        .key(0, pose().scale(1.25f).alpha(0f))
                        .key(6, pose().scale(1f).alpha(1f), Ease.OUT),
                6, ticks, period, pose().alpha(0.7f), pose().alpha(1f));
        ParticleShapes.modelOnTurning(sl, figure, caster, Vec3.ZERO, 0f, 0f, 0f);
        SpellFx.risingRings(sl, caster.position(), 0.9, 2.0, 3, 0xC0DCE8FF);
        ParticleShapes.burst(sl, SPARKLE, chestOf(caster), 16, 0.03, 0.1);
        sound(sl, caster.position(), ModSounds.BEAUTY_MOONLIGHT_FORM, 1.0f, 1.0f);
    }

    /** Every few ticks as moonlight: motes drift off the body. */
    public static void moonlightFormTick(ServerLevel sl, Player caster) {
        ParticleShapes.burst(sl, MOON_MOTE, chestOf(caster), 2, 0.005, 0.02);
    }

    /** They are flesh again. */
    public static void moonlightFormEnd(ServerLevel sl, Player caster, String tag) {
        ParticleShapes.clearModels(sl, caster, tag);
        ParticleShapes.burst(sl, SPARKLE, chestOf(caster), 12, 0.03, 0.08);
        sound(sl, caster.position(), ModSounds.BEAUTY_MOON_BLINK, 0.5f, 0.8f);
    }

    /** The moonlight body is struck and scatters into shards of light. */
    public static void moonlightShatter(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, GLASS_SHARD.color(0xF0DCE8FF), at, 24, 0.08, 0.25);
        ParticleShapes.burst(sl, MOON_MOTE, at, 16, 0.05, 0.15);
        SpellFx.shockRing(sl, at.add(0, -0.8, 0), 2.5, 0xC0DCE8FF, 10, 0);
        play(sl, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 1.2f);
    }

    /** The moonlight gathers into the caster again: motes are drawn in and it is whole at MOONLIGHT_REFORMS. */
    public static void moonlightReform(ServerLevel sl, Player caster) {
        ParticleShapes.ring(sl, MOON_MOTE, chestOf(caster), 3.0, 28, -0.15);
        SpellFx.closingRing(sl, caster.position(), 3.0, 0xC0DCE8FF, MOONLIGHT_REFORMS, 0);
        sound(sl, caster.position(), ModSounds.BEAUTY_MOONLIGHT_REFORM, 1.0f, 1.0f);
        Later.run(sl, MOONLIGHT_REFORMS, () -> {
            if (!Spells.casterStillHere(caster, sl)) return;
            SpellFx.shockRing(sl, caster.position(), 3.0, 0xE0FFFFFF, 8, 0);
            ParticleShapes.burst(sl, SPARKLE, chestOf(caster), 20, 0.05, 0.15);
        });
    }

    // =====================================================================================
    // Key 6: Gaze of Darkness (Shaman King)
    // =====================================================================================

    /** The tick the darkness has closed round the target: hold it from then. */
    public static final int GAZE_GRIPS = 12;
    /** Ticks after gazeCrush that the crushing lands: hurt the target then. */
    public static final int GAZE_CRUSH_HITS = 3;

    private static Vec3 eyeOf(Player caster) {
        return caster.getEyePosition().add(Spells.flatLook(caster).scale(0.9)).add(Spells.rightOf(caster).scale(0.35)).add(0, -0.2, 0);
    }

    /**
     * The target's reflection is caught in an eye that opens in front of the caster; a thread of darkness runs from
     * it to the target, and solid darkness closes round the target at GAZE_GRIPS and holds until 'ticks'.
     * 'tag' names this gaze (the eye on the caster, the darkness on the target).
     */
    public static void gaze(ServerLevel sl, Player caster, LivingEntity target, int ticks, String tag) {
        ParticleShapes.clearModels(sl, caster, tag);
        ParticleShapes.clearModels(sl, target, tag);
        int period = periodFor(ticks, 10, 40);
        ModelFx eye = swinging(GAZE_EYE.scale(0.7f).lifetime(ticks).fade(0, 6).tag(tag)
                        .key(0, pose().forward(0.9f).right(0.35f).up(-0.2f).scale(0.3f).alpha(0f).frame(0))
                        .key(5, pose().scale(1f).alpha(1f), Ease.OUT_BACK)
                        .during(4, 10, pose().frame(2), Ease.OUT),
                12, ticks, period, pose().scale(1.08f), pose().scale(1f));
        ParticleShapes.modelOnTurning(sl, eye, caster, new Vec3(0, caster.getEyeHeight(), 0), 0f, 0f, 0f);
        SpellFx.tether(sl, DARK_LINE, () -> SpellFx.middleOf(caster, sl) == null ? null : eyeOf(caster),
                () -> SpellFx.middleOf(target, sl), ticks);

        double width = Math.max(0.7, target.getBbWidth() * 1.25);
        double height = target.getBbHeight() * 1.05;
        ModelFx prison = DARKNESS_PRISON.scale((float) width, (float) (height / 2.0), (float) width)
                .lifetime(Math.max(ticks, GAZE_GRIPS + 10)).fade(2, 8).tag(tag)
                .key(0, pose().frame(0).alpha(0f))
                .key(4, pose().alpha(1f))
                .during(4, GAZE_GRIPS, pose().frame(1), Ease.IN);
        ParticleShapes.modelOn(sl, prison, target, Vec3.ZERO, sl.getRandom().nextFloat() * 90f, 0f, 0f);
        sound(sl, caster.position(), ModSounds.BEAUTY_GAZE, 1.0f, 1.0f);
        Later.run(sl, GAZE_GRIPS - 7, () -> {
            if (target.isAlive()) sound(sl, target.position(), ModSounds.BEAUTY_DARKNESS_CLOSE, 1.0f, wobble(1.0f));
        });
        Later.run(sl, GAZE_GRIPS, () -> {
            if (target.isAlive()) ParticleShapes.burst(sl, DARK_MOTE, chestOf(target), 14, 0.03, 0.08);
        });
    }

    /** Every half second while held: violet light seeps from the darkness and a little blood drips. */
    public static void gazeTick(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, DARK_MOTE, chestOf(target), 3, 0.01, 0.04);
        ParticleShapes.burst(sl, BLOOD_DROP, chestOf(target), 1, 0.01, 0.02);
    }

    /** The caster crushes the eye: it bursts, the darkness shatters round the target (the hit lands at GAZE_CRUSH_HITS). */
    public static void gazeCrush(ServerLevel sl, Player caster, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, caster, tag);
        ModelFx squeezed = GAZE_EYE.scale(0.7f).lifetime(GAZE_CRUSH_HITS + 2).fade(0, 2)
                .key(0, pose().forward(0.9f).right(0.35f).up(-0.2f).frame(2))
                .key(GAZE_CRUSH_HITS, pose().scale(0.25f).frame(0), Ease.IN);
        ParticleShapes.modelOnTurning(sl, squeezed, caster, new Vec3(0, caster.getEyeHeight(), 0), 0f, 0f, 0f);
        sound(sl, caster.position(), ModSounds.BEAUTY_EYE_CRUSH, 1.0f, wobble(1.0f));
        Later.run(sl, GAZE_CRUSH_HITS, () -> {
            ParticleShapes.burst(sl, BLOOD_DROP, eyeOf(caster), 12, 0.04, 0.12);
            if (!target.isAlive()) return;
            ParticleShapes.clearModels(sl, target, tag);
            double width = Math.max(0.7, target.getBbWidth() * 1.25);
            ModelFx breaking = DARKNESS_PRISON.scale((float) width, (float) (target.getBbHeight() * 1.05 / 2.0), (float) width)
                    .lifetime(10).fade(0, 8)
                    .key(0, pose().frame(1))
                    .key(8, pose().frame(0).scale(1.3f), Ease.OUT);
            ParticleShapes.modelOn(sl, breaking, target, Vec3.ZERO, 0f, 0f, 0f);
            ParticleShapes.burst(sl, GLASS_SHARD.color(0xF0200830), chestOf(target), 20, 0.08, 0.22);
            ParticleShapes.burst(sl, BLOOD_DROP, chestOf(target), 16, 0.05, 0.15);
            play(sl, target.position(), SoundEvents.GLASS_BREAK, 1.0f, 0.5f);
        });
    }

    /** The gaze is let go without crushing: the eye closes and fades, the darkness crumbles away. */
    public static void gazeEnd(ServerLevel sl, Player caster, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, caster, tag);
        ModelFx closing = GAZE_EYE.scale(0.7f).lifetime(10).fade(0, 6)
                .key(0, pose().forward(0.9f).right(0.35f).up(-0.2f).frame(2))
                .key(6, pose().frame(0), Ease.IN);
        ParticleShapes.modelOnTurning(sl, closing, caster, new Vec3(0, caster.getEyeHeight(), 0), 0f, 0f, 0f);
        if (target.isAlive()) {
            ParticleShapes.clearModels(sl, target, tag);
            ParticleShapes.burst(sl, DARK_WISP, chestOf(target), 14, 0.03, 0.08);
        }
    }

    // ---------- sneak: Bat Swarm Transformation

    private static final int SWARM_BATS = 12;

    /**
     * The caster splits into a swarm of bats for 'ticks': bats circle where they are, in rings at different heights
     * and speeds, flapping, while they are hidden (the bats are shown anyway). 'tag' names the swarm.
     */
    public static void batSwarm(ServerLevel sl, Player caster, int ticks, String tag) {
        ParticleShapes.clearModels(sl, caster, tag);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float scale = 0.42f;
        for (int i = 0; i < SWARM_BATS; i++) {
            double radius = 0.5 + random.nextDouble() * 0.9;
            float offset = (float) (radius * 16.0 / scale);                    // the pivot sits 'radius' blocks to its side
            float height = (float) (0.3 + random.nextDouble() * 1.5);
            float speed = 9f + random.nextFloat() * 8f;
            int period = periodFor(ticks, 3, 54);
            ModelFx bat = swinging(BAT.pivot(8 + offset, 4, 8.6f).scale(scale).lifetime(ticks).fade(4, 6).spin(speed).tag(tag)
                            .shownWhileHidden()
                            .key(0, pose().scale(0.3f))
                            .key(6, pose().scale(1f), Ease.OUT_BACK),
                    random.nextInt(3), ticks, period, pose().frame(2), pose().frame(0));
            ParticleShapes.modelOn(sl, bat, caster, new Vec3(0, height, 0), random.nextFloat() * 360f, 0f, 0f);
        }
        ParticleShapes.burst(sl, DARK_WISP, chestOf(caster), 16, 0.03, 0.1);
        ParticleShapes.burst(sl, BAT_FLIGHT, chestOf(caster), 8, 0.1, 0.25);
        for (int t = 0; t < ticks - 20; t += 60) {
            Later.run(sl, t, () -> {
                if (Spells.casterStillHere(caster, sl)) sound(sl, caster.position(), ModSounds.BEAUTY_BAT_SWARM, 0.8f, wobble(1.0f));
            });
        }
    }

    /** Every few ticks as a swarm: dark motes and the odd bat flying off. */
    public static void batSwarmTick(ServerLevel sl, Player caster) {
        ParticleShapes.burst(sl, DARK_MOTE, chestOf(caster), 2, 0.01, 0.04);
        if (ThreadLocalRandom.current().nextInt(4) == 0) ParticleShapes.burst(sl, BAT_FLIGHT, chestOf(caster), 1, 0.1, 0.2);
    }

    /** One of the bats is killed: it bursts into blood and smoke. */
    public static void batSwarmBatLost(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, BLOOD_DROP, at, 6, 0.03, 0.08);
        ParticleShapes.burst(sl, DARK_WISP, at, 4, 0.02, 0.05);
        sound(sl, at, ModSounds.BEAUTY_WING_FLAP, 0.6f, 1.6f);
    }

    /** The swarm draws together into the caster again. */
    public static void batSwarmEnd(ServerLevel sl, Player caster, String tag) {
        ParticleShapes.clearModels(sl, caster, tag);
        Vec3 middle = chestOf(caster);
        for (int i = 0; i < 8; i++) {
            float yaw = i * 45f;
            Vec3 from = middle.add(ahead(yaw).scale(2.5)).add(0, (i % 3 - 1) * 0.5, 0);
            ModelFx bat = swinging(BAT.scale(0.42f).lifetime(10).fade(0, 4)
                            .key(8, pose().forward(2.4f), Ease.IN),
                    0, 10, 3, pose().frame(2), pose().frame(0));
            ParticleShapes.modelFacing(sl, bat, from, middle.subtract(from));
        }
        Later.run(sl, 8, () -> {
            if (!Spells.casterStillHere(caster, sl)) return;
            ParticleShapes.burst(sl, DARK_WISP, chestOf(caster), 14, 0.03, 0.1);
            sound(sl, caster.position(), ModSounds.BEAUTY_WINGS_UNFURL, 0.8f, 1.2f);
        });
    }

    // =====================================================================================
    // Key 7: the Moon wheel (High Summoner, Life-Giver, Beauty Goddess, Moon)
    // =====================================================================================

    /** The wheel's pictures (textures/gui/...). */
    public static final String WHEEL_DOOR = "ability_menus/beauty_goddess/door_of_summoning";
    public static final String WHEEL_CREATION = "ability_menus/beauty_goddess/creation";
    public static final String WHEEL_BEAUTY = "ability_menus/beauty_goddess/beauty";
    public static final String WHEEL_BLOOD_MOON = "ability_menus/beauty_goddess/blood_moon";

    // ---------- Door of Summoning (High Summoner, Sequence 3)

    /** The tick the doors stand open: the summoned may step out then. */
    public static final int DOOR_OPEN = 32;
    private static final float DOOR_SCALE = 1.2f;
    /** Fewer ticks than this are too few for the door to open and close again. */
    public static final int DOOR_SHORTEST = DOOR_OPEN + 30;

    /**
     * The Door of Summoning: an illusory door rises at 'ground', its front looking along 'yaw', its doors swing open
     * at DOOR_OPEN onto the Spirit World, and at the end they shut and it fades. 'tag' names it (clear it early with
     * ParticleShapes.clearModels(sl, ground, tag)).
     */
    public static void summoningDoor(ServerLevel sl, Vec3 ground, float yaw, int ticks, String tag) {
        int life = Math.max(ticks, DOOR_SHORTEST);
        int shut = life - 22;
        float s = DOOR_SCALE;
        ModelFx frame = SPIRIT_DOOR.scale(s).lifetime(life).fade(0, 12).tag(tag)
                .key(0, pose().scale(0.6f).alpha(0f).up(-1f))
                .key(12, pose().scale(1f).alpha(1f).up(0f), Ease.OUT_BACK);
        ParticleShapes.model(sl, frame, ground, yaw, 0f, 0f);
        for (int side : new int[]{-1, 1}) {
            ModelFx base = side < 0 ? SPIRIT_DOOR_LEFT : SPIRIT_DOOR_RIGHT;
            ModelFx door = base.scale(s).lifetime(life).fade(0, 12).tag(tag)
                    .key(0, pose().right(1.125f * s * side).scale(0.6f).alpha(0f).up(-1f))
                    .key(12, pose().scale(1f).alpha(1f).up(0f), Ease.OUT_BACK)
                    .during(16, DOOR_OPEN, pose().yaw(100f * side), Ease.IN_OUT)
                    .during(shut, shut + 10, pose().yaw(0f), Ease.IN);
            ParticleShapes.model(sl, door, ground, yaw, 0f, 0f);
        }
        ModelFx inside = SPIRIT_DOOR_VOID.scale(s).lifetime(life).fade(0, 12).tag(tag)
                .key(0, pose().scale(0f))
                .key(14, pose().scale(0f))
                .key(DOOR_OPEN - 4, pose().scale(1f), Ease.OUT)
                .during(shut + 4, shut + 12, pose().alpha(0f), Ease.IN);
        ParticleShapes.model(sl, inside, ground, yaw, 0f, 0f);
        seal(sl, ground, 2.6, 0xB0B8A8FF, life, tag);
        sound(sl, ground, ModSounds.BEAUTY_DOOR_OPEN, 1.3f, 1.0f);
        Vec3 doorway = ground.add(0, 1.4, 0);
        for (int t = DOOR_OPEN; t < shut; t += 6) {
            Later.run(sl, t, () -> {
                ParticleShapes.cone(sl, SPIRIT_MOTE, doorway, ahead(yaw), 50, 4, 0.05, 0.15);
                ParticleShapes.cone(sl, MIST.color(0x907A9AFF).endColor(0x003A50C0), doorway.add(0, -1.0, 0), ahead(yaw), 60, 2, 0.03, 0.08);
            });
        }
        Later.run(sl, shut, () -> play(sl, ground, SoundEvents.CHAIN_PLACE, 1.0f, 0.4f));
    }

    /** A hound of the Spirit World runs out of the door along 'yaw' and fades: what comes through, or what it attacks with. */
    public static void spiritCharge(ServerLevel sl, Vec3 ground, float yaw) {
        int life = 26;
        ModelFx beast = swinging(SPIRIT_BEAST.scale(1.1f).lifetime(life).fade(4, 8)
                        .key(life, pose().forward(9f)),
                0, life, 3, pose().frame(2), pose().frame(0));
        ParticleShapes.model(sl, beast, ground, yaw, 0f, 0f);
        for (int t = 0; t < life; t += 3) {
            int at = t;
            Later.run(sl, t, () -> ParticleShapes.burst(sl, SPIRIT_MOTE, ground.add(ahead(yaw).scale(9.0 * at / life)).add(0, 0.6, 0), 3, 0.01, 0.04));
        }
        sound(sl, ground, ModSounds.BEAUTY_SUMMON_ARRIVAL, 1.0f, wobble(1.0f));
    }

    /** Something summoned stands in the world: Spirit World mist round it and rings rising. */
    public static void summonArrives(ServerLevel sl, LivingEntity summoned) {
        SpellFx.risingRings(sl, summoned.position(), Math.max(0.8, summoned.getBbWidth()), summoned.getBbHeight() + 0.4, 3, 0xC0B8A8FF);
        ParticleShapes.burst(sl, MIST.color(0xA07A9AFF), chestOf(summoned), 12, 0.02, 0.08);
        ParticleShapes.burst(sl, SPIRIT_MOTE, chestOf(summoned), 10, 0.03, 0.1);
        sound(sl, summoned.position(), ModSounds.BEAUTY_SUMMON_ARRIVAL, 0.8f, wobble(1.1f));
    }

    /** The door goes wrong: something from the dark Cosmos looks through it, a great green eye in the doorway. */
    public static void summonAnomaly(ServerLevel sl, Vec3 ground, float yaw) {
        ModelFx eye = GAZE_EYE.color(0xFF90FFB0).scale(2.6f).lifetime(50).fade(6, 14)
                .key(0, pose().frame(0).scale(0.5f).alpha(0f))
                .key(10, pose().scale(1f).alpha(1f), Ease.OUT)
                .during(10, 20, pose().frame(2), Ease.OUT)
                .during(36, 46, pose().frame(0), Ease.IN);
        ParticleShapes.model(sl, eye, ground.add(0, 1.6, 0).add(ahead(yaw).scale(0.3)), yaw, 0f, 0f);
        ParticleShapes.burst(sl, DARK_MOTE.color(0xE060FF90), ground.add(0, 1.6, 0), 20, 0.04, 0.12);
        play(sl, ground, SoundEvents.WITHER_AMBIENT, 0.8f, 0.5f);
    }

    // ---------- Creation (Life-Giver, Sequence 2)

    /** The tick the Seed of Life is in full bloom: it starts healing then. */
    public static final int SEED_BLOOMS = 44;
    /** Fewer ticks than this are too few for the seed to bloom and wilt again. */
    public static final int SEED_SHORTEST = SEED_BLOOMS + 30;

    /**
     * The Seed of Life falls on 'ground', splits, sprouts and blooms (at SEED_BLOOMS) inside a golden seal 'radius'
     * wide; it stands glowing until 'ticks' and wilts away. 'tag' names it.
     */
    public static void seedOfLife(ServerLevel sl, Vec3 ground, double radius, int ticks, String tag) {
        int life = Math.max(ticks, SEED_SHORTEST);
        int wilt = life - 20;
        ModelFx seed = SEED_OF_LIFE.scale(1.4f).lifetime(life).fade(0, 10).tag(tag).aura(0x30FFE08A, 0.06f, 3)
                .key(0, pose().frame(0).up(4f).alpha(0f))
                .key(3, pose().alpha(1f))
                .key(8, pose().up(0f), Ease.OUT_BOUNCE)
                .during(12, 18, pose().frame(1), Ease.OUT)
                .during(20, 30, pose().frame(2), Ease.OUT)
                .during(32, 38, pose().frame(3), Ease.OUT)
                .during(38, SEED_BLOOMS, pose().frame(4), Ease.OUT_BACK)
                .during(wilt, wilt + 14, pose().frame(2).scale(0.7f), Ease.IN);
        ParticleShapes.model(sl, seed, ground, sl.getRandom().nextFloat() * 360f, 0f, 0f);
        seal(sl, ground, radius, 0xB0FFE08A, life, tag);
        sound(sl, ground, ModSounds.BEAUTY_CREATION, 1.1f, 1.0f);
        Later.run(sl, 8, () -> ParticleShapes.burst(sl, LEAF, ground.add(0, 0.3, 0), 8, 0.04, 0.1));
        Later.run(sl, SEED_BLOOMS, () -> {
            SpellFx.risingRings(sl, ground, radius * 0.5, 2.2, 3, 0xB0FFE08A);
            ParticleShapes.burst(sl, LIFE_PETAL, ground.add(0, 2.0, 0), 16, 0.03, 0.1);
            ParticleShapes.burst(sl, GOLD_MOTE, ground.add(0, 2.0, 0), 12, 0.03, 0.08);
        });
    }

    /** Every second while it blooms: a golden wave rolls out and petals drift. */
    public static void seedOfLifePulse(ServerLevel sl, Vec3 ground, double radius) {
        SpellFx.steadyRing(sl, ground, radius, 0x90FFE08A, 20, 0);
        ParticleShapes.spiral(sl, LIFE_PETAL, ground.add(0, 1.0, 0), 3, 12, 0.4, radius * 0.7, 0.5, 0.05);
        ParticleShapes.burst(sl, LEAF, ground.add(0, 0.4, 0), 3, 0.02, 0.06);
    }

    /** Someone the seed heals. */
    public static void seedOfLifeHeal(ServerLevel sl, LivingEntity entity) {
        ParticleShapes.ring(sl, GOLD_MOTE, entity.position().add(0, 0.2, 0), Math.max(0.4, entity.getBbWidth() * 0.6), 10, 0);
        ParticleShapes.burst(sl, HEAL_MOTE, chestOf(entity), 5, 0.01, 0.04);
        sound(sl, entity.position(), ModSounds.BEAUTY_HEAL_CHIME, 0.4f, wobble(1.0f));
    }

    /** A fallen ally brought back: a column of golden light where they rise, and petals. */
    public static void seedOfLifeRevive(ServerLevel sl, Vec3 at) {
        ModelFx column = MOON_BEAM.color(0xC0FFE08A).scale(1.2f, 1.6f, 1.2f).lifetime(40).fade(4, 20)
                .key(0, pose().scale(0.2f))
                .key(8, pose().scale(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, column, at, 0f, 0f, 0f);
        SpellFx.risingRings(sl, at, 1.2, 2.4, 4, 0xC0FFE08A);
        ParticleShapes.burst(sl, LIFE_PETAL, at.add(0, 1.2, 0), 20, 0.04, 0.12);
        sound(sl, at, ModSounds.BEAUTY_CREATION, 0.8f, 1.3f);
        play(sl, at, SoundEvents.PLAYER_LEVELUP, 0.5f, 1.4f);
    }

    /** The seed is gone early. */
    public static void seedOfLifeEnd(ServerLevel sl, Vec3 ground, String tag) {
        ParticleShapes.clearModels(sl, ground, tag);
        ParticleShapes.burst(sl, LEAF, ground.add(0, 0.8, 0), 12, 0.04, 0.1);
        ParticleShapes.burst(sl, LIFE_PETAL, ground.add(0, 1.4, 0), 10, 0.03, 0.08);
    }

    // ---------- Beauty (Beauty Goddess, Sequence 1)

    /** Fewer ticks than this are too few for the goddess to rise and fade. */
    public static final int BEAUTY_SHORTEST = 50;

    /**
     * The caster's beauty laid bare: the goddess of the moon rises behind them with a halo, a crescent crown settles
     * on their head, a rose-silver seal 'radius' wide spreads under them and petals spiral in, for 'ticks'.
     * 'tag' names it (on the caster and on the ground).
     */
    public static void beauty(ServerLevel sl, Player caster, double radius, int ticks, String tag) {
        int life = Math.max(ticks, BEAUTY_SHORTEST);
        ParticleShapes.clearModels(sl, caster, tag);
        float yaw = caster.getYRot();
        Vec3 behind = caster.position().subtract(ahead(yaw).scale(1.6));
        int period = periodFor(life - 20, 30, 40);
        ModelFx goddess = swinging(GODDESS.scale(1.6f).lifetime(life).fade(0, 20).tag(tag)
                        .key(0, pose().up(-1.5f).alpha(0f))
                        .key(24, pose().up(0f).alpha(0.85f), Ease.OUT),
                24, life - 20, period, pose().up(0.15f), pose().up(0f));
        ParticleShapes.model(sl, goddess, behind, yaw, 0f, 0f);
        ModelFx halo = MOON_HALO.color(0xB0FFE0F0).scale(2.2f).lifetime(life).fade(10, 20).tag(tag).spin(0.8f)
                .key(0, pose().scale(0.3f).alpha(0f))
                .key(30, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, halo, behind.add(0, 1.6 * 2.9 - 0.3, 0).subtract(ahead(yaw).scale(0.6)), yaw, 0f, 0f);
        ModelFx crown = CRESCENT_CROWN.lifetime(life).fade(0, 10).tag(tag)
                .key(0, pose().up(1.2f).yaw(-180f).alpha(0f))
                .key(16, pose().up(0f).yaw(0f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOnHead(sl, crown, caster, 0, 0f, 0f, 0f);
        seal(sl, caster.position(), radius, 0xB0F2A0C0, life, tag + "_ground");
        ParticleShapes.spiral(sl, PETAL, caster.position().add(0, 0.6, 0), 4, 40, 1.0, radius, 1.0, -0.12);
        sound(sl, caster.position(), ModSounds.BEAUTY_BEAUTY, 1.2f, 1.0f);
    }

    /** An enemy held in awe: a rose circle under it, sparkles and a crescent over it, for 'ticks'. */
    public static void beautyEnthralled(ServerLevel sl, LivingEntity enemy, int ticks) {
        SpellFx.circleUnder(sl, enemy, Math.max(0.6, enemy.getBbWidth() * 0.8), 0xC0F2A0C0, ticks, "bg_beauty_awe");
        ParticleShapes.burst(sl, SPARKLE, enemy.position().add(0, enemy.getBbHeight() + 0.3, 0), 6, 0.01, 0.04);
        ParticleShapes.burst(sl, CRESCENT_MOTE, enemy.position().add(0, enemy.getBbHeight() + 0.4, 0), 2, 0.005, 0.02);
    }

    /** Drawn toward the caster: a faint rose thread and petals along it. */
    public static void beautyPull(ServerLevel sl, LivingEntity enemy, Player caster) {
        SpellFx.tether(sl, ROSE_LINE, enemy, caster, 6);
        ParticleShapes.line(sl, PETAL, chestOf(enemy), chestOf(caster), 0.6);
    }

    /** It ends early (on the caster: the crown, the goddess and the seal go). */
    public static void beautyEnd(ServerLevel sl, Player caster, String tag) {
        ParticleShapes.clearModels(sl, caster, tag);
        ParticleShapes.clearModels(sl, caster.position(), tag);
        ParticleShapes.clearModels(sl, caster.position(), tag + "_ground");
        ParticleShapes.burst(sl, PETAL, chestOf(caster), 16, 0.04, 0.12);
    }

    // ---------- Blood Moon (Moon, Sequence 0)

    /** The tick the Blood Moon stands in the sky. */
    public static final int BLOOD_MOON_RISEN = 50;

    /**
     * The Blood Moon: a huge red moon rises over 'center', a dim red light falls on everything 'radius' around, a
     * crimson seal spreads on the ground, for 'ticks'. 'tag' names it.
     */
    public static void bloodMoon(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        double high = 22 + radius * 0.4;
        float size = (float) Math.max(8.0, radius * 0.9);
        ModelFx moon = BLOOD_MOON.scale(size).lifetime(ticks).fade(0, 30).spin(0.15f).tag(tag)
                .aura(0x40FF2030, 0.6f, 4)
                .key(0, pose().up(-14f).alpha(0f))
                .key(BLOOD_MOON_RISEN, pose().up(0f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, moon, center.add(0, high, 0), 0f, 0f, 0f);
        float width = (float) (radius * 2.0);
        ModelFx light = MOON_BEAM.color(0x30FF2030).scale(width, (float) (high / 3.0), width)
                .lifetime(Math.max(20, ticks - BLOOD_MOON_RISEN + 20)).fade(20, 30).tag(tag).delay(BLOOD_MOON_RISEN - 20);
        ParticleShapes.model(sl, light, center.add(0, 0.05, 0), 0f, 0f, 0f);
        seal(sl, center, radius, 0xC0E0283C, ticks, tag);
        ParticleShapes.ring(sl, MIST.color(0x90A01020).endColor(0x00400008), center.add(0, 0.4, 0), radius, 30, 0.0);
        sound(sl, center, ModSounds.BEAUTY_BLOOD_MOON, 1.6f, 1.0f);
    }

    /** Every second under the Blood Moon: blood rains down and red mist rolls at its edge. */
    public static void bloodMoonTick(ServerLevel sl, Vec3 center, double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 18; i++) {
            double angle = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            Vec3 at = center.add(Math.cos(angle) * far, 5 + random.nextDouble() * 3, Math.sin(angle) * far);
            ParticleShapes.spawn(sl, BLOOD_DROP, at, new Vec3(0, -0.3, 0));
        }
        ParticleShapes.ring(sl, MIST.color(0x70A01020).endColor(0x00400008), center.add(0, 0.4, 0), radius, 12, 0.0);
    }

    /** An enemy under it burns with moon-fire. */
    public static void bloodMoonBurn(ServerLevel sl, LivingEntity enemy) {
        ParticleShapes.burst(sl, BLOOD_EMBER, chestOf(enemy), 6, 0.02, 0.06);
    }

    /** The caster (or an ally) drinks its light: crimson rings and motes. */
    public static void bloodMoonEmpower(ServerLevel sl, LivingEntity who) {
        SpellFx.risingRings(sl, who.position(), 0.9, 2.0, 3, 0xC0E0283C);
        ParticleShapes.burst(sl, BLOOD_MOTE, chestOf(who), 8, 0.02, 0.06);
    }

    /** It sets early. */
    public static void bloodMoonEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.burst(sl, BLOOD_MOTE, center.add(0, 1, 0), 24, 0.04, 0.12);
    }

    // =====================================================================================
    // /beautyfx: every display, to look at in game
    // =====================================================================================

    @FunctionalInterface
    private interface Show {
        void play(ServerLevel sl, Player viewer, LivingEntity target, Vec3 spot);
    }

    /** onViewer = it is shown on the viewer (worn), not on a spot or a target. */
    private record Case(String name, int ticks, boolean onViewer, Show show) {}

    private static String tagFor(String name) {
        return "bg_fx_" + name + "_" + UUID.randomUUID();
    }

    private static final List<Case> CASES = List.of(
            new Case("medicine", 100, false, (sl, v, t, spot) -> {
                Vec3 from = v.getEyePosition();
                int ticks = throwFlask(sl, from, spot, MEDICINE);
                Later.run(sl, ticks, () -> {
                    flaskShatter(sl, spot.add(0, 0.4, 0), MEDICINE, 3);
                    String tag = tagFor("medicine");
                    medicineCloud(sl, spot, 3, 80, tag);
                    for (int i = 1; i < 8; i++) Later.run(sl, i * 10, () -> medicineCloudTick(sl, spot, 3));
                    if (t != v) Later.run(sl, 20, () -> medicinePoison(sl, t));
                    Later.run(sl, 30, () -> medicineHeal(sl, v));
                });
            }),
            new Case("spirit_vision", 60, true, (sl, v, t, spot) -> {
                spiritVision(sl, v, 16);
                if (t != v) Later.run(sl, 10, () -> spiritVisionMark(sl, t, 60, t.getHealth() / Math.max(1f, t.getMaxHealth())));
            }),
            new Case("tame", 80, false, (sl, v, t, spot) -> {
                if (t == v) {
                    tameResisted(sl, v);
                    return;
                }
                tame(sl, v, t, 70);
            }),
            new Case("animal_sense", 70, true, (sl, v, t, spot) -> {
                animalSense(sl, v, 16);
                if (t != v) Later.run(sl, 10, () -> animalSenseHunt(sl, t, 60));
            }),
            new Case("wings", 70, true, (sl, v, t, spot) -> {
                String tag = tagFor("wings");
                wings(sl, v, 70, tag);
                Later.run(sl, 60, () -> wingsBurst(sl, v, tag));
            }),
            new Case("shackles", 80, false, (sl, v, t, spot) -> {
                shackles(sl, t, 80);
                if (t != v) for (int i = 1; i < 4; i++) Later.run(sl, i * 20, () -> shacklesDrain(sl, t, v));
                Later.run(sl, 80, () -> shacklesEnd(sl, t));
            }),
            new Case("invisibility", 70, true, (sl, v, t, spot) -> {
                drink(sl, v, INVISIBILITY);
                Later.run(sl, DRINK_DONE, () -> invisibility(sl, v));
                Later.run(sl, 70, () -> invisibilityEnd(sl, v));
            }),
            new Case("fire_breath", 70, true, (sl, v, t, spot) -> {
                drink(sl, v, FIRE);
                Later.run(sl, DRINK_DONE, () -> fireBreath(sl, v));
                for (int i = DRINK_DONE + 2; i < 70; i += 2) Later.run(sl, i, () -> fireBreathTick(sl, v));
            }),
            new Case("sun_water", 50, false, (sl, v, t, spot) -> {
                int ticks = throwFlask(sl, v.getEyePosition(), spot, SUN);
                Later.run(sl, ticks, () -> {
                    sunWater(sl, spot.add(0, 0.4, 0), 3);
                    if (t != v) sunWaterBurn(sl, t);
                });
            }),
            new Case("shadow", 80, true, (sl, v, t, spot) -> {
                drink(sl, v, SHADOW);
                String tag = tagFor("shadow");
                Later.run(sl, DRINK_DONE, () -> shadowForm(sl, v, 60, tag));
                for (int i = DRINK_DONE + 5; i < 78; i += 5) Later.run(sl, i, () -> shadowFormTick(sl, v));
                Later.run(sl, 78, () -> shadowFormEnd(sl, v, tag));
            }),
            new Case("moon", 140, false, (sl, v, t, spot) -> {
                String tag = tagFor("moon");
                lunarBattlefield(sl, spot, 6, 140, tag);
                for (int i = 1; i < 4; i++) Later.run(sl, MOON_RISEN + i * 30, () -> lunarBattlefieldPulse(sl, spot, 6));
                Later.run(sl, 90, () -> flashTeleport(sl, spot.add(-3, 0, 0), spot.add(3, 0, 0)));
            }),
            new Case("moonlight_form", 100, true, (sl, v, t, spot) -> {
                String tag = tagFor("moonlight");
                moonlightForm(sl, v, 80, tag);
                for (int i = 1; i < 16; i++) Later.run(sl, i * 5, () -> moonlightFormTick(sl, v));
                Later.run(sl, 40, () -> {
                    moonlightShatter(sl, chestOf(v));
                    moonlightReform(sl, v);
                });
                Later.run(sl, 80, () -> moonlightFormEnd(sl, v, tag));
            }),
            new Case("gaze", 90, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("gaze");
                gaze(sl, v, t, 70, tag);
                for (int i = 1; i < 6; i++) Later.run(sl, GAZE_GRIPS + i * 10, () -> gazeTick(sl, t));
                Later.run(sl, 70, () -> gazeCrush(sl, v, t, tag));
            }),
            new Case("bat_swarm", 100, true, (sl, v, t, spot) -> {
                String tag = tagFor("swarm");
                batSwarm(sl, v, 90, tag);
                for (int i = 1; i < 18; i++) Later.run(sl, i * 5, () -> batSwarmTick(sl, v));
                Later.run(sl, 50, () -> batSwarmBatLost(sl, chestOf(v).add(1.2, 0.3, 0)));
                Later.run(sl, 90, () -> batSwarmEnd(sl, v, tag));
            }),
            new Case("door", 150, false, (sl, v, t, spot) -> {
                float yaw = v.getYRot() + 180f;                                 // its front toward the viewer
                summoningDoor(sl, spot, yaw, 140, tagFor("door"));
                Later.run(sl, DOOR_OPEN + 4, () -> spiritCharge(sl, spot, yaw));
                Later.run(sl, DOOR_OPEN + 50, () -> summonAnomaly(sl, spot, yaw));
                if (t != v) Later.run(sl, DOOR_OPEN + 20, () -> summonArrives(sl, t));
            }),
            new Case("creation", 140, false, (sl, v, t, spot) -> {
                String tag = tagFor("seed");
                seedOfLife(sl, spot, 4, 130, tag);
                for (int i = 0; i < 4; i++) Later.run(sl, SEED_BLOOMS + i * 20, () -> seedOfLifePulse(sl, spot, 4));
                Later.run(sl, SEED_BLOOMS + 10, () -> seedOfLifeHeal(sl, v));
                Later.run(sl, SEED_BLOOMS + 30, () -> seedOfLifeRevive(sl, spot.add(1.5, 0, 1.5)));
            }),
            new Case("beauty", 120, true, (sl, v, t, spot) -> {
                beauty(sl, v, 8, 110, tagFor("beauty"));
                if (t != v) {
                    Later.run(sl, 24, () -> beautyEnthralled(sl, t, 80));
                    for (int i = 0; i < 4; i++) Later.run(sl, 24 + i * 15, () -> beautyPull(sl, t, v));
                }
            }),
            new Case("blood_moon", 180, false, (sl, v, t, spot) -> {
                bloodMoon(sl, spot, 10, 170, tagFor("blood_moon"));
                for (int i = 0; i < 6; i++) Later.run(sl, BLOOD_MOON_RISEN + i * 20, () -> {
                    bloodMoonTick(sl, spot, 10);
                    if (t != v) bloodMoonBurn(sl, t);
                });
                Later.run(sl, BLOOD_MOON_RISEN, () -> bloodMoonEmpower(sl, v));
            }));

    /** The names /beautyfx knows. */
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
        event.getDispatcher().register(Commands.literal("beautyfx")
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
