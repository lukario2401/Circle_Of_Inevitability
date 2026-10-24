package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars;

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
 * How the Key Of Stars (Door) pathway looks and sounds: every key, both casts, every wheel entry, the growth at higher
 * Sequences (people coming through Door Opening, the Spatial Cage, Symbolization and Conceptualization, Dimension
 * Sight, Blink's lingering afterimages), the Starlit mark, the Recorded Powers' pages, the passives that show. Visual
 * only: nothing here hurts, holds, hides, heals or moves anyone. The ability code does that, at the ticks named by the
 * constants below (..._OPENED, ..._LANDS, ...), with Later.run. See docs/KEY_OF_STARS.md.
 *
 * ---------- See them in game ----------
 *   /starsfx all              every display one after another (/starsfx all 3 = three times over)
 *   /starsfx alternate_world  just that one (press TAB after /starsfx for the names)
 * Displays that need a target use the nearest mob within 12 blocks, or you when there is none.
 *
 * ---------- Use them in an ability ----------
 *   Key 1   doorOpening(sl, player, from, to) (you step through at DOOR_OPENED), doorStep(sl, who, at) for each one who
 *               comes along (Astrologer), realDoor(sl, at) for a real door, trapdoor or gate swung open
 *           sneak: slipFree(sl, caster, target), travelUnbarred(sl, player) (the Secrets Sorcerer's door through a
 *               Teleport root)
 *   Key 2   the wheel's pictures: WHEEL_FLASH ... WHEEL_ESCAPE_TRICK
 *           flash(sl, player, range), blinded(sl, target, ticks)
 *           blackScreen(sl, center, yaw, ticks, tag) (down at SCREEN_DOWN), blackScreenEnd(sl, center, tag)
 *           loudNoise(sl, player, at), deafened(sl, target, ticks)
 *           frostRay(sl, player, from, to), frosted(sl, target, ticks, tag)
 *           gale(sl, player, range), galeHit(sl, target)
 *           tumble(sl, player, center, radius, ticks, tag), tumbleTick(sl, center, radius) every half second,
 *               slipped(sl, target, ticks), tumbleEnd(sl, center, tag)
 *           escapeTrick(sl, player, from, to, diversion)
 *   Key 3   starDivination(sl, player, target) (Starlit at DIVINED; target null = the sweep), smallStar(owner, who,
 *               ticks) for each creature the sweep finds (only the owner sees it)
 *           starlit(sl, caster, target, ticks, tag), starlitSeen(viewer, who) every second for the caster and each
 *               ally, starlitEnd(sl, target, tag); dimensionSight(owner, who) for one far away (Planeswalker)
 *           sneak: interference(sl, player, ticks, tag), interferenceTick(sl, player), interferenceTurned(sl, player,
 *               caster), mobForgets(sl, mob), interferenceEnd(sl, player, tag)
 *   Key 4   record(sl, player, ticks, tag), recorded(sl, player, from, angel), recordFailed(sl, player),
 *               recordEnd(sl, player, tag); on the screen recordedPages(player, angel[]) whenever the slots change
 *           sneak: replay(sl, player)
 *   Key 5   the wheel's pictures: WHEEL_TRAVELERS_DOOR ... WHEEL_WANDERING
 *           travelersDoor(sl, player, from) (inside at TRAVEL_ENTERED), travelersArrive(sl, at, yaw)
 *           blink(sl, player, from, to, chain), blinkDodge(sl, player), blinkEcho(sl, player, at, ticks, tag) (the
 *               Planeswalker's afterimages that stay), quickened(player, ticks) (the Secrets Sorcerer's quick powers)
 *           invisibleHand(sl, player, target) (grabs at HAND_GRABS), invisibleHandItem(sl, player, at), yanked(sl, target)
 *           starAnchor(sl, player, at, ticks, tag), anchorLift(sl, at, tag), anchorDirection(owner, anchor) every second
 *           wandering(sl, player, ticks, form, tag), wanderingTick(sl, player, form) every 2 ticks, conceptualized(sl,
 *               enemy), wanderingEnd(sl, player, tag)
 *   Key 6   the wheel's pictures: WHEEL_SPACE_CONCEALMENT ... WHEEL_SPACE_TEARING
 *           spaceConcealment(sl, player, center, radius, ticks, doorYaw, cage, tag) (closed at CONCEALED),
 *               concealmentShut(sl, center, radius, doorYaw, tag), concealedHeal(sl, ally), concealmentEnd(sl, center,
 *               radius, tag)
 *           transfiguration(sl, player, layers, ticks, tag), layerTorn(sl, player, from, to, layersLeft),
 *               transfigurationEnd(sl, player, tag)
 *           exile(sl, player, target) (swallowed at EXILE_SWALLOWED), exiled(sl, target, at, ticks, tag),
 *               exileReturn(sl, at, tag)
 *           pocketSpace(sl, player)
 *           spaceTearing(sl, player, target, radius) (tears at TEAR_LANDS), storm(sl, target, ticks, tag),
 *               stormTick(sl, target)
 *   Key 7   the wheel's pictures: WHEEL_TOY_BOX ... WHEEL_CLOSE_THE_DOOR
 *           toyBox(sl, player, center, radius, ticks, tag), toyed(sl, target, ticks, tag) (shrunk at TOY_SHRUNK),
 *               toyTick(sl, target), toyReleased(sl, target, tag), toySlowed(sl, target, ticks), toyBoxEnd(sl, center,
 *               tag)
 *           replication(sl, player, source)
 *           spaceFragmentation(sl, player, center, radius, ticks, tag), fragmentationTick(sl, center, radius) every few
 *               ticks, fragmentationCollapse(sl, center, radius, tag)
 *           starlightSeal(sl, player, target, ticks, tag) (Sealed at SEALED_AT), sealRepaired(sl, target),
 *               sealEnd(sl, target, tag); sealAlly(sl, player, ally, ticks, tag)
 *           labyrinth(sl, player, center, radius, ticks, tag) (risen at LABYRINTH_RISEN), lost(sl, target, ticks),
 *               labyrinthLoop(sl, from, to), labyrinthEnd(sl, center, radius, tag)
 *           alternateWorld(sl, player, center, radius, ticks, tag) (unfolded at WORLD_UNFOLDED), alternateWorldTick(sl,
 *               center, radius) every second, alternateWorldEnd(sl, center, radius, tag)
 *           omnipresence(sl, player, ticks, tag), omnipresenceEcho(sl, player, index), afterimageStruck(sl, player,
 *               index), omnipresenceEnd(sl, player, tag)
 *           closeTheDoor(sl, player, center, radius, ticks, tag) (shut at DOORS_SHUT), doorsClosed(sl, target, ticks,
 *               tag), forcedOut(sl, who)
 *   Passives: wormAura(sl, player, ticks, tag) (Secrets Sorcerer), intuition(owner, from), divinationFelt(owner),
 *             quickStudy(owner, caster), safeLanding(sl, player), illusionBroken(sl, player), afterimageMiss(sl,
 *             player), doorOfDoors(sl, player)
 *   Every timed cast: status(player, Status.X, ticks) shows its picture and its clock; statusEnd(player, Status.X).
 *
 * The models are in models/model_fx/key_of_stars/ (made by tools/model_gen/key_of_stars/models.py; each JSON's
 * "credit" gives its size and pivot), the pictures in textures/gui/key_of_stars/ and
 * textures/gui/ability_menus/key_of_stars/ (icons.py), the particles in textures/particle/fx/ (particles.py), the
 * sounds in sounds/key_of_stars/ (sounds.py) and sounds/ModSounds (STARS_...).
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class KeyOfStarsFx {

    private KeyOfStarsFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // Colors (ARGB)
    // =====================================================================================

    /** The Pathway's own color: pale blue starlight. */
    public static final int PATHWAY_COLOR = 0xFF9DB4FF;
    public static final int STARLIGHT = 0xFF9DB4FF;
    public static final int STAR_WHITE = 0xFFE8EEFF;
    public static final int GOLD = 0xFFFFD98A;
    public static final int INDIGO = 0xFF2A2A6E;
    public static final int NIGHT = 0xFF0E0C2A;
    public static final int STORM = 0xFF7A5AD8;
    public static final int FROST = 0xFFBEECFF;
    public static final int GREASE = 0xC0E8D890;

    /** The nine planets of Space Fragmentation, each its own color. */
    private static final int[] PLANETS = {0xFFF07A5A, 0xFF78C8FF, 0xFFFAD26E, 0xFFAA82FA, 0xFF78E6A0,
            0xFFFFFFFF, 0xFFE07AC8, 0xFF9DB4FF, 0xFFFFA050};

    // =====================================================================================
    // The small particles everything shares
    // =====================================================================================

    private static final ParticleFx STAR_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFE8EEFF).endColor(0x009DB4FF)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(-0.01f).friction(0.92f).glow();
    private static final ParticleFx GOLD_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFFE7A8).endColor(0x00C8962A)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(-0.015f).friction(0.92f).glow();
    private static final ParticleFx TWINKLE = ParticleFx.of("fx/star4")
            .color(0xFFFFFFFF).endColor(0x009DB4FF)
            .size(0.12f).endSize(0.02f).sizeRandom(0.5f)
            .lifetime(16, 10).gravity(0f).friction(0.9f).glow().spin(8f).randomRotation();
    private static final ParticleFx GOLD_TWINKLE = ParticleFx.of("fx/star4")
            .color(0xFFFFF0C8).endColor(0x00FFD98A)
            .size(0.12f).endSize(0.02f).sizeRandom(0.5f)
            .lifetime(16, 10).gravity(0f).friction(0.9f).glow().spin(-8f).randomRotation();
    private static final ParticleFx SPARKLE = ParticleFx.of("fx/sparkle")
            .color(0xFFFFFFFF).endColor(0x00C8D4FF)
            .size(0.1f).endSize(0.04f).sizeRandom(0.4f)
            .lifetime(14, 8).gravity(0f).friction(0.88f).glow().randomRotation();
    private static final ParticleFx DOOR_SPARK = ParticleFx.of("fx/star_door")
            .color(0xFFC8D4FF).endColor(0x002A2A6E)
            .size(0.12f).endSize(0.08f).sizeRandom(0.3f)
            .lifetime(20, 8).gravity(-0.01f).friction(0.9f).glow().spin(4f).randomRotation();
    private static final ParticleFx WORM = ParticleFx.of("fx/star_worm")
            .color(0xFFFFE7A8).endColor(0x009DB4FF)
            .size(0.1f).endSize(0.06f).sizeRandom(0.3f)
            .lifetime(26, 10).gravity(0f).friction(0.92f).glow().spin(10f).randomRotation();
    private static final ParticleFx KEY = ParticleFx.of("fx/star_key")
            .color(0xFFFFE7A8).endColor(0x00C8962A)
            .size(0.12f).endSize(0.1f).sizeRandom(0.3f)
            .lifetime(24, 8).gravity(0.02f).friction(0.92f).glow().spin(9f).randomRotation();
    private static final ParticleFx WIND_KEY = ParticleFx.of("fx/wind_key")
            .color(0xFFFFE7A8).endColor(0x00C8962A)
            .size(0.12f).endSize(0.1f)
            .lifetime(20, 6).gravity(-0.01f).friction(0.9f).glow().spin(14f).randomRotation();
    private static final ParticleFx WAVE = ParticleFx.of("fx/sound_wave")
            .color(0xFFFFF0D0).endColor(0x00FFA050)
            .size(0.16f).endSize(0.4f)
            .lifetime(12, 4).gravity(0f).friction(0.9f).glow();
    private static final ParticleFx PLANET = ParticleFx.of("fx/planet")
            .color(0xFFFFFFFF).endColor(0x009DB4FF)
            .size(0.12f).endSize(0.06f).sizeRandom(0.4f)
            .lifetime(24, 10).gravity(0.02f).friction(0.94f).glow().spin(6f).randomRotation();
    private static final ParticleFx CRACK = ParticleFx.of("fx/rift_crack")
            .color(0xFFE8EEFF).endColor(0x007A5AD8)
            .size(0.14f).endSize(0.08f).sizeRandom(0.4f)
            .lifetime(16, 6).gravity(0f).friction(0.85f).glow().randomRotation();
    private static final ParticleFx MAZE = ParticleFx.of("fx/maze")
            .color(0xC0C8D4FF).endColor(0x002A2A6E)
            .size(0.14f).endSize(0.14f).sizeRandom(0.3f)
            .lifetime(30, 10).gravity(-0.005f).friction(0.9f).glow();
    private static final ParticleFx PAGE = ParticleFx.of("fx/page")
            .color(0xFFE8ECFC).endColor(0x009DB4FF)
            .size(0.12f).endSize(0.1f).sizeRandom(0.3f)
            .lifetime(28, 10).gravity(0.01f).friction(0.93f).spin(9f).randomRotation();
    private static final ParticleFx KEYHOLE = ParticleFx.of("fx/keyhole")
            .color(0xFFFFE7A8).endColor(0x00C8962A)
            .size(0.12f).endSize(0.1f)
            .lifetime(20, 4).gravity(-0.01f).friction(0.9f).glow();
    private static final ParticleFx PADLOCK = ParticleFx.of("fx/padlock")
            .color(0xFFFFE7A8).endColor(0x00C8962A)
            .size(0.14f).endSize(0.1f)
            .lifetime(22, 4).gravity(0.02f).friction(0.9f).glow().spin(6f).randomRotation();
    private static final ParticleFx FROST_FLAKE = ParticleFx.of("fx/flake")
            .color(0xFFFFFFFF).endColor(0x00BEECFF)
            .size(0.08f).endSize(0.04f).sizeRandom(0.5f)
            .lifetime(22, 10).gravity(0.02f).friction(0.92f).glow().spin(6f).randomRotation();
    private static final ParticleFx GREASE_DROP = ParticleFx.of("fx/droplet")
            .color(0xE0E8D890).endColor(0x00A08840)
            .size(0.08f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(18, 8).gravity(0.04f).friction(0.9f).collide();
    private static final ParticleFx FLARE = ParticleFx.of("fx/flare")
            .color(0xFFFFFFFF).endColor(0x00FFF0C8)
            .size(0.5f).endSize(0.1f).sizeRandom(0.3f)
            .lifetime(6, 3).gravity(0f).friction(0.8f).glow();
    private static final ParticleFx WIND = ParticleFx.of("fx/wisp")
            .color(0xB0E8EEFF).endColor(0x00C8D4FF)
            .size(0.2f).endSize(0.35f).sizeRandom(0.4f)
            .lifetime(14, 6).gravity(0f).friction(0.94f).spin(6f).randomRotation();
    private static final ParticleFx NIGHT_SMOKE = ParticleFx.of("fx/smoke")
            .color(0xB0141238).endColor(0x00080618)
            .size(0.3f).endSize(0.8f).sizeRandom(0.3f)
            .lifetime(26, 12).gravity(-0.01f).friction(0.9f).spin(2f).randomRotation();
    private static final ParticleFx STORM_SMOKE = ParticleFx.of("fx/smoke")
            .color(0xA07A5AD8).endColor(0x00201040)
            .size(0.3f).endSize(0.8f).sizeRandom(0.3f)
            .lifetime(22, 10).gravity(-0.01f).friction(0.9f).spin(6f).randomRotation();
    private static final ParticleFx STORM_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFC8B4FF).endColor(0x002A1460)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(18, 8).gravity(0f).friction(0.86f).glow();
    private static final ParticleFx EYE_MARK = ParticleFx.of("fx/eye")
            .color(0xFFC8D4FF).endColor(0x009DB4FF)
            .size(0.12f).endSize(0.1f)
            .lifetime(24, 0).gravity(0f).friction(0f).glow();

    private static void stars(ServerLevel sl, Vec3 at, int count, double minSpeed, double maxSpeed) {
        for (int i = 0; i < count; i++) {
            ParticleShapes.burst(sl, i % 3 == 0 ? GOLD_TWINKLE : TWINKLE, at, 1, minSpeed, maxSpeed);
        }
    }

    // =====================================================================================
    // Lines and arcs
    // =====================================================================================

    private static final SlashFx STAR_LINE = SlashFx.line("slash/smooth")
            .color(0xC09DB4FF).core(0xFFFFFFFF).width(0.06f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx STAR_BEAM = SlashFx.line("slash/smooth")
            .color(0xE09DB4FF).core(0xFFFFFFFF).width(0.22f).taper(SlashFx.Taper.COMET).lifetime(8).sweep(2);
    private static final SlashFx STAR_TRAIL = SlashFx.line("slash/streaks")
            .color(0x909DB4FF).core(0x80FFFFFF).width(0.5f).taper(SlashFx.Taper.COMET).lifetime(8).sweep(2);
    private static final SlashFx FROST_BEAM = SlashFx.line("slash/crystal")
            .color(0xE0BEECFF).core(0xFFFFFFFF).width(0.25f).taper(SlashFx.Taper.COMET).lifetime(7).sweep(2);
    private static final SlashFx GALE_ARC = SlashFx.arc("slash/streaks")
            .color(0xA0E8EEFF).headColor(0xE0FFFFFF).core(0x80FFFFFF)
            .radius(2.4f).arc(100f).width(0.5f).layers(3).spread(0.4f).lifetime(8).sweep(3);
    private static final SlashFx HAND_LINE = SlashFx.line("slash/dashed_fine")
            .color(0xC09DB4FF).core(0x80FFFFFF).width(0.08f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx TEAR_ARC = SlashFx.arc("slash/shatter")
            .color(0xF0C8D4FF).headColor(0xFFFFFFFF).tailColor(0xA07A5AD8).core(0xFFFFFFFF)
            .radius(1.8f).arc(160f).width(0.6f).layers(2).spread(0.2f).lifetime(8).sweep(2);
    private static final SlashFx SEAL_THREAD = SlashFx.line("slash/smooth")
            .color(0xC0FFD98A).core(0xFFFFFFFF).width(0.05f).taper(SlashFx.Taper.UNIFORM);

    // =====================================================================================
    // Sounds
    // =====================================================================================

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

    private static Vec3 ahead(float yaw) {
        double angle = Math.toRadians(yaw);
        return new Vec3(-Math.sin(angle), 0, Math.cos(angle));
    }

    private static float yawToward(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    private static float randomYaw(ServerLevel sl) {
        return sl.getRandom().nextFloat() * 360f;
    }

    private static Vec3 handOf(Player player, ServerLevel sl) {
        Vec3 hand = SpellFx.handOf(player, sl);
        return hand == null ? chestOf(player) : hand;
    }

    // =====================================================================================
    // Statuses: a picture in the row above the hotbar stacks, and a clock beside the hotbar
    // =====================================================================================

    public enum Status {
        // what the Key Of Stars Beyonder holds
        INTERFERENCE("interference", "Interference", 0x9DB4FF),
        RECORD("record", "Record", 0xE8ECFC),
        WANDERING("wandering", "Wandering", 0xC8D4FF),
        SPACE_CONCEALMENT("space_concealment", "Space Concealment", 0x9DB4FF),
        TRANSFIGURATION("transfiguration", "Transfiguration", 0xC8D4FF),
        OMNIPRESENCE("omnipresence", "Omnipresence", 0xE8EEFF),
        ALTERNATE_WORLD("alternate_world", "Alternate World", 0x9DB4FF),
        LABYRINTH("labyrinth", "Labyrinth", 0x9DB4FF),
        SEALED_ALLY("sealed_ally", "Sealed Connections", 0xFFD98A),
        QUICKENED("quickened", "Quickened", 0xC8D4FF),
        UNBOUND("unbound", "Unbound", 0xFFD98A),
        DIMENSION_SIGHT("dimension_sight", "Dimension Sight", 0x9DB4FF),
        // what is done to others (when they are players)
        STARLIT("starlit", "Starlit", 0x9DB4FF),
        SEALED("sealed", "Sealed", 0xFFD98A),
        EXILED("exiled", "Exiled", 0x7A5AD8),
        TOY("toy", "Toy", 0xE05050),
        BLINDED("blinded", "Blinded", 0xFFFFFF),
        SLIPPING("slipping", "Slipping", 0xE8D890),
        FROZEN("frozen", "Frozen", 0xBEECFF),
        DEAFENED("deafened", "Deafened", 0xFFA050),
        LOST("lost", "Lost", 0x9DB4FF),
        DOORS_CLOSED("doors_closed", "Doors Closed", 0xFFD98A),
        STORM("storm", "Space-Time Storm", 0x7A5AD8),
        BANISHED("banished", "Banished", 0x7A5AD8);

        final String image;
        final String label;
        final int rgb;

        Status(String image, String label, int rgb) {
            this.image = image;
            this.label = label;
            this.rgb = rgb;
        }

        String id() {
            return "ks_status_" + name().toLowerCase();
        }
    }

    /** Shows the status's picture (in the row above the hotbar stacks, rows of nine) and a clock beside the hotbar. */
    public static void status(Player player, Status status, int ticks) {
        status(player, status, "key_of_stars/" + status.image, ticks);
    }

    private static void status(Player player, Status status, String image, int ticks) {
        int slot = status.ordinal();
        ScreenImages.show(player, status.id(), image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, ticks);
        HudTimer.show(player, status.id(), status.label, ticks, status.rgb);
    }

    /** It ended early. */
    public static void statusEnd(Player player, Status status) {
        ScreenImages.hide(player, status.id());
        HudTimer.hide(player, status.id());
    }

    /** Transfiguration's picture shows how many layers are left (1 to 8). Call it again after every torn layer. */
    public static void layersLeft(Player player, int layers, int ticks) {
        if (layers <= 0) {
            statusEnd(player, Status.TRANSFIGURATION);
            return;
        }
        status(player, Status.TRANSFIGURATION, "key_of_stars/layers_" + Math.min(8, layers), ticks);
    }

    // ---------- the Recorded Powers' pages, in the row right of the hotbar (where Attendant's stacks are)

    private static String pageId(int slot) {
        return "ks_recorded_" + slot;
    }

    /**
     * The Recorded Powers, oldest first: one page for each (angel[i] = an Angel-level power, recorded weakly, shown
     * on a gold page). Call it every time a slot fills or empties, and after a relog; it stays until changed.
     */
    public static void recordedPages(Player player, boolean[] angel) {
        for (int i = 0; i < 5; i++) {
            if (i < angel.length) {
                ScreenImages.show(player, pageId(i), angel[i] ? "key_of_stars/recorded_angel" : "key_of_stars/recorded",
                        ScreenAnchor.BOTTOM, 103 + i * 18, -3, 16, ScreenImages.FOREVER);
            } else {
                ScreenImages.hide(player, pageId(i));
            }
        }
    }

    public static void recordedPagesHide(Player player) {
        for (int i = 0; i < 5; i++) ScreenImages.hide(player, pageId(i));
    }

    // =====================================================================================
    // The wheels' pictures (textures/gui/ability_menus/key_of_stars/, for AbilityWheel.entry's icon)
    // =====================================================================================

    public static final String WHEEL_FLASH = "ability_menus/key_of_stars/flash";
    public static final String WHEEL_BLACK_SCREEN = "ability_menus/key_of_stars/black_screen";
    public static final String WHEEL_LOUD_NOISE = "ability_menus/key_of_stars/loud_noise";
    public static final String WHEEL_FROST_RAY = "ability_menus/key_of_stars/frost_ray";
    public static final String WHEEL_GALE = "ability_menus/key_of_stars/gale";
    public static final String WHEEL_TUMBLE = "ability_menus/key_of_stars/tumble";
    public static final String WHEEL_ESCAPE_TRICK = "ability_menus/key_of_stars/escape_trick";
    public static final String WHEEL_TRAVELERS_DOOR = "ability_menus/key_of_stars/travelers_door";
    public static final String WHEEL_BLINK = "ability_menus/key_of_stars/blink";
    public static final String WHEEL_INVISIBLE_HAND = "ability_menus/key_of_stars/invisible_hand";
    public static final String WHEEL_STAR_ANCHOR = "ability_menus/key_of_stars/star_anchor";
    public static final String WHEEL_WANDERING = "ability_menus/key_of_stars/wandering";
    public static final String WHEEL_SPACE_CONCEALMENT = "ability_menus/key_of_stars/space_concealment";
    public static final String WHEEL_TRANSFIGURATION = "ability_menus/key_of_stars/transfiguration";
    public static final String WHEEL_EXILE = "ability_menus/key_of_stars/exile";
    public static final String WHEEL_POCKET_SPACE = "ability_menus/key_of_stars/pocket_space";
    public static final String WHEEL_SPACE_TEARING = "ability_menus/key_of_stars/space_tearing";
    public static final String WHEEL_TOY_BOX = "ability_menus/key_of_stars/toy_box";
    public static final String WHEEL_REPLICATION = "ability_menus/key_of_stars/replication";
    public static final String WHEEL_SPACE_FRAGMENTATION = "ability_menus/key_of_stars/space_fragmentation";
    public static final String WHEEL_STARLIGHT_SEAL = "ability_menus/key_of_stars/starlight_seal";
    public static final String WHEEL_LABYRINTH = "ability_menus/key_of_stars/labyrinth";
    public static final String WHEEL_ALTERNATE_WORLD = "ability_menus/key_of_stars/alternate_world";
    public static final String WHEEL_OMNIPRESENCE = "ability_menus/key_of_stars/omnipresence";
    public static final String WHEEL_CLOSE_THE_DOOR = "ability_menus/key_of_stars/close_the_door";

    // =====================================================================================
    // The models (models/model_fx/key_of_stars/, see each JSON's "credit")
    // =====================================================================================

    /** An illusory Door: the frame (1 x 2 blocks, pivot the middle of its threshold) and the door (pivot its hinge). */
    public static final ModelFx DOOR_FRAME = ModelFx.of("key_of_stars/door_frame").glow().aura(0x809DB4FF, 0.08f, 2);
    public static final ModelFx DOOR_LEAF = ModelFx.of("key_of_stars/door_leaf").pivot(2, 0, 8).glow().seeThrough();
    /** Star Divination's crystal ball, 0.8 blocks; pivot the middle of its stand. */
    public static final ModelFx CRYSTAL_BALL = ModelFx.of("key_of_stars/crystal_ball").glow().seeThrough()
            .aura(0x809DB4FF, 0.1f, 3);
    /** A Worm of Star, 0.7 blocks; pivot the middle of its curl. */
    public static final ModelFx WORM_OF_STAR = ModelFx.of("key_of_stars/worm_of_star").pivot(8, 6, 8).glow().unshaded();
    /** The Key of Stars, 1.7 blocks, bow up; pivot the middle of its shaft. */
    public static final ModelFx STAR_KEY = ModelFx.of("key_of_stars/star_key").pivot(8, 15, 8).glow().unshaded()
            .aura(0x80FFD98A, 0.08f, 2);
    /** A toy's wind-up key, 0.6 blocks, shaft north; pivot the tip of its shaft. */
    public static final ModelFx WIND_UP_KEY = ModelFx.of("key_of_stars/wind_up_key").pivot(8, 8, 8).glow();
    /** The Invisible Hand, 1.4 blocks, palm north, fingers up; pivot the wrist. */
    public static final ModelFx STAR_HAND = ModelFx.of("key_of_stars/star_hand").glow().unshaded().seeThrough()
            .aura(0x609DB4FF, 0.08f, 2);
    /** The Scribe's open book (pivot the middle of its spine) and quill (pivot the nib). */
    public static final ModelFx RECORD_BOOK = ModelFx.of("key_of_stars/record_book").glow();
    public static final ModelFx QUILL = ModelFx.of("key_of_stars/quill").glow();
    /** Flat pictures, 1 block, white (tint them): the seals and circles on the ground. */
    public static final ModelFx STAR_GATE = ModelFx.of("key_of_stars/star_gate").glow().unshaded().seeThrough();
    public static final ModelFx CONSTELLATION = ModelFx.of("key_of_stars/constellation").glow().unshaded().seeThrough();
    public static final ModelFx ANCHOR_SEAL = ModelFx.of("key_of_stars/star_anchor").glow().unshaded().seeThrough();
    public static final ModelFx TUMBLE_CIRCLE = ModelFx.of("key_of_stars/tumble_circle").glow().unshaded().seeThrough();
    public static final ModelFx LABYRINTH_FLOOR = ModelFx.of("key_of_stars/labyrinth").glow().unshaded().seeThrough();
    public static final ModelFx TOY_VEIL = ModelFx.of("key_of_stars/toy_veil").glow().unshaded().seeThrough();
    public static final ModelFx SEAL_RING = ModelFx.of("key_of_stars/seal_ring").glow().unshaded().seeThrough();
    public static final ModelFx DOOR_KINGDOM = ModelFx.of("key_of_stars/door_kingdom").glow().unshaded().seeThrough();
    public static final ModelFx PLANET_RING = ModelFx.of("key_of_stars/planet_ring").glow().unshaded().seeThrough();
    /** Standing pictures, 1 block, white (tint them); pivot their middle. */
    public static final ModelFx DOOR_SIGIL = ModelFx.of("key_of_stars/door_sigil").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx STARLIT_MARK = ModelFx.of("key_of_stars/starlit_mark").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx INTERFERENCE_VEIL = ModelFx.of("key_of_stars/interference").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx STORM_VORTEX = ModelFx.of("key_of_stars/storm_vortex").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** Standing pictures in their own colors (leave them untinted); pivot their middle. */
    public static final ModelFx SPACE_TEAR = ModelFx.of("key_of_stars/space_tear").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx RECORD_PAGE = ModelFx.of("key_of_stars/record_page").pivot(8, 8, 8).glow().seeThrough();
    /** Black Screen's curtain, 1 block, its own colors, animated; pivot the middle of its bottom edge. */
    public static final ModelFx CURTAIN = ModelFx.of("key_of_stars/black_screen").seeThrough();
    /** A panel of split-off space, 1 block, standing, animated; pivot the middle of its bottom edge. */
    public static final ModelFx SPACE_SHELL = ModelFx.of("key_of_stars/space_shell").glow().unshaded().seeThrough();
    /** Close the Door's barred door, 1 block, standing; pivot the middle of its bottom edge. */
    public static final ModelFx CLOSED_DOOR = ModelFx.of("key_of_stars/closed_door").glow().unshaded().seeThrough();
    /** An afterimage of a person, 1 x 2 blocks, standing; pivot the middle of its feet. */
    public static final ModelFx AFTERIMAGE = ModelFx.of("key_of_stars/afterimage").glow().unshaded().seeThrough();
    /** The shared soft orb (models/model_fx/orb.json): the planets. */
    private static final ModelFx ORB = ModelFx.of("orb").pivot(8, 8, 8).glow().unshaded().seeThrough();

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

    /** A ring of standing panels round a center: a wall. 'height' in blocks; each panel about 'width' blocks wide. */
    private static void wallRing(ServerLevel sl, ModelFx panel, Vec3 center, double radius, double height, int color,
                                 int ticks, int risen, String tag) {
        int count = Math.max(8, Math.min(40, (int) (Math.PI * 2 * radius / 2.0)));
        float width = (float) (Math.PI * 2 * radius / count) * 1.05f;
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            Vec3 at = center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            float yaw = yawToward(at, center);
            ModelFx one = panel.color(color).scale(width, (float) height, 1f).lifetime(ticks)
                    .fade(0, Math.min(16, ticks / 3)).tag(tag)
                    .key(0, pose().up((float) -height).alpha(0f))
                    .key(risen, pose().up(0f).alpha(1f), Ease.OUT);
            ParticleShapes.model(sl, one, at, yaw, 0f, 0f);
        }
    }

    /**
     * An illusory Door standing at 'at' (the middle of its threshold), facing 'yaw', 'size' (1 = 1 x 2 blocks): the
     * frame appears, the door swings open at 'opens' and shut again at 'shuts' (or stays open when shuts <= opens),
     * gone after 'ticks'.
     */
    private static void door(ServerLevel sl, Vec3 at, float yaw, float size, int color, int opens, int shuts, int ticks,
                             String tag) {
        ModelFx frame = DOOR_FRAME.color(color).scale(size).lifetime(ticks).fade(3, 8)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(opens, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ModelFx leaf = DOOR_LEAF.color(0xF0FFFFFF).scale(size).lifetime(ticks).fade(3, 8)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(opens, pose().scale(1f).alpha(1f).yaw(0f), Ease.OUT_BACK)
                .key(opens + 6, pose().yaw(-100f), Ease.OUT);
        if (shuts > opens) leaf = leaf.key(shuts, pose().yaw(-100f)).key(shuts + 4, pose().yaw(0f), Ease.IN);
        if (tag != null) {
            frame = frame.tag(tag);
            leaf = leaf.tag(tag);
        }
        ParticleShapes.model(sl, frame, at, yaw, 0f, 0f);
        ParticleShapes.model(sl, leaf, at, yaw, 0f, 0f);
    }

    /** An afterimage of 'who' at 'at', facing 'yaw', fading over 'ticks'. */
    private static void afterimage(ServerLevel sl, Entity who, Vec3 at, float yaw, int color, int ticks) {
        float h = Math.max(0.5f, who.getBbHeight() / 2f);
        ModelFx ghost = AFTERIMAGE.color(color).scale(h).lifetime(ticks).fade(0, ticks)
                .key(0, pose().alpha(1f))
                .key(ticks, pose().up(0.1f).alpha(0f), Ease.IN);
        ParticleShapes.model(sl, ghost, at, yaw, 0f, 0f);
    }

    // =====================================================================================
    // Key 1: Door Opening / Slip Free (Apprentice)
    // =====================================================================================

    /** The tick the Door stands open: the step through goes there. */
    public static final int DOOR_OPENED = 6;

    /**
     * Door Opening: an illusory Door opens in the wall at 'from' (where the Apprentice faces it) and another at 'to' on
     * the far side; the Apprentice steps through at DOOR_OPENED and both shut behind them.
     */
    public static void doorOpening(ServerLevel sl, Player player, Vec3 from, Vec3 to) {
        float yaw = player.getYRot();
        door(sl, from, yaw, 1.05f, 0xFFE8EEFF, DOOR_OPENED, DOOR_OPENED + 8, 30, null);
        ParticleShapes.burst(sl, DOOR_SPARK, from.add(0, 1.0, 0), 6, 0.03, 0.08);
        sound(sl, from, ModSounds.STARS_DOOR_OPEN, 1.0f, wobble(1.0f));
        Later.run(sl, DOOR_OPENED, () -> {
            door(sl, to, yaw + 180f, 1.05f, 0xFFE8EEFF, 2, 12, 26, null);
            doorStep(sl, player, to);
        });
    }

    /** Someone comes out of a Door at 'at' (the Apprentice, or one who came through with them from the Astrologer up). */
    public static void doorStep(ServerLevel sl, Entity who, Vec3 at) {
        ParticleShapes.burst(sl, STAR_MOTE, at.add(0, 1.0, 0), 12, 0.04, 0.12);
        stars(sl, at.add(0, 1.0, 0), 5, 0.04, 0.1);
        SpellFx.shockRing(sl, at, 1.4, 0xC09DB4FF, 8, 0);
        sound(sl, at, ModSounds.STARS_DOOR_STEP, 0.9f, wobble(1.0f));
    }

    /** A real door, trapdoor or fence gate swung open by Door Opening: a key turns in it. */
    public static void realDoor(ServerLevel sl, Vec3 at) {
        ParticleShapes.spawn(sl, KEYHOLE, at, Vec3.ZERO);
        ParticleShapes.burst(sl, GOLD_MOTE, at, 5, 0.02, 0.05);
        sound(sl, at, ModSounds.STARS_KEY_TURN, 0.6f, wobble(1.2f));
    }

    // ---------- sneak: Slip Free

    /** The lock on 'target' (the caster or an ally) springs open: keys and locks fly off it, it is Unbound a moment. */
    public static void slipFree(ServerLevel sl, Player caster, LivingEntity target) {
        if (target != caster) ParticleShapes.slashBetween(sl, STAR_BEAM, handOf(caster, sl), chestOf(target));
        ModelFx key = STAR_KEY.scale(0.45f).lifetime(18).fade(2, 8)
                .key(0, pose().roll(0f).alpha(0f))
                .key(4, pose().alpha(1f), Ease.OUT)
                .key(10, pose().roll(90f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, key, target, new Vec3(0, target.getBbHeight() * 0.6, 0), target.getYRot() + 90f, 0f, 0f);
        Later.run(sl, 10, () -> {
            if (!target.isAlive()) return;
            ParticleShapes.burst(sl, PADLOCK, chestOf(target), 4, 0.08, 0.16);
            ParticleShapes.burst(sl, KEY, chestOf(target), 4, 0.06, 0.14);
            ParticleShapes.ring(sl, GOLD_MOTE, target.position().add(0, 0.3, 0), Math.max(0.6, target.getBbWidth()), 12, 0.12);
            sound(sl, chestOf(target), ModSounds.STARS_SLIP_FREE, 1.0f, wobble(1.0f));
        });
        if (target instanceof Player p) status(p, Status.UNBOUND, 30);
    }

    /** The Secrets Sorcerer opens the Door through a Teleport root on themselves: a door in a ring of breaking locks. */
    public static void travelUnbarred(ServerLevel sl, Player player) {
        flash(sl, DOOR_SIGIL, chestOf(player).add(ahead(player.getYRot()).scale(0.6)), player.getYRot(), 0xFFFFD98A, 1.6f, 16);
        ParticleShapes.ring(sl, PADLOCK, chestOf(player), 1.0, 6, 0.12);
        sound(sl, player.position(), ModSounds.STARS_KEY_TURN, 1.0f, wobble(0.9f));
    }

    // =====================================================================================
    // Key 2: Tricks (Trickmaster)
    // =====================================================================================

    /** Flash: a blinding flash many times brighter than a camera, in a cone 'range' blocks ahead. */
    public static void flash(ServerLevel sl, Player player, double range) {
        Vec3 hand = handOf(player, sl);
        Vec3 look = player.getLookAngle();
        ParticleShapes.spawn(sl, FLARE.size(1.4f), hand.add(look.scale(0.4)), Vec3.ZERO);
        ParticleShapes.cone(sl, FLARE, hand, look, 30, 18, range * 0.06, range * 0.1);
        ParticleShapes.cone(sl, SPARKLE, hand, look, 35, 16, 0.2, 0.6);
        ModelFx glare = ORB.color(0xFFFFFFFF).lifetime(6).fade(0, 5)
                .key(0, pose().scale(1f))
                .key(3, pose().scale(4f).alpha(1f), Ease.OUT)
                .key(6, pose().scale(5f).alpha(0f));
        ParticleShapes.model(sl, glare, hand.add(look.scale(1.2)), 0f, 0f, 0f);
        sound(sl, hand, ModSounds.STARS_FLASH, 1.2f, wobble(1.0f));
    }

    /** Blinded by the Flash: stars swim over its eyes. */
    public static void blinded(ServerLevel sl, LivingEntity target, int ticks) {
        Vec3 eyes = target.getEyePosition();
        for (int i = 0; i < 4; i++) {
            final int k = i;
            Later.run(sl, i * 8, () -> {
                if (target.isAlive()) ParticleShapes.burst(sl, TWINKLE, eyes.add(0, 0.2 + k * 0.05, 0), 3, 0.01, 0.04);
            });
        }
        if (target instanceof Player p) status(p, Status.BLINDED, ticks);
    }

    /** The tick the curtain is all the way down. */
    public static final int SCREEN_DOWN = 6;

    /** Black Screen: a dark curtain 6 wide and 3 tall drops at 'center' (the middle of its bottom edge), facing 'yaw'. */
    public static void blackScreen(ServerLevel sl, Vec3 center, float yaw, int ticks, String tag) {
        ModelFx curtain = CURTAIN.scale(6f, 3f, 1f).lifetime(ticks).fade(0, 10).tag(tag)
                .key(0, pose().up(3f).scale(0.02f))
                .key(SCREEN_DOWN, pose().up(0f).scale(1f), Ease.OUT_BOUNCE);
        ParticleShapes.model(sl, curtain, center, yaw, 0f, 0f);
        Vec3 side = ahead(yaw + 90f);
        ParticleShapes.line(sl, STAR_MOTE, center.add(0, 3.0, 0).add(side.scale(-3)), center.add(0, 3.0, 0).add(side.scale(3)), 2.5);
        ParticleShapes.burst(sl, NIGHT_SMOKE, center.add(0, 0.3, 0), 6, 0.02, 0.06);
        sound(sl, center.add(0, 1.5, 0), ModSounds.STARS_BLACK_SCREEN, 1.0f, wobble(1.0f));
    }

    public static void blackScreenEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.burst(sl, NIGHT_SMOKE, center.add(0, 1.5, 0), 10, 0.02, 0.08);
    }

    /** Loud Noise: a deafening bang at 'at', a fake explosion with nothing behind it. */
    public static void loudNoise(ServerLevel sl, Player player, Vec3 at) {
        SpellFx.blast(sl, at.add(0, 0.8, 0), 2.2, 0xFFFFE0A0, SPARKLE);
        SpellFx.shockRing(sl, at, 8.0, 0xC0FFF0D0, 10, 0);
        SpellFx.shockRing(sl, at, 5.0, 0x90FFA050, 8, 3);
        ParticleShapes.ring(sl, WAVE, at.add(0, 1.0, 0), 1.0, 10, 0.35);
        sound(sl, at, ModSounds.STARS_LOUD_NOISE, 2.0f, wobble(1.0f));
    }

    /** Deafened by the bang: rings of sound round its head. */
    public static void deafened(ServerLevel sl, LivingEntity target, int ticks) {
        ParticleShapes.ring(sl, WAVE.size(0.1f).endSize(0.2f), headOf(target), 0.5, 6, 0.04);
        if (target instanceof Player p) status(p, Status.DEAFENED, ticks);
    }

    /** Frost Ray: a ray of frost from the hand to 'to' (the end of the shot, or what it hit). */
    public static void frostRay(ServerLevel sl, Player player, Vec3 from, Vec3 to) {
        ParticleShapes.slashBetween(sl, FROST_BEAM, from, to);
        ParticleShapes.line(sl, FROST_FLAKE, from, to, 3.0);
        ParticleShapes.burst(sl, FROST_FLAKE, from, 6, 0.03, 0.08);
        sound(sl, from, ModSounds.STARS_FROST_RAY, 1.0f, wobble(1.0f));
    }

    /** Frosted: frost crystals on it for 'ticks' (no ice in the world), and any fire on it hisses out. */
    public static void frosted(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        float size = Math.max(0.6f, Math.min(target.getBbWidth() * 1.6f, target.getBbHeight() * 0.8f));
        ModelFx crystals = ModelFx.of("ice_crystals").color(0xC0E0F6FF).scale(size)            // its base is its pivot
                .lifetime(ticks).fade(2, 8).glow().seeThrough().aura(0x5560D8FF, 0.05f, 2).tag(tag)
                .key(0, pose().scale(0.3f))
                .key(4, pose().scale(1f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, crystals, target, Vec3.ZERO, randomYaw(sl), 0f, 0f);
        ParticleShapes.burst(sl, FROST_FLAKE, chestOf(target), 12, 0.04, 0.12);
        if (target instanceof Player p) status(p, Status.FROZEN, ticks);
    }

    /** Gale: a level 7 to 8 gale in a cone 'range' blocks ahead. */
    public static void gale(ServerLevel sl, Player player, double range) {
        Vec3 chest = chestOf(player);
        Vec3 look = Spells.flatLook(player);
        float yaw = player.getYRot();
        for (int i = 0; i < 3; i++) {
            final int k = i;
            Later.run(sl, i * 2, () -> ParticleShapes.slash(sl, GALE_ARC.radius(2.4f + k * 1.6f),
                    chest.add(look.scale(1.0 + k * 1.5)), yaw, 0f, (k - 1) * 12f));
        }
        ParticleShapes.cone(sl, WIND, chest, look, 30, 24, range * 0.05, range * 0.09);
        ParticleShapes.cone(sl, STAR_MOTE, chest, look, 35, 10, range * 0.04, range * 0.08);
        sound(sl, chest, ModSounds.STARS_GALE, 1.4f, wobble(1.0f));
    }

    /** Something caught in the Gale: a gust tears past it (fire on it goes out). */
    public static void galeHit(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, WIND, chestOf(target), 5, 0.06, 0.14);
    }

    /** Tumble: a greased circle 'radius' blocks wide lies on the ground at 'center' for 'ticks'. */
    public static void tumble(ServerLevel sl, Player player, Vec3 center, double radius, int ticks, String tag) {
        floor(sl, TUMBLE_CIRCLE, center, radius, GREASE, ticks, 1.2f, tag, 0);
        ParticleShapes.burst(sl, GREASE_DROP, center.add(0, 0.5, 0), 16, 0.08, 0.18);
        sound(sl, center, ModSounds.STARS_TUMBLE, 1.0f, wobble(1.0f));
    }

    /** Every half second while it lies there: slick drops glint on it. */
    public static void tumbleTick(ServerLevel sl, Vec3 center, double radius) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = r.nextDouble() * radius;
            ParticleShapes.spawn(sl, GREASE_DROP, center.add(Math.cos(a) * d, 0.1, Math.sin(a) * d), new Vec3(0, 0.08, 0));
        }
    }

    /** It slips: a splash of grease under its feet, and a stagger. */
    public static void slipped(ServerLevel sl, LivingEntity target, int ticks) {
        ParticleShapes.burst(sl, GREASE_DROP, target.position().add(0, 0.2, 0), 8, 0.06, 0.14);
        ParticleShapes.spawn(sl, TWINKLE, headOf(target).add(0, 0.3, 0), Vec3.ZERO);
        if (target instanceof Player p) status(p, Status.SLIPPING, ticks);
    }

    public static void tumbleEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
    }

    /**
     * Escape Trick: a crumbling door at 'from', a streak of starlight to 'to'. diversion = another trick was used in
     * the last 3 seconds (the long shift); without one it is a small hop.
     */
    public static void escapeTrick(ServerLevel sl, Player player, Vec3 from, Vec3 to, boolean diversion) {
        float yaw = yawToward(from, to);
        ModelFx crumbling = DOOR_SIGIL.color(0xE0C8D4FF).scale(1.6f).lifetime(18).fade(0, 14)
                .key(0, pose().up(0.9f).alpha(1f))
                .key(18, pose().up(0.3f).scale(0.9f).alpha(0f), Ease.IN);
        ParticleShapes.model(sl, crumbling, from, yaw + 180f, 0f, 0f);
        afterimage(sl, player, from, player.getYRot(), 0xC0C8D4FF, diversion ? 16 : 10);
        ParticleShapes.burst(sl, DOOR_SPARK, from.add(0, 1.0, 0), diversion ? 8 : 4, 0.04, 0.1);
        ParticleShapes.slashBetween(sl, STAR_TRAIL, from.add(0, 1.0, 0), to.add(0, 1.0, 0));
        stars(sl, to.add(0, 1.0, 0), diversion ? 6 : 3, 0.03, 0.08);
        sound(sl, from, ModSounds.STARS_ESCAPE_TRICK, 1.0f, wobble(diversion ? 1.0f : 1.2f));
    }

    // =====================================================================================
    // Key 3: Star Divination / Interference (Astrologer)
    // =====================================================================================

    /** The tick the crystal ball has found its target: Starlit goes there. */
    public static final int DIVINED = 10;

    /**
     * Star Divination: a crystal ball of starlight forms in the hand. On a target, a thread of starlight reaches it at
     * DIVINED; with no target (null), a map of the sky sweeps out round the Astrologer.
     */
    public static void starDivination(ServerLevel sl, Player player, LivingEntity target) {
        ModelFx ball = CRYSTAL_BALL.scale(0.5f).lifetime(30).fade(4, 10)
                .key(0, pose().forward(0.45f).right(0.32f).up(0.1f).scale(0.2f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOnTurning(sl, ball, player, new Vec3(0, player.getEyeHeight() - 0.75, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, STAR_MOTE, handOf(player, sl), 8, 0.02, 0.06);
        sound(sl, player.position(), ModSounds.STARS_DIVINATION, 1.0f, wobble(1.0f));
        if (target == null) {
            floor(sl, CONSTELLATION, player.position(), 4.0, 0xC09DB4FF, 40, 2f, null, 4);
            SpellFx.steadyRing(sl, player.position(), 40.0, 0x809DB4FF, 24, 6);
            return;
        }
        Later.run(sl, DIVINED, () -> {
            if (!target.isAlive()) return;
            ParticleShapes.slashBetween(sl, STAR_LINE.lifetime(14).sweep(4), handOf(player, sl), chestOf(target));
            stars(sl, chestOf(target), 6, 0.03, 0.08);
        });
    }

    /** The sweep found 'who': a small star over it, through walls, for 'ticks' (only the owner sees it). */
    public static void smallStar(ServerPlayer owner, LivingEntity who, int ticks) {
        Vec3 offset = new Vec3(0, who.getBbHeight() + 0.4, 0);
        ModelFx mark = STARLIT_MARK.color(0xC0C8D4FF).scale(0.45f).lifetime(ticks).fade(4, 8).following(who, offset);
        ParticleShapes.spawnFor(owner, mark.rotated(yawToward(who.position(), owner.position()), 0f, 0f),
                who.position().add(offset), Vec3.ZERO);
    }

    // ---------- the Starlit mark

    /** 'target' is Starlit by 'caster': the mark rises over it with a chime; it stays for 'ticks' (its tag). */
    public static void starlit(ServerLevel sl, Entity caster, LivingEntity target, int ticks, String tag) {
        ModelFx mark = STARLIT_MARK.color(0xE0C8D4FF).scale(0.7f).lifetime(ticks).fade(4, 10).spin(3f).tag(tag)
                .key(0, pose().scale(2f).alpha(0f))
                .key(8, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.55, 0), 0f, 0f, 0f);
        stars(sl, chestOf(target), 8, 0.03, 0.1);
        sound(sl, chestOf(target), ModSounds.STARS_STARLIT, 0.9f, wobble(1.0f));
        if (target instanceof Player p) status(p, Status.STARLIT, ticks);
    }

    /**
     * Each second while it is Starlit: 'viewer' (the caster or one of their allies) sees it through walls for a little
     * over a second. Call it for every viewer who should.
     */
    public static void starlitSeen(ServerPlayer viewer, LivingEntity who) {
        seenThroughWalls(viewer, who, 0xA09DB4FF, 26);
    }

    /** Dimension Sight (Planeswalker): a Starlit creature far away, seen wherever it is in this dimension. */
    public static void dimensionSight(ServerPlayer owner, LivingEntity who) {
        seenThroughWalls(owner, who, 0xC0FFD98A, 26);
        ParticleShapes.spawnFor(owner, PLANET.color(0xFFFFD98A), headOf(who).add(0, 0.9, 0), Vec3.ZERO);
    }

    private static void seenThroughWalls(ServerPlayer viewer, LivingEntity who, int color, int ticks) {
        Vec3 offset = new Vec3(0, who.getBbHeight() * 0.55, 0);
        ModelFx mark = STARLIT_MARK.color(color).scale(Math.max(0.8f, who.getBbHeight() * 0.8f)).lifetime(ticks).fade(2, 4)
                .following(who, offset);
        ParticleShapes.spawnFor(viewer, mark.rotated(yawToward(who.position(), viewer.position()), 0f, 0f),
                who.position().add(offset), Vec3.ZERO);
    }

    public static void starlitEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, STAR_MOTE, headOf(target).add(0, 0.4, 0), 5, 0.01, 0.04);
        if (target instanceof Player p) statusEnd(p, Status.STARLIT);
    }

    // ---------- sneak: Interference

    /** Interference: a turning veil of broken lines closes round the Astrologer; for 'ticks' they are Unreadable. */
    public static void interference(ServerLevel sl, Player player, int ticks, String tag) {
        ModelFx veil = INTERFERENCE_VEIL.color(0x90C8D4FF).scale(Math.max(2.2f, player.getBbHeight() * 1.1f))
                .lifetime(ticks).fade(8, 12).tag(tag)
                .key(0, pose().scale(2.5f).alpha(0f))
                .key(10, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOnTurning(sl, veil, player, new Vec3(0, player.getBbHeight() * 0.55, 0), 0f, 0f, 0f);
        SpellFx.closingRing(sl, player.position(), 3.0, 0xC09DB4FF, 10, 0);
        ParticleShapes.burst(sl, SPARKLE, chestOf(player), 12, 0.05, 0.14);
        sound(sl, player.position(), ModSounds.STARS_INTERFERENCE, 1.0f, wobble(1.0f));
        status(player, Status.INTERFERENCE, ticks);
    }

    /** Now and then while Unreadable: a few sparks of static drift off the Astrologer. */
    public static void interferenceTick(ServerLevel sl, Player player) {
        ParticleShapes.burst(sl, SPARKLE, chestOf(player), 2, 0.02, 0.06);
    }

    /** A search or a lock-on failed and is turned back: a line of starlight from the Astrologer back to its caster. */
    public static void interferenceTurned(ServerLevel sl, Player player, LivingEntity caster) {
        flash(sl, INTERFERENCE_VEIL, chestOf(player), yawToward(player.position(), caster.position()), 0xFFFFFFFF, 2.0f, 12);
        ParticleShapes.slashBetween(sl, STAR_BEAM, chestOf(player), chestOf(caster));
        sound(sl, chestOf(player), ModSounds.STARS_INTERFERENCE_REFLECT, 1.0f, wobble(1.0f));
    }

    /** A mob further than 8 blocks forgets the Astrologer: a puff of static over its head. */
    public static void mobForgets(ServerLevel sl, LivingEntity mob) {
        ParticleShapes.burst(sl, SPARKLE, headOf(mob).add(0, 0.3, 0), 3, 0.01, 0.03);
        ParticleShapes.spawn(sl, EYE_MARK.color(0xFF7A80A0), headOf(mob).add(0, 0.5, 0), Vec3.ZERO);
    }

    public static void interferenceEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.burst(sl, SPARKLE, chestOf(player), 6, 0.03, 0.08);
        statusEnd(player, Status.INTERFERENCE);
    }

    // =====================================================================================
    // Key 4: Record / Replay (Scribe)
    // =====================================================================================

    /** Record: the Scribe's book opens before them and the quill waits over it, for 'ticks'. */
    public static void record(ServerLevel sl, Player player, int ticks, String tag) {
        ModelFx book = RECORD_BOOK.scale(0.55f).lifetime(ticks).fade(6, 10).tag(tag)
                .key(0, pose().forward(0.8f).up(-0.4f).pitch(-60f).scale(0.3f).alpha(0f))
                .key(8, pose().pitch(-35f).scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOnTurning(sl, book, player, new Vec3(0, player.getEyeHeight() - 0.55, 0), 0f, 0f, 0f);
        ModelFx quill = QUILL.scale(0.45f).lifetime(ticks).fade(6, 10).tag(tag)
                .key(0, pose().forward(0.85f).right(0.25f).up(-0.2f).roll(25f).alpha(0f))
                .key(8, pose().alpha(1f), Ease.OUT)
                .key(14, pose().roll(15f), Ease.IN_OUT)
                .key(20, pose().roll(25f), Ease.IN_OUT);
        ParticleShapes.modelOnTurning(sl, quill, player, new Vec3(0, player.getEyeHeight() - 0.45, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, PAGE, chestOf(player).add(Spells.flatLook(player).scale(0.8)), 4, 0.02, 0.06);
        sound(sl, player.position(), ModSounds.STARS_RECORD, 1.0f, wobble(1.0f));
        status(player, Status.RECORD, ticks);
    }

    /**
     * A power was copied: a written page flies from 'from' (where it was cast) into the book. angel = an Angel-level
     * power, recorded weakly (a gold page).
     */
    public static void recorded(ServerLevel sl, Player player, Vec3 from, boolean angel) {
        Vec3 to = chestOf(player).add(Spells.flatLook(player).scale(0.8));
        int ticks = Math.max(6, Math.min(16, (int) (from.distanceTo(to) / 2)));
        ModelFx page = RECORD_PAGE.color(angel ? 0xFFFFE7A8 : 0xFFFFFFFF).scale(0.45f).lifetime(ticks + 4).fade(0, 4)
                .spin(18f)
                .key(0, pose().forward(0f))
                .key(ticks, pose().forward((float) from.distanceTo(to)), Ease.IN_OUT);
        ParticleShapes.model(sl, page, from, yawToward(from, to), 0f, 0f);
        ParticleShapes.slashBetween(sl, STAR_LINE.lifetime(ticks).sweep(ticks), from, to);
        Later.run(sl, ticks, () -> {
            stars(sl, to, angel ? 10 : 6, 0.03, 0.08);
            ParticleShapes.burst(sl, PAGE, to, 3, 0.02, 0.06);
            sound(sl, to, ModSounds.STARS_RECORDED, 1.0f, wobble(angel ? 0.9f : 1.0f));
        });
    }

    /** The roll failed: the page crumbles to motes before it is written. */
    public static void recordFailed(ServerLevel sl, Player player) {
        Vec3 at = chestOf(player).add(Spells.flatLook(player).scale(0.8));
        ParticleShapes.burst(sl, PAGE.color(0xFF8A90B0), at, 4, 0.02, 0.08);
        ParticleShapes.burst(sl, NIGHT_SMOKE, at, 4, 0.01, 0.04);
    }

    public static void recordEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        statusEnd(player, Status.RECORD);
    }

    /** Replay: a page is torn out of the book and goes up in starlight, toward the crosshair (play the power after). */
    public static void replay(ServerLevel sl, Player player) {
        Vec3 hand = handOf(player, sl);
        Vec3 look = player.getLookAngle();
        ModelFx page = RECORD_PAGE.scale(0.45f).lifetime(10).fade(0, 6).spin(25f)
                .key(0, pose().forward(0f).alpha(1f))
                .key(10, pose().forward(2.5f).scale(0.3f).alpha(0f), Ease.OUT);
        ParticleShapes.modelFacing(sl, page, hand, look);
        ParticleShapes.cone(sl, PAGE, hand, look, 25, 5, 0.1, 0.25);
        ParticleShapes.cone(sl, TWINKLE, hand, look, 20, 8, 0.1, 0.3);
        sound(sl, hand, ModSounds.STARS_REPLAY, 1.0f, wobble(1.0f));
    }

    // =====================================================================================
    // Key 5: Travel (Traveler, Wanderer)
    // =====================================================================================

    /** The tick the Traveler is inside the door (hide them from there until the arrival). */
    public static final int TRAVEL_ENTERED = 10;

    /** Traveler's Door, the way in: a Door to the Spirit World opens before the Traveler at 'from'. */
    public static void travelersDoor(ServerLevel sl, Player player, Vec3 from) {
        float yaw = player.getYRot();
        Vec3 at = from.add(ahead(yaw).scale(0.8));
        door(sl, at, yaw, 1.25f, 0xFFC8D4FF, 6, TRAVEL_ENTERED + 4, TRAVEL_ENTERED + 14, null);
        floor(sl, STAR_GATE, from, 1.6, 0xC09DB4FF, TRAVEL_ENTERED + 12, 3f, null, 0);
        ParticleShapes.burst(sl, DOOR_SPARK, at.add(0, 1.2, 0), 8, 0.03, 0.1);
        Later.run(sl, TRAVEL_ENTERED, () -> {
            ParticleShapes.burst(sl, STAR_MOTE, at.add(0, 1.2, 0), 14, 0.04, 0.12);
            stars(sl, at.add(0, 1.2, 0), 6, 0.05, 0.12);
        });
        sound(sl, from, ModSounds.STARS_TRAVELERS_DOOR, 1.2f, wobble(1.0f));
    }

    /** Traveler's Door, the way out: a Door opens at 'at' facing 'yaw' and the Traveler steps out of it. */
    public static void travelersArrive(ServerLevel sl, Vec3 at, float yaw) {
        door(sl, at.subtract(ahead(yaw).scale(0.6)), yaw, 1.25f, 0xFFC8D4FF, 4, 14, 24, null);
        floor(sl, STAR_GATE, at, 1.6, 0xC09DB4FF, 24, -3f, null, 0);
        SpellFx.shockRing(sl, at, 2.0, 0xC09DB4FF, 8, 4);
        ParticleShapes.burst(sl, STAR_MOTE, at.add(0, 1.0, 0), 12, 0.04, 0.12);
        sound(sl, at, ModSounds.STARS_TRAVELERS_ARRIVE, 1.0f, wobble(1.0f));
    }

    /** Blink: an afterimage at 'from', a streak, stars at 'to'. chain = 0, 1 or 2 for the hops in a row (higher each). */
    public static void blink(ServerLevel sl, Player player, Vec3 from, Vec3 to, int chain) {
        afterimage(sl, player, from, player.getYRot(), 0xB0C8D4FF, 12);
        ParticleShapes.slashBetween(sl, STAR_TRAIL, from.add(0, 1.0, 0), to.add(0, 1.0, 0));
        ParticleShapes.burst(sl, SPARKLE, from.add(0, 1.0, 0), 5, 0.02, 0.06);
        stars(sl, to.add(0, 1.0, 0), 4, 0.03, 0.08);
        int c = Math.max(0, Math.min(2, chain));
        sound(sl, to, ModSounds.STARS_BLINK, 0.9f, wobble(1.0f + c * 0.08f));
    }

    /** The Blink's dodge took a hit: a door flickers where the blow landed. */
    public static void blinkDodge(ServerLevel sl, Player player) {
        flash(sl, DOOR_SIGIL, chestOf(player), player.getYRot() + 180f, 0xFFC8D4FF, 1.4f, 10);
        ParticleShapes.burst(sl, CRACK, chestOf(player), 4, 0.04, 0.1);
        sound(sl, chestOf(player), ModSounds.STARS_LAYER_TEAR, 0.6f, wobble(1.3f));
    }

    /** Planeswalker: an afterimage that stays at 'at' for 'ticks' and echoes the powers cast (omnipresenceEcho-like). */
    public static void blinkEcho(ServerLevel sl, Player player, Vec3 at, int ticks, String tag) {
        float h = Math.max(0.5f, player.getBbHeight() / 2f);
        ModelFx ghost = AFTERIMAGE.color(0xA0C8D4FF).scale(h).lifetime(ticks).fade(4, 10).tag(tag);
        ParticleShapes.model(sl, ghost, at, player.getYRot(), 0f, 0f);
        floor(sl, STAR_GATE, at, 0.7, 0x809DB4FF, ticks, 2f, tag, 0);
    }

    /** The Secrets Sorcerer's quick powers after a Blink: a clock beside the hotbar. */
    public static void quickened(Player player, int ticks) {
        status(player, Status.QUICKENED, ticks);
    }

    /** The tick the Invisible Hand closes on what it reached for. */
    public static final int HAND_GRABS = 6;

    /** Invisible Hand on a creature: a faint hand of starlight reaches out, closes on it at HAND_GRABS and yanks. */
    public static void invisibleHand(ServerLevel sl, Player player, LivingEntity target) {
        reach(sl, player, chestOf(target));
        Later.run(sl, HAND_GRABS, () -> {
            if (!target.isAlive()) return;
            SpellFx.tether(sl, HAND_LINE, () -> SpellFx.handOf(player, sl), () -> SpellFx.middleOf(target, sl), 8);
            ParticleShapes.burst(sl, STAR_MOTE, chestOf(target), 8, 0.03, 0.08);
        });
        sound(sl, player.position(), ModSounds.STARS_INVISIBLE_HAND, 1.0f, wobble(1.0f));
    }

    /** Invisible Hand on items, XP or arrows at 'at': the same hand, smaller. */
    public static void invisibleHandItem(ServerLevel sl, Player player, Vec3 at) {
        reach(sl, player, at);
        sound(sl, player.position(), ModSounds.STARS_INVISIBLE_HAND, 0.7f, wobble(1.25f));
    }

    private static void reach(ServerLevel sl, Player player, Vec3 at) {
        Vec3 hand = handOf(player, sl);
        float distance = (float) hand.distanceTo(at);
        ModelFx grab = STAR_HAND.scale(0.7f).lifetime(HAND_GRABS + 10).fade(2, 8)
                .key(0, pose().forward(0f).alpha(0f))
                .key(HAND_GRABS, pose().forward(Math.max(0f, distance - 0.4f)).alpha(1f), Ease.OUT)
                .key(HAND_GRABS + 2, pose().pitch(60f), Ease.IN)
                .key(HAND_GRABS + 10, pose().forward(0f).alpha(0f), Ease.IN);
        ParticleShapes.model(sl, grab, hand, yawToward(hand, at), 90f, 0f);
        ParticleShapes.slashBetween(sl, HAND_LINE.lifetime(HAND_GRABS + 2).sweep(HAND_GRABS), hand, at);
    }

    /** Yanked to the Traveler's feet: it lands with a puff of starlight and staggers. */
    public static void yanked(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, STAR_MOTE, target.position().add(0, 0.3, 0), 10, 0.04, 0.1);
        SpellFx.shockRing(sl, target.position(), 1.2, 0xA09DB4FF, 6, 0);
    }

    /** Star Anchor: the spot is saved, a compass of starlight sinks into the ground and stays faintly for 'ticks'. */
    public static void starAnchor(ServerLevel sl, Player player, Vec3 at, int ticks, String tag) {
        floor(sl, ANCHOR_SEAL, at, 0.9, 0xE0C8D4FF, ticks, 0.6f, tag, 0);
        ModelFx mark = DOOR_FRAME.color(0x809DB4FF).scale(0.45f).lifetime(ticks).fade(10, 20).tag(tag)
                .key(0, pose().up(1.5f).alpha(0f))
                .key(12, pose().up(0f).alpha(0.6f), Ease.OUT);
        ParticleShapes.model(sl, mark, at, player.getYRot(), 0f, 0f);
        SpellFx.risingRings(sl, at, 0.9, 1.6, 3, 0xC09DB4FF);
        stars(sl, at.add(0, 0.6, 0), 8, 0.03, 0.08);
        sound(sl, at, ModSounds.STARS_STAR_ANCHOR, 1.0f, wobble(1.0f));
    }

    /** The Anchor at 'at' is picked up (or expires). */
    public static void anchorLift(ServerLevel sl, Vec3 at, String tag) {
        ParticleShapes.clearModels(sl, at, tag);
        SpellFx.closingRing(sl, at, 1.2, 0xA09DB4FF, 8, 0);
        ParticleShapes.burst(sl, STAR_MOTE, at.add(0, 0.4, 0), 8, 0.02, 0.08);
        sound(sl, at, ModSounds.STARS_ANCHOR_LIFT, 0.8f, wobble(1.0f));
    }

    /** Each second: a small star 3 blocks out toward one of the owner's Anchors, only they see it. */
    public static void anchorDirection(ServerPlayer owner, Vec3 anchor) {
        Vec3 eye = owner.getEyePosition();
        Vec3 way = anchor.add(0, 1.0, 0).subtract(eye);
        if (way.lengthSqr() < 9) return;
        ParticleShapes.spawnFor(owner, TWINKLE.size(0.08f), eye.add(way.normalize().scale(3.0)), Vec3.ZERO);
    }

    /** Wandering's form: Wandering (Wanderer), Symbolization (Planeswalker) or Conceptualization (Door). */
    public enum Form { WANDERING, SYMBOLIZATION, CONCEPTUALIZATION }

    /** Wandering: the Beyonder turns into a trail of starlight and enters the Astral World for 'ticks'. */
    public static void wandering(ServerLevel sl, Player player, int ticks, Form form, String tag) {
        int color = form == Form.CONCEPTUALIZATION ? 0xE0FFD98A : 0xE0C8D4FF;
        ModelFx vortex = STORM_VORTEX.color(color).scale(2.4f).lifetime(14).fade(0, 10).spin(20f)
                .key(0, pose().scale(0.2f))
                .key(6, pose().scale(1f), Ease.OUT)
                .key(14, pose().scale(0.1f), Ease.IN);
        ParticleShapes.model(sl, vortex, chestOf(player), player.getYRot(), 0f, 0f);
        afterimage(sl, player, player.position(), player.getYRot(), 0xC0C8D4FF, 14);
        ParticleShapes.burst(sl, TWINKLE, chestOf(player), 16, 0.08, 0.2);
        if (form != Form.WANDERING) ParticleShapes.burst(sl, WORM, chestOf(player), 8, 0.06, 0.14);
        sound(sl, player.position(), ModSounds.STARS_WANDERING, 1.2f, form == Form.CONCEPTUALIZATION ? 0.85f : 1.0f);
        status(player, Status.WANDERING, ticks);
    }

    /** Every 2 ticks while Wandering: the trail of starlight behind the Beyonder. */
    public static void wanderingTick(ServerLevel sl, Player player, Form form) {
        Vec3 chest = chestOf(player);
        Vec3 back = chest.subtract(player.getDeltaMovement().scale(3.0));
        ParticleShapes.slashBetween(sl, STAR_TRAIL.lifetime(6), back, chest);
        ParticleShapes.burst(sl, form == Form.CONCEPTUALIZATION ? GOLD_TWINKLE : TWINKLE, chest, 2, 0.01, 0.04);
        if (form != Form.WANDERING) ParticleShapes.spawn(sl, WORM, chest, Vec3.ZERO);
    }

    /** Conceptualization flew through an enemy: it is wrapped in starlight and gone for a while. */
    public static void conceptualized(ServerLevel sl, LivingEntity enemy) {
        ModelFx wrap = STORM_VORTEX.color(0xE0FFD98A).scale(Math.max(1.4f, enemy.getBbHeight() * 1.2f)).lifetime(12)
                .fade(0, 8).spin(30f)
                .key(0, pose().scale(1.4f))
                .key(12, pose().scale(0.1f), Ease.IN);
        ParticleShapes.model(sl, wrap, chestOf(enemy), randomYaw(sl), 0f, 0f);
        ParticleShapes.burst(sl, WORM, chestOf(enemy), 10, 0.04, 0.12);
    }

    public static void wanderingEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        SpellFx.shockRing(sl, player.position(), 2.0, 0xC09DB4FF, 8, 0);
        ParticleShapes.burst(sl, STAR_MOTE, chestOf(player), 14, 0.04, 0.12);
        sound(sl, player.position(), ModSounds.STARS_WANDERING_END, 1.0f, wobble(1.0f));
        statusEnd(player, Status.WANDERING);
    }

    // =====================================================================================
    // Key 6: Secrets and Space (Secrets Sorcerer, Wanderer)
    // =====================================================================================

    /** The tick the shell of split-off space has closed. */
    public static final int CONCEALED = 12;

    /**
     * Space Concealment: a sphere of 'radius' at 'center' is split off from the world for 'ticks'; a shell of space
     * rises round it and one lit Door stands in it at 'doorYaw' (the side facing the caster). cage = the Wanderer's
     * Spatial Cage (instant: the shell is there at once).
     */
    public static void spaceConcealment(ServerLevel sl, Player player, Vec3 center, double radius, int ticks, float doorYaw,
                                        boolean cage, String tag) {
        int risen = cage ? 2 : CONCEALED;
        floor(sl, STAR_GATE, center, Math.min(radius, 3.0), 0xA09DB4FF, ticks, 0.5f, tag, 0);
        wallRing(sl, SPACE_SHELL, center, radius, radius * 0.9, 0x909DB4FF, ticks, risen, tag);
        wallRing(sl, SPACE_SHELL, center.add(0, radius * 0.9, 0), radius * 0.7, radius * 0.55, 0x709DB4FF, ticks, risen + 4, tag);
        wallRing(sl, SPACE_SHELL, center.add(0, radius * 1.45, 0), radius * 0.35, radius * 0.4, 0x509DB4FF, ticks, risen + 8, tag);
        Vec3 doorAt = center.add(ahead(doorYaw + 180f).scale(radius * 0.98));
        door(sl, doorAt, doorYaw, 1.2f, 0xFFFFE7A8, risen, -1, ticks, tag);
        SpellFx.shockRing(sl, center, radius, 0xE0C8D4FF, risen, 0);
        ParticleShapes.burst(sl, STAR_MOTE, center.add(0, 1.0, 0), 20, 0.05, 0.16);
        sound(sl, center, ModSounds.STARS_SPACE_CONCEALMENT, 1.4f, cage ? 1.15f : 1.0f);
        status(player, Status.SPACE_CONCEALMENT, ticks);
    }

    /** Cast again: the Door in the shell slams shut and is barred for the rest of the time. */
    public static void concealmentShut(ServerLevel sl, Vec3 center, double radius, float doorYaw, String tag) {
        Vec3 doorAt = center.add(ahead(doorYaw + 180f).scale(radius * 0.98));
        ModelFx barred = CLOSED_DOOR.color(0xE0FFE7A8).scale(1.4f, 2.6f, 1f).lifetime(400).fade(2, 12).tag(tag)
                .key(0, pose().scale(1.6f).alpha(0f))
                .key(4, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, barred, doorAt, doorYaw, 0f, 0f);
        ParticleShapes.burst(sl, PADLOCK, doorAt.add(0, 1.2, 0), 4, 0.04, 0.1);
        sound(sl, doorAt, ModSounds.STARS_CONCEALMENT_SHUT, 1.2f, wobble(1.0f));
    }

    /** An ally hidden inside heals a little: soft motes rise off them. */
    public static void concealedHeal(ServerLevel sl, LivingEntity ally) {
        ParticleShapes.burst(sl, STAR_MOTE, ally.position().add(0, 0.4, 0), 3, 0.01, 0.03);
    }

    public static void concealmentEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        SpellFx.closingRing(sl, center, radius, 0xA09DB4FF, 12, 0);
        ParticleShapes.burst(sl, TWINKLE, center.add(0, radius * 0.6, 0), 20, 0.05, 0.2);
    }

    /**
     * Transfiguration: the Beyonder becomes layers upon layers of illusory Doors, 'layers' of them, for 'ticks'. Each
     * layer is a door standing round them; layerTorn takes one away.
     */
    public static void transfiguration(ServerLevel sl, Player player, int layers, int ticks, String tag) {
        for (int i = 0; i < layers; i++) {
            ModelFx layer = DOOR_SIGIL.color(0xA0C8D4FF).scale(1.6f + i * 0.12f).lifetime(ticks).fade(4, 8).tag(tag)
                    .key(0, pose().scale(0.2f).alpha(0f))
                    .key(6 + i * 2, pose().scale(1f).alpha(0.9f), Ease.OUT_BACK);
            float yaw = i * 360f / layers;
            ParticleShapes.modelOn(sl, layer, player, ahead(yaw).scale(0.55 + i * 0.04).add(0, player.getBbHeight() * 0.5, 0),
                    yaw + 180f, 0f, 0f);
        }
        ParticleShapes.burst(sl, DOOR_SPARK, chestOf(player), 12, 0.04, 0.12);
        sound(sl, player.position(), ModSounds.STARS_TRANSFIGURATION, 1.0f, wobble(1.0f));
        layersLeft(player, layers, ticks);
    }

    /**
     * A blow tore one layer: the door it hit shatters at 'from' and the Beyonder is 3 blocks aside at 'to'. Call
     * transfiguration again with the layers left (and the same tag after clearing it) if the doors should be redrawn.
     */
    public static void layerTorn(ServerLevel sl, Player player, Vec3 from, Vec3 to, int layersLeft) {
        flash(sl, DOOR_SIGIL, from.add(0, 1.0, 0), yawToward(from, to), 0xFFFFFFFF, 1.8f, 12);
        ParticleShapes.burst(sl, CRACK, from.add(0, 1.0, 0), 6, 0.06, 0.14);
        ParticleShapes.burst(sl, DOOR_SPARK, from.add(0, 1.0, 0), 6, 0.06, 0.16);
        ParticleShapes.slashBetween(sl, STAR_TRAIL, from.add(0, 1.0, 0), to.add(0, 1.0, 0));
        afterimage(sl, player, from, player.getYRot(), 0xA0C8D4FF, 10);
        sound(sl, from, ModSounds.STARS_LAYER_TEAR, 1.0f, wobble(1.0f));
        layersLeft(player, layersLeft, 200);
    }

    public static void transfigurationEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.burst(sl, DOOR_SPARK, chestOf(player), 10, 0.03, 0.1);
        statusEnd(player, Status.TRANSFIGURATION);
    }

    /** The tick the Doors have swallowed an Exile: hide it from there. */
    public static final int EXILE_SWALLOWED = 12;

    /** Exile: Doors open round 'target' and swallow it at EXILE_SWALLOWED. */
    public static void exile(ServerLevel sl, Player player, LivingEntity target) {
        Vec3 at = target.position();
        float yaw = yawToward(at, player.position());
        float size = Math.max(1.1f, target.getBbHeight() * 0.6f);
        for (int i = 0; i < 3; i++) {
            float y = yaw + i * 120f;
            door(sl, at.add(ahead(y).scale(Math.max(0.8, target.getBbWidth()))), y + 180f, size, 0xFFC8B4FF, 4, EXILE_SWALLOWED,
                    EXILE_SWALLOWED + 10, null);
        }
        ModelFx vortex = STORM_VORTEX.color(0xC07A5AD8).scale(size * 2f).lifetime(EXILE_SWALLOWED + 6).fade(2, 6).spin(24f)
                .key(0, pose().scale(0.2f))
                .key(EXILE_SWALLOWED, pose().scale(1f), Ease.OUT);
        ParticleShapes.model(sl, vortex, chestOf(target), yaw, 0f, 0f);
        sound(sl, at, ModSounds.STARS_EXILE, 1.2f, wobble(1.0f));
        Later.run(sl, EXILE_SWALLOWED, () -> {
            ParticleShapes.burst(sl, STORM_SMOKE, at.add(0, 1.0, 0), 10, 0.03, 0.1);
            ParticleShapes.burst(sl, STORM_MOTE, at.add(0, 1.0, 0), 12, 0.05, 0.15);
        });
    }

    /** While it is gone: a closed door of starlight stands on its spot, a storm turning inside it, for 'ticks'. */
    public static void exiled(ServerLevel sl, LivingEntity target, Vec3 at, int ticks, String tag) {
        ModelFx shut = DOOR_SIGIL.color(0x90C8B4FF).scale(Math.max(1.4f, target.getBbHeight() * 0.9f)).lifetime(ticks)
                .fade(4, 6).tag(tag);
        ParticleShapes.model(sl, shut, at.add(0, Math.max(0.7, target.getBbHeight() * 0.45), 0), randomYaw(sl), 0f, 0f);
        ModelFx storm = STORM_VORTEX.color(0x907A5AD8).scale(Math.max(1.0f, target.getBbHeight() * 0.6f)).lifetime(ticks)
                .fade(4, 6).spin(12f).tag(tag);
        ParticleShapes.model(sl, storm, at.add(0, Math.max(0.7, target.getBbHeight() * 0.45), 0), randomYaw(sl), 0f, 0f);
        if (target instanceof Player p) status(p, Status.EXILED, ticks);
    }

    /** It comes back out of the same door, hurt: the door bursts open. */
    public static void exileReturn(ServerLevel sl, Vec3 at, String tag) {
        ParticleShapes.clearModels(sl, at, tag);
        door(sl, at, randomYaw(sl), 1.2f, 0xFFC8B4FF, 2, -1, 14, null);
        ParticleShapes.burst(sl, STORM_SMOKE, at.add(0, 1.0, 0), 12, 0.05, 0.14);
        ParticleShapes.burst(sl, CRACK, at.add(0, 1.0, 0), 6, 0.06, 0.14);
        SpellFx.shockRing(sl, at, 2.4, 0xC07A5AD8, 8, 0);
        sound(sl, at, ModSounds.STARS_EXILE_RETURN, 1.2f, wobble(1.0f));
    }

    /** Pocket Space: a small Door opens before the Beyonder onto their own Concealed space (open the menu after). */
    public static void pocketSpace(ServerLevel sl, Player player) {
        float yaw = player.getYRot();
        Vec3 at = player.position().add(ahead(yaw).scale(1.2));
        door(sl, at, yaw, 0.8f, 0xFFFFE7A8, 4, 16, 22, null);
        ParticleShapes.burst(sl, GOLD_MOTE, at.add(0, 0.8, 0), 6, 0.02, 0.06);
        sound(sl, at, ModSounds.STARS_POCKET_SPACE, 0.9f, wobble(1.0f));
    }

    /** The tick space tears round the target: the damage goes there. */
    public static final int TEAR_LANDS = 10;

    /**
     * Space Tearing: a ring warns round 'target' for half a second, then space tears in jagged rifts 'radius' blocks
     * round it. (Planeswalker: call it with the Beyonder as the target too, for the tear round them.)
     */
    public static void spaceTearing(ServerLevel sl, Player player, LivingEntity target, double radius) {
        Vec3 at = target.position();
        SpellFx.warningCircle(sl, at, radius, 0xC0C8B4FF, TEAR_LANDS);
        SpellFx.closingRing(sl, at, radius + 1.5, 0xA07A5AD8, TEAR_LANDS, 0);
        sound(sl, at, ModSounds.STARS_TEARING_WARN, 1.0f, wobble(1.0f));
        Later.run(sl, TEAR_LANDS, () -> {
            Vec3 c = target.isAlive() ? chestOf(target) : at.add(0, 1.0, 0);
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < 4; i++) {
                ModelFx rift = SPACE_TEAR.scale((float) (radius * 0.9 + r.nextDouble() * radius * 0.4)).lifetime(16).fade(0, 10)
                        .key(0, pose().scale(0.1f).roll(i * 45f))
                        .key(3, pose().scale(1f).roll(i * 45f), Ease.OUT)
                        .key(16, pose().scale(1.15f).roll(i * 45f), Ease.LINEAR);
                ParticleShapes.model(sl, rift, c, randomYaw(sl), 0f, 0f);
            }
            for (int i = 0; i < 3; i++) ParticleShapes.slash(sl, TEAR_ARC.radius((float) (radius * 0.8)), c, randomYaw(sl), 0f, i * 60f);
            SpellFx.shockRing(sl, at, radius * 1.4, 0xE0E8EEFF, 8, 0);
            ParticleShapes.burst(sl, CRACK, c, 16, 0.1, 0.3);
            ParticleShapes.burst(sl, STORM_SMOKE, c, 10, 0.05, 0.15);
            sound(sl, c, ModSounds.STARS_SPACE_TEARING, 1.6f, wobble(1.0f));
        });
    }

    /** It burns in a Space-Time storm for 'ticks': a storm turning round it. */
    public static void storm(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        ModelFx vortex = STORM_VORTEX.color(0xA07A5AD8).scale(Math.max(1.6f, target.getBbHeight() * 1.2f)).lifetime(ticks)
                .fade(4, 8).spin(18f).tag(tag);
        ParticleShapes.modelOn(sl, vortex, target, new Vec3(0, target.getBbHeight() * 0.5, 0), randomYaw(sl), 90f, 0f);
        if (target instanceof Player p) status(p, Status.STORM, ticks);
    }

    /** Each second of the storm: shards of space and violet smoke. */
    public static void stormTick(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, CRACK, chestOf(target), 3, 0.05, 0.12);
        ParticleShapes.burst(sl, STORM_MOTE, chestOf(target), 5, 0.04, 0.1);
    }

    // =====================================================================================
    // Key 7: Starlight (Planeswalker, Key of Stars, Door)
    // =====================================================================================

    /** The tick a creature in the Toy Box has shrunk into a toy (the scale change goes there). */
    public static final int TOY_SHRUNK = 10;

    /** Toy Box: a circular veil, a music-box carousel, opens on the ground at 'center' for 'ticks'. */
    public static void toyBox(ServerLevel sl, Player player, Vec3 center, double radius, int ticks, String tag) {
        floor(sl, TOY_VEIL, center, radius, 0xC0FFE7A8, ticks, 0.8f, tag, 0);
        SpellFx.shockRing(sl, center, radius, 0xE0FFE7A8, 10, 0);
        ParticleShapes.ring(sl, WIND_KEY, center.add(0, 0.4, 0), radius * 0.8, 10, 0.02);
        ParticleShapes.burst(sl, GOLD_TWINKLE, center.add(0, 1.0, 0), 14, 0.05, 0.15);
        sound(sl, center, ModSounds.STARS_TOY_BOX, 1.4f, wobble(1.0f));
    }

    /** It shrinks into a wind-up toy: a key turns in its back, for 'ticks'. */
    public static void toyed(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        ModelFx key = WIND_UP_KEY.scale(0.8f).lifetime(ticks).fade(4, 6).tag(tag)
                .key(0, pose().scale(0.1f))
                .key(TOY_SHRUNK, pose().scale(1f), Ease.OUT_BACK);
        for (int t = TOY_SHRUNK; t < ticks; t += 20) {
            key = key.key(t + 10, pose().roll(90f), Ease.OUT).key(t + 11, pose().roll(0f));
        }
        Vec3 back = ahead(target.getYRot() + 180f).scale(target.getBbWidth() * 0.55);
        ParticleShapes.modelOnTurning(sl, key, target, back.add(0, target.getBbHeight() * 0.55, 0), 180f, 0f, 0f);
        ParticleShapes.burst(sl, GOLD_TWINKLE, chestOf(target), 8, 0.03, 0.08);
        Later.run(sl, TOY_SHRUNK, () -> {
            if (target.isAlive()) sound(sl, chestOf(target), ModSounds.STARS_TOY_WIND, 0.8f, wobble(1.0f));
        });
        if (target instanceof Player p) status(p, Status.TOY, ticks);
    }

    /** Now and then while it is a toy: the clockwork ticks. */
    public static void toyTick(ServerLevel sl, LivingEntity target) {
        ParticleShapes.spawn(sl, WIND_KEY, headOf(target).add(0, 0.2, 0), new Vec3(0, 0.03, 0));
        sound(sl, chestOf(target), ModSounds.STARS_TOY_WIND, 0.4f, wobble(1.1f));
    }

    /** Players and big creatures are only slowed: the veil's light clings to their feet. */
    public static void toySlowed(ServerLevel sl, LivingEntity target, int ticks) {
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), 0xA0FFE7A8, ticks, "ks_toy_slow");
    }

    public static void toyReleased(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, WIND_KEY, chestOf(target), 3, 0.05, 0.12);
        ParticleShapes.burst(sl, GOLD_MOTE, chestOf(target), 8, 0.03, 0.08);
        if (target instanceof Player p) statusEnd(p, Status.TOY);
    }

    public static void toyBoxEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
    }

    /** Replication: a power used by 'source' is copied in the moment: a line from it to the Planeswalker, two pages. */
    public static void replication(ServerLevel sl, Player player, LivingEntity source) {
        Vec3 from = chestOf(source);
        Vec3 to = chestOf(player);
        ParticleShapes.slashBetween(sl, STAR_LINE.lifetime(12).sweep(6), from, to);
        ParticleShapes.burst(sl, PAGE, from, 3, 0.03, 0.08);
        ParticleShapes.burst(sl, PAGE, to, 3, 0.03, 0.08);
        flash(sl, RECORD_PAGE, to.add(Spells.flatLook(player).scale(0.7)), player.getYRot() + 180f, 0xFFFFFFFF, 0.6f, 14);
        stars(sl, to, 8, 0.03, 0.1);
        sound(sl, to, ModSounds.STARS_REPLICATION, 1.0f, wobble(1.0f));
    }

    /**
     * Space Fragmentation: nine colored planets of starlight circle 'center' at up to 'radius' for 'ticks', ringed by a
     * slow pull of starlight.
     */
    public static void spaceFragmentation(ServerLevel sl, Player player, Vec3 center, double radius, int ticks, String tag) {
        floor(sl, CONSTELLATION, center, radius, 0x809DB4FF, ticks, 0.6f, tag, 0);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 9; i++) {
            double orbit = radius * (0.35 + 0.65 * (i + 1) / 9.0);
            float size = 0.7f + r.nextFloat() * 0.8f;
            float speed = (float) (60.0 / orbit) * (i % 2 == 0 ? 1f : -1f);
            int color = PLANETS[i];
            Vec3 anchor = center.add(0, 1.2 + r.nextDouble() * 2.4, 0);
            ModelFx planet = ORB.color(color).scale(size).pivot(8, 8, 8 + 16f * (float) orbit / size)
                    .lifetime(ticks).fade(10, 6).spin(speed).tag(tag)
                    .key(0, pose().spin(i * 40f).alpha(0f))
                    .key(10, pose().spin(i * 40f).alpha(1f), Ease.OUT);
            ParticleShapes.model(sl, planet, anchor, 0f, 0f, 0f);
            if (i % 3 == 0) {
                ModelFx ring = PLANET_RING.color(color).scale(size * 2.2f).pivot(8, 0, 8 + 16f * (float) orbit / (size * 2.2f))
                        .lifetime(ticks).fade(10, 6).spin(speed).tag(tag)
                        .key(0, pose().spin(i * 40f).alpha(0f))
                        .key(10, pose().spin(i * 40f).alpha(0.9f), Ease.OUT);
                ParticleShapes.model(sl, ring, anchor, 0f, 0f, 0f);
            }
        }
        SpellFx.closingRing(sl, center, radius, 0xC09DB4FF, 14, 0);
        sound(sl, center, ModSounds.STARS_FRAGMENTATION, 1.8f, wobble(1.0f));
    }

    /** Every few ticks: starlight spirals in toward the middle (the pull). */
    public static void fragmentationTick(ServerLevel sl, Vec3 center, double radius) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 8; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            Vec3 at = center.add(Math.cos(a) * radius, 0.3 + r.nextDouble() * 3, Math.sin(a) * radius);
            Vec3 in = center.add(0, 1.5, 0).subtract(at).scale(0.06);
            ParticleShapes.spawn(sl, i % 2 == 0 ? STAR_MOTE : PLANET.color(PLANETS[r.nextInt(PLANETS.length)]), at, in);
        }
    }

    /** At the end the planets collapse in one shatter. */
    public static void fragmentationCollapse(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        Vec3 c = center.add(0, 1.5, 0);
        SpellFx.blast(sl, c, Math.min(radius * 0.5, 8.0), 0xFFE8EEFF, TWINKLE);
        SpellFx.shockRing(sl, center, radius, 0xF0E8EEFF, 12, 0);
        SpellFx.shockRing(sl, center, radius * 0.6, 0xC0FFD98A, 10, 3);
        for (int color : PLANETS) ParticleShapes.burst(sl, PLANET.color(color), c, 3, 0.2, 0.5);
        ParticleShapes.burst(sl, CRACK, c, 20, 0.2, 0.5);
        sound(sl, c, ModSounds.STARS_FRAGMENTATION_COLLAPSE, 2.0f, wobble(1.0f));
    }

    /** The tick a Starlight Seal is closed on its target. */
    public static final int SEALED_AT = 14;

    /**
     * Starlight Seal: starlight falls on 'target' and every drop becomes a door that sinks into it; Sealed at SEALED_AT,
     * a ring of keyholes under it for 'ticks'.
     */
    public static void starlightSeal(ServerLevel sl, Player player, LivingEntity target, int ticks, String tag) {
        Vec3 top = headOf(target).add(0, 3.0, 0);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            Vec3 drop = top.add(r.nextGaussian() * 0.6, r.nextDouble() * 1.5, r.nextGaussian() * 0.6);
            int delay = i * 2;
            Later.run(sl, delay, () -> ParticleShapes.spawn(sl, TWINKLE.lifetime(SEALED_AT - delay), drop, new Vec3(0, -0.3, 0)));
            Later.run(sl, SEALED_AT - 4 + i % 3, () -> {
                if (target.isAlive()) flash(sl, DOOR_SIGIL, chestOf(target), randomYaw(sl), 0xFFFFE7A8, 0.6f, 8);
            });
        }
        ModelFx key = STAR_KEY.scale(0.8f).lifetime(SEALED_AT + 16).fade(4, 10)
                .key(0, pose().up(2.2f).alpha(0f))
                .key(SEALED_AT - 4, pose().up(0f).alpha(1f), Ease.IN)
                .key(SEALED_AT + 2, pose().roll(90f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, key, target, new Vec3(0, target.getBbHeight() + 0.8, 0), yawToward(target.position(),
                player.position()), 0f, 0f);
        Later.run(sl, SEALED_AT, () -> {
            if (!target.isAlive()) return;
            ModelFx seal = SEAL_RING.color(0xE0FFE7A8).scale(Math.max(1.6f, target.getBbWidth() * 2.4f)).lifetime(ticks)
                    .fade(4, 10).spin(1.5f).tag(tag)
                    .key(0, pose().scale(0.3f).alpha(0f))
                    .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
            ParticleShapes.modelOn(sl, seal, target, new Vec3(0, 0.07, 0), randomYaw(sl), 0f, 0f);
            ModelFx band = SEAL_RING.color(0x90C8D4FF).scale(Math.max(1.2f, target.getBbWidth() * 1.6f)).lifetime(ticks)
                    .fade(4, 10).spin(-2f).tag(tag);
            ParticleShapes.modelOn(sl, band, target, new Vec3(0, target.getBbHeight() * 0.55, 0), randomYaw(sl), 0f, 0f);
            for (int i = 0; i < 3; i++) {
                ModelFx worm = WORM_OF_STAR.scale(0.35f).pivot(8, 6, 8 + 16f * 0.9f / 0.35f).lifetime(Math.min(ticks, 60))
                        .fade(2, 10).spin(i % 2 == 0 ? 9f : -9f).tag(tag)
                        .key(0, pose().spin(i * 120f));
                ParticleShapes.modelOn(sl, worm, target, new Vec3(0, target.getBbHeight() * (0.3 + i * 0.25), 0), 0f, 0f, 0f);
            }
            ParticleShapes.ring(sl, PADLOCK, chestOf(target), Math.max(0.6, target.getBbWidth()), 4, 0.04);
            ParticleShapes.ring(sl, GOLD_MOTE, chestOf(target), 1.0, 12, -0.06);
            sound(sl, chestOf(target), ModSounds.STARS_STARLIGHT_SEAL, 1.2f, wobble(1.0f));
        });
        if (target instanceof Player p) status(p, Status.SEALED, ticks + SEALED_AT);
    }

    /** Something tried to cleanse the Seal and it repaired itself: keys flash round it. */
    public static void sealRepaired(ServerLevel sl, LivingEntity target) {
        ParticleShapes.ring(sl, KEY, chestOf(target), Math.max(0.7, target.getBbWidth()), 6, 0.0);
        ParticleShapes.burst(sl, GOLD_TWINKLE, chestOf(target), 6, 0.03, 0.08);
        sound(sl, chestOf(target), ModSounds.STARS_KEY_TURN, 0.8f, wobble(1.0f));
    }

    public static void sealEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, KEY, chestOf(target), 3, 0.05, 0.12);
        ParticleShapes.burst(sl, STAR_MOTE, chestOf(target), 8, 0.03, 0.08);
        if (target instanceof Player p) statusEnd(p, Status.SEALED);
    }

    /** Starlight Seal on an ally: their connections are sealed for 'ticks', a gold seal over their head. */
    public static void sealAlly(ServerLevel sl, Player player, LivingEntity ally, int ticks, String tag) {
        ModelFx halo = SEAL_RING.color(0xC0FFE7A8).scale(1.1f).lifetime(ticks).fade(6, 10).spin(2f).tag(tag);
        ParticleShapes.modelOn(sl, halo, ally, new Vec3(0, ally.getBbHeight() + 0.3, 0), 0f, 0f, 0f);
        ParticleShapes.slashBetween(sl, SEAL_THREAD.lifetime(10).sweep(4), handOf(player, sl), chestOf(ally));
        ParticleShapes.burst(sl, GOLD_TWINKLE, chestOf(ally), 8, 0.03, 0.08);
        sound(sl, chestOf(ally), ModSounds.STARS_KEY_TURN, 0.9f, wobble(1.0f));
        if (ally instanceof Player p) status(p, Status.SEALED_ALLY, ticks);
    }

    /** The tick the Space-Time Labyrinth's walls have risen. */
    public static final int LABYRINTH_RISEN = 14;

    /** Space-Time Labyrinth: a maze of starlight fills 'radius' blocks round 'center' for 'ticks', walled at the edge. */
    public static void labyrinth(ServerLevel sl, Player player, Vec3 center, double radius, int ticks, String tag) {
        floor(sl, LABYRINTH_FLOOR, center, radius, 0xC09DB4FF, ticks, 0.3f, tag, 0);
        wallRing(sl, SPACE_SHELL, center, radius, 3.0, 0x909DB4FF, ticks, LABYRINTH_RISEN, tag);
        wallRing(sl, SPACE_SHELL, center, radius * 0.6, 2.0, 0x609DB4FF, ticks, LABYRINTH_RISEN + 4, tag);
        SpellFx.shockRing(sl, center, radius, 0xE0C8D4FF, LABYRINTH_RISEN, 0);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 16; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = r.nextDouble() * radius;
            ParticleShapes.spawn(sl, MAZE, center.add(Math.cos(a) * d, 0.5 + r.nextDouble() * 2, Math.sin(a) * d), new Vec3(0, 0.02, 0));
        }
        sound(sl, center, ModSounds.STARS_LABYRINTH, 1.6f, wobble(1.0f));
        status(player, Status.LABYRINTH, ticks);
    }

    /** An enemy inside is Lost (a player sees the clock; everyone sees maze squares round its head). */
    public static void lost(ServerLevel sl, LivingEntity target, int ticks) {
        ParticleShapes.ring(sl, MAZE.size(0.08f), headOf(target).add(0, 0.2, 0), 0.5, 4, 0.0);
        if (target instanceof Player p) status(p, Status.LOST, ticks);
    }

    /** Someone walked out of the edge and is turned back in at the opposite side: a door at each end. */
    public static void labyrinthLoop(ServerLevel sl, Vec3 from, Vec3 to) {
        flash(sl, DOOR_SIGIL, from.add(0, 1.0, 0), yawToward(from, to), 0xFFC8D4FF, 1.6f, 12);
        flash(sl, DOOR_SIGIL, to.add(0, 1.0, 0), yawToward(to, from), 0xFFC8D4FF, 1.6f, 12);
        ParticleShapes.burst(sl, MAZE, to.add(0, 1.0, 0), 4, 0.03, 0.08);
        sound(sl, to, ModSounds.STARS_LABYRINTH_LOOP, 0.9f, wobble(1.0f));
    }

    public static void labyrinthEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        SpellFx.closingRing(sl, center, radius, 0xA09DB4FF, 12, 0);
        ParticleShapes.burst(sl, MAZE, center.add(0, 1.0, 0), 10, 0.05, 0.15);
    }

    /** The tick the Alternate World has unfolded (its rules are in force from there). */
    public static final int WORLD_UNFOLDED = 24;

    /**
     * Alternate World: a kingdom of Doors covers 'radius' blocks round 'center' for 'ticks': its ground, doors standing
     * everywhere, a wall of space at the edge and a dome of stars overhead.
     */
    public static void alternateWorld(ServerLevel sl, Player player, Vec3 center, double radius, int ticks, String tag) {
        floor(sl, DOOR_KINGDOM, center, radius, 0xB09DB4FF, ticks, 0.05f, tag, 0);
        floor(sl, STAR_GATE, center, 3.5, 0xE0FFE7A8, ticks, -0.4f, tag, 8);
        wallRing(sl, SPACE_SHELL, center, radius, 8.0, 0x809DB4FF, ticks, WORLD_UNFOLDED, tag);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int doors = (int) Math.min(36, radius * 0.9);
        for (int i = 0; i < doors; i++) {
            double a = Math.PI * 2 * i / doors + r.nextDouble() * 0.2;
            double d = radius * (0.25 + r.nextDouble() * 0.65);
            Vec3 at = center.add(Math.cos(a) * d, 0, Math.sin(a) * d);
            float size = 1.0f + r.nextFloat() * 1.4f;
            ModelFx frame = DOOR_FRAME.color(0xC0C8D4FF).scale(size).lifetime(ticks).fade(6, 20).tag(tag)
                    .key(0, pose().up(-2f * size).alpha(0f))
                    .key(WORLD_UNFOLDED - 6 + r.nextInt(12), pose().up(0f).alpha(1f), Ease.OUT);
            ModelFx leaf = DOOR_LEAF.color(0xD0FFFFFF).scale(size).lifetime(ticks).fade(6, 20).tag(tag)
                    .key(0, pose().up(-2f * size).alpha(0f))
                    .key(WORLD_UNFOLDED, pose().up(0f).alpha(1f).yaw(0f), Ease.OUT)
                    .key(WORLD_UNFOLDED + 10, pose().yaw(-60f - r.nextFloat() * 50f), Ease.OUT);
            float yaw = yawToward(at, center) + (float) (r.nextGaussian() * 30);
            ParticleShapes.model(sl, frame, at, yaw, 0f, 0f);
            ParticleShapes.model(sl, leaf, at, yaw, 0f, 0f);
        }
        SpellFx.shockRing(sl, center, radius, 0xF0E8EEFF, WORLD_UNFOLDED, 0);
        SpellFx.risingRings(sl, center, 4.0, 8.0, 4, 0xC09DB4FF);
        sound(sl, center, ModSounds.STARS_ALTERNATE_WORLD, 3.0f, 1.0f);
        status(player, Status.ALTERNATE_WORLD, ticks);
    }

    /** Every second while it lasts: stars come out in the dome overhead and drift down like a dream. */
    public static void alternateWorldTick(ServerLevel sl, Vec3 center, double radius) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int count = (int) Math.min(40, radius);
        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double tilt = r.nextDouble() * Math.PI * 0.45;
            double rr = radius * 0.9;
            Vec3 at = center.add(Math.cos(a) * Math.cos(tilt) * rr, 2 + Math.sin(tilt) * rr * 0.5, Math.sin(a) * Math.cos(tilt) * rr);
            ParticleShapes.spawn(sl, i % 4 == 0 ? GOLD_TWINKLE.lifetime(40, 10) : TWINKLE.lifetime(40, 10), at, new Vec3(0, -0.02, 0));
        }
    }

    public static void alternateWorldEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        SpellFx.closingRing(sl, center, radius, 0xA09DB4FF, 20, 0);
        ParticleShapes.burst(sl, TWINKLE, center.add(0, 2.0, 0), 30, 0.1, 0.4);
        sound(sl, center, ModSounds.STARS_CLOSE_THE_DOOR, 1.2f, 1.3f);
    }

    /** Omnipresence: six afterimages stand round the Beyonder in a ring of 'radius' 6 for 'ticks', following them. */
    public static void omnipresence(ServerLevel sl, Player player, int ticks, String tag) {
        float h = Math.max(0.5f, player.getBbHeight() / 2f);
        for (int i = 0; i < 6; i++) {
            float yaw = player.getYRot() + i * 60f;
            ModelFx self = AFTERIMAGE.color(0xB0C8D4FF).scale(h).lifetime(ticks).fade(6, 10).tag(tag + "_" + i)
                    .key(0, pose().forward(-6f).alpha(0f))
                    .key(8, pose().forward(0f).alpha(1f), Ease.OUT);
            ParticleShapes.modelOn(sl, self, player, ahead(yaw).scale(6.0), player.getYRot(), 0f, 0f);
            ModelFx gate = STAR_GATE.color(0x809DB4FF).scale(1.4f).lifetime(ticks).fade(6, 10).spin(2f).tag(tag + "_" + i);
            ParticleShapes.modelOn(sl, gate, player, ahead(yaw).scale(6.0).add(0, 0.05, 0), 0f, 0f, 0f);
        }
        floor(sl, STAR_GATE, player.position(), 2.0, 0xC0FFE7A8, 20, 4f, null, 0);
        stars(sl, chestOf(player), 16, 0.1, 0.3);
        sound(sl, player.position(), ModSounds.STARS_OMNIPRESENCE, 1.4f, 1.0f);
        status(player, Status.OMNIPRESENCE, ticks);
    }

    /** Where afterimage 'index' (0 to 5) stands right now. */
    public static Vec3 afterimageAt(Player player, int index) {
        return player.position().add(ahead(player.getYRot() + index * 60f).scale(6.0));
    }

    /** Afterimage 'index' repeats the power just cast: a flash of starlight from it. */
    public static void omnipresenceEcho(ServerLevel sl, Player player, int index) {
        Vec3 at = afterimageAt(player, index).add(0, 1.2, 0);
        ParticleShapes.burst(sl, TWINKLE, at, 4, 0.03, 0.08);
        ParticleShapes.slashBetween(sl, STAR_LINE.lifetime(6).sweep(2), chestOf(player), at);
    }

    /** Afterimage 'index' took the blow meant for the Beyonder and fades. */
    public static void afterimageStruck(ServerLevel sl, Player player, String tag, int index) {
        Vec3 at = afterimageAt(player, index);
        ParticleShapes.clearModels(sl, player, tag + "_" + index);
        flash(sl, DOOR_SIGIL, at.add(0, 1.0, 0), player.getYRot(), 0xFFC8D4FF, 1.6f, 10);
        ParticleShapes.burst(sl, CRACK, at.add(0, 1.0, 0), 6, 0.05, 0.12);
        sound(sl, at, ModSounds.STARS_LAYER_TEAR, 0.8f, wobble(1.1f));
    }

    public static void omnipresenceEnd(ServerLevel sl, Player player, String tag) {
        for (int i = 0; i < 6; i++) {
            ParticleShapes.clearModels(sl, player, tag + "_" + i);
            ParticleShapes.burst(sl, STAR_MOTE, afterimageAt(player, i).add(0, 1.0, 0), 4, 0.02, 0.06);
        }
        statusEnd(player, Status.OMNIPRESENCE);
    }

    /** The tick every Door has closed. */
    public static final int DOORS_SHUT = 16;

    /**
     * Close the Door: a great barred door rises behind the Beyonder and slams; a wave of closing doors races out
     * 'radius' blocks. 'ticks' is how long travel stays closed.
     */
    public static void closeTheDoor(ServerLevel sl, Player player, Vec3 center, double radius, int ticks, String tag) {
        float yaw = player.getYRot();
        ModelFx great = CLOSED_DOOR.color(0xE0FFE7A8).scale(4.5f).lifetime(DOORS_SHUT + 30).fade(4, 16)
                .key(0, pose().up(-4.5f).alpha(0f))
                .key(DOORS_SHUT - 4, pose().up(0f).alpha(1f), Ease.OUT)
                .key(DOORS_SHUT, pose().scale(1.08f), Ease.IN)
                .key(DOORS_SHUT + 3, pose().scale(1f), Ease.OUT);
        ParticleShapes.model(sl, great, player.position().subtract(ahead(yaw).scale(3.0)), yaw, 0f, 0f);
        floor(sl, SEAL_RING, center, 4.0, 0xC0FFE7A8, ticks, 0.8f, tag, DOORS_SHUT);
        Later.run(sl, DOORS_SHUT, () -> {
            SpellFx.shockRing(sl, center, radius, 0xF0FFE7A8, 16, 0);
            SpellFx.shockRing(sl, center, radius * 0.6, 0xC09DB4FF, 12, 3);
            ParticleShapes.ring(sl, PADLOCK, center.add(0, 1.0, 0), 3.0, 12, 0.3);
            ParticleShapes.ring(sl, KEY, center.add(0, 1.4, 0), 2.0, 8, 0.25);
        });
        sound(sl, center, ModSounds.STARS_CLOSE_THE_DOOR, 2.4f, 1.0f);
    }

    /** An enemy's travel is closed for 'ticks': a small barred door hangs over it. */
    public static void doorsClosed(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        ModelFx barred = CLOSED_DOOR.color(0xC0FFE7A8).scale(0.6f).lifetime(ticks).fade(4, 10).tag(tag)
                .key(0, pose().scale(1.6f).alpha(0f))
                .key(5, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, barred, target, new Vec3(0, target.getBbHeight() + 0.35, 0), randomYaw(sl), 0f, 0f);
        if (target instanceof Player p) status(p, Status.DOORS_CLOSED, ticks);
    }

    /** A hidden enemy is forced out of hiding: the door it hid behind bursts open round it. */
    public static void forcedOut(ServerLevel sl, LivingEntity who) {
        flash(sl, DOOR_SIGIL, chestOf(who), randomYaw(sl), 0xFFFFE7A8, Math.max(1.4f, who.getBbHeight()), 14);
        ParticleShapes.burst(sl, KEY, chestOf(who), 4, 0.05, 0.12);
        ParticleShapes.burst(sl, STAR_MOTE, chestOf(who), 10, 0.04, 0.1);
    }

    // =====================================================================================
    // The passives that show
    // =====================================================================================

    /** Starlit Eyes (Secrets Sorcerer): a faint orbit of Worms of Star round the Beyonder for 'ticks' (renew it). */
    public static void wormAura(ServerLevel sl, Player player, int ticks, String tag) {
        for (int i = 0; i < 3; i++) {
            float scale = 0.3f;
            ModelFx worm = WORM_OF_STAR.color(0xC0FFFFFF).scale(scale).pivot(8, 6, 8 + 16f * 0.75f / scale)
                    .lifetime(ticks).fade(10, 10).spin(i % 2 == 0 ? 5f : -4f).tag(tag)
                    .key(0, pose().spin(i * 120f));
            ParticleShapes.modelOn(sl, worm, player, new Vec3(0, 0.6 + i * 0.5, 0), 0f, 0f, 0f);
        }
    }

    /** Spiritual Intuition: something is hunting the owner or aiming at them from 'from': a chime and a star that way. */
    public static void intuition(ServerPlayer owner, Vec3 from) {
        Vec3 eye = owner.getEyePosition();
        Vec3 way = from.subtract(eye);
        Vec3 at = way.lengthSqr() < 1e-4 ? eye.add(0, 0.6, 0) : eye.add(way.normalize().scale(1.6));
        ParticleShapes.spawnFor(owner, TWINKLE, at, Vec3.ZERO);
        soundFor(owner, at, ModSounds.STARS_INTUITION, 0.7f, 1.0f);
    }

    /** A Divination looks for the owner: a faint star over their head, only they see it. */
    public static void divinationFelt(ServerPlayer owner) {
        ParticleShapes.spawnFor(owner, STARLIT_MARK.color(0x80C8D4FF).scale(0.3f).lifetime(30).fade(4, 10),
                headOf(owner).add(0, 0.8, 0), Vec3.ZERO);
        soundFor(owner, owner.getEyePosition(), ModSounds.STARS_INTUITION, 0.4f, 1.3f);
    }

    /** Quick Study: a power Record could copy was cast by 'caster': a glint on it, only the owner sees and hears. */
    public static void quickStudy(ServerPlayer owner, LivingEntity caster) {
        ParticleShapes.spawnFor(owner, SPARKLE.size(0.2f), headOf(caster).add(0, 0.5, 0), Vec3.ZERO);
        ParticleShapes.spawnFor(owner, PAGE, headOf(caster).add(0, 0.8, 0), new Vec3(0, 0.02, 0));
        soundFor(owner, chestOf(caster), ModSounds.STARS_QUICK_STUDY, 0.6f, 1.0f);
    }

    /** Safe Landing after a teleport of this Pathway: stars puff at the feet. */
    public static void safeLanding(ServerLevel sl, Player player) {
        ParticleShapes.ring(sl, STAR_MOTE, player.position().add(0, 0.1, 0), 0.6, 10, 0.06);
    }

    /** Starlit Eyes broke an illusion or shook off blindness: a crack of light before the eyes. */
    public static void illusionBroken(ServerLevel sl, Player player) {
        ParticleShapes.burst(sl, CRACK, player.getEyePosition().add(player.getLookAngle().scale(0.6)), 4, 0.03, 0.08);
        ParticleShapes.burst(sl, SPARKLE, player.getEyePosition(), 4, 0.03, 0.08);
    }

    /** Afterimages (Planeswalker): a hit passed through an illusory door and missed. */
    public static void afterimageMiss(ServerLevel sl, Player player) {
        flash(sl, DOOR_SIGIL, chestOf(player), randomYaw(sl), 0xC0C8D4FF, 1.4f, 10);
        afterimage(sl, player, player.position(), player.getYRot(), 0x80C8D4FF, 8);
    }

    /** Door of Doors (Door): a Key of Stars turns over the Beyonder when one of their free travels is used. */
    public static void doorOfDoors(ServerLevel sl, Player player) {
        ModelFx key = STAR_KEY.scale(0.35f).lifetime(16).fade(2, 8)
                .key(0, pose().alpha(0f))
                .key(4, pose().alpha(1f), Ease.OUT)
                .key(10, pose().roll(90f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, key, player, new Vec3(0, player.getBbHeight() + 0.6, 0), player.getYRot() + 90f, 0f, 0f);
        sound(sl, player.position(), ModSounds.STARS_KEY_TURN, 0.5f, wobble(1.2f));
    }

    // =====================================================================================
    // /starsfx: every display, to look at in game
    // =====================================================================================

    @FunctionalInterface
    private interface Show {
        void play(ServerLevel sl, Player viewer, LivingEntity target, Vec3 spot);
    }

    /** onViewer = it is shown on the viewer (worn), not on a spot or a target. */
    private record Case(String name, int ticks, boolean onViewer, Show show) {}

    private static String tagFor(String name) {
        return "ks_fx_" + name + "_" + UUID.randomUUID();
    }

    private static final List<Case> CASES = List.of(
            new Case("door_opening", 40, true, (sl, v, t, spot) -> {
                Vec3 from = v.position().add(ahead(v.getYRot()).scale(1.5));
                doorOpening(sl, v, from, v.position().add(ahead(v.getYRot()).scale(4.5)));
            }),
            new Case("real_door", 20, false, (sl, v, t, spot) -> realDoor(sl, spot.add(0, 1.0, 0))),
            new Case("slip_free", 40, true, (sl, v, t, spot) -> {
                slipFree(sl, v, t);
                Later.run(sl, 20, () -> travelUnbarred(sl, v));
            }),
            new Case("flash", 50, true, (sl, v, t, spot) -> {
                flash(sl, v, 14);
                if (t != v) Later.run(sl, 2, () -> blinded(sl, t, 40));
            }),
            new Case("black_screen", 90, false, (sl, v, t, spot) -> {
                String tag = tagFor("screen");
                blackScreen(sl, spot, v.getYRot(), 80, tag);
                Later.run(sl, 80, () -> blackScreenEnd(sl, spot, tag));
            }),
            new Case("loud_noise", 40, false, (sl, v, t, spot) -> {
                loudNoise(sl, v, spot);
                if (t != v) Later.run(sl, 2, () -> deafened(sl, t, 40));
            }),
            new Case("frost_ray", 50, false, (sl, v, t, spot) -> {
                Vec3 end = t == v ? v.getEyePosition().add(v.getLookAngle().scale(10)) : chestOf(t);
                frostRay(sl, v, handOf(v, sl), end);
                if (t != v) Later.run(sl, 4, () -> frosted(sl, t, 40, tagFor("frost")));
            }),
            new Case("gale", 40, true, (sl, v, t, spot) -> {
                gale(sl, v, 12);
                if (t != v) Later.run(sl, 4, () -> galeHit(sl, t));
            }),
            new Case("tumble", 130, false, (sl, v, t, spot) -> {
                String tag = tagFor("tumble");
                tumble(sl, v, spot, 5, 120, tag);
                for (int i = 10; i < 120; i += 10) Later.run(sl, i, () -> tumbleTick(sl, spot, 5));
                if (t != v) for (int i = 0; i < 3; i++) Later.run(sl, 20 + i * 30, () -> slipped(sl, t, 20));
                Later.run(sl, 120, () -> tumbleEnd(sl, spot, tag));
            }),
            new Case("escape_trick", 40, true, (sl, v, t, spot) -> {
                escapeTrick(sl, v, v.position(), v.position().add(ahead(v.getYRot()).scale(4)), false);
                Later.run(sl, 20, () -> escapeTrick(sl, v, v.position(), v.position().add(ahead(v.getYRot() + 90f).scale(10)), true));
            }),
            new Case("star_divination", 50, true, (sl, v, t, spot) -> {
                starDivination(sl, v, t == v ? null : t);
                if (t != v) Later.run(sl, DIVINED, () -> starlit(sl, v, t, 30, tagFor("starlit")));
            }),
            new Case("divination_sweep", 60, true, (sl, v, t, spot) -> {
                starDivination(sl, v, null);
                if (v instanceof ServerPlayer sp && t != v) Later.run(sl, 10, () -> smallStar(sp, t, 40));
            }),
            new Case("starlit", 90, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("starlit");
                starlit(sl, v, t, 80, tag);
                if (v instanceof ServerPlayer sp) for (int i = 1; i < 4; i++) Later.run(sl, i * 20, () -> starlitSeen(sp, t));
                Later.run(sl, 80, () -> starlitEnd(sl, t, tag));
            }),
            new Case("dimension_sight", 50, false, (sl, v, t, spot) -> {
                if (t == v || !(v instanceof ServerPlayer sp)) return;
                for (int i = 0; i < 2; i++) Later.run(sl, i * 20, () -> dimensionSight(sp, t));
            }),
            new Case("interference", 110, true, (sl, v, t, spot) -> {
                String tag = tagFor("interference");
                interference(sl, v, 100, tag);
                for (int i = 10; i < 100; i += 10) Later.run(sl, i, () -> interferenceTick(sl, v));
                if (t != v) {
                    Later.run(sl, 40, () -> interferenceTurned(sl, v, t));
                    Later.run(sl, 60, () -> mobForgets(sl, t));
                }
                Later.run(sl, 100, () -> interferenceEnd(sl, v, tag));
            }),
            new Case("record", 110, true, (sl, v, t, spot) -> {
                String tag = tagFor("record");
                record(sl, v, 100, tag);
                Vec3 from = t == v ? spot.add(0, 1.2, 0) : chestOf(t);
                Later.run(sl, 30, () -> {
                    recorded(sl, v, from, false);
                    recordedPages(v, new boolean[]{false});
                });
                Later.run(sl, 60, () -> {
                    recorded(sl, v, from, true);
                    recordedPages(v, new boolean[]{false, true});
                });
                Later.run(sl, 85, () -> recordFailed(sl, v));
                Later.run(sl, 100, () -> recordEnd(sl, v, tag));
            }),
            new Case("replay", 40, true, (sl, v, t, spot) -> {
                replay(sl, v);
                recordedPages(v, new boolean[]{true});
                Later.run(sl, 30, () -> recordedPagesHide(v));
            }),
            new Case("travelers_door", 60, true, (sl, v, t, spot) -> {
                travelersDoor(sl, v, v.position());
                Vec3 out = v.position().add(ahead(v.getYRot()).scale(8));
                Later.run(sl, TRAVEL_ENTERED + 8, () -> travelersArrive(sl, out, v.getYRot()));
            }),
            new Case("blink", 50, true, (sl, v, t, spot) -> {
                Vec3 a = v.position();
                for (int i = 0; i < 3; i++) {
                    int n = i;
                    Vec3 from = a.add(ahead(v.getYRot()).scale(n * 4));
                    Vec3 to = a.add(ahead(v.getYRot()).scale(n * 4 + 4));
                    Later.run(sl, i * 8, () -> blink(sl, v, from, to, n));
                }
                Later.run(sl, 30, () -> blinkDodge(sl, v));
            }),
            new Case("blink_echo", 90, false, (sl, v, t, spot) -> {
                String tag = tagFor("echo");
                blinkEcho(sl, v, spot, 80, tag);
                quickened(v, 60);
            }),
            new Case("invisible_hand", 40, false, (sl, v, t, spot) -> {
                if (t == v) {
                    invisibleHandItem(sl, v, spot.add(0, 0.5, 0));
                    return;
                }
                invisibleHand(sl, v, t);
                Later.run(sl, HAND_GRABS + 6, () -> yanked(sl, t));
            }),
            new Case("star_anchor", 110, false, (sl, v, t, spot) -> {
                String tag = tagFor("anchor");
                starAnchor(sl, v, spot, 100, tag);
                if (v instanceof ServerPlayer sp) for (int i = 1; i < 5; i++) Later.run(sl, i * 20, () -> anchorDirection(sp, spot));
                Later.run(sl, 100, () -> anchorLift(sl, spot, tag));
            }),
            new Case("wandering", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("wander");
                wandering(sl, v, 80, Form.WANDERING, tag);
                for (int i = 2; i < 80; i += 2) Later.run(sl, i, () -> wanderingTick(sl, v, Form.WANDERING));
                Later.run(sl, 80, () -> wanderingEnd(sl, v, tag));
            }),
            new Case("conceptualization", 70, true, (sl, v, t, spot) -> {
                String tag = tagFor("wander");
                wandering(sl, v, 60, Form.CONCEPTUALIZATION, tag);
                for (int i = 2; i < 60; i += 2) Later.run(sl, i, () -> wanderingTick(sl, v, Form.CONCEPTUALIZATION));
                if (t != v) Later.run(sl, 20, () -> conceptualized(sl, t));
                Later.run(sl, 60, () -> wanderingEnd(sl, v, tag));
            }),
            new Case("space_concealment", 210, false, (sl, v, t, spot) -> {
                String tag = tagFor("conceal");
                float doorYaw = yawToward(spot, v.position()) + 180f;
                spaceConcealment(sl, v, spot, 6, 200, doorYaw, false, tag);
                Later.run(sl, 40, () -> concealedHeal(sl, v));
                Later.run(sl, 100, () -> concealmentShut(sl, spot, 6, doorYaw, tag));
                Later.run(sl, 200, () -> concealmentEnd(sl, spot, 6, tag));
            }),
            new Case("spatial_cage", 110, false, (sl, v, t, spot) -> {
                String tag = tagFor("cage");
                spaceConcealment(sl, v, spot, 4, 100, v.getYRot() + 90f, true, tag);
                Later.run(sl, 100, () -> concealmentEnd(sl, spot, 4, tag));
            }),
            new Case("transfiguration", 120, true, (sl, v, t, spot) -> {
                String tag = tagFor("transfig");
                transfiguration(sl, v, 5, 110, tag);
                for (int i = 1; i <= 3; i++) {
                    int left = 5 - i;
                    Later.run(sl, i * 25, () -> {
                        Vec3 from = v.position();
                        layerTorn(sl, v, from, from.add(ahead(v.getYRot() + 90f).scale(3)), left);
                    });
                }
                Later.run(sl, 110, () -> transfigurationEnd(sl, v, tag));
            }),
            new Case("exile", 140, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("exile");
                Vec3 at = t.position();
                exile(sl, v, t);
                Later.run(sl, EXILE_SWALLOWED, () -> exiled(sl, t, at, 100, tag));
                Later.run(sl, EXILE_SWALLOWED + 100, () -> exileReturn(sl, at, tag));
            }),
            new Case("pocket_space", 30, true, (sl, v, t, spot) -> pocketSpace(sl, v)),
            new Case("space_tearing", 110, false, (sl, v, t, spot) -> {
                if (t == v) return;
                spaceTearing(sl, v, t, 3);
                String tag = tagFor("storm");
                Later.run(sl, TEAR_LANDS, () -> storm(sl, t, 80, tag));
                for (int i = 1; i < 4; i++) Later.run(sl, TEAR_LANDS + i * 20, () -> stormTick(sl, t));
            }),
            new Case("toy_box", 170, false, (sl, v, t, spot) -> {
                String tag = tagFor("toybox");
                Vec3 c = t == v ? spot : t.position();
                toyBox(sl, v, c, 8, 160, tag);
                if (t != v) {
                    String toy = tagFor("toy");
                    toyed(sl, t, 140, toy);
                    for (int i = 1; i < 6; i++) Later.run(sl, TOY_SHRUNK + i * 20, () -> toyTick(sl, t));
                    Later.run(sl, 140, () -> toyReleased(sl, t, toy));
                }
                Later.run(sl, 160, () -> toyBoxEnd(sl, c, tag));
            }),
            new Case("replication", 30, false, (sl, v, t, spot) -> {
                if (t != v) replication(sl, v, t);
            }),
            new Case("space_fragmentation", 140, false, (sl, v, t, spot) -> {
                String tag = tagFor("fragment");
                spaceFragmentation(sl, v, spot, 10, 120, tag);
                for (int i = 5; i < 120; i += 5) Later.run(sl, i, () -> fragmentationTick(sl, spot, 10));
                Later.run(sl, 120, () -> fragmentationCollapse(sl, spot, 10, tag));
            }),
            new Case("starlight_seal", 140, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("seal");
                starlightSeal(sl, v, t, 110, tag);
                Later.run(sl, SEALED_AT + 50, () -> sealRepaired(sl, t));
                Later.run(sl, SEALED_AT + 110, () -> sealEnd(sl, t, tag));
            }),
            new Case("seal_ally", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("sealally");
                sealAlly(sl, v, v, 80, tag);
            }),
            new Case("labyrinth", 210, false, (sl, v, t, spot) -> {
                String tag = tagFor("maze");
                labyrinth(sl, v, spot, 10, 200, tag);
                if (t != v) {
                    Later.run(sl, LABYRINTH_RISEN, () -> lost(sl, t, 180));
                    Later.run(sl, 80, () -> labyrinthLoop(sl, spot.add(ahead(v.getYRot()).scale(10)),
                            spot.add(ahead(v.getYRot() + 180f).scale(9))));
                }
                Later.run(sl, 200, () -> labyrinthEnd(sl, spot, 10, tag));
            }),
            new Case("alternate_world", 420, true, (sl, v, t, spot) -> {
                String tag = tagFor("world");
                Vec3 c = v.position();
                alternateWorld(sl, v, c, 24, 400, tag);
                for (int i = 1; i < 20; i++) Later.run(sl, i * 20, () -> alternateWorldTick(sl, c, 24));
                Later.run(sl, 400, () -> alternateWorldEnd(sl, c, 24, tag));
            }),
            new Case("omnipresence", 130, true, (sl, v, t, spot) -> {
                String tag = tagFor("omni");
                omnipresence(sl, v, 120, tag);
                for (int i = 0; i < 6; i++) {
                    int n = i;
                    Later.run(sl, 30 + i * 4, () -> omnipresenceEcho(sl, v, n));
                }
                Later.run(sl, 80, () -> afterimageStruck(sl, v, tag, 2));
                Later.run(sl, 120, () -> omnipresenceEnd(sl, v, tag));
            }),
            new Case("close_the_door", 120, true, (sl, v, t, spot) -> {
                String tag = tagFor("close");
                Vec3 c = v.position();
                closeTheDoor(sl, v, c, 24, 100, tag);
                if (t != v) {
                    Later.run(sl, DOORS_SHUT + 4, () -> doorsClosed(sl, t, 80, tagFor("closed")));
                    Later.run(sl, DOORS_SHUT + 8, () -> forcedOut(sl, t));
                }
                Later.run(sl, 100, () -> ParticleShapes.clearModels(sl, c, tag));
            }),
            new Case("passives", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("worms");
                wormAura(sl, v, 80, tag);
                if (v instanceof ServerPlayer sp) {
                    intuition(sp, spot.add(0, 1.0, 0));
                    Later.run(sl, 15, () -> divinationFelt(sp));
                    if (t != v) Later.run(sl, 30, () -> quickStudy(sp, t));
                }
                Later.run(sl, 40, () -> safeLanding(sl, v));
                Later.run(sl, 50, () -> illusionBroken(sl, v));
                Later.run(sl, 60, () -> afterimageMiss(sl, v));
                Later.run(sl, 70, () -> doorOfDoors(sl, v));
            }),
            new Case("statuses", 100, true, (sl, v, t, spot) -> {
                for (Status s : Status.values()) status(v, s, 90);
                Later.run(sl, 30, () -> layersLeft(v, 3, 60));
                recordedPages(v, new boolean[]{false, false, true, false, true});
                Later.run(sl, 90, () -> recordedPagesHide(v));
            }));

    /** The names /starsfx knows. */
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
        event.getDispatcher().register(Commands.literal("starsfx")
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
