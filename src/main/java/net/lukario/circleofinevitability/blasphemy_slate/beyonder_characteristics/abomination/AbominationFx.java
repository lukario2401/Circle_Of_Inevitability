package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination;

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
 * How the Abomination (Chained) pathway looks and sounds: every key, both casts, every wheel entry, and the passives
 * that show. Visual only: nothing here hurts, binds, curses, hides or moves anyone. The ability code does that, at
 * the ticks named by the constants below (..._REACHES, ..._LOCKED, ..._RISEN, ..._SHUT...), with Later.run.
 * See docs/ABOMINATION.md for which call goes with which ability.
 *
 * ---------- See them in game ----------
 *   /abominationfx all            every display one after another (/abominationfx all 3 = three times over)
 *   /abominationfx coffin         just that one (press TAB after /abominationfx for the names)
 * Displays that need a target use the nearest mob within 12 blocks, or you when there is none.
 *
 * ---------- Use them in an ability ----------
 *   Key 1  bloodChain(sl, player, target) (it reaches at CHAIN_REACHES), bloodChainHold(sl, player, target, ticks, tag),
 *              bloodChainEnd(sl, target, tag); bleed(sl, target) once a second while Bleeding lasts
 *          sneak: restraint(sl, player, ticks, tag) (locked at RESTRAINT_LOCKED), restraintHit(sl, player), restraintEnd(sl, player, tag)
 *   Key 2  moonstruck(sl, player, target, ticks, tag) (the moon is up at MOON_SHOWN), madnessTick(sl, target, laugh),
 *              moonstruckEnd(sl, target, tag)
 *          sneak: lunarFrenzy(sl, player, ticks, tag), frenzyTick(sl, player), frenzyCrash(sl, player, ticks, tag), frenzyEnd(sl, player, tag)
 *   Key 3  pounceLeap(sl, player), pounceTrail(sl, player) every tick of the leap, pounceClaw(sl, player, target, index) for 0..2
 *          sneak: werewolfForm(sl, player, height, ticks, tag) (full at WOLF_RISEN), werewolfClaw(sl, player, target),
 *              werewolfMadness(sl, player, madness, height), howl(sl, player), werewolfFormEnd(sl, player, tag)
 *   Key 4  the Zombie wheel's pictures: WHEEL_RAISE_THE_DEAD, WHEEL_GRAVE_CHILL, WHEEL_UNDYING_FLESH, WHEEL_DEATH_SENTENCE
 *          raiseTheDead(sl, ground, radius), zombieRises(sl, at) (spawn the zombie at HANDS_UP), fallenRises(sl, at),
 *              servantMark(sl, servant, ticks, tag)
 *          graveChill(sl, player, radius, ticks, tag) (spread at CHILL_SPREAD), graveChillTick(sl, player, radius),
 *              chilled(sl, enemy, cold), chillFreeze(sl, enemy), graveChillEnd(sl, player, tag)
 *          undyingFlesh(sl, player, ticks, tag), undyingTick(sl, player), undyingHit(sl, player), undyingEnd(sl, player, tag)
 *          deathSentence(sl, player, target, ticks, tag), sentenceTick(sl, target, secondsLeft),
 *              sentenceFalls(sl, target, tag) (the blow lands at SENTENCE_LANDS), sentenceLifted(sl, target, tag)
 *   Key 5  the Wraith wheel's pictures: WHEEL_MIRROR_STEP, WHEEL_POSSESSION, WHEEL_GHOST_HOST
 *          mirrorEnter(sl, player, mirror) (gone at MIRROR_OPENS), mirrorExit(sl, player, mirror), reflectionHint(viewer, at)
 *          possessionDive(sl, player, host) (inside at GHOST_ARRIVES), possessed(sl, host, ticks, tag), possessionTick(sl, host),
 *              bodyLeft(sl, player, ticks, tag), possessionExit(sl, host, player, tag)
 *          ghostHost(sl, player, count) (the ghosts are up at GHOSTS_RISEN), ghostServant(sl, ghost),
 *              ghostTakenOver(sl, mob, ticks, tag), ghostFades(sl, at)
 *   Key 6  the Chains wheel's pictures: WHEEL_ANIMATE_OBJECTS, WHEEL_GRAVE_SHACKLES, WHEEL_THORNED_MAUSOLEUM,
 *              WHEEL_COFFIN_OF_AGES, WHEEL_THORN_GARDEN
 *          animateObjects(sl, player, things) (they rise at OBJECTS_RISE), hurlLaunch(sl, thing), hurledTrail(sl, thing), hurledHit(sl, at)
 *          graveShackles(sl, enemies, ticks, tag) (locked at SHACKLES_LOCK), painShared(sl, from, to), graveShacklesEnd(sl, enemies, tag)
 *          mausoleum(sl, center, radius, ticks, tag) (sealed at MAUSOLEUM_SEALED), mausoleumTick(sl, center, radius),
 *              mausoleumThorn(sl, enemy), mausoleumEnd(sl, center, radius, tag)
 *          coffinOfAges(sl, target, ticks, tag) (shut at COFFIN_SHUT), coffinOpen(sl, target, tag) (open at COFFIN_OPENED);
 *              on yourself the same calls, then coffinRise(sl, player, radius) for the burst of thorns
 *          thornGarden(sl, center, radius, ticks, tag) (grown at GARDEN_GROWN), gardenTick(sl, center, radius),
 *              gardenThorn(sl, enemy, held), roseHeal(sl, ally), thornGardenEnd(sl, center, tag)
 *   Key 7  the Curses wheel's pictures: WHEEL_CURSE_BY_CONNECTION, WHEEL_CURSED_ARTIFACT, WHEEL_RITE_OF_SILENCE,
 *              WHEEL_BANE_MIST, WHEEL_ANCIENT_BLIGHT, WHEEL_ABOMINATION_FORM
 *          curseByConnection(sl, player, target, ticks, tag) (it lands at CURSE_LANDS), connectionMade(owner, target)
 *          relicPlaced(owner, at), relicTick(owner, at), relicTriggered(sl, victim, at, ticks, tag)
 *          riteOfSilence(sl, center, radius, ticks, tag) (it falls at RITE_FALLS), silenced(sl, enemy), riteEnd(sl, center, tag)
 *          baneMist(sl, center, radius, ticks, tag), baneMistTick(sl, center, radius), baneMistOn(sl, enemy, level), baneMistEnd(sl, center, tag)
 *          blight(sl, target, stage, ticks, tag), blightJump(sl, from, to, stage)
 *          abominationForm(sl, player, height, ticks, tag) (whole at FORM_RISEN), abominationTick(sl, player, radius),
 *              abominationCursed(sl, attacker), calamity(sl, victim), abominationFormEnd(sl, player, height, tag)
 *   Passives: fullMoonRises(sl, player), keenSenses(player), spiritSeen(viewer, spirit), whisperingCurse(sl, attacker), baneAura(sl, player, radius),
 *          chainedAura(sl, player, radius), thornsBack(sl, attacker)
 *   Every timed cast: status(player, Status.X, ticks) shows its picture and its clock; statusEnd(player, Status.X) takes them away.
 *
 * The models are in models/model_fx/abomination/ (made by tools/model_gen/abomination/models.py; each JSON's
 * "credit" gives its size and pivot), the pictures in textures/gui/abomination/ and
 * textures/gui/ability_menus/abomination/ (icons.py), the particles in textures/particle/fx/ (particles.py), the
 * sounds in sounds/abomination/ (sounds.py) and sounds/ModSounds (ABOMINATION_...).
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class AbominationFx {

    private AbominationFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // Colors (ARGB)
    // =====================================================================================

    /** The Pathway's own color: eminence purple. */
    public static final int PATHWAY_COLOR = 0xFF6C3082;
    public static final int PURPLE_LIGHT = 0xFFB070FF;
    public static final int BLOOD = 0xFFB01020;
    public static final int BLOOD_DARK = 0xFF5A0610;
    public static final int IRON = 0xFF5A5068;
    public static final int MOON = 0xFFE8E4D6;
    public static final int MAD_PINK = 0xFFFF7AC0;
    public static final int ICE = 0xFF96D2FF;
    public static final int WOLF = 0xFF4C6050;
    public static final int WOLF_EYE = 0xFFDCFF78;
    public static final int ROT = 0xFF7A9060;
    public static final int GHOST = 0xFFC8E0FA;
    public static final int BANDAGE = 0xFFD6BE6E;
    public static final int THORN = 0xFF2A1430;
    public static final int ROSE = 0xFFE61832;
    public static final int MIST = 0xFFBEAAB4;

    // =====================================================================================
    // The small particles everything shares
    // =====================================================================================

    private static final ParticleFx BLOOD_DROP = ParticleFx.of("fx/droplet")
            .color(0xFFC01020).endColor(0xE0600010)
            .size(0.06f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(24, 10).gravity(0.06f).friction(0.98f).collide();
    private static final ParticleFx BLOOD_MIST = ParticleFx.of("fx/smoke")
            .color(0xA0801018).endColor(0x00300408)
            .size(0.2f).endSize(0.5f).sizeRandom(0.3f)
            .lifetime(20, 8).gravity(0.002f).friction(0.9f).spin(2f).randomRotation();
    private static final ParticleFx BLOOD_LINK = ParticleFx.of("fx/chain_link")
            .color(0xFFC02030).endColor(0x80600010)
            .size(0.08f).endSize(0.06f).sizeRandom(0.3f)
            .lifetime(20, 8).gravity(0.04f).friction(0.94f).spin(8f).randomRotation();
    private static final ParticleFx IRON_LINK = ParticleFx.of("fx/chain_link")
            .color(0xFF7A6E8A).endColor(0x80302838)
            .size(0.08f).endSize(0.07f).sizeRandom(0.3f)
            .lifetime(22, 8).gravity(0.05f).friction(0.94f).spin(8f).randomRotation();
    private static final ParticleFx PADLOCK = ParticleFx.of("fx/padlock")
            .color(0xFFC8B0E0).endColor(0x006C3082)
            .size(0.12f).endSize(0.1f)
            .lifetime(22, 6).gravity(-0.01f).friction(0.9f).glow();
    private static final ParticleFx PURPLE_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFC890FF).endColor(0x003A0A5A)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(-0.02f).friction(0.92f).glow();
    private static final ParticleFx MOON_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFF4F0E4).endColor(0x00A090C0)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(-0.015f).friction(0.92f).glow();
    private static final ParticleFx CRESCENT = ParticleFx.of("fx/crescent")
            .color(0xFFF0E8FF).endColor(0x00C080FF)
            .size(0.12f).endSize(0.08f).sizeRandom(0.3f)
            .lifetime(26, 10).gravity(-0.01f).friction(0.92f).glow().spin(6f).randomRotation();
    private static final ParticleFx MAD_SPARK = ParticleFx.of("fx/sparkle")
            .color(0xFFFF9AD8).endColor(0x00A02080)
            .size(0.08f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(16, 8).gravity(-0.02f).friction(0.9f).glow().spin(12f).randomRotation();
    private static final ParticleFx FRENZY_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFF5050).endColor(0x00600010)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(18, 8).gravity(-0.03f).friction(0.9f).glow();
    private static final ParticleFx BREATH = ParticleFx.of("fx/smoke")
            .color(0x80E0E0E8).endColor(0x00A0A0B0)
            .size(0.12f).endSize(0.35f).sizeRandom(0.3f)
            .lifetime(14, 6).gravity(-0.01f).friction(0.9f).spin(3f).randomRotation();
    private static final ParticleFx GREY_SMOKE = ParticleFx.of("fx/smoke")
            .color(0x90606068).endColor(0x00202024)
            .size(0.3f).endSize(0.7f).sizeRandom(0.3f)
            .lifetime(26, 10).gravity(0.004f).friction(0.9f).spin(1.5f).randomRotation();
    private static final ParticleFx DUST = ParticleFx.of("fx/smoke")
            .color(0xA0706050).endColor(0x00403020)
            .size(0.3f).endSize(0.7f).sizeRandom(0.3f)
            .lifetime(22, 10).gravity(-0.005f).friction(0.88f).spin(2f).randomRotation();
    private static final ParticleFx WOLF_HAIR = ParticleFx.of("fx/hair")
            .color(0xFF5A6E5E).endColor(0x80202A22)
            .size(0.08f).endSize(0.07f).sizeRandom(0.4f)
            .lifetime(30, 10).gravity(0.02f).friction(0.94f).spin(6f).randomRotation();
    private static final ParticleFx WOLF_GLINT = ParticleFx.of("fx/glow")
            .color(0xFFE8FF90).endColor(0x0080A020)
            .size(0.06f).endSize(0.02f)
            .lifetime(10, 4).gravity(0f).friction(0.9f).glow();
    private static final ParticleFx CLAW_P = ParticleFx.of("fx/claw")
            .color(0xFFE02838).endColor(0x00600010)
            .size(0.16f).endSize(0.12f)
            .lifetime(10, 4).gravity(0f).friction(0.9f).glow();
    private static final ParticleFx EARTH = ParticleFx.of("fx/ash")
            .color(0xFF5A4028).endColor(0xC03A2818)
            .size(0.08f).endSize(0.07f).sizeRandom(0.5f)
            .lifetime(24, 10).gravity(0.06f).friction(0.96f).spin(6f).randomRotation().collide();
    private static final ParticleFx GRAVE_WISP = ParticleFx.of("fx/wisp")
            .color(0xC0A0E0A0).endColor(0x00406040)
            .size(0.1f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(30, 10).gravity(-0.02f).friction(0.94f).glow();
    private static final ParticleFx FROST = ParticleFx.of("fx/flake")
            .color(0xFFE0F4FF).endColor(0x0080C0FF)
            .size(0.07f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(30, 10).gravity(0.01f).friction(0.95f).glow().spin(4f).randomRotation();
    private static final ParticleFx FROST_MIST = ParticleFx.of("fx/smoke")
            .color(0x70B8E0FF).endColor(0x0060A0D0)
            .size(0.4f).endSize(0.9f).sizeRandom(0.3f)
            .lifetime(34, 12).gravity(0.002f).friction(0.92f).spin(1f).randomRotation();
    private static final ParticleFx ROT_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFA0D080).endColor(0x00305020)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(20, 8).gravity(-0.02f).friction(0.92f).glow();
    private static final ParticleFx SKULL = ParticleFx.of("fx/skull")
            .color(0xFFE0C8FF).endColor(0x006C3082)
            .size(0.12f).endSize(0.1f).sizeRandom(0.3f)
            .lifetime(28, 10).gravity(-0.02f).friction(0.92f).glow();
    private static final ParticleFx GLASS = ParticleFx.of("fx/sparkle")
            .color(0xFFF0F8FF).endColor(0x0090B0E0)
            .size(0.08f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(16, 8).gravity(0f).friction(0.88f).glow().spin(10f).randomRotation();
    private static final ParticleFx GHOST_WISP = ParticleFx.of("fx/wisp")
            .color(0xC0D8ECFF).endColor(0x006080C0)
            .size(0.12f).endSize(0.06f).sizeRandom(0.4f)
            .lifetime(28, 10).gravity(-0.015f).friction(0.94f).glow();
    private static final ParticleFx STRING_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFD8B0FF).endColor(0x006C3082)
            .size(0.06f).endSize(0.02f)
            .lifetime(12, 4).gravity(0f).friction(0.9f).glow();
    private static final ParticleFx[] CURSE = {
            ParticleFx.of("fx/curse_0").color(0xFFC890FF).endColor(0x003A0A5A).size(0.1f).endSize(0.08f)
                    .lifetime(28, 10).gravity(-0.015f).friction(0.92f).glow(),
            ParticleFx.of("fx/curse_1").color(0xFFC890FF).endColor(0x003A0A5A).size(0.1f).endSize(0.08f)
                    .lifetime(28, 10).gravity(-0.015f).friction(0.92f).glow()};
    private static final ParticleFx STITCH = ParticleFx.of("fx/stitch")
            .color(0xFFE8DCB0).endColor(0x00806040)
            .size(0.1f).endSize(0.08f)
            .lifetime(24, 8).gravity(-0.005f).friction(0.9f).glow();
    private static final ParticleFx BANE_MIST = ParticleFx.of("fx/smoke")
            .color(0x80C8B4BE).endColor(0x00504048)
            .size(0.6f).endSize(1.3f).sizeRandom(0.4f)
            .lifetime(50, 20).gravity(-0.002f).friction(0.95f).spin(0.8f).randomRotation();
    private static final ParticleFx BANE_BLOOD = ParticleFx.of("fx/glow")
            .color(0xFFA01020).endColor(0x00400008)
            .size(0.07f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(40, 10).gravity(0f).friction(0.95f);
    private static final ParticleFx BANE_BLACK = ParticleFx.of("fx/glow")
            .color(0xFF100810).endColor(0x00100810)
            .size(0.08f).endSize(0.06f).sizeRandom(0.4f)
            .lifetime(40, 10).gravity(0f).friction(0.95f);
    private static final ParticleFx THORN_P = ParticleFx.of("fx/thorn")
            .color(0xFF4A2A58).endColor(0x80200C28)
            .size(0.1f).endSize(0.08f).sizeRandom(0.4f)
            .lifetime(24, 8).gravity(0.03f).friction(0.94f).spin(6f).randomRotation();
    private static final ParticleFx BANDAGE_P = ParticleFx.of("fx/bandage")
            .color(0xFFE0CC80).endColor(0x80806020)
            .size(0.12f).endSize(0.1f).sizeRandom(0.4f)
            .lifetime(36, 12).gravity(0.008f).friction(0.95f).spin(5f).randomRotation();
    private static final ParticleFx PETAL = ParticleFx.of("fx/petal")
            .color(0xFFFF2A40).endColor(0xC0900010)
            .size(0.08f).endSize(0.07f).sizeRandom(0.4f)
            .lifetime(40, 14).gravity(0.012f).friction(0.95f).spin(5f).randomRotation();
    private static final ParticleFx ROSE_GLOW = ParticleFx.of("fx/glow")
            .color(0xFFFF8090).endColor(0x00FF2040)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(20, 8).gravity(-0.025f).friction(0.92f).glow();

    private static final SlashFx BLOOD_TETHER = SlashFx.line("slash/dashed")
            .color(0xE0B01020).core(0xFFFF6070).width(0.16f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx IRON_TETHER = SlashFx.line("slash/dashed")
            .color(0xC05A5068).core(0xFFB070FF).width(0.14f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx STRING = SlashFx.line("slash/smooth")
            .color(0xA0B070FF).core(0xFFF0E0FF).width(0.04f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx CURSE_LINE = SlashFx.line("slash/dashed_fine")
            .color(0xC06C3082).core(0xFFE0C0FF).width(0.12f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx PAIN_LINE = SlashFx.line("slash/dashed_fine")
            .color(0xC0B01020).core(0xFFFF8090).width(0.1f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx GHOST_LINE = SlashFx.line("slash/smooth")
            .color(0x80C8E0FA).core(0xC0FFFFFF).width(0.3f).taper(SlashFx.Taper.COMET);
    private static final SlashFx LEAP_LINE = SlashFx.line("slash/streaks")
            .color(0x904C6050).core(0x80C0D0C0).width(0.3f).taper(SlashFx.Taper.COMET);
    private static final SlashFx CLAW_ARC = SlashFx.arc("slash/streaks")
            .color(0xE0E02838).core(0xFFFFC0C0)
            .radius(1.1f).arc(70f).width(0.18f).layers(3).spread(0.28f)
            .lifetime(7).sweep(2).taper(SlashFx.Taper.COMET);

    // =====================================================================================
    // Sounds
    // =====================================================================================

    /** One of the pathway's own sounds (sounds/ModSounds), for everyone near. */
    private static void sound(ServerLevel sl, Vec3 at, RegistryObject<SoundEvent> sound, float volume, float pitch) {
        Spells.sound(sl, at, sound.get(), volume, pitch);
    }

    /** A sound only this player hears (a relic only its owner knows of, a sense of being hunted). */
    private static void privateSound(ServerPlayer player, RegistryObject<SoundEvent> sound, float volume, float pitch) {
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
        return entity.position().add(0, entity.getBbHeight() * 0.9, 0);
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

    private static float random() {
        return ThreadLocalRandom.current().nextFloat();
    }

    private static ParticleFx curse() {
        return CURSE[ThreadLocalRandom.current().nextInt(CURSE.length)];
    }

    // =====================================================================================
    // Statuses: a picture in the row above the hotbar stacks, and a clock beside the hotbar
    // =====================================================================================

    /** The picture for the Pathway's stacks, if it gets them (textures/gui/abomination/stack_link.png). */
    public static final String STACK_IMAGE = "abomination/stack_link";

    public enum Status {
        RESTRAINT("restraint", "Restraint", 0x8A7AA0),
        BLEEDING("bleeding", "Bleeding", 0xC01020),
        MOONSTRUCK("moonstruck", "Moonstruck", 0xE8E4D6),
        FRENZY("frenzy", "Lunar Frenzy", 0xFF5050),
        FRENZY_CRASH("frenzy_crash", "Crash", 0x908880),
        WEREWOLF("werewolf", "Werewolf", 0x7A9A80),
        MADNESS("madness", "Madness", 0xD04080),
        FULL_MOON("full_moon", "Full Moon", 0xF4F0E4),
        RISEN("risen", "The Dead", 0x7A9060),
        GRAVE_CHILL("grave_chill", "Grave Chill", 0x96D2FF),
        UNDYING("undying", "Undying", 0x7A9060),
        SENTENCE("sentence", "Sentence", 0xB070FF),
        MIRROR("mirror", "In the Mirror", 0xB0C8F0),
        POSSESSION("possession", "Possession", 0xC8E0FA),
        GHOST_HOST("ghost_host", "Ghosts", 0xC8E0FA),
        SHACKLES("shackles", "Shackles", 0x8A7AA0),
        MAUSOLEUM("mausoleum", "Mausoleum", 0xD6BE6E),
        COFFIN("coffin", "Coffin", 0xA07050),
        THORN_GARDEN("thorn_garden", "Thorn Garden", 0xE61832),
        CONNECTION("connection", "Connection", 0xB070FF),
        CURSE("curse", "Curse", 0xB070FF),
        RELIC("relic", "Relics", 0xA08870),
        SILENCE("silence", "Silence", 0xE8DCB0),
        BANE_MIST("bane_mist", "Bane Mist", 0xBEAAB4),
        BLIGHT("blight", "Blight", 0x8A40A0),
        ABOMINATION("abomination", "Abomination", 0x6C3082);

        final String image;
        final String label;
        final int rgb;

        Status(String image, String label, int rgb) {
            this.image = image;
            this.label = label;
            this.rgb = rgb;
        }

        String id() {
            return "ab_status_" + name().toLowerCase();
        }
    }

    /** Shows the status's picture (in the row above the hotbar stacks, rows of nine) and a clock beside the hotbar. */
    public static void status(Player player, Status status, int ticks) {
        int slot = status.ordinal();
        ScreenImages.show(player, status.id(), "abomination/" + status.image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, ticks);
        HudTimer.show(player, status.id(), status.label, ticks, status.rgb);
    }

    /** How long something "until ended" is shown at most (an hour): show it again after that. */
    public static final int FOREVER = 72000;

    /** Only the status's picture, no clock: for what lasts until it is ended (Werewolf Form at Sequence 2, relics). */
    public static void statusForever(Player player, Status status) {
        int slot = status.ordinal();
        ScreenImages.show(player, status.id(), "abomination/" + status.image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, FOREVER);
        HudTimer.hide(player, status.id());
    }

    /** It ended early. */
    public static void statusEnd(Player player, Status status) {
        ScreenImages.hide(player, status.id());
        HudTimer.hide(player, status.id());
    }

    // =====================================================================================
    // The wheels' pictures (textures/gui/ability_menus/abomination/, for AbilityWheel.entry's icon)
    // =====================================================================================

    public static final String WHEEL_RAISE_THE_DEAD = "ability_menus/abomination/raise_the_dead";
    public static final String WHEEL_GRAVE_CHILL = "ability_menus/abomination/grave_chill";
    public static final String WHEEL_UNDYING_FLESH = "ability_menus/abomination/undying_flesh";
    public static final String WHEEL_DEATH_SENTENCE = "ability_menus/abomination/death_sentence";
    public static final String WHEEL_MIRROR_STEP = "ability_menus/abomination/mirror_step";
    public static final String WHEEL_POSSESSION = "ability_menus/abomination/possession";
    public static final String WHEEL_GHOST_HOST = "ability_menus/abomination/ghost_host";
    public static final String WHEEL_ANIMATE_OBJECTS = "ability_menus/abomination/animate_objects";
    public static final String WHEEL_GRAVE_SHACKLES = "ability_menus/abomination/grave_shackles";
    public static final String WHEEL_THORNED_MAUSOLEUM = "ability_menus/abomination/thorned_mausoleum";
    public static final String WHEEL_COFFIN_OF_AGES = "ability_menus/abomination/coffin_of_ages";
    public static final String WHEEL_THORN_GARDEN = "ability_menus/abomination/thorn_garden";
    public static final String WHEEL_CURSE_BY_CONNECTION = "ability_menus/abomination/curse_by_connection";
    public static final String WHEEL_CURSED_ARTIFACT = "ability_menus/abomination/cursed_artifact";
    public static final String WHEEL_RITE_OF_SILENCE = "ability_menus/abomination/rite_of_silence";
    public static final String WHEEL_BANE_MIST = "ability_menus/abomination/bane_mist";
    public static final String WHEEL_ANCIENT_BLIGHT = "ability_menus/abomination/ancient_blight";
    public static final String WHEEL_ABOMINATION_FORM = "ability_menus/abomination/abomination_form";

    // =====================================================================================
    // The models (models/model_fx/abomination/, see each JSON's "credit")
    // =====================================================================================

    /** The chain of blood, 3 blocks standing up, a hook at its top; held at its lower end. */
    public static final ModelFx BLOOD_CHAIN = ModelFx.of("abomination/blood_chain").pivot(8, -16, 8).glow();
    /** The black iron chain with a cuff at its top, 3 blocks; held at its lower end. */
    public static final ModelFx IRON_CHAIN = ModelFx.of("abomination/iron_chain").pivot(8, -16, 8);
    /** Chains wound round a standing player, a padlock on the chest; worn at the feet. */
    public static final ModelFx RESTRAINT = ModelFx.of("abomination/restraint").seeThrough();
    /** An eight-sided cuff with glowing runes, 1.2 blocks across; its pivot is its middle. */
    public static final ModelFx SHACKLE = ModelFx.of("abomination/shackle").pivot(8, 8, 8).glow();
    /** The Werewolf, 2 blocks tall at scale 1; worn at the feet. Face = front. */
    public static final ModelFx WEREWOLF = ModelFx.of("abomination/werewolf");
    /** A rotting hand clawing out of the ground, three steps (0 = straight, 2 = hooked); its pivot is the forearm's end. */
    public static final ModelFx GRAVE_HAND = ModelFx.of("abomination/grave_hand").frames(3).smooth().pivot(8, -16, 8);
    /** The coffin, 2.4 blocks tall, four steps (0 = lid wide open, 3 = shut); its pivot is the middle of its foot. */
    public static final ModelFx COFFIN = ModelFx.of("abomination/coffin").frames(4).smooth().pivot(8, -16, 8);
    /** A body wrapped in yellow bandages, sized to a player; worn at the feet. */
    public static final ModelFx BANDAGE_WRAP = ModelFx.of("abomination/bandage_wrap");
    /** The vessel: a rag doll bound by thorny vines, 2.75 blocks tall; its pivot is between its feet. */
    public static final ModelFx DOLL_VESSEL = ModelFx.of("abomination/doll_vessel").pivot(8, -16, 8);
    /** Black thorny vines bursting out of the ground, 1.9 blocks; their pivot is the middle of their roots. */
    public static final ModelFx THORN_SPIKES = ModelFx.of("abomination/thorn_spikes").pivot(8, -16, 8);
    /** A red rose on a thorny stem, 0.8 blocks; its pivot is where it meets the ground. */
    public static final ModelFx ROSE_MODEL = ModelFx.of("abomination/rose").glow();
    /** A see-through ghost, 2 blocks, two steps (0 = arms hanging, 1 = reaching); its pivot is the end of its tail. */
    public static final ModelFx GHOST_MODEL = ModelFx.of("abomination/ghost").frames(2).smooth().glow().seeThrough();
    /** The two halves of the Abomination seal, 1 block wide, white: tint them and turn them against each other. */
    public static final ModelFx SEAL_RING = ModelFx.of("abomination/seal_ring").glow().unshaded().seeThrough();
    public static final ModelFx SEAL_CORE = ModelFx.of("abomination/seal_core").glow().unshaded().seeThrough();
    /** Sewn lips in a ring, 1 block, flat, white. */
    public static final ModelFx SILENCE_SIGIL = ModelFx.of("abomination/silence_sigil").glow().unshaded().seeThrough();
    /** A coffin with a skull and an hourglass, 1 block, standing, white. */
    public static final ModelFx SENTENCE_MARK = ModelFx.of("abomination/sentence_mark").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** An eye in a ring of thorns, 1 block, standing, white. */
    public static final ModelFx CURSE_MARK = ModelFx.of("abomination/curse_mark").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** A jagged ring of grave-frost, 1 block, flat, white. */
    public static final ModelFx FROST_RING = ModelFx.of("abomination/frost_ring").glow().unshaded().seeThrough();
    /** A pool of blood, 1 block, flat, its own colors. */
    public static final ModelFx BLOOD_POOL = ModelFx.of("abomination/blood_pool").seeThrough();
    /** Three gashes, 1 block, standing, white. */
    public static final ModelFx CLAW_MARKS = ModelFx.of("abomination/claw_marks").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** The mad moon, 1 block, standing, its own colors. */
    public static final ModelFx MAD_MOON = ModelFx.of("abomination/mad_moon").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** An oval of reflection with ripples, 1 block, standing, white; its pivot is the middle of its bottom edge. */
    public static final ModelFx MIRROR_RIPPLE = ModelFx.of("abomination/mirror_ripple").glow().unshaded().seeThrough();
    /** One tile of thorns over bandages, 1 block, standing, its own colors; its pivot is the middle of its bottom edge. */
    public static final ModelFx THORN_WALL = ModelFx.of("abomination/thorn_wall").seeThrough();

    /** A seal on the ground: the two halves of the Abomination seal turning against each other, 'radius' wide. */
    public static void seal(ServerLevel sl, Vec3 ground, double radius, int color, int ticks, String tag) {
        float width = (float) (radius * 2.0);
        ModelFx ring = SEAL_RING.color(color).scale(width).lifetime(ticks).fade(0, 12).spin(0.6f)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(10, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ModelFx core = SEAL_CORE.color(color).scale(width * 0.55f).lifetime(ticks).fade(0, 12).spin(-1.1f)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(14, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        if (tag != null) {
            ring = ring.tag(tag);
            core = core.tag(tag);
        }
        float yaw = random() * 360f;
        ParticleShapes.model(sl, ring, ground.add(0, 0.05, 0), yaw, 0f, 0f);
        ParticleShapes.model(sl, core, ground.add(0, 0.07, 0), yaw, 0f, 0f);
    }

    /** Thorns bursting out of the ground at a spot and sinking back after 'ticks'. */
    private static void thornsAt(ServerLevel sl, Vec3 ground, float size, int delay, int ticks, String tag) {
        ModelFx thorns = THORN_SPIKES.scale(size).delay(delay).lifetime(ticks).fade(0, 8)
                .key(0, pose().up(-2.2f * size))
                .key(5, pose().up(0f), Ease.OUT_BACK)
                .key(Math.max(6, ticks - 6), pose().up(0f))
                .key(ticks, pose().up(-2.2f * size), Ease.IN);
        if (tag != null) thorns = thorns.tag(tag);
        ParticleShapes.model(sl, thorns, ground, random() * 360f, 0f, 0f);
    }

    // =====================================================================================
    // Key 1: Blood Chain (Prisoner) / Restraint
    // =====================================================================================

    /** Ticks until the chain of blood reaches the one it was thrown at (bind and drag from then). */
    public static final int CHAIN_REACHES = 4;

    /** A chain of blood lashes out of the hand to the target and wraps round it. */
    public static void bloodChain(ServerLevel sl, Player player, LivingEntity target) {
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand == null) hand = chestOf(player);
        Vec3 to = chestOf(target);
        Vec3 dir = to.subtract(hand);
        float length = (float) Math.max(0.5, dir.length());
        ModelFx chain = BLOOD_CHAIN.scale(1f, length / 3f, 1f).lifetime(CHAIN_REACHES + 6).fade(0, 5)
                .key(0, pose().scale(0.05f))
                .key(CHAIN_REACHES, pose().scale(1f), Ease.OUT);
        ParticleShapes.modelAlong(sl, chain, hand, dir);
        ParticleShapes.burst(sl, BLOOD_DROP, hand, 6, 0.04, 0.12);
        Later.run(sl, CHAIN_REACHES, () -> {
            if (!target.isAlive()) return;
            ParticleShapes.burst(sl, BLOOD_LINK, chestOf(target), 10, 0.05, 0.15);
            ParticleShapes.burst(sl, BLOOD_DROP, chestOf(target), 8, 0.05, 0.15);
            SpellFx.shockRing(sl, target.position(), 1.4, 0xC0B01020, 6, 0);
        });
        sound(sl, player.position(), ModSounds.ABOMINATION_CHAIN_WHIP, 1.0f, wobble(1.0f));
    }

    /** While it holds (and drags): a chain of blood between hand and target, a cuff of blood round the target. */
    public static void bloodChainHold(ServerLevel sl, Player player, LivingEntity target, int ticks, String tag) {
        SpellFx.tether(sl, BLOOD_TETHER, () -> {
            if (!player.isAlive() || player.level() != sl) return null;
            Vec3 hand = SpellFx.handOf(player, sl);
            return hand != null ? hand : chestOf(player);
        }, () -> SpellFx.middleOf(target, sl), ticks);
        ModelFx cuff = SHACKLE.color(0xFFC02030).scale(Math.max(0.6f, target.getBbWidth() / 0.9f)).lifetime(ticks)
                .fade(3, 6).tag(tag)
                .key(0, pose().scale(1.6f).alpha(0f))
                .key(5, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, cuff, target, new Vec3(0, target.getBbHeight() * 0.55, 0), 0f, 0f, 0f);
        sound(sl, target.position(), ModSounds.ABOMINATION_CHAIN_BIND, 0.9f, wobble(1.0f));
    }

    /** The chain lets go: its links break into drops of blood. */
    public static void bloodChainEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, BLOOD_LINK, chestOf(target), 8, 0.04, 0.12);
        ParticleShapes.burst(sl, BLOOD_DROP, chestOf(target), 6, 0.03, 0.1);
    }

    /** Once a second while something bleeds (status/Bleeding, through its listener in Abomination): drops fall, now and then a pool. */
    public static void bleed(ServerLevel sl, LivingEntity target) {
        ParticleShapes.cone(sl, BLOOD_DROP, chestOf(target), new Vec3(0, -1, 0), 120, 5, 0.02, 0.08);
        ParticleShapes.burst(sl, BLOOD_MIST, chestOf(target), 1, 0.01, 0.02);
        if (random() < 0.35f) {
            ModelFx pool = BLOOD_POOL.scale(0.4f + random() * 0.4f).lifetime(80).fade(4, 30)
                    .key(0, pose().scale(0.2f)).key(10, pose().scale(1f), Ease.OUT);
            ParticleShapes.model(sl, pool, target.position().add(0, 0.03, 0), random() * 360f, 0f, 0f);
        }
        sound(sl, target.position(), ModSounds.ABOMINATION_BLEED, 0.4f, wobble(1.0f));
    }

    // ---------- sneak: Restraint

    /** Ticks until the chains have wound round and the padlock snaps shut. */
    public static final int RESTRAINT_LOCKED = 22;

    /** The Prisoner binds himself: chains wind in from all round, tighten, and a padlock locks on the chest. */
    public static void restraint(ServerLevel sl, Player player, int ticks, String tag) {
        float size = player.getBbHeight() / 1.8f;
        ModelFx chains = RESTRAINT.scale(size).lifetime(ticks).fade(2, 8).tag(tag)
                .key(0, pose().scale(1.8f).alpha(0f).spin(-90f))
                .key(RESTRAINT_LOCKED, pose().scale(1f).alpha(1f).spin(0f), Ease.OUT);
        ParticleShapes.modelOnTurning(sl, chains, player, Vec3.ZERO, 0f, 0f, 0f);
        SpellFx.circleUnder(sl, player, 0.9, 0xA05A5068, ticks, tag);
        ParticleShapes.spiral(sl, IRON_LINK, player.position().add(0, 0.2, 0), 3, 24, 0.4, 1.6, 1.2, -0.08);
        Later.run(sl, RESTRAINT_LOCKED, () -> {
            if (!player.isAlive()) return;
            ParticleShapes.spawn(sl, PADLOCK, chestOf(player).add(Spells.flatLook(player).scale(0.4)), new Vec3(0, 0.03, 0));
            SpellFx.shockRing(sl, player.position(), 1.6, 0xC08A7AA0, 8, 0);
        });
        sound(sl, player.position(), ModSounds.ABOMINATION_RESTRAINT, 1.0f, 1.0f);
    }

    /** A blow lands on the chains instead of the body: they rattle and spark. */
    public static void restraintHit(ServerLevel sl, Player player) {
        ParticleShapes.burst(sl, IRON_LINK, chestOf(player), 4, 0.04, 0.1);
        ParticleShapes.burst(sl, ParticleFx.of("fx/spark").color(0xFFE0D0FF).endColor(0x006C3082).size(0.05f)
                .endSize(0.02f).lifetime(8, 4).gravity(0.04f).friction(0.88f).glow(), chestOf(player), 6, 0.08, 0.2);
        sound(sl, player.position(), ModSounds.ABOMINATION_CHAIN_BIND, 0.5f, wobble(1.5f));
    }

    /** The chains come off and fall in coils. */
    public static void restraintEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        for (double y = 0.2; y < player.getBbHeight(); y += 0.5) {
            ParticleShapes.ring(sl, IRON_LINK, player.position().add(0, y, 0), 0.45, 6, 0.04);
        }
        sound(sl, player.position(), ModSounds.ABOMINATION_UNCHAIN, 0.9f, 1.0f);
    }

    // =====================================================================================
    // Key 2: Moonstruck (Lunatic) / Lunar Frenzy
    // =====================================================================================

    /** Ticks until the false moon stands over the target (the madness takes it then). */
    public static final int MOON_SHOWN = 10;

    /** A moon that is not there rises over the target's head and stares; pale crescents circle it. */
    public static void moonstruck(ServerLevel sl, Player player, LivingEntity target, int ticks, String tag) {
        ModelFx moon = MAD_MOON.scale(0.9f).lifetime(ticks).fade(4, 12).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f).up(-0.6f))
                .key(MOON_SHOWN, pose().scale(1f).alpha(1f).up(0f), Ease.OUT_BACK)
                .during(MOON_SHOWN, ticks, pose().up(0.15f), Ease.IN_OUT);
        ParticleShapes.modelOn(sl, moon, target, new Vec3(0, target.getBbHeight() + 0.9, 0), yawToward(target.position(), player.position()), 0f, 0f);
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth() * 0.9), 0xA0E8E4D6, ticks, tag);
        ParticleShapes.slashBetween(sl, STRING.color(0x80E8E4D6).lifetime(10), player.getEyePosition(), headOf(target));
        ParticleShapes.ring(sl, CRESCENT, headOf(target).add(0, 0.3, 0), 0.8, 8, 0.02);
        sound(sl, target.position(), ModSounds.ABOMINATION_MOONSTRUCK, 1.0f, 1.0f);
    }

    /** Once a second on the maddened: pink sparks spin round the head; laugh = it giggles (now and then). */
    public static void madnessTick(ServerLevel sl, LivingEntity target, boolean laugh) {
        ParticleShapes.ring(sl, MAD_SPARK, headOf(target).add(0, 0.2, 0), 0.5, 5, 0.03);
        ParticleShapes.burst(sl, CRESCENT, headOf(target), 1, 0.01, 0.03);
        if (laugh) sound(sl, target.position(), ModSounds.ABOMINATION_MADNESS, 0.6f, wobble(target.getBbHeight() > 2.5 ? 0.7f : 1.0f));
    }

    public static void moonstruckEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, MOON_MOTE, headOf(target), 8, 0.02, 0.08);
    }

    // ---------- sneak: Lunar Frenzy

    /** The Lunatic gives in to the moon: a huge mad moon flares in the sky above, red light runs over the body. */
    public static void lunarFrenzy(ServerLevel sl, Player player, int ticks, String tag) {
        Vec3 sky = player.position().add(Spells.flatLook(player).scale(6)).add(0, 9, 0);
        ModelFx moon = MAD_MOON.scale(7f).lifetime(50).fade(10, 25)
                .key(0, pose().scale(0.5f).alpha(0f))
                .key(14, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, moon, sky, player.getYRot() + 180f, 0f, 0f);
        SpellFx.circleUnder(sl, player, 1.1, 0xC0FF5050, ticks, tag);
        SpellFx.risingRings(sl, player.position(), 1.0, 2.2, 4, 0xC0FF7070);
        ParticleShapes.burst(sl, CRESCENT, chestOf(player), 12, 0.06, 0.18);
        ParticleShapes.burst(sl, FRENZY_MOTE, chestOf(player), 16, 0.04, 0.14);
        sound(sl, player.position(), ModSounds.ABOMINATION_FRENZY, 1.0f, 1.0f);
    }

    /** Every few ticks of the frenzy: red motes and hot breath. */
    public static void frenzyTick(ServerLevel sl, Player player) {
        ParticleShapes.burst(sl, FRENZY_MOTE, chestOf(player), 2, 0.02, 0.06);
        ParticleShapes.spawn(sl, BREATH, player.getEyePosition().add(Spells.flatLook(player).scale(0.4)),
                Spells.flatLook(player).scale(0.05));
    }

    /** The crash after: grey smoke sinks off the body, a dull ring under the feet. */
    public static void frenzyCrash(ServerLevel sl, Player player, int ticks, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        SpellFx.circleUnder(sl, player, 0.9, 0x80908880, ticks, tag);
        for (double y = 0.3; y < player.getBbHeight(); y += 0.5) {
            ParticleShapes.ring(sl, GREY_SMOKE, player.position().add(0, y, 0), 0.4, 5, 0.02);
        }
        sound(sl, player.position(), ModSounds.ABOMINATION_FRENZY_CRASH, 0.9f, 1.0f);
    }

    public static void frenzyEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
    }

    // =====================================================================================
    // Key 3: Rending Pounce (Werewolf) / Werewolf Form
    // =====================================================================================

    /** The leap: earth kicks up behind, a growl. */
    public static void pounceLeap(ServerLevel sl, Player player) {
        Vec3 back = player.position().subtract(Spells.flatLook(player).scale(0.5));
        ParticleShapes.cone(sl, DUST, back.add(0, 0.2, 0), Spells.flatLook(player).scale(-1).add(0, 0.4, 0), 70, 12, 0.05, 0.2);
        ParticleShapes.burst(sl, EARTH, player.position().add(0, 0.1, 0), 8, 0.08, 0.2);
        SpellFx.shockRing(sl, player.position(), 1.6, 0xA0706050, 8, 0);
        sound(sl, player.position(), ModSounds.ABOMINATION_POUNCE, 1.0f, wobble(1.0f));
    }

    /** Every tick of the leap: grey-green streaks of speed behind the body. */
    public static void pounceTrail(ServerLevel sl, Player player) {
        Vec3 look = player.getDeltaMovement().lengthSqr() > 0.01 ? player.getDeltaMovement().normalize() : Spells.flatLook(player);
        Vec3 chest = chestOf(player);
        ParticleShapes.slashBetween(sl, LEAP_LINE, chest.subtract(look.scale(1.6)), chest);
        if (random() < 0.4f) ParticleShapes.spawn(sl, WOLF_HAIR, chest, look.scale(-0.05));
    }

    /** One of the three claws (index 0..2, the last throws it down): torn air, claw marks, blood. */
    public static void pounceClaw(ServerLevel sl, Player player, LivingEntity target, int index) {
        boolean last = index >= 2;
        Vec3 at = chestOf(target);
        float yaw = player.getYRot();
        float roll = (index % 2 == 0 ? -1 : 1) * (35f + index * 15f);
        ParticleShapes.slash(sl, last ? CLAW_ARC.radius(1.6f).width(0.26f) : CLAW_ARC,
                chestOf(player).add(Spells.flatLook(player).scale(0.5)), yaw, 0f, roll);
        clawMarks(sl, at, yaw + 180f, roll, 0xFFE02838, last ? 1.3f : 0.9f);
        ParticleShapes.burst(sl, BLOOD_DROP, at, last ? 14 : 6, 0.06, last ? 0.25 : 0.15);
        if (last) {
            SpellFx.shockRing(sl, target.position(), 2.2, 0xC0B01020, 8, 0);
            ModelFx pool = BLOOD_POOL.scale(1.2f).lifetime(100).fade(2, 30).key(0, pose().scale(0.2f))
                    .key(6, pose().scale(1f), Ease.OUT);
            ParticleShapes.model(sl, pool, target.position().add(0, 0.03, 0), random() * 360f, 0f, 0f);
        }
        sound(sl, at, ModSounds.ABOMINATION_CLAW, 1.0f, wobble(last ? 0.8f : 1.0f));
    }

    /** Three gashes hanging in the air for a moment. */
    private static void clawMarks(ServerLevel sl, Vec3 at, float yaw, float roll, int color, float size) {
        ModelFx marks = CLAW_MARKS.color(color).scale(size).lifetime(10).fade(0, 6)
                .key(0, pose().scale(0.4f).roll(roll * 0.3f))
                .key(3, pose().scale(1f).roll(roll * 0.3f), Ease.OUT);
        ParticleShapes.model(sl, marks, at, yaw, 0f, 0f);
    }

    // ---------- sneak: Werewolf Form

    /** Ticks until the Werewolf's body has fully grown round the player (the howl comes then). */
    public static final int WOLF_RISEN = 26;

    /** Bones crack and fur bursts out: the Werewolf's body grows round the player, then a howl. 'height' = the
     *  player's height in the form (1.8 * 1.1 at Sequence 7). */
    public static void werewolfForm(ServerLevel sl, Player player, double height, int ticks, String tag) {
        float full = (float) (height / 2.0);
        ModelFx body = WEREWOLF.scale(full).lifetime(ticks).fade(4, 10).tag(tag)
                .key(0, pose().scale(0.8f).alpha(0f))
                .key(WOLF_RISEN, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOnTurning(sl, body, player, Vec3.ZERO, 0f, 0f, 0f);
        for (int t = 0; t < WOLF_RISEN; t += 5) {
            Later.run(sl, t, () -> {
                if (!player.isAlive()) return;
                ParticleShapes.burst(sl, WOLF_HAIR, chestOf(player), 6, 0.04, 0.14);
                ParticleShapes.burst(sl, DUST, player.position().add(0, 0.2, 0), 2, 0.03, 0.08);
            });
        }
        SpellFx.circleUnder(sl, player, 1.0, 0x904C6050, ticks, tag);
        Later.run(sl, WOLF_RISEN, () -> {
            if (!player.isAlive()) return;
            howl(sl, player);
        });
        sound(sl, player.position(), ModSounds.ABOMINATION_WEREWOLF_FORM, 1.2f, 1.0f);
    }

    /** A claw blow in the form: a dark-green rend with blood behind it. */
    public static void werewolfClaw(ServerLevel sl, Player player, LivingEntity target) {
        Vec3 at = chestOf(target);
        float roll = random() < 0.5f ? -40f : 40f;
        ParticleShapes.slash(sl, CLAW_ARC.color(0xE04C6050).core(0xFFE8FF90), chestOf(player).add(Spells.flatLook(player).scale(0.5)),
                player.getYRot(), 0f, roll);
        clawMarks(sl, at, player.getYRot() + 180f, roll, 0xFFB01020, 0.9f);
        ParticleShapes.burst(sl, BLOOD_DROP, at, 6, 0.05, 0.15);
        sound(sl, at, ModSounds.ABOMINATION_CLAW, 0.9f, wobble(0.9f));
    }

    /** The madness rising in the form (0..1): eye glints and red motes, more the madder. */
    public static void werewolfMadness(ServerLevel sl, Player player, float madness, double height) {
        int count = Math.round(1 + madness * 8);
        Vec3 eyes = player.position().add(0, height * 0.85, 0).add(Spells.flatLook(player).scale(0.4));
        ParticleShapes.burst(sl, WOLF_GLINT, eyes, 2, 0.0, 0.01);
        ParticleShapes.burst(sl, FRENZY_MOTE, player.position().add(0, height * 0.8, 0), count, 0.02, 0.05 + madness * 0.1);
        if (madness > 0.75f && random() < 0.3f) sound(sl, player.position(), ModSounds.ABOMINATION_MADNESS, 0.5f, 0.7f);
    }

    /** The howl: breath in the air, a ring of sound rolling out. */
    public static void howl(ServerLevel sl, Player player) {
        Vec3 mouth = player.getEyePosition().add(Spells.flatLook(player).scale(0.5)).add(0, 0.2, 0);
        ParticleShapes.cone(sl, BREATH, mouth, Spells.flatLook(player).add(0, 0.8, 0), 40, 8, 0.04, 0.1);
        for (int i = 0; i < 3; i++) SpellFx.shockRing(sl, player.position(), 4 + i * 3, 0x80C0D0C0, 16, i * 6);
        sound(sl, player.position(), ModSounds.ABOMINATION_HOWL, 1.6f, wobble(1.0f));
    }

    /** The form falls away: fur and dust. */
    public static void werewolfFormEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        for (double y = 0.2; y < player.getBbHeight(); y += 0.5) {
            ParticleShapes.ring(sl, WOLF_HAIR, player.position().add(0, y, 0), 0.4, 6, 0.05);
        }
        sound(sl, player.position(), ModSounds.ABOMINATION_FRENZY_CRASH, 0.6f, 1.2f);
    }

    // =====================================================================================
    // Key 4: the Zombie wheel (Zombie)
    // =====================================================================================

    /** Ticks after zombieRises until the hand is up: spawn the zombie then. */
    public static final int HANDS_UP = 14;

    /** Raise the Dead: a sickly seal opens on the grave ground and the earth heaves. */
    public static void raiseTheDead(ServerLevel sl, Vec3 ground, double radius) {
        seal(sl, ground, radius, 0xC07A9060, 60, null);
        SpellFx.shockRing(sl, ground, radius * 1.2, 0xA05A4028, 14, 0);
        ParticleShapes.ring(sl, EARTH, ground.add(0, 0.2, 0), radius * 0.6, 16, 0.08);
        ParticleShapes.burst(sl, GRAVE_WISP, ground.add(0, 0.5, 0), 8, 0.02, 0.06);
        sound(sl, ground, ModSounds.ABOMINATION_RAISE_DEAD, 1.2f, 1.0f);
    }

    /** One of the dead claws out of the ground at 'at': a hand bursts up and grasps, dirt flies. */
    public static void zombieRises(ServerLevel sl, Vec3 at) {
        ModelFx hand = GRAVE_HAND.scale(0.9f).lifetime(HANDS_UP + 10).fade(0, 8)
                .key(0, pose().up(-1.8f).frame(0f))
                .key(HANDS_UP - 4, pose().up(0f).frame(1f), Ease.OUT)
                .key(HANDS_UP, pose().frame(2f));
        ParticleShapes.model(sl, hand, at, random() * 360f, 0f, 0f);
        for (int t = 0; t < HANDS_UP; t += 4) {
            Later.run(sl, t, () -> ParticleShapes.burst(sl, EARTH, at.add(0, 0.1, 0), 4, 0.06, 0.16));
        }
        Later.run(sl, HANDS_UP, () -> {
            ParticleShapes.burst(sl, DUST, at.add(0, 0.3, 0), 6, 0.03, 0.1);
            sound(sl, at, ModSounds.ABOMINATION_GROAN, 0.9f, wobble(1.0f));
        });
    }

    /** One killed near the spot rises as it was: a purple skull and grave-light on the corpse's spot. */
    public static void fallenRises(ServerLevel sl, Vec3 at) {
        ParticleShapes.spawn(sl, SKULL, at.add(0, 1.0, 0), new Vec3(0, 0.04, 0));
        SpellFx.risingRings(sl, at, 0.8, 2.0, 3, 0xA07A9060);
        ParticleShapes.burst(sl, GRAVE_WISP, at.add(0, 0.5, 0), 10, 0.02, 0.08);
        zombieRises(sl, at);
    }

    /** A faint ring under one of the risen while it serves. */
    public static void servantMark(ServerLevel sl, LivingEntity servant, int ticks, String tag) {
        SpellFx.circleUnder(sl, servant, Math.max(0.5, servant.getBbWidth() * 0.7), 0x807A9060, ticks, tag);
    }

    /** Ticks until the grave-frost has spread to its full radius. */
    public static final int CHILL_SPREAD = 14;

    /** Grave Chill: a jagged ring of frost spreads round the Zombie and stays with them; cold mist rolls out. */
    public static void graveChill(ServerLevel sl, Player player, double radius, int ticks, String tag) {
        float width = (float) (radius * 2.0);
        ModelFx frost = FROST_RING.color(0xE096D2FF).scale(width).lifetime(ticks).fade(2, 14).spin(0.3f).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(CHILL_SPREAD, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, frost, player, new Vec3(0, 0.05, 0), random() * 360f, 0f, 0f);
        SpellFx.steadyRing(sl, player.position(), radius, 0xC096D2FF, CHILL_SPREAD, 0);
        ParticleShapes.ring(sl, FROST_MIST, player.position().add(0, 0.3, 0), 1.0, 16, 0.25);
        sound(sl, player.position(), ModSounds.ABOMINATION_GRAVE_CHILL, 1.0f, 1.0f);
    }

    /** Every few ticks: mist low over the ground and flakes drifting. */
    public static void graveChillTick(ServerLevel sl, Player player, double radius) {
        for (int i = 0; i < 4; i++) {
            double a = random() * Math.PI * 2, r = Math.sqrt(random()) * radius;
            Vec3 at = player.position().add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
            ParticleShapes.spawn(sl, FROST_MIST, at, new Vec3(0, 0.005, 0));
            ParticleShapes.spawn(sl, FROST, at.add(0, 1.0 + random(), 0), new Vec3(0, -0.02, 0));
        }
    }

    /** On an enemy in the chill: frost on its body, more the colder it is (0..1). */
    public static void chilled(ServerLevel sl, LivingEntity enemy, float cold) {
        ParticleShapes.burst(sl, FROST, chestOf(enemy), Math.round(1 + cold * 5), 0.02, 0.06);
    }

    /** The chill locks it in ice for a moment. */
    public static void chillFreeze(ServerLevel sl, LivingEntity enemy) {
        ParticleShapes.burst(sl, FROST, chestOf(enemy), 14, 0.06, 0.18);
        SpellFx.shockRing(sl, enemy.position(), 1.4, 0xE0C8ECFF, 6, 0);
        sound(sl, enemy.position(), ModSounds.ABOMINATION_FREEZE, 0.9f, wobble(1.0f));
    }

    public static void graveChillEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.ring(sl, FROST_MIST, player.position().add(0, 0.3, 0), 2.0, 12, 0.06);
    }

    /** Undying Flesh: the heart stops and the body goes on; a slow dark-green beat under the feet. */
    public static void undyingFlesh(ServerLevel sl, Player player, int ticks, String tag) {
        SpellFx.circleUnder(sl, player, 1.0, 0xA07A9060, ticks, tag);
        SpellFx.shockRing(sl, player.position(), 2.4, 0xC05A7048, 12, 0);
        ParticleShapes.burst(sl, ROT_MOTE, chestOf(player), 12, 0.03, 0.1);
        for (int t = 20; t < Math.min(ticks, 400); t += 40) {
            Later.run(sl, t, () -> {
                if (!player.isAlive()) return;
                SpellFx.shockRing(sl, player.position(), 1.4, 0x605A7048, 10, 0);
            });
        }
        sound(sl, player.position(), ModSounds.ABOMINATION_UNDYING, 1.0f, 1.0f);
    }

    /** Once a second: the flesh knits (green motes). */
    public static void undyingTick(ServerLevel sl, Player player) {
        ParticleShapes.burst(sl, ROT_MOTE, chestOf(player), 2, 0.01, 0.04);
    }

    /** A blow that would hurt does not: grey dust puffs off dead skin, no blood. */
    public static void undyingHit(ServerLevel sl, Player player) {
        ParticleShapes.burst(sl, GREY_SMOKE, chestOf(player), 3, 0.02, 0.06);
    }

    public static void undyingEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
    }

    /** Ticks from sentenceFalls until the blow lands (deal the damage then). */
    public static final int SENTENCE_LANDS = 6;

    /** Death Sentence: a knell, a purple coffin-mark over the sentenced head and a ring at its feet. */
    public static void deathSentence(ServerLevel sl, Player player, LivingEntity target, int ticks, String tag) {
        ModelFx mark = SENTENCE_MARK.color(0xE0B070FF).scale(0.8f).lifetime(ticks).fade(4, 6).tag(tag)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(8, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.8, 0), yawToward(target.position(), player.position()), 0f, 0f);
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth() * 0.9), 0xC06C3082, ticks, tag);
        ParticleShapes.slashBetween(sl, CURSE_LINE.lifetime(12), player.getEyePosition(), chestOf(target));
        ParticleShapes.burst(sl, SKULL, headOf(target), 2, 0.02, 0.05);
        sound(sl, target.position(), ModSounds.ABOMINATION_SENTENCE, 1.0f, 1.0f);
    }

    /** Each second of the count: the mark pulses, the knock gets higher as the end comes. */
    public static void sentenceTick(ServerLevel sl, LivingEntity target, int secondsLeft) {
        SpellFx.shockRing(sl, target.position(), 1.6, 0xA0B070FF, 8, 0);
        ParticleShapes.burst(sl, PURPLE_MOTE, headOf(target).add(0, 0.8, 0), 4, 0.02, 0.05);
        sound(sl, target.position(), ModSounds.ABOMINATION_SENTENCE_TICK, 0.8f, 1.0f + Math.max(0, 5 - secondsLeft) * 0.1f);
    }

    /** The count reaches 0: the mark drops onto the head, a column of purple, skulls fly. */
    public static void sentenceFalls(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        Vec3 top = target.position().add(0, target.getBbHeight() + 0.8, 0);
        ModelFx drop = SENTENCE_MARK.color(0xFFB070FF).scale(1.1f).lifetime(SENTENCE_LANDS + 6).fade(0, 5)
                .key(0, pose().up(0f))
                .key(SENTENCE_LANDS, pose().up(-(target.getBbHeight() * 0.5f + 0.8f)).scale(1.4f), Ease.IN);
        ParticleShapes.model(sl, drop, top, random() * 360f, 0f, 0f);
        Later.run(sl, SENTENCE_LANDS, () -> {
            Vec3 at = target.isAlive() ? target.position() : top.add(0, -target.getBbHeight() - 0.8, 0);
            SpellFx.risingRings(sl, at, 1.0, 3.0, 4, 0xE0B070FF);
            SpellFx.shockRing(sl, at, 3.0, 0xE06C3082, 10, 0);
            ParticleShapes.burst(sl, SKULL, at.add(0, 1.0, 0), 6, 0.06, 0.16);
            ParticleShapes.burst(sl, PURPLE_MOTE, at.add(0, 1.0, 0), 16, 0.06, 0.2);
        });
        sound(sl, target.position(), ModSounds.ABOMINATION_SENTENCE_FALL, 1.2f, 1.0f);
    }

    /** The sentence is lifted (out of range, sentenced someone else): the mark fades. */
    public static void sentenceLifted(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, PURPLE_MOTE, headOf(target).add(0, 0.8, 0), 6, 0.01, 0.04);
    }

    // =====================================================================================
    // Key 5: the Wraith wheel (Wraith)
    // =====================================================================================

    /** Ticks after mirrorEnter until the Wraith is gone into the reflection (hide and move them then). */
    public static final int MIRROR_OPENS = 6;

    /** A ripple opens on the reflection and the Wraith is drawn into it, glass glittering. */
    public static void mirrorEnter(ServerLevel sl, Player player, Vec3 mirror) {
        mirrorDoor(sl, mirror, yawToward(mirror, player.position()), MIRROR_OPENS + 10);
        ParticleShapes.slashBetween(sl, GHOST_LINE.lifetime(MIRROR_OPENS + 2), chestOf(player), mirror.add(0, 1.0, 0));
        ParticleShapes.burst(sl, GLASS, chestOf(player), 12, 0.04, 0.12);
        sound(sl, player.position(), ModSounds.ABOMINATION_MIRROR_ENTER, 1.0f, 1.0f);
    }

    /** Out of the other reflection: a ripple there, and the Wraith steps out of it. */
    public static void mirrorExit(ServerLevel sl, Player player, Vec3 mirror) {
        mirrorDoor(sl, mirror, player.getYRot() + 180f, 16);
        ParticleShapes.burst(sl, GLASS, mirror.add(0, 1.0, 0), 16, 0.06, 0.16);
        sound(sl, mirror, ModSounds.ABOMINATION_MIRROR_EXIT, 1.0f, 1.0f);
    }

    private static void mirrorDoor(ServerLevel sl, Vec3 ground, float yaw, int ticks) {
        ModelFx door = MIRROR_RIPPLE.color(0xE0C0D8F8).scale(1.2f, 2.2f, 1f).lifetime(ticks).fade(0, 6)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(5, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, door, ground.add(0, 0.05, 0), yaw, 0f, 0f);
    }

    /** A glint only this player sees, on a reflection they could step through (call it on spots near them). */
    public static void reflectionHint(ServerPlayer viewer, Vec3 at) {
        ParticleShapes.spawnFor(viewer, GLASS, at, new Vec3(0, 0.01, 0));
    }

    /** Ticks after possessionDive until the ghost has gone into the host (start steering then). */
    public static final int GHOST_ARRIVES = 10;

    /** The Wraith's ghost leaves the body and dives into the host. */
    public static void possessionDive(ServerLevel sl, Player player, LivingEntity host) {
        Vec3 from = player.position();
        Vec3 to = host.position();
        Vec3 dir = to.subtract(from);
        float yaw = yawToward(from, to);
        double dist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        ModelFx ghost = GHOST_MODEL.scale(player.getBbHeight() / 2f).lifetime(GHOST_ARRIVES + 4).fade(2, 4)
                .key(0, pose().frame(1f).alpha(0.9f))
                .key(GHOST_ARRIVES, pose().forward((float) dist).up((float) dir.y).scale(0.5f).alpha(0.5f), Ease.IN);
        ParticleShapes.model(sl, ghost, from, yaw, 0f, 0f);
        ParticleShapes.slashBetween(sl, GHOST_LINE.lifetime(GHOST_ARRIVES), chestOf(player), chestOf(host));
        ParticleShapes.burst(sl, GHOST_WISP, chestOf(player), 8, 0.02, 0.08);
        Later.run(sl, GHOST_ARRIVES, () -> {
            if (!host.isAlive()) return;
            ParticleShapes.burst(sl, GHOST_WISP, chestOf(host), 12, 0.04, 0.12);
            SpellFx.shockRing(sl, host.position(), 1.6, 0xA0C8E0FA, 8, 0);
        });
        sound(sl, player.position(), ModSounds.ABOMINATION_POSSESSION, 1.0f, 1.0f);
    }

    /** While the host is ridden: a faint ghost inside it and a pale ring at its feet. */
    public static void possessed(ServerLevel sl, LivingEntity host, int ticks, String tag) {
        ModelFx inside = GHOST_MODEL.scale(Math.max(0.5f, host.getBbHeight() / 2f)).lifetime(ticks).fade(6, 10).tag(tag)
                .key(0, pose().alpha(0f)).key(8, pose().alpha(0.35f));
        ParticleShapes.modelOnTurning(sl, inside, host, Vec3.ZERO, 0f, 0f, 0f);
        SpellFx.circleUnder(sl, host, Math.max(0.6, host.getBbWidth() * 0.8), 0x80C8E0FA, ticks, tag);
    }

    /** Every second or so: wisps leak out of the host. */
    public static void possessionTick(ServerLevel sl, LivingEntity host) {
        ParticleShapes.burst(sl, GHOST_WISP, headOf(host), 2, 0.01, 0.04);
    }

    /** The body left behind while the ghost is away: grey and still, a cold ring under it. */
    public static void bodyLeft(ServerLevel sl, Player player, int ticks, String tag) {
        SpellFx.circleUnder(sl, player, 0.8, 0x80A0A8B8, ticks, tag);
        ParticleShapes.burst(sl, GREY_SMOKE, chestOf(player), 4, 0.01, 0.03);
    }

    /** The ghost steps out of the host and back into the body. */
    public static void possessionExit(ServerLevel sl, LivingEntity host, Player player, String tag) {
        ParticleShapes.clearModels(sl, host, tag);
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.burst(sl, GHOST_WISP, chestOf(host), 10, 0.03, 0.1);
        ParticleShapes.slashBetween(sl, GHOST_LINE.lifetime(8), chestOf(host), chestOf(player));
        sound(sl, player.position(), ModSounds.ABOMINATION_POSSESSION_EXIT, 1.0f, 1.0f);
    }

    /** Ticks after ghostHost until the ghosts are out of the Spirit World (spawn the vexes then). */
    public static final int GHOSTS_RISEN = 24;

    /** Ghost Host: a pale seal on the ground and 'count' ghosts rising out of it round the Wraith, wailing. */
    public static void ghostHost(ServerLevel sl, Player player, int count) {
        Vec3 ground = player.position();
        seal(sl, ground, 2.5, 0xC0C8E0FA, GHOSTS_RISEN + 20, null);
        int shown = Math.min(count, 6);
        for (int i = 0; i < shown; i++) {
            float out = player.getYRot() + i * 360f / shown;
            Vec3 at = ground.add(ahead(out).scale(1.8));
            ModelFx ghost = GHOST_MODEL.scale(0.8f).delay(i * 3).lifetime(GHOSTS_RISEN + 6).fade(4, 6)
                    .key(0, pose().up(-1.6f).alpha(0f).frame(0f))
                    .key(GHOSTS_RISEN - i * 3, pose().up(0.4f).alpha(0.9f).frame(1f), Ease.OUT);
            ParticleShapes.model(sl, ghost, at, out, 0f, 0f);
        }
        ParticleShapes.burst(sl, GHOST_WISP, ground.add(0, 0.5, 0), 14, 0.03, 0.1);
        sound(sl, ground, ModSounds.ABOMINATION_GHOST_HOST, 1.2f, 1.0f);
    }

    /** One of the ghosts, now a summon: a puff of wisps where it appears. */
    public static void ghostServant(ServerLevel sl, LivingEntity ghost) {
        ParticleShapes.burst(sl, GHOST_WISP, chestOf(ghost), 8, 0.03, 0.1);
        if (random() < 0.5f) sound(sl, ghost.position(), ModSounds.ABOMINATION_GHOST_WAIL, 0.6f, wobble(1.1f));
    }

    /** Another's vex or phantom is taken over: its ring turns pale, wisps pour into it. */
    public static void ghostTakenOver(ServerLevel sl, LivingEntity mob, int ticks, String tag) {
        SpellFx.circleUnder(sl, mob, Math.max(0.5, mob.getBbWidth() * 0.8), 0xA0C8E0FA, ticks, tag);
        ParticleShapes.burst(sl, GHOST_WISP, chestOf(mob), 10, 0.03, 0.1);
        SpellFx.shockRing(sl, mob.position(), 1.4, 0xA0C8E0FA, 8, 0);
        sound(sl, mob.position(), ModSounds.ABOMINATION_GHOST_WAIL, 0.7f, wobble(0.9f));
    }

    /** A ghost goes back to the Spirit World. */
    public static void ghostFades(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, GHOST_WISP, at, 10, 0.02, 0.08);
    }

    // =====================================================================================
    // Key 6: the Chains wheel (Puppet, Chained)
    // =====================================================================================

    /** Ticks after animateObjects until the things have risen (throw them then). */
    public static final int OBJECTS_RISE = 12;

    /** Animate Objects: purple strings run from the Puppet's hand to every loose thing, which glows as it lifts. */
    public static void animateObjects(ServerLevel sl, Player player, List<? extends Entity> things) {
        for (Entity thing : things) {
            SpellFx.tether(sl, STRING, () -> {
                if (!player.isAlive() || player.level() != sl) return null;
                Vec3 hand = SpellFx.handOf(player, sl);
                return hand != null ? hand : chestOf(player);
            }, () -> thing.isAlive() ? thing.position().add(0, thing.getBbHeight() * 0.5, 0) : null, OBJECTS_RISE + 6);
            ParticleShapes.burst(sl, STRING_MOTE, thing.position(), 4, 0.02, 0.06);
            ParticleShapes.ring(sl, DUST, thing.position().add(0, 0.1, 0), 0.4, 5, 0.04);
        }
        SpellFx.risingRings(sl, player.position(), 1.0, 2.2, 3, 0xA0B070FF);
        sound(sl, player.position(), ModSounds.ABOMINATION_ANIMATE, 1.0f, 1.0f);
    }

    /** A thing is thrown. */
    public static void hurlLaunch(ServerLevel sl, Entity thing) {
        ParticleShapes.burst(sl, STRING_MOTE, thing.position(), 4, 0.04, 0.1);
        sound(sl, thing.position(), ModSounds.ABOMINATION_HURL, 0.7f, wobble(1.0f));
    }

    /** Every tick of its flight: a purple glow trailing off it. */
    public static void hurledTrail(ServerLevel sl, Entity thing) {
        ParticleShapes.spawn(sl, STRING_MOTE, thing.position().add(0, thing.getBbHeight() * 0.5, 0), Vec3.ZERO);
    }

    /** It hits. */
    public static void hurledHit(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, PURPLE_MOTE, at, 8, 0.04, 0.12);
        SpellFx.shockRing(sl, at.add(0, -0.5, 0), 1.0, 0x80B070FF, 6, 0);
    }

    /** Ticks after graveShackles until the chains have reached the enemies and locked (hold them from then). */
    public static final int SHACKLES_LOCK = 10;

    /** Grave Shackles: black chains rise out of the ground round each enemy and lock a cuff on it. */
    public static void graveShackles(ServerLevel sl, List<? extends LivingEntity> enemies, int ticks, String tag) {
        Vec3 sound = null;
        for (LivingEntity enemy : enemies) {
            if (sound == null) sound = enemy.position();
            Vec3 middle = chestOf(enemy).subtract(0, enemy.getBbHeight() * 0.1, 0);
            float spin = random() * 360f;
            for (int k = 0; k < 3; k++) {
                Vec3 ground = enemy.position().add(ahead(spin + k * 120f).scale(1.6 + enemy.getBbWidth() * 0.5));
                Vec3 dir = middle.subtract(ground);
                float length = (float) dir.length();
                ModelFx chain = IRON_CHAIN.scale(1f, length / 3f, 1f).lifetime(ticks).fade(0, 8).tag(tag)
                        .key(0, pose().scale(0.05f))
                        .key(SHACKLES_LOCK, pose().scale(1f), Ease.OUT);
                ParticleShapes.modelAlong(sl, chain, ground, dir);
                ParticleShapes.burst(sl, EARTH, ground.add(0, 0.1, 0), 5, 0.05, 0.14);
            }
            ModelFx cuff = SHACKLE.scale(Math.max(0.6f, enemy.getBbWidth() / 0.9f)).lifetime(ticks).fade(2, 8).tag(tag)
                    .key(0, pose().scale(0f))
                    .key(SHACKLES_LOCK, pose().scale(1.4f))
                    .key(SHACKLES_LOCK + 3, pose().scale(1f), Ease.OUT);
            ParticleShapes.modelOn(sl, cuff, enemy, new Vec3(0, enemy.getBbHeight() * 0.5, 0), 0f, 0f, 0f);
            SpellFx.circleUnder(sl, enemy, Math.max(0.6, enemy.getBbWidth()), 0xA05A5068, ticks, tag);
        }
        if (sound != null) sound(sl, sound, ModSounds.ABOMINATION_SHACKLES, 1.2f, 1.0f);
    }

    /** One shackled feels another's pain: a red thread flashes between them. */
    public static void painShared(ServerLevel sl, LivingEntity from, LivingEntity to) {
        ParticleShapes.slashBetween(sl, PAIN_LINE.lifetime(8), chestOf(from), chestOf(to));
        ParticleShapes.burst(sl, BLOOD_DROP, chestOf(to), 4, 0.03, 0.08);
        sound(sl, to.position(), ModSounds.ABOMINATION_PAIN_LINK, 0.6f, wobble(1.0f));
    }

    /** The shackles let go: links fall. */
    public static void graveShacklesEnd(ServerLevel sl, List<? extends LivingEntity> enemies, String tag) {
        for (LivingEntity enemy : enemies) {
            ParticleShapes.clearModels(sl, enemy, tag);
            ParticleShapes.burst(sl, IRON_LINK, chestOf(enemy), 6, 0.04, 0.1);
        }
        if (!enemies.isEmpty()) ParticleShapes.clearModels(sl, enemies.get(0).position(), tag);
    }

    /** Ticks after mausoleum until the shell of thorns has closed (enemies are kept in from then). */
    public static final int MAUSOLEUM_SEALED = 30;

    /** Thorned Mausoleum: thorns burst up round the edge, then a sphere of thorns and bandages closes row by row. */
    public static void mausoleum(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        seal(sl, center, radius, 0xC0D6BE6E, ticks, tag);
        int around = (int) Math.max(6, Math.min(16, radius * 2));
        for (int i = 0; i < around; i++) {
            Vec3 at = center.add(ahead(i * 360f / around).scale(radius));
            thornsAt(sl, at, 1.2f, i % 3, ticks, tag);
        }
        shell(sl, center, radius, ticks, tag, new double[]{-80, -55, -28, 0, 28, 55, 80}, 4);
        ParticleShapes.ring(sl, BANDAGE_P, center.add(0, 1.0, 0), radius * 0.8, 20, 0.03);
        sound(sl, center, ModSounds.ABOMINATION_MAUSOLEUM, 1.5f, 1.0f);
    }

    /** A whole sphere of THORN_WALL tiles round 'center' (its middle), closing from the equator to both poles.
     *  One model per tile: about 80 for a sphere 12 blocks across. */
    private static void shell(ServerLevel sl, Vec3 center, double radius, int ticks, String tag, double[] edges, int rowDelay) {
        float spin = random() * 360f;
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
                ModelFx tile = THORN_WALL.scale(width, height, 1f).delay(delay).lifetime(ticks)
                        .fade(4, 10).key(0, pose().alpha(0f)).key(6, pose().alpha(1f), Ease.OUT);
                if (tag != null) tile = tile.tag(tag);
                ParticleShapes.model(sl, tile, at, azimuth + 180f, (float) Math.toDegrees(mid), 0f);
            }
        }
        double cap = radius * Math.cos(Math.toRadians(80)) * 2.2;
        double up = radius * Math.sin(Math.toRadians(80));
        for (int s = -1; s <= 1; s += 2) {
            ModelFx top = SEAL_CORE.color(0xFFD6BE6E).scale((float) cap).delay(MAUSOLEUM_SEALED - 6).lifetime(ticks).fade(4, 10).spin(1f);
            if (tag != null) top = top.tag(tag);
            ParticleShapes.model(sl, top, center.add(0, s * up, 0), 0f, 0f, 0f);
        }
    }

    /** Every second or so inside: thorns and scraps of bandage drift down. */
    public static void mausoleumTick(ServerLevel sl, Vec3 center, double radius) {
        for (int i = 0; i < 3; i++) {
            double a = random() * Math.PI * 2, r = Math.sqrt(random()) * radius * 0.8;
            Vec3 at = center.add(Math.cos(a) * r, radius * 0.6, Math.sin(a) * r);
            ParticleShapes.spawn(sl, i == 0 ? BANDAGE_P : THORN_P, at, new Vec3(0, -0.02, 0));
        }
    }

    /** An enemy inside is pricked by the thorns. */
    public static void mausoleumThorn(ServerLevel sl, LivingEntity enemy) {
        ParticleShapes.burst(sl, THORN_P, chestOf(enemy), 4, 0.04, 0.1);
        ParticleShapes.burst(sl, BLOOD_DROP, chestOf(enemy), 3, 0.03, 0.08);
        sound(sl, enemy.position(), ModSounds.ABOMINATION_THORN_HIT, 0.6f, wobble(1.0f));
    }

    /** The mausoleum crumbles: thorns and bandages fall. */
    public static void mausoleumEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        for (int i = 0; i < 4; i++) {
            ParticleShapes.ring(sl, i % 2 == 0 ? BANDAGE_P : THORN_P, center.add(0, radius * (0.2 + i * 0.2), 0),
                    radius * Math.cos(Math.asin(Math.min(1, 0.2 + i * 0.2))), 14, 0.02);
        }
        sound(sl, center, ModSounds.ABOMINATION_THORN_BURST, 1.0f, 0.8f);
    }

    /** Ticks after coffinOfAges until the lid has slammed shut (it is sealed, asleep and untouchable from then). */
    public static final int COFFIN_SHUT = 28;
    /** Ticks after coffinOpen until the lid is open (wake it then). */
    public static final int COFFIN_OPENED = 22;

    /** Coffin of Ages: bandages wind round the target, a coffin rises round it and the lid swings shut, chained. */
    public static void coffinOfAges(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        float size = Math.max(0.5f, target.getBbHeight() / 1.8f);
        ModelFx wrap = BANDAGE_WRAP.scale(size).lifetime(ticks).fade(2, 6).tag(tag)
                .key(0, pose().scale(1.3f).alpha(0f).spin(120f))
                .key(14, pose().scale(1f).alpha(1f).spin(0f), Ease.OUT);
        ParticleShapes.modelOnTurning(sl, wrap, target, Vec3.ZERO, 0f, 0f, 0f);
        float coffinSize = Math.max(1f, target.getBbHeight() / 2.2f);
        ModelFx coffin = COFFIN.scale(coffinSize).lifetime(ticks).fade(4, 10).tag(tag)
                .key(0, pose().frame(0f).up(-2.6f * coffinSize))
                .key(12, pose().frame(0f).up(0f), Ease.OUT)
                .key(COFFIN_SHUT - 4, pose().frame(2f))
                .key(COFFIN_SHUT, pose().frame(3f), Ease.IN);
        ParticleShapes.modelOn(sl, coffin, target, Vec3.ZERO, random() * 360f, 0f, 0f);
        ParticleShapes.spiral(sl, BANDAGE_P, target.position().add(0, 0.3, 0), 2, 16, 0.4, 1.2, 1.5, -0.06);
        ParticleShapes.burst(sl, EARTH, target.position().add(0, 0.1, 0), 10, 0.06, 0.16);
        Later.run(sl, COFFIN_SHUT, () -> {
            SpellFx.shockRing(sl, target.position(), 2.4, 0xC0A07050, 10, 0);
            ParticleShapes.burst(sl, IRON_LINK, chestOf(target), 10, 0.05, 0.14);
            ParticleShapes.spawn(sl, PADLOCK, chestOf(target).add(0, 0.6, 0), new Vec3(0, 0.03, 0));
        });
        Later.run(sl, COFFIN_SHUT - 14, () -> sound(sl, target.position(), ModSounds.ABOMINATION_COFFIN_CLOSE, 1.2f, 1.0f));
    }

    /** The coffin opens again: chains fall, the lid swings wide, bandages blow away. */
    public static void coffinOpen(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        float coffinSize = Math.max(1f, target.getBbHeight() / 2.2f);
        ModelFx coffin = COFFIN.scale(coffinSize).lifetime(COFFIN_OPENED + 20).fade(0, 14)
                .key(0, pose().frame(3f))
                .key(COFFIN_OPENED, pose().frame(0f), Ease.OUT)
                .key(COFFIN_OPENED + 20, pose().frame(0f).up(-2.6f * coffinSize), Ease.IN);
        ParticleShapes.model(sl, coffin, target.position(), random() * 360f, 0f, 0f);
        ParticleShapes.burst(sl, IRON_LINK, chestOf(target), 8, 0.05, 0.12);
        Later.run(sl, COFFIN_OPENED, () -> ParticleShapes.burst(sl, BANDAGE_P, chestOf(target), 16, 0.06, 0.2));
        sound(sl, target.position(), ModSounds.ABOMINATION_COFFIN_OPEN, 1.0f, 1.0f);
    }

    /** Sealed on yourself, you rise: thorns burst up in a ring round you, roses light, you are whole. */
    public static void coffinRise(ServerLevel sl, Player player, double radius) {
        int around = (int) Math.max(5, Math.min(12, radius * 2.5));
        for (int i = 0; i < around; i++) {
            Vec3 at = player.position().add(ahead(i * 360f / around).scale(radius * (0.6 + random() * 0.4)));
            thornsAt(sl, at, 0.9f + random() * 0.4f, i % 3, 40, null);
        }
        SpellFx.shockRing(sl, player.position(), radius * 1.2, 0xC0E61832, 12, 0);
        ParticleShapes.burst(sl, ROSE_GLOW, chestOf(player), 16, 0.04, 0.14);
        ParticleShapes.burst(sl, PETAL, chestOf(player), 12, 0.06, 0.16);
        sound(sl, player.position(), ModSounds.ABOMINATION_THORN_BURST, 1.2f, 1.0f);
        sound(sl, player.position(), ModSounds.ABOMINATION_ROSE_HEAL, 0.8f, 1.0f);
    }

    /** Ticks after thornGarden until the whole garden has grown out to its edge. */
    public static final int GARDEN_GROWN = 30;

    /** Thorn Garden: thorns and roses grow outward in a wave from the middle over the whole radius. */
    public static void thornGarden(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        seal(sl, center, Math.min(radius, 6), 0xC0E61832, ticks, tag);
        int thorns = (int) Math.max(8, Math.min(18, radius));
        int roses = (int) Math.max(6, Math.min(14, radius * 0.7));
        for (int i = 0; i < thorns; i++) {
            double a = random() * Math.PI * 2, r = Math.sqrt(random()) * radius;
            Vec3 at = center.add(Math.cos(a) * r, 0, Math.sin(a) * r);
            int delay = (int) (r / radius * GARDEN_GROWN);
            thornsAt(sl, at, 0.6f + random() * 0.6f, delay, Math.max(10, ticks - delay), tag);
        }
        for (int i = 0; i < roses; i++) {
            double a = random() * Math.PI * 2, r = Math.sqrt(random()) * radius;
            Vec3 at = center.add(Math.cos(a) * r, 0.02, Math.sin(a) * r);
            int delay = (int) (r / radius * GARDEN_GROWN) + 4;
            ModelFx rose = ROSE_MODEL.scale(0.8f + random() * 0.6f).delay(delay).lifetime(Math.max(10, ticks - delay)).fade(0, 10)
                    .tag(tag).key(0, pose().scale(0f)).key(8, pose().scale(1f), Ease.OUT_BACK);
            ParticleShapes.model(sl, rose, at, random() * 360f, 0f, 0f);
        }
        SpellFx.steadyRing(sl, center, radius, 0xA0E61832, GARDEN_GROWN, 0);
        sound(sl, center, ModSounds.ABOMINATION_THORN_GARDEN, 1.4f, 1.0f);
    }

    /** Every second or so: petals drift over the garden. */
    public static void gardenTick(ServerLevel sl, Vec3 center, double radius) {
        for (int i = 0; i < 4; i++) {
            double a = random() * Math.PI * 2, r = Math.sqrt(random()) * radius;
            ParticleShapes.spawn(sl, PETAL, center.add(Math.cos(a) * r, 1.5 + random(), Math.sin(a) * r), new Vec3(0, -0.01, 0));
        }
    }

    /** An enemy in the garden is caught by thorns; held = they have closed round it. */
    public static void gardenThorn(ServerLevel sl, LivingEntity enemy, boolean held) {
        ParticleShapes.burst(sl, THORN_P, chestOf(enemy), held ? 6 : 3, 0.03, 0.08);
        ParticleShapes.burst(sl, BLOOD_DROP, chestOf(enemy), 2, 0.02, 0.06);
        if (held) {
            thornsAt(sl, enemy.position(), Math.max(0.6f, enemy.getBbHeight() / 2.2f), 0, 24, null);
            sound(sl, enemy.position(), ModSounds.ABOMINATION_THORN_HIT, 0.8f, wobble(0.9f));
        }
    }

    /** An ally in the garden is healed by the roses. */
    public static void roseHeal(ServerLevel sl, LivingEntity ally) {
        ParticleShapes.burst(sl, ROSE_GLOW, chestOf(ally), 6, 0.02, 0.06);
        ParticleShapes.ring(sl, PETAL, ally.position().add(0, 0.3, 0), 0.6, 6, 0.02);
        sound(sl, ally.position(), ModSounds.ABOMINATION_ROSE_HEAL, 0.5f, wobble(1.0f));
    }

    public static void thornGardenEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
    }

    // =====================================================================================
    // Key 7: the Curses wheel (Disciple of Silence, Ancient Bane, Abomination)
    // =====================================================================================

    /** Ticks after curseByConnection until the curse lands on the far one. */
    public static final int CURSE_LANDS = 16;

    /** Curse by Connection: whispers round the caster, then a thorned eye opens over the cursed, wherever it is. */
    public static void curseByConnection(ServerLevel sl, Player player, LivingEntity target, int ticks, String tag) {
        seal(sl, player.position(), 1.2, 0xC0B070FF, CURSE_LANDS + 10, null);
        ParticleShapes.spiral(sl, curse(), player.position().add(0, 0.2, 0), 2, 14, 0.3, 1.2, 1.0, 0.05);
        sound(sl, player.position(), ModSounds.ABOMINATION_CURSE, 1.0f, 1.0f);
        if (target.level() == sl && target.distanceToSqr(player) < 64 * 64) {
            ParticleShapes.slashBetween(sl, CURSE_LINE.lifetime(CURSE_LANDS), player.getEyePosition(), chestOf(target));
        }
        Later.run(sl, CURSE_LANDS, () -> {
            if (!target.isAlive() || !(target.level() instanceof ServerLevel far)) return;
            curseMark(far, target, ticks, tag, 0xE0B070FF);
            ParticleShapes.burst(far, curse(), headOf(target), 8, 0.03, 0.1);
            sound(far, target.position(), ModSounds.ABOMINATION_CURSE, 0.9f, 0.9f);
        });
    }

    /** A thorned eye over the head and a ring at the feet, for 'ticks'. */
    private static void curseMark(ServerLevel sl, LivingEntity target, int ticks, String tag, int color) {
        ModelFx mark = CURSE_MARK.color(color).scale(0.7f).lifetime(ticks).fade(4, 10).tag(tag).spin(1.5f)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(8, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.7, 0), 0f, 0f, 0f);
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth() * 0.8), withAlpha(color, 0xA0), ticks, tag);
    }

    /** A connection is made (a hit, a chain, a curse): a padlock blinks over the target, seen only by the owner. */
    public static void connectionMade(ServerPlayer owner, LivingEntity target) {
        ParticleShapes.spawnFor(owner, PADLOCK, headOf(target).add(0, 0.5, 0), new Vec3(0, 0.02, 0));
    }

    /** Cursed Artifact: the relic is set down. Only its owner sees and hears it. */
    public static void relicPlaced(ServerPlayer owner, Vec3 at) {
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            ParticleShapes.spawnFor(owner, CURSE[i % 2], at.add(Math.cos(a) * 0.6, 0.2, Math.sin(a) * 0.6), new Vec3(0, 0.03, 0));
        }
        ParticleShapes.spawnFor(owner, PADLOCK, at.add(0, 0.6, 0), new Vec3(0, 0.02, 0));
        privateSound(owner, ModSounds.ABOMINATION_RELIC_SET, 0.8f, 1.0f);
    }

    /** Every second or so while it waits: a curse glyph over it, for the owner only. */
    public static void relicTick(ServerPlayer owner, Vec3 at) {
        ParticleShapes.spawnFor(owner, curse(), at.add(0, 0.4, 0), new Vec3(0, 0.02, 0));
    }

    /** Someone stepped near it: the doll springs up for all to see, laughs and crumbles; its curse goes into them. */
    public static void relicTriggered(ServerLevel sl, LivingEntity victim, Vec3 at, int ticks, String tag) {
        ModelFx doll = DOLL_VESSEL.scale(0.3f).lifetime(24).fade(0, 8)
                .key(0, pose().up(-0.8f).scale(0.4f))
                .key(5, pose().up(0f).scale(1f), Ease.OUT_BACK)
                .key(18, pose().spin(40f))
                .key(24, pose().scale(0.2f).alpha(0f), Ease.IN);
        ParticleShapes.model(sl, doll, at, yawToward(at, victim.position()), 0f, 0f);
        ParticleShapes.burst(sl, curse(), at.add(0, 0.5, 0), 12, 0.05, 0.14);
        ParticleShapes.slashBetween(sl, CURSE_LINE.lifetime(10), at.add(0, 0.5, 0), chestOf(victim));
        Later.run(sl, 20, () -> ParticleShapes.burst(sl, DUST, at.add(0, 0.3, 0), 6, 0.02, 0.06));
        curseMark(sl, victim, ticks, tag, 0xE0B070FF);
        silenced(sl, victim);
        sound(sl, at, ModSounds.ABOMINATION_RELIC_TRIGGER, 1.0f, 1.0f);
    }

    /** Ticks after riteOfSilence until the hush has fallen over the whole ring. */
    public static final int RITE_FALLS = 12;

    /** Rite of Silence: a sigil of sewn lips spreads over the ground, a ring of quiet closes in, everything hushes. */
    public static void riteOfSilence(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        float width = (float) (radius * 2.0);
        ModelFx sigil = SILENCE_SIGIL.color(0xE0E8DCB0).scale(width).lifetime(ticks).fade(0, 14).spin(0.4f).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(RITE_FALLS, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, sigil, center.add(0, 0.05, 0), random() * 360f, 0f, 0f);
        SpellFx.closingRing(sl, center, radius * 1.3, 0xA0B070FF, RITE_FALLS, 0);
        ParticleShapes.ring(sl, STITCH, center.add(0, 1.2, 0), radius * 0.7, 12, 0.0);
        sound(sl, center, ModSounds.ABOMINATION_SILENCE, 1.4f, 1.0f);
    }

    /** Someone is hushed: a stitch over the mouth, a padlock. */
    public static void silenced(ServerLevel sl, LivingEntity enemy) {
        Vec3 mouth = enemy.getEyePosition().add(enemy.getLookAngle().scale(0.35)).add(0, -0.15, 0);
        ParticleShapes.spawn(sl, STITCH, mouth, new Vec3(0, 0.01, 0));
        ParticleShapes.spawn(sl, PADLOCK, headOf(enemy).add(0, 0.5, 0), new Vec3(0, 0.02, 0));
    }

    public static void riteEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
    }

    /** Bane Mist: a dark seal, and a pale, blood-spotted mist starts rolling out (keep it going with baneMistTick). */
    public static void baneMist(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        seal(sl, center, Math.min(radius * 0.5, 6), 0xA0BEAAB4, ticks, tag);
        SpellFx.steadyRing(sl, center, radius, 0x80BEAAB4, 30, 0);
        ParticleShapes.ring(sl, BANE_MIST, center.add(0, 0.6, 0), 1.5, 16, 0.2);
        Vec3 sky = center.add(0, 14, 0);
        ModelFx moon = MAD_MOON.color(0xFFC08890).scale(5f).lifetime(ticks).fade(30, 30).tag(tag)
                .key(0, pose().alpha(0f)).key(30, pose().alpha(0.5f));
        ParticleShapes.model(sl, moon, sky, random() * 360f, 0f, 0f);
        sound(sl, center, ModSounds.ABOMINATION_BANE_MIST, 1.6f, 1.0f);
    }

    /** Every few ticks: mist billows over the radius, with drops of blood and black spots hanging in it. */
    public static void baneMistTick(ServerLevel sl, Vec3 center, double radius) {
        for (int i = 0; i < 6; i++) {
            double a = random() * Math.PI * 2, r = Math.sqrt(random()) * radius;
            Vec3 at = center.add(Math.cos(a) * r, 0.4 + random() * 1.6, Math.sin(a) * r);
            ParticleShapes.spawn(sl, BANE_MIST, at, new Vec3(0, 0.004, 0));
            if (i < 2) ParticleShapes.spawn(sl, i == 0 ? BANE_BLOOD : BANE_BLACK, at, Vec3.ZERO);
        }
    }

    /** On one caught in it: curse glyphs crawl over the body, more the deeper its misfortune (level 1..5). */
    public static void baneMistOn(ServerLevel sl, LivingEntity enemy, int level) {
        ParticleShapes.burst(sl, curse(), chestOf(enemy), Math.max(1, Math.min(5, level)), 0.02, 0.05);
    }

    public static void baneMistEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
    }

    /** Ancient Blight on one carrier at 'stage' (0 = the first): the eye over its head darkens with every jump. */
    public static void blight(ServerLevel sl, LivingEntity target, int stage, int ticks, String tag) {
        float t = Math.min(1f, stage / 5f);
        int r = (int) (0xB0 + (0x40 - 0xB0) * t), g = (int) (0x70 + (0x10 - 0x70) * t), b = (int) (0xFF + (0x60 - 0xFF) * t);
        curseMark(sl, target, ticks, tag, 0xE0000000 | (r << 16) | (g << 8) | b);
        ParticleShapes.burst(sl, SKULL, headOf(target), 1 + stage / 2, 0.02, 0.05);
    }

    /** The blight jumps from one to the next. */
    public static void blightJump(ServerLevel sl, LivingEntity from, LivingEntity to, int stage) {
        ParticleShapes.slashBetween(sl, CURSE_LINE.color(0xC04A1060).lifetime(10), chestOf(from), chestOf(to));
        ParticleShapes.burst(sl, curse(), chestOf(to), 6 + stage, 0.03, 0.1);
        sound(sl, to.position(), ModSounds.ABOMINATION_BLIGHT_JUMP, 0.9f, wobble(1.0f - stage * 0.05f));
    }

    /** Ticks after abominationForm until the vessel is whole round the player. */
    public static final int FORM_RISEN = 34;

    /** Abomination Form: thorns burst up round the player, the doll-vessel closes round them, bound by vines,
     *  and the curses it holds spill out as glyphs. 'height' = how tall the vessel stands (2.75 = its own size). */
    public static void abominationForm(ServerLevel sl, Player player, double height, int ticks, String tag) {
        float full = (float) (height / 2.75);
        ModelFx vessel = DOLL_VESSEL.scale(full).lifetime(ticks).fade(4, 14).tag(tag)
                .key(0, pose().scale(0.3f).alpha(0f).spin(180f))
                .key(FORM_RISEN, pose().scale(1f).alpha(1f).spin(0f), Ease.OUT_BACK);
        ParticleShapes.modelOnTurning(sl, vessel, player, Vec3.ZERO, 0f, 0f, 0f);
        for (int i = 0; i < 8; i++) {
            Vec3 at = player.position().add(ahead(i * 45f).scale(1.6 + random() * 0.8));
            thornsAt(sl, at, 1.1f + random() * 0.5f, i * 2, FORM_RISEN + 10, null);
        }
        seal(sl, player.position(), 3.5, 0xE06C3082, FORM_RISEN + 30, null);
        for (int t = 0; t < FORM_RISEN; t += 4) {
            Later.run(sl, t, () -> {
                if (!player.isAlive()) return;
                ParticleShapes.spiral(sl, curse(), player.position().add(0, 0.2, 0), 3, 6, 0.4, 1.4, 1.2, 0.06);
            });
        }
        Later.run(sl, FORM_RISEN, () -> {
            if (!player.isAlive()) return;
            SpellFx.shockRing(sl, player.position(), 8, 0xE06C3082, 16, 0);
            ParticleShapes.burst(sl, PURPLE_MOTE, chestOf(player), 24, 0.08, 0.25);
        });
        sound(sl, player.position(), ModSounds.ABOMINATION_ABOMINATION_FORM, 1.6f, 1.0f);
    }

    /** Once a second in the form: curse glyphs drift out to the edge of the aura ('radius'). */
    public static void abominationTick(ServerLevel sl, Player player, double radius) {
        for (int i = 0; i < 4; i++) {
            double a = random() * Math.PI * 2;
            Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
            ParticleShapes.spawn(sl, curse(), chestOf(player).add(out.scale(0.6)), out.scale(radius / 28.0).add(0, 0.01, 0));
        }
    }

    /** Whoever hurts the vessel is cursed: a brief eye over them, glyphs. */
    public static void abominationCursed(ServerLevel sl, LivingEntity attacker) {
        ModelFx mark = CURSE_MARK.color(0xE0B070FF).scale(0.6f).lifetime(20).fade(2, 8)
                .key(0, pose().scale(0.2f)).key(5, pose().scale(1f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, mark, attacker, new Vec3(0, attacker.getBbHeight() + 0.6, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, curse(), chestOf(attacker), 5, 0.03, 0.08);
    }

    /** The curses come due: on each victim, a black-purple blast, skulls and a toll. */
    public static void calamity(ServerLevel sl, LivingEntity victim) {
        Vec3 at = chestOf(victim);
        ModelFx mark = CURSE_MARK.color(0xFF2A0A3A).scale(1.6f).lifetime(14).fade(0, 8)
                .key(0, pose().scale(0.3f)).key(4, pose().scale(1.2f), Ease.OUT).key(14, pose().scale(1.6f).alpha(0f));
        ParticleShapes.model(sl, mark, at.add(0, 0.6, 0), random() * 360f, 0f, 0f);
        SpellFx.blast(sl, at, 2.2, 0xE06C3082, PURPLE_MOTE);
        SpellFx.risingRings(sl, victim.position(), 1.4, 3.0, 4, 0xE02A0A3A);
        ParticleShapes.burst(sl, SKULL, at, 6, 0.08, 0.2);
        sound(sl, victim.position(), ModSounds.ABOMINATION_CALAMITY, 1.4f, wobble(1.0f));
    }

    /** The form ends: the vessel unravels into thorns, cloth and a last cloud of glyphs. */
    public static void abominationFormEnd(ServerLevel sl, Player player, double height, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        for (double y = 0.2; y < height; y += 0.5) {
            ParticleShapes.ring(sl, THORN_P, player.position().add(0, y, 0), 0.6, 6, 0.06);
            ParticleShapes.ring(sl, curse(), player.position().add(0, y, 0), 0.4, 3, 0.04);
        }
        sound(sl, player.position(), ModSounds.ABOMINATION_THORN_BURST, 1.0f, 0.7f);
    }

    // =====================================================================================
    // Passives
    // =====================================================================================

    /** Lunatic: the full moon rises over the player (once, when the night of the full moon begins). */
    public static void fullMoonRises(ServerLevel sl, Player player) {
        ParticleShapes.ring(sl, CRESCENT, player.position().add(0, 1.0, 0), 1.2, 10, 0.04);
        SpellFx.risingRings(sl, player.position(), 1.0, 2.2, 3, 0x80E8E4D6);
        sound(sl, player.position(), ModSounds.ABOMINATION_HOWL, 0.5f, 1.1f);
    }

    /** Werewolf's Keen Senses: something started hunting the player out of sight. A sound only they hear. */
    public static void keenSenses(ServerPlayer player) {
        privateSound(player, ModSounds.ABOMINATION_GROAN, 0.25f, 1.8f);
    }

    /** Wraith's Weightless, once a second: a wisp over a spirit, ghost or summon near the player, only they see it. */
    public static void spiritSeen(ServerPlayer viewer, LivingEntity spirit) {
        ParticleShapes.spawnFor(viewer, GHOST_WISP, headOf(spirit).add(0, 0.4, 0), new Vec3(0, 0.02, 0));
    }

    /** Disciple of Silence's Whispering Curse (and the Abomination's): a melee attacker is cursed. */
    public static void whisperingCurse(ServerLevel sl, LivingEntity attacker) {
        ParticleShapes.burst(sl, curse(), headOf(attacker), 3, 0.02, 0.06);
        sound(sl, attacker.position(), ModSounds.ABOMINATION_CURSE, 0.35f, wobble(1.4f));
    }

    /** Ancient Bane's aura, once a second: a few blood and black motes hang in the air round the player. */
    public static void baneAura(ServerLevel sl, Player player, double radius) {
        for (int i = 0; i < 3; i++) {
            double a = random() * Math.PI * 2, r = 1.5 + random() * (radius - 1.5);
            ParticleShapes.spawn(sl, i == 0 ? BANE_BLOOD : BANE_BLACK,
                    player.position().add(Math.cos(a) * r, 0.5 + random() * 1.5, Math.sin(a) * r), Vec3.ZERO);
        }
    }

    /** Chained's aura, once a second: thorns and a rose petal at the ground round the player. */
    public static void chainedAura(ServerLevel sl, Player player, double radius) {
        for (int i = 0; i < 3; i++) {
            double a = random() * Math.PI * 2, r = 1.0 + random() * (radius - 1.0);
            ParticleShapes.spawn(sl, i == 0 ? PETAL : THORN_P,
                    player.position().add(Math.cos(a) * r, 0.3 + random() * 0.6, Math.sin(a) * r), new Vec3(0, 0.01, 0));
        }
    }

    /** Chained: whoever hits the player in melee is pricked by thorns. */
    public static void thornsBack(ServerLevel sl, LivingEntity attacker) {
        ParticleShapes.burst(sl, THORN_P, chestOf(attacker), 5, 0.04, 0.1);
        sound(sl, attacker.position(), ModSounds.ABOMINATION_THORN_HIT, 0.7f, wobble(1.0f));
    }

    // =====================================================================================
    // /abominationfx: every display, to look at in game
    // =====================================================================================

    @FunctionalInterface
    private interface Show {
        void play(ServerLevel sl, Player viewer, LivingEntity target, Vec3 spot);
    }

    /** onViewer = it is shown on the viewer (worn), not on a spot or a target. */
    private record Case(String name, int ticks, boolean onViewer, Show show) {}

    private static String tagFor(String name) {
        return "ab_fx_" + name + "_" + UUID.randomUUID();
    }

    private static final List<Case> CASES = List.of(
            new Case("blood_chain", 50, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("chain");
                bloodChain(sl, v, t);
                Later.run(sl, CHAIN_REACHES, () -> bloodChainHold(sl, v, t, 30, tag));
                for (int i = 1; i < 3; i++) Later.run(sl, CHAIN_REACHES + i * 10, () -> bleed(sl, t));
                Later.run(sl, CHAIN_REACHES + 30, () -> bloodChainEnd(sl, t, tag));
            }),
            new Case("restraint", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("restraint");
                restraint(sl, v, 80, tag);
                Later.run(sl, RESTRAINT_LOCKED + 20, () -> restraintHit(sl, v));
                Later.run(sl, 80, () -> restraintEnd(sl, v, tag));
            }),
            new Case("moonstruck", 90, false, (sl, v, t, spot) -> {
                String tag = tagFor("moon");
                moonstruck(sl, v, t, 80, tag);
                for (int i = 1; i < 4; i++) {
                    boolean laugh = i == 2;
                    Later.run(sl, MOON_SHOWN + i * 20, () -> madnessTick(sl, t, laugh));
                }
                Later.run(sl, 80, () -> moonstruckEnd(sl, t, tag));
            }),
            new Case("lunar_frenzy", 120, true, (sl, v, t, spot) -> {
                String tag = tagFor("frenzy");
                lunarFrenzy(sl, v, 70, tag);
                for (int i = 4; i < 70; i += 4) Later.run(sl, i, () -> frenzyTick(sl, v));
                Later.run(sl, 70, () -> frenzyCrash(sl, v, 40, tag));
                Later.run(sl, 110, () -> frenzyEnd(sl, v, tag));
            }),
            new Case("rending_pounce", 40, true, (sl, v, t, spot) -> {
                pounceLeap(sl, v);
                for (int i = 1; i < 8; i++) Later.run(sl, i, () -> pounceTrail(sl, v));
                if (t != v) for (int i = 0; i < 3; i++) {
                    int index = i;
                    Later.run(sl, 8 + i * 5, () -> pounceClaw(sl, v, t, index));
                }
            }),
            new Case("werewolf_form", 140, true, (sl, v, t, spot) -> {
                String tag = tagFor("wolf");
                double height = v.getBbHeight() * 1.1;
                werewolfForm(sl, v, height, 130, tag);
                for (int i = 0; i < 4; i++) {
                    float madness = i / 3f;
                    Later.run(sl, WOLF_RISEN + 20 + i * 20, () -> werewolfMadness(sl, v, madness, height));
                }
                if (t != v) Later.run(sl, WOLF_RISEN + 30, () -> werewolfClaw(sl, v, t));
                Later.run(sl, 130, () -> werewolfFormEnd(sl, v, tag));
            }),
            new Case("raise_the_dead", 70, false, (sl, v, t, spot) -> {
                raiseTheDead(sl, spot, 3);
                for (int i = 0; i < 3; i++) {
                    Vec3 at = spot.add(ahead(i * 120f).scale(1.6));
                    Later.run(sl, 6 + i * 6, () -> zombieRises(sl, at));
                }
                Later.run(sl, 30, () -> fallenRises(sl, spot));
            }),
            new Case("grave_chill", 110, true, (sl, v, t, spot) -> {
                String tag = tagFor("chill");
                graveChill(sl, v, 6, 100, tag);
                for (int i = 4; i < 100; i += 4) Later.run(sl, i, () -> graveChillTick(sl, v, 6));
                if (t != v) {
                    for (int i = 1; i < 4; i++) {
                        float cold = i / 3f;
                        Later.run(sl, i * 15, () -> chilled(sl, t, cold));
                    }
                    Later.run(sl, 60, () -> chillFreeze(sl, t));
                }
                Later.run(sl, 100, () -> graveChillEnd(sl, v, tag));
            }),
            new Case("undying_flesh", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("undying");
                undyingFlesh(sl, v, 80, tag);
                for (int i = 20; i < 80; i += 20) Later.run(sl, i, () -> undyingTick(sl, v));
                Later.run(sl, 40, () -> undyingHit(sl, v));
                Later.run(sl, 80, () -> undyingEnd(sl, v, tag));
            }),
            new Case("death_sentence", 130, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("sentence");
                deathSentence(sl, v, t, 100, tag);
                for (int i = 1; i <= 4; i++) {
                    int left = 5 - i;
                    Later.run(sl, i * 20, () -> sentenceTick(sl, t, left));
                }
                Later.run(sl, 100, () -> sentenceFalls(sl, t, tag));
            }),
            new Case("mirror_step", 50, true, (sl, v, t, spot) -> {
                Vec3 in = v.position().add(Spells.flatLook(v).scale(1.5));
                mirrorEnter(sl, v, in);
                Later.run(sl, MIRROR_OPENS + 14, () -> mirrorExit(sl, v, spot));
            }),
            new Case("possession", 110, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("possess");
                possessionDive(sl, v, t);
                Later.run(sl, GHOST_ARRIVES, () -> {
                    possessed(sl, t, 80, tag);
                    bodyLeft(sl, v, 80, tag);
                });
                for (int i = 20; i < 90; i += 20) Later.run(sl, i, () -> possessionTick(sl, t));
                Later.run(sl, 90, () -> possessionExit(sl, t, v, tag));
            }),
            new Case("ghost_host", 60, true, (sl, v, t, spot) -> {
                ghostHost(sl, v, 3);
                Later.run(sl, GHOSTS_RISEN + 10, () -> ghostFades(sl, v.position().add(0, 1, 0)));
                if (t != v) Later.run(sl, GHOSTS_RISEN, () -> ghostTakenOver(sl, t, 30, tagFor("taken")));
            }),
            new Case("animate_objects", 50, true, (sl, v, t, spot) -> {
                List<Entity> things = new ArrayList<>(sl.getEntities(v, v.getBoundingBox().inflate(12),
                        e -> !(e instanceof LivingEntity)));
                if (things.size() > 12) things = things.subList(0, 12);
                animateObjects(sl, v, things);
                for (Entity thing : things) Later.run(sl, OBJECTS_RISE, () -> hurlLaunch(sl, thing));
                if (t != v) Later.run(sl, OBJECTS_RISE + 10, () -> hurledHit(sl, chestOf(t)));
            }),
            new Case("grave_shackles", 100, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("shackles");
                List<LivingEntity> enemies = List.of(t);
                graveShackles(sl, enemies, 90, tag);
                Later.run(sl, 40, () -> painShared(sl, t, v));
                Later.run(sl, 90, () -> graveShacklesEnd(sl, enemies, tag));
            }),
            new Case("mausoleum", 180, false, (sl, v, t, spot) -> {
                String tag = tagFor("mausoleum");
                mausoleum(sl, spot, 5, 170, tag);
                for (int i = MAUSOLEUM_SEALED; i < 170; i += 20) Later.run(sl, i, () -> mausoleumTick(sl, spot, 5));
                if (t != v) Later.run(sl, 60, () -> mausoleumThorn(sl, t));
                Later.run(sl, 170, () -> mausoleumEnd(sl, spot, 5, tag));
            }),
            new Case("coffin", 130, false, (sl, v, t, spot) -> {
                String tag = tagFor("coffin");
                coffinOfAges(sl, t, 90, tag);
                Later.run(sl, 90, () -> coffinOpen(sl, t, tag));
                if (t == v) Later.run(sl, 90 + COFFIN_OPENED, () -> coffinRise(sl, v, 3));
            }),
            new Case("thorn_garden", 150, false, (sl, v, t, spot) -> {
                String tag = tagFor("garden");
                thornGarden(sl, spot, 8, 140, tag);
                for (int i = GARDEN_GROWN; i < 140; i += 15) Later.run(sl, i, () -> gardenTick(sl, spot, 8));
                if (t != v) {
                    Later.run(sl, 50, () -> gardenThorn(sl, t, false));
                    Later.run(sl, 80, () -> gardenThorn(sl, t, true));
                }
                Later.run(sl, 60, () -> roseHeal(sl, v));
                Later.run(sl, 140, () -> thornGardenEnd(sl, spot, tag));
            }),
            new Case("curse_by_connection", 90, false, (sl, v, t, spot) -> {
                String tag = tagFor("curse");
                if (v instanceof ServerPlayer sp && t != v) connectionMade(sp, t);
                curseByConnection(sl, v, t, 60, tag);
            }),
            new Case("cursed_artifact", 80, false, (sl, v, t, spot) -> {
                if (v instanceof ServerPlayer sp) {
                    relicPlaced(sp, spot);
                    for (int i = 10; i < 40; i += 10) Later.run(sl, i, () -> relicTick(sp, spot));
                }
                Later.run(sl, 40, () -> relicTriggered(sl, t, spot, 40, tagFor("relic")));
            }),
            new Case("rite_of_silence", 110, false, (sl, v, t, spot) -> {
                String tag = tagFor("rite");
                riteOfSilence(sl, spot, 5, 100, tag);
                if (t != v) for (int i = 1; i < 5; i++) Later.run(sl, RITE_FALLS + i * 20, () -> silenced(sl, t));
                Later.run(sl, 100, () -> riteEnd(sl, spot, tag));
            }),
            new Case("bane_mist", 160, false, (sl, v, t, spot) -> {
                String tag = tagFor("bane");
                baneMist(sl, spot, 8, 150, tag);
                for (int i = 4; i < 150; i += 4) Later.run(sl, i, () -> baneMistTick(sl, spot, 8));
                if (t != v) for (int i = 1; i < 6; i++) {
                    int level = i;
                    Later.run(sl, i * 25, () -> baneMistOn(sl, t, level));
                }
                Later.run(sl, 150, () -> baneMistEnd(sl, spot, tag));
            }),
            new Case("ancient_blight", 80, false, (sl, v, t, spot) -> {
                String tag = tagFor("blight");
                blight(sl, t, 0, 30, tag);
                Later.run(sl, 30, () -> {
                    blightJump(sl, t, v, 1);
                    blight(sl, v, 1, 30, tag);
                });
                Later.run(sl, 60, () -> blight(sl, t, 3, 20, tag));
            }),
            new Case("abomination_form", 200, true, (sl, v, t, spot) -> {
                String tag = tagFor("vessel");
                abominationForm(sl, v, 2.75, 180, tag);
                for (int i = FORM_RISEN; i < 180; i += 20) Later.run(sl, i, () -> abominationTick(sl, v, 8));
                if (t != v) {
                    Later.run(sl, FORM_RISEN + 30, () -> abominationCursed(sl, t));
                    Later.run(sl, 180, () -> calamity(sl, t));
                }
                Later.run(sl, 180, () -> abominationFormEnd(sl, v, 2.75, tag));
            }),
            new Case("passives", 60, true, (sl, v, t, spot) -> {
                fullMoonRises(sl, v);
                if (v instanceof ServerPlayer sp) Later.run(sl, 10, () -> keenSenses(sp));
                for (int i = 0; i < 3; i++) Later.run(sl, i * 20, () -> {
                    baneAura(sl, v, 10);
                    chainedAura(sl, v, 16);
                });
                if (t != v) {
                    Later.run(sl, 20, () -> whisperingCurse(sl, t));
                    Later.run(sl, 40, () -> thornsBack(sl, t));
                }
            }));

    /** The names /abominationfx knows. */
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
        event.getDispatcher().register(Commands.literal("abominationfx")
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
