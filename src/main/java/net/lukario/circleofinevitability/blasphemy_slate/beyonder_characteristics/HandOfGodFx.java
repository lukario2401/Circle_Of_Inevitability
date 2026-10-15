package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
 * How the Hand Of God (Twilight Giant) pathway looks and sounds: every key, both casts, every wheel entry, the
 * growth at higher Sequences (Silver Armor, Cage, the larger Giant) and the passives that show. Visual only: nothing
 * here hurts, heals, roots, hides or moves anyone. The ability code does that, at the ticks named by the constants
 * below (..._RISEN, ..._GRIPS, ..._HITS...), with Later.run. See docs/HAND_OF_GOD.md.
 *
 * ---------- See them in game ----------
 *   /handfx all              every display one after another (/handfx all 3 = three times over)
 *   /handfx hurricane        just that one (press TAB after /handfx for the names)
 * Displays that need a target use the nearest mob within 12 blocks, or you when there is none.
 *
 * ---------- Use them in an ability ----------
 *   Key 1  chargeStart(sl, player), chargeTrail(sl, player) every tick of the rush, chargeImpact(sl, player, target)
 *          sneak: divineBlood(sl, player, ticks, tag), divineBloodEnd(sl, player, tag)
 *   Key 2  flurryBlow(sl, player, target, index) for index 0..4 (the last one is the big one)
 *          sneak: resistance(sl, player, ticks, tag), resistanceShrug(sl, player), resistanceEnd(sl, player, tag)
 *   Key 3  grandmastersEdge(sl, player, ticks), edgeSwing(sl, player, index), edgeFinisher(sl, player, target), edgeTick(sl, player)
 *          sneak: riposte(sl, player), riposteParry(sl, player, attacker), riposteCounter(sl, player, target)
 *   Key 4  the Dawn wheel's pictures: WHEEL_LIGHT_OF_DAWN, WHEEL_DAWN_ARMOR, WHEEL_SWORD_OF_DAWN, WHEEL_HURRICANE
 *          lightOfDawn(sl, center, radius, ticks, tag) (bright at LIGHT_RISEN), lightOfDawnPulse(sl, center, radius),
 *              lightOfDawnBurn(sl, evil), lightOfDawnReveal(sl, hidden), lightOfDawnHeal(sl, ally), lightOfDawnEnd(sl, center, tag)
 *          dawnArmor(sl, player, ticks, tag, silver), armorHit(sl, player, silver), dawnArmorEnd(sl, player, tag, silver)
 *          swordOfDawn(sl, player, ticks, tag), dawnSlash(sl, player, crescent), purify(sl, target), swordOfDawnEnd(sl, player, tag)
 *          hurricaneGather(sl, player) (released at HURRICANE_GATHERS), then hurricane(sl, ground, yaw, length, ticks, glory)
 *              which returns its ticks; hurricaneHit(sl, entity, glory) on each one it reaches
 *   Key 5  protection(sl, player, center, radius, ticks, tag) (raised at PROTECTION_RAISED), domeHit(sl, at),
 *              domeWeaken(sl, center, radius), protectionEnd(sl, center, tag); the Glory form: cage(sl, center, radius, ticks, tag)
 *              (closed at CAGE_CLOSED), cageEnd(sl, center, tag); dome(...) and sphere(...) on their own for any shell
 *          sneak: transference(sl, player, allies, ticks, tag), transferenceTaken(sl, ally, guardian), transferenceEnd(sl, player, allies, tag)
 *   Key 6  the wheel's pictures: WHEEL_EYE, WHEEL_ALCHEMY, WHEEL_MIND_CONCEALMENT, WHEEL_GIANT_FORM
 *          eye(sl, player, ticks, tag) (open at EYE_OPENS), eyeMark(sl, target, ticks, tag), eyeEvil(sl, evil, ticks), eyeEnd(sl, player, tag)
 *          alchemy(sl, player, coat) (coated at VIAL_POURED), coatTick(sl, player, coat), coatHit(sl, target, coat)
 *          mindConcealment(sl, player, ticks, tag), mindConcealmentEnd(sl, player, tag)
 *          giantForm(sl, player, height, ticks, tag) (full size at GIANT_RISEN), giantStep(sl, player, height),
 *              giantTerror(sl, onlooker), giantMadness(sl, player, madness), giantFormEnd(sl, player, height, tag)
 *   Key 7  the wheel's pictures: WHEEL_MERCURY, WHEEL_RAPIER, WHEEL_LIGHT_CONCEALMENT, WHEEL_TWILIGHT_SWORD,
 *              WHEEL_DIVINE_HAND, WHEEL_PASSAGE_OF_TIME, WHEEL_DECAY, WHEEL_DIVINE_KINGDOM
 *          mercury(sl, player, ticks, tag) (melted at MERCURY_MELTED), mercuryTick(sl, player), mercuryWrap(sl, enemy, ticks),
 *              mercuryReform(sl, player, tag), mercuryArmor(sl, ally, ticks, tag)
 *          silverRapiers(sl, target, count) (they strike at RAPIER_STRIKES)
 *          lightEnter(sl, player, light), lightExit(sl, player, at), withering(sl, player)
 *          twilightSword(sl, ground) (it lands at TWILIGHT_SWORD_HITS)
 *          divineHand(sl, player, target, ticks, tag) (it grips at HAND_GRIPS), divineHandFling(sl, target, tag),
 *              divineHandSlam(sl, player, ground, radius) (it lands at HAND_SLAMS)
 *          passageOfTime(sl, center, radius, ticks, tag) (frozen at TIME_FROZEN), timeFrozen(sl, enemy, ticks, tag),
 *              timeResume(sl, enemy, tag)
 *          decay(sl, center, radius, ticks, tag), decayTick(sl, center, radius), decayOn(sl, enemy), decayEnd(sl, center, tag)
 *          divineKingdom(sl, player, radius, ticks, tag) (descended at KINGDOM_RISEN), kingdomTick(sl, center, radius),
 *              kingdomAcknowledged(sl, ally), kingdomEnd(sl, player, center, tag)
 *   Passives: dawnAura(sl, player, radius) once a second, footprint(sl, at, yaw), divineTouch(sl, target)
 *   Every timed cast: status(player, Status.X, ticks) shows its picture and its clock; statusEnd(player, Status.X) takes them away.
 *
 * The models are in models/model_fx/hand_of_god/ (made by tools/model_gen/hand_of_god/models.py; each JSON's
 * "credit" gives its size and pivot), the pictures in textures/gui/hand_of_god/ and textures/gui/ability_menus/
 * hand_of_god/ (icons.py), the particles in textures/particle/fx/ (particles.py), the sounds in
 * sounds/hand_of_god/ (sounds.py) and sounds/ModSounds (HAND_...).
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class HandOfGodFx {

    private HandOfGodFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // Colors (ARGB)
    // =====================================================================================

    /** The Pathway's own color: tangerine, the orange-red of dusk. */
    public static final int PATHWAY_COLOR = 0xFFFF7A1A;
    public static final int DAWN = 0xFFFFD86A;
    public static final int DAWN_WHITE = 0xFFFFF6D8;
    public static final int SILVER = 0xFFD8DEEA;
    public static final int GLORY = 0xFFFF7A1A;
    public static final int TWILIGHT = 0xFFE0482A;
    public static final int HUNTER = 0xFF3AD06A;
    public static final int STEEL = 0xFFB8C0D0;
    public static final int GUARD = 0xFFFFE8A0;
    public static final int BLOOD = 0xFFD02828;
    public static final int DECAY = 0xFF8A6A3A;
    public static final int GIANT_BLUE = 0xFF7A8AA0;

    /** Alchemy's coatings, each with its color (the vial's liquid, the glow on the weapon, the hit). */
    public enum Coat {
        PURIFICATION(0xFFFFE89A),
        FREEZE(0xFF9AD8FF),
        LIGHTNING(0xFFC8B0FF),
        BURNING(0xFFFF8A2A),
        DECAY(0xFF9A7A3A),
        EXORCISM(0xFFB0FFC8);

        public final int color;

        Coat(int color) {
            this.color = color;
        }
    }

    // =====================================================================================
    // The small particles everything shares
    // =====================================================================================

    private static final ParticleFx DAWN_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFFF0B0).endColor(0x00FFB040)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(24, 12).gravity(-0.025f).friction(0.92f).glow();
    private static final ParticleFx DAWN_RAY = ParticleFx.of("fx/ray")
            .color(0xFFFFF8D8).endColor(0x00FFC860)
            .size(0.16f).endSize(0.06f).sizeRandom(0.4f)
            .lifetime(16, 8).gravity(-0.05f).friction(0.9f).glow();
    private static final ParticleFx DAWN_STAR = ParticleFx.of("fx/feather_light")
            .color(0xFFFFFFFF).endColor(0x00FFD86A)
            .size(0.08f).endSize(0.03f).sizeRandom(0.5f)
            .lifetime(20, 10).gravity(-0.01f).friction(0.92f).glow().randomRotation();
    private static final ParticleFx SILVER_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFF4F8FF).endColor(0x0090A0C0)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(20, 10).gravity(-0.02f).friction(0.92f).glow();
    private static final ParticleFx SILVER_DROP = ParticleFx.of("fx/droplet")
            .color(0xFFE8ECF4).endColor(0xC0A0A8B8)
            .size(0.06f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(22, 8).gravity(0.07f).friction(0.98f).collide();
    private static final ParticleFx STEEL_SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFFFFFF).endColor(0x00FFC060)
            .size(0.06f).endSize(0.02f)
            .lifetime(10, 6).gravity(0.04f).friction(0.88f).glow();
    private static final ParticleFx GLORY_EMBER = ParticleFx.of("fx/glow")
            .color(0xFFFFB050).endColor(0x00C02010)
            .size(0.1f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(20, 10).gravity(-0.03f).friction(0.92f).glow();
    private static final ParticleFx ASH = ParticleFx.of("fx/ash")
            .color(0xE0A08A6A).endColor(0x40403020)
            .size(0.07f).endSize(0.05f).sizeRandom(0.5f)
            .lifetime(40, 20).gravity(0.006f).friction(0.95f).spin(4f).randomRotation();
    private static final ParticleFx GLOWING_ASH = ASH.color(0xFFFFA050).endColor(0x40602010).glow();
    private static final ParticleFx DUST = ParticleFx.of("fx/smoke")
            .color(0xA08A7A64).endColor(0x00605040)
            .size(0.3f).endSize(0.7f).sizeRandom(0.3f)
            .lifetime(22, 10).gravity(-0.005f).friction(0.88f).spin(2f).randomRotation();
    private static final ParticleFx DECAY_SMOKE = ParticleFx.of("fx/smoke")
            .color(0xA05A4428).endColor(0x00201408)
            .size(0.35f).endSize(0.9f).sizeRandom(0.3f)
            .lifetime(34, 14).gravity(-0.008f).friction(0.9f).spin(1.5f).randomRotation();
    private static final ParticleFx DUSK_SMOKE = ParticleFx.of("fx/smoke")
            .color(0x90FF7A3A).endColor(0x00602010)
            .size(0.4f).endSize(1.0f).sizeRandom(0.3f)
            .lifetime(36, 14).gravity(-0.01f).friction(0.9f).spin(1.5f).randomRotation();
    private static final ParticleFx BLOOD_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFF4A40).endColor(0x00700810)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(18, 8).gravity(-0.03f).friction(0.9f).glow();
    private static final ParticleFx[] RUNES = {
            ParticleFx.of("fx/rune_0").color(0xFF6AFF8A).endColor(0x0020A040).size(0.09f).endSize(0.07f)
                    .lifetime(26, 10).gravity(-0.01f).friction(0.9f).glow(),
            ParticleFx.of("fx/rune_1").color(0xFF6AFF8A).endColor(0x0020A040).size(0.09f).endSize(0.07f)
                    .lifetime(26, 10).gravity(-0.01f).friction(0.9f).glow(),
            ParticleFx.of("fx/rune_2").color(0xFF6AFF8A).endColor(0x0020A040).size(0.09f).endSize(0.07f)
                    .lifetime(26, 10).gravity(-0.01f).friction(0.9f).glow()};
    private static final ParticleFx HOURGLASS = ParticleFx.of("fx/hourglass")
            .color(0xFFFFC080).endColor(0x00FF7A1A)
            .size(0.1f).endSize(0.08f).sizeRandom(0.3f)
            .lifetime(30, 10).gravity(-0.005f).friction(0.95f).glow().randomRotation();
    private static final ParticleFx FOOTPRINT = ParticleFx.of("fx/footprint")
            .color(0xC03AD06A).endColor(0x0020A040)
            .size(0.12f).endSize(0.12f)
            .lifetime(400, 0).gravity(0f).friction(0f).glow();
    private static final ParticleFx MIND_MIST = ParticleFx.of("fx/smoke")
            .color(0x80203028).endColor(0x00101810)
            .size(0.25f).endSize(0.6f).sizeRandom(0.3f)
            .lifetime(24, 10).gravity(-0.012f).friction(0.9f).spin(2f).randomRotation();

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

    /** One block the way a yaw looks, level with the ground. */
    private static Vec3 ahead(float yaw) {
        double angle = Math.toRadians(yaw);
        return new Vec3(-Math.sin(angle), 0, Math.cos(angle));
    }

    private static int rgbOf(int argb) {
        return argb & 0x00FFFFFF;
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    // =====================================================================================
    // Statuses: a picture in the row above the hotbar stacks, and a clock beside the hotbar
    // =====================================================================================

    /** The picture for the Pathway's stacks, if it gets them (textures/gui/hand_of_god/stack_sun.png). */
    public static final String STACK_IMAGE = "hand_of_god/stack_sun";

    public enum Status {
        DIVINE_BLOOD("divine_blood", "Divine Blood", 0xE03030),
        RESISTANCE("resistance", "Resistance", 0xC8D0E0),
        GRANDMASTER("grandmaster", "Grandmaster", 0xF4F8FF),
        RIPOSTE("riposte", "Riposte", 0xFFE8A0),
        LIGHT_OF_DAWN("light_of_dawn", "Dawn", 0xFFD86A),
        DAWN_ARMOR("dawn_armor", "Dawn Armor", 0xFFD86A),
        SWORD_OF_DAWN("sword_of_dawn", "Sword of Dawn", 0xFFF0B0),
        PROTECTION("protection", "Protection", 0xFFE8A0),
        TRANSFERENCE("transference", "Transference", 0xFFC060),
        EYE("eye", "Demon Hunting", 0x3AD06A),
        ALCHEMY("alchemy", "Coating", 0x6AFF8A),
        CONCEALMENT("concealment", "Concealed", 0x4A8A5A),
        GIANT("giant", "Giant", 0x7A8AA0),
        MADNESS("madness", "Madness", 0xC04050),
        MERCURY("mercury", "Mercury", 0xD8DEEA),
        LIGHT_CONCEALMENT("light_concealment", "In the Light", 0xFFF6D8),
        DIVINE_HAND("divine_hand", "Divine Hand", 0xFF7A1A),
        TIME_STOP("time_stop", "Time Stopped", 0xFFB060),
        DECAY("decay", "Decay", 0x9A7A3A),
        KINGDOM("kingdom", "Divine Kingdom", 0xFF7A1A),
        CAGE("cage", "Cage", 0xFF9A4A);

        final String image;
        final String label;
        final int rgb;

        Status(String image, String label, int rgb) {
            this.image = image;
            this.label = label;
            this.rgb = rgb;
        }

        String id() {
            return "hg_status_" + name().toLowerCase();
        }
    }

    /** Shows the status's picture (in the row above the hotbar stacks, rows of nine) and a clock beside the hotbar. */
    public static void status(Player player, Status status, int ticks) {
        int slot = status.ordinal();
        ScreenImages.show(player, status.id(), "hand_of_god/" + status.image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, ticks);
        HudTimer.show(player, status.id(), status.label, ticks, status.rgb);
    }

    /** It ended early. */
    public static void statusEnd(Player player, Status status) {
        ScreenImages.hide(player, status.id());
        HudTimer.hide(player, status.id());
    }

    // =====================================================================================
    // The wheels' pictures (textures/gui/ability_menus/hand_of_god/, for AbilityWheel.entry's icon)
    // =====================================================================================

    public static final String WHEEL_LIGHT_OF_DAWN = "ability_menus/hand_of_god/light_of_dawn";
    public static final String WHEEL_DAWN_ARMOR = "ability_menus/hand_of_god/dawn_armor";
    public static final String WHEEL_SWORD_OF_DAWN = "ability_menus/hand_of_god/sword_of_dawn";
    public static final String WHEEL_HURRICANE = "ability_menus/hand_of_god/hurricane_of_light";
    public static final String WHEEL_EYE = "ability_menus/hand_of_god/eye_of_demon_hunting";
    public static final String WHEEL_ALCHEMY = "ability_menus/hand_of_god/alchemy";
    public static final String WHEEL_MIND_CONCEALMENT = "ability_menus/hand_of_god/mind_concealment";
    public static final String WHEEL_GIANT_FORM = "ability_menus/hand_of_god/giant_form";
    public static final String WHEEL_MERCURY = "ability_menus/hand_of_god/mercury_liquefaction";
    public static final String WHEEL_RAPIER = "ability_menus/hand_of_god/silver_rapier";
    public static final String WHEEL_LIGHT_CONCEALMENT = "ability_menus/hand_of_god/light_concealment";
    public static final String WHEEL_TWILIGHT_SWORD = "ability_menus/hand_of_god/twilight_sword";
    public static final String WHEEL_DIVINE_HAND = "ability_menus/hand_of_god/divine_right_hand";
    public static final String WHEEL_PASSAGE_OF_TIME = "ability_menus/hand_of_god/passage_of_time";
    public static final String WHEEL_DECAY = "ability_menus/hand_of_god/decay";
    public static final String WHEEL_DIVINE_KINGDOM = "ability_menus/hand_of_god/divine_kingdom";

    // =====================================================================================
    // The models (models/model_fx/hand_of_god/, see each JSON's "credit")
    // =====================================================================================

    /** The Sword of Dawn, 2.6 blocks long at scale 1, point up, held by the middle of its grip. */
    public static final ModelFx DAWN_SWORD = ModelFx.of("hand_of_god/dawn_sword").pivot(8, -5, 8).glow()
            .aura(0xA0FFD86A, 0.08f, 3);
    /** The Twilight Sword, 2.7 blocks long, point up, held by the middle of its grip. */
    public static final ModelFx TWILIGHT_SWORD = ModelFx.of("hand_of_god/twilight_sword").pivot(8, -5, 8).glow().unshaded()
            .aura(0xA0FF7A1A, 0.12f, 3);
    /** A Silver Rapier, 2.4 blocks long, point up; pivot where the blade leaves the cup. */
    public static final ModelFx SILVER_RAPIER = ModelFx.of("hand_of_god/silver_rapier").glow().unshaded();
    /** The armor, worn on the body (modelOnTurning at the feet) and on the head (modelOnHead). */
    public static final ModelFx DAWN_ARMOR = ModelFx.of("hand_of_god/dawn_armor").glow().seeThrough();
    public static final ModelFx DAWN_HELM = ModelFx.of("hand_of_god/dawn_helm").pivot(8, 8.4f, 8).glow().seeThrough();
    public static final ModelFx SILVER_ARMOR = ModelFx.of("hand_of_god/silver_armor");
    public static final ModelFx SILVER_HELM = ModelFx.of("hand_of_god/silver_helm").pivot(8, 8.4f, 8);
    /** The Divine Right Hand, 2 blocks at scale 1, five steps from open (0) to a fist (4). Palm = front. */
    public static final ModelFx GIANT_HAND = ModelFx.of("hand_of_god/giant_hand").frames(5).smooth().pivot(8, 6, 8)
            .glow().unshaded();
    /** The one-eyed Giant, 3 blocks tall at scale 1. Face = front. */
    public static final ModelFx GIANT = ModelFx.of("hand_of_god/giant").pivot(8, -16, 8);
    /** The Demon Hunter's vial and the coating inside it (tinted with .color(...)). */
    public static final ModelFx VIAL = ModelFx.of("hand_of_god/hunter_vial").pivot(8, 5, 8).glow().seeThrough();
    public static final ModelFx VIAL_LIQUID = ModelFx.of("hand_of_god/hunter_vial_liquid").pivot(8, 5, 8).glow().seeThrough();
    /** The two halves of the dawn seal, 1 block wide, white: tint them and turn them against each other. */
    public static final ModelFx DAWN_SEAL_RING = ModelFx.of("hand_of_god/dawn_seal_ring").glow().unshaded().seeThrough();
    public static final ModelFx DAWN_SEAL_CORE = ModelFx.of("hand_of_god/dawn_seal_core").glow().unshaded().seeThrough();
    /** One tile of a dome of light, 1 block square, standing, white; its pivot is the middle of its bottom edge. */
    public static final ModelFx DAWN_WALL = ModelFx.of("hand_of_god/dawn_wall").glow().unshaded().seeThrough();
    /** A funnel of light 3 blocks tall and 2.6 wide at the top, white; its pivot is the bottom tip. */
    public static final ModelFx HURRICANE = ModelFx.of("hand_of_god/hurricane").pivot(8, -16, 8).glow().unshaded().seeThrough();
    /** The Eye of Demon Hunting, 1 block, standing, white. */
    public static final ModelFx EYE_SYMBOL = ModelFx.of("hand_of_god/eye_symbol").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** A puddle of liquid silver, 1 block, flat, its own colors. */
    public static final ModelFx MERCURY_POOL = ModelFx.of("hand_of_god/mercury_pool").glow().seeThrough();
    /** A gate of light, 1 block, standing, white. */
    public static final ModelFx LIGHT_GATE = ModelFx.of("hand_of_god/light_gate").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** The clock of Passage of Time, 1 block, flat, white: the dial and its hands. */
    public static final ModelFx TIME_DIAL = ModelFx.of("hand_of_god/time_dial_ring").glow().unshaded().seeThrough();
    public static final ModelFx TIME_HANDS = ModelFx.of("hand_of_god/time_dial_hands").glow().unshaded().seeThrough();
    /** The crumbling ring of Decay, 1 block, flat, white. */
    public static final ModelFx DECAY_SIGIL = ModelFx.of("hand_of_god/decay_sigil").glow().unshaded().seeThrough();
    /** The setting sun of Twilight, 1 block, standing, its own colors. */
    public static final ModelFx TWILIGHT_SUN = ModelFx.of("hand_of_god/twilight_sun").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** The crown and walls of the Divine Kingdom, 1 block, flat, white. */
    public static final ModelFx KINGDOM_SEAL = ModelFx.of("hand_of_god/kingdom_seal").glow().unshaded().seeThrough();
    /** A kite shield with a sun, 1 block, standing, white. */
    public static final ModelFx AEGIS_MARK = ModelFx.of("hand_of_god/aegis_mark").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** A flash of steel, an eight-pointed star, 1 block, standing, white. */
    public static final ModelFx PARRY_FLASH = ModelFx.of("hand_of_god/parry_flash").pivot(8, 8, 8).glow().unshaded().seeThrough();

    /** A seal on the ground: the two halves of the dawn seal turning against each other, 'radius' wide, for 'ticks'. */
    public static void seal(ServerLevel sl, Vec3 ground, double radius, int color, int ticks, String tag) {
        float width = (float) (radius * 2.0);
        ModelFx ring = DAWN_SEAL_RING.color(color).scale(width).lifetime(ticks).fade(0, 12).spin(0.7f)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(10, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ModelFx core = DAWN_SEAL_CORE.color(color).scale(width * 0.6f).lifetime(ticks).fade(0, 12).spin(-1.2f)
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

    /** A flash of steel at a spot, facing a yaw, 'size' blocks wide. */
    private static void flash(ServerLevel sl, Vec3 at, float yaw, int color, float size) {
        ModelFx star = PARRY_FLASH.color(color).scale(size).lifetime(9).fade(0, 5).spin(12f)
                .key(0, pose().scale(0.3f))
                .key(3, pose().scale(1.15f), Ease.OUT)
                .key(9, pose().scale(0.8f).alpha(0f), Ease.IN);
        ParticleShapes.model(sl, star, at, yaw, 0f, 0f);
    }

    // =====================================================================================
    // Shells of light: a dome (Protection) and a whole sphere (Cage), made of DAWN_WALL tiles.
    // Each tile is one model, so a dome 6 blocks wide is about 45 of them: keep the radius sensible.
    // =====================================================================================

    /** A dome of light over 'center' (on the ground), 'radius' blocks, rising row by row; every tile gets 'tag'. */
    public static void dome(ServerLevel sl, Vec3 center, double radius, int color, int ticks, String tag) {
        shell(sl, center, radius, color, ticks, tag, new double[]{0, 28, 55, 80}, 3);
        cap(sl, center.add(0, radius * Math.sin(Math.toRadians(80)), 0), radius * Math.cos(Math.toRadians(80)) * 2.2,
                color, ticks, tag, 10);
    }

    /** A whole sphere of light round 'center' (its middle), closing from the equator to both poles. */
    public static void sphere(ServerLevel sl, Vec3 center, double radius, int color, int ticks, String tag) {
        shell(sl, center, radius, color, ticks, tag, new double[]{-80, -55, -28, 0, 28, 55, 80}, 3);
        double cap = radius * Math.cos(Math.toRadians(80)) * 2.2;
        double up = radius * Math.sin(Math.toRadians(80));
        cap(sl, center.add(0, up, 0), cap, color, ticks, tag, 10);
        cap(sl, center.add(0, -up, 0), cap, color, ticks, tag, 10);
    }

    private static void shell(ServerLevel sl, Vec3 center, double radius, int color, int ticks, String tag,
                              double[] edges, int rowDelay) {
        float spin = sl.getRandom().nextFloat() * 360f;
        int equator = 0;
        for (int i = 0; i < edges.length; i++) if (edges[i] == 0) equator = i;
        for (int row = 0; row < edges.length - 1; row++) {
            double lo = Math.toRadians(edges[row]), hi = Math.toRadians(edges[row + 1]);
            double mid = (lo + hi) / 2;
            double wide = radius * Math.cos(Math.min(Math.abs(lo), Math.abs(hi)));
            int count = Math.max(6, Math.min(22, (int) Math.round(2 * Math.PI * radius * Math.cos(mid) / 1.8)));
            float width = (float) (2 * wide * Math.sin(Math.PI / count) * 1.08);
            float height = (float) (2 * radius * Math.sin((hi - lo) / 2) * 1.04);
            int delay = Math.abs(row - (row < equator ? equator - 1 : equator)) * rowDelay;
            for (int k = 0; k < count; k++) {
                float azimuth = spin + k * 360f / count;
                Vec3 out = ahead(azimuth);
                Vec3 at = center.add(out.scale(radius * Math.cos(lo))).add(0, radius * Math.sin(lo), 0);
                ModelFx tile = DAWN_WALL.color(color).scale(width, height, 1f).delay(delay).lifetime(ticks)
                        .fade(4, 10).key(0, pose().alpha(0f)).key(6, pose().alpha(1f), Ease.OUT);
                if (tag != null) tile = tile.tag(tag);
                ParticleShapes.model(sl, tile, at, azimuth + 180f, (float) Math.toDegrees(mid), 0f);
            }
        }
    }

    private static void cap(ServerLevel sl, Vec3 at, double width, int color, int ticks, String tag, int delay) {
        ModelFx top = DAWN_SEAL_CORE.color(color).scale((float) width).delay(delay).lifetime(ticks).fade(4, 10).spin(1f);
        if (tag != null) top = top.tag(tag);
        ParticleShapes.model(sl, top, at, 0f, 0f, 0f);
    }

    // =====================================================================================
    // Key 1: Warrior's Charge (Warrior) / Divine Blood
    // =====================================================================================

    private static final SlashFx RUSH_LINE = SlashFx.line("slash/streaks")
            .color(0xA0FFE0A0).core(0x80FFFFFF).width(0.25f).taper(SlashFx.Taper.COMET);

    /** The warrior lowers a shoulder: dust kicks up behind them and the ground cracks under the first step. */
    public static void chargeStart(ServerLevel sl, Player player) {
        Vec3 back = player.position().subtract(Spells.flatLook(player).scale(0.6));
        ParticleShapes.cone(sl, DUST, back.add(0, 0.2, 0), Spells.flatLook(player).scale(-1).add(0, 0.3, 0), 60, 14, 0.05, 0.2);
        SpellFx.shockRing(sl, player.position(), 1.8, 0xA0C8B080, 8, 0);
        sound(sl, player.position(), ModSounds.HAND_CHARGE, 1.0f, wobble(1.0f));
    }

    /** Every tick of the rush: streaks of air past the body and dust from the feet. */
    public static void chargeTrail(ServerLevel sl, Player player) {
        Vec3 look = Spells.flatLook(player);
        Vec3 chest = chestOf(player);
        ParticleShapes.slashBetween(sl, RUSH_LINE, chest.subtract(look.scale(1.8)), chest.add(look.scale(0.2)));
        ParticleShapes.spawn(sl, DUST, player.position().add(0, 0.1, 0), look.scale(-0.08).add(0, 0.03, 0));
    }

    /** The first one hit: a flash of the blow, a ring of dust and the sound of a body thrown back. */
    public static void chargeImpact(ServerLevel sl, Player player, LivingEntity target) {
        Vec3 at = chestOf(target);
        flash(sl, at, player.getYRot() + 180f, 0xFFFFE8B0, 1.6f);
        ParticleShapes.burst(sl, STEEL_SPARK, at, 14, 0.1, 0.35);
        ParticleShapes.ring(sl, DUST, target.position().add(0, 0.2, 0), 1.2, 12, 0.12);
        SpellFx.shockRing(sl, target.position(), 3.0, 0xC0FFE0A0, 10, 0);
        sound(sl, at, ModSounds.HAND_IMPACT, 1.0f, wobble(0.9f));
    }

    // ---------- sneak: Divine Blood

    /** The blood surges: a red-gold glow rises round the body, a ring under the feet beats like a heart. */
    public static void divineBlood(ServerLevel sl, Player player, int ticks, String tag) {
        SpellFx.circleUnder(sl, player, 1.0, 0xC0D02828, ticks, tag);
        SpellFx.risingRings(sl, player.position(), 1.0, 2.2, 4, 0xC0FF5040);
        ParticleShapes.spiral(sl, BLOOD_MOTE, player.position().add(0, 0.1, 0), 3, 30, 0.3, 1.2, 1.0, 0.06);
        ParticleShapes.burst(sl, DAWN_MOTE, chestOf(player), 12, 0.04, 0.12);
        for (int t = 10; t < Math.min(ticks, 200); t += 20) {
            Later.run(sl, t, () -> {
                if (!player.isAlive()) return;
                SpellFx.shockRing(sl, player.position(), 1.6, 0x80FF4040, 8, 0);
                ParticleShapes.burst(sl, BLOOD_MOTE, chestOf(player), 4, 0.02, 0.06);
            });
        }
        sound(sl, player.position(), ModSounds.HAND_DIVINE_BLOOD, 1.0f, 1.0f);
    }

    public static void divineBloodEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
    }

    // =====================================================================================
    // Key 2: Gladiator's Flurry (Pugilist) / Supernatural Resistance
    // =====================================================================================

    private static final SlashFx BLOW_ARC = SlashFx.arc("slash/streaks")
            .color(0xC0FFE8B0).core(0xFFFFFFFF)
            .radius(0.9f).arc(70f).width(0.35f).layers(2).spread(0.1f)
            .lifetime(6).sweep(2);

    /** One of the five blows (index 0..4): a short arc and a flash on the target; the last is twice the size. */
    public static void flurryBlow(ServerLevel sl, Player player, LivingEntity target, int index) {
        boolean last = index >= 4;
        Vec3 at = chestOf(target);
        float yaw = player.getYRot();
        float roll = (index % 2 == 0 ? -1 : 1) * (20f + index * 12f);
        ParticleShapes.slash(sl, last ? BLOW_ARC.radius(1.5f).width(0.6f) : BLOW_ARC,
                chestOf(player).add(Spells.flatLook(player).scale(0.6)), yaw, 0f, roll);
        flash(sl, at, yaw + 180f, 0xFFFFF0C8, last ? 1.4f : 0.7f);
        ParticleShapes.burst(sl, STEEL_SPARK, at, last ? 12 : 5, 0.08, last ? 0.3 : 0.18);
        if (last) {
            SpellFx.shockRing(sl, target.position(), 2.2, 0xC0FFE0A0, 8, 0);
            sound(sl, at, ModSounds.HAND_IMPACT, 1.0f, wobble(1.0f));
        } else if (index == 0) {
            sound(sl, at, ModSounds.HAND_FLURRY, 1.0f, wobble(1.0f));
        }
    }

    // ---------- sneak: Supernatural Resistance

    /** A steel-grey seal turns under the warrior's feet and runes circle the body while the resistance holds. */
    public static void resistance(ServerLevel sl, Player player, int ticks, String tag) {
        SpellFx.circleUnder(sl, player, 1.2, 0xC0B8C0D0, ticks, tag);
        SpellFx.risingRings(sl, player.position(), 1.1, 2.0, 3, 0xA0C8D0E0);
        ParticleShapes.ring(sl, SILVER_MOTE, player.position().add(0, 1.0, 0), 0.9, 16, 0);
        sound(sl, player.position(), ModSounds.HAND_RESIST, 1.0f, 1.0f);
    }

    /** Something supernatural was shaken off (an effect cleansed, a root broken): a grey flash off the body. */
    public static void resistanceShrug(ServerLevel sl, Player player) {
        ParticleShapes.burst(sl, SILVER_MOTE, chestOf(player), 10, 0.06, 0.18);
        SpellFx.shockRing(sl, player.position(), 1.4, 0xA0C8D0E0, 6, 0);
        sound(sl, player.position(), ModSounds.HAND_PARRY, 0.4f, wobble(1.5f));
    }

    public static void resistanceEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
    }

    // =====================================================================================
    // Key 3: Grandmaster's Edge (Weapon Master) / Riposte
    // =====================================================================================

    private static final SlashFx EDGE_ARC = SlashFx.arc("slash/streaks")
            .color(0xC0E8F0FF).core(0xFFFFFFFF)
            .radius(2.6f).arc(140f).width(0.5f).layers(2).spread(0.15f)
            .lifetime(8).sweep(3);
    private static final SlashFx FINISHER_ARC = SlashFx.arc("slash/streaks")
            .color(0xE0FFD86A).core(0xFFFFFFFF)
            .radius(3.4f).arc(170f).width(1.1f).layers(3).spread(0.25f)
            .lifetime(10).sweep(3);

    /** The weapon becomes a grandmaster's: a glint runs along the hand and a white ring flares. */
    public static void grandmastersEdge(ServerLevel sl, Player player, int ticks) {
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand == null) hand = chestOf(player);
        ParticleShapes.burst(sl, STEEL_SPARK, hand, 10, 0.04, 0.15);
        SpellFx.shockRing(sl, player.position(), 2.0, 0xA0F0F4FF, 8, 0);
        sound(sl, player.position(), ModSounds.HAND_EDGE_SWING, 0.8f, 1.3f);
    }

    /** While it lasts, every few ticks: a glint on the hand. */
    public static void edgeTick(ServerLevel sl, Player player) {
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand != null) ParticleShapes.spawn(sl, STEEL_SPARK, hand, new Vec3(0, 0.02, 0));
    }

    /** A swing (index 0, 1, ... alternate left and right): a wide arc that cuts everything in front. */
    public static void edgeSwing(ServerLevel sl, Player player, int index) {
        Vec3 center = chestOf(player).add(Spells.flatLook(player).scale(0.4));
        float roll = index % 2 == 0 ? -12f : 168f;
        ParticleShapes.slash(sl, EDGE_ARC, center, player.getYRot(), 0f, roll);
        sound(sl, player.position(), ModSounds.HAND_EDGE_SWING, 0.9f, wobble(1.0f));
    }

    /** The third swing in a row: a broad golden arc, a flash on the one it lands on. */
    public static void edgeFinisher(ServerLevel sl, Player player, LivingEntity target) {
        Vec3 center = chestOf(player).add(Spells.flatLook(player).scale(0.4));
        ParticleShapes.slash(sl, FINISHER_ARC, center, player.getYRot(), 0f, -25f);
        if (target != null) {
            flash(sl, chestOf(target), player.getYRot() + 180f, 0xFFFFE8A0, 1.8f);
            ParticleShapes.burst(sl, DAWN_STAR, chestOf(target), 14, 0.08, 0.25);
        }
        sound(sl, player.position(), ModSounds.HAND_FINISHER, 1.0f, wobble(1.0f));
    }

    // ---------- sneak: Riposte

    /** The parrying stance: a pale ring of steel stands in front of the warrior for a moment. */
    public static void riposte(ServerLevel sl, Player player) {
        ModelFx ring = PARRY_FLASH.color(0x80E8F0FF).scale(1.6f).lifetime(30).fade(2, 8).spin(3f)
                .key(0, pose().forward(0.8f).scale(0.4f).alpha(0f))
                .key(4, pose().scale(1f).alpha(1f), Ease.OUT_BACK)
                .key(30, pose().scale(1.1f).alpha(0.4f));
        ParticleShapes.modelOnTurning(sl, ring, player, new Vec3(0, 1.2, 0), 0f, 0f, 0f);
        sound(sl, player.position(), ModSounds.HAND_EDGE_SWING, 0.6f, 1.4f);
    }

    /** A blow was turned aside: sparks fly at the point of contact and the attacker reels. */
    public static void riposteParry(ServerLevel sl, Player player, Entity attacker) {
        Vec3 between = attacker == null ? chestOf(player).add(Spells.flatLook(player).scale(0.8))
                : chestOf(player).add(chestOf(attacker).subtract(chestOf(player)).normalize().scale(0.8));
        flash(sl, between, player.getYRot() + 180f, 0xFFFFFFFF, 1.4f);
        ParticleShapes.burst(sl, STEEL_SPARK, between, 18, 0.1, 0.35);
        sound(sl, between, ModSounds.HAND_PARRY, 1.0f, wobble(1.0f));
    }

    /** The counter: a short golden thrust-line into the target and a flash on it. */
    public static void riposteCounter(ServerLevel sl, Player player, LivingEntity target) {
        ParticleShapes.slashBetween(sl, RUSH_LINE.color(0xE0FFD86A).width(0.4f), chestOf(player), chestOf(target));
        flash(sl, chestOf(target), player.getYRot() + 180f, 0xFFFFD86A, 1.6f);
        ParticleShapes.burst(sl, DAWN_STAR, chestOf(target), 10, 0.06, 0.2);
        sound(sl, chestOf(target), ModSounds.HAND_FINISHER, 0.8f, wobble(1.2f));
    }

    // =====================================================================================
    // Key 4: the Dawn wheel (Dawn Paladin)
    // =====================================================================================

    /** The tick Light of Dawn is at its brightest: reveal, burn and heal from here on. */
    public static final int LIGHT_RISEN = 12;

    /** Dawn breaks round the caster: a sun climbs behind them, a seal spreads 'radius' wide and rays shoot up. */
    public static void lightOfDawn(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        seal(sl, center, radius, 0xC0FFD86A, ticks, tag);
        ModelFx sunrise = LIGHT_GATE.color(0xE0FFE8A0).scale(3.2f).lifetime(Math.min(ticks, 60)).fade(0, 20).spin(0.8f)
                .key(0, pose().up(0.5f).scale(0.3f).alpha(0f))
                .key(LIGHT_RISEN, pose().up(4.5f).scale(1f).alpha(1f), Ease.OUT);
        if (tag != null) sunrise = sunrise.tag(tag);
        float yaw = sl.getRandom().nextFloat() * 360f;
        ParticleShapes.model(sl, sunrise, center, yaw, 0f, 0f);
        ParticleShapes.model(sl, sunrise, center, yaw + 180f, 0f, 0f);
        SpellFx.steadyRing(sl, center, radius, 0xD0FFF0B0, LIGHT_RISEN, 0);
        SpellFx.risingRings(sl, center, 1.5, 4.0, 4, 0xC0FFE8A0);
        Later.run(sl, LIGHT_RISEN, () -> {
            ParticleShapes.cone(sl, DAWN_RAY, center.add(0, 0.3, 0), Spells.UP, 160, 40, 0.15, 0.5);
            ParticleShapes.burst(sl, DAWN_STAR, center.add(0, 1.5, 0), 24, 0.05, 0.25);
        });
        sound(sl, center, ModSounds.HAND_DAWN_LIGHT, 1.2f, 1.0f);
    }

    /** Every second while the dawn lasts: rays rise all over the circle. */
    public static void lightOfDawnPulse(ServerLevel sl, Vec3 center, double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            Vec3 at = center.add(Math.cos(angle) * far, 0.2, Math.sin(angle) * far);
            ParticleShapes.spawn(sl, DAWN_RAY, at, new Vec3(0, 0.12 + random.nextDouble() * 0.1, 0));
            if (i % 3 == 0) ParticleShapes.spawn(sl, DAWN_MOTE, at.add(0, 0.5, 0), new Vec3(0, 0.03, 0));
        }
    }

    /** Evil or undead in the light: it smoulders with white fire. */
    public static void lightOfDawnBurn(ServerLevel sl, LivingEntity evil) {
        ParticleShapes.burst(sl, DAWN_RAY, chestOf(evil), 6, 0.04, 0.12);
        ParticleShapes.burst(sl, ASH.color(0xE0F0E8D0), chestOf(evil), 4, 0.02, 0.06);
        play(sl, evil.position(), SoundEvents.FIRE_EXTINGUISH, 0.35f, wobble(1.6f));
    }

    /** Someone hidden (or an illusion) is torn out of hiding: a flash and a ring of light round them. */
    public static void lightOfDawnReveal(ServerLevel sl, LivingEntity hidden) {
        flash(sl, chestOf(hidden), sl.getRandom().nextFloat() * 360f, 0xFFFFF6D8, 1.6f);
        SpellFx.closingRing(sl, hidden.position(), 2.0, 0xE0FFE8A0, 8, 0);
        ParticleShapes.burst(sl, DAWN_STAR, chestOf(hidden), 16, 0.06, 0.2);
        play(sl, hidden.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.6f);
    }

    /** An ally the dawn cleanses and heals: gold motes rise round them. */
    public static void lightOfDawnHeal(ServerLevel sl, LivingEntity ally) {
        ParticleShapes.ring(sl, DAWN_MOTE, ally.position().add(0, 0.2, 0), Math.max(0.4, ally.getBbWidth() * 0.6), 8, 0);
    }

    public static void lightOfDawnEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.burst(sl, DAWN_MOTE, center.add(0, 1, 0), 12, 0.03, 0.1);
    }

    // ---------- Dawn Armor (Silver Armor from Sequence 3)

    /** Plates of light (or of silver) lock onto the body and a helm closes over the head, for 'ticks'. */
    public static void dawnArmor(ServerLevel sl, Player player, int ticks, String tag, boolean silver) {
        ModelFx body = (silver ? SILVER_ARMOR : DAWN_ARMOR).lifetime(ticks).fade(6, 10).tag(tag)
                .key(0, pose().scale(1.4f).alpha(0f))
                .key(8, pose().scale(1f).alpha(1f), Ease.OUT);
        ModelFx helm = (silver ? SILVER_HELM : DAWN_HELM).lifetime(ticks).fade(6, 10).tag(tag)
                .key(0, pose().up(0.6f).alpha(0f))
                .key(10, pose().up(0f).alpha(1f), Ease.OUT_BOUNCE);
        ParticleShapes.modelOnTurning(sl, body, player, Vec3.ZERO, 0f, 0f, 0f);
        ParticleShapes.modelOnHead(sl, helm, player, 0, 0f, 0f, 0f);
        SpellFx.closingRing(sl, player.position(), 2.2, silver ? 0xC0D8DEEA : 0xC0FFD86A, 8, 0);
        ParticleShapes.burst(sl, silver ? SILVER_MOTE : DAWN_MOTE, chestOf(player), 16, 0.03, 0.12);
        sound(sl, player.position(), ModSounds.HAND_DAWN_ARMOR, 1.0f, silver ? 0.85f : 1.0f);
    }

    /** A blow glances off the armor. */
    public static void armorHit(ServerLevel sl, Player player, boolean silver) {
        ParticleShapes.burst(sl, silver ? STEEL_SPARK : DAWN_STAR, chestOf(player), 8, 0.06, 0.2);
        play(sl, player.position(), SoundEvents.ANVIL_LAND, 0.25f, wobble(1.8f));
    }

    public static void dawnArmorEnd(ServerLevel sl, Player player, String tag, boolean silver) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.burst(sl, silver ? SILVER_MOTE : DAWN_MOTE, chestOf(player), 20, 0.04, 0.15);
    }

    // ---------- Sword of Dawn

    /**
     * The greatsword forms in the right hand: it is drawn out of light point-first, then held at the ready beside the
     * body (it turns with the body, not with the arm), for 'ticks'. Show each swing with dawnSlash.
     */
    public static void swordOfDawn(ServerLevel sl, Player player, int ticks, String tag) {
        ModelFx held = DAWN_SWORD.scale(0.85f).lifetime(ticks).fade(4, 10).tag(tag)
                .key(0, pose().right(0.42f).forward(0.25f).pitch(25f).scale(0.1f).alpha(0f))
                .key(12, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOnTurning(sl, held, player, new Vec3(0, 0.7, 0), 0f, 0f, 0f);
        Vec3 hand = player.position().add(0, 0.9, 0).add(Spells.rightOf(player).scale(0.42));
        ParticleShapes.line(sl, DAWN_STAR, hand, hand.add(0, 2.2, 0), 4);
        ParticleShapes.burst(sl, DAWN_RAY, hand.add(0, 1.0, 0), 12, 0.04, 0.12);
        sound(sl, player.position(), ModSounds.HAND_DAWN_SWORD, 1.0f, 1.0f);
    }

    private static final SlashFx DAWN_ARC = SlashFx.arc("slash/streaks")
            .color(0xD0FFD86A).core(0xFFFFFFFF)
            .radius(3.0f).arc(150f).width(0.8f).layers(3).spread(0.2f)
            .lifetime(9).sweep(3);
    private static final SlashFx CRESCENT = SlashFx.arc("slash/band")
            .color(0xC0FFE8A0).core(0xFFFFFFFF)
            .radius(1.4f).arc(120f).width(0.4f).lifetime(10).sweep(1);

    /** A swing of the Sword of Dawn: a golden arc; crescent = also a crescent of light flying 6 blocks ahead. */
    public static void dawnSlash(ServerLevel sl, Player player, boolean crescent) {
        Vec3 center = chestOf(player).add(Spells.flatLook(player).scale(0.4));
        float yaw = player.getYRot();
        ParticleShapes.slash(sl, DAWN_ARC, center, yaw, 0f, -15f);
        if (crescent) {
            Vec3 look = Spells.flatLook(player);
            for (int i = 1; i <= 6; i++) {
                int step = i;
                Later.run(sl, step, () -> ParticleShapes.slash(sl, CRESCENT, center.add(look.scale(1.0 + step)), yaw, 0f, -15f));
            }
        }
        sound(sl, player.position(), ModSounds.HAND_EDGE_SWING, 1.0f, wobble(0.9f));
    }

    /** A hit of the Sword of Dawn purifies: white light and ash flake off the wound. */
    public static void purify(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, DAWN_STAR, chestOf(target), 8, 0.05, 0.15);
        ParticleShapes.burst(sl, DAWN_RAY, chestOf(target), 4, 0.04, 0.1);
        sound(sl, chestOf(target), ModSounds.HAND_COAT_HIT, 0.6f, wobble(1.3f));
    }

    public static void swordOfDawnEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        Vec3 hand = player.position().add(0, 0.9, 0).add(Spells.rightOf(player).scale(0.42));
        ParticleShapes.line(sl, DAWN_MOTE, hand, hand.add(0, 2.0, 0), 4);
    }

    // ---------- Hurricane of Light

    /** The tick the hurricane is released, after the light gathers. */
    public static final int HURRICANE_GATHERS = 20;

    /** Rays of light gather into the raised sword. */
    public static void hurricaneGather(ServerLevel sl, Player player) {
        Vec3 tip = player.position().add(0, 3.2, 0).add(Spells.rightOf(player).scale(0.42));
        ModelFx raised = DAWN_SWORD.scale(0.9f).lifetime(HURRICANE_GATHERS + 4).fade(3, 4)
                .key(0, pose().right(0.42f).alpha(0f))
                .key(5, pose().alpha(1f))
                .during(4, HURRICANE_GATHERS, pose().spin(360f), Ease.IN);
        ParticleShapes.modelOnTurning(sl, raised, player, new Vec3(0, 1.4, 0), 0f, 0f, 0f);
        for (int t = 0; t < HURRICANE_GATHERS; t += 2) {
            Later.run(sl, t, () -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int i = 0; i < 4; i++) {
                    Vec3 from = tip.add(random.nextDouble() * 6 - 3, random.nextDouble() * 3 - 1, random.nextDouble() * 6 - 3);
                    ParticleShapes.spawn(sl, DAWN_RAY, from, tip.subtract(from).scale(0.12));
                }
            });
        }
        SpellFx.closingRing(sl, player.position(), 5.0, 0xC0FFE8A0, HURRICANE_GATHERS, 0);
        sound(sl, player.position(), ModSounds.HAND_HURRICANE, 1.4f, 1.0f);
    }

    /**
     * The hurricane: a spinning funnel of light rolling 'length' blocks from 'ground' the way 'yaw' looks, in 'ticks'.
     * glory = the Sequence 2 form, twice as big and orange. Returns how long it is on screen.
     */
    public static int hurricane(ServerLevel sl, Vec3 ground, float yaw, double length, int ticks, boolean glory) {
        float scale = glory ? 3.2f : 1.6f;
        int color = glory ? 0xE0FF8A3A : 0xE0FFE8A0;
        int life = ticks + 10;
        ModelFx outer = HURRICANE.color(color).scale(scale).lifetime(life).fade(4, 10).spin(28f)
                .key(0, pose().scale(0.3f).alpha(0f))
                .key(5, pose().scale(1f).alpha(1f), Ease.OUT)
                .during(0, ticks, pose().forward((float) length));
        ModelFx inner = HURRICANE.color(0xC0FFFFFF).scale(scale * 0.65f).lifetime(life).fade(4, 10).spin(-40f)
                .key(0, pose().scale(0.3f).alpha(0f))
                .key(5, pose().scale(1f).alpha(1f), Ease.OUT)
                .during(0, ticks, pose().forward((float) length));
        ParticleShapes.model(sl, outer, ground, yaw, 0f, 0f);
        ParticleShapes.model(sl, inner, ground, yaw, 0f, 0f);
        Vec3 way = ahead(yaw);
        ParticleFx debris = glory ? GLOWING_ASH : DAWN_RAY;
        for (int t = 0; t < ticks; t += 2) {
            double along = length * t / ticks;
            Vec3 at = ground.add(way.scale(along));
            Later.run(sl, t, () -> {
                ParticleShapes.spiral(sl, debris, at.add(0, 0.3, 0), 3, 12, 0.5, 2.0 * scale / 1.6, 0.6, 0.3);
                ParticleShapes.spawn(sl, DUST, at.add(0, 0.2, 0), new Vec3(0, 0.04, 0));
            });
        }
        return life;
    }

    /** Someone the hurricane reaches: torn by light (and decayed, in the Glory form). */
    public static void hurricaneHit(ServerLevel sl, LivingEntity entity, boolean glory) {
        ParticleShapes.burst(sl, glory ? GLORY_EMBER : DAWN_STAR, chestOf(entity), 12, 0.1, 0.3);
        if (glory) ParticleShapes.burst(sl, ASH, chestOf(entity), 8, 0.05, 0.15);
    }

    // =====================================================================================
    // Key 5: Protection (Guardian) / Damage Transference; the Cage (Glory)
    // =====================================================================================

    /** The tick the dome is whole. */
    public static final int PROTECTION_RAISED = 16;
    /** The tick the cage is closed. */
    public static final int CAGE_CLOSED = 14;

    /**
     * The sword is driven into the ground at 'center' and a dome of Dawn light blooms out of it, 'radius' wide,
     * for 'ticks'. Every model gets 'tag' (end it early with protectionEnd).
     */
    public static void protection(ServerLevel sl, Player player, Vec3 center, double radius, int ticks, String tag) {
        ModelFx planted = DAWN_SWORD.scale(0.9f).lifetime(ticks).fade(0, 10).tag(tag)
                .key(0, pose().pitch(180f).up(-3.2f).alpha(0f))
                .key(4, pose().up(-2.2f).alpha(1f), Ease.IN);
        ParticleShapes.model(sl, planted, center.add(0, 0.1, 0), player.getYRot(), 0f, 0f);
        seal(sl, center, Math.max(1.5, radius * 0.6), 0xC0FFE8A0, ticks, tag);
        Later.run(sl, 4, () -> {
            SpellFx.shockRing(sl, center, radius, 0xE0FFF0B0, 10, 0);
            ParticleShapes.ring(sl, DUST, center.add(0, 0.2, 0), 0.8, 14, 0.15);
            dome(sl, center, radius, 0x90FFE8A0, ticks - 4, tag);
        });
        sound(sl, center, ModSounds.HAND_PROTECTION, 1.2f, 1.0f);
    }

    /** Something strikes the dome (a projectile stopped, an enemy kept out): a ripple of light there. */
    public static void domeHit(ServerLevel sl, Vec3 at) {
        flash(sl, at, sl.getRandom().nextFloat() * 360f, 0xC0FFE8A0, 1.2f);
        ParticleShapes.burst(sl, DAWN_STAR, at, 8, 0.03, 0.12);
        sound(sl, at, ModSounds.HAND_DOME_HIT, 0.9f, wobble(1.0f));
    }

    /** The guardian attacks from inside: the dome dims and flickers for a moment. */
    public static void domeWeaken(ServerLevel sl, Vec3 center, double radius) {
        ParticleShapes.ring(sl, DAWN_MOTE.color(0x80FFE8A0), center.add(0, radius * 0.5, 0), radius * 0.9, 20, -0.02);
        play(sl, center, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 0.6f);
    }

    public static void protectionEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.burst(sl, DAWN_MOTE, center.add(0, 1.5, 0), 30, 0.05, 0.2);
    }

    /** Glory's Cage: liquid light closes into a whole sphere round 'center' (its middle), orange-gold. */
    public static void cage(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        SpellFx.closingRing(sl, center.add(0, -radius, 0), radius * 2, 0xE0FF8A3A, CAGE_CLOSED, 0);
        sphere(sl, center, radius, 0xA0FF9A4A, ticks, tag);
        Later.run(sl, CAGE_CLOSED, () -> ParticleShapes.sphere(sl, GLORY_EMBER, center, radius, 40));
        sound(sl, center, ModSounds.HAND_CAGE, 1.2f, 1.0f);
    }

    public static void cageEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.burst(sl, GLORY_EMBER, center, 30, 0.05, 0.2);
    }

    // ---------- sneak: Damage Transference

    private static final SlashFx AEGIS_LINE = SlashFx.line("slash/smooth")
            .color(0xA0FFD86A).core(0x80FFFFFF).width(0.06f).taper(SlashFx.Taper.UNIFORM);

    /** A golden thread runs from each ally to the guardian and a shield shows over their heads, for 'ticks'. */
    public static void transference(ServerLevel sl, Player player, List<? extends LivingEntity> allies, int ticks, String tag) {
        SpellFx.circleUnder(sl, player, 1.4, 0xC0FFC060, ticks, tag);
        for (LivingEntity ally : allies) {
            SpellFx.tether(sl, AEGIS_LINE, ally, player, ticks);
            ModelFx mark = AEGIS_MARK.color(0xE0FFD86A).scale(0.5f).lifetime(ticks).fade(4, 10).spin(2f).tag(tag)
                    .key(0, pose().scale(0.2f).alpha(0f))
                    .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
            ParticleShapes.modelOn(sl, mark, ally, new Vec3(0, ally.getBbHeight() + 0.5, 0), 0f, 0f, 0f);
        }
        sound(sl, player.position(), ModSounds.HAND_TRANSFERENCE, 1.0f, 1.0f);
    }

    /** A wound passes from an ally to the guardian: red light runs down the thread. */
    public static void transferenceTaken(ServerLevel sl, LivingEntity ally, LivingEntity guardian) {
        Vec3 from = chestOf(ally), to = chestOf(guardian);
        for (int i = 0; i < 6; i++) {
            Vec3 at = from.add(to.subtract(from).scale(i / 6.0));
            Later.run(sl, i, () -> ParticleShapes.spawn(sl, BLOOD_MOTE, at, Vec3.ZERO));
        }
        Later.run(sl, 6, () -> ParticleShapes.burst(sl, BLOOD_MOTE, to, 6, 0.03, 0.08));
    }

    public static void transferenceEnd(ServerLevel sl, Player player, List<? extends LivingEntity> allies, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        for (LivingEntity ally : allies) ParticleShapes.clearModels(sl, ally, tag);
    }

    // =====================================================================================
    // Key 6: the Demon Hunting wheel (Demon Hunter)
    // =====================================================================================

    /** The tick the eye is open: show what it sees from here on. */
    public static final int EYE_OPENS = 6;

    /** Dark green symbols fill the hunter's eyes (others see a sigil before their face) and a ring sweeps out. */
    public static void eye(ServerLevel sl, Player player, int ticks, String tag) {
        ModelFx sigil = EYE_SYMBOL.color(0xE03AD06A).scale(0.45f).lifetime(ticks).fade(0, 8).spin(1.5f).tag(tag)
                .key(0, pose().forward(0.45f).scale(0.1f).alpha(0f))
                .key(EYE_OPENS, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOnHead(sl, sigil, player, -0.3, 0f, 0f, 0f);
        SpellFx.steadyRing(sl, player.position(), 16, 0xA03AD06A, 14, EYE_OPENS);
        for (ParticleFx rune : RUNES) ParticleShapes.ring(sl, rune, player.getEyePosition(), 0.7, 4, 0.04);
        sound(sl, player.position(), ModSounds.HAND_EYE, 1.0f, 1.0f);
    }

    /** The one the hunter looks at: its weakness is found. An eye watches over it. */
    public static void eyeMark(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        ModelFx watched = EYE_SYMBOL.color(0xE03AD06A).scale(0.8f).lifetime(ticks).fade(4, 10).tag(tag)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.modelOn(sl, watched, target, new Vec3(0, target.getBbHeight() + 0.7, 0), 0f, 0f, 0f);
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth() * 0.8), 0xC03AD06A, ticks, tag);
        ParticleShapes.burst(sl, RUNES[1], chestOf(target), 6, 0.02, 0.06);
    }

    /** Evil, undead or corrupted things the eye picks out: a green ring under them. */
    public static void eyeEvil(ServerLevel sl, LivingEntity evil, int ticks) {
        SpellFx.circleUnder(sl, evil, Math.max(0.6, evil.getBbWidth() * 0.8), 0xA020A040, ticks, "hg_eye_evil");
    }

    public static void eyeEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
    }

    // ---------- Alchemy

    /** The tick the coating is on the weapon. */
    public static final int VIAL_POURED = 24;

    /** A vial appears, is shaken and poured along the weapon: the coating's color glows on the hand. */
    public static void alchemy(ServerLevel sl, Player player, Coat coat) {
        ModelFx glass = VIAL.scale(0.6f).lifetime(VIAL_POURED + 4).fade(3, 4)
                .key(0, pose().forward(0.55f).right(0.3f).alpha(0f))
                .key(4, pose().alpha(1f))
                .during(4, 16, pose().roll(30f), Ease.IN_OUT)
                .during(16, VIAL_POURED, pose().roll(-120f), Ease.IN);
        ModelFx liquid = VIAL_LIQUID.color(coat.color).scale(0.6f).lifetime(VIAL_POURED + 4).fade(3, 4)
                .key(0, pose().forward(0.55f).right(0.3f).alpha(0f))
                .key(4, pose().alpha(1f))
                .during(4, 16, pose().roll(30f), Ease.IN_OUT)
                .during(16, VIAL_POURED, pose().roll(-120f).alpha(0f), Ease.IN);
        ParticleShapes.modelOnTurning(sl, glass, player, new Vec3(0, 1.25, 0), 0f, 0f, 0f);
        ParticleShapes.modelOnTurning(sl, liquid, player, new Vec3(0, 1.25, 0), 0f, 0f, 0f);
        ParticleFx drip = ParticleFx.of("fx/droplet").color(coat.color).endColor(withAlpha(coat.color, 0x80))
                .size(0.05f).endSize(0.04f).lifetime(16, 6).gravity(0.06f).friction(0.98f).glow();
        Later.run(sl, VIAL_POURED - 6, () -> {
            Vec3 hand = SpellFx.handOf(player, sl);
            if (hand != null) ParticleShapes.burst(sl, drip, hand, 10, 0.02, 0.08);
        });
        Later.run(sl, VIAL_POURED, () -> coatTick(sl, player, coat));
        sound(sl, player.position(), ModSounds.HAND_ALCHEMY, 1.0f, 1.0f);
    }

    /** Every few ticks while the weapon is coated: a glow of the coating's color on the hand. */
    public static void coatTick(ServerLevel sl, Player player, Coat coat) {
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand == null) return;
        ParticleFx glow = ParticleFx.of("fx/glow").color(coat.color).endColor(withAlpha(coat.color, 0))
                .size(0.08f).endSize(0.02f).lifetime(12, 6).gravity(-0.01f).friction(0.9f).glow();
        ParticleShapes.burst(sl, glow, hand, 2, 0.005, 0.02);
    }

    /** A coated blow lands: the coating flares on the target in its own way. */
    public static void coatHit(ServerLevel sl, LivingEntity target, Coat coat) {
        Vec3 at = chestOf(target);
        ParticleFx glow = ParticleFx.of("fx/glow").color(coat.color).endColor(withAlpha(coat.color, 0))
                .size(0.1f).endSize(0.02f).lifetime(14, 6).gravity(-0.02f).friction(0.9f).glow();
        ParticleShapes.burst(sl, glow, at, 12, 0.06, 0.2);
        switch (coat) {
            case PURIFICATION -> ParticleShapes.burst(sl, DAWN_RAY, at, 6, 0.05, 0.15);
            case FREEZE -> play(sl, at, SoundEvents.GLASS_BREAK, 0.5f, 1.6f);
            case LIGHTNING -> {
                ParticleShapes.burst(sl, STEEL_SPARK.color(0xFFE0D8FF), at, 14, 0.15, 0.4);
                play(sl, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.4f, 1.6f);
            }
            case BURNING -> ParticleShapes.burst(sl, SpellFx.FIRE_EMBER, at, 10, 0.05, 0.15);
            case DECAY -> ParticleShapes.burst(sl, ASH, at, 10, 0.03, 0.1);
            case EXORCISM -> SpellFx.shockRing(sl, target.position(), 1.6, withAlpha(coat.color, 0xC0), 8, 0);
        }
        sound(sl, at, ModSounds.HAND_COAT_HIT, 0.9f, wobble(1.0f));
    }

    // ---------- Mind Concealment

    /** The hunter's mind closes: green mist curls in round the head and the eye-sigil shuts. */
    public static void mindConcealment(ServerLevel sl, Player player, int ticks, String tag) {
        ParticleShapes.ring(sl, MIND_MIST, player.getEyePosition(), 0.9, 12, -0.03);
        SpellFx.closingRing(sl, player.position(), 3.0, 0x804A8A5A, 12, 0);
        SpellFx.circleUnder(sl, player, 0.9, 0x60406A50, ticks, tag);
        sound(sl, player.position(), ModSounds.HAND_CONCEAL, 1.0f, 1.0f);
    }

    public static void mindConcealmentEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.burst(sl, MIND_MIST, player.getEyePosition(), 6, 0.02, 0.06);
    }

    // ---------- Giant Form

    /** The tick the Giant has grown to full size. */
    public static final int GIANT_RISEN = 30;

    /**
     * The one-eyed Giant: its body grows round the caster to 'height' blocks and stays for 'ticks'. It turns with
     * the body, and the caster does not see it in first person. (The caster's real size is the ability's job: a
     * scale attribute. This is the skin of it.)
     */
    public static void giantForm(ServerLevel sl, Player player, double height, int ticks, String tag) {
        float full = (float) (height / 3.0);
        float start = (float) (player.getBbHeight() / 3.0);
        ModelFx body = GIANT.scale(full).lifetime(ticks).fade(4, 12).tag(tag)
                .key(0, pose().scale(start / full).alpha(0.3f))
                .key(GIANT_RISEN, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOnTurning(sl, body, player, Vec3.ZERO, 0f, 0f, 0f);
        for (int t = 0; t < GIANT_RISEN; t += 6) {
            Later.run(sl, t, () -> {
                ParticleShapes.ring(sl, DUST, player.position().add(0, 0.2, 0), 1.2 + height * 0.2, 12, 0.12);
                ParticleShapes.burst(sl, ParticleFx.of("fx/glow").color(0xFFA8E8FF).endColor(0x0040A0C0)
                        .size(0.1f).endSize(0.02f).lifetime(16, 6).gravity(-0.03f).friction(0.9f).glow(),
                        player.position().add(0, height * 0.9, 0), 4, 0.02, 0.08);
            });
        }
        Later.run(sl, GIANT_RISEN, () -> SpellFx.shockRing(sl, player.position(), height * 1.5, 0xC07A8AA0, 14, 0));
        sound(sl, player.position(), ModSounds.HAND_GIANT_FORM, 1.5f, (float) Math.max(0.6, 1.1 - height * 0.05));
    }

    /** A step of the Giant: the ground shakes, dust rolls out. */
    public static void giantStep(ServerLevel sl, Player player, double height) {
        SpellFx.shockRing(sl, player.position(), 1.5 + height * 0.6, 0x90A89070, 10, 0);
        ParticleShapes.ring(sl, DUST, player.position().add(0, 0.2, 0), 0.8 + height * 0.15, 10, 0.12);
        sound(sl, player.position(), ModSounds.HAND_GIANT_STEP, (float) Math.min(2.0, 0.6 + height * 0.2), wobble(1.0f));
    }

    /** Someone weaker sees the Giant: their mind strains (dark mist round the head). */
    public static void giantTerror(ServerLevel sl, LivingEntity onlooker) {
        Vec3 head = onlooker.position().add(0, onlooker.getBbHeight() + 0.1, 0);
        ParticleShapes.burst(sl, MIND_MIST.color(0xA0402030), head, 6, 0.02, 0.06);
        ParticleShapes.burst(sl, BLOOD_MOTE, head, 3, 0.02, 0.05);
    }

    /** The madness rising in the Giant (0..1): red motes round its head, more the madder it is. */
    public static void giantMadness(ServerLevel sl, Player player, float madness, double height) {
        int count = Math.round(2 + madness * 10);
        ParticleShapes.burst(sl, BLOOD_MOTE, player.position().add(0, height * 0.9, 0), count, 0.03, 0.06 + madness * 0.1);
        if (madness > 0.75f) play(sl, player.position(), SoundEvents.WARDEN_HEARTBEAT, 0.6f, 0.8f);
    }

    /** The form ends: the body breaks into grey-blue dust. */
    public static void giantFormEnd(ServerLevel sl, Player player, double height, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        for (double y = 0; y < height; y += 0.6) {
            ParticleShapes.ring(sl, DUST.color(0xA07A8AA0), player.position().add(0, y, 0), height * 0.18, 8, 0.05);
        }
    }

    // =====================================================================================
    // Key 7: the Silver and Twilight wheel (Silver Knight, Glory, Hand of God, Twilight Giant)
    // =====================================================================================

    // ---------- Mercury Liquefaction

    /** The tick the body has melted into a puddle. */
    public static final int MERCURY_MELTED = 8;

    /** The knight melts into liquid silver: the puddle under them is seen while they are hidden. */
    public static void mercury(ServerLevel sl, Player player, int ticks, String tag) {
        ModelFx pool = MERCURY_POOL.scale(1.6f).lifetime(ticks).fade(2, 8).tag(tag).shownWhileHidden()
                .key(0, pose().scale(0.2f))
                .key(MERCURY_MELTED, pose().scale(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, pool, player, new Vec3(0, 0.04, 0), 0f, 0f, 0f);
        for (int t = 0; t < MERCURY_MELTED; t += 2) {
            int step = t;
            Later.run(sl, t, () -> ParticleShapes.line(sl, SILVER_DROP, player.position().add(0, 1.8 - step * 0.2, 0),
                    player.position().add(0, 1.6 - step * 0.2, 0), 6));
        }
        sound(sl, player.position(), ModSounds.HAND_MERCURY, 1.0f, 1.0f);
    }

    /** While liquid: silver drops scatter from the puddle. */
    public static void mercuryTick(ServerLevel sl, Player player) {
        ParticleShapes.burst(sl, SILVER_DROP, player.position().add(0, 0.1, 0), 3, 0.04, 0.1);
    }

    /** An enemy the liquid slid past is wrapped in silver: a puddle under it and silver climbing it. */
    public static void mercuryWrap(ServerLevel sl, LivingEntity enemy, int ticks) {
        ModelFx pool = MERCURY_POOL.scale(Math.max(1.0f, enemy.getBbWidth() * 1.6f)).lifetime(ticks).fade(2, 8);
        ParticleShapes.modelOn(sl, pool, enemy, new Vec3(0, 0.04, 0), 0f, 0f, 0f);
        SpellFx.risingRings(sl, enemy.position(), Math.max(0.5, enemy.getBbWidth() * 0.7), enemy.getBbHeight(), 3, 0xE0D8DEEA);
        ParticleShapes.burst(sl, SILVER_DROP, chestOf(enemy), 8, 0.03, 0.1);
    }

    /** The knight re-forms: the puddle draws together and rises into a body. */
    public static void mercuryReform(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        SpellFx.risingRings(sl, player.position(), 0.6, 1.9, 4, 0xE0E8ECF4);
        ParticleShapes.burst(sl, SILVER_MOTE, chestOf(player), 14, 0.03, 0.12);
        sound(sl, player.position(), ModSounds.HAND_MERCURY, 0.8f, 1.3f);
    }

    /** Become an ally's Silver Armor: their body is coated in silver plate for 'ticks'. */
    public static void mercuryArmor(ServerLevel sl, LivingEntity ally, int ticks, String tag) {
        if (ally instanceof Player wearer) {
            dawnArmor(sl, wearer, ticks, tag, true);
        } else {
            SpellFx.circleUnder(sl, ally, Math.max(0.6, ally.getBbWidth() * 0.8), 0xC0D8DEEA, ticks, tag);
        }
        SpellFx.risingRings(sl, ally.position(), Math.max(0.5, ally.getBbWidth() * 0.7), ally.getBbHeight(), 4, 0xE0D8DEEA);
    }

    // ---------- Silver Rapier

    /** The tick the rapiers strike. */
    public static final int RAPIER_STRIKES = 10;

    /** 'count' silver rapiers appear round the target out of thin air, aim at its heart and stab in; one bursts out of it. */
    public static void silverRapiers(ServerLevel sl, LivingEntity target, int count) {
        Vec3 heart = chestOf(target);
        float spin = sl.getRandom().nextFloat() * 360f;
        for (int i = 0; i < count; i++) {
            float yaw = spin + i * 360f / count;
            double rise = (i % 2 == 0 ? 0.6 : -0.3);
            Vec3 from = heart.add(ahead(yaw).scale(2.6)).add(0, rise, 0);
            Vec3 way = heart.subtract(from);
            ModelFx rapier = SILVER_RAPIER.scale(0.75f).lifetime(RAPIER_STRIKES + 8).fade(2, 6)
                    .key(0, pose().up(-0.6f).alpha(0f))
                    .key(RAPIER_STRIKES - 3, pose().up(-0.8f).alpha(1f), Ease.OUT)
                    .key(RAPIER_STRIKES, pose().up((float) way.length() * 0.85f), Ease.IN);
            ParticleShapes.modelAlong(sl, rapier, from, way);
        }
        ModelFx burst = SILVER_RAPIER.scale(0.6f).lifetime(14).fade(0, 6)
                .key(0, pose().scale(0.05f).alpha(0f))
                .key(RAPIER_STRIKES, pose().scale(0.05f).alpha(0f))
                .key(RAPIER_STRIKES + 3, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelAlong(sl, burst, heart.add(0, -0.3, 0), new Vec3(0.2, 1, 0.1));
        Later.run(sl, RAPIER_STRIKES, () -> {
            ParticleShapes.burst(sl, SILVER_MOTE, heart, 20, 0.1, 0.35);
            flash(sl, heart, spin, 0xFFE8F0FF, 1.4f);
        });
        sound(sl, heart, ModSounds.HAND_RAPIER, 1.0f, wobble(1.0f));
    }

    // ---------- Light Concealment

    /** The knight steps into a light: a gate of light opens there and swallows them. */
    public static void lightEnter(ServerLevel sl, Player player, Vec3 light) {
        gate(sl, light, player.getYRot() + 180f);
        ParticleShapes.line(sl, DAWN_STAR, chestOf(player), light, 3);
        sound(sl, light, ModSounds.HAND_LIGHT_STEP, 1.0f, 1.0f);
    }

    /** And steps out of another one. */
    public static void lightExit(ServerLevel sl, Player player, Vec3 at) {
        gate(sl, at.add(0, 1.0, 0), player.getYRot());
        ParticleShapes.burst(sl, DAWN_STAR, at.add(0, 1.0, 0), 16, 0.04, 0.15);
        sound(sl, at, ModSounds.HAND_LIGHT_STEP, 1.0f, 1.2f);
    }

    private static void gate(ServerLevel sl, Vec3 at, float yaw) {
        ModelFx gate = LIGHT_GATE.color(0xF0FFF6D8).scale(1.8f).lifetime(18).fade(0, 8).spin(4f)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(5, pose().scale(1f).alpha(1f), Ease.OUT_BACK)
                .key(18, pose().scale(0.3f).alpha(0f), Ease.IN);
        ParticleShapes.model(sl, gate, at, yaw, 0f, 0f);
    }

    /** Glory's way out: the body ages in moments into rotten liquid that evaporates. */
    public static void withering(ServerLevel sl, Player player) {
        for (int t = 0; t < 16; t += 2) {
            int step = t;
            Later.run(sl, t, () -> {
                ParticleShapes.burst(sl, ASH, player.position().add(0, 1.8 - step * 0.1, 0), 6, 0.02, 0.06);
                ParticleShapes.spawn(sl, DECAY_SMOKE, player.position().add(0, 0.3, 0), new Vec3(0, 0.04, 0));
            });
        }
        SpellFx.closingRing(sl, player.position(), 1.6, 0xC08A6A3A, 16, 0);
        sound(sl, player.position(), ModSounds.HAND_DECAY, 0.9f, 1.3f);
    }

    // ---------- Twilight Sword

    /** The tick the sword hits the ground. */
    public static final int TWILIGHT_SWORD_HITS = 18;

    /** The void tears open above 'ground' and a broadsword of Glory plunges out of it, ripping the area apart. */
    public static void twilightSword(ServerLevel sl, Vec3 ground, double radius) {
        float yaw = sl.getRandom().nextFloat() * 360f;
        ModelFx rift = DECAY_SIGIL.color(0xE0FF5A1A).scale(3f).lifetime(TWILIGHT_SWORD_HITS + 10).fade(4, 8).spin(-4f)
                .key(0, pose().scale(0.1f))
                .key(8, pose().scale(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, rift, ground.add(0, 9, 0), yaw, 0f, 0f);
        ModelFx sword = TWILIGHT_SWORD.scale(1.8f).lifetime(TWILIGHT_SWORD_HITS + 22).fade(4, 12)
                .key(0, pose().pitch(180f).up(-12f).alpha(0f))
                .key(8, pose().up(-11f).alpha(1f))
                .key(TWILIGHT_SWORD_HITS, pose().up(-4.2f), Ease.IN);
        ParticleShapes.model(sl, sword, ground, yaw, 0f, 0f);
        SpellFx.warningCircle(sl, ground, radius, 0xC0FF7A1A, TWILIGHT_SWORD_HITS);
        Later.run(sl, TWILIGHT_SWORD_HITS, () -> {
            SpellFx.blast(sl, ground.add(0, 0.6, 0), radius, 0xE0FF7A1A, GLORY_EMBER);
            SpellFx.shockRing(sl, ground, radius * 1.4, 0xE0FFB050, 12, 0);
            ParticleShapes.burst(sl, GLOWING_ASH, ground.add(0, 1, 0), 40, 0.1, 0.4);
            ParticleShapes.ring(sl, DUSK_SMOKE, ground.add(0, 0.4, 0), radius * 0.7, 18, 0.12);
        });
        sound(sl, ground, ModSounds.HAND_TWILIGHT_SWORD, 1.6f, 1.0f);
    }

    // ---------- Divine Right Hand

    /** The tick the hand closes on its target. */
    public static final int HAND_GRIPS = 10;
    /** The tick the slam lands. */
    public static final int HAND_SLAMS = 14;

    /** A hand of Twilight reaches past the target, palm toward it, closes round it at HAND_GRIPS and holds for 'ticks'. */
    public static void divineHand(ServerLevel sl, Player player, LivingEntity target, int ticks, String tag) {
        float yaw = Spells.yawOf(target.position().subtract(player.position()));
        float scale = (float) Math.max(1.4, target.getBbHeight() * 1.1);
        ModelFx hand = GIANT_HAND.scale(scale).lifetime(ticks).fade(4, 8).tag(tag)
                .key(0, pose().forward(-1.6f).alpha(0f).frame(0))
                .key(HAND_GRIPS - 3, pose().forward(-0.5f).alpha(1f), Ease.OUT)
                .key(HAND_GRIPS, pose().frame(4), Ease.IN)
                .during(HAND_GRIPS, ticks, pose().roll(6f), Ease.IN_OUT);
        ParticleShapes.modelOn(sl, hand, target, new Vec3(0, target.getBbHeight() * 0.5, 0), yaw, 0f, 0f);
        ParticleShapes.burst(sl, GLORY_EMBER, chestOf(target), 10, 0.05, 0.15);
        Later.run(sl, HAND_GRIPS, () -> {
            ParticleShapes.burst(sl, GLORY_EMBER, chestOf(target), 20, 0.08, 0.25);
            ParticleShapes.burst(sl, ASH, chestOf(target), 10, 0.03, 0.1);
        });
        sound(sl, chestOf(target), ModSounds.HAND_DIVINE_HAND, 1.4f, 1.0f);
    }

    /** It flings the target: the hand opens and dusk streaks after what it threw. */
    public static void divineHandFling(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, GLORY_EMBER, chestOf(target), 24, 0.1, 0.35);
        SpellFx.shockRing(sl, target.position(), 3.0, 0xC0FF7A1A, 8, 0);
    }

    /** The hand slams down, palm first, on 'ground': everything in 'radius' is pressed into the earth. */
    public static void divineHandSlam(ServerLevel sl, Player player, Vec3 ground, double radius) {
        float scale = (float) Math.max(2.0, radius * 0.9);
        ModelFx hand = GIANT_HAND.scale(scale).lifetime(HAND_SLAMS + 16).fade(4, 10)
                .key(0, pose().pitch(90f).up(12f).alpha(0f))
                .key(4, pose().up(11f).alpha(1f))
                .key(HAND_SLAMS, pose().up(scale * 0.45f), Ease.IN)
                .key(HAND_SLAMS + 12, pose().up(scale * 0.4f).frame(2), Ease.OUT);
        ParticleShapes.model(sl, hand, ground, player.getYRot(), 0f, 0f);
        SpellFx.warningCircle(sl, ground, radius, 0xC0FF7A1A, HAND_SLAMS);
        Later.run(sl, HAND_SLAMS, () -> {
            SpellFx.shockRing(sl, ground, radius * 1.6, 0xE0FF9A4A, 14, 0);
            ParticleShapes.ring(sl, DUST, ground.add(0, 0.3, 0), radius * 0.8, 24, 0.2);
            ParticleShapes.burst(sl, GLOWING_ASH, ground.add(0, 0.6, 0), 30, 0.08, 0.3);
            decayRing(sl, ground, radius, 60, null);
        });
        sound(sl, ground, ModSounds.HAND_DIVINE_HAND, 1.6f, 0.8f);
    }

    // ---------- Passage of Time

    /** The tick time stands still (the clock's hands stop). */
    public static final int TIME_FROZEN = 24;

    /** A clock spreads over the ground 'radius' wide; its hands race, slow and stop at TIME_FROZEN. */
    public static void passageOfTime(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        float width = (float) (radius * 2.0);
        ModelFx dial = TIME_DIAL.color(0xC0FFB060).scale(width).lifetime(ticks).fade(0, 12).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(TIME_FROZEN, pose().scale(1f).alpha(1f), Ease.OUT);
        ModelFx hands = TIME_HANDS.color(0xE0FFD8A0).scale(width * 0.8f).lifetime(ticks).fade(0, 12).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(10, pose().scale(1f).alpha(1f), Ease.OUT)
                .during(0, TIME_FROZEN, pose().spin(1080f), Ease.OUT);
        float yaw = sl.getRandom().nextFloat() * 360f;
        ParticleShapes.model(sl, dial, center.add(0, 0.06, 0), yaw, 0f, 0f);
        ParticleShapes.model(sl, hands, center.add(0, 0.09, 0), yaw, 0f, 0f);
        SpellFx.steadyRing(sl, center, radius, 0xE0FF9A4A, TIME_FROZEN, 0);
        Later.run(sl, TIME_FROZEN, () -> {
            SpellFx.shockRing(sl, center, radius, 0xA0FFFFFF, 6, 0);
            ParticleShapes.ring(sl, HOURGLASS, center.add(0, 1.0, 0), radius * 0.6, 16, 0);
        });
        sound(sl, center, ModSounds.HAND_TIME_STOP, 1.4f, 1.0f);
    }

    /** One frozen in time: greyed light hangs round it and a small clock stands still over its head. */
    public static void timeFrozen(ServerLevel sl, LivingEntity enemy, int ticks, String tag) {
        ModelFx clock = TIME_DIAL.color(0xC0FFC080).scale(0.7f).lifetime(ticks).fade(4, 6).tag(tag)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ModelFx still = TIME_HANDS.color(0xE0FFE0B0).scale(0.56f).lifetime(ticks).fade(4, 6).tag(tag)
                .key(0, pose().alpha(0f))
                .key(6, pose().alpha(1f));
        Vec3 over = new Vec3(0, enemy.getBbHeight() + 0.6, 0);
        ParticleShapes.modelOn(sl, clock, enemy, over, 0f, 90f, 0f);
        ParticleShapes.modelOn(sl, still, enemy, over.add(0, 0.01, 0), 0f, 90f, 0f);
        SpellFx.circleUnder(sl, enemy, Math.max(0.6, enemy.getBbWidth() * 0.8), 0xA0FFB060, ticks, tag);
        ParticleShapes.burst(sl, HOURGLASS, chestOf(enemy), 4, 0.01, 0.03);
    }

    /** Time resumes for it: the clock shatters, and everything written down lands at once. */
    public static void timeResume(ServerLevel sl, LivingEntity enemy, String tag) {
        ParticleShapes.clearModels(sl, enemy, tag);
        flash(sl, chestOf(enemy), sl.getRandom().nextFloat() * 360f, 0xFFFFD8A0, 1.6f);
        ParticleShapes.burst(sl, HOURGLASS, chestOf(enemy), 10, 0.08, 0.25);
        sound(sl, chestOf(enemy), ModSounds.HAND_TIME_RESUME, 1.0f, wobble(1.0f));
    }

    // ---------- Decay

    /** Twilight shines over 'radius' for 'ticks': a crumbling ring spreads and ash drifts up from everything in it. */
    public static void decay(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        decayRing(sl, center, radius, ticks, tag);
        ParticleShapes.ring(sl, DUSK_SMOKE, center.add(0, 0.5, 0), radius * 0.9, 20, 0.02);
        SpellFx.steadyRing(sl, center, radius, 0xC0FF7A3A, 14, 0);
        sound(sl, center, ModSounds.HAND_DECAY, 1.2f, 1.0f);
    }

    private static void decayRing(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        ModelFx sigil = DECAY_SIGIL.color(0xC0D07A3A).scale((float) (radius * 2.0)).lifetime(ticks).fade(0, 14).spin(0.3f)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(14, pose().scale(1f).alpha(1f), Ease.OUT);
        if (tag != null) sigil = sigil.tag(tag);
        ParticleShapes.model(sl, sigil, center.add(0, 0.06, 0), sl.getRandom().nextFloat() * 360f, 0f, 0f);
    }

    /** Every half second while it lasts: ash and dusk smoke rise inside the circle. */
    public static void decayTick(ServerLevel sl, Vec3 center, double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            Vec3 at = center.add(Math.cos(angle) * far, 0.2 + random.nextDouble(), Math.sin(angle) * far);
            ParticleShapes.spawn(sl, i % 3 == 0 ? GLOWING_ASH : ASH, at, new Vec3(0, 0.03, 0));
            if (i % 4 == 0) ParticleShapes.spawn(sl, DECAY_SMOKE, at, new Vec3(0, 0.01, 0));
        }
    }

    /** Someone rotting in it: ash flakes off them. */
    public static void decayOn(ServerLevel sl, LivingEntity enemy) {
        ParticleShapes.burst(sl, ASH, chestOf(enemy), 5, 0.02, 0.06);
        ParticleShapes.spawn(sl, DECAY_SMOKE, chestOf(enemy), new Vec3(0, 0.02, 0));
    }

    public static void decayEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
    }

    // ---------- Divine Kingdom

    /** The tick the kingdom has descended. */
    public static final int KINGDOM_RISEN = 40;

    /**
     * The sky turns to dusk: a huge setting sun stands on the horizon where the caster looks, the kingdom's seal
     * covers 'radius', and its walls of dusk smoke rise at the edge, for 'ticks'.
     */
    public static void divineKingdom(ServerLevel sl, Player player, double radius, int ticks, String tag) {
        Vec3 center = player.position();
        float yaw = player.getYRot();
        ModelFx sun = TWILIGHT_SUN.scale((float) Math.max(30, radius * 1.2)).lifetime(ticks).fade(0, 30).tag(tag)
                .key(0, pose().up(-20f).alpha(0f))
                .key(KINGDOM_RISEN, pose().up(0f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, sun, center.add(ahead(yaw).scale(radius * 2.0)).add(0, radius * 0.4, 0), yaw + 180f, 0f, 0f);
        ModelFx seal = KINGDOM_SEAL.color(0xB0FF8A3A).scale((float) (radius * 2.0)).lifetime(ticks).fade(0, 20).spin(0.1f).tag(tag)
                .key(0, pose().scale(0.05f).alpha(0f))
                .key(KINGDOM_RISEN, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, seal, center.add(0, 0.06, 0), yaw, 0f, 0f);
        seal(sl, center, 3.0, 0xC0FFB060, ticks, tag);
        SpellFx.steadyRing(sl, center, radius, 0xE0FF7A1A, KINGDOM_RISEN, 0);
        Later.run(sl, KINGDOM_RISEN, () -> kingdomTick(sl, center, radius));
        sound(sl, center, ModSounds.HAND_KINGDOM, 2.0f, 1.0f);
    }

    /** Every second while it stands: dusk smoke rises along the walls and embers drift inside. */
    public static void kingdomTick(ServerLevel sl, Vec3 center, double radius) {
        ParticleShapes.ring(sl, DUSK_SMOKE, center.add(0, 0.5, 0), radius, Math.max(16, (int) (radius * 2)), 0);
        ParticleShapes.ring(sl, DUSK_SMOKE, center.add(0, 2.5, 0), radius, Math.max(12, (int) (radius * 1.5)), 0);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 12; i++) {
            double angle = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            ParticleShapes.spawn(sl, GLORY_EMBER, center.add(Math.cos(angle) * far, 0.5 + random.nextDouble() * 3,
                    Math.sin(angle) * far), new Vec3(0, 0.02, 0));
        }
    }

    /** An ally the kingdom acknowledges: a golden ring and motes round them. */
    public static void kingdomAcknowledged(ServerLevel sl, LivingEntity ally) {
        SpellFx.risingRings(sl, ally.position(), Math.max(0.5, ally.getBbWidth() * 0.7), ally.getBbHeight(), 2, 0xC0FFD86A);
        ParticleShapes.ring(sl, DAWN_MOTE, ally.position().add(0, 0.2, 0), 0.6, 8, 0);
    }

    public static void kingdomEnd(ServerLevel sl, Player player, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.burst(sl, GLORY_EMBER, center.add(0, 2, 0), 40, 0.1, 0.3);
    }

    // =====================================================================================
    // Passives that show
    // =====================================================================================

    /** Silver Knight's Dawn Aura, once a second: a few motes of dawn drift round the knight. */
    public static void dawnAura(ServerLevel sl, Player player, double radius) {
        ParticleShapes.ring(sl, DAWN_MOTE.color(0x90FFF0B0), player.position().add(0, 0.3, 0), radius * 0.4, 4, 0.01);
    }

    /** Demon Hunter's Tracking: a green footprint where a hunted enemy stepped (only send it to the hunter if you can). */
    public static void footprint(ServerLevel sl, Vec3 at, float yaw) {
        ParticleShapes.spawn(sl, FOOTPRINT, at.add(0, 0.05, 0), Vec3.ZERO);
    }

    /** The same footprint, seen only by the hunter. */
    public static void footprint(ServerPlayer hunter, Vec3 at, float yaw) {
        ParticleShapes.spawnFor(hunter, FOOTPRINT, at.add(0, 0.05, 0), Vec3.ZERO);
    }

    /** Spiritual Intuition: something started hunting the Demon Hunter out of sight. A chime only they hear. */
    public static void intuition(ServerPlayer hunter) {
        hunter.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME),
                SoundSource.PLAYERS, hunter.getX(), hunter.getY(), hunter.getZ(), 0.6f, 1.8f, hunter.getRandom().nextLong()));
    }

    /** Hand of God's Divine Touch: a bare-handed blow decays what it lands on. */
    public static void divineTouch(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, GLOWING_ASH, chestOf(target), 6, 0.04, 0.12);
        flash(sl, chestOf(target), sl.getRandom().nextFloat() * 360f, 0xC0FF7A1A, 0.8f);
    }

    // =====================================================================================
    // /handfx: every display, to look at in game
    // =====================================================================================

    @FunctionalInterface
    private interface Show {
        void play(ServerLevel sl, Player viewer, LivingEntity target, Vec3 spot);
    }

    /** onViewer = it is shown on the viewer (worn), not on a spot or a target. */
    private record Case(String name, int ticks, boolean onViewer, Show show) {}

    private static String tagFor(String name) {
        return "hg_fx_" + name + "_" + UUID.randomUUID();
    }

    private static final List<Case> CASES = List.of(
            new Case("charge", 40, true, (sl, v, t, spot) -> {
                chargeStart(sl, v);
                for (int i = 1; i < 10; i++) Later.run(sl, i, () -> chargeTrail(sl, v));
                if (t != v) Later.run(sl, 10, () -> chargeImpact(sl, v, t));
            }),
            new Case("divine_blood", 80, true, (sl, v, t, spot) -> {
                String tag = tagFor("blood");
                divineBlood(sl, v, 70, tag);
            }),
            new Case("flurry", 40, false, (sl, v, t, spot) -> {
                if (t == v) return;
                for (int i = 0; i < 5; i++) {
                    int index = i;
                    Later.run(sl, i * 4, () -> flurryBlow(sl, v, t, index));
                }
            }),
            new Case("resistance", 80, true, (sl, v, t, spot) -> {
                String tag = tagFor("resist");
                resistance(sl, v, 70, tag);
                Later.run(sl, 30, () -> resistanceShrug(sl, v));
            }),
            new Case("grandmaster", 60, true, (sl, v, t, spot) -> {
                grandmastersEdge(sl, v, 60);
                for (int i = 0; i < 3; i++) {
                    int index = i;
                    Later.run(sl, 8 + i * 10, () -> {
                        if (index < 2) edgeSwing(sl, v, index);
                        else edgeFinisher(sl, v, t == v ? null : t);
                    });
                }
                for (int i = 4; i < 50; i += 4) Later.run(sl, i, () -> edgeTick(sl, v));
            }),
            new Case("riposte", 50, true, (sl, v, t, spot) -> {
                riposte(sl, v);
                Later.run(sl, 12, () -> riposteParry(sl, v, t == v ? null : t));
                if (t != v) Later.run(sl, 22, () -> riposteCounter(sl, v, t));
            }),
            new Case("light_of_dawn", 120, false, (sl, v, t, spot) -> {
                String tag = tagFor("dawn");
                lightOfDawn(sl, spot, 6, 110, tag);
                for (int i = 1; i < 5; i++) Later.run(sl, LIGHT_RISEN + i * 20, () -> lightOfDawnPulse(sl, spot, 6));
                if (t != v) Later.run(sl, LIGHT_RISEN + 4, () -> {
                    lightOfDawnReveal(sl, t);
                    lightOfDawnBurn(sl, t);
                });
                Later.run(sl, LIGHT_RISEN + 10, () -> lightOfDawnHeal(sl, v));
            }),
            new Case("dawn_armor", 80, true, (sl, v, t, spot) -> {
                String tag = tagFor("armor");
                dawnArmor(sl, v, 70, tag, false);
                Later.run(sl, 30, () -> armorHit(sl, v, false));
            }),
            new Case("silver_armor", 80, true, (sl, v, t, spot) -> {
                String tag = tagFor("silver");
                dawnArmor(sl, v, 70, tag, true);
                Later.run(sl, 30, () -> armorHit(sl, v, true));
            }),
            new Case("sword_of_dawn", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("sword");
                swordOfDawn(sl, v, 80, tag);
                Later.run(sl, 30, () -> dawnSlash(sl, v, false));
                Later.run(sl, 50, () -> dawnSlash(sl, v, true));
                if (t != v) Later.run(sl, 52, () -> purify(sl, t));
            }),
            new Case("hurricane", 110, true, (sl, v, t, spot) -> {
                hurricaneGather(sl, v);
                Vec3 ground = v.position().add(Spells.flatLook(v).scale(1.5));
                Later.run(sl, HURRICANE_GATHERS, () -> hurricane(sl, ground, v.getYRot(), 20, 60, false));
                if (t != v) Later.run(sl, HURRICANE_GATHERS + 20, () -> hurricaneHit(sl, t, false));
            }),
            new Case("hurricane_glory", 110, true, (sl, v, t, spot) -> {
                hurricaneGather(sl, v);
                Vec3 ground = v.position().add(Spells.flatLook(v).scale(1.5));
                Later.run(sl, HURRICANE_GATHERS, () -> hurricane(sl, ground, v.getYRot(), 30, 70, true));
                if (t != v) Later.run(sl, HURRICANE_GATHERS + 20, () -> hurricaneHit(sl, t, true));
            }),
            new Case("protection", 140, false, (sl, v, t, spot) -> {
                String tag = tagFor("protection");
                protection(sl, v, spot, 5, 130, tag);
                for (int i = 0; i < 3; i++) {
                    Later.run(sl, PROTECTION_RAISED + 20 + i * 20, () -> domeHit(sl, spot.add(ahead(v.getYRot() + 180f).scale(5)).add(0, 1.5, 0)));
                }
                Later.run(sl, 100, () -> domeWeaken(sl, spot, 5));
            }),
            new Case("cage", 140, false, (sl, v, t, spot) -> {
                cage(sl, spot.add(0, 2.5, 0), 4, 130, tagFor("cage"));
            }),
            new Case("transference", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("aegis");
                List<LivingEntity> allies = t == v ? List.of() : List.of(t);
                transference(sl, v, allies, 80, tag);
                if (t != v) for (int i = 1; i < 4; i++) Later.run(sl, i * 20, () -> transferenceTaken(sl, t, v));
            }),
            new Case("eye", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("eye");
                eye(sl, v, 80, tag);
                if (t != v) Later.run(sl, EYE_OPENS + 4, () -> {
                    eyeMark(sl, t, 70, tag);
                    eyeEvil(sl, t, 70);
                });
            }),
            new Case("alchemy", 80, true, (sl, v, t, spot) -> {
                Coat[] coats = Coat.values();
                Coat coat = coats[ThreadLocalRandom.current().nextInt(coats.length)];
                alchemy(sl, v, coat);
                for (int i = VIAL_POURED + 4; i < 70; i += 4) Later.run(sl, i, () -> coatTick(sl, v, coat));
                if (t != v) Later.run(sl, 50, () -> coatHit(sl, t, coat));
            }),
            new Case("mind_concealment", 70, true, (sl, v, t, spot) -> {
                String tag = tagFor("mind");
                mindConcealment(sl, v, 60, tag);
                Later.run(sl, 60, () -> mindConcealmentEnd(sl, v, tag));
            }),
            new Case("giant_form", 140, true, (sl, v, t, spot) -> {
                String tag = tagFor("giant");
                giantForm(sl, v, 4.0, 130, tag);
                for (int i = 0; i < 4; i++) Later.run(sl, GIANT_RISEN + 10 + i * 18, () -> giantStep(sl, v, 4.0));
                for (int i = 0; i < 5; i++) {
                    float madness = i / 4f;
                    Later.run(sl, GIANT_RISEN + i * 20, () -> giantMadness(sl, v, madness, 4.0));
                }
                if (t != v) Later.run(sl, GIANT_RISEN, () -> giantTerror(sl, t));
                Later.run(sl, 130, () -> giantFormEnd(sl, v, 4.0, tag));
            }),
            new Case("giant_twilight", 120, false, (sl, v, t, spot) -> {
                ModelFx colossus = GIANT.scale(10f).lifetime(110).fade(20, 20)
                        .key(0, pose().up(-6f).alpha(0f))
                        .key(40, pose().up(0f).alpha(1f), Ease.OUT);
                Vec3 far = v.position().add(ahead(v.getYRot()).scale(24));
                ParticleShapes.model(sl, colossus, far, v.getYRot() + 180f, 0f, 0f);
                sound(sl, far, ModSounds.HAND_GIANT_FORM, 2.0f, 0.6f);
                for (int i = 0; i < 3; i++) Later.run(sl, 50 + i * 20, () -> SpellFx.shockRing(sl, far, 14, 0xC07A8AA0, 16, 0));
            }),
            new Case("mercury", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("mercury");
                mercury(sl, v, 70, tag);
                for (int i = MERCURY_MELTED; i < 70; i += 5) Later.run(sl, i, () -> mercuryTick(sl, v));
                if (t != v) Later.run(sl, 30, () -> mercuryWrap(sl, t, 40));
                Later.run(sl, 70, () -> mercuryReform(sl, v, tag));
            }),
            new Case("rapier", 50, false, (sl, v, t, spot) -> {
                if (t == v) return;
                silverRapiers(sl, t, 3);
            }),
            new Case("light_concealment", 50, true, (sl, v, t, spot) -> {
                Vec3 light = v.getEyePosition().add(Spells.flatLook(v).scale(2));
                lightEnter(sl, v, light);
                Later.run(sl, 20, () -> lightExit(sl, v, spot));
            }),
            new Case("withering", 40, true, (sl, v, t, spot) -> withering(sl, v)),
            new Case("twilight_sword", 70, false, (sl, v, t, spot) -> twilightSword(sl, spot, 5)),
            new Case("divine_hand", 80, false, (sl, v, t, spot) -> {
                if (t == v) {
                    divineHandSlam(sl, v, spot, 6);
                    return;
                }
                String tag = tagFor("hand");
                divineHand(sl, v, t, 60, tag);
                Later.run(sl, 60, () -> divineHandFling(sl, t, tag));
            }),
            new Case("hand_slam", 60, false, (sl, v, t, spot) -> divineHandSlam(sl, v, spot, 6)),
            new Case("passage_of_time", 140, false, (sl, v, t, spot) -> {
                String tag = tagFor("time");
                passageOfTime(sl, spot, 8, 130, tag);
                if (t != v) {
                    Later.run(sl, TIME_FROZEN, () -> timeFrozen(sl, t, 100, tag));
                    Later.run(sl, 124, () -> timeResume(sl, t, tag));
                }
            }),
            new Case("decay", 120, false, (sl, v, t, spot) -> {
                String tag = tagFor("decay");
                decay(sl, spot, 6, 110, tag);
                for (int i = 1; i < 10; i++) Later.run(sl, i * 10, () -> decayTick(sl, spot, 6));
                if (t != v) for (int i = 1; i < 5; i++) Later.run(sl, i * 20, () -> decayOn(sl, t));
            }),
            new Case("divine_kingdom", 200, true, (sl, v, t, spot) -> {
                String tag = tagFor("kingdom");
                Vec3 center = v.position();
                divineKingdom(sl, v, 16, 190, tag);
                for (int i = 1; i < 7; i++) Later.run(sl, KINGDOM_RISEN + i * 20, () -> kingdomTick(sl, center, 16));
                Later.run(sl, KINGDOM_RISEN, () -> kingdomAcknowledged(sl, v));
                Later.run(sl, 190, () -> kingdomEnd(sl, v, center, tag));
            }),
            new Case("passives", 60, true, (sl, v, t, spot) -> {
                for (int i = 0; i < 3; i++) Later.run(sl, i * 20, () -> dawnAura(sl, v, 6));
                for (int i = 0; i < 6; i++) {
                    Vec3 step = spot.add(ahead(v.getYRot()).scale(i * 0.7)).add(Spells.rightOf(v).scale(i % 2 == 0 ? 0.2 : -0.2));
                    footprint(sl, step, v.getYRot());
                }
                if (t != v) Later.run(sl, 20, () -> divineTouch(sl, t));
            }));

    /** The names /handfx knows. */
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
        event.getDispatcher().register(Commands.literal("handfx")
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
