package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator;

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
 * How the Illuminator (Paragon) pathway looks and sounds: every key, both casts, every wheel entry, the growth at
 * higher Sequences (two flasks and the acid puddle, Recall on an ally, Exploit Flaw on every mark, the arm cannon, the
 * steam form), the Appraised mark, the statuses, the passives that show and the melee combo. Visual only: nothing here
 * hurts, holds, hides, heals or moves anyone. The ability code does that, at the ticks named by the constants below
 * (..._LANDS, ..._READY, ..._RISEN...), with Later.run. See docs/ILLUMINATOR.md.
 *
 * ---------- See them in game ----------
 *   /illuminatorfx all              every display one after another (/illuminatorfx all 3 = three times over)
 *   /illuminatorfx meteor_shower    just that one (press TAB after /illuminatorfx for the names)
 * Displays that need a target use the nearest mob within 12 blocks, or you when there is none.
 *
 * ---------- Use them in an ability ----------
 *   Mark    appraisedMark(sl, target, weakness, ticks, tag) puts the Appraised lens over a head; appraisedEnd
 *   Key 1   flaskThrow(sl, player), FLASK_FLYING for the Shot, flaskTrail(sl, at), flaskBurst(sl, at, radius),
 *               corroded(sl, target, ticks, tag), corrodedEnd; Arcane Scholar: acidPuddle(sl, ground, radius, ticks,
 *               tag), acidPuddleEnd(sl, ground, tag)
 *           sneak: recall(sl, who, from, to) (the Savant, or the ally sent back at Illuminator)
 *   Key 2   survey(sl, player, reach) (the pulse reaches its edge at SURVEY_PULSE), surveyed(sl, target),
 *               surveyForcedOut(sl, hidden); Arcane Scholar: surveyResidue(viewer, at, color)
 *           sneak: ritualCircle(sl, player, center, radius, ticks, tag) (drawn at RITUAL_DRAWN), ritualPulse(sl,
 *               center, radius) every 3 seconds, ritualCleansed(sl, ally), ritualEnd(sl, center, radius, tag)
 *   Key 3   appraise(sl, player, target, weakness, ticks, tag) (the mark settles at APPRAISE_LANDS)
 *           sneak: exploitFlaw(sl, player, target) (it strikes at FLAW_LANDS), noFlaw(sl, player),
 *               constructTakenApart(sl, construct, destroyed); Illuminator: exploitAll(sl, player, targets)
 *   Key 4   the Workshop wheel's pictures: WHEEL_CLOCKWORK_RIFLE, WHEEL_SPIRIT_CANNON, WHEEL_MYSTIC_BROOCH
 *           rifle(sl, player, rounds, armCannon) (round i leaves at RIFLE_FIRES + i * RIFLE_STEP), rifleShot(sl,
 *               player, from, to, weakness), ROUND for a Shot, rifleHit(sl, at, weakness)
 *           spiritCannon(sl, at, yaw, ticks, tag) (it fires from CANNON_READY), cannonAim(sl, at, tag, target),
 *               cannonFire(sl, at, tag, target) returns where the ball leaves, CANNONBALL for the Shot, cannonBlast(sl,
 *               at, radius), cannonEnd(sl, at, tag)
 *           mysticBrooch(sl, wearer, ticks, tag), broochAbsorb(sl, wearer), broochBurst(sl, wearer, radius, tag)
 *   Key 5   the Astronomy wheel's pictures: WHEEL_STAR_OF_CURSES ... WHEEL_METEOR_SHOWER
 *           starOfCurses(sl, player, target, curse) (the ray lands at CURSE_LANDS), cursed(sl, target, curse, ticks,
 *               tag), curseEnd(sl, target, tag)
 *           blessingOfStars(sl, center, radius, ticks, tag) (risen at BLESSING_RISEN), blessingTick(sl, center,
 *               radius) every second, pressed(sl, enemy), weightless(sl, ally), blessingEnd(sl, center, radius, tag)
 *           falseConstellations(sl, center, radius, ticks, tag), falseConstellationsEnd(sl, center, radius, tag)
 *           meteorShower(sl, center, radius) as it starts, meteor(sl, ground, strong) per meteor (lands at
 *               METEOR_LANDS: its warning ring shows before)
 *   Key 6   the Alchemy and Arcana wheel's pictures: WHEEL_LIVING_DOLL ... WHEEL_KNOWLEDGE_SPIRIT
 *           dollRise(sl, at, yaw, material) (it stands at DOLL_STANDS), dollBody(sl, doll, material, ticks, tag),
 *               dollBolt(sl, doll, to), dollFall(sl, doll, material, tag)
 *           golemAssemble(sl, at, yaw) (whole at GOLEM_STANDS), golemBody(sl, golem, ticks, tag), golemStrike(sl,
 *               golem, ticksLeft, tag) (the blow lands at GOLEM_STRIKE_LANDS), golemTaunt(sl, golem, radius),
 *               golemEnd(sl, golem, tag)
 *           mechanicalBody(sl, player, ticks, tag) (clamped on at MECH_ON), jet(sl, player) every few ticks while
 *               rising, mechanicalBodyEnd(sl, player, tag); Magister: steamForm(sl, player, ticks, tag),
 *               steamTrail(sl, player), steamReform(sl, player, tag)
 *           dissociation(sl, player, from, to, width), dissolved(sl, target), blockFades(sl, blockCenter)
 *           knowledgeSpirit(sl, player, from, to, flight) (it arrives at 'flight'), mindEntered(sl, target),
 *               eroded(sl, target, ticks, tag), spiritStepOut(sl, player)
 *   Key 7   the Civilization wheel's pictures: WHEEL_REWRITE_PRINCIPLES ... WHEEL_MECHANICAL_HEAVEN
 *           rewritePrinciples(sl, center, radius, ticks, tag) (risen at PRINCIPLES_RISEN), lifted(sl, enemy, ticks,
 *               tag), principlesTick(sl, center, radius), principlesEnd(sl, center, radius, tag)
 *           colossus(sl, player, ticks, tag) (it stands at COLOSSUS_READY), colossusStep(sl, player),
 *               colossusStride(sl, player, ticksLeft, tag), fusionCharge(sl, player, ticksLeft, tag) (it fires at
 *               FUSION_FIRES), fusionBeam(sl, from, to, width), colossusEnd(sl, player, tag)
 *           scrollsOfCivilization(sl, player, radius, ticks, tag), scrollsStop(sl, at), scrollsRelease(sl, player,
 *               radius, tag) (the scrolls reach 'radius' at SCROLLS_FLY), immersed(sl, target, ticks, tag)
 *           mathematicalMaze(sl, player, target, ticks, tag) (closed at MAZE_CLOSED), mazeTouch(sl, at), mazeEnd;
 *               enlightenment(sl, player, ally, ticks, tag)
 *           masterwork(sl, player, radius, ticks, tag), masterworkTick(sl, player, radius) every 10 ticks,
 *               masterworkEnd(sl, player, tag)
 *           realityKnowledge(sl, player, target, ticks, tag) (it lands at REALITY_LANDS), forgottenEnd
 *           illusoryBook(sl, at, yaw, ticks, tag) (open at BOOK_OPEN), written(sl, victim, at) for each one written in,
 *               bookRelease(sl, at, tag) (they step out at BOOK_RELEASES)
 *           mechanicalHeaven(sl, player, radius, ticks, tag) (risen at HEAVEN_RISEN), heavenTick(sl, center, radius)
 *               every second, heavenStatic(sl, at) for a projectile collapsing, heavenEnd(sl, center, radius, tag)
 *   Passives: starlight(sl, player), constructorsBond(viewer, construct), absorption(sl, player, from),
 *             analysis(viewer, hidden), enlightenmentAura(sl, ally), realityGuard(sl, player)
 *   Melee: combo(sl, player, target, step) for hits 0, 1 and 2
 *   Every timed cast: status(player, Status.X, ticks) shows its picture and its clock; statusEnd(player, Status.X).
 *
 * The models are in models/model_fx/illuminator/ (made by tools/model_gen/illuminator/models.py and pictures.py; each
 * JSON's "credit" gives its size and pivot), the pictures in textures/gui/illuminator/ and
 * textures/gui/ability_menus/illuminator/ (icons.py), the particles in textures/particle/fx/ (particles.py), the sounds
 * in sounds/illuminator/ (sounds.py) and sounds/ModSounds (ILLUMINATOR_...).
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class IlluminatorFx {

    private IlluminatorFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // Colors (ARGB)
    // =====================================================================================

    /** The Pathway's own color: brass. */
    public static final int PATHWAY_COLOR = 0xFFD9A441;
    public static final int BRASS = 0xFFD9A441;
    public static final int BRASS_LIGHT = 0xFFFFE296;
    public static final int COPPER = 0xFFD67846;
    public static final int BLUEPRINT = 0xFF4696F0;
    public static final int BLUEPRINT_LIGHT = 0xFFBEE4FF;
    public static final int STEAM_WHITE = 0xFFECF0F6;
    public static final int PARCHMENT = 0xFFF3E6C4;
    public static final int INK = 0xFF1B2A41;
    public static final int STAR_GOLD = 0xFFFFECAA;
    public static final int ACID = 0xFFC8E63C;
    public static final int CURSE_VIOLET = 0xFFB078FF;
    public static final int WHITE_GOLD = 0xFFFFF4D0;

    /** What an Appraised enemy is weak to, with the lens shown over it and its color (docs/ILLUMINATOR.md). */
    public enum Weakness {
        STARLIGHT("illuminator/appraisal_starlight", 0xFFFFECAA),
        CORROSION("illuminator/appraisal_corrosion", 0xFFC8E63C),
        GRAVITY("illuminator/appraisal_gravity", 0xFFB078FF),
        PIERCE("illuminator/appraisal_pierce", 0xFFEBF0FA);

        public final int color;
        final ModelFx mark;

        Weakness(String model, int color) {
            this.color = color;
            this.mark = ModelFx.of(model).pivot(8, 8, 8).glow().unshaded().seeThrough();
        }
    }

    /** What a Living Doll is made of (the ground under the Alchemist), with its model and the dust it sheds. */
    public enum Material {
        STONE("illuminator/doll_stone", 0xFFA8A8A8),
        STRAW("illuminator/doll_straw", 0xFFF0D888),
        IRON("illuminator/doll_iron", 0xFF8A8E98),
        DUST("illuminator/doll_dust", 0xFFECD49A),
        FROST("illuminator/doll_frost", 0xFFD8F4FF);

        public final int color;
        final ModelFx body;

        Material(String model, int color) {
            this.color = color;
            ModelFx m = ModelFx.of(model).glow();
            this.body = model.endsWith("frost") ? m.seeThrough() : m;
        }
    }

    /** The ladder of the Star of Curses: Weakness, then Trauma, then Mutation. */
    public enum Curse {
        WEAKNESS("illuminator/curse_weakness", 0xFFD8C8FF),
        TRAUMA("illuminator/curse_trauma", 0xFFFF6A6A),
        MUTATION("illuminator/curse_mutation", 0xFF9ADC5A);

        public final int color;
        final ModelFx mark;

        Curse(String model, int color) {
            this.color = color;
            this.mark = ModelFx.of(model).pivot(8, 8, 8).glow().unshaded().seeThrough();
        }
    }

    // =====================================================================================
    // The small particles everything shares
    // =====================================================================================

    private static final ParticleFx BRASS_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFFE296).endColor(0x00A06A1E)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(-0.015f).friction(0.92f).glow();
    private static final ParticleFx BLUE_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFBEE4FF).endColor(0x002A5AB0)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(-0.015f).friction(0.92f).glow();
    private static final ParticleFx GOLD_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFFF4D0).endColor(0x00FFC060)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(24, 12).gravity(-0.02f).friction(0.92f).glow();
    private static final ParticleFx STEAM = ParticleFx.of("fx/steam")
            .color(0xB0F4F6FA).endColor(0x00D8E0E8)
            .size(0.3f).endSize(0.9f).sizeRandom(0.3f)
            .lifetime(30, 14).gravity(-0.012f).friction(0.92f).spin(2f).randomRotation();
    private static final ParticleFx JET = STEAM.color(0xE0FFFFFF).endColor(0x00A0A8B0).size(0.2f).endSize(0.7f)
            .lifetime(14, 6).gravity(0.02f);
    private static final ParticleFx GEAR = ParticleFx.of("fx/gear")
            .color(0xFFE8B858).endColor(0x80A06A1E)
            .size(0.1f).endSize(0.08f).sizeRandom(0.4f)
            .lifetime(26, 10).gravity(0.04f).friction(0.94f).spin(14f).randomRotation().collide();
    private static final ParticleFx COG = ParticleFx.of("fx/cog")
            .color(0xFFD9A441).endColor(0x807A5018)
            .size(0.07f).endSize(0.06f).sizeRandom(0.4f)
            .lifetime(24, 10).gravity(0.04f).friction(0.94f).spin(-18f).randomRotation().collide();
    private static final ParticleFx FLOATING_GEAR = GEAR.gravity(-0.01f).color(0xFFFFE296).endColor(0x00D9A441).glow();
    private static final ParticleFx SPRING = ParticleFx.of("fx/spring")
            .color(0xFFC8D0DC).endColor(0x80788090)
            .size(0.07f).endSize(0.06f).sizeRandom(0.3f)
            .lifetime(24, 10).gravity(0.05f).friction(0.95f).spin(20f).randomRotation().collide();
    private static final ParticleFx SCREW = ParticleFx.of("fx/screw")
            .color(0xFFC8D0DC).endColor(0x80788090)
            .size(0.06f).endSize(0.05f).sizeRandom(0.3f)
            .lifetime(24, 10).gravity(0.05f).friction(0.95f).spin(20f).randomRotation().collide();
    private static final ParticleFx LENS = ParticleFx.of("fx/lens")
            .color(0xFFBEE4FF).endColor(0x004696F0)
            .size(0.12f).endSize(0.1f)
            .lifetime(26, 0).gravity(-0.01f).friction(0.9f).glow();
    private static final ParticleFx COMPASS = ParticleFx.of("fx/compass")
            .color(0xFFBEE4FF).endColor(0x004696F0)
            .size(0.1f).endSize(0.08f).sizeRandom(0.2f)
            .lifetime(22, 8).gravity(-0.005f).friction(0.9f).glow().spin(6f);
    private static final ParticleFx[] EQUATIONS = {
            equation("fx/equation_0"), equation("fx/equation_1"), equation("fx/equation_2")};
    private static final ParticleFx PAGE = ParticleFx.of("fx/page")
            .color(0xFFF3E6C4).endColor(0x00C8B080)
            .size(0.1f).endSize(0.08f).sizeRandom(0.3f)
            .lifetime(36, 14).gravity(0.008f).friction(0.94f).spin(10f).randomRotation();
    private static final ParticleFx TEXT = ParticleFx.of("fx/text_line")
            .color(0xFFF3E6C4).endColor(0x001B2A41)
            .size(0.09f).endSize(0.06f).sizeRandom(0.3f)
            .lifetime(22, 10).gravity(-0.01f).friction(0.9f).glow();
    private static final ParticleFx ACID_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFF0FF8A).endColor(0x0060800A)
            .size(0.07f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(18, 8).gravity(0.01f).friction(0.92f).glow();
    private static final ParticleFx ACID_DROP = ParticleFx.of("fx/droplet")
            .color(0xFFDCF050).endColor(0xA078A010)
            .size(0.06f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(22, 8).gravity(0.06f).friction(0.97f).glow().collide();
    private static final ParticleFx ACID_SMOKE = ParticleFx.of("fx/smoke")
            .color(0x90C8E63C).endColor(0x00607020)
            .size(0.3f).endSize(0.8f).sizeRandom(0.3f)
            .lifetime(26, 10).gravity(-0.012f).friction(0.9f).spin(2f).randomRotation();
    private static final ParticleFx GLASS = ParticleFx.of("fx/sliver")
            .color(0xE0F0FAFF).endColor(0x40A0C0E0)
            .size(0.06f).endSize(0.05f).sizeRandom(0.5f)
            .lifetime(20, 8).gravity(0.06f).friction(0.96f).spin(16f).randomRotation().collide();
    private static final ParticleFx CHALK = ParticleFx.of("fx/ash")
            .color(0xE0FFFFFF).endColor(0x00E8E0D0)
            .size(0.06f).endSize(0.04f).sizeRandom(0.5f)
            .lifetime(30, 10).gravity(-0.004f).friction(0.92f).randomRotation();
    private static final ParticleFx STAR_SPARK = ParticleFx.of("fx/star5")
            .color(0xFFFFFFFF).endColor(0x00FFE6A0)
            .size(0.08f).endSize(0.03f).sizeRandom(0.5f)
            .lifetime(18, 10).gravity(0.01f).friction(0.9f).glow().spin(8f).randomRotation();
    private static final ParticleFx SPARKLE = ParticleFx.of("fx/sparkle")
            .color(0xFFFFFFFF).endColor(0x00BEE4FF)
            .size(0.07f).endSize(0.02f).sizeRandom(0.5f)
            .lifetime(16, 8).gravity(-0.005f).friction(0.9f).glow().randomRotation();
    private static final ParticleFx SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFFFFFF).endColor(0x00FFC060)
            .size(0.06f).endSize(0.02f)
            .lifetime(8, 6).gravity(0.02f).friction(0.86f).glow();
    private static final ParticleFx NOTE = ParticleFx.of("fx/note")
            .color(0xFFFFECAA).endColor(0x00D9A441)
            .size(0.12f).endSize(0.1f).sizeRandom(0.3f)
            .lifetime(34, 10).gravity(-0.02f).friction(0.94f).glow();
    private static final ParticleFx CURSE_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFD0A8FF).endColor(0x00400860)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(0.01f).friction(0.9f).glow();
    private static final ParticleFx MIST = ParticleFx.of("fx/smoke")
            .color(0x70F0F4FA).endColor(0x00C0C8D0)
            .size(0.4f).endSize(1.1f).sizeRandom(0.4f)
            .lifetime(36, 14).gravity(-0.004f).friction(0.95f).spin(1f).randomRotation();
    private static final ParticleFx FRAGMENT = ParticleFx.of("fx/splinter")
            .color(0xE0F4F6FA).endColor(0x00A0A8B0)
            .size(0.07f).endSize(0.04f).sizeRandom(0.5f)
            .lifetime(22, 10).gravity(-0.01f).friction(0.94f).spin(10f).randomRotation();

    private static ParticleFx equation(String texture) {
        return ParticleFx.of(texture)
                .color(0xFFBEE4FF).endColor(0x002A5AB0)
                .size(0.09f).endSize(0.07f).sizeRandom(0.3f)
                .lifetime(26, 10).gravity(-0.01f).friction(0.9f).glow();
    }

    private static ParticleFx anyEquation() {
        return EQUATIONS[ThreadLocalRandom.current().nextInt(EQUATIONS.length)];
    }

    /** A few random scraps of mathematics bursting out of a point. */
    private static void equationBurst(ServerLevel sl, Vec3 at, int count, double minSpeed, double maxSpeed) {
        for (int i = 0; i < count; i++) ParticleShapes.burst(sl, anyEquation(), at, 1, minSpeed, maxSpeed);
    }

    /** The dust a doll of this material sheds. */
    private static ParticleFx dust(Material material) {
        return ParticleFx.of(material == Material.STRAW ? "fx/splinter" : material == Material.FROST ? "fx/flake" : "fx/ash")
                .color(withAlpha(material.color, 0xF0)).endColor(withAlpha(material.color, 0))
                .size(0.07f).endSize(0.05f).sizeRandom(0.5f)
                .lifetime(24, 10).gravity(0.03f).friction(0.94f).spin(8f).randomRotation().collide();
    }

    // =====================================================================================
    // Lines and arcs
    // =====================================================================================

    private static final SlashFx BLUEPRINT_LINE = SlashFx.line("slash/dashed_fine")
            .color(0xC04696F0).core(0xFFBEE4FF).width(0.06f).taper(SlashFx.Taper.UNIFORM).lifetime(12).sweep(3);
    private static final SlashFx HAIRLINE = SlashFx.line("slash/edge")
            .color(0xF0BEE4FF).core(0xFFFFFFFF).width(0.08f).taper(SlashFx.Taper.CRESCENT).lifetime(8).sweep(2);
    private static final SlashFx STAR_RAY = SlashFx.line("slash/smooth")
            .color(0xE0FFE6A0).core(0xFFFFFFFF).width(0.22f).taper(SlashFx.Taper.COMET).lifetime(8).sweep(2);
    private static final SlashFx TRACER = SlashFx.line("slash/smooth")
            .color(0xC0FFE296).core(0xFFFFFFFF).width(0.06f).taper(SlashFx.Taper.COMET).lifetime(5).sweep(1);
    private static final SlashFx DASH = SlashFx.line("slash/streaks")
            .color(0x90D9A441).core(0x80FFFFFF).width(0.3f).taper(SlashFx.Taper.COMET);
    private static final SlashFx MIST_LINE = SlashFx.line("slash/band")
            .color(0x90F0F4FA).core(0xC0FFFFFF).width(0.6f).taper(SlashFx.Taper.UNIFORM).lifetime(14).sweep(2)
            .translucent();
    private static final SlashFx FUSION_LINE = SlashFx.line("slash/band")
            .color(0xF0FFF4D0).core(0xFFFFFFFF).width(1.2f).taper(SlashFx.Taper.UNIFORM).lifetime(12).sweep(2);
    private static final SlashFx PAGE_TRAIL = SlashFx.line("slash/streaks")
            .color(0x90F3E6C4).core(0x60FFFFFF).width(0.5f).taper(SlashFx.Taper.COMET).lifetime(10).sweep(3);

    // =====================================================================================
    // Sounds
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

    /** The pitch (like an entity's: + = looking down) from one point to another. */
    private static float pitchToward(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double length = d.length();
        if (length < 1.0e-6) return 0f;
        return (float) -Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, d.y / length))));
    }

    private static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
    }

    private static float randomYaw(ServerLevel sl) {
        return sl.getRandom().nextFloat() * 360f;
    }

    // =====================================================================================
    // Statuses: a picture in the row above the hotbar stacks, and a clock beside the hotbar
    // =====================================================================================

    public enum Status {
        // what the Illuminator holds
        SURVEY("survey", "Survey", 0x4696F0),
        RITUAL("ritual", "Ritual Circle", 0xF3E6C4),
        BROOCH("brooch", "Mystic Brooch", 0x4696F0),
        MECHANICAL_BODY("mechanical_body", "Mechanical Body", 0xD9A441),
        COLOSSUS("colossus", "Colossus", 0xD9A441),
        HEAVEN("heaven", "Mechanical Heaven", 0xFFE296),
        FALSE_SKY("false_sky", "False Sky", 0xBEE4FF),
        MASTERWORK("masterwork", "Masterwork", 0xFFECAA),
        STEAM_FORM("steam_form", "Steam", 0xECF0F6),
        SPIRIT_FORM("spirit_form", "Knowledge Spirit", 0xF3E6C4),
        ENLIGHTENED("enlightened", "Enlightened", 0xFFECAA),
        WEIGHTLESS("weightless", "Weightless", 0xBEE4FF),
        // what is done to others (when they are players)
        APPRAISED("appraised", "Appraised", 0x4696F0),
        CORRODED("corroded", "Corroded", 0xC8E63C),
        CURSED_WEAK("cursed_weak", "Weakened", 0xD8C8FF),
        TRAUMA("trauma", "Trauma", 0xFF6A6A),
        MUTATION("mutation", "Mutation", 0x9ADC5A),
        PRESSED("pressed", "Pressed Down", 0xB078FF),
        LIFTED("lifted", "Weightless Ground", 0xBEE4FF),
        ERODED("eroded", "Eroded", 0xF3E6C4),
        MAZE("maze", "Lost in Thought", 0x4696F0),
        FORGOTTEN("forgotten", "Forgotten", 0xD84040),
        WRITTEN("written", "Written", 0x1B2A41),
        IMMERSED("immersed", "Immersed", 0xF3E6C4);

        final String image;
        final String label;
        final int rgb;

        Status(String image, String label, int rgb) {
            this.image = image;
            this.label = label;
            this.rgb = rgb;
        }

        String id() {
            return "il_status_" + name().toLowerCase();
        }
    }

    /** Shows the status's picture (in the row above the hotbar stacks, rows of nine) and a clock beside the hotbar. */
    public static void status(LivingEntity player, Status status, int ticks) {
        int slot = status.ordinal();
        if (player instanceof Player viewerOfHud) ScreenImages.show(viewerOfHud, status.id(), "illuminator/" + status.image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, ticks);
        if (player instanceof Player viewerOfHud) HudTimer.show(viewerOfHud, status.id(), status.label, ticks, status.rgb);
    }

    /** How long something "until ended" is shown at most (an hour): show it again after that. */
    public static final int FOREVER = 72000;

    /** Only the picture, no clock: for what lasts until it is ended (the Mechanical Body from Illuminator on). */
    public static void statusForever(LivingEntity player, Status status) {
        int slot = status.ordinal();
        if (player instanceof Player viewerOfHud) ScreenImages.show(viewerOfHud, status.id(), "illuminator/" + status.image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, FOREVER);
        if (player instanceof Player viewerOfHud) HudTimer.hide(viewerOfHud, status.id());
    }

    /** It ended early. */
    public static void statusEnd(LivingEntity player, Status status) {
        if (player instanceof Player viewerOfHud) ScreenImages.hide(viewerOfHud, status.id());
        if (player instanceof Player viewerOfHud) HudTimer.hide(viewerOfHud, status.id());
    }

    // =====================================================================================
    // The wheels' pictures (textures/gui/ability_menus/illuminator/, for AbilityWheel.entry's icon)
    // =====================================================================================

    public static final String WHEEL_CLOCKWORK_RIFLE = "ability_menus/illuminator/clockwork_rifle";
    public static final String WHEEL_SPIRIT_CANNON = "ability_menus/illuminator/spirit_cannon";
    public static final String WHEEL_MYSTIC_BROOCH = "ability_menus/illuminator/mystic_brooch";
    public static final String WHEEL_STAR_OF_CURSES = "ability_menus/illuminator/star_of_curses";
    public static final String WHEEL_BLESSING_OF_STARS = "ability_menus/illuminator/blessing_of_stars";
    public static final String WHEEL_FALSE_CONSTELLATIONS = "ability_menus/illuminator/false_constellations";
    public static final String WHEEL_METEOR_SHOWER = "ability_menus/illuminator/meteor_shower";
    public static final String WHEEL_LIVING_DOLL = "ability_menus/illuminator/living_doll";
    public static final String WHEEL_GOLEM = "ability_menus/illuminator/golem";
    public static final String WHEEL_MECHANICAL_BODY = "ability_menus/illuminator/mechanical_body";
    public static final String WHEEL_DISSOCIATION = "ability_menus/illuminator/dissociation";
    public static final String WHEEL_KNOWLEDGE_SPIRIT = "ability_menus/illuminator/knowledge_spirit";
    public static final String WHEEL_REWRITE_PRINCIPLES = "ability_menus/illuminator/rewrite_principles";
    public static final String WHEEL_COLOSSUS = "ability_menus/illuminator/colossus";
    public static final String WHEEL_SCROLLS_OF_CIVILIZATION = "ability_menus/illuminator/scrolls_of_civilization";
    public static final String WHEEL_MATHEMATICAL_MAZE = "ability_menus/illuminator/mathematical_maze";
    public static final String WHEEL_MASTERWORK = "ability_menus/illuminator/masterwork";
    public static final String WHEEL_REALITY_KNOWLEDGE = "ability_menus/illuminator/reality_knowledge";
    public static final String WHEEL_ILLUSORY_BOOK = "ability_menus/illuminator/illusory_book";
    public static final String WHEEL_MECHANICAL_HEAVEN = "ability_menus/illuminator/mechanical_heaven";

    // =====================================================================================
    // The models (models/model_fx/illuminator/, see each JSON's "credit")
    // =====================================================================================

    /** The Compound Flask, 0.85 blocks; pivot the middle of its belly. */
    public static final ModelFx FLASK = ModelFx.of("illuminator/flask").pivot(8, 5, 8).glow().seeThrough();
    /** The Clockwork Rifle, 2.3 blocks at scale 1; pivot the grip, muzzle north. */
    public static final ModelFx RIFLE = ModelFx.of("illuminator/clockwork_rifle").pivot(8, 6, 13).glow();
    /** A rifle round with its streak; top = its front. For a Shot. */
    public static final ModelFx ROUND = ModelFx.of("illuminator/rifle_round").pivot(8, 8, 8).glow().unshaded();
    /** The Spirit Cannon's carriage, and its barrel (pivot the trunnions, aim it with pitch and yaw). */
    public static final ModelFx CANNON = ModelFx.of("illuminator/spirit_cannon").glow();
    public static final ModelFx CANNON_BARREL = ModelFx.of("illuminator/spirit_cannon_barrel").pivot(8, 10, 9).glow();
    /** A cannonball, 0.4 blocks; for a Shot. */
    public static final ModelFx CANNONBALL = ModelFx.of("illuminator/cannonball").pivot(8, 8, 8).glow();
    /** The Mystic Brooch, 0.5 blocks, standing; pivot its middle. */
    public static final ModelFx BROOCH = ModelFx.of("illuminator/mystic_brooch").pivot(8, 8, 8).glow();
    /** A soft ball of light (the shared "orb"): the Brooch's ward. White, tint it. */
    private static final ModelFx WARD = ModelFx.of("orb").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** The Golem, 3 blocks, three steps (standing, drawn back, striking); pivot between its feet. */
    public static final ModelFx GOLEM = ModelFx.of("illuminator/golem").frames(3).smooth().pivot(8, -16, 8).glow();
    /** The Mechanical Body: the plates, the goggles (on the head) and the arm cannon. */
    public static final ModelFx MECH_BODY = ModelFx.of("illuminator/mechanical_body").glow();
    public static final ModelFx GOGGLES = ModelFx.of("illuminator/mechanical_goggles").glow();
    public static final ModelFx ARM_CANNON = ModelFx.of("illuminator/arm_cannon").glow();
    /** The Colossus, 3 blocks at scale 1 (play it at 2), three steps (standing, a stride, firing). */
    public static final ModelFx COLOSSUS = ModelFx.of("illuminator/colossus").frames(3).smooth().pivot(8, -16, 8).glow();
    /** A brass gear, 1 block, lying flat; pivot its hub. */
    public static final ModelFx GEAR_MODEL = ModelFx.of("illuminator/gear").pivot(8, 8, 8).glow();
    /** A rolled scroll of civilization, 1 block, lying east-west; pivot its middle. */
    public static final ModelFx SCROLL = ModelFx.of("illuminator/scroll").pivot(8, 8, 8).glow();
    /** Masterwork's baton, 0.8 blocks, tip up; pivot the grip. */
    public static final ModelFx BATON = ModelFx.of("illuminator/baton").pivot(8, 2, 8).glow();
    /** A meteor of starlight; top = its front, pivot the core. */
    public static final ModelFx METEOR = ModelFx.of("illuminator/star_meteor").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** The Illusory Book, three steps (shut, opening, wide open); pivot the bottom of the spine. */
    public static final ModelFx BOOK = ModelFx.of("illuminator/illusory_book").frames(3).smooth().glow();
    /** Standing pictures, 1 block; pivot their middle. White unless said. */
    public static final ModelFx FLAW_MARK = ModelFx.of("illuminator/flaw_mark").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx ERODED_MARK = ModelFx.of("illuminator/eroded_mark").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx REJECTION_SEAL = ModelFx.of("illuminator/rejection_seal").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx RECALL_CLOCK = ModelFx.of("illuminator/recall_clock").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx EMBLEM = ModelFx.of("illuminator/illuminator_emblem").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** Flat pictures, 1 block; pivot their middle. White unless said. */
    public static final ModelFx BLUEPRINT_CIRCLE = ModelFx.of("illuminator/blueprint_circle").glow().unshaded().seeThrough();
    public static final ModelFx CHALK_CIRCLE = ModelFx.of("illuminator/chalk_circle").glow().unshaded().seeThrough();
    public static final ModelFx BLUEPRINT_SPIRAL = ModelFx.of("illuminator/blueprint_spiral").glow().unshaded().seeThrough();
    public static final ModelFx STAR_CHART = ModelFx.of("illuminator/star_chart").glow().unshaded().seeThrough();
    public static final ModelFx PRINCIPLE_CIRCLE = ModelFx.of("illuminator/principle_circle").glow().unshaded().seeThrough();
    public static final ModelFx SYMPHONY_RING = ModelFx.of("illuminator/symphony_ring").glow().unshaded().seeThrough();
    /** Its own colors: the acid's puddle, the fake night sky, the sky of gears. */
    public static final ModelFx ACID_POOL = ModelFx.of("illuminator/acid_pool").glow().unshaded().seeThrough();
    public static final ModelFx FALSE_SKY = ModelFx.of("illuminator/false_sky").glow().unshaded().seeThrough();
    public static final ModelFx GEAR_SKY = ModelFx.of("illuminator/gear_sky").glow().unshaded().seeThrough();
    /** The Mathematical Maze's lattice cube, 1 block; pivot the middle of its floor. */
    public static final ModelFx LATTICE = ModelFx.of("illuminator/equation_lattice").glow().unshaded().seeThrough();
    /** Beams: two crossed planes, 1 block long; pivot the middle of their start. Lay them with modelAlong. */
    public static final ModelFx MIST_BEAM = ModelFx.of("illuminator/mist_beam").glow().unshaded().seeThrough();
    public static final ModelFx FUSION_BEAM = ModelFx.of("illuminator/fusion_beam").glow().unshaded().seeThrough();
    /** The four murals of Scrolls of Civilization (their own colors); pivot the middle of their bottom edge. */
    public static final ModelFx[] MURALS = {
            ModelFx.of("illuminator/mural_0").glow().seeThrough(), ModelFx.of("illuminator/mural_1").glow().seeThrough(),
            ModelFx.of("illuminator/mural_2").glow().seeThrough(), ModelFx.of("illuminator/mural_3").glow().seeThrough()};

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

    // =====================================================================================
    // The Appraised mark (docs/ILLUMINATOR.md, "The pathway mechanic")
    // =====================================================================================

    /** The lens of the Appraised over the target's head for 'ticks', showing what it is weak to. */
    public static void appraisedMark(ServerLevel sl, LivingEntity target, Weakness weakness, int ticks, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        if (ticks <= 0) return;
        ModelFx lens = weakness.mark.scale(0.55f).lifetime(ticks).fade(4, 10).spin(2f).tag(tag)
                .key(0, pose().scale(1.8f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, lens, target, new Vec3(0, target.getBbHeight() + 0.6, 0), 0f, 0f, 0f);
    }

    /** The mark is gone (spent, faded, wiped): the lens closes in a flicker of equations. */
    public static void appraisedEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        equationBurst(sl, headOf(target).add(0, 0.5, 0), 4, 0.02, 0.05);
    }

    // =====================================================================================
    // Key 1: Compound Flask / Recall (Savant)
    // =====================================================================================

    /** The flask for the Shot: small, tumbling, its acid glowing. Give it the flight's lifetime. */
    public static final ModelFx FLASK_FLYING = FLASK.scale(0.55f).spin(24f).lifetime(40).fade(0, 2);

    /** The flask leaves the hand. */
    public static void flaskThrow(ServerLevel sl, LivingEntity player) {
        Vec3 hand = handOf(player, sl);
        ParticleShapes.burst(sl, ACID_MOTE, hand, 4, 0.01, 0.04);
        ParticleShapes.slashBetween(sl, DASH.width(0.2f).lifetime(5).sweep(2), hand, hand.add(player.getLookAngle().scale(1.2)));
        sound(sl, player.position(), ModSounds.ILLUMINATOR_FLASK_THROW, 0.8f, wobble(1.0f));
    }

    /** Every tick the flask flies: a drop of glowing acid behind it. */
    public static void flaskTrail(ServerLevel sl, Vec3 at) {
        ParticleShapes.spawn(sl, ACID_MOTE, at, Vec3.ZERO);
        if (sl.getRandom().nextInt(3) == 0) ParticleShapes.spawn(sl, ACID_DROP, at, new Vec3(0, -0.02, 0));
    }

    /** The flask bursts: glass, a flash of the explosion, acid splashing out to 'radius'. */
    public static void flaskBurst(ServerLevel sl, Vec3 at, double radius) {
        SpellFx.blast(sl, at, radius, 0xFFDCF050, ACID_MOTE);
        ParticleShapes.burst(sl, GLASS, at, 14, 0.08, 0.28);
        ParticleShapes.burst(sl, ACID_DROP, at, 18, 0.08, 0.3);
        ParticleShapes.burst(sl, ACID_SMOKE, at, 6, 0.02, 0.06);
        SpellFx.shockRing(sl, at.add(0, -0.4, 0), radius, 0xD0C8E63C, 8, 0);
        sound(sl, at, ModSounds.ILLUMINATOR_FLASK_BURST, 1.2f, wobble(1.0f));
    }

    /** Corroded: a green circle under the target and acid dripping off it for 'ticks'. */
    public static void corroded(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), 0x90C8E63C, ticks, tag);
        for (int t = 0; t < ticks; t += 10) {
            Later.run(sl, t, () -> {
                if (!target.isAlive()) return;
                ParticleShapes.burst(sl, ACID_DROP, chestOf(target), 2, 0.01, 0.03);
                ParticleShapes.spawn(sl, ACID_SMOKE, chestOf(target), new Vec3(0, 0.02, 0));
            });
        }
    }

    public static void corrodedEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
    }

    /** Arcane Scholar: the burst leaves a puddle of acid 'radius' wide for 'ticks', bubbling and hissing. */
    public static void acidPuddle(ServerLevel sl, Vec3 ground, double radius, int ticks, String tag) {
        floor(sl, ACID_POOL, ground, radius, 0xFFFFFFFF, ticks, 0.3f, tag, 0);
        sound(sl, ground, ModSounds.ILLUMINATOR_ACID_HISS, 0.8f, wobble(1.0f));
        for (int t = 10; t < ticks; t += 10) {
            Later.run(sl, t, () -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int i = 0; i < 3; i++) {
                    double a = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
                    Vec3 at = ground.add(Math.cos(a) * far, 0.1, Math.sin(a) * far);
                    ParticleShapes.spawn(sl, i == 0 ? ACID_SMOKE : ACID_MOTE, at, new Vec3(0, 0.03, 0));
                }
            });
        }
    }

    public static void acidPuddleEnd(ServerLevel sl, Vec3 ground, String tag) {
        ParticleShapes.clearModels(sl, ground, tag);
        ParticleShapes.burst(sl, ACID_SMOKE, ground.add(0, 0.2, 0), 4, 0.01, 0.03);
    }

    // ---------- sneak: Recall

    /**
     * Someone steps back to where they stood a few seconds ago ('who' is the Savant, or from Illuminator the ally
     * sent back): a clock turning backward where they were, pages streaming back along the way, the clock again where
     * they arrive. Show it as the move happens.
     */
    public static void recall(ServerLevel sl, LivingEntity who, Vec3 from, Vec3 to) {
        float yaw = who.getYRot();
        flash(sl, RECALL_CLOCK, from.add(0, who.getBbHeight() * 0.6, 0), yaw + 180f, 0xE0FFE296, 1.0f, 16);
        Vec3 a = from.add(0, who.getBbHeight() * 0.55, 0);
        Vec3 b = to.add(0, who.getBbHeight() * 0.55, 0);
        ParticleShapes.slashBetween(sl, PAGE_TRAIL, a, b);
        ParticleShapes.line(sl, PAGE, a, b, 2.5);
        ParticleShapes.burst(sl, PAGE, a, 8, 0.03, 0.1);
        Later.run(sl, 2, () -> {
            flash(sl, RECALL_CLOCK, b.add(0, 0.4, 0), yaw + 180f, 0xF0FFE296, 1.2f, 18);
            SpellFx.closingRing(sl, to, 1.6, 0xC0D9A441, 8, 0);
            ParticleShapes.burst(sl, BRASS_MOTE, b, 10, 0.03, 0.08);
        });
        sound(sl, to, ModSounds.ILLUMINATOR_RECALL, 1.0f, wobble(1.0f));
    }

    // =====================================================================================
    // Key 2: Survey / Ritual Circle (Archaeologist)
    // =====================================================================================

    /** The tick the survey's pulse reaches its edge. */
    public static final int SURVEY_PULSE = 14;

    /** A pulse of blueprint light spreads 'reach' blocks from the Archaeologist; a lens flashes before their eyes. */
    public static void survey(ServerLevel sl, LivingEntity player, double reach) {
        float edge = (float) Math.min(reach, 48.0) * 2f;
        ModelFx pulse = BLUEPRINT_CIRCLE.color(0xC04696F0).lifetime(SURVEY_PULSE + 8).fade(0, 10).spin(1f)
                .key(0, pose().scale(1f))
                .key(SURVEY_PULSE, pose().scale(edge), Ease.OUT);
        ParticleShapes.model(sl, pulse, player.position().add(0, 0.06, 0), randomYaw(sl), 0f, 0f);
        floor(sl, BLUEPRINT_CIRCLE, player.position(), 1.6, 0xE0BEE4FF, 30, 2f, null, 0);
        SpellFx.steadyRing(sl, player.position(), Math.min(reach, 32.0), 0xA04696F0, SURVEY_PULSE, 0);
        ParticleShapes.spawn(sl, LENS, player.getEyePosition().add(Spells.flatLook(player).scale(0.5)), Vec3.ZERO);
        ParticleShapes.ring(sl, COMPASS, player.position().add(0, 0.3, 0), 1.2, 6, 0.04);
        sound(sl, player.position(), ModSounds.ILLUMINATOR_SURVEY, 1.2f, 1.0f);
    }

    /** One thing the survey found (its outline is the ability's glowing): a lens and a few equations over it. */
    public static void surveyed(ServerLevel sl, LivingEntity target) {
        ParticleShapes.spawn(sl, LENS, headOf(target).add(0, 0.5, 0), Vec3.ZERO);
        equationBurst(sl, chestOf(target), 3, 0.02, 0.05);
    }

    /** Someone hidden is forced out of hiding: blueprint lines close in round them. */
    public static void surveyForcedOut(ServerLevel sl, LivingEntity hidden) {
        SpellFx.closingRing(sl, hidden.position(), 2.2, 0xE04696F0, 8, 0);
        flash(sl, FLAW_MARK, chestOf(hidden), randomYaw(sl), 0xE0BEE4FF, 1.0f, 14);
        ParticleShapes.burst(sl, BLUE_MOTE, chestOf(hidden), 12, 0.04, 0.12);
        sound(sl, hidden.position(), ModSounds.ILLUMINATOR_ANALYSIS, 0.8f, 1.2f);
    }

    /**
     * Arcane Scholar (Environment Controller): where a Beyonder ability was cast, a faint column of equations in the
     * caster's pathway color, seen by the Illuminator alone. Show it again every second or two while it is wanted.
     */
    public static void surveyResidue(ServerPlayer viewer, Vec3 at, int color) {
        for (int i = 0; i < 6; i++) {
            ParticleFx scrap = EQUATIONS[i % EQUATIONS.length].color(withAlpha(color, 0xE0)).endColor(withAlpha(color, 0));
            ParticleShapes.spawnFor(viewer, scrap, at.add(0, 0.2 + i * 0.35, 0), new Vec3(0, 0.01, 0));
        }
    }

    // ---------- sneak: Ritual Circle

    /** The tick the chalk circle is drawn whole. */
    public static final int RITUAL_DRAWN = 16;

    /** A chalk circle 'radius' wide is drawn round the Archaeologist's feet and stays for 'ticks'. */
    public static void ritualCircle(ServerLevel sl, LivingEntity player, Vec3 center, double radius, int ticks, String tag) {
        ModelFx chalk = CHALK_CIRCLE.color(0xF0FFFAF0).scale((float) (radius * 2.0)).lifetime(ticks).fade(0, 12)
                .spin(0.3f).tag(tag)
                .key(0, pose().scale(0.05f).alpha(0.3f))
                .key(RITUAL_DRAWN, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, chalk, center.add(0, 0.05, 0), randomYaw(sl), 0f, 0f);
        float start = randomYaw(sl);
        for (int i = 0; i < RITUAL_DRAWN; i++) {                              // the chalk going round
            int step = i;
            Later.run(sl, i, () -> {
                double a = Math.toRadians(start + step * 360.0 / RITUAL_DRAWN);
                Vec3 at = center.add(Math.cos(a) * radius, 0.1, Math.sin(a) * radius);
                ParticleShapes.burst(sl, CHALK, at, 3, 0.005, 0.02);
                if (step % 4 == 0) ParticleShapes.spawn(sl, COMPASS, at.add(0, 0.4, 0), Vec3.ZERO);
            });
        }
        sound(sl, center, ModSounds.ILLUMINATOR_RITUAL_CIRCLE, 1.0f, 1.0f);
    }

    /** Every 3 seconds while the circle lies: a soft ring of chalk-light rises off it. */
    public static void ritualPulse(ServerLevel sl, Vec3 center, double radius) {
        SpellFx.risingRings(sl, center, radius * 0.9, 1.4, 2, 0x70FFFAF0);
        ParticleShapes.ring(sl, CHALK, center.add(0, 0.15, 0), radius, 12, 0.0);
        sound(sl, center, ModSounds.ILLUMINATOR_RITUAL_PULSE, 0.5f, wobble(1.0f));
    }

    /** An ally inside is healed and cleansed: a few sparks of chalk-light. */
    public static void ritualCleansed(ServerLevel sl, LivingEntity ally) {
        ParticleShapes.burst(sl, SPARKLE, chestOf(ally), 6, 0.02, 0.06);
        ParticleShapes.burst(sl, CHALK, ally.position().add(0, 0.2, 0), 4, 0.01, 0.03);
    }

    public static void ritualEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.ring(sl, CHALK, center.add(0, 0.2, 0), radius, 16, 0.03);
    }

    // =====================================================================================
    // Key 3: Appraise / Exploit Flaw (Appraiser)
    // =====================================================================================

    /** The tick the Appraised mark settles over the target. */
    public static final int APPRAISE_LANDS = 5;

    /** A dashed blueprint line from the eye to the target, a lens flash, then the mark over its head for 'ticks'. */
    public static void appraise(ServerLevel sl, LivingEntity player, LivingEntity target, Weakness weakness, int ticks, String tag) {
        Vec3 eye = player.getEyePosition().add(Spells.flatLook(player).scale(0.4));
        ParticleShapes.slashBetween(sl, BLUEPRINT_LINE, eye, chestOf(target));
        ParticleShapes.spawn(sl, LENS, eye, Vec3.ZERO);
        sound(sl, player.position(), ModSounds.ILLUMINATOR_APPRAISE, 0.9f, wobble(1.0f));
        Later.run(sl, APPRAISE_LANDS, () -> {
            if (!target.isAlive()) return;
            appraisedMark(sl, target, weakness, ticks - APPRAISE_LANDS, tag);
            equationBurst(sl, chestOf(target), 6, 0.03, 0.08);
            ParticleShapes.burst(sl, BLUE_MOTE, chestOf(target), 6, 0.02, 0.06);
        });
    }

    // ---------- sneak: Exploit Flaw

    /** The tick the flaw is struck: the damage goes there. */
    public static final int FLAW_LANDS = 4;

    /** A hairline of blueprint light finds the flaw, then it gives: the crack flashes on the body. */
    public static void exploitFlaw(ServerLevel sl, LivingEntity player, LivingEntity target) {
        Vec3 eye = player.getEyePosition().add(Spells.flatLook(player).scale(0.4));
        ParticleShapes.slashBetween(sl, HAIRLINE, eye, chestOf(target));
        sound(sl, player.position(), ModSounds.ILLUMINATOR_EXPLOIT_FLAW, 1.0f, wobble(1.0f));
        Later.run(sl, FLAW_LANDS, () -> {
            if (!target.isAlive()) return;
            Vec3 at = chestOf(target);
            flash(sl, FLAW_MARK, at, yawToward(at, player.position()), 0xF0BEE4FF, 1.2f, 14);
            ParticleShapes.burst(sl, SPARKLE, at, 12, 0.06, 0.2);
            equationBurst(sl, at, 5, 0.05, 0.12);
            SpellFx.shockRing(sl, target.position(), 1.6, 0xC04696F0, 6, 0);
        });
    }

    /** Illuminator: every Appraised enemy at once, a hairline to each. */
    public static void exploitAll(ServerLevel sl, LivingEntity player, List<LivingEntity> targets) {
        for (LivingEntity target : targets) exploitFlaw(sl, player, target);
    }

    /** No mark on it: the hairline flickers and finds nothing (the message is the ability's). */
    public static void noFlaw(ServerLevel sl, LivingEntity player) {
        ParticleShapes.burst(sl, EQUATIONS[0], player.getEyePosition().add(Spells.flatLook(player).scale(0.6)), 3, 0.01, 0.03);
        sound(sl, player.position(), ModSounds.ILLUMINATOR_APPRAISE, 0.4f, 0.7f);
    }

    /** A construct, doll or golem taken apart: springs, screws and gears burst out of it (destroyed = all of it). */
    public static void constructTakenApart(ServerLevel sl, LivingEntity construct, boolean destroyed) {
        Vec3 at = chestOf(construct);
        int n = destroyed ? 3 : 1;
        ParticleShapes.burst(sl, GEAR, at, 5 * n, 0.08, 0.25);
        ParticleShapes.burst(sl, COG, at, 4 * n, 0.08, 0.25);
        ParticleShapes.burst(sl, SPRING, at, 3 * n, 0.08, 0.22);
        ParticleShapes.burst(sl, SCREW, at, 3 * n, 0.08, 0.22);
        ParticleShapes.burst(sl, STEAM, at, 2 * n, 0.02, 0.06);
        sound(sl, at, ModSounds.ILLUMINATOR_CONSTRUCT_BREAK, destroyed ? 1.2f : 0.7f, wobble(1.0f));
    }

    // =====================================================================================
    // Key 4: Workshop (Artisan): Clockwork Rifle, Spirit Cannon, Mystic Brooch
    // =====================================================================================

    /** The tick the first round leaves the rifle, and how many ticks apart the rounds are. */
    public static final int RIFLE_FIRES = 6;
    public static final int RIFLE_STEP = 2;

    /**
     * The brass rifle appears in the hands (or, armCannon = true, the Mechanical Body's arm cannon is raised: no
     * winding), is wound, and fades after 'rounds' rounds. The rounds themselves: rifleShot at RIFLE_FIRES + i * STEP.
     */
    public static void rifle(ServerLevel sl, LivingEntity player, int rounds, boolean armCannon) {
        int last = (armCannon ? 2 : RIFLE_FIRES) + Math.max(0, rounds - 1) * RIFLE_STEP;
        if (!armCannon) {
            ModelFx gun = RIFLE.scale(0.55f).lifetime(last + 10).fade(3, 8)
                    .key(0, pose().right(0.38f).up(-0.2f).forward(0.2f).pitch(20f).scale(0.4f).alpha(0f))
                    .key(RIFLE_FIRES - 1, pose().right(0.38f).up(0f).forward(0.35f).pitch(0f).scale(1f).alpha(1f), Ease.OUT_BACK);
            for (int i = 0; i < rounds; i++) {                                  // a kick at every round
                int at = RIFLE_FIRES + i * RIFLE_STEP;
                gun = gun.during(at, at, pose().forward(0.22f)).during(at + 1, at + 1, pose().forward(0.35f));
            }
            ParticleShapes.modelOnTurning(sl, gun, player, new Vec3(0, player.getEyeHeight() - 0.35, 0), 0f, 0f, 0f);
            sound(sl, player.position(), ModSounds.ILLUMINATOR_RIFLE_WIND, 0.8f, wobble(1.0f));
            for (int i = 0; i < 3; i++) {
                Later.run(sl, i * 2, () -> {
                    if (player.isAlive()) ParticleShapes.spawn(sl, COG, handOf(player, sl), new Vec3(0, 0.03, 0));
                });
            }
        }
    }

    /** One round: the muzzle flash, a brass tracer to 'to', and a hit there. weakness: the mark it carries (or null). */
    public static void rifleShot(ServerLevel sl, LivingEntity player, Vec3 from, Vec3 to, Weakness weakness) {
        ParticleShapes.slashBetween(sl, TRACER.color(weakness == null ? 0xC0FFE296 : withAlpha(weakness.color, 0xC0)), from, to);
        ParticleShapes.burst(sl, SPARK, from, 5, 0.04, 0.12);
        ParticleShapes.spawn(sl, STEAM.size(0.15f).endSize(0.4f), from, Spells.flatLook(player).scale(0.03));
        sound(sl, from, ModSounds.ILLUMINATOR_RIFLE_SHOT, 1.0f, wobble(1.0f));
        rifleHit(sl, to, weakness);
    }

    /** Where a round lands: brass sparks, and a flicker of its weakness's color. */
    public static void rifleHit(ServerLevel sl, Vec3 at, Weakness weakness) {
        ParticleShapes.burst(sl, SPARK, at, 6, 0.05, 0.16);
        if (weakness != null) {
            ParticleShapes.burst(sl, BLUE_MOTE.color(weakness.color).fadeOut(), at, 5, 0.03, 0.08);
        }
    }

    // ---------- Spirit Cannon

    /** The tick the cannon has unfolded and can fire. */
    public static final int CANNON_READY = 16;

    /** Where the barrel turns, from the spot the cannon stands on. */
    private static Vec3 trunnion(Vec3 at) {
        return at.add(0, 10 / 16.0, 0);
    }

    /** The cannon unfolds on 'at' facing 'yaw' and stays for 'ticks' (carriage and barrel, both under 'tag'). */
    public static void spiritCannon(ServerLevel sl, Vec3 at, float yaw, int ticks, String tag) {
        ModelFx carriage = CANNON.lifetime(ticks).fade(0, 10).tag(tag)
                .key(0, pose().up(-0.5f).scale(0.3f).alpha(0f))
                .key(CANNON_READY - 4, pose().up(0f).scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, carriage, at, yaw, 0f, 0f);
        ModelFx barrel = CANNON_BARREL.lifetime(ticks).fade(0, 10).tag(tag + "_barrel")
                .key(0, pose().scale(0.2f).pitch(40f).alpha(0f))
                .key(CANNON_READY - 6, pose().scale(0.2f).alpha(0f))
                .key(CANNON_READY, pose().scale(1f).pitch(0f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, barrel, trunnion(at), yaw, 0f, 0f);
        ParticleShapes.burst(sl, COG, at.add(0, 0.5, 0), 8, 0.04, 0.12);
        Later.run(sl, CANNON_READY - 2, () -> ParticleShapes.burst(sl, STEAM, at.add(ahead(yaw).scale(-1.0)).add(0, 0.9, 0), 4, 0.01, 0.04));
        sound(sl, at, ModSounds.ILLUMINATOR_CANNON_UNFOLD, 1.0f, 1.0f);
    }

    /** Turns the barrel toward a target (call before cannonFire, or every few ticks to track). */
    public static void cannonAim(ServerLevel sl, Vec3 at, String tag, Vec3 target) {
        Vec3 pivot = trunnion(at);
        ParticleShapes.moveModel(sl, tag + "_barrel", pivot, yawToward(pivot, target), pitchToward(pivot, target), 0f);
    }

    /** The cannon fires at 'target': flash, smoke and steam at the muzzle. Returns where the ball leaves (for the Shot). */
    public static Vec3 cannonFire(ServerLevel sl, Vec3 at, String tag, Vec3 target) {
        cannonAim(sl, at, tag, target);
        Vec3 pivot = trunnion(at);
        Vec3 muzzle = pivot.add(target.subtract(pivot).normalize().scale(1.45));
        SpellFx.blast(sl, muzzle, 0.6, 0xFFFFE296, SPARK);
        ParticleShapes.burst(sl, STEAM, muzzle, 5, 0.03, 0.1);
        ParticleShapes.burst(sl, STEAM, at.add(0, 0.7, 0), 3, 0.01, 0.03);
        sound(sl, muzzle, ModSounds.ILLUMINATOR_CANNON_FIRE, 1.4f, wobble(1.0f));
        return muzzle;
    }

    /** A cannonball bursts: a blast 'radius' wide with a flash of its blue rune. */
    public static void cannonBlast(ServerLevel sl, Vec3 at, double radius) {
        SpellFx.blast(sl, at, radius, 0xFF6AAAF0, BLUE_MOTE);
        ParticleShapes.burst(sl, SPARK, at, 12, 0.1, 0.3);
        ParticleShapes.burst(sl, STEAM, at, 4, 0.02, 0.06);
        sound(sl, at, ModSounds.ILLUMINATOR_CANNON_BLAST, 1.3f, wobble(1.0f));
    }

    /** Every tick a cannonball flies: a little blue light behind it. */
    public static void cannonballTrail(ServerLevel sl, Vec3 at) {
        ParticleShapes.spawn(sl, BLUE_MOTE, at, Vec3.ZERO);
    }

    /** The cannon folds away (its time ran out, or a new one replaced it). */
    public static void cannonEnd(ServerLevel sl, Vec3 at, String tag) {
        ParticleShapes.clearModels(sl, at, tag);
        ParticleShapes.clearModels(sl, at, tag + "_barrel");
        ParticleShapes.burst(sl, COG, at.add(0, 0.5, 0), 6, 0.04, 0.1);
        ParticleShapes.burst(sl, STEAM, at.add(0, 0.5, 0), 4, 0.02, 0.05);
    }

    // ---------- Mystic Brooch

    /** The brooch pins itself to the wearer's chest for 'ticks', a faint blue ward round them. */
    public static void mysticBrooch(ServerLevel sl, LivingEntity wearer, int ticks, String tag) {
        ParticleShapes.clearModels(sl, wearer, tag);
        ParticleShapes.clearModels(sl, wearer, tag + "_ward");
        double h = wearer.getBbHeight();
        float front = (float) (wearer.getBbWidth() * 0.55);                  // just in front of the chest
        ModelFx pin = BROOCH.scale(0.35f).lifetime(ticks).fade(2, 8).tag(tag)
                .key(0, pose().forward(front + 1.2f).scale(0.2f).spin(0f))
                .key(8, pose().forward(front).scale(1f).spin(360f), Ease.OUT);
        ParticleShapes.modelOnTurning(sl, pin, wearer, new Vec3(0, h * 0.72, 0), 0f, 0f, 0f);
        ModelFx ward = WARD.color(0x284696F0).scale((float) Math.max(1.4, h * 1.1)).lifetime(ticks).fade(8, 10)
                .tag(tag + "_ward");
        ParticleShapes.modelOn(sl, ward, wearer, new Vec3(0, h * 0.5, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, BLUE_MOTE, chestOf(wearer), 8, 0.02, 0.06);
        sound(sl, wearer.position(), ModSounds.ILLUMINATOR_BROOCH_PIN, 0.9f, wobble(1.0f));
    }

    /** A blow soaked up by the brooch: the ward flickers. */
    public static void broochAbsorb(ServerLevel sl, LivingEntity wearer) {
        ParticleShapes.sphere(sl, BLUE_MOTE, chestOf(wearer), Math.max(0.7, wearer.getBbHeight() * 0.55), 10);
    }

    /** Used up or run out: the brooch bursts, shoving enemies within 'radius'. */
    public static void broochBurst(ServerLevel sl, LivingEntity wearer, double radius, String tag) {
        ParticleShapes.clearModels(sl, wearer, tag);
        ParticleShapes.clearModels(sl, wearer, tag + "_ward");
        Vec3 at = chestOf(wearer);
        SpellFx.shockRing(sl, wearer.position(), radius, 0xE04696F0, 8, 0);
        ParticleShapes.burst(sl, GLASS.color(0xE0BEE4FF).fadeOut(), at, 14, 0.08, 0.25);
        ParticleShapes.burst(sl, BLUE_MOTE, at, 12, 0.06, 0.2);
        sound(sl, at, ModSounds.ILLUMINATOR_BROOCH_BURST, 1.0f, wobble(1.0f));
    }

    // =====================================================================================
    // Key 5: Astronomy (Astronomer): Star of Curses, Blessing of Stars, False Constellations, Meteor Shower
    // =====================================================================================

    /** The tick the ray of starlight reaches the target. */
    public static final int CURSE_LANDS = 3;

    /**
     * A ray of condensed starlight from the hand into the target. curse = the step of the ladder it now puts on (null:
     * only the ray, the first time).
     */
    public static void starOfCurses(ServerLevel sl, LivingEntity player, LivingEntity target, Curse curse) {
        Vec3 hand = handOf(player, sl);
        ParticleShapes.slashBetween(sl, STAR_RAY, hand, chestOf(target));
        ParticleShapes.burst(sl, STAR_SPARK, hand, 6, 0.03, 0.1);
        sound(sl, player.position(), ModSounds.ILLUMINATOR_STAR_CURSE, 1.0f, wobble(1.0f));
        Later.run(sl, CURSE_LANDS, () -> {
            if (!target.isAlive()) return;
            Vec3 at = chestOf(target);
            ParticleShapes.burst(sl, STAR_SPARK, at, 10, 0.05, 0.16);
            ParticleShapes.burst(sl, curse == null ? GOLD_MOTE : CURSE_MOTE.color(curse.color).fadeOut(), at, 8, 0.03, 0.1);
            if (curse != null) sound(sl, at, ModSounds.ILLUMINATOR_CURSE_STEP, 0.8f, 0.9f + curse.ordinal() * 0.1f);
        });
    }

    /** The curse's mark over the head for 'ticks' (one at a time: a new step replaces the old under the same tag). */
    public static void cursed(ServerLevel sl, LivingEntity target, Curse curse, int ticks, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        if (ticks <= 0) return;
        ModelFx mark = curse.mark.color(withAlpha(curse.color, 0xE0)).scale(0.5f).lifetime(ticks).fade(4, 10).spin(-2f)
                .tag(tag)
                .key(0, pose().scale(1.8f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 1.1, 0), 0f, 0f, 0f);
        if (curse == Curse.TRAUMA) {
            for (int t = 10; t < ticks; t += 10) {
                Later.run(sl, t, () -> {
                    if (target.isAlive()) ParticleShapes.burst(sl, CURSE_MOTE.color(0xFFFF4040).fadeOut().gravity(0.05f), chestOf(target), 3, 0.01, 0.03);
                });
            }
        }
    }

    public static void curseEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, CURSE_MOTE, headOf(target).add(0, 0.6, 0), 6, 0.02, 0.05);
    }

    // ---------- Blessing of Stars

    /** The tick the starlight has gathered and the gravity turns. */
    public static final int BLESSING_RISEN = 12;

    /** Starlight gathers over 'center': a star chart opens on the ground, 'radius' wide, for 'ticks'. */
    public static void blessingOfStars(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        floor(sl, STAR_CHART, center, radius, 0xC0FFECAA, ticks, 0.6f, tag, 0);
        ParticleShapes.spiral(sl, STAR_SPARK, center.add(0, 4.0, 0), 5, 30, 0.4, radius * 0.6, 1.0, -0.06);
        SpellFx.warningCircle(sl, center, radius, 0xA0FFECAA, BLESSING_RISEN);
        sound(sl, center, ModSounds.ILLUMINATOR_BLESSING_STARS, 1.3f, 1.0f);
        Later.run(sl, BLESSING_RISEN, () -> {
            SpellFx.shockRing(sl, center, radius, 0xD0FFECAA, 10, 0);
            blessingTick(sl, center, radius);
        });
    }

    /** Every second the blessing lasts: stars sink toward the ground, motes float up. */
    public static void blessingTick(ServerLevel sl, Vec3 center, double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            double a = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            Vec3 at = center.add(Math.cos(a) * far, 0, Math.sin(a) * far);
            if (i % 2 == 0) ParticleShapes.spawn(sl, STAR_SPARK, at.add(0, 3.5, 0), new Vec3(0, -0.12, 0));
            else ParticleShapes.spawn(sl, GOLD_MOTE, at.add(0, 0.2, 0), new Vec3(0, 0.05, 0));
        }
    }

    /** An enemy pressed down: violet light falling onto it. */
    public static void pressed(ServerLevel sl, LivingEntity enemy) {
        ParticleShapes.burst(sl, CURSE_MOTE.color(0xFFB078FF).fadeOut().gravity(0.08f), headOf(enemy).add(0, 0.6, 0), 4, 0.0, 0.02);
        ParticleShapes.ring(sl, CURSE_MOTE.color(0xFFB078FF).fadeOut(), enemy.position().add(0, 0.1, 0), Math.max(0.5, enemy.getBbWidth()), 6, 0.02);
    }

    /** An ally made weightless: soft motes lifting off their feet. */
    public static void weightless(ServerLevel sl, LivingEntity ally) {
        ParticleShapes.ring(sl, GOLD_MOTE, ally.position().add(0, 0.1, 0), Math.max(0.5, ally.getBbWidth()), 6, 0.0);
    }

    public static void blessingEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        SpellFx.closingRing(sl, center, radius, 0x90FFECAA, 10, 0);
    }

    // ---------- False Constellations

    /** How high the false sky hangs over its circle. */
    public static final double FALSE_SKY_HEIGHT = 7.0;

    /** A fake starry sky opens over a circle 'radius' wide for 'ticks', a faint ring marking its edge below. */
    public static void falseConstellations(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        ModelFx sky = FALSE_SKY.scale((float) (radius * 2.0)).lifetime(ticks).fade(16, 16).spin(0.15f).tag(tag)
                .key(0, pose().scale(0.1f))
                .key(16, pose().scale(1f), Ease.OUT);
        ParticleShapes.model(sl, sky, center.add(0, FALSE_SKY_HEIGHT, 0), 0f, 0f, 0f);
        floor(sl, BLUEPRINT_CIRCLE, center, radius, 0x404696F0, ticks, 0.2f, tag, 4);
        SpellFx.steadyRing(sl, center, radius, 0x80BEE4FF, 16, 0);
        for (int i = 0; i < 20; i++) {
            Later.run(sl, i, () -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                double a = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
                ParticleShapes.spawn(sl, SPARKLE, center.add(Math.cos(a) * far, FALSE_SKY_HEIGHT - 0.3, Math.sin(a) * far), Vec3.ZERO);
            });
        }
        sound(sl, center, ModSounds.ILLUMINATOR_FALSE_CONSTELLATIONS, 1.3f, 1.0f);
    }

    public static void falseConstellationsEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.clearModels(sl, center.add(0, FALSE_SKY_HEIGHT, 0), tag);
        SpellFx.closingRing(sl, center, radius, 0x80BEE4FF, 10, 0);
    }

    // ---------- Meteor Shower

    /** The tick a meteor lands (its warning ring shows until then). */
    public static final int METEOR_LANDS = 10;

    /** The shower begins over 'center': a ring marks the field. Then meteor(...) for each one. */
    public static void meteorShower(ServerLevel sl, Vec3 center, double radius) {
        SpellFx.steadyRing(sl, center, radius, 0x80FFECAA, 20, 0);
        ParticleShapes.spiral(sl, STAR_SPARK, center.add(0, 12.0, 0), 3, 20, 0.5, radius * 0.6, 0.8, 0.04);
    }

    /** One meteor of light falls head first onto 'ground' (strong = inside a false sky: bigger, brighter). */
    public static void meteor(ServerLevel sl, Vec3 ground, boolean strong) {
        double radius = strong ? 3.2 : 2.5;
        SpellFx.warningCircle(sl, ground, radius, 0xC0FFECAA, METEOR_LANDS);
        float yawOff = randomYaw(sl);
        ModelFx falling = METEOR.scale(strong ? 1.6f : 1.2f).lifetime(METEOR_LANDS + 2).fade(0, 2)
                .aura(0x60FFE6A0, 0.15f, 3)
                .key(0, pose().up(-16f).pitch(-20f))
                .key(METEOR_LANDS, pose().up(0f).pitch(0f), Ease.IN);
        ParticleShapes.model(sl, falling, ground, yawOff, 180f, 0f);
        sound(sl, ground.add(0, 6, 0), ModSounds.ILLUMINATOR_METEOR_FALL, 1.0f, wobble(1.0f));
        Later.run(sl, METEOR_LANDS, () -> {
            SpellFx.shockRing(sl, ground, radius, 0xE0FFECAA, 8, 0);
            ParticleShapes.burst(sl, STAR_SPARK, ground.add(0, 0.4, 0), strong ? 22 : 14, 0.08, 0.3);
            ParticleShapes.burst(sl, GOLD_MOTE, ground.add(0, 0.4, 0), 10, 0.04, 0.12);
            sound(sl, ground, ModSounds.ILLUMINATOR_METEOR_IMPACT, strong ? 1.4f : 1.1f, wobble(1.0f));
        });
    }

    // =====================================================================================
    // Key 6: Alchemy and Arcana (Alchemist, Arcane Scholar)
    // =====================================================================================

    // ---------- Living Doll

    /** The tick the doll has risen out of the ground and stands. */
    public static final int DOLL_STANDS = 14;

    /** A doll of 'material' rises out of the ground at 'at' (show the body on its mob from DOLL_STANDS). */
    public static void dollRise(ServerLevel sl, Vec3 at, float yaw, Material material) {
        ModelFx rising = material.body.lifetime(DOLL_STANDS + 1).fade(2, 1)
                .key(0, pose().up(-1.5f))
                .key(DOLL_STANDS, pose().up(0f), Ease.OUT);
        ParticleShapes.model(sl, rising, at, yaw, 0f, 0f);
        ParticleFx shed = dust(material);
        for (int t = 0; t < DOLL_STANDS; t += 2) {
            Later.run(sl, t, () -> ParticleShapes.ring(sl, shed, at.add(0, 0.1, 0), 0.6, 6, 0.04));
        }
        SpellFx.shockRing(sl, at, 1.4, withAlpha(material.color, 0xC0), 8, 0);
        sound(sl, at, ModSounds.ILLUMINATOR_DOLL_RISE, 0.9f, material == Material.STONE ? 0.8f : material == Material.STRAW ? 1.2f : 1.0f);
    }

    /** The doll's body worn on its mob for 'ticks' (scale = the mob's height / 1.45). */
    public static void dollBody(ServerLevel sl, LivingEntity doll, Material material, int ticks, String tag) {
        float scale = (float) (doll.getBbHeight() / 1.45);
        wear(sl, doll, material.body.scale(scale).fade(0, 6), Vec3.ZERO, ticks, tag);
    }

    /** The Iron Doll throws a bolt (the damage is the ability's, when it lands). */
    public static void dollBolt(ServerLevel sl, LivingEntity doll, Vec3 to) {
        Vec3 from = chestOf(doll);
        ParticleShapes.slashBetween(sl, TRACER.color(0xC0C8D0DC), from, to);
        ParticleShapes.burst(sl, SPARK, to, 4, 0.04, 0.1);
        sound(sl, from, ModSounds.ILLUMINATOR_RIFLE_SHOT, 0.5f, 1.5f);
    }

    /** The doll falls (dead, or its time is up): it crumbles back into what it was made of. */
    public static void dollFall(ServerLevel sl, LivingEntity doll, Material material, String tag) {
        ParticleShapes.clearModels(sl, doll, tag);
        ParticleShapes.burst(sl, dust(material), chestOf(doll), 18, 0.04, 0.16);
        if (material == Material.STRAW) SpellFx.shockRing(sl, doll.position(), 3.0, 0xC0F0D888, 8, 0);
        sound(sl, doll.position(), ModSounds.ILLUMINATOR_DOLL_FALL, 0.9f, wobble(1.0f));
    }

    // ---------- Golem

    /** The tick the golem is whole (the silent countdown is the ability's). */
    public static final int GOLEM_STANDS = 80;

    /**
     * Brass and iron fly together over four seconds at 'at': plates and gears spiral in from all round and the golem
     * grows out of them, standing at GOLEM_STANDS (then golemBody on its mob).
     */
    public static void golemAssemble(ServerLevel sl, Vec3 at, float yaw) {
        ModelFx forming = GOLEM.lifetime(GOLEM_STANDS + 2).fade(10, 1)
                .key(0, pose().frame(1f).scale(0.2f).alpha(0f).spin(0f))
                .key(GOLEM_STANDS - 10, pose().scale(0.85f).alpha(0.8f).spin(720f), Ease.IN_OUT)
                .key(GOLEM_STANDS, pose().frame(0f).scale(1f).alpha(1f).spin(720f), Ease.OUT_BACK);
        ParticleShapes.model(sl, forming, at, yaw, 0f, 0f);
        for (int t = 0; t < GOLEM_STANDS - 8; t += 4) {
            int step = t;
            Later.run(sl, t, () -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                double a = random.nextDouble() * Math.PI * 2;
                Vec3 from = at.add(Math.cos(a) * 6, 1 + random.nextDouble() * 3, Math.sin(a) * 6);
                Vec3 to = at.add(0, 1.5, 0);
                ParticleShapes.spawn(sl, step % 8 == 0 ? FLOATING_GEAR : BRASS_MOTE, from, to.subtract(from).scale(0.08));
                if (step % 12 == 0) {
                    ModelFx part = GEAR_MODEL.scale(0.5f).lifetime(12).fade(0, 4).spin(30f)
                            .key(0, pose().up(0f))
                            .key(12, pose().up((float) to.subtract(from).length()), Ease.IN);
                    ParticleShapes.modelAlong(sl, part, from, to.subtract(from));
                }
            });
        }
        sound(sl, at, ModSounds.ILLUMINATOR_GOLEM_ASSEMBLE, 1.2f, 1.0f);
        Later.run(sl, GOLEM_STANDS, () -> {
            SpellFx.shockRing(sl, at, 3.0, 0xE0D9A441, 10, 0);
            ParticleShapes.burst(sl, STEAM, at.add(0, 2.4, 0).add(ahead(yaw).scale(-0.5)), 6, 0.02, 0.06);
        });
    }

    /** The golem's body worn on its mob for 'ticks' (scale = the mob's height / 3). */
    public static void golemBody(ServerLevel sl, LivingEntity golem, int ticks, String tag) {
        float scale = (float) (golem.getBbHeight() / 3.0);
        ModelFx body = GOLEM.scale(scale).fade(0, 8).key(0, pose().frame(0f));
        wear(sl, golem, body, Vec3.ZERO, ticks, tag);
    }

    /** The tick the golem's blow lands. */
    public static final int GOLEM_STRIKE_LANDS = 8;

    /** The golem draws back and strikes, then stands again; 'ticksLeft' = how long its body still lasts. */
    public static void golemStrike(ServerLevel sl, LivingEntity golem, int ticksLeft, String tag) {
        float scale = (float) (golem.getBbHeight() / 3.0);
        ModelFx body = GOLEM.scale(scale).fade(0, 8)
                .key(0, pose().frame(0f))
                .key(5, pose().frame(1f), Ease.OUT)
                .key(GOLEM_STRIKE_LANDS, pose().frame(2f).forward(0.4f), Ease.IN)
                .key(GOLEM_STRIKE_LANDS + 10, pose().frame(0f).forward(0f), Ease.IN_OUT);
        wear(sl, golem, body, Vec3.ZERO, ticksLeft, tag);
        sound(sl, golem.position(), ModSounds.ILLUMINATOR_GOLEM_STRIKE, 1.0f, wobble(1.0f));
        Later.run(sl, GOLEM_STRIKE_LANDS, () -> {
            if (!golem.isAlive()) return;
            Vec3 fist = golem.position().add(ahead(golem.getYRot()).scale(golem.getBbWidth() * 0.9)).add(0, golem.getBbHeight() * 0.4, 0);
            ParticleShapes.burst(sl, SPARK, fist, 8, 0.05, 0.15);
            ParticleShapes.burst(sl, STEAM, golem.position().add(0, golem.getBbHeight(), 0), 3, 0.02, 0.05);
        });
    }

    /** The golem calls the enemies within 'radius' to it: a steam whistle and a ring of brass. */
    public static void golemTaunt(ServerLevel sl, LivingEntity golem, double radius) {
        SpellFx.shockRing(sl, golem.position(), radius, 0xC0D9A441, 12, 0);
        ParticleShapes.burst(sl, STEAM, golem.position().add(0, golem.getBbHeight() + 0.2, 0), 8, 0.03, 0.1);
        sound(sl, golem.position(), ModSounds.ILLUMINATOR_GOLEM_TAUNT, 1.3f, 1.0f);
    }

    /** The golem falls apart (dead, out of time, or its maker too far). */
    public static void golemEnd(ServerLevel sl, LivingEntity golem, String tag) {
        ParticleShapes.clearModels(sl, golem, tag);
        constructTakenApart(sl, golem, true);
    }

    // ---------- Mechanical Body

    /** The tick the last plate clamps on. */
    public static final int MECH_ON = 12;

    /** Brass plates and gears fly onto the Alchemist and clamp on; the goggles go over the eyes. Worn for 'ticks'. */
    public static void mechanicalBody(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        float scale = (float) (player.getBbHeight() / 1.8);
        ModelFx plates = MECH_BODY.scale(scale).fade(0, 8)
                .key(0, pose().scale(1.6f).alpha(0f))
                .key(MECH_ON, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        wear(sl, player, plates, Vec3.ZERO, ticks, tag);
        ParticleShapes.clearModels(sl, player, tag + "_eyes");
        ModelFx goggles = GOGGLES.lifetime(ticks).fade(0, 8).tag(tag + "_eyes")
                .key(0, pose().up(0.6f).alpha(0f))
                .key(MECH_ON, pose().up(0f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOnHead(sl, goggles, player, 0, 0f, 0f, 0f);
        ParticleShapes.burst(sl, COG, chestOf(player), 10, 0.06, 0.16);
        Later.run(sl, MECH_ON, () -> {
            if (!player.isAlive()) return;
            ParticleShapes.burst(sl, STEAM, chestOf(player), 4, 0.02, 0.06);
            ParticleShapes.burst(sl, BLUE_MOTE, player.getEyePosition(), 4, 0.01, 0.03);
        });
        sound(sl, player.position(), ModSounds.ILLUMINATOR_MECH_ON, 1.0f, 1.0f);
    }

    /** The Mechanical Body's arm cannon raised (while the Clockwork Rifle fires through it), for 'ticks'. */
    public static void armCannon(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        float scale = (float) (player.getBbHeight() / 1.8);
        wear(sl, player, ARM_CANNON.scale(scale).fade(2, 6), Vec3.ZERO, ticks, tag + "_arm");
    }

    /** While the jetpack burns (call it every 2 or 3 ticks): steam and flame from the two nozzles. */
    public static void jet(ServerLevel sl, LivingEntity player) {
        Vec3 back = Spells.flatLook(player).scale(-0.32);
        Vec3 right = Spells.rightOf(player).scale(0.16);
        Vec3 base = player.position().add(0, 0.65, 0).add(back);
        for (Vec3 nozzle : new Vec3[]{base.add(right), base.subtract(right)}) {
            ParticleShapes.spawn(sl, JET, nozzle, new Vec3(0, -0.15, 0));
            ParticleShapes.spawn(sl, SpellFx.FIRE_EMBER, nozzle, new Vec3(0, -0.1, 0));
        }
        if (player.tickCount % 12 == 0) sound(sl, player.position(), ModSounds.ILLUMINATOR_JETPACK, 0.7f, wobble(1.0f));
    }

    /** The plates come away. */
    public static void mechanicalBodyEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.clearModels(sl, player, tag + "_eyes");
        ParticleShapes.clearModels(sl, player, tag + "_arm");
        ParticleShapes.burst(sl, COG, chestOf(player), 8, 0.06, 0.14);
        ParticleShapes.burst(sl, STEAM, chestOf(player), 5, 0.02, 0.06);
        sound(sl, player.position(), ModSounds.ILLUMINATOR_MECH_OFF, 0.9f, 1.0f);
    }

    // ---------- the Mythical Creature Form (Knowledge Magister): white steam

    /** The Magister dissolves into white steam for 'ticks' (the hiding is the ability's). */
    public static void steamForm(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        ParticleShapes.burst(sl, STEAM, chestOf(player), 16, 0.04, 0.14);
        ParticleShapes.burst(sl, PAGE, chestOf(player), 6, 0.04, 0.1);
        SpellFx.shockRing(sl, player.position(), 2.0, 0xC0ECF0F6, 8, 0);
        sound(sl, player.position(), ModSounds.ILLUMINATOR_STEAM_FORM, 1.0f, 1.0f);
    }

    /** Every tick or two in the steam form: a cloud rolling along the ground. */
    public static void steamTrail(ServerLevel sl, LivingEntity player) {
        ParticleShapes.spawn(sl, STEAM, player.position().add(0, 0.4, 0), Vec3.ZERO);
        if (sl.getRandom().nextInt(4) == 0) ParticleShapes.spawn(sl, TEXT, player.position().add(0, 0.6, 0), new Vec3(0, 0.02, 0));
    }

    /** The steam gathers back into the Magister. */
    public static void steamReform(ServerLevel sl, LivingEntity player, String tag) {
        SpellFx.closingRing(sl, player.position(), 2.0, 0xC0ECF0F6, 8, 0);
        ParticleShapes.burst(sl, STEAM, chestOf(player), 8, 0.02, 0.06);
        sound(sl, player.position(), ModSounds.ILLUMINATOR_STEAM_REFORM, 0.9f, 1.0f);
    }

    // ---------- Dissociation

    /** A clear, misty beam from the eye: 'from' (the eye) to 'to', 'width' blocks wide. Instant. */
    public static void dissociation(ServerLevel sl, LivingEntity player, Vec3 from, Vec3 to, double width) {
        Vec3 way = to.subtract(from);
        double length = way.length();
        if (length < 0.1) return;
        float w = (float) width;
        ModelFx beam = MIST_BEAM.color(0xC0F0F4FA).scale(w, (float) length, w).lifetime(16).fade(0, 10)
                .key(0, pose().scale(0.3f))
                .key(3, pose().scale(1f), Ease.OUT);
        ParticleShapes.modelAlong(sl, beam, from, way);
        ParticleShapes.slashBetween(sl, MIST_LINE.width(w * 0.6f), from, to);
        Vec3 step = way.normalize();
        for (double d = 1; d < length; d += 1.5) {
            Vec3 at = from.add(step.scale(d));
            ParticleShapes.spawn(sl, MIST, at, Vec3.ZERO);
            if (sl.getRandom().nextBoolean()) ParticleShapes.spawn(sl, FRAGMENT, at, new Vec3(0, 0.02, 0));
        }
        ParticleShapes.spawn(sl, LENS.color(0xFFF0F4FA).fadeOut(), from, Vec3.ZERO);
        sound(sl, player.position(), ModSounds.ILLUMINATOR_DISSOCIATION, 1.2f, wobble(1.0f));
    }

    /** Something the beam touched: pale flakes peel off it (armor wearing away, a shield or hiding ending). */
    public static void dissolved(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, FRAGMENT, chestOf(target), 12, 0.04, 0.12);
        ParticleShapes.burst(sl, MIST, chestOf(target), 2, 0.01, 0.03);
    }

    /** A block the beam crossed flashes and sheds pale fragments, its color draining (the block is not changed). */
    public static void blockFades(ServerLevel sl, Vec3 blockCenter) {
        ParticleShapes.burst(sl, FRAGMENT, blockCenter, 4, 0.02, 0.06);
        ParticleShapes.spawn(sl, SPARKLE.color(0xFFF0F4FA).fadeOut(), blockCenter, Vec3.ZERO);
    }

    // ---------- Knowledge Spirit

    /**
     * The Scholar turns into drifting pages and flies from 'from' to 'to' in 'flight' ticks (into a mind, or a jump
     * when no enemy is in view). The hiding and the move are the ability's.
     */
    public static void knowledgeSpirit(ServerLevel sl, LivingEntity player, Vec3 from, Vec3 to, int flight) {
        Vec3 a = from.add(0, 1.0, 0);
        Vec3 b = to.add(0, 1.0, 0);
        ParticleShapes.burst(sl, PAGE, a, 18, 0.05, 0.16);
        Vec3 way = b.subtract(a);
        for (int t = 0; t < flight; t += 2) {
            int step = t;
            Later.run(sl, t, () -> {
                Vec3 at = a.add(way.scale(step / (double) Math.max(1, flight)));
                ParticleShapes.burst(sl, PAGE, at, 4, 0.02, 0.06);
                if (step % 4 == 0) ParticleShapes.spawn(sl, TEXT, at, Vec3.ZERO);
            });
        }
        ParticleShapes.slashBetween(sl, PAGE_TRAIL.lifetime(flight + 4).sweep(Math.max(1, flight)), a, b);
        sound(sl, from, ModSounds.ILLUMINATOR_KNOWLEDGE_SPIRIT, 1.0f, wobble(1.0f));
    }

    /** The Spirit goes through the target's mind: its thoughts scatter as text. */
    public static void mindEntered(ServerLevel sl, LivingEntity target) {
        Vec3 at = headOf(target);
        ParticleShapes.burst(sl, TEXT, at, 14, 0.04, 0.12);
        ParticleShapes.burst(sl, PAGE, at, 8, 0.04, 0.12);
        sound(sl, at, ModSounds.ILLUMINATOR_MIND_ENTER, 1.0f, wobble(1.0f));
    }

    /** Eroded for 'ticks': a dissolving page over the head. */
    public static void eroded(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        if (ticks <= 0) return;
        ModelFx mark = ERODED_MARK.color(0xD0F3E6C4).scale(0.5f).lifetime(ticks).fade(4, 10).spin(1.5f).tag(tag);
        ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.55, 0), 0f, 0f, 0f);
    }

    public static void erodedEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
    }

    /** The Scholar steps out beside the target in a burst of pages. */
    public static void spiritStepOut(ServerLevel sl, LivingEntity player) {
        ParticleShapes.burst(sl, PAGE, chestOf(player), 16, 0.05, 0.16);
        SpellFx.closingRing(sl, player.position(), 1.8, 0xC0F3E6C4, 8, 0);
    }

    // =====================================================================================
    // Key 7: Civilization (Knowledge Magister, Illuminator, Paragon)
    // =====================================================================================

    // ---------- Rewrite Principles

    /** The tick the new principle holds. */
    public static final int PRINCIPLES_RISEN = 12;

    /** The laws are rewritten over a circle 'radius' wide for 'ticks': a circle of struck-out equations, arrows rising. */
    public static void rewritePrinciples(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        floor(sl, PRINCIPLE_CIRCLE, center, radius, 0xC0FFF4D0, ticks, 0.4f, tag, 0);
        floor(sl, BLUEPRINT_CIRCLE, center, radius * 0.5, 0x704696F0, ticks, -0.8f, tag, 6);
        equationBurst(sl, center.add(0, 1.0, 0), 16, 0.08, 0.2);
        sound(sl, center, ModSounds.ILLUMINATOR_REWRITE_PRINCIPLES, 1.4f, 1.0f);
        Later.run(sl, PRINCIPLES_RISEN, () -> {
            SpellFx.risingRings(sl, center, radius, 3.0, 3, 0xA0FFF4D0);
            principlesTick(sl, center, radius);
        });
    }

    /** Every second the principle holds: equations and motes drifting up out of the ground. */
    public static void principlesTick(ServerLevel sl, Vec3 center, double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            double a = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            Vec3 at = center.add(Math.cos(a) * far, 0.2, Math.sin(a) * far);
            ParticleShapes.spawn(sl, i % 3 == 0 ? anyEquation() : GOLD_MOTE, at, new Vec3(0, 0.06, 0));
        }
    }

    /** An enemy lifted off the ground for 'ticks': a faint ring under it, a little below its feet. */
    public static void lifted(ServerLevel sl, LivingEntity enemy, int ticks, String tag) {
        SpellFx.circleUnder(sl, enemy, Math.max(0.6, enemy.getBbWidth()), 0x80BEE4FF, ticks, tag);
        ParticleShapes.burst(sl, BLUE_MOTE, enemy.position(), 6, 0.02, 0.05);
    }

    public static void principlesEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        SpellFx.closingRing(sl, center, radius, 0x90FFF4D0, 12, 0);
    }

    // ---------- Colossus

    /** The tick the Colossus is whole round the Magister. */
    public static final int COLOSSUS_READY = 40;

    /** The Colossus's size: 2 = 6 blocks. */
    public static final float COLOSSUS_SCALE = 2f;

    /** A walking machine of brass and steam assembles round the Magister for 'ticks' (they stand in its cage). */
    public static void colossus(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        ModelFx body = COLOSSUS.scale(COLOSSUS_SCALE).fade(0, 12)
                .key(0, pose().frame(1f).scale(0.3f).alpha(0f).spin(-90f))
                .key(COLOSSUS_READY - 8, pose().scale(0.95f).alpha(1f).spin(0f), Ease.OUT)
                .key(COLOSSUS_READY, pose().frame(0f).scale(1f), Ease.OUT_BACK);
        wear(sl, player, body, Vec3.ZERO, ticks, tag);
        for (int t = 0; t < COLOSSUS_READY; t += 3) {
            int step = t;
            Later.run(sl, t, () -> {
                if (!player.isAlive()) return;
                ThreadLocalRandom random = ThreadLocalRandom.current();
                double a = random.nextDouble() * Math.PI * 2;
                Vec3 from = player.position().add(Math.cos(a) * 7, 1 + random.nextDouble() * 5, Math.sin(a) * 7);
                Vec3 to = player.position().add(0, 3, 0);
                ParticleShapes.spawn(sl, step % 6 == 0 ? FLOATING_GEAR : BRASS_MOTE, from, to.subtract(from).scale(0.1));
                if (step % 9 == 0) ParticleShapes.burst(sl, STEAM, player.position().add(0, 1, 0), 3, 0.03, 0.08);
            });
        }
        sound(sl, player.position(), ModSounds.ILLUMINATOR_COLOSSUS_ASSEMBLE, 1.6f, 1.0f);
        Later.run(sl, COLOSSUS_READY, () -> {
            if (!player.isAlive()) return;
            SpellFx.shockRing(sl, player.position(), 6.0, 0xE0D9A441, 12, 0);
            ParticleShapes.burst(sl, STEAM, player.position().add(0, 5.5, 0), 10, 0.03, 0.1);
        });
    }

    /** A step of the Colossus: the ground shakes in a ring 6 blocks wide, steam from its stacks. */
    public static void colossusStep(ServerLevel sl, LivingEntity player) {
        SpellFx.shockRing(sl, player.position(), 6.0, 0x90D9A441, 8, 0);
        ParticleShapes.burst(sl, STEAM, player.position().add(Spells.flatLook(player).scale(-1.0)).add(0, 5.5, 0), 3, 0.02, 0.06);
        sound(sl, player.position(), ModSounds.ILLUMINATOR_COLOSSUS_STEP, 1.2f, wobble(1.0f));
    }

    /** Walking: the Colossus swings its legs for a stride (20 ticks); 'ticksLeft' = how long it still stands. */
    public static void colossusStride(ServerLevel sl, LivingEntity player, int ticksLeft, String tag) {
        ModelFx body = COLOSSUS.scale(COLOSSUS_SCALE).fade(0, 12)
                .key(0, pose().frame(0f))
                .key(10, pose().frame(1f), Ease.IN_OUT)
                .key(20, pose().frame(0f), Ease.IN_OUT);
        wear(sl, player, body, Vec3.ZERO, ticksLeft, tag);
        Later.run(sl, 10, () -> {
            if (player.isAlive()) colossusStep(sl, player);
        });
    }

    /** The tick the Fusion Beam fires after the charge. */
    public static final int FUSION_FIRES = 14;

    /** The fusion cannon is raised and charges (its arm comes up, the core brightens); 'ticksLeft' as above. */
    public static void fusionCharge(ServerLevel sl, LivingEntity player, int ticksLeft, String tag) {
        ModelFx body = COLOSSUS.scale(COLOSSUS_SCALE).fade(0, 12)
                .key(0, pose().frame(0f))
                .key(FUSION_FIRES - 4, pose().frame(2f), Ease.OUT)
                .key(FUSION_FIRES + 10, pose().frame(2f))
                .key(FUSION_FIRES + 20, pose().frame(0f), Ease.IN_OUT);
        wear(sl, player, body, Vec3.ZERO, ticksLeft, tag);
        for (int t = 0; t < FUSION_FIRES; t += 2) {
            Later.run(sl, t, () -> {
                if (!player.isAlive()) return;
                Vec3 core = player.position().add(Spells.flatLook(player).scale(1.1)).add(0, 2.6, 0);
                ParticleShapes.spawn(sl, GOLD_MOTE, core.add(sl.getRandom().nextGaussian() * 0.8, sl.getRandom().nextGaussian() * 0.8,
                        sl.getRandom().nextGaussian() * 0.8), Vec3.ZERO);
            });
        }
        sound(sl, player.position(), ModSounds.ILLUMINATOR_FUSION_CHARGE, 1.2f, 1.0f);
    }

    /** The Fusion Beam: an instant line of white-gold light 'width' wide from 'from' to 'to'. */
    public static void fusionBeam(ServerLevel sl, Vec3 from, Vec3 to, double width) {
        Vec3 way = to.subtract(from);
        double length = way.length();
        if (length < 0.1) return;
        float w = (float) width;
        ModelFx beam = FUSION_BEAM.color(0xF0FFF4D0).scale(w, (float) length, w).lifetime(18).fade(0, 12)
                .key(0, pose().scale(0.2f))
                .key(3, pose().scale(1.1f), Ease.OUT)
                .key(18, pose().scale(0.6f), Ease.IN);
        ParticleShapes.modelAlong(sl, beam, from, way);
        ParticleShapes.slashBetween(sl, FUSION_LINE.width(w * 0.5f), from, to);
        Vec3 step = way.normalize();
        for (double d = 2; d < length; d += 3) {
            ParticleShapes.burst(sl, GOLD_MOTE, from.add(step.scale(d)), 2, 0.04, 0.1);
        }
        SpellFx.blast(sl, to, Math.max(1.5, width), 0xFFFFF4D0, GOLD_MOTE);
        sound(sl, from, ModSounds.ILLUMINATOR_FUSION_BEAM, 2.0f, 1.0f);
    }

    /** The Colossus comes apart (its time is up, or its absorption is gone). */
    public static void colossusEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        Vec3 at = player.position().add(0, 2.5, 0);
        ParticleShapes.burst(sl, GEAR, at, 16, 0.1, 0.3);
        ParticleShapes.burst(sl, COG, at, 12, 0.1, 0.3);
        ParticleShapes.burst(sl, STEAM, at, 12, 0.04, 0.12);
        sound(sl, at, ModSounds.ILLUMINATOR_CONSTRUCT_BREAK, 1.6f, 0.7f);
    }

    // ---------- Scrolls of Civilization

    /** How many mural scrolls stand in the ring. */
    public static final int MURAL_COUNT = 8;

    /** Murals of history unfold in a ring 'radius' round the Illuminator for 'ticks', facing out, going where they go. */
    public static void scrollsOfCivilization(ServerLevel sl, LivingEntity player, double radius, int ticks, String tag) {
        float start = randomYaw(sl);
        for (int k = 0; k < MURAL_COUNT; k++) {
            float azimuth = start + k * 360f / MURAL_COUNT;
            Vec3 offset = ahead(azimuth).scale(radius).add(0, 0.2, 0);
            ModelFx mural = MURALS[k % MURALS.length].scale(1.8f, 2.6f, 1f).delay(k).lifetime(ticks).fade(4, 12).tag(tag)
                    .key(0, pose().up(-2.6f).alpha(0f))
                    .key(10, pose().up(0f).alpha(1f), Ease.OUT);
            ParticleShapes.modelOn(sl, mural, player, offset, azimuth, 0f, 0f);
        }
        ParticleShapes.ring(sl, PAGE, player.position().add(0, 1.0, 0), radius, 16, 0.02);
        sound(sl, player.position(), ModSounds.ILLUMINATOR_SCROLLS_UNFOLD, 1.3f, 1.0f);
    }

    /** A projectile stopped at the ring of murals. */
    public static void scrollsStop(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, PAGE, at, 6, 0.03, 0.08);
        ParticleShapes.burst(sl, BRASS_MOTE, at, 4, 0.02, 0.06);
    }

    /** The tick the released scrolls reach the end of their flight. */
    public static final int SCROLLS_FLY = 10;

    /** The ring ends: the murals roll up and fly outward 'radius' blocks as scrolls. */
    public static void scrollsRelease(ServerLevel sl, LivingEntity player, double radius, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        Vec3 center = player.position().add(0, 1.2, 0);
        float start = randomYaw(sl);
        for (int k = 0; k < MURAL_COUNT; k++) {
            Vec3 way = ahead(start + k * 360f / MURAL_COUNT).add(0, 0.05, 0);
            ModelFx flying = SCROLL.scale(1.2f).lifetime(SCROLLS_FLY + 4).fade(0, 4).spin(30f)
                    .key(0, pose().up(0.8f))
                    .key(SCROLLS_FLY, pose().up((float) radius), Ease.OUT);
            ParticleShapes.modelAlong(sl, flying, center, way);
        }
        SpellFx.shockRing(sl, player.position(), radius, 0xC0F3E6C4, SCROLLS_FLY, 0);
        ParticleShapes.ring(sl, PAGE, center, 1.0, 20, 0.3);
        sound(sl, player.position(), ModSounds.ILLUMINATOR_SCROLLS_RELEASE, 1.3f, 1.0f);
    }

    /** Immersed in a vision of a lost civilization for 'ticks': a mural over the head, sepia motes. */
    public static void immersed(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        if (ticks <= 0) return;
        ModelFx mural = MURALS[ThreadLocalRandom.current().nextInt(MURALS.length)].scale(0.6f, 0.8f, 1f).lifetime(ticks)
                .fade(4, 8).spin(4f).tag(tag);
        ParticleShapes.modelOn(sl, mural, target, new Vec3(0, target.getBbHeight() + 0.3, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, BRASS_MOTE, headOf(target), 8, 0.02, 0.06);
        sound(sl, target.position(), ModSounds.ILLUMINATOR_IMMERSION, 0.9f, 1.0f);
    }

    // ---------- Mathematical Maze / Enlightenment

    /** The tick the lattice has closed round the target. */
    public static final int MAZE_CLOSED = 8;

    /** A lattice of equations closes round the target (a cube 'size' blocks), held there for 'ticks'. */
    public static void mathematicalMaze(ServerLevel sl, LivingEntity player, LivingEntity target, int ticks, String tag) {
        float size = Math.max(3f, target.getBbHeight() * 1.4f);
        ModelFx cube = LATTICE.color(0xE04696F0).scale(size).lifetime(ticks).fade(0, 10).tag(tag)
                .key(0, pose().scale(3f).alpha(0f).spin(90f))
                .key(MAZE_CLOSED, pose().scale(1f).alpha(1f).spin(0f), Ease.OUT_BACK);
        ParticleShapes.model(sl, cube, target.position(), randomYaw(sl), 0f, 0f);
        ParticleShapes.slashBetween(sl, BLUEPRINT_LINE, handOf(player, sl), chestOf(target));
        equationBurst(sl, chestOf(target), 10, 0.04, 0.12);
        sound(sl, target.position(), ModSounds.ILLUMINATOR_MAZE_CLOSE, 1.2f, 1.0f);
    }

    /** The held one touches the lattice: it flares and pushes back. */
    public static void mazeTouch(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, SPARK.color(0xFFBEE4FF).fadeOut(), at, 8, 0.05, 0.14);
        equationBurst(sl, at, 2, 0.02, 0.05);
    }

    public static void mazeEnd(ServerLevel sl, Vec3 at, String tag) {
        ParticleShapes.clearModels(sl, at, tag);
        equationBurst(sl, at.add(0, 1.2, 0), 10, 0.06, 0.18);
    }

    /** Aimed at an ally: Enlightenment for 'ticks': the lamp of knowledge flares over them, gold light round them. */
    public static void enlightenment(ServerLevel sl, LivingEntity player, LivingEntity ally, int ticks, String tag) {
        flash(sl, EMBLEM, headOf(ally).add(0, 0.8, 0), yawToward(ally.position(), player.position()), 0xF0FFECAA, 1.2f, 20);
        SpellFx.circleUnder(sl, ally, Math.max(0.7, ally.getBbWidth()), 0x90FFECAA, ticks, tag);
        SpellFx.risingRings(sl, ally.position(), 0.9, 2.2, 3, 0xC0FFECAA);
        ParticleShapes.burst(sl, GOLD_MOTE, chestOf(ally), 12, 0.03, 0.1);
        sound(sl, ally.position(), ModSounds.ILLUMINATOR_ENLIGHTENMENT, 1.0f, 1.0f);
    }

    // ---------- Masterwork

    /** The Illuminator conducts for up to 'ticks': a baton in the hand, a ring of the score at their feet. */
    public static void masterwork(ServerLevel sl, LivingEntity player, double radius, int ticks, String tag) {
        ModelFx baton = BATON.scale(0.8f).fade(4, 6).key(0, pose().right(0.38f).forward(0.3f));
        for (int t = 0; t + 10 <= ticks && t < 300; t += 10) {                 // a beat every half second
            baton = baton.key(t + 5, pose().roll(t % 20 == 0 ? 30f : -20f).pitch(-25f), Ease.IN_OUT)
                    .key(t + 10, pose().roll(0f).pitch(-5f), Ease.IN_OUT);
        }
        wear(sl, player, baton, new Vec3(0, player.getEyeHeight() - 0.45, 0), ticks, tag);
        ParticleShapes.clearModels(sl, player, tag + "_score");
        ModelFx score = SYMPHONY_RING.color(0xC0FFECAA).scale(5f).lifetime(ticks).fade(8, 10).spin(0.6f).tag(tag + "_score");
        ParticleShapes.modelOn(sl, score, player, new Vec3(0, 0.08, 0), 0f, 0f, 0f);
        flash(sl, EMBLEM, headOf(player).add(0, 1.0, 0), player.getYRot() + 180f, 0xD0FFECAA, 1.4f, 24);
        SpellFx.steadyRing(sl, player.position(), radius, 0x80FFECAA, 20, 0);
        sound(sl, player.position(), ModSounds.ILLUMINATOR_MASTERWORK, 2.0f, 1.0f);
    }

    /** Every 10 ticks while the music plays: notes rise round the Illuminator and drift out to 'radius'. */
    public static void masterworkTick(ServerLevel sl, LivingEntity player, double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            ParticleShapes.spawn(sl, NOTE, player.position().add(dir.scale(1.2)).add(0, 1.4, 0), dir.scale(0.08 * radius / 20.0).add(0, 0.02, 0));
        }
        SpellFx.shockRing(sl, player.position(), radius * 0.4, 0x50FFECAA, 10, 0);
    }

    /** The music stops (its time is up, or another cast broke it off). */
    public static void masterworkEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.clearModels(sl, player, tag + "_score");
        ParticleShapes.burst(sl, NOTE, chestOf(player).add(0, 0.5, 0), 6, 0.04, 0.1);
    }

    // ---------- Reality Knowledge

    /** The tick knowledge rejects the target. */
    public static final int REALITY_LANDS = 6;

    /** Knowledge rejects the target for 'ticks': a struck-out book flashes before it and stays small over its head. */
    public static void realityKnowledge(ServerLevel sl, LivingEntity player, LivingEntity target, int ticks, String tag) {
        ParticleShapes.slashBetween(sl, HAIRLINE.color(0xE01B2A41).core(0xFFD9A441), handOf(player, sl), chestOf(target));
        sound(sl, target.position(), ModSounds.ILLUMINATOR_REALITY_KNOWLEDGE, 1.3f, 1.0f);
        Later.run(sl, REALITY_LANDS, () -> {
            if (!target.isAlive()) return;
            flash(sl, REJECTION_SEAL, chestOf(target), yawToward(target.position(), player.position()), 0xF0D9A441, 1.6f, 18);
            ParticleShapes.burst(sl, TEXT.color(0xFF1B2A41).fadeOut(), headOf(target), 12, 0.04, 0.14);
            ParticleShapes.clearModels(sl, target, tag);
            ModelFx mark = REJECTION_SEAL.color(0xD0D9A441).scale(0.45f).lifetime(Math.max(1, ticks - REALITY_LANDS))
                    .fade(4, 8).spin(2f).tag(tag);
            ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.55, 0), 0f, 0f, 0f);
        });
    }

    public static void forgottenEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
    }

    // ---------- Illusory Book

    /** The tick the book is wide open (write them in then), and when they step out after 'ticks'. */
    public static final int BOOK_OPEN = 14;
    public static final int BOOK_RELEASES = 10;

    /** A great book opens in the air at 'at' and hangs there for 'ticks' (until bookRelease). */
    public static void illusoryBook(ServerLevel sl, Vec3 at, float yaw, int ticks, String tag) {
        ModelFx book = BOOK.scale(2.4f).lifetime(ticks + BOOK_RELEASES + 6).fade(4, 6).tag(tag)
                .key(0, pose().frame(0f).scale(0.3f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK)
                .key(BOOK_OPEN, pose().frame(2f), Ease.OUT)
                .key(BOOK_OPEN + 8, pose().frame(1f), Ease.IN)
                .key(ticks, pose().frame(1f))
                .key(ticks + 6, pose().frame(2f), Ease.OUT);
        ParticleShapes.model(sl, book, at, yaw, 0f, 0f);
        ParticleShapes.burst(sl, PAGE, at.add(0, 0.5, 0), 10, 0.04, 0.12);
        sound(sl, at, ModSounds.ILLUMINATOR_BOOK_WRITE, 1.3f, 1.0f);
    }

    /** One is written into the book: they come apart into lines of text that stream into its pages. */
    public static void written(ServerLevel sl, LivingEntity victim, Vec3 book) {
        Vec3 from = chestOf(victim);
        Vec3 to = book.add(0, 0.6, 0);
        ParticleShapes.burst(sl, TEXT.color(0xFF1B2A41).fadeOut(), from, 16, 0.04, 0.12);
        for (int t = 0; t < 10; t += 2) {
            Later.run(sl, t, () -> {
                for (int i = 0; i < 3; i++) {
                    Vec3 at = from.add(sl.getRandom().nextGaussian() * 0.3, sl.getRandom().nextGaussian() * 0.5, sl.getRandom().nextGaussian() * 0.3);
                    ParticleShapes.spawn(sl, TEXT.color(0xFFF3E6C4).fadeOut(), at, to.subtract(at).scale(0.1));
                }
            });
        }
    }

    /** The book throws itself open again and lets them out where it hangs; it is gone after BOOK_RELEASES. */
    public static void bookRelease(ServerLevel sl, Vec3 at, String tag) {
        ParticleShapes.burst(sl, PAGE, at.add(0, 0.6, 0), 16, 0.05, 0.16);
        ParticleShapes.burst(sl, TEXT, at.add(0, 0.6, 0), 12, 0.05, 0.14);
        SpellFx.shockRing(sl, at.add(0, -1.0, 0), 3.0, 0xC0F3E6C4, 8, 0);
        sound(sl, at, ModSounds.ILLUMINATOR_BOOK_RELEASE, 1.2f, 1.0f);
        Later.run(sl, BOOK_RELEASES, () -> ParticleShapes.clearModels(sl, at, tag));
    }

    // ---------- Mechanical Heaven

    /** The tick the Divine Kingdom has descended. */
    public static final int HEAVEN_RISEN = 40;

    /**
     * The sky turns white-gold and a Divine Kingdom of Mechanical Heaven covers 'radius' round the Paragon for 'ticks':
     * great gears turning overhead, a fainter wheel of them on the ground, gears hanging in the air and steam at the
     * edge.
     */
    public static void mechanicalHeaven(ServerLevel sl, LivingEntity player, double radius, int ticks, String tag) {
        Vec3 center = player.position();
        double sky = Math.min(radius, 24.0) * 0.5 + 10;
        ModelFx heavens = GEAR_SKY.scale((float) (Math.min(radius, 40.0) * 2.0)).lifetime(ticks).fade(HEAVEN_RISEN, 20)
                .spin(0.08f).tag(tag)
                .key(0, pose().scale(0.2f))
                .key(HEAVEN_RISEN, pose().scale(1f), Ease.OUT);
        ParticleShapes.model(sl, heavens, center.add(0, sky, 0), 0f, 0f, 0f);
        floor(sl, GEAR_SKY, center, Math.min(radius, 40.0), 0x60FFFFFF, ticks, -0.05f, tag, 10);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {                                         // great gears hanging in the air
            double a = Math.PI * 2 * i / 10 + random.nextDouble() * 0.3;
            double far = radius * (0.35 + random.nextDouble() * 0.5);
            Vec3 at = center.add(Math.cos(a) * far, sky * (0.45 + random.nextDouble() * 0.4), Math.sin(a) * far);
            ModelFx gear = GEAR_MODEL.scale(3f + random.nextFloat() * 4f).delay(10 + i * 2).lifetime(ticks)
                    .fade(20, 20).spin((i % 2 == 0 ? 1f : -1f) * (0.6f + random.nextFloat())).tag(tag);
            ParticleShapes.model(sl, gear, at, random.nextFloat() * 360f, i % 3 == 0 ? 90f : 0f, 0f);
        }
        SpellFx.steadyRing(sl, center, Math.min(radius, 40.0), 0xD0FFE296, HEAVEN_RISEN, 0);
        sound(sl, center, ModSounds.ILLUMINATOR_MECHANICAL_HEAVEN, 2.5f, 1.0f);
        Later.run(sl, HEAVEN_RISEN, () -> heavenTick(sl, center, radius));
    }

    /** Every second while the Kingdom stands: steam rising at its edge, gears and gold motes drifting down. */
    public static void heavenTick(ServerLevel sl, Vec3 center, double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double r = Math.min(radius, 40.0);
        for (int i = 0; i < 8; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            ParticleShapes.spawn(sl, STEAM.size(0.8f).endSize(2.4f).lifetime(50, 20), center.add(Math.cos(a) * r, 0.5, Math.sin(a) * r),
                    new Vec3(0, 0.08, 0));
        }
        for (int i = 0; i < 10; i++) {
            double a = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * r;
            Vec3 at = center.add(Math.cos(a) * far, 6 + random.nextDouble() * 6, Math.sin(a) * far);
            ParticleShapes.spawn(sl, i % 3 == 0 ? FLOATING_GEAR.gravity(0.01f) : GOLD_MOTE, at, new Vec3(0, -0.05, 0));
        }
    }

    /** An enemy's projectile collapses into static inside the Kingdom. */
    public static void heavenStatic(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, SPARK.color(0xFFFFF4D0).fadeOut(), at, 12, 0.04, 0.16);
        ParticleShapes.burst(sl, COG, at, 3, 0.02, 0.06);
    }

    public static void heavenEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        double sky = Math.min(radius, 24.0) * 0.5 + 10;
        ParticleShapes.clearModels(sl, center.add(0, sky, 0), tag);
        SpellFx.closingRing(sl, center, Math.min(radius, 40.0), 0xC0FFE296, 16, 0);
        ParticleShapes.burst(sl, STEAM, center.add(0, 1.5, 0), 16, 0.05, 0.2);
    }

    // =====================================================================================
    // Passives
    // =====================================================================================

    /** Starlight (Astronomer): at night under open sky, faint stars drift down onto the Illuminator now and then. */
    public static void starlight(ServerLevel sl, LivingEntity player) {
        Vec3 above = player.position().add(sl.getRandom().nextDouble() - 0.5, 3.0, sl.getRandom().nextDouble() - 0.5);
        ParticleShapes.spawn(sl, STAR_SPARK.gravity(0.01f).lifetime(40, 10), above, new Vec3(0, -0.04, 0));
    }

    /** Constructor's Bond (Alchemist): one of the Alchemist's constructs, traced in brass for them alone. */
    public static void constructorsBond(ServerPlayer viewer, LivingEntity construct) {
        double w = Math.max(0.3, construct.getBbWidth() * 0.55), h = construct.getBbHeight();
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            Vec3 foot = construct.position().add(Math.cos(a) * w, 0.1, Math.sin(a) * w);
            ParticleShapes.spawnFor(viewer, BRASS_MOTE, foot.add(0, h * (i % 3) / 2.5, 0), Vec3.ZERO);
        }
    }

    /** Absorption (Arcane Scholar): a thread of life drawn from an enemy at 'from' into the Scholar. */
    public static void absorption(ServerLevel sl, LivingEntity player, Vec3 from) {
        Vec3 to = chestOf(player);
        ParticleShapes.spawn(sl, GOLD_MOTE.color(0xFFFFB0A0).fadeOut(), from, to.subtract(from).scale(0.08));
        if (player.tickCount % 40 == 0) sound(sl, player.position(), ModSounds.ILLUMINATOR_ABSORPTION, 0.4f, wobble(1.0f));
    }

    /** Analysis (Knowledge Magister): something concealed or disguised seen through, by the Magister alone. */
    public static void analysis(ServerPlayer viewer, LivingEntity hidden) {
        ParticleShapes.spawnFor(viewer, LENS, headOf(hidden).add(0, 0.4, 0), Vec3.ZERO);
        for (int i = 0; i < 3; i++) {
            ParticleShapes.spawnFor(viewer, EQUATIONS[i], chestOf(hidden).add(0, i * 0.3 - 0.3, 0), new Vec3(0, 0.01, 0));
        }
        soundFor(viewer, hidden.position(), ModSounds.ILLUMINATOR_ANALYSIS, 0.5f, 1.2f);
    }

    /** Enlightenment Aura (Illuminator): now and then, a gold mote rises off an ally within it. */
    public static void enlightenmentAura(ServerLevel sl, LivingEntity ally) {
        ParticleShapes.spawn(sl, GOLD_MOTE, ally.position().add(0, 0.2, 0), new Vec3(0, 0.04, 0));
    }

    /** Reality Knowledge (Paragon): someone tried to read or erase the Paragon and failed: pages close like a shield. */
    public static void realityGuard(ServerLevel sl, LivingEntity player) {
        ParticleShapes.sphere(sl, PAGE, chestOf(player), 0.9, 12);
        sound(sl, player.position(), ModSounds.ILLUMINATOR_BOOK_RELEASE, 0.5f, 1.4f);
    }

    // =====================================================================================
    // Melee combo (docs/ILLUMINATOR.md, "Melee combo"): parchment front, brass middle, ink tail
    // =====================================================================================

    private static final SlashFx COMBO_GEARS = SlashFx.arc("slash/streaks")
            .color(0xEED9A441).headColor(0xF2F3E6C4).tailColor(0xA01B2A41).core(0xFFFFE296)
            .radius(1.6f).arc(300f).width(0.5f).layers(2).spread(0.1f).lifetime(8).sweep(3).flat();
    private static final SlashFx COMBO_PAGES = SlashFx.arc("slash/dashed")
            .color(0xEED9A441).headColor(0xF2F3E6C4).tailColor(0xA01B2A41).core(0xFFF3E6C4)
            .radius(1.5f).arc(260f).width(0.4f).layers(2).spread(0.1f).lifetime(8).sweep(3).flat()
            .rotation(0f, 0f, -24f);
    private static final SlashFx COMBO_X = SlashFx.arc("slash/edge")
            .color(0xEE4696F0).headColor(0xF2F3E6C4).tailColor(0xA01B2A41).core(0xFFBEE4FF)
            .radius(1.0f).arc(150f).width(0.35f).lifetime(8).sweep(2);

    /**
     * Hit 'step' (0, 1, 2) of the melee combo on 'target'. 0: a brass ring of gear teeth sweeps round the Illuminator.
     * 1: a tilted ring of drifting pages, the other way. 2: either a blueprint spiral winding out on the ground, or two
     * crossed lines of equations on the target with a small lattice flashing round it.
     */
    public static void combo(ServerLevel sl, LivingEntity player, LivingEntity target, int step) {
        Vec3 chest = chestOf(player);
        switch (step) {
            case 0 -> {
                ParticleShapes.slash(sl, COMBO_GEARS, chest, player.getYRot(), 0f, 0f);
                ParticleShapes.ring(sl, COG, chest, 1.5, 8, 0.05);
                sound(sl, chest, ModSounds.ILLUMINATOR_GEAR_SWING, 0.8f, wobble(1.0f));
            }
            case 1 -> {
                ParticleShapes.slash(sl, COMBO_PAGES.mirrored(), chest, player.getYRot(), 0f, 0f);
                ParticleShapes.ring(sl, PAGE, chest, 1.4, 8, 0.05);
                sound(sl, chest, ModSounds.ILLUMINATOR_PAGE_SWING, 0.8f, wobble(1.0f));
            }
            default -> {
                if (sl.getRandom().nextBoolean()) {
                    ModelFx spiral = BLUEPRINT_SPIRAL.color(0xD04696F0).lifetime(16).fade(0, 8).spin(-12f)
                            .key(0, pose().scale(0.6f))
                            .key(8, pose().scale(4.4f), Ease.OUT);
                    ParticleShapes.model(sl, spiral, player.position().add(0, 0.06, 0), randomYaw(sl), 0f, 0f);
                    ParticleShapes.spiral(sl, BLUE_MOTE, player.position().add(0, 0.2, 0), 1, 18, 0.4, 2.2, 1.5, 0.06);
                } else {
                    Vec3 at = chestOf(target);
                    float yaw = yawToward(player.position(), target.position());
                    ParticleShapes.slash(sl, COMBO_X, at, yaw, 0f, 45f);
                    ParticleShapes.slash(sl, COMBO_X, at, yaw, 0f, -45f);
                    float size = Math.max(1.2f, target.getBbHeight() * 0.9f);
                    ModelFx cube = LATTICE.color(0xC04696F0).scale(size).lifetime(10).fade(0, 6)
                            .key(0, pose().scale(1.5f).alpha(0f))
                            .key(3, pose().scale(1f).alpha(1f), Ease.OUT);
                    ParticleShapes.model(sl, cube, target.position(), yaw, 0f, 0f);
                    equationBurst(sl, at, 4, 0.04, 0.1);
                }
                ParticleShapes.burst(sl, SPARKLE, chestOf(target), 6, 0.08, 0.18);
                sound(sl, chest, ModSounds.ILLUMINATOR_BLUEPRINT_STRIKE, 0.8f, wobble(1.0f));
            }
        }
    }

    // =====================================================================================
    // /illuminatorfx: every display, to look at in game
    // =====================================================================================

    @FunctionalInterface
    private interface Show {
        void play(ServerLevel sl, Player viewer, LivingEntity target, Vec3 spot);
    }

    /** onViewer = it is shown on the viewer (worn), not on a spot or a target. */
    private record Case(String name, int ticks, boolean onViewer, Show show) {}

    private static String tagFor(String name) {
        return "il_fx_" + name + "_" + UUID.randomUUID();
    }

    private static final List<Case> CASES = List.of(
            new Case("appraise", 90, false, (sl, v, t, spot) -> {
                if (t == v) return;
                Weakness[] all = Weakness.values();
                Weakness w = all[ThreadLocalRandom.current().nextInt(all.length)];
                String tag = tagFor("mark");
                appraise(sl, v, t, w, 80, tag);
                Later.run(sl, 80, () -> appraisedEnd(sl, t, tag));
            }),
            new Case("exploit_flaw", 30, false, (sl, v, t, spot) -> {
                if (t == v) noFlaw(sl, v);
                else exploitFlaw(sl, v, t);
            }),
            new Case("construct_break", 30, false, (sl, v, t, spot) -> constructTakenApart(sl, t, true)),
            new Case("flask", 40, false, (sl, v, t, spot) -> {
                flaskThrow(sl, v);
                Vec3 from = handOf(v, sl);
                Vec3 to = t == v ? spot.add(0, 0.5, 0) : chestOf(t);
                Vec3 way = to.subtract(from);
                int flight = Math.max(4, (int) (way.length() / 1.5));
                ParticleShapes.modelAlong(sl, FLASK_FLYING.lifetime(flight + 1).key(flight, pose().up((float) way.length())), from, way);
                for (int i = 0; i < flight; i++) {
                    int s = i;
                    Later.run(sl, i, () -> flaskTrail(sl, from.add(way.scale(s / (double) flight))));
                }
                Later.run(sl, flight, () -> {
                    flaskBurst(sl, to, 3);
                    if (t != v) corroded(sl, t, 60, tagFor("corroded"));
                });
            }),
            new Case("acid_puddle", 110, false, (sl, v, t, spot) -> {
                String tag = tagFor("puddle");
                flaskBurst(sl, spot.add(0, 0.5, 0), 3);
                acidPuddle(sl, spot, 3, 100, tag);
                Later.run(sl, 100, () -> acidPuddleEnd(sl, spot, tag));
            }),
            new Case("recall", 30, true, (sl, v, t, spot) -> recall(sl, v, v.position().add(Spells.flatLook(v).scale(4)), v.position())),
            new Case("survey", 40, true, (sl, v, t, spot) -> {
                survey(sl, v, 40);
                if (t != v) Later.run(sl, SURVEY_PULSE, () -> {
                    surveyed(sl, t);
                    surveyForcedOut(sl, t);
                });
            }),
            new Case("survey_residue", 40, false, (sl, v, t, spot) -> {
                if (v instanceof ServerPlayer viewer) {
                    for (int i = 0; i < 3; i++) Later.run(sl, i * 12, () -> surveyResidue(viewer, spot, 0xFFC070E8));
                }
            }),
            new Case("ritual_circle", 130, false, (sl, v, t, spot) -> {
                String tag = tagFor("ritual");
                ritualCircle(sl, v, spot, 6, 120, tag);
                for (int i = 1; i < 5; i++) Later.run(sl, RITUAL_DRAWN + i * 20, () -> {
                    ritualPulse(sl, spot, 6);
                    ritualCleansed(sl, v);
                });
                Later.run(sl, 120, () -> ritualEnd(sl, spot, 6, tag));
            }),
            new Case("clockwork_rifle", 40, false, (sl, v, t, spot) -> {
                rifle(sl, v, 3, false);
                for (int i = 0; i < 3; i++) {
                    Later.run(sl, RIFLE_FIRES + i * RIFLE_STEP, () -> {
                        Vec3 from = v.getEyePosition().add(Spells.flatLook(v).scale(1.2)).add(Spells.rightOf(v).scale(0.3)).add(0, -0.25, 0);
                        rifleShot(sl, v, from, t == v ? spot.add(0, 1, 0) : chestOf(t), t == v ? null : Weakness.PIERCE);
                    });
                }
            }),
            new Case("spirit_cannon", 140, false, (sl, v, t, spot) -> {
                String tag = tagFor("cannon");
                float yaw = yawToward(spot, v.position()) + 180f;
                spiritCannon(sl, spot, yaw, 130, tag);
                for (int i = 0; i < 3; i++) {
                    Later.run(sl, CANNON_READY + 10 + i * 30, () -> {
                        Vec3 target = t == v ? spot.add(ahead(yaw).scale(10)).add(0, 1, 0) : chestOf(t);
                        Vec3 muzzle = cannonFire(sl, spot, tag, target);
                        Vec3 way = target.subtract(muzzle);
                        int flight = Math.max(3, (int) (way.length() / 2.0));
                        ParticleShapes.modelAlong(sl, CANNONBALL.spin(20f).lifetime(flight + 1).key(flight, pose().up((float) way.length())), muzzle, way);
                        Later.run(sl, flight, () -> cannonBlast(sl, target, 2.5));
                    });
                }
                Later.run(sl, 130, () -> cannonEnd(sl, spot, tag));
            }),
            new Case("mystic_brooch", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("brooch");
                mysticBrooch(sl, v, 80, tag);
                Later.run(sl, 30, () -> broochAbsorb(sl, v));
                Later.run(sl, 80, () -> broochBurst(sl, v, 5, tag));
            }),
            new Case("star_of_curses", 120, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("curse");
                for (int i = 0; i < 3; i++) {
                    Curse curse = Curse.values()[i];
                    Later.run(sl, i * 30, () -> {
                        starOfCurses(sl, v, t, curse);
                        Later.run(sl, CURSE_LANDS, () -> cursed(sl, t, curse, 40, tag));
                    });
                }
                Later.run(sl, 110, () -> curseEnd(sl, t, tag));
            }),
            new Case("blessing_of_stars", 120, false, (sl, v, t, spot) -> {
                String tag = tagFor("blessing");
                blessingOfStars(sl, spot, 6, 110, tag);
                for (int i = 1; i < 5; i++) Later.run(sl, BLESSING_RISEN + i * 20, () -> {
                    blessingTick(sl, spot, 6);
                    weightless(sl, v);
                    if (t != v) pressed(sl, t);
                });
                Later.run(sl, 110, () -> blessingEnd(sl, spot, 6, tag));
            }),
            new Case("false_constellations", 120, false, (sl, v, t, spot) -> {
                String tag = tagFor("false_sky");
                falseConstellations(sl, spot, 7, 110, tag);
                Later.run(sl, 110, () -> falseConstellationsEnd(sl, spot, 7, tag));
            }),
            new Case("meteor_shower", 100, false, (sl, v, t, spot) -> {
                meteorShower(sl, spot, 6);
                for (int i = 0; i < 8; i++) {
                    Later.run(sl, i * 10, () -> {
                        ThreadLocalRandom random = ThreadLocalRandom.current();
                        double a = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * 6;
                        meteor(sl, spot.add(Math.cos(a) * far, 0, Math.sin(a) * far), false);
                    });
                }
            }),
            new Case("living_doll", 140, false, (sl, v, t, spot) -> {
                Material[] all = Material.values();
                for (int i = 0; i < all.length; i++) {
                    Material m = all[i];
                    Vec3 at = spot.add(ahead(v.getYRot() + 90f).scale((i - 2) * 1.6));
                    Later.run(sl, i * 6, () -> {
                        dollRise(sl, at, v.getYRot() + 180f, m);
                        ModelFx standing = m.body.lifetime(80).fade(0, 6);
                        Later.run(sl, DOLL_STANDS, () -> ParticleShapes.model(sl, standing, at, v.getYRot() + 180f, 0f, 0f));
                        Later.run(sl, DOLL_STANDS + 80, () -> ParticleShapes.burst(sl, dust(m), at.add(0, 0.8, 0), 18, 0.04, 0.16));
                    });
                }
            }),
            new Case("golem", 170, false, (sl, v, t, spot) -> {
                float yaw = yawToward(spot, v.position());
                golemAssemble(sl, spot, yaw);
                ModelFx standing = GOLEM.lifetime(70).fade(0, 8)
                        .key(0, pose().frame(0f))
                        .key(20, pose().frame(0f))
                        .key(25, pose().frame(1f), Ease.OUT)
                        .key(28, pose().frame(2f).forward(0.4f), Ease.IN)
                        .key(38, pose().frame(0f).forward(0f), Ease.IN_OUT);
                Later.run(sl, GOLEM_STANDS, () -> ParticleShapes.model(sl, standing, spot, yaw, 0f, 0f));
                Later.run(sl, GOLEM_STANDS + 28, () -> sound(sl, spot, ModSounds.ILLUMINATOR_GOLEM_STRIKE, 1.0f, 1.0f));
                Later.run(sl, GOLEM_STANDS + 45, () -> {
                    SpellFx.shockRing(sl, spot, 10, 0xC0D9A441, 12, 0);
                    sound(sl, spot, ModSounds.ILLUMINATOR_GOLEM_TAUNT, 1.0f, 1.0f);
                });
                Later.run(sl, GOLEM_STANDS + 70, () -> {
                    ParticleShapes.burst(sl, GEAR, spot.add(0, 1.5, 0), 15, 0.1, 0.3);
                    sound(sl, spot, ModSounds.ILLUMINATOR_CONSTRUCT_BREAK, 1.0f, 1.0f);
                });
            }),
            new Case("mechanical_body", 130, true, (sl, v, t, spot) -> {
                String tag = tagFor("mech");
                mechanicalBody(sl, v, 120, tag);
                Later.run(sl, 30, () -> armCannon(sl, v, 40, tag));
                for (int i = 0; i < 20; i++) Later.run(sl, 80 + i * 2, () -> jet(sl, v));
                Later.run(sl, 120, () -> mechanicalBodyEnd(sl, v, tag));
            }),
            new Case("steam_form", 50, true, (sl, v, t, spot) -> {
                String tag = tagFor("steam");
                steamForm(sl, v, 40, tag);
                for (int i = 0; i < 20; i++) Later.run(sl, i * 2, () -> steamTrail(sl, v));
                Later.run(sl, 40, () -> steamReform(sl, v, tag));
            }),
            new Case("dissociation", 30, false, (sl, v, t, spot) -> {
                Vec3 from = v.getEyePosition();
                Vec3 to = t == v ? from.add(v.getLookAngle().scale(24)) : chestOf(t);
                dissociation(sl, v, from, to, 1.5);
                if (t != v) dissolved(sl, t);
            }),
            new Case("knowledge_spirit", 60, false, (sl, v, t, spot) -> {
                Vec3 to = t == v ? spot : t.position();
                knowledgeSpirit(sl, v, v.position(), to, 16);
                Later.run(sl, 16, () -> {
                    if (t != v) {
                        mindEntered(sl, t);
                        eroded(sl, t, 40, tagFor("eroded"));
                    }
                    spiritStepOut(sl, v);
                });
            }),
            new Case("rewrite_principles", 120, false, (sl, v, t, spot) -> {
                String tag = tagFor("principles");
                rewritePrinciples(sl, spot, 7, 110, tag);
                for (int i = 1; i < 5; i++) Later.run(sl, PRINCIPLES_RISEN + i * 20, () -> principlesTick(sl, spot, 7));
                if (t != v) Later.run(sl, PRINCIPLES_RISEN, () -> lifted(sl, t, 90, tagFor("lifted")));
                Later.run(sl, 110, () -> principlesEnd(sl, spot, 7, tag));
            }),
            new Case("colossus", 160, true, (sl, v, t, spot) -> {
                String tag = tagFor("colossus");
                colossus(sl, v, 150, tag);
                Later.run(sl, COLOSSUS_READY + 10, () -> colossusStride(sl, v, 100, tag));
                Later.run(sl, COLOSSUS_READY + 40, () -> fusionCharge(sl, v, 70, tag));
                Later.run(sl, COLOSSUS_READY + 40 + FUSION_FIRES, () -> {
                    Vec3 from = v.position().add(Spells.flatLook(v).scale(1.5)).add(Spells.rightOf(v).scale(1.8)).add(0, 2.2, 0);
                    fusionBeam(sl, from, from.add(v.getLookAngle().scale(40)), 4);
                });
                Later.run(sl, 150, () -> colossusEnd(sl, v, tag));
            }),
            new Case("scrolls_of_civilization", 120, true, (sl, v, t, spot) -> {
                String tag = tagFor("scrolls");
                scrollsOfCivilization(sl, v, 5, 100, tag);
                Later.run(sl, 40, () -> scrollsStop(sl, v.position().add(Spells.flatLook(v).scale(5)).add(0, 1.2, 0)));
                Later.run(sl, 100, () -> scrollsRelease(sl, v, 12, tag));
                if (t != v) Later.run(sl, 100 + SCROLLS_FLY / 2, () -> immersed(sl, t, 40, tagFor("immersed")));
            }),
            new Case("mathematical_maze", 110, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("maze");
                Vec3 at = t.position();
                mathematicalMaze(sl, v, t, 100, tag);
                Later.run(sl, 40, () -> mazeTouch(sl, at.add(1.5, 1, 0)));
                Later.run(sl, 100, () -> mazeEnd(sl, at, tag));
            }),
            new Case("enlightenment", 40, true, (sl, v, t, spot) -> enlightenment(sl, v, v, 40, tagFor("enlightened"))),
            new Case("masterwork", 210, true, (sl, v, t, spot) -> {
                String tag = tagFor("masterwork");
                masterwork(sl, v, 20, 200, tag);
                for (int i = 1; i < 20; i++) Later.run(sl, i * 10, () -> masterworkTick(sl, v, 20));
                Later.run(sl, 200, () -> masterworkEnd(sl, v, tag));
            }),
            new Case("reality_knowledge", 110, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("forgotten");
                realityKnowledge(sl, v, t, 100, tag);
                Later.run(sl, 100, () -> forgottenEnd(sl, t, tag));
            }),
            new Case("illusory_book", 120, false, (sl, v, t, spot) -> {
                String tag = tagFor("book");
                Vec3 at = spot.add(0, 2.0, 0);
                illusoryBook(sl, at, yawToward(spot, v.position()), 90, tag);
                if (t != v) Later.run(sl, BOOK_OPEN, () -> written(sl, t, at));
                Later.run(sl, 90, () -> bookRelease(sl, at, tag));
            }),
            new Case("mechanical_heaven", 240, true, (sl, v, t, spot) -> {
                String tag = tagFor("heaven");
                Vec3 center = v.position();
                mechanicalHeaven(sl, v, 24, 230, tag);
                for (int i = 1; i < 9; i++) Later.run(sl, HEAVEN_RISEN + i * 20, () -> heavenTick(sl, center, 24));
                Later.run(sl, HEAVEN_RISEN + 30, () -> heavenStatic(sl, center.add(Spells.flatLook(v).scale(4)).add(0, 1.5, 0)));
                Later.run(sl, 230, () -> heavenEnd(sl, center, 24, tag));
            }),
            new Case("passives", 80, true, (sl, v, t, spot) -> {
                for (int i = 0; i < 6; i++) Later.run(sl, i * 6, () -> starlight(sl, v));
                if (v instanceof ServerPlayer viewer && t != v) {
                    Later.run(sl, 10, () -> constructorsBond(viewer, t));
                    Later.run(sl, 30, () -> analysis(viewer, t));
                }
                if (t != v) for (int i = 0; i < 10; i++) Later.run(sl, 40 + i * 2, () -> absorption(sl, v, chestOf(t)));
                Later.run(sl, 50, () -> enlightenmentAura(sl, v));
                Later.run(sl, 60, () -> realityGuard(sl, v));
            }),
            new Case("combo", 50, false, (sl, v, t, spot) -> {
                if (t == v) return;
                for (int i = 0; i < 4; i++) {
                    int step = Math.min(2, i);
                    Later.run(sl, i * 12, () -> combo(sl, v, t, step));
                }
            }),
            new Case("statuses", 100, true, (sl, v, t, spot) -> {
                for (Status s : Status.values()) status(v, s, 90);
            }));

    /** The names /illuminatorfx knows. */
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
        event.getDispatcher().register(Commands.literal("illuminatorfx")
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
