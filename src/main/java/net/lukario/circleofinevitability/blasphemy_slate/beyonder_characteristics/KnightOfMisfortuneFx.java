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
 * How the Knight Of Misfortune (Darkness) pathway looks and sounds: every key, both casts, every wheel entry, what
 * grows at higher Sequences (the complete Demonic Wolf, Misfortune and Catastrophe, the Sword's edicts) and the
 * passives that show. Visual only: nothing here hurts, holds, hides, puts to sleep or moves anyone. The ability code
 * does that, at the ticks named by the constants below (..._GROWN, ..._RISEN, ..._OPENS...), with Later.run.
 * See docs/KNIGHT_OF_MISFORTUNE.md.
 *
 * ---------- See them in game ----------
 *   /knightfx all              every display one after another (/knightfx all 3 = three times over)
 *   /knightfx night_domain     just that one (press TAB after /knightfx for the names)
 * Displays that need a target use the nearest mob within 12 blocks, or you when there is none.
 *
 * ---------- Use them in an ability ----------
 *   Key 1  spiritVision(viewer, ticks) (only the viewer sees it), spiritVisionMark(viewer, seen, spirit)
 *          sneak: dowsing(sl, player, toward, ticks, tag), dowsingEnd(sl, player, tag)
 *   Key 2  the poem wheel's pictures: WHEEL_TRANQUILIZE, WHEEL_LULLABY, WHEEL_PACIFY, WHEEL_FEAR, WHEEL_AGITATING
 *          every poem starts with recite(sl, player, Poem.X, tag); then
 *          tranquilize(sl, player, radius), tranquilized(sl, ally), tranquilizeReveal(sl, target)
 *          lullaby(sl, player, range) (asleep at POEM_LANDS), slumber(sl, sleeper, ticks, tag), wake(sl, sleeper, tag)
 *          pacify(sl, player, target, ticks, tag) (it lands at POEM_LANDS)
 *          fear(sl, player, range), afraid(sl, victim, ticks, tag)
 *          agitate(sl, player, target, ticks, tag)
 *   Key 3  nightmareLimbs(sl, player, ticks, tag) (grown at LIMBS_GROWN), limbLash(sl, player, target, index),
 *              limbsTear(sl, player, tag)
 *          sneak: nightmareState(sl, player, ticks, tag) (out at SOUL_OUT), nightmareTick(sl, player),
 *              nightmareReturn(sl, player, body, tag); dreamInvade(sl, player, sleeper, ticks, tag) (in at DREAM_OPENS),
 *              dreamShape(sl, sleeper, Dream.X), dreamEnd(sl, sleeper, tag)
 *   Key 4  requiem(sl, center, radius, ticks, tag) (sung at REQUIEM_RISEN), requiemPulse(sl, center, radius),
 *              requiemOn(sl, enemy, depth), requiemEnd(sl, center, tag)
 *          sneak: soothingRequiem(sl, player, radius), soothed(sl, ally)
 *   Key 5  the wheel's pictures: WHEEL_TERROR_BANSHEE, WHEEL_EARTHEN_SPIRIT, WHEEL_GALE_SPIRIT, WHEEL_COMMAND_SPIRITS
 *          spiritRelease(sl, player, at) before each spirit; banshee(sl, body, ticks, tag) on the summoned body,
 *              bansheeWail(sl, body, radius), bansheeEnd(sl, body, tag)
 *          earthMerge(sl, player, ticks, tag) (sunk at EARTH_SUNK), earthTick(sl, player), earthEmerge(sl, player, tag)
 *          gale(sl, player, ticks, tag), galeTick(sl, player), galeEnd(sl, player, tag)
 *          commandSpirits(sl, player, radius), spiritBound(sl, creature, ticks, tag)
 *   Key 6  the wheel's pictures: WHEEL_NIGHT_DOMAIN, WHEEL_HAIR_ENTANGLEMENT, WHEEL_CURSE_OF_MISFORTUNE, WHEEL_DEMONIC_WOLF
 *          nightDomain(sl, center, radius, ticks, tag) (spread at DOMAIN_RISEN), domainPulse(sl, center, radius),
 *              domainCorrode(sl, enemy), domainEnd(sl, center, tag)
 *          hairEntangle(sl, player, caught) (they are bound at HAIR_BINDS), hairBind(sl, victim, ticks, tag)
 *          curse(sl, player, target) (it lands at CURSE_LANDS), misfortune(sl, target, level, ticks, tag),
 *              misfortuneArea(sl, center, radius, ticks, tag) (Sequence 1), mishap(sl, victim, Mishap.X)
 *          wolfForm(sl, player, height, ticks, tag, complete) (full size at WOLF_RISEN), wolfStep(sl, player, height),
 *              wolfHowl(sl, player, height), wolfBite(sl, player, target), wolfMadness(sl, player, madness, height),
 *              wolfFormEnd(sl, player, height, tag)
 *   Key 7  the wheel's pictures: WHEEL_SWORD_OF_DARKNESS, WHEEL_NIGHTMARE_WORLD, WHEEL_CONCEALED_STATE,
 *              WHEEL_SECRET_CONCEALMENT, WHEEL_CONCEALED_WORLD, WHEEL_MANTLE_OF_DARKNESS, WHEEL_ETERNAL_REST,
 *              WHEEL_DESTINATION_OF_ALL_THINGS
 *          darkSword(sl, player, ticks, tag) (formed at SWORD_FORMED), darkSwing(sl, player), darkTide(sl, player,
 *              length, ticks) which returns its ticks, tideMelt(sl, victim), edict(sl, player, target, Edict.X),
 *              darkSwordEnd(sl, player, tag)
 *          nightmareWorld(sl, center, radius, ticks, tag) (closed at WORLD_CLOSED), nightmareWorldPulse(sl, center, radius)
 *          concealedState(sl, target, ticks, tag) (gone at CONCEALED), concealedStateEnd(sl, target, tag),
 *              reverseConcealment(sl, target)
 *          secretConcealment(sl, center, radius, ticks, tag), secretPulse(sl, center, radius)
 *          concealedWorld(sl, center, radius, ticks, tag) (closed at WORLD_CLOSED), concealedWorldEnd(sl, center, radius, tag)
 *          mantle(sl, player, ticks, tag) (risen at MANTLE_RISEN), mantleTick(sl, player), mantleSwing(sl, player, struck),
 *              mantleEnd(sl, player, tag)
 *          eternalRest(sl, player, target) (it falls at REST_FALLS), restWard(sl, player, ticks, tag), restWake(sl, player, tag)
 *          destination(sl, center, radius, ticks, tag) (open at DESTINATION_OPENS), destinationTick(sl, center, radius),
 *              dissolve(sl, victim), destinationEnd(sl, center, tag)
 *   Passives: nocturnality(sl, player), dangerChime(player), horrorAura(sl, player, radius), horrorFlinch(sl, victim),
 *          illOmen(sl, attacker)
 *   Shells: veilShell(sl, center, radius, color, ticks, tag), a sphere of fog tiles for any wall of darkness.
 *   Every timed cast: status(player, Status.X, ticks) shows its picture and its clock; statusEnd(player, Status.X) takes them away.
 *
 * The models are in models/model_fx/knight_of_misfortune/ (made by tools/model_gen/knight_of_misfortune/models.py; each
 * JSON's "credit" gives its size and pivot), the pictures in textures/gui/knight_of_misfortune/ and
 * textures/gui/ability_menus/knight_of_misfortune/ (icons.py), the particles in textures/particle/fx/ (particles.py),
 * the sounds in sounds/knight_of_misfortune/ (sounds.py) and sounds/ModSounds (KNIGHT_...).
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class KnightOfMisfortuneFx {

    private KnightOfMisfortuneFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // Colors (ARGB)
    // =====================================================================================

    /** The Pathway's own color: purpureus, the violet-black of Darkness. */
    public static final int PATHWAY_COLOR = 0xFF5A2E8C;
    public static final int DARKNESS = 0xFF5A2E8C;
    public static final int VIOLET = 0xFF9A5AD8;
    public static final int VIOLET_LIGHT = 0xFFD2B4FF;
    public static final int MOON = 0xFFC8DCFF;
    public static final int DREAM = 0xFFA8C0FF;
    public static final int BONE = 0xFFE8E0CE;
    public static final int OMEN = 0xFFB8A25A;
    public static final int ASH = 0xFF6A6474;
    public static final int FEAR_RED = 0xFFC02838;
    public static final int SPIRIT = 0xFFB4D4E8;

    /** The Midnight Poems, each with its color (the script round the head, the notes, the wave). */
    public enum Poem {
        TRANQUILIZE(0xFFC8DCFF),
        LULLABY(0xFFA8C0FF),
        PACIFY(0xFFB4B4D8),
        FEAR(0xFFC02838),
        AGITATING(0xFFE04A9A);

        public final int color;

        Poem(int color) {
            this.color = color;
        }
    }

    /** What Dream Shaping makes of a dream. */
    public enum Dream {
        SECRETS(0xFFA8C0FF),
        TERROR(0xFFC02838),
        OBEDIENCE(0xFF9A5AD8);

        public final int color;

        Dream(int color) {
            this.color = color;
        }
    }

    /** The Misfortunes that befall someone who carries Misfortune (docs/KNIGHT_OF_MISFORTUNE.md, "Misfortune"). */
    public enum Mishap {
        STUMBLE,
        FUMBLE,
        BACKFIRE,
        FALLING_STONE,
        CALAMITY
    }

    /** The three edicts of the Knight of Misfortune's Sword of Darkness. */
    public enum Edict {
        ETERNAL_SLEEP(0xFFA8C0FF),
        DESPAIR(0xFF6A6474),
        DESTRUCTION(0xFF9A2AD8);

        public final int color;

        Edict(int color) {
            this.color = color;
        }
    }

    // =====================================================================================
    // The small particles everything shares
    // =====================================================================================

    private static final ParticleFx DARK_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFB48AFF).endColor(0x002A0A4A)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(24, 12).gravity(-0.02f).friction(0.92f).glow();
    private static final ParticleFx MOON_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFE8F0FF).endColor(0x004A6AB0)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(-0.015f).friction(0.92f).glow();
    private static final ParticleFx DREAM_MOTE = MOON_MOTE.color(0xFFA8C0FF).endColor(0x005A2E8C);
    private static final ParticleFx STAR = ParticleFx.of("fx/sparkle")
            .color(0xFFFFFFFF).endColor(0x00A8C0FF)
            .size(0.07f).endSize(0.02f).sizeRandom(0.5f)
            .lifetime(20, 10).gravity(-0.005f).friction(0.94f).glow().randomRotation();
    private static final ParticleFx DARK_SMOKE = ParticleFx.of("fx/smoke")
            .color(0xC0180A28).endColor(0x00080410)
            .size(0.35f).endSize(0.9f).sizeRandom(0.3f)
            .lifetime(30, 12).gravity(-0.008f).friction(0.9f).spin(1.5f).randomRotation();
    private static final ParticleFx FOG = ParticleFx.of("fx/smoke")
            .color(0x90302048).endColor(0x00100818)
            .size(0.5f).endSize(1.2f).sizeRandom(0.3f)
            .lifetime(40, 16).gravity(-0.003f).friction(0.9f).spin(1f).randomRotation();
    private static final ParticleFx NOTE = ParticleFx.of("fx/note")
            .color(0xFFC8DCFF).endColor(0x004A6AB0)
            .size(0.1f).endSize(0.08f).sizeRandom(0.3f)
            .lifetime(30, 10).gravity(-0.02f).friction(0.94f).glow();
    private static final ParticleFx SLEEP_Z = ParticleFx.of("fx/sleep_z")
            .color(0xFFE0E8FF).endColor(0x00A8C0FF)
            .size(0.09f).endSize(0.14f).sizeRandom(0.2f)
            .lifetime(36, 10).gravity(-0.02f).friction(0.95f).glow();
    private static final ParticleFx WISP = ParticleFx.of("fx/wisp")
            .color(0xC0C8DCFF).endColor(0x004A6AB0)
            .size(0.14f).endSize(0.2f).sizeRandom(0.3f)
            .lifetime(30, 12).gravity(-0.02f).friction(0.93f).glow().randomRotation();
    private static final ParticleFx DARK_WISP = WISP.color(0xD0A070E0).endColor(0x00200840);
    private static final ParticleFx HAIR = ParticleFx.of("fx/hair")
            .color(0xFFE0E0EC).endColor(0x40606078)
            .size(0.14f).endSize(0.12f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(0.01f).friction(0.9f).spin(6f).randomRotation();
    private static final ParticleFx TOOTH = ParticleFx.of("fx/tooth")
            .color(0xFFF0E8D8).endColor(0x80A09888)
            .size(0.07f).endSize(0.06f)
            .lifetime(16, 6).gravity(0.02f).friction(0.9f).spin(8f).randomRotation();
    private static final ParticleFx HORSESHOE = ParticleFx.of("fx/horseshoe")
            .color(0xFFD8C27A).endColor(0x406A5A2A)
            .size(0.1f).endSize(0.08f).sizeRandom(0.3f)
            .lifetime(26, 10).gravity(0.03f).friction(0.94f).spin(5f).randomRotation();
    private static final ParticleFx SKULL = ParticleFx.of("fx/skull")
            .color(0xFFE8E0CE).endColor(0x205A2E8C)
            .size(0.1f).endSize(0.12f).sizeRandom(0.3f)
            .lifetime(26, 10).gravity(-0.015f).friction(0.94f).glow();
    private static final ParticleFx EYE = ParticleFx.of("fx/eye")
            .color(0xFFD2B4FF).endColor(0x005A2E8C)
            .size(0.09f).endSize(0.11f).sizeRandom(0.3f)
            .lifetime(24, 10).gravity(-0.01f).friction(0.94f).glow();
    private static final ParticleFx BONE_CHIP = ParticleFx.of("fx/splinter")
            .color(0xFFE8E0CE).endColor(0x80A09888)
            .size(0.07f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(18, 8).gravity(0.05f).friction(0.92f).spin(10f).randomRotation();
    private static final ParticleFx BLOOD = ParticleFx.of("fx/droplet")
            .color(0xFF4A0A2A).endColor(0xC0200410)
            .size(0.06f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(20, 8).gravity(0.07f).friction(0.98f).collide();
    private static final ParticleFx DUST = ParticleFx.of("fx/smoke")
            .color(0xA06A5A48).endColor(0x00403020)
            .size(0.3f).endSize(0.7f).sizeRandom(0.3f)
            .lifetime(22, 10).gravity(-0.005f).friction(0.88f).spin(2f).randomRotation();
    private static final ParticleFx WIND = ParticleFx.of("fx/sliver")
            .color(0x90D8E8FF).endColor(0x004A6AB0)
            .size(0.12f).endSize(0.06f).sizeRandom(0.4f)
            .lifetime(12, 6).gravity(0f).friction(0.96f);
    private static final ParticleFx SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFFFFFF).endColor(0x00B48AFF)
            .size(0.06f).endSize(0.02f)
            .lifetime(10, 6).gravity(0.04f).friction(0.88f).glow();

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

    /** One of the pathway's own sounds that only this player hears. */
    private static void soundFor(ServerPlayer player, RegistryObject<SoundEvent> sound, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound.get()),
                SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), volume, pitch, player.getRandom().nextLong()));
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
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private static double widthOf(Entity entity) {
        return Math.max(0.6, entity.getBbWidth() * 0.8);
    }

    // =====================================================================================
    // Statuses: a picture in the row above the hotbar stacks, and a clock beside the hotbar
    // =====================================================================================

    public enum Status {
        SPIRIT_VISION("spirit_vision", "Spirit Vision", 0xA8C0FF),
        DOWSING("dowsing", "Dowsing", 0xC8DCFF),
        LIMBS("limbs", "Nightmare Limbs", 0x9A5AD8),
        NIGHTMARE_STATE("nightmare_state", "Nightmare", 0xC8DCFF),
        DREAM("dream", "In a Dream", 0xA8C0FF),
        REQUIEM("requiem", "Requiem", 0xC8DCFF),
        SOOTHING("soothing", "Soothed", 0xA8C0FF),
        BANSHEE("banshee", "Banshee", 0xB4D4E8),
        EARTH("earth", "Earthen Spirit", 0xAA8256),
        GALE("gale", "Gale Spirit", 0x96BEE6),
        SPIRITS("spirits", "Spirits", 0xE8E0CE),
        NIGHT_DOMAIN("night_domain", "Night Domain", 0x9A5AD8),
        MISFORTUNE("misfortune", "Misfortune", 0xB8A25A),
        WOLF("wolf", "Demonic Wolf", 0x6A6474),
        MADNESS("madness", "Madness", 0xC04080),
        DARK_SWORD("dark_sword", "Sword of Darkness", 0xD2B4FF),
        NIGHTMARE_WORLD("nightmare_world", "Nightmare World", 0x9A5AD8),
        CONCEALED("concealed", "Concealed", 0x6A4A90),
        SECRET("secret", "Secret", 0xD2B4FF),
        CONCEALED_WORLD("concealed_world", "Concealed World", 0x9A5AD8),
        MANTLE("mantle", "Mantle of Darkness", 0x5A2E8C),
        ETERNAL_REST("eternal_rest", "Eternal Rest", 0xD2B4FF),
        SLUMBER("slumber", "Slumber", 0xC8DCFF),
        FEAR("fear", "Fear", 0xC02838);

        final String image;
        final String label;
        final int rgb;

        Status(String image, String label, int rgb) {
            this.image = image;
            this.label = label;
            this.rgb = rgb;
        }

        String id() {
            return "km_status_" + name().toLowerCase();
        }
    }

    /** Shows the status's picture (in the row above the hotbar stacks, rows of nine) and a clock beside the hotbar. */
    public static void status(Player player, Status status, int ticks) {
        int slot = status.ordinal();
        ScreenImages.show(player, status.id(), "knight_of_misfortune/" + status.image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, ticks);
        HudTimer.show(player, status.id(), status.label, ticks, status.rgb);
    }

    /** It ended early. */
    public static void statusEnd(Player player, Status status) {
        ScreenImages.hide(player, status.id());
        HudTimer.hide(player, status.id());
    }

    // =====================================================================================
    // The wheels' pictures (textures/gui/ability_menus/knight_of_misfortune/, for AbilityWheel.entry's icon)
    // =====================================================================================

    public static final String WHEEL_TRANQUILIZE = "ability_menus/knight_of_misfortune/tranquilize";
    public static final String WHEEL_LULLABY = "ability_menus/knight_of_misfortune/lullaby";
    public static final String WHEEL_PACIFY = "ability_menus/knight_of_misfortune/pacify";
    public static final String WHEEL_FEAR = "ability_menus/knight_of_misfortune/fear";
    public static final String WHEEL_AGITATING = "ability_menus/knight_of_misfortune/agitating";
    public static final String WHEEL_TERROR_BANSHEE = "ability_menus/knight_of_misfortune/terror_banshee";
    public static final String WHEEL_EARTHEN_SPIRIT = "ability_menus/knight_of_misfortune/earthen_spirit";
    public static final String WHEEL_GALE_SPIRIT = "ability_menus/knight_of_misfortune/gale_spirit";
    public static final String WHEEL_COMMAND_SPIRITS = "ability_menus/knight_of_misfortune/command_spirits";
    public static final String WHEEL_NIGHT_DOMAIN = "ability_menus/knight_of_misfortune/night_domain";
    public static final String WHEEL_HAIR_ENTANGLEMENT = "ability_menus/knight_of_misfortune/hair_entanglement";
    public static final String WHEEL_CURSE_OF_MISFORTUNE = "ability_menus/knight_of_misfortune/curse_of_misfortune";
    public static final String WHEEL_DEMONIC_WOLF = "ability_menus/knight_of_misfortune/demonic_wolf";
    public static final String WHEEL_SWORD_OF_DARKNESS = "ability_menus/knight_of_misfortune/sword_of_darkness";
    public static final String WHEEL_NIGHTMARE_WORLD = "ability_menus/knight_of_misfortune/nightmare_world";
    public static final String WHEEL_CONCEALED_STATE = "ability_menus/knight_of_misfortune/concealed_state";
    public static final String WHEEL_SECRET_CONCEALMENT = "ability_menus/knight_of_misfortune/secret_concealment";
    public static final String WHEEL_CONCEALED_WORLD = "ability_menus/knight_of_misfortune/concealed_world";
    public static final String WHEEL_MANTLE_OF_DARKNESS = "ability_menus/knight_of_misfortune/mantle_of_darkness";
    public static final String WHEEL_ETERNAL_REST = "ability_menus/knight_of_misfortune/eternal_rest";
    public static final String WHEEL_DESTINATION_OF_ALL_THINGS = "ability_menus/knight_of_misfortune/destination_of_all_things";

    // =====================================================================================
    // The models (models/model_fx/knight_of_misfortune/, see each JSON's "credit")
    // =====================================================================================

    /** The Sword of Darkness, 2.6 blocks long at scale 1, point up, held by the middle of its grip. */
    public static final ModelFx BONE_SWORD = ModelFx.of("knight_of_misfortune/bone_sword").pivot(8, -5, 8).glow()
            .aura(0xA05A2E8C, 0.1f, 3);
    /** A Nightmare Limb, 2 blocks at scale 1, four steps from straight (0) to curled (3); pivot = its root. */
    public static final ModelFx NIGHTMARE_LIMB = ModelFx.of("knight_of_misfortune/nightmare_limb").frames(4).smooth();
    /** The Demonic Wolf, 3 blocks long and 1.8 tall at scale 1, face = front; pivot between its feet. */
    public static final ModelFx DEMONIC_WOLF = ModelFx.of("knight_of_misfortune/demonic_wolf").pivot(8, -16, 11)
            .aura(0x805A2E8C, 0.12f, 2);
    /** The complete Demonic Wolf with six arms, same size and pivot. */
    public static final ModelFx DEMONIC_WOLF_COMPLETE = ModelFx.of("knight_of_misfortune/demonic_wolf_complete")
            .pivot(8, -16, 11).aura(0x805A2E8C, 0.12f, 2);
    /** The Dowsing pendulum, 1 block, hung from the top of its chain. */
    public static final ModelFx PENDULUM = ModelFx.of("knight_of_misfortune/pendulum").pivot(8, 16, 8).glow().seeThrough();
    /** The Terror Banshee, 2 blocks tall, face = front; pivot = the middle of its body. */
    public static final ModelFx BANSHEE = ModelFx.of("knight_of_misfortune/banshee").pivot(8, -8, 8).seeThrough().unshaded();
    /** A soul out of its body, 2 blocks tall, face = front; pivot between its feet. */
    public static final ModelFx SOUL_FIGURE = ModelFx.of("knight_of_misfortune/soul_figure").pivot(8, -16, 8)
            .seeThrough().unshaded();
    /** The seal of Darkness, 1 block, flat, white. */
    public static final ModelFx NIGHT_SEAL = ModelFx.of("knight_of_misfortune/night_seal").glow().unshaded().seeThrough();
    /** The script of a Midnight Poem, a ring 1 block wide, flat, white. */
    public static final ModelFx POEM_SCRIPT = ModelFx.of("knight_of_misfortune/poem_script").glow().unshaded().seeThrough();
    /** A dream: a blurry oval of light, 1 block, standing, white. */
    public static final ModelFx DREAM_OVAL = ModelFx.of("knight_of_misfortune/dream_oval").pivot(8, 8, 8).glow().unshaded()
            .seeThrough();
    /** Misfortune: a broken horseshoe, 1 block, standing, white. */
    public static final ModelFx MISFORTUNE_MARK = ModelFx.of("knight_of_misfortune/misfortune_mark").pivot(8, 8, 8).glow()
            .unshaded().seeThrough();
    /** Horror: a staring eye, 1 block, standing, white. */
    public static final ModelFx HORROR_EYE = ModelFx.of("knight_of_misfortune/horror_eye").pivot(8, 8, 8).glow().unshaded()
            .seeThrough();
    /** Secret Concealment: a veiled, closed eye, 1 block, flat, white. */
    public static final ModelFx SECRET_SEAL = ModelFx.of("knight_of_misfortune/secret_seal").glow().unshaded().seeThrough();
    /** Eternal Rest: a closed eye on a crescent, 1 block, flat, white. */
    public static final ModelFx REST_SIGIL = ModelFx.of("knight_of_misfortune/rest_sigil").glow().unshaded().seeThrough();
    /** A tile of fog, 1 block, standing, its own (dark) colors: no glow. Pivot = the middle of its bottom edge. */
    public static final ModelFx VEIL_WALL = ModelFx.of("knight_of_misfortune/veil_wall").seeThrough().unshaded();
    /** The front of the tide of Darkness, 1 block, standing, its own colors. */
    public static final ModelFx DARK_TIDE = ModelFx.of("knight_of_misfortune/dark_tide").seeThrough().unshaded();
    /** The Destination of All Things, a vortex 1 block wide, flat, its own colors. */
    public static final ModelFx VORTEX = ModelFx.of("knight_of_misfortune/vortex").seeThrough().unshaded();

    /** The seal of Darkness on the ground, 'radius' wide, turning, for 'ticks'. */
    public static void seal(ServerLevel sl, Vec3 ground, double radius, int color, int ticks, String tag) {
        ModelFx seal = NIGHT_SEAL.color(color).scale((float) (radius * 2.0)).lifetime(ticks).fade(0, 14).spin(0.6f)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(12, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        if (tag != null) seal = seal.tag(tag);
        ParticleShapes.model(sl, seal, ground.add(0, 0.05, 0), sl.getRandom().nextFloat() * 360f, 0f, 0f);
    }

    // =====================================================================================
    // A shell of fog (Nightmare World, Concealed World): VEIL_WALL tiles laid into a sphere, row by row.
    // Each tile is one model, so a sphere 8 blocks wide is about 70 of them: keep the radius sensible.
    // =====================================================================================

    /** A whole sphere of fog round 'center' (its middle), closing from the equator to both poles. */
    public static void veilShell(ServerLevel sl, Vec3 center, double radius, int color, int ticks, String tag) {
        double[] edges = {-80, -55, -28, 0, 28, 55, 80};
        float spin = sl.getRandom().nextFloat() * 360f;
        int equator = 3;
        for (int row = 0; row < edges.length - 1; row++) {
            double lo = Math.toRadians(edges[row]), hi = Math.toRadians(edges[row + 1]);
            double mid = (lo + hi) / 2;
            double wide = radius * Math.cos(Math.min(Math.abs(lo), Math.abs(hi)));
            int count = Math.max(6, Math.min(22, (int) Math.round(2 * Math.PI * radius * Math.cos(mid) / 1.8)));
            float width = (float) (2 * wide * Math.sin(Math.PI / count) * 1.1);
            float height = (float) (2 * radius * Math.sin((hi - lo) / 2) * 1.06);
            int delay = Math.abs(row - (row < equator ? equator - 1 : equator)) * 4;
            for (int k = 0; k < count; k++) {
                float azimuth = spin + k * 360f / count;
                Vec3 at = center.add(ahead(azimuth).scale(radius * Math.cos(lo))).add(0, radius * Math.sin(lo), 0);
                ModelFx tile = VEIL_WALL.color(color).scale(width, height, 1f).delay(delay).lifetime(ticks)
                        .fade(6, 14).key(0, pose().alpha(0f)).key(8, pose().alpha(1f), Ease.OUT);
                if (tag != null) tile = tile.tag(tag);
                ParticleShapes.model(sl, tile, at, azimuth + 180f, (float) Math.toDegrees(mid), 0f);
            }
        }
        double cap = radius * Math.cos(Math.toRadians(80)) * 2.4;
        double up = radius * Math.sin(Math.toRadians(80));
        for (int side = -1; side <= 1; side += 2) {
            ModelFx lid = NIGHT_SEAL.color(withAlpha(color, 0xC0)).scale((float) cap).delay(12).lifetime(ticks).fade(4, 12)
                    .spin(0.5f);
            if (tag != null) lid = lid.tag(tag);
            ParticleShapes.model(sl, lid, center.add(0, up * side, 0), 0f, 0f, 0f);
        }
    }

    // =====================================================================================
    // Key 1: Spirit Vision (Sleepless) / Dowsing
    // =====================================================================================

    /** The eyes open to spirits: a pale ring sweeps out, an eye flickers before the face. Only the viewer sees it. */
    public static void spiritVision(ServerPlayer viewer, int ticks) {
        Vec3 eyes = viewer.getEyePosition().add(viewer.getLookAngle().scale(0.6));
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24;
            Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
            ParticleShapes.spawnFor(viewer, MOON_MOTE, viewer.position().add(0, 0.3, 0).add(out.scale(0.6)), out.scale(0.5));
        }
        ParticleShapes.spawnFor(viewer, EYE, eyes, Vec3.ZERO);
        soundFor(viewer, ModSounds.KNIGHT_SPIRIT_VISION, 0.8f, 1.0f);
    }

    /** Something the vision shows: motes rise round it, only for the viewer (spirits and summons in violet). */
    public static void spiritVisionMark(ServerPlayer viewer, LivingEntity seen, boolean spirit) {
        ParticleFx mote = spirit ? DARK_WISP : MOON_MOTE;
        double r = widthOf(seen);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            ParticleShapes.spawnFor(viewer, mote, seen.position().add(Math.cos(a) * r, 0.2, Math.sin(a) * r),
                    new Vec3(0, 0.06, 0));
        }
        ParticleShapes.spawnFor(viewer, spirit ? EYE : STAR, headOf(seen).add(0, 0.3, 0), Vec3.ZERO);
    }

    /**
     * The pendulum swings in front of the caster and settles toward 'toward' (or swings on aimlessly when null); a thin
     * line of light points the way for 'ticks'.
     */
    public static void dowsing(ServerLevel sl, Player player, Vec3 toward, int ticks, String tag) {
        float lean = 0f;
        if (toward != null) {
            float diff = yawToward(player.position(), toward) - player.getYRot();
            while (diff > 180f) diff -= 360f;
            while (diff < -180f) diff += 360f;
            lean = Math.max(-35f, Math.min(35f, diff * 0.4f));
        }
        ModelFx swing = PENDULUM.scale(0.6f).lifetime(Math.min(ticks, 80)).fade(4, 10).tag(tag)
                .key(0, pose().roll(30f).alpha(0f))
                .key(8, pose().roll(-28f).alpha(1f), Ease.IN_OUT)
                .key(16, pose().roll(22f), Ease.IN_OUT)
                .key(24, pose().roll(-14f), Ease.IN_OUT)
                .key(34, pose().roll(-lean), Ease.IN_OUT);
        ParticleShapes.modelOnTurning(sl, swing, player, new Vec3(0, 1.45, 0), 0f, 0f, 0f);
        if (toward != null) {
            SlashFx line = SlashFx.line("slash/dashed_fine").color(0x90C8DCFF).core(0xC0FFFFFF)
                    .width(0.05f).taper(SlashFx.Taper.UNIFORM);
            Later.run(sl, 34, () -> SpellFx.tether(sl, line,
                    () -> player.isAlive() ? chestOf(player) : null,
                    () -> chestOf(player).add(toward.subtract(chestOf(player)).normalize().scale(6)), Math.max(1, ticks - 34)));
        }
        sound(sl, player.position(), ModSounds.KNIGHT_DOWSING, 0.8f, 1.0f);
    }

    public static void dowsingEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
    }

    // =====================================================================================
    // Key 2: the Midnight Poem wheel (Midnight Poet)
    // =====================================================================================

    /** The tick a poem reaches the ones it is spoken to. */
    public static final int POEM_LANDS = 10;

    /** Every poem: the script of the poem turns round the head and notes drift off; a whisper. */
    public static void recite(ServerLevel sl, Player player, Poem poem, String tag) {
        ModelFx script = POEM_SCRIPT.color(withAlpha(poem.color, 0xD0)).scale(1.4f).lifetime(30).fade(2, 10).spin(4f)
                .key(0, pose().scale(0.4f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        if (tag != null) script = script.tag(tag);
        ParticleShapes.modelOnHead(sl, script, player, -0.2, 0f, 0f, 0f);
        ParticleShapes.ring(sl, NOTE.color(poem.color), player.getEyePosition(), 0.6, 6, 0.04);
        sound(sl, player.position(), ModSounds.KNIGHT_POEM, 0.9f, wobble(1.0f));
    }

    /** Tranquilize: a pale ring of stillness spreads; the air goes quiet. */
    public static void tranquilize(ServerLevel sl, Player player, double radius) {
        SpellFx.steadyRing(sl, player.position(), radius, 0xA0C8DCFF, 14, 4);
        SpellFx.risingRings(sl, player.position(), 1.0, 2.0, 3, 0x80C8DCFF);
        ParticleShapes.ring(sl, MOON_MOTE, player.position().add(0, 0.3, 0), 1.2, 20, 0.2);
        sound(sl, player.position(), ModSounds.KNIGHT_TRANQUILIZE, 1.0f, 1.0f);
    }

    /** An ally calmed: a soft ring rises round them. */
    public static void tranquilized(ServerLevel sl, LivingEntity ally) {
        SpellFx.risingRings(sl, ally.position(), widthOf(ally), ally.getBbHeight(), 2, 0x80C8DCFF);
        ParticleShapes.burst(sl, MOON_MOTE, chestOf(ally), 4, 0.02, 0.05);
    }

    /** The one Tranquilize was aimed at: what is wrong with it shows (an oval of its dream over its head). */
    public static void tranquilizeReveal(ServerLevel sl, LivingEntity target) {
        ModelFx oval = DREAM_OVAL.color(0xC0C8DCFF).scale(0.7f).lifetime(60).fade(6, 14)
                .key(0, pose().scale(0.2f).alpha(0f)).key(8, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, oval, target, new Vec3(0, target.getBbHeight() + 0.7, 0), 0f, 0f, 0f);
    }

    /** Lullaby: a cone of notes and sleep drifts out 'range' blocks ahead. */
    public static void lullaby(ServerLevel sl, Player player, double range) {
        Vec3 look = Spells.flatLook(player);
        ParticleShapes.cone(sl, NOTE.color(0xFFA8C0FF), player.getEyePosition(), look, 35, 18, 0.15, range / 20.0);
        ParticleShapes.cone(sl, SLEEP_Z, player.getEyePosition(), look, 30, 8, 0.1, range / 24.0);
        ParticleShapes.cone(sl, MOON_MOTE, chestOf(player), look, 40, 20, 0.2, range / 14.0);
        sound(sl, player.position(), ModSounds.KNIGHT_LULLABY, 1.0f, 1.0f);
    }

    /** Someone asleep (Lullaby, Hair Entanglement, Slumber): z's rise from their head and a pale ring lies under them. */
    public static void slumber(ServerLevel sl, LivingEntity sleeper, int ticks, String tag) {
        SpellFx.circleUnder(sl, sleeper, widthOf(sleeper), 0x80A8C0FF, ticks, tag);
        for (int t = 0; t < Math.min(ticks, 400); t += 20) {
            Later.run(sl, t, () -> {
                if (sleeper.isAlive()) ParticleShapes.spawn(sl, SLEEP_Z, headOf(sleeper), new Vec3(0.01, 0.03, 0));
            });
        }
    }

    /** They wake (hurt, or the time is up). */
    public static void wake(ServerLevel sl, LivingEntity sleeper, String tag) {
        ParticleShapes.clearModels(sl, sleeper, tag);
        ParticleShapes.burst(sl, MOON_MOTE, headOf(sleeper), 6, 0.04, 0.1);
    }

    /** Pacify: notes fly into the target; its will goes out of it and a grey ring weighs it down. */
    public static void pacify(ServerLevel sl, Player player, LivingEntity target, int ticks, String tag) {
        ParticleShapes.line(sl, NOTE.color(0xFFB4B4D8), player.getEyePosition(), chestOf(target), 2);
        Later.run(sl, POEM_LANDS, () -> {
            if (!target.isAlive()) return;
            SpellFx.circleUnder(sl, target, widthOf(target), 0x90B4B4D8, ticks, tag);
            SpellFx.closingRing(sl, target.position(), 2.0, 0xA0B4B4D8, 10, 0);
            ParticleShapes.burst(sl, DARK_SMOKE.color(0x80505070), chestOf(target), 8, 0.01, 0.04);
        });
        sound(sl, player.position(), ModSounds.KNIGHT_PACIFY, 1.0f, 1.0f);
    }

    /** Fear: a red-black wave of dread rolls out 'range' blocks ahead. */
    public static void fear(ServerLevel sl, Player player, double range) {
        Vec3 look = Spells.flatLook(player);
        ParticleShapes.cone(sl, DARK_SMOKE.color(0xC0300818), chestOf(player), look, 40, 26, 0.15, range / 14.0);
        ParticleShapes.cone(sl, EYE.color(0xFFFF5A6A), player.getEyePosition(), look, 30, 8, 0.15, range / 18.0);
        flashEye(sl, player.getEyePosition().add(look.scale(1.2)), player.getYRot() + 180f, 0xC0C02838, 1.4f);
        sound(sl, player.position(), ModSounds.KNIGHT_FEAR, 1.0f, 1.0f);
    }

    /** Someone afraid: a staring eye over their head, and they tremble (shaky motes). */
    public static void afraid(ServerLevel sl, LivingEntity victim, int ticks, String tag) {
        ModelFx eye = HORROR_EYE.color(0xC0C02838).scale(0.55f).lifetime(ticks).fade(4, 10).tag(tag)
                .key(0, pose().scale(0.2f).alpha(0f)).key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.clearModels(sl, victim, tag);
        ParticleShapes.modelOn(sl, eye, victim, new Vec3(0, victim.getBbHeight() + 0.6, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, DARK_SMOKE.color(0x90300818), chestOf(victim), 6, 0.02, 0.06);
    }

    /** Agitating: the target's spirit is stirred: jagged red-violet sparks crackle round it. */
    public static void agitate(ServerLevel sl, Player player, LivingEntity target, int ticks, String tag) {
        ParticleShapes.line(sl, NOTE.color(0xFFE04A9A), player.getEyePosition(), chestOf(target), 2);
        SpellFx.circleUnder(sl, target, widthOf(target), 0xB0E04A9A, ticks, tag);
        for (int t = POEM_LANDS; t < Math.min(ticks, 300); t += 10) {
            Later.run(sl, t, () -> {
                if (target.isAlive()) ParticleShapes.burst(sl, SPARK.color(0xFFFF7AB0), chestOf(target), 3, 0.05, 0.15);
            });
        }
        sound(sl, target.position(), ModSounds.KNIGHT_AGITATE, 1.0f, 1.0f);
    }

    /** A staring eye that flashes and fades at a spot (Fear, Horror). */
    private static void flashEye(ServerLevel sl, Vec3 at, float yaw, int color, float size) {
        ModelFx eye = HORROR_EYE.color(color).scale(size).lifetime(14).fade(0, 8)
                .key(0, pose().scale(0.3f)).key(4, pose().scale(1.1f), Ease.OUT).key(14, pose().scale(1.3f).alpha(0f));
        ParticleShapes.model(sl, eye, at, yaw, 0f, 0f);
    }

    // =====================================================================================
    // Key 3: Nightmare Limbs (Nightmare) / Nightmare State, Dream Invasion and Dream Shaping
    // =====================================================================================

    /** The tick the limbs have grown out. */
    public static final int LIMBS_GROWN = 10;

    /** Four tentacles tear out of the caster's back and stand writhing behind the shoulders for 'ticks'. */
    public static void nightmareLimbs(ServerLevel sl, Player player, int ticks, String tag) {
        float[][] limbs = {{-30f, -0.25f}, {30f, 0.25f}, {-60f, -0.2f}, {60f, 0.2f}};     // roll outward, side offset
        for (int i = 0; i < limbs.length; i++) {
            float roll = limbs[i][0];
            float side = limbs[i][1];
            int phase = i * 7;
            ModelFx limb = NIGHTMARE_LIMB.scale(0.55f).lifetime(ticks).fade(2, 8).tag(tag)
                    .key(0, pose().scale(0.05f).frame(3f).roll(roll).pitch(30f))
                    .key(LIMBS_GROWN, pose().scale(1f).frame(1f).roll(roll).pitch(30f), Ease.OUT_BACK);
            for (int t = LIMBS_GROWN + 10 + phase; t < ticks - 10; t += 30) {                 // slow writhing
                limb = limb.key(t, pose().frame(2f).roll(roll * 1.15f), Ease.IN_OUT)
                        .key(t + 15, pose().frame(1f).roll(roll * 0.9f), Ease.IN_OUT);
            }
            ParticleShapes.modelOnTurning(sl, limb, player, new Vec3(side, 1.3, -0.2), 0f, 0f, 0f);
        }
        ParticleShapes.burst(sl, BLOOD, chestOf(player).subtract(Spells.flatLook(player).scale(0.3)), 12, 0.05, 0.15);
        sound(sl, player.position(), ModSounds.KNIGHT_LIMBS, 1.0f, wobble(1.0f));
    }

    private static final SlashFx LASH = SlashFx.line("slash/smooth")
            .color(0xE03A1A4A).core(0xC08A5AA0).width(0.22f).taper(SlashFx.Taper.COMET).translucent();

    /** One limb lashes 'target' (index 0..3 picks the shoulder): a whip of flesh from the back to it. */
    public static void limbLash(ServerLevel sl, Player player, LivingEntity target, int index) {
        Vec3 from = player.position().add(0, 1.5, 0).add(Spells.rightOf(player).scale(index % 2 == 0 ? -0.4 : 0.4))
                .subtract(Spells.flatLook(player).scale(0.2));
        ParticleShapes.slashBetween(sl, LASH, from, chestOf(target));
        ParticleShapes.burst(sl, BLOOD, chestOf(target), 5, 0.04, 0.12);
        sound(sl, chestOf(target), ModSounds.KNIGHT_LASH, 0.9f, wobble(1.0f));
    }

    /** The limbs are torn off: a burst of dark blood and the backlash. */
    public static void limbsTear(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        Vec3 back = chestOf(player).subtract(Spells.flatLook(player).scale(0.3)).add(0, 0.3, 0);
        ParticleShapes.burst(sl, BLOOD, back, 24, 0.08, 0.3);
        ParticleShapes.burst(sl, DARK_SMOKE, back, 8, 0.02, 0.08);
        sound(sl, player.position(), ModSounds.KNIGHT_LIMBS_TEAR, 1.0f, 1.0f);
    }

    // ---------- sneak: Nightmare State

    /** The tick the soul is out. */
    public static final int SOUL_OUT = 16;

    /**
     * The soul steps out: a pale figure rises out of the caster (who goes hidden: the ability's part) and goes with
     * them, seen while they are hidden. Show the body left behind with sleepingBody.
     */
    public static void nightmareState(ServerLevel sl, Player player, int ticks, String tag) {
        ModelFx soul = SOUL_FIGURE.color(0xC0C8DCFF).scale(0.95f).lifetime(ticks).fade(6, 12).tag(tag).shownWhileHidden()
                .key(0, pose().up(-0.6f).alpha(0f))
                .key(SOUL_OUT, pose().up(0.15f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOnTurning(sl, soul, player, Vec3.ZERO, 0f, 0f, 0f);
        SpellFx.risingRings(sl, player.position(), 0.6, 2.0, 4, 0xA0C8DCFF);
        ParticleShapes.spiral(sl, WISP, player.position().add(0, 0.2, 0), 2, 20, 0.3, 0.8, 1.2, 0.05);
        sound(sl, player.position(), ModSounds.KNIGHT_SOUL_OUT, 1.0f, 1.0f);
    }

    /** The body left behind: a still, dark figure lying where the caster stood, for 'ticks'. */
    public static void sleepingBody(ServerLevel sl, Vec3 at, float yaw, int ticks, String tag) {
        ModelFx body = SOUL_FIGURE.color(0xE03A2A4A).lifetime(ticks).fade(4, 8).key(0, pose().pitch(-90f).up(0.3f));
        if (tag != null) body = body.tag(tag);
        ParticleShapes.model(sl, body, at, yaw, 0f, 0f);
        SpellFx.warningCircle(sl, at, 1.0, 0x80A8C0FF, Math.min(ticks, 600));
    }

    /** While the soul drifts: wisps trail from it. */
    public static void nightmareTick(ServerLevel sl, Player player) {
        ParticleShapes.spawn(sl, WISP, player.position().add(0, 1.0, 0), new Vec3(0, 0.02, 0));
    }

    /** The soul goes back into its body. */
    public static void nightmareReturn(ServerLevel sl, Player player, Vec3 body, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.clearModels(sl, body, tag);
        ParticleShapes.line(sl, WISP, chestOf(player), body.add(0, 0.5, 0), 3);
        ParticleShapes.burst(sl, MOON_MOTE, body.add(0, 0.6, 0), 12, 0.03, 0.1);
        sound(sl, body, ModSounds.KNIGHT_SOUL_RETURN, 1.0f, 1.0f);
    }

    /** The tick the dream is entered. */
    public static final int DREAM_OPENS = 12;

    /** Dream Invasion: a thread of pale light to the sleeper and the oval of its dream over its head. */
    public static void dreamInvade(ServerLevel sl, Player player, LivingEntity sleeper, int ticks, String tag) {
        SlashFx thread = SlashFx.line("slash/dashed").color(0x90A8C0FF).core(0xC0FFFFFF)
                .width(0.06f).taper(SlashFx.Taper.UNIFORM);
        SpellFx.tether(sl, thread, () -> player.isAlive() ? chestOf(player) : null,
                () -> sleeper.isAlive() ? headOf(sleeper) : null, ticks);
        ModelFx oval = DREAM_OVAL.color(0xD0A8C0FF).scale(1.2f).lifetime(ticks).fade(6, 14).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(DREAM_OPENS, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, oval, sleeper, new Vec3(0, sleeper.getBbHeight() + 0.9, 0), 0f, 0f, 0f);
        sound(sl, sleeper.position(), ModSounds.KNIGHT_DREAM_ENTER, 0.9f, 1.0f);
    }

    /** Dream Shaping: the dream turns the color of what is made of it, and bends. */
    public static void dreamShape(ServerLevel sl, LivingEntity sleeper, Dream dream) {
        ModelFx bent = DREAM_OVAL.color(withAlpha(dream.color, 0xD0)).scale(1.3f).lifetime(30).fade(2, 12).spin(6f)
                .key(0, pose().scale(1f)).key(10, pose().scale(1.5f), Ease.OUT).key(30, pose().scale(0.6f).alpha(0f));
        ParticleShapes.modelOn(sl, bent, sleeper, new Vec3(0, sleeper.getBbHeight() + 0.9, 0), 0f, 0f, 0f);
        ParticleFx mote = dream == Dream.TERROR ? EYE.color(0xFFFF5A6A) : dream == Dream.OBEDIENCE ? DARK_WISP : STAR;
        ParticleShapes.burst(sl, mote, headOf(sleeper).add(0, 0.8, 0), 10, 0.03, 0.1);
        sound(sl, sleeper.position(), ModSounds.KNIGHT_DREAM_SHAPE, 0.9f, dream == Dream.TERROR ? 0.8f : 1.0f);
    }

    public static void dreamEnd(ServerLevel sl, LivingEntity sleeper, String tag) {
        ParticleShapes.clearModels(sl, sleeper, tag);
        ParticleShapes.burst(sl, MOON_MOTE, headOf(sleeper).add(0, 0.8, 0), 8, 0.04, 0.1);
    }

    // =====================================================================================
    // Key 4: Requiem (Soul Assurer) / Soothing Requiem
    // =====================================================================================

    /** The tick the requiem is fully sung: the field acts from here on. */
    public static final int REQUIEM_RISEN = 14;

    /** A pale seal of Darkness spreads 'radius' wide; notes and stars rise from it while it lasts. */
    public static void requiem(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        seal(sl, center, radius, 0xB0C8DCFF, ticks, tag);
        SpellFx.steadyRing(sl, center, radius, 0xA0C8DCFF, REQUIEM_RISEN, 0);
        SpellFx.risingRings(sl, center, 1.2, 3.0, 4, 0x90A8C0FF);
        Later.run(sl, REQUIEM_RISEN, () -> ParticleShapes.ring(sl, NOTE, center.add(0, 0.4, 0), radius * 0.8, 16, 0.02));
        sound(sl, center, ModSounds.KNIGHT_REQUIEM, 1.2f, 1.0f);
    }

    /** Every second while it lasts: notes and stars drift up over the field. */
    public static void requiemPulse(ServerLevel sl, Vec3 center, double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 8; i++) {
            double a = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            Vec3 at = center.add(Math.cos(a) * far, 0.3, Math.sin(a) * far);
            ParticleShapes.spawn(sl, i % 2 == 0 ? NOTE : STAR, at, new Vec3(0, 0.05, 0));
        }
    }

    /** An enemy in the requiem, 'depth' 0..1 (1 = frozen still): pale frost-like motes settle on it, more as it slows. */
    public static void requiemOn(ServerLevel sl, LivingEntity enemy, float depth) {
        int count = Math.round(1 + depth * 6);
        ParticleShapes.burst(sl, MOON_MOTE, chestOf(enemy), count, 0.01, 0.03);
        if (depth >= 1f) ParticleShapes.spawn(sl, SLEEP_Z, headOf(enemy), new Vec3(0, 0.02, 0));
    }

    public static void requiemEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.ring(sl, MOON_MOTE, center.add(0, 0.3, 0), 2.0, 16, 0.1);
    }

    /** Soothing Requiem: a warm silver wave spreads over the caster's allies. */
    public static void soothingRequiem(ServerLevel sl, Player player, double radius) {
        SpellFx.steadyRing(sl, player.position(), radius, 0xA0E0E8FF, 12, 0);
        ParticleShapes.ring(sl, NOTE.color(0xFFE0E8FF), player.position().add(0, 1.0, 0), 1.0, 10, 0.12);
        ParticleShapes.burst(sl, STAR, chestOf(player), 12, 0.04, 0.12);
        sound(sl, player.position(), ModSounds.KNIGHT_SOOTHE, 1.0f, 1.0f);
    }

    /** An ally soothed: a soft ring and stars rise round them. */
    public static void soothed(ServerLevel sl, LivingEntity ally) {
        SpellFx.risingRings(sl, ally.position(), widthOf(ally), ally.getBbHeight(), 2, 0x90E0E8FF);
        ParticleShapes.burst(sl, STAR, chestOf(ally), 5, 0.02, 0.06);
    }

    // =====================================================================================
    // Key 5: the Spirit Commanding wheel (Spirit Warlock, Knight of Misfortune)
    // =====================================================================================

    /** A spirit leaves the caster's teeth: a spark of teeth at the mouth and a wisp flying to 'at'. */
    public static void spiritRelease(ServerLevel sl, Player player, Vec3 at) {
        Vec3 mouth = player.getEyePosition().add(player.getLookAngle().scale(0.4)).add(0, -0.2, 0);
        ParticleShapes.burst(sl, TOOTH, mouth, 4, 0.04, 0.08);
        ParticleShapes.line(sl, WISP, mouth, at, 3);
        sound(sl, mouth, ModSounds.KNIGHT_TEETH, 0.8f, wobble(1.0f));
    }

    // ---------- Terror Banshee

    /** The banshee, on the (invisible) body the ability summons for it, floating and turning with it for 'ticks'. */
    public static void banshee(ServerLevel sl, Entity body, int ticks, String tag) {
        ModelFx spirit = BANSHEE.scale(0.9f).lifetime(ticks).fade(10, 16).tag(tag)
                .key(0, pose().up(-0.6f).alpha(0f))
                .key(14, pose().up(0.6f).alpha(1f), Ease.OUT);
        for (int t = 14; t < ticks - 20; t += 40) {
            spirit = spirit.key(t + 20, pose().up(0.85f), Ease.IN_OUT).key(t + 40, pose().up(0.6f), Ease.IN_OUT);
        }
        ParticleShapes.modelOnTurning(sl, spirit, body, Vec3.ZERO, 0f, 0f, 0f);
        ParticleShapes.burst(sl, WISP, body.position().add(0, 1.0, 0), 12, 0.03, 0.1);
    }

    /** The banshee wails: a cold ring rolls out 'radius' blocks and the air shudders. */
    public static void bansheeWail(ServerLevel sl, Entity body, double radius) {
        Vec3 at = body.position();
        SpellFx.steadyRing(sl, at, radius, 0xA0B4D4E8, 10, 0);
        SpellFx.shockRing(sl, at.add(0, 1.4, 0), radius * 0.4, 0x80E0F0FF, 8, 0);
        ParticleShapes.ring(sl, WISP, at.add(0, 1.4, 0), 0.6, 12, 0.35);
        sound(sl, at, ModSounds.KNIGHT_BANSHEE, 1.4f, wobble(1.0f));
    }

    public static void bansheeEnd(ServerLevel sl, Entity body, String tag) {
        ParticleShapes.clearModels(sl, body, tag);
        ParticleShapes.burst(sl, WISP, body.position().add(0, 1.0, 0), 16, 0.04, 0.12);
    }

    // ---------- Earthen Spirit

    /** The tick the caster has sunk into the earth. */
    public static final int EARTH_SUNK = 8;

    /** The caster fuses with the earth: a ring of disturbed ground and a mound that goes where they go (seen while hidden). */
    public static void earthMerge(ServerLevel sl, Player player, int ticks, String tag) {
        ParticleShapes.ring(sl, DUST, player.position().add(0, 0.1, 0), 0.8, 16, 0.12);
        SpellFx.closingRing(sl, player.position(), 1.6, 0xA0AA8256, EARTH_SUNK, 0);
        ModelFx mound = SOUL_FIGURE.color(0x90AA8256).scale(0.5f).lifetime(ticks).fade(2, 6).tag(tag).shownWhileHidden()
                .key(0, pose().up(0f)).key(EARTH_SUNK, pose().up(-0.8f), Ease.IN);
        ParticleShapes.modelOnTurning(sl, mound, player, Vec3.ZERO, 0f, 0f, 0f);
        sound(sl, player.position(), ModSounds.KNIGHT_EARTH, 1.0f, 1.0f);
    }

    /** While under the ground: earth churns where the caster passes. */
    public static void earthTick(ServerLevel sl, Player player) {
        ParticleShapes.spawn(sl, DUST, player.position().add(0, 0.1, 0), new Vec3(0, 0.03, 0));
    }

    /** The caster rises out of the earth. */
    public static void earthEmerge(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.ring(sl, DUST, player.position().add(0, 0.1, 0), 0.6, 16, 0.18);
        SpellFx.shockRing(sl, player.position(), 2.2, 0xA0AA8256, 8, 0);
        sound(sl, player.position(), ModSounds.KNIGHT_EARTH, 0.8f, 1.3f);
    }

    // ---------- Gale Spirit

    private static final SlashFx GUST = SlashFx.arc("slash/streaks")
            .color(0x80C8E0FF).core(0xA0FFFFFF).radius(1.2f).arc(160f).width(0.25f).lifetime(8).sweep(3);

    /** A wind spirit lifts the caster: gusts circle them and a spirit's shape hangs over their shoulders. */
    public static void gale(ServerLevel sl, Player player, int ticks, String tag) {
        ModelFx spirit = SOUL_FIGURE.color(0x70C8E0FF).scale(1.15f).lifetime(ticks).fade(8, 12).tag(tag)
                .key(0, pose().up(0.3f).alpha(0f)).key(10, pose().up(0.1f).forward(-0.25f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOnTurning(sl, spirit, player, Vec3.ZERO, 0f, 0f, 0f);
        ParticleShapes.ring(sl, WIND, player.position().add(0, 0.2, 0), 1.2, 20, 0.3);
        SpellFx.shockRing(sl, player.position(), 2.6, 0x80C8E0FF, 8, 0);
        sound(sl, player.position(), ModSounds.KNIGHT_GALE, 1.0f, 1.0f);
    }

    /** Every few ticks of the flight: a gust swirls round the body. */
    public static void galeTick(ServerLevel sl, Player player) {
        ParticleShapes.slash(sl, GUST, chestOf(player), sl.getRandom().nextFloat() * 360f, 0f,
                sl.getRandom().nextFloat() * 40f - 20f);
        ParticleShapes.spawn(sl, WIND, player.position(), Spells.flatLook(player).scale(-0.2));
    }

    public static void galeEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.burst(sl, WIND, chestOf(player), 10, 0.1, 0.3);
    }

    // ---------- Command Spirits (Knight of Misfortune)

    /** The command: a dark choir; spirits rise all round the caster, 'radius' blocks. */
    public static void commandSpirits(ServerLevel sl, Player player, double radius) {
        seal(sl, player.position(), 3.0, 0xC0B4D4E8, 60, null);
        SpellFx.steadyRing(sl, player.position(), radius, 0x90B4D4E8, 20, 4);
        ParticleShapes.spiral(sl, WISP, player.position().add(0, 0.3, 0), 6, 48, 1.0, 4.0, 1.5, 0.08);
        sound(sl, player.position(), ModSounds.KNIGHT_COMMAND_SPIRITS, 1.4f, 1.0f);
    }

    /** A creature that now serves: its spirit, pale, stands over it a moment, and a cold ring stays under it. */
    public static void spiritBound(ServerLevel sl, LivingEntity creature, int ticks, String tag) {
        SpellFx.circleUnder(sl, creature, widthOf(creature), 0xA0B4D4E8, ticks, tag);
        ModelFx soul = SOUL_FIGURE.color(0x90B4D4E8).scale((float) Math.max(0.5, creature.getBbHeight() / 2.0))
                .lifetime(24).fade(4, 14).key(0, pose().up(0f)).key(24, pose().up(1.0f), Ease.OUT);
        ParticleShapes.model(sl, soul, creature.position(), creature.getYRot(), 0f, 0f);
    }

    // =====================================================================================
    // Key 6: the Nightwatcher wheel (Nightwatcher)
    // =====================================================================================

    /** The tick the night has spread over the whole domain. */
    public static final int DOMAIN_RISEN = 20;

    /** A tide of pure Darkness spreads from the caster: a dark seal 'radius' wide and fog rolling over it. */
    public static void nightDomain(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        ModelFx seal = NIGHT_SEAL.color(0xE05A2E8C).scale((float) (radius * 2.0)).lifetime(ticks).fade(0, 20).spin(0.3f)
                .key(0, pose().scale(0.05f).alpha(0f))
                .key(DOMAIN_RISEN, pose().scale(1f).alpha(1f), Ease.OUT);
        if (tag != null) seal = seal.tag(tag);
        ParticleShapes.model(sl, seal, center.add(0, 0.05, 0), 0f, 0f, 0f);
        SpellFx.steadyRing(sl, center, radius, 0xC03A1A5A, DOMAIN_RISEN, 0);
        for (int t = 0; t < DOMAIN_RISEN; t += 4) {
            double r = radius * t / DOMAIN_RISEN;
            Later.run(sl, t, () -> ParticleShapes.ring(sl, DARK_SMOKE, center.add(0, 0.3, 0), Math.max(0.5, r), 18, 0.05));
        }
        sound(sl, center, ModSounds.KNIGHT_NIGHT_DOMAIN, 1.5f, 1.0f);
    }

    /** Every second while the domain lasts: fog drifts and stars glint over it. */
    public static void domainPulse(ServerLevel sl, Vec3 center, double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 12; i++) {
            double a = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            Vec3 at = center.add(Math.cos(a) * far, 0.2 + random.nextDouble() * 2.5, Math.sin(a) * far);
            ParticleShapes.spawn(sl, i % 3 == 0 ? STAR : FOG, at, new Vec3(0, 0.01, 0));
        }
    }

    /** An enemy in the domain is corroded: dark smoke peels off it. */
    public static void domainCorrode(ServerLevel sl, LivingEntity enemy) {
        ParticleShapes.burst(sl, DARK_SMOKE, chestOf(enemy), 3, 0.01, 0.04);
        ParticleShapes.burst(sl, SPARK.color(0xFFB48AFF), chestOf(enemy), 2, 0.03, 0.08);
    }

    public static void domainEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.ring(sl, DARK_SMOKE, center.add(0, 0.4, 0), 3.0, 24, 0.2);
    }

    // ---------- Hair Entanglement

    /** The tick the hairs have wrapped what they caught. */
    public static final int HAIR_BINDS = 6;

    private static final SlashFx HAIR_LINE = SlashFx.line("slash/smooth")
            .color(0xC0D8D8E8).core(0xE0FFFFFF).width(0.04f).taper(SlashFx.Taper.UNIFORM);

    /** Countless hairs lash out of the caster to everything 'caught'. */
    public static void hairEntangle(ServerLevel sl, Player player, List<LivingEntity> caught) {
        Vec3 from = chestOf(player);
        ParticleShapes.cone(sl, HAIR, from, Spells.flatLook(player), 35, 30, 0.2, 0.5);
        for (LivingEntity victim : caught) {
            for (int i = 0; i < 3; i++) {
                double off = (i - 1) * 0.15;
                ParticleShapes.slashBetween(sl, HAIR_LINE, from.add(0, off, 0), chestOf(victim).add(0, off * 2, 0));
            }
        }
        sound(sl, player.position(), ModSounds.KNIGHT_HAIR, 1.0f, 1.0f);
    }

    /** Someone bound by hair: strands wound round them and a cold ring under them, for 'ticks'. */
    public static void hairBind(ServerLevel sl, LivingEntity victim, int ticks, String tag) {
        SpellFx.circleUnder(sl, victim, widthOf(victim), 0x90D8D8E8, ticks, tag);
        SpellFx.risingRings(sl, victim.position(), widthOf(victim) * 0.8, victim.getBbHeight(), 4, 0xC0E0E0EC);
        for (int t = 0; t < Math.min(ticks, 200); t += 8) {
            Later.run(sl, t, () -> {
                if (victim.isAlive()) ParticleShapes.spiral(sl, HAIR, victim.position().add(0, 0.2, 0), 2, 6,
                        widthOf(victim) * 0.7, widthOf(victim) * 0.7, 0.8, 0.02);
            });
        }
    }

    // ---------- Curse of Misfortune, Misfortune (Sequence 1), Catastrophe (Sequence 0)

    /** The tick the curse lands on its target. */
    public static final int CURSE_LANDS = 10;

    /** The curse flies: a broken horseshoe homes into the target. Show its mark with misfortune(...) at CURSE_LANDS. */
    public static void curse(ServerLevel sl, Player player, LivingEntity target) {
        ModelFx omen = MISFORTUNE_MARK.color(0xE0B8A25A).scale(0.6f).lifetime(CURSE_LANDS + 4).fade(2, 4).spin(10f);
        SpellFx.homing(sl, omen, player.getEyePosition(), target, CURSE_LANDS);
        ParticleShapes.line(sl, HORSESHOE, player.getEyePosition(), chestOf(target), 1);
        sound(sl, player.position(), ModSounds.KNIGHT_CURSE, 1.0f, 1.0f);
    }

    /** Someone carries 'level' (1..5) Misfortune for 'ticks': the mark over their head, larger and redder with it. */
    public static void misfortune(ServerLevel sl, LivingEntity target, int level, int ticks, String tag) {
        int lv = Math.max(1, Math.min(5, level));
        int[] colors = {0xC0B8A25A, 0xD0C89A4A, 0xD0C87A3A, 0xE0C04A2A, 0xF0B01A1A};
        ModelFx mark = MISFORTUNE_MARK.color(colors[lv - 1]).scale(0.4f + lv * 0.1f).lifetime(ticks).fade(4, 10).tag(tag)
                .key(0, pose().scale(0.3f).alpha(0f)).key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.7, 0), 0f, 0f, 0f);
        SpellFx.circleUnder(sl, target, widthOf(target), colors[lv - 1], ticks, tag);
        ParticleShapes.burst(sl, HORSESHOE, headOf(target), lv * 2, 0.02, 0.06);
    }

    /** Misfortune on an area (Sequence 1): a tarnished seal 'radius' wide, motes of luck falling out of the air. */
    public static void misfortuneArea(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        seal(sl, center, radius, 0xB0B8A25A, ticks, tag);
        SpellFx.steadyRing(sl, center, radius, 0xC0B8A25A, 14, 0);
        for (int t = 0; t < Math.min(ticks, 400); t += 10) {
            Later.run(sl, t, () -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                double a = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
                ParticleShapes.spawn(sl, HORSESHOE, center.add(Math.cos(a) * far, 3.5, Math.sin(a) * far), new Vec3(0, -0.05, 0));
            });
        }
        sound(sl, center, ModSounds.KNIGHT_CURSE, 1.4f, 0.8f);
    }

    /** A Misfortune befalls someone. CALAMITY is the lightning of Catastrophe (no fire: the ability deals the damage). */
    public static void mishap(ServerLevel sl, LivingEntity victim, Mishap mishap) {
        Vec3 chest = chestOf(victim);
        switch (mishap) {
            case STUMBLE -> {
                ParticleShapes.ring(sl, DUST, victim.position().add(0, 0.1, 0), 0.5, 8, 0.1);
                sound(sl, victim.position(), ModSounds.KNIGHT_MISHAP, 0.8f, wobble(1.0f));
            }
            case FUMBLE -> {
                ParticleShapes.burst(sl, SPARK.color(0xFFD8C27A), chest, 8, 0.05, 0.15);
                ParticleShapes.burst(sl, HORSESHOE, chest, 3, 0.04, 0.1);
                sound(sl, chest, ModSounds.KNIGHT_MISHAP, 0.8f, wobble(1.2f));
            }
            case BACKFIRE -> {
                ParticleShapes.burst(sl, HORSESHOE, headOf(victim), 4, 0.04, 0.1);
                sound(sl, chest, ModSounds.KNIGHT_MISHAP, 0.6f, wobble(1.4f));
            }
            case FALLING_STONE -> {
                Vec3 above = victim.position().add(0, victim.getBbHeight() + 4, 0);
                ParticleShapes.line(sl, DUST, above, headOf(victim), 2);
                ParticleShapes.burst(sl, DUST, headOf(victim), 10, 0.05, 0.15);
                play(sl, victim.position(), SoundEvents.STONE_BREAK, 1.0f, wobble(0.8f));
                sound(sl, chest, ModSounds.KNIGHT_MISHAP, 0.8f, wobble(0.8f));
            }
            case CALAMITY -> catastrophe(sl, victim);
        }
    }

    private static final SlashFx BOLT = SlashFx.line("slash/streaks")
            .color(0xE0D2B4FF).core(0xFFFFFFFF).width(0.35f).taper(SlashFx.Taper.UNIFORM);

    /** Catastrophe: a jagged bolt out of a black sky onto the victim, and a dark blast. */
    public static void catastrophe(ServerLevel sl, LivingEntity victim) {
        Vec3 ground = victim.position();
        Vec3 last = ground.add(0, 16, 0);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 1; i <= 5; i++) {
            Vec3 next = i == 5 ? chestOf(victim) : ground.add(random.nextDouble() * 1.6 - 0.8, 16 - i * 3.2, random.nextDouble() * 1.6 - 0.8);
            ParticleShapes.slashBetween(sl, BOLT, last, next);
            last = next;
        }
        SpellFx.blast(sl, chestOf(victim), 2.5, 0xE05A2E8C, SPARK);
        SpellFx.shockRing(sl, ground, 4.0, 0xC0B8A25A, 10, 0);
        ParticleShapes.burst(sl, DARK_SMOKE, ground.add(0, 0.5, 0), 12, 0.05, 0.15);
        sound(sl, ground, ModSounds.KNIGHT_CATASTROPHE, 1.6f, wobble(1.0f));
    }

    // ---------- Demonic Wolf Form

    /** The tick the wolf has its full size. */
    public static final int WOLF_RISEN = 24;

    /**
     * The Demonic Wolf: the body grows round the caster to 'height' blocks tall and stays for 'ticks'; complete = the
     * Servant of Concealment's whole wolf with six arms. It turns with the body. (The caster's real size is the
     * ability's job, as for GiantForm: this is the skin of it.)
     */
    public static void wolfForm(ServerLevel sl, Player player, double height, int ticks, String tag, boolean complete) {
        float full = (float) (height / 1.8);
        float start = (float) (player.getBbHeight() / 1.8);
        ModelFx body = (complete ? DEMONIC_WOLF_COMPLETE : DEMONIC_WOLF).scale(full).lifetime(ticks).fade(4, 12).tag(tag)
                .key(0, pose().scale(start / full).alpha(0.3f))
                .key(WOLF_RISEN, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOnTurning(sl, body, player, Vec3.ZERO, 0f, 0f, 0f);
        for (int t = 0; t < WOLF_RISEN; t += 6) {
            Later.run(sl, t, () -> {
                ParticleShapes.ring(sl, DARK_SMOKE, player.position().add(0, 0.3, 0), 1.0 + height * 0.3, 14, 0.1);
                ParticleShapes.burst(sl, BONE_CHIP, chestOf(player), 3, 0.05, 0.12);
            });
        }
        Later.run(sl, WOLF_RISEN, () -> {
            SpellFx.shockRing(sl, player.position(), height * 2, 0xC05A2E8C, 14, 0);
            wolfHowl(sl, player, height);
        });
        sound(sl, player.position(), ModSounds.KNIGHT_WOLF_FORM, 1.5f, (float) Math.max(0.6, 1.1 - height * 0.04));
    }

    /** A step of the wolf: the ground shudders a little (more for a bigger wolf). */
    public static void wolfStep(ServerLevel sl, Player player, double height) {
        SpellFx.shockRing(sl, player.position(), 1.0 + height * 0.5, 0x805A2E8C, 8, 0);
        sound(sl, player.position(), ModSounds.KNIGHT_WOLF_STEP, (float) Math.min(2.0, 0.5 + height * 0.15), wobble(1.0f));
    }

    /** The wolf howls: a dark ring and the howl, deeper for a bigger wolf. */
    public static void wolfHowl(ServerLevel sl, Player player, double height) {
        Vec3 head = player.position().add(0, height * 0.75, 0).add(Spells.flatLook(player).scale(height * 0.8));
        ParticleShapes.cone(sl, DARK_WISP, head, Spells.flatLook(player).add(0, 0.6, 0), 30, 14, 0.1, 0.3);
        sound(sl, head, ModSounds.KNIGHT_WOLF_HOWL, (float) Math.min(3.0, 1.0 + height * 0.2),
                (float) Math.max(0.55, 1.1 - height * 0.05));
    }

    /** A bite: jaws of darkness close on the target. */
    public static void wolfBite(ServerLevel sl, Player player, LivingEntity target) {
        Vec3 at = chestOf(target);
        float yaw = player.getYRot();
        SlashFx jaw = SlashFx.arc("slash/streaks").color(0xE0E8E0CE).core(0xFFFFFFFF)
                .radius(0.9f).arc(120f).width(0.25f).lifetime(6).sweep(2);
        ParticleShapes.slash(sl, jaw, at, yaw, 0f, 0f);
        ParticleShapes.slash(sl, jaw, at, yaw, 0f, 180f);
        ParticleShapes.burst(sl, BLOOD, at, 10, 0.05, 0.2);
        sound(sl, at, ModSounds.KNIGHT_WOLF_BITE, 1.0f, wobble(1.0f));
    }

    /** The madness rising in the wolf (0..1): violet motes round its head, more the madder it is. */
    public static void wolfMadness(ServerLevel sl, Player player, float madness, double height) {
        int count = Math.round(2 + madness * 10);
        Vec3 head = player.position().add(0, height * 0.8, 0);
        ParticleShapes.burst(sl, SPARK.color(0xFFFF5AC0), head, count, 0.03, 0.06 + madness * 0.1);
        if (madness > 0.75f) play(sl, player.position(), SoundEvents.WARDEN_HEARTBEAT, 0.6f, 0.8f);
    }

    /** The form ends: the wolf breaks into ash and bone. */
    public static void wolfFormEnd(ServerLevel sl, Player player, double height, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        for (double y = 0; y < height; y += 0.6) {
            ParticleShapes.ring(sl, DARK_SMOKE, player.position().add(0, y, 0), height * 0.35, 8, 0.05);
        }
        ParticleShapes.burst(sl, BONE_CHIP, chestOf(player), 14, 0.05, 0.2);
    }

    // =====================================================================================
    // Key 7: the Darkness and Concealment wheel (Horror Bishop and up)
    // =====================================================================================

    // ---------- Sword of Darkness

    /** The tick the sword has formed. */
    public static final int SWORD_FORMED = 12;

    /** The bone sword forms in the right hand out of darkness and is held at the ready beside the body, for 'ticks'. */
    public static void darkSword(ServerLevel sl, Player player, int ticks, String tag) {
        ModelFx held = BONE_SWORD.scale(0.85f).lifetime(ticks).fade(4, 10).tag(tag)
                .key(0, pose().right(0.42f).forward(0.25f).pitch(25f).scale(0.1f).alpha(0f))
                .key(SWORD_FORMED, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOnTurning(sl, held, player, new Vec3(0, 0.7, 0), 0f, 0f, 0f);
        Vec3 hand = player.position().add(0, 0.9, 0).add(Spells.rightOf(player).scale(0.42));
        ParticleShapes.line(sl, DARK_MOTE, hand, hand.add(0, 2.2, 0), 4);
        ParticleShapes.burst(sl, BONE_CHIP, hand.add(0, 1.0, 0), 10, 0.04, 0.12);
        ParticleShapes.burst(sl, DARK_SMOKE, hand.add(0, 1.0, 0), 6, 0.02, 0.06);
        sound(sl, player.position(), ModSounds.KNIGHT_DARK_SWORD, 1.0f, 1.0f);
    }

    private static final SlashFx DARK_ARC = SlashFx.arc("slash/streaks")
            .color(0xE03A1A5A).core(0xFFD2B4FF)
            .radius(3.0f).arc(150f).width(0.8f).layers(3).spread(0.2f)
            .lifetime(9).sweep(3).translucent();

    /** A swing of the sword: a black arc with a violet edge. */
    public static void darkSwing(ServerLevel sl, Player player) {
        Vec3 center = chestOf(player).add(Spells.flatLook(player).scale(0.4));
        ParticleShapes.slash(sl, DARK_ARC, center, player.getYRot(), 0f, -15f);
        sound(sl, player.position(), ModSounds.KNIGHT_SHADOW_SWING, 1.0f, wobble(0.9f));
    }

    /**
     * The tide of Darkness: a wall of black water forms in front of the swing and rolls 'length' blocks forward over
     * 'ticks', widening as it goes. Returns how long it is seen.
     */
    public static int darkTide(ServerLevel sl, Player player, double length, int ticks) {
        float yaw = player.getYRot();
        Vec3 ground = player.position().add(Spells.flatLook(player).scale(1.2));
        Vec3 side = Spells.rightOf(player);
        int life = ticks + 12;
        int pieces = 5;
        for (int i = 0; i < pieces; i++) {
            double across = (i - (pieces - 1) / 2.0) * 1.1;
            float splay = (float) (across * 8);
            ModelFx wave = DARK_TIDE.scale(1.3f, 1.6f, 1f).lifetime(life).fade(3, 12)
                    .key(0, pose().scale(0.3f).alpha(0f))
                    .key(4, pose().scale(1f).alpha(1f), Ease.OUT)
                    .during(0, ticks, pose().forward((float) length).scale(1.8f), Ease.OUT);
            ParticleShapes.model(sl, wave, ground.add(side.scale(across)), yaw + splay, 0f, 0f);
        }
        Vec3 way = ahead(yaw);
        for (int t = 0; t < ticks; t += 2) {
            double along = length * t / ticks;
            Vec3 at = ground.add(way.scale(along));
            Later.run(sl, t, () -> ParticleShapes.ring(sl, DARK_SMOKE, at.add(0, 0.3, 0), 1.5 + along * 0.15, 10, 0.06));
        }
        sound(sl, player.position(), ModSounds.KNIGHT_DARK_TIDE, 1.4f, 1.0f);
        return life;
    }

    /** Someone the tide submerges: they melt, black and violet, for a moment. */
    public static void tideMelt(ServerLevel sl, LivingEntity victim) {
        ParticleShapes.burst(sl, DARK_SMOKE, chestOf(victim), 6, 0.02, 0.06);
        ParticleShapes.burst(sl, DARK_MOTE, chestOf(victim), 6, 0.04, 0.1);
        ParticleShapes.spawn(sl, BLOOD.color(0xFF2A0A4A), chestOf(victim), new Vec3(0, -0.05, 0));
    }

    /** One of the three edicts lands with a swing (Knight of Misfortune). */
    public static void edict(ServerLevel sl, Player player, LivingEntity target, Edict edict) {
        Vec3 at = chestOf(target);
        flashEye(sl, at.add(0, 0.6, 0), player.getYRot() + 180f, withAlpha(edict.color, 0xC0), 1.1f);
        switch (edict) {
            case ETERNAL_SLEEP -> {
                ParticleShapes.burst(sl, SLEEP_Z, headOf(target), 6, 0.03, 0.08);
                sound(sl, at, ModSounds.KNIGHT_EDICT_SLEEP, 1.0f, 1.0f);
            }
            case DESPAIR -> {
                ParticleShapes.burst(sl, SKULL, headOf(target), 5, 0.02, 0.06);
                ParticleShapes.burst(sl, DARK_SMOKE.color(0xA0302830), at, 8, 0.01, 0.05);
                sound(sl, at, ModSounds.KNIGHT_EDICT_DESPAIR, 1.0f, 1.0f);
            }
            case DESTRUCTION -> {
                SpellFx.blast(sl, at, 2.0, 0xE09A2AD8, BONE_CHIP);
                ParticleShapes.burst(sl, SPARK, at, 16, 0.1, 0.3);
                sound(sl, at, ModSounds.KNIGHT_EDICT_DESTRUCTION, 1.2f, 1.0f);
            }
        }
    }

    public static void darkSwordEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        Vec3 hand = player.position().add(0, 0.9, 0).add(Spells.rightOf(player).scale(0.42));
        ParticleShapes.line(sl, BONE_CHIP, hand, hand.add(0, 2.0, 0), 3);
    }

    // ---------- Nightmare World

    /** The tick the Nightmare World (and the Concealed World) is closed. */
    public static final int WORLD_CLOSED = 30;

    /** A dream world opens round 'center' (on the ground): a dim violet dome of fog 'radius' wide, the seal of Darkness under it. */
    public static void nightmareWorld(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        veilShell(sl, center, radius, 0xC0B090E0, ticks, tag);
        seal(sl, center, radius * 0.9, 0x905A2E8C, ticks, tag);
        SpellFx.steadyRing(sl, center, radius, 0xC09A5AD8, WORLD_CLOSED, 0);
        sound(sl, center, ModSounds.KNIGHT_NIGHTMARE_WORLD, 1.6f, 1.0f);
    }

    /** Every second inside: dream motes, and z's drift up. */
    public static void nightmareWorldPulse(ServerLevel sl, Vec3 center, double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            double a = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius * 0.9;
            Vec3 at = center.add(Math.cos(a) * far, 0.3 + random.nextDouble() * 3, Math.sin(a) * far);
            ParticleShapes.spawn(sl, i % 4 == 0 ? SLEEP_Z : i % 2 == 0 ? DREAM_MOTE : STAR, at, new Vec3(0, 0.02, 0));
        }
    }


    // ---------- Concealed State, Reverse Concealment

    /** The tick the target is gone from the world. */
    public static final int CONCEALED = 8;

    /** Dark fog wraps the target and swallows it (it is hidden from then on: the ability's part). */
    public static void concealedState(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        double r = widthOf(target);
        for (int t = 0; t < CONCEALED; t += 2) {
            int step = t;
            Later.run(sl, t, () -> ParticleShapes.spiral(sl, FOG, target.position().add(0, 0.1 + step * 0.2, 0), 3, 9,
                    r * 1.6, r * 0.6, 0.6, 0.06));
        }
        SpellFx.closingRing(sl, target.position(), r * 2.5, 0xC05A2E8C, CONCEALED, 0);
        ModelFx gone = VEIL_WALL.color(0xE0FFFFFF).scale((float) (r * 1.8), (float) target.getBbHeight() * 1.1f, 1f)
                .lifetime(CONCEALED + 8).fade(2, 8).key(0, pose().alpha(0f)).key(CONCEALED, pose().alpha(1f));
        if (tag != null) gone = gone.tag(tag);
        ParticleShapes.modelOnTurning(sl, gone, target, new Vec3(0, 0, 0.2), 0f, 0f, 0f);
        sound(sl, target.position(), ModSounds.KNIGHT_CONCEAL, 1.0f, 1.0f);
    }

    /** It comes back out of the fog (its time is up). */
    public static void concealedStateEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, FOG, chestOf(target), 10, 0.03, 0.1);
    }

    /** Reverse Concealment: the hidden one is torn into the light: a pale flash, fog thrown off and a ring round them. */
    public static void reverseConcealment(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, FOG, chestOf(target), 14, 0.1, 0.3);
        ParticleShapes.burst(sl, STAR, chestOf(target), 16, 0.06, 0.2);
        SpellFx.shockRing(sl, target.position(), 3.0, 0xE0D2B4FF, 10, 0);
        SpellFx.risingRings(sl, target.position(), widthOf(target), target.getBbHeight(), 3, 0xC0D2B4FF);
        sound(sl, target.position(), ModSounds.KNIGHT_REVEAL, 1.0f, 1.0f);
    }

    // ---------- Secret Concealment

    /** A veil over an area: the closed eye of the secret seal lies over it, 'radius' wide, for 'ticks'. */
    public static void secretConcealment(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        ModelFx seal = SECRET_SEAL.color(0xB09A5AD8).scale((float) (radius * 2.0)).lifetime(ticks).fade(0, 20).spin(-0.4f)
                .key(0, pose().scale(0.1f).alpha(0f)).key(16, pose().scale(1f).alpha(1f), Ease.OUT);
        if (tag != null) seal = seal.tag(tag);
        ParticleShapes.model(sl, seal, center.add(0, 0.06, 0), 0f, 0f, 0f);
        SpellFx.steadyRing(sl, center, radius, 0xA09A5AD8, 16, 0);
        ParticleShapes.ring(sl, FOG, center.add(0, 0.5, 0), radius, 30, 0);
        sound(sl, center, ModSounds.KNIGHT_SECRET, 1.2f, 1.0f);
    }

    /** Every second: fog drifts along the edge of the veil. */
    public static void secretPulse(ServerLevel sl, Vec3 center, double radius) {
        ParticleShapes.ring(sl, FOG, center.add(0, 0.6, 0), radius, 14, 0);
    }

    // ---------- Concealed World

    /** A sealed world closes round 'center': an opaque shell of fog, its seam glinting as it shuts. */
    public static void concealedWorld(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        veilShell(sl, center, radius, 0xFFFFFFFF, ticks, tag);
        SpellFx.steadyRing(sl, center.subtract(0, radius * 0.95, 0), radius, 0xC09A5AD8, WORLD_CLOSED, 0);
        Later.run(sl, WORLD_CLOSED, () -> {
            ParticleShapes.ring(sl, STAR, center, radius * 0.98, 40, 0);
            SpellFx.shockRing(sl, center, radius, 0xE0D2B4FF, 10, 0);
        });
        sound(sl, center, ModSounds.KNIGHT_CONCEALED_WORLD, 1.6f, 1.0f);
    }

    public static void concealedWorldEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.sphere(sl, FOG, center, radius, 40);
        sound(sl, center, ModSounds.KNIGHT_REVEAL, 1.2f, 0.8f);
    }

    // ---------- Mantle of Darkness (Knight of Misfortune)

    /** The tick the mantle has risen. */
    public static final int MANTLE_RISEN = 12;

    /** The body half-dissolves into living Darkness: a black mantle round it, eyes of violet, two shadow arms. */
    public static void mantle(ServerLevel sl, Player player, int ticks, String tag) {
        ModelFx shade = SOUL_FIGURE.color(0xD0180A28).scale(1.12f).lifetime(ticks).fade(MANTLE_RISEN, 14).tag(tag)
                .key(0, pose().scale(0.9f)).key(MANTLE_RISEN, pose().scale(1f), Ease.OUT);
        ParticleShapes.modelOnTurning(sl, shade, player, Vec3.ZERO, 0f, 0f, 0f);
        SpellFx.circleUnder(sl, player, 1.4, 0xC05A2E8C, ticks, tag);
        ParticleShapes.spiral(sl, DARK_SMOKE, player.position().add(0, 0.1, 0), 3, 30, 0.4, 1.4, 1.0, 0.06);
        SpellFx.risingRings(sl, player.position(), 1.2, 2.4, 4, 0xC09A5AD8);
        sound(sl, player.position(), ModSounds.KNIGHT_MANTLE, 1.2f, 1.0f);
    }

    /** Every few ticks while it lasts: darkness drips off the body. */
    public static void mantleTick(ServerLevel sl, Player player) {
        ParticleShapes.spawn(sl, DARK_SMOKE, chestOf(player), new Vec3(0, -0.02, 0));
        ParticleShapes.spawn(sl, DARK_MOTE, player.getEyePosition(), new Vec3(0, 0.01, 0));
    }

    private static final SlashFx SHADOW_ARM = SlashFx.arc("slash/streaks")
            .color(0xE0200C38).core(0xC09A5AD8).radius(2.6f).arc(110f).width(0.6f).layers(2).spread(0.15f)
            .lifetime(8).sweep(3).translucent();

    /** A swing with the mantle on: the two shadow arms follow, each to one of the 'struck'. */
    public static void mantleSwing(ServerLevel sl, Player player, List<LivingEntity> struck) {
        Vec3 center = chestOf(player);
        ParticleShapes.slash(sl, SHADOW_ARM, center, player.getYRot(), 0f, -40f);
        ParticleShapes.slash(sl, SHADOW_ARM, center, player.getYRot(), 0f, 220f);
        for (LivingEntity one : struck) {
            ParticleShapes.slashBetween(sl, LASH.color(0xE0200C38), center.add(0, 0.6, 0), chestOf(one));
            ParticleShapes.burst(sl, DARK_SMOKE, chestOf(one), 4, 0.02, 0.06);
        }
        sound(sl, player.position(), ModSounds.KNIGHT_SHADOW_SWING, 1.0f, wobble(0.8f));
    }

    public static void mantleEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.burst(sl, DARK_SMOKE, chestOf(player), 16, 0.04, 0.12);
    }

    // ---------- Eternal Rest (Darkness)

    /** The tick the word of Darkness falls on its target. */
    public static final int REST_FALLS = 20;

    /** The closed eye of Eternal Rest opens over the target, sinks onto it and darkness folds it away. */
    public static void eternalRest(ServerLevel sl, Player player, LivingEntity target) {
        ModelFx sigil = REST_SIGIL.color(0xE0D2B4FF).scale(1.6f).lifetime(REST_FALLS + 16).fade(4, 12).spin(1f)
                .key(0, pose().up(3f).scale(0.2f).alpha(0f))
                .key(10, pose().up(3f).scale(1f).alpha(1f), Ease.OUT_BACK)
                .key(REST_FALLS, pose().up(0.2f).scale(0.6f), Ease.IN);
        ParticleShapes.model(sl, sigil, target.position().add(0, target.getBbHeight(), 0), 0f, 0f, 0f);
        SpellFx.warningCircle(sl, target.position(), widthOf(target) * 1.5, 0xC05A2E8C, REST_FALLS);
        Later.run(sl, REST_FALLS, () -> {
            ParticleShapes.sphere(sl, DARK_SMOKE, chestOf(target), 1.0, 20);
            SpellFx.closingRing(sl, target.position(), 3.0, 0xE05A2E8C, 10, 0);
            ParticleShapes.burst(sl, SLEEP_Z, headOf(target), 4, 0.02, 0.05);
        });
        sound(sl, target.position(), ModSounds.KNIGHT_ETERNAL_REST, 1.2f, 1.0f);
    }

    /** Eternal Rest aimed at nothing: the caster's own rest waits over them (their next death is cancelled). */
    public static void restWard(ServerLevel sl, Player player, int ticks, String tag) {
        ModelFx sigil = REST_SIGIL.color(0x90D2B4FF).scale(0.7f).lifetime(ticks).fade(6, 10).spin(0.8f).tag(tag);
        ParticleShapes.modelOnHead(sl, sigil, player, 0.4, 0f, 0f, 0f);
        SpellFx.risingRings(sl, player.position(), 0.8, 2.0, 3, 0x90D2B4FF);
        sound(sl, player.position(), ModSounds.KNIGHT_ETERNAL_REST, 0.6f, 1.3f);
    }

    /** The ward is used: the caster rises out of the darkness that would have taken them. */
    public static void restWake(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        SpellFx.risingRings(sl, player.position(), 1.0, 2.2, 5, 0xE0D2B4FF);
        ParticleShapes.burst(sl, STAR, chestOf(player), 20, 0.05, 0.2);
        sound(sl, player.position(), ModSounds.KNIGHT_SOUL_RETURN, 1.0f, 0.9f);
    }

    // ---------- Destination of All Things (Darkness)

    /** The tick the black sphere is open: the pull begins. */
    public static final int DESTINATION_OPENS = 20;

    /** A vortex opens where it is aimed: a black hole standing in the air and a spiral turning under it, 'radius' wide. */
    public static void destination(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        ModelFx floor = VORTEX.scale((float) (radius * 2.0)).lifetime(ticks).fade(0, 20).spin(-3f)
                .key(0, pose().scale(0.05f).alpha(0f)).key(DESTINATION_OPENS, pose().scale(1f).alpha(1f), Ease.OUT);
        ModelFx hole = VORTEX.scale((float) Math.max(2.0, radius * 0.35)).lifetime(ticks).fade(0, 20).spin(-6f)
                .key(0, pose().scale(0.05f)).key(DESTINATION_OPENS, pose().scale(1f), Ease.OUT_BACK);
        if (tag != null) {
            floor = floor.tag(tag);
            hole = hole.tag(tag);
        }
        Vec3 heart = center.add(0, 2.5, 0);
        ParticleShapes.model(sl, floor, center.add(0, 0.06, 0), 0f, 0f, 0f);
        ParticleShapes.model(sl, hole, heart, 0f, 90f, 0f);
        ParticleShapes.model(sl, hole, heart, 90f, 90f, 0f);
        SpellFx.closingRing(sl, center, radius, 0xE05A2E8C, DESTINATION_OPENS, 0);
        sound(sl, center, ModSounds.KNIGHT_DESTINATION, 2.0f, 1.0f);
    }

    /** Every few ticks while it is open: everything streams inward to the heart. */
    public static void destinationTick(ServerLevel sl, Vec3 center, double radius) {
        Vec3 heart = center.add(0, 2.5, 0);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 8; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            Vec3 from = center.add(Math.cos(a) * radius, 0.3 + random.nextDouble() * 3.5, Math.sin(a) * radius);
            ParticleShapes.spawn(sl, i % 2 == 0 ? DARK_MOTE : DARK_SMOKE, from, heart.subtract(from).scale(0.06));
        }
    }

    /** Someone in it dissolves: they come apart into dark motes that fly to the heart. */
    public static void dissolve(ServerLevel sl, LivingEntity victim) {
        ParticleShapes.burst(sl, DARK_MOTE, chestOf(victim), 8, 0.05, 0.15);
        ParticleShapes.burst(sl, DARK_SMOKE, chestOf(victim), 4, 0.02, 0.06);
        sound(sl, chestOf(victim), ModSounds.KNIGHT_DISSOLVE, 0.8f, wobble(1.0f));
    }

    public static void destinationEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.clearModels(sl, center.add(0, 2.5, 0), tag);
        SpellFx.shockRing(sl, center, 6.0, 0xC09A5AD8, 12, 0);
        ParticleShapes.burst(sl, STAR, center.add(0, 2.5, 0), 30, 0.1, 0.4);
    }

    // =====================================================================================
    // Passives
    // =====================================================================================

    /** Nocturnality, now and then in full dark: violet motes in the eyes. */
    public static void nocturnality(ServerLevel sl, Player player) {
        ParticleShapes.spawn(sl, DARK_MOTE, player.getEyePosition().add(player.getLookAngle().scale(0.3)), Vec3.ZERO);
    }

    /** Sensing hidden danger in the dark: a low tone only the Beyonder hears. */
    public static void dangerChime(ServerPlayer player) {
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME),
                SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 0.5f, 0.6f, player.getRandom().nextLong()));
    }

    /** The Horror Aura once a second: dark motes drift outward round the Bishop, now and then an eye. */
    public static void horrorAura(ServerLevel sl, Player player, double radius) {
        ParticleShapes.ring(sl, DARK_SMOKE.color(0x70180A28), player.position().add(0, 0.2, 0), radius * 0.5, 8, 0.05);
        if (sl.getRandom().nextInt(4) == 0) ParticleShapes.spawn(sl, EYE, headOf(player).add(0, 0.4, 0), new Vec3(0, 0.02, 0));
    }

    /** Someone trembles at the Bishop or flinches before Darkness (their act is cancelled: the ability's part). */
    public static void horrorFlinch(ServerLevel sl, LivingEntity victim) {
        flashEye(sl, headOf(victim).add(0, 0.5, 0), victim.getYRot() + 180f, 0xC09A5AD8, 0.7f);
        ParticleShapes.burst(sl, DARK_SMOKE, chestOf(victim), 4, 0.02, 0.06);
        sound(sl, victim.position(), ModSounds.KNIGHT_HORROR, 0.8f, wobble(1.0f));
    }

    /** Ill Omen: who hurts the Knight gets Misfortune: a horseshoe falls from them. */
    public static void illOmen(ServerLevel sl, LivingEntity attacker) {
        ParticleShapes.burst(sl, HORSESHOE, headOf(attacker), 3, 0.02, 0.06);
    }

    // =====================================================================================
    // /knightfx: every display, to look at in game
    // =====================================================================================

    @FunctionalInterface
    private interface Show {
        void play(ServerLevel sl, Player viewer, LivingEntity target, Vec3 spot);
    }

    /** onViewer = it is shown on the viewer (worn), not on a spot or a target. */
    private record Case(String name, int ticks, boolean onViewer, Show show) {}

    private static String tagFor(String name) {
        return "km_fx_" + name + "_" + UUID.randomUUID();
    }

    private static final List<Case> CASES = List.of(
            new Case("spirit_vision", 60, true, (sl, v, t, spot) -> {
                if (!(v instanceof ServerPlayer viewer)) return;
                spiritVision(viewer, 60);
                if (t != v) for (int i = 0; i < 3; i++) Later.run(sl, 10 + i * 15, () -> spiritVisionMark(viewer, t, false));
            }),
            new Case("dowsing", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("dowsing");
                dowsing(sl, v, t == v ? spot : t.position(), 80, tag);
            }),
            new Case("tranquilize", 50, true, (sl, v, t, spot) -> {
                recite(sl, v, Poem.TRANQUILIZE, tagFor("poem"));
                Later.run(sl, POEM_LANDS, () -> {
                    tranquilize(sl, v, 8);
                    tranquilized(sl, v);
                    if (t != v) tranquilizeReveal(sl, t);
                });
            }),
            new Case("lullaby", 100, false, (sl, v, t, spot) -> {
                recite(sl, v, Poem.LULLABY, tagFor("poem"));
                lullaby(sl, v, 8);
                if (t != v) {
                    String tag = tagFor("sleep");
                    Later.run(sl, POEM_LANDS, () -> slumber(sl, t, 80, tag));
                    Later.run(sl, 90, () -> wake(sl, t, tag));
                }
            }),
            new Case("pacify", 80, false, (sl, v, t, spot) -> {
                if (t == v) return;
                recite(sl, v, Poem.PACIFY, tagFor("poem"));
                pacify(sl, v, t, 70, tagFor("pacify"));
            }),
            new Case("fear", 80, false, (sl, v, t, spot) -> {
                recite(sl, v, Poem.FEAR, tagFor("poem"));
                fear(sl, v, 12);
                if (t != v) Later.run(sl, POEM_LANDS, () -> afraid(sl, t, 60, tagFor("afraid")));
            }),
            new Case("agitate", 80, false, (sl, v, t, spot) -> {
                if (t == v) return;
                recite(sl, v, Poem.AGITATING, tagFor("poem"));
                agitate(sl, v, t, 70, tagFor("agitate"));
            }),
            new Case("limbs", 100, true, (sl, v, t, spot) -> {
                String tag = tagFor("limbs");
                nightmareLimbs(sl, v, 90, tag);
                if (t != v) for (int i = 0; i < 4; i++) {
                    int index = i;
                    Later.run(sl, LIMBS_GROWN + 10 + i * 12, () -> limbLash(sl, v, t, index));
                }
                Later.run(sl, 88, () -> limbsTear(sl, v, tag));
            }),
            new Case("nightmare_state", 110, true, (sl, v, t, spot) -> {
                String tag = tagFor("soul");
                Vec3 body = v.position();
                nightmareState(sl, v, 100, tag);
                sleepingBody(sl, body, v.getYRot(), 100, tag);
                for (int i = SOUL_OUT; i < 90; i += 6) Later.run(sl, i, () -> nightmareTick(sl, v));
                Later.run(sl, 100, () -> nightmareReturn(sl, v, body, tag));
            }),
            new Case("dream", 110, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("dream");
                dreamInvade(sl, v, t, 100, tag);
                Dream[] dreams = Dream.values();
                for (int i = 0; i < 3; i++) {
                    Dream dream = dreams[i];
                    Later.run(sl, DREAM_OPENS + 15 + i * 22, () -> dreamShape(sl, t, dream));
                }
                Later.run(sl, 100, () -> dreamEnd(sl, t, tag));
            }),
            new Case("requiem", 120, false, (sl, v, t, spot) -> {
                String tag = tagFor("requiem");
                requiem(sl, spot, 6, 110, tag);
                for (int i = 1; i < 5; i++) Later.run(sl, REQUIEM_RISEN + i * 20, () -> requiemPulse(sl, spot, 6));
                if (t != v) for (int i = 0; i < 5; i++) {
                    float depth = i / 4f;
                    Later.run(sl, REQUIEM_RISEN + i * 20, () -> requiemOn(sl, t, depth));
                }
                Later.run(sl, 110, () -> requiemEnd(sl, spot, tag));
            }),
            new Case("soothing", 50, true, (sl, v, t, spot) -> {
                soothingRequiem(sl, v, 8);
                Later.run(sl, 10, () -> soothed(sl, v));
            }),
            new Case("banshee", 120, false, (sl, v, t, spot) -> {
                spiritRelease(sl, v, spot.add(0, 1, 0));
                if (t == v) return;
                String tag = tagFor("banshee");
                banshee(sl, t, 110, tag);
                for (int i = 0; i < 3; i++) Later.run(sl, 30 + i * 30, () -> bansheeWail(sl, t, 8));
                Later.run(sl, 110, () -> bansheeEnd(sl, t, tag));
            }),
            new Case("earthen_spirit", 70, true, (sl, v, t, spot) -> {
                String tag = tagFor("earth");
                spiritRelease(sl, v, v.position());
                earthMerge(sl, v, 60, tag);
                for (int i = EARTH_SUNK; i < 56; i += 4) Later.run(sl, i, () -> earthTick(sl, v));
                Later.run(sl, 60, () -> earthEmerge(sl, v, tag));
            }),
            new Case("gale_spirit", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("gale");
                gale(sl, v, 80, tag);
                for (int i = 6; i < 76; i += 6) Later.run(sl, i, () -> galeTick(sl, v));
                Later.run(sl, 80, () -> galeEnd(sl, v, tag));
            }),
            new Case("command_spirits", 90, true, (sl, v, t, spot) -> {
                commandSpirits(sl, v, 12);
                if (t != v) Later.run(sl, 20, () -> spiritBound(sl, t, 60, tagFor("bound")));
            }),
            new Case("night_domain", 160, true, (sl, v, t, spot) -> {
                String tag = tagFor("domain");
                Vec3 center = v.position();
                nightDomain(sl, center, 12, 150, tag);
                for (int i = 1; i < 7; i++) Later.run(sl, DOMAIN_RISEN + i * 20, () -> domainPulse(sl, center, 12));
                if (t != v) for (int i = 1; i < 6; i++) Later.run(sl, DOMAIN_RISEN + i * 20, () -> domainCorrode(sl, t));
                Later.run(sl, 150, () -> domainEnd(sl, center, tag));
            }),
            new Case("hair", 90, false, (sl, v, t, spot) -> {
                List<LivingEntity> caught = t == v ? List.of() : List.of(t);
                hairEntangle(sl, v, caught);
                if (t != v) {
                    String tag = tagFor("hair");
                    Later.run(sl, HAIR_BINDS, () -> hairBind(sl, t, 70, tag));
                    Later.run(sl, 76, () -> slumber(sl, t, 10, tag));
                }
            }),
            new Case("curse", 90, false, (sl, v, t, spot) -> {
                if (t == v) return;
                curse(sl, v, t);
                String tag = tagFor("misfortune");
                for (int lv = 1; lv <= 5; lv++) {
                    int level = lv;
                    Later.run(sl, CURSE_LANDS + (lv - 1) * 14, () -> misfortune(sl, t, level, 80 - (level - 1) * 14, tag));
                }
            }),
            new Case("mishaps", 120, false, (sl, v, t, spot) -> {
                if (t == v) return;
                Mishap[] all = Mishap.values();
                for (int i = 0; i < all.length; i++) {
                    Mishap mishap = all[i];
                    Later.run(sl, i * 22, () -> mishap(sl, t, mishap));
                }
            }),
            new Case("misfortune_area", 120, false, (sl, v, t, spot) -> {
                misfortuneArea(sl, spot, 6, 110, tagFor("area"));
            }),
            new Case("catastrophe", 40, false, (sl, v, t, spot) -> {
                if (t != v) catastrophe(sl, t);
            }),
            new Case("wolf_form", 160, true, (sl, v, t, spot) -> {
                String tag = tagFor("wolf");
                wolfForm(sl, v, 3.0, 150, tag, false);
                for (int i = 0; i < 4; i++) Later.run(sl, WOLF_RISEN + 30 + i * 14, () -> wolfStep(sl, v, 3.0));
                for (int i = 0; i < 5; i++) {
                    float madness = i / 4f;
                    Later.run(sl, WOLF_RISEN + i * 22, () -> wolfMadness(sl, v, madness, 3.0));
                }
                if (t != v) Later.run(sl, WOLF_RISEN + 40, () -> wolfBite(sl, v, t));
                Later.run(sl, 150, () -> wolfFormEnd(sl, v, 3.0, tag));
            }),
            new Case("wolf_complete", 140, false, (sl, v, t, spot) -> {
                ModelFx colossus = DEMONIC_WOLF_COMPLETE.scale(8f).lifetime(130).fade(20, 20)
                        .key(0, pose().up(-6f).alpha(0f))
                        .key(40, pose().up(0f).alpha(1f), Ease.OUT);
                Vec3 far = v.position().add(ahead(v.getYRot()).scale(26));
                ParticleShapes.model(sl, colossus, far, v.getYRot() + 180f, 0f, 0f);
                sound(sl, far, ModSounds.KNIGHT_WOLF_FORM, 2.0f, 0.6f);
                Later.run(sl, 50, () -> sound(sl, far, ModSounds.KNIGHT_WOLF_HOWL, 3.0f, 0.55f));
            }),
            new Case("sword_of_darkness", 110, true, (sl, v, t, spot) -> {
                String tag = tagFor("sword");
                darkSword(sl, v, 100, tag);
                Later.run(sl, 30, () -> darkSwing(sl, v));
                Later.run(sl, 50, () -> {
                    darkSwing(sl, v);
                    darkTide(sl, v, 10, 24);
                });
                if (t != v) Later.run(sl, 62, () -> tideMelt(sl, t));
                Later.run(sl, 100, () -> darkSwordEnd(sl, v, tag));
            }),
            new Case("edicts", 110, true, (sl, v, t, spot) -> {
                String tag = tagFor("edicts");
                darkSword(sl, v, 100, tag);
                Edict[] edicts = Edict.values();
                for (int i = 0; i < 3; i++) {
                    Edict edict = edicts[i];
                    Later.run(sl, SWORD_FORMED + 14 + i * 20, () -> {
                        darkSwing(sl, v);
                        if (t != v) edict(sl, v, t, edict);
                    });
                }
                Later.run(sl, 100, () -> darkSwordEnd(sl, v, tag));
            }),
            new Case("nightmare_world", 150, false, (sl, v, t, spot) -> {
                String tag = tagFor("world");
                Vec3 center = spot.add(0, 0.1, 0);
                nightmareWorld(sl, center, 5, 140, tag);
                for (int i = 1; i < 6; i++) Later.run(sl, WORLD_CLOSED + i * 20, () -> nightmareWorldPulse(sl, center, 5));
            }),
            new Case("concealed_state", 90, false, (sl, v, t, spot) -> {
                LivingEntity who = t == v ? v : t;
                String tag = tagFor("conceal");
                concealedState(sl, who, 70, tag);
                Later.run(sl, 50, () -> concealedStateEnd(sl, who, tag));
                Later.run(sl, 70, () -> reverseConcealment(sl, who));
            }),
            new Case("secret_concealment", 120, false, (sl, v, t, spot) -> {
                secretConcealment(sl, spot, 6, 110, tagFor("secret"));
                for (int i = 1; i < 5; i++) Later.run(sl, i * 20, () -> secretPulse(sl, spot, 6));
            }),
            new Case("concealed_world", 140, false, (sl, v, t, spot) -> {
                String tag = tagFor("cworld");
                Vec3 center = spot.add(0, 4, 0);
                concealedWorld(sl, center, 4, 130, tag);
                Later.run(sl, 128, () -> concealedWorldEnd(sl, center, 4, tag));
            }),
            new Case("mantle", 110, true, (sl, v, t, spot) -> {
                String tag = tagFor("mantle");
                mantle(sl, v, 100, tag);
                for (int i = MANTLE_RISEN; i < 96; i += 5) Later.run(sl, i, () -> mantleTick(sl, v));
                List<LivingEntity> struck = t == v ? List.of() : List.of(t);
                Later.run(sl, 40, () -> mantleSwing(sl, v, struck));
                Later.run(sl, 70, () -> mantleSwing(sl, v, struck));
                Later.run(sl, 100, () -> mantleEnd(sl, v, tag));
            }),
            new Case("eternal_rest", 60, false, (sl, v, t, spot) -> {
                if (t != v) eternalRest(sl, v, t);
            }),
            new Case("rest_ward", 80, true, (sl, v, t, spot) -> {
                String tag = tagFor("ward");
                restWard(sl, v, 70, tag);
                Later.run(sl, 60, () -> restWake(sl, v, tag));
            }),
            new Case("destination", 160, false, (sl, v, t, spot) -> {
                String tag = tagFor("destination");
                Vec3 center = spot.add(ahead(v.getYRot()).scale(4));
                destination(sl, center, 6, 150, tag);
                for (int i = DESTINATION_OPENS; i < 145; i += 4) Later.run(sl, i, () -> destinationTick(sl, center, 6));
                if (t != v) for (int i = 1; i < 5; i++) Later.run(sl, DESTINATION_OPENS + i * 20, () -> dissolve(sl, t));
                Later.run(sl, 150, () -> destinationEnd(sl, center, tag));
            }),
            new Case("passives", 60, true, (sl, v, t, spot) -> {
                for (int i = 0; i < 3; i++) Later.run(sl, i * 20, () -> {
                    nocturnality(sl, v);
                    horrorAura(sl, v, 8);
                });
                if (v instanceof ServerPlayer sp) dangerChime(sp);
                if (t != v) {
                    Later.run(sl, 20, () -> horrorFlinch(sl, t));
                    Later.run(sl, 40, () -> illOmen(sl, t));
                }
            }));

    /** The names /knightfx knows. */
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
        event.getDispatcher().register(Commands.literal("knightfx")
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
